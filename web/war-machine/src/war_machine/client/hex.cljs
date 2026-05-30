(ns war-machine.client.hex
  "Pointy-top hex geometry + layout. Direct port of the helpers in
   futon0/scripts/futon0/report/war_machine_visual.clj: hex->pixel, hex-polygon,
   hex-neighbours, hex-spiral, hex-ring-coords, assign-layout, fit-hex-size,
   layout-offset. Rendering-agnostic — returns numbers; sprites.cljs emits SVG."
  (:require [clojure.string :as str]
            [war-machine.client.labels :as labels]))

(def sqrt3 (.sqrt js/Math 3.0))

(defn hex->pixel
  "Axial (q, r) → pixel center (x, y). Odd-r offset layout."
  [q r size]
  (let [x (+ (* size sqrt3 q)
             (if (odd? r) (* size (/ sqrt3 2.0)) 0.0))
        y (* size 1.5 r)]
    [(+ x (* size (/ sqrt3 2.0)))
     (+ y size)]))

(defn hex-points
  "SVG points string for a pointy-top hexagon centered at (cx, cy)."
  [cx cy size]
  (->> (range 6)
       (map (fn [i]
              (let [angle (* (/ (.-PI js/Math) 180.0)
                             (- (* 60.0 i) 30.0))
                    px (+ cx (* size (.cos js/Math angle)))
                    py (+ cy (* size (.sin js/Math angle)))]
                (str (.toFixed px 2) "," (.toFixed py 2)))))
       (str/join " ")))

(defn hex-neighbours
  "Return 6 axial neighbours of (q, r) in odd-r offset layout."
  [q r]
  (if (odd? r)
    [[(inc q) r] [q (inc r)] [(dec q) (inc r)]
     [(dec q) r] [(dec q) (dec r)] [q (dec r)]]
    [[(inc q) r] [(inc q) (inc r)] [q (inc r)]
     [(dec q) r] [q (dec r)] [(inc q) (dec r)]]))

(defn hex-ring-coords
  "Axial coords for ring N around origin."
  [ring]
  (if (zero? ring)
    [[0 0]]
    (let [dirs [[1 0] [0 1] [-1 1] [-1 0] [0 -1] [1 -1]]]
      (vec (for [side (range 6)
                 step (range ring)]
             (let [[dq dr] (nth dirs side)
                   [sq sr] (nth dirs (mod (+ side 2) 6))
                   q (+ (* ring dq) (* step sq))
                   r (+ (* ring dr) (* step sr))]
               [q r]))))))

