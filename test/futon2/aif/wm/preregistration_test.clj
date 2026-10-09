(ns futon2.aif.wm.preregistration-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.wm.preregistration :as prereg]))

(def card {:run-id "2026-10-10-next"
           :headline {:fail 0}
           :sections [{:id :outcome :data {:review-verdict "APPROVE"}}]})
(def record {:registered-run/chronology
             {:source-revisions-before {"futon2" "next-head"}}})

(defn fixture-prereg [card-path]
  {:id :wm/example
   :against {:run-id "2026-10-09-prior"
             :report-card-sha256 (prereg/sha256-file card-path)}
   :defect {:path [:headline :fail] :facts {:fail 1}}
   :change {:commits ["abcdef1"]}
   :predictions [{:path [:headline :fail] :expect '= :value 0}]
   :applies-when {:path [:sections 0 :data :review-verdict] :expect 'present}
   :refuted-by "The next exercised card still has a failed Lean row."
   :author "fixture"
   :registered-for "the next run after these commits"})

(defmacro with-against [[path prereg-sym] & body]
  `(let [dir# (.toFile (java.nio.file.Files/createTempDirectory
                        "wm-prereg" (make-array java.nio.file.attribute.FileAttribute 0)))
         ~path (io/file dir# "report-card.edn")]
     (spit ~path (pr-str {:run-id "2026-10-09-prior"}))
     (let [~prereg-sym (fixture-prereg ~path)] ~@body)))

(deftest validator-pins-existing-card-and-closed-predicate-vocabulary
  (with-against [path p]
    (is (= p (prereg/validate p {"2026-10-09-prior" path})))
    (testing "unknown predicates are rejected"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"unknown or malformed"
            (prereg/validate (assoc-in p [:predictions 0 :expect] 'roughly=)
                             {"2026-10-09-prior" path}))))
    (testing "a digest mismatch is rejected"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"digest mismatch"
            (prereg/validate (assoc-in p [:against :report-card-sha256] "bad")
                             {"2026-10-09-prior" path}))))))

(deftest confirmed-and-refuted-outcomes
  (with-against [_ p]
    (let [opts {:ancestor? (constantly true)}
          confirmed (prereg/evaluate-one p card record opts)
          refuted (prereg/evaluate-one
                   (assoc p :predictions [{:path [:headline :fail] :expect '= :value 1}])
                   card record opts)]
      (is (= :confirmed (:status confirmed)))
      (is (= :refuted (:status refuted)))
      (is (= 0 (get-in refuted [:checks 0 :observed])))
      (is (false? (get-in refuted [:checks 0 :holds?]))))))

(deftest not-exercised-and-not-yet-applicable
  (with-against [_ p]
    (let [not-exercised (prereg/evaluate-one
                         (assoc p :applies-when
                                {:path [:sections 0 :data :review-verdict]
                                 :expect '= :value "REJECT"})
                         card record {:ancestor? (constantly true)})
          not-yet (prereg/evaluate-one p card record {:ancestor? (constantly false)})]
      (is (= :not-exercised (:status not-exercised)))
      (is (= "APPROVE" (get-in not-exercised [:observed :value])))
      (is (= :not-yet-applicable (:status not-yet)))
      (is (= :change-not-ancestor (:reason not-yet))))))

(deftest first-decisive-card-closes-forever
  (with-against [_ p]
    (let [prior {:run-id "2026-10-10-decision"
                 :preregistrations {:entries [{:id (:id p) :status :refuted
                                               :deciding-run "2026-10-10-decision"}]}}
          later (prereg/evaluate-one p card record
                                     {:ancestor? (constantly true)
                                      :prior-cards [prior]})]
      (is (= :closed (:status later)))
      (is (= :refuted (:deciding-status later)))
      (is (= {:run-id "2026-10-10-decision"
              :reference "../2026-10-10-decision/report-card.html"}
             (:deciding-card later))))))

(deftest aggregate-counts-are-index-ready
  (with-against [_ p]
    (let [result (prereg/evaluate [p] card record {:ancestor? (constantly true)})]
      (is (= 1 (get-in result [:counts :confirmed])))
      (is (= 0 (get-in result [:counts :refuted]))))))
