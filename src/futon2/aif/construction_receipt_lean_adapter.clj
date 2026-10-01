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

(defn -main [& [input expected-sha output :as args]]
  (when-not (= 3 (count args))
    (binding [*out* *err*]
      (println "usage: clojure -M -m futon2.aif.construction-receipt-lean-adapter INPUT SHA256 OUTPUT"))
    (System/exit 2))
  (let [{:keys [lean source-sha256]} (adapt-file input expected-sha)]
    (spit output lean)
    (println (pr-str {:status :written :output output :source-sha256 source-sha256}))))
