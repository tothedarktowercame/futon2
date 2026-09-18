(ns futon2.aif.find-expectation
  "WM-08 external F2 expectations (agreed task futon2 371d89db, owner
  zai-16's corrections accepted; executor zai-53, 2026-09-18).

  An INDEPENDENT expectation artifact (:wm/find-expectation-v1) for one
  real retrieval occurrence, and the distinct validation function that
  requires it. Two laws make the independence real:

  1. PRE-OUTPUT BINDING. The artifact binds to the independently captured
     request/occurrence context — target source identity and digest, the
     pinned source set, the repository digest, :pinned-at — all of which
     exist before any interpretation or finder output. It is REFUSED a
     binding to a hash of the completed interpretation/output record:
     that hash is computable only after the output exists and would
     silently convert a frozen expectation into a post-hoc echo.

  2. NO SELF-SUPPLY. The producer must be independent of the finder.
     `:author {:role …}` is a DECLARED-ROLE negative control, not
     authenticated identity: a role on the finder/interpreter side is
     refused, because finder/interpreter output cannot provide its own
     expectations — the system agreeing with itself is not evidence.

  The expected rows carry the four comparison fields with FULL content:
     :clause-kind, :acknowledged-clause {:text … :lines …} (text AND
     lines, not span indices alone), :route, :as-of.

  `validate-external!` takes the artifact as a REQUIRED positional
  argument and validates it first — there is no optional-arity path
  through which the ordinary flow silently skips the requirement. It is
  the external F2 check; find-receipt's own validate-result! (F1/F3/
  guard/F4 and its compiled-context F2) stays for its own clauses and is
  expressly NOT credited as this check. F4 designation authority is
  untouched here.

  Input/validator machinery only: no caller integration (codex-7 owns
  receipt_construction.clj), no serving/ordinary-use claim."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.find-receipt :as find]))

(def schema :wm/find-expectation-v1)

(def finder-side-roles
  "Declared roles on the producing side — supplying the finder's or the
  interpreter's own output as its expectation. A DECLARED-ROLE control:
  this refuses what the artifact admits about itself; it does not, and
  cannot, authenticate who actually wrote it."
  #{:finder :find :interpreter :interpretation :retrieval
    :retrieval-system :finder-interpreter :self})

(defn- refuse! [ok law reason data]
  (when-not ok
    (throw (ex-info "External F2 expectation refused"
                    (merge {:finding :find-expectation/refusal
                            :law law :reason reason}
                           data)))))

(defn- clause-lines-key [clause-kind]
  (case clause-kind
    :if-clause :if-lines
    :however-clause :however-lines
    nil))

(defn- check-author! [author]
  (refuse! (not (and (map? author)
                     (contains? finder-side-roles (:role author))))
           :expectation :self-supplied-author
           {:author author
            :note "declared-role control: finder/interpreter output cannot provide its own expectations"}))

(defn- text-at [entry [a b :as span]]
  (refuse! (and (= 2 (count span)) (pos-int? a) (pos-int? b)
                (<= a b (count (:captured-lines entry))))
           :expectation :invalid-span {:span span})
  (str/join "\n" (subvec (:captured-lines entry) (dec a) b)))

