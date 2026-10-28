(ns futon2.aif.retry-patch-identity-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-repair-coordinator :as coordinator]
            [futon2.aif.meta-repair-coordinator-test :as fixture]
            [futon2.aif.meta-self-heal-production :as production]))

(defn- temp-ledger-path []
  (let [dir (io/file "target/retry-patch-identity" (str (java.util.UUID/randomUUID)))]
    (.mkdirs dir)
    (str (io/file dir "ledger.edn"))))

(defn- stopped-result [history transaction ports initial]
  (try
    (#'coordinator/coordinate-queued-retry
     history transaction ports
     (get-in initial [:transaction :work-order :injury-id])
     (get-in initial [:transaction :work-order :repair-id])
     1 nil)
    (catch clojure.lang.ExceptionInfo e
      (:result (ex-data e)))))

(deftest requeue-mints-from-injury-authored-patch
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
    (testing "an initial enacted entry uses its matching integration commit"
      (let [enacted (last (filter #(= :enacted (:state %))
                                  (get-in initial [:ledger :entries])))]
        (is (not (contains? enacted :patch-sha)))
        (is (= fixture/sha-a (:integration-commit enacted)))))))

(deftest foreign-transaction-author-cannot-change-requeue-patch
  (let [initial (#'fixture/awaiting-retry (#'fixture/opened-ledger))
        history (#'fixture/inconclusive (:ledger initial) "2-first" "click-retry-repair-1")
        foreign-patch fixture/sha-b
        contaminated (#'coordinator/seal-transaction
                      (assoc-in (:transaction initial)
                                [:receipts 0 :payload :patch-sha]
                                foreign-patch))
        minted (atom nil)
        ps (#'fixture/ports
            {:retry-click-id (fn [repair-id attempt ordinal patch-sha]
                               (reset! minted [repair-id attempt ordinal patch-sha])
                               (str "retry/" patch-sha))
             :retry (fn [r]
                      (#'fixture/receipt r :queued
                                         {:retry-click (:retry-click r)}))})
        result (#'coordinator/coordinate-queued-retry
                history contaminated ps
                (get-in initial [:transaction :work-order :injury-id])
                (get-in initial [:transaction :work-order :repair-id])
                1 nil)
        retry-click (:retry-click result)
        retry {:injury-id (get-in initial [:transaction :work-order :injury-id])
               :repair-id (get-in initial [:transaction :work-order :repair-id])
               :retry-click retry-click
               :patch-sha fixture/sha-a}
        ledger-path (temp-ledger-path)]
    (is (= fixture/sha-a (last @minted)))
    (is (= (str "retry/" fixture/sha-a) retry-click))
    (spit ledger-path (str (pr-str (:ledger result)) "\n"))
    (is (= :ready
           (:status (production/attributable-retry-readiness
                     {:meta-injury-ledger-path ledger-path}
                     retry))))))

(deftest requeue-refuses-missing-malformed-and-disagreeing-ledger-patches
  (let [initial (#'fixture/awaiting-retry (#'fixture/opened-ledger))
        history (#'fixture/inconclusive (:ledger initial) "2-first" "click-retry-repair-1")
        transaction (:transaction initial)
        ps (#'fixture/ports)
        authored? #(= :authored (:state %))
        enacted? #(= :enacted (:state %))
        missing (update history :entries
                        (fn [entries] (vec (remove authored? entries))))
        malformed (update history :entries
                          (fn [entries]
                            (mapv #(if (authored? %) (assoc % :patch-sha "bad") %)
                                  entries)))
        disagreeing (update history :entries
                            (fn [entries]
                              (mapv #(if (enacted? %)
                                       (assoc % :integration-commit fixture/sha-b)
                                       %)
                                    entries)))
        missing-result (stopped-result missing transaction ps initial)
        malformed-result (stopped-result malformed transaction ps initial)
        disagreement-result (stopped-result disagreeing transaction ps initial)]
    (is (= :coordinator/retry-authored-transition-unavailable
           (:reason missing-result)))
    (is (= :coordinator/retry-authored-patch-invalid
           (:reason malformed-result)))
    (is (= :coordinator/retry-enacted-patch-disagrees
           (:reason disagreement-result)))
    (is (= :integration-commit (:field disagreement-result)))
    (is (= fixture/sha-a (:expected disagreement-result)))
    (is (= fixture/sha-b (:actual disagreement-result)))))
