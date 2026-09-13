(require '[futon2.aif.machine-pre-enact-authorization :as e3]
         '[futon2.aif.machine-pre-enact-authorization-test :as f]
         '[futon2.aif.r9-checker :as r9])
(let [late (-> f/r9-input
               (assoc-in [:reviewer-job :finished-at] "2026-09-13T12:30:00Z")
               (assoc :admission-at "2026-09-13T12:31:00Z"))
      records {:pending f/pending
               :review (assoc f/review :r9/input late)
               :verdict (assoc f/verdict :canonical-admission (r9/check-independence late))}
      out (e3/verify-pre-enact (#'f/config records))]
  (assert (= :mechanism-authorized (:decision out)))
  (prn {:case :review-after-authorization :decision (:decision out)
        :authorization-at (:authorization-at f/pending)
        :actual-review-finished (get-in late [:reviewer-job :finished-at])}))
(let [records (into {} (map (fn [[k v]] [k (assoc v :run/id "different-run" :event/id "different-event")])
                            {:pending f/pending :review f/review :verdict f/verdict}))
      out (e3/verify-pre-enact (#'f/config records))]
  (assert (= :mechanism-authorized (:decision out)))
  (prn {:case :borrowed-canonical-event :decision (:decision out)
        :identity (:identity out) :canonical-input-unchanged true}))
