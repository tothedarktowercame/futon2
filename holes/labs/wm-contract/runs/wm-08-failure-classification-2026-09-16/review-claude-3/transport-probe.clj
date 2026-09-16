(ns claude3-w08fc-probe
  (:require [futon2.aif.interpretation-job-test :as t]
            [futon2.aif.interpretation-job :as job]
            [futon2.aif.receipt-construction :as construction]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as runner-fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [clojure.pprint :as pp]))

(defn- report [label {:keys [result failures close diagnostics]}]
  (prn {:case label
        :runner-failure-kind (get-in result [:data :failure-kind])
        :repair-class (get-in result [:data :repair-obligation :repair/class])
        :recorded-kind (get-in failures [0 :failure :kind])
        :diagnostic-causes (mapv :class (get-in diagnostics [0 :causes]))
        :close-written? (boolean (seq (get-in close [:payload :close-evidence-manifest :entries])))})
  (flush))

(defn- with-ports [f port-updates]
  (let [run-job job/run!]
    (with-redefs [job/run! (fn [opts action identity dir ports]
                             (run-job opts action identity dir (merge ports port-updates)))]
      (f))))

(defn -main [& _]
  (prn :transport-classes (mapv (fn [[k v]] [(.getName ^Class k) v]) @#'runner/transport-failure-classes))
  (prn :runner-direct {:connect (#'runner/failure-kind-from (java.net.ConnectException. "x"))
                       :timeout (#'runner/failure-kind-from (java.net.SocketTimeoutException. "x"))})
  (hermetic/with-hermetic-stores
    (fn []
      (runner-fixture/with-hermetic-traces
        (fn []
          (with-redefs [pp/pprint (fn [x & _] (prn x))]
            (report :control-construct-npe
                    (with-redefs [construction/construct! (fn [& _] (throw (NullPointerException. "control")))]
                      (t/run-case :receipt :valid)))
            (report :readiness-connect-refused
                    (with-ports #(t/run-case :receipt :valid)
                      {:ready! (fn [_] (throw (java.net.ConnectException. "Connection refused")))}))
            (report :poll-socket-timeout
                    (with-ports #(t/run-case :receipt :valid)
                      {:poll! (fn [_] (throw (java.net.SocketTimeoutException. "Read timed out")))}))
            (report :dispatch-wrapped-connect
                    (with-ports #(t/run-case :receipt :valid)
                      {:dispatch! (fn [& _] (throw (ex-info "dispatch transport" {} (java.net.ConnectException. "Connection refused"))))}))
            (report :construct-non-edn-data
                    (with-redefs [construction/construct!
                                  (fn [& _] (throw (ex-info "fault with object data"
                                                            {:failure-kind :build-failed :file (java.io.File. "/tmp/x")})))]
                      (t/run-case :receipt :valid))))))))
  (shutdown-agents))
