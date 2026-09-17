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
  (:require [cheshire.core :as cheshire]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
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

(def agency-url "http://localhost:7070")

(def ^:private registry-in-process
  "The registry's own check function and evidence backend, when futon3c is on
  THIS JVM's classpath. It is in the serving JVM, where the tick runs; it is
  not in futon2's test JVM, which falls back to the CLI. Resolved once."
  (delay
    (try
      (let [check (requiring-resolve 'futon3c.test-registry/check-record!)
            backend (requiring-resolve 'futon3c.evidence.http-backend/make-http-backend)]
        (when (and check backend) {:check check :backend backend}))
      (catch Throwable _ nil))))

(defn- in-process-check
  "The registry check in this JVM. Same result as the CLI: `-main` prints
  `(json/generate-string (check-record! backend options))`, and on a refusal
  the ex-data instead — so the JSON round-trip here produces the identical
  shape at a fraction of the cost (the CLI pays a JVM start per check, which
  was about 20 s of every tick). nil means the registry is not loadable here
  and the caller shells out."
  [repo entry-id]
  (when-let [{:keys [check backend]} @registry-in-process]
    (let [options {:agency-url agency-url
                   :entry-id entry-id
                   :repo-root (str repo-root "/" repo)
                   :changed-paths []}
          result (try (check (backend agency-url) options)
                      (catch clojure.lang.ExceptionInfo e
                        (or (ex-data e)
                            {:status :refused :reason :registry-failed}))
                      (catch Throwable e
                        {:status :refused :reason :registry-failed
                         :error (.getMessage e)}))]
      (json/read-str (cheshire/generate-string result)))))

(defn- registry-check-cli
  "Run `futon3c.test-registry check` for ENTRY-ID against REPO in its own JVM.
  Returns the parsed JSON result, or a typed refusal when the registry cannot
  be run."
  [repo entry-id]
  (let [config (java.io.File/createTempFile "wm04-registry-check" ".edn")]
    (try
      (spit config (pr-str {:agency-url agency-url
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

(defn- registry-check
  "The registry check for ENTRY-ID against REPO: in this JVM when futon3c is
  loadable here, else in a CLI JVM. Both return the same JSON shape."
  [repo entry-id]
  (or (in-process-check repo entry-id)
      (registry-check-cli repo entry-id)))

(def evidence-url (str agency-url "/api/alpha/evidence"))

(defn- registry-runs
  "Test Registry run records (newest first), read from the evidence store by
  tag. Bounded: at most LIMIT entries. A store that cannot be read is a typed
  refusal."
  [limit]
  (try
    (let [body (json/read-str (slurp (str evidence-url "?tag=test-registry&limit=" limit)))]
      (for [e (get body "entries")
            :let [payload (try (edn/read-string (get-in e ["evidence/body" "payload-edn"]))
                               (catch Exception _ nil))]
            :when (= :run (:kind payload))]
        (assoc payload :entry-id (get e "evidence/id"))))
    (catch Exception e
      (refuse :registry-unavailable {:error (.getMessage e)}))))

(def recent-limit
  "How many evidence records a pass reads before widening. The store's cost is
  proportional to what it returns: 1 record takes ~2 s, all 73 take ~13 s, and
  records are newest-first. A warrant a locator wants is almost always recent,
  so read a window and widen only on a miss."
  25)

(def wide-limit 500)

(def ^:dynamic *registry-runs*
  "Delays holding the run records for one pass of observations — {limit delay}
  — so a pass that checks several C1/C2 facts reads the store once per window
  rather than once per fact. `observe` binds this; nil means read afresh."
  nil)

(defn- runs-at
  "Run records at LIMIT, through the pass cache when there is one."
  [limit]
  (if-let [cache *registry-runs*]
    @(or (get @cache limit)
         (get (swap! cache assoc limit (delay (registry-runs limit))) limit))
    (registry-runs limit)))

(defn latest-warrant-id
  "The newest registry run for REPO whose recorded command satisfies
  COMMAND-PRED and that recorded a warrant. Returns its entry-id, or nil.
  Validity now is still decided by `check`; this only finds the candidate, so
  a locator need not name an entry-id that editing the located file would stale."
  [repo command-pred]
  (let [match (fn [runs]
                (when-not (and (map? runs) (:status runs))
                  (->> runs
                       (filter #(and (true? (:warrant? %))
                                     (= (str repo-root "/" repo) (:repo/root %))
                                     (command-pred (:command %))))
                       (sort-by :finished-at #(compare %2 %1))
                       first
                       :entry-id)))]
    ;; the recent window first; widen only when it holds no candidate, so the
    ;; common case costs ~2 s instead of ~13 s
    (or (match (runs-at recent-limit))
        (match (runs-at wide-limit)))))

(defn- resolve-entry-id
  "The locator's :entry-id when it names one (a string), else the latest
  matching warrant. Recorded on the evidence as :entry-id-source."
  [{:keys [repo entry-id]} command-pred]
  (if (string? entry-id)
    {:entry-id entry-id :entry-id-source :locator}
    (when-let [id (latest-warrant-id repo command-pred)]
      {:entry-id id :entry-id-source :latest-warrant})))

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
  "C2: the Test Registry warrant (the locator's :entry-id, or when it names
  none, the newest recorded warrant for this namespace) is valid now for exactly the named
  namespace. Observed true when its results show 0 failures and 0 errors, and
  false only when a valid warrant records failures or errors. A missing or
  stale warrant, or one for another namespace, is refused
  :no-current-warrant. The recorded code and test files must match HEAD
  (cutoff recorded), else :working-tree-differs-from-head."
  [{:keys [repo ns] :as m}]
  (or (locator-refusal :C2 m [:repo :ns])
      (let [{:keys [entry-id entry-id-source]}
            (resolve-entry-id m #(and (sequential? %) (= ns (last %)) (= "-n" (last (butlast %)))))]
       (if-not entry-id
        (refuse :no-current-warrant {:check :C2 :ns ns :entry-id-source :no-warrant-found})
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
               :evidence (assoc (warrant-evidence r) :repo repo :ns ns :entry-id entry-id
                                :entry-id-source entry-id-source)}))))))))

(defn check-lean-warrant
  "C1: a valid Test Registry warrant (the locator's :entry-id, or when it names
  none, the newest recorded warrant for the module's build) for `lake build MODULE`, with the
  file PATH in its load closure. Observed true when the build exited 0 with 0
  errors, PATH has no sorries in the warrant's per-file sorry-files, and PATH
  at HEAD has the anchored declaration head DECL. Other modules' sorries
  (e.g. Holes.lean) do not count against this declaration. A missing or stale
  warrant, a different module, or PATH outside the closure is refused
  :no-current-warrant. A dirty PATH refuses
  :working-tree-differs-from-head."
  [{:keys [repo module path decl] :as m}]
  (or (locator-refusal :C1 m [:repo :module :path :decl])
      (let [{:keys [entry-id entry-id-source]}
            (resolve-entry-id m #(= ["lake" "build" module] %))]
       (if-not entry-id
        (refuse :no-current-warrant {:check :C1 :module module :entry-id-source :no-warrant-found})
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
                                  :entry-id entry-id :entry-id-source entry-id-source)})))))))))

(def checks
  {:C1 check-lean-warrant
   :C2 check-test-warrant
   :C3 check-path-exists
   :C4 check-decl-in-file
   :C5 check-registry-entry})

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
  "Observe a set of located tokens at their pinned shas.
  tokens: {token {:class :C3|:C4|:C5 …locator}}.
  Returns {:observed #{tokens observed true} :results {token result}
  :refused {token refusal}}. A class without a mechanical check (J, or
  unknown) is refused :no-mechanical-check. It is never treated as observed
  or absent.

  The pass reads the registry's run records once (see `*registry-runs*`),
  whatever the number of C1/C2 facts in it. Nesting is safe: an outer
  binding (a whole tick, say) is kept."
  [tokens]
  (binding [*registry-runs* (or *registry-runs* (atom {}))]
    (observe* tokens)))

(defn with-registry-runs*
  "Call F with ONE read of the registry's run records shared by every
  observation made inside it — for a caller that observes several token sets
  in one pass (a tick over several declared sources, say). An outer binding
  is kept, so nesting reads once."
  [f]
  (binding [*registry-runs* (or *registry-runs* (atom {}))]
    (f)))
