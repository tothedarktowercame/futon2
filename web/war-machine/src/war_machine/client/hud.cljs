(ns war-machine.client.hud
  "Live scoreboard strip at the bottom of the window.

   The leading position indicator is tethered to the Audacity waveform: it
   shows the wall-clock time of the global playback cursor, and (when a
   selection is active) the % progress through that selection.  Falls back
   to the abstract tick counter when no timeline is loaded yet.

   Anchoring discipline (per `~/code/futon5a/data/wm-ui-anchors.edn`,
   M-war-machine-frontend-upgrade1 §2.2.1): interactive HUD segments
   carry a `:title` tooltip from the per-element anchor's rationale, plus
   an `:on-click` that surfaces the anchor's `:anthology-anchor` for
   the operator. v1: console.log the anchor id + concept; v2: route to
   an Arxana entity-view when one lands."
  (:require [war-machine.client.api :as api]
            [war-machine.client.state :as s]
            [war-machine.client.waveform :as wf]))

;; ---------------------------------------------------------------------------
;; Per-anchor anthology targets (sourced from
;; `~/code/futon5a/data/wm-ui-anchors.edn :anthology-anchor`). Each value is
;; passed to `api/open-target-in-emacs!` on click so the operator can jump
;; from the HUD glyph into the substrate that explains it — same machinery
;; graph-node clicks have used since the WM UI shipped.
;; ---------------------------------------------------------------------------

(def ^:private strategic-vocab-target
  {:kind :workspace-file
   :path "futon5a/data/war-machine-strategic-vocabulary.edn"})

;; ---------------------------------------------------------------------------
;; Anchor rationales (per `:μ/modes` in
;; `~/code/futon5a/data/war-machine-strategic-vocabulary.edn` :147-159)
;; ---------------------------------------------------------------------------

(def ^:private mode-rationale
  "Per-mode one-sentence rationale, sourced from
   `war-machine-strategic-vocabulary.edn :μ/modes` block. Hardcoded for v1;
   if the vocab updates, re-sync here. (Reading the EDN dynamically is a
   future substrate-side move, not in scope for the v1 anchor discharge.)"
  {"multiplied"       "inhabitation × evolution > threshold; loop healthy, evidence flowing, sorrys closing"
   "foraging-trapped" "mode stuck on :foraging under π-scholar; high stack-pct, low consulting-pct, cargo accumulating"
   "hermit"           "stack→stack pathological loop; stack work feeding stack work, no external sorrys closing"
   "depositing"       "mode switched to :depositing; consulting-pct rising, sorry-market-interface advancing"
   "stagnant"         "inhabitation × (1−evolution); surfaces used but not improving"
   "dark"             "1−inhabitation; no evidence, no commits, infrastructure rotting"})

(defn- normalise-mode [m]
  (cond
    (keyword? m) (name m)
    (string? m)  (if (clojure.string/starts-with? m ":") (subs m 1) m)
    :else        (str m)))

(defn- mode-tooltip-text
  "Compose the `:title` text for the HUD Mode span. The text combines the
   mode name + its one-sentence rationale + the affordance hint that the
   element is clickable. Falls back to a generic label when the mode is
   unknown (off-vocabulary)."
  [mode-str]
  (let [rationale (get mode-rationale mode-str)]
    (if rationale
      (str mode-str " — " rationale ". Click for the 6-mode taxonomy (see strategic-vocabulary :μ/modes).")
      (str mode-str " — (off-vocabulary; expected one of :multiplied :foraging-trapped :hermit :depositing :stagnant :dark)"))))

(defn- on-click-mode
  "Click handler for the HUD Mode span (wm-ui-anchor:0004). Opens the
   strategic-vocabulary EDN in the operator's Emacs via the same
   `api/open-target-in-emacs!` machinery the graph-node clicks use.
   Logs to the JS console as well so devtools-side debugging still works."
  [mode-str]
  (fn [_evt]
    (js/console.log
     (str "[anchor wm-ui-anchor:0004] mode=" mode-str
          " → opening ~/code/futon5a/data/war-machine-strategic-vocabulary.edn :μ/modes"))
    (api/open-target-in-emacs! strategic-vocab-target)))

