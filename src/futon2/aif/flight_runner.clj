(ns futon2.aif.flight-runner
  "Production adapters for futon2.aif.flight: a click is one
  full_loop_runner/run-opportunity! with the flight on its judge options;
  an observation is the want locators checked through observation-checks.

  Kept apart from futon2.aif.flight so the flight core stays pure and its
  tests need no runner."
  (:require [babashka.http-client]
            [cheshire.core]
            [clojure.edn]
            [clojure.java.io :as io]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.interpretation-construction :as ic]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.task-execution-evidence]
            [futon2.aif.flight :as flight]
            [futon2.aif.interpretation-evidence]
            [futon2.aif.mission-criteria :as criteria]
            [futon2.aif.mission-reading :as reading]
            [futon2.aif.want-interpretation :as wi]
            [futon2.report.war-machine :as wm]))

(defn click-summary
  "What the flight needs from one run's RESULT (run-opportunity-core!'s
  map) for TARGET: the chosen plan's :unreached-wants, from the
  construction receipt the chosen action carries; and, when the tick
  abstained, the flight target's own decline from the abstention carrier
  the run record is built from (D8, 97a84770)."
  [target run-id result]
  (let [decision (or (get-in result [:checkpoints :selection :judgment :controller-decision])
                     (get-in result [:checkpoints :selection :judgment :decision]))
        sorry (get-in result [:checkpoints :selection :sorry])
        carrier (runner/abstention-carrier (or decision (:decision sorry))
                                           (:dropped-candidates sorry))
        action (:action decision)
        mine (when (= :abstained (:status carrier))
               (or (first (filter #(= target (:target %)) (:targets carrier)))
                   {:kind :target-not-in-refusals :missing :refusal}))]
    (cond-> {:click-id run-id
             :chosen (when (and action (= target (:target action)))
                       {:candidate (:id action)
                        :precedence (mapv #(or (:id %) %) (:precedence action))})
             :unreached-wants (vec (when (= target (:target action))
                                     (get-in action [:construction-receipt :unreached-wants])))}
      mine (assoc :abstention mine))))

(defn click-fn
  "A flight click function over RUN! (default runner/run-opportunity!):
  each call runs one opportunity with BASE-OPTS plus the flight's judge
  options, the flight on :flight so the default selection judge assembles
  only the flight's target (war_machine/flight-assembly-input). RUN-ID-FN
  names each run."
  [{:keys [base-opts run! run-id-fn]}]
  (let [run! (or run! runner/run-opportunity!)]
    (fn [judge-opts]
      (let [flight (:flight judge-opts)
            run-id ((or run-id-fn #(str (:flight/id flight) "-click-" (:click flight))))
            result (run! (assoc base-opts :run-id run-id :flight flight))]
        (click-summary (:target flight) run-id result)))))

(defn observe-fn
  "A flight observe function: check each want's locator. A refused check
  is :unknown, never false and never true, so it cannot count as a want
  advanced or met."
  ([] (observe-fn checks/observe))
  ([observe]
   (fn [_target locators]
     (let [{:keys [observed results refused]} (observe locators)]
       (merge (into {} (map (fn [t] [t (contains? observed t)])) (keys results))
              (into {} (map (fn [t] [t :unknown])) (keys refused)))))))

;; ---------------------------------------------------------------------------
;; D11 part 4: the flight asks for the interpretations its wants lack

(defn agency-answer-fn
  "An answer function that asks SEAT through Agency: a bell (mode work, the
  flight's target as the requisition) carrying the want-interpretation
  prompt, then a poll to a terminal state (a whistle would block the flight
  for the minutes an answer takes; a bellback has no return path to the
  machine's persona). Returns {:seat :job-id :state :text}. OPTS are the
  runner's (:agency-base, poll settings)."
  [{:keys [seat caller opts dispatch! poll! job-text prompt-fn]
    :or {caller "wm-flight" dispatch! runner/dispatch! poll! runner/poll-job!
         job-text futon2.aif.task-execution-evidence/job-text}}]
  (fn [issued]
    (let [prompt-fn (or prompt-fn (if (#{:locator :criteria} (:kind issued)) reading/prompt wi/prompt))
          sent (dispatch! opts seat caller (:target issued) (prompt-fn issued))
          job-id (:job-id sent)]
      (if-not job-id
        {:seat seat :state :not-dispatched :text nil :dispatch sent}
        (let [job (poll! opts job-id)]
          {:seat seat :job-id job-id :state (:state job) :text (job-text job)})))))

(defn target-view
  "The flight target's sources as the tick would see them: the flight's
  wants, locators and observations, and the published interpretations."
  [store flight wants sources]
  (let [target (:target flight)]
    (-> (wm/flight-assembly-input {:target target :wants (:wants wants)
                                   :locators (:locators wants) :universe (:universe wants)}
                                  {:targets [target] :sources sources})
        :sources
        (wi/merge-published store [target])
        (assoc :construction {:construct ic/construct
                              :budget (:value (wm/construction-budget sources))
                              :move-cost (:value wm/construction-move-cost)
                              :evaluate-g wm/constructed-candidate-g}))))

(defn- issue-request
  "Issue the request for WANT, or return {::refused refusal-kind}."
  [store view target want criterion request-options]
  (try (wi/issue! store (wi/request! {:target target :want want :criterion criterion
                                      :facts (get-in view [:universes target])
                                      :patterns (get-in view [:interpretations target :patterns])}
                                     (io/file store "evidence" target)
                                     (or request-options {})))
       (catch clojure.lang.ExceptionInfo e
         {::refused (:interpretation/refusal (ex-data e))})))

(defn- settle
  "Parse, validate and publish ANSWER to ISSUED; the outcome entry."
  [{:keys [store constraints admit code-root]} view issued answer base]
  (let [who (select-keys answer [:seat :job-id])
        base (merge base {:request-id (:request-id issued)} who)
        parsed (when (= "done" (:state answer)) (wi/parse-reply (:text answer)))]
    (cond
      (nil? parsed)
      (assoc base :outcome :not-answered :state (:state answer))

      (:unparseable-response parsed)
      (assoc base :outcome :unparseable-response :detail (:unparseable-response parsed))

      (:decline parsed)
      (assoc base :outcome :declined :decline (:decline parsed))

      :else
      (let [v (wi/validate-response issued (:response parsed)
                                    {:sources view :constraints constraints :admit admit
                                     :code-root (or code-root "/home/joe/code")})]
        (if (= :valid (:status v))
          (do (wi/publish! store issued (:response parsed) v {:answered-by who})
              (assoc base :outcome :published :pattern (ffirst (:interpretation v))))
          (assoc base :outcome :rejected :reasons (:reasons v)
                 ::retry {:issued issued :response (:response parsed) :who who}))))))

(defn- revalidate
  "Validate a rejected entry's answer again against the current VIEW (after
  another want's publication); the same answer, never a new request."
  [{:keys [store constraints admit code-root]} view {::keys [retry] :as entry}]
  (let [{:keys [issued response who]} retry
        v (wi/validate-response issued response
                                {:sources view :constraints constraints :admit admit
                                 :code-root (or code-root "/home/joe/code")})]
    (if (= :valid (:status v))
      (do (wi/publish! store issued response v {:answered-by who})
          (-> entry (dissoc ::retry :reasons)
              (assoc :outcome :published :pattern (ffirst (:interpretation v))
                     :published-on :revalidation)))
      entry)))

(defn- ask-one
  [{:keys [store answer-fn request-options] :as opts} flight wants sources want]
  (let [target (:target flight)
        view (target-view store flight wants sources)
        criterion (get-in wants [:source :criteria-by-token want])
        base {:want want}]
    (if-not criterion
      (assoc base :outcome :no-criterion)
      (let [issued (issue-request store view target want criterion request-options)]
        (if-let [r (::refused issued)]
          (assoc base :outcome :request-refused :refusal r)
          (settle opts view issued (answer-fn issued) base))))))

(defn- constraints-for
  "The owner constraints validation applies: those the want source read from
  the mission text. A DECLARED constraint (opts :constraints) is accepted
  only if the text states the same edge; one the text does not state
  refuses, so a hand declaration can never override or add to the mission's
  own words."
  [{declared :constraints} wants]
  (let [read (get-in wants [:source :constraints :requires])
        edge (juxt :want :requires)
        stated (set (map edge read))
        extra (remove (comp stated edge) declared)]
    (when (seq extra)
      (throw (ex-info "a declared constraint is not stated in the mission text"
                      {:interpretation/refusal :want/declared-constraint-not-in-text
                       :declared (vec extra) :read (vec read)})))
    (vec read)))

(defn ask-fn
  "The flight's ask step: for each want no admitted or published
  interpretation produces, issue a request (want-interpretation/issue! and
  request!), get an answer (ANSWER-FN, e.g. agency-answer-fn), parse it
  against the reply grammar, validate it, and publish it when valid. Wants
  are asked in order and each sees what the previous one published; after a
  publication, answers rejected earlier are validated again (the same
  answers, no new request) until nothing changes, so a chain whose wants
  are listed out of order (ARGUE before DOCUMENT) settles in one step.
  Owner constraints are those the want source read from the mission text
  (mission-criteria/constraints); a declared one must match a read one.
  Everything that is not a publication is also a flight :need with the job
  id, never retried silently: :no-criterion, :request-refused,
  :not-answered, :unparseable-response, :declined, :rejected."
  [{:keys [store admit] :as opts}]
  (let [opts (assoc opts :admit (or admit (var-get #'wm/admit-cascade-problem)))]
    (fn [flight wants sources]
      (let [opts (assoc opts :constraints (constraints-for opts wants))
            target (:target flight)
            view (target-view store flight wants sources)
            todo (wi/unproduced-wants (:wants wants) (get-in view [:universes target])
                                      (get-in view [:interpretations target :patterns]))
            first-pass (mapv #(ask-one opts flight wants sources %) todo)
            asked (loop [entries first-pass]
                    (let [view (target-view (:store opts) flight wants sources)
                          after (mapv #(if (::retry %) (revalidate opts view %) %) entries)]
                      (if (= (map :outcome after) (map :outcome entries))
                        (mapv #(dissoc % ::retry) after)
                        (recur after))))]
        {:asked asked
         :needs (vec (for [a asked :when (not= :published (:outcome a))]
                       (merge {:kind (:outcome a) :missing :interpretation}
                              (select-keys a [:want :request-id :seat :job-id]))))}))))

;; ---------------------------------------------------------------------------
;; Clicks through the serving JVM (POST /api/alpha/wm/click)

(defn record-summary
  "What the flight needs from a click's RUN RECORD (tick-run-record-<run-id>)
  for TARGET: the chosen plan's :unreached-wants (the record's
  [:decision :chosen], when it is this target's) and, when the tick
  abstained, the target's own decline from [:decision :abstention]."
  [target run-id record]
  (let [chosen (get-in record [:decision :chosen])
        carrier (get-in record [:decision :abstention])
        mine (when (= :abstained (:status carrier))
               (or (first (filter #(= target (:target %)) (:targets carrier)))
                   {:kind :target-not-in-refusals :missing :refusal}))]
    (cond-> {:click-id run-id
             :chosen (when (= target (:target chosen)) (select-keys chosen [:candidate :precedence]))
             :unreached-wants (vec (when (= target (:target chosen)) (:unreached-wants chosen)))}
      mine (assoc :abstention (select-keys mine [:target :kind :missing :declines]))
      (nil? record) (assoc :abstention {:kind :run-record-missing :missing :run-record}))))

(defn http-click-fn
  "A flight click function over the serving JVM: POST /api/alpha/wm/click
  with the flight as flight-edn and a dated run id, wait until that click is
  no longer running, then read its run record. The click is an ordinary
  click: it goes through the same budget and cast-seat preflight. A click
  the server does not start is recorded as an abstention
  (:click-not-started), never retried. Ports are injectable for tests."
  [{:keys [agency-base run-record-dir caller poll-ms post! get-status! read-record! sleep! today]
    :or {agency-base "http://localhost:7070" caller "wm-flight" poll-ms 5000
         run-record-dir runner/default-run-record-dir
         sleep! #(Thread/sleep (long %))
         today #(subs (str (java.time.Instant/now)) 0 10)}}]
  (let [post! (or post! (fn [body] (let [r (babashka.http-client/post
                                            (str agency-base "/api/alpha/wm/click")
                                            {:headers {"Content-Type" "application/json"}
                                             :body (cheshire.core/generate-string body)
                                             :throw false})]
                                     {:status (:status r) :body (cheshire.core/parse-string (:body r) true)})))
        get-status! (or get-status! (fn [] (-> (babashka.http-client/get (str agency-base "/api/alpha/wm/click")
                                                                         {:throw false})
                                               :body (cheshire.core/parse-string true))))
        read-record! (or read-record! (fn [run-id]
                                        (let [f (io/file run-record-dir (str "tick-run-record-" run-id ".edn"))]
                                          (when (.isFile f) (clojure.edn/read-string {:default tagged-literal} (slurp f))))))]
    (fn [judge-opts]
      (let [flight (:flight judge-opts)
            target (:target flight)
            run-id (str (today) "-" (:flight/id flight) "-click-" (:click flight))
            {:keys [status body]} (post! {:flight-edn (pr-str flight) :run-id run-id
                                          :issuing-caller caller :trigger "duree-click-on-demand"})
            click-id (:click-id body)]
        (if-not (and (= 200 status) click-id)
          {:click-id run-id
           :unreached-wants []
           :abstention {:kind :click-not-started :missing :click :status status
                        :detail (select-keys body [:error :message :rejected])}}
          (do (loop []
                (let [s (get-status!)]
                  (when (and (:running? s) (= click-id (:click-id s)))
                    (sleep! poll-ms)
                    (recur))))
              (assoc (record-summary target run-id (read-record! run-id)) :server-click-id click-id)))))))

;; ---------------------------------------------------------------------------
;; D11 part 5: the flight reads what the mission does not state

(defn- answered [answer]
  (select-keys answer [:seat :job-id]))

(defn- evidence-sha [text]
  (when text (futon2.aif.interpretation-evidence/sha256 (.getBytes ^String text "UTF-8"))))

(defn- read-one
  "Issue a reading request, answer it, parse, validate, publish. The entry."
  [{:keys [store answer-fn]} issued schema validate publish]
  (let [issued (wi/issue! store issued)
        answer (answer-fn issued)
        who (answered answer)
        base (merge {:kind (:kind issued) :want (get-in issued [:want :token]) :request-id (:request-id issued)} who)
        parsed (when (= "done" (:state answer)) (wi/parse-reply schema (:text answer)))]
    (cond
      (nil? parsed) (assoc base :outcome :not-answered :state (:state answer))
      (:unparseable-response parsed) (assoc base :outcome :unparseable-response :detail (:unparseable-response parsed))
      (:decline parsed) (assoc base :outcome :declined :decline (:decline parsed))
      :else (let [v (validate issued (:response parsed))]
              (if (= :valid (:status v))
                (do (publish issued (:response parsed) v who) (assoc base :outcome :published))
                (assoc base :outcome :rejected :reasons (:reasons v)))))))

(defn read-fn
  "The flight's read step (D11 part 5), run before the wants are read. When
  the :a-exits source found no criteria in a recognised form, it asks for
  criteria extracted from the mission text; then, for each criterion with
  no stated verdict and no published locator, it asks for a checkable
  locator. Everything that is not a publication is a flight :need with its
  job id; a mission is never refused for a missing list, and a typed
  absence arises only when a reading ran and found nothing."
  [{:keys [store read-text code-root] :as opts}]
  (fn [flight sources]
    (let [ws (:want-source flight)
          target (:target flight)
          mission (select-keys ws [:repo :path])
          text ((or read-text (:read-text ws) criteria/read-mission)
                (or code-root (:code-root ws) "/home/joe/code") (:repo ws) (:path ws))
          needed #(get-in (flight/click-wants flight sources) [:source :readings-needed])
          first-need (needed)
          criteria-entry
          (when (:criteria? first-need)
            (read-one opts (reading/criteria-request target mission (:sections-read first-need))
                      reading/criteria-schema
                      (fn [issued resp] (reading/validate-criteria issued resp text))
                      (fn [issued resp v who]
                        (reading/publish-criteria! store issued resp v who
                                                   (evidence-sha text)))))
          ;; locators are asked after any criteria publication, so newly
          ;; extracted criteria get theirs in the same step
          cw (flight/click-wants flight sources)
          by-token (get-in cw [:source :criteria-by-token])
          locator-entries
          (vec (for [t (get-in cw [:source :readings-needed :locators])]
                 (read-one opts (reading/locator-request target mission (assoc (get by-token t) :token t))
                           reading/locator-schema
                           (fn [issued resp] (reading/validate-locator issued resp (select-keys opts [:observe])))
                           (fn [issued resp v who] (reading/publish-locator! store issued resp v who)))))
          asked (vec (concat (when criteria-entry [criteria-entry]) locator-entries))]
      {:asked asked
       :needs (vec (for [a asked :when (not= :published (:outcome a))]
                     (merge {:kind (:outcome a) :missing (if (= :criteria (:kind a)) :criteria :locator)}
                            (select-keys a [:want :request-id :seat :job-id]))))})))
