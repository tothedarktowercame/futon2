(ns futon2.aif.scan-model
  "Prototype item 6 (b) generative model for two raw-scan coverage channels.

   For a single declared entity, the latent state is its seven-way `:mu-pre`
   status belief.  Conditional on status s, covered support and attack claims
   are independent Binomial observations with rates theta[k,s], and each rate
   has the caller's authorised Beta(alpha,beta) parent prior. `learn` applies
   the standard uncertain-state fractional-count approximation: a += mu(s)*x
   and b += mu(s)*(n-x). Typed-absent and zero-exposure channels add no trial.

   `score` records two reductions fixed independently of the observations. The
   tied reduction shares one theta across statuses and uses the closed-form
   Beta evidence under that same fractional-count approximation. The hand-set
   reduction compares with supplied per-status point predictions when every
   value is in [0,1], otherwise recording :hand-set-not-comparable.

   Sign convention: delta-F = log evidence(full) - log evidence(reduced), and
   a reduction is favoured at delta-F <= -3. This prototype is recorded only;
   it never changes belief updating or selection."
  (:require [clojure.string :as str]
            [futon2.aif.belief :as belief]
            [futon2.aif.bmr :as bmr]))

(def schema :wm/scan-model-v1)
(def label
  "prototype: item 6 (b) first slice, two coverage channels, one declared entity's mu-pre; recorded, not applied")
(def ^:private channels [:support :attack])
(def ^:private statuses (vec (sort belief/status-set)))

(defn- valid-prior? [{:keys [alpha beta authority]}]
  (and (or (integer? alpha) (ratio? alpha)) (pos? alpha)
       (or (integer? beta) (ratio? beta)) (pos? beta)
       (string? authority) (not (str/blank? authority))))

(defn- log-beta [a b]
  (bmr/log-multivariate-beta [a b]))

(defn- valid-mu? [mu]
  (and (= (set statuses) (set (keys mu)))
       (every? #(and (number? %) (not (neg? %))) (vals mu))
       (< (Math/abs (- 1.0 (double (reduce + (vals mu))))) 1.0e-9)))

(defn- exposure? [x]
  (and (map? x) (integer? (:covered x)) (integer? (:claims x))
       (pos? (:claims x)) (<= 0 (:covered x) (:claims x))))

(defn learn
  "Learn status-indexed Beta posterior counts from trace RECORDS for ENTITY-ID.
   Invalid priors return the observation-rates-compatible typed refusal."
  [records entity-id prior]
  (if-not (valid-prior? prior)
    {:status :missing :kind :invalid-prior :prior prior}
    (let [initial (into {}
                        (for [k channels]
                          [k (into {} (for [s statuses]
                                        [s {:alpha (:alpha prior)
                                            :beta (:beta prior)
                                            :success 0 :failure 0}]))]))
          result
          (reduce-kv
           (fn [{:keys [posterior excluded]} i record]
             (let [mu (get-in record [:mu-pre entity-id])]
               (if-not (valid-mu? mu)
                 {:posterior posterior
                  :excluded (conj excluded {:record i :reason :mu-pre-unavailable})}
                 (reduce
                  (fn [acc k]
                    (let [x (get-in record [:scan-exposures k])]
                      (cond
                        (and (map? x) (= :absent (:status x)))
                        (update acc :excluded conj {:record i :channel k
                                                    :reason (:reason x)})

                        (not (exposure? x))
                        (update acc :excluded conj {:record i :channel k
                                                    :reason :scan-exposure-malformed})

                        :else
                        (reduce
                         (fn [a s]
                           (let [w (get mu s)
                                 yes (* w (:covered x))
                                 no (* w (- (:claims x) (:covered x)))]
                             (-> a
                                 (update-in [:posterior k s :alpha] + yes)
                                 (update-in [:posterior k s :beta] + no)
                                 (update-in [:posterior k s :success] + yes)
                                 (update-in [:posterior k s :failure] + no))))
                         acc statuses))))
                  {:posterior posterior :excluded excluded}
                  channels))))
           {:posterior initial :excluded []}
           (vec records))]
      {:schema schema :entity-id entity-id :prior prior
       :posterior (:posterior result) :excluded (:excluded result)})))

(defn- evidence [cell prior]
  (- (log-beta (:alpha cell) (:beta cell))
     (log-beta (:alpha prior) (:beta prior))))

(defn- comparable-hand-set? [row]
  (and (= (set statuses) (set (keys row)))
       (every? #(and (number? %) (<= 0 % 1)) (vals row))))

(defn- point-term [theta success failure]
  (cond
    (and (zero? theta) (pos? success)) ::impossible
    (and (= 1 theta) (pos? failure)) ::impossible
    :else (+ (if (zero? success) 0.0 (* success (Math/log (double theta))))
             (if (zero? failure) 0.0 (* failure (Math/log (- 1.0 (double theta))))))))

(defn score
  "Score tied-status and supplied hand-set point reductions of a learned result."
  [learned prior hand-set]
  (if-not (valid-prior? prior)
    {:status :missing :kind :invalid-prior :prior prior}
    (let [posterior (:posterior learned)
          scored
          (into {}
                (for [k channels
                      :let [cells (get posterior k)
                            full (reduce + 0.0 (map #(evidence (get cells %) prior) statuses))
                            success (reduce + 0 (map #(get-in cells [% :success]) statuses))
                            failure (reduce + 0 (map #(get-in cells [% :failure]) statuses))
                            tied (- (log-beta (+ (:alpha prior) success)
                                                (+ (:beta prior) failure))
                                    (log-beta (:alpha prior) (:beta prior)))
                            row (get hand-set k)
                            terms (when (comparable-hand-set? row)
                                    (mapv #(point-term (get row %)
                                                       (get-in cells [% :success])
                                                       (get-in cells [% :failure])) statuses))]]
                  [k {:tied {:delta-f (- full tied)
                             :log-evidence-full full
                             :log-evidence-reduced tied}
                      :hand-set (cond
                                  (not (comparable-hand-set? row))
                                  {:status :hand-set-not-comparable}

                                  (some #{::impossible} terms)
                                  {:impossible-under-reduced true
                                   :log-evidence-full full}

                                  :else
                                  (let [reduced (reduce + 0.0 terms)]
                                    {:delta-f (- full reduced)
                                     :log-evidence-full full
                                     :log-evidence-reduced reduced}))}]))]
      {:schema schema :label label :entity-id (:entity-id learned)
       :prior prior :threshold bmr/acceptance-threshold :applied false
       :reductions scored :excluded (:excluded learned)})))
