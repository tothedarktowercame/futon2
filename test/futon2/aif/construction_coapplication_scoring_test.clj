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

;; Hand reference for this scorer at zero adjudication rates and certain
;; beliefs (cascade-model-manifest/horizon-g-sparse*): the step risk is
;; sum over wants of KL(Bern(q_v) || Bern(sigmoid(w_v))), which for a want
;; that is held is softplus(-w_v) and for one that is not is softplus(w_v).
(defn softplus [x] (Math/log (+ 1.0 (Math/exp x))))
(defn step-risk [weight wants-held wants-total]
  (+ (* wants-held (softplus (- weight)))
     (* (- wants-total wants-held) (softplus weight))))
(defn close? [a b] (< (Math/abs (- (double a) (double b))) 1e-12))

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

(deftest one-step-frontier-and-list-have-different-construction-g
  ;; Three independent producers, one step, weight 1/3 per want. The frontier
  ;; holds all three wants after the step; the list holds one.
  (let [interpretations {:p (pattern [] [:w1] 1)
                         :q (pattern [] [:w2] 1)
                         :r (pattern [] [:w3] 1)}
        p (problem interpretations [:w1 :w2 :w3] 1)
        ordered (candidate interpretations [:p :q :r])
        co (score p ordered)
        list-score (score p (dissoc ordered :order))]
    (is (= {:order :co-application} (:order-use co)))
    (is (= {:order {:absent :no-order-on-receipt}} (:order-use list-score)))
    (is (close? (step-risk 1/3 3 3) (:value co)))
    (is (close? (step-risk 1/3 1 3) (:value list-score)))
    (is (not= (:value co) (:value list-score)))))

(deftest the-construction-lane-does-not-carry-a-patterns-theta
  ;; Not a wanted behaviour, a recorded one (claude-2, 2026-10-05): the lane
  ;; rebuilds each pattern from its interpretation and the :theta is not among
  ;; the keys it keeps, so every pattern is scored as certain to succeed. A
  ;; learned success rate will not reach construction G until that changes;
  ;; this test will then fail and should be replaced by the theta-1/2 values.
  (let [with-theta (fn [theta] {:p (pattern [] [:w1] theta)
                                :q (pattern [] [:w2] theta)
                                :r (pattern [] [:w3] theta)})
        g (fn [theta] (let [ix (with-theta theta)]
                        (:value (score (problem ix [:w1 :w2 :w3] 1)
                                       (candidate ix [:p :q :r])))))]
    (is (= (g 1) (g 1/2)))))

(deftest a-conflicting-frontier-is-scored-without-refusal
  (let [interpretations {:p {:guard {:needs #{} :forbids #{:b}} :produces #{:a} :theta 1}
                         :q {:guard {:needs #{} :forbids #{:a}} :produces #{:b} :theta 1}}
        scored (score (problem interpretations [:a :b] 1)
                      (candidate interpretations [:p :q]))]
    (is (= {:order :co-application} (:order-use scored)))
    (is (close? (step-risk 1/2 2 2) (:value scored)))))

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
    ;; wants held after steps 1..4: frontier 3,4,4,4; list 1,2,3,4
    (is (close? (reduce + (map #(step-risk 1/4 % 4) [3 4 4 4])) co))
    (is (close? (reduce + (map #(step-risk 1/4 % 4) [1 2 3 4])) list-score))
    (is (close? 1.25 (- list-score co)) "five want-steps earlier, at weight 1/4 each")
    (is (< co list-score))))
