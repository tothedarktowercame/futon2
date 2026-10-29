(ns futon2.aif.stop-line
  "One rule for whether a completed opportunity needs a repair finding.

  This namespace has no runner or tripwire dependency so both producers can
  apply the same rule without introducing a require cycle."
  (:require [clojure.string :as str]))

(defn qualified-grounded-commit?
  "True only for the retained shape emitted after the runner validated an
  existing commit.  Commit existence is established by the runner's producer;
  this shared rule checks that the qualification survived into its consumer."
  [grounded-commit]
  (and (map? grounded-commit)
       (string? (:repo grounded-commit))
       (not (str/blank? (:repo grounded-commit)))
       (string? (:sha grounded-commit))
       (boolean (re-matches #"[0-9a-f]{7,40}" (:sha grounded-commit)))))

(defn no-finding-required?
  "Whether OUTCOME is a completed opportunity that requires no stop-line
  finding.  `:already-satisfied` is successful only when the runner-retained
  validated existing-commit qualification is present."
  [{:keys [outcome grounded-commit]}]
  (or (contains? #{:grounded-change
                   :grounded-progress
                   :historical-verification-awaiting-validation}
                 outcome)
      (and (= :already-satisfied outcome)
           (qualified-grounded-commit? grounded-commit))))
