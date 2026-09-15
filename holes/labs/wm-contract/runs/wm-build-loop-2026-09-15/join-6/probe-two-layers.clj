;; claude-2 join-6 probe 3. Read-only diagnostic; no repository change.
;; Same replay as probe-cause.clj with two differences only: :runner-options {}
;; (layer 1) and the trusted-entry attestation seam bound to a success-shaped
;; stub (layer 2). If the valid request then reaches click! with 200, the
;; fixture is stale in exactly these two respects.
(load-file "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/join-6/probe-cause-lib.clj")
(require '[futon3c.wm.run4-trusted-entry :as entry])
(def stub-attestation
  (fn [_declaration]
    {:schema :wm/run4-effective-environment-attestation-v1
     :hierarchy {:model :single-level :scope :RUN4}
     :flags [] :recording {:status :not-attested-by-this-component}}))
(prn {:both-layers-bypassed
      (binding [entry/*attest-effective-environment* stub-attestation]
        (replay {}))})
(shutdown-agents)
