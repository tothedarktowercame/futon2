(ns futon2.aif.meta-algorithm-evidence-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-algorithm-evidence :as sut]))

(def selection
  {:schema :wm/outer-task-selection-v1
   :chosen {:id "A-self-heal" :kind :algorithm}
   :action {:type :run-algorithm :target "A-self-heal"}})

(def authority
  {:selection selection :algorithm-id "A-self-heal"
   :started {:status :started :run-id "repair-1"}
   :terminal {:status :completed :outcome :repaired}
   :verification {:status :verified :checks [:terminal-run-record]}
   :repair-ticket-publication {:status :published :ticket "T-repair"}})

(deftest execution-and-publication-are-separate-evidence
  (let [good (sut/receipt authority)]
    (is (= :verified (:status good)))
    (is (= :verified (:status (sut/verify good authority))))
    (testing "a ticket is not execution evidence"
      (let [only-ticket (sut/receipt
                         (assoc authority
                                :started {:status :absent}
                                :terminal {:status :absent}
                                :verification {:status :absent}))]
        (is (= :selected-not-started (:status only-ticket)))
        (is (= :published
               (get-in only-ticket [:repair-ticket-publication :status])))))
    (testing "identity cannot change between selection and execution"
      (is (= :identity-mismatch
             (:status (sut/receipt (assoc authority :algorithm-id "A-other"))))))))
