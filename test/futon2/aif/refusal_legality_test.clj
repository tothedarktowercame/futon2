(ns futon2.aif.refusal-legality-test
  (:require [clojure.set]
            [clojure.test :refer [deftest is]]
            [futon2.aif.meta-terminal-injury]
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
