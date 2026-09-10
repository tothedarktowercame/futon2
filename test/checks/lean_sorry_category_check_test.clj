(ns checks.lean-sorry-category-check-test
  (:require [checks.lean-sorry-category-check :as check]
            [clojure.test :refer [deftest is testing]]))

(def historical-mention-fixture
  "/-- OPEN, RUN-GATED · contract kind HOLE intentionally · The PERMANENT EXTERNAL ATTESTATION reading is REFUSED BY ITS OWNER. -/
def routedEvent : Prop := sorry")

(deftest historical-mention-does-not-become-current-category
  (let [report (check/validate-source
                historical-mention-fixture
                {"routedEvent" {:closability :run-gated :readiness :not-ready}})]
    (is (:pass? report))
    (is (= {"OPEN, RUN-GATED" 1} (:sorry-category-counts report)))
    (is (= {"PERMANENT EXTERNAL ATTESTATION" 1}
           (:historical-category-mentions report)))
    (is (= {:closability {:run-gated 1} :readiness {:not-ready 1}}
           (:lifecycle-counts report)))
    (is (= {:name "routedEvent"
            :declaration-category "OPEN, RUN-GATED"
            :closability :run-gated
            :readiness :not-ready}
           (first (:sorry-declarations report))))))

(deftest unknown-and-ambiguous-current-categories-refuse
  (testing "unknown first clause"
    (let [report (check/validate-source
                  "/-- SOMETHING NEW · contract kind HOLE intentionally -/\ndef x : Prop := sorry")]
      (is (false? (:pass? report)))
      (is (some #(= :unknown-current-category (:reason %)) (:findings report)))))
  (testing "two categories in the first clause"
    (let [report (check/validate-source
                  "/-- OPEN, RUN-GATED PERMANENT EXTERNAL ATTESTATION · history -/\ndef x : Prop := sorry")]
      (is (false? (:pass? report)))
      (is (some #(= :double-category (:reason %)) (:findings report))))))

(deftest category-must-be-the-entire-current-clause
  (doseq [clause ["NOT PERMANENT EXTERNAL ATTESTATION"
                  "PREFIX OPEN, RUN-GATED"
                  "OPEN, RUN-GATED SUFFIX"]]
    (let [report (check/validate-source
                  (str "/-- " clause " · evidence: `checks/lean_sorry_category_check.clj` -/\n"
                       "def x : Prop := sorry"))]
      (is (false? (:pass? report)) clause)
      (is (some #(= :unknown-current-category (:reason %)) (:findings report)) clause)
      (is (empty? (:sorry-category-counts report)) clause))))

(deftest registry-witness-and-lifecycle-provenance-are-explicit
  (let [lifecycle {:as-of "2026-09-08"
                   :authority {:contract-git-sha "bf79f"}
                   :by-name {"x" {:closability :pre-run-closable
                                   :readiness :not-ready}}}
        registry {"x" {:witnesses "x" :recorded-at "2026-09-08T00:00:00Z"
                        :result :passed
                        :check {:path "checks/lean_sorry_category_check.clj"}
                        :expected-rejection {:mode :negative}}}
        report (check/validate-source
                "/-- PERMANENT EXTERNAL ATTESTATION · historical audit omitted a docstring citation -/\ndef x : Prop := sorry"
                lifecycle registry)
        row (first (:sorry-declarations report))]
    (is (:pass? report))
    (is (= {:as-of "2026-09-08" :authority {:contract-git-sha "bf79f"}}
           (:lifecycle-provenance report)))
    (is (= :witness-registry (get-in row [:witness-evidence :source])))
    (is (true? (get-in row [:witness-evidence :check-present?])))))

(deftest lifecycle-and-declaration-category-remain-distinct
  (let [report (check/validate-source
                "/-- PERMANENT EXTERNAL ATTESTATION · evidence: `checks/lean_sorry_category_check.clj` -/\ndef x : Prop := sorry"
                {"x" {:closability :pre-run-closable :readiness :not-ready}})
        row (first (:sorry-declarations report))]
    (is (:pass? report))
    (is (= "PERMANENT EXTERNAL ATTESTATION" (:declaration-category row)))
    (is (= :pre-run-closable (:closability row)))))

(deftest negative-controls-require-their-own-finding
  (let [baseline {:findings [{:reason :attestation-checker-absent}]}
        detected {:findings [{:reason :attestation-checker-absent}
                             {:reason :double-category}]}]
    (is (not (check/negative-detected? "--negative-double" baseline)))
    (is (check/negative-detected? "--negative-double" detected))))
