(ns futon2.aif.medic-disposition-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.medic-disposition :as medic]))

(def e1 {:injury-id "a9" :entry-id "e1" :predecessor-entry-id nil
         :cause-record-id "r1" :cause-pin "p1" :active? true})
(def e2 {:injury-id "41" :entry-id "e2" :predecessor-entry-id nil
         :cause-record-id "r2" :cause-pin "p2" :active? true})
(def base {:history [e1 e2] :corrections []})
(def hist {:kind :historical-misclassification :injury-id "a9" :entry-id "e1"
           :predecessor-entry-id nil :original-record-id "r1" :original-pin "p1"
           :classifier-revision "rev-c" :replay :producer-qualified-non-injury
           :patch-id "patch-a" :reviewer "reviewer" :evaluator "evaluator"
           :evidence-pin "ev-a"})
(def genuine {:kind :genuine-repair :injury-id "41" :entry-id "e2"
              :predecessor-entry-id nil :cause-record-id "r2" :cause-pin "p2"
              :acceptance-contract "contract" :pre-replay-receipt "pre"
              :post-replay-receipt "post" :negative-control-receipt "neg"
              :patch-id "patch-b" :source-revision "rev-b"
              :evaluator "eval-b" :reviewer "review-b"
              :evidence-pin "ev-b" :original-acceptance-repaired? true})
(def allow (constantly true))
(defn req [candidate evidence] {:candidate candidate :evidence evidence})

(def e45
  {:injury-id "injury/9c6220388033cbd1fbc888cdf6bb3a5b9a3082c0"
   :entry-id "45"
   :predecessor-entry-id nil
   :cause-record-id
   "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-26-8be8ff03-5009-4f3d-8c12-5b11cfd46d92.edn"
   :cause-pin "e9a5eb355f9a6f70b41d5bdbd99e127badcd6d13e6bd7165867fa2bb9477f404"
   :active? true})

;; These five strings stand in for the correction envelope that does not yet
;; exist. Reviewer and evaluator remain distinct, as the admission requires.
(def entry45-successful-close
  {:kind :historical-misclassification
   :injury-id (:injury-id e45)
   :entry-id (:entry-id e45)
   :predecessor-entry-id nil
   :original-record-id (:cause-record-id e45)
   :original-pin (:cause-pin e45)
   :classifier-revision "classifier-revision-placeholder"
   :replay :successful-close-non-injury
   :patch-id "patch-placeholder"
   :reviewer "reviewer-placeholder"
   :evaluator "evaluator-placeholder"
   :evidence-pin "evidence-pin-placeholder"})

(deftest only-non-injury-replays-qualify
  (doseq [replay [:injury :unsupported-author-refusal :done-at-base-revision]]
    (let [candidate (assoc hist :replay replay)
          result (medic/process allow base (req candidate candidate))]
      (is (= [:refused :binding-mismatch]
             ((juxt :status :reason) result))
          (str "injury_replay_refuses / qualified_replay_nonInjury: " replay)))))

(deftest a9-successful-close-value-requires-its-exact-binding
  (let [candidate (assoc hist :replay :successful-close-non-injury)
        old-evidence (medic/process allow base (req candidate hist))
        old-authority (fn [_ c _] (= c hist))
        rejected (medic/process old-authority base (req candidate candidate))
        appended (medic/process allow base (req candidate candidate))]
    (is (= [:refused :binding-mismatch]
           ((juxt :status :reason) old-evidence))
        "a9_new_value_qualifies_only_under_binding: old evidence")
    (is (= [:refused :authority-rejected]
           ((juxt :status :reason) rejected))
        "a9_new_value_qualifies_only_under_binding: old authority")
    (is (= :appended (:status appended))
        "a9_new_value_qualifies_only_under_binding: exact binding")
    (is (= [["41" "e2"]]
           (medic/effective-active (:state appended))))
    (is (= (:history base) (get-in appended [:state :history])))))

(deftest entry45-successful-close-correction-and-reopening
  (let [state {:history [e45 e2] :corrections []}
        appended (medic/process allow state
                                (req entry45-successful-close
                                     entry45-successful-close))
        old-value (assoc entry45-successful-close
                         :replay :producer-qualified-non-injury)
        only-successful-close (fn [_ candidate _]
                                (= candidate entry45-successful-close))
        old-refused (medic/process only-successful-close state
                                   (req old-value old-value))
        reopened (assoc e45 :entry-id "46" :predecessor-entry-id "45")
        reopened-state (update (:state appended) :history conj reopened)]
    (is (= :appended (:status appended))
        "entry45_correction_qualifies_with_successful_close_replay")
    (is (= (:history state) (get-in appended [:state :history])))
    (is (= [["41" "e2"]]
           (medic/effective-active (:state appended))))
    (is (= :refused (:status old-refused)))
    (is (= [["41" "e2"] [(:injury-id e45) "46"]]
           (medic/effective-active reopened-state))
        "entry45_later_reopening_remains_active")))

