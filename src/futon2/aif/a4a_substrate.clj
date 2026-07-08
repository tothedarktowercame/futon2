(ns futon2.aif.a4a-substrate
  "Thin Drawbridge adapter for A4a/R17 demo-validated concept formation.

   This namespace reads the substrate corpus and can mint capability-star
   identity docs, guarded by :write? false by default."
  (:require [futon2.aif.a4a :as a4a]
            [futon2.aif.actuator-a3 :as a3]))

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
