(ns futon2.aif.hierarchical-budget-adapter-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.hierarchical-budget-adapter :as adapter]))

(def live-fields
  {:root-id :campaign/shared
   :shared-budget 9
   :context {:campaign/id "r11-two-tick-poc" :tick/id "tick-a"}
   :fields
   [{:id :repair-planner
     :budget 6
     :proposals
     [{:id :repair-deep
       :action {:type :repair :target "deep"}
       :cost 6 :utility 10}
      {:id :repair-compact
       :action {:type :repair :target "compact"}
       :cost 3 :utility 8}]}
    {:id :evidence-planner
     :budget 6
     :proposals
     [{:id :evidence-deep
       :action {:type :gather-evidence :target "deep"}
       :cost 6 :utility 9}
      {:id :evidence-compact
       :action {:type :gather-evidence :target "compact"}
       :cost 3 :utility 6}]}]})

(deftest live-fields-produce-a-feasible-globally-selected-portfolio
  (testing "local rank-1 winners cost 12 against a shared budget of 9"
    (let [out (adapter/select-ranked-proposal-fields live-fields)]
      (is (= #{:repair-compact :evidence-deep} (:selected-ids out)))
      (is (= 9.0 (:total-cost out)))
      (is (= 17.0 (:total-utility out)))
      (is (= {:campaign/shared 9.0
              :repair-planner 3.0
              :evidence-planner 6.0}
             (:node-usage out)))
      (is (= #{:campaign/shared :repair-planner :evidence-planner}
             (:oversubscribed-nodes out)))
      (is (:within-all-budgets? out)))))

(deftest adapter-preserves-source-field-action-rank-and-disposition
  (let [out (adapter/select-ranked-proposal-fields live-fields)
        proposals (concat (:selected out) (:rejected out))
        by-id (into {} (map (juxt :id identity)) proposals)]
    (is (= #{:repair-deep :repair-compact
             :evidence-deep :evidence-compact}
           (set (keys by-id))))
    (is (= :repair-planner
           (:proposal/field-id (by-id :repair-compact))))
    (is (= {:type :repair :target "compact"}
           (:proposal/action (by-id :repair-compact))))
    (is (= 2 (:rank (by-id :repair-compact))))
    (is (= 1 (:proposal/source-index (by-id :repair-compact))))
    (is (= 3 (:cost (by-id :repair-compact))))
    (is (= 8 (:utility (by-id :repair-compact))))
    (is (= :shared-budget-arbitration
           (:rejection/reason (by-id :repair-deep))))
    (is (= {:repair-planner 1 :evidence-planner 1}
           (:node-selection-limits out)))))

(deftest one-ranked-field-can-select-at-most-one-alternative
  (let [out (adapter/select-ranked-proposal-fields
             {:shared-budget 10
              :fields [{:id :one-source
                        :budget 10
                        :proposals
                        [{:id :a :action {:type :a} :cost 2 :utility 5}
                         {:id :b :action {:type :b} :cost 2 :utility 4}]}]})]
    (is (= #{:a} (:selected-ids out)))
    (is (= 1 (count (:selected out))))))

(deftest receipt-replays-the-entire-arbitration-output-exactly
  (let [out (adapter/select-ranked-proposal-fields live-fields)
        replay (adapter/replay (:replay/receipt out))]
    (is (:replay/identical? replay))
    (is (= (dissoc out :replay/receipt) (:replay/actual replay)))
    (is (= (:replay/expected replay) (:replay/actual replay)))))

(deftest replay-detects-a-changed-recorded-output
  (let [receipt (:replay/receipt
                 (adapter/select-ranked-proposal-fields live-fields))
        tampered (update-in receipt [:output :total-utility] inc)]
    (is (false? (:replay/identical? (adapter/replay tampered))))))

(deftest malformed-live-fields-fail-closed
  (testing "source action is mandatory evidence"
    (let [failure (try
                    (adapter/select-ranked-proposal-fields
                     {:shared-budget 1
                      :fields [{:id :source :budget 1
                                :proposals [{:id :p :cost 1 :utility 1}]}]})
                    nil
                    (catch clojure.lang.ExceptionInfo e e))]
      (is (= :invalid-ranked-proposal-fields
             (:failure-kind (ex-data failure)))))))
