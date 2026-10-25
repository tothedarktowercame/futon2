(ns futon2.aif.meta-medic-caller-adoption-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-medic-admission-test :as fixture]
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
