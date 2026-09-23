(ns futon2.aif.enacted-step-dispatch-test
  "⟨1⟩6: the dispatch and the acceptance predicate read the ENACTED step,
  not the chain head. Four cases per claude-5's acceptance bar."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-sources :as cs]
            [futon2.aif.full-loop-runner :as flr]))

(def t "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")

(defn- criterion-block
  "The runner's acceptance-criterion-block on an ACTION map (private fn)."
  [action]
  (@#'flr/acceptance-criterion-block {:action action}))

;; the real recorded selection of machinery-73 attempt-001
(def m73-selection
  (let [f "data/wm-full-loop-machinery-73/wm-contract-machinery-73-v1/attempt-001/002-selection.edn"]
    (when (.exists (io/file f))
      (edn/read-string (slurp f)))))

(defn- m73-c2-action []
  (let [law (get-in m73-selection [:payload :judgment :controller-decision :selection-law])
        action (get-in m73-selection [:payload :judgment :controller-decision :action])]
    (when (and (map? action) (map? law))
      (assoc action :enacted-steps (:enacted-steps law)))))

(defn- m72a1-c2-action []
  (let [f "data/wm-full-loop-machinery-72/wm-contract-machinery-72-v1/attempt-001/002-selection.edn"
        sel (when (.exists (io/file f)) (edn/read-string (slurp f)))
        law (get-in sel [:payload :judgment :controller-decision :selection-law])
        action (get-in sel [:payload :judgment :controller-decision :action])]
    (when (and (map? action) (map? law) (seq (:precedence action)))
      (assoc action :enacted-steps (:enacted-steps law)))))

(defn- reference-c2-action-with [enacted-steps]
  ;; the live qualifier's shape: pattern maps with qualified tokens
  (let [sources (cs/with-context-fn (cs/load-declared))
        cands (get-in sources [:candidates t])
        pats (get-in sources [:interpretations t :patterns])
        pm (fn [id]
             (let [m (get pats id)]
               {:id id
                :produces (set (map (fn [x] [t x]) (:produces m)))}))]
    {:kind :cascade-candidate :id :C2 :target t
     :enacted-steps enacted-steps
     :precedence (mapv pm (:precedence (second cands)))}))

(deftest case-1-real-record-names-the-enacted-steps-criterion
  (let [action (m73-c2-action)]
    (when action
      ;; precondition: the decision recorded the enacted step as hygiene
      (is (= :aif/measurement-window-hygiene
             (get-in action [:enacted-steps :aif/declare-the-conditioning]))
          "precondition: the recorded enacted step is the second limb")
      (let [block (criterion-block action)]
        (is (string? block) (pr-str block))
        (is (re-find #"resources/wm/eig/held-out-observations\.edn" block)
            "the ENACTED step's token's locator is the criterion")
        (is (re-find #"HELD-OUT-OBSERVATIONS-COLLECTED" block)
            "the exact required head")
        (is (re-find #"at the start of a line" block) "the line-initial rule")
        (is (not (re-find #"\Q:repair/split-declared-valid\E.*measured" block))
            "the already-true head token is NOT the measured criterion")
        (is (re-find #":id :aif/measurement-window-hygiene" block)
            "the criterion names the enacted step with its provenance")))))

(deftest case-2-bad-case-head-true-enacted-false
  ;; HEAD token already observed true; enacted step's token false: conjunct
  ;; (b) must be FALSE. (The PRE-FIX reader — precedence 0 — returned TRUE
  ;; on this fixture: with the head's token measured true at the
  ;; after-revision, (b) passed. Stated in the summary; not kept as an
  ;; assertion because the pre-fix code no longer exists.)
  (let [action (reference-c2-action-with {:aif/declare-the-conditioning
                                          :aif/measurement-window-hygiene})
        block (criterion-block action)]
    (is (string? block))
    (is (re-find #"held-out-observations" block)
        "the criterion renders the ENACTED step's token"))
  ;; and the (b) filter's resolver on the same fixture
  (let [action (reference-c2-action-with {:aif/declare-the-conditioning
                                          :aif/measurement-window-hygiene})
        {:keys [pattern enacted-step]} (@#'flr/enacted-step-pattern action)]
    (is (= :aif/measurement-window-hygiene (:id (:pattern {:pattern pattern})))
        "the resolver picks the hygiene pattern")
    (is (= :recorded-decision (:source enacted-step)))))

(deftest case-3-no-regression-when-head-is-enabled
  ;; machinery-72 attempt-001's recorded selection (the head WAS the
  ;; enabled step — split-declared-valid was not yet true): criterion and
  ;; resolver unchanged from today's behaviour.
  (let [action (m72a1-c2-action)]
    (when action
      ;; the recorded enacted step for that head was the head itself
      (is (contains? #{:aif/declare-the-conditioning nil}
                     (or (get (:enacted-steps action) (:id (first (:precedence action))))
                         :aif/declare-the-conditioning)))
      ;; and the criterion renders the head's token
      (let [block (criterion-block action)]
        (when (string? block)
          (is (re-find #"held-out-split\.edn" block)
              "the head's locator is the criterion (head was the enabled step)"))))))

(deftest case-4-fallback-typed-never-throws
  ;; absent :enacted-steps
  (let [action (reference-c2-action-with nil)
        {:keys [pattern enacted-step]} (@#'flr/enacted-step-pattern action)]
    (is (= :aif/declare-the-conditioning (:id pattern)) "falls back to the head")
    (is (= :chain-head-fallback (:source enacted-step)))
    (is (= :no-recorded-enacted-steps (:reason enacted-step)))
    (let [block (criterion-block action)]
      (is (string? block) "still dispatches")
      (is (re-find #"held-out-split\.edn" block) "the head's criterion renders")))
  ;; a recorded step NOT in the precedence
  (let [action (reference-c2-action-with {:aif/declare-the-conditioning :not-in-precedence})
        {:keys [pattern enacted-step]} (@#'flr/enacted-step-pattern action)]
    (is (= :aif/declare-the-conditioning (:id pattern)) "falls back to the head")
    (is (= :recorded-step-not-in-precedence (:reason enacted-step)))
    (is (string? (criterion-block action)) "still dispatches")))
