(ns futon2.aif.wm.pattern-graph-diff-test
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as load-identity]
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

(defn- temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "pattern-graph-diff" (make-array java.nio.file.attribute.FileAttribute 0))))

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
    (is (= 3 (count (filter #(= "untouched" %) (vals outcomes)))))
    (let [producer (some (fn [pattern]
                           (when (contains? (:produces pattern) reached)
                             (str (namespace (:id pattern)) "/" (name (:id pattern)))))
                         (:precedence action))
          carrier (some (fn [{:keys [pattern wants]}]
                          (when (some #(= "reached" (:outcome %)) wants) pattern))
                        (:add_uses proposal))]
      (is (some? producer))
      (is (= producer carrier) "the reached outcome sits on the pattern that produces the want"))))

(def ^:private verified-join
  {:verification {:status :admitted
                  :candidate-to-minted-join {:status :verified
                                             :selected-action-sha256 "same"
                                             :enacted-action-sha256 "same"}}})

(deftest a-pattern-with-two-wants-carries-both-outcomes
  ;; Authored input: no recorded run so far has a pattern with two wants or a
  ;; pattern with none.
  (let [proposal (graph-diff/pattern-graph-diff
                  {:run-id "r" :target "M-x"
                   :selected-action
                   {:target "M-x"
                    :precedence [{:id :p/one :produces #{["M-x" :b] ["M-x" :a]}}
                                 {:id :p/two :produces #{}}]}
                   :d-task-enactment verified-join
                   :want-outcome-accounting
                   {:status :verified
                    :by-class {:reached [["M-x" :a]] :untouched [["M-x" :b]]}}
                   :graph {:path "g" :sha256 "0"}})]
    (is (= [{:pattern "p/one" :position 1
             :wants [{:want "a" :outcome "reached"} {:want "b" :outcome "untouched"}]}
            {:pattern "p/two" :position 2 :wants []}]
           (:add_uses proposal)))
    (is (= 1 (count (:add_edges proposal))))))

(deftest bare-want-tokens-are-read-against-the-action-target
  (let [proposal (graph-diff/pattern-graph-diff
                  {:run-id "r"
                   :selected-action {:target "M-x"
                                     :precedence [{:id :p/one :produces #{:a}}]}
                   :d-task-enactment verified-join
                   :want-outcome-accounting
                   {:status :verified :by-class {:reached [["M-x" :a]]}}
                   :graph {:path "g" :sha256 "0"}})]
    (is (= [{:pattern "p/one" :position 1 :wants [{:want "a" :outcome "reached"}]}]
           (:add_uses proposal)))
    (is (= "M-x" (get-in proposal [:source :target])))))

(deftest changed-enacted-action-sha-attests-nothing
  (let [proposal (graph-diff/pattern-graph-diff
                  (assoc-in (recorded-input)
                            [:d-task-enactment :verification :candidate-to-minted-join
                             :enacted-action-sha256]
                            "changed"))]
    (is (= [] (:add_uses proposal)))
    (is (= [] (:add_edges proposal)))
    (is (= "enactment-not-verified" (:nothing_to_add proposal)))))

(deftest publish-writes-a-pinned-diff-and-counts-its-additions
  (let [dir (temp-dir) graph-file (io/file dir "graph.json")
        output-dir (io/file dir "out")
        graph-bytes (.getBytes "{\"pattern_ids\":[]}" "UTF-8")
        _ (spit graph-file (String. graph-bytes "UTF-8"))
        input (dissoc (recorded-input) :graph)
        result (graph-diff/publish! input (.getPath graph-file) (.getPath output-dir))
        written (json/parse-string (slurp (:path result)))
        graph {:path (.getPath graph-file) :sha256 (load-identity/sha256 graph-bytes)}]
    (is (= :written (:status result)))
    (is (= 4 (:uses result)))
    (is (= 6 (:links result)))
    (is (= (json-round-trip (graph-diff/pattern-graph-diff (assoc input :graph graph)))
           written))
    (is (= (:sha256 result)
           (load-identity/sha256
            (java.nio.file.Files/readAllBytes (.toPath (io/file (:path result)))))))))

(deftest publish-needs-a-run-id-that-can-name-a-file
  (let [dir (temp-dir) graph-file (io/file dir "graph.json")
        output-dir (io/file dir "out")
        _ (spit graph-file "{}")
        input (dissoc (recorded-input) :graph)]
    (doseq [run-id [nil "" "../outside" "a/b"]]
      (let [result (graph-diff/publish! (assoc input :run-id run-id)
                                        (.getPath graph-file) (.getPath output-dir))]
        (is (= :not-written (:status result)) (pr-str run-id))
        (is (= :run-id-unusable-as-file-name (:reason result)) (pr-str run-id))))
    (is (not (.exists output-dir)))
    (is (= ["graph.json"] (vec (.list dir))))))

(deftest publish-reports-an-unreadable-graph-without-writing
  (let [dir (temp-dir) output-dir (io/file dir "out")
        missing (io/file dir "missing.json")
        result (graph-diff/publish! (dissoc (recorded-input) :graph)
                                    (.getPath missing) (.getPath output-dir))]
    (is (= {:status :not-written :reason :pattern-graph-unreadable
            :path (.getPath missing)} result))
    (is (not (.exists output-dir)))))

(deftest publish-turns-an-output-path-failure-into-a-typed-result
  (let [dir (temp-dir) graph-file (io/file dir "graph.json")
        not-directory (io/file dir "regular-file")
        _ (spit graph-file "{}")
        _ (spit not-directory "occupied")
        result (graph-diff/publish! (dissoc (recorded-input) :graph)
                                    (.getPath graph-file) (.getPath not-directory))]
    (is (= :not-written (:status result)))
    (is (= :pattern-graph-diff-write-failed (:reason result)))
    (is (= "occupied" (slurp not-directory)))))
