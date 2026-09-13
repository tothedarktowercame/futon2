#!/usr/bin/env bb
;; :F11 slice 1 -- the F2-falsifier reconciliation, measured against the
;; current library state.
;;
;; P-validated-R5.md:490-497 (the C61 amendment, 2026-08-31) records the four
;; `findF*` bindings as stale "until the representations are reconciled and the
;; strengthened check passes".  Both happened (C73, C500).  What this script
;; measures is whether the reconciliation still holds at the library as it is
;; TODAY, and what moved in the pinned record since C500 measured it.
;;
;; It does NOT invoke `find_snatch.clj`, for the reason `u46_transcribe.bb`
;; does not: the positive path recomputes the report from the live library and
;; OVERWRITES `futon3:checks/find-snatch.edn`, which is the pinned fixture the
;; four Lean docstrings name (C500 s3).  The live re-run is therefore taken
;; once, by hand, and committed here as `01-find-snatch-live.edn`; the
;; comparison below is a pure function of that file and the pin, so it
;; reproduces.
;;
;;   --negative  mutate one live `:if-text` and confirm the classifier stops
;;               calling the difference lines-only.

(require '[babashka.classpath :as cp]
         '[clojure.java.io :as io])
(cp/add-classpath (.getCanonicalPath
                   (io/file (.getParent (io/file *file*)) "../../../src")))

(require '[futon2.aif.find-reconciliation :as reconciliation]
         '[clojure.edn :as edn]
         '[clojure.pprint :as pprint]
         '[clojure.string :as str])

(def task-home (System/getenv "HOME"))
(def pin-path (str task-home "/code/futon3/checks/find-snatch.edn"))
(def live-path "runs/F11-find/01-find-snatch-live.edn")
(def out-path "runs/F11-find/02-reconciliation.edn")

(defn sha256 [path]
  (let [d (java.security.MessageDigest/getInstance "SHA-256")]
    (->> (.digest d (java.nio.file.Files/readAllBytes (.toPath (io/file path))))
         (map #(format "%02x" %))
         str/join)))

(def report reconciliation/report)

(defn -main [& args]
  (let [pin (edn/read-string (slurp pin-path))
        live (edn/read-string (slurp live-path))]
    (if (some #{"--negative"} args)
      (let [mutated (update-in live [:scenarios 0 :round-results 0 :find :receipts
                                     (first (get-in live [:scenarios 0 :round-results 0 :find :selected]))
                                     :warrant :if-text]
                               (constantly "a clause the library does not carry"))
            r (report pin mutated)]
        (if (:difference-is-line-coordinates-only? r)
          (do (println "f11-f2-reconcile: FAIL a mutated clause text was still counted lines-only"
                       "exit-convention=0-pass/1-fail")
              (System/exit 2))
          (do (println "f11-f2-reconcile: PASS mutated clause text rejected"
                       "finding=:clause-text-differs exit-convention=0-pass/1-fail")
              (System/exit 0))))
      (let [r (assoc (report pin live)
                     :pin-path "futon3:checks/find-snatch.edn"
                     :pin-sha256 (sha256 pin-path)
                     :live-path live-path
                     :live-sha256 (sha256 live-path))]
        (io/make-parents out-path)
        (spit out-path (with-out-str (pprint/pprint r)))
        (println (format "f11-f2-reconcile: %d/%d receipts differ over %d/%d rounds; fields %s; lines-only? %s; live drift %d"
                         (:receipts-differing r) (:receipts-compared r)
                         (:rounds-differing r) (:rounds-total r)
                         (pr-str (keys (:differing-fields r)))
                         (:difference-is-line-coordinates-only? r)
                         (:drift-mismatch-count-live r)))
        (println (str "wrote " out-path))))))

(apply -main *command-line-args*)
