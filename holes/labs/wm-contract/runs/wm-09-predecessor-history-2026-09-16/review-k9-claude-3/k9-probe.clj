;; claude-3 K9 review controls for 8d5b569d. Temporary stores only; author fixtures reused.
(require '[clojure.java.io :as io] '[clojure.string]
         '[futon2.aif.receipt-construction :as construction]
         '[futon2.aif.receipt-construction-test :as t])
(import '[java.nio.file Files] '[java.nio.file.attribute FileAttribute])

(defn temp [] (.toFile (Files/createTempDirectory "c3-k9" (make-array FileAttribute 0))))
(defn rm! [d] (doseq [f (reverse (file-seq d))] (Files/delete (.toPath f))))
(defn run [f]
  (try (let [r (f)] {:admission-reason (:admission-reason r)
                     :excluded (mapv :reason (get-in r [:provenance :excluded-attempts]))
                     :auxiliary (mapv :reason (get-in r [:provenance :auxiliary-exclusions]))})
       (catch clojure.lang.ExceptionInfo e
         (let [d (ex-data e)] {:refusal (:construction/refusal d) :reason (:reason d)
                               :file (some-> (:file d) str (clojure.string/replace #"^/tmp/c3-k9[0-9]+" "TMP"))}))))
(defn copy-attempt! [files dest-dir]
  (.mkdirs dest-dir)
  (doseq [f files] (spit (io/file dest-dir (.getName f)) (slurp f))))
(defn case! [label f] (let [d (temp)] (try (prn {:case label :result (f d)}) (finally (rm! d)))))
(def current {:occurrence {:action/value {:target "M-history"} :action-at "2026-09-15T12:00:00Z"}})

;; C1: identical copy of an UNRELATED attempt (live + archive) beside a valid matching predecessor.
;; K9 K8-a retains ambiguity for the same target; for another target the copies should not block.
(case! :identical-unrelated-copy-with-valid-matching
  (fn [d]
    (t/history-fixture! (io/file d "valid") :valid "2026-09-15T10:00:00Z" "2026-09-15T10:01:00Z")
    (let [files (t/discovery-history! (io/file d "live") "M-other" false)]
      (copy-attempt! files (io/file d "live" "archives" "snapshot" "fixture" "attempt-002")))
    (run #(construction/previous! current [(io/file d "valid") (io/file d "live")]))))

;; C2: identical copy of an unrelated attempt, fresh requested target, no other history.
(case! :identical-unrelated-copy-fresh-target
  (fn [d]
    (let [files (t/discovery-history! (io/file d "live") "M-other" false)]
      (copy-attempt! files (io/file d "live" "archives" "snapshot" "fixture" "attempt-002")))
    (run #(construction/previous! current [(io/file d "live")]))))

;; C3: identical copy of a MATCHING attempt (same target): K8-a ambiguity/unresolved refusal expected.
(case! :identical-matching-copy
  (fn [d]
    (let [files (t/discovery-history! (io/file d "live") "M-history" false)]
      (copy-attempt! files (io/file d "live" "archives" "snapshot" "fixture" "attempt-002")))
    (run #(construction/previous! current [(io/file d "live")]))))

;; C4: same identity, different content (K8-b collision) for an unrelated target: refusal expected.
(case! :differing-reused-identity-unrelated
  (fn [d]
    (let [files (t/discovery-history! (io/file d "live") "M-other" false)
          dest (io/file d "live" "archives" "snapshot" "fixture" "attempt-002")]
      (copy-attempt! files dest)
      (t/rewrite-history! (io/file dest "001-time-step.edn") #(assoc-in % [:payload :ground :annotation] :different)))
    (run #(construction/previous! current [(io/file d "live")]))))

;; C5: overlapping roots (parent and archive child) over a single attempt: no artificial duplicate.
(case! :overlapping-roots-single-record
  (fn [d]
    (t/discovery-history! (io/file d "live" "archives" "snapshot") "M-other" false)
    (run #(construction/previous! current [(io/file d "live") (io/file d "live" "archives" "snapshot")]))))

;; C6: archive-group auxiliary subtree holding REGULAR files named attempt-NNN.edn (retained derived-projections shape).
(case! :auxiliary-regular-files-named-attempt
  (fn [d]
    (let [items (io/file d "live" "archives" "snapshot" "derived-projections" "morning-brief-items")]
      (.mkdirs items)
      (spit (io/file items "attempt-013.edn") "{:note :projection}")
      (spit (io/file d "live" "archives" "snapshot" "derived-projections" "failed-wm-traces.edn") "{}"))
    (t/discovery-history! (io/file d "live" "archives" "snapshot") "M-other" false)
    (run #(construction/previous! current [(io/file d "live")]))))

;; C7: same auxiliary subtree with a non-attempt file name: exclusion expected (control for C6).
(case! :auxiliary-regular-files-other-name
  (fn [d]
    (let [items (io/file d "live" "archives" "snapshot" "derived-projections" "morning-brief-items")]
      (.mkdirs items)
      (spit (io/file items "item-013.edn") "{:note :projection}"))
    (t/discovery-history! (io/file d "live" "archives" "snapshot") "M-other" false)
    (run #(construction/previous! current [(io/file d "live")]))))

;; C8: attempt evidence subdirectory containing a regular file named attempt-note.edn.
(case! :attempt-evidence-file-named-attempt
  (fn [d]
    (let [files (t/discovery-history! (io/file d "live") "M-other" false)
          ev (io/file (.getParentFile (first files)) "evidence")]
      (.mkdirs ev)
      (spit (io/file ev "attempt-identity.edn") "{}"))
    (run #(construction/previous! current [(io/file d "live")]))))
(shutdown-agents)
