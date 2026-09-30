(ns futon2.aif.target-policy-family
  "Build one target's structurally distinct policy family from its current 象
  reading and pinned graph. The defaults, k=3 and weights why/how=1,
  co-cited=2, rejected-beside/next-in-session=3, are the parameters recorded
  by the S3c mission-head retraction artifacts. This namespace does not score."
  (:require [futon2.aif.analysis-cascade :as analysis]
            [futon2.aif.cascade-shape-g :as shape-g]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.pattern-graph-pin :as graph-pin]
            [futon2.aif.pattern-retraction :as retraction]))

(identity/register! *ns* *file*)

(def default-retraction {:k 3 :weights graph-pin/default-weights})

(defn- stable-id [target structure]
  (str target "/policy-"
       (subs (identity/sha256 (.getBytes (pr-str structure) "UTF-8")) 0 20)))

(defn- reading-patterns [analysis-map]
  (vec (sort (distinct
              (for [sentence (:sentences analysis-map)
                    fragment (:fragments sentence)
                    ref (:pattern_refs fragment)
                    :when (= "candidate" (:status ref))]
                (:id ref))))))

(defn- reading-policies [target analysis-map]
  (vec (for [[mode kind] [[:alternatives :reading-alternatives]
                          [:overlap :reading-overlap]]
             cascade (:cascades (analysis/analysis->cascades analysis-map {:mode mode}))]
         {:target target :mission target :kind kind :cascade cascade
          :analysis analysis-map})))

(defn- retraction-policies [target analysis-map result]
  (mapv (fn [r]
          {:target target :mission target :kind :retraction :analysis analysis-map
           :cascade {:nodes (:nodes r) :edges (:edges r) :precedence (:nodes r)}})
        (:retractions result)))

(defn- deduplicate [policies]
  (:rows
   (reduce (fn [{:keys [seen] :as acc} policy]
             (let [structure (shape-g/structural-identity policy)]
               (if (seen structure) acc
                   (-> acc (update :seen conj structure)
                       (update :rows conj
                               (assoc policy :policy-id
                                      (stable-id (:target policy) structure)))))))
           {:seen #{} :rows []} policies)))

(defn policy-family
  "Return reading and retraction policies, preserving every counted failure."
  [{:keys [reading graph retraction]}]
  (let [target (:target-id reading)]
    (if (= :absent (:status reading))
      {:status :failed :target-id target :policies []
       :reported-count 0 :distinct-count 0
       :failures [(select-keys reading [:kind :target-id :expected-digest :found-digests])]
       :failure-count 1
       :provenance {:excerpt-digest (:expected-digest reading)}}
      (let [analysis-map (:analysis reading)
            params (merge default-retraction retraction)
            seeds (reading-patterns analysis-map)
            retract (retraction/retractions graph (assoc params :seeds seeds))
            reported (vec (concat (reading-policies target analysis-map)
                                  (retraction-policies target analysis-map retract)))
            policies (deduplicate reported)
            failures (vec (:failures retract))
            failures (cond-> failures
                       (empty? policies) (conj {:kind :empty-policy-family
                                                :target-id target}))]
        {:status (if (seq policies) :computed :failed)
         :target-id target :policies policies
         :reported-count (count reported) :distinct-count (count policies)
         :failures failures :failure-count (count failures)
         :provenance {:excerpt-digest (:excerpt-digest reading)
                      :analysis-digest (:analysis-digest reading)
                      :graph-digest (identity/sha256 (.getBytes (pr-str graph) "UTF-8"))
                      :retraction params}}))))

