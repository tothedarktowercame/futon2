(ns futon2.aif.wm08-route-a-test
  "WM-08 Route A rehearsal: re-express the F13-redo retrieval occurrence
  (interpreted-pattern-set.edn, pinned by FROZEN-CONTEXT.edn) in the
  receipted-find record shape, then run find-receipt/find end to end with the
  independent external expectations and the designation path.

  THIS IS A REHEARSAL, NOT ORDINARY-RUN EVIDENCE. The original retrieval was
  performed by other constructors; a re-expression is hermetic by
  construction (ORDINARY-RUN-SCOPE.md §4).

  Translation rule (§2): verbatim-copy-or-refuse. Interpretations, guards,
  effects, fact values and judgments are copied verbatim from the recorded
  codex-27 output. Line spans and digests are computed from the pinned bytes.
  q0 values keep :method :mission-document-assertion even though the record's
  own finding flags them — upgrading would be interpreting.

  The builder below is the zaif-fixture translation (interpretation-evidence
  test) lifted to a first-class, deterministic, digest-verified artifact:
  fixed identity (no random UUIDs), pinned-at from FROZEN-CONTEXT, and
  membership citations from the FROZEN index copy (retrieval-index.tsv),
  not the drifted live index."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.close-retention :as retention]
            [futon2.aif.find-receipt :as find]
            [futon2.aif.find-expectations :as fx]
            [futon2.aif.find-designation :as fd])
  (:import [java.nio.file Files]
           [java.time Instant]
           [java.util UUID]))

(def run-dir "holes/labs/wm-contract/runs/wm-08-external-f2-2026-09-16/")
(def redo "holes/labs/wm-contract/runs/F13-model-manifest-2026-09-15/redo/")
(def library-root "/home/joe/code/futon3/library")
(def frozen-index (.getCanonicalPath (io/file (str redo "retrieval-index.tsv"))))
;; The genesis pattern's registration row, captured at occurrence time. The
;; frozen retrieval index is the PRE-retrieval snapshot, so a pattern this
;; occurrence authored has no row in it; this patch is its membership record.
(def genesis-row (.getCanonicalPath (io/file (str redo "new-index-row.patch"))))
;; FROZEN-CONTEXT.edn pinned-at: the record's introducing commit instant.
(def pinned-at "2026-09-15T13:02:32Z")

(defn bytes-of [s] (.getBytes (str s) "UTF-8"))

(defn ex-data-of [f] (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e))))

