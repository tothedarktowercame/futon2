(ns futon2.aif.fold
  "The FOLD interface — the R16 loop-closure contract (E-close-the-loop).

   R16 is the arc from R10 (act) back to R2 (observe). The engineering gate is
   `:pass` iff `cascade-score>0` and `coverage-score-delta<0`. A fold supplies
   the checkable construction and the latter score:

       fold : (cascade, circumstance) → {:wiring :coverage-score-delta :policy-holes}

   This ns is ONLY the contract those folds work to — so a classical, an
   LLM-turn, or an embedding fold all plug into the same socket and are
   comparable (E-close-the-loop §2).

   LOAD-BEARING (Joe, 2026-06-26): the contract is **data-agnostic**. It names
   ports and types only — NO store / corpus / weights / arrow source. If
   code-is-data, the data a fold draws on is part of its *construction*, not the
   interface. (A leak of any data source into this ns is the failure mode.)

   The ports:
     `:wiring`       — the construction (boxes/wires/terminals; what
                       `apply-cascade!` runs). Present on any successful fold.
     `:coverage-score-delta` — number | nil. Negative means the constructed
                       fold covers at least one surfaced obligation; nil means
                       no construction was available and the gate abstains.
                       It carries no EFE semantics.
     `:policy-holes` — sequential. What the fold left FREE or could not derive —
                       surfaced, never silently dropped (the fold's coverage
                       discipline, per E-llm-fold).

   `cascade` = the pattern halo condensed around the mission's (have→want) meme;
   `circumstance` = the mission/sorry context the construction must fit. HOW each
   is obtained (query / embedding / store) is solution-side.")

(defn valid-fold-output?
  "True iff M satisfies the fold contract: a map carrying `:wiring`, a `:coverage-score-delta`
   that is a number or nil, and a sequential `:policy-holes`."
  [m]
  (and (map? m)
       (contains? m :wiring)
       (let [g (:coverage-score-delta m)] (or (nil? g) (number? g)))
       (sequential? (:policy-holes m))))

(def warrant-kinds #{:pattern :worked-example :deduction})
(def condition-statuses #{:established :absent :unchecked})

(defn- obligation-id? [value]
  (or (keyword? value) (and (string? value) (seq value))))

(defn- nonempty-string? [value]
  (and (string? value) (seq value)))

(defn- hole-findings [box-index box-id hole]
  (let [at {:box/index box-index :box/id box-id}]
    (cond-> []
      (not (keyword? (:kind hole)))
      (conj (assoc at :finding :hole-kind-missing))
      (not (nonempty-string? (:wanted hole)))
      (conj (assoc at :finding :hole-wanted-missing))
      (and (contains? hole :discharge) (not (keyword? (:discharge hole))))
      (conj (assoc at :finding :hole-discharge-invalid
                      :observed (:discharge hole)))
      (and (contains? hole :satiety) (not (keyword? (:satiety hole))))
      (conj (assoc at :finding :hole-satiety-invalid
                      :observed (:satiety hole)))
      (not (obligation-id? (:obligation/id hole)))
      (conj (assoc at :finding :box-hole-obligation-id-missing)))))

(defn- policy-hole-findings [index hole]
  (let [at {:policy-hole/index index}]
    (cond-> []
      (not (nonempty-string? (:free hole)))
      (conj (assoc at :finding :policy-hole-free-missing))
      (not (nonempty-string? (:why hole)))
      (conj (assoc at :finding :policy-hole-why-missing))
      (not (contains? hole :unfolded-pattern))
      (conj (assoc at :finding :policy-hole-unfolded-pattern-missing))
      (not (obligation-id? (:obligation/id hole)))
      (conj (assoc at :finding :policy-hole-obligation-id-missing)))))

(defn- condition-findings [box-index condition-index condition]
  (let [at {:box/index box-index :condition/index condition-index}]
    (cond-> []
      (not (and (string? (:condition condition))
                (seq (:condition condition))))
      (conj (assoc at :finding :condition-text-missing))
      (nil? (:status condition))
      (conj (assoc at :finding :condition-status-missing))
      (and (some? (:status condition))
           (not (condition-statuses (:status condition))))
      (conj (assoc at :finding :condition-status-invalid
                      :observed (:status condition)))
      (and (= :absent (:status condition))
           (nil? (:obstruction condition)))
      (conj (assoc at :finding :condition-obstruction-missing))
      (and (contains? #{:established :unchecked} (:status condition))
           (nil? (:witness condition)))
      (conj (assoc at :finding :condition-witness-missing)))))

(defn- box-findings [index box]
  (let [pattern (:fits-pattern box)
        warrant (:warrant-kind box)
        conditions (:conditions box)
        at {:box/index index :box/id (:id box)}]
    (into
     (into
      (cond-> []
       (not (map? pattern))
       (conj (assoc at :finding :box-pattern-reference-invalid))
       (and (map? pattern) (nil? (:pattern/id pattern)))
       (conj (assoc at :finding :box-pattern-id-missing))
       (and (map? pattern) (nil? (:pattern/revision pattern)))
       (conj (assoc at :finding :box-pattern-revision-missing))
       (not (warrant-kinds warrant))
       (conj (assoc at :finding :box-warrant-kind-invalid :observed warrant))
       (not (vector? conditions))
       (conj (assoc at :finding :box-conditions-not-vector))
       (not (vector? conditions))
       (conj (assoc at :finding :condition-status-missing))
       (and (vector? conditions) (empty? conditions)
            (not= :deduction warrant))
       (conj (assoc at :finding :box-conditions-empty))
       (and (contains? box :hole) (not (map? (:hole box))))
       (conj (assoc at :finding :box-hole-invalid)))
      (when (map? (:hole box))
        (hole-findings index (:id box) (:hole box))))
     (when (vector? conditions)
       (mapcat (fn [[condition-index condition]]
                 (condition-findings index condition-index condition))
               (map-indexed vector conditions))))))

(defn validate-fold-output-v1
  "Validate the structured-proof fold contract. Refusal is valid but explicitly
  exceptional; ordinary wiring failures carry stable, machine-readable findings."
  [m]
  (cond
    (nil? m)
    {:ok false :fold/schema :invalid :findings [{:finding :nil-fold-output}]}
    (and (map? m) (:fold/refused m))
    (let [findings (cond-> []
                     (not (and (string? (:why m)) (seq (:why m))))
                     (conj {:finding :refusal-why-missing})
                     (not (keyword? (:refusal/class m)))
                     (conj {:finding :refusal-class-invalid}))]
      {:ok (empty? findings) :fold/schema :refusal
       :fold/exceptional? true :findings findings})
    (not (map? m))
    {:ok false :fold/schema :invalid :findings [{:finding :fold-output-not-map}]}
    :else
    (let [wiring (:wiring m)
          boxes (:boxes wiring)
          holes (:policy-holes m)
          findings
          (into
           (cond-> []
             (not (map? wiring)) (conj {:finding :wiring-missing})
             (not (vector? boxes)) (conj {:finding :boxes-not-vector})
             (not (vector? holes)) (conj {:finding :policy-holes-not-vector})
             (vector? holes)
             (into (mapcat (fn [[index hole]]
                             (policy-hole-findings index hole))
                           (map-indexed vector holes))))
           (when (vector? boxes)
             (mapcat (fn [[index box]] (box-findings index box))
                     (map-indexed vector boxes))))]
      {:ok (empty? findings) :fold/schema :enriched :findings findings})))

(defn- box-pattern-id [box]
  (let [reference (:fits-pattern box)]
    (if (map? reference) (:pattern/id reference) reference)))

(defn validate-fold-correspondence
  "Check that a fold output accounts for its cascade outline. Every cascade
  pattern must be filled by a box or named by a policy hole, and non-deduction
  box warrants must come from the cascade. An outside-outline warrant is a
  finding, not a permission system: how such warrants become licensed is a
  later policy decision that requires evidence."
  [fold-output cascade]
  (let [cascade-set (set cascade)
        boxes (get-in fold-output [:wiring :boxes])
        policy-holes (:policy-holes fold-output)
        box-patterns (keep box-pattern-id boxes)
        unfolded-patterns (keep :unfolded-pattern policy-holes)
        accounted (into (set box-patterns) unfolded-patterns)
        unaccounted (remove accounted cascade)
        outside (keep-indexed
                 (fn [index box]
                   (let [pattern-id (box-pattern-id box)]
                     (when (and (not= :deduction (:warrant-kind box))
                                (some? pattern-id)
                                (not (cascade-set pattern-id)))
                       {:finding :box-warrant-outside-cascade
                        :box/index index :box/id (:id box)
                        :pattern/id pattern-id})))
                 boxes)
        findings (into (mapv (fn [pattern-id]
                               {:finding :cascade-pattern-unaccounted
                                :pattern/id pattern-id})
                             unaccounted)
                       outside)]
    {:ok (empty? findings) :findings findings}))

(defn valid-fold-output-v1?
  "Boolean projection of `validate-fold-output-v1`."
  [m]
  (:ok (validate-fold-output-v1 m)))

(defn coverage-score-leg
  "The gate's coverage-score leg, or nil when no construction was evaluated."
  [fold-output]
  (:coverage-score-delta fold-output))

(defn closes?
  "Does this fold output give the gate a negative coverage-score delta?
   The gate also requires a positive cascade score."
  [fold-output]
  (let [g (coverage-score-leg fold-output)]
    (boolean (and (number? g) (neg? g)))))
