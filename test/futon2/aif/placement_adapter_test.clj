(ns futon2.aif.placement-adapter-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.placement-adapter :as adapter]))

(def fixture-paths
  {:tsv "test/resources/placement-adapter/placements.tsv"
   :classes "test/resources/placement-adapter/classes.tsv"})

(deftest loads-pinned-per-stem-and-class-placements
  (let [loaded (adapter/load-placements fixture-paths)]
    (is (= :pinned (:status loaded)))
    (is (re-matches #"[0-9a-f]{64}" (get-in loaded [:source :tsv :sha256])))
    (is (re-matches #"[0-9a-f]{64}" (get-in loaded [:source :classes :sha256])))
    (is (= 6 (count (:rows loaded))))
    (is (= 5 (get-in loaded [:source :rows])))
    (is (= 1 (get-in loaded [:source :class-rows])))
    (is (= :class (get-in loaded [:rows "E-class-only" :source])))
    (is (= "x" (get-in loaded [:rows "E-class-only" :class])))
    (is (= "cluster:x" (get-in loaded [:rows "E-class-only" :action])))
    (is (= :per-stem (get-in loaded [:rows "E-overlap" :source])))))

(deftest parses-way-lists
  (let [rows (:rows (adapter/load-placements fixture-paths))]
    (testing "none is no way"
      (is (= [] (get-in rows ["E-held" :ways]))))
    (testing "comma-separated ways retain their order"
      (is (= ["a" "b"] (get-in rows ["E-work" :ways]))))))

(deftest missing-tsv-is-typed-absence
  (let [loaded (adapter/load-placements
                (assoc fixture-paths :tsv "test/resources/placement-adapter/missing.tsv"))]
    (is (= :absent (:status loaded)))
    (is (= :placements-unavailable (:reason loaded)))
    (is (= adapter/absent-placement (adapter/placement-fact loaded "E-work"))))
  (testing "a missing optional class file leaves per-stem placements pinned"
    (let [loaded (adapter/load-placements
                  (assoc fixture-paths :classes "test/resources/placement-adapter/missing.tsv"))]
      (is (= :pinned (:status loaded)))
      (is (= 0 (get-in loaded [:source :class-rows])))
      (is (= :absent (get-in loaded [:source :classes :status])))
      (is (= 5 (count (:rows loaded)))))))

(deftest annotates-every-target-and-retains-source
  (let [loaded (adapter/load-placements fixture-paths)
        observation {:facts {"E-work" {:status :ready}
                             "E-unplaced" {:status :ready}}}
        annotated (adapter/annotate observation loaded)]
    (is (= "E-work" (get-in annotated [:facts "E-work" :placement :id])))
    (is (= adapter/absent-placement
           (get-in annotated [:facts "E-unplaced" :placement])))
    (is (= :pinned (get-in annotated [:placement/source :status])))
    (is (= (get-in loaded [:source :tsv :sha256])
           (get-in annotated [:placement/source :tsv :sha256])))))
