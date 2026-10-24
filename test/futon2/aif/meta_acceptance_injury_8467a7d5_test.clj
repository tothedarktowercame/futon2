(ns futon2.aif.meta-acceptance-injury-8467a7d5-test
  "Acceptance pin for injury/8467a7d5b6b4a3203d9053561c1d0d59f5fa1c1d.

  Pinned run record: /home/joe/code/futon2/data/wm-runs/
  tick-run-record-2026-10-23-90ca5944-3d52-4bd6-940c-fe2f6c2c8c6b.edn
  sha256 976d1bfc7983b3cc234dbc5a25ae848e0c73b018fa2076940b31bbfc5dcdc5b9.

  The injury: at :reviewer-wait for target M-war-machine-wiring the
  selection-time token-outcome prediction never reached :status :frozen
  (freeze-prediction refused :prediction-input-unavailable; the retained
  [:decision] carries :action and a :selection-certificate with a complete
  loop-node-exercise but no :token-belief-stage). compare-outcomes therefore
  refused, selected-want accounting refused
  :selected-want-after-observation-missing
  {:comparison-status :refused}, and the falsifier receipt (receipt/id
  00ec3c7b7d6cdf4f12730c6b972c6857808b57bb29eee47b6e1d6e9ad6b23215) failed
  [:selected-want-accounting :evidence-authority], vetoing reviewer zai-3's
  FULL_LOOP_REVIEW: APPROVE with a passing review gate and a corroborated
  artifact binding to commit 249ce8a. The retained failure recorded
  :cause {:absent :no-cause} and pruned :failed-checks from its detail, so
  the failed-check identity lived only inside the receipt the record
  dropped. This test drives the real production functions from the
  diagnosis's :production-functions with pinned literals; it fails on
  injured code at the classification attribution pin and passes once the
  terminal injury classification itself retains the falsifier's
  failed-check identity. No stubs."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.token-outcome :as token-outcome]
            [futon2.aif.selected-want-outcome :as selected-want-outcome]
            [futon2.aif.reviewer-falsifier :as falsifier]
            [futon2.aif.wm.loop-node-exercise :as loop-node-exercise]
            [futon2.aif.wm.terminal-receipt :as terminal-receipt]
            [futon2.aif.meta-terminal-injury :as meta-terminal-injury]))

;; Literals pinned from the run record and its attempt evidence
;; (data/wm-full-loop-machinery-149/wm-contract-machinery-149-v1/
;; attempt-001/002-selection.edn and 005-build.edn).
(def target "M-war-machine-wiring")
(def commit "249ce8a6685edbbb23b019d2661d5dc2c2198e90")
(def policy-id "M-war-machine-wiring/policy-2629917c87a61978be8f")
(def wants
  [:hole/h3b6587f68a09 :hole/h367d19f57c3e :hole/h2ec21424642e
   :hole/h459471323b5c :hole/hbc31d1f47ff8 :hole/h66d04c790126])

;; The selected decision exactly as retained in the run record's [:decision]:
;; a cascade-candidate action with a nonempty want list, and a selection
;; certificate whose only retained stage content is the complete
;; loop-node-exercise — no :token-belief-stage for the target.
(def pinned-decision
  {:action {:kind :cascade-candidate
            :id policy-id
            :target target
            :want wants}
   :selection-certificate
   {:loop-node-exercise {:schema :wm/loop-node-exercise-v1
                         :status :complete}}})

;; The prediction as persisted in 002-selection.edn: refused at selection.
(def pinned-prediction
  {:schema :wm/token-outcome-prediction-v1
   :target target
   :status :refused
   :reason :prediction-input-unavailable})

;; The comparison that reached terminal accounting (002-selection/005-build:
;; comparison-status :refused under the unfrozen prediction).
(def pinned-comparison {:status :refused :reason :prediction-unavailable})

;; Artifact binding as retained in the injury record: corroborates? true,
;; commit claimed in the author window.
(def artifact-binding
  {:repo "/home/joe/code/futon5a"
   :commit commit
   :text-artifact-sha commit
   :observed-head commit
   :pre-dispatch-head "0382af2386c8afd5df4b9fa07e8538b889162cbe"
   :in-author-window? true
   :corroborates? true})

;; Review gate as observed: the failed-review-gate branch was NOT taken, so
;; the gate passed and only the falsifier refused.
(def review-gate
  {:required? true
   :executed? true
   :tool-events 3
   :execution-source :job-events
   :passed? true})

