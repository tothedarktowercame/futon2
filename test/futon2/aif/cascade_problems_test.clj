(ns futon2.aif.cascade-problems-test
  "SPEC-flat-removal-and-cascade-decision H2: per-target cascade problem
  assembly. Targets are mission/ticket identities; every target lands in
  exactly one of :problems / :refusals; a missing input is a typed refusal
  in the fixed order (:universe-not-admitted, :no-admitted-interpretation,
  :want-not-declared, :no-constructed-candidate, :beta-not-declared); a
  missing :horizon-steps refuses all targets; a fully supplied target
  assembles a problem the REAL cascade-lane accepts. Tick 1's inputs
  (vm/tick-001/01..07) are the fixture."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-problems :as cp]
            [futon2.report.war-machine :as wm]))

(def target :wm-tick-001-observation-crash)

(def universe
  {:summary-without-total-repos-throws true
   :active-repo-ratio-absent-default-is-0 true
   :coupling-density-reads-same-key-with-default true
   :observe-empty-does-not-throw true
   :test-covers-missing-total-repos false
   :summary-without-total-repos-observes-cleanly :unknown})

(def interpretations
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
     :produces #{:test-covers-missing-total-repos}}}})

(def want
  [:summary-without-total-repos-observes-cleanly
   :active-repo-ratio-absent-default-is-0
   :test-covers-missing-total-repos])

(def receipt
  {:kind :construction-receipt
   :moves [:interpret :order]
   :family-searched 3
   :coverage 1})

(def candidates
  [{:precedence [:test-step-covering-missing-total-repos
                 :aif/structured-observation-vector]
    :construction-receipt receipt}
   {:precedence [:aif/placeholder-is-load-bearing
                 :test-step-covering-missing-total-repos
                 :aif/structured-observation-vector]
    :construction-receipt receipt}
   {:precedence [:aif/structured-observation-vector]
    :construction-receipt receipt}])

(def full-sources
  {:universes {target universe}
   :interpretations {target interpretations}
   :wants {target want}
   :candidates {target candidates}
   :horizon-steps 3
   :beta-by-context {:tick-1 {:beta 1}}
   :context-of (fn [_] :tick-1)})

(defn- kinds
  [result]
  (mapv :kind (:refusals result)))

(deftest h2-assemble
  ;; --- a fully supplied target assembles a problem the REAL cascade-lane
  ;; accepts (tick 1's inputs; route complete, no refusal).
  (let [{:keys [problems refusals]} (cp/assemble {:targets [target]
                                                  :sources full-sources})]
    (is (and (= 1 (count problems)) (= [] refusals))
        "a fully supplied target lands in :problems, not :refusals")
    (let [problem (first problems)]
      (is (= target (:target problem)))
      (is (= [receipt receipt receipt] (:construction-receipts problem))
          "each candidate's construction receipt travels with the problem")
      (let [lane (wm/cascade-lane (:cascade-problem problem))]
        (is (and (nil? (:refusal lane)) (nil? (:stopped-at lane)))
            "the real cascade-lane accepts the assembled problem end-to-end")
        (is (= [:R1 :R6 :R13 :R4 :R5 :R14 :R16 :R9] (mapv :node (:route lane)))
            "the lane runs the full node sequence on the assembled problem")))
    ;; the family always includes the empty cascade, first
    (is (= [] (first (get-in (first problems) [:cascade-problem :precedences])))
        "the target's empty cascade is always in the family, first")))

(deftest h2-refusals-in-order
  ;; 1 :universe-not-admitted — no universe at all (horizon still declared,
  ;; so the per-target order is exercised, not the refuse-all rule)
  (is (= [:universe-not-admitted]
         (kinds (cp/assemble {:targets [target]
                              :sources (dissoc full-sources :universes)}))))
  (is (= :universes (:missing (first (:refusals
                                       (cp/assemble {:targets [target]
                                                     :sources (dissoc full-sources
                                                                      :universes)})))))
      "the refusal records which source was absent")
  ;; 2 :no-admitted-interpretation — with the failing clause when given
  (let [r (cp/assemble {:targets [target]
                        :sources (-> full-sources
                                     (assoc-in [:interpretations target]
                                               {:patterns {}
                                                :refused {:clause
                                                          "D3 not approved: f⁺ over observed f⁻"}}))})]
    (is (= [:no-admitted-interpretation] (kinds r)))
    (is (= "D3 not approved: f⁺ over observed f⁻"
           (get-in (first (:refusals r)) [:clause]))
        "the failing clause from the source is included"))
  ;; 2 also fires for a candidate pattern with no admitted interpretation
  (is (= [:no-admitted-interpretation]
         (kinds (cp/assemble
                 {:targets [target]
                  :sources (assoc-in full-sources
                                     [:candidates target]
                                     (conj candidates
                                           {:precedence [:aif/pattern-never-interpreted]
                                            :construction-receipt receipt}))}))))
  ;; 3 :want-not-declared
  (is (= [:want-not-declared]
         (kinds (cp/assemble {:targets [target]
                              :sources (dissoc full-sources :wants)}))))
  ;; 4 :no-constructed-candidate — no candidates at all
  (is (= [:no-constructed-candidate]
         (kinds (cp/assemble {:targets [target]
                              :sources (dissoc full-sources :candidates)}))))
  ;; 4 — non-empty precedences without construction receipts are proposals,
  ;; not constructed cascades
  (is (= [:no-constructed-candidate]
         (kinds (cp/assemble
                 {:targets [target]
                  :sources (assoc-in full-sources [:candidates target]
                                     (mapv #(dissoc % :construction-receipt)
                                           candidates))}))))
  (is (= :construction-receipt
         (:missing (first (:refusals
                           (cp/assemble
                            {:targets [target]
                             :sources (assoc-in full-sources [:candidates target]
                                                (mapv #(dissoc % :construction-receipt)
                                                      candidates))})))))
      "a receipt-less candidate refuses with :missing :construction-receipt"))
  ;; 5 :beta-not-declared — no β for the target's context
  (is (= [:beta-not-declared]
         (kinds (cp/assemble {:targets [target]
                              :sources (dissoc full-sources :beta-by-context)}))))
  ;; every target lands in exactly one bucket
  (let [r (cp/assemble {:targets [target :M-other]
                        :sources full-sources})]
    (is (= 2 (+ (count (:problems r)) (count (:refusals r))))
        "every target lands in exactly one of :problems / :refusals")
    (is (= [:universe-not-admitted]
           (mapv :kind (filter (fn [x] (= :M-other (:target x))) (:refusals r))))))

(deftest h2-missing-horizon-refuses-all
  (let [r (cp/assemble {:targets [target :M-other :T-other]
                        :sources (dissoc full-sources :horizon-steps)})]
    (is (= [] (:problems r))
        "nothing is assembled without a declared horizon")
    (is (= [:horizon-not-declared :horizon-not-declared :horizon-not-declared]
           (kinds r))
        "a missing :horizon-steps refuses ALL targets")
    (is (every? #(= :horizon-steps (:missing %)) (:refusals r))
        "each refusal records which source was absent")))

(deftest h2-empty-sources-refuse-everything
  (let [targets [:M-foo :T-bar]
        r (cp/assemble {:targets targets :sources {}})]
    (is (= [] (:problems r))
        "with empty sources nothing is assembled")
    (is (= 2 (count (:refusals r)))
        "every target is refused (fixture list; the real list works the same)")
    (is (every? :kind (:refusals r))
        "each refusal is typed — the empty-sources tick abstains with the list")))
