(ns futon2.data-paths
  "Single authority for paths below futon2's mutable data tree.

  Production uses the canonical checkout.  Tests bind `*data-root*` to a
  fresh directory, so defaults remain hermetic even when the process cwd is
  the production checkout or a git worktree."
  (:require [clojure.java.io :as io])
  (:import [java.nio.file Files Path Paths]
           [java.nio.file.attribute FileAttribute]))

(defn configured-data-root
  "Select the mutable futon2 authority for one composed serving process.
   An explicit FUTON2_DATA_ROOT wins; otherwise an isolated WM code root owns
   its sibling futon2/data. With neither, preserve the canonical authority."
  [data-root code-root]
  (or (some-> data-root str not-empty)
      (some-> code-root str not-empty (io/file "futon2" "data") str)
      "/home/joe/code/futon2/data"))

(def production-data-root
  (configured-data-root (System/getenv "FUTON2_DATA_ROOT")
                        (System/getenv "FUTON_WM_CODE_ROOT")))

(def production-repo-root
  "Canonical futon2 checkout root. Code/config paths must resolve here, never
  against the serving JVM's process working directory."
  (.getPath (.getParentFile (io/file production-data-root))))

(def production-code-root
  "Canonical parent containing the futon repositories."
  (.getPath (.getParentFile (io/file production-repo-root))))

(defn local-path
  "Resolve a retained canonical host path into this serving composition.

  Paths below /home/joe/code follow production-code-root. Paths below
  /home/joe/notes follow FUTON_WM_NOTES_ROOT when it is non-empty. Matching is
  by Path components, so similarly prefixed names such as /home/joe/codex are
  not relocated. Relative and unrelated absolute paths are unchanged."
  ([p]
   (local-path p production-code-root (System/getenv "FUTON_WM_NOTES_ROOT")))
  ([p code-root notes-root]
   (let [path (Paths/get (str p) (make-array String 0))
         code-prefix (Paths/get "/home/joe/code" (make-array String 0))
         notes-prefix (Paths/get "/home/joe/notes" (make-array String 0))
         relocate (fn [^Path prefix root]
                    (str (.resolve (Paths/get root (make-array String 0))
                                   (.relativize prefix path))))]
     (cond
       (and (.isAbsolute path) (.startsWith path code-prefix))
       (relocate code-prefix code-root)

       (and (.isAbsolute path) (.startsWith path notes-prefix)
            (some-> notes-root str not-empty))
       (relocate notes-prefix notes-root)

       :else (str p)))))

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
  (str (.toFile (Files/createTempDirectory
                 "futon2-test-data-" (make-array FileAttribute 0)))))

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
                   ;; LOG-DELETE-1: two agents read this list as their own
                   ;; test artifacts and deleted live WM click logs.  The
                   ;; comparison is metadata only and cannot tell a test
                   ;; write from a concurrent production writer.
                   (println (str "These paths changed during the test JVM. They may be"
                                 " concurrent production writes (for example a live WM"
                                 " click log), not test artifacts. DO NOT DELETE OR"
                                 " MOVE THEM. Rerun the test when production writers"
                                 " are idle."))
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
