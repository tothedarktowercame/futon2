(ns futon2.aif.pattern-observation
  "Projection of frozen pattern-domain wants into supplied observations.

  A row's :observed is a boolean only when the retained condition's authored
  :completion-check is an executable artifact locator, evaluated by
  futon2.aif.completion-check at the supplied after revision. Everything else
  is a typed absence. A retained fold condition is never itself an
  after-revision measurement, regardless of its status or any boolean-looking
  fields it happens to carry."
  (:require [futon2.aif.completion-check :as completion-check]
            [futon2.aif.interpretation-evidence :as evidence]
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

(defn- completion-check [condition]
  (get-in condition [:witness :authored-clause :completion-check]))

(defn- observation
  "The evaluator's result for the one matching condition, or a typed absence
  for none or several. Only an executable completion check can observe."
  [revision matches]
  (case (count matches)
    0 {:status :missing :reason :occurrence-not-in-fold}
    1 (completion-check/evaluate (completion-check (first matches)) revision)
    {:status :missing :reason :occurrence-ambiguous-in-fold
     :count (count matches)}))

(defn- supplied-row [revision source by-occurrence occurrence effect]
  (let [matches (get by-occurrence occurrence)
        condition (when (= 1 (count matches)) (first matches))
        result (observation revision matches)
        observed? (= :observed (:status result))]
    {:schema row-schema
     :pattern (first occurrence)
     :occurrence occurrence
     :effect effect
     :revision revision
     :observed (if observed? (:value result) result)
     :authority-evidence (when observed?
                           (select-keys result [:check :evidence]))
     :source (assoc source
                    :fold-status (:status condition)
                    :completion-check (completion-check condition))}))

(defn supply
  "Supply one revision-bound observation for every frozen pattern want: a
  boolean where the condition's completion check is executable, a typed
  absence otherwise.

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
