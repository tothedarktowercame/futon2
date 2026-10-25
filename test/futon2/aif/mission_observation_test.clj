(ns futon2.aif.mission-observation-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.d-predecessor-task-authority :as d-task]
            [futon2.aif.d-predecessor-task-authority-test :as fixture]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.mission-observation :as mission]
            [futon2.aif.observation-checks :as observation]
            [futon2.aif.selected-want-outcome :as selected]))

(defn reseal [baseline]
  (assoc baseline :baseline-sha256
         (evidence/value-digest (dissoc baseline :baseline-sha256))))

(defn scenario-with [opts f]
  (fixture/with-artifact
   (merge {:action {:kind :cascade-candidate :id :C0 :target "target"
                    :want [:artifact]
                    :precedence [{:id :make-file :theta 1
                                  :produces #{["target" :artifact]}}]}}
          opts)
   (fn [{:keys [inputs repo commit] :as data}]
     (with-redefs [observation/repo-root (.getParent (java.io.File. (str repo)))]
       (f (assoc data :dispatch (:dispatch inputs) :repo-path (str repo) :after commit))))))

(defn scenario [f] (scenario-with {} f))

(deftest real-reader-baseline-and-projection
  (scenario
   (fn [{:keys [dispatch repo-path after]}]
     (let [before (get-in dispatch [:before :head])
           baseline (mission/capture-before {:dispatch dispatch :repository repo-path
                                             :revision before})
           unchanged-rows (d-task/artifact-tokens dispatch repo-path before)
           unchanged (mission/project {:baseline baseline :dispatch dispatch
                                       :repository repo-path :after-revision before
                                       :after-rows unchanged-rows})
           after-rows (d-task/artifact-tokens dispatch repo-path after)
           projected (mission/project {:baseline baseline :dispatch dispatch
                                       :repository repo-path :after-revision after
                                       :after-rows after-rows})
           accounting (selected/receipt
                       {:selected-action
                        (assoc (get-in dispatch [:occurrence :action/value])
                               :observation-locators
                               {(get-in baseline [:rows 0 :token])
                                (get-in baseline [:rows 0 :locator])})
                        :token-comparison {:status :compared :tokens []
                                           :prediction {:initial-belief {#{} 1}}}
                        :mission-observation projected})]
       (is (= :captured (:status baseline)))
       (is (false? (get-in baseline [:rows 0 :observed])))
       (is (= :admitted (:status unchanged)))
       (is (false? (get-in unchanged [:rows 0 :after :observed])))
       (is (= :admitted (:status projected)))
       (is (= :verified (:status accounting)))
       (is (true? (get-in projected [:rows 0 :after :observed])))
       (is (= before (:before-revision projected)))
       (is (= after (:after-revision projected)))))))

(deftest refusals-preserve-absence-and-bindings
  (scenario
   (fn [{:keys [dispatch repo-path after]}]
     (let [before (get-in dispatch [:before :head])
           baseline (mission/capture-before {:dispatch dispatch :repository repo-path :revision before})
           rows (d-task/artifact-tokens dispatch repo-path after)
           run #(mission/project {:baseline %1 :dispatch dispatch :repository repo-path
                                  :after-revision after :after-rows %2})]
       (is (= :after-authority-mismatch (:reason (run baseline []))))
       (is (= :after-authority-mismatch (:reason (run baseline (conj rows (first rows))))))
       (is (= :before-authority-mismatch
              (:reason (run (reseal (update baseline :rows conj (first (:rows baseline)))) rows))))
       (is (= :before-authority-mismatch
              (:reason (run (reseal (assoc-in baseline [:rows 0 :observed] true)) rows))))
       (is (= :before-authority-mismatch
              (:reason (run (reseal (-> baseline
                                        (assoc-in [:rows 0 :observed] true)
                                        (assoc-in [:rows 0 :result :observed] true))) rows))))
       (is (= :before-authority-mismatch
              (:reason (run (reseal (assoc-in baseline [:rows 0 :result :evidence :resolved-sha]
                                              after)) rows))))
       (is (= :before-authority-mismatch
              (:reason (run (reseal (assoc baseline :occurrence-id "foreign")) rows))))
       (is (= :before-authority-mismatch
              (:reason (run (reseal (assoc-in baseline [:rows 0 :locator :path] "foreign")) rows))))
       (is (= :after-authority-mismatch
              (:reason (run baseline (assoc-in rows [0 :observed] false)))))
       (is (= :after-authority-mismatch
              (:reason (run baseline (assoc-in rows [0 :result :observed] nil)))))
       (is (= :after-authority-mismatch
              (:reason (run baseline (assoc-in rows [0 :result :evidence :resolved-sha] before)))))
       (is (= :before-authority-mismatch
              (:reason (mission/project {:baseline baseline :dispatch dispatch
                                         :repository (.getParent (java.io.File. repo-path))
                                         :after-revision after :after-rows rows}))))))))

(deftest historical-not-measured-is-not-a-baseline
  (is (= :before-authority-unavailable
         (:reason (mission/project {:baseline {:before-evidence :not-measured}
                                    :dispatch {} :repository "."
                                    :after-revision "after" :after-rows []})))))

(deftest actual-reader-true-before-and-c4
  (testing "an actual C3 true baseline is a policy refusal, not a new achievement"
    (scenario-with
     {:locator-path "base.txt"}
     (fn [{:keys [dispatch repo-path after]}]
       (let [before (get-in dispatch [:before :head])
             baseline (mission/capture-before {:dispatch dispatch :repository repo-path
                                               :revision before})
             rows (d-task/artifact-tokens dispatch repo-path after)]
         (is (true? (get-in baseline [:rows 0 :observed])))
         (is (= :selected-want-true-before
                (:reason (mission/project {:baseline baseline :dispatch dispatch
                                           :repository repo-path :after-revision after
                                           :after-rows rows}))))))))
  (testing "C4 is read by the same immutable-revision authority path"
    (scenario-with
     {:action {:kind :cascade-candidate :id :C4 :target "target" :want [:decl]
               :precedence [{:id :make-file :theta 1 :produces #{["target" :decl]}}]}
      :universe #{["target" :decl]}
      :locators {:decl {:class :C4 :repo "repo" :sha "HEAD"
                        :path "created.clj" :decl "(ns"}}}
     (fn [{:keys [dispatch repo-path after]}]
       (let [before (get-in dispatch [:before :head])
             baseline (mission/capture-before {:dispatch dispatch :repository repo-path
                                               :revision before})
             rows (d-task/artifact-tokens dispatch repo-path after)
             projected (mission/project {:baseline baseline :dispatch dispatch
                                         :repository repo-path :after-revision after
                                         :after-rows rows})]
         (is (= :C4 (get-in baseline [:rows 0 :result :check])))
         (is (false? (get-in baseline [:rows 0 :observed])))
         (is (true? (get-in rows [0 :result :observed])))
         (is (= :admitted (:status projected))))))))
