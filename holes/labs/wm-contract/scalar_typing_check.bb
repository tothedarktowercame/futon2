#!/usr/bin/env bb
;; scalar_typing_check.bb -- U30's pin: every scalar the defect tally names
;; (row :c-cost-vs-distribution) is either typed :scalar-awaiting-density in
;; source or derived from a declared C. WM half enforced; zaif half reported
;; as pending until U15 (zaif board) lands, at which point its sites move
;; from :pending-zaif-half into the enforced set.
(require '[clojure.string :as str])
(def root (str (System/getProperty "user.home") "/code"))
(def enforced ;; [file token] -- both the marker and the named token must be present
  [[(str root "/futon2/scripts/futon2/report/war_machine.clj") "roi-map-for-missions"]
   [(str root "/futon2/scripts/futon2/report/wm_regulator_sweep.clj") "sustainability"]])
(def pending-zaif-half
  [[(str root "/futon3c/src/futon3c/agents/zaif_controller.clj") "arm constants + ask payoff (zaif U15)"]])
(def failures
  (for [[f token] enforced
        :let [s (try (slurp f) (catch Exception _ nil))]
        :when (or (nil? s)
                  (not (str/includes? s ":scalar-awaiting-density"))
                  (not (str/includes? s token)))]
    [f token]))
(doseq [[f note] pending-zaif-half
        :let [s (try (slurp f) (catch Exception _ ""))]]
  (println "pending-zaif-half:" f
           (if (str/includes? s ":scalar-awaiting-density") "TYPED (flip to enforced!)" (str "not yet typed -- " note))))
(if (seq failures)
  (do (doseq [[f token] failures] (println "MISSING marker or token:" f token))
      (System/exit 1))
  (println "scalar_typing_check: wm half OK --" (count enforced) "sites typed"))
