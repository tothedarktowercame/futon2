(ns futon2.aif.phase-refresh-record-test
  "WM-PHASE-SWALLOW-I: the :preference-refresh phase caught every throwable
  and said :ok, so on the eighth flight (flight-ada87008) it reported :ok
  after 85,894 ms whatever the refresh did. The phase stays non-fatal and
  its :outcome stays :ok; the refresh's own outcome is recorded beside it,
  on the phase event and the run record (:refresh). Live pin: that phase
  event (fixture header: path and sha)."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false] (f))))

(def eighth-event (edn/read-string (slurp "test/fixtures/phase-refresh/eighth-flight-refresh-event.edn")))

(defn- run [refresh-fn]
  (let [log (atom [])
        result (runner/run-opportunity!
                (merge (fixture/isolated-runner-opts)
                       {:refresh-fn refresh-fn
                        :phase-log-fn #(swap! log conj %)
                        :judge-fn (fn [_] (throw (ex-info "stop after selection" {})))
                        :repair-system-record-fn (fn [m] {:repair/id "repair-test-1"
                                                          :repair/class (:repair-class m)})
                        :dispatch-fn (fn [& _] (throw (ex-info "Unexpected dispatch" {})))}))
        events @log
        end (first (filter #(and (= :preference-refresh (:phase %)) (= :end (:transition %))) events))]
    {:end end
     :after (map :phase (rest (drop-while #(not= end %) events)))
     :record (edn/read-string (slurp (:run-record result)))}))

(deftest a-failed-refresh-is-recorded-and-the-tick-goes-on
  (let [{:keys [end after record]}
        (run (fn [] (throw (ex-info "x" {:kind :substrate-unreachable}
                                    (java.net.ConnectException. "Connection refused")))))
        r (:refresh end)]
    (is (= :ok (:outcome end)) "the phase's own outcome is unchanged")
    (is (= :refresh-failed (:outcome r)))
    (is (= :substrate-unreachable (:failure-kind r)))
    (is (= {:class "clojure.lang.ExceptionInfo" :message "x" :ex-kind :substrate-unreachable
            :cause [{:class "java.net.ConnectException" :message "Connection refused"}]}
           (:error r)))
    (is (= :stop-line-memory (first after)) "the tick proceeds to the next phase")
    (is (some #{:selection} after))
    (is (= r (:refresh record) (runner/phase-refresh record)) "the run record carries it")))

(deftest a-refresh-that-returns
  (let [{:keys [end record]} (run (fn [] {:some :c-state}))]
    (is (= :ok (:outcome end)))
    (is (= {:outcome :ok :freshness {:absent :not-reported-by-maybe-refresh}} (:refresh end)))
    (is (= (:refresh end) (:refresh record)))))

(deftest an-error-not-an-exception-is-recorded-and-non-fatal
  ;; the bad case: the old catch was Throwable; an Error must neither escape
  ;; nor pass unrecorded
  (let [{:keys [end after]} (run (fn [] (throw (StackOverflowError. "deep"))))
        r (:refresh end)]
    (is (= :ok (:outcome end)))
    (is (= :refresh-failed (:outcome r)))
    (is (= :untyped-failure (:failure-kind r)))
    (is (= "java.lang.StackOverflowError" (get-in r [:error :class])))
    (is (= :stop-line-memory (first after)))))

(deftest explicit-typing-wins-over-the-throwers-kind
  (is (= :b (get-in (run (fn [] (throw (ex-info "x" {:kind :a :failure-kind :b}))))
                    [:end :refresh :failure-kind]))))

(deftest the-eighth-flights-phase-event-reads-typed-absent
  (is (= [:ok 85894] [(:outcome eighth-event) (:duration-ms eighth-event)]) "what the eighth flight recorded")
  (is (not (contains? eighth-event :refresh)))
  (is (= {:absent :no-refresh-record} (runner/phase-refresh eighth-event)))
  (is (= {:absent :no-refresh-record} (runner/phase-refresh {:route []})) "an older run record likewise"))
