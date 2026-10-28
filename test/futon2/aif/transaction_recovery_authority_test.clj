(ns futon2.aif.transaction-recovery-authority-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-medic-authority :as authority]
            [futon2.aif.meta-self-heal-production :as production]
            [futon2.aif.unadmitted-transaction-recovery-test :as recovery-fixture]))

(def author {:seat "recovery-author" :session "author-1"})
(def evaluator {:seat "recovery-evaluator" :session "evaluator-1"})
(def reviewer {:seat "recovery-reviewer" :session "reviewer-1"})
(def authorizer {:seat "recovery-authorizer" :session "authorizer-1"})

(defn- pinned [path value]
  (let [text (str (pr-str value) "\n")]
    [{:path path :sha256 (load-identity/sha256 (.getBytes text "UTF-8"))} text]))

(defn- file-sha [path]
  (load-identity/sha256
   (java.nio.file.Files/readAllBytes (.toPath (io/file path)))))

(defn- case-fixture [label]
  (#'recovery-fixture/temp-case
   label (#'recovery-fixture/fixture "transaction")))

(defn- authority-bundle
  ([case] (authority-bundle case reviewer identity))
  ([case review-identity alter-intent]
   (let [history (edn/read-string (slurp (:ledger-path case)))
         latest (last (filter #(and (= :wm/meta-injury-transition-v1 (:schema %))
                                    (= (:injury-id case) (:injury-id %)))
                              (:entries history)))
         ledger-author (last (filter #(and (= :authored (:state %))
                                           (= (:injury-id case) (:injury-id %))
                                           (= (:repair-id case) (:repair-id %))
                                           (= (:attempt case) (:attempt %)))
                                     (:entries history)))
         intent (alter-intent
                 {:schema authority/recovery-intent-schema
                  :scope :transaction-recovery
                  :injury-id (:injury-id case)
                  :repair-id (:repair-id case)
                  :attempt (:attempt case)
                  :ledger-entry-id (:entry-id latest)
                  :ledger-entry-pin (:entry-pin latest)
                  :canonical-transaction-path (:transaction-path case)
                  :canonical-transaction-sha256 (file-sha (:transaction-path case))
                  :deferred-transaction-path (:deferred-path case)
                  :deferred-transaction-sha256 (file-sha (:deferred-path case))
                  :ledger-author-receipt-pin (:port-receipt-pin ledger-author)
                  :ledger-author-patch (:patch-sha ledger-author)
                  :author author :evaluator evaluator :reviewer review-identity})
         intent-id (authority/intent-digest intent)
         evaluation {:schema authority/recovery-evaluation-schema
                     :intent-digest intent-id :evaluator evaluator
                     :verdict :transaction-recovery-qualified}
         [evaluation-pin evaluation-bytes] (pinned "recovery/evaluation.edn" evaluation)
         review {:schema authority/recovery-review-schema
                 :intent-digest intent-id :evaluation-pin evaluation-pin
                 :reviewer review-identity :verdict :transaction-recovery-qualified}
         [review-pin review-bytes] (pinned "recovery/review.edn" review)
         authorization {:schema authority/recovery-authorization-schema
                        :intent-digest intent-id :evaluation-pin evaluation-pin
                        :review-pin review-pin :scope :transaction-recovery
                        :authorizer authorizer :verdict :qualified}
         [authorization-pin authorization-bytes]
         (pinned "recovery/authorization.edn" authorization)
         candidate (select-keys intent
                                [:injury-id :repair-id :attempt :ledger-entry-id
                                 :ledger-entry-pin :canonical-transaction-path
                                 :canonical-transaction-sha256
                                 :deferred-transaction-path
                                 :deferred-transaction-sha256
                                 :ledger-author-receipt-pin :ledger-author-patch])
         request {:candidate candidate :evidence candidate}
         operation {:operation-id
                    (authority/final-operation-id intent-id request evaluation-pin
                                                  review-pin authorization-pin)
                    :authority-receipt-pin authorization-pin}
         trusted #{{:kind :evaluation :identity evaluator :intent-digest intent-id
                    :receipt-pin evaluation-pin}
                   {:kind :review :identity review-identity :intent-digest intent-id
                    :receipt-pin review-pin}
                   {:kind :authorization :identity authorizer :intent-digest intent-id
                    :receipt-pin authorization-pin}}]
     {:bundle {:intent intent
               :artifacts {(:path evaluation-pin) evaluation-bytes
                           (:path review-pin) review-bytes
                           (:path authorization-pin) authorization-bytes}
               :evaluation-pin evaluation-pin :review-pin review-pin
               :authorization-pin authorization-pin
               :trust #(contains? trusted %)}
      :operation operation})))

(defn- recover [case authority-fixture]
  (let [resolved (authority/resolve-context (:bundle authority-fixture))]
    (production/recover-unadmitted-transaction!
     (merge (select-keys case [:ledger-path :transaction-path
                               :injury-id :repair-id :attempt])
            {:medic-authority (:operation authority-fixture)
             :authority-verifier
             (authority/resolution-verifier (:resolution resolved))}))))

(deftest installed-provider-authorizes-exact-transaction-recovery
  (let [case (case-fixture "provider-recovery")
        ledger-before (slurp (:ledger-path case))
        result (recover case (authority-bundle case))]
    (is (= :archived (:status result)) (pr-str result))
    (is (= ledger-before (slurp (:ledger-path case))))
    (is (not (.exists (io/file (:transaction-path case)))))
    (is (.isFile (io/file (get-in result [:archive-pin :path]))))))

(deftest installed-provider-refuses-recovery-binding-drift
  (doseq [[label alter]
          [[:canonical-sha #(assoc % :canonical-transaction-sha256
                                  (apply str (repeat 64 "0")))]
           [:deferred-sha #(assoc % :deferred-transaction-sha256
                                 (apply str (repeat 64 "0")))]
           [:attempt #(update % :attempt inc)]
           [:author-pin #(assoc-in % [:ledger-author-receipt-pin :sha256]
                                   (apply str (repeat 64 "0")))]]]
    (testing (name label)
      (let [case (case-fixture (str "provider-drift-" (name label)))
            canonical-before (slurp (:transaction-path case))
            deferred-before (slurp (:deferred-path case))
            result (recover case (authority-bundle case reviewer alter))]
        (is (= :meta-self-heal/recovery-medic-authority-refused (:reason result)))
        (is (= canonical-before (slurp (:transaction-path case))))
        (is (= deferred-before (slurp (:deferred-path case))))))))

(deftest installed-provider-requires-independent-reviewer
  (let [case (case-fixture "provider-self-review")
        canonical-before (slurp (:transaction-path case))
        result (recover case (authority-bundle case author identity))]
    (is (= :meta-self-heal/recovery-medic-authority-refused (:reason result)))
    (is (= canonical-before (slurp (:transaction-path case))))))
