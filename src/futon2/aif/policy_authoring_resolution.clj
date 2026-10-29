(ns futon2.aif.policy-authoring-resolution
  "Indexes reviewed operational authoring by the reading policy it resolves."
  (:require [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.wm.authored.req-f0ba279a :as req-f0ba279a]))

(def ^:private resolutions
  {"M-futonzero-prelim-practice/policy-87eb5c880a0b9ab8e194"
   {:resolution-id :req-f0ba279a
    :completion-checks (vec (sort-by pr-str (map :completion-check (vals req-f0ba279a/clauses))))}})

(defn for-policy [policy-id]
  (when-let [resolution (get resolutions policy-id)]
    (assoc resolution :digest (evidence/value-digest resolution))))
