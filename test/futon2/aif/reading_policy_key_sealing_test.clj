(ns futon2.aif.reading-policy-key-sealing-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.policy-prefix-admission :as admission]))

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
