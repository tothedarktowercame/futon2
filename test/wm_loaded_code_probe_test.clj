(ns wm-loaded-code-probe-test
  (:require [clojure.test :refer [deftest is]]
            [wm-loaded-code-probe :as probe]))

(deftest presence-is-not-loaded-identity
  (is (= :unknown (:loaded-current? (probe/classify {:disk-sha256 "current"} {:loaded? true})))))

(deftest transitively-order-through-unchanged-dependencies
  (is (= ['a 'c] (probe/dependency-order {'c #{'b} 'b #{'a} 'a #{}} ['c 'a]))))

(deftest cycles-refuse
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"dependency cycle"
                       (probe/dependency-order {'a #{'b} 'b #{'a}} ['a]))))

(deftest prefix-requires-and-explicit-base
  (is (= ['a.b 'a.c] (probe/lib-names '(a [b :as b] [c]))))
  (is (= ['a.b 'a.c] (probe/lib-names '(a b [c]))))
  (is (= :loaded-base-unavailable
         (try (probe/probe "/unused" nil "/unused")
              (catch clojure.lang.ExceptionInfo e (:kind (ex-data e)))))))

(deftest generated-probe-is-read-only-core-form
  (let [form (read-string (probe/read-only-form ['futon2.aif.policy]))
        symbols (set (filter symbol? (tree-seq coll? seq form)))]
    (is (= 'let (first form)))
    (is (not-any? symbols '[require load-file eval def intern alter-var-root swap! reset!]))))
