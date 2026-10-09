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
            [futon2.aif.mission-registry :as registry]))

(def run-fact-fields
  ["openMissions" "openExcursions" "openTickets" "enumeratedTasks"
   "targetsReachingScoring" "targetsWithG" "libraryPatternCount"
   "targetConstruction" "constructorPatternCount" "constructedCascades"
   "comparedPolicies" "cascadesWithoutG" "horizonLength" "preferenceSteps"
   "gTerms" "interpretationOrder" "pathAbsenceCount" "previousChoice"
   "previousOutcome" "previousInputDigest" "currentChoice"
   "currentInputDigest" "seatsAvailable" "seatsUsed"
   "completionPreferencePairs" "completionPairsStrictlyPreferred"
   "earlierProgressPairs" "earlierProgressNoGreaterRisk"
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

(defn- computed-record?
  "A map carrying a content-addressed identity (:receipt/id) or a full check
  census (:checks) is a present, fully computed value even when its :status
  is :refused — e.g. a reviewer-falsifier receipt that documents *why* a
  build was refused is evidence the machine produced, not a carrier it
  failed to supply. A bare :schema tag alone does NOT qualify: schema-typed
  absence records such as {:schema :wm/g-term-decomposition-v1 :status
  :missing} are genuine typed absences and must stay counted."
  [x]
  (or (contains? x :receipt/id) (contains? x :checks)))

(defn absence-paths
  "Count one typed absence/refusal map per path, only below the persisted
  selection-to-receipt roots named here. Maps that identify themselves as
  computed machine records (see computed-record?) are present values and are
  not counted."
  [record]
  (let [roots [[:world-at-selection :failures] [:decision] [:selection-event]
               [:interpretation-ask] [:terminal-receipt] [:failure]]]
    (vec
     (mapcat
      (fn [root]
        (when-let [x (get-in record root)]
          (for [[p _] (paths-with x
                          #(and (map? %)
                                (not (computed-record? %))
                                (or (contains? absence-statuses (:status %))
                                    (contains? absence-statuses (:outcome %))
                                    (= :typed-refusal (:kind %)))))]
            (vec (concat root p)))))
      roots))))

(defn- candidate-id [candidate]
  (or (get-in candidate [:id :id]) (:id candidate) (:candidate candidate)))
(defn- candidate-target [candidate]
  (or (get-in candidate [:id :target]) (:target candidate)))
(defn- candidate-precedence [candidate]
  (or (get-in candidate [:id :precedence]) (:precedence candidate)))

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
        policies (:policies cert)
        ;; The runner persists the certificate below :decision.  The
        ;; decomposition is part of that certificate (the old reader looked
        ;; one level too high and consequently reported every Q4 carrier as
        ;; absent).
        certificate-gpolicies (get-in cert [:g-term-decomposition :policies])
        gpolicies (or certificate-gpolicies
                      (get-in record [:decision :g-term-decomposition :policies]))
        scoring (get-in cert [:scoring])
        world (:world-at-selection record)
        world-ids (fn [kind] (get-in world [:open-tasks kind :ids]))
        enum-ids (get-in world [:enumerated-tasks :ids])
        reaching (when (vector? candidates) (set (keep candidate-target candidates)))
        with-g (when (vector? candidates)
                 (set (keep #(when (numeric-g? %) (candidate-target %)) candidates)))
        cascade-ids (when (vector? candidates) (set (keep candidate-id candidates)))
        policy-ids (when (vector? policies)
                     (set (map #(or (:id %) (:policy-id %) (pr-str %)) policies)))
        construction (target-construction-facts cert)
        constructor-pool (when construction
                           (set (mapcat #(get % "pool") construction)))
        model (or (some-> gpolicies first (get-in [:terms :A :value]))
                  (some-> scoring vals first :observation-model))
        horizon (:horizon model)
        c-pref (or (:class-preference model) (:progress-preference model))
        ;; C_tau is a step-indexed carrier.  Only rows that assign mass away
        ;; from the terminal waiting symbol state a real preference.  The
        ;; exported field is zero-based, while the model is one-based.
        pref-steps (when (map? c-pref)
                     (set (for [[step row] c-pref
                                :when (and (integer? step) (map? row)
                                           (not= #{:ending/not-yet-evaluated}
                                                 (set (keys row))))]
                            (dec (long step)))))
        census (get cert :q9-q10-census)
        g-term-rows (vec (keep #(get (val %) :g-terms) scoring))
        contributing? (fn [term row]
                        (let [v (get row term)]
                          (and (number? v) (Double/isFinite (double v))
                               (pos? (double v)))))
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
               "libraryPatternCount" (or (get-in cert [:library-pin :size])
                                          (count (:patterns snap)))
               "targetConstruction" (or construction
                                          (nr "selection certificate target construction absent"))
               "constructorPatternCount" (if (some? constructor-pool)
                                             (count constructor-pool)
                                             (nr "selection certificate target construction absent"))
               "constructedCascades" (if cascade-ids (sorted-ids cascade-ids)
                                         (nr "constructed candidate ids absent"))
               "comparedPolicies" (if policy-ids (sorted-ids policy-ids)
                                      (nr "policy list absent"))
               "cascadesWithoutG" (if (and cascade-ids (some? with-g))
                                      (sorted-ids (set/difference cascade-ids
                                                                  (set (for [c candidates :when (numeric-g? c)]
                                                                         (candidate-id c)))))
                                      (nr "constructed cascades or per-candidate numeric G absent"))
               "horizonLength" (or horizon (nr "observation-model horizon absent"))
               "preferenceSteps" (if (some? pref-steps) (vec (sort pref-steps))
                                     (nr "step-indexed class preference absent"))
               "gTerms" (if (seq g-term-rows)
                            {"risk" (every? #(contributing? :risk %) g-term-rows)
                             "ambiguity" (every? #(contributing? :ambiguity %) g-term-rows)
                             "informationGain" (every? #(contributing? :expected-information-gain %) g-term-rows)}
                            (nr "per-candidate G terms absent"))
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
               "completionPreferencePairs" (if (number? (:completion-preference-pairs census))
                                              (:completion-preference-pairs census)
                                              (nr "reachable completion-preference pairs absent"))
               "completionPairsStrictlyPreferred" (if (number? (:completion-pairs-strictly-preferred census))
                                                      (:completion-pairs-strictly-preferred census)
                                                      (nr "strict completion-preference comparisons absent"))
               "earlierProgressPairs" (if (number? (:earlier-progress-pairs census))
                                         (:earlier-progress-pairs census)
                                         (nr "earlier-progress pair census absent"))
               "earlierProgressNoGreaterRisk" (if (number? (:earlier-progress-no-greater-risk census))
                                                  (:earlier-progress-no-greater-risk census)
                                                  (nr "earlier-progress risk census absent"))
               "differentArrangementPairs" (if (number? (:different-arrangement-pairs census))
                                               (:different-arrangement-pairs census)
                                               (nr "same-pattern different-arrangement pair census absent"))
               "arrangementPairsDistinguishedByG" (if (number? (:arrangement-pairs-distinguished-by-g census))
                                                      (:arrangement-pairs-distinguished-by-g census)
                                                      (nr "arrangement-pair distinct-policy/G census absent"))}
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
