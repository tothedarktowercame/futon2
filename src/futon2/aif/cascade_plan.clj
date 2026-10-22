(ns futon2.aif.cascade-plan
  "Pure presentation of recorded construction evidence; never observes or infers guards."
  (:require [futon2.aif.load-identity :as load-identity]
            [clojure.string :as str]))

(load-identity/register! *ns* *file*)

(def max-plan-chars 24000)

(defn- text [x]
  (cond
    (nil? x) "not recorded"
    (map? x) (str/join "; " (for [[k v] (sort-by (comp str key) x)]
                              (str (text k) " = " (text v))))
    (set? x) (str/join ", " (sort (map text x)))
    (sequential? x) (str/join " / " (map text x))
    :else (str x)))

(defn- bounded [s limit]
  (if (<= (count s) limit)
    s
    (let [prefix (max 0 (- limit 40))
          removed (- (count s) prefix)]
      (str (subs s 0 prefix) "\n[truncated " removed " chars]\n"))))

(defn- condition-text [{:keys [condition status witness]}]
  (let [{:keys [token expected observation]} witness
        evidence (:evidence observation)]
    (str "  - " (if token (str (text token) " expected " (text expected)) (text condition))
         ": " (if (= :established status) "established" "not established")
         " (recorded status: " (text status) ")"
         "; check " (text (:check observation))
         "; observed " (text (:observed observation))
         "; resolved sha " (text (:resolved-sha evidence))
         ;; :decl is the line the locator searches for, not the line it found
         (when (contains? evidence :decl)
           (str "; looked for line " (text (:decl evidence))))
         "; evidence " (text (dissoc evidence :decl)) "\n")))

