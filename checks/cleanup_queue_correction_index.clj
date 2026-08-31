#!/usr/bin/env bb
(ns checks.cleanup-queue-correction-index
  (:require [clojure.string :as str]))

(def queue-path "/home/joe/code/p4ng/vetting/CLEANUP-QUEUE.md")

(def index-pattern
  #"<!-- CORRECTION id=([^ ]+) target=([^ ]+) -->")

(def pointer-pattern
  #"<!-- CORRECTION-POINTER id=([^ ]+) target=([^ ]+) -->")

(def count-pattern
  #"<!-- CORRECTION-INDEX count=([0-9]+) -->")

(defn rows [pattern text]
  (mapv (fn [[_ id target]] {:id id :target target})
        (re-seq pattern text)))

(defn duplicates [xs]
  (->> xs frequencies (keep (fn [[x n]] (when (> n 1) x))) sort vec))

(defn assess [text]
  (let [index (rows index-pattern text)
        pointers (rows pointer-pattern text)
        declared (some-> (re-find count-pattern text) second parse-long)
        index-map (into {} (map (juxt :id :target) index))
        pointer-map (into {} (map (juxt :id :target) pointers))
        index-duplicates (duplicates (map :id index))
        pointer-duplicates (duplicates (map :id pointers))
        missing-index (sort (remove (set (keys index-map)) (keys pointer-map)))
        missing-pointer (sort (remove (set (keys pointer-map)) (keys index-map)))
        target-mismatches (->> (keys index-map)
                               (filter #(and (contains? pointer-map %)
                                             (not= (index-map %) (pointer-map %))))
                               sort vec)
        failures (cond-> []
                   (nil? declared) (conj :missing-declared-count)
                   (and declared (not= declared (count index)))
                   (conj :declared-count-mismatch)
                   (seq index-duplicates) (conj :duplicate-index-id)
                   (seq pointer-duplicates) (conj :duplicate-pointer-id)
                   (seq missing-index) (conj :pointer-without-index)
                   (seq missing-pointer) (conj :index-without-pointer)
                   (seq target-mismatches) (conj :target-mismatch))]
    {:pass? (empty? failures)
     :declared declared
     :index-count (count index)
     :pointer-count (count pointers)
     :missing-index missing-index
     :missing-pointer missing-pointer
     :target-mismatches target-mismatches
     :failures failures}))

(defn remove-first-index-entry [text]
  (str/replace-first text index-pattern "<!-- NEGATIVE-CONTROL REMOVED INDEX ENTRY -->"))

(defn -main [& args]
  (let [negative? (= ["--negative-control"] (vec args))
        source (slurp queue-path)
        result (assess (if negative? (remove-first-index-entry source) source))
        success? (if negative? (not (:pass? result)) (:pass? result))]
    (println "cleanup-queue-correction-index:"
             (if success? "PASS" "FAIL")
             (pr-str (assoc result :negative-control negative?))
             "exit-convention=0-pass/1-fail")
    (System/exit (if success? 0 1))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
