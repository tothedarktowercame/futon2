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

(defn negative-control []
  (let [source (slurp (str fixture))
        needle "example : ¬ beliefUpdate learningRate kernel prior observation precision prior := by"
        mutant (str/replace source needle
                            "example : beliefUpdate learningRate kernel prior observation precision prior := by")
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
              (do (println "belief-update-check: negative-control PASS (inert update rejected)") 0)))))
      (finally (fs/delete-if-exists tmp)))))

(defn -main [& args]
  (let [negative? (some #{"--negative" "--negative-control"} args)
        exit (if negative?
               (negative-control)
               (let [result (lean! fixture)]
                 (if (zero? (:exit result))
                   (do (println "belief-update-check: PASS") 0)
                   (do (binding [*out* *err*]
                         (println "belief-update-check: FAIL")
                         (println (:err result)))
                       1))))]
    (System/exit exit)))

(apply -main *command-line-args*)
