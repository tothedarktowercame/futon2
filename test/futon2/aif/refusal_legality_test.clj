(ns futon2.aif.refusal-legality-test
  (:require [clojure.set]
            [clojure.test :refer [deftest is]]
            [futon2.aif.meta-terminal-injury]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.refusal-legality :as sut]))

(deftest nonempty-field-not-exhausted
  (is (false? (sut/loop-stop-legal?
               {:kind :work-exhausted :scoredField [:candidate]
                :operatorSignal false}))))

(deftest unexcluded-not-legal
  (is (false? (sut/click-legal?
               {:kind :already-satisfied :target :t
                :satisfactionMeasured true :ambiguityCited false
                :questionInBrief false :harmRuleMatched false
                :exclusionRecorded false}))))

(deftest question-needs-brief
  (is (false? (sut/click-legal?
               {:kind :legitimate-question :target :t
                :satisfactionMeasured false :ambiguityCited true
                :questionInBrief false :harmRuleMatched false
                :exclusionRecorded true}))))

(deftest unlisted-is-debugger-error
  (is (= :debugger-error (sut/disposition {:refusal :other}))))

(deftest retained-scored-field-preserves-absence
  (is (nil? (sut/retained-scored-field {}))))

(deftest retained-scored-field-keeps-unrefused-candidates
  (is (= ["M-b"]
         (sut/retained-scored-field
          {:outer/selection
           {:outer-field/admitted ["E-a" "M-b"]
            :reading-reselection {:refusals [{:target "E-a"}]}}}))))

(deftest retained-scored-field-can-be-exhausted-by-in-click-refusals
  (is (= []
         (sut/retained-scored-field
          {:outer/selection
           {:outer-field/admitted ["E-a"]
            :reading-reselection {:refusals [{:target "E-a"}]}}}))))

(deftest evidence-for-close-rejects-absent-scored-field
  (let [e (sut/evidence-for-close :abstained {:failure-kind :empty-field}
                                  nil false)]
    (is (= :scored-field-carrier-absent (get-in e [:evidence :reason])))
    (is (= :debugger-error (sut/disposition e)))))

(deftest evidence-for-close-checks-retained-field
  (is (= :debugger-error
         (-> (sut/evidence-for-close :abstained {:failure-kind :empty-field}
                                     ["M-work"] false)
             sut/disposition)))
  (is (= :loop-stop
         (-> (sut/evidence-for-close :abstained {:failure-kind :empty-field}
                                     [] false)
             sut/disposition))))

(deftest evidence-for-close-requires-retained-operator-signal
  (is (= :loop-stop
         (-> (sut/evidence-for-close :cancelled
                                     {:failure-kind :debugger/aborted
                                      :debugger/restart :abort}
                                     nil true)
             sut/disposition)))
  (is (= :debugger-error
         (-> (sut/evidence-for-close :cancelled
                                     {:failure-kind :operator-cancelled}
                                     nil false)
             sut/disposition))))

(deftest work-outcome-has-no-refusal-evidence
  (is (nil? (sut/evidence-for-close :grounded-change {} nil false))))

(deftest measured-c1-with-digest-carrier-is-pending-exclusion
  (let [result (sut/resolve-close-refusal
                true (fn [_] (throw (AssertionError. "must not hold")))
                :already-satisfied
                {:failure-kind :already-satisfied
                 :mission-observation {:status :admitted}
                 :mission-source-sha256 "mission"}
                (constantly nil) false)]
    (is (= :pending-exclusion
           (get-in result [:data :refusal/disposition :disposition])))
    (is (= :C1 (get-in result [:data :refusal/disposition :case])))))

(deftest c1-without-measurement-or-digest-remains-debugger-error
  (doseq [data [{:failure-kind :already-satisfied
                 :mission-source-sha256 "mission"}
                {:failure-kind :already-satisfied
                 :mission-observation {:status :admitted}}]]
    (is (= :debugger-error
           (get-in (sut/resolve-close-refusal false nil :already-satisfied data
                                              (constantly nil) false)
                   [:data :refusal/disposition :disposition])))))

