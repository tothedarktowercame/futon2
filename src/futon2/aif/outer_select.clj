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

(defn- relation-class
  "The relation class the scorer would see for TARGET, or ::no-relation-fn
  when the observation carries no relation function at all (pure unit
  fixtures; the production scoring path always supplies one, and a nil
  function cannot be scored -- `ending-class` would throw)."
  [observation target]
  (let [relation (:relation observation)]
    (if (fn? relation)
      (relation (:focus observation) target)
      ::no-relation-fn)))

(defn- known-relation?
  [class]
  (contains? #{:focus :associated :useful-elsewhere} class))

(defn known-term-field
  "Supply R20Step.applySelectionStep the known-term subset; retain the
  excluded population and its knowledge requests separately, without zeroes.

  TIED-OUTER-CASCADES (2026-10-31, rev per claude-1 decision): the relation
  term is NOT an admission gate -- a relation-unknown candidate stays in the
  field (Joe's rule: the machine must not abstain, and a small field is a
  failure).  Ignorance is retained beside each pick instead: `select`
  records `:cascade-pick-relations`, so a scored stop that rests on an
  unknown relation reads as ignorance in the record, not as a deliberate
  stop.  The earlier revision's `:outer-score/relation-unavailable`
  exclusion and the `:outer-score/no-known-relation-members` refusal are
  removed; mapping an unknown relation onto a `:not-yet-evaluated` landing
  was measured on the real fields and is inadmissible (c-kit preference 0
  gives risk ⊤ for every pick with reach > 0), which would refuse the whole
  menu -- see the row report, addendum 3."
  [{:keys [field facts]}]
  (let [graph-exclusion
        (fn [result target _fact graph terms]
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
                (update :requests conj request))))]
    (reduce
     (fn [result candidate]
       (let [target (:target candidate)
             fact (if (fn? facts) (facts target) (get facts target))
             graph (:graph-terms fact)
             terms (filterv #(or (= :unknown (:status graph))
                                 (not (nat-int? (get fact %)))) [:cone :unblocks])]
         (if (seq terms)
           (graph-exclusion result target fact graph terms)
           (update result :field conj candidate))))
     {:field [] :excluded [] :requests []} field)))

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
         ;; TIED-OUTER-CASCADES: retain each cascade's pick beside its score,
         ;; so a tied score reads as same-pick, degenerate, or genuine
         ;; without replaying the field.
         :cascade-picks (mapv (fn [{:keys [steps]}]
                                (let [p (outer-score/cascade-pick
                                         steps (:facts observation) (:field observation))]
                                  (if p (:target p) :no-pick)))
                              menu)
         ;; TIED-OUTER-CASCADES (claude-1 decision): retain beside each pick
         ;; whether its relation was known, so a scored stop resting on an
         ;; unknown relation reads as ignorance in the record.  ::no-relation-fn
         ;; observations (pure fixtures with no relation producer) retain
         ;; {:relation ::no-relation-fn}.
         :cascade-pick-relations
         (mapv (fn [pick]
                 {:target pick
                  :relation (if (= :no-pick pick) :no-pick
                                (relation-class observation pick))
                  :relation-known? (if (= :no-pick pick) nil
                                       (known-relation? (relation-class observation pick)))})
               (mapv (fn [{:keys [steps]}]
                       (let [p (outer-score/cascade-pick
                                steps (:facts observation) (:field observation))]
                         (if p (:target p) :no-pick)))
                     menu))
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
