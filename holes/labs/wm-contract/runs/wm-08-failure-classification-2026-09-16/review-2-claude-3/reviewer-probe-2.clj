;; claude-3 follow-up controls for fb849b22. Both hermetic fixtures; ports injected via job/run!.
(require '[futon2.aif.interpretation-job-test :as t]
         '[futon2.aif.interpretation-job :as job]
         '[futon2.aif.receipt-construction :as construction]
         '[futon2.aif.full-loop-runner-test :as runner-fixture]
         '[futon2.aif.hermetic-repair-fixture :as hermetic]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp])

(defn- report [label {:keys [result failures diagnostics calls constructors]}]
  (prn {:case label
        :runner-failure-kind (get-in result [:data :failure-kind])
        :repair-class (get-in result [:data :repair-obligation :repair/class])
        :recorded-kind (get-in failures [0 :failure :kind])
        :causes (mapv :class (get-in diagnostics [0 :causes]))
        :calls calls :constructors constructors})
  (flush))

(defn- with-ports [port-updates]
  (let [run-job job/run!]
    (with-redefs [job/run! (fn [opts action identity dir ports]
                             (run-job opts action identity dir (merge ports port-updates)))]
      (t/run-case :receipt :valid))))

(defn- thrower [e] (fn [& _] (throw e)))

(defn main []
  (hermetic/with-hermetic-stores
    (fn []
      (runner-fixture/with-hermetic-traces
        (fn []
          (with-redefs [pp/pprint (fn [x & _] (prn x))]
            ;; 1. message-gated bare SocketException: listed wording vs deliberately absent wording
            (report :poll-socket-connection-reset
                    (with-ports {:poll! (thrower (java.net.SocketException. "Connection reset"))}))
            (report :poll-socket-is-closed-local-misuse
                    (with-ports {:poll! (thrower (java.net.SocketException. "Socket is closed"))}))
            ;; 2. subclass not enumerated by name
            (report :ready-no-route-to-host
                    (with-ports {:ready! (thrower (java.net.NoRouteToHostException. "No route"))}))
            ;; 3. double wrapping
            (report :dispatch-double-wrapped-timeout
                    (with-ports {:dispatch! (thrower (ex-info "outer" {} (RuntimeException. "mid" (java.net.SocketTimeoutException. "Read timed out"))))}))
            ;; 4. explicit :outcome nested above transport stays explicit
            (report :poll-outcome-typed-over-transport
                    (with-ports {:poll! (thrower (ex-info "typed outcome" {:outcome :agent-job-stalled} (java.net.ConnectException. "refused")))}))
            ;; 5. content refusal wrapping transport keeps content classification
            (report :poll-refusal-over-transport
                    (with-ports {:poll! (thrower (ex-info "refusal" {:interpretation/refusal :interpretation/invalid-receipt} (java.net.ConnectException. "refused")))}))
            ;; 6. transport during construction (after a valid receipt)
            (report :construct-connect-refused
                    (with-redefs [construction/construct! (thrower (java.net.ConnectException. "construct refused"))]
                      (t/run-case :receipt :valid)))
            ;; 7. classifier that itself throws: must not mask the original fault silently
            (report :classifier-throws
                    (with-ports {:transport-failure-kind (fn [_] (throw (IllegalStateException. "classifier bug")))
                                 :poll! (thrower (java.net.ConnectException. "refused"))}))
            ;; 8. missing classifier with a real directory: refuses and writes nothing
            (let [dir (io/file (System/getProperty "java.io.tmpdir") (str "c3-wm08b-missing-" (System/nanoTime)))
                  r (try (job/run! {} {} {} dir {:ready! (constantly {:ok true})}) :no-throw
                         (catch clojure.lang.ExceptionInfo e (ex-data e)))]
              (prn {:case :missing-classifier-real-dir :result r :dir-exists? (.exists dir)})))))))
  (shutdown-agents))

(main)
