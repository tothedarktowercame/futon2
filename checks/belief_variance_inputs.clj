#!/usr/bin/env bb
(ns checks.belief-variance-inputs
  (:require [clojure.edn :as edn]))

(def default-path "holes/labs/wm-contract/belief-variance-inputs.edn")

(defn valid? [x]
  (let [alpha (get-in x [:ema-rate :alpha])
        floor (get-in x [:sensor-noise-floor :default])
        mapping (get-in x [:evidence-class-precision :mapping])
        independent (get-in mapping [:independent :multiplier])
        self (get-in mapping [:self :multiplier])
        unknown (:unknown mapping)]
    (and (= :C31-belief-variance-update (:consumer x))
         (number? alpha) (< 0.0 alpha 1.0)
         (= [2 21] (get-in x [:ema-rate :alpha-rational]))
         (number? floor) (pos? floor)
         (= 1.0 independent)
         (number? self) (< 0.0 self independent)
         (= :absent (:variant unknown))
         (= :pass-through-with-loud-absence (:consumer-action unknown))
         (not (contains? unknown :multiplier)))))

(defn -main [& args]
  (let [negative? (some #{"--negative"} args)
        path (or (some #(when-not (= "--negative" %) %) args) default-path)
        value (edn/read-string (slurp path))
        candidate (if negative?
                    (assoc-in value [:evidence-class-precision :mapping :self :multiplier] 1.0)
                    value)
        accepted? (valid? candidate)]
    (if negative?
      (if accepted?
        (do (binding [*out* *err*]
              (println "belief-variance-inputs: mutation slipped"))
            (System/exit 2))
        (println "belief-variance-inputs: negative-control PASS (self/full collapse rejected)"))
      (if accepted?
        (println "belief-variance-inputs: PASS")
        (do (binding [*out* *err*] (println "belief-variance-inputs: FAIL"))
            (System/exit 1))))))

(apply -main *command-line-args*)
