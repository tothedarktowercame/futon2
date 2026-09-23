(ns futon2.aif.full-loop-runner-1-4-wiring-test
  "Close adapter and ledger integration, with retained and constructed cases."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.accepted-increment :as ai]
            [futon2.aif.accepted-increment-test :as fixtures]
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

(deftest constructed-missing-commit-records-false-and-writes-nothing
  ;; Constructed no-commit case; actual retained inputs are replayed below.
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

(deftest constructed-stale-binding-records-false-and-writes-nothing
  ;; Constructed non-corroborating binding; actual retained inputs are below.
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
        _ (spit (io/file tmp "attempts.edn") "")
        ;; The occurrence this attempt ran, as production identifies it.
        occurrence {:run/id "runner-14" :attempt/id "attempt-001"}
        effect ["T-runner-14" :apparatus/authority-stated]
        ;; claude-5, 2026-09-23: the banked row used to be hand-spat as a
        ;; string, and 6cf508b6 (the theta-key re-keying) left it unreadable
        ;; -- no :trial, so theta-key could not attribute it, and b-update was
        ;; never passed the :occurrence it dedupes on, so r2 could not have
        ;; reported :already-recorded whatever the ledger said. The row is now
        ;; banked through the PRODUCTION writer, record!, so the test cannot
        ;; drift from the shape the runner actually appends.
        bank! (fn []
                (ledger/record!
                 (.getPath tmp)
                 {:contract :v2
                  :trials [{:status :admitted-at-attempt-grain
                            :learning-family :cfg/runner-14-configuration-digest
                            :meaning-sha256 "runner-14-meaning"
                            :after-observation true
                            :effect effect
                            :selected-cascade
                            {:precedence [{:id :apparatus/one-authority-per-question
                                           :produces #{effect}}]}
                            :deduplication {:identity "runner-14-occurrence"
                                            :inputs {:occurrence occurrence}}}]}))
        r1 (ledger/b-update {:family :apparatus/one-authority-per-question
                             :occurrence occurrence
                             :occurrence-identity "runner-14-occurrence"
                             :accepted-verdict (assoc verdict :observed true)
                             :ledger-root (.getPath tmp)})
        ;; the close writes the row through record!, as the runner does
        banked (bank!)
        r2 (ledger/b-update {:family :apparatus/one-authority-per-question
                             :occurrence occurrence
                             :occurrence-identity "runner-14-occurrence"
                             :accepted-verdict (assoc verdict :observed true)
                             :ledger-root (.getPath tmp)})]
    (is (true? (:accepted? verdict)) (pr-str verdict))
    (is (= :updated (:status r1)))
    (is (= 3/4 (:theta r1)) "cold Laplace success")
    (is (= [:appended] (mapv #(get-in % [:ledger :status]) (:trials banked)))
        (str "the row must be APPENDED, not held -- a held row would leave the "
             "ledger empty and r2 would read :updated for the wrong reason: "
             (pr-str (mapv (juxt :status :reason) (:trials banked)))))
    (is (= :already-recorded (:status r2)))
    (is (= (:theta r1) (:theta r2)) "the second run writes nothing")))

(deftest retained-closes-through-runner-adapter-cannot-update-b
  (doseq [label ["r4-1" "r4-2" "machinery-71-attempt-002"]]
    (let [verdict (ai/evaluate-close (fixtures/retained-input label))
          update (try (ledger/b-update {:family :apparatus/done-is-observed-running
                                        :occurrence-identity (str label "-adapter-replay")
                                        :accepted-verdict verdict})
                      (catch Exception e (ex-data e)))]
      (is (= :no-acceptance-declared (:accepted? verdict)) label)
      (is (= :close-not-accepted (:learning-ledger/refusal update)) label))))
