(ns futon2.aif.outer-select
  "Pure task choice corresponding to `R20Step.chosenCascadeEntry` and `step`."
  (:require [futon2.aif.outer-score :as outer-score]))

(defn- refuse! [kind data]
  (throw (ex-info "Outer selection refused" (assoc data :kind kind))))

(defn select
  "Score the makeup's cascade menu and return Lean `step` selection fields.

  A tie is resolved by menu order only so this is a function.  The returned
  `:tie?` records that condition because ties are a defect to remove with
  better evidence, not a task-selection rule (Joe, 2026-10-28)."
  [makeup state observation]
  (let [menu (:cascades makeup)]
    (when-not (seq menu)
      (refuse! :empty-cascade-menu {}))
    (let [score-input (merge (select-keys observation [:focus :relation :facts :field])
                             {:state-counts (:state-counts state)
                              :ending-counts (:c state)
                              :gain-counts (:gain-counts state)})
          scores (mapv #(outer-score/outer-score
                         (assoc score-input :cascade (:steps %))) menu)]
      (when-let [bad (first (keep-indexed
                             (fn [index score]
                               (when-not (and (number? score)
                                              (Double/isFinite (double score)))
                                 {:index index :score score}))
                             scores))]
        (refuse! :cascade-score-inadmissible bad))
      (let [minimum (reduce min scores)
            index (.indexOf scores minimum)
            chosen (nth menu index)
            picked (outer-score/cascade-pick (:steps chosen)
                                             (:facts observation)
                                             (:field observation))]
        {:cascade-index index
         :cascade-id (:id chosen)
         :cascade-scores scores
         :selection (when picked {:candidates (:field observation)
                                  :selected picked})
         ;; Cascades that share the minimum but pick the same task are not a
         ;; tie in the choice; only distinct tied picks are the defect.
         :tied-cascades (vec (keep-indexed #(when (= minimum %2) (:id (nth menu %1)))
                                           scores))
         :tie? (< 1 (count (distinct
                            (keep-indexed
                             #(when (= minimum %2)
                                (:target (outer-score/cascade-pick
                                          (:steps (nth menu %1))
                                          (:facts observation)
                                          (:field observation))))
                             scores))))}))))
