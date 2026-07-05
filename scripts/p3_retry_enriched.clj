;; P3-retry — M-action-vocabulary ENRICHED-DECK re-test
;; P3-retry — M-action-vocabulary ENRICHED-DECK RE-TEST
;; Usage: clojure -M scripts/p3_retry_enriched.clj
;; Driver: zai-3, 2026-07-05. Design approved by claude-16 (with PASS-TRIVIAL amendment).
;;
;; Scores the golden rounds on ENRICHED decks: original advance-moves + synthesized
;; close/survey candidates, with per-mission metric-matrix substrate (kappa, resolvedness)
;; merged onto the action maps. Three sub-tests:
;;   A — close-candidate discrimination (primary; verdict-bearing)
;;   B — advance-pick robustness under competition (non-regression)
;;   C — survey-candidate discrimination (secondary, informational)
;;
;; Extends p3_golden_retest.clj's scoring approach; does not modify it.
;; Pure scoring — no live env touch. Flag bound in harness only.
(require '[futon2.aif.move-class-intensity :as mci]
         '[clojure.data.json :as json]
         '[clojure.string :as str])

;; =============================================================================
;; DATA LOADING
;; =============================================================================
(def circumstances
  (json/read-str (slurp "/home/joe/code/futon2/holes/labs/E-cascade-sampler-sampler/s4/circumstances-v0.json")
                 :key-fn keyword))

(defn get-circ [id] (first (filter #(= (:id %) id) circumstances)))

(def round1-circ (get-circ "wm-freeze-2026-06-12-00"))
(def round2-circ (get-circ "wm-freeze-2026-06-12-06"))

;; The v1.1 metric matrix — per-mission substrate (kappa, resolvedness, phase)
;; This is the SAME substrate the freeze was built from, at a richer grain.
;; Merging it onto action maps is legitimate: the freeze's source_action lacks
;; explicit resolvedness/kappa (falls back to ohc-only); the metric matrix
;; supplies the substrate-level values the mci formulas are designed to consume.
(def metric-matrix
  (json/read-str (slurp "/home/joe/code/futon2/holes/labs/E-cascade-sampler-sampler/s4/metric-matrix-v1.1.json")
                 :key-fn keyword))

(def mm-intensity (:intensity metric-matrix))

(defn mm-for [target]
  (get mm-intensity (if (keyword? target) target (keyword target))))

;; =============================================================================
;; ACTION MAP ENRICHMENT — merge metric-matrix substrate onto source_action
;; =============================================================================
;; mci/kappa reads :min-kappa or :curvature/min-incident-kappa.
;; mci/resolvedness reads :resolvedness or :resolution-state/resolvedness.
;; The metric matrix carries :min_incident_kappa and :resolvedness.
;; We merge using the key names mci actually reads.

(defn enrich-action [action]
  (if-let [target (:target action)]
    (if-let [mm (mm-for target)]
      (-> action
          (assoc :min-kappa (Math/abs (double (:min_incident_kappa mm))))
          (assoc :kappa (Math/abs (double (:min_incident_kappa mm))))
          (assoc :resolvedness (double (:resolvedness mm)))
          (assoc :resolution-state/resolvedness (double (:resolvedness mm))))
      action)
    action))

;; =============================================================================
;; BASE DECKS (enriched advance-moves from the freeze)
;; =============================================================================
(defn actions-with-dg [circ]
  (for [m (:moves circ)]
    (let [sa (or (:source_action m) {:type "unknown"})
          dg (:delta-g m)]
      (-> sa
          (assoc :delta-g (or dg 0.0))
          (update :type #(if (string? %) (keyword %) %))
          enrich-action))))

(def r1-base-deck (actions-with-dg round1-circ))
(def r2-base-deck (actions-with-dg round2-circ))

;; =============================================================================
;; CANDIDATE SYNTHESIS — close and survey moves with provenance
;; =============================================================================
;; Close candidates: synthesized from saturated missions (ohc=0) in the SAME freeze.
;;   :type :close, inherits substrate fields, delta-g = 0.0 (close has no advance delta-g).
;;   The metric-matrix resolvedness determines close-intensity discrimination.
;;
;; Survey candidates: synthesized from operator-marked survey-first targets.
;;   :type :survey, plus a synthesized staleness signal.

(defn synthesize-close [advance-action]
  (let [target (:target advance-action)
        mm (mm-for target)]
    (-> advance-action
        (assoc :type :close)
        (assoc :delta-g 0.0)
        (assoc :synthesized {:from-freeze "same-freeze"
                             :substrate-field :open-hole-count
                             :rationale "ohc=0 saturated mission -> close candidate"
                             :metric-matrix-merged (some? mm)})
        enrich-action)))

(defn synthesize-survey [advance-action staleness-days]
  (let [target (:target advance-action)
        mm (mm-for target)]
    (-> advance-action
        (assoc :type :survey)
        (assoc :delta-g 0.0)
        (assoc :staleness-days staleness-days)
        (assoc :synthesized {:from-freeze "same-freeze"
                             :substrate-field :open-hole-count
                             :rationale "operator-marked survey-first (legacy) -> survey candidate"
                             :staleness-source "operator-language 'legacy' -> 14d estimate"
                             :metric-matrix-merged (some? mm)})
        enrich-action)))

;; Identify saturated targets (ohc=0) per round for close-candidate synthesis
(defn saturated-targets [deck]
  (filter #(and (= 0 (:open-hole-count %))
                (:target %))
          deck))

(def r1-saturated (saturated-targets r1-base-deck))
(def r2-saturated (saturated-targets r2-base-deck))

;; Synthesize close candidates
(def r1-close-candidates (map synthesize-close r1-saturated))
(def r2-close-candidates (map synthesize-close r2-saturated))

;; Survey candidate: M-patterns-done-right (R2, operator :survey-first, "legacy")
(def survey-target-r2
  (first (filter #(= "M-patterns-done-right" (:target %)) r2-base-deck)))
(def r2-survey-candidate
  (when survey-target-r2
    (synthesize-survey survey-target-r2 14.0)))

;; =============================================================================
;; ENRICHED DECKS
;; =============================================================================
;; Sub-test A+B deck: base advance-moves + close candidates
(def r1-deck-ab (concat r1-base-deck r1-close-candidates))
(def r2-deck-ab (concat r2-base-deck r2-close-candidates))

;; Sub-test C deck: A+B deck + survey candidate (R2 only; R1 has no survey target)
(def r2-deck-abc (if r2-survey-candidate
                   (conj (vec r2-deck-ab) r2-survey-candidate)
                   (vec r2-deck-ab)))

;; =============================================================================
;; OPERATOR DISPOSITIONS (from golden-selections-v0.edn)
;; =============================================================================
(def r1-close-dispositions
  "Targets where the operator indicated a close-move is correct (not advance).
  M-agency-hardening is saturated-by-prose (operator: 'substantially done', 'mark CLOSED')
  but ohc=11 in the freeze — the stale-doc-or-operator-drift edge case.
  M-fulab-wiring-survey is ohc=0 but operator DISQUALIFIES ('legacy, worst') — edge case."
  #{"M-agency-hardening"})

(def r2-close-dispositions
  #{"M-war-machine-first-outing-expectations"})

(def r1-advance-dispositions
  "Targets where the operator indicated an advance-move is correct."
  #{"M-first-flights" "M-superpod-mark3" "M-autoclock-in"})

(def r2-advance-dispositions
  #{"M-a-sorry-enterprise" "M-bounded-in-flight-state" "M-futonzero-mvp"
    "M-web-arxana-missions" "M-learning-loop"})

(def r2-survey-dispositions
  #{"M-patterns-done-right"})

;; Edge-case targets (documented, not counted as pass/fail)
(def r1-edge-cases
  #{"M-fulab-wiring-survey"})  ; saturated but operator says worst

(def r2-edge-cases
  #{"M-essay-corpus-substrate"})  ; operator uncertain, doc says resolvedness 1.0

;; =============================================================================
;; SCORING (same approach as p3_golden_retest.clj)
;; =============================================================================
(defn target-of [action]
  (or (:target action) (str "no-op:" (:type action))))

(defn advance-only-score [action]
  (Math/abs (double (or (:delta-g action) 0.0))))

(defn v1-score [action]
  (let [int (mci/intensity action)]
    (double (or (:value int) 0.0))))

(defn score-deck [deck score-fn]
  (let [scored (for [a deck]
                 {:target (target-of a)
                  :score (score-fn a)
                  :move-type (:type a)
                  :open-holes (:open-hole-count a)
                  :resolvedness (:resolvedness a)
                  :synthesized (:synthesized a)
                  :action a})
        sorted (sort-by (comp - :score) scored)
        n (count sorted)]
    (map-indexed (fn [idx item]
                   (assoc item
                          :rank (inc idx)
                          :quartile (int (inc (Math/floor (* 4.0 (/ (double idx) n)))))))
                 sorted)))

(defn find-in-table [table target]
  (first (filter #(= (:target %) target) table)))

(defn find-close-move
  "Find the synthesized :close move for a target in the scored table."
  [table target]
  (first (filter #(and (= (:target %) target)
                       (= (:move-type %) :close))
                 table)))

(defn find-advance-move
  "Find the :advance-mission move for a target in the scored table."
  [table target]
  (first (filter #(and (= (:target %) target)
                       (= (:move-type %) :advance-mission))
                 table)))

;; =============================================================================
;; MEDIAN HELPER
;; =============================================================================
(defn median [coll]
  (let [s (sort coll) n (count s) mid (quot n 2)]
    (if (odd? n) (nth s mid) (/ (+ (nth s (dec mid)) (nth s mid)) 2.0))))

;; =============================================================================
;; SUB-TEST A: CLOSE-CANDIDATE DISCRIMINATION (primary, verdict-bearing)
;; =============================================================================
;; For each saturated target: compare its close-move rank vs advance-move rank
;; under v1. The close-move should rank higher when resolvedness is high.
;; PASS-TRIVIAL check: do all close-moves score identically?

(defn close-census
  "Report the score distribution within the close class."
  [deck-v1]
  (let [close-moves (filter #(= :close (:move-type %)) deck-v1)
        scores (map :score close-moves)]
    {:n (count close-moves)
     :scores (map (fn [m] {:target (:target m) :score (:score m)
                          :resolvedness (:resolvedness m)
                          :rank (:rank m)}) close-moves)
     :min-score (apply min scores)
     :max-score (apply max scores)
     :all-identical (apply = scores)}))

(defn sub-test-a [circ-label deck close-dispositions edge-cases]
  (let [baseline (score-deck deck advance-only-score)
        v1 (score-deck deck v1-score)
        census (close-census v1)
        deck-size (count deck)]
    {:circ circ-label
     :deck-size deck-size
     :census census
     :targets (for [target (sort (set (map target-of
                                            (filter #(= :close (:type %)) deck))))]
                (let [close-move (find-close-move v1 target)
                      advance-move (find-advance-move v1 target)
                      advance-baseline (find-advance-move baseline target)]
                  {:target target
                   :operator-close (contains? close-dispositions target)
                   :edge-case (contains? edge-cases target)
                   :close-v1-rank (:rank close-move)
                   :close-v1-score (:score close-move)
                   :close-resolvedness (:resolvedness close-move)
                   :advance-v1-rank (:rank advance-move)
                   :advance-v1-score (:score advance-move)
                   :advance-baseline-rank (:rank advance-baseline)
                   :advance-baseline-score (:score advance-baseline)}))}))

(def r1-sub-a (sub-test-a "wm-freeze-2026-06-12-00" r1-deck-ab
                          r1-close-dispositions r1-edge-cases))
(def r2-sub-a (sub-test-a "wm-freeze-2026-06-12-06" r2-deck-ab
                          r2-close-dispositions r2-edge-cases))

;; =============================================================================
;; SUB-TEST B: ADVANCE-PICK ROBUSTNESS (non-regression)
;; =============================================================================
(defn sub-test-b [circ-label deck advance-dispositions]
  (let [v1 (score-deck deck v1-score)
        deck-size (count deck)
        half-threshold (/ deck-size 2.0)]
    {:circ circ-label
     :deck-size deck-size
     :half-threshold half-threshold
     :picks (for [target (sort advance-dispositions)]
              (let [advance-move (find-advance-move v1 target)]
                {:target target
                 :v1-rank (:rank advance-move)
                 :v1-score (:score advance-move)}))}))

(def r1-sub-b (sub-test-b "R1" r1-deck-ab r1-advance-dispositions))
(def r2-sub-b (sub-test-b "R2" r2-deck-ab r2-advance-dispositions))

;; =============================================================================
;; SUB-TEST C: SURVEY-CANDIDATE DISCRIMINATION (secondary, R2 only)
;; =============================================================================
(def r2-sub-c
  (when r2-survey-candidate
    (let [v1 (score-deck r2-deck-abc v1-score)
          survey-move (first (filter #(and (= "M-patterns-done-right" (:target %))
                                           (= :survey (:move-type %))) v1))
          advance-move (find-advance-move v1 "M-patterns-done-right")]
      {:circ "R2"
       :deck-size (count r2-deck-abc)
       :survey-v1-rank (:rank survey-move)
       :survey-v1-score (:score survey-move)
       :advance-v1-rank (:rank advance-move)
       :advance-v1-score (:score advance-move)})))

;; =============================================================================
;; VERDICT LOGIC
;; =============================================================================
;; Sub-test A grades: PASS (real discrimination), PASS-TRIVIAL (arithmetic only), FAIL
;; A passes non-trivially when: close-moves do NOT all tie identically,
;; AND operator-marked close-out targets have close-move rank in top half,
;; AND close-move ranks above advance-move for operator-marked targets.

(defn grade-sub-a [result]
  (let [census (:census result)
        targets (:targets result)
        deck-size (:deck-size result)
        half-threshold (/ deck-size 2.0)]
    (cond
      ;; PASS-TRIVIAL: all close-moves tie identically
      (:all-identical census)
      {:grade :pass-trivial
       :reason (format "All %d close-moves score identically (%.4f); no intra-class discrimination"
                       (:n census) (:max-score census))}
      ;; PASS: at least one operator-close target with close-rank in top half
      ;; AND close-rank < advance-rank
      :else
      (let [passing (filter (fn [t]
                              (and (:operator-close t)
                                   (not (:edge-case t))
                                   (<= (:close-v1-rank t) half-threshold)
                                   (< (:close-v1-rank t) (:advance-v1-rank t))))
                            targets)]
        (if (seq passing)
          {:grade :pass
           :reason (format "%d operator-marked close-out target(s) have close-move in top half and above advance-move"
                           (count passing))}
          {:grade :fail
           :reason "No operator-marked close-out target has close-move in top half above advance-move"})))))

(def r1-a-grade (grade-sub-a r1-sub-a))
(def r2-a-grade (grade-sub-a r2-sub-a))

;; Sub-test B: PASS if median advance-pick rank <= half threshold
(defn grade-sub-b [result]
  (let [picks (:picks result)
        ranks (map :v1-rank picks)
        med (double (median ranks))
        threshold (double (:half-threshold result))]
    {:grade (if (<= med threshold) :pass :fail)
     :median-rank med
     :threshold threshold}))

(def r1-b-grade (grade-sub-b r1-sub-b))
(def r2-b-grade (grade-sub-b r2-sub-b))

;; Overall verdict
(defn overall-verdict [a-grades b-grades]
  (let [a-any-pass (some #(= :pass %) (map :grade a-grades))
        a-any-trivial (some #(= :pass-trivial %) (map :grade a-grades))
        a-any-fail (some #(= :fail %) (map :grade a-grades))
        b-all-pass (every? #(= :pass %) (map :grade b-grades))]
    (cond
      (and a-any-pass b-all-pass) :pass
      (and a-any-trivial (not a-any-pass)) :pass-trivial
      a-any-fail :fail
      :else :pass-trivial)))

(def verdict (overall-verdict [r1-a-grade r2-a-grade] [r1-b-grade r2-b-grade]))

;; =============================================================================
;; PORTFOLIO vs SOLO-SUM (on the enriched deck)
;; =============================================================================
;; The operator's round-2 picks include close-out targets. On the enriched deck,
;; we can form a mixed-class bundle and see if the diversity multiplier fires.

(def r2-mixed-bundle
  (let [v1 (score-deck r2-deck-ab v1-score)
        ;; operator's picks: advance picks + close-out (war-machine-first-outing)
        advance-picks (filter #(some #{(:target %)} r2-advance-dispositions)
                              (filter #(= :advance-mission (:move-type %)) v1))
        close-pick (find-close-move v1 "M-war-machine-first-outing-expectations")
        bundle-actions (map :action (concat advance-picks [close-pick]))]
    (mci/score-bundle bundle-actions)))


;; =============================================================================
;; REPORT
;; =============================================================================
;; =============================================================================
;; REPORT — builds a string (returned as the file's value for proof-eval)
;; =============================================================================

(defn build-sub-a-report [result grade]
  (let [hdr (format "--- SUB-TEST A: CLOSE DISCRIMINATION (%s, deck=%d) ---"
                    (:circ result) (:deck-size result))
        c (:census result)
        census-line (format "  Close-class census: %d moves, scores range [%.4f, %.4f], all-identical=%s"
                            (:n c) (:min-score c) (:max-score c) (:all-identical c))
        target-lines (for [t (:targets result)]
                       (let [marker (cond (:operator-close t) " <== OPERATOR-CLOSE"
                                          (:edge-case t) " <== EDGE-CASE"
                                          :else "")]
                         (format "  %-50s | close: rank %2d/%d (%.4f, res=%.2f) | advance: rank %2d/%d (%.4f)%s"
                                 (:target t)
                                 (:close-v1-rank t) (:deck-size result) (:close-v1-score t) (:close-resolvedness t)
                                 (:advance-v1-rank t) (:deck-size result) (:advance-v1-score t)
                                 marker)))
        grade-line (format "  GRADE: %s — %s" (:grade grade) (:reason grade))]
    (str/join "\n" (concat [hdr census-line] target-lines [grade-line ""]))))

(defn build-sub-b-report [result grade]
  (let [hdr (format "--- SUB-TEST B: ADVANCE-PICK ROBUSTNESS (%s, deck=%d) ---"
                    (:circ result) (:deck-size result))
        pick-lines (for [p (:picks result)]
                     (format "  %-50s | v1 rank %2d/%d (%.4f)"
                             (:target p) (:v1-rank p) (:deck-size result) (:v1-score p)))
        med-line (format "  Median rank: %.1f, half-threshold: %.1f => %s"
                         (:median-rank grade) (:threshold grade) (:grade grade))]
    (str/join "\n" (concat [hdr] pick-lines [med-line ""]))))

(defn build-sub-c-report [result]
  (if (nil? result)
    "  (no survey candidate)"
    (str/join "\n"
              [(format "--- SUB-TEST C: SURVEY DISCRIMINATION (%s, deck=%d) ---"
                       (:circ result) (:deck-size result))
               (format "  M-patterns-done-right survey: rank %2d/%d (%.4f)"
                       (:survey-v1-rank result) (:deck-size result) (:survey-v1-score result))
               (format "  M-patterns-done-right advance: rank %2d/%d (%.4f)"
                       (:advance-v1-rank result) (:deck-size result) (:advance-v1-score result))
               (format "  Survey > Advance? %s"
                       (if (< (:survey-v1-rank result) (:advance-v1-rank result)) "YES" "NO"))
               "  (INFORMATIONAL — staleness signal is synthetic)"
               ""])))

(defn get-report-text []
  (str/join "\n"
    (concat
     ["================================================================"
      "P3-RETRY — M-action-vocabulary ENRICHED-DECK RE-TEST"
      "Driver: zai-3. Design approved by claude-16 (with PASS-TRIVIAL amendment)."
      "================================================================"
      "Substrate: metric-matrix-v1.1 (kappa + resolvedness per mission, merged onto action maps)"
      "Close candidates: ohc=0 targets from same freeze, :type :close"
      "Survey candidates: operator-marked survey-first targets, :type :survey + synthesized staleness"
      ""]
     [(build-sub-a-report r1-sub-a r1-a-grade)
      (build-sub-a-report r2-sub-a r2-a-grade)
      (build-sub-b-report r1-sub-b r1-b-grade)
      (build-sub-b-report r2-sub-b r2-b-grade)
      (build-sub-c-report r2-sub-c)
      (format "--- PORTFOLIO vs SOLO-SUM (R2 mixed bundle: advance picks + close-out) ---")
      (format "  Portfolio score: %.4f (diversity-x %.2f, classes %s)"
              (:score r2-mixed-bundle)
              (:diversity-multiplier r2-mixed-bundle)
              (str (:classes r2-mixed-bundle)))
      ""
      "================================================================"
      "OVERALL VERDICT"
      "================================================================"
      (format "  Sub-test A (R1): %s" (:grade r1-a-grade))
      (format "  Sub-test A (R2): %s" (:grade r2-a-grade))
      (format "  Sub-test B (R1): %s" (:grade r1-b-grade))
      (format "  Sub-test B (R2): %s" (:grade r2-b-grade))
      (format "  OVERALL: %s" verdict)
      "================================================================"
      (case verdict
        :pass "  PASS — v1 close-intensity discriminates correctly on enriched decks."
        :pass-trivial "  PASS-TRIVIAL — v1 seats move classes but cannot discriminate within a class."
        :fail "  FAIL — P3 negative #2. Kill criterion reached.")
      "================================================================"])))

;; Print the report when run as a script.
(println (get-report-text))
