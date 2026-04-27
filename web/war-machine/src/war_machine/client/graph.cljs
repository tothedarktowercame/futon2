(ns war-machine.client.graph
  "SVG hex map — the main visualiser panel. Subscribes to state ratoms,
   computes layout each render (cheap for <200 hexes), renders sprites,
   coupling edges, and the session-ant swarm."
  (:require [war-machine.client.state :as s]
            [war-machine.client.hex :as hex]
            [war-machine.client.sprites :as sprites]
            [war-machine.client.aif-join :as aif]
            [war-machine.client.api :as api]
            [war-machine.client.labels :as labels]
            [clojure.string :as str]))

(defn- cell-pixel [{:keys [q r]} size]
  (hex/hex->pixel q r size))

(defn- cell-id [cell]
  (let [n (:node cell)]
    (or (:id n) (:mission/id n))))

(defn- node-id-text [node]
  (when-let [id (or (:id node) (:mission/id node))]
    (if (keyword? id) (name id) (str id))))

(defn- coupling-edges-hiccup [cells coupling-edges size]
  (let [cell-map (into {} (map (fn [c] [(cell-id c) c]) cells))]
    (for [{:keys [from to strength]} coupling-edges
          :let [a (get cell-map from)
                b (get cell-map to)]
          :when (and a b)]
      (let [[ax ay] (cell-pixel a size)
            [bx by] (cell-pixel b size)
            s (double (or strength 0.5))
            alpha (+ 0.12 (* 0.65 (min 1.0 s)))
            width (+ 1.0 (* 3.0 (min 1.0 s)))]
        ^{:key (str (if (keyword? from) (name from) from)
                    "->"
                    (if (keyword? to) (name to) to))}
        [:line {:x1 ax :y1 ay :x2 bx :y2 by
                :stroke "#6464c8" :stroke-opacity alpha
                :stroke-width width}]))))

(defn- bite-edges-hiccup
  "For the :aif-stack view: draw a line from each conflict cell to each
   spine cell it bites.  Stroke colour reflects empirical evidence —
   solid red when at least one session in the window touched the bitten
   spine, dashed grey when the bite is still purely logical."
  [cells conflicts hits-by-spine size]
  (let [cell-map (into {} (map (fn [c] [(cell-id c) c]) cells))]
    (for [c conflicts
          :let [from-cell (get cell-map (:id c))]
          :when from-cell
          b (:bites c)
          :let [bid (if (keyword? b) (name b) (str b))
                to-cell (get cell-map bid)]
          :when to-cell]
      (let [[ax ay] (cell-pixel from-cell size)
            [bx by] (cell-pixel to-cell size)
            n (or (get hits-by-spine bid) 0)
            empirical? (pos? n)
            stroke (if empirical? "#dc2626" "#9ca3af")
            opacity (if empirical?
                      (min 0.85 (+ 0.35 (* 0.08 n)))
                      0.25)
            width (if empirical? (min 4.0 (+ 1.5 (* 0.4 n))) 1.0)
            dash  (when-not empirical? "4 4")]
        ^{:key (str (:id c) "→" bid)}
        [:line (cond-> {:x1 ax :y1 ay :x2 bx :y2 by
                        :stroke stroke
                        :stroke-opacity opacity
                        :stroke-width width
                        :pointer-events "none"}
                 dash (assoc :stroke-dasharray dash))]))))

