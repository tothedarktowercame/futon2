(ns futon2.aif.injury-04e14314-acceptance-test
  "Acceptance pin for injury/04e14314a0eec3aab88d8e7044be26f3ae5dfe6d
  (run 2026-10-22-02587641-d615-4a5b-8bc8-a3df48e2ab45, attempt-002,
  receipt/id 5d519f8e1cf9912335aaddc8d3b5186545c19fa6fabbe5211c4c34f98bd5a136).

  The injury: the reviewer approved, but reviewer-falsifier/verify refused
  with :reviewer-falsifier-applicable-check-failed because the selected
  action carried no want identity — selected-want-outcome/receipt refused
  with :selected-want-identity-missing ({:target \"M-stack-geometry\"
  :wants []}), failing :selected-want-accounting and :evidence-authority,
  so the click closed :build-failed at :reviewer-wait and the terminal
  receipt was a :failure instead of an :action-receipt.

  This test drives ONLY the production functions named in the diagnosis —
  outer-task-selection/select-task, wm.loop-node-exercise/require-complete!,
  selected-want-outcome/receipt, reviewer-falsifier/receipt + verify, and
  wm.terminal-receipt/terminal-receipt — with no stubs. On injured code the
  first assertions fail because select-task's :action projects no :want;
  after repair the want identity selected at the entrypoint flows through
  want accounting and evidence authority, the falsifier verifies, and the
  click terminates in exactly one :action-receipt."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-task-selection :as outer-task-selection]
            [futon2.aif.selected-want-outcome :as selected-want-outcome]
            [futon2.aif.reviewer-falsifier :as reviewer-falsifier]
            [futon2.aif.wm.loop-node-exercise :as loop-node-exercise]
            [futon2.aif.wm.terminal-receipt :as terminal-receipt]))

(def target "M-stack-geometry")
(def want-token "mission-crossref-edges-ingested")
(def commit "87d4289289da83a1040f5bf97252870f33ae6378")

(def task
  {:id target
   :kind :mission
   :status-class :open
   :open-hole-count 12
   :priority 1
   ;; The want identity the selection entrypoint must retain on the action.
   :want [want-token]
   :requisition {:state :open}})

(defn- selection-action
  [tasks seed]
  (:action (outer-task-selection/select-task {:tasks tasks :seed seed})))

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
  (testing "select-task's action retains the task's declared want identity"
    (let [action (selection-action [task] 7)]
      (is (= :advance-mission (:type action)) "the action targets the mission")
      (is (= target (:target action)))
      (is (seq (:want action))
          "injury 04e14314: the selected action carried no :want, so
           selected-want-outcome refused with :selected-want-identity-missing")
      (is (= [want-token] (mapv #(if (vector? %) (second %) %) (:want action))))))

  (testing "selected-want-outcome verifies the selected want"
    (let [outcomes (selected-want-outcomes (selection-action [task] 7))]
      (is (not= :selected-want-identity-missing (:reason outcomes)))
      (is (= :verified (:status outcomes))
          (str "want accounting refused: " (pr-str outcomes)))
      (is (= :reached (-> outcomes :outcomes first :classification)))
      (is (string? (:receipt/id outcomes)))))

  (testing "reviewer-falsifier verifies with want accounting and authority"
    (let [outcomes (selected-want-outcomes (selection-action [task] 7))
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
