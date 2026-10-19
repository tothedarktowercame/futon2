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
  (:require [clojure.edn :as edn]
            [clojure.string]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-model-manifest :as manifest]
            [futon2.aif.focus-receipt :as focus-receipt]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.cascade-selection :as selection]
            [futon2.aif.live-c :as lc]
            [futon2.aif.locator-fixtures :as locfix]
            [futon2.aif.policy-prefix-admission :as prefix-admission]
            [futon2.aif.wm.cascade-decision :as wm-cd]
            [futon2.aif.cascade-shape-g :as shape-g]
            [futon2.aif.policy :as policy]
            [futon2.aif.wm.family-selection :as family-selection]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.test-support.reading-route :as route]))

(use-fixtures :once hermetic/with-hermetic-stores)
(use-fixtures :each route/with-empty-learning-ledger)

(defn assemble*
  "cp/assemble with every token given a fixture C3 locator (P5 locator
  requirement); tests about locators call cp/assemble directly."
  [m]
  (let [square (edn/read-string
                (slurp "test/fixtures/three-halves-square/publication-cadence.edn"))
        m (update-in m [:sources :interpretations]
                     (fn [targets]
                       (into {}
                             (map (fn [[target v]]
                                    [target (update v :receipts
                                                    (fn [receipts]
                                                      (into {}
                                                            (map (fn [[id receipt]]
                                                                   [id (assoc receipt :blend-square square)]))
                                                            receipts)))])
                                  targets))))]
    (update (cp/assemble (update m :sources locfix/locate-all)) :problems
            (fn [problems]
              (mapv (fn [problem]
                      (let [attach (fn [receipts]
                                     (into {} (map (fn [[id r]]
                                                     [id (assoc r :blend-square square)])) receipts))]
                        (-> problem
                            (update :interpretation-receipts attach)
                            (update :constructed-candidates
                                    (fn [candidates]
                                      (mapv (fn [candidate]
                                              (update candidate :interpretation-receipts attach))
                                            candidates))))))
                    problems)))))

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
  "WIRE-live-c test seam: a mission-grain token for the tick target.  The
  production vocabulary map projects it onto that mission's declared wants."
  {:want #{(keyword "alive" (name tick-1-target))}
   :weights {(keyword "alive" (name tick-1-target)) 1}
   :lam 1 :entries [] :gaps [] :refusals nil
   :signature "cascade-decision-test-live-c"})

(def live-c-opts
  ;; PROOF-wm-works 1.3 handoff B: the synthetic targets have no corpus
  ;; relations, and under the shared relation producer an unresolved relation
  ;; gets no scalar G -- so the fixture injects a corpus with relations for
  ;; its own targets (the same injection seam as :live-c).
  {:live-c {:derived live-c-fixture}
   :focus-inputs (assoc (futon2.aif.focus-receipt/read-inputs)
                        :relations [{:target :wm-tick-001-observation-crash :facet "WM"
                                     :relation "focus"
                                     :source {:repo "fixture" :commit "0" :path "test" :section "fixture"}
                                     :effective-from "2026-01-01T00:00:00Z"}
                                    {:target :B :facet "WM"
                                     :relation "focus"
                                     :source {:repo "fixture" :commit "0" :path "test" :section "fixture"}
                                     :effective-from "2026-01-01T00:00:00Z"}
                                    {:target :A :facet "WM"
                                     :relation "focus"
                                     :source {:repo "fixture" :commit "0" :path "test" :section "fixture"}
                                     :effective-from "2026-01-01T00:00:00Z"}])})

