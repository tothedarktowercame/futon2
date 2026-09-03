(ns futon2.aif.survey-mission-value
  "The epistemic payload of a `:survey-mission` candidate: what surveying a
   mission would tell the judge, in nats.

   WHY THIS EXISTS. `:survey-mission` has been a declared action type since the
   portfolio proposer landed (`futon2.aif.portfolio-action-proposer:50-60`), but
   nothing about it was epistemic: its `predict-effects` arm delegates to
   `:survey` (`forward_model.clj:180-187`), which predicts an EMPTY obs-delta and
   one variance channel, and its candidate carried a flat `:weight 0.3`. So the
   only per-target signal a survey candidate could ever have was none: the
   forward model ignores `:target`, and two survey candidates on two different
   missions score IDENTICALLY at G-core. On the 2026-09-02 field all 133 of them
   do, at -116.79209997800035 ambiguity and 5.454879482482923 G-core.

   That target-invariance is the reason this namespace exists rather than a
   nicety about it: EVERY per-target difference between two survey candidates
   has to come from the epistemic leg, because G-core supplies none. `G is
   dominated by information gain` is not a hope about the weighting here, it is
   an identity about where the variation lives.

   Joe's 2026-09-02 rulings that this discharges are 3b (the epistemic-action
   hole) and 3c (the catalog design) in
   `futon4/holes/mission-lifecycle-wm-alignment.md`; the row is wm-contract
   :U24, ported from zaif-harness :S6 half (b).

   THE FORM.

     survey-eig(M) = survey-availability(phase(M)) * SUM_q EIG(q)     [nats]

     G(survey-mission M) = G-core(survey) - survey-eig-weight * survey-eig(M)

   NATS ARE NOT RESCALED HERE, and that is a deliberate difference from
   `futon2.aif.mission-epistemic-value`, whose term is normalized by a constant
   reference into [0, 1]. There the term had to blend additively with three
   dimensionless exploit factors, so it had to be dimensionless too. Here the
   leg enters G, and G-risk and G-ambiguity are already in nats -- so the
   epistemic quantity is subtracted in its own units and the exchange rate is
   the ONE declared scalar `survey-eig-weight`. `:normalized` is still reported
   beside the nats, against `mission-epistemic-value/reference-open-questions`,
   so the two rows' numbers are comparable; nothing reads it.

   `survey-availability` is `mission-epistemic-value/phase-survey-availability`
   -- REUSED, not re-declared. It is the right gate for a survey action for the
   same reason it was the right gate for the mission-value term: it says whether
   the phase produces field readings at all, so a VERIFY-phase mission's survey
   earns 0.0 nats by the table rather than by a special case.

   THE QUESTIONS, two kinds, both typed.

   (a) THE MISSION'S UNANSWERED MAP QUESTIONS. The carrier is the numbered list
   under a standalone `MAP must answer:` line in the mission doc -- named as the
   obvious next question kind by C492 section 4c, which pointed at
   `futon0/holes/missions/M-apm-capability-ratchet.md:260-274`. Each listed
   question is one binary latent the judge does not hold.

   MEASURED, AND THE MEASUREMENT IS MOSTLY AN ABSENCE: over the 238 primary
   mission docs on 2026-09-03, exactly ONE carries this list (that one, six
   questions). Four other docs contain the string `must answer` in a different
   construction -- `Questions the evidence bundle must answer:`
   (futon3c M-agency-hardening:196), `Questions the audit must answer:`
   (futon5a M-stack-stereolithography:1323), and M-apm-demonstration's prose
   and table uses (futon3c M-apm-demonstration:15, :278) -- and NONE of them is
   a MAP-phase question list. They are recorded as `near-miss-phrasings` and
   deliberately NOT matched. Widening the regex to collect them is the exact
   move that let 266 unresolvable cross-references buy survey priority in U22's
   first run (`mission_epistemic_value.clj:47-57`); a carrier that exists once
   is a thin carrier, and saying so is the finding.

   NO PER-QUESTION ANSWER CARRIER EXISTS, so every listed question is open.
   M-apm-capability-ratchet's own `MAP findings` sections (`:276` onward) are
   numbered `Finding 1..n` against no question id, so nothing joins a finding to
   the question it answers. The questions are therefore all held at p = 0.5 with
   basis `{:reading :absent :reason :no-per-question-answer-carrier}` -- an
   absence with a pointer, not an assumption that they are unanswered.

   (b) THE KIN-CATALOG GAP, from U23's reader
   (`holes/labs/wm-contract/u23_cascade_catalog_reader.clj`, artifact
   `runs/U23-cascade-catalog/carrier-population.edn`). U23 declared an
   eleven-carrier cascade record and read all eleven for three subjects. Six of
   the eleven cannot be answered for ANY mission on this field, and U23's own
   absence reasons say why; those six are excluded from the question set and
   counted, on the same principle as (a). The five that remain are
   mission-keyed and populated corpus-wide, so a survey of an uncatalogued
   mission returns either a reading or a typed zero -- which is what makes them
   answerable.

   A carrier the catalog already holds a reading for is SETTLED and worth 0
   nats. So surveying a mission U23 already catalogued gains nothing from this
   half, and the three subjects it read score 0 here. That is the behaviour the
   term should have, and it is worth stating that it inverts the naive
   expectation: M-zaif-harness-v1, the mission the machine wants, is the LEAST
   informative survey target on the catalog half precisely because it has
   already been surveyed.

   EVERY QUESTION GOES THROUGH ONE KERNEL. `mission-epistemic-value/latent-eig`
   -> `epistemic-value/expected-information-gain`, with its Bayes-coherence
   gate. No spread, no gap lookup -- `epistemic_value.clj:9-13` refuses those by
   name, and a second EIG implementation here would be the same refusal
   evaded.

   DEFAULT OFF, twice over. `:survey-mission` is not proposable unless
   `portfolio-action-proposer/*portfolio-proposer-active?*` is true (false by
   default), and `efe/default-survey-eig-weight` is 0.0, so the leg is absent
   from `:augmentation-terms` and `:controller-score` is byte-identical on the
   default path. Flipping either default is Joe's.

   DISCHARGE is declared and unwritable: see `discharge-shape`."
  (:require [clojure.string :as str]
            [futon2.aif.mission-epistemic-value :as mev]))

