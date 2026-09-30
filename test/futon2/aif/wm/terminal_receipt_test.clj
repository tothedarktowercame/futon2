(ns futon2.aif.wm.terminal-receipt-test
  "PROOF-2b ⟨0⟩0 acceptance 1: three planted records (selected-and-enacted,
  abstained, exception-before-selection) each yield exactly one terminal
  receipt of the right kind, and planted bad records — both kinds, or
  neither — are rejected by the builder. Planted controls on live code; no
  substrate reads, no writes."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.wm.terminal-receipt :as tr]))

(def selected-record
  "A selected-and-enacted record in the shape persist-run-record! writes:
  cascade-selected selection event, a decision with a selection-law action
  and g-term-decomposition, the close's outcome, a d-task revision pair,
  and participants."
  {:run/id "2026-09-23-1790136186"
   :click/id "wm-click-8b290466-0cdb-4c1f-aca5-5bbbd4268cad"
   :startedAt "2026-09-23T04:11:44.820127088Z"
   :selection-event {:event :cascade-selected
                     :target "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade"}
   :decision {:selection-law {:per-policy-argmax
                              {:action {:kind :cascade-candidate :id :C2}}}
              :g-term-decomposition {:status :present :terms [:R1 :R2]}}
   :outcome :grounded-change
   :d-task-enactment {:verification {:revision-pair
                                     {:after "0798f96ad2082a8f80c8a"}}}
   :participants {:roles {:reviewer-of-record {:status :present :identity "codex-13"}}}
   :failure {:absent :no-failure}})

(def abstained-record
  "An abstained record: no selection event, the D8/AR-16 sorry-cell
  abstention carrier on the decision, no failure kind."
  {:run/id "2026-09-29-1790701955"
   :click/id "wm-click-5c34bd6e-0fe6-466e-a4f9-9cb07e8a69c2"
   :startedAt "2026-09-29T17:15:34.408560407Z"
   :selection-event {:status :absent :reason :selection-not-reached}
   :decision {:abstention {:status :abstained
                           :targets [{:target "M-a-sorry-enterprise"
                                      :kind :universe-not-admitted
                                      :missing :universes}]}}
   :failure {:absent :no-failure}})

(def exception-record
  "A record the close wrote after an exception before selection: a failure
  with a kind, no abstention, no selection."
  {:run/id "2026-09-25-1bc6d9f0"
   :click/id "wm-click-abcd0000-0000-0000-0000-000000000000"
   :startedAt "2026-09-25T09:00:00Z"
   :selection-event {:status :absent :reason :selection-not-reached}
   :decision {}
   :failure {:kind :build-failed :stage :full-loop-close
             :error "exception before selection"}})

(deftest selected-and-enacted-yields-action-receipt
  (let [r (tr/terminal-receipt selected-record)]
    (is (= :action-receipt (:kind r)))
    (is (= (:click/id selected-record) (:click-id r)))
    (is (= "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade"
           (:target r)))
    (is (= :ticket (:target-kind r)))
    (is (= :cascade-candidate (:action-kind r)))
    (is (= (get-in selected-record [:decision :g-term-decomposition]) (:G r)))
    (is (= :grounded-change (:outcome r)))
    (is (= "0798f96ad2082a8f80c8a" (:commit r)))
    (is (= "codex-13" (:reviewer r)))))

(deftest abstained-yields-failure
  (let [r (tr/terminal-receipt abstained-record)]
    (is (= :failure (:kind r)))
    (is (= :abstained (:failure-kind r)))
    (is (= :abstained (:abstention-status r)))
    (is (= "2026-09-29-1790701955-abstained" (:id r)))
    (is (= "futon2.aif.wm.terminal-receipt/terminal-receipt" (:source r)))
    (is (= (:startedAt abstained-record) (:at r)))))

(deftest exception-before-selection-yields-failure
  (let [r (tr/terminal-receipt exception-record)]
    (is (= :failure (:kind r)))
    (is (= :build-failed (:failure-kind r)))
    (is (string? (:id r)))
    (is (= (:startedAt exception-record) (:at r)))))

(deftest a-record-that-selected-then-failed-yields-failure-naming-its-target
  ;; the 2026-09-29-1790654511 shape: cascade-selected, then :close-exception
  (let [r (tr/terminal-receipt (assoc selected-record :failure {:kind :close-exception}))]
    (is (= :failure (:kind r)))
    (is (= :close-exception (:failure-kind r)))
    (is (= (get-in selected-record [:selection-event :target]) (:target r)))
    (is (= :ticket (:target-kind r)))))

(deftest a-record-that-is-neither-yields-no-terminal-state-failure
  (let [r (tr/terminal-receipt (assoc exception-record :failure {:absent :no-failure}))]
    (is (= :failure (:kind r)))
    (is (= :no-terminal-state (:failure-kind r)))
    (is (= "2026-09-25-1bc6d9f0-no-terminal-state" (:id r)))))

(deftest attach-never-throws-and-types-an-invalid-receipt
  ;; no :click/id on a selected record -> the action receipt is ill-formed
  (let [record (tr/attach (dissoc selected-record :click/id) :grounded-change)
        r (:terminal-receipt record)]
    (is (= :failure (:kind r)))
    (is (= :terminal-receipt-invalid (:failure-kind r)))
    (is (= 64 (count (:terminal-receipt-digest record))))))

(deftest a-receipt-of-both-kinds-is-rejected
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"not exactly one kind"
                        (tr/validate-receipt {:kind :action-receipt
                                              :click-id "c" :target "M-x"
                                              :target-kind :mission
                                              :action-kind :cascade-candidate
                                              :G {} :outcome :grounded-change
                                              :failure-kind :abstained}))))

(deftest a-receipt-without-any-kind-is-rejected
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"not exactly one kind"
                        (tr/validate-receipt {}))))

(deftest a-receipt-missing-required-keys-is-rejected
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"missing required keys"
                        (tr/validate-receipt {:kind :failure :id "F"}))))

(deftest attach-puts-receipt-and-digest-on-the-record
  (let [record (tr/attach selected-record :grounded-change)
        r (:terminal-receipt record)]
    (is (= :action-receipt (:kind r)))
    (is (= 64 (count (:terminal-receipt-digest record))))
    (is (= (:terminal-receipt-digest record)
           (tr/terminal-receipt-digest r)))))

(deftest attach-works-as-persist-run-record-threads-it
  ;; the exact call shape in full_loop_runner.clj persist-run-record!
  (let [record (cond-> abstained-record true (tr/attach :abstained))]
    (is (map? record))
    (is (= :failure (get-in record [:terminal-receipt :kind])))))
