(ns futon2.aif.attempt-learning
  "Attempt-grain endpoint admission. No production model consumes these counts."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.d-predecessor-task-authority :as task]
            [futon2.aif.learning-trial :as trial]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)
(def contract-resource "wm/attempt-learning-contract.edn")
(def contract-v2-resource "wm/attempt-learning-contract-v2.edn")
(def contract-v3-resource "wm/attempt-learning-contract-v3.edn")
(defn declared-contract [] (edn/read-string (slurp (io/resource contract-resource))))
(defn declared-contract-v2 []
  "PROOF-wm-works ⟨1⟩4: the v2 contract authorising production consumption.
   Joe's signature on the proof plan is the authorisation (cited in the
   resource). The v1 resource stays untouched and still reads old events."
  (edn/read-string (slurp (io/resource contract-v2-resource))))
(defn declared-contract-v3
  "Decision sheet 2026-10-26 item 1, option (a), taken by Joe the same day:
   v2 plus the pattern domain. Mission-domain consumption is v2's, unchanged;
   pattern-domain rows (retained :pattern-comparison) update the occurrence's
   own pattern key. The resource cites the signature."
  []
  (edn/read-string (slurp (io/resource contract-v3-resource))))

(defn- supported-contract? [contract]
  ;; PROOF-wm-works ⟨1⟩4: two supported shapes. v2
  ;; (:mode :production-consumption) is the live contract; v1
  ;; (:mode :record-only) remains recognised so historical events read.
  (contains?
   #{;; v3: v2 plus pattern-domain consumption (decision sheet 2026-10-26 item 1 (a))
     {:schema :wm/attempt-learning-contract-v3 :authority :declared
      :mode :production-consumption
      :trial-grain :selected-cascade-effect-attempt
      :occurrence-schema :wm/action-transition-occurrence-v2
      :observation-schema :wm/d-task-token-observations-v2
      :placement :post-build-artifact-revision
      :selection-condition :effect-absent-and-predicted-positive}
     ;; v2: production consumption authorised by the proof plan's signature
     {:schema :wm/attempt-learning-contract-v2 :authority :declared
      :mode :production-consumption
      :trial-grain :selected-cascade-effect-attempt
      :occurrence-schema :wm/action-transition-occurrence-v2
      :observation-schema :wm/d-task-token-observations-v2
      :placement :post-build-artifact-revision
      :selection-condition :effect-absent-and-predicted-positive}
     ;; v1: the record-only original, for reading old events
     {:schema :wm/attempt-learning-contract-v1 :authority :declared :mode :record-only
      :trial-grain :selected-cascade-effect-attempt
      :occurrence-schema :wm/action-transition-occurrence-v2
      :observation-schema :wm/d-task-token-observations-v2
      :placement :post-build-artifact-revision
      :selection-condition :effect-absent-and-predicted-positive}}
   (select-keys contract [:schema :authority :mode :trial-grain :occurrence-schema
                          :observation-schema :placement :selection-condition])))

