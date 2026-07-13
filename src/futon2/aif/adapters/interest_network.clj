(ns futon2.aif.adapters.interest-network
  "M-interest-network-coupling capstone: turn the lived interest-network posterior
   (XTDB interest-event entities, read via futon1a) into per-candidate AIF bias.

   Route 1 (per the ratified wiring diagram): on :open-mission / :address-sorry
   candidates whose :target a bridged mission concerns, set :intrinsic-value
   (finish-what-you-started) and ADD to :structural-pressure-per-action
   (make-a-long-standing-interest-real). compute-efe already honours both, so no
   change to efe.clj / preferences.clj is needed.

   The bridge is an explicit, reviewable table (mission -> the EoI entities it
   concerns). Auto territory-tagging of missions is the deferred generalisation —
   see M-interest-network-coupling.aif-wiring.md §5. fetch failures degrade to a
   no-op (empty bias), so this never breaks a judge cycle."
  (:require [babashka.http-client :as http]
            [clojure.edn :as edn]))

(def ^:private f1a (or (System/getenv "FUTON_SUBSTRATE_URL")
                       (System/getenv "FUTON1A_URL") "http://127.0.0.1:7071"))
(def ^:private w-finish 0.15)   ; hand-set (Q4), tunable
(def ^:private w-real   0.6)    ; hand-set (Q4), tunable

;; Explicit bridge: WM mission :target (mission-id) -> EoI entities it concerns.
;; The lived standing of those entities (from XTDB) drives the bias.
(def bridge
  {"M-interim-director"
   #{":node/hyperreal-enterprises"
     "vsat-poc-2026-q2-q3-scenario-c-vsatlatarium"
     "vsat-poc-2026-q2-q3-scenario-a-vsatelier"}})

(defn- fetch-standing
  "entity-id -> latest posterior-state, from futon1a interest-event entities."
  []
  (try
    (let [resp (http/get (str f1a "/api/alpha/entities/latest?type=interest-event&limit=500")
                         {:headers {"accept" "application/edn" "x-penholder" "api"}})
          parsed (edn/read-string (:body resp))]
      (into {} (for [e (:entities parsed)
                     :let [p (:props e)]
                     :when (and (:target/entity-id p) (:posterior-state p))]
                 [(:target/entity-id p) (:posterior-state p)])))
    (catch Exception _ {})))

(defn interest-bias-map
  "mission-target -> {:intrinsic-value n :structural-pressure-per-action n :because [...]}.
   finish  = the mission's concerned entities are actively moving (strengthened/refined);
   make-real = proportion of concerns advancing."
  []
  (let [standing (fetch-standing)]
    (into {}
          (for [[mission entities] bridge
                :let [moving (filter #(#{:strengthened :refined} (get standing %)) entities)
                      n (count moving) tot (max 1 (count entities))]
                :when (pos? n)]
            [mission {:intrinsic-value w-finish
                      :structural-pressure-per-action (* w-real (/ (double n) tot))
                      :because (mapv (fn [e] [e (get standing e)]) moving)}]))))

(defn enrich-candidates
  "Add interest bias to candidates whose :target is bridged. :intrinsic-value and
   :structural-pressure-per-action are ADDED to any existing values, so this composes
   with enrich-candidates-with-structural-pressure."
  ([candidates] (enrich-candidates candidates (interest-bias-map)))
  ([candidates bias]
   (mapv (fn [c]
           (if-let [b (get bias (:target c))]
             (-> c
                 (update :intrinsic-value (fnil + 0) (:intrinsic-value b))
                 (update :structural-pressure-per-action (fnil + 0) (:structural-pressure-per-action b))
                 (assoc :interest-because (:because b)))
             c))
         candidates)))
