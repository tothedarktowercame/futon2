(ns futon2.aif.registered-run-series
  "Validation and per-opportunity admission for registered WM run series."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def schema :wm/registered-run-series-v1)
(def opportunity-schema :wm/registered-run-opportunity-v1)
(def entity-kinds #{:mission :excursion :ticket})
(def certificate-kinds
  #{:construction :admission :selection :g :target-local-aqc
    :enactment-grounding})

(defn- refuse [kind data]
  {:status :refused :kind kind :data data})

(defn validate-registration [registration]
  (let [sampling (:sampling registration)
        required (set (get-in registration [:running-example-eligibility :requires]))
        measurement-certs (set (get-in registration [:measurements :conformance]))]
    (cond
      (not= schema (:schema registration))
      (refuse :registration-schema-mismatch {:schema (:schema registration)})

      (not (keyword? (:series/id registration)))
      (refuse :series-id-required {})

      (not (pos-int? (:maximum-opportunities sampling)))
      (refuse :maximum-opportunities-required {})

      (not= :ordinary-live-priority-order (:target-choice sampling))
      (refuse :target-choice-must-remain-live {})

      (not (true? (:no-target-retries-for-balance sampling)))
      (refuse :balance-retries-must-be-forbidden {})

      (not= certificate-kinds measurement-certs)
      (refuse :certificate-family-mismatch
              {:expected certificate-kinds :actual measurement-certs})

      (not (set/subset?
            #{:complete-timing :complete-token-usage
              :all-applicable-lean-certificates-compiled
              :certificate-mutation-caught}
            required))
      (refuse :running-example-evidence-incomplete {:requires required})

      (not (true? (get-in registration [:measurement-rules :no-cost-imputation])))
      (refuse :cost-imputation-must-be-forbidden {})

      :else {:status :valid :series/id (:series/id registration)})))

(defn read-registration [path]
  (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file path)))
        registration (edn/read-string (String. bytes "UTF-8"))
        validation (validate-registration registration)]
    (merge validation
           {:path (.getCanonicalPath (io/file path))
            :sha256 (load-identity/sha256 bytes)
            :registration registration})))

(defn evaluate-opportunity
  "Evaluate one retained series row without inventing absent measurements.
   Every admitted opportunity is reportable; eligibility is deliberately
   stricter than successful termination."
  [registration row]
  (let [registration-check (validate-registration registration)
        timing (:timing row)
        usage (:model-usage row)
        certs (:certificates row)
        complete-timing? (and (number? (:wall-clock-ms timing))
                              (pos? (:wall-clock-ms timing))
                              (map? (:phase-timings-ms timing))
                              (seq (:phase-timings-ms timing))
                              (every? (fn [[_ v]] (and (number? v) (<= 0 v)))
                                      (:phase-timings-ms timing)))
        complete-usage? (and (integer? (:input-tokens usage))
                             (<= 0 (:input-tokens usage))
                             (integer? (:output-tokens usage))
                             (<= 0 (:output-tokens usage))
                             (= (:total-tokens usage)
                                (+ (:input-tokens usage) (:output-tokens usage))))
        compiled (set (for [[kind receipt] certs
                            :when (= :compiled (:status receipt))]
                        kind))
        mutations? (every? #(= :caught (get-in certs [% :mutation]))
                           certificate-kinds)
        observed (cond-> #{}
                   (:selection-reached? row) (conj :selection-reached)
                   (seq (:verified-applied-patterns row)) (conj :verified-applied-pattern)
                   (= :present (:before-after-want-evidence row))
                   (conj :before-after-want-evidence)
                   (= :independent (get-in row [:review :independence]))
                   (conj :independent-review)
                   complete-timing? (conj :complete-timing)
                   complete-usage? (conj :complete-token-usage)
                   (= certificate-kinds compiled)
                   (conj :all-applicable-lean-certificates-compiled)
                   mutations? (conj :certificate-mutation-caught)
                   (seq (:plain-language-achievement row))
                   (conj :plain-language-achievement)
                   (seq (:plop-pattern-anchors row)) (conj :plop-pattern-anchors))
        required (set (get-in registration [:running-example-eligibility :requires]))
        missing (set/difference required observed)
        terminal-ok? (contains?
                      (get-in registration [:running-example-eligibility
                                            :terminal-outcomes])
                      (:terminal-outcome row))]
    (cond
      (not= :valid (:status registration-check)) registration-check
      (not= opportunity-schema (:schema row))
      (refuse :opportunity-schema-mismatch {:schema (:schema row)})
      (not (:admitted-opportunity? row))
      (refuse :opportunity-not-admitted {})
      :else
      {:status :recorded
       :series/id (:series/id registration)
       :opportunity/id (:opportunity/id row)
       :counts-in-denominator? true
       :running-example-eligible? (and terminal-ok? (empty? missing))
       :terminal-outcome-admitted? terminal-ok?
       :observed observed
       :missing missing
       :measurement-status
       {:timing (if complete-timing? :complete :typed-missing)
        :model-usage (if complete-usage? :complete :typed-missing)
        :certificates {:compiled compiled
                       :required certificate-kinds
                       :mutations (if mutations? :caught :incomplete)}}})))
