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
(defn ev-nr [facts ks]
  (let [missing (filter #(or (not (contains? facts %)) (nr? (get facts %))) ks)]
    (str ".nr \"not recomputable: " (str/join "," missing) "\"")))
(defn ev-known [body] (str ".known { Requirements.click20 with " body " }"))
(defn choice [x] (format "⟨%d, %d⟩" (id (x "target")) (id (x "cascade"))))
(defn target-construction [xs]
  (str "[" (str/join ", "
                     (for [x xs]
                       (format "{ targets := %s, slice := %s, pool := %s, sliceFromWholeLibrary := %s, policyCount := %d }"
                               (fs (x "targets")) (fs (x "slice")) (fs (x "pool"))
                               (bool-lit (x "sliceFromWholeLibrary")) (x "policyCount")))) "]"))
(def outcomes {"changed" ".changed" "alreadySatisfied" ".alreadySatisfied"
               "question" ".question" "refused" ".refused" "invalid" ".invalid"
               "timedOut" ".timedOut"})

(def deps
  {:q1 ["openMissions" "openExcursions" "openTickets" "enumeratedTasks"]
   :q2 ["openMissions" "openExcursions" "openTickets" "targetConstruction"
        "libraryPatternCount" "constructorPatternCount"]
   :q3 ["cascadesWithoutG"]
   :q4 ["horizonLength" "preferenceSteps" "gradedPreferenceSteps"
        "progressivePreferenceRequired" "gTerms"
        "policiesWithRiskTerm" "policiesWithAmbiguityTerm"
        "policiesWithInformationTerm" "comparedPolicies"]
   :q5 ["interpretationOrder"]
   :q6 ["previousChoice" "previousOutcome" "previousInputDigest"
        "currentChoice" "currentInputDigest"]
   :q7 ["pathAbsenceCount"]
   :q8 ["openMissions" "openExcursions" "openTickets" "enumeratedTasks"
        "targetsReachingScoring" "targetsWithG" "libraryPatternCount"
        "targetConstruction" "constructedCascades" "comparedPolicies"
        "seatsAvailable" "seatsUsed"]
   :q9 ["completionPreferencePairs" "completionPairsStrictlyPreferred"]
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
              (ev-known (format "horizonLength := %d, preferenceSteps := %s, gradedPreferenceSteps := %s, progressivePreferenceRequired := %s, gTerms := ⟨%s, %s, %s⟩, policiesWithRiskTerm := %d, policiesWithAmbiguityTerm := %d, policiesWithInformationTerm := %d, comparedPolicies := %s"
                                (facts "horizonLength") (nat-fs (facts "preferenceSteps"))
                                (nat-fs (facts "gradedPreferenceSteps"))
                                (bool-lit (facts "progressivePreferenceRequired"))
                                (bool-lit (g "risk")) (bool-lit (g "ambiguity"))
                                (bool-lit (g "informationGain"))
                                (facts "policiesWithRiskTerm")
                                (facts "policiesWithAmbiguityTerm")
                                (facts "policiesWithInformationTerm")
                                (fs (facts "comparedPolicies")))))
        :q2 (ev-known (format "openMissions := %s, openExcursions := %s, openTickets := %s, targetConstruction := %s, libraryPatternCount := %d, constructorPatternCount := %d"
                              (fs (facts "openMissions")) (fs (facts "openExcursions"))
                              (fs (facts "openTickets")) (target-construction (facts "targetConstruction"))
                              (facts "libraryPatternCount") (facts "constructorPatternCount")))
        :q5 (ev-known (str "interpretationOrder := ." (facts "interpretationOrder")))
        :q6 (ev-known (format "previousChoice := %s, previousOutcome := %s, previousInputDigest := %d, currentChoice := %s, currentInputDigest := %d"
                              (choice (facts "previousChoice")) (outcomes (facts "previousOutcome"))
                              (id (facts "previousInputDigest")) (choice (facts "currentChoice"))
                              (id (facts "currentInputDigest"))))
        :q7 (ev-known (str "pathAbsenceCount := " (facts "pathAbsenceCount")))
        :q8 (ev-known (format "openMissions := %s, openExcursions := %s, openTickets := %s, enumeratedTasks := %s, targetsReachingScoring := %s, targetsWithG := %s, libraryPatternCount := %d, targetConstruction := %s, constructedCascades := %s, comparedPolicies := %s, seatsAvailable := %s, seatsUsed := %s"
                              (fs (facts "openMissions")) (fs (facts "openExcursions"))
                              (fs (facts "openTickets")) (fs (facts "enumeratedTasks"))
                              (fs (facts "targetsReachingScoring")) (fs (facts "targetsWithG"))
                              (facts "libraryPatternCount") (target-construction (facts "targetConstruction"))
                              (fs (facts "constructedCascades")) (fs (facts "comparedPolicies"))
                              (fs (facts "seatsAvailable")) (fs (facts "seatsUsed"))))
        :q9 (ev-known (format "completionPreferencePairs := %d, completionPairsStrictlyPreferred := %d"
                              (facts "completionPreferencePairs") (facts "completionPairsStrictlyPreferred")))
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
