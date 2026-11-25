(ns futon2.aif.r1-belief
  "Pure port of R1 in the deep temporal form (mathlib4 worktree
  c14-cascade-outcome, branch c14/cascade-outcome-type):

  - DarkTower/WarMachine/BenefitBelief.lean   : Rung, Rung.up, climb
  - DarkTower/WarMachine/R1DeepTemporal.lean  : SlowBelief, lik, evid, post,
    update, predict, CauseCounts, theta, learn
  - DarkTower/WarMachine/R1DeepTemporalB17.lean : the B17 instance numbers
    (uniform prior, traceProb; see the test namespace)

  All arithmetic is exact (Clojure ratios) wherever the Lean uses real
  literals, so the transcribed theorem numbers (3/34, 17/25) come out
  exactly. No wiring into the click, selection, or runner: this namespace
  is data and pure functions only (handoff 1 of
  ~/notes/claude-14/P-r1-benefit-belief-port-2026-11-25.md)."
  (:require [clojure.set :as set]))

;;; ## Rungs (BenefitBelief.Rung)

(def rungs
  "The reach side of the evidential ladder, weakest first."
  [:none :available :used :witnessed :realised])

(defn up
  "Rung.up: one rung up; the top rung stays."
  [rung]
  (case rung
    :none :available
    :available :used
    :used :witnessed
    :witnessed :realised
    :realised :realised))

;;; ## Argued records: Pearl's rule (R1ArguedRecords.lean)

(defn reading-channel
  "R1ArguedRecords.readingChannel: a reader claiming rung k0 with
  reliability r is a channel from the true rung to the reader's say-so.
  For the record kind :says-true (the reader says the benefit is at k0)
  the likelihood is r at k0 and 1 - r elsewhere; for :says-false (the
  reader says it is not) the likelihood is the complement. Pearl's
  virtual evidence: factor the reading in as a likelihood, so iterated
  readings commute (Jacobs 2019 Prop. 4.3(3), via update-cons).

  r is REQUIRED and must be a number in [0, 1] (the Lean hypotheses h0,
  h1): there is no default reliability. Refuses (ex-info) a missing r, a
  non-number r, r outside [0, 1], or a k0 that is not a rung. Exact
  ratios throughout."
  ([k0]
   (throw (ex-info "reading-channel requires an explicit reliability r in [0, 1] (no default)"
                   {:type :missing-reliability :k0 k0})))
  ([k0 r]
   (when-not (contains? (set rungs) k0)
     (throw (ex-info "reading-channel: claimed rung must be one of the R1 rungs"
                     {:type :invalid-rung :k0 k0})))
   (when-not (and (number? r) (<= 0 r 1))
     (throw (ex-info "reading-channel: reliability must be a number in [0, 1]"
                     {:type :invalid-reliability :r r})))
   (fn [rung kind]
     (case kind
       :says-true (if (= rung k0) r (- 1 r))
       :says-false (if (= rung k0) (- 1 r) r)))))

;;; ## Slow belief (R1DeepTemporal.SlowBelief)

(defn check-belief
  "Validate a slow belief: a map from every rung to a nonnegative number,
  summing to exactly 1. Returns the belief; refuses (ex-info) otherwise,
  as the Lean structure's `nonneg`/`sum_one` fields rule the value out."
  [belief]
  (when-not (map? belief)
    (throw (ex-info "slow belief must be a map {rung prob}"
                    {:type :invalid-belief :belief belief})))
  (let [missing (set/difference (set rungs) (set (keys belief)))
        extra (set/difference (set (keys belief)) (set rungs))]
    (when (or (seq missing) (seq extra))
      (throw (ex-info "slow belief must assign a probability to every rung and nothing else"
                      {:type :invalid-belief :missing missing :extra extra})))
    (doseq [[rung p] belief]
      (when-not (and (number? p) (not (neg? p)))
        (throw (ex-info "slow belief entries must be nonnegative numbers"
                        {:type :invalid-belief :rung rung :prob p}))))
    (when-not (== 1 (reduce + 0 (vals belief)))
      (throw (ex-info "slow belief must sum to 1"
                      {:type :invalid-belief :sum (reduce + 0 (vals belief))})))
    belief))

(defn uniform-belief
  "R1DeepTemporalB17.uniform: 1/5 on every rung."
  []
  (zipmap rungs (repeat 1/5)))

;;; ## Likelihood (the A channel)

(defn- check-prob
  "A likelihood entry is a probability: a number in [0, 1]. Refuses
  otherwise (a Lean ProbabilityKernel mass is nonnegative and at most 1,
  since each row sums to 1)."
  [rung kind p]
  (when-not (and (number? p) (<= 0 p 1))
    (throw (ex-info "likelihood must be a probability in [0, 1]"
                    {:type :invalid-likelihood :rung rung :kind kind :prob p})))
  p)

(defn likelihood
  "Accept the likelihood A either as a function (fn [rung record-kind] ->
  prob) or as a nested map {rung {kind prob}}. Returns a function
  [rung record-kind] -> prob that refuses entries outside [0, 1]."
  [A]
  (cond
    (fn? A) (fn [rung kind] (check-prob rung kind (A rung kind)))
    (map? A) (fn [rung kind]
               (check-prob rung kind (get-in A [rung kind] 0)))
    :else (throw (ex-info "likelihood must be a function or a nested map"
                          {:type :invalid-likelihood :A A}))))

