(ns futon2.aif.mission-observation
  "Exact pre/post selected-mission observations. This namespace performs the
   C3/C4 reads; labels supplied by callers are never authority."
  (:require [clojure.java.io :as io]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.d-predecessor-task-authority :as d-task]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def schema :wm/mission-observation-baseline-v1)
(def projection-schema :wm/mission-observation-projection-v1)

(defn- refusal [reason details]
  {:schema projection-schema :status :refused :reason reason :details details})

(defn- qualified-wants [action]
  (mapv #(if (vector? %) % [(:target action) %]) (:want action)))

(defn- canonical [path] (.getCanonicalPath (io/file path)))

(defn- validate-census [wants rows]
  (let [groups (group-by :token rows)
        extras (vec (remove (set wants) (keys groups)))
        duplicates (vec (for [[token matches] groups :when (> (count matches) 1)] token))
        missing (vec (remove #(contains? groups %) wants))]
    (cond
      (empty? wants) {:reason :selected-wants-empty}
      (not= (count wants) (count (distinct wants))) {:reason :selected-wants-duplicate}
      (seq extras) {:reason :measurement-extra :tokens extras}
      (seq duplicates) {:reason :measurement-ambiguous :tokens duplicates}
      (seq missing) {:reason :measurement-missing :tokens missing}
      :else {:rows (mapv #(first (get groups %)) wants)})))

(defn capture-before
  "Run the existing C3/C4 readers at the immutable pre-author revision.
   Unsupported readers remain explicit missing results."
  [{:keys [dispatch repository revision]}]
  (let [action (get-in dispatch [:occurrence :action/value])
        wants (qualified-wants action)
        occurrence-id (get-in dispatch [:occurrence :action/id])
        bound-revision (get-in dispatch [:before :head])]
    (cond
      (not (and (string? occurrence-id) (string? repository) (string? revision)))
      (refusal :request-binding-missing {})
      (not= bound-revision revision)
      (refusal :before-revision-mismatch {:expected bound-revision :actual revision})
      :else
      (let [rows (d-task/artifact-tokens dispatch repository bound-revision)
            census (validate-census wants rows)]
        (if (:reason census)
          (refusal (:reason census) (dissoc census :reason))
          (let [normalized
            (mapv (fn [row]
                    (let [result (:result row)
                          observed (:observed result)]
                      {:occurrence-id occurrence-id
                       :action-sha256 (identity/digest action)
                       :target (:target action)
                       :token (:token row)
                       :locator (:declared-locator row)
                       :repository (canonical repository)
                       :revision revision
                       :observed (when (boolean? observed) observed)
                       :result result
                       :evidence-sha256 (evidence/value-digest result)}))
                  (:rows census))
            base {:schema schema :status :captured :occurrence-id occurrence-id
                  :action-sha256 (identity/digest action) :target (:target action)
                  :repository (canonical repository) :revision revision
                  :wants wants :rows normalized}]
            (assoc base :baseline-sha256 (evidence/value-digest base))))))))

(defn project
  "Admit the exact retained before baseline and post-build reader rows.
   Before=true is a typed refusal under the current selected-want policy."
  [{:keys [baseline dispatch repository after-revision after-rows]}]
  (let [action (get-in dispatch [:occurrence :action/value])
        occurrence-id (get-in dispatch [:occurrence :action/id])
        wants (qualified-wants action)
        trusted-before (capture-before {:dispatch dispatch :repository repository
                                        :revision (get-in dispatch [:before :head])})
        trusted-after (when (and (string? repository) (string? after-revision))
                        (d-task/artifact-tokens dispatch repository after-revision))
        supplied-after after-rows
        after-rows (mapv (fn [row]
                           (if (contains? row :observed)
                             row
                             (assoc row :observed (get-in row [:result :observed]))))
                         after-rows)
        before-census (validate-census wants (:rows baseline))
        after-census (validate-census wants after-rows)
        unsigned (dissoc baseline :baseline-sha256)]
    (cond
      (not= :captured (:status trusted-before))
      (refusal :before-authority-unavailable {:trusted-result trusted-before})
      (not= schema (:schema baseline)) (refusal :baseline-missing {})
      (not= (:baseline-sha256 baseline) (evidence/value-digest unsigned))
      (refusal :baseline-payload-mismatch {})
      (not= trusted-before baseline) (refusal :before-authority-mismatch {})
      (not= trusted-after supplied-after) (refusal :after-authority-mismatch {})
      (not= occurrence-id (:occurrence-id baseline)) (refusal :occurrence-mismatch {})
      (not= (identity/digest action) (:action-sha256 baseline)) (refusal :action-mismatch {})
      (not= (:target action) (:target baseline)) (refusal :target-mismatch {})
      (not= (canonical repository) (:repository baseline)) (refusal :repository-mismatch {})
      (:reason before-census) (refusal (:reason before-census) (dissoc before-census :reason))
      (:reason after-census) (refusal (:reason after-census) (dissoc after-census :reason))
      :else
      (let [pairs (mapv vector (:rows before-census) (:rows after-census))
            problem
            (some (fn [[before after]]
                    (cond
                      (not= (:token before) (:token after)) :token-mismatch
                      (not= (:locator before) (:declared-locator after)) :locator-mismatch
                      (not= (:revision baseline) (get-in before [:result :evidence :resolved-sha]))
                      :before-revision-mismatch
                      (not= after-revision (get-in after [:result :evidence :resolved-sha]))
                      :after-revision-mismatch
                      (not= (:observed before) (get-in before [:result :observed]))
                      :before-payload-result-mismatch
                      (not= (:observed after) (get-in after [:result :observed]))
                      :after-payload-result-mismatch
                      (not (boolean? (:observed before))) :before-observation-missing
                      (true? (:observed before)) :selected-want-true-before
                      (not (boolean? (:observed after))) :after-observation-missing))
                  pairs)]
        (if problem
          (refusal problem {})
          {:schema projection-schema :status :admitted
           :occurrence-id occurrence-id :before-revision (:revision baseline)
           :after-revision after-revision
           :rows (mapv (fn [[before after]]
                         {:token (:token before) :before before
                          :after {:observed (:observed after) :measurement after}})
                       pairs)})))))
