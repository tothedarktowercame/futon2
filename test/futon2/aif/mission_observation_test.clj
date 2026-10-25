(ns futon2.aif.mission-observation-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.d-predecessor-task-authority :as d-task]
            [futon2.aif.d-predecessor-task-authority-test :as fixture]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.mission-observation :as mission]
            [futon2.aif.observation-checks :as observation]))

(defn reseal [baseline]
  (assoc baseline :baseline-sha256
         (evidence/value-digest (dissoc baseline :baseline-sha256))))

(defn scenario [f]
  (fixture/with-artifact
   {:action {:kind :cascade-candidate :id :C0 :target "target"
             :want [:artifact]
             :precedence [{:id :make-file :theta 1 :produces #{["target" :artifact]}}]}}
   (fn [{:keys [inputs repo commit] :as data}]
     (with-redefs [observation/repo-root (.getParent (java.io.File. (str repo)))]
       (f (assoc data :dispatch (:dispatch inputs) :repo-path (str repo) :after commit))))))

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
                                       :after-rows after-rows})]
       (is (= :captured (:status baseline)))
       (is (false? (get-in baseline [:rows 0 :observed])))
       (is (= :admitted (:status unchanged)))
       (is (false? (get-in unchanged [:rows 0 :after :observed])))
       (is (= :admitted (:status projected)))
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
       (is (= :measurement-missing (:reason (run baseline []))))
       (is (= :measurement-ambiguous (:reason (run baseline (conj rows (first rows))))))
       (is (= :measurement-ambiguous
              (:reason (run (reseal (update baseline :rows conj (first (:rows baseline)))) rows))))
       (is (= :before-payload-result-mismatch
              (:reason (run (reseal (assoc-in baseline [:rows 0 :observed] true)) rows))))
       (is (= :selected-want-true-before
              (:reason (run (reseal (-> baseline
                                        (assoc-in [:rows 0 :observed] true)
                                        (assoc-in [:rows 0 :result :observed] true))) rows))))
       (is (= :before-revision-mismatch
              (:reason (run (reseal (assoc-in baseline [:rows 0 :result :evidence :resolved-sha]
                                              after)) rows))))
       (is (= :occurrence-mismatch
              (:reason (run (reseal (assoc baseline :occurrence-id "foreign")) rows))))
       (is (= :locator-mismatch
              (:reason (run (reseal (assoc-in baseline [:rows 0 :locator :path] "foreign")) rows))))
       (is (= :after-payload-result-mismatch
              (:reason (run baseline (assoc-in rows [0 :observed] false)))))
       (is (= :after-observation-missing
              (:reason (run baseline (assoc-in rows [0 :result :observed] nil)))))
       (is (= :after-revision-mismatch
              (:reason (run baseline (assoc-in rows [0 :result :evidence :resolved-sha] before)))))
       (is (= :repository-mismatch
              (:reason (mission/project {:baseline baseline :dispatch dispatch
                                         :repository (.getParent (java.io.File. repo-path))
                                         :after-revision after :after-rows rows}))))))))

(deftest historical-not-measured-is-not-a-baseline
  (is (= :baseline-missing
         (:reason (mission/project {:baseline {:before-evidence :not-measured}
                                    :dispatch {} :repository "."
                                    :after-revision "after" :after-rows []})))))
