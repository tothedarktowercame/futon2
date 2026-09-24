(ns futon2.aif.flight-runner
  "Production adapters for futon2.aif.flight: a click is one
  full_loop_runner/run-opportunity! with the flight on its judge options;
  an observation is the want locators checked through observation-checks.

  Kept apart from futon2.aif.flight so the flight core stays pure and its
  tests need no runner."
  (:require [clojure.java.io :as io]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.interpretation-construction :as ic]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.task-execution-evidence]
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
  [{:keys [seat caller opts dispatch! poll! job-text]
    :or {caller "wm-flight" dispatch! runner/dispatch! poll! runner/poll-job!
         job-text futon2.aif.task-execution-evidence/job-text}}]
  (fn [issued]
    (let [sent (dispatch! opts seat caller (:target issued) (wi/prompt issued))
          job-id (:job-id sent)]
      (if-not job-id
        {:seat seat :state :not-dispatched :text nil :dispatch sent}
        (let [job (poll! opts job-id)]
          {:seat seat :job-id job-id :state (:state job) :text (job-text job)})))))

(defn- target-view
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

(defn ask-fn
  "The flight's ask step: for each want no admitted or published
  interpretation produces, issue a request (want-interpretation/issue! and
  request!), get an answer (ANSWER-FN, e.g. agency-answer-fn), parse it
  against the reply grammar, validate it, and publish it when valid. Wants
  are asked in order and each sees what the previous one published; after a
  publication, answers rejected earlier are validated again (the same
  answers, no new request) until nothing changes, so a chain whose wants
  are listed out of order (ARGUE before DOCUMENT) settles in one step.
  Everything that is not a publication is also a flight :need with the job
  id, never retried silently: :no-criterion, :request-refused,
  :not-answered, :unparseable-response, :declined, :rejected."
  [{:keys [store admit] :as opts}]
  (let [opts (assoc opts :admit (or admit (var-get #'wm/admit-cascade-problem)))]
    (fn [flight wants sources]
      (let [target (:target flight)
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
