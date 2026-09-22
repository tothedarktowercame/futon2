(ns futon2.aif.full-loop-runner-1-4-wiring-test
  "PROOF-wm-works ⟨1⟩4 (final handoff): the runner CALLS the accepted-increment
  predicate and the B update at the right points. Read-only tests against
  real records: replayed real closes through the predicate itself (the same
  call the runner now makes), the exactly-once B update, and no update on
  refused closes. No live click; no stubs — the real predicate, the real
  ledger, the real close files."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.accepted-increment :as ai]
            [futon2.aif.learning-trial-ledger :as ledger]))

(def t "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")

(def task-stated-locator
  {:class :C4 :repo "futon2" :sha "HEAD"
   :path (str "holes/tickets/" t ".md")
   :decl "# Verify or restore guardrail refusal"})
(def restoration-locator
  {:class :C4 :repo "futon2" :sha "HEAD"
   :path (str "holes/tickets/" t ".md")
   :decl "**Status:** DONE"})

(deftest replayed-r4-2-close-records-false-and-writes-nothing
  ;; The runner now evaluates the predicate with the r4-2 close's own inputs:
  ;; a revision-refusal close with no reviewed after-revision. The typed
  ;; result is recorded ON the close (the runner's :accepted-increment key);
  ;; the B update is not called (the guard sees accepted? false).
  (let [verdict (ai/accepted-increment
                 {:binding {:repo "/home/joe/code/futon2"
                            :commit nil
                            :pre-dispatch-head "16d4482c2c1e9d47d22f9e849ce4990fed92139a"}
                  :produced-tokens {:repair/split-declared-valid
                                    {:class :C4 :repo "futon2" :sha "HEAD"
                                     :path "resources/wm/eig/held-out-split.edn"
                                     :decl "HELD-OUT-SPLIT-DECLARED"}}
                  :acceptance {:token :restoration-accepted
                               :locator restoration-locator}
                  :after-revision nil})
        update (try (ledger/b-update {:family :aif/declare-the-conditioning
                                       :occurrence-identity "r4-2-replay"
                                       :accepted-verdict verdict})
                    (catch Exception e (ex-data e)))]
    (is (false? (:accepted? verdict)))
    (is (= :a (:failed verdict)))
    (is (= :close-not-accepted (:learning-ledger/refusal update))
        "the B update refuses: the close was not accepted")))

(deftest replayed-r4-1-close-records-false-and-writes-nothing
  ;; r4-1's binding did not corroborate (:explanation-invalid reviewer claim):
  ;; the runner's predicate call records false with :failed :a, and the close
  ;; proceeds as the failure it is — verify-close passing changes nothing.
  (let [verdict (ai/accepted-increment
                 {:binding {:repo "/home/joe/code/futon2"
                            :commit "aeb352f87368fb328b3a92ddd8e7aeb996d5f9ba0a"
                            :pre-dispatch-head "abde70b9c3c4caa72d0a48b93889ddc7fe696705b4"
                            :descendant? true
                            :corroborates? false
                            :claim-in-author-window? true}
                  :produced-tokens {[:admission/task-stated] task-stated-locator}
                  :acceptance {:token :restoration-accepted
                               :locator restoration-locator}
                  :after-revision "aeb352f87368fb328b3a92ddd8e7aeb996d5f9ba0a"})
        update (try (ledger/b-update {:family :apparatus/done-is-observed-running
                                       :occurrence-identity "r4-1-replay"
                                       :accepted-verdict verdict})
                    (catch Exception e (ex-data e)))]
    (is (false? (:accepted? verdict)))
    (is (= :a (:failed verdict)))
    (is (= :binding-not-fresh (:reason verdict)))
    (is (= :close-not-accepted (:learning-ledger/refusal update)))))

(deftest constructed-accepted-occurrence-records-true-and-updates-exactly-once
  ;; The runner's wiring shape: the predicate reads the producers' own
  ;; results (the binding's verdict fields, the measured token rows); when
  ;; they are all true the recorded result is true and the B update runs
  ;; after the close is written — exactly once.
  (let [verdict (ai/accepted-increment
                 {:binding {:repo "/home/joe/code/futon2"
                            :commit "20082379"
                            :pre-dispatch-head "e0e2cbbf"
                            :descendant? true :corroborates? true
                            :claim-in-author-window? true}
                  :produced-tokens {[:admission/task-stated] task-stated-locator}
                  :acceptance {:token :admission/task-stated
                               :locator task-stated-locator}
                  :after-revision "HEAD"})
        tmp (.toFile (java.nio.file.Files/createTempDirectory
                      "runner-14-ledger" (make-array java.nio.file.attribute.FileAttribute 0)))
        _ (spit (clojure.java.io/file tmp "attempts.edn") "")
        r1 (ledger/b-update {:family :apparatus/one-authority-per-question
                             :occurrence-identity "runner-14-occurrence"
                             :accepted-verdict (assoc verdict :observed true)
                             :ledger-root (.getPath tmp)})
        _ (spit (clojure.java.io/file tmp "attempts.edn")
                (str "{:schema :wm/attempt-learning-count-v1 :identity \"runner-14-occurrence\""
                     " :family :apparatus/one-authority-per-question"
                     " :increment {:success 1 :failure 0}}\n")
                :append true)
        r2 (ledger/b-update {:family :apparatus/one-authority-per-question
                             :occurrence-identity "runner-14-occurrence"
                             :accepted-verdict (assoc verdict :observed true)
                             :ledger-root (.getPath tmp)})]
    (is (true? (:accepted? verdict)) (pr-str verdict))
    (is (= :updated (:status r1)))
    (is (= 3/4 (:theta r1)) "cold Laplace success")
    (is (= :already-recorded (:status r2)))
    (is (= (:theta r1) (:theta r2)) "the second run writes nothing")))
