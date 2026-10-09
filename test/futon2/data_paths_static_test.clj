(ns futon2.data-paths-static-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]))

(defn- source-files []
  (->> (file-seq (io/file "src"))
       (filter #(.isFile ^java.io.File %))
       (filter #(str/ends-with? (.getName ^java.io.File %) ".clj"))))

(defn- offending-lines [file]
  (let [relative-path (.toString (.relativize (.toPath (io/file "."))
                                              (.toPath file)))]
    (when-not (= "src/futon2/data_paths.clj" relative-path)
      (->> (str/split-lines (slurp file))
           (map-indexed vector)
           (keep (fn [[idx line]]
                   (when (or (str/includes? line "/home/joe/code/futon2/data")
                             (and (str/includes? line "/code/futon2/data/")
                                  ;; Documentation may spell the public live
                                  ;; layout; this rule governs executable roots.
                                  (not (str/includes? line "~/code/futon2/data/")))
                             (and (str/includes? line "\"data/")
                                  ;; A substring classifier, not a path default.
                                  (not (str/includes? line "\"data/fold-turns\""))))
                     {:file relative-path :line (inc idx) :text (str/trim line)})))))))

(deftest mutable-data-roots-go-through-data-paths
  (let [offenders (vec (mapcat offending-lines (source-files)))]
    (is (empty? offenders)
        (str "src may not define CWD-relative or canonical production data roots; "
             "use futon2.data-paths/path: " (pr-str offenders)))))
