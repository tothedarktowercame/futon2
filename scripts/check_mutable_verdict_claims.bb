#!/usr/bin/env bb
(ns scripts.check-mutable-verdict-claims
  (:require [clojure.edn :as edn]
            [clojure.set :as set]
            [scripts.mutable-verdict-population :as population]))

(def registry-path "checks/mutable-verdict-claims.edn")

(defn assess [registry population]
  (let [content (set (:content-shaped registry))
        event (set (keys (:event-shaped registry)))
        neither (set (keys (:neither registry)))
        declared (set/union content event neither)
        members (set (:members population))
        overlap (set/union (set/intersection content event)
                           (set/intersection content neither)
                           (set/intersection event neither))
        failures (cond-> []
                   (not= :mutable-verdict-claims/v1 (:schema registry))
                   (conj :wrong-registry-schema)
                   (not= (:population-schema registry) (:schema population))
                   (conj :wrong-population-schema)
                   (seq overlap) (conj :classification-overlap)
                   (seq (set/difference members declared)) (conj :undeclared-member)
                   (seq (set/difference declared members)) (conj :stale-declaration))]
    {:pass? (empty? failures)
     :population (count members)
     :content-shaped (count content)
     :event-shaped (count event)
     :neither (count neither)
     :undeclared (sort (set/difference members declared))
     :stale (sort (set/difference declared members))
     :overlap (sort overlap)
     :failures failures}))

(let [registry (edn/read-string (slurp registry-path))
      negative? (= ["--negative-control"] (vec *command-line-args*))
      tested (if negative? (update registry :content-shaped subvec 1) registry)
      result (assess tested (population/report))
      success? (if negative? (not (:pass? result)) (:pass? result))]
  (println "mutable-verdict-claims:"
           (cond (and negative? success?) "negative-control PASS (member missing and rejected)"
                 negative? "mutation slipped"
                 success? "PASS"
                 :else "FAIL")
           (pr-str result)
           "exit-convention=0-pass/1-fail/2-mutation-slipped")
  (System/exit (cond success? 0 negative? 2 :else 1)))
