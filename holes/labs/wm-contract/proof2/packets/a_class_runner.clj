(ns a-class-runner
  "A-CLASS runner: execute the five observation class checks over
  a-class-cases.edn, join with the independently derived ground truth
  (a-class-truth.json, written by a_class_groundtruth.py), tabulate
  false-pass/false-fail per class with the A-S arithmetic
  (futon2.aif.check-error-rates/measured-rates), and write
  a-class-results.edn.

  Read-only against the repos and the registry. A_CLASS_ROOT overrides the
  repository root for the scratch dry run (twin checkout holding this
  packet's fixtures); A_CLASS_AGENCY overrides the evidence API base."
  (:require [clojure.edn :as edn]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.pprint]
            [clojure.java.shell]
            [clojure.string]
            [futon2.aif.observation-checks :as oc]
            [futon2.aif.check-error-rates :as cer]))

(def here (str (System/getProperty "user.dir")
               "/holes/labs/wm-contract/proof2/packets/"))

(when-let [root (System/getenv "A_CLASS_ROOT")]
  (alter-var-root #'oc/repo-root (constantly root)))

(def cases (:cases (edn/read-string (slurp (str here "a-class-cases.edn")))))
(def truth-rows (json/read-str (slurp (str here "a-class-truth.json")) :key-fn keyword))
(def truth-by-id (into {} (map (fn [r] [(:id r) r])) truth-rows))

(defn run-case [{:keys [id class locator] :as case}]
  (let [check-fn (get oc/checks (keyword class))
        result (check-fn locator)
        t (get truth-by-id (name id))
        derived (:derived-truth t)
        refused? (contains? result :status)
        observed (when-not refused? (:observed result))]
    {:id id :class class :near-miss? (:near-miss? case) :note (:note case)
     :locator locator
     :intended-truth (:truth case) :derived-truth derived :truth-how (:truth-how t)
     :expected (some-> (:expect case) keyword)
     :outcome (if refused? :refusal :observation)
     :refusal-kind (when refused? (:kind result))
     :observed observed
     :agree (when (and (some? observed) (some? derived)) (= observed derived))
     :check-evidence (select-keys (:evidence result)
                                  [:path :decl :resolved-sha :file-present :entry
                                   :contract-found :clojure-loci :witness-present
                                   :warrant-id :recorded-namespace :postcheck
                                   :run-counts :moved-paths :reason])}))

(def results (mapv run-case cases))

;; ------------------------------------------------------------ tabulation
;; Ledger rows for the A-S arithmetic: one per case with a boolean derived
;; truth AND an observation outcome. Refusals are reported, not rated.
(def ledger-rows
  (vec (keep (fn [r]
               (when (and (= :observation (:outcome r))
                          (contains? #{true false} (:derived-truth r)))
                 {:id (:id r) :kind (keyword (:class r))
                  :truth (:derived-truth r) :passed (:observed r)
                  :truth-kind :constructed-bad-case}))
             results)))

(def rates (cer/measured-rates {:schema cer/ledger-schema :rows ledger-rows}))

(def disagreements
  (vec (filter #(and (= :observation (:outcome %)) (false? (:agree %))) results)))

(def refusals (vec (filter #(= :refusal (:outcome %)) results)))
(def unexpected-refusals (vec (remove #(= :refusal (:expected %)) refusals)))
(def missing-refusals (vec (remove #(= :refusal (:outcome %))
                                   (filter #(= :refusal (:expected %)) results))))

(def summary
  {:schema :proof2/a-class-results-v1
   :written-by "kimi-4"
   :repo-root oc/repo-root
   :futon2-head (clojure.string/trim (:out (clojure.java.shell/sh "git" "-C" (str oc/repo-root "/futon2") "rev-parse" "HEAD")))
   :n-cases (count results)
   :per-class (into {} (map (fn [cls]
                              (let [rs (filter #(= cls (:class %)) results)
                                    obs (filter #(= :observation (:outcome %)) rs)]
                                [cls {:n (count rs)
                                      :observed-outcomes (count obs)
                                      :refusals (count (filter #(= :refusal (:outcome %)) rs))
                                      :true (count (filter :derived-truth obs))
                                      :false (count (remove :derived-truth obs))
                                      :false-pass (count (filter #(and (not (:derived-truth %)) (:observed %)) obs))
                                      :false-fail (count (filter #(and (:derived-truth %) (not (:observed %))) obs))
                                      :near-miss-false (count (filter #(and (:near-miss? %) (not (:derived-truth %))) rs))}]))
                            ["C3" "C4" "C5" "C6" "C8"]))
   :rates (into {} (filter (fn [[k _]] (keyword? k))) rates)
   :disagreements disagreements
   :unexpected-refusals unexpected-refusals
   :missing-refusals missing-refusals
   :results results})

(spit (str here "a-class-results.edn") (with-out-str (clojure.pprint/pprint summary)))

(println "== per-class ==")
(doseq [[cls s] (:per-class summary)]
  (println cls s))
(println "== rates ==")
(doseq [[k v] (:rates summary)]
  (println k (select-keys v [:status :n :n-true :n-false :false-pass :false-fail :fp-rate :fn-rate :fp-interval :fn-interval])))
(println "== disagreements ==" (count disagreements))
(doseq [d disagreements] (println " " (:id d) "truth" (:derived-truth d) "observed" (:observed d) "-" (:truth-how d)))
(println "== unexpected refusals ==" (map :id unexpected-refusals))
(println "== missing expected refusals ==" (map :id missing-refusals))
