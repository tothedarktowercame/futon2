(ns war-machine.client.sprites
  "SVG sprite renderers for hex nodes. Ports draw-hex-sprite from
   war_machine_visual.clj. Each returns Reagent hiccup for a <g> subtree.

   Sprite types (keyword keys, cljs-http default): :repo, :sorry, :tick,
   :mission, :pattern-collection, :invariant-family, :invariant-candidate,
   :invariant-layer.

   Sprite-type values arrive from the server as strings (e.g. \"repo\") when
   assigned in assign-*-layout — hex.cljs assigns keyword forms, so the case
   below matches keywords."
  (:require [war-machine.client.hex :as hex]
            [clojure.string :as str]))

(defn- workstream-color [ws]
  (case ws
    "stack"       "#4a90e2"
    "mathematics" "#22c55e"
    "portfolio"   "#eab308"
    "consulting"  "#ef4444"
    :stack        "#4a90e2"
    :mathematics  "#22c55e"
    :portfolio    "#eab308"
    :consulting   "#ef4444"
    "#9ca3af"))

(defn- severity-color [sev]
  (case (some-> sev name)
    "critical"    "#b91c1c"
    "high"        "#dc2626"
    "warning"     "#d97706"
    "medium"      "#ea580c"
    "info"        "#3b82f6"
    "low"         "#60a5fa"
    ;; Invariant-tier hues (used by invariants-as-cells via the sorry sprite).
    "operational" "#15803d"
    "candidate"   "#a78bfa"
    "#6b7280"))

(defn- mission-status-color [status]
  (case (str status)
    "blocked"                               "#ef4444"
    ("active" "in-progress" "in_progress")  "#3b82f6"
    "testing"                               "#a855f7"
    "ready"                                 "#22c55e"
    "complete"                              "#b4d2b4"
    "deferred"                              "#c8b48c"
    "#c8c8d2"))

(defn- tick-color [fired?]
  (if fired? "#f97316" "#9ca3af"))

(defn- activity-alpha [commits max-commits]
  (if (and max-commits (pos? max-commits))
    (let [r (/ commits (double max-commits))]
      (+ 0.3 (* 0.7 (min 1.0 r))))
    0.3))

(defn- glow-ring [points glow]
  (let [g (max 0.0 (min 1.0 (double (or glow 0.0))))
        w (+ 2.0 (* 5.0 g))
        a (* 0.9 g)]
    [:polygon {:points points
               :fill "none"
               :stroke "#ffd866"
               :stroke-width w
               :stroke-opacity a
               :pointer-events "none"}]))

(declare wrap-label)

(defn- repo-sprite [node cx cy size points glow]
  (let [ws       (:workstream node)
        commits  (or (:commits node) 0)
        active?  (:active? node)
        ;; phase-color (set by missions-as-cells) wins over workstream-color
        base     (or (:phase-color node) (workstream-color ws))
        alpha    (if active? (activity-alpha commits 50) 0.25)
        ;; blocked? gets a vivid red border so it stands out from the phase fill
        blocked? (:blocked? node)
        border   (cond blocked?  "#dc2626"
                       active?   "#3c3c3c"
                       :else     "#b4b4b4")
        b-width  (cond blocked? 3.0  active? 2.0  :else 1.0)
        raw-label (or (:label node) "?")
        max-line  (max 6 (int (* size 0.34)))
        lines     (wrap-label raw-label max-line)
        font-sz   (max 9 (int (* size 0.22)))
        lh        (int (* font-sz 1.05))
        n         (count lines)
        y-start   (- cy (* lh (/ (- n 1) 2.0)))]
    [:g
     [:polygon {:points points
                :fill base :fill-opacity alpha
                :stroke border :stroke-width b-width}]
     (for [[i line] (map-indexed vector lines)]
       ^{:key (str "rl" i)}
       [:text {:x cx :y (+ y-start (* i lh) (quot font-sz 3))
               :text-anchor "middle"
               :font-family "SansSerif"
               :font-size font-sz
               :fill "#1e1e1e"}
        line])
     (when (and active? (pos? commits))
       [:text {:x cx :y (+ cy (* size 0.45))
               :text-anchor "middle"
               :font-family "SansSerif"
               :font-weight "bold"
               :font-size (max 8 (int (* size 0.22)))
               :fill "#505050"}
        (str commits)])
     (when (pos? (double (or glow 0.0))) (glow-ring points glow))]))

(defn- wrap-label
  "Split a label into ≤2 short lines.  Prefer breaking at '-', '_' or
   '.'; fall back to a midpoint split when no boundary fits."
  [s max-line]
  (let [s (str s)]
    (cond
      (<= (count s) max-line) [s]
      :else
      (let [break-idx (->> (range (count s))
                           (filter #(#{\- \_ \. \space} (.charAt s %)))
                           (filter #(<= % max-line))
                           last)
            cut (or break-idx (min max-line (quot (count s) 2)))
            head (subs s 0 cut)
            tail (subs s (if (and break-idx (#{\- \_ \. \space} (.charAt s break-idx)))
                           (inc break-idx) cut))]
        [head (if (> (count tail) max-line)
                (str (subs tail 0 (- max-line 1)) "…")
                tail)]))))

(defn- sorry-sprite [node cx cy size points glow]
  (let [sev    (:severity node)
        base   (severity-color sev)
        queue-rank (:queue-rank node)
        ;; Prefer the truncated :label set upstream (e.g. invariants-as-cells)
        ;; over reconstructing one from raw-id — otherwise the sprite ignores
        ;; whatever fitting work the layout did.
        raw    (or (:label node)
                   (let [rid (or (:id node) "?")]
                     (-> (str (if (keyword? rid) (name rid) rid))
                         (str/replace #"^SORRY-" "")
                         (str/replace #"^sorry-" ""))))
        ;; Wrap into ≤2 lines so long ids don't bleed into neighbours.
        max-line (max 6 (int (* size 0.30)))
        lines    (wrap-label raw max-line)
        font-sz  (max 9 (int (* size 0.22)))
        ;; Line height is the font size plus small leading.
        lh       (int (* font-sz 1.05))
        n        (count lines)
        ;; Vertically centre the block within the cell.
        y-start  (- cy (* lh (/ (- n 1) 2.0)))]
    [:g
     [:polygon {:points points
                :fill base :fill-opacity 0.78
                :stroke "#780000" :stroke-width 2.5}]
     ;; Render each line as its own <text> so we don't need <tspan> arithmetic.
     ;; Dark text on light/saturated severity fills reads better than the prior
     ;; white-on-light combo that disappeared on info/low cells.
     (for [[i line] (map-indexed vector lines)]
       ^{:key (str "sl" i)}
       [:text {:x cx :y (+ y-start (* i lh) (quot font-sz 3))
               :text-anchor "middle"
               :font-family "SansSerif"
               :font-weight "600"
               :font-size font-sz
               :fill "#1e1e1e"}
        line])
     (when queue-rank
       [:text {:x cx :y (+ cy (* size 0.46))
               :text-anchor "middle"
               :font-family "SansSerif"
               :font-size (max 7 (int (* size 0.17)))
               :font-weight "bold"
               :fill "#3f3f46"}
        (str "#" queue-rank)])
     (when (pos? (double (or glow 0.0))) (glow-ring points glow))]))

(defn- tick-sprite [node cx cy size points glow]
  (let [fired? (:fired? node)
        base   (tick-color fired?)
        raw-id (or (:id node) "?")
        label  (-> (str (if (keyword? raw-id) (name raw-id) raw-id))
                   (str/replace "-warning" "!")
                   (str/replace "-" " "))]
    [:g
     [:polygon {:points points
                :fill base :fill-opacity (if fired? 0.9 0.3)
                :stroke (if fired? "#c83200" "#a0a0a0")
                :stroke-width (if fired? 3.0 1.0)}]
     [:text {:x cx :y (+ cy 4)
             :text-anchor "middle"
             :font-family "SansSerif"
             :font-weight "bold"
             :font-size (max 7 (int (* size 0.22)))
             :fill (if fired? "#ffffff" "#505050")}
      label]
     (when (pos? (double (or glow 0.0))) (glow-ring points glow))]))

(defn- default-sprite [_node _cx _cy _size points glow]
  [:g
   [:polygon {:points points
              :fill "#c8c8c8" :fill-opacity 0.4
              :stroke "#969696" :stroke-width 1.0}]
   (when (pos? (double (or glow 0.0))) (glow-ring points glow))])

(defn draw-sprite
  "Top-level sprite dispatcher. Returns Reagent hiccup <g> for the given node."
  [node cx cy size glow]
  (let [points (hex/hex-points cx cy size)
        sprite-type (:sprite-type node)]
    (case sprite-type
      :repo  (repo-sprite  node cx cy size points glow)
      :sorry (sorry-sprite node cx cy size points glow)
      :tick  (tick-sprite  node cx cy size points glow)
      (default-sprite node cx cy size points glow))))

(def ant-colors
  ["#ff5000" "#00b4ff" "#c832c8" "#32c832"
   "#ffc800" "#ff3264" "#64c8c8" "#c89632"])

(defn session-ant
  "Render a session-ant as a bright dot with a fading trail."
  [current-positions trail cell-map hex-size color-idx]
  (let [ant-color (nth ant-colors (mod (or color-idx 0) (count ant-colors)))]
    [:g {:pointer-events "none"}
     (for [{:keys [ids age]} trail
           id ids
           :let [cell (get cell-map id)]
           :when cell
           :let [[cx cy] (hex/hex->pixel (:q cell) (:r cell) hex-size)
                 alpha (max 0.08 (- 0.6 (* age 0.08)))
                 radius (* hex-size 0.25)]]
       ^{:key (str "trail-" id "-" age)}
       [:circle {:cx cx :cy cy :r radius
                 :fill ant-color :fill-opacity alpha}])
     (for [id current-positions
           :let [cell (get cell-map id)]
           :when cell
           :let [[cx cy] (hex/hex->pixel (:q cell) (:r cell) hex-size)
                 radius (* hex-size 0.35)]]
       ^{:key (str "ant-" id)}
       [:g
        [:circle {:cx cx :cy cy :r (+ radius 3)
                  :fill ant-color :fill-opacity 0.4}]
        [:circle {:cx cx :cy cy :r radius
                  :fill ant-color}]
        [:circle {:cx cx :cy cy :r (* radius 0.4)
                  :fill "#fff8c8"}]])]))
