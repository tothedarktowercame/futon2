(ns futon2.test-support.runner-fixture-test
  "Bad case for with-hermetic-traces: a real meta-injury deposit made while the
   fixture is active must land under the fixture's temporary root and leave the
   canonical data/wm-meta-injuries file set unchanged. On 2026-10-19 four test
   runs deposited into the canonical store (finding F1, PACKETS-2026-10-19.md)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-injury-store :as meta-injury-store]
            [futon2.test-support.runner-fixture :as runner-fixture]))

(def canonical-root "/home/joe/code/futon2/data/wm-meta-injuries")

(def live-run-record
  "L1, 2026-10-19: a real abstained click record, read only."
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-19-d608312b-f4e8-458d-a541-481aabeff61b.edn")

(defn- file-set [dir]
  (let [d (io/file dir)]
    (if (.isDirectory d)
      (into (sorted-map) (map (fn [^java.io.File x] [(.getPath x) (.length x)]))
            (filter #(.isFile ^java.io.File %) (file-seq d)))
      (sorted-map))))

(deftest deposit-under-fixture-does-not-touch-canonical-store
  (let [before (file-set canonical-root)
        seen (atom nil)]
    (runner-fixture/with-hermetic-traces
      (fn []
        (testing "the fixture redirects the default injury root away from the canonical store"
          (is (not= canonical-root meta-injury-store/default-root)))
        (when (.exists (io/file live-run-record))
          (let [result (meta-injury-store/deposit! live-run-record)]
            (reset! seen result)
            (testing "the real deposit lands under the fixture root"
              (is (string? (:head-path result)))
              (is (.startsWith ^String (str (:head-path result))
                               ^String meta-injury-store/default-root)))))))
    (testing "the canonical injury store is byte-for-byte the same file set afterwards"
      (is (= before (file-set canonical-root))))
    (when-not @seen
      (println "runner-fixture-test: live L1 record absent; deposit half not exercised"))))
