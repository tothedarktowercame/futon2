(ns futon2.aif.anticipation
  "Forward-axis anticipation reader for the WM AIF apparatus (v0.13).

   The canonical anticipation surface lives at `~/code/calendar/events.edn`
   per `~/code/futon5a/README-anticipation.md`. Each event is a typed
   AIF-shaped prior over when-it-fires + whether-it-fires + (optional)
   EFE-sketch over outcome branches.

   v0.13 scope is READ-ONLY: load the canonical source, expose
   `events-in-horizon` (events whose `:event/at` falls within a window
   from now), and pass-through to trace so the operator can SEE what's
   upcoming. Time-conditioned preferences (R5 → v0.14 candidate) and
   multi-horizon EFE composition (R4 → v0.15 candidate) build on this
   foundation but are NOT in v0.13.

   Cross-references:
   - `~/code/calendar/events.edn` — canonical source
   - `~/code/futon5a/README-anticipation.md` — framing
   - `~/code/futon2/docs/ants-aif-audit.md` §'Anticipation integration' — design ladder
   - `~/code/futon7/holes/M-interim-director-proxy-metric-inventory.md` §2.A.2.38 — full design"
  (:require [clojure.edn :as edn])
  (:import (java.time Duration Instant)))

(def default-events-path
  (str (System/getProperty "user.home") "/code/calendar/events.edn"))

(defn load-anticipations
  "Read the canonical anticipation source. Returns the full document map
   (`{:meta ... :events [...] :resolutions [...]}`) or `nil` on read failure
   — the WM remains operational under intermittent substrate availability."
  ([] (load-anticipations default-events-path))
  ([path]
   (try
     (edn/read-string {:readers {'inst #(Instant/parse %)}} (slurp path))
     (catch Exception _ nil))))

(defn- ->instant
  "Coerce :event/at into a `java.time.Instant`. Accepts Instant, Date, or
   ISO-8601 string; returns nil on unrecognised input."
  [v]
  (cond
    (instance? Instant v) v
    (instance? java.util.Date v) (.toInstant ^java.util.Date v)
    (string? v) (try (Instant/parse v) (catch Exception _ nil))
    :else nil))

(defn events-in-horizon
  "Return events from `doc` whose `:event/at` falls within `horizon` (a
   `java.time.Duration`) of `now` (a `java.time.Instant`). Events with
   malformed or missing `:event/at` are dropped silently. Sorted ascending
   by `:event/at`."
  [doc now horizon]
  (let [deadline (.plus ^Instant now ^Duration horizon)]
    (->> (:events doc)
         (keep (fn [event]
                 (when-let [at (->instant (:event/at event))]
                   (when (and (.isAfter at now)
                              (not (.isAfter at deadline)))
                     (assoc event ::at-instant at)))))
         (sort-by ::at-instant)
         vec)))

(defn summarise-event
  "Compact projection of an event for the trace's `:anticipated-events`
   field — drops verbose prose (`:event/p-fires-rationale`,
   `:event/notes`, `:event/efe-sketch` nested rationales) and keeps the
   typed structure that downstream R5/R6 work will compose against.

   `:event/at` is coerced to an ISO-8601 string (rather than a
   `java.time.Instant`) so the EDN-lines trace store can read it back
   without needing custom tag readers — pr-str on Instant produces a
   non-EDN-readable `#object[...]` form."
  [event]
  (-> (select-keys event [:event/id
                          :event/kind
                          :event/at
                          :event/at-precision
                          :event/p-fires
                          :event/basin
                          :event/mission
                          :event/lifecycle-status
                          :event/next-gate
                          :event/visibility])
      (update :event/at (fn [at]
                          (cond
                            (instance? Instant at) (str at)
                            (instance? java.util.Date at) (str (.toInstant ^java.util.Date at))
                            :else at)))))

(defn- event-seconds-until
  "Seconds from `now` until `event`'s `:event/at`; nil if unparseable
   or in the past."
  [now event]
  (when-let [at (->instant (:event/at event))]
    (let [delta-ms (- (.toEpochMilli at) (.toEpochMilli ^Instant now))]
      (when (pos? delta-ms)
        (/ (double delta-ms) 1000.0)))))

(defn time-pressure
  "v0.14 — urgency signal in [0,1] derived from anticipation snapshot.

   Returns 1.0 when an event in the snapshot is AT the deadline (now);
   0.0 when no events are within the horizon; linearly interpolates
   between based on the closest event's proximity. The interpretation:
   higher time-pressure ⇒ deadlines are looming ⇒ the WM should
   prioritise current-state alignment over exploration.

   For a snapshot with multiple events, the CLOSEST event dominates
   (most urgent wins). Events with no parseable `:event/at` or already
   in the past are ignored.

   Pure: same `(snapshot, now)` → same output."
  [snapshot now]
  (if (not (:events-loaded? snapshot))
    0.0
    (let [horizon-seconds (* 86400.0 (double (:horizon-days snapshot)))
          deltas (keep #(event-seconds-until now %) (:events snapshot))]
      (if (empty? deltas)
        0.0
        (let [closest-seconds (apply min deltas)
              ratio (/ closest-seconds horizon-seconds)]
          (max 0.0 (min 1.0 (- 1.0 ratio))))))))

(defn anticipation-snapshot
  "Read the canonical source and return a compact snapshot for trace
   propagation: `{:events <vec of summarised events in horizon>
                  :events-loaded? <bool> :path <string>
                  :horizon-days <int>}`. Resilient: returns
   `{:events-loaded? false ...}` if the source is unreadable.

   `horizon-days` controls the lookahead window; default 30 days
   (enough to capture lifecycle-deadlines without flooding the trace
   with far-future entries)."
  ([] (anticipation-snapshot {}))
  ([{:keys [path horizon-days now]
     :or {path default-events-path horizon-days 30}}]
   (let [now (or now (Instant/now))
         horizon (Duration/ofDays (long horizon-days))
         doc (load-anticipations path)]
     (if doc
       {:events-loaded? true
        :path path
        :horizon-days horizon-days
        :events (mapv summarise-event (events-in-horizon doc now horizon))}
       {:events-loaded? false
        :path path
        :horizon-days horizon-days
        :events []}))))
