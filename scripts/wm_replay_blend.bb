#!/usr/bin/env bb
;; wm_replay_blend.bb — M-evaluate-policies D6: replay harness (implements C9,
;; fills the Q8 gap). The before/after comparator for every D-stage and the
;; vehicle for D3's renormalisation experiment (E4).
;;
;; Reads persisted wm-trace ticks (one EDN form per tick; same reader + cascade
;; lane guard as wm_trace_census.bb) and RECOMPUTES each candidate's total under
;; a CANDIDATE FORMULA SPEC, then reports how the argmin winner moves vs actual.
;;
;; Persisted-term formula (futon2.aif.efe/compute-efe; see wm_trace_census.bb):
;;   controller-score = G-risk + G-ambiguity - 0.4*predictability-bonus + 1.2*homeostatic-pressure
;;             - 0.35*structural-pressure + G-goal-outcome
;;             + (graph-control-score - gap-exploration-bonus)
;; The last parenthesis is STRIPPED at persist time (trace.clj strip-ranked-action)
;; and is recoverable only as the RESIDUAL of reconstructing :controller-score from the
;; persisted terms at default weights. The replay keeps that residual FIXED per
;; candidate (it is a forward-model product we cannot re-derive) and varies only
;; the weights/inclusion of the persisted terms. Under the default spec the
;; recomputed total == persisted :controller-score exactly (invariant I1, replay-equality),
;; so the default spec flips nothing — the harness's built-in sanity check.
;;
;; LIMITATION (stated, per §8.7): replay can vary weights/terms over *persisted*
;; values only; it CANNOT re-run forward-model modes (e.g. D5a/D5c risk-mode or
;; c-distribution changes) — those need sim-first spikes in VERIFY.
;;
;; Spec EDN shape (weights override defaults; :exclude drops terms entirely):
;;   {:name "survival-off"
;;    :weights {:info 0.4 :survival 1.2 :structural-pressure 0.35}
;;    :exclude #{:survival}}
;; Term names: :risk :ambiguity :info :survival :structural-pressure :goal-outcome
;; plus the pseudo-term :hidden in :exclude to drop the residual as well.
;;
;; Usage: bb scripts/wm_replay_blend.bb <spec.edn> [trace-dir]
;; Writes: holes/labs/M-evaluate-policies/replay-<name>.edn

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def default-weights
  {:risk 1.0 :ambiguity 1.0 :info 0.4 :survival 1.2
   :structural-pressure 0.35 :goal-outcome 1.0})

;; Sign each term enters controller-score with (info and structural-pressure are subtracted).
(def term-sign
  {:risk 1.0 :ambiguity 1.0 :info -1.0 :survival 1.0
   :structural-pressure -1.0 :goal-outcome 1.0})

(def term->g
  {:risk :G-risk :ambiguity :G-ambiguity :info :predictability-bonus :survival :homeostatic-pressure
   :structural-pressure :structural-pressure :goal-outcome :G-goal-outcome})

(def terms (vec (keys term->g)))

(defn read-all-forms [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [acc []]
      (let [form (edn/read {:eof ::eof :default (fn [_tag v] v)} r)]
        (if (= ::eof form) acc (recur (conj acc form)))))))

(defn term-val ^double [e t]
  (double (or ((term->g t) e) 0.0)))

(defn recon-default ^double [e]
  (reduce + (for [t terms]
              (* (term-sign t) (double (default-weights t)) (term-val e t)))))

(defn residual ^double [e]
  (- (double (or (:controller-score e) 0.0)) (recon-default e)))

(defn spec-total
  "Replay controller-score for candidate e under `spec`. The fixed hidden-term residual is
   kept unless :hidden is in :exclude."
  ^double [spec e]
  (let [w  (merge default-weights (:weights spec))
        ex (set (:exclude spec))
        visible (reduce + (for [t terms :when (not (ex t))]
                            (* (term-sign t) (double (w t)) (term-val e t))))
        res (if (ex :hidden) 0.0 (residual e))]
    (+ visible res)))

