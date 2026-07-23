(ns xeno-gentle-sweep
  "THE GENTLE-PROPAGATOR SWEEP — the falsifying test named in
  holes/F-propagator-on-c-vector-NEGATIVE.md, widened from cadence-only to
  (cadence x magnitude). (M-propagators L5 / M-aif-ants-port, 2026-07-16.)

  THE HYPOTHESIS UNDER TEST. The negative's reading is that propagating C is
  amnesia. But the amnesia is carried by two DISANALOGIES the port introduced,
  and both are knobs:

    MAGNITUDE  the MetaCA substrate is binary, so a flip is the SMALLEST move
               available there; on a continuous C-mean, `:= 1 - C[k]` is the
               LARGEST. The faithful continuous analogue of a discrete coupling
               is a rate-limited relaxation:
                   C[sigma(k)].mean <- (1-eps)*C[sigma(k)].mean + eps*(1 - C[k].mean)
               eps = 1 reproduces the old operator EXACTLY (same RNG stream,
               same draws, same writes); small eps couples preferences while
               goals persist on a ~1/eps-tick timescale. Coupling without
               amnesia.
    FREQUENCY  EVERY = ticks between applications (the doc's cadence sweep).

  (The third disanalogy — locus: per-cell-across-SPACE in the MetaCA vs one
  global C across TIME here — needs per-ant C threaded through policy.clj:619
  and is out of scope for this script; see the writeup.)

  *** PREREGISTERED READINGS ***
  Sigma is FIXED per arm (no evolution). Score 16 held-out boards — the SAME
  held-out set as the negative run (read from /tmp/xeno-loop.edn) — paired
  sign test vs baseline per cell.
    1. If some (non-identity sigma, cell) BEATS baseline (p<.05, +delta), the
       transfer was UNTUNED, not refuted: amnesia was the artifact. Any winner
       is a CANDIDATE, to be confirmed on a fresh screened pool and then
       re-evolved at that cell before it counts (36 comparisons at p<.05
       expect ~2 false positives).
    2. If gentle cells only approach baseline from below as eps -> 0, with no
       bump above it anywhere, the refutation is STRENGTHENED: coupling
       preferences does not help at ANY rate, not just at the amnesic rate.
    3. Sanity/audit cells, all of which must hit EXACTLY (deterministic seeds):
       - evolved & identity sigma at (EVERY 1, eps 1.0) must reproduce the
         negative run's per-seed y-evo / y-ident from the edn;
       - baseline rerun must reproduce y-base;
       - identity sigma at eps 0.0 must TIE baseline per-seed (proves run-live's
         hand-stepped loop is equivalent to the stock harness — the sham).

  NB identity-sigma at eps<1 is not 'noise' as at eps=1: C[k] <- C[k]+eps(1-2C[k])
  relaxes every touched channel toward 0.5 — a preference-DILUTION control.

  Run: cd futon2 && clojure -M scripts/xeno_gentle_sweep.clj [ticks]"
  (:require [ants.aif.policy :as policy]
            [ants.aif.experiment :as experiment]
            [ants.war]
            [clojure.edn :as edn]))

(def run-single #'experiment/run-single)
(def base-c @#'policy/default-c-vectors)

(def SPECIES :aif)
(def FOOD :patchy)
(def SIZE [30 30])

;; ---------------- propagator pieces (as xeno_loop.clj, magnitude added) ------
(defn- pref-channels [c mode]
  (vec (sort (keep (fn [[k v]] (when (number? (:mean v)) k)) (get c mode)))))

(defn- apply-propagator
  "One application of: pick channel k per mode; relax C[sigma(k)].mean toward
   (1 - C[k].mean) at rate eps. eps=1.0 is byte-identical to xeno_loop's
   operator (same per-mode draw structure and RNG consumption), so the
   (EVERY 1, eps 1.0) cells replicate the negative run exactly."
  [c sigma eps rng]
  (reduce
   (fn [a mode]
     (let [here (set (pref-channels a mode))
           valid (vec (sort (filter #(and (here %) (here (get sigma % %))) here)))]
       (if (empty? valid)
         a
         (let [k (nth valid (.nextInt ^java.util.Random rng (count valid)))
               dst (get sigma k k)
               old (double (get-in a [mode dst :mean]))
               tgt (- 1.0 (double (get-in a [mode k :mean])))]
           (assoc-in a [mode dst :mean]
                     (+ (* (- 1.0 eps) old) (* eps tgt)))))))
   c (keys c)))

(defn- run-live
  "One run, propagator firing every EVERY ticks at magnitude eps, live C.
   RNG seeding identical to xeno_loop/run-live so eps=1.0/EVERY=1 replicates."
  [sigma seed ticks every eps]
  (let [w0 (experiment/make-seeded-world SPECIES FOOD seed (+ 10000 seed) SIZE ticks)
        rng (java.util.Random. (+ 999 seed))]
    (loop [w w0, n 0, c base-c]
      (if (>= n ticks)
        (double (get-in w [:scores SPECIES] 0.0))
        (let [c' (if (and (pos? eps) (zero? (mod n every)))
                   (apply-propagator c sigma eps rng)
                   c)
              w' (with-redefs [policy/default-c-vectors c']
                   (ants.war/step w))]
          (recur w' (inc n) c'))))))

;; ---------------- stats (as xeno_loop.clj) -----------------------------------
(defn- mean [xs] (if (seq xs) (/ (reduce + xs) (double (count xs))) 0.0))
(defn- choose-nk [n k] (reduce * 1.0 (map (fn [i] (/ (double (- n i)) (inc i))) (range k))))
(defn- sign-p [n k]
  (if (zero? n) 1.0
      (min 1.0 (* 2.0 (reduce + (map #(* (choose-nk n %) (Math/pow 0.5 n)) (range k (inc n))))))))
(defn- verdict [a b]
  (let [d (mapv - a b)
        nz (filterv #(not (zero? %)) d)
        k (max (count (filter pos? nz)) (count (filter neg? nz)) 0)
        p (sign-p (count nz) k)]
    {:mean-delta (mean d) :n (count nz) :wins (count (filter pos? nz)) :p p
     :beats (and (< p 0.05) (pos? (mean d)))}))

;; ---------------- sigma candidates -------------------------------------------
(defn- identity-sigma []
  (into {} (map (fn [k] [k k])
                [:food :pher :home-prox :enemy-prox :h :ingest
                 :trail-grad :dist-home :reserve-home :cargo])))

(defn- random-sigma
  "3 seeded transpositions on identity — an arbitrary non-trivial coupling,
   independent of the (noise-landscape-selected) evolved sigma."
  [seed]
  (let [rng (java.util.Random. (long seed))]
    (reduce (fn [s _]
              (let [ks (vec (sort (keys s)))
                    a (nth ks (.nextInt rng (count ks)))
                    b (nth ks (.nextInt rng (count ks)))]
                (assoc s a (get s b) b (get s a))))
            (identity-sigma) (range 3))))

;; ---------------- main --------------------------------------------------------
(defn -main [& args]
  (let [ticks (Integer/parseInt (or (first args) "300"))
        prior (edn/read-string (slurp "/tmp/xeno-loop.edn"))
        test-seeds (vec (:test prior))
        _ (assert (= 16 (count test-seeds)) "expected the negative run's 16 held-out seeds")
        sigmas {:evolved (:evolved prior)
                :random (random-sigma 42)
                :identity (identity-sigma)}
        ;; (EVERY, eps) grid. (1, 1.0) = exact replication of the negative.
        cells [[1 0.05] [1 0.15] [1 0.4] [1 1.0]
               [10 0.4] [10 1.0]
               [50 1.0]]
        _ (println (format "=== GENTLE-PROPAGATOR SWEEP ===\n  held-out seeds (from negative run): %s\n  sigmas: %s\n  cells (EVERY, eps): %s\n  ticks %d\n"
                           test-seeds (keys sigmas) cells ticks))
        ;; baseline: stock harness, rerun as an apparatus-drift audit vs edn
        y-base (vec (pmap (fn [s] (double (:yield (run-single SPECIES FOOD s (+ 10000 s) SIZE ticks false))))
                          test-seeds))
        _ (println (format "  baseline rerun mean %.3f   (edn recorded %.3f; per-seed match: %s)"
                           (mean y-base) (mean (:y-base prior))
                           (= (mapv double y-base) (mapv double (:y-base prior)))))
        ;; sham: identity sigma, eps 0 — must tie baseline per-seed.
        ;; NB all run-live calls are SEQUENTIAL (mapv, not pmap): run-live moves C
        ;; via with-redefs on a GLOBAL var, so concurrent runs stomp each other's C.
        ;; (Caught live: a pmap version failed the replication audit — evolved @
        ;; (EVERY 1, eps 1.0) gave 12.99 vs the negative run's 11.29.)
        y-sham (mapv (fn [s] (run-live (identity-sigma) s ticks 1 0.0)) test-seeds)
        _ (println (format "  SHAM (identity, eps 0) mean %.3f   per-seed ties baseline: %s\n"
                           (mean y-sham) (= (mapv double y-sham) (mapv double y-base))))
        results
        (vec (for [[sig-name sigma] sigmas
                   [every eps] cells]
               (let [ys (mapv (fn [s] (run-live sigma s ticks every eps)) test-seeds)
                     v (verdict ys y-base)]
                 (println (format "  %-9s EVERY %3d  eps %.2f   yield %8.3f   Δ %+8.3f  wins %2d/%2d  p %.4f%s"
                                  (name sig-name) every eps (mean ys)
                                  (:mean-delta v) (:wins v) (:n v) (:p v)
                                  (if (:beats v) "   *** BEATS BASELINE ***" "")))
                 (flush)
                 {:sigma sig-name :every every :eps eps :ys ys :verdict v})))]
    ;; replication audit against the negative run's edn
    (let [cell-of (fn [nm] (first (filter #(and (= (:sigma %) nm) (= (:every %) 1) (= (:eps %) 1.0)) results)))
          rep-evo (= (mapv double (:ys (cell-of :evolved))) (mapv double (:y-evo prior)))
          rep-id  (= (mapv double (:ys (cell-of :identity))) (mapv double (:y-ident prior)))]
      (println (format "\n  replication audit @ (EVERY 1, eps 1.0): evolved matches negative-run y-evo: %s; identity matches y-ident: %s" rep-evo rep-id)))
    (println "\n=== PREREGISTERED READING ===")
    (let [winners (filter #(and (get-in % [:verdict :beats]) (not= (:sigma %) :identity)) results)]
      (if (seq winners)
        (do (println "  UNTUNED, NOT REFUTED — cells beating baseline (CANDIDATES: confirm on a fresh pool, then re-evolve at that cell):")
            (doseq [w winners]
              (println (format "    %s EVERY %d eps %.2f  Δ %+.3f p %.4f"
                               (name (:sigma w)) (:every w) (:eps w)
                               (get-in w [:verdict :mean-delta]) (get-in w [:verdict :p])))))
        (println "  NO CELL BEATS BASELINE — the refutation is STRENGTHENED:\n   coupling C is not helpful at any tested rate, not merely at the amnesic rate.\n   Remaining outs: per-ant/colony locus (per-ant C), precision-not-preference.")))
    (spit "/tmp/xeno-gentle-sweep.edn"
          (pr-str {:ticks ticks :test test-seeds :y-base y-base :y-sham y-sham
                   :sigmas sigmas :results results}))
    (println "\n  wrote /tmp/xeno-gentle-sweep.edn")
    (flush)
    (shutdown-agents)))

(apply -main *command-line-args*)
