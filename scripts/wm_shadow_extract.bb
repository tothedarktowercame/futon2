#!/usr/bin/env bb
;; wm_shadow_extract.bb — extract one WM trace step into a JAX-friendly JSON.
;;
;; Part of the WM differentiable-shadow pilot (claude-6 owns harness; claude-3
;; owns WM/aif2 semantics; parallel-shadow, run superpod-gated). Lightweight EDN
;; read — NOT the heavy JAX run. Picks the max-belief-movement step as the
;; replay target and dumps mu-pre / mu-post / observation / precision as numeric
;; arrays for wm_shadow_pilot.py.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[cheshire.core :as json])

(def trace-file "data/wm-trace/wm-trace-2026-05-24.edn")
(def out-file "data/wm-trace/wm-shadow-step.json")

;; 7 belief statuses (axes preserved — relax coordinates, not ontology)
(def statuses [:spawned :refined :strengthened :addressed :falsified :foreclosed :reopened])
;; 8 channels with an R3a likelihood model (belief.clj:565 channels-with-likelihood)
(def channels [:annotation-health :sorry-count-norm :mission-health :active-repo-ratio
               :support-coverage :attack-coverage :coupling-density :ticks-firing-ratio])

(defn read-all [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [v []] (let [x (edn/read {:eof ::eof} r)]
                   (if (= x ::eof) v (recur (conj v x)))))))

(defn agg-delta [m]
  (let [pre (:mu-pre m) post (:mu-post m)]
    (reduce + (for [e (keys pre) k statuses]
                (Math/abs (- (double (get-in post [e k] 0.0))
                             (double (get-in pre [e k] 0.0))))))))

(let [forms (read-all trace-file)
      idx   (apply max-key #(agg-delta (nth forms %)) (range (count forms)))
      m     (nth forms idx)
      pre   (:mu-pre m)
      post  (:mu-post m)
      ents  (vec (keys pre))
      row   (fn [mu e] (mapv #(double (get-in mu [e %] 0.0)) statuses))
      out   {:source        trace-file
             :step-index    idx
             :timestamp     (:timestamp m)
             :statuses      (mapv name statuses)
             :channels      (mapv name channels)
             :entities      ents
             :mu_pre        (mapv #(row pre %) ents)
             :mu_post       (mapv #(row post %) ents)
             :observation   (mapv #(double (get-in m [:observation %] 0.0)) channels)
             ;; frozen R7 precision = accuracy-term channel weighting (claude-3 ruling ii)
             :precision     (mapv #(double (or (get-in m [:precision-state % :precision]) 1.0)) channels)}]
  (spit out-file (json/generate-string out))
  (println "step-index" idx "ts" (:timestamp m)
           "entities" (count ents) "-> " out-file))
