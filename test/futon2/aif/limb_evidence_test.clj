(ns futon2.aif.limb-evidence-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.limb-evidence :as limb]))

(def sha-a (apply str (repeat 64 "a")))
(def sha-b (apply str (repeat 64 "b")))

(defn- sha256 [bytes]
  (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" (bit-and 0xff %)) digest))))

(def receipt
  {:schema :wm/limb-receipt-v1
   :repair/id "repair-1"
   :limb :distinct-repair-commit
   :command "git rev-parse HEAD"
   :exit 0
   :stdout-sha256 sha-a
   :stderr-sha256 sha-b
   :recorded-at "2026-09-14T12:00:00Z"})

(def standing
  {:schema :wm/target-standing-decision-v1
   :entity/id "entity-1"
   :decision :still-live
   :decided-by "reviewer"
   :implementation-author "author"
   :decided-at "2026-09-14T12:01:00Z"
   :explanation (str "The cited execution and review records show that the selected target "
                     "still has an undischarged production-successor obligation.")
   :evidence ["record-1"]})

(def revision
  {:schema :wm/entity-revision-pair-v1
   :entity/id "entity-1"
   :before {:source-path "/evidence/before.edn" :sha256 sha-a
            :captured-at "2026-09-14T11:00:00Z"}
   :after {:source-path "/evidence/after.edn" :sha256 sha-b
           :captured-at "2026-09-14T12:00:00Z"}
   :dimensions [:implementation :review]})

(defn- refusal [f]
  (try (f) nil
       (catch clojure.lang.ExceptionInfo e
         (:limb-evidence/refusal (ex-data e)))))

(deftest each-record-shape-validates
  (is (= receipt (limb/validate-limb-receipt receipt)))
  (is (= standing (limb/validate-standing-decision standing)))
  (is (= revision (limb/validate-revision-pair revision)))
  (is (= receipt (limb/validate-record receipt)))
  (is (= standing (limb/validate-record standing)))
  (is (= revision (limb/validate-record revision))))

(deftest standing-and-revision-refusals
  (testing "implementation author cannot make the standing decision"
    (is (= :standing-decision-not-independent
           (refusal #(limb/validate-standing-decision
                      (assoc standing :decided-by "author"))))))
  (testing "a revision must change selected-entity bytes"
    (is (= :revision-unchanged
           (refusal #(limb/validate-revision-pair
                      (assoc-in revision [:after :sha256] sha-a))))))
  (testing "the after capture must follow the before capture"
    (is (= :revision-order-invalid
           (refusal #(limb/validate-revision-pair
                      (assoc-in revision [:after :captured-at]
                                "2026-09-14T10:59:00Z")))))
    (is (= :revision-order-invalid
           (refusal #(limb/validate-revision-pair
                      (assoc-in revision [:after :captured-at]
                                "2026-09-14T11:00:00Z")))))))

(deftest receipt-and-common-field-refusals
  (testing "a prose-only receipt lacks the byte digests"
    (is (= :shape-invalid
           (refusal #(limb/validate-limb-receipt
                      (-> receipt
                          (dissoc :stdout-sha256 :stderr-sha256)
                          (assoc :summary "tests passed")))))))
  (testing "timestamps are parsed"
    (is (= :timestamp-invalid
           (refusal #(limb/validate-limb-receipt
                      (assoc receipt :recorded-at "yesterday"))))))
  (testing "digests are lowercase exact SHA-256"
    (is (= :sha256-invalid
           (refusal #(limb/validate-limb-receipt
                      (assoc receipt :stdout-sha256 "abc")))))
    (is (= :sha256-invalid
           (refusal #(limb/validate-revision-pair
                      (assoc-in revision [:before :sha256]
                                (apply str (repeat 64 "A")))))))))

(deftest receipt-companion-outputs-are-byte-verified
  (let [stdout (.getBytes "actual stdout\n" "UTF-8")
        stderr (.getBytes "actual stderr\n" "UTF-8")
        with-files (assoc receipt
                          :stdout-file "command.stdout"
                          :stderr-file "command.stderr"
                          :stdout-sha256 (sha256 stdout)
                          :stderr-sha256 (sha256 stderr))
        reads {"command.stdout" stdout "command.stderr" stderr}]
    (is (= with-files
           (limb/validate-limb-receipt-outputs with-files #(get reads %))))
    (is (= :output-digest-mismatch
           (refusal #(limb/validate-limb-receipt-outputs
                      with-files
                      (fn [filename]
                        (if (= filename "command.stdout")
                          (.getBytes "changed\n" "UTF-8")
                          (get reads filename)))))))
    (is (= :output-file-invalid
           (refusal #(limb/validate-limb-receipt
                      (assoc with-files :stdout-file "nested/command.stdout")))))))

(deftest standing-decision-requires-review-grade-explanation
  (is (= :explanation-invalid
         (refusal #(limb/validate-standing-decision
                    (dissoc standing :explanation)))))
  (is (= :explanation-invalid
         (refusal #(limb/validate-standing-decision
                    (assoc standing :explanation "still live"))))))

(deftest revision-pair-companion-bytes-are-pinned
  (let [before (.getBytes "before bytes" "UTF-8")
        after (.getBytes "after bytes changed" "UTF-8")
        pair {:schema :wm/entity-revision-pair-v1 :entity/id "entity-1"
              :before {:file "runner.before" :sha256 (sha256 before)
                       :bytes (alength before)}
              :after {:file "runner.after" :sha256 (sha256 after)
                      :bytes (alength after)}
              :dimensions {:insertions 1 :deletions 0 :commit "abc123"}}
        reads {"runner.before" before "runner.after" after}]
    (is (= pair (limb/validate-revision-pair-files pair reads)))
    (is (= :output-digest-mismatch
           (refusal #(limb/validate-revision-pair-files
                      pair (assoc reads "runner.after"
                                  (.getBytes "tampered" "UTF-8"))))))
    (is (= :output-file-invalid
           (refusal #(limb/validate-revision-pair-files
                      (assoc-in pair [:before :file] "nested/runner.before")
                      reads))))))

(deftest cohort-53-revision-companion-live-pins
  (let [root "/home/joe/code/futon2/data/wm-full-loop-machinery-53/wm-contract-machinery-53-v1/attempt-001/evidence"
        pinned {"runner.before" "cdfef17652ee498875b846bf39e0a95ef892df22f48ed166303927a1715ff995"
                "runner.after" "f6238a7da08bde85e37f12490e0a1202eaccc139bbf5fd1f69942076e4bc4115"}]
    (doseq [[filename expected] pinned]
      (is (= expected
             (sha256 (java.nio.file.Files/readAllBytes
                      (.toPath (java.io.File. root filename)))))))))

(deftest bundle-reports-covered-and-absent-limbs
  (let [review-receipt (assoc receipt
                              :limb :independent-review
                              :command "review-check")
        result (limb/validate-limb-bundle
                {:requires [:distinct-repair-commit :independent-review
                            :grounded-repair
                            :distinct-production-shaped-successor]}
                #{receipt review-receipt standing revision})]
    (is (= :incomplete (:status result)))
    (is (= [:distinct-repair-commit :independent-review] (:covered result)))
    (is (= [:grounded-repair :distinct-production-shaped-successor]
           (:absent result)))
    (is (= 4 (count (:records result))))))
