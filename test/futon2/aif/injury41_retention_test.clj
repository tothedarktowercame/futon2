(ns futon2.aif.injury41-retention-test
  "Regression for injury 41a00c62: retain the bounded diagnostics produced by
  a real resume-protocol violation when the terminal run record is sealed.

  The historical fc464172 wrapper below is deliberately only the wrapper that
  was retained: it does not manufacture the checks, controls, or booleans
  that that old record omitted."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.wm.open-cascade-resume :as resume])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def ^:private cascade-digest (apply str (repeat 64 "a")))

(def ^:private retained-fc464172-wrapper
  {:outcome :incomplete
   :failure-kind :resume-protocol-violation
   :held-phase nil
   :repairable true
   :failure-detail {:status :error :kind :resume-protocol-violation}})

(defn- temp-dir []
  (.toFile (Files/createTempDirectory "injury41-retention-"
                                     (make-array FileAttribute 0))))

(defn- delete-tree! [dir]
  (doseq [f (reverse (file-seq dir))]
    (io/delete-file f true)))

(defn- protocol-violation []
  (let [ledger (resume/new-ledger "injury41-synthetic")
        clicks (atom 0)
        selections (atom 0)]
    (resume/resume!
     ledger
     {:cascade-digest cascade-digest
      :phase :enact
      :retry (fn [_]
               ;; This is the known invariant violation: the held retry
               ;; secretly selected again, while keeping its cascade digest.
               (swap! selections inc)
               {:cascade-digest cascade-digest :outcome :retried})}
     {:click-budget clicks :selection-counter selections})))

(defn- sealed-record [dir]
  (let [violation (protocol-violation)
        failure (#'runner/no-entry-failure-data
                 {:decision {:status :selected}} violation)
        result {:outcome (:outcome failure)
                :data {:failure-kind (:failure-kind failure)
                       :failure-stage :cascade-resume
                       :error "War Machine abstained or selected no addressable action"
                       ;; This is the close boundary's retained wrapper:
                       ;; outer failure classification plus the downstream
                       ;; resume result under :failure-detail.
                       :error-data failure}}
        written (runner/persist-run-record!
                 {:run-record-dir (.getPath dir)
                  :observation-labels-path (str (io/file dir "labels.edn"))
                  :scan-render-fn (fn [& _] nil)
                  :nano-time-fn (constantly 0)}
                 "injury41-synthetic" "2026-10-23T00:00:00Z" result)]
    {:violation violation
     :record (edn/read-string (slurp (:run-record written)))}))

(deftest resume-protocol-diagnostics-survive-the-sealed-terminal-record
  (let [dir (temp-dir)]
    (try
      (let [{:keys [violation record]} (sealed-record dir)
            retained (get-in record [:failure :detail :failure-detail])]
        (testing "the real resume! invariant supplies false values and controls"
          (is (= :resume-protocol-violation (:kind violation)))
          (is (= false (get-in violation [:checks :selection-count-unchanged?])))
          (is (= true (get-in violation [:checks :click-count-unchanged?])))
          (is (= {:click-budget-present? true
                  :selection-counter-present? true}
                 (:controls violation))))
        (testing "the actual terminal writer retains bounded nested evidence"
          (is (= (:checks violation) (:checks retained)))
          (is (= (:controls violation) (:controls retained)))
          (is (= (:repair violation) (:repair retained)))
          (is (= :error (:status retained)))
          (is (= :resume-protocol-violation (:kind retained)))))
      (finally (delete-tree! dir)))))

(deftest historical-wrapper-remains-honestly-absent-and-generic-nested-errors-stay-shallow
  (testing "the retained fc464172 wrapper does not invent its lost diagnostics"
    (let [detail (#'runner/run-record-failure
                  {:data {:failure-kind :resume-protocol-violation
                          :failure-stage :cascade-resume
                          :error "War Machine abstained or selected no addressable action"
                          :error-data retained-fc464172-wrapper}})]
      (is (= {:status :error :kind :resume-protocol-violation}
             (get-in detail [:detail :failure-detail])))
      (is (not (contains? (get-in detail [:detail :failure-detail]) :checks)))
      (is (not (contains? (get-in detail [:detail :failure-detail]) :controls)))
      (is (not (contains? (get-in detail [:detail :failure-detail]) :repair)))))
  (testing "unrelated nested maps keep the existing scalar-only retention"
    (is (= {:status :error :kind :other}
           (get-in (#'runner/run-record-failure
                    {:data {:failure-kind :other
                            :failure-stage :cascade-resume
                            :error-data {:failure-detail {:status :error
                                                            :kind :other
                                                            :checks {:would :omit}}}}})
                   [:detail :failure-detail])))))
