(ns futon2.aif.f45-requeue-ordinal-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-repair-coordinator :as coordinator]
            [futon2.aif.meta-repair-coordinator-test]))

(def injury-a "injury/2026-10-19/reload")
(def live-injury "injury/d755c886a22d6fc8c04db7fada64089e07157f62")

(defn- fixture-var [sym]
  (var-get (ns-resolve 'futon2.aif.meta-repair-coordinator-test sym)))

(defn- pin [suffix digit]
  {:path (str "data/wm-runs/" suffix ".edn")
   :sha256 (apply str (repeat 64 digit))})

(defn- append-transition! [history transition]
  (let [result (ledger/append-transition history transition)]
    (is (= :appended (:status result)) (pr-str result))
    (:ledger result)))

(defn- observe [history record-pin]
  (let [retry-click (get-in (ledger/active-injuries history)
                            [:injuries injury-a :retry-click])
        result (ledger/append-observation
                history record-pin :retry-inconclusive
                {:injury-id injury-a :retry-click retry-click})]
    (is (= :appended (:status result)) (pr-str result))
    (:ledger result)))

(defn- open-other [history injury-id record-pin]
  (append-transition!
   history {:injury-id injury-id :state :open :attempt 1
            :cause-record-pin record-pin :failure-stage :selection
            :failure-kind :other-injury :detected-click (str "click-" injury-id)}))

(deftest phase-and-click-use-requeue-ordinal-not-budget-count
  (let [opened-ledger (fixture-var 'opened-ledger)
        awaiting-retry (fixture-var 'awaiting-retry)
        inconclusive (fixture-var 'inconclusive)
        durable (fixture-var 'durable)
        ports (fixture-var 'ports)
        receipt (fixture-var 'receipt)
        initial (awaiting-retry (opened-ledger))
        calls (atom [])
        ps (ports {:retry (fn [request]
                            (swap! calls conj request)
                            (receipt request :queued
                                     {:retry-click (:retry-click request)}))})
        history1 (inconclusive (:ledger initial) "1-first"
                               "click-retry-repair-1")
        first (coordinator/coordinate (durable history1) (:transaction initial) ps)
        history2 (inconclusive (:ledger first) "2-second"
                               "click-retry-requeue-1")
        second (coordinator/coordinate (durable history2) (:transaction first) ps)
        other-pin-3 (pin "other-third" "3")
        history3 (-> (:ledger second)
                     (open-other "injury/other-third" other-pin-3)
                     (observe other-pin-3))
        third (coordinator/coordinate (durable history3) (:transaction second) ps)
        call-count-after-third (count @calls)
        replay (coordinator/coordinate (durable (:ledger third))
                                       (:transaction third) ps)
        call-count-after-replay (count @calls)
        other-pin-4 (pin "other-fourth" "4")
        history4 (-> (:ledger replay)
                     (open-other "injury/other-fourth" other-pin-4)
                     (observe other-pin-4))
        fourth (coordinator/coordinate (durable history4) (:transaction replay) ps)]
    (testing "ordinary inconclusive retries retain their existing phase sequence"
      (is (= [:retry-requeue-1 :retry-requeue-2]
             (mapv :phase (take 2 @calls))))
      (is (= ["click-retry-requeue-1" "click-retry-requeue-2"]
             (mapv :retry-click (take 2 @calls))))
      (is (= [1 2] (mapv :requeue-ordinal (take 2 @calls))))
      (is (= 1 (get-in first [:ledger :entries (dec (count (:entries (:ledger first))))
                              :requeues])))
      (is (= 2 (get-in second [:ledger :entries (dec (count (:entries (:ledger second))))
                               :requeues]))))

    (testing "an other-injury retry advances identity while preserving budget"
      (is (= :awaiting-retry (:status third)))
      (is (= :retry-requeue-3 (:phase (nth @calls 2))))
      (is (= "click-retry-requeue-3" (:retry-click (nth @calls 2))))
      (is (= 3 (:requeue-ordinal (nth @calls 2))))
      (is (= 2 (-> third :ledger :entries last :requeues)))
      (is (= "click-retry-requeue-3" (-> third :ledger :entries last :retry-click))))

    (testing "resume replays the ordinal-three receipt without effects"
      (is (= :awaiting-retry (:status replay)))
      (is (= call-count-after-third call-count-after-replay))
      (is (= (:ledger third) (:ledger replay))))

    (testing "a second other-injury retry advances to ordinal four"
      (is (= :awaiting-retry (:status fourth)))
      (is (= :retry-requeue-4 (:phase (last @calls))))
      (is (= "click-retry-requeue-4" (:retry-click (last @calls))))
      (is (= 4 (:requeue-ordinal (last @calls))))
      (is (= 2 (-> fourth :ledger :entries last :requeues))))))

(def live-fixture
  "The live injury's transitions as they stood after click 15 (ordinal 25 the
  latest entry), projected to the keys requeue-ordinal reads, values verbatim.
  The production ledger is not read here: it moves with every click, and a pin
  on it goes red on the next re-queue. Review fix, F45 (claude-5)."
  "test/futon2/aif/fixtures/meta-injury-ledger-2026-10-21-d755c886-requeue-ordinal.edn")

(deftest live-ledger-distinguishes-ordinal-from-budget
  (let [captured (edn/read-string (slurp live-fixture))
        latest (last (filter #(= live-injury (:injury-id %)) (:entries captured)))]
    (is (= 15 (count (:entries captured))))
    (is (= 25 (:ordinal latest)))
    (is (= :enacted (:state latest)))
    (is (= 3 (ledger/requeue-ordinal captured live-injury)))
    (is (= 2 (:requeues latest)))))
