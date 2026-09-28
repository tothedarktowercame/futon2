(ns futon2.aif.flight
  "A flight: the clicks on one mission (or other target), ending in closure
  (PROOF-2a, \"A flight: the clicks on one mission, ending in closure\").

  The target is chosen once and stays fixed across the flight's clicks;
  open stop-lines come first (bbae7593: repair fixes go to the front of the
  queue). Each click's wants are the target's wants from a pluggable WANT
  SOURCE, plus every want an earlier click left unreached, so nothing is
  dropped between clicks. Each click records which wants it advanced
  (false before, true after); a click that advances none is recorded as
  :no-progress and ends the flight, because clicks carry a heavy overhead and
  a click that moves nothing is a counterexample, not a step.

  `run!` calls the injected click and observe functions; `judge-opts` reads
  the last enactment publication through its digest-checked citation."
  (:refer-clojure :exclude [run!])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.cascade-model-manifest :as manifest]
            [futon2.aif.cascade-policy :as policy]
            [futon2.aif.cascade-problems :as cascade-problems]
            [futon2.aif.interpretation-evidence :as ievidence]
            [futon2.aif.mission-criteria :as criteria]
            [futon2.aif.lifecycle-exits :as exits]
            [futon2.aif.mission-reading :as reading]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.outcome-wants :as outcome-wants]
            [futon2.aif.repair-proposals :as repairs]
            [futon2.aif.temporal-update :as temporal-update])
  (:import [java.util UUID]))

;; ---------------------------------------------------------------------------
;; Want sources

(defmulti source-wants
  "The target's wants from a want source. Returns
  {:wants [token …] :source {:kind … …}}, and, for wants the tick's
  sources do not already locate, :locators {token locator} and
  :universe {token bool}. SOURCES is the tick's assembled
  sources map; FLIGHT the flight record."
  (fn [want-source _flight _sources] (:kind want-source)))

;; The checkbox reader the tick already runs (mission_hole_wants merges one
;; :hole/h<id> want per unchecked `- [ ]` task into :wants).
(defmethod source-wants :checkbox [_ {:keys [target]} sources]
  {:wants (vec (get-in sources [:wants target]))
   :source {:kind :checkbox :via "futon2.aif.mission-hole-wants"}})

