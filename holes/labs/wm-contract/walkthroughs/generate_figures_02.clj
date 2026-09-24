;; generate_figures_02.clj — regenerate every figure in 02-selection-law.md
;; from the records. Run from the futon2 checkout root:
;;
;;   clojure -M -i holes/labs/wm-contract/walkthroughs/generate_figures_02.clj \
;;           -e "(generate-figures-02/generate!)"
;;
;; Read-only: reads run records, writes only SVGs beside this script. Every
;; figure file name carries the short sha256 of the record it was drawn from.
;; Where a figure shows what the code computes on a record (the exact decode,
;; the law receipt, the Bayes choice), it calls the code — checks.proof2-numbers
;; and futon2.aif.cascade-selection — in this fresh process, on the rows the
;; record already holds. Nothing is written back to any record.
(ns generate-figures-02
  (:require [checks.proof2-numbers :as num]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [futon2.aif.cascade-selection :as sel])
  (:import [java.security MessageDigest]))

(def here "holes/labs/wm-contract/walkthroughs")
(def record-two "data/wm-runs/tick-run-record-2026-09-23-1790199409.edn")
(def record-one "data/wm-runs/tick-run-record-2026-09-23-1790131591.edn")
(def record-0922 "data/wm-runs/tick-run-record-2026-09-22-1790053967.edn")

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
;; Exact decode. The record holds plain doubles; CERT-S's carrier is the
;; #wm/double hex form. Double/toHexString is that form, Double/parseDouble
;; must give the same double back (checked), and checks.proof2-numbers
;; (commit 604ecfd7, NUM-R) turns the hex into the exact dyadic rational.
;; ---------------------------------------------------------------------------

(defn- pow2 [e] (.shiftLeft (biginteger 1) (int e)))

(defn hex-of [d] (Double/toHexString (double d)))

(defn double->rational [d]
  (let [hex (hex-of d)]
    (assert (= (double d) (Double/parseDouble hex)) "hex must round-trip")
    (:rational (num/decode-exact (clojure.lang.TaggedLiteral/create 'wm/double hex)))))

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
(defn cid [c] (get-in c [:id :id]))

;; policy.clj:403-404 builds action-of as {candidate-id -> cascade-first-action},
;; and cascade-first-action (policy.clj:147) is the chain head: precedence 0.
(defn action-of [r]
  (into {} (map (fn [c] [(:id c) (get-in c [:id :precedence 0])])) (candidates r)))

(defn head-id [action] (:id action))

(defn score-row [r c]
  (let [beta (:beta (law r))]
    {:id (cid c)
     :habit (:habit c) :g (:g c) :f (:f c) :f-status (:f-status c)
     :log-habit (Math/log (double (:habit c)))
     ;; cascade_selection.clj:112, verbatim semantics
     :f-term (if (= :not-supplied (:f-status c)) 0.0 (- (double (:f c))))
     :g-term (- (/ (double (:g c)) (double beta)))
     :beta beta}))

(defn- key-anywhere?
  "True if k appears as a map key at any depth of x. Used to state, from the
   record itself, that the F-ABS receipt is not on it."
  [x k]
  (cond (map? x) (or (contains? x k) (some #(key-anywhere? % k) (vals x)))
        (sequential? x) (some #(key-anywhere? % k) x)
        :else false))

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
      (htxt x0 58 "score = log(habit) + 0.0 - G/beta — the law that RAN. The docstring's habit * exp(-F - G/beta) (cascade_selection.clj:63-65) needs an F no record supplies."
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
                               "  = " (format "%.4f" (+ (:log-habit row) (:f-term row) (:g-term row))))))))
      (htxt x0 (+ 110 (* 54 (count all)) 12)
            "On the one-candidate record the posterior is {1.0}: SPEC-N section 1 says it cannot establish multi-candidate numerical stability."
            :size 10)))))

