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
            :docstring-checker-citations []
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
                        :check {:repo "futon2"
                                :path "checks/lean_sorry_category_check.clj"}
                        :report {:repo "futon2"
                                 :path "holes/labs/wm-contract/U80-current-sorry-census-2026-09-10.md"}
                        :expected-rejection {:mode :negative}
                        :control {:kind :in-memory-synthetic-edge
                                  :writes-live-state? false
                                  :expected-cause :negative}}}
        report (check/validate-source
                "/-- PERMANENT EXTERNAL ATTESTATION · historical audit omitted a docstring citation -/\ndef x : Prop := sorry"
                lifecycle registry)
        row (first (:sorry-declarations report))]
    (is (:pass? report))
    (is (= {:as-of "2026-09-08" :authority {:contract-git-sha "bf79f"}}
           (:lifecycle-provenance report)))
    (is (= :witness-registry (get-in row [:witness-evidence :source])))
    (is (true? (get-in row [:witness-evidence :check :present?])))
    (is (true? (get-in row [:witness-evidence :admitted?])))
    (is (empty? (:docstring-checker-citations row)))))

(deftest registry-admission-refuses-foreign-and-malformed-evidence
  (let [source "/-- PERMANENT EXTERNAL ATTESTATION · owner x -/\ndef x : Prop := sorry"
        base {:witnesses "x" :result :passed
              :check {:repo "futon2" :path "checks/lean_sorry_category_check.clj"}
              :control {:kind :in-memory-synthetic-edge
                        :writes-live-state? false :expected-cause :negative}}
        cases [(assoc base :check {:repo "nonexistent-repo"
                                   :path "checks/lean_sorry_category_check.clj"})
               (assoc base :check {:repo "futon2" :path "../foreign.clj"})
               (assoc base :control true)]]
    (doseq [row cases]
      (let [report (check/validate-source source {} {"x" row})]
        (is (false? (:pass? report)))
        (is (some #(= :registry-witness-not-admitted (:reason %))
                  (:findings report)))
        (is (false? (get-in report [:sorry-declarations 0
                                    :witness-evidence :admitted?])))))))

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

(deftest registered-legacy-metadata-does-not-invalidate-docstring-evidence
  ;; Real existing rows have checker citations but predate structured controls.
  ;; The registry is an alternative binding route, not a new requirement on them.
  (let [registry (check/read-registry check/witness-registry-path)
        source (slurp check/source-path)
        report (check/validate-source source {} registry)]
    (doseq [name ["preferenceStackLiveRecorded" "wmRunsOnce"]]
      (is (not-any? #(= name (:declaration %)) (:findings report)) name))))
