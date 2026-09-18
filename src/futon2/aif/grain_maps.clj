(ns futon2.aif.grain-maps
  "Mirror of mathlib4 DarkTower/AIF/GrainMaps.lean (9ea6f2a23c +
  284610789f + f5cad1ede8): the mission-to-cascade preference pullback at
  WEIGHT level. The Clojure mirrors the module's LAWS, not its signatures.

  The problem (WIRE-3, 2026-09-18): the live C is derived at mission grain;
  cascade decisions score outcomes at cascade-token grain; the vocabularies
  are disjoint (:derived-no-overlap). This namespace states what
  legitimately induces the finer-grain preference: a PULLBACK OF WEIGHTS
  along a PARTIAL grain map g — a cascade outcome either closes a mission
  event (g o' = m) or maps to nothing (absent from g).

  The four laws, as built in the Lean and mirrored here:
  1. PULL-ONLY DIRECTION. C pulls back; Q never moves grain at runtime.
     The pushforward exists only as the conservativity CHECK
     (pushforward-eq-mission-weight); no function in this namespace
     transports Q.
  2. ZEROS ARE EXACT. A pulled weight is zero iff the outcome is unmapped
     (NEUTRAL weight 0 — absent from want, enlarging Z downstream, c_v =
     sigma(0) = 1/2: neither wanted nor unwanted, and NOT a hard zero) or
     its mission weight is zero. No renormalisation exists in the pullback,
     so nothing can smooth or mint a zero.
  3. RENORMALISATION LIVES DOWNSTREAM. Pulled weights feed the
     already-live Z-normalisation of cascade-model-manifest/log-preference-fn;
     this namespace introduces NO second normaliser. Unreachable mission
     mass simply never arrives (total-pulled = reachable mission mass), and
     COVERAGE — the reachable fraction of mission mass, an exact rational —
     is a recorded quantity. coverage = 0 is the :derived-no-overlap point
     of the same scale, not a special case.
  4. THE WITHIN-FIBER SPLIT IS A DECLARED DEFAULT. uniform-split satisfies
     the FiberSplit law; any future institutional weighting
     (preference-over-HOW) replaces the split, not the pullback. The
     default is carried VISIBLY as {:fiber-split :uniform-declared}.

  AMPLIFICATION (pull-zeroed, the D-ZERO-PATH interaction): the hard-zero
  set pulls back as the EXACT PREIMAGE — a cascade outcome is zeroed iff
  it closes a zeroed mission event. Exact preservation means a
  mission-grain zero fans out to its ENTIRE fiber, and under non-zero
  observation rates Q has full support, so a hard zero anywhere in the
  preimage refuses the whole comparison family. A mission-level 'never do
  this' is a strictly stronger constraint at cascade grain than the same
  zero declared there directly. Intended semantics, recorded so a refusal
  is traceable to the zero its author declared two grains away.

  All arithmetic is exact rationals. Pure; no wiring — the live consumer
  (live-c/cascade-spec) is a separate slice with its own ruling on where
  the grain map is declared."
  )

