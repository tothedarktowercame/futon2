(ns futon2.aif.morning-brief
  "Append-only operator QA for completed full-loop opportunities.

  The opportunity runner writes one item. The operator later records a typed
  verdict; that review projects to an entity-grain A-matrix event consumed by
  the next War Machine judgement. Items and reviews are immutable files."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.data-paths :as data-paths])
  (:import [java.nio.file Files StandardOpenOption]
           [java.security MessageDigest]
           [java.time Instant]
           [java.util UUID]))

(def default-root "/home/joe/code/futon2/data/wm-morning-brief")
(defn resolved-root []
  (if (= default-root "/home/joe/code/futon2/data/wm-morning-brief")
    (data-paths/path "wm-morning-brief")
    default-root))
(def lifecycle-schema :wm/morning-brief-lifecycle-v1)

(defn- sha256 [^bytes bytes]
  (format "%064x" (BigInteger. 1 (.digest (doto (MessageDigest/getInstance "SHA-256")
                                            (.update bytes))))))

(defn item-identity [item]
  (let [identity (select-keys item [:attempt-id :run-id :click-id :opportunity-id
                                    :selected-target :commit :review-job
                                    :selected-wants])]
    {:identity identity
     :identity-sha256 (sha256 (.getBytes (pr-str (into (sorted-map) identity)) "UTF-8"))}))

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

(defn evidence-time-provenance
  "Classify occurrence versus recording time without changing evidence weight.

  Historical events without the v2 occurrence carrier remain readable as
  typed `:predates-field` absence.  Current events with a missing carrier are
  malformed.  This function records chronology only; it is deliberately not
  an ageing, refusal, or decay policy."
  [{:keys [evidence/occurred-at evidence/recorded-at
            morning-brief/event-schema-version]}]
  (let [legacy? (or (nil? event-schema-version) (< event-schema-version 2))]
    (cond
      (and (= :present (:status occurred-at))
           (= :present (:status recorded-at)))
      (try
        (let [occurred (Instant/parse (:value occurred-at))
              recorded (Instant/parse (:value recorded-at))]
          {:status :present
           :relationship (cond
                           (.isBefore occurred recorded) :occurrence-precedes-recording
                           (.isAfter occurred recorded) :occurrence-follows-recording
                           :else :same-instant)})
        (catch Exception e
          {:status :absent :reason :malformed
           :cause (ex-message e)}))

      (= :predates-field (:reason occurred-at))
      {:status :absent :reason :predates-field}

      legacy?
      {:status :absent :reason :predates-field}

      :else
      {:status :absent :reason :malformed})))

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

(defn item-summary
  "Compact inbox envelope for ITEM.  Full audit evidence remains in SOURCE."
  [source item]
  (let [build (get-in item [:achievement :build])]
    (cond-> (select-keys item [:attempt-id :queued-at :outcome :commit
                               :selected-target :operator-action])
      true (assoc :morning-brief/summary-version 1
                  :source-path (.getPath (io/file source))
                  :achievement {:build (when build {:present true})})
      (:failure item) (assoc :failure (select-keys (:failure item) [:kind :stage]))
      (:feature-card item) (assoc :feature-card
                                  (select-keys (:feature-card item)
                                               [:built :matches-intent?])))))

(defn- write-summary! [root source item]
  (write-new! (io/file root "summaries" (str (:attempt-id item) ".edn"))
              (item-summary source item)))

(defn queue-item!
  ([item] (queue-item! (resolved-root) item))
  ([root {:keys [attempt-id] :as item}]
   (when-not (and (string? attempt-id) (not (str/blank? attempt-id)))
     (throw (ex-info "Morning Brief item requires attempt-id" {:item item})))
   (let [occurred-at (str (Instant/now))
         item (assoc item :queued-at occurred-at
                          :evidence/occurred-at
                          {:status :present :value occurred-at}
                          :morning-brief/schema-version 3)
         path (write-new! (io/file root "items" (str attempt-id ".edn")) item)]
     (write-summary! root path item)
     (write-new! (io/file root "lifecycle" attempt-id "queued.edn")
                 (merge {:schema lifecycle-schema :transition :queued
                         :at occurred-at :source :queue-item!}
                        (item-identity item)))
     path)))

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
  ([operator-action] (queue-operator-gate! (resolved-root) operator-action))
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

