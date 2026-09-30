(ns futon2.aif.cascade-shape-g
  "Score an arranged pattern cascade through the existing bounded scorer.

  This adapter is deliberately structural.  Each node produces its own done
  token.  A directed non-overlap edge makes the destination require the
  source's done token; an overlap edge gives both endpoints a shared token
  which both operators produce.  Consequently equal node orders with
  different edges generate different B operators before
  cascade-observation-scoring/rank-cascade-actions evaluates them.

  Claude-1's modelling choices are the token reading above and the Jeffreys
  Beta(1/2,1/2) prior when the learning ledger has no trials.  The existing
  scorer remains the authority for rollout, step-indexed C, risk, ambiguity,
  and G.  Parameter information is computed with parameter-novelty's
  canonical Beta kernel and reported beside G; the existing scorer does not
  consume that term.  This is therefore still the current list rollout, with
  structure compiled into guards, rather than the future recursive fold."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.analysis-cascade :as analysis]
            [futon2.aif.cascade-observation-scoring :as scorer]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.parameter-novelty :as novelty]))

(def ^:private class-universe
  [:focused :related :unrelated :stop-the-line :ending/not-yet-evaluated])

(defn- pattern-key [s]
  (keyword (str/replace (str s) #"^:" "")))

(defn- done-token [target pattern] [target :pattern-done pattern])
(defn- overlap-token [target a b]
  [target :overlap (vec (sort [(str a) (str b)]))])

(defn- normalize-edge [edge]
  {:from (or (:from edge) (:a edge))
   :to (or (:to edge) (:b edge))
   :kind (or (:kind edge) (:kind_used edge) (:kind-used edge) :precedes)})

(defn- occurrence-arrangement [{:keys [nodes edges]}]
  (let [occurrences
        (mapv (fn [i n]
                (let [pattern (or (:pattern n) (:id n) n)]
                  {:occurrence-id [pattern (or (:fragment-index n) i) i]
                   :pattern pattern :roles (vec (:roles n))
                   :fragment-index (:fragment-index n)}))
              (range) nodes)
        lookup (fn [pattern fragment]
                 (or (:occurrence-id
                      (first (filter #(and (= pattern (:pattern %))
                                           (or (nil? fragment)
                                               (= fragment (:fragment-index %))))
                                     occurrences)))
                     [pattern fragment 0]))
        occurrence-edges
        (mapv (fn [e]
                (let [n (normalize-edge e)]
                  (assoc n
                         :from (lookup (:from n) (:from-fragment e))
                         :to (lookup (:to n) (:to-fragment e)))))
              edges)]
    {:nodes occurrences :edges occurrence-edges
     :precedence (mapv :occurrence-id occurrences)}))

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
        precedence
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
              order)]
    {:kind :cascade-candidate :id id :target target :precedence precedence
     :arrangement {:nodes nodes :edges edges :precedence order}}))

