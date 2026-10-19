(ns futon2.aif.meta-self-heal-harness-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-repair-coordinator :as coordinator]
            [futon2.aif.meta-self-heal-harness :as harness]))

(defn- temp-root [label]
  (.getCanonicalPath
   (.toFile (java.nio.file.Files/createTempDirectory
             label (make-array java.nio.file.attribute.FileAttribute 0)))))

(defn- run-mode [mode]
  (let [env (harness/prepare-environment! (temp-root (str "wm-harness-" (name mode))))
        prepared (coordinator/prepare (:durable env) (harness/work-order-options env))
        calls (atom {})
        result (coordinator/coordinate
                (:durable env) (:transaction prepared)
                (harness/fake-ports (:repo env) calls mode))]
    {:env env :result result :calls @calls}))

(deftest isolated-success-uses-git-evidence-and-restarts-each-boundary
  (let [result (harness/run-success! (temp-root "wm-harness-success"))
        states (mapv :state (get-in result [:ledger :entries]))
        patch (:patch-sha result)
        repair (:repair-id result)]
    (is (= :dedicated-repair-required
           (get-in result [:initial-arbitration :status])))
    (is (false? (get-in result [:initial-arbitration :ordinary-work-eligible?])))
    (is (= :discharged (:status result)))
    (is (= [:open :claimed :authored :reviewed :enacted :discharged] states))
    (is (= :ordinary-work-eligible
           (get-in result [:final-arbitration :status])))
    (is (= 40 (count patch)))
    (is (= patch (get-in result [:ledger :entries 2 :patch-sha])))
    (is (= patch (get-in result [:ledger :entries 5 :patch-sha])))
    (is (every? #(= repair (:repair-id %))
                (drop 1 (get-in result [:ledger :entries]))))
    (is (= {:author 1 :review 1 :enact 1 :retry 1} (:calls result)))
    (is (= [:claim :author :review :enact]
           (mapv :phase (:checkpoints result))))
    (is (every? #(and (zero? (get-in % [:verify :exit]))
                      (= :valid (get-in % [:verify :value :status])))
                (:checkpoints result)))
    (is (empty? (:out (shell/sh "git" "status" "--porcelain"
                                :dir (:repo result)))))))

(deftest injected-transaction-failures-never-fall-through-to-ordinary-work
  (doseq [[mode status state expected-calls]
          [[:author-failure :held :claimed {:author 1}]
           [:review-rejection :held :authored {:author 1 :review 1}]
           [:review-substitution :refused :authored {:author 1 :review 1}]
           [:enact-failure :held :reviewed {:author 1 :review 1 :enact 1}]
           [:retry-failure :reopened :reopened
            {:author 1 :review 1 :enact 1 :retry 1}]
           [:retry-unattributed :refused :enacted
            {:author 1 :review 1 :enact 1 :retry 1}]]]
    (testing (name mode)
      (let [{:keys [result] :as run} (run-mode mode)
            durable {:ledger (:ledger result)}]
        (is (= status (:status result)) (pr-str run))
        (is (= state (-> result :ledger :entries last :state)))
        (is (= expected-calls (:calls run)))
        (is (false? (:ordinary-work-eligible? (harness/arbitrate durable))))))))

(deftest detection-and-claim-failures-retain-exclusive-injury-boundary
  (let [root (temp-root "wm-harness-detection")
        bad-record (str root "/bad.edn")
        ledger-path (str root "/ledger.edn")]
    (spit bad-record "{:broken")
    (is (= :store/malformed-edn
           (:reason (store/ingest-record-file! ledger-path bad-record))))
    (is (not (.exists (io/file ledger-path)))))
  (let [env (harness/prepare-environment! (temp-root "wm-harness-claim"))
        opts (assoc (harness/work-order-options env)
                    :reviewer-session "fresh-author-session-1")
        refused (coordinator/prepare (:durable env) opts)]
    (is (= :coordinator/preparation-refused (:reason refused)))
    (is (= :dedicated-repair-required (:status (harness/arbitrate (:durable env)))))
    (is (false? (:ordinary-work-eligible? (harness/arbitrate (:durable env)))))))

(deftest campaign-trace-comparison-records-real-differences
  (let [real (harness/campaign-cause)
        result (harness/run-success! (temp-root "wm-harness-trace"))]
    (is (= :construction (get-in real [:record :failure :stage])))
    (is (= :fold-output-invalid (get-in real [:record :failure :kind])))
    (is (= [:open :claimed :authored :reviewed :enacted :discharged]
           (mapv :state (get-in result [:ledger :entries]))))
    ;; The campaign record has one terminal failure and no repair transaction;
    ;; the harness intentionally adds sealed repair phases and an attributed retry.
    (is (nil? (get-in real [:record :repair-id])))
    (is (string? (:repair-id result)))))

(deftest ordinary-machinery-is-not-a-harness-dependency
  (let [source (pr-str (read-string
                        (slurp "src/futon2/aif/meta_self_heal_harness.clj")))
        coordinator-source
        (pr-str (read-string
                 (slurp "src/futon2/aif/meta_repair_coordinator.clj")))]
    (doseq [forbidden ["loop-node" "pattern-fold" "d-task" "policy-selection"
                       "family-selection"]]
      (is (not (str/includes? source forbidden)))
      (is (not (str/includes? coordinator-source forbidden))))))
