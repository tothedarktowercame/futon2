(ns futon2.aif.wm.boundary-dependency-model
  "P32's advisory, static boundary-interface model.

  Facts describe source-pinned claims.  They are intentionally separate from
  the runtime and produce findings only; no query here authorises or blocks a
  click.  An unknown fact remains :unknown in every query result."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn valid-source-pin?
  "A P32 source pin is resolvable enough for a reviewer to inspect: repository,
  path, immutable commit, and a symbol or line locator."
  [pin]
  (and (map? pin)
       (string? (:repository pin))
       (string? (:path pin))
       (re-matches #"[0-9a-f]{40}" (:commit pin))
       (map? (:locator pin))
       (or (string? (get-in pin [:locator :symbol]))
           (pos-int? (get-in pin [:locator :line])))))

(defn validate-facts!
  "Rejects prose-only source pins without attempting a general pin framework."
  [facts]
  (doseq [fact facts]
    (when-not (valid-source-pin? (:source-pin fact))
      (throw (ex-info "P32 fact has no resolvable source pin"
                      {:fact (:id fact) :source-pin (:source-pin fact)}))))
  facts)

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
  (validate-facts! (edn/read-string (slurp (io/file path)))))

(defn facts-of-kind
  "Returns all facts of one relation from the flat graph."
  [facts kind]
  (filter #(= kind (:kind %)) facts))

(defn producers-for [facts consumer]
  (filter #(and (= (:input consumer) (:output %))
                (= (:scenario consumer) (:scenario %)))
          (facts-of-kind facts :producer)))

(defn producer-status [consumer producers]
  (let [covers? #(get scope-covers? [(:scope %) (:scope consumer)])
        known-covering? (some #(and (= :present (:status %)) (covers? %)) producers)
        unknown-covering? (some #(and (= :unknown (:status %)) (covers? %)) producers)
        known? (some #(= :present (:status %)) producers)
        unknown? (some #(= :unknown (:status %)) producers)]
    (cond
      known-covering? :present
      unknown-covering? :unknown
      known? :scope-insufficient
      unknown? :unknown
      :else :missing)))

(defn unproduced-inputs
  "Required consumer inputs without a usable producer.  :unknown is a finding
  status, never a substitute for :missing or a successful producer."
  [facts]
  (->> (facts-of-kind facts :consumer)
       (filter :required?)
       (keep (fn [consumer]
               (let [producers (producers-for facts consumer)
                     status (producer-status consumer producers)]
                 (when (not= :present status)
                   {:kind :input-availability
                    :status status
                    :consumer (:id consumer)
                    :scenario (:scenario consumer)
                    :input (:input consumer)
                    :consumer-phase (:phase consumer)
                    :consumer-scope (:scope consumer)
                    :producer-ids (mapv :id producers)}))))
       vec))

(defn selection-preparation-cycles
  "Finds blocking two-edge cycles only. A refused selection that explicitly
  records-and-continues into preparation is feedback, not a chosen-action
  prerequisite and therefore is not returned as a blocking cycle."
  [facts]
  (vec
   (for [consumer (facts-of-kind facts :consumer)
         producer (producers-for facts consumer)
         :when (and (= :present (:status consumer))
                    (= :present (:status producer))
                    (some #{(:produces consumer)} (:requires producer))
                    (= :chosen-action (:selection-prerequisite producer))
                    (contains? #{:blocks-selection :family-failure}
                               (:absence-disposition consumer))
                    (> (:phase producer) (:phase consumer)))]
     {:kind :selection-preparation-cycle
      :status :present
      :consumer (:id consumer)
      :scenario (:scenario consumer)
      :producer (:id producer)
      :input (:input consumer)
      :selection-phase (:phase consumer)
      :preparation-phase (:phase producer)
      :source-pins [(:source-pin consumer) (:source-pin producer)]})))

(defn measured-preparation-feedback
  "Returns the non-blocking refused-selection preparation route separately so
  callers cannot mistake it for a successful selection dependency."
  [facts]
  (vec
   (for [consumer (facts-of-kind facts :consumer)
         producer (producers-for facts consumer)
         :when (and (= :present (:status consumer))
                    (= :present (:status producer))
                    (some #{(:produces consumer)} (:requires producer))
                    (= :attempted-or-refused (:selection-prerequisite producer))
                    (= :record-and-continue (:absence-disposition consumer)))]
     {:kind :measured-preparation-feedback
      :status :present
      :consumer (:id consumer)
      :scenario (:scenario consumer)
      :producer (:id producer)
      :input (:input consumer)})))

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
                  :scenario (:scenario edge)
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
   :measured-preparation-feedback (measured-preparation-feedback facts)
   :lost-retention-edges (lost-retention-edges facts)})
