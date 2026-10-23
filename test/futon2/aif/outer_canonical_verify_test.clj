(ns futon2.aif.outer-canonical-verify-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.outer-canonical-g :as frontier]
            [futon2.aif.outer-canonical-selection :as selection]
            [futon2.aif.selected-outer-cascade-policy :as carrier]
            [futon2.aif.selected-outer-cascade-policy-test]))

(defn- production []
  ((var-get (ns-resolve 'futon2.aif.selected-outer-cascade-policy-test 'production-carriers))))
(defn- reseal [c] (assoc c :carrier-pin (carrier/carrier-pin c)))

(defn- canonical-carrier []
  (let [{:keys [menu evidence objective]} (production)
        f (frontier/produce menu)
        s (selection/evaluate f menu 0.0)]
    (carrier/seal-canonical menu evidence objective f s)))

(defn- replay-after [c f]
  (let [s (selection/evaluate f (:sealed-menu c) (get-in c [:canonical-selection :draw :u]))]
    (-> (assoc c :canonical-frontier f :canonical-selection s) reseal)))

(deftest canonical-frontier-is-replayed-before-choice
  (let [honest (canonical-carrier)
        f (:canonical-frontier honest)
        id (get-in f [:entries 0 :policy-id])
        mutate (fn [xf] (replay-after honest (xf f)))]
    (is (nil? (carrier/verify honest)))
    (doseq [bad [(mutate #(update-in % [:entries 0 :G] inc))
                 (mutate #(update-in % [:horizon-choice :horizon] inc))
                 (mutate #(assoc % :count-table {:forged true}
                                  :count-table-digest "forged"))
                 (mutate #(assoc-in % [:class-model-ids id] "forged"))
                 (mutate #(assoc-in % [:entries 0 :status] :refused))]]
      (is (= :selected-policy-canonical-frontier-replay (carrier/verify bad))))))

(deftest canonical-tie-draw-is-pinned
  ;; The b5 menu's two scored entries tie on G, so the recorded draw decides
  ;; the action.  A draw that does not reproduce the recorded action, or lies
  ;; outside [0,1), is refused by the choice replay.
  (let [honest (canonical-carrier)
        u (fn [x] (-> honest (assoc-in [:canonical-selection :draw :u] x) reseal))]
    (is (some? (get-in honest [:canonical-selection :draw])))
    (is (= 2 (count (get-in honest [:canonical-selection :draw :candidates]))))
    (is (nil? (carrier/verify (u 0.49))))
    (doseq [x [0.9 1.0 -0.1]]
      (is (= :selected-policy-canonical-replay (carrier/verify (u x)))))))
