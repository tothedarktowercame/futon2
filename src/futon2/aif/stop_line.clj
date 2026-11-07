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

(def legal-abstention-exclusion-reasons
  "Exclusion reasons whose removal of every scored field member is a legal
  terminal abstention.  Mirrors
  futon2.aif.full-loop-runtime/legal-terminal-exclusion-reasons, which commit
  345ebdfb9 established for :c1-already-satisfied and a later commit extended
  with :c3-harm-declined.  Selection mints an {:status :abstained
  :case :empty-field} outcome only after checking every exclusion carries one
  of these reasons."
  #{:c1-already-satisfied :c3-harm-declined})

(defn qualified-legal-abstention?
  "Whether an :abstained outcome retains the selection's own abstention
  record: the :empty-field case together with the checked exclusion evidence
  that emptied the field.  The case keyword alone is not enough — the close
  must see the exclusions, each with a legal reason, or the abstention is
  unexplained and still requires a finding."
  [{:keys [outcome selection-outcome outer-field/excluded]}]
  (and (= :abstained outcome)
       (map? selection-outcome)
       (= :abstained (:status selection-outcome))
       (= :empty-field (:case selection-outcome))
       (vector? excluded)
       (seq excluded)
       (every? #(contains? legal-abstention-exclusion-reasons (:reason %))
               excluded)))

(defn producer-qualified-already-satisfied?
  "Whether the producer retained the typed already-satisfied outcome together
  with the existing commit it validated."
  [{:keys [outcome failure-kind grounded-commit]}]
  (and (= :already-satisfied outcome)
       (= :already-satisfied failure-kind)
       (qualified-grounded-commit? grounded-commit)))

(defn no-finding-required?
  "Whether OUTCOME is a completed opportunity that requires no stop-line
  finding.  `:already-satisfied` is successful only when the runner-retained
  validated existing-commit qualification is present.  `:abstained` is exempt
  only when the close retains the selection's legal abstention record: the
  :empty-field case plus its checked exclusion evidence."
  [{:keys [outcome failure-kind grounded-commit reviewed-claim-refused?
           selection-outcome outer-field/excluded]}]
  (or (and (= :guardrail-refusal outcome)
           (or (true? reviewed-claim-refused?)
               (= :author-response-unparseable failure-kind)
               ;; An existing-artifact claim refused mechanically before
               ;; review because its commit does not cite the selected
               ;; target: the author's answer, not the apparatus, is wrong.
               (= :claimed-artifact-not-on-selected-target failure-kind)))
      (contains? #{:grounded-change
                   :grounded-progress
                   :historical-verification-awaiting-validation}
                 outcome)
      (producer-qualified-already-satisfied?
       {:outcome outcome :failure-kind outcome :grounded-commit grounded-commit})
      ;; A legal abstention is exempt only when its retained record — the
      ;; :empty-field case and the checked exclusions that emptied the
      ;; field — is visible at the close. Every other abstention (typed
      ;; refusal, missing case, missing or illegal exclusion evidence)
      ;; still opens a stop-line finding.
      (qualified-legal-abstention?
       {:outcome outcome
        :selection-outcome selection-outcome
        :outer-field/excluded excluded})))
