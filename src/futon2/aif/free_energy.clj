(ns futon2.aif.free-energy
  "Controller diagnostics and prediction-error computation for the War Machine.

   The inference step between observation and render. Reads observation
   vectors from `futon2.aif.observation/observe` and preferences from
   `futon2.aif.preferences`, producing explicitly named engineering diagnostics.
   The per-tick scalar `compute-variational-free-energy` was removed by
   worklist I5 slice (c) on Joe's J2 ruling (holes/labs/wm-contract/
   aif-equations.edn :choices :free-energy-form): the Laplace-channel F was a
   shortcut that no equation in the registry imported, and the replacement his
   ruling required -- F_pi in `futon2.aif.policy-free-energy`, entering the
   policy posterior -- is realised and consumed (RUN9/S4).

   cf. cyberants `ants/aif/policy.clj` — EFE computation
   cf. portfolio/policy.clj — action ranking
   cf. M-aif-head: the war machine integrates all heads, not replaces them

   Invariants:
   - WM-I1 (read-only — judge produces data, never writes)
   - WM-I4 (sovereignty — priorities are informational, not commands)"
  (:require [futon2.aif.observation :as observation]
            [futon2.aif.preferences :as pref]))

(defn- channel-gap
  "Distance of observation from preferred range.
   Returns 0.0 if within [lo, hi], positive distance otherwise."
  [obs-val [lo hi]]
  (let [v (double obs-val)]
    (cond (< v lo) (- lo v)
          (> v hi) (- v hi)
          :else 0.0)))

(defn channel-source-status
  "Whether CHANNEL of OBS carries an observed value, and why not when it does
   not. The value is returned RAW: callers that must tell a non-numeric value
   from an absent one cannot be handed a coerced double. Plain explicit maps
   are a supported legacy boundary; metadata-bearing observations retain
   absence.

   Public since AC4: `futon2.aif.policy/sorry-pressure-record` reads
   `:sorry-count-norm` through this same reader, so the fallback selector and
   the diagnostics cannot disagree about what counts as observed — including
   the legacy plain-map allowance, which a second implementation would be free
   to drift from."
  [obs channel]
  (let [source-status (observation/observation-status obs channel)
        explicit-legacy-value? (and (= :status-metadata-missing
                                       (:reason source-status))
                                    (contains? obs channel)
                                    (some? (get obs channel)))]
    (if (or (= :observed (:variant source-status)) explicit-legacy-value?)
      {:status :present :value (get obs channel)}
      (merge {:status :absent}
             (select-keys source-status [:reason :paths])))))

(defn- channel-reading
  "Return a reason-bearing reading for CHANNEL. Plain explicit maps are a
   supported legacy boundary; metadata-bearing observations retain absence."
  [obs channel]
  (let [source (channel-source-status obs channel)]
    (if (= :present (:status source))
      {:status :present :value (double (:value source))}
      source)))

(defn- avoidance-verdict
  "Tri-state avoided-range diagnostic. An absent source is unknown; it is
   never converted to the numeric observation zero. Plain explicit maps remain
   supported for library callers that predate observation metadata."
  [obs channel [lo hi :as avoided-range]]
  (let [source-status (observation/observation-status obs channel)
        explicit-legacy-value? (and (= :status-metadata-missing
                                       (:reason source-status))
                                    (contains? obs channel)
                                    (some? (get obs channel)))]
    (if (or (= :observed (:variant source-status)) explicit-legacy-value?)
      (let [v (double (get obs channel))]
        {:status (if (and (>= v lo) (<= v hi)) :violated :satisfied)
         :observation {:variant :observed :value v}
         :avoided-range avoided-range})
      {:status :unknown
       :observation (select-keys source-status [:variant :reason :paths])
       :avoided-range avoided-range})))