(defn fiber
  "Mirror of GrainMaps.fiber: the cascade outcomes that close mission event
  m. Deterministic order (sort by print string) so arithmetic is stable."
  [g m]
  (vec (sort-by pr-str (keep (fn [[o' m']] (when (= m m') o')) g))))

(defn fiber-split?
  "Mirror of GrainMaps.FiberSplit: s is nonnegative everywhere and sums to
  exactly 1 on every nonempty fiber."
  [g s]
  (and (every? (fn [w] (and (number? w) (not (neg? (double w))))) (vals s))
       (every? (fn [m]
                 (let [f (fiber g m)]
                   (or (empty? f)
                       (= 1 (reduce + (map #(get s % 0) f))))))
               (set (vals g)))))

(defn uniform-split
  "The declared default split (GrainMaps.uniformSplit): 1/|fiber| on every
  mapped outcome."
  [g]
  (into {} (map (fn [[o' m]] [o' (/ 1 (count (fiber g m)))])) g))

(defn pull-weights
  "Mirror of GrainMaps.pullWeight with a validated split: a mapped outcome
  carries s(o')·w(m); an unmapped outcome carries weight 0 (NEUTRAL, never
  a hard zero — law 2). Refuses typed {:kind :invalid-fiber-split} when s
  is not a FiberSplit, naming the offending fiber — the refusal stops the
  pullback, it does not fall back to uniform silently.

  Returns {:weights {cascade-outcome exact-rational} :fiber-split
  {:uniform-declared true} when s is the uniform default (the caller
  passes (uniform-split g)); a non-default split records
  {:fiber-split :declared} — the default must stay visible and nothing may
  assume it stays uniform}. w maps mission events to nonnegative exact
  rationals; a negative or non-rational mission weight refuses typed
  {:kind :invalid-mission-weight}."
  [g s w]
  (let [[bad-m bad-x :as bad-w]
        (some (fn [[m x]] (when-not (and (or (ratio? x) (integer? x))
                                         (not (neg? x)))
                            [m x]))
              w)]
    (cond
      bad-w {:status :missing :kind :invalid-mission-weight
             :mission-event bad-m :value bad-x}
      (not (fiber-split? g s))
      (let [offender (some (fn [m]
                             (let [f (fiber g m)]
                               (when (and (seq f)
                                          (not= 1 (reduce + (map #(get s % 0) f))))
                                 {:mission-event m :fiber f})))
                           (sort-by pr-str (set (vals g))))]
        {:status :missing :kind :invalid-fiber-split :offender offender})
      :else
      (let [uniform? (= s (uniform-split g))]
        {:weights (into {}
                        (map (fn [o']
                               [o' (if-let [m (get g o')]
                                     (* (get s o' 0) (get w m 0))
                                     0)]))
                        (set (keys g)))
         :fiber-split (if uniform? :uniform-declared :declared)}))))

(defn pushforward-eq-mission-weight
  "The conservativity CHECK (GrainMaps.pushforward_pullWeight, stated as a
  theorem there, computed here): the sum of pulled weights over m's fiber
  equals w(m) exactly, on every mission event with a nonempty fiber. There
  is no renormalisation anywhere in this computation — conservativity is
  EXACT. Returns a map {mission-event [pulled-sum w-m]} so callers and
  tests can assert equality; unreachable events are absent."
  [g s w]
  (into {}
        (map (fn [m]
               (let [f (fiber g m)]
                 [m [(reduce + (map (fn [o'] (* (get s o' 0) (get w m 0))) f))
                     (get w m 0)]])))
        (set (vals g))))

(defn coverage
  "The recorded quantity (GrainMaps sum_pullWeight's corollary): reachable
  mission mass / total mission mass, an exact rational in [0,1]. coverage
  = 0 is exactly the :derived-no-overlap point — the live C has nothing to
  say in this comparison — and is a recorded value, never an error.
  Refuses typed {:kind :empty-mission-mass} when the total mission mass is
  zero (the ratio is undefined; the caller's own empty-belly refusal
  applies first in every real flow)."
  [g w]
  (let [total (reduce + (vals w))]
    (if (zero? total)
      {:status :missing :kind :empty-mission-mass}
      (let [reachable (set (vals g))
            reached (reduce + (keep #(get w %) reachable))]
        (/ reached total)))))

(defn total-pulled
  "GrainMaps.sum_pullWeight, computed: the total pulled mass equals the
  REACHABLE mission mass — unreachable mission mass never arrives at
  cascade grain. Exact rational."
  [g s w]
  (reduce + (vals (:weights (pull-weights g s w)))))

(defn pull-zeroed
  "Mirror of GrainMaps.pullZeroed: the EXACT PREIMAGE of the mission
  hard-zero set — a cascade outcome is zeroed iff it closes a zeroed
  mission event. See the AMPLIFICATION note in the ns docstring: this fans
  a mission grain zero out to its entire fiber, and under non-zero
  observation rates that refuses the whole comparison family."
  [g mission-zeroed]
  (set (keep (fn [[o' m]] (when (contains? (set mission-zeroed) m) o')) g)))
