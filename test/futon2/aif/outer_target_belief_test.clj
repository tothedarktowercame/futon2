(ns futon2.aif.outer-target-belief-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.outer-target-belief :as belief]))

(defn- row [run target terminal & {:keys [effects collateral blocker]}]
  {:run/id run :click/id (str run "-click") :task {:id target :kind :mission}
   :terminal-cause {:schema :wm/terminal-cause-v1 :class :task-outcome}
   :terminal-outcome {:class terminal}
   :selected-want-effects (or effects {:status :absent})
   :collateral-progress (or collateral {:status :absent})
   :blocker (or blocker {:status :absent})})

(defn- dataset [rows]
  {:status :produced :source-pin {:path "fixture" :sha256 "abc"} :rows rows})

(deftest folds-only-observed-outcomes-into-the-chosen-target
  (let [result
        (belief/build
         {:prior (belief/prior 1 :operator-provisional)
          :dataset
          (dataset
           [(row "r1" "M-a" :grounded-progress
                 :effects {:status :present
                           :counts {:reached 0 :progressed 0 :blocked 0 :untouched 4}}
                 :collateral {:status :present :kind :grounded-collateral-progress})
            (row "r2" "M-b" :closure)
            (row "r3" "M-a" :grounded-progress)])})]
    (is (= :produced (:status result)))
    (is (= {:rows 3 :applied 2 :unresolved 1} (:counts result)))
    (is (= 2 (get-in result [:targets "M-a" :alpha
                             :grounded-collateral-progress])))
    (is (= 1 (get-in result [:targets "M-a" :alpha :closure])))
    (is (= 2 (get-in result [:targets "M-b" :alpha :closure])))
    (is (= :categorical-negative-log-evidence
           (get-in result [:events 0 :present-fit :kind])))
    (is (= :before-parameter-update
           (get-in result [:events 0 :present-fit :computed])))
    (is (= 0.2 (get-in result [:events 0 :present-fit
                               :predictive-probability])))
    (is (< (Math/abs (- (Math/log 5.0)
                        (get-in result [:events 0 :present-fit :value])))
           1.0e-12))
    (is (= :outcome-evidence-insufficient
           (get-in result [:events 2 :observation :reason])))))

(deftest selected-want-progress-outranks-collateral-classification
  (let [result
        (belief/build
         {:prior (belief/prior 1)
          :dataset
          (dataset
           [(row "r1" "M-a" :grounded-progress
                 :effects {:status :present
                           :counts {:reached 0 :progressed 1 :blocked 0 :untouched 3}}
                 :collateral {:status :present :kind :grounded-collateral-progress})])})]
    (is (= :selected-want-progress
           (get-in result [:events 0 :observation :class])))))

(deftest apparatus-and-ambiguous-failures-never-update-target-alpha
  (let [apparatus (assoc (row "r-apparatus" "M-a" :abstention-or-failure)
                         :terminal-cause {:schema :wm/terminal-cause-v1
                                          :class :apparatus-failure
                                          :kind :resume-protocol-violation
                                          :stage :cascade-resume})
        ambiguous (assoc (row "r-ambiguous" "M-a" :abstention-or-failure)
                         :terminal-cause {:schema :wm/terminal-cause-v1
                                          :class :unclassified
                                          :kind :untyped-failure})
        task-failure (row "r-task" "M-a" :abstention-or-failure)
        result (belief/build {:prior (belief/prior 1)
                              :dataset (dataset [apparatus ambiguous task-failure])})]
    (is (= {:rows 3 :applied 1 :unresolved 2} (:counts result)))
    (is (= 2 (get-in result [:targets "M-a" :alpha :abstention-or-failure])))
    (is (= [:apparatus-failure-not-task-evidence
            :terminal-cause-unclassified]
           (mapv #(get-in % [:observation :reason]) (take 2 (:events result)))))
    (is (= {:apparatus {:count 1
                        :identities [["r-apparatus" "r-apparatus-click"]]
                        :reasons {:apparatus-failure-not-task-evidence 1}}
            :unknown {:count 1
                      :identities [["r-ambiguous" "r-ambiguous-click"]]
                      :reasons {:terminal-cause-unclassified 1}}
            :missing {:count 0 :identities [] :reasons {}}}
           (:non-task-observations result)))))

(deftest replay-order-does-not-change-alpha-and-no-target-is-not-a-belief
  (let [rows [(row "r-success" "M-a" :closure)
              (row "r-failure" "M-a" :abstention-or-failure)]
        a (belief/build {:prior (belief/prior 1) :dataset (dataset rows)})
        b (belief/build {:prior (belief/prior 1) :dataset (dataset (reverse rows))})]
    (is (= (get-in a [:targets "M-a" :alpha])
           (get-in b [:targets "M-a" :alpha])))
    (is (nil? (get-in a [:targets nil])))))

(deftest refuses-implicit-or-malformed-authority
  (is (= :prior-invalid
         (:reason (belief/build {:dataset (dataset [])}))))
  (is (= :dataset-not-produced
         (:reason (belief/build {:dataset {:status :refused}
                                 :prior (belief/prior 1)})))))
