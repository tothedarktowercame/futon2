(ns futon2.aif.token-outcome-pair
  "OBS-P (PROOF-2 strategy row 18): the occurrence-bound measurement pair
   carrier :wm/token-outcome-pair-v1, specified by OBS-D (proof2/packets/
   OBS-D.md §4, commit 3277e9e6).

   For one occurrence and one token, a pair joins the OBSERVATION leg
   (what the locator/check reported) with the TRUTH leg (whether the
   token was really produced, adjudicated through a channel independent
   of that locator). Each leg is a boolean or a typed absence with a
   reason -- never a substituted value. A pair is estimable (usable in a
   false-positive / false-negative denominator) only when BOTH legs are
   booleans; the ineligibility is a field on the pair, not a convention.

   Two refusals are checks in code, not instructions (the OBS-D
   falsifiers):
   - a typed-missing observation can never surface as a measured
     false (or true): the missing form propagates as {:status :missing};
   - an accepted-increment verdict is refused from BOTH legs: it is a
     verdict built FROM locator observations, so it can be neither the
     observation (relabelled verdict) nor the truth (the thing those
     observations would be measured against), and any truth leg whose
     evidence is the same verdict source as the observation leg refuses
     the whole pair."
  (:require [clojure.string :as str]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def schema :wm/token-outcome-pair-v1)

(def occurrence-keys
  [:run/id :cohort/id :attempt/id :transition/id :action/id :action/value-sha256])

(defn- missing [reason] {:status :missing :reason reason})

(defn canonical-edn
  "CERT-S §3 bytes as text: preserve collection types, recursively sort
   canonical keys/elements, exact ratios, hex doubles, no printer metadata."
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
  "OBS-D Revision 2: extracted legs, token, occurrence identity and revisions.
   Eligibility, schema, full action payload and embedded pair hash are excluded."
  [pair]
  {:occurrence (into {} (map (fn [k] [k (or (get (:occurrence pair) k)
                                           (missing :identity-not-recorded))])
                            occurrence-keys))
   :token (:token pair)
   :revision-pair (:revision-pair pair)
   :observation (:observation pair)
   :truth (:truth pair)})

(defn pair-digest [pair]
  (str "sha256:" (evidence/sha256 (.getBytes (canonical-edn (pair-value pair)) "UTF-8"))))

(defn- learning-trial-source? [x]
  (and (map? x)
       (or (contains? #{:wm/learning-trial-receipt-v1 :wm/learning-trial-receipt-v2}
                      (:schema x))
           (contains? x :trial-grain)
           (= :learning-trial-receipt (:observation-source x))
           (= :learning-trial-receipt (:truth-source x)))))

;; ---------------------------------------------------------------------------
;; Verdict detection. An accepted-increment map carries :accepted?; a
;; comparison token row's verdict is fine as a VERDICT but must not be
;; sourced into a leg as if it were a fresh observation or truth.
;; ---------------------------------------------------------------------------

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
   token_outcome.clj:66-72 already draws."
  [token-row]
  (let [observed (:observed token-row)
        result (get-in token-row [:measurement :result])]
    (cond
      (learning-trial-source? token-row)
      {:status :refused :reason :learning-trial-receipt-not-a-leg}

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
   A verdict-shaped or accepted-increment-sourced input is REFUSED, not
   defaulted: the refusal records the attempted relabelling."
  [truth]
  (cond
    (learning-trial-source? truth)
    {:status :refused :reason :learning-trial-receipt-not-a-leg}

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

;; ---------------------------------------------------------------------------
;; Same-verdict-source refusal. When both legs carry full evidence maps
;; and those maps are EQUAL, both legs report the same read of the same
;; artifact at the same revision: the pair would have correlation 1 by
;; construction (OBS-D §5, second bad case). The whole pair refuses.
;; ---------------------------------------------------------------------------

(defn- same-verdict-source?
  "True when both resolved legs carry evidence maps that are identical,
   or when the truth leg's declared source names the observation's own
   check act. Only meaningful when both legs resolved to values; a
   typed-absent leg has nothing to correlate with."
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

(defn kernel-observation-leg
  "Extract one already admitted locator observation, preserving its raw-file
   source reference. Never promote the example's prediction or disposition.
   This pure extraction does not re-run execution verification or read a file."
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
                 (learning-trial-source? example) :learning-trial-receipt-not-a-leg
                 (not= :wm/aligned-kernel-example-v1 (:schema example)) :kernel-example-schema-mismatch
                 (not= :recorded (:status example)) (or (:kind example) :kernel-example-not-recorded)
                 (not= occurrence (:occurrence example)) :occurrence-mismatch
                 (not= :admitted (:status projection)) (or (:kind projection) :observation-not-admitted)
                 (not= occurrence (:occurrence projection)) :occurrence-mismatch
                 (not (and (string? (:path source))
                           (string? (:sha256 source))
                           (re-matches #"[0-9a-f]{64}" (:sha256 source)))) :observation-source-not-recorded
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

(defn build-pair
  "One pair for `token` of one occurrence. Inputs:

     :occurrence        identity map (must carry :run/id and :attempt/id)
     :token             the qualified token, or nil when the comparison
                        itself was absent and no token is knowable
     :token-row         the comparison :tokens entry for this token, or
                        nil when the comparison supplied no row
     :comparison-reason reason keyword when the comparison itself was
                        absent/refused (e.g. :comparison-not-supplied)
     :reviewed-revision the after-revision sha both legs pin to
     :truth             adjudication input for truth-leg, or nil

   Returns the pair map, or a refusal {:schema … :status :refused :kind
   :same-verdict-source} when both legs would report one act. Every pair
   carries a canonical extracted-value :pair-sha256 (OBS-D Revision 2),
   distinct from raw record identity. :kernel-example selects the admitted
   projection row for :token; :revision-pair may supply the recorded pair."
  [{:keys [occurrence token token-row comparison-reason reviewed-revision
           truth kernel-example revision-pair measurement-source]}]
  (let [revision-pair (or revision-pair
                          (get-in kernel-example [:observation-projection :revision-pair])
                          {:before (missing :before-revision-not-recorded)
                           :after (or reviewed-revision (missing :after-revision-not-recorded))})
        revision-pair (reduce (fn [r [k reason]]
                                (update r k #(or % (missing reason))))
                              revision-pair
                              [[:before :before-revision-not-recorded]
                               [:after :after-revision-not-recorded]])
        reviewed-revision (or reviewed-revision
                              (when (string? (:after revision-pair)) (:after revision-pair)))
        base {:schema schema :schema-version 2
              :pair-hash-domain :wm/token-outcome-pair-value-v2
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
        pair (merge base {:observation obs-leg :truth tr-leg}
                    (estimable-fields obs-leg tr-leg))]
    (assoc (if (and (boolean? (:observed obs-leg)) (boolean? (:truth tr-leg))
             (same-verdict-source? obs-leg tr-leg))
      (assoc base :status :refused :kind :same-verdict-source
             :observation obs-leg :truth tr-leg
             :estimable? false
             :ineligibility-reasons [:same-verdict-source])
      pair)
           :pair-sha256 (pair-digest pair))))

(defn pairs-from-comparison
  "Runner adapter. Derive the pair vector for one close from a comparison
   receipt (the value retained under :token-outcome-comparison):

     :comparison        the comparison receipt (any :status), or a bare
                        {:status :absent :reason <kw>} map as the
                        judgment records it (machinery-75 attempt-002's
                        form)
     :occurrence        the close's occurrence identity
     :reviewed-revision the artifact sha the close pinned
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
  "All observed tokens in the admitted projection, including non-wanted
   tokens. Unavailable projections retain wanted-token missing rows; an
   absent carrier yields a typed absence, never a guessed token/boolean.
   Callers must not concatenate comparison duplicates into an A population."
  [{:keys [kernel-example occurrence reviewed-revision truth-by-token]}]
  (let [projection (:observation-projection kernel-example)
        tokens (if (= :admitted (:status projection))
                 (keys (:observations projection))
                 (map :token (:tokens kernel-example)))]
    (mapv (fn [token]
            (build-pair {:occurrence occurrence :token token
                         :kernel-example (or kernel-example (missing :kernel-example-not-recorded))
                         :reviewed-revision reviewed-revision
                         :truth (get truth-by-token token)}))
          (if (seq tokens) (sort-by canonical-edn tokens) [nil]))))

;; ---------------------------------------------------------------------------
;; Structural validator. pair-ok? states the OBS-D invariants over a
;; built pair so tests can kill deliberately wrong builders: a builder
;; that coerces missing to false, or that admits a same-source pair as
;; estimable, produces maps that fail these predicates.
;; ---------------------------------------------------------------------------

(defn pair-ok?
  "Structural invariants of a lawful pair:
   - :estimable? is true IFF both legs are booleans;
   - a refused pair carries a refusal :kind and is never estimable;
   - neither leg is or embeds an accepted-increment verdict;
   - every pair's :pair-sha256 matches its canonical extracted value."
  [pair]
  (let [obs-bool (boolean? (get-in pair [:observation :observed]))
        tr-bool (boolean? (get-in pair [:truth :truth]))
        refused (= :refused (:status pair))]
    (and (map? pair)
         (= schema (:schema pair))
         (= (and obs-bool tr-bool (not refused))
            (true? (:estimable? pair)))
         (or (not refused) (keyword? (:kind pair)))
         (not (verdict-shaped? (:observation pair)))
         (not (verdict-shaped? (:truth pair)))
         (not= :accepted-increment (get-in pair [:truth :truth-source]))
         (not (learning-trial-source? (:observation pair)))
         (not (learning-trial-source? (:truth pair)))
         (= (:pair-sha256 pair) (pair-digest pair)))))
