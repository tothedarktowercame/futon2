(ns futon2.aif.unadmitted-transaction-recovery-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-repair-coordinator :as coordinator]
            [futon2.aif.meta-self-heal-production :as production]))

(def fixture-root "test/futon2/aif/fixtures/m2d/")
(def authority-pin {:path "fixture/medic-authority.edn"
                    :sha256 (apply str (repeat 64 "4"))})
(def unadmitted-pin {:path "receipts/unadmitted-author.edn"
                     :sha256 (apply str (repeat 64 "8"))})
(def mismatched-pin {:path "receipts/mismatched-deferred-author.edn"
                     :sha256 (apply str (repeat 64 "9"))})
(def unadmitted-patch (apply str (repeat 40 "b")))

(defn- fixture [name]
  (edn/read-string (slurp (str fixture-root name ".edn"))))

(defn- transaction-with-author [transaction receipt-pin patch-sha]
  (let [author (some #(when (= :author (:phase %)) %) (:receipts transaction))]
    (#'coordinator/seal-transaction
     (assoc transaction :receipts [(assoc author
                                          :receipt-pin receipt-pin
                                          :payload (assoc (:payload author)
                                                          :patch-sha patch-sha))]))))

(defn- temp-case [label deferred-transaction]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       (str label "-")
                       (make-array java.nio.file.attribute.FileAttribute 0)))
        ledger-path (.getPath (io/file root "ledger.edn"))
        transaction-path (.getPath (io/file root "transaction.edn"))
        deferred-path (str transaction-path
                           ".deferred-injury_04e14314a0eec3aab88d8e7044be26f3ae5dfe6d.edn")
        history (fixture "ledger")
        admitted (fixture "transaction")
        order (:work-order admitted)
        unadmitted (transaction-with-author admitted unadmitted-pin unadmitted-patch)]
    (spit ledger-path (str (pr-str history) "\n"))
    (spit transaction-path (str (pr-str unadmitted) "\n"))
    (when deferred-transaction
      (spit deferred-path (str (pr-str deferred-transaction) "\n")))
    {:ledger-path ledger-path
     :transaction-path transaction-path
     :deferred-path deferred-path
     :history history
     :injury-id (:injury-id order)
     :repair-id (:repair-id order)
     :attempt (:attempt order)}))

(defn- authority [expected-pin]
  (fn [{:keys [operation candidate evidence]}]
    (and (= expected-pin (:authority-receipt-pin operation))
         (= candidate evidence))))

(defn- recovery-opts [case]
  (merge (select-keys case [:ledger-path :transaction-path
                            :injury-id :repair-id :attempt])
         {:medic-authority {:authority-receipt-pin authority-pin}
          :authority-verifier (authority authority-pin)}))

(deftest unadmitted-canonical-is-archived-for-existing-restore
  (let [case (temp-case "unadmitted-recovery" (fixture "transaction"))
        ledger-bytes (slurp (:ledger-path case))
        canonical-bytes (slurp (:transaction-path case))
        result (production/recover-unadmitted-transaction! (recovery-opts case))]
    (is (= :archived (:status result)) (pr-str result))
    (is (= (:deferred-path case) (:deferred-path result)))
    (is (= ledger-bytes (slurp (:ledger-path case))))
    (is (not (.exists (io/file (:transaction-path case)))))
    (is (= canonical-bytes (slurp (get-in result [:archive-pin :path]))))
    (is (= (load-identity/sha256 (.getBytes canonical-bytes "UTF-8"))
           (get-in result [:archive-pin :sha256])))
    (let [selection (production/injury-selection
                     (:transaction-path case)
                     {:ledger (:history case)}
                     {:injuries {(:injury-id case) {:state :enacted}}})]
      (is (= :restore (:mode selection)) (pr-str selection))
      (is (= (:deferred-path case) (:transaction-path selection)))
      (is (= (:transaction-path case) (:restore-to selection))))))

(deftest recovery-requires-ledger-admitted-deferred-transaction
  (testing "no deferred transaction"
    (let [case (temp-case "no-deferred" nil)
          ledger-before (slurp (:ledger-path case))
          transaction-before (slurp (:transaction-path case))
          result (production/recover-unadmitted-transaction! (recovery-opts case))]
      (is (= :meta-self-heal/recovery-no-admitted-transaction (:reason result)))
      (is (= ledger-before (slurp (:ledger-path case))))
      (is (= transaction-before (slurp (:transaction-path case))))))
  (testing "deferred author pin differs from the ledger"
    (let [admitted (fixture "transaction")
          patch (get-in (first (:receipts admitted)) [:payload :patch-sha])
          mismatched (transaction-with-author admitted mismatched-pin patch)
          case (temp-case "mismatched-deferred" mismatched)
          ledger-before (slurp (:ledger-path case))
          transaction-before (slurp (:transaction-path case))
          deferred-before (slurp (:deferred-path case))
          result (production/recover-unadmitted-transaction! (recovery-opts case))]
      (is (= :meta-self-heal/recovery-no-admitted-transaction (:reason result)))
      (is (= ledger-before (slurp (:ledger-path case))))
      (is (= transaction-before (slurp (:transaction-path case))))
      (is (= deferred-before (slurp (:deferred-path case)))))))

(deftest recovery-requires-medic-authority
  (let [case (temp-case "missing-authority" (fixture "transaction"))
        ledger-before (slurp (:ledger-path case))
        transaction-before (slurp (:transaction-path case))
        result (production/recover-unadmitted-transaction!
                (dissoc (recovery-opts case) :medic-authority))]
    (is (= :meta-self-heal/recovery-medic-authority-required (:reason result)))
    (is (= ledger-before (slurp (:ledger-path case))))
    (is (= transaction-before (slurp (:transaction-path case))))))