;; ---------------------------------------------------------------------------
;; Generic anchor-span helper — used by HUD cluster anchors 0003/0005/0006/0007.
;; ---------------------------------------------------------------------------

(defn- anchor-span
  "Wrap LABEL-TEXT in a tooltip-bearing span for wm-ui-anchor ANCHOR-ID.

   TOOLTIP-TEXT is the hover text (per the anchor's discharge-procedure).
   The on-click handler opens the anchor's anthology target in the
   operator's Emacs via `api/open-target-in-emacs!` — same call the
   graph-node clicks have used since the WM UI shipped. The console.log
   stays so devtools-side debugging still works.

   TESTID is the data-testid (e.g. \"hud-ants\") so Playwright probes can
   target the span cleanly.

   Visual indicator: cursor:pointer + underline-dotted + ' ⓘ' suffix —
   same shape as the anchor-0004 template (M-war-machine-frontend-upgrade1
   §6.5). Per-value rationale (when the tooltip text depends on the live
   value) is computed by the caller and passed in via TOOLTIP-TEXT."
  [anchor-id testid tooltip-text label-text]
  [:span {:data-testid testid
          :title tooltip-text
          :on-click (fn [_evt]
                      (js/console.log
                       (str "[anchor " anchor-id "] " label-text
                            " → opening strategic-vocabulary.edn"))
                      (api/open-target-in-emacs! strategic-vocab-target))
          :style {:cursor "pointer"
                  :text-decoration "underline dotted"}}
   label-text
   " ⓘ"])

(defn- running-count [replay]
  (count (filter (fn [[k v]] (and (not= k :playing?) (map? v))) replay)))

(defn- format-position [data]
  "Composed HUD position indicator. U3 fix (2026-05-24,
   M-war-machine-frontend-upgrade1 §6.14): Joe's QA flagged `Play 22:01`
   as 'random-looking numbers in HH:MM format that seem unrelated to
   anything'. Root cause: `Play <wall-time>` was labelled `Play` (which
   reads as 'play position') but the HH:MM was the wall-clock time of
   the replay cursor's tick — no caller-visible connection to a
   selection, a session, or a duration.

   v1 fix: relabel from `Play <wall>` to `Replay cursor <wall>` so the
   label names what the time IS (the replay cursor's position on the
   timeline above), and from `Tick <n>` to `Replay tick #<n>` for the
   no-cursor fallback. Selection-active branch becomes `Replay cursor
   <wall> (<pct>% through selection)`. No anchor needed (per claude-10's
   batch-1 §U3: rename/relabel/remove or anchor; v1 takes rename)."
  (let [_ @s/tick                         ;; subscribe so HUD re-renders each tick
        ms (wf/playhead-ms data)
        sel (:selection @s/waveform)
        wall (when ms (wf/fmt-time ms))]
    (cond
      (and ms sel (:start-ms sel) (:end-ms sel))
      (let [span (max 1 (- (:end-ms sel) (:start-ms sel)))
            pos  (max 0 (- ms (:start-ms sel)))
            pct  (.toFixed (* 100.0 (min 1.0 (/ (double pos) (double span)))) 0)]
        (str "Replay cursor " wall " (" pct "% through selection)"))

      ms
      (str "Replay cursor " wall)

      :else
      (str "Replay tick #" (.padStart (str @s/tick) 4 " ")))))

