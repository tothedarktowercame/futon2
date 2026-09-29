(ns futon2.aif.scan-bins-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.scan-bins :as scan-bins]))

(deftest unit-five-bin-boundaries
  (testing "every interval and exact edge"
    (is (= [0 0 1 1 2 2 3 3 4 4 4]
           (mapv #(get (scan-bins/bin scan-bins/unit-5-v1 %) :bin)
                 [0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 1.0]))))
  (is (= {:schema :wm/scan-bins-unit-5-v1 :bin :underflow :value -0.01}
         (scan-bins/bin scan-bins/unit-5-v1 -0.01)))
  (is (= {:schema :wm/scan-bins-unit-5-v1 :bin :overflow :value 1.01}
         (scan-bins/bin scan-bins/unit-5-v1 1.01))))

(deftest non-finite-values-are-refused
  (doseq [x [nil ##NaN ##Inf ##-Inf "0.5"]]
    (let [result (scan-bins/bin scan-bins/unit-5-v1 x)]
      (is (= :refused (:status result)))
      (is (= :not-a-finite-number (:reason result)))
      (is (= (pr-str x) (:value result))))))
