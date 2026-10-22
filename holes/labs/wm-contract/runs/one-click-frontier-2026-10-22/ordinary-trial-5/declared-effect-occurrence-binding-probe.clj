(require '[futon2.aif.close-retention :as close])
(try
 (close/mint-occurrence {:run-id "probe" :cohort-id "probe" :attempt-id "probe"
                         :selected-action {:kind :cascade-candidate :target "M-probe"}
                         :declared-effect-sha256 (apply str (repeat 64 "a"))
                         :now #(java.time.Instant/parse "2026-10-22T00:00:00Z")
                         :uuid-fn #(java.util.UUID/randomUUID)})
 (throw (ex-info "Unexpected acceptance" {}))
 (catch clojure.lang.ExceptionInfo e
  (assert (= :shape-invalid (:close-retention/refusal (ex-data e))))
  (prn (ex-data e))))