(defn hud-line []
  (let [data    @s/data
        aif     @s/aif-data
        replay  @s/replay
        running (when (:playing? replay) (running-count replay))
        total   (running-count replay)
        tri     (:mission-triage data)
        active-m (or (:active tri) 0)
        total-m  (or (:total tri) 0)
        ab       (or (:abandoned-count tri) 0)
        arrows   (get-in data [:loop-health :arrows])
        avg      (if (seq arrows)
                   (/ (reduce + (map (comp double :health) arrows))
                      (double (count arrows)))
                   0.0)
        sorry-n  (count (get-in data [:graph :nodes :sorrys] []))
        ;; Mode preference order:
        ;;   1. data ratom's `:judgement :mode` — the WM's per-tick inferred
        ;;      strategic mode (populated by `futon2.report.war-machine/judge`;
        ;;      arrives as a string like "multiplied" via JSON encoding).
        ;;   2. aif-data ratom's `:scheduler :last-diagnostics :mode` — older
        ;;      path (scheduler diagnostics), kept as fallback for the case
        ;;      where war-machine data isn't loaded yet but the aif-stack
        ;;      poll has fired.
        ;;   3. "?" — last resort if neither source has populated.
        ;;
        ;; Pre-§2.D fix: only (2) was consulted, so the status bar showed
        ;; "Mode ?" whenever the aif-data ratom wasn't fully populated
        ;; (consistently the case post-§2.G with the cross-origin api-base
        ;; rewire). Per M-war-machine-frontend-upgrade1 §2.D, this is the
        ;; single-line wire that closes the visible-bug.
        mode     (or (get-in data [:judgement :mode])
                     (get-in aif [:scheduler :last-diagnostics :mode])
                     "?")]
    (let [mode-str (normalise-mode mode)
          ants-text (str (or running 0) "/" total)
          missions-text (str active-m "/" total-m " (abandoned " ab ")")
          sorrys-text (str sorry-n)
          loop-text (str (.toFixed (* 100.0 avg) 0) "%")
          ;; Per-anchor tooltip text composed inline; per-value rationale
          ;; only where the value belongs to a closed-set lookup (mode).
          ;; The other tooltips are static (anchor-level explanation) since
          ;; the live value doesn't need per-value rationale beyond
          ;; numerator/denominator disambiguation.
          loop-tip (str "Loop " loop-text " — aggregate of the 6 holistic-argument loop arrows "
                       "(work→proof→patterns→coord→self-rep→inference→work). "
                       "Same data as the Loop Health box above. "
                       "Click for the per-arrow breakdown (see strategic-vocabulary :loop-health).")
          ants-tip (str "Ants " ants-text " = active cyberant agents / total registered. "
                       "The WM uses the cyberant model as its strategic template "
                       "(food/pher/home/cargo/mode → strategic equivalents — see "
                       "strategic-vocabulary :harmonisation :cyberants-template). "
                       "Click for the cyberants template (devmap-futon2).")
          missions-tip (str "Missions " missions-text " = active/total (abandoned tracked separately). "
                           "Missions are increments of work that produce prototypes; "
                           "WM uses :mission-health (composite of complete-ratio, blocked-ratio, "
                           "stall-count). Click for the mission portfolio view "
                           "(see strategic-vocabulary :mission-health + foreword).")
          ;; Sorry-registry split is honest here: the UI count comes from
          ;; `[:graph :nodes :sorrys]` (holistic-argument substrate);
          ;; sorrys.edn carries a separate 13-entry registry with :kind +
          ;; :severity per entry. M-war-machine-frontend-upgrade1 §2.H
          ;; tracks the split as flag-only for upgrade-1; unification is a
          ;; future mission.
          sorrys-tip (str "Sorrys " sorrys-text " = open typed obligations from the holistic-argument "
                         "substrate (graph.nodes.sorrys). NOTE: sorrys.edn carries a separate "
                         "registry with 13 entries + per-entry :kind/:severity — registry-split is "
                         "tracked as a known gap (§2.H). Click for the Strategic SORRY Topology "
                         "(see strategic-vocabulary :sorry-count-norm + sorrys.edn).")]
      [:div.hud {:data-testid "hud"}
       (format-position data) "  |  Ants "
       (anchor-span "wm-ui-anchor:0005" "hud-ants" ants-tip ants-text)
       "  |  Missions "
       (anchor-span "wm-ui-anchor:0006" "hud-missions" missions-tip missions-text)
       "  |  Sorrys "
       (anchor-span "wm-ui-anchor:0007" "hud-sorrys" sorrys-tip sorrys-text)
       "  |  Loop "
       (anchor-span "wm-ui-anchor:0003" "hud-loop" loop-tip loop-text)
       "  |  Mode "
       ;; Anchored HUD segment per wm-ui-anchor:0004 (the template-setter).
       ;; Kept as a separate call (not via anchor-span) because the tooltip
       ;; text depends on the per-mode rationale lookup, which the generic
       ;; anchor-span doesn't carry.
       [:span {:data-testid "hud-mode"
               :title (mode-tooltip-text mode-str)
               :on-click (on-click-mode mode-str)
               :style {:cursor "pointer"
                       :text-decoration "underline dotted"}}
        mode-str
        " ⓘ"]])))
