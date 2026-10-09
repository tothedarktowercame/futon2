(ns census-head-policies-g
  "Cold, exhaustive G census for every distinct recorded HEAD policy.

  Run from the futon2 repository root:
    clojure -M scripts/census_head_policies_g.clj

  This is deliberately separate from the bounded unit suite."
  (:require [clojure.java.io :as io]
            [futon2.aif.cascade-shape-g :as shape-g])
  (:import (java.time Instant ZoneOffset)
           (java.time.format DateTimeFormatter)))

(def artifacts
  "holes/labs/wm-contract/mission-head-cascades-2026-09-30")

(def declared-refusal
  :frontier-too-wide-for-exact-enumeration)

(defn- now [] (str (Instant/now)))

(defn- finite-number? [x]
  (and (number? x) (Double/isFinite (double x))))

(defn- score-one [ordinal total policy]
  (let [started-ns (System/nanoTime)
        base {:ordinal ordinal
              :total total
              :target (:target policy)
              :policy-id (:policy-id policy)
              :node-count (count (get-in policy [:cascade :nodes]))}
        row (try
              (let [score (shape-g/score-policy policy)]
                (merge base
                       (select-keys score [:status :kind :g :f :risk :ambiguity
                                           :information-gain :horizon])
                       {:valid? (or (and (= :computed (:status score))
                                          (finite-number? (:g score)))
                                     (and (= :refused (:status score))
                                          (= declared-refusal (:kind score))))}))
              (catch Throwable t
                (assoc base :status :error :valid? false
                       :error-class (.getName (class t))
                       :error (.getMessage t))))
        elapsed-ms (long (/ (- (System/nanoTime) started-ns) 1000000))
        row (assoc row :elapsed-ms elapsed-ms)]
    (println (format "[%02d/%02d] %s nodes=%d status=%s%s elapsed-ms=%d"
                     ordinal total (:policy-id policy) (:node-count row)
                     (name (:status row))
                     (if (:kind row) (str " reason=" (name (:kind row))) "")
                     elapsed-ms))
    (flush)
    row))

(defn- output-path []
  (let [stamp (.format (DateTimeFormatter/ofPattern "yyyyMMdd'T'HHmmss'Z'")
                       (.atZone (Instant/now) ZoneOffset/UTC))]
    (io/file (System/getProperty "user.home") "runs" "wmq-head-policy-census"
             (str "head-policies-g-" stamp ".edn"))))

(defn run-census! []
  (let [started-at (now)
        started-ns (System/nanoTime)
        policies (shape-g/materialize-policies artifacts)
        total (count policies)
        rows (mapv (fn [i policy] (score-one (inc i) total policy))
                   (range total) policies)
        invalid (filterv (comp not :valid?) rows)
        result {:schema :wm/head-policy-g-census-v1
                :artifacts artifacts
                :started-at started-at
                :finished-at (now)
                :wall-ms (long (/ (- (System/nanoTime) started-ns) 1000000))
                :reported-count (:reported-count (meta policies))
                :distinct-count (:distinct-count (meta policies))
                :policy-count total
                :computed-count (count (filter #(= :computed (:status %)) rows))
                :declared-refusal-count
                (count (filter #(and (= :refused (:status %))
                                     (= declared-refusal (:kind %))) rows))
                :invalid-count (count invalid)
                :valid? (empty? invalid)
                :policies rows}
        out (output-path)]
    (io/make-parents out)
    (spit out (str (pr-str result) "\n"))
    (println (format "RESULT valid=%s policies=%d invalid=%d wall-ms=%d path=%s"
                     (:valid? result) total (count invalid) (:wall-ms result)
                     (.getPath out)))
    (flush)
    {:result result :path (.getPath out)}))

(let [{:keys [result]} (run-census!)]
  (shutdown-agents)
  (System/exit (if (:valid? result) 0 1)))
