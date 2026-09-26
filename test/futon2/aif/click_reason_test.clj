(ns futon2.aif.click-reason-test
  "WM-CLICK-REASON-I: the flight's click entry says why the click closed,
  :failure {:kind :stage :error :cause}. record-summary reads only the run
  record, which carried no failure, so the eighth flight's entry
  (flight-ada87008) said :outcome :incomplete and nothing more. The runner
  now writes :failure onto the run record from the close map
  (run-record-failure); record-summary reads it; record-click keeps it.
  Live pin: that click entry (fixture header: path and sha)."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false] (f))))

(def eighth-click (edn/read-string (slurp "test/fixtures/click-reason/eighth-flight-click.edn")))
(def eighth-failure (edn/read-string (slurp "test/fixtures/phase-kind/eighth-flight-failure.edn")))

(def route [{:fromNode "RUNNER" :toNode "FULL_LOOP_CLOSE" :via :incomplete}])

(def eighth-shaped-record
  {:route route
   :failure {:kind :substrate-unreachable
             :stage :selection
             :error "substrate-2 mission registry unreachable"
             :cause {:cause [{:class "java.net.ConnectException" :message "Connection refused"}]}}})

(defn- entry
  "The click entry record-click writes over RUN-RECORD, through
  record-summary, as http-click-fn hands it on."
  [run-record]
  (let [f (flight/start {:target "M-autoclock-in" :chosen-because {:kind :requested}}
                        {:kind :a-exits :repo "futon3c" :path "p" :read-text (fn [& _] "")}
                        {:id "flight-test"})
        summary (fr/record-summary "M-autoclock-in" "click-1" run-record)]
    (first (:clicks (flight/record-click f (merge summary {:wants [:exit/a] :before {} :after {}}))))))

(deftest the-eighth-flights-click-entry-reads-typed-absent
  (is (= :incomplete (:outcome eighth-click)) "what the eighth flight's entry carried")
  (is (not (contains? eighth-click :failure)))
  (is (= {:absent :no-failure-on-click-entry} (flight/click-failure eighth-click))))

(deftest an-eighth-flight-shaped-close-reaches-the-click-entry
  (let [e (entry eighth-shaped-record)]
    (is (= (:failure eighth-shaped-record) (:failure e)) "all four parts")
    (is (= (:failure e) (flight/click-failure e)))
    (is (= :incomplete (:outcome e)))))

(deftest through-the-runner-the-run-record-carries-the-failure
  ;; the hop this packet adds: the close map onto the run record
  (let [result (runner/run-opportunity!
                (merge (fixture/isolated-runner-opts)
                       {:judge-fn (fn [_] (throw (ex-info (:failure-error eighth-failure)
                                                          (:failure-data eighth-failure)
                                                          (java.net.ConnectException. "Connection refused"))))
                        :repair-system-record-fn (fn [m] {:repair/id "repair-test-1"
                                                          :repair/class (:repair-class m)})
                        :dispatch-fn (fn [& _] (throw (ex-info "Unexpected dispatch" {})))}))
        record (edn/read-string (slurp (:run-record result)))
        e (entry record)]
    (is (= {:kind :substrate-unreachable
            :stage :selection
            :error "substrate-2 mission registry unreachable"
            :cause {:cause [{:class "java.net.ConnectException" :message "Connection refused"}]}}
           (:failure record)
           (:failure e)))
    (is (= :incomplete (:outcome e)))))

(deftest a-record-with-no-failure
  (let [e (entry {:route [{:fromNode "RUNNER" :toNode "FULL_LOOP_CLOSE" :via :grounded-change}]
                  :failure {:absent :no-failure}})]
    (is (= {:absent :no-failure-on-run-record} (:failure e)))
    (is (= :grounded-change (:outcome e)) "outcome unchanged")))

(deftest a-record-written-before-this-packet
  (is (= {:absent :failure-not-on-run-record} (:failure (entry {:route route})))))

(deftest no-record
  (let [s (fr/record-summary "M-autoclock-in" "click-1" nil)]
    (is (= {:kind :run-record-missing :missing :run-record} (:abstention s)))
    (is (not (contains? s :failure)) "no :failure invented")
    (is (not (contains? (entry nil) :failure)))))

(deftest a-failure-recorded-before-the-cause-was
  ;; the bad case: a kind with no cause (a record written before ac06a830)
  ;; types the cause, neither nil nor the whole :failure absent
  (let [f (:failure (entry (update eighth-shaped-record :failure dissoc :cause)))]
    (is (= :substrate-unreachable (:kind f)))
    (is (= {:absent :cause-not-on-record} (:cause f)))
    (is (= "substrate-2 mission registry unreachable" (:error f)))))
