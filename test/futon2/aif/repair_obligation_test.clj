(ns futon2.aif.repair-obligation-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.repair-obligation :as repair]))

(defn- temp-root []
  (let [f (java.io.File/createTempFile "wm-repair-" "")]
    (.delete f)
    (.mkdirs f)
    (.getPath f)))

(deftest finding-remains-open-until-grounded-successor-resolution
  (let [root (temp-root)
        finding (repair/record-review-failure!
                 root {:attempt-id "failed-1"
                       :target :sorry/g2
                       :commit "bad123"
                       :selected-entry {:action {:type :address-sorry
                                                 :target :sorry/g2}}
                       :reviewer "codex-7"
                       :review-job "review-1"
                       :review-verdict :request-changes
                       :review-text "provenance gate is optional"})]
    (is (= [finding] (repair/open-obligations root)))
    (testing "tests or prose without a grounded witness cannot clear the line"
      (is (thrown? clojure.lang.ExceptionInfo
                   (repair/resolve! root finding
                                    {:attempt-id "repair-1" :commit "good456"
                                     :reviewer "codex-7" :review-job "review-2"
                                     :witness {:resolved? true :dial-moved? false}}))))
    (repair/resolve! root finding
                     {:attempt-id "repair-1" :commit "good456"
                      :reviewer "codex-7" :review-job "review-2"
                      :witness {:resolved? true :dial-moved? true}})
    (is (empty? (repair/open-obligations root)))))
