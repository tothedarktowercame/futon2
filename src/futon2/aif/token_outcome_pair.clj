(ns futon2.aif.token-outcome-pair
  "OBS-P (PROOF-2 strategy row 18): the occurrence-bound measurement pair
   carrier :wm/token-outcome-pair-v1, specified by OBS-D (proof2/packets/
   OBS-D.md §4, commit 3277e9e6) and amended by OBS-D Revision 2 (commit
   508a410e: complete close-carrier census and extracted pair identity).
   The amended producer emits :schema-version 2 and declares
   :pair-hash-domain :wm/token-outcome-pair-value-v2; version-1 hashes
   (evidence/value-digest over the whole pair map) are not reinterpreted.

   For one occurrence and one token, a pair joins the OBSERVATION leg
   (what the locator/check reported) with the TRUTH leg (whether the
   token was really produced, adjudicated through a channel independent
   of that locator). Each leg is a boolean or a typed absence with a
   reason -- never a substituted value. A pair is estimable (usable in a
   false-positive / false-negative denominator) only when BOTH legs are
   booleans; the ineligibility is a field on the pair, not a convention.

   Observation-leg SOURCES (Revision 2 §R2.1). Two close carriers may
   supply the observation leg, each recording the raw enactment file it
   was extracted from as :source {:path :sha256}:
   - a comparison :tokens row ([:payload :judgment :token-outcome-comparison]
     or its route-attestation copy), source = the receipt's
     :measurement-source;
   - a kernel-example projection row ([:payload :judgment :kernel-example
     :observation-projection :observations <token> :artifact-observation]),
     source = the example's :observation-source. The projection is the
     complete admitted token population; :tokens is only the wanted
     projection.
   The comparison rows are copies of kernel rows on the same
   occurrence/token/revision; a caller must not count both.

   Refusals are checks in code, not instructions (the OBS-D falsifiers):
   - a typed-missing observation can never surface as a measured
     false (or true): the missing form propagates as {:status :missing};
   - an accepted-increment verdict is refused from BOTH legs: it is a
     verdict built FROM locator observations, so it can be neither the
     observation (relabelled verdict) nor the truth (the thing those
     observations would be measured against), and any truth leg whose
     evidence is the same verdict source as the observation leg refuses
     the whole pair;
   - the learning-trial receipt ([:payload :judgment
     :learning-trial-receipt], :wm/learning-trial-receipt-v2) and its
     trial wrappers carry the B trial (admission, ledger :counted?,
     illustrative Beta) over the SAME verified observations the kernel
     example holds; they are neither a fresh observation nor a truth and
     are refused from both legs with :learning-trial-receipt-not-a-leg."
  (:require [clojure.string :as str]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def schema :wm/token-outcome-pair-v1)
(def schema-version 2)
(def pair-hash-domain :wm/token-outcome-pair-value-v2)

(def occurrence-keys
  "The occurrence identity projection (Revision 2 §R2.3): exactly these
   six keys enter the pair hash; the full :action/value payload does not."
  [:run/id :cohort/id :attempt/id :transition/id :action/id :action/value-sha256])

(defn- missing [reason] {:status :missing :reason reason})

(defn occurrence-identity
  "Project `occurrence` onto occurrence-keys. A key the record did not
   carry is the typed absence {:status :missing :reason
   :identity-not-recorded}, never nil and never omitted."
  [occurrence]
  (into {} (map (fn [k] [k (or (get occurrence k) (missing :identity-not-recorded))]))
        occurrence-keys))

;; ---------------------------------------------------------------------------
;; Canonical bytes and the pair hash (Revision 2 §R2.3, CERT-S §3).
;; ---------------------------------------------------------------------------

(defn canonical-edn
  "CERT-S §3 canonical text of `x`, UTF-8 encoded by the caller, no
   trailing newline:
   - maps print as {k v k v}, entries sorted by the recursively
     canonical printed key, one space between key and value and between
     entries, no commas;
   - sets print as #{e e}, elements sorted by their canonical text, set
     delimiters preserved (so #{:a :b} and [:a :b] differ);
   - vectors stay vectors [..]; other sequentials print as (..);
   - doubles print as #wm/double \"<Double/toHexString>\"; integers and
     ratios print exactly (2/3 stays 2/3);
   - everything else prints with pr-str under *print-readably* (normal
     EDN string escaping), with metadata, print-length/level and
     namespaced-map syntax disabled."
  [x]
  (cond
    (map? x) (str "{" (str/join " " (map (fn [[k v]] (str k " " v))
                                        (sort-by first
                                                 (map (fn [[k v]] [(canonical-edn k)
                                                                  (canonical-edn v)]) x)))) "}")
    (set? x) (str "#{" (str/join " " (sort (map canonical-edn x))) "}")
    (vector? x) (str "[" (str/join " " (map canonical-edn x)) "]")
    (sequential? x) (str "(" (str/join " " (map canonical-edn x)) ")")
    (double? x) (str "#wm/double " (pr-str (Double/toHexString x)))
    :else (binding [*print-meta* false *print-length* nil *print-level* nil
                    *print-namespace-maps* false *print-readably* true]
            (pr-str x))))

(defn pair-value
  "The hashed value of a pair -- exactly the five-key map Revision 2
   §R2.3 names, and nothing else:

     {:occurrence    (occurrence-identity (:occurrence pair))  ; six keys
      :token         the qualified token, or the typed absence
      :revision-pair {:before .. :after ..} as recorded, each side a sha
                     string or a typed absence
      :observation   the complete extracted observation leg, INCLUDING
                     its :source {:path :sha256} raw-file reference
      :truth         the complete extracted truth leg or typed absence}

   Outside the byte domain: :schema, :schema-version, :pair-hash-domain,
   :reviewed-revision, :estimable?, :ineligibility-reasons, :status/:kind
   of a refused pair, the full :action/value payload, and :pair-sha256
   itself. Recompute from these five values to verify."
  [pair]
  {:occurrence (occurrence-identity (:occurrence pair))
   :token (:token pair)
   :revision-pair (:revision-pair pair)
   :observation (:observation pair)
   :truth (:truth pair)})

(defn pair-digest
  "\"sha256:<hex>\" over the UTF-8 bytes of (canonical-edn (pair-value
   pair)). This is a canonical EXTRACTED-VALUE hash (GEN-D §1.3), not a
   raw-record identity: it addresses what the pair says, and neither
   verifies nor replaces the raw file sha carried in
   [:observation :source :sha256]."
  [pair]
  (str "sha256:" (evidence/sha256 (.getBytes ^String (canonical-edn (pair-value pair)) "UTF-8"))))

