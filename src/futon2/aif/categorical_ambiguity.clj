(ns futon2.aif.categorical-ambiguity
  "Categorical R5 ambiguity over supplied, already-admitted machine objects.

  `ambiguity` computes

      sum_s Q(s) * (- sum_o A(o|s) ln A(o|s)).

  A zero probability contributes zero (`0 * ln 0 = 0` by convention). This
  namespace does not construct, license, normalize, or install Q or A."
  (:require [futon2.aif.machine-model :as machine-model]))

(defn- refuse!
  [kind path & [detail]]
  (throw (ex-info "Categorical ambiguity refused"
                  (cond-> {:refusal {:kind kind :path path}
                           :machine-model/refusal kind
                           :path path}
                    detail (assoc :detail detail)))))

(defn- model-identity?
  [x]
  (and (map? x)
       (or (and (string? (:id x)) (seq (:id x))) (keyword? (:id x)))
       (or (and (string? (:revision x)) (seq (:revision x)))
           (keyword? (:revision x)))))

(defn- ordered-support!
  [support path]
  (when-not (and (vector? support) (seq support)
                 (= (count support) (count (set support))))
    (refuse! :support-mismatch path))
  support)

(defn- mass-admission!
  [mass support path]
  (when-not (map? mass)
    (refuse! :mass-invalid path))
  (when-not (= (set support) (set (keys mass)))
    (refuse! :support-mismatch path))
  (when-not (every? #(and (number? %)
                          (Double/isFinite (double %))
                          (not (neg? %)))
                    (vals mass))
    (refuse! :mass-invalid path))
  (or (machine-model/row-sum-admission mass)
      (refuse! :mass-invalid path {:reason :unnormalized})))

(defn- validate-q!
  [q]
  (when-not (map? q) (refuse! :malformed-map [:Q]))
  (doseq [field [:model :state-support :mass :authority]]
    (when-not (contains? q field) (refuse! :missing-field [:Q field])))
  (when-not (model-identity? (:model q))
    (refuse! :model-revision-mismatch [:Q :model]))
  (let [support (ordered-support! (:state-support q) [:Q :state-support])
        admission (mass-admission! (:mass q) support [:Q :mass])]
    {:support support :admission admission}))

(defn- validate-a!
  [a]
  (when-not (map? a) (refuse! :malformed-map [:A]))
  (doseq [field [:model :state-support :outcome-support :rows :authority]]
    (when-not (contains? a field) (refuse! :missing-field [:A field])))
  (when-not (model-identity? (:model a))
    (refuse! :model-revision-mismatch [:A :model]))
  (when (= :declared-prior (:authority a))
    (refuse! :declared-prior-refused [:A :authority]))
  (when-not (= :observed-estimate (:authority a))
    (refuse! :authority-unlicensed [:A :authority]))
  (let [states (ordered-support! (:state-support a) [:A :state-support])
        outcomes (ordered-support! (:outcome-support a) [:A :outcome-support])
        rows (:rows a)]
    (when-not (and (map? rows) (seq rows))
      (refuse! :missing-rows [:A :rows]))
    (when-not (= (set states) (set (keys rows)))
      (refuse! :support-mismatch [:A :rows]))
    {:states states
     :outcomes outcomes
     :admissions
     (into {}
           (for [state states]
             (let [row (get rows state)
                   path [:A :rows state]]
               (when-not (map? row) (refuse! :missing-row path))
               (when-not (= outcomes (:support row))
                 (refuse! :support-mismatch (conj path :support)))
               [state (mass-admission! (:mass row) outcomes
                                       (conj path :mass))])))}))

(defn- entropy
  [row support]
  (reduce + 0.0
          (for [outcome support
                :let [p (double (get row outcome))]
                :when (pos? p)]
            (- (* p (Math/log p))))))

(defn ambiguity
  "Compute categorical ambiguity for supplied Q(s) and A(o|s).

  Q shape:
    {:model {:id ... :revision ...} :state-support [...] :mass {...}
     :authority ...}

  A shape:
    {:model {:id ... :revision ...} :state-support [...]
     :outcome-support [...] :rows {state {:support [...] :mass {...}}}
     :authority :observed-estimate}

  Supports are ordered contracts. Inputs are admitted with
  machine-model/row-sum-admission and are never renormalized."
  [q a]
  (let [qv (validate-q! q)
        av (validate-a! a)]
    (when-not (= (:model q) (:model a))
      (refuse! :model-revision-mismatch [:model]
               {:Q (:model q) :A (:model a)}))
    (when-not (= (:support qv) (:states av))
      (refuse! :support-mismatch [:state-support]))
    {:ok true
     :model (:model q)
     :state-support (:support qv)
     :outcome-support (:outcomes av)
     :authority {:Q (:authority q) :A (:authority a)}
     :admission {:Q (:admission qv) :A (:admissions av)}
     :ambiguity
     (reduce + 0.0
             (for [state (:support qv)]
               (* (double (get-in q [:mass state]))
                  (entropy (get-in a [:rows state :mass])
                           (:outcomes av)))))}))
