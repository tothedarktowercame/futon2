(ns futon2.aif.observation-checks
  "Mechanical observation checks for the checkable token classes of the WM-04
  observation contract (futon2 resources/wm/observation-contract.edn, classes
  C3-C6). Each check reads a repository at a pinned sha and returns either
  {:observed true|false :check … :evidence …} or a typed refusal
  {:status :missing :kind …}.

  These observe exactly the fact each check defines: that a path exists, that
  a declaration head is present in a file, or that a contract entry exists
  with a clojure locus. A true result is not evidence that the artifact is
  correct. Under P5 these channels have zero adjudication rates by construction
  (TokenObservation.tokenLikelihood_checkable). Any claim needing judgement is
  class J, which has no measured rate and is refused at assembly.

  Warrant checks are outside the WM observation contract."
  (:require [clojure.edn :as edn]
            [clojure.data.json :as json]
            [clojure.java.shell :as sh]
            [clojure.string :as str]))

(def repo-root "/home/joe/code")

(defn- refuse [kind data] {:status :missing :kind kind :data data})

(defn- git [repo & args]
  (apply sh/sh "git" "-C" (str repo-root "/" repo) args))

(defn- locator-refusal
  "A check needs every locator field as a non-blank string."
  [check m ks]
  (when-let [missing (seq (remove #(and (string? (get m %)) (not (str/blank? (get m %)))) ks))]
    (refuse :no-locator {:check check :missing (vec missing)})))

(defn- resolve-reference [repo reference]
  (let [{:keys [exit out]} (git repo "rev-parse" "--verify" "--end-of-options"
                               (str reference "^{commit}"))]
    (if (zero? exit)
      {:repo repo :sha reference :resolved-sha (str/trim out)}
      (refuse :unknown-sha {:repo repo :sha reference}))))

(defn check-path-exists
  "C3: resolve the declared reference once; check the file at that commit."
  [{:keys [repo sha path] :as m}]
  (or (locator-refusal :C3 m [:repo :sha :path])
      (let [reference (resolve-reference repo sha)]
        (if (:status reference) reference
            (let [{:keys [exit]} (git repo "cat-file" "-e"
                                     (str (:resolved-sha reference) ":" path))]
              {:observed (zero? exit) :check :C3
               :evidence (assoc reference :path path)})))))

(defn decl-present?
  "The declaration head DECL starts a line of TEXT (after optional leading
  whitespace), and the token after it ends at whitespace, `:`, `(`, `{`, `[`
  or end of line. So `theorem foo` does not match `theorem foo_bar`, a
  comment or a docstring (claude-7's applicability reading, 2026-09-17)."
  [text decl]
  (boolean
   (re-find (re-pattern (str "(?m)^\\s*" (java.util.regex.Pattern/quote decl) "(?=[\\s:({\\[]|$)"))
            text)))

(defn check-decl-in-file
  "C4: check the anchored declaration head at the resolved commit."
  [{:keys [repo sha path decl] :as m}]
  (or (locator-refusal :C4 m [:repo :sha :path :decl])
      (let [reference (resolve-reference repo sha)]
        (if (:status reference) reference
            (let [{:keys [exit out]} (git repo "show" (str (:resolved-sha reference) ":" path))]
              {:observed (and (zero? exit) (decl-present? out decl)) :check :C4
               :evidence (assoc reference :path path :decl decl :file-present (zero? exit))})))))

(defn- observe-locus [locus]
  (if-let [[_ repo path line] (re-matches #"([^/]+)/(.+):(\d+)" (str locus))]
    (let [reference (resolve-reference repo "HEAD")]
      (if (:status reference)
        (update reference :data assoc :locus locus)
        (let [{:keys [exit out]} (git repo "show" (str (:resolved-sha reference) ":" path))]
          {:observed (boolean (and (zero? exit)
                                   (<= (Long/parseLong line) (count (str/split-lines out)))))
           :evidence (assoc reference :path path :line (Long/parseLong line))})))
    {:observed false :reason :invalid-clojure-locus :locus locus}))

(defn check-registry-entry
  "C5: resolve the bundle reference and each locus repository HEAD separately.
   Every content check uses its recorded resolved commit."
  [{:keys [repo sha bundle-path entry] :as m}]
  (or (locator-refusal :C5 m [:repo :sha :bundle-path :entry])
      (let [reference (resolve-reference repo sha)]
        (if (:status reference) reference
          (let [{:keys [exit out]} (git repo "show" (str (:resolved-sha reference) ":" bundle-path))]
            (if-not (zero? exit)
              (refuse :bundle-not-found (assoc reference :bundle-path bundle-path))
              (let [contract (some #(when (= entry (get % "contract-id")) %)
                                   (get (json/read-str out) "contracts"))
                    loci (vec (keep #(get % "clojure-locus") (get contract "declarations")))
                    observations (into {} (map (fn [l] [l (observe-locus l)])) loci)
                    refusal (some #(when (:status %) %) (vals observations))]
                (if refusal
                  (assoc refusal :evidence (assoc reference :bundle-path bundle-path
                                                 :entry entry :locus-evidence observations))
                  (let [resolved (into {} (map (fn [[l o]] [l (boolean (:observed o))])) observations)]
                    {:observed (boolean (and contract (seq loci) (every? true? (vals resolved))))
                     :check :C5
                     :evidence (assoc reference :bundle-path bundle-path :entry entry
                                      :contract-found (boolean contract) :clojure-loci resolved
                                      :locus-evidence observations)})))))))))

(defn check-witness-reference
  "C6: an EDN witness contains {:repo string :sha string}, optionally
  :entry (a repository path at that commit). Observe only that the referenced
  commit or entry exists; no claim about its authorship or correctness.
  Missing witness is false. Malformed or unresolved references refuse."
  [{:keys [repo sha path] :as locator}]
  (or (locator-refusal :C6 locator [:repo :sha :path])
      (let [reference (resolve-reference repo sha)]
        (if (:status reference) reference
          (let [evidence (assoc reference :path path)
                {:keys [exit out]} (git repo "show" (str (:resolved-sha reference) ":" path))]
            (if-not (zero? exit)
              {:observed false :check :C6 :evidence (assoc evidence :witness-present false)}
              (let [witness (try (edn/read-string out)
                                 (catch Exception _ ::malformed))
                    bad (when (map? witness)
                          (or (locator-refusal :C6 witness [:repo :sha])
                              (when (or (:require-entry locator) (contains? witness :entry))
                                (locator-refusal :C6 witness [:entry]))))]
                (cond
                  (not (map? witness))
                  (assoc (refuse :invalid-witness {:check :C6}) :evidence evidence)
                  bad (assoc bad :evidence evidence)
                  :else
                  (let [resolved (resolve-reference (:repo witness) (:sha witness))]
                    (if (:status resolved)
                      (assoc resolved :evidence (assoc evidence :witness witness))
                      (let [entry (:entry witness)
                            present? (or (nil? entry)
                                         (zero? (:exit (git (:repo witness) "cat-file" "-e"
                                                            (str (:resolved-sha resolved) ":" entry)))))]
                        {:observed (boolean present?) :check :C6
                         :evidence (assoc evidence :witness-present true
                                          :reference (cond-> resolved entry (assoc :entry entry)))})))))))))))

(def checks
  {:C3 check-path-exists
   :C4 check-decl-in-file
   :C5 check-registry-entry
   :C6 check-witness-reference})

(defn- observe* [tokens]
  (reduce-kv
   (fn [acc token {:keys [class] :as locator}]
     (let [f (get checks class)
           r (if f (f locator) (refuse :no-mechanical-check {:class class}))]
       (if (contains? r :status)
         (assoc-in acc [:refused token] r)
         (cond-> (assoc-in acc [:results token] r)
           (:observed r) (update :observed conj token)))))
   {:observed #{} :results {} :refused {}}
   tokens))

(defn observe
  "Observe located tokens through C3/C4/C5/C6. Unknown classes are refused,
   never observed absent. No warrant service is consulted."
  [tokens]
  (observe* tokens))
