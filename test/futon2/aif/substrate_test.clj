(ns futon2.aif.substrate-test
  (:require [babashka.http-client :as http]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.substrate :as substrate]))

(deftest canonical-url-normalizes-legacy-api-base
  (is (= "http://store.test:7073"
         (substrate/configured-url
          {:substrate-url "http://store.test:7073/api/alpha/"}))))

(deftest semantic-operations-have-hermetic-injection-seams
  (let [written (atom [])
        opts {:inhabitation-fn
              (fn [bindings]
                (mapv #(assoc % :inhabited? (= :present (:type %))) bindings))
              :entities-by-type-fn
              (fn [type] [{:entity/id "e" :entity/type type}])
              :entity-by-id-fn
              (fn [id] {:entity/id id :entity/type :test})
              :relations-fn
              (fn [filters] [{:relation/id "r" :relation/type (:type filters)}])
              :put-doc-fn
              (fn [doc] (swap! written conj doc) {:ok true})}]
    (is (= [true false]
           (mapv :inhabited?
                 (substrate/inhabitation
                  [{:kind :entity :type :present}
                   {:kind :entity :type :absent}]
                  opts))))
    (is (= :capability
           (:entity/type (first (substrate/entities-by-type :capability opts)))))
    (is (= "specific" (:entity/id (substrate/entity-by-id "specific" opts))))
    (is (= :outcome-ref
           (:relation/type (first (substrate/relations {:type :outcome-ref} opts)))))
    (is (= [{:xt/id "e" :entity/type :test}]
           (do (substrate/put-doc! {:xt/id "e" :entity/type :test} opts)
               @written)))))

(deftest entities-by-type-asks-for-neither-total-nor-cursor
  ;; This client reads a whole type in one page and returns :entities only, so
  ;; the two scans behind :count and :next-cursor are pure cost
  ;; (futon1b/TN-entities-speedups-2026-09-26.md). Pinned because the saving is
  ;; in the URL: drop either param and the substrate goes back to scanning.
  (let [seen (atom nil)]
    (with-redefs [http/request
                  (fn [req]
                    (reset! seen (:uri req))
                    {:status 200 :body (pr-str {:entities [{:entity/id "e"}]})})]
      (is (= [{:entity/id "e"}]
             (substrate/entities-by-type "mission"
                                         {:substrate-url "http://store.test:7073"
                                          :limit 1000})))
      (is (= (str "http://store.test:7073/api/alpha/entities?type=mission"
                  "&limit=1000&include-total=false&ordered=false")
             @seen)))))

(deftest unsupported-transaction-operations-fail-loudly
  (testing "there is no backend-specific evict escape hatch"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"unsupported substrate operation"
         (substrate/submit-puts! [[:xtdb.api/evict "e"]]
                                 {:put-doc-fn identity})))))

(deftest futon1b-hyperedge-wire-shapes-normalize-test
  (with-redefs [http/request
                (fn [_]
                  {:status 200
                   :body (pr-str
                          {:hyperedges
                           [{:hx/type "code/v05/mission-doc"
                             :hx/ends [{:entity-id "repo-d/mission/x"}]
                             :hx/props (pr-str {:mission/id "M-x"})}]})})]
    (let [[hx] (substrate/hyperedges-by-type "code/v05/mission-doc"
                                             {:substrate-url "http://store.test:7073"})]
      (is (= ["repo-d/mission/x"] (:hx/endpoints hx)))
      (is (= {:mission/id "M-x"} (:hx/props hx))))))

(deftest hyperedges-by-end-filters-served-type-routes-test
  (let [seen (atom [])
        opts {:types [:mission-doc :related-mission]
              :hyperedges-by-type-fn
              (fn [type]
                (swap! seen conj type)
                [{:hx/type type :hx/endpoints ["other" "target"]}
                 {:hx/type type :hx/endpoints ["other"]}])}
        found (substrate/hyperedges-by-end "target" opts)]
    (is (= [:mission-doc :related-mission] @seen))
    (is (= 2 (count found)))
    (is (every? #(some #{"target"} (:hx/endpoints %)) found))))
