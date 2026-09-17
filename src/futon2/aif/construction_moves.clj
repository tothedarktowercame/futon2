(ns futon2.aif.construction-moves
  "H7c-2: the first four moves of claude-7's cascade-construction library
  (futon3/library/cascade-construction/) as move functions for
  futon2.aif.construction/construct.

  Each move is produced by a CONSTRUCTOR taking injected data and returning
  (fn [family] -> result), so construct stays pure: nothing inside the move
  reads the world. The termination condition of each move is its flexiarg's
  @done line, and its :epistemic-estimate records the flexiarg's
  @epistemic-value line. All four are :state-information (recorded by
  construct, never added — active-horizon-g already values a check's state
  information), so none of them can inflate its own value.

  'The best existing candidate' below is the FIRST candidate of the family:
  construction's G is injected into construct, not into these constructors,
  so the moves take the family as written (its head) as the carrying
  candidate and say so here rather than inventing a scorer."
  (:require [futon2.aif.check-candidates :as cc]))

(def ^:private four-sources
  "read-what-exists-first.flexiarg: the requirement, the implementation,
  the live state and prior attempts — read or recorded unavailable."
  [:requirement :implementation :live-state :prior-attempts])

(defn read-what-exists
  "read-what-exists-first.flexiarg. Injected:
    :sources             {source-key {:read? true :locus ...} |
                                      {:unavailable :reason-keyword}}
    :available-patterns  patterns the sources establish exist for this target
                         (in production: the target's declared cascade source,
                         futon2.aif.cascade-sources — not read here)
    :cost                move cost (default 1)

  Proposes the family plus ONE new candidate: the first candidate extended
  with every available pattern no candidate already uses (by :id), appended
  to :precedence in the order given. Records :sources-read,
  :sources-unavailable and the count of recorded sources as
  :state-information.

  :no-move :sources-not-supplied when :sources lacks any of the four keys
  (per the @done line, read AND recorded-unavailable both count as supplied);
  :no-move :nothing-unread when every available pattern is already used —
  reading more of a source no candidate's guard depends on adds nothing."
  [{:keys [sources available-patterns cost]}]
  (fn [family]
    (let [missing (vec (remove #(contains? sources %) four-sources))]
      (if (seq missing)
        {:status :no-move :move-id :read-what-exists
         :reason :sources-not-supplied :detail {:missing missing}}
        (let [used (into #{}
                         (mapcat (fn [c] (concat (:precedence c)
                                                 (map :id (:patterns c)))))
                         family)
              fresh (vec (remove #(contains? used (:id %)) available-patterns))]
          (if (empty? fresh)
            {:status :no-move :move-id :read-what-exists
             :reason :nothing-unread}
            (let [read (vec (filter #(get-in sources [% :read?]) four-sources))
                  unavail (vec (filter #(contains? (get sources %) :unavailable)
                                       four-sources))
                  base (first family)
                  extended (-> base
                               (update :precedence
                                       #(into (vec %) (map :id fresh)))
                               (update :patterns #(into (vec %) fresh)))]
              {:move-id :read-what-exists
               :proposed-family (conj (vec family) extended)
               :sources-read read
               :sources-unavailable unavail
               :epistemic-estimate
               {:kind :state-information
                :value (+ (count read) (count unavail))
                :basis :read-what-exists-first}
               :cost (or cost 1)})))))))

(defn borrow-a-sibling
  "borrow-a-sibling-cascade.flexiarg. Injected:
    :sibling     {:id ... :rows [{:row :observe :pattern-id ...} ...]}
    :counterpart (fn [row family] -> pattern-or-nil)
    :row-of      (fn [pattern] -> row-keyword-or-nil)
    :cost        move cost (default 1)

  Walks the sibling's rows in order. A row is COVERED when some pattern in
  the family has that row under :row-of. For each uncovered row, :counterpart
  decides: a pattern is appended to the first candidate (:patterns and the
  end of :precedence) — one per call; nil records the row as a GAP and the
  walk moves on in the SAME call. A gap is a finding, never filled with an
  invented pattern: :gaps [row ...] is returned either way.

  :no-move :sibling-rows-walked when the walk ends with every row covered or
  recorded as a gap (the @done line: the walk is finite, so this move cannot
  run on)."
  [{:keys [sibling counterpart row-of cost]}]
  (fn [family]
    (let [covered-rows (into #{}
                             (comp (mapcat :patterns)
                                   (keep row-of))
                             family)
          uncovered (remove #(contains? covered-rows (:row %))
                            (:rows sibling))]
      (loop [[row & more] uncovered
             gaps []]
        (if (nil? row)
          {:status :no-move :move-id :borrow-a-sibling
           :reason :sibling-rows-walked :gaps gaps}
          (let [p (counterpart (:row row) family)]
            (if p
              (let [base (first family)
                    extended (-> base
                                 (update :precedence conj (:id p))
                                 (update :patterns conj p))]
                {:move-id :borrow-a-sibling
                 :proposed-family (conj (vec family) extended)
                 :row-covered (:row row)
                 :gaps gaps
                 :epistemic-estimate
                 {:kind :state-information
                  :value (count gaps)
                  :basis :borrow-a-sibling-cascade}
                 :cost (or cost 1)})
              (recur more (conj gaps (:row row))))))))))

(defn- stable-topo
  "Stable topological order of IDS under DEPS (id -> coll of prerequisite
  ids): among ids with no remaining unmet prerequisite, the one written
  earliest (per IDX) goes first — where two patterns need nothing from each
  other, do not invent an order. nil when a dependency cycle prevents
  placing every id."
  [ids idx deps]
  (let [dependents
        (reduce (fn [m [id prereqs]]
                  (reduce (fn [m2 d] (update m2 d (fnil conj []) id))
                          m prereqs))
                {} deps)]
    (loop [remaining (vec ids)
           indeg (into {} (map (fn [id] [id (count (distinct (get deps id)))]))
                      ids)
           out []]
      (if (empty? remaining)
        out
        (let [ready (sort-by idx (filter #(zero? (get indeg %)) remaining))]
          (if (empty? ready)
            nil
            (let [pick (first ready)]
              (recur (filterv #(not= % pick) remaining)
                     (reduce (fn [m d] (update m d dec)) indeg
                             (get dependents pick))
                     (conj out pick)))))))))

(defn order-by-need
  "order-by-what-each-step-needs.flexiarg. Injected: {:cost 0}. No other
  data: the order comes from the guards themselves.

  For each candidate, reorders :precedence so a pattern whose guard :needs
  token t comes after the pattern whose :produces contains t, by a stable
  topological sort (existing relative order kept among independent
  patterns). A need produced by no pattern in the candidate is an UNMET
  NEED: recorded under :unmet-needs, not reordered around — these are
  exactly the check candidates add-a-check turns into checks. A dependency
  cycle leaves that candidate's order unchanged, recorded under :cycles.

  :no-move :already-ordered when no candidate's :precedence changes."
  [{:keys [cost]}]
  (fn [family]
    (let [reordered
          (mapv
           (fn [c]
             (let [pats (vec (:patterns c))
                   idx (into {} (map-indexed (fn [i p] [(:id p) i])) pats)
                   producers (reduce (fn [m p]
                                       (reduce #(update %1 %2 (fnil conj [])
                                                       (:id p))
                                               m (set (:produces p))))
                                     {} pats)
                   unmet (vec (sort (distinct
                                     (filter #(empty? (get producers %))
                                             (mapcat #(-> % :guard :needs)
                                                     pats)))))
                   deps (into {}
                              (map (fn [p]
                                     (let [needs (set (-> p :guard :needs))]
                                       [(:id p)
                                        (vec (distinct
                                              (remove #{(:id p)}
                                                      (mapcat #(get producers %)
                                                              needs))))])))
                              pats)
                   order (stable-topo (mapv :id pats) idx deps)]
               {:candidate c
                :precedence (if (nil? order) (:precedence c) order)
                :cyclic? (nil? order)
                :unmet unmet}))
           family)
          changed (not= (mapv :precedence reordered)
                        (mapv :precedence family))
          findings {:unmet-needs (vec (keep #(when (seq (:unmet %))
                                                {:candidate (get-in % [:candidate :id])
                                                 :needs (:unmet %)})
                                           reordered))
                    :cycles (vec (keep #(when (:cyclic? %)
                                          {:candidate (get-in % [:candidate :id])
                                           :precedence (:precedence %)})
                                       reordered))}]
      (if-not changed
        ;; unchanged order, but findings are still findings
        (merge {:status :no-move :move-id :order-by-need
                :reason :already-ordered}
               findings)
        (merge {:move-id :order-by-need
                :proposed-family (mapv #(assoc (:candidate %)
                                               :precedence (:precedence %))
                                       reordered)}
               findings
               {:epistemic-estimate
                {:kind :state-information
                 :value (count (distinct (mapcat :unmet reordered)))
                 :basis :order-by-what-each-step-needs}
                :cost (or cost 0)})))))

(defn add-a-check
  "add-a-pattern-when-an-item-fits-no-class.flexiarg, read here as: add a
  CHECK for an unknown fact. The gap this flexiarg names, in the
  construction runtime, is a gating fact nothing observes. Injected:
    :facts        {fact true | false | :unknown}
    :want         coll of want tokens
    :check-theta  the check's probability of returning an answer (REQUIRED
                  by check-candidates, never defaulted)
    :cost         move cost (default 1)

  Calls futon2.aif.check-candidates/check-patterns with the family's
  patterns (union over candidates, de-duplicated by :id; :kind :check
  patterns are excluded — check-candidates knows neither their guard shape
  nor their gating role) and adds the FIRST
  returned check whose :id is not already in the family to EVERY candidate,
  at the FRONT of :precedence (a check must be able to act before the
  patterns that need its fact). One check per move, so the move budget stays
  meaningful.

  :no-move :no-unknown-gating-fact (no checks, or all already present);
  :check-candidates-refused with check-candidates' typed law under :detail
  when check-patterns refuses; :inputs-not-supplied when
  facts/want/check-theta are missing."
  [{:keys [facts want check-theta cost]}]
  (fn [family]
    (if (or (nil? facts) (nil? want) (nil? check-theta))
      {:status :no-move :move-id :add-a-check :reason :inputs-not-supplied}
      (let [patterns (vals (into {} (map (juxt :id identity))
                                 (remove #(identical? :check (:kind %))
                                         (mapcat :patterns family))))
            present (into #{}
                          (mapcat (fn [c] (concat (:precedence c)
                                                  (map :id (:patterns c)))))
                          family)
            result (try
                     (cc/check-patterns {:facts facts :want want
                                         :patterns patterns
                                         :check-theta check-theta})
                     (catch clojure.lang.ExceptionInfo e
                       {:refused (:law (ex-data e))}))]
        (if (contains? result :refused)
          {:status :no-move :move-id :add-a-check
           :reason :check-candidates-refused :detail (:refused result)}
          (let [check (first (remove #(contains? present (:id %))
                                     (:checks result)))]
            (if (nil? check)
              {:status :no-move :move-id :add-a-check
               :reason :no-unknown-gating-fact}
              {:move-id :add-a-check
               :proposed-family
               (mapv (fn [c]
                       (-> c
                           (update :precedence #(into [(:id check)] (vec %)))
                           (update :patterns #(into [check] (vec %)))))
                     family)
               :check check
               :not-gating (:not-gating result)
               :epistemic-estimate
               {:kind :state-information
                :value check-theta
                :basis :check-candidates}
               :cost (or cost 1)})))))))
