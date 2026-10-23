(ns futon2.aif.retry-patch-identity-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.meta-repair-coordinator :as coordinator]
            [futon2.aif.meta-repair-coordinator-test :as fixture]))

(deftest requeue-mints-from-transaction-patch-and-refuses-its-absence
  (let [initial (#'fixture/awaiting-retry (#'fixture/opened-ledger))
        history (#'fixture/inconclusive (:ledger initial) "2-first" "click-retry-repair-1")
        args (atom [])
        ps (#'fixture/ports
            {:retry-click-id (fn [& xs] (swap! args conj xs) "patch-bound-retry")
             :retry (fn [r] (#'fixture/receipt r :queued {:retry-click (:retry-click r)}))})
        result (coordinator/coordinate (#'fixture/durable history) (:transaction initial) ps)]
    (is (= :awaiting-retry (:status result)))
    (is (= [(get-in initial [:transaction :work-order :repair-id]) 1 1 fixture/sha-a]
           (vec (first @args))))
    (is (= "patch-bound-retry" (get-in result [:ledger :entries (dec (count (get-in result [:ledger :entries]))) :retry-click])))
    (is (= "patch-bound-retry" (get-in (last (get-in result [:transaction :receipts])) [:payload :retry-click])))
    (reset! args [])
    (let [missing (#'coordinator/seal-transaction
                   (update-in (:transaction initial) [:receipts 0]
                              #(coordinator/seal-port-receipt
                                (update % :payload dissoc :patch-sha))))
          refused (coordinator/coordinate (#'fixture/durable history) missing ps)]
      (is (= :coordinator/author-payload-refused (:reason refused)))
      (is (empty? @args))
      (is (= history (:ledger refused))))))