(defn build-artifact
  "Build an external expectation artifact from INDEPENDENTLY captured
  bytes — everything this reads exists before interpretation or finder
  output. INPUT map:

    :library-root  the repository root the occurrence pinned
    :sources       the occurrence's pinned source set (the same pins the
                   interpreted-pattern-set record carries)
    :read-bytes    byte reader over the captured files
    :author        {:role … :name …} — an EXTERNAL role (refused on the
                   finder side; declared-role control)
    :target-source the pinned source id of the target
    :pinned-at     the occurrence's pinned-at
    :expected      {pattern-id {:clause-kind :if-clause :lines [a b]
                                :route :structured-antecedent}} — the
                   author's declared expectations; TEXT is pulled from the
                   captured bytes at the declared lines, never accepted
                   from the author (so a wrong clause is refused here as
                   :clause-not-in-captured-bytes, not later as a mismatch).

  Returns {:schema :wm/find-expectation-v1 :author …
           :occurrence {:target-sha256 … :repository-sha256 … :pinned-at …
                        :target-source …}
           :expected {id {:clause-kind … :acknowledged-clause {:text … :lines …}
                          :route … :as-of <the occurrence binding>}}}.

  Refuses: a finder-side author (:self-supplied-author); a declared span
  outside the pattern's authored clause block (:not-authored-clause —
  the author may expect a subspan of IF, not arbitrary text); an unknown
  pattern (:pattern-outside-repository)."
  [{:keys [library-root sources read-bytes author target-source pinned-at expected]
    :or {expected {}}}]
  (check-author! author)
  (let [repository (find/read-repository library-root sources read-bytes)
        source-ids (into {} (map (juxt :id identity)) sources)
        target (get source-ids target-source)
        _ (refuse! (some? target) :expectation :unknown-target-source
                   {:target-source target-source})
        occurrence {:target-sha256 (:sha256 target)
                    :repository-sha256 (:digest repository)
                    :pinned-at pinned-at
                    :target-source target-source}
        rows (into (sorted-map)
                   (map (fn [[id {:keys [clause-kind lines route]
                                  :or {clause-kind :if-clause
                                       route :structured-antecedent}}]]
                          (let [entry (get-in repository [:entries id])]
                            (refuse! (some? entry) :expectation
                                     :pattern-outside-repository {:pattern id})
                            (let [[lo hi] (get entry (clause-lines-key clause-kind))]
                              (refuse! (and lo hi (<= lo (first lines) (second lines) hi))
                                       :expectation :not-authored-clause
                                       {:pattern id :clause clause-kind :span lines}))
                          [id {:clause-kind clause-kind
                               :acknowledged-clause {:text (text-at entry lines)
                                                     :lines lines}
                               :route route
                               :as-of (select-keys occurrence
                                                   [:target-sha256 :repository-sha256 :pinned-at])}]))
                        expected))]
    {:schema schema
     :author author
     :occurrence occurrence
     :expected rows}))

(defn validate-external!
  "The external four-field F2 check: emitted receipts against the REQUIRED
  external expected content. ARTIFACT is positional and validated FIRST —
  there is no nil/optional path that skips the requirement.

  CTX is find-receipt's independently compiled context (its :as-of is the
  live occurrence binding). RESULT is the FindResult. Refusals, each typed:

    :expectation-artifact-required  nil or wrong-schema artifact
    :self-supplied-author           author declares a finder-side role
    :occurrence-mismatch            artifact binding ≠ live occurrence
                                    (names the mismatched fields)
    :unexpected-selection           a selected pattern with no expected row
                                    (names the patterns)
    :expectation-mismatch           a receipt's :clause-kind /
                                    :acknowledged-clause {:text :lines} /
                                    :route / :as-of ≠ the expected row

  Returns RESULT on success. This check is separate from F4 designation
  authority and from find-receipt/validate-result!'s own F1/F3/F4 and
  compiled-context checks, which remain for their own clauses."
  [ctx artifact result]
  (refuse! (and (map? artifact) (= schema (:schema artifact)))
           :expectation :expectation-artifact-required
           {:artifact (if (map? artifact) (:schema artifact) (type artifact))})
  (check-author! (:author artifact))
  (let [live (:as-of ctx)
        binding (:occurrence artifact)
        fields [:target-sha256 :repository-sha256 :pinned-at]
        mismatched (vec (remove #(= (get binding %) (get live %)) fields))]
    (refuse! (empty? mismatched) :expectation :occurrence-mismatch
             {:mismatched-fields mismatched
              :artifact-occurrence (select-keys binding fields)
              :live-occurrence (select-keys live fields)}))
  (let [selected (set (:selected result))
        expected (:expected artifact)
        unexpected (vec (sort (set/difference selected (set (keys expected)))))]
    (refuse! (empty? unexpected) :F2 :unexpected-selection {:patterns unexpected})
    (doseq [id selected
            :let [receipt (get-in result [:receipts id])
                  row (get expected id)
                  four [:clause-kind :acknowledged-clause :route :as-of]]]
      (refuse! (= (select-keys receipt four) (select-keys row four))
               :F2 :expectation-mismatch
               {:pattern id
                :emitted (select-keys receipt four)
                :expected (select-keys row four)}))
    result))
