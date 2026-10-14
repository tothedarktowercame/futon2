(ns build
  (:refer-clojure :exclude [test])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]))

(def lib 'futon2/futon2)
(def version "0.1.0-SNAPSHOT")
(def main 'futon2.aif.full-loop-cli)
(def class-dir "target/classes")
(def test-batch-size 8)

(defn- test-namespaces []
  (->> (file-seq (io/file "test"))
       (filter #(.isFile %))
       (map #(.getPath %))
       (filter #(re-find #"_test\.clj$" %))
       (map #(-> %
                 (subs (inc (count "test")))
                 (str/replace #"\.clj$" "")
                 (str/replace java.io.File/separator ".")
                 (str/replace "_" "-")
                 symbol))
       sort
       vec))

(defn- run-test-batch [basis batch-index namespaces]
  (println (format "\nCI test batch %d: %d namespaces" batch-index (count namespaces)))
  (let [main-args (into ["-m" "cognitect.test-runner"]
                        (mapcat (fn [namespace] ["-n" (str namespace)]) namespaces))
        command (b/java-command {:basis basis
                                 :main 'clojure.main
                                 :main-args main-args})]
    (:exit (b/process command))))

(defn test "Run all the tests." [opts]
  ;; A single runner retains every namespace-global executor until process
  ;; exit.  The suite has grown beyond the bounded CI task budget, so run the
  ;; same namespace set in sequential JVM batches.  A failed batch stops CI;
  ;; each completed JVM also proves its processes and threads were reaped.
  (let [basis (b/create-basis {:aliases [:test]})
        namespaces (test-namespaces)]
    (when (empty? namespaces)
      (throw (ex-info "No test namespaces discovered" {})))
    (doseq [[batch-index batch] (map-indexed vector (partition-all test-batch-size namespaces))]
      (when-not (zero? (run-test-batch basis (inc batch-index) batch))
        (throw (ex-info "Tests failed" {:batch (inc batch-index)
                                         :namespaces batch})))))
  opts)

(defn warranted-test
  "Check current namespace warrants and run only namespaces whose evidence is stale."
  [opts]
  (let [result (b/process {:command-args ["python3" "scripts/warrant_suite.py"
                                          "--pinned" "HEAD" "--author" "futon2-ci"
                                          "-j" "6"]
                           :out :inherit :err :inherit})]
    (when-not (zero? (:exit result))
      (throw (ex-info "Warranted tests failed" {:exit (:exit result)}))))
  opts)

(defn- uber-opts [opts]
  (assoc opts
         :lib lib :main main
         :uber-file (format "target/%s-%s.jar" (name lib) version)
         :basis (b/create-basis {})
         :class-dir class-dir
         :src-dirs ["src"]
         :ns-compile [main]))

(defn uber "Build the runnable Futon2 uberjar without running tests." [opts]
  (b/delete {:path "target"})
  (let [opts (uber-opts opts)]
    (println "\nCopying source...")
    (b/copy-dir {:src-dirs ["resources" "src"] :target-dir class-dir})
    (println (str "\nCompiling " main "..."))
    (b/compile-clj opts)
    (println "\nBuilding JAR...")
    (b/uber opts))
  opts)

(defn ci "Run the CI pipeline of tests (and build the uberjar)." [opts]
  (warranted-test opts)
  (uber opts))
