(ns futon2.aif.outer-select
  "Task choice with retained library evidence, corresponding to `R20Step.chosenCascadeEntry` and `step`."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.outer-argue :as outer-argue]
            [futon2.aif.outer-menu :as menu]
            [futon2.aif.outer-observation :as observation]
            [futon2.aif.outer-score :as outer-score]))

(declare value-of)

(defn- argument-frame [input]
  (try
    (outer-argue/frame input)
    (catch Throwable error
      {:schema :cascade/argument-frame-v1
       :status :unavailable
       :reason (or (ex-data error) (ex-message error) (str error))})))

(defn source-header
  "Parse the three operator-facing header fields and compare the live bytes to PIN."
  [{:keys [path sha256]}]
  (if-not path
    {:path :absent :sha256 :absent :pin :absent
     :header {:date :absent :status :absent :timebox :absent}}
    (let [file (io/file path)]
      (if-not (.isFile file)
        ;; The pinned source is gone or unreadable at selection time: typed, so
        ;; a renamed task file cannot fail the selection it is explaining.
        {:path path :sha256 :absent :pin :unreadable
         :header {:date :absent :status :absent :timebox :absent}}
        (let [bytes (java.nio.file.Files/readAllBytes (.toPath file))
              text (String. bytes "UTF-8")
              digest (identity/sha256 text)
              lines (take 30 (str/split-lines text))
              field (fn [label]
                      (or (some #(second (re-matches
                                          (re-pattern (str "\\*\\*" label ":?\\*\\*:?\\s*(.+)")) %))
                                lines)
                          :absent))]
          {:path (.getCanonicalPath file)
           :sha256 digest
           :pin (if (= sha256 digest) :matches :drifted)
           :header {:date (field "Date") :status (field "Status") :timebox (field "Timebox")}})))))

(defn- fmt4 [x]
  (String/format java.util.Locale/ROOT "%.4f" (to-array [(double x)])))
