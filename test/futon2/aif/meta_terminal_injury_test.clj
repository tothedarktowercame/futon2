(ns futon2.aif.meta-terminal-injury-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-terminal-injury :as terminal]))

(def campaign
  (edn/read-string (slurp "test/fixtures/meta-self-heal/campaign-2026-10-19.edn")))
(def pin-a {:path "fixtures/a.edn"
            :sha256 "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
(def patch "95145f09395145f09395145f09395145f0939514")
(def review "42bfb4bb542bfb4bb542bfb4bb542bfb4bb542bf")
(def integration "5a5b421bc5a5b421bc5a5b421bc5a5b421bc5a5b")

;; Projected by extract_click1_terminal_projection.clj from retained record
;; sha256 e9a5eb355f9a6f70b41d5bdbd99e127badcd6d13e6bd7165867fa2bb9477f404.
(def click-1-terminal
  {:run/id "2026-10-26-8be8ff03-5009-4f3d-8c12-5b11cfd46d92"
   :click/id "wm-click-2cea5ee6-4470-4ecc-a6b3-96ada0dab4e7"
   :terminal-receipt {:outcome :grounded-progress}
   :failure {:absent :no-failure}
   :traceWritten true
   :run-output {}})

(defn classify [record pin] (terminal/classify record pin))
(defn append! [history event]
  (let [result (ledger/append-transition history event)]
    (is (= :appended (:status result)) (pr-str result))
    (:ledger result)))

(deftest successful-close-images-are-non-injuries
  (testing "click 1's retained grounded progress is a non-injury"
    (is (= [:non-injury :successful-task-outcome]
           ((juxt :classification :non-injury-reason)
            (classify click-1-terminal pin-a)))))
  (testing "the other two successful images are non-injuries"
    (doseq [outcome [:already-satisfied :grounded-no-change]]
      (is (= [:non-injury :successful-task-outcome]
             ((juxt :classification :non-injury-reason)
              (classify (assoc-in click-1-terminal
                                  [:terminal-receipt :outcome] outcome)
                        pin-a))))))
  (testing "an invalid-image close remains an injury"
    ;; CloseOutcomeImage.lean does not decide this contradictory no-failure
    ;; record; this packet deliberately preserves the classifier's behavior.
    (is (= :injury
           (:classification
            (classify (assoc-in click-1-terminal
                                [:terminal-receipt :outcome] :build-failed)
                      pin-a))))))

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

(defn- enacted-ledger
  "A ledger with one injury carried to :enacted."
  ([] (enacted-ledger "retry-click"))
  ([retry-click]
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
                                   :retry-click retry-click})]
    {:injury-id injury-id :ledger enacted})))

(defn- pin-of [c] {:path (str "fixtures/" c ".edn") :sha256 (apply str (repeat 64 c))})

;; The seven no-work outcomes, each record built so that classify yields the
;; reason named for it (rev 8 RetryClickOutcome, the no-work constructors).
(def no-work-retry-records
  [[:ordinary-selection-abstention
    {:failure {:kind :abstained :stage :selection}}]
   [:evaluated-no-action
    {:failure {:kind :abstained :stage :selection
               :detail {:refusals [{:reason :policy-nondiscrimination}]}}
     :decision {:status :abstained}}]
   [:partial-frontier-continuation
    {:failure {:kind :abstained :stage :close}
     :decision {:status :selected
                :selection-certificate
                {:reading-frontier {:eligible 3 :assessed 1 :reading-absent 2}}}}]
   [:operator-cancelled {:failure {:kind :operator-cancelled :stage :selection}}]
   [:declared-no-work {:failure {:kind :declared-no-work :stage :selection}}]
   [:diagnostic-only {:failure {:kind :diagnostic-only :stage :selection}}]
   [:absent-terminal-failure {}]])

(deftest abstained-retry-click-keeps-the-injury-enacted
  ;; The bad case: a retry click that abstained (the ordinary outcome since
  ;; 10-15) must not record the repair as healed.
  (let [{:keys [injury-id ledger]} (enacted-ledger)
        retry (classify {:run/id "retry-abstained" :click/id "retry-click"
                         :failure {:kind :abstained :stage :selection}}
                        (pin-of "1"))
        result (terminal/integrate ledger retry)
        active (:injuries (ledger/active-injuries (:ledger result)))
        last-entry (last (:entries (:ledger result)))]
    (is (= :ordinary-selection-abstention (:non-injury-reason retry)))
    (is (= :retry-inconclusive (:transition result)) (pr-str (:transition result)))
    (is (= ledger/observation-schema (:schema last-entry)))
    (is (= :retry-inconclusive (:observation-kind last-entry)))
    (is (= injury-id (:injury-id last-entry)))
    (is (= "retry-click" (:retry-click last-entry)))
    (is (= :enacted (:state (get active injury-id))))
    (is (= {:injury-id injury-id :repair-id "repair/async" :retry-click "retry-click"}
           (:retry-of result)))))

