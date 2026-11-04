(ns futon2.aif.out-of-band-fix-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.medic-disposition :as medic]
            [futon2.aif.out-of-band-fix :as fix]
            [futon2.aif.meta-self-heal-production :as production]))

(deftest checked-three-evidence-rule
  (let [green {:tests 1 :failures 0 :errors 0}
        red {:tests 1 :failures 7 :errors 0}]
    (is (fix/qualifies? true green red))
    (is (not (fix/qualifies? false green red)))
    (is (not (fix/qualifies? true green green)))
    (is (not (fix/qualifies? true green (assoc red :errors 1))))
    (is (not (fix/qualifies? true (assoc green :tests 0) red)))))

(deftest absent-artifact-preserves-ordinary-replay
  (let [root (str (java.nio.file.Files/createTempDirectory
                   "oob-empty" (make-array java.nio.file.attribute.FileAttribute 0)))
        calls (atom 0)
        run (fn [_] (swap! calls inc))
        request {:phase :retry :work-order {:injury-id "injury/absent"
                                          :repair-id "repair/absent" :attempt 1}}]
    (is (nil? (fix/compute! root (:work-order request) run)))
    (is (= ((production/ordinary-acceptance-replay-port run root) request)
           ((production/acceptance-replay-port run root) request)))
    (is (zero? @calls))))

(deftest artifact-selection-is-scoped-to-the-current-occurrence
  (let [root (str (java.nio.file.Files/createTempDirectory
                   "oob-reopened" (make-array java.nio.file.attribute.FileAttribute 0)))
        injury "injury/reopened"
        first-pin {:path "/records/first.edn" :sha256 "first"}
        second-pin {:path "/records/second.edn" :sha256 "second"}
        first-artifact {:injury-id injury :historical-pin first-pin :occurrence 1}
        second-artifact {:injury-id injury :historical-pin second-pin :occurrence 2}]
    (fix/retain! root "out-of-band" first-artifact)
    (fix/retain! root "out-of-band" second-artifact)
    (is (= second-artifact
           (:artifact (fix/artifact-for root injury second-pin))))
    (is (= first-artifact
           (:artifact (fix/artifact-for root injury first-pin))))))

(deftest duplicate-artifacts-for-one-occurrence-remain-ambiguous
  (let [root (str (java.nio.file.Files/createTempDirectory
                   "oob-ambiguous" (make-array java.nio.file.attribute.FileAttribute 0)))
        injury "injury/ambiguous"
        cause-pin {:path "/records/current.edn" :sha256 "current"}]
    (fix/retain! root "out-of-band"
                 {:injury-id injury :historical-pin cause-pin :artifact 1})
    (fix/retain! root "out-of-band"
                 {:injury-id injury :historical-pin cause-pin :artifact 2})
    (try
      (fix/artifact-for root injury cause-pin)
      (is false "two artifacts for the same occurrence must refuse")
      (catch clojure.lang.ExceptionInfo e
        (is (= :out-of-band/ambiguous-artifact (:reason (ex-data e))))))))

(deftest artifact-refusal-does-not-fall-through-to-ordinary-discharge
  (let [root (str (java.nio.file.Files/createTempDirectory
                   "oob-refusal" (make-array java.nio.file.attribute.FileAttribute 0)))
        calls (atom 0)
        refused {:status :refused :reason :out-of-band/counterfactual-not-causal}
        result (with-redefs [fix/compute! (fn [& _] refused)]
                 ((production/acceptance-replay-port (fn [_] (swap! calls inc)) root)
                  {:work-order {}}))]
    (is (= :refused (:status result)))
    (is (= refused (edn/read-string
                    (fix/read-pin (:out-of-band-receipt-pin result)))))
    (is (zero? @calls))))

;; L5-25b (click wm-click-21ae0429): a :reopened injury presents its retained
;; artifact too, and the medic payload binds the medic occurrence (entry id,
;; predecessor, inherited cause), not the raw latest entry.  Lean:
;; DarkTower.WarMachine.OutOfBandReopened.

(deftest an-occurrence-awaiting-a-fresh-attempt-presents-the-artifact
  (is (fix/eligible? {:state :open}))
  (is (fix/eligible? {:state :reopened}))
  (doseq [live [:claimed :authored :reviewed :enacted :discharged :abandoned]]
    (is (not (fix/eligible? {:state live})) (str live " must not be interrupted or revived"))))

(def entry-154
  ;; injury d984347f as the medic state reads it after the failed attempt
  {:injury-id "injury/d984347f3175b83feb1b65444431a05f6750a84a"
   :entry-id "154" :predecessor-entry-id "153"
   :cause-record-id "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-11-01-0e6ff636-1b8d-4fe0-9b65-cb8588223767.edn"
   :cause-pin "43ce01bc2b120b464f94e716888e34b455360ef03df023a3579bedf492e6459a"
   :active? true})

(deftest the-medic-payload-binds-the-reopened-occurrence
  (let [receipt {:status :checked :fixed-by "21c40aff1d8e25752663032b65d0fa8318f6db05"
                 :base-sha (apply str (repeat 40 "b"))
                 :historical-pin {:path (:cause-record-id entry-154) :sha256 (:cause-pin entry-154)}
                 :current {:run-pin {:sha256 (apply str (repeat 64 "1"))}}
                 :counterfactual {:run-pin {:sha256 (apply str (repeat 64 "2"))}}}
        ledger {:ledger-pin {:path "meta-injury-ledger/ledger.edn" :sha256 (apply str (repeat 64 "3"))}}
        artifact-pin {:path "a" :sha256 (apply str (repeat 64 "4"))}
        receipt-pin {:path "r" :sha256 (apply str (repeat 64 "5"))}
        {:keys [operation]} (fix/checked-medic-operation ledger entry-154 artifact-pin receipt-pin receipt)
        candidate (get-in operation [:request :candidate])]
    (is (= "154" (:entry-id candidate)))
    (is (= "153" (:predecessor-entry-id candidate)))
    (is (= (:cause-record-id entry-154) (:cause-record-id candidate)))
    (is (= (:cause-pin entry-154) (:cause-pin candidate)))
    (is (medic/payload-valid? candidate))
    (is (:qualified? (medic/qualify (constantly true) entry-154 candidate candidate)))
    ;; the deployed builder's shape: nil predecessor on a reopened occurrence
    (is (= {:qualified? false :reason :binding-mismatch}
           (medic/qualify (constantly true) entry-154
                          (assoc candidate :predecessor-entry-id nil)
                          (assoc candidate :predecessor-entry-id nil))))))
