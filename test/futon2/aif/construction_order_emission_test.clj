(ns futon2.aif.construction-order-emission-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-order :as cascade-order]
            [futon2.aif.construction :as structural]
            [futon2.aif.interpretation-construction :as construction]))

(defn pattern
  ([needs produces] (pattern needs [] produces))
  ([needs forbids produces]
   {:guard {:needs (set needs) :forbids (set forbids)}
    :produces (set produces)}))

(defn construct [patterns wants horizon]
  (construction/construct
   {:target "M-order-emission"
    :want wants
    :observation (zipmap (set (concat wants
                                     (mapcat #(get-in % [:guard :needs]) (vals patterns))
                                     (mapcat #(get-in % [:guard :forbids]) (vals patterns))))
                        (repeat false))
    :interpretations patterns
    :interpretation-receipts (zipmap (keys patterns) (repeat {:kind :test}))
    :budget {:max-moves 4 :max-expansions 100}
    :horizon horizon
    :move-cost 0
    :evaluate-g (fn [candidate]
                  {:value (if (seq (:precedence candidate)) 1 10)
                   :universe (vec wants)})}))

(defn emitted [patterns wants horizon]
  (first (:candidates (construct patterns wants horizon))))

(defn recomputed-order [patterns candidate]
  (structural/containment-order
   {:precedence (:precedence candidate)
    :patterns (mapv #(assoc (patterns %) :id %) (:precedence candidate))}))

(deftest independent-order-evidence-is-emitted
  (let [patterns {:p (pattern [] [:w1])
                  :q (pattern [] [:w2])
                  :r (pattern [] [:w3])}
        candidate (emitted patterns [:w1 :w2 :w3] 1)
        receipt (:construction-receipt candidate)]
    (is (= {:order :co-application} (:order-use receipt)))
    (is (= [] (get-in receipt [:order :descent])))
    (is (= [] (:frontier-conflicts receipt)))
    (is (= (:order receipt) (recomputed-order patterns candidate)))
    (is (= (:order-use receipt)
           (:meta (cascade-order/order-use candidate))))))

(deftest conflicting-frontier-is-emitted
  (let [patterns {:p (pattern [] [:b] [:a])
                  :q (pattern [] [:a] [:b])}
        receipt (:construction-receipt (emitted patterns [:a :b] 1))]
    (is (= [{:tau 1 :state #{} :frontier [:p :q]}]
           (:frontier-conflicts receipt)))))

(deftest emitted-order-is-the-structural-order-for-each-shape
  (doseq [[patterns wants horizon]
          [[{:p (pattern [] [:w1])
             :q (pattern [:w1] [:w2])
             :r (pattern [:w2] [:w3])}
            [:w1 :w2 :w3] 3]
           [{:p (pattern [] [:w1])
             :q (pattern [] [:w2])
             :r (pattern [] [:w3])}
            [:w1 :w2 :w3] 1]
           ;; Authored after run 2026-10-05-c9d25d6a's shape.
           [{:reader (pattern [] [:reader-want])
             :capture (pattern [] [:capture-want])
             :handoff (pattern [] [:handoff-want])
             :warrant (pattern [:handoff-want] [:warrant-want])}
            [:reader-want :capture-want :handoff-want :warrant-want] 2]]]
    (let [candidate (emitted patterns wants horizon)
          receipt (:construction-receipt candidate)]
      (is (= (:order receipt) (recomputed-order patterns candidate)))
      (is (= (:order-use receipt)
             (:meta (cascade-order/order-use candidate)))))))

(deftest nonfinite-left-out-retains-compiled-order-evidence
  (let [patterns {:p (pattern [] [:w1]) :q (pattern [] [:w2])}
        result (construction/construct
                {:target "M-left-out" :want [:w1 :w2]
                 :observation {:w1 false :w2 false}
                 :interpretations patterns
                 :interpretation-receipts {:p {:kind :test} :q {:kind :test}}
                 :budget {:max-moves 4 :max-expansions 100}
                 :horizon 1 :move-cost 0
                 :evaluate-g (fn [candidate]
                               (if (seq (:precedence candidate)) ##Inf 10))})
        left (first (:left-out result))]
    (is (= {:order :co-application} (:order-use left)))
    (is (= [] (get-in left [:order :descent])))
    (is (= [] (:frontier-conflicts left)))))
