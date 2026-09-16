(ns futon2.vm.tick-001-s03-r6-test
  "Virtual War Machine tick 1, step 3 (R6 candidate action space).

  Real-machine REQUIREMENT test (VM-PROTOCOL.md, p4ng b8e7751): asserts what
  the model requires of the real tick's R6 node on THIS tick's inputs
  (vm/tick-001/02-R1.edn belief, 03-R6.edn candidate space), calling the real
  code. Failing assertions are the node's open requirements, listed in
  03-R6.edn :requirements-open.

  Real R6 code under test:
    - route-tagged :R6 function futon2.aif.policy/select-action
      (war_machine.clj:6570);
    - cascade candidate construction futon2.aif.receipt-construction/construct
      (receipt_construction.clj:547) and futon2.aif.cascade-policy/organise
      (cascade_policy.clj:117).

  Requirement source: the tick's candidate action space must contain the
  candidate cascades the model constructs (C0 empty, C1 test-first, C2
  fix-first, C3 fix-only; 03-R6.edn :computed :cascades), each pattern step
  carrying a guard and produces over the tick's tokens, with monotone-union
  transitions (P2/P10, R1's :q0-basis and :caution)."
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-policy :as cp]
            [futon2.aif.policy :as policy]
            [futon2.aif.receipt-construction :as rc]))

;; --- the tick's tokens, interpretations and cascades (03-R6.edn) ---------

(def q0
  #{:summary-without-total-repos-throws
    :active-repo-ratio-absent-default-is-0
    :coupling-density-reads-same-key-with-default
    :observe-empty-does-not-throw})

(def want
  [:summary-without-total-repos-observes-cleanly
   :active-repo-ratio-absent-default-is-0
   :test-covers-missing-total-repos])

