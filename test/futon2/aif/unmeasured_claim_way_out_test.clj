(ns futon2.aif.unmeasured-claim-way-out-test
  "UNMEASURED-CLAIM-NO-WAY-OUT (runs 4955b6e6, e71c7380, f4b95abf): an
  already-satisfied claim refused :claim-mission-text-not-an-artifact gets
  exactly one round back to the author, in the same click, before the typed
  refusal lands. Fixtures are the real retained f4b95abf author reply
  (verbatim, sha-pinned in the test)."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.stop-line :as stop]))

(def reply-sha256
  "a2596820452b99a4cf70083d5cd868435cc50364a9f2da90fe816a67434b6511")

(def real-reply
  (slurp "test/resources/wm/unmeasured-claim/f4b95abf-author-reply.txt"))

(def real-author-job
  "The retained f4b95abf author job, minimally shaped: the reply text is
  verbatim from the run record's retained evidence."
  {:job-id "f4b95abf-author" :agent-id "wm-author" :state "done"
   :result real-reply})

(def refused-observation
  {:schema :wm/mission-observation-projection-v1
   :status :refused :reason :claim-mission-text-not-an-artifact
   :details {:token :hole/h2045faa0e7cc
             :locator "holes/missions/M-pattern-retrieval-calibration.md"}})

(def claim-context
  {:mission-baseline {:status :captured :rows []}
   :dispatch {:occurrence {:action/value
                           {:target "M-pattern-retrieval-calibration"
                            :want [[:hole/h2045faa0e7cc]]
                            :observation-locators
                            {[:hole/h2045faa0e7cc]
                             {:class :C4 :repo "futon3" :sha "HEAD"
                              :path "holes/missions/M-pattern-retrieval-calibration.md"
                              :decl "- [x] Publish the strict successful successor link"}}}}}})

(def mission
  {:id "M-pattern-retrieval-calibration"
   :source {:path "/home/joe/code/futon3/holes/missions/M-pattern-retrieval-calibration.md"
            :sha256 "5c141a9dfa1517c5e5e2ab9835084d876da9fb27acb04df4959ea2a5e9f0eba7"}})

(defn- run-loop
  "Drive unmeasured-claim-correction-loop with stubbed transport. The
  correction reply defaults to the author repeating the real f4b95abf claim
  verbatim."
  [& {:keys [reply allowance]}]
  (let [contacts (atom [])
        phases (atom [])
        opts (cond-> {:phase-log-fn #(swap! phases conj %)
                      :dispatch-fn (fn [_ seat caller t prompt]
                                     (swap! contacts conj
                                            {:seat seat :caller caller :target t
                                             :prompt prompt})
                                     {:job-id "correction-1"})
                      :poll-fn (fn [_ id]
                                 {:job-id id :agent-id "wm-author" :state "done"
                                  :result (or reply real-reply)})}
               (some? allowance)
               (assoc :unmeasured-claim-corrections allowance))]
    (try
      {:result (runner/unmeasured-claim-correction-loop
                opts {} "wm-author" "M-pattern-retrieval-calibration"
                "/home/joe/code/futon3" mission claim-context real-author-job
                (atom 0) refused-observation)
       :contacts @contacts :phases @phases}
      (catch Throwable t
        {:threw t :data (ex-data t)
         :contacts @contacts :phases @phases}))))

(deftest the-retained-reply-is-the-real-f4b95abf-answer
  (is (str/ends-with? (str/trimr real-reply)
                      "FULL_LOOP_AUTHOR: REFUSE :already-satisfied-at-base-head"))
  (is (str/includes? real-reply "25684350a0b7896230492d6a34984fb2323f6daa"))
  (is (str/includes? real-reply "already mechanically satisfied in the mission file")))

(deftest repeated-claim-gets-one-round-then-the-existing-typed-refusal
  (let [{:keys [threw data contacts phases]} (run-loop)]
    (testing "the author who repeats the claim gets the typed refusal"
      (is (some? threw))
      (is (= :guardrail-refusal (:outcome data)))
      (is (= :already-satisfied-wants-unmeasured (:failure-kind data)))
      (is (= :claim-mission-text-not-an-artifact
             (get-in data [:mission-observation :reason])))
      (is (true? (:reviewed-claim-refused? data)))
      (is (= :repeated-claim
             (get-in data [:unmeasured-claim-correction :status])))
      ;; no injury: the author's answer failed, not the apparatus
      (is (stop/no-finding-required? data)))
    (testing "exactly one correction round, no third contact"
      (is (= 1 (count contacts)))
      (is (= "wm-author" (:seat (first contacts))))
      (is (= ["unmeasured-claim-correction-dispatch"
              "unmeasured-claim-correction-wait"]
             (into [] (comp (filter #(str/starts-with? (name (get % :phase))
                                                       "unmeasured-claim"))
                           (map :phase)
                           (distinct)
                           (map name))
                   phases))))
    (testing "the prompt names the unmeasured wants and the mission-text rule"
      (let [prompt (:prompt (first contacts))]
        (is (str/includes? prompt "UNMEASURED CLAIM CORRECTION"))
        (is (str/includes? prompt ":claim-mission-text-not-an-artifact"))
        (is (str/includes? prompt
                           "holes/missions/M-pattern-retrieval-calibration.md"))
        (is (str/includes? prompt "ONE correction"))
        (is (str/includes? prompt "no third contact"))
        (is (str/includes? prompt "/home/joe/code/futon3"))))))

