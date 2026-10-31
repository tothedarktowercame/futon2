(ns futon2.aif.entry-opportunity-standing-test
  "F3 entry-point witness (external review bf0d10e2f / reconciliation 2026-10-31).

  Drives the REAL production entry (runtime/run-opportunity! with an EMPTY
  options map) through the real shared default authority resolver and the real
  internal arbitration/runner path — no stub of resolve-medic-authority, no
  stub of run-opportunity-with-authority!, no reload. The ONLY stubbed thing
  is the downstream ordinary contact port (find-giraffe-run), which is counted
  to prove the production path REACHES it under valid standing authority.

  The injury ledger is a hermetic temp copy of the pinned live 129-entry
  ledger (sha-pinned below); the live file is read once to make the copy and
  its bytes are asserted unchanged. Every meta-self-heal default path is
  redirected into the same temp root before the entry call, so the run can
  write nothing outside the temp directory.

  Negative controls:
  - tampered ledger bytes (same temp copy, :state :open -> :state :doctored)
    must produce the typed refusal :meta-self-heal/ledger-unreadable derived
    from the actual bytes, must NOT reach the contact port (charge/selection
    cannot precede validation), and must not write the ledger.
  - a deny-all authority resolver must change the happy-path outcome, proving
    the happy path genuinely consulted the shared default resolver."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.meta-self-heal-production :as meta-self-heal]
            [futon2.aif.standing-authority :as standing])
  (:import [java.nio.file Files]
           [java.nio.file CopyOption]
           [java.nio.file.attribute FileAttribute]))

(def live-ledger "/home/joe/code/futon2/data/wm-meta-injuries-live-2026-10-22/ledger.edn")
(def live-ledger-sha256 "f7b5a7aac642b35302f30e1254c707805e95188ffd3c8158c51ae6bde56a0074")

(defn- sha256 [path]
  (let [d (.digest (java.security.MessageDigest/getInstance "SHA-256")
                   (Files/readAllBytes (.toPath (io/file path))))]
    (apply str (map #(format "%02x" %) d))))

(defn- temp-root []
  (str (Files/createTempDirectory "f3-entry-witness-"
                                  (make-array FileAttribute 0))))

(defn- hermetic-copy
  "Temp copy of the pinned live ledger; nil when the pinned bytes are absent
  in this environment (witness skips, same convention as the live-ledger
  witness in standing-retained-authority-test)."
  []
  (when (.isFile (io/file live-ledger))
    (let [root (temp-root)
          copy (str root "/ledger.edn")]
      (Files/copy (.toPath (io/file live-ledger))
                  (.toPath (io/file copy))
                  (into-array CopyOption []))
      (when-not (= live-ledger-sha256 (sha256 copy))
        (throw (ex-info "pinned live ledger bytes drifted; witness must not run"
                        {:expected live-ledger-sha256 :actual (sha256 copy)})))
      copy)))

(defn- redirections
  "Vars to redef so the whole entry run stays inside ROOT. These are plain
  path defaults; redefining them is fixture redirection, not stubbing
  behaviour. No namespace reload is performed inside the fixture."
  [root]
  {(resolve 'futon2.aif.meta-self-heal-production/default-ledger-path)
   (str root "/ledger.edn")
   (resolve 'futon2.aif.meta-self-heal-production/default-transaction-path)
   (str root "/transaction.edn")
   (resolve 'futon2.aif.meta-self-heal-production/default-diagnosis-root)
   (str root "/diagnoses")
   (resolve 'futon2.aif.meta-self-heal-production/default-acceptance-root)
   (str root "/acceptances")})

(defn- run-entry!
  "Call the production entry with an EMPTY options map under the given extra
  redirections. find-giraffe-run (the downstream ordinary contact port) is
  replaced by a counting stub — the only behavioural stub in this namespace —
  and the counter atom is returned alongside the result."
  ([root] (run-entry! root {}))
  ([root extra]
   (let [reached (atom 0)
         giraffe-var (resolve 'futon2.aif.full-loop-runtime/find-giraffe-run)
         redirect (merge (redirections root)
                         {giraffe-var (fn [& _] (swap! reached inc) nil)}
                         extra)]
     (with-redefs-fn redirect
       (fn [] {:result (runtime/run-opportunity! {})
               :giraffe-calls @reached
               :verifier-passed-down? (volatile! nil)})))))

(deftest empty-options-entry-runs-the-shared-default-authority
  (if-let [copy (hermetic-copy)]
    (let [root (.getParent (io/file copy))
          before (sha256 copy)
          {:keys [result giraffe-calls]} (run-entry! root)
          authority ((var-get (resolve 'futon2.aif.full-loop-runtime/resolve-medic-authority)) {})
          ident (:identity (standing/standing-context))]
      (testing "the shared default resolver is ready with the standing identity"
        (is (= :ready (:status authority)))
        (is (= 11 (count (:scopes (:identity authority)))))
        (is (= ident (:identity authority))
            "no-opts resolution IS the standing context identity"))
      (testing "the real entry arbitrates and falls through to the ordinary contact port"
        (is (= {:absent :no-verified-giraffe-run} (select-keys result [:absent])))
        (is (= 1 giraffe-calls)
            "production reached the (only stubbed) downstream contact port"))
      (testing "no ledger write by the entry run"
        (is (= before (sha256 copy)))))
    (is true "pinned live ledger absent in this environment; witness skipped")))

(deftest tampered-ledger-refuses-from-actual-bytes-before-the-contact-port
  (if-let [copy (hermetic-copy)]
    (do
      ;; Doctored bytes: every :open transition becomes an invalid state.
      ;; The refusal must be DERIVED from these bytes by the real default
      ;; resolver/load path, not injected.
      (spit copy (.replace (slurp copy) ":state :open" ":state :doctored"))
      (let [root (.getParent (io/file copy))
            before (sha256 copy)
            {:keys [result giraffe-calls]} (run-entry! root)
            carried (:carried-bindings result)]
        (testing "typed refusal derived from the ledger bytes"
          (is (= :refused (:status result)))
          (is (= :meta-self-heal/ledger-unreadable (:reason result)))
          (is (= :ledger-refused (:reason carried)))
          (is (= :store/tampered-ledger (:durable-ledger-reason carried)))
          (is (seq (:durable-ledger-defects carried))))
        (testing "nothing downstream of validation ran"
          (is (zero? giraffe-calls)
              "charge/selection must not move before ledger validation"))
        (testing "no ledger write on refusal"
          (is (= before (sha256 copy))))))
    (is true "pinned live ledger absent in this environment; witness skipped")))

(deftest deny-all-resolver-changes-the-outcome
  ;; Negative control for the happy path above: if run-opportunity! could
  ;; somehow bypass resolve-medic-authority, this redefinition would change
  ;; nothing. It must flip the outcome to the dedicated refusal route.
  (if-let [copy (hermetic-copy)]
    (let [root (.getParent (io/file copy))
          {:keys [result giraffe-calls]}
          (run-entry! root
                      {(resolve 'futon2.aif.full-loop-runtime/resolve-medic-authority)
                       (fn [_] {:status :refused :reason :control/deny-all})})]
      (is (= :refused (:status result)))
      (is (= :control/deny-all (:reason result)))
      (is (= :dedicated-meta-self-heal (:route result)))
      (is (zero? giraffe-calls)
          "a refused authority must not reach the ordinary contact port"))
    (is true "pinned live ledger absent in this environment; witness skipped")))
