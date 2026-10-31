(ns futon2.aif.meta-repair-attempt-policy-test
  "One-author-attempt / one-retry-child operating caps (Joe ruling
  2026-10-31, codex-11 dispatch 8538; blocker receipt
  run001/adf04df0-single-repair/BLOCKER-no-start-one-attempt-bound.md).

  The raw ledger attempt limit (3) stays untouched history; the caps are
  enforced from sealed ledger evidence at the coordinator boundaries, so they
  survive restarts and repeated clicks. Negative controls with the cap raised
  (the documented explicit-grant shape) expose the excess author contacts and
  the second retry child the guard suppresses."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-repair-coordinator :as coordinator]
            [futon2.aif.meta-self-heal-harness :as harness]
            [futon2.aif.meta-self-heal-production :as production]))

(defn- root []
  (.getCanonicalPath
   (.toFile (java.nio.file.Files/createTempDirectory
             "wm-attempt-policy"
             (make-array java.nio.file.attribute.FileAttribute 0)))))

(defn- opts [env ports]
  (merge (harness/work-order-options env)
         {:meta-injury-ledger-path (:ledger-path env)
          :meta-repair-transaction-path (str (:root env) "/transaction.edn")
          :meta-acceptance-run-fn
          (constantly {:status :ran :outcome :passed :exit 0 :summary "fixture pass"
                       :run-pin {:path "fixture://acceptance-run"
                                 :sha256 (apply str (repeat 64 "a"))}})
          :meta-repair-ports ports}))

(defn- ledger-limit [env]
  (-> (store/load-durable-ledger (:ledger-path env)) :ledger :attempt-limit))

(deftest first-unspent-injury-is-allowed-and-discharges-normally
  (let [env (harness/prepare-environment! (root))
        counters (atom {})
        ports (harness/fake-ports (:repo env) counters :success)
        result (runtime/run-opportunity! (opts env ports))]
    (is (= :discharged (:status result)) (pr-str result))
    (is (= 1 (:author @counters)))
    (is (= 1 (:review @counters)))
    (is (= 1 (:enact @counters)))
    (is (empty? (:injuries (ledger/active-injuries
                            (:ledger (store/load-durable-ledger
                                      (:ledger-path env)))))))))

(deftest author-failure-spends-exactly-one-author-contact-despite-raw-limit-three
  (testing "the raw ledger limit stays 3; the stop is policy, not exhaustion"
    (let [env (harness/prepare-environment! (root))
          counters (atom {})
          ports (harness/fake-ports (:repo env) counters :author-failure)
          result (runtime/run-opportunity! (opts env ports))]
      (is (= 3 (ledger-limit env)))
      (is (= :refused (:status result)) (pr-str result))
      (is (= :meta-self-heal/preparation-refused (:reason result)))
      (is (= :coordinator/policy-author-attempt-cap
             (get-in result [:prepare :policy-attempt :reason])))
      (is (= [1] (get-in result [:prepare :policy-attempt :attempts-spent])))
      ;; exactly one author contact for the whole click, including its
      ;; author-failed continuation path:
      (is (= 1 (:author @counters)))
      (is (= [:reopened 2 :author-failed]
             (-> (store/load-durable-ledger (:ledger-path env)) :ledger :entries
                 last ((juxt :state :attempt :reason))))
          "the stop is not written as ledger exhaustion"))))

(deftest repeated-clicks-and-restarts-cannot-invoke-a-second-author
  (let [env (harness/prepare-environment! (root))
        counters1 (atom {})
        result1 (runtime/run-opportunity! (opts env (harness/fake-ports (:repo env)
                                                                counters1
                                                                :author-failure)))]
    (is (= :refused (:status result1)))
    (is (= 1 (:author @counters1)))
    ;; a fresh click reloads ledger and transaction state from disk; the
    ;; persisted spent attempt must refuse a new author attempt.
    (let [counters2 (atom {})
          result2 (runtime/run-opportunity! (opts env (harness/fake-ports (:repo env)
                                                                  counters2
                                                                  :success)))]
      (is (= :refused (:status result2)) (pr-str result2))
      (is (= :coordinator/policy-author-attempt-cap
             (get-in result2 [:prepare :policy-attempt :reason])))
      (is (zero? (or (:author @counters2) 0))
          "no second author contact on the later click"))))

