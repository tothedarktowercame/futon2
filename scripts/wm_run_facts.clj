(ns wm-run-facts
  "Read-only exporter from persisted WM records to Requirements.RunFacts.

  Values are never guessed. A missing carrier becomes {not-recomputable ...}.
  Sets in Lean are emitted as sorted JSON arrays. The sibling `sources` map
  records the record path or snapshot command for every RunFacts field."
  (:require [futon2.aif.inner-abstention :as stop]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.action-identity :as action-identity]
            [futon2.aif.cascade-selection :as cascade-selection]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.mission-registry :as registry]
            [futon2.aif.policy-prefix-evidence :as policy-prefix-evidence]
            [futon2.aif.previous-run :as previous-run]
            [futon2.aif.selection-record :as selection-record]
            [futon2.aif.q4-preference :as q4]
            )
  (:import [java.nio.file Files]))

(def run-fact-fields
  ["kind" "openMissions" "openExcursions" "openTickets" "enumeratedTasks"
   "targetsReachingScoring" "targetsWithG" "libraryPatternCount"
   "targetConstruction" "constructorPatternCount" "constructedCascades"
   "comparedPolicies" "cascadesWithoutG" "policyPreferences"
   "policyG" "policyGScale" "abstentionG" "policyPrediction" "abstentionPrediction"
   ;; one run-wide horizon: no longer a Requirements field (Q4 is stated
   ;; per compared policy), kept for the history table and alert text
   "horizonLength" "preferenceSteps"
   "gradedPreferenceSteps" "preferenceSemantics" "gTerms" "policiesWithRiskTerm"
   "policiesWithAmbiguityTerm" "policiesWithInformationTerm"
   "interpretationOrder" "pathAbsenceCount" "previousChoice"
   "previousOutcome" "previousInputDigest" "currentChoice" "refusalHistory"
   "refusalHistorySource" "refusalHistoryUnsealed" "refusalHistorySealedBeforeR18"
   "currentInputDigest" "seatsAvailable" "seatsUsed"
   "completionPreferencePairs" "completionPairsStrictlyPreferred"
   "factorizedPreference" "calibrationAuthorizations"
   "factorizationAuthorizations"
])

(defn not-recomputable [s] {"not-recomputable" s})

;; Q11 compares exact rationals, so a recorded double is rounded to one declared
;; scale.  Scores within a unit of it can become an exact tie, which Q11 refuses:
;; arithmetic noise must not decide a selection.
(def ^:private g-scale cascade-selection/g-scale)

(def ^:private stopped-kernel-absent
  "no stopped-kernel G is computed on the inner path (L4b row 4; L5)")

(defn policy-identity
  "The one id of a policy: every side that names one uses this.  The Q6 carrier
  holds the policy key and no cascade digest, so a digest on one side and a key
  on the other would make `previous ≠ current` always true."
  [policy-key]
  (action-identity/digest policy-key))

(defn exact-g
  "G as an exact rational at `g-scale`, with the source double for audit.
  Nil when `x` is not a finite number."
  [x]
  (when (and (number? x) (Double/isFinite (double x)))
    {"numerator" (cascade-selection/g-units x)
     "denominator" g-scale "scale" g-scale "double" (double x)}))

(defn- calibration-json [cal]
  (when (map? cal)
    {"identity" (:identity cal) "version" (:version cal)
     "authority" (:authority cal)
     "registration-token" (:registration-token cal)
     "focused-cost" (:focused-cost cal)
     "related-cost" (:related-cost cal)
     "unrelated-cost" (:unrelated-cost cal)}))

(defn factorized-preference-record
  "The retained :wm/factorized-preference-v1 carrier of RECORD, or nil. The
  sole producer (futon2.aif.factorized-preference-sut) is deliberately absent
  from the serving composition, so absence is a fact about the run, not a
  missing export. Values are never guessed: no carrier means nil."
  [record]
  (some #(when (and (map? %) (= :wm/factorized-preference-v1 (:schema %))) %)
        [(get-in record [:factorized-preference])
         (get-in record [:decision :factorized-preference])
         (get-in record [:decision :selection-certificate :factorized-preference])
         (get-in record [:close :factorized-preference])]))

