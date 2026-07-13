(ns futon2.aif.epistemic-value
  "Pure policy-conditioned expected information gain.

   For a policy π with predicted observations Q(o|π), prior Q(s|π), and the
   posterior that would follow each possible observation Q(s|o,π):

     EIG(π) = Σ_o Q(o|π) KL[Q(s|o,π) || Q(s|π)].

   HONESTY: this kernel is the canonical information-theoretic calculation,
   but it is not wired into the controller. Wiring requires a defensible
   policy-conditioned observation distribution and simulated posterior update
   for each observation. Current posterior spread and gap lookup do not satisfy
   that contract and must not be passed off as EIG.")

(def ^:private tolerance 1.0e-9)

(defn- probability-distribution!
  [label distribution]
  (when-not (and (map? distribution)
                 (seq distribution)
                 (every? (fn [[_ p]]
                           (and (number? p)
                                (Double/isFinite (double p))
                                (not (neg? (double p)))))
                         distribution)
                 (< (Math/abs (- 1.0 (reduce + 0.0 (vals distribution))))
                    tolerance))
    (throw (ex-info "expected a finite normalized probability distribution"
                    {:label label :distribution distribution})))
  distribution)

(defn kl-divergence
  "KL[posterior || prior] in nats. Fails closed when posterior mass lies
   outside prior support."
  [posterior prior]
  (probability-distribution! :posterior posterior)
  (probability-distribution! :prior prior)
  (reduce-kv
   (fn [total state q]
     (let [q (double q)
           p (double (get prior state 0.0))]
       (cond
         (zero? q) total
         (zero? p) (throw (ex-info "posterior mass outside prior support"
                                   {:state state :posterior q :prior p}))
         :else (+ total (* q (Math/log (/ q p)))))))
   0.0 posterior))

(defn- posterior-mixture
  [predicted-observations posteriors]
  (reduce-kv
   (fn [mixture observation p-observation]
     (let [posterior (get posteriors observation)]
       (when-not posterior
         (throw (ex-info "missing simulated posterior for predicted observation"
                         {:observation observation})))
       (probability-distribution! [:posterior observation] posterior)
       (reduce-kv (fn [m state p-state]
                    (update m state (fnil + 0.0)
                            (* (double p-observation) (double p-state))))
                  mixture posterior)))
   {} predicted-observations))

(defn expected-information-gain
  "Compute policy-conditioned EIG in nats.

   Input:
     {:prior                  Q(s|π)
      :predicted-observations Q(o|π)
      :posteriors             {o Q(s|o,π)}}

   The posterior mixture must reconstruct the prior. This Bayes-coherence gate
   prevents arbitrary posterior maps from manufacturing apparent information."
  [{:keys [prior predicted-observations posteriors]}]
  (probability-distribution! :prior prior)
  (probability-distribution! :predicted-observations predicted-observations)
  (let [mixture (posterior-mixture predicted-observations posteriors)
        states (set (concat (keys prior) (keys mixture)))
        mismatch (apply max 0.0
                        (map #(Math/abs (- (double (get prior % 0.0))
                                           (double (get mixture % 0.0))))
                             states))]
    (when (>= mismatch tolerance)
      (throw (ex-info "predicted posterior mixture does not reconstruct prior"
                      {:max-mismatch mismatch :prior prior :mixture mixture})))
    (reduce-kv
     (fn [total observation p-observation]
       (+ total (* (double p-observation)
                   (kl-divergence (get posteriors observation) prior))))
     0.0 predicted-observations)))

(defn policy-information-gains
  "Compute EIG independently for each policy descriptor."
  [policy-models]
  (into {} (map (fn [[policy model]]
                  [policy (expected-information-gain model)]))
        policy-models))
