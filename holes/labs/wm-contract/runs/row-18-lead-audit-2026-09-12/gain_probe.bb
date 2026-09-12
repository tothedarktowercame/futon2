(ns gain-probe
  "Read-only controller sensitivity on the explicitly first retained trace form.
   No judge invocation, actuation, trip creation, or live reload."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.policy :as policy])
  (:import [java.io PushbackReader StringReader]
           [java.security MessageDigest]))

(defn sha256 [bytes]
  (apply str (map #(format "%02x" (bit-and (int %) 0xff))
                 (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn finite-number? [x]
  (and (number? x) (Double/isFinite (double x))))

(defn entropy [weights]
  (- (reduce + (map #(if (pos? %) (* % (Math/log %)) 0.0) weights))))

(defn probe [scores base-gain mode multiplier]
  (let [tau (policy/effective-temperature
             scores (* base-gain multiplier)
             {:tau-mode mode :variational-beta 0.7})
        weights (policy/softmax-weights scores tau)]
    {:multiplier multiplier :effective-gain (* base-gain multiplier)
     :tau tau :weights weights :entropy (entropy weights)
     :ranked-indices (vec (sort-by #(nth weights %) > (range (count weights))))}))

(let [[trace-path out-dir] *command-line-args*
      _ (when-not (and trace-path out-dir)
          (throw (ex-info "usage: bb -cp src gain_probe.bb TRACE OUTPUT-DIR" {})))
      ;; Hash exactly the bytes parsed; selecting ordinal zero is intentional,
      ;; not an assertion about every record in the daily file.
      raw-bytes (java.nio.file.Files/readAllBytes (.toPath (io/file trace-path)))
      record (with-open [reader (PushbackReader. (StringReader. (String. raw-bytes "UTF-8")))]
               (edn/read {:eof ::eof :default tagged-literal} reader))
      ranked (:ranked-actions record)
      scores (mapv :controller-score ranked)
      base-gain (get-in record [:selection-gain :selection-gain])
      _ (when-not (and (> (count scores) 1) (every? finite-number? scores)
                       (> (count (distinct scores)) 1)
                       (finite-number? base-gain) (pos? base-gain))
          (throw (ex-info "First form lacks a discriminating scored field and gain" {})))
      modes [:spread :selection-gain-only :variational-beta-gamma]
      arms (into {} (map (fn [mode]
                          [mode (mapv #(probe scores base-gain mode %)
                                      [1.0 0.75 0.5 0.25])]) modes))
      positive-modes-change?
      (every? (fn [mode]
                (let [[baseline _ half] (get arms mode)]
                  (and (> (:tau half) (:tau baseline))
                       (not= (:weights half) (:weights baseline))
                       (> (:entropy half) (:entropy baseline)))))
              [:spread :selection-gain-only])
      variational-mode-ignores-gain?
      (apply = (map #(select-keys % [:tau :weights :entropy :ranked-indices])
                    (:variational-beta-gamma arms)))
      floor-control
      (mapv #(policy/effective-temperature scores (* 0.005 %)
                                          {:tau-mode :selection-gain-only})
            [1.0 0.5])
      floor-can-hide-modulation? (apply = floor-control)
      _ (when-not (and positive-modes-change? variational-mode-ignores-gain?
                       floor-can-hide-modulation?)
          (throw (ex-info "Controller sensitivity disagrees with inspected laws"
                          {:positive-modes-change? positive-modes-change?
                           :variational-mode-ignores-gain? variational-mode-ignores-gain?
                           :floor-can-hide-modulation? floor-can-hide-modulation?})))
      input {:source {:path trace-path :sha256 (sha256 raw-bytes)
                      :byte-count (alength raw-bytes) :edn-form-index 0}
             :record-identity (select-keys record [:run/id :run-id :timestamp :tick :schema-version])
             :base-task-gain base-gain
             :ranked-actions (mapv #(select-keys % [:action :controller-score :rank]) ranked)}
      result {:row 18 :scope :controller-weight-sensitivity-only
              :source (:source input) :candidate-count (count scores)
              :interventions {:multipliers [1.0 0.75 0.5 0.25]
                              :variational-beta 0.7 :log-habit-priors nil
                              :f-pi :absent
                              :note "Controlled inputs, not a replay of the live complete posterior."}
              :arms arms
              :controls {:positive-modes-change? positive-modes-change?
                         :variational-mode-ignores-gain? variational-mode-ignores-gain?
                         :floor-can-hide-modulation? floor-can-hide-modulation?
                         :floor-control-temperatures floor-control}
              :not-established [:empirical-calibration :trip-discharge-join
                                :selection-enaction-change :safer-behavior]}]
  (io/make-parents (io/file out-dir "input.edn"))
  (spit (io/file out-dir "input.edn") (str (pr-str input) "\n"))
  (spit (io/file out-dir "result.edn") (str (pr-str result) "\n"))
  (prn (select-keys result [:scope :candidate-count :controls])))
