(ns futon2.aif.precision
  "Adaptive precision tracking for the WM AIF apparatus (R7).

   Maintains per-channel precision Π updated over time. v0.13 combines two
   components, ported from the AIF traditions reconciled across the two
   reference implementations:

     Π = variance-component + need-component (bounded by tau-floor/tau-cap)

     variance-component = 1 / max(rolling-variance, min-variance)
       — standard Bayesian/AIF precision, sourced from prediction-error
         history (v0.12 was variance-component-only).

     need-component = need-scale × max(0, channel-gap-from-preference-range)
       — ants-style need-modulated precision (per
         `futon2/src/ants/aif/affect.clj affect/modulate-precisions`).
         When the observed channel value is outside its preferred range,
         precision rises proportionally to the gap — the agent attends
         more sharply to channels its preferences flag as unhealthy.

   State shape per channel:
     {:precision           <current precision Π>
      :variance-component  <last computed variance-derived precision>
      :need-component      <last computed need-derived precision>
      :error-history       <vec of recent prediction errors, bounded>}

   Cross-call persistence: the WM trace store IS the precision state's
   home — `judge` reads the latest trace record's `:precision-state` to
   continue the rolling window; first call falls back to
   `initial-precision-state` (precision = 1.0, empty history).

   Contract: contributes to R7 (adaptive precision) per
   `futon2/docs/futon-aif-completeness.md`. Cross-maps to F10 (dual-loop
   fitness) at stack scope. v0.13 reference: `futon2/docs/ants-aif-audit.md`
   §'Patterns worth porting' #1 (the cyberants `modulate-precisions`
   pattern is the structural source for the need-component term).

   HONESTY (R18 badge discipline; M-aif-faithfulness B-2c): only the
   variance leg 1/max(var,ε) is canonical AIF precision (inverse expected
   uncertainty of the channel's error stream). The summed-in need term is
   an AFFECT/SALIENCE modulation — attention allocated by preference gap,
   not by uncertainty — wearing precision's name in the v0.13 sum. B-2c
   builds the separation DARK behind `:salience-mode`:

     :summed   (DEFAULT, live) — Π = variance-component + need-component,
               byte-identical to v0.13 (witness:
               test/resources/goldens/r7_default_path_witness.edn).
     :separate (dark)          — Π = bounded variance-component ONLY; the
               need term is reported under the named `:salience` key
               (and retained as `:need-component` for readers), never
               summed into Π.

   The arena resolves the flag via `arena-salience-mode` in
   `scripts/futon2/report/war_machine.clj` (`FUTON_WM_SALIENCE_MODE=separate`
   turns the dark path ON; unset/other = `:summed`). The production flip is
   Joe's, recorded in the M-aif-faithfulness §2.1 verdict ledger — not
   claimed here."
  (:require [futon2.aif.belief :as belief]
            [futon2.aif.preferences :as pref]))

(def default-window-size 20)
(def default-min-variance 0.01)
(def default-initial-precision 1.0)

;; v0.13: ants-style bounds and need-component scaling.
;; tau-floor / tau-cap ported from ants/aif/affect.clj defaults
;; (0.08 / 1.5 there; scaled up here because WM channels report in
;; a different magnitude regime than ant hunger).
(def default-need-scale 5.0)
(def default-precision-floor 0.1)
(def default-precision-cap 200.0)

;; B-2c (M-aif-faithfulness §2.2): how the need term relates to Π.
;; :summed = v0.13 behaviour (need term added into Π) — the live default.
;; :separate = dark path: Π keeps only the canonical variance leg; the need
;; term travels under the named :salience key. Flip is Joe's (§2.1 ledger).
(def default-salience-mode :summed)

