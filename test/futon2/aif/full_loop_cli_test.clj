(ns futon2.aif.full-loop-cli-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-cli :as cli]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.morning-brief :as brief]))

(deftest continuous-count-is-parsed-and-bounds-opportunities
  (let [calls (atom [])]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (swap! calls conj opts)
                    {:outcome :grounded-change})]
      (with-out-str
        (#'cli/continuous! {:count "3"
                            :interval-seconds "0"
                            :batch-id "overnight-1"
                            :author "codex-6"
                            :reviewer "claude-7"})))
    (is (= 3 (count @calls)))
    (is (every? #(= :duree-click-continuous (:trigger %)) @calls))
    (is (every? #(= "codex-6" (:author %)) @calls))
    (is (every? #(= "claude-7" (:reviewer %)) @calls))
    (is (every? #(= "overnight-1" (:batch-id %)) @calls))))

(deftest tripwire-action-is-an-explicit-validated-operator-flag
  (is (= :record (:tripwire/action
                  (#'cli/runner-opts :wallclock-cron
                                      {:tripwire-action "record"}))))
  (is (= :park-and-summon
         (:tripwire/action
          (#'cli/runner-opts :wallclock-cron
                             {:tripwire-action "park-and-summon"}))))
  (is (nil? (:tripwire/action (#'cli/runner-opts :wallclock-cron {})))
      "absence preserves the tripwire namespace's compiled :record default")
  (is (thrown? clojure.lang.ExceptionInfo
               (#'cli/runner-opts :wallclock-cron
                                  {:tripwire-action "promote-now"}))))

(deftest repair-reviewer-is-an-explicit-runner-role
  (is (= "codex-1" runner/default-repair-reviewer))
  (is (= "codex-1"
         (:repair-reviewer
          (#'cli/runner-opts :duree-click-on-demand
                             {:repair-reviewer "codex-1"}))))
  (is (nil? (:repair-reviewer
             (#'cli/runner-opts :duree-click-on-demand {})))
      "the runner config owns the visible default"))

(deftest continuous-stops-after-first-non-grounded-opportunity
  (let [calls (atom 0)
        outcomes [:grounded-change :agent-unavailable :grounded-change]
        failure (with-redefs [runner/run-opportunity!
                              (fn [_opts]
                                {:outcome (nth outcomes (dec (swap! calls inc)))})]
                  (try
                    (with-out-str
                      (#'cli/continuous! {:count "3"
                                          :interval-seconds "0"
                                          :author "codex-6"
                                          :reviewer "claude-7"}))
                    nil
                    (catch clojure.lang.ExceptionInfo e e)))]
    (is (= 2 @calls) "the third opportunity must never be emitted")
    (is (= :continuous-stopped (:outcome (ex-data failure))))
    (is (= 2 (:completed-clicks (ex-data failure))))
    (is (= :agent-unavailable
           (get-in (ex-data failure) [:last-result :outcome])))))

(deftest continuous-stops-after-consecutive-repeated-target
  (let [calls (atom 0)
        targets ["M-one" "M-one" "M-two"]
        failure (with-redefs [runner/run-opportunity!
                              (fn [_opts]
                                {:outcome :grounded-change
                                 :checkpoints
                                 {:selection
                                  {:judgment
                                   {:selected-mission
                                    (nth targets (dec (swap! calls inc)))}}}})]
                  (try
                    (with-out-str
                      (#'cli/continuous! {:count "3" :interval-seconds "0"}))
                    nil
                    (catch clojure.lang.ExceptionInfo e e)))]
    (is (= 2 @calls) "the opportunity after a repeated target must not be emitted")
    (is (= :repeated-selection (:reason (ex-data failure))))
    (is (= "M-one" (:repeated-target (ex-data failure))))))

(deftest batch-brief-is-one-ordered-surface
  (with-redefs [brief/items
                (fn [] [{:attempt-id "attempt-7" :batch-id "night"
                         :queued-at "2026-07-16T02:00:00Z"}
                        {:attempt-id "attempt-6" :batch-id "night"
                         :queued-at "2026-07-16T01:00:00Z"}
                        {:attempt-id "old" :batch-id "other"
                         :queued-at "2026-07-15T01:00:00Z"}])
                brief/reviews (constantly [])]
    (let [report (#'cli/batch-brief "night")]
      (is (= ["attempt-6" "attempt-7"] (:judgment-order report)))
      (is (= 4 (:pending-count report)))
      (is (= "night" (:batch-id report))))))
