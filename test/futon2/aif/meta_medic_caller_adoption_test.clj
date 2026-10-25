(ns futon2.aif.meta-medic-caller-adoption-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-medic-admission-test :as fixture]
            [futon2.aif.meta-medic-authority :as authority]
            [futon2.aif.meta-medic-authority-test :as authority-fixture]
            [futon2.aif.meta-medic-recurrence-test :as recurrence-fixture]
            [futon2.aif.meta-repair-coordinator :as coordinator]
            [futon2.aif.meta-self-heal-production :as self-heal]))

(def fixture-root "target/medic-caller-adoption")

(defn path [label]
  (let [dir (io/file fixture-root (str label "-" (java.util.UUID/randomUUID)))]
    (.mkdirs dir)
    (str (io/file dir "ledger.edn"))))

(defn write-form! [p value] (spit p (str (pr-str value) "\n")))

(defn source-pin [p]
  {:path p :sha256 (load-identity/sha256 (.getBytes (slurp p) "UTF-8"))})

(def work-options
  {:diagnosis-evidence-pin {:path "fixtures/diagnosis.edn"
                            :sha256 (apply str (repeat 64 "7"))}
   :acceptance-test {:id "synthetic" :command ["true"] :expected {:exit 0}}
   :author "author" :reviewer "reviewer" :repair-reviewer "repair-reviewer"
   :author-session "author/session" :reviewer-session "reviewer/session"
   :repair-reviewer-session "repair-reviewer/session"
   :target-repository "futon2" :target-worktree "target/synthetic"})

