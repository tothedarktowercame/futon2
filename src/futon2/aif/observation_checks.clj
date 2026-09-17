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

  C1 (a Lean declaration builds) and C2 (a test namespace passes) never rerun
  a build or tests. They read a Test Registry warrant
  (futon3c.test-registry check, futon3c 33824f0f). The registry verifies that
  the warrant's recorded code, load closure, environment and log still match
  the current checkout, so these observe \"as of now\", not as of an arbitrary
  sha. A missing or stale warrant is observed false with its reason; nothing
  is rerun here."
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

(def futon3c-root "/home/joe/code/futon3c")

(defn- registry-check
  "Run `futon3c.test-registry check` for ENTRY-ID against REPO. Returns the
  parsed JSON result, or a typed refusal when the registry cannot be run."
  [repo entry-id]
  (let [config (java.io.File/createTempFile "wm04-registry-check" ".edn")]
    (try
      (spit config (pr-str {:agency-url "http://localhost:7070"
                            :entry-id entry-id
                            :repo-root (str repo-root "/" repo)
                            :changed-paths []
                            :output :json}))
      (let [{:keys [out err]} (sh/sh "clojure" "-M" "-m" "futon3c.test-registry" "check"
                                     (str config) :dir futon3c-root)
            line (last (remove str/blank? (str/split-lines (or out ""))))]
        (try (json/read-str line)
             (catch Exception _
               (refuse :registry-unavailable {:entry-id entry-id :err (subs (str err) 0 (min 400 (count (str err))))}))))
      (finally (.delete config)))))

(defn- warrant-evidence [result]
  {:entry-id (get-in result ["record" "entry-id"] (get result "entry-id"))
   :warrant? (get result "warrant?")
   :reason (get result "reason")
   :command (get-in result ["record" "command"])
   :results (get-in result ["record" "results"])})

(defn check-test-warrant
  "C2: the Test Registry warrant ENTRY-ID is valid now, its command runs exactly
  the named namespace, and its results show 0 failures and 0 errors."
  [{:keys [repo entry-id ns] :as m}]
  (or (locator-refusal :C2 m [:repo :entry-id :ns])
      (let [r (registry-check repo entry-id)]
        (if (contains? r :status)
          r
          (let [cmd (get-in r ["record" "command"])
                res (get-in r ["record" "results"])]
            {:observed (boolean (and (true? (get r "warrant?"))
                                     (sequential? cmd) (= ns (last cmd)) (= "-n" (last (butlast cmd)))
                                     (= 0 (get res "failures")) (= 0 (get res "errors"))))
             :check :C2
             :evidence (assoc (warrant-evidence r) :repo repo :ns ns :entry-id entry-id)})))))

(defn check-lean-warrant
  "C1: the Test Registry build warrant ENTRY-ID is valid now for
  `lake build MODULE`, with exit 0, 0 errors and 0 sorries. The file declaring
  DECL is in the warrant's load closure, and it contains the declaration head
  DECL (the warrant pins that file's bytes)."
  [{:keys [repo entry-id module path decl] :as m}]
  (or (locator-refusal :C1 m [:repo :entry-id :module :path :decl])
      (let [r (registry-check repo entry-id)]
        (if (contains? r :status)
          r
          (let [cmd (get-in r ["record" "command"])
                res (get-in r ["record" "results"])
                closure-paths (set (map #(get % "path") (get-in r ["record" "load-closure"])))
                file (java.io.File. (str repo-root "/" repo "/" path))
                decl? (and (.exists file) (str/includes? (slurp file) decl))]
            {:observed (boolean (and (true? (get r "warrant?"))
                                     (= ["lake" "build" module] cmd)
                                     (= 0 (get res "exit")) (= 0 (get res "error-count"))
                                     (= 0 (get res "sorry-count"))
                                     (contains? closure-paths path)
                                     decl?))
             :check :C1
             :evidence (assoc (warrant-evidence r) :repo repo :module module :path path :decl decl
                              :path-in-closure (contains? closure-paths path) :decl-present decl?
                              :entry-id entry-id)})))))

(def checks
  {:C1 check-lean-warrant
   :C2 check-test-warrant
   :C3 check-path-exists
   :C4 check-decl-in-file
   :C5 check-registry-entry})

(defn observe
  "Observe a set of located tokens at their pinned shas.
  tokens: {token {:class :C3|:C4|:C5 …locator}}.
  Returns {:observed #{tokens observed true} :results {token result}
  :refused {token refusal}}. A class without a mechanical check (J, or unknown) is refused :no-mechanical-check. It is never treated as observed or
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
