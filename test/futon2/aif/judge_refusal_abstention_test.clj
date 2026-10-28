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
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.full-loop-runtime]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.test-support.runner-fixture :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.learning-trial-ledger :as learning-ledger]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.trace :as trace]
            [futon2.aif.wm.cascade-decision :as wm-cd]
            [futon2.test-support.reading-route :as route])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false]
                              (route/with-empty-learning-ledger f))))

(def live (edn/read-string
           (slurp (io/resource "fixtures/judge-refusal/fourth-flight-failure-data.edn"))))

(def ^:private critical-source-namespaces
  ['futon2.aif.full-loop-runtime
   'futon2.report.war-machine
   'futon2.aif.wm.family-selection
   'futon2.aif.meta-pipeline-selector])

(deftest critical-source-registration-is-exact-and-substitution-refuses
  ;; Load the production composition root above so its producers register
  ;; their own canonical bytes; no test registration is synthesized.
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
  ;; Migrated 2026-10-15 (Q5 route ledger). The judge-fn IS the real
  ;; reading-family decision, refusing: the flight ledger is unread, so no
  ;; policy's history is known, the selection law refuses
  ;; :free-energy-not-supplied, and the decision rethrows that as "cascade
  ;; decision refused" (the production repair in this packet). (Retired:
  ;; the live-C refusal guard; live C is observation only.)
  (let [fx (route/fixture)
        unreadable (route/decide-field (route/field {"M-a" nil}) {"M-a" 1} {})
        selected (route/decide fx)
        unread-ledger {:steps [] :read [] :unread [] :dir-status {:absent :no-flights-dir}}
        {:keys [result record]}
        (run-judge (fn [_] (wm-cd/cascade-decision
                            (route/assembled fx 1)
                            (route/opts fx {:conditioning-steps unread-ledger})))
                   {:target "M-autoclock-in"})
        carrier (get-in record [:decision :abstention])]
    (is (= :no-acting-cascade-candidate (get-in unreadable [:decision :reason]))
        "missing construction evidence remains generic no-selection")
    (is (some? (get-in selected [:decision :action]))
        "the same field with a readable ledger selects")
    (is (= :free-energy-not-supplied (get-in carrier [:targets 0 :kind])))
    (is (= route/target (get-in carrier [:targets 0 :target]))
        "the refusal names the field's one target")
    (is (= :abstained (:status carrier)))
    (is (= :free-energy-not-supplied
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