(defn receipt
  "Verify signed endpoints using the existing execution authority and supplied
   read-job port. Admit only v2 occurrences with the frozen selected action.
   counted? stays false until the append-only ledger acknowledges the row."
  [{:keys [comparison occurrence route source-record expected read-job contract]
    :as input}]
  (let [contract (or contract (declared-contract))
        base (trial/receipt input)
        prediction (:prediction comparison)
        action (:action prediction)
        q0 (:initial-belief prediction)
        horizon (:horizon prediction)
        precedence (trial/precedence-analysis (:precedence action))
        model-valid? (and (= :frozen (:status prediction)) (seq q0)
                          (model/normalized-exact? q0) (every? set? (keys q0))
                          (integer? horizon) (pos? horizon)
                          (= :present (:status precedence)))
        q (when model-valid? (model/rollout (constantly (:step precedence)) q0 horizon))
        prediction-valid? (and model-valid? (model/normalized-exact? q))
        signed (if (and source-record expected read-job)
                 (task/verify-observations-v2 source-record expected read-job)
                 {:status :refused :kind :observation-authority-unavailable})
        declarations (get-in source-record [:dispatch :declarations])
        admitted-rows (mapv
              (fn [row]
                (let [token (:effect row)
                      observed (get-in signed [:observations token :artifact-observation :observed])
                      observation (get-in signed [:observations token])
                      declarations (filter #(= (first token) (get-in % [:snapshot :target])) declarations)
                      wanted? (and (= 1 (count declarations))
                                   (contains? (set (get-in (first declarations) [:snapshot :want])) (second token)))
                      predicted (when prediction-valid?
                                  (reduce-kv (fn [p s mass] (+ p (if (contains? s token) mass 0))) 0 q))
                      original (first (filter #(= token (:token %)) (:tokens comparison)))
                      reason (cond
                               ;; These earlier held negatives remain held at the new grain.
                               (#{:unselected-target :effect-already-present :duplicate-replay
                                  :revised-meaning :observation-missing :illustrative-prior-invalid} (:reason row)) (:reason row)
                               (not (and (supported-contract? contract) (= contract (declared-contract)))) :attempt-contract-invalid
                               (not= :wm/action-transition-occurrence-v2 (:schema occurrence)) :occurrence-v2-required
                               (not= action (:action/value occurrence)) :selected-cascade-mismatch
                               (not= occurrence (get-in source-record [:dispatch :occurrence])) :occurrence-mismatch
                               (not= route (:route source-record)) :route-mismatch
                               (not prediction-valid?) :prediction-input-unavailable
                               (not (pos? predicted)) :effect-not-predicted
                               (not= predicted (:predicted original)) :prediction-mismatch
                               (not wanted?) :effect-not-declared-want
                               (not= :admitted (:status signed)) (or (:kind signed) :observation-not-admitted)
                               (not= occurrence (:occurrence signed)) :occurrence-mismatch
                               (not (boolean? observed)) :observation-missing
                               (not= observed (:observed original)) :observation-mismatch
                               (not= (:artifact-sha comparison)
                                     (get-in observation [:artifact-observation :artifact-sha])
                                     (get-in signed [:revision-pair :after])) :artifact-revision-mismatch
                               (not= (:meaning row) (:meaning observation)) :revised-meaning
                               :else nil)
                      key {:occurrence (select-keys occurrence [:action/id :action/value-sha256 :transition/id])
                           :effect token :grain (:trial-grain contract)}
                      family {:target (:target action) :cascade (:id action)
                              :patterns (mapv :id (:patterns precedence)) :effect token :route route}]
                  (assoc row
                         :status (if reason :held :admitted-at-attempt-grain) :reason reason :counted? false
                         :trial-grain (:trial-grain contract) :selected-cascade action
                         :calibration (:calibration contract) :causal-attribution :not-established
                         :placement {:status :declared :placement (:placement contract)
                                     :contract-sha256 (identity/digest contract)}
                         :performed-step {:status :not-claimed :grain :attempt}
                         :measurement-source (:measurement-source comparison)
                         :signed-observation observation :observation-verification signed
                         :attempt-beta
                         (if reason {:status :held :reason reason}
                           (let [{:keys [alpha beta]} (:prior base)
                                 a (+ alpha (if observed 1 0)) b (+ beta (if observed 0 1))]
                             {:scope :illustrative-single-attempt-summary
                              :prior {:alpha alpha :beta beta} :after {:alpha a :beta b}
                              :delivery-mean (/ a (+ a b)) :cumulative-posterior :not-computed
                              :production-consumption :none}))
                         :deduplication {:identity (identity/digest key) :inputs key}
                         :learning-family (identity/digest family)
                         :shadow (assoc (:shadow row) :scope :illustrative-per-firing-sensitivity-only
                                        :attempt-parameter-consumption :none))))
              (:trials base))
        ;; One cascade/effect trial even when several patterns mention it.
        ;; Legacy vectors retain their historical global effect sort. Co-apply
        ;; candidates retain declared unit order, choosing the first declaring
        ;; unit when an effect is repeated.
        rows (if (vector? (:step precedence))
               (vec (vals (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
                                (map (juxt :effect identity)) admitted-rows)))
               (second
                (reduce (fn [[seen out] row]
                          (if (contains? seen (:effect row))
                            [seen out]
                            [(conj seen (:effect row)) (conj out row)]))
                        [#{} []] admitted-rows)))]
    (cond->
     (assoc base :schema :wm/learning-trial-receipt-v2 :contract contract
            :contract-sha256 (identity/digest contract)
            :status :record-only :reason nil :trials rows)
      (= :held (:status precedence))
      (assoc :status :held :reason (:reason precedence)
             :precedence-finding (:finding precedence)))))

(def pattern-receipt-schema :wm/pattern-learning-trial-receipt-v1)
(def pattern-trial-schema :wm/pattern-learning-trial-v1)

(defn pattern-receipt
  "Contract v3 (decision sheet 2026-10-26 item 1 (a)): one trial per wanted
   pattern-done token of the retained :pattern-comparison
   (token-outcome/compare-supplied-outcomes), at the pattern-occurrence grain.

   A trial is admitted when the comparison's :observed is a boolean, that is,
   when the occurrence's authored completion check was executable at the after
   revision and futon2.aif.completion-check/evaluate answered. Both verdicts
   admit: :predicted-and-observed is a success, :predicted-not-observed a
   failure (Lean PatternObservationAdmission witnessed_false_reaches_failure_update).
   Every typed-missing row is held under the comparison's own reason
   (:completion-check-not-executable, :occurrence-not-in-fold, ...), so the 27
   prose checks of attempt-002 hold exactly as they did under v2.

   The parameter key is the occurrence's own pattern, (first occurrence),
   carried as :pattern-key so learning-trial-ledger/theta-key reads it instead
   of deriving a producer from the cascade precedence. The ledger holds a
   replay :duplicate-replay on the deduplication identity
   (occurrence ids + effect token + pattern grain). Nothing here reads or
   changes the mission-domain receipt."
  [{:keys [pattern-comparison occurrence route contract]}]
  (let [contract (or contract (declared-contract-v3))
        contract-ok? (and (supported-contract? contract)
                          (= :wm/attempt-learning-contract-v3 (:schema contract)))
        compared? (= :compared (:status pattern-comparison))
        artifact-sha (:artifact-sha pattern-comparison)
        grain (:pattern-trial-grain contract)
        trials
        (mapv
         (fn [{:keys [token predicted observed verdict reason measurement]}]
           (let [pattern-occurrence (nth token 2)
                 pattern-key (first pattern-occurrence)
                 hold (cond
                        (not contract-ok?) :attempt-contract-invalid
                        (not= :wm/action-transition-occurrence-v2 (:schema occurrence))
                        :occurrence-v2-required
                        (not (boolean? observed)) (or reason :observation-missing)
                        (not (and (number? predicted) (pos? predicted))) :effect-not-predicted
                        (not (contains? #{:predicted-and-observed :predicted-not-observed}
                                        verdict))
                        :verdict-not-admissible
                        (not= artifact-sha (get-in measurement [:revision :sha]))
                        :artifact-revision-mismatch
                        :else nil)
                 key {:occurrence (select-keys occurrence
                                              [:action/id :action/value-sha256 :transition/id])
                      :effect token :grain grain}
                 family {:target (first token) :pattern pattern-key
                         :occurrence pattern-occurrence :effect token :route route}]
             {:schema pattern-trial-schema
              :effect token
              :pattern-key pattern-key
              :pattern-occurrence pattern-occurrence
              :predicted predicted
              :after-observation (when (boolean? observed) observed)
              :verdict verdict
              :status (if hold :held :admitted-at-attempt-grain)
              :reason hold
              :counted? false
              :trial-grain grain
              :calibration (:pattern-calibration contract)
              :causal-attribution :not-established
              :placement {:status :declared :placement (:placement contract)
                          :contract-sha256 (identity/digest contract)}
              :authority-evidence (:authority-evidence measurement)
              :measurement-source (:source measurement)
              :deduplication {:identity (identity/digest key) :inputs key}
              :learning-family (identity/digest family)
              :meaning-sha256 (identity/digest
                               {:pattern pattern-key :effect token
                                :completion-check (get-in measurement [:source :completion-check])})}))
         (if compared? (:tokens pattern-comparison) []))
        reason (cond (not contract-ok?) :attempt-contract-invalid
                     (not compared?) (or (:reason pattern-comparison)
                                         :pattern-comparison-unavailable)
                     :else nil)]
    {:schema pattern-receipt-schema
     :contract contract
     :contract-sha256 (identity/digest contract)
     :status (if reason :held :record-only)
     :reason reason
     :trials trials}))
