(ns futon2.aif.meta-medic-authority-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-medic-authority :as authority]))

(def evaluator {:seat "evaluator-seat" :session "eval-session"})
(def reviewer {:seat "reviewer-seat" :session "review-session"})
(def authorizer {:seat "operator-seat" :session "operator-session"})

(defn pinned [path value]
  (let [text (str (pr-str value) "\n")]
    [{:path path :sha256 (load-identity/sha256 (.getBytes text "UTF-8"))} text]))

(defn fixture []
  (let [original {:failure {:kind :already-satisfied :stage :author-wait}
                  :run-output {:outcome :already-satisfied}}
        [original-pin original-bytes] (pinned "artifacts/original.edn" original)
        contract-path "holes/labs/wm-contract/already-satisfied-terminal-classification-invariant.md"
        contract-bytes (slurp contract-path)
        contract-pin {:path contract-path
                      :sha256 (load-identity/sha256
                               (.getBytes contract-bytes "UTF-8"))}
        opened (ledger/append-transition
                ledger/empty-ledger
                {:injury-id "injury/a9" :state :open :attempt 1
                 :cause-id "cause/a9" :cause-record-pin original-pin
                 :failure-stage :author-wait :failure-kind :already-satisfied
                 :detected-click "click/a9"})
        history (:ledger opened)
        open-entry (:entry opened)
        intent {:schema authority/intent-schema
                :scope :a9-historical-misclassification
                :disposition-kind :historical-misclassification
                :injury-id "injury/a9"
                :ordinal (:ordinal open-entry)
                :ledger-entry-id (:entry-id open-entry)
                :predecessor-entry-id nil
                :state :open :attempt 1
                :ledger-root (:ledger-pin history)
                :original-record-pin original-pin
                :acceptance-contract-pin contract-pin
                :patch-revision "36afa9142331fbe99eb99b67ae510322ca543f0c"
                :classifier-revision "c8e65d96213c884d08b1d3a07f51c76fece0c5bb"
                :replay :producer-qualified-non-injury
                :evaluator evaluator :reviewer reviewer}
        intent-id (authority/intent-digest intent)
        evaluation {:schema authority/evaluation-schema :intent-digest intent-id
                    :evaluator evaluator :findings authority/required-findings}
        [evaluation-pin evaluation-bytes] (pinned "receipts/evaluation.edn" evaluation)
        review {:schema authority/review-schema :intent-digest intent-id
                :evaluation-pin evaluation-pin :reviewer reviewer
                :verdict :historical-misclassification-qualified}
        [review-pin review-bytes] (pinned "receipts/review.edn" review)
        authorization {:schema authority/authorization-schema
                       :intent-digest intent-id :evaluation-pin evaluation-pin
                       :review-pin review-pin :scope :a9-historical-misclassification
                       :authorizer authorizer :verdict :qualified}
        [authorization-pin authorization-bytes]
        (pinned "receipts/authorization.edn" authorization)
        candidate {:kind :historical-misclassification
                   :injury-id "injury/a9" :entry-id (str (:ordinal open-entry))
                   :predecessor-entry-id nil :patch-id (:patch-revision intent)
                   :reviewer (:seat reviewer) :evaluator (:seat evaluator)
                   :evidence-pin (:sha256 evaluation-pin)
                   :original-record-id (:path original-pin)
                   :original-pin (:sha256 original-pin)
                   :classifier-revision (:classifier-revision intent)
                   :replay :producer-qualified-non-injury}
        request {:candidate candidate :evidence candidate}
        operation-id (authority/final-operation-id
                      intent-id request evaluation-pin review-pin authorization-pin)
        operation {:operation-id operation-id
                   :expected-ledger-pin (:ledger-root intent)
                   :request request :authority-receipt-pin authorization-pin}
        current {:injury-id "injury/a9" :entry-id (str (:ordinal open-entry))
                 :predecessor-entry-id nil
                 :cause-record-id (:path original-pin)
                 :cause-pin (:sha256 original-pin) :active? true}
        ledger-entry open-entry
        artifacts {(:path original-pin) original-bytes
                   (:path contract-pin) contract-bytes
                   (:path evaluation-pin) evaluation-bytes
                   (:path review-pin) review-bytes
                   (:path authorization-pin) authorization-bytes}
        trusted #{{:kind :evaluation :identity evaluator
                   :intent-digest intent-id :receipt-pin evaluation-pin}
                  {:kind :review :identity reviewer
                   :intent-digest intent-id :receipt-pin review-pin}
                  {:kind :authorization :identity authorizer
                   :intent-digest intent-id :receipt-pin authorization-pin}}]
    {:operation operation :current current :ledger-entry ledger-entry :ledger history
     :intent intent :artifacts artifacts
     :evaluation-pin evaluation-pin :review-pin review-pin
     :authorization-pin authorization-pin
     :trust (fn [attestation] (contains? trusted attestation))}))

