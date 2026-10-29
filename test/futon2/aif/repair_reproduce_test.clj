(ns futon2.aif.repair-reproduce-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-injury-ledger :as injury-ledger]
            [futon2.aif.repair-reproduce :as reproduce]))

(def cases
  [{:label "a9 producer-qualified already-satisfied"
    :injury-id "injury/a9b900a41f704673f9d7f35f545f0482b06135ea"
    :ledger-path "/home/joe/notes/wm-next/backlog-triage/snapshots/ledger.edn"
    :record-path (str "/home/joe/code/futon2/data/wm-runs/"
                      "tick-run-record-2026-10-24-d0608eec-cca3-45de-9c19-14d6db26577d.edn")
    :classification {:classification :non-injury
                     :non-injury-reason :producer-qualified-already-satisfied
                     :failure-kind :already-satisfied
                     :failure-stage :author-wait}
    :empty-transition :observation
    :retry-transition :retry-inconclusive}
   {:label "entry 45 successful close"
    :injury-id "injury/9c6220388033cbd1fbc888cdf6bb3a5b9a3082c0"
    :ledger-path "/home/joe/notes/wm-next/offline-repair/authority/45-evaluation/ledger-snapshot.edn"
    :record-path (str "/home/joe/code/futon2/data/wm-runs/"
                      "tick-run-record-2026-10-26-8be8ff03-5009-4f3d-8c12-5b11cfd46d92.edn")
    :classification {:classification :non-injury
                     :non-injury-reason :successful-task-outcome
                     :failure-kind :none
                     :failure-stage :unknown-stage}
    :empty-transition :observation
    :retry-transition :discharged}])

(defn- available? [{:keys [ledger-path record-path]}]
  (and (.isFile (io/file ledger-path)) (.isFile (io/file record-path))))

(defn- read-ledger [path]
  (edn/read-string (slurp path)))

(deftest historical-reproductions
  (doseq [{:keys [label injury-id ledger-path record-path classification
                  empty-transition retry-transition] :as replay} cases]
    (testing label
      (if-not (available? replay)
        (is true (str "SKIP: retained authority files absent for " label))
        (let [receipt (reproduce/reproduce {:injury-id injury-id
                                            :ledger (read-ledger ledger-path)
                                            :record-path record-path})]
          (is (= reproduce/receipt-schema (:schema receipt)))
          (is (= classification
                 (select-keys (:classification receipt)
                              [:classification :non-injury-reason
                               :failure-kind :failure-stage])))
          (is (= empty-transition
                 (get-in receipt [:integrations :empty-history :transition])))
          (is (= retry-transition
                 (get-in receipt [:integrations :enacted-retry :transition])))
          (is (= :non-injury (:reproduced-as receipt)))
          (is (= :unknown (:classifier-revision receipt)))
          (is (= (str "repair-reproduce-receipts/" injury-id "/"
                      (get-in receipt [:record-pin :sha256]) ".edn")
                 (get-in receipt [:receipt-pin :path]))))))))

(deftest mismatched-record-bytes-are-refused
  (let [{:keys [injury-id ledger-path record-path] :as replay} (first cases)]
    (if-not (available? replay)
      (is true "SKIP: retained a9 authority files absent")
      (let [history (read-ledger ledger-path)
            tampered (update history :entries
                             (fn [entries]
                               (mapv (fn [entry]
                                       (if (and (= injury-ledger/transition-schema
                                                   (:schema entry))
                                                (= injury-id (:injury-id entry))
                                                (:cause-record-pin entry))
                                         (assoc-in entry [:cause-record-pin :sha256]
                                                   (apply str (repeat 64 "0")))
                                         entry))
                                     entries)))
            result (reproduce/reproduce {:injury-id injury-id
                                         :ledger tampered
                                         :record-path record-path})]
        (is (= :refused (:status result)))
        (is (= :record-pin-mismatch (:reason result)))))))
