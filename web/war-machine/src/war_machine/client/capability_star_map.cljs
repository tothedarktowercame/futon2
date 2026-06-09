(ns war-machine.client.capability-star-map
  "Capability star-map renderer for the War Machine UI."
  (:require [clojure.string :as str]
            [war-machine.client.state :as s]))

(defn- id-text [x]
  (cond
    (keyword? x) (name x)
    (string? x) x
    (nil? x) ""
    :else (str x)))

(def ^:private styles
  ".capability-map-view{max-width:1280px;margin:0 auto;display:flex;flex-direction:column;gap:12px;min-height:100%}
.capability-map-header{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;background:rgba(255,255,255,.94);border:1px solid #cbd5e1;border-radius:6px;padding:12px 14px;box-shadow:0 8px 20px rgba(15,23,42,.06)}
.capability-map-header h2{margin:0;font-size:20px;color:#0f172a}.capability-map-subtitle{margin-top:4px;color:#64748b;font-size:12px;font-family:monospace}
.cap-progress{min-width:280px;display:flex;flex-direction:column;gap:6px;font-size:11px;color:#334155;font-family:monospace}.cap-progress-top{display:flex;gap:10px;justify-content:flex-end;flex-wrap:wrap}
.cap-progress-bar{height:8px;background:#e2e8f0;border:1px solid #cbd5e1;border-radius:999px;overflow:hidden}.cap-progress-fill{height:100%;background:linear-gradient(90deg,#16a34a,#22c55e)}
.capability-map-body{display:grid;grid-template-columns:minmax(0,1fr) 300px;gap:12px;min-height:560px}.capability-map-svg,.cap-detail-panel,.capability-map-empty{background:rgba(255,255,255,.96);border:1px solid #cbd5e1;border-radius:6px;box-shadow:0 8px 22px rgba(15,23,42,.06)}
.capability-map-svg{width:100%;min-height:560px;display:block}.cap-edge{stroke:#94a3b8;stroke-width:1.8;stroke-opacity:.52}.cap-depth-line{stroke:#e2e8f0;stroke-width:1}.cap-depth-label{fill:#94a3b8;font-size:11px;font-family:monospace}
.cap-node-ring{fill:rgba(255,255,255,.9);stroke:#e2e8f0;stroke-width:2}.cap-node-selected .cap-node-ring{stroke:#0f172a;stroke-width:3}.cap-node-dot{stroke:#fff;stroke-width:2}.cap-node-witness{fill:none;stroke:#facc15;stroke-width:2;stroke-dasharray:3 3}
.cap-node-label{fill:#334155;font-size:10px;font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;pointer-events:none}.cap-detail-panel{padding:14px;overflow:auto}.cap-detail-panel h3{margin:4px 0 6px;font-size:15px;line-height:1.3;color:#0f172a}
.cap-detail-kicker,.cap-detail-label,.cap-detail-id{font-size:10px;font-family:monospace;text-transform:uppercase;color:#64748b;letter-spacing:.4px}.cap-detail-id{text-transform:none;letter-spacing:0;margin-bottom:10px;color:#475569}.cap-detail-section{margin-top:12px;display:flex;flex-direction:column;gap:6px}.cap-detail-text{font-size:12px;line-height:1.45;color:#334155}.cap-chip-row{display:flex;flex-wrap:wrap;gap:6px}.cap-chip{font-size:10px;font-family:monospace;color:#0f172a;background:#e0f2fe;border:1px solid #bae6fd;border-radius:999px;padding:3px 7px}.cap-chip-gate{background:#fef3c7;border-color:#fde68a}.capability-map-empty{padding:18px;color:#64748b;font-size:13px}
@media(max-width:900px){body{overflow:auto}.war-machine{min-height:100vh;height:auto}.toolbar{flex-wrap:wrap}.main-area{flex-direction:column;overflow:visible}.legend-panel,.sidebar{width:100%;min-width:0;max-height:none;border-left:0}.legend-panel{order:2}.hex-canvas{order:1;width:100%;min-height:640px;overflow:visible}.sidebar{order:3;border-top:1px solid var(--border)}.capability-map-header,.capability-map-body{grid-template-columns:1fr}.capability-map-header{flex-direction:column}.cap-progress{min-width:0;width:100%}.cap-progress-top{justify-content:flex-start}}")

(defn- style-node []
  [:style styles])

(defn- status-text [x]
  (id-text x))

(defn- short-label [s n]
  (let [s (str s)]
    (if (<= (count s) n)
      s
      (str (subs s 0 (max 1 (- n 1))) "..."))))

(defn- title-label [id node]
  (or (:title node) (:label node) (id-text id)))

(defn- normalize-capabilities [graph]
  (let [caps (:capabilities graph)]
    (->> caps
         (map (fn [[id node]]
                (let [sid (id-text id)]
                  [sid (-> node
                           (assoc :id sid)
                           (update :scope #(mapv id-text (or % [])))
                           (update :minted-by #(mapv id-text (or % []))))])))
         (into {}))))

(declare cap-depth)

(defn- cap-depth* [caps id seen]
  (let [scope (:scope (get caps id))]
    (cond
      (contains? seen id) 0
      (empty? scope) 0
      :else (inc (apply max (map #(cap-depth caps % (conj seen id)) scope))))))

(defn- cap-depth
  ([caps id] (cap-depth caps id #{}))
  ([caps id seen]
   (cap-depth* caps id seen)))

(defn- layout [caps width height]
  (let [ids (sort (keys caps))
        with-depth (mapv (fn [id] [id (cap-depth caps id)]) ids)
        groups (->> with-depth
                    (group-by second)
                    (map (fn [[depth rows]]
                           [depth (sort-by first rows)]))
                    (into (sorted-map)))
        max-depth (max 1 (apply max 0 (keys groups)))
        left 80
        right 80
        top 64
        bottom 72
        usable-w (max 1 (- width left right))
        x-for (fn [depth] (+ left (* usable-w (/ depth max-depth))))
        positioned
        (mapcat
         (fn [[depth rows]]
           (let [n (count rows)
                 usable-h (max 1 (- height top bottom))
                 gap (/ usable-h (inc n))]
             (map-indexed
              (fn [idx [id _]]
                [id {:x (x-for depth)
                     :y (+ top (* gap (inc idx)))
                     :depth depth}])
              rows)))
         groups)]
    (into {} positioned)))

(defn- node-color [node]
  (let [status (status-text (:status node))]
    (cond
      (= status "satisfied") "#16a34a"
      (:keystone node) "#7c3aed"
      (:frontier node) "#2563eb"
      (= status "held") "#d97706"
      :else "#64748b")))

(defn- node-class [node]
  (str "cap-node "
       (case (status-text (:status node))
         "satisfied" "cap-node-satisfied"
         "held" "cap-node-held"
         "cap-node-unknown")
       (when (:frontier node) " cap-node-frontier")
       (when (:keystone node) " cap-node-keystone")
       (when (seq (:pre-witness node)) " cap-node-pre-witness")))

(defn- progress [caps]
  (let [nodes (vals caps)
        total (count nodes)
        satisfied (count (filter #(= "satisfied" (status-text (:status %))) nodes))
        held (count (filter #(= "held" (status-text (:status %))) nodes))
        frontier (count (filter :frontier nodes))
        pct (if (pos? total) (/ satisfied total) 0)]
    {:total total
     :satisfied satisfied
     :held held
     :frontier frontier
     :pct pct}))

(defn- max-depth-count [caps]
  (->> (keys caps)
       (map #(cap-depth caps %))
       frequencies
       vals
       (apply max 1)))

(defn- selected-panel [node]
  [:div.cap-detail-panel
   (if node
     [:<>
      [:div.cap-detail-kicker (str (status-text (:status node))
                                   (when (:frontier node) " / frontier")
                                   (when (:keystone node) " / keystone"))]
      [:h3 (title-label (:id node) node)]
      [:div.cap-detail-id (:id node)]
      (when (seq (:scope node))
        [:div.cap-detail-section
         [:div.cap-detail-label "Requires"]
         (into [:div.cap-chip-row]
               (map (fn [id] [:span.cap-chip {:key id} id]) (:scope node)))])
      (when (seq (:minted-by node))
        [:div.cap-detail-section
         [:div.cap-detail-label "Minted by"]
         (into [:div.cap-chip-row]
               (map (fn [id] [:span.cap-chip {:key id} id]) (:minted-by node)))])
      (when-let [w (first (:pre-witness node))]
        [:div.cap-detail-section
         [:div.cap-detail-label "Pre-witness"]
         [:div.cap-detail-text (:doing w)]
         (when (seq (:hard-gates w))
           (into [:div.cap-chip-row]
                 (map (fn [id] [:span.cap-chip.cap-chip-gate {:key (id-text id)}
                                (id-text id)])
                      (:hard-gates w))))])]
     [:<>
      [:div.cap-detail-kicker "No node selected"]
      [:h3 "Capability star-map"]
      [:div.cap-detail-text
       "Click a capability node to inspect prerequisites, producing missions, and witness shape."]])])

(defn- edges [caps positions]
  (into []
        (for [[id node] caps
              req (:scope node)
              :let [from (get positions req)
                    to (get positions id)]
              :when (and from to)]
          ^{:key (str req "->" id)}
          [:line.cap-edge
           {:x1 (:x from)
            :y1 (:y from)
            :x2 (:x to)
            :y2 (:y to)}])))

(defn- node-svg [positions [id node]]
  (let [{:keys [x y]} (get positions id)
        selected? (= id (:id @s/selected))
        r (cond
            (:frontier node) 18
            (:keystone node) 17
            :else 15)]
    ^{:key id}
    [:g {:class (str (node-class node) (when selected? " cap-node-selected"))
         :transform (str "translate(" x "," y ")")
         :data-capability-id id
         :on-click #(reset! s/selected (assoc node
                                              :sprite-type :capability
                                              :label (title-label id node)))
         :style {:cursor "pointer"}}
     [:circle.cap-node-ring {:r (+ r 7)}]
     [:circle.cap-node-dot {:r r
                            :fill (node-color node)}]
     (when (seq (:pre-witness node))
       [:circle.cap-node-witness {:r (+ r 3)}])
     [:text.cap-node-label
      {:x 0 :y (+ r 17) :text-anchor "middle"}
      (short-label id 24)]
     [:title (str (title-label id node)
                  "\nstatus: " (status-text (:status node))
                  (when (seq (:scope node))
                    (str "\nrequires: " (str/join ", " (:scope node)))))] ]))

(defn star-map-view []
  (let [graph (or (:capability-star-map @s/data)
                  (when-not (:unavailable @s/capability-star-map)
                    @s/capability-star-map))
        caps (normalize-capabilities graph)
        vp @s/viewport
        w (max 760 (:w vp))
        h (max 620 (:h vp) (+ 150 (* 34 (max-depth-count caps))))
        positions (layout caps w h)
        p (progress caps)
        selected (when (= :capability (:sprite-type @s/selected))
                   @s/selected)]
    [:div.capability-map-view {:data-testid "capability-star-map"}
     [style-node]
     [:div.capability-map-header
      [:div
       [:h2 "Capability Star Map"]
       [:div.capability-map-subtitle
        (:star-map/region graph)
        (when-let [slice (:star-map/slice graph)]
          (str " / " slice))]]
      [:div.cap-progress
       [:div.cap-progress-top
        [:span (str (:satisfied p) "/" (:total p) " satisfied")]
        [:span (str (:held p) " held")]
        [:span (str (:frontier p) " frontier")]]
       [:div.cap-progress-bar
        [:div.cap-progress-fill
         {:style {:width (str (* 100 (:pct p)) "%")}}]]]]
     (if (seq caps)
       [:div.capability-map-body
        [:svg.capability-map-svg
         {:viewBox (str "0 0 " w " " h)
          :preserveAspectRatio "xMidYMid meet"
          :style {:height (str h "px")}
          :data-capability-count (count caps)}
         (into [:g.cap-depth-lines]
               (let [depths (->> positions vals (map :depth) distinct sort)]
                 (for [d depths
                       :let [x (:x (first (filter #(= d (:depth %)) (vals positions))))]]
                   ^{:key (str "depth-" d)}
                   [:g
                    [:line.cap-depth-line {:x1 x :x2 x :y1 32 :y2 (- h 36)}]
                    [:text.cap-depth-label {:x x :y 24 :text-anchor "middle"}
                     (str "level " d)]])))
         (into [:g.cap-edges] (edges caps positions))
         (into [:g.cap-nodes] (map #(node-svg positions %) (sort-by key caps)))]
        [selected-panel selected]]
       [:div.capability-map-empty
        "No capability graph is present in the current War Machine payload."])]))
