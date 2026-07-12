#!/usr/bin/env bb
;; B1 — marks→γ(mission) via the R14 fold (M-zaif-harness §laptop-side port
;; recon; PZ3 verdict: adapter-only). The fold core is LOADED from the real
;; source (src/futon2/aif/policy_precision.clj), never copied — this script
;; is only the adapter: event shape, (at,id) ordering + dedup, per-cell map,
;; fixed ±0.5 magnitudes, asymmetric admissibility, coerce-state on read.
;;
;; Event sources:
;;   retro — PZ1 final truth (:true? rows), joined to the labeling sheet for
;;           :at/:context; mission attribution = token-grep over context
;;           (PZ2's rule; known ~1/3 loss — counted, not hidden). Gold-
;;           conflicted ids (same doc labeled yes AND no) are EXCLUDED.
;;   live  — b1-live-marks.edn (declared ✘/✓; autoclock attribution).
;;
;; Usage (any cwd):
;;   bb holes/labs/M-zaif-harness/b1_gamma_mission_fold.clj          # fold + write EDN
;;   bb holes/labs/M-zaif-harness/b1_gamma_mission_fold.clj --check  # fold + independent recomputation + asserts
(require '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[babashka.fs :as fs])

(def lab (str (fs/parent (fs/absolutize *file*))))
(defn lab-file [n] (str lab "/" n))

;; The real R14 fold (pure ns, no requires — loads clean in bb).
(load-file (str lab "/../../../src/futon2/aif/policy_precision.clj"))
(alias 'pp 'futon2.aif.policy-precision)

;; ---------------------------------------------------------------------------
;; Adapter constants (PZ3 deltas 4–5; fixed here, pre-Z1, in the prereg spirit)
;; ---------------------------------------------------------------------------
(def perf-magnitude
  "v0 binary signal magnitudes: a single mark may not slam γ to a bound
   (±1.0 would); ±0.5 needs a sustained run. The v1 sorry-discharge ledger
   restores a continuous signal."
  {:correction -0.5 :approval 0.5})

(defn admissible?
  "Asymmetric admissibility (zaif-only, pre-fold): only operator-authored
   events (or a discharged interface-sorry — none in v0) may carry POSITIVE
   perf; agent-authored events may only lower."
  [{:keys [author perf]}]
  (or (neg? perf) (= author "joe")))

(def mission-re
  "PZ2's attribution rule for historical (pre-autoclock-witness) events."
  #"\b([MEC]-[a-z0-9][a-z0-9-]{2,})\b")

(defn missions-of [text] (distinct (map first (re-seq mission-re (str text)))))

;; ---------------------------------------------------------------------------
;; Event extraction
;; ---------------------------------------------------------------------------
(def sheet (:items (edn/read-string (slurp (lab-file "pz1-labeling-sheet.edn")))))
(def truth (:rows (edn/read-string (slurp (lab-file "pz1-final-truth.edn")))))
(def live  (edn/read-string (slurp (lab-file "b1-live-marks.edn"))))

(def ctx-of (into {} (map (fn [i] [(:id i) i])) sheet))

(def conflicted-ids
  ;; same doc id carrying both true and false rows — genuinely disputed; excluded.
  (->> (group-by :id truth)
       (filter (fn [[_ rows]] (> (count (distinct (map :true? rows))) 1)))
       (map key) set))

(def retro-events
  ;; one event per (doc, attributed mission); unattributed docs tracked apart.
  (for [r truth
        :when (:true? r)
        :when (not (conflicted-ids (:id r)))
        :let [item (ctx-of (:id r))
              cells (missions-of (:context item))]
        cell (or (seq cells) ["<unattributed>"])]
    {:id (:id r) :at (:at item) :cell cell
     :perf (perf-magnitude :correction) :mark :correction
     :author "joe" :source :retro :attribution :token-grep}))

(def live-events
  (for [e (:events live)]
    {:id (:id e) :at (:at e) :cell (:mission e)
     :perf (perf-magnitude (:mark e)) :mark (:mark e)
     :author (:author e) :source :live :attribution :autoclock-witness}))

(def events
  (->> (concat retro-events live-events)
       (filter admissible?)
       (sort-by (juxt :at :id :cell))))              ; (at,id) cursor order

(def folded
  ;; per-cell R14 fold, coerce-state guarding every read (PZ3 delta 6).
  (reduce (fn [cells {:keys [cell perf]}]
            (if (= cell "<unattributed>")
              cells
              (update cells cell
                      (fn [st] (pp/update-policy-precision (pp/coerce-state st) perf)))))
          {} events))

(def unattributed (filter #(= "<unattributed>" (:cell %)) events))

(def summary
  {:events (count events)
   :retro-docs (count (distinct (map :id retro-events)))
   :live-events (count live-events)
   :conflicted-excluded (vec (sort conflicted-ids))
   :unattributed-events (count unattributed)
   :cells (count folded)
   :burned-in (count (filter #(>= (:samples (val %)) pp/default-min-history) folded))
   :gamma-range (when (seq folded)
                  [(apply min (map (comp :policy-precision val) folded))
                   (apply max (map (comp :policy-precision val) folded))])})

(def out
  {:provenance
   {:generated-by "b1_gamma_mission_fold.clj"
    :fold-source "src/futon2/aif/policy_precision.clj (loaded at run time, not copied)"
    :constants {:window pp/default-window-size :burn-in pp/default-min-history
                :gain pp/default-gain :floor pp/default-gamma-floor :cap pp/default-gamma-cap}
    :magnitudes perf-magnitude
    :admissibility "positive perf requires operator author (asymmetric; agent events may only lower)"
    :attribution {:retro "token-grep over sheet :context (PZ2 rule; ~1/3 known loss)"
                  :live "autoclock :clocked-mission (attribution of record)"}
    :inputs {:pz1-final-truth {:rows (count truth) :trues (count (filter :true? truth))}
             :b1-live-marks {:events (count (:events live))}}
    :as-of (last (sort (map :at events)))}
   :summary summary
   :cells (into (sorted-map) folded)
   :events (vec events)})

(spit (lab-file "b1-gamma-mission.edn")
      (with-out-str (clojure.pprint/pprint out)))

(println "== B1 γ(mission) fold ==")
(doseq [[m st] (sort-by (comp - :samples val) folded)]
  (println (format "  γ %.4f  samples %2d  %s%s"
                   (:policy-precision st) (:samples st) m
                   (if (< (:samples st) pp/default-min-history) "  (burn-in: prior)" ""))))
(println (pr-str summary))
(println (str "wrote " (lab-file "b1-gamma-mission.edn")))

;; ---------------------------------------------------------------------------
;; --check: independent recomputation (plain arithmetic, not the fold) + asserts
;; ---------------------------------------------------------------------------
(when (some #{"--check"} *command-line-args*)
  (let [by-cell (group-by :cell (remove #(= "<unattributed>" (:cell %)) events))
        expect (fn [perfs]
                 (let [n (count perfs)
                       window (take-last pp/default-window-size perfs)]
                   (if (< n pp/default-min-history)
                     1.0
                     (-> (Math/pow 2.0 (/ (reduce + 0.0 window) (count window)))
                         (max pp/default-gamma-floor) (min pp/default-gamma-cap)))))
        failures (atom [])]
    ;; C1: every cell's γ matches an independent recomputation
    (doseq [[cell st] folded
            :let [e (expect (map :perf (by-cell cell)))]]
      (when (> (Math/abs (- e (:policy-precision st))) 1e-12)
        (swap! failures conj [:c1-gamma-mismatch cell e (:policy-precision st)])))
    ;; C2: every under-burn-in cell sits EXACTLY at the prior 1.0
    (doseq [[cell st] folded
            :when (< (:samples st) pp/default-min-history)]
      (when (not= 1.0 (:policy-precision st))
        (swap! failures conj [:c2-burn-in-not-prior cell (:policy-precision st)])))
    ;; C3: the gold-conflicted doc minted no event
    (doseq [id conflicted-ids]
      (when (some #(= id (:id %)) events)
        (swap! failures conj [:c3-conflicted-leaked id])))
    ;; C4: all-corrections cells at/after burn-in sit exactly at 2^(-1/2)
    (let [g (Math/pow 2.0 -0.5)]
      (doseq [[cell st] folded
              :when (and (>= (:samples st) pp/default-min-history)
                         (every? #(= :correction (:mark %)) (by-cell cell)))]
        (when (> (Math/abs (- g (:policy-precision st))) 1e-12)
          (swap! failures conj [:c4-not-half-log cell (:policy-precision st)]))))
    ;; C5: PZ2 census cross-check (38-truth basis, conflicted row INCLUDED, to
    ;; match pz2_grain_census.clj) — reported, and the headline counts asserted.
    (let [pz2-cells (frequencies
                     (mapcat (fn [r] (or (seq (missions-of (:context (ctx-of (:id r)))))
                                         ["<no-mission-token>"]))
                             (filter :true? truth)))
          attributed (remove #(= "<no-mission-token>" (key %)) pz2-cells)
          got [(count attributed)
               (count (filter #(>= (val %) 3) attributed))
               (count (filter #(>= (val %) 5) attributed))
               (get pz2-cells "<no-mission-token>" 0)]]
      (println (str "C5 census (PZ2 basis) [cells >=3 >=5 unattributed]: " (pr-str got)
                    " — PZ2 published: [17 4 1 12]"))
      (when (not= got [17 4 1 12])
        (swap! failures conj [:c5-census-drift got])))
    (if (seq @failures)
      (do (println "CHECK FAIL:") (doseq [f @failures] (prn f)) (System/exit 1))
      (println (format "CHECK OK — C1 γ-recomputation %d cells · C2 burn-in · C3 conflicted-excluded · C4 half-log cells · C5 census"
                       (count folded))))))
