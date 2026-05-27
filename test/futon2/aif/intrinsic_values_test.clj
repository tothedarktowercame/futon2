(ns futon2.aif.intrinsic-values-test
  "Tests for the per-class intrinsic-value table (R12 narrow-take-up).

   Coverage:
     - Beta(1,1) prior defaults: 0.5 intrinsic-value, fresh-entry shape
     - posterior-mode arithmetic for general (α, β)
     - rehydrate! latest-wins per class; ordering by :as-of
     - apply-update! folds one record correctly
     - credit-for unknown class returns prior mode
     - next-record arithmetic (α += followthrough, β += emissions - followthrough)
     - normalise-record handles keyword vs string class encoding"
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.intrinsic-values :as iv]))

(use-fixtures :each
  (fn [t]
    (iv/reset-to-prior!)
    (try (t) (finally (iv/reset-to-prior!)))))

;; ---------------------------------------------------------------------------
;; Prior defaults
;; ---------------------------------------------------------------------------

(deftest fresh-entry-defaults
  (testing "fresh entry holds Beta(1,1) prior and 0.5 intrinsic-value"
    (let [e (iv/fresh-entry)]
      (is (= 1.0 (:alpha e)))
      (is (= 1.0 (:beta e)))
      (is (= 0.5 (double (:intrinsic-value e))))
      (is (zero? (:n-emissions e)))
      (is (zero? (:n-followthrough e)))
      (is (nil? (:as-of e))))))

(deftest credit-for-defaults-to-prior-mode
  (testing "credit-for unknown class returns 0.5"
    (is (= 0.5 (double (iv/credit-for :address-sorry))))
    (is (= 0.5 (double (iv/credit-for :open-mission))))
    (is (= 0.5 (double (iv/credit-for :fire-pattern))))
    (is (= 0.5 (double (iv/credit-for :totally-unknown-class))))))

;; ---------------------------------------------------------------------------
;; rehydrate! latest-wins
;; ---------------------------------------------------------------------------

(deftest rehydrate-latest-record-wins-per-class
  (testing "out-of-order records still resolve to latest-as-of per class"
    (let [records
          [{:class :address-sorry
            :as-of "2026-05-15T00:00:00Z"
            :alpha-post 2.0 :beta-post 1.0 :intrinsic-value-post 0.67
            :n-emissions-in-window 1 :n-followthrough-in-window 1}
           {:class :address-sorry
            :as-of "2026-05-20T00:00:00Z"
            :alpha-post 4.0 :beta-post 2.0 :intrinsic-value-post 0.75
            :n-emissions-in-window 4 :n-followthrough-in-window 3}
           {:class :open-mission
            :as-of "2026-05-18T00:00:00Z"
            :alpha-post 1.0 :beta-post 3.0 :intrinsic-value-post 0.25
            :n-emissions-in-window 2 :n-followthrough-in-window 0}]]
      (iv/rehydrate! records)
      (is (= 0.75 (double (iv/credit-for :address-sorry))))
      (is (= 0.25 (double (iv/credit-for :open-mission))))
      (is (= 0.5 (double (iv/credit-for :fire-pattern))))  ; no record
      (testing "n-emissions and n-followthrough reflect latest record"
        (let [snap (iv/current)]
          (is (= 4 (get-in snap [:address-sorry :n-emissions])))
          (is (= 3 (get-in snap [:address-sorry :n-followthrough])))
          (is (= 2 (get-in snap [:open-mission :n-emissions])))
          (is (= 0 (get-in snap [:open-mission :n-followthrough]))))))))

;; ---------------------------------------------------------------------------
;; apply-update!
;; ---------------------------------------------------------------------------

(deftest apply-update-folds-single-record
  (testing "apply-update! installs the record into the atom"
    (let [rec {:class :address-sorry
               :as-of "2026-05-21T12:00:00Z"
               :alpha-post 3.0 :beta-post 1.0 :intrinsic-value-post 1.0
               :n-emissions-in-window 2 :n-followthrough-in-window 2}]
      (iv/apply-update! rec)
      (is (= 1.0 (double (iv/credit-for :address-sorry))))
      (is (= 3.0 (get-in (iv/current) [:address-sorry :alpha])))
      (is (= 1.0 (get-in (iv/current) [:address-sorry :beta]))))))

;; ---------------------------------------------------------------------------
;; next-record arithmetic
;; ---------------------------------------------------------------------------

(deftest next-record-beta-update
  (testing "α += followthrough; β += (emissions - followthrough)"
    (let [prior (iv/fresh-entry)
          rec (iv/next-record :address-sorry prior 5 2
                              {:as-of "2026-05-21T12:00:00Z"
                               :outer-loop-run-id "wm-ol:20260521T120000Z"
                               :window-days 14
                               :evidence-refs ["git:futon2:abc:sorrys.edn"]})]
      (is (= 1.0 (:alpha-pre rec)))
      (is (= 1.0 (:beta-pre rec)))
      (is (= 3.0 (:alpha-post rec)))   ; 1 + 2 followthrough
      (is (= 4.0 (:beta-post rec)))    ; 1 + (5 - 2) ignored
      (is (= 0.5 (double (:intrinsic-value-pre rec))))
      (is (= (/ (- 3.0 1.0) (- (+ 3.0 4.0) 2.0))  ; (α-1)/(α+β-2)
             (double (:intrinsic-value-post rec))))
      (is (= 5 (:n-emissions-in-window rec)))
      (is (= 2 (:n-followthrough-in-window rec)))
      (is (= ["git:futon2:abc:sorrys.edn"] (:evidence-refs rec))
          "evidence-refs preserved as-is"))))

