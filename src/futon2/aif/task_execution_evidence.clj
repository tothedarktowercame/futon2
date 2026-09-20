(ns futon2.aif.task-execution-evidence
  "Shared artifact and review checks used by the runner and D task verifier."
  (:require [clojure.java.shell :as shell] [clojure.string :as str])
  (:import (java.time Instant)))

(def artifact-window-tolerance-ms (* 2 60 1000))
(defn- git [repo & args] (apply shell/sh "git" "-C" repo args))
(defn commit-ish? [v] (and (string? v) (boolean (re-matches #"(?i)[0-9a-f]{7,40}" v))))

(defn job-text [job]
  ;; The prompt itself names all verdict markers, so including it makes every
  ;; review look approved. Agency persists the response prefix in
  ;; :result-summary; require the reviewer to put its verdict first.
  ;; :result is the full response and must be read too: Agency trims event
  ;; text at ~2000 chars, so a terminal marker at the end of a longer reply
  ;; survives only there (attempt-003 round 2, 2026-09-13: the author's DONE
  ;; line was trimmed from the text event and the binding fell back to the
  ;; dispatch-time :artifact-ref, condemning a corroborable commit).
  (str/join "\n"
            (concat (keep identity [(:result-summary job)
                                    (:terminal-message job)
                                    (:result job)])
                    (keep :text (remove #(= "prompt" (:type %)) (:events job))))))

(defn review-verdict [job]
  (let [text (job-text job)
        marker (some-> (re-find #"(?m)^FULL_LOOP_REVIEW:\s*(APPROVE|REQUEST_CHANGES|REJECT)\b"
                                text)
                       second)]
    (case marker
      "APPROVE" :approve
      "REQUEST_CHANGES" :request-changes
      "REJECT" :reject
      :unverifiable)))

(defn observe-repo-head [opts repo]
  (if-let [f (:repo-head-observation-fn opts)]
    (f repo)
    (let [head (when repo (git repo "rev-parse" "HEAD"))]
      {:repo repo
       :head (when (and head (zero? (:exit head))) (str/trim (:out head)))
       :observed-at-ms (System/currentTimeMillis)})))

(defn resolve-commit-sha [opts repo commit]
  (when (and repo (not (str/blank? (str commit))))
    (if-let [f (:resolve-commit-sha-fn opts)]
      (f repo commit)
      (let [result (git repo "rev-parse" (str commit "^{commit}"))]
        (when (zero? (:exit result)) (str/trim (:out result)))))))

(defn commit-time-ms [opts repo commit]
  (if-let [f (:commit-time-ms-fn opts)]
    (f repo commit)
    (let [result (git repo "show" "-s" "--format=%cI" commit)]
      (when (zero? (:exit result))
        (try (.toEpochMilli (Instant/parse (str/trim (:out result))))
             (catch Throwable _ nil))))))

(defn ancestor? [opts repo ancestor descendant]
  (if-let [f (:ancestor-fn opts)]
    (boolean (f repo ancestor descendant))
    (zero? (:exit (git repo "merge-base" "--is-ancestor" ancestor descendant)))))


(defn- author-claimed-ref
  "The sha the author's own FULL_LOOP_AUTHOR: DONE line claims.  The Agency
  job's :artifact-ref is stamped at dispatch with the pre-dispatch head, so
  corroborating against it condemns every author who actually commits
  (attempt-002, 2026-09-13: observed 9a6a012f, corroborated against the
  base 2e5e7409, mismatch).  The text claim is the author's; the job field
  is only a fallback for jobs with no readable marker."
  [author-job]
  ;; LAST match, not first: revision prompts quote prior findings verbatim,
  ;; so an earlier round's DONE line can appear inside the reply body ahead
  ;; of the author's own final declaration (repair-ea1-3f4cac attempt-002
  ;; family). The final DONE line is the author's authoritative claim.
  (or (some->> (job-text author-job)
               (re-seq #"(?m)^FULL_LOOP_AUTHOR:\s*DONE\b[ \t]*([0-9a-fA-F]{7,40})")
               last
               second)
      (:artifact-ref author-job)))

(defn fresh-artifact-binding
  "Observe and validate the commit produced by one fresh author dispatch.
  The claimed commit must resolve to the validated repository HEAD.
  Divergent or missing claims fail closed. Injected observations cannot
  override an explicit disagreement."
  [opts repo before author-job]
  (let [binding
        (if-let [f (:author-artifact-observer-fn opts)]
          (f repo before author-job)
          (let [after (observe-repo-head opts repo)
                before-head (:head before)
                observed-head (:head after)
                reported-text-ref (author-claimed-ref author-job)
                job-ref (:artifact-ref author-job)
                start-ms (:observed-at-ms before)
                end-ms (:observed-at-ms after)
                changed? (and before-head observed-head (not= before-head observed-head))
                descendant? (and changed?
                                 (ancestor? opts repo before-head observed-head))
                timestamp-ms (when changed? (commit-time-ms opts repo observed-head))
                in-window? (and timestamp-ms start-ms end-ms
                                (<= (- start-ms artifact-window-tolerance-ms)
                                    timestamp-ms
                                    (+ end-ms artifact-window-tolerance-ms)))
                observed-valid? (and changed? descendant? in-window?)
                reported-text-sha (resolve-commit-sha opts repo reported-text-ref)
                job-ref-sha (when (and (nil? reported-text-sha)
                                       (commit-ish? job-ref))
                              (resolve-commit-sha opts repo job-ref))
                ;; A DONE carrier can retain a bad long expansion while
                ;; Agency retains the actual short Git ref separately
                ;; (repair-ea1-9b6ce3a4). Git rev-parse supplies the
                ;; unambiguous-prefix check. Never borrow the dispatch-time
                ;; base stamp as a fallback claim.
                fallback-sha (when (and job-ref-sha
                                        (not= job-ref-sha before-head))
                               job-ref-sha)
                text-sha (or reported-text-sha fallback-sha)
                text-ref (if reported-text-sha reported-text-ref
                             (if fallback-sha job-ref reported-text-ref))
                resolved-from (cond reported-text-sha :done-line
                                    fallback-sha :job-artifact-ref)
                ;; A claimed commit corroborates when it IS the observed
                ;; head, or when it is the author's own commit that later
                ;; commits (concurrent machinery deposits, revision-round
                ;; bookkeeping) have moved HEAD past. The claim must still
                ;; be a fresh descendant of the pre-dispatch head -- a claim
                ;; naming the base or an unrelated sha never corroborates,
                ;; so the guard stays fail-closed (review round 2 of
                ;; repair-ea1-3f4cac: the unchanged guard was the finding).
                claim-descendant?
                (and text-sha
                     (not= text-sha before-head)
                     (ancestor? opts repo before-head text-sha))
                ;; Freshness applies to the RETURNED commit, not only the
                ;; observed head: a pre-existing side-branch commit merged
                ;; during the window is a descendant of the base and an
                ;; ancestor of head, yet was not authored by this dispatch.
                ;; Round-2 review of repair-ea1-3f4cac (job
                ;; invoke-1789420253972) reproduced exactly that: an
                ;; out-of-window claim reported :in-author-window? true
                ;; because only HEAD's timestamp was ever read.
                claim-time-ms (when (and text-sha
                                         (not= text-sha observed-head))
                                (commit-time-ms opts repo text-sha))
                claim-in-window? (if (or (nil? text-sha)
                                         (= text-sha observed-head))
                                   in-window?
                                   (and claim-time-ms start-ms end-ms
                                        (<= (- start-ms
                                               artifact-window-tolerance-ms)
                                            claim-time-ms
                                            (+ end-ms
                                               artifact-window-tolerance-ms))))
                corroborates? (and observed-valid? claim-descendant?
                                    claim-in-window?
                                    (or (= observed-head text-sha)
                                        (ancestor? opts repo text-sha
                                                  observed-head)))]
            {:fresh-author? true
             :repo repo
             :pre-dispatch-head before-head
             :observed-head observed-head
             :observed-commit-time-ms timestamp-ms
             :author-window-start-ms start-ms
             :author-window-end-ms end-ms
             :text-artifact-ref text-ref
             :text-artifact-sha text-sha
             :reported-text-artifact-ref reported-text-ref
             :resolved-from resolved-from
             :claim-resolution (if text-sha :resolved :unresolved)
             :claim-commit-time-ms claim-time-ms
             :descendant? (boolean descendant?)
             ;; Keep HEAD freshness and returned-claim freshness distinct in
             ;; the durable binding.  A moved HEAD can be fresh while the
             ;; claimed ancestor is stale; collapsing those observations made
             ;; rejection evidence misleading even after the guard was fixed.
             :in-author-window? (boolean in-window?)
             :claim-in-author-window? (boolean claim-in-window?)
             :corroborates? (boolean corroborates?)
             :disagreement? (and observed-valid? (some? text-sha)
                                 (not corroborates?))
             :commit (when corroborates? (or text-sha observed-head))}))]
    (when (or (:disagreement? binding)
              (= :unresolved (:claim-resolution binding)))
      ;; A claim that does not even LOOK like a commit is not a binding
      ;; disagreement: it is upstream extraction handing us prose (the
      ;; canary-de75cee9 shape -- a file path). unvalidated-artifact-failure
      ;; already classifies this on the build-gate path; this boundary owed
      ;; the same distinction (round-3 of repair-ea1-3f4cac).
      (let [ref (:text-artifact-ref binding)
            malformed? (and (string? ref) (not (commit-ish? ref)))
            failure-kind (cond
                           malformed? :artifact-ref-malformed
                           (= :unresolved (:claim-resolution binding))
                           :artifact-ref-unresolved
                           :else :artifact-binding-mismatch)]
        (throw (ex-info
                (case failure-kind
                  :artifact-ref-malformed
                  "Author artifact claim is not a commit at all"
                  :artifact-ref-unresolved
                  "Author artifact claim does not resolve in the repository"
                  "Author commit claim disagrees with observed repository HEAD")
                {:outcome :build-failed
                 :failure-kind failure-kind
                 :failure-stage :artifact-binding
                 :author-job author-job
                 :artifact-binding (assoc binding :commit nil)}))))
    binding))

(def ^:private code-file-pattern
  #"(?i)\.(?:clj|cljc|cljs|bb|el|lean|py|js|jsx|ts|tsx|java|go|rs|c|cc|cpp|h|hpp|sh)$")

(defn- review-execution-evidence [review-job]
  (let [reported (:execution review-job)
        reported-executed? (true? (:executed reported))
        reported-tool-events (long (or (:tool-events reported) 0))
        reported-command-events (long (or (:command-events reported) 0))
        ledger-events (->> (:events review-job)
                           (filter #(and (= "tool_use" (str (:type %)))
                                         (seq (:tools %))))
                           vec)
        ledger-tool-events (long (count ledger-events))
        ledger-command-events (->> ledger-events
                                   (filter #(some #{"Bash"} (:tools %)))
                                   count
                                   long)
        reported-valid? (and reported-executed?
                             (pos? reported-tool-events))
        ledger-executed? (pos? ledger-tool-events)]
    {:execution
     {:executed (or reported-valid?
                    ledger-executed?)
      :tool-events (max reported-tool-events ledger-tool-events)
      :command-events (max reported-command-events ledger-command-events)}
     :source (if (and ledger-executed?
                      (or (not reported-valid?)
                          (> ledger-tool-events reported-tool-events)))
               :job-events
               :job-summary)}))

(defn review-execution-gate [files review-job]
  (let [code-files (vec (filter #(re-find code-file-pattern (str %)) files))
        required? (boolean (seq code-files))
        {:keys [execution source]}
        (review-execution-evidence review-job)
        executed? (true? (:executed execution))
        tool-events (:tool-events execution)
        passed? (or (not required?)
                    (and executed? (pos? tool-events)))]
    {:required? required?
     :code-files code-files
     :executed? executed?
     :tool-events tool-events
     :execution execution
     :execution-source source
     :passed? passed?
     :failure-kind (when-not passed? :review-execution-evidence-missing)}))

(defn independent-review-evidence
  "Public read-only validator for an already fetched Agency review job. Uses
  the same verdict and execution evidence as full-loop adjudication, but always
  requires execution, including when the reviewed artifacts are data files."
  [files review-job]
  (let [gate (review-execution-gate files review-job)
        verdict (review-verdict review-job)]
    {:job-id (:job-id review-job)
     :state (:state review-job)
     :verdict verdict
     :execution (:execution gate)
     :execution-source (:execution-source gate)
     :valid? (and (string? (:job-id review-job))
                  (= "done" (:state review-job))
                  (= :approve verdict)
                  (:executed? gate)
                  (:passed? gate))}))