(def steps
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
    :produces #{:test-covers-missing-total-repos}}})

(def cascades
  [[:C0-empty []]
   [:C1-test-first [:test-step-covering-missing-total-repos
                    :aif/structured-observation-vector]]
   [:C2-fix-first [:aif/placeholder-is-load-bearing
                   :test-step-covering-missing-total-repos
                   :aif/structured-observation-vector]]
   [:C3-fix-only [:aif/structured-observation-vector]]])

(defn- step-fires?
  [state step-id]
  (let [{:keys [needs forbids]} (get-in steps [step-id :guard])]
    (and (set/superset? state needs) (empty? (set/intersection state forbids)))))

(defn- simulate
  "P10 monotone-union transition fold: a step whose guard holds against the
  current state unions its produces into the state; no retraction."
  [precedence]
  (reduce (fn [state id]
            (if (step-fires? state id)
              (set/union state (get-in steps [id :produces]))
              state))
          q0
          precedence))

;; --- 1. organise: the real cascade organiser on this tick's space --------

(def repository
  {:patterns (set (keys steps))
   ;; authored reachability quoted from the patterns' own @how edges
   ;; (03-R6.edn :computed :retrieval citations)
   :stands-on #{[:aif/placeholder-is-load-bearing :aif/structured-observation-vector]}})

(def c2-precedence (second (nth cascades 2)))

(defn- organise**
  [previous selected precedence]
  (try
    {:ok (cp/organise previous selected repository {}
                      {:temperament {:id :r6-tick-001 :closure :selected-only
                                     :precedence precedence}
                       :acting-order-fn (fn [c] (vec (:precedence c [])))
                       :score-fn (fn [c] {:kind :token-coverage
                                          :established (simulate (:precedence c []))})})}
    (catch Exception e {:throw e})))

(deftest s03-r6
  (testing "REQUIRED: real organise organises the tick's candidate cascade with guards+produces intact"
    (let [{:keys [ok throw]} (organise** cp/first-attempt-cascade
                                         (set (keys steps))
                                         c2-precedence)]
      (is (nil? throw)
          (str "organise refused: " (when throw (.getMessage throw)) " "
               (when throw (pr-str (ex-data throw)))))
      (when ok
        (let [diff ok]
          (is (= (set (keys steps)) (:nodes diff))
              "carrier = the tick's three interpreted patterns (O1)")
          (is (= c2-precedence (:acting-order-after diff))
              "acting order after = the C2 precedence the tick authored")
          ;; the acting order's guards+produces fold from q0 to the want tokens
          (is (set/superset? (simulate (:acting-order-after diff)) (set want))
              "C2's real acting order establishes all three want tokens from q0 (P10)")))))

  ;; claude-4 review: a block asserting the fixture's own `simulate` over
  ;; hand-built cascades was removed. It called no real code, so it only
  ;; restated the roleplay.

  ;; --- 2. the R6 route function on cascade candidates -------------------

  (testing "REQUIRED: select-action (:R6 route) preserves cascade candidates and their guards"
    ;; efe/rank-actions returns candidates best-first; select-action takes the
    ;; first entry as best (policy.clj `best (first ranked-actions)`), so the
    ;; fixture must arrive ranked exactly as the real route contract says.
    (let [ranked (mapv (fn [[id prec]]
                         {:action {:kind :cascade-candidate :id id :precedence prec
                                   :steps (select-keys steps prec)}
                          :controller-score (if (set/superset? (simulate prec) (set want)) -1.0 -3.0)
                          :rank 1})
                       (reverse cascades))
          decision (try {:ok (policy/select-action ranked)}
                        (catch Exception e {:throw e}))]
      (is (nil? (:throw decision))
          (str "select-action threw on cascade candidates: "
               (when-let [t (:throw decision)] (.getMessage t))))
      (when-let [d (:ok decision)]
        (is (map? (:action d)) "a cascade candidate is chosen, not flattened to a keyword")
        (is (seq (get-in d [:action :precedence]))
            "the chosen candidate keeps its pattern precedence")
        (is (every? :guard (vals (get-in d [:action :steps])))
            "the chosen candidate keeps each pattern's guard over the tick tokens"))))

  (testing "REQUIRED: select-action abstains with :no-candidates when the space is empty"
    (is (= :no-candidates (:reason (policy/select-action [])))))

  ;; --- 3. the real cascade construction entry point on this tick --------

  ;; claude-4 review: a block asserting that `construct` refuses this tick was
  ;; removed. The refusal is the missing wiring, not a requirement; the
  ;; REQUIRED OPEN assertion below states what must be built.

  (testing "REQUIRED: real candidate-space builds the tick's candidate family C0-C3 from the belief"
    ;; RE-POINTED (claude-4-approved form of the former ill-posed assertion,
    ;; see 03-R6.edn :fix): before, this block called
    ;; receipt-construction/construct on an empty record, an entry point that
    ;; needs a packet-1 receipted find record and could never yield C0-C3.
    ;; After, it calls the real R6 constructor cascade-policy/candidate-space
    ;; on this tick's belief (q0, want, interpretations, repository, authored
    ;; precedences) and requires the SAME values: the family contains the
    ;; empty cascade plus C1-C3, each step keeping its guard and produces.
    (let [space (try {:ok (cp/candidate-space
                           {:q0 q0 :want want :interpretations steps
                            :repository repository
                            :precedences (mapv second (rest cascades))})}
                     (catch Exception e {:throw e}))]
      (is (nil? (:throw space))
          (str "candidate-space refused: "
               (when-let [t (:throw space)] (.getMessage t)) " "
               (when-let [t (:throw space)] (pr-str (ex-data t)))))
      (when-let [sp (:ok space)]
        (let [cands (:candidates sp)]
          (is (= 4 (count cands)) "C0 plus the three authored candidates, nothing else")
          (is (= [] (:precedence (first cands)))
              "the empty cascade is always first")
          (is (= (second (nth cascades 1)) (:precedence (nth cands 1)))
              "C1 test-first order preserved")
          (is (= (second (nth cascades 2)) (:precedence (nth cands 2)))
              "C2 fix-first order preserved")
          (is (= (second (nth cascades 3)) (:precedence (nth cands 3)))
              "C3 fix-only order preserved")
          (is (every? :guard (mapcat #(vals (:steps %)) cands))
              "every step keeps its guard over the tick tokens")
          (is (every? :produces (mapcat #(vals (:steps %)) cands))
              "every step keeps its produces")
          (is (every? map? (map :organised cands))
              "every candidate is organised through the real organise (O1-O4)")
          (is (= (:acting-order (nth cands 2))
                 (get-in (nth cands 2) [:organised :acting-order-after]))
              "the organised diff's acting order equals the fold's acting order")
          (is (set/superset? (:established (nth cands 1)) (set want))
              "C1's real kernel fold establishes all three want tokens")
          (is (set/superset? (:established (nth cands 2)) (set want))
              "C2's real kernel fold establishes all three want tokens")
          (is (not (contains? (:established (nth cands 3))
                              :test-covers-missing-total-repos))
              "C3 never establishes the test token")
          (is (= 1 (:coverage (nth cands 2)))
              "C2's coverage over the want signature is exactly 1")))))

  (testing "REQUIRED: default precedence derivation enumerates the interpreted subsets through the real laws"
    (let [space (try {:ok (cp/candidate-space
                           {:q0 q0 :want want :interpretations steps
                            :repository repository})}
                     (catch Exception e {:throw e}))]
      (is (nil? (:throw space))
          (str "default candidate-space refused: "
               (when-let [t (:throw space)] (.getMessage t))))
      (when-let [sp (:ok space)]
        (is (= 8 (count (:candidates sp)))
            "empty cascade plus one candidate per non-empty subset of the three interpretations")
        (is (= :not-performed-here (:retrieval (:rules sp)))
            "retrieval is explicitly recorded as not performed here (still open)")))))