(defn- terminal-patterns [{:keys [nodes edges]}]
  (let [goal (set (for [n nodes
                        :when (and (map? n) (some #{"goal"} (:roles n)))]
                    (or (:occurrence-id n) (:pattern n))))
        all (set (map #(or (:occurrence-id %) (:pattern %) (:id %) %) nodes))
        sources (set (map :from (remove #(= :overlap (:kind %))
                                        (map normalize-edge edges))))]
    (if (seq goal) goal (set/difference all sources))))

(defn score-arranged
  "Return finite G and its recorded terms for one arranged cascade.

  LEDGER-ROOT is optional.  It exists so tests can state their learning
  evidence; production-shaped callers normally use the declared ledger."
  ([target id cascade] (score-arranged target id cascade nil))
  ([target id cascade ledger-root]
   (let [read-theta ledger/pattern-theta
         candidate (if ledger-root
                     (with-redefs [ledger/pattern-theta #(read-theta % ledger-root)]
                       (arranged->candidate target id cascade))
                     (arranged->candidate target id cascade))
         precedence (:precedence candidate)
         occurrence-shape (occurrence-arrangement cascade)
         terminals (terminal-patterns occurrence-shape)
         acceptance (set (map #(done-token target %) terminals))
         universe (set (mapcat (fn [p]
                                 (concat (:produces p)
                                         (mapcat :present (get-in p [:guard :clauses]))))
                               precedence))
         horizon (max 1 (min scorer/max-horizon (count precedence)))
         preference (into {} (for [tau (range 1 (inc horizon))]
                               [tau (if (= tau horizon)
                                      {:focused 9/10 :related 1/40 :unrelated 1/40
                                       :stop-the-line 1/40 :ending/not-yet-evaluated 1/40}
                                      {:focused 1/40 :related 1/40 :unrelated 1/40
                                       :stop-the-line 1/40 :ending/not-yet-evaluated 9/10})]))
         model {:schema :wm/observation-model-v1 :backend :exact-enumeration
                :kind :class-emission :universe universe :horizon horizon
                :class-universe class-universe :acceptance acceptance
                :target-class {target :focused} :class-preference preference
                :provenance {:status :synthetic :calibrated false
                             :source "cascade_shape_g arrangement model"}}
         observation {:status :observed :occurrence-id (str id) :tau horizon
                      :present acceptance :absent #{}}
         ranked (scorer/rank-cascade-actions
                 {:cascade-belief {#{} 1}} [candidate]
                 {:horizon-steps horizon :observation-model model
                  :prediction-context {:occurrence-id (str id) :tau horizon}
                  :observation observation
                  :cascade-spec {:want acceptance :evidence #{} :zeroed #{}
                                 :c {:source :terminal-and-progress-preference}
                                 :c-schedule (vec (range 1 (inc horizon)))}})
         entry (when (vector? ranked) (first ranked))
         steps (get-in entry [:certificate :steps])
         priors (map :theta-record precedence)
         information (reduce + 0.0
                             (map (fn [r]
                                    (let [[a b] (if (= :recorded-trials (:status r))
                                                  [(+ 1/2 (:successes r))
                                                   (+ 1/2 (- (:trials-count r) (:successes r)))]
                                                  [1/2 1/2])]
                                      (:nats (novelty/beta-information a b))))
                                  priors))
         result {:status (if entry :computed :refused)
                 :policy-id id :target target :candidate candidate
                 :horizon horizon :terminals terminals
                 :preference-at-each-step preference
                 :risk (when entry (reduce + 0.0 (map :risk steps)))
                 :ambiguity (when entry (reduce + 0.0 (map :ambiguity steps)))
                 :information-gain information
                 :g (:controller-score entry)
                 :scorer-result ranked}]
     (if (and entry (Double/isFinite (double (:g result))))
       result
       (assoc result :status :refused :reason (or (:kind ranked) :non-finite-g))))))

(defn read-json [path]
  (json/parse-string (slurp path) true))

(defn materialize-policies
  "Read S3c's fixed artifacts.  Returns both analysis arrangements and graph
  retractions; the result is data and performs no scoring or writes."
  [dir]
  (let [reported
        (vec
         (mapcat
    (fn [analysis-file]
      (let [stem (str/replace (.getName (io/file analysis-file)) #"\.request\.json\.analysis\.json$" "")
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
                             :cascade {:nodes (:nodes r) :edges (:edges r)
                                       :precedence (:nodes r)}}))]
        (map-indexed (fn [i p] (assoc p :target target :reported-id (str stem "/" (inc i))))
                     (concat modes retractions))))
    (sort-by #(.getName (io/file %))
             (filter #(and (.isFile (io/file %))
                           (str/ends-with? (.getName (io/file %))
                                           ".request.json.analysis.json"))
                     (file-seq (io/file dir))))))
        structure (fn [p]
                    (let [c (:cascade p)]
                      {:mission (:mission p)
                       :node-sequence (mapv #(or (:pattern %) (:id %) %) (:nodes c))
                       :edges (mapv #(select-keys (normalize-edge %)
                                                  [:from :to :kind]) (:edges c))}))
        distinct-policies
        (:rows (reduce (fn [{:keys [seen] :as acc} p]
                         (let [shape (structure p)]
                           (if (contains? seen shape)
                             acc
                             (-> acc (update :seen conj shape) (update :rows conj p)))))
                       {:seen #{} :rows []} reported))]
    (with-meta
      (mapv (fn [i p] (assoc p :policy-id (str (:mission p) "/distinct-" (inc i))))
            (range) distinct-policies)
      {:reported-count (count reported) :distinct-count (count distinct-policies)})))