(defn fig2-posterior [r]
  (let [posterior (:posterior (law r))
        entries (vec posterior)
        masses (mapv (comp double->rational second) entries)
        sum (reduce + masses)
        specn-sum (+ 1 (/ 7 (pow2 55)))
        replay (sel/selection-posterior {:beta (:beta (law r)) :candidates (candidates r)})
        width 1080 height 450 max-mass (apply max (map second entries))]
    (hsvg
     width height
     (str
      (htxt 40 36 "The posterior as recorded — two doubles, decoded exactly" :size 15 :weight "bold")
      (htxt 40 58 "Key path [:decision :selection-law :posterior]; keys are the candidate id maps (:kind :cascade-candidate, :id C1 / C2)." :size 11)
      (apply str
             (for [[i [k v]] (map-indexed vector entries)
                   :let [y (+ 90 (* 70 i)) w (int (* 600 (/ (double v) max-mass)))]]
               (str (htxt 40 (+ y 14) (str (:id k)) :weight "bold")
                    (box 120 y w 34 "#dceee3" "#287447")
                    (htxt (+ 130 w) (+ y 16) (str v))
                    (htxt (+ 130 w) (+ y 32) (hex-of v) :size 10 :mono true))))
      (htxt 40 260 "Exact decode by checks.proof2-numbers/decode-exact (NUM-R, 604ecfd7) of Double/toHexString; Double/parseDouble round-trips:" :weight "bold")
      (apply str
             (for [[i [[k _] m]] (map-indexed vector (map vector entries masses))]
               (htxt 40 (+ 282 (* 20 i)) (str (:id k) " = " m) :mono true :size 11)))
      (htxt 40 (+ 282 (* 20 (count masses)))
            (str "sum = " sum " = 1 + " (- sum 1)) :mono true :size 11)
      (htxt 40 (+ 306 (* 20 (count masses)))
            (str "SPEC-N section 1 / test record-1790199409-posterior-sum: 1 + 7/2^55 = " specn-sum
                 (if (= sum specn-sum) "   — REPRODUCED exactly." "   — REFUTED.")) :mono true :size 11 :weight "bold")
      (htxt 40 (+ 330 (* 20 (count masses)))
            (str "selection-posterior re-run on the recorded candidate rows returns the recorded doubles bit for bit: "
                 (= replay posterior) ". Normalisation ran in floating point; the exact sum of what it wrote is not 1.")
            :size 10)))))

(defn fig3-marginal [r]
  (let [law (law r)
        marginal (:action-marginal law)
        argmax (:per-policy-argmax law)
        action-of (action-of r)
        choice (sel/bayes-choice (:posterior law) action-of)
        winner (:action choice)
        rows (for [c (candidates r)
                   :let [a (get action-of (:id c))]]
               {:cand (cid c) :mass (get (:posterior law) (:id c))
                :action a :marginal (get marginal a)})]
    (hsvg
     1080 380
     (str
      (htxt 40 36 "The action marginal — one policy per action, so the sum is a lookup" :size 15 :weight "bold")
      (htxt 40 58 "bayes-choice (cascade_selection.clj:147-152) sums posterior mass per action key; the key is each cascade's chain head (policy.clj:147, :403-404)."
            :size 11)
      (apply str
             (for [[i row] (map-indexed vector rows)
                   :let [y (+ 100 (* 90 i)) win? (= (:action row) winner)]]
               (str (box 40 y 260 56 "#f6f7f8" "#68717b")
                    (htxt 50 (+ y 22) (str "candidate " (:cand row)) :weight "bold")
                    (htxt 50 (+ y 40) (str "posterior mass " (:mass row)))
                    (arrow 300 (+ y 28) 430 (+ y 28) :label "chain head")
                    (box 430 y 330 56 (if win? "#dceee3" "#f6f7f8") (if win? "#287447" "#68717b"))
                    (htxt 440 (+ y 22) (str "action " (head-id (:action row))) :size 11)
                    (htxt 440 (+ y 40) (str "marginal mass " (:marginal row)) :size 11)
                    (when win? (htxt 780 (+ y 32) "<- bayes-choice" :weight "bold")))))
      (htxt 40 300 (str "Each candidate's head is a distinct key, so every action's marginal is exactly one policy's mass; the argmax is a lookup, "
                        "not a sum. (Several policies sharing one head would add — not on this record.)") :size 10)
      (htxt 40 320 (str "per-policy argmax (policy.clj:437-440): " (get-in argmax [:action :id]) " at " (:probability argmax)
                        " — agrees with the marginal's winner " (head-id winner) " at " (:mass choice) ".") :size 10)
      (htxt 40 340 (str "ticket-queue receipt: :decided-by " (get-in law [:ticket-queue :decided-by])
                        ", :status " (get-in law [:ticket-queue :status])
                        ", one eligible target, stratum posterior = full posterior: "
                        (= (get-in law [:ticket-queue :stratum-posterior]) (:posterior law)) ".") :size 10)))))