;; The mission's completion criteria read from its text (A-exits,
;; futon2.aif.mission-criteria), plus the checkbox wants. The want source
;; names the mission file: {:kind :a-exits :repo … :path … :code-root …}.
;; A criterion with no stated verdict is a want with no locator; assembly
;; then refuses the target naming it, which is the hole to close.
(defmethod source-wants :a-exits [{:keys [repo path code-root read-text observe lifecycle store]} {:keys [target]} sources]
  (let [read (or read-text criteria/read-mission)
        root (or code-root "/home/joe/code")
        text (read root repo path)
        store (or store reading/default-store)
        ;; a declared lifecycle file: phases judged in data only are named
        ;; out of view, so a closure over the stated criteria is never read
        ;; as the mission's completion
        out-of-view (when lifecycle
                      (some-> (read root (:repo lifecycle) (:path lifecycle))
                              criteria/data-only-phases))
        mission-sha (when text (reading/text-sha text))
        stated (criteria/criteria target (or text ""))
        ;; coverage reading (once per text): criteria the bullets miss,
        ;; scope-outs, anchors for found criteria
        coverage (when (seq stated) (reading/published-coverage store target mission-sha))
        stated (vec (concat stated (remove (set (map :token stated)) (:criteria coverage))))
        ;; D11 part 5: no criteria in a recognised form is not a refusal;
        ;; criteria extracted by a reading (published, cues still resolving)
        ;; stand in, and until one exists the source asks for it
        extracted (when (empty? stated) (reading/published-criteria store target text))
        cs (if (seq stated) stated extracted)
        w (criteria/wants cs (cond-> {:repo repo :path path} observe (assoc :observe observe)))
        ;; a criterion with no stated verdict: a published machine locator
        ;; decides it, observed each click like a stated verdict's
        machine (select-keys (reading/published-locators store target) (map :token (:unlocated w)))
        ;; a criterion whose locator reading raised owner questions waits
        ;; for its owner: not a want, named out of view
        questioned (select-keys (reading/published-locator-questions store target)
                                (remove (set (keys machine)) (map :token (:unlocated w))))
        observe-loc (or observe #(contains? (:observed (checks/observe {::t %})) ::t))
        definition (try (read root "futon4" "holes/mission-lifecycle.md")
                        (catch Exception _ nil))
        ;; A definition that cannot be read, or that does not yield the eight
        ;; phase exits, adds no wants and is a typed absence on the record: it
        ;; never stops the wants step (claude-8, after EXIT-WANTS-WIRE-I: a
        ;; thrown :invalid-lifecycle-definition aborted the click at :wants).
        secondary (when definition
                    (try (exits/flight-exits target (or text "") definition
                                             {:repo repo :path path :observe observe-loc})
                         (catch clojure.lang.ExceptionInfo e
                           (if (= :invalid-lifecycle-definition (:kind (ex-data e)))
                             {::invalid (select-keys (ex-data e) [:kind :expected])}
                             (throw e)))))
        invalid (::invalid secondary)
        secondary (when-not invalid secondary)
        lifecycle-exits (cond
                          invalid {:absent :lifecycle-definition-invalid :detail invalid}
                          definition
                          {:current-phase (:current-phase secondary)
                           :supplied (:wants secondary) :not-started (:not-started secondary)
                           :not-counted (vec (for [[t c] (:criteria-by-token secondary)
                                                  :when (:not-counted c)]
                                              (assoc (:not-counted c) :token t :phase (:phase c))))}
                          :else {:absent :lifecycle-definition-unreadable})
        declined (reading/published-locator-declines store target mission-sha)
        text-constraints (criteria/constraints target (or text ""))
        read-constraints (reading/published-constraints store target mission-sha)
        still-unlocated (vec (remove #(or (contains? machine (:token %)) (contains? questioned (:token %)))
                                     (:unlocated w)))
        to-ask (vec (remove #(contains? declined (:token %)) still-unlocated))]
    {:wants (vec (distinct (remove (set (keys questioned))
                                   (concat (get-in sources [:wants target]) (:wants w) (:wants secondary)))))
     :locators (merge (:locators secondary) (:locators w) machine)
     :universe (merge (:universe secondary) (:universe w) (into {} (for [[t l] machine] [t (boolean (observe-loc l))])))
     :source {:kind :a-exits :via "futon2.aif.mission-criteria"
              :repo repo :path path :text-read? (some? text)
              :lifecycle-exits lifecycle-exits
              :criteria (count cs)
              :criteria-from (cond (seq stated) :mission-text (seq extracted) :machine-reading :else :none)
              :machine-located (vec (sort-by str (keys machine)))
              :unlocated (mapv #(cond-> % (contains? declined (:token %))
                                  (assoc :reason :locator-declined
                                         :decline (get-in declined [(:token %) :decline :reason])))
                               still-unlocated)
              ;; the readings this source still needs; the flight's read
              ;; step asks for them before the click (never a refusal)
              :readings-needed {:criteria? (empty? cs)
                                :coverage? (and (some? text) (seq (criteria/criteria target text)) (nil? coverage))
                                :locators (mapv :token to-ask)
                                ;; dependencies stated in forms the reader does
                                ;; not recognise are read once per text
                                :constraints? (and (some? text) (nil? read-constraints))
                                :mission-sha mission-sha
                                :sections-read (vec (keep #(second (re-matches #"^#+\s+(.*)$" %))
                                                          (str/split-lines (str text))))}
              ;; ordering constraints the mission states in its own words
              ;; the text's own recognised form, plus a reading's (by
              ;; :machine-reading) for the text as it stands
              :constraints (update text-constraints :requires into (:constraints read-constraints))
              :constraint-questions (:questions read-constraints)
              ;; every token an edge may join: each want's criterion and each
              ;; fact's own line
              :known-tokens (vec (concat
                                  (for [c (:criteria w)] {:token (:token c) :text (:stated c)})
                                  (for [[t l] (get-in sources [:locators target])
                                        :when (string? (:decl l))]
                                    {:token t :text (:decl l)})))
              ;; token -> the criterion it was read from, for the D11 request
              :criteria-by-token (merge
                                  (:criteria-by-token secondary)
                                  ;; a checkbox want's criterion is its own task
                                  ;; line, unchecked (its locator's :decl is the
                                  ;; checked form)
                                  (into {} (for [t (get-in sources [:wants target])
                                                 :let [d (get-in sources [:locators target t :decl])]
                                                 :when (and (string? d) (re-find #"^[-*]\s+\[x\]" d))]
                                             [t {:kind :checkbox-task :phase "checkbox"
                                                 :stated (str/replace-first d #"\[x\]" "[ ]")}]))
                                  (into {} (map (fn [c] [(:token c)
                                                         (cond-> (select-keys c [:kind :line :phase :stated])
                                                           (get-in coverage [:anchors (:token c)])
                                                           (assoc :anchor (get-in coverage [:anchors (:token c)])))]))
                                        (:criteria w)))
              ;; phases judged in data only, and findings the owner retains
              ;; as not met: neither is a want, both are named
              :coverage-questions (:questions coverage)
              :out-of-view (vec (concat out-of-view (:retained w) (:scope-outs coverage)
                                        (for [[t qs] questioned]
                                          {:token t :reason :owner-question :questions qs})))
              :lifecycle lifecycle}}))

(defn- git-primary-seam
  [{:keys [repo path code-root rev]
    :or {code-root "/home/joe/code" rev "HEAD"}}]
  (let [repo-dir (str (io/file code-root repo))
        git (fn [& args]
              (apply sh/sh "env" "-u" "GIT_DIR" "-u" "GIT_WORK_TREE"
                     "git" "-C" repo-dir args))
        resolved (git "rev-parse" "--verify" (str rev "^{commit}"))
        read-sha (some-> (:out resolved) str/trim not-empty)
        shown (when (zero? (:exit resolved)) (git "show" (str read-sha ":" path)))
        absent (fn [reason]
                 {:absence {:absent reason :repo repo :path path :read-sha read-sha}})]
    (cond
      (not (zero? (:exit resolved)))
      (absent :seam-document-unreadable)

      (not (zero? (:exit shown)))
      (absent :no-seam-document)

      :else
      (let [text (:out shown)
            bytes (.getBytes ^String text "UTF-8")
            parsed (try {:document (edn/read-string {:default tagged-literal} text)}
                        (catch Exception _ {:unreadable true}))
            document (:document parsed)
            read {:repo repo :path path :sha read-sha
                  :sha256 (ievidence/sha256 bytes)}]
        (cond
          (:unreadable parsed)
          (assoc (absent :seam-document-unreadable) :read read)

          (not= :wm/primary-wants-seam-v1 (:schema document))
          (assoc (absent :seam-schema-mismatch) :read read)

          :else
          (let [ancestor (git "merge-base" "--is-ancestor"
                              (str (get-in document [:source :commit])) read-sha)]
            (if (zero? (:exit ancestor))
              {:document document :read read}
              (assoc (absent :seam-source-not-ancestor) :read read))))))))

(defn- primary-seam-result [document observe read]
  (let [target (:target document)
        outcomes (:outcomes document)
        extractor (:extractor document)
        criterion (fn [outcome]
                    (assoc (outcome-wants/outcome-criterion target outcome)
                           :provenance (:provenance outcome)))
        wait-reason (fn [outcome]
                      (let [locator (:locator outcome)]
                        (cond
                          (some #(= {:absent :unconfirmed} (:confirmed %))
                                (get-in outcome [:provenance :steps]))
                          :unconfirmed-classification

                          (or (= :no-admitted-locator (:absent locator))
                              (not (contains? cascade-problems/checkable-classes
                                              (:class locator))))
                          :no-admitted-locator)))
        waiting (keep (fn [outcome]
                        (when-let [reason (wait-reason outcome)]
                          (merge (select-keys (criterion outcome) [:token :stated :role])
                                 {:reason reason}
                                 (select-keys (:locator outcome) [:would-be :why]))))
                      outcomes)
        admitted (remove wait-reason outcomes)
        published (into {} (map (fn [outcome]
                                  [(:token (criterion outcome)) (:locator outcome)]))
                        admitted)
        observe-loc (or observe #(contains? (:observed (checks/observe {::t %})) ::t))
        wants (outcome-wants/wants target admitted published observe-loc)
        criteria-by-token (into {} (map (fn [outcome]
                                         (let [c (criterion outcome)] [(:token c) c])))
                                outcomes)
        served-by (:served-by extractor)
        served-by-for (fn [source-outcome]
                        (if extractor
                          (if (sequential? served-by)
                            (filterv (fn [entry]
                                       (some #(= source-outcome (:outcome %))
                                             (:serves entry)))
                                     served-by)
                            [])
                          {:absent :not-in-seam-document}))
        c {:status :derived
           :source :primary-seam
           :read (or read {:absent :not-read-from-git})
           :weighting (if extractor
                        (:weighting extractor)
                        {:absent :not-in-seam-document})
           :outcomes (mapv (fn [outcome]
                             (let [criterion (criterion outcome)
                                   source-outcome (or (:source-outcome outcome)
                                                      {:absent :not-extracted})]
                               {:token (:token criterion)
                                :source-outcome source-outcome
                                :served-by (served-by-for source-outcome)}))
                           admitted)}]
    {:wants (:wants wants)
     :locators (:locators wants)
     :universe (:universe wants)
     :c c
     :source (cond-> {:kind :primary-seam
                      :via "futon2.aif.outcome-wants"
                      :document-source (:source document)
                      :unlocated (vec waiting)
                      :criteria-by-token criteria-by-token}
               read (assoc :read read))}))

(defmethod source-wants :primary-seam [{:keys [document observe] :as source} _flight _sources]
  (if document
    (primary-seam-result document observe nil)
    (let [{:keys [document read absence]} (git-primary-seam source)]
      (if document
        (primary-seam-result document observe read)
        {:wants [] :locators {} :universe {}
         :source (merge {:kind :primary-seam} absence
                        (when read {:read read}))}))))

;; A hand-declared list, for tests. Typed on every record it reaches, so a
;; reader can never mistake it for wants the machine read from the mission.
(defmethod source-wants :operator-declared [{:keys [wants declared-by]} _ _]
  {:wants (vec wants)
   :source {:kind :operator-declared :declared-by declared-by
            :note "test-only want list; not read from the mission text"}})

;; ---------------------------------------------------------------------------
;; Choosing the target

(defn choose-target
  "The flight's target. The oldest open stop-line's repair target comes
  first; otherwise REQUESTED. Returns {:target … :chosen-because …}, or nil
  when there is neither."
  [{:keys [open-obligations requested]}]
  (if-let [ob (first (sort-by (juxt :opened-at :repair/id) open-obligations))]
    {:target (repairs/target-id (:repair/id ob))
     :chosen-because {:kind :open-stop-line :repair/id (:repair/id ob)
                      :authority "bbae7593: repair fixes go to the front of the queue"
                      :open-stop-lines (count open-obligations)}}
    (when requested
      {:target requested :chosen-because {:kind :requested}})))

(defn start
  "A new flight on CHOSEN (from `choose-target`) with WANT-SOURCE."
  [chosen want-source & [{:keys [id at]}]]
  (merge chosen
         {:flight/id (or id (str "flight-" (UUID/randomUUID)))
          :started-at at
          :want-source want-source
          :carried-wants []
          :needs []
          :clicks []
          :status :open}))

;; ---------------------------------------------------------------------------
;; One click

(defn click-wants
  "The wants for the flight's next click: the want source's wants plus every
  want carried from earlier clicks, in first-seen order."
  [flight sources]
  (let [{:keys [wants source locators universe] :as result}
        (source-wants (:want-source flight) flight sources)]
    (cond-> {:wants (vec (distinct (concat wants (:carried-wants flight))))
             :source source
             :locators (or locators {})
             :universe (or universe {})}
      (contains? result :c) (assoc :c (:c result)))))

(defn judge-opts
  "What the flight passes the tick's judge: the fixed target and the wants
  to use for it. The judge considers only this target (flight rule 4)."
  [flight wants]
  (cond-> {:flight {:flight/id (:flight/id flight)
            :target (:target flight)
            :wants (:wants wants)
            :locators (:locators wants)
            :universe (:universe wants)
            :want-source (:source wants)
            :click (inc (count (:clicks flight)))}}
    (contains? wants :c)
    (assoc-in [:flight :c] (:c wants))
    (seq (:enactments flight))
    (assoc-in [:flight :temporal-previous]
              (temporal-update/read-receipt (:temporal-receipt (peek (:enactments flight)))))))

(defn advanced
  "Wants false or unknown BEFORE and true AFTER."
  [wants before after]
  (vec (filter #(and (not (true? (get before %))) (true? (get after %))) wants)))

(defn click-failure
  "A flight record click ENTRY's :failure (WM-CLICK-REASON-I), or
  {:absent :no-failure-on-click-entry} for an entry written before it, so
  an older entry is typed rather than read as a click that did not fail."
  [entry]
  (if (contains? entry :failure)
    (:failure entry)
    {:absent :no-failure-on-click-entry}))

;; ---------------------------------------------------------------------------
;; The observation after an enacted step (F1a-1, F1b-I; PROOF-2a-PLAN <2>2d F1)

(defn- by-str [] (sorted-set-by #(compare (str %1) (str %2))))

(defn step-observation
  "The observation o that followed an enacted step: the check channel's
  RECORDED VERDICTS, as TokenObservation's tokenLikelihood r s o reads them
  (o = the tokens the channel reported). No admission is applied: admission
  (observation-admission/admit, a blinded review) makes the independent
  REFERENCE labels measured A is counted against, not the observation
  (F1b-D, futon3c d8b6bf7f).

  Sources: the after-observation AFTER ({token bool}; a non-boolean reading
  is a refused check, so the token is unchecked, not false) and the
  enactment's ATTEMPTS (each produced token's check result). :checked is
  every token with a boolean verdict (V for this step), :o the checked
  tokens whose verdict is true (o is a subset of checked), :channel the class
  whose check produced each verdict (the attempt's :check :class, or the
  locator's :class in LOCATORS for the after-observation). UNIVERSE is the
  flight's view of the target's tokens (the want source's universe and
  wants); a universe token not checked is :unobserved -- marginalised,
  never absent. Returns {:schema :wm/step-observation-v2 :status :observed
  ...}, or :status :nothing-observed when no token was checked."
  [universe after attempts locators click-id]
  (let [from-attempts (into {} (for [a attempts
                                     :let [r (get-in a [:check :result :observed])]
                                     :when (and (some? (:produced a)) (boolean? r))]
                                 [(:produced a) {:verdict r :class (get-in a [:check :class])}]))
        from-after (into {} (for [[t v] after :when (boolean? v)]
                              [t {:verdict v :class (get-in locators [t :class])}]))
        ;; the after-observation is the later reading, so its verdict stands;
        ;; its class comes from the locator, else from the attempt's check
        verdicts (merge-with (fn [att aft] (cond-> aft (nil? (:class aft)) (assoc :class (:class att))))
                             from-attempts from-after)
        checked (into (by-str) (keys verdicts))
        universe (into (set universe) checked)]
    {:schema :wm/step-observation-v2
     :status (if (seq checked) :observed :nothing-observed)
     :click-id click-id
     :o (into (by-str) (keep (fn [[t {:keys [verdict]}]] (when verdict t))) verdicts)
     :checked checked
     :channel (into (sorted-map-by #(compare (str %1) (str %2)))
                    (for [[t {:keys [class]}] verdicts]
                      [t (or class {:absent :no-class-on-check})]))
     :unobserved (into (by-str) (remove checked) universe)
     :universe {:source :flight-want-source
                :sha256 (ievidence/sha256 (.getBytes (pr-str (vec (sort-by str universe))) "UTF-8"))
                :count (count universe)}}))

;; ---------------------------------------------------------------------------
;; The conditioning step (F1b-join-I; PROOF-2a-PLAN <2>2d F1; F1c-D futon3c
;; 8cc2d425, SPEC-F s1): the six fields of one step bound together, with the
;; exact posterior and its F term.

(defn- target-local
  "The part of a target-qualified map {[target token] v} that belongs to
  TARGET, keyed by token."
  [target m]
  (into {} (for [[k v] m :when (and (vector? k) (= target (first k)))] [(second k) v])))

(defn- target-marginal
  "BELIEF over target-qualified token states ({#{[target token] ...} mass}),
  marginalised to TARGET's own tokens."
  [target belief]
  (reduce-kv (fn [acc st mass]
               (update acc (set (for [k st :when (and (vector? k) (= target (first k)))] (second k)))
                       (fnil + 0) mass))
             {} belief))

(defn- prior-step
  "The last earlier step for POLICY-KEY, including typed absences. Never
  skip an ended chain to recover an older posterior or the initial belief."
  [enactments policy-key]
  (some->> enactments
           (map :step)
           (filter #(= policy-key (:policy-key %)))
           last))

(defn conditioning-step
  "One conditioning step for the enacted click, or a typed absence/refusal.

  INPUTS: :run-record (the click's, or nil), :target, :flight-id, :click-id,
  :observation (the entry's v2 :observation), :policy-key (the :increment
  receipt's), :precedence (the chosen candidate's pattern ids), :enactments
  (the flight's earlier entries, for the chain).

  The step is SPEC-F s1's: o = the observation's :o over V = its :checked;
  A = the decision's measured rates, target-local, restricted to V; B = the
  candidate's patterns from the run record's :domain-inputs; sPrev = the
  chain's own q for this policy, else the decision's initial belief
  marginalised to the target (step 1 ONLY; a contradictory or unavailable
  chain posterior is a typed absence, never an initial-belief restart);
  q = cascade-model-manifest
  /exact-update over token-likelihood with rates AND state intersected with V
  (C5's restriction: token-likelihood refuses any state token without a rate);
  f = -ln P(o) at that posterior, and a P(o) = 0 is :f :contradiction, never
  a number. A checked token whose class has no measured cell refuses the step
  :unmeasured-class (SPEC-F: a step needs measured A). Any other missing input
  is {:status :absent :reason <the first>}; no value stands in."
  [{:keys [run-record target flight-id click-id observation policy-key precedence enactments]}]
  (let [ma (get-in run-record [:decision :measured-a])
        rates-q (:rates ma)
        measurement-q (:measurement ma)
        previous (prior-step enactments policy-key)
        V (set (:checked observation))
        o (set (:o observation))
        interps (some #(when (= target (:target %)) (get-in % [:declaration :interpretations]))
                      (get-in run-record [:decision :selection-certificate :token-belief-stage :domain-inputs]))
        ;; a refusal or absence names the step's policy and occurrence when
        ;; known, so admission can end that policy's prefix at it (F1b-admit-I)
        ident (cond-> {:occurrence {:flight flight-id :click click-id}} policy-key (assoc :policy-key policy-key))
        absent (fn [reason & [inputs]] (merge (cond-> {:status :absent :reason reason} inputs (assoc :inputs inputs)) ident))]
    (cond
      (nil? run-record) (absent :no-run-record)
      (not= :observed (:status observation)) (absent :nothing-observed {:observation-status (:status observation)})
      (nil? policy-key) (absent :no-policy-key)
      (or (= :contradiction (:f previous))
          (contains? (:q previous) :status)
          (= :chain-contradiction (:reason previous)))
      (absent :chain-contradiction {:previous (:occurrence previous)})
      (and previous (nil? (:q previous)))
      (absent :chain-prior-unavailable {:previous (:occurrence previous)})
      (nil? ma) (absent :no-measured-a)
      (= :absent (:status ma)) (absent :measured-a-absent {:measured-a ma})
      (nil? rates-q) (absent :no-rates-value {:rates-sha (:rates-sha ma)})
      (nil? measurement-q) (absent :no-measurement-provenance {:rates-sha (:rates-sha ma)})
      (empty? precedence) (absent :no-precedence)
      (nil? interps) (absent :no-interpretations {:target target})
      :else
      (let [rates (target-local target rates-q)
            measurement (target-local target measurement-q)
            unmeasured (vec (sort-by str (filter #(or (not (contains? rates %))
                                                      (= :absent (get measurement %))
                                                      (not (contains? measurement %)))
                                                 V)))]
        (if (seq unmeasured)
          {:status :refused :reason :unmeasured-class
           :policy-key policy-key :occurrence {:flight flight-id :click click-id}
           :tokens unmeasured
           :classes (into {} (for [t unmeasured] [t (get-in observation [:channel t])]))}
          (let [pats (mapv (fn [id]
                             (policy/declared->interpreted
                              id (when-let [p (get interps id)] (assoc p :id id)))) precedence)
                missing (filterv #(= :missing (:status %)) pats)]
            (if (seq missing)
              (absent :no-interpretation {:patterns (mapv :pattern missing)
                                         :refusals missing})
              (let [chain-q (:q previous)
                    s-prev (if previous chain-q (target-marginal target (get-in run-record [:decision :initial-belief-receipt :value])))
                    rates-v (select-keys rates V)
                    lik (fn [st obs] (manifest/token-likelihood rates-v (set/intersection st V) obs))
                    pushed (manifest/rollout (constantly pats) s-prev 1)]
                (cond
                  (empty? s-prev) (absent :no-initial-belief)
                  (and (map? pushed) (contains? pushed :status)) (absent :transition-refused {:refusal pushed})
                  :else
                  (let [p-o (reduce + 0 (for [[st mass] pushed] (* mass (lik st o))))
                        q (manifest/exact-update lik pushed o)]
                    {:schema :wm/conditioning-step-v1
                     :status :present
                     :policy-key policy-key
                     :occurrence {:flight flight-id :click click-id}
                     :target target
                     :o {:o o :checked V}
                     :measured-a {:rates-sha (:rates-sha ma) :rates rates-v :classes (:classes ma)}
                     :b {:precedence (vec precedence)
                         :digest (ievidence/sha256 (.getBytes (pr-str [(vec precedence) (select-keys interps precedence)]) "UTF-8"))}
                     :s-prev {:value s-prev :source (if chain-q :chain :initial-belief)}
                     :q q
                     :p-o p-o
                     :f (if (zero? p-o) :contradiction (- (Math/log (double p-o))))}))))))))))

(defn record-click
  "FLIGHT after one click. CLICK is
    {:click-id … :wants [..] :want-source {..}
     :before {token bool} :after {token bool}     the target's facts
     :unreached-wants [{:token :reason}]            chosen candidate's receipt
     :abstention {:kind :missing …} | nil}          the judge's decline, if any
  Records the wants advanced; carries unreached wants and the abstention's
  missing input forward; closes when every want holds after the click; ends
  :no-progress when nothing advanced."
  [flight {:keys [click-id wants want-source before after unreached-wants abstention] :as click}]
  (let [moved (advanced wants before after)
        open (vec (remove #(true? (get after %)) wants))
        status (cond (empty? open) :closed
                     (empty? moved) :no-progress
                     :else :open)]
    (-> flight
        (update :clicks conj
                (cond-> {:click-id click-id
                         :n (inc (count (:clicks flight)))
                         :wants wants
                         :want-source want-source
                         :advanced moved
                         :open-after open
                         :unreached-wants (vec unreached-wants)
                         :progress? (boolean (seq moved))}
                  abstention (assoc :abstention (select-keys abstention [:kind :missing :declines :status :detail]))
                  ;; WM-CAST-I: the click's selection and close kind
                  ;; (record-summary) and the cast it was sent with
                  ;; (http-click-fn), as the click function gave them
                  (:chosen click) (assoc :chosen (:chosen click))
                  (contains? click :outcome) (assoc :outcome (:outcome click))
                  ;; WM-CLICK-REASON-I: why the click closed (record-summary)
                  (contains? click :failure) (assoc :failure (:failure click))
                  (contains? click :cast) (assoc :cast (:cast click))
                  ;; RUNNER-DRIFT-I: the serving JVM's displaced namespaces
                  ;; when the click ran (record-summary)
                  (contains? click :displacement) (assoc :displacement (:displacement click))))
        (update :carried-wants #(vec (distinct (concat % (map :token unreached-wants)))))
        (update :needs #(cond-> % (:missing abstention)
                          (conj (merge {:click-id click-id :kind (:kind abstention)
                                        :missing (:missing abstention)}
                                       (select-keys abstention [:status :detail])))))
        (assoc :status status)
        ;; what a closure covered: the criteria in view, and what was not
        (cond-> (= :closed status)
          (assoc :closure-scope {:criteria-in-view (count wants)
                                 :want-source (:kind want-source)
                                 :out-of-view (vec (:out-of-view want-source))})))))

;; ---------------------------------------------------------------------------
;; The loop

(defn throwable-summary
  "A Throwable as data for a record: :class, :message, the ex-data's :kind
  as :ex-kind when present, and :cause, the chain beneath it as
  {:class :message}, at most 5. When the chain goes deeper than five,
  :cause-cut-at 5 says so (WM-CAUSE-ON-RECORD-I), so a cut chain is not
  read as the whole of it."
  [e]
  (let [chain (take 6 (take-while some? (iterate ex-cause (ex-cause e))))]
    (cond-> {:class (.getName (class e)) :message (ex-message e)}
      (:kind (ex-data e)) (assoc :ex-kind (:kind (ex-data e)))
      (seq chain) (assoc :cause (vec (for [c (take 5 chain)]
                                       {:class (.getName (class c)) :message (ex-message c)})))
      (< 5 (count chain)) (assoc :cause-cut-at 5))))

(defn aborted-flight
  "The flight record a run! abort carries (:status :aborted), or nil."
  [e]
  (some-> e ex-data ::aborted))

(defn run!
  "Run FLIGHT to closure. CLICK-FN takes the judge-opts and returns
  {:click-id … :unreached-wants … :abstention …}; OBSERVE-FN takes the
  target and the wants' locators {token locator} (the tick's sources'
  locators, overlaid with the want source's own) and returns the facts
  {token bool}. ASK-FN, when given, runs before each click with the flight,
  its wants and the sources, and returns {:asked [...] :needs [...]}: the
  D11 requests made for wants no interpretation produces (futon2.aif.
  flight-runner/ask-fn); its needs join the flight's. READ-FN, when given,
  runs first, before the wants are read (flight-runner/read-fn, D11 part 5). SOURCES-FN returns the tick's
  sources (for the want source). Stops when the flight closes, when a click
  advances nothing (:no-progress, or :awaiting-answer with :pending when an
  ask's answer was still in flight at the click), after MAX-CLICKS (then :status :click-limit, with the
  open wants on the last click), or before any click when owner questions
  leave it no wants (:status :not-a-target-yet, :open-questions). Returns the
  flight record."
  [flight {:keys [click-fn observe-fn sources-fn max-clicks ask-fn read-fn enact-fn wc-fn fetch-run-record]}]
  ;; WM-SPIKE-FIX-III: a Throwable out of any step still leaves a record.
  ;; P holds the flight as recorded so far and the step running; the catch
  ;; throws an ex-info carrying that flight with :status :aborted, which the
  ;; driver writes before exiting non-zero. The third flight
  ;; (flight-74325007) lost its read step's record this way.
  (let [p (volatile! {:f flight :step nil})
        at (fn [step thunk] (vswap! p assoc :step step) (thunk))
        keep! (fn [f] (vswap! p assoc :f f) f)]
   (try
  (loop [f flight]
    (if (or (not= :open (:status f)) (>= (count (:clicks f)) max-clicks))
      (cond-> f (= :open (:status f)) (assoc :status :click-limit))
      (let [_ (keep! f)
            sources (at :sources sources-fn)
            ;; D11 part 5: readings the want source still needs (criteria a
            ;; mission does not state in a recognised form; locators for
            ;; criteria with no stated verdict) are asked before the wants
            ;; are read, so this click sees what they publish
            read (when read-fn (at :read #(read-fn f sources)))
            f (keep! (cond-> f
                       read (-> (update :readings (fnil conj []) (assoc read :before-click (inc (count (:clicks f)))))
                                (update :needs into (:needs read)))))
            wants (at :wants #(click-wants f sources))
            questions (filterv #(= :owner-question (:kind %)) (:needs f))]
        (if (and (seq questions) (empty? (:wants wants)))
          ;; genuinely unclear: no click is spent; the flight ends with the
          ;; owner's questions on its record (Joe: good questions logged,
          ;; not a refusal and not bad work against a vague specification)
          (assoc f :status :not-a-target-yet :open-questions questions)
          (let [locators (select-keys (merge (get-in sources [:locators (:target f)]) (:locators wants))
                                      (:wants wants))
                before (at :observe #(observe-fn (:target f) locators))
                asked (when ask-fn (at :ask #(ask-fn f wants sources)))
                f (keep! (cond-> f
                           asked (-> (update :asks (fnil conj []) (assoc asked :before-click (inc (count (:clicks f)))))
                                     (update :needs into (:needs asked)))))
                result (at :click #(click-fn (judge-opts f wants)))
                ;; M-wm-wiring row 0: the enactment step, after the click and
                ;; before the after-observation, which should see its effect
                enacted (when enact-fn (at :enact #(enact-fn f result)))
                ;; step 11: the W_c verdict of that enactment, handed to the
                ;; habit fold's increment unchanged
                wc (when (and wc-fn (:enactment enacted)) (at :wc #(wc-fn f enacted)))
                f (cond-> f enacted (update :enactments (fnil conj [])
                                            (merge (assoc (if (:enactment enacted)
                                                            (assoc (select-keys enacted [:record-path :temporal-receipt])
                                                                   ;; row 10: copied from the enactment
                                                                   ;; record, whose writer is
                                                                   ;; observe-publication-fn
                                                                   :publication-observed
                                                                   (get-in enacted [:enactment :publication-observed]))
                                                            {:enactment (select-keys enacted [:absent])})
                                                          :click-id (:click-id result))
                                                   wc)))
                _ (keep! f)
                after (at :observe #(observe-fn (:target f) locators))
                ;; F1a-1: the observation that followed the enacted step, on
                ;; its :enactments entry (only when an enactment record exists)
                f (cond-> f
                    (:enactment enacted)
                    (update :enactments
                            ;; bound to the record's name, so the map's scoped
                            ;; write [:observation {:record :enactment-entry}]
                            ;; is attributed to it (the prover's receiver form)
                            (fn [es] (let [enactment-entry (peek es)]
                                       (conj (pop es)
                                             (assoc enactment-entry :observation
                                                    (step-observation
                                                     (concat (keys (:universe wants)) (:wants wants))
                                                     after
                                                     (get-in enacted [:enactment :attempts])
                                                     locators
                                                     (:click-id result))))))))
                ;; F1b-join-I: the conditioning step, from the entry and the
                ;; click's run record (fetched by :click-id when a fetcher is
                ;; given; without one the step is the typed absence)
                f (cond-> f
                    (:enactment enacted)
                    (update :enactments
                            (fn [es] (let [enactment-entry (peek es)]
                                       (conj (pop es)
                                             (assoc enactment-entry :step
                                                    (conditioning-step
                                                     {:run-record (when fetch-run-record (fetch-run-record (:click-id result)))
                                                      :target (:target f) :flight-id (:flight/id f)
                                                      :click-id (:click-id result)
                                                      :observation (:observation enactment-entry)
                                                      :policy-key (get-in enactment-entry [:increment :policy-key])
                                                      :precedence (get-in result [:chosen :precedence])
                                                      :enactments (pop es)})))))))
                f (record-click f (merge result {:wants (:wants wants)
                                                 :want-source (:source wants)
                                                 :before before
                                                 :after after}))
                pending (filterv #(= :pending (:kind %)) (:needs asked))]
            ;; a closure over the clear criteria names the questions left open;
            ;; a click that advanced nothing while an ask was still pending
            ;; ends :awaiting-answer with those jobs, not :no-progress: the
            ;; answer never reached the click (M-wm-wiring row 3)
            (recur (cond-> f
                     (and (= :closed (:status f)) (seq questions))
                     (assoc-in [:closure-scope :open-questions] questions)
                     (and (= :no-progress (:status f)) (seq pending))
                     (assoc :status :awaiting-answer :pending pending))))))))
   (catch Throwable e
     (let [{:keys [f step]} @p]
       (throw (ex-info (str "flight aborted in step " (some-> step name) ": " (ex-message e))
                       {::aborted (assoc f :status :aborted
                                         :aborted (merge {:step step} (throwable-summary e)))}
                       e)))))))
