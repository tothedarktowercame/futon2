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
       (and (contains? box :hole)
            (not (obligation-id? (get-in box [:hole :obligation/id]))))
       (conj (assoc at :finding :box-hole-obligation-id-missing)))
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
             (into (keep-indexed
                    (fn [index hole]
                      (when-not (obligation-id? (:obligation/id hole))
                        {:finding :policy-hole-obligation-id-missing
                         :policy-hole/index index}))
                    holes)))
           (when (vector? boxes)
             (mapcat (fn [[index box]] (box-findings index box))
                     (map-indexed vector boxes))))]
      {:ok (empty? findings) :fold/schema :enriched :findings findings})))

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
