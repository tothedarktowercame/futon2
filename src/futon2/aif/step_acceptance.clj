(ns futon2.aif.step-acceptance
  "Pure pin decision over captured battery verdicts (not ledger admission)."
  (:require [clojure.string :as str]))

(defn advance-pin [pin rows step run-id store reason at]
  (when (and (some? reason) (str/blank? reason))
    (throw (ex-info "--override-red requires a nonblank reason" {})))
  (let [red (filterv #(and (= run-id (:row/run-id %))
                           (= :red (:row/verdict %))) rows)
        checks (vec (distinct (map :row/check-id red)))]
    (when (and (seq red) (nil? reason))
      (throw (ex-info "accept refuses red battery verdicts"
                      {:run-id run-id :red-checks checks})))
    (-> pin
        (update :pin/generation inc)
        (assoc :pin/advanced-at at)
        (update :pin/accepted-steps
                #(conj (vec %)
                       (cond-> {:step step :run-id run-id :store store}
                         (some? reason) (assoc :override-red
                                              {:reason reason :checks checks})))))))
