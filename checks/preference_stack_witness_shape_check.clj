#!/usr/bin/env bb
(ns checks.preference-stack-witness-shape-check
  (:require [clojure.edn :as edn]
            [clojure.string :as str]))

(def witness-path "holes/labs/wm-contract/PreferenceStackWitness.edn")
(def expected-layer-ids
  #{:floor :capability-zone-load :live-goal-outcomes :c-vector-overlays :habit-prior})
(def required-top-level
  #{:witness/type :witness/version :recorded-at :producer :input :consumer :preference-stack})
(def required-layer-fields #{:layer/id :source :author :basis :folded? :site})

(defn- nonblank? [x] (and (string? x) (not (str/blank? x))))

(defn validate [w]
  (let [stack (:preference-stack w)
        ids (mapv :layer/id stack)
        failures
        (cond-> []
          (not= required-top-level (set (keys w))) (conj :top-level-shape)
          (not= :PreferenceStackWitness (:witness/type w)) (conj :witness-type)
          (not= 1 (:witness/version w)) (conj :witness-version)
          (not (nonblank? (:recorded-at w))) (conj :recorded-at)
          (not= 'futon2.aif.efe/compute-efe (get-in w [:producer :function])) (conj :producer)
          (not= expected-layer-ids (set ids)) (conj :layer-population)
          (not= (count ids) (count (distinct ids))) (conj :duplicate-layer)
          (not= 5 (count stack)) (conj :layer-count)
          (not= [:habit-prior] (mapv :layer/id (filter #(false? (:folded? %)) stack)))
          (conj :unfolded-layer)
          (some #(not= required-layer-fields (set (keys %))) stack) (conj :layer-shape)
          (some #(or (not (nonblank? (:author %)))
                     (not (nonblank? (:basis %)))
                     (not (nonblank? (:site %)))) stack)
          (conj :empty-provenance))]
    {:pass? (empty? failures)
     :failures failures
     :layers (count stack)}))

(defn -main [& args]
  (let [negative? (some #{"--negative"} args)
        witness (edn/read-string (slurp witness-path))
        candidate (if negative?
                    (update witness :preference-stack
                            #(filterv (fn [layer] (not= :live-goal-outcomes (:layer/id layer))) %))
                    witness)
        report (validate candidate)]
    (cond
      (and negative? (not (:pass? report)))
      (do (println "preference-stack-witness-shape-check: negative-control PASS (missing-layer mutation rejected) exit-convention=0-pass/1-fail/2-mutation-slipped"
                   (pr-str report))
          (System/exit 0))

      negative?
      (do (println "preference-stack-witness-shape-check: FAIL (mutation slipped) exit-convention=0-pass/1-fail/2-mutation-slipped"
                   (pr-str report))
          (System/exit 2))

      (:pass? report)
      (println "preference-stack-witness-shape-check: PASS exit-convention=0-pass/1-fail/2-mutation-slipped"
               (pr-str report))

      :else
      (do (println "preference-stack-witness-shape-check: FAIL exit-convention=0-pass/1-fail/2-mutation-slipped"
                   (pr-str report))
          (System/exit 1)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
