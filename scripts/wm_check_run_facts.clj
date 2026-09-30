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

(def deps
  {:q1 ["openMissions" "openExcursions" "openTickets" "enumeratedTasks"]
   :q2 ["openMissions" "openExcursions" "openTickets" "targetConstruction"
        "libraryPatternCount" "constructorPatternCount"]
   :q3 ["cascadesWithoutG"]
   :q4 ["horizonLength" "preferenceSteps" "gTerms"]
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
              (ev-known (format "horizonLength := %d, preferenceSteps := %s, gTerms := ⟨%s, %s, %s⟩"
                                (facts "horizonLength") (nat-fs (facts "preferenceSteps"))
                                (bool-lit (g "risk")) (bool-lit (g "ambiguity"))
                                (bool-lit (g "informationGain")))))
        :q7 (ev-known (str "pathAbsenceCount := " (facts "pathAbsenceCount")))
        ;; The current exporter cannot make these arms known. Keeping this
        ;; explicit makes a schema extension fail here instead of being guessed.
        (throw (ex-info "known field group has no Lean encoder" {:requirement q}))))))

(defn lean-source [export definition-name]
  (let [facts (get export "facts")]
    (str "import DarkTower.WarMachine.RequirementsReader\n"
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
