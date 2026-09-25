(ns futon2.aif.target-field
  "PROOF-2a Clause T, step 1: the target field `[:decision :target-field]`,
  read from the substrate as it is. No target is chosen and nothing is
  scored.

  :considered  every work target the enumerators propose: the pre-H5b
               mission and ticket enumerators (mission-registry, still in
               src, not called by the judge since 5d55e7a0) and an excursion
               enumerator on the same shape, over M-, T- and E- objects.
  :feasible    targets in the constructor's support: its support step
               (interpretation-construction/support, the constructor before
               any G) returns at least one plan over the target's published
               interpretations. Feasibility is support, not a G comparison
               (E-outer-loop O3).
  :exclusions  every other considered target, with a typed :reason and
               :what-would-make-feasible.

  Exclusion reasons, in the order they are tested:
    :not-lifecycle-shaped   an M- object without the mission-lifecycle form
                            (futon4/holes/mission-lifecycle.md): a Status
                            line, `**Exit criterion:**` paragraphs under
                            lifecycle phase headings, verdict lines. The
                            test is mission-criteria's reader; E- and T-
                            objects are not missions and are not tested
                            against it (no form is defined for them).
    :text-unreadable        the file is not at HEAD of its repository.
    :needs-reading          no criteria in a recognised form: the flight's
                            read step would ask a seat for them.
    :want-already-observed  every want reads true.
    :needs-interpretation   an open want no published interpretation
                            produces (no seat is called here).
    otherwise the constructor's own typed refusal (:observation-required,
    :no-supported-order, :search-budget-exhausted, ...).

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
  Status line is STATUS-LINE: shaped when it has a Status line, at least one
  `**Exit criterion:**` paragraph under a lifecycle phase heading, and at
  least one of those carrying a verdict line. :phase-headings counts `## `
  headings naming a lifecycle phase, recorded but not part of the test."
  [mission-id text status-line]
  (let [exits (filter #(and (= :phase-exit (:kind %)) (lifecycle-phases (phase-name (:phase %))))
                      (mc/criteria mission-id (str text)))
        headings (for [l (str/split-lines (str text))
                       :let [[_ h] (re-matches #"^##\s+(.*)$" l)]
                       :when (and h (lifecycle-phases (phase-name h)))]
                   (phase-name h))
        parts {:status-line (some? status-line)
               :phase-exits (count exits)
               :verdict-lines (count (filter :verdict exits))}
        missing (cond-> []
                  (not (:status-line parts)) (conj :status-line)
                  (zero? (:phase-exits parts)) (conj :phase-exits)
                  (zero? (:verdict-lines parts)) (conj :verdict-lines))]
    (assoc parts
           :phase-headings (count headings)
           :lifecycle-shaped? (empty? missing)
           :missing missing)))

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

(defn- open-wants [wants universe] (vec (remove #(true? (get universe %)) wants)))

(defn- named-wants
  "Each want with the criterion it stands for, so the record reads without
  the token hash."
  [tokens criteria-by-token]
  (vec (for [t tokens :let [c (get criteria-by-token t)]]
         (cond-> {:want t}
           c (assoc :line (:line c) :criterion (first (str/split-lines (str (:stated c)))))))))

(defn- unobserved [wants universe] (vec (remove #(boolean? (get universe %)) wants)))

(defn- constructor-exclusion [t r wants universe cbt]
  (let [open (set (open-wants wants universe))
        unproduced (vec (sort-by pr-str (distinct (for [f (:findings r)
                                                        :when (and (= :unproduced-need (:kind f)) (open (:token f)))]
                                                    (:token f)))))]
    (case (:kind r)
      :observation-required
      (exclusion t :observation-required {:observations-for (named-wants (:tokens r) cbt)
                                          :via "a checkable locator per token, observed at click time"})
      :no-supported-order
      (if (seq unproduced)
        (exclusion t :needs-interpretation {:interpretations-for (named-wants unproduced cbt)
                                            :via "an interpretation request per want (the flight's ask step)"}
                   {:constructor-finding :no-supported-order :findings (:findings r)
                    :unobserved (unobserved wants universe)})
        (exclusion t :no-supported-order {:constructor-findings (:findings r)}))
      (exclusion t (:kind r) {:constructor-refusal (dissoc r :candidates :status)}))))

(defn assess
  "Feasible entry or exclusion for one considered target T.
  OPTS: :store :sources :code-root, and for tests :read-text / :observe."
  [{:keys [store sources code-root read-text observe]} t]
  (let [read (or read-text (fn [root repo path] (mc/read-mission root repo path)))
        text (when (:repo t) (read code-root (:repo t) (:path t)))
        shape (when (and text (= :mission (:kind t)))
                (lifecycle-shape (:target t) text (:status-line t)))]
    (cond
      (nil? text)
      (exclusion t :text-unreadable {:file-at-head (str (:repo t) "/" (:path t))})

      (and shape (not (:lifecycle-shaped? shape)))
      (exclusion t :not-lifecycle-shaped {:lifecycle-parts-missing (:missing shape)
                                          :form "futon4/holes/mission-lifecycle.md"}
                 {:shape shape})

      :else
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
          (exclusion t :needs-reading
                     {:reading (if (get-in src [:readings-needed :criteria?]) :criteria :criteria-or-coverage)
                      :via "the flight's read step (a seat names the criteria, each cued to the text)"})
          :else
          (let [view (fr/target-view store f cw sources)
                target (:target t)
                universe (get-in view [:universes target])
                open (open-wants wants universe)
                patterns (get-in view [:interpretations target :patterns])]
            (cond
              (empty? open)
              (exclusion t :want-already-observed {:a-want-not-yet-observed :none-stated
                                                   :note "every stated want reads true at HEAD"})
              (empty? patterns)
              (exclusion t :needs-interpretation {:interpretations-for (named-wants open (:criteria-by-token src))
                                                  :via "an interpretation request per want (the flight's ask step)"}
                         {:unobserved (unobserved open universe)})
              :else
              (let [r (ic/support {:target target :want wants :observation universe
                                   :interpretations patterns
                                   :interpretation-receipts (get-in view [:interpretations target :receipts])
                                   :budget (:value (wm/construction-budget sources))
                                   :horizon (:value (wm/resolve-cascade-horizon view [target]))
                                   :move-cost (:value wm/construction-move-cost)})]
                (if (= :supported (:status r))
                  (cond-> {:target target :kind (:kind t)
                           :support (count (:family r))
                           :open-wants open}
                    shape (assoc :shape (select-keys shape [:phase-exits :verdict-lines])))
                  (constructor-exclusion t r wants universe (:criteria-by-token src)))))))))))

(defn target-field
  "`{:considered [...] :feasible [...] :exclusions [...]}` over LOADED."
  [opts loaded]
  (let [cs (considered (:code-root opts) loaded)
        assessed (for [t cs]
                   (try (assess opts t)
                        (catch Exception e
                          (exclusion t :assembly-refused
                                     {:refusal (or (ex-data e) {:message (.getMessage e)})
                                      :via "the refusal names what the assembly lacked"}))))]
    {:considered cs
     :feasible (vec (remove :reason assessed))
     :exclusions (vec (filter :reason assessed))}))

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
      (seq (remove #(seq (:what-would-make-feasible %)) exclusions))
      (conj {:failure :exclusion-without-what-would-make-feasible
             :targets (vec (map :target (remove #(seq (:what-would-make-feasible %)) exclusions)))}))))

(defn counts [{:keys [considered feasible exclusions]}]
  {:considered (count considered)
   :feasible (count feasible)
   :excluded (count exclusions)
   :considered-by-kind (frequencies (map :kind considered))
   :excluded-by-reason (into (sorted-map) (frequencies (map :reason exclusions)))})

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
                :check (check-field field)
                :read {:code-root code-root
                       :mission-source :file-scan
                       :store (str (io/file wi/default-store))
                       :heads (into (sorted-map) (for [r repos] [r (head-sha code-root r)]))}})
    (shutdown-agents)))