(defn assign-stack-layout
  "Pack repos into one tight hex cluster, most-connected nodes
   near the center. Mirrors assign-layout in war_machine_visual.clj.

   Strategic SORRY nodes and pocketwatch/tick diagnostics are intentionally
   excluded here. They have dedicated explanatory surfaces elsewhere, and
   mixing them into the Stack view made the repo picture harder to read.

   Accepts JSON-decoded graph data with keyword keys (cljs-http default)."
  [graph-data]
  (let [repos    (get-in graph-data [:nodes :repos] [])
        coupling (get-in graph-data [:edges :temporal-coupling] [])
        edge-count (reduce (fn [m {:keys [from to]}]
                             (-> m (update from (fnil inc 0)) (update to (fnil inc 0))))
                           {} coupling)
        score (fn [node]
                (+ (* 10 (get edge-count (:id node) 0))
                   (or (:commits node) 0)))
        all-nodes (concat
                   (->> repos
                        (map #(assoc % :sprite-type :repo))
                        (sort-by score >)))
        occupied (atom #{})
        cells    (atom [])
        place!   (fn [node q r]
                   (swap! occupied conj [q r])
                   (swap! cells conj {:node node :q q :r r}))]
    (when-let [first-node (first all-nodes)]
      (place! first-node 0 0))
    (let [type-anchors (atom {})]
      (doseq [node (rest all-nodes)]
        (let [st (:sprite-type node)
              frontier (->> @occupied
                            (mapcat (fn [[q r]] (hex-neighbours q r)))
                            distinct
                            (remove @occupied))
              anchor (get @type-anchors st)
              [target-q target-r]
              (if anchor
                anchor
                [(if (seq @cells) (/ (reduce + (map :q @cells)) (double (count @cells))) 0.0)
                 (if (seq @cells) (/ (reduce + (map :r @cells)) (double (count @cells))) 0.0)])
              best (first (sort-by (fn [[q r]]
                                     (let [dq (- q (double target-q))
                                           dr (- r (double target-r))]
                                       (+ (* dq dq) (* dr dr))))
                                   frontier))]
          (when best
            (place! node (first best) (second best))
            (when-not anchor
              (swap! type-anchors assoc st best))))))
    @cells))

;; ----------------------------------------------------------------------------
;; Generic node-list layout — packs an arbitrary sequence of nodes into hexes.
;; Used by the :missions / :invariants / :patterns view-modes so each shows a
;; distinct subset of the war-machine data (no silent fallback to :stack).
;; ----------------------------------------------------------------------------

(defn assign-nodes-layout
  "Pack a flat seq of nodes into a tight hex cluster.  Each `nodes` entry
  becomes a cell, decorated with `sprite-type` and any optional `:tooltip`.

  Layout strategy mirrors `assign-stack-layout`: place the first node at
  origin; subsequent nodes go to the closest unoccupied frontier hex.

  Optional `group-fn` enables clustering by group (e.g. mission phase): the
  first node of each group establishes that group's anchor; subsequent
  nodes of the same group bias toward that anchor instead of the global
  centroid.  Same mechanism `assign-stack-layout` uses for sprite-type
  anchoring.  When `group-fn` is nil, behaviour is unchanged."
  ([nodes sprite-type] (assign-nodes-layout nodes sprite-type nil))
  ([nodes sprite-type group-fn]
   (let [tagged   (map #(assoc % :sprite-type sprite-type) nodes)
         ;; Sort so each group is placed contiguously — the first node of
         ;; each group lands its anchor before that group's other members
         ;; arrive looking for it.
         ordered  (if group-fn (sort-by group-fn tagged) tagged)
         occupied (atom #{})
         cells    (atom [])
         anchors  (atom {})           ; group-key → [q r]
         place!   (fn [node q r]
                    (swap! occupied conj [q r])
                    (swap! cells conj {:node node :q q :r r}))]
     (when-let [first-node (first ordered)]
       (place! first-node 0 0)
       (when group-fn
         (swap! anchors assoc (group-fn first-node) [0 0])))
     (doseq [node (rest ordered)]
       (let [g        (when group-fn (group-fn node))
             anchor   (when g (get @anchors g))
             frontier (->> @occupied
                           (mapcat (fn [[q r]] (hex-neighbours q r)))
                           distinct
                           (remove @occupied))
             [target-q target-r]
             (cond
               anchor anchor
               (seq @cells) [(/ (reduce + (map :q @cells)) (double (count @cells)))
                             (/ (reduce + (map :r @cells)) (double (count @cells)))]
               :else [0.0 0.0])
             best (first (sort-by (fn [[q r]]
                                    (let [dq (- q (double target-q))
                                          dr (- r (double target-r))]
                                      (+ (* dq dq) (* dr dr))))
                                  frontier))]
         (when best
           (place! node (first best) (second best))
           (when (and group-fn (nil? anchor))
             (swap! anchors assoc g best)))))
     @cells)))

;; Futonic 7-phase lifecycle palette.  Maps the IDENTIFY → MAP → DERIVE →
;; ARGUE → VERIFY → INSTANTIATE → DOCUMENT/COMPLETE sequence to a colour
;; gradient (cool → warm → green) so a glance at the missions view shows
;; where each mission sits in its own cycle.  The legend in the sidebar
;; documents this scheme.
(def phase-palette
  ;; Lifecycle palette rebalanced 2026-05-27 (second pass) for maximum hue
  ;; distinctness. The cool→warm→cool gradient is sacrificed in favour of
  ;; legibility: every active phase lives in its own hue family rather than
  ;; doubling-up in the blue/green families. Joe's diagnostic 2026-05-27:
  ;; "2 blue-ish colours, 3 green-ish colours" — the previous pass produced
  ;; that collision. This pass: each phase in a distinct hue family.
  ;;
  ;; Family map: pink → violet → blue → orange → yellow → emerald → indigo
  ;; → deep-slate → light-grey → mid-grey. Only one green (instantiate),
  ;; only one blue (derive). Pink/violet/indigo are visually separable.
  ;; If even this proves insufficient, patterns/stripes are the next move.
  {:identify    "#ec4899"  ;; pink — fresh start (distinct from blue and green families)
   :map         "#a78bfa"  ;; violet — mapping the territory
   :derive      "#3b82f6"  ;; blue — constructing (the ONLY blue)
   :argue       "#fb923c"  ;; orange — defending
   :verify      "#eab308"  ;; yellow — checking
   :instantiate "#10b981"  ;; emerald — building (the ONLY green)
   :document    "#78350f"  ;; dark brown / amber-900 — closing out / filing (distinct from map's violet; semantically "paperwork/archive ink")
   :complete    "#1e293b"  ;; deep slate — settled / archived
   :ready       "#ffffff"  ;; white — declared but not started (clearly distinct from :unknown grey)
   :testing     "#a855f7"  ;; purple
   :unknown     "#d1d5db"  ;; light-mid grey — off-map / no phase declared (sits between :ready white and :complete dark slate; previously #6b7280 was too close to :complete)
   })

(defn parse-phase
  "Take a free-form raw-status string and pick the dominant lifecycle phase
  keyword. Examples: 'MAP' → :map; 'INSTANTIATE (all 6 gates pass)' → :instantiate;
  'Complete (...)' → :complete; 'inferred:ready (0/5 gates)' → :ready;
  ':greenfield' → :identify; nil → :unknown."
  [raw-status]
  (let [s (some-> raw-status str clojure.string/lower-case)]
    (cond
      (nil? s)                           :unknown
      (clojure.string/includes? s "complete")   :complete
      (clojure.string/includes? s "done")       :complete
      (clojure.string/includes? s "instantiate"):instantiate
      (clojure.string/includes? s "document")   :document
      (clojure.string/includes? s "verify")     :verify
      (clojure.string/includes? s "argue")      :argue
      (clojure.string/includes? s "derive")     :derive
      (clojure.string/includes? s "map")        :map
      (clojure.string/includes? s "identify")   :identify
      (clojure.string/includes? s "ready")      :ready
      (clojure.string/includes? s "greenfield") :identify
      (clojure.string/includes? s "testing")    :testing
      (clojure.string/includes? s "active")     :derive
      (clojure.string/includes? s "in-progress"):derive
      :else                                     :unknown)))

(defn- mission-detail-index
  "Build {[id repo] {:raw-status ... :blocked-by ... :phase ...}} from
  the mission-detail payload so we can join lifecycle info onto the simpler
  graph.nodes.missions list."
  [war-machine-data]
  (let [mds (get-in war-machine-data [:mission-detail :missions] [])]
    (into {}
          (keep (fn [m]
                  (when-let [id (or (get m :mission/id) (get m "mission/id"))]
                    (let [repo (or (get m :mission/repo) (get m "mission/repo"))
                          raw  (or (get m :mission/raw-status) (get m "mission/raw-status"))
                          blk  (or (get m :mission/blocked-by) (get m "mission/blocked-by"))]
                      [[(name id) (some-> repo name)]
                       {:raw-status raw
                        :blocked?   (boolean (and blk (or (not (sequential? blk)) (seq blk))))
                        :phase      (parse-phase raw)}]))))
          mds)))

(defn- truncate-label
  "Trim a label to N chars with an ellipsis suffix when truncated."
  [s n]
  (let [s (str s)]
    (if (<= (count s) n) s (str (subs s 0 (max 1 (- n 1))) "…"))))

(defn missions-as-cells
  "Adapt the 45 missions in (:graph :nodes :missions) for repo-style hex render.

   Mission :id is NOT unique — the same mission can appear from multiple repos
   (e.g. f6-ingest exists in both futon3 and futon5).  We rewrite :id to
   id@repo so React keys stay unique; without this, duplicate keys cause
   stale-cell ghosting across view-mode switches.

   Lifecycle phase is joined from `mission-detail.missions[*].mission/raw-status`
   (which carries the actual futonic-cycle phase like 'MAP', 'INSTANTIATE',
   'Complete (...)') so colour reflects where each mission sits in its cycle.
   Falls back to :unknown grey when raw-status is missing.

   Now takes the FULL war-machine payload, not just :graph, so it can read
   mission-detail."
  [war-machine-data]
  (let [graph-data (:graph war-machine-data)
        missions   (get-in graph-data [:nodes :missions] [])
        detail-idx (mission-detail-index war-machine-data)
        adapted (map (fn [m]
                       (let [orig-id (:id m)
                             repo    (:repo m)
                             key     [(some-> orig-id name) (some-> repo name)]
                             detail  (get detail-idx key)
                             phase   (or (:phase detail) :unknown)
                             color   (or (get phase-palette phase)
                                         (get phase-palette :unknown))
                             unique  (if (and orig-id repo)
                                       (str (name orig-id) "@" (name repo))
                                       (str orig-id))
                             full    (or (:label m) (str orig-id))]
                         (assoc m
                                :id          unique
                                :label       (truncate-label full 11)
                                ;; Override workstream-based colouring with phase
                                :phase       phase
                                :phase-color color
                                :workstream  nil  ;; ignored — phase-color wins
                                :active?     true
                                ;; Suppress the per-cell "1" badge for the missions
                                ;; view (2026-05-27 fix). The commits default was
                                ;; uninformative noise — every mission rendered
                                ;; with a "1" overlay. Set to 0 so the sprite's
                                ;; `(pos? commits)` gate keeps the badge hidden.
                                :commits     0
                                :blocked?    (:blocked? detail)
                                :tooltip     (str "MISSION: " full
                                                  "\nPhase: " (name phase)
                                                  (when (:raw-status detail)
                                                    (str " (" (:raw-status detail) ")"))
                                                  "\nStatus: " (:status m)
                                                  "\nRepo: " repo
                                                  (when (:blocked? detail) "\n⛔ blocked")))))
                     missions)]
    ;; Cluster by lifecycle phase so each colour forms a contiguous patch.
    ;; Phases ordered by the futonic cycle so the cluster sequence reads
    ;; identify → map → derive → argue → verify → instantiate → complete.
    (let [phase-order {:identify 0 :map 1 :derive 2 :argue 3 :verify 4
                       :instantiate 5 :document 6 :complete 7
                       :ready 8 :testing 9 :unknown 10}
          group-fn    #(get phase-order (:phase %) 99)]
      (assign-nodes-layout adapted :repo group-fn))))

(defn sorrys-as-cells
  "Adapt strategic SORRYs as cells, clustered by severity so each colour band
   reads as a contiguous patch.

   This is the renamed predecessor of `invariants-as-cells` — SORRYs are
   strategic open commitments, NOT mechanical invariant family violations.
   The :invariants view-mode now renders the AIF+ invariant families (see
   `invariants-as-cells` below).

   Workstream cells are deliberately omitted: redundant with the sidebar
   Workstream Balance bar chart, and conflating two different kinds of thing
   produced the confusing two-island layout.  This view is SORRYs only.

   Strategic SORRY ids keep a full display alias for detail panes, but the
   in-hex label stays compact (`🌐N`) until hover expands it to the semantic
   suffix (`market-interface`, `novelty-floor`, ...)."
  [graph-data]
  (let [sorrys (get-in graph-data [:nodes :sorrys] [])
        ;; Severity ordering: most-severe first so the first cell of each
        ;; severity establishes its anchor in a predictable left-to-right
        ;; gradient (critical → high → warning → medium → info → low).
        sev-order {"critical" 0 "high" 1 "warning" 2 "medium" 3
                   "info" 4 "low" 5}
        adapted (map (fn [s]
                       (let [raw-id     (or (:id s) "?")
                             id-str     (if (keyword? raw-id) (name raw-id) (str raw-id))
                             display-id (labels/strategic-sorry-display-id id-str)
                             short-label (labels/strategic-sorry-short-label id-str)
                             hover-label (labels/strategic-sorry-hover-label id-str)]
                         (assoc s
                                :label        short-label
                                :short-label  short-label
                                :hover-label  hover-label
                                :display-id   display-id
                                :canonical-id id-str)))
                     sorrys)
        group-fn (fn [s] (get sev-order
                              (some-> (:severity s) name)
                              99))]
    (assign-nodes-layout adapted :sorry group-fn)))

;; ----------------------------------------------------------------------------
;; Invariants view — backed by leaf-invariants.aif.edn
;;
;; The leaf encodes 9 operational + 10 candidate invariant families as
;; :family-claim nodes.  Two tier-organiser nodes (:nOp, :nCand) carry the
;; same :family-claim role but with shorter ids — we filter them out so the
;; view shows only the actual families.
;;
;; Cells cluster by tier: operational on the left (always-on, runtime-enforced)
;; then candidate (design pressure, not yet wired).  Status colour reflects
;; the tier; status-mapped severity colour kept compatible with the sorry
;; sprite's existing palette so the cell rendering pipeline doesn't need a
;; new sprite type.
;; ----------------------------------------------------------------------------

(defn- invariant-family?
  "True for the actual invariant family claims; false for tier-organiser
   nodes (:nOp / :nCand) and the pillar-claim (:nPI / :nS6)."
  [node]
  (let [id (some-> (:id node) name)]
    (and (= "family-claim" (some-> (:role node) name))
         id
         (or (clojure.string/starts-with? id "nOp")
             (clojure.string/starts-with? id "nCa")
             (clojure.string/starts-with? id "nCand"))
         (not (#{"nOp" "nCand"} id)))))

(defn- invariant-tier
  "Bucket a family node by its :status. :operational vs :candidate (the leaf
   uses :active for candidates).  Returns :operational, :candidate, or :other."
  [node]
  (case (some-> (:status node) name)
    "operational" :operational
    "active"      :candidate
    :other))

(def ^:private invariant-tier-color
  {:operational "#15803d"   ;; dark green — always-on, runtime-enforced
   :candidate   "#a78bfa"   ;; soft purple — articulated, not yet wired
   :other       "#9ca3af"})

(def ^:private invariant-ref-labels
  {"graph-existence" "graph existence"
   "mission-process" "mission process"
   "system-auth" "system auth"
   "custody" "custody"
   "inspectability" "inspectability"
   "control" "control"})

(defn- invariant-ref-token
  "Return the last structural-law token from NODE's :ref."
  [node]
  (some-> (:ref node) str (str/split #"\|") last))

(defn- invariant-precision-proxy
  "Return a first-pass precision proxy for NODE.

   This is a v1 heuristic, not a learned AIF score: operational families are
   treated as high-precision priors, candidate families as lower-precision
   priors, with the control family slightly elevated because the leaf itself
   treats it as the closest candidate tier to promotion."
  [node]
  (case (invariant-tier node)
    :operational 0.90
    :candidate   (case (invariant-ref-token node)
                   "control" 0.60
                   "inspectability" 0.45
                   "custody" 0.40
                   0.35)
    0.20))

(defn- invariant-family-label
  "Return a readable family label for the invariant hex.

   Prefer the structural-law ref token over the leaf-internal nOp*/nCa* id so
   the operator sees a meaningful family name instead of an anthology-local
   abbreviation."
  [node]
  (let [raw (or (some-> node invariant-ref-token invariant-ref-labels)
                (some-> (:id node) name (or "?")))]
    (truncate-label
      (cond-> raw
        (and (string? raw) (clojure.string/starts-with? raw "n")) (subs 1))
      9)))

(defn- candidate-queue-overlay
  "Return queue overlay metadata for NODE from the live AIF payload.

   Only the compressed candidate tier (`custody` / `inspectability` /
   `control`) projects cleanly into the leaf. Anything else remains visible in
   `:candidate-queue.unmapped-families` server-side rather than being guessed
   into a bucket."
  [aif-payload node]
  (let [family-token (invariant-ref-token node)]
    (or (get-in aif-payload [:candidate-queue :by-leaf-family family-token])
        (get-in aif-payload [:candidate-queue :by-leaf-family (keyword family-token)]))))

(defn invariants-as-cells
  "Render the AIF+ invariant families from the live payload's :leaf-invariants
   slot.  Cells cluster by tier (operational then candidate)."
  [aif-payload]
  (let [leaf      (:leaf-invariants aif-payload)
        families  (filter invariant-family? (:nodes leaf))
        tier-order {:operational 0 :candidate 1 :other 2}
        adapted   (->> families
                       (map (fn [n]
                              (let [tier (invariant-tier n)
                                    color (get invariant-tier-color tier)
                                    precision (invariant-precision-proxy n)
                                    family-label (invariant-family-label n)
                                    queue (when (= tier :candidate)
                                            (candidate-queue-overlay aif-payload n))
                                    queue-rank (:top-rank queue)
                                    queue-score (:top-score queue)
                                    queue-count (:item-count queue)
                                    queue-families (:families queue)]
                                (assoc n
                                       :id          (some-> (:id n) name)
                                       :label       family-label
                                       :hover-label (cond-> family-label
                                                      queue-rank (str " #" queue-rank))
                                       :sprite-type :sorry
                                       ;; Borrow the sorry sprite (hex + dark
                                       ;; centred label).  Map tier to a
                                       ;; pseudo-severity that severity-color
                                       ;; renders with the right tier hue.
                                       :severity    (case tier
                                                      :operational "operational"
                                                      :candidate   "candidate"
                                                      :other       "info")
                                       :tier        tier
                                       :tier-color  color
                                       :queue-rank  queue-rank
                                       :queue-score queue-score
                                       :tooltip     (str "INVARIANT FAMILY " family-label "\n"
                                                         "Canonical id: " (name (:id n)) "\n"
                                                         "Tier: " (name tier) "\n"
                                                         "Precision proxy π: " (.toFixed precision 2) " (v1 heuristic)\n"
                                                         (when queue-rank
                                                           (str "Queue top rank: #" queue-rank "\n"
                                                                "Queue top score: " queue-score "\n"
                                                                "Queue item count: " queue-count "\n"
                                                                "Projected families: "
                                                         (str/join ", " queue-families)
                                                                "\n"))
                                                         "\n"
                                                         (:content n))))))
                       (sort-by (fn [n]
                                  [(get tier-order (:tier n) 99)
                                   (or (:queue-rank n) 999)
                                   (:label n)])))
        group-fn (fn [n] (get tier-order (:tier n) 99))]
    (assign-nodes-layout adapted :sorry group-fn)))

(defn assign-linear-layout
  "Pack a flat seq of nodes into a row-major hex grid (left→right, top→bottom).
   Used by `patterns-as-cells` so that, after sorting input by activation
   balance, the resulting hex tiles form a visual hue gradient from one
   pole (pure-blue, balance=0) to the other (green/yellow, balance≥0.5)
   per Joe's emacs-repl 2026-05-25 directive.

   `cols` controls grid width.  Input order is preserved verbatim — sort
   upstream to control which cell lands at which position."
  [nodes sprite-type cols]
  (let [tagged (map #(assoc % :sprite-type sprite-type) nodes)]
    (vec
     (map-indexed
      (fn [i node]
        (let [col (mod i cols)
              row (quot i cols)]
          {:node node
           :q   col
           :r   row}))
      tagged))))

(defn- pattern-activation-hue
  "Joe's blue-yellow-green hue scheme (emacs-repl 2026-05-25):

   - Pattern count → BLUE weighting (HSL hue 240).
   - Activation count → YELLOW weighting (HSL hue 60).
   - Mixing yellow+blue → GREEN (HSL hue 120) at equal balance.

   Computes `balance = activations / (pattern-count + activations)`, then
   maps via a piecewise-linear hue:
     - balance ≤ 0.5 :  240 - 240·balance   (blue → green)
     - balance > 0.5 :  120 - 120·(balance − 0.5)   (green → yellow)
   so balance ∈ {0, 0.5, 1} yields hue ∈ {240, 120, 60}.

   Saturation/lightness intentionally NOT in this function — Joe explicitly
   asked that saturation track the existing activity-alpha-on-pattern-count
   behaviour, which is implemented in sprites.cljs via :fill-opacity."
  [pattern-count activation-count]
  (let [pc (max 0 (long (or pattern-count 0)))
        ac (max 0 (long (or activation-count 0)))
        total (+ pc ac)]
    (cond
      ;; No signal at all → default to blue (pattern-count axis).
      (zero? total) 240
      :else
      (let [balance (/ (double ac) (double total))
            hue (if (<= balance 0.5)
                  (- 240.0 (* 240.0 balance))           ;; 240 → 120
                  (- 120.0 (* 120.0 (- balance 0.5))))] ;; 120 → 60
        (int (Math/round (double hue)))))))

(defn- pattern-activation-color
  "Wrap [[pattern-activation-hue]] into an HSL color string consumable by
   SVG :fill.  Full saturation + medium lightness give vivid pure hues; the
   sprite's existing activity-alpha provides the pattern-count-based fade."
  [pattern-count activation-count]
  (str "hsl(" (pattern-activation-hue pattern-count activation-count)
       ", 80%, 50%)"))

(defn patterns-as-cells
  "Adapt pattern collections; cap at top 24 by :count for visual clarity.

   When the API surfaces a per-collection activations count (e.g.
   :activations-14d, populated by futon3c.transport.http/derive-pattern-activations
   from PSR evidence-store entries — M-pattern-application-diagnostic
   integration, 2026-05-25):

   - badge reads 'activations/pattern-count' instead of just 'pattern-count'
   - :phase-color overrides the workstream-color to encode the
     pattern-vs-activation balance in HSL hue (blue ↔ yellow with green
     at the midpoint), per Joe's emacs-repl 2026-05-25 directive

   :commits stays = pattern-count so the existing activity-alpha continues
   to drive saturation/fade.  Joe explicitly asked that saturation track
   pattern count 'much as it currently is.'"
  [war-machine-data]
  (let [collections (get-in war-machine-data [:patterns :collections] [])
        window-days (some (fn [c] (or (:activations-window-days c)
                                      (get c "activations-window-days")))
                          collections)
        activation-key (when window-days
                         (keyword (str "activations-" window-days "d")))
        activation-of (fn [c]
                        (when activation-key
                          (or (get c activation-key)
                              (get c (name activation-key)))))
        ;; Selection: keep the top-24 by pattern count (most substantive
        ;; collections worth showing).
        top (->> collections
                 (sort-by (comp - #(or (:count %) 0)))
                 (take 24))
        balance-of (fn [c]
                     (let [pc (long (or (:count c) 0))
                           ac (long (or (activation-of c) 0))
                           total (+ pc ac)]
                       (if (pos? total)
                         (/ (double ac) (double total))
                         0.0)))
        ;; Geometric arrangement: re-sort the selected top-24 by
        ;; activation balance ASCENDING, so pure-blue (balance 0,
        ;; pattern-heavy / activation-low) lands at one pole and
        ;; green / yellow tiles (high activation share) lands at the
        ;; other.  Joe's emacs-repl 2026-05-25 directive.  The
        ;; assign-nodes-layout call respects input order, so a sorted
        ;; input yields a visual hue gradient across the hex grid.
        arranged (sort-by balance-of top)
        adapted (map (fn [c]
                       (let [full      (or (:name c) (str (:id c)))
                             total     (or (:count c) 0)
                             activated (activation-of c)
                             badge-display (when activated
                                             (str activated "/" total))
                             phase-color (pattern-activation-color
                                          total
                                          (or activated 0))]
                         (cond-> (assoc c
                                        :label       (truncate-label full 11)
                                        :workstream  :mathematics
                                        :active?     true
                                        :commits     total
                                        :phase-color phase-color
                                        :tooltip     (str "PATTERN: " full
                                                          "\nCount: " total
                                                          (when activated
                                                            (str
                                                             "\nActivations (last "
                                                             window-days "d): "
                                                             activated))))
                           badge-display (assoc :badge-display badge-display))))
                     arranged)]
    ;; Linear row-major layout (NOT centroid-packed) so the balance-sorted
    ;; input becomes a left→right hue gradient on the rendered hex grid.
    ;; 6 cols × 4 rows fits 24 tiles cleanly.
    (assign-linear-layout adapted :repo 6)))

;; ----------------------------------------------------------------------------
;; AIF+ stack-self-model layout — for the :aif-stack view-mode.
;;
;; Inputs come from /api/alpha/aif-stack/live and arrive with keyword keys
;; (cljs-http default).  Spine nodes are rendered as :repo sprites (so they
;; get label + color + active treatment for free).  Conflict nodes are
;; rendered as :sorry sprites (red border, id label, severity-coloured fill).
;; The layout places spine nodes in a tight cluster (most-cited-by near the
;; centre) with conflict nodes ringing the outside.
;; ----------------------------------------------------------------------------

(defn- aif-role->workstream
  "Map an AIF+ spine-node :role to one of the four workstream colours so the
  existing repo-sprite picks the right palette."
  [role]
  (case (some-> role name keyword)
    :thesis        :stack
    :ur-claim      :stack
    :pillar        :mathematics
    :cycle         :mathematics
    :aif-substrate :portfolio
    :substrate     :portfolio
    :frame         :consulting
    :stack))

(defn- aif-status->severity
  "Map AIF+ conflict status/weight to severity for the sorry-sprite palette."
  [weight]
  (cond
    (and weight (>= weight 7)) :critical
    (and weight (>= weight 4)) :warning
    :else :info))

(defn- spine-node->cell-node
  "Adapt a spine node to the shape repo-sprite expects."
  [node]
  (let [id (or (:id node) "?")]
    {:id           id
     :label        (labels/stack-spine-display-id id)
     :short-label  (labels/stack-spine-display-id id)
     :hover-label  (labels/stack-spine-hover-label id)
     :display-id   (labels/stack-spine-display-id id)
     :canonical-id (labels/id->text id)
     :sprite-type  :repo
     :workstream   (aif-role->workstream (:role node))
     :active?      true
     :commits      (count (:cited-by node))
     :tooltip      (:content node)
     :status       (:status node)
     :live-status? (:live-status? node)
     :role         (:role node)}))

(defn- conflict-node->cell-node
  "Adapt a conflict node to the shape sorry-sprite expects."
  [c]
  (let [id (or (:id c) "?")
        weight (or (:weight c) 0)]
    {:id          id
     :sprite-type :sorry
     :severity    (aif-status->severity weight)
     :tooltip     (:content c)
     :weight      weight
     :bites       (:bites c)
     :coalesces-from (:coalesces-from c)
     :role        :load-bearing-conflict}))

(defn assign-aif-stack-layout
  "Given the AIF+ live payload (with :stack-nodes and :stack-conflicts),
   produce hex cells in the same shape `assign-stack-layout` returns.

   Spine nodes go first, ordered by inbound :cited-by count (descending) so
   the most-connected nodes anchor near the centre.  Conflict nodes follow
   and get pushed to the periphery via the same anchor mechanism."
  [aif-payload]
  (let [spine     (:stack-nodes aif-payload)
        conflicts (:stack-conflicts aif-payload)
        spine-by-cites (sort-by #(- (count (:cited-by %))) spine)
        all-nodes (concat (map spine-node->cell-node spine-by-cites)
                          (map conflict-node->cell-node conflicts))
        occupied (atom #{})
        cells    (atom [])
        place!   (fn [node q r]
                   (swap! occupied conj [q r])
                   (swap! cells conj {:node node :q q :r r}))]
    (when-let [first-node (first all-nodes)]
      (place! first-node 0 0))
    (let [type-anchors (atom {})]
      (doseq [node (rest all-nodes)]
        (let [st (:sprite-type node)
              frontier (->> @occupied
                            (mapcat (fn [[q r]] (hex-neighbours q r)))
                            distinct
                            (remove @occupied))
              anchor (get @type-anchors st)
              [target-q target-r]
              (if anchor
                anchor
                [(if (seq @cells) (/ (reduce + (map :q @cells)) (double (count @cells))) 0.0)
                 (if (seq @cells) (/ (reduce + (map :r @cells)) (double (count @cells))) 0.0)])
              best (first (sort-by (fn [[q r]]
                                     (let [dq (- q (double target-q))
                                           dr (- r (double target-r))]
                                       (+ (* dq dq) (* dr dr))))
                                   frontier))]
          (when best
            (place! node (first best) (second best))
            (when-not anchor
              (swap! type-anchors assoc st best))))))
    @cells))

(defn layout-bounds [cells]
  (if (empty? cells)
    {:q-min 0 :q-max 0 :r-min 0 :r-max 0 :q-span 1 :r-span 1}
    (let [qs (map :q cells)
          rs (map :r cells)
          q-min (apply min qs) q-max (apply max qs)
          r-min (apply min rs) r-max (apply max rs)]
      {:q-min q-min :q-max q-max :r-min r-min :r-max r-max
       :q-span (max 1 (- q-max q-min))
       :r-span (max 1 (- r-max r-min))})))

(defn fit-hex-size
  "Compute hex size that uses panel real-estate efficiently.  Lower margin
  + tighter padding than the original assign-stack-layout default — the old
  numbers wasted a lot of whitespace, leaving labels squished into too-small
  hexes.  Min size raised so labels stay legible even on dense layouts."
  [cells panel-w panel-h]
  (let [margin 12  ;; reduced from 24 (2026-05-27) — large-scale viewports had visible wasted whitespace
        usable-w (- panel-w (* 2 margin))
        usable-h (- panel-h (* 2 margin))
        {:keys [q-span r-span]} (layout-bounds cells)
        ;; Padding-factor was 0.6 each side (1 hex of breathing room per dimension);
        ;; tightened to 0.2 (2026-05-27) so connected-components fill the available
        ;; space at large scale. Still leaves enough margin for label/border without
        ;; overflow.
        size-w (/ usable-w (* sqrt3 (+ q-span 0.2)))
        size-h (/ usable-h (* 1.5 (+ r-span 0.2)))]
    (max 18.0 (min size-w size-h))))

(defn layout-offset [cells hex-size panel-w panel-h]
  (if (empty? cells)
    [0.0 0.0]
    (let [pxs (map (fn [{:keys [q r]}] (hex->pixel q r hex-size)) cells)
          xs (map first pxs)
          ys (map second pxs)
          px-min (apply min xs) px-max (apply max xs)
          py-min (apply min ys) py-max (apply max ys)
          off-x (- (/ panel-w 2.0) (/ (+ px-min px-max) 2.0))
          off-y (- (/ panel-h 2.0) (/ (+ py-min py-max) 2.0))]
      [off-x off-y])))
