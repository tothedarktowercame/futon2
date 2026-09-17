(ns futon2.aif.strategic-habit-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.habit-prior :as tactical]
            [futon2.aif.policy :as policy]
            [futon2.aif.strategic-habit :as habit]
            [futon2.aif.trace :as trace]))

(def decision
  "A cascade decision (SPEC flat-removal H4, 2026-09-17): the observed policy
   identity is the chosen cascade's enacted FIRST ACTING PATTERN."
  {:selection-law {:applied :cascade-selection-posterior}
   ;; the flat-path label that real select-action-cascades still records; the
   ;; habit must not depend on it (claude-4 review)
   :selection-boundary :strategic-recommendation
   :action {:kind :cascade-candidate :cascade-id "c-alpha"
            :precedence [:aif/placeholder-is-load-bearing
                         :aif/belief-state-operational-hypotheses]
            :construction-receipt {:cascade/id "c-alpha"}
            :interpretation-receipts [{:pattern :aif/placeholder-is-load-bearing}]}})

(def abstention
  {:status :abstained
   :refusals [{:target "M-x" :kind :beta-not-declared :missing :beta-by-context}]})

(def captured "2026-09-09T17:00:00Z")

(deftest explicit-flag-test
  (is (false? (habit/enabled? {} nil)))
  (is (true? (habit/enabled? {} "1")))
  (is (false? (habit/enabled? {:accumulate-strategic-habit? false} "1")))
  (is (true? (habit/enabled? {:accumulate-strategic-habit? true} nil))))

(deftest legacy-and-persistence-test
  (let [old (tactical/observe-action (tactical/initial-state)
                                     {:type :advance-mission :target "M-x"})
        output {:habit-prior-state old :decision decision}
        empty-side (habit/load-state (:strategic-habit-state output))
        off (habit/carry nil decision "tick-1" captured false)
        on (habit/carry nil decision "tick-1" captured true)
        legacy-trace (trace/trace-record output)
        new-trace (trace/trace-record (assoc output :strategic-habit-state on))]
    (is (= old (tactical/coerce-state old)))
    (is (= :forward-accumulation-not-started (:empty-reason empty-side)))
    (is (= :empty (:status empty-side)))
    (is (nil? off))
    (is (not (contains? legacy-trace :strategic-habit-state)))
    (is (= (pr-str old) (pr-str (:habit-prior-state legacy-trace))
           (pr-str (:habit-prior-state new-trace))))
    (is (= (dissoc legacy-trace :timestamp)
           (dissoc new-trace :timestamp :strategic-habit-state)))
    (is (= on (:strategic-habit-state new-trace)))
    (is (= 2 (:version on)))
    (is (= :strategic (:grain on)))
    (is (= 1.0 (:alpha on)))
    (is (= captured (:captured-at on)))
    ;; the observed policy id is the FIRST acting pattern of the chosen cascade
    (is (= {(str (first (:precedence (:action decision)))) 1} (:counts on)))
    (is (= :strategic (get-in on [:events "tick-1" :grain])))
    (is (= on (habit/carry on nil nil nil false)))
    (is (= on (habit/carry on decision "tick-1" captured true)))
    (is (= 2 (get-in (habit/carry on decision "tick-2" captured true)
                     [:counts (str (first (:precedence (:action decision))))])))))

(deftest abstention-observes-nothing-test
  (let [state (habit/carry nil decision "tick-1" captured true)]
    ;; an abstention enacts nothing: the store is returned unchanged
    (is (= state (habit/carry state abstention "tick-2" captured true)))
    ;; and an empty store stays empty — no fabricated policy observation
    (is (= :empty (:status (habit/carry nil abstention "tick-1" captured true))))))

(deftest refuses-insufficient-or-wrong-grain-test
  (doseq [d [(assoc-in decision [:action :precedence] [])
             (update decision :action dissoc :precedence)
             (assoc decision :selection-law {:applied :controller-head})]]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"insufficient"
                         (habit/carry nil d "tick-1" captured true))))
  (is (thrown? clojure.lang.ExceptionInfo
               (habit/load-state (tactical/initial-state))))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"nonempty"
                       (habit/require-promotable nil)))
  (let [state (habit/carry nil decision "tick-1" captured true)]
    (is (= state (habit/require-promotable state)))
    ;; a different cascade whose first acting pattern differs is a different
    ;; policy observation for the same event id: refuse
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"conflicting"
                         (habit/carry state
                                      (assoc-in decision
                                                [:action :precedence 0]
                                                :aif/other-pattern)
                                      "tick-1" captured true)))))

(deftest real-cascade-decision-is-observed-test
  ;; claude-4 review: the fixture above was hand-built; this one is the real
  ;; select-action-cascades output, which the old boundary check refused
  (let [cand (fn [id prec g]
               {:action {:kind :cascade-candidate :cascade-id id :precedence prec
                         :construction-receipt {:cascade/id id}
                         :interpretation-receipts (mapv (fn [p] {:pattern p}) prec)}
                :controller-score g})
        real (policy/select-action-cascades
              [(cand "c-empty" [] 13.1)
               (cand "c-fix" [:aif/placeholder-is-load-bearing] 11.4)]
              {:beta 1.0})
        on (habit/carry nil real "tick-1" captured true)]
    (is (= {(str :aif/placeholder-is-load-bearing) 1} (:counts on)))))
