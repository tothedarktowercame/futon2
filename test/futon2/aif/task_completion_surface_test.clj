(ns futon2.aif.task-completion-surface-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.task-completion-surface :as completion]
            [wm-run-facts :as facts]))

(defn describe-text [text]
  (let [file (java.io.File/createTempFile "excursion-completion-" ".md")]
    (try
      (spit file text)
      (completion/describe {:id "E-comprehension-foundation"
                            :source {:path (.getPath file)
                                     :sha256 (identity/sha256 (.getBytes text "UTF-8"))}})
      (finally (io/delete-file file)))))

(def v1
  ;; Click 11's picked completion carrier, projected to the fields under test.
  {:schema :wm/completion-surface-v1
   :target "E-comprehension-foundation"
   :completion-class :checkbox-locators-with-retained-prose
   :checkbox-count 4 :criteria-count 0 :located-criteria-count 0
   :unlocated-criteria-count 0
   :source-pin {:path "/home/joe/code/futon6/holes/excursions/E-comprehension-foundation.md"
                :sha256 "6863756378922c1aa289e75c0c750f04bc66e797e04dae721f3c9e39b1afb663"}
   :producer :selected-task-revision-baseline
   :baseline {:status :absent :reason :selected-task-baseline-not-retained}})

(defn selection-record [c]
  {:outer/selection
   {:selection {:selected {:target "E-comprehension-foundation"}}
    :field-facts {:admitted {"E-comprehension-foundation"
                            {:status :ready :rank 1 :cone 1 :unblocks 0
                             :pin "pinned-row" :reach {:value 1/2} :completion c}}}}})

(deftest discovery-has-work-phase-pointer-not-missing-baseline
  (let [c (describe-text "# Excursion

## Completion
- [ ] first
- [x] second
- [ ] third
- [ ] fourth
")]
    (is (= :wm/completion-surface-v2 (:schema c)))
    (is (= 3 (:checkbox-count c)))
    (is (not (contains? c :baseline)))
    (is (= :work (:baseline-phase c)))
    (is (= :selected-task-revision-baseline (:baseline-producer c)))
    (is (= [] (facts/selection-absence-paths (selection-record c))))))

(deftest undefined-completion-is-still-a-selection-deficit
  (let [c (describe-text "# Excursion

Just an idea.
")]
    (is (= :completion-definition-not-found (:completion-class c)))
    (is (= [[:outer/selection :pick-fact :completion :completion-class]]
           (facts/selection-absence-paths (selection-record c))))))

(deftest acceptance-evidence-bullets-are-an-unlocated-completion-definition
  (let [c (describe-text "# Do not fold apparatus failures

## Acceptance evidence

- A fixture run that ends `:incomplete` with a resume-protocol failure kind
  does not change the target's alpha; it appears on the interoceptive record.
- A fixture run that ends with a genuine task abstention or task failure
  still updates alpha as before.
- The before/after alpha for the three 10-18 targets, with the dataset
  digests.
")]
    (is (= :completion-prose-needs-locators (:completion-class c)))
    (is (= 3 (:criteria-count c)))
    (is (= 3 (:unlocated-criteria-count c)))
    (is (zero? (:located-criteria-count c)))))

(deftest v1-and-selection-dependencies-stay-visible
  (is (= [[:outer/selection :pick-fact :completion :baseline]]
         (facts/selection-absence-paths (selection-record v1))))
  (let [v2 (-> v1 (dissoc :baseline)
               (assoc :schema :wm/completion-surface-v2
                      :baseline-phase :work :baseline-producer :selected-task-revision-baseline))]
    (doseq [k [:status :rank :cone :unblocks :reach :pin]]
      (is (= [[:outer/selection :pick-fact k]]
             (facts/selection-absence-paths
              (assoc-in (selection-record v2)
                        [:outer/selection :field-facts :admitted "E-comprehension-foundation" k]
                        {:status :absent :reason :required-selection-fact-missing})))))
    ;; A malformed v2 which still carries a typed baseline absence is not
    ;; silently laundered; only the new producer removes that placeholder.
    (is (= [[:outer/selection :pick-fact :completion :baseline]]
           (facts/selection-absence-paths
            (selection-record (assoc v2 :baseline (:baseline v1))))))))

(deftest required-work-baseline-absence-remains-on-work-path
  (is (= [[:failure :baseline]]
         (facts/absence-paths
          {:failure {:baseline {:status :absent
                                :reason :pre-author-baseline-unavailable}}}))))
