(ns checks.preference-stack-binding-check
  (:require [clojure.edn :as edn]
            [futon2.aif.belief :as belief]
            [futon2.aif.efe :as efe]
            [futon2.aif.trace :as trace]
            [checks.preference-stack-witness-shape-check :as shape]))

(def fragment-path "checks/witness-fragments/preferenceStackLiveRecorded.edn")

(defn- fail! [finding data]
  (throw (ex-info (str "preference-stack-binding-check: " (name finding))
                  (assoc data :finding finding))))

(defn- read-witness! [binding mode]
  (let [path (get-in binding [:fixture :path])]
    (try
      (case mode
        :absent (edn/read-string (slurp (str path ".absent-control")))
        :malformed (edn/read-string "[")
        (edn/read-string (slurp path)))
      (catch Exception failure
        (fail! (if (= :malformed mode) :malformed-witness :absent-witness)
               {:path path :message (.getMessage failure)})))))

(defn- production-stack []
  (let [state {:observation {} :belief (belief/initial-belief-state [:m1])}
        scored (efe/compute-efe state {:type :no-op} {:goal-outcome-entries []})
        record (trace/trace-record
                {:belief (:belief state)
                 :observation (:observation state)
                 :free-energy {}
                 :ranked-actions [(assoc scored :rank 1)]
                 :decision {:action {:type :no-op}}
                 :mode :binding-check})]
    {:compute (:preference-stack scored)
     :trace-status (get-in record [:preference-stack :status])
     :trace (get-in record [:preference-stack :value])}))

(defn- check! [mode]
  (let [binding (edn/read-string (slurp fragment-path))
        witness (read-witness! binding mode)
        shape-report (shape/validate witness)
        {:keys [compute trace trace-status]} (production-stack)
        serialized (:preference-stack witness)]
    (when-not (= "preferenceStackLiveRecorded" (:witnesses binding))
      (fail! :wrong-binding {:actual (:witnesses binding)}))
    (when-not (= "holes/labs/wm-contract/PreferenceStackWitness.edn"
                 (get-in binding [:fixture :path]))
      (fail! :wrong-fixture {:fixture (:fixture binding)}))
    (when-not (:pass? shape-report)
      (fail! :shape-rejected shape-report))
    (when-not (= :present trace-status)
      (fail! :trace-did-not-record-stack {:status trace-status}))
    (when-not (= serialized compute trace)
      (fail! :production-artifact-divergence
             {:artifact-layers (count serialized)
              :compute-layers (count compute)
              :trace-layers (count trace)}))
    {:layers (count serialized) :trace-status trace-status
     :same-object-value true}))

(defn -main [& args]
  (let [negative (some->> args (drop-while #(not= "--negative" %)) second keyword)]
    (try
      (let [report (check! negative)]
        (if negative
          (do (println "preference-stack-binding-check: FAIL mutation slipped exit-convention=0-pass/1-fail/2-mutation-slipped"
                       (pr-str report))
              (System/exit 2))
          (println "preference-stack-binding-check: PASS exit-convention=0-pass/1-fail/2-mutation-slipped"
                   (pr-str report))))
      (catch Exception failure
        (if negative
          (do (println (str "preference-stack-binding-check: negative-control PASS ("
                            (name negative) " witness rejected) exit-convention=0-pass/1-fail/2-mutation-slipped")
                       (pr-str (ex-data failure)))
              (System/exit 0))
          (do (println (.getMessage failure) (pr-str (ex-data failure)))
              (System/exit 1)))))))
