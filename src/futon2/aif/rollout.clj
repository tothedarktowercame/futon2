(ns futon2.aif.rollout
  "Policy rollout search over meme-arrow transition leaves."
  (:require [clojure.edn :as edn]
            [clojure.set :as set]
            [meme.step :as meme-step]))

(def default-move-set-path
  "/home/joe/code/futon6/data/diffsub-moves-stub.edn")

(defn load-move-set
  ([] (load-move-set default-move-set-path))
  ([path]
   (edn/read-string (slurp path))))

(defn moves
  ([] (moves (load-move-set)))
  ([move-set]
   (vec (:moves move-set))))

(defn constructed-reachable
  [state]
  (->> (:arrows state)
       vals
       (filter #(= :constructed (:status %)))
       (map :want)
       set))

(defn normalize-state
  [state]
  (update state :reachable
          (fn [reachable]
            (set (concat reachable (constructed-reachable state))))))

(defn root-haves
  "The unconstructed :haves — endpoints no move ever produces as a :want. Every
   non-root :have is some precursor move's :want, so this set-difference recovers
   exactly the root endpoints WITHOUT hardcoding them (drift-free as the move set
   changes). They partition into three classes (claude-3's root taxonomy) — two
   seeded axiom classes, one intended-dark island class — plus any drift. Strings
   are matched verbatim; never normalize case (the §12.11 trap: mission stems are
   lowercased but scope-id prefixes keep original case)."
  [moves]
  (set/difference (set (keep :have moves))
                  (set (keep :want moves))))

(defn- roots-matching [moves re]
  (set (filter #(re-find re %) (root-haves moves))))

(defn mission-roots
  "SEED class 1: mission entities `<repo>-d/mission/<stem>` — the given axioms
   that ignite each close-hole phase-chain at t=0."
  [moves]
  (roots-matching moves #"-d/mission/"))

(defn capability-roots
  "SEED class 2: claimed capabilities `scope/capability/<id>` — an achieved goal
   is an axiom exactly like a mission. The :have of the reachable summit moves;
   without seeding these the cap summits never ignite."
  [moves]
  (roots-matching moves #"^scope/capability/"))

(defn conjectural-roots
  "INTENDED-DARK class: conjectural footholds `scope/conjectural/<cap>-foothold` —
   island :haves no move constructs BY DESIGN. They must stay unreachable until a
   foothold is built (the summit/island reachability axis), so they are NOT seeded.
   Their darkness is the 'needs a constructed foothold' signal, not drift."
  [moves]
  (roots-matching moves #"^scope/conjectural/"))

(defn drift-roots
  "Unconstructed :haves in NONE of the three known classes — genuine drift (a
   producer left a dangling :have that would silently seed the search). Surface
   loudly (the §12.11 guard); a non-empty result is a real problem."
  [moves]
  (set/difference (root-haves moves)
                  (mission-roots moves)
                  (capability-roots moves)
                  (conjectural-roots moves)))

(defn seed-roots
  "Fold the SEED axiom classes (mission entities + claimed capabilities) into
   :reachable at t=0 so the phase-chains and cap summits ignite. Conjectural
   footholds are deliberately EXCLUDED — islands stay dark until a foothold is
   constructed. Missions/claimed-caps are given, not constructed."
  [state moves]
  (update state :reachable (fnil into #{})
          (set/union (mission-roots moves) (capability-roots moves))))

(defn reachable? [state move]
  (contains? (:reachable state) (:have move)))

(defn spent?
  "A move whose arrow is already :constructed — the hole is closed, so the
   move is spent and must not be re-offered. Without this, a uniform-cost
   search farms the same construction (re-closing a closed hole) instead of
   advancing down the precursor chain."
  [state move]
  (= :constructed (get-in state [:arrows [(:have move) (:want move)] :status])))

(defn frontier-no-path? [state move]
  (let [cap-id (:advances-cap move)
        cap (when cap-id (get-in state [:cap-overlay cap-id]))]
    (and cap
         (meme-step/cap-frontier? cap)
         (not (reachable? state move)))))

(defn reachable-moves
  "Apply the moving reachable mask for a single node."
  [state moves]
  (let [state (normalize-state state)]
    (->> moves
         (remove #(frontier-no-path? state %))
         (remove #(spent? state %))
         (filter #(reachable? state %))
         vec)))

(defn renormalize-priors
  "R1: per-node PUCT branching weights, renormalized over THIS node's reachable
   survivors. Consumes the producer's sharpened :prior field (the policy-head
   output) when every survivor carries a positive one, falling back to
   softmax(:score) otherwise. (Originally this always recomputed softmax(:score),
   which silently discarded a sharpened :prior whenever :score was flat — as it
   is at scope-grain — re-flattening the policy head to uniform.)"
  [moves]
  (let [have-prior? (and (seq moves)
                         (every? #(let [p (:prior %)] (and p (pos? (double p)))) moves))
        weight (if have-prior?
                 #(double (:prior %))
                 #(Math/exp (double (or (:score %) 0.0))))
        weights (mapv weight moves)
        total (reduce + 0.0 weights)]
    (mapv (fn [move w]
            (assoc move :prior (if (pos? total) (/ w total) 0.0)))
          moves
          weights)))

(defn ranked-survivors
  [state moves & {:keys [top-k]
                  :or {top-k 5}}]
  (->> (reachable-moves state moves)
       renormalize-priors
       (sort-by (juxt (comp - double :prior) :rank :move/id))
       (take top-k)
       vec))

(defn move-cost
  "Local g(s_t) proxy from the locked stub.

   Negative :delta-g is a benefit. Already-satisfied capability steps get zero
   pragmatic credit so a rollout cannot farm an already closed cap."
  [state move]
  (let [cap-id (:advances-cap move)
        cap (when cap-id (get-in state [:cap-overlay cap-id]))
        status (get-in cap [:props :capability/status])]
    (cond
      (:truncated? state) 0.0
      (= :satisfied status) 0.0
      :else (double (or (:delta-g move) (- (double (or (:score move) 0.0))))))))

(defn apply-move
  [state move]
  (let [state (normalize-state state)
        state' (meme-step/step state (assoc move :to-state :constructed))]
    (update state' :write-count (fnil + 0) 0)))

(defn- rollout-horizon
  "R15 bridge: name the temporal search depth as a horizon H while preserving
   the older :depth option. This is still flat temporal rollout, not nested
   fast/slow hierarchy."
  [{:keys [horizon depth] :or {depth 2}}]
  (long (or horizon depth)))

(defn- rollout-discount
  "R15 bridge: name the temporal discount while preserving the older :gamma
   option used by the R13 rollout apparatus."
  [{:keys [temporal-discount gamma] :or {gamma 0.9}}]
  (double (or temporal-discount gamma)))

(defn project-policy
  "Port of ukrn's path accumulator shape: G(pi)=sum gamma^t g(s_t).
   :truncated is sticky: terminal moves carry their local cost and stop expansion."
  [state policy & {:as opts}]
  (let [gamma (rollout-discount opts)]
    (loop [state (normalize-state state)
           remaining (seq policy)
           discount 1.0
           total 0.0
           steps []]
      (if (or (nil? remaining) (:truncated? state))
        {:G total
         :final-state state
         :steps (vec steps)
         :policy (mapv #(select-keys % [:move/id :move/class :have :want
                                        :advances-cap :prior :delta-g
                                        :move/terminal?])
                       policy)
         :truncated? (boolean (:truncated? state))}
        (let [move (first remaining)
              g (move-cost state move)
              state' (apply-move state move)]
          (recur state'
                 (next remaining)
                 (* discount gamma)
                 (+ total (* discount g))
                 (conj steps {:move/id (:move/id move)
                              :g g
                              :discount discount
                              :discounted-g (* discount g)
                              :prior (:prior move)
                              :truncated? (:truncated? state')})))))))

(defn expand-policies
  [state moves & {:as opts :keys [top-k]
                  :or {top-k 5}}]
  (let [horizon (rollout-horizon opts)]
    (letfn [(expand [state prefix remaining-depth]
              (if (zero? remaining-depth)
                [{:state state :policy prefix}]
                (let [survivors (ranked-survivors state moves :top-k top-k)]
                  (if (empty? survivors)
                    [{:state state :policy prefix}]
                    (mapcat
                     (fn [move]
                       (let [state' (apply-move state move)
                             prefix' (conj prefix move)]
                         (if (or (:truncated? state') (:move/terminal? move))
                           [{:state state' :policy prefix'}]
                           (expand state' prefix' (dec remaining-depth)))))
                     survivors)))))]
      (vec (expand (normalize-state state) [] horizon)))))

(defn score-policies
  [state moves & {:as opts :keys [top-k]
                  :or {top-k 5}}]
  (let [horizon (rollout-horizon opts)
        gamma (rollout-discount opts)]
    (->> (expand-policies state moves :horizon horizon :top-k top-k)
       (mapv (fn [{:keys [policy]}]
               (project-policy state policy :gamma gamma)))
       (sort-by :G)
       vec)))

(defn greedy-one-step
  [state moves & {:as opts :keys [top-k]
                  :or {top-k 20}}]
  (first (score-policies state moves
                         :depth 1
                         :top-k top-k
                         :temporal-discount (rollout-discount opts))))

(defn best-rollout
  [state moves & {:as opts :keys [top-k]
                  :or {top-k 5}}]
  (let [horizon (rollout-horizon opts)
        gamma (rollout-discount opts)]
    (first (score-policies state moves :horizon horizon :top-k top-k :gamma gamma))))

(defn softmax
  [scores tau]
  (let [tau (double (or tau 1.0))
        weights (mapv #(Math/exp (/ (- (double (:G %))) tau)) scores)
        total (reduce + 0.0 weights)]
    (mapv (fn [score weight]
            (assoc score :selection/probability (if (pos? total) (/ weight total) 0.0)))
          scores
          weights)))

(defn select-policy
  "Argmin G with WM-I4 abstain on flat fields."
  [scored & {:keys [tau abstain-epsilon]
             :or {tau 1.0 abstain-epsilon 1.0e-6}}]
  (let [ranked (vec (sort-by :G scored))
        [best second-best] ranked
        ranked' (softmax ranked tau)]
    (if (and best second-best
             (< (Math/abs (- (double (:G second-best)) (double (:G best))))
                abstain-epsilon))
      {:decision :abstain
       :ranked ranked'}
      {:decision :select
       :selected (first ranked')
       :ranked ranked'})))
