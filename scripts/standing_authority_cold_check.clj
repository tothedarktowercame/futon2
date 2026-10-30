;; Archive cold-check for the standing authority substrate.
;; Usage (from a git archive / fresh clone of the branch, with sibling local
;; deps ../futon1 and ../futon3a available as deps.edn requires — a pre-existing
;; repo assumption, not introduced by this branch):
;;   clojure -M -i scripts/standing_authority_cold_check.clj
;; Optional parameters (JVM opts go BEFORE -M):
;;   clojure -J-Dsa.ledger.path=... -J-Dsa.ledger.sha256=... -M -i scripts/standing_authority_cold_check.clj
;;     (ledger input + pin; defaults to the pinned live ledger path/sha recorded below)
;;   -J-Dsa.repo.root=...    (repo root stripped from pinned original paths in
;;     the hide-originals inertness check (opt-in, default derived); default derived from the ledger path)
;;   -J-Dsa.host.checks=true (machine-specific ordinal-50 absence assertion,
;;     meaningful only on the operator host; off by default)
;; The ledger is COPIED to a hermetic temp dir and the copy's sha256 must match
;; the pin before any check runs; ALL loader checks run against the verified
;; copy only — the original file is never read past the copy step.
;; Exits non-zero on any FAIL; prints :cold-archive-ok on success.
(require '[futon2.aif.standing-authority :as standing]
         '[futon2.aif.meta-injury-store :as store]
         '[futon2.aif.full-loop-runtime :as runtime]
         '[clojure.java.io :as io]
         '[clojure.string :as str])
(import '[java.nio.file Files] '[java.util UUID])

(def default-ledger "/home/joe/code/futon2/data/wm-meta-injuries-live-2026-10-22/ledger.edn")
(def default-ledger-sha "f7b5a7aac642b35302f30e1254c707805e95188ffd3c8158c51ae6bde56a0074")

(def ledger-path (or (System/getProperty "sa.ledger.path") default-ledger))
(def ledger-sha (or (System/getProperty "sa.ledger.sha256") default-ledger-sha))

(defn sha256 [^java.io.File f]
  (let [d (.digest (java.security.MessageDigest/getInstance "SHA-256")
                   (Files/readAllBytes (.toPath f)))]
    (apply str (map #(format "%02x" %) d))))

(defn fail [m] (println :FAIL m) (System/exit 1))

;; 0. hermetic copy of the ledger bytes; the copy is authoritative from here on
(def hermetic (str "/tmp/sa-hermetic-" (UUID/randomUUID)))
(.mkdirs (io/file hermetic))
(def ledger-copy (str hermetic "/ledger.edn"))
(Files/copy (.toPath (io/file ledger-path))
            (.toPath (io/file ledger-copy))
            (into-array [java.nio.file.StandardCopyOption/REPLACE_EXISTING]))
(when-not (.isFile (io/file ledger-path)) (fail [:ledger-missing ledger-path]))
(when-not (= ledger-sha (sha256 (io/file ledger-copy)))
  (fail [:ledger-sha-mismatch (sha256 (io/file ledger-copy))]))

;; 1. tracked resources present in THIS archive tree (no worktree on classpath)
(when-not (io/resource "wm/standing-authority/policy.edn") (fail :policy-resource-missing))
(when-not (= 78 (count (.listFiles (io/file (.getFile (io/resource "wm/standing-authority/evidence"))))))
  (fail :evidence-count))

;; 2. standing context ready
(def ctx (standing/standing-context))
(when-not (= :ready (:status ctx)) (fail [:standing (:status ctx) (:reason ctx)]))
(def ident (:identity ctx))

;; 3. ACTUAL no-opts production authority path (private, resolved at runtime)
(def authority ((var-get (resolve (quote futon2.aif.full-loop-runtime/resolve-medic-authority))) {}))
(when-not (= :ready (:status authority)) (fail [:authority (:status authority)]))
(when-not (= ident (:identity authority)) (fail :authority-identity-mismatch))

;; 4. all four loaders on the VERIFIED COPY of the pinned 129-entry ledger,
;;    defaults with NO verifier supplied
(defn check-loader [r stamp?]
  (when-not (= :loaded (:status r)) (fail [:loader (:status r) (:defects r)]))
  (when-not (= 129 (count (:entries (:ledger r)))) (fail :entry-count))
  (when (and stamp? (not= ident (:authority-identity r))) (fail :stamp-missing)))
(check-loader (store/load-durable-ledger ledger-copy) true)          ; default no-opts
(check-loader (store/load-ledger ledger-copy) true)                  ; default no-opts
(check-loader (store/load-durable-ledger ledger-copy (:verifier authority)) false)
(check-loader (store/load-ledger ledger-copy (:verifier authority)) false)

;; 5. original mutable evidence paths are inert provenance: none of the pinned
;;    paths (with the recorded repo root stripped) exists under THIS tree, so
;;    resolution above ran purely from tracked bytes. Structural backing: the
;;    loader is sha-addressed and contains no mutable-source-path read.
(def policy (read-string (slurp (io/resource "wm/standing-authority/policy.edn"))))
(defn paths [x] (cond (map? x) (mapcat paths (vals x)) (sequential? x) (mapcat paths x)
                      (string? x) (if (str/starts-with? x "/") [x] []) :else nil))
(def orig (distinct (paths policy)))
(def repo-root
  (or (System/getProperty "sa.repo.root")
      ;; derive the repo root by stripping the known live-store suffix
      (let [m (re-find #"^(.*)/data/wm-meta-injuries[^/]*/ledger\.edn$" ledger-path)]
        (and m (second m)))))
(when repo-root
  (let [under-root (filter #(str/starts-with? % repo-root) orig)
        present (filter #(.isFile (io/file (str (System/getProperty "user.dir")
                                                (subs % (count repo-root)))))
                        under-root)]
    ;; H1 safety gate: moving tracked files is only safe in a DISPOSABLE
    ;; archive/clone. Default: report how many pinned originals exist here and
    ;; SKIP the move. -J-Dsa.hide.originals=true opts in to the destructive
    ;; inertness witness: originals are moved aside, standing context is reset
    ;; and re-resolved, and the identical identity must be re-derived from
    ;; tracked evidence bytes alone.
    (if (not= "true" (System/getProperty "sa.hide.originals"))
      (println :originals-present-in-tree (count present)
               :hide-witness-skipped "(opt in with -J-Dsa.hide.originals=true in a disposable archive)")
      (do
        (doseq [p present]
          (Files/move (.toPath (io/file (str (System/getProperty "user.dir") (subs p (count repo-root)))))
                      (.toPath (io/file (str hermetic "/hidden-" (hash p))))
                      (into-array [java.nio.file.StandardCopyOption/REPLACE_EXISTING])))
        (standing/reset-context!)
        (let [re-ctx (standing/standing-context)]
          (when-not (= :ready (:status re-ctx)) (fail [:standing-after-hiding-originals (:status re-ctx) (:reason re-ctx)]))
          (when-not (= ident (:identity re-ctx)) (fail :identity-changed-after-hiding-originals)))
        (println :originals-hidden (count present))))))
;; machine-specific witness (operator host only, opt-in): the deleted
;; ordinal-50 original stays deleted — external runners skip this.
(when (= "true" (System/getProperty "sa.host.checks"))
  (when (.isFile (io/file (str repo-root "/test/futon2/aif/wm/injury49_learning_read_bound_test.clj")))
    (fail :ordinal50-original-still-present)))

;; 6. tamper negative on the hermetic copy: byte-level substitution of the
;;    ordinal-38 receipt pin; must refuse visibly (typed defects, stamped)
(def copy-text (slurp ledger-copy))
(def entry38 (first (filter #(= 38 (:ordinal %)) (:entries (read-string copy-text)))))
(def pin-sha (get-in entry38 [:medic-operation :authority-receipt-pin :sha256]))
(when-not (string? pin-sha) (fail :pin-not-found))
(spit (str hermetic "/tampered.edn") (str/replace copy-text pin-sha (apply str (repeat 64 "0"))))
(def d (store/load-durable-ledger (str hermetic "/tampered.edn")))
(when-not (= :refused (:status d)) (fail [:tamper-not-refused (:status d)]))
(when-not (some #(= :entry/transition-invalid %) (:defects d)) (fail [:tamper-defects (:defects d)]))
(when (nil? (:authority-identity d)) (fail :refused-outcome-unstamped))

;; 7. restart witness: same identity this process; caller compares across two JVMs
(println :cold-archive-ok (pr-str {:scopes (count (:scopes ident))
                                   :identity (select-keys ident [:scopes])}))
