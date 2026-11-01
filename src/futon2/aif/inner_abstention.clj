(ns futon2.aif.inner-abstention
  "No-action comparison under each acting policy's retained model.
  Version 1: no transition information or node-fit ambiguity; no fabricated
  observation conditioning. The minimum's reference is a declared cross-lens
  approximation when compared with other policies."
  (:require [futon2.aif.cascade-observation-scoring :as scorer]
            [futon2.aif.cascade-selection :as selection]
            [futon2.aif.action-identity :as identity]))

(def no-action-rule-version 1)
(defn- refuse! [kind detail]
  (throw (ex-info "Inner stopping comparison refused" {:kind kind :detail detail})))

(defn- channel [model initial target id]
  (when-not (seq (or (:want model) (:acceptance model)))
    (refuse! :empty-want-stop {:policy id}))
  (let [h (:horizon model)
        result (scorer/rank-cascade-actions
                {:cascade-belief initial}
                [{:kind :cascade-candidate :id id :target target :precedence []}]
                {:horizon-steps h :observation-model model :prediction-only? true
                 :parameter-information-mode :none
                 :g-normalization :per-step-capacity-and-pattern
                 :prediction-context {:occurrence-id id :tau h}
                 :cascade-spec {:want (or (:want model) (:acceptance model))
                                :evidence #{} :zeroed #{}
                                :c {:source :retained-reference-model}
                                :c-schedule (vec (range 1 (inc h)))}})]
    (when-not (vector? result) (refuse! :stopped-policy-scoring-refused result))
    (first result)))

(defn score
  "Candidate is the SC candidate shape, scored its progress certificate.
  Preserve the reference initial belief, models, C and horizon, not a model
  reconstructed from an empty arrangement. Retain full predictions for replay."
  [candidate scored]
  (let [id (get-in candidate [:id :id]) target (get-in candidate [:id :target])
        initial (get-in scored [:consumed-g :D])
        pm (:observation-model scored)
        cm (get-in candidate [:class-emission :certificate :observation-model])]
    (when-not (and initial pm cm)
      (refuse! :stop-reference-model-unavailable {:policy id}))
    (let [p (channel pm initial target (str id "/stop"))
          c (channel cm initial target (str id "/stop"))
          pt (get-in p [:certificate :g-terms]) ct (get-in c [:certificate :g-terms])
          ps (get-in p [:certificate :steps]) cs (get-in c [:certificate :steps])
          terms {:risk (+ (:risk pt) (:risk ct))
                 :ambiguity (+ (:ambiguity pt) (:ambiguity ct))
                 :information-gain 0.0 :fit-ambiguity 0.0}]
      {:G (+ (:risk terms) (:ambiguity terms)) :terms terms
       :reference-model-digests [(identity/digest pm) (identity/digest cm)]
       :horizon (:horizon pm) :initial-belief initial
       :prediction {:progress (mapv :prediction ps) :class (mapv :prediction cs)}
       :prediction-digest (selection/prediction-digest ps cs)
       :normalizations [(get-in p [:certificate :g-terms :normalization-declaration])
                        (get-in c [:certificate :g-terms :normalization-declaration])]
       :no-action-rule-version no-action-rule-version})))

(defn valid-entry? [e]
  (let [{:keys [risk ambiguity information-gain fit-ambiguity]} (:terms e)]
    (and (= no-action-rule-version (:no-action-rule-version e))
         (every? #(and (number? %) (Double/isFinite (double %)))
                 [(:G e) risk ambiguity information-gain fit-ambiguity])
         (= 0.0 (double fit-ambiguity))
         (< (abs (- (double (:G e)) (- (+ risk ambiguity) information-gain))) 1.0e-9)
         (seq (get-in e [:prediction :progress]))
         (= (:horizon e) (count (get-in e [:prediction :progress]))
            (count (get-in e [:prediction :class])))
         (= (:prediction-digest e)
            (identity/digest [(get-in e [:prediction :progress])
                              (get-in e [:prediction :class])])))))

(defn validate!
  "Validate the carrier against the compared SC candidates; no historical default."
  [carrier candidates]
  (let [ids (set (map #(get-in % [:id :id]) candidates))
        per (:per-policy carrier)]
    (when-not (and (= :stop-under-the-minimum (:rule carrier))
                   (boolean? (:cross-lens carrier))
                   (= ids (set (keys per)))
                   (contains? ids (:chosen-reference carrier))
                   (every? valid-entry? (vals per)))
      (refuse! :invalid-inner-abstention {:reference (:chosen-reference carrier)})))
  carrier)

(defn comparison
  "All compared entries retained, minimum and admissible entries supplied by
  the existing law. Legacy callers without stopping carriers keep their path;
  a partly attached carrier refuses rather than silently skipping candidates."
  [ranked admissible minimum]
  (when (some :abstention ranked)
    (let [per (into {} (map #(vector (get-in % [:action :id]) (:abstention %)) ranked))
          ref (get-in minimum [:action :id])
          carrier {:per-policy per :chosen-reference ref :rule :stop-under-the-minimum
                   :cross-lens (> (count per) 1)}
          _ (validate! carrier (map #(hash-map :id (:action %)) ranked))
          beats? (fn [e] (< (:controller-score e) (:G (per (get-in e [:action :id])))))
          any-beats? (some beats? admissible)]
      (cond
        (beats? minimum) {:carrier carrier :status :chosen}
        any-beats? (refuse! :pick-not-below-abstention {:reference ref :abstention carrier})
        :else {:carrier (assoc carrier :cross-lens true) :status :abstain-on-task}))))