(defn winner-idx
  "Index of the minimum score (matches wm_trace_census.bb: apply min-key)."
  [scores]
  (first (apply min-key second (map-indexed vector scores))))

(defn lane-valid
  "Ranked actions with a real decomposition — drops :apply-cascade placeholder
   rows (persisted as {:controller-score 0.0} with nil :G-risk), which have no scored
   terms and must never win a replay."
  [tick]
  (vec (filter #(some? (:G-risk %)) (:ranked-actions tick))))

(defn tick-replay [spec tick]
  (let [ra (lane-valid tick)]
    (when (seq ra)
      (let [actual (winner-idx (mapv #(double (or (:controller-score %) 0.0)) ra))
            specw  (winner-idx (mapv #(spec-total spec %) ra))]
        {:actual-type (get-in ra [actual :action :type])
         :spec-type   (get-in ra [specw :action :type])
         :flip?       (not= actual specw)}))))

(defn load-corpus [dir]
  (->> (.listFiles (io/file dir))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
       (sort-by #(.getName %))
       (mapv (fn [f] {:file (.getName f) :forms (read-all-forms f)}))))

(defn pct [num den]
  (when (pos? den) (/ (Math/round (* 1000.0 (/ (double num) den))) 10.0)))

(defn replay
  "Run `spec` over an already-loaded `corpus`; return the report map."
  [corpus spec]
  (let [per-day (for [{:keys [file forms]} corpus]
                  (let [rs (keep #(tick-replay spec %) forms)]
                    {:file file :ticks (count rs)
                     :flips (count (filter :flip? rs)) :results rs}))
        all     (mapcat :results per-day)
        n       (count all)
        flips   (filter :flip? all)
        af      (frequencies (map :actual-type all))
        sf      (frequencies (map :spec-type all))]
    {:generated-by "scripts/wm_replay_blend.bb (M-evaluate-policies D6)"
     :spec spec
     :limitation
     (str "Replay varies weights/terms over PERSISTED values only; the hidden "
          "(graph-pragmatic - gap) residual is held fixed per candidate and "
          "forward-model modes cannot be re-run (need sim-first spikes in VERIFY).")
     :corpus {:files (count corpus) :ticks n}
     :flip-rate-pct (pct (count flips) n)
     :n-flips (count flips)
     :type-flow
     (into (sorted-map)
           (frequencies (map (juxt :actual-type :spec-type) flips)))
     :winner-types
     {:actual (into (sorted-map) af)
      :spec   (into (sorted-map) sf)
      :net-delta (into (sorted-map)                       ; spec-count - actual-count
                       (for [k (into (set (keys af)) (keys sf))]
                         [k (- (get sf k 0) (get af k 0))]))}
     :per-day (mapv #(dissoc % :results) per-day)}))

;; ---------------------------------------------------------------------------
(defn run-cli [spec-file trace-dir]
  (let [spec   (edn/read-string (slurp spec-file))
        corpus (load-corpus trace-dir)
        _      (println (format "Loaded %d trace files from %s" (count corpus) trace-dir))
        rep    (replay corpus spec)
        nm     (or (:name spec)
                   (-> (io/file spec-file) .getName (str/replace #"\.edn$" "")))
        out    (str "holes/labs/M-evaluate-policies/replay-" nm ".edn")]
    (io/make-parents out)
    (spit out (with-out-str (pp/pprint rep)))
    (println (format "\n=== REPLAY %s ===" nm))
    (pp/pprint (dissoc rep :per-day :spec :generated-by))
    (println "\nWrote" out)))

;; Run the CLI only when invoked directly with a spec argument; stays inert when
;; this file is (load-file)'d by the E4 sweep, which passes no command-line args.
(when (seq *command-line-args*)
  (run-cli (first *command-line-args*)
           (or (second *command-line-args*) "data/wm-trace")))
