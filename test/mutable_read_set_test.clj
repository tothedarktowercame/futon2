(ns mutable-read-set-test
  (:require [checks.mutable-read-set :as read-set]
            [clojure.test :refer [deftest is]])
  (:import [java.nio.file Files]))

(deftest digest-and-text-come-from-one-read
  (let [dir (Files/createTempDirectory "mutable-read-set-" (make-array java.nio.file.attribute.FileAttribute 0))
        path (str (.resolve dir "one.edn"))]
    (spit path "{:value 1}\n")
    (let [observation (read-set/observe-files [path])
          entry (read-set/entry-by-path (:snapshot observation) path)]
      (is (= :stable (:status observation)))
      (is (= "{:value 1}\n" (:text entry)))
      (is (= (:sha256 entry)
             (read-set/sha256-bytes (.getBytes (:text entry) "UTF-8")))))))

(deftest movement-is-a-result-not-a-hybrid-verdict
  (let [dir (Files/createTempDirectory "mutable-read-set-move-" (make-array java.nio.file.attribute.FileAttribute 0))
        path (str (.resolve dir "moving.edn"))
        observation (do
                      (spit path "{:state :before}\n")
                      (read-set/observe-files
                       [path]
                       {:after-capture (fn [_] (spit path "{:state :after}\n"))}))]
    (is (= :moved (:status observation)))
    (is (= :changed (get-in observation [:comparison 0 :status])))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"moved during observation"
                          (read-set/require-stable! observation)))))
