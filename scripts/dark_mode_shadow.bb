#!/usr/bin/env bb
;; dark_mode_shadow.bb — M-aif-faithfulness dark-mode-shadow-trio (owner claude-12).
;;
;; READ-ONLY selection-side flip evidence for THREE dark modes, replayed over the
;; trace corpus's persisted :ranked-actions (all G summands recorded post-2d6533e).
;; No writes to data/wm-trace. Sibling of a_matrix_shadow.bb (the belief-side one).
;; Cascade placeholder rows (no :G-risk) excluded (E6 convention).
;;
;;   :tau-mode :gamma-only (B-2d, policy/effective-temperature)
;;     τ_eff :spread = τ_spread/γ (recorded :tau) vs :gamma-only = 1/γ.
;;     Selection winner = argmin controller-score ⇒ τ-INVARIANT; abstain = gap-exploration-bonus
;;     (no-op.G − best.G < ε) ⇒ ALSO τ-invariant. So the ONLY effect is softmax
;;     ENTROPY (commitment sharpness). Reported: winner-flip (0 by construction),
;;     abstain-delta (0 by construction), per-tick softmax-entropy delta.
;;
;;   :salience-mode :separate (B-2c, precision/precision-state)
;;     :precision-state records per-channel {:precision(=summed) :variance-component
;;     :need-component}. :separate ⇒ Π = variance-component only (drop need). The
;;     Π-vector shadow is DIRECT (no error-history replay). Whether the Π change
;;     flips a SELECTION needs re-running free-energy/G per candidate with the new
;;     Π — heavier re-simulation, NOT done (said, not faked). Reported: per-tick
;;     Π-vector relative change + need-share dropped, per channel + aggregate.
;;
;;   :structural-pressure-mode :habit-prior (D-1d, efe.clj)
;;     sp term LEAVES :controller-score (G − spc) and re-enters as a +spc log-prior bias
;;     (spc = :structural-pressure, already w·sp in G-units). Selection becomes
;;     argmin(G − spc·(1+τ)). Reported: winner-flip rate + direction.
;;
;; Deterministic. Run: bb scripts/dark_mode_shadow.bb  [trace-dir]
(require '[clojure.edn :as edn] '[clojure.java.io :as io] '[clojure.pprint :as pp])

(def trace-dir (or (first *command-line-args*) "data/wm-trace"))
(def out-dir "holes/labs/M-aif-faithfulness")
(def tau-min 0.01)
(def abstain-eps 0.01)

(defn read-all-forms [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [acc []]
      (let [form (edn/read {:eof ::eof :default (fn [_tag v] v)} r)]
        (if (= ::eof form) acc (recur (conj acc form)))))))

