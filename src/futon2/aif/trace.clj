(ns futon2.aif.trace
  "Per-call trace persistence for the WM AIF apparatus (R8).

   Each call to `futon2.report.war-machine/judge` may emit one trace
   record summarising what the agent observed, believed, ranked, and
   chose. Records are appended as EDN-lines to a daily file under the
   configured trace directory:

     ~/code/futon2/data/wm-trace/wm-trace-YYYY-MM-DD.edn

   EDN-lines (one EDN value per line) preserves Clojure types natively
   (no keyword/string conversion) and remains streamable / appendable /
   re-parseable by `read-trace`.

   Contract: contributes to R8 (per-tick trace) per
   `futon2/docs/futon-aif-completeness.md`. Cross-maps to F7 (validation
   harness) at stack scope — trace records are the evidence R9 properties
   are checked against.

   Schema (v1):
     {:timestamp        <ISO-8601 string>
      :mu-pre           <belief map: entity-id → posterior>
      :mu-post          <belief map: entity-id → posterior, after this tick's
                          R3d belief update; carried forward as the next tick's
                          :mu-pre prior (reconciled to the new entity domain)
                          when belief/*carry-belief?* is on>
      :observation      <compatible numeric obs channel map>
      :observation-envelope <lossless tagged channel values/absences>
      :free-energy      {:preference-gap-score :coverage-uncertainty-pressure :controller-score
                          :per-channel :avoidance-by-channel :avoided-active}
      :decision         a cascade decision {:action {:kind :cascade-candidate
                          :cascade-id ... :precedence [...] :construction-receipt ...
                          :interpretation-receipts ...} :chosen-action-mass
                          :beta {:value :status} :selection-law {...} ...}
                          or a typed abstention {:status :abstained
                          :refusals [...]}
      :cascade-problems {:problems [...] :refusals [...]}
      :mode             <strategic-mode keyword>}

   Honest gap: prediction errors (ε per R8) are not yet recorded because
   R3a (predicted-observation likelihood model) is still partial. The
  trace schema will gain `:prediction-errors` when R3a lands.

   B-0a (M-aif-faithfulness §2.0, 2026-07-04): records written by the
   scheduled runner additionally carry `:wm-version` — the tick provenance
   stamp (git sha + dirty flag, the resolved mode/flag set, and
   `trace-schema-version`). Present-only; see `wm-version-stamp` /
   `wm-version-of`."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.forward-model :as forward-model]
            [futon2.aif.lane-futility :as lane-futility]
            [futon2.aif.observation :as observation])
  (:import (java.io PushbackReader)
           (java.time Instant LocalDate ZoneId)
           (java.time.format DateTimeFormatter)))

(def ^:private default-trace-dir
  (str (System/getProperty "user.home") "/code/futon2/data/wm-trace"))

(def ^:private date-fmt
  (DateTimeFormatter/ofPattern "yyyy-MM-dd"))

(def ^:private utc-zone
  (ZoneId/of "UTC"))

(def ^:dynamic *persist-policy-trace-details?*
  "Whether trace records retain the policy prediction and Q(π) diagnostics.
   Read once when this namespace loads. `FUTON_WM_TRACE_POLICY_DETAILS=1`
   enables the additive fields; absent or any other value preserves the
   historical byte shape. Dynamic binding exists only for isolated tests and
   offline measurement."
  (= "1" (System/getenv "FUTON_WM_TRACE_POLICY_DETAILS")))

(defn- today-date-string []
  (.format (LocalDate/now utc-zone) date-fmt))

(defn- daily-path
  "Path of the trace file for a given date (YYYY-MM-DD string) under
   the given directory. Defaults to today's date in the default dir."
  ([] (daily-path default-trace-dir (today-date-string)))
  ([dir date-str] (str dir "/wm-trace-" date-str ".edn")))

;; validated-machine-q admission RETIRED with the flat decision (SPEC
;; flat-removal H4, 2026-09-17): there are no ranked-action rows to
;; admit a Q/C pair for.
;; strip-ranked-action and the per-ranked-action machine-Q admission
;; RETIRED with the flat decision (SPEC flat-removal H4, 2026-09-17):
;; there are no ranked flat actions to strip.
(defn- cascade-candidate-id
  "Stringable identity of a cascade candidate. Candidates are maps
   ({:kind :cascade-candidate :cascade-id …}); the id is what a reader joins
   the persisted posterior on. Candidates with neither id are refused —
   persisting a hash-keyed posterior would make replay irreproducible."
  [candidate]
  (let [id (if (map? candidate)
             (or (:cascade-id candidate) (:id candidate))
             candidate)]
    (when-not (or (string? id) (keyword? id))
      (throw (ex-info "Cascade posterior candidate has no stable id"
                      {:refusal :cascade-candidate-unidentifiable
                       :path [:decision :selection-law :posterior]
                       :candidate candidate})))
    (str id)))

(defn- stringable-cascade-posterior
  "Re-key the decision's recorded posterior {candidate-map → p} by candidate
   id. Probabilities are untouched; only the join key becomes stringable. An
   absent posterior is an empty map (the abstention arm never reaches here)."
  [posterior]
  (if (map? posterior)
    (into {}
          (map (fn [[candidate p]] [(cascade-candidate-id candidate) p]))
          posterior)
    {}))

;; selection-proof-input fields RETIRED with the flat decision (SPEC
;; flat-removal H4, 2026-09-17); see the note below.
;; selection-proof-input validation RETIRED with the flat decision (SPEC
;; flat-removal H4, 2026-09-17). The envelope described the flat
;; selector policy table over ranked actions; the cascade decision
;; records its own posterior, beta and receipts, and no flat proof
;; envelope can be produced.
(defn- strip-decision
  "Persist the tick's decision for trace. Exactly two arms (SPEC
   flat-removal H4, Joe's 2026-09-17 ruling; the flat single-action
   decision is removed and cannot be produced):

   - a CASCADE decision from select-action-cascades: the chosen candidate
     (with its construction/interpretation receipts), :chosen-action-mass,
     :beta {:value :status}, :selection-law (posterior re-keyed by candidate
     id — candidate maps are not stable join keys), and the pattern-keyed
     :softmax-weights;
   - a typed abstention {:status :abstained :refusals […]}, persisted as-is.

   There is no flat branch and no compatibility read of :ranked-actions."
  [d]
  (if (= :abstained (:status d))
    ;; persisted as-is: the refusals ARE the record
    (assoc d :refusals (vec (:refusals d)))
    ;; the cascade decision is persisted whole — the candidate carries its
    ;; receipts — with ONE transformation: the recorded posterior's
    ;; candidate-map keys become candidate ids (a stable join key).
    (if (map? (:selection-law d))
      (update-in d [:selection-law :posterior] stringable-cascade-posterior)
      d)))

;; ---------------------------------------------------------------------------
;; B-0a tick provenance (M-aif-faithfulness §2.0, V-1) — which code, which
;; config produced this tick, answerable from the record alone.
;; ---------------------------------------------------------------------------

(def trace-schema-version
  "Monotonic integer version of the trace RECORD SHAPE. Bump on any change to
   the record's key set or key semantics (the append-only discipline means
   bumps are additive). Ledger:
     1 — the accreted pre-provenance shape: everything up to and including
         :goal-outcome-mode per ranked action (fb15d66, 2026-07-04).
     2 — adds :wm-version (B-0a, 2026-07-04).
     3 — adds :controller-augmentation + :augmentation-terms (B-2a) and
         :graph-feasibility-penalty + :graph-control-score-proxy (B-2b) per ranked
         action — the struct-split relabels, additive only (2026-07-04).
     4 — adds :structural-pressure-mode (always) + :habit-prior-bias (dark
         :habit-prior mode only) per ranked action (D-1d, 2026-07-04). The
         ledger rule (any key-set change bumps) is broader than the D-1d
         parcel's top-level-only wording; the ledger rule wins.
     5 — adds :ambiguity-mode per ranked action (D5c provenance for the
         gaussian-entropy nats ambiguity lane).
     6 — adds :G-efe per ranked action — the EFE readout
         (risk + ambiguity), additive. Model-uncertainty bonuses remain in the
         controller augmentation rather than masquerading as EIG.
     7 — adds the distinct per-tick :variational-free-energy diagnostic.
     8 — atomically renames the non-variational R14 controller state to
         :selection-gain and realised-outcome legs to :expected-score /
         :realized-score (2026-07-13).
     9 — atomically removes noncanonical G labels from controller augmentation,
         graph diagnostics, move-class contribution, current-observation
         diagnostics, and policy rollout; also renames the posterior-spread
         exploration term to :model-uncertainty-bonus (2026-07-13).
    10 — records the live/legacy disposition of predictability, homeostatic,
         and graph-feasibility controls per ranked action, and adds the
         top-level :policy-support-exclusions audit trail (2026-07-13).
    11 — records the production belief :likelihood-mode and the A/B/D model
         manifest hashes in :wm-version (2026-07-13).
    12 — records learned habit-prior state per tick and its source per ranked
         action (B1, dark build, 2026-07-13).
    13 — records Morning Brief QA events applied through the live A-matrix
         belief update, the consumed event ids, and any events held because
         their entity is outside the current belief domain (2026-07-14).
    14 — records capability-zone C load and Beta-predictive ambiguity provenance
         for learn-action-class candidates (2026-07-21).
    15 — records the exact C-entry, graph/durable projection, and typed q-sat
         inputs needed to replay goal-outcome risk per candidate (2026-08-31).
    16 — records the preference stack taken from the ranked evaluation objects,
         deduplicated once per tick with typed absent/partial/conflict variants
         (2026-08-31).
    17 — records the lossless observation envelope once per tick, derived from
         the same observation object as the compatible numeric projection;
         absent channels retain their reason and measured zero stays present
         (2026-08-31).
    18 — adds the support-typed scoring shadow. It records current and masked
         scores, support/absence provenance, comparability, and counterfactual
         rank while retaining zero selection authority (2026-08-31).
     19 — adds the tri-state :avoidance-by-channel diagnostic; missing
          observations are :unknown rather than tested as numeric zero
          (2026-08-31).
     20 — declares :producer-contract :r8/stored-f-controller-v1. R8 readers
          use this record-carried contract rather than inferring new-record
          semantics from the trace filename's day (C129, 2026-08-31).
     21 — removes the per-tick :variational-free-energy scalar added at 7 and
          declares :producer-contract :r8/retired-f-controller-v1. Joe's J2
          ruling retired the Laplace-channel F once its replacement F_pi was
          realised and consumed; :selection-gain and the controller-map
          free-energy shape are unchanged, which is why the R8 era needed a
          third member rather than a bumped version string (I5 slice (c),
          C473, 2026-09-01).
    22 — adds the decision's :selection-law map: which selection law was
         requested, which one ran, and — when they differ — the refusal
         reason (U10, 2026-09-02). Additive, and present on the
         :strategic-recommendation boundary only, which is the one the live
         path takes. Bumped because a reader must be able to tell “no
         :selection-law key because the producer predates the law” from
         “no key because the law was absent”; the decision's
         :f-pi-posterior (RUN9, 9867157) was added inside era 21 without a
         bump, so 22 is the first version that declares anything about the
         decision's selection law at all.
    23 — adds the present-only :mission-c C_mis readback (U11 (d),
         2026-09-02). Additive and default-off (FUTON_WM_MISSION_C), so no
         record's existing bytes change. Bumped for the same reason as 22 and
         under the ledger rule that any key-set change bumps: absence of
         :mission-c must be readable as \"this producer predates C_mis\" versus
         \"the flag was off on this tick\", and only the version separates
         them. It declares nothing about selection — the key is attached after
         the decision and no selection path reads it.
    24 — adds the present-only :mission-focus, the mission THIS tick selected
         (U21, 2026-09-03). Additive and default-off
         (FUTON_WM_SELECTION_FOCUS), so no record's existing bytes change.
         Bumped under the same ledger rule as 23: absence must be readable as
         \"this producer predates the selection focus\" versus \"the flag was
         off on this tick\". It is a SECOND field beside :active-mission, not a
         redefinition of it — 23's key stays the durable clock read, and 24's
         is a projection of the tick's own decision, so a reader can measure
         the lag between them. It declares nothing about selection: the key is
         attached after the decision and no selection path reads it.
    25 — adds :gauge-observables INSIDE the present-only :mission-c readback:
         one typed record per DECLARED GAUGE observable, `:measured` with the
         artifact it was read from and that artifact's sha256, or `:absent`
         with a reason and what would have to exist (U42, 2026-09-03).
         Additive, nested, and still default-off (FUTON_WM_MISSION_C), so no
         record's existing bytes change. Bumped under the ledger rule that ANY
         key-set change bumps — the rule is not top-level-only, as version 4
         already records. What the version separates: a :mission-c record with
         no :gauge-observables is one whose producer PREDATES the gauge
         producers, not one on which every producer came back absent; the
         producers always emit a record per declared observable, so `absent`
         at 25 and later is a measurement about the artifacts, not about the
         code. It declares nothing about selection: the observables are merged
         into the observation the READBACK reads only, never into the tick's
         own observation, so no channel, weight, G term, admissibility verdict
         or selector can see one.
    26 - adds the decision's present-only :enumeration-completeness record:
         per kind, the population an INDEPENDENT filesystem scan found
         available, the candidates this tick enumerated, the membership diff
         both ways, and a typed reason for every exclusion (U37, 2026-09-03).
         Additive and flag-gated (FUTON_WM_ENUMERATION_ASSERT; default ON
         since 2026-09-19, =0 disarms), so no
         record's existing bytes change. Bumped under the ledger rule that any
         key-set change bumps, and for a reason specific to this key: a reader
         who finds no :enumeration-completeness on a record must be able to
         tell \"this producer predates the check\" from \"the check ran and the
         enumeration was complete\" -- the second would be a false clean bill,
         and only the version separates them. It declares nothing about
         selection: the record is attached after the decision and no selection
         path reads it.
    27 - every write carries a :TRACE route hop whose :reason names either the
         routing rule and answerable operator question, or the explicit
         :trace-route-reason-missing machine-triage rule. The writer supplies
         this invariant, including for callers that predate reasoned routes
         (PA14z, 2026-09-08).
    28 - adds the complete present-only :machine-q Q/C pair to each ranked
         action that the opt-in scorer evaluated. Partial pairs and ordered
         support disagreement refuse before append (row 14, 2026-09-12).
    29 - adds present-only :cohort-attempt to traces produced by a full-loop
         cohort attempt. The literal cohort/attempt identity is threaded by
         the runner; ordinary and scheduled ticks remain byte-identical.
    30 - the flat decision is removed (SPEC flat-removal-and-cascade-decision
         H4, Joe's 2026-09-17 ruling). :decision is now a cascade decision
         (chosen candidate with receipts, :chosen-action-mass, :beta,
         :selection-law with the posterior re-keyed by candidate id) or a
         typed {:status :abstained :refusals [...]}. :ranked-actions,
         :preference-stack, :support-typed-scoring-shadow,
         :policy-support-exclusions and :default-mode-events are REMOVED, and
         the top-level :selection-gain statistic (the flat controller's
         gamma, previously defaulted silently) is no longer written:
         each was a projection of the flat scoring lane. :cascade-problems
         {:problems [...] :refusals [...]} is added beside the decision.
         Bumped (not additive) because a reader must distinguish \"no ranked
         actions because the producer predates the cascade ruling\" from
         \"none because the flat path can never run again\"."
  30)

(def r8-producer-contract
  "Contract carried by trace records that require selection gain and the
   controller-map free-energy shape and that carry NO stored variational F.
   Its predecessor :r8/stored-f-controller-v1 required all three; F was retired
   by I5 slice (c) on Joe's J2 ruling, and the two surviving requirements are
   why checks/r8_f_contract.clj now reads a per-era expectation rather than the
   identity stored? = gain? = controller?."
  :r8/retired-f-controller-v1)

;; preference-stack-evidence and support-typed-scoring-shadow RETIRED
;; with the flat decision (SPEC flat-removal H4, 2026-09-17): both
;; read the flat ranked-action entries that no longer exist. The
;; per-tick preference stack and the support-typed shadow were
;; projections of the flat scoring lane.

(defn- futon2-git-version
  "Git identity of the futon2 checkout this JVM loaded its code from:
   {:git-sha <full sha> :git-dirty? <bool>}. `:git-dirty?` counts TRACKED
   modifications only (`--untracked-files=no`) — the build-stamp convention:
   untracked holes/-docs churn doesn't change which code ran, a modified
   tracked source does. Only trustworthy from a one-shot JVM (the scheduled
   runner): a long-running JVM's loaded code can predate the working tree.
   Non-throwing: any git failure ⇒ {:git-sha :unknown :git-dirty? :unknown}
   (a stamp that can kill the tick would invert B-0a's purpose)."
  []
  (try
    (let [dir (str (System/getProperty "user.home") "/code/futon2")
          sha (shell/sh "git" "-C" dir "rev-parse" "HEAD")
          dirty (shell/sh "git" "-C" dir "status" "--porcelain" "--untracked-files=no")]
      (if (and (zero? (:exit sha)) (zero? (:exit dirty)))
        {:git-sha (str/trim (:out sha))
         :git-dirty? (not (str/blank? (:out dirty)))}
        {:git-sha :unknown :git-dirty? :unknown}))
    (catch Exception _ {:git-sha :unknown :git-dirty? :unknown})))

(defn wm-version-stamp
  "Build the `:wm-version` provenance map: the futon2 git identity merged with
   the caller-RESOLVED mode/flag set (the arena fns + the runner's switches —
   see `wm-scheduled-run`; resolution stays with the fns the tick actually
   uses, never a second env read here) plus `:trace-schema-version`. The
   scheduled runner assocs this onto the judgement before `write-trace!`."
  [resolved-flags]
  (merge (futon2-git-version)
         resolved-flags
         {:trace-schema-version trace-schema-version}))

(defn wm-version-of
  "B-0a acceptance accessor: recover the provenance stamp (sha + flags) from a
   trace record — no human correlation step. Nil for pre-B-0a records."
  [record]
  (:wm-version record))

(def trace-evidence-fields
  "Evidence fields added by the rapid v15-v20 trace sequence. Paths name the
   persisted location; `:introduced` is the first contract that requires it."
  {:goal-outcome-replay-inputs {:path [:ranked-actions 0 :goal-outcome-replay-inputs]
                                :introduced 15}
   :preference-stack {:path [:preference-stack] :introduced 16}
   :observation-envelope {:path [:observation-envelope] :introduced 17}
   :support-typed-scoring-shadow {:path [:support-typed-scoring-shadow]
                                  :introduced 18}
   :avoidance-by-channel {:path [:free-energy :avoidance-by-channel]
                          :introduced 19}
   :producer-contract {:path [:producer-contract] :introduced 20}})

(defn trace-field-evidence
  "Read one versioned evidence field without turning version skew into nil or
   a numeric/container default. Missing pre-contract fields are typed legacy
   absence; a field required by the record's declared version is malformed."
  [record field]
  (let [{:keys [path introduced]} (get trace-evidence-fields field)
        version (get-in record [:wm-version :trace-schema-version])
        missing (Object.)
        value (when path (get-in record path missing))]
    (cond
      (nil? path)
      {:status :absent :reason :unknown-field-contract :field field}

      ;; Fields retired at schema 30 (flat-decision removal, SPEC H4,
      ;; 2026-09-17): a v30+ record can never carry them.
      (and (number? version) (>= version 30)
           (contains? #{:goal-outcome-replay-inputs :preference-stack
                        :support-typed-scoring-shadow} field))
      {:status :absent :reason :retired-at-schema-30 :field field
       :record-schema-version version}

      (not (identical? missing value))
      {:status :present :value value :record-schema-version (or version :unversioned)}

      (or (nil? version) (< version introduced))
      {:status :absent :reason :predates-field :field field
       :introduced-version introduced
       :record-schema-version (or version :unversioned)}

      :else
      {:status :absent :reason :malformed :field field
       :introduced-version introduced :record-schema-version version})))

(defn trace-record
  "Pure: construct a v1 trace record from a `judge`-style output map.
   Accepts a map carrying at minimum `:belief`, `:observation`,
   `:free-energy`, `:decision`, `:mode`. As of schema 30 (SPEC
   flat-removal H4, 2026-09-17) the decision is a cascade decision or a
   typed abstention, there is no `:ranked-actions` field, and the
   judgement's `:cascade-problems` is persisted beside the decision.

   As of v0.10 (R3a landed), `:mu-pre` and `:mu-post` are read from
   `:belief-pre` and `:belief` respectively; when `:belief-pre` is
   absent (callers that don't compute it) both default to `:belief`,
   preserving forward-compatibility. `:prediction-errors` is also
   propagated — the R8 schema field flagged as gap-pending in v0.7
   now lands as R3a likelihood models become available.

   As of v0.12 (R7 landed), `:precision-state` is also propagated —
   per-channel precision tracked across calls via prediction-error
   history. Subsequent calls read this field to continue the rolling
   window; trace-record itself is pure (read-side is `judge`'s
   responsibility).
   The flat controller's `:selection-gain` γ-state is RETIRED at schema 30
   (SPEC flat-removal H4, 2026-09-17): β is caller-declared on the cascade
   selection path, and no silent default statistic is written."
  [judge-output]
  (let [observed (:observation judge-output)
        ;; C104: derive both persisted views from the one evaluation object.
        ;; Calling observe again here could let the trace disagree with scoring.
        observed-envelope (observation/observation-envelope observed)]
    (cond->
   {:timestamp (str (Instant/now))
    :producer-contract r8-producer-contract
    :mu-pre (or (:belief-pre judge-output) (:belief judge-output))
    :mu-post (:belief judge-output)
    :observation observed
    :observation-envelope observed-envelope
    :free-energy (:free-energy judge-output)
    :prediction-errors (:prediction-errors judge-output {})
    :precision-state (:precision-state judge-output {})
    ;; :selection-gain RETIRED with the flat decision (SPEC flat-removal H4,
    ;; 2026-09-17, review fix): persisting the old controller γ behind a
    ;; silent default would write a statistic no producer produced.
    :micro-step-trace (:micro-step-trace judge-output [])
    :morning-brief-events (:morning-brief-events judge-output [])
    :morning-brief-held-events (:morning-brief-held-events judge-output [])
    :morning-brief-consumed-event-ids
    (:morning-brief-consumed-event-ids judge-output [])
    :anticipation (:anticipation judge-output {:events-loaded? false :events []})
    ;; Schema 30 (SPEC flat-removal H4): no :ranked-actions, no
    ;; :preference-stack, no :policy-support-exclusions — all were projections
    ;; of the flat scoring lane. The candidate population now lives inside
    ;; the cascade decision's recorded posterior.
    :decision (strip-decision (:decision judge-output))
    :cascade-problems (:cascade-problems judge-output)
    :mode (:mode judge-output)}
    (contains? judge-output :horizon-steps)
    ;; Row 15 depth capture. A present nil is the observed scorer input, not a
    ;; missing value: it selects EFE's single-step path.
    (assoc :horizon-steps (:horizon-steps judge-output))
    (contains? judge-output :policy-depth-used)
    (assoc :policy-depth-used (:policy-depth-used judge-output))
    ;; RUN10 topology evidence. A measured nine-hop route adds 1,093 bytes to
    ;; a 1,042,451-byte policy-detail trace record (0.105%), so it is retained
    ;; unconditionally rather than coupled to the much larger detail flag.
    ;; Present-only: an absent or empty route means there is no traversal to
    ;; claim, and therefore no :wm/route key.
    (seq (:wm/route judge-output))
    (assoc :wm/route (:wm/route judge-output))
    ;; AC1 (Joe's 2026-09-02 C130 ruling, the self-repair condition): the
    ;; typed records the prediction-triple producer emitted instead of a
    ;; fabricated zero -- one per omitted-because-unobserved channel, one per
    ;; refused-because-malformed channel. Persisting them is what lets the
    ;; harvester turn a refusal into a work item rather than a standing red.
    ;; Present-only: no key means every channel's triple was complete, which
    ;; is a different claim from "the producer did not report".
    (seq (:prediction-triple-events judge-output))
    (assoc :prediction-triple-events (:prediction-triple-events judge-output))
    ;; AC2 (same ruling, same condition): the typed records the R3d belief
    ;; aggregator emitted instead of substituting zero for a missing
    ;; weighted-error or precision -- one per omitted channel, one per
    ;; rejected entry. Present-only: no key means every channel in the
    ;; aggregation contributed, which is a different claim from "the
    ;; aggregator did not report".
    (seq (:belief-aggregation-events judge-output))
    (assoc :belief-aggregation-events (:belief-aggregation-events judge-output))
    ;; AC3 (same ruling, same condition): the strategic-mode record, kept only
    ;; when mode inference could not classify -- a required feature absent
    ;; (`:unknown`) or malformed (`:refused`). Present-only: no key means the
    ;; tick's `:mode` was inferred from six observed features, which is a
    ;; different claim from "the classifier did not report". The single record
    ;; carries every offending feature, so this is a vector of at most one.
    (seq (:strategic-mode-events judge-output))
    (assoc :strategic-mode-events (:strategic-mode-events judge-output))
    ;; AC4 (:default-mode-events) RETIRED with the flat decision's fallback
    ;; selector (SPEC flat-removal H4, 2026-09-17): the I6 fallback no longer
    ;; exists, so no sorry-pressure record can be produced.
    ;; RUN11 run identity. The per-date trace file is shared by every run on
    ;; the day, so without this key the only discriminator between two runs'
    ;; records is the timestamp and a reader has to select by range. `:run/id`
    ;; is the SAME key and the same value the tick's receipt carries
    ;; (`run_tick_once.clj/tick-run-record`), so a receipt and its trace record
    ;; join literally. MEASURED cost, on the twenty S1b policy-detail records
    ;; (holes/labs/wm-contract/runs/2026-09-01-s1b/wm-trace-s1b.edn): +48 bytes
    ;; on a 1,045,286-byte record, 0.0046%, identical on every record because
    ;; a UUID string has fixed width. So it is unconditional, not flagged.
    ;; Present-only: a producer that has no run id (the scheduled runner,
    ;; `scripts/wm_scheduled_run.clj:113`, and the full-loop runner,
    ;; `src/futon2/aif/full_loop_runner.clj:2639`, neither of which mints one)
    ;; writes no key — an absent `:run/id` means the producer could not name a
    ;; run, NOT that the record belongs to an unnamed one.
    (:run/id judge-output)
    (assoc :run/id (:run/id judge-output))
    (:cohort-attempt judge-output)
    (assoc :cohort-attempt (:cohort-attempt judge-output))
    (:policy-depth judge-output)
    (assoc :policy-depth (:policy-depth judge-output))
    ;; U52 ladder judgement. The judge attaches the ladder's own record and
    ;; its typed refusals to the judgement; the per-action rung annotations
    ;; survive via `strip-ranked-action` but these two top-level fields are
    ;; what carries the refusal census, and without them a ladder-on tick is
    ;; unauditable from its trace (C511: the first ladder-on step could not
    ;; state its refusal count). Present-only: an absent key means the ladder
    ;; did not run, not that it ran and refused nothing — and it is what keeps
    ;; the default (flag-off) record byte-identical.
    (:task-belief-ladder judge-output)
    (assoc :task-belief-ladder (:task-belief-ladder judge-output)
           :task-belief-refusals (:task-belief-refusals judge-output))
    ;; S4 durable mission focus. Present only behind FUTON_WM_CLOCK_FOCUS=1;
    ;; the enabled record always carries either the typed focus or its typed
    ;; absence, while default-OFF trace bytes remain unchanged.
    (contains? judge-output :active-mission)
    (assoc :active-mission (:active-mission judge-output))
    ;; U11 (d) C_mis readback. Present only behind FUTON_WM_MISSION_C=1; the
    ;; enabled record always carries either the per-mission-action risk_mis
    ;; fields or the typed absence that says which of the five links broke,
    ;; while default-OFF trace bytes remain unchanged. Nothing in selection
    ;; reads it -- it is attached after the decision (`carry-mission-c`).
    (contains? judge-output :mission-c)
    (assoc :mission-c (:mission-c judge-output))
    ;; U21 selection focus. Present only behind FUTON_WM_SELECTION_FOCUS=1;
    ;; the enabled record carries the mission THIS tick selected, what the S4
    ;; durable read said beside it, and whether the two agree, while
    ;; default-OFF trace bytes remain unchanged. It is a SECOND field rather
    ;; than a redefinition of `:active-mission` above, because a projection of
    ;; the tick's own decision and a witnessed clock edge are not the same
    ;; claim; without both on the record the lag between them is invisible.
    ;; Attached after the decision (`carry-mission-focus`) and read by nothing
    ;; in selection.
    (contains? judge-output :mission-focus)
    (assoc :mission-focus (:mission-focus judge-output))
    ;; R16 close-the-loop seam (interface paired with claude-10): the enactor
    ;; writes `:realized-outcome` at enactment; R14's γ reader consumes it next
    ;; tick (see `selection-gain/fold-realized-outcome`). Present-only —
    ;; LIVE-WIRED 2026-07-02 (Joe-ratified; `futon2.aif.enact` in the scheduled
    ;; runner); absent whenever nothing enacted, which keeps γ at its prior.
    (:realized-outcome judge-output)
    (assoc :realized-outcome (:realized-outcome judge-output))
    ;; R16 audit fields (present-only): the per-tick act-gate verdicts and the
    ;; enactment summary — so a trace reader can see WHAT the gate decided and
    ;; what was enacted, not just the γ-facing outcome record.
    (seq (:act-gate-verdicts judge-output))
    (assoc :act-gate-verdicts (:act-gate-verdicts judge-output))
    (:enactment judge-output)
    (assoc :enactment (:enactment judge-output))
    ;; B1 learned E(π), dark and present-only: the flag-off trace shape remains
    ;; unchanged, while enabled ticks carry the sufficient-statistic state.
    (:strategic-habit-state judge-output)
    (assoc :strategic-habit-state (:strategic-habit-state judge-output))
    (:habit-prior-state judge-output)
    (assoc :habit-prior-state (:habit-prior-state judge-output))
    (:accumulation-state judge-output)
    (assoc :accumulation-state (:accumulation-state judge-output)
           :accumulation-update-input (:accumulation-update-input judge-output))
    (:accumulation-initialization judge-output)
    (assoc :accumulation-initialization (:accumulation-initialization judge-output))
    ;; I3: one keyword per tick, not per candidate, and inside the flag so the
    ;; default record stays byte-identical. Storing the prediction itself means
    ;; REPLAY does not need the mode — but READING does: under
    ;; `forward-model/*effects-mode*` :constant every same-type candidate
    ;; predicts identically, so a flat per-candidate field cannot be told from a
    ;; machine with no discrimination unless the mode is on the record
    ;; (C462 §6 item 3).
    *persist-policy-trace-details?*
    (assoc :effects-mode forward-model/*effects-mode*)
    ;; I2(c) dark F_pi readback. Present only when the separately default-off
    ;; FUTON_WM_FPI_DARK path put a result on the judge output. Nothing in
    ;; policy selection reads either field.
    (contains? judge-output :f-pi-by-candidate-id)
    (assoc :f-pi-by-candidate-id (:f-pi-by-candidate-id judge-output)
           :f-pi-provenance (:f-pi-provenance judge-output))
    ;; RUN7 / stage S2 dark beta carry. Present only when the separately
    ;; default-off FUTON_WM_BETA_DARK path put a state on the judge output.
    ;; This is the field the NEXT tick reads its carried beta back out of, so
    ;; unlike :f-pi-by-candidate-id it closes a loop through the trace -- and
    ;; the loop is still dark, because nothing in selection reads it at either
    ;; end. `futon2.aif.policy-precision/coerce-state` is the read-side guard.
    ;; The persisted state drops the two 110-element distributions (pi, pi_0)
    ;; and keeps the scalars, so the cost is a few hundred bytes on a ~1 MB
    ;; policy-detail record rather than the tens of kilobytes the vectors would
    ;; add.
    (contains? judge-output :policy-precision-state)
    (assoc :policy-precision-state (:policy-precision-state judge-output))
    ;; B-0a tick provenance (present-only, schema v2): the scheduled runner
    ;; stamps it via `wm-version-stamp`; bare judge calls don't — purely
    ;; additive, no nil seam in un-stamped records.
    (:wm-version judge-output)
    (assoc :wm-version (:wm-version judge-output)))))

(def ^:private missing-trace-reason
  {:kind :machine-triage
   :rule :trace-route-reason-missing
   :question "Which producer routing rule should replace this missing TRACE reason?"})

(defn- reasoned-trace-route
  [judge-output]
  (let [reason (or (:trace/reason judge-output) missing-trace-reason)
        route (vec (:wm/route judge-output))
        trace-index (first (keep-indexed (fn [i hop]
                                           (when (= :TRACE (:node hop)) i))
                                         route))]
    (if (some? trace-index)
      (assoc-in route [trace-index :reason]
                (or (get-in route [trace-index :reason]) reason))
      (conj route {:node :TRACE
                   :via "futon2.aif.trace/write-trace!"
                   :at (str (Instant/now))
                   :reason reason}))))

(defn write-trace!
  "Append one trace record (constructed from a judge-style output) to
   the daily trace file. Creates the trace directory if absent. Returns
   the path written. With `:return-record? true`, return
   `{:path <path> :record <exact-record-written>}`; the default return value and
   persisted bytes are unchanged. This lets a post-write witness cite the
   record's own timestamp/run id without constructing a second record.

   Opts:
     :dir       — directory to write under (default `default-trace-dir`)
     :date-str  — override date string for the filename (default today UTC)
     :return-record? — return the exact record alongside the path (default false)"
  [judge-output & {:keys [dir date-str return-record?]
                   :or {dir default-trace-dir
                        date-str (today-date-string)}}]
  (let [judge-output (cond-> (assoc judge-output
                                    :wm/route
                                    (reasoned-trace-route judge-output))
                       (:wm-version judge-output)
                       (assoc-in [:wm-version :trace-schema-version]
                                 trace-schema-version))
        record (trace-record judge-output)
        path (daily-path dir date-str)
        written-path (do
                       (io/make-parents path)
                       (lane-futility/append-indexed-trace! dir path record))]
    (if return-record?
      {:path written-path :record record}
      written-path)))

(def ^:private default-tag-reader
  "Tolerant default-tag reader: any unknown EDN tag becomes
   `{:trace/edn-tag <tag> :trace/value <form>}` instead of throwing. This
   keeps `read-trace` robust against schema drift (e.g. an interim record
   was written with a non-EDN-readable form before a serialisation fix
   landed) — old records remain parseable."
  (fn [tag value] {:trace/edn-tag tag :trace/value value}))

(defn read-trace
  "Read all trace records from a single daily file. Returns a vector of
   records, or `[]` if the file doesn't exist. Records that fail to parse
   (malformed EDN past tag-recovery) are skipped silently — a broken
   record doesn't poison subsequent readback."
  [& {:keys [dir date-str]
      :or {dir default-trace-dir
           date-str (today-date-string)}}]
  (let [path (daily-path dir date-str)
        f (io/file path)
        opts {:eof ::eof :default default-tag-reader}]
    (if (.exists f)
      (with-open [rdr (io/reader f)
                  pbr (PushbackReader. rdr)]
        (loop [out []]
          (let [next-val (try (edn/read opts pbr)
                              (catch Exception _ ::skip))]
            (cond
              (= ::eof next-val) out
              (= ::skip next-val) (recur out)
              :else (recur (conj out next-val))))))
      [])))

(defn read-trace-range
  "Read trace records across a date range (inclusive). `start-date` and
   `end-date` are `LocalDate` instances. Returns a vector of all records
   in chronological order across the files."
  [start-date end-date & {:keys [dir] :or {dir default-trace-dir}}]
  (let [dates (->> (iterate #(.plusDays % 1) start-date)
                   (take-while #(not (.isAfter % end-date))))]
    (vec (mapcat #(read-trace :dir dir
                              :date-str (.format % date-fmt))
                 dates))))

(defn- trace-files [dir]
  (let [root (io/file dir)]
    (if (.isDirectory root)
      (->> (.listFiles root)
           (filter #(.isFile %))
           (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn"
                                (.getName %)))
           (sort-by #(.getName %)))
      [])))

(defn read-all-traces
  "Read every `wm-trace-YYYY-MM-DD.edn` file in lexical/date order. This is the
   deterministic cold-start fold used by the dark learned habit prior; callers
   that already have persisted state should use that state instead."
  [& {:keys [dir] :or {dir default-trace-dir}}]
  (let [files (trace-files dir)]
    (vec (mapcat (fn [f]
                   (let [date-str (subs (.getName f) 9 19)]
                     (read-trace :dir dir :date-str date-str)))
                 files))))

(defn recent-trace-records
  "Return at most n trace records in chronological order without reading the
  full corpus. Daily files are visited newest-first until the bound is met."
  [n & {:keys [dir] :or {dir default-trace-dir}}]
  (let [limit (max 0 (long n))]
    (if (zero? limit)
      []
      (loop [files (reverse (trace-files dir))
             newest-first []]
        (if (or (>= (count newest-first) limit) (empty? files))
          (->> newest-first (take limit) reverse vec)
          (let [file (first files)
                date-str (subs (.getName file) 9 19)
                records (read-trace :dir dir :date-str date-str)]
            (recur (rest files)
                   (into newest-first (reverse records)))))))))

(defn reduce-traces
  "Chronologically reduce the trace corpus without retaining it in memory.
  At most one daily file's parsed records is resident at a time."
  [rf init & {:keys [dir] :or {dir default-trace-dir}}]
  (reduce (fn [acc f]
            (let [date-str (subs (.getName f) 9 19)]
              (reduce rf acc (read-trace :dir dir :date-str date-str))))
          init
          (trace-files dir)))

(defn latest-trace-record
  "Return the most recent trace record visible within a bounded UTC day
   window. This is the safe read path for cross-midnight continuity:
   just after midnight UTC, today's file may still be empty while
   yesterday's latest record carries the last `:precision-state`.

   Opts:
     :dir           — trace directory (default `default-trace-dir`)
     :end-date      — inclusive LocalDate upper bound (default today UTC)
     :lookback-days — number of UTC day buckets to scan, inclusive
                      (default 2)."
  [& {:keys [dir end-date lookback-days]
      :or {dir default-trace-dir
           end-date (LocalDate/now utc-zone)
           lookback-days 2}}]
  (let [days (max 1 (int lookback-days))
        start-date (.minusDays end-date (long (dec days)))
        recent (last (read-trace-range start-date end-date :dir dir))
        fallback-file (->> (trace-files dir)
                           (filter #(not (pos? (compare (subs (.getName %) 9 19)
                                                        (str end-date)))))
                           last)
        fallback (when fallback-file
                   (last (read-trace :dir dir
                                     :date-str (subs (.getName fallback-file) 9 19))))]
    ;; A planned or accidental pause must not silently reset the carried
    ;; posterior, precision, selection gain, habit prior, or consumed QA ids.
    ;; The fallback reads only the newest eligible daily file, not the corpus.
    (or recent fallback)))
