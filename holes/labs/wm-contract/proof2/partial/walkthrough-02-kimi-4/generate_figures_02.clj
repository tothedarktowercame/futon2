;; generate_figures_02.clj — regenerate every figure in 02-selection-law.md
;; from the records. Run from the futon2 checkout root:
;;
;;   clojure -M -i holes/labs/wm-contract/walkthroughs/generate_figures_02.clj \
;;           -e "(generate-figures-02/generate!)"
;;
;; Read-only: reads run records, writes only SVGs beside this script.
(ns generate-figures-02
  (:require [clojure.edn :as edn]
            [clojure.string :as str])
  (:import [java.security MessageDigest]))

(def here "holes/labs/wm-contract/walkthroughs")
(def record-two "data/wm-runs/tick-run-record-2026-09-23-1790199409.edn")
(def record-one "data/wm-runs/tick-run-record-2026-09-23-1790131591.edn")

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

;; Exact binary decode of an IEEE-754 double to a rational (SPEC-N §3: the
;; decoded recorded masses are rational arithmetic, never a tolerance).
(defn- pow2 [e] (.shiftLeft (biginteger 1) (int e)))
(defn double->rational [d]
  (let [bits (Double/doubleToRawLongBits (double d))
        sign (if (zero? (bit-and bits (bit-shift-left 1 63))) 1 -1)
        exp (bit-and 0x7FF (bit-shift-right bits 52))
        frac (bit-and bits 0xFFFFFFFFFFFFF)]
    (cond
      (= exp 0x7FF) :non-finite
      (zero? exp) (/ (* sign frac) (pow2 1074))
      :else (let [mantissa (+ (bigint frac) 0x10000000000000)
                  e (- exp 1075)]
              (if (neg? e)
                (/ (* sign mantissa) (pow2 (- e)))
                (* sign mantissa (pow2 e)))))))

;; ---------------------------------------------------------------------------
;; svg helpers (same style as walkthrough 01)
;; ---------------------------------------------------------------------------

(defn- esc [s]
  (-> (str s) (str/replace "&" "&amp;") (str/replace "<" "&lt;") (str/replace ">" "&gt;")))

(defn- htxt [x y s & {:keys [size anchor weight mono] :or {size 12 anchor "start"}}]
  (str "<text x=\"" x "\" y=\"" y "\" font-size=\"" size "\" text-anchor=\"" anchor "\""
       (when weight (str " font-weight=\"" weight "\""))
       " font-family=\"" (if mono "monospace" "system-ui,sans-serif") "\">" (esc s) "</text>"))

(defn- box [x y w h fill stroke & {:keys [dash]}]
  (str "<rect x=\"" x "\" y=\"" y "\" width=\"" w "\" height=\"" h "\" rx=\"5\""
       " fill=\"" fill "\" stroke=\"" stroke "\""
       (when dash (str " stroke-dasharray=\"" dash "\"")) "/>"))

