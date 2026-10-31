(ns futon2.aif.effective-run-history
  "Cross-run identity for effective work, independent of E3 refusal history."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def required-keys
  [:target :giraffe-manifest-sha256 :policy-id
   :compiled-check-set-sha256 :mission-source-sha256])

(defn comparable? [identity]
  (every? #(some? (get identity %)) required-keys))

(defn- measured-finished-work? [row]
  (if (contains? row :satisfaction)
    (true? (get-in row [:satisfaction :wants-measured]))
    ;; Legacy rows predate the satisfaction record and cannot be rewritten.
    ;; Only an already-satisfied close could qualify on declaration presence
    ;; (run 4955b6e6); every other finished-work row is grounded work and
    ;; still blocks.
    (not= :already-satisfied (get-in row [:failure :kind]))))

(defn blocking-row? [row]
  (and (= :indexed (:status row))
       (comparable? (:effective-run-identity row))
       (true? (:finished-work? row))
       (measured-finished-work? row)))

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

(defn index-rows
  "Every parsed row of the effective-run index, or nil when the index is
  absent. Pre-selection admission (outer-select) reads the same rows the
  post-construction guard (compare-index) reads; both call blocking-row?,
  which zai-5's concurrent work owns."
  [dir]
  (let [file (io/file dir "outer-refusal-index.ednl")]
    (when (.isFile file)
      (with-open [reader (io/reader file)]
        (vec (map edn/read-string (line-seq reader)))))))

(defn blocking-rows-for-target
  "Index rows whose own rule (blocking-row?) makes TARGET unrepeatable.
  A newer non-blocking row does not unblock the target: the duplicate
  guard refuses on any blocking row, and pre-selection exclusion must not
  offer a target the guard would refuse."
  [rows target]
  (filterv (fn [row] (and (= target (:target row)) (blocking-row? row))) rows))

(defn require-novel! [comparison]
  (when (= :duplicate-effective-run (:status comparison))
    (throw (ex-info "Effective run identity already has a terminal close"
                    {:outcome :refused
                     :failure-kind :outer/duplicate-effective-run
                     :failure-stage :effective-run-comparison
                     :effective-run-comparison comparison})))
  comparison)
