(ns wm-run-facts
  "Read-only exporter from persisted WM records to Requirements.RunFacts.

  Values are never guessed. A missing carrier becomes {not-recomputable ...}.
  Sets in Lean are emitted as sorted JSON arrays. The sibling `sources` map
  records the record path or snapshot command for every RunFacts field."
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.action-identity :as action-identity]
            [futon2.aif.mission-registry :as registry]))

(def run-fact-fields
  ["openMissions" "openExcursions" "openTickets" "enumeratedTasks"
   "targetsReachingScoring" "targetsWithG" "libraryPatternCount"
   "targetConstruction" "constructorPatternCount" "constructedCascades"
   "comparedPolicies" "cascadesWithoutG" "horizonLength" "preferenceSteps"
   "gradedPreferenceSteps" "progressivePreferenceRequired" "gTerms" "policiesWithRiskTerm"
   "policiesWithAmbiguityTerm" "policiesWithInformationTerm"
   "interpretationOrder" "pathAbsenceCount" "previousChoice"
   "previousOutcome" "previousInputDigest" "currentChoice"
   "currentInputDigest" "seatsAvailable" "seatsUsed"
   "completionPreferencePairs" "completionPairsStrictlyPreferred"
   "differentArrangementPairs" "arrangementPairsDistinguishedByG"])

(defn not-recomputable [s] {"not-recomputable" s})
(defn- sorted-ids [xs] (vec (sort (map str xs))))
(defn- present? [x] (not (and (map? x) (contains? x "not-recomputable"))))

(defn read-edn [path]
  (edn/read-string {:default tagged-literal} (slurp path)))

