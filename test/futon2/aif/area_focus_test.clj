(ns futon2.aif.area-focus-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.area-focus :as sut]
            [futon2.aif.mission-registry :as mission-registry]
            [futon2.aif.operator-focus :as operator-focus]
            [futon2.aif.selection-record :as selection-record]))

(def rows {"direct" {:target "direct" :area "WM" :cite "p:1" :quote "q"}
           "also" {:target "also" :area "APM" :also "WM"}
           "other" {:target "other" :area "memory"}})
(def present {:result {:status :present :ranked [{:area "WM" :share 1/2}]}})

(deftest area-relation-rule
  (is (= [:focus :focus :focused]
         ((juxt :relation-key :class :scored-class)
          (sut/classify-target rows "corpus" present "focus" "direct"))))
  (is (= :associated (:class (sut/classify-target rows "c" present "f" "also"))))
  (is (= :useful-elsewhere (:class (sut/classify-target rows "c" present "f" "other"))))
  (is (= :no-area-row (get-in (sut/classify-target rows "c" present "f" "missing") [:relation :reason])))
  (is (= :no-recent-operator-turns
         (get-in (sut/classify-target rows "c" {:result {:status :absent :reason :no-recent-operator-turns}}
                                            "f" "direct") [:relation :reason]))))

(deftest receipt-class-is-the-class-the-scorer-sees
  ;; wm-click-loop-repair-260438a7: held run 2026-11-04-ede75c1a stopped at
  ;; outer-selection with :relation-receipt-class-mismatch because the area
  ;; receipt recorded the Lean projection (:unrelated) in :class while the
  ;; scorer's relation fn returned the relation (:useful-elsewhere). One
  ;; classification for both the scoring path and the receipt.
  (let [receipt (sut/classify-target rows "c" present "f" "other")
        scorer-class (:relation-key receipt)]
    (is (= scorer-class (:class receipt))
        "receipt :class must equal the class the scorer's relation fn returns")))

(deftest third-place-ties-are-all-focus
  (let [focus {:result {:status :present :ranked [{:area "A"} {:area "B"} {:area "C"} {:area "D"}]}}]
    (is (= #{"A" "B" "C" "D"} (sut/focus-areas focus)))))

(deftest pin-change-defers
  (let [f (java.io.File/createTempFile "focus-pin" ".edn")]
    (spit f "{:as-of \"2026-11-04T00:00:00Z\"}")
    (let [pin (sut/read-manifest f)]
      (spit f "{:changed true}")
      (is (= :focus-source-changed (:deferred (sut/verify-pin pin)))))))

(deftest sixty-one-minutes-is-stale
  (let [d (.toFile (java.nio.file.Files/createTempDirectory "focus-manifests" (make-array java.nio.file.attribute.FileAttribute 0)))
        f (java.io.File. d (str "operator-focus-" (apply str (repeat 64 "a")) ".edn"))]
    (spit f "{:as-of \"2026-11-04T00:00:00Z\"}")
    (is (nil? (sut/newest-retained d "2026-11-04T01:01:00Z")))))

(deftest selection-record-retains-focus-pin
  (let [d (.toFile (java.nio.file.Files/createTempDirectory "focus-selection" (make-array java.nio.file.attribute.FileAttribute 0)))
        pin {:path "/tmp/focus.edn" :sha256 "abc" :as-of "2026-11-04T00:00:00Z" :age-minutes 4}
        ref (selection-record/write-decision! d {:run-id "r" :startedAt "2026-11-04T00:04:00Z"}
                                              {:focus/source {:manifest pin}} {:status :selected})]
    (is (= pin (get-in (selection-record/read-reference ref)
                       [:outer/selection :focus/source :manifest])))))

(deftest acquire-focus-defaults-to-canonical-code-root
  (let [d (.toFile (java.nio.file.Files/createTempDirectory "no-focus" (make-array java.nio.file.attribute.FileAttribute 0)))
        seen (atom nil)]
    (with-redefs [operator-focus/read-focus! (fn [opts] (reset! seen opts) {:status :absent})]
      (is (nil? (sut/acquire-focus! {:decision-as-of "2026-11-04T00:00:00Z"
                                     :manifest-dir d :code-root nil :getenv-fn (constantly nil)}))))
    (is (= (str mission-registry/default-code-root "/futon3c/holes/excursions/ant-reads")
           (:area-corpus-path @seen)))
    (is (= (str mission-registry/default-code-root "/futon2") (:task-repo @seen)))))
