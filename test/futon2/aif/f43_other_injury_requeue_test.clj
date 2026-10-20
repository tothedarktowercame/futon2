(ns futon2.aif.f43-other-injury-requeue-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-ledger-test :as fixture]))

(def injury-a "injury/2026-10-19/meta-route")
(def injury-b "injury/other-open")
(def pin-p {:path "data/wm-runs/other-open.edn"
            :sha256 "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
(def pin-q {:path "data/wm-runs/no-injury.edn"
            :sha256 "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"})

(defn- append! [history transition]
  (:ledger (ledger/append-transition history transition)))

(defn- open-other [history]
  (append! history {:injury-id injury-b :state :open :attempt 1
                    :cause-record-pin pin-p :failure-stage :selection
                    :failure-kind :other-injury :detected-click "other-click"}))

(defn- observe [history pin]
  (:ledger (ledger/append-observation
            history pin :retry-inconclusive
            {:injury-id injury-a
             :retry-click (get-in (ledger/active-injuries history)
                                  [:injuries injury-a :retry-click])})))

(defn- at-retry-limit []
  (reduce (fn [history n]
            (:ledger (ledger/append-requeue history injury-a (str "retry/" n))))
          (fixture/through-enacted) (range 1 4)))

(deftest other-open-injury-does-not-spend-retry-budget
  (testing "an attributed other injury retains the current requeue count"
    (let [history (-> (fixture/through-enacted) open-other (observe pin-p))
          result (ledger/append-requeue history injury-a "retry/other-1")]
      (is (= :appended (:status result)))
      (is (= :enacted (get-in result [:entry :state])))
      (is (= 0 (get-in result [:entry :requeues])))
      (is (= {:kind :other-open-injury :injury-id injury-b :record-pin pin-p}
             (get-in result [:entry :requeue-cause])))
      (is (= :valid (:status (ledger/validate-ledger (:ledger result)))))))

  (testing "an inconclusive child with no injury attribution spends one retry"
    (let [history (-> (fixture/through-enacted) (observe pin-q))
          result (ledger/append-requeue history injury-a "retry/no-injury-1")]
      (is (= :appended (:status result)))
      (is (= 1 (get-in result [:entry :requeues])))))

  (testing "only unattributed inconclusive children exhaust the retry budget"
    (let [spent-history (observe (at-retry-limit) pin-q)
          spent (ledger/append-requeue spent-history injury-a "retry/spent")
          retained-history (-> (at-retry-limit) open-other (observe pin-p))
          retained (ledger/append-requeue retained-history injury-a "retry/other-4")]
      (is (= :abandoned (get-in spent [:entry :state])))
      (is (= :retry-budget-spent (get-in spent [:entry :reason])))
      (is (= :enacted (get-in retained [:entry :state])))
      (is (= 3 (get-in retained [:entry :requeues])))))

  (testing "the validator rejects a forged other-injury attribution"
    (let [history (-> (fixture/through-enacted) open-other (observe pin-p))
          forged (ledger/validate-transition
                  history {:injury-id injury-a :state :enacted :attempt 1
                           :repair-id "repair/meta-self-heal-route"
                           :patch-sha fixture/patch-sha
                           :integration-commit fixture/integration-sha
                           :retry-click "retry/forged" :requeues 0
                           :requeue-cause {:kind :other-open-injury
                                           :injury-id injury-b
                                           :record-pin pin-q}})]
      (is (= :transition/invalid (:reason forged)))
      (is (some #{:requeue/evidence} (:defects forged)))))

  (testing "the production ledger remains valid"
    (let [production (edn/read-string
                      (slurp "/home/joe/code/futon2/data/wm-meta-injuries/ledger.edn"))]
      (is (= :valid (:status (ledger/validate-ledger production)))))))
