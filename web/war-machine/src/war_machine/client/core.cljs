(ns war-machine.client.core
  "Root Reagent component + init. Mirrors webarxana.client.core."
  (:require [reagent.dom.client :as rdc]
            [clojure.string :as str]
            [war-machine.client.labels :as labels]
            [war-machine.client.state :as s]
            [war-machine.client.api :as api]
            [war-machine.client.tick :as tick]
            [war-machine.client.graph :as graph]
            [war-machine.client.hud :as hud]
            [war-machine.client.aif-join :as aif]
            [war-machine.client.waveform :as waveform]))

(defn- toolbar []
  (let [vmode @s/view-mode
        playing? (:playing? @s/replay)]
    [:div.toolbar
     [:span.title "War Machine — Strategic Synthesis"]
     [waveform/waveform-component]
     [:button {:on-click #(api/load!)
               :data-testid "refresh"}
      "Refresh"]
     ;; 0017 + 0018 (Joe directive 2026-05-24): sorrys + invariants
     ;; views are deprecated as WM UI surfaces — operator vote was to
     ;; deprecate them in favour of the richer VSATARCS globe pages
     ;; (sorrys) and leaf-invariants.md + claude-4's R-criteria apparatus
     ;; (invariants). Removed from the view-toggle cycle below; renderers
     ;; in `hex.cljs` + `graph.cljs` stay for defensive completeness in
     ;; case some other path sets `:view-mode` to those keywords. New
     ;; cycle order: stack → self-watch → aif-stack → missions → patterns
     ;; → stack (5 modes; was 7). Fallback case in `case` defaults to
     ;; :stack so a stale :sorrys / :invariants value cycles forward
     ;; cleanly. See M-war-machine-frontend-upgrade1 §6.17.
     [:button.toggle {:on-click (fn []
                                  (let [nxt (case vmode
                                              :stack :self-watch
                                              :self-watch :aif-stack
                                              :aif-stack :missions
                                              :missions :patterns
                                              :patterns :stack
                                              ;; Stale-state fallback —
                                              ;; if :view-mode is somehow
                                              ;; :sorrys or :invariants
                                              ;; (e.g. via a saved
                                              ;; setting from before
                                              ;; deprecation), cycle
                                              ;; forward to :stack.
                                              :stack)]
                                    (reset! s/view-mode nxt)))
                      :data-testid "view-toggle"}
      (str "View: " (name vmode))]
     [:button.toggle {:on-click (fn []
                                  (swap! s/days-window
                                         (fn [d] (case d 14 90 90 14 14)))
                                  ;; Trigger a re-fetch so the data refreshes
                                  (api/load!))
                      :data-testid "days-toggle"
                      :title "Toggle the time window for commit/activity scan"}
     (str "Window: " @s/days-window "d")]
     [:button.toggle {:on-click #(swap! s/replay update :playing? not)
                      :data-testid "play-toggle"}
      (if playing? "Pause" "Play")]
     [:button.toggle {:on-click (fn []
                                  (swap! s/replay assoc :playing? false)
                                  (reset! s/hotspot {})
                                  (tick/reset-playback! @s/data))
                      :data-testid "stop"}
      "Stop"]]))

(defn- legend []
  "Per-view-mode colour-key. Renders inside a bordered card with grid-aligned
   swatches and labels."
  (let [vmode @s/view-mode
        swatch (fn [color label]
                 [:div.row
                  [:div.swatch {:style {:background color}}]
                  [:span label]])
        outline-swatch (fn [border-color label]
                         [:div.row
                          [:div.swatch.outline {:style {:border-color border-color}}]
                          [:span label]])
        section (fn [label] [:div.legend-section-label label])]
    [:div.legend-card
     [:h3 "Legend"]
     (case vmode
       :missions
       [:div
        (section "Phase")
        (swatch "#ec4899" "identify")
        (swatch "#a78bfa" "map")
        (swatch "#3b82f6" "derive / in-progress")
        (swatch "#fb923c" "argue")
        (swatch "#eab308" "verify")
        (swatch "#10b981" "instantiate")
        (swatch "#78350f" "document")
        (swatch "#1e293b" "complete")
        (swatch "#ffffff" "ready")
        (swatch "#d1d5db" "unknown / no phase")
        (section "Border")
        (outline-swatch "#dc2626" "blocked")]

       :aif-stack
       [:div
        (section "Spine role")
        (swatch "#4a90e2" "thesis / ur-claim")
        (swatch "#22c55e" "pillar / cycle")
        (swatch "#eab308" "substrate")
        (swatch "#ef4444" "frame (preference)")
        (section "Conflicts")
        (swatch "#b91c1c" "load-bearing (≥7)")
        (swatch "#d97706" "secondary (4-6)")
        (swatch "#3b82f6" "info (≤3)")
        (section "Bite edges (C → S)")
        (swatch "#dc2626" "empirical bite (sessions hit)")
        (swatch "#9ca3af" "logical bite (no session hit)")]

       :sorrys
       [:div
        (section "SORRY severity")
        (swatch "#b91c1c" "critical")
        (swatch "#dc2626" "high")
        (swatch "#d97706" "warning")
        (swatch "#ea580c" "medium")
        (swatch "#3b82f6" "info")
        (swatch "#60a5fa" "low")]

       :invariants
       [:div
        (section "Tier")
        (swatch "#15803d" "operational (always-on)")
        (swatch "#a78bfa" "candidate (design pressure)")]

       :patterns
       [:div
        (section "Hue (pattern × activation balance)")
        (swatch "hsl(240, 80%, 50%)" "blue — pattern-heavy, low activation")
        (swatch "hsl(120, 80%, 50%)" "green — balanced (mixes)")
        (swatch "hsl(60, 80%, 50%)"  "yellow — activation-heavy, few patterns")
        (section "Saturation / fade")
        [:div {:style {:font-size "11px" :color "#64748b"}}
         "= pattern count (more patterns → more vivid)"]
        (section "Numeric badge")
        [:div {:style {:font-size "11px" :color "#64748b"}}
         "= activations / pattern-count (last 14d)"]
        (section "Layout")
        [:div {:style {:font-size "11px" :color "#64748b"}}
         "Left→right, top→bottom: balance ascending (blue pole → green/yellow pole)"]]

       :self-watch
       [:div
        (section "Severity")
        (swatch "#b91c1c" "critical / stop-the-line")
        (swatch "#d97706" "warning / high pressure")
        (swatch "#2563eb" "recovery / informational")
        (section "Intent")
        [:div {:style {:font-size "11px" :color (:text-dim "#64748b")}}
         "Translate boot/watchdog/metabolic signals into concrete maintenance work."]]

       :stack
       [:div
        (section "Workstreams")
        (swatch "#4a90e2" "stack")
        (swatch "#22c55e" "mathematics")
        (swatch "#eab308" "portfolio")
        (swatch "#ef4444" "consulting")]
       nil)]))

(declare aif-mode-tile)
(declare aif-efe-tile)
(declare aif-prediction-error-tile)
(declare aif-observation-tile)
(declare next-move-tile)
(declare warnings-tile)

(defn- legend-panel []
  "The legend rendered as a free-floating tile to the LEFT of the hexagons,
  so it doesn't crowd the right-side stats panel.  The Recommended Next
  Move tile sits directly underneath so the strategic recommendation is
  paired with the colour key it explains.  Only directly computed warnings
  are surfaced, and only when one is currently firing."
  [:div.legend-panel
   [legend]
   ;; aif-mode-tile pulled 2026-04-27 per E-war-machine-qa: failed the
   ;; "fresh-Claude-session" test — gives a stance without the evidence
   ;; or action affordances that would make it operator-actionable.
   ;; Function definition retained below; reinstate when J3 inspect-
   ;; trace lands or when an action-shaped redesign is ready.
   [aif-efe-tile]
   [aif-prediction-error-tile]
   [aif-observation-tile]
   [next-move-tile]
   [warnings-tile]])

