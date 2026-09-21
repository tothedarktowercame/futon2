(ns futon2.aif.d-predecessor-task-authority
  "D-only executed-with-artifacts authority. Does not establish E1 portfolio
   membership, the R6-R11 domain, or broader machine-enactment correspondence."
  (:require [clojure.edn :as edn]
            [clojure.set :as set]
            [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.close-retention :as retention]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.cascade-sources :as cascade-sources]
            [futon2.aif.policy-precision-carry :as precision-carry]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.observation-checks :as observation]
            [futon2.aif.task-execution-evidence :as execution])
  (:import (java.nio.file Files StandardOpenOption)
           (java.io PushbackReader StringReader)))

(def authority :d-predecessor-task-authority-v1)
(def default-root "data/wm-d-task-enactment")
(def scope {:certifies :executed-with-artifacts
            :does-not-establish #{:e1-portfolio-membership :r6-r11-domain
                                  :machine-enactment-correspondence}})
(defn- refuse! [kind data]
  (throw (ex-info "D task predecessor refused" (assoc data :d-predecessor/refusal kind))))
(defn- require! [p kind data] (when-not p (refuse! kind data)))
(defn- sha [bytes] (evidence/sha256 bytes))
(defn- file-bytes [path] (Files/readAllBytes (.toPath (io/file path))))
(defn- read-one [bytes]
  (with-open [r (PushbackReader. (StringReader. (String. ^bytes bytes "UTF-8")))]
    (let [eof (Object.) value (edn/read {:eof eof} r)]
      (require! (and (not (identical? value eof)) (identical? eof (edn/read {:eof eof} r)))
                :record-not-one-form {})
      value)))
(defn- git! [repo & args]
  (let [r (apply shell/sh "git" "-C" repo args)]
    (require! (zero? (:exit r)) :git-evidence-unavailable {:repository repo :arguments (vec args)})
    (str/trim (:out r))))
(defn- canonical [path] (.getCanonicalPath (io/file path)))

