(ns futon2.aif.wm.injury49-learning-read-bound-test
  "Acceptance boundary for injury 49: production learning fits a bounded JVM."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.outer-learning-manifest :as learning-manifest])
  (:import [java.util.concurrent TimeUnit]))

(use-fixtures :once hermetic/with-hermetic-stores)

(def live-manifest-path learning-manifest/default-path)

(def child-form
  (str "(require '[futon2.aif.outer-learning-runtime :as runtime]) "
       "(let [result (runtime/load-shadow)] "
       "(prn {:result (select-keys result [:status]) "
       ":dataset-status (get-in result [:dataset :status]) "
       ":record-read (get-in result [:dataset :counts :record-read])}))"))

(defn- last-nonblank-line [text]
  (last (remove str/blank? (str/split-lines text))))

(defn- run-bounded-child []
  (let [started (System/nanoTime)
        process (-> (ProcessBuilder.
                     ^java.util.List ["clojure" "-J-Xmx1g" "-M" "-e" child-form])
                    (.directory (io/file (System/getProperty "user.dir")))
                    .start)
        stdout-future (future (slurp (.getInputStream process)))
        stderr-future (future (slurp (.getErrorStream process)))
        finished? (.waitFor process 300 TimeUnit/SECONDS)]
    (when-not finished?
      (.destroyForcibly process)
      (.waitFor process))
    (let [stdout @stdout-future
          stderr @stderr-future
          last-line (last-nonblank-line stdout)
          parsed (when last-line
                   (try (edn/read-string last-line)
                        (catch Exception _ ::invalid)))
          result {:status (if finished? :completed :timed-out)
                  :exit (when finished? (.exitValue process))
                  :wall-ms (quot (- (System/nanoTime) started) 1000000)
                  :last-line last-line
                  :parsed parsed}]
      (println "INJURY49_CHILD_RESULT" (pr-str result))
      (println "INJURY49_CHILD_STDERR_BEGIN")
      (print stderr)
      (when-not (or (empty? stderr) (str/ends-with? stderr "\n")) (println))
      (println "INJURY49_CHILD_STDERR_END")
      result)))

(deftest live-learning-dataset-fits-one-gibibyte
  (let [{:keys [status exit parsed]} (run-bounded-child)
        record-read (:record-read parsed)]
    (is (= :completed status))
    (is (zero? exit))
    (is (map? parsed))
    (is (= :produced (:dataset-status parsed)))
    (is (= 0 (:missing record-read)))
    (is (= 0 (:drifted record-read)))
    (is (and (map? record-read)
             (= (:readable record-read)
                (+ (:verified record-read) (:excluded record-read)))))))

(deftest live-learning-projections-are-small
  (let [entries (:entries (learning-manifest/read-manifest live-manifest-path))]
    (when (some :projection entries)
      (is (every? (fn [{:keys [projection]}]
                    (or (nil? projection)
                        (let [file (io/file (:path projection))]
                          (and (.isFile file) (<= (.length file) 65536)))))
                  entries)))))