(deftest a-typed-cannot-answer-returns-the-corrected-job-for-the-unchanged-gates
  (let [{:keys [result contacts]} (run-loop
                                   :reply "FULL_LOOP_AUTHOR: REFUSE :no-real-artifact-possible-the-want-is-a-mission-text-checkbox")]
    (is (= 1 (count contacts)))
    (is (map? (:author-job result)))
    (is (= :typed-reason (get-in result [:unmeasured-claim-correction :status])))
    (is (= :typed-reason
           (get-in (:author-job result) [:unmeasured-claim-correction :status])))
    (testing "the unchanged refusal gate closes it typed, with no injury"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo #"typed reason"
           (runner/throw-if-author-refused!
            (:author-job result) "M-pattern-retrieval-calibration" :author-wait))))))

(deftest a-done-reply-returns-real-work-for-the-normal-measurement
  (let [fresh "263dfc26218e6bf0ea7417e48a897fe762801a66"
        {:keys [result contacts]} (run-loop
                                   :reply (str "FULL_LOOP_FEATURE_CARD: "
                                               "{:built \"real artifact for one want\" "
                                               ":want-coverage \"one unmeasured want\" "
                                               ":matches-intent? true "
                                               ":things-to-try [\"test -> pass\"]}\n"
                                               "FULL_LOOP_AUTHOR: DONE " fresh))]
    (is (= 1 (count contacts)))
    (is (= :real-work (get-in result [:unmeasured-claim-correction :status])))
    (is (str/includes? (get-in (:author-job result) [:result]) (str "DONE " fresh)))
    (is (= "correction-1"
           (get-in (:author-job result) [:unmeasured-claim-correction
                                         :corrected :job :job-id])))))

(deftest allowance-zero-restores-the-legacy-no-round-behavior
  (let [{:keys [result contacts]} (run-loop :allowance 0)]
    (is (nil? result))
    (is (empty? contacts))))

(deftest the-gate-fires-only-on-a-mission-text-unmeasured-claim
  (letfn [(gate [observation]
            (let [dispatches (atom 0)]
              (with-redefs
                [runner/claim-mode-observation
                 (fn [_ _ _ _] observation)]
                (try
                  [(runner/unmeasured-claim-correction-state
                    {:dispatch-fn (fn [& _] (swap! dispatches inc) {:job-id "x"})
                     :poll-fn (fn [& _] {:job-id "x" :agent-id "wm-author"
                                         :state "done" :result real-reply})}
                    {} {:author "wm-author"} "M-pattern-retrieval-calibration"
                    mission {:status :captured} (:dispatch claim-context)
                    (atom 0) real-author-job "/home/joe/code/futon3")
                   @dispatches]
                  (catch Throwable t
                    [(:failure-kind (ex-data t)) @dispatches])))))]
    (testing "a mission-text-unmeasured claim gets the round"
      (is (= 1 ((gate refused-observation) 1))))
    (testing "an admitted measurement does not"
      (is (nil? (first (gate {:status :admitted}))))
      (is (zero? ((gate {:status :admitted}) 1))))
    (testing "a different refusal reason does not"
      (is (nil? (first (gate {:status :refused
                              :reason :claim-want-false-at-reviewed-commit}))))
      (is (zero? ((gate {:status :refused
                         :reason :claim-want-false-at-reviewed-commit}) 1))))))

(deftest the-prompts-no-longer-invite-the-mission-text-claim
  (let [author (#'runner/author-prompt
                {:author "wm-author" :reviewer "wm-reviewer" :batch-id "b"
                 :target-repository "/home/joe/code/futon3"
                 :target-repository-head "263dfc26218e6bf0ea7417e48a897fe762801a66"}
                "M-pattern-retrieval-calibration" {} [] nil)]
    (is (str/includes? author "mission's own text is NEVER that existing artifact"))    (is (str/includes? author ":claim-mission-text-not-an-artifact"))))
