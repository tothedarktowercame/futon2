#!/usr/bin/env bb
(require '[babashka.classpath :as cp])
(cp/add-classpath "src")

(ns checks.r19-stack-witness
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [futon2.aif.belief :as belief]
            [futon2.aif.efe :as efe]))

(def reference-path "holes/labs/wm-contract/R19-preference-stack.edn")

(def expected-ids
  #{:floor :capability-zone-load :live-goal-outcomes :c-vector-overlays :habit-prior})

;; Same minimal no-op dry-run shape used by c_vector_test.clj:76-79.
(def dry-run-fixture
  {:state {:observation {} :belief (belief/initial-belief-state [:m1])}
   :action {:type :no-op}
   :opts {:goal-outcome-entries []}})

(defn- nonblank? [x]
  (and (string? x) (not (str/blank? x))))

(defn- fail! [finding data]
  (throw (ex-info (str "r19-stack-witness: FAIL " (name finding))
                  (assoc data :finding finding))))

(defn- reference-folded []
  (->> (:layers (edn/read-string (slurp reference-path)))
       (filter :folded?)
       (map (juxt :layer/id #(select-keys % [:source :author :basis])))
       (into {})))

(defn- check-stack! [stack]
  (let [by-id (into {} (map (juxt :layer/id identity)) stack)
        ids (set (keys by-id))
        missing (first (sort (remove ids expected-ids)))
        unexpected (first (sort (remove expected-ids ids)))
        unfolded (filterv #(false? (:folded? %)) stack)]
    (when missing (fail! :missing-layer {:layer missing}))
    (when unexpected (fail! :unexpected-layer {:layer unexpected}))
    (when-not (= 5 (count stack))
      (fail! :layer-count-mismatch {:expected 5 :actual (count stack)}))
    (when-not (= [:habit-prior] (mapv :layer/id unfolded))
      (fail! :unfolded-layer-mismatch {:actual (mapv :layer/id unfolded)}))
    (when-not (str/includes? (:site (first unfolded)) "counterfactual-only")
      (fail! :habit-prior-site-invalid {:site (:site (first unfolded))}))
    (doseq [layer stack, field [:author :basis]]
      (when-not (nonblank? (field layer))
        (fail! :layer-provenance-empty {:layer (:layer/id layer) :field field})))
    (let [expected (reference-folded)
          actual (->> stack (filter :folded?)
                      (map (juxt :layer/id #(select-keys % [:source :author :basis])))
                      (into {}))]
      (when-not (= expected actual)
        (fail! :folded-reference-mismatch {:expected expected :actual actual})))
    {:layers (count stack) :unfolded (count unfolded)}))

(defn -main [& args]
  (let [negative? (some #{"--negative"} args)]
   (try
    (let [{:keys [state action opts]} dry-run-fixture
          result (efe/compute-efe state action opts)
          stack (:preference-stack result)
          stack (if negative?
                  (filterv #(not= :habit-prior (:layer/id %)) stack)
                  stack)
          {:keys [layers unfolded]} (check-stack! stack)]
      (if negative?
        (do (println "r19-stack-witness: FAIL negative mutation passed exit-convention=0-pass/1-fail")
            (System/exit 2))
        (println (str "r19-stack-witness: PASS layers=" layers " unfolded=" unfolded
                      " exit-convention=0-pass/1-fail"))))
    (catch Exception e
      (if negative?
        (do (println (str "r19-stack-witness: PASS negative control rejected finding=" (.getMessage e)
                          " exit-convention=0-pass/1-fail"))
            (System/exit 0))
        (do (println (str (.getMessage e) " exit-convention=0-pass/1-fail"))
            (System/exit 1)))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
