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
     [:button.toggle {:on-click (fn []
                                  (let [nxt (case vmode
                                              :stack :aif-stack
                                              :aif-stack :missions
                                              :missions :sorrys
                                              :sorrys :invariants
                                              :invariants :patterns
                                              :patterns :stack)]
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
        (swatch "#94a3b8" "identify")
        (swatch "#a78bfa" "map")
        (swatch "#4a90e2" "derive / in-progress")
        (swatch "#fb923c" "argue")
        (swatch "#eab308" "verify")
        (swatch "#10b981" "instantiate")
        (swatch "#15803d" "complete")
        (swatch "#cbd5e1" "ready")
        (swatch "#9ca3af" "unknown / no phase")
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
        (section "All cells")
        (swatch "#22c55e" "pattern collection")
        (section "Numeric badge")
        [:div {:style {:font-size "11px" :color (:text-dim "#64748b")}}
         "= pattern count under each hex"]]

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

(defn- next-move-tile []
  "Reads :reading :next-move from the live AIF+ payload and renders it as
   a prominent recommendation card.  Clicking 'Show in graph' selects the
   conflict node it dis-bites so the detail panel populates."
  (let [aif @s/aif-data
        nm  (get-in aif [:reading :next-move])]
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
         [:div.next-move-target
          (str "→ Close " (labels/stack-spine-display-id display-close))]
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
              (str (if shown? "Hide " "Show ") pid " detail")]))]))))

(defn- tick-title [tick-id]
  (case (if (keyword? tick-id) tick-id (keyword (str tick-id)))
    :hermit-warning "hermit!"
    :hobby-warning "hobby!"
    :foraging-warning "foraging!"
    :cargo-warning "cargo!"
    (str tick-id)))

(defn- warnings-tile []
  "Minimal warning strip: show only directly computed warnings that are
   currently firing. Proxy diagnostics stay out of the main UI."
  (let [ticks (->> (get-in @s/data [:graph :dynamics :ticks])
                   (filter :computed?)
                   (filter :fired?)
                   vec)]
    (when (seq ticks)
      [:div.warnings-card {:data-testid "pocketwatch-warnings"}
       [:h3 "Warnings"]
       [:ul.warnings-list
        (for [t ticks]
          ^{:key (str (:id t))}
          [:li
           [:span.warning-name (tick-title (:id t))]
           " "
           (or (:fires t) "warning firing")])]])))

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
     (when-let [lh (:loop-health data)]
       (for [{:keys [label health]} (:arrows lh)]
         ^{:key label}
         [:div
          [:div.row
           [:span label]
           [:span (str (.toFixed (* 100 (double health)) 0) "%")]]
          [:div.bar
           [:div {:style {:width (str (* 100 (double health)) "%")
                          :background (cond
                                        (> health 0.7) "#22c55e"
                                        (> health 0.4) "#eab308"
                                        :else "#ef4444")}}]]]))
     [:h3 "Workstream Balance"]
     (when-let [pcts (get-in data [:graph :dynamics :commit-percentages])]
       (for [[ws pct] (sort-by val > pcts)]
         ^{:key ws}
         [:div
          [:div.row
           [:span (if (keyword? ws) (name ws) (str ws))]
           [:span (str (.toFixed (* 100 (double pct)) 0) "%")]]
          [:div.bar
           [:div {:style {:width (str (* 100 (double pct)) "%")
                          :background (case (if (keyword? ws) (name ws) (str ws))
                                        "stack" "#4a90e2"
                                        "mathematics" "#22c55e"
                                        "portfolio" "#eab308"
                                        "consulting" "#ef4444"
                                        "#9ca3af")}}]]]))
     [:h3 "Missions"]
     (when-let [mt (:mission-triage data)]
       [:div
        [:div.row [:span "Active"] [:span (str (:active mt))]]
        [:div.row [:span "Total"] [:span (str (:total mt))]]
        [:div.row [:span "Abandoned"] [:span (str (:abandoned-count mt))]]])
     [tracks-panel data]]))

(defn app []
  (let [data @s/data]
    (if (nil? data)
      [:div.loading "Loading War Machine..."]
      [:div.war-machine
       [toolbar]
       [:div.main-area
        [legend-panel]
        [:div.hex-canvas [graph/hex-map]]
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
  (rdc/render root [app]))

(defn init []
  (js/console.log "[init] war machine starting")
  (resize-viewport!)
  (.addEventListener js/window "resize" resize-viewport!)
  (api/load!)
  (api/load-aif-stack!)
  (api/ensure-aif-poll!)
  (tick/start!)
  (reload))
