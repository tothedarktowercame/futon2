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

(defn- hex64?
  "Same digest shape the compiler enforces (cascade_enactment_compiler/hex64?):
  a lowercase 64-char hex sha256 string."
  [x]
  (and (string? x) (boolean (re-matches #"[0-9a-f]{64}" x))))

(defn- completion-check-valid?
  "A compiled clause's :completion-check, per the producers that mint it:
  authored clauses (futon2.aif.wm.authored.*, e.g. req-fbad4b8b) carry
  [:check-name-keyword <observable condition string>], and the compiler's
  square-derived completions (cascade_enactment_compiler/square-derived-completion)
  carry [:square-B-observed <B-theory elems>].  Both are vectors whose first
  element names the check as a keyword and whose second element is the
  observed condition.  Anything else (including nil and boolean false) is
  not a compiled completion check."
  [check]
  (and (vector? check) (>= (count check) 2)
       (keyword? (first check)) (some? (second check))))

(defn- step-malformation
  "Returns a reason keyword when a recorded step cannot be rendered as a
  step/occurrence/clause/check; nil when the step is well-formed."
  [step]
  (cond
    (nil? (:occurrence-id step)) :step-occurrence-id-missing
    (not (map? (:clause step))) :step-clause-missing
    (nil? (get-in step [:clause :operation])) :step-operation-missing
    (nil? (get-in step [:clause :completion-check])) :step-completion-check-missing
    (not (completion-check-valid? (get-in step [:clause :completion-check])))
    :step-completion-check-malformed
    :else nil))

(defn- compiled-plan-steps
  "Validate every recorded step's occurrence/clause/check shape and occurrence
  uniqueness before rendering.  Returns [:ok steps] or [:invalid reason]."
  [steps]
  (or (when-let [bad (some step-malformation steps)]
        [:invalid bad])
      (when (let [ids (map :occurrence-id steps)]
              (not= (count ids) (count (distinct ids))))
        [:invalid :duplicate-occurrence-id])
      [:ok steps]))

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
  Returns {:status :absent :reason k} when the plan is absent, malformed
  (step shape, digest shape) or too large for the promised budget -- it can
  never silently render as complete.  Otherwise
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
      (not (and (hex64? (:selected-cascade-digest plan))
                (hex64? (:compiled-cascade-digest plan))))
      {:status :absent :reason :compiled-operational-plan-digests-malformed}
      :else
      (let [[validity bad] (compiled-plan-steps (:steps plan))]
        (if (= :invalid validity)
          {:status :absent :reason :compiled-operational-plan-step-invalid
           :detail bad}
          (let [steps (:steps plan)
                total (count steps)
                limit 18000
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
                marker-for (fn [shown]
                             (let [omitted (- total shown)]
                               (when (pos? omitted)
                                 (str "[" shown " of " total
                                      " operational steps shown; " omitted
                                      " omitted for space]\n"))))
                fits? (fn [shown body]
                        (<= (count (str header body (marker-for shown))) limit))]
            ;; The 18,000-character budget applies to the FINAL text
            ;; including the header and the counted omission marker.  When
            ;; even the bare header plus a zero-shown marker exceeds it,
            ;; the digests cannot be rendered truthfully within the
            ;; promise; refuse as a typed absence instead of silently
            ;; clipping recorded identity.
            (if-not (fits? 0 "")
              {:status :absent :reason :compiled-operational-plan-over-budget
               :detail {:header-chars (count header) :limit limit}}
              (let [blocks (compiled-plan-blocks plan)
                    budget (reduce (fn [acc block]
                                     (let [shown (inc (:shown acc))
                                           body (str (:body acc) block)]
                                       (if (fits? shown body)
                                         {:shown shown :body body}
                                         (reduced acc))))
                                   {:shown 0 :body ""}
                                   blocks)
                    shown (:shown budget)
                    body (:body budget)
                    omitted (- total shown)]
                {:status :rendered
                 :steps-shown shown :steps-total total :omitted omitted
                 :text (str header body (marker-for shown))}))))))))

(defn compiled-completion-criteria-text
  "The acceptance-criterion rendering for a :compiled-cascade construction:
  every step's completion check VERBATIM with its occurrence identity,
  operation, and both recorded digests, in RECORDED order (no deduplication:
  steps are rendered as recorded; a duplicate occurrence id is rejected up
  front in compiled-operational-plan-text).  Typed-absent when the compiled
  plan is absent, malformed, or the criteria header alone exceeds the
  12,000-character budget; never silently renders as complete.  Nothing is
  coerced into C3/C4 locator classes -- these are the compiled clauses'
  own checks."
  [construction]
  (let [rendered (compiled-operational-plan-text construction)]
    (if (and (= :absent (:status rendered))
             ;; A plan-header over-budget is about the plan's own 18,000-char
             ;; budget (guards/application order); the criteria header is a
             ;; different, smaller text judged by its own 12,000-char budget
             ;; below, so it is not relayed.
             (not= :compiled-operational-plan-over-budget (:reason rendered)))
      rendered
      (let [plan (:operational-plan construction)
            ;; Sequential, preserving recorded order; keys are not collapsed.
            entries (mapv (fn [step]
                            {:occurrence-id (:occurrence-id step)
                             :operation (get-in step [:clause :operation])
                             :check (get-in step [:clause :completion-check])})
                          (:steps plan))
            lines (mapv (fn [{:keys [occurrence-id operation check]}]
                          (str "  " (pr-str occurrence-id) " (" operation ") \u2014 "
                               (pr-str check)))
                        entries)
            total (count lines)
            header (str "ACCEPTANCE CRITERIA (the tests your work will be measured against):\n"
                        "These are the reviewed completion checks of the compiled cascade "
                        "(selected digest " (:selected-cascade-digest plan)
                        ", compiled digest " (:compiled-cascade-digest plan)
                        "), verbatim.\n")
            ;; The 12,000-character budget includes the header, the joined
            ;; lines AND the counted omission marker.
            limit 12000
            marker-for (fn [shown]
                         (let [omitted (- total shown)]
                           (when (pos? omitted)
                             (str "[" shown " of " total " checks shown; "
                                  omitted " omitted for space]\n"))))
            fits? (fn [shown kept]
                    (<= (count (str header (str/join "\n" kept) "\n"
                                       (marker-for shown)))
                        limit))]
        ;; The criteria header (both digests) can alone exceed the budget;
        ;; refuse as typed absence rather than silently clipping digests.
        (if-not (fits? 0 [])
          {:status :absent :reason :compiled-completion-criteria-over-budget
           :detail {:header-chars (count header) :limit limit}}
          (let [budget (reduce (fn [acc line]
                                 (let [shown (inc (:shown acc))
                                       kept (conj (:lines acc) line)]
                                   (if (fits? shown kept)
                                     {:shown shown :lines kept}
                                     (reduced acc))))
                               {:shown 0 :lines []}
                               lines)
                shown (:shown budget)]
            (str header
                 (str/join "\n" (:lines budget)) "\n"
                 (marker-for shown))))))))

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