(defn- node-detail-text [node]
  (cond
    (= :load-bearing-conflict (:role node))
    (let [ctx       (aif/join-context)
          eb        (get-in ctx [:empirical-bites (:id node)])
          hits      (:hits-by-spine ctx)
          bite-strs (map (fn [b] (if (keyword? b) (name b) (str b)))
                         (:bites node))]
      (str "CONFLICT " (:id node) "  (weight " (or (:weight node) "?") ")\n\n"
           (or (:tooltip node) "(no rationale captured)")
           (when (seq bite-strs)
             (str "\n\nBites:"
                  (str/join ""
                    (for [b bite-strs
                          :let [n (or (get hits b) 0)]]
                      (str "\n  " b
                           (if (pos? n)
                             (str " — " n " hit" (when (> n 1) "s") " in window")
                             " — no empirical hit yet"))))))
           (when eb
             (str "\n\nEmpirical bite coverage: "
                  (:hit eb) "/" (:total eb)
                  " spine targets touched in window."))))

    :else
    (case (:sprite-type node)
      :repo  (str (cond
                    (#{:thesis :ur-claim :pillar :cycle :substrate :aif-substrate :frame}
                     (some-> (:role node) keyword))
                    (str "STACK NODE " (or (:display-id node)
                                           (labels/stack-spine-display-id (:id node))
                                           (:id node))
                         " (" (some-> (:role node) name) ")\n")
                    :else
                    (str "REPO: " (:label node) "\n"))
                  (when-let [canon (:canonical-id node)]
                    (when (not= canon (:display-id node))
                      (str "Canonical id: " canon "\n")))
                  (when (:workstream node)
                    (str "Workstream: " (name (:workstream node)) "\n"))
                    (when (:tooltip node)
                      (str "\n" (labels/stack-text->display (:tooltip node)) "\n"))
                  (when-let [gap (:gap node)]
                    (str "\nGap: " gap))
                  (when (some? (:live-self-step-count node))
                    (str "\nPI self-step-count: " (:live-self-step-count node)))
                  (when (:commits node)
                    (str "\nCommits: " (:commits node)))
                  (when (some? (:active? node))
                    (str "\nActive: " (:active? node))))
      :sorry (let [canon   (or (:canonical-id node) (node-id-text node))
                   display (or (:display-id node)
                               (labels/strategic-sorry-display-id canon)
                               canon)]
               (str "SORRY: " (or display canon "?") "\n"
                    (when (and canon display (not= canon display))
                      (str "Canonical id: " canon "\n"))
                    "Severity: " (or (:severity node) "?") "\n"
                    "Status: " (or (:status node) "?")))
      :tick  (str "TICK: " (:id node) "\n"
                  "Fired: " (:fired? node))
      (str "Node: " (pr-str node)))))

(defn- step->ids
  "Translate one session step into the list of cell ids it touches in the
   current view-mode.  In :aif-stack we go through the empirical join
   (repo → devmap leaf → spine-id); in :missions we use mission ids
   directly; everywhere else, repo ids."
  [vmode leaf->spine mid->detail step]
  (case vmode
    :aif-stack (vec (aif/step->spine-ids leaf->spine mid->detail step))
    :missions  (:missions step)
    (:repos step)))

(declare node-emacs-target)

(defn hex-map []
  (let [data     @s/data
        aif-data @s/aif-data
        vmode    @s/view-mode
        hot      @s/hotspot
        hovered  @s/hovered
        replay   @s/replay
        vp       @s/viewport
        w (:w vp) h (:h vp)
        cells (case vmode
                :stack      (hex/assign-stack-layout (:graph data))
                :aif-stack  (if aif-data
                              (hex/assign-aif-stack-layout aif-data)
                              ;; AIF+ data hasn't loaded yet — fall back to stack
                              (hex/assign-stack-layout (:graph data)))
                :missions   (hex/missions-as-cells data)
                :sorrys     (hex/sorrys-as-cells (:graph data))
                :invariants (if aif-data
                              (hex/invariants-as-cells aif-data)
                              ;; AIF+ data hasn't loaded yet — fall back to
                              ;; the SORRY layout so the view isn't blank
                              (hex/sorrys-as-cells (:graph data)))
                :patterns   (hex/patterns-as-cells data)
                (hex/assign-stack-layout (:graph data)))
        coupling (get-in data [:graph :edges :temporal-coupling] [])
        size  (hex/fit-hex-size cells w h)
        [ox oy] (hex/layout-offset cells size w h)
        cell-map (into {} (map (fn [c] [(cell-id c) c]) cells))
        ;; Pre-compute the spine join once per render, and only when
        ;; we're actually in aif-stack mode (cheap but not free).
        join-ctx (when (= vmode :aif-stack) (aif/join-context))
        leaf->spine (:leaf->spine join-ctx)
        mid->detail (:mid->detail join-ctx)]
    [:svg {:xmlns "http://www.w3.org/2000/svg"
           :viewBox (str "0 0 " w " " h)
           :preserveAspectRatio "xMidYMid meet"
           :data-testid "hex-svg"
           :data-cell-count (count cells)}
     [:g {:transform (str "translate(" ox "," oy ")")}
      [:g.coupling (coupling-edges-hiccup cells coupling size)]
      (when (= vmode :aif-stack)
        [:g.bite-edges
         (bite-edges-hiccup cells
                            (:stack-conflicts aif-data)
                            (:hits-by-spine join-ctx)
                            size)])
       [:g.cells
       (for [cell cells
             :let [node (:node cell)
                   id (cell-id cell)
                   target (node-emacs-target node)
                   render-node (cond-> node
                                 (= id hovered) (assoc :label (or (:hover-label node)
                                                                  (:label node))))
                   glow (when id (get hot id))
                   [cx cy] (cell-pixel cell size)]]
         ^{:key (or (when id (str id)) (str (:q cell) ":" (:r cell)))}
         [:g {:on-click (fn []
                          (reset! s/selected node)
                          (when target
                            (api/open-target-in-emacs! target)))
              :on-mouse-enter #(when (:hover-label node)
                                 (reset! s/hovered id))
              :on-mouse-leave #(when (:hover-label node)
                                 (reset! s/hovered nil))
              :style {:cursor "pointer"}
              :data-node-id (when id (str id))}
          (sprites/draw-sprite render-node cx cy size glow)])]
       [:g.ants
       (for [[sid state] replay
             :when (and (not= sid :playing?) (map? state))
             :let [{:keys [steps step-idx color-idx]} state
                   n (count steps)
                   current (when (and (pos? n) (some? step-idx))
                             (step->ids vmode leaf->spine mid->detail
                                        (nth steps step-idx)))
                   trail (when (and (pos? n) (some? step-idx))
                           (for [i (range 1 7)
                                 :let [idx (mod (- step-idx i) n)
                                       step (nth steps idx)
                                       ids (step->ids vmode leaf->spine
                                                      mid->detail step)]
                                 :when (seq ids)]
                             {:ids ids :age i}))]]
         ^{:key (str sid)}
         [sprites/session-ant current trail cell-map size color-idx])]]]))