(defn build-reexpressed
  "Deterministic verbatim translation of the F13-redo record. Mirrors
  interpretation-evidence-test/zaif-fixture with the rehearsal's own pinned
  identity and the frozen index as the membership/index source."
  []
  (let [old (edn/read-string (slurp (str redo "interpreted-pattern-set.edn")))
        genesis-patterns (set (map :id (:new-patterns old)))
        sources (atom {}) captured (atom {})
        add! (fn [path*]
               ;; :sources paths must be absolute (evidence schema
               ;; :path-not-absolute). The library paths already are; the two
               ;; in-repo captures are repo-relative, so absolutise here.
               (let [path (.getAbsolutePath (io/file path*))
                     id path bs (Files/readAllBytes (.toPath (io/file path)))
                     file (str (evidence/sha256 (bytes-of path)) ".source")]
                 (swap! sources assoc id {:id id :path path :file file
                                          :sha256 (evidence/sha256 bs)
                                          :revision "frozen-occurrence-cc231bf6"})
                 (swap! captured assoc file bs)
                 id))
        cite (fn [s] {:source (add! (:path s)) :lines (:lines s) :quote (:quote s)})
        mission-cite (cite (get-in old [:target :status-source]))
        norm (fn [x] (str/replace (str/trim x) #"\s+" " "))
        ;; Locating a clause's span used to scan every window of every length
        ;; (O(lines^2) joins, each re-normalised) — 1.26M windows over the
        ;; 1587-line frozen index — and an ABSENT needle paid that whole bill
        ;; before failing. That is what hung the suite. Normalised lines and
        ;; the joined document are built once per file; the first occurrence
        ;; maps back to the smallest line span containing it, and a needle
        ;; that is not in the bytes refuses immediately.
        doc-of (memoize
                (fn [path]
                  (let [lines (vec (str/split-lines (slurp path)))
                        sb (StringBuilder.)
                        spans (reduce (fn [acc [i l]]
                                        (let [n (norm l)]
                                          (if (str/blank? n)
                                            acc
                                            (do (when (pos? (.length sb)) (.append sb " "))
                                                (let [at (.length sb)]
                                                  (.append sb ^String n)
                                                  (conj acc [at (.length sb) i]))))))
                                      [] (map-indexed vector lines))]
                    {:lines lines :doc (.toString sb) :spans spans})))
        clause (fn [path text]
                 (let [{:keys [lines doc spans]} (doc-of path)
                       needle (norm text)
                       at (.indexOf ^String doc ^String needle)
                       _ (when (neg? at)
                           (throw (ex-info "Record clause not found in captured bytes"
                                           {:reason :clause-not-in-captured-bytes
                                            :path path :needle needle})))
                       end (+ at (count needle))
                       covered (filterv (fn [[s e _]] (and (< s end) (> e at))) spans)
                       i (nth (first covered) 2)
                       j (inc (nth (peek covered) 2))]
                   {:source (add! path) :lines [(inc i) j]
                    :quote (str/join "\n" (subvec lines i j))}))
        facts (mapv (fn [f] {:id (:id f) :meaning (:meaning f) :value (:q0 f)
                             :citations [(cite (:source f))] :observed-at pinned-at
                             ;; q0 stays a mission-document assertion; the record's
                             ;; own :runtime-state-unverified finding is retained
                             ;; verbatim, not acted on (§2).
                             :method :mission-document-assertion
                             :scope "documented capability"}) (:facts old))
        guard (fn guard [g] (into [(keyword (first g))]
                                  (map #(if (vector? %) (guard %) %)) (rest g)))
        interpretations
        (mapv (fn [x]
                (let [path (get-in x [:source :path])
                      record {:pattern (:pattern x) :source (add! path)
                              ;; Membership cites the FROZEN index copy, not the
                              ;; drifted live index (ORDINARY-RUN-SCOPE §3).
                              ;; EXCEPT for a pattern this occurrence AUTHORED:
                              ;; retrieval-index.tsv is the PRE-retrieval
                              ;; snapshot, so a genesis pattern cannot have a
                              ;; row in it. Its registration row was captured
                              ;; at occurrence time as new-index-row.patch,
                              ;; which is what §3's rule means for that case.
                              :membership [(clause (if (genesis-patterns (:pattern x))
                                                     genesis-row frozen-index)
                                                   (:pattern x))]
                              :clauses (into {} (for [k [:if :however :then]]
                                                  [k (clause path (get-in x [:quotes (keyword (str/upper-case (name k)))]))]))
                              :guard (guard (:guard x)) :effect (:effect x)
                              :authority :documented-interpretation :author "codex-27"}]
                  (assoc record :sha256 (evidence/value-digest record))))
              (:interpretations old))
        candidates (mapv (fn [i j]
                           (let [s (first (:sources j)) path (:path s)
                                 ;; Packet-2 candidate id normalization, as
                                 ;; find-receipt-test/sample applies it: the
                                 ;; retained artifact predates it, and tier-0
                                 ;; arms record bare names while the source
                                 ;; path carries the canonical section/name.
                                 ;; Mechanical, from pinned bytes only.
                                 canonical (when (and path (str/starts-with? path (str library-root "/")))
                                             (str/replace (subs path (inc (count library-root)))
                                                          #"\.flexiarg$" ""))]
                             {:pattern (or canonical (:candidate j))
                              :source (if path (add! path)
                                        {:status :none :reason :pattern-source-missing})
                              :rank (inc i)
                              :judgment {:relevant? (:relevant j) :reason (:judgment j)
                                         :mission-citations [mission-cite]
                                         :pattern-citations
                                         (if path [(clause path (get-in s [:clauses :IF]))]
                                           {:status :none :reason :pattern-source-missing})}}))
                         (range) (:candidate-judgments old))
        index-id (add! frozen-index)
        ;; Deterministic occurrence identity: fixed ids, UUIDs derived from a
        ;; fixed seed, so the committed artifact is byte-stable.
        seed (atom 0)
        uuid-fn #(UUID/nameUUIDFromBytes (bytes-of (str "wm08-route-a-" (swap! seed inc))))
        occurrence (retention/mint-occurrence
                    {;; The evidence schema requires :run/id to parse as a
                     ;; UUID; derive it from the rehearsal name so the minted
                     ;; identity stays deterministic and byte-stable.
                     :run-id (str (UUID/nameUUIDFromBytes (bytes-of "wm08-route-a-rehearsal")))
                     :cohort-id ":wm08-route-a"
                     :attempt-id "attempt-route-a"
                     :selected-action {:type :advance-mission :target "M-zaif-harness-v1"}
                     :now #(Instant/parse pinned-at) :uuid-fn uuid-fn})
        identity {:occurrence occurrence :semantic-epoch :wm08-route-a-v1
                  :data-root "/wm08-route-a"
                  :start-event-sha256 (evidence/sha256 (bytes-of "wm08-route-a-start"))
                  :interpreter-job "f13-redo-cc231bf6-reexpression" :author "codex-27"
                  :schema-version 1}
        record {:schema :wm/interpreted-pattern-set-v1 :identity identity
                :sources (vec (vals @sources))
                :target {:id "M-zaif-harness-v1" :kind :mission :action (:action/value occurrence)
                         :source (:source mission-cite) :citations [mission-cite] :pinned-at pinned-at}
                :retrieval {:query (get-in old [:retrieval :query])
                            :citations [mission-cite]
                            :runs [{:retriever "retained-combined-candidate-judgments"
                                    :version "cc231bf6"
                                    :index-source index-id :parameters {:rehearsal true}
                                    :candidates candidates :failures []}]}
                :facts facts :interpretations interpretations
                :holes (mapv (fn [f] {:kind (keyword (:kind f)) :reason (:reason f)
                                      :citations [mission-cite]})
                             (filter #(= :missing-pattern-interpretation (:kind %)) (:findings old)))
                :genesis (mapv (fn [g] {:pattern (:id g) :source (get-in g [:source :path])
                                        :index-source frozen-index
                                        :commit "cc231bf6f479c0dd87629edeca87b7c7dd973909"
                                        :gap (:gap g)}) (:new-patterns old))
                :failure nil}]
    {:record record :captured @captured}))

(defn frozen-occurrence
  "The occurrence binding validate-external!/resolve-designation check
  against: built from FROZEN-CONTEXT.edn, never from the run's output."
  []
  (let [frozen (edn/read-string (slurp (str run-dir "FROZEN-CONTEXT.edn")))
        o (:occurrence frozen)]
    {:target (get-in o [:target :id])
     :target-source (get-in o [:target :source])
     :repository-sha256 (:repository-sha256 o)
     :pinned-at (:pinned-at o)
     :source-digests (:source-digests o)}))

(defn ensure-artifact!
  "Build the deterministic re-expression, validate it, and make sure the
  committed artifact and companion bytes exist on disk. Returns the build."
  []
  (let [{:keys [record captured]} (build-reexpressed)
        ;; A refusal must not leave a half-validated artifact on disk: an
        ;; earlier version recorded the error and then wrote the record
        ;; anyway, so the next run re-read bytes that had never validated.
        validated? (try (= record (evidence/validate-sources! record captured))
                        (catch clojure.lang.ExceptionInfo e
                          (is false (str "source/citation validation refused: "
                                         (pr-str (ex-data e))))
                          false))
        record-bytes (bytes-of (pr-str record))
        companions (io/file run-dir "companions")]
    (is validated? "translated record passes source/citation validation as built")
    (when-not validated?
      (throw (ex-info "rehearsal record did not validate; artifact not written"
                      {:reason :record-invalid})))
    (.mkdirs companions)
    (doseq [[file bs] captured]
      (Files/write (.toPath (io/file companions file)) bs
                   (make-array java.nio.file.OpenOption 0)))
    (let [artifact (io/file run-dir "REEXPRESSED-RECORD.edn")]
      (when-not (.exists artifact)
        (Files/write (.toPath artifact) record-bytes (make-array java.nio.file.OpenOption 0)))
      (is (java.util.Arrays/equals record-bytes
                                   (Files/readAllBytes (.toPath artifact)))
          "committed REEXPRESSED-RECORD.edn is byte-identical to a fresh deterministic build"))
    {:record record :captured captured}))

(deftest route-a-reexpression-artifact
  (ensure-artifact!))

(defn load-from-disk
  "Reload the committed artifact plus its companion bytes — the rehearsal
  consumes files, not the builder's memory."
  []
  (ensure-artifact!)
  (let [record (edn/read-string (slurp (str run-dir "REEXPRESSED-RECORD.edn")))
        companions (str run-dir "companions/")
        read-bytes (fn [file] (Files/readAllBytes (.toPath (io/file (str companions file)))))]
    {:record record :read-bytes read-bytes
     :read-string (fn [file] (String. ^bytes (read-bytes file) "UTF-8"))}))

(deftest route-a-rehearsal
  (let [{:keys [record read-bytes]} (load-from-disk)
        ;; 1. independent context compiled from the record + companion bytes
        ctx (find/context record read-bytes library-root)
        ;; 2. the run; no designation artifact (honest vacuity)
        designated (fd/designation-for (frozen-occurrence) nil (:repository ctx))
        result (find/find record read-bytes library-root designated)
        _ (is (= result (find/validate-result! ctx designated result))
               "F1-F4 validation of the rehearsal run against independent context")
        ;; 3. designation path with NO artifact must report honest vacuity
        _ (is (= {:designated nil :f4 :vacuous :vacuous-because :no-designation-supplied}
                 (fd/resolve-designation (frozen-occurrence) nil (:repository ctx))))
        ;; 4. external expectations: zai-45's immutable artifact
        expectations-path (str run-dir "EXPECTATIONS.edn")
        read-refusal (ex-data-of #(fx/read-artifact expectations-path))]
    ;; What the run selected, and the guard values behind it.
    (is (= [:agent/evidence-over-assertion
            :coordination/bind-promotion-to-post-repair-replay
            :coordination/cross-validation-protocol
            :social/tension-before-code
            :social/verify-before-compose
            :stack-coherence/ready-blocked-triage
            :war-machine/operational-not-decorative]
           (vec (:selected result))))
    ;; The two known disagreements, adjudicated per §6.2: guard truth on the
    ;; recorded frozen facts is the decider, not either side's judgment.
    (testing "session-durability-check: expected by zai-45, judged not relevant"
      ;; No recorded interpretation exists for it, so there is no recorded
      ;; guard: it CANNOT fire on the frozen facts.
      (is (not (contains? (:interpretations ctx) :coordination/session-durability-check)))
      (is (not (some #{:coordination/session-durability-check} (:selected result)))))
    (testing "tension-before-code: judged relevant, no zai-45 row"
      ;; Its recorded guard fires on the recorded facts, so it is selected and
      ;; receipted regardless of any relevance judgment.
      (is (true? (get (:values ctx) :social/tension-before-code)))
      (is (contains? (:receipts result) :social/tension-before-code)))
    (testing "zai-45's artifact as committed"
      ;; The artifact is bound to the frozen occurrence but does not carry the
      ;; validator's flat :target-source field (its :target is the nested
      ;; FROZEN-CONTEXT map). read-artifact refuses it: reported, not edited.
      (is (= :invalid-expectation-artifact (:reason read-refusal)) (pr-str read-refusal)))
    (testing "counterfactual content check (diagnostic only, artifact untouched)"
      ;; With only the artifact's SHAPE normalized to the validator's flat
      ;; occurrence binding and its :expected rows copied verbatim, the
      ;; content refusals the run would raise — both §6.2 directions.
      (let [raw (edn/read-string (slurp expectations-path))
            shaped {:schema (:schema raw) :author (:author raw)
                    :occurrence (frozen-occurrence) :expected (:expected raw)}
            expected (set (keys (:expected raw)))
            fired (set (:selected result))
            unexpected (sort (set/difference fired expected))
            missing (sort (set/difference expected fired))
            d (ex-data-of #(fx/validate-external! (frozen-occurrence) shaped result))]
        ;; Fired with no expectation row: tension-before-code (the known
        ;; disagreement) AND the genesis pattern (no interpretation-independent
        ;; row can exist for a pattern this occurrence authored).
        (is (= [:coordination/bind-promotion-to-post-repair-replay
                :social/tension-before-code] unexpected))
        ;; Rows for patterns with no recorded interpretation, so no recorded
        ;; guard that could fire: the two known ones plus
        ;; maturity-evidence-audit (its missing-pattern-interpretation
        ;; finding is retained verbatim in :holes).
        (is (= [:agent/provisional-claims-ledger
                :coordination/session-durability-check
                :stack-coherence/maturity-evidence-audit] missing))
        ;; Even the first SHARED row refuses on content: the receipt's
        ;; :as-of :repository-sha256 is read-repository's VALUE digest while
        ;; the blind external producer can only pin the frozen INDEX digest
        ;; (the two-digest rule, §3), and the acknowledged-clause text differs
        ;; by leading indentation. Reported for zai-45's v2; not fixed here.
        (is (= :expectation-mismatch (:reason d))
            (pr-str (select-keys d [:reason :pattern]))))))
)
