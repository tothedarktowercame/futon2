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

(def verdict->event-type
  {:approve :strengthened
   :confirmed :strengthened
   :request-changes :reopened
   :reject :falsified
   :uncertain :refined})

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

(defn review!
  ([attempt-id entity-id verdict note reviewer]
   (review! default-root attempt-id entity-id verdict note reviewer))
  ([root attempt-id entity-id verdict note reviewer]
   (let [event-type (get verdict->event-type verdict)]
     (when-not event-type
       (throw (ex-info "Unknown Morning Brief verdict"
                       {:verdict verdict :allowed (set (keys verdict->event-type))})))
     (let [review-id (str "mbqa-" (UUID/randomUUID))
           record {:morning-brief/review-id review-id
                   :attempt-id attempt-id
                   :entity-id entity-id
                   :verdict verdict
                   :note note
                   :reviewer reviewer
                   :reviewed-at (str (Instant/now))
                   :belief-event {:event-id review-id
                                  :entity-id entity-id
                                  :type event-type
                                  :weight 1.0
                                  :source :morning-brief-qa}}]
       (write-new! (io/file root "reviews" (str review-id ".edn")) record)
       record))))

(defn reviews
  ([] (reviews default-root))
  ([root] (read-records (io/file root "reviews"))))

(defn pending-items
  ([] (pending-items default-root))
  ([root]
   (let [reviewed (set (map :attempt-id (reviews root)))]
     (->> (read-records (io/file root "items"))
          (remove #(contains? reviewed (:attempt-id %)))
          vec))))

(defn unseen-belief-events
  "Return QA events not named in consumed-ids."
  ([consumed-ids] (unseen-belief-events default-root consumed-ids))
  ([root consumed-ids]
   (let [seen (set consumed-ids)]
     (->> (reviews root)
          (map :belief-event)
          (remove #(contains? seen (:event-id %)))
          vec))))
