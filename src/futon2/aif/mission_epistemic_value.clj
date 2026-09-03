(ns futon2.aif.mission-epistemic-value
  "The EPISTEMIC term of mission value: what advancing a mission would tell the
   judge about the field, in nats.

   WHY THIS EXISTS. The three-factor mission value
   (`futon2.report.war-machine/enrich-candidates-with-mission-value`) prices
   only the exploit side. `central` and `strategic` say where a payoff lands;
   `doable` says how close the mission is to delivering one (the
   `phase-doability` table rises monotonically toward INSTANTIATE). Nothing in
   it pays for finding out. Joe ruled on 2026-09-02 that the selector must be
   able to prefer a survey phase -- `futon4/holes/mission-lifecycle-wm-alignment.md`
   section 5, question 1 -- and named the AIF-native form: not a bigger fiat
   number for MAP, but an epistemic term, so that a MAP-phase mission wins
   exactly when uncertainty about the field is what blocks everything else, and
   the hand-authored table becomes a prior rather than the whole value.

   THE FORM. Per candidate mission M,

     epistemic(M) = survey-availability(phase(M))
                    * min(1, EIG(M) / (reference-open-questions * ln 2))

   where EIG(M) is the sum, over M's field questions, of the policy-conditioned
   expected information gain of resolving that question, computed by
   `futon2.aif.epistemic-value/expected-information-gain` -- the canonical
   kernel with its Bayes-coherence gate, not a spread or a gap lookup, which
   that namespace's docstring refuses.

   THE DENOMINATOR IS A DECLARED CONSTANT, not the mission's own question
   count, and the difference is not cosmetic. Dividing by n(M) * ln 2 measures
   the DENSITY of ignorance, and on the 2026-09-03 field that ranked
   M-metric-harness (one question, open, 0.69 nats) above M-pattern-mining
   (twelve questions, nine open, 6.24 nats) -- the thinnest neighbourhood
   winning the survey. Dividing by a fixed reference keeps the term
   proportional to the nats, which is the additive quantity the theory has, and
   leaves the factor in [0, 1] so it blends with the other three on their
   scale. The raw nats are recorded beside it and are the honest quantity.

   WHAT IS MEASURED AND WHAT IS DECLARED, kept apart on purpose.

   MEASURED, per question, from the `code/v05/mission-doc` hyperedge the judge
   already reads: for each mission id in M's `:mission/cross-refs` THAT NAMES A
   MISSION THE INDEX KNOWS, whether that neighbour's `:mission/phase` is
   readable at all; and whether M's own `:mission/mtime` is inside the
   freshness window. Each is a bit the judge either holds or does not, and the
   counts come from the substrate.

   A CROSS-REFERENCE THAT RESOLVES TO NOTHING IS NOT A QUESTION, and this is
   not a technicality. On 2026-09-03, 266 of the field's cross-references over
   116 missions named no mission in the index, and 39 of those were numeric --
   `M-1` through `M-7` on M-pattern-mining, `M-foo`/`M-bar`/`M-baz` on
   M-portfolio-inference, `M-INC`, `M-WS`. Some are a regex scrape catching
   list markers; some may be real references to missions that have no doc. The
   judge cannot tell those apart, so paying ln 2 nats for each would let a
   malformed cross-reference list buy survey priority -- and it did: the first
   run of this term put M-pattern-mining at rank 4 on nine open questions, of
   which eight were `M-1`..`M-7` and `M-trip-report`. They are excluded from
   the score and counted as `:unresolvable-cross-references` instead.

   DECLARED, and this is the fiat half:
   (a) the latent behind a neighbour question is `workable?` -- is the
       neighbour at or past DERIVE -- because that is the property of the
       neighbourhood a plan needs and cannot get from an unreadable phase;
   (b) the observation model is a PERFECT reading of that bit, so a question
       whose phase is unreadable has prior 0.5 and yields ln 2 nats, and a
       question whose phase is read is settled and yields 0;
   (c) `phase-survey-availability`, one number per phase, saying whether
       advancing that phase produces field readings at all.
   (c) is a hand-authored table exactly like `phase-doability`. Under the
   ruling that is what it is allowed to be: it multiplies measured nats
   instead of standing in for them, so a MAP-phase mission whose neighbourhood
   is fully read scores 0 here and cannot win on its phase alone.

   NOT IN THE SCORE, recorded instead: the field-level unknown census (how many
   `code/v05/mission-doc` hyperedges carry no `:mission/id` at all, so the judge
   cannot even name their phase). It is the same for every candidate on a tick,
   so it cannot move a rank; `field-readings` returns it as `:census` so a
   reader can see the denominator the per-mission numbers sit in.

   TYPED ABSENCE, never a flat zero standing in for a reading. A mission whose
   own phase is unreadable gets `:status :phase-unreadable` and contributes
   nothing; a phase outside the availability table gets
   `:status :phase-not-in-availability-table`. Both carry a 0.0 term, and the
   reason is what tells them apart from a measured 0.0.

   DEFAULT OFF. `:epistemic` is 0.0 in
   `war-machine/default-mission-value-weights`, so on the default path
   `field-readings` returns nil, no substrate read happens, and no recorded
   number moves. Turning it on is a declared weights map or
   FUTON_WM_VALUE_WEIGHTS; flipping the DEFAULT is Joe's (worklist J row)."
  (:require [clojure.string :as str]
            [futon2.aif.epistemic-value :as epistemic-value]
            [futon2.aif.substrate :as substrate])
  (:import (java.time LocalDate)
           (java.time.temporal ChronoUnit)))

