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
(defn declared-contract [] (edn/read-string (slurp (io/resource contract-resource))))

(defn- supported-contract? [contract]
  (= (select-keys contract [:schema :authority :mode :trial-grain :occurrence-schema :observation-schema :placement :selection-condition])
     {:schema :wm/attempt-learning-contract-v1 :authority :declared :mode :record-only
      :trial-grain :selected-cascade-effect-attempt
      :occurrence-schema :wm/action-transition-occurrence-v2
      :observation-schema :wm/d-task-token-observations-v2
      :placement :post-build-artifact-revision
      :selection-condition :effect-absent-and-predicted-positive}))

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
        model-valid? (and (= :frozen (:status prediction)) (seq q0)
                          (model/normalized-exact? q0) (every? set? (keys q0))
                          (integer? horizon) (pos? horizon) (vector? (:precedence action)))
        q (when model-valid? (model/rollout (constantly (:precedence action)) q0 horizon))
        prediction-valid? (and model-valid? (model/normalized-exact? q))
        signed (if (and source-record expected read-job)
                 (task/verify-observations-v2 source-record expected read-job)
                 {:status :refused :kind :observation-authority-unavailable})
        declarations (get-in source-record [:dispatch :declarations])
        rows (mapv
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
                              :patterns (mapv :id (:precedence action)) :effect token :route route}]
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
              ;; One cascade/effect trial even when several patterns mention it.
              (vals (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
                          (map (juxt :effect identity)) (:trials base))))]
    (assoc base :schema :wm/learning-trial-receipt-v2 :contract contract
           :contract-sha256 (identity/digest contract)
           :status :record-only :reason nil :trials rows)))
