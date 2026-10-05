(ns futon2.aif.selected-want-outcome
  "Terminal execution accounting for every selected want."
  (:require [futon2.aif.action-identity :as identity]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-field-observation :as field]))

(load-identity/register! *ns* *file*)

(def schema :wm/selected-want-outcome-v1)
(def classes #{:reached :progressed :blocked :untouched})

(defn- refuse [reason details]
  {:schema schema :status :refused :reason reason :details details})

(defn- pinned-source? [{:keys [path sha256]}]
  (try
    (and (string? path) (string? sha256)
         (= sha256 (field/sha256 (.getBytes (slurp path) "UTF-8"))))
    (catch Throwable _ false)))

(defn receipt [{:keys [selected-action token-comparison progress-evidence blockers]}]
  (let [target (:target selected-action)
        ;; The selection writes a want as a bare token of the action's own
        ;; target; predictions, locators and evidence use [target token]. A
        ;; want already written as a vector is checked below as it stands.
        wants (mapv (fn [want] (if (vector? want) want [target want]))
                    (:want selected-action))
        duplicates (->> wants frequencies (keep (fn [[w n]] (when (> n 1) w))) vec)
        rows (:tokens token-comparison)
        row-tokens (mapv :token rows)
        progress (group-by :token (vec progress-evidence))
        blocked (group-by :token (vec blockers))
        extras (vec (remove (set wants) (concat (keys progress) (keys blocked))))
        initial (get-in token-comparison [:prediction :initial-belief])
        locators (:observation-locators selected-action)]
    (cond
      (not (and (string? target) (seq wants)))
      (refuse :selected-want-identity-missing {:target target :wants wants})
      (seq duplicates) (refuse :selected-want-duplicate {:duplicates duplicates})
      (not-every? #(and (vector? %) (= 2 (count %)) (= target (first %))) wants)
      (refuse :selected-want-target-mismatch {:target target :wants wants})
      (not= :compared (:status token-comparison))
      (refuse :selected-want-after-observation-missing
              {:comparison-status (:status token-comparison)})
      (or (not= (set wants) (set row-tokens)) (not= (count wants) (count rows)))
      (refuse :selected-want-census-mismatch {:selected wants :observed row-tokens})
      (seq extras) (refuse :selected-want-evidence-extra {:extra-tokens extras})
      :else
      (let [outcomes
            (mapv
             (fn [want]
               (let [row (first (filter #(= want (:token %)) rows))
                     observed (:observed row)
                     measurement (:measurement row)
                     expected-locator (get locators want)
                     declared-locator (:declared-locator measurement)
                     after-locator (:after-locator measurement)
                     before-false? (and (map? initial) (seq initial)
                                        (every? (fn [[state mass]]
                                                  (or (not (pos? mass))
                                                      (not (contains? state want))))
                                                initial))
                     progress-rows (get progress want [])
                     blocker-rows (get blocked want [])
                     problem (cond
                               (not before-false?) :selected-want-not-false-before
                               (not (boolean? observed)) :selected-want-after-observation-missing
                               (not= expected-locator declared-locator) :selected-want-locator-mismatch
                               (not= (:sha after-locator)
                                     (get-in measurement [:result :evidence :resolved-sha]))
                               :selected-want-source-mismatch
                               (> (count progress-rows) 1) :selected-want-progress-duplicate
                               (> (count blocker-rows) 1) :selected-want-blocker-duplicate
                               (and (seq progress-rows) (seq blocker-rows))
                               :selected-want-classification-duplicate)
                     progress-row (first progress-rows)
                     progress-valid? (and progress-row
                                          (every? number? ((juxt :before :after :terminal)
                                                           progress-row))
                                          (pinned-source? (:source progress-row))
                                          (< (Math/abs (- (double (:terminal progress-row))
                                                          (double (:after progress-row))))
                                             (Math/abs (- (double (:terminal progress-row))
                                                          (double (:before progress-row))))))
                     blocker-row (first blocker-rows)
                     classification (cond problem nil
                                          observed :reached
                                          progress-row (if progress-valid? :progressed :invalid-progress)
                                          blocker-row (if (and (keyword? (:kind blocker-row))
                                                               (pinned-source? (:source blocker-row)))
                                                        :blocked :invalid-blocker)
                                          :else :untouched)]
                 (cond-> {:target target :want want
                          :before {:observed false :authority :selected-initial-belief}
                          :after {:observed observed :measurement measurement}
                          :classification classification}
                   progress-row (assoc :progress-evidence progress-row)
                   blocker-row (assoc :blocker blocker-row)
                   problem (assoc :error problem))))
             wants)
            errors (vec (keep (fn [row]
                                (or (:error row)
                                    (when (= :invalid-progress (:classification row))
                                      :selected-want-progress-invalid)
                                    (when (= :invalid-blocker (:classification row))
                                      :selected-want-blocker-invalid))) outcomes))]
        (if (seq errors)
          (refuse (first errors) {:errors errors :outcomes outcomes})
          (let [base {:schema schema :status :verified :target target
                      :selected-wants wants :outcomes outcomes
                      :by-class (into {} (map (fn [class]
                                                [class (mapv :want
                                                             (filter #(= class (:classification %))
                                                                     outcomes))])) classes)}]
            (assoc base :receipt/id (identity/digest base))))))))
