(ns futon2.aif.full-loop-runner
  "Authoritative real-actuation runner for one War Machine opportunity.

  One opportunity selects exactly one strategic action, constructs for that
  decision, dispatches an author, dispatches a distinct reviewer, verifies the
  resulting commit, records a typed implementation/discharge in Futon1b, queues
  Morning Brief QA, and closes every preregistered checkpoint. The deterministic
  fold executor is not an actuator here."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.aif.c-vector :as cv]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.mission-registry :as missions]
            [futon2.aif.morning-brief :as brief]
            [futon2.aif.substrate :as substrate]
            [futon2.aif.trace :as trace]
            [futon2.report.cascade-lane :as cascade]
            [futon2.report.war-machine :as wm])
  (:import [java.security MessageDigest]
           [java.time Instant]
           [java.util UUID]))

(def default-agency-base "http://127.0.0.1:7070")
(def default-substrate-base "http://127.0.0.1:7073")
(def default-author "zai-5")
(def default-reviewer "codex-7")
(def default-timeout-ms (* 90 60 1000))
(def semantic-epoch :full-loop-real-actuation-v1)
(def required-checkpoints [:selection :construction :dispatch :build :adjudication])

(defn config
  ([] (config {}))
  ([opts]
   (merge {:agency-base (or (System/getenv "FUTON_WM_AGENCY_BASE")
                            default-agency-base)
           :author (or (System/getenv "FUTON_WM_AUTHOR_AGENT") default-author)
           :reviewer (or (System/getenv "FUTON_WM_REVIEWER_AGENT") default-reviewer)
           :substrate-url (or (System/getenv "FUTON_SUBSTRATE_URL")
                              (System/getenv "FUTON1B_URL")
                              default-substrate-base)
           :timeout-ms (or (some-> (System/getenv "FUTON_WM_AGENT_TIMEOUT_MS")
                                   parse-long)
                           default-timeout-ms)
           :poll-ms 2000
           :window-days 14
           :trigger :duree-click-on-demand
           :cohort? true
           :semantic-epoch semantic-epoch}
          opts)))

