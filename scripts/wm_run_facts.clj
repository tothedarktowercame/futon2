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
            [futon2.aif.mission-registry :as registry]
            [futon2.aif.policy-prefix-evidence :as policy-prefix-evidence]
            [futon2.aif.previous-run :as previous-run]
            [futon2.aif.q4-preference :as q4]
            ))

(def run-fact-fields
  ["openMissions" "openExcursions" "openTickets" "enumeratedTasks"
   "targetsReachingScoring" "targetsWithG" "libraryPatternCount"
   "targetConstruction" "constructorPatternCount" "constructedCascades"
   "comparedPolicies" "cascadesWithoutG" "policyPreferences"
   ;; one run-wide horizon: no longer a Requirements field (Q4 is stated
   ;; per compared policy), kept for the history table and alert text
   "horizonLength" "preferenceSteps"
   "gradedPreferenceSteps" "preferenceSemantics" "gTerms" "policiesWithRiskTerm"
   "policiesWithAmbiguityTerm" "policiesWithInformationTerm"
   "interpretationOrder" "pathAbsenceCount" "previousChoice"
   "previousOutcome" "previousInputDigest" "currentChoice"
   "currentInputDigest" "seatsAvailable" "seatsUsed"
   "completionPreferencePairs" "completionPairsStrictlyPreferred"
])

(defn not-recomputable [s] {"not-recomputable" s})
(defn- sorted-ids [xs] (vec (sort (map str xs))))
(defn- present? [x] (not (and (map? x) (contains? x "not-recomputable"))))

(def ^:private law-names
  {:per-step-capacity-and-pattern "perStepCapacityAndPattern"
   :raw-cumulative "rawCumulative"})

(defn q4-facts
  "Requirements Q4 for a reading-family record, from the persisted
  decision's Q4 carrier only (q4-preference/verify-record): the compared
  policies, one record per compared policy (its own horizon, steps,
  grading, semantics, G normalization and evaluated terms), and the term
  counts. nil for a record of another route, which keeps the historical
  reader."
  [record]
  (when (= :wm/reading-policy-family-v1
           (get-in record [:decision :selection-certificate :selection-route]))
    (let [v (q4/verify-record record)
          single (not-recomputable "per-policy horizons: see policyPreferences")
          singles {"horizonLength" single "preferenceSteps" single
                   "gradedPreferenceSteps" single "preferenceSemantics" single}]
      (if-not (= :verified (:status v))
        (let [why (not-recomputable (str "Q4 carrier " (name (:reason v))))]
          (merge singles
                 (zipmap ["comparedPolicies" "policyPreferences" "gTerms"
                          "policiesWithRiskTerm" "policiesWithAmbiguityTerm"
                          "policiesWithInformationTerm"]
                         (repeat why))))
        (let [c (:carrier v)]
          (merge
           singles
           {"comparedPolicies" (vec (sort (map pr-str (:compared c))))
            "policyPreferences"
            (vec (for [p (:policies c)]
                   {"policy" (pr-str (:policy-key p))
                    "horizon" (:horizon p)
                    "preferenceSteps" (:preference-steps p)
                    "gradedPreferenceSteps" (:graded-steps p)
                    "semantics" (name (:semantics p))
                    "normalization" (law-names (get-in p [:normalization :law]))
                    "terms" {"risk" (get-in p [:terms :risk])
                             "ambiguity" (get-in p [:terms :ambiguity])
                             "informationGain" (get-in p [:terms :information-gain])}}))
            "gTerms" {"risk" (get-in c [:g-terms :risk])
                      "ambiguity" (get-in c [:g-terms :ambiguity])
                      "informationGain" (get-in c [:g-terms :information-gain])}
            "policiesWithRiskTerm" (get-in c [:counts :risk])
            "policiesWithAmbiguityTerm" (get-in c [:counts :ambiguity])
            "policiesWithInformationTerm" (get-in c [:counts :information-gain])}))))))

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
                        :history-unavailable-noncanonical-policy-key
                        :history-index-absent :history-index-incomplete
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

