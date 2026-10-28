(ns futon2.aif.ticket-readiness-inputs
  "Canonical observations retained for ticket readiness, without task selection."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-pipeline-selector :as meta]
            [futon2.aif.pattern-registry :as pattern-registry]))

(def ^:private root-task-id #"^[MET]-[^/]+$")

(defn- pin [path body]
  {:path path :sha256 (field/sha256 (.getBytes ^String body "UTF-8"))})

(defn- fetch-json [url]
  (let [response (http/get url {:headers {"Accept" "application/json"}
                                :timeout 90000 :throw false})
        body (str (:body response))]
    (when-not (= 200 (:status response))
      (throw (ex-info "Live META source unavailable"
                      {:kind :meta-live-source-unavailable
                       :url url :status (:status response)})))
    {:source (pin url body) :bytes body :value (json/parse-string body true)}))

(defn- local-cascade-source
  "Call a cascade producer in the shared serving JVM and pin its canonical JSON.
  This avoids making a blocking HTTP request back into the same bounded server
  worker pool. Returns nil when the producer is not present in this process."
  [source-id producer-symbol]
  (try
    (when-let [producer (requiring-resolve producer-symbol)]
      (let [value (producer)
            body (json/generate-string value)]
        {:source (pin source-id body) :bytes body :value value}))
    (catch java.io.FileNotFoundException _ nil)))

(defn fetch-pipeline-snapshot
  "Fetch and byte-pin the current summary and graph exactly once each."
  ([] (fetch-pipeline-snapshot (pattern-registry/configured-evidence-base)))
  ([base-url]
   (let [summary-url (str base-url "/api/alpha/cascade-real")
         graph-url (str base-url "/api/alpha/cascade-real/graph")
         agency-url (str base-url "/api/alpha/agents")
         ;; Futon2 and Futon3c share the canonical serving JVM. Prefer direct
         ;; producer calls there: synchronous self-HTTP can wait behind the
         ;; request currently serving META. Standalone consumers retain HTTP.
         summary (or (local-cascade-source
                      summary-url
                      'futon3c.logic.cascade-real-live/cascade-real-summary)
                     (fetch-json summary-url))
         graph (or (local-cascade-source
                    graph-url
                    'futon3c.logic.cascade-real-live/cascade-real-graph)
                   (fetch-json graph-url))
         agency (or (local-cascade-source
                     agency-url 'futon3c.agency.registry/registry-status)
                    (fetch-json agency-url))
         graph-value (update (:value graph) :section-status
                             (fn [sections]
                               (into {} (map (fn [[k v]]
                                               [k (update v :status keyword)]))
                                     sections)))]
     {:schema meta/snapshot-schema
      :summary-source (:source summary) :graph-source (:source graph)
      :agency-source (:source agency)
      :source-bytes {:summary (:bytes summary) :graph (:bytes graph)
                     :agency (:bytes agency)}
      :summary (:value summary) :graph graph-value :agency (:value agency)})))

(defn- agency-target [agent]
  (some #(when (and (string? %) (re-matches root-task-id %)) %)
        [(:mission-id agent) (:excursion-id agent) (:ticket-id agent)]))

(defn ownership-observation [task snapshot]
  (let [source (:agency-source snapshot)
        agents (get-in snapshot [:agency :agents])
        owners (when (and (map? agents) (map? source))
                 (->> agents
                      (keep (fn [[agent-id agent]]
                              (when (= (:id task) (agency-target agent))
                                {:agent-id (name agent-id)
                                 :session-id (:session-id agent)
                                 :status (:status agent)})))
                      (sort-by :agent-id) vec))]
    (cond
      (nil? owners) {:state :unknown :reason :agency-evidence-unavailable}
      (empty? owners) {:state :unowned :source source}
      (= 1 (count owners)) {:state :active :owner (first owners) :source source}
    :else {:state :ambiguous :owners owners :source source})))

(defn ticket-readiness-input
  "Current preselection facts from existing canonical readers. Dependency and
  implementation evidence remain explicitly unknown until their real producer
  supplies them; preparation does not turn those unknowns into implementation
  authority."
  [task snapshot base-membership]
  {:source (:source task)
   :dependency-evidence {:status :unknown :scope :implementation}
   :ownership-evidence (ownership-observation task snapshot)
   :implementation-evidence {:status :unknown}
   :operator-evidence {:status-class (:status-class task)
                       :requisition (:requisition task)}
   :root-route (if (contains? (:ids base-membership) (:id task))
                 :admitted :standalone)})
