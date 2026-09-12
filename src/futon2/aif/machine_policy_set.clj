(ns futon2.aif.machine-policy-set
  "Exact projection from retained policy rankings to MachinePolicySet.Candidate.

   MachinePolicySet's Candidate.score is an integer while production scores are
   IEEE-754 doubles.  The projection therefore carries the raw signed 64-bit
   representation into Lean's Int.  This is lossless and makes no ordering claim;
   machinePolicySet itself uses only extensional list membership."
  (:require [futon2.aif.trace :as trace]))

(defn- refuse!
  [cause detail]
  (throw (ex-info (name cause) (assoc detail :refusal cause))))

(defn project-candidate
  "Project one compact trace ranking into MachineAction.Candidate data.

   :id is the retained positive rank, :score is the exact raw binary64 bit
   pattern interpreted as a signed integer, and :no-op marks action type
   :no-op."
  [ranked-action]
  (let [rank (:rank ranked-action)
        score (:controller-score ranked-action)
        action (:action ranked-action)]
    (when-not (and (integer? rank) (pos? rank))
      (refuse! :policy-set-invalid-rank {:rank rank}))
    (when-not (and (number? score)
                   (Double/isFinite (double score)))
      (refuse! :policy-set-invalid-score {:rank rank :score score}))
    (when-not (and (map? action) (keyword? (:type action)))
      (refuse! :policy-set-invalid-action {:rank rank :action action}))
    {:id rank
     :score (Long/valueOf (Double/doubleToRawLongBits (double score)))
     :no-op (= :no-op (:type action))}))

(defn project-ranked-actions
  "Project the complete ordered ranking; duplicate ranks refuse closed."
  [ranked-actions]
  (when-not (vector? ranked-actions)
    (refuse! :policy-set-ranked-actions-missing
             {:ranked-actions-type (some-> ranked-actions class str)}))
  (let [projected (mapv project-candidate ranked-actions)
        ids (mapv :id projected)]
    (when-not (= (count ids) (count (distinct ids)))
      (refuse! :policy-set-duplicate-rank {:ids ids}))
    projected))

(defn compare-projections!
  "Require exact ordered agreement with a pinned projection.

   Ordering is a readback/pin property, not part of machinePolicySet's
   extensional theorem.  Keeping it here commissions reorder and drop controls."
  [expected observed]
  (if (= expected observed)
    {:status :matched :candidate-count (count observed) :projection observed}
    (refuse! :policy-set-projection-mismatch
             {:expected-count (count expected)
              :observed-count (count observed)
              :first-mismatch
              (first (keep-indexed (fn [i pair]
                                     (when (not= (first pair) (second pair)) i))
                                   (map vector expected observed)))})))

(defn read-projected-policy-set
  "Read one bounded daily-trace fixture through trace/read-trace and project it."
  [dir date-str]
  (let [records (trace/read-trace :dir dir :date-str date-str)]
    (when-not (= 1 (count records))
      (refuse! :policy-set-record-count {:record-count (count records)}))
    (project-ranked-actions (:ranked-actions (first records)))))
