(require '[futon2.aif.interpretation-request :as request]
         '[futon2.aif.interpretation-evidence :as evidence]
         '[futon2.aif.close-retention :as retention]
         '[clojure.java.io :as io])
(import '[java.nio.file Files] '[java.nio.file.attribute FileAttribute]
        '[java.time Instant] '[java.util UUID])

(defn smoke [type target]
  (let [root (.toFile (Files/createTempDirectory "codex27-p2-real-" (make-array FileAttribute 0)))]
    (try
      (let [dir (io/file root "cohort" "attempt-001")
            _ (.mkdirs dir)
            start (pr-str {:attempt/id "attempt-001" :cohort/id :cohort
                           :payload {:judgment {:semantic-epoch :real-smoke}}})
            _ (spit (io/file dir "001-time-step.edn") start)
            action {:type type :target target}
            identity {:occurrence (retention/mint-occurrence
                                   {:run-id (str (UUID/randomUUID)) :cohort-id "cohort" :attempt-id "attempt-001"
                                    :selected-action action :now #(Instant/now) :uuid-fn #(UUID/randomUUID)})
                      :semantic-epoch :real-smoke :data-root (.getCanonicalPath root)
                      :start-event-sha256 (evidence/sha256 (.getBytes start "UTF-8"))
                      :interpreter-job {:status :none :reason :not-dispatched}
                      :author "codex-27-smoke" :schema-version 1}
            started (System/currentTimeMillis)
            result (try {:request (request/prepare! action identity)}
                        (catch clojure.lang.ExceptionInfo e {:failure (ex-data e)}))
            r (or (:request result) (get-in result [:failure :request]))
            runs (get-in r [:retrieval :runs])
            qualified? (and (= 2 (count runs))
                            (every? #(and (empty? (:failures %)) (seq (:candidates %))
                                          (every? (fn [c] (boolean (re-matches #"[^/]+/[^/]+" (:pattern c)))) (:candidates %))) runs))]
        {:target target :elapsed-ms (- (System/currentTimeMillis) started)
         :qualified? (boolean qualified?) :captured-bytes (:captured-bytes r)
         :tension-rule (get-in r [:target :tension-rule])
         :citations (mapv :lines (get-in r [:target :citations]))
         :query-chars (count (get-in r [:retrieval :query]))
         :temp-root (str root) :failure (dissoc (:failure result) :request)
         :runs (mapv #(select-keys % [:retriever :failures :row-failures :candidates]) runs)})
      (finally
        (doseq [file (reverse (file-seq root))] (Files/delete (.toPath file)))))))

(let [results (mapv (fn [[type target]] (smoke type target))
                    [[:advance-mission "M-zaif-harness-v1"]
                     [:advance-ticket "T-fail-agent-not-found"]])]
  (doseq [result results] (prn (assoc result :temp-root-cleaned? (not (.exists (io/file (:temp-root result)))))))
  (shutdown-agents)
  (System/exit (if (every? :qualified? results) 0 1)))
