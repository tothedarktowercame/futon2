(ns futon2.aif.a4a-substrate
  "Thin Drawbridge adapter for A4a/R17 demo-validated concept formation.

   This namespace reads the substrate corpus and can mint capability-star
   identity docs, guarded by :write? false by default."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.a4a :as a4a]
            [futon2.aif.actuator-a3 :as a3]
            [futon2.aif.substrate :as substrate])
  (:import [java.net URLEncoder]))

(def default-semilattice-clusters-path
  "/home/joe/code/futon3c/holes/excursions/pipeline-semilattice-clusters.edn")

(def default-mission-pattern-scopes-path
  "/home/joe/code/futon6/data/mission-pattern-scopes.edn")

(def default-slush-candidates-path
  "/home/joe/code/futon2/holes/labs/slush-demo/findings/slush_candidates.edn")

(defn capability-query
  []
  {:find '[e]
   :where '[[e :entity/type :capability]]})

(defn capability-edge-query
  []
  {:find '[cap mission]
   :where '[[hx :hx/type :capability/*]
            [hx :hx/endpoints cap]
            [cap :entity/type :capability]
            [hx :hx/endpoints mission]
            [mission :entity/type :mission/doc]]})

(defn discharge-query
  []
  {:find '[mission endpoint type]
   :where '[[e :entity/type :discharge]
            [e :discharge/mission mission]
            [e :discharge/endpoint endpoint]
            [e :discharge/type type]]})

