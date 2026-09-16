(ns futon2.aif.shadow-cascade-g
  "Shadow cascade G for receipted construction (P11 step 1b-ii, Joe-approved).

  A SHADOW scorer: it computes G over cascades with the approved model
  (futon2.aif.cascade-model-manifest) and records it beside construction, but
  no selection, admission or shown-order decision consumes it. Every parameter
  that the model does not supply from source is DECLARED here (zero
  adjudication rates, lam = mu = 1, empty evidence/zeroed, horizon 3,
  documented-default theta from with-pattern-theta) and is recorded in
  :inputs, never silent.

  Aligned quantity: Lean DarkTower.WarMachine.PolicyHorizon.horizonEFE
  (G(pi) = sum_tau [KL(Q(o_tau|pi) || C_tau) + sum_s q_tau(s) H(A(.|s))]; the
  sparse form at zero rates is exact, see cascade-model-manifest/horizon-g-sparse)
  and DarkTower.WarMachine.OutcomeRiskKL.outcomeRisk with
  ZeroPreferenceExclusion (positive Q on zero C gives :infinite risk, never a
  smoothed finite value). The Clojure locus for the correspondence claim is
  this file, shadow-cascade-g, plus horizon-g-sparse.

  Pure: no JVM state, no writes; all IO is reading the mission document and
  pattern library files named by the record. Every failure is a typed
  {:status :missing :kind ...} outcome per cascade arm; an exception inside an
  arm's computation is caught and typed :shadow-scorer-error so a broken input
  can never break construction."
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [futon2.aif.cascade-model-manifest :as manifest]))

(def ^:private repo-root-prefixes ["" "/home/joe/code/"])

(defn default-opts []
  {:library-root "/home/joe/code/futon3/library"
   :horizon 3
   :lam 1
   :mu 1})

(defn- readable-file
  [path]
  (when (string? path)
    (some (fn [prefix]
            (let [f (io/file (str prefix path))]
              (when (.isFile f) f)))
          repo-root-prefixes)))

(defn mission-document
  "The record's mission document: {:id :path :text} or nil. Derived from the
  target's pinned citation source path (falling back to the target source and
  then to the identity occurrence target under the missions roots). A record
  whose mission path cannot be derived yields nil — callers refuse, they never
  guess."
  [record]
  (let [sources (into {} (map (juxt :id identity)) (:sources record))
        cite-path (get-in sources [(get-in record [:target :citations 0 :source]) :path])
        target-path (get-in sources [(get-in record [:target :source]) :path])
        id (or (get-in record [:target :id])
               (get-in record [:identity :occurrence :action/value :target]))
        guessed (some-> id (str) (->> (map (fn [root] (str root "/" id ".md")))
                                      (some readable-file)))
        f (or (readable-file cite-path) (readable-file target-path) guessed)]
    (when (and f id)
      (try {:id (str id) :path (.getCanonicalPath f) :text (slurp f)}
           (catch Exception _ nil)))))

(defn- pattern-file
  [library-root id]
  (readable-file (str library-root "/" (subs (str id) 1) ".flexiarg")))

(defn- interpret
  [opts id]
  (if-let [f (pattern-file (:library-root opts) id)]
    (let [path (.getCanonicalPath f)
          text (try (slurp f) (catch Exception _ nil))]
      (cond
        (nil? text) {:status :missing :kind :missing-pattern-interpretation
                     :pattern id :reason :library-file-unreadable}
        (:interpret-fn opts) ((:interpret-fn opts) id path text)
        :else (try
                (manifest/interpret-pattern id path text)
                (catch Exception _
                  {:status :missing :kind :missing-pattern-interpretation
                   :pattern id :reason :interpretation-failed}))))
    {:status :missing :kind :missing-pattern-interpretation
     :pattern id :reason :library-file-missing}))

