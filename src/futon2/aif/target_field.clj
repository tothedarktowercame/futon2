(ns futon2.aif.target-field
  "PROOF-2a Clause T, step 1: the target field `[:decision :target-field]`,
  read from the substrate as it is. No target is chosen and nothing is
  scored.

  :considered  every work target the enumerators propose: the pre-H5b
               mission and ticket enumerators (mission-registry, still in
               src, not called by the judge since 5d55e7a0) and an excursion
               enumerator on the same shape, over M-, T- and E- objects.
  :feasible    every considered work target: a lifecycle-shaped mission, a
               ticket or an excursion, readable at HEAD. An owner wrote it
               down, so it can be done; each entry carries the machine's
               :next-step for it: :read-criteria, :ask-interpretation
               (naming the wants and criterion lines), :observe (naming the
               tokens), :construct (the constructor's finding as data) or
               :ready (the constructor's support step,
               interpretation-construction/support, before any G, has a
               plan).
               Each feasible entry also carries :requisition (the state read
               from the file, or a typed absence), :eligible, and
               :ineligible-reason when a requisition makes it ineligible.
               See `requisition`. A feasible entry also carries :delta-g
               (H-G-target part 2, HG2-Ia): a :ready entry's ΔG_t (value +
               universe, or a typed absence; see `ready-delta-g`), every
               other entry the typed absence {:absent
               :no-constructed-candidate :next-step <its next step>}. An
               ineligible entry stays on the record with its :next-step:
               the ruling makes it ineligible, it does not unwrite it.
  :exclusions  non-targets only, with :reason and :what-would-make-feasible:
    :not-lifecycle-shaped   an M- object without the mission-lifecycle form
                            (futon4/holes/mission-lifecycle.md, Conventions):
                            a Status line and at least one `## ` heading
                            naming a lifecycle phase. E- and T- objects are
                            not missions and are not tested against it (no
                            form is defined for them).
    :text-unreadable        the file is not at HEAD of its repository.
  Nothing else excludes: what the machine still has to do for a target is
  its :next-step, never a reason it cannot be done.

  Reads only: git show at HEAD, the published interpretation store, the
  declared cascade sources. Writes nothing."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.aif.task-requisition :as task-requisition]
            [futon2.aif.cascade-sources :as cs]
            [futon2.aif.construction :as construction]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.interpretation-construction :as ic]
            [futon2.aif.cascade-problems :as cascade-problems]
            [futon2.aif.mission-criteria :as mc]
            [futon2.aif.mission-registry :as mr]
            [futon2.aif.served-by-reading :as served]
            [futon2.aif.want-interpretation :as wi]
            [futon2.aif.wm.construction-inputs :as wm-inputs]))

