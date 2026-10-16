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
(defn choice [x] (format "⟨%d, %d⟩" (id (x "target")) (id (x "cascade"))))
(defn target-construction
  "An absent pin or digest renders 0 (id of the empty string), which Q2
  refuses."
  [xs]
  (str "[" (str/join ", "
                     (for [x xs]
                       (format "{ targets := %s, action := %d, reading := %d, library := %d, graph := %d, kernel := %d, slice := %s, pool := %s, sliceFromWholeLibrary := %s, policyCount := %d, policies := %s }"
                               (fs (x "targets")) (id (x "action")) (id (x "reading"))
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
(def outcomes {"changed" ".changed" "alreadySatisfied" ".alreadySatisfied"
               "question" ".question" "refused" ".refused" "invalid" ".invalid"
               "timedOut" ".timedOut"})

(def deps
  {:q1 ["openMissions" "openExcursions" "openTickets" "enumeratedTasks"]
   :q2 ["outerActions" "targetConstruction" "libraryPatternCount"
        "constructorPatternCount" "frontierCensus" "frontierSelection"
        "selectedCascade" "comparedPolicies"]
   :q3 ["cascadesWithoutG"]
   :q4 ["policyPreferences" "gTerms"
        "policiesWithRiskTerm" "policiesWithAmbiguityTerm"
        "policiesWithInformationTerm" "comparedPolicies"]
   :q5 ["interpretationOrder"]
   :q6 ["previousChoice" "previousOutcome" "previousInputDigest"
        "currentChoice" "currentInputDigest"]
   :q7 ["pathAbsenceCount"]
   :q8 ["openMissions" "openExcursions" "openTickets" "frontierCensus"
        "frontierRefresh" "frontierSelection" "selectedCascade"]
   :q9 ["factorizedPreference" "calibrationAuthorizations"
        "factorizationAuthorizations"]
   :q10 ["differentArrangementPairs" "arrangementPairsDistinguishedByG"]})

(defn evidence [facts q]
  (let [ks (deps q)]
    (if-not (known? facts ks)
      (ev-nr facts ks)
      (case q
        :q1 (ev-known (format "openMissions := %s, openExcursions := %s, openTickets := %s, enumeratedTasks := %s"
                              (fs (facts "openMissions")) (fs (facts "openExcursions"))
                              (fs (facts "openTickets")) (fs (facts "enumeratedTasks"))))
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
        :q2 (ev-known (format "outerActions := %s, targetConstruction := %s, libraryPatternCount := %d, constructorPatternCount := %d, frontierCensus := %s, frontierSelection := %s, selectedCascade := %s, comparedPolicies := %s"
                              (outer-actions (facts "outerActions"))
                              (target-construction (facts "targetConstruction"))
                              (facts "libraryPatternCount") (facts "constructorPatternCount")
                              (census-term (facts "frontierCensus"))
                              (selection-term (facts "frontierSelection"))
                              (cascade-term (facts "selectedCascade"))
                              (fs (facts "comparedPolicies"))))
        :q5 (ev-known (str "interpretationOrder := ." (facts "interpretationOrder")))
        :q6 (ev-known (format "previousChoice := %s, previousOutcome := %s, previousInputDigest := %d, currentChoice := %s, currentInputDigest := %d"
                              (choice (facts "previousChoice")) (outcomes (facts "previousOutcome"))
                              (id (facts "previousInputDigest")) (choice (facts "currentChoice"))
                              (id (facts "currentInputDigest"))))
        :q7 (ev-known (str "pathAbsenceCount := " (facts "pathAbsenceCount")))
        ;; Q8 reads the Q6 facts only to justify a requirement exclusion:
        ;; with none, previousOutcome .invalid makes the justification empty
        :q8 (let [q6? (known? facts (deps :q6))
                  excluded? (seq (field (facts "selectedCascade") :requirement-excluded))]
              (if (and excluded? (not q6?))
                (ev-nr facts (into (deps :q8) (deps :q6)))
                (ev-known (format "openMissions := %s, openExcursions := %s, openTickets := %s, frontierCensus := %s, frontierRefresh := %s, frontierSelection := %s, selectedCascade := %s, %s"
                                  (fs (facts "openMissions")) (fs (facts "openExcursions"))
                                  (fs (facts "openTickets"))
                                  (census-term (facts "frontierCensus"))
                                  (refresh-term (facts "frontierRefresh"))
                                  (selection-term (facts "frontierSelection"))
                                  (cascade-term (facts "selectedCascade"))
                                  (if q6?
                                    (format "previousChoice := %s, previousOutcome := %s, previousInputDigest := %d, currentChoice := %s, currentInputDigest := %d"
                                            (choice (facts "previousChoice")) (outcomes (facts "previousOutcome"))
                                            (id (facts "previousInputDigest")) (choice (facts "currentChoice"))
                                            (id (facts "currentInputDigest")))
                                    "previousOutcome := .invalid")))))
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
                         calibration-registry factorization-registry))))
        :q10 (ev-known (format "differentArrangementPairs := %d, arrangementPairsDistinguishedByG := %d"
                               (facts "differentArrangementPairs") (facts "arrangementPairsDistinguishedByG")))))))

(defn lean-source [export definition-name]
  (let [facts (get export "facts")]
    (str "import DarkTower.WarMachine.RequirementsReader\n"
         "set_option maxRecDepth 100000\n"
         "set_option maxHeartbeats 5000000\n"
         "open DarkTower.WarMachine\n"
         "open DarkTower.WarMachine.Requirements\n"
         "open DarkTower.WarMachine.RequirementsReader\n"
         "def " definition-name " : PartialRunFacts where\n"
         (apply str (for [q (map #(keyword (str "q" %)) (range 1 11))]
                      (str "  " (name q) " := " (evidence facts q) "\n")))
         "#eval alert " definition-name "\n")))

(defn -main [& [json-path lean-path definition-name]]
  (when-not (and json-path lean-path)
    (throw (ex-info "usage: wm_check_run_facts.clj EXPORT.json OUTPUT.lean" {})))
  (spit lean-path (lean-source (json/parse-string (slurp json-path))
                               (or definition-name "exported"))))
