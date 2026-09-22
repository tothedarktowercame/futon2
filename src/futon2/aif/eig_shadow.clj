(ns futon2.aif.eig-shadow
  "Default-off, record-only separation of model-relative EIG from held-out calibration."
  (:require [futon2.aif.a4a :as a4a]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.epistemic-value :as eig]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(defn- refuse! [kind data]
  (throw (ex-info "EIG shadow refused" (assoc data :eig-shadow/refusal kind))))

(defn- entropy [distribution]
  ;; The canonical EIG kernel performs the distribution validation.
  (eig/kl-divergence distribution distribution)
  (- (reduce-kv (fn [h _ p]
                  (let [p (double p)]
                    (if (zero? p) h (+ h (* p (Math/log p))))))
                0.0 distribution)))

(defn- model-layer [model]
  (let [prior (:prior model)
        observations (:predicted-observations model)
        posteriors (:posteriors model)
        prior-h (entropy prior)
        expected-h (reduce-kv
                    (fn [total observation probability]
                      (let [posterior (get posteriors observation)]
                        (when-not posterior
                          (refuse! :missing-simulated-posterior
                                   {:observation observation}))
                        (+ total (* (double probability) (entropy posterior)))))
                    0.0 observations)
        information-gain (eig/expected-information-gain model)]
    {:status :model-relative
     :prior-entropy prior-h
     :expected-posterior-entropy expected-h
     :expected-information-gain information-gain
     :entropy-reduction (- prior-h expected-h)
     :consistency-error (Math/abs (- information-gain (- prior-h expected-h)))
     :degeneracy-reasons (cond-> []
                           (< (Math/abs information-gain) 1.0e-12)
                           (conj :zero-expected-information-gain))}))

(defn- empirical-layer [held-out]
  (if-not held-out
    {:status :held :reason :held-out-evidence-unavailable}
    (let [{:keys [predicted-probability realised prior posterior evidence/id]}
          held-out
          p (double predicted-probability)]
      (when-not (and (<= 0.0 p 1.0) (boolean? realised)
                     (string? id) (seq id))
        (refuse! :held-out-evidence-invalid {}))
      (let [q (if realised p (- 1.0 p))
            y (if realised 1.0 0.0)]
        {:status :observed
         :evidence/id id
         :log-loss (if (zero? q) ##Inf (- (Math/log q)))
         :brier (* (- p y) (- p y))
         :predicted-probability p
         :realised realised
         :prior-entropy (entropy prior)
         :posterior-entropy (entropy posterior)
         :realised-information-gain (- (entropy prior) (entropy posterior))}))))

(defn- shared-update-layer [state observation]
  (let [hypothetical (a4a/hypothetical-posterior state observation)
        observed (a4a/observed-posterior state observation)
        hypothetical-receipt (last (:posterior-update-receipts hypothetical))
        observed-receipt (last (:posterior-update-receipts observed))]
    (when-not (= (:concentrations hypothetical) (:concentrations observed))
      (refuse! :shared-updater-state-divergence {}))
    {:status :shared
     :updater-id a4a/updater-id
     :posterior-sha256 (identity/digest (:concentrations hypothetical))
     :hypothetical-receipt hypothetical-receipt
     :observed-receipt observed-receipt}))

(defn collect
  "Create a record-only shadow packet. Selection must be byte-value identical
  across the off-mode replay; the packet never supplies a controller score."
  [{:keys [model model-id model-source-sha256 selection-before selection-after
           held-out posterior-state observation]}]
  (when-not (= selection-before selection-after)
    (refuse! :off-replay-selection-drift {}))
  (when-not (and (string? model-id) (seq model-id)
                 (string? model-source-sha256)
                 (re-matches #"[0-9a-f]{64}" model-source-sha256))
    (refuse! :model-pin-invalid {}))
  (let [shared-update (shared-update-layer posterior-state observation)
        packet {:schema :wm/eig-shadow-packet-v1
                :mode :record-only-default-off
                :selection-replay {:status :byte-value-identical
                                   :sha256 (identity/digest selection-before)}
                :model {:id model-id :source-sha256 model-source-sha256
                        :updater-id (:updater-id shared-update)}
                :shared-update shared-update
                :model-relative (model-layer model)
                :empirical (empirical-layer held-out)
                :controller-effect :none}]
    (assoc packet :packet-sha256 (identity/digest packet))))
