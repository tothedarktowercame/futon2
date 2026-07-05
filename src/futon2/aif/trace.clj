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
      :mu-post          <belief map; identical to mu-pre until R3d wires
                          observation-driven belief update>
      :observation      <obs channel map>
      :free-energy      {:G-pragmatic :G-epistemic :G-total
                          :per-channel :avoided-active}
      :ranked-actions   [{:action :G-risk :G-ambiguity :G-total :rank}]
      :decision         {:action :rank? :G-total? :tau? :reason?
                          :gap-report? ...}
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
            [futon2.aif.policy-precision :as policy-precision])
  (:import (java.io PushbackReader)
           (java.time Instant LocalDate ZoneId)
           (java.time.format DateTimeFormatter)))

(def ^:private default-trace-dir
  (str (System/getProperty "user.home") "/code/futon2/data/wm-trace"))

(def ^:private date-fmt
  (DateTimeFormatter/ofPattern "yyyy-MM-dd"))

(def ^:private utc-zone
  (ZoneId/of "UTC"))

(defn- today-date-string []
  (.format (LocalDate/now utc-zone) date-fmt))

(defn- daily-path
  "Path of the trace file for a given date (YYYY-MM-DD string) under
   the given directory. Defaults to today's date in the default dir."
  ([] (daily-path default-trace-dir (today-date-string)))
  ([dir date-str] (str dir "/wm-trace-" date-str ".edn")))

(defn- strip-ranked-action
  "Compact a ranked-action entry for trace — keep the EFE summary plus
   the action, drop the deeply-nested :prediction (recoverable on
   demand by re-running forward-model/predict). v0.13 added
   `:G-info` + `:G-survival`; v0.14 added `:time-pressure`; v0.15
   added `:horizon-steps`; v0.20 adds `:G-structural-pressure`."
  [r]
  ;; :G-goal-outcome (R19, 2026-07-02): the belly's predictive-risk term was
  ;; computed per action but STRIPPED here — so no flight could show the belly
  ;; steering. Persist it: the R19 analog of R16's :act-gate-verdicts audit.
  ;; :G-gap :G-graph-pragmatic :G-core (M-evaluate-policies D1a/D2, 2026-07-03):
  ;; same fix, same reason — these terms ENTER :G-total but were stripped, so
  ;; recomputing the total from persisted terms carried a hidden residual
  ;; (census: mean-abs 0.324). Whitelist ⊇ terms entering :G-total (invariant I4).
  ;; :score-provenance (D1b follow-up, same day): the cascade-row marker was
  ;; attached in war_machine.clj but stripped HERE — the D1a spec omitted it
  ;; from the whitelist (caught by the live post-deploy I-check: markers nil in
  ;; the 13:03Z tick). Invariant I2 needs it persisted, not just attached.
  ;; :risk-mode (D5a, 2026-07-03): whitelisted AT BIRTH with the key itself —
  ;; the :score-provenance lesson: a key that isn't persisted the day it is
  ;; emitted becomes a silent spec gap later.
  ;; :G-augmentation :augmentation-terms :G-graph-feasibility
  ;; :G-graph-pragmatic-proxy (B-2a/B-2b struct split, 2026-07-04): the named
  ;; multi-objective layer + the mask/value split of the graph term —
  ;; whitelisted AT BIRTH (same lesson); values are relabels of quantities
  ;; already entering :G-total, so I4 (whitelist ⊇ terms entering :G-total)
  ;; is preserved and the persisted-total residual stays 0.
  ;; :structural-pressure-mode :habit-prior-bias (D-1d dark build, 2026-07-04):
  ;; where structural pressure sits (always emitted, self-describing like
  ;; :risk-mode) + the relocated term's ln-E bias (dark mode only) —
  ;; whitelisted AT BIRTH.
  ;; :move-class-intensity-mode :G-move-class-intensity :move-class-intensity
  ;; (M-action-vocabulary P2 dark build, 2026-07-05): same birth rule. If the
  ;; dark term enters :G-total, the trace must carry both the signed G
  ;; contribution and the conditioning bundle that produced it.
  (select-keys r [:action :G-risk :G-ambiguity :G-info :G-survival
                  :G-structural-pressure :G-goal-outcome
                  :G-gap :G-graph-pragmatic :G-core :score-provenance :risk-mode
                  :goal-outcome-mode
                  :G-augmentation :augmentation-terms
                  :G-graph-feasibility :G-graph-pragmatic-proxy
                  :structural-pressure-mode :habit-prior-bias
                  :move-class-intensity-mode :G-move-class-intensity
                  :move-class-intensity
                  :G-total :rank :time-pressure :horizon-steps]))

