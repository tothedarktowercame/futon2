(ns futon2.aif.enact-failure-test
  (:require [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.enact :as enact]
            [futon2.aif.fold-realized :as fr]))

(defn- with-gate-builder [f thunk]
  (with-redefs-fn {(ns-resolve 'futon2.aif.enact 'act-gates-with-shown) f}
    thunk))

(defn- with-engine-wiring [f thunk]
  (with-redefs-fn {(ns-resolve 'futon2.aif.enact 'engine-wiring) f}
    thunk))

(def passed-entry
  {:mission :test-mission
   :shown [:test-pattern]
   :act-gate {:coverage-score-delta -0.25
              :coverage-score/source :fold
              :fold {:coverage-score-delta -0.25}}})

(deftest enactment-result-is-a-three-way-tagged-union
  (let [constructed (with-engine-wiring
                      (constantly {:status :constructed-wiring
                                   :wiring {:boxes [{:id :one}] :policy-holes []}})
                      #(enact/enact! passed-entry))
        no-construction (with-engine-wiring
                          (constantly {:status :no-construction
                                       :reason :engine-returned-no-wiring})
                          #(enact/enact! passed-entry))]
    (is (= :constructed-wiring (get-in constructed [:enactment :status])))
    (is (= {:boxes 1 :policy-holes 0}
           (get-in constructed [:enactment :result :constructed-wiring])))
    (is (= :no-construction (get-in no-construction [:enactment :status])))
    (is (= {:reason :engine-returned-no-wiring}
           (get-in no-construction [:enactment :result :no-construction])))))

(deftest engine-throw-is-not-reported-as-no-construction
  (let [record (with-redefs [shell/sh
                             (fn [& _]
                               (throw (IllegalStateException.
                                       "forced engine process failure")))
                             fr/with-realized-outcome
                             (fn [judgement _ _ _] judgement)]
                 (with-gate-builder
                   (constantly [(assoc passed-entry :verdict :pass)])
                   #(enact/close-loop! {:ranked-actions [:forced]} 43)))]
    (is (= :enactment-failed (get-in record [:enactment :status])))
    (is (nil? (get-in record [:enactment :result :no-construction])))
    (is (= :engine-execution
           (get-in record [:enactment :enactment-failed :phase])))
    (is (= "java.lang.IllegalStateException"
           (get-in record [:enactment :enactment-failed :exception-class])))
    (is (= "forced engine process failure"
           (get-in record [:enactment :enactment-failed :message])))))

(deftest close-loop-preserves-enactment-failure-as-a-variant
  (let [judgement {:ranked-actions [] :existing-evidence :preserved}
        clean-no-op (with-gate-builder (constantly [])
                      #(enact/close-loop! judgement 42))
        failure (with-gate-builder
                  (fn [_] (throw (IllegalStateException. "forced enactment-path failure")))
                  #(enact/close-loop! judgement 42))]
    (testing "failure is not byte-identical to an unattempted/clean no-op"
      (is (not= judgement failure))
      (is (not= clean-no-op failure)))
    (testing "the record retains a typed, inspectable failure"
      (is (= :enactment-failed (get-in failure [:enactment :status])))
      (is (= :act-gates (get-in failure [:enactment :enactment-failed :phase])))
      (is (= "java.lang.IllegalStateException"
             (get-in failure [:enactment :enactment-failed :exception-class])))
      (is (= "forced enactment-path failure"
             (get-in failure [:enactment :enactment-failed :message]))))
    (is (= [] (:act-gate-verdicts clean-no-op))
        "a clean no-pass result remains distinguishable and unchanged")))
