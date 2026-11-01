(ns futon2.aif.outer-select
  "Task choice with retained library evidence, corresponding to `R20Step.chosenCascadeEntry` and `step`."
  (:require [clojure.java.io :as io]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.outer-menu :as menu]
            [futon2.aif.outer-observation :as observation]
            [futon2.aif.outer-score :as outer-score]))

(defn- refuse! [kind data]
  (throw (ex-info "Outer selection refused" (cond-> (assoc data :kind kind)
                     (:selection-evidence data)
                     (assoc-in [:selection-evidence :outcome] {:status :refused :kind kind})))))

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

(defn- cascade-record [observation cascade model score]
  (let [candidate (:pick model) target (:target candidate)
        class (if candidate (relation-class observation target) :no-pick)
        receipt (get-in (value-of (:facts observation) target) [:relation :receipt])]
    (when (and receipt (not= class (:class receipt)))
      (refuse! :relation-receipt-class-mismatch {:target target :class class :receipt receipt}))
    (merge (update cascade :steps #(mapv (fn [i step] (outer-score/typed-step step (str (name (:id cascade)) "/step-" i))) (range) %)) score
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

(defn g-units
  "Same six-place HALF_EVEN quantization as the run-facts exporter. The inner
  cascade-selection namespace at this base exposes no g-units helper."
  [g]
  (-> (BigDecimal. (double g))
      (.setScale 6 java.math.RoundingMode/HALF_EVEN)
      (.movePointRight 6) .longValueExact))

(defn prediction-classes
  "Prediction outcomes (canonical digest) and quantized G identify one policy.
  Preserve menu order only to choose an arrangement within that policy."
  [cascades]
  (let [key-of #(vector (identity/digest (get-in % [:prediction :outcomes])) (g-units (:G %)))
        groups (group-by key-of cascades)]
    (mapv (fn [k]
            (let [members (get groups k) representative (first members)]
              (assert (every? #(= (second k) (g-units (:G %))) members))
              (merge (select-keys representative [:pick :G :terms :prediction :relation-basis])
                     {:id (identity/sha256 (pr-str k)) :prediction-digest (first k)
                      :g-units (second k) :members (mapv :id members)
                      :representative (:id representative)})))
          (distinct (map key-of cascades)))))

(defn choice-input-digest
  "Choice-scoped Q6 inputs. Whole-field reading/graph pins remain in the
  construction, not in the equality test for repeating a choice situation."
  [selection focus]
  (let [c (:construction selection)
        covers {:classes (vec (sort-by pr-str (map #(select-keys % [:prediction-digest :g-units :pick]) (:classes selection))))
                :abstention-g-units (g-units (get-in selection [:abstention :G]))
                :forbidden (vec (sort-by pr-str (:forbidden selection)))
                :focus focus :library (get-in c [:library :sha256]) :kernel (:kernel c)}]
    {:sha256 (identity/digest covers) :covers covers
     :excludes {:reading (:reading c) :graph (:graph c)
                :reason "whole-field pins change on every click; the field enters the digest through the classes it produces"}}))

(defn select
  "Score every cascade and stopping; only a unique minimum strictly below
  abstention can select. Refusals retain the same evidence as successful selection."
  [makeup state observation]
  (let [construction (menu/construct
                       (.getCanonicalPath (io/file (or (:task-selection-library-root makeup)
                         (str (System/getProperty "user.home") "/code/futon3/library/task-selection"))))
                       makeup observation)
        menu (:cascades construction)
        partition (known-term-field observation)
        observation (assoc observation :field (:field partition))
        field-facts (:field-facts observation)
        forbidden (or (:forbidden observation) #{})
        forbidden-evidence (vec (:forbidden-evidence observation))]
    (when-not (and (set? forbidden)
                   (= forbidden (set (map :target forbidden-evidence)))
                   (= (count forbidden) (count forbidden-evidence))
                   (every? #(and (contains? (set (map :target (:field observation))) (:target %))
                                 (keyword? (:reason %)) (string? (:pin %)) (seq (:pin %)))
                           forbidden-evidence))
      (refuse! :invalid-forbidden-evidence {:forbidden forbidden-evidence}))
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
                                             (cond-> (assoc (ex-data e) :construction construction) field-facts (assoc :field-facts field-facts)) e))))) menu)
          details (mapv #(outer-score/score-model score-input %) models)
          library (:library construction)
          cascades (mapv #(cascade-record observation %1 %2 %3) menu models details)
          scores (mapv :G cascades)
          abstention (outer-score/abstention-score score-input)
          legacy-relations (mapv (fn [c] {:target (:pick c) :relation (get-in c [:relation :class])
                                         :relation-known? (when-not (= :no-pick (:pick c))
                                                            (known-relation? (get-in c [:relation :class])))}) cascades)
          evidence (cond-> {:outer-score/admitted (mapv :target (:field partition))
                    :outer-score/excluded (:excluded partition)
                    :knowledge-producer-requests (:requests partition)
                    :library library :construction construction :cascades cascades :abstention abstention
                    :forbidden forbidden-evidence
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
      (let [all-classes (prediction-classes cascades)
            stop-digest (identity/digest (get-in abstention [:prediction :outcomes]))
            stopping (filterv #(= stop-digest (:prediction-digest %)) all-classes)
            classes (filterv #(not= stop-digest (:prediction-digest %)) all-classes)
            class-of (into {} (for [c all-classes member (:members c)] [member (:id c)]))
            evidence (-> evidence
                         (assoc :classes classes
                                :stopping-equivalent
                                (mapv (fn [c]
                                        {:class (:id c) :members (:members c) :pick (:pick c)
                                         :landing (get-in c [:prediction :landing])
                                         :g-units (:g-units c) :reason :predicts-stopping}) stopping)
                                :previous (or (:previous-selection observation)
                                              {:status :absent :reason :previous-selection-not-supplied})
                                :representative-rule :first-in-menu-order-within-one-prediction-class)
                         (assoc-in [:construction :policy-count] (count classes))
                         (update-in [:construction :cascades]
                                    #(mapv (fn [c] (assoc c :class (class-of (:id c)))) %)))
            evidence (assoc evidence :choice-input-digest (choice-input-digest evidence (:focus observation)))
            ;; Never choose a forbidden representative. Every arrangement remains
            ;; scored and retained; refusal cannot disappear through grouping.
            admissible (filterv #(not (contains? forbidden (:pick %))) classes)
            _ (when (and (seq classes) (empty? admissible))
                (refuse! :no-admissible-pick
                         {:cascades (mapv :id cascades) :forbidden forbidden-evidence
                          :selection-evidence evidence}))
            minimum (when (seq admissible) (reduce min (map :G admissible)))
            tied (filterv #(= minimum (:G %)) admissible)]
        (if (empty? classes)
          (assoc evidence :outcome {:status :abstained :case :all-policies-predict-stopping}
                 :selection nil :prediction (:prediction abstention))
          (do
        (when (< 1 (count tied))
          (refuse! :tie-at-minimum {:classes (mapv :id tied) :G minimum
                                   :cascades (mapv :representative tied)
                                   :tied-cascades (mapv :representative tied)
                                   :selection-evidence evidence}))
        (let [chosen-class (first tied)
              index (first (keep-indexed #(when (= (:representative chosen-class) (:id %2)) %1) cascades))
              chosen (nth cascades index) picked (:pick (nth models index))]
          (when-not (< minimum (:G abstention))
            (refuse! :pick-not-below-abstention {:cascade (:id chosen) :G minimum
                                               :abstention-G (:G abstention)
                                               :selection-evidence evidence}))
          (let [result (assoc evidence :outcome {:status :chosen} :chosen-class (:id chosen-class) :cascade-index index :cascade-id (:id chosen) :cascade-digest (:digest chosen)
                             :selection (when picked {:candidates (:field observation) :selected picked})
                             :prediction (:prediction chosen))]
            (assert (and (= (:cascade-scores result) (mapv :G (:cascades result)))
                         (= (:cascade-picks result) (mapv :pick (:cascades result)))
                         (= (:cascade-pick-relations result) legacy-relations)
                         (= (:prediction result) (:prediction (nth (:cascades result) index)))))
            result))))))))
