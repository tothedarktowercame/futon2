(ns futon2.aif.parameter-delivery
  "Observation-bound parameter posterior and information-gain computation
   delivered to the epistemic consumer (DAG node WM-07-delivery).

   Information gain here is the Lean definition in
   DarkTower.WarMachine.Holes (parameterInformationGain /
   expectedInformationGain): the predictive-outcome expectation of the
   posterior-to-prior KL over the POSTERIOR's support, in nats. Lean's
   positivePrior hypothesis (every θ with positive posterior mass has
   positive prior mass) becomes the typed refusal
   :zero-prior-in-posterior-support, never a silent skip.

   The information gain is DELIVERED, never folded into G: an epistemic term
   inside G is a separate, unapproved change (SPEC-flat-removal S2 / D3
   amendment 4). g-with-information-gain returns :g exactly as the injected
   g-fn produced it."
  (:require [futon2.aif.machine-parameters :as machine-parameters]))

(def delivery-schema :wm/parameter-delivery-v1)

(defn- refused [kind path]
  {:status :refused :kind kind :path path})

(defn parameter-information-gain
  "Posterior-to-prior KL for one (policy, outcome), in nats, over the
   posterior kernel's support. θ with posterior mass 0 contribute 0 (the
   0·log 0 convention). Refuses with :zero-evidence-conditioning when the
   posterior entry is not :ok, and :zero-prior-in-posterior-support when some
   θ with positive posterior mass has zero prior mass (Lean positivePrior)."
  [kernels policy-id outcome]
  (let [posterior (get-in kernels [:posterior-kernel [policy-id outcome]])]
    (cond
      (not (:ok posterior))
      (refused :zero-evidence-conditioning [:posterior-kernel [policy-id outcome]])
      :else
      (let [prior (get-in kernels [:prior-kernel policy-id])
            mass (:mass posterior)
            zero-prior (seq (filter (fn [[theta p]]
                                      (and (pos? p) (not (pos? (get prior theta)))))
                                    mass))]
        (if zero-prior
          (refused :zero-prior-in-posterior-support
                   [:posterior-kernel [policy-id outcome] (ffirst zero-prior)])
          (double (reduce (fn [acc [theta p]]
                            (if (pos? p)
                              (+ acc (* p (Math/log (/ p (get prior theta)))))
                              acc))
                          0 mass)))))))

(defn expected-information-gain
  "Predictive-outcome expectation of parameter-information-gain for one
   policy. An outcome with zero predictive mass contributes 0 and its
   posterior is NOT consulted; an outcome with positive mass whose posterior
   is refused makes the whole EIG that refusal."
  [kernels policy-id]
  (let [predictive (get-in kernels [:posterior-predictive policy-id])]
    (if (nil? predictive)
      (refused :unknown-policy [:posterior-predictive policy-id])
      (reduce (fn [acc [outcome q]]
                (if-not (pos? q)
                  acc
                  (let [pig (parameter-information-gain kernels policy-id outcome)]
                    (if (= :refused (:status pig))
                      (reduced pig)
                      (+ acc (* (double q) pig))))))
              0.0 predictive))))

(defn delivery
  "Deliver the observation-bound record. :a-identity (identity of the A the
   kernels were computed against) and :observations (the observations the
   state distribution came from) are required with no default; missing either
   is a typed refusal."
  [kernels {:keys [a-identity observations]}]
  (cond
    (nil? a-identity) (refused :a-identity-required [:a-identity])
    (nil? observations) (refused :observations-required [:observations])
    :else {:schema delivery-schema
           :model (:model kernels)
           :a-identity a-identity
           :observations observations
           :expected-information-gain
           (into {} (map (fn [[policy-id _]]
                           [policy-id (expected-information-gain kernels policy-id)])
                         (:posterior-predictive kernels)))
           :posterior-kernel (:posterior-kernel kernels)
           :prior-kernel (:prior-kernel kernels)}))

(defn stale?
  "nil when the delivery is current, else {:stale true :because ...}.
   :a-identity is compared for equality; the model comparison is on the
   revision record (:model of the full model map), matching what parameter-
   kernels stamps into the kernels and hence into the delivery."
  [delivery {:keys [a-identity model]}]
  (cond
    ;; asking without saying what A and which model revision are current
    ;; would answer "current" for every delivery: refuse instead
    (and (nil? a-identity) (nil? model))
    (refused :nothing-to-compare-against [:a-identity :model])

    (and (contains? delivery :a-identity)
         (some-> a-identity (not= (:a-identity delivery))))
    {:stale true :because :a-changed}
    (and (contains? delivery :model)
         (some-> model :model (not= (:model delivery))))
    {:stale true :because :model-revision-changed}
    :else nil))

(defn refresh
  "When stale?, recompute by calling machine-parameters/parameter-kernels
   (which re-hashes every registration pin — that re-hash is what makes the
   refresh real) and return a fresh delivery. When not stale, return the
   delivery unchanged with :refreshed :not-required. A registration refusal
   from parameter-kernels passes through untouched."
  [delivery-record {:keys [model parameter-state policies outcome-support
                           a-identity observations] :as inputs}]
  (let [s (stale? delivery-record inputs)]
    (cond
      ;; nothing to compare against: refuse rather than recompute blind
      (= :refused (:status s)) s

      s (let [kernels (machine-parameters/parameter-kernels
                       model parameter-state policies outcome-support)]
          (if (:ok kernels)
            (assoc (delivery kernels {:a-identity a-identity :observations observations})
                   :refreshed :stale)
            kernels))

      :else (assoc delivery-record :refreshed :not-required))))

(defn g-with-information-gain
  "Consumer hand-off. Runs the injected g-fn on g-input and returns its value
   under :g, UNCHANGED — the information gain is not added to G. Production
   passes futon2.aif.active-horizon-g/active-horizon-g as g-fn."
  [{:keys [g-fn g-input delivery policy-id]}]
  {:g (g-fn g-input)
   :expected-information-gain (get-in delivery [:expected-information-gain policy-id])
   :basis {:lean "DarkTower.WarMachine.Holes.expectedInformationGain"
           :delivery-schema delivery-schema}})
