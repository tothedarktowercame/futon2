(ns futon2.aif.construction-receipt-lean-adapter
  "Identity-preserving adapter from a pinned runtime construction receipt to
  DarkTower.WarMachine.ConstructionReceipt.  The generated theorem evaluates
  the decoded value; source occurrence is not treated as correspondence."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
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

(defn -main [& [input expected-sha output :as args]]
  (when-not (= 3 (count args))
    (binding [*out* *err*]
      (println "usage: clojure -M -m futon2.aif.construction-receipt-lean-adapter INPUT SHA256 OUTPUT"))
    (System/exit 2))
  (let [{:keys [lean source-sha256]} (adapt-file input expected-sha)]
    (spit output lean)
    (println (pr-str {:status :written :output output :source-sha256 source-sha256}))))
