(ns futon2.aif.meta-self-heal-phase-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.meta-self-heal-production :as meta-self-heal]))

(defn- observed-step [entry]
  (if (= :route entry)
    entry
    [(:phase entry) (:transition entry) (:outcome entry)]))

(deftest meta-self-heal-route-reports-phase-boundaries
  (testing "successful route emits start before the call and end after it"
    (let [observed (atom [])
          opts {:phase-log-fn #(swap! observed conj %)}]
      (with-redefs [meta-self-heal/route!
                    (fn [_]
                      (swap! observed conj :route)
                      {:status :routed})]
        (is (= {:status :routed}
               (#'runtime/run-meta-self-heal-route! opts))))
      (is (= [[:meta-self-heal :start nil]
              :route
              [:meta-self-heal :end :ok]]
             (mapv observed-step @observed)))))
  (testing "throwing route still emits an error end event"
    (let [observed (atom [])
          boom (ex-info "route failed" {})
          opts {:phase-log-fn #(swap! observed conj %)}]
      (with-redefs [meta-self-heal/route!
                    (fn [_]
                      (swap! observed conj :route)
                      (throw boom))]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"route failed"
                              (#'runtime/run-meta-self-heal-route! opts))))
      (is (= [[:meta-self-heal :start nil]
              :route
              [:meta-self-heal :end :error]]
             (mapv observed-step @observed))))))
