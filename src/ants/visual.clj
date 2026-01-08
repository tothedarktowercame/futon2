(ns ants.visual
  "Swing-based visualizer for the AIF vs classic war simulation."
  (:require [ants.ui :as ui]
            [ants.war :as war]
            [clojure.string :as str])
  (:import [javax.swing JFrame JPanel JLabel SwingUtilities]
           [java.awt Color Dimension Graphics BorderLayout]
           [java.awt.event WindowAdapter]
           [java.util.concurrent Executors TimeUnit]))

(defn- parse-long
  [s]
  (try
    (Long/parseLong s)
    (catch Exception _ nil)))

(defn- choose-scale
  [[w h]]
  (let [max-side (max w h)]
    (cond
      (<= max-side 30) 18
      (<= max-side 60) 10
      :else 6)))

(defn- clamp
  [x lo hi]
  (-> x (max lo) (min hi)))

(defn- species-color [species]
  (case species
    :classic (Color. 60 60 60)
    :aif (Color. 200 40 60)
    :cyber (Color. 70 140 220)
    (Color. 100 100 100)))

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

(defn- draw-cell
  [^Graphics g world scale x y]
  (let [cell (get-in world [:grid :cells [x y]] {})
        base-x (* x scale)
        base-y (* y scale)
        home (:home cell)
        max-food (double (or (get-in world [:grid :max-food]) 1.0))
        max-pher (double (or (get-in world [:grid :max-pher]) 1.0))
        food (double (or (:food cell) 0.0))
        pher (double (or (:pher cell) 0.0))
        ant-id (:ant cell)
        ant (get-in world [:ants ant-id])]
    (.setColor g (Color. 245 245 245))
    (.fillRect g base-x base-y scale scale)
    (when home
      (.setColor g (case home
                     :aif (Color. 255 236 236)
                     :cyber (Color. 236 247 255)
                     (Color. 236 240 255)))
      (.fillRect g base-x base-y scale scale))
    (when (> pher 0.0)
      (let [alpha (int (clamp (* 255.0 (clamp (/ pher (max max-pher 1e-3)) 0.0 1.0)) 10 160))]
        (.setColor g (Color. 0 180 0 alpha))
        (.fillRect g base-x base-y scale scale)))
    (when (> food 0.0)
      (let [alpha (int (clamp (* 255.0 (clamp (/ food (max max-food 1e-3)) 0.0 1.0)) 40 220))]
        (.setColor g (Color. 255 120 0 alpha))
        (.fillRect g base-x base-y scale scale)))
    (when ant
      (let [species (:species ant)
            color (case species
                    :aif (Color. 210 40 60)
                    :cyber (Color. 70 140 220)
                    (Color. 40 40 40))
            inset (max 1 (int (/ scale 4)))
            size (max 2 (- scale (* 2 inset)))]
        (.setColor g color)
        (.fillOval g (+ base-x inset) (+ base-y inset) size size)
        (when (> (double (or (:cargo ant) 0.0)) 0.0)
          (.setColor g (Color. 255 255 0 200))
          (.fillOval g (+ base-x inset) (+ base-y inset) size size))))
    (when-let [graves (seq (:graves cell))]
      (let [pad (max 1 (int (/ scale 6)))
            x1 (+ base-x pad)
            y1 (+ base-y pad)
            x2 (- (+ base-x scale) pad 1)
            y2 (- (+ base-y scale) pad 1)]
        (doseq [grave graves]
          (let [species (:species grave)
                color (case species
                        :aif (Color. 200 60 80 220)
                        :cyber (Color. 80 160 230 220)
                        (Color. 60 60 60 220))]
            (.setColor g color)
            (.drawLine g x1 y1 x2 y2)
            (.drawLine g x1 y2 x2 y1)
            (.drawLine g (inc x1) y1 (dec x2) y2)
            (.drawLine g (inc x1) y2 (dec x2) y1)
            (.setColor g (Color. 255 255 255 40))
            (.drawLine g x1 (inc y1) x2 (inc y2))
            (.drawLine g x1 (dec y2) x2 (dec y1))))))))

(defn- render-world
  [^Graphics g world scale]
  (let [[w h] (get-in world [:grid :size] [0 0])]
    (dotimes [y h]
      (dotimes [x w]
        (draw-cell g world scale x y)))))

(defn- make-panel
  [world-atom scale]
  (proxy [JPanel] []
    (paintComponent [^Graphics g]
      (proxy-super paintComponent g)
      (when-let [world @world-atom]
        (render-world g world scale)))))

(defn- hunger-color
  "Return a color representing hunger level h (0.0 good → 1.0 bad)."
  [h]
  (let [h (clamp h 0.0 1.0)
        r (int (clamp (* 255.0 h) 0.0 255.0))
        g (int (clamp (* 255.0 (- 1.0 h)) 0.0 255.0))]
    (Color. r g 60)))

