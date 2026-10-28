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
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.full-loop-runtime]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.test-support.runner-fixture :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.learning-trial-ledger :as learning-ledger]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.trace :as trace]
            [futon2.aif.wm.cascade-decision :as wm-cd]
            [futon2.aif.reading-interpretation-debt :as debt]
            [futon2.test-support.reading-route :as route])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false]
                              (route/with-empty-learning-ledger f))))

(def live (edn/read-string
           (slurp (io/resource "fixtures/gate-refusal/fifth-flight-failure-data.edn"))))

(def ^:private critical-source-namespaces
  ['futon2.aif.full-loop-runtime
   'futon2.report.war-machine
   'futon2.aif.wm.family-selection
   'futon2.aif.meta-pipeline-selector])

(deftest critical-source-registration-is-exact-and-substitution-refuses
  ;; The runner-source gate was added after this refusal fixture. Loading the
  ;; production composition root above registers the exact sources the test's
  ;; real run consumes; no test registration is synthesized.
  (doseq [n critical-source-namespaces]
    (let [entry (get @load-identity/registry n)
          canonical (get load-identity/required-sources n)]
      (is (= :captured (:status entry)) (str n " is registered by its producer"))
      (is (= (load-identity/sha256 (load-identity/read-bytes canonical))
             (:sha256 entry))
          (str n " is bound to the canonical bytes"))))
  (let [poisoned 'futon2.aif.meta-pipeline-selector
        registry (assoc-in @load-identity/registry [poisoned :sha256]
                           (load-identity/sha256 (.getBytes "substituted" "UTF-8")))
        failure (try
                  (with-redefs [load-identity/registry (atom registry)]
                    (#'runner/refuse-on-runner-source-drift!))
                  nil
                  (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (= :stale-outer-selection-composition (:failure-kind failure)))
    (is (= [poisoned] (keys (:critical-namespaces failure))))
    (is (= :stale (get-in failure [:critical-namespaces poisoned :status])))))

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
  ;; Migrated 2026-10-15 (Q5 route ledger). The judge-fn drives the REAL
  ;; reading-family decision, whose one gate refuses: every policy carries
  ;; an interpretation-debt carrier that no longer checks, so decision-gate
  ;; emit! throws {:error :inadmissible-decision :reason
  ;; :reading-policy-debt-invalid}. (Retired: the interpreted route's
  ;; missing-observation-locators refusal; reading policies have no
  ;; interpreted guard locators.)
  (let [fx (route/fixture)
        unreadable (route/decide-field (route/field {"M-a" nil}) {"M-a" 1} {})
        selected (route/decide fx)
        debt debt/debt
        broken (fn [action fit pin] (assoc (debt action fit pin) :owed [{:occurrence-id "x" :pattern "p/x"}]))
        {:keys [result record]}
        (run-judge (fn [_] (with-redefs [debt/debt broken]
                             (wm-cd/cascade-decision (route/assembled fx 1) (route/opts fx {}))))
                   {:target "M-autoclock-in"})
        [t] (get-in record [:decision :abstention :targets])]
    (is (= :no-acting-cascade-candidate (get-in unreadable [:decision :reason]))
        "missing earlier evidence remains generic no-selection, not a gate refusal")
    (is (some? (get-in selected [:decision :action]))
        "the same evidence with intact debt carriers selects an action")
    (is (= :reading-policy-debt-invalid (:kind t)))
    (is (= :abstained (get-in record [:decision :abstention :status])))
    (is (= :reading-policy-debt-invalid
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
