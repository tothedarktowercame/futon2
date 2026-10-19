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

(deftest publication-failure-retains-the-unfinished-need
  (let [result (sut/prepare! {} field
                 {:deadline-ms 100 :now-fn (constantly 0)
                  :repair-fn (fn [& _] (throw (ex-info "offline" {})))})]
    (is (= :refused (:status result)))
    (is (= 1 (count (:records result))))
    (is (= 771 (count (:pending result))))
    (is (= :reading-preparation-threw (get-in result [:records 0 :kind])))))