(defn capture
  "Retain the minted occurrence and declaration bytes before dispatch.
   No pre-side token mapping is inferred from interpretation facts."
  [{:keys [occurrence carry-occurrence-id universe declaration-reads before candidate-id precision-family]}]
  (retention/validate-occurrence occurrence)
  (require! (and (string? carry-occurrence-id) (set? universe)) :carry-identity-unavailable {})
  {:schema :wm/d-task-dispatch-v1 :occurrence occurrence
   :carry-occurrence-id carry-occurrence-id :universe universe
   :r6-candidate-occurrence candidate-id :candidate-to-minted-join :not-established
   :before before :before-evidence :not-measured
   :precision-family (when precision-family
                       (precision-carry/validate-binding! precision-family occurrence))
   :declarations
   (mapv (fn [{:keys [path sha256]}]
           (let [bytes (file-bytes path)]
             (require! (= sha256 (sha bytes)) :declaration-changed-before-dispatch {:path path})
             {:path path :sha256 sha256 :snapshot-edn (String. bytes "UTF-8") :snapshot (read-one bytes)}))
         (distinct (map #(select-keys % [:path :sha256]) declaration-reads)))})

(defn- artifact-tokens
  "Revision-pair C3/C4 affirmations. Other check classes remain explicit
   unavailable measurements; their declared locators are never modified."
  [dispatch repo commit]
  (vec
   (for [{:keys [snapshot sha256]} (:declarations dispatch)
         [token locator] (:locators snapshot)
         :let [qualified [(:target snapshot) token]]
         :when (contains? (:universe dispatch) qualified)]
     (let [same-repo? (= (canonical repo) (canonical (io/file observation/repo-root (:repo locator))))
           class (:class locator)
           after-locator (assoc locator :sha commit)
           result (if (and same-repo? (#{:C3 :C4} class))
                    ((if (= class :C3) observation/check-path-exists observation/check-decl-in-file)
                     after-locator)
                    {:status :missing :kind (if same-repo? :revision-pair-reader-unavailable
                                                :different-artifact-repository)})]
       {:token qualified :declaration-sha256 sha256
        :declared-locator locator :after-locator (when (and same-repo? (#{:C3 :C4} class)) after-locator)
        :result result}))))

(defn prompt-binding [dispatch]
  (str "D_TASK_DISPATCH_SHA256: " (evidence/value-digest dispatch)))

(defn claim
  "Assemble a task claim from final artifact/review inputs. This is not admission."
  [{:keys [dispatch artifact-binding author-job review-job files repository route]}]
  {:schema :wm/d-task-enactment-v1 :authority authority :scope scope
   :enactment-grain :task :b-authority :declared-kernel-of-verified-macro-action
   :causal-attribution :independent-check-required
   :route (case route :recovery :deferred-completion route)
   :dispatch dispatch :artifact-binding artifact-binding
   :author-job author-job :review-job review-job :files (vec files)
   :repository repository
   :revision-pair {:before (get-in dispatch [:before :head])
                   :after (:commit artifact-binding) :before-evidence :not-measured}
   :after-token-evidence
   (when (and (= route :fresh-author) (:commit artifact-binding))
     (artifact-tokens dispatch repository (:commit artifact-binding)))})

(defn- verify-execution!
  "Verify minted identity, corroborated fresh artifact, independent review and
   revision-bound token affirmations. Must not transform model-misfit or unknown
   evidence into successful execution. Portfolio approval is not established.
   READ-JOB resolves the independently owned Agency job by its exact ID."
  [record expected read-job]
  (require! (= :wm/d-task-enactment-v1 (:schema record)) :record-schema-mismatch {})
  (require! (= (:authority record) authority) :authority-mismatch {})
  (require! (= (:scope record) scope) :authority-scope-mismatch {})
  (case (:route record)
    ;; Legacy claims remain readable, but neither spelling certifies execution.
    (:recovery :deferred-completion) (refuse! :deferred-artifact-not-fresh-execution {})
    :historical (refuse! :historical-verification-not-execution {})
    :binding-not-retained (refuse! :binding-not-retained {})
    :dispatch-not-retained (refuse! :dispatch-not-retained {})
    :fresh-author nil
    (refuse! :task-execution-incomplete {}))
  (let [{:keys [dispatch repository artifact-binding files revision-pair]} record
        occurrence (retention/validate-occurrence (:occurrence dispatch))
        author (read-job (get-in record [:author-job :job-id]))
        reviewer (read-job (get-in record [:review-job :job-id]))
        before (:before dispatch)
        final (:commit artifact-binding)]
    (require! (= occurrence (:occurrence expected)) :occurrence-mismatch {})
    (require! (= (:carry-occurrence-id expected) (:carry-occurrence-id dispatch)) :carry-occurrence-mismatch {})
    (require! (= (:universe expected) (:universe dispatch)) :carry-domain-changed {})
    (require! (= :not-established (:candidate-to-minted-join dispatch)) :unestablished-identity-join {})
    (require! (and (= :task (:enactment-grain record))
                   (= :declared-kernel-of-verified-macro-action (:b-authority record)))
              :kernel-authority-mismatch {})
    (require! (and (string? (:job-id author)) (string? (:job-id reviewer))
                   (= (:job-id author) (get-in record [:author-job :job-id]))
                   (= (:job-id reviewer) (get-in record [:review-job :job-id]))
                   (= "done" (:state author)) (= "done" (:state reviewer))
                   (string? (:agent-id author)) (string? (:agent-id reviewer))
                   (not= (:agent-id author) (:agent-id reviewer))
                   (not= (:job-id author) (:job-id reviewer))) :independent-jobs-unestablished {})
    (doseq [job [author reviewer]]
      (let [prompts (str/join "\n" (keep :text (filter #(= "prompt" (:type %)) (:events job))))]
        (require! (str/includes? prompts (prompt-binding dispatch))
                  :job-occurrence-binding-unestablished {:job-id (:job-id job)})))
    (let [prompts (str/join "\n" (keep :text (filter #(= "prompt" (:type %)) (:events reviewer))))]
      (require! (and (string? final) (str/includes? prompts final)
                     (str/includes? prompts (str "Repository: " repository)))
                :review-artifact-binding-unestablished {}))
    (require! (:valid? (execution/independent-review-evidence files reviewer))
              :independent-review-not-passed {})
    (require! (and (string? final) (re-matches #"[0-9a-f]{40}" final)
                   (= final (git! repository "rev-parse" (str final "^{commit}")))
                   (= repository (:repo before))
                   (= (:head before) (:before revision-pair))
                   (= final (:after revision-pair))
                   (= :not-measured (:before-evidence revision-pair))) :revision-pair-invalid {})
    (git! repository "merge-base" "--is-ancestor" (:head before) final)
    ;; Reuse the runner's check with the retained post-dispatch observation,
    ;; while independently re-resolving Git ancestry, commit and timestamps.
    (let [actual (execution/fresh-artifact-binding
                  {:repo-head-observation-fn
                   (fn [_] {:repo repository :head (:observed-head artifact-binding)
                            :observed-at-ms (:author-window-end-ms artifact-binding)})}
                  repository
                  {:repo repository :head (:pre-dispatch-head artifact-binding)
                   :observed-at-ms (:author-window-start-ms artifact-binding)} author)]
      (require! (and (:corroborates? actual) (= final (:commit actual))
                     (= (select-keys artifact-binding [:pre-dispatch-head :observed-head :commit
                                                      :author-window-start-ms :author-window-end-ms])
                        (select-keys actual [:pre-dispatch-head :observed-head :commit
                                            :author-window-start-ms :author-window-end-ms])))
                :fresh-artifact-unverified {}))
    (require! (= (:declaration-pins expected)
                 (mapv #(select-keys % [:path :sha256]) (:declarations dispatch)))
              :declaration-pins-mismatch {})
    (doseq [{:keys [sha256 snapshot snapshot-edn]} (:declarations dispatch)]
      (require! (and (string? snapshot-edn)
                     (= sha256 (sha (.getBytes ^String snapshot-edn "UTF-8")))
                     (= snapshot (read-one (.getBytes ^String snapshot-edn "UTF-8"))))
                :declaration-snapshot-mismatch {}))
    (when-let [family (:precision-family dispatch)]
      (precision-carry/validate-binding! family occurrence)
      (let [target (get-in family [:selected-action :target])
            declarations (filter #(= target (get-in % [:snapshot :target])) (:declarations dispatch))]
        (require! (= 1 (count declarations)) :precision-selected-declaration-unestablished {})
        (require! (= (cascade-sources/observation-schedule (:snapshot (first declarations)))
                     (get-in family [:observation-schedules target]))
                  :precision-observation-schedule-mismatch {})))
    (let [replayed (artifact-tokens dispatch repository final)
          affirmations (filter #(true? (get-in % [:result :observed])) replayed)
          present (set (map :token affirmations))]
      (require! (= replayed (:after-token-evidence record)) :after-token-evidence-mismatch {})
      (require! (seq present) :after-token-evidence-unavailable {})
      {:status :admitted :authority authority :scope scope
       :occurrence occurrence :carry-occurrence-id (:carry-occurrence-id dispatch)
       :candidate-to-minted-join :not-established
       :r6-candidate-occurrence (:r6-candidate-occurrence dispatch)
       :enactment-grain :task :b-authority :declared-kernel-of-verified-macro-action
       :declared-action (:action/value occurrence)
       :revision-pair revision-pair :before-evidence :not-measured
       :present present :absent #{} :unknown (set/difference (:universe dispatch) present)
       :causal-attribution :independent-check-required
       :precision-family (when-let [family (:precision-family dispatch)]
                           (precision-carry/validate-binding! family occurrence))
       :record-sha256 (evidence/value-digest record)})))

(defn- verify-with-identity! [record expected read-job]
  (let [receipt (retention/occurrence-identity-receipt (get-in record [:dispatch :occurrence]))
        modes (:matched-print-namespace-maps receipt)]
    (if (seq modes)
      ;; Legacy prompt/dispatch hashes used the same ambient printer as the
      ;; occurrence. Replay only recognized modes that match the stored action
      ;; digest, and require ALL existing execution checks in the chosen mode.
      (loop [[mode & more] modes]
        (let [result (try {:verification (identity/with-printer
                                         mode #(verify-execution! record expected read-job))}
                          (catch clojure.lang.ExceptionInfo e {:error e}))]
          (if-let [error (:error result)]
            (if (seq more) (recur more) (throw error))
            (assoc result :identity-verification
                   (assoc receipt :execution-print-namespace-maps mode)))))
      {:verification (verify-execution! record expected read-job)
       :identity-verification receipt})))

(defn verify!
  "Verify execution unchanged; legacy records replay their verified printer
   mode rather than inheriting the reader's CLI/server print settings."
  [record expected read-job]
  (:verification (verify-with-identity! record expected read-job)))

(defn verify [record expected read-job]
  (try (verify! record expected read-job)
       (catch clojure.lang.ExceptionInfo e
         (if-let [kind (:d-predecessor/refusal (ex-data e))]
           {:status :refused :authority authority :scope scope :kind kind}
           {:status :invalid :authority authority :scope scope
            :kind :verification-input-invalid :detail (ex-data e)}))))

(defn- record-file [root occurrence]
  (retention/validate-occurrence occurrence)
  (io/file root (str (:action/id occurrence) ".edn")))
(defn write-claim! [root record]
  (let [f (record-file root (get-in record [:dispatch :occurrence]))
        text (str (pr-str record) "\n")]
    (io/make-parents f)
    (Files/write (.toPath f) (.getBytes text "UTF-8")
                 (into-array StandardOpenOption [StandardOpenOption/CREATE_NEW StandardOpenOption/WRITE]))
    {:path (.getCanonicalPath f) :sha256 (sha (.getBytes text "UTF-8"))}))
(defn read-predecessor [root expected read-job]
  (if-not (:occurrence expected)
    {:status :refused :authority authority :scope scope :kind :carry-no-predecessor}
    (let [f (record-file root (:occurrence expected))]
      (if-not (.isFile f)
        {:status :refused :authority authority :scope scope :kind :carry-no-predecessor}
        (try (let [bytes (file-bytes f)]
               (assoc (verify (read-one bytes) expected read-job)
                      :source {:path (.getCanonicalPath f) :sha256 (sha bytes)}))
             (catch Exception _ {:status :invalid :authority authority :scope scope
                                 :kind :predecessor-record-unreadable}))))))

(defn agency-job [job-id]
  (require! (and (string? job-id) (re-matches #"[A-Za-z0-9_-]+" job-id))
            :job-id-invalid {})
  (let [r (http/get (str "http://localhost:7070/api/alpha/invoke/jobs/" job-id)
                    {:timeout 10000 :throw false})]
    (require! (= 200 (:status r)) :agency-job-unavailable {:job-id job-id})
    (:job (json/parse-string (:body r) true))))

(defn context [decision occurrence declaration-reads]
  (let [carry (get-in decision [:selection-certificate :token-belief-stage :prospective-carry])]
    {:occurrence occurrence :carry-occurrence-id (:occurrence-id carry)
     :universe (:universe carry)
     :precision-family (get-in decision [:selection-certificate :precision-family])
     :candidate-id (or (:r6-candidate-occurrence decision) (:selected/occurrence-id decision))
     :candidate-to-minted-join :not-established
     :declaration-pins (vec (distinct (map #(select-keys % [:path :sha256]) declaration-reads)))}))

(defn produce! [root inputs expected read-job]
  (let [record (claim inputs)
        verification (verify record expected read-job)
        source (write-claim! root record)]
    {:authority authority :scope scope :verification verification :source source}))

(defn capture-result [inputs]
  (try {:status :captured :dispatch (capture inputs)}
       (catch Exception e
         {:status :refused :authority authority :scope scope
          :kind (or (:d-predecessor/refusal (ex-data e)) :dispatch-evidence-unavailable)})))

(defn complete! [root captured expected data read-job]
  (if (:historical? data)
    {:status :refused :authority authority :scope scope
     :kind :historical-verification-not-execution}
    (if-not (= :captured (:status captured))
      (or captured {:status :refused :authority authority :scope scope
                    :kind :dispatch-evidence-unavailable})
      (let [inputs {:dispatch (:dispatch captured)
                    :artifact-binding (:artifact-binding data)
                    :author-job (:author-job data) :review-job (:review-job data)
                    :files (:files data) :repository (get-in data [:artifact-binding :repo])
                    :route (cond
                             (#{:recovery :deferred-completion} (:dispatch-route data)) :deferred-completion
                             (nil? (:commit data)) :incomplete
                             (nil? (:artifact-binding data)) :binding-not-retained
                             (= :fresh-author (:dispatch-route data)) :fresh-author
                             :else :dispatch-not-retained)}]
        (try (produce! root inputs expected read-job)
             (catch Exception e
               {:status :refused :authority authority :scope scope
                :kind (or (:d-predecessor/refusal (ex-data e)) :task-evidence-unavailable)}))))))

(def observation-authority :d-task-token-observations-v2)
(def observation-scope
  {:certifies :revision-bound-checkable-observations
   :does-not-establish #{:causal-attribution :mission-completion
                        :belief-conditioning :machine-enactment-correspondence}})

(defn- checked-observation [locator]
  (case (:class locator)
    :C3 (observation/check-path-exists locator)
    :C4 (observation/check-decl-in-file locator)
    {:status :missing :kind :revision-pair-reader-unavailable}))

(defn- observation-value [result]
  (if (boolean? (:observed result))
    (:observed result)
    {:status :missing :kind (or (:kind result) :observation-unavailable)}))

(defn- signed-observations [record]
  (let [dispatch (:dispatch record)
        rows (group-by :token (:after-token-evidence record))
        final (get-in record [:revision-pair :after])]
    (into
     (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
     (for [[target token :as qualified] (sort-by pr-str (:universe dispatch))]
       (let [declarations (filter #(= target (get-in % [:snapshot :target])) (:declarations dispatch))
             _ (require! (= 1 (count declarations)) :observation-declaration-ambiguous {:token qualified})
             {:keys [snapshot sha256]} (first declarations)
             declared (set (concat (:facts snapshot) (:want snapshot) (keys (:locators snapshot))))
             _ (require! (contains? declared token) :observation-token-unbound {:token qualified})
             locator (get-in snapshot [:locators token])
             matches (get rows qualified)
             _ (require! (<= (count matches) 1) :observation-token-ambiguous {:token qualified})
             measurement (first matches)
             result (or (:result measurement) {:status :missing :kind :no-locator})
             _ (when (boolean? (:observed result))
                 (require! (and (= final (get-in result [:evidence :resolved-sha]))
                                (= sha256 (:declaration-sha256 measurement))
                                (= locator (:declared-locator measurement)))
                           :observation-artifact-binding-mismatch {:token qualified}))
             ;; A revision-pair measurement is NOT the pinned historical proposition.
             ;; Preserve both questions, including missing historical revisions.
             historical? (and (string? (:sha locator)) (not= "HEAD" (:sha locator)))
             meaning {:token qualified :declaration-sha256 sha256 :locator locator}
             schedule (cascade-sources/observation-schedule snapshot)]
         [qualified
          (cond-> {:meaning meaning :meaning-sha256 (evidence/value-digest meaning)
                   :schedule schedule :schedule-sha256 (evidence/value-digest schedule)
                   :artifact-observation {:temporal-scope :artifact-revision
                                          :artifact-sha final :observed (observation-value result)
                                          :measurement measurement
                                          :evidence-sha256 (evidence/value-digest result)}
                   :consumption :not-authorized}
            historical?
            (assoc :declared-revision-observation
                   (let [historical (checked-observation locator)]
                     {:temporal-scope :declared-revision :locator locator
                      :observed (observation-value historical) :result historical
                      :evidence-sha256 (evidence/value-digest historical)})))])))))

(defn verify-observations-v2
  "Opt-in signed observations, with unchanged v1 execution verification first.
   V1 producers/readers and their byte representation remain untouched. This
   projection grants no scheduled conditioning or causal authority."
  [record expected read-job]
  (try
    (let [{execution :verification identity-receipt :identity-verification}
          (verify-with-identity! record expected read-job)]
      {:schema :wm/d-task-token-observations-v2
       :occurrence-identity-verification identity-receipt :status :admitted
       :authority observation-authority :scope observation-scope
       :execution-verification execution
       :occurrence (:occurrence execution)
       :carry-occurrence-id (:carry-occurrence-id execution)
       :universe (get-in record [:dispatch :universe])
       :revision-pair (:revision-pair record)
       :record-sha256 (evidence/value-digest record)
       :observations (signed-observations record)
       :causal-attribution :independent-check-required
       :consumption :not-authorized})
    (catch clojure.lang.ExceptionInfo e
      {:schema :wm/d-task-token-observations-v2
       :status :refused :authority observation-authority :scope observation-scope
       :kind (or (:d-predecessor/refusal (ex-data e)) :observation-input-invalid)
       :detail (ex-data e)})))
