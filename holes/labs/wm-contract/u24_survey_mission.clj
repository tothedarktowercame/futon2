#!/usr/bin/env clojure
;; U24 -- the :survey-mission epistemic action, on a STAGED field, with the
;; U4 ambiguity measurement re-run as the before/after.
;;
;;   clojure -M holes/labs/wm-contract/u24_survey_mission.clj [outdir]
;;
;; WHAT THIS ROW OWES (worklist :U24, ported from zaif-harness :S6 half (b)):
;; the action type constructible and selectable in a STAGED field with its G
;; terms recorded; one planted demonstration where an IDENTIFY-phase mission
;; generates a survey flight; the U4 ambiguity sweep re-run as the before/after
;; measurement of whether ambiguity finally discriminates; default off.
;;
;; THE SHIPPED PATH IS WHAT IS MEASURED. Every score comes from
;; `futon2.aif.efe/rank-actions` and every selection from
;; `futon2.aif.policy/select-action` -- the two functions the live judge calls
;; (`scripts/futon2/report/war_machine.clj:6033-6047`). Nothing is recomputed by
;; hand.
;;
;; THE FIELD IS PINNED TO A COMMITTED RECORD, NOT RE-ENUMERATED. The 146
;; candidates are the `:action` maps of the 146 `:ranked-actions` of
;; `data/wm-trace/wm-trace-2026-09-02.edn` line 2 -- the tick whose
;; `:decision` selected M-zaif-harness-v1 -- together with that record's own
;; `:observation` and `:mu-pre`. Control A reproduces the record's own
;; `:G-risk`, `:G-ambiguity` and `:controller-score` for all 146, and control B
;; reproduces its `:decision`, so the staged field is the recorded field and
;; not a lookalike.
;;
;; ONE INPUT IS NOT BYTES, and it is the same one U22 named: the mission phase
;; comes from the `code/v05/mission-doc` hyperedge family the judge already
;; reads (`mission-epistemic-value/field-readings`). Its census is written into
;; the artifact so a re-run against a changed store is visible as a census
;; change rather than as an unexplained number.
;;
;; NOTHING IS WRITTEN UNDER data/. Replay only: no tick, no trace append, no
;; run lock, no substrate write.

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str]
         '[futon2.aif.efe :as efe]
         '[futon2.aif.mission-epistemic-value :as mev]
         '[futon2.aif.policy :as policy]
         '[futon2.aif.survey-mission-value :as smv])

