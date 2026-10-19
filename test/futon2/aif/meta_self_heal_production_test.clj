(ns futon2.aif.meta-self-heal-production-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-repair-coordinator :as coordinator]
            [futon2.aif.meta-self-heal-production :as production]
            [futon2.aif.meta-self-heal-harness :as harness]
            [futon2.aif.load-identity :as load-identity]))

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

(defn- recorded-injury-source! [dir]
  ;; This is the separately recorded campaign fold-output-invalid shape, not
  ;; P16's L1 reading-coverage pin or an invented diagnosis fixture.
  (let [record (:record (second (edn/read-string
                                 (slurp "test/fixtures/meta-self-heal/campaign-2026-10-19.edn"))))
        path (str dir "/record.edn")
        bytes (.getBytes (str (pr-str record) "\n") "UTF-8")]
    (spit path (String. bytes "UTF-8"))
    {:path path :sha256 (load-identity/sha256 bytes)}))

(defn- durable-with-active-source [pin]
  {:ledger {:entries [{:injury-id "injury-recorded-l1"
                       :state :open :cause-record-pin pin}]}
   :active {:injury-id "injury-recorded-l1" :state :open}})

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

(deftest retry-is-enqueued-and-not-recursively-executed
  (let [env (harness/prepare-environment! (root))
        calls (atom {})
        result (runtime/run-opportunity!
                (opts env (harness/fake-ports (:repo env) calls
                                              :retry-queued)))]
    (is (= :awaiting-retry (:status result)))
    (is (= "offline-retry-click-1" (:retry-click result)))
    (is (= {:author 1 :review 1 :enact 1 :retry 1} @calls))
    (is (seq (:injuries
              (ledger/active-injuries
               (:ledger (store/load-ledger (:ledger-path env)))))))))

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

(deftest pinned-injury-evidence-produces-a-durable-diagnosis-input
  (let [dir (root)
        pin (recorded-injury-source! dir)
        {:keys [ledger active]} (durable-with-active-source pin)
        seen (atom nil)
        result (production/produce-diagnosis!
                {:meta-diagnosis-root (str dir "/diagnoses")
                 :meta-diagnosis-fn #(do (reset! seen %)
                                          {:status :succeeded
                                           :diagnosis {:class :configuration}})}
                {:ledger ledger} active)
        receipt (edn/read-string (slurp (get-in result [:diagnosis-evidence-pin :path])))]
    (is (= :available (:status result)))
    (is (= pin (:source-record-pin @seen)))
    (is (= "2026-10-19-bd1f106d-5bdc-4b9d-8350-5d9c9bd6832f"
           (get-in @seen [:injury-record :run/id])))
    (is (= :fold-output-invalid (get-in @seen [:injury-record :failure :kind])))
    (is (= (get-in result [:diagnosis-evidence-pin :sha256])
           (load-identity/sha256
            (java.nio.file.Files/readAllBytes
             (.toPath (java.io.File. (get-in result [:diagnosis-evidence-pin :path])))))))
    (is (= pin (:source-record-pin receipt)))
    (is (false? (:repair-claimed? receipt)))))

(deftest diagnosis-absence-and-bad-source-never-claim-a-repair-or-a-pin
  (let [dir (root)
        pin (recorded-injury-source! dir)
        {:keys [ledger active]} (durable-with-active-source pin)
        failed (production/produce-diagnosis!
                {:meta-diagnosis-fn (constantly {:status :failed :reason :offline})}
                {:ledger ledger} active)
        bad-pin (assoc pin :sha256 (apply str (repeat 64 "0")))
        bad (production/produce-diagnosis!
             {:meta-diagnosis-fn (fn [_] (throw (Exception. "must not call")))}
             {:ledger (:ledger (durable-with-active-source bad-pin))} active)]
    (is (= :meta-self-heal/diagnosis-unavailable (:reason failed)))
    (is (= :offline (:diagnosis-reason failed)))
    (is (nil? (:diagnosis-evidence-pin failed)))
    (is (false? (:repair-claimed? failed)))
    (is (= :meta-self-heal/diagnosis-source-pin-mismatch (:reason bad)))
    (is (nil? (:diagnosis-evidence-pin bad)))
    (is (false? (:repair-claimed? bad)))))

(deftest diagnosis-persistence-failure-is-not-a-durable-diagnosis
  (let [dir (root)
        pin (recorded-injury-source! dir)
        {:keys [ledger active]} (durable-with-active-source pin)
        result (with-redefs [production/persist-diagnosis!
                             (fn [& _] {:status :refused
                                        :reason :meta-self-heal/diagnosis-persist-failed})]
                 (production/produce-diagnosis!
                  {:meta-diagnosis-fn (constantly {:status :succeeded
                                                   :diagnosis {:class :configuration}})}
                  {:ledger ledger} active))]
    (is (= :meta-self-heal/diagnosis-persist-failed (:reason result)))
    (is (nil? (:diagnosis-evidence-pin result)))
    (is (false? (:repair-claimed? result)))))

