(ns futon2.aif.cause-reattribution-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cause-reattribution :as reattribution]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-medic-authority :as authority]
            [futon2.aif.meta-terminal-injury :as terminal])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def fixture-root "test/resources/wm/misattribution-117")
(def old-injury "injury/41a00c626ed7f9f9d5454e148745c951ec3db80e")
(def replacement-injury "injury/a46779bc8935e575ff19ea6719aa1be7122f8c40")

(defn- temp-root []
  (.toFile (Files/createTempDirectory "cause-reattribution-"
                                      (make-array FileAttribute 0))))
(defn- copy! [from to]
  (io/make-parents to)
  (io/copy (io/file from) (io/file to))
  to)
(defn- fixture-case []
  (let [root (temp-root)
        ledger-path (copy! (str fixture-root "/ledger.edn")
                           (str root "/ledger.edn"))
        run-path (copy! (str fixture-root "/run.edn") (str root "/run.edn"))
        receipt0 (edn/read-string (slurp (str fixture-root "/reattribution-receipt.edn")))
        receipt receipt0
        receipt-path (str root "/receipt.edn")
        _ (spit receipt-path (str (pr-str receipt) "\n"))
        pin {:path receipt-path
             :sha256 (load-identity/sha256
                      (Files/readAllBytes (.toPath (io/file receipt-path))))}
        history (:value (store/read-one-form (slurp ledger-path)))
        operation (reattribution/checked-operation (:ledger-pin history) pin receipt)
        normal (authority/with-retained-authorities
                (fn [{:keys [candidate]}]
                  (not= :cause-reattribution (:kind candidate))))]
    {:root root :ledger-path ledger-path :run-path run-path :receipt receipt
     :pin pin :history history :operation operation :authority normal}))

(deftest retained-ordinal-117-reattribution-is-atomic-and-reloadable
  (let [{:keys [ledger-path run-path receipt operation authority]} (fixture-case)
        record-pin {:path (get-in receipt [:candidate :original-record-id])
                    :sha256 (get-in receipt [:candidate :original-pin])}
        replacement-classification
        (terminal/classify
         (assoc (edn/read-string (slurp run-path))
                :failure {:kind :capability-route-missing :stage :cascade-resume})
         record-pin)
        result (store/transact-cause-reattribution!
                ledger-path record-pin operation authority)
        final (:ledger result)
        active (:injuries (ledger/effective-active-injuries final authority))]
    (is (= replacement-injury (:injury-id replacement-classification)))
    (is (= :appended (:status result)) (pr-str result))
    (is (= 119 (count (:entries final))))
    (is (nil? (get active old-injury)))
    (is (= :open (get-in active [replacement-injury :state])))
    (is (= :capability-route-missing
           (get-in active [replacement-injury :failure-kind])))
    (is (= :cascade-resume (get-in active [replacement-injury :failure-stage])))
    (is (= "ae9e715844c49be7ed2110fbdc97b482a2f5a9a7"
           (->> (:entries final)
                (filter #(= :medic-repaired (:observation-kind %)))
                (map #(get-in % [:medic-operation :request :candidate]))
                (filter #(= old-injury (:injury-id %))) first :patch-id)))
    (testing "both production disk loaders resolve the retained authority"
      (is (= :loaded (:status (store/load-ledger ledger-path authority))))
      (is (= :loaded (:status (store/load-durable-ledger ledger-path authority)))))))

(deftest malformed-reattributions-are-inert
  (doseq [[label mutate]
          [[:same-cause #(assoc-in % [:candidate :replacement-cause-id]
                                  (get-in % [:candidate :rejected-cause-id]))]
           [:mismatched-pin #(assoc-in % [:candidate :original-pin]
                                      (apply str (repeat 64 "0")))]
           [:missing-evidence #(dissoc % :candidate)]]]
    (let [{:keys [history receipt pin authority]} (fixture-case)
          changed (mutate receipt)
          operation (reattribution/checked-operation (:ledger-pin history) pin changed)
          record-pin {:path (get-in receipt [:candidate :original-record-id])
                      :sha256 (get-in receipt [:candidate :original-pin])}
          result (ledger/append-cause-reattribution history record-pin operation authority)]
      (is (not= :appended (:status result)) (name label))
      (is (nil? (:ledger result)) (name label)))))

(deftest protocol-wrapper-recurrence-requires-matching-signature
  (let [{:keys [history receipt operation authority]} (fixture-case)
        record-pin {:path (get-in receipt [:candidate :original-record-id])
                    :sha256 (get-in receipt [:candidate :original-pin])}
        corrected (:ledger (ledger/append-cause-reattribution
                            history record-pin operation authority))
        record {:run/id "different-protocol-cause" :click/id "different-click"
                :failure {:kind :resume-protocol-violation :stage :cascade-resume}}
        classification (terminal/classify
                        record {:path "fixture/different-protocol-cause.edn"
                                :sha256 (apply str (repeat 64 "d"))})
        mismatch (terminal/integrate corrected classification authority
                                     (constantly :mismatch))
        missing (terminal/integrate corrected classification authority
                                    (constantly :missing))
        matched (terminal/integrate corrected classification authority
                                    (constantly :match))]
    (is (= old-injury (:injury-id classification)))
    (is (= :medic-recurrence/cause-mismatch (:reason mismatch)))
    (is (= :medic-recurrence/signature-required (:reason missing)))
    (is (= :reopened (:transition matched)))
    (is (nil? (:ledger mismatch)))
    (is (nil? (:ledger missing)))))
