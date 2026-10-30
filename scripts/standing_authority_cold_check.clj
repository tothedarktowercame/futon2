;; Archive cold-check for the standing authority substrate.
;; Usage (from a git archive / fresh clone of the branch, with sibling local
;; deps ../futon1 and ../futon3a available as deps.edn requires):
;;   clojure -M -i scripts/standing_authority_cold_check.clj
;; Exits non-zero on any FAIL; prints :cold-archive-ok on success.
(require '[futon2.aif.standing-authority :as standing]
         '[futon2.aif.meta-injury-store :as store]
         '[futon2.aif.full-loop-runtime :as runtime]
         '[clojure.java.io :as io]
         '[clojure.string :as str])
(import '[java.nio.file Files] '[java.util UUID])

(def live "/home/joe/code/futon2/data/wm-meta-injuries-live-2026-10-22/ledger.edn")
(def hermetic (str "/tmp/sa-hermetic-" (UUID/randomUUID)))
(.mkdirs (io/file hermetic))
;; hermetic copy of ONLY the pinned ledger bytes
(Files/copy (.toPath (io/file live))
            (.toPath (io/file (str hermetic "/ledger.edn")))
            (into-array [java.nio.file.StandardCopyOption/REPLACE_EXISTING]))

(defn fail [m] (println :FAIL m) (System/exit 1))

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

;; 4. all four loaders on the pinned 129-entry ledger, defaults with NO verifier supplied
(defn check-loader [r stamp?]
  (when-not (= :loaded (:status r)) (fail [:loader (:status r) (:defects r)]))
  (when-not (= 129 (count (:entries (:ledger r)))) (fail :entry-count))
  (when (and stamp? (not= ident (:authority-identity r))) (fail :stamp-missing)))
(check-loader (store/load-durable-ledger live) true)          ; default no-opts
(check-loader (store/load-ledger live) true)                  ; default no-opts
(check-loader (store/load-durable-ledger live (:verifier authority)) false)
(check-loader (store/load-ledger live (:verifier authority)) false)

;; 5. hermetic copy: original mutable evidence paths are absent from the archive
;;    tree and the deleted ordinal-50 original is absent on disk; retained blobs
;;    still work (resolution above already proves it from tracked bytes)
(def policy (read-string (slurp (io/resource "wm/standing-authority/policy.edn"))))
(defn paths [x] (cond (map? x) (mapcat paths (vals x)) (sequential? x) (mapcat paths x)
                      (string? x) (if (str/starts-with? x "/") [x] []) :else nil))
(def orig (distinct (paths policy)))
(when (some #(.isFile (io/file (str "/tmp/sa-archive" %))) orig) (fail :original-path-in-archive))
(when (.isFile (io/file "/home/joe/code/futon2/test/futon2/aif/wm/injury49_learning_read_bound_test.clj"))
  (fail :ordinal50-original-still-present))
(check-loader (store/load-durable-ledger (str hermetic "/ledger.edn")) true)

;; 6. tamper negative on the hermetic copy: byte-level substitution of the
;;    ordinal-38 receipt pin; must refuse visibly (typed defects, stamped)
(def live-text (slurp live))
;; target: the authority-receipt-pin sha recorded with ordinal 38's entry
(def entry38 (first (filter #(= 38 (:ordinal %)) (:entries (read-string live-text)))))
(def pin-sha (get-in entry38 [:medic-operation :authority-receipt-pin :sha256]))
(when-not (string? pin-sha) (fail :pin-not-found))
(spit (str hermetic "/tampered.edn") (str/replace live-text pin-sha (apply str (repeat 64 "0"))))
(def d (store/load-durable-ledger (str hermetic "/tampered.edn")))
(when-not (= :refused (:status d)) (fail [:tamper-not-refused (:status d)]))
(when-not (some #(= :entry/transition-invalid %) (:defects d)) (fail [:tamper-defects (:defects d)]))
(when (nil? (:authority-identity d)) (fail :refused-outcome-unstamped))

;; 7. restart witness: same identity this process; caller compares across two JVMs
(println :cold-archive-ok (pr-str {:scopes (count (:scopes ident))
                                   :identity (select-keys ident [:scopes])}))
