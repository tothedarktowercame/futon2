(ns futon2.aif.interpretation-job
  "One interpretation dispatch per attempt. Packet 3b owns successor recovery:
  never rebind an old receipt, reopen a close, or recover it as a code commit."
  (:refer-clojure :exclude [run! bytes])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.interpretation-request :as request]
            [futon2.aif.fact-measurement :as measurement])
  (:import [java.nio.file Files StandardOpenOption]
           [java.time Instant]
           [java.util Base64 UUID]))

(defn- refuse! [kind data]
  (throw (ex-info "Interpretation job refused" (assoc data :interpretation/refusal kind))))
(defn- need! [x kind data] (when-not x (refuse! kind data)))
(defn- bytes [file] (Files/readAllBytes (.toPath (io/file file))))
(defn- write-new! [file x]
  (Files/write (.toPath (io/file file)) (.getBytes (pr-str x) "UTF-8")
               (into-array StandardOpenOption [StandardOpenOption/CREATE_NEW StandardOpenOption/WRITE])))
(defn source [file]
  (let [f (io/file file) path (.getCanonicalPath f) sha (evidence/sha256 (bytes f))]
    {:id (str path "#" sha) :path path :file (.getName f) :sha256 sha :revision "interpretation-job-artifact-v1"}))
(defn- read-record [file]
  (with-open [reader (java.io.PushbackReader. (io/reader file))]
    (let [end (Object.) r (edn/read {:eof end} reader)]
      (need! (and (map? r) (identical? end (edn/read {:eof end} reader)))
             :interpretation/invalid-receipt {:reason :not-one-edn-record}) r)))
(defn- queue-time [job]
  (try (- (.toEpochMilli (Instant/parse (:started-at job)))
          (.toEpochMilli (Instant/parse (:created-at job))))
       (catch Exception _ {:status :none :reason :agency-timestamps-unavailable})))

(defn prompt [dir]
  (str "INTERPRETATION ONLY. Read " (io/file dir "interpretation-request.source")
       " and interpretation-job.source in the same directory. Wait for the job file if dispatch publication is still completing. "
       "Use its exact identity including interpreter-job. Read the pinned .source bytes, not mutable mission/library files. "
       "Write one EDN :wm/interpreted-pattern-set-v1 receipt to " (io/file dir "interpretation-receipt.edn") ". "
       "Read src/futon2/aif/interpretation_evidence.clj for the strict schemas. No prose-only completion. "
       "Copy the exact target fields id/kind/action/source/citations/pinned-at; tension-rule is retained in the request source. "
       "Judge EVERY original normalized candidate, in order, relevant or not, with a reason and mission/pattern citations. "
       "A citation is {:source source-id :lines [first last] :quote exact-captured-lines}; keep whitespace. "
       "Name facts with true/false/:unknown, meaning, scope, observed-at, method, citations. Lack of evidence means unknown. "
       "Never add facts merely to enable a guard. Interpret relevant patterns with :fact/:not/:and/:or guard vectors over "
       "declared facts and Boolean effects over those same facts; quote IF/HOWEVER/THEN, cite captured index membership, "
       "authority :documented-interpretation, your author identity, and value-digest of each interpretation excluding :sha256. "
       "These are intended effects, not guaranteed success probabilities. Record holes instead of defaults. "
       "Whole-section retrieval can be weak: you may search the captured library and append conformant runs named agent-search; "
       "judge those candidates too. Preserve each original run's retriever/version/index-source/parameters, stripping intermediate "
       "row-failures/raw/retriever-rank/unjudged fields when forming schema fields; their raw evidence stays in the request. "
       "At most ONE new pattern: only for a named missing move, author library .flexiarg plus index row and commit explicit paths, "
       "then capture the new pattern/index bytes as additional .source companions and cite them. Never overwrite original snapshots. "
       "Include every existing .source companion in receipt :sources (id/path/file/sha256/revision), including request, intent and job files. "
       "In this SAME job write interpretation-reader-plan.edn: a vector with at most one entry per declared fact, "
       "each {:fact id :reader kind :parameters map :citations [exact-span-citations]}. Read fact_measurement.clj "
       "for the strict parameter maps and literal predicates. Plan only justified independent measurements; omit unmeasurable facts. "
       "Use [] when none can be measured. Include the plan as a hashed receipt source. Do not use outcomes, grounded?, priors, "
       "controller scores or predicted effects as evidence. Freeze this plan before construction/build; never revise it after the result. "
       "Use evidence/value-digest for hashes. Do not include additional top-level fields. "
       "If blocked, write :wm/interpretation-failure-v1 with the exact identity, stage, sources, typed absent sections and failure map; "
       "use interpretation/no-relevant-pattern, genesis-required or unmeasurable-fact as appropriate. No build, reload, click, push, "
       "or claimed goal discharge. A pending request is not a successful interpretation."))