(defn- strip-decision
  "Compact the decision for trace. The full softmax-weights map is
   keyed by action maps (non-stringable); drop it from the persisted
   form. Chosen-action / abstain identity is preserved."
  [d]
  (-> d
      (dissoc :softmax-weights :ranked-actions)
      ;; If the decision carries a gap-report, keep it (it's the
      ;; operator-facing output of the abstain branch).
      ))

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
     3 — adds :G-augmentation + :augmentation-terms (B-2a) and
         :G-graph-feasibility + :G-graph-pragmatic-proxy (B-2b) per ranked
         action — the struct-split relabels, additive only (2026-07-04).
     4 — adds :structural-pressure-mode (always) + :habit-prior-bias (dark
         :habit-prior mode only) per ranked action (D-1d, 2026-07-04). The
         ledger rule (any key-set change bumps) is broader than the D-1d
         parcel's top-level-only wording; the ledger rule wins."
  4)

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

(defn trace-record
  "Pure: construct a v1 trace record from a `judge`-style output map.
   Accepts a map carrying at minimum `:belief`, `:observation`,
   `:free-energy`, `:ranked-actions`, `:decision`, `:mode`.

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

   As of R14 (precision-over-policies), `:policy-precision` is propagated
   too — the policy-scale γ-state (`futon2.aif.policy-precision`): a single
   bounded inverse-temperature learned from the realized-vs-expected
   outcomes of chosen policies. Same cross-call read-back pattern as
   `:precision-state`; absent ⇒ the prior (γ=1.0) is reconstructed."
  [judge-output]
  (cond->
   {:timestamp (str (Instant/now))
    :mu-pre (or (:belief-pre judge-output) (:belief judge-output))
    :mu-post (:belief judge-output)
    :observation (:observation judge-output)
    :free-energy (:free-energy judge-output)
    :prediction-errors (:prediction-errors judge-output {})
    :precision-state (:precision-state judge-output {})
    ;; R14 precision-over-policies (γ): the policy-scale sibling of
    ;; :precision-state. The next tick reads this back to continue the rolling
    ;; realized-outcome window. Absent ⇒ the prior (γ=1.0) is reconstructed.
    :policy-precision (:policy-precision judge-output
                                         (policy-precision/initial-policy-precision-state))
    :micro-step-trace (:micro-step-trace judge-output [])
    :anticipation (:anticipation judge-output {:events-loaded? false :events []})
    :ranked-actions (mapv strip-ranked-action (:ranked-actions judge-output))
    :decision (strip-decision (:decision judge-output))
    :mode (:mode judge-output)}
    ;; R16 close-the-loop seam (interface paired with claude-10): the enactor
    ;; writes `:realized-outcome` at enactment; R14's γ reader consumes it next
    ;; tick (see `policy-precision/fold-realized-outcome`). Present-only —
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
    ;; B-0a tick provenance (present-only, schema v2): the scheduled runner
    ;; stamps it via `wm-version-stamp`; bare judge calls don't — purely
    ;; additive, no nil seam in un-stamped records.
    (:wm-version judge-output)
    (assoc :wm-version (:wm-version judge-output))))

(defn write-trace!
  "Append one trace record (constructed from a judge-style output) to
   the daily trace file. Creates the trace directory if absent. Returns
   the path written.

   Opts:
     :dir       — directory to write under (default `default-trace-dir`)
     :date-str  — override date string for the filename (default today UTC)"
  [judge-output & {:keys [dir date-str]
                   :or {dir default-trace-dir
                        date-str (today-date-string)}}]
  (let [record (trace-record judge-output)
        path (daily-path dir date-str)]
    (io/make-parents path)
    (spit path (str (pr-str record) "\n") :append true)
    path))

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
        start-date (.minusDays end-date (long (dec days)))]
    (last (read-trace-range start-date end-date :dir dir))))
