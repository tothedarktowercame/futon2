(ns futon2.aif.observation-checks
  "Mechanical observation checks for the checkable token classes of the WM-04
  observation contract (futon2 resources/wm/observation-contract.edn, classes
  C3-C5). Each check reads a repository at a pinned sha and returns either
  {:observed true|false :check … :evidence …} or a typed refusal
  {:status :missing :kind …}.

  These observe exactly the fact each check defines: that a path exists, that
  a declaration head is present in a file, or that a contract entry exists
  with a clojure locus. A true result is not evidence that the artifact is
  correct. Under P5 these channels have zero adjudication rates by construction
  (TokenObservation.tokenLikelihood_checkable). Any claim needing judgement is
  class J, which has no measured rate and is refused at assembly.

  C1 (Lean declaration builds) and C2 (test namespace aggregate) need a build
  or test run at the sha and are not implemented here."
  (:require [clojure.data.json :as json]
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

(defn- sha-refusal [repo sha]
  (let [{:keys [exit]} (git repo "cat-file" "-e" (str sha "^{commit}"))]
    (when-not (zero? exit)
      (refuse :unknown-sha {:repo repo :sha sha}))))

(defn check-path-exists
  "C3: `git cat-file -e sha:path` succeeds."
  [{:keys [repo sha path] :as m}]
  (or (locator-refusal :C3 m [:repo :sha :path])
      (sha-refusal repo sha)
      (let [{:keys [exit]} (git repo "cat-file" "-e" (str sha ":" path))]
        {:observed (zero? exit) :check :C3
         :evidence {:repo repo :sha sha :path path}})))

(defn check-decl-in-file
  "C4: the file at sha:path contains the exact declaration head `decl`."
  [{:keys [repo sha path decl] :as m}]
  (or (locator-refusal :C4 m [:repo :sha :path :decl])
      (sha-refusal repo sha)
      (let [{:keys [exit out]} (git repo "show" (str sha ":" path))]
        {:observed (and (zero? exit) (str/includes? out decl)) :check :C4
         :evidence {:repo repo :sha sha :path path :decl decl
                    :file-present (zero? exit)}})))

(defn check-registry-entry
  "C5: the emitted contract bundle JSON at sha:bundle-path contains a contract
  whose contract-id is `entry` and whose declarations include a non-blank
  clojure-locus."
  [{:keys [repo sha bundle-path entry] :as m}]
  (or (locator-refusal :C5 m [:repo :sha :bundle-path :entry])
      (sha-refusal repo sha)
      (let [{:keys [exit out]} (git repo "show" (str sha ":" bundle-path))]
        (if-not (zero? exit)
          (refuse :bundle-not-found {:repo repo :sha sha :bundle-path bundle-path})
          (let [contract (some #(when (= entry (get % "contract-id")) %)
                               (get (json/read-str out) "contracts"))
                loci (keep #(get % "clojure-locus") (get contract "declarations"))]
            {:observed (boolean (and contract (some (complement str/blank?) loci)))
             :check :C5
             :evidence {:repo repo :sha sha :bundle-path bundle-path :entry entry
                        :contract-found (boolean contract) :clojure-loci (vec loci)}})))))

(def checks
  {:C3 check-path-exists
   :C4 check-decl-in-file
   :C5 check-registry-entry})

(defn observe
  "Observe a set of located tokens at their pinned shas.
  tokens: {token {:class :C3|:C4|:C5 …locator}}.
  Returns {:observed #{tokens observed true} :results {token result}
  :refused {token refusal}}. A class without a mechanical check (C1, C2, J, or
  unknown) is refused :no-mechanical-check. It is never treated as observed or
  absent."
  [tokens]
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