(defn- aif-prediction-error-tile
  "Per-channel prediction error — TI-5.
   Reads :scheduler/last-prediction-errors, specifically the :raw
   sub-map (observation - μ.sens). Channels are ranked by absolute
   error and the top N (= 6) are shown with sign-coloured bars.

   Sparkline-over-history (the original TI-5 spec) requires
   accumulated snapshots; this v1 surfaces the latest tick's per-channel
   error as a stationary chart. History becomes available as the
   notebook snapshot log grows."
  []
  (let [aif @s/aif-data
        sched (:scheduler aif)
        errs (get-in sched [:last-prediction-errors :raw])
        free-energy (get-in sched [:last-prediction-errors :free-energy])]
    (when (and errs (seq errs))
      (let [;; Filter to channels with abs(err) > 1e-3 (the rest are noise floor).
            top (->> errs
                     (filter (fn [[_ v]] (and (number? v) (> (js/Math.abs (double v)) 1e-3))))
                     (sort-by (fn [[_ v]] (- (js/Math.abs (double v)))))
                     (take 6))
            max-abs (or (some-> top first second double js/Math.abs) 1.0)
            row (fn [[k v]]
                  (let [pct (max 1 (* 100 (/ (js/Math.abs (double v)) max-abs)))
                        positive? (pos? v)]
                    [:div.aif-pe-row {:key (name k)}
                     [:span.aif-pe-label (name k)]
                     [:span.aif-pe-value (.toFixed (double v) 3)]
                     [:div.aif-pe-bar
                      [:div {:class (if positive? "aif-pe-bar-fill-pos" "aif-pe-bar-fill-neg")
                             :style {:width (str pct "%")}}]]]))]
        [:div.aif-pe-tile {:data-testid "aif-prediction-error"
                           :title (str "Per-channel prediction error (observation − μ.sens). "
                                       "Positive = observation higher than predicted. "
                                       "Negative = observation lower than predicted.")}
         [:div.aif-pe-header
          [:span "Prediction error"]
          (when (number? free-energy)
            [:span " · F=" (.toFixed (double free-energy) 3)])
          [:span " · top " (count top)]]
         (if (empty? top)
           [:div.aif-pe-empty "(all channels within noise floor)"]
           (into [:div.aif-pe-rows] (map row top)))]))))

(defn- aif-efe-tile
  "Expected free-energy decomposition for the chosen action — TI-4.
   Reads :scheduler/last-efe-terms (from
   futon3c.portfolio.policy/expected-free-energy). 4-term EFE:
   pragmatic, epistemic, upvote, effort. Not the 2-term G in the
   strategic vocabulary (see TI-6).

   Bar magnitudes are normalised to the maximum absolute term in the
   current snapshot; sign indicated by colour (green = value-bearing
   contribution, red = cost contribution)."
  []
  (let [aif @s/aif-data
        sched (:scheduler aif)
        terms (:last-efe-terms sched)
        action (:last-action sched)
        free-energy (get-in sched [:last-diagnostics :free-energy])]
    (when (and terms (seq terms))
      (let [max-abs (->> terms vals (map (comp js/Math.abs double)) (apply max 0.001))
            ordered [[:pragmatic "value (goal progress)"]
                     [:epistemic "value (info gain)"]
                     [:upvote    "value (collective desire)"]
                     [:effort    "cost (mana)"]]
            row (fn [[k label]]
                  (let [v (get terms k 0)
                        pct (max 1 (* 100 (/ (js/Math.abs (double v)) max-abs)))
                        cost? (= k :effort)]
                    [:div.aif-efe-row {:key (name k)}
                     [:span.aif-efe-label (name k)]
                     [:span.aif-efe-value (.toFixed (double v) 3)]
                     [:div.aif-efe-bar
                      [:div {:class (if cost? "aif-efe-bar-fill-cost" "aif-efe-bar-fill-value")
                             :style {:width (str pct "%")}}]]
                     [:span.aif-efe-note label]]))]
        [:div.aif-efe-tile {:data-testid "aif-efe"
                            :title "Expected free-energy decomposition for the chosen action."}
         [:div.aif-efe-header
          [:span "EFE"]
          (when action [:span " · " (name action)])
          (when (number? free-energy)
            [:span " · G=" (.toFixed (double free-energy) 3)])]
         (into [:div.aif-efe-rows] (map row ordered))]))))

(defn- aif-observation-tile
  "16-channel portfolio-AIF observation panel — TI-2.
   Reads :scheduler/last-observation from the AIF stack response.
   Each channel renders as: name · value (3dp) · proportional bar.

   No preferred-band overlay yet — the WM vocabulary's :C/preferred
   describes the strategic surface, not the portfolio surface this
   reads (TI-6 architectural finding). Bands land when TI-6 resolves
   and a portfolio-surface preferences block exists.

   Channels rendered in alphabetical order for stable diffability."
  []
  (let [aif @s/aif-data
        sched (:scheduler aif)
        obs (:last-observation sched)]
    (when (and obs (seq obs))
      (let [rows (->> obs
                      (sort-by (fn [[k _]] (name k)))
                      (mapv (fn [[k v]]
                              (let [pct (if (number? v) (max 0 (min 100 (* 100 v))) 0)]
                                [:div.aif-obs-row {:key (name k)}
                                 [:span.aif-obs-label (name k)]
                                 [:span.aif-obs-value
                                  (if (number? v) (.toFixed v 3) "?")]
                                 [:div.aif-obs-bar
                                  [:div.aif-obs-bar-fill
                                   {:style {:width (str pct "%")}}]]]))))]
        [:details.aif-obs-tile {:data-testid "aif-observation"
                                :open false
                                :title (str "Portfolio AIF 16-channel observation. "
                                            "TI-6 architectural finding: this is the portfolio surface "
                                            "(futon3c.portfolio.observe), not the strategic surface "
                                            "described in war-machine-terminal-vocabulary.edn.")}
         [:summary.aif-obs-summary
          [:span.aif-obs-summary-label "AIF observation"]
          [:span.aif-obs-summary-count (str " · " (count obs) " channels")]]
         (into [:div.aif-obs-rows] rows)]))))

(defn- aif-mode-tile
  "Compact AIF mode strip — first WM-I3 trace landing
   (M-war-machine-tuning § TI-3, Checkpoint 4 working assumption).
   Reads :scheduler/last-diagnostics from the AIF stack response.

   Source: futon3c.portfolio.core/portfolio-step! diagnostics
   ({:mode :urgency :tau :free-energy}). Mode vocabulary is the
   *portfolio-inference* mode (:BUILD / :MAINTAIN / :CONSOLIDATE),
   NOT the strategic mode (:multiplied / :hermit / etc.) declared
   in war-machine-terminal-vocabulary.edn. TI-6 records the
   architectural finding behind this distinction; this tile is
   honest about which surface it reads."
  []
  (let [aif @s/aif-data
        sched (:scheduler aif)
        diag (:last-diagnostics sched)
        tick-at (:last-tick-at sched)
        period-s (:period-seconds sched)
        ;; Freshness threshold derives from period (single source of truth).
        live-ms (when (number? period-s) (* period-s 1500))
        tick-ms (when tick-at (.getTime (js/Date. tick-at)))
        age-ms (when tick-ms (- (.now js/Date) tick-ms))
        fresh? (and live-ms age-ms (< age-ms live-ms))
        fmt-duration (fn [ms]
                       (let [m (/ ms 60000)]
                         (cond
                           (< m 1)  "<1 min"
                           (< m 60) (str (js/Math.floor m) " min")
                           (< m (* 60 24)) (str (js/Math.floor (/ m 60)) " hr")
                           :else (str (js/Math.floor (/ m (* 60 24)))
                                      (if (< m (* 60 48)) " day" " days")))))
        age-text (when age-ms (fmt-duration age-ms))
        period-text (when (number? period-s)
                      (fmt-duration (* period-s 1000)))
        mode (:mode diag)
        urgency (:urgency diag)
        tau (:tau diag)
        fe (:free-energy diag)]
    (when diag
      [:div.aif-mode-tile {:data-testid "aif-mode"
                           :title (str "Portfolio AIF mode (futon3c.portfolio.core/portfolio-step!). "
                                       "TI-6 in M-war-machine-tuning records that this is one of two "
                                       "AIF surfaces; the strategic surface in "
                                       "war-machine-terminal-vocabulary.edn is a sibling reading.")}
       [:div.aif-mode-header
        [:span.aif-mode-label "AIF mode"]
        (when (some? fresh?)
          [:span {:class (if fresh? "aif-mode-fresh" "aif-mode-aging")}
           (str " · " (if fresh? "live" "aging")
                (when age-text (str " · ticked " age-text " ago"))
                (when period-text (str " · period " period-text)))])]
       [:div.aif-mode-value
        (cond
          (keyword? mode) (name mode)
          (string? mode) mode
          :else "?")]
       (let [mode-key (cond
                        (keyword? mode) mode
                        (string? mode) (keyword mode)
                        :else nil)]
         [:<>
          [:div.aif-mode-caption
           (case mode-key
             :BUILD       "open new fronts — gaps, stalls drive expansion"
             :MAINTAIN    "hold current direction — coverage is stable"
             :CONSOLIDATE "review accumulated work — spinoff pressure / review age"
             nil)]
          [:div.aif-mode-others
           (case mode-key
             :BUILD       "(also: MAINTAIN · CONSOLIDATE)"
             :MAINTAIN    "(also: BUILD · CONSOLIDATE)"
             :CONSOLIDATE "(also: BUILD · MAINTAIN)"
             nil)]])
       [:div.aif-mode-fields
        (when (number? urgency)
          [:span.aif-mode-field "urgency=" (.toFixed urgency 2)])
        (when (number? tau)
          [:span.aif-mode-field " · τ=" (.toFixed tau 2)])
        (when (number? fe)
          [:span.aif-mode-field " · G=" (.toFixed fe 3)])]])))

