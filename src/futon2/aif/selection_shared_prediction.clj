(ns futon2.aif.selection-shared-prediction
  "Shadow of outer-select's policy grouping, keyed on the SHARED scorer's own
  prediction instead of outer-score's (M-cascade-live-causal; Joe 2026-11-25
  19:34, \"Let's do that now\": retire the special-track task selection. This
  handoff is the shadow step: it changes NO decision; outer-select/select
  carries it under :shared-prediction-shadow and every existing key stays
  byte-identical).

  What is still special to selection (retirement-plan.md row C1): each
  cascade's :prediction :outcomes comes from outer-score/outer-model +
  score-model, fed by outer-state's carried :state-counts/:c/:gain-counts;
  prediction-classes groups cascades by the digest of those outcomes and
  stopping-equivalence compares them with abstention-score's prediction.

  The shared scorer (selection-canonical-g/score ->
  cascade-coapply-g/compare-at-horizon, the same G the work run uses) already
  predicts: each scored entry's [:certificate :steps] carries the per-step
  predicted :belief (cascade-observation-scoring/score-candidate), and the
  stopping entry has the same shape. So a policy's prediction can be read off
  the shared scorer with no outer-state at all.

  The shared prediction of an entry is its per-step belief trajectory
  ([:certificate :steps] -> [{:tau :belief}]) and NOTHING else:

  * The trajectory determines the ending class the selection channel emits:
    the class-emission observation model reads the horizon belief, so two
    entries with the same trajectory have the same predicted ending class at
    every tau -- the analogue of outer-score's :outcomes.
  * The trajectory already contains what differs between policies: which
    tokens the cascade's patterns produce and when. G, the fired-pattern
    parameters, and the pick are already separate components of the class
    key (g-units, parameters-digest) or of the class itself, so digesting
    them again here would only double-count them.
  * It excludes the certificate's scores (risk/ambiguity/want terms): those
    are the G terms, not the prediction; grouping on them would split
    policies that predict identically but score differently, which the old
    grouping never did either.

  A refused entry has no trajectory: its shared prediction is typed
  {:status :no-shared-prediction :reason ...} and it groups under that typed
  absence, never under a digest of nothing.

  Stopping-equivalence: an entry is stopping-equivalent under the shadow when
  its shared-prediction digest equals the canonical stopping entry's. With
  theta 1 the co-application transition is deterministic, so a cascade whose
  patterns never fire keeps the initial state at every step, exactly as the
  empty policy does -- the shared analogue of \"predicts stopping\"."
  (:require [futon2.aif.action-identity :as identity]))

