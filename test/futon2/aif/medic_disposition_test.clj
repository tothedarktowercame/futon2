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

(deftest both-evidence-kinds-and-certified-clear-all
  (let [partial (medic/certified-sweep allow base [(req hist hist)])
        all (medic/certified-sweep allow base [(req hist hist) (req genuine genuine)])]
    (is (:certified? partial))
    (is (= [["41" "e2"]] (medic/effective-active (:state partial))))
    (is (false? (medic/certified-clear-all? partial)))
    (is (:certified? all))
    (is (empty? (medic/effective-active (:state all))))
    (is (medic/certified-clear-all? all))
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
    (is (false? (medic/certified-clear-all? certified)))))

(deftest rejected-step-withholds-sweep-certification
  (let [result (medic/certified-sweep allow base [(req hist hist) (req hist hist)])]
    (is (= [:appended :duplicate] (mapv :status (:results result))))
    (is (= :uncertified (:status result)))
    (is (false? (:certified? result)))))
