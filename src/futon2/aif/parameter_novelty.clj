(ns futon2.aif.parameter-novelty
  "Record-only endpoint parameter information. No score or parameter is updated."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.attempt-learning :as attempt]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.load-identity :as load-identity])
  (:import [java.io PushbackReader StringReader]))

(load-identity/register! *ns* *file*)
(defn absent [reason] {:status :absent :reason reason})
(defn- positive-finite? [x] (and (number? x) (Double/isFinite (double x)) (pos? x)))
(defn- digamma [x]
  (loop [x (double x) correction 0.0]
    (if (< x 12.0) (recur (inc x) (- correction (/ 1.0 x)))
      (let [z (/ 1.0 (* x x))]
        (+ correction (Math/log x) (- (/ 0.5 x))
           (* z (+ (- (/ 1.0 12)) (* z (+ (/ 1.0 120) (* z (- (/ 1.0 252))))))))))))
(defn beta-information
  "Expected posterior/prior KL for ONE perfectly observed Bernoulli endpoint."
  [a b]
  (if-not (and (positive-finite? a) (positive-finite? b) (Double/isFinite (double (+ a b))))
    (absent :invalid-beta-prior)
    (let [n (+ a b) p (/ (double a) n) q (/ (double b) n)]
      {:status :computed :nats (max 0.0 (+ (* p (- (digamma (inc a)) (Math/log p)))
                                         (* q (- (digamma (inc b)) (Math/log q)))
                                         (- (digamma (inc n)))))})))

(defn read-ledger
  "Read one byte snapshot; no creation, locks, repair or writes. Partial EDN is held."
  [path]
  (try
    (if-not (.isFile (io/file path))
      (assoc (absent :ledger-not-found) :path path)
      (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file path)))
            revision (load-identity/sha256 bytes)]
        (with-open [r (PushbackReader. (StringReader. (String. bytes "UTF-8")))]
          (loop [rows []]
            (let [row (edn/read {:eof ::eof} r)]
              (cond
                (= ::eof row) {:status :present :path path :sha256 revision :records rows}
                (and (= :wm/attempt-learning-count-v1 (:schema row))
                     (= :record-only (:mode row)) (boolean? (:observed row))
                     (string? (:identity row)) (string? (:family row))
                     (= (:increment row) (if (:observed row) {:success 1 :failure 0} {:success 0 :failure 1}))
                     (not-any? #(= (:identity %) (:identity row)) rows)) (recur (conj rows row))
                :else (assoc (absent :invalid-ledger) :path path :sha256 revision)))))))
    (catch Exception e (assoc (absent :unreadable-ledger) :path path :error-class (.getName (class e))))))

(defn read-inputs
  "Snapshot the declared ledger once per tick; caller may inject a snapshot in tests."
  []
  (let [contract (attempt/declared-contract)]
    {:contract contract :ledger (read-ledger (:ledger contract))
     :prior (edn/read-string (slurp (io/resource "wm/learning-trial-prior.edn")))}))

