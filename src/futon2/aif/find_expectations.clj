(ns futon2.aif.find-expectations
  "External F2 expectation artifact and its validator.
  TASK holes/labs/wm-contract/runs/wm-08-external-f2-2026-09-16/TASK.md:
  find-receipt/context compiles expectations from the same interpretation
  record the finder consumed -- a self-consistency check, not credited as the
  independent external F2 check. This namespace holds an expectation artifact
  that exists BEFORE the finder's output and is bound to independently
  captured occurrence context, plus validate-external! which compares the
  emitted receipts against it on the four comparison fields
  [:clause-kind :acknowledged-clause :route :as-of].

  Credit is input/validator machinery only; no caller integration here.

  The author control is a DECLARED ROLE check, not authentication: an
  artifact whose :author :role is the finder's or interpreter's own role
  (anything other than :external-expectation-producer) is refused as
  self-supplied. There is no optional-artifact path: a caller with no
  artifact is refused, never waved through.

  build-artifact (ported from the singular duplicate 6eabefe6 after
  claude-4's duplicate finding, 2026-09-18): the author declares LINES,
  never TEXT — clause text is pulled from the independently captured
  pattern bytes at the declared span, and a span outside the pattern's
  authored clause block is refused at BUILD time. :repository-sha256 is
  an INPUT here (the frozen index digest from FROZEN-CONTEXT.edn), never
  computed from the pattern bytes — see the two-digest trap below."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [futon2.aif.find-receipt :as find]))

(def schema-id :wm/find-expectations-v1)

(def comparison-fields [:clause-kind :acknowledged-clause :route :as-of])

(defn- need! [ok reason data]
  (when-not ok
    (throw (ex-info "External expectation validation refused"
                    (merge {:finding :find/refusal :law :F2 :reason reason} data)))))

(defn read-artifact
  "Read and structurally validate an expectation artifact from EDN. This is
  the ONLY entry point that mints a usable artifact; a malformed map read
  by any other means still fails validate-external!."
  [source]
  (let [artifact (edn/read-string (slurp source))]
    (need! (= schema-id (:schema artifact)) :invalid-expectation-artifact
           {:schema (:schema artifact)})
    (need! (and (map? (:author artifact)) (:id (:author artifact))
                (keyword? (:role (:author artifact))))
           :invalid-expectation-artifact {:author (:author artifact)})
    (need! (and (map? (:occurrence artifact))
                (:target (:occurrence artifact))
                (map? (:target-source (:occurrence artifact)))
                (:repository-sha256 (:occurrence artifact))
                (:pinned-at (:occurrence artifact)))
           :invalid-expectation-artifact {:occurrence (:occurrence artifact)})
    (need! (and (map? (:expected artifact))
                (seq (:expected artifact))
                (every? (fn [[id row]]
                          (and (keyword? id) (namespace id)
                               (= :if-clause (:clause-kind row))
                               (map? (:acknowledged-clause row))
                               (string? (:text (:acknowledged-clause row)))
                               (let [l (:lines (:acknowledged-clause row))]
                                 (and (vector? l) (= 2 (count l))
                                      (every? pos-int? l)))
                               (keyword? (:route row))
                               (map? (:as-of row))))
                        (:expected artifact)))
           :invalid-expectation-artifact {})
    artifact))

(defn- check-occurrence-binding! [occurrence artifact]
  ;; TWO DIFFERENT DIGESTS SHARE THIS KEY NAME, and unifying them would void
  ;; both bindings (zai-16, link-4 discovery, 2026-09-17):
  ;;   - HERE, :repository-sha256 is the frozen INDEX digest of the pattern
  ;;     library at retrieval time (FROZEN-CONTEXT.edn, from the retrieval
  ;;     index TSV). It says WHICH library state the occurrence happened in.
  ;;   - In find-receipt/validate-result! it is read-repository's VALUE digest
  ;;     over the pattern entries' {:path :sha256}. It says the finder read
  ;;     the repository it claims to have read.
  ;; They are never compared with each other, and they must not be: the
  ;; occurrence binding and the result check answer different questions. If
  ;; you are here because the two "don't match", that is correct behaviour.
  (let [o (:occurrence artifact)
        mismatch (fn [k] (need! false :occurrence-binding-mismatch
                                {:field k :artifact (get o k)
                                 :occurrence (get occurrence k)}))]
    (need! (and (map? occurrence) (:target occurrence)
                (:target-source occurrence) (:repository-sha256 occurrence)
                (:pinned-at occurrence))
           :occurrence-required {})
    (doseq [k [:target :target-source :repository-sha256 :pinned-at]]
      (when-not (= (get occurrence k) (get o k)) (mismatch k)))
    (when (and (:source-digests occurrence) (:source-digests o))
      (when-not (= (:source-digests occurrence) (:source-digests o)) (mismatch :source-digests)))))

(defn validate-external!
  "Compare RESULT's receipts against the independently supplied ARTIFACT on
  the four comparison fields, and check the artifact's occurrence binding
  against the independently captured OCCURRENCE context
  {:target id :target-source {:path .. :sha256 ..}
   :repository-sha256 .. :pinned-at .. :source-digests {}}.

  Refusals (typed {:finding :find/refusal :law :F2 :reason ..} as
  find-receipt/need! produces): :external-expectations-required when no
  artifact was supplied (there is NO optional path that silently skips the
  requirement); :self-supplied-expectations when the declared author role is
  the finder's/interpreter's own rather than :external-expectation-producer;
  :occurrence-binding-mismatch when the artifact is bound to a different
  occurrence; :unexpected-selected-pattern for a receipt with no expected
  row; :expectation-mismatch on any of the four fields;
  :expected-pattern-not-selected when a row the artifact requires did not
  fire (opt out per row with :must-fire? false).

  The declared-role control is not authentication -- it refuses the role the
  finder and interpreter themselves occupy, nothing more."
  [occurrence artifact result]
  (need! (some? artifact) :external-expectations-required {})
  (need! (= schema-id (:schema artifact)) :invalid-expectation-artifact
         {:schema (:schema artifact)})
  (need! (= :external-expectation-producer
            (get-in artifact [:author :role]))
         :self-supplied-expectations {:author-role (get-in artifact [:author :role])})
  (check-occurrence-binding! occurrence artifact)
  (let [selected (vec (:selected result))
        expected (:expected artifact)]
    (need! (and (vector? selected) (seq selected)) :invalid-result {})
    (doseq [id selected
            :let [receipt (get-in result [:receipts id]) row (get expected id)]]
      (need! (map? receipt) :invalid-result {:pattern id})
      (need! (some? row) :unexpected-selected-pattern {:pattern id})
      ;; compare the four fields on BOTH sides: a row may carry its own
      ;; provenance (which frozen context it was written from, by whom), and
      ;; that must not read as a content mismatch
      (need! (= (select-keys row comparison-fields)
                (select-keys receipt comparison-fields))
             :expectation-mismatch
             {:pattern id
              :expected (select-keys row comparison-fields)
              :actual (select-keys receipt comparison-fields)}))
    ;; The symmetric direction: an expectation the finder did NOT satisfy.
    ;; Without this a finder that retrieves a SUBSET of what was independently
    ;; expected passes -- and retrieving too little is the failure the
    ;; independent expectations exist to catch. A row may opt out with an
    ;; explicit :must-fire? false (an expectation that only constrains content
    ;; IF the pattern fires); the default is that it must fire, so nothing is
    ;; weakened silently.
    (let [fired (set selected)
          missing (vec (sort (keep (fn [[id row]]
                                     (when (and (not (contains? fired id))
                                                (not (false? (:must-fire? row))))
                                       id))
                                   expected)))]
      (need! (empty? missing) :expected-pattern-not-selected
             {:patterns missing :selected selected})))
  result)

(defn build-artifact
  "Build an expectation artifact from INDEPENDENTLY captured bytes — the
  author declares LINES, never TEXT. Everything this reads exists before
  interpretation or finder output. INPUT map:

    :library-root  the repository root the occurrence pinned
    :sources       the occurrence's pinned source set
    :read-bytes    byte reader over the captured files
    :author        {:id …} — the ROLE is fixed at
                   :external-expectation-producer here; anything the caller
                   tries to smuggle in :role is refused as self-supplied
    :occurrence    {:target id :repository-sha256 <frozen INDEX digest from
                   FROZEN-CONTEXT.edn — an INPUT, never recomputed from the
                   pattern bytes; see the two-digest trap above>
                   :pinned-at … :source-digests {…}}
    :expected      {pattern-id {:lines [a b] :route :structured-antecedent
                                :must-fire? true}} — the author's declared
                   expectations; TEXT is read from the captured bytes at
                   the declared lines and any extra author-supplied
                   :acknowledged-clause is overwritten, never trusted.

  Refuses (typed, law :F2 as this namespace's other refusals):
  :self-supplied-expectations when :author :role is supplied and is not
  :external-expectation-producer; :pattern-outside-repository;
  :not-authored-clause (a span outside the pattern's authored IF block —
  the author may expect a subspan of IF, not arbitrary text);
  :invalid-span.

  The returned map conforms to read-artifact's shape, so it round-trips
  through EDN and the ordinary validate-external! path."
  [{:keys [library-root sources read-bytes author occurrence expected]
    :or {expected {}}}]
  (need! (= :external-expectation-producer
            (or (:role author) :external-expectation-producer))
         :self-supplied-expectations {:author author})
  (let [repository (find/read-repository library-root sources read-bytes)
        ;; Row :as-of is receipt-shaped (find-receipt's as-of), derived from
        ;; the independently captured occurrence binding — target sha from
        ;; the pinned target source, repository digest and pinned-at as
        ;; captured. NOT a comparison against the receipt's as-of computed
        ;; by the finder: validate-external! compares the two and a build
        ;; from the true occurrence context makes them agree.
        row-as-of {:target-sha256 (get-in occurrence [:target-source :sha256])
                   :repository-sha256 (:repository-sha256 occurrence)
                   :pinned-at (:pinned-at occurrence)}
        text-at (fn [entry [a b :as span]]
                  (need! (and (= 2 (count span)) (every? pos-int? span)
                              (<= a b (count (:captured-lines entry))))
                         :invalid-span {:span span})
                  (str/join "\n" (subvec (:captured-lines entry) (dec a) b)))
        rows (into (sorted-map)
                   (map (fn [[id {:keys [lines route must-fire?]
                                  :or {route :structured-antecedent}}]]
                          (let [entry (get-in repository [:entries id])
                                [lo hi] (:if-lines entry)]
                            (need! (some? entry) :pattern-outside-repository
                                   {:pattern id})
                            (need! (and lo hi (<= lo (first lines) (second lines) hi))
                                   :not-authored-clause
                                   {:pattern id :clause :if-clause :span lines})
                            [id {:clause-kind :if-clause
                                 :acknowledged-clause
                                 {:text (text-at entry lines) :lines lines}
                                 :route route
                                 :as-of row-as-of
                                 :must-fire? (not (false? must-fire?))}]))
                        expected))]
    {:schema schema-id
     :author (assoc author :role :external-expectation-producer)
     :occurrence occurrence
     :expected rows}))

(defn external-expectations-checked?
  "Pure acceptance reader (WM-08-delivery control C1's consumer,
  zai-50/claude-4 2026-09-18): true iff a receipt payload's recorded
  construction actually RAN the external F2 check — status :validated
  with a named external author. The seam records {:status :not-supplied}
  when no artifact was configured; at RUNTIME that absence is a recorded
  fact, not a fault (receipt-construction's own docstring — every ordinary
  non-WM-08 interpretation runs without one). THIS predicate is where
  absence becomes a failure: the item whose acceptance demands the
  evidence reads this and refuses. Returns a map, never a bare boolean,
  so the refusal it feeds names exactly what was recorded."
  [payload]
  (let [status (get-in payload [:judgment :receipted-construction
                                :external-expectations])]
    (if (and (map? status) (= :validated (:status status)) (:author status))
      {:checked? true :record status}
      {:checked? false
       :record status
       :reason (cond (nil? status) :no-external-expectations-record
                     (map? status) (:status status)
                     :else :invalid-record)})))
