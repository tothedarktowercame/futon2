#!/usr/bin/env bb
(ns checks.hyper-edge-exemplar-check
  (:require [clojure.edn :as edn]))

(def default-schema
  "/home/joe/code/p4ng/empirics-futon/hyper-edge-schema.edn")

(defn- missing-keys [required x]
  (remove #(contains? x %) required))

(defn validate-instance [schema instance]
  (let [ports (:ports instance)
        errors
        (vec
         (concat
          (for [k (missing-keys (:required/edge schema) instance)]
            {:error :missing-edge-field :field k})
          (when-not (seq (:members instance)) [{:error :members-empty}])
          (when-not (= #{:in :out} (set (map :direction ports)))
            [{:error :both-directions-required :actual (set (map :direction ports))}])
          (mapcat
           (fn [port]
             (concat
              (for [k (missing-keys (:required/port schema) port)]
                {:error :missing-port-field :port (:port/id port) :field k})
              (when-not (or (some? (:emits port)) (some? (:accepts port)))
                [{:error :port-has-no-type :port (:port/id port)}])))
           ports)
          (for [k (missing-keys (:required/semantics schema) (:semantics instance))]
            {:error :missing-semantics-field :field k})))
        freehand (count (filter #(= :freehand (:emitted-by %)) ports))]
    {:pass? (empty? errors) :edge/id (:edge/id instance)
     :ports (count ports) :freehand-ports freehand :errors errors}))

(defn- parse-args [args]
  (loop [xs args out {}]
    (if (empty? xs) out
        (if (= "--negative" (first xs))
          (recur (rest xs) (assoc out :negative? true))
          (recur (nnext xs) (assoc out (keyword (subs (first xs) 2)) (second xs)))))))

(defn -main [& args]
  ;; Default: validate EVERY instance in the schema file (a gate that defaults to
  ;; one exemplar isn't looking — found by R5 in the pairing run, 2026-08-31).
  ;; --edge :some/id selects one; --negative strips :emitted-by from the first
  ;; port of the selected (or first) instance and must fail.
  (let [{:keys [schema edge negative?]
         :or {schema default-schema}}
        (parse-args args)
        doc (edn/read-string (slurp schema))
        all (vec (:instances doc))
        selected (if edge
                   (let [eid (keyword (subs edge 1))]
                     (filterv #(= eid (:edge/id %)) all))
                   all)
        selected (if (and edge (empty? selected))
                   [{:edge/id (keyword (subs edge 1)) ::missing true}]
                   selected)
        selected (if negative?
                   [(update-in (first selected) [:ports 0] dissoc :emitted-by)]
                   selected)
        results (mapv (fn [inst]
                        (if (::missing inst)
                          {:pass? false :errors [{:error :unknown-edge :edge (:edge/id inst)}]}
                          (validate-instance doc inst)))
                      selected)]
    (doseq [r results] (println (pr-str r)))
    (println (str "hyper-edge-check: instances=" (count results)
                  " passed=" (count (filter :pass? results))))
    (System/exit (if (every? :pass? results) 0 1))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
