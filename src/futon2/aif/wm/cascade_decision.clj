(ns futon2.aif.wm.cascade-decision
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.candidate-derivations :as candidate-derivations]
            [futon2.aif.cascade-model-manifest :as cascade-manifest]
            [futon2.aif.cascade-policy :as cascade-policy]
            [futon2.aif.cascade-problems :as cascade-problems]
            [futon2.aif.controller-authority :as controller-authority]
            [futon2.aif.previous-run :as previous-run]
            [futon2.aif.decision-gate :as decision-gate]
            [futon2.aif.efe :as efe]
            [futon2.aif.focus-receipt :as focus-receipt]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.learning-trial-ledger :as learning-ledger]
            [futon2.aif.live-c :as live-c]
            [futon2.aif.mission-registry :as mission-registry]
            [futon2.aif.observation-label-reader :as label-reader]
            [futon2.aif.observation-labels :as observation-labels]
            [futon2.aif.observation-rates :as observation-rates]
            [futon2.aif.parameter-novelty :as novelty]
            [futon2.aif.policy :as policy]
            [futon2.aif.policy-depth :as policy-depth]
            [futon2.aif.policy-precision-carry :as precision-carry]
            [futon2.aif.policy-prefix-admission :as prefix-admission]
            [futon2.aif.policy-prefix-evidence :as policy-prefix]
            [futon2.aif.receipt-construction :as receipt-construction]
            [futon2.aif.scoring-input-receipts :as input-receipts]
            [futon2.aif.ticket-queue :as ticket-queue]
            [futon2.aif.token-a-bmr :as token-a-bmr]
            [futon2.aif.token-belief-carry :as token-carry]
            [futon2.aif.token-belief-predecessor :as token-predecessor]
            [futon2.aif.zeta-posterior :as zeta-posterior])
  (:import (java.time Instant)))
(defn- observation-contract []
  (clojure.edn/read-string (slurp (io/resource "wm/observation-contract.edn"))))

(defn- authorised-prior?
  [{:keys [alpha beta authority]}]
  (and (or (integer? alpha) (ratio? alpha))
       (pos? alpha)
       (or (integer? beta) (ratio? beta))
       (pos? beta)
       (string? authority)
       (not (str/blank? authority))))

(defn- lane-step
  "Run one cascade-lane node call. A typed refusal — an ex-info thrown by the
  node function or a returned {:status … :kind …} refusal map — is returned
  as {:refusal …}, never coerced into a value."
  [f]
  (try
    (let [v (f)]
      (if (and (map? v) (contains? v :status) (contains? v :kind))
        {:refusal v}
        {:value v}))
    (catch clojure.lang.ExceptionInfo e
      {:refusal (let [d (ex-data e)]
                  {:kind (or (:finding d) (:law d) (:reason d) :typed-refusal)
                   :message (ex-message e)
                   :data d})})))

(defn token-preference-schedule
  "Write-only receipt from R5's effective scoring spec. An absent placement
   has preference-member's constant (every-step) semantics. Source ids are
   the target ids that key cascade-sources' declarations."
  [spec source-id]
  {:schema :wm/preference-schedule-v1
   :placement (or (get-in spec [:c-schedule :placement :value]) :every-step)
   :source (if (= :declared (get-in spec [:c-schedule :placement :status]))
             [:declared source-id]
             :defaulted)
   :family :token
   :lam (:lam spec)
   :mu (:mu spec)
   :weights-ruling :none-found})

