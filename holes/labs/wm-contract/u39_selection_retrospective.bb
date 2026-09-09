#!/usr/bin/env bb
;; U39 -- SELECTION-RATIONALE RETROSPECTIVE, design pass.
;;
;;   bb holes/labs/wm-contract/u39_selection_retrospective.bb
;;   bb holes/labs/wm-contract/u39_selection_retrospective.bb --deposit <run-id>  (RE6)
;;
;; READ-ONLY. Reads the recorded wm-trace corpus and the committed U42 readback;
;; writes only under holes/labs/wm-contract/runs/U39-selection-retrospective/.
;; No tick, no run lock, no substrate call, no network. Nothing under data/ is
;; written or touched.
;;
;; WHAT IT PRODUCES. The measurements the U39 design doc quotes: the carrier
;; census (which of the fields a retrospective would read are populated, and on
;; which records), the two refusal channels, one worked RATIONALE-AS-CLAIM
;; record, one worked RETROSPECTIVE-VERDICT record, one worked TENSION MINT
;; payload, and the controls. It does not implement any of the three shapes on
;; the live path -- it PROJECTS them out of records that already exist, which is
;; the point of the design: no new logging of what is already logged.
;;
;; DETERMINISM. The artifact carries no wall-clock field, so two runs over an
;; unchanged corpus are byte-identical. Every number is read from a record; the
;; only arithmetic is subtraction of two recorded numbers, and the controls pin
;; that the differences reproduce the recorded scores exactly.

