(ns futon2.aif.policy-depth
  "Run-scoped horizon configuration; no implicit default changes."
  (:require [clojure.edn :as edn]))

(defn validate [config]
  (when config
    (doseq [k [:anticipation :cascade-rollout]]
      (when-not (pos-int? (get config k))
        (throw (ex-info "Policy depth must name two positive integer horizons"
                        {:field k :value (get config k)}))))
    (select-keys config [:anticipation :cascade-rollout])))

(defn configured
  "Explicit judge opts win. The stepped CLI can read the RUN4 sheet through
   FUTON_WM_RUN_CONFIG; no file is read when that variable is absent."
  [opts]
  (validate
   (or (:policy-depth opts)
       (when-let [path (System/getenv "FUTON_WM_RUN_CONFIG")]
         (let [sheet (edn/read-string (slurp path))]
           (or (:policy-depth sheet) (get-in sheet [:flags :policy-depth])
               (throw (ex-info "Run config lacks policy depth" {:path path}))))))))

(defn anticipation [snapshot config]
  (let [ready? (and (:events-loaded? snapshot) (seq (:events snapshot)))
        effective (if ready? (or (:anticipation config) 3) 1)]
    {:horizon-steps (when ready? effective)
     :record (when config
               {:kind :anticipation :requested (:anticipation config)
                :effective effective
                :fallback? (not (boolean ready?))
                :reason (if ready? :events-available :anticipation-events-unavailable)})}))
