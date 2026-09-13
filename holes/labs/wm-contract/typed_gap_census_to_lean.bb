#!/usr/bin/env bb
(require '[babashka.fs :as fs] '[clojure.edn :as edn] '[clojure.string :as str])

(def families [:fullLoopCheckpoints :wmTraceRecords :tickRunRecords :closeCohortRecords
               :dispatchJobRecords :parkContinuationRecords :reviewAdmissionRecords])
(defn refuse [k x] (throw (ex-info (name k) {:refusal k :data x})))
(defn exact-keys! [m ks]
  (when-not (and (map? m) (= (set (keys m)) (set ks))) (refuse :keys ks)))
(defn text! [s]
  (when-not (and (string? s) (not (str/blank? s))
                 (not-any? #(Character/isSurrogate %) s)
                 (every? #(or (>= (int %) 32) (contains? #{\newline \return \tab} %)) s))
    (refuse :invalid-text s))
  s)
(defn q [s]
  (str "\"" (str/escape (text! s)
                         {\\ "\\\\" \" "\\\"" \newline "\\n" \return "\\r" \tab "\\t"}) "\""))
(defn lean-list [xs] (str "[" (str/join ", " xs) "]"))
(defn node [x]
  (case (:state x)
    :unvalidated
    (do (exact-keys! x [:id :state :claim :scope])
        (format "⟨%s, .unvalidated %s %s⟩" (q (:id x)) (q (:claim x)) (q (:scope x))))
    :typed-absence
    (do (exact-keys! x [:id :state :claim :scope :reason])
        (format "⟨%s, .typedAbsence %s %s %s⟩"
                (q (:id x)) (q (:claim x)) (q (:scope x)) (q (:reason x))))
    (refuse :unsupported-node-state (:state x))))
(defn edge [x]
  (exact-keys! x [:id :state :scope])
  (when-not (= :mandatory-unfired (:state x)) (refuse :unsupported-edge-state (:state x)))
  (format "⟨%s, .mandatoryUnfired %s⟩" (q (:id x)) (q (:scope x))))
(defn family [x]
  (exact-keys! x [:family :state :reason])
  (when-not (= :typed-gap (:state x)) (refuse :unsupported-family-state (:state x)))
  (format ".typedGap .%s %s" (name (:family x)) (q (:reason x))))
(defn domain! [ids entries kind]
  (when-not (and (vector? ids) (seq ids) (vector? entries)
                 (= (count ids) (count (distinct ids))))
    (refuse :invalid-domain kind))
  (doseq [id ids] (text! id))
  (when-not (= ids (mapv :id entries)) (refuse :domain-order kind)))
(defn generate [x]
  (exact-keys! x [:schema :scope :authority/status :declared-nodes :nodes
                 :declared-connections :connections :selection :families])
  (when-not (and (= :wm/typed-gap-census-v1 (:schema x))
                 (= :isolated (:scope x)) (= :none (:authority/status x)))
    (refuse :schema-or-authority nil))
  (domain! (:declared-nodes x) (:nodes x) :nodes)
  (domain! (:declared-connections x) (:connections x) :connections)
  (when-not (and (vector? (:families x)) (= families (mapv :family (:families x))))
    (refuse :family-order nil))
  (exact-keys! (:selection x) [:state :reason])
  (when-not (= :refused-shape (get-in x [:selection :state])) (refuse :selection nil))
  (let [nodes (mapv node (:nodes x)) edges (mapv edge (:connections x))
        records (mapv family (:families x))
        gap (q (:reason (first (:families x))))]
    (str "import DarkTower.WarMachine.FullCertificateCrossLayerBinding\n"
         "namespace GeneratedTypedGap\n"
         "open DarkTower.WarMachine\n"
         "open CertificateStates FullCertificatePredicate FullCertificateCrossLayerBinding\n"
         "-- Isolated structural input; no acquisition or production authority.\n"
         "def att : FullAttestation :=\n"
         "  { declaredNodes := " (lean-list (map q (:declared-nodes x))) "\n"
         "    nodeStates := " (lean-list nodes) "\n"
         "    declaredConnections := " (lean-list (map q (:declared-connections x))) "\n"
         "    connectionStates := " (lean-list edges) "\n"
         "    selectionEnaction := .refusedShape " (q (get-in x [:selection :reason])) "\n"
         "    recordFamilies := " (lean-list records) "\n"
         "    negativeScope := [] }\n"
         "theorem census : CensusComplete att := by decide\n"
         "theorem rejects (req : FullScopeRequirements) (ev : FullScopeEvidence) :\n"
         "    ¬ FullQualifyingRun req ev att :=\n"
         "  rejects_missing_record_family att req ev .fullLoopCheckpoints " gap " (by decide)\n"
         "theorem rejects_cross {fixed events subjects expected actual x req ev rb eb} :\n"
         "    ¬ CrossLayerQualifyingRun fixed events subjects expected actual x req ev rb eb att :=\n"
         "  fun h => rejects req ev (crossLayer_implies_full h)\n"
         "#print axioms census\n#print axioms rejects\n#print axioms rejects_cross\n"
         "end GeneratedTypedGap\n")))
(defn sha256 [bs]
  (apply str (map #(format "%02x" (bit-and % 255))
                  (.digest (java.security.MessageDigest/getInstance "SHA-256") bs))))
(defn decode-input [bs expected]
  (when-not (and (string? expected) (re-matches #"[0-9a-f]{64}" expected)
                 (= expected (sha256 bs))) (refuse :pin-mismatch nil))
  (let [decoder (doto (.newDecoder java.nio.charset.StandardCharsets/UTF_8)
                  (.onMalformedInput java.nio.charset.CodingErrorAction/REPORT)
                  (.onUnmappableCharacter java.nio.charset.CodingErrorAction/REPORT))
        text (str (.decode decoder (java.nio.ByteBuffer/wrap bs)))
        eof (Object.)]
    (with-open [reader (java.io.PushbackReader. (java.io.StringReader. text))]
      (let [x (edn/read {:eof eof} reader)]
        (when (or (identical? eof x) (not (identical? eof (edn/read {:eof eof} reader))))
          (refuse :input-exhaustion nil))
        x))))
(defn main [& args]
  (when-not (= 3 (count args)) (refuse :usage "INPUT OUTPUT EXPECTED-SHA256"))
  (let [[in out expected] args
        bs (fs/read-all-bytes in)
        source (generate (decode-input bs expected))]
    (spit out source :encoding "UTF-8")
    (prn {:status :generated-only :scope :isolated :authority/status :none
          :input-sha256 expected :lean-sha256 (sha256 (.getBytes source "UTF-8"))
          :lean-checked? false :output out})))
(when (= *file* (System/getProperty "babashka.file")) (apply main *command-line-args*))