(deftest next-record-zero-emissions-no-change
  (testing "no emissions in window → post == pre"
    (let [prior {:alpha 3.0 :beta 2.0 :intrinsic-value 0.667
                 :n-emissions 5 :n-followthrough 2 :as-of "2026-05-20T00:00:00Z"}
          rec (iv/next-record :open-mission prior 0 0
                              {:as-of "2026-05-21T12:00:00Z"
                               :outer-loop-run-id "wm-ol:20260521T120000Z"
                               :window-days 14})]
      (is (= 3.0 (:alpha-post rec)))
      (is (= 2.0 (:beta-post rec)))
      (is (= (double (:intrinsic-value-pre rec))
             (double (:intrinsic-value-post rec)))))))

(deftest next-record-caps-followthrough-at-emissions
  (testing "§2.J safety: n-followthrough capped at n-emissions; uncapped
            count preserved in :n-followthrough-observed when cap bites"
    (let [prior (iv/fresh-entry)
          ;; 3 emissions but 5 follow-through events (operator addressed
          ;; sorrys independently of WM recommendations)
          rec (iv/next-record :address-sorry prior 3 5
                              {:as-of "2026-05-21T12:00:00Z"
                               :outer-loop-run-id "wm-ol:20260521T120000Z"
                               :window-days 14})]
      (is (= 3 (:n-followthrough-in-window rec))
          "followthrough capped at emissions")
      (is (= 5 (:n-followthrough-observed rec))
          ":n-followthrough-observed surfaces the original count")
      (is (= 4.0 (:alpha-post rec)) "α += min(5,3) = 3")
      (is (= 1.0 (:beta-post rec)) "β += (3 - 3) = 0; remains at prior")))
  (testing "when cap doesn't bite, :n-followthrough-observed is absent"
    (let [prior (iv/fresh-entry)
          rec (iv/next-record :address-sorry prior 5 2
                              {:as-of "2026-05-21T12:00:00Z"
                               :outer-loop-run-id "wm-ol:20260521T120000Z"
                               :window-days 14})]
      (is (= 2 (:n-followthrough-in-window rec)))
      (is (nil? (:n-followthrough-observed rec))
          ":n-followthrough-observed absent when no cap applied")))
  (testing "negative followthrough clamped to 0 (defensive)"
    (let [prior (iv/fresh-entry)
          rec (iv/next-record :address-sorry prior 3 -1
                              {:as-of "2026-05-21T12:00:00Z"
                               :outer-loop-run-id "wm-ol:20260521T120000Z"
                               :window-days 14})]
      (is (= 0 (:n-followthrough-in-window rec)))
      (is (= 1.0 (:alpha-post rec)) "α += 0; prior")
      (is (= 4.0 (:beta-post rec)) "β += (3 - 0) = 3"))))

;; ---------------------------------------------------------------------------
;; Round-trip: apply-update! then rehydrate! should be idempotent
;; ---------------------------------------------------------------------------

(deftest round-trip-apply-then-rehydrate
  (testing "rehydrate! after apply-update! gives the same atom state"
    (let [rec {:class :address-sorry
               :as-of "2026-05-21T12:00:00Z"
               :alpha-post 4.0 :beta-post 2.0 :intrinsic-value-post 0.75
               :n-emissions-in-window 4 :n-followthrough-in-window 3}]
      (iv/apply-update! rec)
      (let [snap-before (iv/current)]
        (iv/rehydrate! [rec])
        (is (= snap-before (iv/current)))))))

;; ---------------------------------------------------------------------------
;; Inner-loop invariant — the proof R12 actually wants
;; ---------------------------------------------------------------------------

(deftest inner-loop-invariant-credit-for-replaces-static-0.1
  (testing "R12 narrow-take-up: credit-for returns a value driven by atom,
            not the static 0.1 the old action_proposer hardcoded"
    ;; Empty atom — credit-for returns prior mode 0.5, NOT 0.1
    (is (not= 0.1 (double (iv/credit-for :address-sorry))))
    (is (= 0.5 (double (iv/credit-for :address-sorry))))
    ;; After an update, credit-for tracks the posterior
    (iv/apply-update! {:class :address-sorry
                       :as-of "2026-05-21T12:00:00Z"
                       :alpha-post 5.0 :beta-post 1.0 :intrinsic-value-post 1.0
                       :n-emissions-in-window 4 :n-followthrough-in-window 4})
    (is (= 1.0 (double (iv/credit-for :address-sorry))))
    (is (not= 0.1 (double (iv/credit-for :address-sorry))))))
