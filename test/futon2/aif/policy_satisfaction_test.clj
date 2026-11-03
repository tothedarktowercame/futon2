(ns futon2.aif.policy-satisfaction-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.outer-observation :as outer-observation]
            [futon2.aif.outer-select :as outer-select]
            [futon2.aif.outer-state :as outer-state]
            [futon2.aif.placement-adapter :as placement-adapter]
            [futon2.aif.policy-authoring-resolution :as resolution]
            [futon2.aif.policy-satisfaction :as sut]
            [futon2.aif.wm.authored.req-f0ba279a :as authored]))

(def mission "M-futonzero-prelim-practice")
(def policy-id "M-futonzero-prelim-practice/policy-87eb5c880a0b9ab8e194")
(def mission-sha "e1b01eb3e0c1cef716174978e63a6b9ac100e968032bb59adbdf31929d805cf0")
(def grounded {:repo "/home/joe/code/futon0"
               :sha "cfb1408629826c6d825e2ddc06aad84ee5ac6108"})
(def checks (vec (sort-by pr-str (map :completion-check (vals authored/clauses)))))
(def authoring-resolution (resolution/for-policy policy-id))
(def construction
  {:mission mission :selected-cascade policy-id
   :cascade {:operational-plan
             {:steps (mapv (fn [check] {:clause {:completion-check check}}) checks)}
             :selected-action {:authoring-resolution authoring-resolution}}})
(def selection-judgment
  {:ranked-candidates [{:target mission :cascade-id policy-id
                        :action {:authoring-resolution authoring-resolution}}]})
(def qualified
  {:run/id "attempt-002"
   :failure {:kind :already-satisfied :stage :author-wait
             :detail {:failure-kind :already-satisfied :failure-stage :author-wait
                      :outcome :already-satisfied :grounded-commit grounded}}
   :run-output {:outcome :already-satisfied}})
(def field {:families [{:status :computed :target-id mission
                        :policies [{:policy-id policy-id :cascade {:nodes []}
                                    :authoring-resolution authoring-resolution}
                                   {:policy-id (str mission "/policy-other") :cascade {:nodes []}}]
                        :failures [] :failure-count 0}]})

(deftest producer-qualified-close-excludes-exact-next-policy
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "wm-policy-satisfaction" (make-array java.nio.file.attribute.FileAttribute 0)))
        retained (sut/retain! root {:run-record qualified :construction construction
                                    :selection-judgment selection-judgment
                                    :mission-source-sha256 mission-sha})
        next-field (sut/exclude-satisfied field root {mission mission-sha})
        family (first (:families next-field))]
    (is (= policy-id (:policy-id retained)))
    (is (= checks (:completion-checks retained)))
    (is (= grounded (:grounded-commit retained)))
    (is (= :all-policies-already-satisfied
           (:reason (sut/all-satisfied-exclusion root mission mission-sha))))
    (is (.isAbsolute (io/file sut/default-root)))
    (is (= (get-in retained [:record-pin :sha256])
           (evidence/sha256
            (java.nio.file.Files/readAllBytes
             (.toPath (io/file (get-in retained [:record-pin :path])))))))
    (is (= [(str mission "/policy-other")] (mapv :policy-id (:policies family))))
    (is (= :policy-already-satisfied (-> family :failures last :kind)))
    (is (get-in family [:failures 0 :record-pin :sha256]))
    (testing "mission byte change invalidates the evidence"
      (is (= 2 (count (get-in (sut/exclude-satisfied field root {mission "changed"})
                              [:families 0 :policies])))))
    (is (nil? (sut/all-satisfied-exclusion root mission "changed")))
    (testing "authored check-set change invalidates the evidence"
      (let [changed (assoc-in field [:families 0 :policies 0 :authoring-resolution]
                              (assoc authoring-resolution :completion-checks []))]
        (is (= 2 (count (get-in (sut/exclude-satisfied changed root {mission mission-sha})
                                [:families 0 :policies]))))))
    (testing "an unqualified close writes nothing"
      (is (nil? (sut/retain! root {:run-record (assoc-in qualified [:failure :stage] :build)
                                   :construction construction
                                   :selection-judgment selection-judgment
                                   :mission-source-sha256 mission-sha}))))))

