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

(defn- produced-diagnosis! [dir]
  (let [pin (recorded-injury-source! dir)
        {:keys [ledger active]} (durable-with-active-source pin)]
    (production/produce-diagnosis!
     {:meta-diagnosis-root (str dir "/diagnoses")
      :meta-diagnosis-fn (constantly {:status :succeeded
                                      :diagnosis {:class :configuration}})}
     {:ledger ledger} active)))

(deftest ordinary-readiness-retains-dedicated-input-absence
  ;; Historical input pin: wm-click-07653f2a was refused after the 08:19Z
  ;; active-injury ledger appeared; this fixture writes only an isolated ledger.
  (let [env (harness/prepare-environment! (root))
        ordinary (production/production-readiness
                  {:meta-injury-ledger-path (:ledger-path env)
                   :meta-self-heal/readiness-route :ordinary}
                  (production/diagnosis-context
                   {:meta-injury-ledger-path (:ledger-path env)}))
        dedicated (production/production-readiness
                   {:meta-injury-ledger-path (:ledger-path env)})]
    (is (= {:status :ready
            :self-heal {:status :absent
                         :absent-terms [:diagnosis-port
                                        :acceptance-proposal-port
                                        :acceptance-review-port]}}
           (select-keys ordinary [:status :self-heal])))
    (is (= :refused (:status dedicated)))
    (is (= :meta-self-heal/production-config-required (:reason dedicated)))))

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

(deftest verified-diagnosis-produces-an-unreviewed-pinned-acceptance-specification
  (let [dir (root)
        diagnosis (produced-diagnosis! dir)
        seen (atom nil)
        result (production/produce-acceptance!
                {:meta-acceptance-root (str dir "/acceptances")
                 :meta-acceptance-fn
                 (fn [request]
                   (reset! seen request)
                   {:status :proposed
                    :acceptance-test {:id "P17-reproduce-recorded-injury"
                                      :command ["bb" "checks/reproduce.clj"]
                                      :expected {:kind :configuration-injury}}})}
                diagnosis)
        bytes (java.nio.file.Files/readAllBytes
               (.toPath (java.io.File. (get-in result [:acceptance-evidence-pin :path]))))
        receipt (edn/read-string (String. bytes "UTF-8"))]
    (is (= :proposed (:status result)))
    (is (= (:diagnosis-evidence-pin diagnosis) (:diagnosis-evidence-pin @seen)))
    (is (= (:source-record-pin diagnosis) (:source-record-pin @seen)))
    (is (= :unreviewed (:review-status receipt)))
    (is (= :not-run (:result-status receipt)))
    (is (= (:acceptance-evidence-pin result)
           {:path (get-in result [:acceptance-evidence-pin :path])
            :sha256 (load-identity/sha256 bytes)}))
    (is (false? (:repair-claimed? result)))))

(deftest acceptance-rejects-prose-and-unverified-diagnosis-identities
  (let [dir (root)
        diagnosis (produced-diagnosis! dir)
        calls (atom 0)
        port (fn [_] (swap! calls inc) {:status :proposed :prose "fixed"})
        missing (production/produce-acceptance!
                 {:meta-acceptance-fn (fn [_] (throw (Exception. "must not call")))} {})
        failed (production/produce-acceptance!
                {:meta-acceptance-fn (constantly {:status :failed :reason :offline})} diagnosis)
        malformed (production/produce-acceptance!
                   {:meta-acceptance-fn (constantly :not-a-map)} diagnosis)
        prose (production/produce-acceptance! {:meta-acceptance-fn port} diagnosis)
        changed-source (production/produce-acceptance!
                        {:meta-acceptance-fn (fn [_] (throw (Exception. "must not call")))}
                        (assoc diagnosis :source-record-pin
                               {:path "changed" :sha256 (apply str (repeat 64 "a"))}))
        unreadable (production/produce-acceptance!
                    {:meta-acceptance-fn (fn [_] (throw (Exception. "must not call")))}
                    (assoc diagnosis :diagnosis-evidence-pin
                           {:path (str dir "/missing.edn")
                            :sha256 (apply str (repeat 64 "a"))}))
        drifted (production/produce-acceptance!
                 {:meta-acceptance-fn (fn [_] (throw (Exception. "must not call")))}
                 (assoc diagnosis :diagnosis-evidence-pin
                        (assoc (:diagnosis-evidence-pin diagnosis)
                               :sha256 (apply str (repeat 64 "0")))))]
    (is (= :meta-self-heal/acceptance-diagnosis-pin-unavailable (:reason missing)))
    (is (= :meta-self-heal/acceptance-unavailable (:reason failed)))
    (is (= :meta-self-heal/acceptance-port-malformed (:reason malformed)))
    (is (= :meta-self-heal/acceptance-prose-or-malformed (:reason prose)))
    (is (= :meta-self-heal/acceptance-diagnosis-identity-mismatch (:reason changed-source)))
    (is (= :meta-self-heal/acceptance-diagnosis-unreadable (:reason unreadable)))
    (is (= :meta-self-heal/acceptance-diagnosis-pin-mismatch (:reason drifted)))
    (is (= 1 @calls))
    (is (every? nil? (map :acceptance-evidence-pin
                          [missing failed malformed prose changed-source unreadable drifted])))))