(def valid-harm-action
  {:operation :git-push :repo "/repo" :remote "origin"
   :ref "refs/heads/main" :force? true})

(deftest matched-c3-with-digest-carrier-is-pending-exclusion
  (let [result (sut/resolve-close-refusal
                true (fn [_] (throw (AssertionError. "must not hold")))
                :harm-declined
                {:mission-source-sha256 "mission"
                 :harm-match {:rule :H2 :declined-action valid-harm-action
                              :declined-action-digest (identity/digest valid-harm-action)}}
                (constantly nil) false)]
    (is (= :pending-exclusion
           (get-in result [:data :refusal/disposition :disposition])))
    (is (= :C3 (get-in result [:data :refusal/disposition :case])))))

(deftest invalid-c3-match-remains-debugger-error
  (doseq [harm-match [nil
                      {:rule :H9 :declined-action valid-harm-action
                       :declined-action-digest (identity/digest valid-harm-action)}
                      {:rule :H2 :declined-action (assoc valid-harm-action :force? false)
                       :declined-action-digest (identity/digest valid-harm-action)}]]
    (is (= :debugger-error
           (get-in (sut/resolve-close-refusal
                    false nil :harm-declined
                    {:mission-source-sha256 "mission" :harm-match harm-match}
                    (constantly nil) false)
                   [:data :refusal/disposition :disposition])))))

(deftest plain-guardrail-refusal-remains-debugger-error
  (is (= :debugger-error
         (get-in (sut/resolve-close-refusal false nil :guardrail-refusal
                                            {:refusal-reason ":blocked"}
                                            (constantly nil) false)
                 [:data :refusal/disposition :disposition]))))

(deftest close-refusal-controller-restarts
  (let [calls (atom 0)
        result (sut/resolve-close-refusal
                true (fn [_] (if (= 1 (swap! calls inc))
                               {:action :retry}
                               {:action :continue}))
                :already-satisfied {:failure-kind :already-satisfied} (constantly nil) false)]
    (is (= 2 @calls))
    (is (= :debugger-error (get-in result [:data :refusal/disposition :disposition])))
    (is (= :continue (get-in result [:data :refusal/restart])))))

(deftest discharged-abort-close-does-not-raise-a-second-decision
  ;; Held run 2026-11-06-73839343 (repair wm-click-loop-repair-b310a7df):
  ;; the operator aborted the occurrence-square-authoring phase stop; the
  ;; ordinary typed failure reached close, no classification matched, and the
  ;; close demanded a SECOND operator decision for the same discharged
  ;; condition. With the debugger's retained discharge proof on the close
  ;; data, the close persists as the :continue restart would, and the hold
  ;; fires only when no decision was made.
  (let [await (fn [_] (throw (ex-info "second decision must not be demanded" {})))
        result (sut/resolve-close-refusal
                true await
                :incomplete
                {:failure-kind :occurrence-square-answer-invalid
                 :failure-stage :occurrence-square-authoring
                 :error-data {:outcome :incomplete
                              :failure-kind :occurrence-square-answer-invalid
                              :debugger/discharged? true
                              :debugger/restart :abort}}
                (constantly nil) false)]
    (is (= :incomplete (:outcome result)))
    (is (= :debugger-error
           (get-in result [:data :refusal/disposition :disposition]))
        "the retained classification still says what it was")
    (is (= :continue (get-in result [:data :refusal/restart]))
        "the close proceeds as a decided continue, not a fresh hold"))
  ;; without the retained proof the hold still fires exactly once
  (let [calls (atom 0)]
    (sut/resolve-close-refusal
     true (fn [_] (swap! calls inc) {:action :abort})
     :incomplete
     {:failure-kind :occurrence-square-answer-invalid
      :error-data {:failure-kind :occurrence-square-answer-invalid}}
     (constantly nil) false)
    (is (= 1 @calls))))

