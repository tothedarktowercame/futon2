(ns futon2.aif.observation-checks-test
  "Checks against real pinned shas in futon2 and mathlib4."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.observation-checks :as oc]))

(def futon2-sha "b81afd97")   ; H7c-1 construction commit
(def mathlib-sha "3b19f6225e") ; EpistemicValue.lean

(deftest c3-path
  (is (true? (:observed (oc/check-path-exists {:repo "futon2" :sha futon2-sha :path "src/futon2/aif/construction.clj"}))))
  (is (false? (:observed (oc/check-path-exists {:repo "futon2" :sha futon2-sha :path "src/futon2/aif/no_such_file.clj"}))))
  ;; the file did not exist before its commit: observation is at the sha, not the working tree
  (is (false? (:observed (oc/check-path-exists {:repo "futon2" :sha "77cf311a" :path "src/futon2/aif/construction.clj"})))))

(deftest c4-decl
  (is (true? (:observed (oc/check-decl-in-file {:repo "mathlib4" :sha mathlib-sha
                                                 :path "DarkTower/WarMachine/EpistemicValue.lean"
                                                 :decl "theorem fixture_check_strictly_better"}))))
  (is (false? (:observed (oc/check-decl-in-file {:repo "mathlib4" :sha mathlib-sha
                                                  :path "DarkTower/WarMachine/EpistemicValue.lean"
                                                  :decl "theorem no_such_theorem"})))))

(deftest c5-registry
  (let [base {:repo "mathlib4" :sha "3726659d84"
              :bundle-path "DarkTower/WarMachine/machine-contracts-2026-09-17-r14/machine-contracts.json"}]
    (is (true? (:observed (oc/check-registry-entry (assoc base :entry "wm-machine-observe")))))
    (is (false? (:observed (oc/check-registry-entry (assoc base :entry "no-such-contract")))))))

(deftest refusals
  (is (= :no-locator (:kind (oc/check-path-exists {:repo "futon2" :sha futon2-sha}))))
  (is (= :unknown-sha (:kind (oc/check-path-exists {:repo "futon2" :sha "0000000000" :path "x"}))))
  (let [r (oc/observe {:t-path {:class :C3 :repo "futon2" :sha futon2-sha :path "src/futon2/aif/construction.clj"}
                       :t-judgement {:class :J}
                       :t-unknown {:class :C9}})]
    (is (= #{:t-path} (:observed r)))
    (is (= #{:t-judgement :t-unknown} (set (keys (:refused r)))))
    (is (every? #(= :no-mechanical-check (:kind %)) (vals (:refused r))))))

;; claude-7's applicability reading (2026-09-17): anchored declaration heads
(deftest c4-anchored-decl
  (let [text "theorem foo_bar : True := trivial\n-- theorem foo in a comment\n  theorem foo (x : Nat) : True := trivial\n"]
    (is (true? (oc/decl-present? text "theorem foo")))
    (is (false? (oc/decl-present? "theorem foo_bar : True := trivial\n" "theorem foo")))
    (is (false? (oc/decl-present? "-- theorem foo here\n" "theorem foo")))
    (is (true? (oc/decl-present? "(defn check-test-warrant\n  [x])" "(defn check-test-warrant")))))

(deftest c5-locus-resolves
  (let [base {:repo "mathlib4" :sha "52d6516922"
              :bundle-path "DarkTower/WarMachine/machine-contracts-2026-09-17-r15/machine-contracts.json"}
        r (oc/check-registry-entry (assoc base :entry "wm-token-observation"))]
    ;; every declared clojure-locus resolves at its repo's HEAD
    (is (true? (:observed r)) (pr-str (:evidence r)))
    (is (seq (get-in r [:evidence :clojure-loci])))))
