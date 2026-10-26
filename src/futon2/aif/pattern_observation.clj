(ns futon2.aif.pattern-observation
  "Projection of frozen pattern-domain wants into supplied observations.

  This producer deliberately supplies typed absences only. A retained fold
  condition is not an after-revision measurement, regardless of its status or
  any boolean-looking fields it happens to carry."
  (:require [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.token-outcome :as token-outcome]))

(load-identity/register! *ns* *file*)

(def schema :wm/supplied-pattern-observations-v1)
(def row-schema :wm/supplied-pattern-observation-v1)

(defn- pattern-prediction? [prediction]
  (let [target (:target prediction)
        wanted (:wanted prediction)]
    (and (= :frozen (:status prediction))
         (seq wanted)
         (every? #(token-outcome/pattern-token? target (:token %)) wanted))))

(defn- occurrence-order [[pattern index]]
  [pattern index])

(defn- absent-observation [matches]
  (case (count matches)
    0 {:status :missing :reason :occurrence-not-in-fold}
    1 {:status :missing :reason :completion-check-not-executable}
    {:status :missing :reason :occurrence-ambiguous-in-fold
     :count (count matches)}))

(defn- supplied-row [revision source by-occurrence occurrence effect]
  (let [matches (get by-occurrence occurrence)
        condition (when (= 1 (count matches)) (first matches))]
    {:schema row-schema
     :pattern (first occurrence)
     :occurrence occurrence
     :effect effect
     :revision revision
     :observed (absent-observation matches)
     :authority-evidence nil
     :source (assoc source
                    :fold-status (:status condition)
                    :completion-check
                    (get-in condition [:witness :authored-clause
                                       :completion-check]))}))

(defn supply
  "Supply one revision-bound typed absence for every frozen pattern want.

  Fold status and result-like fields are retained only as provenance and can
  never become the observation value."
  [{:keys [prediction fold-conditions revision source]}]
  (cond
    (not (pattern-prediction? prediction))
    {:schema schema
     :status :not-applicable
     :kind :prediction-not-pattern-domain
     :target (:target prediction)}

    (not (and (string? (:sha revision))
              (re-matches #"[0-9a-f]{40}" (:sha revision))))
    {:schema schema :status :refused :kind :after-revision-missing}

    :else
    (let [by-occurrence (group-by #(get-in % [:witness :occurrence-id])
                                  fold-conditions)
          wants (sort-by (comp occurrence-order #(nth % 2) :token)
                         (:wanted prediction))
          rows (mapv (fn [{effect :token}]
                       (let [occurrence (nth effect 2)]
                         (supplied-row revision source by-occurrence occurrence
                                       effect)))
                     wants)]
      {:schema schema
       :status :supplied
       :target (:target prediction)
       :rows rows
       :rows-sha256 (evidence/value-digest rows)})))
