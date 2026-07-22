(ns futon2.aif.morning-brief
  "Append-only operator QA for completed full-loop opportunities.

  The opportunity runner writes one item. The operator later records a typed
  verdict; that review projects to an entity-grain A-matrix event consumed by
  the next War Machine judgement. Items and reviews are immutable files."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str])
  (:import [java.nio.file Files StandardOpenOption]
           [java.time Instant]
           [java.util UUID]))

(def default-root "/home/joe/code/futon2/data/wm-morning-brief")

(def objective-order
  [:operator-gate :feature-verdict :selection-quality :substantive-achievement
   :evidence-sufficiency :machine-response])

(def objective-specs
  {:operator-gate
   {:question "Disposition the mission's operator gate."
    :answers #{:acknowledged :resolved :deferred}
    :use "Operator-only disposition; it does not project to the A-matrix."}
   :feature-verdict
   {:question "Accept the built feature?"
    :answers #{:accept-feature :accept-with-follow-ups :reject}
    :use "Feature-acceptance verdict; it does not project to the A-matrix."}
   :selection-quality
   {:question "Was this the best available policy selection?"
    :answers #{:yes :no :uncertain}
    :use "Calibration evidence for the target-value model; never an A-matrix observation."}
   :substantive-achievement
   {:question "Did the result substantively advance the selected target?"
    :answers #{:yes :partial :no :uncertain}
    :use "A realized target-state judgment; projected to A only when a grounded entity target exists."}
   :evidence-sufficiency
   {:question "Are the commit, validation, review, and grounding evidence sufficient?"
    :answers #{:sufficient :insufficient :uncertain}
    :use "Audit evidence; it does not masquerade as a sensory observation."}
   :machine-response
   {:question "If the click failed, did the machine stop, remember, and prescribe an adequate discharge?"
    :answers #{:correct :incorrect :uncertain}
    :use "Control-loop evidence for stop-line calibration; not an A-matrix observation."}})

(def achievement-answer->event-type
  {:yes :strengthened :partial :refined :no :falsified
   :uncertain :refined})

