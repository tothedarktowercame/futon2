(ns futon2.aif.observation-checks
  "Mechanical observation checks for the checkable token classes of the WM-04
  observation contract (futon2 resources/wm/observation-contract.edn, classes
  C1-C5). Each check reads a repository at a pinned sha and returns either
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
  (futon3c.test-registry check, futon3c 33824f0f and ab388662). The registry verifies that
  the warrant's recorded code, load closure, environment and log still match
  the current checkout, so these observe \"as of now\", not as of an arbitrary
  sha. Nothing is rerun here. A missing or stale warrant is refused, not
  observed false, and the checkout HEAD at check time is recorded as the
  cutoff."
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
  "C4: the file at sha:path has a line starting with the declaration head
  DECL (anchored, see decl-present?)."
  [{:keys [repo sha path decl] :as m}]
  (or (locator-refusal :C4 m [:repo :sha :path :decl])
      (sha-refusal repo sha)
      (let [{:keys [exit out]} (git repo "show" (str sha ":" path))]
        {:observed (and (zero? exit) (decl-present? out decl)) :check :C4
         :evidence {:repo repo :sha sha :path path :decl decl
                    :file-present (zero? exit)}})))

(defn- locus-resolves?
  "A clojure-locus \"repo/path:line\" resolves at the repo's current HEAD:
  the file exists there and has at least LINE lines."
  [locus]
  (when-let [[_ lrepo lpath line] (re-matches #"([^/]+)/(.+):(\d+)" (str locus))]
    (let [{:keys [exit out]} (git lrepo "show" (str "HEAD:" lpath))]
      (and (zero? exit)
           (<= (Long/parseLong line) (count (str/split-lines out)))))))

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
                loci (vec (keep #(get % "clojure-locus") (get contract "declarations")))
                resolved (into {} (map (fn [l] [l (boolean (locus-resolves? l))])) loci)]
            ;; every declared locus must resolve (file present with that many
            ;; lines at the locus repo's HEAD), not merely be non-blank
            {:observed (boolean (and contract (seq loci) (every? true? (vals resolved))))
             :check :C5
             :evidence {:repo repo :sha sha :bundle-path bundle-path :entry entry
                        :contract-found (boolean contract) :clojure-loci resolved}})))))

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
  {:warrant? (get result "warrant?")
   :reason (get result "reason")
   :command (get-in result ["record" "command"])
   :results (get-in result ["record" "results"])
   :git-head-at-run (get-in result ["record" "git-head"])})

(defn- head-cutoff
  "The checkout's HEAD at check time, and whether any of PATHS differs from
  HEAD in the working tree. A warrant observes the working tree now; the
  cutoff is recorded as this HEAD, and a dirty path refuses."
  [repo paths]
  (let [head (str/trim (:out (git repo "rev-parse" "HEAD")))
        dirty (vec (filter #(seq (str/trim (:out (git repo "status" "--porcelain" "--" %)))) paths))]
    {:cutoff {repo head} :dirty dirty}))

(defn- no-current-warrant
  "A missing or stale warrant says nothing about the artifact: refuse rather
  than observe false (false negatives would enter A)."
  [check entry-id r]
  (refuse :no-current-warrant {:check check :entry-id entry-id
                               :registry (warrant-evidence r)}))

(defn check-test-warrant
  "C2: the Test Registry warrant ENTRY-ID is valid now for exactly the named
  namespace. Observed true when its results show 0 failures and 0 errors, and
  false only when a valid warrant records failures or errors. A missing or
  stale warrant, or one for another namespace, is refused
  :no-current-warrant. The recorded code and test files must match HEAD
  (cutoff recorded), else :working-tree-differs-from-head."
  [{:keys [repo entry-id ns] :as m}]
  (or (locator-refusal :C2 m [:repo :entry-id :ns])
      (let [r (registry-check repo entry-id)]
        (cond
          (contains? r :status) r
          (not (true? (get r "warrant?"))) (no-current-warrant :C2 entry-id r)
          :else
          (let [cmd (get-in r ["record" "command"])
                res (get-in r ["record" "results"])
                files (concat (keys (get-in r ["record" "code-files"]))
                              (keys (get-in r ["record" "test-files"])))
                {:keys [cutoff dirty]} (head-cutoff repo files)]
            (cond
              (not (and (sequential? cmd) (= ns (last cmd)) (= "-n" (last (butlast cmd)))))
              (no-current-warrant :C2 entry-id r)
              (seq dirty)
              (refuse :working-tree-differs-from-head {:repo repo :paths dirty})
              :else
              {:observed (and (= 0 (get res "failures")) (= 0 (get res "errors")))
               :check :C2 :cutoff cutoff
               :evidence (assoc (warrant-evidence r) :repo repo :ns ns :entry-id entry-id)}))))))

(defn check-lean-warrant
  "C1: a valid Test Registry warrant ENTRY-ID for `lake build MODULE`, with the
  file PATH in its load closure. Observed true when the build exited 0 with 0
  errors, PATH has no sorries in the warrant's per-file sorry-files, and PATH
  at HEAD has the anchored declaration head DECL. Other modules' sorries
  (e.g. Holes.lean) do not count against this declaration. A missing or stale
  warrant, a different module, or PATH outside the closure is refused
  :no-current-warrant. A dirty PATH refuses
  :working-tree-differs-from-head."
  [{:keys [repo entry-id module path decl] :as m}]
  (or (locator-refusal :C1 m [:repo :entry-id :module :path :decl])
      (let [r (registry-check repo entry-id)]
        (cond
          (contains? r :status) r
          (not (true? (get r "warrant?"))) (no-current-warrant :C1 entry-id r)
          :else
          (let [cmd (get-in r ["record" "command"])
                res (get-in r ["record" "results"])
                closure-paths (set (map #(get % "path") (get-in r ["record" "load-closure"])))
                {:keys [cutoff dirty]} (head-cutoff repo [path])]
            (cond
              (not (and (= ["lake" "build" module] cmd) (contains? closure-paths path)))
              (no-current-warrant :C1 entry-id r)
              (seq dirty)
              (refuse :working-tree-differs-from-head {:repo repo :paths dirty})
              :else
              (let [{:keys [exit out]} (git repo "show" (str "HEAD:" path))
                    decl? (and (zero? exit) (decl-present? out decl))
                    file-sorries (get-in res ["sorry-files" path] 0)]
                {:observed (and (= 0 (get res "exit")) (= 0 (get res "error-count"))
                                (= 0 file-sorries) decl?)
                 :check :C1 :cutoff cutoff
                 :evidence (assoc (warrant-evidence r) :repo repo :module module :path path
                                  :decl decl :decl-present decl? :file-sorries file-sorries
                                  :entry-id entry-id)})))))))

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
