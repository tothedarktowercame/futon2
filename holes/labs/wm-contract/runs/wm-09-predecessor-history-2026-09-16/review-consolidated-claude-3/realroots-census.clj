;; claude-3: per-record discovery census at ab0b56f2 over a /tmp COPY of data/wm-full-loop*.
;; For each closed attempt: candidate construction, K2/K3 exclusion, or K5 relevance exclusion
;; for a fresh target that matches nothing; records every refusal reason instead of stopping at the first.
(require '[futon2.aif.receipt-construction :as construction] '[clojure.string :as str] '[clojure.java.io :as io])
(def base "/tmp/c3-wm09c/realcopy/")
(def candidate! @#'construction/closed-candidate!)
(def relevance! @#'construction/relevance-evidence!)
(def closes (sort (for [root (.listFiles (io/file base)) :when (.isDirectory root)
                        cohort (.listFiles root) :when (.isDirectory cohort)
                        attempt (.listFiles cohort) :when (.isDirectory attempt)
                        :let [f (io/file attempt "007-closed.edn")] :when (.exists f)] f)))
(def rows
  (for [f closes]
    (let [path (str/replace (str f) base "")]
      (try (let [c (candidate! f)]
             (if (:non-construction? c)
               {:path path :disposition :k2k3-excluded}
               (try (relevance! c)
                    {:path path :disposition :k5-excluded-for-fresh-target :target (:target c)}
                    (catch clojure.lang.ExceptionInfo e
                      {:path path :disposition :refuses-relevance :target (:target c)
                       :reason ((juxt :construction/refusal :reason) (ex-data e))}))))
           (catch clojure.lang.ExceptionInfo e
             {:path path :disposition :refuses-discovery :reason ((juxt :construction/refusal :reason) (ex-data e))})))))
(println "closes" (count rows))
(prn :dispositions (frequencies (map :disposition rows)))
(prn :refusal-reasons (frequencies (keep :reason rows)))
(doseq [r (filter :reason rows)] (prn r))
(shutdown-agents)
