(ns futon2.aif.cascade-shape-g
  "Score an arranged pattern cascade through the existing bounded scorer.

  This adapter is deliberately structural.  Each node produces its own done
  token. Directed non-overlap edges become the descent relation of the
  existing co-application kernel; an overlap edge gives both endpoints a
  shared token which both operators produce. Consequently equal node orders
  with different edges generate different transition distributions before
  cascade-observation-scoring/rank-cascade-actions evaluates them.

  Claude-1's modelling choices are the token reading above and the Jeffreys
  Beta(1/2,1/2) prior when the learning ledger has no trials.  The existing
  scorer remains the authority for rollout, step-indexed C, risk, ambiguity,
  and G.  Parameter information is computed with parameter-novelty's
  canonical Beta kernel and subtracted by the existing scorer."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.set :as cset]
            [clojure.string :as str]
            [futon2.aif.analysis-cascade :as analysis]
            [futon2.aif.target-reading-registry :as reading-registry]
            [futon2.aif.cascade-observation-scoring :as scorer]
            [futon2.aif.cascade-selection :as selection]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.matched-observation-evidence :as matched]
            [futon2.aif.retraction-cascade :as retraction-cascade]))

(defn- pattern-key [s]
  (keyword (str/replace (str s) #"^:" "")))

(defn- done-token [target pattern] [target :pattern-done pattern])
(defn- overlap-token [target a b]
  [target :overlap (vec (sort [(str a) (str b)]))])
(defn- normalize-edge [edge]
  {:from (or (:from edge) (:a edge))
   :to (or (:to edge) (:b edge))
   :kind (or (:kind edge) (:kind_used edge) (:kind-used edge) :precedes)})

(defn- node-occurrence-id [node]
  (let [pattern (or (:pattern node) (:id node) node)]
    (or (:occurrence-id node)
        (when (and (map? node) (contains? node :fragment-index))
          [pattern (:fragment-index node)])
        pattern)))

(defn- occurrence-arrangement [{:keys [nodes edges]}]
  (let [occurrences
        (mapv (fn [node]
                (let [pattern (or (:pattern node) (:id node) node)]
                  {:occurrence-id (node-occurrence-id node) :pattern pattern
                   :roles (vec (distinct (:roles node)))
                   :fragment-indices (vec (keep identity [(:fragment-index node)]))}))
              nodes)
        by-pattern-fragment
        (into {} (for [node nodes :when (and (map? node)
                                             (contains? node :fragment-index))]
                   [[(or (:pattern node) (:id node)) (:fragment-index node)]
                    (node-occurrence-id node)]))
        occurrence-ids (set (map :occurrence-id occurrences))
        endpoint (fn [edge side]
                   (let [pattern (get edge side)
                         fragment (get edge (keyword (str (name side) "-fragment")))]
                     (or (get by-pattern-fragment [pattern fragment])
                         (when (occurrence-ids pattern) pattern)
                         pattern)))
        occurrence-edges
        (->> edges
             (map #(merge % (normalize-edge %)))
             (map #(assoc % :from (endpoint % :from) :to (endpoint % :to)))
             distinct vec)]
    {:nodes occurrences :edges occurrence-edges
     :precedence (mapv :occurrence-id occurrences)}))

(defn- directed-cycle [nodes edges]
  (let [outgoing (group-by :from (remove #(= :overlap (:kind %)) edges))
        found (volatile! nil)
        state (atom {})]
    (letfn [(visit [node path]
              (when-not @found
                (case (get @state node)
                  :done nil
                  :visiting (let [start (.indexOf ^java.util.List path node)]
                              (vreset! found (conj (subvec path start) node)))
                  (do (swap! state assoc node :visiting)
                      (doseq [edge (get outgoing node)]
                        (visit (:to edge) (conj path node)))
                      (swap! state assoc node :done)))))]
      (doseq [node nodes] (visit node []))
      @found)))

(defn- topo-order [nodes edges fallback]
  (let [ids (set nodes)
        directed (remove #(= :overlap (:kind %)) edges)
        incoming (frequencies (map :to directed))
        outgoing (group-by :from directed)
        rank (zipmap fallback (range))]
    (loop [left ids in incoming out []]
      (if (empty? left)
        out
        (let [ready (sort-by #(get rank % Long/MAX_VALUE)
                             (filter #(zero? (get in % 0)) left))
              n (or (first ready)
                    ;; Retraction data is not promised acyclic.  Preserve its
                    ;; recorded order rather than dropping a node.
                    (first (sort-by #(get rank % Long/MAX_VALUE) left)))
              children (map :to (get outgoing n))]
          (recur (disj left n) (reduce #(update %1 %2 (fnil dec 0)) in children)
                 (conj out n)))))))

(defn arranged->candidate
  "Compile CASCADE into the existing scorer's candidate shape."
  [target id cascade]
  (let [{:keys [nodes edges precedence]} (occurrence-arrangement cascade)
        by-id (into {} (map (juxt :occurrence-id identity)) nodes)
        node-ids (mapv :occurrence-id nodes)
        fallback precedence
        order (topo-order node-ids edges fallback)
        incoming (group-by :to (remove #(= :overlap (:kind %)) edges))
        overlaps (filter #(= :overlap (:kind %)) edges)
        shared (fn [occurrence-id] (for [{:keys [from to]} overlaps
                                        :when (or (= occurrence-id from) (= occurrence-id to))]
                         (overlap-token target from to)))
        theta-records (into {} (for [{:keys [pattern]} nodes]
                                 [pattern (ledger/pattern-theta (pattern-key pattern))]))
        patterns
        (mapv (fn [occurrence-id]
                (let [p (:pattern (by-id occurrence-id))
                      theta-rec (theta-records p)
                      theta (if (= :recorded-trials (:status theta-rec))
                              (:theta theta-rec) 1/2)
                      needs (set (map #(done-token target (:from %)) (incoming occurrence-id)))
                      produces (conj (set (shared occurrence-id))
                                     (done-token target occurrence-id))]
                  {:id occurrence-id
                   :pattern-id p :occurrence-id occurrence-id :target target
                   :guard {:status :interpreted
                           :clauses [{:present needs :absent #{}}]}
                   :produces produces :theta theta :theta-record theta-rec}))
              order)
        pattern-map (into {} (map (juxt :id identity)) patterns)
        descent (mapv (juxt :from :to) (remove #(= :overlap (:kind %)) edges))]
    {:kind :cascade-candidate :id id :target target
     :precedence {:co-apply {:units order :descent descent :patterns pattern-map}}
     :arrangement {:nodes nodes :edges edges :precedence order}}))

(defn- candidate-patterns [candidate]
  (let [{:keys [units patterns]} (get-in candidate [:precedence :co-apply])]
    (mapv patterns units)))

(defn- terminal-patterns [{:keys [nodes edges]}]
  (let [goal (set (for [n nodes
                        :when (and (map? n) (some #{"goal"} (:roles n)))]
                    (or (:occurrence-id n) (:pattern n))))
        all (set (map #(or (:occurrence-id %) (:pattern %) (:id %) %) nodes))
        sources (set (map :from (remove #(= :overlap (:kind %))
                                        (map normalize-edge edges))))]
    (if (seq goal) goal (cset/difference all sources))))

(defn- progress-preference
  "A compact C over [completed-pattern-count want-met?].  It strictly
  prefers each additional completed pattern and gives a further preference
  to satisfying the want.  This is O(n), rather than the 2^n token
  powerset, so it does not inherit observation-model/max-tokens = 10."
  [n horizon]
  (let [outcomes (for [done (range (inc n)) met? [false true]] [done met?])
        weights (into {} (for [[done met? :as outcome] outcomes]
                           [outcome (Math/exp (+ (* 4.0 (/ done (max 1 n)))
                                                  (if met? 2.0 0.0)))]))
        total (reduce + (vals weights))
        distribution (update-vals weights #(/ % total))]
    (into {} (for [tau (range 1 (inc horizon))] [tau distribution]))))

;; Measured 2026-09-30 on unordered fixtures in the tooling JVM: widths 5..9
;; scored in 0.43, 0.47, 0.56, 0.80 and 1.59 seconds; width 10 took 2.90
;; seconds. Exact enumeration is therefore admitted through width 9.
(def exact-enumeration-frontier-limit 9)

(defn- reachable-from [outgoing start]
  (loop [todo (seq (get outgoing start)) seen #{}]
    (if-let [node (first todo)]
      (if (seen node)
        (recur (next todo) seen)
        (recur (concat (next todo) (get outgoing node)) (conj seen node)))
      seen)))

(defn- maximum-antichain-bound
  "Width of the directed arrangement's partial order, via Dilworth's
  n-minus-maximum-matching construction. Overlap edges impose no order."
  [units edges]
  (let [directed (remove #(= :overlap (:kind %)) edges)
        outgoing (reduce (fn [m {:keys [from to]}] (update m from (fnil conj []) to))
                         {} directed)
        reachable (into {} (map (fn [u] [u (reachable-from outgoing u)]) units))]
    (letfn [(augment [left matched seen]
              (some (fn [right]
                      (when-not (@seen right)
                        (vswap! seen conj right)
                        (let [prior (get matched right)]
                          (if (nil? prior)
                            [true (assoc matched right left)]
                            (when-let [[_ rematched] (augment prior matched seen)]
                              [true (assoc rematched right left)])))))
                    (get reachable left)))]
      (- (count units)
         (count (reduce (fn [matched left]
                          (if-let [[_ next-matched] (augment left matched (volatile! #{}))]
                            next-matched
                            matched))
                        {} units))))))

(defn score-arranged
  "Return finite G and its recorded terms for one arranged cascade.

  LEDGER-ROOT is optional.  It exists so tests can state their learning
  evidence; production-shaped callers normally use the declared ledger."
  ([target id cascade] (score-arranged target id cascade nil))
  ([target id cascade ledger-root]
   (let [occurrence-shape (occurrence-arrangement cascade)
         units (mapv :occurrence-id (:nodes occurrence-shape))
         cycle (directed-cycle (mapv :occurrence-id (:nodes occurrence-shape))
                               (:edges occurrence-shape))
         directed (remove #(= :overlap (:kind %)) (:edges occurrence-shape))
         roots (count (remove (set (map :to directed)) units))
         bound (when-not cycle
                 (maximum-antichain-bound units (:edges occurrence-shape)))]
     (cond
       cycle
       {:status :refused :kind :cyclic-arrangement :cycle cycle
        :policy-id id :target target}

       (> bound exact-enumeration-frontier-limit)
       {:status :refused :kind :frontier-too-wide-for-exact-enumeration
        :policy-id id :target target :units (count units) :roots roots
        :bound bound :limit exact-enumeration-frontier-limit}

       :else
       (let [read-theta ledger/pattern-theta
         candidate (if ledger-root
                     (with-redefs [ledger/pattern-theta #(read-theta % ledger-root)]
                       (arranged->candidate target id cascade))
                     (arranged->candidate target id cascade))
         patterns (candidate-patterns candidate)
         terminals (terminal-patterns occurrence-shape)
         acceptance (set (map #(done-token target %) terminals))
         universe (set (mapcat (fn [p]
                                 (concat (:produces p)
                                         (mapcat :present (get-in p [:guard :clauses]))))
                               patterns))
         horizon (max 1 (min scorer/max-horizon (count patterns)))
         progress-tokens (set (filter #(= :pattern-done (second %))
                                      universe))
         preference (progress-preference (count progress-tokens) horizon)
         model {:schema :wm/observation-model-v1 :backend :exact-enumeration
                :kind :progress-count :universe universe :horizon horizon
                :progress-tokens progress-tokens :want acceptance
                :progress-preference preference
                :provenance {:status :synthetic :calibrated false
                             :source "cascade_shape_g arrangement model"}}
         observation {:status :observed :occurrence-id (str id) :tau horizon
                      :present acceptance :absent #{}}
         ranked (scorer/rank-cascade-actions
                 {:cascade-belief {#{} 1}} [candidate]
                 {:horizon-steps horizon :observation-model model
                  :parameter-information-mode :beta-pattern
                  :g-normalization :per-step-capacity-and-pattern
                  :prediction-context {:occurrence-id (str id) :tau horizon}
                  :observation observation
                  :cascade-spec {:want acceptance :evidence #{} :zeroed #{}
                                 :c {:source :terminal-and-progress-preference}
                                 :c-schedule (vec (range 1 (inc horizon)))}})
         entry (when (vector? ranked) (first ranked))
         information (get-in entry [:certificate :g-terms :expected-information-gain])
         result {:status (if entry :computed :refused)
                 :policy-id id :target target :candidate candidate
                 :horizon horizon :terminals terminals
                 :preference-at-each-step preference
                 :risk (get-in entry [:certificate :g-terms :risk])
                 :ambiguity (get-in entry [:certificate :g-terms :ambiguity])
                 :information-gain information
                 :g (:controller-score entry)
                 :scorer-result ranked}]
     (if (and entry (Double/isFinite (double (:g result))))
       result
       (assoc result :status :refused :reason (or (:kind ranked) :non-finite-g))))))))

(defn read-json [path]
  (json/parse-string (slurp path) true))

(defn- binary-entropy [p]
  (- (reduce + 0.0 (for [x [p (- 1.0 p)] :when (pos? x)]
                          (* x (Math/log x))))))

(defn fit-evidence
  "Fit of one arranged policy to its mission-HEAD reading.

  Accepted nodes use p=.9, rejected nodes p=.1, and nodes never read against
  this circumstance p=.5 (maximum Bernoulli uncertainty).  Coverage is the
  fraction of read fragments accounted for by at least one policy node, with
  Jeffreys smoothing.  F is the negative log likelihood of these stated fit
  observations.  FIT-ambiguity adds H(p)+(1-p), so unknown and poor fits both
  raise G.  These probabilities are an explicit preliminary model, not facts
  recovered from the retriever; the stored analyses contain ranks and
  rationales but no numeric relevance score."
  [cascade analysis-map]
  (let [fragments (vec (mapcat :fragments (:sentences analysis-map)))
        accepted (reduce (fn [m [i f]]
                           (reduce (fn [m r] (update m (:id r) (fnil conj [])
                                                    {:fragment-index i :rationale (:rationale r)}))
                                   m (:pattern_refs f)))
                         {} (map-indexed vector fragments))
        rejected (set (map :id (mapcat :pattern_rejections fragments)))
        patterns (vec (distinct (map #(or (:pattern %) (:id %) %) (:nodes cascade))))
        nodes (mapv (fn [pattern]
                      (let [evidence (get accepted pattern)
                            status (cond (seq evidence) :accepted
                                         (rejected pattern) :rejected
                                         :else :not-read)
                            p ({:accepted 9/10 :rejected 1/10 :not-read 1/2} status)]
                        {:pattern pattern :status status :probability p
                         :reading-evidence evidence
                         :retriever-relevance {:status :absent
                                               :reason :numeric-score-not-recorded}}))
                    patterns)
        covered (set (mapcat #(map :fragment-index (:reading-evidence %)) nodes))
        total (count fragments)
        coverage-p (/ (+ (count covered) 1/2) (+ total 1))
        node-f (if (seq nodes)
                 (/ (reduce + 0.0 (map #(matched/surprisal (:probability %)) nodes))
                    (count nodes))
                 0.0)
        coverage-f (matched/surprisal coverage-p)
        fit-ambiguity (if (seq nodes)
                        (/ (reduce + 0.0
                                   (map (fn [{:keys [probability]}]
                                          (+ (binary-entropy (double probability))
                                             (- 1.0 (double probability))))
                                        nodes))
                           (count nodes))
                        0.0)]
    {:status :computed :nodes nodes
     :coverage {:covered (count covered) :total total
                :share (if (zero? total) 0 (/ (count covered) total))
                :unexplained (vec (remove covered (range total)))}
     :retriever-relevance {:status :absent :reason :numeric-scores-not-in-analysis-artifact}
     :f (+ node-f coverage-f) :fit-ambiguity fit-ambiguity
     :interpretation-owed (mapv :pattern (filter #(= :not-read (:status %)) nodes))}))

(defn score-policy
  "Score one materialized policy, attaching circumstance fit as F and adding
  its stated uncertainty to the scorer's ambiguity term."
  [policy]
  (let [base (score-arranged (:target policy) (:policy-id policy) (:cascade policy))]
    (if (= :refused (:status base))
      base
      (let [fit (fit-evidence (:cascade policy) (:analysis policy))
        ambiguity (+ (double (:ambiguity base)) (:fit-ambiguity fit))
        g (- (+ (double (:risk base)) ambiguity)
             (double (:information-gain base)))
        computed-f {:status :computed :value (:f fit) :source :xiang-reading-fit
                    :evidence fit}
        carrier {:id (:policy-id policy) :habit 1.0 :g g :f (:f fit)
                 :f-status :computed :computed-f computed-f}]
    (assoc base :g g :controller-score g :ambiguity ambiguity
           :scorer-g (:g base) :scorer-ambiguity (:ambiguity base)
           :fit fit :f (:f fit) :f-status :computed :computed-f computed-f
           :selection-candidate carrier
           :selection-law (selection/law-receipt [carrier]))))))

(defn policy-shape-stats [policy]
  (let [candidate (arranged->candidate (:target policy) (:policy-id policy)
                                       (:cascade policy))
        nodes (vec (get-in candidate [:arrangement :nodes]))
        edges (vec (get-in candidate [:arrangement :edges]))
        precedence (vec (get-in candidate [:arrangement :precedence]))
        incoming (group-by :to (remove #(= :overlap (:kind %)) edges))
        depth (reduce (fn [d n]
                        (assoc d n (inc (reduce max 0 (map #(get d (:from %) 0)
                                                           (incoming n))))))
                      {} precedence)]
    {:distinct-patterns (count (set (map :pattern nodes)))
     :nodes (count nodes)
     :longest-dependency-chain (reduce max 0 (vals depth))}))

(defn structural-identity
  "The shared policy identity: target/mission scope, node sequence, and
  normalized directed/overlap edges. Annotation text is deliberately absent."
  [policy]
  (let [c (:cascade policy)]
    {:mission (or (:mission policy) (:target policy))
     :node-sequence (mapv #(or (:pattern %) (:id %) %) (:nodes c))
     :edges (mapv #(select-keys (normalize-edge %) [:from :to :kind]) (:edges c))}))

(defn materialize-policies
  "Read S3c's fixed artifacts.  Returns both analysis arrangements and graph
  retractions; the result is data and performs no scoring or writes."
  [dir]
  (let [reported
        (vec
         (mapcat
    (fn [analysis-file]
      (let [stem (str/replace (.getName (io/file analysis-file)) #"\.request\.json\.analysis\.json$" "")
            request-file (io/file dir (str stem ".request.json"))
            request (when (.isFile request-file) (read-json request-file))
            target stem analysis-map (read-json analysis-file)
            modes (:rows
                   (reduce (fn [{:keys [seen] :as acc} p]
                             (let [c (:cascade p)
                                   shape (select-keys c [:nodes :edges :precedence :alternatives])]
                               (if (contains? seen shape)
                                 acc
                                 (-> acc (update :seen conj shape) (update :rows conj p)))))
                           {:seen #{} :rows []}
                           (for [mode [:alternatives :overlap]
                                 [i c] (map-indexed vector (:cascades (analysis/analysis->cascades analysis-map {:mode mode})))]
                             {:mission stem :kind mode :adjustment (str (name mode) "-" (inc i))
                              :cascade c})))
            rp (io/file dir (str stem ".retractions.json"))
            retractions (when (.isFile rp)
                          (for [r (:retractions (read-json rp))]
                            {:mission stem :kind :retraction
                             :adjustment (str "retraction-" (:rank r))
                             :cascade (retraction-cascade/from-retraction
                                       target analysis-map r)}))]
        (when-not (reading-registry/source-text-template-only? (:source_text request))
          (map-indexed (fn [i p] (assoc p :target target :analysis analysis-map
                                        :reported-id (str stem "/" (inc i))))
                       (concat modes retractions)))))
    (sort-by #(.getName (io/file %))
             (filter #(and (.isFile (io/file %))
                           (str/ends-with? (.getName (io/file %))
                                           ".request.json.analysis.json"))
                     (file-seq (io/file dir))))))
        distinct-policies
        (:rows (reduce (fn [{:keys [seen] :as acc} p]
                         (let [shape (structural-identity p)]
                           (if (contains? seen shape)
                             acc
                             (-> acc (update :seen conj shape) (update :rows conj p)))))
                       {:seen #{} :rows []} reported))]
    (with-meta
      (mapv (fn [i p] (assoc p :policy-id (str (:mission p) "/distinct-" (inc i))))
            (range) distinct-policies)
      {:reported-count (count reported) :distinct-count (count distinct-policies)})))
