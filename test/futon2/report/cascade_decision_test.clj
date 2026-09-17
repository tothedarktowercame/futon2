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
            [futon2.report.war-machine :as wm]))

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
  (let [assembled (cp/assemble {:targets [:A :B]
                                :sources {:horizon-steps 3}})
        r (wm/cascade-decision assembled {})]
    (is (= :abstained (get-in r [:decision :status]))
        "no assembled problem ⇒ the abstention")
    (is (= 2 (count (get-in r [:decision :refusals])))
        "the abstention lists both targets' refusals")
    (is (= [:universe-not-admitted :universe-not-admitted]
           (mapv :kind (get-in r [:decision :refusals])))
        "each refusal is typed; the decision has passed emit! by construction")))

(deftest h5a-tick-1-decision
  (let [assembled (cp/assemble {:targets [tick-1-target]
                                :sources tick-1-sources})
        r (wm/cascade-decision assembled {})
        decision (:decision r)]
    (is (= :aif/placeholder-is-load-bearing
           (-> decision :action :precedence first :id))
        "the gated decision chooses :aif/placeholder-is-load-bearing")
    (is (= 0.37005613489124095 (:chosen-action-mass decision))
        "the chosen action's mass is the posterior marginal 0.37005613489124095")
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
        assembled (cp/assemble {:targets [tick-1-target b-target]
                                :sources sources})
        r (wm/cascade-decision assembled {})
        decision (:decision r)
        posterior (get-in decision [:selection-law :posterior])]
    (is (= :b-fix (-> decision :action :precedence first :id))
        "B's lower-G cascade wins the JOINT selection")
    (is (= #{tick-1-target b-target}
           (set (map :target (keys posterior))))
        "one posterior spans both targets' candidates")
    (is (< 0.999999999 (reduce + (vals posterior)) 1.000000001)
        "the joint posterior's masses sum to 1")))

(deftest h5a-unmatched-receipt-drops-the-candidate
  (let [assembled (cp/assemble {:targets [tick-1-target]
                                :sources tick-1-sources})
        ;; strip the last construction receipt: that precedence can no
        ;; longer be matched, so its candidate must be dropped, recorded,
        ;; and never passed through unreceipted.
        stripped (update-in assembled
                            [:problems 0 :construction-receipts]
                            #(vec (butlast %)))
        r (wm/cascade-decision stripped {})
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
    (is (= :aif/placeholder-is-load-bearing
           (-> decision :action :precedence first :id))
        "the remaining family still selects through the gate")))

(deftest h5a-incommensurable-family-refuses
  (let [assembled (cp/assemble {:targets [tick-1-target]
                                :sources tick-1-sources})
        ;; one assemble call declares one T; a different-T problem can only
        ;; arrive as a second assembled problem, so build it directly.
        other (cp/assemble {:targets [:B]
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
         (wm/cascade-decision merged {})))
    (is (= :incommensurable-family
           (try (wm/cascade-decision merged {})
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
        assembled (cp/assemble
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
        r (wm/cascade-decision assembled {})
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