(import '[java.security MessageDigest]
        '[java.time LocalDate])

;; ---------------------------------------------------------------------------
;; declared inputs
;; ---------------------------------------------------------------------------

(def trace-path "data/wm-trace/wm-trace-2026-09-02.edn")
(def trace-line 2)
(def u23-artifact "holes/labs/wm-contract/runs/U23-cascade-catalog/carrier-population.edn")

(def as-of
  "DECLARED so the U22 staleness questions this field inherits are reproducible."
  (LocalDate/parse "2026-09-03"))

(def survey-eig-weight
  "DECLARED HERE, for this step-through only; `efe/default-survey-eig-weight`
   is 0.0 and flipping it is worklist :J8.

   The value is a round number ABOVE BOTH break points this run measures, and
   both are reported so a reader can see exactly what it buys: 0.004808, at
   which the field's best survey candidate overtakes the field head, and
   0.018170, at which the planted IDENTIFY mission's survey overtakes its own
   advance. Neither break point was tuned; they are solved from the dark arm.
   What the weight cannot do at any value is reorder the survey candidates
   among themselves -- they share one G-core, so their order is the nats order
   and is weight-invariant."
  0.02)

(def arena-opts
  "The modes the 2026-09-02 record itself carries on every ranked entry, read
   off the record rather than re-declared: `:risk-mode :kl`,
   `:ambiguity-mode :gaussian-entropy`, `:goal-outcome-mode :kl`,
   `:structural-pressure-mode :habit-prior`, predictability and homeostatic
   controls `:telemetry-only`, `:graph-feasibility-mode :policy-support`."
  {:risk-mode :kl
   :ambiguity-mode :gaussian-entropy
   :goal-outcome-mode :kl
   :structural-pressure-mode :habit-prior
   :predictability-control-mode :telemetry-only
   :homeostatic-control-mode :telemetry-only
   :graph-feasibility-mode :policy-support})

(def select-opts
  {:selection-gain 1.0
   :selection-boundary :strategic-recommendation
   :selection-law :controller-head})

(def planted-identify-mission
  "PLANTED, and it is the only planted thing in this script. The acceptance asks
   for a PLANTED demonstration in which an IDENTIFY-phase mission generates a
   survey flight; the field also carries 36 REAL identify-phase missions
   (`02-phase-readings.edn :identify-phase-candidates`), and D0 below runs the
   same comparison on all of them.

   THE PLANT IS THE HARDER CASE, DELIBERATELY. On all 36 real ones the survey
   candidate already beats its own recorded advance candidate at
   `survey-eig-weight` 0.0 -- for a reason that has nothing to do with
   information gain, which D0 measures and section 4 of C493 states. So a
   demonstration built on a real identify mission would pass without the
   epistemic term doing anything at all. The plant's rival advance candidate
   carries `:open-hole-count 1` -- the value the mission-enumerator proposer
   gives a live doc with one unchecked item, and the value the recorded field's
   own head M-zaif-harness-v1 carries -- which puts it BELOW the survey
   candidate at G-core, so the epistemic leg is the only thing that can move
   it. Three MAP questions are planted because the point here is the SELECTION;
   the doc parse is measured on the real M-apm-capability-ratchet."
  {:id "M-planted-identify-demo"
   :phase "identify"
   :map-question-count 3
   :advance-open-hole-count 1})

;; ---------------------------------------------------------------------------
;; helpers
;; ---------------------------------------------------------------------------

(defn fmt [f & args] (apply format f args))

(defn sha256 [^String s]
  (let [d (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8"))]
    (apply str (map #(fmt "%02x" %) d))))

(def outdir
  (io/file (or (first *command-line-args*)
               "holes/labs/wm-contract/runs/U24-survey-mission")))
(.mkdirs outdir)

(defn write-edn! [name data]
  (let [f (io/file outdir name)]
    (spit f (with-out-str (pp/pprint data)))
    (println (fmt "wrote %s" (.getPath f)))))

(defn round [x] (when (number? x) (double x)))

;; ---------------------------------------------------------------------------
;; the pinned field
;; ---------------------------------------------------------------------------

(def trace-lines
  (with-open [r (io/reader trace-path)] (vec (line-seq r))))

(def record-text (nth trace-lines (dec trace-line)))
(def record (edn/read-string record-text))

(def state {:observation (:observation record) :belief (:mu-pre record)})
(def recorded-entries (:ranked-actions record))
(def recorded-actions (mapv :action recorded-entries))

(def advance-actions
  (filterv #(= :advance-mission (:type %)) recorded-actions))

(def missions
  "The mission entries a WM state carries for these candidates, rebuilt from
   what the candidates themselves record: `:target` and `:mission-path`."
  (mapv (fn [a] {:id (:target a) :path (:mission-path a)}) advance-actions))

;; ---------------------------------------------------------------------------
;; readings
;; ---------------------------------------------------------------------------

(def field-readings
  (mev/field-readings {:epistemic 1.0} {:epistemic-as-of as-of}))

(def phase-of
  (into {} (map (fn [[k v]] [k (:phase v)])) (:missions field-readings)))

(def u23
  (edn/read-string (slurp u23-artifact)))

(def catalog
  "Per mission, the answerable carriers the cascade catalog holds a READING for.
   A U23 record's carrier counts as read whether its variant is `:observed` or
   `:absent`: `:absent :records-exist-none-for-subject` IS the answer 'zero for
   this mission', which is exactly what a survey would have returned. Derived
   from the artifact, not listed here."
  (into {}
        (map (fn [rec]
               [(mev/normalize-mission-id (:mission rec))
                (into #{} (filter (set smv/answerable-carriers))
                      (keys (:carriers rec)))]))
        (:cascade-records u23)))

(def readings
  (smv/readings-from-missions
   {:missions missions :phase-of phase-of :catalog catalog}))

(def survey-actions
  (smv/enrich-survey-candidates
   readings
   (mapv (fn [m] {:type :survey-mission
                  :target (:id m)
                  :weight 0.3
                  :rationale (str "portfolio: survey " (:id m)
                                  " before advance — information-gathering option")})
         missions)))

(def learn-survey-gap
  "The bootstrap proposer's `:learn-action-class :survey-mission` candidate, in
   the recorded field because `can-propose? :survey-mission` was false."
  (first (filter #(and (= :learn-action-class (:type %))
                       (= :survey-mission (:target-class %)))
                 recorded-actions)))

(def after-actions
  "The recorded field with the survey gap candidate REMOVED and the 133 real
   survey candidates added. Removing the gap is not cosmetic: once
   `can-propose? :survey-mission` is true the bootstrap proposer no longer
   emits it (`action_proposer.clj:34-46`), and leaving it in would put a
   candidate saying 'survey is unavailable' beside 133 survey candidates."
  (into (filterv #(not= % learn-survey-gap) recorded-actions) survey-actions))

;; ---------------------------------------------------------------------------
;; arms
;; ---------------------------------------------------------------------------

(defn rank [actions opts]
  (efe/rank-actions state actions (merge arena-opts opts)))

(defn entry->row [e]
  (cond-> {:type (get-in e [:action :type])
           :target (or (get-in e [:action :target])
                       (get-in e [:action :target-class]))
           :rank (:rank e)
           :G-risk (round (:G-risk e))
           :G-ambiguity (round (:G-ambiguity e))
           :G-core (round (:G-core e))
           :controller-score (round (:controller-score e))}
    (contains? e :survey-eig-contribution)
    (assoc :survey-eig-nats (round (:survey-eig-nats e))
           :survey-eig-contribution (round (:survey-eig-contribution e))
           :survey-eig-weight (round (:survey-eig-weight e)))))

(def arm-recorded (rank recorded-actions {}))
(def arm-dark (rank after-actions {}))
(def arm-live (rank after-actions {:survey-eig-weight survey-eig-weight}))

(defn decision-of [ranked]
  (let [d (policy/select-action ranked select-opts)]
    {:type (get-in d [:action :type])
     :target (or (get-in d [:action :target]) (get-in d [:action :target-class]))
     :rank (:rank d)
     :controller-score (round (:controller-score d))
     :reason (:reason d)}))

;; ---------------------------------------------------------------------------
;; controls
;; ---------------------------------------------------------------------------

(def control-a
  "The staged field IS the recorded field: recompute every recorded candidate's
   G terms from the record's own observation and belief and compare."
  (let [by-target (into {} (map (juxt :action identity)) arm-recorded)
        deltas (for [e recorded-entries
                     :let [got (get by-target (:action e))]]
                 {:matched? (some? got)
                  :d-risk (when got (Math/abs (- (double (:G-risk got)) (double (:G-risk e)))))
                  :d-ambig (when got (Math/abs (- (double (:G-ambiguity got)) (double (:G-ambiguity e)))))
                  :d-score (when got (Math/abs (- (double (:controller-score got))
                                                  (double (:controller-score e)))))})]
    {:n (count deltas)
     :matched (count (filter :matched? deltas))
     :max-d-risk (apply max 0.0 (keep :d-risk deltas))
     :max-d-ambiguity (apply max 0.0 (keep :d-ambig deltas))
     :max-d-controller-score (apply max 0.0 (keep :d-score deltas))}))

(def control-b
  "The staged field reproduces the record's own decision through the shipped
   selection law."
  (let [got (decision-of arm-recorded)
        want (get-in record [:decision :action])]
    {:recomputed got
     :recorded {:type (:type want) :target (:target want)
                :rank (get-in record [:decision :rank])}
     :same-action? (= [(:type got) (:target got)]
                      [(:type want) (:target want)])
     :recorded-selection-boundary (get-in record [:decision :selection-boundary])
     :note (str "The record's own boundary is "
                (get-in record [:decision :selection-boundary])
                ", a further filter this replay does not apply; the two agree "
                "on this tick and the agreement is what is claimed, not that "
                "the boundaries are the same object.")}))

(def control-c
  "The default is off: at `efe/default-survey-eig-weight` the `:survey-eig`
   key is absent from every candidate's `:augmentation-terms` and the survey
   candidates' scores are identical to their unenriched selves."
  (let [bare (rank (mapv #(dissoc % :survey-eig-nats :survey-eig-basis) after-actions) {})
        with (rank after-actions {})
        key-> (fn [ranked] (into {} (map (juxt :action :controller-score)) ranked))
        b (key-> bare) w (key-> with)]
    {:default-weight (round efe/default-survey-eig-weight)
     :augmentation-keys-with-survey-eig
     (count (filter #(contains? (:augmentation-terms %) :survey-eig) with))
     :max-score-delta-payload-vs-none
     (apply max 0.0 (for [[a s] w
                          :let [s2 (get b (dissoc a :survey-eig-nats :survey-eig-basis))]
                          :when s2]
                      (Math/abs (- (double s) (double s2)))))
     :n-compared (count w)}))

;; ---------------------------------------------------------------------------
;; the U4 re-run
;; ---------------------------------------------------------------------------

(defn arg-best
  "U4's arg-best: the minimum over KEY-FN, with ties reported rather than
   broken silently."
  [entries key-fn]
  (let [m (apply min (map #(double (key-fn %)) entries))
        tied (filterv #(= m (double (key-fn %))) entries)]
    {:score m
     :n-tied (count tied)
     :action (let [a (:action (first (sort-by #(str (get-in % [:action :type]) "|"
                                                    (get-in % [:action :target])) tied)))]
               [(:type a) (or (:target a) (:target-class a))])}))

(defn lambda-break
  "U4's lambda_break: the smallest scale factor lambda > 1 at which arg-best
   over `risk + lambda*ambiguity` stops being the arg-best at lambda = 1. Only
   a candidate with LOWER ambiguity can overtake, at
   (risk_w - risk_j)/(amb_j - amb_w) -- see C487 section 4."
  [entries]
  (let [w (first (sort-by #(double (:G-core %)) entries))
        rw (double (:G-risk w)) aw (double (:G-ambiguity w))]
    (->> entries
         (remove #(identical? % w))
         (keep (fn [c]
                 (let [d (- (double (:G-ambiguity c)) aw)]
                   (when (neg? d)
                     (let [l (/ (- rw (double (:G-risk c))) d)]
                       (when (> l 1.0) {:lambda l
                                        :overtaker [(get-in c [:action :type])
                                                    (get-in c [:action :target])]}))))))
         (sort-by :lambda)
         first)))

(defn ambiguity-report [label entries]
  (let [with (arg-best entries :G-core)
        risk-only (arg-best entries :G-risk)
        amb-only (arg-best entries :G-ambiguity)
        ambs (mapv #(double (:G-ambiguity %)) entries)
        entropy-carrying (filterv #(not= :learn-action-class (get-in % [:action :type])) entries)]
    {:field label
     :n (count entries)
     ;; grain A, U4's theory grain
     :arg-best-with-ambiguity (:action with)
     :arg-best-risk-only (:action risk-only)
     :grain-a-changed? (not= (:action with) (:action risk-only))
     :ties-with-ambiguity (:n-tied with)
     :ties-risk-only (:n-tied risk-only)
     :lambda-break (lambda-break entries)
     ;; U4's C4 positive control, run on this field
     :positive-control-drop-risk
     {:arg-best-ambiguity-only (:action amb-only)
      :changed? (not= (:action with) (:action amb-only))}
     ;; U4 section 6's sharper statement, re-measured
     :ambiguity-distinct-values (count (distinct ambs))
     :ambiguity-spread (- (apply max ambs) (apply min ambs))
     :ambiguity-distinct-values-entropy-carrying
     (count (distinct (map #(double (:G-ambiguity %)) entropy-carrying)))
     :ambiguity-spread-entropy-carrying
     (let [a (mapv #(double (:G-ambiguity %)) entropy-carrying)]
       (- (apply max a) (apply min a)))
     :by-action-type
     (into (sorted-map)
           (map (fn [[t es]]
                  [t {:n (count es)
                      :ambiguity (round (:G-ambiguity (first es)))
                      :distinct (count (distinct (map :G-ambiguity es)))}]))
           (group-by #(get-in % [:action :type]) entries))}))

;; ---------------------------------------------------------------------------
;; break point
;; ---------------------------------------------------------------------------

(def break-point
  "The smallest `survey-eig-weight` at which a `:survey-mission` candidate
   takes rank 1 on the staged field, solved from the dark arm rather than
   searched: the head is an :advance-mission at G_a, the best survey candidate
   is at G_s with payload n nats, and it overtakes when w > (G_s - G_a) / n."
  (let [head (first arm-dark)
        surveys (filterv #(= :survey-mission (get-in % [:action :type])) arm-dark)
        best (first (sort-by (fn [e] (let [n (double (:survey-eig-nats (:action e) 0.0))]
                                       (if (pos? n)
                                         (/ (- (double (:controller-score e))
                                               (double (:controller-score head)))
                                            n)
                                         Double/POSITIVE_INFINITY)))
                             surveys))
        n (double (:survey-eig-nats (:action best) 0.0))
        gap (- (double (:controller-score best)) (double (:controller-score head)))]
    {:head [(get-in head [:action :type]) (get-in head [:action :target])]
     :head-controller-score (round (:controller-score head))
     :best-survey (get-in best [:action :target])
     :best-survey-controller-score (round (:controller-score best))
     :best-survey-nats (round n)
     :gap-to-head (round gap)
     :w-break (round (/ gap n))
     :declared-weight survey-eig-weight
     :declared-above-break? (> survey-eig-weight (/ gap n))}))

;; ---------------------------------------------------------------------------
;; the planted IDENTIFY demonstration
;; ---------------------------------------------------------------------------

(def planted-reading
  {:mission-id (mev/normalize-mission-id (:id planted-identify-mission))
   :phase (:phase planted-identify-mission)
   :phase-carrier :planted
   :map-questions {:questions (vec (for [i (range 1 (inc (:map-question-count
                                                          planted-identify-mission)))]
                                     {:ordinal i :line (+ 100 i)
                                      :text (str "planted MAP question " i)}))}
   :catalog-readings #{}})

(def planted-demo
  "TWO CLAIMS, kept apart because only the first is the demonstration the
   acceptance asks for.

   D1, THE DEMONSTRATION: on the demonstration's own minimal field -- the
   planted mission's advance candidate, its survey candidate, and `:no-op` --
   the selector chooses the SURVEY. That is what `an IDENTIFY-phase mission
   generates a survey flight` means with nothing else in the way: for a mission
   still being framed, looking beats advancing.

   D2, THE CONTEXT, reported and not smoothed: the same two planted candidates
   dropped into the full 2026-09-02 field. The planted survey does not take
   rank 1 there, and the reason is the term working rather than failing --
   M-apm-capability-ratchet is a real MAP-phase mission with six listed MAP
   questions and 7.62 nats against the plant's 3.33, and no weight can reorder
   two survey candidates (they share a G-core)."
  (let [pr planted-reading
        readings' (assoc-in readings [:missions (:mission-id pr)] pr)
        survey-action (first (smv/enrich-survey-candidates
                              readings'
                              [{:type :survey-mission
                                :target (:id planted-identify-mission)
                                :weight 0.3
                                :rationale "planted IDENTIFY-phase mission (U24 demonstration)"}]))
        advance-action {:type :advance-mission
                        :target (:id planted-identify-mission)
                        :weight 1.0
                        :open-hole-count (:advance-open-hole-count planted-identify-mission)
                        :rationale "planted IDENTIFY-phase mission, advance rival (U24 demonstration)"}
        minimal (rank [advance-action survey-action {:type :no-op}]
                      {:survey-eig-weight survey-eig-weight})
        minimal-dark (rank [advance-action survey-action {:type :no-op}] {})
        d1 (decision-of minimal)
        full (rank (into after-actions [advance-action survey-action])
                   {:survey-eig-weight survey-eig-weight})
        d2 (decision-of full)
        planted-survey-rank (first (keep-indexed
                                    (fn [i e] (when (and (= :survey-mission (get-in e [:action :type]))
                                                         (= (:id planted-identify-mission)
                                                            (get-in e [:action :target])))
                                                (inc i)))
                                    full))
        dark-adv (first (filter #(= :advance-mission (get-in % [:action :type])) minimal-dark))
        dark-sur (first (filter #(= :survey-mission (get-in % [:action :type])) minimal-dark))
        nats (double (:survey-eig-nats survey-action 0.0))]
    {:planted planted-identify-mission
     :availability (get mev/phase-survey-availability (:phase planted-identify-mission))
     :d0-real-identify-phase-missions
     (let [ident (set (filterv (fn [id] (= "identify" (get phase-of (mev/normalize-mission-id id))))
                               (map :id missions)))
           rows (for [m missions
                      :when (ident (:id m))
                      :let [adv (first (filter #(= (:id m) (:target %)) advance-actions))
                            sv (first (filter #(= (:id m) (:target %)) survey-actions))
                            a (first (rank [adv] {}))
                            sd (first (rank [sv] {}))]]
                  {:id (:id m)
                   :survey-eig-nats (round (:survey-eig-nats sv 0.0))
                   :advance-controller-score-dark (round (:controller-score a))
                   :survey-controller-score-dark (round (:controller-score sd))
                   :survey-wins-at-zero-weight?
                   (< (double (:controller-score sd)) (double (:controller-score a)))})]
       {:n (count rows)
        :survey-wins-at-zero-weight (count (filter :survey-wins-at-zero-weight? rows))
        :finding (str "On every real IDENTIFY-phase candidate the survey option "
                      "already outranks its own advance option with the epistemic "
                      "leg OFF. That ordering is bought by the forward model, not "
                      "by information gain: predict-effects :survey declares a "
                      "variance on one channel and none on the other thirteen, so "
                      "its ambiguity term is 7.915 nats better than "
                      ":advance-mission's. A survey demonstration built on one of "
                      "these would pass without the epistemic term doing anything.")
        :rows (vec (take 5 rows))})
     :survey-value (-> (smv/survey-value pr) (dissoc :questions) (update :nats round))
     :d1-minimal-field
     {:candidates (mapv entry->row minimal)
      :decision d1
      :selected-the-planted-survey?
      (= [(:type d1) (:target d1)] [:survey-mission (:id planted-identify-mission)])
      :w-break (round (/ (- (double (:controller-score dark-sur))
                            (double (:controller-score dark-adv)))
                         nats))
      :declared-weight survey-eig-weight
      :advance-controller-score-dark (round (:controller-score dark-adv))
      :survey-controller-score-dark (round (:controller-score dark-sur))
      :survey-eig-nats (round nats)}
     :d2-full-field
     {:field-size (count full)
      :planted-survey-rank planted-survey-rank
      :planted-survey-entry (entry->row (nth full (dec planted-survey-rank)))
      :decision d2
      :why-not-rank-1
      (str "M-apm-capability-ratchet carries " (:best-survey-nats break-point)
           " nats against the plant's " (round nats)
           "; survey candidates share one G-core, so their order is the nats "
           "order at every weight.")}}))

;; ---------------------------------------------------------------------------
;; write
;; ---------------------------------------------------------------------------

(def survey-value-rows
  (vec (for [m missions
             :let [v (smv/survey-value (get-in readings
                                               [:missions (mev/normalize-mission-id (:id m))]))]]
         (-> v
             (dissoc :questions)
             (assoc :id (:id m))
             (update :nats round)))))

(write-edn! "01-field.edn"
            {:trace-path trace-path
             :trace-line trace-line
             :record-sha256 (sha256 record-text)
             :record-timestamp (:timestamp record)
             :run-id (get record :run/id)
             :recorded-candidates (count recorded-actions)
             :recorded-types (into (sorted-map)
                                   (frequencies (map :type recorded-actions)))
             :arena-opts arena-opts
             :select-opts select-opts
             :learn-action-class-survey-gap-removed (some? learn-survey-gap)
             :after-candidates (count after-actions)})

(write-edn! "02-phase-readings.edn"
            {:as-of (str as-of)
             :source "futon2.aif.mission-epistemic-value/field-readings (code/v05/mission-doc)"
             :census (:census field-readings)
             :candidates (count missions)
             :phase-carrier-counts
             (frequencies (map (fn [m] (get-in readings [:missions (mev/normalize-mission-id (:id m))
                                                         :phase-carrier]))
                               missions))
             :phase-counts (into (sorted-map)
                                 (frequencies (keep (fn [m] (get phase-of (mev/normalize-mission-id (:id m))))
                                                    missions)))
             :identify-phase-candidates
             (filterv (fn [id] (= "identify" (get phase-of (mev/normalize-mission-id id))))
                      (map :id missions))})

(write-edn! "03-survey-readings.edn"
            {:carrier-partition
             {:answerable smv/answerable-carriers
              :unanswerable smv/unanswerable-carriers
              :basis u23-artifact
              :detail smv/catalog-carriers}
             :map-question-carrier
             {:pattern (str smv/map-question-heading-pattern)
              :docs-with-list (filterv some?
                                       (for [m missions
                                             :let [r (get-in readings [:missions (mev/normalize-mission-id (:id m))])]
                                             :when (seq (get-in r [:map-questions :questions]))]
                                         {:id (:id m)
                                          :path (:doc-path r)
                                          :heading-line (get-in r [:map-questions :heading-line])
                                          :questions (count (get-in r [:map-questions :questions]))}))
              :docs-without-list (count (filter (fn [m]
                                                  (empty? (get-in readings
                                                                   [:missions (mev/normalize-mission-id (:id m))
                                                                    :map-questions :questions])))
                                                missions))
              :near-miss-phrasings smv/near-miss-phrasings}
             :catalog-readings-from-u23 catalog
             :status-counts (into (sorted-map) (frequencies (map :status survey-value-rows)))
             :nats-histogram (into (sorted-map) (frequencies (map :nats survey-value-rows)))
             :top-10-by-nats (vec (take 10 (reverse (sort-by :nats survey-value-rows))))
             :rows survey-value-rows})

(write-edn! "04-arms.edn"
            {:controls {:a-field-reproduction control-a
                        :b-decision-reproduction control-b
                        :c-default-off control-c}
             :break-point break-point
             :arms
             {:recorded {:n (count arm-recorded)
                         :decision (decision-of arm-recorded)
                         :top-10 (mapv entry->row (take 10 arm-recorded))}
              :survey-dark {:n (count arm-dark)
                            :survey-eig-weight (round efe/default-survey-eig-weight)
                            :decision (decision-of arm-dark)
                            :top-10 (mapv entry->row (take 10 arm-dark))
                            :best-survey-rank
                            (first (keep-indexed (fn [i e]
                                                   (when (= :survey-mission (get-in e [:action :type]))
                                                     (inc i)))
                                                 arm-dark))}
              :survey-live {:n (count arm-live)
                            :survey-eig-weight survey-eig-weight
                            :decision (decision-of arm-live)
                            :top-10 (mapv entry->row (take 10 arm-live))
                            :survey-candidates-in-top-10
                            (count (filter #(= :survey-mission (get-in % [:action :type]))
                                           (take 10 arm-live)))}}
             :survey-candidates-are-target-invariant-at-G-core
             (let [ss (filterv #(= :survey-mission (get-in % [:action :type])) arm-dark)]
               {:n (count ss)
                :distinct-G-core (count (distinct (map :G-core ss)))
                :distinct-survey-eig-nats (count (distinct (map #(get-in % [:action :survey-eig-nats]) ss)))})})

(write-edn! "05-ambiguity-rerun.edn"
            {:question "U4 re-run: does the ambiguity leg discriminate once :survey-mission candidates exist?"
             :grain "U4 grain A (the theory grain): arg-best over G-risk + G-ambiguity against arg-best over G-risk alone. Both legs are recorded per candidate, so this needs no reconstruction."
             :before (ambiguity-report :recorded-146 arm-recorded)
             :after (ambiguity-report :with-133-survey-candidates arm-dark)
             :note "The epistemic leg lives in the augmentation layer, not in G-core (invariant I3 pins :G-core = risk + ambiguity), so the survey-live arm's grain-A numbers are identical to the survey-dark arm's by construction and are not reported twice."})

(write-edn! "06-planted-identify-demo.edn" planted-demo)

(write-edn! "07-discharge-shape.edn" smv/discharge-shape)

;; ---------------------------------------------------------------------------
;; console summary
;; ---------------------------------------------------------------------------

(println)
(println "== U24 staged field ==")
(println (fmt "record %s line %d, sha256 %s"
              trace-path trace-line (subs (sha256 record-text) 0 12)))
(println (fmt "control A (field reproduction): %d/%d matched, max |dG-risk| %.3e, max |dG-amb| %.3e, max |dscore| %.3e"
              (:matched control-a) (:n control-a)
              (:max-d-risk control-a) (:max-d-ambiguity control-a)
              (:max-d-controller-score control-a)))
(println (fmt "control B (decision reproduction): %s" (:same-action? control-b)))
(println (fmt "control C (default off): %d candidates carry a :survey-eig key, max |dscore| %.3e"
              (:augmentation-keys-with-survey-eig control-c)
              (:max-score-delta-payload-vs-none control-c)))
(println)
(println (fmt "survey readings: %s" (pr-str (into (sorted-map) (frequencies (map :status survey-value-rows))))))
(println (fmt "MAP-question lists found: %d of %d docs"
              (count (filter (fn [m] (seq (get-in readings [:missions (mev/normalize-mission-id (:id m))
                                                            :map-questions :questions])))
                             missions))
              (count missions)))
(println)
(println (fmt "break point: w_break = %.6f (gap %.6f over %.4f nats on %s); declared %.4f"
              (:w-break break-point) (:gap-to-head break-point)
              (:best-survey-nats break-point) (:best-survey break-point)
              (double survey-eig-weight)))
(println (fmt "decision  recorded arm: %s" (pr-str (decision-of arm-recorded))))
(println (fmt "decision  survey-dark : %s" (pr-str (decision-of arm-dark))))
(println (fmt "decision  survey-live : %s" (pr-str (decision-of arm-live))))
(println)
(println "== U4 ambiguity re-run ==")
(doseq [[label rep] [["before" (ambiguity-report :recorded-146 arm-recorded)]
                     ["after " (ambiguity-report :with-survey arm-dark)]]]
  (println (fmt "%s n=%-4d grain-A changed? %-5s lambda_break %s  positive control (drop risk) changed? %s"
                label (:n rep) (str (:grain-a-changed? rep))
                (if-let [lb (:lambda-break rep)] (fmt "%.6f" (:lambda lb)) "none")
                (str (get-in rep [:positive-control-drop-risk :changed?])))))
(println)
(println (fmt "planted IDENTIFY demo D1 (minimal field): survey selected over advance? %s (w_break %.6f, declared %.4f)"
              (str (get-in planted-demo [:d1-minimal-field :selected-the-planted-survey?]))
              (get-in planted-demo [:d1-minimal-field :w-break])
              (double survey-eig-weight)))
(println (fmt "planted IDENTIFY demo D2 (full field): planted survey rank %d of %d; decision %s"
              (get-in planted-demo [:d2-full-field :planted-survey-rank])
              (get-in planted-demo [:d2-full-field :field-size])
              (pr-str (get-in planted-demo [:d2-full-field :decision]))))
(println)
(println (str/join " " ["artifacts under" (.getPath outdir)]))