;; ---------------------------------------------------------------------------
;; (a) the MAP-question carrier
;; ---------------------------------------------------------------------------

(def map-question-heading-pattern
  "DECLARED NARROWLY. A standalone `MAP must answer:` line, optionally
   emphasised or a heading. Deliberately not `(?i)must answer` anywhere in a
   line -- see the namespace docstring for the four docs that would add and why
   none of them is a MAP question list."
  #"(?i)^\s*(?:#+\s*)?(?:\*\*)?MAP must answer(?:\*\*)?\s*:?\s*$")

(def numbered-item-pattern
  #"^\s*(\d+)[.)]\s+(\S.*)$")

(def near-miss-phrasings
  "Measured 2026-09-03 over the 238 primary mission docs: every other
   occurrence of `must answer`, with what it actually is. Recorded so the
   narrow carrier is a choice on the record rather than an oversight."
  [{:pointer "futon3c/holes/missions/M-agency-hardening.md:196"
    :text "Questions the evidence bundle must answer:"
    :why-not-matched :not-a-map-phase-question-list}
   {:pointer "futon5a/holes/missions/M-stack-stereolithography.md:1323"
    :text "Questions the audit must answer:"
    :why-not-matched :not-a-map-phase-question-list}
   {:pointer "futon3c/holes/missions/M-apm-demonstration.md:15"
    :text "each with the question MAP must answer"
    :why-not-matched :prose-reference-no-list}
   {:pointer "futon3c/holes/missions/M-apm-demonstration.md:278"
    :text "with the question MAP must answer stated as: already done / partly done / greenfield?"
    :why-not-matched :one-question-restated-for-a-table-column}])

(defn map-questions-from-text
  "Pure. The numbered items under the doc's `MAP must answer:` line, with the
   1-based line number of each. A blank line does not end the list (the block in
   M-apm-capability-ratchet has one between the heading and item 1); a
   non-blank, non-numbered line does.

   Returns `{:questions [...]}` when the carrier is present, or
   `{:questions [] :absence {:reason :no-map-question-list}}` when it is not."
  [text]
  (let [lines (vec (str/split-lines (or text "")))
        start (first (keep-indexed (fn [i l]
                                     (when (re-matches map-question-heading-pattern l) i))
                                   lines))]
    (if (nil? start)
      {:questions [] :absence {:reason :no-map-question-list}}
      (let [items (loop [i (inc start) acc []]
                    (if (>= i (count lines))
                      acc
                      (let [l (nth lines i)]
                        (cond
                          (str/blank? l) (recur (inc i) acc)
                          (re-matches numbered-item-pattern l)
                          (let [[_ n t] (re-matches numbered-item-pattern l)]
                            (recur (inc i) (conj acc {:ordinal (parse-long n)
                                                      :line (inc i)
                                                      :text (str/trim t)})))
                          ;; a continuation line of the previous item
                          (and (seq acc) (re-matches #"^\s{3,}\S.*$" l))
                          (recur (inc i)
                                 (update-in acc [(dec (count acc)) :text]
                                            #(str % " " (str/trim l))))
                          :else acc))))]
        (if (seq items)
          {:questions items :heading-line (inc start)}
          {:questions [] :absence {:reason :map-question-heading-with-no-numbered-items}
           :heading-line (inc start)})))))

;; ---------------------------------------------------------------------------
;; (b) the kin-catalog gap
;; ---------------------------------------------------------------------------

(def catalog-carriers
  "U23's eleven-carrier cascade record, partitioned. `:answerable?` is not a
   judgement about the carrier's importance; it is whether a survey of an
   uncatalogued mission could return a reading OR a typed zero for it, which is
   what makes the latent readable at all. Each entry carries U23's own absence
   reason and the corpus count it measured, so the partition is auditable
   against `runs/U23-cascade-catalog/carrier-population.edn` rather than
   asserted here."
  [{:carrier :clocked-on :answerable? true
    :u23-reason :records-exist-none-for-subject
    :corpus "65 edges over 14 missions; 7 for M-zaif-harness-v1"}
   {:carrier :trace-decision :answerable? true
    :u23-reason :observed
    :corpus "885 trace records; n=3 / 125 / 125 for U23's three subjects"}
   {:carrier :trace-shown :answerable? true
    :u23-reason :records-exist-none-for-subject
    :corpus "234 records with :shown, 450 occurrences, 5 missions"}
   {:carrier :held-on-mission :answerable? true
    :u23-reason :records-exist-none-for-subject
    :corpus "124 edges"}
   {:carrier :shares-capability-with :answerable? true
    :u23-reason :records-exist-none-for-subject
    :corpus "1 relation -- populated, and thin enough to say so"}
   {:carrier :psr :answerable? false
    :u23-reason :no-typed-carrier
    :corpus "no writer exists; flight-log.spec.edn:61 is v0.1 and unwired"}
   {:carrier :pur :answerable? false
    :u23-reason :no-typed-carrier
    :corpus "no writer exists; flight-log.spec.edn:93"}
   {:carrier :pattern-phylogeny :answerable? false
    :u23-reason :records-exist-not-keyed-by-mission
    :corpus "2 co-application / 0 descent edges, pattern-keyed"}
   {:carrier :apm-frames :answerable? false
    :u23-reason :records-exist-not-keyed-by-mission
    :corpus "172 frames keyed by Lean problem id"}
   {:carrier :cross-mission-references :answerable? false
    :u23-reason :subject-absent-from-carrier-key-space
    :corpus "9 relations over 8 :mission entities in a disjoint id space"}
   {:carrier :flight-discharge :answerable? false
    :u23-reason :writer-exists-no-records
    :corpus "writer reachable, zero *.flight.edn anywhere under ~/code"}])

(def answerable-carriers
  (into [] (comp (filter :answerable?) (map :carrier)) catalog-carriers))

(def unanswerable-carriers
  (into [] (comp (remove :answerable?) (map :carrier)) catalog-carriers))

;; ---------------------------------------------------------------------------
;; the question set and the term
;; ---------------------------------------------------------------------------

(def open-latent-probability
  "DECLARED, and it is U22's declaration reused: a latent the judge holds no
   evidence either way about sits at 0.5, so reading it perfectly is worth ln 2
   nats (`mission_epistemic_value.clj:59-71` fiats (a) and (b))."
  0.5)

(defn survey-questions
  "The typed question set for surveying one mission.

   Input:
     :mission-id       the registry id (`M-foo` or `foo`)
     :map-questions    the `map-questions-from-text` result for its doc, or nil
     :catalog-readings set (or map) of carriers the catalog already holds a
                       reading for, for THIS mission; nil = none read

   Every question carries `:p` (the judge's belief) and `:nats` (what resolving
   it would gain), both through the one kernel."
  [{:keys [map-questions catalog-readings]}]
  (let [read? (set (if (map? catalog-readings) (keys catalog-readings) catalog-readings))
        map-qs (for [q (:questions map-questions)]
                 {:question [:map-question (:ordinal q)]
                  :basis {:reading :absent
                          :reason :no-per-question-answer-carrier
                          :doc-line (:line q)
                          :text (:text q)}
                  :p open-latent-probability
                  :nats (mev/latent-eig open-latent-probability)})
        carrier-qs (for [c answerable-carriers
                         :let [seen (contains? read? c)
                               p (if seen 1.0 open-latent-probability)]]
                     {:question [:catalog-carrier c]
                      :basis (if seen
                               {:reading :catalog-record :carrier c}
                               {:reading :absent
                                :reason :mission-not-in-cascade-catalog
                                :carrier c})
                      :p p
                      :nats (mev/latent-eig p)})]
    {:questions (vec (concat map-qs carrier-qs))
     :map-question-absence (:absence map-questions)
     :excluded-carriers unanswerable-carriers
     :excluded-carrier-count (count unanswerable-carriers)}))

(defn- readable-phase
  "The phase readings `mission-epistemic-value` accepts, re-derived from its
   public table rather than declared again."
  [phase]
  (let [p (some-> phase str str/trim str/lower-case)]
    (when (and (seq p)
               (contains? mev/phase-doability-prior p)
               (not= "unknown" p))
      p)))

(defn survey-value
  "The epistemic payload of surveying one mission, typed.

   Returns `:status :measured` with `:nats` (the quantity the G leg subtracts),
   `:availability`, `:normalized` (reported, not read) and the per-question
   basis; or a `:status` naming why no number was produced, with `:nats` 0.0."
  [{:keys [mission-id phase] :as reading}]
  (let [phase (readable-phase phase)]
    (cond
      (nil? phase)
      {:status :phase-unreadable :mission-id mission-id :nats 0.0}

      (nil? (get mev/phase-survey-availability phase))
      {:status :phase-not-in-availability-table :mission-id mission-id
       :phase phase :nats 0.0}

      :else
      (let [availability (double (get mev/phase-survey-availability phase))
            {:keys [questions map-question-absence
                    excluded-carriers excluded-carrier-count]}
            (survey-questions reading)
            raw-nats (reduce + 0.0 (map :nats questions))
            nats (* availability raw-nats)
            reference (* (long mev/reference-open-questions) mev/ln-2)]
        {:status :measured
         :mission-id mission-id
         :phase phase
         :availability availability
         :raw-nats raw-nats
         :nats nats
         :reference-nats reference
         :normalized (min 1.0 (/ nats reference))
         :question-count (count questions)
         :open-question-count (count (filter #(= open-latent-probability (:p %)) questions))
         :map-question-count (count (filter #(= :map-question (first (:question %))) questions))
         :map-question-absence map-question-absence
         :excluded-carriers excluded-carriers
         :excluded-carrier-count excluded-carrier-count
         :questions questions}))))

(defn record-for
  "What the enricher attaches to one `:survey-mission` candidate, or nil when
   `readings` is absent (the default path)."
  [readings mission]
  (when readings
    (let [id (mev/normalize-mission-id mission)
          reading (get-in readings [:missions id])
          result (if reading
                   (survey-value reading)
                   {:status :mission-absent-from-survey-readings
                    :mission-id id :nats 0.0})]
      {:survey-eig-nats (double (:nats result 0.0))
       :survey-eig-basis (dissoc result :nats)})))

(defn enrich-survey-candidates
  "Attach the epistemic payload to every `:survey-mission` candidate. Other
   candidate types pass through untouched, and so does every candidate when
   `readings` is nil."
  [readings candidates]
  (if-not readings
    (vec candidates)
    (mapv (fn [c]
            (if (= :survey-mission (:type c))
              (merge c (record-for readings (:target c)))
              c))
          candidates)))

(defn readings-from-missions
  "Build the survey readings from mission-registry entries.

   `missions` is the `:missions` seq the WM state already carries -- each entry
   has `:id`, `:path` and `:status-class`. `phase-of` maps mission id -> phase
   string (the `:mission/phase` prop `mission-epistemic-value` reads); when a
   mission is absent from it, `:status-class` is used as the phase, which is the
   registry's own lifecycle token (`mission_registry.clj:120-127`) and is
   recorded per reading as `:phase-carrier`.

   `catalog` maps mission id -> the set of carriers the cascade catalog holds a
   reading for. Absent ids read as uncatalogued.

   Pure apart from one `slurp` per mission doc."
  [{:keys [missions phase-of catalog]}]
  {:missions
   (into {}
         (map (fn [{:keys [id path status-class]}]
                (let [nid (mev/normalize-mission-id id)
                      hyperedge-phase (get phase-of nid)
                      phase (or hyperedge-phase (some-> status-class name))
                      text (try (slurp path) (catch Exception _ nil))]
                  [nid {:mission-id nid
                        :phase phase
                        :phase-carrier (if hyperedge-phase
                                         :mission-doc-prop
                                         :registry-status-class)
                        :doc-path path
                        :doc-readable? (some? text)
                        :map-questions (map-questions-from-text text)
                        :catalog-readings (set (get catalog nid))}])))
         missions)})

;; ---------------------------------------------------------------------------
;; discharge
;; ---------------------------------------------------------------------------

(def discharge-shape
  "DECLARED, AND NOT WRITABLE TODAY. U24's statement says the discharge of a
   survey flight is `MAP answers as typed observations at mission grain`. This
   is that shape, written as data so the next row can build against it rather
   than re-derive it -- and with the reason it cannot be emitted yet, which is
   not this row's to fix.

   `:blocked-by` is U23's measurement, not an opinion: the flight-discharge
   carrier's writer exists and is reachable
   (`futon3c/src/futon3c/aif/flight_record.clj:347 write-flight-record!`, called
   at `futon3c/src/futon3c/peripheral/war_machine_pilot.clj:585`) and there are
   ZERO `*.flight.edn` files anywhere under ~/code. So no survey flight can
   discharge until something flies, and no answer can be typed until it does."
  {:grain :mission
   :subject {:mission-id :string :run-id :string :action-type :survey-mission}
   :answers [{:question [:map-question :ordinal]
              :answer :string
              :basis {:reading :keyword :pointer :string}
              :p-after {:true :double :false :double}}]
   :carrier-readings [{:carrier :keyword
                       :variant #{:observed :absent}
                       :reason :keyword
                       :n :long}]
   :blocked-by {:carrier :flight-discharge
                :reason :writer-exists-no-records
                :writer "futon3c/src/futon3c/aif/flight_record.clj:347"
                :caller "futon3c/src/futon3c/peripheral/war_machine_pilot.clj:585"
                :measured-by "U23, runs/U23-cascade-catalog/carrier-population.edn"}
   :status :declared-not-implemented})
