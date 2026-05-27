(ns war-machine.client.waveform
  "Audacity-style activity waveform sitting in the toolbar.

   Shows one stripe per session, with a vertical bar for each step at its
   wall-clock timestamp.  Supports click-to-jump (sets each session's
   step-idx to the closest step), and click-drag to select a region (sets
   `:selection {:start-ms :end-ms}`).  When a selection is active and Play
   is on, the tick loop loops within that region (see tick.cljs).

   Step timestamps come from sessions.sessions[*].steps[*].at as ISO strings."
  (:require [war-machine.client.state :as s]))

;; ---------- Time helpers ----------

(defn parse-ms
  "Parse an ISO-8601 timestamp into ms-since-epoch.  Returns nil on bad input."
  [iso-string]
  (try
    (when iso-string
      (.getTime (js/Date. iso-string)))
    (catch :default _ nil)))

(defn fmt-time
  "Format ms-since-epoch as 'HH:MM' for axis labels."
  [ms]
  (when ms
    (let [d (js/Date. ms)
          h (.getHours d)
          m (.getMinutes d)]
      (str (when (< h 10) "0") h ":" (when (< m 10) "0") m))))

(def ^:private month-names
  ["Jan" "Feb" "Mar" "Apr" "May" "Jun"
   "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"])

(defn fmt-date-time
  "Format ms-since-epoch as 'DD Mon HH:MM' in local time."
  [ms]
  (when ms
    (let [d (js/Date. ms)
          dd (.getDate d)
          mon (nth month-names (.getMonth d))
          hh (.getHours d)
          mm (.getMinutes d)]
      (str dd " " mon " "
           (when (< hh 10) "0") hh ":"
           (when (< mm 10) "0") mm))))

(defn- fmt-axis-label
  "Format the waveform edge labels. Multi-day windows include the date."
  [ms earliest-ms latest-ms]
  (let [span (- (or latest-ms 0) (or earliest-ms 0))]
    (if (> span (* 36 60 60 1000))
      (fmt-date-time ms)
      (fmt-time ms))))

(defn- fmt-duration
  "Format a millisecond span as a compact human string."
  [ms]
  (let [total-min (max 0 (js/Math.round (/ ms 60000)))
        days (quot total-min (* 60 24))
        rem-min (- total-min (* days 60 24))
        hours (quot rem-min 60)
        mins (mod rem-min 60)]
    (cond
      (pos? days) (str days "d " hours "h")
      (pos? hours) (str hours "h " mins "m")
      :else (str mins "m"))))

(defn session-step-times
  "For one session, return [{:idx i :ms ms :weight w} ...] sorted by ms.
   weight = repos+missions touched count (visual bar height proxy)."
  [session]
  (->> (:steps session [])
       (map-indexed
        (fn [idx step]
          (let [ms (parse-ms (:at step))
                weight (+ (count (:repos step [])) (count (:missions step [])))]
            (when ms
              {:idx idx :ms ms :weight (max 1 weight)}))))
       (remove nil?)
       (sort-by :ms)
       vec))

(defn- track-enabled? [sid]
  (get-in @s/track-ui [:enabled sid] true))

