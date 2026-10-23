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
