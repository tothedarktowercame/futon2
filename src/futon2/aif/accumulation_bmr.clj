(ns futon2.aif.accumulation-bmr
  "ITEM6-ADAPTER-I: the explicit seven-column (channel-given-status) adapter
   from machine-accumulation state to Dirichlet factors, plus an aggregate
   BMR score emitted as a replayable receipt.

   Declared model: each status s indexes an independent Dirichlet over the
   fourteen channels (reading 2, \"channel given status\", of
   holes/labs/wm-contract/ITEM6-NORMALIZER-D.md). This is provisionally
   declared per Joe's 2026-09-27 rulings: the score is consumed and recorded
   as a replayable receipt only; adoption is deferred and NOT decided here.

   Label: \"prototyping our way forward, not closure of the proof\".

   Pure namespace: no IO. All scoring goes through futon2.aif.bmr's existing
   ln-B machinery (`log-multivariate-beta`, bmr.clj:84-90, and
   `bayesian-model-reduction`, bmr.clj:108-138); log-gamma is NOT
   reimplemented here."
  (:require [futon2.aif.bmr :as bmr])
  (:import [java.security MessageDigest]
           [java.nio.charset StandardCharsets]))

(def receipt-schema :wm/accumulation-bmr-score-v1)

(def declared-model :channel-given-status)

(def prototype-label "prototyping our way forward, not closure of the proof")