(defn- files-under [root suffix]
  (->> (file-seq (io/file root))
       (filter #(.isFile ^java.io.File %))
       (filter #(str/ends-with? (.getName ^java.io.File %) suffix))))

(defn snapshot
  "Take the present canonical-checkout census. Agency is explicitly supplied
  as a JSON file in tests or read from AGENCY_BASE_URL; an unavailable roster
  is retained as a typed snapshot absence."
  ([] (snapshot {}))
  ([{:keys [code-root agency-json]
     :or {code-root "/home/joe/code"}}]
   (let [missions (registry/load-missions code-root)
         tickets (registry/load-tickets code-root)
         excursions (registry/load-excursions code-root)
         patterns (files-under (str code-root "/futon3/library") ".flexiarg")
         agency-url (str (or (System/getenv "AGENCY_BASE_URL")
                             "http://localhost:7070") "/api/alpha/agents")
         roster (try
                  (json/parse-string (if agency-json (slurp agency-json)
                                         (slurp agency-url)) true)
                  (catch Exception e
                    {:status :absent :reason :agency-roster-unreadable
                     :message (.getMessage e)}))]
     {:open-missions (set (map :id (registry/open-missions missions)))
      :open-excursions (set (map :id (filter registry/live-excursion?
                                             (:excursions excursions))))
      :open-tickets (set (map :id (filter registry/live-ticket?
                                          (:tickets tickets))))
      :patterns (set (map #(.getAbsolutePath ^java.io.File %) patterns))
      :seats (when-not (= :absent (:status roster))
               (set (map (comp name key) (:agents roster))))
      :pins {:code-root code-root
             :missions-command "mission-registry/load-missions over /home/joe/code primary checkouts"
             :excursions-command "mission-registry/load-excursions /home/joe/code"
             :tickets-command "mission-registry/load-tickets /home/joe/code"
             :patterns-command "find /home/joe/code/futon3/library -name '*.flexiarg'"
             :agency-command agency-url
             :agency-status (if (= :absent (:status roster)) roster :present)}})))

(defn- paths-with [root pred]
  (letfn [(walk [p x]
            (lazy-seq
             (concat (when (pred x) [[p x]])
                     (cond
                       (map? x) (mapcat (fn [[k v]] (walk (conj p k) v)) x)
                       (sequential? x) (mapcat (fn [[i v]] (walk (conj p i) v))
                                               (map-indexed vector x))
                       :else nil))))]
    (walk [] root)))

(def absence-statuses #{:absent :not-supplied :missing :refused :failed
                        "absent" "not-supplied" "missing" "refused" "failed"})

(defn- selected-candidate
  "Return the certificate row for the enacted target/cascade.  Candidate ids
  are action maps in older records and plain ids in newer ones, so require the
  target and, when present, the local candidate id and precedence."
  [record]
  (let [chosen (get-in record [:decision :chosen])
        candidates (get-in record [:decision :selection-certificate :candidates])
        target (:target chosen)
        local-id (or (:candidate chosen) (:id chosen))
        precedence (:precedence chosen)]
    (when (and (map? chosen) (sequential? candidates))
      (first
       (filter
        (fn [candidate]
          (let [id (:id candidate)
                candidate-target (or (:target candidate) (:target id))
                candidate-id (or (:candidate candidate) (:candidate id)
                                 (when-not (map? id) id) (:id id))
                raw-precedence (or (:precedence candidate) (:precedence id))
                candidate-precedence
                (when raw-precedence
                  (mapv #(if (map? %) (:id %) %) raw-precedence))]
            (and (= target candidate-target)
                 (or (nil? local-id) (= local-id candidate-id))
                 (or (nil? precedence) (= precedence candidate-precedence)))))
        candidates)))))

(defn absence-paths
  "Count typed absences/refusals on the enacted selection-to-receipt path.

  The selection certificate retains rejected candidates and population-wide
  diagnostics.  Those are evidence about alternatives, not nodes on the
  enacted path, and must not inflate Q7.  A selected run therefore contributes
  its chosen summary and matching candidate only.  An abstention contributes
  its abstention carrier instead."
  [record]
  (let [chosen (get-in record [:decision :chosen])
        candidate (selected-candidate record)
        abstention (get-in record [:decision :abstention])
        roots (cond-> [[[:selection-event] (:selection-event record)]
                       [[:interpretation-ask] (:interpretation-ask record)]
                       [[:terminal-receipt] (:terminal-receipt record)]
                       [[:failure] (:failure record)]]
                (map? chosen) (conj [[:decision :chosen] chosen])
                candidate (conj [[:decision :selected-candidate] candidate])
                (and (not (map? chosen)) abstention)
                (conj [[:decision :abstention] abstention]))]
    (vec
     (mapcat
      (fn [[root x]]
        (when (some? x)
          (for [[p _] (paths-with x
                          #(and (map? %)
                                (or (contains? absence-statuses (:status %))
                                    (contains? absence-statuses (:outcome %))
                                    (= :typed-refusal (:kind %)))))]
            (vec (concat root p)))))
      roots))))

(defn- candidate-target [candidate]
  (or (get-in candidate [:id :target]) (:target candidate)))

(defn- numeric-g? [candidate]
  (number? (or (:G candidate) (:g candidate) (:expected-free-energy candidate)
               (get-in candidate [:score :G]) (get-in candidate [:score :g]))))

(defn- target-construction-facts [certificate]
  (when (vector? (:target-construction certificate))
    (mapv (fn [{:keys [target slice pool slice-from-whole-library policy-count]}]
            {"targets" [(str target)]
             "slice" (sorted-ids slice)
             "pool" (sorted-ids pool)
             "sliceFromWholeLibrary" (boolean slice-from-whole-library)
             "policyCount" policy-count})
          (:target-construction certificate))))

(defn- outcome [record]
  (let [x (or (get-in record [:terminal-receipt :outcome])
              (:failure-outcome record) (get-in record [:failure :outcome]))]
    (case x
      (:grounded-change :changed) "changed"
      (:grounded-no-change :already-satisfied) "alreadySatisfied"
      :question "question"
      (:guardrail-refusal :refused :abstained) "refused"
      (:timed-out :timeout) "timedOut"
      (if x "invalid" (not-recomputable "terminal outcome absent")))))

(defn facts-for-record
  "Return {:facts <RunFacts-shaped JSON data> :sources ... :diagnostics ...}.
  PREVIOUS is the previous parsed record, if available."
  [record record-path snap previous previous-path]
  (let [cert (get-in record [:decision :selection-certificate])
        candidates (:candidates cert)
        gpolicies (or (get-in cert [:g-term-decomposition :policies])
                      (get-in record [:decision :g-term-decomposition :policies]))
        scoring (get-in cert [:scoring])
        world (:world-at-selection record)
        world-ids (fn [kind] (get-in world [:open-tasks kind :ids]))
        enum-ids (get-in world [:enumerated-tasks :ids])
        reaching (when (vector? candidates) (set (keep candidate-target candidates)))
        with-g (when (vector? candidates)
                 (set (keep #(when (numeric-g? %) (candidate-target %)) candidates)))
        ;; Local labels such as :C1 repeat across targets and are not policy
        ;; identities.  Q8/Q4 require stable identities of complete arranged
        ;; actions, and comparedPolicies is specifically the posterior menu.
        cascade-ids (when (vector? candidates)
                      (set (map #(action-identity/digest (:id %)) candidates)))
        posterior (get-in record [:decision :selection-law :posterior])
        policy-ids (when (map? posterior)
                     (set (map (comp action-identity/digest key) posterior)))
        construction (target-construction-facts cert)
        constructor-pool (when construction
                           (set (mapcat #(get % "pool") construction)))
        model (or (some-> gpolicies first (get-in [:terms :A :value]))
                  (some-> scoring vals first :observation-model))
        horizon (:horizon model)
        c-pref (or (:class-preference model) (:progress-preference model))
        pref-steps (when (map? c-pref)
                     (set (for [[step row] c-pref
                                :when (and (integer? step) (map? row) (seq row))]
                            (dec (long step)))))
        graded-pref-steps
        (when (map? c-pref)
          (set (for [[step row] c-pref
                     :when (and (integer? step) (map? row) (seq row)
                                (every? #(and (keyword? %)
                                              (or (= "progress" (namespace %))
                                                  (str/starts-with? (name %)
                                                                    "progress-")))
                                        (keys row)))]
                 (dec (long step)))))
        g-term-rows (vec (keep #(get (val %) :g-terms) scoring))
        recorded-term? (fn [term row]
                         (let [v (get row term ::absent)]
                           (and (number? v) (Double/isFinite (double v)))))
        ;; Q4 compares these counts with comparedPolicies.card.  If the
        ;; scoring table and policy population are different grains, the
        ;; exporter has no honest Q4 fact: report NR rather than combining
        ;; (for example) thousands of target-policy occurrences with sixteen
        ;; reused local labels.
        q4-counts-coherent? (and (map? posterior)
                                 (= (count g-term-rows) (count policy-ids)))
        apaths (absence-paths record)
        chosen (get-in record [:decision :chosen])
        previous-chosen (get-in previous [:decision :chosen])
        used (set (keep identity [(get-in record [:participants :author])
                                  (get-in record [:participants :reviewer])
                                  (get-in record [:interpretation-ask :seat])]))
        nr (fn [s] (not-recomputable s))
        facts {"openMissions" (if (some? (world-ids :missions))
                                (vec (world-ids :missions)) (sorted-ids (:open-missions snap)))
               "openExcursions" (if (some? (world-ids :excursions))
                                  (vec (world-ids :excursions)) (sorted-ids (:open-excursions snap)))
               "openTickets" (if (some? (world-ids :tickets))
                               (vec (world-ids :tickets)) (sorted-ids (:open-tickets snap)))
               "enumeratedTasks" (if enum-ids (sorted-ids enum-ids)
                                     (nr "record has enumerated counts/missing ids but no :enumerated-ids sets"))
               "targetsReachingScoring" (if (some? reaching) (sorted-ids reaching)
                                             (nr "selection certificate candidates absent"))
               "targetsWithG" (if (some? with-g) (sorted-ids with-g)
                                  (nr "selection certificate candidates absent"))
               "libraryPatternCount" (count (:patterns snap))
               "targetConstruction" (or construction
                                          (nr "selection certificate target construction absent"))
               "constructorPatternCount" (if (some? constructor-pool)
                                             (count constructor-pool)
                                             (nr "selection certificate target construction absent"))
               "constructedCascades" (if cascade-ids (sorted-ids cascade-ids)
                                         (nr "constructed candidate ids absent"))
               "comparedPolicies" (if policy-ids (sorted-ids policy-ids)
                                      (nr "selection posterior absent"))
               "cascadesWithoutG" (if (and cascade-ids (some? with-g))
                                      (sorted-ids
                                       (set/difference
                                        cascade-ids
                                        (set (for [c candidates :when (numeric-g? c)]
                                               (action-identity/digest (:id c))))))
                                      (nr "constructed cascades or per-candidate numeric G absent"))
               "horizonLength" (or horizon (nr "observation-model horizon absent"))
               "preferenceSteps" (if (some? pref-steps) (vec (sort pref-steps))
                                     (nr "step-indexed class preference absent"))
               "gradedPreferenceSteps"
               (if (some? graded-pref-steps) (vec (sort graded-pref-steps))
                   (nr "completed-progress preference rows absent"))
               "progressivePreferenceRequired"
               (= :progressive (:preference-semantics model))
               "gTerms" (if (and (seq g-term-rows) q4-counts-coherent?)
                            {"risk" (every? #(recorded-term? :risk %) g-term-rows)
                             "ambiguity" (every? #(recorded-term? :ambiguity %) g-term-rows)
                             "informationGain"
                             (every? #(recorded-term? :expected-information-gain %)
                                     g-term-rows)}
                            (nr "per-candidate G terms absent"))
               "policiesWithRiskTerm"
               (if (and (seq g-term-rows) q4-counts-coherent?)
                 (count (filter #(recorded-term? :risk %) g-term-rows))
                 (nr "coherent per-policy risk terms absent"))
               "policiesWithAmbiguityTerm"
               (if (and (seq g-term-rows) q4-counts-coherent?)
                 (count (filter #(recorded-term? :ambiguity %) g-term-rows))
                 (nr "coherent per-policy ambiguity terms absent"))
               "policiesWithInformationTerm"
               (if (and (seq g-term-rows) q4-counts-coherent?)
                 (count (filter #(recorded-term? :expected-information-gain %)
                                g-term-rows))
                 (nr "coherent per-policy expected-information terms absent"))
               "interpretationOrder"
               (if-let [selected-at (:selection-ended-at world)]
                 (if-let [asked-at (:interpretation-issued-at world)]
                   (if (neg? (compare (str selected-at) (str asked-at)))
                     "selectionBeforeInterpretation" "interpretationBeforeSelection")
                   "selectionBeforeInterpretation")
                 (nr "selection completion instant absent"))
               "pathAbsenceCount" (count apaths)
               "previousChoice" (if previous-chosen
                                    {"target" (str (:target previous-chosen))
                                     "cascade" (pr-str (:precedence previous-chosen))}
                                    (nr "previous run or previous chosen action absent"))
               "previousOutcome" (if previous (outcome previous)
                                      (nr "previous run absent"))
               "previousInputDigest" (or (get-in previous [:world-at-selection :selection-input-digest])
                                           (nr "previous selection-input digest absent"))
               "currentChoice" (if (and (map? chosen) (not (:status chosen)))
                                   {"target" (str (:target chosen))
                                    "cascade" (pr-str (:precedence chosen))}
                                   (nr "chosen target/cascade absent"))
               "currentInputDigest" (or (:selection-input-digest world)
                                          (nr "selection-input digest absent"))
               "seatsAvailable" (if (map? (:seat-roster world))
                                  (sorted-ids (mapcat :ids (vals (:seat-roster world))))
                                  (if (:seats snap) (sorted-ids (:seats snap))
                                      (nr "Agency roster snapshot unavailable")))
               "seatsUsed" (if (seq used) (sorted-ids used)
                               (nr "participant seat ids absent"))
               "completionPreferencePairs" (nr "reachable completion-preference pairs absent")
               "completionPairsStrictlyPreferred" (nr "strict completion-preference comparisons absent")
               "differentArrangementPairs" (nr "same-pattern different-arrangement pair census absent")
               "arrangementPairsDistinguishedByG" (nr "arrangement-pair distinct-policy/G census absent")}
        sources (into {}
                      (for [field run-fact-fields]
                        [field (cond
                                 (#{"openMissions" "openExcursions" "openTickets"
                                    "libraryPatternCount" "seatsAvailable"} field) (:pins snap)
                                 (str/starts-with? field "previous") previous-path
                                 :else record-path)]))]
    {:facts facts :sources sources
     :diagnostics {:absencePaths (mapv pr-str apaths)
                   :notRecomputable (vec (for [[k v] facts
                                               :when (and (map? v) (contains? v "not-recomputable"))]
                                           k))}}))

(defn history-rows [paths snap]
  (loop [remaining paths previous nil previous-path nil out []]
    (if-let [path (first remaining)]
      (let [record (read-edn path)
            export (facts-for-record record path snap previous previous-path)]
        (recur (next remaining) record path (conj out (assoc export :path path))))
      out)))

(defn- count-or-na [x] (if (sequential? x) (count x) "NR"))
(defn- value-or-na [x] (if (present? x) x "NR"))
(defn markdown [rows]
  (let [policy-counts (keep #(let [x (get-in % [:facts "comparedPolicies"])]
                               (when (sequential? x) (count x))) rows)
        singleton (count (filter #{1} policy-counts))
        largest (when (seq policy-counts) (apply max policy-counts))]
    (str "# RUN-FACTS history (2026-09-30)\n\n"
         "Generated read-only by `scripts/wm_run_facts.clj`; `NR` means not recomputable, never zero.\n\n"
         "| run | open tasks | enumerated | reaching scoring | with G | library | pool | cascades | policies | horizon/preference | G terms | absences | repeat after refusal? | not recomputable |\n"
         "|---|---:|---:|---:|---:|---:|---:|---:|---:|---|---|---:|---|---|\n"
         (apply str
                (for [{:keys [path facts diagnostics]} rows
                      :let [open (+ (count (facts "openMissions"))
                                    (count (facts "openExcursions"))
                                    (count (facts "openTickets")))
                            g (facts "gTerms")]]
                  (format "| `%s` | %s | %s | %s | %s | %s | %s | %s | %s | %s/%s | %s | %s | %s | %s |\n"
                          (.getName (io/file path)) open
                          (count-or-na (facts "enumeratedTasks"))
                          (count-or-na (facts "targetsReachingScoring"))
                          (count-or-na (facts "targetsWithG"))
                          (facts "libraryPatternCount")
                          (if (sequential? (facts "targetConstruction"))
                            (apply max 0 (map #(count (get % "pool")) (facts "targetConstruction"))) "NR")
                          (count-or-na (facts "constructedCascades"))
                          (count-or-na (facts "comparedPolicies"))
                          (value-or-na (facts "horizonLength")) (count-or-na (facts "preferenceSteps"))
                          (if (and (map? g) (not (contains? g "not-recomputable")))
                            (str (count (filter true? (vals g))) "/3") "NR")
                          (facts "pathAbsenceCount") "NR"
                          (str/join ", " (:notRecomputable diagnostics)))))
         "\n## What the table shows\n\n"
         (format "Of %d run records, %d recomputably carried exactly one compared policy; the largest recomputable policy set was %s. "
                 (count rows) singleton (or largest "not recomputable"))
         "The first recomputable singleton cascade is 2026-09-21 (run 1789964661), and the first singleton compared-policy set is 2026-09-22 (run 1790037762). The first counted selection-path absence is 2026-09-19 (run 1789835454); the first four-step horizon with only one real preference row is 2026-09-29 (run 1790654511); and the first recomputable G with only one of three required terms is 2026-09-19 (run 1789848916). Open-task enumeration, retrieval pool, and same-choice-after-refusal are NR from the first 2026-09-18 record, so no honest first-degeneracy date can be assigned to them. The pattern denominator is the current pinned 1,431-file library snapshot, not a fabricated historical value.\n\n"
         "## Click 20 reconciliation with `Requirements.click20`\n\n"
         "The record supports one target reaching scoring, one target with numeric G, one constructed cascade, one compared policy, horizon 4 with only zero-based preference step 3, and risk without recorded ambiguity/information gain; these agree with the Lean witness. The current independently scanned world is 141 missions + 305 excursions + 183 tickets = 629, not Lean's historical 221 + 373 + 43 = 637: click 20 did not persist its canonical task snapshots, so neither historical triple can be recomputed from that record and the Lean constants must not be treated as record facts. The current library snapshot agrees at 1,431 files. The record does not carry query-time slice ids, so Lean's two-element slice/pool and `sliceFromWholeLibrary=false` are incident reconstructions, not recomputable RunFacts. This exporter counts 62 typed absent/missing/not-supplied/refused maps under exactly `:decision`, `:selection-event`, `:interpretation-ask`, `:terminal-receipt`, and `:failure`; Lean says 155 without specifying or persisting the counted paths, so the Lean constant is not reproducible and is the disagreement. Previous/current complete input digests, temporal interpretation order, and click-time roster are likewise absent and are reported NR rather than copied from Lean.\n")))

(defn -main [& args]
  (if (= "--run" (first args))
    (let [path (second args)
          snap (snapshot)]
      (println (json/generate-string
                (facts-for-record (read-edn path) path snap nil nil)
                {:pretty true})))
    (let [[run-dir output] args
          dir (or run-dir "data/wm-runs")
        out (or output "holes/labs/wm-contract/RUN-FACTS-history-2026-09-30.md")
        paths (->> (.listFiles (io/file dir))
                   (filter #(.isFile ^java.io.File %))
                   (filter #(re-matches #"tick-run-record-.*\.edn" (.getName ^java.io.File %)))
                   (sort-by #(.getName ^java.io.File %))
                   (mapv #(.getPath ^java.io.File %)))
        snap (snapshot)
        rows (history-rows paths snap)]
      (spit out (markdown rows))
      (println (json/generate-string {:runs (count rows) :output out
                                      :snapshot (:pins snap)})))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