(defn- live-next-move-tile
  "Render the live recommendation (E-wm-live-recommendation) — top of
   judgement.ranked-actions, projected by the stack-generator into a
   next-move-shape.  Reads :reading :next-move-live from the AIF+ payload."
  [live]
  (let [rationale       (:rationale live)
        specifically    (:specifically live)
        g-total         (:G-total live)
        age-s           (:age-seconds live)
        stale?          (:stale? live)
        mode            (:mode live)
        period-s        (or (:scheduler-period-seconds live) 300)
        age-label       (when (number? age-s)
                          (str (long (/ age-s 60)) "m ago"))
        fresh-class     (if stale? "wm-live-badge-aging" "wm-live-badge-live")
        alts            (:alternatives-considered live)
        priorities      (:priorities live)]
    [:div.next-move-card.next-move-card-live {:data-testid "next-move-live"}
     [:h3 "Recommended Next Move "
      [:span.wm-live-badge {:class fresh-class
                            :style {:font-size "0.7em"
                                    :padding "2px 6px"
                                    :border-radius "10px"
                                    :margin-left "8px"
                                    :background (if stale? "#fbbf24" "#10b981")
                                    :color "white"}}
       (if stale? "AGING" "LIVE")
       (when age-label (str " · " age-label))]]
     [:div.next-move-source
      {:style {:font-size "0.8em" :color "#6b7280" :margin-bottom "4px"}}
      "Source: judgement.ranked-actions (recomputed every " period-s "s; "
      "see E-wm-live-recommendation.md)"]
     ;; E-wm-live-recommendation v1.1: tied-bucket display.
     ;; Surface multiple tied actions when WM EFE has near-equal G-totals
     ;; (e.g. 5 sibling sorries) instead of falsely promoting rank-1 as
     ;; if it were a real preference.
     (let [tied-actions (:tied-actions live)
           tied-count   (or (:tied-count live) 1)
           anam-values  (->> tied-actions
                             (keep :anamnesis-concentration)
                             (map double))
           distinct-anam-levels (count (distinct anam-values))
           tie-broken? (and (seq anam-values) (> distinct-anam-levels 1))
           top-entry (first tied-actions)
           top-anam (some-> top-entry :anamnesis-concentration double)]
       (cond
         (> tied-count 1)
         [:div.next-move-tied-bucket
          {:data-testid "next-move-live-tied-bucket"
           :style {:padding "6px 0"}}
          [:div.next-move-tied-header
           {:style {:margin-bottom "4px"}}
           "→ " [:strong tied-count " G-tied options"]
           (when (number? g-total)
             [:span {:style {:font-size "0.85em" :color "#6b7280" :margin-left "8px"}}
              "G=" (.toFixed g-total 3) " (each)"])
           (cond
             tie-broken?
             [:span {:style {:font-size "0.75em" :color "#6b7280" :margin-left "8px"
                             :font-style "italic"}}
              "broken by ΔT-anamnesis-concentration ("
              distinct-anam-levels " distinct levels; top = "
              (.toFixed top-anam 2) ")"]
             :else
             [:span {:style {:font-size "0.75em" :color "#9ca3af" :margin-left "8px"
                             :font-style "italic"}}
              "WM has no clear preference among these — pick any"])]
          (when tie-broken?
            [:div.next-move-tied-recommend
             {:style {:padding "4px 0 6px 0" :font-size "0.92em"}
              :data-testid "next-move-tied-top-recommend"}
             "Recommended: " [:strong (:specifically top-entry)]
             [:span {:style {:font-size "0.85em" :color "#6b7280" :margin-left "8px"
                             :font-family "monospace"}}
              "anamnesis Δ = " (.toFixed top-anam 2)]])
          [:ul.next-move-tied-list
           {:style {:padding-left "20px" :margin "4px 0"}}
           (for [entry tied-actions]
             ^{:key (str (:rank entry))}
             [:li {:style {:padding "2px 0"}
                   :data-testid (str "next-move-tied-entry-" (:rank entry))}
              (:specifically entry)
              (let [anam (:anamnesis-concentration entry)
                    g-risk (:G-risk entry)
                    g-amb  (:G-ambiguity entry)
                    g-info (:G-info entry)
                    g-surv (:G-survival entry)
                    g-tot  (:G-total entry)
                    has-trace? (or (number? anam)
                                   (some number? [g-risk g-amb g-info g-surv]))]
                (when has-trace?
                  [:details.next-move-trace
                   {:style {:margin "4px 0 4px 0" :font-size "0.85em" :color "#4b5563"}
                    :data-testid (str "next-move-trace-" (:rank entry))}
                   [:summary {:style {:cursor "pointer"}}
                    "trace"
                    (when (number? anam)
                      [:span {:style {:margin-left "8px" :font-family "monospace"
                                      :color (if (pos? anam) "#059669" "#9ca3af")}}
                       "anamnesis Δ = " (.toFixed (double anam) 2)])]
                   [:div {:style {:padding "4px 0 4px 16px" :font-family "monospace"}}
                    (when (number? g-tot)
                      [:div "G-total = " (.toFixed (double g-tot) 4)])
                    (when (some number? [g-risk g-amb g-info g-surv])
                      [:div
                       (when (number? g-risk)    [:div "  G-risk      = " (.toFixed (double g-risk) 4)])
                       (when (number? g-amb)     [:div "  G-ambiguity = " (.toFixed (double g-amb) 4)])
                       (when (number? g-info)    [:div "  G-info      = " (.toFixed (double g-info) 4)])
                       (when (number? g-surv)    [:div "  G-survival  = " (.toFixed (double g-surv) 4)])])
                    (when (number? anam)
                      [:div {:style {:margin-top "4px" :color "#6b7280" :font-family "inherit"}}
                       "tie-breaker (ΔT-anamnesis-concentration over related missions) = "
                       (.toFixed (double anam) 4)
                       (when (zero? anam) " — no incident open-sorry edges in current substrate")])]]))])]]

         :else
         [:div.next-move-target {:data-testid "next-move-live-target"}
          "→ " specifically
          (when (number? g-total)
            [:span {:style {:font-size "0.85em" :color "#6b7280" :margin-left "8px"}}
             "G=" (.toFixed g-total 3)])]))
     (when mode
       [:div.next-move-mode
        {:style {:font-size "0.85em" :color "#6b7280"}}
        "WM mode: " (if (keyword? mode) (name mode) (str mode))])
     (when rationale
       [:div.next-move-rationale-inline
        {:style {:margin-top "8px" :font-style "italic"}}
        rationale])
     (when (seq alts)
       [:details.next-move-alts
        [:summary (str "Alternatives considered (" (count alts) ")")]
        [:ul
         (for [[k v] alts]
           ^{:key (str k)}
           [:li
            [:strong (if (keyword? k) (name k) (str k))]
            ": " v])]])
     (when (seq priorities)
       [:details.next-move-priorities
        [:summary (str "Priorities driving this rank (" (count priorities) ")")]
        [:ul
         (for [p priorities]
           ^{:key (str (:id p) "-" (:rank p))}
           [:li (or (:summary p) (pr-str p))])]])]))

(defn- cached-prose-warning-banner
  "Render an amber stripe when the stack-generator has attached a
   :freshness-warning to the cached :reading :next-move
   (E-wm-live-recommendation)."
  [warning]
  (when warning
    (let [age-d (:age-days warning)
          mtime (:mtime warning)]
      [:div.next-move-freshness-warning
       {:style {:background "#fef3c7"
                :border "1px solid #f59e0b"
                :color "#78350f"
                :padding "8px 12px"
                :margin-bottom "8px"
                :border-radius "4px"
                :font-size "0.85em"}}
       [:strong "⚠ Cached prior · "]
       (when (number? age-d) (str age-d " days old"))
       (when mtime (str " (mtime " mtime ")"))
       [:br]
       "This is a static prior, not a live recommendation.  See "
       [:code ":reading :next-move-live"]
       " for the per-tick WM-judgement-derived recommendation."])))

