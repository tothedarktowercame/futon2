#!/usr/bin/env bb
(ns checks.contract-authority-current
  (:require [babashka.process :as process]
            [cheshire.core :as json]
            [clojure.string :as str])
  (:import [java.time Instant]))

(def mathlib-root "/home/joe/code/mathlib4")
(def contract-path
  "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json")
(def lean-path "DarkTower/WarMachine/Holes.lean")

(defn shell [& argv]
  (apply process/shell {:continue true :out :string :err :string
                        :dir mathlib-root}
         argv))

(defn current-state []
  (let [started-at (str (Instant/now))
        head-result (shell "git" "rev-parse" "HEAD")
        last-change-result (shell "git" "log" "-1" "--format=%H" "--" lean-path)
        diff-result (shell "git" "diff" "--quiet" "HEAD" "--" lean-path)
        blob-result (shell "git" "rev-parse" (str "HEAD:" lean-path))
        contract (json/parse-string (slurp contract-path) true)
        recorded-authority (get-in contract [:source :git-sha])
        recorded-blob-result (shell "git" "rev-parse"
                                    (str recorded-authority ":" lean-path))
        finish-head-result (shell "git" "rev-parse" "HEAD")
        finish-blob-result (shell "git" "rev-parse" (str "HEAD:" lean-path))]
    {:recorded-authority recorded-authority
     :mathlib-head (str/trim (:out head-result))
     :mathlib-head-after (str/trim (:out finish-head-result))
     :holes-last-content-change (str/trim (:out last-change-result))
     :recorded-holes-blob (str/trim (:out recorded-blob-result))
     :current-holes-blob (str/trim (:out blob-result))
     :current-holes-blob-after (str/trim (:out finish-blob-result))
     :holes-clean? (zero? (:exit diff-result))
     :recorded-authority-readable? (zero? (:exit recorded-blob-result))
     :readable? (and (zero? (:exit head-result))
                     (zero? (:exit last-change-result))
                     (zero? (:exit blob-result))
                     (zero? (:exit finish-head-result))
                     (zero? (:exit finish-blob-result)))
     :observation-interval {:started-at started-at
                            :finished-at (str (Instant/now))}}))

(defn assess [{:keys [recorded-authority holes-last-content-change
                      recorded-holes-blob current-holes-blob holes-clean?
                      recorded-authority-readable? readable? mathlib-head
                      mathlib-head-after current-holes-blob-after]
               :as state}]
  (let [failures (cond-> []
                   (not readable?) (conj :source-unreadable)
                   (not recorded-authority-readable?)
                   (conj :recorded-authority-unreadable)
                   (not holes-clean?) (conj :holes-working-tree-dirty)
                   (not= recorded-authority holes-last-content-change)
                   (conj :contract-authority-not-last-source-change)
                   (and recorded-authority-readable?
                        (not= recorded-holes-blob current-holes-blob))
                   (conj :contract-source-not-current)
                   (or (not= mathlib-head mathlib-head-after)
                       (not= current-holes-blob current-holes-blob-after))
                   (conj :repository-basis-moved))]
    (assoc state :pass? (empty? failures) :failures failures)))

(defn parse-args [args]
  (loop [xs args out {:negative? false
                      :writer-fence-id (System/getenv "FUTON_WRITER_FENCE_ID")}]
    (if-let [x (first xs)]
      (case x
        "--negative-control" (recur (rest xs) (assoc out :negative? true))
        "--writer-fence" (if-let [id (second xs)]
                           (recur (nnext xs) (assoc out :writer-fence-id id))
                           (throw (ex-info "--writer-fence requires an id" {})))
        (throw (ex-info "unknown argument" {:argument x})))
      out)))

(defn event-claim [result writer-fence-id]
  (let [moved? (some #{:repository-basis-moved} (:failures result))]
    {:claim :event-free
     :interval (:observation-interval result)
     :writer-fence (if writer-fence-id
                     {:status :held :id writer-fence-id}
                     {:status :absent :reason :not-declared})
     :event-free? (cond moved? false writer-fence-id true :else :unverified)
     :distinguishable-cause? (boolean (or moved? writer-fence-id))}))

(defn -main [& args]
  (let [{:keys [negative? writer-fence-id]} (parse-args args)
        state (current-state)
        tested (if negative?
                 (assoc state
                        :recorded-authority (apply str (repeat 40 "0"))
                        :recorded-holes-blob (apply str (repeat 40 "0")))
                 state)
        result (assess tested)
        claim (event-claim result writer-fence-id)
        success? (if negative? (not (:pass? result)) (:pass? result))]
    (println "contract-authority-current:"
             (cond
               negative? (if success? "negative-control PASS" "mutation slipped")
               (not success?) "FAIL"
               writer-fence-id (str "PASS (FENCE-CONDITIONAL " writer-fence-id ")")
               :else "PASS-CONTENT-ONLY (event-free unverified)")
             "assertion=the contract was generated from the current content of Holes.lean"
             (pr-str (assoc result :negative-control negative? :event-claim claim))
             "exit-convention=0-pass/1-fail")
    (System/exit (if success? 0 1))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
