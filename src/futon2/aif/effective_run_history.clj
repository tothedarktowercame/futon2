(ns futon2.aif.effective-run-history
  "Cross-run identity for effective work, independent of E3 refusal history."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def required-keys
  [:target :giraffe-manifest-sha256 :policy-id
   :compiled-check-set-sha256 :mission-source-sha256])

(def transient-failure-kinds
  ;; The environmental-hold arm of full-loop-runner/repair-class-for.
  #{:agent-unavailable :agent-readiness-failed :substrate-unavailable
    :dispatch-failed :abstained :no-selection :guardrail-refusal
    :transport-timeout :transport-unavailable :trigger-ineligible
    :operator-cancelled})

(defn comparable? [identity]
  (every? #(some? (get identity %)) required-keys))

(defn blocking-row? [row]
  (and (= :indexed (:status row))
       (comparable? (:effective-run-identity row))
       (not (contains? transient-failure-kinds
                       (get-in row [:failure :kind])))))

(defn compare-rows [identity rows]
  (if-not (comparable? identity)
    {:schema :wm/effective-run-comparison-v1 :status :incomparable
     :identity identity :missing (vec (remove #(some? (get identity %)) required-keys))}
    (let [candidates (filter blocking-row? rows)
          matched (first
                   (filter
                    (fn [row]
                      (let [prior (:effective-run-identity row)
                            base-comparable? (and (:base-head identity) (:base-head prior))]
                        (= (cond-> (select-keys identity required-keys)
                             base-comparable? (assoc :base-head (:base-head identity)))
                           (cond-> (select-keys prior required-keys)
                             base-comparable? (assoc :base-head (:base-head prior))))))
                    candidates))]
      {:schema :wm/effective-run-comparison-v1
       :status (if matched :duplicate-effective-run :no-duplicate)
       :identity identity :matched-run matched})))

(defn compare-index [dir identity]
  (let [file (io/file dir "outer-refusal-index.ednl")]
    (if-not (.isFile file)
      {:schema :wm/effective-run-comparison-v1 :status :history-index-absent
       :identity identity}
      (with-open [reader (io/reader file)]
        (compare-rows identity (mapv edn/read-string (line-seq reader)))))))

(defn require-novel! [comparison]
  (when (= :duplicate-effective-run (:status comparison))
    (throw (ex-info "Effective run identity already has a terminal close"
                    {:outcome :refused
                     :failure-kind :outer/duplicate-effective-run
                     :failure-stage :effective-run-comparison
                     :effective-run-comparison comparison})))
  comparison)
