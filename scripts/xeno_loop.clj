(ns xeno-loop
  "THE XENO LOOP — evolve propagators on the ant's C-vector under foraging selection.
  (M-propagators L5 / M-aif-ants-port, 2026-07-16)

  WHAT A XENOTYPE IS (Joe): 'a place where these exotypes are being evolved, e.g.
  selection pressure coming from ants that plays back into our design of new types of
  blending apparatus.' Until now that phrase had no referent -- the agent used it anyway,
  which was narrating past the evidence. Every piece now exists and is gated:

    GENOME     sigma, a permutation over the C-vector's preference channels. The
               propagator, moved off the rule byte and onto PREFERENCES.
    ACTUATOR   the C-vector. GATE-PASSED (ant_authority_gate.clj): invert -> yield 0.000
               on every seed, sign-p .0078; the sham moves nothing. Unlike cyber.clj's
               :cyber-pattern, this knob is real.
    SELECTION  ant foraging yield. The ants just like to eat -- a grounded objective, not
               an unanchored proxy like the tokamak's transport (M-propagators 4b).
    MUTATION   TRANSPOSITION. Not a guess: the Ollivier-Ricci curvature says 80% (83%
               excluding near-duplicates) of the most negatively-curved edges connect
               sigma differing by a single transposition -- i.e. minimal sigma-edits are
               exactly where the space BRANCHES. Curvature told us where to mutate.

  THE PROPAGATOR ON C. Same operator, new substrate:
      pick channel k at random;  C[sigma(k)].mean := 1 - C[k].mean
  i.e. copy the INVERSE of the preference for channel k into the preference for channel
  sigma(k). sigma = identity is ordinary preference noise (a random walk, no attractor);
  a non-trivial sigma COUPLES preferences and has a fixed point that selects the C the
  colony lands on. That is the conatus reading: the fixed point is not represented
  anywhere, it is what the operator's structure makes the ant tend toward.

  *** WHAT MAKES THIS FALSIFIABLE ***
  A loop that always 'improves' proves nothing -- selection on noise improves training
  fitness by construction. So:
    1. HELD-OUT SEEDS. Fitness is evolved on TRAIN seeds and scored on TEST seeds never
       selected against. Improvement on train is not a result; improvement on test is.
    2. THREE NULLS, all run identically:
         :evolved   selection ON  (the claim)
         :drift     selection OFF -- same mutations, random survivor. Isolates whether
                    SELECTION did anything, vs mere churn. THE key null.
         :identity  sigma = identity forever (ordinary preference noise, no coupling)
         :baseline  no propagator at all (the shipped C)
    3. PREREGISTERED VERDICT: the loop works iff :evolved beats BOTH :drift and
       :baseline on HELD-OUT seeds, by a sign test over seeds (p<.05). If :evolved beats
       :baseline but not :drift, the loop is churn, not evolution -- and that is the
       honest report.

  Run: cd futon2 && clojure -M scripts/xeno_loop.clj [gens] [pop] [train-seeds] [ticks]"
  (:require [ants.aif.policy :as policy]
            [ants.aif.experiment :as experiment]
            [ants.war]
            [clojure.set]))

