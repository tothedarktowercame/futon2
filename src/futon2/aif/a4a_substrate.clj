(ns futon2.aif.a4a-substrate
  "Thin Drawbridge adapter for A4a/R17 demo-validated concept formation.

   This namespace reads the substrate corpus and can mint capability-star
   identity docs, guarded by :write? false by default."
  (:require [clojure.edn :as edn]
            [futon2.aif.a4a :as a4a]
            [futon2.aif.actuator-a3 :as a3]))

(def default-semilattice-clusters-path
  "/home/joe/code/futon3c/holes/excursions/pipeline-semilattice-clusters.edn")

(def default-mission-pattern-scopes-path
  "/home/joe/code/futon6/data/mission-pattern-scopes.edn")

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

(defn q-form
  [query & args]
  (pr-str `(do
             (require (quote xtdb.api))
             (xtdb.api/q (xtdb.api/db (:node @futon3c.dev/!f1-sys))
                         (quote ~query)
                         ~@args))))

(defn- first-col
  [rows]
  (mapv first rows))

(defn- edge-row
  [[capability mission]]
  [(str capability) (str mission)])

(defn- discharge-row
  [[mission endpoint type]]
  {:mission (str mission)
   :endpoint (str endpoint)
   :type type})

(defn read-corpus
  "Read the A4a corpus from substrate via the A3 Drawbridge evaluator.

   Returns the pure corpus shape consumed by futon2.aif.a4a/corpus->concentration."
  ([] (read-corpus {}))
  ([opts]
   (let [capabilities (sort (map str (first-col (a3/drawbridge-eval
                                                 (q-form (capability-query))
                                                 opts))))
         edges (sort-by pr-str (mapv edge-row (a3/drawbridge-eval
                                               (q-form (capability-edge-query))
                                               opts)))
         discharges (sort-by pr-str (mapv discharge-row (a3/drawbridge-eval
                                                         (q-form (discharge-query))
                                                         opts)))]
     {:capabilities (vec capabilities)
      :edges (vec edges)
      :discharges (vec discharges)})))

(defn mint-stars!
  "Build or write capability-star identity docs.

   With :write? false, the default, returns the docs that would be written and
   performs no Drawbridge transaction. With :write? true, submits ::xt/put docs
   through the A3 Drawbridge transaction helper; callers must use a quiet window."
  ([concepts] (mint-stars! concepts {}))
  ([concepts {:keys [write?] :as opts}]
   (let [docs (mapv a4a/concept->star-doc concepts)]
     (if write?
       {:write? true
        :docs docs
        :tx (a3/drawbridge-submit-tx!
             (mapv (fn [doc] [:xtdb.api/put doc]) docs)
             opts)}
       docs))))

(defn- read-edn-file
  [path]
  (edn/read-string (slurp path)))

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

(defn load-pattern-grain-eig
  "Load the pattern-grain constellation EIG maps from EDN files.

   Pure file reads only: no Drawbridge. Returns the mission->eig table plus a
   two-argument EIG closure for efe/graph-efe-terms."
  ([] (load-pattern-grain-eig {}))
  ([{:keys [semilattice-clusters-path mission-pattern-scopes-path]
     :or {semilattice-clusters-path default-semilattice-clusters-path
          mission-pattern-scopes-path default-mission-pattern-scopes-path}}]
   (let [clusters (read-edn-file semilattice-clusters-path)
         scopes (read-edn-file mission-pattern-scopes-path)
         p->c (pattern->constellation clusters)
         m->patterns (mission->patterns scopes)
         edges (corpus-edges scopes)
         c->eig (a4a/constellation->eig p->c edges)
         m->eig (a4a/mission->eig m->patterns p->c c->eig)]
     {:pattern->constellation p->c
      :mission->patterns m->patterns
      :corpus-edges edges
      :constellation->eig c->eig
      :mission->eig m->eig
      :eig-fn (a4a/make-eig-fn m->eig)})))
