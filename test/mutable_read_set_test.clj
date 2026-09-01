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
      (is (= {:status :present} (:text-status entry)))
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
                          #"claim not satisfied"
                          (read-set/require-claim! observation :content-current)))))

(deftest non-utf8-keeps-authoritative-bytes-without-lossy-text
  (let [dir (Files/createTempDirectory "mutable-read-set-binary-" (make-array java.nio.file.attribute.FileAttribute 0))
        path (.resolve dir "binary.dat")
        bytes (byte-array [(unchecked-byte 0xc3) (unchecked-byte 0x28)])]
    (Files/write path bytes (make-array java.nio.file.OpenOption 0))
    (let [observation (read-set/observe-files [(str path)])
          entry (read-set/entry-by-path (:snapshot observation) (str path))]
      (is (= :stable (:status observation)))
      (is (= {:status :absent :reason :non-utf8} (:text-status entry)))
      (is (not (contains? entry :text)))
      (is (= (:sha256 entry) (read-set/sha256-bytes bytes))))))

(deftest unavailable-reasons-are-typed
  (let [dir (Files/createTempDirectory "mutable-read-set-reasons-" (make-array java.nio.file.attribute.FileAttribute 0))
        path (.resolve dir "subject.edn")]
    (spit (str path) "{:state :captured}\n")
    (let [absent (read-set/observe-files
                  [(str path)]
                  {:after-capture (fn [_] (Files/delete path))})]
      (is (= :absent (get-in absent [:comparison 0 :reason]))))
    (spit (str path) "{:state :captured}\n")
    (let [wrong-kind (read-set/observe-files
                      [(str path)]
                      {:after-capture (fn [_]
                                       (Files/delete path)
                                       (Files/createDirectory path
                                                              (make-array java.nio.file.attribute.FileAttribute 0)))})]
      (is (= :wrong-kind (get-in wrong-kind [:comparison 0 :reason]))))
    (Files/delete path)
    (spit (str path) "{:state :captured}\n")
    (let [calls (atom 0)
          unreadable (with-redefs [read-set/read-bytes
                                   (fn [p]
                                     (if (= 1 (swap! calls inc))
                                       (Files/readAllBytes (java.nio.file.Paths/get (str p) (make-array String 0)))
                                       (throw (java.nio.file.AccessDeniedException. (str p)))))]
                       (read-set/observe-files [(str path)]))]
      (is (= :unreadable (get-in unreadable [:comparison 0 :reason]))))))

(deftest aba-is-content-current-but-not-event-free
  (let [dir (Files/createTempDirectory "mutable-read-set-aba-" (make-array java.nio.file.attribute.FileAttribute 0))
        path (str (.resolve dir "aba.edn"))
        before "{:state :a}\n"
        observation (do
                      (spit path before)
                      (read-set/observe-files
                       [path]
                       {:after-capture (fn [_]
                                         (spit path "{:state :b}\n")
                                         (spit path before))}))
        content (read-set/assess-claim observation :content-current)
        events (read-set/assess-claim observation :event-free)]
    (is (:endpoint-equal? observation))
    (is (= :satisfied (:verdict content)))
    (is (= true (:content-current? content)))
    (is (= :unverified (:verdict events)))
    (is (= :unverified (:event-free? events)))
    (is (= false (:distinguishable-cause? events)))))
