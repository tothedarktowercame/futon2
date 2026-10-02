(ns futon2.aif.meta-preference-authority
  "Pure reader for the operator-authorized META provisional prior. This emits
   preference and normalization inputs only, never candidate predictions."
  (:require [clojure.edn :as edn])
  (:import [java.security MessageDigest]))

(def schema :meta/outer-provisional-prior-v1)
(def vocabulary
  [:closure :grounded-progress :abstention-or-failure
   :elapsed-budget-fraction :token-budget-fraction])

(defn- sha256 [^bytes bytes]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn- pin? [x]
  (and (map? x) (string? (:path x)) (string? (:sha256 x))
       (boolean (re-matches #"[0-9a-f]{64}" (:sha256 x)))))

(defn- finite? [x]
  (and (number? x) (Double/isFinite (double x))))

(defn- refusal [reason details]
  {:schema :wm/meta-preference-inputs-v1 :status :refused
   :reason reason :details details})

(defn- declaration-errors [declaration]
  (let [channels (:channels declaration)
        preferences (map :preference channels)]
    (cond-> []
      (not= schema (:schema declaration)) (conj :schema-invalid)
      (not= 1 (:version declaration)) (conj :version-invalid)
      (not= :operator-authorized-provisional-prior (:status declaration))
      (conj :authority-status-invalid)
      (not= :declared-prior-not-empirical-calibration
            (get-in declaration [:authority :epistemic-status]))
      (conj :empirical-calibration-claim-forbidden)
      (not= :none (get-in declaration [:update-rule :current-calibration-claim]))
      (conj :empirical-calibration-claim-forbidden)
      (not= :registered-run-evidence-only
            (get-in declaration [:update-rule :allowed-source]))
      (conj :update-source-invalid)
      (not= vocabulary (:outcome-vocabulary declaration))
      (conj :outcome-vocabulary-invalid)
      (not= vocabulary (mapv :outcome channels))
      (conj :channel-order-or-coverage-invalid)
      (some #(not (finite? (:mean %))) preferences)
      (conj :preference-mean-invalid)
      (some #(not (and (finite? (:variance %)) (pos? (double (:variance %)))))
            preferences)
      (conj :preference-variance-invalid)
      (or (some #(not (and (finite? (:weight %)) (pos? (double (:weight %)))))
                preferences)
          (not= 1 (count (set (map :weight preferences)))))
      (conj :preference-weights-not-equal-positive)
      (not= [:candidate :resource-envelope :time-budget-ms]
            (get-in channels [3 :normalization :denominator]))
      (conj :elapsed-envelope-normalization-invalid)
      (not= [:candidate :resource-envelope :token-budget]
            (get-in channels [4 :normalization :denominator]))
      (conj :token-envelope-normalization-invalid))))

(defn read-authority
  [{:keys [source-bytes expected-source-pin]}]
  (cond
    (not (bytes? source-bytes))
    (refusal :preference-authority-bytes-missing {})
    (not (pin? expected-source-pin))
    (refusal :preference-authority-unpinned
             {:expected-source-pin expected-source-pin})
    (not= (:sha256 expected-source-pin) (sha256 source-bytes))
    (refusal :preference-authority-source-drift
             {:expected (:sha256 expected-source-pin)
              :actual (sha256 source-bytes)})
    :else
    (try
      (let [declaration (edn/read-string (String. ^bytes source-bytes "UTF-8"))
            errors (declaration-errors declaration)
            preferences (mapv :preference (:channels declaration))]
        (if (seq errors)
          (refusal :preference-authority-invalid {:errors errors})
          {:schema :wm/meta-preference-inputs-v1
           :status :verified
           :authority-source expected-source-pin
           :epistemic-status :declared-prior-not-empirical-calibration
           :outcome-vocabulary vocabulary
           :preference-means (mapv :mean preferences)
           :preference-variances (mapv :variance preferences)
           :weights (mapv :weight preferences)
           :excluded-outcomes (:excluded-outcomes declaration)
           :update-rule (:update-rule declaration)}))
      (catch Throwable t
        (refusal :preference-authority-unreadable {:message (ex-message t)})))))

(defn- fraction [raw budget]
  (when (and (integer? raw) (not (neg? raw)) (pos-int? budget))
    (min 1.0 (max 0.0 (/ (double raw) (double budget))))))

(defn normalize-observation
  "Normalize canonical observed outcomes. Time and tokens require the same
   candidate's explicit positive resource envelope; missing values refuse."
  [authority {:keys [resource-envelope observation]}]
  (cond
    (not= :verified (:status authority))
    (refusal :preference-authority-not-verified {})
    (not (and (pos-int? (:time-budget-ms resource-envelope))
              (pos-int? (:token-budget resource-envelope))))
    (refusal :candidate-resource-envelope-required
             {:resource-envelope resource-envelope})
    :else
    (let [outcome (get-in observation [:run-output :outcome])
          terminal (get-in observation [:terminal-outcome :class])
          elapsed (fraction (get-in observation [:registered-run/timing :wall-clock-ms])
                            (:time-budget-ms resource-envelope))
          tokens (fraction (get-in observation [:registered-run/model-usage :total-tokens])
                           (:token-budget resource-envelope))]
      (if (or (nil? elapsed) (nil? tokens))
        (refusal :normalization-input-missing-or-invalid
                 {:elapsed-ms (get-in observation [:registered-run/timing :wall-clock-ms])
                  :token-use (get-in observation [:registered-run/model-usage :total-tokens])})
        {:schema :wm/meta-normalized-outcome-v1
         :status :normalized
         :authority-source (:authority-source authority)
         :outcome-vocabulary vocabulary
         :values [(if (#{:grounded-change :already-satisfied} outcome) 1.0 0.0)
                  (if (= :grounded-progress outcome) 1.0 0.0)
                  (if (= :abstention-or-failure terminal) 1.0 0.0)
                  elapsed tokens]}))))
