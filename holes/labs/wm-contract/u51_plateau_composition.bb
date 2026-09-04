#!/usr/bin/env bb
;; U51 -- THE PLATEAU'S COMPOSITION, partitioned by ladder rung.
;;
;;   bb holes/labs/wm-contract/u51_plateau_composition.bb
;;
;; READ-ONLY DISCOVERY. Reads the two committed run-store traces, the committed
;; RE4 rationale store, and the recorded wm-trace corpus under data/; writes only
;; under holes/labs/wm-contract/runs/U51-plateau-composition/. No tick, no run
;; lock, no substrate call, no network, nothing under data/ is written or
;; touched, no scoring or selection code is read at run time and none is changed.
;;
;; WHAT IT PRODUCES. For the s5 55-wide plateau and the re5 field, one row per
;; candidate saying which rung of the ratified zero-support ladder
;; (aif-equations.edn :choices :task-belief-actand-source, Joe 2026-09-03) could
;; score it:
;;
;;   rung 1  DIRECT CASE HISTORY  -- the candidate's own action key occurs as a
;;           persisted decision's chosen action somewhere in the recorded corpus.
;;   rung 2  CONSTRUCTIVE GENERALIZATION -- no case history of its own, but a
;;           KIN candidate has some, under a named kin relation.
;;   rung 3  NOTHING -- typed-refusal territory.
;;
;; THE KIN RELATION IS NOT DECLARED FOR THIS GRAIN and this row does not declare
;; it. The ruling names "kin actand-classes in the same table, shared patterns,
;; cascade-catalog playout"; at :arm-session grain the v1 relation is "same arm +
;; same correction-label" (zaif worklist :U11g). Nothing equivalent is written
;; down at mission grain, so FOUR candidate relations are measured, each
;; computable from recorded fields alone, and the rung-2 count is reported per
;; relation plus as their UNION -- an upper bound on how much rung 2 can drain,
;; not a choice among them. Choosing one is U52's design question or a :choices
;; entry; no ruling is written here.
;;
;; DETERMINISM. No wall-clock field is written. Every number is read off a record
;; or a committed file; two runs over an unchanged tree are byte-identical.

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.set :as set]
         '[clojure.string :as str])

(def repo-root (str (System/getProperty "user.home") "/code/futon2"))
(def lab (io/file repo-root "holes/labs/wm-contract"))
(def out-dir (io/file lab "runs/U51-plateau-composition"))

