#!/usr/bin/env bb
(ns checks.r2-pinned-snapshot-witness
  (:require [clojure.edn :as edn]))

(def fixture "holes/labs/wm-contract/R2-D2-report.edn")
(def claim :content-current)
(def expected-pin "b2c3aeb408cc4de59947ad93f9c1ea17b735fc0da26e188ada7c24609bffbca1")
(defn valid? [x]
  (and (= 54 (get-in x [:summary :files]))
       (= 801 (get-in x [:summary :forms]))
       (= expected-pin (get-in x [:content-pin :sha256]))
       (= 2 (get-in x [:r2ContractCensusWmTrace :ill-formed]))
       (= 2 (count (get-in x [:r2ContractCensusWmTrace :ill-formed-ticks])))))
(defn -main [& args]
  (let [kind (cond (some #{"--negative-pin"} args) :pin
                   (some #{"--negative-census"} args) :census)
        x (edn/read-string (slurp fixture))
        tested (case kind
                 :pin (assoc-in x [:content-pin :sha256] (apply str (repeat 64 "0")))
                 :census (assoc-in x [:r2ContractCensusWmTrace :ill-formed] 3)
                 x)
        accepted? (valid? tested)]
    (println (cond
               (and kind accepted?) "r2-pinned-snapshot: mutation slipped"
               kind (str "r2-pinned-snapshot: negative-control PASS (" (name kind) " rejected)")
               accepted? (str "r2-pinned-snapshot: PASS claim=" claim)
               :else "r2-pinned-snapshot: FAIL"))
    (System/exit (cond (and kind accepted?) 2 kind 0 accepted? 0 :else 1))))
(apply -main *command-line-args*)
