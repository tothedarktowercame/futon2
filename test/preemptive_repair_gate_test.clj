(ns preemptive-repair-gate-test
  (:require [checks.preemptive-repair-suite :as gate]
            [clojure.test :refer [deftest is]]))

(deftest build-gate-consumes-preemptive-repair-lints
  (let [positive (gate/gate-result)
        negative (gate/gate-result true)]
    (is (:pass? positive))
    (is (nat-int? (:absence-count positive))
        "known C81 absence debt is emitted but does not mask extinct classes")
    (is (false? (:pass? negative))
        "an injected lint finding must fail the build-level consumer")))

(deftest one-gate-verdict-captures-the-live-corpus-once
  ;; Before C289, gate-result called corpus once per lint kind. A writer could
  ;; therefore expose an intermediate worktree state to just one scanner. This
  ;; control returns a different (bad) population on every call after the first:
  ;; the gate must consume only the first coherent snapshot.
  (let [calls (atom 0)
        first-view []
        changing-view [{:repo :negative :root "/tmp" :path "test/concurrent-edit.clj"
                        :text "a later worktree population"}]
        result (with-redefs [checks.preemptive-repair-lint/corpus
                             (fn [] (if (= 1 (swap! calls inc))
                                      first-view changing-view))]
                 (gate/gate-result))]
    (is (= 1 @calls) "one verdict must not combine multiple live-worktree snapshots")
    (is (:pass? result) "later concurrent state must not leak into the captured verdict")))
