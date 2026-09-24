;; generate_figures.clj — regenerate every figure in 01-cascades.md from the
;; records. Run from the futon2 checkout root:
;;
;;   clojure -M -i holes/labs/wm-contract/walkthroughs/generate_figures.clj \
;;           -e "(generate-figures/generate!)"
;;
;; Read-only: reads run records, writes only SVGs beside this script. Every
;; figure file name carries the short sha256 of the record it was drawn
;; from; a figure that cannot be regenerated this way is not accepted.
(ns generate-figures
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [futon2.aif.cascade-structure :as structure]
            [futon2.aif.narrative-figures :as figures])
  (:import [java.security MessageDigest]))

(def here "holes/labs/wm-contract/walkthroughs")
(def record-0923 "data/wm-runs/tick-run-record-2026-09-23-1790131591.edn")
(def record-0922 "data/wm-runs/tick-run-record-2026-09-22-1790053967.edn")
(def machinery-72 "data/wm-full-loop-machinery-72/wm-contract-machinery-72-v1")

(defn read-record [path]
  (edn/read-string {:default (fn [t v] (tagged-literal t v))} (slurp path)))

(defn sha8 [path]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes (slurp path) "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and 255 %)) (take 4 digest)))))

(defn spit-svg [name svg]
  (let [file (str here "/" name)]
    (spit file svg)
    (println "wrote" file)))

;; ---------------------------------------------------------------------------
;; Small hand-built SVG helpers (figures 2, 3, 5). Figures 1, 4 and 6 reuse
;; futon2.aif.narrative-figures/cascade-svg.
;; ---------------------------------------------------------------------------

(defn- esc [s]
  (-> (str s) (str/replace "&" "&amp;") (str/replace "<" "&lt;") (str/replace ">" "&gt;")))

(defn- wrap [s n]
  (let [words (str/split (str s) #"\s+")]
    (loop [ws words line "" lines []]
      (if (empty? ws)
        (if (str/blank? line) lines (conj lines line))
        (let [w (first ws) trial (if (str/blank? line) w (str line " " w))]
          (if (> (count trial) n)
            (recur (rest ws) w (conj lines line))
            (recur (rest ws) trial lines)))))))

(defn- htxt [x y s & {:keys [size anchor weight dash] :or {size 12 anchor "start"}}]
  (str "<text x=\"" x "\" y=\"" y "\" font-size=\"" size "\" text-anchor=\"" anchor "\""
       (when weight (str " font-weight=\"" weight "\""))
       (when dash (str " stroke-dasharray=\"" dash "\""))
       " font-family=\"system-ui,sans-serif\">" (esc s) "</text>"))

(defn- box [x y w h lines & {:keys [fill stroke dash] :or {fill "#f6f7f8" stroke "#68717b"}}]
  (str "<rect x=\"" x "\" y=\"" y "\" width=\"" w "\" height=\"" h "\" rx=\"5\""
       " fill=\"" fill "\" stroke=\"" stroke "\""
       (when dash (str " stroke-dasharray=\"" dash "\"")) "/>"
       (apply str (map-indexed (fn [i line] (htxt (+ x 8) (+ y 18 (* 14 i)) line)) lines))))

(defn- arrow [x1 y1 x2 y2 & {:keys [dash color label] :or {color "#68717b"}}]
  (str "<path d=\"M" x1 "," y1 " L" x2 "," y2 "\" fill=\"none\" stroke=\"" color "\""
       (when dash (str " stroke-dasharray=\"" dash "\""))
       " marker-end=\"url(#harrow)\"/>"
       (when label (htxt (/ (+ x1 x2) 2) (- (/ (+ y1 y2) 2) 4) label :size 10 :anchor "middle"))))

(defn- hsvg [width height body]
  (str "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" width "\" height=\"" height
       "\" viewBox=\"0 0 " width " " height "\">"
       "<defs><marker id=\"harrow\" markerWidth=\"8\" markerHeight=\"8\" refX=\"7\" refY=\"4\" orient=\"auto\">"
       "<path d=\"M0,0 L8,4 L0,8 Z\" fill=\"#68717b\"/></marker></defs>"
       "<rect width=\"" width "\" height=\"" height "\" fill=\"white\"/>"
       body "</svg>"))

;; ---------------------------------------------------------------------------
;; Record extraction
;; ---------------------------------------------------------------------------

(def token-abbrev
  {:admission/task-stated "task-stated"
   :repair/split-declared-valid "split-declared-valid"
   :repair/held-out-observations-collected "observations-collected"
   :repair/calibration-evidence-present "calibration-evidence"
   :repair/obstruction-observed-cleared "obstruction-cleared"
   :restoration-accepted "restoration-accepted"})

(def token-short
  {:admission/task-stated "stated"
   :repair/split-declared-valid "split-valid"
   :repair/held-out-observations-collected "obs-collected"
   :repair/calibration-evidence-present "calib-evidence"
   :repair/obstruction-observed-cleared "obstr-cleared"
   :restoration-accepted "accepted"})

(defn tok-short [[_ t]] (or (token-short t) (name t)))

(defn tok-label [[_ t]] (or (token-abbrev t) (name t)))

(defn action-0923 [r]
  (get-in r [:decision :selection-law :per-policy-argmax :action]))

;; ---------------------------------------------------------------------------
;; Figures
;; ---------------------------------------------------------------------------

(defn fig1-cascade-0923 [r]
  (let [a (action-0923 r)
        receipt (structure/receipt a)
        c (first (get-in r [:decision :selection-certificate :candidates]))]
    (figures/cascade-svg
     {:target (:target a)
      :patterns (:precedence a)
      :outcomes []
      :shape (:shape receipt)
      :prediction-source
      (str "one admitted candidate " (get-in c [:id :id])
           ": habit E = " (:habit c) ", G = " (:g c)
           ", F " (:f-status c) " — the law that ran is habit minus G/beta"
           " (PROOF-2-F-discovery-2026-09-24).")})))

(defn fig3-order-0923 [r]
  (let [a (action-0923 r)
        receipt (structure/receipt a)
        prec (:precedence a)
        n (count prec)
        boxw 300 boxh 64 gapx 70
        width (+ 60 (* n (+ boxw gapx)))
        height 340
        positions (into {} (map-indexed (fn [i p] [(:id p) [(+ 30 (* i (+ boxw gapx))) 110]]) prec))]
    (hsvg
     width height
     (str
      (htxt 30 36 "Order structure of the 2026-09-23 repair cascade (C2), computed by futon2.aif.cascade-structure/receipt"
            :size 15 :weight "bold")
      (htxt 30 58 (str "shape: " (name (:shape receipt))
                       " · basis: declared need-support (produces -> guard present)"
                       " · components: " (:component-count receipt)
                       " · intersection-closed: " (:intersection-closed? receipt)))
      (htxt 30 76 (str "input-sha256 " (:input-sha256 receipt)) :size 10)
      (apply str
             (for [{:keys [from to tokens]} (sort-by (comp pr-str :from) (:need-edges receipt))
                   :let [[x1 _] (positions from) [x2 _] (positions to)
                         y1 110 y2 110]]
               (arrow (+ x1 boxw) (+ y1 20) x2 (+ y2 20)
                      :label (str/join " " (map tok-short tokens)))))
      (apply str
             (for [p prec
                   :let [[x y] (positions (:id p))
                         produces (str/join " " (map tok-label (:produces p)))]]
               (box x y boxw boxh
                    (concat (wrap (str (:id p)) 38)
                            ["produces:"]
                            (wrap produces 38)))))
      (htxt 30 (+ 110 boxh 40)
            (str "support sets (self + ancestors, by containment): "
                 (str/join "  "
                           (for [p prec]
                             (str (second (str/split (str (:id p)) #"/"))
                                  " -> {" (count (get (:supports receipt) (:id p))) "}"))))
            :size 11)
      (htxt 30 (+ 110 boxh 60)
            "Every pair of support sets is comparable by containment: a chain, not a semilattice (cascade_structure.clj classify-family)."
            :size 11)))))

(defn fig4-singletons-0922 [r]
  (let [cands (get-in r [:decision :selection-certificate :candidates])
        argmax (get-in r [:decision :selection-law :per-policy-argmax])
        chosen (get-in argmax [:action :id])
        n (count cands) boxw 340 boxh 120 gapx 60
        width (+ 60 (* n (+ boxw gapx))) height 300]
    (hsvg
     width height
     (str
      (htxt 30 36 "The three candidates of 2026-09-22 (tick-run-record-2026-09-22-1790053967): three singletons"
            :size 15 :weight "bold")
      (htxt 30 58 "Each candidate is one pattern with one produces token; no wires exist between limbs because there are no second limbs."
            :size 11)
      (apply str
             (for [[i c] (map-indexed vector cands)
                   :let [x (+ 30 (* i (+ boxw gapx))) y 90
                         p (first (get-in c [:id :precedence]))
                         selected? (= (get-in c [:id :id]) chosen)]]
               (str (box x y boxw boxh
                         (concat [(str (get-in c [:id :id]) "  target: " (get-in c [:id :target]))]
                                 (wrap (str (:id p)) 44)
                                 ["produces:"]
                                 (wrap (str/join " " (map (fn [t] (str (second t))) (:produces p))) 44)
                                 [(if selected?
                                    (str "SELECTED (per-policy argmax, p = " (:probability argmax) ")")
                                    "not selected")])
                         :fill (if selected? "#dceee3" "#f6f7f8")
                         :stroke (if selected? "#287447" "#68717b")))))
      (htxt 30 (+ 90 boxh 36)
            "Three singletons: the 09-22 field contained no multi-limb cascade at all (fix-5: only 3 hand-written candidates)."
            :size 11)))))

(defn fig5-hypothetical-semilattice [r]
  (let [a (action-0923 r)
        prec (:precedence a)]
    (hsvg
     980 380
     (str
      (htxt 30 36 "HYPOTHETICAL — a semilattice cascade on the same target (not on any record)"
            :size 15 :weight "bold")
      (htxt 30 58 "Drawn from the same tokens as the 09-23 C2 cascade to show the shape the code never recorded: two independent limbs"
            :size 11)
      (htxt 30 74 "converging on one acceptance. Supports {A,C}, {B,C}, {C} overlap without containment and are intersection-closed:"
            :size 11)
      (htxt 30 90 "classify-family's :semilattice."
            :size 11)
      ;; Limb A
      (box 40 110 300 64 (concat (wrap (str (:id (first prec))) 38) ["limb A (as recorded)"]))
      (box 420 90 300 64 (concat (wrap (str (:id (second prec))) 38) ["limb A cont. (as recorded)"]))
      ;; Limb B (hypothetical)
      (box 40 240 300 64 (concat (wrap "HYPOTHETICAL: a pattern citing the ticket precedent" 38)
                                 ["produces: calibration-evidence"])
           :dash "6 4" :fill "#fdf6ec")
      ;; Convergence
      (box 420 260 300 64 (concat (wrap (str (:id (last prec))) 38)
                                  ["guard needs BOTH limbs' tokens" "produces: restoration-accepted"]))
      (arrow 340 142 420 122 :label "split-declared-valid")
      (arrow 340 272 420 292 :dash "6 4" :label "calibration-evidence")
      (htxt 420 246 "convergence: the acceptance guard consumes the union of two independent limbs" :size 10)
      (htxt 30 356 "What would REQUIRE this shape: one acceptance token whose guard consumes tokens no single chain produces."
            :size 11)))))

(defn fig2-pipeline [r]
  (let [a (action-0923 r)
        receipt (:construction-receipt a)
        stages [["declared source files" "resources/wm/cascade-sources/*.edn" "live"]
                ["cascade_sources.clj" "sources: files -> declared sources, warrant-pinned" "live"]
                ["cascade_problems.clj" "assemble: sources -> cascade problems" "live"]
                ["cascade_proposals.clj + interpretation_construction.clj" "proposals -> interpretations" "live"]
                ["construction.clj / war_machine.clj" "admit candidates with full family evidence" "live"]
                ["policy.clj + cascade_selection.clj" "score the assembled field: sigma(log E - F - G/beta)" "live"]
                ["full_loop_runner.clj construct-selected-action" "thread the selected action onto the click" "live"]]
        offpath [["cascade_policy.clj organise" "exists, NOT on the serving path (fix-9)"]
                 ["cascade_prior.clj canonical-semilattice" "exists, NOT on the serving path"]
                 ["cascade_structure.clj receipt" "record-only; computes shape, never selects"]
                 ["cascade_order_check.clj" "admission check, not an assembler"]]
        width 1080 height 640 y0 100 step 68]
    (hsvg
     width height
     (str
      (htxt 30 36 "Where cascades come from: the assembly pipeline as it runs, with the 09-23 record's numbers"
            :size 15 :weight "bold")
      (htxt 30 58 (str "In the 09-23 record the field is ONE admitted candidate (:C2, four limbs), construction-receipt :kind "
                       (:kind receipt) " :by \"" (:by receipt) "\" :date \"" (:date receipt) "\".")
            :size 11)
      (htxt 30 74 (str "Reading: \"" (subs (:reading receipt) 0 (min 110 (count (:reading receipt)))) "…\"") :size 10)
      (apply str
             (for [[i [title sub live]] (map-indexed vector stages)
                   :let [y (+ y0 (* i step))]]
               (str (box 40 y 560 52 [title sub])
                    (htxt 620 (+ y 30) live :size 10)
                    (when (< i (dec (count stages)))
                      (arrow 320 (+ y 52) 320 (+ y step))))))
      (htxt 700 110 "Off the serving path" :weight "bold")
      (apply str
             (for [[i [title sub]] (map-indexed vector offpath)
                   :let [y (+ 130 (* i 64))]]
               (box 700 y 340 52 (into [title] (wrap sub 46)) :fill "#fdf6ec" :dash "4 3")))
      (htxt 40 (+ y0 (* (count stages) step) 30)
            "No namespace is named for assembly: it is spread across sources/problems/proposals/interpretation-construction/construction."
            :size 11)))))

(defn fig6-narrative [token-outcome]
  (let [prediction (:prediction token-outcome)
        a (get-in prediction [:action])
        receipt (structure/receipt a)
        outcomes (mapv (fn [row]
                         {:token (:token row)
                          :predicted (some #(when (= (:token row) (:token %)) (:predicted %))
                                           (:wanted prediction))
                          :observed (get-in row [:result :observed])})
                       (:measurements token-outcome))]
    (figures/cascade-svg
     {:target (:target a)
      :patterns (:precedence a)
      :outcomes outcomes
      :shape (:shape receipt)
      :prediction-source
      (str "machinery-72 attempt-002: s0 = {admission/task-stated}; wanted restoration-accepted (predicted 1). "
           "Limb 1 fired (split-declared-valid observed true); limb 2's product was never observed, "
           "so limbs 3-4 could not fire. artifact " (:artifact-sha token-outcome))})))

(defn generate! []
  (let [r0923 (read-record record-0923)
        r0922 (read-record record-0922)
        h0923 (sha8 record-0923)
        h0922 (sha8 record-0922)
        tok2 (read-record (str machinery-72 "/attempt-002/retained/token-outcome.edn"))
        htok2 (sha8 (str machinery-72 "/attempt-002/retained/token-outcome.edn"))]
    (spit-svg (str "fig1-cascade-0923-" h0923 ".svg") (fig1-cascade-0923 r0923))
    (spit-svg (str "fig2-pipeline-" h0923 ".svg") (fig2-pipeline r0923))
    (spit-svg (str "fig3-order-0923-" h0923 ".svg") (fig3-order-0923 r0923))
    (spit-svg (str "fig4-singletons-0922-" h0922 ".svg") (fig4-singletons-0922 r0922))
    (spit-svg (str "fig5-hypothetical-semilattice-" h0923 ".svg") (fig5-hypothetical-semilattice r0923))
    (spit-svg (str "fig6-narrative-machinery72-" htok2 ".svg") (fig6-narrative tok2))))