(deftest acceptance-persistence-is-immutable-and-failure-never-claims-a-pin
  (let [dir (root)
        diagnosis (produced-diagnosis! dir)
        calls (atom 0)
        opts {:meta-acceptance-root (str dir "/acceptances")
              :meta-acceptance-fn
              (fn [_] {:status :proposed
                         :acceptance-test {:id (str "P17-" (swap! calls inc))
                                           :command ["bb" "checks/reproduce.clj"]
                                           :expected :configuration}})}
        first-result (production/produce-acceptance! opts diagnosis)
        first-bytes (java.nio.file.Files/readAllBytes
                     (.toPath (java.io.File.
                               (get-in first-result [:acceptance-evidence-pin :path]))))
        second-result (production/produce-acceptance! opts diagnosis)
        failed (with-redefs [production/persist-acceptance!
                             (fn [& _] {:status :refused
                                        :reason :meta-self-heal/acceptance-persist-failed})]
                 (production/produce-acceptance! opts diagnosis))]
    (is (not= (get-in first-result [:acceptance-evidence-pin :path])
              (get-in second-result [:acceptance-evidence-pin :path])))
    (is (= (get-in first-result [:acceptance-evidence-pin :sha256])
           (load-identity/sha256
            (java.nio.file.Files/readAllBytes
             (.toPath (java.io.File.
                       (get-in first-result [:acceptance-evidence-pin :path])))))))
    (is (= (seq first-bytes)
           (seq (java.nio.file.Files/readAllBytes
                 (.toPath (java.io.File.
                           (get-in first-result [:acceptance-evidence-pin :path])))))))
    (is (= :meta-self-heal/acceptance-persist-failed (:reason failed)))
    (is (nil? (:acceptance-evidence-pin failed)))))

(deftest supplied-acceptance-remains-a-non-invoking-compatibility-path
  (let [acceptance {:id "supplied" :command ["not-run"] :expected :unknown}
        result (production/produce-acceptance!
                {:acceptance-test acceptance
                 :meta-acceptance-fn (fn [_] (throw (Exception. "must not call")))}
                {})]
    (is (= :present (:status result)))
    (is (= acceptance (:acceptance-test result)))))

(deftest acceptance-admission-binds-the-exact-persisted-proposal
  (let [dir (root)
        diagnosis (produced-diagnosis! dir)
        proposal (production/produce-acceptance!
                  {:meta-acceptance-root (str dir "/acceptances")
                   :meta-acceptance-fn
                   (constantly {:status :proposed
                                :acceptance-test {:id "P22-admission"
                                                  :command ["not-run"] :expected :injury}})}
                  diagnosis)
        accept (fn [request]
                 (assoc (select-keys request [:injury-id :source-record-pin
                                              :diagnosis-evidence-pin
                                              :proposal-evidence-pin :acceptance-test])
                        :status :accepted))
        admitted (production/admit-acceptance!
                  {:meta-acceptance-root (str dir "/acceptances")
                   :meta-acceptance-review-fn accept} proposal)
        tampered (production/admit-acceptance!
                  {:meta-acceptance-review-fn accept}
                  (assoc-in proposal [:acceptance-test :id] "changed"))
        refused (production/admit-acceptance!
                 {:meta-acceptance-review-fn (constantly {:status :refused})}
                 proposal)]
    (is (= :accepted (:status admitted)))
    (is (= (:acceptance-evidence-pin proposal) (:proposal-evidence-pin admitted)))
    (is (= (:acceptance-test proposal) (:acceptance-test admitted)))
    (is (= :meta-self-heal/acceptance-proposal-identity-mismatch (:reason tampered)))
    (is (= :meta-self-heal/acceptance-review-refused (:reason refused)))
    (is (nil? (:acceptance-test tampered)))))

