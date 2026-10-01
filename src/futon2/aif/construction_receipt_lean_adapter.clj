(ns futon2.aif.construction-receipt-lean-adapter
  "Identity-preserving adapter from a pinned runtime construction receipt to
  DarkTower.WarMachine.ConstructionReceipt.  The generated theorem evaluates
  the decoded value; source occurrence is not treated as correspondence."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.candidate-derivations :as candidate-derivations]
            [futon2.aif.action-identity :as action-identity]
            [futon2.aif.cascade-equivalence :as equivalence]
            [futon2.aif.g-term-decomposition :as decomposition]
            [futon2.aif.wm.terminal-receipt :as terminal-receipt])
  (:import (java.io PushbackReader StringReader)
           (java.nio ByteBuffer)
           (java.nio.charset CodingErrorAction StandardCharsets)
           (java.security MessageDigest)))

(def schema :wm/construction-receipt-lean-input-v1)

(defn- refuse! [kind data]
  (throw (ex-info (name kind) (assoc data :refusal kind))))

(defn- hex [bytes]
  (apply str (map #(format "%02x" (bit-and 0xff %)) bytes)))

(defn sha256 [bytes]
  (hex (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bytes)))))

(defn- one!
  [kind values data]
  (if (= 1 (count values))
    (first values)
    (refuse! kind (assoc data :matches (count values)))))

(defn- runtime-token
  "Undo only the target qualification added to tokens in the recorded
   selection candidate. Relation witnesses are recorded in the constructor's
   bare token domain, so a differently qualified token is a refusal."
  [target token]
  (if (vector? token)
    (if (and (= 2 (count token)) (= target (first token)))
      (second token)
      (refuse! :adapter/mixed-target-token {:target target :token token}))
    token))

(defn- recorded-needs
  [target guard]
  (when-not (and (map? guard) (vector? (:clauses guard)))
    (refuse! :adapter/recorded-guard-shape-mismatch {:guard guard}))
  (into #{}
        (mapcat (fn [clause]
                  (when-not (set? (:present clause))
                    (refuse! :adapter/recorded-guard-shape-mismatch {:clause clause}))
                  (map #(runtime-token target %) (:present clause))))
        (:clauses guard)))

(defn input-from-run-record
  "Project the uniquely selected production candidate into the strict Lean
   adapter input. Selection identity comes from :decision/:chosen; it is not
   guessed from candidate order. The projection reverses the recorder's
   [target token] wrapper and otherwise preserves runtime values exactly."
  [run-record]
  (let [decision (:decision run-record)
        chosen (:chosen decision)
        target (:target chosen)
        candidate-id (:candidate chosen)]
    (when-not (and (map? decision) (map? chosen) (string? target) candidate-id)
      (refuse! :adapter/selected-candidate-identity-missing {}))
    (let [candidates (get-in decision [:selection-certificate :candidates])
          _ (when-not (vector? candidates)
              (refuse! :adapter/candidate-set-missing {}))
          candidate (one! :adapter/selected-candidate-ambiguous
                          (filter (fn [entry]
                                    (let [id (:id entry)]
                                      (and (= target (:target id))
                                           (= candidate-id (:id id)))))
                                  candidates)
                          {:target target :candidate candidate-id})
          recorded (:id candidate)
          units (:precedence recorded)
          order (mapv :id units)]
      (when-not (and (= :cascade-candidate (:kind recorded))
                     (seq units) (= (count units) (count (distinct order)))
                     (= :machine-constructed (get-in recorded [:construction-receipt :kind])))
        (refuse! :adapter/recorded-candidate-shape-mismatch
                 {:target target :candidate candidate-id}))
      {:schema schema
       :precedence order
       :interpretations
       (into {}
             (map (fn [unit]
                    (when-not (= target (:target unit))
                      (refuse! :adapter/mixed-candidate-target
                               {:target target :unit (:id unit)
                                :unit-target (:target unit)}))
                    [(:id unit)
                     {:produces (into #{} (map #(runtime-token target %)) (:produces unit))
                      :guard {:needs (recorded-needs target (:guard unit))}}]))
             units)
       :construction-receipt (:construction-receipt recorded)
       :runtime-source {:run-id (:run/id run-record)
                        :click-id (:click/id run-record)
                        :target target
                        :candidate candidate-id}})))

(defn- strict-edn [bytes]
  (try
    (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                    (.onMalformedInput CodingErrorAction/REPORT)
                    (.onUnmappableCharacter CodingErrorAction/REPORT))
          reader (PushbackReader. (StringReader. (str (.decode decoder (ByteBuffer/wrap bytes)))))
          eof (Object.)
          value (edn/read {:eof eof} reader)
          trailing (edn/read {:eof eof} reader)]
      (when (identical? eof value) (refuse! :adapter/empty-input {}))
      (when-not (identical? eof trailing) (refuse! :adapter/trailing-form {}))
      value)
    (catch clojure.lang.ExceptionInfo failure (throw failure))
    (catch Throwable failure
      (refuse! :adapter/malformed-edn {:cause (.getMessage failure)}))))

(defn- finset [xs ids]
  (if (seq xs)
    (str "{" (str/join ", " (map ids (sort-by pr-str xs))) "}")
    "∅"))

(defn- edge [row unit-ids token-ids]
  (when-not (and (map? row) (set? (:tokens row))
                 (contains? unit-ids (:from row)) (contains? unit-ids (:to row)))
    (refuse! :adapter/malformed-edge {:edge row}))
  (format "⟨%d, %d, %s⟩" (unit-ids (:from row)) (unit-ids (:to row))
          (finset (:tokens row) token-ids)))

(defn- lean-list [render xs]
  (str "[" (str/join ", " (map render xs)) "]"))

(defn- meet [row unit-ids token-ids]
  (let [[left right :as pair] (:pair row)
        evidence (:evidence row)]
    (when-not (and (= 2 (count pair)) (contains? unit-ids left)
                   (contains? unit-ids right) (contains? unit-ids (:meet row))
                   (map? evidence) (vector? (:left-path evidence))
                   (vector? (:right-path evidence)))
      (refuse! :adapter/malformed-meet {:meet row}))
    (format "⟨%d, %d, %d, %s, %s⟩"
            (unit-ids left) (unit-ids right) (unit-ids (:meet row))
            (lean-list #(edge % unit-ids token-ids) (:left-path evidence))
            (lean-list #(edge % unit-ids token-ids) (:right-path evidence)))))

(defn render
  "Render a decoded, already pinned input as a standalone Lean witness."
  [input source-sha]
  (when-not (= schema (:schema input))
    (refuse! :adapter/schema-mismatch {:actual (:schema input)}))
  (let [order (:precedence input)
        interpretations (:interpretations input)
        receipt (:construction-receipt input)]
    (when-not (and (vector? order) (seq order) (= (count order) (count (distinct order)))
                   (= (set order) (set (keys interpretations))) (map? receipt))
      (refuse! :adapter/domain-mismatch {}))
    (let [unit-ids (zipmap order (range))
          tokens (set (mapcat (fn [[_ interpretation]]
                                (concat (:produces interpretation)
                                        (get-in interpretation [:guard :needs])))
                              interpretations))
          token-order (vec (sort-by pr-str tokens))
          token-ids (zipmap token-order (range))
          relations (:relations receipt)
          support (get-in relations [:support :relations])
          meets (get-in relations [:meet :relations])
          precedence (get-in relations [:precedence :relations])
          extension (get-in relations [:precedence :linear-extension])
          violations (get-in relations [:precedence :violations])]
      (when-not (and (= :machine-constructed (:kind receipt))
                     (= :computed (:status relations))
                     (every? vector? [support meets precedence extension violations]))
        (refuse! :adapter/receipt-shape-mismatch {}))
      (when-not (every? #(contains? token-ids %) (mapcat :tokens (concat support precedence)))
        (refuse! :adapter/token-outside-interpretation {}))
      (let [semantics
            (str/join "\n"
                      (for [[unit id] unit-ids
                            :let [interpretation (interpretations unit)]]
                        (format "  | %d => ⟨%s, %s⟩" id
                                (finset (:produces interpretation) token-ids)
                                (finset (get-in interpretation [:guard :needs]) token-ids))))
            render-edge #(edge % unit-ids token-ids)
            render-meet #(meet % unit-ids token-ids)
            order-lean (lean-list unit-ids order)
            extension-lean (lean-list unit-ids extension)
            violations-lean
            (lean-list (fn [[a b]] (format "(%d, %d)" (unit-ids a) (unit-ids b))) violations)]
        (str "import DarkTower.WarMachine.ConstructionReceipt\n\n"
             "/-! GENERATED FILE — DO NOT EDIT.\n"
             "Source SHA-256: " source-sha "\n"
             (when-let [runtime-source (:runtime-source input)]
               (str "Runtime source: " (pr-str runtime-source) "\n"))
             "Generator: futon2.aif.construction-receipt-lean-adapter\n"
             "Unit identity map: " (pr-str unit-ids) "\n"
             "Token identity map: " (pr-str token-ids) "\n-/\n\n"
             "namespace DarkTower.WarMachine.ConstructionReceipt.RuntimeWitness\n"
             "open DarkTower.WarMachine.ConstructionReceipt\n\n"
             "def runtimeSemantics : Nat → UnitSemantics\n" semantics "\n"
             "  | _ => ⟨∅, ∅⟩\n\n"
             "def runtimeReceipt : Receipt :=\n"
             "  { order := " order-lean "\n"
             "    support := " (lean-list render-edge support) "\n"
             "    meets := " (lean-list render-meet meets) "\n"
             "    precedence := " (lean-list render-edge precedence) "\n"
             "    linearExtension := " extension-lean "\n"
             "    precedenceViolations := " violations-lean " }\n\n"
             "theorem decoded_runtime_receipt_valid :\n"
             "    valid runtimeSemantics runtimeReceipt = true := by\n"
             "  native_decide\n\n"
             "#print axioms decoded_runtime_receipt_valid\n"
             "end DarkTower.WarMachine.ConstructionReceipt.RuntimeWitness\n")))))

(defn adapt-bytes
  "Verify EXPECTED-SHA against BYTES and render the exact decoded value."
  [bytes expected-sha]
  (let [actual (sha256 bytes)]
    (when-not (= expected-sha actual)
      (refuse! :adapter/source-pin-mismatch {:expected expected-sha :actual actual}))
    {:source-sha256 actual
     :lean (render (strict-edn bytes) actual)}))

(defn adapt-file [path expected-sha]
  (adapt-bytes (java.nio.file.Files/readAllBytes (.toPath (io/file path))) expected-sha))

(defn adapt-run-record-bytes
  "Pin a complete production run record, select its enacted candidate, and
   render the projected construction receipt. Returns the projection so the
   normalization boundary remains inspectable."
  [bytes expected-sha]
  (let [actual (sha256 bytes)]
    (when-not (= expected-sha actual)
      (refuse! :adapter/source-pin-mismatch {:expected expected-sha :actual actual}))
    (let [projection (input-from-run-record (strict-edn bytes))]
      {:source-sha256 actual
       :projection projection
       :lean (render projection actual)})))

(defn adapt-run-record-file [path expected-sha]
  (adapt-run-record-bytes
   (java.nio.file.Files/readAllBytes (.toPath (io/file path))) expected-sha))

(defn- selected-scoring-entry [run-record]
  (let [chosen (get-in run-record [:decision :chosen])
        target (:target chosen)
        candidate-id (:candidate chosen)
        scoring (get-in run-record [:decision :selection-certificate :scoring])]
    (when-not (map? scoring)
      (refuse! :adapter/g-scoring-missing {}))
    (one! :adapter/g-selected-candidate-ambiguous
          (filter (fn [entry]
                    (let [id (:id entry)]
                      (and (= :cascade-candidate (:kind id))
                           (= target (:target id))
                           (= candidate-id (:id id)))))
                  (vals scoring))
          {:target target :candidate candidate-id})))

(defn g-input-from-run-record
  "Select the enacted candidate's retained JVM-double G decomposition. This
   checks the runtime combination law without claiming A/Q/C correspondence."
  [run-record]
  (let [chosen (get-in run-record [:decision :chosen])
        entry (selected-scoring-entry run-record)
        terms (:g-terms entry)
        values [(:risk terms) (:ambiguity terms)
                (:expected-information-gain terms) (:g entry)]]
    (when-not (and (= :wm/bounded-observation-score-v1 (:schema entry))
                   (= :risk-plus-ambiguity-minus-information-gain (:combination terms))
                   (= :nats (:units terms))
                   (every? #(and (number? %) (Double/isFinite (double %))) values))
      (refuse! :adapter/g-certificate-shape-mismatch {}))
    {:runtime-source {:run-id (:run/id run-record)
                      :click-id (:click/id run-record)
                      :target (:target chosen)
                      :candidate (:candidate chosen)}
     :risk (double (:risk terms))
     :ambiguity (double (:ambiguity terms))
     :information (double (:expected-information-gain terms))
     :recorded-g (double (:g entry))}))

(defn- float-bits [value]
  (Long/toUnsignedString (Double/doubleToRawLongBits value)))

(defn render-g
  "Render exact IEEE-754 inputs and the named runtime G combination check."
  [input source-sha]
  (let [{:keys [risk ambiguity information recorded-g runtime-source]} input]
    (str "import Mathlib\n\n"
         "/-! GENERATED FILE — DO NOT EDIT.\n"
         "Source SHA-256: " source-sha "\n"
         "Runtime source: " (pr-str runtime-source) "\n"
         "Scope: retained JVM-double combination law only; not A/Q/C correspondence.\n-/\n\n"
         "namespace DarkTower.WarMachine.RuntimeG\n\n"
         "def risk : Float := Float.ofBits " (float-bits risk) "\n"
         "def ambiguity : Float := Float.ofBits " (float-bits ambiguity) "\n"
         "def information : Float := Float.ofBits " (float-bits information) "\n"
         "def recordedG : Float := Float.ofBits " (float-bits recorded-g) "\n\n"
         "theorem recorded_g_matches_risk_plus_ambiguity_minus_information :\n"
         "    ((risk + ambiguity - information) == recordedG) = true := by\n"
         "  native_decide\n\n"
         "#print axioms recorded_g_matches_risk_plus_ambiguity_minus_information\n"
         "end DarkTower.WarMachine.RuntimeG\n")))

(defn adapt-run-record-g-bytes [bytes expected-sha]
  (let [actual (sha256 bytes)]
    (when-not (= expected-sha actual)
      (refuse! :adapter/source-pin-mismatch {:expected expected-sha :actual actual}))
    (let [projection (g-input-from-run-record (strict-edn bytes))]
      {:source-sha256 actual
       :projection projection
       :lean (render-g projection actual)})))

(defn aqc-input-from-run-record
  "Project the selected policy's repaired, target-local A/Q/C census. The
  retained scoring fields are classified but never rescored."
  [run-record]
  (let [chosen (get-in run-record [:decision :chosen])
        selected-action (get-in run-record
                                [:decision :selection-law :per-policy-argmax :action])
        entry (selected-scoring-entry run-record)
        selected (:id entry)
        candidates (get-in run-record [:decision :selection-certificate :candidates])
        candidate (one! :adapter/aqc-candidate-identity-mismatch
                        (filter #(= selected (:id %)) candidates)
                        {:selected selected})
        census (decomposition/census
                [{:action selected :controller-score (:g entry) :certificate entry}]
                [candidate])
        policy (first (:policies census))
        terms (:terms policy)
        a (:A terms) q (:Q terms) c (:C terms)
        target (:target selected)
        c-steps (get-in c [:value :steps])
        final-c (:distribution (last c-steps))
        initial (get-in q [:value :initial-belief])
        q-steps (get-in q [:value :steps])
        acceptance (get-in a [:value :acceptance])
        positive-states (fn [belief]
                          (map key (filter (comp pos? val) belief)))
        none-accepted? (fn [belief]
                         (every? #(empty? (set/intersection acceptance %))
                                 (positive-states belief)))
        all-accepted? (fn [belief]
                        (every? #(set/subset? acceptance %)
                                (positive-states belief)))
        chosen-matches? (and (= (:kind selected-action) (:kind selected))
                             (= (:id selected-action) (:id chosen) (:candidate chosen)
                                (:id selected))
                             (= (:target selected-action) (:target chosen) (:target selected))
                             (= (mapv :id (:precedence selected-action)) (:precedence chosen))
                             (= (get-in selected-action [:construction-receipt :kind])
                                (:construction-kind chosen)))]
    (when-not (and chosen-matches? (= selected-action selected)
                   (= selected (:id candidate)) (= selected (:id policy)))
      (refuse! :adapter/aqc-candidate-identity-mismatch
               {:selection-law selected-action :chosen chosen
                :scoring selected :candidate (:id candidate) :policy (:id policy)}))
    (when-not (and (= :present (:status a)) (= :present (:status q))
                   (= :present (:status c)) (= {target :related}
                                                (get-in a [:value :target-class]))
                   (set? acceptance) (seq acceptance)
                   (every? #(and (vector? %) (= target (first %))) acceptance)
                   (= (count q-steps) (count c-steps))
                   (seq q-steps) (none-accepted? initial)
                   (all-accepted? (:belief (last q-steps)))
                   (= 1 (reduce + (vals final-c)))
                   (pos? (get final-c :related 0)))
      (refuse! :adapter/aqc-correspondence-mismatch
               {:target target :A (select-keys a [:status :reason])
                :Q (select-keys q [:status :reason])
                :C (select-keys c [:status :reason])}))
    {:runtime-source {:run-id (:run/id run-record)
                      :click-id (:click/id run-record)
                      :candidate (select-keys selected [:kind :id :target])}
     :horizon (count q-steps)
     :target-class :related
     :q-initial-target false
     :q-terminal-target true
     :terminal-c final-c}))

(defn- lean-ratio [x]
  (cond
    (ratio? x) (str "(" (numerator x) " / " (denominator x) " : ℝ)")
    (integer? x) (str "(" x " : ℝ)")
    :else (refuse! :adapter/aqc-non-exact-preference {:value x})))

(defn render-aqc [input source-sha]
  (let [c (:terminal-c input)]
    (str "import DarkTower.WarMachine.CascadeEFE\n\n"
         "/-! GENERATED FILE — DO NOT EDIT.\nSource SHA-256: " source-sha
         "\nRuntime source: " (pr-str (:runtime-source input))
         "\nScope: selected-policy target-local retained A/Q/C only; F and whole-census completeness excluded.\n-/\n\n"
         "namespace DarkTower.WarMachine.RuntimeAQC\n"
         "open Holes CascadeEFE\nopen scoped BigOperators\nnoncomputable section\n\n"
         "inductive Outcome | focused | related | unrelated | stopTheLine | notYet\n"
         "  deriving DecidableEq, Fintype\n\n"
         "def cFocused : ℝ := " (lean-ratio (:focused c)) "\n"
         "def cRelated : ℝ := " (lean-ratio (:related c)) "\n"
         "def cUnrelated : ℝ := " (lean-ratio (:unrelated c)) "\n"
         "def cStopTheLine : ℝ := " (lean-ratio (:stop-the-line c)) "\n"
         "def cNotYet : ℝ := " (lean-ratio (:ending/not-yet-evaluated c 0)) "\n"
         "def retainedAQ : ProbabilityKernel Unit Outcome := point .related\n\n"
         "def retainedAClass : Outcome := .related\n"
         "def qInitialHasTarget : Bool := false\n"
         "def qTerminalHasTarget : Bool := true\n\n"
         "theorem retained_A_is_target_local : retainedAClass = .related := rfl\n"
         "theorem retained_Q_reaches_target : qInitialHasTarget = false ∧ qTerminalHasTarget = true := by decide\n"
         "theorem retained_A_Q_kernel_normalised : (∑ o, retainedAQ.mass () o) = 1 := kernel_sum _ _\n"
         "theorem retained_C_is_normalised : cFocused + cRelated + cUnrelated + cStopTheLine + cNotYet = 1 := by\n"
         "  norm_num [cFocused, cRelated, cUnrelated, cStopTheLine, cNotYet]\n"
         "theorem retained_target_has_positive_preference : 0 < cRelated := by\n"
         "  norm_num [cRelated]\n\n"
         "#print axioms retained_A_is_target_local\n#print axioms retained_Q_reaches_target\n"
         "#print axioms retained_A_Q_kernel_normalised\n#print axioms retained_C_is_normalised\n"
         "#print axioms retained_target_has_positive_preference\n"
         "end\nend DarkTower.WarMachine.RuntimeAQC\n")))

(defn adapt-run-record-aqc-bytes [bytes expected-sha]
  (let [actual (sha256 bytes)]
    (when-not (= expected-sha actual)
      (refuse! :adapter/source-pin-mismatch {:expected expected-sha :actual actual}))
    (let [projection (aqc-input-from-run-record (strict-edn bytes))]
      {:source-sha256 actual :projection projection
       :lean (render-aqc projection actual)})))

(defn selection-input-from-run-record
  "Project the complete recorded policy field and its no-competing-policy
  comparison branch. This validates retained comparison data; it does not
  rescore candidates or treat absent F as zero."
  [run-record]
  (let [decision (:decision run-record)
        chosen (:chosen decision)
        action (get-in decision [:selection-law :per-policy-argmax :action])
        comparison (get-in decision [:selection-law :policy-comparison])
        candidates (get-in decision [:selection-certificate :candidates])
        scoring (get-in decision [:selection-certificate :scoring])
        candidate-ids (mapv :id candidates)
        scoring-ids (mapv :id (vals scoring))
        selected (:id (first (vals scoring)))
        candidate (first candidates)
        winner (:winner comparison)
        chosen-matches? (and (= (:kind action) (:kind selected))
                             (= (:id action) (:id chosen) (:candidate chosen)
                                (:id selected))
                             (= (:target action) (:target chosen) (:target selected))
                             (= (mapv :id (:precedence action)) (:precedence chosen))
                             (= (get-in action [:construction-receipt :kind])
                                (:construction-kind chosen)))]
    (when-not (and (vector? candidates) (map? scoring) (= 1 (count candidates))
                   (= 1 (count scoring)) (= (count candidate-ids)
                                            (count (distinct candidate-ids)))
                   (= (set candidate-ids) (set scoring-ids)))
      (refuse! :adapter/selection-candidate-coverage-mismatch
               {:candidate-ids candidate-ids :scoring-ids scoring-ids}))
    (when-not (and chosen-matches? (= action selected) (= selected (:id candidate))
                   (= selected (:id winner)))
      (refuse! :adapter/selection-candidate-identity-mismatch
               {:selection-law action :chosen chosen :scoring selected
                :candidate (:id candidate) :winner (:id winner)}))
    (when-not (and (= :no-competing-policy (:status comparison))
                   (= :acting-policy (:comparison-domain comparison))
                   (= :unrestricted (:selection-domain comparison))
                   (= :no-competing-policy (:runner-up comparison))
                   (= :no-competing-policy (:decided-by comparison))
                   (nil? (:contributions comparison))
                   (= :policy-printed-identity-ascending (:tie-break-rule comparison))
                   (= [:habit :free-energy :G] (:contribution-tie-order comparison))
                   (= {:status :undeclared} (:near-tie-threshold comparison))
                   (= :threshold-undeclared (:near-tie? comparison))
                   (= 1.0 (:posterior winner))
                   (= 1.0 (:habit winner) (:habit candidate))
                   (nil? (:f winner)) (nil? (:f candidate))
                   (= :not-supplied (:f-status winner) (:f-status candidate))
                   (number? (:g candidate)) (= (:g candidate) (:g (first (vals scoring)))))
      (refuse! :adapter/selection-law-mismatch
               {:status (:status comparison)
                :contribution-order (:contribution-tie-order comparison)}))
    {:runtime-source {:run-id (:run/id run-record)
                      :click-id (:click/id run-record)
                      :candidate (select-keys selected [:kind :id :target])}
     :candidate-count 1
     :winner-index 0
     :habit (:habit candidate)
     :g (:g candidate)
     :f-status :not-supplied
     :comparison-status :no-competing-policy
     :contributions :absent
     :contribution-order [:habit :free-energy :G]}))

(defn render-selection [input source-sha]
  (let [{:keys [runtime-source habit g]} input]
    (str "import Mathlib\n\n"
         "/-! GENERATED FILE — DO NOT EDIT.\nSource SHA-256: " source-sha
         "\nRuntime source: " (pr-str runtime-source)
         "\nScope: complete recorded policy field; singleton no-competing-policy branch. F remains absent.\n-/\n\n"
         "namespace DarkTower.WarMachine.RuntimeSelection\n\n"
         "inductive Contribution | habit | freeEnergy | G deriving DecidableEq\n\n"
         "structure Policy where\n  id : Nat\n  habit : Float\n  freeEnergy : Option Float\n  G : Float\n\n"
         "def selected : Policy := ⟨0, Float.ofBits " (float-bits habit)
         ", none, Float.ofBits " (float-bits g) "⟩\n"
         "def recordedCandidates : List Policy := [selected]\n"
         "def recordedContributionOrder : List Contribution := "
         "[.habit, .freeEnergy, .G]\n"
         "def recordedContributions : Option (Float × Float × Float) := none\n"
         "def chooseNoCompeting : List Policy → Option Policy\n"
         "  | [policy] => some policy\n  | _ => none\n\n"
         "theorem selected_is_recorded_no_competing_winner :\n"
         "    chooseNoCompeting recordedCandidates = some selected := rfl\n"
         "theorem retained_contribution_order :\n"
         "    recordedContributionOrder = [.habit, .freeEnergy, .G] := rfl\n"
         "theorem retained_F_and_pairwise_contributions_are_absent :\n"
         "    selected.freeEnergy = none ∧ recordedContributions = none := ⟨rfl, rfl⟩\n\n"
         "#print axioms selected_is_recorded_no_competing_winner\n"
         "#print axioms retained_contribution_order\n"
         "#print axioms retained_F_and_pairwise_contributions_are_absent\n"
         "end DarkTower.WarMachine.RuntimeSelection\n")))

(defn adapt-run-record-selection-bytes [bytes expected-sha]
  (let [actual (sha256 bytes)]
    (when-not (= expected-sha actual)
      (refuse! :adapter/source-pin-mismatch {:expected expected-sha :actual actual}))
    (let [projection (selection-input-from-run-record (strict-edn bytes))]
      {:source-sha256 actual :projection projection
       :lean (render-selection projection actual)})))

(defn admission-input-from-run-record
  "Project the selected candidate's recorded P0 provenance admission. Missing
  P0 evidence stays excluded; only the canonical forbidden-provenance check
  and exact admitted/scored field correspondence are certified."
  [run-record]
  (let [selection (selection-input-from-run-record run-record)
        selected-action (get-in run-record
                                [:decision :selection-law :per-policy-argmax :action])
        selected-id (:id selected-action)
        candidates (get-in run-record [:decision :selection-certificate :candidates])
        derivations (get-in run-record
                            [:decision :selection-certificate :candidate-derivations])
        derivation (get derivations selected-id)
        payload-sha (equivalence/canonical-sha256 selected-action)
        required-fields (conj (set candidate-derivations/p0-fields)
                              :candidate-payload-sha256 :payload-canonicalisation :status)]
    (when-not (and (map? derivations) (= #{selected-id} (set (keys derivations)))
                   (= 1 (count candidates))
                   (= selected-action (:id (first candidates))))
      (refuse! :adapter/admission-field-identity-mismatch
               {:selected selected-id :derivation-ids (set (keys derivations))}))
    (when-not (set/subset? required-fields (set (keys derivation)))
      (refuse! :adapter/admission-check-coverage-mismatch
               {:missing (set/difference required-fields (set (keys derivation)))}))
    (when-not (and (= :admitted (:status derivation))
                   (= {:kind :declared-file-load :admitted-by :war-machine-judge}
                      (:admission derivation))
                   (= {:admissible true} (equivalence/admissible-provenance? derivation))
                   (= payload-sha (:candidate-payload-sha256 derivation))
                   (= {:form :cert-s-v1-canonical-edn
                       :note "same candidate iff [:id :id] and :candidate-payload-sha256 agree"}
                      (:payload-canonicalisation derivation)))
      (refuse! :adapter/admission-check-failed
               {:status (:status derivation)
                :verdict (equivalence/admissible-provenance? derivation)}))
    {:runtime-source (:runtime-source selection)
     :candidate-payload-sha256 payload-sha
     :admitted-count 1
     :selected-count 1
     :provenance-checks [:interpretation :construction :review-publication :admission]
     :typed-exclusions (select-keys derivation
                                    [:source-revision :source-content-sha256
                                     :discovered-at :review-publication
                                     :acceptance :scope])}))

(defn render-admission [input source-sha]
  (let [digest (:candidate-payload-sha256 input)]
    (str "import DarkTower.WarMachine.Requirements\n\n"
         "/-! GENERATED FILE — DO NOT EDIT.\nSource SHA-256: " source-sha
         "\nRuntime source: " (pr-str (:runtime-source input))
         "\nCandidate payload SHA-256: " digest
         "\nScope: recorded P0 forbidden-provenance admission only; typed missing checks are excluded.\n-/\n\n"
         "namespace DarkTower.WarMachine.RuntimeAdmission\n"
         "open DarkTower.WarMachine.Requirements\n\n"
         "def admittedTargets : Finset Id := {0}\n"
         "def targetsReachingScoring : Finset Id := {0}\n"
         "def selectedTarget : Id := 0\n"
         "def selectedCandidateDigest : String := \"" digest "\"\n"
         "def admittedCandidateDigest : String := \"" digest "\"\n\n"
         "theorem selected_full_identity_is_admitted :\n"
         "    selectedCandidateDigest = admittedCandidateDigest := rfl\n"
         "theorem selected_target_reached_scoring :\n"
         "    selectedTarget ∈ admittedTargets := by decide\n"
         "theorem selection_field_is_exactly_admitted_field :\n"
         "    targetsReachingScoring = admittedTargets := rfl\n\n"
         "#print axioms selected_full_identity_is_admitted\n"
         "#print axioms selected_target_reached_scoring\n"
         "#print axioms selection_field_is_exactly_admitted_field\n"
         "end DarkTower.WarMachine.RuntimeAdmission\n")))

(defn adapt-run-record-admission-bytes [bytes expected-sha]
  (let [actual (sha256 bytes)]
    (when-not (= expected-sha actual)
      (refuse! :adapter/source-pin-mismatch {:expected expected-sha :actual actual}))
    (let [projection (admission-input-from-run-record (strict-edn bytes))]
      {:source-sha256 actual :projection projection
       :lean (render-admission projection actual)})))

(defn enactment-grounding-input-from-run-record
  "Project only the admitted selected/enacted identity and grounded runtime
  receipt. Refused precision and token-observation subreceipts remain explicit
  non-consumed exclusions."
  [run-record]
  (let [action (get-in run-record [:decision :selection-law :per-policy-argmax :action])
        chosen (get-in run-record [:decision :chosen])
        identity (select-keys action [:kind :id :target])
        d-task (:d-task-enactment run-record)
        verification (:verification d-task)
        bridge (:candidate-to-minted-join verification)
        scope (:scope verification)
        grounded (:grounded-commit run-record)
        terminal (:terminal-receipt run-record)
        roles (get-in run-record [:participants :roles])
        reviewer (get roles :reviewer-of-record)
        configured-reviewer (get roles :configured-reviewer)
        action-sha (action-identity/digest action)]
    (when-not (and (= (:id action) (:id chosen) (:candidate chosen))
                   (= (:target action) (:target chosen))
                   (= identity (:identity bridge))
                   (= :wm/selected-enacted-action-correspondence-v1 (:schema bridge))
                   (= :verified (:status bridge))
                   (= action-sha (:selected-action-sha256 bridge)
                                 (:enacted-action-sha256 bridge)))
      (refuse! :adapter/enactment-identity-mismatch
               {:selected identity :chosen chosen :bridge bridge}))
    (when-not (and (= :admitted (:status verification))
                   (= :d-predecessor-task-authority-v1 (:authority verification)
                      (:authority d-task))
                   (= scope (:scope d-task))
                   (= :executed-with-artifacts (:certifies scope))
                   (= #{:e1-portfolio-membership :r6-r11-domain}
                      (:does-not-establish scope)))
      (refuse! :adapter/enactment-admission-mismatch
               {:status (:status verification) :scope scope}))
    (when-not (and (= {:status :refused
                       :kind :precision-selected-declaration-unestablished}
                      (:precision-verification verification))
                   (= {:status :refused :kind :after-token-evidence-unavailable}
                      (:token-observation-verification verification)))
      (refuse! :adapter/refused-subreceipt-mismatch
               {:precision (:precision-verification verification)
                :token-observation (:token-observation-verification verification)}))
    (when-not (and (map? grounded)
                   (re-matches #"[0-9a-f]{40}" (:sha grounded ""))
                   (string? (:repo grounded)) (not (str/blank? (:repo grounded)))
                   (= grounded (:commit terminal))
                   (= (:sha grounded) (get-in verification [:revision-pair :after])))
      (refuse! :adapter/grounded-commit-mismatch
               {:grounded grounded :terminal (:commit terminal)
                :revision (get-in verification [:revision-pair :after])}))
    (when-not (and (= :present (:status reviewer) (:status configured-reviewer))
                   (= (:identity reviewer) (:identity configured-reviewer)
                      (:reviewer terminal)))
      (refuse! :adapter/reviewer-mismatch
               {:reviewer reviewer :configured configured-reviewer
                :terminal (:reviewer terminal)}))
    (when-not (and (= {:absent :no-failure} (:failure run-record))
                   (= terminal (terminal-receipt/validate-receipt terminal))
                   (= (:terminal-receipt-digest run-record)
                      (terminal-receipt/terminal-receipt-digest terminal))
                   (= :action-receipt (:kind terminal))
                   (= :grounded-progress (:outcome terminal))
                   (= (:kind action) (:action-kind terminal))
                   (= (:target action) (:target terminal))
                   (= action (get-in terminal [:G :candidate])))
      (refuse! :adapter/terminal-receipt-mismatch
               {:failure (:failure run-record) :terminal terminal}))
    {:runtime-source {:run-id (:run/id run-record)
                      :click-id (:click/id run-record)
                      :candidate identity}
     :selected-action-sha256 action-sha
     :enacted-action-sha256 (:enacted-action-sha256 bridge)
     :commit (:sha grounded) :repository (:repo grounded)
     :reviewer (:identity reviewer)
     :outcome :grounded-progress
     :precision-status :refused :token-observation-status :refused
     :precision-consumed false :token-observation-consumed false}))

(defn render-enactment-grounding [input source-sha]
  (let [{:keys [selected-action-sha256 enacted-action-sha256 commit repository reviewer]}
        input]
    (str "import DarkTower.WarMachine.CertificateStates\n\n"
         "/-! GENERATED FILE — DO NOT EDIT.\nSource SHA-256: " source-sha
         "\nRuntime source: " (pr-str (:runtime-source input))
         "\nScope: admitted execution/grounding correspondence only; refused precision and token observations are not consumed.\n-/\n\n"
         "namespace DarkTower.WarMachine.RuntimeEnactmentGrounding\n"
         "open DarkTower.WarMachine.CertificateStates\n\n"
         "def selectedDigest : String := \"" selected-action-sha256 "\"\n"
         "def enactedDigest : String := \"" enacted-action-sha256 "\"\n"
         "def selectionEnaction : SelectionEnaction := .match selectedDigest enactedDigest\n"
         "def groundedCommit : String := \"" commit "\"\n"
         "def terminalCommit : String := \"" commit "\"\n"
         "def groundedRepository : String := \"" repository "\"\n"
         "def terminalRepository : String := \"" repository "\"\n"
         "def reviewerOfRecord : String := \"" reviewer "\"\n"
         "def terminalReviewer : String := \"" reviewer "\"\n"
         "def executionAdmitted : Bool := true\n"
         "def executedWithArtifacts : Bool := true\n"
         "def terminalReceiptCount : Nat := 1\n"
         "def failurePresent : Bool := false\n"
         "def groundedProgress : Bool := true\n"
         "def precisionRefused : Bool := true\n"
         "def tokenObservationRefused : Bool := true\n"
         "def precisionConsumed : Bool := false\n"
         "def tokenObservationConsumed : Bool := false\n\n"
         "theorem selected_enacted_exact : selectionEnaction = .match selectedDigest selectedDigest := rfl\n"
         "theorem grounded_artifact_consistent : groundedCommit = terminalCommit ∧ groundedRepository = terminalRepository := ⟨rfl, rfl⟩\n"
         "theorem reviewer_consistent : reviewerOfRecord = terminalReviewer := rfl\n"
         "theorem admitted_grounded_unique_terminal_no_failure : executionAdmitted = true ∧ executedWithArtifacts = true ∧ groundedProgress = true ∧ terminalReceiptCount = 1 ∧ failurePresent = false := ⟨rfl, rfl, rfl, rfl, rfl⟩\n"
         "theorem refused_subreceipts_not_consumed : precisionRefused = true ∧ tokenObservationRefused = true ∧ precisionConsumed = false ∧ tokenObservationConsumed = false := ⟨rfl, rfl, rfl, rfl⟩\n\n"
         "#print axioms selected_enacted_exact\n"
         "#print axioms grounded_artifact_consistent\n"
         "#print axioms reviewer_consistent\n"
         "#print axioms admitted_grounded_unique_terminal_no_failure\n"
         "#print axioms refused_subreceipts_not_consumed\n"
         "end DarkTower.WarMachine.RuntimeEnactmentGrounding\n")))

(defn adapt-run-record-enactment-grounding-bytes [bytes expected-sha]
  (let [actual (sha256 bytes)]
    (when-not (= expected-sha actual)
      (refuse! :adapter/source-pin-mismatch {:expected expected-sha :actual actual}))
    (let [projection (enactment-grounding-input-from-run-record (strict-edn bytes))]
      {:source-sha256 actual :projection projection
       :lean (render-enactment-grounding projection actual)})))

(defn -main [& [input expected-sha output :as args]]
  (when-not (= 3 (count args))
    (binding [*out* *err*]
      (println "usage: clojure -M -m futon2.aif.construction-receipt-lean-adapter INPUT SHA256 OUTPUT"))
    (System/exit 2))
  (let [{:keys [lean source-sha256]} (adapt-file input expected-sha)]
    (spit output lean)
    (println (pr-str {:status :written :output output :source-sha256 source-sha256}))))
