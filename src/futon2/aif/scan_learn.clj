(ns futon2.aif.scan-learn
  "Pure online variational learner for the raw-scan observation model A.

   One declared system-proxy status generates the raw scan channels.  Each
   admitted tick first predicts q through B_rho=(1-rho)I+rho*Uniform, then uses
   the current Beta/Dirichlet posterior predictive likelihoods to infer q, and
   finally adds q-weighted outcome counts to A.  Run ids are admitted once.

   Informative initial concentrations are smoothed versions of the declared
   hand-set status rows (PROOF-2a decisions 6B-2a and 6B-7); rows without a
   probability interpretation are exchangeable.  Typed absent/refused inputs
   are no trial.  This namespace never reads production :mu-pre or :mu-post."
  (:require [futon2.aif.belief :as belief]
            [futon2.aif.bmr :as bmr]
            [futon2.aif.scan-bins :as scan-bins]
            [futon2.aif.trace :as trace]))

(def schema :wm/scan-learn-v1)
(def delta 1/10)
(def kappa 2)
(def rho 1/20)
(def authority "PROOF-2a decisions 6B-2a, 6B-7 (2026-09-29)")

(def ^:private statuses (vec (sort belief/status-set)))
(def ^:private workstreams [:stack :consulting :portfolio :mathematics])
(def ^:private unit-outcomes [0 1 2 3 4 :underflow :overflow])
(def ^:private sorry-outcomes [0 1 2 3 4 5 6 7 8 9 :ge10])
(def ^:private exposure-keys
  [:support :attack :workstream-commits :active-repos :coupling :ticks :sorrys
   :annotation :loop-health :mission-health :depositing-signal])
(def ^:private zero-channel-ticks (zipmap exposure-keys (repeat 0)))

(def ^:private models
  {:support {:family :binomial :fields [:covered :claims]
             :hand-set :support-coverage}
   :attack {:family :binomial :fields [:covered :claims]
            :hand-set :attack-coverage}
   :workstream-commits {:family :multinomial :alphabet workstreams}
   :active-repos {:family :binomial :fields [:active :repositories]
                  :hand-set :active-repo-ratio}
   :coupling {:family :binomial :fields [:edges :possible]
              :hand-set :coupling-density}
   :ticks {:family :binomial :fields [:fired :eligible]
           :hand-set :ticks-firing-ratio}
   :sorrys {:family :categorical :alphabet sorry-outcomes
            :hand-set :sorry-count-norm}
   :annotation {:family :categorical :alphabet unit-outcomes}
   :loop-health {:family :categorical :alphabet unit-outcomes}
   :mission-health {:family :categorical :alphabet unit-outcomes
                    :hand-set :mission-health}
   :depositing-signal {:family :categorical :alphabet unit-outcomes}})

