(ns futon2.aif.outer-canonical-g-frontier-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-policy :as policy]
            [futon2.aif.cascade-shape-g :as shape]
            [futon2.aif.outer-canonical-g :as sut]))

(defn- action [policy-id target units]
  (let [ids (mapv #(vector target :occurrence %) (range units))]
    {:kind :cascade-candidate :id policy-id :target target :cascade-digest (str "digest-" policy-id)
     :precedence {:co-apply
                  {:units ids :descent (mapv vector ids (rest ids))
                   :patterns (into {} (map (fn [id]
                                             [id (assoc (policy/token-interpretation
                                                         id {:guard {:needs #{} :forbids #{}}
                                                             :produces #{[target :done (last id)]}})
                                                        :pattern-id (str "pattern-" (last id))
                                                        :theta 1
                                                        :theta-record {:status :no-recorded-trials})]) ids))}}}))

(defn- entry [policy-id target units]
  (let [a (action policy-id target units)
        universe (into #{} (mapcat :produces (vals (get-in a [:precedence :co-apply :patterns]))))]
    {:policy-id policy-id :target-id target :action a
     :class-emission {:model (shape/class-emission-model target universe universe 10 :focused
                                                        {:status :synthetic
                                                         :source "frozen focus target class"
                                                         :focus {:status :frozen-focus
                                                                 :as-of "2026-10-23T14:12:32.782772218Z"}})}}))

;; Projection of b2r's read shape: two retained action candidates, unit counts
;; 8 and 19 (the live menu's first two), and every theta record is
;; {:status :no-recorded-trials}.  The 18 MB run record is deliberately not a
;; test fixture, so this remains hermetic in a worktree.
(defn- live-shaped-menu []
  {:status :sealed :menu-pin {:path "wm://sealed-outer-policy-menu-v1" :sha256 "b2r-projection"}
   :entries [(entry "M-self-improvement-loop/policy-1bbcf6a06eb2766b5e77"
                    "M-self-improvement-loop" 8)
             (entry "M-stack-geometry/policy-c7d7518e1e185c2e2318"
                    "M-stack-geometry" 19)]})

(defn- reverse-objective-order [scored]
  ;; The production record's gamma/lnE tie.  These two retained-shape policies
  ;; get distinct hypothetical lnE values only to construct O2's counterexample:
  ;; objective = G - lnE reverses this canonical G ordering.
  (let [[a b] (take 2 (sort-by :G scored))]
    [(assoc a :objective (+ (:G a) 10.0))
     (assoc b :objective (- (:G b) 10.0))]))

(deftest b2r-frontier-is-sealed-diagnostic-and-can-disagree-with-objective
  (let [menu (live-shaped-menu)
        selected "M-stack-geometry/policy-c7d7518e1e185c2e2318"
        frontier (sut/produce menu)
        scored (filterv #(= :scored (:status %)) (:entries frontier))
        [canonical-first canonical-second] (sort-by :G scored)
        [objective-first objective-second] (sort-by :objective
                                                    (reverse-objective-order
                                                     [canonical-first canonical-second]))]
    (testing "the actual retained candidate shape supplies the shared frontier"
      (is (= :produced (:status frontier)))
      (is (= {:horizon 10 :rule :max-field-unit-count-clamped :limit 10
              :unit-counts {"M-self-improvement-loop/policy-1bbcf6a06eb2766b5e77" 8
                            "M-stack-geometry/policy-c7d7518e1e185c2e2318" 19
                            }}
             (:horizon-choice frontier)))
      (is (= 2 (count (:class-model-ids frontier))))
      (is (string? (get-in frontier [:frontier-pin :sha256])))
      (is (= :compareAtHorizonN (get-in frontier [:comparison :lean-reduction]))))
    (testing "the O2 counterexample is recorded but not consumed"
      (is (not= (:policy-id canonical-first) (:policy-id objective-first)))
      (is (not= (:policy-id canonical-second) (:policy-id objective-second)))
      ;; O3-1 must leave the existing objective-selected policy and menu shape alone.
      (is (= "M-stack-geometry/policy-c7d7518e1e185c2e2318" selected))
      (is (= menu (live-shaped-menu))))))

(deftest missing-class-model-is-a-retained-unscored-refusal
  (let [menu (live-shaped-menu)
        policy-id (:policy-id (first (:entries menu)))
        frontier (sut/produce (update menu :entries
                                      #(assoc % 0 (update (first %) :class-emission dissoc :model))))
        refusal (first (filter #(= policy-id (:policy-id %)) (:entries frontier)))]
    (is (= :refused (:status refusal)))
    (is (= :class-emission-model-unavailable (:reason refusal)))
    (is (nil? (:G refusal)))
    (is (nil? (:rank refusal)))
    (is (= (get-in menu [:entries 0 :action :cascade-digest])
           (get-in refusal [:action-pin :cascade-digest])))))
