(ns futon2.aif.reading-policy-class-history-index-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.reading-policy-class-free-energy :as class-f]
            [futon2.aif.reading-policy-class-history-index :as index])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def key-a [:wm/reading-policy-v1 "M-a" "M-a/policy-1" {}])

(defn temp-dir []
  (.toFile (Files/createTempDirectory "class-history-index-"
                                      (make-array FileAttribute 0))))

(defn record [run-id class]
  {:run/id run-id
   :terminal {:schema :wm/q6-terminal-v1
              :choice {:status :present :policy-key key-a}}
   :run-ending-classification {:class class}})

(defn write-record! [dir run-id class]
  (let [file (io/file dir (str "tick-run-record-2026-10-29-" run-id ".edn"))]
    (spit file (pr-str (record run-id class)))
    file))

(deftest root-must-be-absolute
  (is (= :relative-class-history-root
         (try (class-f/absolute-history-directory! "data/wm-runs") nil
              (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))
  (is (.isAbsolute (class-f/absolute-history-directory! "/home/joe/code/futon2/data/wm-runs"))))

(deftest absent-and-incomplete-index-are-not-empty-history
  (let [dir (temp-dir)
        _ (write-record! dir "r1" :focused)]
    (is (= :history-index-absent (:status (index/history dir key-a 4))))
    (spit (io/file dir index/index-name) "")
    (is (= :history-index-incomplete (:status (index/history dir key-a 4))))))

(deftest backfill-matches-full-scan-without-selection-record-parsing
  (let [dir (temp-dir)
        f1 (write-record! dir "r1" :focused)
        f2 (write-record! dir "r2" :related)
        output (io/file dir index/index-name)
        _ (index/backfill! dir output)
        indexed (with-redefs [edn/read (fn [& _]
                                        (throw (ex-info "selection parsed a run record" {})))]
                  (index/history dir key-a 2))
        sources [{:path (.getPath f1) :record (record "r1" :focused)}
                 {:path (.getPath f2) :record (record "r2" :related)}]
        prediction {:focused 0.75 :related 0.25}]
    (is (= :complete (:status indexed)))
    (is (= (select-keys (class-f/evaluate key-a 2 sources prediction) [:status :value])
           (select-keys (class-f/evaluate key-a 2 (:sources indexed) prediction)
                        [:status :value])))))
