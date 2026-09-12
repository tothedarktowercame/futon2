(ns row13-full-judge
  (:require [clojure.java.io :as io] [clojure.pprint :as pp]
            [futon2.aif.trace :as trace] [futon2.report.war-machine :as wm]))

(defn run-tick [dir id]
  (:judgement (wm/generate-war-machine
               1 (merge (wm/accumulation-config)
                        {:trace? true :trace-dir dir :run-id id
                         :include-advisory-lanes? false :step-portfolio? false
                         :step-mission-detail-portfolio? false
                         :eval-invariant-fallback? false}))))
(defn trace-file [dir] (first (filter #(.isFile %) (file-seq (io/file dir)))))
(defn overwrite-last! [dir f]
  (let [records (trace/read-trace :dir dir :date-str "2026-09-12")]
    (spit (trace-file dir) (apply str (map #(str (pr-str %) "\n")
                                           (conj (vec (butlast records)) (f (last records))))))))
(defn refusal [dir id]
  (let [before (some-> (trace-file dir) .length)]
    (try (run-tick dir id) {:refusal nil :appended? true}
         (catch clojure.lang.ExceptionInfo e
           {:refusal (:refusal (ex-data e))
            :appended? (not= before (some-> (trace-file dir) .length))}))))
(let [root (.toFile (java.nio.file.Files/createTempDirectory
                     "row13-full-judge-" (make-array java.nio.file.attribute.FileAttribute 0)))
      main (str root "/main") _ (.mkdirs (io/file main))
      ids ["row13-full-1" "row13-full-2" "row13-full-3"]
      _ (doseq [id ids] (run-tick main id))
      records (trace/read-trace :dir main :date-str "2026-09-12")
      copy-dir (fn [name]
                 (let [d (io/file root name)] (.mkdirs d)
                   (io/copy (trace-file main) (io/file d (.getName (trace-file main)))) (str d)))
      missing (copy-dir "missing") gap (copy-dir "gap") support (copy-dir "support")
      _ (overwrite-last! missing #(dissoc % :accumulation-state))
      _ (overwrite-last! gap #(assoc-in % [:accumulation-state :last-tick] "skipped"))
      _ (overwrite-last! support #(update-in % [:accumulation-state :support :state] pop))
      controls [(assoc (refusal missing "row13-control-missing") :expected :accumulation-migration-required)
                (assoc (refusal gap "row13-control-gap") :expected :carry-chain-gap)
                (assoc (refusal support "row13-control-support") :expected :support-mismatch)]
      entity (:accumulation-entity-id (wm/accumulation-config))
      checks (for [r records]
               {:tick-id (:run/id r)
                :previous-id (get-in r [:accumulation-update-input :previous-id])
                :input-observation-equal? (= (:observation r) (get-in r [:accumulation-update-input :observation]))
                :input-belief-equal? (= (get-in r [:mu-post entity])
                                        (get-in r [:accumulation-update-input :belief-post]))})
      result {:schema :wm/row13-full-judge-v1 :status :executed
              :full-judge-redirected-run true :chain checks :controls controls}]
  (when-not (and (= ids (mapv :tick-id checks))
                 (every? :input-observation-equal? checks) (every? :input-belief-equal? checks)
                 (every? #(and (= (:expected %) (:refusal %)) (false? (:appended? %))) controls))
    (throw (ex-info "Row 13 full judge evidence failed" result)))
  (prn result))
