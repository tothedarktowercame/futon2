(ns futon2.aif.arguing-worlds
  "Ungrounded v0 for M-arguing-worlds.

  The referee yardstick is realized rollout S(pi), not cascade C. C is only used
  to define the single-buildout baseline that the dialectic must beat. Diversity
  is checked before judging; a non-diverse generator returns :monotone-generator."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [futon2.aif.rollout :as rollout]))



(def default-freeze-path
  "/home/joe/code/futon2/holes/labs/E-cascade-sampler-sampler/wm-judgement-freeze-2026-06-12.json")

(def default-circumstances-path
  "/home/joe/code/futon2/holes/labs/E-cascade-sampler-sampler/circumstances-v0.edn")

(def default-lenses
  [{:lens/id :capability-ascent
    :description "Prefer capability-advancing moves."
    :class-weights {:advance-capability 1.45 :close-hole 0.85 :centre-mess 0.8 :graft-pattern 1.0}}
   {:lens/id :hole-closure
    :description "Prefer concrete close-hole work."
    :class-weights {:close-hole 1.55 :centre-mess 1.15 :graft-pattern 0.9 :advance-capability 0.75}}
   {:lens/id :pattern-graft
    :description "Prefer transfer-pattern grafts and diagnostic bridges."
    :class-weights {:graft-pattern 1.75 :centre-mess 1.05 :close-hole 0.85 :advance-capability 0.7}}
   {:lens/id :fast-terminal
    :description "Prefer terminal or immediately bounded moves."
    :terminal-weight 1.8
    :class-weights {:centre-mess 1.35 :close-hole 1.0 :graft-pattern 0.9 :advance-capability 0.85}}])

(defn move-token-set
  [move]
  (set (remove nil? [(:move/id move)
                     (:move/class move)
                     (:have move)
                     (:want move)
                     (:advances-cap move)
                     (:confidence move)])))

(defn jaccard
  [a b]
  (let [a (set a)
        b (set b)
        union (set/union a b)]
    (if (empty? union)
      1.0
      (/ (double (count (set/intersection a b)))
         (double (count union))))))

(defn- lens-weight
  [lens move]
  (let [class-weight (double (get (:class-weights lens) (:move/class move) 1.0))
        terminal-weight (if (:move/terminal? move)
                          (double (or (:terminal-weight lens) 1.0))
                          1.0)]
    (* class-weight terminal-weight)))

(defn lens-move
  [lens move]
  (let [w (lens-weight lens move)]
    (-> move
        (update :score #(double (* w (double (or % 0.0)))))
        (update :step-score-delta #(double (* w (double (or % 0.0)))))
        (assoc :lens/id (:lens/id lens)))))

(defn score-buildout-c
  "Internal wholeness proxy for buildout construction only. Not the referee
   yardstick. The outside yardstick is rollout/project-policy's realized :policy-rollout-score."
  [policy]
  (let [intensity (reduce + 0.0 (map #(double (or (:score %) 0.0)) policy))
        token-sets (mapv move-token-set policy)
        pairs (for [i (range (count token-sets))
                    j (range (inc i) (count token-sets))]
                (jaccard (token-sets i) (token-sets j)))
        harmony (if (seq pairs)
                  (/ (reduce + 0.0 (map #(* 4.0 % (- 1.0 %)) pairs))
                     (count pairs))
                  1.0)]
    (* intensity harmony)))

(defn buildout-pattern-set
  [policy]
  (set (mapcat move-token-set policy)))

(defn generate-buildout
  [state moves lens {:keys [depth top-k gamma]
                     :or {depth 2 top-k 5 gamma 0.9}}]
  (let [lens-moves (mapv #(lens-move lens %) moves)
        move-by-id (into {} (map (juxt :move/id identity) lens-moves))
        scored (rollout/score-policies state lens-moves
                                       :depth depth :top-k top-k :gamma gamma)
        selected (or (:selected (rollout/select-policy scored :abstain-epsilon -1.0e-12))
                     (first scored))
        policy (mapv #(merge (get move-by-id (:move/id %)) %) (:policy selected))]
    {:buildout/id (:lens/id lens)
     :lens lens
     :policy policy
     :implied-moves (mapv :move/id policy)
     :pattern-set (buildout-pattern-set policy)
     :C (score-buildout-c policy)
     :realized-score (:policy-rollout-score selected)
     :argument {:selected-by :lens-varied-coherence-greedy
                :lens (:lens/id lens)
                :note "Buildout generated on a copied state; no live writes."}}))

(defn generate-buildouts
  "Build N lens-varied possible worlds over the same real |psi> state."
  [state moves & {:keys [lenses n depth top-k gamma]
                  :or {lenses default-lenses n 3 depth 2 top-k 5 gamma 0.9}}]
  (->> lenses
       (take n)
       (mapv #(generate-buildout state moves % {:depth depth :top-k top-k :gamma gamma}))))

(defn diversity-report
  [buildouts & {:keys [max-overlap require-disagreeing-moves?]
                :or {max-overlap 0.85 require-disagreeing-moves? true}}]
  (let [pairs (for [i (range (count buildouts))
                    j (range (inc i) (count buildouts))]
                {:left (:buildout/id (buildouts i))
                 :right (:buildout/id (buildouts j))
                 :pattern-overlap (jaccard (:pattern-set (buildouts i))
                                           (:pattern-set (buildouts j)))
                 :same-implied-moves? (= (:implied-moves (buildouts i))
                                         (:implied-moves (buildouts j)))})
        max-seen (if (seq pairs) (apply max (map :pattern-overlap pairs)) 1.0)
        disagree? (or (not require-disagreeing-moves?)
                      (some (complement :same-implied-moves?) pairs))
        diverse? (and (>= (count buildouts) 2)
                      (<= max-seen max-overlap)
                      (boolean disagree?))]
    {:diverse? diverse?
     :max-overlap max-seen
     :max-overlap-bound max-overlap
     :disagreeing-implied-moves? (boolean disagree?)
     :pairs (vec pairs)}))

(defn realized-g-score
  [state buildout & {:keys [gamma] :or {gamma 0.9}}]
  (rollout/project-policy state (:policy buildout) :gamma gamma))

(defn referee-harness
  "Judge buildouts with realized S(pi), after diversity has passed."
  [state buildouts & {:keys [gamma diversity-opts]
                      :or {gamma 0.9}}]
  (let [diversity (apply diversity-report buildouts (mapcat identity diversity-opts))]
    (if-not (:diverse? diversity)
      {:verdict :monotone-generator
       :diversity diversity
       :argument-record [{:finding :non-diverse-generator
                          :reason "Diversity precondition failed before judging; null result would be a measurement artifact."}]}
      (let [scored (->> buildouts
                        (mapv (fn [b]
                                (let [g (realized-g-score state b :gamma gamma)]
                                  (assoc b :yardstick :realized-score :realized-score (:policy-rollout-score g) :rollout g))))
                        (sort-by :realized-score)
                        vec)
            winner (first scored)
            top-c (last (sort-by :C scored))
            dialectic-wins? (< (double (:realized-score winner))
                               (double (:realized-score top-c)))]
        {:verdict (if dialectic-wins? :dialectic-wins :single-best-holds)
         :diversity diversity
         :winner winner
         :top-c-buildout top-c
         :argument-record [{:finding :diversity-passed
                            :max-overlap (:max-overlap diversity)}
                           {:finding :winner-by-realized-rollout-score
                            :winner (:buildout/id winner)
                            :realized-score (:realized-score winner)}
                           {:finding :single-best-C-baseline
                            :baseline (:buildout/id top-c)
                            :C (:C top-c)
                            :realized-score (:realized-score top-c)}
                           {:finding :comparison
                            :verdict (if dialectic-wins? :dialectic-wins :single-best-holds)}]}))))

(defn evaluate-circumstance
  [circumstance & {:keys [lenses n depth top-k gamma diversity-opts]
                   :or {lenses default-lenses n 3 depth 2 top-k 5 gamma 0.9}}]
  (let [buildouts (generate-buildouts (:state circumstance) (:moves circumstance)
                                      :lenses lenses :n n :depth depth :top-k top-k :gamma gamma)]
    (assoc (referee-harness (:state circumstance) buildouts
                            :gamma gamma :diversity-opts diversity-opts)
           :circumstance/id (:circumstance/id circumstance)
           :buildouts buildouts)))

(defn- psi-reachable-state
  [moves re]
  {:arrows {}
   :cap-overlay (into {}
                      (keep (fn [m]
                              (when-let [cap (:advances-cap m)]
                                [(name cap)
                                 {:id (str "scope/capability/" (name cap))
                                  :props {:capability/frontier? false
                                          :capability/status :held}}]))
                            moves))
   :reachable (set (keep (fn [m] (when (re-find re (str (:have m) " " (:note m))) (:have m))) moves))})

(defn- all-move-haves-state
  [moves]
  (assoc (psi-reachable-state moves #"^$") :reachable (set (map :have moves))))

(defn- root-seeded-state
  [moves]
  (update (psi-reachable-state moves #"^$") :reachable
          (fnil into #{})
          (set (filter #(or (re-find #"-d/mission/" %)
                            (re-find #"^scope/capability/" %))
                       (set/difference (set (keep :have moves))
                                       (set (keep :want moves)))))))

(defn experiment-runner
  "Run the ungrounded v0 over real circumstances. This is the realized rollout score floor;
   grounded peradam certification remains an external seam."
  ([] (experiment-runner (rollout/moves)))
  ([moves]
   (let [circumstances [{:circumstance/id :lens-varied-diffsub-frontier
                         :psi "real diffsub emitted move frontier with all copied haves available"
                         :state (all-move-haves-state moves)
                         :moves moves}
                        {:circumstance/id :root-seeded-focused-frontier
                         :psi "real diffsub mission/capability roots"
                         :state (root-seeded-state moves)
                         :moves moves}]]
     {:yardstick :realized-score
      :peradam-grounding :escrowed
      :results (mapv #(evaluate-circumstance % :n 4 :depth 1 :top-k 19)
                     circumstances)})))

(defn read-freeze
  "Read the frozen WM judgement used by E-cascade-sampler-sampler S1."
  ([] (read-freeze default-freeze-path))
  ([path]
   (json/parse-string (slurp path) true)))

(defn- action-target
  [action]
  (or (:target action) (:name action) (str (:rank action))))

(defn- action-class
  [action]
  (case (:type action)
    "advance-mission" :advance-capability
    "open-mission" :close-hole
    "close-hole" :close-hole
    "graft-pattern" :graft-pattern
    :centre-mess))

(defn action->move
  "Render a frozen ranked action as a rollout move. The move is synthetic and
  deterministic; it gives the contest harness a realized rollout score yardstick without
  claiming these transitions model the live field."
  [action]
  (let [rank (long (or (:rank action) 0))
        target (action-target action)
        open-holes (double (or (:open-hole-count action) 1.0))
        weight (double (or (:weight action) 1.0))]
    {:move/id (str (:type action) ":" target ":" rank)
     :move/class (action-class action)
     :have "wm-freeze/root"
     :want (str "wm-freeze/action/" rank "/" target)
     :score weight
     :step-score-delta (- (+ 0.1 (* 0.2 open-holes) (* 0.01 (max 0 (- 122 rank)))))
     :rank rank
     :move/terminal? (<= open-holes 1.0)
     :source/action action}))

(defn assemble-circumstances
  "Assemble deterministic arguing-worlds-shaped circumstances from the frozen
  WM ranked actions. Each circumstance carries an action FRONTIER strictly
  larger than the cascade budget (review fix, fable-2 2026-06-12: with
  frontier == budget every budget-bounded selection is the whole frontier and
  the contest degenerates). Deterministic stride interleave: circumstance i
  takes ranked actions where (mod idx n) == i, so every frontier spans the
  full rank spectrum instead of being one rank-band."
  ([freeze] (assemble-circumstances freeze {:n 10 :budget 6}))
  ([freeze {:keys [n budget] :or {n 10 budget 6}}]
   (let [actions (->> (:ranked-actions freeze)
                      (sort-by #(long (or (:rank %) Long/MAX_VALUE)))
                      vec)]
     (->> (range (count actions))
          (group-by #(mod % n))
          (sort-by key)
          (map (fn [[_ idxs]] (mapv actions idxs)))
          (take n)
          (map-indexed
           (fn [idx chunk]
             (let [moves (mapv action->move chunk)
                   ranks (map :rank chunk)]
               {:circumstance/id (keyword (format "wm-freeze-2026-06-12-%02d" idx))
                :psi (format "WM judgement freeze 2026-06-12 ranks %s-%s"
                             (first ranks) (last ranks))
                :state {:arrows {}
                        :cap-overlay {}
                        :reachable #{"wm-freeze/root"}}
                :moves moves})))
          vec))))

(defn write-circumstances!
  "Write S1 circumstances to EDN. Deterministic for a fixed freeze file."
  ([] (write-circumstances! default-freeze-path default-circumstances-path))
  ([freeze-path out-path]
   (let [circumstances (assemble-circumstances (read-freeze freeze-path))
         f (io/file out-path)]
     (.mkdirs (.getParentFile f))
     (spit f (with-out-str (prn circumstances)))
     {:path out-path
      :circumstance-count (count circumstances)
      :move-count (reduce + (map (comp count :moves) circumstances))})))

(defn- buildout-move-count
  [buildout]
  (count (:policy buildout)))

(defn- score-sampler-buildout
  [state gamma sampler-id wall-clock-ms buildout]
  (let [g (realized-g-score state buildout :gamma gamma)]
    (assoc buildout
           :sampler/id sampler-id
           :yardstick :realized-score
           :realized-score (:policy-rollout-score g)
           :rollout g
           :move-count (buildout-move-count buildout)
           :wall-clock-ms (double (or wall-clock-ms 0.0)))))

(defn- valid-buildouts
  [sampler-result]
  (->> (:buildouts sampler-result)
       (filter #(seq (:policy %)))
       vec))

(defn referee-field-harness
  "N-contestant referee. Each sampler contributes its best buildout per
  circumstance; the field is judged only by realized S(pi). C never enters the
  verdict path. Equal G prefers fewer moves, then lower wall-clock, then reports
  a tie."
  [state sampler-results & {:keys [gamma diversity-opts]
                            :or {gamma 0.9}}]
  (let [partials (->> sampler-results
                      (keep (fn [{:keys [sampler/id] :as result}]
                              (when (empty? (valid-buildouts result))
                                {:sampler/id id
                                 :status :partial
                                 :reason :zero-valid-buildouts})))
                      vec)
        scored (->> sampler-results
                    (mapcat (fn [{:keys [sampler/id wall-clock-ms] :as result}]
                              (map #(score-sampler-buildout state gamma id wall-clock-ms %)
                                   (valid-buildouts result))))
                    vec)]
    (if (empty? scored)
      {:verdict :no-valid-buildouts
       :partials partials
       :argument-record [{:finding :no-valid-buildouts}]}
      (let [diversity (apply diversity-report scored (mapcat identity diversity-opts))]
        (if-not (:diverse? diversity)
          {:verdict :monotone-generator
           :partials partials
           :diversity diversity
           :argument-record [{:finding :non-diverse-generator
                              :reason "Diversity precondition failed across the contestant field."}]}
          (let [per-sampler (->> scored
                                 (group-by :sampler/id)
                                 (mapv (fn [[sid bs]]
                                         (first (sort-by (juxt :realized-score :move-count :wall-clock-ms)
                                                         (map #(assoc % :sampler/id sid) bs))))))
                min-g (apply min (map :realized-score per-sampler))
                g-tied (filterv #(= min-g (:realized-score %)) per-sampler)
                min-moves (apply min (map :move-count g-tied))
                move-tied (filterv #(= min-moves (:move-count %)) g-tied)
                min-wall (apply min (map :wall-clock-ms move-tied))
                final-tied (filterv #(= min-wall (:wall-clock-ms %)) move-tied)
                winner (first final-tied)]
            {:verdict (if (= 1 (count final-tied)) :sampler-wins :tie)
             :winner (when (= 1 (count final-tied)) winner)
             :ties (when (< 1 (count final-tied)) final-tied)
             :per-sampler-best (sort-by :realized-score per-sampler)
             :partials partials
             :diversity diversity
             :argument-record [{:finding :field-diversity-passed
                                :max-overlap (:max-overlap diversity)}
                               {:finding :winner-by-realized-rollout-score
                                :winner (:sampler/id winner)
                                :realized-score (:realized-score winner)}]}))))))

(defn- sampler-buildout
  [id policy]
  {:buildout/id id
   :policy (vec policy)
   :pattern-set (buildout-pattern-set policy)
   :implied-moves (mapv :move/id policy)
   :C (score-buildout-c policy)})

(defn incumbent-sampler
  "Deterministic budget-6 incumbent. This uses the in-JVM rollout/cascade lane
  already present in this ns so tests stay offline; the sampler protocol allows
  a later cascade_lane/cascade-policy-for implementation to emit the same entry
  shape without changing the referee."
  [circumstance]
  (let [b (generate-buildout (:state circumstance) (:moves circumstance)
                             (first default-lenses)
                             {:depth 1 :top-k 6})]
    [(assoc b :buildout/id :incumbent/budget-6)]))

(defn greedy-eps-sampler
  ([circumstance] (greedy-eps-sampler circumstance {:eps 0.15 :budget 6}))
  ([circumstance {:keys [eps budget] :or {eps 0.15 budget 6}}]
   (let [moves (vec (sort-by (comp - double #(or (:score %) 0.0)) (:moves circumstance)))
         greedy (vec (take budget moves))
         explore-idx (min (dec (count moves)) (max 0 (long (Math/ceil (/ 1.0 eps)))))
         explore (when (and (pos? eps) (seq moves)) (moves explore-idx))
         policy (if (and explore (seq greedy) (not (some #(= (:move/id %) (:move/id explore)) greedy)))
                  (conj (vec (butlast greedy)) explore)
                  greedy)]
     [(sampler-buildout :greedy-eps/eps-0-15 policy)])))

(defn- seeded-shuffle
  [seed xs]
  (let [al (java.util.ArrayList. xs)
        rng (java.util.Random. (long seed))]
    (java.util.Collections/shuffle al rng)
    (vec al)))

(defn random-under-budget-sampler
  ([circumstance] (random-under-budget-sampler circumstance {:budget 6}))
  ([circumstance {:keys [budget] :or {budget 6}}]
   (let [seed (hash (:circumstance/id circumstance))
         policy (take budget (seeded-shuffle seed (:moves circumstance)))]
     [(sampler-buildout :random-under-budget policy)])))

(defn uniform-best-of-k-sampler
  "The fairness null for multi-entry samplers (review note, fable-2
  2026-06-12): a sampler submitting K entries gains order-statistics
  advantage under per-sampler-best aggregation regardless of learning.
  This arm submits K seeded-uniform budget-selections; whatever margin a
  trained sampler shows BEYOND this arm is the part attributable to
  learning rather than to sampling more."
  ([circumstance] (uniform-best-of-k-sampler circumstance {:k 8 :budget 6}))
  ([circumstance {:keys [k budget] :or {k 8 budget 6}}]
   (let [moves (vec (:moves circumstance))]
     (vec
      (for [i (range k)
            :let [seed (hash [(:circumstance/id circumstance) i])
                  policy (vec (take budget (seeded-shuffle seed moves)))]
            :when (seq policy)]
        (sampler-buildout (keyword "uniform-best-of-k" (str "s" i)) policy))))))

(defn run-sampler-field
  [circumstance samplers]
  (mapv (fn [[sampler-id sampler-fn]]
          {:sampler/id sampler-id
           :buildouts (vec (sampler-fn circumstance))
           :wall-clock-ms 0.0})
        samplers))
