(ns futon2.aif.full-loop-runtime
  "Production composition root for the full-loop runner."
  (:require [cheshire.core :as json]
            [futon2.aif.wm.cascade-patch-delivery :as patch-delivery]
            [futon2.aif.wm.cascade-capability-dispatch :as capability-dispatch]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.cascade-feedback :as cascade-feedback]
            [futon2.aif.cascade-revision-producer :as cascade-revision-producer]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.flight-runner :as flight-runner]
            [futon2.aif.focus-receipt :as focus-receipt]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-pipeline-selector :as meta-pipeline]
            [futon2.aif.meta-field-observation :as meta-field]
            [futon2.aif.meta-injury-store :as injury-store]
            [futon2.aif.meta-medic-authority :as medic-authority]
            [futon2.aif.meta-self-heal-production :as meta-self-heal]
            [futon2.aif.outer-learning-manifest :as outer-learning-manifest]
            [futon2.aif.outer-observation :as outer-observation]
            [futon2.aif.outer-select :as outer-select]
            [futon2.aif.outer-state :as outer-state]
            [futon2.aif.outer-pending-observation :as pending-observation]
            [futon2.aif.c-vector :as cv]
            [futon2.aif.mission-registry :as mission-registry]
            [futon2.aif.selection-timing :as selection-timing]
            [futon2.aif.selected-target-reading :as selected-target-reading]
            [futon2.aif.occurrence-square-authoring :as square-authoring]
            [futon2.aif.selected-reading :as selected-reading]
            [futon2.aif.handoff-contract :as handoff]
            [futon2.aif.interpretation-request :as interpretation-request]
            [futon2.aif.ticket-readiness :as ticket-readiness]
            [futon2.aif.ticket-readiness-inputs :as readiness-inputs]
            [futon2.aif.wm.click-ask :as click-ask]
            [futon2.aif.wm.open-cascade-resume :as open-resume]
            [futon2.aif.wm.on-prod-history :as on-prod-history]
            [futon2.aif.wm.cascade-continuation :as cascade-continuation]
            [futon2.aif.wm.cascade-decision :as cascade-decision]
            [futon2.report.war-machine :as wm])
  (:import [java.time Instant]
           [java.util UUID]))

(load-identity/register! *ns* *file*)

(def default-pattern-graph-path
  "/home/joe/code/storage/operator-turns/mined-pattern-graph.json")
(def default-pattern-graph-diff-dir
  "/home/joe/code/futon2/data/wm-pattern-graph-diffs")
(def default-reading-precision-campaign-path
  "/home/joe/code/futon2/resources/wm/reading-precision-beta/wm-readiness-2026-10-17-campaign.edn")
(def default-cascade-continuation-close-root
  "/home/joe/code/futon2/data/wm-cascade-continuation-closes")
(def default-cascade-continuation-failure-root
  "/home/joe/code/futon2/data/wm-cascade-continuation-failures")
(def default-ticket-readiness-manifest
  "/home/joe/code/futon2/data/wm-ticket-readiness/manifest.edn")
(def default-ticket-readiness-prompt-budget-chars
  ;; futon3c.agents.zai-api/default-context-policy records 128000 as the
  ;; conservative floor used when the provider publishes no measured limit.
  ;; A character bound at that same floor is stricter for ordinary prose.
  128000)
(def default-ticket-readiness-source-summary-limit 16)
(def default-ticket-readiness-citation-limit 4)
(def default-ticket-readiness-citation-chars 4096)
(def default-ticket-readiness-candidate-limit 16)
(def default-ticket-readiness-candidate-chars 1024)
(def default-giraffe-root "/home/joe/code/storage/giraffe")
(def self-heal-runtime-config-resource "wm/self-heal-runtime.edn")

(defn self-heal-runtime-config
  "Read the declared production limits for the dedicated self-heal route.
  Missing or malformed declarations remain absent so the consumer returns its
  typed configuration refusal."
  []
  (try
    (let [resource (io/resource self-heal-runtime-config-resource)
          config (when resource (edn/read-string (slurp resource)))]
      (if (and (= :wm/self-heal-runtime-v1 (:schema config))
               (nat-int? (:acceptance-static-correction-limit config))
               (nat-int? (:acceptance-return-limit config))
               (boolean? (:cause-scope-required? config)))
        config
        {}))
    (catch Throwable _ {})))

(defn production-self-heal-options
  "Compose declared self-heal runtime limits into the options consumed by
  prepare-acceptance. This is the serving composition boundary."
  [opts]
  (let [config (self-heal-runtime-config)]
    (cond-> opts
      (contains? config :acceptance-static-correction-limit)
      (assoc :meta-acceptance-static-correction-limit
             (:acceptance-static-correction-limit config))
      (contains? config :acceptance-return-limit)
      (assoc :meta-acceptance-return-limit
             (:acceptance-return-limit config))
      (contains? config :cause-scope-required?)
      (assoc :meta-cause-scope-required? (:cause-scope-required? config)))))

(def budget-observation-schema :wm/meta-click-budget-observation-v1)

(defn- resolve-medic-authority [opts]
  (cond
    (contains? opts :meta-medic-authority-resolution)
    (let [resolution (:meta-medic-authority-resolution opts)]
      (if (medic-authority/resolved-context? resolution)
        {:status :ready
         :identity (medic-authority/resolution-identity resolution)
         :verifier (medic-authority/resolution-verifier resolution)}
        {:status :refused :reason :meta-self-heal/medic-authority-unavailable}))

    (contains? opts :meta-medic-authority-context)
    (let [resolved (medic-authority/resolve-context
                    (:meta-medic-authority-context opts))]
      (if (= :ready (:status resolved))
        {:status :ready :identity (:identity resolved)
         :verifier (medic-authority/resolution-verifier (:resolution resolved))}
        resolved))

    :else {:status :absent
           ;; Standing retained authority is the same per-grammar union used
           ;; by every resolved context (5a1de6f3b): each retained verifier
           ;; re-derives its own operation/receipt binding and fails closed
           ;; on any other grammar. Wiring the raw out-of-band verifier here
           ;; admitted only the genuine-repair grammar and rejected the
           ;; cause-reattribution and ordinary-author-refusal retained
           ;; receipts the runtime itself had appended (ordinals 118, 125).
           :verifier (medic-authority/with-retained-authorities
                      (fn [_] false))}))