(def addendum-kinds #{:repro :why-built :note})

(defn- nonblank-string? [value]
  (and (string? value) (not (str/blank? value))))

(defn- write-new! [path value]
  (let [file (io/file path)]
    (io/make-parents file)
    (Files/write (.toPath file)
                 (.getBytes (with-out-str (pp/pprint value)) "UTF-8")
                 (into-array StandardOpenOption
                             [StandardOpenOption/CREATE_NEW
                              StandardOpenOption/WRITE]))
    (.getPath file)))

(defn- edn-files [dir]
  (->> (or (.listFiles (io/file dir)) [])
       (filter #(.isFile %))
       (filter #(str/ends-with? (.getName %) ".edn"))
       (sort-by #(.getName %))))

(defn read-records [dir]
  (mapv #(edn/read-string (slurp %)) (edn-files dir)))

(defn queue-item!
  ([item] (queue-item! default-root item))
  ([root {:keys [attempt-id] :as item}]
   (when-not (and (string? attempt-id) (not (str/blank? attempt-id)))
     (throw (ex-info "Morning Brief item requires attempt-id" {:item item})))
   (write-new! (io/file root "items" (str attempt-id ".edn"))
               (assoc item :queued-at (str (Instant/now))
                           :morning-brief/schema-version 1))))

(declare reviews items)

(defn- open-operator-gate-item
  [root mission gate-kind]
  (let [reviewed-attempts (set (map :attempt-id (reviews root)))]
    (some (fn [item]
            (let [operator-action (:operator-action item)]
              (when (and (= :mission-gate (:type operator-action))
                         (= mission (:mission operator-action))
                         (= gate-kind (:gate-kind operator-action))
                         (not (contains? reviewed-attempts (:attempt-id item))))
                item)))
          (items root))))

(defn queue-operator-gate!
  "Queue one typed operator gate unless the same mission+kind is already open."
  ([operator-action] (queue-operator-gate! default-root operator-action))
  ([root {:keys [mission gate-kind gate-text date] :as operator-action}]
   (when-not (and (nonblank-string? (str mission))
                  (nonblank-string? (str gate-kind))
                  (nonblank-string? gate-text)
                  (nonblank-string? date))
     (throw (ex-info "Operator gate requires mission, kind, text, and date"
                     {:operator-action operator-action})))
   (if-let [open-item (open-operator-gate-item root mission gate-kind)]
     {:status :already-open
      :attempt-id (:attempt-id open-item)}
     (let [attempt-id (str "operator-gate-" (UUID/randomUUID))
           item {:attempt-id attempt-id
                 :outcome :operator-action-required
                 :selected-target mission
                 :operator-action (assoc operator-action :type :mission-gate)}
           ref (queue-item! root item)]
       {:status :queued :attempt-id attempt-id :ref ref}))))

(defn item-objectives [item]
  (if (:operator-action item)
    [:operator-gate]
    (cond-> []
      (:commit item)
      (conj :feature-verdict)
      true
      (conj :selection-quality :substantive-achievement)
      (or (:commit item) (seq (get-in item [:achievement :build])))
      (conj :evidence-sufficiency)
      (or (and (:outcome item) (not= :grounded-change (:outcome item)))
          (:failure item))
      (conj :machine-response))))

(defn- item-by-attempt [root attempt-id]
  (some #(when (= attempt-id (:attempt-id %)) %)
        (read-records (io/file root "items"))))

(defn- belief-event-for [review-id item objective answer]
  (when (= :substantive-achievement objective)
    (when-let [entity-id (get-in item [:qa-targets :achievement :entity-id])]
      {:event-id review-id
       :entity-id entity-id
       :type (get achievement-answer->event-type answer)
       :weight 1.0
       :source :morning-brief-qa
       :objective objective})))

(defn review!
  ([attempt-id objective answer note reviewer]
   (review! default-root attempt-id objective answer note reviewer))
  ([root attempt-id objective answer note reviewer]
   (let [item (item-by-attempt root attempt-id)
         spec (get objective-specs objective)
         prior-review (some #(when (and (= attempt-id (:attempt-id %))
                                         (= objective (:objective %)))
                                %)
                            (reviews root))]
     (when-not item
       (throw (ex-info "Unknown Morning Brief attempt" {:attempt-id attempt-id})))
     (when-not (some #{objective} (item-objectives item))
       (throw (ex-info "QA objective does not apply to this attempt"
                       {:attempt-id attempt-id :objective objective
                        :applicable (item-objectives item)})))
     (when-not (contains? (:answers spec) answer)
       (throw (ex-info "Unknown Morning Brief answer"
                       {:objective objective :answer answer
                        :allowed (:answers spec)})))
     (when prior-review
       (throw (ex-info "Morning Brief objective was already reviewed"
                       {:attempt-id attempt-id :objective objective
                        :review-id (:morning-brief/review-id prior-review)})))
     (let [review-id
           (str "mbqa-"
                (UUID/nameUUIDFromBytes
                 (.getBytes (str attempt-id "\u0000" (name objective)) "UTF-8")))
           record {:morning-brief/review-id review-id
                   :attempt-id attempt-id
                   :objective objective
                   :answer answer
                   :note note
                   :reviewer reviewer
                   :reviewed-at (str (Instant/now))
                   :qa-target (get-in item [:qa-targets objective])
                   :belief-event (belief-event-for review-id item objective answer)}]
       (write-new! (io/file root "reviews" (str review-id ".edn")) record)
       record))))

(defn addendum!
  "Append a reproducibility or rationale note to an existing attempt."
  ([attempt-id kind title body author]
   (addendum! default-root attempt-id kind title body author))
  ([root attempt-id kind title body author]
   (when-not (item-by-attempt root attempt-id)
     (throw (ex-info "Unknown Morning Brief attempt" {:attempt-id attempt-id})))
   (when-not (contains? addendum-kinds kind)
     (throw (ex-info "Unknown Morning Brief addendum kind"
                     {:kind kind :allowed addendum-kinds})))
   (when-not (every? nonblank-string? [title body author])
     (throw (ex-info "Morning Brief addendum title, body, and author must be non-blank strings"
                     {:title title :body body :author author})))
   (let [addendum-id (str "mba-" (UUID/randomUUID))
         record {:morning-brief/addendum-id addendum-id
                 :attempt-id attempt-id
                 :kind kind
                 :title title
                 :body body
                 :author author
                 :created-at (str (Instant/now))
                 :morning-brief/schema-version 1}]
     (write-new! (io/file root "addenda" (str addendum-id ".edn")) record)
     record)))

(defn reviews
  ([] (reviews default-root))
  ([root] (read-records (io/file root "reviews"))))

(defn items
  ([] (items default-root))
  ([root] (read-records (io/file root "items"))))

(defn addenda
  ([] (addenda default-root))
  ([root]
   (->> (read-records (io/file root "addenda"))
        (sort-by :created-at)
        vec)))

(defn with-pending-objectives [item review-records]
  (let [reviewed (set (map (juxt :attempt-id :objective) review-records))]
    (assoc item :pending-objectives
           (filterv #(not (contains? reviewed [(:attempt-id item) %]))
                    (item-objectives item)))))

(defn pending-items
  ([] (pending-items default-root))
  ([root]
   (let [review-records (reviews root)]
     (->> (items root)
          (mapv #(with-pending-objectives % review-records))
          (filterv #(seq (:pending-objectives %)))
          vec))))

(defn unseen-belief-events
  "Return QA events not named in consumed-ids."
  ([consumed-ids] (unseen-belief-events default-root consumed-ids))
  ([root consumed-ids]
   (let [seen (set consumed-ids)]
     (->> (reviews root)
          (keep :belief-event)
          (remove #(contains? seen (:event-id %)))
          vec))))
