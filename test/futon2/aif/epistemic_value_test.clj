(ns futon2.aif.epistemic-value-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.epistemic-value :as eig]))

(def prior {:left 0.5 :right 0.5})

(deftest perfect-binary-sensor-yields-one-bit-test
  (let [value (eig/expected-information-gain
               {:prior prior
                :predicted-observations {:see-left 0.5 :see-right 0.5}
                :posteriors {:see-left {:left 1.0 :right 0.0}
                             :see-right {:left 0.0 :right 1.0}}})]
    (is (< (Math/abs (- (Math/log 2.0) value)) 1.0e-12))))

(deftest uninformative-policy-has-zero-eig-test
  (is (< (Math/abs
          (eig/expected-information-gain
           {:prior prior
            :predicted-observations {:a 0.25 :b 0.75}
            :posteriors {:a prior :b prior}}))
         1.0e-12)))

(deftest eig-is-policy-conditioned-test
  (let [values
        (eig/policy-information-gains
         {:inspect {:prior prior
                    :predicted-observations {:l 0.5 :r 0.5}
                    :posteriors {:l {:left 1.0 :right 0.0}
                                 :r {:left 0.0 :right 1.0}}}
          :wait {:prior prior
                 :predicted-observations {:same 1.0}
                 :posteriors {:same prior}}})]
    (is (> (:inspect values) (:wait values)))))

(deftest incoherent-posterior-simulation-fails-closed-test
  (testing "posteriors cannot manufacture information without reconstructing the prior"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"does not reconstruct prior"
         (eig/expected-information-gain
          {:prior prior
           :predicted-observations {:only 1.0}
           :posteriors {:only {:left 0.9 :right 0.1}}})))))

(deftest missing-posterior-fails-closed-test
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo
       #"missing simulated posterior"
       (eig/expected-information-gain
        {:prior prior
         :predicted-observations {:a 0.5 :b 0.5}
         :posteriors {:a prior}}))))