(defn decision
  "Run the joint decision with one explicit admitted-history step for every
  constructed fixture policy. These tests exercise stages after prefix-F;
  the prefix-specific suites test missing, broken, and contradictory history."
  [assembled opts]
  (let [candidates (for [{:keys [target constructed-candidates]} (:problems assembled)
                         {:keys [candidate-id precedence]} constructed-candidates]
                     {:id candidate-id :target target :precedence precedence})
        steps (mapv (fn [i candidate]
                      (let [k (prefix-admission/candidate-key candidate)]
                        {:step {:status :present :policy-key k
                                :occurrence {:flight "fixture"
                                             :click (str "run-" i)}
                                :s-prev {:value {#{} 1}}
                                :q {#{} 1}
                                :f 0.0}
                         :path "cascade-decision-test"
                         :sha256 "fixture"}))
                    (range) candidates)]
    (wm-cd/cascade-decision
     assembled
     (assoc opts :conditioning-steps {:steps steps :read [] :unread []}))))

(deftest cross-source-want-cannot-resurrect-zeroed-outcome
  ;; Exercise the production merge used by cascade-decision.  The same token
  ;; is wanted by the decision and excluded as the singleton outcome by live
  ;; C.  Utility still sees the want, but C's exact-zero override must win all
  ;; the way through selection at every temperature.
  (let [token [:M-a :closed]
        outcome #{token}
        merged (wm-cd/merge-live-cascade-spec
                #{token}
                {:want #{}
                 :weights {}
                 :lam 1 :mu 0 :evidence #{}
                 :zeroed #{outcome}
                 :live-c {:signature "zero-over-want"}})
        spec (manifest/preference-spec merged)
        c (manifest/preference-distribution spec #{token})
        g (manifest/outcome-risk {outcome 1} c)]
    (is (contains? (:want merged) token)
        "the other source's want reached the real merged spec")
    (is (contains? (:zeroed merged) outcome)
        "the production merge retained the conflicting exact exclusion")
    (is (zero? (get c outcome))
        "zeroed overrides the wanted token's positive utility")
    (is (= :infinite g))
    (doseq [temperature [1/10 10]]
      (is (= 0.0
             (get (selection/selection-posterior
                   {:beta temperature
                    :candidates [{:id :zeroed :habit 1 :f 0 :g g}
                                 {:id :admissible :habit 1 :f 0 :g 0}]})
                  :zeroed))
          (str "zeroed posterior stays 0 at temperature " temperature)))))

(deftest wire-live-c-projection-laws
  (let [joint-want #{[:M-a :x] [:M-a :y] [:M-b :z]}
        derived {:want #{:alive/M-a :closed/M-b :star/capability}
                 :weights {:alive/M-a 1/2
                           :closed/M-b 1/3
                           :star/capability 1/6}
                 :lam 1 :entries [] :gaps [] :signature "projection-laws"}
        p (lc/project-want derived joint-want)
        spec (lc/cascade-spec derived joint-want joint-want)]
    (is (= 1/2 (+ (get-in p [:weights [:M-a :x]])
                   (get-in p [:weights [:M-a :y]])))
        "expansion over k outcomes conserves the source token's total mass")
    (is (= 1/3 (get-in p [:weights [:M-b :z]])))
    (is (not (contains? (:want spec) :star/capability)))
    (is (some #{":star/capability"}
              (get-in spec [:live-c :unreached-in-domain])))))

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

;; ===== Q5 reading-family route (migrated 2026-10-15, Q5 route ledger) =====
;;
;; The joint decision now selects once, by G, over every target's reading
;; policy family (test-support/reading-route). The tests below keep their
;; names and re-prove each preserved claim on that route. Retired with the
;; interpreted route, and noted at each test: construction/interpretation
;; receipts on candidates, diagnostic lanes and their null comparisons,
;; target-token qualification, the class scorer's unknown-class decline and
;; live C as a scored preference (live C is observation only; its
;; projection laws above still hold for the live-C module itself).

(def ^:private p-ab ["p/a" "p/b"])

(defn- refusal-kinds [r]
  (mapv (juxt :kind :failure-kind) (get-in r [:decision :refusals])))

(defn- posterior-of [r] (get-in r [:decision :selection-law :posterior]))

(defn- refusing-policies
  "score-policy, except that policies whose id satisfies REFUSE? are refused
  typed, as the scorer refuses a policy it cannot score."
  [refuse?]
  (let [score shape-g/score-policy]
    (fn [p] (if (refuse? (:policy-id p))
              {:status :refused :reason :fixture-unscorable :policy-id (:policy-id p)}
              (score p)))))

(deftest h5a-no-problems-is-a-gated-abstention
  ;; no target has a current reading: no family, the gated abstention
  (let [fld (route/field {"M-a" nil "M-b" nil})
        r (route/decide-field fld {"M-a" 1 "M-b" 1} {})]
    (is (= :abstained (get-in r [:decision :status])))
    (is (= 0 (:selector-calls r)))
    (is (= [[:no-policy-family :no-current-target-reading]
            [:no-policy-family :no-current-target-reading]]
           (refusal-kinds r))
        "each target's refusal is typed; the decision passed emit! by construction")))

(deftest missing-slice-refuses-the-whole-field
  ;; the reading-route counterpart of a missing slice is a missing reading
  (let [fld (route/field {"M-a" p-ab "M-unread" nil})
        r (route/decide-field fld {"M-a" 1 "M-unread" 1} {})
        refusals (get-in r [:decision :refusals])]
    (is (= 0 (:selector-calls r)))
    (is (= :abstained (get-in r [:decision :status])))
    (is (nil? (get-in r [:decision :action])))
    (is (some #(and (= "M-unread" (:target %))
                    (= :no-policy-family (:kind %))) refusals))))

(deftest h5a-tick-1-decision
  ;; retired: locators on the action, interpretation receipts on candidates,
  ;; the diagnostic lane route
  (let [r (route/decide (route/fixture))
        decision (:decision r)
        posterior (posterior-of r)]
    (is (= 1 (:selector-calls r)))
    (is (= {:value 1 :status :declared} (:beta decision)) "β = 1 is recorded :declared")
    (is (every? #(= (:policy-key %) (prefix-admission/candidate-key %)) (keys posterior))
        "every posterior candidate carries its own reading-policy identity")
    (is (< 0.999999999 (reduce + (vals posterior)) 1.000000001) "the posterior is normalised")
    (is (= [] (:lanes r)) "no diagnostic lane runs")
    (is (= :unavailable (get-in decision [:selection-certificate :retired-diagnostics
                                          :token-rate-lanes :status])))))

(deftest h5a-joint-selection-across-targets
  (let [fld (route/field {"M-a" p-ab "M-b" ["p/b" "p/c"]})
        r (route/decide-field fld {"M-a" 1 "M-b" 1} {})
        posterior (posterior-of r)]
    (is (= 1 (:selector-calls r)) "ONE selector call for both targets")
    (is (= #{"M-a" "M-b"} (set (map :target (keys posterior))))
        "one posterior spans both targets' policies")
    (is (< 0.999999999 (reduce + (vals posterior)) 1.000000001)
        "the joint posterior's masses sum to 1")))

(deftest h5a-unmatched-receipt-drops-the-candidate
  ;; retired: construction-receipt matching. Preserved: a policy that cannot
  ;; be admitted to the posterior is dropped with a recorded reason, never
  ;; passed through, and the rest of the family still selects.
  (let [fx (route/fixture)
        ids (keys (route/policy-keys (:decision (route/decide fx))))
        dropped (first ids)
        r (with-redefs [shape-g/score-policy (refusing-policies #{dropped})]
            (route/decide fx))
        cert (get-in r [:decision :selection-certificate])]
    (is (= 2 (count ids)))
    (is (some #(and (= dropped (:policy-id %)) (= :fixture-unscorable (:reason %)))
              (:family-failures cert))
        "the unscorable policy is recorded with its reason")
    (is (not-any? #(= dropped (:id %)) (keys (posterior-of r)))
        "it never reaches the posterior")
    (is (= 1 (:selector-calls r)))
    (is (some? (get-in r [:decision :action])) "the remaining family still selects")))

(deftest h5a-incommensurable-family-refuses
  ;; differing declared temperatures refuse, typed, with the values, and
  ;; the selector is not called (the reading route abstains rather than throws)
  (let [fld (route/field {"M-a" p-ab "M-b" ["p/b" "p/c"]})
        r (route/decide-field fld {"M-a" 1 "M-b" 2} {})
        refusal (first (get-in r [:decision :refusals]))]
    (is (= 0 (:selector-calls r)))
    (is (= :abstained (get-in r [:decision :status])))
    (is (= :incommensurable-family (:kind refusal)))
    (is (= #{1 2} (set (:beta refusal))))))

(deftest h5a-targets-stay-apart
  ;; two targets reading the SAME patterns: their policies stay distinct
  ;; (identity is target-scoped), the marginal is per policy, and the action
  ;; carries its target. Retired: target-token qualification of the old
  ;; operator tokens.
  (let [fld (route/field {"M-a" p-ab "M-b" p-ab})
        r (route/decide-field fld {"M-a" 1 "M-b" 1} {})
        posterior (posterior-of r)
        keys* (map prefix-admission/candidate-key (keys posterior))
        per-target (group-by :target (keys posterior))]
    (is (= 1 (:selector-calls r)))
    (is (= #{"M-a" "M-b"} (set (keys per-target))))
    (is (= (count (get per-target "M-a")) (count (get per-target "M-b"))))
    (is (= (count posterior) (count (distinct keys*)))
        "same patterns on distinct targets remain distinct policies")
    (is (every? (fn [[t cs]] (every? #(clojure.string/starts-with? (:id %) (str t "/policy-")) cs))
                per-target))
    (is (contains? #{"M-a" "M-b"} (get-in r [:decision :action :target]))
        "the decision's action carries its :target")
    (is (< (Math/abs (- (double (get-in r [:decision :chosen-action-mass]))
                        (double (get posterior (get-in r [:decision :action])))))
           1e-9)
        "the chosen mass is that one policy's posterior mass, not a sum across targets")))

(deftest wire-3-stale-live-c-refuses-the-decision-typed
  ;; retired: live C is no longer a scored preference (codex-68 ruling:
  ;; shape-policy C is the decision C, live C observation only), so there is
  ;; nothing for a stale live C to corrupt. Preserved: a stale live C cannot
  ;; change the selection.
  (let [fx (route/fixture)
        plain (route/decide fx)
        stale (route/decide fx 1 {:live-c {:derived live-c-fixture
                                           :sources-now (lc/read-sources)}})]
    (is (= (get-in plain [:decision :selection-law :posterior])
           (get-in stale [:decision :selection-law :posterior])))
    (is (= :observation-only (get-in stale [:decision :selection-certificate
                                            :preference :live-c])))))

(deftest wire-3-refused-derivation-propagates-typed
  ;; retired with live C as a scored preference; preserved: a refused live-C
  ;; derivation is not silently substituted into the decision's C
  (let [fx (route/fixture)
        r (route/decide fx 1 {:live-c {:sources {:wholeness {:refusal {:kind :source-missing
                                                                       :path "/nonexistent"}}
                                                 :missions []
                                                 :stars {:value {:capabilities {}}}}}})]
    (is (= (get-in (route/decide fx) [:decision :selection-law :posterior])
           (get-in r [:decision :selection-law :posterior])))
    (is (every? #(= :certified (:status %))
                (get-in r [:decision :selection-certificate :preference-receipts]))
        "the decision's C is the certified shape preference, whatever live C did")))

(deftest wire-live-c-projects-real-mission-tokens
  (let [d (lc/derive-live-c (lc/read-sources))]
    (is (seq (:want d)) "the real corpus derives a non-empty live C")
    (is (every? keyword? (:want d))
        "live-C tokens are (namespaced) keywords, never [target token] pairs")
    (let [pair [tick-1-target (first tick-1-want)]
          matching (assoc d
                          :want #{(keyword "alive" (name tick-1-target))}
                          :weights {(keyword "alive" (name tick-1-target)) 1})]
      (is (= #{pair} (:want (lc/cascade-spec matching #{pair} #{pair})))
          "mission token projects through that mission's own declared want")))
  ;; retired: the decision's target-token qualification scheme; preserved:
  ;; the decision still produces a choice whatever live C derives
  (let [r (route/decide (route/fixture))]
    (is (some? (get-in r [:decision :action])))
    (is (seq (posterior-of r)) "candidates were scored")))

(deftest real-candidates-have-no-empty-sibling-and-keep-their-own-receipts
  ;; retired: construction receipts and lane null comparisons. Preserved:
  ;; every compared policy is a real, non-empty arrangement carrying its
  ;; own identity and debt, and nothing is dropped from a scorable family.
  (let [r (route/decide (route/fixture))
        candidates (keys (posterior-of r))]
    (is (seq candidates))
    (is (every? #(seq (get-in % [:precedence :co-apply :units])) candidates))
    (is (every? #(= (:policy-key %) (prefix-admission/candidate-key %)) candidates))
    (is (every? #(= (:id %) (get-in % [:interpretation-debt :policy-id])) candidates))
    (is (empty? (get-in r [:decision :selection-certificate :family-failures])))))

(deftest all-declined-family-is-a-recorded-abstention
  (let [fx (route/fixture)]
    (doseq [[label r]
            [[:no-reading (route/decide-field (route/field {"M-a" nil}) {"M-a" 1} {})]
             [:every-policy-unscorable
              (with-redefs [shape-g/score-policy (refusing-policies (constantly true))]
                (route/decide fx))]]]
      (is (= :abstained (get-in r [:decision :status])) (str label))
      (is (= :no-acting-cascade-candidate (get-in r [:decision :reason])) (str label))
      (is (empty? (posterior-of r)) (str label))
      (is (= 0 (:selector-calls r)) (str label))
      (is (every? #(and (:target %) (= :no-policy-family (:kind %)) (seq (:failures %)))
                  (get-in r [:decision :refusals]))
          (str label)))))

(deftest class-unknown-target-is-declined-not-a-decision-refusal
  ;; retired: the class scorer's :class-unknown-no-scalar-g. Preserved: a
  ;; target none of whose policies can be scored is declined and counted
  ;; while selection proceeds over the scorable target; alone, it abstains.
  (let [fld (route/field {"M-a" p-ab "M-u" ["p/b" "p/c"]})
        unscorable #(clojure.string/starts-with? (str %) "M-u/")
        r (with-redefs [shape-g/score-policy (refusing-policies unscorable)]
            (route/decide-field fld {"M-a" 1 "M-u" 1} {}))
        alone (with-redefs [shape-g/score-policy (refusing-policies unscorable)]
                (route/decide-field (route/field {"M-u" ["p/b" "p/c"]}) {"M-u" 1} {}))]
    (is (= "M-a" (get-in r [:decision :action :target])))
    (is (= 1 (:selector-calls r)))
    (is (= #{"M-a"} (set (map :target (keys (posterior-of r))))))
    (is (some #(and (= "M-u" (:target-id %)) (= :fixture-unscorable (:reason %)))
              (get-in r [:decision :selection-certificate :family-failures])))
    (is (= :abstained (get-in alone [:decision :status])))
    (is (= 0 (:selector-calls alone)))))

(deftest all-declined-family-with-admission-refusals-abstains
  ;; one target has no reading, the other no scorable policy: abstain with
  ;; both refusals in field order, never throw
  (let [fld (route/field {"M-a" nil "M-u" ["p/b" "p/c"]})
        r (with-redefs [shape-g/score-policy (refusing-policies (constantly true))]
            (route/decide-field fld {"M-a" 1 "M-u" 1} {}))]
    (is (= :abstained (get-in r [:decision :status])))
    (is (= ["M-a" "M-u"] (mapv :target (get-in r [:decision :refusals]))))
    (is (= [[:no-policy-family :no-current-target-reading]
            [:no-policy-family :policy-scoring-refused]]
           (refusal-kinds r)))
    (is (= :fixture-unscorable (get-in r [:decision :refusals 1 :failures 0 :reason])))))

(deftest every-accounted-target-lands-in-the-field
  ;; production repair (Q5 report packet): a target the assembly accounted
  ;; for but with no source row is a counted failure, not silently absent;
  ;; a field with no target at all abstains typed (keeping a field-level
  ;; outer refusal) instead of failing the gate on an empty refusal list
  (let [fx (route/fixture)
        base (route/assembled fx 1)
        with-rowless (update base :refusals conj {:target "M-rowless" :kind :no-admitted-interpretation})
        r (wm-cd/cascade-decision with-rowless (route/opts fx {}))
        outer {:target {:absent :outer-task-not-selected} :kind :outer-task-selection-refused}
        empty-field (wm-cd/cascade-decision {:problems [] :refusals [outer] :target-sources []}
                                            (route/opts fx {}))
        bare (wm-cd/cascade-decision {:problems [] :refusals [] :target-sources []}
                                     (route/opts fx {}))]
    (is (= :abstained (get-in r [:decision :status])))
    (is (nil? (get-in r [:decision :action])))
    (is (some #(and (= "M-rowless" (:target %))
                    (= :no-policy-family (:kind %)))
              (get-in r [:decision :refusals])))
    (is (= [outer] (mapv #(select-keys % [:target :kind]) (get-in empty-field [:decision :refusals])))
        "the outer selector's refusal is the empty field's abstention")
    (is (= [[:no-policy-family :no-enumerated-target]] (refusal-kinds bare)))))

(deftest selection-refusal-is-the-decisions-typed-refusal
  ;; production repair (Q5 proposal/refusal packet): the selection law's
  ;; typed refusal leaves the decision as "cascade decision refused", which
  ;; the runner reads as the tick's typed abstention; anything else thrown
  ;; passes through unchanged
  (let [fx (route/fixture)
        unread {:steps [] :read [] :unread [] :dir-status {:absent :no-flights-dir}}
        calls (atom 0)
        select policy/select-action-cascades
        e (with-redefs [policy/select-action-cascades
                        (fn [& args] (swap! calls inc) (apply select args))]
            (try (wm-cd/cascade-decision (route/assembled fx 1)
                                         (route/opts fx {:conditioning-steps unread}))
                 nil
                 (catch clojure.lang.ExceptionInfo e e)))
        other (try (with-redefs [family-selection/select-over-families
                                 (fn [& _] (throw (ex-info "something else" {:kind :x})))]
                     (wm-cd/cascade-decision (route/assembled fx 1) (route/opts fx {})))
                   nil
                   (catch clojure.lang.ExceptionInfo e e))]
    (is (= 1 @calls) "the one selector call ran and refused")
    (is (= "cascade decision refused" (ex-message e)))
    (is (= :free-energy-not-supplied (:kind (ex-data e))))
    (is (= :historical-prefix-f (:missing (ex-data e))))
    (is (= route/target (:target (ex-data e))))
    (is (= :free-energy-not-supplied (get-in (ex-data e) [:refusal :kind])))
    (is (= "something else" (ex-message other)) "an unrelated failure is not dressed as a refusal")))
