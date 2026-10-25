(ns futon2.aif.injury-8467-repair-acceptance-test
  "Proposed genuine-repair acceptance for reopened injury 8467.

   This is not the historical task acceptance and does not assert task success.
   External author/reviewer replies used by the owner runner fixture are
   synthetic. Reader observations are real reads from fixture-owned Git
   revisions. The contract keeps mission acceptance and pattern learning in
   distinct domains."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.d-predecessor-task-authority-test :as d-fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.mission-observation :as mission]
            [futon2.aif.mission-observation-deferred-test :as deferred]
            [futon2.aif.selected-want-outcome :as selected]
            [futon2.aif.full-loop-runner-test :as owner]
            [futon2.test-support.runner-fixture :as runner-fixture]))

(use-fixtures :once hermetic/with-hermetic-stores
              runner-fixture/with-hermetic-traces)
(use-fixtures :each (var owner/with-field-desk-stub)
              (var owner/without-live-wm-status))

(def contract
  (edn/read-string
   (slurp (io/resource "resources/wm/injury8467/repair-acceptance.edn"))))

(def target (get-in contract [:retained-attempt :target]))
(def wants (get-in contract [:retained-attempt :selected-wants]))
(def locators
  (into {}
        ;; The retained locator names canonical repo futon5a.  The owner
        ;; fixture creates the same repository content under the transparent
        ;; test-only name "repo"; only that filesystem name is adapted.
        (map (fn [[[token] locator]]
               [[target token] (assoc locator :repo "repo")]))
        (:declared-locators contract)))
(def action
  {:kind :cascade-candidate
   :id "M-war-machine-wiring/policy-2629917c87a61978be8f"
   :target target
   :want wants
   :observation-locators locators
   :precedence []})

(defn- unchecked-mission []
  (str (str/join
        "\n"
        (map #(str/replace (:decl %) "- [x]" "- [ ]")
             (vals (:declared-locators contract))))
       "\n"))

(deftest retained-occurrence-and-domain-shape-are-exact
  (is (= :reopened (get-in contract [:current-occurrence :state])))
  (is (= 2 (get-in contract [:current-occurrence :attempt])))
  (is (= "56fd92d4f3c6914a0500b9483eb397c34d4593eb"
         (get-in contract [:attribution-only-repair :patch])))
  (is (= 6 (count wants)))
  (is (= 2 (count (get-in contract [:retained-attempt :pattern-predictions]))))
  (is (= [false false false false false false]
         (get-in contract [:retained-attempt :measurement-booleans])))
  (is (= :not-measured (get-in contract [:retained-attempt :before-evidence])))
  (is (= {:trial-count 27 :status :held :reason :observation-missing}
         (get-in contract [:retained-attempt :learning]))))

(deftest six-real-c4-false-results-are-untouched-not-missing
  (d-fixture/with-artifact
   {:target target
    :action action
    :universe (set (map #(vector target %) wants))
    :before-files {"holes/missions/M-war-machine-wiring.md" (unchecked-mission)}
    :locators (into {} (map (fn [[[token] locator]] [token locator]))
                    (:declared-locators contract))}
   (fn [{:keys [inputs repo commit]}]
     (let [dispatch (:dispatch inputs)
           before (get-in dispatch [:before :head])
           baseline (mission/capture-before
                     {:dispatch dispatch :repository (str repo) :revision before})
           after (mission/capture-after
                  {:dispatch dispatch :repository (str repo) :revision commit})
           projection (mission/project
                       {:baseline baseline :dispatch dispatch :repository (str repo)
                        :after-revision commit :after-rows after})
           pattern-comparison
           {:status :refused :reason :prediction-unavailable
            :tokens (mapv (fn [token]
                            {:token token
                             :observed {:status :missing
                                        :kind :measurement-unavailable}})
                          (get-in contract [:retained-attempt :pattern-predictions]))}
           accounting (selected/receipt
                       {:selected-action action
                        ;; Pattern comparison is deliberately independent and
                        ;; remains unable to observe the mission-hole rows.
                        :token-comparison pattern-comparison
                        :mission-observation projection})]
       (is (= :captured (:status baseline)))
       (is (= 6 (count (:rows baseline))))
       (is (every? false? (map :observed (:rows baseline))))
       (is (= :admitted (:status projection)))
       (is (= :verified (:status accounting)))
       (is (= #{:untouched} (set (map :classification (:outcomes accounting)))))
       (is (= :refused (:status pattern-comparison))
           "the separate pattern comparison remains held/refused")))))

(deftest actual-owner-hold-resume-contract
  ;; `scenario` runs two real run-opportunity! calls on one fixture Git repo.
  ;; Its Agency replies are explicitly synthetic; its C4 reads are real.
  (let [{:keys [first second origin close]} ((var deferred/scenario) :valid)
        validation (get-in second [:result :checkpoints :build :judgment :validation])]
    (is (= :incomplete (get-in first [:result :outcome])))
    (is (= "attempt-001" (:attempt-id origin)))
    (is (= :grounded-change (get-in second [:result :outcome])))
    (is (= '("feature-review") (sort (keys (:dispatch-requests second))))
        "a completed retained job does not grant another author send")
    (is (= :pass (get-in validation [:reviewer-falsifier :checks
                                     :selected-want-accounting :status])))
    (is (= :refused (get-in close [:payload :judgment
                                    :token-outcome-comparison :status]))
        "mission acceptance does not manufacture a pattern observation"))
  (doseq [sidecar [:missing :tampered]]
    (let [run ((var deferred/scenario) sidecar)]
      (is (= :incomplete (get-in run [:second :result :outcome])))
      (is (empty? (get-in run [:second :dispatch-requests]))
          (str sidecar " original baseline refuses before send")))))

(deftest historical-not-measured-cannot-be-retrofitted
  (is (= :before-authority-unavailable
         (:reason (mission/project
                   {:baseline {:before-evidence :not-measured}
                    :dispatch {} :repository "."
                    :after-revision "historical-after" :after-rows []})))))