(defn compute-controller-diagnostics
  "Compute legacy controller diagnostics from an observation vector.

   Returns {:controller-score :preference-gap-score :coverage-uncertainty-pressure
            :per-channel-gaps :avoidance-by-channel :avoided-active}.

   preference-gap-score: weighted distance from preferences (dominated by workstream balance).
   coverage-uncertainty-pressure: uncertainty from dark arrows and unaddressed claims.
   controller-score: 0.65 * preference gap + 0.35 * coverage uncertainty.

   cf. war-machine-terminal-vocabulary.edn :G/pragmatic-fn, :G/epistemic-fn"
  ([obs] (compute-controller-diagnostics obs {:support-aware? true}))
  ([obs {:keys [support-aware?] :or {support-aware? true}}]
  (let [;; Pragmatic: gap between observations and preferences. Read through the
        ;; current-C seam (E-C-vector-live §4.5) so the channel preferences can
        ;; go live without touching this consumer; today identical to the static map.
        per-channel (into {}
                          (for [[ch pref-range] (pref/current-C)
                                :let [reading (if support-aware?
                                                (channel-reading obs ch)
                                                {:status :present
                                                 :value (double (get obs ch 0.0))})]]
                            [ch (if (= :present (:status reading))
                                  (let [v (:value reading)
                                        gap (channel-gap v pref-range)]
                                    {:status :present
                                     :value v
                                     :preferred pref-range
                                     :gap gap
                                     :in-range? (zero? gap)})
                                  (assoc reading :preferred pref-range))]))
        g-pragmatic (reduce-kv (fn [acc ch weight]
                                 (if-let [gap (get-in per-channel [ch :gap])]
                                   (+ acc (* weight gap))
                                   acc))
                               0.0
                               pref/pragmatic-weights)
        ;; Epistemic: uncertainty from dark areas
        epistemic-weights {:loop-health 0.4
                           :attack-coverage 0.3
                           :support-coverage 0.3}
        epistemic-terms
        (into {}
              (for [[ch weight] epistemic-weights
                    :let [reading (if support-aware?
                                    (channel-reading obs ch)
                                    {:status :present
                                     :value (double (get obs ch 0.0))})]]
                [ch (if (= :present (:status reading))
                      (assoc reading :weighted-term
                             (* weight (- 1.0 (:value reading))))
                      reading)]))
        g-epistemic (reduce + 0.0 (keep :weighted-term (vals epistemic-terms)))
        support {:pragmatic
                 {:present (->> per-channel (keep (fn [[ch x]]
                                                    (when (= :present (:status x)) ch))) set)
                  :absent (into {} (keep (fn [[ch x]]
                                           (when (= :absent (:status x))
                                             [ch (select-keys x [:reason :paths])]))
                                         per-channel))}
                 :epistemic
                 {:present (->> epistemic-terms (keep (fn [[ch x]]
                                                        (when (= :present (:status x)) ch))) set)
                  :absent (into {} (keep (fn [[ch x]]
                                           (when (= :absent (:status x))
                                             [ch (select-keys x [:reason :paths])]))
                                         epistemic-terms))}}
        ;; Total
        g-total (+ (* 0.65 g-pragmatic) (* 0.35 g-epistemic))
        avoidance-by-channel
        (into {}
              (for [[k v] pref/avoided-states
                    :when (not= k :strategic-mode)
                    :when (vector? v)]
                [k (avoidance-verdict obs k v)]))
        ;; Compatibility view: only measured violations are active. Unknown is
        ;; retained in :avoidance-by-channel and never silently counted either way.
        avoided (vec (for [[k verdict] avoidance-by-channel
                           :when (= :violated (:status verdict))]
                       k))]
    {:controller-score g-total
     :preference-gap-score g-pragmatic
     :coverage-uncertainty-pressure g-epistemic
     :score-support support
     :epistemic-terms epistemic-terms
     :per-channel per-channel
     :avoidance-by-channel avoidance-by-channel
     :avoided-active avoided})))

;; ---------------------------------------------------------------------------
;; v0.10: R3a prediction-error computation against a likelihood model.
;; ---------------------------------------------------------------------------

(def prediction-error-contract
  "Producer contract stamped on every prediction-triple record the producer
   emits, whether it scored, omitted, or refused."
  :prediction-error/v1)

(defn- finite-double
  "X as a double when it is a finite number; nil otherwise. A string, a
   keyword, NaN, or an infinity is a value the producer was GIVEN and cannot
   use — that is malformed, not absent."
  [x]
  (when (number? x)
    (let [d (double x)]
      (when-not (or (Double/isNaN d) (Double/isInfinite d)) d))))

(defn- prediction-member
  "Classify one likelihood-model member (`:mean` or `:variance`) of
   PREDICTION. Missing and non-finite are separate verdicts because the
   refusal record has to say which one happened."
  [prediction k]
  (let [raw (get prediction k)]
    (cond
      (not (contains? prediction k)) {:member k :status :missing}
      (nil? raw) {:member k :status :missing}
      (nil? (finite-double raw)) {:member k :status :not-finite :value raw}
      :else {:member k :status :present :value (finite-double raw)})))

