(ns futon2.aif.tripwire-dismissal-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.tripwire :as tripwire])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.time Instant]))

(defn- with-findings [ids dismissed f]
  (let [root (.toFile (Files/createTempDirectory "t8-dismissal-" (make-array FileAttribute 0)))
        write! (fn [child id record]
                 (let [file (io/file root child (str id ".edn"))]
                   (io/make-parents file)
                   (spit file (pr-str record))))]
    (try
      (doseq [id ids]
        (write! "findings" id {:repair/id id :repair/status :open
                               :repair/class :machine-failure :failure-kind :build-failed
                               :target "same-repair-target" :opened-at (str (Instant/now))}))
      (doseq [id dismissed]
        (write! "dismissals" id {:repair/id id :repair/status :dismissed-unexecuted
                                 :execution {:executed false :tool-events 0 :command-events 0}}))
      (let [snapshot (tripwire/repair-snapshot (.getPath root))
            observation (#'tripwire/cross-run-observation
                         {:repair-root (.getPath root) :cohort? true
                          :tripwire/cohort-history [] :tripwire/a-matrix-events []
                          :tripwire/grounding-witnesses []}
                         {:phase :opportunity :transition :start})]
        (f snapshot observation (tripwire/evaluate-wire :T8 observation)))
      (finally
        (doseq [file (reverse (file-seq root))] (io/delete-file file))))))

(deftest all-dismissed-findings-produce-no-witness
  (with-findings ["a" "b" "c"] ["a" "b" "c"]
    (fn [snapshot observation witnesses]
      (is (= 6 (count snapshot)))
      (is (= {"a" :dismissed-unexecuted "b" :dismissed-unexecuted "c" :dismissed-unexecuted}
             (#'tripwire/effective-statuses snapshot)))
      (is (= #{"a" "b" "c"} (:closed-repair-ids observation)))
      (is (empty? witnesses)))))

(deftest live-group-above-threshold-still-trips-without-dismissed-sources
  (with-findings ["a" "b" "c" "dismissed"] ["dismissed"]
    (fn [_ observation witnesses]
      (is (= #{"dismissed"} (:closed-repair-ids observation)))
      (is (= 1 (count witnesses)))
      (is (= :duplicate-finding-livelock (:kind (first witnesses))))
      (is (= 3 (:finding-count (first witnesses))))
      (is (= #{"a" "b" "c"} (set (:repair-ids (first witnesses)))))
      (is (not-any? #{"dismissed"} (mapcat :repair-ids witnesses))))))

(deftest one-live-member-does-not-evade-the-existing-threshold
  (with-findings ["a" "b" "c"] ["a" "b"]
    (fn [_ observation witnesses]
      (is (= #{"a" "b"} (:closed-repair-ids observation)))
      (is (empty? witnesses)))))
