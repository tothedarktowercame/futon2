(ns futon2.aif.strategic-habit-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.habit-prior :as tactical]
            [futon2.aif.strategic-habit :as habit]
            [futon2.aif.trace :as trace]))

(def decision
  {:selection-boundary :reason-bearing-strategic-policy
   :selected-policy-id "pi-s-9dbc2ceb3317bc38050c41ce"
   :action {:type :advance-mission :target "M-aif-policy-conditioned-eig"}})
(def captured "2026-09-09T17:00:00Z")

(deftest explicit-flag-test
  (is (false? (habit/enabled? {} nil)))
  (is (true? (habit/enabled? {} "1")))
  (is (false? (habit/enabled? {:accumulate-strategic-habit? false} "1")))
  (is (true? (habit/enabled? {:accumulate-strategic-habit? true} nil))))

(deftest legacy-and-persistence-test
  (let [old (tactical/observe-action (tactical/initial-state) (:action decision))
        output {:habit-prior-state old :decision decision}
        empty-side (habit/load-state (:strategic-habit-state output))
        off (habit/carry nil decision "tick-1" captured false)
        on (habit/carry nil decision "tick-1" captured true)
        legacy-trace (trace/trace-record output)
        new-trace (trace/trace-record (assoc output :strategic-habit-state on))]
    (is (= old (tactical/coerce-state old)))
    (is (= :forward-accumulation-not-started (:empty-reason empty-side)))
    (is (= :empty (:status empty-side)))
    (is (nil? off))
    (is (not (contains? legacy-trace :strategic-habit-state)))
    (is (= (pr-str old) (pr-str (:habit-prior-state legacy-trace))
           (pr-str (:habit-prior-state new-trace))))
    (is (= (dissoc legacy-trace :timestamp)
           (dissoc new-trace :timestamp :strategic-habit-state)))
    (is (= on (:strategic-habit-state new-trace)))
    (is (= 2 (:version on)))
    (is (= :strategic (:grain on)))
    (is (= 1.0 (:alpha on)))
    (is (= captured (:captured-at on)))
    (is (= {(:selected-policy-id decision) 1} (:counts on)))
    (is (= :strategic (get-in on [:events "tick-1" :grain])))
    (is (= on (habit/carry on nil nil nil false)))
    (is (= on (habit/carry on decision "tick-1" captured true)))
    (is (= 2 (get-in (habit/carry on decision "tick-2" captured true)
                     [:counts (:selected-policy-id decision)])))))

(deftest refuses-insufficient-or-wrong-grain-test
  (doseq [d [(dissoc decision :selected-policy-id)
             (assoc decision :selected-policy-id "")
             (assoc decision :selection-boundary :actuation)]]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"insufficient"
                         (habit/carry nil d "tick-1" captured true))))
  (is (thrown? clojure.lang.ExceptionInfo
               (habit/load-state (tactical/initial-state))))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"nonempty"
                       (habit/require-promotable nil)))
  (let [state (habit/carry nil decision "tick-1" captured true)]
    (is (= state (habit/require-promotable state)))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"conflicting"
                         (habit/carry state (assoc decision :selected-policy-id "pi-s-other")
                                      "tick-1" captured true)))))
