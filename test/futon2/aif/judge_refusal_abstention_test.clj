(ns futon2.aif.judge-refusal-abstention-test
  "WM-CLICK-REFUSAL-I: a typed refusal of the cascade decision (the judge's
  ex-info \"cascade decision refused\" with :kind) is the tick's typed
  abstention on the run record and on the flight's click entry, never
  :untyped-failure. The fourth flight (flight-e70b4baf) closed its click
  :untyped-failure on :class-unknown-no-scalar-g; live pin: that click's
  repair finding's :failure-data (fixture header: path and sha)."
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
           (slurp (io/resource "fixtures/judge-refusal/fourth-flight-failure-data.edn"))))

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

(deftest the-family-refusal-is-the-ticks-typed-abstention
  ;; the bad case: before the fix this closed :untyped-failure with a
  ;; machine-failure repair obligation and an absent abstention
  (let [{:keys [result findings record]}
        (run (ex-info "cascade decision refused" {:kind :incommensurable-family :horizon-steps [4 6]})
             {:target "M-autoclock-in"})
        carrier (get-in record [:decision :abstention])]
    (is (= :abstained (:status carrier)))
    (is (= [{:target "M-autoclock-in" :kind :incommensurable-family :missing :common-horizon
             :data {:kind :incommensurable-family :horizon-steps [4 6]} :declines []}]
           (:targets carrier)))
    (is (not= :untyped-failure (get-in result [:data :failure-kind])))
    (is (= :abstained (get-in result [:data :failure-kind])))
    (is (= [:environmental-hold] (mapv :repair-class findings))
        "the close records it as every abstained tick is recorded, not as a machine failure")
    (is (= :incommensurable-family (get-in result [:checkpoints :selection :sorry :judge-refusal :kind])))
    (is (= :incommensurable-family (get-in result [:checkpoints :construction :sorry :judge-refusal-kind])))
    (is (= {:target "M-autoclock-in" :kind :incommensurable-family :missing :common-horizon :declines []}
           (:abstention (fr/record-summary "M-autoclock-in" "click-1" record)))
        "the flight's click entry names the kind")))

(deftest the-fourth-flights-refusal-replayed
  (let [{:keys [result record]}
        (run (ex-info "cascade decision refused" (:failure-data live)) {:target "M-autoclock-in"})
        [t] (get-in record [:decision :abstention :targets])]
    (is (= :untyped-failure (:failure-kind live)) "what the fourth flight recorded")
    (is (= :class-unknown-no-scalar-g (:kind t)))
    (is (= "M-autoclock-in" (:target t)))
    (is (= :target-relation (:missing t)))
    (is (= (get-in live [:failure-data :possible-costs]) (get-in t [:data :possible-costs]))
        "the possible costs are kept")
    (is (not= :untyped-failure (get-in result [:data :failure-kind])))
    (is (= :class-unknown-no-scalar-g
           (:kind (:abstention (fr/record-summary "M-autoclock-in" "click-1" record)))))))

(defn- temp-dir [prefix]
  (.getPath (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0)))))

(defn- run-judge
  "Like run, but the judge-fn itself is supplied: the decision writer under
  test is called inside the tick, not replayed from a supplied exception.
  Self-contained stores and record dirs, so the var may be called directly
  (futon3c's wire test does, wrapping the writer var)."
  [judge-fn & [flight]]
  (let [findings (atom [])]
    (with-redefs-fn {#'trace/default-trace-dir (temp-dir "wire-judge-trace-")
                     #'runner/default-run-record-dir (temp-dir "wire-judge-records-")
                     #'learning-ledger/default-root (temp-dir "wire-judge-learning-")}
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

(deftest the-real-judge-refusal-is-the-ticks-typed-abstention
  ;; WIRE-23-C2 (PROOF-2a <2>3 lane C2): the judge-fn IS the real writer,
  ;; war-machine/cascade-decision, refusing: the injected live-C derivation
  ;; carries a refusal, so the :live-c guard in cascade-decision-admitted
  ;; (war_machine.clj) throws ex-info "cascade decision refused"
  ;; {:kind :live-c-refused} before any scoring.
  (let [assembled (cp/assemble {:targets [cfix/tick-1-target]
                                :sources (loc/locate-all cfix/tick-1-sources)})
        {:keys [result record]}
        (run-judge (fn [_]
                     (wm/cascade-decision
                      assembled
                      (assoc cfix/live-c-opts
                             :live-c {:derived {:refusals [{:kind :source-not-available}]}})))
                   {:target "M-autoclock-in"})
        carrier (get-in record [:decision :abstention])]
    (is (= :live-c-refused (get-in carrier [:targets 0 :kind])))
    (is (= :abstained (:status carrier)))
    (is (= :live-c-refused
           (get-in result [:checkpoints :selection :sorry :judge-refusal :kind])))))

(deftest an-untyped-judge-failure-stays-untyped
  (let [{:keys [result findings record]} (run (RuntimeException. "boom"))]
    (is (= :untyped-failure (get-in result [:data :failure-kind])))
    (is (= [:machine-failure] (mapv :repair-class findings)))
    (is (= {:status :absent :reason :no-selection-decision-recorded}
           (get-in record [:decision :abstention])))))

(deftest a-kind-without-the-judges-message-is-not-read-as-a-refusal
  (is (nil? (runner/judge-refusal (ex-info "something else" {:kind :x}) "M")))
  (is (nil? (runner/judge-refusal (ex-info "cascade decision refused" {:status :missing}) "M")))
  (is (= {:absent :refusal-names-no-target}
         (:target (runner/judge-refusal (ex-info "cascade decision refused" {:kind :live-c-stale}) nil)))))