(deftest authority-aware-runtime-boundaries-use-effective-occurrences
  (let [medic (:ledger (fixture/appended-medic))
        p (path "effective")
        _ (write-form! p medic)
        context (self-heal/diagnosis-context
                 {:meta-injury-ledger-path p
                  :meta-medic-authority-verifier fixture/synthetic-authority})
        prepared (coordinator/prepare (:durable-ledger context)
                                     (assoc work-options :injury-id "injury/b")
                                     fixture/synthetic-authority)
        persisted-path (path "persisted")
        persisted (store/persist-ledger! persisted-path medic
                                         fixture/synthetic-authority)]
    (is (= :loaded (get-in context [:durable-ledger :status])))
    (is (= #{"injury/b"} (set (keys (get-in context [:active-injuries :injuries])))))
    (is (= "injury/b" (get-in prepared [:work-order :injury-id])))
    (is (= :persisted (:status persisted)))
    (is (= :loaded (:status (store/load-durable-ledger
                             persisted-path fixture/synthetic-authority))))
    (is (= (get-in context [:durable-ledger :source-pin]) (source-pin p)))))

(deftest terminal-ingestion-and-fresh-recurrence-retain-authority
  (let [medic (:ledger (fixture/appended-medic))
        p (path "ingestion")
        record-path (str p ".record.edn")
        _ (write-form! p medic)
        _ (write-form! record-path
                       {:run/id "new-run" :click/id "new-click"
                        :failure {:kind :new-independent-failure
                                  :stage :construction
                                  :detail {:target "caller-adoption"}}})
        ingested (store/ingest-record-file! p record-path fixture/synthetic-authority)
        loaded (store/load-durable-ledger p fixture/synthetic-authority)
        recurrence (ledger/append-transition
                    medic {:injury-id "injury/a" :state :claimed :attempt 1
                           :repair-id "repair/a" :algorithm "synthetic"
                           :author-session "author/new" :reviewer-session "reviewer/new"}
                    fixture/synthetic-authority)
        effective (ledger/effective-active-injuries
                   (:ledger recurrence) fixture/synthetic-authority)]
    (is (= :appended (:status ingested)) (pr-str ingested))
    (is (= :loaded (:status loaded)))
    (is (= :appended (:status recurrence)))
    (is (= #{"injury/a" "injury/b"}
           (set (keys (:injuries effective)))))))

(deftest stale-covered-transaction-is-not-selected-for-resume
  (let [base (fixture/base-ledger)
        base-path (path "base")
        _ (write-form! base-path base)
        durable {:schema :wm/durable-meta-injury-ledger-v1 :status :loaded
                 :source-pin (source-pin base-path) :ledger base}
        prepared (coordinator/prepare durable
                                     (assoc work-options :injury-id "injury/a"))
        tx-path (str base-path ".transaction.edn")
        _ (write-form! tx-path (:transaction prepared))
        medic (:ledger (fixture/appended-medic))
        active (ledger/effective-active-injuries medic fixture/synthetic-authority)
        selection (self-heal/injury-selection tx-path
                                             (assoc durable :ledger medic) active)]
    (is (= :prepared (:status prepared)))
    (is (= :fresh (:mode selection)))
    (is (= "injury/b" (:injury-id selection)))
    (is (= "injury/a" (get-in selection [:stale-transaction :injury-id])))))

(deftest malformed-authority-refuses-before-any-runtime-selection
  (let [ordinary (atom 0)
        diagnosis (atom 0)
        result (with-redefs [self-heal/diagnosis-context
                             (fn [_] (swap! diagnosis inc) nil)]
                 (runtime/run-opportunity!
                  {:meta-medic-authority-context {:artifacts {} :intent {}}
                   :ordinary-selection-fn #(swap! ordinary inc)}))]
    (is (= :refused (:status result)))
    (is (= :meta-self-heal/medic-authority-unavailable (:reason result)))
    (is (false? (:ordinary-selection-called? result)))
    (is (zero? @diagnosis))
    (is (zero? @ordinary))))

(deftest malformed-authority-with-retry-refuses-before-readiness
  (let [calls (atom [])
        result (with-redefs [self-heal/attributable-retry-readiness
                             (fn [& _] (swap! calls conj :readiness) {:status :ready})]
                 (runtime/run-opportunity!
                  {:meta-medic-authority-context nil
                   :meta-repair-retry {:injury-id "synthetic"}}))]
    (is (= :refused (:status result)))
    (is (= :meta-self-heal/medic-authority-unavailable (:reason result)))
    (is (empty? @calls)))
  (is (= :refused
         (:status (#'runtime/resolve-medic-authority
                   {:meta-medic-authority-context
                    {:trust {} :intent {} :artifacts {}}})))))

(deftest caller-altered-resolution-map-is-refused-not-downgraded
  (let [forged {:status :ready :identity {:forged true}
                :verifier (constantly true)}
        resolved (#'runtime/resolve-medic-authority
                  {:meta-medic-authority-resolution forged})]
    (is (= :refused (:status resolved)))
    (is (= :meta-self-heal/medic-authority-unavailable (:reason resolved)))))

(deftest retry-readiness-requires-current-effective-enacted-occurrence
  (let [history (recurrence-fixture/enacted-ledger)
        enacted (last (:entries history))
        p (path "retry-current")
        retry (assoc (select-keys enacted [:injury-id :repair-id :retry-click])
                     :patch-sha recurrence-fixture/patch-sha)
        _ (write-form! p history)
        current (self-heal/attributable-retry-readiness
                 {:meta-injury-ledger-path p
                  :meta-medic-authority-verifier recurrence-fixture/synthetic-authority}
                 retry)
        covered (:ledger (recurrence-fixture/cover-latest history "covered/retry"))
        _ (write-form! p covered)
        hidden (self-heal/attributable-retry-readiness
                {:meta-injury-ledger-path p
                 :meta-medic-authority-verifier recurrence-fixture/synthetic-authority}
                retry)
        reopened (ledger/append-retry-result
                  history {:injury-id (:injury-id enacted)
                           :repair-id (:repair-id enacted)
                           :patch-sha recurrence-fixture/patch-sha
                           :retry-click (:retry-click enacted)
                           :new-cause-pin {:path "fixtures/new-cause.edn"
                                           :sha256 (apply str (repeat 64 "6"))}}
                  recurrence-fixture/synthetic-authority)
        _ (write-form! p (:ledger reopened))
        superseded (self-heal/attributable-retry-readiness
                    {:meta-injury-ledger-path p
                     :meta-medic-authority-verifier recurrence-fixture/synthetic-authority}
                    retry)]
    (is (= :ready (:status current)))
    (is (= :meta-self-heal/retry-attribution-refused (:reason hidden)))
    (is (= :meta-self-heal/retry-attribution-refused (:reason superseded)))))

(defn sealed-port [counters phase payload-fn]
  (fn [request]
    (swap! counters update phase (fnil inc 0))
    (let [order (:work-order request)]
      (coordinator/seal-port-receipt
       {:phase phase :injury-id (:injury-id order) :repair-id (:repair-id order)
        :attempt (:attempt order) :predecessor-pin (:predecessor-pin request)
        :status :succeeded :payload (payload-fn request)}))))

(deftest exact-scope-context-reaches-real-runtime-and-coordinator
  (let [bundle (authority-fixture/fixture)
        verifier (authority/authority-verifier bundle)
        medic-result (ledger/append-medic-repaired
                      (:ledger bundle)
                      {:path "receipts/full-path-medic.edn"
                       :sha256 (apply str (repeat 64 "8"))}
                      (:operation bundle) verifier)
        unrelated-pin {:path "fixtures/unrelated.edn"
                       :sha256 (apply str (repeat 64 "9"))}
        unrelated (ledger/append-transition
                   (:ledger medic-result)
                   {:injury-id "injury/unrelated" :state :open :attempt 1
                    :cause-record-pin unrelated-pin :failure-stage :construction
                    :failure-kind :synthetic :detected-click "click/unrelated"}
                   verifier)
        p (path "full-context")
        tx-path (str p ".transaction.edn")
        _ (write-form! p (:ledger unrelated))
        counters (atom {})
        patch (apply str (repeat 40 "a"))
        ports {:author (sealed-port counters :author
                                   (fn [_] {:patch-sha patch :worktree "target/hermetic"}))
               :review (sealed-port counters :review
                                   (fn [_] {:patch-sha patch :reviewer-sha patch}))
               :enact (sealed-port counters :enact
                                  (fn [_] {:patch-sha patch :integration-commit patch
                                           :retry-click "retry/hermetic"}))
               :retry (sealed-port counters :retry
                                  (fn [_] {:repair-id "repair/placeholder"}))}
        ;; Retry payload needs the deterministic repair id prepared at runtime;
        ;; replace just that port while retaining sealed provenance.
        ports (assoc ports :retry
                     (fn [request]
                       (swap! counters update :retry (fnil inc 0))
                       (let [order (:work-order request)]
                         (coordinator/seal-port-receipt
                          {:phase :retry :injury-id (:injury-id order)
                           :repair-id (:repair-id order) :attempt (:attempt order)
                           :predecessor-pin (:predecessor-pin request)
                           :status :succeeded
                           :payload {:repair-id (:repair-id order) :patch-sha patch
                                     :retry-click "retry/hermetic"
                                     :outcome :grounded-change}}))))
        result (runtime/run-opportunity!
                (merge work-options
                       {:meta-medic-authority-context
                        (select-keys bundle [:intent :artifacts :evaluation-pin
                                             :review-pin :authorization-pin :trust])
                        :meta-injury-ledger-path p
                        :meta-repair-transaction-path tx-path
                        :meta-repair-ports ports
                        :meta-acceptance-run-fn (constantly {:status :ran :exit 0})}))]
    (is (= :appended (:status medic-result)))
    (is (= :appended (:status unrelated)))
    (is (= :discharged (:status result)) (pr-str result))
    (is (= "injury/unrelated" (:injury-id result)))
    (is (= {:author 1 :review 1 :enact 1 :retry 1} @counters))
    (is (= :valid (:status (ledger/validate-ledger
                            (:ledger (store/load-durable-ledger p verifier))
                            verifier))))))

(deftest caller-supplied-derived-verifier-cannot-bypass-default-deny
  (let [medic (:ledger (fixture/appended-medic))
        p (path "spoof")
        _ (write-form! p medic)
        result (runtime/run-opportunity!
                {:meta-injury-ledger-path p
                 :meta-medic-authority-verifier fixture/synthetic-authority})]
    (is (= :refused (:status result)))
    (is (= :meta-self-heal/ledger-unreadable (:reason result)))
    (is (not (true? (:ordinary-selection-called? result))))))

(deftest post-rename-failure-needs-readback-and-identical-replay
  (let [p (path "indeterminate")
        initial (fixture/base-ledger)
        operation (fixture/operation initial)
        real-write @#'store/atomic-write!]
    (write-form! p initial)
    (let [result (with-redefs [store/atomic-write!
                               (fn [target value]
                                 (real-write target value)
                                 (throw (ex-info "after rename" {})))]
                   (store/transact-medic-disposition!
                    p fixture/medic-record-pin operation fixture/synthetic-authority))
          readback (store/load-durable-ledger p fixture/synthetic-authority)
          replay (store/transact-medic-disposition!
                  p fixture/medic-record-pin operation fixture/synthetic-authority)]
      (is (= :store/medic-transaction-failed (:reason result)))
      (is (= :loaded (:status readback)))
      (is (= :duplicate (:status replay)))
      (is (= 1 (count (filter #(= :medic-repaired (:observation-kind %))
                              (get-in readback [:ledger :entries]))))))))