(defn- arrow [x1 y1 x2 y2 & {:keys [dash label]}]
  (str "<path d=\"M" x1 "," y1 " L" x2 "," y2 "\" fill=\"none\" stroke=\"#68717b\""
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

(defn law [r] (get-in r [:decision :selection-law]))
(defn candidates [r] (get-in r [:decision :selection-certificate :candidates]))

(defn score-row [r c]
  (let [beta (:beta (law r))]
    {:id (get-in c [:id :id])
     :habit (:habit c) :g (:g c) :f (:f c) :f-status (:f-status c)
     :log-habit (Math/log (double (:habit c)))
     :f-term (if (= :not-supplied (:f-status c)) 0.0 (- (double (:f c))))
     :g-term (- (/ (double (:g c)) (double beta)))
     :beta beta}))

;; ---------------------------------------------------------------------------
;; Figures
;; ---------------------------------------------------------------------------

(defn fig1-field [r-two r-one]
  (let [rows-two (map #(score-row r-two %) (candidates r-two))
        rows-one (map #(score-row r-one %) (candidates r-one))
        all (concat (map #(assoc % :record "1790199409 (two candidates)") rows-two)
                    (map #(assoc % :record "1790131591 (one candidate)") rows-one))
        width 1080 height (+ 190 (* 54 (count all))) x0 40]
    (hsvg
     width height
     (str
      (htxt x0 36 "The field as scored — habit, G, and the F that was never there" :size 15 :weight "bold")
      (htxt x0 58 "score = log(habit) + 0.0  - G/beta — the law that RAN. The docstring's sigma(log E - F - gamma G) requires an F no record supplies."
            :size 11)
      (htxt x0 76 "The 0.0 is cascade_selection.clj:112: \"Missing prefix contributes no term, not a measured F=0.\" beta = 1 on both records."
            :size 11)
      (apply str
             (for [[i row] (map-indexed vector all)
                   :let [y (+ 100 (* 54 i))]]
               (str (box x0 y (- width 80) 46 "#f6f7f8" "#68717b")
                    (htxt (+ x0 10) (+ y 18) (str (:id row) "  [" (:record row) "]") :weight "bold")
                    (htxt (+ x0 10) (+ y 36)
                          (str "habit " (:habit row)
                               "  G " (:g row)
                               "  F " (pr-str (:f row)) " (" (name (:f-status row)) ")"
                               "  ->  log(habit) = " (format "%.4f" (:log-habit row))
                               "  + 0.0"
                               "  - G/beta = " (format "%.4f" (:g-term row))
                               "  = " (format "%.4f" (+ (:log-habit row) (:f-term row) (:g-term row)))))
                    )))
      (htxt x0 (+ 110 (* 54 (count all)) 12)
            "On the one-candidate record the posterior is {1.0} by construction — it cannot witness multi-candidate behaviour (SPEC-N section 1)."
            :size 10)))))

(defn fig2-posterior [r]
  (let [posterior (:posterior (law r))
        entries (vec posterior)
        masses (mapv (comp double->rational second) entries)
        sum (reduce + masses)
        specn-sum (+ 1 (/ 7 (pow2 55)))
        width 1080 height 430 max-mass (apply max (map second entries))]
    (hsvg
     width height
     (str
      (htxt 40 36 "The posterior as recorded — two hex doubles, decoded exactly" :size 15 :weight "bold")
      (htxt 40 58 "Key path [:decision :selection-law :posterior]; keys are the candidate id payloads C1 and C2." :size 11)
      (apply str
             (for [[i [k v]] (map-indexed vector entries)
                   :let [y (+ 90 (* 70 i)) w (int (* 600 (/ (double v) max-mass)))]]
               (str (htxt 40 (+ y 14) (str (:id k)) :weight "bold")
                    (box 120 y w 34 "#dceee3" "#287447")
                    (htxt (+ 130 w) (+ y 16) (str v))
                    (htxt (+ 130 w) (+ y 32) (Double/toHexString (double v)) :size 10 :mono true))))
      (htxt 40 260 (str "Exact decode (mantissa x 2^e, no tolerance):") :weight "bold")
      (apply str
             (for [[i [[k _] m]] (map-indexed vector (map vector entries masses))]
               (htxt 40 (+ 282 (* 20 i)) (str (:id k) " = " m) :mono true :size 11)))
      (htxt 40 (+ 282 (* 20 (count masses)))
            (str "sum = " sum " = 1 + " (- sum 1)) :mono true :size 11)
      (htxt 40 (+ 306 (* 20 (count masses)))
            (str "SPEC-N section 1: 1 + 7/2^55 = " specn-sum
                 (if (= sum specn-sum) "   — REPRODUCED exactly." "   — REFUTED.")) :mono true :size 11 :weight "bold")
      (htxt 40 (+ 330 (* 20 (count masses)))
            "The recorded masses sum to 1 + 7/2^55, not 1: two roundings visible in the record itself (an arithmetic observation, not a claim of causal error)."
            :size 10)))))

(defn fig3-marginal [r]
  (let [law (law r)
        marginal (:action-marginal law)
        argmax (get-in law [:per-policy-argmax])
        winner (first (apply max-key val marginal))]
    (hsvg
     1080 360
     (str
      (htxt 40 36 "The action marginal — one policy per action, so the sum is a lookup" :size 15 :weight "bold")
      (htxt 40 58 "bayes-choice (cascade_selection.clj:131) sums posterior mass per FIRST ACTION and argmaxes the sums; the per-policy argmax is recorded beside it."
            :size 11)
      (apply str
             (for [[i [action mass]] (map-indexed vector marginal)
                   :let [y (+ 100 (* 90 i))
                         cand (some (fn [[k v]] (when (= v mass) k)) (:posterior law))]]
               (str (box 40 y 260 56 "#f6f7f8" "#68717b")
                    (htxt 50 (+ y 22) (str "candidate " (:id cand)) :weight "bold")
                    (htxt 50 (+ y 40) (str "posterior mass " mass))
                    (arrow 300 (+ y 28) 430 (+ y 28) :label "first action")
                    (box 430 y 330 56 (if (= action winner) "#dceee3" "#f6f7f8")
                         (if (= action winner) "#287447" "#68717b"))
                    (htxt 440 (+ y 22) (str "first action " (or (:id action) action)) :size 11)
                    (htxt 440 (+ y 40) (str "marginal mass " mass) :size 11)
                    (when (= action winner)
                      (htxt 780 (+ y 32) "<- argmax" :weight "bold")))))
      (htxt 40 310 (str "Each candidate projects to a DISTINCT first action, so every action's marginal is exactly one policy's mass: "
                        "the argmax is a one-policy action, not a sum. (When several policies share a first action, their masses would add — "
                        "not shown on this record.)") :size 10)
      (htxt 40 330 (str "per-policy argmax: " (get-in argmax [:action :id]) " at " (:probability argmax)
                        " — the two argmaxes agree here.") :size 10)))))

(defn generate! []
  (let [r-two (read-record record-two)
        r-one (read-record record-one)
        h-two (sha8 record-two)
        h-one (sha8 record-one)]
    (spit-svg (str "fig1-field-0923-" h-two "-" h-one ".svg") (fig1-field r-two r-one))
    (spit-svg (str "fig2-posterior-0923-" h-two ".svg") (fig2-posterior r-two))
    (spit-svg (str "fig3-marginal-0923-" h-two ".svg") (fig3-marginal r-two))))
