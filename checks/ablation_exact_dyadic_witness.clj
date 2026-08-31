#!/usr/bin/env bb
(ns checks.ablation-exact-dyadic-witness
  (:require [clojure.edn :as edn]
            [clojure.set :as set]))

(def fixture "holes/labs/wm-contract/ablation-exact-dyadic.edn")
(defn q [[n d]] (/ n d))
(defn argmins [rows k]
  (let [best (apply min (map #(q (k %)) rows))]
    (set (map :policy (filter #(= best (q (k %))) rows)))))
(defn valid? [x]
  (let [rows (:rows x) g (argmins rows :G) risk (argmins rows :risk)]
    (and (= :exact-dyadic-ablation/v1 (:schema x))
         (= :exact-dyadic-rational (get-in x [:numeric-semantics :interpretation]))
         (= (set (:policies x)) (set (map :policy rows)))
         (= g (get-in x [:expected :argmin-G]))
         (= risk (get-in x [:expected :argmin-risk]))
         (empty? (set/intersection g risk)))))
(defn -main [& args]
  (let [x (edn/read-string (slurp fixture))
        negative? (some #{"--negative" "--negative-control"} args)
        tested (if negative?
                 (assoc x :rows (mapv #(assoc % :risk (:G %)) (:rows x))
                          :expected (assoc (:expected x)
                                           :argmin-risk #{:grim :probe-one-token}))
                 x)
        accepted? (valid? tested)]
    (println (if negative?
               (if accepted? "ablation-exact-dyadic: mutation slipped"
                   "ablation-exact-dyadic: negative-control PASS (minimizer separation removed and rejected)")
               (if accepted? "ablation-exact-dyadic: PASS" "ablation-exact-dyadic: FAIL")))
    (System/exit (cond (and negative? accepted?) 2 negative? 0 accepted? 0 :else 1))))
(apply -main *command-line-args*)
