(ns futon2.aif.f46-repeated-injury-requeue-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-terminal-injury :as terminal]
            [futon2.aif.meta-terminal-injury-test]))

(defn- fixture-var [sym]
  (var-get (ns-resolve 'futon2.aif.meta-terminal-injury-test sym)))

(defn- budget-two []
  (let [{:keys [injury-id ledger]} ((fixture-var 'enacted-ledger) "retry/base")
        r1 (ledger/append-requeue ledger injury-id "retry/one")
        r2 (ledger/append-requeue (:ledger r1) injury-id "retry/two")]
    {:injury-id injury-id :ledger (:ledger r2)}))

(defn- repeat-on-retry [history retry-click record-pin]
  (let [classify (fixture-var 'classify)
        seed (classify {:run/id "other-seed" :click/id "other-detection"
                        :failure {:kind :review-failed :stage :review}}
                       ((fixture-var 'pin-of) "c"))
        opened (:ledger (terminal/integrate history seed))
        repeated (classify {:run/id "other-repeat" :click/id retry-click
                            :failure {:kind :review-failed :stage :review}}
                           record-pin)]
    (terminal/integrate opened repeated)))

(deftest repeated-active-injury-does-not-spend-retry-budget
  (testing "a repeated other injury is attributed and preserves budget two"
    (let [{:keys [injury-id ledger]} (budget-two)
          pin ((fixture-var 'pin-of) "d")
          integrated (repeat-on-retry ledger "retry/two" pin)
          repeat-observation (->> (:entries (:ledger integrated))
                                  (filter #(= :repeated-active-injury
                                              (:observation-kind %))) last)
          result (ledger/append-requeue (:ledger integrated) injury-id "retry/three")]
      (is (= (:injury-id (:classification integrated))
             (:injury-id repeat-observation)))
      (is (= 2 (get-in result [:entry :requeues])))
      (is (= {:kind :other-open-injury
              :injury-id (:injury-id (:classification integrated))
              :record-pin pin}
             (get-in result [:entry :requeue-cause])))
      (is (= :valid (:status (ledger/validate-ledger (:ledger result)))))))

  (testing "an attributed repeat still requeues at the budget boundary"
    (let [{:keys [injury-id ledger]} (budget-two)
          at-limit (:ledger (ledger/append-requeue ledger injury-id "retry/three"))
          pin ((fixture-var 'pin-of) "e")
          integrated (repeat-on-retry at-limit "retry/three" pin)
          result (ledger/append-requeue (:ledger integrated) injury-id "retry/four")]
      (is (= :enacted (get-in result [:entry :state])))
      (is (= 3 (get-in result [:entry :requeues])))))

  (testing "legacy unattributed repeat remains budget-consuming"
    (let [{:keys [injury-id ledger]} (budget-two)
          pin ((fixture-var 'pin-of) "f")
          repeated (:ledger (ledger/append-observation ledger pin
                                                       :repeated-active-injury))
          inconclusive (:ledger (ledger/append-observation
                                 repeated pin :retry-inconclusive
                                 {:injury-id injury-id :retry-click "retry/two"}))
          result (ledger/append-requeue inconclusive injury-id "retry/three")]
      (is (= 3 (get-in result [:entry :requeues])))
      (is (nil? (get-in result [:entry :requeue-cause])))))

  (testing "no-work and same-injury observations remain budget-consuming"
    (doseq [kind [:no-work :same-injury]]
      (let [{:keys [injury-id ledger]} (budget-two)
            pin ((fixture-var 'pin-of) (if (= kind :no-work) "8" "9"))
            history (if (= kind :same-injury)
                      (:ledger (ledger/append-observation
                                ledger pin :repeated-active-injury
                                {:injury-id injury-id}))
                      ledger)
            observed (:ledger (ledger/append-observation
                                history pin :retry-inconclusive
                                {:injury-id injury-id :retry-click "retry/two"}))
            result (ledger/append-requeue observed injury-id "retry/three")]
        (is (= 3 (get-in result [:entry :requeues])) (name kind)))))

  (testing "the projected click-16 evidence records the legacy blind spot"
    (let [entries (edn/read-string
                   (slurp "test/futon2/aif/fixtures/meta-injury-ledger-click-16-projection.edn"))
          by-ordinal (into {} (map (juxt :ordinal identity)) entries)
          pin27 (get-in by-ordinal [27 :record-pin])]
      (is (nil? (get-in by-ordinal [26 :injury-id])))
      (is (= pin27 (get-in by-ordinal [26 :record-pin])))
      (is (not-any? #(= pin27 (:cause-record-pin %)) entries)))))
