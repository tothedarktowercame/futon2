(ns futon2.aif.run-record-io-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.previous-run :as previous-run]
            [futon2.aif.run-record-io :as run-record-io]))

(defn- temp-dir []
  (doto (.toFile (java.nio.file.Files/createTempDirectory
                  "run-record-io" (make-array java.nio.file.attribute.FileAttribute 0)))
    (.deleteOnExit)))

(defn- big-decision [n]
  (let [shared (vec (for [i (range 300)] {:i i :text (apply str (repeat 40 "y"))}))]
    {:chosen {:target "M-x" :id :C1}
     :selection-certificate {:scoring (into {} (for [i (range n)] [i {:c-source shared :g (double i)}]))
                             :candidates (vec (for [i (range n)] {:id i :c-source shared}))}}))

(defn- record [run-id started-at]
  {:run/id run-id :click/id (str "wm-click-" run-id) :startedAt started-at
   :route [:a :b] :decision (big-decision 400)
   :world-at-selection {:open-tasks {:missions {:ids ["M-x"]}}}})

(deftest written-records-round-trip-and-keep-top-level-fields-plain
  (let [dir (temp-dir)
        f (io/file dir "tick-run-record-2026-10-10-a.edn")
        r (record "2026-10-10-a" "2026-10-10T01:00:00Z")]
    (runner/write-run-record-stream! f r)
    (testing "the head line comes first and names the run"
      (is (str/starts-with? (first (line-seq (io/reader f))) run-record-io/head-prefix))
      (is (= {:run/id "2026-10-10-a" :click/id "wm-click-2026-10-10-a"
              :startedAt "2026-10-10T01:00:00Z"}
             (run-record-io/read-head f))))
    (testing "the file is much smaller than the record printed whole"
      (is (< (.length f) (/ (count (pr-str r)) 4))))
    (testing "a reader that does not hydrate still sees the top-level fields"
      (let [raw (edn/read-string (slurp f))]
        (is (= (:run/id r) (:run/id raw)))
        (is (= (:route r) (:route raw)))
        (is (contains? raw :durable/interned))))
    (testing "run-record-io returns exactly the record"
      (is (= r (run-record-io/read-record f))))))

(deftest read-head-falls-back-for-records-without-a-head-line
  (let [dir (temp-dir)
        f (io/file dir "tick-run-record-2026-10-10-old.edn")]
    (spit f (pr-str {:run/id "old" :startedAt "2026-10-10T00:00:00Z" :decision {}}))
    (is (= {:run/id "old" :startedAt "2026-10-10T00:00:00Z"} (run-record-io/read-head f)))))

(deftest previous-run-orders-by-head-lines-and-reads-the-hydrated-record
  (let [dir (temp-dir)
        a (record "2026-10-10-aaa" "2026-10-10T01:00:00Z")
        b (record "2026-10-10-bbb" "2026-10-10T03:00:00Z")]
    (runner/write-run-record-stream! (io/file dir "tick-run-record-2026-10-10-aaa.edn") a)
    (runner/write-run-record-stream! (io/file dir "tick-run-record-2026-10-10-bbb.edn") b)
    (let [f (previous-run/previous-record-file dir "2026-10-10-zzz")]
      (is (= "tick-run-record-2026-10-10-bbb.edn" (.getName f)) "latest :startedAt wins")
      (is (= b (:record (previous-run/read-record f)))))))
