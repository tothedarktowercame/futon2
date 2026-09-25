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
            [clojure.pprint]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.grain-gate :as gate]
            [futon2.aif.enactment-habit :as enactment-habit]
            [futon2.aif.cascade-prior :as cascade-prior]
            [clojure.java.shell]
            [futon2.aif.interpretation-construction :as ic]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.task-execution-evidence]
            [futon2.aif.flight :as flight]
            [futon2.aif.interpretation-evidence]
            [futon2.aif.mission-criteria :as criteria]
            [futon2.aif.mission-reading :as reading]
            [futon2.aif.served-by-reading :as served]
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

(def reading-kinds
  "Request kinds answered in mission-reading's grammar; any other kind is a
  want interpretation."
  #{:locator :criteria :coverage :constraints})

(defn agency-answer-fn
  "An answer function that asks SEAT through Agency: a bell (mode work, the
  flight's target as the requisition) carrying the want-interpretation
  prompt, then a poll to a terminal state (a whistle would block the flight
  for the minutes an answer takes; a bellback has no return path to the
  machine's persona). Returns {:seat :job-id :state :text :library-root}.
  OPTS are the runner's (:agency-base, poll settings).

  poll! is runner/poll-job!, which waits until the job reaches a terminal
  state (it records stalls and keeps waiting), so a real answer is settled
  before the flight's click posts. LIBRARY-ROOT (M-wm-wiring row 3) is
  passed to the want-interpretation prompt, which names it as the library
  the seat may search; when the flight's opts carry none, the prompt's own
  pinned default applies and the answer records that as
  {:absent :not-in-flight-opts}."
  [{:keys [seat caller opts dispatch! poll! job-text prompt-fn library-root]
    :or {caller "wm-flight" dispatch! runner/dispatch! poll! runner/poll-job!
         job-text futon2.aif.task-execution-evidence/job-text}}]
  (fn [issued]
    (let [prompt-fn (or prompt-fn
                        (cond (reading-kinds (:kind issued)) reading/prompt
                              library-root #(wi/prompt % {:library-root library-root})
                              :else wi/prompt))
          root (or library-root {:absent :not-in-flight-opts})
          ;; Kimi seats refuse a call without this line in the prompt
          ;; (Agency: "You can't use a Kimi seat without a requisition")
          requisition (str "Requisition: " (:target issued) " — War Machine "
                           (name (or (:kind issued) :interpretation)) " request "
                           (:request-id issued) "\n\n")
          sent (dispatch! opts seat caller (:target issued) (str requisition (prompt-fn issued)))
          job-id (:job-id sent)]
      (if-not job-id
        {:seat seat :state :not-dispatched :text nil :dispatch sent :library-root root}
        (let [job (poll! opts job-id)]
          {:seat seat :job-id job-id :state (:state job) :text (job-text job)
           :library-root root})))))

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
        base (merge base {:request-id (:request-id issued)} who
                    (select-keys answer [:library-root]))
        parsed (when (= "done" (:state answer)) (wi/parse-reply (:text answer)))]
    (cond
      ;; an answer still in flight when the step settles (the job exists and
      ;; is not terminal) is pending, not unanswered: the click cannot see it
      ;; yet, and the record says so (M-wm-wiring row 3)
      (and (nil? parsed) (string? (:state answer))
           (not (contains? runner/terminal-states (:state answer))))
      (assoc base :outcome :pending :state (:state answer))

      (nil? parsed)
      (assoc base :outcome :not-answered :state (:state answer))

      (:unparseable-response parsed)
      (assoc base :outcome :unparseable-response :detail (:unparseable-response parsed))

      (:decline parsed)
      (assoc base :outcome :declined :decline (:decline parsed))

      :else
      (let [base (assoc base :retrieval
                        (if-let [runs (seq (get-in parsed [:response :retrieval :runs]))]
                          {:appended-runs (count runs)}
                          {:absent :no-appended-runs}))
            v (wi/validate-response issued (:response parsed)
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
                              (select-keys a [:want :request-id :seat :job-id :state]))))}))))

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
  [{:keys [store answer-fn]} issued schema validate publish & [publish-questions]]
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
              (case (:status v)
                :valid (do (publish issued (:response parsed) v who) (assoc base :outcome :published))
                :questions (do (when publish-questions (publish-questions issued (:response parsed) v who))
                               (assoc base :outcome :questions :questions (:questions v)))
                (assoc base :outcome :rejected :reasons (:reasons v)))))))

