(ns futon2.aif.meta-medic-store-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-terminal-injury :as terminal]))

(def fixture-root "target/medic-store-fixtures")
(def opening-pin {:path "fixtures/store-opening.edn"
                  :sha256 (apply str (repeat 64 "1"))})
(def medic-record-pin {:path "fixtures/store-medic.edn"
                       :sha256 (apply str (repeat 64 "2"))})
(def authority-pin {:path "fixtures/store-authority.edn"
                    :sha256 (apply str (repeat 64 "3"))})

(defn synthetic-authority [{:keys [operation candidate evidence current]}]
  (and (= authority-pin (:authority-receipt-pin operation))
       (= candidate evidence)
       (= [(:injury-id candidate) (:entry-id candidate)]
          [(:injury-id current) (:entry-id current)])))

(defn fixture-path [label]
  (let [dir (io/file fixture-root (str label "-" (java.util.UUID/randomUUID)))]
    (.mkdirs dir)
    (str (io/file dir "ledger.edn"))))

(defn open-ledger []
  (:ledger
   (terminal/integrate
    ledger/empty-ledger
    (terminal/classify
     {:run/id "store-open" :click/id "store-detect"
      :failure {:kind :store-fixture :stage :construction
                :detail {:target "medic-store"}}}
     opening-pin))))

(defn payload [entry]
  {:kind :historical-misclassification
   :injury-id (:injury-id entry)
   :entry-id (str (:ordinal entry))
   :predecessor-entry-id nil
   :patch-id "patch/store"
   :reviewer "reviewer/store"
   :evaluator "evaluator/store"
   :evidence-pin "evidence/store"
   :original-record-id (:path opening-pin)
   :original-pin (:sha256 opening-pin)
   :classifier-revision "classifier/store"
   :replay :producer-qualified-non-injury})

(defn operation [history operation-id]
  (let [entry (last (filter #(= ledger/transition-schema (:schema %))
                            (:entries history)))
        p (payload entry)]
    {:operation-id operation-id
     :expected-ledger-pin (:ledger-pin history)
     :request {:candidate p :evidence p}
     :authority-receipt-pin authority-pin}))

(defn write-ledger! [path value]
  (spit path (str (pr-str value) "\n")))

(deftest admitted-append-readback-and-exact-replay-are-durable
  (let [path (fixture-path "append-replay")
        initial (open-ledger)
        op (operation initial "medic/store-append")]
    (write-ledger! path initial)
    (let [result (store/transact-medic-disposition!
                  path medic-record-pin op synthetic-authority)
          bytes (slurp path)
          replay (store/transact-medic-disposition!
                  path medic-record-pin op synthetic-authority)]
      (is (= :appended (:status result)) (pr-str result))
      (is (= :atomic-locked-one-form (:durability result)))
      (is (= :valid (:status (ledger/validate-ledger
                              (:ledger (store/load-ledger path synthetic-authority))
                              synthetic-authority))))
      (is (= :duplicate (:status replay)))
      (is (= bytes (slurp path))))))

(deftest stale-conflicting-and-unqualified-operations-never-rewrite
  (let [path (fixture-path "refusals")
        initial (open-ledger)
        first-op (operation initial "medic/store-first")]
    (write-ledger! path initial)
    (is (= :appended (:status (store/transact-medic-disposition!
                               path medic-record-pin first-op synthetic-authority))))
    (let [bytes (slurp path)
          stale (assoc first-op :operation-id "medic/store-stale")
          stale-result (store/transact-medic-disposition!
                        path medic-record-pin stale synthetic-authority)
          current (:ledger (store/load-ledger path synthetic-authority))
          conflict-op (operation current "medic/store-conflict")
          conflict-result (store/transact-medic-disposition!
                           path medic-record-pin conflict-op synthetic-authority)
          denied-op (operation current "medic/store-denied")
          denied-result (store/transact-medic-disposition!
                         path medic-record-pin denied-op (constantly false))]
      (is (= :medic/stale-root (:reason stale-result)))
      (is (= bytes (slurp path)))
      (is (= :conflict (:status conflict-result)) (pr-str conflict-result))
      (is (= bytes (slurp path)))
      (is (= :store/tampered-ledger (:reason denied-result)))
      (is (= bytes (slurp path))))))

(deftest missing-malformed-and-failed-replace-fail-closed
  (let [missing (fixture-path "missing")
        _ (.delete (io/file missing))
        malformed (fixture-path "malformed")
        initial (open-ledger)]
    (spit malformed "{:not-edn")
    (is (= :store/durable-ledger-required
           (:reason (store/transact-medic-disposition!
                     missing medic-record-pin {} synthetic-authority))))
    (is (= :store/malformed-edn
           (:reason (store/transact-medic-disposition!
                     malformed medic-record-pin {} synthetic-authority))))
    (let [path (fixture-path "replace-failure")
          op (operation initial "medic/store-write-failure")]
      (write-ledger! path initial)
      (let [before (slurp path)
            result (with-redefs-fn
                     {#'store/atomic-write!
                      (fn [& _] (throw (ex-info "injected" {})))}
                     #(store/transact-medic-disposition!
                       path medic-record-pin op synthetic-authority))]
        (is (= :store/medic-transaction-failed (:reason result)))
        (is (= before (slurp path)))
        (is (= initial (edn/read-string (slurp path))))))))

(deftest same-root-competitors-cannot-both-append-through-lock-path
  (let [path (fixture-path "competitors")
        initial (open-ledger)
        op-a (operation initial "medic/store-a")
        op-b (operation initial "medic/store-b")]
    (write-ledger! path initial)
    (let [a (store/transact-medic-disposition!
             path medic-record-pin op-a synthetic-authority)
          bytes (slurp path)
          b (store/transact-medic-disposition!
             path medic-record-pin op-b synthetic-authority)]
      (is (= :appended (:status a)))
      (is (= :medic/stale-root (:reason b)))
      (is (= bytes (slurp path)))
      (is (= 1 (count (filter #(= :medic-repaired (:observation-kind %))
                              (:entries (:ledger (store/load-ledger
                                                 path synthetic-authority))))))))))
