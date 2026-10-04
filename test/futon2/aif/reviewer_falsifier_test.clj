(ns futon2.aif.reviewer-falsifier-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.reviewer-falsifier :as falsifier]))

(def commit (apply str (repeat 40 "a")))
(def target "M-clean")
(def want [target "DOCUMENT"])

(defn measurement []
  {:token want
   :declared-locator {:class :C3 :repo "futon2" :sha "HEAD" :path "DOCUMENT"}
   :after-locator {:class :C3 :repo "futon2" :sha commit :path "DOCUMENT"}
   :result {:observed false :check :C3
            :evidence {:repo "futon2" :resolved-sha commit :path "DOCUMENT"}}})

(defn outcomes []
  {:schema :wm/selected-want-outcome-v1 :status :verified :receipt/id "outcome-1"
   :target target :selected-wants [want]
   :outcomes [{:target target :want want :classification :untouched
               :before {:observed false}
               :after {:observed false :measurement (measurement)}}]})

(defn inputs []
  {:target target :commit commit
   :mission-standing {:status :consistent
                      :source {:path "/missions/M-clean.md" :sha256 "source-sha"}
                      :declarations []}
   :selected-want-outcomes (outcomes)
   :artifact-binding {:repo "/repo" :commit commit :observed-head commit
                      :pre-dispatch-head (apply str (repeat 40 "b"))
                      :corroborates? true}
   :review-gate {:required? true :code-files ["src/x.clj"] :executed? true
                 :tool-events 2 :execution-source :job-events :passed? true}})

(deftest clean-partial-progress-is-verified-and-still-live
  (let [in (inputs) receipt (falsifier/receipt in)]
    (is (= :verified (:status receipt)))
    (is (= :not-applicable (get-in receipt [:checks :standing-disposition :status])))
    (is (= :still-live
           (get-in receipt [:checks :standing-disposition :evidence :expected])))
    (is (= :verified (:status (falsifier/verify receipt in))))))

(deftest approval-prose-cannot-overrule-standing-or-authority
  (testing "the evaluated click's conflicting declarations"
    (let [in (assoc (inputs) :mission-standing
                    {:status :conflict
                     :source {:path "/missions/M-self-documenting-stack.md"
                              :sha256 "pinned"}
                     :declarations [{:kind :terminal-lifecycle-declaration
                                     :text "Status: POC COMPLETE"}
                                    {:kind :unchecked-acceptance
                                     :text "- [ ] Walkthrough accepted"}]})
          receipt (falsifier/receipt in)]
      (is (= :mission-standing-conflict
             (get-in receipt [:checks :mission-standing :reason])))
      (let [verification (falsifier/verify receipt in)]
        (is (= :refused (:status verification)))
        (is (false? (falsifier/approved?
                     {:review-state "done" :review-verdict :approve
                      :review-gate (:review-gate in)
                      :falsifier-verification verification}))))))
  (testing "a measurement without immutable revision authority"
    (let [in (assoc-in (inputs)
                       [:selected-want-outcomes :outcomes 0 :after :measurement
                        :result :evidence :resolved-sha]
                       nil)
          receipt (falsifier/receipt in)]
      (is (= :revision-authority-missing
             (get-in receipt [:checks :evidence-authority :reason])))
      (let [verification (falsifier/verify receipt in)]
        (is (= :refused (:status verification)))
        (is (false? (falsifier/approved?
                     {:review-state "done" :review-verdict :approve
                      :review-gate (:review-gate in)
                      :falsifier-verification verification})))))))

(deftest receipt-mutations-do-not-self-verify
  (let [in (inputs) receipt (falsifier/receipt in)]
    (is (= :reviewer-falsifier-check-census-mismatch
           (:reason (falsifier/verify
                     (update receipt :checks dissoc :mission-standing) in))))
    (is (= :reviewer-falsifier-target-mismatch
           (:reason (falsifier/verify (assoc receipt :target "M-other") in))))
    (is (= :reviewer-falsifier-commit-mismatch
           (:reason (falsifier/verify (assoc receipt :commit "other") in))))
    (let [bad-in (assoc (inputs) :mission-standing {:status :conflict})
          bad (falsifier/receipt bad-in)
          fabricated (-> bad
                         (assoc :status :verified :failed-checks [])
                         (assoc-in [:checks :mission-standing :status] :pass))]
      (is (= :reviewer-falsifier-evidence-mismatch
             (:reason (falsifier/verify fabricated bad-in)))))))

(deftest claimed-disposition-must-match-outcomes
  (let [receipt (falsifier/receipt (assoc (inputs) :disposition :resolved))]
    (is (= :standing-disposition-mismatch
           (get-in receipt [:checks :standing-disposition :reason])))
    (is (= :refused (:status receipt)))))
