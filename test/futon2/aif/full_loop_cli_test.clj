(ns futon2.aif.full-loop-cli-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [clojure.string :as str]
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

(def qa-item
  {:attempt-id "attempt-qa"
   :selected-target "M-example"
   :outcome :grounded-change
   :author "author-1"
   :reviewer "reviewer-1"
   :commit "abc123"
   :queued-at "2026-07-16T12:00:00Z"
   :selection-review
   {:selected-action {:type :advance-mission
                      :target "M-example"
                      :mission-path "/code/example/mission.md"
                      :rationale "advance the open behavior"}
    :ranked-candidates
    [{:rank 1 :action {:target "M-other"} :G-efe 1.0}
     {:rank 2
      :action {:type :advance-mission
               :target "M-example"
               :mission-path "/code/example/mission.md"
               :rationale "advance the open behavior"}
      :G-efe 2.0}]
    :selection-reasons {:rank 2}}
   :achievement
   {:tier :fully-grounded
    :summary "Independently reviewed and grounded change"
    :build {:artifacts ["src/example.clj" "test/example_test.clj"]
            :validation {:review-job "review-job-1"
                         :approved? true
                         :artifact-binding {:fresh-author? true
                                            :descendant? true}}}
    :adjudication
    {:after {:implementation-entity
             {:props #:implementation{:repository "/code/example"}}}}}
   :witness {:resolved? true
             :dial-moved? true
             :implementation-id "implementation/abc123"}})

(deftest attempt-brief-is-a-readable-inspection-and-submission-surface
  (with-redefs [brief/items (fn [] [qa-item])
                brief/reviews (constantly [])]
    (let [rendered (-> "attempt-qa" cli/attempt-brief
                       cli/render-attempt-brief)]
      (is (str/includes? rendered "MORNING BRIEF QA — attempt-qa"))
      (is (str/includes? rendered "selected rank 2 of 2"))
      (is (str/includes? rendered
                         "git -C /code/example show --stat --oneline abc123"))
      (is (str/includes? rendered "Look for: an observable behavior"))
      (is (str/includes? rendered
                         "clojure -M:wm-full-loop review attempt-qa joe")))))

(deftest feature-acceptance-renders-honest-build-time-gaps-without-card
  (with-redefs [brief/items (fn [] [qa-item])
                brief/reviews (constantly [])]
    (let [sheet (-> "attempt-qa" cli/attempt-brief cli/feature-acceptance)
          rendered (cli/render-feature-acceptance sheet)]
      (is (= :not-rendered (get-in sheet [:sorry :status])))
      (is (= :not-rendered (get-in sheet [:wiring :status])))
      (is (= :pending (get-in sheet [:feature :status])))
      (is (= :renderer-generated-evidence
             (get-in sheet [:things-to-try :source])))
      (is (str/includes? rendered
                         "not rendered for this attempt ⟵ build-time gap"))
      (is (str/includes? rendered "build-time feature card pending"))
      (is (str/includes? rendered
                         "git -C /code/example show --stat abc123"))
      (doseq [section ["1. HEADER" "2. SELECTED MISSION" "3. THE CASCADE"
                       "4. THE SORRY / PROOF-HOLE"
                       "5. WIRING DIAGRAM (FOLD BOXES/WIRES)"
                       "6. LOGIC PROOF"
                       "7. THE FEATURE — DOES IT MATCH INTENT?"
                       "8. THINGS TO TRY" "9. VERDICT"]]
        (is (str/includes? rendered section))))))

(deftest feature-acceptance-renders-card-authored-sections-and-fold
  (let [fold-file (java.io.File/createTempFile "feature-fold-" ".executed.edn")
        _ (spit fold-file
                (pr-str {:boxes [{:id :sense} {:id :act}]
                         :want-coverage [:observe :learn :reuse]}))
        card {:built "A deterministic capability-graph ingest command"
              :want-coverage "missions become reusable capability evidence"
              :matches-intent? true
              :things-to-try ["clojure -M -m ingest --help"
                              "clojure -M:test ingest-test"]
              :fold-ref (.getPath fold-file)
              :proof-ref "logic/witness-22.edn"}
        item (assoc qa-item :feature-card card)]
    (try
      (with-redefs [brief/items (fn [] [item])
                    brief/reviews (constantly [])]
        (let [attempt-view (cli/attempt-brief "attempt-qa")
              sheet (cli/feature-acceptance attempt-view)
              rendered (cli/render-feature-acceptance sheet)
              edn-output (with-redefs [cli/attempt-brief (constantly attempt-view)]
                           (with-out-str
                             (#'cli/run-command!
                              ["feature" "attempt-qa" "--format" "edn"])))
              round-tripped (edn/read-string edn-output)]
          (is (= :rendered-from-feature-card
                 (get-in sheet [:sorry :status])))
          (is (= :discovered (get-in sheet [:wiring :status])))
          (is (= 2 (get-in sheet [:wiring :box-count])))
          (is (= 3 (get-in sheet [:wiring :want-coverage-magnitude])))
          (is (= :linked (get-in sheet [:logic-proof :behavioral :status])))
          (is (= (:built card) (get-in sheet [:feature :built])))
          (is (= (:things-to-try card) (get-in sheet [:things-to-try :steps])))
          (is (str/includes? rendered (:built card)))
          (is (str/includes? rendered "logic/witness-22.edn"))
          (is (= sheet round-tripped))))
      (finally
        (.delete fold-file)))))

(deftest completed-click-prints-its-operator-qa-document
  (with-redefs [runner/run-opportunity!
                (fn [_]
                  {:attempt-id "attempt-qa"
                   :outcome :grounded-change
                   :morning-brief-ref "/brief/attempt-qa.edn"})
                cli/attempt-brief (fn [attempt-id]
                                    {:attempt {:attempt-id attempt-id}})
                cli/render-attempt-brief
                (fn [_] "MORNING BRIEF QA — attempt-qa")]
    (let [output (with-out-str (#'cli/run-once! :duree-click-on-demand {}))]
      (is (str/includes? output "MORNING BRIEF QA — attempt-qa")))))

(deftest interactive-review-validates-and-appends-each-answer
  (let [recorded (atom [])
        item (dissoc qa-item :commit :achievement)]
    (with-redefs [brief/items (fn [] [item])
                  brief/reviews (constantly [])
                  brief/review! (fn [& args]
                                  (swap! recorded conj args))]
      (with-in-str "yes\nselection evidence\nyes\nachievement evidence\n"
        (with-out-str
          (cli/review-interactively! "attempt-qa" "joe"))))
    (is (= [["attempt-qa" :selection-quality :yes
             "selection evidence" "joe"]
            ["attempt-qa" :substantive-achievement :yes
             "achievement evidence" "joe"]]
           @recorded))))
