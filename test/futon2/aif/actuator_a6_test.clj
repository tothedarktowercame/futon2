(ns futon2.aif.actuator-a6-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.actuator-a3 :as a3]
            [futon2.aif.actuator-a6 :as a6]))

(defn- uuid-suffix []
  (str (java.util.UUID/randomUUID)))

(defn- evict-ids! [& ids]
  (let [ids (filterv some? ids)]
    (when (seq ids)
      (a3/drawbridge-submit-tx! (mapv (fn [id] [:xtdb.api/evict id]) ids)))))

(defn- put-docs! [& docs]
  (a3/drawbridge-submit-tx! (mapv (fn [doc] [:xtdb.api/put doc]) docs)))

(defn- fixture-ids []
  (let [suffix (uuid-suffix)
        cap (str "a6-test/cap/" suffix)
        star (str "a6-test/star/" suffix)
        discharge (str "a6-test/discharge/" suffix)]
    {:cap cap
     :star star
     :discharge discharge
     :status (a6/status-id star)}))

(defn- star-doc [{:keys [star cap]}]
  {:xt/id star
   :entity/type :capability-star
   :star/capability cap})

(defn- discharge-doc [{:keys [discharge cap]}]
  {:xt/id discharge
   :entity/type :discharge
   :discharge/type :capability
   :discharge/endpoint cap})

(defn- graph [cap]
  {:capabilities {"goal" {:scope [cap "cap-other"]}
                  cap {}
                  "cap-other" {}}
   :missions {"M-a" {:scope []
                     :produces [cap]
                     :open-hole-count 1}
              "M-b" {:scope []
                     :produces ["cap-other"]
                     :open-hole-count 2}}})