(defn cascade-lane
  "The cascade lane of the tick (VM-PROTOCOL Figure 5A node order; R10 wiring,
  tick 1). PROBLEM is a cascade problem map:

    {:facts {fact-id true|false|:unknown}    R2's adjudicated facts
     :want [token …]                         the want signature
     :interpretations {pattern-id {:guard {:needs #{} :forbids #{}}
                                   :produces #{}}}   R6's on-the-fly readings
     :repository {:patterns #{} :stands-on #{[u v]}}
     :precedences [[pattern-id …] …]         authored candidate orders
     :horizon-steps T                        R13's declared common horizon
     :cascade-spec {:want #{} …}             R5's preference spec
     :beta β}                                R14's DECLARED temperature

  It runs R1 → R6 → R13 → R4 → R5 → R14 → R16 → R9 through the real node
  functions (no maths is re-derived here) and route-tags each node in order:

    :R1  cascade-model-manifest/observed-belief      q0 from the true facts
    :R6  cascade-policy/candidate-space              C0..Cn from the problem
    :R13 policy-depth/configured                     the common T, typed,
                                                     never defaulted
    :R4  forward-model/predict-multi-horizon         every candidate at T
    :R5  efe/rank-actions                            G over a common universe
    :R14 policy/select-action-cascades → authorize   at the declared β
    :R16 receipt-construction/acting-order           the enactment PLAN on
                                                     the facts
    :R9  the certification below

  Returns {:route [route-tags …] :candidates … :predictions … :ranked …
  :decision … :authorization … :enactment-plan … :certification
  {:status :independent-check-required :enactor …}}. R16 here is the
  machine's enactment plan (the acting order); real source edits are a
  builder's work and never part of the tick. R9: the tick never marks its
  own enactment confirmed — the claim is :independent-check-required with
  the enactor recorded. Any node's typed refusal stops the lane and is
  returned with the route so far: {:route … :refusal … :stopped-at <node>}.

  With OPTS {:through :R5} the lane stops after R5 and returns
  {:route … :candidates … :predictions … :ranked …}: G for every candidate,
  no selection and no β needed. The constructor scores a candidate this
  way, with the same G selection uses (see `constructed-candidate-g`)."
  ([problem] (cascade-lane problem {}))
  ([problem {:keys [through universe observation-labels] :as lane-opts}]
  (let [{:keys [facts want interpretations repository precedences horizon-steps
                cascade-spec beta]} problem
        route (atom [])
        state (atom {})
        stopped (atom nil)
        halted (atom false)
        step (fn [node via f]
               (when-not (or @stopped @halted)
                 (swap! route conj {:node node :via via
                                    :at (str (Instant/now))})
                 (let [r (lane-step f)]
                   (if (:refusal r)
                     (reset! stopped {:node node :refusal (:refusal r)})
                     (swap! state assoc node (:value r))))))
        stop! (fn [node refusal]
                (reset! stopped {:node node :refusal refusal}))]
    ;; R1 — belief from the true facts (absence is the negative: a false or
    ;; :unknown fact contributes no token).
    (step :R1 "futon2.aif.cascade-model-manifest/observed-belief"
          (fn []
            (cascade-manifest/observed-belief
             (set (for [[fact v] facts :when (true? v)] fact)))))
    ;; R6 — candidate action space from the problem's own readings; the empty
    ;; cascade is always the first candidate (candidate-space's own law).
    (step :R6 "futon2.aif.cascade-policy/candidate-space"
          (fn []
            (cascade-policy/candidate-space
             (cond-> {:q0 (ffirst (get @state :R1))
                      :want want
                      :interpretations interpretations
                      :repository repository}
               (seq precedences) (assoc :precedences (vec precedences))))))
    ;; R13 — the common horizon, read as a typed declared value through the
    ;; real depth entry point. A missing or non-positive :horizon-steps is a
    ;; typed refusal (:missing-common-horizon); no default is applied.
    (step :R13 "futon2.aif.policy-depth/configured"
          (fn []
            (policy-depth/configured
             {:policy-depth (when (pos-int? horizon-steps)
                              {:anticipation horizon-steps
                               :cascade-rollout horizon-steps})})))
    (when (and (not @stopped) (nil? (get @state :R13)))
      (stop! :R13 {:kind :missing-common-horizon
                   :horizon-steps horizon-steps
                   :message "the cascade lane requires a declared positive :horizon-steps; no default"}))
    ;; R4 — predict every candidate at the SAME declared T. A typed
    ;; prediction refusal (e.g. :missing-cascade-belief) stops the lane.
    (step :R4 "futon2.aif.forward-model/predict-multi-horizon"
          (fn []
            (let [T (get-in @state [:R13 :cascade-rollout])
                  pattern-maps
                  (into {}
                        (map (fn [[id x]]
                               [id (cascade-policy/token-interpretation id x)]))
                        interpretations)
                  candidates
                  (mapv (fn [c]
                          (cond-> {:kind :cascade-candidate
                                   :id (:id c)
                                   :precedence (mapv pattern-maps (:precedence c))}
                            (empty? (:precedence c))
                            (assoc :type :no-op)))
                        (:candidates (get @state :R6)))
                  predictions
                  (mapv (fn [a]
                          {:cascade-id (:id a)
                           :prediction (fm/predict-multi-horizon
                                        {:cascade-belief (get @state :R1)}
                                        a T)})
                        candidates)
                  refused (some (fn [{:keys [prediction]}]
                                  (when (and (map? prediction)
                                             (contains? prediction :status)
                                             (contains? prediction :kind))
                                    prediction))
                                predictions)]
              (if refused
                refused
                {:candidates candidates :predictions predictions}))))
    ;; R5 — G per candidate over one common universe, all candidates at T.
    ;; WIRE-5: when the problem carries :locators (cascade-problems'
    ;; assemble puts them there; every token must have a CHECKABLE locator
    ;; or the problem was never admitted), the lane SOURCES the adjudication
    ;; rates from futon2.aif.observation-rates instead of letting the
    ;; scorer default to the identity kernel. With no admitted judgement
    ;; labels yet, an all-checkable universe sources the EXACT ZERO kernel
    ;; (tokenLikelihood_checkable) — numerically the identity kernel, but
    ;; provenance-recorded as :sourced, not defaulted. A judgement-class
    ;; token with no admitted rate is the producer's :unsupported-class
    ;; refusal and stops the lane here: that refusal is the finding, never
    ;; padded. A problem without :locators (hand-built) keeps the previous
    ;; behaviour and the certificate records :identity-default.
    (step :R5 "futon2.aif.efe/rank-actions"
          (fn []
            (let [locators (:locators problem)
                  ;; One entry snapshot supplies the lane and measured-A.
                  ;; No view keeps the unmeasured zero kernel and nil prior.
                  labels (vec (:labels observation-labels))
                  sourced (when (map? locators)
                            (observation-rates/sourced-rates
                             labels (or (:subjects observation-labels) {}) (:prior observation-labels)
                             locators (observation-contract)))
                  adopted (when sourced
                            (if (authorised-prior? (:prior observation-labels))
                              (token-a-bmr/adopt-error-free
                               sourced
                               (token-a-bmr/score
                                (observation-rates/rates-by-class
                                 labels (or (:subjects observation-labels) {})
                                 (:prior observation-labels))
                                (:prior observation-labels)))
                              (assoc sourced :adoption
                                     {:status :absent
                                      :reason :prior-not-authorised})))
                  ;; H-VALUE-G-D (2026-09-25): the scored universe is the
                  ;; PROBLEM's declared token universe (facts, want, every
                  ;; interpreted pattern's guard and produces — the same set
                  ;; locator coverage uses), not the candidate family's, so
                  ;; every evaluate-g call of one problem normalises ln Z
                  ;; over one universe and compared G's are commensurable.
                  base-opts (merge (select-keys lane-opts [:zeta :zeta-basis])
                              {:f-prefix-production? true
                             :horizon-steps (get-in @state [:R13 :cascade-rollout])
                             :cascade-spec cascade-spec
                             :universe (or universe
                                           (cascade-problems/problem-tokens
                                            facts want interpretations))})]
              (cond
                ;; no locators on the problem: previous behaviour, the
                ;; certificate records :identity-default.
                (nil? sourced)
                (efe/rank-actions {:cascade-belief (get @state :R1)}
                                  (:candidates (get @state :R4))
                                  base-opts)
                ;; the producer refused (judgement class with no admitted
                ;; rate, or an unknown class): the refusal IS the finding.
                ;; Returning it here makes lane-step stop the lane at R5 —
                ;; it is never merged into opts where it would be ignored
                ;; and silently default to the identity kernel.
                (not= :sourced (:status adopted))
                adopted
                :else
                (efe/rank-actions {:cascade-belief (get @state :R1)}
                                  (:candidates (get @state :R4))
                                  (merge base-opts
                                         {:adjudication-rates (:rates adopted)
                                          :rates-provenance
                                          {:source (:source adopted)
                                           :basis (:basis adopted)
                                           :labels (if (some :admitted labels)
                                                     {:admitted (count (filter :admitted labels))}
                                                     :none-admitted)
                                           :measurement (:measurement adopted)
                                           :adoption (:adoption adopted)
                                           :effective-rates (:rates adopted)
                                           :contract :wm/observation-contract-v1}}))))))
    (when (= :R5 through) (reset! halted true))
    ;; R14 — selection at the DECLARED β (no default: a missing β is
    ;; selection-posterior's typed refusal :invalid-temperature), then
    ;; authorize the decision on the same :R14 node — one node, both legs,
    ;; so the lane's route carries each node exactly once.
    (step :R14 "futon2.aif.policy/select-action-cascades"
          (fn []
            (let [;; Q6: a bound futon2.aif.previous-run/*excluded-pair*
                  ;; (a re-decision after a refusal with an unchanged
                  ;; selection-input digest) removes that exact
                  ;; (target, cascade) pair from the admissible candidates
                  ;; before selection; the exclusion is recorded as typed
                  ;; evidence on the decision.
                  ranked (previous-run/excluded-ranked
                          (policy-prefix/production-ranked (get @state :R5) nil))
                  decision (policy/select-action-cascades (:kept ranked)
                                                          {:beta beta})
                  decision (cond-> decision
                             (seq (:excluded ranked))
                             (assoc :q6-exclusion
                                    {:excluded-by :q6-repeat-after-refusal
                                     :pair (:pair ranked)
                                     :excluded-count (count (:excluded ranked))}))
                  ;; The candidates' :precedence carries manifest pattern MAPS
                  ;; (that is what R4/R5 consume), so select-action-cascades
                  ;; keys the action marginal by the pattern map; re-key it by
                  ;; the pattern id for the judgement — same probabilities,
                  ;; readable names, no value touched.
                  decision (assoc decision
                                  :softmax-weights
                                  (into {}
                                        (map (fn [[k v]]
                                               [(if (map? k)
                                                  (or (:id k)
                                                      (first (:precedence k)))
                                                  k)
                                                 v]))
                                        (:softmax-weights decision)))]
              {:decision decision
               :authorization (controller-authority/authorize decision
                                                              (get @state :R5))})))
    ;; R16 — the enactment PLAN: acting order for the chosen cascade on the
    ;; facts. Interpretations are translated into acting-order's Strong-Kleene
    ;; guard language (one conjunctive clause per reading: every :needs token
    ;; a [:fact _], every :forbids token a [:not [:fact _]]); facts are
    ;; closed-world over the token universe and mirror R1's tokenization —
    ;; only a literal-true fact is true; absent, false and :unknown are
    ;; literal false (an :unknown guard value would never fire a :not, per
    ;; 08-R16's precedent and receipt_construction's own tests).
    (when-not (or @stopped @halted)
      (step :R16 "futon2.aif.receipt-construction/acting-order"
            (fn []
              (let [chosen (get (:decision (get @state :R14)) :action)
                    chosen-precedence (mapv :id (:precedence chosen))
                    sk-interpretations
                    (into {}
                          (map (fn [[id {:keys [guard produces]}]]
                                 [id {:guard
                                      (into [:and]
                                            (concat
                                             (map (fn [t] [:fact t])
                                                  (:needs guard))
                                             (map (fn [t] [:not [:fact t]])
                                                  (:forbids guard))))
                                      :effect (into {} (map (fn [t] [t true])
                                                            produces))}]))
                          interpretations)
                    universe
                    (reduce (fn [acc [_ {:keys [guard produces]}]]
                              (clojure.set/union acc (:needs guard)
                                                 (:forbids guard) produces))
                            (clojure.set/union (set (keys facts))
                                               (ffirst (get @state :R1))
                                               (set want))
                            interpretations)
                    facts-closed (into {}
                                       (map (fn [t] [t (true? (get facts t))]))
                                       universe)]
                {:chosen-cascade (:id chosen)
                 :acting-order (receipt-construction/acting-order
                                sk-interpretations facts-closed
                                chosen-precedence)}))))
    (cond
      @stopped
      {:route @route
       :refusal (:refusal @stopped)
       :stopped-at (:node @stopped)}
      @halted
      {:route @route
       :candidates (:candidates (:R4 @state))
       :predictions (:predictions (:R4 @state))
       :ranked (:R5 @state)}
      :else
      (let [s @state
            _ (swap! route conj {:node :R9
                                 :via "futon2.report.war-machine/cascade-lane"
                                 :at (str (Instant/now))})]
        ;; R9 — no self-certification: the enactment claim stays
        ;; :independent-check-required, with the enactor recorded; the tick
        ;; never marks its own enactment confirmed.
        {:route @route
         :candidates (:candidates (:R4 s))
         :predictions (:predictions (:R4 s))
         :ranked (:R5 s)
         :decision (assoc (:decision (:R14 s))
                          :preference-schedule
                          (token-preference-schedule
                           (get-in (meta (:R5 s)) [:cascade-scoring :spec])
                           (:preference-source-id problem)))
         :authorization (:authorization (:R14 s))
         :enactment-plan (:R16 s)
         :certification {:status :independent-check-required
                         :enactor "futon2.report.war-machine/cascade-lane"
                         :claim :enactment-plan}})))))

(defn- observation-label-view [opts]
  (if (contains? opts :observation-labels-view)
    (:observation-labels-view opts)
    (if-let [path (:observation-labels-path opts)]
      (label-reader/read-rates-inputs path (observation-labels/loaded-identities))
      {:status :absent :reason :no-label-store-configured})))

(defn- observation-label-inputs [view]
  (when-not (:status view) (select-keys view [:labels :subjects :prior])))

(def ^:private token-a-bmr-prototype-prior
  {:alpha 1
   :beta 1
   :authority "claude-1 2026-09-28: uniform parent prior for the item 6 (c) prototype score; Joe: build (c) as a prototype"})

(defn- token-a-bmr-receipt
  "Write-only item 6(c) prototype score from the decision's label snapshot.
   Every failure is data: this receipt can neither refuse nor change selection."
  [view]
  (if (or (nil? view) (:status view))
    {:status :absent :reason :no-observation-labels}
    (try
      (let [{:keys [labels subjects]} (observation-label-inputs view)
            view-prior (:prior view)
            from-view? (authorised-prior? view-prior)
            prior (if from-view? view-prior token-a-bmr-prototype-prior)
            rates (observation-rates/rates-by-class (vec labels) (or subjects {}) prior)]
        (if (:status rates)
          {:status :absent :reason :rates-refused :refusal rates}
          (assoc (token-a-bmr/score rates prior)
                 :prior-source (if from-view? :view :prototype-default))))
      (catch Throwable t
        {:status :absent
         :reason :token-a-bmr-unavailable
         :error {:class (.getName (class t))
                 :message (ex-message t)}}))))

(defn- observation-label-certificate [view opts]
  (merge (select-keys view [:snapshot-sha256 :identities :subjects :excluded
                            :minimum :prior :reason])
         {:status (or (:kind view) (:status view) :sourced)
          :labels-count (count (:labels view))
          :constructor-scored-with (or (:constructor-scored-with opts)
                                       :not-scored-in-this-call)}))

(defn constructed-candidate-g
  "G of one constructed CANDIDATE on its target's PROBLEM (a cascade problem
  without :precedences), as {:value G :universe [token ...]}, computed by the lane's own R1-R5 over a fixed
  family: the single-pattern order of every interpretation enabled on the
  problem's facts, plus the candidate's own order. (R6's law O4 refuses an
  order whose pattern cannot fire, so disabled patterns are left out.) The
  universe G is taken over is the problem's declared token universe
  (cascade-problems/problem-tokens), passed to the lane explicitly
  (H-VALUE-G-D), so it is the same for every candidate the constructor
  compares, the empty cascade included (R6 always adds it as C0). This is the
  constructor's :evaluate-g, so a constructed plan is taken only if the same
  G selection uses scores it better than the empty family -- no G is
  injected or pinned (E-cascade-real D12). A lane refusal is thrown with its
  data, and the constructor carries it as its refusal."
  ([problem candidate] (constructed-candidate-g problem candidate {}))
  ([problem candidate opts]
  (let [prec (vec (:precedence candidate))
        true-facts (set (for [[t v] (:facts problem) :when (true? v)] t))
        enabled? (fn [[_ {:keys [guard]}]]
                   (and (every? true-facts (:needs guard))
                        (not-any? true-facts (:forbids guard))))
        singles (mapv (comp vector key)
                      (sort-by (comp pr-str key) (filter enabled? (:interpretations problem))))
        family (vec (distinct (cond-> singles (seq prec) (conj prec))))
        ;; H-VALUE-G-D: score over the problem's declared token universe so
        ;; every evaluate-g call of one problem — and the empty baseline it
        ;; is subtracted from at construction.clj — is normalised over the
        ;; SAME universe (the cross-universe subtraction differed by T·k·ln2).
        universe (cascade-problems/problem-tokens
                  (:facts problem) (:want problem) (:interpretations problem))
        lane (cascade-lane (assoc problem :precedences family)
                           {:through :R5 :universe universe
                            :observation-labels (observation-label-inputs
                                                 (:observation-labels-view opts))})]
    (when (:refusal lane)
      (throw (ex-info "constructed-candidate-g: lane refused"
                      {:constructor/refusal :lane-refused :refusal (:refusal lane)
                       :stopped-at (:stopped-at lane)})))
    (let [id-of (into {} (map (fn [c] [(:id c) (mapv :id (:precedence c))]) (:candidates lane)))
          entry (first (filter #(= prec (get id-of (:cascade-id %))) (:ranked lane)))]
      (when-not entry
        (throw (ex-info "constructed-candidate-g: candidate not ranked"
                        {:constructor/refusal :candidate-not-ranked :precedence prec})))
      ;; the universe G was taken over, so the construction receipt records
      ;; the scorer's universe rather than one the caller declares for it
      {:value (double (:G-efe entry)) :universe (vec (sort-by pr-str universe))}))))

(defn merge-live-cascade-spec
  "Merge the decision's declared wants with a derived live C spec.

   `:zeroed` is copied unchanged from LIVE-SPEC.  In particular, adding a
   token to `:want` does not remove an outcome containing that token from
   `:zeroed`: the preference law applies the exact-zero exclusion after
   computing utility, so a cross-source want cannot resurrect a zeroed
   outcome."
  [joint-want live-spec]
  {:want (into joint-want (:want live-spec))
   :c-schedule (:c-schedule live-spec)
   :weights (:weights live-spec)
   :lam (:lam live-spec)
   :mu (:mu live-spec)
   :evidence (:evidence live-spec)
   :zeroed (:zeroed live-spec)
   :c {:status :derived
       :source :futon2.aif.live-c/cascade-spec
       :live-c (:live-c live-spec)}})

(def class-preference-weights
  "Joe's fixed terminal class preference (2026-09-22)."
  {:focused 55/100 :related 35/100 :unrelated 5/100 :stop-the-line 5/100})

(defn class-preference-schedule
  "Write-only receipt read from the exact class model passed to the scorer."
  [model]
  {:family :class
   :placement :terminal
   :weights (get-in model [:class-preference (:horizon model)])
   :site "scripts/futon2/report/war_machine.clj:class-preference-weights"})

(defn- class-observation-model
  "PROOF-wm-works 1.3 build 2/3: the class observation model for the joint
   family. Emission is deterministic: before the common horizon every state
   emits :ending/not-yet-evaluated (preference 1, risk exactly 0); AT the
   horizon each state emits its run-ending class -- an acceptance token
   reached on a target whose facet is the current focus is :focused, a
   same-focus-edge facet :related, another facet :unrelated, and a state
   reaching nothing is :stop-the-line (Joe 2026-09-22: an unmeasured or
   unreached outcome belongs to stop-the-line). Class C at the horizon is
   Joe's fixed {focused .55, related .35, unrelated .05, stop-the-line .05};
   the token preference path (preference-member, live-c weights) is NOT
   replaced -- this is an additional observation model the bounded scorer
   consumes, and live-c is still derived and recorded."
  [{:keys [universe acceptance target-class horizon]}]
  (let [progress-classes (into {} (for [n (range (inc (count acceptance)))]
                                    [n (keyword (str "progress-" n))]))
        progress-labels (vec (vals progress-classes))
        terminal-labels [:focused :related :unrelated :stop-the-line]
        class-universe (vec (concat progress-labels terminal-labels))
        ;; Ordinal progress preference: each additional completed item gets
        ;; one additional unit of C mass.  This is the declared completed-
        ;; progress scale, not a waiting placeholder.
        progress-weights (into {} (map (fn [[n label]] [label (inc n)])
                                       progress-classes))
        progress-total (reduce + (vals progress-weights))
        progress-c (update-vals progress-weights #(/ % progress-total))
        joe-c class-preference-weights
        class-pref (into {} (for [tau (range 1 (inc horizon))]
                              [tau (if (= tau horizon) joe-c progress-c)]))
        ;; Jeffreys half-count baseline plus one declared pseudo-observation
        ;; for the state-associated emission.  q7-belief-novelty can replace
        ;; this map with its posterior at this seam; no accumulation occurs
        ;; here.
        dirichlet-prior (into {}
                              (for [label class-universe
                                    :let [domain (if (some #{label} progress-labels)
                                                    progress-labels
                                                    terminal-labels)]]
                                [label (into {}
                                             (for [outcome domain]
                                               [outcome (if (= label outcome) 3/2 1/2)]))]))]
    {:schema :wm/observation-model-v1
     :backend :exact-enumeration
     :kind :class-emission
     :universe universe
     :horizon horizon
     :class-universe class-universe
     :acceptance acceptance
     :target-class target-class
     :progress-classes progress-classes
     :class-preference class-pref
     :dirichlet-prior dirichlet-prior
     :provenance {:status :synthetic :calibrated false
                  :source "WM-Q4: ordinal completed-progress C; Dirichlet(1/2) Jeffreys baseline plus one state-emission pseudo-count; Joe terminal C 55/35/5/5"}}))

(defn- cascade-family-parameters
  "Validate the declared comparison BEFORE admission can remove a target.
   A declined candidate cannot hide incompatible horizons or temperatures."
  [problems]
  (let [Ts (distinct (map #(get-in % [:cascade-problem :horizon-steps]) problems))
        betas (distinct (map #(get-in % [:cascade-problem :beta]) problems))]
    (when (not= 1 (count Ts))
      (throw (ex-info "cascade decision refused"
                      {:kind :incommensurable-family :horizon-steps (vec Ts)})))
    (when (not= 1 (count betas))
      (throw (ex-info "cascade decision refused"
                      {:kind :incommensurable-family :beta (vec betas)})))
    {:horizon-steps (first Ts) :beta (first betas)}))

(defn canonical-pr
  "A deterministic printed serialisation for digesting: map entries sorted
  by printed key, sets sorted by printed element, sequential values in
  order, everything else pr-str. Equal values serialise identically
  regardless of hash order."
  [v]
  (cond
    (map? v) (str "{"
                  (str/join " " (map (fn [[k x]]
                                       (str (canonical-pr k) " " (canonical-pr x)))
                                     (sort-by (comp pr-str key) v)))
                  "}")
    (set? v) (str "#{" (str/join " " (map canonical-pr (sort-by pr-str v))) "}")
    (sequential? v) (str "[" (str/join " " (map canonical-pr v)) "]")
    :else (pr-str v)))

(defn sha256-hex
  "Hex SHA-256 of a string's UTF-8 bytes."
  [^String s]
  (apply str
         (map #(format "%02x" (bit-and 0xff %))
              (.digest (java.security.MessageDigest/getInstance "SHA-256")
                       (.getBytes s "UTF-8")))))

(defn measured-a-version
  "F1a-2-I: the measured-A version for the tick's admitted decision,
  computed the way cascade-lane's R5 step computes its rates —
  observation-rates/sourced-rates → token-likelihood-rates over each
  problem's :locators; the arithmetic is not copied here.

  PROBLEMS is the admitted family (the assembled problems);
  OBSERVATION-LABELS is {target {:labels [...] :subjects {class n} :prior p}}.
  The decision's lanes and this function share one entry snapshot. With no
  usable view, every located token keeps the unmeasured checkable default
  (the zero kernel, :measurement :absent) and this returns the
  typed absence — packet F1a-2-I: never a digest of a default/identity/zero
  kernel; an absence must not read as a value.

  Rate keys are target-qualified ([target token]), the decision's own
  qualification scheme, so identical tokens on different targets stay
  distinct. Returns

    {:schema :wm/measured-a-v1
     :rates {[target token] {:false-neg r :false-pos r}}  ; the value
     :rates-sha <sha256 of canonical-pr of :rates>
     :measurement {[target token] :absent | {:false-neg {:numerator n
                   :denominator d} :false-pos {...}}}   ; per-token provenance
     :source :futon2.aif.observation-rates/sourced-rates   ; verbatim
     :classes [class-id …]}

  or {:status :absent :reason :no-measured-rates} when nothing measured is
  sourced, or {:status :absent :reason :sourcing-refused :refusals …} when
  every located problem's sourcing refused (the refusal is the finding,
  recorded, never thrown from here: this write refuses nothing)."
  [problems observation-labels]
  (let [contract (observation-contract)
        per-target
        (into {}
              (keep (fn [p]
                      (let [t (:target p)
                            locators (get-in p [:cascade-problem :locators])]
                        (when (map? locators)
                          (let [{:keys [labels subjects prior]} (get observation-labels t)]
                            [t (observation-rates/sourced-rates
                                (vec labels) (or subjects {}) prior
                                locators contract)])))))
              problems)
        sourced (into {} (filter (fn [[_ s]] (= :sourced (:status s))) per-target))
        refusals (into {} (remove (fn [[_ s]] (= :sourced (:status s))) per-target))]
    (if (empty? sourced)
      (if (seq refusals)
        {:status :absent :reason :sourcing-refused :refusals refusals}
        {:status :absent :reason :no-measured-rates})
      (let [qualify (fn [t m] (into {} (map (fn [[tok v]] [[t tok] v])) m))
            rates (into {} (mapcat (fn [[t s]] (qualify t (:rates s))) sourced))
            measurement (into {} (mapcat (fn [[t s]] (qualify t (:measurement s))) sourced))
            class-of (into {} (mapcat (fn [[t s]] (qualify t (:class-of s))) sourced))]
        (if-not (some (fn [[_ m]] (not= :absent m)) measurement)
          ;; Every located token is the unmeasured checkable default: the
          ;; zero kernel. Digesting it would make an absence read as a
          ;; value, so the absence is written instead.
          {:status :absent :reason :no-measured-rates}
          ;; F1a-2b: the value travels with its digest, so F's conditioning
          ;; step can be recomputed from the run record (F1c-D §3).
          ;; F1a-2c: and with its per-token measurement, which the step
          ;; needs to refuse a checked token whose class is unmeasured
          ;; (flight/conditioning-step, :unmeasured-class)
          (cond-> {:schema :wm/measured-a-v1
                   :rates rates
                   :rates-sha (sha256-hex (canonical-pr rates))
                   :measurement measurement
                   :source (:source (first (vals sourced)))
                   :classes (vec (sort-by pr-str (distinct (vals class-of))))}
            (seq refusals) (assoc :refusals refusals)))))))

(defn- cascade-decision-admitted
  "Joint cascade decision over ASSEMBLED, the output of
  futon2.aif.cascade-problems/assemble. OPTS is reserved (ignored today),
  except :cascade-sources (B4 2c): the declared sources map, read by the
  candidate-derivations carrier for :source-content-sha256 and :acceptance.

  Returns {:decision … :lanes [… ] :cascade-problems assembled}, where
  :decision has passed futon2.aif.decision-gate/emit! and :lanes records
  every problem's cascade-lane route plus any candidate dropped for
  unmatched receipts (a candidate is NEVER passed through unreceipted).

  - No problems ⇒ the abstention {:status :abstained :refusals …} through
    emit! (an empty refusal list throws there — a tick with no targets and
    no refusals is a configuration error).
  - Targets are kept APART in the joint family: before scoring, every
    token of every problem is qualified by its target (the deterministic
    scheme [target token], recorded on the decision under
    :token-qualification) — facts, want, guard present/absent sets and
    :produces — and every pattern map in a candidate's precedence carries
    its :target, so identical pattern ids on different targets remain
    distinct first acting patterns. The enacted step's marginal is then
    per (target, pattern); one target's fact can never satisfy another
    target's guard or want.
  - Each problem's R1/R6/R13/R4/R5 run inside cascade-lane (no maths is
    copied, no lane output is used for the joint score); G is computed by
    ONE efe/rank-actions call over the union family at the common
    declared T, whose universe is the union of every problem's QUALIFIED
    tokens.
  - ONE policy/select-action-cascades over the union family at the
    declared β; candidates keep :target; selection is never
    per-target-then-ranked.
  - Problems declaring different T or β refuse, typed
    :incommensurable-family with the values."
  [assembled opts]
  (let [problems (:problems assembled)]
    (if (empty? problems)
      {:decision (decision-gate/emit!
                  {:status :abstained :refusals (:refusals assembled)})
       :lanes []
       :cascade-problems assembled}
      (let [{T :horizon-steps beta :beta} (cascade-family-parameters problems)
              ;; WIRE-3: the derived live C enters the joint preference
              ;; spec. Derived from Joe's three named sources at decision
              ;; time; a STALE C (the corpus changed after the derivation)
              ;; or a refused derivation refuses the decision typed —
              ;; never scored against a stale C, never silently uniform.
              ;; opts :live-c {:sources :sources-now :derived} is the test
              ;; injection seam for the source reads and the derived map
              ;; (the derivation itself is covered in
              ;; futon2.aif.live-c-test); production passes nothing and
              ;; reads the real corpus twice (derive, then the freshness
              ;; re-read).
              live-c-opts (:live-c opts)
              live-sources (or (:sources live-c-opts) (live-c/read-sources))
              live-derived (or (:derived live-c-opts)
                               (live-c/derive-live-c live-sources))
              _ (when (seq (:refusals live-derived))
                  (throw (ex-info "cascade decision refused"
                                  {:kind :live-c-refused
                                   :refusals (:refusals live-derived)
                                   :limitation (:limitation live-derived)})))
              ;; Freshness: compare the derived C's signature against a fresh
              ;; source read. Production always derives from a real read, so
              ;; the guard always runs. The :derived test seam bypasses
              ;; DERIVATION, not the guard: a test that injects :derived AND
              ;; a source read gets the real comparison (that is how the
              ;; stale test below refuses); only a bare :derived with no
              ;; source read at all records the seam instead of inventing a
              ;; comparison.
              live-freshness
              (if (and (:derived live-c-opts)
                       (not (or (:sources live-c-opts)
                                (:sources-now live-c-opts))))
                {:stale? false :signature-derived (:signature live-derived)
                 :signature-now :injected-test-seam}
                (live-c/stale?
                 live-derived
                 (or (:sources-now live-c-opts) live-sources)))
              _ (when (:stale? live-freshness)
                  (throw (ex-info "cascade decision refused"
                                  {:kind :live-c-stale
                                   :signature-derived (:signature-derived live-freshness)
                                   :signature-now (:signature-now live-freshness)
                                   :limitation "the corpus changed after C was derived: re-derive before scoring"})))
              preference-scales (live-c/family-scales problems)
              preference-schedule (live-c/family-schedule problems)
              ;; Admission has already paired each order with its own receipt.
              qualification (fn [target token] [target token])
              joint-candidates
              (vec
               (mapcat
                (fn [problem]
                  (let [t (:target problem)
                        cp (:cascade-problem problem)
                        qual (partial qualification t)
                        patterns
                        (into {}
                              (map (fn [[id {:keys [guard produces] :as declared}]]
                                     [id (-> (cascade-policy/token-interpretation
                                              id {:guard {:needs (set (map qual (:needs guard)))
                                                          :forbids (set (map qual (:forbids guard)))}
                                                  :produces (set (map qual produces))})
                                             (assoc :target t)
                                             (merge (select-keys declared
                                                                 [:theta :theta-source
                                                                  :predicted-effect])))]))
                              (:interpretations cp))]
                    (mapv (fn [{:keys [candidate-id precedence construction-receipt]}]
                            {:kind :cascade-candidate :id candidate-id :target t
                             :want (vec (:want cp))
                             :precedence (mapv patterns precedence)
                             :observation-locators
                             (into {} (map (fn [[token locator]] [(qual token) locator]))
                                   (:locators cp))
                             :construction-receipt construction-receipt
                             :interpretation-receipts (:interpretation-receipts problem)})
                          (:constructed-candidates problem))))
                problems))
              dropped (:dropped-candidates assembled)
              ;; Joint belief and want over the target-qualified tokens.
              initial-belief-receipt (input-receipts/initial-belief problems)
              ;; Retain prospective carry without granting it enactment authority.
              token-belief-stage (token-carry/stage
                                  initial-belief-receipt
                                  (token-carry/domain-inputs problems)
                                  (:prospective-token-carry opts)
                                  (let [contexts (into {} (keep (fn [p]
                                                   (when-let [c (get-in p [:cascade-problem :token-initialization])]
                                                     [(:target p) c]))) problems)]
                                    (cond-> (:token-belief-context opts)
                                      (seq contexts) (assoc :observation-initialization contexts))))
              token-belief-input (token-predecessor/input-receipt
                                  token-belief-stage
                                  (token-predecessor/inspect-trace
                                   (:token-belief-predecessor-trace opts) opts))
              recorded-token-belief-stage
              (token-carry/record-observation token-belief-stage token-belief-input)
              joint-q0 (:continuation-belief token-belief-input)
              lanes
              (mapv (fn [problem]
                      (let [lane (cascade-lane (:cascade-problem problem)
                                               (merge (zeta-posterior/lane-options token-belief-input)
                                                      {:observation-labels (observation-label-inputs
                                                                            (:observation-labels-view opts))}))]
                        {:target (:target problem)
                         :route (:route lane)
                         :token-rate-scoring (:cascade-scoring (meta (:ranked lane)))
                         :decision (select-keys (:decision lane) [:preference-schedule])
                         :refusal (when (:stopped-at lane) (:refusal lane))
                         :candidates (filterv #(seq (:precedence %)) (:candidates lane))
                         :null-comparison
                         {:role :per-target-diagnostic-baseline
                          :used-for-joint-selection? false
                          :candidates (filterv #(empty? (:precedence %)) (:candidates lane))}}))
                    problems)
              joint-want (reduce (fn [acc p]
                                   (let [t (:target p)]
                                     (into acc (map (fn [w] [t w]))
                                           (get-in p [:cascade-problem :want]))))
                                 #{} problems)
              ;; WIRE-3: the joint comparison's REACHABLE token domain —
              ;; every token the family's states, patterns or declared
              ;; wants can name — is what live-c/cascade-spec restricts
              ;; the live want to (the producer's own law: an unreachable
              ;; want token dilutes the uniform share and distorts the
              ;; comparison, so it is refused as :no-reachable-want, not
              ;; kept). The spec that reaches scoring is the joint want
              ;; plus the in-domain live want, with the live weights.
              joint-reachable
              (reduce
               (fn [acc candidate]
                 (reduce (fn [a pattern]
                           (reduce conj a
                                   (concat (:produces pattern)
                                           (mapcat (fn [cl]
                                                     (concat (:present cl)
                                                             (:absent cl)))
                                                   (get-in pattern [:guard :clauses])))))
                         acc (:precedence candidate)))
               (reduce clojure.set/union joint-want (map (partial reduce clojure.set/union #{}) (keys joint-q0)))
               joint-candidates)
              live-spec (live-c/cascade-spec live-derived joint-reachable joint-want
                                              preference-scales preference-schedule)
              live-refusal (:refusal live-spec)
              ;; :no-reachable-want is NOT the same class of refusal as
              ;; :live-c-stale or a source failure. Those two mean "C cannot be
              ;; trusted, do not score". This one means "no live-C want token
              ;; lies in this comparison's outcome domain" -- and today that is
              ;; true only when no mission token can project through that
              ;; mission's own declared wants. Capability-grain :star tokens
              ;; intentionally remain outside the target-qualified domain.
              ;;
              ;; live-c is right to refuse when ASKED for a spec it cannot
              ;; honestly give -- that law is not touched here. What the CALLER
              ;; does with the refusal is this decision: a grain mismatch is C
              ;; having no opinion about these outcomes, not C being broken, so
              ;; the comparison proceeds on the uniform spec it used before this
              ;; slice and records that it did. Halting every production
              ;; decision would make this wiring responsible for a gap it only
              ;; revealed. The record is on the certificate, so "C had no
              ;; opinion at this grain" cannot be mistaken for "C was derived
              ;; and agreed".
              grain-mismatch? (= :no-reachable-want (:kind live-refusal))
              _ (when (and live-refusal (not grain-mismatch?))
                  (throw (ex-info "cascade decision refused" live-refusal)))
              ;; PROOF-wm-works 1.3 build 2/3: score the joint family with
              ;; the CLASS observation model (Joe's 55/35/5/5 at the horizon,
              ;; zero risk before it) ALONGSIDE the untouched token
              ;; machinery: the class model is the :observation-model the
              ;; bounded scorer consumes; live-c is still derived, freshness-
              ;; checked and recorded above -- it no longer enters the score.
              ;; PROOF-wm-works 1.3 handoff B(1): evaluate the focus at the
              ;; decision's ACTUAL time. The corpus is retrospective-pinned
              ;; history, so the established focus is first discovered at the
              ;; corpus's latest valid-through and then RETAINED at now by
              ;; focus_receipt's persistence semantics; with no established
              ;; focus the status is :unknown and every target's class is
              ;; recorded :unknown -- never :unrelated, and the decision says
              ;; so under :focus-status rather than refusing to fire.
              ;; injectable like :live-c (tests supply synthetic corpora);
              ;; production reads the canonical resource.
              focus-inputs (or (:focus-inputs opts) (focus-receipt/read-inputs))
              ;; codex-20 correction 3: ONE decision time, captured once and
              ;; used by BOTH consumers (scoring's focus context and the
              ;; receipt attachment). :focus-as-of pins it for replay; no
              ;; consumer computes its own now.
              decision-as-of (or (:focus-as-of opts) (str (java.time.Instant/now)))
              ;; codex-20 correction 2 (revised): FIRST use the discovery
              ;; that is VALID AT the decision time (the window covering it --
              ;; a window's valid-through is its validity endpoint, not the
              ;; moment its evidence becomes available; a decision inside a
              ;; window discovers from that window). Only when NO window
              ;; covers the decision time is a prior focus retained -- from
              ;; the latest window that ENDED before the decision, never
              ;; from evidence dated after it.
              ;; codex-20 corrections 2 and 3 -- the covering window's
              ;; discovery, else the retained prior focus, else :unknown;
              ;; shared with the WM ask's classifier
              {focus-info :info focus-established :established
               established-as-of :established-as-of}
              (focus-receipt/decision-focus focus-inputs decision-as-of)
              class-universe (reduce clojure.set/union
                                     (set joint-reachable)
                                     [(set joint-want)
                                      (set (mapcat identity (keys joint-q0)))
                                      (set (mapcat identity (keys (:value initial-belief-receipt))))])
              ;; THE shared relation producer (codex-20 ruling): the SAME
              ;; classify-target the close receipt uses, same focus context
              ;; (discovered or retained), ticket parents derived through the
              ;; recorded Parent line.
              ;; codex-20 correction 1: anchor the evidence directories to
              ;; the canonical futon2 repository root (the registry's own
              ;; default-code-root convention: <home>/code + the futon2
              ;; checkout the machinery runs from), never the JVM's working
              ;; directory -- the serving JVM runs from futon3c.
              f2-root (str mission-registry/default-code-root "/futon2")
              relation-context {:code-root mission-registry/default-code-root
                                ;; WM-RELATION-I: M- targets' stated Relations are read here
                                :ticket-dir (str f2-root "/holes/tickets")
                                :findings-dir (str f2-root "/data/wm-repair-obligations/findings")}
              target-classifications
              (into {}
                    (for [p problems
                          :let [t (:target p)]]
                      [t (focus-receipt/classify-target
                          focus-inputs focus-info (:as-of focus-info) t relation-context)]))
              ;; relation vocabulary -> scorer classes (the close side maps
              ;; the same keywords through the facet-map)
              scorer-class {:focus :focused :associated :related :useful-elsewhere :unrelated}
              ;; ⟨1⟩8 second half: the scorer CONSUMES the recorded theta.
              ;; Where the model is assembled for scoring (not inside the
              ;; kernel): each candidate's pattern whose family has recorded
              ;; trials takes its theta from the ledger, with provenance on
              ;; the pattern; a family with no trials keeps the documented
              ;; default; an unreadable ledger leaves the default with a
              ;; typed reason. Nothing is a gate. The kernel keeps reading
              ;; theta off the pattern — only the pattern's theta source
              ;; changes.
              ledger-root-for-theta (or (:learning-trial-ledger-root opts)
                                         learning-ledger/default-root)
              theta-consumption
              (into {}
                    (for [c joint-candidates
                          p (:precedence c)
                          :let [family (:id p)]]
                      [family (try
                                (learning-ledger/pattern-theta family ledger-root-for-theta)
                                (catch Exception _e
                                  {:status :defaulted :reason :ledger-read-failed}))] ))
              joint-candidates
              (mapv (fn [c]
                      (update c :precedence
                              (fn [ps]
                                (mapv (fn [p]
                                        (let [ft (get theta-consumption (:id p))]
                                          (cond
                                            (= :retrieval-rank-likelihood (:theta-source p))
                                            p

                                            ;; recorded trials: theta from the ledger
                                            (= :recorded-trials (:status ft))
                                            (assoc p :theta (:theta ft)
                                                     :theta-source :recorded-trials
                                                     ;; :targets and :unattributed-rows are
                                                     ;; provenance the decision record must
                                                     ;; carry, not just the reader's return:
                                                     ;; a 1/8 from three attempts on ONE
                                                     ;; target reads differently from three
                                                     ;; targets, and unattributed rows are
                                                     ;; how a join mismatch becomes visible
                                                     ;; (claude-2's review, 2026-09-23)
                                                     :theta-provenance (select-keys ft [:trials-count :successes :identities
                                                                                        :targets :unattributed-rows]))
                                            ;; unreadable: documented default, typed reason kept
                                            (= :defaulted (:status ft))
                                            (assoc p :theta 1
                                                     :theta-source :documented-default
                                                     :theta-default-reason (:reason ft))
                                            ;; no trials: the kernel's own default, unchanged
                                            :else p)))
                                      ps))))
                    joint-candidates)
              class-model (class-observation-model
                           {:universe class-universe
                            :acceptance joint-want
                            :target-class (into {}
                                                (for [[t c] target-classifications]
                                                  [t (get scorer-class (:class c) :unknown)]))
                            :horizon T})
              rank-opts {:f-prefix-production? true
                                        :scoring-parallelism (:scoring-parallelism opts)
                                        :scoring-cache? true
                                        :scoring-cache-path (:scoring-cache-path opts)
                                        :scoring-cache-prewarm? (:scoring-cache-prewarm? opts)
                                        :horizon-steps T
                                        :observation-model class-model
                                        :upstream-initialization-conditioning
                                        {:observation-updates (:observation-updates token-belief-input)
                                         :conditioning-status (:conditioning-status token-belief-input)
                                         :reason (:reason token-belief-input)
                                         :applied-to (get-in token-belief-stage [:initialization :value])}
                                        :prediction-context {:occurrence-id (str "class-score-" (java.time.Instant/now))
                                                             :tau T}
                                        :cascade-spec
                                        (if (= :class-emission (:kind class-model))
                                          ;; PROOF-wm-works 1.3 build 2/3: with
                                          ;; the class observation model, the
                                          ;; scoring preference is the class C
                                          ;; (on the model, Joe's ruling), and
                                          ;; live-c is RECORD-ONLY -- derived
                                          ;; and freshness-checked above, never
                                          ;; in the score.
                                          {:want joint-want
                                           :evidence #{}
                                           :zeroed #{}
                                           :c {:status :class-observation
                                               :source "PROOF-wm-works 1.3; Joe 2026-09-22 ruling (55/35/5/5; unmeasured -> stop-the-line)"
                                               :live-c-recorded (select-keys live-derived [:signature :sources-read])}}
                                          (if grain-mismatch?
                                          {:want joint-want
                                           :c-schedule preference-schedule
                                           :lam (:lam preference-scales)
                                           :mu (:mu preference-scales)
                                           :preference-scales preference-scales
                                           :c {:status :derived-no-overlap
                                               :source :futon2.aif.live-c/cascade-spec
                                               :reason :no-live-c-mission-want-in-cascade-outcome-domain
                                               :live-want (:live-want live-refusal)
                                               :reachable (:reachable live-refusal)
                                               :unreached-in-domain (:unreached-in-domain live-refusal)}}
                                          (merge-live-cascade-spec
                                           joint-want live-spec)))}
              ;; codex-20 ruling, handoff B (live repair, M-a-wmc-scaling):
              ;; a candidate whose scoring refuses :class-unknown-no-scalar-g
              ;; gets NO scalar G. It is DECLINED -- a typed entry in the
              ;; decision's dropped candidates carrying :possible-costs --
              ;; and selection continues over the candidates that have a
              ;; scalar. Only when NO candidate has a scalar does the
              ;; decision refuse as before. No G is invented for the unknown
              ;; target (no worst case, no average, no default class).
              [ranked class-declines class-unknown-refusals]
              (loop [candidates joint-candidates declines [] refused []]
                (let [r (efe/rank-actions {:cascade-belief joint-q0}
                                          candidates rank-opts)]
                  (if (and (map? r) (contains? r :status)
                           (= :class-unknown-no-scalar-g (:kind r))
                           (some #(= (:target r) (:target %)) candidates))
                    (let [t (:target r)
                          remaining (filterv #(not= t (:target %)) candidates)
                          target-declines (map (fn [c]
                                                 {:target t
                                                  :stage :scoring
                                                  :candidate (:id c)
                                                  :reason :class-unknown-no-scalar-g
                                                  :possible-costs (:possible-costs r)})
                                               (filter #(= t (:target %)) candidates))
                          refusal {:target t
                                   :kind :class-unknown-no-scalar-g
                                   :missing :target-relation
                                   :possible-costs (:possible-costs r)}]
                      (if (empty? remaining)
                        ;; Every scored candidate declined: nil ranked; the
                        ;; body below abstains with all refusals when other
                        ;; targets carry admission refusals, else throws.
                        [nil (into declines target-declines)
                         (conj refused refusal)]
                        (recur remaining
                               (into declines target-declines)
                               (conj refused refusal))))
                    [r declines refused])))
              dropped (vec (concat dropped class-declines))]
          (when (and (map? ranked) (contains? ranked :status))
            (throw (ex-info "cascade decision refused"
                            (merge {:kind (or (:kind ranked) :rank-refused)}
                                   ranked))))
          ;; M-a-wmc-scaling (click 16): when EVERY scored candidate was
          ;; declined :class-unknown-no-scalar-g and other targets carry
          ;; admission refusals, the decision ABSTAINS -- the same typed
          ;; abstention as the no-admitted-problems path, :refusals in their
          ;; existing order plus one :class-unknown-no-scalar-g refusal per
          ;; declined target -- so the runner's
          ;; first-no-admitted-interpretation-refusal ask can fire. When the
          ;; declined family was the whole field the decision still refuses.
          (or (when (nil? ranked)
                (if (seq (:refusals assembled))
                  {:decision (decision-gate/emit!
                              {:status :abstained
                               :refusals (into (vec (:refusals assembled))
                                               class-unknown-refusals)})
                   :lanes (mapv (fn [lane]
                                  (cond-> lane
                                    (seq dropped)
                                    (assoc :dropped-candidates dropped)))
                                lanes)
                   :dropped-candidates dropped
                   :cascade-problems assembled}
                  (throw (ex-info "cascade decision refused"
                                  (merge {:kind :class-unknown-no-scalar-g}
                                         (last class-unknown-refusals))))))
              (let [precision-model (get-in (meta ranked) [:cascade-scoring :precision-model])
                schedules (into {} (map (fn [p] [(:target p) (get-in p [:cascade-problem :observation-schedule])]) problems))
                model-id (precision-carry/model-identity precision-model
                           (mapv (fn [e] {:id (:action e)}) ranked) schedules)
                admission (get-in token-belief-input [:carry-admission :authority])
                beta-state (precision-carry/advance
                            {:previous (get-in opts [:token-belief-predecessor-trace :decision
                                                    :selection-certificate :policy-precision-state])
                             :initialized-beta beta :model-id model-id :admission admission
                             :family (:precision-family admission)})
                ;; F1b-admit-I / F1c-I: each candidate's admitted observed
                ;; prefix from the flights' conditioning steps, recorded per
                ;; candidate under :policy-prefixes BEFORE selection (the
                ;; top-level :observation-updates stay D's initialization
                ;; updates), and production-ranked supplies F from the
                ;; recorded value (a candidate with no admitted steps stays
                ;; :not-supplied). Without the flights' steps in opts every
                ;; candidate is {:conditioning-status :no-flight-records}.
                token-belief-input (assoc token-belief-input :policy-prefixes
                                          (prefix-admission/prefixes
                                           joint-candidates
                                           (or (:conditioning-steps opts)
                                               {:steps [] :dir-status {:absent :no-flight-steps-in-opts}})))
                decision (assoc (binding [input-receipts/*habit-read-purpose* :joint-selection]
                                  (policy/select-action-cascades
                                    (policy-prefix/production-ranked ranked
                                      (select-keys token-belief-input [:conditioning-status :reason :observation-updates])
                                      (:policy-prefixes token-belief-input))
                                    {:beta (:beta beta-state) :beta-state beta-state
                                     :cascade-habit-path (:cascade-habit-path opts)
                                     :pattern-feedback (:pattern-feedback opts)
                                     ;; WM-HABIT-FOLD-CALL-I: judge's fold
                                     :enactment-fold (:enactment-fold opts)
                                     :ticket-queue (:ticket-queue opts)
                                     :ticket-queue-refusals (:ticket-queue-refusals opts)
                                     :novelty-inputs (or (:novelty-inputs opts) (novelty/read-inputs))}))
                                :horizon-steps T
                                :initial-belief-receipt initial-belief-receipt)
                decision (assoc-in decision [:selection-certificate :precision-family]
                                   (precision-carry/family decision precision-model schedules))
                decision (assoc-in decision [:selection-certificate :token-rate-lanes]
                                   (into {} (map (juxt :target :token-rate-scoring)) lanes))
                decision (assoc-in decision [:selection-certificate :token-belief-stage]
                                   recorded-token-belief-stage)
                decision (assoc-in decision [:selection-certificate :token-belief-input]
                                   token-belief-input)
                ;; B4 slice 2b (PROOF-2 P₀ carrier): one derivation entry per
                ;; scored candidate, truthful for today's declared files
                ;; (:construction :kind :hand-admitted from the candidate's
                ;; own receipt, so P₀ fails on provenance, not absence).
                decision (assoc-in decision [:selection-certificate :candidate-derivations]
                                   (candidate-derivations/derivations
                                     (get-in decision [:selection-certificate :candidates])
                                     (candidate-derivations/s0-of token-belief-stage)
                                     {:actions (when-let [a (get-in decision
                                                       [:selection-law :per-policy-argmax :action])]
                                                 [a])
                                      :as-of decision-as-of
                                      :sources (:cascade-sources opts)}))
                ;; CERT-S v1 §0/§5: the certificate declares its schema.
                decision (assoc-in decision [:selection-certificate :certificate-schema]
                                   :wm/proof2-certificate-v1)
                decision (assoc (input-receipts/with-preference-audit decision)
                                :theta-consumption
                                (into {}
                                      (for [[fam ft] theta-consumption]
                                        [fam (if (= :recorded-trials (:status ft))
                                               {:theta (:theta ft) :status :recorded-trials
                                                :trials-count (:trials-count ft)
                                                :identities (:identities ft)}
                                               {:status (:status ft)
                                                :reason (:reason ft)})]))
                                :focus-status
                                {:status (:status focus-info)
                                 :focus (:focus focus-info)
                                 :as-of (:as-of focus-info)
                                 :target-class target-classifications})
                ;; :focus-as-of pins the receipt's clock for replay comparisons.
                ;; the SAME retained-focus context the scorer used reaches
                ;; the receipt attachment (handoff B: one focus context, two
                ;; consumers)
                decision (focus-receipt/attach
                          decision focus-inputs
                          {:as-of decision-as-of
                           :previous-focus (when (:focus focus-established)
                                             {:focus (:focus focus-established)
                                              :as-of established-as-of})
                           :relation-context relation-context
                           :classifications target-classifications})
                authorized (controller-authority/authorize decision ranked)
                emitted (decision-gate/emit! authorized)
                ;; F1a-2-I: the measured-A version, computed the lane's way
                ;; (sourced-rates over each problem's :locators), written
                ;; beside :selection-certificate. Write only: the scoring
                ;; above is unchanged and nothing here is a gate.
                measured-a (measured-a-version problems
                              (zipmap (map :target problems)
                                      (repeat (observation-label-inputs
                                                (:observation-labels-view opts)))))
                adopted-cells (->> lanes
                                   (mapcat #(get-in % [:token-rate-scoring
                                                       :rates-provenance :adoption :cells]))
                                   distinct
                                   (sort-by pr-str)
                                   vec)
                token-a-bmr-score (token-a-bmr-receipt
                                   (:observation-labels-view opts))
                token-a-bmr (cond-> token-a-bmr-score
                              (= :wm/token-a-bmr-v1 (:schema token-a-bmr-score))
                              (assoc :applied (boolean (seq adopted-cells))
                                     :adopted-cells adopted-cells))]
            {:decision (assoc emitted
                              :measured-a measured-a
                              :token-a-bmr token-a-bmr
                              :preference-schedule (class-preference-schedule class-model)
                              :live-c-coverage (:live-c-coverage live-spec)
                              :token-qualification
                              {:scheme :target-token-pair
                               :form "[target token]"
                               :pattern-maps-carry :target})
             :lanes (mapv (fn [lane]
                            (cond-> lane
                              (seq dropped)
                              (assoc :dropped-candidates dropped)))
                          lanes)
             :dropped-candidates dropped
             :cascade-problems assembled}))))))

(defn- candidate-want-progress
  "Use the scorer/constructor's rollout, on this target's fresh true facts.
   The production D initializer currently consumes exactly these facts;
   prospective carry has no consumption authority. Add-only transitions make
   positive terminal probability for an initially absent want a new predicted
   satisfaction. This is a prediction, never an observed discharge."
  [{:keys [facts want interpretations horizon-steps]} precedence]
  (if-not (pos-int? horizon-steps)
    {:reason :candidate-prediction-refused
     :evidence {:horizon horizon-steps
                :refusal {:kind :missing-common-horizon}}}
    (let [initial (set (for [[token value] facts :when (true? value)] token))
          wanted (set want)
          initial-wanted (clojure.set/intersection wanted initial)
          patterns (mapv #(cascade-policy/token-interpretation % (get interpretations %)) precedence)
          terminal (cascade-manifest/rollout
                    (constantly patterns) (cascade-manifest/observed-belief initial) horizon-steps)
          evidence {:horizon horizon-steps :wanted-tokens wanted
                    :initial-facts facts :initial-state initial
                    :initial-wanted-tokens initial-wanted
                    :prediction-source :cascade-model-manifest/rollout
                    :initialization :fresh-target-facts
                    :semantics :positive-terminal-probability-of-initially-absent-want}]
      (if (:status terminal)
        {:reason :candidate-prediction-refused
         :evidence (assoc evidence :refusal terminal)}
        (let [projected (reduce-kv (fn [m state mass]
                                     (if (pos? mass)
                                       (update m (clojure.set/intersection wanted state) (fnil + 0) mass)
                                       m)) {} terminal)
              new-wanted (clojure.set/difference
                          (reduce clojure.set/union #{} (keys projected)) initial-wanted)]
          (when (empty? new-wanted)
            {:reason :no-new-wanted-token
             :evidence (assoc evidence :terminal-wanted-belief projected
                              :new-wanted-tokens new-wanted)}))))))

(defn- witnessed-relation?
  [units patterns positions relation]
  (let [{:keys [from to tokens]} relation
        semantic-tokens (set/intersection
                         (set (get-in patterns [from :produces]))
                         (set (get-in patterns [to :guard :needs])))]
    (and (map? relation)
         (contains? units from)
         (contains? units to)
         (set? tokens)
         (seq tokens)
         (= semantic-tokens tokens)
         (< (positions from) (positions to)))))

(defn- witnessed-path?
  [units patterns positions from to path]
  (and (vector? path)
       (if (= from to)
         (empty? path)
         (and (seq path)
              (= from (:from (first path)))
              (= to (:to (last path)))
              (every? true?
                      (map (fn [left right] (= (:to left) (:from right)))
                           path (rest path)))))
       (every? #(witnessed-relation? units patterns positions %) path)))

(defn- machine-construction-relations-valid?
  "Fail-closed PROOF-2b relation contract for a receipt which claims it was
  machine-constructed. Hand-admitted and fixture receipts do not acquire this
  claim retroactively."
  [precedence patterns receipt]
  (if (not= :machine-constructed (:kind receipt))
    true
    (let [relations (:relations receipt)
          units (set precedence)
          positions (zipmap precedence (range))
          support (get-in relations [:support :relations])
          meet (get-in relations [:meet :relations])
          generative (get-in relations [:precedence :relations])
          meet-valid? (fn [{:keys [pair meet evidence]}]
                        (and (vector? pair) (= 2 (count pair))
                             (every? units pair) (contains? units meet)
                             (map? evidence)
                             (witnessed-path? units patterns positions
                                              (first pair) meet (:left-path evidence))
                             (witnessed-path? units patterns positions
                                              (second pair) meet (:right-path evidence))))]
      (and (= :computed (:status relations))
           (= :produced-token-consumed-by-guard (get-in relations [:support :basis]))
           (vector? support)
           (every? #(witnessed-relation? units patterns positions %) support)
           (= :greatest-common-descendant (get-in relations [:meet :basis]))
           (vector? meet) (every? meet-valid? meet)
           (vector? (get-in relations [:meet :missing]))
           (= :generative-support (get-in relations [:precedence :basis]))
           (vector? generative) (= support generative)
           (= precedence (get-in relations [:precedence :linear-extension]))
           (vector? (get-in relations [:precedence :violations]))
           (empty? (get-in relations [:precedence :violations]))))))

(defn- admit-cascade-problem
  "Check every executable order before lane construction or scoring. Keep each
  rejection with its target and missing evidence; never replace it with []."
  [problem]
  (let [target (:target problem)
        pairs (:constructed-candidates problem)
        patterns (get-in problem [:cascade-problem :interpretations])
        receipts (:interpretation-receipts problem)
        checked
        (mapv (fn [{:keys [candidate-id precedence construction-receipt] :as pair}]
                (let [provisional? (= :query-time-pattern-selection
                                      (:kind construction-receipt))
                      missing (cond-> []
                                (not (and (vector? precedence) (seq precedence)))
                                (conj :nonempty-precedence)
                                (nil? construction-receipt) (conj :construction-receipt)
                                (and (not provisional?) (map? construction-receipt)
                                     (not (machine-construction-relations-valid?
                                           precedence patterns construction-receipt)))
                                (conj :construction-relations)
                                (some #(not (map? (get patterns %))) precedence)
                                (conj :pattern-interpretation)
                                (and (not provisional?)
                                     (or (not (seq receipts))
                                         (some #(not (and (map? (get receipts %))
                                                         (seq (get receipts %))))
                                               precedence)))
                                (conj :interpretation-receipt))]
                  (if (seq missing)
                    {:decline {:target target :stage :candidate-admission
                               :candidate candidate-id
                               :reason (cond
                                         (some #{:nonempty-precedence} missing) :empty-cascade
                                         (some #{:construction-receipt} missing) :construction-receipt-unmatched
                                         (some #{:construction-relations} missing) :machine-construction-relations-invalid
                                         :else :interpretation-receipts-missing)
                               :missing-evidence missing}}
                    (if-let [no-progress (when-not provisional?
                                          (candidate-want-progress (:cascade-problem problem) precedence))]
                      {:decline (merge {:target target :stage :candidate-admission
                                        :candidate candidate-id
                                        :missing-evidence [(if (= :no-new-wanted-token (:reason no-progress))
                                                             :new-wanted-token-within-horizon
                                                             :prediction-within-declared-horizon)]}
                                       no-progress)}
                      {:candidate pair}))))
              pairs)
        admitted (vec (keep :candidate checked))
        declines (vec (keep :decline checked))
        target-decline (when (empty? admitted)
                         {:target target :stage :target-admission
                          :reason :no-admissible-candidate
                          :missing-evidence (if (seq declines)
                                              (vec (distinct (mapcat :missing-evidence declines)))
                                              [:constructed-candidate])})]
    {:problem (when (seq admitted)
                (-> problem
                    (assoc :constructed-candidates admitted)
                    (assoc-in [:cascade-problem :precedences] (mapv :precedence admitted))))
     :declines (cond-> declines target-decline (conj target-decline))
     :refusal (when target-decline
                {:target target :kind :no-constructed-candidate
                 :missing (:missing-evidence target-decline)})}))

(defn target-construction-census
  "Truthful per-target construction facts from admitted PROBLEMS and the
  certificate CANDIDATES.  A missing query-time carrier makes the census
  typed-absent; partial pins make whole-library false."
  ([problems certificate-candidates]
   (target-construction-census problems certificate-candidates nil))
  ([problems certificate-candidates library-pin]
  (let [policy-counts (frequencies (keep (fn [p]
                                           (or (:target p)
                                               (get-in p [:id :target])))
                                         certificate-candidates))
        rows
        (mapv (fn [{:keys [target query-time-slice cascade-problem]}]
                (let [slice (or query-time-slice (:query-time-slice cascade-problem))
                      pins (:library-pins slice)]
                  {:target target
                   :slice (mapv :pattern (:candidates slice))
                   ;; The admitted operators, rather than every unjudged hit,
                   ;; are the constructor's actual permission boundary.
                   :pool (vec (keys (:interpretations cascade-problem)))
                   :slice-from-whole-library
                   (boolean
                    (or (and (:slice-from-whole-library slice)
                             (= :wm/pinned-pattern-library-v1 (:schema library-pin))
                             (= (:library-size slice) (:size library-pin))
                             (= (:library-manifest-digest slice) (:digest library-pin)))
                        (and (= :wm/query-time-library-slice-v1 (:schema slice))
                             (pos-int? (:library-size slice))
                             (= (:library-size slice) (count pins))
                             (every? #(and (:id %) (:sha256 %) (:revision %)) pins))))
                   :library-size (:library-size slice)
                   :policy-count (get policy-counts target 0)}))
              problems)]
    (if (and (seq problems)
             (every? #(= :wm/query-time-library-slice-v1
                         (get-in % [:query-time-slice :schema]))
                     problems))
      rows
      {:status :absent :reason :query-time-construction-slice-not-recorded
       :targets (mapv :target
                      (remove #(= :wm/query-time-library-slice-v1
                                  (get-in % [:query-time-slice :schema]))
                              problems))}))))

(defn cascade-decision
  "Admit explicitly paired nonempty constructions, record every decline, then
  score/select only admitted candidates. An all-declined family abstains."
  [assembled opts]
  (let [view (observation-label-view opts)
        opts (assoc opts :observation-labels-view view)
        _ (when (seq (:problems assembled))
            (cascade-family-parameters (:problems assembled)))
        admissions (mapv admit-cascade-problem (:problems assembled))
        dropped (vec (concat (:dropped-candidates assembled)
                             (map (fn [r] {:target (:target r) :stage :assembly
                                           :reason (:kind r) :missing-evidence [(:missing r)]})
                                  (:refusals assembled))
                             (mapcat :declines admissions)))
        all-admitted-problems (vec (keep :problem admissions))
        scoring-budget (:scoring-target-budget assembled)
        ;; Every admitted target is scored.  Target order is not a resource
        ;; policy: independent families are evaluated concurrently by the
        ;; bounded observation scorer and final ranking is resolution-aware.
        scored-problems all-admitted-problems
        budget-exhausted []
        budget-drops (mapv (fn [p]
                             {:target (:target p) :stage :scoring
                              :reason :budget-exhausted
                              :missing-evidence []})
                           budget-exhausted)
        dropped (into dropped budget-drops)
        admitted (assoc assembled
                        :problems scored-problems
                        :refusals (into (vec (:refusals assembled)) (keep :refusal admissions))
                        :dropped-candidates dropped)
        queue (ticket-queue/validate! (if (contains? opts :ticket-queue)
                                       (:ticket-queue opts) (ticket-queue/read-declaration)))
        result (cascade-decision-admitted admitted
                 (assoc opts :ticket-queue queue
                        :ticket-queue-refusals (vec (concat (:refusals admitted) dropped))))
        ;; An all-refused family still retains the queue's inadmissible entries.
        result (if (and (empty? (:problems admitted)) (seq (:entries queue)))
                 (assoc-in result [:decision :selection-certificate :ticket-queue]
                           (assoc (ticket-queue/plan queue [] (:refusals admitted))
                                  :unrestricted-choice nil :choice nil :stratum-posterior nil
                                  :decided-by :abstention))
                 result)
        previous-beta (get-in opts [:token-belief-predecessor-trace :decision
                                    :selection-certificate :policy-precision-state])
        result (if (and (empty? (:problems admitted)) previous-beta)
                 (assoc-in result [:decision :selection-certificate :policy-precision-state]
                           (precision-carry/advance {:previous previous-beta
                             :initialized-beta (:initialized-beta previous-beta)
                             :model-id (:model-id previous-beta)}))
                 result)]
    (let [construction-census
          (target-construction-census
           (:target-construction-inputs assembled)
           (get-in result [:decision :selection-certificate :candidates])
           (:library-pin assembled))
          interpretations-owed
          (vec (for [{:keys [target constructed-candidates]} (:problems assembled)
                     {:keys [precedence construction-receipt]} constructed-candidates
                     :when (= :query-time-pattern-selection (:kind construction-receipt))
                     pattern precedence]
                 {:kind :interpretation-owed-after-selection
                  :target target :pattern pattern :attested? false}))]
      (cond-> (-> result
                ;; cascade-decision-admitted's own :dropped-candidates (when
                ;; it ran a scored family) already carries this wrapper's
                ;; assembly/admission declines PLUS any scoring-stage
                ;; declines (e.g. :class-unknown-no-scalar-g); the
                ;; early-return abstention path carries none.
                (assoc :dropped-candidates (vec (or (:dropped-candidates result) dropped)))
                (assoc-in [:decision :selection-certificate :observation-labels]
                          (observation-label-certificate view opts))
                (assoc-in [:decision :mission-hole-coverage]
                          (or (:mission-hole-coverage assembled)
                              {:status :absent :reason :source-coverage-not-supplied}))
                (update :decision #(assoc % :live-c-coverage
                                           (or (:live-c-coverage %)
                                               {:status :absent :reason :no-admitted-cascade-problems}))))
      true
      (assoc-in [:decision :selection-certificate :target-construction]
                construction-census)
      true
      (assoc-in [:decision :selection-certificate :library-pin]
                (:library-pin assembled))
      true
      (assoc-in [:decision :selection-certificate :slice-budget]
                (:slice-budget assembled))
      true
      (assoc-in [:decision :selection-certificate :scoring-target-budget]
                (when scoring-budget
                  (assoc scoring-budget
                         :scored-target-count (count scored-problems)
                         :budget-exhausted-targets (mapv :target budget-exhausted))))
      true
      (assoc-in [:decision :selection-certificate :retrieval-refusals]
                (:retrieval-refusals assembled))
      true
      (assoc-in [:decision :selection-certificate :retrieval-timing]
                (:retrieval-timing assembled))
      (seq interpretations-owed)
      (assoc-in [:decision :selection-certificate :interpretations-owed]
                interpretations-owed)
      (:proposal-supply assembled)
      (assoc-in [:decision :selection-certificate :proposal-supply] (:proposal-supply assembled))
      (empty? (:problems admitted))
        (assoc-in [:decision :reason] :no-acting-cascade-candidate)))))

(defn select-and-record-cascade!
  "Select using the existing habit snapshot, without reinforcing the selection.
   The trace retains the decision; the run record labels its selection event
   separately from the outcome-based reinforcement performed at close."
  [assembled opts]
  (cascade-decision assembled opts))
