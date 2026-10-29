(ns futon2.aif.selected-mission-carrier-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.selected-mission-contract :as contract]
            [futon2.aif.wm.family-selection :as family]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.wm.cascade-decision :as decision]
            [futon2.aif.wm.executable-cascade :as executable]
            [futon2.aif.wm.authored.req-aab759a8 :as authored]
            [futon2.aif.close-retention :as retention]
            [futon2.aif.d-predecessor-task-authority :as dispatch]
            [futon2.aif.mission-observation :as observation]
            [futon2.aif.selected-want-outcome :as outcome]
            [futon2.aif.reading-policy-class-free-energy :as class-f]
            [futon2.aif.reading-policy-class-history-index :as history]
            [futon2.test-support.runner-fixture :as fixture])
  (:import [java.time Instant] [java.util UUID] [java.nio.file Files]))

(use-fixtures :once fixture/with-hermetic-traces)
(def fixture-root "test/fixtures/selected-mission-carrier/")
(def target "M-reachable-from-boot")
(def repo "/home/joe/code/futon3c")
(def before "415e94ba068e2b98db283220ca4efcf741b62c67")
(def after "b7ca069d599778f7c55488ea2514842c1c87b54f")
(defn frozen [name] (slurp (io/resource (str "fixtures/selected-mission-carrier/" name))))

(deftest frozen-selection-construction-and-revision-observation
  (let [root (str (Files/createTempDirectory "selected-mission-" (make-array java.nio.file.attribute.FileAttribute 0)))
        _ (history/backfill! root (str root "/reading-policy-class-history-index.ednl"))
        c (contract/source-contract "/home/joe/code" target
                                    (str repo "/holes/missions/" target ".md")
                                    (frozen "mission-before.md"))
        policies (mapv #(assoc % :mission-want-contract c)
                       (edn/read-string (frozen "policies.edn")))
        retained (edn/read-string (frozen "entry.edn"))
        selection (with-redefs [class-f/history-context #(history/history root % 100)]
                    (family/select-over-families
                     [{:status :computed :target-id target :policies policies}]
                     {:beta 1 :enactment-fold nil :novelty-inputs {}}))
        ranked (:ranked selection)
        selected (first (filter #(= (get-in selection [:decision :action :id])
                                    (get-in % [:action :id])) ranked))
        entry (assoc retained :action (merge (:action selected)
                                             (select-keys (:action retained) [:cascade-digest])))
        execution (decision/execute-selected-cascade!
                   entry {:executable-cascade-root (str root "/cascades")
                          :execution-journal-root (str root "/journal")
                          :cascade-operational-authoring
                          (constantly {:attribution {:author :wm-author :reviewer :wm-reviewer
                                                     :authority :wm/reviewed-resume}
                                       :application-order [] :clauses authored/clauses})})]
    (is (= :selected (:status selection)))
    (is (= (get-in retained [:action :id]) (get-in selected [:action :id])))
    (is (= 3 (count ranked)))
    (is (seq (:want c)))
    (is (= (:want c) (get-in selected [:action :want])))
    (is (= :cascade-ready (:status execution)))
    (let [construction (runner/construct-for-decision
                        (assoc entry :compiled-cascade (executable/load-record (str root "/cascades/" (:compiled-digest execution) ".edn"))))
          action (:selected-action construction)
          occurrence (retention/mint-occurrence {:run-id "frozen-b4e9ea0e"
                                                :cohort-id "frozen" :attempt-id "attempt-001"
                                                :selected-action action :now #(Instant/now)
                                                :uuid-fn #(UUID/randomUUID)})
          d (dispatch/capture {:occurrence occurrence :selected-action action
                               :declaration-reads [] :before {:repo repo :head before}})
          baseline (observation/capture-before {:dispatch d :repository repo :revision before})
          rows (observation/capture-after {:dispatch d :repository repo :revision after})
          projected (observation/project {:baseline baseline :dispatch d :repository repo
                                          :after-revision after :after-rows rows})
          receipt (outcome/receipt {:selected-action action :mission-observation projected})]
      (is (= :compiled-cascade (:construction-kind construction)))
      (is (= (:want c) (:want action)))
      (is (= (:observation-locators c) (:observation-locators action)))
      (is (= :captured (:status baseline)))
      (is (= :admitted (:status projected)))
      (is (= :verified (:status receipt)))
      (is (seq (:outcomes receipt)))
      (prn :witness {:source (:mission-want-source action) :want (:want action)
                     :construction (:construction-kind construction)
                     :baseline-status (:status baseline)
                     :before-observed (mapv :observed (:rows baseline))
                     :receipt receipt}))))

(deftest empty-mission-refuses-and-nonmission-is-unchanged
  (is (= :selected-want-contract-missing
         (try (contract/require-before-author! {:target target :want []})
              nil (catch clojure.lang.ExceptionInfo e (:failure-kind (ex-data e))))))
  (doseq [action [{:target "E-example"} {:target "T-example"}
                  {:target target :type :repair-machine-failure}]]
    (is (= action (contract/require-before-author! action)))))