(defn- cached-next-move-tile
  "Render the cached :reading :next-move prose (the static THE-STACK.aif.edn
   prior) — used either standalone (when live surface is absent) or as a
   collapsed `<details>` under the live tile.

   Clicking 'Show in graph' selects the conflict node it dis-bites so the
   detail panel populates."
  [aif nm]
  (when nm
      (let [close       (or (:close nm) "?")           ; e.g. "S6"
            specifically (:specifically nm)
            agenda       (:agenda nm)
            successor    (:successor agenda)
            current      (or successor agenda)
            agenda-state (:status current)
            witness      (:witness agenda)
            effect       (:effect-witness agenda)
            rationale    (:rationale nm)
            feeding      (:feeding-input nm)
            alts         (:alternatives-considered nm)
            successor-status (:status successor)
            successor-state (cond
                              (keyword? successor-status) successor-status
                              (string? successor-status) (keyword successor-status)
                              :else nil)
            successor-missing (:missing-fields successor)
            witness-at   (:at witness)
            witness-inst (when witness-at (js/Date. witness-at))
            witness-ms   (when witness-inst (.getTime witness-inst))
            witness-age-min (when witness-ms
                              (js/Math.floor (/ (- (.now js/Date) witness-ms) 60000)))
            witness-hhmm (when witness-inst
                           (.toLocaleTimeString witness-inst "en-GB"
                                                #js {:hour "2-digit"
                                                     :minute "2-digit"
                                                     :hour12 false}))
            ;; Live-vs-aging threshold derives from the scheduler period
            ;; (single source of truth — see M-stack-stereolithography
            ;; § Checkpoint 5). At daily that is ~36 h before "aging";
            ;; at hourly it's ~90 min. Fall back to 90 min if the server
            ;; response carries no scheduler block.
            scheduler-period-s (get-in aif [:scheduler :period-seconds])
            live-threshold-min (if (number? scheduler-period-s)
                                 (long (/ (* scheduler-period-s 1.5) 60))
                                 90)
            witness-fresh? (when (number? witness-age-min)
                             (< witness-age-min live-threshold-min))
            ;; jump to the C1 conflict (the one this move dis-bites)
            conflicts (:stack-conflicts aif)
            primary   (first conflicts)
            ctx       (aif/join-context)
            eb        (when primary
                        (get-in ctx [:empirical-bites (:id primary)]))
            display-close (or (:close current) close)
            display-specifically (or (:specifically current) specifically)]
        [:div.next-move-card {:data-testid "next-move"}
         [:h3 "Recommended Next Move"]
         ;; Anchor 0009 (Recommended Next Move 'Close 🐜N' dead-weight).
         ;; The card was rich-with-detail but operator-illegible at the
         ;; surface — 🐜N glyph + 'Close' verb both opaque without the
         ;; THE-STACK.aif.edn substrate knowledge. v1 discharge wraps
         ;; the target line in an anchored span with a tooltip that
         ;; expands the 🐜N + 'Close' semantics, plus a source subhead.
         ;; The richer detail below the target stays as-is — it was
         ;; already there, just buried below an inscrutable headline.
         ;; See M-war-machine-frontend-upgrade1 §6.10.
         [:div.next-move-source
          {:style {:font-size "0.8em" :color "#6b7280" :margin-bottom "4px"}}
          "Source: AIF reading layer (see ~/code/futon5a/holes/stories/THE-STACK.aif.edn for the 🐜N stack-spine taxonomy)"]
         (let [display-id (labels/stack-spine-display-id display-close)
               hover-label (labels/stack-spine-hover-label display-close)
               tooltip (str display-id " = stack spine node " display-close
                           " — " hover-label
                           ". 'Close' = resolve the closure agenda for this node "
                           "(per the AIF reading layer's per-tick recommendation; "
                           "see :reading :next-move in the live war-machine response). "
                           "Click for the anchor reference + substrate pointers.")]
           [:div.next-move-target
            "→ Close "
            [:span {:data-testid "next-move-target"
                    :title tooltip
                    :on-click (fn [_]
                                (js/console.log
                                 (str "[anchor wm-ui-anchor:0009] close=" display-close
                                      " (" display-id " — " hover-label ")"
                                      " → opening ~/code/futon5a/holes/stories/THE-STACK.aif.edn"))
                                (api/open-target-in-emacs!
                                 {:kind :workspace-file
                                  :path "futon5a/holes/stories/THE-STACK.aif.edn"}))
                    :style {:cursor "pointer"
                            :text-decoration "underline dotted"}}
             display-id " ⓘ"]])
         (when agenda
           [:div.next-move-phase.next-move-phase-complete
            [:strong "Completed: "]
            (name (:status agenda))
            " "
            (:id agenda)
            (when witness
              (str " via "
                   (or (:evidence-id witness) (:run-id (:run witness)) "witness")))])
         (when successor
           [:div.next-move-phase
            {:class (if (= :underspecified successor-state)
                      "next-move-phase next-move-phase-warn"
                      "next-move-phase")}
            [:strong "Next: "]
            (name agenda-state)
            " "
            (:id successor)])
         (when (and (not successor) current)
           [:div.next-move-agenda
            [:strong "Agenda: "]
            (name agenda-state)])
         (when display-specifically
           [:div.next-move-specifically (labels/stack-text->display display-specifically)])
         (when successor
           [:div.next-move-rollover
            (str "Rolled forward from " (:id agenda) " after effect witness")])
         (when (= :underspecified successor-state)
           [:div.next-move-underspecified
            (or (:note successor)
                "The successor is documented, but it is not yet an executable closure agenda.")])
         (when (seq successor-missing)
           [:div.next-move-missing
            [:strong "Still missing: "]
            (->> successor-missing
                 (map name)
                 (str/join ", "))])
         (when-let [run (:run witness)]
           [:div.next-move-witness
            (str "Witness: step "
                 (:step-before run "?")
                 " → "
                 (:step-after run "?")
                 (when witness-hhmm
                   (str " · " witness-hhmm))
                 (when (number? witness-age-min)
                   (str " · "
                        (if witness-fresh? "live" "aging")
                        " "
                        witness-age-min
                        "m ago")))])
         (when effect
           [:div.next-move-effect
            (str "Effect: "
                 (name (:counter-id effect))
                 " "
                 (:before effect "?")
                 " → "
                 (:after effect "?"))])
         (when-let [ci (:candidate-invariant successor)]
           [:div.next-move-successor
            (str "Successor invariant: " (:title ci))])
         (when (and primary eb)
           [:div.next-move-empirical
            (str "Empirical pressure: " (or (:id primary) "C1") " bites "
                 (:hit eb) "/" (:total eb) " spine targets in window")])
         (when rationale
           [:details.next-move-rationale
            [:summary "Why this move"]
            [:pre (labels/stack-text->display rationale)]])
         (when feeding
           [:details.next-move-feeding
            [:summary "Feeding input"]
            [:pre (labels/stack-text->display feeding)]])
         (when (seq alts)
           [:details.next-move-alts
            [:summary (str "Alternatives considered (" (count alts) ")")]
            [:ul
             (for [[k v] alts]
               ^{:key (str k)}
               [:li
                [:strong (if (keyword? k) (name k) (str k))]
                ": " (labels/stack-text->display v)])]])
         (when primary
           (let [pid     (or (:id primary) "C1")
                 sel     @s/selected
                 shown?  (and (= :load-bearing-conflict (:role sel))
                              (= pid (:id sel)))]
             [:button.next-move-jump
              {:on-click (fn []
                           (if shown?
                             (reset! s/selected nil)
                             (do (reset! s/view-mode :aif-stack)
                                 (reset! s/selected
                                         {:role :load-bearing-conflict
                                          :id pid
                                          :weight (:weight primary)
                                          :tooltip (:content primary)
                                          :bites (:bites primary)
                                          ;; Carry :coalesces-from so the
                                          ;; Open in Emacs button can derive
                                          ;; a leaf name (one of this
                                          ;; conflict's source leaves).
                                          :coalesces-from (:coalesces-from primary)}))))
              :data-testid "next-move-jump"}
              (str (if shown? "Hide " "Show ") pid " detail")]))])))

(defn- next-move-tile
  "Dispatch to the live recommendation tile when :reading :next-move-live is
   present (E-wm-live-recommendation), or fall back to the cached prose tile
   with a freshness-warning banner.  When both are present, the live tile is
   the headline and the cached prior collapses into a <details> for context."
  []
  (let [aif     @s/aif-data
        live    (get-in aif [:reading :next-move-live])
        nm      (get-in aif [:reading :next-move])
        warning (:freshness-warning nm)]
    (cond
      live
      [:div
       (live-next-move-tile live)
       (when nm
         [:details {:style {:margin-top "12px" :font-size "0.85em"}}
          [:summary
           "Cached prior (was the surface before E-wm-live-recommendation"
           (when (number? (:age-days warning))
             (str "; " (:age-days warning) "d old"))
           ")"]
          (cached-prose-warning-banner warning)
          (cached-next-move-tile aif nm)])]

      nm
      [:div
       (cached-prose-warning-banner warning)
       (cached-next-move-tile aif nm)])))

(defn- tick-title [tick-id]
  (case (if (keyword? tick-id) tick-id (keyword (str tick-id)))
    :hermit-warning "hermit!"
    :hobby-warning "hobby!"
    :foraging-warning "foraging!"
    :cargo-warning "cargo!"
    (str tick-id)))

