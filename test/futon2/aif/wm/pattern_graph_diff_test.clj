(ns futon2.aif.wm.pattern-graph-diff-test
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.selected-want-outcome :as want-outcome]
            [futon2.aif.token-outcome :as token-outcome]
            [futon2.aif.wm.pattern-graph-diff :as graph-diff]))

(def expected-path
  "test/fixtures/pattern-graph-diff/2026-10-05-c9d25d6a.expected-diff.json")
(def parts-path
  "test/fixtures/pattern-graph-diff/2026-10-05-c9d25d6a-run-parts.edn")
(def action-path
  "test/fixtures/selected-want-outcome/2026-10-05-c9d25d6a-action.edn")

(defn- read-edn [path] (edn/read-string (slurp path)))
(defn- expected [] (json/parse-string (slurp expected-path)))
(defn- recorded-input []
  (let [parts (read-edn parts-path)
        action (:action (read-edn action-path))]
    (assoc parts :selected-action action :graph (get (expected) "base"))))
(defn- json-round-trip [value]
  (json/parse-string (json/generate-string value)))

(defn- accounting [reached]
  (let [{:keys [action selection-certificate]} (read-edn action-path)
        prediction (token-outcome/freeze-prediction
                    {:action action :selection-certificate selection-certificate})
        artifact-sha "pattern-graph-diff-test-artifact"
        measurements
        (mapv (fn [{:keys [token]}]
                (let [locator (get (:observation-locators action) token)]
                  {:token token :declared-locator locator
                   :after-locator (assoc locator :sha artifact-sha)
                   :result {:observed (contains? reached token)
                            :evidence {:resolved-sha artifact-sha}}}))
              (:wanted prediction))]
    (want-outcome/receipt
     {:selected-action action
      :token-comparison (token-outcome/compare-outcomes
                         prediction measurements artifact-sha)})))

(deftest recorded-run-matches-the-reviewed-diff
  (is (= (expected)
         (json-round-trip (graph-diff/pattern-graph-diff (recorded-input))))))

(deftest verified-accounting-attaches-each-want-outcome-to-its-pattern
  (let [input (recorded-input)
        action (:selected-action input)
        reached [(:target action) (first (:want action))]
        proposal (graph-diff/pattern-graph-diff
                  (assoc input :want-outcome-accounting (accounting #{reached})))
        outcomes (into {} (mapcat (fn [{:keys [wants]}]
                                    (map (juxt :want :outcome) wants))
                                  (:add_uses proposal)))]
    (is (= "reached" (get outcomes (str (namespace (second reached)) "/"
                                           (name (second reached))))))
    (is (= 3 (count (filter #(= "untouched" %) (vals outcomes)))))))

(deftest changed-enacted-action-sha-attests-nothing
  (let [proposal (graph-diff/pattern-graph-diff
                  (assoc-in (recorded-input)
                            [:d-task-enactment :verification :candidate-to-minted-join
                             :enacted-action-sha256]
                            "changed"))]
    (is (= [] (:add_uses proposal)))
    (is (= [] (:add_edges proposal)))
    (is (= "enactment-not-verified" (:nothing_to_add proposal)))))
