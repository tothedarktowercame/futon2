(ns futon2.report.focus-discovery-test
  "Checks the offline focus counterexample; no production preference changes."
  (:require [clojure.test :refer [deftest is]]))

(binding [*out* (java.io.StringWriter.)]
  (load-file "holes/labs/wm-contract/runs/fixlist-2026-09-21/improve-7-evidence/improve_7_replay.clj"))

(deftest focus-domain-counterexamples
  (let [[latest earlier] @(ns-resolve 'improve-7-replay 'results)
        case-of (fn [r k] (first (filter #(= k (:case %)) (:cases r))))
        literal (case-of latest :literal-60-40-zero)
        global (case-of latest :global-split-on-token-powerset-counterexample)
        epsilon (case-of earlier :epsilon-nothing-zero)]
    (is (= {:adjacent 0.4} (:unrepresented-class-mass literal)))
    (is (= "M-aif-policy-conditioned-eig" (get-in literal [:choice :action :target])))
    (is (= :no-admissible-candidate (get-in (case-of earlier :literal-60-40-zero) [:selection-refusal :kind])))
    (is (= "M-expressions-of-interest" (get-in epsilon [:choice :action :target])))
    (is (= :habit (:policy-decided-by epsilon)))
    (is (= "M-wm-08-external-f2" (get-in global [:choice :action :target])))
    (is (= #{:G} (:action-decided-by global)))
    (is (> 1 (:odds (first (:odds-at-initial-context global)))))
    (doseq [r [latest earlier] c (:cases r) :when (nil? (:selection-refusal c))]
      (is (< (abs (- 1 (reduce + (map :posterior (:rows c))))) 1e-8)))))