(deftest distinct-diagnoses-for-one-injury-preserve-both-pinned-receipts
  (let [dir (root)
        pin (recorded-injury-source! dir)
        {:keys [ledger active]} (durable-with-active-source pin)
        calls (atom 0)
        diagnose (fn [_]
                   {:status :succeeded
                    :diagnosis {:class (if (= 1 (swap! calls inc))
                                          :configuration :operator)}})
        opts {:meta-diagnosis-root (str dir "/diagnoses")
              :meta-diagnosis-fn diagnose}
        first-result (production/produce-diagnosis! opts {:ledger ledger} active)
        first-bytes (java.nio.file.Files/readAllBytes
                     (.toPath (java.io.File.
                               (get-in first-result [:diagnosis-evidence-pin :path]))))
        second-result (production/produce-diagnosis! opts {:ledger ledger} active)
        second-bytes (java.nio.file.Files/readAllBytes
                      (.toPath (java.io.File.
                                (get-in second-result [:diagnosis-evidence-pin :path]))))]
    (is (= :available (:status first-result)))
    (is (= :available (:status second-result)))
    (is (not= (get-in first-result [:diagnosis-evidence-pin :path])
              (get-in second-result [:diagnosis-evidence-pin :path])))
    (is (= (get-in first-result [:diagnosis-evidence-pin :sha256])
           (load-identity/sha256 first-bytes)))
    (is (= (get-in second-result [:diagnosis-evidence-pin :sha256])
           (load-identity/sha256 second-bytes)))
    (is (= :configuration (:class (:diagnosis (edn/read-string (String. first-bytes "UTF-8"))))))
    (is (= :operator (:class (:diagnosis (edn/read-string (String. second-bytes "UTF-8"))))))))

(deftest invalid-source-and-nonresponses-are-unavailable-without-a-pin
  (let [dir (root)
        valid-pin (recorded-injury-source! dir)
        missing-pin {:path (str dir "/missing.edn")
                     :sha256 (apply str (repeat 64 "a"))}
        malformed-path (str dir "/malformed.edn")
        _ (spit malformed-path "{")
        malformed-pin {:path malformed-path
                       :sha256 (load-identity/sha256
                                (java.nio.file.Files/readAllBytes
                                 (.toPath (java.io.File. malformed-path))))}
        port-calls (atom 0)
        forbidden-port (fn [_] (swap! port-calls inc)
                         {:status :succeeded :diagnosis {:class :wrong}})
        missing (production/produce-diagnosis!
                 {:meta-diagnosis-fn forbidden-port}
                 {:ledger (:ledger (durable-with-active-source missing-pin))}
                 {:injury-id "injury-recorded-l1"})
        unreadable (production/produce-diagnosis!
                    {:meta-diagnosis-fn forbidden-port}
                    {:ledger (:ledger (durable-with-active-source malformed-pin))}
                    {:injury-id "injury-recorded-l1"})
        {:keys [ledger active]} (durable-with-active-source valid-pin)
        nil-response (production/produce-diagnosis!
                      {:meta-diagnosis-fn (constantly nil)} {:ledger ledger} active)
        malformed-response (production/produce-diagnosis!
                            {:meta-diagnosis-fn (constantly :not-a-map)}
                            {:ledger ledger} active)]
    (is (= :meta-self-heal/diagnosis-source-unreadable (:reason missing)))
    (is (= :meta-self-heal/diagnosis-source-unreadable (:reason unreadable)))
    (is (zero? @port-calls))
    (is (= :meta-self-heal/diagnosis-port-returned-nil (:reason nil-response)))
    (is (= :meta-self-heal/diagnosis-port-malformed (:reason malformed-response)))
    (is (every? nil? (map :diagnosis-evidence-pin
                          [missing unreadable nil-response malformed-response])))))

(deftest existing-diagnosis-pin-remains-a-non-invoking-compatibility-path
  (let [pin {:path "diagnoses/existing.edn" :sha256 (apply str (repeat 64 "a"))}
        result (production/produce-diagnosis!
                {:diagnosis-evidence-pin pin
                 :meta-diagnosis-fn (fn [_] (throw (Exception. "must not call")))}
                {} {:injury-id "injury-1"})]
    (is (= :present (:status result)))
    (is (= pin (:diagnosis-evidence-pin result)))))
