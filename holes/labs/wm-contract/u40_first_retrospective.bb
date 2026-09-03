#!/usr/bin/env bb
;; U40 -- THE FIRST RETROSPECTIVE, replayed over recorded history.
;;
;; READ-ONLY. Reads the recorded wm-trace corpus, the committed U39 and U42
;; artifacts, and git history (`git show`, `git log`) for the receipts leg;
;; writes only under holes/labs/wm-contract/runs/U40-first-retrospective/.
;; No tick, no run lock, no substrate call, no network, nothing under data/ is
;; written or touched, no weight is changed anywhere.
;;
;; WHAT IT PRODUCES. The three cases worklist row U40 names, each as a U39
;; (a)-claim, a U39 (b)-verdict and its evidence pointers; the receipts leg that
;; U39 typed :no-mission-to-receipt-carrier, now measured on the one carrier that
;; DECLARES its mission; the (c) tension records for the refuted cases; and the
;; typed summary of what the machine would have had to know to choose otherwise.
;;
;; SHAPES. The (a)/(b)/(c) projections are U39's, re-implemented here rather than
;; shared, because U39 is :done-unreviewed and its producer is left untouched for
;; its reviewer. Control C1 is what keeps the two from drifting: it re-derives
;; U39's own worked claim and verdict with this file's code and requires equality
;; with the committed u39-measurements.edn. Consolidating the two copies is
;; follow-up work for whoever signs U39.
;;
;; DETERMINISM. No wall-clock field is written. The window boundaries are
;; resolved from the corpus timestamps and pinned into the artifact by sha, and
;; every git query is over a closed past window, so two runs over an unchanged
;; repository are byte-identical.

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.java.shell :refer [sh]]
         '[clojure.string :as str]
         '[clojure.pprint :as pp])

(def repo-root (str (System/getProperty "user.home") "/code/futon2"))
(def out-dir (io/file repo-root "holes/labs/wm-contract/runs/U40-first-retrospective"))

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
(defn trace-outcome
  "The three places `war-machine/trace-outcome` looks (war_machine.clj:2328-2332)."
  [m]
  (or (:outcome m) (get-in m [:enactment :outcome]) (get-in m [:realized-outcome :outcome])))

(defn action-key [a] [(:type a) (:target a)])
(defn num- [x] (double (or x 0.0)))

(defn by-target [m]
  (into {} (map (fn [e] [(action-key (:action e)) e])) (:ranked-actions m)))

(defn ranking-index [m]
  (into {} (map (fn [e] [(action-key (:action e)) (:rank e)])) (ranking m)))

;; ---------------------------------------------------------------------------
;; 0. THE ENACTMENT PATH. Before any rationale can be scored, one question has to
;;    be answered from the records: did the ranking that carries the rationale
;;    decide anything? The selector is named on every record
;;    (`[:decision :selected-policy-id]`), and the candidate set the
;;    scheduler-habit selector draws from is a hardcoded three-element set of
;;    mission ids at war_machine.clj:6235-6238, recorded per tick at
;;    `[:decision :strategic-memory :counterfactuals :scheduler-habit]`.

(def strategic-candidate-ids
  "war_machine.clj:6235-6238 -- read from the source, not invented here."
  #{"M-aif-policy-conditioned-eig"
    "M-shared-memory-control-build-test"
    "M-wm-aif-policy-grain-compliance"})

