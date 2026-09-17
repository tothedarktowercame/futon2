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
  (is (= :no-locator (:kind (oc/check-test-warrant {:repo "futon2" :ns "x"}))))
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

;; Real warrants (futon3c 33824f0f, ab388662). These call the registry CLI
;; (a few seconds each) and depend on the warrants still matching the checkout.
;; The futon2 warrant covers a namespace this file does not, so editing these
;; checks does not stale it.
(def futon2-warrant "test-registry-0b4a2378bb224daa499a8012209eff3a35208871e529c7b5c1eb578364496978")
;; MachineContracts build warrant after the sorry/error parse fix (bundle r15)
(def contracts-warrant "test-registry-d97d4143f16ccf4248c5bfdd964c8ce2642ad0e3177885977d0424fcb145b936")

(deftest c2-test-warrant
  (let [r (oc/check-test-warrant {:repo "futon2" :entry-id futon2-warrant :ns "futon2.aif.observation-rates-test"})]
    (is (true? (:observed r)) (pr-str r))
    (is (string? (get-in r [:cutoff "futon2"]))))
  ;; a warrant for another namespace, or no warrant, says nothing: refused
  (is (= :no-current-warrant (:kind (oc/check-test-warrant {:repo "futon2" :entry-id futon2-warrant :ns "futon2.aif.trace-test"}))))
  (is (= :no-current-warrant (:kind (oc/check-test-warrant {:repo "futon2" :entry-id "test-registry-nonexistent" :ns "futon2.aif.observation-rates-test"})))))

(deftest c1-lean-warrant
  (let [base {:repo "mathlib4" :entry-id contracts-warrant :module "DarkTower.WarMachine.MachineContracts"
              :path "DarkTower/WarMachine/TokenObservation.lean"}]
    ;; Holes.lean's sorries in the same build do not count against this file
    (is (true? (:observed (oc/check-lean-warrant (assoc base :decl "theorem tokenLikelihood_checkable")))))
    (is (false? (:observed (oc/check-lean-warrant (assoc base :decl "theorem no_such_theorem")))))
    (is (= :no-current-warrant
           (:kind (oc/check-lean-warrant (assoc base :module "DarkTower.WarMachine.Other" :decl "theorem tokenLikelihood_checkable")))))
    ;; a file with sorries in the warrant is observed false
    (is (false? (:observed (oc/check-lean-warrant (assoc base :path "DarkTower/WarMachine/Holes.lean" :decl "theorem")))))))

(deftest c5-locus-resolves
  (let [base {:repo "mathlib4" :sha "52d6516922"
              :bundle-path "DarkTower/WarMachine/machine-contracts-2026-09-17-r15/machine-contracts.json"}
        r (oc/check-registry-entry (assoc base :entry "wm-token-observation"))]
    ;; every declared clojure-locus resolves at its repo's HEAD
    (is (true? (:observed r)) (pr-str (:evidence r)))
    (is (seq (get-in r [:evidence :clojure-loci])))))
