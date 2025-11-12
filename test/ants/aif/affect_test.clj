(ns ants.aif.affect-test
  (:require [clojure.test :refer [deftest is testing]]
            [ants.aif.affect :as affect]))

(def precision-config
  {:tau-floor 0.08
   :tau-cap 1.5
   :need-gain 0.6
   :dhdt-gain 0.8
   :hunger-thresh 0.45
   :ingest-thresh 0.60})

(deftest need-sensitive-precision-increases-tau
  (let [prec {:tau 0.2}
        state {:obs {:hunger 0.7 :ingest 0.1}
               :dhdt 0.05}
        updated (affect/update-tau prec state precision-config)
        expected (+ (:tau prec)
                    (* (:need-gain precision-config)
                       (affect/need-error state precision-config))
                    (* (:dhdt-gain precision-config)
                       (:dhdt state)))]
    (testing "tau increases at least by configured gains"
      (is (>= (:tau updated) (:tau prec))))
    (testing "delta matches gain-weighted need and trend"
      (is (< (Math/abs (- (:tau updated) expected)) 1e-6)))))