;; ---------------------------------------------------------------------------
;; Carrier detection. Three close-time carriers must never become a leg:
;; the accepted-increment verdict (:accepted?), the learning-trial receipt
;; (:wm/learning-trial-receipt-v*) and its per-trial wrappers.
;; ---------------------------------------------------------------------------

(defn learning-trial-carrier?
  "True when `x` is the learning-trial receipt, one of its trial
   wrappers, its prior, or a leg input that declares the receipt as its
   source. Detection is by the marks the live carrier actually carries
   (machinery-76 attempt-002): the receipt's :schema; a trial's
   :trial-grain, :learning-family, :counted? and :signed-observation
   (the prior map also carries :trial-grain)."
  [x]
  (and (map? x)
       (or (contains? #{:wm/learning-trial-receipt-v1 :wm/learning-trial-receipt-v2
                        :wm/learning-trial-prior-v1}
                      (:schema x))
           (contains? x :trial-grain)
           (contains? x :learning-family)
           (contains? x :counted?)
           (contains? x :signed-observation)
           (= :learning-trial-receipt (:observation-source x))
           (= :learning-trial-receipt (:truth-source x)))))

(defn verdict-shaped?
  "True when `x` is an accepted-increment-style verdict map. Such a map is
   a compound judgment over locator observations (accepted_increment.clj
   conjuncts (a)-(c)); OBS-D §1.2 bars it from both legs."
  [x]
  (and (map? x) (contains? x :accepted?)))

(defn- accepted-increment-source?
  "True when a declared source keyword names the accepted-increment
   verdict (either conjunct) rather than a check or adjudication act."
  [source]
  (contains? #{:accepted-increment
               :accepted-increment-verdict
               :accepted-increment-conjunct-b
               :accepted-increment-conjunct-c}
             source))

