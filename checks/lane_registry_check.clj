#!/usr/bin/env bb
(ns checks.lane-registry-check
  "Validate the four-vertex dispatch registry.

   Exit convention: 0 = pass (including a rejected negative mutation),
   1 = ordinary validation/check failure, 2 = negative mutation slipped."
  (:require [clojure.edn :as edn]
            [clojure.string :as str])
  (:import [java.time Instant]))

(def default-registry
  "/home/joe/code/futon2/holes/labs/wm-contract/lane-registry.edn")

(def required-lanes
  #{:wm-nouns :wm-verbs :wm-organization :wm-evidence})

(def required-row-keys
  #{:lane :holding :dispatched-at :job-id :expected-by})

(defn- parse-instant [value]
  (when (string? value)
    (try (Instant/parse value)
         (catch Exception _ nil))))

(defn- row-errors [row now]
  (let [missing-keys (remove #(contains? row %) required-row-keys)
        holding (:holding row)
        dispatched (:dispatched-at row)
        job-id (:job-id row)
        expected (:expected-by row)
        dispatched-time (parse-instant dispatched)
        expected-time (parse-instant expected)]
    (vec
     (concat
      (for [key missing-keys]
        {:error :missing-row-key :lane (:lane row) :key key})
      (when (and (nil? holding) (some some? [dispatched job-id expected]))
        [{:error :idle-row-has-dispatch-state :lane (:lane row)}])
      (when (and (some? holding)
                 (not (and dispatched-time
                           (string? job-id) (not (str/blank? job-id))
                           expected-time)))
        [{:error :active-row-incomplete :lane (:lane row)}])
      (when (and (some? holding) dispatched-time expected-time
                 (not (.isAfter expected-time dispatched-time)))
        [{:error :expected-by-not-after-dispatch :lane (:lane row)}])
      (when (and (some? holding) expected-time (.isBefore expected-time now))
        [{:error :overdue :lane (:lane row)
          :holding holding :expected-by expected}])))))

(defn validate-registry
  ([data] (validate-registry data (Instant/now)))
  ([data now]
   (let [rows (:lanes data)
         lane-frequencies (frequencies (map :lane rows))
         present (set (keys lane-frequencies))
         missing (sort (remove present required-lanes))
         unexpected (sort (remove required-lanes present))
         duplicates (sort (keep (fn [[lane n]] (when (> n 1) lane))
                                lane-frequencies))
         by-lane (into {} (map (juxt :lane identity) rows))
         errors (vec
                 (concat
                  (when-not (vector? rows)
                    [{:error :lanes-not-vector}])
                  (for [lane missing] {:error :missing-lane :lane lane})
                  (for [lane unexpected] {:error :unexpected-lane :lane lane})
                  (for [lane duplicates] {:error :duplicate-lane :lane lane})
                  (mapcat #(row-errors % now) rows)))
         lane-reports
         (mapv (fn [lane]
                 (if-let [row (get by-lane lane)]
                   (let [expected (parse-instant (:expected-by row))
                         overdue? (and (some? (:holding row)) expected
                                       (.isBefore expected now))]
                     {:lane lane
                      :state (cond overdue? :overdue
                                   (nil? (:holding row)) :idle
                                   :else :holding)
                      :holding (:holding row)
                      :job-id (:job-id row)
                      :expected-by (:expected-by row)})
                   {:lane lane :state :missing :holding nil
                    :job-id nil :expected-by nil}))
               (sort required-lanes))]
     {:pass? (empty? errors)
      :as-of (:as-of data)
      :lanes lane-reports
      :errors errors})))

(defn- parse-args [args]
  (loop [opts {:registry default-registry} xs args]
    (if (empty? xs)
      opts
      (let [[arg & more] xs]
        (case arg
          "--registry" (if-let [path (first more)]
                         (recur (assoc opts :registry path) (rest more))
                         (throw (ex-info "--registry requires a path" {})))
          "--negative" (let [candidate (first more)
                               mode (if (and candidate
                                             (not (str/starts-with? candidate "--")))
                                      (keyword candidate)
                                      :missing)
                               remaining (if (= mode :missing)
                                           (if (= candidate "missing") (rest more) more)
                                           (rest more))]
                           (recur (assoc opts :negative mode) remaining))
          (throw (ex-info "unknown argument" {:argument arg})))))))

(defn- mutate [data mode]
  (case mode
    :missing (update data :lanes
                     #(vec (remove (fn [row] (= :wm-evidence (:lane row))) %)))
    :overdue (update data :lanes
                     (fn [rows]
                       (mapv (fn [row]
                               (if (= :wm-verbs (:lane row))
                                 (assoc row
                                        :holding :C33-negative-control
                                        :dispatched-at "2020-01-01T00:00:00Z"
                                        :job-id "invoke-negative-control"
                                        :expected-by "2020-01-01T00:01:00Z")
                                 row))
                             rows)))
    (throw (ex-info "negative mode must be missing or overdue" {:mode mode}))))

(defn -main [& args]
  (let [opts (try (parse-args args)
                  (catch Exception failure
                    (binding [*out* *err*]
                      (println (.getMessage failure))
                      (println "usage: lane_registry_check.clj [--registry FILE] [--negative missing|overdue]"))
                    (System/exit 1)))
        negative (:negative opts)
        report (try
                 (let [data (edn/read-string (slurp (:registry opts)))
                       candidate (if negative (mutate data negative) data)]
                   (validate-registry candidate))
                 (catch Exception failure
                   {:pass? false :lanes []
                    :errors [{:error :check-failed
                              :message (.getMessage failure)}]}))]
    (doseq [lane (:lanes report)] (println (pr-str lane)))
    (doseq [error (:errors report)]
      (binding [*out* *err*] (println (pr-str error))))
    (if negative
      (if (:pass? report)
        (do (println "lane-registry-check: FAIL mutation slipped exit-convention=0-pass/1-fail/2-mutation-slipped")
            (System/exit 2))
        (do (println (str "lane-registry-check: PASS negative " (name negative)
                          " rejected exit-convention=0-pass/1-fail/2-mutation-slipped"))
            (System/exit 0)))
      (do (println (str "lane-registry-check: " (if (:pass? report) "PASS" "FAIL")
                        " exit-convention=0-pass/1-fail/2-mutation-slipped"))
          (System/exit (if (:pass? report) 0 1))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
