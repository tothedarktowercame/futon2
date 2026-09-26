(ns futon2.aif.gate-refusal-abstention-test
  "WM-GATE-REFUSAL-I: the decision gate's refusal (decision_gate.clj refuse!,
  ex-data {:error :inadmissible-decision :reason r :detail d}) is the tick's
  typed abstention on the run record and on the flight's click entry, as a
  judge refusal is, never :untyped-failure. The fifth flight
  (flight-7f89646a) closed its click :untyped-failure on
  :missing-observation-locators; live pin: that click's repair finding's
  :failure-data (fixture header: path and sha)."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false] (f))))

(def live (edn/read-string (slurp "test/fixtures/gate-refusal/fifth-flight-failure-data.edn")))

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
