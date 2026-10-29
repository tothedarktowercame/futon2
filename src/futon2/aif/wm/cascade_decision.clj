(ns futon2.aif.wm.cascade-decision
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.application-order-certificate :as application-order]
            [futon2.aif.cascade-model-manifest :as cascade-manifest]
            [futon2.aif.cascade-policy :as cascade-policy]
            [futon2.aif.cascade-problems :as cascade-problems]
            [futon2.aif.controller-authority :as controller-authority]
            [futon2.aif.decision-gate :as decision-gate]
            [futon2.aif.efe :as efe]
            [futon2.aif.interpretation-evidence :as interpretation-evidence]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.observation-label-reader :as label-reader]
            [futon2.aif.observation-labels :as observation-labels]
            [futon2.aif.observation-rates :as observation-rates]
            [futon2.aif.parameter-novelty :as novelty]
            [futon2.aif.policy-satisfaction :as policy-satisfaction]
            [futon2.aif.previous-run :as previous-run]
            [futon2.aif.q2-construction :as q2-construction]
            [futon2.aif.q4-preference :as q4-preference]
            [futon2.aif.selected-reading :as selected-reading]
            [futon2.aif.policy :as policy]
            [futon2.aif.policy-depth :as policy-depth]
            [futon2.aif.policy-prefix-admission :as prefix-admission]
            [futon2.aif.policy-prefix-evidence :as policy-prefix]
            [futon2.aif.prefix-free-energy-sut :as prefix-f-sut]
            [futon2.aif.receipt-construction :as receipt-construction]
            [futon2.aif.ticket-queue :as ticket-queue]
            [futon2.aif.three-halves-square :as three-halves-square]
            [futon2.aif.three-halves-policy :as three-halves-policy]
            [futon2.aif.target-reading-registry :as reading-registry]
            [futon2.aif.wm.family-selection :as family-selection]
            [futon2.aif.wm.selection-cascade-identity :as selection-cascade-identity]
            [futon2.aif.wm.executable-cascade :as executable-cascade]
            [futon2.aif.wm.cascade-enactment-compiler :as cascade-enactment-compiler]
            [futon2.aif.wm.cascade-capability-dispatch :as capability-dispatch]
            [futon2.aif.wm.execution-journal :as execution-journal]
            [futon2.aif.reading-policy-precision :as precision]
            [futon2.aif.reading-policy-preference :as pref]
            [futon2.aif.token-a-bmr :as token-a-bmr])
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