(defn- record-kind
  "Records may be plain keywords (record kinds) or maps with a :kind key
  (as in the B17 example data)."
  [record]
  (if (map? record) (:kind record) record))

(defn lik
  "R1DeepTemporal.lik, accepting records as keywords or {:kind ...} maps."
  [A records rung]
  (let [Af (likelihood A)]
    (reduce * 1 (map #(Af rung (record-kind %)) records))))

(defn evid
  "R1DeepTemporal.evid: the evidence for a click's records under `belief`."
  [belief A records]
  (check-belief belief)
  (reduce + 0 (map (fn [rung] (* (get belief rung) (lik A records rung))) rungs)))

(defn post
  "R1DeepTemporal.post: the posterior over rungs after a click's records
  (Bayes, once). If the evidence is zero the records are impossible under
  the prior at every rung, and the Lean update's hypothesis `0 < evid`
  cannot be met: refuse (ex-info :zero-evidence), never divide."
  [belief A records]
  (let [e (evid belief A records)]
    (when-not (pos? e)
      (throw (ex-info "zero evidence: records impossible under the prior at every rung"
                      {:type :zero-evidence :records records})))
    (into {} (map (fn [rung]
                    [rung (/ (* (get belief rung) (lik A records rung)) e)])
                  rungs))))

(defn update-belief
  "R1DeepTemporal.update: the slow belief after a click's records. This is
  `post`; the name mirrors the Lean. `update_cons` (a test in the test
  namespace) guarantees updating record by record equals updating with all
  records at once, so no record counts twice."
  [belief A records]
  (post belief A records))

;;; ## Prediction between clicks (R1DeepTemporal.predict)

(defn climb
  "BenefitBelief.climb as a kernel {from {to prob}}: from :realised the
  benefit stays at :realised; otherwise it goes up one rung with
  probability theta and stays with 1 - theta."
  [theta]
  (into {} (map (fn [rung]
                  [rung (if (= :realised rung)
                          {:realised 1}
                          {rung (- 1 theta) (up rung) theta})])
                rungs)))

(defn predict
  "R1DeepTemporal.predict: the slow level's prediction between clicks. With
  probability pD the work delivers and the benefit climbs with reliability
  theta; otherwise it stays."
  [belief theta pD]
  (check-belief belief)
  (let [kernel (climb theta)]
    (into {} (map (fn [k]
                    [k (+ (* pD (reduce + 0 (map (fn [j]
                                                   (* (get belief j)
                                                      (get-in kernel [j k] 0)))
                                                 rungs)))
                          (* (- 1 pD) (get belief k)))])
                  rungs))))

;;; ## R1-cause: learnt per source rung (CauseCounts, theta, learn)

(defn check-cause-counts
  "Validate cause counts: {link {rung [climb stay]}}, every entry a pair of
  positive numbers (the Lean structure's `pos` field). Returns the counts;
  refuses otherwise."
  [counts]
  (when-not (map? counts)
    (throw (ex-info "cause counts must be a map {link {rung [climb stay]}}"
                    {:type :invalid-cause-counts :counts counts})))
  (doseq [[link cols] counts]
    (when-not (and (map? cols) (= (set rungs) (set (keys cols))))
      (throw (ex-info "cause counts per link must be a map {rung [climb stay]} covering every rung"
                      {:type :invalid-cause-counts :link link :cols cols})))
    (doseq [[rung pair] cols]
      (when-not (and (contains? (set rungs) rung)
                     (vector? pair) (= 2 (count pair))
                     (every? #(and (number? %) (pos? %)) pair))
        (throw (ex-info "cause count columns must be [climb stay] pairs of positive numbers on rungs"
                        {:type :invalid-cause-counts :link link :rung rung :pair pair})))))
  counts)

(defn theta
  "R1DeepTemporal.theta: reliability of `link` from starting rung `rung`,
  climb / (climb + stay)."
  [counts link rung]
  (check-cause-counts counts)
  (let [[c s] (get-in counts [link rung])]
    (/ c (+ c s))))

(defn learn
  "R1DeepTemporal.learn (Da Costa eq. 32, b = b + sτ ⊗ sτ−1): column `rung`
  of `link` gains sPrev(rung) * w climbs and sPrev(rung) * (1 - w) stays,
  where `s-prev` is the slow belief before the click and `w` the posterior
  that the climb occurred. Other links are untouched (learn_other_link); a
  starting rung the slow belief rules out gains nothing (learn_other_col)."
  [counts link s-prev w]
  (check-cause-counts counts)
  (check-belief s-prev)
  (when-not (and (number? w) (<= 0 w 1))
    (throw (ex-info "climb posterior w must be a probability in [0, 1]"
                    {:type :invalid-weight :w w})))
  (update counts link
          (fn [cols]
            (into {} (map (fn [rung]
                            (let [[c s] (get cols rung [0 0])
                                  mass (get s-prev rung)]
                              [rung [(+ c (* mass w)) (+ s (* mass (- 1 w)))]]))
                          rungs)))))
