(ns futon2.aif.genuine-repair-authority-test
  "Synthetic authority grammar only.  The fields mirror the retained cfb/940
  acceptance evidence categories (cause, original contract, pre/post and a
  negative replay), but no live approval is represented here."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-medic-authority :as authority]))

(def evaluator {:seat "synthetic-evaluator" :session "eval-1"})
(def reviewer {:seat "synthetic-reviewer" :session "review-1"})
(def authorizer {:seat "synthetic-authorizer" :session "auth-1"})

(defn- pinned [path value]
  (let [text (str (pr-str value) "\n")]
    [{:path path :sha256 (load-identity/sha256 (.getBytes text "UTF-8"))} text]))

(defn- fixture []
  (let [[cause-pin cause-bytes] (pinned "cfb/cause.edn" {:schema :wm/run :failure :held})
        [contract-pin contract-bytes] (pinned "cfb/acceptance.edn" {:schema :wm/acceptance :id :cfb})
        opened (ledger/append-transition ledger/empty-ledger
                                        {:injury-id "injury/cfb-synthetic" :state :open :attempt 1
                                         :cause-id "cause/cfb" :cause-record-pin cause-pin
                                         :failure-stage :reviewer-wait
                                         :failure-kind :reviewer-falsifier-failed
                                         :detected-click "click/cfb"})
        history (:ledger opened)
        entry (:entry opened)
        replay (fn [phase verdict]
                 {:schema authority/genuine-replay-schema
                  :acceptance-contract-pin contract-pin :phase phase :verdict verdict})
        [pre-pin pre-bytes] (pinned "cfb/pre.edn" (replay :pre :original-contract-failed))
        [post-pin post-bytes] (pinned "cfb/post.edn" (replay :post :original-contract-repaired))
        [negative-pin negative-bytes]
        (pinned "cfb/negative.edn" (replay :negative :negative-control-refused))
        intent {:schema authority/genuine-intent-schema :scope :cfb-genuine-repair
                :disposition-kind :genuine-repair :injury-id "injury/cfb-synthetic"
                :ordinal (:ordinal entry) :ledger-entry-id (:entry-id entry)
                :predecessor-entry-id nil :state :open :attempt 1
                :ledger-root (:ledger-pin history) :cause-record-pin cause-pin
                :acceptance-contract-pin contract-pin
                :patch-revision "809a7f7ac74b5a4f951c2270cfa856ba06650f17"
                :source-revision "809a7f7ac74b5a4f951c2270cfa856ba06650f17"
                :pre-replay-pin pre-pin :post-replay-pin post-pin :negative-control-pin negative-pin
                :evaluator evaluator :reviewer reviewer}
        intent-id (authority/intent-digest intent)
        evaluation {:schema authority/genuine-evaluation-schema :intent-digest intent-id
                    :evaluator evaluator :acceptance-contract-pin contract-pin
                    :pre-replay-pin pre-pin :post-replay-pin post-pin
                    :negative-control-pin negative-pin
                    :verdict :original-acceptance-repaired}
        [evaluation-pin evaluation-bytes] (pinned "cfb/evaluation.edn" evaluation)
        review {:schema authority/genuine-review-schema :intent-digest intent-id
                :evaluation-pin evaluation-pin :reviewer reviewer
                :verdict :genuine-repair-qualified}
        [review-pin review-bytes] (pinned "cfb/review.edn" review)
        authorization {:schema authority/genuine-authorization-schema :intent-digest intent-id
                       :evaluation-pin evaluation-pin :review-pin review-pin
                       :scope :cfb-genuine-repair :authorizer authorizer :verdict :qualified}
        [authorization-pin authorization-bytes] (pinned "cfb/authorization.edn" authorization)
        candidate {:kind :genuine-repair :injury-id "injury/cfb-synthetic"
                   :entry-id (str (:ordinal entry)) :predecessor-entry-id nil
                   :patch-id (:patch-revision intent) :reviewer (:seat reviewer)
                   :evaluator (:seat evaluator) :evidence-pin (:sha256 evaluation-pin)
                   :cause-record-id (:path cause-pin) :cause-pin (:sha256 cause-pin)
                   :acceptance-contract (:sha256 contract-pin)
                   :pre-replay-receipt (:sha256 pre-pin)
                   :post-replay-receipt (:sha256 post-pin)
                   :negative-control-receipt (:sha256 negative-pin)
                   :source-revision (:source-revision intent)
                   :original-acceptance-repaired? true}
        request {:candidate candidate :evidence candidate}
        operation {:operation-id (authority/final-operation-id intent-id request evaluation-pin review-pin authorization-pin)
                   :expected-ledger-pin (:ledger-pin history) :request request
                   :authority-receipt-pin authorization-pin}
        artifacts {(:path cause-pin) cause-bytes (:path contract-pin) contract-bytes
                   (:path pre-pin) pre-bytes (:path post-pin) post-bytes
                   (:path negative-pin) negative-bytes (:path evaluation-pin) evaluation-bytes
                   (:path review-pin) review-bytes (:path authorization-pin) authorization-bytes}
        trusted #{{:kind :evaluation :identity evaluator :intent-digest intent-id :receipt-pin evaluation-pin}
                  {:kind :review :identity reviewer :intent-digest intent-id :receipt-pin review-pin}
                  {:kind :authorization :identity authorizer :intent-digest intent-id :receipt-pin authorization-pin}}]
    {:intent intent :ledger history :ledger-entry entry
     :current {:injury-id "injury/cfb-synthetic" :entry-id (str (:ordinal entry))
               :predecessor-entry-id nil :cause-record-id (:path cause-pin)
               :cause-pin (:sha256 cause-pin) :active? true}
     :operation operation :artifacts artifacts :evaluation-pin evaluation-pin
     :review-pin review-pin :authorization-pin authorization-pin
     :trust #(contains? trusted %)}))

(deftest genuine-repair-authority-binds-every-replay-and-admits-one-occurrence
  (let [f (fixture)
        verifier (authority/authority-verifier f)
        appended (ledger/append-medic-repaired (:ledger f)
                                               {:path "cfb/medic.edn" :sha256 (apply str (repeat 64 "f"))}
                                               (:operation f) verifier)]
    (is (= :verified (:status (authority/verify f))))
    (is (= :appended (:status appended)) (pr-str appended))
    (is (empty? (:injuries (ledger/effective-active-injuries (:ledger appended) verifier))))))

(deftest genuine-repair-drift-and-cross-scope-are-refused
  (let [f (fixture)
        pin (get-in f [:intent :post-replay-pin])
        wrong-payload (assoc-in f [:operation :request :evidence :patch-id] "other")
        wrong-contract (assoc-in f [:intent :acceptance-contract-pin :sha256] (apply str (repeat 64 "0")))
        self-review (assoc-in f [:intent :reviewer] evaluator)
        wrong-root (assoc-in f [:intent :ledger-root] "stale")
        wrong-predecessor (assoc-in f [:intent :predecessor-entry-id] "other")
        false-verdict (assoc-in f [:operation :request :candidate :original-acceptance-repaired?] false)
        replay-drift (assoc-in f [:artifacts (:path pin)] "{:wrong true}\n")]
    (is (= :payload/swap (:reason (authority/verify wrong-payload))))
    (is (= :intent/payload-mismatch (:reason (authority/verify wrong-contract))))
    (is (= :roles/not-independent (:reason (authority/verify self-review))))
    (is (= :intent/root-mismatch (:reason (authority/verify wrong-root))))
    (is (= :intent/payload-mismatch (:reason (authority/verify wrong-predecessor))))
    (is (= :payload/swap (:reason (authority/verify false-verdict))))
    (is (= :artifact/hash-drift (:reason (authority/verify replay-drift))))
    (let [bundle (select-keys f [:intent :artifacts :evaluation-pin :review-pin :authorization-pin :trust])
          context (authority/resolve-context {:bundles [bundle bundle]})]
      (is (= :meta-self-heal/medic-authority-ambiguous (:reason context))))))
