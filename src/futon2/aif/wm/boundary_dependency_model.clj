(ns futon2.aif.wm.boundary-dependency-model
  "P32's advisory, static boundary-interface model.

  Facts describe source-pinned claims.  They are intentionally separate from
  the runtime and produce findings only; no query here authorises or blocks a
  click.  An unknown fact remains :unknown in every query result."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def scope-covers?
  {[:all :all] true
   [:all :eligible-targets] true
   [:all :targets-with-readings] true
   [:all :selected-needs] true
   [:all :selected-target] true
   [:eligible-targets :eligible-targets] true
   [:eligible-targets :targets-with-readings] true
   [:targets-with-readings :targets-with-readings] true
   [:selected-needs :selected-needs] true
   [:selected-needs :selected-target] true
   [:selected-target :selected-target] true})

(defn load-facts
  "Reads a flat EDN fact vector.  Callers may supply a different file for a
  constructed case; no filesystem state is changed."
  [path]
  (edn/read-string (slurp (io/file path))))

(defn facts-of-kind
  "Returns all facts of one relation from the flat graph."
  [facts kind]
  (filter #(= kind (:kind %)) facts))

(defn producers-for [facts input]
  (filter #(= input (:output %)) (facts-of-kind facts :producer)))

(defn producer-status [consumer producers]
  (cond
    (empty? producers) :missing
    (some #(= :present (:status %)) producers)
    (if (some #(and (= :present (:status %))
                    (get scope-covers? [(:scope %) (:scope consumer)]))
              producers)
      :present
      :scope-insufficient)
    (some #(= :unknown (:status %)) producers) :unknown
    :else :missing))

(defn unproduced-inputs
  "Required consumer inputs without a usable producer.  :unknown is a finding
  status, never a substitute for :missing or a successful producer."
  [facts]
  (->> (facts-of-kind facts :consumer)
       (filter :required?)
       (keep (fn [consumer]
               (let [producers (producers-for facts (:input consumer))
                     status (producer-status consumer producers)]
                 (when (not= :present status)
                   {:kind :input-availability
                    :status status
                    :consumer (:id consumer)
                    :input (:input consumer)
                    :consumer-phase (:phase consumer)
                    :consumer-scope (:scope consumer)
                    :producer-ids (mapv :id producers)}))))
       vec))

(defn selection-preparation-cycles
  "Finds two-edge cycles where selection consumes a producer's output while
  that producer requires the selection result.  The phase comparison makes the
  after-selection preparation dependency explicit."
  [facts]
  (vec
   (for [consumer (facts-of-kind facts :consumer)
         producer (producers-for facts (:input consumer))
         :when (and (= :present (:status consumer))
                    (= :present (:status producer))
                    (some #{(:produces consumer)} (:requires producer))
                    (> (:phase producer) (:phase consumer)))]
     {:kind :selection-preparation-cycle
      :status :present
      :consumer (:id consumer)
      :producer (:id producer)
      :input (:input consumer)
      :selection-phase (:phase consumer)
      :preparation-phase (:phase producer)
      :source-pins [(:source-pin consumer) (:source-pin producer)]})))

(defn lost-retention-edges
  "Reports only absent and unknown retention.  A :present corrected edge is
  deliberately omitted, so correcting one fact cannot clear another edge."
  [facts]
  (->> (facts-of-kind facts :retention)
       (keep (fn [edge]
               (when (not= :present (:status edge))
                 {:kind :retention
                  :status (:status edge)
                  :edge (:id edge)
                  :carrier (:carrier edge)
                  :from (:from edge)
                  :to (:to edge)
                  :source-pin (:source-pin edge)})))
       vec))

(defn query
  "Evaluates all P32 advisory queries against a supplied fact vector."
  [facts]
  {:unproduced-inputs (unproduced-inputs facts)
   :selection-preparation-cycles (selection-preparation-cycles facts)
   :lost-retention-edges (lost-retention-edges facts)})
