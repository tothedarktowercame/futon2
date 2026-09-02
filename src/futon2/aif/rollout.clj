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

;; ---------------------------------------------------------------------------
;; AC5 (Joe's 2026-09-02 ruling on C130 §5): the validated rollout step
;; producer. Every proposed move is classified before its numbers are used, so
;; a move that carries no score can no longer be exponentiated as though it had
;; scored zero.
;; ---------------------------------------------------------------------------

(def move-score-contract
  "Producer contract stamped on every record `move-score-record` emits, whether
   it read a score, found none, or refused a malformed one."
  :rollout-move-score/v1)

(def move-score-field
  "The move field the prior renormalizer reads. Named as a var because the
   census row, the lint and the tests all have to agree on which field this row
   is about."
  :score)

(def move-prior-field
  "The other numeric field the same expression reads. A non-number here used to
   throw out of `have-prior?` (`(double \"x\")`); it is now refused with the
   offending value named."
  :prior)

(defn- finite-double
  "X as a double when it is a finite number, else nil. Distinguishes a measured
   0.0 (a real score) from a value that cannot enter arithmetic at all."
  [x]
  (when (number? x)
    (let [d (double x)]
      (when-not (or (Double/isNaN d) (Double/isInfinite d)) d))))

(defn move-score-record
  "Classify one proposed MOVE as a typed record. AC5 (Joe's 2026-09-02 ruling
   on C130 §5) removed the `0.0` `:score` used to be defaulted to.

   `:scored` — the move supplies a finite `:score`:
     {:status :scored :value <double> :move/id <id>
      :producer-contract :rollout-move-score/v1}
   A supplied `0.0` lands here. A move that scored zero is scored.

   `:unscored` — the move supplies NO `:score` (key absent, or present as nil).
   No `:value` key is written; `:absent` names the field and records whether
   the key was there at all, so a nil-valued key and a missing key stay apart.

   `:unscored` is the honest reading of an absent score, not a verdict on what
   ranking should do with it: excluding it is this row's ranking rule (below),
   and what the ROLLOUT COST does with an unscored move is AC6's separate site
   at `move-cost`.

   `:refused` — a partial map with a numeric fallback: `:score` or `:prior` is
   present but is not a finite number (a string, a keyword, NaN, an infinity),
   or the move is not a map at all. `:offending` names the field and the value
   it was given. Absence and malformation are separated here exactly as they
   are in AC1's `free-energy/compute-prediction-error`: a producer that emitted
   no score has a gap; a producer that emitted \"0.4\" has a defect, and that
   has to stay loud."
  [move]
  (if-not (map? move)
    {:producer-contract move-score-contract
     :status :refused
     :reason :malformed-move-record
     :offending {:field nil :status :not-a-map :value move}}
    (let [stamp {:producer-contract move-score-contract
                 :move/id (:move/id move)}
          prior (get move move-prior-field)
          score (get move move-score-field)]
      (cond
        (and (some? prior) (nil? (finite-double prior)))
        (merge stamp {:status :refused
                      :reason :malformed-move-score
                      :offending {:field move-prior-field
                                  :status :not-finite
                                  :value prior}})

        (nil? score)
        (merge stamp {:status :unscored
                      :reason :score-not-supplied
                      :absent {:field move-score-field
                               :key-present? (contains? move move-score-field)}})

        :else
        (if-let [v (finite-double score)]
          (merge stamp {:status :scored :value v})
          (merge stamp {:status :refused
                        :reason :malformed-move-score
                        :offending {:field move-score-field
                                    :status :not-finite
                                    :value score}}))))))

