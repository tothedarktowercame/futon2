(ns futon2.aif.outer-cascade
  "PROOF-2a Clause T, the choice (H-T-CALLER-I): the outer cascade's `select`.

  The target field (`target-field/target-field`) lists every considered
  target; the feasible ones carry `:eligible`. `select` chooses among the
  ELIGIBLE ones, feasibility acting as policy support and not as a value term.
  It is the mixture law's degenerate case, and says so on the record:

    E  absent an enactment fold, uniform over the support with basis
       `:uniform-no-data`. Given a fold, admitted (`:delta 1`) receipts are
       joined by their policy key's mission slot to targets in the support.
       With n_t joined receipts, E_t = (n_t + 1) / Σ_s(n_s + 1). Admitted
       receipts outside the support are recorded by id as unjoined;
    G  per-target (H-G-TARGET part 2, HG2-Ib): each eligible entry's :delta-g
       (HG2-Ia) is either a recorded value {:delta Δ :universe U} or a typed
       absence, never a number standing in. Over D = {t : ΔG_t recorded},

         p(t) = E(D)·E_t·e^(−ΔG_t) / Σ_{s∈D} E_s·e^(−ΔG_s)   (t ∈ D)
         p(t) = E_t                                            (t ∉ D)

       which is E alone when D is empty and the ordinary softmax-weighted
       posterior when D = S. D members are recorded as doubles (ΔG_t is a
       double); t ∉ D keeps the exact ratio E_t. The choice is a seeded draw
       from this mixture.

  The draw is recomputable: the support sorted by target id, a
  `java.util.SplittableRandom` on the seed, its first `nextDouble` u, the first
  target whose cumulative posterior exceeds u. The generator, seed, value and
  index are on the record. (`SplittableRandom`, not `Random`: adjacent small
  seeds give `Random` nearly the same first value, so successive seeds would
  hardly vary the choice; SplittableRandom mixes the seed.)

  Pure: no file, store, clock or environment access. The seed is the caller's."
  (:require [futon2.aif.enactment-habit :as enactment-habit])
  (:import [java.util SplittableRandom]))

(defn- support-of
  "The eligible feasible entries of FIELD, by target id."
  [field]
  (->> (:feasible field) (filter :eligible) (sort-by :target) vec))

(defn- excluded-of
  "Every considered target that is not in the support, with why: the feasible
  entries a requisition makes ineligible, and the exclusions (non-targets)."
  [field]
  (->> (concat
        (for [e (:feasible field) :when (not (:eligible e))]
          {:target (:target e) :kind :ineligible
           :reason (or (:ineligible-reason e) :not-eligible)})
        (for [x (:exclusions field)]
          (cond-> {:target (:target x) :kind :excluded :reason (:reason x)}
            (:what-would-make-feasible x)
            (assoc :what-would-make-feasible (:what-would-make-feasible x)))))
       (sort-by :target)
       vec))

(defn draw
  "The seeded draw over POSTERIOR (a sorted map target -> mass): {:generator :seed
  :value u :index i :order [targets…]}. Recomputable from these alone."
  [posterior seed]
  (let [order (vec (keys posterior))
        u (.nextDouble (SplittableRandom. (long seed)))
        index (loop [i 0 cum 0]
                (let [cum (+ cum (get posterior (nth order i)))]
                  (cond (< u (double cum)) i
                        (= i (dec (count order))) i
                        :else (recur (inc i) cum))))]
    {:generator "java.util.SplittableRandom" :seed seed :value u :index index :order order}))

(defn- recorded-input [m k reason]
  (if (some? (get m k)) (get m k) {:absent reason}))

(defn- g-of
  "One entry's contribution to G: its :delta-g re-keyed to {:delta Δ :universe U}
  when it is a recorded value; its typed absence verbatim; the HG2-Ia default
  when the entry carries no :delta-g key. A :delta-g that is neither a typed
  absence nor a well-formed value is itself a typed absence — never a number."
  [entry]
  (let [dg (:delta-g entry ::missing)]
    (cond
      (= ::missing dg) {:absent :no-target-grain-g}
      (and (map? dg) (contains? dg :absent)) dg
      (and (map? dg) (number? (:value dg)) (some? (:universe dg)))
      {:delta (:value dg) :universe (:universe dg)}
      :else {:absent :delta-g-malformed})))

