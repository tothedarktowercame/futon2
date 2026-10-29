(ns futon2.aif.policy-satisfaction-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.policy-satisfaction :as sut]
            [futon2.aif.wm.authored.req-f0ba279a :as authored]))

(def mission "M-futonzero-prelim-practice")
(def policy-id "M-futonzero-prelim-practice/policy-87eb5c880a0b9ab8e194")
(def mission-sha "e1b01eb3e0c1cef716174978e63a6b9ac100e968032bb59adbdf31929d805cf0")
(def grounded {:repo "/home/joe/code/futon0"
               :sha "cfb1408629826c6d825e2ddc06aad84ee5ac6108"})
(def checks (vec (sort-by pr-str (map :completion-check (vals authored/clauses)))))
(def construction
  {:mission mission :selected-cascade policy-id
   :cascade {:operational-plan
             {:steps (mapv (fn [check] {:clause {:completion-check check}}) checks)}}})
(def qualified
  {:run/id "attempt-002"
   :failure {:kind :already-satisfied :stage :author-wait
             :detail {:failure-kind :already-satisfied :failure-stage :author-wait
                      :outcome :already-satisfied :grounded-commit grounded}}
   :run-output {:outcome :already-satisfied}})
(def field {:families [{:status :computed :target-id mission
                        :policies [{:policy-id policy-id :cascade {:nodes []}}
                                   {:policy-id (str mission "/policy-other") :cascade {:nodes []}}]
                        :failures [] :failure-count 0}]})

(deftest producer-qualified-close-excludes-exact-next-policy
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "wm-policy-satisfaction" (make-array java.nio.file.attribute.FileAttribute 0)))
        retained (sut/retain! root {:run-record qualified :construction construction
                                    :mission-source-sha256 mission-sha})
        next-field (sut/exclude-satisfied field root {mission mission-sha})
        family (first (:families next-field))]
    (is (= policy-id (:policy-id retained)))
    (is (= checks (:completion-checks retained)))
    (is (= grounded (:grounded-commit retained)))
    (is (= [(str mission "/policy-other")] (mapv :policy-id (:policies family))))
    (is (= :policy-already-satisfied (-> family :failures last :kind)))
    (is (get-in family [:failures 0 :record-pin :sha256]))
    (testing "mission byte change invalidates the evidence"
      (is (= 2 (count (get-in (sut/exclude-satisfied field root {mission "changed"})
                              [:families 0 :policies])))))
    (testing "an unqualified close writes nothing"
      (is (nil? (sut/retain! root {:run-record (assoc-in qualified [:failure :stage] :build)
                                   :construction construction
                                   :mission-source-sha256 mission-sha}))))))

(deftest excluding-the-only-policy-is-typed
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "wm-policy-satisfaction" (make-array java.nio.file.attribute.FileAttribute 0)))
        one (assoc-in field [:families 0 :policies]
                      [{:policy-id policy-id :cascade {:nodes []}}])]
    (sut/retain! root {:run-record qualified :construction construction
                       :mission-source-sha256 mission-sha})
    (let [family (get-in (sut/exclude-satisfied one root {mission mission-sha}) [:families 0])]
      (is (= :failed (:status family)))
      (is (empty? (:policies family)))
      (is (= :policy-already-satisfied (-> family :failures last :kind))))))