(deftest every-no-work-retry-outcome-is-inconclusive
  (doseq [[i [reason record]] (map-indexed vector no-work-retry-records)]
    (let [{:keys [injury-id ledger]} (enacted-ledger)
          retry (classify (assoc record :run/id (str "retry-" (name reason))
                                 :click/id "retry-click")
                          (pin-of (str (+ 2 i))))
          result (terminal/integrate ledger retry)
          active (:injuries (ledger/active-injuries (:ledger result)))
          observation (last (:entries (:ledger result)))]
      (is (= [:non-injury reason]
             [(:classification retry) (:non-injury-reason retry)]) (pr-str reason))
      (is (= :retry-inconclusive (:transition result)) (pr-str reason))
      (is (= :retry-inconclusive (:observation-kind observation))
          (pr-str reason))
      (is (= injury-id (:injury-id observation)) (pr-str reason))
      (is (= "retry-click" (:retry-click observation)) (pr-str reason))
      (is (= :enacted (:state (get active injury-id))) (pr-str reason))
      (is (= :enacted (:state (last (filter #(= ledger/transition-schema (:schema %))
                                            (:entries (:ledger result))))))
          (pr-str reason)))))

(deftest different-injury-on-retry-click-is-inconclusive-for-enacted-injury
  ;; Pins requeue_todayMissesUnobserved: both new and already-active other
  ;; injuries leave the attributed observation needed by pending-requeue,
  ;; while the same injury follows append-retry-result instead.
  (let [{injury-a :injury-id history-a :ledger}
        (enacted-ledger "wm-retry-r-1")
        unknown-b (classify {:run/id "other-new" :click/id "wm-retry-r-1"
                             :failure {:kind :review-failed :stage :review}}
                            (pin-of "b"))
        opened-b (terminal/integrate history-a unknown-b)
        pending-open (ledger/pending-requeue (:ledger opened-b) injury-a)

        seed-b (classify {:run/id "other-seed" :click/id "other-detection"
                          :failure {:kind :review-failed :stage :review}}
                         (pin-of "c"))
        with-active-b (:ledger (terminal/integrate history-a seed-b))
        repeated-b (classify {:run/id "other-repeat" :click/id "wm-retry-r-1"
                              :failure {:kind :review-failed :stage :review}}
                             (pin-of "d"))
        retained-b (terminal/integrate with-active-b repeated-b)
        pending-repeat (ledger/pending-requeue (:ledger retained-b) injury-a)

        same-a (classify {:run/id "same-injury" :click/id "wm-retry-r-1"
                          :failure {:kind :reload-failed :stage :reload}}
                         (pin-of "e"))
        reopened-a (terminal/integrate history-a same-a)]
    (is (not= injury-a (:injury-id unknown-b)))
    (is (= :open (:transition opened-b)))
    (is (= :open (->> (:entries (:ledger opened-b))
                      (filter #(= (:injury-id unknown-b) (:injury-id %)))
                      last :state)))
    (is (= :retry-inconclusive (:observation-kind pending-open)))
    (is (= injury-a (:injury-id pending-open)))
    (is (= "wm-retry-r-1" (:retry-click pending-open)))
    (is (= (:record-pin unknown-b) (:record-pin pending-open)))
    (is (= {:injury-id injury-a :repair-id "repair/async"
            :retry-click "wm-retry-r-1"}
           (:retry-of opened-b)))

    (is (= :active-injury-retained (:transition retained-b)))
    (is (= :repeated-active-injury
           (->> (:entries (:ledger retained-b))
                (filter #(= :repeated-active-injury (:observation-kind %)))
                last :observation-kind)))
    (is (= :retry-inconclusive (:observation-kind pending-repeat)))
    (is (= injury-a (:injury-id pending-repeat)))
    (is (= "wm-retry-r-1" (:retry-click pending-repeat)))
    (is (= (:record-pin repeated-b) (:record-pin pending-repeat)))
    (is (= {:injury-id injury-a :repair-id "repair/async"
            :retry-click "wm-retry-r-1"}
           (:retry-of retained-b)))

    (is (= injury-a (:injury-id same-a)))
    (is (= :reopened (:transition reopened-a)))
    (is (nil? (ledger/pending-requeue (:ledger reopened-a) injury-a)))
    (is (empty? (filter #(= :retry-inconclusive (:observation-kind %))
                        (:entries (:ledger reopened-a)))))))

(deftest no-work-record-off-the-retry-click-records-its-reason
  (let [{:keys [injury-id ledger]} (enacted-ledger)
        other (classify {:run/id "other-abstained" :click/id "other-click"
                         :failure {:kind :abstained :stage :selection}}
                        (pin-of "9"))
        result (terminal/integrate ledger other)
        last-entry (last (:entries (:ledger result)))]
    (is (= :observation (:transition result)))
    (is (nil? (:retry-of result)))
    (is (= :ordinary-selection-abstention (:observation-kind last-entry)))
    (is (= :enacted (:state (get (:injuries (ledger/active-injuries (:ledger result)))
                                 injury-id))))))
