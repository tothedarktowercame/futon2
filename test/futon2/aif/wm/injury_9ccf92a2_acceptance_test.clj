(ns futon2.aif.wm.injury-9ccf92a2-acceptance-test
  "Commissioned repair acceptance for the c52/9cc author handoff defect.

  The retained identities are historical. All corrected replies are explicitly
  synthetic test evidence; this namespace does not claim that zai-2 supplied a
  correction or that the original mission task succeeded."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :as test :refer [deftest is testing use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]))

(def fixture
  (-> "futon2/aif/wm/injury_9ccf92a2_acceptance_fixture.edn"
      io/resource slurp edn/read-string))
(def retained (:retained-identities fixture))
(def synthetic (:synthetic-responses fixture))

(defn- with-owner-fixtures [thunk]
  ;; The deciding correction path is runner-owned. Compose the owner namespace's
  ;; registered once/each chain rather than calling its helpers unfixture'd.
  (require 'futon2.aif.full-loop-runner-test)
  (let [m (meta (the-ns 'futon2.aif.full-loop-runner-test))
        fixtures (test/join-fixtures
                  (concat (:clojure.test/once-fixtures m)
                          (:clojure.test/each-fixtures m)))]
    (fixtures thunk)))

(use-fixtures :each with-owner-fixtures)

(defn- original-job []
  (merge {:job-id (:original-job-id retained)
          :agent-id (:author retained)}
         (:original-done-at-base synthetic)))

(defn- corrected-job
  ([] (corrected-job {}))
  ([overrides]
   (merge {:job-id "synthetic-9cc-correction-1"
           :agent-id (:author retained)}
          (:corrected-existing-refusal synthetic)
          overrides)))

(defn- construction []
  {:construction-kind :selected-cascade
   :selected-action {:target (:target retained)
                     :compiled-cascade-digest (:compiled-cascade-digest retained)}
   :repair-contract {:dispatch-sha256 (:dispatch-sha256 retained)}})

(defn- correction-call
  [{:keys [allowance corrected original repo]}]
  (let [dispatches (atom [])
        correction-var (ns-resolve 'futon2.aif.full-loop-runner
                                   'author-response-correction-loop)]
    ;; Cross-revision adapter: c52's source has no correction operation. Return
    ;; a semantic absence value so the historical red is about missing promised
    ;; behavior, never an unresolved symbol or fixture API crash.
    (if-not correction-var
      {:status :correction-behavior-absent :dispatches dispatches}
      (let [opts {:author-response-corrections (if (nil? allowance) 1 allowance)
                  :dispatch-fn
                  (fn [_ agent caller target prompt]
                    (swap! dispatches conj {:agent agent :caller caller
                                            :target target :prompt prompt})
                    {:job-id "synthetic-9cc-correction-1"})
                  :poll-fn (fn [_ _] (or corrected (corrected-job)))}
            result
            (with-redefs-fn
              {#'runner/run-phase! (fn [_ _ _ thunk & _] (thunk))}
              #(try
                 (correction-var
                  opts {} (:author retained) (atom 0) (:target retained)
                  (or repo (:repository retained))
                  {:repo (or repo (:repository retained))
                   :head (:base-head retained) :observed-at-ms 1}
                  (construction) {:job-id (:original-job-id retained)}
                  "PINNED ORIGINAL PROMPT" (or original (original-job)))
                 (catch clojure.lang.ExceptionInfo e e)))]
        {:status :correction-behavior-present
         :dispatches dispatches :result result}))))

(deftest retained-fixture-pins-the-full-c52-identity
  (is (= "38ee0703e9bb7ee8b3bcb69f2dc0ded8a7b43c0af239e53f144b1d42061b3a5f"
         (get-in retained [:cause-record-pin :sha256])))
  (is (= "90ba01cdc4ffa3cf8cc061901def0756f1304e391071f4da119763d999f206ad"
         (:dispatch-sha256 retained)))
  (is (= ["M-war-machine-wiring" "/home/joe/code/futon5a"
          "68120423443eab3243c400563cc9c66f45fea6e7" "zai-2"
          "invoke-1792869943375-3025-243e90fa"
          "cc5cbb7fe7ecd93ed9af5efb608fd1b87b5bb84a2f2aa4bf89dbe21dc437321f"
          "249ce8a6685edbbb23b019d2661d5dc2c2198e90"]
         ((juxt :target :repository :base-head :author :original-job-id
                :compiled-cascade-digest :existing-artifact) retained)))
  (is (str/includes? (:notice synthetic) "synthetic replies")))

(deftest prompt-explicitly-states-already-satisfied-refusal
  (let [prompt-var (ns-resolve 'futon2.aif.full-loop-runner 'author-prompt)
        text (when prompt-var
               (prompt-var {:author (:author retained) :reviewer "reviewer"
                            :target-repository (:repository retained)
                            :target-repository-head (:base-head retained)}
                           (:target retained) {:id (:target retained)} {} []))]
    (is (string? text) "historical red: explicit prompt behavior is absent")
    (is (str/includes? text "do NOT present the base HEAD as a new DONE artifact"))
    (is (str/includes? text
                       "FULL_LOOP_AUTHOR: REFUSE :already-satisfied-at-base-head"))))

(deftest done-at-base-gets-one-immutable-same-author-correction
  (let [{:keys [status dispatches result]} (correction-call {})
        correction (first (:author-response-corrections result))]
    (is (= :correction-behavior-present status)
        "historical red: the promised correction behavior is absent")
    (is (= 1 (count @dispatches)))
    (is (= [(:author retained) (:target retained)]
           ((juxt :agent :target) (first @dispatches))))
    (is (= [(:target retained) (:repository retained) (:base-head retained)
            (:author retained) (:original-job-id retained)]
           ((juxt :target :repository :base-head :author :original-request-id)
            (:binding correction))))
    (is (= :corrected (:status correction)))))

(deftest synthetic-corrected-refusal-passes-the-unchanged-bound-repository-gate
  (let [{:keys [result]} (correction-call {})
        job (:author-job result)
        classification
        (runner/author-refusal-classification
         job (:repository retained)
         (fn [repo sha] (and (= repo (:repository retained))
                             (= sha (:existing-artifact retained)))))]
    (is (= {:outcome :already-satisfied
            :grounded-commit {:repo "futon5a"
                              :sha (:existing-artifact retained)}}
           classification))
    (is (= (:existing-artifact retained) (:artifact-ref job)))
    (is (nil? (:commit
               (runner/fresh-artifact-binding
                {:repo-head-observation-fn
                 (fn [repo] {:repo repo :head (:base-head retained)
                             :observed-at-ms 2})
                 :resolve-commit-sha-fn (fn [_ sha] sha)
                 :ancestor-fn (constantly true)
                 :commit-time-ms-fn (constantly 1)}
                (:repository retained)
                {:repo (:repository retained) :head (:base-head retained)
                 :observed-at-ms 1}
                (original-job))))
        "DONE at the dispatched base never becomes a fresh artifact")))

(deftest repeated-base-wrong-job-foreign-repo-and-zero-allowance-refuse
  (testing "repeated base"
    (let [{:keys [result]}
          (correction-call
           {:corrected (merge (corrected-job)
                              {:artifact-ref nil
                               :result (str "FULL_LOOP_AUTHOR: DONE "
                                            (:base-head retained))})})]
      (is (= :corrected-response-repeats-base-done
             (get-in (ex-data result) [:author-response-correction :reason])))))
  (testing "wrong returned job identity"
    (let [{:keys [result]}
          (correction-call {:corrected (corrected-job {:job-id "foreign-job"})})]
      (is (= :reply-provenance-mismatch
             (get-in (ex-data result) [:author-response-correction :reason])))))
  (testing "foreign repository cannot ground the synthetic refusal"
    (let [{:keys [result]} (correction-call {})]
      (is (= {:outcome :invalid-author-refusal}
             (runner/author-refusal-classification
              (:author-job result) "/foreign/repository"
              (fn [repo sha] (and (= repo (:repository retained))
                                  (= sha (:existing-artifact retained)))))))))
  (testing "zero allowance"
    (let [{:keys [dispatches result]} (correction-call {:allowance 0})]
      (is (zero? (count @dispatches)))
      (is (= :author-response-correction-exhausted
             (get-in (ex-data result) [:author-response-correction :reason]))))))