(deftest excluding-the-only-policy-is-typed
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "wm-policy-satisfaction" (make-array java.nio.file.attribute.FileAttribute 0)))
        one (assoc-in field [:families 0 :policies]
                      [{:policy-id policy-id :cascade {:nodes []}
                        :authoring-resolution authoring-resolution}])]
    (sut/retain! root {:run-record qualified :construction construction
                       :selection-judgment selection-judgment
                       :mission-source-sha256 mission-sha})
    (let [family (get-in (sut/exclude-satisfied one root {mission mission-sha}) [:families 0])]
      (is (= :failed (:status family)))
      (is (empty? (:policies family)))
      (is (= :policy-already-satisfied (-> family :failures last :kind))))))

(defonce ^:private giraffe-loader
  (doto (clojure.lang.DynamicClassLoader.
         (.getContextClassLoader (Thread/currentThread)))
    (.addURL (.toURL (.toURI (io/file "/home/joe/code/futon3c/src"))))))

(defn- actual-var [symbol]
  (with-bindings {clojure.lang.Compiler/LOADER giraffe-loader}
    (requiring-resolve symbol)))

(deftest qualified-close-does-not-exclude-target-from-real-outer-field
  ;; Since 1cbe9f55b (2026-11-01) the outer field is the registry-open task
  ;; population and its exclusions are task-level evidence gaps only
  ;; (`outer-observation/task-exclusion-reasons`): "registry statuses and
  ;; inner-policy satisfaction are not exclusions". The producer above still
  ;; reports the qualified close; the real field does not consume it. Before
  ;; that commit this test pinned the opposite (the close removed the target).
  ;; Retained row (run 20261101T224600.983Z-6f02dbc0): M-futonzero-prelim-practice
  ;; is :ready at rank 43, source-order 14, dependency record pin 225b1fa5...,
  ;; reach 0.
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "wm-policy-outer" (make-array java.nio.file.attribute.FileAttribute 0)))
        run-dir (io/file "/home/joe/code/storage/giraffe/20261101T224600.983Z-6f02dbc0")
        makeup (outer-state/load-makeup)
        state (outer-state/init-state makeup)]
    (with-redefs [sut/default-root (.getAbsolutePath root)]
      (sut/retain! root {:run-record qualified :construction construction
                         :selection-judgment selection-judgment
                         :mission-source-sha256 mission-sha})
      (let [outer (binding [outer-observation/*resolve-var* actual-var]
                    (outer-observation/giraffe-field run-dir makeup))
            exclusion (some #(when (= mission (:target %)) %)
                            (:outer-field/excluded outer))
            ;; the runtime attaches placements between the field and the
            ;; selector (full_loop_runtime.clj:1407-1414); so does this test
            placed-outer (placement-adapter/annotate
                          outer
                          (placement-adapter/load-placements
                           placement-adapter/default-paths))
            result (outer-select/select
                    makeup state
                    (assoc placed-outer :focus "M-wm-outer-selection"
                           :relation (fn [_ _] :focus)))]
        (is (= :all-policies-already-satisfied
               (:reason (sut/all-satisfied-exclusion root mission mission-sha)))
            "the producer still reports the qualified close")
        (is (nil? exclusion) "satisfaction is not an outer exclusion (1cbe9f55b)")
        (is (some #{mission} (map :target (:field outer))))
        (is (every? outer-observation/task-exclusion-reasons
                    (map :reason (:outer-field/excluded outer))))
        ;; Selected over the real pin-era field with the real placements:
        ;; E-memory-whitepaper-v2-programme, row :ready, rank 2, source-order
        ;; 540, cone 3, unblocks 1; no placement row, so its placement is the
        ;; typed absence and a non-placement cascade selects it.
        (is (= "E-memory-whitepaper-v2-programme"
               (get-in result [:selection :selected :target])))))))
