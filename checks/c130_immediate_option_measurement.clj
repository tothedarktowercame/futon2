#!/usr/bin/env bb
(ns checks.c130-immediate-option-measurement
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def trace-dir "/home/joe/code/futon2/data/wm-trace")
(def health-signs
  {:annotation-health 1 :sorry-count-norm -1 :mission-health 1
   :active-repo-ratio 1 :support-coverage 1 :attack-coverage 1
   :coupling-density 1 :ticks-firing-ratio 1})

(defn sha256 [s]
  (let [d (java.security.MessageDigest/getInstance "SHA-256")]
    (format "%064x" (java.math.BigInteger. 1 (.digest d (.getBytes s "UTF-8"))))))

(defn trace-files []
  (->> (fs/list-dir trace-dir)
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn"
                            (fs/file-name %)))
       sort vec))

(defn forms [path]
  (with-open [r (java.io.PushbackReader. (io/reader (str path)))]
    (loop [out []]
      (let [x (edn/read {:eof ::eof
                         :default (fn [tag value] (tagged-literal tag value))} r)]
        (if (= ::eof x) out (recur (conj out x)))))))

(defn tagged? [record]
  (= :futon2.aif/tagged-observation
     (get-in record [:observation-envelope :type])))

(defn absent-channel? [record channel]
  (= :absent (get-in record [:observation-envelope :channels channel :variant])))

(defn fallback-without-sorry [ranked]
  (let [by-type (group-by #(get-in % [:action :type]) ranked)
        learns (get by-type :learn-action-class [])
        address (get by-type :address-sorry [])
        no-ops (get by-type :no-op [])]
    (or (when (seq learns)
          (apply max-key #(double (or (get-in % [:action :intrinsic-value]) 0.0))
                 learns))
        (first address)
        (first no-ops))))

(defn aggregate-driver [weighted-errors]
  (let [eligible (filter (fn [[ch _]] (not (zero? (get health-signs ch 0))))
                         weighted-errors)
        numerator (reduce + 0.0
                          (map (fn [[ch e]]
                                 (* (double (get health-signs ch))
                                    (double (:weighted-error e))))
                               eligible))
        denominator (reduce + 0.0 (map #(double (:precision (second %))) eligible))]
    (when (pos? denominator) (/ numerator denominator))))

(defn sorry-measure [record]
  (let [ranked (:ranked-actions record)
        continued (fallback-without-sorry ranked)]
    {:timestamp (:timestamp record)
     :candidate-population (count ranked)
     :missing-reason (get-in record [:observation-envelope :channels
                                     :sorry-count-norm :reason])
     :option-a {:result :abstain :reason :unknown-sorry-pressure}
     :option-b {:result (if continued :selected :abstain)
                :action (:action continued)
                :rank (:rank continued)}}))

(defn belief-measure [record]
  (let [errors (:prediction-errors record)
        absent (set (for [[ch _] errors :when (absent-channel? record ch)] ch))
        kept (apply dissoc errors absent)
        current (aggregate-driver errors)
        omit (aggregate-driver kept)
        recorded (some-> record :micro-step-trace last :aggregated-signed-error)]
    {:timestamp (:timestamp record)
     :channels-recorded (count errors)
     :honestly-absent-channels (vec (sort absent))
     :current-coerced-driver current
     :recorded-final-driver recorded
     :reconstruction-error (when (number? recorded) (Math/abs (- current recorded)))
     :option-a {:result :aggregate-observed-support
                :channels-used (count kept)
                :driver omit
                :driver-delta-from-current (- omit current)
                :sign-changed? (not= (compare omit 0.0) (compare current 0.0))}
     :option-b {:result (if (seq absent) :refuse-incomplete-collection
                            :aggregate-complete-collection)}}))

(defn report []
  (let [files (trace-files)
        records (mapcat forms files)
        current (filter tagged? records)
        sorry-records (filter #(absent-channel? % :sorry-count-norm) current)
        belief-records (filter #(and (map? (:prediction-errors %))
                                     (seq (:prediction-errors %))) current)
        sorry (mapv sorry-measure sorry-records)
        belief (mapv belief-measure belief-records)
        pin-input (apply str (for [f files]
                               (str (fs/file-name f) "\n" (slurp (str f)) "\n")))]
    {:measurement :c130-immediate-option-effects
     :boundary :immediate-only
     :downstream-ranking-effects :unmeasured-requires-sequential-replay
     :corpus {:dir trace-dir :files (count files) :records (count records)
              :content-sha256 (sha256 pin-input)
              :current-schema-records (count current)}
     :sorry-pressure
     {:records-with-unknown-pressure (count sorry)
      :candidate-populations-total (reduce + 0 (map :candidate-population sorry))
      :note :candidate-count-is-exposure-not-independent-decision-count
      :records sorry}
     :belief-aggregation
     {:records-measured (count belief)
      :records-with-honestly-absent-input
      (count (filter #(seq (:honestly-absent-channels %)) belief))
      :records-where-omit-changes-driver
      (count (filter #(not (zero? (get-in % [:option-a :driver-delta-from-current])))
                     belief))
     :records-where-omit-changes-sign
      (count (filter #(true? (get-in % [:option-a :sign-changed?])) belief))
      :records belief}
     :coverage-limitation
     "Option A/B effects are measured only at the selector/aggregate boundary. Downstream ranking effects are unmeasured and require sequential replay."}))

;; Keep the literal report construction legible while avoiding a second pass
;; hidden behind a summary helper.
(let [r (report)]
  (when (or (zero? (get-in r [:corpus :current-schema-records]))
            (zero? (get-in r [:sorry-pressure :records-with-unknown-pressure]))
            (zero? (get-in r [:belief-aggregation :records-measured]))
            (some #(or (nil? (:reconstruction-error %))
                       (> (:reconstruction-error %) 1.0e-12))
                  (get-in r [:belief-aggregation :records])))
    (binding [*out* *err*] (println "C130 immediate measurement lacks required records"))
    (System/exit 1))
  (println (pr-str r)))
