(ns futon2.aif.meta-self-heal-production-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-repair-coordinator :as coordinator]
            [futon2.aif.meta-self-heal-production :as production]
            [futon2.aif.meta-self-heal-harness :as harness]))

(defn- root []
  (.getCanonicalPath
   (.toFile (java.nio.file.Files/createTempDirectory
             "wm-production-repair"
             (make-array java.nio.file.attribute.FileAttribute 0)))))
(defn- opts [env ports]
  (merge (harness/work-order-options env)
         {:meta-injury-ledger-path (:ledger-path env)
          :meta-repair-transaction-path (str (:root env) "/transaction.edn")
          :meta-repair-ports ports}))

(deftest active-injury-preempts-ordinary-runtime-and-persists-every-boundary
  (let [env (harness/prepare-environment! (root))
        calls (atom {})
        ports (harness/fake-ports (:repo env) calls :success)
        result (runtime/run-opportunity! (opts env ports))]
    (is (= :dedicated-meta-self-heal (:route result)))
    (is (= :discharged (:status result)))
    (is (false? (:ordinary-selection-called? result)))
    (is (= [:claim :author :review :enact]
           (mapv :phase (:checkpoint-receipts result))))
    (is (= {:author 1 :review 1 :enact 1 :retry 1} @calls))
    (is (= :persisted (get-in result [:ledger-write :status])))
    (is (= :persisted (get-in result [:transaction-write :status])))
    (is (= 40 (count (:patch-sha result))))
    (is (= "offline-retry-click-1" (:retry-click result)))
    (is (:ordinary-work-eligible?
         (harness/arbitrate
          (store/load-durable-ledger (:ledger-path env)))))))

(deftest missing-production-authority-fails-closed-before-ordinary-selection
  (let [env (harness/prepare-environment! (root))
        result (runtime/run-opportunity!
                {:meta-injury-ledger-path (:ledger-path env)})]
    (is (= :meta-self-heal/production-config-required (:reason result)))
    (is (= :dedicated-meta-self-heal (:route result)))
    (is (false? (:ordinary-selection-called? result)))
    (is (some #{:repair-ports} (:absent-terms result)))))

(deftest persisted-author-checkpoint-resumes-without-duplicate-dispatch
  (let [env (harness/prepare-environment! (root))
        first-calls (atom {})
        prepared (coordinator/prepare (:durable env)
                                      (harness/work-order-options env))
        checkpoint (coordinator/coordinate
                    (:durable env) (:transaction prepared)
                    (assoc (harness/fake-ports (:repo env) first-calls :success)
                           :stop-after :author))
        transaction-path (str (:root env) "/transaction.edn")
        _ (store/persist-ledger! (:ledger-path env) (:ledger checkpoint))
        _ (spit transaction-path (str (pr-str (:transaction checkpoint)) "\n"))
        resumed-calls (atom {})
        result (runtime/run-opportunity!
                (assoc (opts env (harness/fake-ports (:repo env) resumed-calls :success))
                       :meta-repair-transaction-path transaction-path))]
    (is (= {:author 1} @first-calls))
    (is (= :discharged (:status result)))
    (is (zero? (get @resumed-calls :author 0)))
    (is (= 1 (get @resumed-calls :review)))
    (is (= 1 (get @resumed-calls :enact)))
    (is (= 1 (get @resumed-calls :retry)))))

(deftest agency-adapter-binds-fresh-seat-job-and-session-evidence
  (let [seen (atom nil)
        port (production/agency-port
              (fn [request]
                (reset! seen request)
                {:status :succeeded :job-id "job-1"
                 :session-id (:session-id request)
                 :payload {:patch-sha (apply str (repeat 40 "a"))
                           :worktree "/tmp/worktree"}})
              "wm-author")
        order {:injury-id "injury-1" :repair-id "repair-1" :attempt 1
               :author-session "fresh-author"}
        receipt (port {:work-order order :phase :author
                       :predecessor-pin {:path "p" :sha256
                                         (apply str (repeat 64 "a"))}})]
    (is (= "wm-author" (:seat @seen)))
    (is (= "fresh-author" (:session-id @seen)))
    (is (= "job-1" (get-in receipt [:payload :agency-job-id])))
    (is (= "fresh-author" (get-in receipt [:payload :agency-session-id])))))

(deftest ordinary-algorithm-construction-is-quarantined
  (let [result (runner/construct-selected-action
                {:action {:type :run-algorithm :target "A-self-heal"}})]
    (is (= :refused (:status result)))
    (is (= :meta-algorithm-wrong-route (:reason result)))
    (is (= :dedicated-meta-self-heal (:required-route result)))
    (is (= #{:ordinary-loop-node :pattern-fold :policy-selection :d-task-authority}
           (set (:forbidden-boundaries result))))))
