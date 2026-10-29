(ns futon2.aif.out-of-band-load-authority-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.action-identity :as action-identity]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-medic-authority :as authority]
            [futon2.aif.meta-self-heal-production :as production]
            [futon2.aif.out-of-band-fix :as out-of-band]))

(def fixture-root "test/resources/wm/open-medic-load-authority")
(def ledger-fixture (str fixture-root "/ledger-a654144f.edn"))
(def receipt-fixture (str fixture-root "/receipt-198c734a.edn"))

(defn- temp-root []
  (.toFile (java.nio.file.Files/createTempDirectory
            "open-medic-load-authority-"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- exact-prior-authority [history]
  (let [operations
        (into {}
              (keep (fn [entry]
                      (when (and (= :medic-repaired (:observation-kind entry))
                                 (not= :out-of-band-fix (:medic-disposition entry)))
                        (let [operation (:medic-operation entry)]
                          [(:operation-id operation) operation]))))
              (:entries history))]
    (fn [{:keys [operation current candidate evidence]}]
      (and (= operation (get operations (:operation-id operation)))
           (= candidate evidence (get-in operation [:request :candidate]))
           (= [(:injury-id candidate) (:entry-id candidate)]
              [(:injury-id current) (:entry-id current)])))))

(defn- pin [path]
  {:path path
   :sha256 (identity/sha256
            (java.nio.file.Files/readAllBytes (.toPath (io/file path))))})

(defn- rehome-last-receipt
  "Keep the retained ledger semantics but point ordinal 116 at a receipt in
  the hermetic test root. Recompute only identities that bind that path."
  [history receipt-pin]
  (let [entry (last (:entries history))
        operation (-> (:medic-operation entry)
                      (assoc :authority-receipt-pin receipt-pin))
        operation (assoc operation :operation-id
                         (action-identity/digest
                          [:open-out-of-band-medic
                           {:ledger-pin (:expected-ledger-pin operation)
                            :request (:request operation)
                            :authority-receipt-pin receipt-pin}]))
        entry (-> entry
                  (assoc :medic-operation operation
                         :record-pin receipt-pin
                         :out-of-band-receipt-pin receipt-pin)
                  (assoc-in [:medic-stamp :receipt-pin] receipt-pin)
                  ledger/seal-entry)]
    (ledger/seal-ledger
     (assoc history :entries (conj (pop (:entries history)) entry)))))

(defn- loaders [ledger-path verifier transaction-path]
  (let [opts {:meta-injury-ledger-path ledger-path
              :meta-repair-transaction-path transaction-path
              :meta-medic-authority-verifier verifier}
        context (production/diagnosis-context opts)]
    {:load-ledger (store/load-ledger ledger-path verifier)
     :load-durable-ledger (store/load-durable-ledger ledger-path verifier)
     :diagnosis-context (:durable-ledger context)
     :production-readiness (production/production-readiness opts context)}))

(deftest ordinal-116-is-the-entry-missing-from-the-unextended-normal-authority
  (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file ledger-fixture)))
        history (edn/read-string (String. bytes "UTF-8"))
        prior (ledger/seal-ledger (update history :entries pop))
        normal (exact-prior-authority history)]
    (is (= "a654144f574a4a766cded4295a419ef9c4aef24c124a96e261f08d28c4ef57c9"
           (identity/sha256 bytes)))
    (is (= 116 (:ordinal (last (:entries history)))))
    (is (= :valid (:status (ledger/validate-ledger prior normal))))
    (is (= {:status :refused :reason :ledger/invalid
            :defects [:entry/transition-invalid]}
           (ledger/validate-ledger history normal)))))

(deftest every-production-loader-rederives-ordinal-116-from-its-receipt
  (let [history (edn/read-string (slurp ledger-fixture))
        root (temp-root)
        receipt-path (str (io/file root "receipt-198c734a.edn"))
        ledger-path (str (io/file root "ledger.edn"))
        transaction-path (str (io/file root "transaction.edn"))
        _ (spit receipt-path (slurp receipt-fixture))
        history (rehome-last-receipt history (pin receipt-path))
        _ (spit ledger-path (str (pr-str history) "\n"))
        verifier (authority/with-retained-authorities
                  (exact-prior-authority history))
        results (loaders ledger-path verifier transaction-path)]
    (is (= #{:loaded} (set (map :status
                                ((juxt :load-ledger :load-durable-ledger
                                       :diagnosis-context) results))))
        (pr-str results))
    (is (= 116 (count (get-in results [:load-ledger :ledger :entries]))))
    (is (= 116 (count (get-in results [:load-durable-ledger :ledger :entries]))))
    (is (= 116 (count (get-in results [:diagnosis-context :ledger :entries]))))
    (is (= :ready (get-in results [:production-readiness :status]))
        (pr-str (:production-readiness results)))))

(deftest tampered-or-other-injury-receipts-refuse-through-every-loader
  (let [source (edn/read-string (slurp ledger-fixture))
        root (temp-root)
        transaction-path (str (io/file root "transaction.edn"))
        receipt-path (str (io/file root "receipt.edn"))
        ledger-path (str (io/file root "ledger.edn"))
        _ (spit receipt-path (slurp receipt-fixture))
        correct-pin (pin receipt-path)
        correct-ledger (rehome-last-receipt source correct-pin)
        verifier (authority/with-retained-authorities
                  (exact-prior-authority source))]
    (testing "hash drift in the retained receipt fails every load"
      (spit ledger-path (str (pr-str correct-ledger) "\n"))
      (spit receipt-path (str (slurp receipt-path) "\n"))
      (let [results (loaders ledger-path verifier transaction-path)]
        (is (every? #(= :refused (:status %))
                    ((juxt :load-ledger :load-durable-ledger
                           :diagnosis-context :production-readiness) results))
            (pr-str results))))
    (testing "a valid receipt naming another injury cannot validate ordinal 116"
      (let [other (assoc (edn/read-string (slurp receipt-fixture))
                         :injury-id "injury/different")
            other-pin (out-of-band/retain! (.getPath root) "other" other)
            other-ledger (rehome-last-receipt source other-pin)
            _ (spit ledger-path (str (pr-str other-ledger) "\n"))
            results (loaders ledger-path verifier transaction-path)]
        (is (every? #(= :refused (:status %))
                    ((juxt :load-ledger :load-durable-ledger
                           :diagnosis-context :production-readiness) results))
            (pr-str results))))))
