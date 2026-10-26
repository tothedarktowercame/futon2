(ns futon2.aif.outer-canonical-selection-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.outer-canonical-g :as frontier]
            [futon2.aif.outer-policy-evidence :as evidence]
            [futon2.aif.outer-policy-objective :as objective]
            [futon2.aif.outer-canonical-selection :as sut]
            [futon2.aif.selected-outer-cascade-policy :as carrier]
            [futon2.aif.selected-outer-cascade-policy-test]
            [futon2.aif.outer-policy-runtime-selection-test]))

(def fixture (edn/read-string (slurp "test/fixtures/outer-canonical-g/b5-input-fc464172.edn")))
(def menu (:sealed-menu fixture))
(def wiring "M-war-machine-wiring/policy-2629917c87a61978be8f")
(def bindings "M-recommendation-bindings/policy-7e8a0806fea29fa47914")
(def self-improvement "M-self-improvement-loop/policy-1bbcf6a06eb2766b5e77")
(def stack "M-stack-geometry/policy-c7d7518e1e185c2e2318")

(deftest b5-canonical-draw-replaces-the-objective-tie-only-for-choice
  (let [f (frontier/produce menu) first (sut/evaluate f menu 0.0) last (sut/evaluate f menu 0.9)]
    (is (= wiring (get-in first [:action :policy-id])))
    (is (= bindings (get-in last [:action :policy-id])))
    (is (= ["3a5228e7942579f3e0095ee4a902b1b08a0a9d0bf7a11c966c52bddbbb4e4475"
            "13c0068ddbc12266370e59d82812d9b10472a38831e7e61f0e9aef6a9e690e11"]
           (get-in first [:draw :candidates])))
    (is (= 0.0 (get-in first [:draw :u])))
    (is (= stack (get-in fixture [:policy-objective :selected :policy-id])))
    (is (not= stack (get-in first [:action :policy-id])))))

(deftest ln-e-changes-the-objective-winner-but-not-the-canonical-choice
  ;; Two evidence declarations that differ only in each policy's lnE flip the
  ;; old gamma*G-lnE winner; the canonical choice reads the menu's frontier
  ;; alone, so it is the same under both.
  (let [production (var-get (ns-resolve 'futon2.aif.selected-outer-cascade-policy-test
                                         'production-carriers))
        declaration (var-get (ns-resolve 'futon2.aif.selected-outer-cascade-policy-test
                                          'declaration))
        {:keys [menu]} (production)
        digests (mapv :cascade-digest (:entries menu))
        winner (fn [ln-es]
                 (get-in (objective/evaluate (evidence/join menu (declaration digests ln-es) [])
                                             menu)
                         [:selected :cascade-digest]))
        f (frontier/produce menu)
        a (sut/evaluate f menu 0.0)]
    (is (= (second digests) (winner [0.0 1.0])))
    (is (= (first digests) (winner [1.0 0.0])))
    (is (= :selected (:status a)))
    (is (= :canonical-G-min-over-scored-frontier (:basis a)))
    (is (= a (sut/evaluate (frontier/produce menu) menu 0.0)))))

(deftest refused-frontier-rows-never-enter-the-minimum
  (let [broken (update menu :entries
                       #(mapv (fn [e] (if (contains? #{wiring bindings} (:policy-id e))
                                        (update e :class-emission dissoc :model) e)) %))
        chosen (sut/evaluate (frontier/produce broken) broken 0.0)]
    (is (= self-improvement (get-in chosen [:action :policy-id])))
    (is (not (contains? #{wiring bindings} (get-in chosen [:action :policy-id]))))))

(deftest canonical-carrier-replays-the-frontier-choice
  (let [production (var-get (ns-resolve 'futon2.aif.selected-outer-cascade-policy-test
                                         'production-carriers))
        {:keys [menu evidence objective]} (production)
        f (frontier/produce menu)
        selection (sut/evaluate f menu 0.0)
        sealed (carrier/seal-canonical menu evidence objective f selection)
        wrong-entry (second (:entries menu))
        wrong-selection (assoc selection
                               :action {:type :advance-cascade-live-policy
                                        :target (:target-id wrong-entry)
                                        :policy-id (:policy-id wrong-entry)
                                        :cascade-digest (:cascade-digest wrong-entry)}
                               :selected {:policy-id (:policy-id wrong-entry)})
        forged (carrier/seal-canonical menu evidence objective f wrong-selection)
        forged (assoc forged :carrier-pin (carrier/carrier-pin forged))]
    (is (nil? (carrier/verify sealed)))
    (is (= :selected (get-in (carrier/decision sealed) [:decision :status])))
    (is (= :selected-policy-canonical-replay (carrier/verify forged)))))

(deftest select-live-activates-the-canonical-carrier
  ;; lnE [0.0 1.0] makes the old objective pick the second menu entry; the
  ;; receipt's choice, its :meta-selection and the carrier must all name the
  ;; canonical choice instead, with the objective kept only as evidence.
  (let [runtime-var #(var-get (ns-resolve 'futon2.aif.outer-policy-runtime-selection-test %))
        as-of "2026-10-22T12:34:56Z"
        receipt ((runtime-var 'run-live)
                 ((runtime-var 'explicit-provider) [0.0 1.0] [])
                 {:selection-as-of as-of
                  :focus-inputs {}
                  :focus-context {:as-of as-of :status :retained :focus "WM"}
                  :focus-classify-fn (fn [_ _ _ target _] {:target target :class :focus})})
        policy (:policy receipt)
        sealed (:selected-outer-cascade-policy policy)
        canonical (:canonical-policy-selection policy)]
    (is (= :selected (:status policy)))
    (is (= :canonical-G-frontier (:basis-kind sealed)))
    (is (nil? (carrier/verify sealed)))
    ;; The receipt's action is the sealed menu entry's action, keyed :id.
    (is (= (:policy-id (:action canonical)) (get-in receipt [:action :id])))
    (is (= (:cascade-digest (:action canonical)) (get-in receipt [:action :cascade-digest])))
    (is (not= (get-in policy [:policy-objective :selected :cascade-digest])
              (get-in receipt [:action :cascade-digest])))
    (is (= (:action canonical) (get-in policy [:meta-selection :action])))
    (is (= :canonical-G-frontier (get-in policy [:meta-selection :selection-basis :kind])))
    (is (= :selected (get-in policy [:policy-objective :status])))
    ;; Q8a: the canonical route binds the Lean frontier statement — the
    ;; receipt's :q8-frontier is a sealed :wm/q8-outer-frontier-v1 carrier,
    ;; not the stub.
    (is (= :wm/q8-outer-frontier-v1 (get-in receipt [:q8-frontier :schema])))))
