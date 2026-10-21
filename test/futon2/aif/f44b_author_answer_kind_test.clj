(ns futon2.aif.f44b-author-answer-kind-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runtime :as runtime]))

(def held-error
  {:kind :wm/open-cascade-execution-error
   :cascade-digest "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
   :holes [{:hole-id :f44b :owner :wm :question :operation}]
   :phase :compile
   :attempts []
   :resume-port {:retry (fn [_] {:outcome :not-called})}})

(deftest invalid-author-answer-does-not-contact-the-reviewer
  ;; F44a accepts prose around one unfenced object.  Two top-level objects are
  ;; still intentionally undecodable, so this is the author-side bad case.
  (let [reviewer-called? (atom false)
        handler (runtime/open-cascade-resume-handler
                 {:author "wm-author"
                  :reviewer "wm-reviewer"
                  :open-cascade-author-answer-fn
                  (fn [_]
                    {:state "done"
                     :job-id "author-job-f44b"
                     :result "Author report follows.\n{\"one\":1}\n{\"two\":2}"})
                  :open-cascade-reviewer-answer-fn
                  (fn [_]
                    (reset! reviewer-called? true)
                    {:state "done" :result "must not be called"})})
        result (handler held-error)]
    (is (= :error (:status result)))
    (is (= :open-cascade-author-answer-invalid (:kind result)))
    (is (= "wm-author" (:author result)))
    (is (= "author-job-f44b" (:author-job result)))
    (is (false? @reviewer-called?))))

(deftest invalid-reviewer-answer-retains-the-reviewer-identity
  (let [handler (runtime/open-cascade-resume-handler
                 {:author "wm-author"
                  :reviewer "wm-reviewer"
                  :open-cascade-author-answer-fn (fn [_] {:author :answer})
                  :open-cascade-reviewer-answer-fn
                  (fn [_]
                    {:state "done"
                     :job-id "reviewer-job-f44b"
                     :result "prose only"})})
        result (handler held-error)]
    (is (= :error (:status result)))
    (is (= :open-cascade-review-answer-invalid (:kind result)))
    (is (= "wm-reviewer" (:reviewer result)))
    (is (= "reviewer-job-f44b" (:reviewer-job result)))))

(deftest uncontacted-author-is-not-reported-as-a-direct-answer
  ;; designated-agent-answer-fn answers :state :not-contacted when the seat is
  ;; not invoke-ready; response-map decodes that to nil like a refused answer.
  (let [reviewer-called? (atom false)
        handler (runtime/open-cascade-resume-handler
                 {:author "wm-author"
                  :reviewer "wm-reviewer"
                  :open-cascade-author-answer-fn
                  (fn [_]
                    {:state :not-contacted
                     :reason :designated-author-unavailable
                     :author "wm-author"})
                  :open-cascade-reviewer-answer-fn
                  (fn [_]
                    (reset! reviewer-called? true)
                    {:state "done" :result "must not be called"})})
        result (handler held-error)]
    (is (= :open-cascade-author-answer-invalid (:kind result)))
    (is (= {:status :absent
            :reason :not-contacted
            :contact-reason :designated-author-unavailable}
           (:author-job result)))
    (is (false? @reviewer-called?))))

(deftest directly-injected-reviewer-answer-that-does-not-decode-is-a-direct-absence
  (let [handler (runtime/open-cascade-resume-handler
                 {:author "wm-author"
                  :reviewer "wm-reviewer"
                  :open-cascade-author-answer-fn (fn [_] {:author :answer})
                  :open-cascade-reviewer-answer-fn (fn [_] nil)})
        result (handler held-error)]
    (is (= :open-cascade-review-answer-invalid (:kind result)))
    (is (= "wm-reviewer" (:reviewer result)))
    (is (= {:status :absent :reason :direct-answer} (:reviewer-job result)))))
