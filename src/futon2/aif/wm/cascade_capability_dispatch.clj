(ns futon2.aif.wm.cascade-capability-dispatch
  "Dispatch executable cascade frontier nodes to explicitly registered
  capability executors. Success requires a content-bound verified-effect
  receipt; labels or successful return values alone are not effects."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.interpretation-evidence :as evidence]))

(def receipt-schema :wm/cascade-effect-receipt-v1)

(defn receipt-digest [receipt]
  (evidence/value-digest (dissoc receipt :digest)))

(defn seal-receipt [receipt]
  (assoc receipt :digest (receipt-digest receipt)))

(defn valid-receipt?
  [receipt cascade-digest node]
  (and (= receipt-schema (:schema receipt))
       (= cascade-digest (:cascade-digest receipt))
       (= (:occurrence-id node) (:occurrence-id receipt))
       (= (get-in node [:clause :operation]) (:operation receipt))
       (= (get-in node [:clause :capability]) (:capability receipt))
       (= :verified-effect (:evidence-kind receipt))
       (map? (:effect receipt))
       (= (:digest receipt) (receipt-digest receipt))))

(defn dispatch-node
  "Execute NODE using REGISTRY {capability executor-fn}. The executor receives
  a request containing the canonical cascade digest and node, and must return
  a sealed :wm/cascade-effect-receipt-v1 bound to both."
  [registry cascade-digest node]
  (let [capability (get-in node [:clause :capability])
        executor (get registry capability)]
    (cond
      (nil? executor)
      {:status :error :kind :capability-executor-missing
       :cascade-digest cascade-digest
       :occurrence-id (:occurrence-id node)
       :capability capability}

      :else
      (try
        (let [receipt (executor {:cascade-digest cascade-digest :node node})]
          (if (valid-receipt? receipt cascade-digest node)
            {:status :enacted :receipt receipt}
            {:status :error :kind :capability-effect-receipt-invalid
             :cascade-digest cascade-digest
             :occurrence-id (:occurrence-id node)
             :capability capability
             :receipt receipt}))
        (catch Throwable t
          {:status :error :kind :capability-executor-failed
           :cascade-digest cascade-digest
           :occurrence-id (:occurrence-id node)
           :capability capability
           :message (.getMessage t)})))))

(defn dispatch-frontier
  "Dispatch in canonical frontier order, stopping at the first typed fault."
  [registry cascade]
  (loop [nodes (:frontier cascade) enacted []]
    (if-let [node (first nodes)]
      (let [result (dispatch-node registry (:digest cascade) node)]
        (if (= :enacted (:status result))
          (recur (next nodes) (conj enacted (:receipt result)))
          (assoc result :enacted enacted)))
      {:status :enacted :receipts enacted})))

(defn route-frontier
  "Bind each frontier node to a declared production route without claiming an
  effect. REGISTRY maps capabilities to route identifiers. The full-loop
  build/review/close path later supplies the verified effect evidence."
  [registry cascade]
  (let [steps (mapv (fn [node]
                      (let [capability (get-in node [:clause :capability])]
                        {:occurrence-id (:occurrence-id node)
                         :operation (get-in node [:clause :operation])
                         :capability capability
                         :route (get registry capability)}))
                    (:frontier cascade))
        missing (filterv #(nil? (:route %)) steps)]
    (if (seq missing)
      {:status :error :kind :capability-route-missing :missing missing}
      {:status :routed :cascade-digest (:digest cascade) :steps steps})))

(defn recording-pattern-executor
  "Return a :wm/pattern-apply executor whose real, bounded effect is an
  append-only application record under ROOT. This is suitable for an offline
  recorded flight: it proves dispatch and persistence, but does not claim a
  live mission mutation."
  [root]
  (fn [{:keys [cascade-digest node]}]
    (let [application {:schema :wm/recorded-pattern-application-v1
                       :cascade-digest cascade-digest
                       :occurrence-id (:occurrence-id node)
                       :pattern (get-in node [:structural-node :pattern])
                       :operation (get-in node [:clause :operation])
                       :capability (get-in node [:clause :capability])
                       :completion-check (get-in node [:clause :completion-check])}
          application-digest (evidence/value-digest application)
          path (io/file root (str application-digest ".edn"))
          bytes (.getBytes (pr-str application) "UTF-8")]
      (io/make-parents path)
      (if (.exists path)
        (when-not (= application
                     (edn/read-string (slurp path)))
          (throw (ex-info "recorded pattern application collision"
                          {:kind :pattern-application-collision
                           :path (.getPath path)})))
        (java.nio.file.Files/write
         (.toPath path) bytes
         (into-array java.nio.file.OpenOption
                     [java.nio.file.StandardOpenOption/CREATE_NEW
                      java.nio.file.StandardOpenOption/WRITE])))
      (seal-receipt
       {:schema receipt-schema
        :cascade-digest cascade-digest
        :occurrence-id (:occurrence-id node)
        :operation (get-in node [:clause :operation])
        :capability (get-in node [:clause :capability])
        :evidence-kind :verified-effect
        :effect {:kind :recorded-pattern-application
                 :scope :offline-replay
                 :path (.getPath path)
                 :digest application-digest}}))))
