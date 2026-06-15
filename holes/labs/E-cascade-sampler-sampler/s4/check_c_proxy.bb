#!/usr/bin/env bb
;; exit-1 cross-check: Clojure score-buildout-c vs the Python port, on real
;; circumstance moves. Emits per-sample pairs; the Python side asserts |Δ|<1e-9.
(require '[clojure.edn :as edn]
         '[cheshire.core :as json]
         '[clojure.set :as set])
(defn move-token-set [move]
  (set (remove nil? [(:move/id move) (:move/class move) (:have move)
                     (:want move) (:advances-cap move) (:confidence move)])))
(defn jaccard [a b]
  (let [u (set/union a b)]
    (if (empty? u) 1.0 (/ (double (count (set/intersection a b))) (double (count u))))))
(defn score-c [policy]
  (let [t (reduce + 0.0 (map #(double (or (:score %) 0.0)) policy))
        ts (mapv move-token-set policy)
        ps (for [i (range (count ts)) j (range (inc i) (count ts))] (jaccard (ts i) (ts j)))
        h (if (seq ps) (/ (reduce + 0.0 (map #(* 4.0 % (- 1.0 %)) ps)) (count ps)) 1.0)]
    (* t h)))
(let [circs (edn/read-string (slurp "../circumstances-v0.edn"))
      samples (for [c circs
                    k [1 2 3 6]
                    :let [policy (vec (take k (:moves c)))]
                    :when (seq policy)]
                {:circumstance (name (:circumstance/id c))
                 :k k
                 :clj-c (score-c policy)})]
  (spit "c_crosscheck_clj.json" (json/generate-string (vec samples)))
  (println "wrote" (count samples) "clojure-side samples"))