(defn factorized-preference
  "Q9's deciding carrier in the JSON shape wm-check-run-facts reads: nil (the
  verified negative) when the record carries none."
  [record]
  (when-let [carrier (factorized-preference-record record)]
    (let [cert (:certificate carrier)]
      (when (map? cert)
        {"calibration" (calibration-json (:calibration cert))
         "selector-authority" (:selector-authority cert)
         "factorization-identity" (:factorization-identity cert)
         "factorization-version" (:factorization-version cert)
         "factorization-authority" (:factorization-authority cert)
         "calibration-authorization" (:calibration-authorization cert)
         "factorization-authorization" (:factorization-authorization cert)}))))
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
           {"comparedPolicies" (sorted-ids (map policy-identity (:compared c)))
            "policyPreferences"
            (vec (for [p (:policies c)]
                   {"policy" (policy-identity (:policy-key p))
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

(defn- key-patterns
  "The pattern ids a policy key arranges, from its nodes."
  [policy-key]
  (map second (:nodes (nth policy-key 3))))

(defn- census-construction
  "One `TargetConstruction` from a Q2 construction census entry.  `action` has no
  source and no Q reads it, so it is the recorded absence."
  [{:keys [library graph]} {:keys [task reading kernel slice policies]}]
  (let [patterns (:patterns library)]
    {"targets" [(str task)]
     "action" 0
     "reading" (action-identity/digest reading)
     "library" (:sha256 library)
     "graph" (get-in graph [:pin :sha256])
     "kernel" (action-identity/digest kernel)
     "slice" (sorted-ids (map :pattern slice))
     "pool" (sorted-ids (distinct (mapcat key-patterns policies)))
     "sliceFromWholeLibrary" (every? #(and (:source-sha256 %)
                                           (= (:source-sha256 %) (get patterns (:pattern %))))
                                     slice)
     "policyCount" (count policies)
     "policies" (sorted-ids (map policy-identity policies))}))

(defn- target-construction-facts [certificate]
  (if-let [entries (seq (get-in certificate [:q2-construction :entries]))]
    (mapv #(census-construction (:q2-construction certificate) %) entries)
    (when (vector? (:target-construction certificate))
    (mapv (fn [{:keys [target slice pool slice-from-whole-library policy-count]}]
            {"targets" [(str target)]
             "slice" (sorted-ids slice)
             "pool" (sorted-ids pool)
             "sliceFromWholeLibrary" (boolean slice-from-whole-library)
             "policyCount" policy-count})
          (:target-construction certificate)))))

(defn- construction-absent
  "Which path has no construction."
  [certificate]
  (not-recomputable
   (if (contains? certificate :q2-construction)
     "[:decision :selection-certificate :q2-construction :entries] empty"
     "[:decision :selection-certificate :q2-construction] absent")))

(defn- g-carriers-absent [reason]
  {"policyG" (not-recomputable reason) "policyPrediction" (not-recomputable reason)})

(defn- candidate-g-row [policy-key candidate]
  (when-let [g (exact-g (:g candidate))]
    {"policy" (policy-identity policy-key) "g" g}))

(defn- candidate-prediction-row
  "The digest of what the law scored: progress and class predictions, step by
  step.  `action-identity/digest` is canonical over map order and exact over
  ratios.  The fit-ambiguity input stays out, so policies predicting the same
  outcomes share a digest."
  [policy-key candidate scored]
  (let [progress (:steps scored)
        emission (get-in candidate [:class-emission :certificate :steps])]
    (when (and (seq progress) (seq emission))
      {"policy" (policy-identity policy-key)
       "digest" (cascade-selection/prediction-digest progress emission)})))

(defn- candidates-with-g-identities
  "Policy identities (the hash of the policy key, as everywhere else) of the
  candidates carrying a numeric G, joined through `:q4 :policies`.  nil when any
  candidate cannot be joined, so Q3's carrier reads not-recomputable rather than
  comparing two id spaces."
  [certificate]
  (let [candidates (:candidates certificate)
        key-of (into {} (map (juxt :policy-id :policy-key)) (get-in certificate [:q4 :policies]))
        id-of #(get-in % [:id :id])]
    (when (and (vector? candidates) (seq key-of)
               (every? (comp key-of id-of) candidates))
      (set (for [c candidates :when (numeric-g? c)]
             (policy-identity (key-of (id-of c))))))))

(defn g-carriers
  "`policyG` and `policyPrediction` from the candidates, each joined to its policy
  key through `:q4 :policies`.  One candidate that cannot be joined or read makes
  the whole carrier not recomputable."
  [certificate]
  (let [candidates (:candidates certificate)
        rows (get-in certificate [:q4 :policies])
        key-of (into {} (map (juxt :policy-id :policy-key)) rows)
        id-of #(get-in % [:id :id])
        scored-of (into {} (map (fn [[_ row]] [(id-of row) row])) (:scoring certificate))
        unjoined (first (remove (comp key-of id-of) candidates))
        by-id (fn [row-of]
                (mapv (fn [c] [(id-of c) (row-of (key-of (id-of c)) c)]) candidates))
        g-rows (when-not unjoined (by-id candidate-g-row))
        prediction-rows (when-not unjoined
                          (by-id #(candidate-prediction-row %1 %2 (scored-of (id-of %2)))))
        missing (fn [pairs] (some (fn [[candidate row]] (when-not row (or candidate :unidentified)))
                                  pairs))]
    (cond
      (not (and (vector? candidates) (seq candidates)))
      (g-carriers-absent "[:decision :selection-certificate :candidates] absent or empty")
      (empty? rows)
      (g-carriers-absent "[:decision :selection-certificate :q4 :policies] absent")
      unjoined
      (g-carriers-absent (str "candidate " (id-of unjoined) " has no :q4 :policies row"))
      :else
      {"policyG" (if-let [id (missing g-rows)]
                   (not-recomputable (str "candidate " id " has no finite :g"))
                   (vec (sort-by #(get % "policy") (map second g-rows))))
       "policyPrediction" (if-let [id (missing prediction-rows)]
                            (not-recomputable
                             (str "candidate " id " has no recorded progress or class predictions"))
                            (vec (sort-by #(get % "policy") (map second prediction-rows))))})))

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
  {"target" (:target c) "cascade" (policy-identity (:policy-key c))})

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

(defn inner-abstention-facts [record]
  (let [sc (get-in record [:decision :selection-certificate])
        a (:abstention sc)
        nr #(zipmap ["abstentionG" "abstentionPrediction"] (repeat (not-recomputable %)))]
    (if-not a
      (nr stopped-kernel-absent)
      (try
        (stop/validate! a (:candidates sc))
        (let [entry (get-in a [:per-policy (:chosen-reference a)])]
          {"abstentionG" (exact-g (:G entry))
           "abstentionPrediction" (:prediction-digest entry)
           "abstentionAudit" (select-keys a [:chosen-reference :rule :cross-lens])})
        (catch Exception e (nr (str "invalid inner abstention: " (:kind (ex-data e)))))))))

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
        ;; Resolve immutable terminal decision bytes only when the tick lacks
        ;; an open-id carrier. Never reconstruct a roster from a newer registry.
        selection-roster
        (delay
          (if-let [ref (:selection-record record)]
            (let [terminal (last (:entries (selection-record/read-chain ref)))]
              {:roster (get-in terminal [:record :outer/selection :world-census :open-tasks])
               :path (get-in terminal [:reference :path])})
            {:roster (get-in record [:outer/selection :world-census :open-tasks])
             :path record-path}))
        roster-keys {"openMissions" :missions "openExcursions" :excursions "openTickets" :tickets}
        open-ids (fn [kind snapshot-key]
                   (if-some [ids (world-ids kind)]
                     (vec ids)
                     (if-some [ids (get-in @selection-roster [:roster kind])]
                       (sorted-ids ids)
                       (sorted-ids (get snap snapshot-key)))))
        open-source (fn [kind]
                      (cond
                        (some? (world-ids kind))
                        (str record-path " " (pr-str [:world-at-selection :open-tasks kind :ids]))
                        (some? (get-in @selection-roster [:roster kind]))
                        (str (:path @selection-roster) " "
                             (pr-str [:outer/selection :world-census :open-tasks kind]))
                        :else (:pins snap)))
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
        constructed-policies (seq (mapcat #(get % "policies") construction))
        with-g-identities (candidates-with-g-identities cert)
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
        ;; The projector labels the kind; the record never does.  A record
        ;; choosing its own label could dodge the selection floor of two.
        facts {"kind" "work"
               "policyGScale" g-scale
               "abstentionG" (get (inner-abstention-facts record) "abstentionG")
               "abstentionPrediction" (get (inner-abstention-facts record) "abstentionPrediction")
               "openMissions" (open-ids :missions :open-missions)
               "openExcursions" (open-ids :excursions :open-excursions)
               "openTickets" (open-ids :tickets :open-tickets)
               "enumeratedTasks" (if enum-ids (sorted-ids enum-ids)
                                     (nr "record has enumerated counts/missing ids but no :enumerated-ids sets"))
               "targetsReachingScoring" (if (some? reaching) (sorted-ids reaching)
                                             (nr "selection certificate candidates absent"))
               "targetsWithG" (if (some? with-g) (sorted-ids with-g)
                                  (nr "selection certificate candidates absent"))
               "libraryPatternCount" (or (get-in cert [:q2-construction :library :count])
                                         (nr "[:decision :selection-certificate :q2-construction :library :count] absent"))
               "targetConstruction" (or construction (construction-absent cert))
               "constructorPatternCount" (if (some? constructor-pool)
                                             (count constructor-pool)
                                             (construction-absent cert))
               "constructedCascades" (cond
                                       constructed-policies (vec (sort constructed-policies))
                                       cascade-ids (sorted-ids cascade-ids)
                                       :else (nr "constructed candidate ids absent"))
               "comparedPolicies" (if policy-ids (sorted-ids policy-ids)
                                      (nr "selection posterior absent"))
               ;; Same id space as constructedCascades: a non-empty set must name
               ;; ids that appear there.  The digest-of-:id path stays only for
               ;; records with no census.
               "cascadesWithoutG" (cond
                                    (and constructed-policies with-g-identities)
                                    (vec (sort (set/difference (set constructed-policies)
                                                               with-g-identities)))
                                    (and (nil? constructed-policies) cascade-ids (some? with-g))
                                    (sorted-ids
                                     (set/difference
                                      cascade-ids
                                      (set (for [c candidates :when (numeric-g? c)]
                                             (action-identity/digest (:id c))))))
                                    :else
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
               "currentChoice" (if (= :abstain-on-task (get-in record [:decision :abstention :case]))
                                 {"abstain-on-task" (get-in record [:decision :abstention :target])}
                                 (q6 "currentChoice"))
               ;; L5-2L-2: the refusal history as far as the record knows it:
               ;; every refusal retained for this task. A record from before
               ;; retention carries only its predecessor's, and says so.
               "refusalHistory" (let [retained (get-in cert [:q6 :refusal-history])
                                      pc (q6 "previousChoice") po (q6 "previousOutcome") pd (q6 "previousInputDigest")]
                                  (cond (vector? retained)
                                        (mapv (fn [{:keys [cascade input-digest]}]
                                                {"cascade" cascade "input-digest" input-digest})
                                              retained)
                                        (some #(and (map? %) (contains? % "not-recomputable")) [pc po pd])
                                        (nr "refusal history needs the predecessor's choice, outcome and digest")
                                        (= "refused" po) [{"cascade" (get pc "cascade") "input-digest" pd}]
                                        :else []))
               "refusalHistorySource" (if (vector? (get-in cert [:q6 :refusal-history]))
                                        "retained" "predecessor-only")
               ;; L5-2L-3: the producer's count of unverifiable terminals; absent on
               ;; records written before the carrier, which then read Q6 unverifiable.
               "refusalHistoryUnsealed" (let [u (get-in cert [:q6 :refusal-history-unsealed] ::absent)]
                                          (cond (nat-int? u) u
                                                (= ::absent u) (nr "[:decision :selection-certificate :q6 :refusal-history-unsealed] absent (pre-L5-2L-3 record)")
                                                :else (nr (str "refusal-history-unsealed is not a count: " (pr-str u)))))
               ;; L5-6: terminals verified under the pre-R18 printer, for the report card
               "refusalHistorySealedBeforeR18"
               (let [k (get-in cert [:q6 :refusal-history-sealed-before-r18] ::absent)]
                 (if (nat-int? k) k
                     (nr "[:decision :selection-certificate :q6 :refusal-history-sealed-before-r18] absent")))
               "currentInputDigest" (q6 "currentInputDigest")
               "seatsAvailable" (if (map? (:seat-roster world))
                                  (sorted-ids (mapcat :ids (vals (:seat-roster world))))
                                  (if (:seats snap) (sorted-ids (:seats snap))
                                      (nr "Agency roster snapshot unavailable")))
               "seatsUsed" (if (seq used) (sorted-ids used)
                               (nr "participant seat ids absent"))
               "completionPreferencePairs"
               (nr "no runtime completion-preference census producer: legacy habit store retired, Q9 SUT absent from serving")
               "completionPairsStrictlyPreferred"
               (nr "no runtime strict-preference census producer: legacy habit store retired, Q9 SUT absent from serving")
               ;; Q9's deciding carriers (Requirements.lean Q9 reads only the
               ;; factorized-preference certificate and its authorization
               ;; registries; the pair censuses above are unread carriers).
               ;; A record carrying no :wm/factorized-preference-v1 carrier
               ;; supplies a VERIFIED NEGATIVE (nil), which the generated
               ;; checker renders as `factorizedPreference := none` and Lean
               ;; evaluates Q9 := false -- a decided verdict, never a guess.
               ;; nil is only correct because the sole producer
               ;; (futon2.aif.factorized-preference-sut) is deliberately
               ;; absent from the serving composition, so a carrier cannot
               ;; exist outside the record.
               "factorizedPreference" (factorized-preference record)
               "calibrationAuthorizations"
               (vec (for [e (get-in (factorized-preference-record record)
                                    [:registries :calibration] [])]
                      {"receipt" (:receipt e)
                       "calibration" (calibration-json (:calibration e))
                       "authorized-by" (:authorized-by e)}))
               "factorizationAuthorizations"
               (vec (for [e (get-in (factorized-preference-record record)
                                    [:registries :factorization] [])]
                      {"receipt" (:receipt e) "identity" (:identity e)
                       "version" (:version e) "authority" (:authority e)}))
               }
        ;; a reading-family record's Q4 comes from its carrier alone
        facts (merge facts q4 (g-carriers cert))
        sources (into {}
                      (for [field run-fact-fields]
                        [field (cond
                                 (contains? roster-keys field) (open-source (get roster-keys field))
                                 (#{"libraryPatternCount" "seatsAvailable"} field) (:pins snap)
                                 (= "kind" field) "scripts/wm_run_facts.clj (projector label)"
                                 (#{"previousChoice" "previousOutcome" "previousInputDigest"
                                    "currentChoice" "currentInputDigest"} field)
                                 (str record-path " [:q6]")
                                 :else record-path)]))]
    {:facts facts :sources sources
     :diagnostics {:abstentionAudit (get (inner-abstention-facts record) "abstentionAudit")
                   :previousSkipped (get-in record [:q6 :predecessor :previous-skipped])
                   :absencePaths (mapv pr-str apaths)
                   :absenceGroups absence-groups
                   :notRecomputable (vec (for [[k v] facts
                                               :when (and (map? v) (contains? v "not-recomputable"))]
                                           k))}}))

;; -- the selection run -----------------------------------------------------------

(def ^:private default-makeup-path "data/wm-learning/outer-makeup.edn")
;; The runtime's phase log; the alert passes no path, so the canonical default
;; is read like the makeup's (L5-4h review).
(def ^:private default-phase-log-path "data/wm-full-loop-phases.edn.log")

(def ^:private no-section "no :outer/selection section")
(def ^:private no-library-pin
  "task-selection library pin, slice and cascade digests absent from the record (L4b row 11; L5)")
(def ^:private one-prediction "only the chosen cascade has a prediction (L4b row 9; L5)")
(def ^:private no-stopped-score
  "no separately scored stopped kernel on the selection path (L4b row 10; L5)")
(def ^:private no-previous-decision
  "no selection-level previous decision in the record (L4b row 13; L5)")
(def ^:private no-terms "no per-cascade terms on the selection path (L4b row 9)")
(def ^:private no-carrier "no carrier on the selection path")
(def ^:private kept-when-not-exercised
  "What a record says whether or not a selection ran; every other fact is a
  selection carrier."
  #{"kind" "policyGScale" "openMissions" "openExcursions" "openTickets"})

(defn- read-phase-events [path]
  (when (and path (.isFile (io/file path)))
    (with-open [reader (io/reader path)]
      (doall
       (keep (fn [line]
               (when-not (str/blank? line)
                 (try (edn/read-string {:default tagged-literal} line)
                      (catch Exception _ nil))))
             (line-seq reader))))))

(defn selection-interpretation-order
  "Project Q5 from the retained runtime phase log for RECORD's execution.
  The decision boundary is :selection :end; interpretation begins at
  :occurrence-square-authoring :start. Missing carriers remain typed absent."
  [record phase-log-path]
  (let [execution-id (get-in record [:runner-execution/identity :id])
        missing (fn [carrier]
                  (not-recomputable
                   (str "selection interpretation order needs " carrier)))
        events (read-phase-events phase-log-path)]
    (cond
      (nil? phase-log-path) (missing ":phase-log path")
      (nil? events) (missing (str "retained phase log at " phase-log-path))
      (nil? execution-id) (missing "[:runner-execution/identity :id]")
      :else
      (let [matching (filter #(= execution-id (get-in % [:execution-identity :id])) events)
            indexed (map-indexed vector matching)
            selection-end (some (fn [[i event]]
                                  (when (and (= :selection (:phase event))
                                             (= :end (:transition event))) i))
                                indexed)
            authoring-start (some (fn [[i event]]
                                    (when (and (= :occurrence-square-authoring (:phase event))
                                               (= :start (:transition event))) i))
                                  indexed)]
        (cond
          (nil? selection-end) (missing (str ":selection :end in " phase-log-path))
          (nil? authoring-start) (missing (str ":occurrence-square-authoring :start in " phase-log-path))
          (< selection-end authoring-start) "selectionBeforeInterpretation"
          :else "interpretationBeforeSelection")))))

;; A reach measured for this run would be retained here.  No record has one yet.
(def ^:private reach-receipt-key :reach-receipt)

(def ^:private known-relations #{:focus :associated :useful-elsewhere})

(defn pinned-makeup
  "The makeup file at PATH when its bytes hash to PIN, else nil.  The menu's
  names live only there, so a file at any other sha says nothing about this run."
  [path pin]
  (let [file (io/file path)]
    (when (and pin (.isFile file))
      (let [bytes (Files/readAllBytes (.toPath file))]
        (when (= pin (load-identity/sha256 bytes))
          (edn/read-string {:default tagged-literal} (String. bytes "UTF-8")))))))

(defn- menu-facts
  "Compared policies, constructed cascades, G and cascades without G, by menu
  position.  The record's scores have no names of their own."
  [selection makeup]
  (if-not makeup
    (let [why (not-recomputable (str "outer-makeup.edn at pinned sha "
                                     (:makeup/sha256 selection) " not available"))]
      (zipmap ["comparedPolicies" "constructedCascades" "policyG" "cascadesWithoutG"]
              (repeat why)))
    (let [menu (mapv (comp policy-identity :id) (:cascades makeup))
          scores (vec (:cascade-scores selection))
          rows (mapv (fn [cascade score]
                       (when-let [g (exact-g score)] {"policy" cascade "g" g}))
                     menu scores)]
      {"comparedPolicies" (vec (sort menu))
       "constructedCascades" (vec (sort menu))
       "cascadesWithoutG" (vec (sort (subvec menu (min (count scores) (count menu)))))
       "policyG" (cond
                   (not= (count scores) (count menu))
                   (not-recomputable (str (count scores) " scores for "
                                          (count menu) " menu cascades"))
                   (some nil? rows) (not-recomputable "a menu cascade has no finite score")
                   :else (vec (sort-by #(get % "policy") rows)))})))

(defn- selection-history-facts [selection]
  (let [p (:previous selection)
        present (= :present (:status p))
        nr #(not-recomputable (str "previous selection: " (or (:reason p) %)))
        decision (cond
                   (and present (= :abstained (:outcome p)))
                   (if (:pick p) {"abstain-on-task" (:pick p)} "abstain-empty-field")
                   (and present (:chosen-class p) (:pick p))
                   {"target" (:pick p) "cascade" (:chosen-class p)}
                   :else (nr :choice-unretained))]
    {"previousChoice" decision
     "previousOutcome" (if (and present (#{:changed :refused :abstained} (:outcome p)))
                         (name (:outcome p)) (nr :outcome-unretained))
     "previousInputDigest" (if (and present (:choice-input-digest p)) (:choice-input-digest p)
                              (nr :digest-unretained))
     "currentInputDigest" (or (get-in selection [:choice-input-digest :sha256])
                              (not-recomputable "choice-input-digest unretained"))}))

(defn- selection-refusal-history-facts [selection]
  (when-let [q6 (:q6 selection)]
    {"refusalHistory"
     (if (vector? (:refusal-history q6))
       (mapv (fn [{:keys [cascade input-digest]}]
               {"cascade" cascade "input-digest" input-digest})
             (:refusal-history q6))
       (not-recomputable "selection refusal history is not a vector"))
     "refusalHistorySource" "retained"
     "refusalHistoryUnsealed"
     (if (nat-int? (:refusal-history-unsealed q6))
       (:refusal-history-unsealed q6)
       (not-recomputable "selection refusal-history-unsealed is not a count"))
     "refusalHistorySealedBeforeR18"
     (if (nat-int? (:refusal-history-sealed-before-r18 q6))
       (:refusal-history-sealed-before-r18 q6)
       (not-recomputable "selection refusal-history-sealed-before-r18 is not a count"))}))

(defn- selection-preferences
  "One terminal-only control step per retained prediction class. Novelty is
  the information term; missing numeric terms are not inferred from G."
  [classes]
  (let [missing (first (filter #(not (every? (fn [k] (number? (get-in % [:terms k])))
                                            [:risk :ambiguity :novelty])) classes))
        fields ["policyPreferences" "gTerms" "policiesWithRiskTerm"
                "policiesWithAmbiguityTerm" "policiesWithInformationTerm"]]
    (if (or (empty? classes) missing)
      (zipmap fields (repeat (not-recomputable (str "selection class " (:id missing) " lacks recorded terms"))))
      {"policyPreferences" (mapv (fn [c] {"policy" (:id c) "horizon" 1
                                         "preferenceSteps" [0] "gradedPreferenceSteps" []
                                         "semantics" "terminal-only" "normalization" "rawCumulative"
                                         "terms" {"risk" true "ambiguity" true "informationGain" true}}) classes)
       "gTerms" {"risk" true "ambiguity" true "informationGain" true}
       "policiesWithRiskTerm" (count classes)
       "policiesWithAmbiguityTerm" (count classes)
       "policiesWithInformationTerm" (count classes)})))

(defn- constructed-menu-facts
  "Project retained construction, never the present library. Recompute slice
  membership from its manifest and unit pins, not a producer truth flag."
  [selection enumerated]
  (let [c (:construction selection)
        library (:library c)
        policies (:cascades c)
        classes (:classes selection)
        scored (or classes (:cascades selection))
        policy-id (if classes :id :digest)
        patterns (:patterns library)
        units (mapcat :units policies)
        pattern-name #(second (re-matches #"futon3/library/task-selection/(.+)\.flexiarg" (:pattern %)))
        unit-patterns (set (map pattern-name units))
        slice? (and (seq units)
                    (= (:count library) (count patterns))
                    (= (:manifest-sha library)
                       (load-identity/sha256 (.getBytes (pr-str (into (sorted-map) patterns)) "UTF-8")))
                    (= (:sha256 library)
                       (load-identity/sha256 (.getBytes (pr-str [(:manifest-sha library)
                                                                (:mapping-sha library)
                                                                (:readme-sha library)]) "UTF-8")))
                    (= (set (:slice c)) unit-patterns (set (:pool c)))
                    (every? #(and (pattern-name %) (:sha256 %)
                                  (= (:sha256 %) (get patterns (pattern-name %)))) units))
        pin #(if (and (string? %) (re-matches #"[0-9a-f]{64}" %)) % "")
        ids (sorted-ids (if classes (distinct (map :class policies)) (map :digest policies)))
        score-ids (sorted-ids (map policy-id scored))
        g-rows (mapv #(when-let [g (exact-g (:G %))] {"policy" (policy-id %) "g" g}) scored)
        prediction-rows (mapv #(when (seq (:prediction %))
                                {"policy" (policy-id %) "digest" (action-identity/digest (if classes (get-in % [:prediction :outcomes]) (:prediction %)))}) scored)
        rows-or-nr (fn [rows why] (if (every? some? rows)
                                   (vec (sort-by #(get % "policy") rows))
                                   (not-recomputable why)))]
    {"targetConstruction" [{"targets" enumerated "action" 0
                            "reading" (pin (:reading c)) "library" (pin (:sha256 library))
                            "graph" (pin (:graph c)) "kernel" (pin (:kernel c))
                            "slice" (sorted-ids (:slice c)) "pool" (sorted-ids (:pool c))
                            "sliceFromWholeLibrary" (boolean slice?)
                            "policyCount" (:policy-count c) "policies" ids}]
     "constructorPatternCount" (count (:pool c))
     "libraryPatternCount" (:count library)
     "constructedCascades" ids "comparedPolicies" score-ids
     "classMembers" (mapv #(hash-map "class" (:id %) "members" (mapv str (:members %))) classes)
     "cascadesWithoutG" (sorted-ids (map policy-id (remove #(exact-g (:G %)) scored)))
     "policyG" (rows-or-nr g-rows "constructed cascade missing finite G")
     "policyPrediction" (rows-or-nr prediction-rows "constructed cascade missing prediction")
     "abstentionG" (or (exact-g (get-in selection [:abstention :G]))
                        (not-recomputable "abstention G unavailable"))
     "abstentionPrediction" (if-let [prediction (seq (get-in selection [:abstention :prediction]))]
                               (action-identity/digest (if classes (get-in selection [:abstention :prediction :outcomes]) (into {} prediction)))
                               (not-recomputable "abstention prediction unavailable"))}))

(defn- selection-decision
  "The target and cascade chosen, or the empty-field abstention when the chosen
  cascade picked nothing.  Lean reads that on a non-empty field as a violation."
  [selection]
  (let [target (get-in selection [:selection :selected :target])
        picks (:cascade-picks selection)
        index (:cascade-index selection)
        pick (when (and picks index (< -1 index (count picks))) (nth picks index))]
    (if (or (nil? target) (= :no-pick pick))
      "abstain-empty-field"
      {"target" target "cascade" (if (:construction selection) (or (:chosen-class selection) (:cascade-digest selection))
                                      (policy-identity (:cascade-id selection)))})))

(defn- pick-relation
  "Modern constructions retain the chosen classify-target receipt digest.
  Historical records keep the weaker class-and-target digest fallback."
  [selection]
  (let [chosen (first (filter #(= (:cascade-digest selection) (:digest %)) (:cascades selection)))
        relation (:selected-relation selection)]
    (if (:construction selection)
      (if (and (known-relations (get-in chosen [:relation :class]))
               (get-in chosen [:relation :receipt-digest]))
        {"observed" (get-in chosen [:relation :receipt-digest])} "absent")
    (if (known-relations relation)
      {"observed" (action-identity/digest
                   {:selected-relation relation
                    :target (get-in selection [:selection :selected :target])})}
      "absent"))))

(defn- pick-reach
  "New fields retain the measured status and declared prior kit. Legacy
  records continue to distinguish an explicit receipt from makeup defaults."
  [selection makeup]
  (let [target (get-in selection [:selection :selected :target])
        reach (get-in selection [:selection :selected :reach])
        fact-reach (get-in selection [:field-facts :admitted target :reach])
        receipt (get selection reach-receipt-key)]
    (cond
      (contains? selection :field-facts)
      (if (and (number? (:value fact-reach)) (<= 0 (:value fact-reach) 1)
               (:source fact-reach) (get-in fact-reach [:inputs :pin])
               (= reach (:value fact-reach)))
        {"observed" (action-identity/digest fact-reach)} "absent")
      (nil? reach) "absent"
      receipt {"observed" (action-identity/digest receipt)}
      (and makeup (= reach (:default-reach makeup))) "defaulted"
      :else "absent")))

(defn- ending-provenance [ending]
  (if (or (:classification-absence ending)
          (contains? #{nil :not-yet-evaluated} (:ending-class ending)))
    "absent"
    {"observed" (action-identity/digest ending)}))

(defn- selection-path [selection makeup]
  (let [{:keys [status receipt]} (:outer/learning selection)
        path {"receipt" {"observed" (action-identity/digest selection)}
              "pickRelation" (pick-relation selection)
              "pickReach" (pick-reach selection makeup)}]
    (case status
      :applied (let [row [(:run-id receipt) (ending-provenance (:ending receipt))]]
                 (assoc path "pendingRun" row "consumedRuns" [row] "learningApplied" true))
      ;; the record has no run id for a deferred ending
      :deferred (assoc path "pendingRun" [0 "absent"] "consumedRuns" [] "learningApplied" false)
      ;; nothing was pending: no run to consume, nothing applied, nothing deferred
      :none (assoc path "consumedRuns" [] "learningApplied" false)
      (not-recomputable "no :outer/learning status on the selection"))))

(defn selection-absence-paths
  "Q7's rule on the selection path.  The field exclusions and knowledge requests
  are population-wide evidence about alternatives, as the work path's rejected
  candidates are, and stay out."
  [record]
  (let [selection (:outer/selection record)
        chosen-id (:cascade-id selection)
        chosen-class (:chosen-class selection)
        pick (get-in selection [:selection :selected :target])
        ;; L5-4f review: since L5-4b the section carries every admitted member's
        ;; facts and every cascade's row.  Those are population-wide evidence
        ;; about alternatives; the path is what the chosen decision rests on: the
        ;; chosen cascade's row, its class, the chosen pick's own field fact, and
        ;; the section's own carriers (construction, abstention, previous,
        ;; forbidden, learning, prediction, receipt).
        on-path (-> selection
                    (dissoc :outer-field/excluded :outer-score/excluded :knowledge-producer-requests
                            :field-facts :cascades :classes :cascade-picks :cascade-pick-relations
                            :cascade-scores :tied-cascades)
                    (assoc :chosen-cascade (first (filter #(= chosen-id (:id %)) (:cascades selection)))
                           :chosen-class-row (first (filter #(= chosen-class (:id %)) (:classes selection)))
                           :pick-fact (when pick (get-in selection [:field-facts :admitted pick]))))
        roots [[[:outer/selection] on-path]
               [[:outer-task-selection] (:outer-task-selection record)]]]
    (vec
     (mapcat
      (fn [[root x]]
        (when (some? x)
          (for [[p _] (paths-with x
                          #(and (map? %)
                                (let [m (into {} %)]
                                  (or (contains? absence-statuses (:status m))
                                      (contains? absence-statuses (:outcome m))
                                      (= :typed-refusal (:kind m))
                                      (= :classification-receipt-absent (:reason m))))))]
            (vec (concat root p)))))
      roots))))

(defn selection-facts-for-record
  "The selection run in a tick record's `:outer/selection`, as its own RunFacts.
  The projector labels the kind; nothing in the record does."
  [record record-path snap {:keys [makeup-path phase-log-path]
                            :or {makeup-path default-makeup-path
                                 phase-log-path default-phase-log-path}}]
  (let [tick-path-original record-path
        reference (:selection-record record)
        standalone? (= selection-record/schema (:schema record))
        completion (when (and standalone? (.isFile (io/file record-path)))
                     (selection-record/read-completion {:path (.getCanonicalPath (io/file record-path)) :sha256 (selection-record/file-sha record-path)}))
        terminal-ref (or reference (:selection-record completion))
        chain (when terminal-ref (selection-record/read-chain terminal-ref))
        source (if chain (:record (last (:entries chain))) record)
        disagreement? (and reference (contains? record :outer/selection)
                           (not= (:outer/selection record) (:outer/selection source)))
        record-path (or (:path terminal-ref) record-path)
        record (if reference (assoc source :decision (:decision record)
                                   :world-at-selection (:world-at-selection record)) source)
        work (facts-for-record
              (cond-> record reference (assoc :selection-record reference))
              (if reference tick-path-original record-path) snap)
        selection (:outer/selection record)
        section? (seq selection)
        makeup (pinned-makeup makeup-path (:makeup/sha256 selection))
        nr not-recomputable
        carried (fn [reason] (if section? (nr reason) (nr no-section)))
        field-facts (:field-facts selection)
        admitted (if field-facts (keys (:admitted field-facts)) (:outer-field/admitted selection))
        exclusions (if field-facts (:excluded field-facts) (:outer-field/excluded selection))
        excluded (map :target exclusions)
        facts (merge
               (zipmap run-fact-fields (repeat (nr no-carrier)))
               (select-keys (:facts work)
                            ["openMissions" "openExcursions" "openTickets"
                             "factorizedPreference" "calibrationAuthorizations"
                             "factorizationAuthorizations"])
               (zipmap ["targetConstruction" "constructorPatternCount" "libraryPatternCount"]
                       (repeat (carried no-library-pin)))
               (zipmap ["previousChoice" "previousOutcome" "previousInputDigest"
                        "currentInputDigest"]
                       (repeat (carried no-previous-decision)))
               (zipmap ["policyPreferences" "gTerms" "policiesWithRiskTerm"
                        "policiesWithAmbiguityTerm" "policiesWithInformationTerm"]
                       (repeat (carried no-terms)))
               (zipmap ["abstentionG" "abstentionPrediction"]
                       (repeat (carried no-stopped-score)))
               {"kind" "selection"
                "policyGScale" g-scale
                "policyPrediction" (carried one-prediction)
                "interpretationOrder" (selection-interpretation-order record phase-log-path)}
               (if section?
                 (merge
                  (if (:construction selection)
                    (merge (constructed-menu-facts selection (sorted-ids (distinct (concat admitted excluded))))
                           (when (:classes selection) (selection-preferences (:classes selection)))
                           (when (:choice-input-digest selection) (selection-history-facts selection)))
                    (menu-facts selection makeup))
                  {"enumeratedTasks" (sorted-ids (distinct (concat admitted excluded)))
                   "currentChoice" (selection-decision selection)
                   "pathAbsenceCount" (count (selection-absence-paths record))
                   "selectionPath" (selection-path selection makeup)}
                  (selection-refusal-history-facts selection))
                 (zipmap ["comparedPolicies" "constructedCascades" "policyG" "cascadesWithoutG"
                          "enumeratedTasks" "currentChoice" "pathAbsenceCount" "selectionPath"]
                         (repeat (nr no-section)))))
        facts (if disagreement?
                (into {} (map (fn [[k v]] [k (if (#{"kind" "policyGScale"} k) v
                                                (nr "selection-reselected-after-decision"))]) facts))
                facts)
        outcome (:selection-outcome source)
        not-exercised? (= :not-exercised (:status outcome))
        ;; The record may hold the field although no selection ran. An empty
        ;; enumeration here would read as the empty-field abstention.
        field (seq (concat admitted excluded))
        facts (if not-exercised?
                (let [why (nr (str "selection not exercised: " (some-> (:path outcome) name)))]
                  (into {} (map (fn [[k v]]
                                  [k (cond (kept-when-not-exercised k) v
                                           (and field (= "enumeratedTasks" k)) (sorted-ids (distinct field))
                                           :else why)]) facts)))
                facts)
        tick-path (get-in completion [:tick-record :path])]
    {:facts facts
     :sources (assoc (merge (zipmap (keys facts) (repeat (str record-path " [:outer/selection]")))
                            (select-keys (:sources work) ["openMissions" "openExcursions" "openTickets"]))
                     "interpretationOrder"
                     (or phase-log-path ":phase-log path absent"))
     :diagnostics {:recordKind (if (= selection-record/schema (:schema source)) "selection-record" "tick-record")
                   :selectionOutcome outcome
                   :selectionStatus (when not-exercised? "not-exercised")
                   :selectionBypass (when not-exercised? (some-> (:path outcome) name))
                   :selectionChainLength (or (:length chain) (:chain-index source))
                   :selectionRecordDisagreement (when disagreement? :selection-reselected-after-decision)
                   :workRecord tick-path
                   :workRunStatus (when (and (nil? reference)
                                             (= selection-record/schema (:schema source))
                                             (or (#{:refused :abstained} (:status outcome))
                                                 (and not-exercised? (nil? tick-path))))
                                    "not-run")
                   :fieldFacts (when-let [roster (:open-tasks field-facts)]
                                 {:selectorRoster roster
                                  :censusDifference
                                  (into {} (for [[k field] [[:missions "openMissions"]
                                                           [:excursions "openExcursions"]
                                                           [:tickets "openTickets"]]
                                                 :let [c (set (get facts field)) r (set (get roster k))]]
                                             [k {:census-only (sorted-ids (clojure.set/difference c r))
                                                 :selector-only (sorted-ids (clojure.set/difference r c))}]))})
                   :taskExclusions (mapv #(select-keys % [:target :reason :pin]) exclusions)
                   :absencePaths (mapv pr-str (selection-absence-paths record))
                   :notRecomputable (vec (for [[k v] facts
                                               :when (and (map? v) (contains? v "not-recomputable"))]
                                           k))}}))

(defn export-for-kind
  "A record's RunFacts export for one of its two runs.  Work is the default."
  [record record-path snap {:keys [kind] :as opts}]
  (case (or kind "work")
    "work" (facts-for-record record record-path snap)
    "selection" (selection-facts-for-record record record-path snap (dissoc opts :kind))))

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
          {:strs [--kind --makeup --phase-log]} (apply hash-map (drop 2 args))
          snap (snapshot)]
      (println (json/generate-string
                (export-for-kind (read-edn path) path snap
                                 (cond-> {:kind --kind}
                                   --makeup (assoc :makeup-path --makeup)
                                   --phase-log (assoc :phase-log-path --phase-log)))
                {:pretty true})))
    (let [[run-dir output] args
          dir (or run-dir "data/wm-runs")
        out (or output "holes/labs/wm-contract/RUN-FACTS-history-2026-09-30.md")
        paths (->> (.listFiles (io/file dir))
                   (filter #(.isFile ^java.io.File %))
                   (filter #(re-matches #"tick-run-record-[^.]+\.edn" (.getName ^java.io.File %)))
                   (sort-by #(.getName ^java.io.File %))
                   (mapv #(.getPath ^java.io.File %)))
        snap (snapshot)
        rows (history-rows paths snap)]
      (spit out (markdown rows))
      (println (json/generate-string {:runs (count rows) :output out
                                      :snapshot (:pins snap)})))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
