(ns futon2.aif.preference-family-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.g-term-decomposition :as g]
            [futon2.aif.live-c :as lc]
            [futon2.aif.efe :as efe]
            [futon2.aif.cascade-sources :as sources]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.wm.cascade-decision :as wm-cd]))

(def schedule {:placement {:value :terminal :status :declared}
               :elsewhere {:value :uniform-over-non-ruled-zero :status :declared}})
(def spec {:want #{:x} :evidence #{} :zeroed #{} :lam 1 :mu 0})
(defn evaluate [s]
  (m/horizon-g-sparse-cert {:rates {:x {:false-neg 0 :false-pos 0}}
                            :q0 {#{} 1} :precedence-fn (constantly [])
                            :horizon 2 :spec s :universe #{:x}}))

(deftest family-and-constant-controls-read-consumed-distributions
  (let [varying (evaluate (assoc spec :c-schedule schedule))
        constant (evaluate spec)
        cv (get-in varying [:certificate :consumed-g :C])
        cc (get-in constant [:certificate :consumed-g :C])
        v (g/verdict :C (assoc cv :form :constant-spec))
        c (g/verdict :C (assoc cc :form :step-indexed))]
    (is (= :varies-across-horizon (:reason v)))
    (is (= :non-degenerate (:verdict v)))
    (is (= 2 (:C-steps-count v)))
    (is (= :constant-across-horizon (:reason c)))
    (is (= :degenerate (:verdict c)))
    (is (= 2 (:C-steps-count c)))
    (is (not= (:g varying) (:g constant)))
    (is (= {} (get-in cv [:steps 0 :distribution :weights])))
    (is (= {:x 1} (get-in cv [:steps 1 :distribution :weights])))
    (is (= :missing (:status (g/verdict :C {:form :constant-spec}))))
    (println "C-FAMILY-CONTROLS" (pr-str {:varying v :constant c}))))

(deftest exclusions-remain-zero-and-equivalent-distributions-remain-constant
  (let [s (assoc spec :zeroed #{#{}} :c-schedule schedule)
        members (mapv #(m/preference-member s #{:x} 2 %) [1 2])]
    (doseq [member members]
      (is (= ##-Inf ((m/member-log-probability member) #{}))))
    (is (m/same-preference-distribution? (first members) (second members)))
    (is (= :constant-across-horizon
           (:reason (g/verdict :C {:steps (mapv #(hash-map :distribution %) members)}))))))

(deftest declarations-supply-schedule-through-production-merge
  (let [loaded (sources/load-declared)
        production-sources (assoc (sources/with-context-fn loaded) :horizon-steps 2)
        all-assembled (problems/assemble {:targets (keys (:universes loaded))
                                          :sources production-sources})
        assembled-problems (:problems all-assembled)
        scoped-targets (for [[target universe] (:universes loaded)
                             :when (and (= schedule (get-in loaded [:preference-schedules target]))
                                        (every? boolean? (vals universe)))]
                         target)
        assembled (problems/assemble {:targets scoped-targets :sources production-sources})
        scoped (:problems assembled)
        schedules-by-target (into {}
                                  (map (juxt :target
                                             #(get-in % [:cascade-problem :c-schedule])))
                                  assembled-problems)
        declared-targets (set (for [[target s] schedules-by-target :when (= schedule s)] target))
        differing-targets (set (for [[target s] schedules-by-target :when (not= schedule s)] target))
        ;; The refusal case is built here, so it does not depend on which
        ;; declarations happen to be open when the test runs.
        other-schedule (assoc-in schedule [:placement :value] :every-step)
        mixed (conj (vec scoped)
                    (assoc-in (first scoped) [:cascade-problem :c-schedule] other-schedule))
        full-refusal (try (lc/family-schedule mixed) nil
                          (catch clojure.lang.ExceptionInfo e
                            {:message (.getMessage e) :data (ex-data e)}))
        declared (lc/family-schedule scoped)
        pair ["M-expressions-of-interest" :change-authored-and-bound]
        live (lc/cascade-spec {:want #{:closed/M-expressions-of-interest}
                              :weights {:closed/M-expressions-of-interest 1}}
                             #{pair} #{pair} (lc/family-scales scoped) declared)
        merged (wm-cd/merge-live-cascade-spec #{pair} live)
        ranked (efe/rank-actions {:cascade-belief {#{} 1}}
                                 [{:kind :cascade-candidate :id :probe :precedence []}]
                                 {:horizon-steps 2 :cascade-spec merged})
        verdict (g/verdict :C (get-in ranked [0 :certificate :consumed-g :C]))]
    (is (= :varies-across-horizon (:reason verdict)))
    (is (= 2 (:C-steps-count verdict)))
    (is (empty? (:refusals assembled)))
    (is (= schedule declared (:c-schedule merged)))
    (is (= "incompatible preference schedules" (:message full-refusal)))
    (is (= :incommensurable-family (get-in full-refusal [:data :kind])))
    (is (seq scoped) "at least one loaded declaration carries the terminal schedule")
    (is (= #{schedule other-schedule}
           (set (get-in full-refusal [:data :preference-schedules]))))
    (is (every? #(= schedule (get-in % [:cascade-problem :cascade-spec :c-schedule]))
                scoped))
    (is (= :invalid-preference-schedule
           (try (lc/preference-schedule {:c-schedule (assoc-in schedule [:placement :value] :random)}) nil
                (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))
    (println "PREFERENCE-SCHEDULE-FAMILY"
             (pr-str {:declared-targets declared-targets
                      :differing-targets differing-targets
                      :refusal full-refusal}))))
