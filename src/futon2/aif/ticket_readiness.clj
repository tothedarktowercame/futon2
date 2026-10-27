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

(defn- file-bytes [path] (Files/readAllBytes (.toPath (io/file path))))
(defn file-pin [path]
  {:path (.getCanonicalPath (io/file path))
   :sha256 (load-identity/sha256 (file-bytes path))})

(defn- pin-matches? [{:keys [path sha256]}]
  (and (string? path) (.isFile (io/file path))
       (= sha256 (:sha256 (file-pin path)))))

(defn- read-edn [path] (edn/read-string (slurp path)))

(defn semantic-negative
  "Admit a retained semantic red only when it ran the named contract failure
  at the immutable before revision.  C8=false is deliberately not an input."
  [{:keys [record expected-revision contract-pin failure-id]}]
  (cond
    (not (map? record)) {:status :unknown :reason :semantic-negative-not-established}
    (not= :executed (:status record))
    {:status :unknown :reason :semantic-negative-not-established}
    (not= expected-revision (:revision record))
    {:status :unknown :reason :before-revision-mismatch}
    (not= contract-pin (:contract-pin record))
    {:status :unknown :reason :semantic-negative-not-established}
    (not= failure-id (:failure-id record))
    {:status :unknown :reason :semantic-negative-not-established}
    (not (and (pin-matches? (:log-pin record))
              (pin-matches? (:postcheck-pin record))))
    {:status :unknown :reason :semantic-negative-not-established}
    :else {:status :established :revision expected-revision
           :failure-id failure-id :contract-pin contract-pin}))

(defn validate-preparation-output
  "Validate contract contents and a separately bound independent review.
  Existence alone (C3) and a producer's self-asserted reviewer never qualify."
  [{:keys [contract-pin review-pin ticket-id source-pin increment-id]}]
  (try
    (let [contract (when (pin-matches? contract-pin) (read-edn (:path contract-pin)))
          review (when (pin-matches? review-pin) (read-edn (:path review-pin)))
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
        (or (str/blank? (:reviewer review)) (= (:reviewer review) (:producer contract)))
        {:status :refused :reason :review-not-independent}
        (not= :accepted (:verdict review)) {:status :refused :reason :review-not-accepted}
        :else {:status :qualified :contract-pin contract-pin :review-pin review-pin}))
    (catch Throwable e
      {:status :refused :reason :contract-output-unreadable :message (ex-message e)})))

(defn classify
  "Classify one increment. Explicit operational blocks precede unknowns.
  Unknown implementation dependencies do not block contract preparation."
  [{:keys [increment source-current? increment-effect? ticket-completed?
           operator-allowed? owned? active? preparation-dependencies-resolved?
           relevant-dependencies-resolved? interpretation-valid? root-route
           semantic-negative after-revision expected-after-revision
           preparation-output-qualified?]}]
  (cond
    increment-effect?
    (if (and expected-after-revision (not= expected-after-revision after-revision))
      {:classification :unknown :reason :after-revision-mismatch}
      {:classification :completed :increment-completed? true
       :ticket-completed? (boolean ticket-completed?)
       :next-disposition (when-not ticket-completed? :assess-remaining-increments)})
    (false? operator-allowed?) {:classification :not-suitable :reason :operator-gate}
    owned? {:classification :not-suitable :reason :owned}
    (false? active?) {:classification :not-suitable :reason :inactive}
    (= :admitted root-route) {:classification :not-suitable :reason :admitted-root
                              :root-observation-retained true}
    (not source-current?) {:classification :unknown :reason :assessment-stale}
    (not interpretation-valid?) {:classification :unknown :reason :interpretation-invalid}
    (= :preparation increment)
    (if (false? preparation-dependencies-resolved?)
      {:classification :not-suitable :reason :preparation-dependency-blocked}
      {:classification :suitable :increment :preparation
       :implementation-admitted false :global-completeness-claimed false})
    (= :implementation increment)
    (cond
      (not preparation-output-qualified?)
      {:classification :unknown :reason :reviewed-contract-not-established}
      (not= :established (:status semantic-negative))
      (select-keys semantic-negative [:classification :reason])
      (not relevant-dependencies-resolved?)
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
  (if (.isFile (io/file path)) (read-edn path)
      {:schema manifest-schema :entries [] :attempts {}}))

(defn suitable-section [manifest]
  {:items (->> (:entries manifest)
               (filter #(= :suitable (get-in % [:assessment :classification])))
               (mapv (fn [e] {:ticket-id (:ticket-id e)
                               :increment (:increment e)
                               :increment-want (:increment-want e)
                               :assessment-pin (:assessment-pin e)})))})

(defn refresh-one!
  "Contact and durably assess at most one canonical ticket.  PREPARE-FN and
  ANSWER-FN are the existing proposal/contact seams supplied by the caller.
  Preview callers must not invoke this function."
  [{:keys [tickets manifest-path prepare-fn answer-fn validate-fn now-fn]
    :or {now-fn #(str (Instant/now))}}]
  (let [manifest (read-manifest manifest-path)
        current (into {} (map (juxt :ticket-id identity) (:entries manifest)))
        candidates (remove (fn [{:keys [id source]}]
                             (= (:source-pin (get current id)) source))
                           (ordered-population tickets (:attempts manifest)))]
    (if-let [ticket (first candidates)]
      (let [proposal (prepare-fn ticket)
            response (answer-fn proposal)
            assessed (validate-fn ticket proposal response)
            at (now-fn)
            record (assoc assessed :schema schema :ticket-id (:id ticket)
                          :source-pin (:source ticket) :assessed-at at
                          :external-reply-provenance (:provenance response))
            record-path (str manifest-path ".records/" (:id ticket) "-" (subs (:sha256 (:source ticket)) 0 12) ".edn")
            _ (atomic-spit! record-path record)
            entry (assoc (select-keys record [:ticket-id :source-pin :increment :increment-want :assessment])
                         :assessment-pin (file-pin record-path))
            entries (conj (vec (remove #(= (:ticket-id %) (:id ticket)) (:entries manifest))) entry)
            next-manifest (assoc manifest :schema manifest-schema :entries entries
                                 :attempts (assoc (:attempts manifest) (:id ticket) at))]
        (atomic-spit! manifest-path next-manifest)
        {:status :assessed :ticket-id (:id ticket) :assessment (:assessment record)
         :record-pin (file-pin record-path) :manifest-pin (file-pin manifest-path)})
      {:status :no-unassessed-ticket :manifest-pin (when (.isFile (io/file manifest-path))
                                                     (file-pin manifest-path))})))
