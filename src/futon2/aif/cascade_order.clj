(ns futon2.aif.cascade-order
  "Resolve a constructed cascade's containment order to the transition carrier
   consumed by rollout. Shared by both EFE scoring routes."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(defn order-use [action]
  (let [prec (:precedence action)
        order (get-in action [:construction-receipt :order])
        list-use (fn [meta] {:precedence prec :meta meta})]
    (cond
      (nil? order) (list-use {:order {:absent :no-order-on-receipt}})
      (:kind order) (list-use {:order-not-used {:refused (:kind order)}})
      (keyword? (:precedence-violations order))
      (list-use {:order-not-used (:precedence-violations order)})
      (seq (:precedence-violations order))
      (list-use {:order-not-used {:precedence-violations (count (:precedence-violations order))}})
      :else
      (let [units (map :unit (:units order))
            carried-patterns (if (every? map? prec) prec (:patterns action))
            children (reduce (fn [m [a b]] (update m a (fnil conj #{}) b)) {} (:descent order))
            below (fn below [u] (reduce into (set (children u)) (map below (children u))))
            n (count units)
            chain? (= (* n (dec n)) (* 2 (reduce + (map (comp count below) units))))]
        (if-not chain?
          (let [by-id (into {} (map (fn [p] [(if (map? p) (:id p) p) p])) carried-patterns)
                patterns (into {} (for [{:keys [unit pattern]} (:units order)]
                                    [unit (get by-id pattern)]))]
            (if (and (seq units) (every? map? (vals patterns)))
              {:precedence prec
               :kernel-step {:co-apply {:units (vec units) :descent (vec (:descent order))
                                        :patterns patterns}}
               :meta {:order :co-application}}
              (list-use {:order-not-used :units-not-mapped-to-precedence})))
          (let [linear (sort-by (comp - count below) units)
                pattern-of (into {} (map (juxt :unit :pattern)) (:units order))
                by-id (into {} (map (fn [p] [(if (map? p) (:id p) p) p])) carried-patterns)
                ordered (mapv #(get by-id (pattern-of %)) linear)]
            (if (and (= n (count prec)) (every? some? ordered))
              {:precedence ordered :meta {:order :chain}}
              (list-use {:order-not-used :units-not-mapped-to-precedence}))))))))
