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
               See `requisition`. An
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
            [futon2.aif.cascade-sources :as cs]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.interpretation-construction :as ic]
            [futon2.aif.mission-criteria :as mc]
            [futon2.aif.mission-registry :as mr]
            [futon2.aif.want-interpretation :as wi]
            [futon2.report.war-machine :as wm]))

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
  (merge {:target (:target t) :kind (:kind t) :next-step next-step} detail))

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
  {"in-progress" :in-progress "completed" :completed})

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
  (let [lines (str/split-lines (str text))
        after-h1 (next (drop-while #(not (re-matches #"^#\s+\S.*$" %)) lines))
        line (first (drop-while str/blank? after-h1))]
    (if-not (and line (re-find #"^\*\*Requisition:\*\*" line))
      {:absent :no-requisition}
      (let [[_ word rest] (re-matches #"^\*\*Requisition:\*\*\s+(\S+)\s*(.*)$" (str line))
            state (requisition-states word)
            text (str/trim (str/replace (str rest) #"^[—-]\s*" ""))]
        (if-not state
          {:malformed line}
          (cond-> {:state state} (seq text) (assoc :text text)))))))

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

(defn assess
  "One considered target T: an exclusion when T is not a work target (an M-
  file without the lifecycle form, a file not at HEAD), else a feasible entry
  carrying the machine's :next-step for it.
  OPTS: :store :sources :code-root, and for tests :read-text / :observe."
  [{:keys [store sources code-root read-text observe]} t]
  (let [read (or read-text (fn [root repo path] (mc/read-mission root repo path)))
        text (when (:repo t) (read code-root (:repo t) (:path t)))
        shape (when (and text (= :mission (:kind t)))
                (lifecycle-shape (:target t) text (:status-line t)))
        req (requisition text)
        entry
        (cond
          (nil? text)
          (exclusion t :text-unreadable {:file-at-head (str (:repo t) "/" (:path t))})

          (and shape (not (:lifecycle-shaped? shape)))
          (exclusion t :not-lifecycle-shaped {:lifecycle-parts-missing (:missing shape)
                                              :form "futon4/holes/mission-lifecycle.md"}
                     {:shape shape})

          :else
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
                    (let [r (ic/support {:target target :want wants :observation universe
                                         :interpretations patterns
                                         :interpretation-receipts (get-in view [:interpretations target :receipts])
                                         :budget (:value (wm/construction-budget sources))
                                         :horizon (:value (wm/resolve-cascade-horizon view [target]))
                                         :move-cost (:value wm/construction-move-cost)})]
                      (if (= :supported (:status r))
                        (step t :ready {:support (count (:family r)) :open-wants open})
                        (constructor-step t r wants universe (:criteria-by-token src))))))))
            (catch Exception e
              (step t :construct {:finding {:kind :assembly-refused
                                            :refusal (or (ex-data e) {:message (.getMessage e)})}}))))]
    (if (:reason entry) entry (with-eligibility entry req (:kind t)))))

(defn target-field
  "`{:considered [...] :feasible [...] :exclusions [...]}` over LOADED."
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
     :feasible (vec (remove :reason assessed))
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

(defn -main
  "Read the field over the live checkouts and print it as EDN
  ({:decision {:target-field …}} with the read's provenance). Writes nothing."
  [& _]
  (let [code-root mr/default-code-root
        loaded {:missions (:missions (mr/load-missions-from-files code-root))
                :tickets (:tickets (mr/load-tickets code-root))
                :excursions (:excursions (mr/load-excursions code-root))}
        opts {:code-root code-root :store wi/default-store
              :sources (cs/with-context-fn (cs/load-declared cs/default-dir))}
        field (target-field opts loaded)
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