(defn compute-prediction-error
  "R3a + R3b: prediction error (and precision-weighted error) for one channel
   given the observed value and a likelihood-model output `{:mean :variance}`.

   AC1 (Joe's 2026-09-02 ruling on C130 §2) removed the three zero
   substitutions that used to stand in for the members of this triple. The
   producer now emits one of three typed records, and which one it emits is
   the decision:

   `:present` — all three members are finite numbers:
     {:status :present
      :observed <number> :predicted-mean <number> :predicted-variance <number>
      :error <observed − predicted-mean>          ; R3a
      :precision <1 / max(variance, ε)>
      :weighted-error <error * precision>          ; R3b
      :producer-contract :prediction-error/v1}

   `:absent` — the OBSERVATION was not taken. The channel carries no numbers
   at all and the caller omits it from the update; the reason travels with the
   record (`:reason`, `:paths` from the observation envelope when the caller
   read it through `channel-prediction-error`). An observation nobody made is
   not an observation of zero.

   `:refused` — a MODEL parameter is missing, or any member is present but not
   a finite number. C130 §2 splits the triple deliberately: absence of an
   observation omits, absence or corruption of model output refuses, because a
   likelihood that produced no mean is a producer defect and must stay loud.
   The record names every offending member under `:offending`.

   The ε floor (`min-variance`) prevents division by zero when the
   likelihood reports certainty (variance ≈ 0). Default min-variance 0.01.

   Opts: `:min-variance`, plus `:observation-status` and `:channel`, which a
   caller reading through an observation envelope supplies so the emitted
   record can name what was missing and where."
  ([observed prediction] (compute-prediction-error observed prediction {}))
  ([observed prediction {:keys [min-variance observation-status channel]
                         :or {min-variance 0.01}}]
   (let [stamp (cond-> {:producer-contract prediction-error-contract}
                 channel (assoc :channel channel))
         mean-member (prediction-member prediction :mean)
         var-member (prediction-member prediction :variance)
         observed-malformed? (and (some? observed) (nil? (finite-double observed)))
         offending (cond-> (vec (remove #(= :present (:status %))
                                        [mean-member var-member]))
                     observed-malformed?
                     (conj {:member :observed :status :not-finite :value observed}))]
     (cond
       ;; Malformed or missing model output, or a non-numeric observation:
       ;; refuse loudly rather than score a substituted value.
       (seq offending)
       (merge stamp {:status :refused
                     :reason :malformed-prediction-triple
                     :offending offending})

       ;; Honestly absent observation: omit the channel, keep the reason.
       (nil? observed)
       (merge stamp
              {:status :absent :absent-member :observed}
              (or (not-empty (select-keys observation-status [:reason :paths]))
                  {:reason :observation-absent}))

       :else
       (let [pm (:value mean-member)
             pv (:value var-member)
             o (double observed)
             err (- o pm)
             precision (/ 1.0 (max pv min-variance))]
         (merge stamp
                {:status :present
                 :observed o
                 :predicted-mean pm
                 :predicted-variance pv
                 :error err
                 :precision precision
                 :weighted-error (* err precision)}))))))

(defn channel-prediction-error
  "`compute-prediction-error` for CHANNEL read through OBS's observation
   envelope, so an absent channel carries the envelope's own reason and paths
   instead of the 0.0 the caller used to substitute for it. This is the form
   the war-machine inner loop calls; the two-argument producer above stays
   available to callers that already hold a scalar."
  ([obs channel prediction] (channel-prediction-error obs channel prediction {}))
  ([obs channel prediction opts]
   (let [source (channel-source-status obs channel)
         present? (= :present (:status source))]
     (compute-prediction-error
      (when present? (:value source))
      prediction
      (cond-> (assoc opts :channel channel)
        (not present?)
        (assoc :observation-status (select-keys source [:reason :paths])))))))

;; ---------------------------------------------------------------------------
;; AC3 (Joe's 2026-09-02 ruling on C130 §3): strategic-mode inference.
;; ---------------------------------------------------------------------------

(def strategic-mode-contract
  "Producer contract stamped on every strategic-mode record `infer-mode-record`
   emits, whether it classified, reported `:unknown`, or refused."
  :strategic-mode/v1)

(def strategic-mode-features
  "The six observation channels the mode classifier branches on, in the order
   the branches read them.

   EVERY ONE IS REQUIRED, and that is the substance of C130 §3 option A. The
   classifier's last branch is `:else :multiplied`, so returning `:multiplied`
   asserts that none of the six other conditions held — a claim about all six
   features at once. There is no subset of these on which a partial
   classification is sound, and no prior/stale rule is specified here: a single
   absent feature makes the whole classification unsupported."
  [:stack-pct :consulting-pct :loop-health :active-repo-ratio
   :ticks-firing-ratio :depositing-signal])

(defn- classify-strategic-mode
  "The equilibrium classification itself, over six features every one of which
   the caller has already established is a finite number. Unchanged from the
   pre-AC3 branch structure; what changed is that it is no longer reachable
   with a substituted zero."
  [{:keys [stack-pct consulting-pct loop-health active-repo-ratio
           ticks-firing-ratio depositing-signal]}]
  (cond
    ;; Dark: nothing happening
    (and (< active-repo-ratio 0.2) (< loop-health 0.3))
    :dark

    ;; Depositing: consulting active (commit-based or frame-based)
    (or (> consulting-pct 0.2) (> depositing-signal 0.15))
    :depositing

    ;; Hermit: stack-dominated, no consulting AND no depositing signal
    (and (> stack-pct 0.7) (< consulting-pct 0.05) (< depositing-signal 0.05))
    :hermit

    ;; Scanning: stack-dominated but daily scans active (transitional)
    (and (> stack-pct 0.7) (> depositing-signal 0.0))
    :scanning

    ;; Foraging-trapped: stuck on stack under math/portfolio pressure
    (and (> stack-pct 0.5) (> ticks-firing-ratio 0.5))
    :foraging-trapped

    ;; Stagnant: surfaces used but not improving
    (and (> active-repo-ratio 0.3) (< loop-health 0.5))
    :stagnant

    ;; Multiplied: healthy balance
    :else
    :multiplied))

(defn infer-mode-record
  "Infer strategic mode from an observation vector, as a typed record.

   AC3 (Joe's 2026-09-02 ruling on C130 §3) removed the six zero substitutions
   this classifier used to read its features through. Each of
   `strategic-mode-features` is read through the observation envelope
   (`channel-source-status`), and which record comes back is the decision:

   `:present` — all six features are finite numbers:
     {:status :present :mode <one of the seven mode keywords>
      :features {<feature> <double> …}
      :producer-contract :strategic-mode/v1}

   `:unknown` — at least one required feature was NOT OBSERVED. `:mode` is
   `:unknown`, `:absent` names every missing feature with the envelope's own
   `:reason` and `:paths`, and no classification is offered. Six absences used
   to arrive as six zeros, which could fabricate `:dark` (active 0.0 < 0.2 and
   loop-health 0.0 < 0.3 — the branch an EMPTY scan reached), suppress
   `:depositing`, or help satisfy `:hermit`. A mode nobody could observe is not
   the mode `:dark`.

   `:refused` — a feature is present but is NOT A FINITE NUMBER (a string, a
   keyword, NaN, an infinity). `:mode` is `:unknown` and `:offending` names
   every such feature. Absence and malformation are separated here exactly as
   they are in `compute-prediction-error`: a feature nobody measured is a gap
   in the scan, a feature that arrived as a string is a producer defect and has
   to stay loud. When both occur the record refuses and still carries `:absent`,
   so neither fault is lost.

   Whether anything may ACT on `:unknown` is a separate question and is NOT
   decided here — it is the hard-guard decision (DECISIONS-PENDING §3). This
   producer's contract is only that it never invents a mode.

   Plain explicit maps remain a supported legacy boundary, through
   `channel-source-status`: a caller that hands over `{:stack-pct 0.2 …}`
   without observation metadata still gets its six features read, and the keys
   it left out are absent rather than zero."
  [obs]
  (let [stamp {:producer-contract strategic-mode-contract}
        readings (into {} (map (juxt identity #(channel-source-status obs %)))
                       strategic-mode-features)
        absent (vec (for [f strategic-mode-features
                          :let [r (get readings f)]
                          :when (= :absent (:status r))]
                      (merge {:feature f} (select-keys r [:reason :paths]))))
        offending (vec (for [f strategic-mode-features
                             :let [r (get readings f)]
                             :when (and (= :present (:status r))
                                        (nil? (finite-double (:value r))))]
                         {:feature f :status :not-finite :value (:value r)}))]
    (cond
      ;; Malformed feature: refuse loudly rather than classify around it.
      (seq offending)
      (merge stamp
             {:status :refused :mode :unknown
              :reason :malformed-mode-feature
              :offending offending}
             (when (seq absent) {:absent absent}))

      ;; Honestly absent feature: no classification, reason carried.
      (seq absent)
      (merge stamp
             {:status :unknown :mode :unknown
              :reason :required-feature-absent
              :absent absent})

      :else
      (let [features (into {} (map (fn [f]
                                     [f (finite-double
                                         (:value (get readings f)))]))
                           strategic-mode-features)]
        (merge stamp
               {:status :present
                :mode (classify-strategic-mode features)
                :features features})))))

(defn infer-mode
  "Strategic mode from an observation vector, as the bare keyword callers that
   only render or key off the mode still want. Exactly `(:mode
   (infer-mode-record obs))`.

   Returns one of :multiplied, :depositing, :foraging-trapped, :hermit,
   :scanning, :stagnant, :dark — or `:unknown` when a required feature was
   absent or malformed. `:unknown` carries no reason on its own; the reason
   lives on `infer-mode-record`, which is the form the war machine calls and
   persists."
  [obs]
  (:mode (infer-mode-record obs)))
