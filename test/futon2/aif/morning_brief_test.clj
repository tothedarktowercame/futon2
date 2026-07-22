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
    (is (thrown? clojure.lang.ExceptionInfo
                 (brief/review! root "attempt-001"
                                :substantive-achievement :yes
                                "duplicate" "joe")))
    (is (thrown? java.nio.file.FileAlreadyExistsException
                 (brief/queue-item! root {:attempt-id "attempt-001"})))))

(deftest operator-gate-items-are-typed-and-idempotent-while-open
  (let [root (temp-root)
        gate {:type :mission-gate
              :mission "M-learning-loop"
              :gate-kind "operator-acceptance"
              :gate-text "Joe accepts the rendered graph"
              :date "2026-07-22"}
        first-result (brief/queue-operator-gate! root gate)
        second-result (brief/queue-operator-gate! root gate)
        item (first (brief/items root))]
    (is (= :queued (:status first-result)))
    (is (= :already-open (:status second-result)))
    (is (= 1 (count (brief/items root))))
    (is (= gate (:operator-action item)))
    (is (= [:operator-gate] (brief/item-objectives item)))
    (is (= [(:attempt-id item)]
           (mapv :attempt-id (brief/pending-items root))))))

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

(deftest built-items-accept-a-feature-verdict-without-projecting-belief
  (let [root (temp-root)
        item {:attempt-id "attempt-feature"
              :outcome :grounded-change
              :commit "abc123"
              :feature-card {:built "A usable Field Desk"}}
        _ (brief/queue-item! root item)
        objectives (brief/item-objectives item)
        review (brief/review! root "attempt-feature" :feature-verdict
                              :accept-with-follow-ups
                              "Accept; improve empty-state copy" "joe")]
    (is (= :feature-verdict (first objectives)))
    (is (some #{:evidence-sufficiency} objectives))
    (is (= :feature-verdict (:objective review)))
    (is (= :accept-with-follow-ups (:answer review)))
    (is (= "Accept; improve empty-state copy" (:note review)))
    (is (nil? (:belief-event review)))
    (is (= review (first (brief/reviews root))))))

(deftest partial-authored-build-also-requires-a-feature-verdict
  (is (some #{:feature-verdict}
            (brief/item-objectives
             {:attempt-id "attempt-partial"
              :commit "partial123"
              :achievement {:tier :partial-authored}}))))

(deftest addenda-are-append-only-repeatable-and-chronological
  (let [root (temp-root)
        _ (brief/queue-item! root {:attempt-id "attempt-notebook"})
        first-note (brief/addendum! root "attempt-notebook" :why-built
                                    "Why this exists" "To close the review gap." "joe")
        second-note (brief/addendum! root "attempt-notebook" :repro
                                     "Exercise it" "Open the desk and press n." "joe")
        stored (brief/addenda root)]
    (is (= 2 (count stored)))
    (is (= [(:morning-brief/addendum-id first-note)
            (:morning-brief/addendum-id second-note)]
           (mapv :morning-brief/addendum-id stored)))
    (is (every? #(re-matches #"mba-[0-9a-f-]+" %)
                (map :morning-brief/addendum-id stored)))
    (is (not= (:morning-brief/addendum-id first-note)
              (:morning-brief/addendum-id second-note)))
    (is (= [:why-built :repro] (mapv :kind stored)))
    (is (= 1 (:morning-brief/schema-version first-note)))
    (is (= stored (vec (sort-by :created-at stored))))))

(deftest addendum-rejects-unknown-attempt-kind-and-blank-content
  (let [root (temp-root)]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Unknown Morning Brief attempt"
                          (brief/addendum! root "missing" :note
                                           "Title" "Body" "joe")))
    (brief/queue-item! root {:attempt-id "attempt-notebook"})
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Unknown Morning Brief addendum kind"
                          (brief/addendum! root "attempt-notebook" :other
                                           "Title" "Body" "joe")))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"must be non-blank"
                          (brief/addendum! root "attempt-notebook" :note
                                           " " "Body" "joe")))))
