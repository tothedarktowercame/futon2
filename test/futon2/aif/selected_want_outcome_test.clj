(ns futon2.aif.selected-want-outcome-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.selected-want-outcome :as outcome]
            [futon2.aif.token-outcome :as token-outcome]))

(def target "M-example")
(def document [target :document])
(def walkthrough [target :walkthrough])
(def locators {document {:kind :file-check :path "mission.md" :needle "DOCUMENT"}
               walkthrough {:kind :file-check :path "mission.md" :needle "walkthrough"}})
(def action {:target target :want [document walkthrough]
             :observation-locators locators})
(def evidence-path "test/futon2/aif/selected_want_outcome_test.clj")
(def evidence-source
  {:path evidence-path
   :sha256 (field/sha256 (.getBytes (slurp evidence-path) "UTF-8"))})

(defn comparison [observed]
  {:status :compared
   :prediction {:initial-belief {#{} 1}}
   :tokens (mapv (fn [token]
                   {:token token :observed (get observed token)
                    :measurement {:token token
                                  :declared-locator (get locators token)
                                  :after-locator (assoc (get locators token) :sha "after")
                                  :result {:observed (get observed token)
                                           :evidence {:resolved-sha "after"}}}})
                 [document walkthrough])})

(deftest honest-partial-close-classifies-each-selected-want-once
  (let [receipt (outcome/receipt {:selected-action action
                                  :token-comparison
                                  (comparison {document true walkthrough false})})]
    (is (= :verified (:status receipt)))
    (is (= [document] (get-in receipt [:by-class :reached])))
    (is (= [walkthrough] (get-in receipt [:by-class :untouched])))
    (is (= 2 (count (:outcomes receipt))))
    (is (= #{:reached :untouched} (set (map :classification (:outcomes receipt)))))))

(deftest progress-and-blocker-require-want-tied-mechanical-evidence
  (let [base {:selected-action action
              :token-comparison (comparison {document false walkthrough false})}
        progressed (outcome/receipt
                    (assoc base :progress-evidence
                           [{:token document :before 0 :after 1 :terminal 2
                             :source evidence-source}]))
        blocked (outcome/receipt
                 (assoc base :blockers
                        [{:token walkthrough :kind :operator-acceptance-missing
                          :source evidence-source}]))]
    (is (= [document] (get-in progressed [:by-class :progressed])))
    (is (= [walkthrough] (get-in blocked [:by-class :blocked])))
    (is (= :selected-want-progress-invalid
           (:reason (outcome/receipt
                     (assoc base :progress-evidence
                            [{:token document :before 0 :after 0 :terminal 2}])))))))

(deftest malformed-terminal-correspondence-refuses
  (let [base {:selected-action action
              :token-comparison (comparison {document true walkthrough false})}]
    (testing "missing and duplicate observations"
      (is (= :selected-want-census-mismatch
             (:reason (outcome/receipt
                       (update base :token-comparison update :tokens pop)))))
      (is (= :selected-want-census-mismatch
             (:reason (outcome/receipt
                       (update base :token-comparison update :tokens
                               conj (first (get-in base [:token-comparison :tokens]))))))))
    (testing "selected identity and target are exact"
      (is (= :selected-want-duplicate
             (:reason (outcome/receipt
                       (assoc-in base [:selected-action :want]
                                 [document document])))))
      (is (= :selected-want-target-mismatch
             (:reason (outcome/receipt
                       (assoc-in base [:selected-action :want 0]
                                 ["M-other" :document]))))))
    (testing "locator and after truth must be present"
      (is (= :selected-want-locator-mismatch
             (:reason (outcome/receipt
                       (assoc-in base [:token-comparison :tokens 0
                                       :measurement :declared-locator :path]
                                 "other.md")))))
      (is (= :selected-want-after-observation-missing
             (:reason (outcome/receipt
                       (assoc-in base [:token-comparison :tokens 0 :observed]
                                 {:status :missing})))))
      (is (= :selected-want-source-mismatch
             (:reason (outcome/receipt
                       (assoc-in base [:token-comparison :tokens 0
                                       :measurement :after-locator :sha]
                                 "different"))))))))

(deftest recorded-bare-wants-join-real-qualified-prediction-census
  ;; The fixture holds the selected action of run 2026-10-05-c9d25d6a and the
  ;; parts of its selection certificate the prediction reads, copied from the
  ;; run record (see the fixture's :note; the run record is not tracked in git,
  ;; so the test does not read it).
  (let [{:keys [action selection-certificate] :as fixture}
        (edn/read-string
         (slurp "test/fixtures/selected-want-outcome/2026-10-05-c9d25d6a-action.edn"))
        decision {:action action :selection-certificate selection-certificate}
        prediction (token-outcome/freeze-prediction decision)
        artifact-sha "recorded-action-test-artifact"
        measurements
        (mapv (fn [{:keys [token]}]
                (let [locator (get (:observation-locators action) token)]
                  {:token token :declared-locator locator
                   :after-locator (assoc locator :sha artifact-sha)
                   :result {:observed false
                            :evidence {:resolved-sha artifact-sha}}}))
              (:wanted prediction))
        comparison (token-outcome/compare-outcomes prediction measurements artifact-sha)
        accounting (outcome/receipt {:selected-action action
                                     :token-comparison comparison})
        qualified-wants (mapv (fn [want] [(:target action) want]) (:want action))]
    (is (= "2026-10-05-c9d25d6a" (:run-id fixture)))
    (is (every? keyword? (:want action)) "the recorded action carries bare wants")
    (is (= :frozen (:status prediction)))
    (is (= :compared (:status comparison)))
    (is (not= :selected-want-target-mismatch (:reason accounting)))
    (is (= qualified-wants (:selected-wants accounting)))
    (is (= (set qualified-wants) (set (mapv :token (:tokens comparison)))))
    (is (= (count qualified-wants) (count (:tokens comparison))))
    (is (= qualified-wants (mapv :want (:outcomes accounting))))
    (is (= :selected-want-target-mismatch
           (:reason
            (outcome/receipt
             {:selected-action (assoc-in action [:want 0] ["M-other" (first (:want action))])
              :token-comparison comparison}))))))
