(ns futon2.aif.repaired-elsewhere-medic-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.repaired-elsewhere-medic :as medic]))

(def injury-id "injury/repaired-elsewhere-fixture")
(def finding-id "repair-repaired-elsewhere-fixture")
(def repair-commit "a0e3de410")

(defn- write-pinned! [path value]
  (io/make-parents path)
  (let [text (str (pr-str value) "\n")]
    (spit path text)
    {:path (.getCanonicalPath (io/file path))
     :sha256 (identity/sha256 (.getBytes text "UTF-8"))}))

(defn- fixture []
  (let [root (io/file "target" "repaired-elsewhere-medic"
                      (str (java.util.UUID/randomUUID)))
        ledger-path (str (io/file root "ledger.edn"))
        acceptance-root (str (io/file root "acceptances"))
        repair-root (str (io/file root "repairs"))
        cause-pin (write-pinned! (io/file root "cause.edn")
                                 {:startedAt "2020-01-01T00:00:00Z"
                                  :failure {:kind :mission-substrate-freshness-unestablished}})
        opened (ledger/append-transition
                ledger/empty-ledger
                {:injury-id injury-id :state :open :attempt 1
                 :cause-id "cause/repaired-elsewhere" :cause-record-pin cause-pin
                 :failure-stage :preference-refresh
                 :failure-kind :decision-input-freshness-unestablished
                 :detected-click "click/repaired-elsewhere"})
        history (:ledger opened)
        entry (:entry opened)
        _ (spit ledger-path (str (pr-str history) "\n"))
        finding {:repair/id finding-id :repair/schema-version 3 :repair/status :open
                 :opened-at "2020-01-01T00:00:01Z" :attempt-id "attempt-fixture"
                 :machine-repo "/home/joe/code/futon2"
                 :failure-kind :decision-input-freshness-unestablished
                 :failure-stage :preference-refresh
                 :failure-data {:kind :mission-substrate-freshness-unestablished}}
        _ (write-pinned! (io/file repair-root "findings" (str finding-id ".edn")) finding)
        rerun-pin (medic/retain-rerun! acceptance-root
                                        {:status :established :scanned 347})]
    {:root root :ledger-path ledger-path :acceptance-root acceptance-root
     :repair-root repair-root :entry entry :rerun-pin rerun-pin}))

(defn- request [f]
  {:ledger-path (:ledger-path f)
   :acceptance-root (:acceptance-root f)
   :repair-root (:repair-root f)
   :injury-id injury-id :expected-entry-pin (get-in f [:entry :entry-pin])
   :repository "/home/joe/code/futon2" :commits [repair-commit]
   :rerun-pin (:rerun-pin f)
   :healthy-condition {:op :path-equals :path [:status] :expected :established}
   :finding-id finding-id :actor "test/medic" :reason :repaired-elsewhere
   :normal-authority (constantly false)})

(deftest repaired-elsewhere-medic-masks-exact-open-occurrence-and-dismisses-finding
  (let [f (fixture)
        result (medic/close! (request f))
        persisted (edn/read-string (slurp (:ledger-path f)))
        observation (last (:entries persisted))
        dismissal-path (io/file (:repair-root f) "dismissals" (str finding-id ".edn"))]
    (is (= :discharged (:status result)) (pr-str result))
    (is (= 2 (:ordinal observation)))
    (is (= :medic-repaired (:observation-kind observation)))
    (is (= :open (get-in persisted [:entries 0 :state])))
    (is (empty? (:injuries (ledger/effective-active-injuries
                            persisted medic/retained-authority))))
    (is (.isFile dismissal-path))
    (is (= :dismissed-repaired-elsewhere
           (:repair/status (edn/read-string (slurp dismissal-path)))))))

(deftest repaired-elsewhere-medic-refuses-unbound-or-unhealthy-evidence
  (testing "stale occurrence pin"
    (let [f (fixture)
          result (medic/close! (assoc (request f) :expected-entry-pin
                                      {:path "stale" :sha256 (apply str (repeat 64 "0"))}))]
      (is (= :repaired-elsewhere/stale-occurrence (:reason result)))
      (is (= 1 (count (:entries (edn/read-string (slurp (:ledger-path f)))))))))
  (testing "non-ancestor commit"
    (let [f (fixture)
          result (medic/close! (assoc (request f) :commits [(apply str (repeat 40 "0"))]))]
      (is (= :repaired-elsewhere/commit-not-ancestor (:reason result)))
      (is (= 1 (count (:entries (edn/read-string (slurp (:ledger-path f)))))))))
  (testing "rerun does not establish condition"
    (let [f (fixture)
          bad-pin (medic/retain-rerun! (:acceptance-root f) {:status :stale})
          result (medic/close! (assoc (request f) :rerun-pin bad-pin))]
      (is (= :repaired-elsewhere/condition-not-established (:reason result)))
      (is (= 1 (count (:entries (edn/read-string (slurp (:ledger-path f))))))))))
