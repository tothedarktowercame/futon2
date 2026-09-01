#!/usr/bin/env bb
(ns checks.r8-pinned-snapshot-witness
  (:require [checks.mutable-read-set :as read-set]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def fixture "holes/labs/wm-contract/R8-D3-report.edn")
(def generated "holes/labs/wm-contract/R8-D3-report.lean")
(def source "/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean")
(def expected-pin
  "c9add16ac96c973ba4fd9a0c61f3b7319780c304424e2d14ea7b477309947880")

(defn literal [text declaration next-marker]
  (second (re-find
           (re-pattern (str "(?s)def " declaration
                            " : List R8TickLit :=\\n(\\[.*?\\n\\])\\n\\n"
                            next-marker))
           text)))

(defn valid? [report source-text generated-text]
  (let [source-literal (literal source-text "wmTraceR8" "/-- CLOSED-BY-RECORD")
        generated-literal (literal generated-text "wmTraceR8Generated" "theorem generatedCensus")]
    (and (= 53 (get-in report [:summary :files]))
         (= 792 (get-in report [:summary :forms]))
         (= expected-pin (get-in report [:content-pin :sha256]))
         (= {:missing-F-computable 755 :stored-F 32 :insufficient-inputs 5}
            (get-in report [:r8CensusWmTrace :counts]))
         (some? source-literal)
         (= generated-literal source-literal))))

(defn -main [& args]
  (let [kind (cond (some #{"--negative-pin"} args) :pin
                   (some #{"--negative-census"} args) :census)
        observation (read-set/observe-files [fixture generated source])
        snapshot (read-set/require-stable! observation)
        text #(-> (read-set/entry-by-path snapshot %) :text)
        report (edn/read-string (text fixture))
        tested (case kind
                 :pin (assoc-in report [:content-pin :sha256]
                                (str/join (repeat 64 "0")))
                 :census (assoc-in report
                                   [:r8CensusWmTrace :counts :stored-F] 31)
                 report)
        accepted? (valid? tested (text source) (text generated))]
    (println (cond
               (and kind accepted?) "r8-pinned-snapshot: mutation slipped"
               kind (str "r8-pinned-snapshot: negative-control PASS ("
                         (name kind) " rejected)")
               accepted? "r8-pinned-snapshot: PASS"
               :else "r8-pinned-snapshot: FAIL"))
    (System/exit (cond (and kind accepted?) 2 kind 0 accepted? 0 :else 1))))

(apply -main *command-line-args*)
