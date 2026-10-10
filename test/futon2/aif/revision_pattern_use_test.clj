(ns futon2.aif.revision-pattern-use-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.revision-pattern-use :as sut]))

(def action-digest "action-digest")
(def loci [{:path "src/futon2/example.clj" :evidence "guard now checks the blocker token"}])
(def claim {:schema sut/claim-schema :pattern :p/new
            :dispatched-action-sha256 action-digest :artifact-commit "abc1234"
            :loci loci})
(def review {:schema sut/review-schema :verdict :verified :pattern :p/new
             :dispatched-action-sha256 action-digest :artifact-commit "abc1234"
             :loci-sha256 (identity/digest loci)})
(defn line [marker value] (str marker " " (pr-str value)))
(def base {:pattern :p/new :pattern-source {:path "patterns/new.md" :sha256 "pin"}
           :action-sha256 action-digest :artifact-commit "abc1234"
           :reviewer-job-id "review-2" :reviewer-verdict :approve})

(deftest author-assertion-alone-is-typed-absent
  (let [r (sut/observation (assoc base :author-text (line sut/author-marker claim)
                                  :reviewer-text "FULL_LOOP_REVIEW: APPROVE"))]
    (is (= :absent (:status r)))
    (is (= :reviewer-verification-absent (:reason r)))))

(deftest matching-independent-verification-produces-bound-observation
  (let [r (sut/observation (assoc base
                                  :author-text (line sut/author-marker claim)
                                  :reviewer-text (str "FULL_LOOP_REVIEW: APPROVE\n"
                                                      (line sut/reviewer-marker review))))]
    (is (= :verified (:status r)))
    (is (= :p/new (:pattern r)))
    (is (= action-digest (:action-sha256 r)))
    (is (= "abc1234" (:artifact-commit r)))
    (is (= loci (:application-claims r)))
    (is (= "review-2" (get-in r [:reviewer :job-id])))
    (is (string? (:observation-sha256 r)))))

(deftest mismatched-review-bindings-refuse
  (doseq [[field value expected]
          [[:pattern :p/other :pattern]
           [:dispatched-action-sha256 "wrong-action" :dispatched-action]
           [:artifact-commit "wrong-commit" :artifact-commit]]]
    (let [r (sut/observation
             (assoc base :author-text (line sut/author-marker claim)
                    :reviewer-text (line sut/reviewer-marker (assoc review field value))))]
      (is (= :refused (:status r)))
      (is (some #{expected} (:mismatches r))))))

(deftest malformed-or-uncheckable-claims-never-verify
  (let [bad-claim (assoc claim :loci [])
        r (sut/observation
           (assoc base :author-text (line sut/author-marker bad-claim)
                  :reviewer-text (line sut/reviewer-marker review)))]
    (is (= :refused (:status r)))
    (is (some #{:application-loci} (:mismatches r)))))

(deftest unpinned-pattern-source-refuses
  (let [r (sut/observation
           (assoc base :pattern-source nil
                  :author-text (line sut/author-marker claim)
                  :reviewer-text (line sut/reviewer-marker review)))]
    (is (= :refused (:status r)))
    (is (some #{:pattern-source-pin} (:mismatches r)))))
