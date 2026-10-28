(ns futon2.aif.injury-04e14314-acceptance-test
  "Supplied action wants survive accounting and terminal verification.
  The removed outer selector's want-projection behavior is no longer tested."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.selected-want-outcome :as selected-want-outcome]
            [futon2.aif.reviewer-falsifier :as reviewer-falsifier]
            [futon2.aif.wm.loop-node-exercise :as loop-node-exercise]
            [futon2.aif.wm.terminal-receipt :as terminal-receipt]))

(def target "M-stack-geometry")
(def want-token "mission-crossref-edges-ingested")
(def commit "87d4289289da83a1040f5bf97252870f33ae6378")

(def supplied-action
  {:type :advance-mission :target target :want [want-token]})

(defn- selected-want-outcomes
  [selected-action]
  (let [want [target want-token]
        measurement
        {:after-locator {:sha commit}
         :result {:observed true
                  :evidence {:resolved-sha commit
                             :repo "futon5a"
                             :path (str "holes/missions/" target ".md")}}}
        token-comparison
        {:status :compared
         ;; The want is false in the initial belief, as required.
         :prediction {:initial-belief {{:unrelated-state 1} 1.0}}
         :rows [{:token want :observed true :measurement measurement}]}]
    (selected-want-outcome/receipt
      {:selected-action selected-action
       :token-comparison token-comparison})))

(deftest selected-want-identity-flows-to-verified-terminal-receipt
  (testing "selected-want-outcome verifies the selected want"
    (let [outcomes (selected-want-outcomes supplied-action)]
      (is (not= :selected-want-identity-missing (:reason outcomes)))
      (is (= :verified (:status outcomes))
          (str "want accounting refused: " (pr-str outcomes)))
      (is (= :reached (-> outcomes :outcomes first :classification)))
      (is (string? (:receipt/id outcomes)))))

  (testing "reviewer-falsifier verifies with want accounting and authority"
    (let [outcomes (selected-want-outcomes supplied-action)
          input {:target target
                 :commit commit
                 :work-evidence {:kind :ordinary-cascade}
                 :mission-standing {:status :consistent
                                    :source {:path (str "holes/missions/" target ".md")}
                                    :declarations []}
                 :selected-want-outcomes outcomes
                 :disposition nil
                 :artifact-binding {:repo "futon5a"
                                    :commit commit
                                    :pre-dispatch-head "9d8d6993c7b3d813aabce8e90862f2d97bbdb727"
                                    :observed-head commit
                                    :corroborates? true}
                 :review-gate {:required? true
                               :code-files ["scripts/build_mission_crossref_edges.py"]
                               :executed? true
                               :tool-events 8
                               :execution-source :job-summary
                               :passed? true}}
          receipt (reviewer-falsifier/receipt input)
          verification (reviewer-falsifier/verify receipt input)]
      (is (= [] (:failed-checks receipt))
          (str "injury 04e14314 failed-checks were [:selected-want-accounting
            :evidence-authority]; now: " (pr-str (:failed-checks receipt))))
      (is (= :verified (:status receipt)))
      (is (= :verified (:status verification))
          (str "verification refused: " (pr-str verification)))))

  (testing "the loop-node evaluator annotates the decision"
    (let [annotated (loop-node-exercise/require-complete! {:decision :empty})]
      (is (= loop-node-exercise/schema
             (get-in annotated
                     [:selection-certificate :loop-node-exercise :schema])))))

  (testing "terminal-receipt routes the verified close to one action-receipt"
    (let [record {:click/id "wm-click-2026-10-22-02587641"
                  :run/id "2026-10-22-02587641-d615-4a5b-8bc8-a3df48e2ab45"
                  :startedAt "2026-10-22T05:00:33.974164962Z"
                  :selection-event {:event :cascade-selected :target target}
                  :decision {}
                  :participants {:roles {:reviewer-of-record {:identity "zai-3"}}}
                  :outcome {:status :present :outcome :accepted}}
          receipt (terminal-receipt/terminal-receipt record)]
      (is (= :action-receipt (:kind receipt))
          "injury 04e14314: the run closed :build-failed and received a
           :failure receipt; a verified close gets exactly one action-receipt")
      (is (= target (:target receipt)))
      (is (= :mission (:target-kind receipt)))
      (is (= :accepted (-> receipt :outcome :outcome))))))
