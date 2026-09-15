(ns futon2.aif.work-target-belief
  "P1a work-target admission and monotone carry. No observation/update path.
   Only read-declaration performs I/O; registry acquisition belongs to the caller."
  (:require [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.machine-belief :as machine-belief]
            [futon2.aif.mission-registry :as registry])
  (:import (java.nio.file Files Paths)
           (java.nio.charset StandardCharsets)
           (java.security MessageDigest)))

(def declaration-path
  "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/declarations/wm-work-target-interpretation-v1.edn")

(def declaration-sha256
  "055d579d4bec9ef52c6a3e2949b730d413624ddbc6b60cc00931810a77675e5e")

(defn- refusal [kind path]
  {:ok false :refusal {:kind kind :path path}})

(defn- sha256 [bytes]
  (format "%064x" (BigInteger. 1 (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn- checked-declaration [text path]
  (if (or (not (string? text))
          (not= declaration-sha256
                (sha256 (.getBytes ^String text StandardCharsets/UTF_8))))
    (refusal :declaration-hash-mismatch [:declaration path])
    {:ok true :text text
     :declaration (edn/read-string text)
     :source {:path path :sha256 declaration-sha256}}))

(defn read-declaration
  "Read and verify the pinned declaration's bytes. Optional path permits a
   relocated copy, never a different pin. The returned envelope is the
   declaration argument to carry-and-introduce. No D values are compiled in."
  ([] (read-declaration declaration-path))
  ([path]
   (try
     (let [bytes (Files/readAllBytes (Paths/get path (make-array String 0)))]
       (if (= declaration-sha256 (sha256 bytes))
         (checked-declaration (String. bytes StandardCharsets/UTF_8) path)
         (refusal :declaration-hash-mismatch [:declaration path])))
     (catch java.io.IOException _
       (refusal :declaration-unreadable [:declaration path])))))

(defn- candidate-action [candidate]
  (if (contains? candidate :action) (:action candidate) candidate))

(defn- registry-id [{:keys [type target]}]
  (when (string? target)
    (case type
      :advance-mission (registry/mission-target-id target)
      :advance-ticket (when (str/starts-with? target "T-") target)
      nil)))

(defn- pinned-entry? [entry]
  (and (string? (:path entry)) (seq (:path entry))
       (string? (:sha256 entry))
       (re-matches #"[0-9a-f]{64}" (:sha256 entry))
       (keyword? (:status-class entry))))

(defn admissions
  "Admit exact candidate target strings against a caller-acquired snapshot.
   Entries have the load-missions/load-tickets shape plus :sha256 from the
   bytes acquired by the caller. Status is context, not an observation or an
   additional live-status filter. :timestamp in tick-context is required.
   A readable snapshot can legitimately admit zero candidates; an unreadable
   snapshot returns only a refusal, never an empty successful domain."
  [candidates registry-snapshot tick-context]
  (let [entries (:entries registry-snapshot)]
    (cond
      (not= :read (:status registry-snapshot))
      (assoc (refusal :registry-unreadable [:registry-snapshot])
             :reason (:reason registry-snapshot))

      (not (and (sequential? entries) (some? (:read-at registry-snapshot))))
      (refusal :invalid-registry-snapshot [:registry-snapshot])

      (not= (count entries) (count (set (map :id entries))))
      (refusal :ambiguous-registry-id [:registry-snapshot :entries])

      (nil? (:timestamp tick-context))
      (refusal :missing-tick-time [:tick-context :timestamp])

      :else
      (let [by-id (into {} (map (juxt :id identity)) entries)]
        (reduce
         (fn [result candidate]
           (if-not (:ok result)
             (reduced result)
             (let [{:keys [type target] :as action} (candidate-action candidate)
                   id (registry-id action)
                   entry (get by-id id)
                   ;; Exact identity: an endpoint that normalises to a registry
                   ;; id is another string, and admitting it would give one
                   ;; mission two beliefs. It waits for a declared alias mapping.
                   reason (cond
                            false
                            :not-a-work-target-type
                            (and (some? id) (not= id target)) :target-alias-undeclared
                            (nil? entry) :not-registry-eligible)]
               (cond
                 reason (update result :not-admitted conj
                                (cond-> {:candidate candidate :reason reason}
                                  (= :target-alias-undeclared reason)
                                  (assoc :resolves-to id)))
                 (not (pinned-entry? entry))
                 (refusal :registry-pin-missing [:registry-snapshot :entries id])
                 :else
                 (assoc-in result [:admitted target]
                           {:type type :registry-id id
                            :registry-pin (select-keys entry [:path :sha256 :status-class])
                            :admitted-at (:timestamp tick-context)})))))
         {:ok true :admitted {} :not-admitted []} candidates)))))

(defn- model-context [declaration]
  (let [value (:declaration declaration)]
    {:model {:id (get-in value [:initial-distribution :name])
             :revision (:revision value)}
     :declaration (:source declaration)
     :interpretation-revision (:revision value)
     :state-support (:state-support value)}))

(defn- half-present-targets [state]
  (let [rows (set (keys (:belief state)))
        lineages (set (keys (:lineage state)))]
    (set/union (set/difference rows lineages)
               (set/difference lineages rows)
               (set (for [id (set/intersection rows lineages)
                          :when (or (nil? (get-in state [:belief id]))
                                    (nil? (get-in state [:lineage id])))]
                      id)))))

(defn carry-and-introduce
  "Return a state with :belief, :lineage, :model-context and :target-refusals.
   Per-target :carry-missing preserves the damaged predecessor for diagnosis;
   it never repairs it. Whole-state refusals contain no belief/lineage rows.
   Pass read-declaration's envelope, admissions' result, and :timestamp.
   Existing rows and lineage values are retained verbatim, including targets
   outside this tick's admissions. No strategic filter manifest is consumed."
  [declaration predecessor admission-result tick-context]
  (let [checked (checked-declaration (:text declaration)
                                     (get-in declaration [:source :path]))
        context (when (:ok checked) (model-context checked))
        previous (:state predecessor)]
    (cond
      (false? (:ok declaration)) declaration
      (not (:ok checked)) checked
      (not= (select-keys declaration [:declaration :source])
            (select-keys checked [:declaration :source]))
      (refusal :declaration-content-mismatch [:declaration])

      (#{:missing-after-genesis :unreadable} (:status predecessor))
      (assoc (refusal :carry-missing [:predecessor])
             :predecessor-status (:status predecessor) :reason (:reason predecessor))

      (not (#{:present :established-no-snapshots} (:status predecessor)))
      (assoc (refusal :invalid-predecessor [:predecessor])
             :predecessor-status (:status predecessor))

      (and (= :present (:status predecessor))
           (not (and (map? (:belief previous)) (map? (:lineage previous)))))
      (refusal :carry-missing [:predecessor :state])

      (and (= :present (:status predecessor))
           (not= context (:model-context previous)))
      (refusal :model-context-mismatch [:predecessor :state :model-context])

      (some #(and (some? %) (not= :no-admitted-observations (:updates %)))
            (vals (:lineage previous)))
      (refusal :unsupported-update-lineage [:predecessor :state :lineage])

      (not (:ok admission-result)) admission-result
      (nil? (:timestamp tick-context))
      (refusal :missing-tick-time [:tick-context :timestamp])

      :else
      (let [state (if (= :present (:status predecessor))
                    previous {:belief {} :lineage {}})
            broken (half-present-targets state)
            retained (set/union (set (keys (:belief state)))
                                (set (keys (:lineage state))))
            value (:declaration checked)
            d (:initial-distribution value)
            timestamp (:timestamp tick-context)]
        (reduce-kv
         (fn [result target admission]
           (if (contains? retained target)
             result
             (-> result
                 (assoc-in [:belief target] (:masses d))
                 (assoc-in [:lineage target]
                           {:introduced-at timestamp :admission admission
                            :D {:name (:name d) :revision (:revision value)
                                :authority (:authority d)
                                :declaration-sha256 declaration-sha256}
                            :interpretation-revision (:revision value)
                            :decision-ref (get-in value [:decision :bell])
                            :updates :no-admitted-observations
                            :information-cutoff timestamp}))))
         (assoc state :ok true :model-context context
                :target-refusals
                (into {} (map (fn [id] [id (refusal :carry-missing [:target id])])) broken))
         (:admitted admission-result))))))

(defn target-belief-input
  "Domain-aware row-7 reader. registry-ids contains exact eligible registry
   ids; undeclared endpoint aliases are never admitted. model-context
   is row 7's full context (including policy scope); it is passed unchanged.
   Lineage is attached beside, never inside, row 7's :belief-input."
  [model-context work-target-state registry-ids target]
  (let [row? (some? (get-in work-target-state [:belief target]))
        lineage? (some? (get-in work-target-state [:lineage target]))
        seen? (or (contains? (:belief work-target-state) target)
                  (contains? (:lineage work-target-state) target))]
    (cond
      (not (contains? (set registry-ids) target))
      (refusal :entity-outside-registry [:target target])
      (not seen?) (refusal :registered-not-admitted [:target target])
      (not (and row? lineage?)) (refusal :carry-missing [:target target])
      (not= target (:entity/id model-context))
      (refusal :entity-context-mismatch [:model-context :entity/id])
      :else
      (assoc (machine-belief/belief-state-distribution
              model-context (:belief work-target-state))
             :lineage (get-in work-target-state [:lineage target])))))
