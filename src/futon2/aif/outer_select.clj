(ns futon2.aif.outer-select
  "Task choice with retained library evidence, corresponding to `R20Step.chosenCascadeEntry` and `step`."
  (:require [clojure.java.io :as io]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.q2-construction :as q2]
            [futon2.aif.outer-observation :as observation]
            [futon2.aif.outer-score :as outer-score]))

(defn- refuse! [kind data]
  (throw (ex-info "Outer selection refused" (assoc data :kind kind))))

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
  "Compatibility entrypoint: unknown metrics stay in the field. An order that
  needs an unavailable term refuses at apply-selection-step, never by silently
  restricting the candidate domain."
  [{:keys [field]}]
  {:field (vec field) :excluded [] :requests []})

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
            :reach (or (:reach (value-of (:facts observation) target))
                       {:value (:reach candidate)
                        :source (if candidate (or (:reach-source candidate)
                                                  :reach-source-unavailable) :no-pick)})})))

(defn select
  "Score every cascade and stopping; only a unique minimum strictly below
  abstention can select. Refusals retain the same evidence as successful selection."
  [makeup state observation]
  (let [menu (:cascades makeup)
        partition (known-term-field observation)
        observation (assoc observation :field (:field partition))
        field-facts (:field-facts observation)]
    (when field-facts
      (observation/validate-field! (:enumerated field-facts)
                                   (mapv :target (:field observation))
                                   (:excluded field-facts))
      (when-not (= (set (keys (:admitted field-facts)))
                   (set (map :target (:field observation))))
        (refuse! :task-field-facts-mismatch {})))
    (when (empty? (:field partition))
      (refuse! :outer-score/no-admitted-tasks
               {:field-facts field-facts :outer-score/excluded (:excluded field-facts)}))
    (when-not (seq menu) (refuse! :empty-cascade-menu {}))
    (let [score-input (merge (select-keys observation [:focus :relation :facts :field])
                             {:state-counts (:state-counts state) :ending-counts (:c state)
                              :gain-counts (:gain-counts state)})
          models (mapv (fn [cascade]
                         (try
                           ;; Apply the cascade once, including any function
                           ;; steps. The scorer receives that exact pick.
                           (let [candidate (outer-score/cascade-pick (:steps cascade)
                                                                    (:facts observation)
                                                                    (:field observation))]
                             (when (and field-facts candidate (not (number? (:reach candidate))))
                               (refuse! :outer-score/reach-unavailable
                                        {:target (:target candidate) :field-facts field-facts}))
                             (outer-score/outer-model
                              (assoc score-input :cascade [] :field (if candidate [candidate] []))))
                           (catch clojure.lang.ExceptionInfo e
                             (throw (ex-info (ex-message e)
                                             (cond-> (ex-data e) field-facts (assoc :field-facts field-facts)) e))))) menu)
          details (mapv #(outer-score/score-model score-input %) models)
          library (library-evidence makeup)
          cascades (mapv #(cascade-record library observation %1 %2 %3) menu models details)
          scores (mapv :G cascades)
          abstention (outer-score/abstention-score score-input)
          legacy-relations (mapv (fn [c] {:target (:pick c) :relation (get-in c [:relation :class])
                                         :relation-known? (when-not (= :no-pick (:pick c))
                                                            (known-relation? (get-in c [:relation :class])))}) cascades)
          evidence (cond-> {:outer-score/admitted (mapv :target (:field partition))
                    :outer-score/excluded (:excluded partition)
                    :knowledge-producer-requests (:requests partition)
                    :library library :cascades cascades :abstention abstention
                    :cascade-scores scores :cascade-picks (mapv :pick cascades)
                    :cascade-pick-relations legacy-relations}
                     field-facts (assoc :field-facts field-facts
                                        :giraffe/pass-sha256 (:giraffe/pass-sha256 observation)
                                        :default-reach-ignored (:default-reach-ignored observation)))
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
