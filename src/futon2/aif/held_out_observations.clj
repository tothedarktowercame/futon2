(ns futon2.aif.held-out-observations
  "Pure hygiene boundary for a preregistered held-out observation window."
  (:require [futon2.aif.held-out-split :as split]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def schema :wm/eig-held-out-observations-v1)
(def disposition 'HELD-OUT-OBSERVATIONS-COLLECTED)

(defn- parse-instant [value]
  (when (string? value)
    (try
      (java.time.Instant/parse value)
      (catch java.time.format.DateTimeParseException _ nil))))

(defn- valid-row?
  [declaration {:keys [run-id target recorded-at close-sha256 outcome-class]}]
  (let [observed-at (parse-instant recorded-at)
        registered-at (parse-instant (:registered-at declaration))]
    (and (string? run-id)
         (= (:ticket/id declaration) target)
         observed-at
         registered-at
         (.isAfter observed-at registered-at)
         (boolean (re-matches #"[0-9a-f]{64}" (or close-sha256 "")))
         (contains? (set (:outcome-classes declaration)) outcome-class))))

(defn collect-window
  "Retain every supplied row and close only the first declared N distinct,
  valid, post-registration observations. Invalid and duplicate rows remain in
  :observations with a hygiene verdict; they never silently disappear."
  [declaration rows]
  (split/validate-v2 declaration)
  (let [seen (volatile! #{})
        retained (mapv (fn [row]
                         (let [duplicate? (contains? @seen (:run-id row))
                               _ (vswap! seen conj (:run-id row))
                               observed-at (parse-instant (:recorded-at row))
                               registered-at (parse-instant (:registered-at declaration))
                               reason (cond
                                        duplicate? :duplicate-run-id
                                        (not= (:ticket/id declaration) (:target row)) :different-target
                                        (not (string? (:recorded-at row))) :missing-recorded-at
                                        (nil? observed-at) :malformed-recorded-at
                                        (nil? registered-at) :malformed-registration-instant
                                        (not (.isAfter observed-at registered-at)) :before-registration
                                        (not (re-matches #"[0-9a-f]{64}" (or (:close-sha256 row) ""))) :invalid-close-digest
                                        (not (contains? (set (:outcome-classes declaration))
                                                        (:outcome-class row))) :unknown-outcome-class)]
                           (assoc row :hygiene (if (and (nil? reason)
                                                        (valid-row? declaration row))
                                                 :valid :invalid)
                                      :hygiene-reason reason)))
                       rows)
        valid (filterv #(= :valid (:hygiene %)) retained)
        n (get-in declaration [:window :next-n])
        closed? (>= (count valid) n)]
    (cond-> {:schema schema
             :split {:schema (:schema declaration)
                     :ticket/id (:ticket/id declaration)
                     :registered-at (:registered-at declaration)
                     :starting-point (:starting-point declaration)}
             :status (if closed? :closed :open)
             :required n
             :valid-count (count valid)
             :missing-count (max 0 (- n (count valid)))
             :observations retained
             :claims {:window-closed? closed?
                      :calibration-evidence-present? false
                      :restoration-accepted? false}}
      closed? (assoc :disposition disposition))))
