(ns futon2.aif.c5-habit-legacy-candidate-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.selected-outer-cascade-policy :as selected]))

;; Live pin: the full sealed carrier in tick-run-record-2026-10-23-57cd4dc4-
;; 7bf2-43ad-9cbd-7549e183ab32.edn.  The click seals with
;; *print-namespace-maps* false, and family/menu pin verification still depends
;; on that binding (DOSSIER C5, 2026-10-23 22:02Z), so the live call preserves
;; that sealing condition.  The full carrier is needed because verification
;; intentionally covers every sealed field.
(def ^:private live-run-path
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-23-57cd4dc4-7bf2-43ad-9cbd-7549e183ab32.edn")
(def ^:private pinned-cascade-id
  "M-stack-geometry/policy-c7d7518e1e185c2e2318")

(defn- live-carrier []
  (get-in (edn/read-string {:default tagged-literal} (slurp live-run-path))
          [:outer-task-selection :policy :selected-outer-cascade-policy]))

(defn- live-decision [carrier]
  (binding [*print-namespace-maps* false]
    (selected/decision carrier)))

(deftest selected-outer-projection-retains-legacy-wc-candidate-when-present
  (let [carrier (live-carrier)
        result (live-decision carrier)]
    (testing "the retained META entry is the live, sealed authority"
      (is (nil? (binding [*print-namespace-maps* false]
                 (selected/verify carrier))))
      (is (= pinned-cascade-id (get-in carrier [:selected-entry :cascade-id])))
      (is (= pinned-cascade-id (get-in carrier [:selected-entry :policy-id]))))
    (testing "the verified carrier projects its legacy W_c candidate"
      (is (= :selected (:status result)))
      (is (= pinned-cascade-id
             (get-in result [:decision :selection-law :candidate]))))))

(deftest selected-outer-projection-omits-legacy-wc-candidate-when-absent
  ;; The seal covers :selected-entry, so an entry with :cascade-id removed is
  ;; intentionally not authentic. Stub only verification to exercise the
  ;; projection's absence rule; this does not claim the altered carrier passed
  ;; verification.
  (let [carrier (update (live-carrier) :selected-entry dissoc :cascade-id)
        result (with-redefs [selected/verify (constantly nil)]
                 (live-decision carrier))]
    (is (= :selected (:status result)))
    (is (not (contains? (:decision result) :selection-law)))))
