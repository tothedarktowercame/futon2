(ns futon2.aif.machine-pre-enact-authorization-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-pre-enact-authorization :as e3])
  (:import (java.nio.charset StandardCharsets)
           (java.nio.file Files)
           (java.security MessageDigest)))

(def ids {:model/id :wm :model/revision "rev-1" :run/id "run-1"
          :cohort/id "cohort-1" :tick/index 7 :event/id "pending-7"})
(def subject {:candidate/occurrence-id "occ-2" :action {:type :inspect :target "x"}
              :construction {:policy/id "p2" :missions ["m1"]}
              :field-pins [{:label :ranked :sha256 (apply str (repeat 64 "a"))}]
              :producer/id "codex-22" :claim/id "claim-e3"
              :artifact/ref "artifact-e3" :trace/id "trace-producer"})
(def pending (merge {:schema/version :wm/e3-pending-construction-v1 :scope :isolated-test
                     :phase :pending-pre-enact :authorization-at "2026-09-13T12:00:00Z"
                     :subject subject} ids))
(def verdict (merge {:schema/version :wm/e3-independence-verdict-v1 :scope :isolated-test
                     :verdict :independent :reviewer/id "claude-15"
                     :review-trace/id "trace-review" :subject subject} ids))
(def review (merge {:schema/version :wm/e3-independent-review-v1 :scope :isolated-test
                    :reviewer/id "claude-15" :claim/id "claim-e3"
                    :artifact/ref "artifact-e3" :producer-trace/id "trace-producer"
                    :review-trace/id "trace-review" :execution/status :executed
                    :authority/ref "isolated-fixture-authority"
                    :completed-at "2026-09-13T11:59:00Z" :subject subject} ids))

(defn- sha [bytes]
  (apply str (map #(format "%02x" (bit-and 0xff %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bytes))))))
(defn- config [records]
  (let [root (Files/createTempDirectory "e3-" (make-array java.nio.file.attribute.FileAttribute 0))]
    {:mode :isolated-test :evidence-root (str root)
     :evidence (into {} (for [[label record] records
                             :let [bytes (.getBytes (pr-str record) StandardCharsets/UTF_8)
                                   name (str (name label) ".edn")]]
                         (do (Files/write (.resolve root name) bytes (make-array java.nio.file.OpenOption 0))
                             [label {:relative-path name :sha256 (sha bytes)}])))}))
(defn- refusal [cfg]
  (try (e3/verify-pre-enact cfg) nil
       (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest exact-pending-subject-is-mechanically-authorized
  (let [out (e3/verify-pre-enact (config {:pending pending :verdict verdict :review review}))]
    (is (= :mechanism-authorized (:decision out)))
    (is (= subject (:subject out)))
    (is (= :unavailable (get-in out [:external-dependencies :production-authority])))
    (is (= :required (get-in out [:external-dependencies :r6-scoring-and-posterior])))))

(deftest closed-controls
  (testing "missing, self and unknown verdicts"
    (is (= :e3/evidence-set-incomplete (refusal (config {:pending pending :review review}))))
    (is (= :e3/self-review (refusal (config {:pending pending :review review
                                              :verdict (assoc verdict :verdict :self)}))))
    (is (= :e3/unknown-verdict (refusal (config {:pending pending :review review
                                                 :verdict (assoc verdict :verdict :unknown)})))))
  (testing "borrowed candidate, producer, trace, stale field and context"
    (doseq [changed [(assoc subject :candidate/occurrence-id "occ-other")
                     (assoc subject :producer/id "other-producer")
                     (assoc subject :trace/id "other-trace")
                     (assoc subject :field-pins [{:label :ranked :sha256 (apply str (repeat 64 "b"))}])]]
      (is (= :e3/subject-mismatch
             (refusal (config {:pending pending :review review :verdict (assoc verdict :subject changed)})))))
    (is (= :e3/context-mismatch
           (refusal (config {:pending pending :review review
                             :verdict (assoc verdict :event/id "borrowed-event")})))))
  (testing "review and temporal controls"
    (is (= :e3/self-review
           (refusal (config {:pending pending :verdict verdict
                             :review (assoc review :reviewer/id "codex-22")}))))
    (is (= :e3/review-join-mismatch
           (refusal (config {:pending pending :verdict verdict
                             :review (assoc review :review-trace/id "wrong")}))))
    (is (= :e3/review-not-pre-enact
           (refusal (config {:pending pending :verdict verdict
                             :review (assoc review :completed-at "2026-09-13T12:01:00Z")}))))
    (is (= :e3/not-pending-pre-enact
           (refusal (config {:pending (assoc pending :phase :enacted)
                             :verdict verdict :review review})))))
  (is (= :e3/production-authority-unavailable
         (refusal {:mode :production}))))