(defn- short-header [header]
  (let [short #(if (= :absent %) "absent" (first (str/split % #" maximum| —| \(")))]
    (str "Header: dated " (short (:date header)) ", status " (short (:status header))
         ", timebox " (short (:timebox header)) ".")))

(defn- render-step [step]
  (cond
    (= :ready-gate step) "ready gate"
    (= :placed-gate step) "placed gate"
    (map? step) (str "status gate " (:status-gate step))
    :else (name step)))

(defn- render-order-value [step value]
  (str (case step :by-rank "rank " :by-cone "cone " :by-unblocks "unblocks " "") value))

(defn render-selection-why
  "The sole renderer for typed selection explanations."
  [kind row]
  (case kind
    :class
    (let [steps (:steps row)
          gate (first (filter #(contains? % :out) steps))
          orders (filter #(contains? % :pick-value) steps)
          order-text (str/join "; " (map (fn [{:keys [step pick-value runner-up]}]
                                            (str (render-step step) " took "
                                                 (render-order-value step pick-value)
                                                 (when runner-up
                                                   (str " (runner-up " (:target runner-up) " at "
                                                        (render-order-value step (:value runner-up)) ")")))) orders))
          rank-note (if (= :computed-before-reading (get-in row [:giraffe :rank-basis]))
                      (str " Rank was computed before the reading stage demoted it (prerequisite "
                           (first (get-in row [:giraffe :prerequisites])) " unresolved).") "")
          altitude (get-in row [:terms :altitude])
          ways (get-in row [:placement :ways])
          action (get-in row [:placement :action])]
      (str (str/join ", " (map name (:cascades row))) " picked " (:pick row) ": "
           (when gate (str (render-step (:step gate)) " left " (:out gate) " of " (:in gate) "; "))
           order-text "." rank-note " " (short-header (get-in row [:source :header]))
           " G " (fmt4 (:G row))
           (when altitude (str " (+" altitude " altitude: predicts stopping)")) "."
           (when (seq ways)
             (str " Serves " (first ways)
                  (when-let [also (second ways)] (str ", also " also))
                  " (" action ")."))))
    :abstention
    (str "stopping: G " (fmt4 (:G row)) " = risk " (fmt4 (get-in row [:terms :risk]))
         " + altitude " (get-in row [:terms :altitude]) " ("
         (get-in row [:altitude-source :resource]) " "
         (subs (get-in row [:altitude-source :sha256]) 0 6) "…)")
    :decision
    (str "chose " (:pick row) " (G " (fmt4 (:G row)) ") over stopping ("
         (fmt4 (:abstention-G row)) ") and " (:other-class-count row)
         " other class; margin to stopping " (fmt4 (:margin-to-abstention row)) " nats")))

(defn- why-class [class cascades traces facts]
  (let [members (set (:members class))
        arrangements (filter #(members (:id %)) cascades)
        cascade (last (sort-by #(count (:steps %)) arrangements))
        fact (value-of facts (:pick class))
        prerequisites (mapv :to (get-in fact [:graph-terms :prerequisites]))
        source (source-header (get-in fact [:dependency-reading :source]))
        row {:class (:id class) :pick (:pick class) :cascades (:members class)
             :steps (get traces (:id cascade))
             :giraffe {:rank (:rank fact) :status (:status fact)
                       :reason (get-in fact [:graph-terms :reason])
                       :unblocks (:unblocks fact) :cone (:cone fact)
                       ;; GR-2 labels the Giraffe row itself once ranks are
                       ;; reassigned after reading; prefer the row's own label.
                       :rank-basis (or (:rank-basis fact)
                                       (if (and (:rank fact) (not= :ready (:status fact)))
                                         :computed-before-reading :as-ranked))
                       :prerequisites prerequisites
                       :evidence (get-in fact [:graph-terms :prerequisites 0 :evidence])}
             :placement (select-keys (:placement fact) [:ways :action :status :wing])
             :source source :terms (:terms class) :G (:G class)}]
    (assoc row :rendered (render-selection-why :class row))))

(defn selection-why [classes cascades abstention traces facts outcome chosen-class]
  (let [class-rows (mapv #(why-class % cascades traces facts) classes)
        ordered (sort-by :G class-rows)
        class-rows (mapv (fn [row]
                           (let [others (remove #(= (:class row) (:class %)) ordered)]
                             (assoc row
                                    :margin-to-next (when-let [next (first others)]
                                                      (Math/abs (double (- (:G next) (:G row)))))
                                    :margin-to-abstention (- (:G abstention) (:G row)))))
                         class-rows)
        abstention (assoc abstention :altitude-source
                          (assoc (:altitude-source abstention)
                                 :resource outer-score/abstention-altitude-resource))
        abstention (assoc abstention :rendered (render-selection-why :abstention abstention))
        chosen (some #(when (= chosen-class (:class %)) %) class-rows)
        decision (when chosen
                   (let [row {:status (:status outcome) :pick (:pick chosen) :G (:G chosen)
                              :abstention-G (:G abstention)
                              :other-class-count (dec (count class-rows))
                              :margin-to-abstention (- (:G abstention) (:G chosen))}]
                     (assoc row :rendered (render-selection-why :decision row))))]
    {:schema :wm/selection-why-v1 :classes class-rows :abstention abstention :decision decision}))

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

(defn- value-of [x k] (if (fn? x) (x k) (get x k)))

(defn known-term-field
  "Keep only candidates with a Lean-recognised target relation in the scoring
  field. Unknown relations remain in the upstream enumerated census, with the
  receipt's absence reason and authority pins retained in a typed exclusion."
  [{:keys [field focus relation facts]}]
  (reduce
   (fn [partition candidate]
     (let [target (:target candidate)
           class (relation focus target)
           receipt (get-in (value-of facts target) [:relation :receipt])]
       (if (known-relation? class)
         (update partition :field conj candidate)
         (update partition :excluded conj
                 (cond-> {:target target
                          :reason :relation-unrecognised
                          :relation-class class
                          :relation-reason (or (get-in receipt [:relation :reason])
                                               :relation-receipt-unavailable)
                          :relation-receipt-digest (when receipt
                                                     (identity/sha256 (pr-str receipt)))}
                   (get-in receipt [:derived-via :corpus-sha256])
                   (assoc :corpus-sha256 (get-in receipt [:derived-via :corpus-sha256]))
                   (get-in receipt [:derived-via :focus-manifest-sha256])
                   (assoc :focus-manifest-sha256
                          (get-in receipt [:derived-via :focus-manifest-sha256])))))))
   {:field [] :excluded [] :requests []}
   field))

(defn apply-c1-exclusions [partition original-field exclusions]
  (let [targets (set (map :target original-field))]
    (when-not (and (= (count exclusions) (count (set (map :target exclusions))))
                   (every? #(and (contains? targets (:target %))
                                 (contains? #{:c1-already-satisfied
                                              :c2-legitimate-question
                                              :c3-harm-declined}
                                            (:reason %))
                                 (string? (:pin %)) (seq (:pin %))) exclusions))
      (refuse! :invalid-c1-exclusions {:c1-exclusions exclusions}))
    (let [excluded-targets (set (map :target exclusions))]
      (-> partition
          (update :field #(filterv (comp not excluded-targets :target) %))
          (update :excluded into exclusions)))))

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

(defn per-task-ratings
  "Diagnostic conditional G: score each admitted candidate as the pick, using
  the click's existing relation producer and score inputs. This does not run a
  selection cascade or affect eligibility, classes, Q6 inputs or the decision.
  Unformable models remain rows with typed absence, never new refusals."
  [score-input]
  (mapv
   (fn [candidate]
     (let [target (:target candidate)]
       (try
         (let [fact (value-of (:facts score-input) target)
               relation (relation-class score-input target)
               row (merge {:target target :relation {:class relation}}
                          (select-keys fact [:status :rank :cone :unblocks]))]
           (if (= ::no-relation-fn relation)
             (assoc row :absence {:kind :relation-producer-unavailable})
             (if-not (and (number? (:reach candidate))
                          (Double/isFinite (double (:reach candidate))))
               (assoc row :absence {:kind :reach-unavailable})
               (let [input (assoc score-input :cascade [] :field [candidate]
                                  :relation (fn [_ _] relation))
                     score (outer-score/score-model input (outer-score/outer-model input))
                     g (:G score)]
                 (if (and (number? g) (Double/isFinite (double g)))
                   (merge row (select-keys score [:terms :G :altitude-source])
                          {:prediction-digest (identity/digest (get-in score [:prediction :outcomes]))})
                   (assoc row :absence {:kind :score-inadmissible :score g}))))))
         (catch Exception e
           {:target target
            :relation {:status :absent :reason :rating-model-unavailable}
            :absence {:kind (or (:kind (ex-data e)) :model-unavailable)
                      :message (ex-message e)}}))))
   (:field score-input)))

(defn rating-top-ten
  "Lowest diagnostic G; equal G is displayed by target id, not adjudicated.
  This display order does not break a selection tie. Absence rows stay outside."
  [ratings]
  (vec (take 10 (sort-by (juxt :G :target) (filter #(number? (:G %)) ratings)))))


(defn valid-forbidden-evidence?
  "The exact check `select` applies to `:forbidden`/`:forbidden-evidence`:
  one entry per forbidden target, each in the field, with a keyword reason
  and a non-blank string pin."
  [forbidden forbidden-evidence field]
  (and (set? forbidden)
       (= forbidden (set (map :target forbidden-evidence)))
       (= (count forbidden) (count forbidden-evidence))
       (every? #(and (contains? (set (map :target field)) (:target %))
                     (keyword? (:reason %)) (string? (:pin %)) (seq (:pin %)))
               forbidden-evidence)))

(defn select
  "Score every cascade and stopping; only a unique minimum strictly below
  abstention can select. Refusals retain the same evidence as successful selection."
  [makeup state observation]
  (let [library-root (.getCanonicalPath
                      (io/file (or (:task-selection-library-root makeup)
                                   (str (System/getProperty "user.home")
                                        "/code/futon3/library/task-selection"))))
        construction (menu/construct library-root makeup observation)
        menu (:cascades construction)
        partition (apply-c1-exclusions
                   (known-term-field observation) (:field observation)
                   (vec (:c1-exclusions observation)))
        field-facts (:field-facts observation)
        forbidden (or (:forbidden observation) #{})
        forbidden-evidence (vec (:forbidden-evidence observation))]
    (when-not (valid-forbidden-evidence? forbidden forbidden-evidence (:field observation))
      (refuse! :invalid-forbidden-evidence {:forbidden forbidden-evidence}))
    (when field-facts
      (observation/validate-field! (:enumerated field-facts)
                                   (mapv :target (:field observation))
                                   (:excluded field-facts))
      (when-not (= (set (keys (:admitted field-facts)))
                   (set (map :target (:field observation))))
        (refuse! :task-field-facts-mismatch {})))
    (let [observation (assoc observation :field (:field partition))]
    (when (empty? (:field partition))
      (refuse! :outer-score/no-admitted-tasks
               {:field-facts field-facts :outer-score/admitted []
                :outer-score/excluded (:excluded partition)}))
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
          traces (into {} (map (fn [cascade]
                                 [(:id cascade)
                                  (if (some #(and (map? %)
                                                  (or (contains? % :gate) (contains? % :order)))
                                            (:steps cascade))
                                    []
                                    (:trace (outer-score/apply-selection-cascade-traced
                                             (:steps cascade) (:facts observation)
                                             (:field observation))))]) menu))
          details (mapv #(outer-score/score-model score-input %) models)
          library (:library construction)
          cascades (mapv #(cascade-record observation %1 %2 %3) menu models details)
          scores (mapv :G cascades)
          abstention (outer-score/abstention-score score-input)
          ratings (per-task-ratings score-input)
          legacy-relations (mapv (fn [c] {:target (:pick c) :relation (get-in c [:relation :class])
                                         :relation-known? (when-not (= :no-pick (:pick c))
                                                            (known-relation? (get-in c [:relation :class])))}) cascades)
          evidence (cond-> {:outer-score/admitted (mapv :target (:field partition))
                    :outer-score/excluded (:excluded partition)
                    :knowledge-producer-requests (:requests partition)
                    :library library :construction construction :cascades cascades :abstention abstention
                    :per-task-rating ratings :top-ten (rating-top-ten ratings)
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
            tied (filterv #(= minimum (:G %)) admissible)
            prospective (when (= 1 (count tied)) (first tied))
            prospective (when (and prospective (< (:G prospective) (:G abstention))) prospective)
            why (selection-why all-classes cascades abstention traces (:facts observation)
                               (if prospective {:status :chosen} {:status :abstained})
                               (:id prospective))
            argument (argument-frame {:why why :cascades cascades
                                      :relations legacy-relations
                                      :per-task-rating ratings
                                      :library-root library-root})
            evidence (assoc evidence :why why :argument argument)]
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
            result)))))))))
