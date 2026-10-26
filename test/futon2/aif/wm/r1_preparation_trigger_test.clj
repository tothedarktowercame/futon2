(ns futon2.aif.wm.r1-preparation-trigger-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.selected-target-reading :as reading]))

(defn- family
  [target failure]
  {:target-id target
   :failures (if failure
               [{:kind failure :expected-digest (str "digest-" target)}]
               [])})

(defn- field
  [families]
  {:target-sources (mapv (fn [{:keys [target-id]}]
                           {:target-id target-id
                            :source-path (str "/source/" target-id ".md")})
                         families)
   :families families})

(defn- judgement
  [chosen families]
  (let [policy-field (field families)]
    {:outer-task-selection
     {:chosen {:id chosen}
      :policy {:reason :ranked-choice}
      :policy-field policy-field}
     :decision {:selection-certificate {:target-policy-field policy-field}}
     :cascade-problems {:target-sources (:target-sources policy-field)}}))

(deftest retained-policy-field-triggers-bounded-preparation
  (let [readable-winner (judgement
                         "T-winner"
                         [(family "T-winner" nil)
                          (family "T-1" :no-current-target-reading)
                          (family "T-2" :stale-target-reading)
                          (family "T-3" :no-current-target-reading)
                          (family "T-4" :no-current-target-reading)])
        unmet-winner (judgement
                      "T-3"
                      [(family "T-1" :no-current-target-reading)
                       (family "T-2" :stale-target-reading)
                       (family "T-3" :no-current-target-reading)
                       (family "T-4" :no-current-target-reading)])]
    (testing "a readable winner does not hide other retained field needs"
      (is (= ["T-1" "T-2" "T-3" "T-4"]
             (mapv :target (reading/preparation-needs readable-winner))))
      (is (not-any? #{"T-winner"}
                    (map :target (reading/preparation-needs readable-winner))))
      (is (true? (reading/preparation-needed? readable-winner))))
    (testing "an unmet winner is first exactly once"
      (is (= ["T-3" "T-1" "T-2" "T-4"]
             (mapv :target (reading/preparation-needs unmet-winner))))
      (is (= 1 (count (filter #{"T-3"}
                              (map :target (reading/preparation-needs unmet-winner)))))))
    (testing "absence of both a field and a selected failure does not trigger"
      (let [none (-> (judgement "T-winner" [(family "T-winner" nil)])
                     (update :outer-task-selection dissoc :policy-field)
                     (assoc-in [:decision :selection-certificate
                                :target-policy-field :families]
                               []))]
        (is (= [] (reading/preparation-needs none)))
        (is (false? (reading/preparation-needed? none)))))))

(deftest selected-target-reading-step-runs-for-retained-field-needs
  (let [j (judgement
           "T-winner"
           [(family "T-winner" nil)
            (family "T-1" :no-current-target-reading)
            (family "T-2" :no-current-target-reading)
            (family "T-3" :no-current-target-reading)
            (family "T-4" :no-current-target-reading)])]
    (testing "a publication triggers exactly one redecision"
      (let [calls (atom 0)
            redecisions (atom 0)
            record {:schema :wm/target-reading-preparation-v1
                    :records [{:need {:target "T-1"}}
                              {:need {:target "T-2"}}
                              {:need {:target "T-3"}}]
                    :published true}
            step (runner/selected-target-reading-step
                  {} j
                  (fn [_ _] (swap! calls inc) record)
                  (fn [_]
                    (swap! redecisions inc)
                    (assoc j :redecided true)))]
        (is (= 1 @calls))
        (is (= 1 @redecisions))
        (is (true? (get-in step [:judgement :redecided])))
        (is (= record (:record step)))))
    (testing "a nonpublishing batch is attached without redecision"
      (let [calls (atom 0)
            redecisions (atom 0)
            record {:schema :wm/target-reading-preparation-v1
                    :records [{:need {:target "T-1"}}
                              {:need {:target "T-2"}}
                              {:need {:target "T-3"}}]
                    :published false}
            step (runner/selected-target-reading-step
                  {} j
                  (fn [_ _] (swap! calls inc) record)
                  (fn [_] (swap! redecisions inc) j))]
        (is (= 1 @calls))
        (is (zero? @redecisions))
        (is (= record (:record step)))
        (is (= record (get-in step [:judgement
                                    :selected-target-reading-request])))))))
