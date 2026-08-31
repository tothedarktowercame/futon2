#!/usr/bin/env bb
(ns belief-update-check
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.string :as str]))

(def mathlib-root (str (fs/normalize (fs/path (fs/cwd) "../mathlib4"))))
(def fixture (fs/path mathlib-root "DarkTower/WarMachine/BeliefUpdateFalsifier.lean"))

(defn lean! [path]
  (process/shell {:dir mathlib-root :continue true :out :string :err :string}
                 "lake" "env" "lean" (str path)))

(defn negative-control [kind]
  (let [source (slurp (str fixture))
        needle (case kind
                 :mean "example : ¬ beliefUpdate learningRate sensorNoiseFloor evidenceWeight\n    kernel prior observation precision prior := by"
                 :variance "example : ¬ beliefUpdate learningRate sensorNoiseFloor evidenceWeight\n    kernel prior observation precision unresponsiveVariance := by")
        mutant (str/replace source needle (str/replace needle "¬ beliefUpdate" "beliefUpdate"))
        tmp (fs/create-temp-file {:dir (fs/parent fixture)
                                  :prefix "BeliefUpdateMutation-" :suffix ".lean"})]
    (try
      (if (= source mutant)
        (do (binding [*out* *err*] (println "belief-update-check: mutation target absent")) 1)
        (do
          (spit (str tmp) mutant)
          (let [result (lean! tmp)]
            (if (zero? (:exit result))
              (do (binding [*out* *err*] (println "belief-update-check: mutation slipped")) 2)
              (do (println (str "belief-update-check: negative-control PASS ("
                                (case kind :mean "inert update" :variance "unresponsive variance")
                                " rejected)")) 0)))))
      (finally (fs/delete-if-exists tmp)))))

(defn -main [& args]
  (let [variance-negative? (some #{"--negative-variance"} args)
        mean-negative? (some #{"--negative" "--negative-control"} args)
        exit (if (or variance-negative? mean-negative?)
               (negative-control (if variance-negative? :variance :mean))
               (let [result (lean! fixture)]
                 (if (zero? (:exit result))
                   (do (println "belief-update-check: PASS") 0)
                   (do (binding [*out* *err*]
                         (println "belief-update-check: FAIL")
                         (println (:err result)))
                       1))))]
    (System/exit exit)))

(apply -main *command-line-args*)