(defn- valid-click-availability? [availability]
  (and (= :wm/ordinary-click-availability-v1 (:schema availability))
       (= :ordinary-click (:unit availability))
       (map? (:ledger-source availability))
       (string? (get-in availability [:ledger-source :path]))
       (seq (get-in availability [:ledger-source :path]))
       (string? (get-in availability [:ledger-source :sha256]))
       (boolean (re-matches #"[0-9a-f]{64}" (get-in availability [:ledger-source :sha256])))
       (every? integer? [(:allocated availability) (:consumed availability)
                         (:available availability)])
       (<= 0 (:consumed availability) (:allocated availability))
       (= (:available availability) (- (:allocated availability) (:consumed availability)))))

(defn- observation-time [opts]
  (try
    (let [provided (:budget-observed-at opts)
          instant (cond
                    (nil? provided) (Instant/now)
                    (string? provided) (Instant/parse provided)
                    :else nil)]
      (when instant (.toString instant)))
    (catch Throwable _ nil)))

(defn observe-ordinary-click-budget
  "Read one pre-admission ordinary-click snapshot without changing its ledger.
  Reader failure and malformed material are unavailable observations, never a
  synthetic zero."
  [opts]
  (let [read (cond
               (contains? opts :ordinary-click-availability) (constantly (:ordinary-click-availability opts))
               (:ordinary-click-availability-fn opts) (:ordinary-click-availability-fn opts)
               :else (try (requiring-resolve 'futon3c.wm.ordinary-click-budget/availability)
                          (catch Throwable _ nil)))]
    (if-not read
      {:schema budget-observation-schema :status :unavailable
       :reason :ordinary-click-budget-reader-unavailable
       :provenance {:status :absent} :observation-time {:status :absent}
       :phase :pre-admission}
      (try
        (let [availability (read)
              observed-at (observation-time opts)]
          (if-not (valid-click-availability? availability)
            {:schema budget-observation-schema :status :unavailable
             :reason :ordinary-click-budget-observation-malformed
             :provenance {:status :absent} :observation-time {:status :absent}
             :phase :pre-admission}
            (if-not observed-at
              {:schema budget-observation-schema :status :unavailable
               :reason :ordinary-click-budget-observation-time-malformed
               :provenance {:status :present :ledger-source (:ledger-source availability)}
               :observation-time {:status :absent} :phase :pre-admission}
              {:schema budget-observation-schema
               :status (if (pos? (:available availability)) :available :exhausted)
               :availability availability :ledger-source (:ledger-source availability)
               :provenance {:status :present :ledger-source (:ledger-source availability)}
               :observation-time {:status :present :value observed-at}
               :phase :pre-admission})))
        (catch Throwable _
          {:schema budget-observation-schema :status :unavailable
           :reason :ordinary-click-budget-reader-failed
           :provenance {:status :absent} :observation-time {:status :absent}
           :phase :pre-admission})))))

(defn- designated-agent-answer-fn
  "Agency answer port for machine requests.  It contacts exactly the
  configured author and returns a typed no-contact when that seat is not
  currently invoke-ready; it never consults a fallback roster seat."
  [opts seat prompt-fn invoke-mode]
  (let [author seat
        agency-base (or (:agency-base opts) runner/default-agency-base)
        answer (flight-runner/agency-answer-fn
                {:seat author :caller "wm-full-loop"
                 :opts (assoc opts :agency-base agency-base
                                   :invoke-mode (or invoke-mode "brief"))
                 :prompt-fn prompt-fn})]
    (fn [issued]
      (let [roster (try (runner/agent-roster agency-base)
                        (catch Throwable _ {}))
            record (or (get roster author) (get roster (keyword (str author))))
            status (some-> (:status record) name)]
        (if (and (string? author)
                 (true? (:invoke-ready? record))
                 (contains? #{"idle" "restored"} status))
          (answer issued)
          {:state :not-contacted :reason :designated-author-unavailable
           :author author})))))

(defn- designated-author-answer-fn [opts prompt-fn]
  (designated-agent-answer-fn opts (:author opts) prompt-fn "brief"))

(defn- readiness-source-pin [source]
  (if (map? source)
    (select-keys source [:id :path :sha256 :revision])
    {:id (str source)}))

(defn- bounded-prefix [value limit]
  (let [value (str value)]
    (subs value 0 (min (count value) limit))))

(defn- candidate-library-pin [library-pins pattern]
  (let [suffix (str "/" pattern ".flexiarg")]
    (some (fn [pin]
            (when (str/ends-with? (:path pin) suffix)
              (readiness-source-pin pin)))
          library-pins)))

(defn- target-source-pin [proposal sources]
  (let [source-id (get-in proposal [:target :source])]
    (some (fn [source]
            (when (= source-id (:id source))
              (select-keys source [:path :sha256])))
          sources)))

(defn- ticket-readiness-prompt [opts proposal]
  (let [budget (:ticket-readiness-prompt-budget-chars opts)
        source-limit (:ticket-readiness-source-summary-limit opts)
        sources (vec (:sources proposal))
        citations (vec (get-in proposal [:retrieval :citations]))
        candidates (vec (get-in proposal [:retrieval :slice :candidates]))
        library-pins (get-in proposal [:retrieval :slice :library-pins])
        citation-limit (:ticket-readiness-citation-limit opts)
        citation-chars (:ticket-readiness-citation-chars opts)
        candidate-limit (:ticket-readiness-candidate-limit opts)
        candidate-chars (:ticket-readiness-candidate-chars opts)
        ticket-source-pin (target-source-pin proposal sources)
        summary {:schema (:schema proposal)
                 :status (:status proposal)
                 :origin (:origin proposal)
                 :proposal-pin (ticket-readiness/value-digest proposal)
                 :target (if (map? (:target proposal))
                           (select-keys (:target proposal)
                                        [:id :path :sha256 :revision])
                           {:id (str (:target proposal))})
                 :ticket-source-pin ticket-source-pin
                 :citations
                 (mapv (fn [citation]
                         (-> (select-keys citation [:source :lines])
                             (assoc :quote-prefix
                                    (bounded-prefix (:quote citation) citation-chars))))
                       (take citation-limit citations))
                 :citations-truncated? (> (count citations) citation-limit)
                 :candidates
                 (mapv (fn [candidate]
                         {:pattern (:pattern candidate)
                          :rank (:rank candidate)
                          :retriever (:retriever candidate)
                          :source-pin (candidate-library-pin library-pins
                                                            (:pattern candidate))
                          :summary-prefix (bounded-prefix (pr-str candidate)
                                                          candidate-chars)})
                       (take candidate-limit candidates))
                 :candidates-truncated? (> (count candidates) candidate-limit)
                 :source-count (count sources)
                 :source-pins (mapv readiness-source-pin
                                    (take source-limit sources))
                 :source-pins-truncated? (> (count sources) source-limit)}
        prompt
        (str "Assess exactly one bounded ticket preparation increment. Return one JSON object: "
             "{schema:'wm/ticket-readiness-response-v1', increment:'preparation', "
             "ticket-id, source-pin, proposal-pin, increment-id, increment-want, "
             "increment-locator, "
             "preparation-dependencies:'satisfied'|'blocked'}. "
             "Do not claim implementation completion or global dependency coverage. "
             "The proposal is identified by its full-value pin; source bodies are not "
             "part of this contact. Proposal summary: " (pr-str summary))
        prompt-chars (count prompt)]
    (if (<= prompt-chars budget)
      {:status :ready :prompt prompt :prompt-chars prompt-chars
       :prompt-budget-chars budget :proposal-pin (:proposal-pin summary)
       :proposal-summary summary}
      {:state :not-contacted :status :refused
       :reason :readiness/prompt-over-budget
       :prompt-chars prompt-chars :prompt-budget-chars budget
       :proposal-pin (:proposal-pin summary)})))

(defn ticket-readiness-refresh-config
  "Production preparation-only composition from existing canonical readers.
  The Agency response is retained beside the manifest before validation."
  [opts]
  (let [opts (merge {:ticket-readiness-prompt-budget-chars
                     default-ticket-readiness-prompt-budget-chars
                     :ticket-readiness-source-summary-limit
                     default-ticket-readiness-source-summary-limit
                     :ticket-readiness-citation-limit
                     default-ticket-readiness-citation-limit
                     :ticket-readiness-citation-chars
                     default-ticket-readiness-citation-chars
                     :ticket-readiness-candidate-limit
                     default-ticket-readiness-candidate-limit
                     :ticket-readiness-candidate-chars
                     default-ticket-readiness-candidate-chars}
                    opts)
        manifest-path (or (:ticket-readiness-manifest-path opts)
                          default-ticket-readiness-manifest)
        tickets (->> (:tickets (mission-registry/load-tickets))
                     (filter mission-registry/live-ticket?) vec)
        input-fn (fn [ticket]
                   (let [snapshot (readiness-inputs/fetch-pipeline-snapshot)]
                     (readiness-inputs/ticket-readiness-input
                      ticket snapshot (meta-pipeline/pipeline-membership snapshot))))
        contact-resolver #(runner/read-job! opts %)
        contact (designated-agent-answer-fn opts (:author opts) :prompt "brief")]
    {:tickets tickets :manifest-path manifest-path :input-fn input-fn
     :contact-resolver contact-resolver
     :prepare-fn (fn [ticket]
                   (interpretation-request/prepare-proposal!
                    (:id ticket) :ticket mission-registry/default-code-root))
     :answer-fn (fn [proposal]
                  (let [prompt-result (ticket-readiness-prompt opts proposal)
                        response (if (= :ready (:status prompt-result))
                                   (merge (contact prompt-result)
                                          (select-keys prompt-result
                                                       [:prompt-chars
                                                        :prompt-budget-chars
                                                        :proposal-pin]))
                                   prompt-result)
                        answer (handoff/response-map response)
                        result-pin (ticket-readiness/retain-contact-result!
                                    manifest-path response)]
                    (assoc response :answer answer :result-pin result-pin
                                    :contact-resolver contact-resolver)))
     :validate-fn (fn [ticket proposal response]
                    (ticket-readiness/validate-assessment-response
                     ticket proposal response (input-fn ticket)))}))

(defn- open-cascade-author-prompt [req]
  (str "Repair the held executable cascade phase without selecting again. "
       "Implement resolutions for exactly the listed holes. Commit the source in "
       "/home/joe/code/futon2, the canonical absolute repository on the serving "
       "classpath. A sibling worktree is not delivery. Return the full commit SHA "
       "in patch.commit; its source must be reachable from canonical HEAD. "
       "Reply with one JSON object containing requisition-id, cascade-digest, "
       "by, decisions, and patch {namespace,var,source-digest,commit}. "
       "Encode decisions as a JSON array of objects, each with hole: [pattern-id, index] "
       "and operation, capability, completion-check (plus optional consumes/produces). "
       "Example: {\"hole\":[\"pattern/id\",0],\"operation\":\"apply\","
       "\"capability\":\"wm/pattern-apply\",\"completion-check\":[\"checked\"]}. "
       "Never use vectors as JSON object keys. JSON decisions are only a claim compared "
       "against the verified function's output; the function supplies the answers. The patch var "
       "must name a function (defn) of one context map {:cascade-digest :phase} "
       "that returns the decision map keyed by hole id; a bare data var (a def'd "
       "map, set or keyword) is refused at hot-load as "
       ":patch-var-not-a-function. Each decision value is an operational "
       "clause and must contain :operation, :capability, and "
       ":completion-check. Routable :capability values are "
       (pr-str (:routable-capabilities req))
       "; use only these keys, not hole-owner names. :consumes and :produces are optional. A previous "
       ":resume-incomplete cause in the requisition names the missing fields "
       "that this fresh patch must supply. The patch namespace must require "
       "futon2.aif.load-identity and call (load-identity/register! *ns* *file*) "
       "at top level; the trusted loader refuses an unregistered namespace as "
       ":patch-namespace-not-registered.\n\n"
       (when-let [correction (:format-correction req)]
         (str "FORMAT CORRECTION (one round only): the previous response did not parse: "
              (:parser-error correction)
              ". Return one valid JSON object with decisions encoded as the array above. "
              "Keep this requisition and committed patch; no new selection.\n"))
       (pr-str req)))

(defn- open-cascade-review-prompt [{:keys [requisition author-answer]}]
  (str "Independently review this held-cascade repair. Verify the committed "
       "source and every hole decision. Confirm that patch.var names a function "
       "of the context map {:cascade-digest :phase}, not a data var; a data var "
       "is refused at hot-load as :patch-var-not-a-function. Reply with one "
       "JSON object containing "
       "the author's requisition-id, cascade-digest, patch {source-digest} "
       "naming the committed source you verified, and accepted-by set to your "
       "own seat id. Do not restate decisions; acceptance binds the author's "
       "decisions to that verified source digest.\n\nREQUISITION:\n"
       (json/generate-string requisition)
       "\n\nAUTHOR ANSWER:\n"
       (json/generate-string author-answer)))

(defn- cascade-effect-author-prompt [request]
  (str "Execute exactly this certified cascade occurrence through the ordinary "
       "build/review/close work discipline. Ground any claimed effect in durable "
       "artifacts (commit or content-addressed evidence). Reply with one JSON "
       "object naming request-id, cascade-digest, occurrence-id, by, and effect. "
       "The request's :target is the mission to work in; do not contact the reviewer, "
       "the runner sends your answer to the named reviewer.\n\n"
       (pr-str request)))

(defn- cascade-effect-review-prompt [{:keys [request author-answer]}]
  (str "Independently verify this occurrence-level build/review/close result. "
       "Inspect the durable artifacts. Reply with one JSON object containing "
       "the exact request-id, cascade-digest, occurrence-id, author's by, your "
       "accepted-by, and effect {status:'verified', grounding:{kind,digest,revision,repo}}. "
       "For a commit effect, revision is the full 40-hex git commit sha you "
       "inspected and repo is its non-blank repository name or path. Keep digest "
       "as the 64-hex content grounding digest, not the commit sha. For a "
       "content-addressed effect without a commit, omit revision. "
       "Do not accept an ungrounded success label. Do not contact the author "
       "or any other seat; the runner receives this reply.\n\nREQUEST:\n"
       (pr-str request) "\n\nAUTHOR ANSWER:\n" (pr-str author-answer)))

(defn- cascade-effect-contact-answer
  "Decode one cascade-effect Agency response without erasing why it supplied
  no answer. Successful decoding is exactly handoff/response-map's existing
  value; only nil becomes a typed absence."
  [seat response]
  (if-some [answer (handoff/response-map response)]
    answer
    (let [state (:state response)
          state (if (string? state) (keyword state) state)
          reason (cond
                   (= :done state) :unparseable-answer
                   (some? state) state
                   :else :unparseable-answer)]
      (cond-> {:status :absent :reason reason :seat seat}
        (:job-id response) (assoc :job-id (:job-id response))))))

(defn production-cascade-executors
  "The live capability registry for certified cascade continuation."
  [opts]
  (let [author (:author opts)
        reviewer (:reviewer opts)
        author-contact (designated-agent-answer-fn
                        opts author cascade-effect-author-prompt "work")
        reviewer-contact (designated-agent-answer-fn
                          opts reviewer cascade-effect-review-prompt "work")]
    {:wm/pattern-apply
     (cascade-continuation/reviewed-build-review-close-executor
      {:author author :reviewer reviewer :target (:target opts)
       :author-contact #(cascade-effect-contact-answer author (author-contact %))
       :reviewer-contact #(cascade-effect-contact-answer reviewer (reviewer-contact %))})}))

(defn- patch-loader
  "Resolve a reviewed patch only after reloading its named namespace and
  checking the loaded source digest against the reviewed descriptor.  The
  var must hold a FUNCTION of the resume context: a data value that is
  merely `ifn?` (a map, a set or a keyword answers a one-argument lookup) is
  refused as :patch-var-not-a-function, distinct from a var that does not
  resolve (:patch-var-unavailable).  Injury 41a00c62: the author named the
  bare decision map `clauses` instead of the function `decisions`."
  [{:keys [namespace var source-digest]}]
  (let [n (symbol namespace)
        v (symbol var)]
    (require n :reload)
    (let [loaded (get @load-identity/registry n)
          resolved (ns-resolve n v)]
      (when-not loaded
        (throw (ex-info "reviewed patch namespace did not register its load identity"
                        {:kind :patch-namespace-not-registered
                         :namespace namespace})))
      (when-not (= source-digest (:sha256 loaded))
        (throw (ex-info "reviewed patch source digest does not match loaded source"
                        {:kind :patch-source-digest-mismatch
                         :expected source-digest :loaded (:sha256 loaded)})))
      (when-not (var? resolved)
        (throw (ex-info "reviewed patch var does not resolve"
                        {:kind :patch-var-unavailable :namespace namespace :var var})))
      (let [value @resolved]
        (when-not (fn? value)
          (throw (ex-info "reviewed patch var is not a function of the context"
                          {:kind :patch-var-not-a-function
                           :namespace namespace :var var
                           :value-type (if (nil? value) "nil" (.getName (class value)))})))
        value))))

(defn- contact-job
  "The Agency job id a contact's response carries (`agency-answer-fn` returns
  `{:seat :job-id :state :text}`), else a typed absence naming why there is
  none: the designated seat was not invoke-ready (`designated-agent-answer-fn`
  answers `:state :not-contacted`), the dispatch produced no job
  (`:not-dispatched`), or the answer was injected directly."
  [response]
  (or (:job-id response)
      (let [state (:state response)]
        (cond-> {:status :absent
                 :reason (if (#{:not-contacted :not-dispatched} state)
                           state
                           :direct-answer)}
          (:reason response) (assoc :contact-reason (:reason response))))))

(defn open-cascade-resume-handler
  "Production D4 composition. Contact the designated author and distinct
  reviewer through Agency, validate the reviewed answer, hot-load the exact
  reviewed source, and retry D3's held phase in the same click."
  [base-opts]
  (fn handler
    ([error] (handler error {}))
    ([error live-opts]
      (let [opts (merge base-opts live-opts)
            author (:author opts)
            reviewer (:reviewer opts)
            author-contact (or (:open-cascade-author-answer-fn opts)
                               (designated-agent-answer-fn
                                opts author open-cascade-author-prompt "work"))
            reviewer-contact (or (:open-cascade-reviewer-answer-fn opts)
                                 (designated-agent-answer-fn
                                  opts reviewer open-cascade-review-prompt "work"))
            ctx (open-resume/recognize error)
            req (when (= :held (:status ctx))
                  (assoc (open-resume/authoring-requisition ctx author reviewer)
                         :routable-capabilities
                         (vec (sort (keys (capability-dispatch/routes opts))))))]
        (if-not (= :dispatched (:status req))
          (or req ctx)
          (let [{author-response :response author-answer :answer correction :format-correction}
                (handoff/contact-with-format-correction
                 {:contact author-contact :request req :job-ref contact-job})
                author-binding (open-resume/validate-author-binding req author-answer)
                delivery (when (= :bound (:status author-binding))
                           (patch-delivery/check-delivery author-answer))
                ledger (open-resume/new-ledger (or (:run-id opts) "held-click"))
                ;; Check the canonical, digest-validated function's product before
                ;; spending a reviewer contact. Review still binds this source.
                loaded (when (= :delivered (:status delivery))
                         (open-resume/hot-load! ledger req author-answer
                           (or (:open-cascade-patch-loader opts) patch-loader)))
                product-check (when loaded
                                (if (= :error (:status loaded)) loaded
                                  (open-resume/validate-product req author-answer
                                                               (:product loaded) opts)))
                reviewer-response (when (= :accepted (:status product-check))
                                    (reviewer-contact {:requisition req
                                                       :author-answer author-answer}))
                reviewed (when reviewer-response
                           (handoff/response-map reviewer-response))
                ;; Review accepts the author's decisions; it does not author
                ;; a second copy. Mismatched pins retain the original validator
                ;; path, and reviewer identity is still checked there.
                accepted-answer
                (if (and (= (:requisition-id author-answer) (:requisition-id reviewed))
                         (= (:cascade-digest author-answer) (:cascade-digest reviewed))
                         (= (get-in author-answer [:patch :source-digest])
                            (get-in reviewed [:patch :source-digest])))
                  (assoc author-answer :accepted-by (:accepted-by reviewed))
                  reviewed)
                result (cond
              (nil? author-answer)
              {:status :error
               :kind (if (and correction (= "done" (:state author-response)))
                       :author-response-unparseable
                       :open-cascade-author-answer-invalid)
               :author author
               :format-correction correction
               :parser-error (handoff/response-parse-error author-response)
               :author-job (contact-job author-response)}

              (not= :bound (:status author-binding))
              (assoc author-binding :author-job (contact-job author-response))

              (not= :delivered (:status delivery))
              (assoc delivery :author-job (contact-job author-response))

              (not= :accepted (:status product-check))
              (assoc product-check :author-job (contact-job author-response))

              (nil? reviewed)
              {:status :error
               :kind :open-cascade-review-answer-invalid
               :reviewer reviewer
               :reviewer-job (contact-job reviewer-response)}

              :else
              (open-resume/handle-error!
               ledger error author reviewer accepted-answer
               {:cascade-capability-routes (capability-dispatch/routes opts)
                :click-budget (:click-budget opts)
                :selection-counter (:selection-counter opts)
                :patch-loader (or (:open-cascade-patch-loader opts)
                                  patch-loader)}))]
              (cond-> result
                correction (assoc :author-format-correction correction)
                (and correction (:answer-delivery result))
                (assoc-in [:answer-delivery :format-correction] correction)
                (and correction (get-in result [:result :answer-delivery]))
                (assoc-in [:result :answer-delivery :format-correction] correction))))))))

(defn reselect-q6-refusal
  "Run the inner judge, reusing the single click-local reading reselection port
  when Q6 removes the entire family. No port means no new selection authority.
  The port owns the shared one-reselection budget (including reading failures)."
  [opts judge]
  (let [current (selected-target-reading/current-options opts)
        generated (judge current)
        q6 (get-in generated [:judgement :decision :selection-certificate :q6])
        target (get-in current [:task :id])]
    (if (and (= :refused (:status q6))
             (= :repeat-choice-after-refusal (:kind q6))
             target (:selected-target-reading-reselect-fn opts))
      (let [refusal {:target target :kind :repeat-choice-after-refusal
                     :scope :this-click
                     :predecessor-run-id (get-in q6 [:predecessor :previous-run/id])
                     :digest (:input-digest q6) :q6 q6}
            replacement ((:selected-target-reading-reselect-fn opts) refusal)
            after (selected-target-reading/current-options opts)]
        (cond
          (:target replacement) (judge after)
          (nil? (get-in after [:task :id]))
          {:absent :no-task-selected
           :outer/selection (:outer/selection after)
           :judgement {:decision {:status :absent :kind :no-task-selected
                                  :refusals (:outer-field/excluded (:outer/selection after))}}}
          :else generated))
      generated)))

(defn- selection-judge
  [opts days]
  (reselect-q6-refusal opts
    (fn [opts]
     (wm/generate-war-machine
    days
    (merge (wm/accumulation-config)
           (select-keys opts [:g-law :accumulate-strategic-habit? :run-id
                              :loaded-code-identity :cascade-habit-path
                              :cascade-feedback-path :cascade-feedback-metadata
                              :observation-labels-path :flight :trace-dir
                              :task
                              :selection-timing/state :nano-time-fn
                              ;; the decision forms every target's policy
                              ;; family against this pinned graph
                              :pattern-graph-path :target-reading-root
                              :occurrence-square-root :pattern-library-root
                              :require-occurrence-squares?
                              :q6/predecessor :r20/makeup :outer/makeup-path])
           (select-keys opts [:meta-field-observation :meta-field-source-pin])
           ;; Construction publishes below. Do not publish twice.
           {:trace? false :include-advisory-lanes? false :defer-render? true})))))

(defn production-defaults
  "Build the report-backed functions for one production invocation."
  [opts]
  (let [injury-authority (or (:meta-injury-authority opts)
                             (injury-store/load-authority
                              (or (:meta-injury-root opts)
                                  injury-store/default-root)))
        click-availability (observe-ordinary-click-budget opts)
        meta-observation (or (:meta-field-observation opts)
                             (when injury-authority
                               (meta-field/observe
                                {:catalog-path meta-field/default-algorithm-catalog
                                 :click-availability click-availability
                                 :injury-authority injury-authority})))
        opts (cond-> opts
               meta-observation
               (assoc :meta-field-observation meta-observation
                      :meta-field-source-pin (:source-pin meta-observation)))
        selected-judgment (atom nil)]
    {:ticket-readiness-refresh
     (or (:ticket-readiness-refresh opts)
         (fn [runner-opts]
           (ticket-readiness-refresh-config (merge opts runner-opts))))
     :judge-fn (fn [days]
                 (try
                   (let [generated (selection-judge opts days)]
                     (reset! selected-judgment (:judgement generated))
                     generated)
                   (catch Throwable e
                     (selection-timing/abort! (:selection-timing/state opts)
                                              (:nano-time-fn opts)
                                              :selection-judge-threw)
                     (throw e))))
     :cascade-revision-proposals-fn
     (cascade-revision-producer/make-proposals-fn
      {:judgment-fn #(deref selected-judgment)})
     :refresh-required? true
     ;; Stop before dispatch when a selection merely names machinery that this
     ;; click did not exercise. The stop is inside the restartable selection
     ;; phase, so a hot repair followed by :retry reruns the decision.
     :require-loop-node-exercise? true
     :refresh-fn (fn []
                   (let [mission-freshness (mission-registry/refresh-mission-substrate!)]
                     (cv/maybe-refresh!)
                     {:freshness mission-freshness}))
     :interpretation-ask-fn (click-ask/click-ask-fn opts)
     :selected-target-reading-fn
     (fn [runner-opts judgement]
       ;; The runner mints :registered-run/job-ledger after production-defaults
       ;; is built. Construct the Agency answer port here, from the merged live
       ;; options, or dispatch! refuses before it can create the author job.
       (let [live-opts (merge opts runner-opts)
             answer-fn (or (:selected-target-reading-answer-fn live-opts)
                           (designated-author-answer-fn
                            live-opts selected-target-reading/prompt))
             chosen-need (selected-target-reading/selected-need judgement)]
         (when chosen-need
           (selected-target-reading/repair!
            (assoc live-opts :answer-fn answer-fn
                   :run-record-dir (or (:run-record-dir live-opts) runner/default-run-record-dir)) judgement))))
     :occurrence-square-authoring-fn
     (fn [runner-opts judgement]
       (let [live-opts (merge opts runner-opts)
             answer-fn (or (:occurrence-square-answer-fn live-opts)
                           (designated-agent-answer-fn
                            live-opts (:author live-opts)
                            square-authoring/prompt "brief"))]
         (square-authoring/repair!
          (assoc live-opts :answer-fn answer-fn)
          judgement)))
     ;; Unlike the target-reading phase, this phase receives only REQUEST.
     ;; Delay construction of its contact port until the runner has added its
     ;; run-local ledgers, and make that late binding explicit.
     :selected-reading-ask-builder
     (fn [runner-opts]
       (let [live-opts (merge opts runner-opts)]
         (or (:selected-reading-answer-fn live-opts)
             (designated-author-answer-fn live-opts selected-reading/prompt))))
     :scan-render-fn wm/render-war-machine
     :effective-run-configuration-fn wm/effective-run-configuration
     :mode-flags-fn wm/arena-mode-flags}))

(declare run-ordinary-opportunity-with-task!)

(defn- missions-giraffe-manifest? [manifest]
  (let [edge-path (get-in manifest [:sources :edges :path])]
    (and (string? edge-path)
         (boolean (re-find #"(^|/)holes/missions/" edge-path))
         (not (contains? (:sources manifest) :tickets-index)))))

(defn- verified-giraffe-run? [dir]
  (try
    (let [manifest-file (io/file dir "manifest.edn")
          manifest (when (.isFile manifest-file)
                     (edn/read-string (slurp manifest-file)))
          verify-run (requiring-resolve 'futon3c.giraffe.run/verify-run)]
      (and (missions-giraffe-manifest? manifest)
           (:ok? (verify-run dir))))
    (catch Exception _ false)))

(defn- find-giraffe-run [opts]
  (if-let [explicit (:outer/giraffe-run-dir opts)]
    (when (verified-giraffe-run? (io/file explicit)) (io/file explicit))
    (let [root (io/file (or (:outer/giraffe-root opts) default-giraffe-root))]
      (when (.isDirectory root)
        (->> (.listFiles root)
             (filter #(.isDirectory ^java.io.File %))
             (sort-by #(.getName ^java.io.File %) #(compare %2 %1))
             (some #(when (verified-giraffe-run? %) %)))))))

(defn- outer-selection-refusal [e]
  {:absent :outer-selection-refused
   :reason (or (:kind (ex-data e)) :outer-selection-failed)
   :details (dissoc (ex-data e) :kind)})

(defn- pending-run-ending [opts run-id]
  (pending-observation/observe
   (or (:run-record-dir opts) runner/default-run-record-dir) run-id))

(defn- previous-giraffe-observation [current-dir makeup pending]
  (let [run-id (:giraffe/run-id pending)
        previous-dir (when run-id (io/file (.getParentFile (io/file current-dir)) run-id))]
    (when (and previous-dir (.isDirectory previous-dir))
      (let [observation (outer-observation/giraffe-field previous-dir makeup)]
        (when (= run-id (:giraffe/run-id observation)) observation)))))

(defn- ready-targets [observation]
  (into #{} (comp (filter #(= :ready (get-in observation [:facts (:target %) :status])))
                   (map :target))
        (:field observation)))

(defn- ready-gain [before current selected-target]
  ;; Joe, 2026-10-28: gain is observed only after the selected item closes in
  ;; a later Giraffe run.  While it remains open the observation is :none.
  (if (some #(= selected-target (:target %)) (:field current))
    :none
    (case (count (set/difference (ready-targets current)
                                 (ready-targets before)))
      0 :none
      1 :one
      :several)))

(defn- learning-observation [pending previous]
  (let [selection (:selection pending)
        selected-target (get-in selection [:selection :selected :target])
        selected-relation (:selected-relation selection)]
    (assoc previous
           :focus (:focus selection)
           :relation (fn [_ target]
                       (when (= selected-target target) selected-relation)))))

(defn- resolve-pending-learning [opts makeup state current-dir current-observation]
  (if-let [pending (:pending state)]
    (let [ending (pending-run-ending opts (:run-id pending))]
      (if-not (= :ready (:status ending))
        {:status :deferred :state state :reason (:reason ending)}
        (if-let [previous (previous-giraffe-observation current-dir makeup pending)]
          (let [selected-target (get-in pending [:selection :selection :selected :target])
                gain (ready-gain previous current-observation selected-target)
                consumed (outer-state/consume-observation
                          makeup state pending
                          (learning-observation pending previous)
                          (assoc ending :observed-gain gain))]
            (assoc consumed :ending-class (:ending-class ending) :observed-gain gain))
          {:status :deferred :state state :reason :previous-giraffe-run-absent})))
    {:status :none :state state}))

(defn- new-run-id []
  (str (subs (str (Instant/now)) 0 10) "-" (UUID/randomUUID)))

(defn reading-reselection-port
  "One click-local removal and fresh outer certificate, retaining the first
  choice and refusal. The pending learning carrier follows the replacement."
  [makeup state observation initial-selection selection-state persist-pending!]
  (let [used? (atom false)]
    (fn [refusal]
      (when (compare-and-set! used? false true)
        (let [target (:target refusal)
              exclusion (merge {:target target :reason (:kind refusal)
                                :scope :this-click :reader-refusal refusal}
                               (select-keys refusal [:predecessor-run-id :digest]))
              ranked (disj (set (:outer-score/admitted initial-selection)) target)
              reduced (update observation :field
                              #(vec (filter (fn [c] (contains? ranked (:target c))) %)))
              result (when (seq (:field reduced)) (outer-select/select makeup state reduced))
              next-target (get-in result [:selection :selected :target])
              retained (-> (or result {})
                           (update :selection #(some-> % (dissoc :candidates)))
                           (assoc :outer-field/admitted (:outer-score/admitted result)
                                  :giraffe/run-id (:giraffe/run-id observation)
                                  :giraffe/manifest-sha256 (:giraffe/manifest-sha256 observation)
                                  :makeup/sha256 (:makeup/sha256 makeup)
                                  :focus (:focus observation)
                                  :selected-relation (when next-target
                                                      ((:relation observation)
                                                       (:focus observation) next-target))
                                  :outer-field/excluded
                                  (conj (vec (or (:outer-field/excluded initial-selection)
                                                 (into (vec (:outer-field/excluded observation))
                                                       (:outer-score/excluded initial-selection))))
                                        exclusion)
                                  :knowledge-producer-requests
                                  (:knowledge-producer-requests initial-selection)
                                  :outer/learning (:outer/learning initial-selection)
                                  :reading-reselection {:limit 1 :used 1
                                                        :previous-selection initial-selection
                                                        :refusals [refusal]}))]
          (reset! selection-state {:task (when next-target {:id next-target})
                                   :outer/selection retained})
          (when next-target
            (persist-pending! retained)
            {:target next-target :selection retained}))))))

(defn- run-ordinary-opportunity!
  "Run one production opportunity with the report-backed defaults installed.
  A caller-supplied task enters the existing task path unchanged.  Otherwise
  the R20 outer loop selects from the newest verified missions Giraffe run.
  Learning from click n is applied at selection n+1, when its terminal class
  and a later Giraffe pass exist.  Items made ready are counted only after the
  chosen task has closed; while it remains open observed gain is `:none`.
  META injuries are arbitrated before this function."
  [opts]
  (if (get-in opts [:task :id])
    (run-ordinary-opportunity-with-task! opts)
    (if-let [giraffe-dir (find-giraffe-run opts)]
      (try
        (let [makeup (outer-state/load-makeup
                      (or (:outer/makeup-path opts) outer-state/default-makeup-path))
              state (outer-state/read-state
                     makeup (or (:outer/state-path opts) outer-state/default-state-path))
              focus-inputs (focus-receipt/read-inputs)
              observation (outer-observation/outer-observation
                           {:run-dir giraffe-dir
                            :makeup makeup
                            :focus-inputs focus-inputs
                            :decision-as-of (or (:decision-as-of opts) (str (Instant/now)))
                            :previous-focus (:previous-focus opts)
                            :relation-context
                            (focus-receipt/relation-context
                             (or (:code-root opts) "/home/joe/code"))})
              learning (resolve-pending-learning opts makeup state giraffe-dir observation)
              state (:state learning)
              selection (outer-select/select makeup state observation)
              retained-selection (-> selection
                                     (assoc :outer-field/admitted
                                            (:outer-score/admitted selection))
                                     (update :selection #(some-> % (dissoc :candidates)))
                                     (assoc :giraffe/run-id (:giraffe/run-id observation)
                                            :giraffe/manifest-sha256
                                            (:giraffe/manifest-sha256 observation)
                                            :makeup/sha256 (:makeup/sha256 makeup)
                                            :outer-field/excluded
                                            (into (vec (:outer-field/excluded observation))
                                                  (:outer-score/excluded selection))
                                            :focus (:focus observation)
                                            :selected-relation
                                            (when-let [target (get-in selection
                                                                      [:selection :selected :target])]
                                              ((:relation observation)
                                               (:focus observation) target)))
                                     (cond-> (not= :none (:status learning))
                                       (assoc :outer/learning
                                              (select-keys learning
                                                           [:status :reason :ending-class
                                                            :observed-gain :receipt]))))
              state-path (or (:outer/state-path opts) outer-state/default-state-path)]
          (if-let [target (get-in selection [:selection :selected :target])]
            (let [run-id (or (:run-id opts) (new-run-id))]
              (when-not (= :deferred (:status learning))
                (outer-state/write-state!
                 state-path
                 (outer-state/replace-pending state
                        {:selection retained-selection
                         :run-id run-id
                         :giraffe/run-id (:giraffe/run-id observation)})))
              (let [selection-state (atom {:task {:id target} :outer/selection retained-selection})
                    live-opts (assoc opts :run-id run-id :task {:id target}
                                    :outer/selection retained-selection
                                    :reading-reselection/state selection-state)
                    reselect (reading-reselection-port
                              makeup state observation retained-selection selection-state
                              (fn [replacement]
                                (when-not (= :deferred (:status learning))
                                  (outer-state/write-state!
                                   state-path (outer-state/replace-pending state
                                                     {:selection replacement :run-id run-id
                                                      :giraffe/run-id (:giraffe/run-id observation)})))))]
                (run-ordinary-opportunity-with-task!
                 (assoc live-opts :selected-target-reading-reselect-fn reselect))))
            (do
              (when (= :applied (:status learning))
                (outer-state/write-state! state-path state))
              {:absent :no-task-selected :outer/selection retained-selection})))
        (catch clojure.lang.ExceptionInfo e
          (outer-selection-refusal e))
        (catch Exception e
          (outer-selection-refusal e)))
      {:absent :no-verified-giraffe-run})))

(defn- run-ordinary-opportunity-with-task!
  [opts]
  ;; Mint before building the composition root.  The runner historically
  ;; minted later, after `production-defaults` had closed over OPTS; the judge
  ;; therefore saw no tick identity and accumulation refused even though the
  ;; eventual run record had one.
  (let [opts (cond-> opts
               (not (:run-id opts))
               (assoc :run-id (str (subs (str (Instant/now)) 0 10)
                                   "-" (UUID/randomUUID))))
        opts (update opts :cascade-feedback-path
                     #(or % cascade-feedback/default-path))
        opts (update opts :cascade-feedback-metadata
                     #(or % (cascade-feedback/load-construction-metadata
                             (:cascade-feedback-path opts))))
        opts (update opts :pattern-graph-path #(or % default-pattern-graph-path))
        opts (update opts :pattern-graph-diff-dir #(or % default-pattern-graph-diff-dir))
        opts (assoc opts :require-occurrence-squares? true)
        opts (update opts :outer-learning-manifest-path
                     #(or % outer-learning-manifest/default-path))
        ;; The production judge closes over OPTS before the runner adds its
        ;; other ledgers. Mint this collector here and pass the same identity
        ;; through, rather than timing into a disconnected atom.
        opts (update opts :selection-timing/state
                     #(or % (selection-timing/new-state)))
        defaults (production-defaults opts)
        ;; Pass production ports on the actual runner option map as well as
        ;; binding the compatibility lookup.  The serving runner must not
        ;; depend on a dynamic binding surviving namespace reload or an
        ;; execution boundary before it decides whether selection is
        ;; terminal.
        runner-opts0 (assoc opts
                           :ticket-readiness-refresh
                           (:ticket-readiness-refresh defaults)
                           :click-budget (or (:click-budget opts) (atom 0))
                           :selection-counter (or (:selection-counter opts)
                                                  (atom 0))
                           :selected-target-reading-fn
                           (:selected-target-reading-fn defaults)
                           :occurrence-square-authoring-fn
                           (:occurrence-square-authoring-fn defaults)
                           :selected-reading-ask-builder
                           (:selected-reading-ask-builder defaults))
        ;; Construct D4 only now: RUNNER-OPTS carries the live job ledger and
        ;; exact cast.  Closing over the earlier OPTS recreated the stale
        ;; handoff bug this composition root exists to prevent.
        runner-opts (assoc runner-opts0 :open-cascade-resume-fn
                           (or (:open-cascade-resume-fn runner-opts0)
                               (open-cascade-resume-handler runner-opts0)))]
    (binding [runner/*runtime-defaults* defaults]
      (runner/run-opportunity! runner-opts))))

(defn self-heal-acceptance-intake
  "Suppress a stale admitted acceptance only when the durable ledger records an
  unsatisfiable-acceptance claim for the carried transaction. The pinned prior
  diagnosis becomes available input to a fresh proposal; ordinary resumes keep
  both carried bindings and the normal :present diagnosis observation."
  [opts carried-bindings diagnosis-context]
  (let [reconsideration (:acceptance-unsatisfiable-claim carried-bindings)
        opts (if (= :carried-transaction (:source carried-bindings))
               (reduce (fn [options key]
                         (if (contains? options key)
                           options
                           (assoc options key (get carried-bindings key))))
                       opts
                       (if reconsideration
                         [:diagnosis-evidence-pin]
                         [:diagnosis-evidence-pin :acceptance-test]))
               opts)
        opts (cond-> opts
               reconsideration (dissoc :acceptance-test)
               reconsideration (assoc :meta-acceptance-unsatisfiable-claim
                                      reconsideration))
        diagnosis-observation
        (if reconsideration
          {:status :available
           :injury-id (:injury-id carried-bindings)
           :source-record-pin (:source-record-pin carried-bindings)
           :diagnosis-evidence-pin (:diagnosis-evidence-pin carried-bindings)
           :repair-claimed? false
           :source :acceptance-reconsideration}
          (meta-self-heal/prepare-diagnosis opts diagnosis-context))]
    {:opts opts :diagnosis-observation diagnosis-observation
     :acceptance-reconsideration reconsideration}))

(defn- run-opportunity-with-authority!
  "Arbitrate durable META injuries before constructing any ordinary policy
  field. An active injury consumes the opportunity without fallback."
  [opts authority]
  (let [opts (production-self-heal-options opts)
        ;; Never honor a caller-supplied derived verifier. Only a validated
        ;; explicit context may create this process-local function.
        opts (cond-> (dissoc opts :meta-medic-authority-verifier)
               (and (not= :refused (:status authority))
                    (ifn? (:verifier authority)))
               (assoc :meta-medic-authority-verifier (:verifier authority)))
        opts (assoc opts
                    :meta-injury-ledger-path
                    (or (:meta-injury-ledger-path opts)
                        meta-self-heal/default-ledger-path)
                    :meta-repair-transaction-path
                    (or (:meta-repair-transaction-path opts)
                        meta-self-heal/default-transaction-path))
        retry (:meta-repair-retry opts)
        diagnosis-context (when (and (not retry) (not= :refused (:status authority)))
                            (meta-self-heal/diagnosis-context opts))
        open-out-of-band (when (and diagnosis-context (not retry))
                           (meta-self-heal/open-out-of-band-route!
                            opts diagnosis-context))
        carried-bindings (if (or open-out-of-band retry
                                 (= :refused (:status authority)))
                           {:status :absent :reason :retry}
                           (meta-self-heal/carried-bindings-observation
                            opts diagnosis-context))
        intake (when (and (not open-out-of-band) (not retry)
                          (not= :refused (:status authority)))
                 (self-heal-acceptance-intake opts carried-bindings diagnosis-context))
        opts (or (:opts intake) opts)
        diagnosis-observation
        (when (and (not retry) (not= :refused (:status authority)))
          (:diagnosis-observation intake))
        acceptance-observation (if open-out-of-band
                                 {:status :not-required
                                  :reason :meta-self-heal/open-out-of-band-routed}
                                 (if (= :refused (:status authority))
                                 {:status :not-required
                                  :reason :meta-self-heal/medic-authority-unavailable}
                                 (if (= :meta-self-heal/diagnosis-active-set-unavailable
                                     (:reason diagnosis-observation))
                                {:status :not-required :reason :meta-self-heal/acceptance-not-prepared
                                 :diagnosis-reason :meta-self-heal/diagnosis-active-set-unavailable}
                                (if retry
                                {:status :not-required
                                 :reason :meta-self-heal/acceptance-retry-not-prepared}
                                (meta-self-heal/prepare-acceptance opts diagnosis-observation)))))
        effective-opts (cond-> opts
                         diagnosis-context (assoc :meta-diagnosis-context diagnosis-context)
                         diagnosis-observation
                         (assoc :meta-diagnosis-observation diagnosis-observation)
                         acceptance-observation
                         (assoc :meta-acceptance-observation acceptance-observation)
                         (= :available (:status diagnosis-observation))
                         (assoc :diagnosis-evidence-pin
                                (:diagnosis-evidence-pin diagnosis-observation))
                         (#{:accepted :present} (:status acceptance-observation))
                         (assoc :acceptance-test
                                (:acceptance-test acceptance-observation)))
        readiness (if (= :meta-self-heal/diagnosis-active-set-unavailable (:reason diagnosis-observation))
                    {:status :not-required}
                    (if retry
                    (meta-self-heal/attributable-retry-readiness effective-opts retry)
                    (meta-self-heal/production-readiness effective-opts diagnosis-context)))]
    (cond
      open-out-of-band open-out-of-band
      (= :meta-self-heal/diagnosis-active-set-unavailable (:reason diagnosis-observation))
      {:status :refused :reason :meta-self-heal/active-set-unavailable
       :route :dedicated-meta-self-heal :ordinary-selection-called? false
       :active-injuries (select-keys (:active-injuries diagnosis-context) [:status :reason :defects])
       :diagnosis-observation diagnosis-observation :acceptance-observation acceptance-observation
       :carried-bindings carried-bindings}
      (= :refused (:status readiness))
      (assoc readiness :diagnosis-observation diagnosis-observation
             :acceptance-observation acceptance-observation
             :carried-bindings carried-bindings)
      retry (assoc (run-ordinary-opportunity! effective-opts)
                   :diagnosis-observation diagnosis-observation
                   :acceptance-observation acceptance-observation
                   :carried-bindings carried-bindings)
      :else (let [routed (meta-self-heal/route! effective-opts)]
              (assoc (or routed (run-ordinary-opportunity! effective-opts))
                     :diagnosis-observation diagnosis-observation
                     :acceptance-observation acceptance-observation
                     :carried-bindings carried-bindings)))))

(defn run-opportunity!
  "Resolve medic authority before any diagnosis, readiness, ledger, or
  selection work, then arbitrate the opportunity."
  [opts]
  (let [authority (resolve-medic-authority opts)]
    (if (= :refused (:status authority))
      {:status :refused :reason (:reason authority)
       :route :dedicated-meta-self-heal :ordinary-selection-called? false}
      (run-opportunity-with-authority! opts authority))))

(defn resume-on-prod-cascade!
  "Resume the production cascade-execution boundary from a sealed checkpoint.

  This deliberately does not call selection, spend a click, or claim a tick.
  It runs the same production execution function under run-phase! so telemetry,
  debugger stops, stores, validation and journal behaviour remain live."
  [{:keys [source-run-id source-attempt-id] :as opts}]
  (let [checkpoint (on-prod-history/load-checkpoint source-run-id
                                                    source-attempt-id)
        entry (:selected-entry checkpoint)
        run-id (or (:run-id opts)
                   (str "on-prod-history-" (UUID/randomUUID)))
        phase-opts (assoc opts :run-id run-id)
        context {:run-id run-id
                 :attempt-id (str "resume-" source-attempt-id)
                 :trigger :on-prod-potted-history
                 :source-run-id source-run-id
                 :source-attempt-id source-attempt-id}
        result (runner/run-phase!
                phase-opts context :cascade-execution
                #(cascade-decision/execute-selected-cascade! entry phase-opts)
                #(select-keys % [:status :kind :cascade-digest
                                 :compiled-digest]))]
    {:schema :wm/on-prod-history-result-v1
     :target (get-in entry [:action :target])
     :run-id run-id
     :source-run-id source-run-id
     :source-attempt-id source-attempt-id
     :selection-ran false
     :clicks-spent 0
     :result (cascade-decision/persistable-cascade-execution result)}))

(defn continue-on-prod-cascade!
  "Resume a sealed production selection through cascade compilation and then
  consume its certified frontier through the verified continuation bridge.
  This runs in the serving JVM, never selects, and never spends a click.

  Production callers must supply :cascade-capability-executors and
  :cascade-close-fn. Their absence is a typed frontier, never synthetic
  success."
  [{:keys [cascade-capability-executors cascade-close-fn cascade-failure-fn] :as opts}]
  (let [;; Ordinary run-opportunity! installs this context before any Agency
        ;; contact. A selection-free production continuation must do the same,
        ;; otherwise the first capability dispatch fails outside the ledger.
        ;; The run id is fixed here, not inside resume, so the dispatches the
        ;; executors make register under the same run as the resume phase.
        opts (cond-> opts
               (nil? (:run-id opts))
               (assoc :run-id (str "on-prod-history-" (UUID/randomUUID)))
               (nil? (:click-id opts))
               (assoc :click-id
                      (str "potted-history:" (:source-run-id opts) "/"
                           (:source-attempt-id opts)))
               (nil? (:registered-run/job-ledger opts))
               (assoc :registered-run/job-ledger
                      (atom {:order [] :jobs {}}))
               (nil? (:job-liveness/state opts))
               (assoc :job-liveness/state (atom [])))
        resumed (resume-on-prod-cascade! opts)
        ready (:result resumed)]
    (if-not (= :cascade-ready (:status ready))
      (assoc resumed :continuation
             {:status :error :kind :continuation-cascade-not-ready
              :repair {:result ready}})
      (assoc resumed :continuation
             (cascade-continuation/continue-certified-cascade!
              {:ready ready
               :executable-cascade-root
               (or (:executable-cascade-root opts)
                   cascade-decision/default-executable-cascade-root)
               :execution-journal-root
               (or (:execution-journal-root opts)
                   cascade-decision/default-execution-journal-root)
               :executors (or cascade-capability-executors
                              (production-cascade-executors (assoc opts :target (:target resumed))))
               :close-fn
               (or cascade-close-fn
                   (cascade-continuation/production-close-adapter
                    {:root (or (:cascade-continuation-close-root opts)
                               default-cascade-continuation-close-root)
                     :source-run-id (:source-run-id opts)
                     :source-attempt-id (:source-attempt-id opts)}))
               :failure-fn
               (or cascade-failure-fn
                   (cascade-continuation/production-failure-adapter
                    {:root (or (:cascade-continuation-failure-root opts)
                               default-cascade-continuation-failure-root)
                     :source-run-id (:source-run-id opts)
                     :source-attempt-id (:source-attempt-id opts)}))})))))
