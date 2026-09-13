(ns row18-field-applicability
  "Read-only applicability check for the fixed row-18 lead-audit trace form.
   Decimal encodings are compared as BigDecimal rationals. This does not turn
   floating residual convergence into exact-real root equality."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.habit-prior :as habit]
            [futon2.aif.policy-precision :as precision])
  (:import [java.io PushbackReader StringReader]
           [java.nio ByteBuffer]
           [java.nio.charset CodingErrorAction StandardCharsets]
           [java.nio.file Files OpenOption]
           [java.security MessageDigest]))

(def audit-input-sha256
  "1818dd14eabe7274815e1298b7d59909b06f98d853db52a84ec1f3e229f20235")

(defn- refuse! [reason data]
  (throw (ex-info (name reason) (assoc data :refusal reason))))

(defn- sha256 [bytes]
  (apply str (map #(format "%02x" (bit-and (int %) 0xff))
                  (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn- strict-utf8 [bytes]
  (str (.decode (doto (.newDecoder StandardCharsets/UTF_8)
                  (.onMalformedInput CodingErrorAction/REPORT)
                  (.onUnmappableCharacter CodingErrorAction/REPORT))
                (ByteBuffer/wrap bytes))))

(defn- parse-first-buffer [bytes]
  (with-open [r (PushbackReader. (StringReader. (strict-utf8 bytes)))]
    (edn/read {:eof ::eof :default tagged-literal} r)))

(defn- read-pinned-first
  ([path expected] (read-pinned-first path expected nil))
  ([path expected after-read!]
   (let [file (.toPath (io/file path))
         bytes (Files/readAllBytes file)
         actual (sha256 bytes)]
     (when-not (= expected actual)
       (refuse! :source-pin-mismatch {:path path :expected expected :actual actual}))
     (when after-read! (after-read!))
     ;; Parsing is deliberately from BYTES, never a reopened path.
     (let [value (parse-first-buffer bytes)
           after (sha256 (Files/readAllBytes file))]
       (when-not (= actual after)
         (refuse! :source-mutated-after-read
                  {:path path :before actual :after after}))
       {:value value :sha256 actual :bytes bytes}))))

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

(defn- refusal-of [f]
  (try (f) :failed-to-refuse
       (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

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
        temp (Files/createTempFile "row18-mutated-after-read" ".edn"
                                   (make-array java.nio.file.attribute.FileAttribute 0))]
    (try
      (Files/write temp (.getBytes "{:v 1}" "UTF-8") (make-array OpenOption 0))
      (let [expected (sha256 (Files/readAllBytes temp))]
        {:missing-f-pi
         (refusal-of #(inspect-field
                       (assoc-in base [:f-pi-by-candidate-id :status] :absent)))
         :missing-prior
         (refusal-of #(inspect-field
                       (assoc-in base [:policy-precision-state :solve] {})))
         :missing-habit
         (refusal-of #(inspect-field
                       (update-in base [:ranked-actions 0]
                                  dissoc :habit-prior-source)))
         :mutation-after-read
         (refusal-of #(read-pinned-first
                       (str temp) expected
                       (fn [] (Files/write temp (.getBytes "{:v 2}" "UTF-8")
                                           (make-array OpenOption 0)))))} )
      (finally (Files/deleteIfExists temp)))))

(def expected-controls
  {:missing-f-pi :f-pi-authority-absent
   :missing-prior :beta-prior-authority-absent
   :missing-habit :habit-authority-absent
   :mutation-after-read :source-mutated-after-read})

(defn assert-controls! [expected]
  (let [actual (commissioned-controls)]
    (when-not (= expected actual)
      (refuse! :commissioned-control-mismatch
               {:expected expected :actual actual}))
    actual))

(defn -main [& [trace-path audit-input-path mode]]
  (when-not (and trace-path audit-input-path)
    (refuse! :usage {:required ["TRACE" "AUDIT-INPUT"]}))
  (let [audit-read (read-pinned-first audit-input-path audit-input-sha256)
        audit (:value audit-read)
        expected (get-in audit [:source :sha256])
        trace-read (read-pinned-first trace-path expected)
        actual (:sha256 trace-read)
        control-expectation (if (= mode "--negative-wrong-expectation")
                              (assoc expected-controls :missing-habit :wrong)
                              expected-controls)
        controls (assert-controls! control-expectation)]
    (prn {:schema :wm/row18-field-applicability-v1
          :source {:path trace-path :sha256 actual :edn-form-index 0}
          :audit-input {:path audit-input-path :sha256 (:sha256 audit-read)
                        :edn-form-index 0}
          :field (inspect-field (:value trace-read))
          :controls controls})))
