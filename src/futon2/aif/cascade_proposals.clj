(ns futon2.aif.cascade-proposals
  "Evidence-only candidate supply. Never infer applicability or token production
   from signature prose. An agent authors a complete cascade-source-v1 declaration
   through cascade-sources/load-declared; proposals never populate its executable
   :candidates or :interpretations maps. Retrieval is explicit, outside the tick."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.interpretation-request :as request]
            [futon2.aif.mission-registry :as registry]
            [futon2.aif.repair-proposals :as repairs]
            [futon2.aif.repair-obligation :as repair])
  (:import [java.nio.file Files StandardOpenOption]
           [java.util UUID]))

(def default-dir (str (io/file registry/default-code-root "futon2" "data" "wm-cascade-proposals")))

(defn- decline [target reason details]
  {:target target :stage :proposal-supply :reason reason :missing-evidence details})

(defn retrieval-proposals
  "Only retrieved identities backed by captured pattern bytes become proposals.
   Ranking and similarity remain evidence, never applicability."
  [r]
  (let [sources (:sources r)
        pattern-sources (into {}
                              (keep (fn [s]
                                      (when-let [[_ id] (re-find #"/library/(.+)\.flexiarg$" (:path s))]
                                        [id s]))) sources)
        rows (for [run (get-in r [:retrieval :runs])
                   :when (empty? (:failures run))
                   candidate (:candidates run)]
               (assoc candidate :retriever (:retriever run)))
        groups (sort-by key (group-by :pattern rows))
        target (get-in r [:target :id])]
    {:proposals
     (vec (for [[pattern hits] groups :let [pin (get pattern-sources pattern)] :when pin]
            (let [trail {:target (:target r) :pattern-source pin
                         :retrieval (mapv #(select-keys % [:retriever :rank :retriever-rank :raw]) hits)}]
              {:schema :wm/cascade-proposal-v1 :status :proposed :origin :retrieval-proposed
               :target target :pattern pattern :evidence trail
               :proposal-id (evidence/value-digest trail)})))
     :declines
     (vec (concat
           (for [[pattern _] groups :when (nil? (get pattern-sources pattern))]
             (decline target :proposal-pattern-source-missing {:pattern pattern}))
           (when (empty? groups)
             [(decline target :no-evidenced-proposal {:retrieval :no-pinned-pattern-candidates})])))}))

(defn generate-retrieval!
  "Persist a request and immutable byte companions in a new proposal directory.
   Missing evidence is retained as a decline. No agent is invoked or impersonated."
  ([target kind dir] (generate-retrieval! target kind dir {}))
  ([target kind dir options]
   (let [root (io/file dir (str (UUID/randomUUID)))
         _ (.mkdirs root)
         record (try
                  (let [r (request/prepare-proposal! target kind root options)]
                    (merge {:schema :wm/cascade-proposal-bundle-v1 :target target :request r}
                           (retrieval-proposals r)))
                  (catch clojure.lang.ExceptionInfo e
                    (if-let [reason (:interpretation/refusal (ex-data e))]
                      {:schema :wm/cascade-proposal-bundle-v1 :target target
                       :request (:request (ex-data e)) :proposals []
                       :declines [(decline target reason (dissoc (ex-data e) :request))]}
                      (throw e))))
         file (io/file root "proposal.edn")]
     (Files/write (.toPath file) (.getBytes (pr-str record) "UTF-8")
                  (into-array StandardOpenOption [StandardOpenOption/CREATE_NEW StandardOpenOption/WRITE]))
     {:path (.getCanonicalPath file) :record record})))

(defn read-bundle
  "Check all captured bytes before retaining proposal evidence. Snapshot damage
   yields a typed decline, never promotion or re-reading replacement source bytes."
  [path]
  (let [f (io/file path)
        record (edn/read-string (slurp f))
        target (:target record)
        r (:request record)
        problems (vec
                  (concat
                   (when-not (= :wm/cascade-proposal-bundle-v1 (:schema record)) [:bundle-schema])
                   (when (and r (or (not= :wm/cascade-proposal-request-v1 (:schema r))
                                   (not= target (get-in r [:target :id]))
                                   (contains? r :identity) (get-in r [:target :action])))
                     [:request-identity])
                   (for [s (:sources r)
                         :let [name (:file s)
                               safe? (and (string? name) (not (re-find #"[/\\\\]" name))
                                          (not (#{"." ".."} name)))
                               file (when safe? (io/file (.getParentFile f) "evidence" name))]
                         :when (not (and file (.isFile file)
                                         (= (:sha256 s) (evidence/sha256 (Files/readAllBytes (.toPath file))))))]
                     {:source (:id s) :reason :snapshot-missing-or-changed})))]
    (if (seq problems)
      {:target target :proposals [] :declines [(decline target :proposal-evidence-invalid problems)]}
      (if r
        ;; Re-derive proposals from retained retrieval, not editable proposal flags.
        (merge {:target target} (if (some #(empty? (:failures %)) (get-in r [:retrieval :runs]))
                                 (retrieval-proposals r)
                                 {:proposals [] :declines (:declines record)}))
        {:target target :proposals [] :declines (:declines record)}))))

(defn load-proposals
  ([] (load-proposals default-dir))
  ([dir]
   (let [files (->> (file-seq (io/file dir))
                    (filter #(and (.isFile %) (= "proposal.edn" (.getName %))))
                    (sort-by str))
         records (mapv read-bundle files)]
     {:proposals (vec (mapcat :proposals records))
      :declines (vec (mapcat :declines records))
      :files (mapv str files)})))

(defn load-supply
  "Combine retained retrieval evidence with a fresh open-repair-store read."
  [{:keys [proposal-dir repair-root]}]
  (let [retrieved (load-proposals (or proposal-dir default-dir))
        findings (repairs/supply (or repair-root repair/default-root))]
    (-> retrieved
        (update :proposals into (:proposals findings))
        (update :declines into (:declines findings))
        (assoc :repair-scan (:repair-scan findings)))))

(defn -main [target kind dir]
  (try
    (println (pr-str (generate-retrieval! target (keyword kind) (or dir default-dir))))
    (finally (shutdown-agents))))

(defn record-supply
  "Attach evidence and pending-admission declines to the existing assembly record.
   A receipt may cite :proposal-id; that join checks target, pattern and byte pin.
   Execution still uses ONLY the existing declaration loader and admission gate."
  [assembled sources supply]
  (let [by-id (into {} (map (juxt :proposal-id identity)) (:proposals supply))
        ;; PROOF-wm-works ⟨1⟩5 (claude-5 handoff, per Joe's 6714b3ac ruling):
        ;; the withhold protects against the CONSTRUCTOR being unable to
        ;; build a repair proposal while it cannot observe closure. A target
        ;; with an ADMITTED DECLARED cascade source in this assembly — its
        ;; problem came from cs/load-declared with its own interpretations
        ;; and candidates — has that construction supplied by declaration,
        ;; so there is nothing left to protect against: it is NOT withheld,
        ;; and is recorded as supplied-by-declaration with its source path.
        ;; Generated :repair-finding-proposed proposals and repair targets
        ;; with no declared source keep the unchanged withhold.
        declared-source-targets (set (for [[t _] (:interpretations sources)
                                           :when (seq (get-in sources [:candidates t]))]
                                      t))
        generated-repair-targets (set (map :target
                                          (filter #(= :repair-finding-proposed (:origin %))
                                                  (:proposals supply))))
        scan-repair-targets (set (map repairs/target-id
                                      (get-in supply [:repair-scan :open-finding-ids])))
        repair-targets (set (concat scan-repair-targets generated-repair-targets))
        withheld (filter #(and (contains? repair-targets (:target %))
                               (not (contains? declared-source-targets (:target %))))
                         (:problems assembled))
        supplied-by-declaration (filter #(and (contains? repair-targets (:target %))
                                              (contains? declared-source-targets (:target %)))
                                        (:problems assembled))
        repair-declines (mapv #(decline (:target %) :repair-closure-observation-unavailable
                                       [:produced-resolution-evidence]) withheld)
        admissions
        (vec (for [[target interp] (:interpretations sources)
                   [pattern receipt] (:receipts interp)
                   :when (and (:proposal-id receipt) (not (contains? repair-targets target)))]
               (let [proposal (get by-id (:proposal-id receipt))]
                 (when-not (and proposal (= target (:target proposal))
                                (= (if (keyword? pattern) (subs (str pattern) 1) (str pattern)) (:pattern proposal))
                                (= (get-in receipt [:source :sha256])
                                   (get-in proposal [:evidence :pattern-source :sha256])))
                   (throw (ex-info "Admission references mismatched proposal evidence"
                                   {:proposal/refusal :admission-evidence-mismatch
                                    :target target :pattern pattern :proposal-id (:proposal-id receipt)})))
                 {:proposal-id (:proposal-id receipt) :target target :pattern pattern
                  :receipt receipt :authority :agent-authored-declaration})))
        admitted-ids (set (map :proposal-id admissions))
        proposed-targets (set (map :target (:proposals supply)))
        declared-targets (set (for [[t i] (:interpretations sources) :when (seq (:receipts i))] t))
        no-evidence (for [t (keys (:wants sources))
                          :when (and (not (contains? declared-targets t))
                                     (not (contains? proposed-targets t)))]
                      (decline t :no-evidenced-proposal [:pinned-proposal :agent-interpretation]))
        pending (for [p (:proposals supply) :when (not (contains? admitted-ids (:proposal-id p)))]
                  (assoc (decline (:target p) :proposal-awaiting-agent-admission
                                  [:interpretation-receipt :guard :produces :locators])
                         :proposal-id (:proposal-id p)))]
    (-> assembled
        (cond-> (seq withheld)
          (assoc :problems (vec (remove #(and (contains? repair-targets (:target %))
                                              (not (contains? declared-source-targets (:target %))))
                                        (:problems assembled)))))
        (update :refusals #(into (vec %)
                                (map (fn [p] {:target (:target p)
                                              :kind :universe-not-admitted
                                              :reason :repair-closure-observation-unavailable
                                              :missing :locators}) withheld)))
        (update :dropped-candidates into repair-declines)
        ;; the distinction a reader needs: declared-source repair targets are
        ;; SUPPLIED, not withheld, with the rule that applied
        (assoc :repair-withhold-distinction
               {:supplied-by-declaration
                (vec (for [p supplied-by-declaration]
                       {:target (:target p)
                        :rule :declared-source-supersedes-withhold
                        :source-paths (vec (for [[_ rec] (get-in sources
                                                                 [:interpretations (:target p) :receipts])]
                                             (get-in rec [:source :path])))}))
                :withheld-generated-or-sourceless
                (vec (for [p withheld] {:target (:target p)
                                        :rule :repair-closure-observation-unavailable}))})
        (assoc :proposal-supply (assoc supply :admissions admissions
                                      :exact-assurance {:status :unavailable
                                                        :reason :typed-target-link-evidence-missing}))
        (update :dropped-candidates #(vec (concat % (:declines supply) no-evidence pending))))))
