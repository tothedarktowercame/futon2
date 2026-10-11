(ns futon2.aif.cascade-efe-sut-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.application-order-certificate-test :as order-fixture]
            [futon2.aif.cascade-efe-sut :as sut]
            [futon2.aif.factorized-preference-sut :as preference]
            [futon2.aif.factorized-preference-sut-test :as preference-fixture]
            [futon2.aif.policy :as policy]
            [futon2.aif.policy-prefix-admission :as admission]
            [futon2.aif.prefix-free-energy-sut-test :as prefix-fixture]
            [futon2.aif.three-halves-policy-test :as policy-fixture]
            [futon2.aif.wm.cascade-decision :as decision]))

(defn pinned [name declaration]
  (policy-fixture/with-pin name declaration))

(defn fixture-data []
  (let [{:keys [problem controlled]} (order-fixture/fixture-data)
        app-candidate (first (:constructed-candidates problem))
        token-candidate (assoc (get-in (decision/controlled-application-order-run
                                        controlled problem)
                                       [:lane :selected-action])
                               :id (:candidate-id app-candidate)
                               :target (:target problem))
        policy-row {:policy-id (:id token-candidate) :target (:target token-candidate)
                    :completion :completed :relevance :focused
                    :semantics :q9-factorized :horizon 3 :schedule :terminal}
        preference-input {:mode :controlled-sut :carrier preference-fixture/carrier
                          :policies [policy-row] :candidates [token-candidate]
                          :state {:cascade-belief {#{} 1}}
                          :universe #{["controlled-3halves" :q]
                                      ["controlled-3halves" :r]
                                      ["controlled-3halves" :w]}
                          :acceptance #{["controlled-3halves" :w]}
                          :horizon 3 :schedule :terminal}
        preference-preview (preference/compose-and-score! preference-input)
        key (admission/candidate-key token-candidate)
        ctx0 (prefix-fixture/authority-fixture)
        [prior prior-job] (prefix-fixture/prior key)
        prefix-context (update ctx0 :authority-jobs assoc (:job-id prior-job) prior-job)
        prefix-input {:candidates [token-candidate]
                      :conditioning-steps {:steps []}
                      :bootstrap-priors {key prior}
                      :scorer-authority "scorer"}
        transitions
        (into {} (for [[id interpretation] (get-in problem [:cascade-problem :interpretations])]
                   [id (pinned (str "transition-" (name id))
                               {:guard (:guard interpretation) :produces (:produces interpretation)
                                :kernel {#{} 1} :authority 711
                                :implementation-pin :sut/deterministic-token-transition-v1})]))
        initial (pinned "initial-kernel" {:kernel {#{} 1} :authority 712
                                           :implementation-pin :sut/initial-state-v1})
        observation (pinned "observation-kernel"
                            {:kernel (get-in preference-preview [:kernel :probabilities])
                             :authority 713 :implementation-pin :sut/q9-observation-v1})
        predictive0 {:nodes [:s0 :s1 :s2] :construction-edges []
                     :organizer-additions [[0 1] [1 2]]
                     :authorized-precedence [:P :Q :R] :horizon 3
                     :initial-state initial :observation observation
                     :transitions transitions :selector-authority 714}
        predictive (pinned "predictive-policy" predictive0)
        controlled (assoc controlled :captured-sources @policy-fixture/captured)]
    {:mode :controlled-sut :problem problem :order-control controlled
     :predictive predictive :preference-input preference-input
     :prefix-input prefix-input :prefix-context prefix-context :beta 1}))

(deftest complete-controlled-carrier-reaches-real-posterior-and-preparation
  (let [r (sut/compose! (fixture-data))]
    (is (= :prepared-for-enactment (:status r)) (pr-str r))
    (is (= [:P :Q :R] (get-in r [:structure :authorized-precedence])))
    (is (= 1 (count (set (vals (:identity-chain r))))))
    (is (= 1 (count (set (vals (:identity-digests r))))))
    (is (= :ready-for-posterior (get-in r [:prefix-f :status])))
    (is (seq (:efe-terms r)))
    (is (= [:P :Q :R] (get-in r [:order :dispatch :consumed-order])))))

(deftest predictive-carrier-refuses-at-first-mismatch
  (let [base (fixture-data)
        reseal (fn [name p] (pinned (str "predictive-resealed-" name) (dissoc p :source)))
        run (fn [p]
              (sut/compose! (-> base (assoc :predictive p)
                                (assoc-in [:order-control :captured-sources]
                                          @policy-fixture/captured))))
        transition (get-in base [:predictive :transitions :Q])]
    (doseq [[label changed expected]
            [[:missing-transition
              (reseal "missing" (update (:predictive base) :transitions dissoc :Q)) :transition-population]
             [:substituted-kernel
              (reseal "kernel" (assoc-in (:predictive base) [:transitions :Q :kernel] {#{} ##Inf}))
              :transition-kernel]
             [:horizon (reseal "horizon" (assoc (:predictive base) :horizon 2)) :horizon-mismatch]
             [:organizer-cycle
              (reseal "cycle" (assoc (:predictive base) :organizer-additions [[0 1] [1 0]]))
              :organizer-cycle]
             [:missing-initial (reseal "initial" (dissoc (:predictive base) :initial-state))
              :initial-state-kernel]]]
      (testing (name label)
        (is (= expected (:reason (run changed))))))
    (testing "unchanged transition pin detects substituted authority/bytes"
      (is (= :predictive-source-pin
             (:reason (sut/compose!
                       (assoc-in base [:predictive :transitions :Q :authority] 999))))))
    (is (map? transition))))

(deftest downstream-population-and-preference-fallbacks-refuse
  (let [base (fixture-data)]
    (is (= :factorized-preference-refused
           (:reason (sut/compose! (assoc-in base [:preference-input :carrier :weights]
                                            {:focused 0.55})))))
    (is (= :prefix-f-refused
           (:reason (sut/compose! (update-in base [:prefix-input :candidates] pop)))))
    (is (= :production-registries-unconfigured
           (:reason (sut/compose! (assoc base :mode :production)))))
    (testing "posterior identity substitution is caught before dispatch"
      (with-redefs [policy/select-action-cascades
                    (fn [_ _] {:action {:id :foreign :target "other"
                                        :precedence [{:id :X}]}})]
        (is (= :policy-identity-divergence (:reason (sut/compose! base))))))))
