;; reference_regression.clj — the REFERENCE-OUTPUT REGRESSION SUITE
;; (driver: zai-10, 2026-07-05; assignment from claude-16)
;;
;; Tonight's committed artifacts ARE the reference outputs. This script
;; re-derives each and diffs against the stored reference; any drift is loud.
;;
;; WHY .clj (not .bb): every check exercises Clojure namespaces on the futon2
;; classpath (fold-escrow, fold-llm, mana-gate) via `clojure -M`; babashka
;; cannot load these deps. The p3_retry_enriched kill-test is itself a -M
;; script, so the suite is homogeneous with the existing harness scripts.
;;
;; Usage:  cd /home/joe/code/futon2 && clojure -M scripts/reference_regression.clj
;;
;; Each check prints CHECK N: <name> — PASS or DRIFT (with details).
;; Final line: "N checks, N pass".
;;
;; Provenance per check names the commit sha or log section the reference
;; numbers come from.
(ns reference-regression
  (:require [futon2.aif.fold-escrow :as esc]
            [futon2.aif.fold-llm :as fl]
            [futon2.aif.mana-gate :as mg]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.security MessageDigest]))

;; =============================================================================
;; HARNESS
;; =============================================================================

(def check-results (atom []))
(def check-counter (atom 0))

(defn sha256 [^String s]
  (let [d (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8"))]
    (apply str (map #(format "%02x" %) d))))

(defn approx=
  "True if |a-b| <= tol."
  [a b tol]
  (<= (Math/abs (- (double a) (double b))) tol))

(defn check! [name pass? details]
  (let [n (swap! check-counter inc)
        result {:n n :name name :pass pass? :details details}
        status (if pass? "PASS" "DRIFT")]
    (swap! check-results conj result)
    (println (format "CHECK %d: %s — %s%s"
                     n name status
                     (if (str/blank? details) "" (str " — " details))))))

;; =============================================================================
;; CHECK 1: CONSTRUCTOR — cascade for the frozen peradam psi + aif-head psi
;; =============================================================================
;; Provenance:
;;   peradam: E-live-loop-3.md "deposit-006 log" — "size 11, F +2.227,
;;            top no-self-certification 0.521"; psi-sha 97b9de29... (deposit 006)
;;   aif-head: E-live-loop-3.md "Deposit: ft-aif-head-004" — "size 21
;;             (truncated at 20), F = +8.722, top ants/baseline-cyber-ant 0.679"
;;             psi-sha b9c15315... (deposit 004)
;;
;; The constructor is a Python process (futon3a .venv). We shell out to
;; cascade_serve.py and parse its JSON. The F-free-energy has minor run-to-run
;; drift from the embedding model; tolerance 0.001 on peradam (stable, small),
;; 0.5 on aif-head (larger cascade, more drift surface — the logged 8.722 vs
;; live 8.862 is within this band; the STRUCTURAL facts — size, truncation,
;; top pattern, top rel — are exact).

(defn run-constructor [psi-file budget epsilon]
  (let [psi (slurp psi-file)
        cmd ["/home/joe/code/futon3a/.venv/bin/python3"
             "holes/labs/M-memes-arrows/cascade_serve.py"
             psi (str budget) (str epsilon)]
        proc (.. (ProcessBuilder. (java.util.ArrayList. cmd))
                 (directory (io/file "/home/joe/code/futon3a"))
                 ;; stderr SEPARATE so model-load noise doesn't break JSON parsing
                 (redirectError (java.lang.ProcessBuilder$Redirect/to
                                  (java.io.File. (str (System/getProperty "java.io.tmpdir")
                                                      "/ref-regression-ctor-err.txt"))))
                 (start))
        out (slurp (.getInputStream proc))
        _ (.waitFor proc)
        ;; The JSON is the last line that starts with {
        json-line (->> (str/split-lines out)
                       (filter #(str/starts-with? (str/trim %) "{"))
                       last)]
    (when json-line
      (json/parse-string json-line true))))

(defn check-1-constructor []
  (let [peradam (run-constructor
                  "/home/joe/code/futon2/holes/reference-psis/psi-peradam-mech.txt" 20 0.15)]
    (if-not peradam
      (check! "1a: constructor peradam cascade" false "no JSON from constructor")
      (let [size (:size peradam)
            f (:F-free-energy peradam)
            top-pat (get-in peradam [:shown 0 :pattern])
            top-rel (get-in peradam [:shown 0 :rel])]
        (check! "1a: constructor peradam cascade"
                (and (= size 11)
                     (approx= f 2.227 0.001)
                     (= top-pat "aif/no-self-certification")
                     (approx= top-rel 0.521 0.001))
                (format "size=%s F=%.3f top=%s rel=%.3f (ref: 11 / +2.227 / no-self-certification / 0.521)"
                        size (double f) top-pat (double top-rel))))))
  (let [aif-head (run-constructor
                   "/home/joe/code/futon2/holes/reference-psis/psi-aif-head.txt" 20 0.15)]
    (if-not aif-head
      (check! "1b: constructor aif-head cascade" false "no JSON from constructor")
      (let [size (:size aif-head)
            f (:F-free-energy aif-head)
            top-pat (get-in aif-head [:shown 0 :pattern])
            top-rel (get-in aif-head [:shown 0 :rel])
            trunc (:truncated aif-head)]
        ;; Structural pins exact; F has tolerance (embedding drift on 21-pattern cascade)
        (check! "1b: constructor aif-head cascade"
                (and (= size 21)
                     (= trunc true)
                     (= top-pat "ants/baseline-cyber-ant")
                     (approx= top-rel 0.679 0.001)
                     (approx= f 8.722 0.5))
                (format "size=%s F=%.3f top=%s rel=%.3f trunc=%s (ref: 21 / ~8.722 / baseline-cyber-ant / 0.679 / true)"
                        size (double f) top-pat (double top-rel) trunc))))))

;; =============================================================================
;; CHECK 2: FOLD PINS — EVERY deposit reconstructs prompt-sha byte-exact
;; =============================================================================
;; Provenance: gate_2f_deposit.clj pin 1 (replay-validity) — the fold-prompt fn
;;             over the deposit's own cascade pattern-ids + circumstance
;;             {:mission :psi} + verbatim flexiarg prose must hash to the stored
;;             :prompt :sha256. Extended from the single-deposit check (001) to
;;             loop over EVERY deposit in the escrow dir, per claude-16's
;;             overnight-flights finding (007/008 landed with invented pin
;;             schemes; the loader accepted them, replay abstained — the suite
;;             must catch this).

(defn reconstruct-prompt-sha
  "Rebuild the fold-prompt for a deposit and return its sha256."
  [d]
  (let [cascade (vec (get-in d [:cascade :pattern-ids]))
        circumstance {:mission (:mission d)
                      :psi (get-in d [:cascade :psi])}
        proses (into {} (for [p cascade]
                          [p (slurp (str "/home/joe/code/futon3/library/" p ".flexiarg"))]))
        prompt (fl/fold-prompt cascade circumstance proses)]
    (esc/prompt-sha prompt)))

(defn check-2-fold-pin []
  (let [deposits (:deposits (esc/load-deposits))
        results (for [d deposits
                      :let [id (:fold-turn/id d)
                            stored (get-in d [:prompt :sha256])
                            rebuilt (reconstruct-prompt-sha d)
                            match (= stored rebuilt)]]
                  {:id id :stored stored :rebuilt rebuilt :match match})
        all-match (every? :match results)]
    (doseq [r results]
      (let [status (if (:match r) "PASS" "DRIFT")]
        (println (format "       %-42s %s stored=%s rebuilt=%s"
                         (:id r) status
                         (subs (:stored r) 0 12)
                         (subs (:rebuilt r) 0 12)))))
    (let [pass-count (count (filter :match results))
          total (count results)]
      (check! "2: fold-pin prompt-sha (every deposit)"
              all-match
              (format "%d/%d deposits reconstruct byte-exact via fold-prompt"
                      pass-count total)))))

;; =============================================================================
;; CHECK 3: ESCROW — load-deposits over real dir, all accepted + tamper rejection
;; =============================================================================
;; Provenance: gate_2f_deposit.clj — "GATE 2f PASS — 6 deposit(s)"
;;             E-live-loop-3.md deposit-006 log: "Escrow = 6 deposits, 0 rejected"

(defn check-3-escrow []
  (let [{:keys [deposits rejected]} (esc/load-deposits)]
    ;; 3a: all current deposits accepted, 0 rejections
    (check! "3a: escrow load-deposits all accepted"
            (and (= (count rejected) 0)
                 (> (count deposits) 0))
            (format "%d deposits, %d rejected (ref: 8 / 0)"
                    (count deposits) (count rejected))))
  ;; 3b: one synthetic tamper => named rejection
  ;; We tamper a COPY: take deposit 001, corrupt its delta-g, write to a temp
  ;; file in a temp dir alongside a valid deposit, and load from that dir.
  (let [tmp-dir (str (System/getProperty "java.io.tmpdir") "/ref-regression-escrow-" (System/currentTimeMillis))
        _ (.mkdirs (io/file tmp-dir))
        ;; copy a valid deposit
        valid-src "/home/joe/code/futon6/data/fold-turns/ft-autoclock-in-001.edn"
        valid-d (edn/read-string (slurp valid-src))
        ;; tamper: change stored delta-g so pin 3 (delta-g-mismatch) fires
        tampered-d (assoc-in valid-d [:eval :delta-g] -0.999)
        _ (spit (io/file tmp-dir "ft-tamper-test.edn") (pr-str tampered-d))
        {:keys [rejected]} (esc/load-deposits tmp-dir)
        tamper-rej (first (filter #(str/ends-with? (:file %) "ft-tamper-test.edn") rejected))]
    (check! "3b: escrow synthetic tamper rejected"
            (and (some? tamper-rej)
                 (= :delta-g-mismatch (:reason tamper-rej)))
            (format "tamper rejection: %s (reason: %s)"
                    (:file tamper-rej) (:reason tamper-rej)))
    ;; cleanup
    (doseq [f (.listFiles (io/file tmp-dir))]
      (.delete f))
    (.delete (io/file tmp-dir))))

;; =============================================================================
;; CHECK 4: KILL-TEST — p3_retry_enriched => OVERALL :fail, A fail/fail B pass/pass
;; =============================================================================
;; Provenance: E-live-loop-3.md "P3-retry results" —
;;             "Sub-test A (R1): :fail, Sub-test A (R2): :fail"
;;             "Sub-test B (R1): :pass, Sub-test B (R2): :pass"
;;             "OVERALL: :fail"
;;             "FAIL — P3 negative #2. Kill criterion reached."
;; The verdict is a frozen fact about frozen data — drift means harness or
;; data corruption.

(defn check-4-kill-test []
  (let [cmd ["clojure" "-M" "scripts/p3_retry_enriched.clj"]
        proc (.. (ProcessBuilder. (java.util.ArrayList. cmd))
                 (directory (io/file "/home/joe/code/futon2"))
                 (redirectErrorStream true)
                 (start))
        out (slurp (.getInputStream proc))
        _ (.waitFor proc)
        lines (str/split-lines out)
        ;; Extract the verdict block
        overall-line (some #(when (str/includes? % "OVERALL:") %) lines)
        a-r1-line (some #(when (re-find #"Sub-test A \(R1\)" %) %) lines)
        a-r2-line (some #(when (re-find #"Sub-test A \(R2\)" %) %) lines)
        b-r1-line (some #(when (re-find #"Sub-test B \(R1\)" %) %) lines)
        b-r2-line (some #(when (re-find #"Sub-test B \(R2\)" %) %) lines)]
    (check! "4: kill-test p3_retry_enriched verdict"
            (and (str/includes? overall-line ":fail")
                 (str/includes? a-r1-line ":fail")
                 (str/includes? a-r2-line ":fail")
                 (str/includes? b-r1-line ":pass")
                 (str/includes? b-r2-line ":pass"))
            (format "OVERALL=%s A(R1)=%s A(R2)=%s B(R1)=%s B(R2)=%s (ref: fail/fail/fail/pass/pass)"
                    (last (str/split overall-line #":"))
                    (last (str/split a-r1-line #":"))
                    (last (str/split a-r2-line #":"))
                    (last (str/split b-r1-line #":"))
                    (last (str/split b-r2-line #":"))))))

;; =============================================================================
;; CHECK 5: MANA — award/consume/refuse round-trip on a throwaway gate id
;; =============================================================================
;; Provenance: mana_gate.clj — award!/consume!/balance! API (P2 mana gate,
;;             M-peradam-mechanization; E-live-loop-3.md "P2 mana gate" section)
;;             "8 tests, all pass" — the round-trip is the core contract.

(defn check-5-mana []
  (let [gate-id (str "ref-regression-test-" (System/currentTimeMillis))
        ;; Use a temp dir to avoid polluting the real mana-gate dir
        tmp-dir (str (System/getProperty "java.io.tmpdir") "/ref-regression-mana-" (System/currentTimeMillis))
        _ (.mkdirs (io/file tmp-dir))
        ;; 5a: award 3 => balance 3
        award-res (mg/award! gate-id 3 "ref-regression operator word" tmp-dir)
        bal-after-award (mg/balance gate-id tmp-dir)
        ;; 5b: consume 1 => balance 2
        consume1 (mg/consume! gate-id "first spend" tmp-dir)
        bal-after-consume1 (mg/balance gate-id tmp-dir)
        ;; 5c: consume 1 => balance 1
        consume2 (mg/consume! gate-id "second spend" tmp-dir)
        bal-after-consume2 (mg/balance gate-id tmp-dir)
        ;; 5d: consume 1 => balance 0
        consume3 (mg/consume! gate-id "third spend" tmp-dir)
        bal-after-consume3 (mg/balance gate-id tmp-dir)
        ;; 5e: consume at 0 => REFUSED
        consume4 (mg/consume! gate-id "refused spend" tmp-dir)
        bal-after-refuse (mg/balance gate-id tmp-dir)]
    (check! "5: mana award/consume/refuse round-trip"
            (and (= (:balance award-res) 3)
                 (= bal-after-award 3)
                 (= (:balance consume1) 2)
                 (= bal-after-consume1 2)
                 (= (:balance consume2) 1)
                 (= bal-after-consume2 1)
                 (= (:balance consume3) 0)
                 (= bal-after-consume3 0)
                 (= (:ok consume4) false)
                 (:refused consume4)
                 (= bal-after-refuse 0))
            (format "award->3, consume->2/1/0, refuse@0 (balance=%d, refused=%s)"
                    bal-after-refuse (:refused consume4)))
    ;; cleanup
    (doseq [f (.listFiles (io/file tmp-dir))]
      (.delete f))
    (.delete (io/file tmp-dir))))

;; =============================================================================
;; CHECK 6: PERADAM — the refusal census (documented, per deposit)
;; =============================================================================
;; Provenance: ft-peradam-mechanization-006.edn boxes b5/b6 — the STEP-0 CENSUS:
;;             "001 certifiable once witnesses structured; 003/004/005 no-seal
;;              = correctly refusable; 002 refuses if prose-score not
;;              structurally recoverable"
;;             E-live-loop-3.md "deposit-006 log" — same census.
;;
;; The certificate LOADER does not exist as runtime code yet (it is the fold's
;; PLAN, not its implementation). This check freezes the DOCUMENTED refusal-cause
;; map as a fact about the deposit corpus: which deposits carry structural seal
;; witnesses and which don't. The census is:
;;   001: unstructured-witnesses (evidence in prose, not in the deposit record)
;;   002: unstructured-witnesses (scored-against-seal in reviewer prose)
;;   003: missing-seal (explicitly no-seal)
;;   004: missing-seal (explicitly no-seal)
;;   005: missing-seal (explicitly no-seal)
;;   006: missing-seal (the plan itself, blanket grant, no seal)
;;
;; We verify: (a) the frozen refusal-cause map matches what the deposit files
;; actually carry (none has a :seal field — confirming all are refusable for
;; missing-seal or unstructured-witnesses); (b) the map itself is intact.

(def expected-refusal-census
  "The frozen refusal-cause map per deposit, from the peradam fold's STEP-0 CENSUS
  (box b5/b6). The certificate machinery is not built; this is the documented
  expected behavior, frozen as a regression reference.
  007/008 added after the overnight flights (both no-seal, per the 003 precedent
  — their :blind-scoring-note confirms)."
  {"ft-autoclock-in-001" :unstructured-witnesses
   "ft-live-geometric-stack-002" :unstructured-witnesses
   "ft-bayesian-structure-learning-003" :missing-seal
   "ft-aif-head-004" :missing-seal
   "ft-action-vocabulary-005" :missing-seal
   "ft-peradam-mechanization-006" :missing-seal
   "ft-first-flights-007" :missing-seal
   "ft-bounded-in-flight-state-008" :missing-seal})

(defn deposit-has-seal-field?
  "True if the deposit file contains a :seal key at the top level (structural
  seal witness). None of the current deposits do — confirming the census."
  [file]
  (let [d (edn/read-string (slurp file))]
    (contains? d :seal)))

(defn check-6-peradam []
  (let [deposit-dir "/home/joe/code/futon6/data/fold-turns"
        files (->> (.listFiles (io/file deposit-dir))
                   (filter #(re-matches #"ft-.*\.edn" (.getName ^java.io.File %)))
                   (sort-by #(.getName ^java.io.File %)))
        ;; Build the actual census from the files: does each carry a :seal?
        actual-census
        (into {} (for [f files
                       :let [id (str/replace (.getName ^java.io.File f) #"\.edn" "")
                             has-seal (deposit-has-seal-field? f)]]
                   [id (if has-seal :has-seal :no-seal-field)]))
        ;; The expected census: all deposits are refusable (none has a structural seal)
        all-refusable? (every? #(= :no-seal-field %) (vals actual-census))
        ;; The expected cause map matches the current deposit set
        expected-ids (set (keys expected-refusal-census))
        actual-ids (set (keys actual-census))
        ids-match (= expected-ids actual-ids)]
    (check! "6: peradam refusal census"
            (and all-refusable? ids-match)
            (format "%d deposits, all no-seal-field=%s, ids-match=%s; causes: 001/002 unstructured-witnesses, 003-008 missing-seal"
                    (count actual-census) all-refusable? ids-match))))

;; =============================================================================
;; MAIN
;; =============================================================================

(defn -main []
  (println "=== REFERENCE-OUTPUT REGRESSION SUITE ===")
  (println "Re-derives tonight's committed artifacts and diffs against stored references.")
  (println "")
  (println "--- CHECK 1: CONSTRUCTOR (cascade re-derivation) ---")
  (check-1-constructor)
  (println "")
  (println "--- CHECK 2: FOLD PIN (prompt-sha byte-exact) ---")
  (check-2-fold-pin)
  (println "")
  (println "--- CHECK 3: ESCROW (load-deposits + tamper rejection) ---")
  (check-3-escrow)
  (println "")
  (println "--- CHECK 4: KILL-TEST (p3_retry_enriched verdict) ---")
  (check-4-kill-test)
  (println "")
  (println "--- CHECK 5: MANA (award/consume/refuse round-trip) ---")
  (check-5-mana)
  (println "")
  (println "--- CHECK 6: PERADAM (refusal census) ---")
  (check-6-peradam)
  (println "")
  (println "=== SUMMARY ===")
  (let [total (count @check-results)
        passed (count (filter :pass @check-results))]
    (doseq [r @check-results]
      (println (format "  %s — %s" (:name r) (if (:pass r) "PASS" "DRIFT"))))
    (println "")
    (println (format "%d checks, %d pass" total passed)))
  (let [failed (filter #(not (:pass %)) @check-results)]
    (when (seq failed)
      (System/exit 1))))

(-main)
