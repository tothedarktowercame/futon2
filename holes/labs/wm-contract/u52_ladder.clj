#!/usr/bin/env clojure
;; U52 -- the three-rung ladder against the recorded plateau.
;;
;;   clojure -M holes/labs/wm-contract/u52_ladder.clj [outdir]
;;
;; WHAT THIS ROW OWES (worklist :U52): the ladder of
;; `aif-equations.edn :choices :task-belief-actand-source` implemented at the
;; selection scoring seam behind a default-off flag, and -- the part that is the
;; actual acceptance -- THE PLATEAU SHRINKING ON THE RECORDED FIELDS. U51
;; measured the plateau; this measures what the ladder does to it.
;;
;; THE SHIPPED PATH IS WHAT IS MEASURED. Every rung below comes from
;; `war-machine/apply-task-belief-ladder` -- the function the judge calls
;; between enrichment and ranking -- and every controller score comes from
;; `efe/compute-efe`, the function that produced the recorded ones. The
;; off-arm control is what makes that claim checkable: the replay reproduces
;; all 145 recorded controller scores at deviation 0.0 before the ladder is
;; asked to change any of them.
;;
;; THE CANDIDATE SET IS THE RECORD. The recorded `:ranked-actions` carry their
;; enriched `:action` maps -- the output of
;; `enrich-candidates-with-mission-value`, which is exactly the ladder's input
;; -- so the replay starts from bytes rather than from a re-enumeration.
;;
;; NOTHING IS WRITTEN UNDER data/ AND NO TICK IS TAKEN. Replay only: no run
;; lock, no substrate write, no network, no weight or table moved. The tension
;; mint is a SEPARATE step (`u52_mint_refusals.bb`), because the ledger append
;; belongs to U41's sole write API and this producer is read-only over it.
;;
;; DETERMINISM. No wall-clock field is written; `as-of` is pinned below. Two
;; consecutive runs are byte-identical.

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str]
         'futon2.aif.task-belief-ladder
         'futon2.aif.efe
         'futon2.report.war-machine)

(alias 'ladder 'futon2.aif.task-belief-ladder)
(alias 'efe 'futon2.aif.efe)
(alias 'wm 'futon2.report.war-machine)

(def as-of "2026-09-04")

(def args *command-line-args*)
(def out-dir (io/file (or (first args) "holes/labs/wm-contract/runs/U52-ladder")))
(def lab (io/file "holes/labs/wm-contract"))

(def read-opts {:default (fn [t v] {:unread-tag t :value v})})

(defn read-trace [path]
  (edn/read-string read-opts (str "[" (slurp (str path)) "]")))

(def pinned-fields
  "The two recorded fields U51 partitioned, pinned by run id. Both were minted
   into the tension ledger on 2026-09-04, before `:tension/provenance :records`
   existed -- see `mint-payload` for why that has to be said here rather than
   fixed silently."
  [{:id :s5
    :trace "holes/labs/wm-contract/runs/2026-09-01-s5/wm-trace-s5.edn"
    :run-id "4e35e740-8c9f-42c1-b8a9-0cdfc024e9c8"
    :minted-before-the-run-key true}
   {:id :re5
    :trace "holes/labs/wm-contract/runs/2026-09-04-re5/wm-trace-re5.edn"
    :run-id "8ae111bc-d758-45f3-9c5b-f98832e10bb6"
    :minted-before-the-run-key true}])

(def fields
  "The pinned fields, plus at most one MORE named in U52_EXTRA_FIELD as an EDN
   map of the same shape ({:id :trace :run-id}). :U60 uses it to run the ladder
   over a freshly stepped run and mint that run's tension with the structured
   run key, without adding a third field to the committed two-field report --
   the extra arm goes to whatever outdir the caller names. The pinned fields are
   never dropped, so the extra arm is measured beside them, under the same
   corpus, the same relation and the same controls."
  (into pinned-fields
        (when-let [s (System/getenv "U52_EXTRA_FIELD")]
          [(edn/read-string s)])))