(defn- absent-or-refused? [x]
  (and (map? x) (contains? #{:absent :refused} (:status x))))

(defn- counts-outcome [family alphabet counts]
  {:family family :alphabet alphabet :counts (vec counts)})

(defn outcome
  "Translate one exposure KEY/value into fixed-alphabet counts, or nil when the
   exposure is absent, refused, or malformed."
  [key exposure]
  (when-not (or (nil? exposure) (absent-or-refused? exposure))
    (let [{:keys [family fields alphabet]} (get models key)]
      (case family
        :binomial
        (let [[success-field trials-field] fields
              success (get exposure success-field)
              trials (get exposure trials-field)]
          (when (and (integer? success) (integer? trials)
                     (pos? trials) (<= 0 success trials))
            (counts-outcome :binomial [:success :failure]
                            [success (- trials success)])))

        :multinomial
        (let [counts (:counts exposure)
              xs (mapv #(get counts %) alphabet)]
          (when (and (= (set alphabet) (set (keys counts)))
                     (every? #(and (integer? %) (not (neg? %))) xs)
                     (pos? (reduce + xs))
                     (= (reduce + xs) (:total exposure)))
            (counts-outcome :multinomial alphabet xs)))

        :categorical
        (let [observed
              (case key
                :sorrys (let [n (:count exposure)]
                          (when (and (integer? n) (not (neg? n)))
                            (if (< n 10) n :ge10)))
                :annotation
                (let [anomalies (:anomalies exposure)
                      sections (:sections exposure)]
                  (when (and (integer? anomalies) (not (neg? anomalies))
                             (integer? sections) (pos? sections))
                    (:bin (scan-bins/bin scan-bins/unit-5-v1
                                         (/ anomalies sections)))))
                (:bin exposure))
              i (first (keep-indexed #(when (= %2 observed) %1) alphabet))]
          (when (some? i)
            (counts-outcome :categorical alphabet
                            (assoc (vec (repeat (count alphabet) 0)) i 1))))
        nil))))

(defn- uniform-concentrations [n]
  (vec (repeat n (/ kappa n))))

(defn- bin-index [x]
  (:bin (scan-bins/bin scan-bins/unit-5-v1 x)))

(defn- informative-concentrations [key status]
  (let [{:keys [family alphabet hand-set]} (get models key)
        raw-h (get-in belief/channel-emission-matrix [hand-set status])]
    (when (and hand-set (number? raw-h) (<= 0 raw-h 1))
      (let [h (rationalize raw-h)]
        (case family
          :binomial
          (let [m (+ (* (- 1 delta) h) (* delta 1/2))]
            [(* kappa m) (* kappa (- 1 m))])

          :categorical
          (let [n (count alphabet)
                predicted (case key
                            :sorrys (if (== 1 h) :ge10 0)
                            :mission-health (bin-index h))
                i (first (keep-indexed #(when (= %2 predicted) %1) alphabet))
                base (/ (* kappa delta) n)]
            (assoc (vec (repeat n base)) i (+ base (* kappa (- 1 delta)))))
          nil)))))

(defn prior-state
  "Construct the authorised initial learner state.  Concentrations are exact
   rationals and q0 is uniform over the live status vocabulary."
  []
  {:schema schema
   :delta delta
   :kappa kappa
   :rho rho
   :authority authority
   :statuses statuses
   :q (zipmap statuses (repeat (/ 1 (count statuses))))
   :channel-ticks zero-channel-ticks
   :concentrations
   (into {}
         (for [key exposure-keys
               :let [n (if (= :binomial (get-in models [key :family]))
                         2
                         (count (get-in models [key :alphabet])))]]
           [key (into {}
                      (for [status statuses]
                        [status (or (informative-concentrations key status)
                                    (uniform-concentrations n))]))]))
   :admitted-run-ids #{}})

(defn- log-multinomial-coefficient [counts]
  (- (bmr/log-gamma (inc (reduce + counts)))
     (reduce + 0.0 (map #(bmr/log-gamma (inc %)) counts))))

(defn- log-predictive [concentrations {:keys [family counts]}]
  (case family
    :binomial
    (+ (log-multinomial-coefficient counts)
       (- (bmr/log-multivariate-beta (mapv + concentrations counts))
          (bmr/log-multivariate-beta concentrations)))

    :multinomial
    (+ (log-multinomial-coefficient counts)
       (- (bmr/log-multivariate-beta (mapv + concentrations counts))
          (bmr/log-multivariate-beta concentrations)))

    :categorical
    (let [i (first (keep-indexed #(when (pos? %2) %1) counts))]
      (Math/log (/ (double (nth concentrations i))
                   (double (reduce + concentrations)))))))

(defn status-log-likelihoods
  "Return the integrated posterior-predictive log likelihood by status.

   EXPOSED keys use exactly `outcome` and `log-predictive`, the same functions
   as `step`. A requested key whose exposure is absent, refused, or malformed
   contributes zero and is listed under :unused rather than being imputed."
  [state exposures keys]
  (let [outcomes (into {}
                       (keep (fn [key]
                               (when-let [o (outcome key (get exposures key))]
                                 [key o])))
                       keys)
        used (vec (filter #(contains? outcomes %) keys))
        unused (vec (remove #(contains? outcomes %) keys))]
    {:log-likelihoods
     (into {}
           (for [status (:statuses state)]
             [status
              (reduce + 0.0
                      (for [key used]
                        (log-predictive
                          (get-in state [:concentrations key status])
                          (get outcomes key))))]))
     :used used
     :unused unused}))

(defn- predicted-q [state]
  (let [ss (:statuses state)
        u (/ 1 (count ss))
        r (:rho state)]
    (into {} (for [s ss]
               [s (+ (* (- 1 r) (get-in state [:q s])) (* r u))]))))

(defn- log-sum-exp [xs]
  (let [m (apply max xs)]
    (+ m (Math/log (reduce + (map #(Math/exp (- % m)) xs))))))

(defn- finite? [x]
  (and (number? x) (Double/isFinite (double x))))

(defn step
  "Run one prediction/filter/fractional-count step.

   Returns {:state STATE :receipt RECEIPT}.  A duplicate or refused step keeps
   STATE identical."
  [state tick]
  (let [run-id (:run/id tick)]
    (cond
      (contains? (:admitted-run-ids state) run-id)
      {:state state :receipt {:status :duplicate :run/id run-id}}

      (nil? run-id)
      {:state state :receipt {:status :refused :reason :run-id-missing}}

      :else
      (try
        (let [q-prior (predicted-q state)
              exposures (:scan-exposures tick)
              outcomes (into {}
                             (keep (fn [key]
                                     (when-let [o (outcome key (get exposures key))]
                                       [key o])))
                             exposure-keys)
              used (vec (filter #(contains? outcomes %) exposure-keys))
              absent (vec (remove #(contains? outcomes %) exposure-keys))]
          (if (empty? used)
            {:state (-> state
                        (assoc :q q-prior)
                        (update :admitted-run-ids conj run-id))
             :receipt {:status :no-observed-channel
                       :run/id run-id :q-prior q-prior :q q-prior
                       :log-evidence 0.0 :channels-used []
                       :channels-absent absent}}
            (let [log-joint
                  (into {}
                        (for [s (:statuses state)]
                          [s (+ (Math/log (double (get q-prior s)))
                                (reduce + 0.0
                                        (for [key used]
                                          (log-predictive
                                            (get-in state [:concentrations key s])
                                            (get outcomes key)))))]))
                  log-z (log-sum-exp (vals log-joint))]
              (if-not (finite? log-z)
                {:state state
                 :receipt {:status :refused :reason :invalid-normaliser
                           :run/id run-id}}
                (let [q (into {} (map (fn [[s x]] [s (Math/exp (- x log-z))])
                                      log-joint))
                      next-state
                      (reduce
                        (fn [st key]
                          (let [counts (:counts (get outcomes key))]
                            (reduce
                              (fn [st* s]
                                (update-in st* [:concentrations key s]
                                           #(mapv + % (mapv (fn [n] (* (get q s) n))
                                                           counts))))
                              st (:statuses state))))
                        (-> state
                            (assoc :q q)
                            (update :admitted-run-ids conj run-id)
                            (update :channel-ticks
                                    (fn [ticks]
                                      (reduce (fn [m key]
                                                (update m key (fnil inc 0)))
                                              (merge zero-channel-ticks ticks)
                                              used))))
                        used)]
                  {:state next-state
                   :receipt {:run/id run-id :q-prior q-prior :q q
                             :log-evidence log-z
                             :channels-used used :channels-absent absent}})))))
        (catch Exception e
          {:state state
           :receipt {:status :refused :reason :scan-learning-unavailable
                     :run/id run-id
                     :error {:class (.getName (class e))
                             :message (ex-message e)}}})))))

(defn fold
  "Replay RECORDS in their given order from `prior-state`.

   Pre-carrier records and bootstrap records without `:scan-exposures` are
   skipped rather than converted to negative evidence.  Records that reach
   `step` retain every receipt, including duplicates and refusals."
  [records]
  (reduce-kv
    (fn [{:keys [state receipts admitted skipped]} index record]
      (let [run-id (:run/id record)]
        (cond
          (not (map? (:scan-exposures record)))
          {:state state :receipts receipts :admitted admitted
           :skipped (conj skipped {:index index :run/id run-id
                                   :reason :no-scan-exposures})}

          (nil? run-id)
          {:state state :receipts receipts :admitted admitted
           :skipped (conj skipped {:index index :run/id nil
                                   :reason :run-id-missing})}

          :else
          (let [result (step state {:run/id run-id
                                    :scan-exposures (:scan-exposures record)})
                next-state (:state result)
                admitted? (> (count (:admitted-run-ids next-state))
                             (count (:admitted-run-ids state)))]
            {:state next-state
             :receipts (conj receipts (:receipt result))
             :admitted (if admitted? (inc admitted) admitted)
             :skipped skipped}))))
    {:state (prior-state) :receipts [] :admitted 0 :skipped []}
    (vec records)))

(def carrier-epoch
  "No trace file dated before this can carry :scan-exposures: the first
   carrier (futon2 3d9418e3e) landed 2026-09-28 23:18Z, and that day's live
   file holds only the accumulation bootstrap. Recovery reads from here on,
   not the whole corpus (651 MB on 2026-09-29), which a live tick must not
   parse in the serving JVM."
  "2026-09-28")

(defn fold-trace-dir
  "Strictly read and deterministically replay every trace record under DIR
   dated on or after `carrier-epoch`. Strict-reader absences/refusals pass
   through unchanged."
  [dir]
  (let [history (trace/read-history-strict-since carrier-epoch :dir dir)]
    (if (= :ok (:status history))
      (fold (:records history))
      history)))
