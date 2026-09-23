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
  ;; Every run that exists TODAY was recorded before the v2 declaration was
  ;; registered, so the window is correctly EMPTY: nothing already run can
  ;; be held out. The earlier version of this test asserted the ticket's own
  ;; already-closed run was a member, which is the retrospective inclusion
  ;; the supersession exists to remove (claude-5's review of d55e28f0).
  (let [d (edn/read-string (slurp (io/resource "wm/eig/held-out-split-v2.edn")))
          records [{:run-id "2026-09-23-1790136186"
                    :target "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade"
                    :recorded-at "2026-09-23T04:03:00Z"}
                   {:run-id "2026-09-23-1790110142"
                    :target "M-f11-find-production-successor"
                    :recorded-at "2026-09-22T20:00:00Z"}]
          m (split/window-membership d records)]
      (is (empty? (:members m))
          "nothing already run is held out")
      (is (= [:before-registration :different-target]
             (mapv :reason (:not-members m)))
          "and each non-member states which rule excluded it")))

(deftest a-run-recorded-before-registration-is-not-held-out
  ;; The bad case the date comparison let through: the ticket's own second
  ;; click ran earlier on the SAME DAY the v2 declaration was written, so
  ;; its outcome was already known. It must not be a member (claude-5's
  ;; review of d55e28f0).
  (let [decl (edn/read-string (slurp (io/file "resources/wm/eig/held-out-split-v2.edn")))
        ticket (:ticket/id decl)
        {:keys [members not-members]}
        (split/window-membership
         decl
         [{:run-id "2026-09-23-1790136186" :target ticket
           :recorded-at "2026-09-23T03:23:06Z"}      ; before registration
          {:run-id "2026-09-23-1790161992" :target ticket
           :recorded-at "2026-09-23T11:13:12Z"}      ; before registration
          {:run-id "2026-09-24-1790200000" :target ticket
           :recorded-at "2026-09-24T09:00:00Z"}      ; after: genuinely held out
          {:run-id "2026-09-24-1790200001" :target "M-f11-find-production-successor"
           :recorded-at "2026-09-24T10:00:00Z"}])]
    (is (= ["2026-09-24-1790200000"] (mapv :run-id members))
        (pr-str members))
    (is (= #{:before-registration :different-target}
           (set (map :reason not-members)))
        (pr-str not-members))))

(deftest a-declaration-without-a-registration-instant-refuses
  (let [decl (-> (edn/read-string (slurp (io/file "resources/wm/eig/held-out-split-v2.edn")))
                 (dissoc :registered-at))]
    (is (= :missing-registration-instant
           (try (split/validate-v2 decl) nil
                (catch clojure.lang.ExceptionInfo e
                  (:held-out-split/refusal (ex-data e))))))))
