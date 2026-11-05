(ns futon2.test-support.git-fixture
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]))

(def routing-environment-keys
  ["GIT_DIR" "GIT_WORK_TREE" "GIT_INDEX_FILE" "GIT_COMMON_DIR"])

(defn environment
  "The parent environment without variables that redirect Git to another repo."
  []
  (apply dissoc (into {} (System/getenv)) routing-environment-keys))

(defn git-result [repo & args]
  (apply shell/sh
         (concat ["git" "-C" (str repo)] args [:env (environment)])))

(defn git! [repo & args]
  (let [result (apply git-result repo args)]
    (when-not (zero? (:exit result))
      (throw (ex-info "Git fixture command failed"
                      (assoc result :repo (str repo) :args args))))
    (str/trim (:out result))))

(defn delete-tree!
  "Remove ROOT recursively. Intended for roots created by with-temp-repo."
  [root]
  (doseq [f (reverse (file-seq (io/file root)))]
    (io/delete-file f true)))

(defn with-temp-repo
  "Call F with a fresh directory under the OS temporary root and always
  remove it, including when F throws. PREFIX identifies the fixture producer."
  [prefix f]
  (let [dir (.toFile
             (java.nio.file.Files/createTempDirectory
              prefix (make-array java.nio.file.attribute.FileAttribute 0)))]
    (try
      (f dir)
      (finally
        (delete-tree! dir)))))