(def run-single #'experiment/run-single)
(def base-c @#'policy/default-c-vectors)

(def SPECIES :aif)
(def FOOD :patchy)
(def SIZE [30 30])

;; ---------------- the board screen (the principled fix) ----------------
;;
;; RUN 1 WAS VACUOUS: held-out seeds 101-112 yielded ~0 for EVERY arm INCLUDING baseline,
;; so "no improvement" meant "no measurement". You cannot detect improvement on boards
;; where nothing forages. Root cause: yield is wildly zero-inflated (authority gate:
;; baseline mean 19.6, sd 67.5 -- most boards yield nothing, a couple carry everything).
;;
;; THE SCREEN MUST BE ARM-INDEPENDENT. Screening on "baseline yield > 0" is the obvious
;; move and it is BIASED: it keeps boards where the BASELINE happens to work, and would
;; hide exactly the effect we want if evolution's benefit is rescuing boards the baseline
;; fails on. So screen on a property of the BOARD, computed with no simulation and no arm:
;;
;;   nearest = distance from home to the closest food cell.
;;
;; MEASURED over seeds 1-24 (scripts diagnostic): productive boards mean nearest 3.7, dead
;; boards mean 11.3; EVERY board with no food within 10 of home yielded exactly 0 -- the
;; colony physically cannot reach food in the tick budget. total-food was REJECTED as a
;; screen: productive 78.9 vs dead 81.2, i.e. no signal at all.
;;
;; `nearest <= 8` therefore removes boards that are IMPOSSIBLE, not boards that are
;; baseline-unfriendly: of 17 screened boards, 10 are baseline-productive and 7 are boards
;; where food is reachable but the baseline still fails. Those 7 are kept on purpose --
;; they are the headroom, and dropping them is what a baseline-yield screen would do.
(def NEAREST-MAX 8.0)

(defn- board-nearest-food
  "Distance from home to the closest food cell. Pure board geometry: no arm, no sim."
  [seed]
  (let [w (experiment/make-seeded-world SPECIES FOOD seed (+ 10000 seed) SIZE 300)
        [hx hy] (get-in w [:homes SPECIES])
        ds (keep (fn [[[x y] c]]
                   (when (> (:food c) 0.05)
                     (Math/sqrt (+ (* (- x hx) (- x hx)) (* (- y hy) (- y hy))))))
                 (get-in w [:grid :cells]))]
    (if (seq ds) (apply min ds) 999.0)))

(defn- screen-seeds
  "Scan candidate seeds, keep those whose board is physically forageable."
  [candidates want]
  (let [kept (->> candidates
                  (map (fn [s] [s (board-nearest-food s)]))
                  (filter (fn [[_ d]] (<= d NEAREST-MAX)))
                  (map first))]
    (vec (take want kept))))

;; ---------------- the propagator, now on C ----------------
(defn- pref-channels
  "Channels carrying a numeric preference, per mode. These are what sigma permutes."
  [c mode]
  (vec (sort (keep (fn [[k v]] (when (number? (:mean v)) k)) (get c mode)))))

(defn- all-channels
  "Every preference channel appearing in ANY mode -- sigma's domain (the union, 10).

  WHY THE UNION AND NOT THE INTERSECTION. sigma was originally built from :outbound's
  channels and applied to all three modes; :homebound has no :food, so mappings touching
  :food silently NO-OPPED there -- ill-typed, and quiet about it. The obvious repair is the
  intersection (9 channels), and it is WRONG: it drops :food, the single channel most
  relevant to foraging, so the propagator could not touch the preference that matters most.
  A fix that breaks the experiment is not a fix.

  The per-mode difference is not a defect to erase -- it is the operator being SEMANTIC
  (a homebound ant carrying cargo genuinely has no food preference). So sigma ranges over
  the union, and each mode applies it only where BOTH k and sigma(k) exist there. Partial
  by design, explicit rather than silent."
  [c]
  (vec (sort (reduce (fn [acc mode] (clojure.set/union acc (set (pref-channels c mode))))
                     #{} (keys c)))))

(defn- apply-propagator
  "n applications of: pick channel k; C[sigma(k)].mean := 1 - C[k].mean.
   Per mode, k is drawn only from channels where BOTH k and sigma(k) exist in that mode,
   so every draw does something -- no silent no-ops."
  [c sigma n rng]
  (reduce
   (fn [acc _]
     (reduce
      (fn [a mode]
        (let [here (set (pref-channels a mode))
              valid (vec (sort (filter #(and (here %) (here (get sigma % %))) here)))]
          (if (empty? valid)
            a
            (let [k (nth valid (.nextInt ^java.util.Random rng (count valid)))
                  dst (get sigma k k)]
              (assoc-in a [mode dst :mean]
                        (- 1.0 (double (get-in a [mode k :mean]))))))))
      acc (keys acc)))
   c (range n)))

(defn- identity-sigma [c]
  (into {} (map (fn [k] [k k]) (all-channels c))))

(defn- transpose-sigma
  "MUTATION = a single TRANSPOSITION, because that is where the curvature is negative
   (M-propagators 2b.4: 80%/83% of most-negative OR edges are single transpositions)."
  [sigma ^java.util.Random rng]
  (let [ks (vec (sort (keys sigma)))
        i (.nextInt rng (count ks))
        j (.nextInt rng (count ks))
        a (nth ks i) b (nth ks j)]
    (assoc sigma a (get sigma b) b (get sigma a))))

;; ---------------- fitness: the propagator fires DURING the ant's life ----------------
;;
;; *** THE CATEGORY ERROR THIS FIXES (Joe spotted it from the symptom) ***
;;
;; Run 2 did:  c' = apply-propagator(base-c, sigma, 8 times);  freeze c';  run the ants.
;; That is a STATIC PREPROCESSING STEP wearing a dynamical operator's name. In the MetaCA
;; the propagator fires EVERY GENERATION, inside the dynamics -- that is what makes it a
;; propagator rather than a permutation. Frozen, the ants never experience it: they run an
;; arbitrarily scrambled constant.
;;
;; And our own JAX result proves freezing cannot work: NO sigma has a single-byte
;; attractor (0.0% at support<=1); the fixed point is an INVARIANT SET (4-64 bytes) the
;; system keeps cycling within. So "apply 8 times and freeze" grabs ONE ARBITRARY PHASE OF
;; AN ORBIT. Run 2's fitness docstring even said "the C that sigma's propagator settles
;; on" -- false; nothing settles, the propagator never stops moving.
;;
;; That is exactly why drift beat evolved: an arbitrary orbit phase makes the sigma-fitness
;; landscape NOISE, so selection maximised noise on 8 boards (overfitting) while drift,
;; refusing to commit, did not chase it. "Mutation beats selection" was the SIGNATURE of a
;; landscape with no signal -- which is what a category error produces.
;;
;; NOW: the propagator fires every tick against the LIVE C, so the colony's preferences
;; continuously cycle within the invariant set that sigma selects. THAT is the conatus
;; reading -- the fixed point is not represented anywhere; it is what the operator's
;; structure makes the ant tend toward. It is also the first version where the ant and the
;; MetaCA run the SAME OPERATOR IN THE SAME ROLE, which is the only version where "does it
;; transfer?" is a meaningful question.
(def EVERY 1)   ; ticks between propagator applications (1 = every tick, as in MetaCA)

(defn- run-live
  "One run with the propagator firing every EVERY ticks against the live C.
   Mirrors experiment/run-single's scoring; steps the world by hand so C can move."
  [sigma seed ticks]
  (let [w0 (experiment/make-seeded-world SPECIES FOOD seed (+ 10000 seed) SIZE ticks)
        rng (java.util.Random. (+ 999 seed))]
    (loop [w w0, n 0, c base-c]
      (if (>= n ticks)
        {:yield (double (get-in w [:scores SPECIES] 0.0))
         :final-c c}
        (let [c' (if (zero? (mod n EVERY)) (apply-propagator c sigma 1 rng) c)
              w' (with-redefs [policy/default-c-vectors c']
                   (ants.war/step w))]
          (recur w' (inc n) c'))))))

(defn- fitness
  "Mean ant yield with sigma's propagator LIVE in the loop. Deterministic in seeds."
  [sigma seeds ticks]
  (/ (reduce + (map (fn [s] (:yield (run-live sigma s ticks))) seeds))
     (double (count seeds))))

(defn- mean [xs] (if (seq xs) (/ (reduce + xs) (double (count xs))) 0.0))
(defn- choose [n k] (reduce * 1.0 (map (fn [i] (/ (double (- n i)) (inc i))) (range k))))
(defn- sign-p [n k]
  (if (zero? n) 1.0
      (min 1.0 (* 2.0 (reduce + (map #(* (choose n %) (Math/pow 0.5 n)) (range k (inc n))))))))

(defn- paired-verdict [label a b]
  (let [d (mapv - a b)
        nz (filterv #(not (zero? %)) d)
        k (max (count (filter pos? nz)) (count (filter neg? nz)))
        p (sign-p (count nz) k)]
    {:label label :mean-delta (mean d) :n-informative (count nz)
     :wins (count (filter pos? nz)) :p p :better (and (< p 0.05) (pos? (mean d)))}))

;; ---------------- the loop ----------------
(defn- evolve
  "One arm of the loop. `mode` is the ONLY difference between the claim and its key null:
   both mutate identically, only the SURVIVOR RULE differs.

   COMMON RANDOM NUMBERS: mutation draws come from `rng`, selection draws from a SEPARATE
   `sel-rng`, so :drift's extra selection draw cannot desync its mutation stream from
   :evolved's. Without that split the two arms silently explore different sigma sequences
   and the control stops being a control."
  [mode gens pop train ticks ^java.util.Random rng ^java.util.Random sel-rng]
  ;; RETURNS THE BEST-EVER sigma, NOT THE LAST. Run 2 returned the last: train fitness
  ;; peaked at 99.1 (gen 3) and DECLINED to 42.6 (gen 5), so we scored a sigma we already
  ;; knew was worse. The loop also unconditionally jumped to the best CANDIDATE each
  ;; generation, which can be worse than the incumbent -- a biased random walk, not
  ;; hill-climbing. :evolved now keeps the incumbent unless a candidate beats it, and both
  ;; arms return their best-ever. (:drift must NOT hill-climb -- that is the whole point of
  ;; it -- so it keeps its random survivor and reports the best sigma it wandered onto,
  ;; which is the fair mutation-only null.)
  (loop [g 0 sigma (identity-sigma base-c)
         cur (fitness (identity-sigma base-c) train ticks)
         best-sigma (identity-sigma base-c)
         best cur]
    (if (>= g gens)
      best-sigma
      (let [cands (repeatedly pop #(transpose-sigma sigma rng))
            scored (mapv (fn [s] [s (fitness s train ticks)]) cands)
            ;; :evolved  -> keep the best (SELECTION ON)
            ;; :drift    -> keep a RANDOM one (SELECTION OFF, same mutations/churn)
            [cand-s cand-f] (case mode
                              :evolved (apply max-key second scored)
                              :drift (nth scored (.nextInt sel-rng (count scored))))
            ;; :evolved HILL-CLIMBS: keep the incumbent unless the candidate beats it.
            ;; :drift accepts unconditionally -- mutation with no selection, the null.
            [s' f'] (if (and (= mode :evolved) (< cand-f cur))
                      [sigma cur]
                      [cand-s cand-f])
            ties (count (distinct (map second scored)))]
        ;; ties matter: if every candidate scores the same, max-key and a random pick
        ;; COINCIDE and :drift is not a control at all -- it is :evolved wearing a hat.
        ;; At smoke scale (60 ticks, 3 seeds) that is exactly what happened.
        (println (format "    gen %2d  %-8s train-fit %8.2f   distinct-fitnesses %d/%d%s"
                         g (name mode) f' ties pop
                         (if (> f' best) " * new best" "")))
        (flush)
        (recur (inc g) s' f'
               (if (> f' best) s' best-sigma)
               (max best f'))))))

(defn -main [& args]
  (let [gens (Integer/parseInt (or (nth args 0 nil) "6"))
        pop (Integer/parseInt (or (nth args 1 nil) "6"))
        ntrain (Integer/parseInt (or (nth args 2 nil) "6"))
        ticks (Integer/parseInt (or (nth args 3 nil) "150"))
        ;; SCREEN FIRST, then split. Both sets come from the SAME screened pool and the
        ;; SAME rule, so train and test are exchangeable; the screen sees no arm and no
        ;; yield, only board geometry.
        _ (println "  screening boards (arm-independent: distance home -> nearest food)...")
        pool (screen-seeds (range 1 400) (+ ntrain 16))
        train (vec (take ntrain pool))
        test (vec (take 16 (drop ntrain pool)))   ; HELD OUT: never selected against
        _ (println (format "=== THE XENO LOOP ===\n  propagator on the ant C-vector; selection = foraging yield\n  %d gens x pop %d, %d ticks\n  screened pool (nearest food <= %.0f): train %s\n  HELD-OUT test %s\n  mutation = single TRANSPOSITION (curvature says that is where the space branches)\n"
                           gens pop ticks NEAREST-MAX train test))
        rng (java.util.Random. 7)
        _ (println "  --- evolving (selection ON) ---")
        s-evo (evolve :evolved gens pop train ticks rng (java.util.Random. 99))
        _ (println "  --- drift control (selection OFF, same mutations) ---")
        s-drift (evolve :drift gens pop train ticks (java.util.Random. 7) (java.util.Random. 99))
        ;; score everything on HELD-OUT seeds, per-seed (paired)
        ;; held-out scoring uses the SAME live-propagator path; baseline (sigma=nil) runs
        ;; the shipped C with no propagator at all, via the stock harness.
        per-seed (fn [sigma]
                   (if (nil? sigma)
                     (mapv (fn [s] (:yield (run-single SPECIES FOOD s (+ 10000 s) SIZE ticks false))) test)
                     (mapv (fn [s] (:yield (run-live sigma s ticks))) test)))
        y-evo (per-seed s-evo)
        y-drift (per-seed s-drift)
        y-ident (per-seed (identity-sigma base-c))
        y-base (per-seed nil)]
    (println "\n=== HELD-OUT SEEDS (never selected against) ===")
    (println (format "  %-10s %10s   %s" "arm" "mean yield" "what it is"))
    (doseq [[nm ys what] [["baseline" y-base "shipped C, no propagator"]
                          ["identity" y-ident "sigma = identity (preference noise, no coupling)"]
                          ["drift" y-drift "same mutations, NO selection"]
                          ["evolved" y-evo "selection ON"]]]
      (println (format "  %-10s %10.3f   %s" nm (mean ys) what)))
    (println "\n=== VERDICT (paired sign test over held-out seeds) ===")
    (let [v-base (paired-verdict "evolved vs baseline" y-evo y-base)
          v-drift (paired-verdict "evolved vs DRIFT" y-evo y-drift)]
      (doseq [v [v-base v-drift]]
        (println (format "  %-22s Δ %+8.3f  wins %d/%d  p %.4f  %s"
                         (:label v) (:mean-delta v) (:wins v) (:n-informative v) (:p v)
                         (if (:better v) "BEATS" "no"))))
      (println)
      (cond
        (and (:better v-base) (:better v-drift))
        (println "  XENO LOOP WORKS — evolved beats BOTH baseline and drift on held-out\n"
                 "                    seeds. Selection, not churn, improved behaviour.")
        (and (:better v-base) (not (:better v-drift)))
        (println "  CHURN, NOT EVOLUTION — evolved beats baseline but NOT drift. Mutation\n"
                 "                         alone explains it; selection added nothing.\n"
                 "                         This is the honest report, not a failure to hide.")
        :else
        (println "  NO IMPROVEMENT — evolved does not beat baseline on held-out seeds.")))
    (println (format "\n  evolved sigma: %s" (pr-str (into (sorted-map) s-evo))))
    (spit "/tmp/xeno-loop.edn" (pr-str {:evolved s-evo :y-evo y-evo :y-drift y-drift
                                        :y-ident y-ident :y-base y-base :test test}))
    (println "  wrote /tmp/xeno-loop.edn")
    (flush)))

(apply -main *command-line-args*)
