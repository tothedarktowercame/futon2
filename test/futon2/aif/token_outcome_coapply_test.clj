(ns futon2.aif.token-outcome-coapply-test
  "Co-application precedence shape through freeze-prediction/compare-outcomes.
  The {:co-apply {:units … :descent … :patterns …}} map is the shape
  cascade-model-manifest/push-forward already evaluates (Lean coApplyKernel);
  token-outcome's old (vector? …) guard was the sole blocker. These tests pin
  the widened acceptance, the malformed rejections, and the verdict chain."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.token-outcome :as outcome]))

(def target :T-coapply-test)
(defn q [t] [target t])

(defn pattern
  "An interpreted pattern in the shape guard-holds?/produces-of consume."
  [id needs produces & [theta]]
  (cond-> {:id id
           :guard {:status :interpreted :operator :and
                   :clauses [{:present (set (map q needs)) :absent #{}}]}
           :produces (set (map q produces))}
    theta (assoc :theta theta)))

;; u1 and u2 are both enabled at the empty state and both sit above u3 in
;; the descent order, so the enabled frontier is [u1 u2] — a genuine
;; co-application (containment order is not a chain; the cascade kernel's
;; first-enabled semantics would fire only one of them).
(def u1 (pattern :u1 [] [:alpha]))
(def u2 (pattern :u2 [] [:beta]))
(def u3 (pattern :u3 [(keyword (name :alpha))] [:gamma]))
(def co-apply
  {:co-apply {:units [:u1 :u2 :u3]
              :descent [[:u1 :u3] [:u2 :u3]]
              :patterns {:u1 u1 :u2 u2 :u3 u3}}})

(defn decision
  ([] (decision co-apply))
  ([precedence]
   {:action {:kind :cascade-candidate :id :C1 :target target
             :precedence precedence}
    :selection-certificate
    {:precision-family {:model {:q0 {#{} 1} :horizon 1}}
     :token-belief-stage
     {:domain-inputs [{:target target
                       :declaration {:want #{:alpha :beta :gamma}}}]}}}))

(defn rows [sha & [observed]]
  (mapv (fn [t] {:token (q t)
                 :result {:observed (if (nil? observed) false observed)
                          :evidence {:resolved-sha sha}}})
        [:alpha :beta :gamma]))

(deftest coapply-frontier-freezes-with-fractional-mass
  (let [d (assoc-in (decision) [:action :precedence]
                    {:co-apply {:units [:u1 :u2 :u3]
                                :descent [[:u1 :u3] [:u2 :u3]]
                                :patterns {:u1 (assoc u1 :theta 1/2)
                                           :u2 (assoc u2 :theta 1/2)
                                           :u3 u3}}})
        p (outcome/freeze-prediction d)]
    (is (= :frozen (:status p)))
    (is (= :co-apply-map (:precedence-kind p)))
    (is (= #{(q :alpha) (q :beta) (q :gamma)} (:intended-outputs p)))
    ;; frontier {u1,u2} each theta 1/2: states ∅, {a}, {b}, {a b} at 1/4 each,
    ;; so each frontier token carries marginal mass 1/2 — the co-apply kernel,
    ;; not the first-enabled union a chain would produce.
    (is (= {(q :alpha) 1/2 (q :beta) 1/2 (q :gamma) 0}
           (into {} (map (juxt :token :predicted)) (:wanted p))))
    (is (= [:co-application]
           (distinct (map :kernel-kind
                          (get-in p [:rollout :evaluations 0 :states])))))))

(deftest coapply-intended-outputs-accept-nested-transition-produces
  (let [nested (assoc u2 :produces nil
                      :transition {:status :interpreted :operator :union
                                   :produces #{(q :beta)}})
        p (outcome/freeze-prediction
           (decision {:co-apply {:units [:u2]
                                 :descent []
                                 :patterns {:u2 nested}}}))] 
    (is (= :frozen (:status p)))
    (is (= #{(q :beta)} (:intended-outputs p)))))

(deftest coapply-verdicts-through-compare-outcomes
  (let [p (outcome/freeze-prediction (decision))
        yes (outcome/compare-outcomes p (rows "sha" true) "sha")
        no (outcome/compare-outcomes p (rows "sha" false) "sha")
        verdicts #(into {} (map (juxt :token :verdict)) (:tokens %))]
    (is (= :compared (:status yes)))
    (is (= :compared (:status no)))
    ;; theta defaults to 1: both frontier tokens predicted 1; gamma unreachable
    ;; at horizon 1 from the empty state.
    (is (= {(q :alpha) :predicted-and-observed
            (q :beta) :predicted-and-observed
            (q :gamma) :not-predicted-observed}
           (verdicts yes)))
    (is (= {(q :alpha) :predicted-not-observed
            (q :beta) :predicted-not-observed
            (q :gamma) :neither}
           (verdicts no)))))

(deftest malformed-coapply-shapes-refuse-with-typed-finding
  (doseq [[label precedence]
          [[:string "not a step"]
           [:extraneous-keys (assoc co-apply :extra 1)]
           [:empty-units {:co-apply {:units [] :descent [] :patterns {}}}]
           [:duplicate-units {:co-apply {:units [:u1 :u1] :descent []
                                         :patterns {:u1 u1}}}]
           [:descent-arity {:co-apply {:units [:u1] :descent [[:u1]]
                                       :patterns {:u1 u1}}}]
           [:descent-unknown-unit {:co-apply {:units [:u1] :descent [[:u1 :nope]]
                                              :patterns {:u1 u1}}}]
           [:patterns-missing-unit {:co-apply {:units [:u1 :u2] :descent []
                                               :patterns {:u1 u1}}}]
           [:pattern-not-a-map {:co-apply {:units [:u1] :descent []
                                           :patterns {:u1 "pattern"}}}]]]
    (testing (str label)
      (let [p (outcome/freeze-prediction (decision precedence))]
        (is (= :refused (:status p)))
        (is (= :prediction-input-unavailable (:reason p)))
        (is (contains? p :precedence-finding))
        (is (#{:malformed-precedence-step :malformed-co-apply-step}
             (:kind (:precedence-finding p))))))))

(deftest semantic-refusals-come-from-the-model
  ;; Real model validation, not shape validation: an uninterpreted pattern in
  ;; a unit structurally accepted by co-apply-input still refuses, via the
  ;; model's own missing-interpretation check (interpret_eq_none_iff).
  (let [uninterpreted {:id :u1 :guard {:status :missing
                                       :kind :missing-pattern-interpretation}}
        p (outcome/freeze-prediction
           (decision {:co-apply {:units [:u1] :descent []
                                 :patterns {:u1 uninterpreted}}}))]
    (is (= :refused (:status p)))
    (is (= :model-rollout-refused (:reason p)))
    (is (= :missing-pattern-interpretation (get-in p [:finding :kind])))))

(deftest legacy-vector-path-unchanged
  (let [p (outcome/freeze-prediction
           (decision [u1 (assoc u3 :guard {:status :missing})]))]
    (is (= :refused (:status p)))
    (is (= :model-rollout-refused (:reason p)))
    (is (nil? (:precedence-finding p)))))

  (let [p (outcome/freeze-prediction
           (decision {:not-a-precedence true}))]
    (is (= :refused (:status p)))
    (is (= :prediction-input-unavailable (:reason p)))
    (is (#{:malformed-precedence-step :malformed-co-apply-step}
         (:kind (:precedence-finding p)))))

  (let [p (outcome/freeze-prediction (decision [u1 u2 u3]))]
    (is (= :frozen (:status p)))
    (is (= :precedence-vector (:precedence-kind p)))
    (is (= #{(q :alpha) (q :beta) (q :gamma)} (:intended-outputs p)))
    ;; First-enabled: only u1 fires at the empty state (u2 is shadowed behind
    ;; it in the precedence list), unlike the co-apply frontier where both
    ;; fire independently — the two kernels stay distinguishable.
    (is (= {:pattern-kernel 1}
           (frequencies (map :kernel-kind
                             (get-in p [:rollout :evaluations 0 :states])))))
    (is (= 0 (:predicted (first (filter #(= (q :beta) (:token %)) (:wanted p)))))
        "u2 shadowed behind u1 in first-enabled order leaves beta unpredicted at horizon 1"))

  (testing "frozen vector prediction keeps its historical key set"
    (let [p (outcome/freeze-prediction (decision [u1]))]
      (is (= #{:schema :target :action :model-id :prediction-rule :status
               :initial-belief :horizon :model :rollout :precedence-kind
               :observation-locators :intended-outputs :wanted}
             (set (keys p))))))
