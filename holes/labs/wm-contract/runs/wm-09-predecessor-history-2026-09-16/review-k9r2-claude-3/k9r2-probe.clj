;; claude-3 K9R2 review controls for 9d7ab9a5. Temporary stores only; author fixtures reused.
(require '[clojure.java.io :as io] '[clojure.string]
         '[futon2.aif.receipt-construction :as construction]
         '[futon2.aif.receipt-construction-test :as t]
         '[futon2.aif.evidence-manifest :as manifest])
(import '[java.nio.file Files] '[java.nio.file.attribute FileAttribute])

(defn temp [] (.toFile (Files/createTempDirectory "c3-k9r2" (make-array FileAttribute 0))))
(defn rm! [d] (doseq [f (reverse (file-seq d))] (Files/delete (.toPath f))))
(defn run [f]
  (try (let [r (f)] {:admission-reason (:admission-reason r)
                     :carried-epoch (get-in r [:provenance :identity :semantic-epoch])
                     :excluded (mapv :reason (get-in r [:provenance :excluded-attempts]))
                     :auxiliary (mapv :reason (get-in r [:provenance :auxiliary-exclusions]))})
       (catch clojure.lang.ExceptionInfo e
         (let [d (ex-data e)] {:refusal (:construction/refusal d) :reason (:reason d)
                               :identical? (:identical-checkpoint-sets? d)
                               :paths (count (or (:files d) (:records d)))
                               :file (some-> (:file d) str (clojure.string/replace #"^/tmp/c3-k9r2[0-9]+" "TMP"))}))))
(defn copy-attempt! [files dest-dir]
  (.mkdirs dest-dir)
  (doseq [f files] (spit (io/file dest-dir (.getName f)) (slurp f))))
(defn archive-of [root] (io/file root "archives" "snapshot" "fixture" "attempt-002"))
(defn case! [label f] (let [d (temp)] (try (prn {:case label :result (f d)}) (finally (rm! d)))))
(defn cur [at] {:occurrence {:action/value {:target "M-history"} :action-at at}})

(case! :c1-identical-unrelated-copy-with-valid-matching
  (fn [d]
    (t/history-fixture! (io/file d "valid") :valid "2026-09-15T10:00:00Z" "2026-09-15T10:01:00Z")
    (copy-attempt! (t/discovery-history! (io/file d "live") "M-other" false) (archive-of (io/file d "live")))
    (run #(construction/previous! (cur "2026-09-15T12:00:00Z") [(io/file d "valid") (io/file d "live")]))))

(case! :c2-identical-unrelated-copy-fresh-target
  (fn [d]
    (copy-attempt! (t/discovery-history! (io/file d "live") "M-other" false) (archive-of (io/file d "live")))
    (run #(construction/previous! (cur "2026-09-15T12:00:00Z") [(io/file d "live")]))))

(case! :c3-identical-matching-copy-latest
  (fn [d]
    (copy-attempt! (t/discovery-history! (io/file d "live") "M-history" false) (archive-of (io/file d "live")))
    (run #(construction/previous! (cur "2026-09-15T12:00:00Z") [(io/file d "live")]))))

(case! :c4-differing-reused-identity-unrelated
  (fn [d]
    (let [dest (archive-of (io/file d "live"))]
      (copy-attempt! (t/discovery-history! (io/file d "live") "M-other" false) dest)
      (t/rewrite-history! (io/file dest "001-time-step.edn") #(assoc-in % [:payload :ground :annotation] :different)))
    (run #(construction/previous! (cur "2026-09-15T12:00:00Z") [(io/file d "live")]))))

(case! :c6-auxiliary-regular-files-named-attempt
  (fn [d]
    (let [items (io/file d "live" "archives" "snapshot" "derived-projections" "morning-brief-items")]
      (.mkdirs items)
      (spit (io/file items "attempt-013.edn") "{:note :projection}"))
    (t/discovery-history! (io/file d "live" "archives" "snapshot") "M-other" false)
    (run #(construction/previous! (cur "2026-09-15T12:00:00Z") [(io/file d "live")]))))

(case! :c8-attempt-evidence-file-named-attempt
  (fn [d]
    (let [files (t/discovery-history! (io/file d "live") "M-other" false)
          ev (io/file (.getParentFile (first files)) "evidence")]
      (.mkdirs ev)
      (spit (io/file ev "attempt-identity.edn") "{}"))
    (run #(construction/previous! (cur "2026-09-15T12:00:00Z") [(io/file d "live")]))))

(case! :c9-older-identical-matching-pair-then-newer-valid
  (fn [d]
    (copy-attempt! (t/discovery-history! (io/file d "live") "M-history" false) (archive-of (io/file d "live")))
    (t/history-fixture! (io/file d "valid") :newer "2026-09-15T11:30:00Z" "2026-09-15T11:31:00Z")
    (run #(construction/previous! (cur "2026-09-15T12:00:00Z") [(io/file d "live") (io/file d "valid")]))))

(case! :c10-future-identical-matching-pair-over-older-valid
  (fn [d]
    (t/history-fixture! (io/file d "valid") :older "2026-09-15T10:00:00Z" "2026-09-15T10:01:00Z")
    (copy-attempt! (t/discovery-history! (io/file d "live") "M-history" false) (archive-of (io/file d "live")))
    (run #(construction/previous! (cur "2026-09-15T11:05:00Z") [(io/file d "valid") (io/file d "live")]))))

(case! :c11-identical-copy-of-manifested-not-reached-attempt
  (fn [d]
    (copy-attempt! (t/non-construction-fixture! (io/file d "live") true true) (archive-of (io/file d "live")))
    (run #(construction/previous! (cur "2026-09-15T12:00:00Z") [(io/file d "live")]))))

(case! :c12-identical-copy-of-manifested-unrelated-construction
  (fn [d]
    (let [files (t/non-construction-fixture! (io/file d "live") true true)
          action {:type :advance-mission :target "M-other"}]
      (t/rewrite-history! (nth files 1) #(assoc % :payload {:judgment {:selected-action action :selected-mission "M-other"} :ground {:kind :fixture-selection}}))
      (t/rewrite-history! (nth files 2) #(assoc % :payload {:judgment {:mission "M-other" :cascade {:construction-kind :selected-policy}}
                                                            :ground {:kind :decision-pinned-construction :selected-action action}}))
      (t/rewrite-history! (last files)
                          (fn [closed]
                            (-> closed
                                (assoc-in [:payload :close-retention :occurrence :action/value] action)
                                (assoc-in [:payload :close-evidence-manifest]
                                          (manifest/build-manifest {:entries (mapv #(select-keys % [:evidence/id :source-path :admitted-at])
                                                                                   (get-in closed [:payload :close-evidence-manifest :entries]))
                                                                    :read-bytes #(Files/readAllBytes (.toPath (io/file %)))})))))
      {:original-alone (run #(construction/previous! (cur "2026-09-15T12:00:00Z") [(io/file d "live")]))
       :with-identical-copy (do (copy-attempt! files (archive-of (io/file d "live")))
                                (run #(construction/previous! (cur "2026-09-15T12:00:00Z") [(io/file d "live")])))})))
(shutdown-agents)
