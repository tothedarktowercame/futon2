(ns futon2.aif.cascade-want-projection
  "Carry the selected cascade's declared outputs into terminal accounting when
  the target has no stronger source-derived want contract."
  (:require [futon2.aif.load-identity :as identity]))

(identity/register! *ns* *file*)

(defn- patterns [action]
  (let [precedence (:precedence action)]
    (cond
      (sequential? precedence) (filter map? precedence)
      (map? (get-in precedence [:co-apply :patterns]))
      (vals (get-in precedence [:co-apply :patterns]))
      :else [])))

(defn attach
  "Attach cascade-produced wants and their carried locators to ACTION only
  when ACTION has no wants. Mission/source contracts therefore remain the
  authority when present; an output-free cascade remains honestly empty."
  [action]
  (if (seq (:want action))
    action
    (let [target (:target action)
          patterns (patterns action)
          outputs (->> patterns (mapcat :produces) distinct (sort-by pr-str) vec)
          ;; Cascade outputs are target-qualified, but may carry a structured
          ;; tail such as [target :pattern-done occurrence]. Terminal
          ;; accounting's identity is always [target token], so preserve a
          ;; simple pair and group a longer tail as the token.
          wants (mapv (fn [output]
                        (if (and (vector? output) (= target (first output)))
                          (if (= 2 (count output))
                            output
                            [target (vec (rest output))])
                          output))
                      outputs)
          locators (apply merge (or (:observation-locators action) {})
                          (map #(or (:observation-locators %) {}) patterns))]
      (assoc action
             :want wants
             :observation-locators
             (into {} (keep (fn [[output want]]
                              (when-let [locator (or (get locators want)
                                                    (get locators output)
                                                    (get locators [target want]))]
                                [want locator])))
                           (map vector outputs wants))))))
