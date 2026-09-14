(ns futon2.aif.limb-evidence-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.limb-evidence :as limb]))

(def sha-a (apply str (repeat 64 "a")))
(def sha-b (apply str (repeat 64 "b")))

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
                      (assoc-in revision [:after :sha256] sha-a)))))))

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