(defn lifecycle-events
  ([] (lifecycle-events (resolved-root)))
  ([root]
   (->> (or (.listFiles (io/file root "lifecycle")) [])
        (filter #(.isDirectory %))
        (mapcat read-records)
        (sort-by :at)
        vec)))

(defn verify-lifecycle [item events]
  (let [expected (:identity-sha256 (item-identity item))
        transitions (mapv :transition events)
        counts (frequencies transitions)
        valid-orders #{[:queued] [:queued :opened] [:queued :opened :responded]}]
    (cond
      (some #(not= lifecycle-schema (:schema %)) events)
      {:status :refused :reason :lifecycle-schema-mismatch}
      (some #(not= expected (:identity-sha256 %)) events)
      {:status :refused :reason :lifecycle-identity-mismatch}
      (some #(> % 1) (vals counts))
      {:status :refused :reason :duplicate-transition}
      (not (contains? valid-orders transitions))
      {:status :refused :reason :impossible-transition :transitions transitions}
      :else {:status :verified :identity-sha256 expected :transitions transitions})))

(defn lifecycle-state
  ([attempt-id] (lifecycle-state (resolved-root) attempt-id))
  ([root attempt-id]
   (let [item (item-by-attempt root attempt-id)
         events (filterv #(= attempt-id (get-in % [:identity :attempt-id]))
                         (lifecycle-events root))
         verification (when (seq events) (verify-lifecycle item events))
         transitions (set (map :transition events))]
     {:attempt-id attempt-id :events events
      :verification verification
      :status (cond
                (= :refused (:status verification)) :invalid
                (contains? transitions :responded) :responded
                (contains? transitions :opened) :seen-no-response
                :else :unseen-or-uninstrumented)})))

(defn open-item!
  "The Field Desk's single-item read boundary. Listing never calls this."
  ([attempt-id consumer] (open-item! (resolved-root) attempt-id consumer))
  ([root attempt-id consumer]
   (let [item (item-by-attempt root attempt-id)]
     (when-not item
       (throw (ex-info "Unknown Morning Brief attempt" {:attempt-id attempt-id})))
     (when-not (nonblank-string? consumer)
       (throw (ex-info "Morning Brief open requires consumer identity" {})))
     (let [state (lifecycle-state root attempt-id)]
       (when (some #(= :opened (:transition %)) (:events state))
         (throw (ex-info "Morning Brief item was already opened"
                         {:attempt-id attempt-id :reason :duplicate-transition})))
       (when (some #(= :responded (:transition %)) (:events state))
         (throw (ex-info "Morning Brief response precedes open"
                         {:attempt-id attempt-id :reason :impossible-transition})))
       (let [record (merge {:schema lifecycle-schema :transition :opened
                            :at (str (Instant/now)) :source :field-desk-item-read
                            :consumer consumer}
                           (item-identity item))]
         (write-new! (io/file root "lifecycle" attempt-id "opened.edn") record)
         {:item item :event record})))))

(defn- response-class [answer]
  (cond
    (contains? #{:accept-feature :yes :sufficient :correct :acknowledged :resolved} answer)
    :explicit-confirmation
    (contains? #{:reject :no :insufficient :incorrect} answer) :complaint
    (contains? #{:revert :withdraw :withdrawal} answer) :revert-or-withdrawal
    :else :other-response))

(defn- belief-event-for [review-id item objective answer reviewed-at]
  (when (= :substantive-achievement objective)
    (when-let [entity-id (get-in item [:qa-targets :achievement :entity-id])]
      (let [event {:event-id review-id
                   :entity-id entity-id
                   :type (get achievement-answer->event-type answer)
                   :weight 1.0
                   :source :morning-brief-qa
                   :objective objective
                   :morning-brief/event-schema-version 2
                   :evidence/occurred-at
                   (or (:evidence/occurred-at item)
                       {:status :absent :reason :predates-field})
                   :evidence/recorded-at
                   {:status :present :value reviewed-at}}]
        (assoc event :evidence/time-provenance
               (evidence-time-provenance event))))))

(defn review!
  ([attempt-id objective answer note reviewer]
   (review! (resolved-root) attempt-id objective answer note reviewer))
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
     (when (and (>= (long (or (:morning-brief/schema-version item) 0)) 3)
                (not= :seen-no-response (:status (lifecycle-state root attempt-id))))
       (throw (ex-info "Morning Brief response requires an observed item open"
                       {:attempt-id attempt-id :reason :response-before-open})))
     (let [reviewed-at (str (Instant/now))
           review-id
           (str "mbqa-"
                (UUID/nameUUIDFromBytes
                 (.getBytes (str attempt-id "\u0000" (name objective)) "UTF-8")))
           record {:morning-brief/review-id review-id
                   :attempt-id attempt-id
                   :objective objective
                   :answer answer
                   :note note
                   :reviewer reviewer
                   :reviewed-at reviewed-at
                   :qa-target (get-in item [:qa-targets objective])
                   :belief-event (belief-event-for review-id item objective answer
                                                   reviewed-at)}]
       (write-new! (io/file root "reviews" (str review-id ".edn")) record)
       (when-not (some #(= :responded (:transition %))
                       (:events (lifecycle-state root attempt-id)))
         (write-new! (io/file root "lifecycle" attempt-id "responded.edn")
                     (merge {:schema lifecycle-schema :transition :responded
                             :at reviewed-at :source :morning-brief-review
                             :review-id review-id :reviewer reviewer
                             :response-class (response-class answer)
                             :objective objective :answer answer}
                            (item-identity item))))
       record))))

(defn addendum!
  "Append a reproducibility or rationale note to an existing attempt."
  ([attempt-id kind title body author]
   (addendum! (resolved-root) attempt-id kind title body author))
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
  ([] (reviews (resolved-root)))
  ([root] (read-records (io/file root "reviews"))))

(defn items
  ([] (items (resolved-root)))
  ([root] (read-records (io/file root "items"))))

(defn summaries
  "Read compact inbox envelopes without parsing full audit records."
  ([] (summaries (resolved-root)))
  ([root] (read-records (io/file root "summaries"))))

(defn ensure-summaries!
  "Backfill missing compact envelopes. Existing envelopes are immutable."
  ([] (ensure-summaries! (resolved-root)))
  ([root]
   (let [known (set (map :attempt-id (summaries root)))]
     (reduce (fn [result file]
               (let [item (edn/read-string (slurp file))]
                 (if (contains? known (:attempt-id item))
                   result
                   (do (write-summary! root file item)
                       (update result :written inc)))))
             {:written 0}
             (edn-files (io/file root "items"))))))

(defn addenda
  ([] (addenda (resolved-root)))
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
  ([] (pending-items (resolved-root)))
  ([root]
   (let [review-records (reviews root)]
     (->> (items root)
          (mapv #(with-pending-objectives % review-records))
          (filterv #(seq (:pending-objectives %)))
          vec))))

(defn unseen-belief-events
  "Return QA events not named in consumed-ids."
  ([consumed-ids] (unseen-belief-events (resolved-root) consumed-ids))
  ([root consumed-ids]
   (let [seen (set consumed-ids)]
     (->> (reviews root)
          (keep :belief-event)
          (remove #(contains? seen (:event-id %)))
          vec))))
