(ns futon2.aif.outer-canonical-selection
  "Selection over the retained O3-1 canonical-G frontier."
  (:require [futon2.aif.load-identity :as identity]))

(identity/register! *ns* *file*)

(defn- finite? [x] (and (number? x) (Double/isFinite (double x))))

(defn evaluate
  "Choose the least scored canonical G in sealed-menu order.  Refusals are
   deliberately absent from the candidate population, never numeric scores."
  ([frontier menu] (evaluate frontier menu nil))
  ([frontier menu draw]
   (cond
     (not= :produced (:status frontier))
     {:status :no-choice :reason :canonical-frontier-unavailable}

     (not= (:menu-pin frontier) (:menu-pin menu))
     {:status :no-choice :reason :canonical-frontier-menu-mismatch}

     :else
     (let [by-id (into {} (map (juxt :policy-id identity)) (:entries frontier))
           scored (->> (:entries menu)
                       (keep (fn [entry]
                               (let [row (get by-id (:policy-id entry))]
                                 (when (and (= :scored (:status row))
                                            (finite? (:G row))
                                            (= (get-in entry [:action :cascade-digest]) (:cascade-digest row))
                                            (= (get-in entry [:action :cascade-digest])
                                               (get-in row [:action-pin :cascade-digest])))
                                   (assoc row :menu-entry entry)))))
                       vec)]
       (if (empty? scored)
         {:status :no-choice :reason :no-scored-canonical-policy
          :frontier-pin (:frontier-pin frontier)}
         (let [minimum (apply min (map :G scored))
               tied (filterv #(== (double minimum) (double (:G %))) scored)
               tie? (> (count tied) 1)
               u (when tie? draw)]
           (if (and tie? (not (and (finite? u) (<= 0.0 u) (< u 1.0))))
             {:status :no-choice :reason :invalid-canonical-tie-draw
              :draw (if (nil? draw)
                      {:absent :no-sealed-draw}
                      {:invalid draw})
              :frontier-pin (:frontier-pin frontier)}
             (let [selected (nth tied (if tie? (int (* u (count tied))) 0))]
               {:status :selected :basis :canonical-G-min-over-scored-frontier
                :action (assoc (assoc (select-keys (:action (:menu-entry selected))
                                                  [:target :cascade-digest])
                                     :type :advance-cascade-live-policy)
                               :policy-id (:policy-id selected))
                :selected (dissoc selected :menu-entry)
                :frontier-pin (:frontier-pin frontier)
                :draw (when tie? {:law :uniform-over-equal-minima :u u
                                  :candidates (mapv :cascade-digest tied)})}))))))))