(defn- variance
  "Sample variance of a sequence of numbers. Returns 0.0 for sequences
   of length 0 or 1."
  [xs]
  (let [n (count xs)]
    (if (< n 2)
      0.0
      (let [mean (/ (reduce + xs) (double n))
            sq-devs (map #(let [d (- % mean)] (* d d)) xs)]
        (/ (reduce + sq-devs) (double n))))))

(defn initial-precision-state
  "Construct an initial precision state covering every channel in
   `belief/channels-with-likelihood`. Each channel starts with the
   default precision and an empty error history."
  ([] (initial-precision-state belief/channels-with-likelihood))
  ([channels]
   (into {} (for [ch channels]
              [ch {:precision default-initial-precision
                   :error-history []}]))))

(defn- preference-gap
  "Distance from observed value to the channel's preferred range. Returns
   0.0 if the channel isn't in `pref/preferences` or if the value is in
   range; positive distance to the closer boundary otherwise."
  [channel-id observed]
  (if-let [[lo hi] (get pref/preferences channel-id)]
    (let [v (double observed)]
      (cond (< v lo) (- lo v)
            (> v hi) (- v hi)
            :else 0.0))
    0.0))

(defn- need-component-for
  "Per-channel need-modulated precision component (v0.13). Higher when
   the observation is further from the preferred range. Returns 0.0 for
   channels not in `pref/preferences`."
  [channel-id observed need-scale]
  (* (double need-scale) (preference-gap channel-id observed)))

(defn- update-channel-precision
  "Apply one new (error, observed) to a single channel's precision-state.

   :salience-mode :summed (default) — v0.13 behaviour: Π = bounded
   (variance-component + need-component). Output map byte-identical to
   pre-B-2c code.
   :salience-mode :separate (dark) — Π = bounded variance-component only;
   the need term is emitted under the named `:salience` key (kept under
   `:need-component` too, so cross-mode readers see one schema for it)."
  [channel-id channel-state new-error observed
   & {:keys [window-size min-variance need-scale floor cap salience-mode]
      :or {window-size default-window-size
           min-variance default-min-variance
           need-scale default-need-scale
           floor default-precision-floor
           cap default-precision-cap
           salience-mode default-salience-mode}}]
  (let [prev-history (:error-history channel-state [])
        appended (conj prev-history (double new-error))
        bounded (if (> (count appended) window-size)
                  (vec (subvec appended (- (count appended) window-size)))
                  (vec appended))
        v (variance bounded)
        variance-component (/ 1.0 (max v min-variance))
        need-component (need-component-for channel-id observed need-scale)]
    (case salience-mode
      :separate
      (let [precision (-> variance-component (max floor) (min cap))]
        {:precision precision
         :variance-component variance-component
         :need-component need-component
         :salience need-component
         :error-history bounded})
      ;; :summed — the v0.13 live path, byte-identical (witness-tested).
      (let [raw-precision (+ variance-component need-component)
            precision (-> raw-precision (max floor) (min cap))]
        {:precision precision
         :variance-component variance-component
         :need-component need-component
         :error-history bounded}))))

(defn update-precision-state
  "Given previous precision-state (map of channel-id → channel-state) and
   a prediction-errors map (channel-id → error-map carrying `:error` and
   `:observed`), return updated precision-state. v0.13 each channel's
   precision is recomputed as `variance-component + need-component`,
   bounded by precision-floor/cap. B-2c: pass `{:salience-mode :separate}`
   in opts for the dark path (canonical variance-only Π + named
   `:salience`); omitted/`:summed` = v0.13 behaviour, byte-identical.

   Channels in errors-map without a previous state entry are initialised
   from defaults; channels in previous state without a new error are
   passed through unchanged."
  ([prev-state errors] (update-precision-state prev-state errors {}))
  ([prev-state errors opts]
   (let [touched-channels (set (keys errors))
         updated (into {} (for [[ch error-map] errors]
                            (let [channel-state (get prev-state ch
                                                     {:precision default-initial-precision
                                                      :error-history []})
                                  new-err (:error error-map 0.0)
                                  observed (:observed error-map 0.0)]
                              [ch (update-channel-precision ch channel-state
                                                            new-err observed opts)])))
         untouched (into {} (for [[ch s] prev-state
                                  :when (not (contains? touched-channels ch))]
                              [ch s]))]
     (merge untouched updated))))

(defn precision-for
  "Look up the current precision for a channel in a precision-state map.
   Returns the default precision (1.0) if the channel isn't tracked."
  [precision-state channel-id]
  (get-in precision-state [channel-id :precision] default-initial-precision))

(defn salience-for
  "Look up the named salience (need-term) for a channel in a
   precision-state map (B-2c). Present under `:salience` when the state
   was built with `:salience-mode :separate`; falls back to
   `:need-component` (the same quantity's v0.13 home) so callers can read
   one accessor across modes. Returns 0.0 for untracked channels."
  [precision-state channel-id]
  (or (get-in precision-state [channel-id :salience])
      (get-in precision-state [channel-id :need-component])
      0.0))

(defn weighted-error
  "Re-weight a prediction-error map using a channel's history-tracked
   precision instead of the likelihood-derived per-call precision.

   Returns a copy of `error-map` with `:precision` and `:weighted-error`
   replaced by R7 values:
     :precision      ← precision-for(precision-state, channel-id)
     :weighted-error ← :error × :precision

   The original predicted-variance-derived precision is preserved as
   `:per-call-precision` so the trace records both sources."
  [precision-state channel-id error-map]
  (let [adaptive-π (precision-for precision-state channel-id)
        per-call-π (:precision error-map)
        err (:error error-map 0.0)]
    (assoc error-map
           :precision adaptive-π
           :weighted-error (* (double err) adaptive-π)
           :per-call-precision per-call-π)))
