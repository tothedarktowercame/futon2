(ns futon2.aif.wm.enacted.e-comprehension-foundation-test
  "Tests for the mechanical enactment of the E-comprehension-foundation
  reviewed completion checks.  The bad-case test constructs the exact
  failure each check exists to catch (a mutated excerpt) and shows the
  check catches it, against the real pinned excerpt rather than a stub."
  (:require [clojure.string]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.wm.enacted.e-comprehension-foundation :as enacted]))

(deftest pinned-excerpt-digest
  (testing "the pinned excerpt still hashes to the recorded excerpt sha256"
    (is (true? (enacted/excerpt-digest-matches?)))))

(deftest all-reviewed-checks-satisfied-on-pinned-excerpt
  (testing "every reviewed completion check locates its quoted evidence in the pinned excerpt"
    (let [{:keys [results all-satisfied?]} (enacted/verify)]
      (is (true? all-satisfied?))
      (is (= 9 (count results)))
      (doseq [{:keys [satisfied observed expected]} results]
        (is (true? satisfied))
        (is (= expected observed))))))

(deftest nine-holes-covered-exactly
  (testing "the checks cover the nine holes of the compiled cascade exactly"
    (is (= #{["agent/scope-before-action" 0]
             ["stack-coherence/evidence-ledger" 0]
             ["agent/state-is-hypothesis" 1]
             ["agent/commitment-varies-with-confidence" 2]
             ["agency/self-attribution" 3]
             ["agent/scope-before-action" 4]
             ["agent/evidence-over-assertion" 5]
             ["agent/state-is-hypothesis" 6]
             ["agent/handoff-preserves-context" 8]}
           (set (map :hole-id enacted/checks))))))

(deftest mutated-byline-is-caught
  (testing "the exact bad case: replacing the byline makes only the self-attribution check fail, with the mutation visible in :observed"
    (let [mutated (clojure.string/replace enacted/excerpt
                                           "*claude-1 + Joe, 2026-06-22."
                                           "*claude-2 + Joe, 2026-06-22.")
          {:keys [results all-satisfied?]} (enacted/verify mutated)]
      (is (false? all-satisfied?))
      (let [failed (filter (complement :satisfied) results)]
        (is (= 1 (count failed)))
        (is (= ["agency/self-attribution" 3] (:hole-id (first failed))))
        (is (= "*claude-2 + Joe, 2026-06-22." (:observed (first failed))))))))

(deftest missing-open-verdict-is-caught
  (testing "erasing the OPEN verdict makes the status and evidence-ledger checks fail rather than pass vacuously"
    (let [mutated (clojure.string/replace enacted/excerpt "OPEN — " "CLOSED — ")
          {:keys [all-satisfied?]} (enacted/verify mutated)]
      (is (false? all-satisfied?)))))
