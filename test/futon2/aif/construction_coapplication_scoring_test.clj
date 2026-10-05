(ns futon2.aif.construction-coapplication-scoring-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.construction :as construction]
            [futon2.aif.wm.cascade-decision :as cascade-decision]))

(defn pattern [needs produces theta]
  {:guard {:needs (set needs) :forbids #{}}
   :produces (set produces)
   :theta theta})

(defn candidate [interpretations precedence]
  (let [plain {:precedence precedence
               :patterns (mapv #(assoc (interpretations %) :id %) precedence)}]
    {:precedence precedence
     :order (construction/containment-order plain)}))

(defn problem [interpretations wants horizon]
  {:facts (zipmap wants (repeat false))
   :want (vec wants)
   :interpretations interpretations
   :repository {:patterns (set (keys interpretations)) :stands-on #{}}
   :horizon-steps horizon
   ;; Construction's sparse preference is uniform over the target wants.
   :cascade-spec {:want (set wants)
                  :weights (zipmap wants (repeat (/ 1 (count wants))))
                  :lam 1 :mu 0}})

(defn score [p c]
  (cascade-decision/constructed-candidate-g p c))

(defn lane-for [p c]
  (cascade-decision/cascade-lane
   (assoc p
          :precedences [(:precedence c)]
          :candidate-actions
          [{:precedence (:precedence c)
            :construction-receipt {:order (:order c)}}])
   {:through :R5}))

(deftest construction-scoring-receives-chain-and-frontier-orders
  (let [independent {:p (pattern [] [:w1] 1)
                     :q (pattern [] [:w2] 1)
                     :r (pattern [] [:w3] 1)}
        chain {:p (pattern [] [:w1] 1)
               :q (pattern [:w1] [:w2] 1)
               :r (pattern [:w2] [:w3] 1)}
        ids [:p :q :r]
        independent-candidate (candidate independent ids)
        independent-lane (lane-for (problem independent [:w1 :w2 :w3] 1)
                                   independent-candidate)
        cascade-id (:id (first (filter #(= ids (mapv :id (:precedence %)))
                                       (:candidates independent-lane))))
        prediction (:prediction (first (filter #(= cascade-id (:cascade-id %))
                                               (:predictions independent-lane))))]
    (is (= {:order :co-application}
           (:order-use (score (problem independent [:w1 :w2 :w3] 1)
                              independent-candidate))))
    (is (= {:order :co-application}
           (get-in prediction [:trajectory 0 :order-use])))
    (is (= {#{:w1 :w2 :w3} 1}
           (:next-token-state (first (:trajectory prediction)))))
    (is (= {:order :chain}
           (:order-use (score (problem chain [:w1 :w2 :w3] 3)
                              (candidate chain ids)))))))

(deftest half-theta-frontier-and-list-have-different-construction-g
  (let [interpretations {:p (pattern [] [:w1] 1/2)
                         :q (pattern [] [:w2] 1/2)
                         :r (pattern [] [:w3] 1/2)}
        p (problem interpretations [:w1 :w2 :w3] 1)
        ordered (candidate interpretations [:p :q :r])
        co (score p ordered)
        list-score (score p (dissoc ordered :order))]
    (is (= {:order :co-application} (:order-use co)))
    (is (= {:order {:absent :no-order-on-receipt}} (:order-use list-score)))
    (is (< (Math/abs (- 1.6209167240682252 (:value co))) 1e-12))
    (is (< (Math/abs (- 2.2875833907348917 (:value list-score))) 1e-12))
    (is (not= (:value co) (:value list-score)))))

(deftest chain-g-is-the-list-g
  (let [interpretations {:p (pattern [] [:w1] 1/2)
                         :q (pattern [:w1] [:w2] 1/2)
                         :r (pattern [:w2] [:w3] 1/2)}
        p (problem interpretations [:w1 :w2 :w3] 3)
        ordered (candidate interpretations [:p :q :r])]
    (is (= (:value (score p ordered))
           (:value (score p (dissoc ordered :order)))))))

(deftest recorded-run-shape-differs-because-construction-scores-every-step
  ;; As in run 2026-10-05-c9d25d6a: three independent units, then one unit
  ;; needing the handoff product. Both kernels reach the same terminal state,
  ;; but construction's sparse token preference scores every horizon step,
  ;; unlike selection's terminal class schedule. Earlier reachability matters.
  (let [interpretations {:reader (pattern [] [:reader-want] 1)
                         :capture (pattern [] [:capture-want] 1)
                         :handoff (pattern [] [:handoff-want] 1)
                         :warrant (pattern [:handoff-want] [:warrant-want] 1)}
        wants [:reader-want :capture-want :handoff-want :warrant-want]
        ids [:reader :capture :handoff :warrant]
        p (problem interpretations wants 4)
        ordered (candidate interpretations ids)
        co (:value (score p ordered))
        list-score (:value (score p (dissoc ordered :order)))]
    (is (= 9.465030718061497 co))
    (is (= 10.715030718061497 list-score))
    (is (< co list-score))))