(deftest runtime-prepares-and-threads-only-a-durable-diagnosis-pin
  (let [dir (root)
        source-pin (recorded-injury-source! dir)
        context {:durable-ledger
                 {:status :loaded
                  :ledger {:entries [{:injury-id "injury-recorded-l1"
                                      :state :open :cause-record-pin source-pin}]}}
                 :active-injuries {:status :computed
                                   :injuries {"injury-recorded-l1"
                                              {:injury-id "injury-recorded-l1"
                                               :state :open}}}}
        no-active (assoc context :active-injuries {:status :computed :injuries {}})
        ready-opts (atom [])
        route-opts (atom [])
        diagnosis-calls (atom 0)
        readiness (fn [opts _] (swap! ready-opts conj opts) {:status :ready})
        route (fn [opts] (swap! route-opts conj opts) {:status :routed})
        available
        (with-redefs [production/diagnosis-context (constantly context)
                      production/production-readiness readiness
                      production/route! route]
          (runtime/run-opportunity!
           {:meta-diagnosis-root (str dir "/diagnoses")
            :meta-diagnosis-fn (fn [_] (swap! diagnosis-calls inc)
                                 {:status :succeeded :diagnosis {:class :configuration}})}))
        failed
        (with-redefs [production/diagnosis-context (constantly context)
                      production/production-readiness readiness
                      production/route! route]
          (runtime/run-opportunity!
           {:meta-diagnosis-fn (fn [_] (swap! diagnosis-calls inc)
                                 {:status :failed :reason :offline})}))
        supplied-pin {:path "diagnoses/supplied.edn"
                      :sha256 (apply str (repeat 64 "a"))}
        supplied
        (with-redefs [production/diagnosis-context (constantly context)
                      production/production-readiness readiness
                      production/route! route]
          (runtime/run-opportunity!
           {:diagnosis-evidence-pin supplied-pin
            :meta-diagnosis-fn (fn [_] (swap! diagnosis-calls inc)
                                 (throw (Exception. "must not call")))}))
        ordinary-seen (atom nil)
        no-active-result
        (with-redefs [production/diagnosis-context (constantly no-active)
                      production/production-readiness readiness
                      production/route! (constantly nil)]
          (with-redefs-fn
            {#'runtime/run-ordinary-opportunity!
             (fn [opts] (reset! ordinary-seen opts) {:status :ordinary})}
            #(runtime/run-opportunity! {:meta-diagnosis-fn
                                         (fn [_] (swap! diagnosis-calls inc)
                                           (throw (Exception. "must not call")))})))]
    (is (= :routed (:status available)))
    (is (= :available (get-in available [:diagnosis-observation :status])))
    (is (= (get-in available [:diagnosis-observation :diagnosis-evidence-pin])
           (:diagnosis-evidence-pin (first @route-opts))))
    (is (= :meta-self-heal/diagnosis-unavailable
           (get-in failed [:diagnosis-observation :reason])))
    (is (nil? (:diagnosis-evidence-pin (second @route-opts))))
    (is (= :present (get-in supplied [:diagnosis-observation :status])))
    (is (= supplied-pin (:diagnosis-evidence-pin (nth @route-opts 2))))
    (is (= 2 @diagnosis-calls))
    (is (= :ordinary (:status no-active-result)))
    (is (= :not-required (get-in no-active-result [:diagnosis-observation :status])))
    (is (nil? (:diagnosis-evidence-pin @ordinary-seen)))))