(deftest close-refusal-controller-use-value-and-abort
  (let [used (sut/resolve-close-refusal
              true (constantly {:action :use-value
                                :value {:outcome :grounded-change :data {:kept true}}})
              :guardrail-refusal {} (constantly nil) false)
        aborted (sut/resolve-close-refusal
                 true (constantly {:action :abort})
                 :guardrail-refusal {} (constantly nil) false)]
    (is (= :grounded-change (:outcome used)))
    (is (true? (get-in used [:data :kept])))
    ;; bad case: the substituted work outcome must still show it came from
    ;; the debugger and what it replaced
    (is (= :use-value (get-in used [:data :refusal/restart])))
    (is (= :guardrail-refusal (get-in used [:data :refusal/replaced :outcome])))
    (is (= :debugger/aborted (get-in aborted [:data :failure-kind])))))

(deftest detached-close-retains-disposition-and-work-is-unchanged
  (is (= :debugger-error
         (get-in (sut/resolve-close-refusal false nil :already-satisfied {} (constantly nil) false)
                 [:data :refusal/disposition :disposition])))
  (is (= {:outcome :grounded-change :data {:byte :same}}
         (sut/resolve-close-refusal false nil :grounded-change {:byte :same} (constantly nil) false))))

(deftest outcome-outside-the-image-is-validated
  ;; `outcome-from` passes unknown thrown outcomes through; they must not
  ;; be mistaken for work.
  (is (= {:refusal :other} (sut/evidence-for-close :something-new {} nil false)))
  (is (= :debugger-error
         (sut/disposition (sut/evidence-for-close :something-new {} nil false)))))

(deftest outcome-sets-partition-the-close-image
  (is (= (set (keys futon2.aif.meta-terminal-injury/close-outcome-image))
         (into sut/work-outcomes sut/no-work-outcomes)))
  (is (empty? (clojure.set/intersection sut/work-outcomes sut/no-work-outcomes))))

(deftest close-refusal-condition-retains-original-failure
  ;; bad case from repair wm-click-loop-repair-1b5608db (held run
  ;; 2026-11-05-6d57248a): a :selected-target-reading-repair-threw
  ;; no-selection close held with :evidence nil and nothing naming the
  ;; target or the retained reading record, so the operator debugger page
  ;; was causeless.  The condition must carry the original failure's
  ;; retained context.
  (let [held-condition (atom nil)
        result (sut/resolve-close-refusal
                true (fn [condition]
                       (reset! held-condition condition)
                       {:action :abort})
                :no-selection
                {:failure-kind :selected-target-reading-repair-threw
                 :failure-stage :selected-target-reading
                 :target "M-held"
                 :error-data {:selected-target-reading
                              {:status :refused
                               :kind :selected-target-reading-repair-threw
                               :message "Cannot read the excerpt"}}}
                (constantly nil) false)]
    (is (= :cancelled (:outcome result)))
    (is (= :illegal-refusal (:kind @held-condition)))
    (is (= :selected-target-reading-repair-threw
           (:original-kind @held-condition)))
    (is (= {:failure-stage :selected-target-reading
            :target "M-held"
            :error-data {:selected-target-reading
                         {:status :refused
                          :kind :selected-target-reading-repair-threw
                          :message "Cannot read the excerpt"}}}
           (:original-failure @held-condition)))))

(deftest close-refusal-retry-rereads-the-carrier
  ;; bad case: an empty-field abstention held because the field carrier was
  ;; absent; the operator repairs the carrier live and retries.  The retry
  ;; must see the repaired field, not the one read before the hold.
  (let [field (atom nil)
        result (sut/resolve-close-refusal
                true (fn [_] (reset! field []) {:action :retry})
                :abstained {:failure-kind :empty-field} #(deref field) false)]
    (is (= :loop-stop (get-in result [:data :refusal/disposition :disposition])))
    (is (nil? (get-in result [:data :refusal/restart])))))
