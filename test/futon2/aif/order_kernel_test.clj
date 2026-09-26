(ns futon2.aif.order-kernel-test
  "M-wm-wiring row 4: rank-cascade-actions reads the construction receipt's
  :order (construction/containment-order). On a chain with no precedence
  violations it scores the order's own linear order, which is the precedence,
  so G is exactly the list kernel's (the Lean statement is
  coApplyKernel_eq_cascadeKernel_of_chain, DarkTower/WarMachine/Proof2/
  CoApplicationKernel.lean, mathlib4 69c2432f2b). Violations, a refused or
  absent order keep the list kernel with a typed reason on the entry's
  :order-use; a non-chain is scored by the co-application kernel
  (cascade-model-manifest/co-apply-kernel, step 14, WM-COAPPLY-I), labelled
  {:order :co-application}."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.construction :as construction]
            [futon2.aif.efe :as efe]))

;; A produces :x; B needs :x and produces :w; C produces :y (independent)
(def pat
  {:A {:id :A :theta 4/5 :produces #{:x}
       :guard {:status :interpreted :clauses [{:status :interpreted :present #{} :absent #{:x}}]}}
   :B {:id :B :theta 4/5 :produces #{:w}
       :guard {:status :interpreted :clauses [{:status :interpreted :present #{:x} :absent #{:w}}]}}
   :C {:id :C :theta 4/5 :produces #{:y}
       :guard {:status :interpreted :clauses [{:status :interpreted :present #{} :absent #{:y}}]}}})

(def interp
  ;; the same patterns in the interpretation shape containment-order reads
  {:A {:id :A :guard {:needs #{}} :produces #{:x}}
   :B {:id :B :guard {:needs #{:x}} :produces #{:w}}
   :C {:id :C :guard {:needs #{}} :produces #{:y}}})

(defn- candidate [id ids & [receipt-order]]
  (cond-> {:kind :cascade-candidate :id id :precedence (mapv pat ids)}
    (not= receipt-order :absent)
    (assoc :construction-receipt
           {:order (or receipt-order
                       (construction/containment-order {:patterns (mapv interp ids) :precedence ids}))})))

(def candidates
  [(candidate :chain [:A :B])                 ; A above B, precedence agrees
   (candidate :violating [:B :A])             ; precedence reverses A above B
   (candidate :independent [:A :C])           ; no descent: not a chain
   (candidate :no-order [:A :B] :absent)      ; pre-a98f5879 receipt shape
   (candidate :cyclic [:A :B] {:kind :cyclic-containment})])

(defn- rank []
  (efe/rank-cascade-actions {:cascade-belief {#{} 1}}
                            candidates
                            {:horizon-steps 2 :cascade-spec {:want #{:w} :lam 1 :mu 1 :evidence #{} :zeroed #{}}}))

(defn- by-id [ranked] (into {} (map (juxt :cascade-id identity)) ranked))

(deftest the-chain-scores-its-order-and-equals-the-list-kernel
  (let [r (by-id (rank))]
    (is (= {:order :chain} (get-in r [:chain :order-use])))
    (is (= (mapv pat [:A :B]) (:precedence (efe/order-use (first candidates))))
        "the order's one linear extension is the precedence")))

(deftest every-list-g-is-unchanged
  ;; fixture: [[id G] ...] from efe.clj at futon2 f8e766a5, before row 4,
  ;; captured on a prepended classpath before the change. Every candidate
  ;; the list kernel still scores keeps its G to the bit; the non-chain
  ;; :independent is now scored by the co-application kernel, so its G moves
  ;; (step 14) and is excluded here, asserted different.
  (let [before (into {} (edn/read-string (edn/read-string (slurp "test/fixtures/order-kernel/g-before@futon2-f8e766a5.edn"))))
        now (into {} (mapv (juxt :cascade-id :G-efe) (rank)))]
    (is (= (dissoc before :independent) (dissoc now :independent)))
    (is (not= (:independent before) (:independent now)))))

(deftest the-list-kernel-cases-carry-their-reason
  (let [r (by-id (rank))]
    (is (= {:order-not-used {:precedence-violations 1}} (get-in r [:violating :order-use])))
    (is (= {:order :co-application} (get-in r [:independent :order-use])))
    (is (= {:order {:absent :no-order-on-receipt}} (get-in r [:no-order :order-use])))
    (is (= {:order-not-used {:refused :cyclic-containment}} (get-in r [:cyclic :order-use])))))

(deftest a-precedence-that-cannot-be-mapped-is-typed-not-thrown
  (is (= {:order-not-used :units-not-mapped-to-precedence}
         (:meta (efe/order-use (candidate :dup [:A :B] {:units [] :descent []
                                                         :precedence-violations :units-not-mapped-to-precedence}))))))
