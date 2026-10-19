(ns futon2.aif.assessed-policy-basis
  "Pure assessed-population evidence carrier. Not an executable dispatch carrier."
  (:require [futon2.aif.load-identity :as identity]
            [futon2.aif.outer-policy-evidence :as evidence]
            [futon2.aif.outer-policy-objective :as objective]
            [futon2.aif.wm.family-selection :as family]))

(def schema :wm/assessed-policy-basis-v1)

(defn carrier-pin [carrier]
  {:path "wm://assessed-policy-basis-v1"
   :sha256 (identity/sha256
            (.getBytes (pr-str (dissoc carrier :carrier-pin)) "UTF-8"))})

(defn verify
  "Return nil for a recomputable basis, otherwise a typed defect. No IO."
  [carrier]
  (try
    (let [{:keys [menu evidence receipt]} carrier
          selection (:assessed-selection receipt)]
      (cond
        (not= schema (:schema carrier)) :basis-schema
        (not= :sealed (:status carrier)) :basis-status
        (not= :assessed-population (:scope carrier)) :basis-scope
        (not= (carrier-pin carrier) (:carrier-pin carrier)) :basis-pin
        (family/verify-sealed-menu menu) :basis-menu
        (evidence/verify-evidence evidence) :basis-evidence
        (not= (:menu-pin menu) (:menu-pin evidence)) :basis-menu-substitution
        (not= :partial (:status receipt)) :basis-not-partial
        (not= :selected (:status selection)) :basis-not-selected
        (not= :assessed-population (:scope selection)) :basis-selection-scope
        (not= receipt (objective/evaluate evidence menu (get-in selection [:draw :u])))
        :basis-recomputation
        (not= :verified (get-in selection [:identity-check :status])) :basis-identity
        :else nil))
    (catch Throwable _ :basis-malformed)))

(defn seal
  "Retain original menu, full evidence and objective receipt; verify before returning."
  [menu evidence receipt]
  (let [body {:schema schema :status :sealed :scope :assessed-population
              :menu menu :evidence evidence :receipt receipt}
        carrier (assoc body :carrier-pin (carrier-pin body))]
    (if-let [defect (verify carrier)]
      {:schema schema :status :refused :reason defect}
      carrier)))