(defn enactment-path [era]
  (let [chosen-target #(get-in % [:decision :action :target])
        head-target #(get-in % [:decision :controller-ranking 0 :action :target])
        habit-list #(get-in % [:decision :strategic-memory :counterfactuals :scheduler-habit])
        controller-order-restricted
        (fn [m] (let [idx (into {} (map (fn [e] [(get-in e [:action :target]) (:rank e)]))
                                (ranking m))]
                  (vec (sort-by idx (filter idx strategic-candidate-ids)))))]
    {:ticks (count era)
     :selector-ids (frequencies (map #(get-in % [:decision :selected-policy-id]) era))
     :chosen-is-controller-head
     (frequencies (map #(= (chosen-target %) (head-target %)) era))
     :chosen-is-in-the-hardcoded-three
     (frequencies (map #(contains? strategic-candidate-ids (chosen-target %)) era))
     :scheduler-habit-list-is-the-controller-order-restricted-to-the-three
     (frequencies (map #(= (vec (habit-list %)) (controller-order-restricted %)) era))
     :chosen-is-the-head-of-that-list
     (frequencies (map #(= (first (habit-list %)) (chosen-target %)) era))
     :per-tick
     (mapv (fn [m] {:at (:timestamp m) :run-id (:run/id m)
                    :selector (get-in m [:decision :selected-policy-id])
                    :chosen (chosen-target m)
                    :controller-head (head-target m)
                    :chosen-in-hardcoded-three? (contains? strategic-candidate-ids (chosen-target m))})
           era)
     :source
     {:hardcoded-set "scripts/futon2/report/war_machine.clj:6235-6238 strategic-candidate-ids"
      :filter "war_machine.clj:6239-6245 -- wm-admissible keep'd to those ids, in admissible order"
      :handoff "war_machine.clj:6246-6251 -- that list, and the controller ranking, are what invoke-strategic-selection is given"
      :enactment "war_machine.clj:6253-6260 -- the enacted action is the first admissible action matching :selected-mission-ids"
      :set-age "the set has stood since futon2 fa61e98 (2026-07-24) -- git log -S strategic-candidate-ids"}}))

;; ---------------------------------------------------------------------------
;; 1. (a) THE RATIONALE-AS-CLAIM -- U39 section 3, re-implemented, pinned by C1.

(def term-keys
  [:G-risk :G-ambiguity :G-goal-outcome :controller-augmentation
   :structural-pressure :graph-feasibility-penalty :gap-exploration-bonus])

(defn margin-decomposition [win lose]
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

(defn claim-soundness [m]
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

(defn rationale-claim [m file line top-k]
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
;; 2. (b) THE RETROSPECTIVE VERDICT -- U39 section 4, re-implemented, pinned by C1.

(defn overtake [claim-rec later-rec top-k]
  (let [chosen (action-key (get-in claim-rec [:decision :action]))
        claim-ra (by-target claim-rec)
        later-idx (ranking-index later-rec)
        later-ra (by-target later-rec)
        chosen-rank-later (get later-idx chosen)
        losers (->> (:ranked-actions claim-rec)
                    (remove #(= (action-key (:action %)) chosen))
                    (sort-by :rank)
                    (take top-k))]
    {:chosen (vec chosen)
     :chosen-rank-at-claim (get (ranking-index claim-rec) chosen)
     :chosen-rank-later chosen-rank-later
     :overtaken-by
     (vec (for [l losers
                :let [k (action-key (:action l))
                      rank-later (get later-idx k)
                      e-later (get later-ra k)
                      e-claim (get claim-ra k)]
                :when (and rank-later chosen-rank-later (< rank-later chosen-rank-later))]
            (let [chosen-claim (get claim-ra chosen)
                  chosen-entry-later (get later-ra chosen)
                  m0 (- (num- (:G-core e-claim)) (num- (:G-core chosen-claim)))
                  m1 (- (num- (:G-core e-later)) (num- (:G-core chosen-entry-later)))
                  chosen-delta (- (num- (:G-core chosen-entry-later)) (num- (:G-core chosen-claim)))
                  rival-delta (- (num- (:G-core e-claim)) (num- (:G-core e-later)))
                  decay-fell? (< (num- (get-in chosen-entry-later [:action :non-progress-decay]))
                                 (num- (get-in chosen-claim [:action :non-progress-decay])))
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
                         [(get-in claim-ra [chosen :action :mission-value-factor])
                          (get-in later-ra [chosen :action :mission-value-factor])]
                         :non-progress-decay
                         [(get-in claim-ra [chosen :action :non-progress-decay])
                          (get-in later-ra [chosen :action :non-progress-decay])]
                         :non-progress-count
                         [(get-in claim-ra [chosen :action :non-progress-count])
                          (get-in later-ra [chosen :action :non-progress-count])]}
                :rival {:mission-value-factor
                        [(get-in e-claim [:action :mission-value-factor])
                         (get-in e-later [:action :mission-value-factor])]
                        :non-progress-decay
                        [(get-in e-claim [:action :non-progress-decay])
                         (get-in e-later [:action :non-progress-decay])]}}})))}))

(defn c-mis-leg [u42 run-id]
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

(def u39-verdict-rule
  {:name :declared-attributed-overtake
   :status :declared-not-ruled
   :scalars :none
   :statement "REFUTED iff some candidate this claim rejected later out-ranked the chosen one AND its own recorded movement alone would have closed the margin the claim asserted. An overtake that only happens because the chosen candidate was decayed for having been chosen measures the decay, not the rationale, and is UNTESTABLE. UPHELD requires an outcome leg, which no record in this corpus carries."
   :alternatives-not-taken
   {:overtake-dominant "any overtake refutes -- rejected here because on the recorded 09-02 pair it refutes every claim after one tick, since the chosen mission's mission-value-factor halves by construction"
    :weighted-legs "combine the four legs with declared weights -- rejected here because three of the four legs are typed absences on this corpus, so the weights would be unmeasurable"}
   :owner "U39 section 4 -- declared there, used verbatim here"})

(defn retrospective-verdict
  "U39's (b) record, used verbatim so the U40 verdicts are the same objects U39
   declared. `later-rec` may be nil: a claim minted on the last record of the
   corpus has no evaluation record and the overtake leg says so rather than
   returning an empty result that reads like a non-firing test."
  [claim claim-rec later-rec u42 top-k]
  (let [ot (when later-rec (overtake claim-rec later-rec top-k))
        overtaken? (boolean (seq (:overtaken-by ot)))
        readings (frequencies (map #(get-in % [:attribution :reading]) (:overtaken-by ot)))
        own-regret? (pos? (get readings :rival-improved-enough-on-its-own 0))
        unsound? (= :unsound (get-in claim [:claim/soundness-at-mint :status]))
        cm-before (c-mis-leg u42 (:run/id claim-rec))
        cm-after (when later-rec (c-mis-leg u42 (:run/id later-rec)))
        np (if later-rec
             (let [chosen (action-key (get-in claim-rec [:decision :action]))
                   e (get (by-target later-rec) chosen)]
               {:status :measured
                :non-progress? (get-in e [:action :non-progress?])
                :non-progress-count (get-in e [:action :non-progress-count])
                :non-progress-decay (get-in e [:action :non-progress-decay])
                :basis "war_machine.clj:2312-2326 previous-selection-non-progress?, :2339-2356 recent-non-progress-count, :2290 non-progress-decay-k"})
             {:status :absent :reason :no-later-ranking-record-in-corpus})]
    {:verdict/claim-id (:claim/id claim)
     :verdict/evaluated-against (if later-rec
                                  {:run-id (:run/id later-rec) :at (:timestamp later-rec)}
                                  {:status :absent :reason :no-later-ranking-record-in-corpus})
     :verdict/window (if later-rec
                       {:from (:timestamp claim-rec) :to (:timestamp later-rec) :ticks-between 1}
                       {:from (:timestamp claim-rec) :to :none :ticks-between 0})
     :verdict/legs
     {:overtake (if later-rec
                  (assoc ot :status :measured :fired? overtaken?)
                  {:status :absent :reason :no-later-ranking-record-in-corpus
                   :detail "the claim is minted on the last record the corpus carries; the machine has not ticked since"})
      :c-mis {:at-claim cm-before :at-evaluation cm-after
              :movement (if (and (= :measured (:status cm-before))
                                 (= :measured (:status cm-after)))
                          :comparable
                          {:status :untestable
                           :reason :c-mis-reads-the-selected-mission-only
                           :detail "the readback is keyed to the tick's SELECTED mission, so two ticks that select different missions produce two different subjects and no movement"})}
      :non-progress np
      :receipts {:status :measured-outside-the-trace
                 :reason :carrier-is-a-board-ledger-not-a-trace-field
                 :see :section-4-receipts
                 :basis "U39 typed this leg :no-mission-to-receipt-carrier at trace grain; section 4 of this artifact measures it on the one carrier that DECLARES the mission it serves"}}
     :verdict/overtake-readings readings
     :verdict/verdict
     (cond unsound? :rationale-untestable
           own-regret? :rationale-refuted
           :else :rationale-untestable)
     :verdict/verdict-reason
     (cond unsound? :claim-unsound-at-mint
           own-regret? :a-rejected-candidate-closed-the-recorded-margin-on-its-own-movement
           overtaken? :overtake-attributable-to-the-chosen-candidates-own-non-progress-decay
           (nil? later-rec) :no-later-ranking-record-in-corpus
           :else :no-leg-measurable)
     :verdict/verdict-rule u39-verdict-rule
     :verdict/basis
     (cond-> ["[:ranked-actions] of the claim record"
              "[:decision :controller-ranking] of the claim record"
              "runs/U42-producers/measurements.edn"
              "runs/U23-cascade-catalog/carrier-population.edn"]
       later-rec (conj "[:ranked-actions] and [:decision :controller-ranking] of the evaluation record"))}))

;; ---------------------------------------------------------------------------
;; 3. (c) THE TENSION MINT -- U39 section 6, re-implemented, pinned by C1.

(defn tension-mint [claim verdict claim-rec]
  (let [ot (get-in verdict [:verdict/legs :overtake])
        rival (or (first (filter #(= :rival-improved-enough-on-its-own
                                     (get-in % [:attribution :reading]))
                                 (:overtaken-by ot)))
                  (first (:overtaken-by ot)))
        chosen (get-in claim [:claim/chosen :action])
        rej (first (filter #(= (:action %) (:action rival)) (:claim/rejected claim)))]
    (assert (some? rej) "tension mint needs the rejected entry the overtake names")
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
      :minted-by :u40-first-retrospective
      :written-to :nothing}
     :mint/record-shape-owner "DESIGN-tensions-as-patterns.md section 3 (U41 implements)"
     :mint/library-home
     {:carrier :flexiarg
      :root "futon3/library (pattern_registry.clj:48-53)"
      :addressed-by "pattern_registry.clj:158-161 candidate-pattern-path"
      :status :named-not-written
      :note "no flexiarg is written by this row; the birth rule in DESIGN-tensions-as-patterns.md section 2 requires >=2 typed receipts and a person"}}))

;; ---------------------------------------------------------------------------
;; 4. CASE 3 -- the decay events, and the rule that scores them.
;;
;;    A decay event is not a rejection rationale, so U39's rule does not reach
;;    it. The rule below is DECLARED here in the same shape and with the same
;;    status: it is not a ruling, and `:choices` is not written.

(def decay-family-keys
  "The fields the decay mechanism itself writes: its own multiplier, the factor
   it multiplies, and the count and predicate behind it."
  #{:non-progress-decay :mission-value-factor :non-progress-count :non-progress?})

(def decay-verdict-rule
  {:name :declared-next-tick-reversal
   :status :declared-not-ruled
   :scalars :none
   :statement "A decay event asserts that the mission selected on the previous tick made no grounded change and is worth less for it. REFUTED iff the machine itself re-selects that mission on the very next tick AND every recorded field that changed on the mission in between belongs to the decay mechanism -- so the reversal is the decay's own expiry and nothing learned about the mission. UNTESTABLE where the corpus carries no reading of the mission's grounded change either way. UPHELD requires a receipt showing no change in the decay window on a declared carrier."
   :alternatives-not-taken
   {:decay-is-a-rotation-schedule "read the decay as a rotation policy that asserts nothing about progress, in which case nothing can refute it -- not taken here because the field is named :non-progress? and is computed by previous-selection-non-progress? (war_machine.clj:2312-2326), which is a predicate about change"
    :receipts-decide "let board-ledger receipts inside each decay window decide -- not taken here because the recorded windows are 3 and 13 minutes wide and only one of the three missions the corpus selects has a declared carrier"}})

(defn action-of [m target]
  (some #(when (= target (get-in % [:action :target])) (:action %)) (:ranked-actions m)))

(defn decay-events
  "One record per candidate carrying a decay below 1.0, in tick order, with the
   sequel the rule needs: what the machine chose on the next tick, and which of
   the mission's own recorded fields changed in between."
  [era]
  (vec (for [[i m] (map-indexed vector era)
             e (:ranked-actions m)
             :let [d (get-in e [:action :non-progress-decay])]
             :when (and d (< (num- d) 1.0))]
         (let [target (get-in e [:action :target])
               nxt (when (< (inc i) (count era)) (nth era (inc i)))
               a0 (:action e)
               a1 (when nxt (action-of nxt target))
               changed (when a1
                         (vec (sort (remove #(= (get a0 %) (get a1 %))
                                            (distinct (concat (keys a0) (keys a1)))))))]
           {:tick-index i
            :at (:timestamp m)
            :run-id (:run/id m)
            :decayed target
            :decay d
            :non-progress-count (get-in e [:action :non-progress-count])
            :previous-selection (when (pos? i) (get-in (nth era (dec i)) [:decision :action :target]))
            :decayed-is-the-previous-selection?
            (and (pos? i) (= target (get-in (nth era (dec i)) [:decision :action :target])))
            :chosen-this-tick (get-in m [:decision :action :target])
            :next-tick (when nxt {:run-id (:run/id nxt) :at (:timestamp nxt)
                                  :chosen (get-in nxt [:decision :action :target])})
            :re-selected-next-tick? (boolean (and nxt (= target (get-in nxt [:decision :action :target]))))
            :fields-that-changed-before-re-selection changed
            :only-the-decay-mechanism-changed?
            (when changed (every? decay-family-keys changed))}))))

(defn decay-claim
  "The (a)-shape for a decay event family. The subject is the mechanism, not one
   tick: U39 section 6 refuses to mint the same fact N times, and a decay event
   is the same fact 94 times."
  [evs era]
  (let [with-next (filterv :next-tick evs)
        reselected (filterv :re-selected-next-tick? with-next)]
    {:claim/id "rc-decay-family-2026-08-30..2026-09-02"
     :claim/at (:timestamp (first era))
     :claim/source {:producer "scripts/futon2/report/war_machine.clj:2312-2326 previous-selection-non-progress?"
                    :count-walk "war_machine.clj:2339-2356 recent-non-progress-count"
                    :multiplier "war_machine.clj:2290 non-progress-decay-k"
                    :corpus "data/wm-trace, the 94 ranking-era records"}
     :claim/assertion
     "the mission selected on the previous tick made no grounded change, and is worth :non-progress-decay times its mission value for it"
     :claim/soundness-at-mint
     (let [outcome-bearing (count (filter trace-outcome era))]
       (if (zero? outcome-bearing)
         {:status :sound-but-unobservable
          :reason :the-assertions-own-test-has-no-readable-input
          :detail "the :grounded-change test reads trace-outcome, and 0 of the era's records reach any of the three places it looks (war_machine.clj:2328-2332)"
          :records-with-any-trace-outcome 0}
         {:status :sound :records-with-any-trace-outcome outcome-bearing}))
     :claim/events (count evs)
     :claim/ticks (count era)
     :claim/one-event-per-tick? (= (count evs) (count era))
     :claim/decayed-is-the-previous-selection
     (frequencies (map :decayed-is-the-previous-selection? evs))
     :claim/decay-values (frequencies (map :decay evs))
     :claim/non-progress-counts (frequencies (map :non-progress-count evs))
     :claim/events-with-a-next-tick (count with-next)
     :claim/re-selected-on-the-next-tick (count reselected)
     :claim/new-logging-required :none}))

(defn decay-verdict [claim evs era u42]
  (let [with-next (filterv :next-tick evs)
        reselected (filterv :re-selected-next-tick? with-next)
        only-decay (filterv :only-the-decay-mechanism-changed? reselected)
        refuted? (and (seq reselected) (= (count only-decay) (count reselected)))]
    {:verdict/claim-id (:claim/id claim)
     :verdict/evaluated-against {:corpus "data/wm-trace" :ticks (count era)
                                 :window {:from (:timestamp (first era)) :to (:timestamp (last era))}}
     :verdict/legs
     {:reversal
      {:status :measured
       :events-with-a-next-tick (count with-next)
       :re-selected-on-the-next-tick (count reselected)
       :of-those-only-the-decay-mechanism-changed (count only-decay)
       :changed-field-sets (frequencies (map :fields-that-changed-before-re-selection reselected))
       :not-re-selected (mapv #(select-keys % [:at :decayed :chosen-this-tick])
                              (remove :re-selected-next-tick? with-next))}
      :own-test
      {:status :absent
       :reason :the-assertions-own-test-has-no-readable-input
       :records-with-any-trace-outcome (count (filter trace-outcome era))
       :basis "war_machine.clj:2328-2332 trace-outcome; U39 section 1 counted the same 0 over the whole 885-record corpus"}
      :c-mis
      {:status :absent
       :reason :c-mis-reads-the-selected-mission-only
       :measured-runs (count (filter #(= :measured (get-in % [:readback :status])) (:rows u42)))
       :basis "runs/U42-producers/measurements.edn"}
      :receipts {:status :measured-outside-the-trace :see :section-4-receipts}}
     :verdict/verdict (if refuted? :rationale-refuted :rationale-untestable)
     :verdict/verdict-reason
     (if refuted?
       :every-recorded-reversal-is-the-decays-own-expiry-and-nothing-else
       :no-leg-measurable)
     :verdict/verdict-rule decay-verdict-rule
     :verdict/basis
     ["[:ranked-actions <candidate> :action] of every era record"
      "[:decision :action :target] of every era record"
      "scripts/futon2/report/war_machine.clj:2290,2312-2326,2328-2332,2339-2356"]}))

(defn decay-tension-mint [claim verdict]
  (let [rev (get-in verdict [:verdict/legs :reversal])]
    {:mint/from :refuted-rationale
     :mint/verdict-id (:verdict/claim-id verdict)
     :tension/born-of :refuted-rationale
     :tension/poles ["penalise the mission just selected, so attention rotates off it"
                     "return to the mission just selected, because nothing recorded about it changed"]
     :tension/statement
     (format "Over the %d ranking-era ticks the machine recorded %d non-progress decay events, one per tick, %s of them on the mission it had selected on the previous tick. Of the %d events with a next tick, %d were followed by the machine re-selecting the decayed mission, and on %d of those %d the only recorded fields that changed on the mission in between were the decay mechanism's own (%s). The decay expresses a one-tick rotation, not a durable judgement, and its own test -- :grounded-change via trace-outcome -- has no readable input on any of the %d records."
             (:claim/ticks claim)
             (:claim/events claim)
             (get (:claim/decayed-is-the-previous-selection claim) true)
             (:events-with-a-next-tick rev)
             (:re-selected-on-the-next-tick rev)
             (:of-those-only-the-decay-mechanism-changed rev)
             (:re-selected-on-the-next-tick rev)
             (str/join ", " (map name (sort decay-family-keys)))
             (:claim/ticks claim))
     :tension/carried-by :the-selection-mechanism-itself
     :tension/resolution-path :none-named-at-mint
     :tension/status :carried
     :tension/provenance
     {:claim (:claim/id claim)
      :verdict-against "the 94-record ranking era, data/wm-trace 2026-08-30..2026-09-02"
      :records [(get-in claim [:claim/source :corpus])]
      :minted-by :u40-first-retrospective
      :written-to :nothing}
     :mint/one-record-for-the-family
     "U39 section 6: only a REFUTED verdict mints, and a decay refutation is one fact, not 94 -- so the family mints one record and the per-event table stays in the measurements artifact"
     :mint/record-shape-owner "DESIGN-tensions-as-patterns.md section 3 (U41 implements)"
     :mint/library-home
     {:carrier :flexiarg
      :root "futon3/library (pattern_registry.clj:48-53)"
      :addressed-by "pattern_registry.clj:158-161 candidate-pattern-path"
      :status :named-not-written
      :note "no flexiarg is written by this row; the birth rule in DESIGN-tensions-as-patterns.md section 2 requires >=2 typed receipts and a person"}}))

;; ---------------------------------------------------------------------------
;; 5. THE RECEIPTS LEG. U39 typed it :no-mission-to-receipt-carrier because no
;;    TRACE field joins a receipt to a mission. The row names a carrier that is
;;    not a trace field: the boards' ledgers. A board is a declared carrier for a
;;    mission only if the ledger itself says so, in its own :mission key -- that
;;    is read, never inferred. Everything else is reported as a LEXICAL reading
;;    and typed as one.

(defn git [& args]
  (let [r (apply sh (concat ["git" "-C" repo-root] args))]
    (when-not (zero? (:exit r))
      (throw (ex-info "git failed" {:args args :err (:err r)})))
    (str/trim-newline (:out r))))

(defn git-in [dir & args]
  (let [r (apply sh (concat ["git" "-C" dir] args))]
    (if (zero? (:exit r)) (str/trim-newline (:out r)) ::unavailable)))

(defn sha-before [ts path]
  (let [s (git "log" "-1" (str "--before=" ts) "--format=%H %cI" "--" path)]
    (when (seq s)
      (let [[sha at] (str/split s #" ")] {:sha sha :at at}))))

(defn ledger-at [sha path]
  (let [x (edn/read-string read-opts (git "show" (str sha ":" path)))]
    {:mission (:mission x)
     :statuses (into {} (map (juxt :id :status)) (or (:items x) (:rows x)))
     :rows (or (:items x) (:rows x))}))

(def terminal-status #{:done :done-unreviewed})

(defn board-window [path from-ts to-ts]
  (let [a (sha-before from-ts path)
        b (sha-before to-ts path)
        l0 (ledger-at (:sha a) path)
        l1 (ledger-at (:sha b) path)
        s0 (:statuses l0)
        s1 (:statuses l1)]
    {:board path
     :declares-mission (:mission l1)
     :window {:from {:ts from-ts :resolved a} :to {:ts to-ts :resolved b}}
     :rows-at-window-open (count s0)
     :rows-at-window-close (count s1)
     :rows-minted-in-window (count (remove (set (keys s0)) (keys s1)))
     :rows-reaching-a-terminal-status
     (count (filter (fn [[k v]] (and (terminal-status v) (not (terminal-status (get s0 k))))) s1))
     :rows-reaching-reviewed-done
     (count (filter (fn [[k v]] (and (= :done v) (not= :done (get s0 k)))) s1))
     :ledger-commits-in-window
     (count (remove str/blank? (str/split-lines (git "log" (str "--since=" from-ts) (str "--until=" to-ts) "--format=%H" "--" path))))
     :status-at-close (frequencies (vals s1))}))

(defn lexical-attribution
  "A LEXICAL reading, and typed as one: which rows that reached a terminal status
   in the window mention a mission by name anywhere in their own text. It is
   evidence about wording, not a declared carrier binding."
  [path from-ts to-ts patterns]
  (let [a (sha-before from-ts path)
        b (sha-before to-ts path)
        s0 (:statuses (ledger-at (:sha a) path))
        rows (:rows (ledger-at (:sha b) path))
        newly (filter #(and (terminal-status (:status %)) (not (terminal-status (get s0 (:id %))))) rows)
        text #(str/lower-case (str (:statement %) " " (:acceptance %) " " (:evidence %)))]
    {:board path
     :reading :lexical-not-declared
     :rows-reaching-a-terminal-status (count newly)
     :mentions (into {} (for [[k re] patterns]
                          [k (mapv :id (filter #(re-find re (text %)) newly))]))}))

(defn repo-activity [dir from-ts to-ts]
  (let [n (git-in dir "log" (str "--since=" from-ts) (str "--until=" to-ts) "--format=%H")
        last-commit (git-in dir "log" "-1" "--format=%H %cI")]
    (if (= ::unavailable n)
      {:repository dir :status :not-a-readable-git-repository}
      {:repository dir
       :commits-in-window (count (remove str/blank? (str/split-lines n)))
       :last-commit-in-repository last-commit
       :attributable? :no
       :note "repository activity is not mission attribution; it is reported because a count of zero is a receipt of absence and a count above zero is not a receipt of presence"})))

(defn receipts [from-ts to-ts]
  (let [zaif "holes/labs/zaif-harness/worklist.edn"
        wm "holes/labs/wm-contract/worklist.edn"
        pats {:zaif #"zaif" :eoi #"expressions-of-interest|m-eoi"}]
    {:window {:from from-ts :to to-ts :hours 24}
     :carrier-rule
     {:declared "a board ledger is a declared receipt carrier for a mission iff the ledger's own :mission key names that mission's document"
      :read-from "the :mission key of each worklist.edn at the window-close sha"
      :everything-else :lexical-not-declared}
     :boards [(board-window zaif from-ts to-ts) (board-window wm from-ts to-ts)]
     :lexical [(lexical-attribution wm from-ts to-ts pats)]
     :mission-documents
     {"M-zaif-harness-v1"
      {:document "futon2/holes/missions/M-zaif-harness-v1.md"
       :declared-by "holes/labs/zaif-harness/worklist.edn :mission"
       :declared-board zaif}
      "M-expressions-of-interest"
      {:document "futon5a/holes/missions/M-expressions-of-interest.md"
       :declared-by "futon2/holes/core-mission-gaps.edn:607-609 :stem/:file"
       :declared-board :none-found
       :repository (repo-activity (str (System/getProperty "user.home") "/code/futon5a") from-ts to-ts)}
      "M-wm-aif-policy-grain-compliance"
      {:document "futon2/holes/missions/M-wm-aif-policy-grain-compliance.md"
       :declared-board :none-found
       :note "the wm-contract ledger declares no :mission -- its :source is TN-edge-review-aif-wiring.md -- so its rows are not a declared receipt for any mission"}}}))

;; ---------------------------------------------------------------------------
;; Controls.

(def verdict-keys-that-must-agree-with-u39
  "The verdict fields U40 may not move: the identity of what was scored, the
   measured legs, and the verdict itself. The three keys left out are the three
   this row deliberately changes, and C1 enumerates them rather than ignoring
   them."
  [:verdict/claim-id :verdict/evaluated-against :verdict/window
   :verdict/overtake-readings :verdict/verdict :verdict/verdict-reason])

(defn u39-shape-pin
  "C1. The (a) claim must reproduce U39's committed claim exactly. The (b)
   verdict must reproduce every measured leg and the verdict itself exactly; the
   keys that differ must be exactly the three this row declares it changes."
  [u39 claim verdict]
  (let [u39-claim (:section-3a-rationale-claim u39)
        u39-verdict (:section-4b-retrospective-verdict u39)
        legs [:overtake :c-mis :non-progress]
        legs-equal (into {} (for [l legs]
                              [l (= (get-in u39-verdict [:verdict/legs l])
                                    (get-in verdict [:verdict/legs l]))]))
        scored-equal (into {} (for [k verdict-keys-that-must-agree-with-u39]
                                [k (= (get u39-verdict k) (get verdict k))]))
        differing (vec (sort (remove #(= (get u39-verdict %) (get verdict %))
                                     (distinct (concat (keys u39-verdict) (keys verdict))))))
        declared-differences [:verdict/basis :verdict/legs :verdict/verdict-rule]]
    {:asks "this file's (a) and (b) projections reproduce U39's committed worked claim, its measured legs and its verdict exactly, and differ from it only where this row declares it changes them"
     :against "runs/U39-selection-retrospective/u39-measurements.edn"
     :claim-equal? (= u39-claim claim)
     :measured-legs-equal legs-equal
     :scored-fields-equal scored-equal
     :keys-that-differ differing
     :differences-declared
     {:verdict/legs "the :receipts leg: U39 typed it :no-mission-to-receipt-carrier at trace grain; U40 types it :measured-outside-the-trace and points at section 4. The three measured legs are byte-equal."
      :verdict/verdict-rule "an :owner key naming U39 as the rule's author; the :name, :status, :scalars, :statement and :alternatives-not-taken are unchanged"
      :verdict/basis "U40 splits U39's 'of both records' into a claim-record entry and an evaluation-record entry, because U40 also scores a claim that has no evaluation record"}
     :passes? (and (= u39-claim claim)
                   (every? true? (vals legs-equal))
                   (every? true? (vals scored-equal))
                   (= differing declared-differences))}))

(defn margin-identity
  "C2. Every rejection margin in every claim minted here reproduces the recorded
   controller-score difference of the claim's OWN record."
  [claims-and-records]
  (let [deltas (for [[claim rec] claims-and-records
                     :let [win (get (by-target rec) (action-key (get-in rec [:decision :action])))]
                     r (:claim/rejected claim)
                     :let [e (get (by-target rec) (action-key (:action r)))]]
                 {:claim (:claim/id claim)
                  :rejected (:target (:action r))
                  :delta (Math/abs (- (:margin/total r)
                                      (- (num- (:controller-score e)) (num- (:controller-score win)))))})]
    {:asks "every :margin/total equals the recorded controller-score difference on the claim's own record"
     :claims (vec (distinct (map :claim deltas)))
     :margins-checked (count deltas)
     :max-abs-delta (if (seq deltas) (apply max (map :delta deltas)) 0.0)
     :passes? (every? #(< (:delta %) 1.0e-12) deltas)}))

(defn controls [u39 era claims-and-records pin-claim pin-verdict claim-1 verdict-1 decay-evs]
  {:c1-u39-shape-pin (u39-shape-pin u39 pin-claim pin-verdict)
   :c2-margin-identity (margin-identity claims-and-records)
   :c3-fabricated-subject
   {:asks "a mission id that appears in no record yields no claim, no verdict and no decay event"
    :subject "M-not-a-mission-u40-control"
    :appearances-in-era
    (count (filter (fn [m] (some #(= "M-not-a-mission-u40-control" (get-in % [:action :target]))
                                 (:ranked-actions m)))
                   era))
    :appearances-in-claims
    (count (for [[claim _] claims-and-records
                 r (:claim/rejected claim)
                 :when (= "M-not-a-mission-u40-control" (:target (:action r)))]
             r))
    :appearances-in-decay-events
    (count (filter #(= "M-not-a-mission-u40-control" (:decayed %)) decay-evs))
    :passes? (and (zero? (count (filter (fn [m] (some #(= "M-not-a-mission-u40-control"
                                                          (get-in % [:action :target]))
                                                      (:ranked-actions m)))
                                        era)))
                  (zero? (count (filter #(= "M-not-a-mission-u40-control" (:decayed %)) decay-evs))))}
   :c4-decay-rule-negative
   ;; The decay rule must not fire on an event whose mission was NOT re-selected
   ;; on the next tick. Scored against the events the corpus actually carries.
   (let [not-re (remove :re-selected-next-tick? (filter :next-tick decay-evs))]
     {:asks "the reversal leg counts only events where the decayed mission is re-selected on the very next tick"
      :events-not-re-selected (count not-re)
      :any-counted? (boolean (some :re-selected-next-tick? not-re))
      :passes? (not (some :re-selected-next-tick? not-re))})
   :c5-enacted-is-not-the-rationale
   ;; The premise of case 1, stated as a control so it cannot pass silently:
   ;; the record whose law says :chosen-rank 1 must have its chosen action
   ;; somewhere other than rank 1 of its own controller ranking.
   {:asks "case 1's record contradicts itself on its own ranking"
    :law-says (get-in claim-1 [:claim/law :chosen-rank])
    :ranking-says (get-in claim-1 [:claim/chosen :controller-rank])
    :soundness (get-in claim-1 [:claim/soundness-at-mint :status])
    :verdict (:verdict/verdict verdict-1)
    :passes? (and (= :unsound (get-in claim-1 [:claim/soundness-at-mint :status]))
                  (not= (get-in claim-1 [:claim/law :chosen-rank])
                        (get-in claim-1 [:claim/chosen :controller-rank])))}})

;; ---------------------------------------------------------------------------

(defn -main []
  (let [files (trace-files)
        days (mapv (fn [f] {:day (day-of f) :file f :records (read-trace f)}) files)
        era-days (filterv #(some has-ranking? (:records %)) days)
        era (vec (mapcat #(filterv has-ranking? (:records %)) era-days))
        u42 (edn/read-string (slurp (io/file repo-root "holes/labs/wm-contract/runs/U42-producers/measurements.edn")))
        u39 (edn/read-string (slurp (io/file repo-root "holes/labs/wm-contract/runs/U39-selection-retrospective/u39-measurements.edn")))
        d0902 (first (filter #(= "2026-09-02" (:day %)) days))
        recs (:records d0902)
        trace-file "data/wm-trace/wm-trace-2026-09-02.edn"
        rec-0 (nth recs 0)                    ; 0a18c4f7 -- the S4 punch-in record
        rec-1 (nth recs 1)                    ; 4abad68c -- selected M-zaif-harness-v1
        rec-2 (nth recs 2)                    ; 801976e7 -- selected M-expressions-of-interest

        ;; CASE 1 -- the punch-in record. Its claim is refused at mint.
        claim-1 (rationale-claim rec-0 trace-file 1 3)
        verdict-1 (retrospective-verdict claim-1 rec-0 rec-1 u42 5)

        ;; The 14:00 record, which is U39's worked example. Re-derived here so C1
        ;; can pin the shapes, and scored because it is one of the three 09-02
        ;; rationales the row asks for.
        claim-mid (rationale-claim rec-1 trace-file 2 3)
        verdict-mid (retrospective-verdict claim-mid rec-1 rec-2 u42 5)
        mint-mid (tension-mint claim-mid verdict-mid rec-1)

        ;; CASE 2 -- the M-eoi-over-zaif preference, the last record in the corpus.
        claim-2 (rationale-claim rec-2 trace-file 3 5)
        verdict-2 (retrospective-verdict claim-2 rec-2 nil u42 5)

        ;; CASE 3 -- the decay family.
        d-evs (decay-events era)
        d-claim (decay-claim d-evs era)
        d-verdict (decay-verdict d-claim d-evs era u42)
        d-mint (decay-tension-mint d-claim d-verdict)

        window-from (:timestamp rec-2)
        window-to (str/replace window-from #"^2026-09-02" "2026-09-03")
        rcpt (receipts window-from window-to)

        ctl (controls u39 era
                      [[claim-1 rec-0] [claim-mid rec-1] [claim-2 rec-2]]
                      claim-mid verdict-mid claim-1 verdict-1 d-evs)

        zaif-board (first (filter #(= "holes/labs/zaif-harness/worklist.edn" (:board %)) (:boards rcpt)))
        ep (enactment-path era)

        summary
        [{:case :case-1
          :subject (:claim/id claim-1)
          :the-rationale-as-recorded "[:decision :selection-law] says :chosen-rank 1, :moved-from-controller-head? false"
          :what-the-record-says (str "the chosen action sits at controller rank "
                                     (get-in claim-1 [:claim/chosen :controller-rank])
                                     " of " (count (ranking rec-0)))
          :verdict (:verdict/verdict verdict-1)
          :what-would-have-had-to-be-known
          {:answer :nothing-about-the-missions
           :because :the-preferred-candidate-was-not-in-the-selectors-candidate-set
           :measured {:selector (get-in rec-0 [:decision :selected-policy-id])
                      :candidate-set (vec (sort strategic-candidate-ids))
                      :controller-head (get-in rec-0 [:decision :controller-ranking 0 :action :target])
                      :controller-head-in-candidate-set?
                      (contains? strategic-candidate-ids
                                 (get-in rec-0 [:decision :controller-ranking 0 :action :target]))
                      :enacted (get-in rec-0 [:decision :action :target])
                      :enacted-is-the-head-of-the-recorded-scheduler-habit-list?
                      (= (get-in rec-0 [:decision :action :target])
                         (first (get-in rec-0 [:decision :strategic-memory :counterfactuals :scheduler-habit])))}
           :pointer "scripts/futon2/report/war_machine.clj:6235-6238"
           :what-would-have-had-to-change :the-candidate-set-or-the-selector-and-both-are-code}}
         {:case :case-2
          :subject (:claim/id claim-2)
          :the-rationale-as-recorded "M-expressions-of-interest at controller rank 1, M-zaif-harness-v1 rejected at rank 5"
          :verdict (:verdict/verdict verdict-2)
          :verdict-reason (:verdict/verdict-reason verdict-2)
          :what-would-have-had-to-be-known
          {:answer :one-more-tick
           :because :the-declared-rule-scores-a-claim-against-the-next-ranking-record-and-the-machine-has-not-ticked-since
           :measured {:claim-at (:timestamp rec-2)
                      :later-ranking-records-in-corpus 0
                      :last-trace-file (day-of (:file (last days)))}}
          :the-leg-that-is-measurable
          {:leg :receipts
           :rule :declared-carrier-only
           :zaif {:declared-board (:board zaif-board)
                  :declares (:declares-mission zaif-board)
                  :rows-reaching-reviewed-done (:rows-reaching-reviewed-done zaif-board)
                  :rows-minted (:rows-minted-in-window zaif-board)
                  :ledger-commits (:ledger-commits-in-window zaif-board)}
           :eoi {:declared-board :none-found
                 :repository-commits-in-window
                 (get-in rcpt [:mission-documents "M-expressions-of-interest" :repository :commits-in-window])
                 :last-commit-in-that-repository
                 (get-in rcpt [:mission-documents "M-expressions-of-interest" :repository :last-commit-in-repository])}
           :status :measured-not-ruled
           :free-hand "whether a receipts asymmetry may decide a verdict is not decided here; U39's rule has no receipts leg and this row writes no ruling. The choice-set lives at aif-equations.edn :choices :selection-retrospective (:status :observed-not-decided) and is left as U39 registered it."}}
         {:case :case-3
          :subject (:claim/id d-claim)
          :the-rationale-as-recorded "the mission selected on the previous tick made no grounded change"
          :verdict (:verdict/verdict d-verdict)
          :verdict-reason (:verdict/verdict-reason d-verdict)
          :what-would-have-had-to-be-known
          {:answer :whether-anything-had-happened
           :because :the-assertions-own-test-reads-a-field-no-record-carries
           :measured {:events (:claim/events d-claim)
                      :ticks (:claim/ticks d-claim)
                      :records-with-any-trace-outcome
                      (get-in d-verdict [:verdict/legs :own-test :records-with-any-trace-outcome])
                      :re-selected-on-the-next-tick
                      (get-in d-verdict [:verdict/legs :reversal :re-selected-on-the-next-tick])
                      :of-those-only-the-decay-mechanism-changed
                      (get-in d-verdict [:verdict/legs :reversal :of-those-only-the-decay-mechanism-changed])}
           :pointer "scripts/futon2/report/war_machine.clj:2312-2326, :2328-2332"
           :what-would-have-had-to-change :a-producer-writing-an-outcome-onto-the-tick-record}}
         {:case :across-all-three
          :subject :the-enactment-path
          :measured {:ticks (:ticks ep)
                     :chosen-is-controller-head (:chosen-is-controller-head ep)
                     :chosen-is-in-the-hardcoded-three (:chosen-is-in-the-hardcoded-three ep)
                     :selectors (:selector-ids ep)}
          :statement "on 92 of the 94 ranking-era ticks the controller's ranking was computed in full, recorded in full, and not consulted: the enacted mission is one of three hardcoded ids and is not the controller head. The two exceptions are the 14:00 and 14:03 records of 2026-09-02, after the selector id changes from stub:first-ranked-authorized-mission to stub:controller-head. A rationale recorded on any of the other 92 ticks is a rationale for a choice that was not made."
          :pointer "scripts/futon2/report/war_machine.clj:6236-6262; [:decision :selected-policy-id] on every record"}]

        art {:row :U40
             :generated-by "holes/labs/wm-contract/u40_first_retrospective.bb"
             :read-only true
             :builds-on {:row :U39
                         :shapes ["(a) rationale-as-claim" "(b) retrospective-verdict" "(c) tension mint"]
                         :artifact "runs/U39-selection-retrospective/u39-measurements.edn"
                         :pinned-by :c1-u39-shape-pin}
             :corpus {:dir "data/wm-trace"
                      :files (count files)
                      :records (reduce + (map (comp count :records) days))
                      :ranking-era-days (mapv :day era-days)
                      :ranking-era-records (count era)}
             :section-0-enactment-path ep
             :section-1-case-1
             {:row-says "the controller preferred M-eoi/M-zaif variants while the stub took the scheduler-habit head (run 0a18c4f7, a FALSE selection-law stamp already documented)"
              :corroborated
              {:controller-head (get-in rec-0 [:decision :controller-ranking 0 :action :target])
               :controller-rank-2 (get-in rec-0 [:decision :controller-ranking 1 :action :target])
               :enacted (get-in rec-0 [:decision :action :target])
               :enacted-controller-rank (get-in claim-1 [:claim/chosen :controller-rank])
               :recorded-scheduler-habit-list (get-in rec-0 [:decision :strategic-memory :counterfactuals :scheduler-habit])
               :enacted-is-its-head? (= (get-in rec-0 [:decision :action :target])
                                        (first (get-in rec-0 [:decision :strategic-memory :counterfactuals :scheduler-habit])))
               :selector (get-in rec-0 [:decision :selected-policy-id])
               :selector-seam (get-in rec-0 [:decision :strategic-memory :provenance])
               :pointer "[:decision :strategic-memory :counterfactuals :scheduler-habit] of run 0a18c4f7; the list is produced at war_machine.clj:6239-6245 from the hardcoded set at :6235-6238"}
              :corrected
              {:finding :two-different-channels-are-called-scheduler-habit-on-this-record
               :channel-a {:field "[:decision :counterfactual]"
                           :kind (get-in rec-0 [:decision :counterfactual :kind])
                           :winner (get-in rec-0 [:decision :counterfactual :winner :action :target])
                           :produced-by "src/futon2/aif/policy.clj:646"
                           :basis :habit-prior-bias}
               :channel-b {:field "[:decision :strategic-memory :counterfactuals :scheduler-habit]"
                           :head (first (get-in rec-0 [:decision :strategic-memory :counterfactuals :scheduler-habit]))
                           :produced-by "scripts/futon2/report/war_machine.clj:6239-6245"
                           :basis :controller-order-restricted-to-a-hardcoded-three}
               :note "the row's phrase 'the scheduler-habit head' is true of channel B and false of channel A; the enacted mission is rank 125 of the habit-adjusted ranking that channel A orders. Recorded as a measured collision of one name over two channels, not as a ruling."}
              :claim claim-1
              :verdict verdict-1
              :mint :none
              :mint-reason "U39 section 3: a claim whose record contradicts its own ranking is refused at mint, and a refused claim is not scored and does not mint"}
             :section-1b-the-14-00-record
             {:note "the middle of the three 2026-09-02 rationales. This is U39's worked example; it is re-derived here, pinned by C1, and scored because the row asks for the 2026-09-02 rationales."
              :claim claim-mid
              :verdict verdict-mid
              :mint mint-mid}
             :section-2-case-2
             {:row-says "the controller's M-eoi-over-zaif preference vs what actually happened -- zaif-harness received 30+ reviewed rows via operator-driven work in the following 24h while M-eoi received the lexical probe and criteria only"
              :claim claim-2
              :verdict verdict-2
              :mint :none
              :mint-reason "only a REFUTED verdict mints (U39 section 6); the declared rule types this claim untestable for want of a later ranking record"
              :row-figure-checked
              {:row-says "30+ reviewed rows"
               :declared-carrier-reading (:rows-reaching-reviewed-done zaif-board)
               :declared-plus-lexical-reading
               (+ (:rows-reaching-reviewed-done zaif-board)
                  (count (get-in (first (:lexical rcpt)) [:mentions :zaif])))
               :verdict-on-the-figure
               "27 under the declared-carrier rule, 45 if the 18 wm-contract rows that mention zaif in their own text are added; the row's figure is reproduced only under the second, lexical reading, and that reading is not a declared carrier binding"}}
             :section-3-case-3
             {:row-says "the non-progress decay events on record"
              :claim d-claim
              :verdict d-verdict
              :events d-evs
              :mint d-mint}
             :section-4-receipts rcpt
             :section-5-tension-records [mint-mid d-mint]
             :section-6-summary-for-joe summary
             :section-7-what-this-row-did-not-do
             ["no ruling: aif-equations.edn :choices and control-map-edges.edn :decisions are untouched"
              "no live tick, no run lock, no substrate call, no network"
              "nothing under src/ or scripts/futon2/ changed; no weight, threshold or decay constant moved"
              "no tension record written to futon3/library -- the mint names the home and stops there"
              "no gen_aif_dag.bb run into a publish (TN section 9a)"]
             :controls ctl}]
    (.mkdirs out-dir)
    (spit (io/file out-dir "u40-measurements.edn") (with-out-str (pp/pprint art)))
    (spit (io/file out-dir "tension-records.edn")
          (with-out-str (pp/pprint {:row :U40
                                    :minted-by "holes/labs/wm-contract/u40_first_retrospective.bb"
                                    :shape-owner "U39 section 6 mint edge; DESIGN-tensions-as-patterns.md section 3 owns the record (U41 implements)"
                                    :written-to :nothing
                                    :records [mint-mid d-mint]})))
    (let [report
          (with-out-str
            (println "U40 -- the first retrospective, replayed over recorded history")
            (println "=============================================================")
            (println)
            (println (format "corpus: %d files, %d records; ranking era %s (%d records)"
                             (count files) (reduce + (map (comp count :records) days))
                             (str/join ", " (map :day era-days)) (count era)))
            (println)
            (println "0. THE ENACTMENT PATH")
            (println "   selectors:                    " (pr-str (:selector-ids ep)))
            (println "   chosen == controller head:    " (pr-str (:chosen-is-controller-head ep)))
            (println "   chosen in the hardcoded three:" (pr-str (:chosen-is-in-the-hardcoded-three ep)))
            (println "   scheduler-habit list == controller order restricted to the three:"
                     (pr-str (:scheduler-habit-list-is-the-controller-order-restricted-to-the-three ep)))
            (println "   chosen == head of that list:  " (pr-str (:chosen-is-the-head-of-that-list ep)))
            (println "   set:" (get-in ep [:source :hardcoded-set]))
            (println)
            (println "1. CASE 1 --" (:claim/id claim-1))
            (println "   controller head:" (get-in rec-0 [:decision :controller-ranking 0 :action :target])
                     "| rank 2:" (get-in rec-0 [:decision :controller-ranking 1 :action :target]))
            (println "   enacted:" (get-in rec-0 [:decision :action :target])
                     "at controller rank" (get-in claim-1 [:claim/chosen :controller-rank])
                     "| selector" (pr-str (get-in rec-0 [:decision :selected-policy-id])))
            (println "   recorded scheduler-habit list:" (pr-str (get-in rec-0 [:decision :strategic-memory :counterfactuals :scheduler-habit])))
            (println "   soundness at mint:" (pr-str (:claim/soundness-at-mint claim-1)))
            (println "   VERDICT:" (:verdict/verdict verdict-1) (pr-str (:verdict/verdict-reason verdict-1)) "-- mint: none")
            (println)
            (println "1b. THE 14:00 RECORD --" (:claim/id claim-mid) "(U39's worked example, re-derived and pinned)")
            (doseq [r (:claim/rejected claim-mid)]
              (println (format "   rejected rank %d %-40s margin %.8f lost-on %s"
                               (:controller-rank r) (pr-str (:action r)) (:margin/total r) (name (:lost-on r)))))
            (println "   VERDICT:" (:verdict/verdict verdict-mid) (pr-str (:verdict/verdict-reason verdict-mid)))
            (println)
            (println "2. CASE 2 --" (:claim/id claim-2))
            (println "   chosen" (pr-str (get-in claim-2 [:claim/chosen :action])) "at controller rank"
                     (get-in claim-2 [:claim/chosen :controller-rank]))
            (doseq [r (:claim/rejected claim-2)]
              (println (format "   rejected rank %d %-40s margin %.8f lost-on %s"
                               (:controller-rank r) (pr-str (:action r)) (:margin/total r) (name (:lost-on r)))))
            (println "   VERDICT:" (:verdict/verdict verdict-2) (pr-str (:verdict/verdict-reason verdict-2)) "-- mint: none")
            (println (format "   receipts, declared carrier only: %s -> %d rows reviewed-done, %d minted, %d ledger commits"
                             (:declares-mission zaif-board) (:rows-reaching-reviewed-done zaif-board)
                             (:rows-minted-in-window zaif-board) (:ledger-commits-in-window zaif-board)))
            (println (format "   receipts, M-expressions-of-interest: no declared board; its document's repository took %s commits in the window (last commit %s)"
                             (get-in rcpt [:mission-documents "M-expressions-of-interest" :repository :commits-in-window])
                             (get-in rcpt [:mission-documents "M-expressions-of-interest" :repository :last-commit-in-repository])))
            (println (format "   the row's '30+': %d declared, %d declared+lexical"
                             (:rows-reaching-reviewed-done zaif-board)
                             (+ (:rows-reaching-reviewed-done zaif-board)
                                (count (get-in (first (:lexical rcpt)) [:mentions :zaif])))))
            (println)
            (println "3. CASE 3 --" (:claim/id d-claim))
            (println (format "   %d decay events over %d ticks; %s of them on the previous selection"
                             (:claim/events d-claim) (:claim/ticks d-claim)
                             (pr-str (:claim/decayed-is-the-previous-selection d-claim))))
            (println (format "   %d events have a next tick; %d were followed by the decayed mission being re-selected"
                             (:claim/events-with-a-next-tick d-claim) (:claim/re-selected-on-the-next-tick d-claim)))
            (println (format "   of those, %d changed nothing but the decay mechanism's own fields"
                             (get-in d-verdict [:verdict/legs :reversal :of-those-only-the-decay-mechanism-changed])))
            (println (format "   the assertion's own test: %d of %d era records carry any trace-outcome"
                             (get-in d-verdict [:verdict/legs :own-test :records-with-any-trace-outcome]) (count era)))
            (println "   VERDICT:" (:verdict/verdict d-verdict) (pr-str (:verdict/verdict-reason d-verdict)))
            (println)
            (println "4. RECEIPTS (24h window" window-from "->" window-to ")")
            (doseq [b (:boards rcpt)]
              (println (format "   %-45s declares %s | minted %d, reviewed-done %d, terminal %d, commits %d"
                               (:board b) (pr-str (:declares-mission b))
                               (:rows-minted-in-window b) (:rows-reaching-reviewed-done b)
                               (:rows-reaching-a-terminal-status b) (:ledger-commits-in-window b))))
            (doseq [l (:lexical rcpt)]
              (println (format "   lexical (NOT a declared carrier) on %s: zaif %d rows, eoi %d rows, eoi-without-zaif %d"
                               (:board l) (count (get-in l [:mentions :zaif])) (count (get-in l [:mentions :eoi]))
                               (count (remove (set (get-in l [:mentions :zaif])) (get-in l [:mentions :eoi]))))))
            (println)
            (println "5. TENSION RECORDS MINTED:" (count [mint-mid d-mint]))
            (doseq [t [mint-mid d-mint]]
              (println "  -" (:tension/statement t)))
            (println)
            (println "6. WHAT THE MACHINE WOULD HAVE HAD TO KNOW")
            (doseq [s summary]
              (println (format "   %-16s %s"
                               (name (:case s))
                               (or (get-in s [:what-would-have-had-to-be-known :answer])
                                   (:subject s)))))
            (println)
            (println "CONTROLS")
            (doseq [[k v] ctl]
              (println (format "   %-32s %s" (name k) (if (:passes? v) "PASS" "FAIL")))))]
      (spit (io/file out-dir "U40-FIRST-RETROSPECTIVE.txt") report)
      (print report)
      (println)
      (println "wrote" (str (io/file out-dir "u40-measurements.edn")))
      (println "wrote" (str (io/file out-dir "tension-records.edn")))
      (println "wrote" (str (io/file out-dir "U40-FIRST-RETROSPECTIVE.txt")))
      (when-not (every? :passes? (vals ctl))
        (println "CONTROL FAILED")
        (System/exit 1)))))

(-main)
