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
  trace schema will gain `:prediction-errors` when R3a lands."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
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
  (select-keys r [:action :G-risk :G-ambiguity :G-info :G-survival
                  :G-structural-pressure
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
   responsibility)."
  [judge-output]
  {:timestamp (str (Instant/now))
   :mu-pre (or (:belief-pre judge-output) (:belief judge-output))
   :mu-post (:belief judge-output)
   :observation (:observation judge-output)
   :free-energy (:free-energy judge-output)
   :prediction-errors (:prediction-errors judge-output {})
   :precision-state (:precision-state judge-output {})
   :micro-step-trace (:micro-step-trace judge-output [])
   :anticipation (:anticipation judge-output {:events-loaded? false :events []})
   :ranked-actions (mapv strip-ranked-action (:ranked-actions judge-output))
   :decision (strip-decision (:decision judge-output))
   :mode (:mode judge-output)})

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
