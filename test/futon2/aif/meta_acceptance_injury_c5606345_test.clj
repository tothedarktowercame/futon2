(ns futon2.aif.meta-acceptance-injury-c5606345-test
  "Acceptance pin for injury/c5606345ac99815bbb5387aa0954822de8054319.

  Pinned run record: /home/joe/code/futon2/data/wm-runs/
  tick-run-record-2026-10-27-1586bf55-9ad7-4d72-aa70-827be7506212.edn
  sha256 8a19a4d80344f89a0d4ac35fffc235cd63f6e83866813970fd9abd608a63a2e4.

  The injury: a WM retry-repair run (click
  wm-retry-repair/2f6826382458c5b36338189a8623f41abb120f92-2-patch-02e9d5e7e11e92479d586ad078ce960902c36fa1)
  died at :ticket-readiness-refresh with
  {:kind :wm-job-ledger-unavailable
   :error \"WM dispatch lacks its run-local job ledger context\"}
  because the ticket-readiness Agency contact dispatched through opts that
  did not carry :registered-run/job-ledger — the ledger run-opportunity!
  mints only after the composition root has closed over its opts.

  Review correction (first proposal): the pre-repair failures must be the
  injury's own reason only. This version confines the failing assertions to
  the repair pin — the selection entrypoint must surface the same typed
  ledger refusal dispatch! enforces instead of bridging a ledger-less WM
  dispatch — while every other assertion holds on the injured code as it
  stands, so a pre-repair run fails exactly there and a post-repair run
  passes. Only the real production functions from the diagnosis's
  :production-functions are exercised; :strategic-selection-invoke-fn is
  the entrypoint's own documented injection port, not a stub."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.wm.loop-node-exercise :as loop-node-exercise]
            [futon2.aif.run-ending-classification :as run-ending]))

;; Literals pinned from the run record.
(def pinned-run-id "2026-10-27-1586bf55-9ad7-4d72-aa70-827be7506212")
(def pinned-click-id
  "wm-retry-repair/2f6826382458c5b36338189a8623f41abb120f92-2-patch-02e9d5e7e11e92479d586ad078ce960902c36fa1")
(def pinned-request {:target "M-giraffe" :schema :wm/strategic-selection-request-v1})
(def healthy-bridge-response
  {:ok true
   :selection {:status "selected"
               :actuation {:status "ok"}}})

(defn wm-selection-opts [ledger]
  (cond-> {:run-id pinned-run-id
           :click-id pinned-click-id
           :agency-base "http://127.0.0.1:1"
           :strategic-selection-invoke-fn (constantly healthy-bridge-response)}
    ledger (assoc :registered-run/job-ledger ledger)))

(deftest selection-entrypoint-refuses-ledgerless-wm-dispatch
  (testing "REPAIR PIN — a WM selection whose opts carry a run-id but no
  :registered-run/job-ledger must surface the same typed refusal dispatch!
  enforces; on injured code the entrypoint bridges the ledger-less dispatch
  and returns a selection, which is exactly this run's failure mode"
    (let [attempted (atom false)
          opts (assoc (wm-selection-opts nil)
                      :strategic-selection-invoke-fn
                      (fn [_]
                        (reset! attempted true)
                        healthy-bridge-response))
          refusal (try
                    (runner/strategic-selection! opts pinned-request)
                    nil
                    (catch clojure.lang.ExceptionInfo e
                      (ex-data e)))]
      (is (map? refusal)
          "injured code bridges the ledger-less dispatch and returns a selection")
      (when (map? refusal)
        (is (= :wm-job-ledger-unavailable (:failure-kind refusal)))
        (is (= pinned-run-id (:run-id refusal)))
        (is (false? @attempted)
            "the refusal must fire before the bridge, as dispatch! does"))))
  (testing "a present run-local ledger must not block selection — holds on
  injured code too, so this deftest's only pre-repair failure is the pin"
    (let [result (runner/strategic-selection!
                  (wm-selection-opts (atom {:order [] :jobs {}}))
                  pinned-request)]
      (is (map? result))
      (is (= :selected (:status result))))))

(deftest loop-node-receipt-shape-is-stable-evidence
  (testing "the loop-node evaluator returns its typed receipt shape for an
  empty decision — stable across the repair, so this deftest passes both
  before and after"
    (let [r (loop-node-exercise/receipt {})]
      (is (map? r))
      (is (keyword? (:status r)))
      (let [c (:counts r)]
        (is (map? c))
        (is (contains? c :present))
        (is (contains? c :bypassed))
        (is (contains? c :refused))))))

(deftest terminal-routing-records-a-typed-receipt
  (testing "terminal routing returns a recorded receipt for the pinned close
  shape — stable across the repair, so this deftest passes both before and
  after"
    (let [close {:outcome :incomplete
                 :grounded? false
                 :artifact-only? false
                 :failure-kind :wm-job-ledger-unavailable}
          classed (run-ending/classify {:close close})]
      (is (map? classed))
      (is (= :recorded (:status classed)))
      (is (keyword? (:class classed))))))
