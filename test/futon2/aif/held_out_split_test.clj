(ns futon2.aif.held-out-split-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.held-out-split :as split]
            [futon2.aif.observation-checks :as checks]))

(def valid
  {:schema split/schema
   :disposition split/disposition
   :locator {:root "data/wm-full-loop-machinery-72/wm-contract-machinery-72-v1"
             :record "<attempt-id>/007-closed.edn"}
   :training-set ["attempt-001"]
   :held-out-set ["attempt-002" "attempt-003"]
   :outcome-classes [:result :no-result :failure :timeout]
   :window {:opens-after {:event :git-commit-containing-declaration}
            :minimum-observations 2}
   :calibration-authority :none})

(defn refusal [x]
  (try (split/validate x) nil
       (catch clojure.lang.ExceptionInfo e (:held-out-split/refusal (ex-data e)))))

(deftest valid-prospective-declaration
  (is (= valid (split/validate valid)))
  (let [declared (-> "wm/eig/held-out-split.edn" io/resource slurp edn/read-string)]
    (is (= declared (split/validate declared)))
    (is (checks/decl-present? (slurp (io/resource "wm/eig/held-out-split.edn"))
                              "HELD-OUT-SPLIT-DECLARED"))
    (is (false? (get-in declared [:claims :observations-collected?])))))

(deftest malformed-or-retrospective-declarations-refuse
  (testing "every exceptional outcome remains in the declared alphabet"
    (is (= :outcome-classes-incomplete
           (refusal (update valid :outcome-classes pop)))))
  (is (= :partition-overlap
         (refusal (assoc valid :training-set ["attempt-002"]))))
  (is (= :window-not-prospective
         (refusal (assoc-in valid [:window :opens-after] {:event :already-observed}))))
  (is (= :invalid-held-out-set
         (refusal (assoc valid :held-out-set ["attempt-002" "attempt-002"]))))
  (is (= :invalid-locator
         (refusal (assoc-in valid [:locator :record] ""))))
  (is (= :premature-calibration-authority
         (refusal (assoc valid :calibration-authority :passing)))))

;; ⟨1⟩6 supersession (claude-5 handoff): the v1 declaration is superseded,
;; not edited; a v2 declaration names a MINTABLE window; membership says
;; plainly which attempts are in.
(deftest superseded-v1-still-parses-and-satisfies-its-own-locator
  (let [d (edn/read-string (slurp (io/resource "wm/eig/held-out-split.edn")))]
    (is (= :wm/eig-held-out-split-v1 (:schema d)))
    (is (= "resources/wm/eig/held-out-split-v2.edn" (:superseded-by d)))
    (is (re-find #"machinery-72" (:superseded-reason d)))
    ;; the C4 head is unchanged — the split-declared-valid token does not regress
    (is (= (symbol "HELD-OUT-SPLIT-DECLARED") (:disposition d)))))

(deftest v2-declaration-validates-and-rejects-non-mintable-labels
  (let [d (edn/read-string (slurp (io/resource "wm/eig/held-out-split-v2.edn")))
        validated (split/validate-v2 d)]
    (is (= d validated) "the real v2 resource validates"))
  ;; a v2 naming cohort/attempt directory labels is refused
  (let [d (assoc (edn/read-string (slurp (io/resource "wm/eig/held-out-split-v2.edn")))
                 :window {:opens-after {:event :git-commit-containing-declaration}
                          :shape :named-set
                          :minimum-observations 2})]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Held-out EIG split refused"
                          (split/validate-v2 d))
        "the refusal fires")
    (let [r (try (split/validate-v2 d) nil (catch clojure.lang.ExceptionInfo e (ex-data e)))]
      (is (= :window-not-mintable (:held-out-split/refusal r)) (pr-str r)))))

(deftest window-membership-over-todays-attempts
  (let [d (edn/read-string (slurp (io/resource "wm/eig/held-out-split-v2.edn")))
          ;; the run records that exist today (target + recorded-at shapes)
          records [{:run-id "2026-09-23-1790136186"
                    :target "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade"
                    :recorded-at "2026-09-23T04:03:00Z"}
                   {:run-id "2026-09-23-1790110142"
                    :target "M-f11-find-production-successor"
                    :recorded-at "2026-09-22T20:00:00Z"}]
          m (split/window-membership d records)]
      (is (= 1 (count (:members m))) (pr-str m))
      (is (= "2026-09-23-1790136186" (get-in (first (:members m)) [:run-id])))
      (is (= :different-target (get-in (first (:not-members m)) [:reason]))
          "the non-member states its reason")))
