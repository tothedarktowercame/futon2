(ns futon2.aif.want-projection-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.selected-want-outcome :as outcome]
            [futon2.aif.reviewer-falsifier :as falsifier]))

(def ticket "T-car3-queue-routing-design-note")
(def ticket-token [:pattern-done ["agent/scope-before-action" 0]])
(def ticket-output (into [ticket] ticket-token))
(def ticket-locator {:class :C4 :repo "futon2" :sha "before"
                     :path "artifact.edn" :decl ":phase-1-scope-record"})

(defn cascade-action [target produces]
  {:kind :cascade-candidate
   :id (str target "/policy")
   :target target
   :want []
   :observation-locators {}
   :precedence {:co-apply
                {:patterns
                 {[:pattern 0]
                  {:id [:pattern 0]
                   :target target
                   :produces produces
                   :observation-locators
                   (into {} (map (fn [token]
                                   [[target (vec (rest token))] ticket-locator])
                                 produces))}}}}})

(deftest ticket-cascade-produced-tokens-reach-terminal-accounting
  (let [action (:selected-action
                (runner/construct-selected-action
                 {:action (cascade-action ticket #{ticket-output})}))
        commit "3189b765c9cb122624aca34cf20237606a55c004"
        measurement {:result {:observed true
                              :evidence {:resolved-sha commit :repo "futon2"
                                         :path "artifact.edn"}}
                     :declared-locator ticket-locator
                     :after-locator {:sha commit}}
        accounting (outcome/receipt
                    {:selected-action action
                     :token-comparison
                     {:status :compared
                      :prediction {:initial-belief {#{} 1}}
                      :tokens [{:token [ticket ticket-token] :observed true
                                :measurement measurement}]}})
        receipt (falsifier/receipt
                 {:target ticket :commit commit
                  :mission-standing {:status :not-applicable}
                  :selected-want-outcomes accounting
                  :artifact-binding {:commit commit :corroborates? true}
                  :review-gate {:passed? true}
                  :work-evidence {:kind :ordinary-cascade}})]
    (is (= [[ticket ticket-token]] (:want action)))
    (is (= ticket-locator (get (:observation-locators action) [ticket ticket-token])))
    (is (= :verified (:status accounting)))
    (is (= :verified (:status receipt)))))

(deftest mission-source-contract-is-unchanged
  (let [mission "M-existing"
        source-token :source-derived
        source-locator {:class :C3 :repo "futon2" :sha "before" :path "mission.md"}
        action (assoc (cascade-action mission #{[mission :cascade-output]})
                      :want [source-token]
                      :observation-locators {[mission source-token] source-locator})
        projected (:selected-action (runner/construct-selected-action {:action action}))]
    (is (= [source-token] (:want projected)))
    (is (= {[mission source-token] source-locator}
           (:observation-locators projected)))))

(deftest empty-cascade-keeps-the-falsifier-teeth
  (let [action (:selected-action
                (runner/construct-selected-action
                 {:action (cascade-action "T-empty" #{})}))
        accounting (outcome/receipt {:selected-action action})
        receipt (falsifier/receipt
                 {:target "T-empty" :commit "abc"
                  :mission-standing {:status :not-applicable}
                  :selected-want-outcomes accounting
                  :artifact-binding {:commit "abc" :corroborates? true}
                  :review-gate {:passed? true}
                  :work-evidence {:kind :ordinary-cascade}})]
    (is (= [] (:want action)))
    (is (= :selected-want-identity-missing (:reason accounting)))
    (is (= :refused (:status receipt)))
    (is (some #{:selected-want-accounting} (:failed-checks receipt)))))
