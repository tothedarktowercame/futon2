(ns futon2.aif.beta-habit-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.beta-habit :as habit]
            [futon2.aif.policy-precision :as precision]))

(def ranked
  [{:id :a :controller-score 0.2 :habit-prior-bias -2.3 :habit-prior-source :learned-frequency}
   {:id :b :controller-score 1.1 :habit-prior-bias -0.1 :habit-prior-source :learned-frequency}])
(def readback {"rank/2" {:candidate-identity :a :status :present :value 0.8}
               "rank/1" {:candidate-identity :b :status :present :value 0.1}
               "rank/3" {:candidate-identity :gone :status :absent :reason :gone}})
(def opts {:identity-fn :id :score-fn :controller-score})

(deftest opt-precedence
  (is (false? (habit/enabled? {} (constantly nil) nil)))
  (is (true? (habit/enabled? {} (constantly {:beta-habit-in-both? true}) nil)))
  (is (true? (habit/enabled? {} (constantly nil) "1")))
  (is (false? (habit/enabled? {:beta-habit-in-both? false}
                             #(throw (Exception. "must not read file")) "1")))
  (is (false? (habit/enabled? {} (constantly {:beta-habit-in-both? false}) "1")))
  (is (thrown? clojure.lang.ExceptionInfo
               (habit/enabled? {:beta-habit-in-both? :yes} (constantly nil) nil))))

(deftest alignment-and-distinguishable-arms
  (let [state (habit/carry nil readback ranked opts)
        direct (precision/converge-beta 1.0 [1.1 0.2] [0.1 0.8]
                                         {:log-prior-placement :both :log-priors [-0.1 -2.3]})
        pi-only (precision/converge-beta 1.0 [1.1 0.2] [0.1 0.8]
                                          {:log-prior-placement :pi :log-priors [-0.1 -2.3]})]
    (is (= (:beta state) (:beta-posterior direct)))
    (is (> (Math/abs (- (:beta state) (:beta-posterior pi-only))) 1.0e-4))
    (is (= [:b :a] (mapv :identity (get-in state [:habit-provenance :candidates]))))
    (is (= [-0.1 -2.3] (mapv :ln-e (get-in state [:habit-provenance :candidates]))))
    (is (= :both (get-in state [:habit-provenance :placement])))
    (is (= 1 (:f-pi-absent-count state)))
    (is (= :policy-precision-beta-carry (get-in state [:habit-provenance :boundary])))))

(deftest missing-bias-refuses-and-missing-evidence-holds
  (is (thrown? clojure.lang.ExceptionInfo
               (habit/carry nil readback [(dissoc (first ranked) :habit-prior-bias)
                                          (second ranked)] opts)))
  (let [state (habit/carry nil {} ranked opts)]
    (is (= :absent (:status state)))
    (is (= :empty-f-pi-readback (:reason state)))
    (is (= [] (get-in state [:habit-provenance :candidates])))))

(deftest producer-chain-required
  (is (nil? (habit/preconditions! false false false false)))
  (is (nil? (habit/preconditions! true true true true)))
  (doseq [flags [[false true true] [true false true] [true true false]]]
    (is (thrown? clojure.lang.ExceptionInfo
                 (apply habit/preconditions! true flags)))))