(defn- pattern-text [index pattern boxes holes receipts]
  (let [id (if (map? pattern) (:id pattern) pattern)
        box (first (filter #(= (str id) (str (:id %))) boxes))
        hole (first (filter #(= (str id) (str (:unfolded-pattern %))) holes))
        receipt (or (:interpretation-receipt box) (get receipts id))
        source (:source receipt)
        conditions (or (:conditions box) (:guard-evidence hole))]
    (str "pattern " (inc index) ": " (text id) "\n"
         "  flexiarg: " (text (:path source)) "\n"
         "  sha256: " (text (:sha256 source)) "\n"
         "  reading: " (text (:reading receipt)) "\n"
         "  scope limit: " (text (:scope-limit receipt)) "\n"
         "  observation limit: " (text (:observation-limit receipt)) "\n"
         "  guards:\n"
         (if (seq conditions)
           (apply str (map condition-text conditions))
           (str "  - not established: no recorded guard checks; declared guard: "
                (text (:guard pattern)) "\n"))
         "  produces: " (text (or (:produces box) (:produces pattern))) "\n")))

(defn- step-text
  [{:keys [occurrence-id pattern provenance] :as step}]
  (let [clause (:clause step)]
    (str "step " occurrence-id " (pattern " (text pattern) ")\n"
         "  operation: " (text (:operation clause)) "\n"
         "  capability: " (text (:capability clause)) "\n"
         "  consumes: " (text (:consumes clause))
         " -> produces: " (text (:produces clause)) "\n"
         "  completion check: " (pr-str (:completion-check clause)) "\n"
         "  authored by: " (text (:authoring/attribution clause)) "\n"
         "  provenance: reading-record " (text (:reading-record-id provenance))
         "; source sha256 " (text (:source-sha256 provenance)) "\n")))

(defn- compiled-plan-blocks
  "Per-step text blocks for a compiled operational plan."
  [plan]
  (map step-text (:steps plan)))

(defn compiled-operational-plan-text
  "Typed rendering of a :compiled-cascade construction's operational plan.
  Returns {:status :absent :reason k} when the plan is absent or malformed --
  it can never silently render as complete.  Otherwise
  {:status :rendered :text s :steps-shown n :steps-total m} where the text
  names BOTH recorded digests (selected and compiled cascade), every rendered
  step's occurrence identity, operation, capability, consumption/product,
  VERBATIM completion check, authoring attribution and recorded provenance,
  plus the recorded guard/order relation and effect status.  Omission under
  the character budget is counted explicitly, never silent."
  [construction]
  (let [plan (:operational-plan construction)]
    (cond
      (not (and (map? plan) (sequential? (:steps plan)) (seq (:steps plan))
                (every? map? (:steps plan))))
      {:status :absent :reason :compiled-operational-plan-not-renderable}
      (not (and (string? (:selected-cascade-digest plan))
                (string? (:compiled-cascade-digest plan))))
      {:status :absent :reason :compiled-operational-plan-digests-missing}
      :else
      (let [total (count (:steps plan))
            header (str "selected cascade digest: " (:selected-cascade-digest plan) "\n"
                        "compiled cascade digest: " (:compiled-cascade-digest plan) "\n"
                        "application order: "
                        (if (seq (:application-order plan))
                          (text (:application-order plan))
                          "(none recorded)")
                        "\n"
                        "guards: "
                        (if (seq (:guards plan))
                          (text (:guards plan))
                          "(none recorded)")
                        "\n"
                        "effect status: " (text (:effect-status plan)) "\n")
            blocks (compiled-plan-blocks plan)
            ;; Cumulative budget with explicit counted omission: keep whole
            ;; steps while they fit; the final line counts what was dropped.
            budget
            (reduce (fn [acc block]
                      (let [candidate (str (:body acc) block)]
                        (if (<= (count candidate) 18000)
                          {:shown (inc (:shown acc)) :body candidate}
                          (reduced acc))))
                    {:shown 0 :body ""}
                    blocks)
            shown (:shown budget)
            body (:body budget)
            omitted (- total shown)]
        {:status :rendered
         :steps-shown shown :steps-total total :omitted omitted
         :text (str header body
                    (when (pos? omitted)
                      (str "[" shown " of " total " operational steps shown; "
                           omitted " omitted for space]\n")))}))))

(defn compiled-completion-criteria-text
  "The acceptance-criterion rendering for a :compiled-cascade construction:
  every step's completion check VERBATIM with its occurrence identity,
  operation, and both recorded digests.  Typed-absent when the compiled plan
  is absent or malformed; never silently renders as complete.  Nothing is
  coerced into C3/C4 locator classes -- these are the compiled clauses'
  own checks."
  [construction]
  (let [rendered (compiled-operational-plan-text construction)]
    (if (= :absent (:status rendered))
      rendered
      (let [checks (into {}
                         (map (fn [step]
                                {[(:occurrence-id step)
                                  (get-in step [:clause :operation])]
                                 (get-in step [:clause :completion-check])}))
                         (:steps (:operational-plan construction)))
            lines (for [[[occ op] check] checks]
                    (str "  " (pr-str occ) " (" op ") — " (pr-str check)))
            total (count checks)
            budget
            (reduce (fn [acc line]
                      (let [candidate (conj (:lines acc) line)]
                        (if (<= (count (str/join "\n" candidate)) 12000)
                          {:shown (inc (:shown acc)) :lines candidate}
                          (reduced acc))))
                    {:shown 0 :lines []}
                    lines)
            shown (:shown budget)
            omitted (- total shown)]
        (str "ACCEPTANCE CRITERIA (the tests your work will be measured against):\n"
             "These are the reviewed completion checks of the compiled cascade "
             "(selected digest " (:selected-cascade-digest (:operational-plan construction))
             ", compiled digest " (:compiled-cascade-digest (:operational-plan construction))
             "), verbatim.\n"
             (str/join "\n" (:lines budget)) "\n"
             (when (pos? omitted)
               (str "[" shown " of " total " checks shown; " omitted
                    " omitted for space]\n")))))))

(defn- compiled-plan-section
  "Plan section for a :compiled-cascade construction: the rendered compiled
  plan, or an explicit typed-absence marker when it cannot be rendered."
  [construction]
  (let [rendered (compiled-operational-plan-text construction)]
    (if (= :absent (:status rendered))
      (str "COMPILED OPERATIONAL PLAN: NOT RENDERABLE (" (name (:reason rendered))
           ") -- the construction's compiled plan is absent or malformed; "
           "nothing here may be read as its completion checks.\n")
      (:text rendered))))

(defn compiled-cascade?
  [construction]
  (= :compiled-cascade (:construction-kind construction)))

(defn cascade-plan-text
  "Render patterns in precedence order, then holes and wires. Maximum 24,000
  characters: 18,000 shared equally between pattern blocks, 2,800 each for
  holes and wires, with room for headings. Every truncation counts omitted
  characters explicitly. Extremely numerous patterns that cannot each fit a
  truncation marker are covered by a final counted section truncation."
  [construction]
  (let [wiring (or (:wiring construction)
                   (get-in construction [:fold-output :wiring])
                   (get-in construction [:compiled-fold :wiring]))
        boxes (:boxes wiring)
        holes (or (get-in construction [:fold-output :policy-holes])
                  (:policy-holes wiring) (:policy-holes construction))
        patterns (or (seq (:precedence construction))
                     (seq (map :id boxes)) (:shown construction))
        compiled? (compiled-cascade? construction)
        blocks (map-indexed #(pattern-text %1 %2 boxes holes
                                          (:interpretation-receipts construction)) patterns)
        allowance (quot 18000 (max 1 (count patterns)))
        pattern-section (if (< allowance 40)
                          (bounded (apply str blocks) 18000)
                          (apply str (map #(bounded % allowance) blocks)))
        hole-section (if (seq holes)
                       (str "holes:\n"
                            (str/join "\n" (map #(str "- " (text %)) holes)))
                       "holes: none")
        wires (:wires wiring)
        wire-section (if (seq wires)
                       (str "wires:\n"
                            (str/join "\n" (map #(str "- " (text %)) wires)))
                       (if (some? wires) "wires: none" "wires: not recorded"))]
    (str "Recorded cascade plan (limit " max-plan-chars " chars).\n"
         (if compiled?
           (compiled-plan-section construction)
           (if (seq patterns) pattern-section "patterns: none recorded\n"))
         "\n" (bounded hole-section 2800) "\n" (bounded wire-section 2800) "\n")))
