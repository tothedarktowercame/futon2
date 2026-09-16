;; claude-3 consolidated-review controls for ab0b56f2, beyond the author's namespace.
;; Temporary stores only; reuses the author's producer-shaped fixtures.
(require '[clojure.java.io :as io]
         '[futon2.aif.receipt-construction :as construction]
         '[futon2.aif.receipt-construction-test :as t]
         '[futon2.aif.interpretation-evidence :as evidence]
         '[futon2.aif.interpretation-job :as job]
         '[futon2.aif.full-loop-runner :as runner])
(import '[java.nio.file Files] '[java.nio.file.attribute FileAttribute])

(defn temp [] (.toFile (Files/createTempDirectory "c3-wm09-consolidated" (make-array FileAttribute 0))))
(defn rm! [d] (doseq [f (reverse (file-seq d))] (Files/delete (.toPath f))))
(def current {:occurrence {:action/value {:target "M-history"} :action-at "2026-09-15T12:00:00Z"}})
(defn run [f]
  (try (let [r (f)] {:admission-reason (:admission-reason r)
                     :excluded (mapv :reason (get-in r [:provenance :excluded-attempts]))})
       (catch clojure.lang.ExceptionInfo e (let [d (ex-data e)] {:refusal (:construction/refusal d) :reason (:reason d) ::e e}))
       (catch Throwable e {:throwable (.getName (class e)) ::e e})))
(defn case! [label f]
  (let [d (temp)]
    (try (let [r (f d)] (prn {:case label :result (dissoc r ::e)}) r)
         (finally (rm! d)))))

;; G1: retained attempt-036 shape: not-reached, selection of an action type with no :target.
(case! :not-reached-selection-without-target-alongside-valid
  (fn [d]
    (t/history-fixture! (io/file d "old") :older "2026-09-15T10:00:00Z" "2026-09-15T10:01:00Z")
    (let [files (t/non-construction-fixture! (io/file d "marker") true false)]
      (t/rewrite-history! (nth files 1)
                          #(assoc % :payload {:judgment {:selected-action {:type :learn-action-class :target-class :survey-mission}
                                                         :selected-mission ":survey-mission"}
                                              :ground {:kind :wm-judgement}})))
    (run #(construction/previous! current [(io/file d "old") (io/file d "marker")]))))

;; G2: retained attempt-053 shape: all checkpoints not-reached, close ground hand-written witness evidence.
(case! :not-reached-with-non-producer-close-ground
  (fn [d]
    (let [files (t/non-construction-fixture! (io/file d "marker") false false)]
      (t/rewrite-history! (last files) #(assoc-in % [:payload :ground] {:witness "claude-4" :evidence ["manual"]})))
    (run #(construction/previous! current [(io/file d "marker")]))))

;; G5: unrelated attempt with its construction file deleted.
(case! :unrelated-missing-construction-file
  (fn [d]
    (let [files (t/discovery-history! (io/file d "other") "M-other" false)]
      (Files/delete (.toPath (nth files 2))))
    (run #(construction/previous! current [(io/file d "other")]))))

;; G6: unrelated attempt whose selection target disagrees with construction :mission.
(case! :unrelated-internal-target-conflict
  (fn [d]
    (let [files (t/discovery-history! (io/file d "other") "M-other" false)]
      (t/rewrite-history! (nth files 2) #(assoc-in % [:payload :judgment :mission] "M-third")))
    (run #(construction/previous! current [(io/file d "other")]))))

;; G7: colonless requested target versus keyword history (K6: does not agree).
(case! :colonless-request-vs-keyword-history
  (fn [d]
    (t/discovery-history! (io/file d "h") :ns/target false)
    (run #(construction/previous! {:occurrence {:action/value {:target "ns/target"} :action-at "2026-09-15T12:00:00Z"}}
                                  [(io/file d "h")]))))

;; G8: unrelated attempt whose construction is recorded after its close.
(case! :unrelated-construction-after-close
  (fn [d]
    (let [files (t/discovery-history! (io/file d "other") "M-other" false)]
      (t/rewrite-history! (nth files 2) #(assoc % :recorded-at "2026-09-15T11:59:00Z")))
    (run #(construction/previous! current [(io/file d "other")]))))

;; G9: unrelated rejected-construction sorry with an extra key.
(case! :unrelated-rejected-construction-extra-key
  (fn [d]
    (let [files (t/discovery-history! (io/file d "other") "M-other" true)]
      (t/rewrite-history! (nth files 2) #(assoc-in % [:payload :sorry :extra] true)))
    (run #(construction/previous! current [(io/file d "other")]))))

;; G10: K4 matching rejection over an older VALID predecessor: provenance, no fallback.
(case! :matching-rejected-over-valid
  (fn [d]
    (t/history-fixture! (io/file d "old") :older "2026-09-15T10:00:00Z" "2026-09-15T10:01:00Z")
    (let [files (t/discovery-history! (io/file d "latest") "M-history" true)
          r (run #(construction/previous! current [(io/file d "old") (io/file d "latest")]))
          ed (some-> r ::e ex-data)]
      (assoc r :rejection (:history/rejection ed)
               :sha-matches? (= (:history/construction-sha256 ed) (evidence/sha256 (Files/readAllBytes (.toPath (nth files 2)))))
               :target-evidence-count (count (:history/target-evidence ed))
               :requested-target (:history/requested-target ed)))))

;; G11: code fault inside relevance evidence (NPE from digest) stays a machine failure at the pinned caller.
(let [d (temp)]
  (try
    (t/discovery-history! (io/file d "other") "M-other" false)
    (let [r (with-redefs [evidence/sha256 (fn [_] (throw (NullPointerException. "digest code fault")))]
              (run #(construction/previous! current [(io/file d "other")])))
          e (::e r)
          {:keys [kind failure-kind]} (@#'job/failure-classification e @#'runner/transport-failure-kind)]
      (prn {:case :npe-inside-relevance-evidence :thrown (or (:throwable r) (:refusal r))
            :job-kind kind :failure-kind failure-kind :repair-class (#'runner/repair-class-for failure-kind)}))
    (finally (rm! d))))
(shutdown-agents)
