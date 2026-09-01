#!/usr/bin/env bb
(ns checks.exit-code-scope-check
  (:require [checks.wm-workspace-gate :as gate]
            [clojure.string :as str]))

(def makefile "Makefile")
(def bounded-launcher "scripts/run_workspace_gate_bounded.py")
(def status-program "scripts/wm_status_report.py")

(defn report-only-commands [commands]
  (filterv #(contains? (set (:expected-exits %)) 3) commands))

(defn command-path [{:keys [argv]}]
  (first (filter #(re-find #"\.(?:py|clj)$" %) argv)))

(defn findings
  ([commands files] (findings commands files false))
  ([commands files inject-direct?]
   (let [declared (report-only-commands commands)
         paths (mapv command-path declared)
         make-text (str (get files makefile "")
                        (when inject-direct?
                          (str "\nunsafe:\n\tpython3 " (first paths) " --report\n")))
         bounded-text (get files bounded-launcher "")
         status-text (get files status-program "")]
     (vec
      (concat
       (when (empty? declared)
         [{:reason :report-only-set-empty}])
       (for [command declared
             :when (not= #{0 3} (set (:expected-exits command)))]
         {:reason :invalid-report-only-exits :command (:name command)})
       (for [path paths :when (str/includes? make-text path)]
         {:reason :report-only-crosses-make :path path})
       (for [path paths :when (str/includes? bounded-text path)]
         {:reason :report-only-crosses-bounded-launcher :path path})
       (when-not (str/includes? status-text "DECISION-DUE-3")
         [{:reason :decision-due-exit-3-not-declared}]))))))

(defn inputs []
  {makefile (slurp makefile)
   bounded-launcher (slurp bounded-launcher)
   status-program (slurp status-program)})

(defn -main [& args]
  (let [negative? (some #{"--negative-control"} args)
        result (findings (gate/commands) (inputs) negative?)
        rejected? (seq result)]
    (println "exit-code-scope-check:"
             (pr-str {:pass? (if negative? (boolean rejected?) (not rejected?))
                      :mode (if negative? :control :positive)
                      :report-only (mapv :name (report-only-commands (gate/commands)))
                      :findings result
                      :scoped-collision {:exit 3
                                         :report-only :gate-local
                                         :decision-due :status-local}}))
    (System/exit (if negative?
                   (if rejected? 0 2)
                   (if rejected? 1 0)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
