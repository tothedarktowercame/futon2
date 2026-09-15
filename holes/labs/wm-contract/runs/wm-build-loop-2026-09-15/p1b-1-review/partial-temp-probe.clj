;; claude-2 P1b-1 review probe (real filesystem, temp dirs). A crash before any
;; bytes reach a temp file leaves it zero-length. Is that reported as damage to
;; committed history, or as an uncommitted preparation (pending recovery)?
(require '[futon2.aif.work-target-store :as store])
(import '(java.nio.file Files) '(java.util UUID))
(defn genesis []
  {:schema :wm/work-target-store-genesis-v1 :store/id (UUID/randomUUID)
   :storage-protocol/revision "v1"
   :declaration {:path "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/declarations/wm-work-target-interpretation-v1.edn"
                 :sha256 "055d579d4bec9ef52c6a3e2949b730d413624ddbc6b60cc00931810a77675e5e"
                 :interpretation-revision "v1"}
   :decision-refs ["probe"] :created-at "2026-09-15T21:00:00Z"
   :authorized-by {:actor "claude-2-review-probe" :commission "isolated-temp-store"}
   :statement "rollout genesis, not historical initialization"})
(defn probe [leftover]
  (let [dir (Files/createTempDirectory "wts-probe-" (make-array java.nio.file.attribute.FileAttribute 0))
        s (store/open-store (str (.resolve dir "store")) {:payload-validator (constantly :ok)})]
    (store/initialize! s (genesis))
    (store/commit! s (:head (store/read-store s))
                   {:id "one" :kind :probe :caller-identity-type :test :information-cutoff "t"} {})
    (spit (.toFile (.resolve (:path s) leftover)) "")
    (let [r (store/read-store s)]
      {:leftover leftover :status (:status r) :reason (:reason r)})))
(prn [(probe "snapshots/snapshot.tmp") (probe "HEAD.edn.tmp") (probe "PENDING.edn.tmp")])
(shutdown-agents)
