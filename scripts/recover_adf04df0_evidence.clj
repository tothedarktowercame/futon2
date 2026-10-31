;; Recover injury adf04df0's exact delivery-failure check input from the
;; durable Agency invoke-jobs journal, verify every linkage, and retain it
;; content-addressed. Run from the repo root (see recover-adf04df0-evidence.sh).
;;
;; Linkage chain verified here, every step typed:
;;   journal job 8523 (wm-author, caller wm-full-loop, final reply)
;;     -> answer JSON {requisition-id, cascade-digest, patch{...commit}}
;;     -> cascade-digest + requisition-id appear in the pinned tick run
;;        record ba359b5e… (the injury's source record pin)
;;        CORRECTED 2026-10-31: the tick record pins the job-id and the
;;        SELECTED canonical cascade fcbc11a5… (selection-certificate
;;        level); the answer's :cascade-digest 5a0fa2ade… is the
;;        REQUISITION-level cascade and is pinned instead by repair finding
;;        repair-ea1-19929445…attempt-002, which independently records the
;;        job, both digests' requisition id, and the abbreviated + full
;;        commit. Both links are asserted below.
;;     -> patch.source-digest == sha256 of the authored file at the FULL
;;        commit resolved from the abbreviated descriptor, and that commit is
;;        an ancestor of canonical HEAD
;; Nothing historical is rewritten; outputs are new files only.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.java.shell :as shell]
         '[clojure.string :as str]
         '[cheshire.core :as json]
         '[futon2.aif.load-identity :as load-identity]
         '[futon2.aif.wm.delivery-failure-evidence :as dfe])

(def journal-path (or (System/getenv "FUTON3C_INVOKE_JOURNAL")
                      "/tmp/futon3c-invoke-jobs.edn.journal"))
(def job-id "invoke-1793409156227-8523-78848068")
(def tick-record "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-31-f805ca4b-6c35-45cf-accc-f95dd54b0fc8.edn")
(def tick-sha "ba359b5e9f5d6b5a56adaa6f5c23339738110350bc13eb55647f3cdf33b81bea")
(def repo "/home/joe/code/futon2")
;; Recovery output root: canonical inputs above stay read-only; the retained
;; record and proposal are NEW files only. Default to the worktree's own
;; report area so nothing lands in the canonical data tree unreviewed.
(def store-root (or (System/getenv "ADF04DF0_EVIDENCE_ROOT")
                    "data/wm-delivery-failure-evidence-recovery/"))

(defn- latest-job-record []
  ;; The journal is line-delimited EDN upserts; the last line upserting the
  ;; full job map for job-id is the authoritative final state.
  (with-open [r (io/reader journal-path)]
    (loop [line (.readLine ^java.io.BufferedReader r) last nil]
      (if (nil? line)
        (get-in last [:upsert job-id])
        (recur (.readLine ^java.io.BufferedReader r)
               (if (str/includes? line (str "\"" job-id "\""))
                 (try (edn/read-string {:default (fn [_ v] v)} line)
                      (catch Exception _ last))
                 last))))))