(def mission-doc-hyperedge-type
  "The family `war-machine/mission-doc-index` already fetches."
  "code/v05/mission-doc")

(def phase-doability-prior
  "The hand-authored doability table, copied from
   `war-machine/phase-doability` (scripts/futon2/report/war_machine.clj). Kept
   here because the workable partition below is read off it rather than
   declared a second time; `war_machine_test` pins the two equal, so a change
   to one without the other fails a test instead of drifting."
  {"head" 0.1
   "identify" 0.2
   "map" 0.3
   "derive" 0.5
   "argue" 0.6
   "verify" 0.8
   "instantiate" 1.0
   "document" 0.4
   "complete" 0.0
   "unknown" 0.3})

(def workable-threshold
  "DECLARED. A phase is workable when the doability prior puts it at or above
   this. On the table above that is DERIVE and later, which is the boundary the
   lifecycle already draws: before DERIVE a mission is still being framed."
  0.5)

(def workable-phases
  "Derived from `phase-doability-prior`, not declared separately."
  (into #{}
        (comp (filter (fn [[_ d]] (>= (double d) workable-threshold)))
              (map key))
        (dissoc phase-doability-prior "unknown")))

(def phase-survey-availability
  "DECLARED, fiat, one number per phase: does advancing a mission in this phase
   produce readings about the field? MAP is the survey phase and gets 1.0;
   IDENTIFY locates the problem in the field and gets most of it; HEAD frames;
   DERIVE and later consume the map rather than extend it. `unknown` is
   deliberately absent -- a phase the judge cannot read earns no survey credit,
   and says so as a typed absence rather than taking a middle number."
  {"head" 0.3
   "identify" 0.6
   "map" 1.0
   "derive" 0.2
   "argue" 0.1
   "verify" 0.0
   "instantiate" 0.0
   "document" 0.1
   "complete" 0.0})

(def reference-open-questions
  "DECLARED. The number of simultaneously-open field questions at which the
   epistemic factor saturates. Ten is fiat, of the same class as
   `phase-survey-availability`; what it buys is that the factor is the
   mission's EIG rescaled by a CONSTANT rather than by anything about the
   candidate itself, over the whole range where the clamp does not fire.
   `:clamped?` says per record whether it fired, and
   `runs/U22-mission-epistemic/06-field-census.edn` counts how often it did on
   the 2026-09-03 field."
  10)

(def freshness-window-days
  "DECLARED. A mission-doc reading older than this is treated as no longer
   telling the judge what the mission's own field looks like."
  14)

(def ln-2 (Math/log 2.0))