(defn move-score-events
  "The present-only projection over a collection of RECORDS: the ones that are
   not `:scored`. Empty when every proposed move carried a finite score, which
   is the AC1–AC4 discipline — no key means the population validated, a
   different claim from \"the producer did not report\"."
  [records]
  (into [] (remove #(= :scored (:status %))) records))

(defn validated-priors
  "R1 prior renormalization over a VALIDATED move population.

   Returns {:population <moves with :prior>, :records <one per input move>,
            :score-required? <bool>, :move-score-events <present-only>}.

   The score is REQUIRED exactly when the sharpened-`:prior` path is
   unavailable — that is, when some move does not carry a positive finite
   `:prior` and the weights therefore fall back to softmax(`:score`). On the
   prior path no `:score` is read at all, so an unscored move is not a defect
   there and still ranks; its record still reports the absence, with
   `:score-required?` false.

   When the score IS required, only `:scored` moves enter the renormalized
   population. This is the ranking-population change C130 §5 predicted
   (\"validation changes which moves can enter ranking\"): the alternative is
   the fabricated `exp(0.0) = 1.0` weight the old expression gave a move nobody
   scored, which is indistinguishable from a move that scored exactly zero."
  [moves]
  (let [pairs (mapv (fn [m] [m (move-score-record m)]) moves)
        have-prior? (and (seq pairs)
                         (every? (fn [[m r]]
                                   (and (not= :refused (:status r))
                                        (when-let [p (finite-double (get m move-prior-field))]
                                          (pos? p))))
                                 pairs))
        required? (not have-prior?)
        kept (if required?
               (filterv (fn [[_ r]] (= :scored (:status r))) pairs)
               pairs)
        weight (fn [[m r]]
                 (if required?
                   (Math/exp (double (:value r)))
                   (double (get m move-prior-field))))
        weights (mapv weight kept)
        total (reduce + 0.0 weights)
        records (mapv (fn [[_ r]]
                        (cond-> (assoc r :score-required? required?)
                          (and required? (not= :scored (:status r)))
                          (assoc :excluded-from-ranking? true)))
                      pairs)]
    {:population (mapv (fn [[m _] w]
                         (assoc m :prior (if (pos? total) (/ w total) 0.0)))
                       kept
                       weights)
     :records records
     :score-required? required?
     :move-score-events (move-score-events records)}))


;; ---------------------------------------------------------------------------
;; AC6 (Joe's 2026-09-02 ruling on C130 §6): what the rollout COST does with a
;; move it cannot cost, and the floor under "exclude and continue".
;;
;; AC5 stopped `renormalize-priors` reading `(or (:score m) 0.0)`. `move-cost`
;; read the same field through its own default one step later, so a move that
;; reached the cost leg unscored still contributed exactly `0.0` to
;; `project-policy`'s accumulator — the same number a structurally-zero step
;; (a truncated state, an already-satisfied capability) contributes, and the
;; same number a move that genuinely scored zero contributes.
;; ---------------------------------------------------------------------------

(def move-cost-contract
  "Producer contract stamped on every record `move-cost-record` emits, whether
   it costed the move, found nothing to cost it with, or refused a malformed
   field."
  :rollout-move-cost/v1)

(def rollout-refusal-contract
  "Producer contract stamped on the record a REFUSED rollout emits. Separate
   from `move-cost-contract` because it is a claim about the search, not about
   one move."
  :rollout-refusal/v1)

(def move-cost-delta-field
  "The move field `move-cost` reads FIRST. Named as a var for the same reason
   as `move-score-field`: the census row, the lint and the tests all have to
   agree on which fields this row is about. Measured at AC5: absent on 19/19 of
   `diffsub-moves-stub.edn` and 55/55 of `diffsub-moves.edn` — both real sets
   carry `:delta-g` instead — so the second fallback is the live branch today."
  :step-score-delta)

(defn move-cost-record
  "Classify the local cost g(s_t) of one MOVE in STATE as a typed record. AC6
   removed the `0.0` the `:score` used to be defaulted to here.

   `:present` — a cost was determined, carried as `:value`, with `:basis`
   naming WHERE it came from. Four bases, which the old bare `0.0` could not
   tell apart:
     `:truncated-state`       — the state is truncated; the step earns no
                                pragmatic credit whatever the move says.
     `:satisfied-capability`  — the cap this move advances is already satisfied,
                                so a rollout cannot farm it.
     `:step-score-delta`      — a finite `:step-score-delta` (negative = benefit).
     `:negated-score`         — no delta, but a finite `:score`; cost is `-score`.
   A supplied `0.0` in either field lands here: a move that scored zero is
   costed at zero.

   `:uncosted` — NEITHER field is supplied (each key absent, or present as
   nil). No `:value` key; `:absent` names both fields and records whether each
   key was there at all, so a nil-valued key and a missing key stay apart.

   `:refused` — a partial map with a numeric fallback: `:step-score-delta` or
   `:score` present but not a finite number, or the move is not a map at all.
   `:offending` names the field and the value it was given. The `map?` check
   runs BEFORE the structural short-circuits, so a malformed move stays loud
   even in a truncated state, where the old expression never read it.

   The delta is checked before the score because the old expression preferred
   it; a malformed delta is therefore refused rather than silently falling
   through to `:score`, which is the AC1 separation (absence omits,
   malformation refuses) applied to a two-field fallback chain."
  [state move]
  (let [stamp {:producer-contract move-cost-contract
               :move/id (:move/id move)}]
    (if-not (map? move)
      (merge stamp {:status :refused
                    :reason :malformed-move-record
                    :offending {:field nil :status :not-a-map :value move}})
      (let [cap-id (:advances-cap move)
            cap (when cap-id (get-in state [:cap-overlay cap-id]))
            status (get-in cap [:props :capability/status])
            delta (get move move-cost-delta-field)
            score (get move move-score-field)]
        (cond
          (:truncated? state)
          (merge stamp {:status :present :value 0.0 :basis :truncated-state})

          (= :satisfied status)
          (merge stamp {:status :present :value 0.0 :basis :satisfied-capability})

          (and (some? delta) (nil? (finite-double delta)))
          (merge stamp {:status :refused
                        :reason :malformed-move-cost
                        :offending {:field move-cost-delta-field
                                    :status :not-finite :value delta}})

          (some? delta)
          (merge stamp {:status :present :value (finite-double delta)
                        :basis move-cost-delta-field})

          (and (some? score) (nil? (finite-double score)))
          (merge stamp {:status :refused
                        :reason :malformed-move-cost
                        :offending {:field move-score-field
                                    :status :not-finite :value score}})

          (some? score)
          (merge stamp {:status :present :value (- (finite-double score))
                        :basis :negated-score})

          :else
          (merge stamp {:status :uncosted
                        :reason :cost-not-supplied
                        :absent [{:field move-cost-delta-field
                                  :key-present? (contains? move move-cost-delta-field)}
                                 {:field move-score-field
                                  :key-present? (contains? move move-score-field)}]}))))))

(defn move-cost-events
  "The present-only projection over a collection of cost RECORDS: the ones that
   are not `:present`. Empty when every candidate could be costed, which is the
   AC1–AC5 discipline — no key means the population validated, a different
   claim from \"the producer did not report\"."
  [records]
  (into [] (remove #(= :present (:status %))) records))

(defn- renormalize-prior-mass
  "Rescale `:prior` over a population that LOST members to cost exclusion, so
   this node's branching weights still sum to 1. A pure rescale, so the
   survivor ORDER is unchanged; it only repairs the numbers each survivor
   carries. Applied only when something was actually excluded, so an
   unexcluded population is left bit-for-bit alone."
  [ms]
  (let [total (reduce + 0.0 (map #(double (or (:prior %) 0.0)) ms))]
    (if (pos? total)
      (mapv #(assoc % :prior (/ (double (:prior %)) total)) ms)
      ms)))

(defn renormalize-priors
  "R1: per-node PUCT branching weights, renormalized over THIS node's reachable
   survivors. Consumes the producer's sharpened :prior field (the policy-head
   output) when every survivor carries a positive one, falling back to
   softmax(:score) otherwise. (Originally this always recomputed softmax(:score),
   which silently discarded a sharpened :prior whenever :score was flat — as it
   is at scope-grain — re-flattening the policy head to uniform.)

   AC5: the softmax fallback no longer reads `(or (:score move) 0.0)`. The
   population is validated first (`validated-priors`), and a move with no
   finite score is not weighted. Use `validated-priors` directly when the typed
   records are wanted; this arity keeps the older vector-in/vector-out shape."
  [moves]
  (:population (validated-priors moves)))

(defn ranked-survivors-with-records
  "`ranked-survivors` plus the typed records the two validations produced.
   Returns {:survivors [...] :move-score-events [...] :move-cost-events [...]},
   both events vectors present-only, so both are empty whenever every reachable
   move validated.

   AC6 EXCLUDE-AND-CONTINUE: a move that survived AC5's score validation but
   that `move-cost-record` cannot cost does not become a candidate. Its record
   is marked `:excluded-from-rollout?` and the surviving priors are rescaled so
   this node's branching weights still sum to 1.

   AC6 REFUSE FLOOR, first half: when the reachable set was NOT empty and every
   member of it was excluded by validation (AC5's or AC6's), the extra key
   `:candidate-set-emptied? true` is set with `:reachable-count`. That is the
   distinction AC5 left open — a node exhausted by exclusion otherwise looks
   exactly like a node with nothing reachable, and the search would stop on
   both with the prefix it had."
  [state moves & {:keys [top-k]
                  :or {top-k 5}}]
  (let [reachable (reachable-moves state moves)
        {:keys [population move-score-events]} (validated-priors reachable)
        costed (mapv (fn [m] [m (move-cost-record state m)]) population)
        kept (filterv (fn [[_ r]] (= :present (:status r))) costed)
        cost-events (mapv (fn [[_ r]] (assoc r :excluded-from-rollout? true))
                          (remove (fn [[_ r]] (= :present (:status r))) costed))
        survivors (cond-> (mapv first kept)
                    (not= (count kept) (count population)) renormalize-prior-mass)]
    (cond-> {:survivors (->> survivors
                             (sort-by (juxt (comp - double :prior) :rank :move/id))
                             (take top-k)
                             vec)
             :move-score-events move-score-events
             :move-cost-events cost-events}
      (and (seq reachable) (empty? kept))
      (assoc :candidate-set-emptied? true
             :reachable-count (count reachable)))))

(defn ranked-survivors
  [state moves & {:keys [top-k]
                  :or {top-k 5}}]
  (:survivors (ranked-survivors-with-records state moves :top-k top-k)))

(defn move-cost
  "Local g(s_t) proxy from the locked stub, as a double — or **nil** when the
   move cannot be costed.

   Negative :step-score-delta is a benefit. Already-satisfied capability steps get zero
   pragmatic credit so a rollout cannot farm an already closed cap.

   AC6: this no longer reads `(or (:score move) 0.0)`. It is now the numeric
   projection of `move-cost-record`, so an `:uncosted` or `:refused` move
   yields nil rather than a zero indistinguishable from the two structural
   zeros above. Callers inside this namespace never see the nil — the search
   excludes uncostable moves at `ranked-survivors-with-records` and
   `project-policy` refuses a policy that contains one — and any caller that
   wants the reason rather than the number should read the record."
  [state move]
  (:value (move-cost-record state move)))

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
  "Port of ukrn's path accumulator shape: S(pi)=sum gamma^t g(s_t).
   :truncated is sticky: terminal moves carry their local cost and stop expansion.

   AC6: a policy is an ORDERED plan, so a step that cannot be costed cannot be
   dropped and the rest kept — exclude-and-continue is a candidate-set rule, not
   a within-policy one. When a move in POLICY is `:uncosted` or `:refused` the
   whole projection refuses: it returns `{:rollout/refusal … :move-cost-events
   […]}` with **no** `:policy-rollout-score` key, so a caller reading the score
   gets nothing rather than a total short one term. The search never reaches
   this branch — `ranked-survivors-with-records` already excluded such moves,
   and the only state changes the walk makes (`:truncated?`, a cap becoming
   `:satisfied`) can only turn a record `:present` — but `project-policy` is
   also called directly on a caller-supplied policy."
  [state policy & {:as opts}]
  (let [gamma (rollout-discount opts)]
    (loop [state (normalize-state state)
           remaining (seq policy)
           discount 1.0
           total 0.0
           steps []]
      (if (or (nil? remaining) (:truncated? state))
        {:policy-rollout-score total
         :final-state state
         :steps (vec steps)
         :policy (mapv #(select-keys % [:move/id :move/class :have :want
                                        :advances-cap :prior :step-score-delta
                                        :move/terminal?])
                       policy)
         :truncated? (boolean (:truncated? state))}
        (let [move (first remaining)
              record (move-cost-record state move)]
          (if (not= :present (:status record))
            {:rollout/refusal {:producer-contract rollout-refusal-contract
                               :status :refused
                               :reason :uncostable-move-in-policy
                               :move/id (:move/id record)
                               :step (count steps)
                               :cost-record record}
             :move-cost-events [record]
             :steps (vec steps)}
            (let [g (:value record)
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
                                  :truncated? (:truncated? state')})))))))))

(defn expand-policies-with-records
  "`expand-policies` plus the AC5/AC6 records the per-node validation produced,
   collected across the whole expansion and deduplicated. Returns
   {:nodes [...] :move-score-events [...] :move-cost-events [...]
    :emptied-nodes [...]}. The events are a property of the SEARCH, not of any
   one node: the same unscored move can be reached from several prefixes, and
   it is the producer that has to be repaired either way.

   `:emptied-nodes` names each node whose reachable set was non-empty and was
   emptied by validation — the input to AC6's refuse floor. It carries the
   node's `:prefix` (move ids) and `:reachable-count`, not the node's state, so
   the record stays small enough to persist."
  [state moves {:keys [top-k] :or {top-k 5} :as opts}]
  (let [horizon (rollout-horizon opts)
        !events (volatile! [])
        !cost-events (volatile! [])
        !emptied (volatile! [])]
    (letfn [(expand [state prefix remaining-depth]
              (if (zero? remaining-depth)
                [{:state state :policy prefix}]
                (let [{:keys [survivors move-score-events move-cost-events
                              candidate-set-emptied? reachable-count]}
                      (ranked-survivors-with-records state moves :top-k top-k)]
                  (when (seq move-score-events)
                    (vswap! !events into move-score-events))
                  (when (seq move-cost-events)
                    (vswap! !cost-events into move-cost-events))
                  (when candidate-set-emptied?
                    (vswap! !emptied conj {:prefix (mapv :move/id prefix)
                                           :reachable-count reachable-count}))
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
      {:nodes (vec (expand (normalize-state state) [] horizon))
       :move-score-events (vec (distinct @!events))
       :move-cost-events (vec (distinct @!cost-events))
       :emptied-nodes (vec (distinct @!emptied))})))

(defn expand-policies
  [state moves & {:as opts :keys [top-k]
                  :or {top-k 5}}]
  (:nodes (expand-policies-with-records state moves (assoc opts :top-k top-k))))

(defn rollout-authority
  "Read the caller's declared authority for this rollout. `:diagnose` (the
   default) claims the result's `:policy` will NOT be enacted — only its score
   read; `:authorize` says the rollout is choosing an action. Anything else is
   loud.

   `:authority-declared?` is reported alongside because a default is not a
   claim: AC8's harvester can then see a rollout running on an undeclared
   authority, rather than reading the default as an audited `:diagnose`."
  [opts]
  (let [declared (:authority opts)]
    (when-not (contains? #{nil :diagnose :authorize} declared)
      (throw (ex-info "unknown rollout authority"
                      {:authority declared :allowed #{:diagnose :authorize}})))
    {:authority (or declared :diagnose)
     :authority-declared? (some? declared)}))

(defn rollout-refusal
  "AC6's REFUSE FLOOR under exclude-and-continue (Joe's 2026-09-02 ruling on
   C130 §6). Returns a typed refusal record, or nil when the rollout may stand.

   Two conditions, exactly the two the ruling names:

   `:candidate-set-emptied-by-exclusion` — some node's reachable set was not
   empty and validation excluded all of it. Continuing there would return the
   prefix the search happened to have, which is the same shape a genuinely
   terminal node returns; the score would then be an argmin over a branch a
   producer defect silently truncated.

   `:exclusion-under-authorizing-rollout` — the caller declared `:authority
   :authorize` and ANY move was excluded. A reduced candidate set changes which
   action is chosen, so a rollout that is choosing may not run on one. An
   exclusion-free `:authorize` rollout stands, and so does a `:diagnose` one
   that merely reports an absence without excluding anything (an unscored move
   on the sharpened-`:prior` path, where no `:score` is read)."
  [{:keys [move-score-events move-cost-events emptied-nodes]}
   {:keys [authority authority-declared?]}]
  (let [excluded (into (filterv :excluded-from-ranking? move-score-events)
                       move-cost-events)
        base {:producer-contract rollout-refusal-contract
              :status :refused
              :authority authority
              :authority-declared? authority-declared?
              :excluded (mapv :move/id excluded)}]
    (cond
      (seq emptied-nodes)
      (assoc base :reason :candidate-set-emptied-by-exclusion
             :emptied-nodes (vec emptied-nodes))

      (and (= :authorize authority) (seq excluded))
      (assoc base :reason :exclusion-under-authorizing-rollout)

      :else nil)))

(defn score-policies
  "AC5 self-repair condition: when the expansion validated a move as `:unscored`
   or `:refused`, every returned policy carries the search's typed records under
   `:move-score-events`. AC6 adds `:move-cost-events` on the same terms.
   PRESENT-ONLY — with a fully scored, fully costable population neither key is
   written and the returned maps are byte-identical to the pre-AC5 ones.

   AC6 refuse floor: when `rollout-refusal` fires, this returns a SINGLE map
   carrying `:rollout/refusal` and the events, with **no**
   `:policy-rollout-score` and no `:policy`. A refused rollout produces no
   number: `cascade_lane/policy-rollout` then reads nil and the act gate
   abstains, which is the shape it already uses for \"ΔG genuinely unavailable,
   not zero\"."
  [state moves & {:as opts :keys [top-k]
                  :or {top-k 5}}]
  (let [horizon (rollout-horizon opts)
        gamma (rollout-discount opts)
        auth (rollout-authority opts)
        {:keys [nodes move-score-events move-cost-events] :as expansion}
        (expand-policies-with-records state moves {:horizon horizon :top-k top-k})
        projected (mapv (fn [{:keys [policy]}] (project-policy state policy :gamma gamma))
                        nodes)
        events (cond-> {}
                 (seq move-score-events) (assoc :move-score-events move-score-events)
                 (seq move-cost-events) (assoc :move-cost-events move-cost-events))
        refusal (or (rollout-refusal expansion auth)
                    (first (keep :rollout/refusal projected)))]
    (if refusal
      [(merge {:rollout/refusal refusal} events)]
      (->> projected
           (mapv #(merge % events))
           (sort-by :policy-rollout-score)
           vec))))

(defn greedy-one-step
  [state moves & {:as opts :keys [top-k]
                  :or {top-k 20}}]
  (first (score-policies state moves
                         :depth 1
                         :top-k top-k
                         :temporal-discount (rollout-discount opts)
                         ;; AC6: forward the caller's declared authority. nil
                         ;; here means undeclared, not `:diagnose` — see
                         ;; `rollout-authority`.
                         :authority (:authority opts))))

(defn best-rollout
  [state moves & {:as opts :keys [top-k]
                  :or {top-k 5}}]
  (let [horizon (rollout-horizon opts)
        gamma (rollout-discount opts)]
    (first (score-policies state moves :horizon horizon :top-k top-k :gamma gamma
                           :authority (:authority opts)))))

(defn softmax
  [scores tau]
  (let [tau (double (or tau 1.0))
        weights (mapv #(Math/exp (/ (- (double (:policy-rollout-score %))) tau)) scores)
        total (reduce + 0.0 weights)]
    (mapv (fn [score weight]
            (assoc score :selection/probability (if (pos? total) (/ weight total) 0.0)))
          scores
          weights)))

(defn select-policy
  "Argmin policy rollout score with WM-I4 abstain on flat fields.

   AC6: a refused rollout carries no `:policy-rollout-score`, so there is
   nothing to rank. Selection returns `{:decision :refuse …}` with the refusal
   record rather than sorting over a missing number — the AC4 shape (abstain
   and return control) applied at this site."
  [scored & {:keys [tau abstain-epsilon]
             :or {tau 1.0 abstain-epsilon 1.0e-6}}]
  (if-let [refusal (first (keep :rollout/refusal scored))]
    {:decision :refuse
     :reason (:reason refusal)
     :rollout/refusal refusal}
    (let [ranked (vec (sort-by :policy-rollout-score scored))
          [best second-best] ranked
          ranked' (softmax ranked tau)]
      (if (and best second-best
               (< (Math/abs (- (double (:policy-rollout-score second-best))
                               (double (:policy-rollout-score best))))
                  abstain-epsilon))
        {:decision :abstain
         :ranked ranked'}
        {:decision :select
         :selected (first ranked')
         :ranked ranked'}))))
