#!/usr/bin/env bb
;; scalar_typing_check.bb -- U30's pin: every scalar the defect tally names
;; (row :c-cost-vs-distribution) is either typed :scalar-awaiting-density in
;; source or derived from a declared C. Both the WM and zaif halves are
;; enforced; zaif U15 moved its two named sites here from the pending set.
(require '[clojure.string :as str])
(def root (str (System/getProperty "user.home") "/code"))
(def enforced ;; [file token] -- both the marker and the named token must be present
  [[(str root "/futon2/scripts/futon2/report/war_machine.clj") "roi-map-for-missions"]
   [(str root "/futon2/scripts/futon2/report/wm_regulator_sweep.clj") "sustainability"]
   [(str root "/futon3c/src/futon3c/agents/zaif_controller.clj") ":retrieve-eig-scale"]
   [(str root "/futon3c/src/futon3c/agents/zaif_controller.clj") "ask-value"]])
(def failures
  (for [[f token] enforced
        :let [s (try (slurp f) (catch Exception _ nil))]
        :when (or (nil? s)
                  (not (str/includes? s ":scalar-awaiting-density"))
                  (not (str/includes? s token)))]
    [f token]))
(if (seq failures)
  (do (doseq [[f token] failures] (println "MISSING marker or token:" f token))
      (System/exit 1))
  (println "scalar_typing_check: both halves OK --" (count enforced) "sites typed"))
