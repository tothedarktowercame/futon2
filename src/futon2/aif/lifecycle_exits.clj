(ns futon2.aif.lifecycle-exits
  "Secondary mission wants from the lifecycle definition; no flight wiring or IO."
  (:require [clojure.string :as str]
            [futon2.aif.mission-criteria :as mc]
            [futon2.aif.cascade-problems :as problems]))

(def phases [:HEAD :IDENTIFY :MAP :DERIVE :ARGUE :VERIFY :INSTANTIATE :DOCUMENT])

(defn- phase-name [title]
  (some-> (re-find #"^(?:\d+\.\s+)?(HEAD|IDENTIFY|MAP|DERIVE|ARGUE|VERIFY|INSTANTIATE|DOCUMENT)(?:\s|$)"
                   (str title)) second keyword))

(defn definition-exits [definition-text]
  (let [at-line (vec (rest (reductions
                           (fn [phase line]
                             (if-let [[_ _ title] (re-matches #"^(#{1,3})\s+(.*)$" line)]
                               (phase-name title) phase))
                           nil (str/split-lines definition-text))))
        exits (mapv (fn [c] {:phase (get at-line (dec (:line c))) :stated (:stated c)})
                    (filter #(= :phase-exit (:kind %)) (mc/criteria "definition" definition-text)))]
    (when-not (= phases (mapv :phase exits))
      (throw (ex-info "Lifecycle definition must yield the eight ordered phase exits"
                      {:kind :invalid-lifecycle-definition :found exits :expected phases})))
    exits))

(defn lifecycle-shaped?
  "Return the boolean and the recognized level-2 phases, in document order."
  [mission-text]
  (let [found (vec (distinct (keep (fn [line]
                                    (when-let [[_ title] (re-matches #"^##\s+(.*)$" line)]
                                      (phase-name title)))
                                  (str/split-lines mission-text))))]
    {:lifecycle-shaped? (boolean (seq found)) :phases found}))

(defn supplied [mission-id mission-text definition-text]
  (let [shape (lifecycle-shaped? mission-text)]
    (if-not (:lifecycle-shaped? shape)
      (assoc shape :criteria [] :reason :not-lifecycle-shaped)
      (let [present (set (keep #(when (= :phase-exit (:kind %)) (phase-name (:phase %)))
                               (mc/criteria mission-id mission-text)))]
        (assoc shape :criteria
               (mapv (fn [{:keys [phase stated]}]
                       ;; Delegate to the existing public reader, including its token derivation.
                       (let [c (first (mc/criteria mission-id (str "## " (name phase) "\n" stated)))]
                         (assoc (select-keys c [:kind :stated :token])
                                :phase phase :role :how :supplied-by :lifecycle-definition)))
                     (remove #(present (:phase %)) (definition-exits definition-text))))))))

(defn verdict-decl
  "The written Met verdict observed by C4; the person supplies the judgment."
  [phase]
  (str "**" (name phase) " exit: Met.**"))

(defn verdict-locators
  "Convention-derived C4 locators for supplied exits only. Criterion provenance
  is marked by wants when these locators are admitted; locator shape stays C4."
  [criteria {:keys [repo path sha]}]
  (into {} (for [{:keys [token phase supplied-by]} criteria
                 :when (= :lifecycle-definition supplied-by)]
             [token {:class :C4 :repo repo :sha (or sha "HEAD")
                     :path path :decl (verdict-decl phase)}])))

(defn section-verdicts
  "Reporting only: one row per written verdict (duplicates retained), or a
  {:phase p :verdict nil} row when absent. :in-section? refers to the enclosing
  level-2 heading; misplaced rows retain its title, nil before any such section.
  This does not constrain C4's whole-file observation. A verdict line is read
  exactly where C4 would observe its declaration: at the start of a line
  (after optional indent), followed by end of line, whitespace or one of
  : ( { [ -- so prose after the verdict on the same line is allowed, as in
  \"**HEAD exit: Met.** The operator's anchor turn ...\" (claude-17's finding
  on M-象-2000, 2026-09-27: the two readings must agree)."
  [mission-text]
  (let [{:keys [rows]}
        (reduce
         (fn [{:keys [heading] :as state} line]
           (if-let [[_ title] (re-matches #"^##\s+(.*)$" line)]
             (assoc state :heading title)
             (if-let [[_ phase verdict]
                      (re-matches #"^\s*\*\*(HEAD|IDENTIFY|MAP|DERIVE|ARGUE|VERIFY|INSTANTIATE|DOCUMENT) exit: (Met|Not met|Not started)\.\*\*(?:$|[\s:({\[].*$)" line)]
               (let [p (keyword phase) own? (= p (phase-name heading))]
                 (update state :rows conj
                         (cond-> {:phase p :verdict (get {"Met" :met "Not met" :not-met
                                                         "Not started" :not-started} verdict)
                                  :in-section? own?}
                           (not own?) (assoc :misplaced-under heading))))
               state)))
         {:heading nil :rows []} (str/split-lines mission-text))]
    (vec (mapcat (fn [p] (or (seq (filter #(= p (:phase %)) rows))
                             [{:phase p :verdict nil}])) phases))))

(defn wants
  "Only checkable published locators admit secondary wants. OBSERVE returns a boolean."
  [criteria published-locators observe]
  (let [criteria (mapv (fn [c]
                              (let [loc (get published-locators (:token c))]
                                (cond-> (assoc c :role :how)
                                  (and (= :lifecycle-definition (:supplied-by c))
                                       (= :C4 (:class loc))
                                       (= (verdict-decl (:phase c)) (:decl loc)))
                                  (assoc :located-by :verdict-line-convention))))
                            criteria)
        located? #(contains? problems/checkable-classes
                             (:class (get published-locators (:token %))))
        admitted (filterv located? criteria)
        missing (filterv (complement located?) criteria)
        locators (select-keys published-locators (map :token admitted))]
    {:wants (mapv :token admitted) :locators locators
     :universe (into {} (for [[token locator] locators]
                         (let [value (observe locator)]
                           (when-not (boolean? value)
                             (throw (ex-info "Observation must be boolean"
                                             {:kind :lifecycle-observation-not-boolean :token token})))
                           [token value])))
     :criteria-by-token (into {} (map (juxt :token identity) criteria))
     :unlocated (mapv #(assoc (select-keys % [:token :stated :role :phase])
                              :reason :no-admitted-locator) missing)
     :to-ask missing}))

(defn current-phase
  "Read the first bold Status line. A plain PHASE (date) names the phase.
  In semicolon composites, take the latest lifecycle phase explicitly marked
  complete, current or pending: 'HEAD complete; IDENTIFY pending' means IDENTIFY,
  whose exit is not yet met. Other status prose (including COMPLETE alone)
  is not interpreted as a phase. mission-registry's status regex is private."
  [mission-text]
  (if-let [line (first (filter #(re-find #"^\s*\*\*Status:\*\*" %)
                              (str/split-lines mission-text)))]
    (let [body (str/trim (str/replace-first line #"^\s*\*\*Status:\*\*\s*" ""))
          parts (str/split body #";")
          pattern (if (> (count parts) 1)
                    #"^\s*(HEAD|IDENTIFY|MAP|DERIVE|ARGUE|VERIFY|INSTANTIATE|DOCUMENT)\s+(?:complete|current|pending)\b"
                    #"^\s*(HEAD|IDENTIFY|MAP|DERIVE|ARGUE|VERIFY|INSTANTIATE|DOCUMENT)(?:\s|$)")
          named (set (keep #(some-> (re-find pattern %) second keyword) parts))]
      (if-let [phase (last (filter named phases))]
        {:phase phase}
        {:absent :status-phase-unrecognised :line line}))
    {:absent :status-line-missing}))

(defn flight-exits
  "Pure supplied secondary wants, through the current phase only. Observation
  and an in-section Met line must both hold. Future/unknown phases are listed,
  never asked about. Mission-authored exits remain the existing reader's job."
  [mission-id mission-text definition-text {:keys [observe] :as opts}]
  (let [current (current-phase mission-text)
        cs (:criteria (supplied mission-id mission-text definition-text))
        reached (if (:phase current)
                  (set (take (inc (.indexOf phases (:phase current))) phases)) #{})
        active (filterv #(reached (:phase %)) cs)
        later (remove #(reached (:phase %)) cs)
        reports (group-by :phase (section-verdicts mission-text))
        locs (verdict-locators active opts)
        result (wants active locs observe)]
    (assoc result
           :current-phase current
           :universe (into {} (for [{:keys [phase token]} active]
                                [token (boolean (and (get-in result [:universe token])
                                                     (some #(and (= :met (:verdict %)) (:in-section? %))
                                                           (reports phase))))]))
           :criteria-by-token
           (into {} (for [[token c] (:criteria-by-token result)
                          :let [rows (reports (:phase c))
                                own? (some #(and (= :met (:verdict %)) (:in-section? %)) rows)
                                misplaced (first (filter #(and (= :met (:verdict %))
                                                                (not (:in-section? %))) rows))]]
                      [token (cond-> c
                               (and misplaced (not own?))
                               (assoc :not-counted {:reason :verdict-line-misplaced
                                                    :misplaced-under (:misplaced-under misplaced)}))]))
           :not-started (mapv #(assoc (select-keys % [:phase :token])
                                     :reason (if (:phase current) :phase-not-reached :current-phase-unknown))
                              later))))
