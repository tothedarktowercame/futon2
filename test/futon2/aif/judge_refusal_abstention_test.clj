(ns futon2.aif.judge-refusal-abstention-test
  "WM-CLICK-REFUSAL-I: a typed refusal of the cascade decision (the judge's
  ex-info \"cascade decision refused\" with :kind) is the tick's typed
  abstention on the run record and on the flight's click entry, never
  :untyped-failure. The fourth flight (flight-e70b4baf) closed its click
  :untyped-failure on :class-unknown-no-scalar-g; live pin: that click's
  repair finding's :failure-data (fixture header: path and sha)."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false] (f))))

(def live (edn/read-string (slurp "test/fixtures/judge-refusal/fourth-flight-failure-data.edn")))

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
