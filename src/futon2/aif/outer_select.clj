(ns futon2.aif.outer-select
  "Task choice with retained library evidence, corresponding to `R20Step.chosenCascadeEntry` and `step`."
  (:require [clojure.java.io :as io]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.q2-construction :as q2]
            [futon2.aif.effective-run-history :as effective-run-history]
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

(defn- value-of [x k] (if (fn? x) (x k) (get x k)))

(defn- library-evidence [makeup]
  (q2/library-manifest
   (.getCanonicalPath
    (io/file (or (:task-selection-library-root makeup)
                 (str (System/getProperty "user.home") "/code/futon3/library/task-selection"))))))

(defn- cascade-identity [library cascade]
  (let [units (mapv (fn [index pattern]
                      (let [safe? (and (string? pattern) (re-matches #"[A-Za-z0-9_-]+" pattern))
                            file (when safe? (io/file (:root library) (str pattern ".flexiarg")))]
                        {:pattern (str "futon3/library/task-selection/" pattern ".flexiarg")
                         :index index
                         :sha256 (get-in library [:patterns pattern])
                         :status (if (and file (.isFile file)) :present :absent)}))
                    (range) (:patterns cascade))
        carrier [(name (:id cascade)) (:readme-cascade cascade)
                 (mapv (fn [u] [(:pattern u) (:sha256 u) (:index u)]) units)
                 (:edges cascade)]]
    {:id (:id cascade) :digest (identity/sha256 (pr-str carrier))
     :digest-scheme :sha256-pr-str-name-readme-units-edges
     :lineage-status (if (and (:readme-cascade cascade) (:edges cascade)) :present :absent)
     :lineage-note "Absent README row/edges remain nil; these makeup digests need not equal witness digests."
     :readme-cascade (:readme-cascade cascade) :units units :edges (:edges cascade)
     :steps (mapv (fn [index step]
                    (outer-score/typed-step step (str (name (:id cascade)) "/step-" index)))
                  (range) (:steps cascade))}))

(defn- cascade-record [library observation cascade model score]
  (let [candidate (:pick model) target (:target candidate)
        class (if candidate (relation-class observation target) :no-pick)
        receipt (get-in (value-of (:facts observation) target) [:relation :receipt])]
    (when (and receipt (not= class (:class receipt)))
      (refuse! :relation-receipt-class-mismatch {:target target :class class :receipt receipt}))
    (merge (cascade-identity library cascade) score
           {:pick (or target :no-pick)
            :pick-facts (when candidate
                          (select-keys (value-of (:facts observation) target)
                                       [:status :cone :unblocks :rank]))
            :relation (cond-> {:class class
                              :receipt-digest (when receipt (identity/sha256 (pr-str receipt)))}
                        (nil? receipt) (assoc :status :absent :reason :receipt-unavailable))
            :reach {:value (:reach candidate)
                    :source (if candidate (or (:reach-source candidate)
                                               (:reach-source (value-of (:facts observation) target))
                                               :reach-source-unavailable) :no-pick)}})))

(defn select
  "Score every cascade and stopping; only a unique minimum strictly below
  abstention can select. Refusals retain the same evidence as successful selection."
  [makeup state observation]
  (let [menu (:cascades makeup)
        partition (known-term-field observation)
        duplicate-excluded (duplicate-effective-run-exclusions (:field partition) observation)
        duplicate-targets (set (map :target duplicate-excluded))
        partition (-> partition
                      (update :field #(vec (remove (fn [c] (contains? duplicate-targets (:target c))) %)))
                      (update :excluded into duplicate-excluded))
        observation (assoc observation :field (:field partition))]
    (when (empty? (:field partition))
      (refuse! (if (seq duplicate-excluded) :outer-score/duplicate-effective-run-field-emptied
                   :outer-score/no-known-graph-members)
               (cond-> {:outer-score/excluded (:excluded partition)
                        :knowledge-producer-requests (:requests partition)}
                 (seq duplicate-excluded) (assoc :duplicate-effective-run duplicate-excluded))))
    (when-not (seq menu) (refuse! :empty-cascade-menu {}))
    (let [score-input (merge (select-keys observation [:focus :relation :facts :field])
                             {:state-counts (:state-counts state) :ending-counts (:c state)
                              :gain-counts (:gain-counts state)})
          models (mapv #(outer-score/outer-model (assoc score-input :cascade (:steps %))) menu)
          details (mapv #(outer-score/score-model score-input %) models)
          library (library-evidence makeup)
          cascades (mapv #(cascade-record library observation %1 %2 %3) menu models details)
          scores (mapv :G cascades)
          abstention (outer-score/abstention-score score-input)
          legacy-relations (mapv (fn [c] {:target (:pick c) :relation (get-in c [:relation :class])
                                         :relation-known? (when-not (= :no-pick (:pick c))
                                                            (known-relation? (get-in c [:relation :class])))}) cascades)
          evidence {:outer-score/admitted (mapv :target (:field partition))
                    :outer-score/excluded (:excluded partition)
                    :knowledge-producer-requests (:requests partition)
                    :library library :cascades cascades :abstention abstention
                    :cascade-scores scores :cascade-picks (mapv :pick cascades)
                    :cascade-pick-relations legacy-relations}
          finite? #(and (number? %) (Double/isFinite (double %)))]
      (when-let [bad (first (keep-indexed (fn [i g] (when-not (finite? g) {:index i :score g})) scores))]
        (refuse! :cascade-score-inadmissible (merge evidence bad)))
      (when-not (finite? (:G abstention))
        (refuse! :abstention-score-inadmissible evidence))
      (let [minimum (reduce min scores)
            indices (vec (keep-indexed #(when (= minimum %2) %1) scores))
            tied (mapv #(:id (nth menu %)) indices)]
        (when (< 1 (count indices))
          (refuse! :tie-at-minimum {:cascades tied :G minimum :tied-cascades tied
                                   :selection-evidence evidence}))
        (let [index (first indices) chosen (nth cascades index) picked (:pick (nth models index))]
          (when-not (< minimum (:G abstention))
            (refuse! :pick-not-below-abstention {:cascade (:id chosen) :G minimum
                                               :abstention-G (:G abstention)
                                               :selection-evidence evidence}))
          (let [result (assoc evidence :cascade-index index :cascade-id (:id chosen)
                             :selection (when picked {:candidates (:field observation) :selected picked})
                             :prediction (:prediction chosen))]
            (assert (and (= (:cascade-scores result) (mapv :G (:cascades result)))
                         (= (:cascade-picks result) (mapv :pick (:cascades result)))
                         (= (:cascade-pick-relations result) legacy-relations)
                         (= (:prediction result) (:prediction (nth (:cascades result) index)))))
            result))))))