(defn- final-reply [job]
  ;; :result-text is the authoritative full reply retained on the job record;
  ;; event :text previews are bounded and may end "[trimmed]".
  (or (when (and (string? (:result-text job))
                 (str/includes? (:result-text job) "requisition-id"))
        (:result-text job))
      (->> (:events job)
           (filter #(= "text" (:type %)))
           (map :text)
           (filter #(str/includes? % "requisition-id"))
           last)))

(defn- git [& args]
  (let [{:keys [exit out err]}
        (apply shell/sh "git" "-C" repo args)]
    (if (zero? exit) out
        (throw (ex-info "git failed" {:args args :err err})))))

(defn- git-trim [& args] (str/trim (apply git args)))

(let [job (latest-job-record)]
  (assert job "job record not found in journal")
  (assert (= "wm-author" (:agent-id job)) "job is not wm-author")
  (assert (= "wm-full-loop" (:caller job)) "job caller mismatch")
  (let [reply (final-reply job)]
    (assert reply "final author reply not retained")
    (let [answer (json/parse-string
                  (or (some-> (re-find #"(?s)```json\s*(\{.*?\})\s*```" reply) second)
                      (throw (ex-info "author reply carries no fenced JSON answer"
                                      {:reply-preview (subs reply 0 (min 200 (count reply)))})))
                  true)
          _ (assert (= "6a8fe374958818d6246ee46acc6dbdb851c7cdf66930626395572519879d195c"
                       (:requisition-id answer)))
          _ (assert (= "5a0fa2adeb101b6bc644bf26511b22f5b88463ac1ef0490e941fd24b22de6995"
                       (:cascade-digest answer)))
          short (get-in answer [:patch :commit])
          _ (assert (re-matches #"[a-f0-9]{4,39}" short)
                    "descriptor commit is not an abbreviated sha")
          full (git-trim "rev-parse" short)
          _ (assert (re-matches #"[a-f0-9]{40}" full))
          _ (assert (zero? (:exit (shell/sh
                                    "git" "-C" repo "merge-base" "--is-ancestor"
                                    full "HEAD")))
                    "resolved commit is not an ancestor of canonical HEAD")
          rel (str "src/"
                  (-> (get-in answer [:patch :namespace])
                      (str/replace "." "/")
                      (str/replace "-" "_"))
                  ".clj")
          blob (git "show" (str full ":" rel))
          blob-sha (load-identity/sha256 (.getBytes ^String blob "UTF-8"))
          _ (assert (= (get-in answer [:patch :source-digest]) blob-sha)
                    "source-digest does not match the committed bytes")
          tick-bytes (java.nio.file.Files/readAllBytes (.toPath (io/file tick-record)))
          _ (assert (= tick-sha (load-identity/sha256 tick-bytes))
                    "tick record pin mismatch")
          tick (slurp tick-record)
          finding-path "/home/joe/code/futon2/data/wm-repair-obligations/findings/repair-ea1-19929445d661380bc279f9e439dd2808eec422ce3bf96f7efd0c168da7e3b153--attempt-002-author-delivery-wrong-repository.edn"
          finding (slurp finding-path)
          _ (assert (str/includes? tick job-id)
                    "author job-id not present in pinned tick record")
          _ (assert (str/includes? tick "fcbc11a5af649403bf4cb1255d537339a31e4077f707fb73ef5d8656b64555d0")
                    "selected canonical cascade digest not in tick record")
          _ (assert (not (str/includes? tick (:cascade-digest answer)))
                    "answer cascade-digest is requisition-level, not the tick record's selected cascade")
          _ (assert (and (str/includes? finding job-id)
                         ;; the finding pins the requisition via the authored
                         ;; namespace prefix (req-6a8fe374…), not the full id
                         (str/includes? finding "req-6a8fe374")
                         (str/includes? finding (:cascade-digest answer))
                         (str/includes? finding short)
                         (str/includes? finding full))
                    "repair finding does not independently pin the job, requisition, cascade and commit")
          check {:status :error
                 :kind :author-delivery-invalid-descriptor
                 :reason :abbreviated-commit-descriptor
                 :supplied-commit short
                 ;; post-fix classification (c96bd9871) reproduced on the
                 ;; recovered input: check-delivery of this exact answer now.
                 :verified-now
                 (let [r ((requiring-resolve
                            'futon2.aif.wm.cascade-patch-delivery/check-delivery)
                          repo (constantly nil) answer)]
                   (select-keys r [:status :kind :reason :supplied-commit]))}
          pin (dfe/retain-record!
                store-root
                (assoc (dfe/delivery-failure-record
                         {:requisition-id (:requisition-id answer)
                          :cascade-digest (:cascade-digest answer)
                          :author-job job-id
                          :answer answer
                          :check check})
                       ;; recovery provenance: this record was reconstructed
                       ;; from the retained job ledger, not written at the
                       ;; boundary at failure time.
                       :recovery {:source :agency-invoke-jobs-journal
                                  :journal-path journal-path
                                  :job-id job-id
                                  :resolved-commit full
                                  :blob-sha256-verified true
                                  :tick-record-pin {:path tick-record
                                                    :sha256 tick-sha}}))
          proposal (dfe/supplemental-carrier-proposal
                     {:injury-id "injury/adf04df0f31ab8727ac57d9c7f2496d8fa28598c"
                      :source-record-pin {:path tick-record :sha256 tick-sha}
                      :retained-pin (dissoc pin :status)
                      :boundary-var "futon2.aif.wm.cascade-patch-delivery/check-delivery"
                      :boundary-file "src/futon2/aif/wm/cascade_patch_delivery.clj"})
          proposal-path (str store-root "/proposal-adf04df0-"
                             (:sha256 pin) ".edn")]
      (spit proposal-path (pr-str proposal))
      (println "retained" (:sha256 pin))
      (println "at" (:path pin))
      (println "proposal" proposal-path)
      (println "readiness"
               (pr-str
                 (dfe/evidence-readiness
                   {:source-pin {:path tick-record :sha256 tick-sha}
                    :candidate-pins [(dissoc pin :status)]
                    :requisition-id (:requisition-id answer)
                    :cascade-digest (:cascade-digest answer)}))))))