;; Anchor 0008 (cargo CRITICAL warning aphorism — Batch 1).
;;
;; Pre-anchor: the Warnings tile rendered just the aphoristic :fires text
;; ('Cargo undelivered. The prospectus converts demonstration to invoice.')
;; with no concrete observed state, no actionable next steps, no link to
;; the JSDQ market-interface vocabulary the aphorism's terms come from.
;;
;; Anchor 0008's discharge calls for: (a) replace aphorism-as-primary
;; with plain-language statement of the concrete constraint, (b) per-term
;; cargo/prospectus/invoice click-through to JSDQ market-interface
;; vocabulary, (c) when warning fires, link click-through to specific
;; actor actions. v1 below ships (a) + (c); per-term hover (b) is
;; deferred to v2 because it requires highlighting specific terms within
;; the aphorism prose.
;;
;; See M-war-machine-frontend-upgrade1 §6.9 +
;; ~/code/futon5a/data/wm-ui-anchors.edn :anchors[0008].
(def ^:private tick-actions
  "Per-tick hardcoded actionable hints for the operator. Same hardcode
   drift surface as the mode-rationale map — sourced from the operator's
   strategic vocabulary + Joe's known operating patterns; covered by
   :sorry/wm-ui-hud-mode-rationale-hardcode (the hardcode-vs-dynamic-read
   resolution-path applies)."
  {"hermit-warning"   ["Open a consulting-workstream repo and commit something concrete."
                       "Check the highest-severity open sorry in sorrys.edn."
                       "Review the cargo: are there undelivered sorries that should ship before more stack work?"]
   "hobby-warning"    ["Audit recent portfolio commits: which advance JSDQ maturity vs which are hobby drift?"
                       "Identify the next JSDQ maturity milestone and the smallest commit that advances it."]
   "foraging-warning" ["Move to depositing mode: pick a near-finished thing and ship it."
                       "Cap mathematics-workstream time-budget for the next session."]
   "cargo-warning"    ["Close the highest-priority open sorry (see sorrys.edn :kind :status)."
                       "Open a consulting-workstream commit (the cargo→depositing constraint)."
                       "Check the Strategic SORRY Topology story for context."]})

(defn- on-click-tick [tick-id observed]
  (fn [_evt]
    (js/console.log
     (str "[anchor wm-ui-anchor:0008] tick=" tick-id
          " observed=" (or observed "(none)")
          " → see ~/code/futon5a/data/war-machine-strategic-vocabulary.edn"
          " :harmonisation :jsdq-mapping AND Strategic SORRY Topology"))))

(defn- warnings-tile []
  "Anchored warning strip per wm-ui-anchor:0008. Shows directly-computed
   firing ticks with: (1) concrete observed state as the primary signal,
   (2) per-tick actionable hints the operator can act on, (3) the
   aphoristic :fires text demoted to italic flavour-text below.

   Each warning card is clickable (testid `warning-card-<tick-id>`);
   tooltip carries the :condition; on-click logs the anchor reference +
   observed state."
  (let [ticks (->> (get-in @s/data [:graph :dynamics :ticks])
                   (filter :computed?)
                   (filter :fired?)
                   vec)]
    (when (seq ticks)
      [:div.warnings-card {:data-testid "pocketwatch-warnings"}
       [:h3 "Warnings"]
       [:ul.warnings-list
        (for [t ticks]
          (let [tick-id-str (let [i (:id t)]
                              (cond
                                (keyword? i) (name i)
                                (string? i)  i
                                :else        (str i)))
                actions (get tick-actions tick-id-str)
                concrete-tooltip (str (tick-title (:id t)) " — "
                                     "condition: " (or (:condition t) "(condition unspecified)")
                                     ". Source: " (or (:signal t) "(unspecified)")
                                     ". Source-of-truth: strategic-vocabulary :harmonisation :jsdq-mapping."
                                     " Click for the anchor reference.")]
            ^{:key (str (:id t))}
            [:li {:data-testid (str "warning-card-" tick-id-str)
                  :on-click (on-click-tick tick-id-str (:observed t))
                  :style {:cursor "pointer"
                          :padding "6px 4px"
                          :border-bottom "1px dotted #d1d5db"}
                  :title concrete-tooltip}
             ;; (a) primary signal: concrete observed state with the
             ;; tick name; replaces the aphorism-as-primary that anchor
             ;; 0008 flagged as the warning's worst legibility failure.
             [:div {:style {:font-weight "600" :margin-bottom "4px"}}
              [:span.warning-name (tick-title (:id t))]
              " "
              (or (:observed t) "(observed state unavailable)")]
             ;; (c) actionable hints — per-tick hardcoded; covered by
             ;; existing :sorry/wm-ui-hud-mode-rationale-hardcode for
             ;; drift surface.
             (when (seq actions)
               [:ul {:style {:margin "4px 0 4px 16px" :padding 0
                             :font-size "0.9em"}}
                (for [[i a] (map-indexed vector actions)]
                  ^{:key i} [:li {:style {:list-style "disc"}} a])])
             ;; The aphoristic :fires text demoted to italic flavour
             ;; below the concrete + actionable content. Anchor 0008:
             ;; 'The aphorism itself may have a place — perhaps as
             ;; italic flavour-text below the plain-language warning —
             ;; but should not be the primary signal.'
             (when-let [aphorism (:fires t)]
               [:div {:style {:font-style "italic"
                              :color "#6b7280"
                              :font-size "0.85em"
                              :margin-top "4px"}}
                aphorism])]))]])))

(defn- severity-class [severity]
  (case severity
    :critical "critical"
    :warning "warning"
    :info "info"
    "warning"))

(defn- tier-class [tier]
  (case tier
    :stop-the-line "critical"
    :high "warning"
    :advisory "info"
    "info"))

(defn- fmt-num [n]
  (when (number? n)
    (.toFixed (double n) 2)))

(defn- fmt-age [n]
  (when (number? n)
    (str (.toFixed (double n) 1) "d")))

;; Anchor 0010 (Self-watch active-warnings counter — batch 2). Joe's
;; verbatim: "Those look like items I could hand off to an agent. I will
;; try that and report back." v1 ships:
;;   (1) anchored counter (tooltip + click-log naming the API path)
;;   (2) per-issue 'Hand off to agent' button that POSTs to /api/alpha/bell
;;       with the issue summary + severity + action. Operator picks the
;;       target agent via a browser prompt (v1 — a dropdown UI is v2).
;; See M-war-machine-frontend-upgrade1 §6.12.
;;
;; Note on shape: this is the SECOND structured-warning-rendering case
;; (after 0008 cargo). Different API path (:self-watch :issues, not
;; :graph :dynamics :ticks) and different per-item schema, so the
;; renderer below is a sibling not a clone of warnings-tile. Per claude-10's
;; threshold rule, this promotes :structured-warning-rendering-net from
;; candidate to tracked sub-kind.
(defn- hand-off-issue! [issue]
  "POST a bell to /api/alpha/bell with the issue text. Operator picks
   the target agent at prompt-time. v1: browser prompt(); errors logged
   to console; success notifies via alert."
  (let [{:keys [severity surface summary action at source-id]} issue
        agent-id (js/prompt
                  (str "Hand off this Self-watch issue to which agent?\n"
                       "(e.g. claude-10, codex-3 — agent must be registered in the futon3c Agency)\n\n"
                       "Issue: " summary)
                  "claude-10")]
    (when (and agent-id (not (clojure.string/blank? agent-id)))
      (let [prompt-text (str "Self-watch hand-off (anchor wm-ui-anchor:0010 / wm-ui surface):\n"
                            "Severity: " (name severity) "\n"
                            "Surface: " surface "\n"
                            "At: " (or at "(unknown)") "\n"
                            "Source-id: " (or source-id "(none)") "\n"
                            "Summary: " summary "\n"
                            "Suggested action: " (or action "(none specified)") "\n\n"
                            "Please investigate and report back.")
            body (.stringify js/JSON
                             (clj->js {:agent-id agent-id
                                       :caller "wm-ui"
                                       :surface "bell"
                                       :prompt prompt-text}))]
        (js/console.log
         (str "[anchor wm-ui-anchor:0010] handing off issue '" summary
              "' (severity=" (name severity) ") to agent=" agent-id))
        (-> (js/fetch "http://localhost:7070/api/alpha/bell"
                      (clj->js {:method "POST"
                                :headers {"Content-Type" "application/json"
                                          "Accept" "application/json"}
                                :body body}))
            (.then (fn [r] (.json r)))
            (.then (fn [j]
                     (let [parsed (js->clj j :keywordize-keys true)
                           job-id (:job-id parsed)]
                       (js/console.log
                        (str "[anchor wm-ui-anchor:0010] bell accepted: job-id="
                             job-id))
                       (js/alert (str "✓ Handed off to " agent-id
                                      "\nJob-id: " job-id
                                      "\n\nThey'll process when their session permits.")))))
            (.catch (fn [e]
                      (js/console.error
                       (str "[anchor wm-ui-anchor:0010] bell FAILED: " e))
                      (js/alert (str "✗ Hand-off failed: " e
                                     "\nCheck console for details.")))))))))

