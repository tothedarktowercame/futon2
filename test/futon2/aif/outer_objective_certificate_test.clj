(ns futon2.aif.outer-objective-certificate-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-objective-certificate :as sut]))

;; SUT authority fixture only. This evaluator is not production policy.
(def calls (atom 0))
(defn fixture-evaluator [channels]
  (swap! calls inc)
  (reduce + (map :channel/value channels)))
(defn substituted-evaluator [_] 0)

(def registration
  {:schema :wm/outer-objective-registration-v1
   :objective/identity :test/sum-values :objective/version 1
   :registration/token :test/registration-1 :registration/authority 41
   :implementation/pin "test-fixture:fixture-evaluator:v1"
   :evaluate fixture-evaluator})
(def sealed (sut/registry [registration]))
(def objective (sut/public-registration registration))
(def selector-authority 42)

(defn input [task value epoch]
  (let [base {:task/id task
              :task/channels [{:channel/id :cost
                               :channel/observation (str task "/observation")
                               :channel/evidence (str task "/evidence")
                               :channel/value-digest (str task "/value")
                               :channel/value value}]
              :task/freshness-epoch epoch}]
    (assoc base :task/dependencies (sut/required-dependencies objective base))))

(defn evaluation [task value epoch]
  (sut/evaluate-task sealed selector-authority objective (input task value epoch)))

(defn stable-frontier []
  (let [a (evaluation "M-a" 2 7) b (evaluation "M-b" 2 7)]
    {:refresh {:prior [a b] :current [a b] :retained [a b]
               :invalidated [] :recomputed [] :removed #{}
               :delta {:changed-observations #{} :added-tasks #{}
                       :removed-tasks #{}}
               :current-open-tasks #{"M-a" "M-b"}}
     :evaluations [a b]}))

(deftest registered-evaluator-is-called-once-and-stable-id-breaks-ties
  (reset! calls 0)
  (let [a (evaluation "M-a" 2 7)]
    (is (= 1 @calls))
    (is (sut/evaluation-valid? sealed selector-authority objective a)))
  (let [selected (sut/select-minimum sealed selector-authority objective
                                     (stable-frontier))]
    (is (= :selected (:status selected)))
    (is (= "M-a" (:selected selected)))
    (is (= :comparison-key-then-stable-task-id (:tie-law selected)))))

(deftest registration-and-input-falsifiers
  (testing "same labels cannot substitute evaluator output"
    (let [forged (sut/registry [(assoc registration :evaluate substituted-evaluator)])]
      (is (not (sut/evaluation-valid? forged selector-authority objective
                                      (evaluation "M-a" 2 7))))))
  (testing "selector cannot authorize itself"
    (is (= [:selector-authority-not-separated]
           (:errors (sut/evaluate-task sealed 41 objective (input "M-a" 2 7))))))
  (testing "dependency closure is exact"
    (is (some #{:dependency-closure-mismatch}
              (:errors (sut/evaluate-task sealed selector-authority objective
                                          (assoc (input "M-a" 2 7)
                                                 :task/dependencies #{}))))))
  (testing "channel identity is unique"
    (let [i (input "M-a" 2 7)
          duplicate (assoc i :task/channels (conj (:task/channels i)
                                                   (first (:task/channels i))))]
      (is (some #{:duplicate-channel}
                (:errors (sut/evaluate-task sealed selector-authority objective
                                            duplicate)))))))

(deftest strict-incremental-refresh-and-no-global-reindexing
  (let [old-a (evaluation "M-a" 2 7) old-b (evaluation "M-b" 5 7)
        new-b (evaluation "M-b" 1 8) added (evaluation "M-c" 3 8)
        refresh {:prior [old-a old-b] :current [old-a new-b added]
                 :retained [old-a] :invalidated [old-b]
                 :recomputed [new-b added] :removed #{}
                 :delta {:changed-observations #{"M-b/observation"}
                         :added-tasks #{"M-c"} :removed-tasks #{}}
                 :current-open-tasks #{"M-a" "M-b" "M-c"}}]
    (let [produced (sut/produce-refresh
                    sealed selector-authority objective [old-a old-b]
                    (:delta refresh)
                    {"M-b" (:input new-b) "M-c" (:input added)}
                    (:current-open-tasks refresh))]
      (is (= :valid (:status produced)))
      (is (= refresh (:refresh produced))))
    (is (sut/refresh-valid? refresh))
    (is (= old-a (first (:retained refresh))))
    (is (some #{:retained-partition-mismatch}
              (sut/refresh-errors (assoc refresh :retained [old-a old-b]))))
    (is (some #{:recomputed-population-mismatch}
              (sut/refresh-errors (assoc refresh :recomputed [new-b]))))
    (is (some #{:recomputed-population-mismatch}
              (sut/refresh-errors (assoc refresh :recomputed [added]))))
    (let [removed-refresh
          {:prior [old-a old-b] :current [old-a] :retained [old-a]
           :invalidated [] :recomputed [] :removed #{"M-b"}
           :delta {:changed-observations #{} :added-tasks #{}
                   :removed-tasks #{"M-b"}}
           :current-open-tasks #{"M-a"}}
          frontier {:refresh removed-refresh :evaluations [old-a]}]
      (is (sut/refresh-valid? removed-refresh))
      (is (= "M-a" (:selected (sut/select-minimum sealed selector-authority
                                                   objective frontier)))))))

(deftest population-and-tie-law-falsifiers
  (let [frontier (stable-frontier)]
    (is (= :outer-frontier-certificate-invalid
           (:reason (sut/select-minimum sealed selector-authority objective
                                        (assoc frontier :evaluations
                                               [(first (:evaluations frontier))])))))
    (let [reversed (-> frontier
                       (update :evaluations #(vec (reverse %)))
                       (update :refresh #(assoc % :current
                                               (vec (reverse (:current %))))))]
      (is (= "M-a" (:selected (sut/select-minimum sealed selector-authority
                                                  objective frontier))))
      (is (= "M-a" (:selected (sut/select-minimum sealed selector-authority
                                                  objective reversed)))))))
