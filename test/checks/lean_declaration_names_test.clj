(ns checks.lean-declaration-names-test
  (:require [clojure.test :refer [deftest is]]
            [checks.lean-declaration-names :as names]))

(deftest qualified-name-and-nested-section
  (let [prefixes (names/namespace-prefixes
                 ["namespace DarkTower.WarMachine.PolicyHorizon" "section Test"
                  "noncomputable def horizonEFE := 1" "end Test" "def next := 2"
                  "namespace Nested" "def witness := 3" "end Nested" "end DarkTower.WarMachine.PolicyHorizon"])]
    (is (= "DarkTower.WarMachine.PolicyHorizon" (nth prefixes 2)))
    (is (= "DarkTower.WarMachine.PolicyHorizon" (nth prefixes 4)))
    (is (= "DarkTower.WarMachine.PolicyHorizon.Nested" (nth prefixes 6)))
    (is (= "" (last prefixes)))))

(deftest qualified-join-does-not-borrow-another-modules-leaf
  (let [index (names/index-declarations
               [{:name "horizonEFE" :qualified-name "DarkTower.WarMachine.Other.horizonEFE" :path "Other.lean" :line 1 :kind "def"}
                {:name "horizonEFE" :qualified-name "DarkTower.WarMachine.PolicyHorizon.horizonEFE" :path "PolicyHorizon.lean" :line 61 :kind "def"}])]
    (is (= "PolicyHorizon.lean" (:path (get index "PolicyHorizon.horizonEFE"))))
    (is (= 61 (:line (get index "DarkTower.WarMachine.PolicyHorizon.horizonEFE"))))
    (is (nil? (get index "Missing.horizonEFE")))))