(defn- endpoint-prior [inputs family model]
  (let [{:keys [ledger contract prior]} inputs
        rows (filter #(= (identity/digest family) (:family %)) (:records ledger))
        meaning (get-in model [:meanings (:effect family)])
        valid? (every? #(and (= contract (:contract %)) (some? meaning)
                            (= meaning (:meaning-sha256 %))) rows)]
    (cond
      (= :known-parameter (get-in model [:prior :kind]))
      (if (and (number? (get-in model [:prior :theta])) (<= 0 (get-in model [:prior :theta]) 1))
        {:status :present :kind :known-parameter :theta (get-in model [:prior :theta])}
        (absent :invalid-known-parameter))
      (not (#{:present :absent} (:status ledger))) (absent :ledger-unavailable)
      (and (= :absent (:status ledger)) (not= :ledger-not-found (:reason ledger))) (absent (:reason ledger))
      (not valid?) (absent :ledger-contract-or-meaning-mismatch)
      (not (and (= :illustrative (:authority prior)) (= :record-only (:mode prior))
                (positive-finite? (:alpha prior)) (positive-finite? (:beta prior)))) (absent :illustrative-prior-invalid)
      :else (let [success (count (filter :observed rows)) failure (- (count rows) success)]
              {:status :present :kind (if (seq rows) :illustrative-plus-learned-counts :illustrative)
               :source prior :grain-adaptation :illustrative-attempt-endpoint-only
               :counts {:success success :failure failure :identities (mapv :identity rows)}
               :alpha (+ (:alpha prior) success) :beta (+ (:beta prior) failure)}))))

(defn- binary-entropy [p]
  (- (reduce + 0.0 (for [x [p (- 1.0 p)] :when (pos? x)] (* x (Math/log x))))))

(defn- prior-mean [{:keys [kind theta alpha beta]}]
  (if (= :known-parameter kind) theta (/ (double alpha) (+ alpha beta))))

(defn- conditional-entropy
  "E_theta H(Bernoulli(theta)); computed directly, not H(p) minus the KL receipt."
  [{:keys [kind theta alpha beta]}]
  (if (= :known-parameter kind) (binary-entropy theta)
    (let [n (+ alpha beta) p (/ (double alpha) n) q (/ (double beta) n)]
      (- (digamma (inc n)) (* p (digamma (inc alpha))) (* q (digamma (inc beta)))))))

(defn theta-latent-terms
  "Terminal endpoint EFE, with theta marginalized in risk and conditioned in
   ambiguity. The supported model has fixed background state and independently
   declared endpoint channels. No coefficient, score mutation, or powerset walk."
  [certificate endpoints information]
  (let [q (get-in certificate [:consumed-g :Q :steps])
        terminal (into {} (filter (comp pos? val)) (:belief (last q)))
        c (:distribution (last (get-in certificate [:consumed-g :C :steps])))
        effects (set (map #(get-in % [:family :effect]) endpoints))]
    (cond
      (not= :computed (:status information)) (absent (:reason information))
      (not= 1 (count terminal)) (absent :non-deterministic-background-unsupported)
      (not (and (set? (:universe c)) (map? (:weights c))
                (set/subset? effects (:universe c)))) (absent :preference-member-unavailable)
      :else
      (let [base (set/difference (ffirst terminal) effects)
            probabilities (into {} (map (fn [e] [(get-in e [:family :effect]) (prior-mean (:prior e))])) endpoints)
            support-excluded? (some (fn [z]
                                      (and (= base (set/difference z effects))
                                           (every? (fn [[t p]] (if (contains? z t) (pos? p) (< p 1))) probabilities))) (:zeroed c))]
        (if support-excluded? (absent :predictive-support-excluded-by-C)
          (let [;; This reference outcome has positive predictive probability;
                ;; C has additive log weights outside its excluded outcomes.
                reference (into base (for [[t p] probabilities :when (= 1.0 (double p))] t))
                logc (model/member-log-probability c)
                cost (- (+ (logc reference)
                           (reduce + 0.0 (for [[t p] probabilities]
                                           (* (- p (if (contains? reference t) 1.0 0.0))
                                              (double (get-in c [:weights t] 0)))))))
                predictive-h (reduce + 0.0 (map binary-entropy (vals probabilities)))
                ambiguity (reduce + 0.0 (map (comp conditional-entropy :prior) endpoints))
                risk (- cost predictive-h)
                lhs (+ risk ambiguity) rhs (- cost (:nats information))
                residual (- lhs rhs) tolerance (* 1e-10 (max 1.0 (abs lhs) (abs rhs)))]
            (if-not (every? #(Double/isFinite (double %)) [cost predictive-h ambiguity risk lhs rhs])
              (absent :non-finite-accounting)
              {:status :computed :accounting :theta-latent-efe-v1 :scope :attempt-endpoint
               :tau (:tau (last q)) :multiplicity 1 :units :nats
               :predictive-distribution {:form :independent-endpoint-bernoulli
                                         :theta-treatment :marginalized :fixed-state base
                                         :probabilities probabilities}
               :predictive-entropy predictive-h :risk-marginal risk
               :ambiguity-conditional ambiguity :pragmatic-cost cost
               :information-gain (:nats information) :efe lhs
               :identity {:formula :risk-plus-conditional-ambiguity-equals-cost-minus-information
                          :lhs lhs :rhs rhs :residual residual :tolerance tolerance
                          :status (if (<= (abs residual) tolerance) :checked :failed)}})))))))

(defn policy-receipt
  "One policy, actual retained trajectory, and explicit prospective endpoint model.
   Models are keyed by the full action. No route/likelihood/factorization is inferred."
  [entry inputs]
  (let [a (:action entry) c (:certificate entry) model (get-in inputs [:models a])
        contract (:contract inputs) horizon (:horizon c)
        q0 (get-in c [:consumed-g :D]) terminal (get-in c [:consumed-g :Q :steps])
        q (:belief (last terminal))
        wants (set (keys (get-in c [:consumed-g :C :steps (dec (max 1 (count terminal))) :distribution :weights])))
        outputs (into #{} (mapcat :produces) (:precedence a))
        initial (into #{} (mapcat key) (filter (comp pos? val) q0))
        future (into #{} (mapcat key) (filter (comp pos? val) q))
        eligible (vec (sort-by pr-str (set/intersection outputs wants (set/difference future initial))))
        witness (:factorization model)
        reason (cond
                 (not= (select-keys contract [:schema :authority :mode :trial-grain :occurrence-schema :observation-schema :placement :selection-condition])
                       {:schema :wm/attempt-learning-contract-v1 :authority :declared :mode :record-only
                        :trial-grain :selected-cascade-effect-attempt :occurrence-schema :wm/action-transition-occurrence-v2
                        :observation-schema :wm/d-task-token-observations-v2 :placement :post-build-artifact-revision
                        :selection-condition :effect-absent-and-predicted-positive}) :observation-contract-unsupported
                 (not (and (pos-int? horizon) (model/normalized-exact? q0) (model/normalized-exact? q) (seq q0) (seq q) (= horizon (:tau (last terminal))))) :trajectory-unavailable
                 (nil? (:route model)) :route-unavailable
                 (not (and (= :wm/attempt-endpoint-parameter-model-v1 (:schema model))
                           (= :illustrative (:authority model)) (string? (:source model)) (seq (:source model)))) :parameter-model-unsupported
                 (nil? (:observation model)) :observation-model-missing
                 (not= {:schema :wm/perfect-attempt-endpoint-v1 :placement :post-build-artifact-revision}
                       (:observation model)) :observation-model-unsupported
                 (and (> (count eligible) 1)
                      (not (and (= :declared-independent (:status witness))
                                (string? (:source witness)) (seq (:source witness))
                                (= (set eligible) (set (:effects witness)))))) :factorization-unavailable
                 :else nil)
        endpoints (mapv (fn [effect]
                          (let [family {:target (:target a) :cascade (:id a) :patterns (mapv :id (:precedence a))
                                        :effect effect :route (or (:route model) (absent :route-unavailable))}
                                prior (endpoint-prior inputs family model)
                                information (cond reason (absent reason)
                                                  (not= :present (:status prior)) prior
                                                  (= :known-parameter (:kind prior)) {:status :computed :nats 0.0}
                                                  :else (beta-information (:alpha prior) (:beta prior)))]
                            {:family family :family-sha256 (identity/digest family) :prior prior
                             :expected-kl information})) eligible)
        information (cond reason (absent reason)
                          (some #(not= :computed (get-in % [:expected-kl :status])) endpoints)
                          (absent :endpoint-prior-unavailable)
                          :else {:status :computed :nats (reduce + 0.0 (map #(get-in % [:expected-kl :nats]) endpoints))})]
    {:schema :wm/parameter-novelty-v1 :mode :record-only :id a
     :focus {:status :no-focus-declared}
     :parameter-model {:kind :attempt-endpoint :authority (:authority model) :production-B-consumption :none
                       :binding (or (:source model) (absent :parameter-model-source-unavailable))}
     :trial-grain :selected-cascade-effect-attempt :units :nats :tau horizon :multiplicity 1
     :observation-contract {:resource attempt/contract-resource :value contract :sha256 (when contract (identity/digest contract))}
     :observation-model (or (:observation model) (absent :observation-model-missing))
     :prior-ledger (if (:ledger inputs) (dissoc (:ledger inputs) :records) (absent :ledger-unavailable))
     :factorization (if (> (count eligible) 1) (or witness (absent :factorization-unavailable))
                        {:status :not-required :reason :at-most-one-endpoint})
     :eligible-endpoints eligible :endpoints endpoints :expected-kl information
     :terms {:serving-G (:controller-score entry)
             :theta-latent (theta-latent-terms c endpoints information)
             :serving-risk (if (seq (:steps c)) (mapv #(select-keys % [:tau :risk :risk-status]) (:steps c)) (absent :risk-not-recorded))
             :serving-ambiguity (if (seq (:steps c)) (mapv #(select-keys % [:tau :ambiguity :ambiguity-status]) (:steps c)) (absent :ambiguity-not-recorded))
             :novelty information :novelty-consumed-in-G? false}}))
