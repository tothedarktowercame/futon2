(ns futon2.aif.failure-cause-record-test
  "WM-CAUSE-ON-RECORD-I: the close map carries the cause chain beneath the
  failing exception (flight/throwable-summary's shape), and the repair
  finding carries it as :failure-cause beside :failure-error/:failure-data.
  The eighth flight (flight-ada87008) recorded the registry read's outer
  message and {:kind :substrate-unreachable} and nothing of the throwable
  beneath; live pin: that finding (test/fixtures/phase-kind, header: path and
  sha). Thrown through the runner seam phase-kind-test uses (:judge-fn)."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false] (f))))

(def live (edn/read-string (slurp "test/fixtures/phase-kind/eighth-flight-failure.edn")))

(defn- run [judge-throws]
  (let [findings (atom [])
        result (runner/run-opportunity!
                (merge (fixture/isolated-runner-opts)
                       {:judge-fn (fn [_] (throw judge-throws))
                        :repair-system-record-fn (fn [m] (swap! findings conj m)
                                                   {:repair/id (str "repair-test-" (count @findings))
                                                    :repair/class (:repair-class m)})
                        :dispatch-fn (fn [& _] (throw (ex-info "Unexpected dispatch" {})))}))]
    {:result result :finding (first @findings)}))

(defn- chain
  "An ex-info whose cause chain beneath it is N RuntimeExceptions c1..cN."
  [n]
  (ex-info "outer" {:kind :substrate-unreachable}
           (reduce (fn [cause i] (RuntimeException. (str "c" i) cause))
                   nil (range n 0 -1))))

(defn- run-record [throwable]
  (let [{:keys [result]} (run throwable)]
    (edn/read-string (slurp (:run-record result)))))

(deftest run-record-retains-bounded-error-detail
  (let [body (apply str (repeat 400 "x"))
        record (run-record
                (ex-info "Agency dispatch failed"
                         {:status 503
                          :response {:error "agent-not-found" :agent "zai-2"}
                          :body body
                          :stream (Object.)}))]
    (is (= {:status 503
            :response {:error "agent-not-found" :agent "zai-2"}
            :omitted [:body :stream]}
           (get-in record [:failure :detail]))))
  (is (= {:absent :no-error-data}
         (get-in (run-record (RuntimeException. "no ex-data"))
                 [:failure :detail])))
  (let [record (run-record
                (ex-info "long detail" {:body (apply str (repeat 301 "x"))}))]
    (is (= {:omitted [:body]} (get-in record [:failure :detail])))))

(deftest the-eighth-flights-throw-carries-its-cause
  (let [{:keys [result finding]}
        (run (ex-info (:failure-error live) (:failure-data live)
                      (java.net.ConnectException. "Connection refused")))]
    (is (not (contains? live :failure-cause)) "what the eighth flight recorded: no cause")
    (is (= {:cause [{:class "java.net.ConnectException" :message "Connection refused"}]}
           (:failure-cause finding)))
    (is (= (:failure-cause finding) (get-in result [:data :cause])) "the close map carries it")
    (is (= :substrate-unreachable (:failure-kind finding)) "J1 unaffected")
    (is (= (:failure-error live) (:error finding)) "the outer message kept")
    (is (= (:failure-data live) (dissoc (:failure-data finding) :failure-kind))
        "the outer ex-data kept (J1 adds :failure-kind)")))

(deftest a-judge-refusal-records-what-is-beneath-the-judges-throw
  ;; the abstention re-throw is the runner's own: the chain starts beneath
  ;; the judge's exception, not with it
  (is (= {:absent :no-cause}
         (:failure-cause (:finding (run (ex-info "cascade decision refused" {:kind :live-c-stale}))))))
  (is (= {:cause [{:class "java.lang.RuntimeException" :message "c1"}]}
         (:failure-cause (:finding (run (ex-info "cascade decision refused" {:kind :live-c-stale}
                                                 (RuntimeException. "c1"))))))))

(deftest a-three-deep-chain-lands-three-entries-in-order
  (is (= {:cause [{:class "java.lang.RuntimeException" :message "c1"}
                  {:class "java.lang.RuntimeException" :message "c2"}
                  {:class "java.lang.RuntimeException" :message "c3"}]}
         (:failure-cause (:finding (run (chain 3)))))))

(deftest no-cause-is-typed
  (is (= {:absent :no-cause}
         (:failure-cause (:finding (run (ex-info (:failure-error live) (:failure-data live))))))))

(deftest a-seven-deep-chain-is-cut-at-five-and-says-so
  (let [c (:failure-cause (:finding (run (chain 7))))]
    (is (= ["c1" "c2" "c3" "c4" "c5"] (mapv :message (:cause c))))
    (is (= 5 (:cause-cut-at c)))
    (is (not (contains? (:failure-cause (:finding (run (chain 5)))) :cause-cut-at))
        "exactly five is the whole chain, not a cut")))

(deftest the-durable-finding-keeps-it
  ;; through the real writer (repair/record-system-failure!, hermetic store):
  ;; its record is built key by key, so the key must be carried there too
  (let [result (runner/run-opportunity!
                (merge (fixture/isolated-runner-opts)
                       {:judge-fn (fn [_] (throw (ex-info (:failure-error live) (:failure-data live)
                                                          (java.net.ConnectException. "Connection refused"))))
                        :dispatch-fn (fn [& _] (throw (ex-info "Unexpected dispatch" {})))}))
        stored (get-in result [:data :repair-obligation])]
    (is (string? (:repair/id stored)))
    (is (= {:cause [{:class "java.net.ConnectException" :message "Connection refused"}]}
           (:failure-cause stored)))
    (is (= (:failure-cause stored) (runner/finding-failure-cause stored)))))

(deftest an-old-finding-reads-typed-absent
  (is (= {:absent :cause-not-on-record} (runner/finding-failure-cause live)))
  (is (= {:absent :no-cause} (runner/finding-failure-cause {:failure-cause {:absent :no-cause}}))))
