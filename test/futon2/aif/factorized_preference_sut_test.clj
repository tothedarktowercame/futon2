(ns futon2.aif.factorized-preference-sut-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-policy :as policy]
            [futon2.aif.factorized-preference-sut :as sut]))

(def calibration {:identity 101 :version 1 :authority 102 :registration-token 103
                  :focused-cost 0 :related-cost 1 :unrelated-cost 2})
(def carrier
  {:schema :wm/factorized-preference-v1
   :certificate {:calibration calibration :selector-authority 108
                 :factorization-identity 106 :factorization-version 1
                 :factorization-authority 107
                 :calibration-authorization 104 :factorization-authorization 105}
   :calibration-authorizations
   [{:receipt 104 :calibration calibration :authorized-by 102
     :implementation :futon2/ordinal-relevance-cost-v1
     :implementation-pin (sut/implementation-pin sut/calibration-implementation)}]
   :factorization-authorizations
   [{:receipt 105 :identity 106 :version 1 :authority 107
     :law :completion-then-conditional-relevance
     :implementation :futon2/completion-first-softmax-v1
     :implementation-pin (sut/implementation-pin sut/factorization-implementation)}]})
(def t1 ["M-1" :done])
(def t2 ["M-2" :done])
(def pattern1 (assoc (policy/token-interpretation
                      :p1 {:guard {:needs #{} :forbids #{}} :produces #{t1}})
                     :target "M-1"))
(def pattern2 (assoc (policy/token-interpretation
                      :p2 {:guard {:needs #{} :forbids #{}} :produces #{t2}})
                     :target "M-2"))
(def candidates [{:kind :cascade-candidate :id :pi1 :target "M-1" :precedence [pattern1]}
                 {:kind :cascade-candidate :id :pi2 :target "M-2" :precedence [pattern2]}])
(def policies [{:policy-id :pi1 :target "M-1" :completion :completed :relevance :focused
                :semantics :q9-factorized :horizon 1 :schedule :terminal}
               {:policy-id :pi2 :target "M-2" :completion :completed :relevance :unrelated
                :semantics :q9-factorized :horizon 1 :schedule :terminal}])
(def base {:mode :controlled-sut :carrier carrier :policies policies :candidates candidates
           :state {:cascade-belief {#{} 1}} :universe #{t1 t2} :acceptance #{t1 t2}
           :horizon 1 :schedule :terminal})

(deftest authorized-kernel-reaches-real-scorer
  (let [r (sut/compose-and-score! base)
        p (get-in r [:kernel :probabilities])]
    (is (= :scored (:status r)) (pr-str r))
    (is (= #{:pi1 :pi2} (set (get-in r [:posterior-grain :policy-identities]))))
    (is (> (:focused p) (:related p) (:unrelated p) (:stop-the-line p)))
    (is (every? #(= p (:distribution (last %))) (get-in r [:consumption :rows])))
    (is (sut/preferred? calibration {:completion :completed :relevance :unrelated}
                        {:completion :not-completed :relevance nil}))
    (is (not (sut/preferred? calibration {:completion :not-completed :relevance nil}
                             {:completion :completed :relevance :focused})))))

(deftest typed-outcomes-do-not-conflate-unmeasured
  (is (= :typed-outcome-invalid
         (:kind (sut/compose-and-score!
                 (assoc-in base [:policies 0 :relevance] nil)))))
  (is (= :typed-outcome-invalid
         (:kind (sut/compose-and-score!
                 (-> base (assoc-in [:policies 0 :completion] :not-completed)
                     (assoc-in [:policies 0 :relevance] :focused))))))
  (is (= :unmeasured-not-observed-noncompletion
         (:kind (sut/compose-and-score!
                 (-> base (assoc-in [:policies 0 :completion] :unmeasured)
                     (assoc-in [:policies 0 :relevance] nil)))))))

(deftest authorization-and-substitution-refusals
  (doseq [[x expected]
          [[(assoc-in base [:carrier :certificate :selector-authority] 102) :certificate-invalid]
           [(assoc-in base [:carrier :calibration-authorizations 0 :implementation-pin] "bad")
            :calibration-authorization-mismatch]
           [(assoc-in base [:carrier :factorization-authorizations 0 :implementation]
                      :caller/substitute) :factorization-authorization-mismatch]
           [(assoc-in base [:carrier :factorization-authorizations 0 :receipt] 999)
            :factorization-authorization-mismatch]
           [(assoc-in base [:carrier :certificate :calibration :focused-cost] 9)
            :certificate-invalid]]]
    (is (= expected (:kind (sut/compose-and-score! x)))))
  (is (= :flat-table-not-a-certificate
         (:kind (sut/compose-and-score! (assoc-in base [:carrier :weights]
                                                  {:focused 0.55})))))
  (is (= :flat-table-not-a-certificate
         (:kind (sut/compose-and-score! (assoc-in base [:carrier :computed-kernel]
                                                  {:focused 99})))))
  (is (= :production-registries-unconfigured
         (:kind (sut/compose-and-score! (assoc base :mode :production))))))

(deftest posterior-grain-and-population-refusals
  (doseq [x [(update base :candidates pop)
             (assoc-in base [:policies 1 :semantics] :other)
             (assoc-in base [:policies 1 :horizon] 2)
             (assoc-in base [:policies 1 :schedule] :other)]]
    (is (contains? #{:policy-population-mismatch :posterior-grain-mismatch}
                   (:kind (sut/compose-and-score! x))))))