(defn- e-masses [targets e-basis]
  (case (:basis e-basis)
    :uniform-no-data
    (into (sorted-map) (map (fn [t] [t (/ 1 (count targets))])) targets)

    :target-habit-prior
    (let [counts (:counts e-basis)
          denominator (reduce + (map #(inc (get counts % 0)) targets))]
      (into (sorted-map)
            (map (fn [t] [t (/ (inc (get counts t 0)) denominator)]))
            targets))))

(defn- mixture-posterior
  "The mixture law over ENTRIES (the eligible support) with basis E-BASIS
  (uniform or target-habit prior). Returns {:posterior sorted-map :g sorted-map
  :g-defined-on sorted-vector}. D = {t : ΔG_t recorded as a value}; for t ∈ D,
  p(t) = E(D)·E_t·e^(−ΔG_t)/Σ_{s∈D} E_s·e^(−ΔG_s) as a double; for t ∉ D,
  p(t) = E_t as an exact ratio. D empty ⇒ the posterior is E alone."
  [entries e-basis]
  (when-not (contains? #{:uniform-no-data :target-habit-prior} (:basis e-basis))
    (throw (ex-info "mixture-posterior: unsupported E basis"
                    {:basis e-basis})))
  (let [n (count entries)
        g (into (sorted-map) (map (fn [e] [(:target e) (g-of e)])) entries)
        d (into [] (comp (filter #(contains? (val %) :delta)) (map key)) g)
        e (e-masses (keys g) e-basis)]
    (if (or (zero? n) (empty? d))
      {:posterior e
       :g g :g-defined-on d}
      (let [e-d (reduce + (map e d))
            w (into {} (map (fn [t] [t (* (e t) (Math/exp (- (double (get-in g [t :delta])))))]) d))
            z (reduce + (vals w))]
        {:posterior (into (sorted-map)
                          (map (fn [[t gv]]
                                 (if (contains? gv :delta)
                                   [t (double (* e-d (/ (get w t) z)))]
                                   [t (e t)])))
                          g)
         :g g :g-defined-on d}))))

(defn- target-habit-basis [support enactment-fold]
  (let [support-set (set support)
        admitted (filter (fn [[_ receipt]] (= 1 (:delta receipt)))
                         (:enactment-records enactment-fold))
        targets (map (fn [[record-id receipt]]
                       [record-id (:mission (#'enactment-habit/key->view
                                             (:policy-key receipt)))])
                     admitted)
        joined (filter (comp support-set second) targets)]
    {:basis :target-habit-prior
     :alpha 1
     :counts (into (sorted-map)
                   (map (fn [target]
                          [target (count (filter #(= target (second %)) joined))]))
                   support)
     :unjoined (->> targets
                    (remove (comp support-set second))
                    (map first)
                    (sort-by pr-str)
                    vec)}))

(defn- selection-inputs [entries opts]
  (merge
   (into {} (for [k [:next-step :pair-overlap]]
              [k (into (sorted-map)
                       (for [entry entries]
                         [(:target entry) (recorded-input entry k :no-such-key-on-entry)]))]))
   (into {} (for [k [:enactment-records :publication-observed :clock-lineage]]
              [k (recorded-input opts k :not-supplied)]))))

(defn select
  "Choose a target from the field. OPTS: :field (the `target-field` map), :seed
  (an integer, the caller's), :trigger (which clock fired, recorded). Returns
  {:target-selection record} and, when the support is non-empty and a seed is given,
  :chosen-target and :draw-seed beside it. An empty support is the recorded
  absence {:absent :no-eligible-target}; a missing seed is {:absent :no-seed}.
  Neither is a refusal: the excluded list is on the record either way.
  Records entry :next-step/:pair-overlap and opts :enactment-records,
  :publication-observed, :clock-lineage verbatim as inputs, including typed
  absences. These are not value terms in the draw; :law-uses names :eligible
  and :delta-g. The posterior is the mixture over E (uniform, no data) and the
  per-target ΔG values (see mixture-posterior). Optional :enactment-fold
  supplies the target-grain habit prior; absent it, the prior and record remain
  the original uniform-no-data basis. :law is :E-only when no entry
  carries a ΔG value, :mixed otherwise, with :g the per-target record and
  :g-defined-on the support's D."
  [opts]
  (let [{:keys [field seed trigger]} opts
        entries (support-of field)
        support (mapv :target entries)
        n (count support)
        e-basis (if (some? (:enactment-fold opts))
                  (target-habit-basis support (:enactment-fold opts))
                  {:basis :uniform-no-data})
        {:keys [posterior g g-defined-on]}
        (if (pos? n)
          (mixture-posterior entries e-basis)
          {:posterior (sorted-map) :g (sorted-map) :g-defined-on []})
        seeded? (integer? seed)
        d (when (and (pos? n) seeded?) (draw posterior seed))
        chosen (some-> d :order (nth (:index d)))]
    (cond-> {:target-selection
             {:rule :seeded-draw-from-E
              :trigger (or trigger {:absent :no-trigger})
              :inputs (selection-inputs entries opts)
              :law-uses [:eligible :delta-g]
              :support support
              :posterior posterior
              :E e-basis
              :law (if (seq g-defined-on) :mixed :E-only)
              :g g
              :g-defined-on g-defined-on
              :draw (cond d d (zero? n) {:absent :no-eligible-target} :else {:absent :no-seed})
              :chosen (cond chosen chosen (zero? n) {:absent :no-eligible-target} :else {:absent :no-seed})
              :excluded (excluded-of field)
              :counts {:considered (count (:considered field))
                       :feasible (count (:feasible field))
                       :eligible n
                       :excluded (count (excluded-of field))}}}
      chosen (assoc :chosen-target chosen :draw-seed seed))))
