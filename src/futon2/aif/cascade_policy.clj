(ns futon2.aif.cascade-policy
  "Runtime ruled organiser: DarkTower/WarMachine/F12RuledCarrier.lean,
  ConformantOrganiseRuled (osel, oauth, oattr, O1-O4). Admissions are explicit
  and attributed; O3 excludes organise-added nodes from its carrier.

  Canonical cascade G is now specified by CascadeEFE.lean and
  CascadeEFEPolicies.lean. The legacy risk/eig helpers below are not that G;
  a runtime scorer and a recorded production selection remain separate work."
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

(def first-attempt-cascade
  "Explicit empty predecessor, not an inferred missing previous cascade."
  {:carrier :first-attempt :nodes #{} :precedence []})

(defn- require-law! [ok law reason data]
  (when-not ok
    (throw (ex-info "Ruled organise refused"
                    (merge {:finding :organise/refusal :law law :reason reason} data)))))

(defn- validate-inputs! [previous selected repository admitted]
  (require-law! (and (map? previous) (set? (:nodes previous))
                     (vector? (:precedence previous)))
                :carrier :previous-cascade-required {})
  (when (= :first-attempt (:carrier previous))
    (require-law! (and (empty? (:nodes previous)) (empty? (:precedence previous)))
                  :carrier :nonempty-first-attempt {}))
  (require-law! (and (set? selected) (set? (:patterns repository))
                     (set? (:stands-on repository)) (map? admitted))
                :carrier :invalid-input-shape {})
  (let [patterns (:patterns repository) edges (:stands-on repository)]
    (require-law! (every? #(and (vector? %) (= 2 (count %))
                                (every? (fn [p] (contains? patterns p)) %)) edges)
                  :repository :edge-outside-repository {})
    (require-law! (set/subset? selected patterns) :osel :selected-outside-repository
                  {:outside (set/difference selected patterns)})
    (require-law! (set/subset? (set (keys admitted)) patterns) :oattr :admitted-outside-repository
                  {:outside (set/difference (set (keys admitted)) patterns)})
    (doseq [[p authority] admitted]
      (require-law! (and (map? authority) (keyword? (:authority authority))
                         (string? (:source authority)) (seq (:source authority)))
                    :oattr :admission-authority-required {:pattern p}))
    ;; Positive-length reachability, including a return to the start, detects
    ;; both self loops and longer cycles. Caller :acyclic? is never consulted.
    (doseq [p patterns]
      (require-law! (not (reach-outside? #{} edges p p))
                    :repository :cyclic-stands-on {:vertex p}))))

(defn validate-cascade-diff!
  "Return DIFF or throw {:finding :organise/refusal :law ... :reason ...}.
  Validate all seven ruled laws against the actual input arguments."
  [previous selected repository admitted diff]
  (validate-inputs! previous selected repository admitted)
  (doseq [k [:selected :nodes :added-by-organise :admitted-by :authored-edges :organised-edges]]
    (require-law! (set? (get diff k)) :carrier :set-field-required {:field k}))
  (doseq [k [:precedence-before :precedence-after :acting-order-before :acting-order-after]]
    (require-law! (vector? (get diff k)) :carrier :order-field-required {:field k}))
  (doseq [k [:score-before :score-after]]
    (require-law! (and (contains? diff k) (some? (get diff k)))
                  :carrier :score-required {:field k}))
  (require-law! (= (:precedence previous) (:precedence-before diff))
                :carrier :previous-precedence-mismatch {})
  (require-law! (= selected (:selected diff)) :osel :selection-mismatch {})
  (require-law! (= (:stands-on repository) (:authored-edges diff)) :oauth :authorship-mismatch {})
  (require-law! (= (set (keys admitted)) (:admitted-by diff)) :oattr :admission-mismatch {})
  (require-law! (= admitted (get-in diff [:provenance :admissions])) :oattr :authority-mismatch {})
  (require-law! (= (:nodes diff) (set/union selected (:added-by-organise diff) (:admitted-by diff)))
                :o1 :node-union-mismatch {})
  (require-law! (set/subset? (:nodes diff) (:patterns repository)) :carrier :nodes-outside-repository {})
  (doseq [edge (:organised-edges diff)]
    (require-law! (and (vector? edge) (= 2 (count edge))
                       (reach-outside? #{} (:stands-on repository) (first edge) (second edge)))
                  :o2 :edge-not-authored-reachable {:edge edge}))
  (require-law! (= (:organised-edges diff)
                   (fast-forward-edges (set/difference (:nodes diff) (:added-by-organise diff))
                                       (:stands-on repository)))
                :o3 :fast-forward-mismatch {})
  (require-law! (or (= (:precedence-before diff) (:precedence-after diff))
                    (not= (:acting-order-before diff) (:acting-order-after diff))
                    (not= (:score-before diff) (:score-after diff)))
                :o4 :precedence-change-without-consequence {})
  diff)

(defn organise
  "Previous cascade → selected set → repository → attributed admissions → diff.
  ADMITTED maps each repository pattern to {:authority keyword :source string}.
  OPTS requires :temperament (id, closure, precedence), :acting-order-fn and
  :score-fn. Both ports receive a cascade {:nodes ... :edges ... :precedence ...}
  (the previous cascade unchanged on the before arm). Scores stay in the port's
  declared domain. Even unchanged precedence requires explicit observations;
  no empty order or score is manufactured. Packet 4 supplies the semantic ports.
  ARM 6 item 6 (RULINGS-walkthrough-2026-09-07): closure-added nodes exclude
  explicit admissions, preserving selected union admitted as the O3 carrier."
  [previous-cascade candidate-space repository admitted opts]
  (validate-inputs! previous-cascade candidate-space repository admitted)
  (let [{:keys [temperament acting-order-fn score-fn]} opts
        selected candidate-space
        succ (reduce (fn [m [a b]] (update m a (fnil conj #{}) b)) {} (:stands-on repository))
        _ (require-law! (and (some? (:id temperament)) (vector? (:precedence temperament)))
                        :carrier :temperament-required {})
        _ (require-law! (and (fn? acting-order-fn) (fn? score-fn))
                        :o4 :observation-ports-required {})
        up-closure (loop [frontier selected acc selected]
                     (let [nxt (set/difference (reduce set/union #{} (map #(get succ % #{}) frontier)) acc)]
                       (if (empty? nxt) acc (recur nxt (set/union acc nxt)))))
        added (case (:closure temperament)
                :selected-only #{}
                :stands-on-up-closure (set/difference up-closure (set/union selected (set (keys admitted))))
                (require-law! false :carrier :no-closure-policy {:temperament temperament}))
        admissions (set (keys admitted))
        nodes (set/union selected added admissions)
        edges (fast-forward-edges (set/difference nodes added) (:stands-on repository))
        after {:nodes nodes :edges edges :precedence (:precedence temperament)}
        diff {:selected selected :nodes nodes :added-by-organise added :admitted-by admissions
              :authored-edges (:stands-on repository) :organised-edges edges
              :precedence-before (:precedence previous-cascade) :precedence-after (:precedence after)
              :acting-order-before (acting-order-fn previous-cascade) :acting-order-after (acting-order-fn after)
              :score-before (score-fn previous-cascade) :score-after (score-fn after)
              :provenance {:temperament (:id temperament) :admissions admitted}}]
    (validate-cascade-diff! previous-cascade selected repository admitted diff)))

;; --- legacy scoring helpers (superseded; runtime G2 still owed) ----------

(defn cascade-grain-G
  "Legacy arbitrary risk-minus-eig helper. Superseded by mathlib4 CascadeEFE;
  this is NOT canonical cascade G. Kept unchanged until the runtime G2 packet."
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
