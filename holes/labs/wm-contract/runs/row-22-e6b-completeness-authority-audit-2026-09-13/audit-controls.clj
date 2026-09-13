(require '[futon2.aif.machine-slow-feedback-completeness :as completeness])
(require '[futon2.aif.machine-slow-feedback-completeness-test :as fixtures])
(require '[futon2.aif.machine-slow-feedback-store-v2 :as store])

(let [{:keys [store config]} (#'fixtures/fixture)]
  (try
    (let [execution (:record (#'completeness/resolve-role
                              :review-execution (get-in config [:roles :review-execution])))
          review (:record (#'completeness/resolve-role
                           :review-artifact (get-in config [:roles :review-artifact])))]
      (println {:control :review-after-terminal-execution
                :execution/finished-at (:finished-at execution)
                :review/reviewed-at (:reviewed-at review)
                :accepted-result (select-keys (completeness/validate config)
                                              [:status :authority/status])}))
    (let [fake-job "unresolved-fake-job" fake-trace "unresolved-fake-trace"
          changed (reduce
                   (fn [c role]
                     (#'fixtures/replace-record
                      c role (fn [x] (assoc x :job/id fake-job :trace/id fake-trace))))
                   config [:review-execution :review-artifact :acceptance])]
      (println {:control :unresolved-job-trace-labels
                :job/id fake-job :trace/id fake-trace
                :accepted-result (select-keys (completeness/validate changed)
                                              [:status :authority/status])}))
    (finally (store/release! store))))
