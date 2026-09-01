#!/usr/bin/env bb
(ns checks.cascade-diff-witness
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def root (fs/cwd))
(def fixture-path (fs/path root "holes/labs/wm-contract/cascade-diff-reference.edn"))
(def claim :content-current)

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

(defn cascade-claims [x]
  (let [selected (set (:selected x))
        additions (set (:added-by-organise x))
        nodes (set (:nodes x))
        authored (set (:authored-edges x))
        organised (set (:organised-edges x))
        expected-fast-forwards
        (set (for [u selected v selected :when (fast-forward? selected authored u v)] [u v]))
        [before after :as variants] (:precedence-variants x)]
    {:schema (and (= :cascade-diff/v1 (:schema x)) (boolean (seq selected)))
     :o1 (= nodes (into selected additions))
     :o2 (every? (fn [[u v]] (reachable? authored u v)) organised)
     :o3 (= organised expected-fast-forwards)
     :o4 (and (= 2 (count variants))
              (= nodes (set (:precedence before)))
              (= nodes (set (:precedence after)))
              (not= (:precedence before) (:precedence after))
              (or (not= (:acting-order before) (:acting-order after))
                  (not= (:score before) (:score after))))}))

(defn cascade-diff? [x]
  (every? boolean (vals (cascade-claims x))))

(defn source-pinned? [x]
  (let [{:keys [path git-sha sha256]} (get-in x [:derivation :source])
        content (:out (process/shell {:dir root :out :string}
                                     "git" "show" (str git-sha ":" path)))
        tmp (fs/create-temp-file {:prefix "cascade-source-"})]
    (spit (str tmp) content)
    (try
    (= sha256 (first (str/split
                       (:out (process/shell {:out :string} "sha256sum"
                                            (str tmp))) #"\s+")))
      (finally (fs/delete-if-exists tmp)))))

(def mutations
  {:o1 #(update % :added-by-organise conj :unrecorded-addition)
   :o2 #(update % :organised-edges conj [:remedy :probe])
   :o3 #(assoc % :organised-edges [])
   :o4 #(assoc-in % [:precedence-variants 1]
                   (get-in % [:precedence-variants 0]))})

(defn -main [& args]
  (let [negative-kind (cond
                        (some #{"--negative-o1"} args) :o1
                        (some #{"--negative-o3"} args) :o3
                        (some #{"--negative-o4"} args) :o4
                        (some #{"--negative" "--negative-control" "--negative-o2"} args) :o2)
        fixture (edn/read-string (slurp (str fixture-path)))
        tested (if negative-kind
                 ((mutations negative-kind) fixture)
                 fixture)
        baseline-valid? (and (cascade-diff? fixture) (source-pinned? fixture))
        mutation-rejected? (not (cascade-diff? tested))
        exit (cond (and negative-kind (not baseline-valid?)) 1
                   (and negative-kind (not mutation-rejected?)) 2
                   negative-kind 0 baseline-valid? 0 :else 1)]
    (println (cond
               (and negative-kind (not baseline-valid?)) "cascade-diff-witness: BASELINE-INVALID (control reason not established)"
               (= exit 2) "cascade-diff-witness: mutation slipped"
               negative-kind (str "cascade-diff-witness: negative-control PASS ("
                                  (name negative-kind) " mutation rejected; claims "
                                  (pr-str (cascade-claims tested)) ")")
               baseline-valid? (str "cascade-diff-witness: PASS claim=" claim)
               :else "cascade-diff-witness: FAIL"))
    (System/exit exit)))

(apply -main *command-line-args*)
