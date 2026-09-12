#!/usr/bin/env bb
(ns witnesses.merge-witnesses-control
  (:require [clojure.java.io :as io] [clojure.java.shell :as sh]
            [clojure.edn :as edn] [witnesses.node-witness :as n]
            [witnesses.node-witness-test :as t]))

;; Run from futon2: bb -cp scripts:test test/witnesses/merge_witnesses_control.bb
;; All generated output is disposable. Synthetic receipts test the admission
;; mechanism only; the genuine F8 fragments must remain pending.
(defn check! [label ok data]
  (prn {:control label :pass? (boolean ok) :evidence data})
  (when-not ok (throw (ex-info "Control failed" {:control label}))))
(defn temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "witness-merge-control-" (make-array java.nio.file.attribute.FileAttribute 0))))
(defn invoke [args]
  (apply sh/sh "bb" "scripts/merge_witnesses.bb" args))
(defn write-fragment! [dir w]
  (spit (io/file dir "machineObservation.edn")
        (pr-str {:witnesses "machineObservation" :node-witnesses [w]})))

(let [dir (temp-dir) output (str dir "/output.edn") fragments (io/file dir "fragments")]
  (.mkdir fragments)
  (doseq [decl ["machineObservation" "machinePrecision" "machineChannelPredictionError"]]
    (io/copy (io/file (str "checks/witness-fragments/" decl ".edn"))
             (io/file fragments (str decl ".edn"))))
  (let [args ["--fragments" (str fragments) "--output" output]
        a (invoke args) first-output (when (zero? (:exit a)) (slurp output))
        b (invoke args) second-output (when (zero? (:exit b)) (slurp output))
        entries (when second-output (edn/read-string second-output))]
    (let [ctx (n/context {"futon2" "/home/joe/code/futon2" "mathlib4" "/home/joe/code/mathlib4"})]
      (doseq [w (mapcat :node-witnesses entries)]
        (doseq [loc (concat [(:artifact w) (:subject-artifact w)] (vals (:retained-evidence w)))]
          (n/resolve! ctx loc (:id w))))
      (doseq [comparison (distinct (mapcat :pin-comparisons (mapcat :node-witnesses entries)))]
        (let [actual (:sha256 (n/read-path! ctx (:repo comparison) (:path comparison) :pin-audit))]
          (check! :credit-pin-audit
                  (and (= actual (:current-sha256 comparison))
                       (= (:match? comparison) (= actual (:retained-census-sha256 comparison))))
                  comparison))))
    (check! :genuine-deterministic-merge
            (and (= 0 (:exit a) (:exit b)) (= first-output second-output)
                 (= 24 (count (mapcat :node-witnesses entries)))
                 (every? #(= :proposed (:status %)) (mapcat :node-witnesses entries)))
            {:first a :second b :output-sha256 (when second-output (n/sha256 (.getBytes second-output "UTF-8")))})
    (check! :check-roundtrip (zero? (:exit (invoke (conj args "--check")))) nil)
    (let [split (io/file dir "split") _ (.mkdir split)
          result (invoke ["--split" "--fragments" (str split) "--output" output])]
      (check! :split-preserves-fields
              (and (zero? (:exit result))
                   (= (set entries) (set (map #(edn/read-string (slurp %)) (.listFiles split))))) result))))

(doseq [mode [:stale-bytes :missing-declaration :nil-receipt :missing-readback :id-reuse]]
  (let [f (t/fixture) dir (io/file (:root f) "fragments") _ (.mkdir dir)
        output (str (:root f) "/merged.edn")
        auth (str (:root f) "/authorities.edn")
        _ (spit auth (pr-str (:authorities f)))
        args ["--fragments" (str dir) "--output" output "--authorities" auth]
        _ (write-fragment! dir (:w f))
        positive (invoke args)
        before (when (zero? (:exit positive)) (slurp output))
        _ (spit (io/file (:root f) "fundamentals.edn") "{:schema :changed-control-authority}")
        stale-check (invoke (conj args "--check"))
        _ (check! :dependency-digest-change (= 1 (:exit stale-check)) stale-check)
        _ (spit (io/file (:root f) "fundamentals.edn") (pr-str {:schema :test-cap-authority}))
        w (case mode
            :id-reuse (t/endorsed f (assoc (:w f) :claim "A different semantic claim.") (:receipt f))
            :stale-bytes (do (spit (io/file (:root f) "Witness.lean") " " :append true) (:w f))
            :missing-declaration
            (let [w (assoc-in (:w f) [:artifact :declaration] "DarkTower.WarMachine.MachineObservationWitness.onlyInComment")]
              (t/endorsed f w (assoc (:receipt f) :dependencies [(:artifact w) (:subject-artifact w)])))
            :nil-receipt (assoc-in (:w f) [:verification :receipt] nil)
            :missing-readback (do (.delete (io/file (:root f) "transcript.txt")) (:w f)))
        _ (write-fragment! dir w)
        rejected (invoke args)
        err (edn/read-string (:err rejected))
        expected (case mode :stale-bytes :node-witness-pin-mismatch
                       :missing-declaration :node-witness-declaration-missing
                       :nil-receipt :node-witness-verification-missing
                       :missing-readback :node-witness-artifact-missing
                       :id-reuse :node-witness-id-revision-required)]
    (check! mode (and (= 0 (:exit positive)) (= 1 (:exit rejected))
                      (= expected (:error err)) (= before (slurp output)))
            {:positive-exit (:exit positive) :refusal rejected :prior-output-unchanged? (= before (slurp output))})
    (when-not (= :id-reuse mode)
      (.delete (io/file output))
      (let [again (invoke args)]
      (check! (keyword (str (name mode) "-no-new-output"))
              (and (= 1 (:exit again)) (not (.exists (io/file output))))
              {:exit (:exit again)})))))
