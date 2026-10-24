(ns futon2.aif.wm.family-canonical-g
  "Opt-in field comparison using the Lean canonical co-application G."
  (:require [futon2.aif.load-identity :as load-identity]
            [futon2.aif.cascade-coapply-g :as canonical]
            [futon2.aif.cascade-pattern-counts :as counts]
            [futon2.aif.cascade-observation-scoring :as scorer]
            [futon2.aif.cascade-selection :as selection]
            [futon2.aif.cascade-shape-g :as shape]))

(load-identity/register! *ns* *file*)

(defn- comparison-id [[p _]] [(:target p) (:policy-id p)])

(defn- exact-rational? [x] (or (integer? x) (ratio? x)))

(defn coherence-finding
  "CoherentThetaCandidate.Coherent at the family boundary. Only exact integer
   or ratio inputs certify this equality; IEEE/decimal coercions and tolerances
   are not proofs of a common posterior. No ledger reads or parameter repairs."
  [candidate pattern-counts]
  (let [{:keys [units patterns]} (get-in candidate [:precedence :co-apply])]
    (if-not (and (vector? units) (seq units) (map? patterns)
                 (map? pattern-counts) (= (set units) (set (keys pattern-counts))))
      {:kind :pattern-coherence-input-missing}
      (some (fn [unit]
              (let [theta (get-in patterns [unit :theta])
                    pair (get pattern-counts unit)
                    reason (cond
                             (not (exact-rational? theta)) :unsupported-pattern-theta
                             (not (and (vector? pair) (= 2 (count pair))
                                       (every? #(and (exact-rational? %) (pos? %)) pair)))
                             :unsupported-pattern-counts
                             (not= theta (/ (first pair) (reduce +' pair)))
                             :incoherent-pattern-theta)]
                (when reason {:kind reason :unit unit :theta theta :counts pair}))) units))))

(defn- replace-score [score entry model inputs horizon-choice]
  (let [terms (get-in entry [:certificate :g-terms])
        carrier (assoc (:selection-candidate score) :g (:G-efe entry))]
    (assoc score
           :g-law :canonical-coapply :scorer-version :canonical-coapply-g-v1
           :g (:G-efe entry) :controller-score (:controller-score entry)
           :risk (:risk terms) :ambiguity (:ambiguity terms)
           :information-gain (:expected-information-gain terms)
           :horizon (:horizon-steps entry) :horizon-choice horizon-choice
           :preference-at-each-step (:class-preference model)
           :g-normalization (get-in entry [:certificate :g-terms :normalization-declaration])
           :raw-g-terms (:raw terms)
           :canonical-inputs inputs
           :legacy-g {:progress-g (:progress-g score)
                      :class-risk (get-in score [:class-emission :risk])
                      :class-ambiguity (get-in score [:class-emission :ambiguity])
                      :fit-ambiguity (get-in score [:fit :fit-ambiguity])
                      :consumed-in-g? false}
           :class-emission {:status :computed :consumed-in-g? true
                            :composition :class-risk-plus-ambiguity-minus-frontier-novelty
                            :model model :risk (:risk terms) :ambiguity (:ambiguity terms)
                            :certificate (:certificate entry)}
           :scorer-result (with-meta [entry] {:cascade-scoring {:model model}})
           :selection-candidate carrier :selection-law (selection/law-receipt [carrier]))))

(defn rescore
  "Rescore all successful legacy constructions once, retaining candidate failures.
   Comparison identity is the ordered target/policy census, not a fired occurrence.
   No token preference is supplied or consumed by this class-only law."
  [rows]
  (let [pairs (vec (mapcat :scored rows))
        candidates (mapv #(assoc (:candidate (second %)) :id (comparison-id %)) pairs)
        counts-result (counts/family-pattern-counts candidates)
        incoherent (into {} (keep (fn [c]
                                   (when-let [finding (coherence-finding c (get-in counts-result [:pattern-counts (:id c)]))]
                                     [(:id c) finding])) candidates))
        eligible (filterv #(not (contains? incoherent (:id %))) candidates)
        h (max 1 (min scorer/max-horizon
                      (reduce max 0 (map #(count (get-in % [:precedence :co-apply :units])) candidates))))
        horizon-choice {:horizon h :rule :max-field-unit-count-clamped
                        :limit scorer/max-horizon
                        :unit-counts (into {} (map #(vector (:id %) (count (get-in % [:precedence :co-apply :units]))) candidates))}
        models (into {} (map (fn [[p score :as pair]]
                              (let [m (get-in score [:class-emission :model])]
                                [(comparison-id pair)
                                 (shape/class-emission-model (:target p) (:universe m) (:acceptance m) h
                                   (get (:target-class m) (:target p)) (:provenance m))])) pairs))
        options {:horizon-steps h :pattern-counts (:pattern-counts counts-result)
                 :observation-model-of models :require-ruled-class-kernel? true
                 :prediction-context {:occurrence-id [:field-comparison (mapv :id candidates)] :tau h}}
        results (when (not= :refused (:status counts-result))
                  (if (seq eligible)
                    (canonical/compare-at-horizon {:cascade-belief {#{} 1}} eligible options)
                    []))
        refusal (cond
                  (= :refused (:status counts-result)) counts-result
                  (empty? eligible) {:status :refused :reason :no-admissible-candidate}
                  (not (vector? results)) results)
        entries (merge (into {} (map (juxt :cascade-id identity))
                             (if (vector? results) results (:candidates results)))
                       (when-not (= :refused (:status counts-result))
                         (into {} (map (fn [[id finding]]
                                         [id {:status :refused :refusal finding}]) incoherent))))
        updated (mapv
                 (fn [row]
                   (let [scored (filterv some? (mapv (fn [[p score :as pair]]
                                        (let [id (comparison-id pair) entry (get entries id)]
                                          (when (= :scored (:status entry))
                                            [p (replace-score score entry (get models id)
                                                 {:candidate (assoc (:candidate score) :id id)
                                                  :state {:cascade-belief {#{} 1}}
                                                  :options (assoc options :observation-model-of {id (get models id)}
                                                                         :pattern-counts {id (get-in counts-result [:pattern-counts id])})}
                                                 horizon-choice)]))) (:scored row)))
                         failures (into (:failures row)
                                        (keep (fn [pair]
                                                (let [id (comparison-id pair) e (get entries id)]
                                                  (when (and e (= :refused (:status e)))
                                                    {:target-id (:target-id row) :policy-id (second id)
                                                     :stage :canonical-g :kind (or (get-in e [:refusal :kind])
                                                                                 (get-in e [:refusal :reason]))
                                                     :refusal (:refusal e)
                                                     :count-refusal (get-in counts-result [:refused id])}))))
                                        (:scored row))]
                     (assoc row :scored scored :failures failures :failure-count (count failures)))) rows)]
    {:rows updated :refusal refusal
     :comparison {:g-law :canonical-coapply :horizon-choice horizon-choice
                  :pattern-table (:table counts-result) :count-refusals (:refused counts-result)
                  :lean-reduction (:lean-reduction (meta results))}}))

(defn certify-score
  "Recompute the canonical component from retained inputs, without ledger IO.
   Check the ruled class schedule, raw terms and current candidate, not a legacy
   progress/fit composition. Field snapshot consistency was checked by rescore."
  [record]
  (let [{:keys [candidate state options]} (:canonical-inputs record)
        model (get-in record [:class-emission :model])
        h (:horizon record)
        ruled (:class-preference (shape/class-emission-model ::ruled #{} #{} h))
        counts-result (counts/family-pattern-counts [candidate])
        expected-counts (:pattern-counts counts-result)
        coherence (coherence-finding candidate (get-in options [:pattern-counts (:id candidate)]))
        result (when-not coherence (canonical/compare-at-horizon state [candidate] options))
        entry (when (vector? result) (first result))
        actual (first (:scorer-result record))
        terms (get-in entry [:certificate :g-terms])
        fields {:g (:G-efe entry) :controller-score (:controller-score entry)
                :risk (:risk terms) :ambiguity (:ambiguity terms)
                :information-gain (:expected-information-gain terms)}
        valid? (and (= :scored (:status entry))
                    (= {:cascade-belief {#{} 1}} state)
                    (= candidate (assoc (:candidate record) :id (:id candidate)))
                    (= [(:target record) (:policy-id record)] (:id candidate))
                    (= expected-counts (:pattern-counts options))
                    (not= :refused (:status counts-result))
                    (= true (:require-ruled-class-kernel? options))
                    (= h (:horizon-steps options) (:horizon model))
                    (= model (get-in options [:observation-model-of (:id candidate)]))
                    (= ruled (:class-preference model) (:preference-at-each-step record))
                    (= (dissoc entry :rank :g-tie) (dissoc actual :rank :g-tie))
                    (= fields (select-keys record (keys fields)))
                    (= (:g record) (get-in record [:selection-candidate :g]))
                    (= (:selection-law record) (selection/law-receipt [(:selection-candidate record)]))
                    (= :none (:normalization terms))
                    (= (:g-normalization record) (:normalization-declaration terms))
                    (= (:raw-g-terms record) (:raw terms)))]
    (if valid?
      {:status :certified :schema :wm/canonical-reading-preference-v1
       :policy-id (:policy-id record) :target (:target record)
       :horizon h :preference ruled :g (:g record)
       :g-terms (select-keys record [:risk :ambiguity :information-gain])
       :g-law :canonical-coapply
       :live-c {:role :diagnostic-only :selected false}
       :normalization {:declaration (:normalization-declaration terms)
                       :raw {:risk (get-in terms [:raw :risk])
                             :ambiguity (get-in terms [:raw :ambiguity])
                             :information-gain (get-in terms [:raw :expected-information-gain])}
                       :terms (select-keys record [:risk :ambiguity :information-gain])
                       :consumed-fit-ambiguity? false}
       :authority {:kind :class-emission :source :classKernel :model-horizon h
                   :lean :compareAtHorizonN :version :canonical-coapply-g-v1}}
      {:status :refused :kind (or (:kind coherence) :canonical-g-not-bound-to-record)
       :coherence-finding coherence
       :policy-id (:policy-id record) :target (:target record)
       :recomputation (when-not (vector? result) result)})))
