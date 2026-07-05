(ns futon2.aif.mana-gate-test
  "Tests for M-peradam-mechanization P2 mana gate.
   Covers: award→consume→balance round-trip; refusal at zero;
   the spend-without-log impossibility (the ⅋ coupling rule)."
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [futon2.aif.mana-gate :as mg]))

(def ^:dynamic *test-dir* nil)

(use-fixtures :each
  (fn [t]
    (let [d (io/file (str "/tmp/mana-gate-test-" (System/nanoTime)))]
      (.mkdirs d)
      (binding [*test-dir* (str d)]
        (try
          (t)
          (finally
            ;; cleanup
            (run! #(.delete %) (file-seq d))))))))

(defn dir [] *test-dir*)

(deftest award-consume-balance-roundtrip
  (testing "award then consume then balance"
    (let [gate "fold-authoring"]
      ;; Award 3
      (let [r1 (mg/award! gate 3 "top-up-for-testing" (dir))]
        (is (:ok r1))
        (is (= 3 (:balance r1))))
      ;; Balance reflects award
      (is (= 3 (mg/balance gate (dir))))
      ;; Consume 1
      (let [r2 (mg/consume! gate "fold-dispatch-001" (dir))]
        (is (:ok r2))
        (is (= 2 (:balance r2)))
        (is (not (:refused r2))))
      ;; Consume another
      (let [r3 (mg/consume! gate "fold-dispatch-002" (dir))]
        (is (:ok r3))
        (is (= 1 (:balance r3))))
      ;; Balance is 1
      (is (= 1 (mg/balance gate (dir)))))))

(deftest refusal-at-zero
  (testing "consume refuses at zero balance loudly"
    (let [gate "empty-gate"]
      ;; No award — balance is 0
      (is (= 0 (mg/balance gate (dir))))
      ;; Consume must refuse
      (let [r (mg/consume! gate "should-be-refused" (dir))]
        (is (not (:ok r)))
        (is (:refused r))
        (is (= "zero balance" (:reason r)))
        (is (zero? (:balance r))))
      ;; Ledger shows no spend events (the refusal was NOT logged)
      (let [events (mg/ledger gate (dir))]
        (is (empty? events) "refused consume must not create a ledger entry")))))

(deftest consume-to-zero-then-refuse
  (testing "consume down to zero, then next consume refuses"
    (let [gate "depletable"]
      (mg/award! gate 2 "top-up" (dir))
      (is (:ok (mg/consume! gate "spend-1" (dir))))
      (is (:ok (mg/consume! gate "spend-2" (dir))))
      (is (zero? (mg/balance gate (dir))))
      ;; Third consume must refuse
      (let [r (mg/consume! gate "spend-3-refused" (dir))]
        (is (not (:ok r)))
        (is (:refused r))))))

(deftest spend-without-log-impossibility
  "The ⅋ coupling rule: a spend is valid ONLY WITH its durable log entry.
   Accepted-but-not-stored is authorization forgery by accident.
   Test: after a successful consume, the ledger on disk MUST contain the
   spend entry. We simulate a 'crash' by reading the file directly (not
   through the API) and checking the event is there."
  (testing "successful consume always has a matching ledger entry on disk"
    (let [gate "coupling-test"
          _ (mg/award! gate 1 "top-up" (dir))
          result (mg/consume! gate "coupling-purpose" (dir))]
      (is (:ok result) "consume must succeed (balance was 1)")
      ;; Read the ledger file DIRECTLY from disk — not through the API
      (let [ledger-file (io/file (dir) (str gate ".edn"))
            raw-content (slurp ledger-file)
            events (edn/read-string raw-content)]
        ;; The spend entry IS on disk — the coupling rule holds
        (is (some #(and (= :spend (:type %))
                        (= "coupling-purpose" (:purpose %)))
                  events)
            "the spend entry must be durably logged on disk"))
      ;; The balance computed from the raw file matches the API return
      (let [disk-balance (mg/balance gate (dir))]
        (is (= 0 disk-balance)
            "balance must be 0 after award(1) + spend(1)")))))

(deftest refused-spend-leaves-no-trace
  "The inverse coupling: a refused consume must NOT create a ledger entry.
   No authorization without a durable record."
  (testing "refused consume does not write to the ledger"
    (let [gate "refused-coupling"
          _ (mg/award! gate 1 "top-up" (dir))
          _ (mg/consume! gate "real-spend" (dir))
          ;; Now balance is 0
          refused (mg/consume! gate "refused-spend" (dir))]
      (is (not (:ok refused)))
      ;; The ledger must contain exactly 1 award + 1 spend, not 2 spends
      (let [events (mg/ledger gate (dir))
            spends (filter #(= :spend (:type %)) events)]
        (is (= 1 (count spends)) "exactly one spend — the refused one is absent")
        (is (= "real-spend" (:purpose (first spends))))))))

(deftest multiple-awards-accumulate
  (testing "multiple awards accumulate balance"
    (let [gate "accumulating"]
      (mg/award! gate 2 "first" (dir))
      (mg/award! gate 3 "second" (dir))
      (is (= 5 (mg/balance gate (dir))))
      (mg/consume! gate "spend" (dir))
      (is (= 4 (mg/balance gate (dir)))))))

(deftest award-validation
  (testing "award rejects invalid inputs"
    (is (thrown? Exception (mg/award! "g" 0 "word" (dir))) "zero award")
    (is (thrown? Exception (mg/award! "g" -1 "word" (dir))) "negative award")
    (is (thrown? Exception (mg/award! "g" 1.5 "word" (dir))) "non-integer award")
    (is (thrown? Exception (mg/award! "g" 1 "" (dir))) "blank operator word")))

(deftest fresh-gate-has-zero-balance
  (testing "a gate with no ledger has zero balance"
    (is (zero? (mg/balance "never-existed" (dir))))))