(defn- sha256-hex [s]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes (pr-str s) StandardCharsets/UTF_8))]
    (apply str (map #(format "%02x" %) digest))))

(defn- support-order
  "Read the DECLARED ordered supports from the accumulation state
   (machine_accumulation.clj:19, `[:support :observation]` /
   `[:support :state]`, both vectors in declared order). Refuses
   :unordered-supports when the state carries no order."
  [state]
  (let [channels (get-in state [:support :observation])
        statuses (get-in state [:support :state])]
    (if (and (vector? channels) (seq channels)
             (vector? statuses) (seq statuses))
      {:channels channels :statuses statuses}
      {:status :refused :kind :unordered-supports})))

(defn factors
  "From the machine accumulation state ({channel {status concentration}},
   machine_accumulation.clj:19-21,38-42) to SEVEN ordered Dirichlet
   vectors: one per status (in the state's declared status support order),
   each over the channels in the state's DECLARED observation support
   order — never alphabetical by accident.

   Returns [f1 .. f7] on success. When the state carries no ordered
   supports, refuses {:status :refused :kind :unordered-supports}. A
   missing cell is a typed absence
   {:status :absent :kind :missing-cell :channel c :status-key s} —
   never 0 and never the prior."
  [state]
  (let [{:keys [channels statuses] :as order} (support-order state)]
    (if (:status order)
      order
      (let [cells (for [s statuses c channels]
                    [s c (get-in state [:concentrations c s] ::missing)])
            missing (first (filter #(= ::missing (nth % 2)) cells))]
        (if missing
          {:status :absent :kind :missing-cell
           :channel (nth missing 1) :status-key (nth missing 0)}
          (mapv (fn [s]
                  (mapv (fn [c] (get-in state [:concentrations c s]))
                        channels))
                statuses))))))

(defn parent-prior
  "The actual declared initialization as seven vectors (one per status,
   over channels in declared support order). The state records a SCALAR
   prior (machine_accumulation.clj:7-21, `[:initialization :prior]`,
   authority :declared); this is that scalar broadcast across every cell —
   the broadcast is what the state records, not an invented per-cell prior.

   Returns [p1 .. p7], or the :unordered-supports refusal, or
   {:status :refused :kind :no-declared-initialization} when the state
   carries no declared initialization."
  [state]
  (let [{:keys [channels statuses] :as order} (support-order state)]
    (if (:status order)
      order
      (let [prior (get-in state [:initialization :prior])]
        (if (number? prior)
          (mapv (fn [_s] (mapv (fn [_c] prior) channels)) statuses)
          {:status :refused :kind :no-declared-initialization})))))

(defn- refusal? [x] (and (map? x) (:status x)))

(defn proposal
  "A positive soft-prior constraint. `deltas` is {[channel status] d} with
   every d a positive rational; the reduced prior is a' = a + deltas
   (ruling: no merging, no negative deltas).

   Returns the reduced prior as seven vectors (same shape as
   `parent-prior`), or a typed refusal:
   {:status :refused :kind :non-positive-delta :cell [c s] :delta d}
   {:status :refused :kind :unknown-cell :cell [c s]}"
  [state deltas]
  (let [{:keys [channels statuses] :as order} (support-order state)]
    (if (:status order)
      order
      (let [channel-set (set channels)
            status-set (set statuses)
            unknown (first (filter (fn [[[c s] _d]]
                                     (not (and (contains? channel-set c)
                                               (contains? status-set s))))
                                   deltas))
            non-positive (first (filter (fn [[_c d]]
                                          (not (and (rational? d) (pos? d))))
                                        deltas))]
        (cond
          unknown {:status :refused :kind :unknown-cell :cell (key unknown)}
          non-positive {:status :refused :kind :non-positive-delta
                        :cell (key non-positive) :delta (val non-positive)}
          :else
          (let [a (parent-prior state)
                s-index (into {} (map-indexed (fn [i s] [s i]) statuses))
                c-index (into {} (map-indexed (fn [i c] [c i]) channels))]
            (reduce (fn [prior [[c s] d]]
                      (update-in prior [(s-index s) (c-index c)] + d))
                    a deltas)))))))

(defn- state-fingerprint [state]
  {:concentrations (:concentrations state)
   :support (:support state)
   :initialization (:initialization state)})

(defn score
  "Aggregate BMR score of the seven channel-given-status factors under a
   positive soft-prior proposal, as a replayable receipt.

   For each of the seven factors, calls bmr/log-multivariate-beta
   (bmr.clj:84-90) on a, A, a', and A' (A' = A + a' - a, from
   bmr/bayesian-model-reduction, bmr.clj:108-138), and takes that
   function's :delta-F. The aggregate :delta-f is the SUM of the seven
   differences. The threshold rule is recorded as {:threshold -3
   :applied false}: adoption is NOT decided here (no :accepted? key).

   On a missing cell returns the typed absence (no :delta-f). On bad
   inputs returns the typed refusal from `factors`/`proposal`."
  [state deltas]
  (let [{:keys [channels statuses] :as order} (support-order state)]
    (if (:status order)
      order
      (let [a (parent-prior state)
            big-a (factors state)
            small-a (proposal state deltas)
            inputs {:state-digest (sha256-hex (state-fingerprint state))
                    :deltas-digest (sha256-hex (into (sorted-map) deltas))
                    :support-order {:observation channels :state statuses}}]
        (cond
          (refusal? big-a) (assoc big-a :schema receipt-schema :inputs inputs)
          (refusal? small-a) (assoc small-a :schema receipt-schema :inputs inputs)
          :else
          (let [per-factor
                (mapv (fn [s a-v A-v a'-v]
                        (let [result (bmr/bayesian-model-reduction a-v A-v a'-v)
                              A'-v (:reduced-posterior result)]
                          {:status s
                           :ln-b {:a (bmr/log-multivariate-beta a-v)
                                  :A (bmr/log-multivariate-beta A-v)
                                  :a' (bmr/log-multivariate-beta a'-v)
                                  :A' (bmr/log-multivariate-beta A'-v)}
                           :delta-f (:delta-F result)}))
                      statuses a big-a small-a)]
            {:schema receipt-schema
             :declared-model declared-model
             :factors 7
             :label prototype-label
             :per-factor per-factor
             :delta-f (reduce + (map :delta-f per-factor))
             :inputs inputs
             :rule {:threshold -3 :applied false}}))))))
