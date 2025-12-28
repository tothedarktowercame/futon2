(ns ants.ui
  "Minimal textual HUD utilities for the CLI demo and visualization overlays."
  (:require [clojure.string :as str]))

(defn- fmt-double
  [x]
  (format "%.2f" (double (or x 0.0))))

(defn- dist
  [[ax ay] [bx by]]
  (when (and ax ay bx by)
    (Math/sqrt (double (+ (Math/pow (- (double ax) (double bx)) 2.0)
                          (Math/pow (- (double ay) (double by)) 2.0))))))

(defn- summarize-event
  [{:keys [id species action gather deposit cargo loc target wander moved G h tau risk ambiguity dhdt]}]
  (when id
    (str (name id) " " (name action)
         (when wander "*wander")
         (when moved "*move")
         (when (some? gather)
           (format " gather=%.2f" (double gather)))
         (when (some? deposit)
           (format " dep=%.2f" (double deposit)))
         (when (some? cargo)
           (format " cargo=%.2f" (double cargo)))

         (when target
           (format " ->%s" (pr-str target)))
         (when loc
           (format " @%s" (pr-str loc)))
         (when (number? G)
           (format " (G %.2f)" G))
         (when (= species :aif)
           (str (format " h=%.2f tau=%.2f" (double h) (double tau))
                (when (number? risk)
                  (format " risk=%.2f" (double risk)))
                (when (number? ambiguity)
                  (format " amb=%.2f" (double ambiguity)))
                (when (number? dhdt)
                  (format " Δh=%+.2f" (double dhdt)))))
;; (when (some? (:white-streak %))
;;            (format " ws=%d" (int (:white-streak %))))
;;          (when (some? (:since-ingest %))
;;            (format " si=%d" (int (:since-ingest %))))
         )))

(defn- last-by-species
  [events species]
  (some (fn [event]
          (when (= species (:species event))
            (summarize-event event)))
        (reverse events)))

(defn- species-label [species]
  (case species
    :classic "Classic"
    :aif "AIF"
    :cyber "Cyber"
    (str/capitalize (name species))))

(defn- species-short [species]
  (case species
    :classic "C"
    :aif "A"
    :cyber "Z"
    (str (first (species-label species)))))

(defn scoreboard
  "Return a single-line HUD string covering tick, scores, rolling G EMA, and
  the most recent action for each species."
  [world]
  (let [tick (:tick world 0)
        armies (or (:armies world) [:classic :aif])
        scores (:scores world)
        ants-by-species (group-by :species (vals (:ants world)))
        white-thresh 0.05
        grid (:grid world)
        homes (:homes world)
        avg-dist (fn [species]
                   (let [home (get homes species)
                         locs (keep :loc (get ants-by-species species))
                         distances (keep #(dist home %) locs)]
                     (when (seq distances)
                       (/ (reduce + distances) (count distances)))))
        white-count (fn [species]
                      (count (for [ant (get ants-by-species species)
                                   :let [food (double (or (get-in grid [:cells (:loc ant) :food]) 0.0))]
                                   :when (<= food white-thresh)]
                               ant)))
        reserve-of (fn [species]
                     (double (or (get-in world [:colonies species :reserves]) 0.0)))
        g (get-in world [:rolling :G])
        ema (if (some? g) (format "%.3f" (double g)) "n/a")
        events (:last-events world)
        action-summary (->> armies
                            (map (fn [species]
                                   (str (species-label species) " "
                                        (or (last-by-species events species) "—"))))
                            (str/join " | "))
        termination (when-let [{:keys [reason species]} (:termination world)]
                      (str (name reason)
                           (when species
                             (str "(" (name species) ")"))))]
    (let [score-section (->> armies
                             (map (fn [species]
                                    (format "%s %s"
                                            (species-label species)
                                            (fmt-double (get scores species 0.0)))))
                             (str/join " vs "))
          reserve-section (->> armies
                               (map (fn [species]
                                      (format "%s:%s"
                                              (species-short species)
                                              (fmt-double (reserve-of species)))))
                               (str/join " "))
          pop-section (->> armies
                           (map (fn [species]
                                  (format "%s:%02d"
                                          (species-short species)
                                          (count (get ants-by-species species)))))
                           (str/join " "))
          white-section (->> armies
                             (map (fn [species]
                                    (format "%s:%02d"
                                            (species-short species)
                                            (white-count species))))
                             (str/join " "))
          dist-section (->> armies
                            (map (fn [species]
                                   (format "%s:%s"
                                           (species-short species)
                                           (fmt-double (avg-dist species)))))
                            (str/join " "))]
      (format "Tick %3d | Scores %s | G_ema %s | Hive %s | Pop %s | White %s | Dist %s%s%s"
              tick
              score-section
              ema
              reserve-section
              pop-section
              white-section
              dist-section
              (if (seq action-summary)
                (str " | " action-summary)
                "")
              (if termination
                (str " | END " termination)
                "")))))

(defn- queen-status-line
  [world]
  (let [armies (or (:armies world) [:classic :aif])
        fmt (fn [species]
              (let [reserves (double (or (get-in world [:colonies species :reserves]) 0.0))
                    starved (int (or (get-in world [:colonies species :starved-ticks]) 0))]
                (format "%s %s (starve %d)"
                        (species-short species)
                        (fmt-double reserves)
                        starved)))]
    (str "Queen " (str/join " | " (map fmt armies)))))

(defn- ant-health-line
  [world]
  (let [crit-thresh 0.85
        armies (or (:armies world) [:classic :aif])
        fmt (fn [species]
              (let [ants (filter #(= (:species %) species) (vals (:ants world)))
                    cnt (max 1 (count ants))
                    hs (map #(double (or (:h %) 0.0)) ants)
                    ingest (map #(double (or (:ingest %) 0.0)) ants)
                    avg-h (/ (reduce + 0.0 hs) cnt)
                    min-h (if (seq hs) (apply min hs) 0.0)
                    avg-ingest (/ (reduce + 0.0 ingest) cnt)
                    crit (count (filter #(>= % crit-thresh) hs))
                    alive (count ants)]
                (format "%s h %s/%s ingest %s crit %d/%d"
                        (species-short species)
                        (fmt-double avg-h)
                        (fmt-double min-h)
                        (fmt-double avg-ingest)
                        crit
                        alive)))]
    (str "Ants " (str/join " | " (map fmt armies)))))

(defn visual-summary
  "Compose an overlay string suitable for the Swing visualization label.

  Returns a multi-line HTML string with the scoreboard plus queen and ant
  health summaries."
  [world]
  (let [board (scoreboard world)
        queen-line (queen-status-line world)
        ant-line (ant-health-line world)
        lines (remove str/blank? [board queen-line ant-line])]
    (if (> (count lines) 1)
      (str "<html>" (str/join "<br/>" lines) "</html>")
      board)))

(defn hud
  "Return a multi-line HUD with scoreboard and last events per tick."
  [world]
  (let [headline (scoreboard world)
        events (->> (:last-events world)
                    (keep summarize-event)
                    (take-last 5))]
    (str headline
         (when (seq events)
           (str "\n" (str/join "\n" (map #(str "  • " %) events)))))))