;; Old trace records carry tagged literals this script has no business
;; interpreting (U39's reader, same reason).
(def read-opts {:default (fn [t v] {:unread-tag t :value v})})

(defn read-trace [path]
  (with-open [r (io/reader (io/file path))]
    (mapv #(edn/read-string read-opts %) (line-seq r))))

(defn akey [a] [(:type a) (:target a)])

(defn trace-outcome
  "The three places `war-machine/trace-outcome` looks (war_machine.clj:2391-2394).
   U39 and U40 cite this as war_machine.clj:2328-2332; that range has since
   drifted onto compute-delta-t-mission's body and is recorded as drift in
   C506, not repaired here."
  [m]
  (or (:outcome m) (get-in m [:enactment :outcome]) (get-in m [:realized-outcome :outcome])))

;; ---------------------------------------------------------------------------
;; the recorded corpus -- the case-history table
;; ---------------------------------------------------------------------------

(defn corpus-files []
  (->> (file-seq (io/file repo-root "data/wm-trace"))
       (filter #(.isFile ^java.io.File %))
       (map str)
       (filter #(re-find #"wm-trace-\d{4}-\d{2}-\d{2}\.edn$" %))
       sort
       vec))

(def corpus (delay (vec (mapcat read-trace (corpus-files)))))

(def chosen-history
  "action key -> how many persisted decisions chose it."
  (delay (frequencies (keep #(some-> (get-in % [:decision :action]) akey) @corpus))))

(def chosen-history-with-outcome
  (delay (frequencies (keep (fn [m] (when (trace-outcome m)
                                      (some-> (get-in m [:decision :action]) akey)))
                            @corpus))))

(def chosen-occurrences
  "action key -> the [timestamp run-id] of every persisted decision that chose it,
   so `case history` can be counted AS OF a tick rather than over the whole
   corpus (the corpus contains the very runs being partitioned)."
  (delay (reduce (fn [acc m]
                   (if-let [k (some-> (get-in m [:decision :action]) akey)]
                     (update acc k (fnil conj []) [(str (:timestamp m)) (str (:run/id m))])
                     acc))
                 {} @corpus)))

(def mission-history
  "mission target -> decisions choosing it under ANY action type (kin grain)."
  (delay (reduce (fn [acc [[_ target] n]]
                   (if (string? target) (update acc target (fnil + 0) n) acc))
                 {} @chosen-history)))

;; ---------------------------------------------------------------------------
;; the fields
;; ---------------------------------------------------------------------------

(defn widest-plateau
  "The widest set of candidates sharing one controller-score, ties on width
   broken by the lower (better) rank so the pick is deterministic."
  [ranked]
  (let [by-score (group-by :controller-score ranked)
        best (->> by-score
                  (sort-by (fn [[_ ms]] [(- (count ms)) (apply min (map :rank ms))]))
                  first)]
    {:score (key best)
     :members (vec (sort-by :rank (val best)))}))

(defn field-of [trace-path run-id]
  (let [recs (read-trace trace-path)
        rec (if run-id (first (filter #(= run-id (:run/id %)) recs)) (first recs))
        ranked (vec (sort-by :rank (:ranked-actions rec)))
        pl (widest-plateau ranked)]
    {:trace trace-path
     :run-id (:run/id rec)
     :timestamp (:timestamp rec)
     :chosen (akey (get-in rec [:decision :action]))
     :chosen-rank (some (fn [m] (when (= (akey (:action m)) (akey (get-in rec [:decision :action])))
                                  (:rank m)))
                        ranked)
     :field-size (count ranked)
     :ranked ranked
     :plateau-score (:score pl)
     :plateau (:members pl)
     :plateau-ranks [(apply min (map :rank (:members pl))) (apply max (map :rank (:members pl)))]}))

;; ---------------------------------------------------------------------------
;; the four candidate kin relations
;; ---------------------------------------------------------------------------

(defn home-repo [m]
  (some->> (get-in m [:action :mission-path]) str (re-find #"/home/joe/code/([^/]+)/") second))

(defn produces [m]
  (set (get-in m [:goal-outcome-replay-inputs :capability-graph-input :produces])))

(def mission-id-re #"\b(M-[A-Za-z0-9][A-Za-z0-9._-]*?)(?=[\s`'\",.;:)\]}]|\.md|$)")

(defn doc-refs
  "The candidate's own mission id together with the missions its document names.
   Two candidates are kin under this relation when one document names the other
   mission, or both name a mission in common. The carrier is the mission docs'
   own cross-reference prose -- declared by their authors, not invented here."
  [m]
  (let [p (get-in m [:action :mission-path])
        self (get-in m [:action :target])
        named (if (and p (.exists (io/file (str p))))
                (set (map #(str/replace (second %) #"\.md$" "") (re-seq mission-id-re (slurp (str p)))))
                #{})]
    (cond-> named (string? self) (conj self))))

(def relation-defs
  "Relation id -> {:statement, :token-fn}. The token function is applied to any
   candidate map, so a planted candidate is classified by the same code path as a
   recorded one rather than by falling out of a lookup table."
  {:k-type {:statement "same action :type"
            :token-fn (fn [m] (if-let [t (get-in m [:action :type])] #{t} #{}))}
   :k-repo {:statement "same home repository, read off :action :mission-path"
            :token-fn (fn [m] (if-let [r (home-repo m)] #{r} #{}))}
   :k-produces {:statement "shares a :produces entry of the capability-graph input recorded on the candidate"
                :token-fn produces}
   :k-doc-xref {:statement "one document names the other mission, or both name a mission in common (the mission docs' own cross-references)"
                :token-fn doc-refs}})

(defn kin-classes
  "Relation-id -> {:statement :token-fn :tokens}, where :tokens is the token set
   of every candidate in this tick's field. Two candidates are kin under a
   relation when their token sets intersect."
  [field]
  (into {}
        (for [[r {:keys [statement token-fn]}] relation-defs]
          [r {:statement statement
              :token-fn token-fn
              :tokens (into {} (map (fn [m] [(akey (:action m)) (token-fn m)]) (:ranked field)))}])))

(defn kin-with-history
  "For relation `rel`, the case-history-bearing keys kin to candidate `m` (its own
   key excluded). The candidate's tokens are COMPUTED, not looked up, so a
   candidate that is not in the field is classified by the same rule."
  [rel k m]
  (let [toks ((:token-fn rel) m)]
    (when (seq toks)
      (->> (:tokens rel)
           (filter (fn [[k2 t2]] (and (not= k2 k) (seq (set/intersection toks t2)))))
           (map first)
           (filter #(pos? (get @chosen-history % 0)))
           sort
           vec))))

;; ---------------------------------------------------------------------------
;; the partition
;; ---------------------------------------------------------------------------

(def relation-order [:k-type :k-repo :k-produces :k-doc-xref])

(defn classify [rels as-of m]
  (let [k (akey (:action m))
        direct (get @chosen-history k 0)
        outcome-bearing (get @chosen-history-with-outcome k 0)
        ;; kin at MISSION grain: a mission chosen under another action type is
        ;; still case history about that mission, so both are reported.
        as-mission (get @mission-history (second k) 0)
        occ (get @chosen-occurrences k [])
        before (count (filter #(neg? (compare (first %) as-of)) occ))
        ;; :run/id is minted per TICK, so "the same run" is not a run-id test;
        ;; the day the tick was taken is the coarsest honest self-exclusion.
        same-day (count (filter #(= (subs (first %) 0 (min 10 (count (first %))))
                                    (subs as-of 0 (min 10 (count as-of)))) occ))
        kin (into {} (for [r relation-order
                           :let [hits (kin-with-history (get rels r) k m)]]
                       [r (vec hits)]))
        kin-any (vec (sort (distinct (mapcat val kin))))]
    (cond-> {:key k
             :rank (:rank m)
             :controller-score (:controller-score m)
             :mission-path (get-in m [:action :mission-path])
             :open-hole-count (get-in m [:action :open-hole-count])
             :direct-decisions direct
             :direct-decisions-before-this-tick before
             :direct-decisions-same-day same-day
             :direct-decisions-first (first (sort (map first occ)))
             :direct-decisions-with-outcome outcome-bearing
             :decisions-naming-this-mission as-mission
             :kin-with-history kin
             :rung (cond (pos? direct) 1 (seq kin-any) 2 :else 3)}
      (pos? direct)
      (assoc :grounds {:rule :direct-case-history
                       :basis (str "chosen action of " direct " persisted decision(s) in the recorded corpus")})
      (and (zero? direct) (seq kin-any))
      (assoc :grounds {:rule :kin-with-case-history
                       :relations (vec (sort (keep (fn [[r hits]] (when (seq hits) r)) kin)))
                       :basis "no decision chose this key; a kin key under the relation(s) named was chosen"})
      (and (zero? direct) (empty? kin-any))
      (assoc :grounds {:rule :nothing-found
                       :basis "not found: no persisted decision chose this key, and no kin key with case history under any measured relation"}))))

(defn refinement
  "How much a relation could actually DRAIN the plateau. A relation whose kin
   classes do not refine the plateau pools every member with the same evidence
   and hands them all the same constructed value -- which is the plateau again,
   one derivation deeper. Reported as the number of distinct kin signatures over
   the plateau and the size of the largest group sharing one."
  [rels field]
  (into {}
        (for [r relation-order
              :let [rel (get rels r)
                    sigs (mapv (fn [m] (let [k (akey (:action m))]
                                         [((:token-fn rel) m)
                                          (set (kin-with-history rel k m))]))
                               (:plateau field))
                    groups (vals (group-by identity sigs))]]
          [r {:distinct-kin-signatures (count (distinct sigs))
              :largest-group-sharing-one-signature (apply max (map count groups))
              :members-with-no-kin-history (count (filter #(empty? (second %)) sigs))}])))

(defn partition-field [field]
  (let [rels (kin-classes field)
        as-of (str (:timestamp field))
        rows (mapv #(classify rels as-of %) (:plateau field))
        by-rung (frequencies (map :rung rows))
        per-relation (into {} (for [r relation-order]
                                [r (count (filter (fn [row] (and (zero? (:direct-decisions row))
                                                                 (seq (get-in row [:kin-with-history r]))))
                                                  rows))]))
        field-rows (mapv #(classify rels as-of %) (:ranked field))]
    {:run-id (:run-id field)
     :trace (str/replace (:trace field) (str repo-root "/") "")
     :timestamp (:timestamp field)
     :field-size (:field-size field)
     :chosen (:chosen field)
     :chosen-rank (:chosen-rank field)
     :chosen-in-plateau? (boolean (some #(= (:key %) (:chosen field)) rows))
     :plateau {:width (count rows)
               :score (:plateau-score field)
               :ranks (:plateau-ranks field)
               :types (frequencies (map #(first (:key %)) rows))}
     :rung-counts {:rung-1 (get by-rung 1 0) :rung-2 (get by-rung 2 0) :rung-3 (get by-rung 3 0)}
     :rung-1-excluding-same-day
     (count (filter #(pos? (- (:direct-decisions %) (:direct-decisions-same-day %))) rows))
     :rung-1-available-before-this-tick
     (count (filter #(pos? (:direct-decisions-before-this-tick %)) rows))
     :rung-2-per-relation per-relation
     :rung-2-refinement (refinement rels field)
     :whole-field-rung-counts (let [f (frequencies (map :rung field-rows))]
                                {:rung-1 (get f 1 0) :rung-2 (get f 2 0) :rung-3 (get f 3 0)})
     ;; SCOPE, stated because it bounds the rung-2 numbers in one direction:
     ;; kin is searched among the tick's OWN recorded candidates, because those
     ;; are the entries whose kin-bearing fields the trace record carries. Keys
     ;; with case history that are absent from this field are listed here; a kin
     ;; search extended to them can only move candidates from rung 3 to rung 2,
     ;; never the other way.
     :case-history-keys-not-in-this-field
     (let [in-field (set (map :key field-rows))]
       (vec (sort-by (comp - val) (remove #(in-field (key %)) @chosen-history))))
     :rows rows
     :field-rows field-rows}))

;; ---------------------------------------------------------------------------
;; what the plateau's own rows would let a score read
;; ---------------------------------------------------------------------------

(defn discrimination-census
  "Which recorded fields take more than one value ACROSS the plateau. A field
   constant over the plateau cannot break it; a field that varies is material a
   rung-2 derivation could read."
  [field]
  (let [pl (:plateau field)
        top-keys (sort (distinct (mapcat keys pl)))
        act-keys (sort (distinct (mapcat #(keys (:action %)) pl)))
        count-distinct (fn [f] (count (distinct (map f pl))))]
    {:plateau-width (count pl)
     :varying-top-level (vec (sort (filter #(> (count-distinct %) 1) top-keys)))
     :constant-top-level (vec (sort (filter #(= 1 (count-distinct %)) top-keys)))
     :action-field-distinct-values (into (sorted-map)
                                         (map (fn [k] [k (count-distinct #(get-in % [:action k]))]) act-keys))
     :scored-terms-constant? (every? #(= 1 (count-distinct %))
                                     [:controller-score :G-core :G-efe :G-risk :G-ambiguity :G-goal-outcome
                                      :habit-prior-bias :predictability-bonus :homeostatic-pressure
                                      :structural-pressure :graph-feasibility-penalty :gap-exploration-bonus])
     :mission-value-inputs (into (sorted-map)
                                 (map (fn [k] [k (frequencies (map #(get-in % [:action k]) pl))])
                                      [:central :strategic :doable :phase :mission-value-factor
                                       :non-progress-decay :operator-gate-factor :completion-gate-factor]))
     :open-hole-count (into (sorted-map) (frequencies (map #(get-in % [:action :open-hole-count]) pl)))
     :home-repository (into (sorted-map) (frequencies (map home-repo pl)))
     :capability-graph-produces (into (sorted-map) (frequencies (map #(count (produces %)) pl)))}))

;; ---------------------------------------------------------------------------
;; is the plateau a property of ONE tick or of the run?
;; ---------------------------------------------------------------------------

(defn s5-tick-ids
  "The four ticks of run 2026-09-01-s5, taken from the committed RE4 rationale
   store rather than from a list written here."
  []
  (->> (.listFiles (io/file lab "runs/RE4-rationale-logging/store"))
       (map str) (filter #(str/ends-with? % ".edn")) sort
       (mapv #(:rationale/run-id (edn/read-string read-opts (slurp %))))))

(defn plateau-stability
  "Per tick: the widest plateau's width, score, rank band and membership, and
   what the four memberships have in common."
  [recs]
  (let [per (mapv (fn [rec]
                    (let [ranked (vec (sort-by :rank (:ranked-actions rec)))
                          pl (widest-plateau ranked)
                          ks (set (map #(akey (:action %)) (:members pl)))
                          chosen (akey (get-in rec [:decision :action]))]
                      {:run-id (:run/id rec) :timestamp (:timestamp rec)
                       :width (count (:members pl)) :score (:score pl)
                       :ranks [(apply min (map :rank (:members pl)))
                               (apply max (map :rank (:members pl)))]
                       :chosen chosen
                       :chosen-in-plateau? (contains? ks chosen)
                       :chosen-direct-decisions (get @chosen-history chosen 0)
                       :keys ks}))
                  (sort-by :timestamp recs))
        u (apply set/union (map :keys per))
        i (apply set/intersection (map :keys per))]
    {:ticks (mapv #(dissoc % :keys) per)
     :widths (mapv :width per)
     :membership-union (count u)
     :membership-intersection (count i)
     :members-not-on-every-tick (vec (sort (set/difference u i)))
     :every-tick-chose-a-plateau-member? (every? :chosen-in-plateau? per)
     :every-tick-chose-a-case-history-candidate? (every? #(pos? (:chosen-direct-decisions %)) per)}))

;; ---------------------------------------------------------------------------
;; controls
;; ---------------------------------------------------------------------------

(defn controls [field]
  (let [rels (kin-classes field)
        as-of (str (:timestamp field))
        planted {:rank -1 :controller-score 0.0
                 :action {:type :fire-pattern :target "u51/planted-nonexistent-pattern"
                          :mission-path "/home/joe/code/u51-planted-nonexistent-repo/holes/missions/M-u51-planted.md"}}
        planted-row (classify rels as-of planted)
        planted-kin (assoc-in planted [:action :type] :advance-mission)
        planted-kin-row (classify rels as-of planted-kin)
        history-keys (filter #(pos? (get @chosen-history (:key %) 0))
                             (map #(classify rels as-of %) (:ranked field)))
        positive (first (sort-by :rank history-keys))]
    {:negative-1
     {:statement "a planted candidate that no decision ever chose, of a type no chosen key in the field carries, in a repository no candidate lives in, with no document and no capability-graph produces, must classify rung 3"
      :expected 3 :actual (:rung planted-row) :pass (= 3 (:rung planted-row))
      :grounds (:grounds planted-row)}
     :positive-4
     {:statement "the SAME planted candidate typed :advance-mission must classify rung 2 under :k-type -- the classifier is not answering `nothing` because the candidate is unknown to it, and the degenerate relation admits anything of the right type"
      :expected 2 :actual (:rung planted-kin-row)
      :relations (get-in planted-kin-row [:grounds :relations])
      :pass (and (= 2 (:rung planted-kin-row))
                 (= [:k-type] (get-in planted-kin-row [:grounds :relations])))}
     :negative-2
     {:statement "no candidate anywhere in the field carries outcome-bearing case history (the ranking era and the outcome era are disjoint, U39)"
      :expected 0
      :actual (count (filter #(pos? (:direct-decisions-with-outcome %)) (map #(classify rels as-of %) (:ranked field))))
      :pass (zero? (count (filter #(pos? (:direct-decisions-with-outcome %)) (map #(classify rels as-of %) (:ranked field)))))}
     :positive-1
     (if positive
       {:statement "a candidate the corpus records as chosen must classify rung 1, citing its own decision count"
        :key (:key positive) :decisions (:direct-decisions positive)
        :expected 1 :actual (:rung positive) :pass (= 1 (:rung positive))}
       {:statement "a candidate the corpus records as chosen must classify rung 1"
        :pass false :actual :no-such-candidate-in-field})
     :positive-3
     (let [ranked (:ranked field)
           zero-support? (fn [m] (and (= :advance-mission (get-in m [:action :type]))
                                      (= 0.0 (get-in m [:action :central]))
                                      (= 0.0 (get-in m [:action :strategic]))
                                      (nil? (get-in m [:action :phase]))
                                      (= 1.0 (get-in m [:action :operator-gate-factor]))
                                      (= 1.0 (get-in m [:action :completion-gate-factor]))
                                      (= 1.0 (get-in m [:action :non-progress-decay]))))
           predicted (set (map #(akey (:action %)) (filter zero-support? ranked)))
           actual (set (map #(akey (:action %)) (:plateau field)))
           ;; default-mission-value-weights :doable 0.30 (war_machine.clj:2358-2361)
           ;; x phase-doability "unknown" 0.3 (war_machine.clj:2363-2373), the
           ;; other three terms contributing 0.0 (war_machine.clj:2569-2571),
           ;; times decay 1.0 (war_machine.clj:2630).
           arithmetic (double (* 0.30 0.3))]
       {:statement "the plateau is exactly the extension of a predicate over recorded fields -- an :advance-mission candidate with zero centrality, no cascade role, an unresolvable phase, no operator gate, no completion gate and no non-progress decay -- and its shared score's mission-value-factor is the doable weight times the unknown-phase doability"
        :predicate-extension (count predicted)
        :plateau (count actual)
        :set-equal (= predicted actual)
        :mission-value-factor-recomputed arithmetic
        :mission-value-factor-recorded (vec (distinct (map #(get-in % [:action :mission-value-factor]) (:plateau field))))
        :pass (and (= predicted actual)
                   (= [arithmetic] (vec (distinct (map #(get-in % [:action :mission-value-factor]) (:plateau field))))))})
     :positive-2
     {:statement "the plateau's own controller-score is constant by construction, so partitioning it cannot be an artefact of score arithmetic"
      :distinct-scores (count (distinct (map :controller-score (:plateau field))))
      :expected 1 :pass (= 1 (count (distinct (map :controller-score (:plateau field)))))}}))

;; ---------------------------------------------------------------------------

(defn -main []
  (.mkdirs out-dir)
  (let [s5 (field-of (str lab "/runs/2026-09-01-s5/wm-trace-s5.edn") nil)
        re5-recs (read-trace (str lab "/runs/2026-09-04-re5/wm-trace-re5.edn"))
        re5-fields (mapv #(field-of (str lab "/runs/2026-09-04-re5/wm-trace-re5.edn") (:run/id %)) re5-recs)
        re5 (first (sort-by :timestamp re5-fields))
        s5-part (partition-field s5)
        re5-part (partition-field re5)
        artifact
        {:schema :wm/u51-plateau-composition-v1
         :row :U51
         :produced-by "holes/labs/wm-contract/u51_plateau_composition.bb"
         :read-only true
         :behaviour-change :none
         :corpus {:files (mapv #(str/replace % (str repo-root "/") "") (corpus-files))
                  :file-count (count (corpus-files))
                  :records (count @corpus)
                  :distinct-chosen-keys (count @chosen-history)
                  :chosen-keys-with-outcome (count @chosen-history-with-outcome)
                  :note "data/wm-trace is untracked (.gitignore:49); the corpus leg is reproducible only on a machine that holds it, exactly as U39 and U40 read it"}
         :ladder {:source "aif-equations.edn :choices :task-belief-actand-source (ruled by Joe 2026-09-03)"
                  :rung-1 "direct case history"
                  :rung-2 "constructive generalization from kin, marked :constructed with its derivation"
                  :rung-3 "typed refusal, minting a tension record"}
         :kin-relations-measured
         (into {} (for [r relation-order] [r (:statement (get (kin-classes s5) r))]))
         :kin-relation-declared? false
         :fields {:s5 s5-part :re5 re5-part}
         :discrimination {:s5 (discrimination-census s5) :re5 (discrimination-census re5)}
         :plateau-stability
         {:s5 (assoc (plateau-stability (let [ids (set (s5-tick-ids))]
                                          (filter #(ids (:run/id %))
                                                  (read-trace (str repo-root "/data/wm-trace/wm-trace-2026-09-01.edn")))))
                     :source "data/wm-trace/wm-trace-2026-09-01.edn restricted to the four tick ids of runs/RE4-rationale-logging/store"
                     :tick-ids (s5-tick-ids))
          :re5 (assoc (plateau-stability re5-recs)
                      :source "holes/labs/wm-contract/runs/2026-09-04-re5/wm-trace-re5.edn (all four ticks are committed)")}
         :controls {:s5 (controls s5) :re5 (controls re5)}}
        all-controls (mapcat vals (vals (:controls artifact)))
        failed (remove :pass all-controls)]
    (spit (io/file out-dir "plateau-composition.edn")
          (with-out-str (pp/pprint artifact)))
    (println "U51 plateau composition")
    (doseq [[nm p] [["s5" s5-part] ["re5" re5-part]]]
      (println (format "  %-4s run %s field %d, plateau %d wide at ranks %s, chosen %s at rank %s (in plateau: %s)"
                       nm (:run-id p) (:field-size p) (:width (:plateau p))
                       (pr-str (:ranks (:plateau p))) (pr-str (:chosen p)) (:chosen-rank p)
                       (:chosen-in-plateau? p)))
      (println (format "        rung-1 available before this tick %d, still rung-1 with the tick's own day excluded %d"
                       (:rung-1-available-before-this-tick p) (:rung-1-excluding-same-day p)))
      (println (format "        rungs %s ; rung-2 per relation %s ; whole field %s"
                       (pr-str (:rung-counts p)) (pr-str (:rung-2-per-relation p))
                       (pr-str (:whole-field-rung-counts p))))
      (doseq [[r v] (:rung-2-refinement p)]
        (println (format "        %-12s %d distinct kin signatures over the plateau, largest group %d, no-kin %d"
                         (name r) (:distinct-kin-signatures v)
                         (:largest-group-sharing-one-signature v)
                         (:members-with-no-kin-history v)))))
    (doseq [[nm k] [["s5" :s5] ["re5" :re5]]]
      (let [st (get-in artifact [:plateau-stability k])]
        (println (format "  %-4s plateau over its four ticks: widths %s, union %d / intersection %d, every tick chose a plateau member %s, a case-history candidate %s"
                         nm (pr-str (:widths st)) (:membership-union st) (:membership-intersection st)
                         (:every-tick-chose-a-plateau-member? st)
                         (:every-tick-chose-a-case-history-candidate? st)))))
    (println (format "  controls: %d run, %d failed" (count all-controls) (count failed)))
    (doseq [c failed] (println "    FAILED" (pr-str c)))
    (when (seq failed) (System/exit 1))))

(-main)
