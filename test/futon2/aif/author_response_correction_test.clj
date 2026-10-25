(ns futon2.aif.author-response-correction-test
  "Hermetic author-wait coverage for the narrow DONE-at-base correction.

  These replies are synthetic.  They exercise the runner's actual correction
  loop and then its existing author refusal/fresh-artifact validators; they do
  not rewrite c52d61b9 or claim an author completed live work."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]))

(def base "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
(def existing "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
(def fresh "cccccccccccccccccccccccccccccccccccccccc")

(defn- done [job-id agent sha]
  {:job-id job-id :agent-id agent :state "done"
   :result (str "FULL_LOOP_FEATURE_CARD: {:built \"implementation\" "
                ":want-coverage \"covered\" :matches-intent? true "
                ":things-to-try [\"test -> pass\"]}\n"
                "FULL_LOOP_AUTHOR: DONE " sha)})

(defn- already-satisfied [job-id agent]
  {:job-id job-id :agent-id agent :state "done"
   :artifact-ref existing
   :feature-card {:built "already satisfied at existing commit"
                  :want-coverage "already true"
                  :matches-intent? true
                  :things-to-try ["git show -> existing evidence"]}
   :result "FULL_LOOP_AUTHOR: REFUSE :already-satisfied-at-base-head"})

(defn- run-correction
  [{:keys [allowance corrected-job original-job dispatch-fn]}]
  (let [dispatches (atom [])
        opts (cond-> {:author-response-corrections (or allowance 1)
                       :dispatch-fn (or dispatch-fn
                                        (fn [_ agent caller target prompt]
                                          (swap! dispatches conj {:agent agent :caller caller
                                                                  :target target :prompt prompt})
                                          {:job-id "correction-1"}))
                       :poll-fn (fn [_ _] corrected-job)}
               (nil? dispatch-fn)
               identity)
        original-job (or original-job (done "original-1" "author" base))
        result
        (with-redefs-fn
          {#'runner/run-phase! (fn [_ _ _ thunk & _] (thunk))}
          #(try
             (#'runner/author-response-correction-loop
              opts {} "author" (atom 0) "M-target" "/repo"
              {:repo "/repo" :head base :observed-at-ms 1}
              {:construction-kind :synthetic
               :selected-action {:type :synthetic :target "M-target"}}
              {:job-id "original-1"}
              "ORIGINAL PROMPT" original-job)
             (catch clojure.lang.ExceptionInfo e e)))]
    {:dispatches dispatches :result result}))

(deftest base-done-gets-one-qualified-correction-and-the-real-refusal-gate
  (let [{:keys [dispatches result]}
        (run-correction {:corrected-job (already-satisfied "correction-1" "author")})
        correction (first (:author-response-corrections result))]
    (is (= 1 (count @dispatches)))
    (is (= "correction-1" (get-in correction [:request :job-id])))
    (is (= "original-1" (get-in correction [:original :request :job-id])))
    (is (= base (get-in correction [:binding :base-head])))
    (is (= "author" (get-in correction [:corrected :job :agent-id])))
    (is (re-find #"IMMUTABLE CORRECTION BINDING"
                 (:prompt (first @dispatches))))
    (is (= {:outcome :already-satisfied
            :grounded-commit {:repo "repo" :sha existing}}
           (runner/author-refusal-classification
            (:author-job result) "/repo"
            (fn [repo sha] (and (= "/repo" repo) (= existing sha))))))
    (is (= :corrected (:status correction)))))

(deftest corrected-fresh-done-keeps-the-existing-fresh-artifact-boundary
  (let [{:keys [result]}
        (run-correction {:corrected-job (done "correction-1" "author" fresh)})
        binding (runner/fresh-artifact-binding
                 {:repo-head-observation-fn
                  (fn [repo] {:repo repo :head fresh :observed-at-ms 2})
                  :resolve-commit-sha-fn (fn [_ sha] (when (= fresh sha) fresh))
                  :ancestor-fn (fn [_ older newer] (= [base fresh] [older newer]))
                  :commit-time-ms-fn (fn [& _] 2)}
                 "/repo" {:repo "/repo" :head base :observed-at-ms 1}
                 (:author-job result))]
    (is (= fresh (:commit binding)))
    (is (= :done (:verdict (@#'runner/author-verdict (:author-job result)))))))

(deftest correction-refuses-bad-replies-before-the-unchanged-author-gates
  (doseq [[label job reason]
          [[:noncompleted (assoc (already-satisfied "correction-1" "author")
                                 :state "failed") :correction-job-not-done]
           [:cancelled (assoc (already-satisfied "correction-1" "author")
                              :state "cancelled") :correction-job-cancelled]
           [:wrong-seat (already-satisfied "correction-1" "other")
            :reply-provenance-mismatch]
           [:wrong-id (already-satisfied "other-id" "author")
            :reply-provenance-mismatch]
           [:malformed {:job-id "correction-1" :agent-id "author" :state "done"
                        :result "no author marker"}
           :corrected-author-response-malformed]
           [:repeated-base (done "correction-1" "author" base)
            :corrected-response-repeats-base-done]]]
    (testing (name label)
      (let [{:keys [dispatches result]} (run-correction {:corrected-job job})]
        (is (= 1 (count @dispatches)))
        (is (= :author-response-correction-refused
               (:failure-kind (ex-data result))))
        (is (= reason (get-in (ex-data result)
                              [:author-response-correction :reason])))))))

(deftest only-the-exact-original-base-done-shape-can-dispatch
  (let [{:keys [dispatches result]}
        (run-correction {:original-job (done "original-1" "author" fresh)
                         :corrected-job (already-satisfied "correction-1" "author")})]
    (is (zero? (count @dispatches))
        "a different artifact failure does not enter correction")
    (is (= [] (:author-response-corrections result)))
    (is (re-find (re-pattern fresh) (get-in result [:author-job :result])))))

(deftest zero-or-malformed-allowance-never-dispatches
  (testing "zero is a typed exhausted correction, not a detour"
    (let [{:keys [dispatches result]}
          (run-correction {:allowance 0
                           :corrected-job (already-satisfied "correction-1" "author")})]
      (is (zero? (count @dispatches)))
      (is (= :author-response-correction-exhausted
             (get-in (ex-data result) [:author-response-correction :reason])))))
  (testing "malformed configuration is rejected before dispatch"
    (let [{:keys [dispatches result]}
          (run-correction {:allowance "1"
                           :corrected-job (already-satisfied "correction-1" "author")})]
      (is (zero? (count @dispatches)))
      (is (= :author-response-correction-config-invalid
             (:failure-kind (ex-data result)))))))

(deftest correction-dispatch-thunk-never-resends-on-replay
  (let [dispatches (atom 0)
        opts {:author-response-corrections 1
              :dispatch-fn (fn [& _]
                             (swap! dispatches inc)
                             (throw (ex-info "synthetic ambiguous transport" {})))}
        replaying-run-phase
        (fn [_ _ phase thunk & _]
          (if (= :author-response-correction-dispatch phase)
            (do (try (thunk) (catch Throwable _ nil))
                (thunk))
            (thunk)))
        thrown
        (with-redefs-fn
          {#'runner/run-phase! replaying-run-phase}
          #(try
             (#'runner/author-response-correction-loop
              opts {} "author" (atom 0) "M-target" "/repo"
              {:repo "/repo" :head base}
              {:construction-kind :synthetic} {:job-id "original-1"}
              "ORIGINAL PROMPT" (done "original-1" "author" base))
             nil
             (catch Throwable t t)))]
    (is (= "synthetic ambiguous transport" (ex-message thrown)))
    (is (= 1 @dispatches))))