(defn- sha256 [x]
  (let [bytes (.digest (MessageDigest/getInstance "SHA-256")
                       (.getBytes (pr-str x) "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and 0xff %)) bytes))))

(defn- git [repo & args]
  (apply shell/sh "git" "-C" repo args))

(defn- repo-state [repo]
  (let [sha (git repo "rev-parse" "HEAD")
        dirty (git repo "status" "--porcelain" "--untracked-files=no")]
    {:repo repo
     :git-sha (when (zero? (:exit sha)) (str/trim (:out sha)))
     :git-dirty? (if (zero? (:exit dirty))
                   (not (str/blank? (:out dirty)))
                   :unknown)}))

(defn- primary-repos []
  (->> (or (.listFiles (io/file "/home/joe/code")) [])
       (filter #(.isDirectory %))
       (filter #(re-matches #"futon[^/]*" (.getName %)))
       (filter #(or (.exists (io/file % ".git"))
                    (.isFile (io/file % ".git"))))
       (mapv #(.getAbsolutePath %))))

(defn- stack-code-state []
  (let [states (mapv repo-state (primary-repos))
        f2 (first (filter #(= "/home/joe/code/futon2" (:repo %)) states))]
    (assoc f2 :repo-heads (into {} (map (juxt :repo :git-sha) states)))))

(defn- agent-roster [agency-base]
  (let [r (http/get (str agency-base "/api/alpha/agents")
                    {:timeout 10000 :throw false})]
    (when-not (= 200 (:status r))
      (throw (ex-info "Agency roster unavailable"
                      {:outcome :agent-unavailable :status (:status r)})))
    (:agents (json/parse-string (:body r) true))))

(defn- available? [roster agent]
  (let [a (or (get roster (keyword agent)) (get roster agent))]
    (and a (true? (:invoke-ready? a)) (= "idle" (name (:status a))))))

(defn readiness
  "Read-only readiness view for the configured author/reviewer pair."
  ([] (readiness {}))
  ([raw-opts]
   (let [{:keys [agency-base author reviewer] :as opts} (config raw-opts)
         roster (agent-roster agency-base)]
     {:configuration (select-keys opts [:agency-base :substrate-url :author :reviewer
                                        :timeout-ms :window-days])
      :agents (into {}
                    (for [agent [author reviewer]
                          :let [record (or (get roster (keyword agent))
                                           (get roster agent))]]
                      [agent {:available? (available? roster agent)
                              :status (:status record)
                              :invoke-ready? (:invoke-ready? record)
                              :session-id (:session-id record)}]))})))

(defn substrate-preflight!
  "Prove that the configured authoritative semantic entity route is reachable.
  A 404 for a unique sentinel is success; the substrate client returns nil."
  [opts]
  (let [started (System/currentTimeMillis)
        probe-id (str "full-loop/preflight/" (UUID/randomUUID))]
    (try
      (substrate/entity-by-id probe-id
                              (assoc opts :substrate-timeout-ms
                                     (or (:substrate-preflight-timeout-ms opts) 15000)))
      {:url (substrate/configured-url opts)
       :route :entity-by-id
       :latency-ms (- (System/currentTimeMillis) started)}
      (catch Throwable e
        (throw (ex-info "Authoritative substrate preflight failed"
                        {:outcome :substrate-unavailable
                         :url (substrate/configured-url opts)
                         :cause-class (.getName (class e))}
                        e))))))

(defn- post-json! [url body]
  (let [r (http/post url {:headers {"Content-Type" "application/json"}
                          :body (json/generate-string body)
                          :timeout 30000 :throw false})
        parsed (try (json/parse-string (:body r) true)
                    (catch Throwable _ {:raw (:body r)}))]
    (if (<= 200 (:status r) 299)
      parsed
      (throw (ex-info "Agency dispatch failed"
                      {:outcome :dispatch-failed :status (:status r)
                       :response parsed})))))

(defn dispatch!
  [{:keys [agency-base]} agent caller mission prompt]
  (post-json! (str agency-base "/api/alpha/bell")
              {:agent-id agent :caller caller :mission-id (str mission)
               :type "request" :prompt prompt}))

(def terminal-states #{"done" "failed" "cancelled" "timed-out"})

(defn poll-job!
  [{:keys [agency-base timeout-ms poll-ms]} job-id]
  (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
    (loop []
      (let [r (http/get (str agency-base "/api/alpha/invoke/jobs/" job-id)
                        {:timeout 10000 :throw false})
            body (when (= 200 (:status r)) (json/parse-string (:body r) true))
            job (:job body)]
        (cond
          (not= 200 (:status r))
          (throw (ex-info "Agency job read failed"
                          {:outcome :dispatch-failed :job-id job-id
                           :status (:status r)}))

          (contains? terminal-states (:state job)) job

          (>= (System/currentTimeMillis) deadline)
          (throw (ex-info "Agency job timed out"
                          {:outcome :dispatch-failed :job-id job-id
                           :timeout-ms timeout-ms}))

          :else (do (Thread/sleep poll-ms) (recur)))))))

(defn- selected-entry [judgement]
  (let [action (get-in judgement [:decision :action])]
    (when (map? action)
      (first (filter #(= action (:action %)) (:ranked-actions judgement))))))

(defn- selected-target [entry]
  (or (get-in entry [:action :target])
      (get-in entry [:action :target-class])
      (get-in entry [:action :type])))

(defn- mission-entry [target]
  (first (filter #(= (missions/mission-target-id target) (:id %))
                 (missions/open-missions))))

(defn construct-for-decision
  "Construct only for the selected policy entry, never the pre-prior rank head."
  [entry]
  (first (cascade/cascade-lane [entry] {:n 1 :budget 6})))

(defn- job-text [job]
  ;; The prompt itself names all verdict markers, so including it makes every
  ;; review look approved. Agency persists the response prefix in
  ;; :result-summary; require the reviewer to put its verdict first.
  (str/join "\n"
            (concat (keep identity [(:result-summary job) (:terminal-message job)])
                    (keep :text (remove #(= "prompt" (:type %)) (:events job))))))

(defn- author-prompt [{:keys [author reviewer]} target mission cascade-entry]
  (str author ": FULL-LOOP IMPLEMENTATION OPPORTUNITY. You are the author; "
       reviewer " is the independent reviewer.\n\n"
       "Implement one bounded, substantive advancement of the selected War Machine action. "
       "This is NOT a request for a fold-turn deposit, wiring diagram, report-only artifact, "
       "or prose claiming that work could be done. Change the actual mission/code world.\n\n"
       "SELECTED TARGET: " (pr-str target) "\n"
       "MISSION RECORD: " (pr-str mission) "\n"
       "PATTERN CASCADE: " (pr-str (select-keys cascade-entry
                                                  [:mission :psi :shown :semilattice
                                                   :cascade-score])) "\n\n"
       "Requirements:\n"
       "1. Inspect the mission and repository state; choose a bounded implementation parcel "
       "that genuinely advances its open work.\n"
       "2. Preserve global invariants; do not special-case or bypass a gate.\n"
       "3. Run the repository-required static checks and relevant tests.\n"
       "4. Commit only your coherent changes. Do not include unrelated dirty files.\n"
       "5. Finish with: FULL_LOOP_AUTHOR: DONE <commit-sha> and list validations.\n"
       "If no safe substantive parcel is possible, make no commit and finish with "
       "FULL_LOOP_AUTHOR: REFUSE <typed reason>."))

(defn- reviewer-prompt [{:keys [reviewer author]} target repo commit author-job]
  (str reviewer ": FULL-LOOP INDEPENDENT REVIEW. " author " authored commit " commit
       " for selected target " (pr-str target) ".\n\n"
       "Repository: " repo "\n"
       "Author job evidence: " (pr-str (select-keys author-job
                                                     [:job-id :state :artifact-ref
                                                      :result-summary :execution])) "\n\n"
       "Inspect the commit rather than trusting the summary. Verify that it is substantive "
       "rather than artifact-only, is in scope for the selected target, preserves invariants, "
       "and clears the required static checks and relevant tests. Do not edit or commit.\n\n"
       "BEGIN your response with exactly one verdict line (Agency preserves the response prefix):\n"
       "FULL_LOOP_REVIEW: APPROVE\n"
       "or FULL_LOOP_REVIEW: REQUEST_CHANGES <reason>\n"
       "or FULL_LOOP_REVIEW: REJECT <reason>"))

(defn- find-commit-repo [commit]
  (some (fn [repo]
          (let [r (git repo "cat-file" "-e" (str commit "^{commit}"))]
            (when (zero? (:exit r)) repo)))
        (primary-repos)))

(defn- commit-files [repo commit]
  (let [r (git repo "diff-tree" "--no-commit-id" "--name-only" "-r" commit)]
    (when-not (zero? (:exit r))
      (throw (ex-info "Cannot inspect authored commit"
                      {:outcome :build-failed :repo repo :commit commit :error (:err r)})))
    (vec (remove str/blank? (str/split-lines (:out r))))))

(defn resolve-build
  "Resolve an Agency artifact-ref to one Futon repository and its changed files."
  [commit]
  (when-let [repo (find-commit-repo commit)]
    {:repo repo :files (commit-files repo commit)}))

(defn- artifact-only-files? [files]
  (and (seq files)
       (every? #(or (str/includes? % "data/fold-turns")
                    (str/includes? % "fold-escrow")
                    (str/includes? % "selection-authoring-flights")
                    (str/includes? % "overnight-flights"))
               files)))

(defn- implementation-id [commit]
  (str "full-loop/implementation/" commit))

(defn- discharge-id [attempt-id]
  (str "full-loop/discharge/" attempt-id))

(defn ground-commit!
  [attempt-id target author reviewer repo commit files review-job opts]
  (let [impl-id (implementation-id commit)
        before (substrate/entity-by-id impl-id opts)
        implementation {:xt/id impl-id
                        :entity/type :implementation/commit
                        :entity/name (str "Reviewed implementation " commit)
                        :entity/source "wm-full-loop"
                        :implementation/target (str target)
                        :implementation/repository repo
                        :implementation/commit commit
                        :implementation/files files
                        :implementation/author author
                        :implementation/reviewer reviewer
                        :implementation/review-job (:job-id review-job)}
        discharge {:xt/id (discharge-id attempt-id)
                   :entity/type :discharge
                   :entity/name (str "Full-loop discharge " attempt-id)
                   :entity/source "wm-full-loop"
                   :discharge/mission (str target)
                   :discharge/endpoint impl-id
                   :discharge/type :implementation/commit
                   :discharge/proof-query (str "GET /api/alpha/entity/" impl-id)
                   :discharge/reviewer reviewer
                   :discharge/review-job (:job-id review-job)
                   :discharge/at (str (Instant/now))}]
    (when before
      (throw (ex-info "Implementation commit already grounded"
                      {:outcome :grounded-no-change :implementation-id impl-id})))
    (substrate/put-doc! implementation opts)
    (substrate/put-doc! discharge opts)
    (let [after (substrate/entity-by-id impl-id opts)]
      {:before {:implementation-entity before}
       :after {:implementation-entity after}
       :resolved? (= commit (get-in after [:props :implementation/commit]))
       :dial-moved? (and (nil? before) (some? after))
       :implementation-id impl-id
       :discharge-id (discharge-id attempt-id)})))

(defn- term [judgment ground]
  {:judgment judgment :ground ground})

(defn- sorry [kind data]
  {:sorry (assoc data :kind kind)})

(defn- outcome-from [e]
  (or (:outcome (ex-data e)) :incomplete))

(defn run-opportunity!
  "Run one opportunity synchronously. Dependencies may be injected in opts for tests."
  [raw-opts]
  (let [{:keys [trigger cohort? semantic-epoch author reviewer window-days]
         :as opts} (config raw-opts)
        started (System/currentTimeMillis)
        opportunity-id (or (:opportunity-id opts)
                           (str (name trigger) "/" (Instant/now) "/" (UUID/randomUUID)))
        checkpoints (atom {})
        closing? (atom false)
        roster ((or (:roster-fn opts) agent-roster) (:agency-base opts))
        code-state ((or (:code-state-fn opts) stack-code-state))
        time-cell (term {:opportunity-id opportunity-id
                         :trigger trigger
                         :machine-state {:started-at (str (Instant/now))}
                         :agent-roster (select-keys roster [(keyword author) (keyword reviewer)
                                                            author reviewer])
                         :code-state (assoc code-state
                                            :resolved-mode-flags (wm/arena-mode-flags)
                                            :configuration-digest
                                            (sha256 (select-keys opts
                                                                 [:author :reviewer :trigger
                                                                  :semantic-epoch])))
                         :semantic-epoch semantic-epoch}
                        {:kind :trigger-opportunity :id opportunity-id})
        start-event (when cohort? (cohort/start-attempt! time-cell))
        attempt-id (or (:attempt/id start-event)
                       (str "canary-" (UUID/randomUUID)))
        checkpoint! (fn [checkpoint cell]
                      (swap! checkpoints assoc checkpoint cell)
                      (when cohort?
                        (cohort/append-checkpoint! attempt-id checkpoint cell))
                      cell)
        close! (fn [outcome data]
                 (reset! closing? true)
                 (doseq [cp required-checkpoints
                         :when (not (contains? @checkpoints cp))]
                   (checkpoint! cp (sorry (keyword (str "not-reached-" (name cp)))
                                          {:outcome outcome})))
                 (let [brief-ref ((or (:queue-fn opts) brief/queue-item!)
                                  {:attempt-id attempt-id :opportunity-id opportunity-id
                                   :trigger trigger :selected-target (:target data)
                                   :outcome outcome :author author :reviewer reviewer
                                   :commit (:commit data) :witness (:witness data)})
                       closed (term (merge {:outcome outcome
                                            :grounded? (= :grounded-change outcome)
                                            :artifact-only? (= :artifact-only outcome)
                                            :morning-brief-ref brief-ref
                                            :duration-ms (- (System/currentTimeMillis) started)
                                            :resource-use {:agent-turns
                                                           (count (filter identity
                                                                          [(:author-job data)
                                                                           (:review-job data)]))}}
                                           (select-keys data [:witness]))
                                    {:kind :full-loop-outcome :attempt-id attempt-id})
                       result {:attempt-id attempt-id :opportunity-id opportunity-id
                               :outcome outcome :checkpoints @checkpoints
                               :morning-brief-ref brief-ref :data data}]
                   (when cohort? (cohort/close-attempt! attempt-id closed))
                   (if-let [path (:canary-out opts)]
                     (do (io/make-parents path)
                         (spit path (with-out-str (pp/pprint result))))
                     nil)
                   result))]
    (try
      (when-not (and (available? roster author) (available? roster reviewer))
        (throw (ex-info "Configured author or reviewer is unavailable"
                        {:outcome :agent-unavailable :author author :reviewer reviewer})))
      ((or (:substrate-preflight-fn opts) substrate-preflight!) opts)
      (try ((or (:refresh-fn opts) cv/maybe-refresh!)) (catch Throwable _ nil))
      (let [judgement0 (:judgement ((or (:judge-fn opts) wm/generate-war-machine)
                                    window-days))
            mode-flags ((or (:mode-flags-fn opts) wm/arena-mode-flags))
            judgement (assoc judgement0 :wm-version
                             ((or (:version-stamp-fn opts) trace/wm-version-stamp)
                              (assoc mode-flags
                                     :trigger trigger
                                     :real-actuation? true
                                     :author author
                                     :reviewer reviewer)))
            trace-path ((or (:trace-fn opts) trace/write-trace!) judgement)
            entry (selected-entry judgement)
            target (some-> entry selected-target)
            selection-cell (if entry
                             (term {:selected-mission (str target)
                                    :selected-action (:action entry)
                                    :ranked-candidates (mapv #(select-keys % [:rank :action
                                                                              :G-efe
                                                                              :controller-score])
                                                             (take 10 (:ranked-actions judgement)))
                                    :selection-reasons
                                    (select-keys (:decision judgement)
                                                 [:source :rank :controller-score :tau
                                                  :selection-gain :habit-prior-applied?])
                                    :trace-path trace-path}
                                   {:kind :wm-judgement :decision (:decision judgement)})
                             (sorry :no-selection {:decision (:decision judgement)}))]
        (checkpoint! :selection selection-cell)
        (when-not entry
          (throw (ex-info "War Machine abstained or selected no addressable action"
                          {:outcome (if (= :abstain (get-in judgement [:decision :action]))
                                      :abstained :no-selection)})))
        (let [mission ((or (:mission-fn opts) mission-entry) target)
              construction ((or (:construct-fn opts) construct-for-decision) entry)]
          (when-not construction
            (throw (ex-info "No construction for selected decision"
                            {:outcome :no-selection :target target})))
          (checkpoint! :construction
                       (term {:mission (str target)
                              :cascade (select-keys construction
                                                    [:psi :cascade-score :semilattice])
                              :sorries (vec (:policy-holes construction))
                              :wiring nil
                              :patterns (vec (:shown construction))
                              :deposit nil}
                             {:kind :decision-pinned-construction
                              :selected-action (:action entry)}))
          (let [author-response ((or (:dispatch-fn opts) dispatch!) opts author
                                 "wm-full-loop" target
                                 (author-prompt opts target mission construction))
                author-job-id (:job-id author-response)]
            (checkpoint! :dispatch
                         (term {:agent author :availability :invoke-ready
                                :job-id author-job-id
                                :prompt-ref (str "agency-job:" author-job-id)}
                               {:kind :agency-dispatch :response author-response}))
            (let [author-job ((or (:poll-fn opts) poll-job!) opts author-job-id)]
              (when-not (= "done" (:state author-job))
                (throw (ex-info "Author job did not complete"
                                {:outcome :build-failed :author-job author-job})))
              (let [commit (:artifact-ref author-job)
                    build (when commit
                            ((or (:resolve-build-fn opts) resolve-build) commit))
                    repo (:repo build)
                    files (:files build)]
                (when-not (and commit repo (vector? files))
                  (throw (ex-info "Author completed without a verifiable commit"
                                  {:outcome :build-failed :author-job author-job})))
                (when (or (empty? files) (artifact-only-files? files))
                  (throw (ex-info "Authored commit is artifact-only"
                                  {:outcome :artifact-only :commit commit :files files})))
                (let [review-response ((or (:dispatch-fn opts) dispatch!) opts reviewer
                                       "wm-full-loop" target
                                       (reviewer-prompt opts target repo commit author-job))
                      review-job ((or (:poll-fn opts) poll-job!) opts
                                  (:job-id review-response))
                      approved? (and (= "done" (:state review-job))
                                     (boolean (re-find #"FULL_LOOP_REVIEW:\s*APPROVE"
                                                       (job-text review-job))))]
                  (checkpoint! :build
                               (term {:artifacts files
                                      :generated-code files
                                      :commits [commit]
                                      :patterns-used (vec (:shown construction))
                                      :inline-improvements []
                                      :validation {:author (:execution author-job)
                                                   :reviewer (:execution review-job)
                                                   :review-job (:job-id review-job)
                                                   :approved? approved?}}
                                     {:kind :git-commit-and-independent-review
                                      :repository repo}))
                  (when-not approved?
                    (throw (ex-info "Independent review did not approve"
                                    {:outcome :build-failed :author-job author-job
                                     :review-job review-job :commit commit})))
                  (let [witness ((or (:ground-fn opts) ground-commit!)
                                 attempt-id target author reviewer repo commit files
                                 review-job opts)]
                    (checkpoint! :adjudication
                                 (term {:before (:before witness)
                                        :after (:after witness)
                                        :witness witness
                                        :build-match {:commit commit
                                                      :review-approved? true}
                                        :dial {:moved? (:dial-moved? witness)
                                               :implementation-id
                                               (:implementation-id witness)}}
                                       {:kind :authoritative-substrate-discharge}))
                    (close! (if (and (:resolved? witness) (:dial-moved? witness))
                              :grounded-change
                              :grounded-no-change)
                            {:target target :commit commit :witness witness
                             :author-job author-job :review-job review-job}))))))))
      (catch Throwable e
        (if @closing?
          (throw e)
          (close! (outcome-from e)
                  {:target (some-> @checkpoints :selection :judgment :selected-mission)
                   :error (.getMessage e)
                   :error-class (.getName (class e))
                   :error-data (ex-data e)}))))))
