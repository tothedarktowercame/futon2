(ns war-machine.client.tick
  "Master tick loop — the Ants-demo heart, ported from the Swing play-timer.

   Every 400 ms:
   - increment tick counter
   - decay hotspot field by ×0.82 (pheromone fade)
   - advance a single global playhead through the active timeline window
   - project each session to the latest step at-or-before that playhead
   - bump hotspot intensity on every visited hex

   Tick always fires; playhead advancement is gated on (:playing? replay).

   When `(:selection @s/waveform)` is set, the playhead loops within that
   selected timestamp range instead of the full loaded timeline window."
  (:require [war-machine.client.state :as s]
            [war-machine.client.aif-join :as aif]
            [war-machine.client.waveform :as wf]))

(def ^:private tick-ms 400)
(def ^:private decay 0.82)
(def ^:private bump 0.6)
(def ^:private drop-below 0.05)
(def ^:private playhead-cycle-ms 90000)

(defn- decay-hotspot [m]
  (reduce-kv
   (fn [acc k v]
     (let [v' (* v decay)]
       (if (> v' drop-below) (assoc acc k v') acc)))
   {} m))

(defn- visited-ids
  "Cell ids the given session step touches in the active view-mode.
   In :aif-stack we upsample through the empirical join (see aif-join);
   everywhere else we read the raw :repos / :missions field."
  [vmode join-ctx step]
  (case vmode
    :aif-stack (aif/step->spine-ids (:leaf->spine join-ctx)
                                    (:mid->detail join-ctx)
                                    step)
    :missions  (:missions step)
    (:repos step)))

(defn- visible-step?
  "True when STEP projects onto at least one visible cell in the current view."
  [vmode join-ctx step]
  (boolean (seq (visited-ids vmode join-ctx step))))

(defn- step-ms [step]
  (when-let [iso (:at step)]
    (try (.getTime (js/Date. iso)) (catch :default _ nil))))

(defn- track-enabled? [sid]
  (get-in @s/track-ui [:enabled sid] true))

(defn- clamp-playhead [playhead-ms earliest-ms latest-ms]
  (cond
    (nil? playhead-ms) earliest-ms
    (< playhead-ms earliest-ms) earliest-ms
    (> playhead-ms latest-ms) latest-ms
    :else playhead-ms))

(defn- advance-playhead-ms [playhead-ms earliest-ms latest-ms]
  (let [span (max 1 (- latest-ms earliest-ms))
        delta (* span (/ (double tick-ms) (double playhead-cycle-ms)))
        next (+ (double playhead-ms) delta)]
    (if (> next latest-ms)
      (+ earliest-ms (mod (- next earliest-ms) span))
      next)))

(defn- step-idx-at-playhead
  "Return the latest step index whose timestamp is within the active window,
   not after PLAYHEAD-MS, and projects onto a visible cell in the current
   view. Returns nil before the first visible in-window step."
  [vmode join-ctx steps earliest-ms playhead-ms]
  (reduce-kv
   (fn [best idx step]
     (let [ms (step-ms step)]
       (let [start (js/Number earliest-ms)
             current (js/Number playhead-ms)
             point (js/Number ms)]
         (if (and (not (js/isNaN start))
                  (not (js/isNaN current))
                  (not (js/isNaN point))
                  (<= start point)
                  (<= point current)
                  (visible-step? vmode join-ctx step))
           idx
           best))))
   nil
   (vec steps)))

(defn- first-step-ms-in-bounds
  "Earliest visible step timestamp that falls within the active playback bounds."
  [data earliest-ms latest-ms vmode join-ctx]
  (->> (get-in data [:sessions :sessions] [])
       (mapcat :steps)
       (filter #(visible-step? vmode join-ctx %))
       (keep step-ms)
       (filter #(and (<= earliest-ms %)
                     (<= % latest-ms)))
       sort
       first))

(defn sync-replay-to-playhead!
  "Project every session onto PLAYHEAD-MS inside the current active window.

   When BUMP? is non-nil, hotspot intensity is bumped only for sessions whose
   projected step changed at this playhead."
  ([playhead-ms data]
   (sync-replay-to-playhead! playhead-ms data false))
  ([playhead-ms data bump?]
   (when-let [{:keys [earliest-ms latest-ms]} (wf/active-bounds data @s/waveform)]
     (let [playhead (clamp-playhead playhead-ms earliest-ms latest-ms)
           vmode @s/view-mode
           join-ctx (when (= vmode :aif-stack) (aif/join-context))
           state @s/replay
           bumped-ids (atom [])
           next-state
           (reduce-kv
            (fn [acc sid v]
              (cond
                (= sid :playing?) (assoc acc sid v)
                (= sid :playhead-ms) acc
                (map? v)
                (if-not (track-enabled? sid)
                  (assoc acc sid (assoc v :step-idx nil))
                  (let [steps (:steps v)
                        prev-idx (:step-idx v)
                        next-idx (step-idx-at-playhead vmode join-ctx
                                                       steps earliest-ms playhead)]
                    (when (and bump?
                               (not= prev-idx next-idx)
                               (some? next-idx))
                      (let [step (nth steps next-idx)]
                        (swap! bumped-ids into
                               (visited-ids vmode join-ctx step))))
                    (assoc acc sid (assoc v :step-idx next-idx))))
                :else
                (assoc acc sid v)))
            {:playing? (:playing? state)
             :playhead-ms playhead}
            state)]
       (reset! s/replay next-state)
       (doseq [id @bumped-ids]
         (swap! s/hotspot update id
                (fn [x] (min 1.0 (+ (double (or x 0.0)) bump)))))
       playhead))))

(defn reset-playback! [data]
  "Reset the global playhead to the first visible event in the active window."
  (when-let [{:keys [earliest-ms latest-ms]} (wf/active-bounds data @s/waveform)]
    (let [vmode @s/view-mode
          join-ctx (when (= vmode :aif-stack) (aif/join-context))
          reset-ms (or (first-step-ms-in-bounds data earliest-ms latest-ms
                                                vmode join-ctx)
                       earliest-ms)]
      (sync-replay-to-playhead! reset-ms data true))))

(defn- advance-ants! []
  (let [data @s/data
        state @s/replay]
    (when (and data (:playing? state))
      (when-let [{:keys [earliest-ms latest-ms]} (wf/active-bounds data @s/waveform)]
        (let [current (clamp-playhead (:playhead-ms state) earliest-ms latest-ms)
              next (advance-playhead-ms current earliest-ms latest-ms)]
          (sync-replay-to-playhead! next data true))))))

(defonce ^:private timer-id (atom nil))

(defn start! []
  (when-let [t @timer-id] (js/clearInterval t))
  (reset! timer-id
          (js/setInterval
           (fn []
             (swap! s/tick inc)
             (swap! s/hotspot decay-hotspot)
             (advance-ants!))
           tick-ms)))

(defn stop! []
  (when-let [t @timer-id]
    (js/clearInterval t)
    (reset! timer-id nil)))

(defn seed-from-data!
  "Populate replay with every session projected onto a single global playhead.

   Accepts JSON-decoded data with keyword keys (cljs-http default)."
  [data]
  (let [sessions (get-in data [:sessions :sessions] [])
        vmode @s/view-mode
        join-ctx (when (= vmode :aif-stack) (aif/join-context))
        initial-playhead (when-let [{:keys [earliest-ms latest-ms]}
                                    (wf/active-bounds data @s/waveform)]
                           (or (first-step-ms-in-bounds data earliest-ms latest-ms
                                                        vmode join-ctx)
                               earliest-ms))]
    (reset! s/replay
            (into {:playing? true
                   :playhead-ms initial-playhead}
                  (map-indexed
                   (fn [i session]
                     (let [sid (:session-id session)
                           steps (vec (:steps session []))]
                       [sid {:steps steps
                             :step-idx nil
                             :color-idx i
                             :session-id sid}]))
                   sessions)))
    (when initial-playhead
      (sync-replay-to-playhead! initial-playhead data true))))