(defn- consumed-prefix-f?
  "The enacted candidate consumed a canonical F carrier: its own computed
  Prefix-F, or the certified zero-history carrier valid under the record's
  own authoritative census PIN (checked exactly as enactment checks it)."
  [candidate pin]
  (let [prefix (:f-prefix candidate)
        input (get-in candidate [:inputs :f])
        f (:f prefix)]
    (if (= :not-applicable (:status prefix))
      (policy-prefix-evidence/consumed-canonical-f? candidate pin)
      (and (= :wm/policy-prefix-evidence-v1 (:schema prefix))
           (= (:id candidate) (:policy prefix))
           (= :computed (:status prefix))
           (number? f) (Double/isFinite (double f))
           (= f (:f candidate))
           (= :computed (:f-status candidate))
           (= {:status :computed :value f :presence :prefix-receipt :reason nil}
              (select-keys input [:status :value :presence :reason]))))))

(defn- q7-candidate
  "The selected candidate as Q7 walks it. A consumed canonical F supersedes
  the scorer's own :computed-f. A :not-applicable carrier that does not
  validate against the record's authoritative census PIN is not an
  applicability certificate, so it counts as refused."
  [candidate pin]
  (cond
    (consumed-prefix-f? candidate pin) (dissoc candidate :computed-f)
    (= :not-applicable (get-in candidate [:f-prefix :status]))
    (assoc-in candidate [:f-prefix :status] :refused)
    :else candidate))

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
                candidate (conj [[:decision :selected-candidate]
                                 (q7-candidate
                                  candidate
                                  (get-in record [:decision :selection-certificate
                                                  :policy-history-pin]))])
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

(defn- absence-records
  "Return the exact absence paths together with the map that caused each one.

  The paths remain the measurement surface for Q7.  This separate projection
  is diagnostic only: several fields in a selected policy can repeat one
  missing Prefix-F carrier, and readers need that cause without losing the
  individual paths."
  [record]
  (let [chosen (get-in record [:decision :chosen])
        candidate (selected-candidate record)
        abstention (get-in record [:decision :abstention])
        roots (cond-> [[[:selection-event] (:selection-event record)]
                       [[:interpretation-ask] (:interpretation-ask record)]
                       [[:terminal-receipt] (:terminal-receipt record)]
                       [[:failure] (:failure record)]]
                (map? chosen) (conj [[:decision :chosen] chosen])
                candidate (conj [[:decision :selected-candidate]
                                 (q7-candidate
                                  candidate
                                  (get-in record [:decision :selection-certificate
                                                  :policy-history-pin]))])
                (and (not (map? chosen)) abstention)
                (conj [[:decision :abstention] abstention]))]
    (vec
     (mapcat
      (fn [[root x]]
        (when (some? x)
          (for [[p value] (paths-with x
                              #(and (map? %)
                                    (or (contains? absence-statuses (:status %))
                                        (contains? absence-statuses (:outcome %))
                                        (= :typed-refusal (:kind %)))))]
            {:path (vec (concat root p)) :value value})))
      roots))))

(defn- absence-group
  "Classify one exact absence path by its actual carrier root."
  [candidate {:keys [path value]}]
  (let [selected? (= [:decision :selected-candidate]
                     (subvec path 0 (min 2 (count path))))
        prefix (when selected? (:f-prefix candidate))]
    (if (and selected? (= :no-flight-records (:reason prefix)))
      {:root-cause :selected-policy-prefix-f/no-flight-records
       :reason :no-flight-records
       :conditioning-status (get-in prefix [:conditioning :conditioning-status])
       :conditioning-reason (get-in prefix [:conditioning :reason])}
      {:root-cause (if selected? :selected-path :record-carrier)
       :status (:status value)
       :reason (:reason value)})))