(defn- make-stats-panel
  [world-atom scale grid-height]
  (let [width 170
        height (* scale grid-height)
        preferred (Dimension. width height)
        panel (proxy [JPanel] []
                (getPreferredSize [] preferred)
                (paintComponent [^Graphics g]
                  (proxy-super paintComponent g)
                  (let [w (.getWidth this)
                        h (.getHeight this)
                        padding 10
                        bar-width (- w (* 2 padding))
                        bar-height 12
                        line-gap 6
                        row-gap 18
                        y-start padding]
                    (.setColor g (Color. 250 250 252))
                    (.fillRect g 0 0 w h)
                    (when-let [world @world-atom]
                      (let [queen-initial (double (or (get-in world [:config :hunger :queen :initial]) 1.0))
                            species-info (or (:armies world) [:classic :aif])]
                        (loop [[species & more] species-info
                               y y-start]
                          (when species
                            (let [label (species-label species)
                                  color (species-color species)
                                  reserves (double (or (get-in world [:colonies species :reserves]) 0.0))
                                  starved (int (or (get-in world [:colonies species :starved-ticks]) 0))
                                  ratio (if (pos? queen-initial)
                                          (clamp (/ reserves queen-initial) 0.0 1.0)
                                          0.0)
                                  fill-width (int (* bar-width ratio))
                                  label-y (+ y 11)]
                              (.setColor g (Color. 40 40 40))
                              (.drawString g (str label " queen") padding label-y)
                              (let [bar-y (+ y 14)]
                                (.setColor g (Color. 230 230 230))
                                (.fillRect g padding bar-y bar-width bar-height)
                                (.setColor g color)
                                (.fillRect g padding bar-y fill-width bar-height)
                                (.setColor g (Color. 120 120 120))
                                (.drawRect g padding bar-y (dec bar-width) (dec bar-height))
                                (.setColor g (Color. 60 60 60))
                                (.drawString g (format "%.2f" reserves)
                                             padding
                                             (+ bar-y bar-height 14))
                                (.drawString g (str "starve " starved)
                                             (+ padding 90)
                                             (+ bar-y bar-height 14))
                                (let [ants (->> (:ants world)
                                                vals
                                                (filter #(= (:species %) species))
                                                (sort-by :id))
                                      ants-start (+ bar-y bar-height 28)
                                      next-y (+ ants-start
                                                (* (max 0 (dec (count ants)))
                                                   (+ bar-height line-gap))
                                                50)]
                                  (.setColor g (Color. 40 40 40))
                                  (.drawString g (str label " ants") padding (+ ants-start -4))
                                  (loop [xs ants
                                         idx 0
                                         y-pos (+ ants-start 4)]
                                    (when-let [ant (first xs)]
                                      (let [hunger (double (or (:h ant) 0.0))
                                            ingest (double (or (:ingest ant) 0.0))
                                            bar-y2 (+ y-pos (* idx (+ bar-height line-gap)))
                                            fill (int (* bar-width hunger))
                                            color-h (hunger-color hunger)
                                            id-text (name (:id ant))]
                                        (.setColor g (Color. 80 80 80))
                                        (.drawString g id-text padding (+ bar-y2 10))
                                        (.setColor g (Color. 230 230 230))
                                        (.fillRect g padding (+ bar-y2 12) bar-width bar-height)
                                        (.setColor g color-h)
                                        (.fillRect g padding (+ bar-y2 12) fill bar-height)
                                        (.setColor g (Color. 120 120 120))
                                        (.drawRect g padding (+ bar-y2 12) (dec bar-width) (dec bar-height))
                                        (.setColor g (Color. 60 60 60))
                                        (.drawString g (format "h=%.2f ing=%.2f" hunger ingest)
                                                     padding
                                                     (+ bar-y2 28))
                                        (recur (rest xs) (inc idx) y-pos))))
                                  (recur more next-y)))))))))))]
    (.setPreferredSize panel preferred)
    panel))

(defn- update-ui!
  [panel label stats-panel world]
  (when label
    (.setText label (ui/scoreboard world)))
  (when panel
    (.repaint panel))
  (when stats-panel
    (.repaint stats-panel)))

(defn visualize
  "Launch an interactive Swing viewer for the war simulation.

  Accepts an optional `config` map merged onto `war/default-config` and an
  options map with:
  - :delay-ms   millis between simulation steps (default 75)
  - :scale      pixels per cell (auto-chosen from world size when nil)
  - :blocking?  block until simulation completes (default true)
  - :log?       print scoreboard info to STDOUT while stepping (default false)
  - :log-every  log once every N ticks (default 1)
  - :log-fn     function applied to each world prior to logging (default scoreboard)
  Returns the final world when blocking, or {:frame … :world … :done …} when
  non-blocking."
  ([config]
   (visualize config {}))
  ([config {:keys [delay-ms scale blocking? log? log-every log-fn]
            :or {delay-ms 75
                 blocking? true
                 log? false
                 log-every 1
                 log-fn ui/scoreboard}}]
   (let [merged (merge war/default-config config)
         total (:ticks merged)
         base-world (war/new-world merged)
         world-atom (atom base-world)
         scale (or scale (choose-scale (:size (:grid base-world))))
         panel-ref (atom nil)
         label-ref (atom nil)
         stats-ref (atom nil)
         frame-ref (atom nil)
         running (atom true)
         finished (promise)
         step-delay (long (max 10 delay-ms))
         executor (Executors/newSingleThreadScheduledExecutor)
         log-every (max 1 (long log-every))
         log-fn (or log-fn ui/scoreboard)]
     (SwingUtilities/invokeAndWait
      (fn []
        (let [[w h] (get-in base-world [:grid :size])
              panel (doto (make-panel world-atom scale)
                      (.setPreferredSize (Dimension. (* scale w) (* scale h))))
              label (JLabel. (ui/scoreboard base-world))
              stats (make-stats-panel world-atom scale h)
              frame (doto (JFrame. "Ant War: Active Inference")
                      (.setLayout (BorderLayout.))
                      (.add panel BorderLayout/CENTER)
                      (.add label BorderLayout/SOUTH)
                      (.add stats BorderLayout/EAST)
                      (.pack)
                      (.setVisible true))]
          (.addWindowListener frame
                              (proxy [WindowAdapter] []
                                (windowClosing [_]
                                  (reset! running false)
                                  (when (not (realized? finished))
                                    (deliver finished @world-atom)))))
          (reset! panel-ref panel)
          (reset! label-ref label)
          (reset! stats-ref stats)
          (reset! frame-ref frame))))
     (.scheduleAtFixedRate executor
                           (fn []
                             (try
                               (let [world @world-atom
                                     tick (:tick world)]
                                 (cond
                                   (not @running)
                                   (do
                                     (.shutdown executor)
                                     (when (not (realized? finished))
                                       (deliver finished world)))

                                   (:terminated? world)
                                   (do
                                     (reset! running false)
                                     (.shutdown executor)
                                     (SwingUtilities/invokeLater
                                      #(update-ui! @panel-ref @label-ref @stats-ref world))
                                     (when (not (realized? finished))
                                       (deliver finished world))
                                     (when-let [msg (war/termination-message world)]
                                       (println msg)))

                                   (>= tick total)
                                   (do
                                     (reset! running false)
                                     (.shutdown executor)
                                     (SwingUtilities/invokeLater
                                      #(update-ui! @panel-ref @label-ref @stats-ref world))
                                     (when (not (realized? finished))
                                       (deliver finished world)))

                                   :else
                                   (let [world' (swap! world-atom war/step)]
                                     (when (and log?
                                                (zero? (mod (:tick world') log-every)))
                                       (println (log-fn world')))
                                     (SwingUtilities/invokeLater
                                      #(update-ui! @panel-ref @label-ref @stats-ref world'))
                                     (when (:terminated? world')
                                       (reset! running false)
                                       (.shutdown executor)
                                       (when (not (realized? finished))
                                         (deliver finished world'))
                                       (when-let [msg (war/termination-message world')]
                                         (println msg))))))
                               (catch Exception e
                                 (.printStackTrace e)
                                 (reset! running false)
                                 (.shutdown executor)
                                 (when (not (realized? finished))
                                   (deliver finished @world-atom)))))
                           step-delay
                           step-delay
                           TimeUnit/MILLISECONDS)
     (if blocking?
       (let [world @finished]
         (SwingUtilities/invokeLater
          #(update-ui! @panel-ref @label-ref @stats-ref world))
         world)
       {:frame @frame-ref
        :world world-atom
        :done finished}))))

(defn -main
  [& args]
  (let [opts (->> args (filter #(str/starts-with? % "--")) set)
        positional (remove #(str/starts-with? % "--") args)
        log-every (some-> positional first parse-long)
        log-every (if (and log-every (pos? log-every)) log-every 5)
        config (merge (cond-> {}
                        (contains? opts "--no-termination")
                        (assoc :enable-termination? false))
                      {{:logging {:alice? true :alice-id 5}}
                       {:log? true :log-every 1 :blocking? true}})]
    (visualize config {:blocking? true
                       :log? true
                       :log-every log-every})
    nil))