(deftest acyclic-trusted-bundle-verifies
  (let [f (fixture) result (authority/verify f)]
    (is (= :verified (:status result)) (pr-str result))
    (is (= (get-in f [:operation :operation-id]) (:operation-id result)))
    (is (not= (:intent-digest result) (:operation-id result)))))

(deftest artifact-presence-and-integrity-fail-closed
  (let [f (fixture) path (get-in f [:evaluation-pin :path])
        contract-path (get-in f [:intent :acceptance-contract-pin :path])
        two-forms "{:one 1} {:two 2}"
        two-pin {:path "artifacts/two.edn"
                 :sha256 (load-identity/sha256 (.getBytes two-forms "UTF-8"))}]
    (is (= :artifact/missing
           (:reason (authority/verify (update f :artifacts dissoc path)))))
    (is (= :artifact/hash-drift
           (:reason (authority/verify
                     (assoc-in f [:artifacts path] "{:tampered true}\n")))))
    (is (= :artifact/missing
           (:reason (authority/verify (update f :artifacts dissoc contract-path)))))
    (is (= :artifact/hash-drift
           (:reason (authority/verify
                     (assoc-in f [:artifacts contract-path]
                               (str (get-in f [:artifacts contract-path]) "tamper"))))))
    (is (= :artifact/multiple-forms
           (:reason (authority/resolve-artifact {"artifacts/two.edn" two-forms}
                                                two-pin))))))

(deftest pinned-markdown-contract-is-opaque-and-hash-verified
  (let [f (fixture)
        pin (get-in f [:intent :acceptance-contract-pin])]
    (is (= "7b8788e96ff60cfd1138a5c641ec148d0defd17cf1f3b9a96e3c021552f7b63d"
           (:sha256 pin)))
    (is (= :resolved (:status (authority/resolve-bytes (:artifacts f) pin))))
    (is (= :verified (:status (authority/verify f))))))

(deftest intent-and-final-envelope-bindings-refuse-drift
  (let [f (fixture)]
    (doseq [[label altered expected]
            [[:root (assoc-in f [:intent :ledger-root :sha256]
                              (apply str (repeat 64 "9"))) :intent/root-mismatch]
             [:occurrence (assoc-in f [:intent :ordinal] 35) :intent/occurrence-mismatch]
             [:patch (assoc-in f [:intent :patch-revision]
                               "46afa9142331fbe99eb99b67ae510322ca543f0c")
              :intent/patch-mismatch]
             [:contract (assoc-in f [:intent :acceptance-contract-pin :sha256] "bad")
              :intent/contract-mismatch]
             [:operation-id (assoc-in f [:operation :operation-id] "forged")
              :operation/final-binding-mismatch]
             [:payload-swap (assoc-in f [:operation :request :evidence :patch-id]
                                      "46afa9142331fbe99eb99b67ae510322ca543f0c")
              :payload/swap]]]
      (testing (name label)
        (is (= expected (:reason (authority/verify altered))))))))

