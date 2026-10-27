(ns futon2.aif.ticket-readiness
  "Durable, source-bound readiness assessments for canonical tickets.

  Readiness is an assessment, never a lifecycle state.  Preparation and
  implementation are distinct increments; generic C3/C8 meanings are not
  changed here."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.registry-port :as registry-port])
  (:import [java.nio.file Files StandardCopyOption]
           [java.time Instant]))

(load-identity/register! *ns* *file*)

(def schema :wm/ticket-readiness-v1)
(def manifest-schema :wm/ticket-readiness-manifest-v1)
(def contract-schema :wm/ticket-increment-contract-v1)
(def review-schema :wm/ticket-increment-contract-review-v1)
(def registry-schema :wm/ticket-semantic-run-v1)
(def semantic-log-schema :wm/ticket-semantic-log-v1)
(def postcheck-schema :wm/ticket-semantic-postcheck-v1)
(def response-schema :wm/ticket-readiness-response-v1)

(defn preparation-contract-path [ticket-id increment-id]
  (when (and (string? ticket-id) (keyword? increment-id)
             (re-matches #"[A-Za-z0-9._-]+" ticket-id)
             (re-matches #"[A-Za-z0-9._-]+" (name increment-id)))
    (str "holes/labs/wm-contract/ticket-readiness/" ticket-id "/"
         (name increment-id) ".edn")))

(defn- file-bytes [path] (Files/readAllBytes (.toPath (io/file path))))
(defn file-pin [path]
  {:path (.getCanonicalPath (io/file path))
   :sha256 (load-identity/sha256 (file-bytes path))})

(defn- pin-matches? [{:keys [path sha256]}]
  (and (string? path) (.isFile (io/file path))
       (= sha256 (:sha256 (file-pin path)))))

(defn- read-edn [path] (edn/read-string (slurp path)))

(defn value-digest [value]
  (load-identity/sha256 (.getBytes (pr-str value) "UTF-8")))

(defn semantic-negative
  "Admit a retained semantic red only when it ran the named contract failure
  at the immutable before revision.  C8=false is deliberately not an input."
  [{:keys [registry-entry-id registry-resolver expected-revision contract-pin
           failure-id expected-code-pin expected-test-pin]}]
  (try
    (let [resolve-entry (or registry-resolver #(registry-port/call :entry %))
          entry (when (string? registry-entry-id) (resolve-entry registry-entry-id))
          payload-text (get-in entry [:evidence/body :payload-edn])
          body-sha (get-in entry [:evidence/body :sha256])
          body-ok? (and (string? payload-text)
                        (= body-sha (load-identity/sha256 (.getBytes payload-text "UTF-8")))
                        (= registry-entry-id (str "test-registry-" body-sha)))
          record (when body-ok? (edn/read-string payload-text))]
    (if-not (map? record)
      {:status :unknown :reason :semantic-negative-not-established}
      (let [relevant? (some #(= failure-id (:id %)) (:semantic-failures record))]
        (cond
          (or (not= "test-registry/v1" (:schema record))
              (not= :run (:kind record)))
          {:status :unknown :reason :semantic-negative-not-established}
          (not= expected-revision (:git-head record))
          {:status :unknown :reason :before-revision-mismatch}
          (or (not= contract-pin (:contract-pin record))
              (not= expected-code-pin (:code-pin record))
              (not= expected-test-pin (:test-pin record)))
          {:status :unknown :reason :semantic-negative-not-established}
          (or (not relevant?)
              (not= :matched (get-in record [:postcheck :status])))
          {:status :unknown :reason :semantic-negative-not-established}
          :else {:status :established :revision expected-revision
                 :failure-id failure-id :contract-pin contract-pin
                 :code-pin expected-code-pin :test-pin expected-test-pin}))))
    (catch Throwable _
      {:status :unknown :reason :semantic-negative-not-established})))

(defn validate-preparation-output
  "Validate contract contents and a separately bound independent review.
  Existence alone (C3) and a producer's self-asserted reviewer never qualify."
  [{:keys [contract-pin review-pin review-job-id review-resolver reviewer-policy
           ticket-id source-pin increment-id]}]
  (try
    (let [contract (when (pin-matches? contract-pin) (read-edn (:path contract-pin)))
          review (when (pin-matches? review-pin) (read-edn (:path review-pin)))
          job (when (and (fn? review-resolver) (string? review-job-id))
                (review-resolver review-job-id))
          expected {:ticket-id ticket-id :source-pin source-pin :increment-id increment-id}]
      (cond
        (not (map? contract)) {:status :refused :reason :contract-missing-or-unreadable}
        (not= contract-schema (:schema contract)) {:status :refused :reason :contract-malformed}
        (not= expected (select-keys contract (keys expected)))
        {:status :refused :reason :contract-binding-mismatch}
        (not (and (seq (:positive-cases contract)) (seq (:negative-cases contract))))
        {:status :refused :reason :contract-cases-empty}
        (not (map? review)) {:status :refused :reason :review-missing-or-unreadable}
        (not= review-schema (:schema review)) {:status :refused :reason :review-malformed}
        (not= contract-pin (:contract-pin review)) {:status :refused :reason :review-binding-mismatch}
        (or (str/blank? (:reviewer review)) (= (:reviewer review) (:producer contract))
            (not= reviewer-policy (:reviewer review)))
        {:status :refused :reason :review-not-independent}
        (or (not= review-job-id (:job-id review))
            (not= review-job-id (:job-id job))
            (not= (:reviewer review) (:seat job))
            (not= (:result-sha256 review) (value-digest (:result job)))
            (not= contract-pin (:contract-pin job))
            (not= source-pin (:source-pin job)))
        {:status :refused :reason :review-port-binding-mismatch}
        (not= :accepted (:verdict review)) {:status :refused :reason :review-not-accepted}
        :else {:status :qualified :contract-pin contract-pin :review-pin review-pin}))
    (catch Throwable e
      {:status :refused :reason :contract-output-unreadable :message (ex-message e)})))

(defn classify
  "Classify one increment. Explicit operational blocks precede unknowns.
  Unknown implementation dependencies do not block contract preparation."
  [{:keys [increment source-current? completion-evidence ticket-completed?
           operator-allowed? owned? active? preparation-dependencies-resolved?
           relevant-dependencies-resolved? interpretation-valid? root-route
           semantic-negative after-revision expected-after-revision
           preparation-output-qualified?]}]
  (cond
    (= :validated (:status completion-evidence))
    (if (or (not (and (string? expected-after-revision)
                      (re-matches #"[0-9a-f]{40,64}" expected-after-revision)))
            (not= expected-after-revision after-revision)
            (not= after-revision (:revision completion-evidence)))
      {:classification :unknown :reason :after-revision-mismatch}
      {:classification :completed :increment-completed? true
       :ticket-completed? (boolean ticket-completed?)
       :next-disposition (when-not ticket-completed? :assess-remaining-increments)})
    (nil? operator-allowed?) {:classification :unknown :reason :operator-evidence-unknown}
    (nil? owned?) {:classification :unknown :reason :ownership-unknown}
    (false? operator-allowed?) {:classification :not-suitable :reason :operator-gate}
    owned? {:classification :not-suitable :reason :owned}
    (false? active?) {:classification :not-suitable :reason :inactive}
    (= :admitted root-route) {:classification :not-suitable :reason :admitted-root
                              :root-observation-retained true}
    (not (#{:standalone :admitted} root-route))
    {:classification :unknown :reason :root-route-unknown}
    (not source-current?) {:classification :unknown :reason :assessment-stale}
    (not interpretation-valid?) {:classification :unknown :reason :interpretation-invalid}
    (= :preparation increment)
    (cond
      (true? preparation-dependencies-resolved?)
      {:classification :suitable :increment :preparation
       :implementation-admitted false :global-completeness-claimed false}
      (false? preparation-dependencies-resolved?)
      {:classification :not-suitable :reason :preparation-dependency-blocked}
      :else {:classification :unknown :reason :preparation-dependency-unknown})
    (= :implementation increment)
    (cond
      (not preparation-output-qualified?)
      {:classification :unknown :reason :reviewed-contract-not-established}
      (not= :established (:status semantic-negative))
      {:classification :unknown :reason (or (:reason semantic-negative)
                                            :semantic-negative-not-established)}
      (not (true? relevant-dependencies-resolved?))
      {:classification :unknown :reason :relevant-dependency-unresolved}
      :else {:classification :suitable :increment :implementation})
    :else {:classification :unknown :reason :increment-unresolved}))

(defn validate-assessment-response
  "Validate one finite preparation proposal returned through the designated
  contact port. The response proposes work; it does not certify implementation
  or independently review its own future output."
  [ticket proposal response inputs]
  (let [answer (or (:answer response) response)
        expected-path (preparation-contract-path (:id ticket) (:increment-id answer))
        locator (:increment-locator answer)
        locator-valid? (and (= :C3 (:check locator))
                            (= expected-path (:path locator))
                            (string? (:repo locator))
                            (string? (:sha locator))
                            (boolean (re-matches #"[0-9a-f]{40,64}" (:sha locator))))
        ownership (get-in inputs [:ownership-evidence :state])
        requisition (get-in inputs [:operator-evidence :requisition])
        operator-status (get-in inputs [:operator-evidence :status-class])
        operator-allowed? (cond
                            (not= :live operator-status) nil
                            (or (nil? requisition) (contains? requisition :malformed)) nil
                            (contains? requisition :absent) true
                            (contains? #{:in-progress :completed} (:state requisition)) false
                            :else nil)
        bound? (and (= response-schema (:schema answer))
                    (= :preparation (:increment answer))
                    (= (:id ticket) (:ticket-id answer))
                    (= (:source ticket) (:source-pin answer))
                    (= (:proposal-pin answer) (value-digest proposal))
                    (keyword? (:increment-id answer))
                    (vector? (:increment-want answer))
                    locator-valid?)
        resolved-job (when (and (fn? (:contact-resolver response))
                                (string? (:job-id response)))
                       ((:contact-resolver response) (:job-id response)))
        port-bound? (and (string? (:job-id response))
                         (map? (:result-pin response))
                         (= (:job-id response) (:job-id resolved-job))
                         (= (:seat response) (:seat resolved-job))
                         (= (:text response) (:result resolved-job)))
        assessment (classify
                    {:increment :preparation :source-current? true
                     :operator-allowed? operator-allowed?
                     :owned? (case ownership :unowned false
                                      (:active :ambiguous) true nil)
                     :active? (= :live operator-status)
                     :preparation-dependencies-resolved?
                     (case (:preparation-dependencies answer)
                       :satisfied true :blocked false nil)
                     :interpretation-valid? (and bound? port-bound?
                                                  (= :unowned ownership))
                     :root-route (:root-route inputs)})]
    {:increment :preparation
     :increment-id (:increment-id answer)
     :increment-want (:increment-want answer)
     ;; Assessment responses name the public check as :check.  The durable
     ;; selected-action reader uses the established locator vocabulary
     ;; (:class). Carry both so the proposal remains auditable while C3/C4
     ;; execute at the immutable before/after revisions.
     :increment-locator (cond-> locator locator-valid? (assoc :class (:check locator)))
     :proposal-pin (value-digest proposal)
     :contact-provenance (select-keys response [:job-id :seat :result-pin])
     :assessment assessment}))

(defn qualify-preparation-run
  "Qualify a selected preparation increment from the reviewed immutable
  repository state. The C3 observation establishes creation only; this reads
  and validates the created contract and binds it to the actual author and
  reviewer jobs retained by the runner."
  [{:keys [selected-action repository after-revision mission-observation
           selected-want-outcomes author-job review-job review-gate]}]
  (let [readiness (:ticket-readiness selected-action)
        increment-id (:increment-id readiness)
        expected-path (preparation-contract-path (:target selected-action) increment-id)
        locator (get (:observation-locators selected-action)
                     (first (:want selected-action)))
        shown (when (and (= :preparation (:increment readiness))
                         (= expected-path (:path locator))
                         (string? repository) (string? after-revision))
                (shell/sh "git" "-C" repository "show"
                          (str after-revision ":" expected-path)))
        contract (when (zero? (or (:exit shown) 1))
                   (try (edn/read-string (:out shown)) (catch Throwable _ nil)))
        expected {:ticket-id (:target selected-action)
                  :source-pin (:source-pin readiness)
                  :increment-id increment-id}
        author-id (or (:seat author-job) (:agent-id author-job) (:author author-job))
        reviewer-id (or (:seat review-job) (:agent-id review-job) (:author review-job))]
    (cond
      (not= :preparation (:increment readiness)) {:status :not-applicable}
      (not= :admitted (:status mission-observation))
      {:status :refused :reason :preparation-observation-not-admitted}
      (not= :verified (:status selected-want-outcomes))
      {:status :refused :reason :preparation-want-not-accounted}
      (not (every? #(= :reached (:classification %)) (:outcomes selected-want-outcomes)))
      {:status :refused :reason :preparation-artifact-not-created}
      (not (map? contract)) {:status :refused :reason :contract-missing-or-unreadable}
      (not= contract-schema (:schema contract)) {:status :refused :reason :contract-malformed}
      (not= expected (select-keys contract (keys expected)))
      {:status :refused :reason :contract-binding-mismatch}
      (not (and (seq (:positive-cases contract)) (seq (:negative-cases contract))))
      {:status :refused :reason :contract-cases-empty}
      (not (:passed? review-gate)) {:status :refused :reason :review-not-accepted}
      (or (str/blank? (str author-id)) (str/blank? (str reviewer-id))
          (= (str author-id) (str reviewer-id)))
      {:status :refused :reason :review-not-independent}
      :else {:status :qualified :revision after-revision
             :contract {:path expected-path :sha256 (value-digest contract)}
             :author-job (:job-id author-job) :review-job (:job-id review-job)
             :reviewer (str reviewer-id)})))

(defn ordered-population
  "Fair canonical population: least recently attempted, then ticket id."
  [tickets attempts]
  (sort-by (fn [{:keys [id]}] [(or (get attempts id) "") id]) tickets))

(defn- atomic-spit! [path value]
  (let [target (io/file path) parent (.getParentFile target)
        _ (.mkdirs parent)
        tmp (io/file parent (str "." (.getName target) "." (random-uuid) ".tmp"))]
    (spit tmp (str (pr-str value) "\n"))
    (Files/move (.toPath tmp) (.toPath target)
                (into-array StandardCopyOption
                            [StandardCopyOption/ATOMIC_MOVE StandardCopyOption/REPLACE_EXISTING]))
    path))

(defn retain-contact-result!
  "Retain the actual designated contact result before its claims are decoded."
  [manifest-path {:keys [job-id seat text] :as response}]
  (when (and (string? job-id) (string? text))
    (let [path (str manifest-path ".contacts/" job-id ".edn")
          value {:schema :wm/ticket-readiness-contact-v1 :job-id job-id
                 :seat seat :result text :response-state (:state response)}]
      (atomic-spit! path value)
      (file-pin path))))

(defn read-manifest [path]
  (let [value (if (.isFile (io/file path)) (read-edn path)
                  {:schema manifest-schema :entries [] :attempts {}})]
    (when-not (and (= manifest-schema (:schema value))
                   (vector? (:entries value)) (map? (:attempts value)))
      (throw (ex-info "Ticket readiness manifest invalid"
                      {:reason :manifest-invalid :path path})))
    value))

(defn- valid-current-inputs? [inputs]
  (and (map? inputs)
       (every? #(contains? inputs %)
               [:source :dependency-evidence :ownership-evidence
                :implementation-evidence :operator-evidence :root-route])))

(defn- authoritative-contact? [record contact-resolver]
  (try
    (let [{:keys [job-id result-pin]} (:contact-provenance record)
          retained (when (pin-matches? result-pin) (read-edn (:path result-pin)))
          job (when (and (fn? contact-resolver) (string? job-id))
                (contact-resolver job-id))]
      (and (= :wm/ticket-readiness-contact-v1 (:schema retained))
           (= job-id (:job-id retained)) (= job-id (:job-id job))
           (= (:seat retained) (:seat job)) (= (:result retained) (:result job))))
    (catch Throwable _ false)))

(defn- current-entry? [entry current-inputs contact-resolver]
  (when (and (valid-current-inputs? current-inputs)
             (pin-matches? (:assessment-pin entry)))
    (let [record (read-edn (get-in entry [:assessment-pin :path]))]
      (and (= schema (:schema record))
           (= (:ticket-id entry) (:ticket-id record))
           (= (:source-pin entry) (:source-pin record))
           (= (:increment entry) (:increment record))
           (= (:increment-id entry) (:increment-id record))
           (= (:increment-want entry) (:increment-want record))
           (= (:increment-locator entry) (:increment-locator record))
           (= (:assessment entry) (:assessment record))
           (= (:input-identity entry) (value-digest current-inputs))
           (= current-inputs (:inputs record))
           (authoritative-contact? record contact-resolver)))))

(defn suitable-section
  ([manifest current-inputs] (suitable-section manifest current-inputs nil))
  ([manifest current-inputs contact-resolver]
   {:items (->> (:entries manifest)
                (filter #(and (= :suitable (get-in % [:assessment :classification]))
                              (current-entry? % (get current-inputs (:ticket-id %))
                                              contact-resolver)))
                (mapv (fn [e] {:ticket-id (:ticket-id e)
                                :source-pin (:source-pin e)
                                :increment (:increment e)
                                :increment-id (:increment-id e)
                                :increment-want (:increment-want e)
                                :increment-locator (:increment-locator e)
                                :assessment-pin (:assessment-pin e)})))}))

(defn refresh-one!
  "Contact and durably assess at most one canonical ticket.  PREPARE-FN and
  ANSWER-FN are the existing proposal/contact seams supplied by the caller.
  Preview callers must not invoke this function."
  [{:keys [tickets manifest-path prepare-fn answer-fn validate-fn input-fn now-fn
           contact-resolver]
    :or {now-fn #(str (Instant/now))}}]
  (let [manifest (read-manifest manifest-path)
        current (into {} (map (juxt :ticket-id identity) (:entries manifest)))
        candidates (remove (fn [{:keys [id] :as ticket}]
                             (let [entry (get current id)
                                   inputs (input-fn ticket)
                                   classification (get-in entry [:assessment :classification])]
                               (and (#{:suitable :completed} classification)
                                    (current-entry? entry inputs contact-resolver))))
                           (ordered-population tickets (:attempts manifest)))]
    (if-let [ticket (first candidates)]
      (let [before-inputs (input-fn ticket)
            proposal (prepare-fn ticket)
            response (answer-fn proposal)
            inputs (input-fn ticket)
            assessed (validate-fn ticket proposal response)
            assessed (if (= before-inputs inputs)
                       assessed
                       (assoc assessed :assessment
                              {:classification :unknown
                               :reason :inputs-changed-during-contact}))
            at (now-fn)
            record (assoc assessed :schema schema :ticket-id (:id ticket)
                          :source-pin (:source ticket) :inputs inputs :assessed-at at
                          :external-reply-provenance (:provenance response))
            record-path (str manifest-path ".records/" (:id ticket) "-"
                             (subs (:sha256 (:source ticket)) 0 12) "-"
                             (subs (value-digest record) 0 20) ".edn")
            _ (atomic-spit! record-path record)
            entry (assoc (select-keys record [:ticket-id :source-pin :increment :increment-id :increment-want
                                              :increment-locator :assessment])
                         :input-identity (value-digest inputs)
                         :assessment-pin (file-pin record-path))
            entries (conj (vec (remove #(= (:ticket-id %) (:id ticket)) (:entries manifest))) entry)
            next-manifest (assoc manifest :schema manifest-schema :entries entries
                                 :attempts (assoc (:attempts manifest) (:id ticket) at))]
        (atomic-spit! manifest-path next-manifest)
        {:status :assessed :ticket-id (:id ticket) :assessment (:assessment record)
         :record-pin (file-pin record-path) :manifest-pin (file-pin manifest-path)})
      {:status :no-unassessed-ticket :manifest-pin (when (.isFile (io/file manifest-path))
                                                     (file-pin manifest-path))})))
