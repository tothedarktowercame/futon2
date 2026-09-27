(ns futon2.aif.zeta-posterior
  "Scalar B.19 likelihood precision for the token-rates lane."
  (:require [futon2.aif.cascade-model-manifest :as manifest]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.likelihood-precision :as precision]))

(def default-prior {:beta-prior 1 :authority :declared-default-pending-joe})
(defn- absent [reason detail] {:status :absent :reason reason :detail detail})
(defn- positive? [x] (and (number? x) (Double/isFinite (double x)) (pos? x)))
(defn- subsets [xs] (reduce (fn [ss x] (into ss (map #(conj % x) ss))) [#{}] xs))

(defn- contribution [{:keys [observation state rates]} zeta]
  (let [outcomes (subsets (sort-by pr-str (keys rates)))
        observation (if (sequential? observation) (zipmap outcomes observation) observation)
        a (into {} (for [s (keys state)] [s (into {} (for [o outcomes]
                                                      [o (manifest/token-likelihood rates s o)]))]))
        bad (first (for [[s row] a [o p] row :when (not (positive? p))] [s o p]))]
    (if bad (absent :nonpositive-likelihood {:cell bad})
        (let [tempered (precision/tempered-rates rates zeta)
              az (when-not (precision/refusal? tempered)
                   (into {} (for [s (keys state)]
                              [s (into {} (for [o outcomes]
                                            [o (reduce * 1 (for [[t {:keys [false-neg false-pos]}] tempered]
                                                             (if (contains? s t)
                                                               (if (contains? o t) (- 1 false-neg) false-neg)
                                                               (if (contains? o t) false-pos (- 1 false-pos)))))]))])))]
          (if (precision/refusal? tempered) (absent (:kind tempered) tempered)
              ;; Enumerate the independent Bernoulli product of the effective
              ;; rates; only tempered-rates implements the tempering law.
              (reduce + 0 (for [o outcomes [s mass] state]
                            (* (- (reduce + 0 (for [[s' q] state] (* (get-in az [s' o]) q)))
                                  (get observation o 0))
                               (Math/log (double (get-in a [s o]))) mass))))))))

(defn beta-posterior
  "Recompute B.19 from the ORIGINAL declared beta-prior over all ordered trials
   at ONE current ζ. A is the token rates map; a trial's :rates overrides it
   with that step's recorded A. This generalizes Lean's fixed-A signature and
   coincides with it when A is constant. Scalar ζ is the declared simplification
   of B.19's row vector. :state is :predicted-state, never prior or posterior.
   AIF validity: log requires strictly positive cells; inversion requires a
   positive beta. Refusals are annotations, never selection gates."
  [trials A beta-prior zeta]
  (let [trials (mapv #(update % :rates (fn [r] (or r A))) trials)
        terms (when (and (positive? beta-prior) (positive? zeta))
                (mapv #(contribution % zeta) trials))
        failure (some #(when (map? %) %) terms)]
    (cond
      (not (positive? beta-prior)) (absent :nonpositive-beta-prior {:beta-prior beta-prior})
      (not (positive? zeta)) (absent :invalid-zeta {:zeta zeta})
      failure failure
      :else
      (let [post (+ beta-prior (reduce + 0 terms))]
        (if-not (positive? post) (absent :nonpositive-beta-post {:beta-post post})
          {:schema :wm/zeta-posterior-v1 :scope :token-rates-lane
           :lean-scope :fixed-A-signature :beta-prior beta-prior :beta-post post
           :zeta-mean (/ 1 post) :evaluated-at-zeta zeta :trials (count trials)
           :basis (if (seq trials) :posterior :prior-no-trials)
           :trajectory-digest (evidence/value-digest
                               [(mapv (fn [t] [(:click-id t) (:observation t) (:state t)
                                               (evidence/value-digest (:rates t))]) trials) zeta])})))))

(defn next-zeta [posterior] (:zeta-mean posterior))

(defn retain-lane
  "Retain the target's actual lane scoring receipt, never the joint class A.
   Older run records do not persist this lane receipt: missing is typed."
  [run-record target attempts]
  (let [sc (get-in run-record [:decision :selection-certificate :token-rate-lanes target])
        checked (set (map :produced attempts))]
    (if (and (map? (get-in sc [:precision-model :rates]))
             (get-in sc [:rates-provenance :measurement])
             (every? #(contains? (get-in sc [:precision-model :rates]) %) checked))
      {:scope :token-rates-lane :target target :checked-tokens checked
       :rates (select-keys (get-in sc [:precision-model :rates]) checked)
       :rates-provenance (:rates-provenance sc) :zeta (:zeta sc)}
      (absent :rates-provenance-missing {:target target}))))

(defn- trial [record]
  (let [{:keys [target checked-tokens rates] :as lane} (:zeta-likelihood record)
        posterior (:temporal-posterior record)]
    (if (or (nil? lane) (= :absent (:status lane)))
      (absent :rates-provenance-missing {:click-id (:click record)})
      {:click-id (get-in posterior [:model :click-id]) :rates rates
       :observation {(if (:observation posterior) checked-tokens #{}) 1}
       :state (reduce-kv (fn [acc s mass]
                          (let [local (set (for [[t token] s
                                                :when (and (= t target) (contains? checked-tokens token))] token))]
                            (update acc local (fnil + 0) mass))) {} (:predicted-state posterior))})))

(defn trajectory-posterior
  "Records are in cursor order, including the new successful publication.
   Replay the full trajectory from its declared prior, never from beta-post.
   Missing historical provenance refuses the whole trajectory, not just a term."
  [records]
  (let [prior (or (:zeta-prior (first records)) default-prior)
        previous (:zeta-posterior (last (butlast records)))
        zeta (if (= :posterior (:basis previous)) (:zeta-mean previous)
                 (when (and (= 1 (count records)) (positive? (:beta-prior prior)))
                   (/ 1 (:beta-prior prior))))
        trials (mapv trial records)]
    (or (when-not (positive? (:beta-prior prior))
          (absent :nonpositive-beta-prior prior))
        (some #(when (= :absent (:status %)) %) trials)
        (when-not zeta (absent :previous-zeta-unavailable previous))
        (assoc (beta-posterior trials nil (:beta-prior prior) zeta) :prior-declaration prior))))


(defn lane-options
  "Only the existing temporal admission's verified receipt may supply ζ.
   A rejected envelope's mean is never lifted. A declared trajectory start
   has no trials; unavailable later publications keep their typed reason.
   This annotates the lane and never changes the joint class-emission model."
  [receipt]
  (let [status (:conditioning-status receipt)
        p (get-in receipt [:temporal-previous :zeta-posterior])]
    (cond
      (= :trajectory-start status)
      {:zeta-basis (assoc default-prior :basis :prior-no-trials)}
      (not= :temporal-posterior status)
      {:zeta-basis {:absent (or status :no-temporal-envelope)}}
      (and (= :posterior (:basis p)) (positive? (:zeta-mean p)))
      {:zeta (:zeta-mean p)
       :zeta-basis (select-keys p [:basis :trajectory-digest :zeta-mean])}
      (= :prior-no-trials (:basis p))
      {:zeta-basis (select-keys p [:basis :beta-prior])}
      :else {:zeta-basis {:absent (or (:reason p) :zeta-posterior-unavailable)}})))
