(ns futon2.aif.outer-state-update-test
  "What remains of the durable state update after M-cascade-live-causal
  step 2b: the carried counts (advance-c, advance-gain-counts,
  advance-state-counts, the outer-state likelihood/posterior) are deleted
  with outer-score. Deleted tests: gain-counts-stay-positive,
  selected-status-learns-and-other-status-decays,
  state-learning-moves-belief, learned-c-matches-slice-3b (their subjects
  are deleted). What remains is the click count, the pinned-cascade replay
  check, and the retirement of the count keys."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.outer-state :as state]))

(def makeup
  {:schema :wm/outer-makeup-v1
   :makeup/sha256 (apply str (repeat 64 "a"))
   :c-kit {:focused 55/100 :related 35/100 :unrelated 5/100
           :stop-the-line 5/100 :not-yet-evaluated 0}
   :kappa 1
   :state-kit {:ready {:doable 1 :blocked 1}
               :not-ready {:doable 1 :blocked 3}
               :undetermined {:doable 1 :blocked 1}}
   :gain-kit {:none 1 :one 2 :several 3}
   :eta-c 1 :omega-c 9/10
   :eta-state 1 :omega-state 9/10
   :eta-gain 1 :omega-gain 9/10
   :default-reach 1/2
   :cascades [{:id :ready :patterns ["test"] :steps [:ready-gate]}]})

(def candidate {:target "M-task" :reach 1/2})

(def observation
  {:focus "M-focus"
   :relation (fn [_ _] :associated)
   :field [candidate]
   :facts {"M-task" {:status :ready :unblocks 1 :cone 1}}})

(def selection
  {:cascade-index 0
   :cascade-id :ready
   :steps [:ready-gate]
   :cascade-scores [0.0]
   :selection {:candidates [candidate] :selected candidate}})

(deftest init-state-carries-no-counts
  (testing "step 2b: the initializer writes no count keys"
    (let [init (state/init-state makeup)]
      (is (= 0 (:click-count init)))
      (is (not (contains? init :c)))
      (is (not (contains? init :state-counts)))
      (is (not (contains? init :gain-counts))))))

(deftest next-state-advances-the-click-count-and-drops-counts
  (testing "the state part of Lean step, minus the retired counts"
    (let [before (assoc (state/init-state makeup) :source :init
                        ;; a pre-2b durable state still carries these; the
                        ;; learned update drops them
                        :c {:focused 1} :state-counts {} :gain-counts {})
          after (state/next-state makeup before selection observation
                                  {:ending-class :related :observed-gain :one})]
      (is (= 1 (:click-count after)))
      (is (not (contains? after :source)))
      (is (not (contains? after :c)))
      (is (not (contains? after :state-counts)))
      (is (not (contains? after :gain-counts))))))

(deftest chain-variant-selection-replays-through-chosen-cascade
  ;; wm-click-loop-repair-bb1de665: held run 2026-11-13-ea3be8a8 stopped at
  ;; outer-selection (pending-steps-unrecoverable) because chain-variants
  ;; minted sha256(pr-str [base-digest :chain k]) while chosen-cascade
  ;; replays the digest from the carrier (name/readme-cascade/units/edges).
  ;; A chain row whose digest is not carrier-recomputable is unrecoverable
  ;; forever, so the state update must accept a carrier-pinned chain row.
  (let [unit {:pattern "futon3/library/task-selection/only-what-can-start-now.flexiarg"
              :sha256 (apply str (repeat 64 "b")) :index 0}
        carrier-digest (fn [entry]
                         (identity/sha256
                          (pr-str [(name (:id entry)) (:readme-cascade entry)
                                   (mapv (fn [u] [(:pattern u) (:sha256 u) (:index u)])
                                         (:units entry))
                                   (:edges entry)])))
        base (assoc {:id :start :readme-cascade 1 :units [unit] :edges []
                     :steps [:ready-gate]}
                    :digest nil)
        chain (-> base
                  (assoc :id :start--chain-2
                         :chain {:base :start :depth 2 :picks ["M-task" "M-other"]})
                  (assoc :digest (carrier-digest (assoc base :id :start--chain-2))))
        rows [(dissoc (assoc base :digest (carrier-digest base)) :chain)
              chain]
        chain-selection {:cascade-index 1
                         :cascade-id :start--chain-2
                         :cascade-digest (:digest chain)
                         :cascades rows
                         :steps [:ready-gate]
                         :selection {:candidates [candidate] :selected candidate}}
        before (assoc (state/init-state makeup) :source :init)
        after (state/next-state makeup before chain-selection observation
                                {:ending-class :related :observed-gain :one})]
    (is (= 1 (:click-count after))
        "a carrier-pinned chain selection must replay, not refuse")
    (is (not= (:digest (second rows))
              (:digest (first rows)))
        "the chain's carrier digest differs from its base's")))