(defn softmax [gs tau]
  (let [t (max tau-min (double tau))
        mn (apply min gs)
        ws (map #(Math/exp (- (/ (- (double %) mn) t))) gs)
        z (reduce + ws)]
    (map #(/ % z) ws)))

(defn entropy [ps] (- (reduce + (map #(if (> % 1e-12) (* % (Math/log %)) 0.0) ps))))
(defn argmin-idx [xs] (first (apply min-key (fn [[_ v]] v) (map-indexed vector xs))))

(def files (->> (.listFiles (io/file trace-dir))
                (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
                (sort-by #(.getName %))))
(println (format "Reading %d trace files from %s …" (count files) trace-dir))

(def ticks
  (for [f files t (read-all-forms f)
        :let [ras (vec (filter :G-risk (:ranked-actions t)))]
        :when (seq ras)]
    {:day (subs (str (:timestamp t)) 0 10)
     :ras ras
     :gamma (get-in t [:decision :selection-gain])
     :tau-spread (get-in t [:decision :tau])
     :precision-state (:precision-state t)}))

;; --- TAU ---------------------------------------------------------------------
(def tau-recs
  (for [{:keys [day ras gamma tau-spread]} ticks
        :let [gs (mapv #(double (:controller-score %)) ras)
              g (max tau-min (double (or gamma 1.0)))
              ts (double (or tau-spread (/ 1.0 g)))
              tg (/ 1.0 g)
              noop (some #(when (= :no-op (get-in % [:action :type])) (double (:controller-score %))) ras)
              best (apply min gs)
              gap (when noop (- noop best))
              abstain? (fn [_] (and gap (< gap abstain-eps)))]]
    {:day day
     :winner-flip? false                     ; argmin G — τ-invariant
     :abstain-spread (boolean (abstain? ts)) :abstain-gamma (boolean (abstain? tg))
     :ent-spread (entropy (softmax gs ts)) :ent-gamma (entropy (softmax gs tg))}))

;; --- SALIENCE ----------------------------------------------------------------
(def sal-recs
  (for [{:keys [day precision-state]} ticks :when (map? precision-state)]
    (let [chans (for [[ch {:keys [precision variance-component need-component]}] precision-state
                      :when (and (number? precision) (number? variance-component))]
                  (let [summed (double precision) sep (double variance-component)
                        need (double (or need-component 0.0))]
                    {:ch ch :rel-change (if (> (Math/abs summed) 1e-12) (/ (Math/abs (- sep summed)) (Math/abs summed)) 0.0)
                     :need-share (if (> (Math/abs summed) 1e-12) (/ need summed) 0.0)}))]
      {:day day
       :n-channels (count chans)
       :n-materially-changed (count (filter #(> (:rel-change %) 0.01) chans))
       :mean-rel-change (if (seq chans) (/ (reduce + (map :rel-change chans)) (count chans)) 0.0)
       :mean-need-share (if (seq chans) (/ (reduce + (map :need-share chans)) (count chans)) 0.0)})))

;; --- STRUCTURAL PRESSURE -----------------------------------------------------
(def sp-recs
  (for [{:keys [day ras gamma tau-spread]} ticks
        :let [gs (mapv #(double (:controller-score %)) ras)
              spc (mapv #(double (or (:structural-pressure %) 0.0)) ras)
              tau (double (or tau-spread (/ 1.0 (max tau-min (double (or gamma 1.0))))))
              live-win (argmin-idx gs)
              habit-score (mapv (fn [g s] (- g (* s (+ 1.0 tau)))) gs spc)
              habit-win (argmin-idx habit-score)
              any-sp? (some pos? spc)]]
    {:day day :any-sp? (boolean any-sp?)
     :winner-flip? (and any-sp? (not= live-win habit-win))
     :flip-toward-sp? (and (not= live-win habit-win) (pos? (nth spc habit-win 0.0)))}))

;; --- aggregate + write -------------------------------------------------------
(defn frac [k xs] (let [n (count xs)] (when (pos? n) (/ (Math/round (* 10000.0 (/ (count (filter k xs)) (double n)))) 10000.0))))
(defn mean [f xs] (let [n (count xs)] (when (pos? n) (/ (reduce + (map f xs)) (double n)))))

(def tau-summary
  {:ticks (count tau-recs)
   :winner-flips 0
   :winner-flip-note "0 by construction — softmax winner = argmin controller-score, τ-invariant."
   :abstain-flips (count (filter #(not= (:abstain-spread %) (:abstain-gamma %)) tau-recs))
   :abstain-note "abstain = (no-op.G − best.G) < ε, a gap-exploration-bonus ⇒ τ-invariant."
   :mean-entropy-spread (mean :ent-spread tau-recs)
   :mean-entropy-gamma-only (mean :ent-gamma tau-recs)
   :mean-entropy-delta (mean #(- (:ent-gamma %) (:ent-spread %)) tau-recs)
   :recommendation-seed "τ-mode :gamma-only changes NEITHER winner NOR abstain — only softmax entropy (commitment sharpness). Flip is behaviourally inert on argmax/abstain; decide jointly with B-3b (γ β-update) as the docstring notes."})

(def sal-summary
  {:ticks (count sal-recs)
   :mean-need-share (mean :mean-need-share sal-recs)
   :mean-rel-Pi-change (mean :mean-rel-change sal-recs)
   :mean-channels-materially-changed (mean :n-materially-changed sal-recs)
   :selection-downstream "NOT computed — needs free-energy/G re-run per candidate under the :separate Π; said, not faked."
   :recommendation-seed "The need-component's share of Π (mean-need-share) is the magnitude of what :separate drops; if ~0, the salience flip is inert; if large, it materially reweights channels and a G-recompute is warranted before flipping."})

(def sp-summary
  {:ticks (count sp-recs)
   :ticks-with-any-sp (count (filter :any-sp? sp-recs))
   :winner-flips (count (filter :winner-flip? sp-recs))
   :winner-flip-rate (frac :winner-flip? sp-recs)
   :flips-toward-sp-action (count (filter :flip-toward-sp? sp-recs))
   :recommendation-seed "REVIEWER-CORRECTED (claude-12): structural-pressure ALREADY enters controller-score as a preference (−w·sp; efe.clj docstring: higher sp reduces G) — :habit-prior does NOT invert a penalty. What changes: the preference moves OUTSIDE the τ division (argmin(G′ − τ_eff·w·sp) vs argmin(G′ − w·sp)), so its effective strength becomes τ_eff-scaled and precision no longer modulates it. winner-flip-rate = decisions the RESCALING changes; all observed flips moved toward the high-sp action (τ_eff > 1 amplifies the prior there)."})

(def combined
  {:generated-by "scripts/dark_mode_shadow.bb (M-aif-faithfulness dark-mode-shadow-trio)"
   :sim-only true :read-only true :corpus {:files (count files) :ticks (count ticks)}
   :per-mode {:tau tau-summary :salience sal-summary :structural-pressure sp-summary}})

(doseq [[k summ] [[:tau (assoc combined :mode :tau-mode/:gamma-only :summary tau-summary)]
                  [:salience (assoc combined :mode :salience-mode/:separate :summary sal-summary)]
                  [:structural-pressure (assoc combined :mode :structural-pressure-mode/:habit-prior :summary sp-summary)]]]
  (let [f (str out-dir "/dark-mode-shadow-" (name k) ".edn")]
    (io/make-parents f)
    (spit f (with-out-str (pp/pprint summ)))))
(spit (str out-dir "/dark-mode-shadow-combined.edn") (with-out-str (pp/pprint combined)))

(println "\n=== DARK-MODE SHADOW TRIO ===")
(println (format "corpus: %d files, %d ticks" (count files) (count ticks)))
(println (format "TAU  :gamma-only → winner-flips 0 (τ-invariant) · abstain-flips %d · mean softmax-entropy Δ %.4f"
                 (:abstain-flips tau-summary) (double (or (:mean-entropy-delta tau-summary) 0))))
(println (format "SALIENCE :separate → mean need-share of Π %.4f · mean rel-Π-change %.4f · downstream G-recompute NOT done"
                 (double (or (:mean-need-share sal-summary) 0)) (double (or (:mean-rel-Pi-change sal-summary) 0))))
(println (format "STRUCT-PRESSURE :habit-prior → winner-flips %d/%d (%.2f%%) · toward-sp %d"
                 (:winner-flips sp-summary) (:ticks sp-summary)
                 (* 100.0 (double (or (:winner-flip-rate sp-summary) 0))) (:flips-toward-sp-action sp-summary)))
(println "wrote" out-dir "/dark-mode-shadow-{tau,salience,structural-pressure,combined}.edn")