(defn normalize-mission-id
  "`M-foo` and `foo` name one mission, the same way war-machine reads them."
  [mission-name]
  (str/replace-first (str mission-name) #"^M-" ""))

(defn prop
  "One prop, read the way `war-machine/hx-prop` reads it. The mission-doc
   family is served with BOTH shapes: on 2026-09-03 the 374 hyperedges carried
   `:mission/id` as a keyword on 75 of them and as the string \"mission/id\" on
   the other 299, and a keyword-only accessor silently reported those 299 as
   unnamed -- which is what the first run of `u22_mission_epistemic.clj` did."
  [props k]
  (let [named (if-let [ns-part (namespace k)] (str ns-part "/" (name k)) (name k))]
    (or (get props k) (get props (keyword named)) (get props named))))

(defn- readable-phase
  [phase]
  (let [p (some-> phase str str/trim str/lower-case)]
    (when (and (seq p) (contains? phase-doability-prior p) (not= "unknown" p))
      p)))

(defn binary-latent-model
  "The kernel input for one binary latent held at probability P.

   An open latent (0 < p < 1) is read perfectly: two possible observations,
   each collapsing the belief onto one branch. A settled latent has nothing to
   observe, so it gets the one-observation model whose posterior IS the prior --
   which the kernel's coherence gate accepts and scores at 0 nats. Writing it
   this way keeps every question on the same code path instead of special-casing
   zero outside the kernel."
  [p]
  (let [p (double p)
        prior {:true p :false (- 1.0 p)}]
    (if (or (zero? p) (== 1.0 p))
      {:prior prior
       :predicted-observations {:no-news 1.0}
       :posteriors {:no-news prior}}
      {:prior prior
       :predicted-observations {:workable p :not-workable (- 1.0 p)}
       :posteriors {:workable {:true 1.0 :false 0.0}
                    :not-workable {:true 0.0 :false 1.0}}})))

(defn latent-eig
  "Expected information gain in nats for one binary latent at probability P."
  [p]
  (epistemic-value/expected-information-gain (binary-latent-model p)))

(defn- stale-reading?
  [mtime as-of]
  (let [read-date (try (LocalDate/parse (str mtime)) (catch Exception _ nil))]
    (cond
      (nil? read-date) :unparseable
      (> (.between ChronoUnit/DAYS read-date as-of) (long freshness-window-days))
      :stale
      :else :fresh)))

(defn mission-questions
  "The field questions for one mission reading, each typed with its basis.

   `:p` is the judge's belief that the latent is true: 0.5 when it holds no
   evidence either way, and 0.0/1.0 when the reading settles it. `:nats` is
   what resolving the question would gain."
  [{:keys [mission-id cross-refs mtime phase-of-neighbour as-of]}]
  (let [own (normalize-mission-id (or mission-id ""))
        referenced (->> (or cross-refs [])
                        (map normalize-mission-id)
                        (remove str/blank?)
                        (remove #(= own %))
                        distinct)
        {neighbours true unresolvable false}
        (group-by #(contains? phase-of-neighbour %) referenced)
        neighbour-qs
        (for [n neighbours
              :let [phase (readable-phase (get phase-of-neighbour n))
                    p (cond (nil? phase) 0.5
                            (contains? workable-phases phase) 1.0
                            :else 0.0)]]
          {:question [:neighbour-workable n]
           :basis (if phase
                    {:reading :mission-doc-phase :phase phase}
                    {:reading :absent :reason :neighbour-phase-unreadable})
           :p p
           :nats (latent-eig p)})
        freshness (stale-reading? mtime as-of)
        p-fresh (if (= :fresh freshness) 1.0 0.5)]
    {:questions
     (vec (conj (vec neighbour-qs)
                {:question [:own-reading-current]
                 :basis {:reading (if (= :unparseable freshness)
                                    :absent
                                    :mission-doc-mtime)
                         :mtime mtime
                         :as-of (str as-of)
                         :freshness freshness
                         :window-days freshness-window-days}
                 :p p-fresh
                 :nats (latent-eig p-fresh)}))
     :unresolvable-cross-references (vec (sort unresolvable))}))

(defn epistemic-of
  "The epistemic term for one mission reading, typed.

   Returns `:status :measured` with `:epistemic` in [0, 1], `:nats`, the
   declared `:availability` and the per-question basis; or a `:status` naming
   why no number was produced, with `:epistemic` 0.0."
  [{:keys [mission-id phase] :as reading}]
  (let [phase (readable-phase phase)]
    (if-not phase
      {:status :phase-unreadable :mission-id mission-id :epistemic 0.0}
      (if-let [availability (get phase-survey-availability phase)]
        (let [{:keys [questions unresolvable-cross-references]}
              (mission-questions reading)
              nats (reduce + 0.0 (map :nats questions))
              reference (* (long reference-open-questions) ln-2)
              normalized (min 1.0 (/ nats reference))]
          {:status :measured
           :mission-id mission-id
           :phase phase
           :phase-carrier :mission-doc-prop
           :availability availability
           :nats nats
           :reference-nats reference
           :normalized normalized
           :clamped? (> nats reference)
           :question-count (count questions)
           :open-question-count (count (filter #(= 0.5 (:p %)) questions))
           :questions questions
           :unresolvable-cross-references unresolvable-cross-references
           :unresolvable-cross-reference-count (count unresolvable-cross-references)
           :epistemic (* (double availability) normalized)})
        {:status :phase-not-in-availability-table
         :mission-id mission-id
         :phase phase
         :epistemic 0.0}))))

(defn readings-from-hyperedges
  "Pure: mission-doc hyperedges -> {mission-id reading}, plus the unknown
   census. A hyperedge with no `:mission/id` cannot be named by the judge at
   all; those are counted, not guessed at."
  [hyperedges as-of]
  (let [props (map :hx/props hyperedges)
        named (filter #(seq (str (or (prop % :mission/id) ""))) props)
        by-id (into {}
                    (map (fn [p]
                           [(normalize-mission-id (prop p :mission/id))
                            {:mission-id (normalize-mission-id
                                          (prop p :mission/id))
                             :phase (prop p :mission/phase)
                             :mtime (prop p :mission/mtime)
                             :cross-refs (vec (prop p :mission/cross-refs))
                             :as-of as-of}]))
                    named)
        phase-of (into {} (map (fn [[id r]] [id (:phase r)])) by-id)]
    {:as-of (str as-of)
     :census {:hyperedges (count hyperedges)
              :named (count named)
              :unnamed (- (count hyperedges) (count named))
              :phase-readable (count (keep (comp readable-phase :phase val)
                                           by-id))}
     :missions (update-vals by-id #(assoc % :phase-of-neighbour phase-of))}))

(defn field-readings
  "Read the mission-doc family once and build every candidate's reading.

   Returns nil when the declared `:epistemic` weight is not positive: on the
   default path this is the whole of the epistemic term's cost, and it is zero
   substrate reads and zero recorded numbers."
  [weights opts]
  (when (pos? (double (or (:epistemic weights) 0.0)))
    (let [as-of (or (:epistemic-as-of opts) (LocalDate/now))
          hyperedges (substrate/hyperedges-by-type mission-doc-hyperedge-type
                                                   (assoc opts :limit 1000))]
      (readings-from-hyperedges hyperedges as-of))))

(defn record-for
  "What the judge attaches to one mission candidate, or nil when the term is
   off. `:epistemic-basis` carries the typed reading, including whether the
   phase this term read agrees with the phase the doability factor was given --
   the two arrive by different carriers (`:mission/phase` on the hyperedge here,
   `futon3c.aif.mission-delta-t` there) and a disagreement is a fact about the
   judge, not a number to average away."
  [readings mission doability-phase]
  (when readings
    (let [id (normalize-mission-id mission)
          reading (get-in readings [:missions id])
          result (if reading
                   (epistemic-of reading)
                   {:status :mission-absent-from-mission-doc-index
                    :mission-id id
                    :epistemic 0.0})
          read-phase (:phase result)
          doability (readable-phase doability-phase)]
      {:epistemic (double (:epistemic result 0.0))
       :epistemic-basis
       (assoc (dissoc result :epistemic)
              :doability-phase doability-phase
              :phase-agreement (cond
                                 (nil? doability) :doability-phase-absent
                                 (nil? read-phase) :epistemic-phase-absent
                                 (= doability read-phase) :agree
                                 :else :disagree))})))