(deftest both-evidence-kinds-and-certified-clear-all
  (let [partial (medic/certified-sweep allow base [(req hist hist)])
        all (medic/certified-sweep allow base [(req hist hist) (req genuine genuine)])]
    (is (:certified? partial))
    (is (= [["41" "e2"]] (medic/effective-active (:state partial))))
    (is (false? (medic/certified-clear-all? allow base [(req hist hist)])))
    (is (:certified? all))
    (is (empty? (medic/effective-active (:state all))))
    (is (medic/certified-clear-all? allow base
                                    [(req hist hist) (req genuine genuine)]))
    (is (= (:history base) (get-in all [:state :history])))))

(deftest latest-occurrence-binding-preserves-reopen-and-inactive
  (let [reopen (assoc e1 :entry-id "e3" :predecessor-entry-id "e1")
        inactive (assoc e2 :entry-id "e4" :predecessor-entry-id "e2" :active? false)
        state {:history [e1 e2 reopen inactive] :corrections []}
        stale (medic/process allow state (req hist hist))]
    (is (= :refused (:status stale)))
    (is (= :binding-mismatch (:reason stale)))
    (is (= [["a9" "e3"]] (medic/effective-active state)))))

(deftest local-cause-check-precedes-permissive-authority
  (let [wrong (assoc genuine :cause-record-id "unrelated")
        result (medic/process allow base (req wrong wrong))]
    (is (= [:refused :binding-mismatch] [(:status result) (:reason result)]))
    (is (= [] (get-in result [:state :corrections])))))

(deftest absent-and-present-false-remain-distinct
  (let [missing (medic/process allow base (req genuine nil))
        false-payload (assoc genuine :original-acceptance-repaired? false)
        present-false (medic/process allow base (req false-payload false-payload))]
    (is (= :missing-evidence (:reason missing)))
    (is (= :binding-mismatch (:reason present-false)))
    (is (= :refused (:status present-false)))))

(deftest duplicate-and-conflict-are-inert
  (let [once (:state (medic/process allow base (req hist hist)))
        duplicate (medic/process allow once (req hist hist))
        changed (assoc hist :patch-id "different" :evidence-pin "different-evidence")
        conflict (medic/process allow once (req changed changed))]
    (is (= :duplicate (:status duplicate)))
    (is (= :conflict (:status conflict)))
    (is (= once (:state duplicate) (:state conflict)))))

(deftest malformed-and-invalid-history-refuse-without-defaults
  (is (= :malformed-payload
         (:reason (medic/process allow base (req (dissoc hist :replay)
                                                (dissoc hist :replay))))))
  (is (= :duplicate-entry-id
         (:reason (medic/process allow {:history [e1 (assoc e2 :entry-id "e1")]
                                        :corrections []}
                                 (req hist hist)))))
  (is (= :invalid-predecessor
         (:reason (medic/process allow
                                 {:history [(assoc e1 :predecessor-entry-id "missing")]
                                  :corrections []}
                                 (req hist hist))))))

(deftest preloaded-correction-never-certifies-clear-all
  (let [forged {:history [e1] :corrections [hist]}
        ordinary (medic/effective-active forged)
        certified (medic/certified-sweep allow forged [])]
    (is (empty? ordinary) "ordinary projection computes but proves no provenance")
    (is (= :preloaded-corrections (:reason certified)))
    (is (false? (:certified? certified)))
    (is (false? (medic/certified-clear-all? certified)))
    (is (false? (medic/certified-clear-all? allow forged [])))))

(deftest public-clear-all-recomputes-and-rejects-forged-results
  (is (false? (medic/certified-clear-all? {:certified? true})))
  (is (false? (medic/certified-clear-all?
               {:certified? true :state {:history [] :corrections []}})))
  (is (true? (medic/certified-clear-all? allow
                                         {:history [] :corrections []} [])))
  (is (= :request-sequence-required
         (:reason (medic/certified-sweep allow
                                         {:history [] :corrections []} nil)))))

(deftest correction-and-predecessor-domain-is-explicit
  (is (= :malformed-correction
         (:reason (medic/validate-state {:history [] :corrections [nil]}))))
  (is (= :malformed-history-entry
         (:reason (medic/validate-state
                   {:history [(assoc e1 :predecessor-entry-id false)]
                    :corrections []}))))
  (is (= :invalid-predecessor
         (:reason (medic/validate-state
                   {:history [(assoc e1 :predecessor-entry-id "unknown")]
                    :corrections []})))))

(deftest rejected-step-withholds-sweep-certification
  (let [result (medic/certified-sweep allow base [(req hist hist) (req hist hist)])]
    (is (= [:appended :duplicate] (mapv :status (:results result))))
    (is (= :uncertified (:status result)))
    (is (false? (:certified? result)))))