(defn fig4-receipt [r-two r-one]
  (let [records [["1790199409" r-two] ["1790131591" r-one]]
        width 1080 height 470]
    (hsvg
     width height
     (str
      (htxt 40 36 "What the F-ABS receipt says when run on the recorded rows — not on either record" :size 15 :weight "bold")
      (htxt 40 58 "law-receipt and f-consumed-record (cascade_selection.clj:177 and :186, commit ad039985) are called here on [:decision :selection-certificate :candidates]."
            :size 11)
      (htxt 40 76 "policy.clj:265 writes the receipt at [:decision :selection-certificate :law-applied]; :274 adds :f-consumed per candidate. Neither key is on these records."
            :size 11)
      (apply str
             (for [[i [label r]] (map-indexed vector records)
                   :let [y (+ 100 (* 170 i))
                         cands (candidates r)
                         receipt (sel/law-receipt cands)
                         consumed (map (fn [c] [(cid c) (sel/f-consumed-record c)]) cands)]]
               (str (box 40 y (- width 80) 150 "#fdf6ec" "#68717b" :dash "6 4")
                    (htxt 50 (+ y 20) (str "record " label " — :law-applied present on record? "
                                           (boolean (key-anywhere? r :law-applied))
                                           "; :f-consumed present? " (boolean (key-anywhere? r :f-consumed)))
                          :weight "bold")
                    (htxt 50 (+ y 44) (str ":law-applied => " (pr-str (update receipt :candidates-without-f #(mapv :id %))))
                          :mono true :size 11)
                    (apply str
                           (for [[j [id fc]] (map-indexed vector consumed)]
                             (htxt 50 (+ y 68 (* 20 j)) (str id " :f-consumed => " (pr-str fc)) :mono true :size 11)))
                    (htxt 50 (+ y 130) "(:candidates-without-f holds the full candidate id maps; shown here by :id only.)" :size 10))))
      (htxt 40 450 "Evidence only: the receipt names the law that ran; it changes no score and refuses nothing (strategy row 27 / F-ABS, PROOF-2-STRATEGY-draft-2026-09-24.md:126,133)."
            :size 10)))))

(defn fig5-tie-0922 [r]
  (let [law (law r)
        posterior (:posterior law)
        marginal (:action-marginal law)
        action-of (action-of r)
        choice (sel/bayes-choice posterior action-of)
        ordered (sort-by (comp str key) marginal)           ; cascade_selection.clj:157
        by-pr (sort-by (comp pr-str key) posterior)         ; policy.clj:439
        rows (for [c (candidates r)]
               {:cand (cid c) :habit (:habit c) :g (:g c)
                :mass (get posterior (:id c)) :rational (double->rational (get posterior (:id c)))
                :head (head-id (get action-of (:id c)))})
        sum (reduce + (map :rational rows))
        width 1080 height 620]
    (hsvg
     width height
     (str
      (htxt 40 36 "2026-09-22-1790053967: two identical doubles, and the tie rule chose" :size 15 :weight "bold")
      (htxt 40 58 "Three singleton candidates; C2 and C3 have the same habit and the same G, so the same score and the same posterior double." :size 11)
      (apply str
             (for [[i row] (map-indexed vector rows)
                   :let [y (+ 84 (* 62 i))]]
               (str (box 40 y (- width 80) 54 "#f6f7f8" "#68717b")
                    (htxt 50 (+ y 18) (str (:cand row) "  habit " (:habit row) "  G " (:g row) "  head " (:head row)) :weight "bold")
                    (htxt 50 (+ y 40) (str "posterior " (:mass row) " = " (hex-of (:mass row)) " = " (:rational row)) :mono true :size 11))))
      (htxt 40 290 (str "exact sum of the three recorded masses = " sum " = 1 + " (- sum 1)) :mono true :size 11)
      (htxt 40 320 "bayes-choice: sort the marginal by (str action) — the printed chain-head map — then keep the first unless a later mass is strictly greater (cascade_selection.clj:157-160):" :size 11 :weight "bold")
      (apply str
             (for [[i [a m]] (map-indexed vector ordered)
                   :let [y (+ 340 (* 20 i))]]
               (htxt 50 y (str i ". " (subs (str a) 0 42) "…   mass " m
                               (when (= a (:action choice)) "   <- kept: first in order, no later mass exceeds it"))
                     :mono true :size 11)))
      (htxt 40 420 (str "recorded: :action-comparison :winner " (get-in law [:action-comparison :winner :action :id])
                        "  :runner-up " (get-in law [:action-comparison :runner-up :action :id])
                        " at the same mass, :decided-by " (get-in law [:action-comparison :decided-by])
                        ", :tie-broken? " (:tie-broken? law) ", :tie-break-rule " (:tie-break-rule law)) :size 10)
      (htxt 40 440 (str "per-policy argmax (policy.clj:437-440) sorts the posterior by pr-str of the candidate id map instead: "
                        (str/join " < " (map (comp str :id key) by-pr))
                        " — so it records " (get-in law [:per-policy-argmax :action :id]) ".") :size 10)
      (htxt 40 460 (str "What was enacted, from the record: :selection-event :policy-key "
                        (pr-str (get-in r [:selection-event :policy-key]))) :size 10)
      (htxt 40 480 (str "[:habit-reinforcement :inputs :comparison :prediction :action :id] = "
                        (get-in r [:habit-reinforcement :inputs :comparison :prediction :action :id])
                        " — the bayes-choice winner, not the per-policy argmax.") :size 10)
      (htxt 40 510 "The two tied candidates C2 and C3 target the same mission; C1 targets another. The mass did not separate C2 from C3; the printed head decided." :size 10)
      (htxt 40 530 (str ":near-tie-threshold on this record: " (pr-str (:near-tie-threshold law))
                        " — the policy comparison reports :near-tie? " (get-in law [:policy-comparison :near-tie?])
                        ", contributions " (pr-str (get-in law [:policy-comparison :contributions])) ".") :size 10)
      (htxt 40 560 "This is the live instance of the name-order case SPEC-N section 7 constructs by hand (there with a certified error interval; here the two doubles are bit-identical)." :size 10)))))

(defn generate! []
  (let [r-two (read-record record-two)
        r-one (read-record record-one)
        r-0922 (read-record record-0922)
        h-two (sha8 record-two)
        h-one (sha8 record-one)
        h-0922 (sha8 record-0922)]
    (spit-svg (str "fig1-field-0923-" h-two "-" h-one ".svg") (fig1-field r-two r-one))
    (spit-svg (str "fig2-posterior-0923-" h-two ".svg") (fig2-posterior r-two))
    (spit-svg (str "fig3-marginal-0923-" h-two ".svg") (fig3-marginal r-two))
    (spit-svg (str "fig4-receipt-0923-" h-two "-" h-one ".svg") (fig4-receipt r-two r-one))
    (spit-svg (str "fig5-tie-0922-" h-0922 ".svg") (fig5-tie-0922 r-0922))))