(deftest roles-review-scope-and-trust-are-not-self-attested
  (let [f (fixture)
        review-path (get-in f [:review-pin :path])
        old-review {:schema :wm/source-review-v1 :status :approved}
        old-review-bytes (str (pr-str old-review) "\n")
        forged-auth (assoc (get-in (authority/resolve-artifact
                                    (:artifacts f) (:authorization-pin f)) [:value])
                           :approved true)
        [forged-pin forged-bytes] (pinned "receipts/forged.edn" forged-auth)]
    (is (= :roles/not-independent
           (:reason (authority/verify
                     (assoc-in f [:intent :reviewer] evaluator)))))
    (is (= :roles/not-independent
           (:reason (authority/verify
                     (assoc-in f [:intent :reviewer]
                               {:seat (:seat evaluator) :session "other-session"})))))
    (is (= :artifact/hash-drift
           (:reason (authority/verify
                     (assoc-in f [:artifacts review-path] old-review-bytes)))))
    (is (= :evaluation/untrusted
           (:reason (authority/verify (assoc f :trust (constantly false))))))
    (let [untrusted (-> f
                        (assoc :authorization-pin forged-pin)
                        (assoc-in [:operation :authority-receipt-pin] forged-pin)
                        (assoc-in [:artifacts (:path forged-pin)] forged-bytes))]
      (is (= :authorization/untrusted
             (:reason (authority/verify untrusted)))))))

(deftest recomputed-envelope-cannot-authorize-substituted-payload
  (let [f (fixture)
        candidate (-> (get-in f [:operation :request :candidate])
                      (assoc :evaluator "unapproved-evaluator"
                             :reviewer "unapproved-reviewer"
                             :evidence-pin (apply str (repeat 64 "9"))))
        request {:candidate candidate :evidence candidate}
        recomputed (authority/final-operation-id
                    (authority/intent-digest (:intent f)) request
                    (:evaluation-pin f) (:review-pin f) (:authorization-pin f))
        altered (-> f
                    (assoc-in [:operation :request] request)
                    (assoc-in [:operation :operation-id] recomputed))]
    (is (= :intent/payload-mismatch
           (:reason (authority/verify altered))))))

(deftest adapter-drives-actual-medic-admission
  (let [f (fixture)
        record-pin {:path "receipts/final-medic.edn"
                    :sha256 (apply str (repeat 64 "8"))}
        result (ledger/append-medic-repaired
                (:ledger f) record-pin (:operation f)
                (authority/authority-verifier f))]
    (is (= :appended (:status result)) (pr-str result))
    (is (= :valid (:status (ledger/validate-ledger
                            (:ledger result)
                            (authority/authority-verifier f)))))))

(deftest source-review-cannot-substitute-for-qualification-review
  (let [f (fixture)
        intent-id (authority/intent-digest (:intent f))
        source-review {:schema :wm/source-review-v1 :intent-digest intent-id
                       :evaluation-pin (:evaluation-pin f)
                       :reviewer reviewer :status :approved}
        [review-pin review-bytes] (pinned "receipts/source-review.edn" source-review)
        authorization {:schema authority/authorization-schema
                       :intent-digest intent-id
                       :evaluation-pin (:evaluation-pin f)
                       :review-pin review-pin :scope :a9-historical-misclassification
                       :authorizer authorizer :verdict :qualified}
        [authorization-pin authorization-bytes]
        (pinned "receipts/source-review-authorization.edn" authorization)
        request (get-in f [:operation :request])
        operation-id (authority/final-operation-id
                      intent-id request (:evaluation-pin f) review-pin authorization-pin)
        altered (-> f
                    (assoc :review-pin review-pin
                           :authorization-pin authorization-pin)
                    (assoc-in [:operation :operation-id] operation-id)
                    (assoc-in [:operation :authority-receipt-pin] authorization-pin)
                    (assoc-in [:artifacts (:path review-pin)] review-bytes)
                    (assoc-in [:artifacts (:path authorization-pin)] authorization-bytes))
        trusted (fn [{:keys [kind]}] (contains? #{:evaluation :review :authorization} kind))]
    (is (= :review/not-qualified
           (:reason (authority/verify (assoc altered :trust trusted)))))))

(deftest candidate-manifest-remains-explicitly-unapproved
  (let [manifest (authority/a9-unapproved-candidate)]
    (is (= :unapproved (:status manifest)))
    (is (some #{:complete-original-record-bytes} (:missing manifest)))
    (is (some #{:trusted-authorizer-attestation} (:missing manifest)))
    (is (nil? (:operation manifest)))))
