(ns futon2.aif.arguing-worlds
  "Ungrounded v0 for M-arguing-worlds.

  The referee yardstick is realized rollout G(pi), not cascade C. C is only used
  to define the single-buildout baseline that the dialectic must beat. Diversity
  is checked before judging; a non-diverse generator returns :monotone-generator."
  (:require [clojure.set :as set]
            [futon2.aif.rollout :as rollout]))

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
        (update :delta-g #(double (* w (double (or % 0.0)))))
        (assoc :lens/id (:lens/id lens)))))

(defn score-buildout-c
  "Internal wholeness proxy for buildout construction only. Not the referee
   yardstick. The outside yardstick is rollout/project-policy's realized :G."
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
     :realized-G (:G selected)
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
  "Judge buildouts with realized G(pi), after diversity has passed."
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
                                  (assoc b :yardstick :realized-G :realized-G (:G g) :rollout g))))
                        (sort-by :realized-G)
                        vec)
            winner (first scored)
            top-c (last (sort-by :C scored))
            dialectic-wins? (< (double (:realized-G winner))
                               (double (:realized-G top-c)))]
        {:verdict (if dialectic-wins? :dialectic-wins :single-best-holds)
         :diversity diversity
         :winner winner
         :top-c-buildout top-c
         :argument-record [{:finding :diversity-passed
                            :max-overlap (:max-overlap diversity)}
                           {:finding :winner-by-realized-G
                            :winner (:buildout/id winner)
                            :realized-G (:realized-G winner)}
                           {:finding :single-best-C-baseline
                            :baseline (:buildout/id top-c)
                            :C (:C top-c)
                            :realized-G (:realized-G top-c)}
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
  "Run the ungrounded v0 over real circumstances. This is the realized-G floor;
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
     {:yardstick :realized-G
      :peradam-grounding :escrowed
      :results (mapv #(evaluate-circumstance % :n 4 :depth 1 :top-k 19)
                     circumstances)})))
