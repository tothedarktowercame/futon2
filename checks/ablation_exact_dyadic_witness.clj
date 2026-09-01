#!/usr/bin/env bb
(ns checks.ablation-exact-dyadic-witness
  (:require [clojure.edn :as edn]
            [clojure.set :as set]))

(def fixture "holes/labs/wm-contract/ablation-exact-dyadic.edn")
(def claim :content-current)
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
        baseline-valid? (valid? x)
        mutation-rejected? (not (valid? tested))]
    (println (if negative?
               (cond (not baseline-valid?) "ablation-exact-dyadic: BASELINE-INVALID (control reason not established)"
                     (not mutation-rejected?) "ablation-exact-dyadic: mutation slipped"
                     :else "ablation-exact-dyadic: negative-control PASS (minimizer separation removed and rejected)")
               (str (if baseline-valid? "ablation-exact-dyadic: PASS" "ablation-exact-dyadic: FAIL")
                    " claim=" claim)))
    (System/exit (cond (and negative? (not baseline-valid?)) 1
                       (and negative? (not mutation-rejected?)) 2
                       negative? 0 baseline-valid? 0 :else 1))))
(apply -main *command-line-args*)
