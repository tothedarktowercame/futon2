(ns futon2.aif.reading-policy-key-sealing-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.policy-prefix-admission :as admission]
            [futon2.aif.cascade-shape-g :as shape-g]))

(def canonical-candidate
  {:target "M-a" :id "M-a/policy-1"
   :precedence {:co-apply {:units [["p" 0]]
                           :patterns {["p" 0] {:pattern-id "p"}}}}
   :arrangement {:nodes [{:occurrence-id ["p" 0] :pattern "p"}]
                 :edges [] :precedence [["p" 0]]}})

(deftest canonical-key-is-sealed-and-validated
  (let [sealed (admission/seal-reading-policy-key canonical-candidate)]
    (is (= :canonical (get-in sealed [:policy-key-carrier :status])))
    (is (= (:policy-key sealed) (get-in sealed [:policy-key-carrier :policy-key])))
    (is (= sealed (admission/validate-sealed-reading-policy-key! sealed)))))

(deftest synthetic-id-is-typed-noncanonical
  (let [sealed (admission/seal-reading-policy-key (assoc canonical-candidate :id "chain"))]
    (is (= {:status :policy-key-noncanonical
            :reason :policy-id-not-canonical-for-target}
           (:policy-key-carrier sealed)))
    (is (nil? (:policy-key sealed)))))

(deftest impossible-class-history-records-zero-posterior-weight
  (let [cascade {:nodes [{:pattern "p"}] :precedence ["p"] :edges []}
        candidate (shape-g/arranged->candidate "M-a" "M-a/policy-1" cascade)
        key (:policy-key candidate)
        source {:path "impossible-run"
                :record {:run/id "impossible-run"
                         :terminal {:schema :wm/q6-terminal-v1
                                    :choice {:status :present :policy-key key}}
                         :run-ending-classification {:class :related}}}
        score (shape-g/score-arranged
               "M-a" "M-a/policy-1" cascade nil
               {:class-free-energy {:window 1 :sources [source]}})]
    (is (= :refused (:status score)))
    (is (= :zero-posterior-mass (:kind score)))
    (is (= 0.0 (get-in score [:class-emission :posterior-weight])))
    (is (= :observation-impossible-under-policy
           (get-in score [:class-emission :reason :status])))
    (is (= :related
           (get-in score [:class-emission :reason :f :failure :class])))))
