(require '[clojure.edn :as edn]
         '[clojure.java.shell :as shell]
         '[clojure.string :as str]
         '[futon2.aif.close-retention :as retention]
         '[futon2.aif.d-predecessor-task-authority :as task]
         '[futon2.aif.interpretation-evidence :as evidence])

(let [path "/home/joe/code/futon3c/data/wm-d-task-enactment/action-a5d2326d-a73a-447c-8bc7-7f77c8bbc187.edn"
      record (edn/read-string (slurp path))
      dispatch (:dispatch record)
      occurrence (:occurrence dispatch)
      action (:action/value occurrence)
      digest #(evidence/sha256 (.getBytes ^String % "UTF-8"))
      printed (into {} (for [flag [false true]]
                         [flag (binding [*print-namespace-maps* flag] (pr-str action))]))
      expected {:occurrence occurrence :carry-occurrence-id (:carry-occurrence-id dispatch)
                :universe (:universe dispatch)
                :declaration-pins (mapv #(select-keys % [:path :sha256]) (:declarations dispatch))}
      jobs (into {} (map (juxt :job-id identity)) [(:author-job record) (:review-job record)])
      attempt #(try (retention/validate-occurrence %) :valid
                    (catch clojure.lang.ExceptionInfo e (ex-data e)))
      fresh (binding [*print-namespace-maps* false]
              (retention/mint-occurrence
               {:run-id "diagnostic" :cohort-id "diagnostic" :attempt-id "diagnostic"
                :selected-action action :now #(java.time.Instant/parse "2026-09-21T04:24:00Z")
                :uuid-fn #(java.util.UUID/fromString "00000000-0000-0000-0000-000000000001")}))]
  (prn {:checkout (str/trim (:out (shell/sh "git" "rev-parse" "HEAD")))
        :clojure (clojure-version)
        :ambient-print-namespace-maps *print-namespace-maps*
        :root-print-namespace-maps (.getRawRoot #'clojure.core/*print-namespace-maps*)
        :thread-bound? (thread-bound? #'clojure.core/*print-namespace-maps*)
        :retained-record-byte-sha256 (digest (slurp path))
        :stored (:action/value-sha256 occurrence)
        :same-edn-value (= (edn/read-string (printed false)) (edn/read-string (printed true)))
        :regimes
        (mapv (fn [flag]
                (binding [*print-namespace-maps* flag]
                  {:print-namespace-maps flag :digest (digest (printed flag))
                   :retained-occurrence (attempt occurrence)
                   :newly-minted-occurrence (attempt fresh)
                   :v1 (select-keys (task/verify record expected jobs) [:status :kind :unknown :detail])
                   :v2 (when-let [f (ns-resolve 'futon2.aif.d-predecessor-task-authority
                                               'verify-observations-v2)]
                         (let [v (f record expected jobs)]
                           {:status (:status v) :kind (:kind v)
                            :updater-observed (get-in v [:observations
                                                        ["M-aif-policy-conditioned-eig" :hole/h6378c65a4012]
                                                        :artifact-observation :observed])}))}))
              [false true])}))
(shutdown-agents)
