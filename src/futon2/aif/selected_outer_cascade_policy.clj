(ns futon2.aif.selected-outer-cascade-policy
  "Sealed handoff from the strict outer policy selector to cascade execution."
  (:require [futon2.aif.load-identity :as load-identity]
            [futon2.aif.outer-policy-evidence :as evidence]
            [futon2.aif.outer-policy-objective :as objective]
            [futon2.aif.wm.executable-cascade :as executable]
            [futon2.aif.wm.family-selection :as family]))

(load-identity/register! *ns* *file*)

(def schema :wm/selected-outer-cascade-policy-v1)
(def ^:private body-keys
  [:schema :status :target-id :policy-id :cascade-digest :selected-entry
   :sealed-menu :menu-pin :evidence-carrier :evidence-pin
   :objective-receipt :objective-pin :recomputation])

(defn- pin [path value]
  {:path path
   :sha256 (load-identity/sha256 (.getBytes (pr-str value) "UTF-8"))})

(defn objective-pin [receipt]
  (pin "wm://outer-policy-objective-v1" (dissoc receipt :objective-pin)))

(defn carrier-pin [carrier]
  (pin "wm://selected-outer-cascade-policy-v1"
       (select-keys carrier body-keys)))

(defn seal
  "Seal a selected strict-objective receipt. Ties/refusals cannot be carried."
  [menu evidence-carrier objective-receipt]
  (if-not (= :selected (:status objective-receipt))
    {:schema schema :status :refused :reason :strict-policy-selection-required
     :objective-status (:status objective-receipt)
     :tied (:tied objective-receipt)}
    (let [entry (first (filter #(and (= (:policy-id %) (get-in objective-receipt [:action :policy-id]))
                                     (= (:cascade-digest %) (get-in objective-receipt [:action :cascade-digest])))
                               (:entries menu)))
          objective-receipt (assoc objective-receipt :objective-pin
                                   (objective-pin objective-receipt))
          body {:schema schema :status :sealed
                :target-id (get-in objective-receipt [:action :target])
                :policy-id (get-in objective-receipt [:action :policy-id])
                :cascade-digest (get-in objective-receipt [:action :cascade-digest])
                :selected-entry entry
                :sealed-menu menu :menu-pin (:menu-pin menu)
                :evidence-carrier evidence-carrier
                :evidence-pin (:evidence-pin evidence-carrier)
                :objective-receipt objective-receipt
                :objective-pin (:objective-pin objective-receipt)
                :recomputation {:q2 {:target-id (get-in objective-receipt [:action :target])
                                     :policy-id (get-in objective-receipt [:action :policy-id])
                                     :cascade-digest (get-in objective-receipt [:action :cascade-digest])}
                                :q4 (:policies objective-receipt)
                                :q8 (:comparison objective-receipt)}}]
      (assoc body :carrier-pin (carrier-pin body)))))

(defn verify
  "Total semantic verification. Returns nil when valid, otherwise a keyword."
  [carrier]
  (try
    (let [menu (:sealed-menu carrier)
          ev (:evidence-carrier carrier)
          receipt (:objective-receipt carrier)
          entry (:selected-entry carrier)
          action (:action receipt)
          recomputed (when (and (map? ev) (map? menu))
                       (objective/evaluate ev menu))]
      (cond
        (not= schema (:schema carrier)) :selected-policy-schema
        (not= :sealed (:status carrier)) :selected-policy-status
        (not= (carrier-pin carrier) (:carrier-pin carrier)) :selected-policy-seal
        (family/verify-sealed-menu menu) :selected-policy-menu-invalid
        (not= (:menu-pin menu) (:menu-pin carrier)) :selected-policy-menu-substitution
        (evidence/verify-evidence ev) :selected-policy-evidence-invalid
        (not= (:evidence-pin ev) (:evidence-pin carrier)) :selected-policy-evidence-substitution
        (not= (objective-pin receipt) (:objective-pin receipt)) :selected-policy-objective-seal
        (not= (:objective-pin receipt) (:objective-pin carrier)) :selected-policy-objective-substitution
        (not= :selected (:status receipt)) :selected-policy-not-strict
        (not= receipt (assoc recomputed :objective-pin (objective-pin recomputed)))
        :selected-policy-objective-recomputation
        (nil? entry) :selected-policy-entry-missing
        (not= (:cascade-digest entry) (get-in entry [:canonical-cascade :digest]))
        :selected-policy-canonical-record
        (not= (:cascade-digest entry) (executable/digest (:canonical-cascade entry)))
        :selected-policy-canonical-record
        (not= (:target-id carrier) (:target-id entry) (:target action)) :selected-policy-target-mismatch
        (not= (:policy-id carrier) (:policy-id entry) (:policy-id action)) :selected-policy-occurrence-mismatch
        (not= (:cascade-digest carrier) (:cascade-digest entry) (:cascade-digest action))
        :selected-policy-digest-mismatch
        :else nil))
    (catch Throwable _ :selected-policy-malformed)))

(defn decision
  "Project a verified handoff into the downstream decision shape without selection."
  [carrier]
  (if-let [defect (verify carrier)]
    {:status :refused :reason defect}
    (let [entry (:selected-entry carrier)]
      {:status :selected
       :decision {:status :selected
                  :action (:action entry)
                  :selection-certificate
                  {:selection-route :wm/selected-outer-cascade-policy-v1
                   :selected-canonical-cascade (:canonical-cascade entry)
                   :selected-outer-cascade-policy carrier
                   :q2 (get-in carrier [:recomputation :q2])
                   :q4 (get-in carrier [:recomputation :q4])
                   :q8 (get-in carrier [:recomputation :q8])}}
       :ranked [entry]
       :preselected true})))
