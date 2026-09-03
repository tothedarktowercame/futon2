#!/usr/bin/env bb
;; U39 -- SELECTION-RATIONALE RETROSPECTIVE, design pass.
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

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[clojure.pprint :as pp])

(def repo-root (str (System/getProperty "user.home") "/code/futon2"))
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
(defn trace-outcome
  "The three places `war-machine/trace-outcome` looks (war_machine.clj:2328-2332)."
  [m]
  (or (:outcome m) (get-in m [:enactment :outcome]) (get-in m [:realized-outcome :outcome])))

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

(defn retrospective-verdict
  "(b) The record. The verdict vocabulary is closed; the rule that assigns it is
   DECLARED here and is the thing the registry entry registers as a free hand."
  [claim claim-rec later-rec u42 top-k]
  (let [ot (overtake claim-rec later-rec top-k)
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
      :receipts {:status :absent
                 :reason :no-mission-to-receipt-carrier
                 :would-need "a carrier joining a commit/receipt to the mission that was held; U23 measured the two candidates -- flight-discharge :writer-exists-no-records (zero *.flight.edn under ~/code) and clocked-on, which records WHO clocked on and not WHAT landed"
                 :basis "runs/U23-cascade-catalog/carrier-population.edn"}}
     :verdict/overtake-readings readings
     :verdict/verdict
     (cond own-regret? :rationale-refuted
           overtaken? :rationale-untestable
           :else :rationale-untestable)
     :verdict/verdict-reason
     (cond own-regret? :a-rejected-candidate-closed-the-recorded-margin-on-its-own-movement
           overtaken? :overtake-attributable-to-the-chosen-candidates-own-non-progress-decay
           :else :no-leg-measurable)
     :verdict/verdict-rule
     {:name :declared-attributed-overtake
      :status :declared-not-ruled
      :scalars :none
      :statement "REFUTED iff some candidate this claim rejected later out-ranked the chosen one AND its own recorded movement alone would have closed the margin the claim asserted. An overtake that only happens because the chosen candidate was decayed for having been chosen measures the decay, not the rationale, and is UNTESTABLE. UPHELD requires an outcome leg, which no record in this corpus carries."
      :alternatives-not-taken
      {:overtake-dominant "any overtake refutes -- rejected here because on the recorded 09-02 pair it refutes every claim after one tick, since the chosen mission's mission-value-factor halves by construction"
       :weighted-legs "combine the four legs with declared weights -- rejected here because three of the four legs are typed absences on this corpus, so the weights would be unmeasurable"}}
     :verdict/basis
     ["[:ranked-actions] of both records" "[:decision :controller-ranking] of both records"
      "runs/U42-producers/measurements.edn" "runs/U23-cascade-catalog/carrier-population.edn"]}))

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

(-main)