(defn- pattern-tokens
  "Every consumed (guard present), forbidden (guard absent) and produced token
  of an interpreted pattern — the pattern's whole move interface."
  [p]
  (when (= :interpreted (get-in p [:guard :status]))
    (set/union (into #{} (mapcat :present) (get-in p [:guard :clauses]))
               (into #{} (mapcat :absent) (get-in p [:guard :clauses]))
               (set (:produces p)))))

(defn up-closure
  "Transitive stands-on up-closure of `carrier` (the same closure
  cascade-policy/organise applies for the :stands-on-up-closure temperament)."
  [carrier stands-on]
  (let [succ (reduce (fn [m [a b]] (update m a (fnil conj #{}) b)) {} stands-on)]
    (loop [frontier (set carrier) acc (set carrier)]
      (let [nxt (set/difference (reduce set/union #{} (map #(get succ % #{}) frontier)) acc)]
        (if (empty? nxt) acc (recur nxt (set/union acc nxt)))))))

(defn- arm-patterns
  "The cascade's interpreted patterns in its :precedence order (nodes absent
  from the precedence vector follow in canonical order). Returns either the
  pattern vector or a typed :missing-pattern-interpretation refusal."
  [interpretations cascade]
  (let [nodes (set (:nodes cascade))
        ordered (concat (:precedence cascade) (sort (set/difference nodes (set (:precedence cascade)))))
        ps (mapv interpretations (filter nodes ordered))
        bad (first (filter #(or (= :missing (:status %))
                                (= :missing (get-in % [:guard :status])))
                           ps))]
    (if bad
      {:status :missing :kind :missing-pattern-interpretation
       :pattern (or (:pattern bad) (:id bad))
       :reason (or (:reason bad) :guard-not-interpretable)}
      ps)))

(defn- compute-arm
  "One arm's G under the shared, already-interpreted inputs. rates are the
  DECLARED all-zero adjudication rates over the common universe; q0 is the
  observed belief at the target's have tokens."
  [{:keys [target universe rates spec patterns horizon]}]
  (let [have (:have target)
        q0 (manifest/observed-belief have)
        precedence-fn (constantly (vec patterns))
        g (manifest/horizon-g-sparse {:rates rates :q0 q0 :precedence-fn precedence-fn
                                      :horizon horizon :spec spec :universe universe})]
    (cond
      (and (map? g) (= :missing (:status g))) g
      (= g :infinite) {:status :computed :g :infinite}
      :else {:status :computed :g (double g)})))

(defn shadow-cascade-g
  "record cascades opts -> {arm {:status ...}}.

  CASCades is a map of arm keyword (e.g. :before/:after) to cascade
  {:nodes #{...} :precedence [...]}. OPTS: :library-root, :horizon, :lam, :mu
  (all defaulted), plus the test seam :interpret-fn.

  ONE common universe for every arm — have ∪ want ∪ all move-interface tokens
  of every arm's patterns — because a per-arm universe shifts G by T·k·ln2 and
  would fake a difference between candidates. Each arm returns
  {:status :computed :g <double or :infinite> :inputs {...}} with every
  declared parameter recorded, or a typed {:status :missing :kind ...}. The
  per-arm computation is exception-safe: :shadow-scorer-error."
  ([record cascades] (shadow-cascade-g record cascades (default-opts)))
  ([record cascades opts]
   (let [opts (merge (default-opts) opts)
         mission (mission-document record)
         typed (fn [kind] (into {} (map (fn [arm] [arm {:status :missing :kind kind}]))
                               (keys cascades)))]
     (if-not mission
       (typed :mission-document-not-derivable)
       (let [target (manifest/extract-target (:id mission) (:path mission) (:text mission))]
         (if (or (not (seq (:have target))) (not (seq (:want target))))
           (typed :missing-source-bound-have)
           (let [all-nodes (apply set/union (map #(set (:nodes %)) (vals cascades)))
                 ;; Shared inputs are a delay so a failure inside interpretation
                 ;; or preference validation is caught by the per-arm try and
                 ;; typed :shadow-scorer-error instead of escaping the function.
                 shared (delay
                          (let [interpretations (into {} (map (fn [id] [id (interpret opts id)])) all-nodes)
                                interpreted (filter #(= :interpreted (get-in % [:guard :status]))
                                                    (vals interpretations))
                                universe (apply set/union (:have target) (:want target)
                                                (map pattern-tokens interpreted))
                                spec-raw {:want (:want target) :evidence #{}
                                          :lam (:lam opts) :mu (:mu opts) :zeroed #{}}
                                ;; Validation happens inside horizon-g-sparse's
                                ;; log-preference-fn (same want/lam/mu refusals,
                                ;; counting zeroed check) rather than
                                ;; preference-spec, whose powerset zeroed_proper
                                ;; check explodes on real mission-scale universes.
                                spec spec-raw
                                rates (into {} (map (fn [t] [t {:false-neg 0 :false-pos 0}])) universe)]
                            {:interpretations interpretations :universe universe
                             :spec spec :rates rates}))]
             (into {}
                   (map (fn [[arm cascade]]
                          [arm (try
                                 (let [{:keys [interpretations universe spec rates]} @shared
                                       ps (arm-patterns interpretations cascade)]
                                   (if (and (map? ps) (= :missing (:status ps)))
                                     ps
                                     (assoc (compute-arm {:target target :universe universe :rates rates
                                                          :spec spec :patterns ps :horizon (:horizon opts)})
                                            :inputs {:mission {:id (:id mission) :path (:path mission)}
                                                     :target-source (:source target)
                                                     :have (vec (:have target)) :want (vec (:want target))
                                                     :universe (vec (sort universe))
                                                     :rates :declared-zero
                                                     :spec {:want (vec (sort (:want target))) :evidence #{}
                                                              :lam (:lam opts) :mu (:mu opts) :zeroed #{}}
                                                       :horizon (:horizon opts)
                                                       :precedence (mapv :id ps)
                                                       :theta-source :documented-default
                                                       :authority :documented-interpretation
                                                       :role :shadow-not-consumed})))
                                 (catch Exception e
                                   {:status :missing :kind :shadow-scorer-error
                                    :message (str (.getMessage e))}))]))
                   cascades))))))))