(defn run!
  "Ports are the runner's existing readiness/dispatch/poll and turn counter.
  Timed-out jobs continue; immutable failure/intent/job records feed packet 3b."
  [opts action identity dir {:keys [ready! dispatch! poll! charge! construct!]}]
  (.mkdirs (io/file dir))
  (let [started (System/currentTimeMillis)
        actor (or (:interpreter opts) (:author opts))
        identity (atom (assoc identity :author actor))
        captured (atom {})
        prepared (atom nil)
        returned-record (atom nil)
        stage (atom :prepare)
        validation-started (atom nil)
        timing (atom {:queue-ms {:status :none :reason :not-dispatched} :validation-ms 0})
        capture! (fn [s] (swap! captured assoc (:file s) s))
        save! (fn [file value]
                (let [path (io/file dir file)] (write-new! path value) (capture! (source path))))
        receipt (io/file dir "interpretation-receipt.edn")]
    (try
      (need! (#{:advance-mission :open-mission :advance-ticket} (:type action))
             :interpretation/action-type-unsupported {:action action})
      (reset! prepared ((or (:interpretation-prepare-fn opts) request/prepare!)
                       action @identity {:on-capture capture!}))
      (doseq [s (:sources @prepared)] (capture! s))
      (save! "interpretation-request.source" @prepared)
      (reset! stage :readiness)
      (ready! actor)
      (reset! stage :dispatch)
      (need! (not (.exists (io/file dir "interpretation-intent.source")))
             :interpretation/job-already-dispatched {})
      (save! "interpretation-intent.source" {:identity @identity :at (str (Instant/now))})
      (charge!)
      (let [response (dispatch! actor (prompt dir))
            id (:job-id response)]
        (need! (and (string? id) (not (str/blank? id))) :interpretation/agent-unavailable {:response response})
        (swap! identity assoc :interpreter-job id)
        (save! "interpretation-job.source" {:identity @identity :response response})
        (reset! stage :wait)
        (let [job (poll! id)]
          (swap! timing assoc :queue-ms (queue-time job))
          (need! (= "done" (:state job)) :interpretation/agent-unavailable {:job job})
          (reset! stage :validation)
          (reset! validation-started (System/currentTimeMillis))
          (let [validation-start (System/currentTimeMillis)
                record (read-record receipt)
                _ (evidence/validate-record record)
                reader (fn [file]
                         (need! (and (string? file) (= file (.getName (io/file file)))
                                     (not (#{"." ".."} file))) :interpretation/invalid-receipt {:file file})
                         (bytes (io/file dir file)))
                _ (evidence/validate-sources! record reader)
                _ (need! (= @identity (:identity record)) :interpretation/invalid-receipt {:reason :identity-mismatch})
                _ (doseq [s (vals @captured)]
                    (need! (some #{s} (:sources record)) :interpretation/source-changed {:source (:id s)}))]
            (reset! returned-record record)
            (doseq [s (:sources record)] (capture! s))
            (swap! timing assoc :validation-ms (- (System/currentTimeMillis) validation-start))
            (when-let [failure (:failure record)]
              (throw (ex-info "Interpreter reported a typed failure" {:interpretation/refusal (:kind failure)
                                                                      :reported-absent (:absent record)})))
            (need! (= :wm/interpreted-pattern-set-v1 (:schema record)) :interpretation/invalid-receipt {})
            (need! (= (select-keys (:target @prepared) [:id :kind :action :source :citations :pinned-at])
                       (:target record)) :interpretation/invalid-receipt {:reason :target-mismatch})
            (let [original (get-in @prepared [:retrieval :runs]) returned (get-in record [:retrieval :runs])]
              (doseq [[before after] (map vector original returned)]
                (need! (and (= (select-keys before [:retriever :version :index-source :parameters])
                               (select-keys after [:retriever :version :index-source :parameters]))
                            (= (mapv :pattern (:candidates before)) (mapv :pattern (:candidates after))))
                       :interpretation/invalid-receipt {:reason :candidate-coverage-mismatch}))
              (need! (and (<= (count original) (count returned))
                          (every? #(= "agent-search" (:retriever %)) (drop (count original) returned)))
                     :interpretation/invalid-receipt {:reason :retrieval-runs-mismatch}))
            (need! (seq (:interpretations record)) :interpretation/no-relevant-pattern {})
            (need! (<= (count (:genesis record)) 1) :interpretation/genesis-required {:reason :genesis-limit})
            (measurement/validate-plan! record reader (:author opts))
            (let [construction (when construct!
                                 (reset! stage :construction)
                                 (let [began (System/currentTimeMillis)]
                                   (try (construct! record reader)
                                        (finally (swap! timing assoc :construction-ms
                                                        (- (System/currentTimeMillis) began))))))]
              (cond-> {:receipt {:file (.getName receipt) :sha256 (evidence/sha256 (bytes receipt))}
                       :timing (assoc @timing :elapsed-ms (- (System/currentTimeMillis) started))}
                construct! (assoc :construction construction))))))
      (catch Exception e
        (let [_ (when (and @validation-started (= :validation @stage))
                  (swap! timing assoc :validation-ms (- (System/currentTimeMillis) @validation-started)))
              data (ex-data e)
              kind (cond (= :o4 (:law data)) :interpretation/no-relevant-pattern
                         (= :agent-budget-expired (:failure-kind data)) :interpretation/budget-exceeded
                         (:interpretation/refusal data) (:interpretation/refusal data)
                         (= :source-digest-mismatch (:interpretation-evidence/refusal data)) :interpretation/source-changed
                         (= :readiness @stage) :interpretation/agent-unavailable
                         :else :interpretation/invalid-receipt)
              kind (if (contains? evidence/failure-kinds kind) kind :interpretation/invalid-receipt)
              _ (when (.exists receipt)
                  ;; Invalid or failed receipt bytes remain evidence, not an admitted success record.
                  (let [path (io/file dir (str "interpretation-return-" (UUID/randomUUID) ".source"))
                        returned (bytes receipt)]
                      ;; Admission identifies records by schema, not extension.
                      ;; Preserve rejected bytes losslessly as an artifact, without
                      ;; presenting them as another admitted receipt.
                      (write-new! path {:returned-sha256 (evidence/sha256 returned)
                                       :returned-bytes-base64 (.encodeToString (Base64/getEncoder) returned)})
                      (capture! (source path))
                      (Files/delete (.toPath receipt))))
              _ (let [plan (io/file dir measurement/plan-file)]
                  (when (.exists plan) (capture! (source plan))))
              _ (doseq [s (or (:sources data) (get-in data [:request :sources]))] (capture! s))
              _ (when (and (nil? @prepared) (:request data)) (reset! prepared (:request data)))
              elapsed (- (System/currentTimeMillis) started)
              _ (save! (str "interpretation-diagnostic-" (UUID/randomUUID) ".source")
                       {:kind kind :stage @stage :error (.getMessage e) :data data
                        :expected-sources (vec (vals @captured))
                        :timing (assoc @timing :elapsed-ms elapsed)})
              _ (doseq [[file s] @captured]
                  (let [path (io/file dir file)]
                    (if-not (.exists path)
                      (swap! captured dissoc file)
                      (when-not (= (:sha256 s) (evidence/sha256 (bytes path)))
                        ;; The old digest remains in the diagnostic. These bytes
                        ;; are observed failure evidence, never a repaired success pin.
                        (capture! (source path))))))
              absent (or (:reported-absent data)
                         (into {} (for [section [:target :retrieval :query :facts :interpretations :genesis]
                                        :when (case section
                                                :target (nil? (:target @prepared))
                                                :retrieval (nil? (:retrieval @prepared))
                                                :query (nil? (get-in @prepared [:retrieval :query]))
                                                (not (contains? @returned-record section)))]
                                    [section {:status :none :reason kind}])))
              record {:schema evidence/failure-schema :identity @identity :stage @stage
                      :sources (vec (vals @captured)) :absent absent
                      :failure {:kind kind :identity @identity :stage @stage :source-refs []
                                :elapsed-ms elapsed :partial-artifacts (mapv :id (vals @captured))}}
              path (io/file dir (str "interpretation-failure-" (UUID/randomUUID) ".edn"))]
          (evidence/validate-sources! record #(bytes (io/file dir %)))
          (write-new! path record)
          (throw (ex-info "Interpretation stopped construction"
                          {:outcome (if (= kind :interpretation/agent-unavailable) :agent-unavailable :incomplete)
                           :failure-kind (case kind :interpretation/budget-exceeded :agent-budget-expired
                                               :interpretation/agent-unavailable :agent-unavailable kind)
                           :failure-stage :interpretation
                           :job-id (when (string? (:interpreter-job @identity)) (:interpreter-job @identity))
                           :interpretation/failure kind :interpretation-record (.getAbsolutePath path)
                           :interpretation/timing (assoc @timing :elapsed-ms elapsed)
                           :recovery-todo :packet-3b-original-receipt-successor-binding}
                          e)))))))
