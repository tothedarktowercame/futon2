(ns futon2.aif.f42b-certificate-carrier-identity-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.selected-outer-cascade-policy :as selected]
            [futon2.aif.selected-outer-cascade-policy-test]))

(defn- fixture-var [sym]
  (var-get (ns-resolve 'futon2.aif.selected-outer-cascade-policy-test sym)))

(deftest certificate-carries-policy-identity-not-the-sealed-carrier
  (let [c (:carrier ((fixture-var 'production-carriers)))
        d (selected/decision c)
        certificate (get-in d [:decision :selection-certificate :selected-outer-cascade-policy])
        entry (#'runner/selected-entry {:decision (:decision d)})]
    (is (= (:carrier-pin c) (:carrier-pin certificate)))
    (is (= (:selected-entry c) (:selected-entry certificate)))
    (is (nil? (get-in certificate [:sealed-menu :entries])))
    (is (= (:menu-pin c) (get-in certificate [:sealed-menu :menu-pin])))
    (is (= (:action (:selected-entry c)) (:action entry)))
    (is (some? (:canonical-cascade entry)))
    (is (= :selected (:status d)))
    (is (= [(:selected-entry c)] (:ranked d)))
    (is (some? (selected/verify certificate)))
    (is (nil? (selected/verify c)))))
