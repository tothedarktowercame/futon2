(ns futon2.aif.outer-cascade
  "PROOF-2a Clause T, the choice (H-T-CALLER-I): the outer cascade's `select`.

  The target field (`target-field/target-field`) lists every considered
  target; the feasible ones carry `:eligible`. `select` chooses among the
  ELIGIBLE ones, feasibility acting as policy support and not as a value term.
  It is the mixture law's degenerate case, and says so on the record:

    E  uniform over the support, `{:basis :uniform-no-data}`: nothing sums the
       enactment habit to target grain, and no enactment has passed W_c;
    G  `{:absent :no-target-grain-g}`: a target with no constructed candidate
       has no ΔG (H-G-TARGET part 2 is owed); never a number standing in;
    so the posterior over the support is E, and the choice is a seeded draw
    from it.

  The draw is recomputable: the support sorted by target id, a
  `java.util.SplittableRandom` on the seed, its first `nextDouble` u, the first
  target whose cumulative posterior exceeds u. The generator, seed, value and
  index are on the record. (`SplittableRandom`, not `Random`: adjacent small
  seeds give `Random` nearly the same first value, so successive seeds would
  hardly vary the choice; SplittableRandom mixes the seed.)

  Pure: no file, store, clock or environment access. The seed is the caller's."
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

(defn select
  "Choose a target from the field. OPTS: :field (the `target-field` map), :seed
  (an integer, the caller's), :trigger (which clock fired, recorded). Returns
  {:target-selection record} and, when the support is non-empty and a seed is given,
  :chosen-target and :draw-seed beside it. An empty support is the recorded
  absence {:absent :no-eligible-target}; a missing seed is {:absent :no-seed}.
  Neither is a refusal: the excluded list is on the record either way."
  [{:keys [field seed trigger]}]
  (let [support (mapv :target (support-of field))
        n (count support)
        posterior (into (sorted-map) (map (fn [t] [t (/ 1 n)])) support)
        seeded? (integer? seed)
        d (when (and (pos? n) seeded?) (draw posterior seed))
        chosen (some-> d :order (nth (:index d)))]
    (cond-> {:target-selection
             {:rule :seeded-draw-from-E
              :trigger (or trigger {:absent :no-trigger})
              :support support
              :posterior posterior
              :E {:basis :uniform-no-data}
              :g {:absent :no-target-grain-g}
              :draw (cond d d (zero? n) {:absent :no-eligible-target} :else {:absent :no-seed})
              :chosen (cond chosen chosen (zero? n) {:absent :no-eligible-target} :else {:absent :no-seed})
              :excluded (excluded-of field)
              :counts {:considered (count (:considered field))
                       :feasible (count (:feasible field))
                       :eligible n
                       :excluded (count (excluded-of field))}}}
      chosen (assoc :chosen-target chosen :draw-seed seed))))