(require '[babashka.classpath :as cp]
         '[babashka.process :as process]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[clojure.pprint :as pp])

(def repo-root (str (System/getProperty "user.home") "/code/futon2"))

;; THE OUTCOME ACCESSOR IS THE PRODUCTION ONE, not a copy of it (worklist
;; :U59). This script used to mirror `war-machine/trace-outcome` with a comment
;; pointing at the line it mirrored, and a mirror is a second thing to keep
;; true: C511-repair-or-elaborate.md section 3 found three key vocabularies for
;; one quantity precisely because every reader had its own spelling.
;; `futon2.aif.realized-outcome` is pure Clojure with no JVM-only dependency, so
;; babashka loads the same file the JVM does and the two cannot drift.
(cp/add-classpath (str repo-root "/src"))
(require '[futon2.aif.realized-outcome :as ro])
(def out-dir (io/file repo-root "holes/labs/wm-contract/runs/U39-selection-retrospective"))

;; Old trace records carry tagged literals this script has no business
;; interpreting; the default reader keeps the tag visible rather than throwing,
;; so the corpus census covers every file instead of stopping at the first one.
(def read-opts {:default (fn [t v] {:unread-tag t :value v})})

(defn trace-files []
  (->> (file-seq (io/file repo-root "data/wm-trace"))
       (filter #(.isFile ^java.io.File %))
       (map str)
       (filter #(re-find #"wm-trace-\d{4}-\d{2}-\d{2}\.edn$" %))
       sort
       vec))

(defn read-trace [path]
  (with-open [r (io/reader path)]
    (mapv #(edn/read-string read-opts %) (line-seq r))))

(defn day-of [path] (second (re-find #"wm-trace-(\d{4}-\d{2}-\d{2})\.edn$" path)))

(defn ranking [m] (get-in m [:decision :controller-ranking]))
(defn has-ranking? [m] (boolean (seq (ranking m))))
(def trace-outcome
  "The categorical outcome of a record, from the one vocabulary
   (`futon2.aif.realized-outcome/categorical-outcome`, which holds the three
   places `war-machine/trace-outcome` looks). Same function, one definition."
  ro/categorical-outcome)

;; ---------------------------------------------------------------------------
;; 1. Carrier census. The retrospective needs a rationale side and an outcome
;;    side. This counts the records that carry each, and the records that carry
;;    both -- which is the number the design turns on.

(defn carrier-census [days]
  (let [all (vec (mapcat :records days))]
    {:files (count days)
     :records (count all)
     :rationale-side
     {:controller-ranking (count (filter has-ranking? all))
      :decision-explanation (count (filter #(get-in % [:decision :decision-explanation]) all))
      :selection-law (count (filter #(get-in % [:decision :selection-law]) all))
      :ranked-actions (count (filter #(seq (:ranked-actions %)) all))
      :policy-support-exclusions (count (filter #(seq (:policy-support-exclusions %)) all))}
     :outcome-side
     {:realized-outcome (count (filter :realized-outcome all))
      :any-trace-outcome (count (filter trace-outcome all))
      :selection-gain-state (count (filter #(get-in % [:selection-gain :selection-gain]) all))
      :selection-gain-samples (frequencies (keep #(get-in % [:selection-gain :samples]) all))}
     :both-sides-on-one-record
     (count (filter #(and (has-ranking? %) (:realized-outcome %)) all))
     :realized-outcome-key-shapes
     (frequencies (map (comp vec sort keys) (keep :realized-outcome all)))
     :realized-outcome-foldable-by-r14
     ;; selection_gain.clj:196-206 requires NUMERIC :expected-score AND
     ;; :realized-score. Anything else is refused and gamma holds at the prior.
     (count (filter #(and (number? (:expected-score %)) (number? (:realized-score %)))
                    (keep :realized-outcome all)))
     :per-day
     (mapv (fn [{:keys [day records]}]
             {:day day :n (count records)
              :controller-ranking (count (filter has-ranking? records))
              :realized-outcome (count (filter :realized-outcome records))})
           days)}))

;; ---------------------------------------------------------------------------
;; 2. The two refusal channels. Joe's question is about what happens when the
;;    machine declines a piece of work; the corpus declines in two different
;;    places and records them in two different shapes.

(defn refusal-channels [era]
  (let [excl (mapcat :policy-support-exclusions era)]
    {:admission
     {:carrier "[:policy-support-exclusions]"
      :records-carrying (count (filter #(seq (:policy-support-exclusions %)) era))
      :total-refusals (count excl)
      :per-tick (frequencies (map #(count (:policy-support-exclusions %)) era))
      :reasons (frequencies (map :reason excl))
      :scored? false
      :targets (vec (sort (distinct (map #(get-in % [:action :target]) excl))))}
     :selection
     {:carrier "[:decision :controller-ranking] rank > chosen"
      :records-carrying (count (filter has-ranking? era))
      :candidates-per-tick (frequencies (map (comp count ranking) era))
      :rejected-per-tick (frequencies (map #(dec (count (ranking %))) era))
      :typed-reason? false
      :note "a rejection has a MARGIN and a per-term decomposition on the record, and no reason field; the claim shape below is what turns the margin into a reason"}}))

;; ---------------------------------------------------------------------------
;; 3. (a) RATIONALE-AS-CLAIM -- projected, never logged anew.

(def term-keys
  "The per-candidate terms `efe/rank-actions` records. A rejection margin is
   attributed among these and nothing else."
  [:G-risk :G-ambiguity :G-goal-outcome :controller-augmentation
   :structural-pressure :graph-feasibility-penalty :gap-exploration-bonus])

(defn by-target [m]
  (into {} (map (fn [e] [[(get-in e [:action :type]) (get-in e [:action :target])] e]))
        (:ranked-actions m)))

(defn ranking-index [m]
  (into {} (map (fn [e] [[(get-in e [:action :type]) (get-in e [:action :target])] (:rank e)]))
        (ranking m)))

(defn action-key [a] [(:type a) (:target a)])

(defn num- [x] (double (or x 0.0)))

(defn margin-decomposition
  "Where the loser's G exceeds the winner's, term by term. Positive entries are
   terms on which the rejected candidate lost."
  [win lose]
  (into {} (for [k term-keys
                 :let [d (- (num- (k lose)) (num- (k win)))]
                 :when (not (zero? d))]
             [k d])))

(defn rejected-entry [m win lose-rank-idx lose]
  (let [decomp (margin-decomposition win lose)
        total (- (num- (:G-core lose)) (num- (:G-core win)))
        lost-on (when (seq decomp) (key (apply max-key val decomp)))
        habit-delta (- (num- (:habit-prior-bias lose)) (num- (:habit-prior-bias win)))
        law (get-in m [:decision :selection-law])]
    {:action (select-keys (:action lose) [:type :target])
     :controller-rank (:rank lose)
     :ranking-position lose-rank-idx
     :G-core (:G-core lose)
     :margin/total total
     :margin/by-term decomp
     :lost-on lost-on
     :lost-on-share (when (and lost-on (not (zero? total))) (/ (get decomp lost-on) total))
     :term-source
     {:G-risk-driver
      {:mission-value-factor {:chosen (get-in win [:action :mission-value-factor])
                              :rejected (get-in lose [:action :mission-value-factor])}
       :central {:chosen (get-in win [:action :central]) :rejected (get-in lose [:action :central])}
       :strategic {:chosen (get-in win [:action :strategic]) :rejected (get-in lose [:action :strategic])}
       :doable {:chosen (get-in win [:action :doable]) :rejected (get-in lose [:action :doable])}
       :non-progress-decay {:chosen (get-in win [:action :non-progress-decay])
                            :rejected (get-in lose [:action :non-progress-decay])}}}
     :counter-channel
     {:channel :habit-prior
      :favours (cond (pos? habit-delta) :rejected (neg? habit-delta) :chosen :else :neither)
      :by (Math/abs habit-delta)
      :consulted? (= :full-score-posterior (:applied law))
      :basis "policy.clj:582 and :593 -- lnE enters the score the selection takes its argmax of only under :full-score-posterior; policy.clj:619-621 records the same fact as :habit-authority"}}))

(defn claim-soundness
  "A claim minted from a record whose own :selection-law contradicts the ranking
   it names is UNSOUND AT MINT, and says so rather than being scored later."
  [m]
  (let [law (get-in m [:decision :selection-law])
        chosen (get-in m [:decision :action])
        idx (ranking-index m)
        actual (get idx (action-key chosen))]
    (cond
      (nil? actual)
      {:status :unsound :reason :chosen-action-absent-from-controller-ranking}

      (and (false? (:moved-from-controller-head? law))
           (number? (:chosen-rank law))
           (not= (:chosen-rank law) actual))
      {:status :unsound
       :reason :law-field-contradicts-ranking
       :law-says {:chosen-rank (:chosen-rank law)
                  :moved-from-controller-head? (:moved-from-controller-head? law)}
       :ranking-says {:chosen-controller-rank actual}}

      :else
      {:status :sound :chosen-controller-rank actual})))

(defn rationale-claim
  "(a) The record. Every field is a projection of a field already on the trace
   record; :claim/derived-from names them."
  [m file line top-k]
  (let [chosen (get-in m [:decision :action])
        ra (by-target m)
        win (get ra (action-key chosen))
        losers (->> (:ranked-actions m)
                    (remove #(= (action-key (:action %)) (action-key chosen)))
                    (sort-by :rank)
                    (take top-k)
                    vec)]
    {:claim/id (str "rc-" (:run/id m))
     :claim/at (:timestamp m)
     :claim/source {:file file :line line :run-id (:run/id m)}
     :claim/law (get-in m [:decision :selection-law])
     :claim/soundness-at-mint (claim-soundness m)
     :claim/chosen
     {:action (select-keys chosen [:type :target])
      :controller-rank (get (ranking-index m) (action-key chosen))
      :G-core (:G-core win)
      :terms (select-keys win term-keys)
      :mission-value-factor (:mission-value-factor chosen)}
     :claim/rejected
     (vec (map-indexed (fn [i l] (rejected-entry m win (inc i) l)) losers))
     :claim/refused
     (mapv (fn [x] {:action (select-keys (:action x) [:type :target])
                    :reason (:reason x)
                    :stage :admission
                    :scored? false})
           (:policy-support-exclusions m))
     :claim/assertion
     "the chosen action's G-core is below each listed rejected candidate's by :margin/total, and that margin is accounted for by :margin/by-term; :lost-on names the term carrying most of it"
     :claim/derived-from
     ["[:decision :action]" "[:decision :controller-ranking]" "[:decision :selection-law]"
      "[:ranked-actions]" "[:policy-support-exclusions]" "[:timestamp]" "[:run/id]"]
     :claim/new-logging-required :none}))

;; ---------------------------------------------------------------------------
;; 4. (b) RETROSPECTIVE VERDICT -- the overtake leg is the one that is
;;    measurable on the recorded corpus today; the other legs are typed.

(defn overtake
  "Did a candidate rejected at CLAIM-REC out-rank the candidate chosen there,
   at LATER-REC, on LATER-REC's own recorded scores?"
  [claim-rec later-rec top-k]
  (let [chosen (action-key (get-in claim-rec [:decision :action]))
        later-idx (ranking-index later-rec)
        later-ra (by-target later-rec)
        chosen-later (get later-idx chosen)
        losers (->> (:ranked-actions claim-rec)
                    (remove #(= (action-key (:action %)) chosen))
                    (sort-by :rank)
                    (take top-k))]
    {:chosen (vec chosen)
     :chosen-rank-at-claim (get (ranking-index claim-rec) chosen)
     :chosen-rank-later chosen-later
     :overtaken-by
     (vec (for [l losers
                :let [k (action-key (:action l))
                      rank-later (get later-idx k)
                      e-later (get later-ra k)
                      e-claim (get (by-target claim-rec) k)]
                :when (and rank-later chosen-later (< rank-later chosen-later))]
            (let [chosen-claim (get (by-target claim-rec) chosen)
                  chosen-later (get later-ra chosen)
                  m0 (- (num- (:G-core e-claim)) (num- (:G-core chosen-claim)))
                  m1 (- (num- (:G-core e-later)) (num- (:G-core chosen-later)))
                  chosen-delta (- (num- (:G-core chosen-later)) (num- (:G-core chosen-claim)))
                  rival-delta (- (num- (:G-core e-claim)) (num- (:G-core e-later)))
                  decay-fell? (< (num- (get-in chosen-later [:action :non-progress-decay]))
                                 (num- (get-in chosen-claim [:action :non-progress-decay])))
                  ;; DECLARED, and deliberately scalar-free: would the rival's OWN
                  ;; movement have closed the recorded margin with the chosen
                  ;; candidate held exactly where it was? Two booleans off the
                  ;; record, no threshold to tune.
                  rival-alone-suffices? (> rival-delta m0)]
            {:action (select-keys (:action l) [:type :target])
             :rank-at-claim (:rank l)
             :rank-later rank-later
             :margin-at-claim m0
             :margin-later m1
             :attribution
             {:swing (- m0 m1)
              :chosen-delta-G chosen-delta
              :rival-delta-G rival-delta
              :chosen-non-progress-decay-fell? decay-fell?
              :rival-alone-suffices? rival-alone-suffices?
              :reading (cond (and decay-fell? (not rival-alone-suffices?)) :chosen-decayed
                             rival-alone-suffices? :rival-improved-enough-on-its-own
                             :else :neither)}
             :what-moved
             {:chosen {:mission-value-factor
                       [(get-in (by-target claim-rec) [chosen :action :mission-value-factor])
                        (get-in later-ra [chosen :action :mission-value-factor])]
                       :non-progress-decay
                       [(get-in (by-target claim-rec) [chosen :action :non-progress-decay])
                        (get-in later-ra [chosen :action :non-progress-decay])]
                       :non-progress-count
                       [(get-in (by-target claim-rec) [chosen :action :non-progress-count])
                        (get-in later-ra [chosen :action :non-progress-count])]}
              :rival {:mission-value-factor
                      [(get-in e-claim [:action :mission-value-factor])
                       (get-in e-later [:action :mission-value-factor])]
                      :non-progress-decay
                      [(get-in e-claim [:action :non-progress-decay])
                       (get-in e-later [:action :non-progress-decay])]}}})))}))

(defn c-mis-leg
  "The U18/U42 gauge leg, read off the COMMITTED U42 readback rather than
   re-run, so this row adds no measurement of its own to that seam."
  [u42 run-id]
  (if-let [row (first (filter #(= run-id (:run-id %)) (:rows u42)))]
    (let [rb (:readback row)]
      (if (= :measured (:status rb))
        {:status :measured
         :mission (:mission rb)
         :criterion-count (:criterion-count rb)
         :measurable-count (:measurable-count rb)
         :risk-mis (:risk-values row)
         :action-sensitivity (:action-sensitivity rb)
         :source "runs/U42-producers/measurements.edn"}
        {:status :absent :reason (:reason rb) :mission (:mission rb)
         :criterion-count (:criterion-count rb)
         :source "runs/U42-producers/measurements.edn"}))
    {:status :absent :reason :run-not-in-u42-corpus}))

(defn outcome-leg
  "The OUTCOME leg: the realized outcome OBSERVED for the claim record's tick,
   by the post-accept observation pass (`wm_step_observe.bb`), joined on
   `:run/id`. Absent on every record of the live corpus and on every run
   accepted before :U59, and the count of that absence is what made it a
   measurement rather than a silence (C511 section 3)."
  [observations claim-rec]
  (if-let [o (get observations (:run/id claim-rec))]
    {:status (if (ro/categorical-outcome o) :measured :unknown)
     :outcome (ro/categorical-outcome o)
     :scale (:scale o)
     :expected-score (ro/expected-score o)
     :realized-score (ro/realized-score o)
     :vocabulary (ro/vocabulary o)
     :dial (:outcome/basis o)
     :observed-at-run (:observation/observed-at-run o)
     :basis "runs/<observing run>/observation/realized-outcome-<observed run>.edn"}
    {:status :absent
     :reason :no-observation-names-this-tick
     :would-need (str "an accepted step AFTER this one: the outcome of a decision does not exist "
                      "when the decision is written, so it is observed one step later and joined "
                      "by :run/id (wm_step.sh observe)")}))

(defn retrospective-verdict
  "(b) The record. The verdict vocabulary is closed; the rule that assigns it is
   DECLARED here and is the thing the registry entry registers as a free hand.

   OBSERVATIONS is a map `{run-id <observation record>}` and defaults to empty,
   which is the pre-:U59 behaviour exactly: no outcome leg, UPHELD unreachable."
  ([claim claim-rec later-rec u42 top-k]
   (retrospective-verdict claim claim-rec later-rec u42 top-k {}))
  ([claim claim-rec later-rec u42 top-k observations]
  (let [ol (outcome-leg observations claim-rec)
        outcome-grounded? (= :grounded-change (:outcome ol))
        ot (overtake claim-rec later-rec top-k)
        overtaken? (seq (:overtaken-by ot))
        readings (frequencies (map #(get-in % [:attribution :reading]) (:overtaken-by ot)))
        own-regret? (pos? (get readings :rival-improved-enough-on-its-own 0))
        cm-before (c-mis-leg u42 (:run/id claim-rec))
        cm-after (c-mis-leg u42 (:run/id later-rec))
        np (let [chosen (action-key (get-in claim-rec [:decision :action]))
                 e (get (by-target later-rec) chosen)]
             {:status :measured
              :non-progress? (get-in e [:action :non-progress?])
              :non-progress-count (get-in e [:action :non-progress-count])
              :non-progress-decay (get-in e [:action :non-progress-decay])
              :basis "war_machine.clj:2312-2326 previous-selection-non-progress?, :2339-2356 recent-non-progress-count, :2290 non-progress-decay-k"})]
    {:verdict/claim-id (:claim/id claim)
     :verdict/evaluated-against {:run-id (:run/id later-rec) :at (:timestamp later-rec)}
     :verdict/window {:from (:timestamp claim-rec) :to (:timestamp later-rec) :ticks-between 1}
     :verdict/legs
     {:overtake (assoc ot :status :measured :fired? (boolean overtaken?))
      :c-mis {:at-claim cm-before :at-evaluation cm-after
              :movement (if (and (= :measured (:status cm-before))
                                 (= :measured (:status cm-after)))
                          :comparable
                          {:status :untestable
                           :reason :c-mis-reads-the-selected-mission-only
                           :detail "the readback is keyed to the tick's SELECTED mission, so two ticks that select different missions produce two different subjects and no movement"})}
      :non-progress np
      :outcome ol
      :receipts {:status :absent
                 :reason :no-mission-to-receipt-carrier
                 :would-need "a carrier joining a commit/receipt to the mission that was held; U23 measured the two candidates -- flight-discharge :writer-exists-no-records (zero *.flight.edn under ~/code) and clocked-on, which records WHO clocked on and not WHAT landed"
                 :basis "runs/U23-cascade-catalog/carrier-population.edn"}}
     :verdict/overtake-readings readings
     :verdict/verdict
     (cond own-regret? :rationale-refuted
           outcome-grounded? :rationale-upheld
           overtaken? :rationale-untestable
           :else :rationale-untestable)
     :verdict/verdict-reason
     (cond own-regret? :a-rejected-candidate-closed-the-recorded-margin-on-its-own-movement
           outcome-grounded? :the-chosen-actions-observed-outcome-is-a-grounded-change
           overtaken? :overtake-attributable-to-the-chosen-candidates-own-non-progress-decay
           (= :measured (:status ol)) :outcome-observed-and-not-a-grounded-change
           :else :no-leg-measurable)
     :verdict/verdict-rule
     {:name :declared-attributed-overtake
      :status :declared-not-ruled
      :scalars :none
      :statement "REFUTED iff some candidate this claim rejected later out-ranked the chosen one AND its own recorded movement alone would have closed the margin the claim asserted. An overtake that only happens because the chosen candidate was decayed for having been chosen measures the decay, not the rationale, and is UNTESTABLE. UPHELD iff the refutation leg does not fire AND the outcome leg was OBSERVED to be a :grounded-change -- the chosen mission has strictly fewer open holes at the next accepted step. Refutation still wins over an upheld outcome: a rival that closed the recorded margin on its own movement refutes the claim about the RANKING whatever the chosen action then produced."
      :alternatives-not-taken
      {:overtake-dominant "any overtake refutes -- rejected here because on the recorded 09-02 pair it refutes every claim after one tick, since the chosen mission's mission-value-factor halves by construction"
       :weighted-legs "combine the four legs with declared weights -- rejected here because three of the four legs are typed absences on this corpus, so the weights would be unmeasurable"}}
     :verdict/basis
     ["[:ranked-actions] of both records" "[:decision :controller-ranking] of both records"
      "runs/U42-producers/measurements.edn" "runs/U23-cascade-catalog/carrier-population.edn"
      "the observing run's observation/realized-outcome-<observed run>.edn, when one names this tick"]})))

;; ---------------------------------------------------------------------------
;; 5. The primitive regret signal, measured.

(defn non-progress-census [days]
  (mapv (fn [{:keys [day records]}]
          (let [era (filterv has-ranking? records)]
            (when (seq era)
              {:day day
               :ticks (count era)
               :candidate-rows (reduce + (map (comp count :ranked-actions) era))
               :decay-values (frequencies (mapcat (fn [m] (keep #(get-in % [:action :non-progress-decay])
                                                                (:ranked-actions m)))
                                                  era))
               :chosen-non-progress-count (frequencies (map #(get-in % [:decision :action :non-progress-count]) era))
               :selected-targets (mapv #(get-in % [:decision :action :target]) era)
               :distinct-selected (count (distinct (map #(get-in % [:decision :action :target]) era)))
               :records-with-an-outcome (count (filter trace-outcome era))})))
        days))

;; ---------------------------------------------------------------------------
;; 6. (c) The tension mint payload. The tension RECORD is U41's
;;    (DESIGN-tensions-as-patterns.md section 3); this is only what a refuted
;;    rationale contributes to it.

(defn tension-mint [claim verdict claim-rec]
  (let [ot (get-in verdict [:verdict/legs :overtake])
        rival (or (first (filter #(= :rival-improved-enough-on-its-own
                                     (get-in % [:attribution :reading]))
                                 (:overtaken-by ot)))
                  (first (:overtaken-by ot)))
        chosen (get-in claim [:claim/chosen :action])
        rej (first (filter #(= (:action %) (:action rival)) (:claim/rejected claim)))
        _ (assert (some? rej) "tension mint needs the rejected entry the overtake names")]
    {:mint/from :refuted-rationale
     :mint/verdict-id (:verdict/claim-id verdict)
     :tension/born-of :refuted-rationale
     :tension/poles [(str "select by the " (name (or (:lost-on rej) :G)) " margin as scored this tick")
                     "select by a preference that survives to the next tick"]
     :tension/statement
     (format "%s was preferred over %s at %s on a %s margin of %s nats; one tick later %s out-ranked it, and its own G-core had fallen by %s -- enough to close that margin without any help from the chosen candidate's non-progress decay."
             (:target chosen)
             (:target (:action rival))
             (:claim/at claim)
             (name (or (:lost-on rej) :G))
             (:margin/total rej)
             (:target (:action rival))
             (get-in rival [:attribution :rival-delta-G]))
     :tension/carried-by (get-in claim-rec [:active-mission :mission-id])
     :tension/resolution-path :none-named-at-mint
     :tension/status :carried
     :tension/provenance
     {:claim (:claim/id claim)
      :verdict-against (get-in verdict [:verdict/evaluated-against :run-id])
      :records [(get-in claim [:claim/source :run-id])
                (get-in verdict [:verdict/evaluated-against :run-id])]
      :minted-by :u39-design-pass
      :written-to :nothing}
     :mint/record-shape-owner "DESIGN-tensions-as-patterns.md section 3 (U41 implements)"
     :mint/library-home
     {:carrier :flexiarg
      :root "futon3/library (pattern_registry.clj:48-53)"
      :addressed-by "pattern_registry.clj:158-161 candidate-pattern-path"
      :status :named-not-written
      :note "no flexiarg is written by this row; the birth rule in DESIGN-tensions-as-patterns.md section 2 requires >=2 typed receipts and a person"}}))

;; ---------------------------------------------------------------------------
;; Controls.

(defn controls [era claim claim-rec later-rec]
  (let [win (get (by-target claim-rec) (action-key (get-in claim-rec [:decision :action])))
        ;; C2: each rejection margin must reproduce the recorded controller-score
        ;; difference exactly -- the claim must not invent a number.
        deltas (for [r (:claim/rejected claim)
                     :let [e (get (by-target claim-rec) (action-key (:action r)))]]
                 (Math/abs (- (:margin/total r)
                              (- (num- (:controller-score e)) (num- (:controller-score win))))))]
    {:c1-fabricated-subject
     {:asks "a mission id that appears in no record yields no claim and no verdict"
      :subject "M-not-a-mission-u39-control"
      :appearances-in-era
      (count (filter (fn [m] (some #(= "M-not-a-mission-u39-control" (get-in % [:action :target]))
                                   (:ranked-actions m)))
                     era))
      :appearances-in-the-claim
      (count (filter #(= "M-not-a-mission-u39-control" (get-in % [:action :target]))
                     (:claim/rejected claim)))
      :passes? (and (zero? (count (filter (fn [m] (some #(= "M-not-a-mission-u39-control"
                                                            (get-in % [:action :target]))
                                                        (:ranked-actions m)))
                                          era)))
                    (empty? (filter #(= "M-not-a-mission-u39-control" (get-in % [:action :target]))
                                    (:claim/rejected claim))))}
     :c2-margin-identity
     {:asks "every :margin/total equals the recorded controller-score difference"
      :max-abs-delta (if (seq deltas) (apply max deltas) 0.0)
      :passes? (every? #(< % 1.0e-12) deltas)}
     :c3-overtake-detector-negative
     ;; The detector must NOT fire when a record is scored against itself: at the
     ;; claim record every rejected candidate is by construction below the chosen.
     (let [self (overtake claim-rec claim-rec 5)]
       {:asks "the overtake detector does not fire on a record scored against itself"
        :fired? (boolean (seq (:overtaken-by self)))
        :passes? (empty? (:overtaken-by self))})
     :c4-overtake-detector-positive
     (let [live (overtake claim-rec later-rec 5)]
       {:asks "the detector fires on the recorded 09-02 pair"
        :fired? (boolean (seq (:overtaken-by live)))
        :passes? (boolean (seq (:overtaken-by live)))})}))

;; ---------------------------------------------------------------------------

(defn -main []
  (let [files (trace-files)
        days (mapv (fn [f] {:day (day-of f) :file f :records (read-trace f)}) files)
        era-days (filterv #(some has-ranking? (:records %)) days)
        era (vec (mapcat :records era-days))
        u42 (edn/read-string (slurp (io/file repo-root "holes/labs/wm-contract/runs/U42-producers/measurements.edn")))
        d0902 (first (filter #(= "2026-09-02" (:day %)) days))
        recs (:records d0902)
        claim-rec (nth recs 1)                 ; 4abad68c -- selected M-zaif-harness-v1
        later-rec (nth recs 2)                 ; 801976e7 -- selected M-expressions-of-interest
        false-rec (nth recs 0)                 ; 0a18c4f7 -- the false selection-law stamp
        claim (rationale-claim claim-rec "data/wm-trace/wm-trace-2026-09-02.edn" 2 3)
        unsound (rationale-claim false-rec "data/wm-trace/wm-trace-2026-09-02.edn" 1 1)
        verdict (retrospective-verdict claim claim-rec later-rec u42 5)
        mint (tension-mint claim verdict claim-rec)
        art {:row :U39
             :generated-by "holes/labs/wm-contract/u39_selection_retrospective.bb"
             :read-only true
             :corpus {:dir "data/wm-trace"
                      :files (count files)
                      :records (reduce + (map (comp count :records) days))
                      :ranking-era-days (mapv :day era-days)
                      :ranking-era-records (count era)}
             :section-1-carrier-census (carrier-census days)
             :section-2-refusal-channels (refusal-channels era)
             :section-3a-rationale-claim claim
             :section-3a-unsound-claim-at-mint
             {:run-id (:run/id false-rec)
              :soundness (:claim/soundness-at-mint unsound)
              :note "the record's own selection-law says chosen-rank 1 and moved-from-controller-head? false; the controller ranking puts the chosen action elsewhere. A claim minted here is refused at mint, not scored later."}
             :section-4b-retrospective-verdict verdict
             :section-5-non-progress-census (non-progress-census days)
             :section-6c-tension-mint mint
             :controls (controls era claim claim-rec later-rec)}]
    (.mkdirs out-dir)
    (spit (io/file out-dir "u39-measurements.edn")
          (with-out-str (pp/pprint art)))
    (let [report (with-out-str
    (println "U39 -- selection-rationale retrospective, design pass")
    (println "=====================================================")
    (println)
    (println (format "corpus: %d files, %d records; ranking era %s (%d records)"
                     (count files) (reduce + (map (comp count :records) days))
                     (str/join ", " (map :day era-days)) (count era)))
    (let [cc (:section-1-carrier-census art)]
      (println)
      (println "1. CARRIER CENSUS")
      (println (format "   rationale side: controller-ranking %d, ranked-actions %d, exclusions %d"
                       (get-in cc [:rationale-side :controller-ranking])
                       (get-in cc [:rationale-side :ranked-actions])
                       (get-in cc [:rationale-side :policy-support-exclusions])))
      (println (format "   outcome side:   realized-outcome %d, any trace outcome %d, R14-foldable %d"
                       (get-in cc [:outcome-side :realized-outcome])
                       (get-in cc [:outcome-side :any-trace-outcome])
                       (:realized-outcome-foldable-by-r14 cc)))
      (println (format "   records carrying BOTH sides: %d" (:both-sides-on-one-record cc))))
    (let [rc (:section-2-refusal-channels art)]
      (println)
      (println "2. REFUSAL CHANNELS")
      (println (format "   admission: %d refusals over %d records, reasons %s"
                       (get-in rc [:admission :total-refusals])
                       (get-in rc [:admission :records-carrying])
                       (pr-str (get-in rc [:admission :reasons]))))
      (println (format "   selection: %s candidates per tick, no typed reason field"
                       (pr-str (get-in rc [:selection :candidates-per-tick])))))
    (println)
    (println "3. (a) THE CLAIM --" (:claim/id claim))
    (println "   chosen" (pr-str (get-in claim [:claim/chosen :action]))
             "controller-rank" (get-in claim [:claim/chosen :controller-rank]))
    (doseq [r (:claim/rejected claim)]
      (println (format "   rejected rank %d %-40s margin %.8f lost-on %s (share %.3f)"
                       (:controller-rank r) (pr-str (:action r)) (:margin/total r)
                       (name (:lost-on r)) (double (:lost-on-share r)))))
    (println "   soundness at mint:" (pr-str (:claim/soundness-at-mint claim)))
    (println "   the same projection over run" (:run/id false-rec) ":"
             (pr-str (:soundness (:section-3a-unsound-claim-at-mint art))))
    (println)
    (println "4. (b) THE VERDICT --" (:verdict/verdict verdict)
             (pr-str (:verdict/verdict-reason verdict)))
    (doseq [o (get-in verdict [:verdict/legs :overtake :overtaken-by])]
      (println (format "   %s: rank %d -> %d; margin %.8f -> %.8f"
                       (pr-str (:action o)) (:rank-at-claim o) (:rank-later o)
                       (:margin-at-claim o) (:margin-later o)))
      (println (format "     chosen mission-value-factor %s, non-progress-decay %s"
                       (pr-str (get-in o [:what-moved :chosen :mission-value-factor]))
                       (pr-str (get-in o [:what-moved :chosen :non-progress-decay]))))
      (println (format "     rival  mission-value-factor %s"
                       (pr-str (get-in o [:what-moved :rival :mission-value-factor]))))
      (println (format "     attribution: swing %.8f = chosen +%.8f / rival +%.8f; rival-alone-suffices? %s -> %s"
                       (get-in o [:attribution :swing])
                       (get-in o [:attribution :chosen-delta-G])
                       (get-in o [:attribution :rival-delta-G])
                       (get-in o [:attribution :rival-alone-suffices?])
                       (name (get-in o [:attribution :reading])))))
    (println "   c-mis leg:" (pr-str (get-in verdict [:verdict/legs :c-mis :movement])))
    (println "   receipts leg:" (pr-str (get-in verdict [:verdict/legs :receipts :reason])))
    (println)
    (println "5. THE PRIMITIVE REGRET SIGNAL")
    (doseq [d (remove nil? (:section-5-non-progress-census art))]
      (println (format "   %s  ticks %-3d candidate-rows %-6d decay %s  chosen non-progress-count %s  distinct missions %d  outcomes %d"
                       (:day d) (:ticks d) (:candidate-rows d) (pr-str (:decay-values d))
                       (pr-str (:chosen-non-progress-count d)) (:distinct-selected d)
                       (:records-with-an-outcome d))))
    (println)
    (println "6. (c) TENSION MINT")
    (println "  " (:tension/statement mint))
    (println "   record shape owner:" (:mint/record-shape-owner mint))
    (println "   library home:" (pr-str (:mint/library-home mint)))
    (println)
    (println "CONTROLS")
    (doseq [[k v] (:controls art)]
      (println (format "   %-32s %s" (name k) (if (:passes? v) "PASS" "FAIL")))))]
      (spit (io/file out-dir "U39-SELECTION-RETROSPECTIVE.txt") report)
      (print report)
      (println)
      (println "wrote" (str (io/file out-dir "u39-measurements.edn")))
      (println "wrote" (str (io/file out-dir "U39-SELECTION-RETROSPECTIVE.txt")))
      (when-not (every? :passes? (vals (:controls art)))
        (println "CONTROL FAILED")
        (System/exit 1)))))

;; ---------------------------------------------------------------------------
;; --deposit <run-id> -- one run-era ledger row (RE6)
;; ---------------------------------------------------------------------------
;;
;; The catalogue names THIS script as :rationale-regret's machinery, so this is
;; the path that deposits that check's rows. What it evaluates is exactly the
;; rule declared above in `retrospective-verdict` -- :declared-attributed-overtake
;; -- applied to the run's OWN consecutive records, read out of the trace in its
;; run store rather than out of the shared per-date corpus. No new rule, no new
;; scalar, and the design pass's own artifact is untouched.
;;
;; WHY THIS CHECK CANNOT DEPOSIT A GREEN ON TODAY'S CORPUS, stated here rather
;; than discovered by a reader: the declared rule has three outcomes and only
;; two are reachable. UPHELD requires an outcome leg -- a record joining the
;; selection to what it produced -- and `carrier-census` measures zero such
;; records. So a run with no refutation folds to a TYPED ABSENCE naming
;; :no-leg-measurable, never to a green. A green would assert the rationales
;; held up, which nothing in the corpus can witness.
;;
;;   some pair :rationale-refuted    -> :red  (regret, attributed: a candidate
;;                                             this run rejected out-ranked the
;;                                             chosen one on its own movement)
;;   no pair can decide               -> :typed-absence, with which leg failed
;;   fewer than two ranking records   -> :typed-absence: no pair to evaluate
;;
;; A pair is REFUSED AT MINT, and never scored, when the record contradicts the
;; claim projected from it: an unsound :selection-law (the design pass's own
;; refusal) or -- the one this row met -- a chosen action that is not the
;; G-minimal candidate, so the rivals the claim calls "rejected" were ahead of
;; it all along and `overtake` has nothing to measure. See `assertion-holds?`.
;;
;; A claim whose record's own :selection-law contradicts its ranking is UNSOUND
;; AT MINT and is not scored -- the same refusal the design pass records for the
;; 2026-09-02 false stamp -- and the receipt names it rather than dropping it.

(def deposit-receipt-dir "holes/labs/wm-contract/runs/RE6-check-deposits")

(defn run-store-dir [run-id]
  (io/file repo-root "holes/labs/wm-contract/runs" run-id))

(defn run-store-files [run-id]
  (let [d (run-store-dir run-id)]
    (when (.isDirectory d)
      (vec (sort (map #(.getName ^java.io.File %)
                      (filter #(.isFile ^java.io.File %) (.listFiles d))))))))

(defn run-tick-ids [run-id]
  (->> (or (run-store-files run-id) [])
       (keep #(second (re-matches #"tick-run-record-\d{4}-\d{2}-\d{2}-(.+)\.edn" %)))
       sort vec))

(defn run-trace-files [run-id]
  (->> (or (run-store-files run-id) [])
       (filter #(re-matches #"wm-trace.*\.edn" %))
       (mapv #(str (io/file (run-store-dir run-id) %)))))

(defn run-records
  "The run's own records, in tick order: the trace in its store, filtered to the
   ids its tick receipts name, so a shared trace file's foreign records cannot
   be scored against it."
  [run-id]
  (let [ids (set (run-tick-ids run-id))]
    (->> (mapcat read-trace (run-trace-files run-id))
         (filter #(contains? ids (:run/id %)))
         (sort-by :timestamp)
         vec)))

(defn run-observations
  "The realized outcomes this run OBSERVED, from `runs/<run-id>/observation/`,
   as a map keyed by the tick they are about. Empty for every run accepted
   before :U59 and for the first accepted step of any pin, and empty is the
   pre-:U59 behaviour exactly.

   A `:typed-absence` observation (there was no previous accepted step, or a
   leg could not be read) is NOT indexed: it names no tick, so it joins to
   nothing. It is still written, and the receipt cites it, because a store that
   merely lacked an observation would not say why."
  [run-id]
  (let [d (io/file (run-store-dir run-id) "observation")]
    (if-not (.isDirectory d)
      {}
      (into {}
            (keep (fn [^java.io.File f]
                    (let [o (edn/read-string read-opts (slurp f))]
                      (when-let [t (:observation/observed-for-tick o)] [t o])))
                  (sort-by #(.getName ^java.io.File %)
                           (filter #(and (.isFile ^java.io.File %)
                                         (str/ends-with? (.getName ^java.io.File %) ".edn"))
                                   (.listFiles d))))))))

(defn observed-runs
  "The run ids this run's observations are ABOUT -- the pair the observation
   names. `wm_step.sh` runs one tick per step, so a run's own records never
   hold two ranking-carrying records and this check deposited `:typed-absence`
   by construction (C511-repair-or-elaborate.md section 3). The pairing is not
   inferred here: the observation record says which run it observed, and it
   says so because the pin said so (`:pin/accepted-steps`)."
  [observations]
  (vec (distinct (keep :observation/observed-for-run (vals observations)))))

(defn paired-records
  "The records this run is scored over: the runs its observations name, in
   accepted order, then its own. With no observation this is exactly
   `run-records`, so nothing about an already-deposited run moves."
  [run-id observations]
  (into (vec (mapcat run-records (observed-runs observations)))
        (run-records run-id)))

(defn assertion-holds?
  "Does the record support the claim's own assertion -- that the chosen action's
   G-core is BELOW each listed rejected candidate's, by :margin/total?

   This is a precondition of the declared rule and not a second rule: `overtake`
   asks whether a rejected candidate later out-ranked the chosen one, and on a
   record where the rivals were ALREADY ahead the question is vacuous -- they
   never were behind, so nothing overtook anything, and `rival-alone-suffices?`
   fires on any rival movement at all because the margin it must close is
   negative. A claim like that is refused here, exactly as an unsound law is,
   rather than scored into a refutation the record does not carry."
  [claim]
  (let [margins (mapv :margin/total (:claim/rejected claim))]
    {:rejected-considered (count margins)
     :min-margin (when (seq margins) (apply min margins))
     :chosen-controller-rank (get-in claim [:claim/chosen :controller-rank])
     :holds? (and (seq margins) (every? pos? margins))}))

(defn pair-verdicts
  "One verdict per consecutive pair of ranking-carrying records, by the rule
   declared in `retrospective-verdict`. A pair whose earlier claim is unsound at
   mint, or whose assertion the record contradicts, is refused at mint instead
   of scored."
  [era u42 top-k source-file observations]
  (vec (for [[claim-rec later-rec] (partition 2 1 era)
             :let [claim (rationale-claim claim-rec source-file 0 top-k)
                   sound? (= :sound (:status (:claim/soundness-at-mint claim)))
                   asrt (assertion-holds? claim)]]
         (cond
           (not sound?)
           {:claim-id (:claim/id claim)
            :from (:run/id claim-rec) :to (:run/id later-rec)
            :at (:timestamp claim-rec)
            :verdict :refused-at-mint
            :reason (:reason (:claim/soundness-at-mint claim))
            :soundness (:claim/soundness-at-mint claim)}

           (not (:holds? asrt))
           {:claim-id (:claim/id claim)
            :from (:run/id claim-rec) :to (:run/id later-rec)
            :at (:timestamp claim-rec)
            :verdict :refused-at-mint
            :reason :claim-assertion-contradicted-by-the-record
            :chosen (get-in claim [:claim/chosen :action])
            :assertion asrt}

           :else
           (let [v (retrospective-verdict claim claim-rec later-rec u42 top-k observations)]
             ;; The outcome keys are carried ONLY when this run observed
             ;; something, and that placement is the whole care here: adding a
             ;; key to the receipt of an already-deposited run turns its replay
             ;; from :already-present into the append-only ledger's divergence
             ;; refusal (run_era_ledger.bb:235-245, the trap :U58 named). With
             ;; no observation the receipt is byte-identical to the committed
             ;; one, and the negative controls assert exactly that.
             (cond-> {:claim-id (:claim/id claim)
              :from (:run/id claim-rec) :to (:run/id later-rec)
              :at (:timestamp claim-rec)
              :verdict (:verdict/verdict v)
              :reason (:verdict/verdict-reason v)
              :chosen (get-in claim [:claim/chosen :action])
              :overtaken-by (mapv #(assoc (select-keys % [:action :rank-at-claim :rank-later
                                                          :margin-at-claim :margin-later])
                                          :reading (get-in % [:attribution :reading])
                                          :rival-delta-G (get-in % [:attribution :rival-delta-G])
                                          :chosen-delta-G (get-in % [:attribution :chosen-delta-G]))
                                  (get-in v [:verdict/legs :overtake :overtaken-by]))
              :assertion asrt
              :legs-measurable
              {:overtake true
               :c-mis (get-in v [:verdict/legs :c-mis :movement])
               :receipts (get-in v [:verdict/legs :receipts :status])}}
               (seq observations)
               (assoc :outcome-leg (get-in v [:verdict/legs :outcome]))))))))

(defn deposit-receipt [run-id]
  (let [observations (run-observations run-id)
        observed (observed-runs observations)
        records (paired-records run-id observations)
        era (filterv has-ranking? records)
        u42 (edn/read-string (slurp (io/file repo-root "holes/labs/wm-contract/runs/U42-producers/measurements.edn")))
        source-file (first (mapv #(str/replace-first % (str repo-root "/") "")
                                 (run-trace-files run-id)))
        verdicts (if (< (count era) 2) [] (pair-verdicts era u42 5 source-file observations))
        tally (into (sorted-map) (frequencies (map :verdict verdicts)))
        refuted (filterv #(= :rationale-refuted (:verdict %)) verdicts)
        upheld (filterv #(= :rationale-upheld (:verdict %)) verdicts)]
    ;; array-map, not a literal: a map literal of this size is a hash-map and
    ;; would print in hash order, so the receipt would not be stable to read.
    (array-map
     :schema :wm/run-era-deposit-receipt-v1
     :row :RE6
     :check :rationale-regret
     :run-id run-id
     :produced-by "holes/labs/wm-contract/u39_selection_retrospective.bb --deposit"
     :deterministic
     (str "No wall-clock field. Every number is read from a record or subtracted from two of "
          "them, so this receipt is rewritten byte-identically on every deposit. That is what "
          "lets the deposit require it to be committed and unmodified, and lets the same deposit "
          "repeat as :already-present.")
     :rule (cond-> {:name :declared-attributed-overtake
            :declared-at "holes/labs/wm-contract/u39_selection_retrospective.bb (retrospective-verdict)"
            :statement (str "REFUTED iff some candidate this claim rejected later out-ranked the "
                            "chosen one AND its own recorded movement alone would have closed the "
                            "margin the claim asserted. An overtake explained by the chosen "
                            "candidate's own non-progress decay measures the decay, not the "
                            "rationale, and is UNTESTABLE.")
            :upheld-unreachable
            (str "UPHELD requires an outcome leg and no record in this corpus carries one, so "
                 "this check cannot deposit a green from the trace alone.")}
             (seq observations)
             (assoc :upheld-reached-how
                    (str "UPHELD is reachable on this run: the outcome leg is an OBSERVATION taken "
                         "at this accepted step about the previous one -- the chosen mission's own "
                         ":open-hole-count on the two records -- and a :grounded-change with no "
                         "refutation upholds. The clause above still describes the trace alone, "
                         "which carries no outcome on any record: what changed is that the check "
                         "no longer reads the trace alone.")))
     :run-store (cond-> {:dir (str "holes/labs/wm-contract/runs/" run-id)
                         :holds (run-store-files run-id)
                         :tick-ids (run-tick-ids run-id)
                         :traces (mapv #(str/replace-first % (str repo-root "/") "") (run-trace-files run-id))
                         :records-of-this-run (count records)
                         :records-carrying-a-ranking (count era)
                         :pairs-evaluated (count verdicts)}
                  (seq observed)
                  (assoc :paired-with
                         {:runs observed
                          :why (str "wm_step.sh runs one tick per step, so a run's own records never "
                                    "hold two ranking-carrying records and this check deposited "
                                    ":typed-absence by construction. The pair is not inferred here: "
                                    "this run's observation record names the run it observed, and it "
                                    "names it because the pin's :pin/accepted-steps ordered them.")
                          :records (mapv #(vector % (count (run-records %))) observed)}))
     :verdict-deposited (cond (< (count era) 2) :typed-absence
                              (seq refuted) :red
                              (and (seq verdicts) (= (count upheld) (count verdicts))) :green
                              :else :typed-absence)
     :pair-verdicts verdicts
     :tally tally
     :outcome-side
     (cond-> {:records-with-a-trace-outcome (count (filter trace-outcome records))
              :records-with-realized-outcome (count (filter :realized-outcome records))
              :why-it-matters "the UPHELD branch needs one of these; the count is what makes its absence a measurement"}
       ;; Carried only when this run observed something, so the receipt of a run
       ;; accepted before :U59 is byte-identical to its committed one and its
       ;; replayed deposit stays :already-present rather than divergent.
       (seq observations)
       (assoc :observed-outcomes
              {:count (count observations)
               :for-ticks (vec (sort (keys observations)))
               :outcomes (into (sorted-map) (frequencies (map :outcome (vals observations))))
               :dir (str "holes/labs/wm-contract/runs/" run-id "/observation")
               :produced-by "holes/labs/wm-contract/wm_step_observe.bb, run by wm_step.sh accept"
               :note (str "the outcome of a decision does not exist when the decision is written, "
                          "so it is not a key on the record: it is observed at the NEXT accepted "
                          "step and joined by :run/id")}))
     :not-what-this-says
     (str "A :typed-absence here does NOT say the run's rationales held up. It says no leg of the "
          "declared rule was measurable on this run's records: no rejected candidate overtook the "
          "chosen one on its own movement, and there is no outcome carrier that could uphold the "
          "claim instead."))))

(defn deposit-notes [run-id r]
  (let [store (:run-store r)
        refuted (filterv #(= :rationale-refuted (:verdict %)) (:pair-verdicts r))]
    (case (:verdict-deposited r)
      :red
      (str "REGRET, attributed: " (count refuted) " of " (:pairs-evaluated store)
           " consecutive-tick pairs in this run are :rationale-refuted by the declared rule "
           ":declared-attributed-overtake -- a candidate the tick rejected out-ranked the chosen "
           "one at the next tick, and the rival's own recorded movement alone would have closed "
           "the margin. "
           (str/join "; " (for [v refuted
                                :let [o (first (:overtaken-by v))]]
                            (str (:from v) " chose " (pr-str (:chosen v)) " and "
                                 (pr-str (:action o)) " went rank " (:rank-at-claim o) " -> "
                                 (:rank-later o) " with rival-delta-G " (:rival-delta-G o))))
           ". Tally " (pr-str (:tally r)) " over " (:records-carrying-a-ranking store)
           " records carrying a ranking.")
      :typed-absence
      (let [refused (filterv #(= :refused-at-mint (:verdict %)) (:pair-verdicts r))]
        (cond
          (< (:records-carrying-a-ranking store) 2)
          (str "runs/" run-id "/ holds fewer than two records carrying a controller ranking ("
               (:records-carrying-a-ranking store) " of " (:records-of-this-run store)
               " records of this run, from " (pr-str (:traces store))
               "), and the declared rule evaluates a claim against the NEXT tick, so there is no "
               "pair to evaluate and no rationale-regret verdict about this run exists.")

          (= (count refused) (:pairs-evaluated store))
          (str "all " (:pairs-evaluated store) " consecutive-tick pairs of this run are REFUSED AT "
               "MINT, tally " (pr-str (:tally r))
               ", so no rationale of this run is scorable by the declared rule. The reason is a "
               "property of the run and is measured on its own records: the projected claim "
               "asserts the chosen action's G-core is BELOW each listed rejected candidate's, and "
               "on these records it is not -- "
               (str/join "; " (for [v refused]
                                (str (:from v) " chose " (pr-str (:chosen v))
                                     " at controller rank "
                                     (get-in v [:assertion :chosen-controller-rank])
                                     " with the top-5 margin reaching "
                                     (get-in v [:assertion :min-margin]))))
               ". The chosen action was not the G-minimal candidate, so its rivals were ahead "
               "from the start and the rule's overtake question is vacuous: nothing overtook "
               "anything, and the margin a rival would have had to close is negative. Scoring it "
               "would produce a refutation the record does not carry. What this run needs for a "
               "rationale-regret verdict is a claim projected from the selector that actually "
               "chose, not from the controller ranking it did not follow.")

          :else
          (let [obs (get-in r [:outcome-side :observed-outcomes])]
            (str "no pair of this run's " (:pairs-evaluated store) " can decide, tally "
                 (pr-str (:tally r))
                 ". No rejected candidate out-ranked the chosen one on its own movement, so the "
                 "refutation leg does not fire; and "
                 (if obs
                   ;; The absence has a DIFFERENT reason once an observation
                   ;; exists, and saying the old one would be false: UPHELD is
                   ;; reachable here and did not fire, which is not the same
                   ;; finding as UPHELD having no way to fire.
                   (str "the rule's other outcome, UPHELD, WAS REACHABLE on this run and did not "
                        "fire: " (:count obs) " observed outcome(s) "
                        (pr-str (:outcomes obs)) " over the pair this run's observation names ("
                        (pr-str (get-in r [:run-store :paired-with :runs]))
                        "), and UPHELD requires a :grounded-change -- strictly fewer open holes on "
                        "the chosen mission at this step than at the one that chose it. The "
                        "observation is a measurement of the outcome, not of its absence: what is "
                        "absent is a change for it to report. ")
                   (str "the rule's other outcome, UPHELD, is unreachable "
                        "on this corpus because it needs an outcome leg -- this run's records carry "
                        (get-in r [:outcome-side :records-with-a-trace-outcome])
                        " trace outcomes and " (get-in r [:outcome-side :records-with-realized-outcome])
                        " realized outcomes. "))
                 "So the check ran, both legs are typed, and neither can decide: "
                 "that is an absence with a reason, not a green."))))
      :green
      (let [obs (get-in r [:outcome-side :observed-outcomes])]
        (str "UPHELD, on an observed outcome: all " (:pairs-evaluated store)
             " pair(s) of this run are :rationale-upheld by the declared rule "
             ":declared-attributed-overtake -- no candidate the tick rejected closed the recorded "
             "margin on its own movement, and the chosen action's outcome was OBSERVED to be a "
             ":grounded-change. The observation is not read off the record: a decision's outcome "
             "does not exist when the decision is written, so this run's accept observed the "
             "PREVIOUS accepted run (" (pr-str (get-in r [:run-store :paired-with :runs]))
             ") and joined it by :run/id -- " (:count obs) " observation(s), outcomes "
             (pr-str (:outcomes obs)) ", in " (:dir obs) ". Tally " (pr-str (:tally r))
             " over " (:records-carrying-a-ranking store) " records carrying a ranking, paired "
             "across the two runs.")))))

(defn deposit! [run-id]
  (let [r (deposit-receipt run-id)
        rel (str deposit-receipt-dir "/rationale-regret-" run-id ".edn")
        path (io/file repo-root rel)]
    (io/make-parents path)
    (spit path (with-out-str (pp/pprint r)))
    (println "u39_selection_retrospective --deposit: receipt" rel)
    (let [{:keys [exit out err]}
          (process/shell {:dir repo-root :out :string :err :string :continue true}
                         "bb" "holes/labs/wm-contract/run_era_ledger.bb" "--deposit"
                         "--run-id" run-id
                         "--check-id" ":rationale-regret"
                         "--verdict" (str (:verdict-deposited r))
                         "--artifact" rel
                         "--author" "u39_selection_retrospective.bb --deposit"
                         "--deposited-by" "RE6 -- wire the four remaining catalogued checks"
                         "--notes" (deposit-notes run-id r))]
      (print out) (print err) (flush)
      (when-not (zero? exit)
        (println (format "u39_selection_retrospective --deposit: the ledger refused the row (exit %d)" exit))
        (println "  if the refusal is artifact-untracked or artifact-dirty, commit" rel "and re-run")
        (System/exit 1))
      (System/exit 0))))

(if-let [run-id (second (drop-while #(not= "--deposit" %) *command-line-args*))]
  (deposit! run-id)
  (if (some #{"--deposit"} *command-line-args*)
    (do (println "u39_selection_retrospective --deposit needs a run-id") (System/exit 1))
    (-main)))
