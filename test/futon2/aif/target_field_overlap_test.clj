(ns futon2.aif.target-field-overlap-test
  "M-wm-wiring row 8: each feasible entry of the target field carries
  :pair-overlap. Two targets' ΔG compare only when one's constructed
  candidate moves no token of the other's universe (target_comparison,
  mathlib4 759b8ca884); otherwise the pair is :incommensurable with the
  shared tokens named. No entry has a constructed candidate at HEAD, so every
  entry records the typed absence, never an empty map."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.target-field :as tf]
            [futon2.aif.target-field-test]))

(deftest a-shared-token-is-incommensurable-disjoint-is-comparable
  (let [f (tf/with-pair-overlap
            [{:target "M-a" :constructed-candidate {:produces #{:x :y}} :universe #{:x :y :a}}
             {:target "M-b" :constructed-candidate {:produces #{:z}} :universe #{:y :b}}
             {:target "M-c" :constructed-candidate {:produces #{:w}} :universe #{:c}}
             {:target "T-d" :next-step :read-criteria}])
        by (into {} (map (juxt :target :pair-overlap)) f)]
    (is (= {:incommensurable {:shared-tokens [:y]}} (get-in by ["M-a" "M-b"]))
        "M-a moves :y, which is in M-b's universe")
    (is (= {:comparable true} (get-in by ["M-a" "M-c"])))
    (is (= {:comparable true} (get-in by ["M-b" "M-a"])) "the relation is not symmetric: M-b's :z is not in U(M-a)")
    (is (= {:absent :no-universe} (get-in by ["M-a" "T-d"])))
    (is (= {:absent :no-constructed-candidate} (get by "T-d")))
    (is (not-any? #(= {} (:pair-overlap %)) f) "never an empty map")))

(deftest the-live-field-at-7bd17dfb-is-all-absent
  ;; the pinned live read (343 feasible): none carries a constructed candidate
  (let [fx (edn/read-string (slurp "test/fixtures/target-field/target-field@futon2-7bd17dfb.edn"))
        f (tf/with-pair-overlap (get-in fx [:decision :target-field :feasible]))]
    (is (= 343 (count f)))
    (is (every? #(= {:absent :no-constructed-candidate} (:pair-overlap %)) f))))

(deftest the-fields-other-keys-are-unchanged
  ;; fixture: pr-str of the target-field test layout's field from
  ;; target_field.clj at futon2 42b5abdc, before row 8
  (let [f (#'futon2.aif.target-field-test/field (#'futon2.aif.target-field-test/layout))]
    (is (= (edn/read-string (slurp "test/fixtures/target-field-overlap/field-before@futon2-42b5abdc.edn"))
           (pr-str (update f :feasible #(mapv (fn [e] (dissoc e :pair-overlap)) %)))))
    (is (every? :pair-overlap (:feasible f)))))
