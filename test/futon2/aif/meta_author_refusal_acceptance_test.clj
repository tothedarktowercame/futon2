(ns futon2.aif.meta-author-refusal-acceptance-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.wm.loop-node-exercise :as loop-node-exercise]
            [futon2.aif.wm.terminal-receipt :as terminal-receipt]))

;; Injury injury/2aef7e0c0f48a851a781b41642bf30e52e3fa246, run
;; 2026-10-27-39eb74f5-9db6-4f1d-8d28-03694c6f1fba: the author-wait refusal
;; for M-recommendation-bindings reported the checks already satisfied at
;; base head 09a7cb8d and named that commit in :artifact-ref, but the report
;; lived in the REFUSE detail/result-summary, not in a feature card, so
;; author-refusal-classification closed :invalid-author-refusal and
;; terminal-receipt sealed a failure.  The test replays the injured job from
;; the pinned record (sha256-verified, read-only) and pins the repaired
;; close: :already-satisfied with the grounded commit, and a terminal
;; :action-receipt instead of the failure receipt.

(def pinned-record-path
  "data/wm-runs/tick-run-record-2026-10-27-39eb74f5-9db6-4f1d-8d28-03694c6f1fba.edn")
(def pinned-record-sha256
  "a543d0066d7384fd7b281e6ee6c39936ff8b1797d39d9eb0f0c3b4cf70282690")
(def base-head-sha "09a7cb8d3468031ed3fa2beb6db21c602617a572")

(defn- sha256 [path]
  (let [bytes (with-open [in (io/input-stream path)]
                (let [buf (java.io.ByteArrayOutputStream.)]
                  (loop [b (.read in)]
                    (when (>= b 0)
                      (.write buf b)
                      (recur (.read in))))
                  (.toByteArray buf)))
        digest (.digest (java.security.MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" %) digest))))

(deftest injury-2aef7e0c-author-wait-already-satisfied-report-closes-already-satisfied
  (testing "the pinned run record is the authority"
    (is (= pinned-record-sha256 (sha256 pinned-record-path))))
  (let [record (edn/read-string (slurp pinned-record-path))]
    (testing "the required loop-node evaluator re-reads the recorded decision"
      (let [replayed (loop-node-exercise/receipt (:decision record))]
        (is (map? replayed))
        (is (= (get-in record [:loop-node-exercise :status])
               (:status replayed)))))
    (testing "the author-wait refusal is classified from the report it carried"
      (let [retained (get-in record [:failure :detail :author-job])
            _ (is (some? retained) "the pinned record retains the author job")
            _ (is (= base-head-sha (:artifact-ref retained)))
            ;; The durable reply: the REFUSE marker line whose detail states
            ;; the already-satisfied report, followed by the retained summary.
            author-job (assoc retained
                              :result
                              (str "FULL_LOOP_AUTHOR: REFUSE all twelve reviewed completion checks are already satisfied by the mission header itself at base head 09a7cb8d\n\n"
                                   (:result-summary retained)))
            thrown (try
                     (runner/throw-if-author-refused!
                      author-job "M-recommendation-bindings" :author-wait)
                     nil
                     (catch clojure.lang.ExceptionInfo e
                       (ex-data e)))]
        (is (map? thrown))
        ;; Injured code returns {:failure-kind :invalid-author-refusal
        ;; :error "Author refusal lacks a reason or claims an artifact"}.
        ;; The report names base head 09a7cb8d, a commit that exists in the
        ;; futon5a repository, so the close is already-satisfied and grounded.
        (is (= :already-satisfied (:failure-kind thrown))
            "injury 2aef7e0c: injured code closed :invalid-author-refusal")
        (is (= base-head-sha (get-in thrown [:grounded-commit :sha])))
        (is (= "futon5a" (get-in thrown [:grounded-commit :repo])))
        (testing "terminal routing: the repaired close is an action receipt"
          (let [injured-close {:run/id (:run/id record)
                               :startedAt (:startedAt record)
                               :selection-event {:event :cascade-selected
                                                 :target "M-recommendation-bindings"}
                               :failure {:kind :invalid-author-refusal}}
                injured-receipt (terminal-receipt/terminal-receipt injured-close)]
            (is (= :failure (:kind injured-receipt)))
            (is (= :invalid-author-refusal (:failure-kind injured-receipt)))
            (is (= "M-recommendation-bindings" (:target injured-receipt))))
          (let [repaired-close {:run/id (:run/id record)
                                :click/id (:click/id record)
                                :startedAt (:startedAt record)
                                :selection-event {:event :cascade-selected
                                                  :target "M-recommendation-bindings"}
                                :outcome :already-satisfied
                                :grounded-commit {:repo "futon5a"
                                                  :sha base-head-sha}}
                repaired-receipt (terminal-receipt/terminal-receipt repaired-close)]
            (is (= :action-receipt (:kind repaired-receipt)))
            (is (= "M-recommendation-bindings" (:target repaired-receipt)))
            (is (= {:repo "futon5a" :sha base-head-sha}
                   (:commit repaired-receipt)))))))))
