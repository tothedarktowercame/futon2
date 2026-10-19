(ns futon2.aif.meta-terminal-injury-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-terminal-injury :as terminal]))

(def campaign
  (edn/read-string (slurp "test/fixtures/meta-self-heal/campaign-2026-10-19.edn")))
(def pin-a {:path "fixtures/a.edn"
            :sha256 "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
(def patch "95145f09395145f09395145f09395145f0939514")
(def review "42bfb4bb542bfb4bb542bfb4bb542bfb4bb542bf")
(def integration "5a5b421bc5a5b421bc5a5b421bc5a5b421bc5a5b")

(defn classify [record pin] (terminal/classify record pin))
(defn append! [history event]
  (let [result (ledger/append-transition history event)]
    (is (= :appended (:status result)) (pr-str result))
    (:ledger result)))

(deftest campaign-replay-census-is-pinned-and-fail-closed
  (let [classified (mapv #(classify (:record %) (:source-pin %)) campaign)
        result (terminal/replay ledger/empty-ledger classified)
        active (:injuries (ledger/active-injuries (:ledger result)))
        entries (:entries (:ledger result))]
    (is (= 5 (count classified)))
    (is (= {:injury 2 :non-injury 3}
           (frequencies (map :classification classified))))
    (is (= 2 (count active)))
    (is (= 2 (count (filter #(= :open (:state %)) entries))))
    (is (zero? (count (filter #(= :reopened (:state %)) entries))))
    (is (= {:ordinary-selection-abstention 3}
           (frequencies (keep :non-injury-reason classified))))
    (is (= #{:fold-output-invalid :carry-identity-unavailable}
           (set (map :failure-kind (vals active)))))
    (is (true? (:trace-written (nth classified 3))))
    (is (every? :run-output-present classified))))

(deftest non-injury-enumeration-is-explicit-and-stage-is-not
  (doseq [[kind reason] terminal/explicit-non-injury-kinds]
    (is (= reason
           (:non-injury-reason
            (classify {:run/id (str "run-" (name kind)) :click/id "click-x"
                       :failure {:kind kind :stage :any-stage}}
                      pin-a)))))
  (doseq [[kind stage] [[:author-dispatch-failed :author-dispatch]
                        [:review-failed :independent-review]
                        [:reload-failed :reload]
                        [:algorithm-crashed :never-seen-before]
                        [:untyped-failure nil]]]
    (let [classification
          (classify {:run/id (str "run-" (name kind)) :click/id "click-y"
                     :failure (cond-> {:kind kind} stage (assoc :stage stage))}
                    pin-a)]
      (is (= :injury (:classification classification)))
      (is (= (or stage :unknown-stage) (:failure-stage classification))))))

(deftest abstention-cause-classification-requires-retained-evidence
  ;; Synthetic provider fixture in the P10 retained-cause shape. Recorded L1
  ;; instead stopped at :incomplete-policy-menu-coverage / :no-current-target-
  ;; reading (frontier ruling, 2026-10-19), so it is tested separately below.
  (let [provider-p10-record
        {:run/id "provider-p10" :click/id "provider-click"
         :failure {:kind :abstained :stage :selection
                   :causes {:total-count 1 :retained-count 1 :omitted-count 0
                            :causes [{:reason :outer-policy-evidence-provider-required}]}}}
        scored-no-action
        {:run/id "scored-none" :click/id "same-coarse-outcome"
         :failure {:kind :abstained :stage :selection
                   :detail {:refusals [{:reason :policy-nondiscrimination}]}}
         :decision {:status :abstained}}
        partial-frontier-continued
        {:run/id "partial-continued" :click/id "same-coarse-outcome"
         :failure {:kind :abstained :stage :close}
         :decision {:status :selected
                    :selection-certificate
                    {:reading-frontier {:eligible 3 :assessed 1 :reading-absent 2}}}}
        unknown-absence
        {:run/id "l1-reading-stop" :click/id "same-coarse-outcome"
         :failure {:kind :abstained :stage :selection
                   :detail {:refusals [{:reason :incomplete-policy-menu-coverage}
                                       {:absent :no-current-target-reading}]}}}
        incidental-provider-metadata
        {:run/id "incidental-provider" :click/id "same-coarse-outcome"
         :failure {:kind :abstained :stage :selection
                   :detail {:refusals [{:reason :policy-nondiscrimination}]}}
         :decision {:status :abstained
                    :metadata {:historical-label :outer-policy-evidence-provider-required}}}
        classified (mapv #(classify % pin-a)
                         [provider-p10-record scored-no-action partial-frontier-continued
                          unknown-absence incidental-provider-metadata])]
    (is (= [:injury :non-injury :non-injury :non-injury :non-injury]
           (mapv :classification classified)))
    (is (= :missing-production-machinery (:observation-class (first classified))))
    (is (= [:evaluated-no-action :partial-frontier-continuation
            :ordinary-selection-abstention :evaluated-no-action]
           (mapv :non-injury-reason (subvec classified 1))))
    (is (= [:evaluated-no-action :partial-frontier-continuation :ordinary-abstention
            :evaluated-no-action]
           (mapv :observation-class (subvec classified 1))))))

(deftest non-abstention-non-injuries-retain-their-own-observation-class
  (doseq [[record expected]
          [[{:run/id "success" :click/id "success-click" :outcome :success}
            :successful-task-outcome]
           [{:run/id "cancelled" :click/id "cancel-click"
             :failure {:kind :operator-cancelled :stage :operator}}
            :operator-cancelled]
           [{:run/id "diagnostic" :click/id "diagnostic-click"
             :failure {:kind :diagnostic-only :stage :diagnostic}}
            :diagnostic-only]]]
    (let [classification (classify record pin-a)]
      (is (= :non-injury (:classification classification)))
      (is (= expected (:observation-class classification))))))

(deftest trace-output-absence-and-unrelated-success-cannot-erase-open-injury
  (let [injury (classify {:run/id "failed" :click/id "failed-click"
                          :failure {:kind :apparatus-phase-failure
                                    :stage :unknown-stage}
                          :traceWritten true :run-output {:present true}}
                         pin-a)
        opened (:ledger (terminal/integrate ledger/empty-ledger injury))
        before (set (keys (:injuries (ledger/active-injuries opened))))
        records [[{:run/id "absent" :click/id "absent-click"} :absent-terminal-failure]
                 [{:run/id "success" :click/id "unrelated-click"
                   :outcome :success :traceWritten true
                   :run-output {:present true}} :successful-task-outcome]
                 [{:run/id "abstain" :click/id "abstain-click"
                   :failure {:kind :abstained :stage :selection}}
                  :ordinary-selection-abstention]]
        after (reduce (fn [history [record expected-reason]]
                        (let [c (classify record
                                          {:path (str "fixtures/" (:run/id record) ".edn")
                                           :sha256 (format "%064x"
                                                           (inc (count (:entries history))))})]
                          (is (= expected-reason (:non-injury-reason c)))
                          (:ledger (terminal/integrate history c))))
                      opened records)]
    (is (= before (set (keys (:injuries (ledger/active-injuries after))))))))

(deftest same-cause-reopens-attributable-retry-and-distinct-causes-stay-distinct
  (let [first-record {:run/id "failure-1" :click/id "detect-1"
                      :failure {:kind :reload-failed :stage :reload
                                :detail {:target "A-self-heal"}}}
        first-c (classify first-record pin-a)
        injury-id (:injury-id first-c)
        open (:ledger (terminal/integrate ledger/empty-ledger first-c))
        claim (append! open {:injury-id injury-id :state :claimed :attempt 1
                             :repair-id "repair/stable" :algorithm "self-heal"
                             :author-session "author/1" :reviewer-session "reviewer/1"})
        authored (append! claim {:injury-id injury-id :state :authored :attempt 1
                                 :repair-id "repair/stable" :patch-sha patch
                                 :worktree "/tmp/fake-repair"})
        reviewed (append! authored {:injury-id injury-id :state :reviewed :attempt 1
                                    :repair-id "repair/stable" :verdict :approved
                                    :reviewer-sha review :reviewer-session "reviewer/1"})
        enacted (append! reviewed {:injury-id injury-id :state :enacted :attempt 1
                                   :repair-id "repair/stable"
                                   :integration-commit integration
                                   :retry-click "retry/1"})
        retry-c (classify (assoc first-record :run/id "failure-2"
                                              :click/id "retry/1")
                          {:path "fixtures/retry.edn"
                           :sha256 "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"})
        reopened (terminal/integrate enacted retry-c)
        distinct (classify {:run/id "failure-3" :click/id "detect-3"
                            :failure {:kind :review-failed :stage :review}}
                           {:path "fixtures/distinct.edn"
                            :sha256 "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"})
        with-distinct (terminal/integrate (:ledger reopened) distinct)]
    (is (= injury-id (:injury-id retry-c)))
    (is (= :reopened (:transition reopened)))
    (is (= 2 (get-in reopened [:entry :attempt])))
    (is (= 2 (count (:injuries (ledger/active-injuries (:ledger with-distinct))))))))

(deftest duplicate-ingestion-and-classification-tamper-are-deterministic
  (let [classification (classify {:run/id "failure" :click/id "click"
                                  :failure {:kind :algorithm-crashed :stage :unknown}}
                                 pin-a)
        first (terminal/integrate ledger/empty-ledger classification)
        second (terminal/integrate (:ledger first) classification)
        forged (assoc classification :failure-kind :abstained)]
    (is (= :duplicate (:status second)))
    (is (= (:ledger first) (:ledger second)))
    (is (= :refused (:status (terminal/integrate (:ledger first) forged))))))

(deftest successful-attributable-retry-discharges-but-unrelated-success-does-not
  (let [failure (classify {:run/id "failed" :click/id "detected"
                           :failure {:kind :reload-failed :stage :reload}}
                          pin-a)
        injury-id (:injury-id failure)
        open (:ledger (terminal/integrate ledger/empty-ledger failure))
        claim (append! open {:injury-id injury-id :state :claimed :attempt 1
                             :repair-id "repair/async" :algorithm "self-heal"
                             :author-session "a" :reviewer-session "r"})
        authored (append! claim {:injury-id injury-id :state :authored :attempt 1
                                 :repair-id "repair/async" :patch-sha patch
                                 :worktree "/tmp/w"})
        reviewed (append! authored {:injury-id injury-id :state :reviewed :attempt 1
                                    :repair-id "repair/async" :verdict :approved
                                    :reviewer-sha review :reviewer-session "r"})
        enacted (append! reviewed {:injury-id injury-id :state :enacted :attempt 1
                                   :repair-id "repair/async"
                                   :integration-commit integration
                                   :retry-click "retry-click"})
        unrelated (classify {:run/id "other" :click/id "other-click"
                             :outcome :success}
                            {:path "fixtures/other.edn"
                             :sha256 (apply str (repeat 64 "d"))})
        after-unrelated (:ledger (terminal/integrate enacted unrelated))
        retry (classify {:run/id "retry" :click/id "retry-click"
                         :outcome :success}
                        {:path "fixtures/retry-success.edn"
                         :sha256 (apply str (repeat 64 "e"))})
        discharged (terminal/integrate after-unrelated retry)]
    (is (= #{injury-id}
           (set (keys (:injuries (ledger/active-injuries after-unrelated))))))
    (is (= :discharged (:transition discharged)))
    (is (empty? (:injuries (ledger/active-injuries (:ledger discharged)))))))
