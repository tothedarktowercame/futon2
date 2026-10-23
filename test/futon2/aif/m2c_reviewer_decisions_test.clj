(ns futon2.aif.m2c-reviewer-decisions-test
  "Live pin: run fd615a2a, author job2583 and reviewer job2585. Raw replies
  are verbatim; only the patch loader and retry port are fixtures."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.handoff-contract :as handoff]
            [futon2.aif.wm.open-cascade-resume :as resume]))

(def requisition (edn/read-string (slurp "test/resources/m2c/requisition.edn")))
(def author-reply (slurp "test/resources/m2c/author-reply.txt"))
(def reviewer-reply (slurp "test/resources/m2c/reviewer-reply.txt"))
(defn envelope [text] {:state "done" :result text})

(defn exercise [reviewer-response]
  (let [validation (atom nil)
        validate resume/validate-answer
        error (assoc (select-keys requisition [:cascade-digest :phase :holes])
                     :kind :wm/open-cascade-execution-error
                     :attempts []
                     :resume-port {:retry (fn [ctx]
                                            {:cascade-digest (:cascade-digest ctx)})})
        handler (runtime/open-cascade-resume-handler
                 {:author "zai-2" :reviewer "zai-3"
                  :open-cascade-author-answer-fn (fn [_] (envelope author-reply))
                  :open-cascade-reviewer-answer-fn (fn [_] reviewer-response)
                  :open-cascade-patch-loader (fn [_] (fn [_] {:fixture true}))})]
    (with-redefs [resume/validate-answer
                  (fn [req answer]
                    (let [v (validate req answer)] (reset! validation v) v))]
      {:result (handler error) :validation @validation})))

(deftest live-review-accepts-authors-decisions-without-transcription
  (let [{:keys [result validation]} (exercise (envelope reviewer-reply))]
    (is (= :accepted (:status validation)) (pr-str validation))
    (is (= 27 (count (:decisions validation))))
    (is (= (set (map :hole-id (:holes requisition)))
           (set (keys (:decisions validation)))))
    (is (= :resumed (:status result)))))

(deftest mismatched-patch-is-not-composed
  (let [reviewed (handoff/response-map (envelope reviewer-reply))
        {:keys [result]} (exercise (assoc-in reviewed [:patch :source-digest]
                                            (apply str (repeat 64 "0"))))]
    (is (= :error (:status result)))
    (is (= :answer-holes-unresolved (:kind result)))))

(deftest reviewer-identity-remains-required
  (let [reviewed (handoff/response-map (envelope reviewer-reply))]
    (doseq [answer [(dissoc reviewed :accepted-by)
                    (assoc reviewed :accepted-by "other-seat")]]
      (is (= :answer-reviewer-mismatch
             (get-in (exercise answer) [:result :kind]))))))