(deftest route-binds-produced-diagnosis-to-its-prepared-transaction
  (let [dir (root)
        ledger-path (str dir "/ledger.edn")
        _ (spit ledger-path "{}")
        source-a (recorded-injury-source! dir)
        source-b {:path "records/b.edn" :sha256 (apply str (repeat 64 "b"))}
        durable {:ledger {:entries [{:injury-id "injury-a" :state :open
                                     :cause-record-pin source-a}]}}
        active-a {:injury-id "injury-a" :state :open}
        produced (production/produce-diagnosis!
                  {:meta-diagnosis-root (str dir "/diagnoses")
                   :meta-diagnosis-fn (constantly {:status :succeeded
                                                    :diagnosis {:class :configuration}})}
                  durable active-a)
        diagnosis-a (:diagnosis-evidence-pin produced)
        diagnosis-b {:path "diagnoses/b.edn" :sha256 (apply str (repeat 64 "d"))}
        transaction-b {:work-order {:injury-id "injury-b"
                                    :cause-record-pin source-b
                                    :diagnosis-evidence-pin diagnosis-b}}
        transaction-a {:work-order {:injury-id "injury-a"
                                    :cause-record-pin source-a
                                    :diagnosis-evidence-pin diagnosis-a
                                    :acceptance-test {:id "P17-absent-but-shaped"
                                                      :command ["not-run"]
                                                      :expected :unavailable}}}
        base-opts {:meta-injury-ledger-path ledger-path
                   :meta-repair-transaction-path (str dir "/transaction.edn")
                   :meta-repair-ports {:author (constantly nil)}
                   :acceptance-test {:id "P17-absent-but-shaped"
                                     :command ["not-run"] :expected :unavailable}
                   :diagnosis-evidence-pin diagnosis-a
                   :meta-diagnosis-observation produced
                   :meta-acceptance-observation
                   {:status :accepted :injury-id "injury-a"
                    :source-record-pin source-a
                    :diagnosis-evidence-pin diagnosis-a
                    :acceptance-test {:id "P17-absent-but-shaped"
                                      :command ["not-run"] :expected :unavailable}}}
        effects (atom 0)
        active-b {:status :computed :injuries {"injury-b" {:injury-id "injury-b"}}}
        run-route (fn [active transaction]
                    (with-redefs [store/load-durable-ledger (constantly {:status :loaded :ledger {}})
                                  ledger/active-injuries (constantly active)
                                  coordinator/prepare (constantly {:transaction transaction})
                                  coordinator/coordinate (fn [& _] (swap! effects inc)
                                                           (throw (ex-info "coordinate" {})))]
                      (production/route! base-opts)))]
    (let [mismatch (run-route active-b transaction-b)]
      (is (= :meta-self-heal/diagnosis-transaction-mismatch (:reason mismatch)))
      (is (= "injury-a" (get-in mismatch [:expected :injury-id])))
      (is (= "injury-b" (get-in mismatch [:actual :injury-id])))
      (is (zero? @effects)))
    (let [positive (try
                     (run-route {:status :computed
                                 :injuries {"injury-a" {:injury-id "injury-a"}}}
                                transaction-a)
                     :returned
                     (catch clojure.lang.ExceptionInfo e
                       (if (= "coordinate" (.getMessage e)) :coordinated :wrong)))]
      (is (= :coordinated positive))
      (is (= 1 @effects)))
    (let [resumed (with-redefs [store/load-durable-ledger (constantly {:status :loaded :ledger {}})
                                ledger/active-injuries
                                (constantly {:status :computed
                                             :injuries {"injury-a" {:injury-id "injury-a"}}})
                                coordinator/coordinate (fn [& _] (swap! effects inc)
                                                         (throw (ex-info "coordinate" {})))]
                    (with-redefs-fn {#'production/load-transaction
                                     (constantly transaction-b)}
                      #(production/route! base-opts)))]
      (is (= :meta-self-heal/diagnosis-transaction-mismatch (:reason resumed)))
      (is (= "injury-a" (get-in resumed [:expected :injury-id])))
      (is (= "injury-b" (get-in resumed [:actual :injury-id])))
      (is (= 1 @effects)))))

(deftest absent-ledger-is-not-required-while-unreadable-ledger-is-unavailable
  (is (= :not-required
         (:status (production/prepare-diagnosis {} {:durable-ledger nil
                                                     :active-injuries nil}))))
  (is (= :unavailable
         (:status (production/prepare-diagnosis
                   {} {:durable-ledger {:status :refused}
                       :active-injuries nil})))))

(deftest runtime-admits-only-an-independently-reviewed-pinned-acceptance
  (let [dir (root)
        source-pin (recorded-injury-source! dir)
        context {:durable-ledger
                 {:status :loaded
                  :ledger {:entries [{:injury-id "injury-recorded-l1" :state :open
                                      :cause-record-pin source-pin}]}}
                 :active-injuries {:status :computed
                                   :injuries {"injury-recorded-l1"
                                              {:injury-id "injury-recorded-l1" :state :open}}}}
        no-active (assoc context :active-injuries {:status :computed :injuries {}})
        readiness-opts (atom [])
        diagnosis-calls (atom 0)
        acceptance-calls (atom 0)
        refused-readiness (fn [opts _]
                            (swap! readiness-opts conj opts)
                            {:status :refused :reason :original-readiness})
        proposed
        (with-redefs [production/diagnosis-context (constantly context)
                      production/production-readiness refused-readiness]
          (runtime/run-opportunity!
           {:meta-diagnosis-root (str dir "/diagnoses")
            :meta-acceptance-root (str dir "/acceptances")
            :meta-diagnosis-fn (fn [_] (swap! diagnosis-calls inc)
                                 {:status :succeeded :diagnosis {:class :configuration}})
            :meta-acceptance-fn (fn [_] (swap! acceptance-calls inc)
                                  {:status :proposed
                                   :acceptance-test {:id "P17-runtime"
                                                     :command ["not-run"]
                                                     :expected :configuration}})
            :meta-acceptance-review-fn
            (fn [request]
              (assoc (select-keys request [:injury-id :source-record-pin
                                           :diagnosis-evidence-pin
                                           :proposal-evidence-pin :acceptance-test])
                     :status :accepted))}))
        supplied-test {:id "already-reviewed" :command ["not-run"] :expected :unknown}
        supplied
        (with-redefs [production/diagnosis-context (constantly context)
                      production/production-readiness refused-readiness]
          (runtime/run-opportunity!
           {:diagnosis-evidence-pin {:path "diagnoses/supplied.edn"
                                     :sha256 (apply str (repeat 64 "a"))}
            :acceptance-test supplied-test
            :meta-diagnosis-fn (fn [_] (swap! diagnosis-calls inc)
                                 (throw (Exception. "must not call")))
            :meta-acceptance-fn (fn [_] (swap! acceptance-calls inc)
                                  (throw (Exception. "must not call")))}))
        failed
        (with-redefs [production/diagnosis-context (constantly context)
                      production/production-readiness refused-readiness]
          (runtime/run-opportunity!
           {:meta-diagnosis-root (str dir "/diagnoses-failed")
            :meta-diagnosis-fn (fn [_] (swap! diagnosis-calls inc)
                                 {:status :succeeded :diagnosis {:class :configuration}})
            :meta-acceptance-fn (fn [_] (swap! acceptance-calls inc)
                                  {:status :failed :reason :offline})}))
        ordinary-calls (atom 0)
        no-active-result
        (with-redefs [production/diagnosis-context (constantly no-active)
                      production/production-readiness (fn [_ _] {:status :ready})
                      production/route! (constantly nil)]
          (with-redefs-fn {#'runtime/run-ordinary-opportunity!
                           (fn [_] (swap! ordinary-calls inc) {:status :ordinary})}
            #(runtime/run-opportunity! {:meta-acceptance-fn
                                         (fn [_] (swap! acceptance-calls inc)
                                           (throw (Exception. "must not call")))})))
        retry-result
        (with-redefs [production/attributable-retry-readiness (fn [& _] {:status :ready})]
          (with-redefs-fn {#'runtime/run-ordinary-opportunity!
                           (fn [_] (swap! ordinary-calls inc) {:status :ordinary})}
            #(runtime/run-opportunity! {:meta-repair-retry {:retry-click "retry"}
                                         :meta-acceptance-fn
                                         (fn [_] (swap! acceptance-calls inc)
                                           (throw (Exception. "must not call")))})))]
    (is (= :original-readiness (:reason proposed)))
    (is (= :accepted (get-in proposed [:acceptance-observation :status])))
    (is (some? (get-in proposed [:acceptance-observation :acceptance-evidence-pin])))
    (is (= "P17-runtime" (get-in (first @readiness-opts) [:acceptance-test :id])))
    (is (= :accepted (get-in proposed [:acceptance-observation :review-status])))
    (is (= supplied-test (:acceptance-test (second @readiness-opts))))
    (is (= :present (get-in supplied [:acceptance-observation :status])))
    (is (= :original-readiness (:reason failed)))
    (is (= :meta-self-heal/acceptance-unavailable
           (get-in failed [:acceptance-observation :reason])))
    (is (nil? (get-in failed [:acceptance-observation :acceptance-evidence-pin])))
    (is (= :not-required (get-in no-active-result [:acceptance-observation :status])))
    (is (= :not-required (get-in retry-result [:acceptance-observation :status])))
    (is (= 2 @diagnosis-calls))
    (is (= 2 @acceptance-calls))
    (is (= 2 @ordinary-calls))))