(deftest negative-control-raised-cap-exposes-the-excess-author-contacts
  ;; The guard is the cap: with the documented explicit-grant shape (cap 3,
  ;; i.e. the raw ledger allowance) the SAME inputs dispatch author attempts
  ;; 2 and 3 that the default cap suppresses.
  (let [env (harness/prepare-environment! (root))
        counters (atom {})
        ports (harness/fake-ports (:repo env) counters :author-failure)
        result (runtime/run-opportunity! (assoc (opts env ports)
                                         :policy/author-attempt-cap 3))]
    (is (contains? #{:held :refused} (:status result)) (pr-str result))
    (is (= 3 (:author @counters))
        "without the operating cap the click itself spends attempts 1-3")))

;;; ---------------------------------------------------------------------------
;;; Retry-child cap (coordinator boundary; production arms the same default)
;;; ---------------------------------------------------------------------------

(def sha-a (apply str (repeat 40 "a")))
(def pin-a {:path "data/wm-learning/runs/2026-10-19/11250189.edn"
            :sha256 (apply str (repeat 64 "1"))})
(def diagnosis-pin {:path "holes/labs/wm-contract/meta-self-heal-diagnosis.edn"
                    :sha256 (apply str (repeat 64 "d"))})
(def source-pin {:path "/tmp/meta-injury-ledger.edn"
                 :sha256 (apply str (repeat 64 "e"))})

(defn- append! [history event]
  (let [result (ledger/append-transition history event)]
    (assert (= :appended (:status result)) (pr-str result))
    (:ledger result)))

(defn- opened-ledger []
  (append! ledger/empty-ledger
           {:injury-id "injury/2026-10-19/reload"
            :state :open :attempt 1 :cause-record-pin pin-a
            :failure-stage :reload :failure-kind :phase-failure
            :detected-click "click-2026-10-19-4"}))

(defn- durable [history]
  {:schema coordinator/durable-ledger-schema :status :loaded
   :source-pin source-pin :ledger history})

(defn- options []
  {:diagnosis-evidence-pin diagnosis-pin
   :acceptance-test {:id "retry-original-failure"
                     :command ["clojure" "-M:test" "-n" "focused"]
                     :expected {:exit 0}}
   :author "agent/author" :reviewer "agent/reviewer"
   :repair-reviewer "agent/repair-reviewer"
   :author-session "author-session-1"
   :reviewer-session "reviewer-session-1"
   :repair-reviewer-session "repair-reviewer-session-1"
   :acceptance-return-limit 1
   :target-repository "futon2"
   :target-worktree "/home/joe/worktrees/futon2-meta-self-heal-repair"})

(defn- receipt [request status payload]
  (let [order (:work-order request)]
    (coordinator/seal-port-receipt
     {:phase (:phase request) :injury-id (:injury-id order)
      :repair-id (:repair-id order) :attempt (:attempt order)
      :predecessor-pin (:predecessor-pin request)
      :status status :payload payload})))

(defn- ports [overrides]
  (merge
   {:author (fn [r] (receipt r :succeeded
                             {:patch-sha sha-a
                              :worktree (get-in r [:work-order :target-worktree])}))
    :review (fn [r] (receipt r :succeeded
                             {:patch-sha sha-a :reviewer-sha sha-a}))
    :enact (fn [r] (receipt r :succeeded
                            {:patch-sha sha-a :integration-commit sha-a
                             :retry-click "click-retry-repair-1"}))
    :retry-click-id (fn [_repair-id _attempt ordinal _patch-sha]
                      (str "click-retry-requeue-" ordinal))
    :retry (fn [r] (receipt r :succeeded
                            {:repair-id (get-in r [:work-order :repair-id])
                             :patch-sha sha-a :retry-click "click-retry-repair-1"
                             :outcome :grounded-change}))}
   overrides))

(defn- awaiting-retry [history]
  (let [start (coordinator/prepare (durable history) (options))]
    (coordinator/coordinate
     (durable history) (:transaction start)
     (ports {:retry (fn [r]
                      (receipt r :queued
                               {:repair-id (get-in r [:work-order :repair-id])
                                :patch-sha sha-a
                                :retry-click "click-retry-repair-1"}))}))))

(defn- inconclusive [history suffix retry-click]
  (let [result (ledger/append-observation
                history {:path (str "data/wm-runs/" suffix ".edn")
                         :sha256 (apply str (repeat 64 (subs suffix 0 1)))}
                :retry-inconclusive
                {:injury-id "injury/2026-10-19/reload"
                 :retry-click retry-click})]
    (assert (= :appended (:status result)) (pr-str result))
    (:ledger result)))

(deftest inconclusive-retry-child-gets-no-second-child-across-resume
  (let [initial (awaiting-retry (opened-ledger))
        history1 (inconclusive (:ledger initial) "2-first" "click-retry-repair-1")
        calls (atom [])
        retry-port (fn [r]
                     (swap! calls conj r)
                     (receipt r :queued {:retry-click (:retry-click r)}))
        armed (assoc (ports {:retry retry-port}) :policy/retry-child-cap 1)
        result (coordinator/coordinate (durable history1)
                                       (:transaction initial) armed)]
    (is (= :held (:status result)) (pr-str result))
    (is (= :coordinator/policy-retry-child-cap (:reason result)))
    (is (= ["click-retry-repair-1"]
           (get-in result [:retry-child-policy :recorded-retry-clicks])))
    (is (empty? @calls) "no second child is enqueued or dispatched")
    (is (= (count (:entries history1)) (count (:entries (:ledger result))))
        "the stop appends nothing; it is not the ledger's retry-budget state")
    (is (= :enacted (->> (:entries (:ledger result))
                         (filter :state) last :state)))
    ;; resume/restart on the same persisted state keeps holding:
    (let [resume (coordinator/coordinate (durable history1)
                                         (:transaction initial) armed)]
      (is (= :held (:status resume)))
      (is (= :coordinator/policy-retry-child-cap (:reason resume)))
      (is (empty? @calls)))
    ;; negative control (guard removed): the same inputs mint the second child
    (let [unarmed (ports {:retry retry-port})
          result2 (coordinator/coordinate (durable history1)
                                          (:transaction initial) unarmed)]
      (is (= :awaiting-retry (:status result2)) (pr-str result2))
      (is (= "click-retry-requeue-1" (:retry-click result2)))
      (is (= 1 (count @calls))
          "without the cap the second child IS enqueued — the guard, not a
           status keyword, is what suppresses it"))))

(deftest spent-attempt-counting-follows-sealed-transition-evidence
  (let [history (:ledger (awaiting-retry (opened-ledger)))
        injury-id "injury/2026-10-19/reload"]
    (testing "a fresh open injury has spent nothing"
      (is (= [] (coordinator/spent-author-attempts (opened-ledger) injury-id))))
    (testing "a claimed/enacted attempt 1 is spent; the policy refuses"
      (is (= [1] (coordinator/spent-author-attempts history injury-id)))
      (let [refused (coordinator/author-attempt-policy history injury-id
                                                       {:policy/author-attempt-cap 1})]
        (is (= :refused (:status refused)))
        (is (= :coordinator/policy-author-attempt-cap (:reason refused)))
        (is (= [1] (:attempts-spent refused)))
        (is (not= :attempt-limit-exhausted (:reason refused))
            "the stop is never described as ledger exhaustion")))
    (testing "an unarmed boundary (no explicit cap key) stays legacy-unlimited"
      (is (= :allowed (:status (coordinator/author-attempt-policy
                                history injury-id nil)))))))
