(ns futon2.aif.enacted-step-test
  "PROOF-wm-works ⟨1⟩6: the record names the action the machine would
  actually take. The marginal's key stays the chain head (the law's and
  tie-break's identity); :enacted-steps records the first-enabled step
  alongside it, each labelled."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-sources :as cs]
            [futon2.aif.policy :as policy]))

(def t "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")

(defn- reference-action [which]
  ;; the live qualifier's shape: pattern maps with qualified tokens
  (let [sources (cs/with-context-fn (cs/load-declared))
        cands (get-in sources [:candidates t])
        cand (case which :C1 (first cands) (second cands))
        pats (get-in sources [:interpretations t :patterns])
        pm (fn [id]
             (let [m (get pats id)]
               {:id id
                :guard {:clauses [{:present (set (map (fn [x] [t x]) (get-in m [:guard :needs])))
                                   :absent (set (map (fn [x] [t x]) (get-in m [:guard :forbids])))}]}
                :produces (set (map (fn [x] [t x]) (:produces m)))}))]
    {:kind :cascade-candidate :id which :target t
     :precedence (mapv pm (:precedence cand))}))

(deftest head-is-the-marginal-key-and-stays-so
  ;; the existing key is the chain head — unchanged by this work
  (let [a (reference-action :C2)
        head (@#'policy/cascade-first-action a)]
    (is (= :aif/declare-the-conditioning (:id head)))))

(deftest enacted-step-moves-down-the-chain
  ;; with split-declared-valid PRESENT in the state, the head's guard
  ;; (forbids it) fails and the enacted step is the second limb
  (let [a (reference-action :C2)
        state-with #{[t :admission/task-stated] [t :repair/split-declared-valid]}
        state-without #{[t :admission/task-stated]}]
    (is (= :aif/measurement-window-hygiene (policy/enacted-step-of a state-with))
        "head completed: the next limb is the enacted step")
    (is (= :aif/declare-the-conditioning (policy/enacted-step-of a state-without))
        "head not completed: the head IS the enacted step")))

(deftest enacted-steps-recorded-on-the-selection-law
  ;; the decision record carries :enacted-steps keyed by the chain head,
  ;; alongside (never replacing) :action-marginal
  (let [c2 (reference-action :C2)
        c1 (reference-action :C1)
        ranked [{:action c1 :controller-score 2.99}
                {:action c2 :controller-score 0.59
                 :prediction {:initial-belief {#{[t :admission/task-stated]
                                                [t :repair/split-declared-valid]} 1}}}]
        decision (policy/select-action-cascades
                  ranked
                  {:beta 1
                   ;; no habit file: cold-start neutral for both
                   :cascade-habit-path "/tmp/nonexistent-habit.edn"})]
    (is (map? decision) (pr-str decision))
    (is (contains? (get-in decision [:selection-law]) :enacted-steps)
        "the selection law records the enacted steps")
    (is (= :aif/measurement-window-hygiene
           (get-in decision [:selection-law :enacted-steps :aif/declare-the-conditioning]))
        (pr-str (get-in decision [:selection-law :enacted-steps])))
    ;; the marginal's key is unchanged: the chain head
    (is (contains? (get-in decision [:selection-law :action-marginal])
                   (first (:precedence c2))))))

;; Typed absences (claude-10, 2026-09-25; fad94c89 found the nils): the step
;; is an id, or a reason there is none, never nil standing in for one.
(deftest no-step-is-a-typed-absence-never-nil
  (let [a (reference-action :C2)]
    (is (= {:absent :no-scoring-belief} (policy/enacted-step-of a nil))
        "the entry carried no prediction belief")
    (is (nil? (policy/enacted-step-of {:kind :cascade-candidate :precedence []} #{}))
        "no precedence: not a row, no step")))

(deftest a-belief-enabling-exactly-one-pattern-records-that-step
  ;; the bad case: a real step must not be turned into an absence
  (let [p {:id :p/only :guard {:status :interpreted
                               :clauses [{:status :interpreted :present #{:x} :absent #{:y}}]}
           :produces #{:y}}
        a {:kind :cascade-candidate :id :one :precedence [p]}]
    (is (= :p/only (policy/enacted-step-of a #{:x})))
    (is (= {:absent :no-enabled-step} (policy/enacted-step-of a #{})))))

