(ns futon2.aif.cascade-policy
  "The cascade-grain policy seam — SPECIFICATION: mathlib4
  DarkTower/WarMachine/GOverCascades.lean (a43440ab61), which this namespace
  mirrors and which cites only the concept records (F-wm-piloted-2026-06-12
  §Sortie-12: policy-grade G(s, π) over policies = distributions over
  CASCADES; M-G-over-cascades §1-2; the glossary's π/cascade paragraphs).

  Direction: this namespace conforms to the Lean module, not the other way
  around. The retained F13 HOLE (CascadeGrainSeam.owed) is retired only when
  a production selection presents the cascades this namespace builds to the
  G below, leaves a durable record, and a Lean witness pins that record
  against the specification.

  The organise core mirrors futon3:checks/find_organise.clj (the F12
  reference implementation, conformance-stated by F12Conformance.lean
  O1-O3): nodes are the two-way union of selected and closure-added
  (:admitted-by carried empty until a temperament that fires lands here);
  edges are the ruled O3 fast-forward — selected endpoints connected through
  authored paths whose INTERMEDIATE vertices are unselected, with
  organise-introduced nodes excluded from the carrier so an introduced node
  cannot justify the edge that introduced it."
  (:require [clojure.set :as set]))

;; --- temperaments (policy-grain cascades; READ, not fired: F12) -----------

(def up-closure-temperament
  {:id :take-the-up-closure :grain :policy :closure :stands-on-up-closure
   :precedence []})

(def selected-only-temperament
  {:id :keep-only-what-was-found :grain :policy :closure :selected-only
   :precedence []})

;; --- the ruled O3 fast-forward (Holes.lean:967 fastForward) ---------------

(defn- reach-outside?
  "ReachOutside: an authored path u→v whose intermediate vertices are all
  outside `selected`. `stands-on` is a set of [u v] pairs."
  [selected stands-on u v]
  (let [succ (reduce (fn [m [a b]] (update m a (fnil conj #{}) b)) {} stands-on)]
    (loop [frontier (get succ u #{}) seen #{}]
      (cond
        (contains? frontier v) true
        (empty? frontier) false
        :else
        (let [passable (set/difference (disj frontier v) seen selected)
              next-frontier (reduce set/union #{} (map #(get succ % #{}) passable))]
          (recur (set/difference next-frontier seen)
                 (set/union seen passable)))))))

(defn fast-forward-edges
  "All [u v] with u,v ∈ carrier and ReachOutside carrier stands-on u v."
  [carrier stands-on]
  (set (for [u carrier, v carrier
             :when (and (not= u v) (reach-outside? carrier stands-on u v))]
         [u v])))

;; --- organise (F12; mirror of find_organise.clj, O1-O3) -------------------

(defn organise
  "Cascade policy → Set P → Repository P → Cascade P.
  Repository: {:patterns #{...} :stands-on #{[u v] ...} :acyclic? bool}."
  [temperament selected repository]
  (when-not (set/subset? (set selected) (:patterns repository))
    (throw (ex-info "organise: selected escapes the repository"
                    {:finding :o1-selected-outside-repository
                     :outside (sort (set/difference (set selected)
                                                    (:patterns repository)))})))
  (let [selected (set selected)
        succ (reduce (fn [m [a b]] (update m a (fnil conj #{}) b)) {}
                     (:stands-on repository))
        up-closure (loop [frontier selected acc selected]
                     (let [nxt (set/difference
                                (reduce set/union #{} (map #(get succ % #{}) frontier))
                                acc)]
                       (if (empty? nxt) acc (recur nxt (set/union acc nxt)))))
        added (case (:closure temperament)
                :selected-only #{}
                :stands-on-up-closure (set/difference up-closure selected)
                (throw (ex-info "organise: temperament declares no closure policy"
                                {:finding :no-closure-policy
                                 :temperament temperament})))
        nodes (set/union selected added)]
    {:temperament (:id temperament)
     :selected selected
     :added-by-organise added
     :admitted-by #{}
     :nodes nodes
     ;; Ruled O3: introduced nodes excluded from the fast-forward carrier.
     :edges (fast-forward-edges (set/difference nodes added)
                                (:stands-on repository))
     :precedence (vec (:precedence temperament))
     :acyclic? (:acyclic? repository)}))

;; --- G at the ratified grain (mirror of GOverCascades.cascadeGrainG) ------

(defn cascade-grain-G
  "Policy-grade expected free energy over a CASCADE: risk minus epistemic
  gain, both legs receiving the whole cascade (nodes, edges, precedence),
  never a per-node summary. Mirror of Holes.G instantiated at
  PolicyIndex := Cascade P."
  [risk-fn eig-fn cascade]
  {:cascade cascade
   :risk (double (risk-fn cascade))
   :eig (double (eig-fn cascade))
   :value (double (- (risk-fn cascade) (eig-fn cascade)))})

(defn composition-blind?
  "True when `score-fn` provably factors through the node set alone on the
  given cascades: equal node-bags force equal scores. The Lean module proves
  such a score cannot separate what cascade-grain G separates
  (compositionBlind_cannot_separate / cascadeGrain_separates_sameBag)."
  [score-fn cascades]
  (every? (fn [[c1 c2]] (== (double (score-fn c1)) (double (score-fn c2))))
          (for [c1 cascades, c2 cascades
                :when (and (not= c1 c2) (= (:nodes c1) (:nodes c2)))]
            [c1 c2])))

;; --- distributions over cascades (mirror of cascadePolicyPosterior) -------

(defn cascade-policy-posterior
  "Q(π) ∝ exp(ln E(π) − G(π)/τ − F_π(π)) at PolicyIndex := Cascade —
  §Sortie-12's 'distributions over CASCADES'. Mirror of
  PolicyPosterior.softmaxWithFPi; returns {:cascade :weight :G} rows,
  weights normalized."
  [habit-fn risk-fn eig-fn f-pi-fn tau cascades]
  (let [graded (map #(cascade-grain-G risk-fn eig-fn %) cascades)
        weights (map (fn [{:keys [cascade value]}]
                       (Math/exp (- (Math/log (double (habit-fn cascade)))
                                    (/ value tau)
                                    (double (f-pi-fn cascade)))))
                     graded)
        total (reduce + 0.0 weights)]
    (mapv (fn [{:keys [cascade value]} w]
            {:cascade cascade :G value :weight (/ w total)})
          graded weights)))

(defn select-over-cascades
  "The seam: present a LIST OF CASCADES to G and select over the posterior.
  Returns {:posterior [...] :selected {:cascade ... :G ... :weight ...}}.
  This — not a flat candidate ranking — is the object the ratified
  definition requires selection to consume."
  [habit-fn risk-fn eig-fn f-pi-fn tau cascades]
  (let [posterior (cascade-policy-posterior habit-fn risk-fn eig-fn
                                            f-pi-fn tau cascades)]
    {:posterior posterior
     :selected (apply max-key :weight posterior)}))