(defn compute-bounds
  "Across all sessions in war-machine data, return {:earliest-ms :latest-ms}
   or nil if no timestamps."
  [data]
  (let [sessions (get-in data [:sessions :sessions] [])
        all-ms (mapcat #(map :ms (session-step-times %)) sessions)]
    (when (seq all-ms)
      {:earliest-ms (apply min all-ms)
       :latest-ms   (apply max all-ms)})))

(defn evidence-bounds
  "The actual span covered by sessionized evidence inside the loaded window."
  [data]
  (compute-bounds data))

(defn payload-bounds
  "Explicit timeline bounds carried by the server payload."
  [data]
  (let [start-ms (parse-ms (get-in data [:window :start]))
        end-ms (parse-ms (get-in data [:window :end]))]
    (when (and start-ms end-ms (< start-ms end-ms))
      {:earliest-ms start-ms
       :latest-ms end-ms})))

(defn current-bounds
  "The active timeline bounds for the current payload."
  [data]
  (or (payload-bounds data)
      (compute-bounds data)))

(defn display-bounds
  "Bounds used to render the waveform itself.

   Prefer actual session-evidence span so the stripe geometry reflects what
   can really be replayed, not just the broader query window."
  [data]
  (or (evidence-bounds data)
      (current-bounds data)))

(defn active-bounds
  "Playback bounds, narrowed to the current selection when present.

   Without a selection, playback traverses the actual evidence span rather than
   dead air across the whole 14d/90d axis."
  [data waveform-state]
  (let [bounds (or (evidence-bounds data)
                   (current-bounds data))
        sel (:selection waveform-state)]
    (cond
      (and bounds sel (:start-ms sel) (:end-ms sel)
           (< (:start-ms sel) (:end-ms sel)))
      {:earliest-ms (:start-ms sel)
       :latest-ms (:end-ms sel)}

      :else
      bounds)))

;; ---------- Geometry ----------

(def WIDTH 600)
(def HEIGHT 56)
(def STRIPE-MARGIN-TOP 6)
(def STRIPE-MARGIN-BOT 14)
(def MAX-STRIPE-HEIGHT 30)

(defn ms->x [ms earliest-ms latest-ms width]
  (let [span (max 1 (- latest-ms earliest-ms))
        frac (/ (double (- ms earliest-ms)) (double span))]
    (* width frac)))

(defn x->ms [x earliest-ms latest-ms width]
  (let [span (max 1 (- latest-ms earliest-ms))
        frac (/ (double x) (double width))]
    (+ earliest-ms (* frac span))))

;; ---------- Rendering ----------

(defn render-stripe
  "Render one session's stripe at vertical offset y."
  [steps y stripe-h color earliest-ms latest-ms width]
  [:g {:opacity 0.85}
   (for [{:keys [idx ms weight]} steps
         :let [x (ms->x ms earliest-ms latest-ms width)
               h (* stripe-h (min 1.0 (/ weight 5.0)))]]
     ^{:key (str idx "-" ms)}
     [:line {:x1 x :y1 (+ y (- stripe-h h)) :x2 x :y2 (+ y stripe-h)
             :stroke color :stroke-width 1.5}])])

(defn render-selection
  "Translucent region overlay if selection is active."
  [{:keys [start-ms end-ms]} earliest-ms latest-ms width]
  (when (and start-ms end-ms)
    (let [x1 (ms->x (min start-ms end-ms) earliest-ms latest-ms width)
          x2 (ms->x (max start-ms end-ms) earliest-ms latest-ms width)]
      [:rect {:x x1 :y 0 :width (max 2 (- x2 x1)) :height HEIGHT
              :fill "#3b82f6" :fill-opacity 0.15
              :stroke "#3b82f6" :stroke-width 1
              :pointer-events "none"}])))

(defn- quantile
  "Nearest-rank quantile from sorted numeric values."
  [sorted-values q]
  (when (seq sorted-values)
    (let [n (count sorted-values)
          idx (js/Math.floor (* q (max 0 (dec n))))]
      (nth sorted-values idx))))

(defn- current-step-mss
  "Projected timestamps for all enabled tracks at the current playhead."
  [data]
  (let [sessions (get-in data [:sessions :sessions] [])
        replay @s/replay]
    (->> sessions
         (keep (fn [session]
                 (let [sid (:session-id session)
                       state (get replay sid)
                       idx (:step-idx state)
                       steps (:steps session)]
                   (when (and (track-enabled? sid)
                              (some? idx)
                              (< idx (count steps)))
                     (parse-ms (:at (nth steps idx)))))))
         sort
         vec)))

(defn- render-density-envelope
  "Render a faint uncertainty band showing the spread of current track times."
  [data earliest-ms latest-ms width]
  (let [mss (current-step-mss data)]
    (when (seq mss)
      (let [lo (first mss)
            hi (last mss)
            q1 (quantile mss 0.25)
            q3 (quantile mss 0.75)
            x-lo (ms->x lo earliest-ms latest-ms width)
            x-hi (ms->x hi earliest-ms latest-ms width)
            x-q1 (ms->x q1 earliest-ms latest-ms width)
            x-q3 (ms->x q3 earliest-ms latest-ms width)]
        [:g {:pointer-events "none"
             :data-testid "waveform-envelope"}
         [:rect {:x x-lo :y 0 :width (max 1 (- x-hi x-lo)) :height (- HEIGHT 14)
                 :fill "#0ea5e9" :fill-opacity 0.06}]
         [:rect {:x x-q1 :y 0 :width (max 1 (- x-q3 x-q1)) :height (- HEIGHT 14)
                 :fill "#0ea5e9" :fill-opacity 0.12}]
         [:line {:x1 x-lo :y1 5 :x2 x-hi :y2 5
                 :stroke "#0284c7" :stroke-opacity 0.3
                 :stroke-width 1.2}]]))))

(defn- selection-label
  "Human label for the current selected region."
  [{:keys [start-ms end-ms]}]
  (when (and start-ms end-ms)
    (str (fmt-date-time start-ms)
         " -> "
         (fmt-date-time end-ms)
         "  (" (fmt-duration (- end-ms start-ms)) ")")))

(defn- bounds-note
  "Explain the relationship between the requested scan window and the replayable
   session-evidence span.

   U2 fix (2026-05-24, M-war-machine-frontend-upgrade1 §6.11): the previous
   cond only annotated the case where session-evidence was NARROWER than the
   requested window. In practice sessions OVERFLOW the window (sessions live
   in futon3c forever; the WM scan-window is just a query parameter), so the
   timeline visually shows e.g. ~96 days while the label said only 'Scan
   window 14d' — operator-confusing per Joe's QA. The fix annotates BOTH
   directions: narrower OR broader gets the explicit session-evidence-span
   note so the timeline axis and the window label can't disagree silently."
  [data display]
  (when-let [payload (payload-bounds data)]
    (let [days (get-in data [:window :days])
          payload-span (- (:latest-ms payload) (:earliest-ms payload))
          display-span (when display
                         (- (:latest-ms display) (:earliest-ms display)))
          disagree? (and display
                         display-span
                         payload-span
                         (not= display-span payload-span))]
      (cond
        (and disagree? (< display-span payload-span))
        (str "Scan window " days "d; session evidence span "
             (fmt-duration display-span)
             " (narrower than window)")

        (and disagree? (> display-span payload-span))
        (str "Scan window " days "d; session evidence span "
             (fmt-duration display-span)
             " (broader than window — timeline axis shows full session history)")

        days
        (str "Scan window " days "d")

        :else nil))))

(defn playhead-ms
  "Global playback cursor for the current replay."
  [_data]
  (:playhead-ms @s/replay))

(defn render-playhead
  "Vertical orange line at the global playback cursor."
  [data earliest-ms latest-ms width]
  (when-let [playhead (playhead-ms data)]
    (let [x (ms->x playhead earliest-ms latest-ms width)]
      [:line {:x1 x :y1 0 :x2 x :y2 (- HEIGHT 14)
              :stroke "#f97316" :stroke-width 2 :pointer-events "none"}])))

;; ---------- Mouse handlers ----------

(defn- evt->x [e]
  ;; offsetX relative to the SVG element
  (let [target (.-currentTarget e)
        rect (.getBoundingClientRect target)]
    (- (.-clientX e) (.-left rect))))

(defn- evt->ms [e earliest-ms latest-ms]
  (let [el (.-currentTarget e)
        bbox-w (.-width (.getBoundingClientRect el))
        x (evt->x e)]
    (x->ms x earliest-ms latest-ms bbox-w)))

(defn- jump-to!
  "Jump the global playhead to TARGET-MS and project sessions onto it."
  [data target-ms]
  (let [sessions (get-in data [:sessions :sessions] [])]
    (swap! s/replay assoc :playhead-ms target-ms)
    (doseq [s sessions]
      (let [sid (:session-id s)
            steps (session-step-times s)]
        (when (and (track-enabled? sid) (seq steps))
          (let [current-idx (or (->> steps
                                     (keep (fn [{:keys [idx ms]}]
                                             (when (<= ms target-ms) idx)))
                                     last)
                                (-> steps first :idx))]
            (swap! s/replay assoc-in [sid :step-idx] current-idx)))))))

(defn- on-mousedown [e earliest-ms latest-ms data]
  (let [ms (evt->ms e earliest-ms latest-ms)]
    (swap! s/waveform assoc :drag {:anchor-ms ms} :selection nil)
    (jump-to! data ms)))

(defn- on-mousemove [e earliest-ms latest-ms]
  (let [ms (evt->ms e earliest-ms latest-ms)
        wf @s/waveform]
    (swap! s/waveform assoc :hover-ms ms)
    (when-let [{:keys [anchor-ms]} (:drag wf)]
      (swap! s/waveform assoc :selection
             {:start-ms (min anchor-ms ms) :end-ms (max anchor-ms ms)}))))

(defn- on-mouseup [_]
  (swap! s/waveform
         (fn [wf]
           (let [sel (:selection wf)
                 wide-enough? (and sel (> (- (:end-ms sel) (:start-ms sel)) 500))]
             (cond-> (assoc wf :drag nil)
               (not wide-enough?) (assoc :selection nil))))))

(defn- on-mouseleave [_]
  (swap! s/waveform assoc :hover-ms nil :drag nil))

;; ---------- Main component ----------

(def session-colors
  ["#ff5000" "#00b4ff" "#c832c8" "#32c832"
   "#ffc800" "#ff3264" "#64c8c8" "#c89632"])

(defn waveform-component []
  (let [data     @s/data
        wf       @s/waveform
        bounds   (display-bounds data)]
    (if-not bounds
      [:div.waveform [:div.label.left "(no timeline data)"]]
      (let [{:keys [earliest-ms latest-ms]} bounds
            sessions (->> (get-in data [:sessions :sessions] [])
                          (filter #(track-enabled? (:session-id %)))
                          vec)
            stripe-count (max 1 (count sessions))
            stripe-h (/ (- HEIGHT STRIPE-MARGIN-TOP STRIPE-MARGIN-BOT) stripe-count)
            sel-label (selection-label (:selection wf))
            note-label (bounds-note data bounds)]
        [:div.waveform
         [:svg {:viewBox (str "0 0 " WIDTH " " HEIGHT)
                :preserveAspectRatio "none"
                :on-mouse-down #(on-mousedown % earliest-ms latest-ms data)
                :on-mouse-move #(on-mousemove % earliest-ms latest-ms)
                :on-mouse-up   on-mouseup
                :on-mouse-leave on-mouseleave
                :data-testid "waveform-svg"}
          ;; stripes
          (for [[i s] (map-indexed vector sessions)
                :let [steps (session-step-times s)
                      y (+ STRIPE-MARGIN-TOP (* i stripe-h))
                      color (nth session-colors (mod i (count session-colors)))]]
            ^{:key (or (:session-id s) i)}
            [render-stripe steps y stripe-h color earliest-ms latest-ms WIDTH])
          ;; selection overlay
          [render-selection (:selection wf) earliest-ms latest-ms WIDTH]
          ;; composite spread of current track positions
          [render-density-envelope data earliest-ms latest-ms WIDTH]
          ;; playhead
          [render-playhead data earliest-ms latest-ms WIDTH]]
         (when sel-label
           [:div.label.center {:data-testid "waveform-selection-label"} sel-label])
         (when (and note-label (not sel-label))
           [:div.label.center {:data-testid "waveform-bounds-note"} note-label])
         [:div.label.left (fmt-axis-label earliest-ms earliest-ms latest-ms)]
         [:div.label.right (fmt-axis-label latest-ms earliest-ms latest-ms)]]))))
