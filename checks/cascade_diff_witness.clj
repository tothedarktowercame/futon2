#!/usr/bin/env bb
(ns checks.cascade-diff-witness
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def root (fs/cwd))
(def fixture-path (fs/path root "holes/labs/wm-contract/cascade-diff-reference.edn"))

(defn reachable? [edges from to]
  (loop [frontier [from] seen #{}]
    (if-let [x (first frontier)]
      (cond (= x to) true
            (seen x) (recur (rest frontier) seen)
            :else (recur (into (vec (rest frontier))
                               (map second (filter #(= x (first %)) edges)))
                         (conj seen x)))
      false)))

(defn fast-forward? [selected edges from to]
  (letfn [(walk [x seen]
            (some (fn [[_ y]]
                    (cond (= y to) true
                          (selected y) false
                          (seen y) false
                          :else (walk y (conj seen y))))
                  (filter #(= x (first %)) edges)))]
    (and (selected from) (selected to) (not= from to) (boolean (walk from #{from})))))

(defn cascade-diff? [x]
  (let [selected (set (:selected x))
        additions (set (:added-by-organise x))
        nodes (set (:nodes x))
        authored (set (:authored-edges x))
        organised (set (:organised-edges x))
        expected-fast-forwards
        (set (for [u selected v selected :when (fast-forward? selected authored u v)] [u v]))
        [before after :as variants] (:precedence-variants x)]
    (and (= :cascade-diff/v1 (:schema x))
         (seq selected) (= nodes (into selected additions))
         (every? (fn [[u v]] (reachable? authored u v)) organised)
         (= organised expected-fast-forwards)
         (= 2 (count variants))
         (= nodes (set (:precedence before)))
         (= nodes (set (:precedence after)))
         (not= (:precedence before) (:precedence after))
         (or (not= (:acting-order before) (:acting-order after))
             (not= (:score before) (:score after))))))

(defn source-pinned? [x]
  (let [{:keys [path sha256]} (get-in x [:derivation :source])]
    (= sha256 (first (str/split
                       (:out (process/shell {:out :string} "sha256sum"
                                            (str (fs/path root path)))) #"\s+")))))

(defn -main [& args]
  (let [negative? (some #{"--negative" "--negative-control"} args)
        fixture (edn/read-string (slurp (str fixture-path)))
        tested (if negative?
                 (update fixture :organised-edges conj [:remedy :probe])
                 fixture)
        accepted? (and (cascade-diff? tested) (or negative? (source-pinned? tested)))
        exit (cond (and negative? accepted?) 2 negative? 0 accepted? 0 :else 1)]
    (println (cond
               (= exit 2) "cascade-diff-witness: mutation slipped"
               negative? "cascade-diff-witness: negative-control PASS (unsupported edge rejected)"
               accepted? "cascade-diff-witness: PASS"
               :else "cascade-diff-witness: FAIL"))
    (System/exit exit)))

(apply -main *command-line-args*)
