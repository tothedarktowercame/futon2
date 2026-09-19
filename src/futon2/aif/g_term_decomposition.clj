(ns futon2.aif.g-term-decomposition
  "Consumed-value census for AGG-single-kl-reduction. Evidence, never a gate.
   Degenerate means the named census reduction, not an invalid AIF value.
   In particular C means constant over the scored horizon, not uniform.")

(def terms [:A :C :D :E :F :Q])

(defn verdict
  "Classify a recorded consumed value. Missing evidence has no verdict; it
   must never count as a degenerate value (or a non-degenerate witness)."
  [term value]
  (if (nil? value)
    {:status :missing :value nil :reason :consumed-value-not-recorded}
    (let [[degenerate? reason]
          (case term
            :A (let [identity? (every? #(and (zero? (:false-neg %))
                                             (zero? (:false-pos %))) (vals value))]
                 [identity? (if identity? :identity-kernel :non-identity-kernel)])
            :C (let [constant? (or (= :constant-spec (:form value))
                                   (and (seq (:steps value))
                                        (apply = (map :distribution (:steps value)))))]
                 [constant? (if constant? :constant-across-horizon :varies-across-horizon)])
            :D (let [support (filter (comp pos? val) value)
                     point? (and (= 1 (count support)) (== 1 (val (first support))))]
                 [point? (if point? :point-mass :distributed-belief)])
            :E [(== 1 value) (if (== 1 value) :unit-habit :non-unit-habit)]
            :F [(zero? value) (if (zero? value) :zero-consumed-f :nonzero-consumed-f)]
            :Q [(empty? (:observation-updates value))
                (if (empty? (:observation-updates value))
                  :open-loop-no-conditioning :observation-conditioned)])]
      {:status :present :value value
       :verdict (if degenerate? :degenerate :non-degenerate) :reason reason})))

(defn census
  "Join scoring's consumed A/C/D/Q to selection's consumed E/F, by position
   in the selector's own candidate vector. Computed-but-unattached F is
   retained as provenance only and cannot determine F's verdict."
  [ranked candidates]
  (let [policies
        (mapv (fn [entry candidate]
                (let [values (assoc (get-in entry [:certificate :consumed-g])
                                    :E (:habit candidate) :F (:f candidate))]
                  {:id (:id candidate)
                   :terms (into {} (map (fn [term] [term (verdict term (get values term))]) terms))
                   :f-provenance (select-keys candidate [:f-status :reason :computed-f])}))
              ranked candidates)
        complete? (and (seq policies)
                       (every? #(every? (fn [term] (= :present (:status term)))
                                        (vals (:terms %))) policies))]
    {:schema :wm/g-term-decomposition-v1
     :status (if complete? :present :missing)
     :policies policies}))

(defn from-result
  "Read the decision retained by the runner's selection checkpoint. No
   selection (including an operator/repair bypass) is explicit missing data."
  [result]
  (or (get-in result [:checkpoints :selection :judgment :controller-decision
                     :selection-certificate :g-term-decomposition])
      (get-in result [:checkpoints :selection :judgment :decision
                     :selection-certificate :g-term-decomposition])
      {:schema :wm/g-term-decomposition-v1 :status :missing
       :reason :no-recorded-cascade-selection :policies []}))
