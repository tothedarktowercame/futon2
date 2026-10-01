(ns futon2.aif.cascade-feedback
  "Production feedback from one provisional cascade execution to later
   construction.  Selection, verified application, and successful use are
   deliberately different events: selection alone never reinforces a pattern."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.load-identity :as load-identity])
  (:import [clojure.lang LineNumberingPushbackReader]
           [java.io RandomAccessFile]))

(load-identity/register! *ns* *file*)

(def receipt-schema :wm/cascade-execution-feedback-v1)
(def metadata-schema :wm/pattern-feedback-metadata-v1)
(def default-path
  (str (System/getProperty "user.home")
       "/code/futon2/data/wm-pattern-feedback/events.edn"))

(defn- selected-patterns [action]
  (->> (:precedence action) (keep :id) distinct vec))

(defn- verified-application? [d-task]
  (let [join (or (get-in d-task [:verification :candidate-to-minted-join])
                 (:candidate-to-minted-join d-task))]
    (and (= :admitted (get-in d-task [:verification :status]))
         (= :verified (:status join))
         (string? (:selected-action-sha256 join))
         (= (:selected-action-sha256 join)
            (:enacted-action-sha256 join)))))

(defn receipt
  "Create the close feedback receipt.  A pattern appears in :applications
   only when the retained D-task bridge proves the selected action was enacted
   and the accepted-increment receipt names a recorded criterion step.  It is
   positive evidence only when that step's increment was accepted on a
   grounded close.  Other selected patterns remain :selected-only."
  [{:keys [run-id target selected-action outcome failure accepted-increment
           d-task-enactment artifact]}]
  (let [target (or target (:target selected-action))
        selected (selected-patterns selected-action)
        step (get-in accepted-increment [:criterion-step :id])
        exact-step? (and (verified-application? d-task-enactment)
                         (= :recorded-decision
                            (get-in accepted-increment [:criterion-step :source]))
                         (contains? (set selected) step))
        grounded? (contains? #{:grounded-change :grounded-progress} outcome)
        success? (and exact-step? grounded? (true? (:accepted? accepted-increment)))
        application (when exact-step?
                      {:pattern step
                       :status (if success? :successful :incomplete)
                       :evidence {:selected-enacted-action :verified
                                  :accepted-increment (:accepted? accepted-increment)
                                  :accepted-reason (:reason accepted-increment)
                                  :terminal-outcome outcome}
                       :reinforcement (if success? :positive :none)})
        applied (cond-> #{} application (conj step))
        selected-only (vec (remove applied selected))
        blocker-kind (when-not success?
                       (or (:kind failure)
                           (:reason accepted-increment)
                           (when selected-action outcome)
                           :execution-evidence-unavailable))
        base {:schema receipt-schema
              :run/id run-id
              :target target
              :cascade-status :provisional-per-run
              :mission-state {:selected-wants (vec (:want selected-action))
                              :reached-wants
                              (vec (or (:reached-wants selected-action)
                                       (get-in selected-action
                                               [:construction-receipt :reached-wants])
                                       []))
                              :unreached-wants
                              (vec (or (:unreached-wants selected-action)
                                       (get-in selected-action
                                               [:construction-receipt :unreached-wants])
                                       []))
                              :terminal-outcome outcome}
              :patterns {:selected selected
                         :applications (cond-> [] application (conj application))
                         :selected-only selected-only
                         :positive-reinforcement (if success? [step] [])}
              :blocker (if blocker-kind
                         {:status :present
                          :kind blocker-kind
                          :stage (or (:stage failure) {:absent :not-recorded})
                          :repair-evidence (cond-> [] application (conj application))}
                         {:status :absent :reason :accepted-grounded-application})
              :artifact (if (and (map? artifact) (string? (:commit artifact)))
                          (assoc (select-keys artifact [:repo :commit])
                                 :grounded? grounded?)
                          {:status :absent :reason :no-grounded-artifact})}
        receipt-id (identity/digest base)]
    (assoc base :receipt/id receipt-id)))

(defn- read-events* [file]
  (if-not (.isFile file)
    []
    (with-open [reader (LineNumberingPushbackReader. (io/reader file))]
      (loop [events []]
        (let [value (edn/read {:eof ::eof} reader)]
          (if (= ::eof value)
            events
            (do
              (when-not (and (= receipt-schema (:schema value))
                             (string? (:receipt/id value)))
                (throw (ex-info "Malformed cascade feedback event"
                                {:cascade-feedback/refusal :invalid-event
                                 :line (.getLineNumber reader)})))
              (recur (conj events value)))))))))

(defn read-snapshot
  ([] (read-snapshot default-path))
  ([path]
   (let [file (.getAbsoluteFile (io/file path))
         events (read-events* file)]
     {:schema :wm/cascade-execution-feedback-snapshot-v1
      :status (if (.isFile file) :present :absent)
      :reason (when-not (.isFile file) :feedback-store-missing)
      :path (.getPath file)
      :events events})))

(defn record!
  "Append RECEIPT exactly once under a process/file lock."
  ([receipt] (record! default-path receipt))
  ([path receipt]
   (when-not (and (= receipt-schema (:schema receipt))
                  (string? (:receipt/id receipt)))
     (throw (ex-info "Invalid cascade feedback receipt"
                     {:cascade-feedback/refusal :invalid-receipt})))
   (let [file (.getAbsoluteFile (io/file path))]
     (io/make-parents file)
     (with-open [raf (RandomAccessFile. file "rw")
                 _lock (.lock (.getChannel raf))]
       (let [events (read-events* file)
             duplicate? (some #(= (:receipt/id receipt) (:receipt/id %)) events)]
         (when-not duplicate?
           (.seek raf (.length raf))
           (.write raf (.getBytes (str (pr-str receipt) "\n") "UTF-8"))
           (.sync (.getFD raf)))
         {:status (if duplicate? :already-recorded :recorded)
          :path (.getPath file)
          :receipt/id (:receipt/id receipt)})))))

(defn construction-metadata
  "Project retained close receipts into target-local construction metadata.
   Counts separate successful, incomplete, and selected-only evidence."
  [snapshot]
  (into {}
        (for [[target events] (group-by :target (:events snapshot))]
          (let [selected (mapcat #(get-in % [:patterns :selected]) events)
                applications (mapcat #(get-in % [:patterns :applications]) events)
                by-pattern (group-by :pattern applications)
                ids (set/union (set selected) (set (keys by-pattern)))]
            [target
             {:schema metadata-schema
              :target target
              :receipt-count (count events)
              :patterns
              (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
                    (for [id ids
                          :let [apps (get by-pattern id [])]]
                      [id {:successful-applications
                           (count (filter #(= :successful (:status %)) apps))
                           :incomplete-applications
                           (count (filter #(= :incomplete (:status %)) apps))
                           :selected-only
                           (count (filter #(some #{id}
                                                  (get-in % [:patterns :selected-only]))
                                          events))}]))
              :latest (select-keys (last events)
                                   [:receipt/id :run/id :mission-state :blocker :artifact])}]))))

(defn load-construction-metadata
  ([] (load-construction-metadata default-path))
  ([path] (construction-metadata (read-snapshot path))))