(defn- prefix-f-posterior-input
  "Prepare the exact ranked population consumed by the posterior selector.

  With no explicit commissioning this is the historical empirical-only path.
  A commissioning is refusal-by-default and must pass the controlled Prefix-F
  validator before any bootstrap value can enter selection."
  [ranked candidates token-belief-input opts]
  (let [conditioning-steps (or (:conditioning-steps opts)
                               {:steps []
                                :dir-status {:absent :no-flight-steps-in-opts}})
        ;; the same flight ledger, pinned: a policy it proves has never run
        ;; gets the certified zero-history carrier, bound to this pin
        census (prefix-admission/history-census conditioning-steps)
        pin (prefix-admission/census-pin census)
        empirical (policy-prefix/production-ranked
                   ranked
                   (select-keys token-belief-input
                                [:conditioning-status :reason :observation-updates])
                   (:policy-prefixes token-belief-input)
                   census)]
    (if-let [{:keys [bootstrap-priors scorer-authority context] :as commissioning}
             (:prefix-f-commissioning opts)]
      (let [result (prefix-f-sut/compose-menu!
                    {:candidates candidates
                     :ranked ranked
                     :conditioning-steps conditioning-steps
                     :bootstrap-priors bootstrap-priors
                     :scorer-authority scorer-authority}
                    context)]
        (if (= :ready-for-posterior (:status result))
          {:ranked (:posterior-input result)
           :receipt result
           :policy-history-pin pin}
          (throw (ex-info "commissioned Prefix-F preparation refused"
                          {:kind :prefix-f-commissioning-refused
                           :reason (:reason result)
                           :details (:details result)
                           :commissioning-keys (set (keys commissioning))}))))
      {:ranked empirical :policy-history-pin pin})))

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
            (let [ranked0 (get @state :R5)
                  ranked (with-meta
                           (mapv #(update % :action assoc :target (:target problem)) ranked0)
                           (meta ranked0))
                  ;; Commissioning's population correspondence is ordered at
                  ;; the posterior boundary, so align candidates with the
                  ;; ranked records rather than R6's construction order.
                  candidates (mapv :action ranked)
                  prefixes (prefix-admission/prefixes
                            candidates
                            (or (:conditioning-steps lane-opts)
                                {:steps [] :dir-status {:absent :no-flight-steps-in-opts}}))
                  prefix-f-input (prefix-f-posterior-input
                                  ranked candidates {:policy-prefixes prefixes} lane-opts)
                  decision (policy/select-action-cascades
                            (:ranked prefix-f-input)
                            {:beta beta
                             :policy-history-pin (:policy-history-pin prefix-f-input)})
                  decision (cond-> decision
                             (:receipt prefix-f-input)
                             (assoc-in [:selection-certificate :prefix-f-routing]
                                       (:receipt prefix-f-input)))
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
                                                              ranked)})))
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
      (let [s @state]
        ;; A later typed refusal must not erase evidence produced by earlier
        ;; completed nodes.  In particular, diagnostic target lanes compute
        ;; the observed-rate provenance at R5 even when R14 truthfully refuses
        ;; selection for lack of an admitted Prefix-F value.
        (cond-> {:route @route
                 :refusal (:refusal @stopped)
                 :stopped-at (:node @stopped)}
          (:R4 s) (assoc :candidates (:candidates (:R4 s))
                         :predictions (:predictions (:R4 s)))
          (:R5 s) (assoc :ranked (:R5 s))))
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
                                              meet (first pair) (:left-path evidence))
                             (witnessed-path? units patterns positions
                                              meet (second pair) (:right-path evidence))))]
      (and (= :computed (:status relations))
           (= :produced-token-consumed-by-guard (get-in relations [:support :basis]))
           (vector? support)
           (every? #(witnessed-relation? units patterns positions %) support)
           (= :closest-common-origin (get-in relations [:meet :basis]))
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
                (let [square-findings
                      (into {}
                            (map (fn [id]
                                   [id (three-halves-square/validate
                                        (or (get-in pair [:interpretation-receipts id :blend-square])
                                            (get-in receipts [id :blend-square])
                                            (get-in construction-receipt [:blend-squares id])))]))
                            precedence)
                      missing (cond-> []
                                (not (and (vector? precedence) (seq precedence)))
                                (conj :nonempty-precedence)
                                (nil? construction-receipt) (conj :construction-receipt)
                                (and (map? construction-receipt)
                                     (not (machine-construction-relations-valid?
                                           precedence patterns construction-receipt)))
                                (conj :construction-relations)
                                (some #(not (map? (get patterns %))) precedence)
                                (conj :pattern-interpretation)
                                (some #(not= :valid (:status (get square-findings %))) precedence)
                                (conj :three-halves-square)
                                (or (not (seq receipts))
                                    (some #(not (and (map? (get receipts %)) (seq (get receipts %)))) precedence))
                                (conj :interpretation-receipt))]
                  (if (seq missing)
                    {:decline (cond-> {:target target :stage :candidate-admission
                               :candidate candidate-id
                               :reason (cond
                                         (some #{:nonempty-precedence} missing) :empty-cascade
                                         (some #{:construction-receipt} missing) :construction-receipt-unmatched
                                         (some #{:construction-relations} missing) :machine-construction-relations-invalid
                                         (some #{:three-halves-square} missing) :three-halves-square-invalid
                                         :else :interpretation-receipts-missing)
                               :missing-evidence missing}
                                (some #{:three-halves-square} missing)
                                (assoc :evidence
                                       {:three-halves-square
                                        (into {}
                                              (filter (fn [[_ result]]
                                                        (not= :valid (:status result))))
                                              square-findings)}))}
                    (if-let [no-progress (candidate-want-progress (:cascade-problem problem) precedence)]
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

(defn controlled-three-halves-admission
  "Controlled-SUT pre-G composition gate. The production pipeline does not
  call this entrypoint: without an explicit controlled mode and pinned policy
  it refuses instead of falling back to the historical one-square gate."
  [{:keys [mode policy captured-sources]} problem]
  (cond
    (not= :controlled-sut mode)
    {:status :refused :stage :pre-g-composition :reason :controlled-authority-absent}

    (nil? policy)
    {:status :refused :stage :pre-g-composition :reason :three-halves-policy-absent}

    :else
    (let [certificate (three-halves-policy/validate
                       policy {:captured-sources captured-sources})]
      (if (not= :valid (:status certificate))
        {:status :refused :stage :pre-g-composition
         :reason :three-halves-policy-invalid :certificate certificate}
        (let [candidate-view (fn [p]
                               {:target (:target p)
                                :candidates
                                (mapv #(select-keys % [:candidate-id :precedence])
                                      (:constructed-candidates p))})
              projected (update (:projection policy) :candidates
                                #(mapv (fn [c] (select-keys c [:candidate-id :precedence])) %))
              square-receipts-join?
              (every?
               (fn [{:keys [receipt occurrence]}]
                 (let [{:keys [candidate-id pattern-id]} occurrence
                       candidate (some #(when (= candidate-id (:candidate-id %)) %)
                                       (:constructed-candidates problem))]
                   (= receipt
                      (or (get-in candidate [:interpretation-receipts pattern-id :blend-square])
                          (get-in problem [:interpretation-receipts pattern-id :blend-square])
                          (get-in candidate [:construction-receipt :blend-squares pattern-id])))))
               (:squares policy))]
          (if (or (not= projected (candidate-view problem))
                  (not square-receipts-join?))
            {:status :refused :stage :pre-g-composition
             :reason :three-halves-policy-projection-mismatch
             :certificate certificate}
            (let [admission (admit-cascade-problem problem)]
              (if-let [admitted (:problem admission)]
                (if (= projected (candidate-view admitted))
            {:status :ready-for-g :stage :pre-g-composition
             :constructed-identity (candidate-view problem)
             :admitted-identity (candidate-view admitted)
             :certificate certificate :admission admission}
                  {:status :refused :stage :pre-g-composition
                   :reason :admitted-policy-population-mismatch
                   :certificate certificate :admission admission})
            {:status :refused :stage :pre-g-composition
             :reason :cascade-admission-refused
             :certificate certificate :admission admission}))))))))

(defn controlled-application-order-run
  "Controlled-SUT application-order gate and real R1..R16 consumer. The live
  pipeline does not call this entrypoint and no production law is defaulted."
  [{:keys [application-certificate application-registry captured-sources]
    :as controlled} problem]
  (let [composition (controlled-three-halves-admission controlled problem)]
    (if (not= :ready-for-g (:status composition))
      composition
      (let [policy (:policy controlled)
            order (application-order/validate
                   application-certificate
                   {:policy policy :policy-certificate (:certificate composition)
                    :registry application-registry :captured-sources captured-sources})]
        (if (not= :valid (:status order))
          {:status :refused :stage :pre-rollout-application-order
           :reason :application-order-invalid :certificate order
           :composition composition}
          (let [pattern-sequence (get-in order [:computed :pattern-sequence])
                attempted-mismatch
                (cond
                  (and (contains? controlled :scoring-order)
                       (not= pattern-sequence (:scoring-order controlled)))
                  :scoring-order-mismatch
                  (and (contains? controlled :enactment-order)
                       (not= pattern-sequence (:enactment-order controlled)))
                  :dispatch-order-mismatch)]
            (if attempted-mismatch
              {:status :refused :stage :pre-rollout-application-order
               :reason attempted-mismatch :application-order order}
          (let [admitted (get-in composition [:admission :problem])
                lane-problem (assoc (:cascade-problem admitted)
                                    :precedences [pattern-sequence])
                lane (cascade-lane lane-problem {:through :R5})
                chosen-action (some #(when (= pattern-sequence (mapv :id (:precedence %))) %)
                                    (:candidates lane))
                chosen (mapv :id (:precedence chosen-action))
                digest (interpretation-evidence/value-digest pattern-sequence)
                scoring-consumed? (some #(= pattern-sequence (mapv :id (:precedence %)))
                                        (:candidates lane))
                dispatch-consumed? (= pattern-sequence chosen)
                sk-interpretations
                (into {} (map (fn [[id {:keys [guard produces]}]]
                                [id {:guard (into [:and]
                                                 (concat (map (fn [t] [:fact t]) (:needs guard))
                                                         (map (fn [t] [:not [:fact t]])
                                                              (:forbids guard))))
                                     :effect (into {} (map (fn [t] [t true]) produces))}]))
                      (:interpretations lane-problem))
                all-tokens (cascade-problems/problem-tokens
                            (:facts lane-problem) (:want lane-problem)
                            (:interpretations lane-problem))
                facts-closed (into {} (map (fn [t] [t (true? (get (:facts lane-problem) t))]))
                                   all-tokens)
                acting-order (when dispatch-consumed?
                               (receipt-construction/acting-order
                                sk-interpretations facts-closed chosen))]
            (if (and (nil? (:refusal lane)) scoring-consumed? dispatch-consumed?)
              {:status :prepared-for-enactment
               :composition composition :application-order order
               :lane (assoc lane :selected-action chosen-action
                                 :enactment-plan {:acting-order acting-order})
               :scoring {:consumed-order pattern-sequence :order-digest digest}
               :dispatch {:consumed-order chosen :order-digest
                          (interpretation-evidence/value-digest chosen)
                          :acting-order acting-order}}
              {:status :refused :stage :application-order-consumption
               :reason (cond (:refusal lane) :rollout-refused
                             (not scoring-consumed?) :scoring-order-mismatch
                             :else :dispatch-order-mismatch)
               :application-order order :lane lane})))))))))

(defn target-construction-census
  "Record per-target construction facts from assembled problems and the actual
  scored certificate population. Missing slice provenance is typed absent;
  incomplete library pins never warrant a whole-library claim."
  [problems certificate-candidates]
  (let [policy-counts
        (frequencies
         (keep (fn [candidate]
                 (or (:target candidate) (get-in candidate [:id :target])))
               certificate-candidates))
        slices-for (fn [{:keys [query-time-slice query-time-construction-slices]}]
                     (let [slices (or (seq query-time-construction-slices)
                                      (when query-time-slice [query-time-slice]))]
                       (when (and (seq slices)
                                  (every? #(= :wm/query-time-library-slice-v1 (:schema %))
                                          slices))
                         (vec slices))))
        coherent-library? (fn [slices]
                            (= 1 (count (set (map (fn [slice]
                                                   [(:library-size slice)
                                                    (set (map #(select-keys % [:id :sha256 :revision])
                                                              (:library-pins slice)))])
                                                 slices)))))
        missing (vec (keep (fn [{:keys [target] :as problem}]
                             (let [slices (slices-for problem)]
                               (when-not (and slices (coherent-library? slices)) target)))
                           problems))]
    (if (and (seq problems) (empty? missing))
      (mapv
       (fn [{:keys [target cascade-problem] :as problem}]
         (let [slices (slices-for problem)
               query-time-slice (first slices)
               _pins (:library-pins query-time-slice)]
           {:target target
            :slice (vec (distinct (mapcat #(map :pattern (:candidates %)) slices)))
            :construction-slice-count (count slices)
            ;; These are the operators actually admitted to construction. A
            ;; mismatch with the retrieval slice is evidence, not something
            ;; to conceal by copying the slice into this field.
            :pool (vec (keys (:interpretations cascade-problem)))
            :slice-from-whole-library
            (every? (fn [slice]
                      (let [slice-pins (:library-pins slice)]
                        (and (pos-int? (:library-size slice))
                             (= (:library-size slice) (count slice-pins))
                             (every? #(and (:id %) (:sha256 %) (:revision %)) slice-pins))))
                    slices)
            :library-size (:library-size query-time-slice)
            :policy-count (get policy-counts target 0)}))
       problems)
      {:status :absent
       :reason :query-time-construction-slice-not-recorded
       :targets (if (seq problems) missing [])})))

(defn- target-policy-field
  "NOTE-wiring-reading-to-decision §7.5 step 2: every enumerated target's
  policy family, from the one source declaration the assembly carries, with
  the pinned graph loaded once for the whole field. Not scored and not
  selected here; a target without a family keeps its typed failure.
  Without a source declaration the field itself is a typed absence."
  [assembled opts]
  (if-not (contains? assembled :target-sources)
    {:status :absent :reason :target-sources-not-supplied}
    (let [reading-root (or (:target-reading-root opts) reading-registry/default-root)
          target (get-in opts [:task :id])
          declared (filterv #(= target (:target-id %)) (:target-sources assembled))
          ;; every target the assembly accounted for lands in the field: one
          ;; with no source row is a counted failure, never silently absent
          rowless (remove (set (map :target-id declared))
                          ;; a field-level refusal (the outer selector's) names
                          ;; no target; it is not a target without a row
                          (distinct (filter #(= target %)
                                            (map :target (concat (:problems assembled)
                                                                 (:refusals assembled))))))
          rows (into (vec declared)
                     (map (fn [t] {:target-id t :source-path nil
                                   :source-absent :target-source-row-absent}))
                     rowless)
          field0 (family-selection/full-eligible-target-field
                 [] (assoc opts
                           :target-source-declarations rows
                           :target-reading-root reading-root))
          mission-shas (into {}
                             (keep (fn [{:keys [target-id source-path]}]
                                     (when (and source-path (.isFile (io/file source-path)))
                                       [target-id (interpretation-evidence/sha256
                                                   (java.nio.file.Files/readAllBytes
                                                    (.toPath (io/file source-path))))])))
                             rows)
          field (policy-satisfaction/exclude-satisfied
                 field0 (:policy-satisfaction-root opts) mission-shas)]
      (assoc field
             :status :formed
             :reading-root reading-root
             :graph-path (some-> (:pattern-graph-path opts) str)))))

(def ^:private retired-diagnostics
  "The interpreted-construction route's per-decision products. The reading
  policy route neither computes nor consumes them (NOTE-wiring-reading-to-
  decision §7.1); they are recorded as unavailable, never as inputs."
  (into {} (map (fn [k] [k {:status :unavailable :reason :retired-with-interpreted-route}]))
        [:token-rate-lanes :token-belief-stage :token-belief-input :joint-q0
         :class-observation-model :theta-consumption :candidate-derivations
         :measured-a :token-a-bmr :live-c-scored :retry-declines]))

(defn- family-refusals
  "Retain each rejected policy, even when pre-join summaries still list it."
  [rows]
  (vec
   (mapcat
    (fn [{:keys [target-id failures policies]}]
      (let [policy-failures (filterv :policy-id failures)]
        (cond
          (seq policy-failures)
          (mapv (fn [failure]
                  {:target target-id :stage (:stage failure :policy-family)
                   :kind :no-policy-family :policy-id (:policy-id failure)
                   :failure-kind (:kind failure)
                   :reason (or (:clause failure) (:reason failure) (:kind failure))
                   :failures [failure]}) policy-failures)
          (empty? policies)
          [{:target target-id :stage :policy-family
            :kind (if (seq failures) :no-policy-family :no-candidates)
            :failure-kind (or (:kind (first failures)) :no-candidates)
            :failures (vec failures)}]
          :else []))) rows)))

(def ^:private progress-authority
  {:authority :cascade-shape-g/arranged-progress-model
   :version "cascade_shape_g arrangement model v1"})

(declare occurrence-readings)

(def ^:private preference-authority
  {:authority :cascade-shape-g/progress-preference
   :version "cascade_shape_g progress-preference v1"})

(def ^:private beta-authority
  {:status :declared :version "cascade-problems beta-by-context v1"})

(defn- precision-config
  "The reading-policy-precision identity inputs, read from what was actually
  scored: the scorer version every record carries, the kernel every ranked
  entry carries, the graph pin, each computed family's reading digests, the
  ranked policies' structural arrangements, the progress tokens and horizons
  of the scorer's own models, and the declared beta authority."
  [field families ranked scored]
  (let [scorer-versions (set (map (comp :scorer-version second) scored))
        kernels (set (map :transition-kernel ranked))
        canonical? (every? #(= :canonical-coapply (:g-law (second %))) scored)
        models (map #(get-in (meta (:scorer-result (second %))) [:cascade-scoring :model])
                    scored)]
    (cond
      (not= 1 (count scorer-versions))
      {:refusal {:kind :scorer-version-not-unique :versions (vec scorer-versions)}}
      (not= 1 (count kernels))
      {:refusal {:kind :kernel-version-not-unique :kernels (vec kernels)}}
      :else
      {:config
       {:scorer-version (first scorer-versions)
        :kernel-version (first kernels)
        :graph-digest (get-in field [:graph :pin :sha256])
        :readings (into {} (for [f families :when (= :computed (:status f))]
                             [(:target-id f) (select-keys (:provenance f)
                                                          [:analysis-digest :excerpt-digest])]))
        :policies (mapv (fn [e] {:policy-id (:policy-id e) :kind (:policy-kind e)
                                 :target-id (:target-id e)
                                 :transition-kernel (:transition-kernel e)
                                 :arrangement (get-in e [:action :arrangement])})
                        ranked)
        :progress-model (assoc (if canonical?
                                  {:authority :cascade-shape-g/class-emission-model
                                   :version "canonical co-application class model v1"}
                                  progress-authority)
                               :progress-tokens (sort-by pr-str (distinct (mapcat (if canonical? :universe :progress-tokens) models))))
        :preference (assoc (if canonical?
                              {:authority :classKernel :version "compareAtHorizonN classKernel v1"}
                              preference-authority)
                           :horizon (reduce max 1 (keep :horizon models)))
        :beta-authority beta-authority}})))

(defn- previous-precision
  "The predecessor's precision receipt: its reading-precision carry, else the
  old q0/class precision state (which the module refuses, never resets)."
  [opts]
  (let [cert (get-in opts [:token-belief-predecessor-trace :decision :selection-certificate])]
    (or (:reading-precision-carry cert) (:policy-precision-state cert))))

(defn- certify-reading-family
  "Before the one selector call: certify every scored record's one C
  (reading-policy-preference) and decide beta continuity over the
  reading-precision identity of what was scored (reading-policy-precision).
  Either refuses typed, and the selector is not called."
  [field families declared opts]
  (fn [ranked scored]
    (let [preference (mapv (fn [[p score]]
                             (pref/certify (assoc score :policy-id (:policy-id p)
                                                  :target (:target p))))
                           scored)
          uncertified (remove #(= :certified (:status %)) preference)
          {:keys [config refusal]} (precision-config field families ranked scored)
          identity (when config (precision/identity-of config))
          carry (when config
                  (try (precision/carry-beta {:previous (previous-precision opts)
                                              :initialized-beta declared
                                              :config config
                                              ;; an explicit identity-bound beta
                                              ;; declaration, the only re-arm
                                              ;; after a refusal (ruling (b))
                                              :declaration
                                              (or (:reading-precision-declaration opts)
                                                  (precision/declaration-for identity))})
                       (catch clojure.lang.ExceptionInfo e
                         {:status :refused :reason (:kind (ex-data e))})))]
      (cond
        (seq uncertified)
        {:status :refused :kind :preference-not-certified
         :refusals (vec uncertified)}
        refusal
        (merge {:status :refused} refusal)
        (= :refused (:status carry))
        {:status :refused :kind :reading-precision-refused :carry carry}
        :else
        {:status :certified :beta (:beta carry)
         :receipt {:reading-precision-carry carry
                   :reading-precision-identity identity
                   :preference-receipts preference}}))))

(defn- declared-beta
  "The one declared β over the targets with computed families, or a typed
  refusal. No default is supplied."
  [assembled families]
  (let [targets (map :target-id (filter #(= :computed (:status %)) families))
        betas (map #(get-in assembled [:declared-betas %]) targets)]
    (cond
      (some nil? betas)
      {:refusal {:kind :beta-not-declared :stage :selection
                 :target (first (keep (fn [[t b]] (when (nil? b) t)) (map vector targets betas)))
                 :targets (vec (keep (fn [[t b]] (when (nil? b) t)) (map vector targets betas)))}}
      (not= 1 (count (distinct betas)))
      {:refusal {:kind :incommensurable-family :stage :selection :target nil
                 :beta (vec (distinct betas))}}
      :else {:beta (first betas)})))

(defn- selector-refusal->judge-refusal
  "Run the one selector call F. A typed refusal from the selection law
  (cascade-selection refuse!: ex-data {:refusal {:kind ...}}, e.g.
  :free-energy-not-supplied when a policy's history is unknown) is the
  decision's typed refusal: rethrown as \"cascade decision refused\" with
  its :kind, the input it is missing, the field's one target when there is
  exactly one, and the selection refusal itself, so the runner closes the
  tick as a typed abstention rather than an untyped failure."
  [families f]
  (try (f)
       (catch clojure.lang.ExceptionInfo e
         (let [r (:refusal (ex-data e))
               targets (distinct (keep :target-id (filter #(= :computed (:status %)) families)))]
           (if (and (= "Cascade selection refused" (ex-message e)) (keyword? (:kind r)))
             (throw (ex-info "cascade decision refused"
                             (cond-> {:kind (:kind r)
                                      :missing (case (:kind r)
                                                 :free-energy-not-supplied :historical-prefix-f
                                                 {:absent :kind-names-no-missing-input})
                                      :refusal r}
                               (= 1 (count targets)) (assoc :target (first targets)))
                             e))
             (throw e))))))

(defn- abstain [refusals]
  (decision-gate/emit! {:status :abstained :refusals refusals}))

(defn- reading-family-decision
  "Q5: the one G selection over the reading policy families of the field
  (NOTE-wiring-reading-to-decision §6 item 6, §7.1). Every computed family's
  policies are scored by cascade-shape-g/score-policy and selected by ONE
  policy/select-action-cascades call inside family-selection/select-over-
  families. F is each policy's canonical carrier: its computed Prefix-F, or
  the certified zero-history carrier bound to this decision's ledger pin.
  C is the shape scorer's progress C (codex-68 ruling); live C is recorded as
  observation only. No interpretation is required before scoring."
  [assembled field opts queue]
  (let [families (vec (:families field))
        steps (or (:conditioning-steps opts)
                  {:steps [] :dir-status {:absent :no-flight-steps-in-opts}})
        census (prefix-admission/history-census steps)
        pin (prefix-admission/census-pin census)
        {:keys [beta refusal]} (declared-beta assembled families)
        attach-f (fn [ranked]
                   (policy-prefix/production-ranked
                    ranked {:conditioning-status (if (:dir-status steps) :not-run :admitted)}
                    (prefix-admission/prefixes (mapv :action ranked) steps)
                    census))
        computed? (some #(= :computed (:status %)) families)]
    (cond
      (not= :formed (:status field))
      {:decision (abstain [{:target nil :stage :policy-family :kind :no-policy-family
                             :failure-kind (:reason field)}])}

      (empty? families)
      ;; no target to form a family for: the field-level refusal that
      ;; emptied it (the outer selector's), else a typed empty field
      {:decision (abstain (or (seq (filter #(= :outer-task-selection-refused (:kind %))
                                           (:refusals assembled)))
                              [{:target nil :stage :policy-family :kind :no-candidates
                                :failure-kind :no-candidates}]))}

      (not computed?)
      {:decision (abstain (family-refusals families))}

      refusal
      {:decision (abstain (into [refusal] (family-refusals families)))}

      :else
      (let [result (selector-refusal->judge-refusal
                    families
                    #(family-selection/select-over-families
                    families
                    {:g-law (:g-law opts)
                     :certify (certify-reading-family field families beta opts)
                     ;; Q6 before the one selector: the previous run's
                     ;; refused choice, on the same selection inputs, is
                     ;; removed from the menu
                     :exclude (fn [ranked selector-opts certified]
                                (previous-run/exclusion
                                 previous-run/*predecessor* ranked selector-opts
                                 (:beta certified)
                                 (get-in certified [:receipt :reading-precision-identity])))
                     :attach-f attach-f
                     :policy-history-pin pin
                     :enactment-fold (:enactment-fold opts)
                     :pattern-feedback (:pattern-feedback opts)
                     :ticket-queue queue
                     :ticket-queue-refusals (family-refusals families)
                     ;; Packet D3: the reading route opts into the canonical
                     ;; cascade identity binding before the one selector
                     ;; (nil when the caller did not supply one)
                     :bind-cascade-identity
                     (or (:bind-cascade-identity opts)
                         (when (:require-occurrence-squares? opts)
                           {:readings
                            (fn [entry]
                              (occurrence-readings entry (:reading-pin entry)))}))
                     :novelty-inputs (or (:novelty-inputs opts) (novelty/read-inputs))}))]
        (case (:status result)
          :repeat-refused
          ;; Q6: every admissible policy is the previous run's refused
          ;; choice on the same inputs; nothing is selected or interpreted
          {:decision (-> (abstain [{:target nil :stage :q6
                                    :kind :repeat-choice-after-refusal
                                    :excluded-key (get-in result [:refusal :rearm :excluded-key])
                                    :rearm (get-in result [:refusal :rearm])}])
                         (update :selection-certificate merge
                                 (get-in result [:certification :receipt]))
                         (assoc-in [:selection-certificate :q6] (:refusal result)))}
          :refused
          {:decision (cond-> (abstain [(merge (select-keys (:refusal result)
                                                           [:reason :refusals :carry :versions :kernels])
                                              {:target nil :stage :certification
                                               :kind :model-contract-refused
                                               :certification-kind (:kind (:refusal result))})])
                       ;; the refused carry is the next tick's previous
                       ;; receipt: it stays refused until a declaration for
                       ;; this identity is consumed, never silently reset
                       (get-in result [:refusal :carry])
                       (assoc-in [:selection-certificate :reading-precision-carry]
                                 (get-in result [:refusal :carry])))}
          :abstained
          {:decision (if (= :canonical-coapply (:g-law opts))
                       (-> (abstain (into (family-refusals (:target-policy-families result))
                                          (when-let [r (get-in result [:decision :refusal])]
                                            [(assoc r :stage :canonical-g)])))
                           (assoc-in [:selection-certificate :canonical-g] (:canonical-g result)))
                       (abstain (family-refusals (:target-policy-families result))))}
          (let [selected-ranked (first (filter #(= (:action (:decision result))
                                                   (:action %))
                                               (:ranked result)))
                decision (-> (:decision result)
                             (assoc-in [:selection-certificate :selection-route]
                                       :wm/reading-policy-family-v1)
                             (assoc-in [:selection-certificate :target-policy-families]
                                       (:target-policy-families result))
                             ;; every counted failure, the field's and each
                             ;; policy the scorer refused
                             (assoc-in [:selection-certificate :family-failures]
                                       (:failures result))
                             (update :selection-certificate merge
                                     (get-in result [:certification :receipt]))
                             (assoc-in [:selection-certificate :q6] (:q6 result))
                             (assoc-in [:selection-certificate :policy-stages]
                                       (:policy-stages result))
                             (assoc-in [:selection-certificate :preference]
                                       (if (= :canonical-coapply (:g-law opts))
                                         {:c :classKernel :certified-by :wm/canonical-reading-preference-v1
                                          :live-c :observation-only}
                                         {:c :cascade-shape-g/progress-preference
                                          :certified-by pref/receipt-schema
                                          :live-c :observation-only}))
                             (assoc-in [:selection-certificate :retired-diagnostics]
                                       retired-diagnostics)
                             (assoc-in [:selection-certificate :certificate-schema]
                                       :wm/proof2-certificate-v1)
                             (assoc-in [:selection-certificate :selected-reading-pin]
                                       (:reading-pin selected-ranked))
                             ;; Requirements Q4: horizon, steps, grading and
                             ;; G terms of the compared policies, from their
                             ;; certified preference receipts
                             (as-> d (assoc-in d [:selection-certificate :q4]
                                               (q4-preference/carrier d))))
                decision (cond-> decision
                           (= :canonical-coapply (:g-law opts))
                           (assoc-in [:selection-certificate :canonical-g] (:canonical-g result)))
                authorized (controller-authority/authorize decision (:ranked result))]
            {:decision (decision-gate/emit! authorized)
             :ranked (:ranked result)}))))))

(defn cascade-decision
  "Q5: one G selection over every target's reading policy family. The field
  is formed once (graph loaded once) from the assembly's source declaration;
  the selector runs once over its scored union; one authority check and one
  gate follow. The interpreted-construction route (admission, joint-q0/class
  rank-actions, retry declines, diagnostic lanes) no longer runs; its
  products are recorded as retired. A field with no scored policy abstains
  with every family failure."
  [assembled opts]
  (cond
    (nil? (get-in opts [:task :id]))
    {:absent :no-task-selection-policy}

    :else
    (let [policy-field (target-policy-field assembled opts)
        view (observation-label-view opts)
        opts (assoc opts :observation-labels-view view)
        queue (ticket-queue/validate! (if (contains? opts :ticket-queue)
                                       (:ticket-queue opts) (ticket-queue/read-declaration)))
        result (reading-family-decision assembled policy-field opts queue)
        census (if (= :formed (:status policy-field))
                 (assoc (family-selection/field-census policy-field)
                        :status :formed
                        :reading-root (:reading-root policy-field)
                        :graph-path (:graph-path policy-field))
                 policy-field)]
    (cond-> (-> result
                (assoc :lanes []
                       :cascade-problems assembled
                       :target-policy-field policy-field
                       ;; the interpreted assembly's refusals are evidence
                       ;; about the retired route, not about this decision
                       ;; proposal and repair accounting the assembly
                       ;; recorded (record-supply) is kept as recorded; the
                       ;; interpreted assembly's own refusals are evidence
                       ;; about the retired route, not about this decision
                       :dropped-candidates
                       (into (vec (:dropped-candidates assembled))
                             (map (fn [r] {:target (:target r) :stage :retired-assembly
                                           :reason (:kind r)}))
                             (:refusals assembled)))
                (assoc-in [:decision :selection-certificate :observation-labels]
                          (observation-label-certificate view opts))
                (assoc-in [:decision :selection-certificate :target-policy-field] census)
                ;; Requirements Q2: the selected task's construction, with
                ;; the whole pattern library pinned by bytes as it stands now
                (assoc-in [:decision :selection-certificate :q2-construction]
                          (q2-construction/construction-census
                           policy-field
                           (q2-construction/library-manifest
                            (or (:pattern-library-root opts)
                                selected-reading/default-library-root))))
                (assoc-in [:decision :mission-hole-coverage]
                          (or (:mission-hole-coverage assembled)
                              {:status :absent :reason :source-coverage-not-supplied}))
                (assoc-in [:decision :live-c-coverage]
                          {:status :observation-only
                           :reason :shape-policy-c-is-decision-c}))
      (and (= :abstained (get-in result [:decision :status]))
           (nil? (get-in result [:decision :reason])))
      ;; An outer-selection refusal names the cause of the empty field, and
      ;; this function builds it with the selector's reason as :failure-kind
      ;; (the handoff refusals above); :reason is the shape a caller may hand
      ;; in. Family/scoring refusals keep the historical reason.
      (assoc-in [:decision :reason]
                (or (some :reason (get-in result [:decision :refusals]))
                    (some #(when (= :outer-task-selection-refused (:kind %))
                             (:failure-kind %))
                          (get-in result [:decision :refusals]))
                    :no-acting-cascade-candidate))
      ;; an abstained field still retains the queue's entries and why each
      ;; was not admitted (no selection: no choice, no stratum posterior)
      (and (= :abstained (get-in result [:decision :status])) (seq (:entries queue)))
      (assoc-in [:decision :selection-certificate :ticket-queue]
                (assoc (ticket-queue/plan queue []
                                          (vec (concat (:refusals assembled)
                                                       (get-in result [:decision :refusals]))))
                       :unrestricted-choice nil :choice nil :stratum-posterior nil
                       :decided-by :abstention))
      ;; an abstention that consumed no precision receipt of its own keeps the
      ;; predecessor's verbatim (sealed, unchanged), so the next tick's
      ;; previous receipt is still the last carried or refused one: an
      ;; abstention never drops a carried beta, and never re-arms a refusal
      (and (= :abstained (get-in result [:decision :status]))
           (nil? (get-in result [:decision :selection-certificate :reading-precision-carry]))
           (get-in opts [:token-belief-predecessor-trace :decision :selection-certificate
                         :reading-precision-carry]))
      (assoc-in [:decision :selection-certificate :reading-precision-carry]
                (get-in opts [:token-belief-predecessor-trace :decision :selection-certificate
                              :reading-precision-carry]))
      (:proposal-supply assembled)
      (assoc-in [:decision :selection-certificate :proposal-supply]
                (:proposal-supply assembled))))))

(defn select-and-record-cascade!
  "Select using the existing habit snapshot, without reinforcing the selection.
   The trace retains the decision; the run record labels its selection event
   separately from the outcome-based reinforcement performed at close."
  [assembled opts]
  (cascade-decision assembled opts))

;; ---------------------------------------------------------------------------
;; Packet D3: the production runner seam for a selected reading-family
;; cascade.  After ONE G selection this seam (1) binds and verifies the
;; canonical :wm/executable-cascade-v1 identity (Packet B), (2) persists the
;; canonical cascade append-only and re-validates the reloaded record,
;; (3) compiles it with whatever ATTRIBUTED operational authoring is
;; genuinely available (none on this route today: every unauthorised
;; occurrence keeps an explicit open hole), and (4) opens/appends the
;; execution journal.  Incomplete authoring holds the SAME runner phase as a
;; typed, repairable, open-cascade condition — never abstention, refusal,
;; or no-selection — and never reruns selection.
;; ---------------------------------------------------------------------------

(def default-executable-cascade-root
  (str (System/getProperty "user.home")
       "/code/futon2/data/wm-executable-cascades"))
(def default-execution-journal-root
  (str (System/getProperty "user.home")
       "/code/futon2/data/wm-execution-journals"))

(declare execute-selected-cascade!)

(defn persistable-cascade-execution
  "Remove the live retry capability before a held cascade is written to an
  EDN checkpoint.  The digest, phase, holes and prior attempts remain; code
  is a runtime capability and must never enter the run record."
  [execution]
  (dissoc execution :resume-port))

(defn- hex64? [s] (and (string? s) (re-matches #"[0-9a-f]{64}" s)))
(defn- concrete-span? [span]
  (and (map? span) (integer? (:start span)) (integer? (:end span))
       (<= 0 (:start span)) (< (:start span) (:end span))))

(defn occurrence-readings
  "The ACTUAL occurrence-keyed readings a selected reading-family action
  carries: per occurrence the reading record identity, the exact cited span,
  the 象 source-text and analysis digests (when the caller pinned them), and
  the cited pattern's source sha256.  The four-map square is not authored on
  this route yet: a reading without :square is an UNREAD citation in the
  canonical census — typed, never fabricated.  Occurrences whose provenance
  the action cannot supply (typed absences, unpinned digests) contribute no
  reading entry at all, which is the same honest unread."
  [entry {:keys [excerpt-digest analysis-digest]}]
  (into {}
        (for [node (get-in entry [:action :arrangement :nodes])
              :let [oid (:occurrence-id node)]
              :when (and oid
                         (concrete-span? (:fragment-span node))
                         (string? (:source-sha256 node))
                         (hex64? excerpt-digest)
                         (hex64? analysis-digest))]
          [oid (cond-> {:reading-record-id (str (or (:target entry) (:target-id entry) "?") "/"
                                                 (get-in entry [:action :id]) "/"
                                                 (pr-str oid))
                         :span (:fragment-span node)
                         :source-text-digest excerpt-digest
                         :analysis-digest analysis-digest
                         :source-sha256 (:source-sha256 node)}
                 (= :wm/three-halves-square-v1
                    (get-in node [:blend-square :schema]))
                 (assoc :square (:blend-square node)))])))

(defn- seam-authoring
  "The attributed operational authoring genuinely available for ENTRY: the
  caller's fn when supplied, else an empty authoring ATTRIBUTED to the
  production seam itself (so the compile is attributed even when it opens
  every hole)."
  [entry opts]
  (or (when-let [f (:cascade-operational-authoring opts)] (f entry))
      {:attribution {:author :wm/production-seam
                     :authority :wm/runner}
       :application-order []
       :clauses {}}))

(defn execute-selected-cascade!
  "The post-selection seam for a selected reading-family ENTRY.  Returns
  {:status :cascade-ready ...} when the compiled cascade has an enactable
  frontier (the flight boundary may then enact it WITH evidence), or
  {:status :cascade-enactment-open :kind :cascade-enactment-open
   :open-cascade true :repairable true :phase :cascade-execution
   :holes [...] :unread-citations [...] :re-entry {...}} when authoring is
  incomplete — the selected cascade is RETAINED (saved, validated, journaled
  under its identity) and the SAME phase is held for repair.  Typed machine
  errors (:cascade-materialisation-error, :cascade-validation-error,
  :cascade-compilation-error, :cascade-evidence-error) carry the cascade
  identity, failed clause and inputs.  Selection is never rerun here."
  [entry opts]
  (let [action (:action entry)
        target (:target entry)
        pins (or (:reading-pin entry)
                 (get-in opts [:cascade-reading-pins target]))
        seam-attribution {:author :wm/production-seam :authority :wm/runner}
        readings-fn (or (:cascade-occurrence-readings opts)
                        (fn [_]
                          (merge (occurrence-readings entry pins)
                                 (or (:cascade-square-readings opts) {}))))
        fail (fn [status kind detail]
               (merge {:status status :kind kind
                       :repairable true :phase :cascade-execution
                       :target target :cascade-id (:id action)
                       :retry-entrypoint :execute-selected-cascade!}
                      detail))
        ;; A reading route with no 象 squares yields a structural layer
        ;; whose every citation is unread (zero occurrence nodes).  No
        ;; canonical record can certify over foreign-edge endpoints then;
        ;; the seam holds the SAME phase open with the unread citations as
        ;; the re-entry inputs rather than reporting a validation defect.
        preview (executable-cascade/build-structural
                 action (readings-fn {:preview true})
                 {:stage :pre-selector-binding})]
    ;; A production checkpoint may carry the already certified canonical
    ;; cascade even when rebuilding from the lighter action would require
    ;; external square-reading inputs.  Never downgrade that retained carrier
    ;; to an unread preview; validate it in the bound path below.
    (if (and (nil? (:canonical-cascade entry))
             (map? preview) (empty? (:nodes preview)))
        {:status :cascade-enactment-open
         :kind :cascade-enactment-open
         :open-cascade true
         :repairable true
         :phase :cascade-execution
         :target target
         :cascade-id (:id action)
         :holes []
         :unread-citations (mapv :citation (get-in preview [:census :unread]))
         :re-entry {:condition :xiang-four-maps-authored
                    :inputs {:unread-citations
                             (mapv :citation (get-in preview [:census :unread]))}}
         :journal-append []}
        (let [retained (:canonical-cascade entry)
              retained-digest (:digest retained)
              selected-digest (:cascade-digest action)
              retained-validation (when retained
                                    (executable-cascade/validate-cascade retained))
              bound (if retained
                      (cond
                        (not= selected-digest retained-digest)
                        {:refusal {:kind :identity-forged
                                   :policy-id (:id action)
                                   :carried selected-digest
                                   :retained retained-digest
                                   :stage :post-selector-retained-identity}}

                        (not= retained-digest
                              (executable-cascade/digest retained))
                        {:refusal {:kind :identity-forged
                                   :policy-id (:id action)
                                   :carried retained-digest
                                   :rebuilt (executable-cascade/digest retained)
                                   :stage :post-selector-record-redigest}}

                        (not (executable-cascade/certified?
                              retained-validation))
                        {:refusal {:kind :cascade-validation-error
                                   :policy-id (:id action)
                                   :clause (:clause retained-validation)
                                   :stage :post-selector-retained-validation}}

                        :else
                        {:cascade retained
                         :digest retained-digest
                         :validation retained-validation
                         :entry (assoc entry
                                       :cascade-digest retained-digest)})
                      ;; Compatibility for direct callers that have not
                      ;; passed through Packet B. Production selection always
                      ;; supplies the retained canonical record.
                      (selection-cascade-identity/bind-entry
                       {:action action :policy-id (:id action) :target-id target}
                       readings-fn
                       (:cascade-operational-authoring opts)))]
          (if (:refusal bound)
        (fail :cascade-materialisation-error (:kind (:refusal bound))
              {:refusal (:refusal bound)})
        (let [bound-entry (:entry bound)
              verify (selection-cascade-identity/verify-selected-identity
                      {:action (:action bound-entry)} [bound-entry])]
          (if (not= :verified (:status verify))
            (fail :cascade-materialisation-error :identity-verification-failed
                  {:verification verify})
            (let [record (:canonical-cascade bound-entry)
                  d (:digest bound)
                  root (or (:executable-cascade-root opts)
                           default-executable-cascade-root)
                  journal-root (or (:execution-journal-root opts)
                                   default-execution-journal-root)
                  path (executable-cascade/save! record root)
                  reloaded (executable-cascade/load-record (io/file path))
                    rev (executable-cascade/validate-cascade reloaded)]
                (if-not (executable-cascade/certified? rev)
                  (fail :cascade-validation-error (:clause rev)
                        {:repair rev :saved path})
                  (let [compiled (cascade-enactment-compiler/compile-executable
                                  (:structural reloaded)
                                  (seam-authoring entry opts))]
                    (if (:refusal compiled)
                      (fail :cascade-compilation-error
                            (:kind (:refusal compiled))
                            {:refusal (:refusal compiled) :saved path})
                      (let [compiled-record (:record compiled)
                            cd (:digest compiled)
                            _ (executable-cascade/save! compiled-record root)
                            holes (vec (keep #(when (= :open-hole
                                                        (:enactment-status %))
                                                {:occurrence-id (:occurrence-id %)
                                                 :hole (:hole %)})
                                             (:executable-nodes compiled-record)))
                            unread (vec (get-in compiled-record
                                                [:structural :census :unread]))
                            now (str (Instant/now))
                            journal-entries
                            (vec (map-indexed
                                  (fn [i {:keys [occurrence-id]}]
                                    (execution-journal/entry
                                     {:journal-id cd
                                      :node-or-group occurrence-id
                                      :phase :open-hole
                                      :evidence {:kind :hole-witness
                                                 :digest (interpretation-evidence/value-digest
                                                          [cd occurrence-id :hole])}
                                      :valid-time now
                                      ;; the store requires strictly increasing
                                      ;; recorded times across entries
                                      :recorded-time (str (.plusNanos
                                                           (Instant/parse now)
                                                           (inc i)))}))
                                  holes))
                            bad-entries (vec (filter :refusal journal-entries))]
                        (if (seq bad-entries)
                          (fail :cascade-evidence-error :journal-entry-invalid
                                {:refusals (mapv :refusal bad-entries)})
                          (let [appended (vec (for [e journal-entries]
                                                (execution-journal/append!
                                                 (io/file journal-root cd) e)))]
                            (cond
                              (seq (get-in compiled-record [:frontier]))
                              (let [routing
                                    (capability-dispatch/route-frontier
                                     (capability-dispatch/routes opts)
                                     compiled-record)]
                                (if (= :routed (:status routing))
                                  {:status :cascade-ready
                                   :cascade-digest d
                                   :selected-cascade-digest d
                                   :compiled-digest cd
                                   :saved path
                                   :frontier (mapv :occurrence-id
                                                   (:frontier compiled-record))
                                   :capability-routing routing
                                   :journal-append appended}
                                  (fail :cascade-evidence-error
                                        (:kind routing)
                                        {:cascade-digest cd :saved path
                                         :routing routing
                                         :journal-append appended})))

                              (or (seq holes) (seq unread))
                              (let [resume-holes
                                    (mapv (fn [{:keys [occurrence-id hole]}]
                                            {:hole-id occurrence-id
                                             :owner (:owner hole)
                                             :question (or (:question hole)
                                                           (:kind hole)
                                                           :operational-clause-owed)})
                                          holes)
                                    retry (fn [resume-context]
                                            (let [accepted (:authoring resume-context)
                                                  decisions (:authoring-product
                                                             resume-context)]
                                              (if-not (map? decisions)
                                                (fail :cascade-evidence-error
                                                      :verified-authoring-product-missing
                                                      {:cascade-digest d
                                                       :requisition-id
                                                       (:requisition-id accepted)})
                                                (let [authoring
                                                      {:attribution
                                                       {:author (:by accepted)
                                                        :reviewer (:accepted-by accepted)
                                                        :authority :wm/reviewed-resume}
                                                       :application-order []
                                                       :clauses decisions}]
                                                  (execute-selected-cascade!
                                                   entry
                                                   (assoc opts
                                                          :cascade-operational-authoring
                                                          (constantly authoring)))))))]
                                {:status :cascade-enactment-open
                               :kind :wm/open-cascade-execution-error
                               :reason :cascade-enactment-open
                               :open-cascade true
                               :repairable true
                               :phase :compile
                               :target target
                               :cascade-id (:id action)
                               ;; D4 resumes the executable refinement.  The
                               ;; selected/pre-selector identity remains
                               ;; explicit rather than being conflated with
                               ;; the refinement identity.
                               :selected-cascade-digest d
                               :cascade-digest cd
                               :compiled-digest cd
                               :saved path
                               :compiled-saved (executable-cascade/save!
                                                compiled-record root)
                               :holes resume-holes
                               :open-hole-details holes
                               :unread-citations (mapv :citation unread)
                               :re-entry
                               {:condition :operational-authoring-supplied
                                :inputs {:holes holes
                                         :unread-citations (mapv :citation unread)}
                                :seam-attribution seam-attribution}
                               :journal-append appended
                               :attempts []
                               :resume-port {:retry retry}})

                              :else
                              {:status :cascade-ready
                               :cascade-digest d
                               :selected-cascade-digest d
                               :compiled-digest cd
                               :saved path
                               :journal-append appended})))))))))))))))
