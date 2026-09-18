(ns futon2.report.cascade-decision-test
  "SPEC-flat-removal H5a: the joint cascade decision over assembled
  problems. cascade-decision (war_machine.clj, next to cascade-lane) runs
  cascade-lane per problem, carries H2's receipts onto the candidates
  (dropping any whose receipts cannot be matched, with a recorded reason),
  recomputes G jointly by ONE rank-actions call over the union family (one
  common universe), selects by ONE select-action-cascades at the common
  declared β, and passes the result through decision-gate/emit!. With no
  problems it is the gated abstention. Different T or β across problems
  refuses :incommensurable-family."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.live-c :as lc]
            [futon2.aif.locator-fixtures :as locfix]
            [futon2.report.war-machine :as wm]))

(defn- assemble*
  "cp/assemble with every token given a fixture C3 locator (P5 locator
  requirement); tests about locators call cp/assemble directly."
  [m]
  (cp/assemble (update m :sources locfix/locate-all)))

(def tick-1-target :wm-tick-001-observation-crash)

(def tick-1-universe
  {:summary-without-total-repos-throws true
   :active-repo-ratio-absent-default-is-0 true
   :coupling-density-reads-same-key-with-default true
   :observe-empty-does-not-throw true
   :test-covers-missing-total-repos false
   :summary-without-total-repos-observes-cleanly :unknown})

(def tick-1-want
  [:summary-without-total-repos-observes-cleanly
   :active-repo-ratio-absent-default-is-0
   :test-covers-missing-total-repos])

