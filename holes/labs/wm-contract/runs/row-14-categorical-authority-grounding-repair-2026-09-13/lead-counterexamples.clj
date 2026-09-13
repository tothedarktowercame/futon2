(require '[futon2.aif.categorical-state-observation :as sut]
         '[futon2.aif.categorical-state-observation-test :as fixture])
(let [{:keys [candidate authority dir records]} (fixture/fixture)
      changed (fixture/write-record! dir "replacement.edn"
                (assoc (fixture/evidence-claim :support-gained)
                       :payload {:kind :operator-note :fact "different evidence"}))
      updated (assoc records [:evidence :evidence/claim-a] changed)
      result (sut/validate-observation! candidate
               (assoc authority :resolver #(get updated [%1 %2])))]
  (assert (= :qualified (:status result)))
  (prn {:control :changed-evidence-same-reference-old-review
        :observed (:status result)
        :old-review-unchanged true
        :evidence-pin-changed (not= changed (get records [:evidence :evidence/claim-a]))}))
(let [{:keys [candidate authority dir records]} (fixture/fixture)
      claim (fixture/write-record! dir "test-claim.edn"
              (assoc (fixture/evidence-claim :support-gained) :authority/scope :test))
      observer (fixture/write-record! dir "production-observer.edn"
                 {:schema sut/authority-schema :role :categorical-state-observer
                  :principal/id "observer/test-a" :authority/scope :production
                  :authority/provenance (get-in authority [:expected :authority/provenance])})
      review (fixture/write-record! dir "production-review.edn"
               (assoc (fixture/review-for candidate "observer/test-a")
                      :authority/scope :production))
      updated (assoc records [:evidence :evidence/claim-a] claim
                             [:observer :authority/observer-a] observer
                             [:review :authority/review-a] review)
      result (sut/validate-observation! candidate
               (-> authority
                   (assoc-in [:expected :authority/scope] :production)
                   (assoc :resolver #(get updated [%1 %2]))))]
  (assert (= :qualified (:status result)))
  (prn {:control :test-claim-in-production-envelope
        :observed (:status result) :envelope-scope (:authority/scope result)}))
