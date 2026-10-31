(ns futon2.aif.outer-select
  "Pure task choice corresponding to `R20Step.chosenCascadeEntry` and `step`."
  (:require [futon2.aif.effective-run-history :as effective-run-history]
            [futon2.aif.outer-score :as outer-score]))

(defn- refuse! [kind data]
  (throw (ex-info "Outer selection refused" (assoc data :kind kind))))

(defn- duplicate-effective-run-exclusions
  "Typed pre-selection exclusions for candidates whose effective-run history
  already holds a blocking row on the field this selection reads
  (DUPLICATE-TARGET-STILL-SELECTED, 2026-10-31).

  This is the outer-level view only: the identity components known before
  selection are the target and the Giraffe manifest the field was built
  from. A blocking row for the same target under the same manifest means
  the post-construction duplicate guard (require-novel!) would refuse the
  run after ~40 s of selection; admission now refuses the target before
  scoring instead. blocking-row? is effective-run-history's own rule and
  is called, not copied (zai-5 owns its concurrent change). No inner
  policy is built here."
  [candidates observation]
  (let [rows (:effective-run-index-rows observation)
        manifest-sha (:giraffe/manifest-sha256 observation)]
    (if-not (and (seq rows) (string? manifest-sha))
      []
      (into []
            (keep (fn [candidate]
                    (let [target (:target candidate)
                          matched (filterv
                                   #(= manifest-sha
                                      (get-in % [:effective-run-identity
                                                 :giraffe-manifest-sha256]))
                                   (effective-run-history/blocking-rows-for-target
                                    rows target))]
                      (when (seq matched)
                        {:kind :outer-score/duplicate-effective-run
                         :target target
                         :reason :blocking-effective-run-on-same-giraffe-field
                         :giraffe-manifest-sha256 manifest-sha
                         :matched-run-ids (mapv :run-id matched)}))))
            candidates))))

(defn known-term-field
  "Supply R20Step.applySelectionStep only the known-term subset; retain the
  excluded population and its knowledge requests separately, without zeroes."
  [{:keys [field facts]}]
  (reduce
   (fn [result candidate]
     (let [target (:target candidate)
           fact (if (fn? facts) (facts target) (get facts target))
           graph (:graph-terms fact)
           terms (filterv #(or (= :unknown (:status graph))
                               (not (nat-int? (get fact %)))) [:cone :unblocks])]
       (if (empty? terms)
         (update result :field conj candidate)
         (let [reason (or (:reason graph) :graph-term-not-observed)
               request {:kind :outer-score/graph-term-knowledge-request
                        :target target :producer (or (:producer graph) :dependency-reader-reviewer)
                        :terms terms :reason reason
                        :prerequisites (:prerequisites graph)
                        :scope (select-keys graph [:scope :population-sha :coverage])}]
           (-> result
               (update :excluded conj
                       {:kind :outer-score/graph-term-unavailable :target target
                        :terms (mapv (fn [term] {:term term :reason reason
                                                :prerequisites (:prerequisites graph)}) terms)
                        :graph-terms graph :knowledge-producer-request request})
               (update :requests conj request))))))
   {:field [] :excluded [] :requests []} field))

(defn select
  "Score the makeup's cascade menu and return Lean `step` selection fields.

  A tie is resolved by menu order only so this is a function.  The returned
  `:tie?` records that condition because ties are a defect to remove with
  better evidence, not a task-selection rule (Joe, 2026-10-28)."
  [makeup state observation]
  (let [menu (:cascades makeup)
        partition (known-term-field observation)
        duplicate-excluded (duplicate-effective-run-exclusions
                            (:field partition) observation)
        duplicate-targets (set (map :target duplicate-excluded))
        partition (-> partition
                      (update :field (fn [field]
                                       (vec (remove #(contains? duplicate-targets
                                                                (:target %))
                                                    field))))
                      (update :excluded into duplicate-excluded))
        observation (assoc observation :field (:field partition))]
    (when (empty? (:field partition))
      (if (seq duplicate-excluded)
        ;; Every candidate the graph partition admitted is refused by its
        ;; own effective-run history: a typed refusal, never a silent
        ;; abstention, and never a fall-through to the graph-term reason.
        (refuse! :outer-score/duplicate-effective-run-field-emptied
                 {:outer-score/excluded (:excluded partition)
                  :duplicate-effective-run duplicate-excluded
                  :knowledge-producer-requests (:requests partition)})
        (refuse! :outer-score/no-known-graph-members
                 {:outer-score/excluded (:excluded partition)
                  :knowledge-producer-requests (:requests partition)})))
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
            model (outer-score/outer-model
                   (assoc score-input :cascade (:steps chosen)))
            picked (:pick model)]
        {:outer-score/admitted (mapv :target (:field partition))
         :outer-score/excluded (:excluded partition)
         :knowledge-producer-requests (:requests partition)
         :cascade-index index
         :cascade-id (:id chosen)
         :cascade-scores scores
         :selection (when picked {:candidates (:field observation)
                                  :selected picked})
         :prediction (when picked
                       {:outcomes (outer-score/push model)
                        :q (:q model)
                        :status (:status model)
                        :landing (:landing model)})
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
