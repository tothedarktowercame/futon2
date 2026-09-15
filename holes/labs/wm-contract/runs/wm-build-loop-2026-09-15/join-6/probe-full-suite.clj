;; claude-2 join-6 probe 4. Read-only diagnostic; the repository test is unchanged.
;; Run the real test namespace from a temp copy that differs only in
;; :runner-options {} (layer 1), with the trusted-entry attestation seam bound to
;; a success-shaped stub (layer 2). Futures inherit the binding (concurrent test).
(require '[clojure.test :as t]
         '[futon3c.wm.run4-trusted-entry :as entry])
(load-file "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/join-6/run4_http_boundary_test.layer1-removed.clj")
(def stub-attestation
  (fn [_]
    {:schema :wm/run4-effective-environment-attestation-v1
     :hierarchy {:model :single-level :scope :RUN4}
     :flags [] :recording {:status :not-attested-by-this-component}}))
(def result
  (binding [entry/*attest-effective-environment* stub-attestation]
    (t/run-tests 'futon3c.wm.run4-http-boundary-test)))
(prn (select-keys result [:test :pass :fail :error]))
(shutdown-agents)
