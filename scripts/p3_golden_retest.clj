;; P3 golden re-test harness — M-action-vocabulary
;; Scores sealed operator picks (golden rounds 1-2) under advance-only (flag off)
;; and v1 move-class-intensity (flag in harness only). Pure scoring, no live env.
(require '[futon2.aif.move-class-intensity :as mci]
         '[clojure.data.json :as json]
         '[clojure.string :as str])

;; =============================================================================
;; STEP 1: THE DENOMINATOR — the candidate decks (FIXED BEFORE SCORING)
;; =============================================================================
;; Round 1 deck: 13 moves in wm-freeze-2026-06-12-00 (12 missions + 1 no-op)
;; Round 2 deck: 12 moves in wm-freeze-2026-06-12-06 (12 missions, all advance)
;; The no-op (centre-mess, rank 81) is a LEGITIMATE deck member — it represents
;; the "do nothing" option the operator could have picked. It stays in the
;; denominator. Under advance-only it scores 0.71 (its |step-score-delta|); under v1 its
;; action-class is nil (mci/action-class returns nil for :no-op) so its
;; intensity is 0.0. This is honest — a no-op has no move-class value.

(def circumstances
  (json/read-str (slurp "/home/joe/code/futon2/holes/labs/E-cascade-sampler-sampler/s4/circumstances-v0.json")
                 :key-fn keyword))

(defn get-circ [id] (first (filter #(= (:id %) id) circumstances)))

(def round1-circ (get-circ "wm-freeze-2026-06-12-00"))
(def round2-circ (get-circ "wm-freeze-2026-06-12-06"))

;; Merge step-score-delta from move level onto action map for the advance-only baseline
;; Also keywordize :type (JSON values are strings; mci/action-class expects keywords)
(defn actions-with-dg [circ]
  (for [m (:moves circ)]
    (let [sa (or (:source_action m) {:type "unknown"})
          dg (:step-score-delta m)]
      (-> sa
          (assoc :step-score-delta (or dg 0.0))
          (update :type #(if (string? %) (keyword %) %))))))

(def r1-deck (actions-with-dg round1-circ))
(def r2-deck (actions-with-dg round2-circ))

;; =============================================================================
;; SEALED OPERATOR PICKS (from golden-selections-v0.edn)
;; =============================================================================
(def round1-picks
  ["M-first-flights" "M-agency-hardening" "M-superpod-mark3"
   "M-autoclock-in" "M-fulab-wiring-survey"])

(def round2-picks
  ["M-a-sorry-enterprise" "M-bounded-in-flight-state" "M-futonzero-mvp"
   "M-learning-loop" "M-patterns-done-right"
   "M-war-machine-first-outing-expectations" "M-web-arxana-missions"
   "M-essay-corpus-substrate"])

;; =============================================================================
;; SCORING
;; =============================================================================

(defn target-of [action]
  (or (:target action) (str "no-op:" (:type action))))

;; Baseline: advance-only score = |step-score-delta| (more negative step-score-delta = higher value)
(defn advance-only-score [action]
  (Math/abs (double (or (:step-score-delta action) 0.0))))

;; v1: move-class-intensity value
(defn v1-score [action]
  (let [int (mci/intensity action)]
    (double (or (:value int) 0.0))))

(defn score-deck [deck score-fn]
  (let [scored (for [a deck]
                 {:target (target-of a)
                  :score (score-fn a)
                  :open-holes (:open-hole-count a)
                  :move-type (:type a)})
        sorted (sort-by (comp - :score) scored)
        n (count sorted)]
    (map-indexed (fn [idx item]
                   (assoc item
                          :rank (inc idx)
                          :quartile (int (inc (Math/floor (* 4.0 (/ (double idx) n)))))))
                 sorted)))

(defn find-in-table [table target]
  (first (filter #(= (:target %) target) table)))

;; =============================================================================
;; RUN
;; =============================================================================

(def r1-baseline (score-deck r1-deck advance-only-score))
(def r1-v1 (score-deck r1-deck v1-score))
(def r2-baseline (score-deck r2-deck advance-only-score))
(def r2-v1 (score-deck r2-deck v1-score))

(defn median [coll]
  (let [s (sort coll) n (count s) mid (quot n 2)]
    (if (odd? n) (nth s mid) (/ (+ (nth s (dec mid)) (nth s mid)) 2.0))))

;; =============================================================================
;; REPORT
;; =============================================================================

(defn pick-rows [picks baseline v1 deck-size]
  (str/join "\n"
            (for [t picks]
              (let [b (find-in-table baseline t)
                    v (find-in-table v1 t)]
                (format "  %-45s | advance-only: rank %2d/%d Q%d (%.3f) | v1: rank %2d/%d Q%d (%.3f)"
                        t
                        (:rank b) deck-size (:quartile b) (:score b)
                        (:rank v) deck-size (:quartile v) (:score v))))))

(println "================================================================")
(println "P3 GOLDEN RE-TEST — M-action-vocabulary")
(println "Scorer: zai-10 (blinded: never opened codex implementation internals)")
(println "================================================================")

(println (str "\n--- ROUND 1 (deck=" (count r1-deck) " candidates) ---"))
(println "DENOMINATOR: 13 moves (12 advance-mission + 1 no-op). No-op kept.")
(println (pick-rows round1-picks r1-baseline r1-v1 (count r1-deck)))
(println (format "\n  Median rank: advance-only %.1f | v1 %.1f (top-quartile threshold: %.1f)"
                 (double (median (map #(:rank (find-in-table r1-baseline %)) round1-picks)))
                 (double (median (map #(:rank (find-in-table r1-v1 %)) round1-picks)))
                 (* 0.25 (count r1-deck))))

(println (str "\n--- ROUND 2 (deck=" (count r2-deck) " candidates) ---"))
(println "DENOMINATOR: 12 moves (all advance-mission).")
(println (pick-rows round2-picks r2-baseline r2-v1 (count r2-deck)))
(println (format "\n  Median rank: advance-only %.1f | v1 %.1f (top-quartile threshold: %.1f)"
                 (double (median (map #(:rank (find-in-table r2-baseline %)) round2-picks)))
                 (double (median (map #(:rank (find-in-table r2-v1 %)) round2-picks)))
                 (* 0.25 (count r2-deck))))

;; =============================================================================
;; STEP 2 CHECK: does baseline REPRODUCE the historical mis-ranking?
;; =============================================================================
;; Historical: advance-only could not rank operator picks well because
;; it prices only advancement. The saturated missions (agency-hardening with
;; resolvedness ~done, fulab-wiring-survey with 0 open holes) should rank LOW
;; under advance-only because their |step-score-delta| is small (saturated = low advance value).
(println "\n--- BASELINE REPRODUCTION CHECK ---")
(println "Historical finding: operator gold is portfolio-shaped (quick wins + close-outs)")
(println "but advance-only prices only advancement. Saturated missions (low |step-score-delta|)")
(println "should rank poorly under advance-only despite operator valuing them as close-outs.")

;; =============================================================================
;; STEP 5: PORTFOLIO vs SOLO-SUM (codex's pure bundle scorer)
;; =============================================================================
;; The operator picked Bundle B in round 2. We score it as a portfolio
;; (mci/score-bundle) vs solo-sum, under both vocabularies.
(def r2-pick-actions (filter #(some #{(target-of %)} round2-picks) r2-deck))

(def r2-bundle-v1 (mci/score-bundle r2-pick-actions))
(def r2-solo-sum-v1 (reduce + 0.0 (map v1-score r2-pick-actions)))

;; For advance-only, solo-sum is just sum of |step-score-delta|
(def r2-solo-sum-baseline (reduce + 0.0 (map advance-only-score r2-pick-actions)))

(println "\n--- STEP 5: PORTFOLIO vs SOLO-SUM (round 2 operator picks) ---")
(println (format "  Advance-only solo-sum: %.3f" r2-solo-sum-baseline))
(println (format "  v1 solo-sum:           %.3f" r2-solo-sum-v1))
(println (format "  v1 portfolio score:    %.3f (diversity-x %.2f, classes %s)"
                 (:score r2-bundle-v1)
                 (:diversity-multiplier r2-bundle-v1)
                 (str (:classes r2-bundle-v1))))
(println (format "  Portfolio vs solo-sum delta: %.3f" (- (:score r2-bundle-v1) r2-solo-sum-v1)))

;; =============================================================================
;; VERDICT
;; =============================================================================
(def r1-v1-median (median (map #(:rank (find-in-table r1-v1 %)) round1-picks)))
(def r2-v1-median (median (map #(:rank (find-in-table r2-v1 %)) round2-picks)))
(def r1-threshold (* 0.25 (count r1-deck)))
(def r2-threshold (* 0.25 (count r2-deck)))

(println "\n================================================================")
(println "VERDICT (target: operator picks at top-quartile MEDIAN under v1)")
(println "================================================================")
(println (format "Round 1: v1 median rank %.1f / %d, top-quartile threshold %.1f => %s"
                 (double r1-v1-median) (count r1-deck) (double r1-threshold)
                 (if (<= r1-v1-median r1-threshold) "PASS" "FAIL")))
(println (format "Round 2: v1 median rank %.1f / %d, top-quartile threshold %.1f => %s"
                 (double r2-v1-median) (count r2-deck) (double r2-threshold)
                 (if (<= r2-v1-median r2-threshold) "PASS" "FAIL")))
(println "================================================================")
