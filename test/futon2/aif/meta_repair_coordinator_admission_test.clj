(ns futon2.aif.meta-repair-coordinator-admission-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-repair-coordinator :as coordinator]
            [futon2.aif.meta-repair-coordinator-test :as fixture]))

(defn- start []
  (let [history (#'fixture/opened-ledger)
        prepared (#'fixture/prepared history (#'fixture/options))]
    {:history history :transaction (:transaction prepared)}))

(defn- pause [history transaction phase overrides]
  (coordinator/coordinate
   (#'fixture/durable history) transaction
   (assoc (#'fixture/ports overrides) :stop-after phase)))

(defn- review-port [reviewer-sha marker]
  (fn [request]
    (#'fixture/receipt request :succeeded
                       {:patch-sha fixture/sha-a
                        :reviewer-sha reviewer-sha
                        :admission-marker marker})))

(defn- enact-port [marker]
  (fn [request]
    (#'fixture/receipt request :succeeded
                       {:patch-sha fixture/sha-a
                        :integration-commit fixture/sha-a
                        :retry-click "click-retry-repair-1"
                        :admission-marker marker})))

(deftest review-receipt-must-be-the-ledger-admitted-receipt
  (let [{:keys [history transaction]} (start)
        authored (pause history transaction :author {})
        admitted (pause (:ledger authored) (:transaction authored) :review
                        {:review (review-port fixture/sha-b :admitted)})
        fresh (pause (:ledger authored) (:transaction authored) :review
                     {:review (review-port fixture/sha-b :fresh)})
        refusal (coordinator/coordinate
                 (#'fixture/durable (:ledger admitted)) (:transaction fresh)
                 (assoc (#'fixture/ports) :stop-after :review))]
    (testing "a same-payload but differently sealed review is not silently kept"
      (is (= :refused (:status refusal)))
      (is (= :coordinator/unadmitted-review-receipt (:reason refusal)))
      (is (= (get-in admitted [:transaction :receipts 1 :receipt-pin])
             (:admitted-receipt-pin refusal)))
      (is (= (get-in fresh [:transaction :receipts 1 :receipt-pin])
             (:received-receipt-pin refusal))))))

(deftest enact-receipt-must-be-the-ledger-admitted-receipt
  (let [{:keys [history transaction]} (start)
        reviewed (pause history transaction :review {})
        admitted (pause (:ledger reviewed) (:transaction reviewed) :enact
                        {:enact (enact-port :admitted)})
        fresh (pause (:ledger reviewed) (:transaction reviewed) :enact
                     {:enact (enact-port :fresh)})
        refusal (coordinator/coordinate
                 (#'fixture/durable (:ledger admitted)) (:transaction fresh)
                 (#'fixture/ports))]
    (testing "a fresh enact receipt cannot ride an already enacted ledger"
      (is (= :refused (:status refusal)))
      (is (= :coordinator/unadmitted-enact-receipt (:reason refusal)))
      (is (= (get-in admitted [:transaction :receipts 2 :receipt-pin])
             (:admitted-receipt-pin refusal)))
      (is (= (get-in fresh [:transaction :receipts 2 :receipt-pin])
             (:received-receipt-pin refusal))))))

(deftest admitted-receipts-replay-and-proceed
  (let [{:keys [history transaction]} (start)
        enacted (pause history transaction :enact {})
        result (coordinator/coordinate
                (#'fixture/durable (:ledger enacted)) (:transaction enacted)
                (#'fixture/ports))]
    (is (= :discharged (:status result)))
    (is (= (:receipt-pin (second (get-in enacted [:transaction :receipts])))
           (get-in enacted [:ledger :entries 3 :port-receipt-pin])))
    (is (= (:receipt-pin (nth (get-in enacted [:transaction :receipts]) 2))
           (get-in enacted [:ledger :entries 4 :port-receipt-pin])))))