(defn shared-prediction
  "The shared prediction of one canonical scored entry (a row of
  selection-canonical-g/score's :by-id or its :stopping): the per-step
  belief trajectory from its certificate. A refused entry has none, typed."
  [entry]
  (if (= :scored (:status entry))
    (let [steps (get-in entry [:certificate :steps])]
      (if (and (sequential? steps) (every? #(and (contains? % :tau) (contains? % :belief)) steps))
        {:status :ok
         :trajectory (mapv #(select-keys % [:tau :belief]) steps)}
        {:status :no-shared-prediction :reason :certificate-steps-incomplete
         :entry (:cascade-id entry)}))
    {:status :no-shared-prediction :reason (or (:status entry) :unknown)
     :refusal (:refusal entry) :entry (:cascade-id entry)}))

(defn shared-prediction-digest
  "Canonical digest of an entry's shared prediction trajectory, or nil when
  the entry has none (typed absence is the caller's key component instead)."
  [entry]
  (let [sp (shared-prediction entry)]
    (when (= :ok (:status sp))
      (identity/digest (:trajectory sp)))))

(defn canonical-prediction-classes
  "The same shape as outer-select/prediction-classes but keyed on
  [shared-prediction digest, g-units, parameters-digest]: the prediction is
  the shared scorer's own trajectory, so no outer-state feeds the grouping.

  ENTRIES: canonical entries (scored or refused). CASCADES: the corresponding
  outer-select cascade records (same order as in the evidence), which carry
  :G and [:prediction :parameters]. g-units-fn and parameters-digest-fn are
  outer-select/g-units and outer-select/parameters-digest, passed in so this
  namespace stays off outer-select's require path (outer-select requires
  this one for the shadow). Returns classes with :id :members
  :representative :g-units :parameters-digest :shared-prediction-digest
  (:no-shared-prediction typed when a member entry is refused)."
  [cascades canonical-by-id g-units-fn parameters-digest-fn]
  (let [key-of (fn [c]
                 (let [entry (get canonical-by-id (:id c))
                       sp (shared-prediction entry)]
                   (if (= :ok (:status sp))
                     [(identity/digest (:trajectory sp))
                      (g-units-fn (:G c))
                      (parameters-digest-fn c)]
                     [(:reason sp)
                      (g-units-fn (:G c))
                      (parameters-digest-fn c)])))
        groups (group-by key-of cascades)]
    (mapv (fn [k]
            (let [members (get groups k) representative (first members)
                  refused? (not (string? (first k)))]
              (assert (every? #(= (second k) (g-units-fn (:G %))) members))
              (cond-> {:id (identity/sha256 (pr-str (if (nth k 2) k (subvec k 0 2))))
                       :g-units (second k)
                       :parameters-digest (nth k 2)
                       :members (mapv :id members)
                       :representative (:id representative)
                       :pick (:pick representative)}
                (not refused?) (assoc :shared-prediction-digest (first k))
                refused? (assoc :no-shared-prediction {:reason (first k)}))))
          (distinct (map key-of cascades)))))

(defn shadow
  "The shadow evidence map for one selection: the shared-prediction grouping
  computed BESIDE the existing one. CASCADES are the evidence's final cascade
  records (post canonical-g), CANONICAL is selection-canonical-g/score's
  return, and OLD-CLASSES/:stopping-equivalent are the existing grouping's
  values for comparison. Pure; never changes any decision.

  :would-differ? is true when the shadow grouping and the existing grouping
  disagree on the policy count, on which cascades are stopping-equivalent,
  or on the chosen class membership (comparing member sets, since class ids
  hash different keys). :differences lists each disagreement, typed.

  g-units-fn and parameters-digest-fn are outer-select's, passed in (see
  canonical-prediction-classes)."
  [cascades canonical old-classes old-stopping-equivalent chosen-class g-units-fn parameters-digest-fn]
  (let [by-id (:by-id canonical)
        stopping-entry (:stopping canonical)
        stop-digest (shared-prediction-digest stopping-entry)
        all-classes (canonical-prediction-classes cascades by-id g-units-fn parameters-digest-fn)
        stopping (filterv #(= stop-digest (:shared-prediction-digest %)) all-classes)
        classes (filterv #(not= stop-digest (:shared-prediction-digest %)) all-classes)
        class-of (into {} (for [c all-classes member (:members c)] [member (:id c)]))
        old-class-of (into {} (for [c (concat old-classes old-stopping-equivalent)
                                    member (:members c)]
                                [member (:id c)]))
        old-stopping-members (set (mapcat :members old-stopping-equivalent))
        new-stopping-members (set (mapcat :members stopping))
        old-groups (set (map (comp set :members) (concat old-classes old-stopping-equivalent)))
        new-groups (set (map (comp set :members) all-classes))
        chosen-members (when chosen-class
                         (some #(when (= chosen-class (:id %)) (set (:members %))) old-classes))
        chosen-new-class (when chosen-class (some #(when (and chosen-members
                                                              (some chosen-members (:members %)))
                                                     %) classes))
        differences (cond-> []
                      (not= (count old-classes) (count classes))
                      (conj {:kind :policy-count
                             :old (count old-classes) :new (count classes)})
                      (not= old-stopping-members new-stopping-members)
                      (conj {:kind :stopping-equivalence
                             :old (vec (sort-by pr-str old-stopping-members))
                             :new (vec (sort-by pr-str new-stopping-members))})
                      (not= old-groups new-groups)
                      (conj {:kind :grouping
                             :old (vec (sort-by pr-str old-groups))
                             :new (vec (sort-by pr-str new-groups))})
                      (and chosen-class chosen-new-class
                           (not= chosen-members (set (:members chosen-new-class))))
                      (conj {:kind :chosen-class-membership
                             :class chosen-class
                             :old (vec (sort-by pr-str chosen-members))
                             :new (vec (sort-by pr-str (:members chosen-new-class)))}))]
    {:schema :wm/shared-prediction-shadow-v1
     :classes (mapv #(select-keys % [:id :members :representative :pick
                                     :shared-prediction-digest :g-units
                                     :parameters-digest]) classes)
     :stopping-equivalent (mapv (fn [c]
                                  {:class (:id c) :members (:members c) :pick (:pick c)
                                   :g-units (:g-units c)
                                   :shared-prediction-digest (:shared-prediction-digest c)
                                   :reason :predicts-stopping})
                                stopping)
     :policy-count (count classes)
     :stopping-shared-prediction-digest stop-digest
     :class-of class-of
     :old-class-of old-class-of
     :would-differ? (boolean (seq differences))
     :differences differences}))
