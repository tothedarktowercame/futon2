(ns futon2.aif.cause-reattribution
  "Retained authority for an exact atomic cause reattribution."
  (:require [clojure.edn :as edn]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.load-identity :as load-identity])
  (:import [java.nio.file Files Paths]))

(def receipt-schema :wm/cause-reattribution-receipt-v1)
(def verifier "DarkTower.WarMachine.HistoricalMisclassificationCorrection")

(defn- read-pin [{:keys [path sha256]}]
  (let [bytes (Files/readAllBytes (Paths/get path (make-array String 0)))]
    (when-not (= sha256 (load-identity/sha256 bytes))
      (throw (ex-info "cause reattribution receipt pin mismatch" {})))
    (edn/read-string (String. bytes "UTF-8"))))

(defn checked-operation [ledger-pin receipt-pin receipt]
  (let [candidate (:candidate receipt)
        request {:candidate candidate :evidence candidate}]
    {:operation-id (identity/digest
                    [:cause-reattribution
                     {:ledger-pin ledger-pin :request request
                      :authority-receipt-pin receipt-pin}])
     :expected-ledger-pin ledger-pin
     :request request
     :authority-receipt-pin receipt-pin}))

(defn retained-authority
  [{:keys [ledger operation current candidate evidence]}]
  (try
    (let [pin (:authority-receipt-pin operation)
          receipt (read-pin pin)
          expected (checked-operation (:expected-ledger-pin operation) pin receipt)]
      (and (= receipt-schema (:schema receipt))
           (= :checked (:status receipt))
           (= verifier (:verifier receipt))
           (= candidate evidence (:candidate receipt))
           (= (:injury-id current) (:injury-id candidate))
           (= operation expected)
           (= (:ledger-pin ledger) (:expected-ledger-pin operation))))
    (catch Throwable _ false)))