(def trace-dir (str (System/getProperty "user.home") "/code/futon2/data/wm-trace"))

(def case-history
  "Read ONCE, through the shipped reader, and threaded into every arm as
   `:case-history` so no arm can differ by reading a different corpus."
  (delay (wm/case-history-index trace-dir)))

;; ---------------------------------------------------------------------------
;; scoring: the recorded modes, recomputed by the function that recorded them
;; ---------------------------------------------------------------------------

(defn efe-opts
  "The per-row modes the record carries. Taken off the row rather than declared
   here, so the replay cannot silently score under different modes than the tick
   did; the off-arm control is what proves it did not."
  [row]
  {:risk-mode (:risk-mode row)
   :ambiguity-mode (:ambiguity-mode row)
   :goal-outcome-mode (:goal-outcome-mode row)
   :structural-pressure-mode (:structural-pressure-mode row)
   :predictability-control-mode (:predictability-control-mode row)
   :homeostatic-control-mode (:homeostatic-control-mode row)
   :graph-feasibility-mode (:graph-feasibility-mode row)
   :time-pressure (double (or (:time-pressure row) 0.0))})

(defn field-of [{:keys [id trace run-id] :as declared}]
  (let [recs (read-trace trace)
        rec (first (filter #(= run-id (:run/id %)) recs))
        rows (vec (sort-by :rank (:ranked-actions rec)))]
    (when-not rec (throw (ex-info "run id not found in trace" {:field id :run-id run-id})))
    ;; MERGED ONTO THE DECLARATION, not built beside it: the declaration carries
    ;; :minted-before-the-run-key and :artifact, which `mint-payload` reads, and
    ;; a field map rebuilt from three keys silently dropped both -- so every
    ;; field got the structured run key including the two already committed
    ;; without it. Found by reading the payloads (:U60).
    (merge
     declared
     {:id id
      :trace trace
      :run-id run-id
      :timestamp (:timestamp rec)
      :state {:observation (:observation rec) :belief (:mu-pre rec)}
      :opts (efe-opts (first rows))
      :rows rows
      :candidates (mapv :action rows)
      :chosen [(get-in rec [:decision :action :type])
               (get-in rec [:decision :action :target])]})))

(defn score-field
  "Controller score for every candidate, through `efe/compute-efe` -- the
   function that produced the recorded numbers."
  [field candidates]
  (mapv (fn [a]
          (let [o (efe/compute-efe (:state field) a (:opts field))]
            {:key (ladder/action-key a)
             :controller-score (:controller-score o)
             :mission-value-factor (:mission-value-factor a)
             :rung (:task-belief/rung a)}))
        candidates))

(defn plateau-of
  "The widest set of scored rows sharing one controller score; ties on width
   broken by the better score so the pick is deterministic."
  [scored]
  (let [g (group-by :controller-score scored)
        best (->> g (sort-by (fn [[s ms]] [(- (count ms)) s])) first)]
    {:score (key best)
     :width (count (val best))
     :members (mapv :key (sort-by :controller-score (val best)))}))

(defn width-census [scored]
  (let [g (group-by :controller-score scored)]
    {:distinct-scores (count g)
     :widest (apply max 0 (map count (vals g)))
     :ties-over-1 (count (filter #(> (count %) 1) (vals g)))}))

;; ---------------------------------------------------------------------------
;; arms
;; ---------------------------------------------------------------------------

(defn off-arm [field]
  (let [in (:candidates field)
        r (wm/apply-task-belief-ladder in {:case-history @case-history})
        scored (score-field field (:candidates r))
        recorded (mapv :controller-score (:rows field))
        replayed (mapv :controller-score scored)
        devs (mapv (fn [a b] (Math/abs (- (double a) (double b)))) recorded replayed)]
    {:identical-object? (identical? in (:candidates r))
     :refusals (count (:refusals r))
     :ladder-record (:ladder r)
     :rows (count scored)
     :max-abs-deviation-from-record (apply max 0.0 devs)
     :rows-differing (count (filter pos? devs))
     :plateau (plateau-of scored)
     :width-census (width-census scored)}))

(defn ladder-arm [field relation]
  (let [in (:candidates field)
        r (wm/apply-task-belief-ladder in {:task-belief-ladder? true
                                           :relation relation
                                           :case-history @case-history})
        scored (score-field field (:candidates r))]
    {:relation relation
     :census (:census r)
     :refused (count (:refusals r))
     :scored-rows (count scored)
     :plateau (plateau-of scored)
     :width-census (width-census scored)
     :ladder-record (:ladder r)
     :result r
     :scored scored}))

(defn plateau-members
  "The recorded plateau: the widest tie of the RECORDED field, by key."
  [field]
  (let [g (group-by :controller-score (:rows field))
        best (->> g (sort-by (fn [[s ms]] [(- (count ms)) s])) first)]
    {:score (key best)
     :width (count (val best))
     :ranks [(apply min (map :rank (val best))) (apply max (map :rank (val best)))]
     :keys (mapv #(ladder/action-key (:action %)) (sort-by :rank (val best)))}))

(defn plateau-report
  "The acceptance measurement: what the ladder does to the RECORDED plateau."
  [field arm]
  (let [pm (plateau-members field)
        member-set (set (:keys pm))
        by-key (into {} (map (juxt :key identity)) (:scored arm))
        refused (into #{} (map :refusal/action-key) (:refusals (:result arm)))
        kept (filterv member-set (map :key (:scored arm)))
        kept-scores (mapv #(:controller-score (by-key %)) kept)
        kept-factors (mapv #(:mission-value-factor (by-key %)) kept)
        rungs (frequencies (mapv #(:rung (by-key %)) kept))]
    {:relation (:relation arm)
     :recorded {:width (:width pm) :ranks (:ranks pm) :score (:score pm)
                :distinct-mission-value-factors 1
                :distinct-controller-scores 1}
     :after {:members-refused (count (filter refused (:keys pm)))
             :members-kept (count kept)
             :rungs-of-kept rungs
             :distinct-mission-value-factors (count (distinct kept-factors))
             :distinct-controller-scores (count (distinct kept-scores))
             :widest-remaining-tie-among-them
             (apply max 0 (map count (vals (group-by identity kept-scores))))}}))

;; ---------------------------------------------------------------------------
;; controls
;; ---------------------------------------------------------------------------

(def planted-unknown
  "U51's negative-1, replanted: a candidate of a type no chosen key carries, in
   a repository no candidate lives in, with no document and no :produces."
  {:type :learn-action-class
   :target "M-u52-planted-control-not-a-real-mission"
   :mission-path "/home/joe/code/futon-nowhere/holes/missions/M-u52-planted.md"
   :mission-value-factor 0.09})

(defn classify-one [field relation candidate history]
  (let [ctx (ladder/field-context (conj (:candidates field) candidate)
                                  history {:relation relation})]
    (ladder/classify ctx candidate)))

(defn controls [field off on]
  (let [hist @case-history
        pm (plateau-members field)
        rung1 (filterv #(= 1 (:task-belief/rung %)) (:candidates (:result on)))
        rung2 (filterv #(= 2 (:task-belief/rung %)) (:candidates (:result on)))
        refusals (:refusals (:result on))
        chosen-key (:chosen field)
        empty-arm (wm/apply-task-belief-ladder (:candidates field)
                                               {:task-belief-ladder? true
                                                :relation ladder/default-relation
                                                :case-history {}})
        ;; injectivity is a claim about the class the ladder scores: the score
        ;; depends on an :advance-mission action only through the factor, but a
        ;; DIFFERENT action type reaches the score by a different predict-effects
        ;; method, so the whole field is the wrong subject for it.
        in-scope-scored (filterv #(= :advance-mission (first (:key %))) (:scored on))
        by-factor (group-by :mission-value-factor in-scope-scored)]
    {:positive/off-arm-reproduces-the-record
     {:rows (:rows off) :max-abs-deviation (:max-abs-deviation-from-record off)
      :rows-differing (:rows-differing off)
      :pass? (and (zero? (:rows-differing off))
                  (= 0.0 (:max-abs-deviation-from-record off)))
      :why "the replay scores through efe/compute-efe with the modes the row carries and reproduces every recorded controller score exactly -- so a difference in the ladder arm is the ladder's and not the replay's"}

     :positive/flag-off-is-the-identity
     {:identical-object? (:identical-object? off) :refusals (:refusals off)
      :ladder-record (:ladder-record off)
      :pass? (and (:identical-object? off) (zero? (:refusals off))
                  (nil? (:ladder-record off)))
      :why "with the declared input absent the SAME vector object comes back and no ladder record is attached, so the default tick cannot differ from a pre-U52 tick by construction rather than by comparison"}

     :positive/plateau-is-drained
     {:recorded-width (:width pm)
      :members-refused (count (filter (into #{} (map :refusal/action-key) refusals) (:keys pm)))
      :members-kept (- (:width pm) (count (filter (into #{} (map :refusal/action-key) refusals) (:keys pm))))
      :widest-remaining-tie
      (let [member-set (set (:keys pm))
            bk (into {} (map (juxt :key :controller-score)) (:scored on))
            kept (filterv member-set (map :key (:scored on)))]
        (apply max 0 (map count (vals (group-by bk kept)))))
      :pass? (let [member-set (set (:keys pm))
                   bk (into {} (map (juxt :key :controller-score)) (:scored on))
                   kept (filterv member-set (map :key (:scored on)))]
               (< (apply max 0 (map count (vals (group-by bk kept)))) (:width pm)))
      :why "the acceptance is the plateau shrinking on the recorded field, not code landing: the widest tie AMONG THE RECORDED PLATEAU'S MEMBERS after the ladder must be strictly narrower than the recorded one"}

     :positive/case-history-wins
     {:min-rung-1-factor (apply min 1.0 (map :task-belief/factor rung1))
      :max-rung-2-factor (apply max 0.0 (map :task-belief/factor rung2))
      :pass? (or (empty? rung2)
                 (> (apply min 1.0 (map :task-belief/factor rung1))
                    (apply max 0.0 (map :task-belief/factor rung2))))
      :why "the ruling's first clause as an ORDERING PROPERTY OF THE ARITHMETIC, not a claim about it: with support n/(n+1) a rung-1 candidate scores at least 0.5 and the 0.5 generalization discount holds every constructed value strictly under 0.5"}

     :positive/constructed-is-marked-and-derived
     {:rung-2 (count rung2)
      :unmarked (count (remove :task-belief/constructed rung2))
      :without-provenance (count (remove #(seq (get-in % [:task-belief/derivation :provenance])) rung2))
      :pass? (and (every? :task-belief/constructed rung2)
                  (every? #(seq (get-in % [:task-belief/derivation :provenance])) rung2))
      :why "the ruling: a constructed value is never passed off as observed, and its provenance points at the kin records it generalized from"}

     :positive/refusal-is-typed-and-grounded
     {:refusals (count refusals)
      :census-rung-3 (get (:census (:result on)) 3 0)
      :mistyped (count (remove #(= ladder/refusal-reason (:refusal/reason %)) refusals))
      :ungrounded (count (remove #(str/starts-with? (str (:refusal/basis %)) "not found") refusals))
      :pass? (and (= (count refusals) (get (:census (:result on)) 3 0))
                  (every? #(= ladder/refusal-reason (:refusal/reason %)) refusals)
                  (every? #(str/starts-with? (str (:refusal/basis %)) "not found") refusals))
      :why "every refusal carries the one typed reason and a `not found` basis naming where the ladder looked -- a refusal is distinguishable from a candidate nobody considered"}

     :negative/planted-unknown-candidate-reaches-rung-3
     {:classification (dissoc (classify-one field ladder/default-relation planted-unknown hist)
                              :task-belief/derivation)
      :pass? (= 3 (:task-belief/rung (classify-one field ladder/default-relation planted-unknown hist)))
      :why "U51's negative-1: a candidate of an unseen type, in a repository no candidate lives in, with no document, must reach rung 3"}

     :positive/the-same-plant-retyped-reaches-rung-2
     {:classification (dissoc (classify-one field :k-type
                                            (assoc planted-unknown :type :advance-mission) hist)
                              :task-belief/derivation)
      :pass? (= 2 (:task-belief/rung (classify-one field :k-type
                                                   (assoc planted-unknown :type :advance-mission) hist)))
      :why "U51's positive-4, and the reason the rung-3 answers mean anything: the SAME plant retyped classifies rung 2 under :k-type alone, so the classifier is not answering `nothing` because the candidate is unknown to it"}

     :positive/a-recorded-chosen-key-reaches-rung-1
     {:chosen chosen-key
      :decisions (get hist chosen-key 0)
      :rung (:task-belief/rung (classify-one field ladder/default-relation
                                             (first (filter #(= chosen-key (ladder/action-key %))
                                                            (:candidates field)))
                                             hist))
      :pass? (= 1 (:task-belief/rung (classify-one field ladder/default-relation
                                                   (first (filter #(= chosen-key (ladder/action-key %))
                                                                  (:candidates field)))
                                                   hist)))
      :why "the key this very tick chose is in the corpus and must classify rung 1"}

     :negative/empty-history-refuses-everything-in-scope
     {:census (:census empty-arm)
      :refused (count (:refusals empty-arm))
      :pass? (and (zero? (get (:census empty-arm) 1 0))
                  (zero? (get (:census empty-arm) 2 0))
                  (pos? (get (:census empty-arm) 3 0)))
      :why "with the case-history index emptied every in-scope candidate reaches rung 3, so the rung-1 and rung-2 answers are reading THAT index and not a field correlated with it"}

     :positive/distinct-factors-give-distinct-scores
     {:subject :advance-mission
      :rows (count in-scope-scored)
      :distinct-factors (count by-factor)
      :distinct-scores (count (group-by :controller-score in-scope-scored))
      :collisions (count (filter (fn [[_ rows]] (> (count (distinct (map :controller-score rows))) 1))
                                 by-factor))
      :pass? (and (= (count by-factor) (count (group-by :controller-score in-scope-scored)))
                  (zero? (count (filter (fn [[_ rows]]
                                          (> (count (distinct (map :controller-score rows))) 1))
                                        by-factor))))
      :why ":mission-value-factor reaches the controller score by exactly one path (forward-model/mission-value-factor into predict-effects :advance-mission), and over the :advance-mission rows the map from factor to score is injective BOTH WAYS -- so `the plateau is 8 values wide` and `8 distinct factors` are the same statement, and the plateau count is not an artefact of counting factors instead of scores"}

     :positive/out-of-scope-candidates-are-untouched
     (let [oos (filterv #(= :out-of-scope (:task-belief/rung %)) (:candidates (:result on)))
           in-oos (filterv #(not (ladder/in-scope? %)) (:candidates field))]
       {:out-of-scope (count oos) :in-field (count in-oos)
        :changed (count (remove (fn [a] (some #(= (dissoc a :task-belief/rung) %) in-oos)) oos))
        :pass? (and (= (count oos) (count in-oos))
                    (every? (fn [a] (some #(= (dissoc a :task-belief/rung) %) in-oos)) oos))
        :why "the ladder scores task belief about missions; :no-op, :address-sorry and :fire-pattern pass through with a scope marker and nothing else changed -- refusing them would remove the machine's fallback moves for a reason this row has not measured"})}))

;; ---------------------------------------------------------------------------
;; the mint payloads (built here, appended by u52_mint_refusals.bb)
;; ---------------------------------------------------------------------------

;; THE STRUCTURED RUN KEY IS NOT PASSED FOR AN ALREADY-MINTED FIELD (:U60), and
;; the omission is derived from the field rather than decided here. `:s5` and
;; `:re5` were minted into the tension ledger on 2026-09-04, before
;; `:tension/provenance :records` existed; `append-tension!` is
;; `:already-present` only for a payload that matches the committed one exactly,
;; so adding the key to those two would turn `u52_mint_refusals.bb --append`
;; from a documented no-op into the append-only ledger's identity-conflict
;; refusal. Each field says for itself whether its tension is already committed
;; (`:minted-before-the-run-key`), a NEW field carries the key, and control 8t
;; checks that the omission set is exactly the tension ids the committed ledger
;; already holds -- so the day one of these is re-minted under a new id the
;; omission goes away without an edit here.

(defn mint-payload [field arm]
  (ladder/refusal-tension
   {:subject-id (keyword (str (name (:id field)) "-" (subs (:run-id field) 0 8)))
    :refusals (:refusals (:result arm))
    :relation (:relation arm)
    :records (when-not (:minted-before-the-run-key field) [(:run-id field)])
    :artifact (or (:artifact field)
                  "futon2/holes/labs/wm-contract/runs/U52-ladder/04-refusals.edn")
    ;; the field's own date, not the report's pinned `as-of`, when it has one:
    ;; a tension minted from a run stepped on another day would otherwise be
    ;; dated by when this report was written
    :at (or (:at field) as-of)
    :by "U52 ladder rung-3 refusal mint"
    :carried-by "M-wm-aif-policy-grain-compliance"
    :pointers (into (vec (:extra-pointers field))
                    [(or (:artifact field)
                         "futon2/holes/labs/wm-contract/runs/U52-ladder/04-refusals.edn")
                     "futon2/holes/labs/wm-contract/C507-u52-three-rung-ladder.md"
                     (str "futon2/" (:trace field) " (run " (:run-id field) ")")])}))

;; ---------------------------------------------------------------------------
;; main
;; ---------------------------------------------------------------------------

(defn write! [name data]
  (io/make-parents (io/file out-dir name))
  (spit (io/file out-dir name) (with-out-str (pp/pprint data)))
  (println "  wrote" (str (io/file out-dir name))))

(defn -main []
  (io/make-parents (io/file out-dir "x"))
  (let [fs (mapv field-of fields)
        hist @case-history
        offs (mapv off-arm fs)
        ons (mapv #(ladder-arm % ladder/default-relation) fs)
        rel-arms (into {} (for [f fs]
                            [(:id f)
                             (into {} (for [r (sort (keys ladder/relations))]
                                        (let [a (ladder-arm f r)]
                                          [r (dissoc a :result :scored)])))]))
        ctrls (into {} (map (fn [f o n] [(:id f) (controls f o n)]) fs offs ons))
        all-pass? (every? true? (map :pass? (mapcat vals (vals ctrls))))]

    (write! "00-inputs.edn"
            {:row :U52
             :as-of as-of
             :ruling "futon2/holes/labs/wm-contract/aif-equations.edn:364-369 (:task-belief-actand-source, Joe 2026-09-03)"
             :measures-against "futon2/holes/labs/wm-contract/runs/U51-plateau-composition/plateau-composition.edn"
             :flag "FUTON_WM_TASK_BELIEF_LADDER=1 (futon2/scripts/futon2/report/war_machine.clj:252-283), DEFAULT OFF"
             :seam "futon2/scripts/futon2/report/war_machine.clj:6273-6274 -- between enrichment and efe/rank-actions"
             :declared-inputs {:relation ladder/default-relation
                               :relation-statement (:statement (get ladder/relations ladder/default-relation))
                               :generalization-discount ladder/default-generalization-discount
                               :support-map "n/(n+1)"
                               :note "DECLARED, NOT RULED. Neither the relation nor the support map is derived from a source; they are this row's inputs, reachable per call, and the :choices entry that would settle them is Joe's. The default-off flag is what keeps that honest."}
             :corpus {:dir trace-dir :distinct-chosen-keys (count hist)
                      :total-decisions (reduce + 0 (vals hist))
                      :cross-check "U51 read the same corpus and reported 17 distinct chosen keys over 889 records (C506-u51-plateau-composition.md); the shipped index reproduces both."
                      :in-scope-keys (count (filter (fn [[[t _] _]] (contains? ladder/in-scope-types t)) hist))
                      :note "case history means CHOSEN, never CHOSEN AND IT WORKED -- U51 measured 0 of the chosen keys carrying an outcome. 146 of the 889 decisions chose [:learn-action-class nil], a typed action with no target: counted in the index, excluded by the ladder's scope rule."}
             :fields (mapv (fn [f] {:id (:id f) :trace (:trace f) :run-id (:run-id f)
                                    :timestamp (:timestamp f) :field-size (count (:candidates f))
                                    :chosen (:chosen f)
                                    :recorded-plateau (dissoc (plateau-members f) :keys)})
                           fs)})

    (write! "01-off-arm.edn"
            {:statement "The default is the identity, and the replay is the shipped scoring path. Both claims are one artifact because the second is what makes the first worth reading."
             :arms (into {} (map (fn [f o] [(:id f) o]) fs offs))})

    (write! "02-ladder-arm.edn"
            {:relation ladder/default-relation
             :arms (into {} (map (fn [f a] [(:id f) (dissoc a :result :scored)]) fs ons))
             :per-plateau-member
             (into {}
                   (map (fn [f a]
                          (let [pm (set (:keys (plateau-members f)))
                                refused (into {} (map (juxt :refusal/action-key identity))
                                              (:refusals (:result a)))
                                kept (into {} (map (juxt ladder/action-key identity))
                                           (:candidates (:result a)))]
                            [(:id f)
                             (vec (for [k (:keys (plateau-members f))
                                        :when (pm k)]
                                    (if-let [c (kept k)]
                                      {:key k
                                       :rung (:task-belief/rung c)
                                       :support (:task-belief/support c)
                                       :ladder-factor (:task-belief/factor c)
                                       :mission-value-factor (:mission-value-factor c)
                                       :pre-ladder-mission-value-factor (:task-belief/pre-ladder-mission-value-factor c)
                                       :constructed (:task-belief/constructed c)
                                       :derivation (:task-belief/derivation c)}
                                      {:key k :rung 3 :refused (refused k)})))]))
                        fs ons))})

    (write! "03-plateau.edn"
            {:statement "The acceptance: what the ladder does to the recorded plateau, under each of the four kin relations U51 measured."
             :default-relation ladder/default-relation
             :under-the-default (into {} (map (fn [f a] [(:id f) (plateau-report f a)]) fs ons))
             :under-each-relation
             (into {} (for [f fs]
                        [(:id f)
                         (into {} (for [r (sort (keys ladder/relations))]
                                    [r (let [a (ladder-arm f r)]
                                         (assoc (plateau-report f a)
                                                :census (:census a)
                                                :field-width-census (:width-census a)))]))]))
             :relation-arms rel-arms})

    (write! "04-refusals.edn"
            {:statement "The rung-3 partition of each field: one typed refusal record per candidate, and one U41 tension/event pair per partition. The pairs are BUILT here and APPENDED by u52_mint_refusals.bb, because the ledger append is U41's sole write API."
             :reason ladder/refusal-reason
             :per-field
             (into {} (map (fn [f a]
                             [(:id f)
                              {:refused (count (:refusals (:result a)))
                               :records (:refusals (:result a))
                               :mint (mint-payload f a)}])
                           fs ons))})

    (write! "05-controls.edn" {:all-pass? all-pass? :per-field ctrls})

    (println (format "u52_ladder: %d field(s); controls %s"
                     (count fs) (if all-pass? "ALL PASS" "FAILED")))
    (doseq [[fid c] ctrls]
      (println (format "  %s: %s"
                       (name fid)
                       (str/join " " (for [[k v] (sort-by key c)]
                                       (str (name k) "=" (if (:pass? v) "ok" "FAIL")))))))
    (when-not all-pass? (System/exit 1))))

(-main)