(defn read-fn
  "The flight's read step (D11 part 5), run before the wants are read. When
  the :a-exits source found no criteria in a recognised form, it asks for
  criteria extracted from the mission text; then, for each criterion with
  no stated verdict and no published locator, it asks for a checkable
  locator. Everything that is not a publication is a flight :need with its
  job id; a mission is never refused for a missing list, and a typed
  absence arises only when a reading ran and found nothing."
  [{:keys [store read-text code-root notify! caller served-by-cascades served-by-quotes] :as opts}]
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
          ;; coverage (once per text, when criteria were found): what the
          ;; bullets miss, scope-outs, anchors
          cov-need (get-in (flight/click-wants flight sources) [:source :readings-needed])
          coverage-entry
          (when (:coverage? cov-need)
            (read-one opts (reading/coverage-request
                            target mission (:mission-sha cov-need)
                            (vec (for [[t c] (get-in (flight/click-wants flight sources) [:source :criteria-by-token])]
                                   {:token t :text (:stated c)})))
                      reading/criteria-schema
                      (fn [issued resp] (reading/validate-coverage issued resp text))
                      (fn [issued resp v who] (reading/publish-coverage! store issued resp v who))))
          ;; locators are asked after any criteria publication, so newly
          ;; extracted criteria get theirs in the same step
          cw (flight/click-wants flight sources)
          by-token (get-in cw [:source :criteria-by-token])
          locator-entries
          (vec (for [t (get-in cw [:source :readings-needed :locators])]
                 (read-one opts (reading/locator-request target mission (assoc (get by-token t) :token t))
                           reading/locator-schema
                           (fn [issued resp] (reading/validate-locator issued resp (assoc (select-keys opts [:observe]) :text text)))
                           (fn [issued resp v who] (reading/publish-locator! store issued resp v who))
                           (fn [issued resp v who] (reading/publish-locator-questions! store issued resp v who)))))
          ;; a declined locator is recorded for this text, not asked again
          _ (doseq [e locator-entries :when (= :declined (:outcome e))]
              (reading/record-locator-decline! store {:target target :want {:token (:want e)} :request-id (:request-id e)}
                                               (:decline e) (select-keys e [:seat :job-id])
                                               (get-in cw [:source :readings-needed :mission-sha])))
          ;; ordering dependencies stated in forms the reader does not
          ;; recognise, read once per text (after criteria, so extracted
          ;; ones are among the tokens an edge may join)
          cw2 (flight/click-wants flight sources)
          constraints-entry
          (when (get-in cw2 [:source :readings-needed :constraints?])
            (read-one opts (reading/constraints-request target mission
                                                        (get-in cw2 [:source :readings-needed :mission-sha])
                                                        (get-in cw2 [:source :known-tokens]))
                      reading/constraints-schema
                      (fn [issued resp] (reading/validate-constraints issued resp text))
                      (fn [issued resp v who] (reading/publish-constraints! store issued resp v who))))
          asked (vec (concat (when criteria-entry [criteria-entry]) (when coverage-entry [coverage-entry]) locator-entries
                             (when constraints-entry [constraints-entry])))
          ;; questions the criteria reading raised: sent to the owner the
          ;; mission names (else recorded for the requisition's caller),
          ;; never a refusal
          questions (vec (concat
                          (when (= :published (:outcome criteria-entry))
                            (reading/published-questions store target))
                          (for [e locator-entries :when (= :questions (:outcome e)) q (:questions e)]
                            (assoc q :want (:want e) :request-id (:request-id e)))
                          (when (= :published (:outcome coverage-entry))
                            (map #(assoc % :request-id (:request-id coverage-entry))
                                 (get-in (flight/click-wants flight sources) [:source :coverage-questions])))
                          (when (= :published (:outcome constraints-entry))
                            (map #(assoc % :request-id (:request-id constraints-entry))
                                 (get-in (flight/click-wants flight sources) [:source :constraint-questions])))))
          served-by (when text
                      (served/reading (str (:repo ws) "/" (:path ws)) text
                                      {:cascades (get served-by-cascades target)
                                       :quotes (get served-by-quotes target)}))
          owner (reading/mission-owner text)
          addressed (or owner caller "requisition-caller")
          notified (when (and (seq questions) owner notify!)
                     (notify! owner target (reading/question-prompt target owner questions)))]
      {:asked asked
       ;; M-wm-wiring row 2: the mission's outcomes and served-by links read
       ;; from its text now; seat quotes, when the caller supplies them
       ;; (:served-by-quotes), placed and verified against this text's sha
       :served-by served-by
       :text-sha256 (:text-sha256 served-by)
       :needs (vec (concat
                    (for [a asked :when (not (#{:published :questions} (:outcome a)))]
                      (merge {:kind (:outcome a) :missing (case (:kind a) :criteria :criteria :coverage :coverage :constraints :constraints :locator)}
                             (select-keys a [:want :request-id :seat :job-id])))
                    (for [q questions]
                      {:kind :owner-question :missing :owner-answer :to addressed
                       :notified (boolean notified) :notification (select-keys notified [:job-id])
                       :question (:question q) :span (:span q) :alternatives (:alternatives q)
                       :want (:want q)
                       :request-id (or (:request-id q) (:request-id criteria-entry))})))})))

;; ---------------------------------------------------------------------------
;; The enactment step (M-wm-wiring row 0, with rows 5 and 10's read side)

(declare observe-publication-fn)

(defn- grain-pattern
  "The chosen candidate's pattern whose interpretation declares :grain."
  [precedence interpretations]
  (first (filter #(get-in interpretations [% :grain]) precedence)))

(defn- run-check [check-fn check]
  (if (map? check)
    (let [r (try (check-fn check) (catch Exception e {:status :refused :reason :check-threw
                                                      :detail (.getMessage e)}))]
      (assoc check :result r))
    {:absent :no-check-from-seat}))

(defn enact-fn
  "The flight's enactment step: after a click that chose one of the flight
  target's candidates, have a seat carry out the candidate's pattern steps
  in precedence order and write the enactment record (the
  click-001-enactment.edn shape: attempts, checks, grain, deviations).

  OPTS:
    :dispatch-step!  (fn [step] ...) -> the seat's answer for one step. STEP
                     is {:target :candidate :pattern :n :interpretation
                     :phase :plan|:commit}. A :plan answer (asked only for the
                     grain attempt) is {:grain {...}}, the grain the attempt
                     will build. A :commit answer is {:commit sha :produced
                     token :check {..check locator..}}, or {:failed {:reason
                     kw ...}}.
    :check-fn        (fn [check] -> {:observed bool ...} or a refusal):
                     observes the attempt's check (default the observation
                     check for the locator's :class).
    :interpretations (fn [flight] -> {pattern-id interpretation}), the
                     interpretations the candidate's patterns carry.
    :fetch-run-record (fn [click-id] -> run record or nil), the click's run
                     record, referenced by id for the W_c checker.
    :publication-observation (fn [flight click] -> observation); default
                     observe-publication-fn over :fetch-run-record and
                     :repair-id-fn (fn [flight click] -> the repair id the
                     chosen action discharges, or nil).
    :record-dir      where the record is written: <store>/flights/enactments.
    :repo-root       for the grain gate's evidence files.

  The grain attempt is the step at the pattern whose interpretation declares
  :grain; grain-gate (candidate grain vs the grain the seat plans) runs
  before its commit is asked for, and a refusal is recorded on the attempt
  with no commit asked. With no such pattern the record says
  {:absent :candidate-names-no-grain-pattern} and the gate's own refusal
  (:grain-not-declared) is recorded; the flight continues either way.
  A click with no chosen candidate writes no record: {:absent :no-decision}."
  [{:keys [dispatch-step! check-fn interpretations fetch-run-record
           publication-observation repair-id-fn record-dir repo-root]
    :or {check-fn (fn [check] (if-let [f (get checks/checks (:class check))]
                                (f check)
                                {:status :refused :reason :no-mechanical-check}))
         repo-root "/home/joe/code/futon3c"}}]
  (fn [flight click]
    (let [chosen (:chosen click)]
      (if-not (and chosen (:candidate chosen))
        {:absent :no-decision :click-id (:click-id click)}
        (let [precedence (vec (:precedence chosen))
              interps (if interpretations (interpretations flight) {})
              grain-p (grain-pattern precedence interps)
              cand-grain (when grain-p (get-in interps [grain-p :grain]))
              base-step {:target (:target flight) :candidate (:candidate chosen)}
              attempts
              (vec
               (for [[i p] (map-indexed vector precedence)
                     :let [step (assoc base-step :pattern p :n (inc i)
                                       :interpretation (get interps p))]]
                 (if (= p grain-p)
                   (let [plan (dispatch-step! (assoc step :phase :plan))
                         g (gate/grain-gate {:grain cand-grain} {:grain (:grain plan)} repo-root)]
                     (if (= :pass (:status g))
                       (let [c (dispatch-step! (assoc step :phase :commit))]
                         (if (:failed c)
                           {:n (inc i) :pattern p :success false :grain-gate g :failed (:failed c)}
                           (let [chk (run-check check-fn (:check c))]
                             {:n (inc i) :pattern p :commit (:commit c) :produced (:produced c)
                              :grain (:grain plan) :grain-gate g
                              :check chk :success (true? (:observed (:result chk)))})))
                       {:n (inc i) :pattern p :success false :grain (:grain plan) :grain-gate g
                        :not-committed :grain-gate-refused}))
                   (let [c (dispatch-step! (assoc step :phase :commit))]
                     (if (:failed c)
                       {:n (inc i) :pattern p :success false :failed (:failed c)}
                       (let [chk (run-check check-fn (:check c))]
                         {:n (inc i) :pattern p :commit (:commit c) :produced (:produced c)
                          :check chk :success (true? (:observed (:result chk)))}))))))
              deviations (vec (concat
                               (for [a attempts :when (:failed a)]
                                 {:kind :step-failed :pattern (:pattern a) :failed (:failed a)})
                               (for [a attempts :when (:not-committed a)]
                                 {:kind :grain-gate-refused :pattern (:pattern a)
                                  :reason (get-in a [:grain-gate :reason])})))
              run-record (when fetch-run-record (fetch-run-record (:click-id click)))
              ;; row 10's read side: the observation's writer is
              ;; observe-publication-fn (step 12); this record copies it
              pub (:publication-observed
                   ((or publication-observation
                        (observe-publication-fn {:fetch-run-record fetch-run-record
                                                 :repair-id-fn repair-id-fn}))
                    flight click))
              record (cond-> {:schema :wm/enactment-v1
                              :flight (:flight/id flight)
                              :click (:click-id click)
                              :candidate (:candidate chosen)
                              ;; the decision's id, so W_c's join is checkable
                              :decision-candidate (:candidate chosen)
                              :run-record (if run-record
                                            {:click-id (:click-id click) :present true}
                                            {:click-id (:click-id click) :absent :run-record-not-fetched})
                              :grain (or cand-grain {:absent :candidate-names-no-grain-pattern})
                              :grain-attempt (if grain-p
                                               {:pattern grain-p}
                                               {:absent :candidate-names-no-grain-pattern})
                              :attempts attempts
                              :conformance {:deviations deviations}
                              :publication-observed pub}
                       (nil? grain-p)
                       (assoc :grain-gate (gate/grain-gate {:grain nil} {:grain nil} repo-root)))
              path (when record-dir
                     (io/file record-dir (str (:flight/id flight) "-" (:click-id click) ".edn")))]
          (when path
            (.mkdirs (.getParentFile path))
            (spit path (with-out-str (clojure.pprint/pprint record))))
          {:enactment record :record-path (some-> path .getCanonicalPath)})))))

(defn wc-verdict-fn
  "The flight's W_c call (M-wm-wiring step 11): after enact-fn writes the
  enactment record, run the W_c checker (futon3c proof2a_check.clj, one
  checker for hand and machine records) on the click's run record and the
  enactment record with --wc --edn, read its EDN verdict, and hand it to
  enactment-habit/increment UNCHANGED (a vector of failures, [] a pass, or
  the typed {:status :join-unverifiable ...}; 531cfaaa passes that status
  through as delta 0).

  OPTS: :checker (path to proof2a_check.clj) and :bb (the binary, default
  \"bb\"); :click-record-path (fn [click-id] -> path); :identity-fn (fn
  [flight enactment] -> policy key; default the target and the attempts'
  patterns in order, :semilattice {}); :increment! (default
  enactment-habit/increment). No :checker: {:wc {:absent
  :no-wc-checker-configured}} and increment is not called (never a default
  pass). A non-zero exit or output that is not one EDN form is {:wc
  {:refused :checker-failed :exit n :stderr s}}, never a verdict."
  [{:keys [checker bb click-record-path identity-fn increment!]
    :or {bb "bb" increment! enactment-habit/increment}}]
  (fn [flight {:keys [enactment record-path]}]
    (cond
      (nil? enactment) nil
      (nil? checker) {:wc {:absent :no-wc-checker-configured}}
      :else
      (let [click-path (when click-record-path (click-record-path (:click enactment)))
            {:keys [exit out err]} (clojure.java.shell/sh bb (str checker) (str click-path) (str record-path)
                                                          "--wc" "--edn")
            verdict (when (zero? exit)
                      (try (let [v (clojure.edn/read-string out)]
                             (when (or (vector? v) (map? v)) v))
                           (catch Exception _ nil)))]
        (if (nil? verdict)
          {:wc {:refused :checker-failed :exit exit :stderr (str err)}}
          (let [identity ((or identity-fn
                              (fn [f e] (cascade-prior/policy-key {:mission (:target f)
                                                                   :shown (mapv :pattern (:attempts e))
                                                                   :semilattice {}})))
                          flight enactment)]
            {:wc {:verdict verdict :click-record click-path}
             :increment (increment! enactment identity verdict)}))))))

(defn- admit-observation
  "The writer's rule: :observed true must carry its evidence, or it is a
  value standing in for an observation, refused with its reason."
  [obs]
  (if (and (true? (:observed obs)) (empty? (:evidence obs)))
    {:absent :observation-refused :reason :observed-true-without-evidence}
    obs))

(defn observe-publication-fn
  "Row 10 (H-publish): did the click's chosen action publish? Publication, in
  PROOF-2a's H-publish, is a repair obligation's discharge receipt reaching
  the store: the tick's catch-up! (repair-discharge-receipt/catch-up!) runs
  publication-result! for every resolution and the run record carries the
  results under :repair/publication, one per :repair/id, :status
  :receipt-committed when it published (else :publication-refused or
  :publication-unreachable).

  Returns (fn [flight click] -> {:publication-observed observation}); the
  enactment step records that value (one authority: this function writes
  it, enact-fn reads it):
    {:observed true :at click-id :evidence entry}      the target's receipt committed
    {:observed false :checked {...}}                   its entry, not committed, or none
    {:absent :no-repair-obligation-for-target ...}     the chosen action discharges no
                                                       repair obligation (publication
                                                       does not apply)
    {:absent :no-publication-observation-source ...}   the run record carries no
                                                       :repair/publication
  OPTS: :fetch-run-record (fn [click-id] -> run record); :repair-id-fn (fn
  [flight click] -> the repair id the chosen action discharges, or nil);
  :observation-fn replaces the reading (tests only). An :observed true with
  no evidence is refused by the writer."
  [{:keys [fetch-run-record repair-id-fn observation-fn]}]
  (fn [flight click]
    {:publication-observed
    (admit-observation
     (if observation-fn
       (observation-fn flight click)
       (let [repair-id (when repair-id-fn (repair-id-fn flight click))
             record (when fetch-run-record (fetch-run-record (:click-id click)))
             entries (:repair/publication record)]
         (cond
           (nil? repair-id)
           {:absent :no-repair-obligation-for-target :target (:target flight)}
           (not (sequential? entries))
           {:absent :no-publication-observation-source
            :missing "[:repair/publication] on the click's run record" :click-id (:click-id click)}
           :else
           (let [mine (filterv #(= repair-id (:repair/id %)) entries)
                 committed (first (filter #(= :receipt-committed (:status %)) mine))]
             (if committed
               {:observed true :at (:click-id click) :evidence committed}
               {:observed false
                :checked {:repair/id repair-id :click-id (:click-id click)
                          :entries (count entries)
                          :statuses (mapv :status mine)}}))))))}))
