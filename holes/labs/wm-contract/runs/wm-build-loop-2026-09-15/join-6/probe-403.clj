;; claude-2 join-6 probe: replay the failing test's valid RUN4 request through the
;; same fixture and print the full response, so the 403's stated reason is on record.
;; Read-only: uses the test namespace's own temp-dir fixture; click! is stubbed.
(require '[futon3c.wm.run4-http-boundary-test :as t]
         '[futon3c.wm.runner-service :as service])
(t/with-handler
  (fn [handler _ _ _]
    (with-redefs [service/click! (fn [opts] {:click-id "probe" :started-at "2026-09-15T00:00:00Z"})]
      (let [resp (handler (t/request {:run4-pin-ref "pin.edn" :run4-attempt-id "attempt-probe"} t/auth))]
        (prn {:status (:status resp)
              :body (let [b (:body resp)] (if (string? b) b (slurp b)))})))))
(shutdown-agents)
