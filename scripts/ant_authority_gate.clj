(ns ant-authority-gate
  "ANT ACTUATOR AUTHORITY GATE — does the C-vector actually drive the forager?
  (M-propagators L5 / M-aif-ants-port, 2026-07-16)

  WHY THIS EXISTS, AND WHY IT RUNS BEFORE ANY TRANSFER CLAIM.

  The previous cyberant 'domain transfer' measured nothing. `cyber.clj`'s
  `config->aif-delta` merges ONLY `:precision` into the live ant; `:policy-priors`,
  `:pattern-sense`, `:adapt-config` are stored under `:cyber-pattern → :config` and
  NEVER READ. The `random-wiring` control only permuted those inert fields, so it was
  operationally byte-identical to the treatment — the 'null' was a TAUTOLOGY, and it was
  read as refuting the hypothesis rather than the apparatus. (M-aif-ants-port, verified.)

  So: before a propagator is allowed anywhere near an ant, PROVE THE KNOB MOVES THE ANT.
  This gate targets the modern AIF forager (futon2/src/ants/aif/*, the NEW ants — NOT
  cyber.clj, which is the post-mortem subject).

  THE TARGET. The transfer analogy is structural and tight:
      MetaCA : 8 neighbourhoods -> binary response     =  the RULE BYTE
      AIF ant: 14 obs channels  -> preference (per mode) = the C-VECTOR
  A propagator couples responses and has a fixed point that selects the rule; the ant
  analogue would couple preferences and select the C. policy.clj:619 reads
  `default-c-vectors` directly into the EFE, and its docstring claims 'Nothing else
  routes preferences into selection.' That is a DOCSTRING. This measures it.

  THE DESIGN — a gate that can actually FAIL.
    arms:
      :baseline   C as shipped
      :identity   SHAM. C rebuilt key-by-key, semantically identical. MUST show ~0
                  effect. If it moves yield, the harness manufactures differences and
                  every other number here is void. This is the control that makes the
                  gate honest, and it is the one cyber.clj never had.
      :flatten    all preferences removed (C = {}). If C has authority this should be
                  the LARGEST effect: no preferences => KL-risk vanishes => selection
                  loses its pragmatic term entirely.
      :shuffle    preference means permuted ACROSS channels (prefer the wrong things).
                  The honest analogue of the old `random-wiring` control — but on a
                  channel that is actually read.
      :invert     mean -> 1-mean (prefer the opposite).
    PAIRED SEEDS: every arm runs the SAME food/move seeds, so differences are not seed
    luck. (Scoring arms on different seeds is an error this session already made once.)
    VERDICT: authority requires |mean paired Δyield| to exceed the seed-noise band, AND
    :identity to sit inside it.

  Run:  cd futon2 && clojure -M scripts/ant_authority_gate.clj [n-seeds] [ticks]"
  (:require [ants.aif.policy :as policy]
            [ants.aif.experiment :as experiment]
            [clojure.string :as str]))

(def run-single #'experiment/run-single)
(def base-c @#'policy/default-c-vectors)

;; ---------------- the perturbations ----------------
(defn- map-prefs
  "Apply f to every {:mean :sd} preference in a mode-conditioned C."
  [c f]
  (into {} (map (fn [[mode chans]]
                  [mode (into {} (map (fn [[k pref]] [k (f pref)]) chans))])
                c)))

(defn- perturb [kind c]
  (case kind
    :baseline c
    ;; SHAM: rebuild the identical structure. Any measured effect here is harness noise.
    :identity (map-prefs c (fn [p] (if (map? p) {:mean (:mean p) :sd (:sd p)} p)))
    ;; strongest kill: no preferences at all
    :flatten  (into {} (map (fn [[mode _]] [mode {}]) c))
    ;; prefer the opposite
    :invert   (map-prefs c (fn [p] (if (and (map? p) (number? (:mean p)))
                                     (assoc p :mean (- 1.0 (double (:mean p)))) p)))
    ;; prefer the WRONG channels: permute the means across channels within each mode
    :shuffle  (into {} (map (fn [[mode chans]]
                              (let [ks (vec (keys chans))
                                    ms (vec (map #(get-in chans [% :mean]) ks))
                                    rot (vec (concat (rest ms) [(first ms)]))]
                                [mode (into {} (map-indexed
                                                (fn [i k]
                                                  [k (if (number? (nth rot i))
                                                       (assoc (get chans k) :mean (nth rot i))
                                                       (get chans k))])
                                                ks))]))
                            c))))

;; ---------------- harness ----------------
(def SPECIES :aif)
(def FOOD :patchy)
(def SIZE [30 30])

(defn- run-arm [kind seeds ticks]
  (let [c (perturb kind base-c)]
    (with-redefs [policy/default-c-vectors c]
      (doall (for [s seeds]
               (let [r (run-single SPECIES FOOD s (+ 10000 s) SIZE ticks false)]
                 {:seed s :yield (:yield r) :starved (:starved r)}))))))

(defn- choose [n k]
  (reduce * 1.0 (map (fn [i] (/ (double (- n i)) (inc i))) (range k))))

(defn- sign-test-p
  "Exact two-sided binomial sign-test p: n informative seeds, k moving the majority way.
   P(at least k of n) * 2 under H0: direction is a coin flip. Ties are excluded BEFORE
   this is called -- a seed that was 0 and stayed 0 carries no directional information."
  [n k]
  (if (zero? n)
    1.0
    (min 1.0 (* 2.0 (reduce + (map #(* (choose n %) (Math/pow 0.5 n)) (range k (inc n))))))))

(defn- mean [xs] (if (seq xs) (/ (reduce + xs) (double (count xs))) 0.0))
(defn- sd [xs] (let [m (mean xs)] (Math/sqrt (mean (map #(let [d (- % m)] (* d d)) xs)))))

(defn -main [& args]
  (let [n (Integer/parseInt (or (first args) "12"))
        ticks (Integer/parseInt (or (second args) "300"))
        seeds (vec (range 1 (inc n)))
        _ (println (format "=== ANT ACTUATOR AUTHORITY GATE ===\n  target: C-vector (policy/default-c-vectors)\n  %d paired seeds, %d ticks, %s food, size %d, species %s\n"
                           n ticks (name FOOD) (first SIZE) (name SPECIES)))
        base (run-arm :baseline seeds ticks)
        base-y (mapv :yield base)
        _ (println (format "  baseline yield: mean %.3f  between-seed sd %.3f"
                           (mean base-y) (sd base-y)))
        ;; THE NOISE BAND IS THE SPREAD OF THE *PAIRED DIFFERENCES*, NOT THE BASELINE'S
        ;; BETWEEN-SEED SD. A paired design exists precisely to cancel between-seed
        ;; variance; scoring against the baseline sd throws that away and buries real
        ;; effects. (First draft did exactly that: it reported `invert` driving yield to
        ;; EXACTLY 0.000 on every seed as "within noise", because the baseline sd 17.7 was
        ;; larger than the baseline mean 14.2.) So: paired t-like statistic
        ;;     t = mean(Δ) / (sd(Δ)/sqrt(n))
        ;; plus a SIGN TEST -- the fraction of seeds moving the same way -- which is
        ;; robust to the wild per-seed yield spread and needs no distributional claim.
        _ (println (format "  (noise band = spread of PAIRED differences, not that sd)\n"))
        _ (println (format "  %-10s %9s %9s %8s %6s %8s %6s  %s"
                           "arm" "mean" "Δ vs base" "t(paired)" "sign" "sign-p" "ties" "reading"))
        results
        (doall
         (for [kind [:identity :flatten :shuffle :invert]]
           (let [rs (run-arm kind seeds ticks)
                 ys (mapv :yield rs)
                 deltas (mapv - ys base-y)          ; PAIRED: same seed, both arms
                 md (mean deltas)
                 sdd (sd deltas)
                 se (if (pos? sdd) (/ sdd (Math/sqrt (count deltas))) 0.0)
                 t (cond (zero? md) 0.0
                         (pos? se) (/ (Math/abs md) se)
                         :else Double/POSITIVE_INFINITY) ; every seed moved identically
                 ;; SIGN TEST, ties EXCLUDED. A seed whose baseline yield was already 0
                 ;; and stays 0 is UNINFORMATIVE, not evidence against the effect --
                 ;; dividing by all n counted those ties as failures and buried `invert`
                 ;; (which zeroes foraging on every seed) at 63%.
                 nz (filterv #(not (zero? %)) deltas)
                 ties (- (count deltas) (count nz))
                 same (if (seq nz)
                        (/ (double (max (count (filter pos? nz)) (count (filter neg? nz))))
                           (count nz))
                        0.0)
                 ;; A paired t is not enough on its own: when the treatment output is
                 ;; CONSTANT (invert -> 0 on every seed), Δ_i = -baseline_i, so sd(Δ)
                 ;; inherits the baseline's whole variance and t collapses even though the
                 ;; effect is total. So the sign test is the second, distribution-free
                 ;; route -- but it must be an ACTUAL TEST, not a threshold I liked.
                 ;; An earlier draft demanded sign >= 0.99 (unanimity) and so read 12-of-13
                 ;; informative seeds moving the same way -- exact binomial p = 0.0034 --
                 ;; as "within noise". Use the real two-sided binomial p instead.
                 p (sign-test-p (count nz) (max (count (filter pos? nz))
                                                (count (filter neg? nz))))
                 moves (or (> t 2.0) (< p 0.05))
                 reading (cond
                           (= kind :identity) (if (and (< (Math/abs md) 1e-9))
                                                "sham OK (exactly 0, as required)"
                                                "*** SHAM MOVED — HARNESS BROKEN ***")
                           moves "MOVES THE ANT"
                           :else "within noise — no authority")]
             (println (format "  %-10s %9.3f %9.3f %8s %5.0f%% %8.4f %6d  %s"
                              (name kind) (mean ys) md
                              (if (Double/isInfinite t) "inf" (format "%.2f" t))
                              (* 100 same) p ties reading))
             {:arm (name kind) :mean (mean ys) :delta md :t t :same-sign same :sign-p p :ties ties
              :moves moves :per-seed-delta deltas})))
        by (into {} (map (juxt :arm identity) results))
        sham-ok (< (Math/abs (:delta (by "identity"))) 1e-9)
        movers (filter :moves (vals (dissoc by "identity")))]
    (println)
    (println "  === VERDICT ===")
    (cond
      (not sham-ok)
      (println "  VOID — the sham (:identity) moved yield. The harness manufactures\n"
               "         differences; no other number here means anything. Fix first.")
      (seq movers)
      (println (format "  AUTHORITY PASS — %s move yield beyond the seed-noise band,\n                   and the sham does not. The C-vector is a live actuator:\n                   a propagator acting on C would be acting on something real."
                       (str/join ", " (map :arm movers))))
      :else
      (println "  AUTHORITY FAIL — no perturbation of C moves yield beyond seed noise.\n"
               "                   The docstring's 'nothing else routes preferences into\n"
               "                   selection' does not imply the route CARRIES anything.\n"
               "                   Do NOT build the propagator transfer on this knob."))
    (spit "/tmp/ant-authority-gate.edn" (pr-str {:seeds seeds :ticks ticks
                                                 :baseline-mean (mean base-y)
                                                 :baseline-sd (sd base-y) :arms results}))
    (println "\n  wrote /tmp/ant-authority-gate.edn")
    (flush)))

(apply -main *command-line-args*)
