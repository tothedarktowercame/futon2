(ns futon2.aif.cause-scope-digest-test
  "The scope digest must not depend on the printing JVM's bindings: the serving
  JVM mints it, a fresh test JVM checks it (clicks 7-8 of 2026-11-01)."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cause-scope :as scope]
            [futon2.aif.load-identity :as identity]))

(def ^:private record
  {:run/id "r" :failure {:kind :untyped-failure :stage :ticket-readiness-refresh}
   :runner-execution/identity {:kind :runner-execution :id "ea1"}
   :world-at-selection {:open-tasks {:missions {:ids ["M-a" "M-b"]}}}
   :nested {:a/b 1 :a/c 2} :long (vec (range 300))})

(deftest digest-is-the-same-under-either-namespace-map-binding
  (let [a (binding [*print-namespace-maps* true] (scope/stable-digest record))
        b (binding [*print-namespace-maps* false] (scope/stable-digest record))
        c (binding [*print-length* 3 *print-level* 2] (scope/stable-digest record))]
    (is (= a b c))
    (is (re-matches #"[0-9a-f]{64}" a))))

(deftest plain-pr-str-did-depend-on-the-binding
  ;; the bug, constructed: a map with namespaced keys prints differently
  (let [plain (fn [] (identity/sha256 (.getBytes (pr-str record) "UTF-8")))
        a (binding [*print-namespace-maps* true] (plain))
        b (binding [*print-namespace-maps* false] (plain))]
    (is (not= a b))
    (is (= b (scope/stable-digest record)) "the stable digest is the serving JVM's value")))
