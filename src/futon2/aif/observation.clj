(ns futon2.aif.observation
  "AIF observation layer for the War Machine.

   Normalises raw scan data (from `futon2.report.war-machine`'s
   `scan-*` projection functions) into a 14-channel observation
   vector in [0,1]. This is the War Machine's g-observe — the bridge
   between scan data and the AIF loop.

   cf. cyberants `ants/aif/observe.clj` — same pattern, strategic domain.")

(def observation-channels
  "The war machine's observation channels, harmonized from all vocabularies.
   Each channel is a named terminal with a source vocabulary and normalization.

   v0.10 added `:annotation-health` — the first belief-derived channel,
   sourced from `stack-annotations.edn`'s `:lift-anomalies` density.
   It is the channel for which R3a (likelihood model) is satisfied in v0.10."
  [:loop-health           ;; overall loop health [0,1] — from holistic argument
   :support-coverage      ;; S1-S5 evidence coverage [0,1] — from holistic argument
   :attack-coverage       ;; A1-A4 evidence coverage [0,1] — from holistic argument
   :mission-health        ;; mission triage health [0,1] — from peripheral-aif
   :stack-pct             ;; stack commit % [0,1] — from logic model / joe-hud
   :consulting-pct        ;; consulting commit % [0,1] — from JSDQ
   :portfolio-pct         ;; portfolio commit % [0,1] — from JSDQ
   :mathematics-pct       ;; mathematics commit % [0,1] — from JSDQ
   :active-repo-ratio     ;; active repos / total repos [0,1] — from logic model
   :sorry-count-norm      ;; open sorrys / 10 (capped at 1) — from sorry topology
   :coupling-density      ;; coupling edges / max edges [0,1] — from temporal analysis
   :ticks-firing-ratio    ;; firing ticks / total ticks [0,1] — from logic model
   :depositing-signal     ;; depositing cardinal direction [0,1] — from daily scan frames
   :annotation-health     ;; 1 − (lift-anomalies / sections) — from stack-annotations.edn (v0.10; R3a-likelihood-derived)
   ])

(def ^:private missing ::missing)

(defn- present-value?
  [data path]
  (let [value (get-in data path missing)]
    (and (not= missing value) (some? value))))

(defn- channel-statuses
  "Classify the raw inputs behind each projected channel.

   This deliberately does not change the numeric projection.  It makes the
   formerly silent distinction between a measured zero and a zero substituted
   for missing input available to callers that are ready to consume it."
  [data]
  (let [requirements
        {:loop-health [[:loop-health :overall]]
         :support-coverage [[:support-attack :support-coverage]]
         :attack-coverage [[:support-attack :attack-coverage]]
         :mission-health [[:mission-triage :health]]
         :stack-pct [[:graph :dynamics :commit-percentages :stack]]
         :consulting-pct [[:graph :dynamics :commit-percentages :consulting]]
         :portfolio-pct [[:graph :dynamics :commit-percentages :portfolio]]
         :mathematics-pct [[:graph :dynamics :commit-percentages :mathematics]]
         :active-repo-ratio [[:graph :summary :active-repos]
                             [:graph :summary :total-repos]]
         :sorry-count-norm [[:graph :summary :total-sorrys]]
         :coupling-density [[:graph :summary :coupling-edges]
                            [:graph :summary :total-repos]]
         :ticks-firing-ratio [[:graph :dynamics :ticks]
                              [:graph :summary :ticks-firing]]
         :depositing-signal [[:frames :depositing-signal]]
         :annotation-health [[:annotation-graph :health]]}]
    (into {}
          (map (fn [[channel paths]]
                 (let [absent (remove #(present-value? data %) paths)]
                   [channel
                    (if (seq absent)
                      {:variant :absent
                       :reason :source-field-missing
                       :paths (vec absent)
                       :coerced-to 0.0}
                      {:variant :observed})])))
          requirements)))

(defn observation-status
  "Return the tagged provenance for CHANNEL in an observation from `observe`."
  [observation channel]
  (get (::channel-statuses (meta observation)) channel
       {:variant :absent :reason :status-metadata-missing}))

(defn observation-envelope
  "Return an EDN-safe tagged observation for persistence or typed consumers.

   Existing numeric consumers can continue to use the map returned by
   `observe`; new boundaries should persist this envelope so absence is not
   erased by EDN metadata printing."
  [observation]
  {:type :futon2.aif/tagged-observation
   :channels
   (into {}
         (map (fn [channel]
                (let [status (observation-status observation channel)
                      value (get observation channel 0.0)]
                  [channel
                   (if (= :observed (:variant status))
                     {:variant :observed :value value}
                     (assoc status :value value))])))
         observation-channels)})

(defn observe
  "Produce normalized observation vector from raw scan data.
   Returns a map of channel-id → [0,1] value.

   This is the war machine's g-observe: the bridge between
   raw scan data and the AIF loop. As of v0.10, also projects
   `:annotation-health` from `(:annotation-graph data)` (sourced
   upstream by `scan-annotation-graph`). The numeric projection remains
   backward-compatible: an absent source is represented as 0.0.  Unlike the
   old silent default, the returned map carries tagged channel provenance in
   metadata; use `observation-status` or the EDN-safe `observation-envelope`
   at a persistence boundary."
  [data]
  (let [{:keys [loop-health support-attack mission-triage graph frames annotation-graph]} data
        {:keys [commit-percentages ticks]} (:dynamics graph {})
        {:keys [summary]} graph
        depositing-signal (or (:depositing-signal frames) 0.0)
        annotation-health (or (:health annotation-graph) 0.0)
        values {:loop-health (:overall loop-health 0.0)
     :support-coverage (:support-coverage support-attack 0.0)
     :attack-coverage (:attack-coverage support-attack 0.0)
     :mission-health (:health mission-triage 0.0)
     :stack-pct (:stack commit-percentages 0.0)
     :consulting-pct (:consulting commit-percentages 0.0)
     :portfolio-pct (:portfolio commit-percentages 0.0)
     :mathematics-pct (:mathematics commit-percentages 0.0)
     :active-repo-ratio (if (and summary (pos? (:total-repos summary)))
                          (/ (double (:active-repos summary 0))
                             (:total-repos summary))
                          0.0)
     :sorry-count-norm (min 1.0 (/ (double (:total-sorrys summary 0)) 10.0))
     :coupling-density (let [n (:total-repos summary 0)
                              max-edges (/ (* n (dec n)) 2)]
                          (if (pos? max-edges)
                            (min 1.0 (/ (double (:coupling-edges summary 0)) max-edges))
                            0.0))
     :ticks-firing-ratio (let [total (count (or ticks []))
                                firing (:ticks-firing summary 0)]
                            (if (pos? total)
                              (/ (double firing) total)
                              0.0))
     :depositing-signal depositing-signal
     :annotation-health annotation-health}]
    (with-meta values {::channel-statuses (channel-statuses data)})))

(defn sense->vector
  "Convert an observation to its ordered numeric vector, requiring the exact
   tagged envelope beside it. Requiring both objects makes absence provenance
   part of every vector boundary."
  [obs envelope]
  (let [expected (observation-envelope obs)]
    (when-not (= expected envelope)
      (throw (ex-info "numeric observation vector requires its matching envelope"
                      {:expected-type (:type expected)
                       :provided-type (:type envelope)})))
    (mapv #(get obs % 0.0) observation-channels)))