(defn injured-inputs [outcomes]
  {:target target
   :commit commit
   :work-evidence {:kind :ordinary-cascade}
   :mission-standing
   {:status :consistent
    :source {:path "/home/joe/code/futon5a/holes/missions/M-war-machine-wiring.md"
             :sha256 "f75873416ac1f328698a2d2292aa5515366bf722ebc232e2098d61ea847a4cdf"}
    :declarations []}
   :selected-want-outcomes outcomes
   :disposition nil
   :artifact-binding artifact-binding
   :review-gate review-gate})

(defn injured-outcomes []
  (selected-want-outcome/receipt
   {:selected-action (assoc (:action pinned-decision) :target target)
    :token-comparison pinned-comparison}))

(deftest unfrozen-prediction-veto-of-approving-review-is-attributed-typed-failure
  (testing "the injury's origin: the selection-time prediction never freezes"
    (let [prediction (token-outcome/freeze-prediction pinned-decision)]
      (is (= :refused (:status prediction)))
      (is (= :prediction-input-unavailable (:reason prediction)))
      (is (= target (:target prediction)))))
  (testing "the injury's reason: terminal selected-want accounting refuses"
    (let [outcomes (injured-outcomes)]
      (is (= :refused (:status outcomes)))
      (is (= :selected-want-after-observation-missing (:reason outcomes)))
      (is (= :refused (get-in outcomes [:details :comparison-status])))))
  (testing "the falsifier refuses under an approving, corroborated review"
    (let [inputs (injured-inputs (injured-outcomes))
          receipt (falsifier/receipt inputs)
          verification (falsifier/verify receipt inputs)]
      (is (= :refused (:status receipt)))
      (is (= [:selected-want-accounting :evidence-authority]
             (:failed-checks receipt)))
      (is (= :selected-want-after-observation-missing
             (get-in receipt [:checks :selected-want-accounting :reason])))
      (is (= :revision-authority-missing
             (get-in receipt [:checks :evidence-authority :reason])))
      (is (= [] (get-in receipt [:checks :evidence-authority
                                 :evidence :measurements])))
      (is (= :refused (:status verification)))
      (testing "approval prose still cannot substitute for the falsifier"
        (is (false? (falsifier/approved?
                     {:review-state "done"
                      :review-verdict :approve
                      :review-gate review-gate
                      :falsifier-verification verification})))))) 
  (testing "terminal routing records exactly one typed failure naming the target"
    (let [record {:run/id "run-2026-10-23-90ca5944"
                  :startedAt "2026-10-23T16:39:25.665071388Z"
                  :selection-event {:event :cascade-selected :target target}
                  :failure {:kind :reviewer-falsifier-failed
                            :stage :reviewer-wait}}
          receipt (terminal-receipt/terminal-receipt record)]
      (is (= :failure (:kind receipt)))
      (is (= :reviewer-falsifier-failed (:failure-kind receipt)))
      (is (= target (:target receipt)))))
  (testing "loop-node retention refusals are evidence, not a veto"
    (let [loop-receipt (loop-node-exercise/receipt {})]
      (is (= :incomplete (:status loop-receipt)))
      (is (= {:present 0 :bypassed 0 :refused 4} (:counts loop-receipt)))))
  (testing "REPAIR PIN — the terminal injury classification retains the falsifier's failed-check identity; on injured code classify emits no :failed-checks and the retained injury carried :cause {:absent :no-cause}"
    (let [inputs (injured-inputs (injured-outcomes))
          receipt (falsifier/receipt inputs)
          verification (falsifier/verify receipt inputs)
          record {:run/id "run-2026-10-23-90ca5944"
                  :click/id "click-8467a7d5"
                  :traceWritten false
                  :failure {:kind :reviewer-falsifier-failed
                            :stage :reviewer-wait
                            :target target
                            :cause {:absent :no-cause}
                            :detail {:reviewer-falsifier-verification verification
                                     :reviewer-falsifier receipt}}}
          pin {:path "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-23-90ca5944-3d52-4bd6-940c-fe2f6c2c8c6b.edn"
               :sha256 "976d1bfc7983b3cc234dbc5a25ae848e0c73b018fa2076940b31bbfc5dcdc5b9"}
          classification (meta-terminal-injury/classify record pin)]
      (is (= :classified (:status classification)))
      (is (= :injury (:classification classification)))
      (is (= :reviewer-falsifier-failed (:failure-kind classification)))
      (is (= [:selected-want-accounting :evidence-authority]
             (:failed-checks classification))))))
