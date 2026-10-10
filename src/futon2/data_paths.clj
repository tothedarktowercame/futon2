(ns futon2.data-paths
  "Single authority for paths below futon2's mutable data tree.

  Production uses the canonical checkout.  Tests bind `*data-root*` to a
  fresh directory, so defaults remain hermetic even when the process cwd is
  the production checkout or a git worktree."
  (:require [clojure.java.io :as io])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def production-data-root "/home/joe/code/futon2/data")

(def production-repo-root
  "Canonical futon2 checkout root. Code/config paths must resolve here, never
  against the serving JVM's process working directory."
  (.getPath (.getParentFile (io/file production-data-root))))

(def production-code-root
  "Canonical parent containing the futon repositories."
  (.getPath (.getParentFile (io/file production-repo-root))))

(defn repo-path [& parts]
  (str (apply io/file production-repo-root parts)))

(defn resolve-repo-path
  "Resolve P absolutely. Relative paths are rooted at canonical futon2,
  never at the process working directory."
  [p]
  (let [f (io/file p)]
    (.getPath (if (.isAbsolute f) f (io/file production-repo-root p)))))

(defn code-path [& parts]
  (str (apply io/file production-code-root parts)))

(def test-mode?
  (= "true" (System/getProperty "futon2.data-root.test")))

(defn- fresh-test-root []
  (if-let [configured (System/getProperty "futon2.data-root.test.path")]
    (let [f (.getCanonicalFile (io/file configured))
          production (.getCanonicalFile (io/file production-data-root))]
      (when (= f production)
        (throw (ex-info "Test data root cannot be the production data root"
                        {:failure-kind :test-data-root-is-production
                         :path (.getPath f)})))
      (when-not (.isDirectory f)
        (throw (ex-info "Configured test data root does not exist"
                        {:failure-kind :test-data-root-missing
                         :path (.getPath f)})))
      (.getPath f))
    (str (.toFile (Files/createTempDirectory
                   "futon2-test-data-" (make-array FileAttribute 0))))))

(def test-data-root
  "One process-visible mutable-data root per test JVM."
  (when test-mode? (fresh-test-root)))

(def ^:dynamic *data-root*
  (or test-data-root production-data-root))

(defn- production-snapshot []
  (let [root (io/file production-data-root)]
    (if-not (.isDirectory root)
      (sorted-map)
      (into (sorted-map)
            (comp (filter #(.isFile ^java.io.File %))
                  (map (fn [^java.io.File file]
                         [(.toString (.relativize (.toPath root) (.toPath file)))
                          [(.length file) (.lastModified file)]])))
            (file-seq root)))))

(defn- changed-paths [before after]
  (->> (into #{} (concat (keys before) (keys after)))
       (filter #(not= (get before %) (get after %)))
       sort vec))

(defonce ^:private production-at-test-start
  (when test-mode? (production-snapshot)))

(defonce ^:private production-guard
  (when test-mode?
    (let [hook
          (Thread.
           (fn []
             (let [changed (changed-paths production-at-test-start
                                          (production-snapshot))]
               (when (seq changed)
                 (binding [*out* *err*]
                   (println "FUTON2 TEST HERMETICITY FAILURE: production data changed")
                   (doseq [path changed] (println path))
                   (flush))
                 ;; Shutdown hooks cannot change an already-selected exit
                 ;; status. halt is deliberate and test-mode-only.
                 (.halt (Runtime/getRuntime) 86))))
           "futon2-production-data-guard")]
      (.addShutdownHook (Runtime/getRuntime) hook)
      hook)))

(defn path
  "Resolve PARTS beneath the currently bound futon2 data root."
  [& parts]
  (str (apply io/file *data-root* parts)))