(def lifecycle-phases
  "The phases of futon4/holes/mission-lifecycle.md, HEAD optional."
  #{"HEAD" "IDENTIFY" "MAP" "DERIVE" "ARGUE" "VERIFY" "INSTANTIATE" "DOCUMENT"})

(defn- phase-name [heading]
  (re-find #"^[A-Z]+" (str/triml (str heading))))

(defn lifecycle-shape
  "The lifecycle form of mission TEXT (id MISSION-ID) whose registry
  Status line is STATUS-LINE, as futon4/holes/mission-lifecycle.md's
  Conventions define it: a Status line at the top and phases as appended
  checkpoints, so shaped when there is a Status line and at least one `## `
  heading naming a lifecycle phase (`INSTANTIATE-7a` counts). :phase-exits
  (`**Exit criterion:**` paragraphs under a phase heading) and :verdict-lines
  are the criteria reader's forms, recorded as data and not tested: a shaped
  mission without them waits on the read step (:needs-reading)."
  [mission-id text status-line]
  (let [exits (filter #(and (= :phase-exit (:kind %)) (lifecycle-phases (phase-name (:phase %))))
                      (mc/criteria mission-id (str text)))
        headings (for [l (str/split-lines (str text))
                       :let [[_ h] (re-matches #"^##\s+(.*)$" l)]
                       :when (and h (lifecycle-phases (phase-name h)))]
                   (phase-name h))
        missing (cond-> []
                  (nil? status-line) (conj :status-line)
                  (empty? headings) (conj :phase-headings))]
    {:status-line (some? status-line)
     :phase-headings (count headings)
     :phase-exits (count exits)
     :verdict-lines (count (filter :verdict exits))
     :lifecycle-shaped? (empty? missing)
     :missing missing}))

;; ---------------------------------------------------------------------------
;; Enumeration

(defn- repo-and-path
  "REPO (directory under CODE-ROOT) and the repo-relative path of ABS."
  [code-root abs]
  (let [rel (str/replace-first (str abs) (str code-root "/") "")
        [repo & more] (str/split rel #"/")]
    (when (seq more) [repo (str/join "/" more)])))

(defn considered
  "Every target the three enumerators propose over LOADED
  {:missions :tickets :excursions}: missions as the pre-H5b loop gave them
  (the live ones, `open-missions`), tickets and excursions live."
  [code-root {:keys [missions tickets excursions]}]
  (let [proposals (concat
                   (.propose mr/mission-enumerator-proposer {:missions (mr/open-missions {:missions missions})})
                   (.propose mr/ticket-enumerator-proposer {:tickets tickets})
                   (.propose mr/excursion-enumerator-proposer {:excursions excursions}))
        by-id (into {} (map (juxt :id identity)) (concat missions tickets excursions))]
    (vec (for [{:keys [type target]} proposals
               :let [e (by-id target)
                     path (or (:path e) "")
                     [repo rel] (repo-and-path code-root path)]]
           {:target target
            :kind ({:advance-mission :mission :advance-ticket :ticket :advance-excursion :excursion} type)
            :proposed-by type
            :repo repo :path rel
            :status-line (:status-line e)
            :status-class (:status-class e)}))))

;; ---------------------------------------------------------------------------
;; Support

(defn- exclusion [t reason wwmf & [details]]
  (cond-> {:target (:target t) :kind (:kind t) :reason reason :what-would-make-feasible wwmf}
    details (assoc :details details)))

(defn- step
  "A feasible target with the machine's NEXT-STEP for it and what that step
  works on; FINDING is the constructor's or reader's typed finding, kept as
  data."
  [t next-step & [detail]]
  (merge {:target (:target t) :kind (:kind t) :next-step next-step}
         (select-keys t [:universe :universe-source]) detail))

(defn- read-universe
  "Read U(t) using the click's served-by reader on the already-read TEXT.
  Outcome ids are the reading's outcome tokens; include wants only from
  instances with a served-by link (vocabulary or a verified proposal).
  Qualify every token [target token], independently of the observation map.
  An explicit outcome collection, including [], is a successful reading;
  an absent/refused reading never becomes an empty universe. The source pins
  the text bytes by SHA-256, exactly as the click's reading does."
  [t text {:keys [served-by-cascades served-by-quotes]}]
  (try
    (let [r (served/reading (str (:repo t) "/" (:path t)) text
                            {:cascades (get served-by-cascades (:target t))
                             :quotes (get served-by-quotes (:target t))})
          out (:outcomes r)
          outcomes (:outcomes out)
          source {:reading (:text-sha256 r)}]
      (cond
        (:refused out) {:universe-source (merge source out)}
        (map? outcomes) {:universe-source (merge source outcomes)}
        (not (sequential? outcomes))
        {:universe-source (assoc source :absent :no-stated-outcome)}
        :else
        (let [proposed (set (keep #(when (get-in % [:result :via])
                                    (get-in % [:result :instance]))
                                 (when (sequential? (:proposals r)) (:proposals r))))
              links (when (sequential? (:served-by out)) (:served-by out))
              wants (mapcat :wants (filter #(or (seq (:serves %))
                                               (contains? proposed (:instance %))) links))
              tokens (concat (map :id outcomes) wants)]
          {:universe (set (map #(vector (:target t) %) tokens))
           :universe-source (assoc source :outcomes (count outcomes)
                                         :cues (reduce + 0 (map #(count (:cues %)) outcomes)))})))
    (catch Exception e
      {:universe-source {:reading (served/sha256 text)
                         :refused :universe-reading-failed
                         :detail (or (ex-data e) {:message (.getMessage e)})}})))

(defn- open-wants [wants universe] (vec (remove #(true? (get universe %)) wants)))

(defn- named-wants
  "Each want with the criterion it stands for, so the record reads without
  the token hash."
  [tokens criteria-by-token]
  (vec (for [t tokens :let [c (get criteria-by-token t)]]
         (cond-> {:want t}
           c (assoc :line (:line c) :criterion (first (str/split-lines (str (:stated c)))))))))

(defn- unobserved [wants universe] (vec (remove #(boolean? (get universe %)) wants)))

(defn- constructor-step [t r wants universe cbt]
  (let [open (set (open-wants wants universe))
        unproduced (vec (sort-by pr-str (distinct (for [f (:findings r)
                                                        :when (and (= :unproduced-need (:kind f)) (open (:token f)))]
                                                    (:token f)))))]
    (cond
      (= :observation-required (:kind r))
      (step t :observe {:observations-for (named-wants (:tokens r) cbt)
                        :finding (dissoc r :candidates :status)})
      (and (= :no-supported-order (:kind r)) (seq unproduced))
      (step t :ask-interpretation {:interpretations-for (named-wants unproduced cbt)
                                   :unobserved (unobserved wants universe)
                                   :finding (dissoc r :candidates :status)})
      :else
      (step t :construct {:finding (dissoc r :candidates :status)}))))

(def requisition-states
  "The state words kimi-task.sh writes. A requisition is made at dispatch
  time, so it flags the work in-progress and then completed."
  task-requisition/states)

(defn requisition
  "The requisition state declared by TEXT, as a typed fact:
  {:state :in-progress|:completed :text <rest of the line>},
  {:malformed <line>}, or {:absent :no-requisition}.

  The form kimi-task.sh writes is one line directly under the H1,
  `**Requisition:** in-progress — <purpose>`; the first non-blank line
  after the H1 is the only line read, so the same words in the body are
  prose and not a requisition. A line carrying the marker with no state
  word, or a word that is neither, is :malformed: it is reported, never
  guessed at.

  Joe's ruling of 2026-09-25 ~17:40Z, recorded at futon2 a461b124
  (PROOF-2a-THEOREM-draft-2026-09-24.md, \"Ruling: a requisition is a
  different semantic layer\"): a requisition is a different semantic layer
  from a pending E-/M-/T- job. It is made at dispatch time and flags the
  work in-progress, then completed, and a target in either state is
  therefore ineligible; a pending object with no requisition is eligible.
  This function reads the state only. What it makes of it is
  `with-eligibility`."
  [text]
  (task-requisition/read-state text))

(defn- with-eligibility
  "Record REQ on feasible entry E, and what the ruling (see `requisition`)
  makes of it. Only a requisition IN A STATE makes a target ineligible:
  an absent or malformed line leaves it eligible, so a file the machine
  cannot read a state from is never silently taken off the table. An
  ineligible entry keeps its :next-step, which is a fact about the target
  and not a plan to act on it.

  Creation is what makes a pending object eligible (Joe, 2026-09-25 ~18:05Z:
  \"when a new T- or E- or M- is created it becomes eligible\"; his earlier
  \"voted\" was \"mooted\", and it carries no tag). Nothing further is read
  or recorded for a pending entry."
  [e req _kind]
  (let [req (or req {:absent :text-unread})
        state (:state req)]
    (cond-> (assoc e :requisition req :eligible (nil? state))
      state (assoc :ineligible-reason (keyword "requisition" (name state))))))

(defn- ready-delta-g
  "ΔG_t for a :ready target (PROOF-2a H-G-target part 2, field side;
  HG2-Ia): run interpretation-construction/construct on the SAME input the
  support step just held, and compare the scored baseline G against the
  receipt's :g-of-best with construction/delta-g — the same compare-g the
  constructor records, never a copy. One of:
    {:value Δ :universe U :receipt-digest … :baseline-g … :g-of-best …}
      a constructed candidate improved on the empty baseline over one shared
      recorded universe (Δ = baseline − best, compare-g's sign); :universe
      is the receipt's own, :receipt-digest the SHA-256 of the construction
      receipt's printed form, so the record replays against it.
    {:absent :incommensurable :universes [ua ub]}
      the comparison has no shared recorded universe — the constructor
      stopped :g-universes-incommensurable (or, defensively, the final
      delta-g did); never a number.
    {:absent :no-constructed-candidate :reason … :next-step :ready}
      construction refused or took no move; support still held, so the
      entry's next step stays :ready.
    {:absent :no-evaluator-supplied} for a view without construction.
    {:absent :problem-not-assembled :reason kind} for assembly refusal.
  The real target-view supplies the click's evaluator; base-problem is the
  same assembly authority selection uses, without candidate precedences."
  [input view]
  (if-let [evaluate-g (get-in view [:construction :evaluate-g])]
    (let [problem (cascade-problems/base-problem view (:horizon input) (:target input))]
      (if (:kind problem)
        {:absent :problem-not-assembled :reason (:kind problem)}
        (let [result (ic/construct (assoc input :evaluate-g (fn [c] (evaluate-g problem c))))]
          (if (= :constructed (:status result))
            (let [receipt (get-in result [:candidates 0 :construction-receipt])
                  cmp (construction/delta-g (:baseline-g result) (:g-of-best receipt))]
              (if-let [inc (:incommensurable cmp)]
                {:absent :incommensurable :universes (:universes inc)}
                {:value (:delta cmp)
                 :universe (:universe cmp)
                 :receipt-digest (served/sha256 (pr-str receipt))
                 :baseline-g (:baseline-g result)
                 :g-of-best (:g-of-best receipt)}))
            (let [receipt (:construction-receipt result)]
              (if (= :g-universes-incommensurable (:stop-reason receipt))
                {:absent :incommensurable
                 :universes (get-in receipt [:coverage :final-evaluation
                                             :compose-by-need :incommensurable :universes])}
                {:absent :no-constructed-candidate
                 :reason (:kind result)
                 :next-step :ready}))))))
    {:absent :no-evaluator-supplied}))

(defn assess
  "One considered target T: an exclusion when T is not a work target (an M-
  file without the lifecycle form, a file not at HEAD), else a feasible entry
  carrying the machine's :next-step for it.
  OPTS: :store :sources :code-root, and for tests :read-text / :observe."
  [{:keys [store sources code-root read-text observe] :as opts} t]
  (let [read (or read-text (fn [root repo path] (mc/read-mission root repo path)))
        text (when (:repo t) (read code-root (:repo t) (:path t)))
        shape (when (and text (= :mission (:kind t)))
                (lifecycle-shape (:target t) text (:status-line t)))
        req (requisition text)
        t (if (and text (or (nil? shape) (:lifecycle-shaped? shape)))
            (merge t (read-universe t text opts)) t)
        entry
        (cond
          (nil? text)
          (exclusion t :text-unreadable {:file-at-head (str (:repo t) "/" (:path t))})

          (and shape (not (:lifecycle-shaped? shape)))
          (exclusion t :not-lifecycle-shaped {:lifecycle-parts-missing (:missing shape)
                                              :form "futon4/holes/mission-lifecycle.md"}
                     {:shape shape})

          :else
          (let [entry
                (try
            (let [f (flight/start {:target (:target t) :chosen-because {:kind :target-field}}
                                  (cond-> {:kind :a-exits :repo (:repo t) :path (:path t) :store store
                                           :code-root code-root :read-text (fn [& _] text)}
                                    observe (assoc :observe observe))
                                  {:id (str "target-field-" (:target t))})
                  cw (flight/click-wants f sources)
                  src (:source cw)
                  wants (:wants cw)]
              (cond
                (empty? wants)
                (step t :read-criteria
                      {:finding {:kind (if (get-in src [:readings-needed :criteria?]) :criteria-not-stated :no-wants)}})
                :else
                (let [view (fr/target-view store f cw sources)
                      target (:target t)
                      universe (get-in view [:universes target])
                      open (open-wants wants universe)
                      patterns (get-in view [:interpretations target :patterns])]
                  (cond
                    (empty? open)
                    ;; the owner lists it live and every stated want reads true:
                    ;; what remains is reading what the text still asks for
                    (step t :read-criteria {:finding {:kind :want-already-observed :wants wants}})
                    (empty? patterns)
                    (step t :ask-interpretation {:interpretations-for (named-wants open (:criteria-by-token src))
                                                 :unobserved (unobserved open universe)
                                                 :finding {:kind :no-published-interpretation}})
                    :else
                    (let [input {:target target :want wants :observation universe
                                 :interpretations patterns
                                 :interpretation-receipts (get-in view [:interpretations target :receipts])
                                 :budget (:value (wm-inputs/construction-budget sources))
                                 :horizon (:value (wm-inputs/resolve-cascade-horizon view [target]))
                                 :move-cost (:value wm-inputs/construction-move-cost)}
                          r (ic/support input)]
                      (if (= :supported (:status r))
                        (step t :ready {:support (count (:family r)) :open-wants open
                                        :delta-g (ready-delta-g input view)})
                        (constructor-step t r wants universe (:criteria-by-token src))))))))
            (catch Exception e
              (step t :construct {:finding {:kind :assembly-refused
                                            :refusal (or (ex-data e) {:message (.getMessage e)})}})))]
            ;; HG2-Ia: ΔG is defined only for :ready targets (the direction's
            ;; "construct first but not for all 343"); every other feasible
            ;; entry carries the typed absence with its own next step, never
            ;; a number standing in.
            (if (= :ready (:next-step entry))
              entry
              (assoc entry :delta-g {:absent :no-constructed-candidate
                                     :next-step (:next-step entry)}))))]
    (if (:reason entry) entry (with-eligibility entry req (:kind t)))))

(defn with-pair-overlap
  "Each feasible entry of FEASIBLE with :pair-overlap (M-wm-wiring row 8;
  PROOF-2a H-G-target part 1). Target-grain ΔG_t is taken over t's own
  universe, and two targets' ΔG compare only when t's constructed candidate
  moves no token of the other's universe (target_comparison, mathlib4
  759b8ca884). For an entry carrying a constructed candidate
  (:constructed-candidate {:produces #{...}}), per other feasible target t':
    {:comparable true}                              no shared token
    {:incommensurable {:shared-tokens [...]}}       the tokens it moves in U(t')
    {:absent :no-universe}                          t' records no :universe
  An entry with no constructed candidate records {:absent
  :no-constructed-candidate}, never an empty map (which would read as no
  overlap). No target has a constructed candidate at HEAD."
  [feasible]
  (let [universe-of (into {} (keep (fn [e] (when (:universe e) [(:target e) (set (:universe e))])))
                          feasible)]
    (mapv (fn [e]
            (assoc e :pair-overlap
                   (if-let [produces (seq (get-in e [:constructed-candidate :produces]))]
                     (into (sorted-map)
                           (for [o feasible :when (not= (:target o) (:target e))]
                             [(:target o)
                              (if-let [u (universe-of (:target o))]
                                (let [shared (vec (sort-by pr-str (filter u produces)))]
                                  (if (seq shared)
                                    {:incommensurable {:shared-tokens shared}}
                                    {:comparable true}))
                                {:absent :no-universe})]))
                     {:absent :no-constructed-candidate})))
          feasible)))

(defn target-field
  "`{:considered [...] :feasible [...] :exclusions [...]}` over LOADED; each
  feasible entry carries :pair-overlap (with-pair-overlap)."
  [opts loaded]
  (let [cs (considered (:code-root opts) loaded)
        ;; `assess` catches its own assembly refusals, with the target's
        ;; requisition already read; this catches a read that throws, where
        ;; no line could be looked at. Eligibility needs a requisition IN A
        ;; STATE, so the entry stays eligible and says the text went unread.
        assessed (for [t cs]
                   (try (assess opts t)
                        (catch Exception e
                          (with-eligibility
                            (step t :construct {:finding {:kind :assembly-refused
                                                          :refusal (or (ex-data e) {:message (.getMessage e)})}})
                            nil (:kind t)))))]
    {:considered cs
     :feasible (with-pair-overlap (vec (remove :reason assessed)))
     :exclusions (vec (filter :reason assessed))}))

(def next-steps
  "What the machine does next for a feasible target."
  #{:read-criteria :ask-interpretation :observe :construct :ready})

(defn check-field
  "Wₜ's partition half for a step-1 field (no :chosen): every considered
  target is feasible or excluded, exactly once, nothing else is listed, and
  every exclusion states a reason and what would make it feasible. Returns
  the failures, empty when the field holds."
  [{:keys [considered feasible exclusions] :as field}]
  (let [cons (map :target considered)
        listed (concat (map :target feasible) (map :target exclusions))
        freq (frequencies listed)]
    (cond-> []
      (contains? field :chosen) (conj {:failure :chosen-in-step-1})
      (seq (remove (set listed) cons)) (conj {:failure :considered-not-partitioned
                                              :targets (vec (remove (set listed) cons))})
      (seq (remove (set cons) listed)) (conj {:failure :listed-not-considered
                                              :targets (vec (remove (set cons) listed))})
      (seq (filter #(> (val %) 1) freq)) (conj {:failure :listed-twice
                                                :targets (vec (keys (filter #(> (val %) 1) freq)))})
      (seq (remove #(keyword? (:reason %)) exclusions)) (conj {:failure :exclusion-without-reason})
      (seq (remove #(next-steps (:next-step %)) feasible))
      (conj {:failure :feasible-without-next-step
             :targets (vec (map :target (remove #(next-steps (:next-step %)) feasible)))})
      (seq (remove #(seq (:what-would-make-feasible %)) exclusions))
      (conj {:failure :exclusion-without-what-would-make-feasible
             :targets (vec (map :target (remove #(seq (:what-would-make-feasible %)) exclusions)))}))))

(defn counts [{:keys [considered feasible exclusions]}]
  {:considered (count considered)
   :feasible (count feasible)
   :excluded (count exclusions)
   :considered-by-kind (frequencies (map :kind considered))
   :excluded-by-reason (into (sorted-map) (frequencies (map :reason exclusions)))})

(defn next-step-counts [{:keys [feasible]}]
  (into (sorted-map) (frequencies (map :next-step feasible))))

(defn- head-sha [code-root repo]
  (str/trim (:out (sh/sh "git" "-C" (str code-root "/" repo) "rev-parse" "HEAD"))))

(defn load-field
  "The target field over the live checkouts, as `-main` reads it: {:field the
  `target-field` map, :opts {:code-root :store :sources}}. Reads only. Optional
  OVERRIDES replace :code-root, :store or :sources (tests and callers with a
  store of their own)."
  [& [{:keys [code-root store sources served-by-cascades served-by-quotes]}]]
  (let [code-root (or code-root mr/default-code-root)
        loaded {:missions (:missions (mr/load-missions-from-files code-root))
                :tickets (:tickets (mr/load-tickets code-root))
                :excursions (:excursions (mr/load-excursions code-root))}
        opts {:code-root code-root :store (or store wi/default-store)
              :served-by-cascades served-by-cascades :served-by-quotes served-by-quotes
              :sources (or sources (cs/with-context-fn (cs/load-declared cs/default-dir)))}]
    {:field (target-field opts loaded) :opts opts}))

(defn -main
  "Read the field over the live checkouts and print it as EDN
  ({:decision {:target-field …}} with the read's provenance). Writes nothing."
  [& _]
  (let [{:keys [field opts]} (load-field)
        code-root (:code-root opts)
        repos (sort (distinct (keep :repo (:considered field))))]
    (pp/pprint {:decision {:target-field field}
                :counts (counts field)
                :next-steps (next-step-counts field)
                :check (check-field field)
                :read {:code-root code-root
                       :mission-source :file-scan
                       :store (str (io/file wi/default-store))
                       :heads (into (sorted-map) (for [r repos] [r (head-sha code-root r)]))}})
    (shutdown-agents)))
