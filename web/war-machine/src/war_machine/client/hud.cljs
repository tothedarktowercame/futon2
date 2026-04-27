(ns war-machine.client.hud
  "Live scoreboard strip at the bottom of the window.

   The leading position indicator is tethered to the Audacity waveform: it
   shows the wall-clock time of the global playback cursor, and (when a
   selection is active) the % progress through that selection.  Falls back
   to the abstract tick counter when no timeline is loaded yet."
  (:require [war-machine.client.state :as s]
            [war-machine.client.waveform :as wf]))

(defn- running-count [replay]
  (count (filter (fn [[k v]] (and (not= k :playing?) (map? v))) replay)))

(defn- format-position [data]
  (let [_ @s/tick                         ;; subscribe so HUD re-renders each tick
        ms (wf/playhead-ms data)
        sel (:selection @s/waveform)
        wall (when ms (wf/fmt-time ms))]
    (cond
      (and ms sel (:start-ms sel) (:end-ms sel))
      (let [span (max 1 (- (:end-ms sel) (:start-ms sel)))
            pos  (max 0 (- ms (:start-ms sel)))
            pct  (.toFixed (* 100.0 (min 1.0 (/ (double pos) (double span)))) 0)]
        (str "Play " wall " (" pct "% of selection)"))

      ms
      (str "Play " wall)

      :else
      (str "Tick " (.padStart (str @s/tick) 4 " ")))))

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
        mode     (or (get-in aif [:scheduler :last-diagnostics :mode])
                     "?")]
    [:div.hud {:data-testid "hud"}
     (str (format-position data)
          "  |  Ants " (or running 0) "/" total
          "  |  Missions " active-m "/" total-m " (abandoned " ab ")"
          "  |  Sorrys " sorry-n
          "  |  Loop " (.toFixed (* 100.0 avg) 0) "%"
          "  |  Mode " (if (keyword? mode) (name mode) (str mode)))]))
