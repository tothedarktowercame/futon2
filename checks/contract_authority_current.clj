#!/usr/bin/env bb
(ns checks.contract-authority-current
  (:require [babashka.process :as process]
            [cheshire.core :as json]
            [clojure.string :as str]))

(def mathlib-root "/home/joe/code/mathlib4")
(def contract-path
  "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json")
(def lean-path "DarkTower/WarMachine/Holes.lean")

(defn shell [& argv]
  (apply process/shell {:continue true :out :string :err :string
                        :dir mathlib-root}
         argv))

(defn current-state []
  (let [head-result (shell "git" "rev-parse" "HEAD")
        diff-result (shell "git" "diff" "--quiet" "HEAD" "--" lean-path)
        blob-result (shell "git" "rev-parse" (str "HEAD:" lean-path))
        contract (json/parse-string (slurp contract-path) true)
        recorded-authority (get-in contract [:source :git-sha])
        recorded-blob-result (shell "git" "rev-parse"
                                    (str recorded-authority ":" lean-path))]
    {:recorded-authority recorded-authority
     :mathlib-head (str/trim (:out head-result))
     :recorded-holes-blob (str/trim (:out recorded-blob-result))
     :current-holes-blob (str/trim (:out blob-result))
     :holes-clean? (zero? (:exit diff-result))
     :recorded-authority-readable? (zero? (:exit recorded-blob-result))
     :readable? (and (zero? (:exit head-result))
                     (zero? (:exit blob-result)))}))

(defn assess [{:keys [recorded-holes-blob current-holes-blob holes-clean?
                      recorded-authority-readable? readable?]
               :as state}]
  (let [failures (cond-> []
                   (not readable?) (conj :source-unreadable)
                   (not recorded-authority-readable?)
                   (conj :recorded-authority-unreadable)
                   (not holes-clean?) (conj :holes-working-tree-dirty)
                   (and recorded-authority-readable?
                        (not= recorded-holes-blob current-holes-blob))
                   (conj :contract-source-not-current))]
    (assoc state :pass? (empty? failures) :failures failures)))

(defn -main [& args]
  (let [negative? (= ["--negative-control"] (vec args))
        state (current-state)
        tested (if negative?
                 (assoc state :recorded-holes-blob (apply str (repeat 40 "0")))
                 state)
        result (assess tested)
        success? (if negative? (not (:pass? result)) (:pass? result))]
    (println "contract-authority-current:"
             (if success? "PASS" "FAIL")
             (pr-str (assoc result :negative-control negative?))
             "exit-convention=0-pass/1-fail")
    (System/exit (if success? 0 1))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
