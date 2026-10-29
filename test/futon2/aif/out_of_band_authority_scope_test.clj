(ns futon2.aif.out-of-band-authority-scope-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.out-of-band-fix :as out-of-band]))

(def fixture-root "test/resources/wm/open-medic-authority-scope")
(def ledger-fixture (str fixture-root "/ledger-70e77fe7.edn"))
(def receipt-fixture (str fixture-root "/receipt-c8c6c14e.edn"))
(def injury-id "injury/1e3f6a623656df0e1a0291a61743e274d3d4742f")

(defn- sha-file [path]
  (identity/sha256
   (java.nio.file.Files/readAllBytes (.toPath (io/file path)))))

(defn- temp-root []
  (.toFile (java.nio.file.Files/createTempDirectory
            "open-medic-authority-scope-"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- retained-normal-authority
  "Fixture authority for the exact medic operations already sealed into the
  retained ledger. It checks the complete operation and its replay payload."
  [history calls]
  (let [operations
        (into {}
              (keep (fn [entry]
                      (when (= :medic-repaired (:observation-kind entry))
                        (let [operation (:medic-operation entry)]
                          [(:operation-id operation) operation]))))
              (:entries history))]
    (fn [{:keys [operation current candidate evidence]}]
      (swap! calls conj (:operation-id operation))
      (and (= operation (get operations (:operation-id operation)))
           (= candidate evidence (get-in operation [:request :candidate]))
           (= [(:injury-id candidate) (:entry-id candidate)]
              [(:injury-id current) (:entry-id current)])))))

(deftest retained-ledger-keeps-normal-authority-while-new-operation-is-scoped
  (let [ledger-bytes (java.nio.file.Files/readAllBytes (.toPath (io/file ledger-fixture)))
        history (edn/read-string (String. ledger-bytes "UTF-8"))
        root (temp-root)
        ledger-path (str (io/file root "ledger.edn"))
        receipt-path (str (io/file root "receipt-c8c6c14e.edn"))
        _ (spit ledger-path (String. ledger-bytes "UTF-8"))
        _ (spit receipt-path (slurp receipt-fixture))
        receipt-pin {:path receipt-path :sha256 (sha-file receipt-path)}
        receipt (edn/read-string (slurp receipt-path))
        normal-calls (atom [])
        normal (retained-normal-authority history normal-calls)
        active (ledger/effective-active-injuries history normal)
        open-entry (assoc (get-in active [:injuries injury-id]) :injury-id injury-id)
        {:keys [operation attribution]}
        (out-of-band/checked-medic-operation
         history open-entry (:artifact-pin receipt) receipt-pin receipt)
        specific (out-of-band/checked-medic-authority operation receipt-pin receipt)
        scoped (out-of-band/scoped-medic-authority normal operation specific)
        before-entries (:entries history)
        result (store/transact-medic-disposition!
                ledger-path receipt-pin operation attribution scoped)
        persisted (store/load-durable-ledger ledger-path scoped)]
    (testing "the retained bytes are the live ledger that exposed the defect"
      (is (= "70e77fe7357a3f020e4eb8b5d7cba63b0844118832085ada6cbd051ebeaa3f78"
             (identity/sha256 ledger-bytes))))
    (testing "the existing ledger validates only through its normal authority"
      (is (= :valid (:status (ledger/validate-ledger history normal))))
      (is (seq @normal-calls)))
    (testing "the checked receipt appends one medic-stamped discharge"
      (is (= :appended (:status result)) (pr-str result))
      (is (= :loaded (:status persisted)) (pr-str persisted))
      (is (= :valid (:status (ledger/validate-ledger (:ledger persisted) scoped))))
      (is (= before-entries (subvec (:entries (:ledger persisted))
                                    0 (count before-entries))))
      (is (= :out-of-band-fix
             (get-in persisted [:ledger :entries (count before-entries)
                                :medic-disposition])))
      (is (empty? (get-in (ledger/effective-active-injuries
                           (:ledger persisted) scoped)
                          [:injuries]))))
    (testing "the operation-specific verifier never receives prior operations"
      (is (every? #(not= (:operation-id operation) %) @normal-calls)))))

(deftest different-injury-receipt-cannot-authorise-this-entry
  (let [history (edn/read-string (slurp ledger-fixture))
        root (temp-root)
        ledger-path (str (io/file root "ledger.edn"))
        wrong-receipt (assoc (edn/read-string (slurp receipt-fixture))
                             :injury-id "injury/different")
        wrong-pin (out-of-band/retain! (.getPath root) "receipts" wrong-receipt)
        normal (retained-normal-authority history (atom []))
        open-entry (assoc (get-in (ledger/effective-active-injuries history normal)
                                  [:injuries injury-id])
                          :injury-id injury-id)
        {:keys [operation attribution]}
        (out-of-band/checked-medic-operation
         history open-entry (:artifact-pin wrong-receipt) wrong-pin wrong-receipt)
        specific (out-of-band/checked-medic-authority
                  operation wrong-pin wrong-receipt)
        scoped (out-of-band/scoped-medic-authority normal operation specific)
        _ (spit ledger-path (slurp ledger-fixture))
        before (slurp ledger-path)
        result (store/transact-medic-disposition!
                ledger-path wrong-pin operation attribution scoped)]
    (is (= :medic/authority-rejected (:reason result)) (pr-str result))
    (is (= before (slurp ledger-path)))))