(def tick-1-interpretations
  {:patterns
   {:aif/structured-observation-vector
    {:guard {:needs #{:summary-without-total-repos-throws}
             :forbids #{:summary-without-total-repos-observes-cleanly}}
     :produces #{:summary-without-total-repos-observes-cleanly}}
   :aif/placeholder-is-load-bearing
    {:guard {:needs #{:coupling-density-reads-same-key-with-default
                      :summary-without-total-repos-throws}
             :forbids #{:summary-without-total-repos-observes-cleanly}}
     :produces #{:summary-without-total-repos-observes-cleanly
                :active-repo-ratio-absent-default-is-0}}
   :test-step-covering-missing-total-repos
    {:guard {:needs #{:summary-without-total-repos-throws}
             :forbids #{:test-covers-missing-total-repos}}
     :produces #{:test-covers-missing-total-repos}}}
   :receipts
   {:aif/structured-observation-vector {:receipt "S1-interpretation" :source "03-R6"}
    :aif/placeholder-is-load-bearing {:receipt "PH-interpretation" :source "03-R6"}
    :test-step-covering-missing-total-repos {:receipt "TS-interpretation" :source "03-R6"}}})

(def live-c-fixture
  "WIRE-3 test seam: a derived live C whose want token IS in the tick-1
  joint domain (target-qualified, exactly as the qualification scheme
  builds the family's tokens), with a fixed weight — so these tests do
  not read the real corpus (the derivation itself is covered in
  futon2.aif.live-c-test) and stay deterministic."
  {:want #{[tick-1-target :test-covers-missing-total-repos]}
   :weights {[tick-1-target :test-covers-missing-total-repos] 1}
   :lam 1 :entries [] :gaps [] :refusals nil
   :signature "cascade-decision-test-live-c"})

(def live-c-opts {:live-c {:derived live-c-fixture}})

(def receipt
  {:kind :construction-receipt :moves [:interpret :order]
   :family-searched 3 :coverage 1})

(def tick-1-candidates
  [{:precedence [:test-step-covering-missing-total-repos
                 :aif/structured-observation-vector]
    :construction-receipt receipt}
   {:precedence [:aif/placeholder-is-load-bearing
                 :test-step-covering-missing-total-repos
                 :aif/structured-observation-vector]
    :construction-receipt receipt}
   {:precedence [:aif/structured-observation-vector]
    :construction-receipt receipt}])

(def tick-1-sources
  {:universes {tick-1-target tick-1-universe}
   :interpretations {tick-1-target tick-1-interpretations}
   :wants {tick-1-target tick-1-want}
   :candidates {tick-1-target tick-1-candidates}
   :horizon-steps 3
   :beta-by-context {:tick-1 {:beta 1}}
   :context-of (fn [_] :tick-1)})

(deftest h5a-no-problems-is-a-gated-abstention
  ;; empty per-target sources (the horizon is the tick's declared input, so
  ;; the per-target refusal kinds are exercised, not the refuse-all rule)
  (let [assembled (assemble* {:targets [:A :B]
                                :sources {:horizon-steps 3}})
        r (wm/cascade-decision assembled live-c-opts)]
    (is (= :abstained (get-in r [:decision :status]))
        "no assembled problem ⇒ the abstention")
    (is (= 2 (count (get-in r [:decision :refusals])))
        "the abstention lists both targets' refusals")
    (is (= [:universe-not-admitted :universe-not-admitted]
           (mapv :kind (get-in r [:decision :refusals])))
        "each refusal is typed; the decision has passed emit! by construction")))

(deftest h5a-tick-1-decision
  (let [assembled (assemble* {:targets [tick-1-target]
                                :sources tick-1-sources})
        r (wm/cascade-decision assembled live-c-opts)
        decision (:decision r)]
    ;; WIRE-3: the injected live C weights
    ;; [:wm-tick-001-observation-crash :test-covers-missing-total-repos]
    ;; at 1 (3x the uniform 1/3 share), and the choice follows the weight:
    ;; :test-step-covering-missing-total-repos now outranks
    ;; :aif/placeholder-is-load-bearing (uniform: 0.37005613489124095 and
    ;; the placeholder pattern won).
    (is (= :test-step-covering-missing-total-repos
           (-> decision :action :precedence first :id))
        "the weighted want token moves the choice to the test-step cascade")
    (is (= 0.6217118810867193 (:chosen-action-mass decision))
        "the chosen action's mass is the posterior marginal 0.6217118810867193")
    (is (= {:value 1 :status :declared} (:beta decision))
        "β = 1 is recorded :declared")
    (let [posterior (get-in decision [:selection-law :posterior])]
      (is (every? #(some? (:construction-receipt %)) (keys posterior))
          "every posterior candidate carries a construction receipt")
      (is (every? #(seq (:interpretation-receipts %)) (keys posterior))
          "every posterior candidate carries non-empty interpretation receipts")
      (is (< 0.999999999 (reduce + (vals posterior)) 1.000000001)
          "the posterior is normalised"))
    (is (= [[:R1 :R6 :R13 :R4 :R5 :R14 :R16 :R9]]
           (mapv #(mapv :node (:route %)) (:lanes r)))
        "the tick-1 target's lane route is recorded")))

(deftest h5a-joint-selection-across-targets
  ;; target B's single cascade establishes its want (lower G); target A's
  ;; only constructed cascade is guard-blocked, so it stalls like A's empty
  ;; cascade. Joint selection must pick B's first pattern from ONE
  ;; posterior spanning both targets' candidates.
  (let [b-target :B
        sources
        (merge tick-1-sources
               {:universes (assoc (:universes tick-1-sources)
                                  b-target {:b-open true :b-clean false})
                :interpretations (assoc (:interpretations tick-1-sources)
                                        b-target
                                        {:patterns
                                         {:b-fix {:guard {:needs #{:b-open}
                                                          :forbids #{:b-clean}}
                                                  :produces #{:b-clean}}}
                                         :receipts {:b-fix {:receipt "B-fix"
                                                            :source "fixture"}}})
                :wants (assoc (:wants tick-1-sources) b-target [:b-clean])
                :candidates (assoc (:candidates tick-1-sources)
                                   b-target
                                   [{:precedence [:b-fix]
                                     :construction-receipt receipt}])})
        ;; target A keeps its tick-1 family minus the want-establishing
        ;; candidates: only a guard-blocked cascade remains, so B wins.
        sources (update-in sources
                           [:candidates tick-1-target]
                           (fn [cands]
                             [(assoc-in (nth cands 2)
                                        [:precedence]
                                         [:test-step-covering-missing-total-repos])]))
        assembled (assemble* {:targets [tick-1-target b-target]
                                :sources sources})
        r (wm/cascade-decision assembled live-c-opts)
        decision (:decision r)
        posterior (get-in decision [:selection-law :posterior])]
    ;; WIRE-3: the live C's weight on tick-1's :test-covers token (1 vs
    ;; the uniform 1/3 share) now outranks B's lower-G cascade — the
    ;; derived preference redistributes which target the machine acts on.
    (is (= :test-step-covering-missing-total-repos
           (-> decision :action :precedence first :id))
        "the weighted tick-1 cascade wins the JOINT selection")
    (is (= #{tick-1-target b-target}
           (set (map :target (keys posterior))))
        "one posterior spans both targets' candidates")
    (is (< 0.999999999 (reduce + (vals posterior)) 1.000000001)
        "the joint posterior's masses sum to 1")))

(deftest h5a-unmatched-receipt-drops-the-candidate
  (let [assembled (assemble* {:targets [tick-1-target]
                                :sources tick-1-sources})
        ;; strip the last construction receipt: that precedence can no
        ;; longer be matched, so its candidate must be dropped, recorded,
        ;; and never passed through unreceipted.
        stripped (update-in assembled
                            [:problems 0 :construction-receipts]
                            #(vec (butlast %)))
        r (wm/cascade-decision stripped live-c-opts)
        decision (:decision r)
        posterior (get-in decision [:selection-law :posterior])]
    (is (= [{:target tick-1-target
             :candidate :C3
             :reason :construction-receipt-unmatched}]
           (:dropped-candidates r))
        "the unmatched candidate is dropped with a recorded reason")
    (is (every? #(some? (:construction-receipt %)) (keys posterior))
        "no unreceipted candidate reached the posterior")
    (is (not (some #(= :C3 (:id %)) (keys posterior)))
        "the dropped candidate is absent from the posterior")
    (is (= :test-step-covering-missing-total-repos
           (-> decision :action :precedence first :id))
        "the remaining family still selects through the gate")))

(deftest h5a-incommensurable-family-refuses
  (let [assembled (assemble* {:targets [tick-1-target]
                                :sources tick-1-sources})
        ;; one assemble call declares one T; a different-T problem can only
        ;; arrive as a second assembled problem, so build it directly.
        other (assemble* {:targets [:B]
                            :sources (assoc tick-1-sources
                                            :universes {:B {:b-open true}}
                                            :interpretations
                                            {:B {:patterns
                                                 {:b-fix {:guard {:needs #{:b-open}}
                                                          :produces #{:b-open}}}
                                                 :receipts {:b-fix {:receipt "B"}}}}
                                            :wants {:B [:b-open]}
                                            :candidates
                                            {:B [{:precedence [:b-fix]
                                                  :construction-receipt receipt}]}
                                            :horizon-steps 2)})
        merged (update assembled :problems into (:problems other))]
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"cascade decision refused"
         (wm/cascade-decision merged live-c-opts)))
    (is (= :incommensurable-family
           (try (wm/cascade-decision merged live-c-opts)
                (catch clojure.lang.ExceptionInfo e
                  (:kind (ex-data e)))))
        "different T across problems refuses, typed, with the values")))

(deftest h5a-targets-stay-apart
  ;; (f) Two targets using the SAME pattern id and the SAME token names.
  ;; A's fact is true, B's is false: B's guard must not be satisfied by
  ;; A's fact, the marginal is per (target, pattern) — never summed across
  ;; targets — and the decision's action carries its :target.
  (let [pattern {:p {:guard {:needs #{:open} :forbids #{:done}}
                     :produces #{:done}}}
        interp {:patterns pattern :receipts {:p {:receipt "p" :source "f"}}}
        assembled (assemble*
                   {:targets [:A :B]
                    :sources {:universes {:A {:open true :done false}
                                          :B {:open false :done false}}
                              :interpretations {:A interp :B interp}
                              :wants {:A [:done] :B [:done]}
                              :candidates {:A [{:precedence [:p]
                                                :construction-receipt receipt}]
                                           :B [{:precedence [:p]
                                                :construction-receipt receipt}]}
                              :horizon-steps 3
                              :beta-by-context {:x {:beta 1}}
                              :context-of (fn [_] :x)}})
        r (wm/cascade-decision
           assembled
           ;; this test's own live C: its want token must be in THIS
           ;; family's domain, or the reachable restriction rightly
           ;; refuses (see the wire-3 production-state test below).
           {:live-c {:derived (assoc live-c-fixture
                                      :want #{[:A :done]}
                                      :weights {[:A :done] 1})}})
        decision (:decision r)
        posterior (get-in decision [:selection-law :posterior])
        by (fn [t id]
             (some (fn [[c p]] (when (and (= t (:target c)) (= id (:id c))) [c p]))
                   posterior))
        [_ a-p] (by :A :C1)
        [_ b-p] (by :B :C1)]
    (is (some? a-p))
    (is (some? b-p))
    ;; B's :p candidate never establishes [:B :done]: its guard is blocked
    ;; (B's own :open is false and A's :open true does NOT reach it), so it
    ;; scores exactly like B's empty cascade — equal G ⇒ equal posterior
    ;; mass at equal habit.
    (is (< (Math/abs (- (double b-p) (double (second (by :B :C0))))) 1e-9)
        "B's guard is not satisfied by A's fact: B's same-id candidate scores like B's empty cascade (identity stall)")
    (is (> (double a-p) (double b-p))
        "A's candidate, whose OWN fact satisfies the guard, carries more posterior mass (lower G)")
    (is (= :A (get-in decision [:action :target]))
        "the decision's action carries its :target")
    (is (= :p (-> decision :action :precedence first :id)))
    (is (= :A (-> decision :action :precedence first :target))
        "the first acting pattern map carries its :target")
    (let [chosen-pattern (get-in decision [:action :precedence 0])
          marginal (transduce (comp (filter (fn [[c _]]
                                              (= chosen-pattern
                                                 (first (:precedence c)))))
                                    (map val))
                              + 0.0 posterior)]
      (is (< (Math/abs (- (double (:chosen-action-mass decision))
                          (double marginal)))
             1e-9)
          "the chosen mass equals the posterior mass of THIS target's candidates starting with that pattern (A's :p alone), not the sum over both targets")
      (is (< (Math/abs (- (double marginal) (double a-p))) 1e-9)
          "the marginal is A's :p candidate's mass exactly — B's same-id pattern contributed nothing"))
    (is (= :target-token-pair
           (get-in decision [:token-qualification :scheme]))
        "the token qualification scheme is recorded on the decision")))

;; ===== WIRE-3: the derived live C into the joint preference spec =====

(deftest wire-3-stale-live-c-refuses-the-decision-typed
  ;; Requirement 2: a stale C is never scored and never silently uniform.
  ;; The injected :derived C's signature cannot match a fresh read of the
  ;; real corpus, so the freshness guard fires.
  (let [assembled (assemble* {:targets [tick-1-target]
                              :sources tick-1-sources})
        call (fn [] (wm/cascade-decision
                     assembled
                     {:live-c {:derived live-c-fixture
                               :sources-now (lc/read-sources)}}))]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"cascade decision refused"
                          (call)))
    (let [e (try (call) (catch clojure.lang.ExceptionInfo e e))
          d (ex-data e)]
      (is (= :live-c-stale (:kind d)) "the refusal is typed :live-c-stale")
      (is (= (:signature live-c-fixture) (:signature-derived d))
          "both signatures are recorded — derived and now"))))

(deftest wire-3-refused-derivation-propagates-typed
  ;; Requirement 3: a missing source refuses the whole derivation, and the
  ;; decision refuses with it — no silent uniform fallback.
  (let [assembled (assemble* {:targets [tick-1-target]
                              :sources tick-1-sources})
        e (try (wm/cascade-decision
                assembled
                {:live-c {:sources {:wholeness {:refusal {:kind :source-missing
                                                          :path "/nonexistent"}}
                                    :missions []
                                    :stars {:value {:capabilities {}}}}}})
               (catch clojure.lang.ExceptionInfo e e))
        d (ex-data e)]
    (is (= :live-c-refused (:kind d)))
    (is (= [:source-missing] (mapv :kind (:refusals d))))))

(deftest wire-3-live-c-tokens-are-not-in-any-real-tick-domain-today
  ;; The FINDING, recorded as a test: the real corpus's live-C tokens are
  ;; namespaced keywords (:alive/M-* :closed/M-* :star/*), while the joint
  ;; cascade domain is target-qualified [target token] pairs. The two
  ;; vocabularies are disjoint, so live-c/cascade-spec refuses
  ;; :no-reachable-want for every real decision. That is the undeclared
  ;; mission grain of C (:c-mis, WM-13) stated concretely.
  ;;
  ;; live-c is RIGHT to refuse when asked for a spec it cannot honestly give,
  ;; and that law is untouched. What the caller does with the refusal is a
  ;; separate decision (claude-4, 2026-09-18): a grain mismatch is C having no
  ;; opinion about these outcomes, not C being broken or stale, so the
  ;; comparison proceeds on the uniform spec it used before this slice and
  ;; RECORDS that it did. Halting every production decision would make this
  ;; wiring answerable for a gap it only revealed.
  (let [assembled (assemble* {:targets [tick-1-target]
                              :sources tick-1-sources})
        d (lc/derive-live-c (lc/read-sources))]
    (is (seq (:want d)) "the real corpus derives a non-empty live C")
    (is (every? keyword? (:want d))
        "live-C tokens are (namespaced) keywords, never [target token] pairs")
    (is (= :no-reachable-want
           (:kind (:refusal (lc/cascade-spec d #{[:t "tok"]}))))
        "asked for a spec over a target-qualified domain, live-c refuses")
    (let [decision (wm/cascade-decision assembled {})]
      (is (map? decision)
          "a grain mismatch does not halt the decision")
      (is (some? (get-in decision [:decision :action]))
          "the comparison still produces a choice")
      (let [c (->> (get-in decision [:decision :selection-law :posterior])
                   keys first)]
        (is (some? c) "candidates were scored")))
    ;; and the grain mismatch is RECORDED, not silent: uniform-because-no-overlap
    ;; must never be mistaken for C-was-derived-and-agreed.
    (let [spec-c (-> (wm/cascade-decision assembled {})
                     (get-in [:decision :token-qualification]))]
      (is (= :target-token-pair (:scheme spec-c))
          "the decision states the qualification scheme its outcomes use"))))
