(ns futon2.aif.already-satisfied-review-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.edn :as edn]
            [cheshire.core :as json]
            [clojure.string :as str]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.mission-registry :as missions]
            [futon2.aif.stop-line :as stop]
            [futon2.aif.author-already-satisfied-test :as prior]
            [futon2.aif.wm.verified-answer-delivery-test :as frozen]
            [futon2.aif.full-loop-runtime :as runtime]
            [clojure.java.io :as io]
            [futon2.aif.meta-terminal-injury :as terminal]
            [futon2.aif.wm.execution-journal :as journal]
            [futon2.aif.tripwire :as tripwire]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.test-support.runner-fixture :as fixture]))
(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(defn exercise [author target verdict]
  (let [mission (first (filter #(= target (:id %)) (:missions (missions/load-missions "/home/joe/code"))))
        classification (runner/author-refusal-classification author
                         (if (= target "M-self-documenting-stack") "/home/joe/code/futon7" "/home/joe/code/futon0"))
        contacts (atom [])
        phases (atom [])
        charge (atom 0)
        result (runner/review-already-satisfied-claim!
                 {:phase-log-fn #(swap! phases conj %)
                  :dispatch-fn (fn [_ seat caller t prompt]
                                 (swap! contacts conj {:seat seat :caller caller :target t :prompt prompt})
                                 {:job-id "retained-claim-review"})
                  :poll-fn (fn [_ id] {:job-id id :state "done" :result verdict
                                       :execution {:executed true :tool-events 1}})}
                 {} {:author "wm-author" :reviewer "wm-reviewer"}
                 target mission {} author classification #(swap! charge inc))]
    {:result result :contacts @contacts :phases @phases :charge @charge}))
(deftest actual-run001-claim-reaches-review-with-completion
  (let [author (:job (json/parse-string (slurp "test/resources/wm/already-satisfied-review/run001-author.json") true))
        {:keys [result contacts charge]} (exercise author "M-futonzero-prelim-practice"
                                          "FULL_LOOP_REVIEW: REJECT REPL startup is not prelim practice")
        prompt (:prompt (first contacts))]
    (is (= 1 charge))
    (is (= "wm-reviewer" (:seat (first contacts))))
    (is (str/includes? prompt "cfb1408629826c6d825e2ddc06aad84ee5ac6108"))
    (is (str/includes? prompt "The mission names the authoritative prelim-practice ledger"))
    (is (str/includes? prompt "One masked operator attempt"))
    (is (= :guardrail-refusal (:outcome result)))
    (is (= :reviewed-claim-refused (:failure-kind result)))
    (is (stop/no-finding-required? result))
    (is (str/includes? (:refusal-reason result) "REPL startup"))
    (is (empty? (#'tripwire/t5 {:grounded-commit (:grounded-commit result)
                              :reviewer-job (:review-job result) :cohort? true})))))
(deftest retained-click14-closes-only-after-review
  (let [author (assoc (dissoc (edn/read-string (slurp "test/resources/wm/already-satisfied-review/click14-author.edn")) :result-summary)
                      :result prior/click-14-reply)
        {:keys [result contacts]} (exercise author "M-self-documenting-stack" "FULL_LOOP_REVIEW: APPROVE")]
    (is (= :already-satisfied (:outcome result)))
    (is (= "891001d08d8b37eaf50e13f985dee30c8d34340a" (get-in result [:grounded-commit :sha])))
    (is (= 1 (count contacts)))
    (is (= "retained-claim-review" (get-in result [:review-job :job-id])))))
(deftest apparatus-refusal-stays-a-failure
  (let [author (:job (json/parse-string (slurp "test/resources/wm/already-satisfied-review/run001-author.json") true))
        {:keys [result]} (exercise author "M-futonzero-prelim-practice"
                           "FULL_LOOP_REVIEW: REJECT bad apparatus\nFULL_LOOP_APPARATUS_DEFECT: completion binding broken")]
    (is (= :build-failed (:outcome result)))
    (is (= :reviewer-apparatus-defect (:failure-kind result)))
    (is (not (stop/no-finding-required? result)))))

(deftest commit-existence-and-missing-review-do-not-clear-c4
  (let [grounded {:repo "futon0" :sha "cfb1408629826c6d825e2ddc06aad84ee5ac6108"}]
    (is (seq (#'tripwire/t5 {:grounded-commit grounded :cohort? true})))
    (is (seq (#'tripwire/t5 {:grounded-commit grounded :cohort? true
                            :reviewer-job {:prompt "Review some other commit"}})))))

(deftest frozen-run001-drives-review-before-ordinary-refusal
  (let [d (:decision (frozen/frozen "selection"))
        decision (assoc d :action (get-in d [:selection-law :per-policy-argmax :action]))
        author (:job (json/parse-string (slurp "test/resources/wm/already-satisfied-review/run001-author.json") true))
        contacts (atom [])
        root (.toFile (java.nio.file.Files/createTempDirectory "reviewed-claim-" (make-array java.nio.file.attribute.FileAttribute 0)))
        handler (runtime/open-cascade-resume-handler
                  {:author "wm-author" :reviewer "wm-reviewer"
                   :open-cascade-author-answer-fn (fn [_] {:state "done" :result (:response (frozen/frozen "author"))})
                   :open-cascade-reviewer-answer-fn (fn [_] {:state "done" :result (:response (frozen/frozen "reviewer"))})})
        original-entry journal/entry
        timestamp-format (.toFormatter (.appendInstant (java.time.format.DateTimeFormatterBuilder.) 9))
        result (binding [runner/*wm-status-reporting?* false]
                 (with-redefs [runner/refuse-on-runner-source-drift! (fn [] nil)
                               ;; Keep clock serialization fixed-width in this tooling witness.
                               ;; Instant/toString may trim fractional zeros, while the existing
                               ;; journal compares strings. Entry sealing and append remain real.
                               journal/entry (fn [e] (original-entry
                                 (update e :recorded-time
                                   #(.format timestamp-format (java.time.Instant/parse %)))))]
                   (runner/run-opportunity!
                     (merge (fixture/isolated-runner-opts)
                       {:run-id "frozen-run001-reviewed-claim"
                        :author "wm-author" :reviewer "wm-reviewer"
                        :mission-fn (fn [target] (first (filter #(= target (:id %)) (:missions (missions/load-missions "/home/joe/code")))))
                        :roster-fn (fn [_] {:wm-author {:status "idle" :invoke-ready? true}
                                            :wm-reviewer {:status "idle" :invoke-ready? true}
                                            :codex-1 {:status "idle" :invoke-ready? true}})
                        :judge-fn (fn [_] {:judgement {:decision decision :belief {} :belief-pre {}
                                                       :observation {} :free-energy {} :prediction-errors {}
                                                       :precision-state {} :micro-step-trace [] :mode :maintain}})
                        :open-cascade-resume-fn handler
                        :executable-cascade-root (str (io/file root "cascades"))
                        :execution-journal-root (str (io/file root "journal"))
                        :pattern-library-root (str (io/file root "library"))
                        :dispatch-fn (fn [_ seat _ _ prompt]
                                       (swap! contacts conj {:seat seat :prompt prompt})
                                       {:job-id (if (= seat "wm-reviewer") "claim-review" (:job-id author))})
                        :poll-fn (fn [_ id]
                                   (if (= id "claim-review")
                                     {:job-id id :state "done" :result "FULL_LOOP_REVIEW: REJECT REPL change does not satisfy prelim practice"
                                      :execution {:executed true :tool-events 1}}
                                     author))}))))]
    (println "FROZEN-REVIEW" (pr-str {:outcome (:outcome result) :data (select-keys (:data result) [:failure-kind :error :reviewed-claim-refused?])
                                     :contacts (mapv #(select-keys % [:seat]) @contacts)}))
    (is (some #(= "wm-reviewer" (:seat %)) @contacts))
    (is (= :guardrail-refusal (:outcome result)))
    (is (= :reviewed-claim-refused (get-in result [:data :failure-kind])))
    (is (nil? (get-in result [:data :repair-obligation])))))

(deftest ordinary-claim-refusal-is-not-an-injury
  (let [r {:run/id "claim-review" :click/id "claim-click"
           :failure {:kind :reviewed-claim-refused :stage :author-wait}
           :run-output {:outcome :guardrail-refusal}}
        c (terminal/classify r {:path "record.edn" :sha256 (apply str (repeat 64 "a"))})]
    (is (= :non-injury (:classification c)))))
