(ns wm-check-run-facts
  "Generate a Lean term from one wm_run_facts JSON export.

  NR remains RequirementsReader.Evidence.nr. Known requirements are evaluated
  by Lean from the exported values; no NR field is replaced by a passing value."
  (:require [cheshire.core :as json]
            [clojure.string :as str]))

(defn nr? [x] (and (map? x) (contains? x "not-recomputable")))
(defn known? [facts ks] (every? #(and (contains? facts %) (not (nr? (get facts %)))) ks))
(defn id [x] (bit-and 0x7fffffff (hash (str x))))
(defn fs [xs] (str "{" (str/join ", " (map id xs)) "}"))
(defn nat-fs [xs] (str "{" (str/join ", " xs) "}"))
(defn bool-lit [x] (if x "true" "false"))
(defn field [m k] (or (get m k) (get m (name k))))
(declare digest-term)
(defn calibration-term [calibration]
  (format "{%s := %d, version := %d, authority := %d, registrationToken := %d, focusedCost := %d, relatedCost := %d, unrelatedCost := %d}"
          "identity" (field calibration :identity) (field calibration :version)
          (field calibration :authority) (field calibration :registration-token)
          (field calibration :focused-cost) (field calibration :related-cost)
          (field calibration :unrelated-cost)))
(defn ev-nr [facts ks]
  (let [missing (filter #(or (not (contains? facts %)) (nr? (get facts %))) ks)]
    (str ".nr \"not recomputable: " (str/join "," missing) "\"")))
(defn ev-known [body] (str ".known { Requirements.click20 with " body " }"))
(defn decision
  "A Requirements.Decision: a pick, or one of the two abstentions L3b admits.
  Any other shape renders as no constructor, which Lean refuses to elaborate."
  [x]
  (cond (and (map? x) (contains? x "abstain-on-task"))
        (format ".abstainOnTask %d" (id (x "abstain-on-task")))
        (= "abstain-empty-field" x) ".abstainEmptyField"
        (and (map? x) (contains? x "target") (contains? x "cascade"))
        (format "(.pick %d %d)" (id (x "target")) (id (x "cascade")))
        :else ".unknownDecision"))
(defn previous-decision
  "The predecessor's optional decision.  A refused-before-choice carrier is a
  known `none`; so is a retained abstention which has no Lean Decision.
  Every actual decision is wrapped in `some`."
  [x]
  (if (or (= "none" x)
          (and (map? x) (= true (x "refused"))))
    "none"
    (str "some " (decision x))))
(defn target-construction
  "An absent pin or digest renders 0 (id of the empty string), which Q2
  refuses."
  [xs]
  (str "[" (str/join ", "
                     (for [x xs]
                       (format "{ targets := %s, action := %d, reading := %d, library := %d, graph := %d, kernel := %d, slice := %s, pool := %s, sliceFromWholeLibrary := %s, policyCount := %d, policies := %s }"
                               (fs (x "targets")) (digest-term (x "action")) (id (x "reading"))
                               (id (x "library")) (id (x "graph")) (id (x "kernel"))
                               (fs (x "slice")) (fs (x "pool"))
                               (bool-lit (x "sliceFromWholeLibrary")) (x "policyCount")
                               (fs (x "policies"))))) "]"))
(defn outer-actions [xs]
  (str "[" (str/join ", " (for [x xs] (format "⟨%d, %d⟩" (id (x "task")) (id (x "action"))))) "]"))
(def semantics {"terminal-only" "terminalOnly" "constant" "constant"
                "progressive" "progressive"})
(defn policy-preferences
  "Each compared policy's own horizon, steps, grading, semantics, G
  normalization and terms. An unknown semantics or normalization name
  renders as no constructor, which Lean refuses to elaborate."
  [ps]
  (str "[" (str/join ", "
                     (for [p ps
                           :let [t (p "terms")]]
                       (format "{ policy := %d, horizon := %d, preferenceSteps := %s, gradedPreferenceSteps := %s, semantics := .%s, normalization := .%s, terms := ⟨%s, %s, %s⟩ }"
                               (id (p "policy")) (p "horizon")
                               (nat-fs (p "preferenceSteps")) (nat-fs (p "gradedPreferenceSteps"))
                               (semantics (p "semantics")) (p "normalization")
                               (bool-lit (t "risk")) (bool-lit (t "ambiguity"))
                               (bool-lit (t "informationGain"))))) "]"))
(defn entry-term [entry]
  (format "{task := %d, observation := %d, evidence := %d, rankingLaw := %d, rankingVersion := %d, dependencies := %s, comparisonKey := %s, freshnessEpoch := %d}"
          (id (field entry :task)) (id (field entry :observation))
          (id (field entry :evidence)) (id (field entry :ranking-law))
          (field entry :ranking-version) (fs (field entry :dependencies))
          (str (biginteger (field entry :comparison-key))) (field entry :freshness-epoch)))
(defn census-term
  "An exclusion with no recorded reason renders reason 0, which Lean refuses."
  [census]
  (format "{eligible := %s, excluded := [%s]}"
          (fs (field census :eligible))
          (str/join ", " (for [x (field census :excluded)]
                           (format "{task := %d, reason := %d}" (id (field x :task))
                                   (if-let [r (field x :reason)] (max 1 (id r)) 0))))))
(defn entry-list [entries]
  (str "[" (str/join ", " (map entry-term entries)) "]"))
(defn refresh-term [refresh]
  (let [delta (field refresh :delta)]
    (format "{prior := %s, current := %s, retained := %s, invalidated := %s, recomputed := %s, removed := %s, delta := {changedObservations := %s, addedTasks := %s, removedTasks := %s}, currentOpenTasks := %s}"
            (entry-list (field refresh :prior)) (entry-list (field refresh :current))
            (entry-list (field refresh :retained)) (entry-list (field refresh :invalidated))
            (entry-list (field refresh :recomputed)) (fs (field refresh :removed))
            (fs (field delta :changed-observations)) (fs (field delta :added-tasks))
            (fs (field delta :removed-tasks)) (fs (field refresh :current-open-tasks)))))
(defn selection-term [selection]
  (format "{selected := %s, currentEpoch := %d}"
          (entry-term (field selection :selected)) (field selection :current-epoch)))
(defn cascade-term [cascade]
  (format "{selectedTask := %d, availableAdmissible := %s, constructed := %s, admitted := %s, scored := %s, requirementExcluded := %s, posterior := %s}"
          (id (field cascade :selected-task))
          (fs (field cascade :available-admissible)) (fs (field cascade :constructed))
          (fs (field cascade :admitted)) (fs (field cascade :scored))
          (fs (field cascade :requirement-excluded))
          (fs (field cascade :posterior))))
(def run-kinds {"work" ".work" "selection" ".selection"})
(defn kind-term
  "An unknown kind renders as no constructor, which Lean refuses to elaborate."
  [kind]
  (get run-kinds kind ".unknownRunKind"))
(defn digest-term
  "A numeric digest is kept as it is: `id` would hash an explicit 0, the
  recorded absence, to a non-zero id."
  [x]
  (if (integer? x) x (id x)))
(defn provenance-term
  "An unknown provenance renders as no constructor, which Lean refuses."
  [x]
  (cond (= "defaulted" x) ".defaulted"
        (= "absent" x) ".absent"
        (and (map? x) (contains? x "observed")) (str ".observed " (digest-term (x "observed")))
        :else ".unknownProvenance"))
(defn run-provenance [[run provenance]]
  (format "(%d, %s)" (id run) (provenance-term provenance)))
(defn selection-path-term [path]
  (format "some { receipt := %s, pickRelation := %s, pickReach := %s, pendingRun := %s, consumedRuns := [%s], learningApplied := %s }"
          (provenance-term (path "receipt"))
          (provenance-term (path "pickRelation"))
          (provenance-term (path "pickReach"))
          (if-let [pending (path "pendingRun")]
            (str "some " (run-provenance pending))
            "none")
          (str/join ", " (map run-provenance (path "consumedRuns")))
          (bool-lit (path "learningApplied"))))
(defn exact-rational?
  "An exact numerator and positive denominator.  A float is not one, and neither
  is n/0, which Lean would read as 0."
  [x]
  (and (map? x) (integer? (x "numerator"))
       (integer? (x "denominator")) (pos? (x "denominator"))))
(defn rational-term [x]
  (format "(%s / %s)" (x "numerator") (x "denominator")))
(defn exact-g? [facts]
  (and (exact-rational? (facts "abstentionG"))
       (sequential? (facts "policyG"))
       (every? #(exact-rational? (% "g")) (facts "policyG"))))
(defn policy-g [rows]
  (str "[" (str/join ", " (for [row rows]
                            (format "(%d, %s)" (id (row "policy")) (rational-term (row "g")))))
       "]"))
(defn policy-prediction [rows]
  (str "[" (str/join ", " (for [row rows]
                            (format "(%d, %s)" (id (row "policy")) (digest-term (row "digest")))))
       "]"))
(defn nested-nr?
  "A construction can carry an NR inside it.  `fs` would hash that map into
  ids, so an inner NR makes the whole carrier not recomputable."
  [x]
  (some nr? (tree-seq coll? seq x)))
(defn refusal-history
  "L5-2L-2: every refused pick retained for this task, as (cascade, input digest)
  in the same id spaces as currentDecision and the input digests."
  [xs]
  (str "[" (str/join ", " (for [x xs] (format "(%d, %d)" (id (x "cascade")) (id (x "input-digest"))))) "]"))
(def outcomes {"changed" ".changed" "alreadySatisfied" ".alreadySatisfied"
               "question" ".question" "refused" ".refused" "invalid" ".invalid"
               "timedOut" ".timedOut" "abstained" ".abstained"})

(def deps
  {:q1 ["openMissions" "openExcursions" "openTickets" "enumeratedTasks"]
   :q2 ["kind" "targetConstruction" "libraryPatternCount" "constructorPatternCount"
        "comparedPolicies" "constructedCascades" "enumeratedTasks" "currentChoice"]
   :q3 ["cascadesWithoutG"]
   :q4 ["policyPreferences" "gTerms"
        "policiesWithRiskTerm" "policiesWithAmbiguityTerm"
        "policiesWithInformationTerm" "comparedPolicies"]
   :q5 ["interpretationOrder"]
   ;; L5-2L: Q6 forbids the refused cascade's prediction class, read from the
   ;; compared policies' prediction digests.
   :q6 ["previousChoice" "previousOutcome" "previousInputDigest"
        "currentChoice" "currentInputDigest" "comparedPolicies" "policyPrediction" "refusalHistory"
        "refusalHistoryUnsealed"]
   :q7 ["pathAbsenceCount" "kind" "selectionPath"]
   :q9 ["factorizedPreference" "calibrationAuthorizations"
        "factorizationAuthorizations"]
   ;; L5-2L: Q11's minimum is over the policies Q6 does not forbid, so it reads
   ;; the previous run's refusal and the input digests too.
   :q11 ["currentChoice" "comparedPolicies" "constructedCascades" "targetConstruction" "abstentionPrediction"
         "enumeratedTasks" "kind" "policyG" "abstentionG" "policyPrediction"
         "previousOutcome" "previousInputDigest" "currentInputDigest" "refusalHistory"]})

(defn required-keys
  "A work run has no selection path to export, so Q7 does not wait on one.  A
  selection run does, and a path on a work run is rendered for Q7 to refuse."
  [facts q]
  (if (and (= :q7 q) (= "work" (facts "kind")) (not (contains? facts "selectionPath")))
    (remove #{"selectionPath"} (deps q))
    (deps q)))

(defn partial-run-fact-fields
  "Read the field names from Lean's PartialRunFacts declaration.  This keeps
  the generated structure literal tied to the specification that Lean will
  compile it against."
  [reader-source]
  (let [body (second (re-find
                      #"(?s)structure\s+PartialRunFacts\s+where\s*\n(.*?)(?:\n\s*\n|\n(?:def|theorem|inductive|structure)\s)"
                      reader-source))
        fields (when body
                 (mapv (comp keyword second)
                       (re-seq #"(?m)^\s+([A-Za-z][A-Za-z0-9_]*)\s*:" body)))]
    (when-not (seq fields)
      (throw (ex-info "could not read fields from Lean structure PartialRunFacts" {})))
    fields))

(defn evidence [facts q]
  (let [ks (required-keys facts q)]
    (cond
      (and (= q :q11) (= "selection" (facts "kind"))
           (str/starts-with? (str (get-in facts ["currentChoice" "not-recomputable"]))
                             "selection refused: "))
      (str ".nr " (pr-str (get-in facts ["currentChoice" "not-recomputable"])))
      (and (= q :q5) (= "no interpretation in this run"
                                      (get-in facts ["interpretationOrder" "not-recomputable"])))
      (str ".nr " (pr-str "no interpretation in this run"))
      (not (known? facts ks)) (ev-nr facts ks)
      (and (#{:q2 :q11} q) (nested-nr? (facts "targetConstruction")))
      ".nr \"not recomputable: targetConstruction carries a not-recomputable value\""
      (and (= :q11 q) (not (exact-g? facts)))
      ".nr \"G not projected at a declared scale\""
      (and (= :q7 q) (= "selection" (facts "kind")) (nil? (facts "selectionPath")))
      ".nr \"not recomputable: selection run without selectionPath\""
      :else
      (case q
        :q1 (ev-known (format "openMissions := %s, openExcursions := %s, openTickets := %s, enumeratedTasks := %s"
                              (fs (facts "openMissions")) (fs (facts "openExcursions"))
                              (fs (facts "openTickets")) (fs (facts "enumeratedTasks"))))
        :q2 (ev-known (format "kind := %s, enumeratedTasks := %s, currentDecision := %s, targetConstruction := %s, libraryPatternCount := %s, constructorPatternCount := %s, comparedPolicies := %s, constructedCascades := %s"
                              (kind-term (facts "kind")) (fs (facts "enumeratedTasks"))
                              (decision (facts "currentChoice"))
                              (target-construction (facts "targetConstruction"))
                              (facts "libraryPatternCount") (facts "constructorPatternCount")
                              (fs (facts "comparedPolicies")) (fs (facts "constructedCascades"))))
        :q3 (ev-known (str "cascadesWithoutG := " (fs (facts "cascadesWithoutG"))))
        :q4 (let [g (facts "gTerms")]
              (ev-known (format "policyPreferences := %s, gTerms := ⟨%s, %s, %s⟩, policiesWithRiskTerm := %d, policiesWithAmbiguityTerm := %d, policiesWithInformationTerm := %d, comparedPolicies := %s"
                                (policy-preferences (facts "policyPreferences"))
                                (bool-lit (g "risk")) (bool-lit (g "ambiguity"))
                                (bool-lit (g "informationGain"))
                                (facts "policiesWithRiskTerm")
                                (facts "policiesWithAmbiguityTerm")
                                (facts "policiesWithInformationTerm")
                                (fs (facts "comparedPolicies")))))
        :q5 (ev-known (str "interpretationOrder := ." (facts "interpretationOrder")))
        :q6 (ev-known (format "previousDecision := %s, previousOutcome := %s, previousInputDigest := %d, currentDecision := %s, currentInputDigest := %d, comparedPolicies := %s, policyPrediction := %s, refusalHistory := %s, refusalHistoryUnsealed := %d"
                              (previous-decision (facts "previousChoice")) (outcomes (facts "previousOutcome"))
                              (id (facts "previousInputDigest")) (decision (facts "currentChoice"))
                              (id (facts "currentInputDigest"))
                              (fs (facts "comparedPolicies"))
                              (policy-prediction (facts "policyPrediction"))
                              (refusal-history (facts "refusalHistory"))
                              (facts "refusalHistoryUnsealed")))
        :q7 (ev-known (format "pathAbsenceCount := %s, kind := %s, selectionPath := %s"
                              (facts "pathAbsenceCount") (kind-term (facts "kind"))
                              (if-let [path (facts "selectionPath")]
                                (selection-path-term path)
                                "none")))
        :q11 (ev-known (format "kind := %s, enumeratedTasks := %s, currentDecision := %s, targetConstruction := %s, comparedPolicies := %s, constructedCascades := %s, policyG := %s, abstentionG := %s, policyPrediction := %s, abstentionPrediction := %s, previousOutcome := %s, previousInputDigest := %d, currentInputDigest := %d, refusalHistory := %s"
                               (kind-term (facts "kind")) (fs (facts "enumeratedTasks"))
                               (decision (facts "currentChoice"))
                               (target-construction (facts "targetConstruction"))
                               (fs (facts "comparedPolicies")) (fs (facts "constructedCascades"))
                               (policy-g (facts "policyG")) (rational-term (facts "abstentionG"))
                               (policy-prediction (facts "policyPrediction"))
                               (digest-term (facts "abstentionPrediction"))
                               (outcomes (facts "previousOutcome"))
                               (id (facts "previousInputDigest")) (id (facts "currentInputDigest"))
                               (refusal-history (facts "refusalHistory"))))
        :q9 (let [certificate (facts "factorizedPreference")
                  calibration (field certificate :calibration)
                  selected-calibration-term (when calibration (calibration-term calibration))
                  calibration-registry
                  (str "[" (str/join ", "
                                  (for [entry (facts "calibrationAuthorizations")]
                                    (format "{receipt := %d, calibration := %s, authorizedBy := %d}"
                                            (field entry :receipt)
                                            (calibration-term (field entry :calibration))
                                            (field entry :authorized-by)))) "]")
                  factorization-registry
                  (str "[" (str/join ", "
                                  (for [entry (facts "factorizationAuthorizations")]
                                    (format "{receipt := %d, identity := %d, version := %d, authority := %d, law := .completionThenConditionalRelevance}"
                                            (field entry :receipt) (field entry :identity)
                                            (field entry :version) (field entry :authority)))) "]")]
              (ev-known
               (if (nil? certificate)
                 ;; a verified negative carrier: the C that selected is not a
                 ;; factorized preference, which Q9 reads as false
                 (format "factorizedPreference := none, calibrationAuthorizations := %s, factorizationAuthorizations := %s"
                         calibration-registry factorization-registry)
                 (format "factorizedPreference := some {calibration := %s, selectorAuthority := %d, factorizationIdentity := %d, factorizationVersion := %d, factorizationAuthority := %d, calibrationAuthorization := %d, factorizationAuthorization := %d}, calibrationAuthorizations := %s, factorizationAuthorizations := %s"
                         selected-calibration-term (field certificate :selector-authority)
                         (field certificate :factorization-identity)
                         (field certificate :factorization-version)
                         (field certificate :factorization-authority)
                         (field certificate :calibration-authorization)
                         (field certificate :factorization-authorization)
                         calibration-registry factorization-registry))))))))

(defn lean-source
  ([export definition-name]
   (lean-source export definition-name [:q1 :q2 :q3 :q4 :q5 :q6 :q7 :q9 :q11]))
  ([export definition-name partial-fields]
   (let [unknown (seq (remove deps partial-fields))]
     (when unknown
       (throw (ex-info "PartialRunFacts has fields with no generator clause"
                       {:fields (vec unknown)}))))
  (let [facts (get export "facts")]
    (str "import DarkTower.WarMachine.RequirementsReader\n"
         "set_option maxRecDepth 100000\n"
         "set_option maxHeartbeats 5000000\n"
         "open DarkTower.WarMachine\n"
         "open DarkTower.WarMachine.Requirements\n"
         "open DarkTower.WarMachine.RequirementsReader\n"
         "def " definition-name " : PartialRunFacts where\n"
         (apply str (for [q partial-fields]
                      (str "  " (name q) " := " (evidence facts q) "\n")))
         "#eval outcomes " definition-name "\n"))))

(defn -main [& [json-path lean-path definition-name reader-path]]
  (when-not (and json-path lean-path reader-path)
    (throw (ex-info "usage: wm_check_run_facts.clj EXPORT.json OUTPUT.lean DEFINITION REQUIREMENTS_READER.lean" {})))
  (let [partial-fields (partial-run-fact-fields (slurp reader-path))]
    (spit lean-path (lean-source (json/parse-string (slurp json-path))
                                 (or definition-name "exported")
                                 partial-fields))))
