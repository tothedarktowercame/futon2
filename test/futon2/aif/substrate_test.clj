(ns futon2.aif.substrate-test
  (:require [clojure.test :refer [deftest is testing]]
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
    (is (= :outcome-ref
           (:relation/type (first (substrate/relations {:type :outcome-ref} opts)))))
    (is (= [{:xt/id "e" :entity/type :test}]
           (do (substrate/put-doc! {:xt/id "e" :entity/type :test} opts)
               @written)))))

(deftest unsupported-transaction-operations-fail-loudly
  (testing "there is no backend-specific evict escape hatch"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"unsupported substrate operation"
         (substrate/submit-puts! [[:xtdb.api/evict "e"]]
                                 {:put-doc-fn identity})))))
