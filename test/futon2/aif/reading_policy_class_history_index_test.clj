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
   :startedAt (str "2026-10-29T00:00:0" (if (= run-id "r1") "1" "2") "Z")
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
        f1 (io/file dir "tick-run-record-z.edn")
        f2 (io/file dir "tick-run-record-a.edn")
        _ (spit f1 (pr-str (record "r1" :focused)))
        _ (spit f2 (pr-str (record "r2" :related)))
        output (io/file dir index/index-name)
        _ (index/backfill! dir output)
        indexed (with-redefs [edn/read (fn [& _]
                                        (throw (ex-info "selection parsed a run record" {})))]
                  (index/history dir key-a 2))
        sources [{:path (.getPath f1) :record (record "r1" :focused)}
                 {:path (.getPath f2) :record (record "r2" :related)}]
        prediction {:focused 0.75 :related 0.25}]
    (is (= :complete (:status indexed)))
    (is (= ["r1" "r2"] (mapv #(get-in % [:record :run/id]) (:sources indexed))))
    (is (= (select-keys (class-f/evaluate key-a 2 sources prediction) [:status :value])
           (select-keys (class-f/evaluate key-a 2 (:sources indexed) prediction)
                        [:status :value])))))

(deftest publication-order-is-independent-of-file-name-order
  (let [dir (temp-dir)
        earlier (io/file dir "tick-run-record-2026-10-29-z-last-by-name.edn")
        later (io/file dir "tick-run-record-2026-10-29-a-first-by-name.edn")
        earlier-record (assoc (record "r1" :focused) :startedAt "2026-10-29T01:00:00Z")
        later-record (assoc (record "r2" :related) :startedAt "2026-10-29T02:00:00Z")]
    (spit earlier (pr-str earlier-record))
    (index/append-run-record! dir earlier earlier-record)
    (spit later (pr-str later-record))
    (index/append-run-record! dir later later-record)
    (let [selected (index/history dir key-a 2)]
      (is (= :complete (:status selected)))
      (is (= ["r1" "r2"] (mapv #(get-in % [:record :run/id]) (:sources selected)))))
    (write-record! dir "r3" :focused)
    (is (= :history-index-incomplete (:status (index/history dir key-a 2))))))

(deftest live-chronology-string-keyed-sorted-map-does-not-break-terminal-search
  (let [fixture (edn/read-string (slurp "test/resources/wm/class-history/run001-projection.edn"))
        retained (:record fixture)
        chronology (:registered-run/chronology retained)
        ;; EDN hydration loses collection type. Restore the sorted-map emitted
        ;; by registered-run-telemetry/source-revisions, using the retained values.
        revisions (into (sorted-map) (:source-revisions-before chronology))
        live-record (array-map :run/id (:run/id retained)
                               :registered-run/chronology
                               (assoc chronology :source-revisions-before revisions)
                               :q6 (:q6 retained))
        dir (temp-dir)
        file (io/file dir "tick-run-record-run001.edn")]
    (try
      (is (seq revisions))
      (is (every? string? (keys revisions)))
      (is (= :wm/q6-terminal-v1 (get-in live-record [:q6 :terminal :schema])))
      (spit file (pr-str live-record))
      ;; Real append, then coverage and the retained policy identity. No live stores.
      (index/append-run-record! dir file live-record)
      (let [indexed (index/read-index dir)
            row (first (:rows indexed))]
        (is (= :complete (:status indexed)))
        (is (= 1 (:covered-count indexed)))
        (is (= (:run/id retained) (:run-id row)))
        (is (= (:policy-key-sha256
                 (index/index-entry (select-keys live-record [:run/id :q6])
                                    (.getPath file) (:source-sha256 fixture)))
               (:policy-key-sha256 row))))
      (finally
        (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))
