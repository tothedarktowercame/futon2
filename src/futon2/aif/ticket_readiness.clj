(ns futon2.aif.ticket-readiness
  "Durable, source-bound readiness assessments for canonical tickets.

  Readiness is an assessment, never a lifecycle state.  Preparation and
  implementation are distinct increments; generic C3/C8 meanings are not
  changed here."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.load-identity :as load-identity])
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
  [{:keys [registry-pin expected-revision contract-pin failure-id
           expected-code-pin expected-test-pin]}]
  (try
    (if-not (pin-matches? registry-pin)
      {:status :unknown :reason :semantic-negative-not-established}
      (let [record (read-edn (:path registry-pin))
            log (when (pin-matches? (:log-pin record))
                  (read-edn (get-in record [:log-pin :path])))
            postcheck (when (pin-matches? (:postcheck-pin record))
                        (read-edn (get-in record [:postcheck-pin :path])))
            relevant? (some #(= failure-id (:id %)) (:failures log))]
        (cond
          (or (not= registry-schema (:schema record))
              (not= :executed (:status record)))
          {:status :unknown :reason :semantic-negative-not-established}
          (not= expected-revision (:revision record))
          {:status :unknown :reason :before-revision-mismatch}
          (or (not= contract-pin (:contract-pin record))
              (not= expected-code-pin (:code-pin record))
              (not= expected-test-pin (:test-pin record)))
          {:status :unknown :reason :semantic-negative-not-established}
          (or (not= semantic-log-schema (:schema log)) (not relevant?))
          {:status :unknown :reason :semantic-negative-not-established}
          (or (not= postcheck-schema (:schema postcheck))
              (not= :failed (:status postcheck))
              (not= expected-revision (:revision postcheck))
              (not= failure-id (:failure-id postcheck)))
          {:status :unknown :reason :semantic-negative-not-established}
          :else {:status :established :revision expected-revision
                 :failure-id failure-id :contract-pin contract-pin
                 :code-pin expected-code-pin :test-pin expected-test-pin})))
    (catch Throwable _
      {:status :unknown :reason :semantic-negative-not-established})))

(defn validate-preparation-output
  "Validate contract contents and a separately bound independent review.
  Existence alone (C3) and a producer's self-asserted reviewer never qualify."
  [{:keys [contract-pin review-pin review-job-pin review-result-pin
           authorized-reviewers ticket-id source-pin increment-id]}]
  (try
    (let [contract (when (pin-matches? contract-pin) (read-edn (:path contract-pin)))
          review (when (pin-matches? review-pin) (read-edn (:path review-pin)))
          job (when (pin-matches? review-job-pin) (read-edn (:path review-job-pin)))
          result (when (pin-matches? review-result-pin) (read-edn (:path review-result-pin)))
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
            (not (contains? (set authorized-reviewers) (:reviewer review))))
        {:status :refused :reason :review-not-independent}
        (or (not= (:job-id review) (:job-id job))
            (not= (:reviewer review) (:seat job))
            (not= (:job-id job) (:job-id result))
            (not= (:result-sha256 review) (value-digest (:result result)))
            (not= contract-pin (:contract-pin result))
            (not= source-pin (:source-pin result)))
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
    (if (or (not= expected-after-revision after-revision)
            (not= after-revision (:revision completion-evidence)))
      {:classification :unknown :reason :after-revision-mismatch}
      {:classification :completed :increment-completed? true
       :ticket-completed? (boolean ticket-completed?)
       :next-disposition (when-not ticket-completed? :assess-remaining-increments)})
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
    (if (true? preparation-dependencies-resolved?)
      {:classification :suitable :increment :preparation
       :implementation-admitted false :global-completeness-claimed false}
      {:classification :not-suitable :reason :preparation-dependency-blocked}
      )
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

(defn- current-entry? [entry current-inputs]
  (and (valid-current-inputs? current-inputs)
       (pin-matches? (:assessment-pin entry))
       (= (:input-identity entry) (value-digest current-inputs))
       (= current-inputs
          (:inputs (read-edn (get-in entry [:assessment-pin :path]))))))

(defn suitable-section [manifest current-inputs]
  {:items (->> (:entries manifest)
               (filter #(and (= :suitable (get-in % [:assessment :classification]))
                             (current-entry? % (get current-inputs (:ticket-id %)))))
               (mapv (fn [e] {:ticket-id (:ticket-id e)
                               :increment (:increment e)
                               :increment-want (:increment-want e)
                               :assessment-pin (:assessment-pin e)})))})

(defn refresh-one!
  "Contact and durably assess at most one canonical ticket.  PREPARE-FN and
  ANSWER-FN are the existing proposal/contact seams supplied by the caller.
  Preview callers must not invoke this function."
  [{:keys [tickets manifest-path prepare-fn answer-fn validate-fn input-fn now-fn]
    :or {now-fn #(str (Instant/now))}}]
  (let [manifest (read-manifest manifest-path)
        current (into {} (map (juxt :ticket-id identity) (:entries manifest)))
        candidates (remove (fn [{:keys [id] :as ticket}]
                             (let [entry (get current id)
                                   inputs (input-fn ticket)
                                   classification (get-in entry [:assessment :classification])]
                               (and (#{:suitable :completed} classification)
                                    (current-entry? entry inputs))))
                           (ordered-population tickets (:attempts manifest)))]
    (if-let [ticket (first candidates)]
      (let [proposal (prepare-fn ticket)
            response (answer-fn proposal)
            inputs (input-fn ticket)
            assessed (validate-fn ticket proposal response)
            at (now-fn)
            record (assoc assessed :schema schema :ticket-id (:id ticket)
                          :source-pin (:source ticket) :inputs inputs :assessed-at at
                          :external-reply-provenance (:provenance response))
            record-path (str manifest-path ".records/" (:id ticket) "-" (subs (:sha256 (:source ticket)) 0 12) ".edn")
            _ (atomic-spit! record-path record)
            entry (assoc (select-keys record [:ticket-id :source-pin :increment :increment-want :assessment])
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