;; Undocumented bug U6 (commit-hygiene routing — claude-10 batch 2):
;; stop-the-line tier commit-hygiene queues don't route into the Self-watch
;; :issues feed. The PROPER fix is substrate-side (futon3c self-watch
;; subsystem should ingest commit-hygiene → :issues); the UI route below is
;; a v1 bandage so the operator sees stop-the-line dirty-tree pressure
;; alongside other Self-watch issues + can hand them off via the same
;; per-issue button. Substrate-side fix is a separate ticket
;; (M-war-machine-frontend-upgrade1 §6.13).
(defn- synthesize-commit-hygiene-issues
  "Convert commit-hygiene queues at :tier \"stop-the-line\" into issues
   shaped like Self-watch :issues, so the counter + per-issue list +
   hand-off button surface them uniformly."
  [ch]
  (->> (or (:queues ch) [])
       (filter #(= "stop-the-line" (let [t (:tier %)]
                                     (cond
                                       (keyword? t) (name t)
                                       (string? t)  t
                                       :else        (str t)))))
       (mapv (fn [{:keys [repo pressure count max-age-days needs-fixing action]}]
               {:severity "critical"
                :surface (str "commit-hygiene/" repo)
                :summary (or needs-fixing
                             (str repo " has " count " dirty paths, pressure " pressure))
                :action (or action
                            (str "Review " repo " for commit/disposition clustering"))
                :at "(synthesized from /api/alpha/war-machine :commit-hygiene :queues; substrate-side routing pending — M-war-machine-frontend-upgrade1 §6.13 U6)"
                :source-id (str "commit-hygiene-" repo)}))))

;; Inhabitation Log card — replaces the static Pilot Contract card per
;; M-war-machine-pilot cycle-3 (2026-05-25).  Reads (:pilot-inhabitations data)
;; from /api/alpha/war-machine; substrate is futon5a/data/pilot-inhabitations.edn.
;; Click [work in progress] / [work done] to expand event details (HTML details).