(defn- node-leaf-name
  "Best-effort: which VSAT-shaped story (futon5a/holes/stories/<leaf>.md)
   most closely binds to NODE? Returns nil when there's no obvious match."
  [node]
  (cond
    ;; AIF-stack spine node — :origin like 'leaf-argument#n0' or
    ;; 'leaf-cycle#nRoot = leaf-argument#nCycle'.  Pick the first leaf token.
    (some-> (:role node) keyword
            #{:thesis :ur-claim :pillar :cycle :substrate
              :aif-substrate :frame})
    (when-let [origin (:origin node)]
      (some-> (re-find #"[a-z][a-z0-9-]*(?=#)" (str origin))))

    ;; Conflict cells — :coalesces-from is a map keyed by leaf name.
    ;; The wire form delivers it as a map of leaf → reason; pick the first.
    (= :load-bearing-conflict (:role node))
    (some-> (:coalesces-from node) keys first
            (#(if (keyword? %) (name %) (str %))))

    ;; Repo cells (the :stack view) — devmap-<repo>.md exists for many.
    (and (:label node) (#{:repo} (:sprite-type node))
         (#{"futon0" "futon1" "futon2" "futon3" "futon3a" "futon4"
            "futon5" "futon6" "futon7"}
          (str (:label node))))
    (str "devmap-" (:label node))

    ;; Invariant family cells — all live in leaf-invariants.
    ;; Detected by :tier (set by invariants-as-cells).
    (#{:operational :candidate} (:tier node))
    "leaf-invariants"

    :else nil))

(def ^:private strategic-sorry-story-leaves
  {"SORRY-market-interface"   "globe1-market-interface"
   "SORRY-mode-violation"     "globe2-mode-violation"
   "SORRY-peer-eval-artifact" "globe3-peer-eval-artifact"
   "SORRY-paragogy-revenue"   "globe4-paragogy-revenue"
   "SORRY-vsat-revenue"       "globe5-vsat-revenue"
   "SORRY-governance-interface" "globe6-governance-interface"
   "SORRY-novelty-floor"      "globe7-novelty-floor"
   "SORRY-policy-transition"  "globe8-policy-transition"})

(defn- strategic-sorry-leaf [node]
  (some-> (or (:canonical-id node) (node-id-text node))
          str
          strategic-sorry-story-leaves))

(defn- node-emacs-target
  "Return the best available Emacs target for NODE, or nil when there is no
   honest textual target yet."
  [node]
  (or (when-let [leaf (strategic-sorry-leaf node)]
        {:kind :vsatarcs-story
         :leaf leaf})
      (when-let [leaf (node-leaf-name node)]
        {:kind :vsatarcs-story
         :leaf leaf})))

(defn- target-title [target]
  (case (:kind target)
    :workspace-file (str "Open " (:path target) " in Emacs")
    :vsatarcs-story (str "Open " (:leaf target) ".md"
                         (when-let [anchor (:scene-anchor target)]
                           (str " #" anchor))
                         " in Emacs/VSATARCS")
    "Open linked source in Emacs"))

(defn detail-panel []
  (let [node @s/selected
        target (when node (node-emacs-target node))]
    [:div.detail {:data-testid "detail"}
     (if node
       [:<>
        (when target
          [:button.detail-arxana
           {:on-click #(api/open-target-in-emacs! target)
            :title (target-title target)
            :data-testid "open-in-emacs"}
           "📖 Open in Emacs"])
        [:pre.detail-text (node-detail-text node)]]
       "(click a hex for details)")]))
