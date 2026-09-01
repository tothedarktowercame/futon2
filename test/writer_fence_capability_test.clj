(ns writer-fence-capability-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is]]
            [writer-fence-capability :as fence]))

(deftest no-in-process-bearer-or-runner-seam-exists
  (is (nil? (ns-resolve 'writer-fence-capability 'capability-token)))
  (is (nil? (ns-resolve 'writer-fence-capability '*run-evidence*)))
  (is (= :unverified
         (:event-free? (fence/assess {:started-at "s" :finished-at "f"}
                                     false "fabricated" nil)))))

(deftest empty-receipt-cannot-establish-a-fence
  (let [path (java.nio.file.Files/createTempFile
              "empty-fence-receipt-" ".json"
              (make-array java.nio.file.attribute.FileAttribute 0))]
    (try
      (is (= :unavailable (:status (fence/verify "fence" (str path)))))
      (finally (java.nio.file.Files/deleteIfExists path)))))

(deftest replayed-observation-interval-is-rejected
  (let [path (java.nio.file.Files/createTempFile
              "stale-fence-receipt-" ".json"
              (make-array java.nio.file.attribute.FileAttribute 0))
        receipt {:verdict "FENCE-VERIFIABLE" :fence-id "stale"
                 :observation-interval {:started-at "1900-01-01T00:00:00Z"
                                        :finished-at "1900-01-01T00:00:01Z"}
                 :classification
                 {:fence-id "stale" :observed {:start {} :finish {}}
                  :attested {:status "complete"
                             :value {:fence-id "stale"
                                     :expires-at "2099-01-01T00:00:00Z"}}}}]
    (try
      (spit (.toFile path) (json/generate-string receipt))
      (let [result (fence/verify "stale" (str path))]
        (is (false? (:verified? result)))
        (is (some #{:prior-observation-interval-stale} (:problems result))))
      (finally (java.nio.file.Files/deleteIfExists path)))))
