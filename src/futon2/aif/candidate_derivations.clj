(ns futon2.aif.candidate-derivations
  "B4 slice 2b: the P₀ carrier. For every candidate the scorer received,
  emit [:decision :selection-certificate :candidate-derivations <id>] with
  the THEOREM-draft P₀ field list, TRUTHFULLY for today's candidates, which
  are declared files with no authoring chain: their construction receipts
  are :hand-admitted, so P₀ fails on provenance rather than on absence, and
  the record says what happened. Every field with no truthful value today
  is a typed absence map, never nil and never a placeholder.

  Provenance verdicts come from futon2.aif.cascade-equivalence/admissible-
  provenance? and normalization sha256s from its normalize; transition rows
  are the Lean-bound manifest/cascade-kernel at s₀. Nothing here reads the
  substrate, fires clicks, or writes data/."
  (:require [futon2.aif.cascade-equivalence :as ce]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.cascade-sources :as cascade-sources]))

(def p0-fields
  "Every field P₀ requires per candidate entry, checked by the tests."
  [:source-kind :source-id :source-revision :source-content-sha256
   :discovered-at :interpretation :construction :review-publication
   :admission :acceptance :locators :scope :normalized-cascade-sha256
   :transition-rows])

(defn- refusal? [x] (and (map? x) (contains? x :status)))

(defn s0-of
  "The recorded initial state from the certificate's token-belief-stage:
  the point mass of :continuation-belief when it is one ({state 1}), else
  nil (a mixture has no single kernel row; the entry then carries the typed
  absence :initial-state-not-a-point-mass)."
  [token-belief-stage]
  (let [cb (:continuation-belief token-belief-stage)]
    (when (and (map? cb) (= 1 (count cb))
               (= 1 (val (first cb))))
      (key (first cb)))))

(defn- declared-source
  "The declared cascade-source FILE for a target, from load-declared's
  :files [{:path :sha256 :target}] when the sources map is supplied. The
  path and sha ARE retained there — but the decision scope does not hold
  the sources map, so the default here is the typed absence, never an
  invented sha (B4 finding, 2026-09-24)."
  [target {:keys [sources]}]
  (if-let [f (some #(when (= target (:target %)) %) (:files sources))]
    {:path (:path f) :sha256 (:sha256 f)}
    {:status :missing :reason :declared-source-sha-not-retained :target target}))

(defn- transition-rows
  "cascadeKernel of the precedence at s₀ (W₀ item 5: the runtime row equals
  the Lean row). Typed absence when s₀ is missing or not a point mass, or
  the typed refusal when the precedence has an uninterpreted pattern."
  [candidate s0]
  (if (nil? s0)
    {:status :missing :reason :initial-state-not-a-point-mass}
    (let [k (m/cascade-kernel (get-in candidate [:id :precedence]) s0)]
      (if (refusal? k) k k))))

(defn- entry
  "One candidate's P₀ entry, built from the candidate map itself (the shape
  at [:decision :selection-certificate :candidates], whose payload sits
  under :id and which carries :construction-receipt and :observation-locators)
  plus whatever the caller can truthfully supply."
  [candidate s0 opts]
  (let [id (get-in candidate [:id :id])
        target (get-in candidate [:id :target])
        dsrc (declared-source target opts)
        base {:source-kind :declared-file
              :source-id id
              :source-revision {:status :missing
                                :reason :declared-source-has-no-revision}
              :source-content-sha256 (if (:sha256 dsrc)
                                       (:sha256 dsrc)
                                       dsrc)
              :discovered-at {:status :missing
                              :reason :discovery-time-not-recorded}
              :interpretation {:kind :declared}
              :construction
              (merge {:kind :hand-admitted
                      :source (if (:sha256 dsrc)
                                (assoc (select-keys dsrc [:path]) :kind :declared-file
                                       :sha256 (:sha256 dsrc))
                                {:kind :declared-file :target target})}
                     ;; today's candidates already self-report their
                     ;; construction receipt; keep its words and its author
                     (:construction-receipt candidate))
              :review-publication {:status :missing
                                   :reason :no-review-publication-for-declared-candidates}
              :admission {:kind :declared-file-load :admitted-by :war-machine-judge}
              :acceptance (or (when-let [a (and (:sources opts)
                                                (cascade-sources/acceptance-of
                                                 target {:sources (:sources opts)}))]
                                ;; acceptance-of keys by the target's own want
                                ;; and locator and can return them even when
                                ;; :files has no entry for the target (nil
                                ;; provenance sha). Accept only a declaration
                                ;; whose declaring file actually resolves.
                                (when (get-in a [:provenance :source-sha256])
                                  a))
                              (if (:sources opts)
                                {:status :missing
                                 :reason :acceptance-source-file-not-found-for-target}
                                {:status :missing
                                 :reason :declared-acceptance-not-in-decision-scope}))
              :locators (or (:observation-locators candidate)
                            {:status :missing :reason :locators-not-carried-on-candidate})
              :scope {:status :missing :reason :feasible-scope-not-declared}
              :normalized-cascade-sha256
              (let [n (ce/normalize candidate)]
                (if (:status n) n (:normalized-cascade-sha256 n)))
              :transition-rows (transition-rows candidate s0)}
        e (merge base (get-in opts [:authored id]))
        v (ce/admissible-provenance? e)
        admitted? (:admissible v)]
    (cond-> e
      :always (assoc :status (if admitted? :admitted :inadmissible))
      (not admitted?) (assoc :offending-path (:path v)
                             :offending-kind (:kind v)))))

(defn derivations
  "The P₀ carrier map keyed by the exact candidate id used in :candidates.
  CANDIDATES is the list at [:decision :selection-certificate :candidates].
  S0 is the initial token state (a set), typically (s0-of token-belief-stage).
  OPTS: {:sources <load-declared map>} populates the declared-file sha for
  :source-content-sha256 and :construction/:source when the caller holds it;
  {:authored {id fields}} lets the B4 authoring lane (2b+) override fields
  with real chains — the merged entry is still judged by
  admissible-provenance?, never self-declared admitted. {:actions [action…]}
  (the selection's argmax actions) is checked against the candidate ids.

  Bijectivity (B4 condition 5): the key set must equal the candidates' id
  set and every action id must be one of them; otherwise the typed refusal
  {:status :refused :kind :candidate-id-mismatch …} — never a partial map."
  ([candidates s0] (derivations candidates s0 nil))
  ([candidates s0 opts]
   (let [cand-ids (mapv #(get-in % [:id :id]) candidates)]
     (if (or (some nil? cand-ids) (not (apply distinct? (vec cand-ids))))
       {:status :refused :kind :candidate-id-mismatch
        :reason :candidate-ids-absent-or-not-unique :ids cand-ids}
       (let [id-set (set cand-ids)
             action-ids (into #{} (keep #(get-in % [:id])) (:actions opts))]
         (if (not (every? id-set action-ids))
           {:status :refused :kind :candidate-id-mismatch
            :reason :action-id-not-in-candidates
            :unmatched (vec (sort (remove id-set action-ids)))}
           (into {} (map (fn [c] [(get-in c [:id :id]) (entry c s0 opts)]))
                 candidates)))))))
