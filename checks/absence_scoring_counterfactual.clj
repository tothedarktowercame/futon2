#!/usr/bin/env bb
(ns checks.absence-scoring-counterfactual
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def trace-dir "/home/joe/code/futon2/data/wm-trace")
(defn sha256 [s]
  (let [d (java.security.MessageDigest/getInstance "SHA-256")]
    (format "%064x" (java.math.BigInteger. 1 (.digest d (.getBytes s "UTF-8"))))))
(defn trace-files []
  (->> (fs/list-dir trace-dir)
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (fs/file-name %)))
       sort vec))
(defn forms [path]
  (with-open [r (java.io.PushbackReader. (io/reader (str path)))]
    (loop [out []]
      (let [x (edn/read {:eof ::eof
                         :default (fn [tag value] (tagged-literal tag value))} r)]
        (if (= ::eof x) out (recur (conj out x)))))))
(defn tagged-observation? [o]
  (and (= :futon2.aif/tagged-observation (:type o))
       (map? (:channels o))
       (every? #(contains? % :variant) (vals (:channels o)))))

(defn report []
  (let [files (trace-files)
        records (mapcat forms files)
        candidates (reduce + 0 (map #(count (:ranked-actions %)) records))
        tagged (filter #(tagged-observation? (:observation-envelope %)) records)
        shadowed (filter #(map? (:support-typed-scoring-shadow %)) records)
        shadow-candidates (mapcat #(get-in % [:support-typed-scoring-shadow
                                              :candidates])
                                  shadowed)
        pin-input (apply str (for [f files]
                               (str (fs/file-name f) "\n" (slurp (str f)) "\n")))]
    {:measurement :absence-in-scoring-counterfactual
     :corpus {:dir trace-dir :files (count files) :records (count records)
              :content-sha256 (sha256 pin-input)}
     :scored-candidates candidates
     :records-with-presence-provenance (count tagged)
     :classifiable-scored-candidates
     (reduce + 0 (map #(count (:ranked-actions %)) tagged))
     :records-with-support-shadow (count shadowed)
     :shadowed-scored-candidates (count shadow-candidates)
     :candidates-with-absent-channels
     (if (seq shadowed)
       (count (filter #(seq (:absent-reasons %)) shadow-candidates))
       :unknown)
     :candidate-rank-changes
     (if (seq shadowed)
       (count (filter true? (map :would-rank-differently shadow-candidates)))
       :unknown)
     :winner-changing-records
     (if (seq shadowed)
       (count (filter #(true? (get-in % [:support-typed-scoring-shadow
                                         :comparison :winner-changed?]))
                      shadowed))
       :unknown)
     :incomparable-support-pairs
     (if (seq shadowed)
       (reduce + 0 (map #(get-in % [:support-typed-scoring-shadow :comparison
                                    :incomparable-support-pairs] 0)
                        shadowed))
       :unknown)
     :coverage-limitation
     (if (seq shadowed)
       :post-v17-shadow-records-only
       :no-post-v17-shadow-records-yet)}))

(println (pr-str (report)))
