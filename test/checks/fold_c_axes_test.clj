(ns checks.fold-c-axes-test
  (:require [clojure.test :refer [deftest is]]
            [checks.fold-c-axes :as axes]
            [futon2.aif.ruled-outcome-c :as ruled]))

(deftest every-active-axis-is-accounted-for
  (is (= {:preference-layer #{} :risk-contribution #{:ruled-outcome-c}}
         (axes/runtime-axes)))
  (doseq [axis [nil :unknown]]
    (with-redefs [ruled/fold-declaration [{:layer/id :test :folded? true
                                         :in-ruled-sum :yes :composition-axis axis}]]
      (is (thrown? clojure.lang.ExceptionInfo (axes/runtime-axes)))))
  (with-redefs [ruled/fold-declaration [{:layer/id :p :folded? true :in-ruled-sum :yes
                                       :composition-axis :preference-layer}]]
    (is (= #{:p} (:preference-layer (axes/runtime-axes))))))

(deftest missing-risk-cannot-pass
  (let [runtime {:risk-contribution #{:ruled-outcome-c}}
        fixture {:risk-contribution-ids [:ruled-outcome-c]}]
    (is (axes/risk-matches? runtime #{:ruled-outcome-c} fixture))
    (is (not (axes/risk-matches? runtime #{} fixture)))
    (is (not (axes/risk-matches? runtime #{:ruled-outcome-c} {:risk-contribution-ids []})))))
