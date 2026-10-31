(ns futon2.aif.wm.reading-refusal-terminal-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.full-loop-cohort :as cohort]))

(deftest selected-reading-execution-boundary-is-a-typed-refusal
  (let [refusal {:kind :reading-policy-execution-semantics-owed}
        failure (#'runner/no-entry-failure-data
                 {:decision {:status :selected}}
                 refusal)]
    (testing "a selected reading policy is not reported as no-selection"
      (is (= :abstained (:outcome failure)))
      (is (= :reading-policy-execution-semantics-owed (:failure-kind failure)))))

  ;; occ-2613ce61: an :identity-forged cascade refusal produced
  ;; {:outcome :refused ...}, which cohort/outcome-kinds rejects, so the
  ;; close checkpoint threw invalid-close-outcome and the click reported
  ;; build-failed instead of a typed abstention.
  (let [forged {:outcome :refused :kind :identity-forged}
        failure (#'runner/no-entry-failure-data
                 {:decision {:status :selected}}
                 forged)]
    (is (contains? cohort/outcome-kinds (:outcome failure))
        "the typed refusal must close on a cohort-valid outcome kind")
    (is (= :identity-forged (:failure-kind failure))
        "the refusal's typed kind must survive into the failure data")))

(deftest ordinary-empty-decisions-retain-their-terminal-class
  (is (= {:outcome :abstained}
         (#'runner/no-entry-failure-data
          {:decision {:status :abstained}}
          nil)))
  (is (= {:outcome :no-selection}
         (#'runner/no-entry-failure-data
          {:decision {:status :selected}}
          nil))))

(deftest cascade-resume-errors-retain-their-typed-evidence
  (let [resume-error {:status :error
                      :kind :open-cascade-review-answer-invalid
                      :phase :cascade-execution
                      :repairable true
                      :review-answer {:status :invalid}}
        failure (#'runner/no-entry-failure-data
                 {:decision {:status :selected}}
                 resume-error)]
    (testing "a failed same-click resume is incomplete, not a policy refusal"
      (is (= :incomplete (:outcome failure)))
      (is (= :open-cascade-review-answer-invalid (:failure-kind failure)))
      (is (= :cascade-execution (:held-phase failure)))
      (is (= resume-error (:failure-detail failure))))))

;; D4-READING-REFUSAL-FINDING-CONTEXT.  The fixture is the verbatim
;; selected-target-reading record of run 2026-10-29-4961d278-6167-4ffb-9adb-
;; 02ac2b74140e (reading job invoke-1793315962107-7916-14ee8762): the
;; wm-author reader answered 24,420 characters of analysis prose with no
;; JSON object, handoff/response-json-object decoded nil, and the tick
;; closed :abstained -- while the close-core finding
;; (repair-occ-3e36e894edb0357df57476b01bef9a9a0eb4a77e38eee3838534c8bfb413877c)
;; lost the target and the typed cause entirely:
;; :failure-kind :abstained, :failure-cause {:absent :no-cause}, no target.
(def ^:private refusal-7916
  {:schema :wm/selected-target-reading-request-v1
   :status :refused
   :kind :selected-target-reading-answer-invalid
   :published false
   :target "M-typed-memories"
   :source-path "/home/joe/code/futon3c/holes/missions/M-typed-memories.md"
   :excerpt-digest "7e71fd134d9e89b62be886133b701933a0b97611bbc927901e4acfec3d1c56e3"
   :detail {:author "wm-author"
            :contacted? true
            :answer {:status :refused
                     :reason :response-invalid
                     :job-id "invoke-1793315962107-7916-14ee8762"}}})

(deftest reading-refusal-abstention-keeps-target-typed-cause-and-response-pins
  (testing "without the reading record the abstention is generic (the defect)"
    (let [failure (#'runner/no-entry-failure-data
                   {:decision {:status :abstained}} nil nil)]
      (is (= {:outcome :abstained} failure))))
  (testing "the real 7916 refusal rides the failure data the close writer copies"
    (let [failure (#'runner/no-entry-failure-data
                   {:decision {:status :abstained}} nil refusal-7916)]
      (is (= :abstained (:outcome failure)))
      (is (= :selected-target-reading-answer-invalid (:failure-kind failure))
          "the typed reading reason, not a generic abstained kind")
      (is (= :selected-target-reading (:failure-stage failure)))
      (is (= "M-typed-memories" (:target failure)))
      (is (= refusal-7916 (get-in failure [:error-data :selected-target-reading]))
          "the whole refusal record -- source path, excerpt digest, answer
           reason and job id -- reaches the finding's :failure-data")))
  (testing "the typed hold is environmental with a named cause, never machine-failure"
    (is (= :environmental-hold (#'runner/repair-class-for
                                :selected-target-reading-answer-invalid)))))

(deftest close-core-finding-for-a-real-reading-refusal-retains-its-context
  ;; Receiver-side: run the finding through the real writer
  ;; (repair/record-system-failure!) into a temp root, with the finding map
  ;; built exactly as close-core! builds it from no-entry-failure-data's
  ;; data, then read the finding EDN back and demand the target, typed
  ;; cause and reader-response pins survived publication.
  (let [failure (#'runner/no-entry-failure-data
                 {:decision {:status :abstained}} nil refusal-7916)
        repair-class (#'runner/repair-class-for (:failure-kind failure))
        dir (java.nio.file.Files/createTempDirectory
             "d4-reading-refusal-finding-"
             (make-array java.nio.file.attribute.FileAttribute 0))
        root (str dir)]
    (try
      (let [finding (repair/record-system-failure!
                     root
                     {:attempt-id "attempt-002"
                      :repair-class repair-class
                      :failure-stage (:failure-stage failure)
                      :outcome (:outcome failure)
                      :failure-kind (or (:failure-kind failure) (:outcome failure))
                      :error "War Machine abstained: selected-target reading refused"
                      :target (:target failure)
                      :failure-data (:error-data failure)
                      :repair-id "repair-d4-fixture-reading-refusal"
                      :observation {:observation/id "d4-fixture"
                                    :observed-at "2026-10-31T00:00:00Z"
                                    :source "wm-runner::d4-fixture"}})
            published (edn/read-string
                       (slurp (str dir "/findings/"
                                   (:repair/id finding) ".edn")))]
        (is (= "M-typed-memories" (:target published)))
        (is (= :selected-target-reading-answer-invalid (:failure-kind published)))
        (is (= :environmental-hold (:repair/class published)))
        (is (= "invoke-1793315962107-7916-14ee8762"
               (get-in published
                       [:failure-data :selected-target-reading
                        :detail :answer :job-id])))
        (is (= :response-invalid
               (get-in published
                       [:failure-data :selected-target-reading
                        :detail :answer :reason])))
        (is (= "/home/joe/code/futon3c/holes/missions/M-typed-memories.md"
               (get-in published [:failure-data :selected-target-reading
                                  :source-path])))
        (is (= "7e71fd134d9e89b62be886133b701933a0b97611bbc927901e4acfec3d1c56e3"
               (get-in published [:failure-data :selected-target-reading
                                  :excerpt-digest]))))
      (finally
        (run! #(try (.delete (java.io.File. (str %)))
                    (catch Exception _))
              (reverse (file-seq (java.io.File. root))))))))
