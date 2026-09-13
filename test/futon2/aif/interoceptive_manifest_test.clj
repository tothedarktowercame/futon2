(ns futon2.aif.interoceptive-manifest-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.interoceptive-manifest :as manifest])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn roots []
  (let [base (.toFile (Files/createTempDirectory
                       "interoceptive-manifest-"
                       (make-array FileAttribute 0)))
        trips (doto (java.io.File. base "trips") .mkdir)
        repair (doto (java.io.File. base "repair") .mkdir)]
    (doseq [child manifest/repair-children]
      (.mkdir (java.io.File. repair child)))
    [trips repair]))

(defn write! [root relative value]
  (let [f (java.io.File. root relative)]
    (spit f (str (pr-str value) "\n")) f))

(defn refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest valid-production-shaped-test-history
  (let [[trips repair] (roots)
        id "trip-valid" rid "repair-trip-valid"]
    (write! trips (str id ".edn")
            {:trip/id id :trip/schema-version 1 :trip/action :stop-line})
    (write! (java.io.File. repair "findings") (str rid ".edn")
            {:repair/id rid :repair/status :open :failure-data {:trip/id id}})
    (write! (java.io.File. repair "implementations") (str rid ".edn")
            {:repair/id rid :repair/status :awaiting-validation})
    (write! (java.io.File. repair "resolutions") (str rid ".edn")
            {:repair/id rid :repair/status :resolved})
    (let [result (manifest/test-snapshot (.getPath trips) (.getPath repair))]
      (is (:stable-census? result))
      (is (= 4 (count (:manifest result))))
      (is (= 1 (get-in result [:snapshot :machine-confidence])))
      (is (= :test-root (get-in result [:snapshot :excluded 0 :reason]))))))

(deftest reader-and-authority-refusals
  (testing "missing directory and production-label spoof"
    (let [[trips repair] (roots)]
      (is (= :interoceptive/directory-unavailable
             (refusal #(manifest/capture "/does/not/exist" (.getPath repair) :test))))
      (is (= :interoceptive/authority-spoof
             (refusal #(manifest/capture (.getPath trips) (.getPath repair)
                                         :production))))))
  (testing "trailing EDN and unexpected structure"
    (let [[trips repair] (roots)]
      (spit (java.io.File. trips "bad.edn") "{} {}")
      (is (= :interoceptive/not-one-edn-form
             (refusal #(manifest/capture (.getPath trips) (.getPath repair) :test)))))
    (let [[trips repair] (roots)]
      (.mkdir (java.io.File. trips "unexpected"))
      (is (= :interoceptive/unexpected-structural-entry
             (refusal #(manifest/capture (.getPath trips) (.getPath repair) :test))))))
  (testing "membership and content changes refuse"
    (let [[trips repair] (roots)
          late (java.io.File. trips "late.edn")]
      (binding [manifest/*after-capture-hook*
                #(spit late "{:trip/id \"late\" :trip/schema-version 1 :trip/action :record}\n")]
        (is (= :interoceptive/store-changed-during-capture
               (refusal #(manifest/capture (.getPath trips) (.getPath repair) :test))))))
    (let [[trips repair] (roots)
          f (write! trips "one.edn"
                    {:trip/id "one" :trip/schema-version 1 :trip/action :record})]
      (binding [manifest/*after-capture-hook* #(spit f "{}\n")]
        (is (= :interoceptive/store-changed-during-capture
               (refusal #(manifest/capture (.getPath trips) (.getPath repair) :test))))))))