(defn read-corpus
  "Read the A4a corpus from substrate via the A3 Drawbridge evaluator.

   Returns the pure corpus shape consumed by futon2.aif.a4a/corpus->concentration."
  ([] (read-corpus {}))
  ([opts]
   (let [caps (substrate/entities-by-type :capability opts)
         missions (substrate/entities-by-type :mission/doc opts)
         cap-ids (set (map #(str (or (:entity/id %) (:xt/id %) (:id %))) caps))
         mission-ids (set (map #(str (or (:entity/id %) (:xt/id %) (:id %))) missions))
         capabilities (sort cap-ids)
         edges (->> (substrate/hyperedges-by-type :capability/* opts)
                    (mapcat (fn [hx]
                              (let [ends (set (map str (:hx/endpoints hx)))]
                                (for [cap (filter ends cap-ids)
                                      mission (filter ends mission-ids)]
                                  [cap mission]))))
                    (sort-by pr-str)
                    vec)
         discharges (->> (substrate/entities-by-type :discharge opts)
                         (map #(merge % (:entity/props %)))
                         (map (fn [doc]
                                {:mission (str (:discharge/mission doc))
                                 :endpoint (str (:discharge/endpoint doc))
                                 :type (:discharge/type doc)}))
                         (sort-by pr-str)
                         vec)]
     {:capabilities (vec capabilities)
      :edges (vec edges)
      :discharges (vec discharges)})))

(declare load-pattern-grain-model-uncertainty)

(defn- parse-long-safe
  [s]
  (try
    (Long/parseLong s)
    (catch NumberFormatException _
      nil)))

(defn- constellation-key
  [concept]
  (cond
    (integer? concept) concept
    (keyword? concept) (constellation-key (name concept))
    (string? concept) (or (parse-long-safe concept)
                          (some-> (re-matches #"P([0-9]+)" concept)
                                  second
                                  parse-long-safe))
    :else nil))

(defn- add-constellation-model-uncertainty
  [constellation->model-uncertainty doc]
  (let [concept (:star/capability doc)
        ckey (constellation-key concept)]
    (if (contains? constellation->model-uncertainty ckey)
      (assoc doc
             :star/constellation-model-uncertainty (double (get constellation->model-uncertainty ckey))
             :star/model-uncertainty-unit :endpoint-count-stddev
             :star/model-uncertainty-source :a4a-constellation-stddev)
      doc)))

(defn- star-docs
  [concepts opts]
  (let [constellation->model-uncertainty (or (:constellation->model-uncertainty opts)
                               (:constellation->model-uncertainty (load-pattern-grain-model-uncertainty opts)))]
    (mapv #(add-constellation-model-uncertainty constellation->model-uncertainty
                                  (a4a/concept->star-doc %))
          concepts)))

(defn mint-stars!
  "Build or write capability-star identity docs.

   With :write? false, the default, returns the docs that would be written and
   performs no Drawbridge transaction. With :write? true, submits ::xt/put docs
   through the A3 Drawbridge transaction helper; callers must use a quiet window."
  ([concepts] (mint-stars! concepts {}))
  ([concepts {:keys [write?] :as opts}]
   (let [docs (star-docs concepts opts)]
     (if write?
       {:write? true
        :docs docs
        :tx (a3/drawbridge-submit-tx!
             (mapv (fn [doc] [:xtdb.api/put doc]) docs)
             opts)}
       docs))))

(defn- uri-token
  [value]
  (URLEncoder/encode (str value) "UTF-8"))

(defn slush-candidates-id
  [mission-id]
  (str "a4a-slush/candidates/" (uri-token (a4a/normalize-mission-id mission-id))))

(defn- read-edn-file
  [path]
  (edn/read-string (slurp path)))

(defn- readable-file?
  [path]
  (let [file (io/file path)]
    (and (.isFile file) (.canRead file))))

(defn- zero-pattern-grain-model-uncertainty
  []
  {:pattern->constellation (sorted-map)
   :mission->patterns (sorted-map)
   :corpus-edges []
   :constellation->model-uncertainty (sorted-map)
   :mission->model-uncertainty (sorted-map)
   :model-uncertainty-fn (constantly 0.0)})

(defn- zero-slush-candidates
  []
  {:source :gfn-slush-lambda-4
   :missions (sorted-map)
   :notes ["No offline slush candidate deposit was readable."]})

(defn load-slush-candidate-deposit
  "Load the offline lambda=4 slush candidate deposit.

   This is a pure file read. It does not train or sample a GFlowNet at live-loop
   time; the slush is an offline candidate producer whose outputs are deposited
   for later A1 consumption."
  ([] (load-slush-candidate-deposit {}))
  ([{:keys [slush-candidates-path]
     :or {slush-candidates-path default-slush-candidates-path}}]
   (if (readable-file? slush-candidates-path)
     (try
       (read-edn-file slush-candidates-path)
       (catch Exception _
         (zero-slush-candidates)))
     (zero-slush-candidates))))

(defn- mission-entry
  [deposit mission-id]
  (let [mission (a4a/normalize-mission-id mission-id)
        missions (:missions deposit)]
    (or (get missions mission)
        (some (fn [[k v]]
                (when (= mission (a4a/normalize-mission-id k))
                  v))
              missions))))

(defn slush-candidates
  "Return top-k distinct A3-passing slush candidate constellation selections.

   Candidates are the offline lambda=4 coverage/A3 frontier from the validated
   slush run. The :model-uncertainty field is the corpus-global constellation posterior spread carried for
   inspection; the known-open point is that it is not yet a per-mission steering
   signal."
  ([mission-id] (slush-candidates mission-id {}))
  ([mission-id {:keys [top-k] :as opts}]
   (let [deposit (or (:slush-candidate-deposit opts)
                     (load-slush-candidate-deposit opts))
         candidates (vec (:candidates (mission-entry deposit mission-id)))]
     (if top-k
       (subvec candidates 0 (min top-k (count candidates)))
       candidates))))

(defn slush-candidates-doc
  "Assemble the stable live-loop read-point doc for deposited slush candidates."
  ([mission-id] (slush-candidates-doc mission-id (slush-candidates mission-id)))
  ([mission-id candidates]
   (let [mission (a4a/normalize-mission-id mission-id)
         cands (mapv (fn [rank candidate]
                       (-> candidate
                           (update :constellations #(mapv int %))
                           (assoc :rank rank)))
                     (range 1 (inc (count candidates)))
                     candidates)]
     {:xt/id (slush-candidates-id mission)
      :entity/type :slush-candidates
      :slush/mission mission
      :slush/source :gfn-slush-lambda-4
      :slush/lambda 4
      :slush/selection-size 5
      :slush/status :coverage-a3-driven
      :slush/model-uncertainty-unit :endpoint-count-stddev
      :slush/model-uncertainty-steering :deferred-per-mission-signal
      :slush/notes (str/join
                    " "
                    ["Candidates are coverage/A3-driven offline slush outputs."
                     "Corpus-global posterior spread is exposed for inspection but stayed below uniform in the validated sweep."
                     "Do not treat this deposit as an online GFN training call."])
      :slush/candidate-count (count cands)
      :slush/candidates cands
      :slush/updated-by :a4a-slush})))

(defn mint-slush-candidates!
  "Build or write a :slush-candidates read-point doc for one mission.

   With :write? false, the default, returns the doc that would be written and
   performs no Drawbridge transaction. With :write? true, submits one ::xt/put
   through the A3 Drawbridge transaction helper; callers must use a quiet window."
  ([mission-id] (mint-slush-candidates! mission-id {}))
  ([mission-id {:keys [write?] :as opts}]
   (let [doc (slush-candidates-doc mission-id (slush-candidates mission-id opts))]
     (if write?
       {:write? true
        :doc doc
        :tx (a3/drawbridge-submit-tx!
             [[:xtdb.api/put doc]]
             opts)}
       doc))))

(defn pattern->constellation
  [semilattice-clusters]
  (into (sorted-map)
        (map (fn [{:keys [pattern cluster]}]
               [(a4a/pattern-stem pattern) cluster]))
        (:pattern-membership semilattice-clusters)))

(defn mission->patterns
  [mission-pattern-scopes]
  (into (sorted-map)
        (map (fn [{:keys [mission applied]}]
               [(a4a/normalize-mission-id mission)
                (mapv a4a/pattern-stem applied)]))
        (:missions mission-pattern-scopes)))

(defn corpus-edges
  [mission-pattern-scopes]
  (mapv (fn [[mission pattern]]
          [pattern mission])
        (for [{:keys [mission applied]} (:missions mission-pattern-scopes)
              pattern applied]
          [(a4a/normalize-mission-id mission) (a4a/pattern-stem pattern)])))

(defn- load-pattern-grain-model-uncertainty*
  "Load the pattern-grain posterior-spread maps from EDN files.

   Pure file reads only: no Drawbridge. Returns the mission->model-uncertainty table plus a
   two-argument model-uncertainty closure for efe/graph-control-terms."
  ([{:keys [semilattice-clusters-path mission-pattern-scopes-path]
     :or {semilattice-clusters-path default-semilattice-clusters-path
          mission-pattern-scopes-path default-mission-pattern-scopes-path}}]
   (if (and (readable-file? semilattice-clusters-path)
            (readable-file? mission-pattern-scopes-path))
     (try
       (let [clusters (read-edn-file semilattice-clusters-path)
             scopes (read-edn-file mission-pattern-scopes-path)
             p->c (pattern->constellation clusters)
             m->patterns (mission->patterns scopes)
             edges (corpus-edges scopes)
             c->uncertainty (a4a/constellation->model-uncertainty p->c edges)
             m->uncertainty (a4a/mission->model-uncertainty m->patterns p->c c->uncertainty)]
         {:pattern->constellation p->c
          :mission->patterns m->patterns
          :corpus-edges edges
          :constellation->model-uncertainty c->uncertainty
          :mission->model-uncertainty m->uncertainty
          :model-uncertainty-fn (a4a/make-model-uncertainty-fn m->uncertainty)})
       (catch Exception _
         (zero-pattern-grain-model-uncertainty)))
     (zero-pattern-grain-model-uncertainty))))

(def ^:private memoized-load-pattern-grain-model-uncertainty
  (memoize load-pattern-grain-model-uncertainty*))

(defn load-pattern-grain-model-uncertainty
  "Load the pattern-grain posterior-spread maps from EDN files.

   Results are memoized by path options. If the static files are absent or
   unreadable, returns a zero model-uncertainty closure so default-armed ranking remains
   hermetic."
  ([] (load-pattern-grain-model-uncertainty {}))
  ([opts]
   (memoized-load-pattern-grain-model-uncertainty
    (select-keys opts [:semilattice-clusters-path
                       :mission-pattern-scopes-path]))))
