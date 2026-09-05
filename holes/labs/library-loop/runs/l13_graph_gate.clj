#!/usr/bin/env bb
;; L13 GRAPH-CERTIFICATE GATE v0 (row L13, from the L4 strawman's costing).
;;
;; Usage:
;;   l13_graph_gate.clj [GRAPH_EDN] [CASCADE_EDN] [READING]
;;   GRAPH_EDN  default runs/L12-census-graph.edn (the current regenerated
;;              L1-graph artifact; any l1_census graph works)
;;   CASCADE_EDN an EDN file containing a vector of pattern-id strings
;;   READING    one of down-problems (default), down-problems+wr,
;;              up-problems, up-problems+wr (the four L1 readings; direction
;;              choice beyond the default is the F7/F8 merge's, not this gate's)
;;
;; Refuses (exit 1, listing every unreachable pattern) any cascade pattern
;; not @why-reachable from a problem-stating node under the chosen reading.
;; Passes (exit 0) with a one-line summary. Bare exits, no wiring: this gate
;; is offered, not imposed — wiring into cascade construction is the F7/F8
;; merge. Per the strawman's own cost table, hard refusal today refuses most
;; of every real cascade; v0 exists so the refusal shape can be inspected.
(ns l13-graph-gate
  (:require [clojure.edn :as edn]
            [clojure.string :as str]))

(def default-graph "runs/L12-census-graph.edn")

(defn reach [graph reading]
  (let [{nodes :nodes edges :edges} graph
        ids (set nodes)
        roots (case reading
                (:down-problems :down-problems+wr :up-problems :up-problems+wr)
                (set (filter #(str/starts-with? % "problems/") nodes)))
        roots (if (str/ends-with? (name reading) "+wr")
                (into roots (filter #(str/starts-with? % "war-room/wr-") nodes))
                roots)
        why-edges (filter #(and (= (:kind %) :why) (:resolved %)) edges)
        adj-up (reduce (fn [m e] (update m (:from e) (fnil conj []) (:to e))) {} why-edges)
        adj-down (reduce (fn [m e] (update m (:to e) (fnil conj []) (:from e))) {} why-edges)
        adj (if (str/starts-with? (name reading) "up") adj-up adj-down)]
    (loop [seen #{} frontier (vec roots)]
      (if (empty? frontier)
        seen
        (let [n (peek frontier)]
          (if (contains? seen n)
            (recur seen (pop frontier))
            (recur (conj seen n)
                   (into (pop frontier) (filter ids (adj n))))))))))

(defn -main [& args]
  (let [[graph-path cascade-path reading-s] (concat args [nil nil nil])
        graph-path (or graph-path default-graph)
        cascade-path (or cascade-path "runs/l13-negative-control.edn")
        reading (keyword (or reading-s "down-problems"))
        _ (assert (#{"down-problems" "down-problems+wr" "up-problems" "up-problems+wr"}
                   (name reading))
                  (str "unknown reading " reading))
        graph (edn/read-string (slurp graph-path))
        cascade (edn/read-string (slurp cascade-path))
        ids (set (:nodes graph))
        unknown (vec (remove ids cascade))
        reach-set (reach graph reading)
        unreachable (vec (remove reach-set (remove (set unknown) cascade)))]
    (if (or (seq unknown) (seq unreachable))
      (do (when (seq unknown)
            (println "REFUSED: unknown pattern ids (not in graph):")
            (doseq [u unknown] (println "  " u)))
          (when (seq unreachable)
            (println "REFUSED: patterns not @why-reachable from a problem node"
                     (str "(" (name reading) "):"))
            (doseq [u unreachable] (println "  " u)))
          (println (format "gate: %d/%d refused" (+ (count unknown) (count unreachable))
                           (count cascade)))
          (System/exit 1))
      (do (println (format "gate: PASS, %d/%d reachable (%s)"
                           (count cascade) (count cascade) (name reading)))
          (System/exit 0)))))

(apply -main *command-line-args*)
