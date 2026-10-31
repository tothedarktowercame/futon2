(ns futon2.aif.wm.adc46631-reviewer-refusal-test
  "Live pin: run adc46631 (2026-10-31), reviewer job
  invoke-1793473761172-8882-652e3bf0 refused with
  {\"accepted\": false, \"accepted-by\": null, ...reason...} after finding
  hole 0's consumes/produces claim diverged from the committed source. The
  handler copied the nil accepted-by onto the author answer (all three pins
  matched) and validate-answer closed the click as :answer-reviewer-mismatch.
  A review refusal must close typed as a review refusal with the reason
  preserved."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.meta-terminal-injury :as terminal]
            [futon2.aif.wm.cascade-patch-delivery :as delivery]))

(def requisition (edn/read-string (slurp "test/resources/m2c/requisition.edn")))
(def refusal-reason
  "Hole [\"cascade-construction/read-what-exists-first\" 0] in the author
  answer declares consumes=unrecorded-cascade-repair-transition but the
  committed source declares consumes=bare-mission-identifier.")

(defn clause [_hole-id]
  {:operation "apply-pattern" :capability :wm/pattern-apply
   :completion-check "clause-applied"})

(defn held-error []
  (assoc (select-keys requisition [:cascade-digest :phase :holes])
         :kind :wm/open-cascade-execution-error
         :attempts []
         :resume-port {:retry (fn [ctx] {:cascade-digest (:cascade-digest ctx)})}))

(defn exercise [reviewer-fn]
  (let [seen-req (atom nil)
        clauses (into {} (map (fn [h] [(:hole-id h) (clause h)])
                              (:holes requisition)))
        handler (runtime/open-cascade-resume-handler
                 {:author "wm-author" :reviewer "wm-reviewer"
                  :open-cascade-author-answer-fn
                  (fn [issued]
                    (let [req (or (:requisition issued) issued)]
                      (reset! seen-req req)
                      (cond-> {:requisition-id (:requisition-id req)
                               :cascade-digest (:cascade-digest req)
                               :by "wm-author"
                               :decisions (into {} (map (fn [h] [(:hole-id h) (clause h)])
                                                        (:holes requisition)))
                               :patch {:namespace "futon2.aif.wm.authored.req-b9e453fd"
                                       :var "decisions"
                                       :source-digest (apply str (repeat 64 "7"))}}
                        ;; the revision round resubmits a CHANGED answer
                        (:objection issued) (assoc :revision "round-1"))))
                  :open-cascade-reviewer-answer-fn (fn [_] (reviewer-fn @seen-req))
                  :open-cascade-patch-loader (fn [_] (fn [_] clauses))})]
    (with-redefs [delivery/check-delivery (fn [_] {:status :delivered})]
      (let [result (handler (held-error))]
        {:req @seen-req :result result}))))

(defn refusal-reviewer-response [req]
  ;; The adc46631 reply shape: all pins match the author answer, but an
  ;; explicit refusal with a nil accepted-by and a reason.
  {:state "done"
   :job-id "reviewer-job-adc46631"
   :result {:accepted false :accepted-by nil
            :requisition-id (:requisition-id req)
            :cascade-digest (:cascade-digest req)
            :patch {:source-digest (apply str (repeat 64 "7"))}
            :reason refusal-reason}})

(deftest explicit-reviewer-refusal-closes-typed-not-as-mismatch
  ;; With a reason the refusal now runs ONE objection round: the same
  ;; reviewer refuses the revision too, and the second refusal closes the
  ;; existing typed kind carrying {:objection :revision :verdict}.
  (let [{:keys [req result]}
        (exercise (fn [req] (refusal-reviewer-response req)))]
    (is (= :error (:status result)) (pr-str result))
    (is (= :open-cascade-review-refused (:kind result)) (pr-str result))
    (is (= "wm-reviewer" (:reviewer result)))
    (is (= "reviewer-job-adc46631" (:reviewer-job result)))
    (is (= refusal-reason (:reason result)))
    (is (= (:requisition-id req) (:requisition-id result)))
    ;; all three legs of the round are retained
    (is (= refusal-reason (get-in result [:objection :reason])))
    (is (= false (get-in result [:objection :review :accepted])))
    (is (= :submitted (:status (:revision result))) (pr-str (:revision result)))
    (is (= false (get-in result [:verdict :accepted])))))

(deftest refusal-without-reason-still-typed
  ;; accepted false with no reason key is still an explicit refusal; the
  ;; reason field is simply absent, never invented.
  (let [{:keys [result]}
        (exercise (fn [req] (update (refusal-reviewer-response req)
                                    :result dissoc :reason)))]
    (is (= :open-cascade-review-refused (:kind result)) (pr-str result))
    (is (nil? (:reason result)))))

(deftest accepting-reviewer-still-resumes
  ;; Regression: an accepting reviewer reply must pass the review seam and
  ;; reach the resume port; the refusal branch must not fire on
  ;; :accepted true.  The fixture retry port cannot compile a cascade, so
  ;; the run stops typed at :resume-incomplete AFTER acceptance — the stop
  ;; line proves the refusal branch did not intercept the accept path.
  (let [accepting (fn [req]
                    {:state "done" :job-id "reviewer-job-ok"
                     :result {:accepted true :accepted-by "wm-reviewer"
                              :requisition-id (:requisition-id req)
                              :cascade-digest (:cascade-digest req)
                              :patch {:source-digest (apply str (repeat 64 "7"))}}})
        {:keys [result]} (exercise accepting)]
    (is (= :error (:status result)) (pr-str result))
    (is (= :resume-incomplete (:kind result)) (pr-str result))))

(deftest a-review-refusal-reaches-the-record-as-a-non-injury
  (let [refusal {:status :error :kind :open-cascade-review-refused
                 :reviewer "wm-reviewer" :reviewer-job "invoke-review"
                 :reason refusal-reason}
        failure (#'runner/run-record-failure
                 {:data (#'runner/held-resume-data
                         {:failure-kind :open-cascade-review-refused
                          :downstream-refusal refusal}
                         "held" {:repair/id "finding"})})]
    (is (= "invoke-review" (get-in failure [:detail :reviewer-job])))
    (is (= :open-cascade-review-refused
           (get terminal/explicit-non-injury-kinds (:kind failure))))))
