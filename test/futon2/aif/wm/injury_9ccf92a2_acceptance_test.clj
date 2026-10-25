(ns futon2.aif.wm.injury-9ccf92a2-acceptance-test
  "Commissioned repair acceptance for the c52/9cc author handoff defect.

  The retained identities are historical. All corrected replies are explicitly
  synthetic test evidence; this namespace does not claim that zai-2 supplied a
  correction or that the original mission task succeeded."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :as test :refer [deftest is testing use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.load-identity :as load-identity]))

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
   ;; The historical prompt bytes are unavailable. This repository-owned
   ;; reconstruction keeps the two retained contract identities together in
   ;; the prompt-construction subset that production hashes and redisplays.
   :repair-contract {:compiled-cascade-digest
                     (:compiled-cascade-digest retained)
                     :dispatch-sha256 (:dispatch-sha256 retained)}})

(defn- sha256 [s]
  (load-identity/sha256 (.getBytes s "UTF-8")))

(defn- composed-author-wait
  "Cross the owner's real run-opportunity!/author-wait caller boundary.

  The owner fixture is intentionally generic. This transparent adapter replaces
  only its external ports and selected fixture identities: author zai-2, target,
  bound repository/base, original job id and compiled/dispatch contract fields.
  It does not call or redefine the correction operation, refusal classifier, or
  fresh-artifact gate. The same adapter runs on c52 and repaired sources."
  [{:keys [allowance corrected repository artifact-valid?]
    :or {allowance 1 artifact-valid? true}}]
  (let [run-attempt (ns-resolve 'futon2.aif.full-loop-runner-test
                                'run-feature-card-attempt)
        dispatches (atom [])
        requests (atom {})
        correction-id "synthetic-9cc-correction-1"
        reviewer-id "synthetic-9cc-review"
        repo (or repository (:repository retained))
        corrected (or corrected (corrected-job))
        dispatch-fn
        (fn [_ agent caller target prompt]
          (let [n (inc (count @dispatches))
                job-id (case n
                         1 (:original-job-id retained)
                         2 correction-id
                         reviewer-id)]
            (swap! dispatches conj {:ordinal n :agent agent :caller caller
                                    :target target :prompt prompt :job-id job-id})
            (swap! requests assoc job-id {:prompt prompt})
            {:job-id job-id}))
        poll-fn
        (fn [_ job-id]
          (condp = job-id
            (:original-job-id retained) (original-job)
            correction-id corrected
            reviewer-id {:job-id reviewer-id :agent-id "codex-7" :state "done"
                         :execution {:executed true :tool-events 1 :command-events 1}
                         :result-summary "FULL_LOOP_REVIEW: APPROVE"
                         :request (get @requests reviewer-id)}))
        transform
        (fn [j]
          (-> j
              (assoc-in [:decision :action :target] (:target retained))
              (assoc-in [:decision :action :compiled-cascade-digest]
                        (:compiled-cascade-digest retained))
              (assoc-in [:decision :action :repair-contract :dispatch-sha256]
                        (:dispatch-sha256 retained))))
        selected-entry-var (ns-resolve 'futon2.aif.full-loop-runner
                                       'selected-entry)
        source-drift-var (ns-resolve 'futon2.aif.full-loop-runner
                                     'refuse-on-runner-source-drift!)
        result
        (with-redefs-fn
          {#'runner/primary-repos (constantly [])
           #'runner/commit-exists-in?
           (fn [candidate-repo sha]
             (and artifact-valid?
                  (= candidate-repo (:repository retained))
                  (= sha (:existing-artifact retained))))
           ;; Current owner fixtures perform these two test-only isolations
           ;; behind :isolate-modified-runner?. c52 has runner-options but not
           ;; that flag, so apply the identical boundary outside the owner
           ;; helper on both revisions. Neither var decides correction,
           ;; refusal classification, nor artifact validity.
           source-drift-var (constantly {:runner/source-check :fixture-current})
           selected-entry-var
           (fn [j]
             (when-let [action (get-in j [:decision :action])]
               {:action action
                :rank (or (get-in j [:decision :rank]) 1)
                :G-efe (get-in j [:decision :controller-score])
                :controller-score (get-in j [:decision :controller-score])}))}
          #(run-attempt
            {:author-response-corrections allowance
             :initial-author-job (original-job)
             :cure-agent (:author retained)
             :cure-summary (:result corrected)
             :cure-commit (:artifact-ref corrected)
             :cure-card (:feature-card corrected)
             :judgement-transform-fn transform
             :runner-options
             {:author (:author retained)
              :reviewer "codex-7"
              :repair-reviewer "codex-1"
              :construct-fn (fn [_] (construction))
              :target-repo-fn (fn [& _] repo)
              :repo-head-observation-fn
              (fn [_] {:repo repo :head (:base-head retained) :observed-at-ms 1})
              :dispatch-fn dispatch-fn
              :poll-fn poll-fn}}))]
    {:result result :dispatches @dispatches :requests @requests
     :synthetic-provenance (:notice synthetic)
     :fixture-adapter {:substitutions
                       {:author (:author retained) :target (:target retained)
                        :repository repo :base-head (:base-head retained)
                        :original-job-id (:original-job-id retained)
                        :construction (construction)}}}))

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

(deftest composed-author-wait-corrects-before-the-bound-refusal-gate
  (let [{:keys [result dispatches fixture-adapter synthetic-provenance]}
        (composed-author-wait {})
        correction (get-in result [:result :data :author-job
                                   :author-response-corrections 0])
        original-dispatch (first dispatches)
        correction-dispatch (second dispatches)
        reconstructed-prompt (:prompt original-dispatch)]
    (is (= {:author (:author retained) :target (:target retained)
            :repository (:repository retained) :base-head (:base-head retained)
            :original-job-id (:original-job-id retained)
            :construction (construction)}
           (:substitutions fixture-adapter))
        "transparent owner-fixture substitutions are retained as test evidence")
    (is (= (:notice synthetic) synthetic-provenance))
    (is (str/includes? synthetic-provenance "test-only synthetic replies"))
    (is (= 2 (count dispatches))
        "exactly the original author request and one correction are sent")
    (is (= [(:author retained) (:author retained)]
           (mapv :agent dispatches))
        "the actual author-wait path dispatches exactly one same-author correction")
    (is (= [(:target retained) (:target retained)]
           (mapv :target dispatches)))
    (is (= [(:target retained) (:repository retained) (:base-head retained)
            (:author retained) (:original-job-id retained)]
           ((juxt :target :repository :base-head :author :original-request-id)
            (:binding correction))))
    (is (= (:compiled-cascade-digest retained)
           (get-in correction [:binding :construction-contract
                               :repair-contract :compiled-cascade-digest])))
    (is (= (:dispatch-sha256 retained)
           (get-in correction [:binding :construction-contract
                               :repair-contract :dispatch-sha256])))
    (is (= (sha256 (pr-str (get-in correction [:binding :construction-contract])))
           (get-in correction [:binding :construction-contract-sha256])))
    (is (= (sha256 (pr-str reconstructed-prompt))
           (get-in correction [:binding :original-prompt-sha256]))
        "the reconstructed prompt—not unavailable historical full bytes—is bound")
    (is (str/includes? reconstructed-prompt
                       "do NOT present the base HEAD as a new DONE artifact"))
    (is (str/includes? reconstructed-prompt
                       "FULL_LOOP_AUTHOR: REFUSE :already-satisfied-at-base-head"))
    (is (str/includes? (:prompt correction-dispatch) "IMMUTABLE CORRECTION BINDING"))
    (is (str/includes? (get-in correction [:corrected :job :result-summary])
                       "REFUSE :already-satisfied-at-base-head")
        "synthetic correction provenance survives the composed author-wait evidence")
    (is (nil? (get-in correction [:original :job :artifact-ref]))
        "the synthetic original DONE-at-base remains unbound")
    (is (= :corrected (:status correction)))
    (is (= :already-satisfied (get-in result [:result :outcome])))
    (is (= (:existing-artifact retained)
           (get-in result [:result :data :grounded-commit :sha])))
    (is (not= (:base-head retained)
              (get-in result [:result :data :grounded-commit :sha])))))

(deftest repeated-base-wrong-job-foreign-repo-and-zero-allowance-refuse
  (testing "repeated base"
    (let [{:keys [result]}
          (composed-author-wait
           {:corrected (merge (corrected-job)
                              {:artifact-ref nil
                               :result (str "FULL_LOOP_AUTHOR: DONE "
                                            (:base-head retained))})})]
      (is (= :corrected-response-repeats-base-done
             (get-in result [:result :data :error-data
                             :author-response-correction :reason])))))
  (testing "wrong returned job identity"
    (let [{:keys [result]}
          (composed-author-wait
           {:corrected (corrected-job {:job-id "foreign-job"})})]
      (is (= :reply-provenance-mismatch
             (get-in result [:result :data :error-data
                             :author-response-correction :reason])))))
  (testing "foreign repository is rejected by the real corrected refusal gate"
    (let [{:keys [result]}
          (composed-author-wait {:repository "/foreign/repository"})]
      (is (= :invalid-author-refusal
             (get-in result [:result :data :failure-kind])))))
  (testing "zero allowance"
    (let [{:keys [dispatches result]} (composed-author-wait {:allowance 0})]
      (is (= 1 (count dispatches)) "only the original author dispatch occurs")
      (is (= :author-response-correction-exhausted
             (get-in result [:result :data :error-data
                             :author-response-correction :reason]))))))
