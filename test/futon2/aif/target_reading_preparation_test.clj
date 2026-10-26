(ns futon2.aif.target-reading-preparation-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.selected-target-reading :as reading]
            [futon2.aif.target-reading-preparation :as sut]))

(def field
  ;; Shape measured in the failed click: 780 tasks, nine stored readings.
  {:source-pin {:path "wm://pre-choice-policy-field-v1" :sha256 "fixture"}
   :target-sources (mapv #(hash-map :target-id (str "T-" %)
                                  :source-path (str "/tasks/T-" % ".md")) (range 780))
   :families (mapv (fn [n]
                     {:target-id (str "T-" n)
                      :status (if (< n 9) :computed :refused)
                      :failures (if (< n 9) []
                                  [{:kind (if (= n 9) :stale-target-reading
                                              :no-current-target-reading)
                                    :expected-digest (str "digest-" n)}])})
                   (range 780))})

(deftest prepares-before-any-outer-choice-and-reuses-current-readings
  (let [seen (atom [])
        result (sut/prepare! {} field
                 {:deadline-ms 100 :now-fn (constantly 0) :limit 780
                  :repair-fn (fn [_ need] (swap! seen conj need)
                               {:published true :status :published})})]
    (is (= 771 (count @seen)))
    (is (= "T-9" (:target (first @seen))))
    (is (= "/tasks/T-9.md" (:source-path (first @seen))))
    (is (= "digest-9" (:excerpt-digest (first @seen))))
    (is (= :prepared (:status result)))
    (is (empty? (:pending result)))))

(deftest incomplete-menu-reaches-the-existing-runner-preparation-seam
  (let [judgement {:outer-task-selection
                   {:chosen {:absent :incomplete-policy-menu-coverage}
                    :policy {:reason :incomplete-policy-menu-coverage}
                    :policy-field field}}]
    (is (= "T-9" (:target (reading/selected-need judgement))))
    (is (nil? (reading/prechoice-field
               (assoc-in judgement [:outer-task-selection :chosen] {:id "T-0"}))))))

(deftest bounded-work-is-pending-not-success
  (let [calls (atom 0)
        options {:deadline-ms 100 :now-fn (constantly 0) :limit 2
                 :repair-fn (fn [& _] (swap! calls inc) {:published true})}
        result (sut/prepare! {} field options)
        expired (sut/prepare! {} field (assoc options :now-fn (constantly 100)))]
    (is (= 2 @calls))
    (is (= :pending (:status result)))
    (is (= 769 (count (:pending result))))
    (is (= :deadline (:reason expired)))
    (is (= 771 (count (:pending expired))))))

(deftest publication-refusal-spends-one-slot-and-the-batch-continues
  (let [result (sut/prepare! {} field
                 {:deadline-ms 100 :now-fn (constantly 0) :limit 3
                  :repair-fn (fn [_ need]
                               (if (= "T-9" (:target need))
                                 (throw (ex-info "offline" {}))
                                 {:status :published :published true}))})]
    (is (= :pending (:status result)))
    (is (= :batch-limit (:reason result)))
    (is (not= :publication-refused (:reason result)))
    (is (= 3 (count (:records result))))
    (is (= 768 (count (:pending result))))
    (is (= ["T-9" "T-10" "T-11"]
           (mapv #(get-in % [:need :target]) (:records result))))
    (is (= :reading-preparation-threw (get-in result [:records 0 :kind])))
    (is (false? (get-in result [:records 0 :published])))
    (is (every? :published (subvec (:records result) 1)))
    (is (= [["T-9" "digest-9"]] (get-in result [:frontier :refused-ids])))
    (is (= [["T-10" "digest-10"] ["T-11" "digest-11"]]
           (get-in result [:frontier :published-ids])))
    (is (not-any? #(= "T-9" (:target %)) (:pending result)))))

(deftest every-refusal-still-spends-the-batch-limit
  (let [result (sut/prepare! {} field
                 {:deadline-ms 100 :now-fn (constantly 0) :limit 3
                  :repair-fn (fn [& _] (throw (ex-info "offline" {})))})]
    (is (= :pending (:status result)))
    (is (= :batch-limit (:reason result)))
    (is (not= :publication-refused (:reason result)))
    (is (= 3 (count (:records result))))
    (is (= 3 (count (get-in result [:frontier :refused-ids]))))
    (is (= 768 (count (:pending result))))))

(deftest completed-batch-with-a-refusal-is-refused
  (let [two-needs (assoc field :families (subvec (:families field) 9 11))
        result (sut/prepare! {} two-needs
                 {:deadline-ms 100 :now-fn (constantly 0) :limit 8
                  :repair-fn (fn [_ need]
                               (if (= "T-9" (:target need))
                                 {:status :refused :published false}
                                 {:status :published :published true}))})]
    (is (= :refused (:status result)))
    (is (= :batch-complete (:reason result)))
    (is (not= :publication-refused (:reason result)))
    (is (empty? (:pending result)))
    (is (= 2 (count (:records result))))))
