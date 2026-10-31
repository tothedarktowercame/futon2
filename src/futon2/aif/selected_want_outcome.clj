(ns futon2.aif.selected-want-outcome
  "Terminal execution accounting for every selected want."
  (:require [futon2.aif.action-identity :as identity]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-field-observation :as field]))

(load-identity/register! *ns* *file*)

(def schema :wm/selected-want-outcome-v1)
(def classes #{:reached :progressed :blocked :untouched :checkbox-only})

(defn- checkbox-locator?
  "A C4 declaration over a file: the mission checkbox class. This is the
  affirmation-shaped instrument whose observed truth says only that the
  owner ticked a box (MISSION-TEXT-ONLY-WANT-LOCATORS, 2026-10-31)."
  [locator]
  (= :C4 (:class locator)))

(defn- c8-locator-retained?
  "A C8 locator (registered passing run / named command) retained anywhere
  in the want's own locator set is itself a second instrument."
  [expected declared after]
  (boolean (some #(= :C8 (:class %)) [expected declared after])))

(defn- demo-corroborates?
  "The run's own :run-output demo receipt is a second instrument when it is
  present and its sha-pinned pointer names the same revision the checkbox
  was measured at. The demo pointer is the grounded commit, so this binds
  corroboration to the measured revision, not to any demo anywhere."
  [demo-receipt after-locator]
  (and (map? demo-receipt)
       (= :present (:status demo-receipt))
       (= (:sha after-locator)
          (get-in demo-receipt [:demo :pointer :sha]))))

(defn- refuse [reason details]
  {:schema schema :status :refused :reason reason :details details})

(defn- pinned-source? [{:keys [path sha256]}]
  (try
    (and (string? path) (string? sha256)
         (= sha256 (field/sha256 (.getBytes (slurp path) "UTF-8"))))
    (catch Throwable _ false)))

(defn receipt [{:keys [selected-action token-comparison mission-observation
                       progress-evidence blockers demo-receipt]
                :as input}]
  (if (and (contains? input :mission-observation)
           (not= :admitted (:status mission-observation)))
    (refuse :selected-want-mission-observation-refused
            {:mission-observation mission-observation})
    (let [target (:target selected-action)
        ;; The selection writes a want as a bare token of the action's own
        ;; target; predictions, locators and evidence use [target token]. A
        ;; want already written as a vector is checked below as it stands.
        wants (mapv (fn [want] (if (vector? want) want [target want]))
                    (:want selected-action))
        duplicates (->> wants frequencies (keep (fn [[w n]] (when (> n 1) w))) vec)
        ;; Comparison rows arrive from token-outcome/compare-outcomes under
        ;; :tokens; callers may also present the row vector under :rows.
        mission? (= :admitted (:status mission-observation))
        rows (if mission?
               (mapv (fn [{:keys [token after]}]
                       {:token token :observed (:observed after)
                        :measurement (:measurement after)})
                     (:rows mission-observation))
               (or (:tokens token-comparison) (:rows token-comparison)))
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
      (and (not mission?) (not= :compared (:status token-comparison)))
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
                     mission-before (some #(when (= want (:token %)) (:before %))
                                          (:rows mission-observation))
                     before-false? (if mission?
                                     (false? (:observed mission-before))
                                     (and (map? initial) (seq initial)
                                          (every? (fn [[state mass]]
                                                    (or (not (pos? mass))
                                                        (not (contains? state want))))
                                                  initial)))
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
                     ;; Two-instrument rule at measurement (never at
                     ;; admission): a want measured only by a C4 mission-file
                     ;; checkbox is promoted to :reached only when the same
                     ;; run retains a second instrument — the run's sha-pinned
                     ;; demo receipt at the measured revision, or a C8
                     ;; locator. Uncorroborated, the observation stays
                     ;; recorded (:after, as now) but the want is not
                     ;; promoted: it stays open and selectable.
                     checkbox? (checkbox-locator? declared-locator)
                     corroborated?
                     (or (c8-locator-retained? expected-locator declared-locator
                                               after-locator)
                         (demo-corroborates? demo-receipt after-locator))
                     instrument (when (and observed checkbox?)
                                  (if corroborated? :corroborated :checkbox-only))
                     classification (cond problem nil
                                          (and observed checkbox? (not corroborated?))
                                          :checkbox-only
                                          observed :reached
                                          progress-row (if progress-valid? :progressed :invalid-progress)
                                          blocker-row (if (and (keyword? (:kind blocker-row))
                                                               (pinned-source? (:source blocker-row)))
                                                        :blocked :invalid-blocker)
                                          :else :untouched)]
                 (cond-> {:target target :want want
                          :before {:observed false
                                   :authority (if mission?
                                                :mission-observation
                                                :selected-initial-belief)}
                          :after {:observed observed :measurement measurement}
                          :classification classification}
                   instrument (assoc :instrument instrument)
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
            (assoc base :receipt/id (identity/digest base)))))))))