(defn- short-event-id [event-id]
  (when event-id
    (str/replace (str event-id) #"^inhab/[^/]+/" "")))

(defn- vsatarcs-status-badge
  "Per-event badge showing whether E-pilot-vsatarcs-feed has ingested this
   pilot-inhabitation event into VSATARCS bilateral evidence.  Joe's
   emacs-repl request 2026-05-25: surface the feeder's per-event status
   in the Inhabitation Log so processed/pending state is operator-visible."
  [processed?]
  [:span {:title (if processed?
                   "VSATARCS bilateral evidence present (E-pilot-vsatarcs-feed ingested this event)"
                   "VSATARCS bilateral evidence NOT yet present (feeder hasn't ingested this event yet, or the event isn't in its substantive-event whitelist)")
          :style {:font-size "0.75em"
                  :padding "1px 6px"
                  :border-radius "8px"
                  :margin-left "6px"
                  :background (if processed? "#d1fae5" "#f3f4f6")
                  :color (if processed? "#065f46" "#6b7280")
                  :cursor "help"}}
   (if processed? "VSATARCS ✓" "VSATARCS ○")])

(defn- inhab-event-row [ev]
  ^{:key (str (:id ev))}
  [:div {:style {:margin-bottom "6px"}}
   [:strong (short-event-id (:id ev))]
   " — " (or (:event ev) "?")
   (when-let [tool (:tool ev)]
     (str " via " tool))
   (when-let [tgt-anchor (get-in ev [:target :anchor])]
     (str " → " tgt-anchor))
   (vsatarcs-status-badge (:vsatarcs-processed? ev))
   (when-let [cg (:cited-consent-gate-event-id ev)]
     [:div {:style {:font-size "0.85em" :color "#666"}}
      (str "cg: " cg)])
   (when-let [target (or (:investigation-target ev)
                         (when (map? (:target ev)) (:file (:target ev))))]
     [:div {:style {:font-size "0.85em" :color "#666"}} target])])

(defn- inhabitation-log-card [data]
  (let [inh    (:pilot-inhabitations data)
        cur    (:current-inhabitant inh)
        prev   (:previous-inhabitant inh)
        stale? (:stale? inh)
        detail-box-style {:padding "6px 8px"
                          :border "1px solid #d1d5db"
                          :border-radius "4px"
                          :margin "4px 0"
                          :background "#f9fafb"
                          :font-size "0.85em"}]
    [:section.dashboard-card
     [:div.dashboard-card-header
      [:h3 "Inhabitation Log"]
      [:span.dashboard-badge "live"]]
     (cond
       stale?
       [:div.dashboard-empty
        (str "Pilot inhabitations log unavailable"
             (when-let [e (:error inh)] (str ": " e)))]

       (and (nil? cur) (nil? prev))
       [:div.dashboard-empty "No inhabitation events recorded yet."]

       :else
       [:div.dashboard-text
        (when cur
          [:span "current inhabitant " [:strong (:agent cur)] ", "
           [:details {:style {:display "inline"}
                      :data-testid "inhabitation-log-wip"}
            [:summary {:style {:cursor "pointer" :display "inline"}}
             [:em "[work in progress]"]]
            [:div {:style detail-box-style}
             (if (seq (:wip-events cur))
               (for [ev (:wip-events cur)] (inhab-event-row ev))
               [:div "No work-in-progress events."])
             [:div {:style {:margin-top "6px" :color "#666"}}
              (str "Inhabiting since " (or (:since cur) "?"))]]]
           " " (or (:wip-cycles cur) 0) " tasks"])
        (when (and cur prev) "; ")
        (when prev
          [:span "previous inhabitant " [:strong (:agent prev)] ", "
           [:details {:style {:display "inline"}
                      :data-testid "inhabitation-log-done"}
            [:summary {:style {:cursor "pointer" :display "inline"}}
             [:em "[work done]"]]
            [:div {:style detail-box-style}
             (if (seq (:done-events prev))
               (for [ev (:done-events prev)] (inhab-event-row ev))
               [:div "No completed cycles."])
             [:div {:style {:margin-top "6px" :color "#666"}}
              (str "Inhabitation ended " (or (:ended-at prev) "?"))]]]
           " " (or (:done-cycles prev) 0) " tasks"])])]))

(defn- self-watch-dashboard []
  (let [data @s/data
        sw (:self-watch data)
        ch (:commit-hygiene data)
        mb (:metabolic-balance data)
        ;; U6 v1: synthesize commit-hygiene stop-the-line queues into the
        ;; issues list. Operator sees them in the counter + can hand them
        ;; off. Substrate-side fix (route via futon3c self-watch) is a
        ;; separate ticket.
        synth-issues (synthesize-commit-hygiene-issues ch)
        issues (vec (concat (or (:issues sw) []) synth-issues))
        recoveries (or (:recoveries sw) [])
        queues (or (:queues ch) [])
        channels (or (:channels mb) [])
        ;; Adjust counts to reflect synthesized critical issues.
        critical-n (+ (or (:critical-count sw) 0) (count synth-issues))
        warning-n (:warning-count sw)
        counter-tooltip (str "Active self-watch warnings: "
                            (count issues) " open"
                            (when critical-n (str " (" critical-n " critical"))
                            (when warning-n (str ", " warning-n " warning"))
                            (when (or critical-n warning-n) ")")
                            ". Source: /api/alpha/war-machine response, :self-watch :issues field "
                            "(populated by the futon3c self-watch subsystem; distinct from the "
                            ":graph :dynamics :ticks pocketwatch warnings shown on the stack view). "
                            "Click for the anchor reference.")
        channel-rows (when (seq channels)
                       (into
                        [:div.dashboard-mini-list]
                        (map (fn [{:keys [channel pressure tier count]}]
                               [:div.dashboard-mini-row {:key (str channel)}
                                [:span (name channel)]
                                [:span (str (name tier)
                                            " • "
                                            (or (fmt-num pressure) "0.00")
                                            (when (number? count)
                                              (str " • " count)))]]))
                        channels))]
    [:div.dashboard-panel {:data-testid "self-watch-dashboard"}
     [:div.dashboard-header
      [:h2 "Self-Watch"]
      [:div.dashboard-subtitle
       "Operational maintenance surface for infrastructure health, load-time warnings, and commit hygiene pressure."]]
     [:div.dashboard-grid
      [:section.dashboard-card.dashboard-card-wide
       [:div.dashboard-card-header
        [:h3 "Active Warnings"]
        ;; Anchored counter per wm-ui-anchor:0010.
        [:span.dashboard-badge
         {:data-testid "self-watch-issue-count"
          :title counter-tooltip
          :on-click (fn [_]
                      (js/console.log
                       (str "[anchor wm-ui-anchor:0010] active-warnings="
                            (count issues)
                            " critical=" (or critical-n 0)
                            " warning=" (or warning-n 0)
                            " → see /api/alpha/war-machine :self-watch")))
          :style {:cursor "pointer"
                  :text-decoration "underline dotted"}}
         (str (count issues) " open"
              (when (and critical-n (pos? critical-n))
                (str " (" critical-n " critical)"))
              " ⓘ")]]
       (if (seq issues)
         [:div.dashboard-list
          (for [{:keys [severity surface summary action at source-id] :as issue} issues]
            ^{:key (str surface "|" summary "|" at)}
            [:article {:class (str "dashboard-item " (severity-class severity))
                       :data-testid (str "self-watch-issue-" (or source-id at))}
             [:div.dashboard-item-top
              [:span.dashboard-chip (name severity)]
              [:span.dashboard-meta surface]
              [:span.dashboard-meta (or at "-")]]
             [:div.dashboard-title summary]
             [:div.dashboard-text action]
             ;; Per-issue Hand-off-to-agent button. Anchor 0010 v1: prompt
             ;; for agent-id, POST to /api/alpha/bell with the issue text.
             ;; v2 would replace the prompt() with a dropdown of currently-
             ;; registered Agency agents (from GET /api/alpha/agents).
             [:button {:data-testid (str "self-watch-handoff-" (or source-id at))
                       :on-click (fn [_] (hand-off-issue! issue))
                       :style {:margin-top "6px"
                               :padding "4px 10px"
                               :font-size "0.85em"
                               :background "#3b82f6"
                               :color "white"
                               :border "none"
                               :border-radius "4px"
                               :cursor "pointer"}}
              "📤 Hand off to agent"]])]
         [:div.dashboard-empty "No active self-watch warnings in the current window."])]

      [:section.dashboard-card
       [:div.dashboard-card-header
        [:h3 "Recoveries"]
        [:span.dashboard-badge (str (count recoveries) " recent")]]
       (if (seq recoveries)
         [:div.dashboard-list
          (for [{:keys [summary at]} (take 6 recoveries)]
            ^{:key (str summary "|" at)}
            [:article {:class (str "dashboard-item " (severity-class :info))}
             [:div.dashboard-item-top
              [:span.dashboard-chip "info"]
              [:span.dashboard-meta (or at "-")]]
             [:div.dashboard-title summary]
             [:div.dashboard-text "Recovered after a prior alert. No action unless it recurs."]])]
         [:div.dashboard-empty "No recent process recoveries recorded."])]

      [:section.dashboard-card.dashboard-card-wide
       [:div.dashboard-card-header
        [:h3 "Commit Hygiene"]
        [:span.dashboard-badge
         (str (or (:active-count ch) 0) " repos"
              " • "
              (name (or (:clustering-status ch) :unavailable)))]]
       [:div.dashboard-card-note
        "This is still repo-level pressure, not safe auto-commit clustering. The next step is grouping changed files into coherent candidate bundles."]
       (if (seq queues)
         [:div.dashboard-list
          (for [{:keys [repo tier pressure count max-age-days action]} queues]
            ^{:key repo}
            [:article {:class (str "dashboard-item " (tier-class tier))}
             [:div.dashboard-item-top
              [:span.dashboard-chip (name tier)]
              [:span.dashboard-meta repo]
              [:span.dashboard-meta (str (or count 0) " dirty")]]
             [:div.dashboard-title
              (str repo " pressure " (or (fmt-num pressure) "?")
                   " • max age " (or (fmt-age max-age-days) "?"))]
             [:div.dashboard-text action]])]
         [:div.dashboard-empty "No repos above the current working-tree reporting floor."])]

      [:section.dashboard-card
       [:div.dashboard-card-header
        [:h3 "Metabolic Pressure"]
        [:span.dashboard-badge (name (or (:max-tier mb) :unknown))]]
       (if (:available? mb)
         [:div.dashboard-metrics
          [:div.dashboard-metric-row
           [:span "Max pressure"]
           [:strong (or (fmt-num (:max-pressure mb)) "0.00")]]
          [:div.dashboard-metric-row
           [:span "Snapshot age"]
           [:strong (if-let [age (:snapshot-age-minutes mb)]
                      (str (.toFixed (double age) 0) "m")
                      "-")]]
          [:div.dashboard-metric-row
           [:span "Stale?"]
           [:strong (if (:stale? mb) "yes" "no")]]
          channel-rows]
         [:div.dashboard-empty "Metabolic snapshot unavailable."])]

      [inhabitation-log-card data]]]))

(defn- track-summary [session]
  (let [start (some-> (:start session) waveform/parse-ms waveform/fmt-date-time)
        end (some-> (:end session) waveform/parse-ms waveform/fmt-date-time)
        entries (or (:entry-count session) 0)
        repo-n (count (:repos-touched session))
        mission-n (count (:missions-touched session))]
    (str (or start "?")
         " -> "
         (or end "?")
         "  |  "
         entries " ev"
         "  |  "
         repo-n " repos"
         "  |  "
         mission-n " missions")))

(defn- sync-track-visibility! []
  (reset! s/hotspot {})
  (tick/sync-replay-to-playhead! (:playhead-ms @s/replay) @s/data false))

(defn- toggle-track! [sid]
  (swap! s/track-ui update :enabled
         (fn [enabled]
           (assoc enabled sid (not (get enabled sid true)))))
  (sync-track-visibility!))

(defn- tracks-panel [data]
  (let [sessions (get-in data [:sessions :sessions] [])
        enabled (:enabled @s/track-ui)
        selected (:selected @s/track-ui)
        selected-session (some #(when (= (:session-id %) selected) %) sessions)]
    [:div
     [:h3 "Replay Tracks"]
     (if (seq sessions)
       [:div {:style {:display "grid" :gap "6px"}}
        (for [session sessions
              :let [sid (:session-id session)
                    on? (get enabled sid true)
                    chosen? (= sid selected)]]
          ^{:key (str sid)}
          [:div {:on-click #(swap! s/track-ui assoc :selected sid)
                 :data-testid (str "track-row-" sid)
                 :style {:border (str "1px solid " (if chosen? "#3b82f6" "#d1d5db"))
                         :background (if on? "#ffffff" "#f3f4f6")
                         :border-radius "4px"
                         :padding "6px 8px"
                         :cursor "pointer"
                         :opacity (if on? 1.0 0.55)}}
           [:div.row
            [:label {:style {:display "flex"
                             :align-items "center"
                             :gap "6px"
                             :cursor "pointer"}
                     :on-click #(.stopPropagation %)}
             [:input {:type "checkbox"
                      :data-testid (str "track-toggle-" sid)
                      :checked on?
                      :on-change #(toggle-track! sid)}]
             [:span {:style {:font-family "monospace"
                             :font-size "11px"
                             :font-weight 600}}
              sid]]]
           [:div {:style {:font-size "11px"
                          :color "#475569"
                          :line-height 1.35}}
            (track-summary session)]])]
       [:div {:data-testid "track-empty"
              :style {:font-size "11px"
                      :color "#64748b"
                      :padding "4px 0"}}
        "No sessionized evidence in this window yet."] )
     (when selected-session
       [:div {:data-testid "track-detail"
              :style {:margin-top "10px"
                      :padding "8px 10px"
                      :border "1px solid #cbd5e1"
                      :border-radius "6px"
                      :background "#f8fafc"
                      :font-size "11px"}}
        [:div {:style {:font-family "monospace"
                       :font-weight 700
                       :margin-bottom "6px"}}
         (:session-id selected-session)]
        [:div (track-summary selected-session)]
        (when-let [repos (seq (:repos-touched selected-session))]
          [:div {:style {:margin-top "6px"}}
           [:strong "Repos: "]
           (str (str/join ", " (take 8 repos))
                (when (> (count repos) 8) " ..."))])
        (when-let [missions (seq (:missions-touched selected-session))]
          [:div {:style {:margin-top "4px"}}
           [:strong "Missions: "]
           (str (str/join ", " missions))])])]))

(defn- sidebar []
  (let [data @s/data]
    [:div.sidebar
     [:h3 "Loop Health"]
     ;; Anchor 0002 (Loop Health bars) — per-arrow tooltip using the API's
     ;; native :description / :count / :last-seen / :days-since fields (no
     ;; hardcode; richer than 0001 because the substrate already exposes
     ;; per-arrow rationale). Each bar is one of the 6 holistic-argument
     ;; arrows. See wm-ui-anchor:0002 + strategic-vocabulary :loop-health.
     ;; M-war-machine-frontend-upgrade1 §6.8.
     (when-let [lh (:loop-health data)]
       (for [{:keys [label health description count last-seen days-since arrow-id]}
             (:arrows lh)]
         (let [health-pct (str (.toFixed (* 100 (double (or health 0))) 0) "%")
               testid (str "loop-arrow-" (or arrow-id label))
               last-seen-text (cond
                                (and last-seen days-since (zero? days-since)) (str last-seen " (today)")
                                (and last-seen days-since) (str last-seen " (" days-since " day(s) ago)")
                                :else "never")
               count-text (cond
                            (or (nil? count) (zero? count)) "no evidence in window"
                            (= 1 count) "1 evidence entry"
                            :else (str count " evidence entries"))
               tooltip (str label " — " (or description "(no description)")
                           ". " count-text "; last seen " last-seen-text
                           ". Health " health-pct ". Source: holistic-argument scan; "
                           "see strategic-vocabulary :o/raw-fields :loop-health.")]
           ^{:key label}
           [:div
            [:div.row
             [:span label]
             [:span {:data-testid testid
                     :title tooltip
                     :on-click (fn [_]
                                 (js/console.log
                                  (str "[anchor wm-ui-anchor:0002] " label "=" health-pct
                                       " count=" (or count 0))))
                     :style {:cursor "pointer"
                             :text-decoration "underline dotted"}}
              health-pct " ⓘ"]]
            [:div.bar
             [:div {:style {:width (str (* 100 (double (or health 0))) "%")
                            :background (cond
                                          (> (or health 0) 0.7) "#22c55e"
                                          (> (or health 0) 0.4) "#eab308"
                                          :else "#ef4444")}}]]])))
     [:h3 "Workstream Balance"]
     ;; Per-workstream anchor tooltips. Anchor 0001 (consulting 0%) is
     ;; explicit substrate-side; the other 3 workstreams (stack, mathematics,
     ;; portfolio) share the same legibility-gap shape (measurement-substrate
     ;; vs repo-presence; target range from strategic-vocab :C/preferred;
     ;; jsdq-analog from :o/provenance) so they get the same affordance.
     ;; claude-10 may author follow-up anchors (0001a/b/c or expand 0001) if
     ;; substrate-side coordination needs them anchored individually.
     ;; See M-war-machine-frontend-upgrade1 §6.8 + wm-ui-anchor:0001.
     (let [;; Per-workstream tooltip data, sourced from
           ;; ~/code/futon5a/data/war-machine-strategic-vocabulary.edn
           ;; :o/provenance and :C/preferred (lines 88-110 + 189-201).
           ;; Hardcoded for v1; same drift-vs-dynamic-read consideration as
           ;; the Mode anchor's rationale map (see sorrys.edn
           ;; :sorry/wm-ui-hud-mode-rationale-hardcode).
           ws-info
           {"consulting"  {:anchor-id "wm-ui-anchor:0001"
                           :label-pattern "Paid consulting + day job"
                           :jsdq "depositing"
                           :target "[0.20, 0.35]"
                           :substrate "commits to consulting-workstream repos over the active scan window (NOT presence-in-view; ~/code/invoices/ and ~/code/statements/ are not classified as consulting-workstream repos by the WM's :o/provenance :consulting-pct)"}
            "stack"       {:anchor-id "wm-ui-anchor:0001a"
                           :label-pattern "Stack inhabitation (futon0-5)"
                           :jsdq "maintaining"
                           :target "[0.15, 0.25]"
                           :substrate "commits to futon0-5 stack-workstream repos over the active scan window"}
            "mathematics" {:anchor-id "wm-ui-anchor:0001b"
                           :label-pattern "Mathematics (futon6)"
                           :jsdq "foraging"
                           :target "[0.15, 0.25]"
                           :substrate "commits to mathematics-workstream repos over the active scan window (primarily futon6)"}
            "portfolio"   {:anchor-id "wm-ui-anchor:0001c"
                           :label-pattern "Portfolio development (JSDQ carrying + trail-laying)"
                           :jsdq "carrying"
                           :target "[0.20, 0.35]"
                           :substrate "commits to portfolio-workstream repos over the active scan window"}}]
       (when-let [pcts (get-in data [:graph :dynamics :commit-percentages])]
         (for [[ws pct] (sort-by val > pcts)]
           (let [ws-name (if (keyword? ws) (name ws) (str ws))
                 info (get ws-info ws-name)
                 pct-text (str (.toFixed (* 100 (double pct)) 0) "%")
                 tooltip (when info
                           (str ws-name "-pct = % of " (:substrate info)
                                ". Target " (:target info) " (JSDQ analog: "
                                (:jsdq info) "). Current " pct-text
                                ". Source: war-machine-strategic-vocabulary.edn "
                                ":o/provenance :" ws-name "-pct; logic-model + jsdq-terminal."))]
             ^{:key ws}
             [:div
              [:div.row
               [:span ws-name]
               (if info
                 ;; Inline anchored span (workstream-anchor style; tooltip
                 ;; per-workstream, click logs the anchor reference). Not
                 ;; via `hud/anchor-span` because that helper lives in the
                 ;; hud ns; this is a local equivalent.
                 [:span {:data-testid (str "ws-pct-" ws-name)
                         :title tooltip
                         :on-click (fn [_]
                                     (js/console.log
                                      (str "[anchor " (:anchor-id info)
                                           "] " ws-name "=" pct-text)))
                         :style {:cursor "pointer"
                                 :text-decoration "underline dotted"}}
                  pct-text " ⓘ"]
                 [:span pct-text])]
              [:div.bar
               [:div {:style {:width (str (* 100 (double pct)) "%")
                              :background (case ws-name
                                            "stack" "#4a90e2"
                                            "mathematics" "#22c55e"
                                            "portfolio" "#eab308"
                                            "consulting" "#ef4444"
                                            "#9ca3af")}}]]]))))
     [:h3 "Missions"]
     (when-let [mt (:mission-triage data)]
        [:div
        [:div.row [:span "Active"] [:span (str (:active mt))]]
        [:div.row [:span "Total"] [:span (str (:total mt))]]
        [:div.row [:span "Abandoned"] [:span (str (:abandoned-count mt))]]])
     [tracks-panel data]]))

(defn- main-panel []
  (if (= :self-watch @s/view-mode)
    [:div.hex-canvas.dashboard-canvas
     [self-watch-dashboard]]
    [:div.hex-canvas
     [graph/hex-map]]))

;; Stop-the-line override banner — surfaces the WM judgement mode override
;; documented in war-machine-strategic-vocabulary.edn :μ/override-modes.
;; When any metabolic-balance channel hits tier :stop-the-line, judge sets
;; mode to :stop-the-line; this banner renders above main-area in every
;; view so the override is not hidden behind a view-mode tab.
(defn- stop-the-line-banner [data]
  (when (= "stop-the-line" (get-in data [:judgement :mode]))
    (let [mb (:metabolic-balance data)
          channels (filter #(= "stop-the-line" (:tier %)) (:channels mb))
          max-pressure (:max-pressure mb)]
      [:div {:data-testid "stop-the-line-banner"
             :style {:background "#7f1d1d"
                     :color "#fef2f2"
                     :padding "12px 20px"
                     :border-bottom "3px solid #450a0a"
                     :font-weight "600"
                     :display "flex"
                     :flex-direction "column"
                     :gap "6px"}}
       [:div {:style {:font-size "1.05em" :letter-spacing "0.05em"}}
        "STOP-THE-LINE — address metabolic pressure before other work"]
       [:div {:style {:font-size "0.9em" :font-weight "400" :opacity "0.95"}}
        (str "Max pressure " (or (fmt-num max-pressure) "?")
             " across " (count channels)
             (if (= 1 (count channels)) " channel" " channels")
             ".  Override active per :μ/override-modes (war-machine-strategic-vocabulary.edn).")]
       (when (seq channels)
         [:ul {:style {:margin "0" :padding-left "20px" :font-size "0.85em" :font-weight "400"}}
          (for [ch channels]
            ^{:key (str (:channel ch))}
            [:li (str (or (:channel ch) "(unnamed channel)")
                      " • pressure " (or (fmt-num (:pressure ch)) "?")
                      (when (number? (:count ch))
                        (str " • " (:count ch) " items")))])])])))

(defn app []
  (let [data @s/data]
    (if (nil? data)
      [:div.loading "Loading War Machine..."]
      [:div.war-machine
       [toolbar]
       [stop-the-line-banner data]
       [:div.main-area
        [legend-panel]
        [main-panel]
        [sidebar]]
       [hud/hud-line]
       [graph/detail-panel]])))

(defn ^:export aif-loaded? [] (some? @s/aif-data))

(defonce root (rdc/create-root (.getElementById js/document "app")))

(defn- resize-viewport! []
  (let [w (.-innerWidth js/window)
        h (.-innerHeight js/window)]
    (reset! s/viewport {:w (max 400 (- w 260))
                        :h (max 300 (- h 160))})))

(defn ^:dev/after-load reload []
  ;; Re-render the React tree AND refetch /api/alpha/war-machine so any
  ;; backend payload changes (e.g. new fields added by an edit this session)
  ;; flow into s/data without an operator-side page refresh.  Adds a WM scan
  ;; per code reload — acceptable dev cost; production builds use `init`
  ;; not `reload`.
  (rdc/render root [app])
  (api/ensure-wm-poll!)
  (api/load!))

(defn init []
  (js/console.log "[init] war machine starting")
  (resize-viewport!)
  (.addEventListener js/window "resize" resize-viewport!)
  (api/load!)
  (api/ensure-wm-poll!)
  (api/load-aif-stack!)
  (api/ensure-aif-poll!)
  (tick/start!)
  (reload))
