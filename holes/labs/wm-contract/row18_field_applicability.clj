(ns row18-field-applicability
  "Read-only applicability check for the fixed row-18 lead-audit trace form.
   Decimal encodings are compared as BigDecimal rationals. This does not turn
   floating residual convergence into exact-real root equality."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.habit-prior :as habit]
            [futon2.aif.policy-precision :as precision])
  (:import [java.io PushbackReader]
           [java.security MessageDigest]))

(defn- refuse! [reason data]
  (throw (ex-info (name reason) (assoc data :refusal reason))))

(defn- sha256 [bytes]
  (apply str (map #(format "%02x" (bit-and (int %) 0xff))
                  (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn- read-first [path]
  (with-open [r (PushbackReader. (io/reader path))]
    (edn/read {:eof ::eof :default tagged-literal} r)))

(defn- decimal-rational [x]
  (when-not (and (number? x) (Double/isFinite (double x)))
    (refuse! :nonfinite-numeric-encoding {:value x}))
  (bigdec (str x)))

(defn criterion [c lo hi]
  (let [c (decimal-rational c)
        lo (decimal-rational lo)
        hi (decimal-rational hi)
        r (- hi lo)
        threshold (/ (* 3M r) 2M)]
    (when (neg? r)
      (refuse! :inverted-g-range {:lo lo :hi hi}))
    {:c c :lo lo :hi hi :R r :three-R-over-two threshold
     :sufficient? (> c threshold)
     :failure-means :uniqueness-unknown}))

(defn inspect-field [record]
  (let [ranked (:ranked-actions record)
        envelope (:f-pi-by-candidate-id record)
        by-id (:by-candidate-id envelope)
        solve (get-in record [:policy-precision-state :solve])]
    (when-not (= :present (:status envelope))
      (refuse! :f-pi-authority-absent {:status (:status envelope)}))
    (when-not (seq ranked)
      (refuse! :ranked-support-absent {}))
    (when-not (map? by-id)
      (refuse! :f-pi-map-absent {}))
    (when-not (number? (:beta-prior solve))
      (refuse! :beta-prior-authority-absent {}))
    (let [aligned (precision/align-f-pi-and-g
                   by-id ranked #(habit/policy-key (:action %))
                   :controller-score)
          by-identity (group-by #(habit/policy-key (:action %)) ranked)
          entries (mapv (fn [id]
                          (first (get by-identity
                                      (:candidate-identity (get by-id id)))))
                        (:candidate-ids aligned))]
      (when (zero? (:present-count aligned))
        (refuse! :aligned-support-empty {}))
      (when-not (= (:present-count aligned) (count entries))
        (refuse! :support-order-mismatch {:aligned (:present-count aligned)
                                          :entries (count entries)}))
      (doseq [[id entry] (map vector (:candidate-ids aligned) entries)]
        (when-not entry
          (refuse! :candidate-identity-unresolved {:candidate-id id}))
        (when-not (and (number? (:habit-prior-bias entry))
                       (:habit-prior-source entry))
          (refuse! :habit-authority-absent {:candidate-id id})))
      (let [g (:g-values aligned)
            lo (apply min g)
            hi (apply max g)
            base (:beta-prior solve)
            adjusted (* 2M (decimal-rational base))]
        {:status :checked
         :candidate-count (:present-count aligned)
         :absent-count (:absent-count aligned)
         :support-order (:candidate-ids aligned)
         :field-authority {:G :ranked-action-controller-score
                           :F-pi :previous-prediction-scored-at-current-observation
                           :habit :sourced-ranked-candidate-log-prior
                           :beta-prior :policy-precision-solve}
         :habit-placement {:production (:log-prior-placement solve)
                           :theorem-required :both
                           :applicable? (= :both (:log-prior-placement solve))}
         :base (criterion base lo hi)
         :adjusted-m-half (criterion adjusted lo hi)
         :floating-solve (select-keys solve
                                      [:solver :beta-posterior :gamma
                                       :fixed-point-residual :converged?
                                       :bracketed? :bracket-width :iterations])
         :numerical-certificate
         {:status :residual-only
          :exact-root? false
          :reason :floating-exp-and-residual-have-no-outward-rounded-real-interval}}))))

(defn commissioned-controls []
  (let [base {:ranked-actions [{:action {:type :no-op}
                                :controller-score 1.0
                                :habit-prior-bias 0.0
                                :habit-prior-source :test}]
              :f-pi-by-candidate-id
              {:status :present
               :by-candidate-id
               {"rank/1" {:candidate-identity [:no-op [:unscoped nil]]
                           :status :present :value 0.0}}}
              :policy-precision-state {:solve {:beta-prior 1.0}}}
        refusal (fn [x]
                  (try (inspect-field x) :failed-to-refuse
                       (catch clojure.lang.ExceptionInfo e
                         (:refusal (ex-data e)))))]
    {:missing-f-pi (refusal (assoc-in base [:f-pi-by-candidate-id :status] :absent))
     :missing-prior (refusal (assoc-in base [:policy-precision-state :solve] {}))
     :missing-habit (refusal (update-in base [:ranked-actions 0]
                                       dissoc :habit-prior-source))}))

(defn -main [& [trace-path audit-input-path]]
  (when-not (and trace-path audit-input-path)
    (refuse! :usage {:required ["TRACE" "AUDIT-INPUT"]}))
  (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file trace-path)))
        audit (edn/read-string (slurp audit-input-path))
        expected (get-in audit [:source :sha256])
        actual (sha256 bytes)]
    (when-not (= expected actual)
      (refuse! :source-pin-mismatch {:expected expected :actual actual}))
    (prn {:schema :wm/row18-field-applicability-v1
          :source {:path trace-path :sha256 actual :edn-form-index 0}
          :field (inspect-field (read-first trace-path))
          :controls (commissioned-controls)})))
