(ns futon2.aif.author-response-correction-test
  "Hermetic author-wait coverage for the narrow DONE-at-base correction.

  These replies are synthetic.  They exercise the runner's actual correction
  loop and then its existing author refusal/fresh-artifact validators; they do
  not rewrite c52d61b9 or claim an author completed live work."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :as test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

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

(deftest corrected-refusal-is-grounded-only-in-its-dispatched-repository
  (let [{:keys [result]} (run-correction
                           {:corrected-job (already-satisfied "correction-1" "author")})
        refused
        (with-redefs-fn
          {#'runner/primary-repos (fn [] ["/other-repository"])
           #'runner/commit-exists-in? (fn [repo _] (= repo "/other-repository"))}
          #(try
             (runner/throw-if-author-refused! (:author-job result) "M-target" :author-wait)
             nil
             (catch clojure.lang.ExceptionInfo e (ex-data e))))]
    (is (= :invalid-author-refusal (:failure-kind refused)))
    (is (not= :already-satisfied (:outcome refused)))))

(deftest corrected-refusal-still-admits-an-existing-artifact-in-its-own-repository
  (let [{:keys [result]} (run-correction
                           {:corrected-job (already-satisfied "correction-1" "author")})
        refused
        (with-redefs-fn
          {#'runner/primary-repos (constantly [])
           #'runner/commit-exists-in? (fn [repo sha] (and (= repo "/repo")
                                                          (= sha existing)))}
          #(try
             (runner/throw-if-author-refused! (:author-job result) "M-target" :author-wait)
             nil
             (catch clojure.lang.ExceptionInfo e (ex-data e))))]
    (is (= :already-satisfied (:outcome refused)))
    (is (= {:repo "repo" :sha existing} (:grounded-commit refused)))))

(defn- temp-dir []
  (.toFile (Files/createTempDirectory "author-correction-record-"
                                     (make-array FileAttribute 0))))

(defn- with-full-loop-runner-fixtures
  "The integration helper belongs to full-loop-runner-test.  Requiring that
  namespace does not install its test fixtures, so compose its owned once/each
  chain explicitly around this one test's real author-wait call."
  [thunk]
  (require 'futon2.aif.full-loop-runner-test)
  (let [metadata (meta (the-ns 'futon2.aif.full-loop-runner-test))
        fixtures (test/join-fixtures
                  (concat (:clojure.test/once-fixtures metadata)
                          (:clojure.test/each-fixtures metadata)))]
    (fixtures thunk)))

(defn- correction-feature-attempt []
  (let [run-attempt (ns-resolve 'futon2.aif.full-loop-runner-test
                                'run-feature-card-attempt)
        feature-card (var-get (ns-resolve 'futon2.aif.full-loop-runner-test
                                          'feature-card-claim))]
    (run-attempt
     {:author-response-corrections 1
      :isolate-modified-runner? true
      :initial-author-job
      {:job-id "feature-author" :agent-id "zai-5"
       :state "done" :artifact-ref "feature123"
       :result-summary "FULL_LOOP_AUTHOR: DONE feature123"
       :execution {:executed true :tool-events 1 :command-events 1}}
      :cure-agent "zai-5"
      :cure-summary "FULL_LOOP_AUTHOR: REFUSE :already-satisfied-at-base-head"
      :cure-card feature-card})))

(deftest correction-refusal-evidence-survives-the-real-sealed-terminal-writer
  (let [dir (temp-dir)]
    (try
      (let [{:keys [result]} (run-correction
                               {:corrected-job (already-satisfied "wrong-id" "author")})
            written (runner/persist-run-record!
                     {:run-record-dir (.getPath dir)
                      :observation-labels-path (str (io/file dir "labels.edn"))
                      :scan-render-fn (fn [& _] nil)
                      :nano-time-fn (constantly 0)}
                     "author-correction-synthetic" "2026-10-25T00:00:00Z"
                     {:outcome :build-failed
                      :data {:failure-kind (:failure-kind (ex-data result))
                             :failure-stage :author-wait
                             :error-data (ex-data result)}})
            record (edn/read-string (slurp (:run-record written)))
            retained (get-in record [:failure :detail :author-response-correction])]
        (is (= "/repo" (get-in retained [:binding :repository])))
        (is (= "original-1" (get-in retained [:original :request :job-id])))
        (is (= "correction-1" (get-in retained [:request :job-id])))
        (is (= "wrong-id" (get-in retained [:corrected :job :job-id])))
        (is (= :reply-provenance-mismatch (:reason retained))))
      (finally
        (doseq [f (reverse (file-seq dir))]
          (io/delete-file f true))))))

(deftest actual-author-wait-path-corrects-before-the-unchanged-refusal-gate
  "The correction crosses the real author-wait boundary.  This fixture does
  not fabricate a git commit in its temporary repository, so the unchanged
  already-satisfied artifact check subsequently refuses it as invalid."
  (with-full-loop-runner-fixtures
    #(let [{:keys [result dispatches]} (correction-feature-attempt)]
       (is (= ["zai-5" "zai-5"] dispatches)
           "the actual author-wait path dispatches exactly one correction")
       (is (= :invalid-author-refusal (get-in result [:data :failure-kind])))
       (is (= "feature-cure"
              (get-in result [:data :error-data :author-job
                              :author-response-corrections 0 :request :job-id]))))))

(deftest composed-author-wait-failure-persists-bounded-correction-evidence
  "A later existing refusal receives the corrected author job.  The real
  persistence boundary must retain its bounded correction chain there too."
  (with-full-loop-runner-fixtures
    #(let [dir (temp-dir)]
       (try
         (let [{:keys [result]} (correction-feature-attempt)
               written (runner/persist-run-record!
                        {:run-record-dir (.getPath dir)
                         :observation-labels-path (str (io/file dir "labels.edn"))
                         :scan-render-fn (fn [& _] nil)
                         :nano-time-fn (constantly 0)}
                        "downstream-correction-synthetic" "2026-10-25T00:00:00Z" result)
               record (edn/read-string (slurp (:run-record written)))
               author-job (get-in record [:failure :detail :author-job])]
           (is (= :invalid-author-refusal (get-in record [:failure :kind])))
           (is (= "feature-cure"
                  (get-in author-job [:author-response-corrections 0 :request :job-id])))
           (is (= "feature-author"
                  (get-in author-job [:author-response-corrections 0 :original
                                      :request :job-id])))
           (is (= :corrected
                  (get-in author-job [:author-response-corrections 0 :status]))))
         (finally
           (doseq [f (reverse (file-seq dir))]
             (io/delete-file f true)))))))
