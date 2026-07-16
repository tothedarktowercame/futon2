(ns futon2.aif.morning-brief-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.morning-brief :as brief]))

(defn- temp-root []
  (.getPath (.toFile (java.nio.file.Files/createTempDirectory
                      "wm-morning-brief-test"
                      (make-array java.nio.file.attribute.FileAttribute 0)))))

(deftest queue-review-and-consumption-are-append-only
  (let [root (temp-root)
        _ (brief/queue-item! root {:attempt-id "attempt-001"
                                   :outcome :grounded-change
                                   :qa-targets {:achievement {:entity-id "entity/a"}}})
        pending-before (brief/pending-items root)
        review (brief/review! root "attempt-001" :substantive-achievement
                              :yes "looks right" "joe")
        event-id (get-in review [:belief-event :event-id])]
    (is (= ["attempt-001"] (mapv :attempt-id pending-before)))
    (is (= :strengthened (get-in review [:belief-event :type])))
    (is (= [:selection-quality]
           (:pending-objectives (first (brief/pending-items root)))))
    (is (= [event-id]
           (mapv :event-id (brief/unseen-belief-events root #{}))))
    (is (empty? (brief/unseen-belief-events root #{event-id})))
    (is (thrown? java.nio.file.FileAlreadyExistsException
                 (brief/queue-item! root {:attempt-id "attempt-001"})))))

(deftest ungrounded-achievement-answer-remains-evaluation-telemetry
  (let [root (temp-root)
        _ (brief/queue-item! root {:attempt-id "attempt-failed"
                                   :outcome :build-failed
                                   :qa-targets {:achievement {:entity-id nil}}})
        review (brief/review! root "attempt-failed"
                              :substantive-achievement :yes
                              "useful idea, no grounded result" "joe")]
    (is (nil? (:belief-event review)))
    (is (empty? (brief/unseen-belief-events root #{})))))