(defn absence-diagnostics
  "Group diagnostic records while retaining every exact path and value."
  [record]
  (let [candidate (selected-candidate record)]
    (->> (absence-records record)
       (group-by #(absence-group candidate %))
       (mapv (fn [[cause entries]]
               (assoc cause :paths (mapv :path entries)
                            :statuses (mapv #(select-keys (:value %) [:status :outcome :kind :reason])
                                            entries))))
       (sort-by (comp pr-str :root-cause))
       vec)))

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

(defn interpretation-order
  "Q5 from the recorded selection-world instants. Instants are compared as
  instants, not as strings (an ISO string drops trailing zeros, so a later
  instant can sort earlier as text). A selected reading policy that still
  owes interpretation which was never issued certifies nothing: it is not
  recomputable, never a vacuous pass. With nothing owed and nothing asked,
  no interpretation preceded the selection."
  [world]
  (let [selected-at (:selection-ended-at world)
        asked-at (:interpretation-issued-at world)
        ask (:interpretation-ask-status world)
        instant (fn [x] (try (java.time.Instant/parse (str x)) (catch Exception _ nil)))]
    (cond
      (nil? selected-at) (not-recomputable "selection completion instant absent")
      (nil? (instant selected-at)) (not-recomputable "selection completion instant unreadable")
      asked-at
      (if-let [a (instant asked-at)]
        (if (.isBefore ^java.time.Instant (instant selected-at) a)
          "selectionBeforeInterpretation" "interpretationBeforeSelection")
        (not-recomputable "interpretation issue instant unreadable"))
      (and (= :selected-reading (:kind ask))
           (not= :discharged (:discharge ask)))
      (not-recomputable (str "selected policy's interpretation owed and never issued: "
               (name (or (:reason ask) :unknown))))
      :else "selectionBeforeInterpretation")))

(def ^:private q6-outcomes
  {:changed "changed" :already-satisfied "alreadySatisfied" :question "question"
   :refused "refused" :timed-out "timedOut" :invalid "invalid"})

(defn- q6-choice [c]
  {"target" (:target c) "cascade" (pr-str (:policy-key c))})

(defn q6-facts
  "Requirements Q6 from the run's own :q6 carrier only: the predecessor the
  runtime read before selecting and this run's terminal carrier, both
  sealed. Nothing is searched for or inferred from other records; a missing,
  tampered or typed-absent part is not recomputable."
  [record]
  (let [{:keys [predecessor terminal]} (:q6 record)
        nr not-recomputable
        pred-ok? (and (= previous-run/predecessor-schema (:schema predecessor))
                      (previous-run/intact? predecessor))
        term-ok? (and (= previous-run/terminal-schema (:schema terminal))
                      (previous-run/intact? terminal)
                      (= (:run/id record) (:run/id terminal)))
        prev (when (and pred-ok? (= :present (:status predecessor))) (:previous predecessor))
        why (cond (nil? predecessor) "Q6 predecessor carrier absent"
                  (not pred-ok?) "Q6 predecessor carrier tampered"
                  (not prev) (str "previous run " (name (or (:reason predecessor) :absent))))
        {pc :choice po :outcome pd :input-digest} prev
        {cc :choice cd :input-digest} (when term-ok? terminal)]
    {"previousChoice" (cond why (nr why)
                            (= :present (:status pc)) (q6-choice pc)
                            :else (nr (str "previous choice " (name (or (:reason pc) :absent)))))
     "previousOutcome" (cond why (nr why)
                             (q6-outcomes (:outcome po)) (q6-outcomes (:outcome po))
                             :else (nr (str "previous outcome is not a RunOutcome: "
                                            (name (or (:outcome po) (:reason po) :absent)))))
     "previousInputDigest" (cond why (nr why)
                                 (string? pd) pd
                                 :else (nr "previous selection-input digest absent"))
     "currentChoice" (cond (not term-ok?) (nr "Q6 terminal carrier absent or tampered")
                           (= :present (:status cc)) (q6-choice cc)
                           :else (nr (str "current choice " (name (or (:reason cc) :absent)))))
     "currentInputDigest" (cond (not term-ok?) (nr "Q6 terminal carrier absent or tampered")
                                (string? cd) cd
                                :else (nr "selection-input digest absent: decision computed none"))}))

(defn facts-for-record
  "Return {:facts <RunFacts-shaped JSON data> :sources ... :diagnostics ...}."
  [record record-path snap]
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
        scoring-entries (when (map? scoring) (vec (sort-by key scoring)))
        scored-bindings
        (when scoring-entries
          (mapv (fn [[idx row]]
                  (let [candidate (when (and (integer? idx) (<= 0 idx)
                                             (< idx (count candidates)))
                                    (nth candidates idx))
                        identity (some-> candidate :id action-identity/digest)
                        gp (when (and (vector? gpolicies) (integer? idx)
                                      (<= 0 idx) (< idx (count gpolicies)))
                             (nth gpolicies idx))
                        fallback (when (and identity
                                            (= identity (some-> gp :id action-identity/digest)))
                                   (get-in gp [:terms :A :value]))]
                    {:index idx :identity identity :row row
                     :model (or (:observation-model row) fallback)}))
                scoring-entries))
        scored-identities (mapv :identity scored-bindings)
        q4-identity-coherent?
        (and (map? posterior) (vector? candidates) (seq scored-bindings)
             (every? some? scored-identities)
             (= (count scored-identities) (count (set scored-identities)))
             (= (set scored-identities) policy-ids)
             (= (count scored-identities) (count policy-ids)))
        scoring-rows (mapv :row scored-bindings)
        models (mapv :model scored-bindings)
        model-signature (fn [m]
                          (when (and (map? m)
                                     (contains? #{:terminal-only :constant :progressive}
                                                (:preference-semantics m)))
                            {:horizon (:horizon m)
                             :preference (or (:class-preference m)
                                             (:progress-preference m))
                             :semantics (:preference-semantics m)}))
        signatures (mapv model-signature models)
        q4-model-coherent? (and q4-identity-coherent?
                                (every? some? signatures)
                                (apply = signatures))
        model (when q4-model-coherent? (first models))
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
        g-term-rows (mapv :g-terms scoring-rows)
        recorded-term? (fn [term row]
                         (let [v (get row term ::absent)]
                           (and (number? v) (Double/isFinite (double v)))))
        ;; Q4 compares these counts with comparedPolicies.card.  If the
        ;; scoring table and policy population are different grains, the
        ;; exporter has no honest Q4 fact: report NR rather than combining
        ;; (for example) thousands of target-policy occurrences with sixteen
        ;; reused local labels.
        q4-counts-coherent? (and q4-identity-coherent?
                                 (every? map? g-term-rows))
        apaths (absence-paths record)
        absence-groups (absence-diagnostics record)
        q6 (q6-facts record)
        used (set (keep identity [(get-in record [:participants :author])
                                  (get-in record [:participants :reviewer])
                                  (get-in record [:interpretation-ask :seat])]))
        nr (fn [s] (not-recomputable s))
        q4 (q4-facts record)
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
               "policyPreferences" (nr "no per-policy G normalization declaration on this route")
               "horizonLength" (or horizon (nr "common declared per-policy observation model unavailable"))
               "preferenceSteps" (if (some? pref-steps) (vec (sort pref-steps))
                                     (nr "common declared per-policy preference schedule unavailable"))
               "gradedPreferenceSteps"
               (if (some? graded-pref-steps) (vec (sort graded-pref-steps))
                   (nr "common declared per-policy graded schedule unavailable"))
               "preferenceSemantics"
               (if q4-model-coherent?
                 (name (:preference-semantics model))
                 (nr "per-policy horizon/preference/semantics missing or non-uniform"))
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
               "interpretationOrder" (interpretation-order world)
               "pathAbsenceCount" (count apaths)
               "previousChoice" (q6 "previousChoice")
               "previousOutcome" (q6 "previousOutcome")
               "previousInputDigest" (q6 "previousInputDigest")
               "currentChoice" (q6 "currentChoice")
               "currentInputDigest" (q6 "currentInputDigest")
               "seatsAvailable" (if (map? (:seat-roster world))
                                  (sorted-ids (mapcat :ids (vals (:seat-roster world))))
                                  (if (:seats snap) (sorted-ids (:seats snap))
                                      (nr "Agency roster snapshot unavailable")))
               "seatsUsed" (if (seq used) (sorted-ids used)
                               (nr "participant seat ids absent"))
               "completionPreferencePairs" (nr "reachable completion-preference pairs absent")
               "completionPairsStrictlyPreferred" (nr "strict completion-preference comparisons absent")
               }
        ;; a reading-family record's Q4 comes from its carrier alone
        facts (merge facts q4)
        sources (into {}
                      (for [field run-fact-fields]
                        [field (cond
                                 (#{"openMissions" "openExcursions" "openTickets"
                                    "libraryPatternCount" "seatsAvailable"} field) (:pins snap)
                                 (#{"previousChoice" "previousOutcome" "previousInputDigest"
                                    "currentChoice" "currentInputDigest"} field)
                                 (str record-path " [:q6]")
                                 :else record-path)]))]
    {:facts facts :sources sources
     :diagnostics {:absencePaths (mapv pr-str apaths)
                   :absenceGroups absence-groups
                   :notRecomputable (vec (for [[k v] facts
                                               :when (and (map? v) (contains? v "not-recomputable"))]
                                           k))}}))

(defn history-rows [paths snap]
  (mapv (fn [path] (assoc (facts-for-record (read-edn path) path snap) :path path))
        paths))

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
                (facts-for-record (read-edn path) path snap)
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
