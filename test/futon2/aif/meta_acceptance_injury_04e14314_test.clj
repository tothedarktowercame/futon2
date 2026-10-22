(ns futon2.aif.meta-acceptance-injury-04e14314-test
  "Acceptance pin for injury/04e14314a0eec3aab88d8e7044be26f3ae5dfe6d.

  Pinned run record: /home/joe/code/futon2/data/wm-runs/
  tick-run-record-2026-10-22-02587641-d615-4a5b-8bc8-a3df48e2ab45.edn
  sha256 64b3eff6a27638cc429c7e7046687e3d3258756e90b9cca8a9dc63b0e32db63f.

  The injury: at :reviewer-wait the reviewer returned FULL_LOOP_REVIEW:
  APPROVE, the artifact binding corroborated commit 87d4289 in futon5a and
  the review gate passed, but the construction's selected-action for
  M-stack-geometry carried no :want tokens, so terminal selected-want
  accounting refused (:selected-want-identity-missing) and the falsifier
  refused the build. The retained failure recorded only
  {:status :refused :reason :reviewer-falsifier-applicable-check-failed}
  with :cause {:absent :no-cause} — the failed-check identity lived only
  inside the receipt's :checks/:failed-checks, which the failure detail
  omitted. This test drives the real production functions with the pinned
  literals; it fails on injured code at the attribution pin and passes
  once the verification refusal itself carries the failed-check identity.
  No stubs: every function below is the real production var from the
  diagnosis's :production-functions."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.reviewer-falsifier :as falsifier]
            [futon2.aif.selected-want-outcome :as selected-want-outcome]
            [futon2.aif.run-ending-classification :as run-ending]
            [futon2.aif.wm.loop-node-exercise :as loop-node-exercise]))

;; Literals pinned from the run record.
(def target "M-stack-geometry")
(def commit "87d4289289da83a1040f5bf97252870f33ae6378")

;; The injured selected-action exactly as the runner passed it to terminal
;; accounting: a target but no :want tokens (record: execution-outcomes
;; {:status :refused :reason :selected-want-identity-missing
;;  :details {:target "M-stack-geometry" :wants []}}).
(def injured-selected-action {:kind :cascade-candidate :target target})

;; Artifact binding as retained: corroborates? true, commit in author window.
(def artifact-binding
  {:repo "/home/joe/code/futon5a"
   :commit commit
   :text-artifact-sha commit
   :observed-head commit
   :pre-dispatch-head "9d8d6993c7b3d813aabce8e90862f2d97bbdb727"
   :in-author-window? true
   :corroborates? true})

;; Review gate as observed: the runner's failed-review-gate branch was NOT
;; taken, so the gate passed and only the falsifier refused.
(def review-gate
  {:required? true
   :code-files ["scripts/build_mission_crossref_edges.py"
                "tests/build_mission_crossref_edges_test.py"]
   :executed? true
   :tool-events 3
   :execution-source :job-events
   :passed? true})

(defn injured-inputs [outcomes]
  {:target target
   :commit commit
   :work-evidence {:kind :ordinary-cascade}
   :mission-standing {:status :consistent
                      :source {:path "/missions/M-stack-geometry.md"
                               :sha256 "pinned-source-sha"}
                      :declarations []}
   :selected-want-outcomes outcomes
   :disposition nil
   :artifact-binding artifact-binding
   :review-gate review-gate})

(defn injured-outcomes []
  (selected-want-outcome/receipt
   {:selected-action injured-selected-action
    :token-comparison {:status :not-compared}}))

(deftest approving-review-without-selected-want-identity-is-an-attributed-typed-failure
  (testing "the injury's reason: terminal selected-want accounting refuses"
    (let [outcomes (injured-outcomes)]
      (is (= :refused (:status outcomes)))
      (is (= :selected-want-identity-missing (:reason outcomes)))
      (is (= target (get-in outcomes [:details :target])))
      (is (= [] (get-in outcomes [:details :wants])))))
  (testing "the falsifier refuses under an approving, corroborated review"
    (let [inputs (injured-inputs (injured-outcomes))
          receipt (falsifier/receipt inputs)
          verification (falsifier/verify receipt inputs)]
      (is (= :refused (:status receipt)))
      (is (= [:selected-want-accounting :evidence-authority]
             (:failed-checks receipt)))
      (is (= :selected-want-identity-missing
             (get-in receipt [:checks :selected-want-accounting :reason])))
      (is (= :refused (:status verification)))
      (testing "REPAIR PIN — the retained verification refusal itself carries the failed-check identity; on injured code the refusal is {:status :refused :reason :reviewer-falsifier-applicable-check-failed} with the attribution buried in :receipt, which the failure record dropped to :cause {:absent :no-cause}"
        (is (= [:selected-want-accounting :evidence-authority]
               (:failed-checks verification))))
      (testing "approval prose still cannot substitute for the falsifier"
        (is (false? (falsifier/approved?
                     {:review-state "done"
                      :review-verdict :approve
                      :review-gate review-gate
                      :falsifier-verification verification})))))) 
  (testing "terminal routing records a typed failure, never an unknown"
    (let [close {:outcome :build-failed
                 :grounded? false
                 :artifact-only? false
                 :failure-kind :reviewer-falsifier-failed}
          classed (run-ending/classify {:close close})]
      (is (= :recorded (:status classed)))
      (is (= :known-typed-failure (:class classed)))
      (is (= :reviewer-falsifier-failed (:failure-kind classed)))))
  (testing "loop-node retention refusals are evidence, not a veto"
    (let [loop-receipt (loop-node-exercise/receipt {})]
      (is (= :incomplete (:status loop-receipt)))
      (is (= {:present 0 :bypassed 0 :refused 4} (:counts loop-receipt))))))