(defn- rank-opts [cap]
  {:pre-registered-goal "goal"
   :capability-filter #{cap}})

(deftest status-doc-contract-shape-test
  (let [doc (a6/status-doc "star-1" {:at #inst "2026-07-08T00:00:00.000-00:00"})]
    (is (= "capability-star-status/star-1" (:xt/id doc)))
    (is (= :capability-star-status (:entity/type doc)))
    (is (= "star-1" (:status/star doc)))
    (is (= :satisfied (:status/status doc)))
    (is (= :a6-discharge (:status/updated-by doc)))
    (is (= #inst "2026-07-08T00:00:00.000-00:00" (:status/at doc)))
    (is (not= :capability-star (:entity/type doc))
        "A6 never writes A4a's :capability-star identity shape")))

(deftest flip-status-noops-when-star-is-absent-test
  (let [ids (fixture-ids)
        opts {:capability-filter #{(:cap ids)}}]
    (try
      (put-docs! (discharge-doc ids))
      (let [result (a6/flip-star-status-on-discharge! opts)]
        (is (empty? (:written result)))
        (is (= [{:discharge (:discharge ids)
                 :capability (:cap ids)
                 :reason :no-capability-star}]
               (:no-star result)))
        (is (empty? (a6/status-rows opts))))
      (finally
        (evict-ids! (:discharge ids) (:status ids))))))

(deftest flip-status-writes-locked-shape-test
  (let [ids (fixture-ids)
        opts {:capability-filter #{(:cap ids)}
              :at #inst "2026-07-08T00:00:00.000-00:00"}]
    (try
      (put-docs! (star-doc ids) (discharge-doc ids))
      (let [result (a6/flip-star-status-on-discharge! opts)
            written (-> result :written first :doc)]
        (is (= 1 (count (:written result))))
        (is (empty? (:no-star result)))
        (is (= {:xt/id (:status ids)
                :entity/type :capability-star-status
                :status/star (:star ids)
                :status/status :satisfied
                :status/updated-by :a6-discharge
                :status/at #inst "2026-07-08T00:00:00.000-00:00"}
               written))
        (is (= [{:status (:status ids)
                 :star (:star ids)
                 :capability (:cap ids)}]
               (a6/status-rows opts))))
      (finally
        (evict-ids! (:discharge ids) (:star ids) (:status ids))))))

(deftest apply-star-status-touches-only-capability-status-test
  (let [ids (fixture-ids)
        opts {:capability-filter #{(:cap ids)}}
        g (graph (:cap ids))]
    (try
      (put-docs! (star-doc ids)
                 (a6/status-doc (:star ids)))
      (let [enriched (a6/apply-star-status g opts)]
        (is (= :satisfied (get-in enriched [:capabilities (:cap ids) :status])))
        (is (= (dissoc (get-in g [:capabilities (:cap ids)]) :status)
               (dissoc (get-in enriched [:capabilities (:cap ids)]) :status)))
        (is (= (get-in g [:missions])
               (get-in enriched [:missions]))
            "A6 bridge mutates no mission fields"))
      (finally
        (evict-ids! (:star ids) (:status ids))))))

(deftest closure-falsifier-rank-moves-after-status-flip-test
  (let [ids (fixture-ids)
        opts (rank-opts (:cap ids))
        g (graph (:cap ids))]
    (try
      (put-docs! (star-doc ids) (discharge-doc ids))
      (let [result (a6/closure-falsifier g opts)]
        (is (= ["M-a" "M-b"] (:before-order result)))
        (is (= ["M-b" "M-a"] (:after-order result)))
        (is (true? (:moved? result))))
      (finally
        (evict-ids! (:discharge ids) (:star ids) (:status ids))))))

(deftest closure-falsifier-negative-control-rank-stable-test
  (let [ids (fixture-ids)
        opts (rank-opts (:cap ids))
        g (graph (:cap ids))]
    (try
      (put-docs! (star-doc ids))
      (let [before (a6/rank-graph g opts)
            after (a6/rank-graph (a6/apply-star-status g opts) opts)
            before-order (mapv #(get-in % [:action :target]) before)
            after-order (mapv #(get-in % [:action :target]) after)]
        (is (= ["M-a" "M-b"] before-order))
        (is (= before-order after-order)))
      (finally
        (evict-ids! (:star ids) (:status ids))))))

(deftest witness-live-positive-uses-real-substrate-facts-test
  (let [ids (fixture-ids)
        opts (rank-opts (:cap ids))
        g (graph (:cap ids))]
    (try
      (put-docs! (star-doc ids) (discharge-doc ids))
      (a6/flip-star-status-on-discharge! opts)
      (let [status-before (a6/status-rows opts)
            result (a6/witness-live g opts)
            status-after (a6/status-rows opts)
            closure (get-in result [:witness :closure])]
        (is (= status-before status-after)
            "witness-live is read-only; it observes the status written by A6")
        (is (= [[(:discharge ids) (:cap ids)]]
               (get-in result [:observation :discharges]))
            "discharge fact came from the real substrate")
        (is (= ["M-a" "M-b"] (get-in result [:observation :before-order])))
        (is (= ["M-b" "M-a"] (get-in result [:observation :after-order])))
        (is (true? (:witnessed? closure)))
        (is (= [[(:discharge ids) (:cap ids) "M-a" "M-b"]]
               (:bindings closure))))
      (finally
        (evict-ids! (:discharge ids) (:star ids) (:status ids))))))

(deftest witness-live-negative-mirror-test
  (let [ids (fixture-ids)
        opts (rank-opts (:cap ids))
        g (graph (:cap ids))]
    (try
      (put-docs! (star-doc ids) (discharge-doc ids))
      (let [result (a6/witness-live g opts)
            closure (get-in result [:witness :closure])]
        (is (= [[(:discharge ids) (:cap ids)]]
               (get-in result [:observation :discharges])))
        (is (= (get-in result [:observation :before-order])
               (get-in result [:observation :after-order])))
        (is (false? (:witnessed? closure)))
        (is (empty? (:bindings closure))))
      (finally
        (evict-ids! (:discharge ids) (:star ids) (:status ids))))))

(deftest operational-witness-remains-pure-test
  (let [source (slurp "/home/joe/code/futon2/src/futon2/aif/operational_witness.clj")]
    (is (not (re-find #"drawbridge|submit-tx|slurp|http|actuator-a6|actuator-a3" source)))))