(defn- source-ref?
  "A raw-file source reference as the runner records it: a path string
   and a 64-hex-digit SHA-256."
  [source]
  (and (map? source)
       (string? (:path source))
       (string? (:sha256 source))
       (some? (re-matches #"[0-9a-f]{64}" (:sha256 source)))))

;; ---------------------------------------------------------------------------
;; Legs
;; ---------------------------------------------------------------------------

(defn observation-leg
  "The observation leg from one comparison :tokens row (token_outcome.clj
   :tokens entry): boolean when the row's :observed is boolean, with the
   check class and revision-pinned evidence carried from the measurement
   row's :result; the row's typed-missing form propagated unchanged when
   not. A missing observation never becomes a measured false -- the
   boolean branch requires (boolean? observed), exactly the distinction
   token_outcome.clj:66-72 already draws. A learning-trial carrier or an
   accepted-increment verdict offered as the row is refused before its
   :observed is read."
  [token-row]
  (let [observed (:observed token-row)
        result (get-in token-row [:measurement :result])]
    (cond
      (learning-trial-carrier? token-row)
      {:status :refused :kind :learning-trial-receipt-not-a-leg
       :reason :learning-trial-receipt-not-a-leg}

      (or (verdict-shaped? token-row)
          (accepted-increment-source? (:observation-source token-row)))
      {:status :refused :kind :verdict-not-an-observation
       :reason :accepted-increment-source-refused}

      (boolean? observed)
      {:observed observed
       :check (:check result)
       :evidence (:evidence result)
       :observation-source :close-measurement-row}

      (and (map? observed) (= :missing (:status observed)))
      {:status :missing
       :kind (or (:kind observed) :measurement-unavailable)
       :observation-source :close-measurement-row}

      :else
      {:status :missing
       :kind :measurement-unavailable
       :observation-source :close-measurement-row})))

(defn truth-leg
  "The truth leg from a supplied adjudication `truth`
   {:truth boolean :truth-source <keyword> :adjudicator <identity>
    :evidence <map> :adjudicated-at <instant>}, or the typed absence
   :no-independent-truth-channel when nothing independent was supplied.
   A learning-trial carrier, a verdict-shaped map, or an
   accepted-increment-sourced input is REFUSED, not defaulted: the
   refusal records the attempted relabelling."
  [truth]
  (cond
    (learning-trial-carrier? truth)
    {:status :refused :kind :learning-trial-receipt-not-a-leg
     :reason :learning-trial-receipt-not-a-leg}

    (nil? truth)
    {:status :missing :reason :no-independent-truth-channel}

    (verdict-shaped? truth)
    {:status :refused :kind :verdict-not-a-truth
     :reason :accepted-increment-verdict-refused}

    (accepted-increment-source? (:truth-source truth))
    {:status :refused :kind :verdict-not-a-truth
     :reason :accepted-increment-source-refused}

    (boolean? (:truth truth))
    (select-keys truth [:truth :truth-source :adjudicator :evidence
                        :adjudicated-at])

    :else
    {:status :missing :reason :truth-not-a-boolean
     :detail (dissoc truth :evidence)}))

(defn kernel-observation-leg
  "The observation leg for `token` from the close's kernel example
   (:wm/aligned-kernel-example-v1). Reads the already admitted row at
   [:observation-projection :observations token :artifact-observation]
   and carries its boolean, check class and evidence, plus the raw-file
   reference the example was aligned from (:observation-source
   {:path :sha256}) as :source. Never promotes the example's prediction,
   disposition or close outcome; never reads the file or re-runs
   verification.

   Every join the row depends on is checked and a failure is a typed
   absence naming the failed join: carrier not recorded; not a recorded
   aligned example; occurrence identity differs from the caller's;
   projection not :admitted (its own :kind, e.g.
   :task-execution-incomplete); source reference malformed; no row for
   the token; measurement token differs; the row's :observed disagrees
   with its measurement result; artifact sha, projection :after and
   evidence :resolved-sha do not all equal `reviewed-revision`."
  [example occurrence token reviewed-revision]
  (let [projection (:observation-projection example)
        row (get-in projection [:observations token])
        artifact (:artifact-observation row)
        measurement (:measurement artifact)
        source (:observation-source example)
        provenance {:observation-source :kernel-example
                    :source (or source (missing :observation-source-not-recorded))
                    :source-key-path [:observation-projection :observations token
                                      :artifact-observation]}
        reason (cond
                 (learning-trial-carrier? example) :learning-trial-receipt-not-a-leg
                 (nil? example) :carrier-not-recorded
                 (not= :recorded (:status example)) (or (:kind example) (:reason example)
                                                        :kernel-example-not-recorded)
                 (not= :wm/aligned-kernel-example-v1 (:schema example)) :kernel-example-schema-mismatch
                 (not (string? (:run/id occurrence))) :occurrence-not-recorded
                 (not= (occurrence-identity occurrence)
                       (occurrence-identity (:occurrence example))) :occurrence-mismatch
                 (not= :admitted (:status projection)) (or (:kind projection) :observation-not-admitted)
                 (not= (occurrence-identity occurrence)
                       (occurrence-identity (:occurrence projection))) :occurrence-mismatch
                 (not (source-ref? source)) :observation-source-not-recorded
                 (nil? row) :token-observation-not-recorded
                 (not= token (:token measurement)) :token-measurement-mismatch
                 (not= (:observed artifact) (get-in measurement [:result :observed])) :observation-evidence-mismatch
                 (not (and (string? reviewed-revision)
                           (= reviewed-revision (:artifact-sha artifact)
                              (get-in projection [:revision-pair :after])
                              (get-in measurement [:result :evidence :resolved-sha])))) :artifact-revision-mismatch)]
    (merge provenance
           (if reason
             {:status (if (= :learning-trial-receipt-not-a-leg reason) :refused :missing)
              :reason reason :kind reason}
             (dissoc (observation-leg {:observed (:observed artifact)
                                      :measurement measurement}) :observation-source)))))

;; ---------------------------------------------------------------------------
;; Same-verdict-source refusal. When both legs carry full evidence maps
;; and those maps are EQUAL, both legs report the same read of the same
;; artifact at the same revision: the pair would have correlation 1 by
;; construction (OBS-D §5, second bad case). The whole pair refuses.
;; ---------------------------------------------------------------------------

(defn- same-verdict-source?
  "True when both resolved legs carry evidence maps that are identical,
   or when the truth leg's declared source names one of the observation
   carriers (the comparison row or the kernel example). Only meaningful
   when both legs resolved to values; a typed-absent leg has nothing to
   correlate with."
  [obs-leg truth-map]
  (let [obs-ev (:evidence obs-leg)
        truth-ev (:evidence truth-map)]
    (boolean
     (and (map? obs-ev) (map? truth-ev)
          (or (= obs-ev truth-ev)
              (contains? #{:close-measurement-row :kernel-example}
                         (:truth-source truth-map)))))))

;; ---------------------------------------------------------------------------
;; Pair assembly
;; ---------------------------------------------------------------------------

(defn- estimable-fields
  "The eligibility FIELDS (acceptance 3): estimability is decided by both
   legs being booleans, and any typed-absent leg is named in
   :ineligibility-reasons. A reader never infers this from convention."
  [obs-leg tr-leg]
  (let [obs-bool (boolean? (:observed obs-leg))
        tr-bool (boolean? (:truth tr-leg))
        reasons (cond-> []
                  (not obs-bool) (conj :observation-not-a-boolean)
                  (not tr-bool) (conj :truth-not-a-boolean))]
    {:estimable? (and obs-bool tr-bool)
     :ineligibility-reasons reasons}))

(defn- revision-pinned?
  "When both the pair's reviewed revision and a leg's evidence resolved
   sha are present, they must agree; a leg that names a different
   revision is demoted to a typed absence rather than silently
   cross-pinned (OBS-P falsifier: mismatched reviewed revision)."
  [leg reviewed-revision]
  (let [resolved (get-in leg [:evidence :resolved-sha])]
    (if (and (some? reviewed-revision) (some? resolved)
             (not= reviewed-revision resolved))
      {:status :missing :kind :artifact-revision-mismatch
       :expected reviewed-revision :actual resolved}
      leg)))

(defn- typed-revision-pair
  "The recorded revision pair with each unrecorded side typed. When only
   an after revision is known, :before is
   {:status :missing :reason :before-revision-not-recorded}; no
   predecessor is reconstructed."
  [revision-pair kernel-example reviewed-revision]
  (let [rp (or revision-pair
               (get-in kernel-example [:observation-projection :revision-pair])
               {:after reviewed-revision})]
    (reduce (fn [r [k reason]] (update r k #(or % (missing reason))))
            rp
            [[:before :before-revision-not-recorded]
             [:after :after-revision-not-recorded]])))

(defn build-pair
  "One pair for `token` of one occurrence. Inputs:

     :occurrence         identity map (must carry :run/id and :attempt/id;
                         only occurrence-keys enter the pair identity)
     :token              the qualified token, or nil when the comparison
                         itself was absent and no token is knowable
     :token-row          the comparison :tokens entry for this token, or
                         nil when the comparison supplied no row
     :measurement-source the comparison receipt's :measurement-source
                         {:path :sha256}, recorded on the leg as :source
     :kernel-example     the close's kernel example; when supplied the
                         observation leg is kernel-observation-leg for
                         :token and :token-row is ignored
     :comparison-reason  reason keyword when the comparison itself was
                         absent/refused (e.g. :comparison-not-supplied)
     :revision-pair      the recorded {:before :after}; defaults to the
                         kernel example's projection pair, else to
                         :reviewed-revision as :after with :before typed
                         missing
     :reviewed-revision  the after-revision sha both legs pin to;
                         defaults to the revision pair's :after
     :truth              adjudication input for truth-leg, or nil

   Returns the pair map, or a refusal {:schema … :status :refused :kind
   :same-verdict-source} when both legs would report one act. Every pair
   carries :pair-sha256 = (pair-digest pair): the canonical
   extracted-value hash over the five pair-value keys, never a
   raw-record identity."
  [{:keys [occurrence token token-row comparison-reason reviewed-revision
           truth kernel-example revision-pair measurement-source]}]
  (let [revision-pair (typed-revision-pair revision-pair kernel-example reviewed-revision)
        reviewed-revision (or reviewed-revision
                              (when (string? (:after revision-pair)) (:after revision-pair)))
        base {:schema schema :schema-version schema-version
              :pair-hash-domain pair-hash-domain
              :occurrence (or occurrence (missing :occurrence-not-recorded))
              :token (or token (missing :token-not-recorded))
              :revision-pair revision-pair
              :reviewed-revision (or reviewed-revision (missing :after-revision-not-recorded))}
        obs-leg (cond
                  (some? kernel-example)
                  (kernel-observation-leg kernel-example occurrence token reviewed-revision)

                  (some? comparison-reason)
                  {:status :missing :kind comparison-reason}

                  (nil? token-row)
                  {:status :missing :kind :measurement-unavailable}

                  :else
                  (cond-> (observation-leg token-row)
                    measurement-source (assoc :source measurement-source)))
        obs-leg (revision-pinned? obs-leg reviewed-revision)
        tr-leg (revision-pinned? (truth-leg truth) reviewed-revision)
        pair (if (and (boolean? (:observed obs-leg)) (boolean? (:truth tr-leg))
                      (same-verdict-source? obs-leg tr-leg))
               (assoc base :status :refused :kind :same-verdict-source
                      :observation obs-leg :truth tr-leg
                      :estimable? false
                      :ineligibility-reasons [:same-verdict-source])
               (merge base {:observation obs-leg :truth tr-leg}
                      (estimable-fields obs-leg tr-leg)))]
    (assoc pair :pair-sha256 (pair-digest pair))))

(defn pairs-from-comparison
  "Runner adapter. Derive the pair vector for one close from a comparison
   receipt (the value retained under :token-outcome-comparison):

     :comparison        the comparison receipt (any :status), or a bare
                        {:status :absent :reason <kw>} map as the
                        judgment records it (machinery-75 attempt-002's
                        form); its :measurement-source becomes each
                        leg's :source
     :occurrence        the close's occurrence identity
     :reviewed-revision the artifact sha the close pinned
     :revision-pair     optional recorded {:before :after}
     :truth-by-token    optional map token -> adjudication input

   A :status :compared receipt yields one pair per :tokens row. Any
   other status yields a single typed-absence pair (token nil): the
   machinery-75 attempt-002 case, where :comparison-not-supplied must
   remain a typed absence and never a measured false."
  [{:keys [comparison occurrence reviewed-revision revision-pair truth-by-token]}]
  (if (= :compared (:status comparison))
    (mapv (fn [row]
            (build-pair {:occurrence occurrence
                         :token (:token row)
                         :token-row row
                         :measurement-source (:measurement-source comparison)
                         :revision-pair revision-pair
                         :reviewed-revision reviewed-revision
                         :truth (get truth-by-token (:token row))}))
          (:tokens comparison))
    [(build-pair {:occurrence occurrence
                  :token nil
                  :token-row nil
                  :comparison-reason (or (:reason comparison)
                                         :comparison-not-supplied)
                  :reviewed-revision reviewed-revision
                  :revision-pair revision-pair
                  :truth nil})]))

(defn pairs-from-kernel-example
  "One pair per observed token of the close's kernel example
   ([:payload :judgment :kernel-example]), in canonical token order:
   every token of the admitted projection, including the non-wanted
   ones. When the projection is not admitted, the wanted :tokens each
   yield a typed-missing pair carrying the projection's own :kind; a nil
   carrier (machinery-75 attempt-002) yields one pair with
   :carrier-not-recorded and no token -- never a guessed token or
   boolean. The comparison's rows are copies of these; a caller must not
   concatenate both into one population."
  [{:keys [kernel-example occurrence reviewed-revision truth-by-token]}]
  (let [projection (:observation-projection kernel-example)
        tokens (if (= :admitted (:status projection))
                 (keys (:observations projection))
                 (map :token (:tokens kernel-example)))]
    (mapv (fn [token]
            (build-pair {:occurrence occurrence :token token
                         :kernel-example (or kernel-example (missing :carrier-not-recorded))
                         :reviewed-revision reviewed-revision
                         :truth (get truth-by-token token)}))
          (if (seq tokens) (sort-by canonical-edn tokens) [nil]))))

;; ---------------------------------------------------------------------------
;; Structural validator. pair-ok? states the OBS-D invariants over a
;; built pair so tests can kill deliberately wrong builders: a builder
;; that coerces missing to false, that admits a same-source pair as
;; estimable, that lets a learning-trial wrapper through as a leg, that
;; cites a kernel row without its raw-file source, or that hashes the
;; record instead of the extracted value, produces maps that fail these
;; predicates.
;; ---------------------------------------------------------------------------

(defn pair-ok?
  "Structural invariants of a lawful pair:
   - :estimable? is true IFF both legs are booleans;
   - a refused pair carries a refusal :kind and is never estimable;
   - neither leg is or embeds an accepted-increment verdict;
   - neither leg is a learning-trial receipt or trial wrapper;
   - a measured kernel-example leg carries its raw-file :source
     {:path :sha256};
   - every pair's :pair-sha256 equals (pair-digest pair)."
  [pair]
  (let [obs (:observation pair)
        obs-bool (boolean? (:observed obs))
        tr-bool (boolean? (get-in pair [:truth :truth]))
        refused (= :refused (:status pair))]
    (and (map? pair)
         (= schema (:schema pair))
         (= (and obs-bool tr-bool (not refused))
            (true? (:estimable? pair)))
         (or (not refused) (keyword? (:kind pair)))
         (not (verdict-shaped? obs))
         (not (verdict-shaped? (:truth pair)))
         (not= :accepted-increment (get-in pair [:truth :truth-source]))
         (not (learning-trial-carrier? obs))
         (not (learning-trial-carrier? (:truth pair)))
         (or (not (and obs-bool (= :kernel-example (:observation-source obs))))
             (source-ref? (:source obs)))
         (= (:pair-sha256 pair) (pair-digest pair)))))
