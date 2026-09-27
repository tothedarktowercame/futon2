(ns futon2.aif.gate-refusal-abstention-test
  "WM-GATE-REFUSAL-I: the decision gate's refusal (decision_gate.clj refuse!,
  ex-data {:error :inadmissible-decision :reason r :detail d}) is the tick's
  typed abstention on the run record and on the flight's click entry, as a
  judge refusal is, never :untyped-failure. The fifth flight
  (flight-7f89646a) closed its click :untyped-failure on
  :missing-observation-locators; live pin: that click's repair finding's
  :failure-data (fixture header: path and sha)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.learning-trial-ledger :as learning-ledger]
            [futon2.aif.locator-fixtures :as loc]
            [futon2.aif.trace :as trace]
            [futon2.report.cascade-decision-test :as cfix]
            [futon2.report.war-machine :as wm])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false] (f))))

(def live (edn/read-string
           (slurp (io/resource "fixtures/gate-refusal/fifth-flight-failure-data.edn"))))

(defn- run [judge-throws & [flight]]
  (let [findings (atom [])
        result (runner/run-opportunity!
                (merge (fixture/isolated-runner-opts)
                       {:judge-fn (fn [_] (throw judge-throws))
                        :repair-system-record-fn (fn [m] (swap! findings conj m)
                                                   {:repair/id (str "repair-test-" (count @findings))
                                                    :repair/class (:repair-class m)})
                        :dispatch-fn (fn [& _] (throw (ex-info "Unexpected dispatch" {})))}
                       (when flight {:flight flight})))]
    {:result result :findings @findings
     :record (edn/read-string (slurp (:run-record result)))}))

(deftest the-fifth-flights-gate-refusal-replayed
  (let [{:keys [result findings record]}
        (run (ex-info "Inadmissible decision" (:failure-data live)) {:target "M-autoclock-in"})
        carrier (get-in record [:decision :abstention])
        [t] (:targets carrier)
        tokens (get-in live [:failure-data :detail :missing-tokens])]
    (is (= :untyped-failure (:failure-kind live)) "what the fifth flight recorded")
    (is (= 2 (count tokens)))
    (is (= :abstained (:status carrier)))
    (is (= :missing-observation-locators (get-in record [:decision :abstention :targets 0 :kind])))
    (is (= "M-autoclock-in" (:target t)))
    (is (= tokens (:missing t)))
    (is (= tokens (get-in t [:data :missing-tokens])) "the two token pairs are kept in :data")
    (is (= (get-in live [:failure-data :detail]) (:data t)))
    (is (= :abstained (get-in result [:data :failure-kind])))
    (is (= [:environmental-hold] (mapv :repair-class findings))
        "recorded as every abstained tick is, not as a machine failure")
    (is (= :missing-observation-locators
           (get-in result [:checkpoints :construction :sorry :judge-refusal-kind])))
    (is (= :missing-observation-locators
           (:kind (:abstention (fr/record-summary "M-autoclock-in" "click-1" record))))
        "the flight's click entry names the kind")))

(deftest a-gate-refusal-without-a-reason-closes-typed-absent
  ;; the bad case: no :reason is a typed absence under :kind, never
  ;; :untyped-failure and never a guessed kind
  (let [{:keys [result record]}
        (run (ex-info "Inadmissible decision" {:error :inadmissible-decision}) {:target "M-autoclock-in"})
        [t] (get-in record [:decision :abstention :targets])]
    (is (= :abstained (get-in record [:decision :abstention :status])))
    (is (= {:absent :no-reason-given} (:kind t)))
    (is (= {:absent :reason-names-no-missing-input} (:missing t)))
    (is (= :abstained (get-in result [:data :failure-kind])))
    (is (not= :untyped-failure (get-in result [:data :failure-kind])))))

(defn- temp-dir [prefix]
  (.getPath (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0)))))

(defn- run-judge
  "Like run, but the judge-fn itself is supplied: the decision writer under
  test is called inside the tick, not replayed from a supplied exception.
  Self-contained stores and record dirs, so the var may be called directly
  (futon3c's wire test does, wrapping the writer var)."
  [judge-fn & [flight]]
  (let [findings (atom [])]
    (with-redefs-fn {#'trace/default-trace-dir (temp-dir "wire-gate-trace-")
                     #'runner/default-run-record-dir (temp-dir "wire-gate-records-")
                     #'learning-ledger/default-root (temp-dir "wire-gate-learning-")}
      #(binding [runner/*wm-status-reporting?* false]
         (let [result (runner/run-opportunity!
                       (merge (fixture/isolated-runner-opts)
                              {:judge-fn judge-fn
                               :repair-system-record-fn (fn [m] (swap! findings conj m)
                                                          {:repair/id (str "repair-test-" (count @findings))
                                                           :repair/class (:repair-class m)})
                               :dispatch-fn (fn [& _] (throw (ex-info "Unexpected dispatch" {})))}
                              (when flight {:flight flight})))]
           {:result result :findings @findings
            :record (edn/read-string (slurp (:run-record result)))})))))

(deftest the-real-gate-refusal-is-the-ticks-typed-abstention
  ;; WIRE-23-C2 (PROOF-2a <2>3 lane C2): the judge-fn drives the REAL
  ;; writer, war-machine/cascade-decision, whose decision gate refuses: the
  ;; fixture family is admitted with one guard token's locator present but
  ;; invalid for its class (C4 without :decl passes assembly's admission
  ;; and fails decision-gate/emit!'s guard-locator check), so
  ;; cascade-decision-admitted throws ex-info "Inadmissible decision"
  ;; {:error :inadmissible-decision :reason :missing-observation-locators}.
  (let [sources (assoc-in (loc/locate-all cfix/tick-1-sources)
                          [:locators cfix/tick-1-target :summary-without-total-repos-throws]
                          {:class :C4 :repo "futon2" :sha "fixture" :path "fixture/p"})
        assembled (cp/assemble {:targets [cfix/tick-1-target] :sources sources})
        {:keys [result record]}
        (run-judge (fn [_] (wm/cascade-decision assembled cfix/live-c-opts))
                   {:target "M-autoclock-in"})
        [t] (get-in record [:decision :abstention :targets])]
    (is (= :missing-observation-locators (:kind t)))
    (is (= :abstained (get-in record [:decision :abstention :status])))
    (is (= :missing-observation-locators
           (get-in result [:checkpoints :construction :sorry :judge-refusal-kind])))))

(deftest an-unrelated-exception-stays-untyped
  ;; control: no :error, no :outcome, no :failure-kind
  (let [{:keys [result findings record]} (run (ex-info "boom" {:something :else}))]
    (is (= :untyped-failure (get-in result [:data :failure-kind])))
    (is (= [:machine-failure] (mapv :repair-class findings)))
    (is (= {:status :absent :reason :no-selection-decision-recorded}
           (get-in record [:decision :abstention])))))

(deftest only-the-gates-error-is-read-as-a-gate-refusal
  (is (nil? (runner/gate-refusal (ex-info "x" {:error :something-else :reason :r}) "M")))
  (is (nil? (runner/gate-refusal (RuntimeException. "x") "M")))
  (is (= {:absent :refusal-names-no-target}
         (:target (runner/gate-refusal (ex-info "x" {:error :inadmissible-decision
                                                     :reason :flat-action
                                                     :detail {:action-type :t}})
                                       nil)))))
