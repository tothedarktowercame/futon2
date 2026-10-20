(ns futon2.aif.meta-self-heal-production-test
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
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
          :meta-acceptance-run-fn
          (constantly {:status :ran :outcome :passed :exit 0 :summary "fixture pass"
                       :run-pin {:path "fixture://acceptance-run"
                                 :sha256 (apply str (repeat 64 "a"))}})
          :meta-repair-ports ports}))

(defn- acceptance-case
  ([id] (acceptance-case id :configuration))
  ([id expected]
   (let [namespace "futon2.aif.meta-selection-contract-acceptance-test"]
     {:id id
      :command ["clojure" "-M:test" "-n" namespace]
      :expected expected
      :source {:namespace namespace
               :path "test/futon2/aif/meta_selection_contract_acceptance_test.clj"
               :text (str "(ns " namespace "\n  (:require [clojure.test :refer [deftest is]]))\n"
                          "(deftest acceptance-control (is true))\n")}})))

(def ^:private repository-root (System/getProperty "user.dir"))

(def ^:private production-functions
  ;; Real futon2 vars, one per role the click wm-click-fca1c921 author seat
  ;; listed as missing (F11).
  [{:role :selection-entrypoint
    :var "futon2.aif.meta-live-outer-selector/select-live"
    :file "src/futon2/aif/meta_live_outer_selector.clj"}
   {:role :required-loop-node-evaluator
    :var "futon2.aif.wm.loop-node-exercise/receipt"
    :file "src/futon2/aif/wm/loop_node_exercise.clj"}
   {:role :terminal-routing-entrypoint
    :var "futon2.aif.full-loop-runner/persist-run-record!"
    :file "src/futon2/aif/full_loop_runner.clj"}])

(defn- diagnosis-fixture [class]
  {:class class :production-functions production-functions})

(defn- diagnosis-fixture-with-entry [index changes]
  {:class :configuration
   :production-functions (update production-functions index merge changes)})

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

(defn- produced-diagnosis!
  ([dir] (produced-diagnosis! dir (diagnosis-fixture :configuration)))
  ([dir diagnosis-map]
   (let [pin (recorded-injury-source! dir)
         {:keys [ledger active]} (durable-with-active-source pin)]
     (production/produce-diagnosis!
      {:meta-diagnosis-root (str dir "/diagnoses")
       :meta-diagnosis-fn (constantly {:status :succeeded
                                       :diagnosis diagnosis-map})}
      {:ledger ledger} active))))

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
                         :absent-terms [:acceptance-run-port
                                        :diagnosis-port
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
              :author)
        order {:injury-id "injury-1" :repair-id "repair-1" :attempt 1
               :author "wm-author" :author-session "fresh-author"}
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
                                           :diagnosis (diagnosis-fixture :configuration)})}
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
                                                   :diagnosis (diagnosis-fixture :configuration)})}
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
                    :diagnosis (diagnosis-fixture
                                (if (= 1 (swap! calls inc))
                                  :configuration :operator))})
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

;; F11 (click wm-click-fca1c921): the author seat refused because the
;; diagnosis named no callable production functions.
(defn- diagnose-with [dir diagnosis]
  (let [pin (recorded-injury-source! dir)
        {:keys [ledger active]} (durable-with-active-source pin)]
    (production/produce-diagnosis!
     {:meta-diagnosis-root (str dir "/diagnoses")
      :meta-repository-root repository-root
      :meta-diagnosis-fn (constantly {:status :succeeded :diagnosis diagnosis})}
     {:ledger ledger} active)))

(defn- persisted-diagnoses [dir]
  (seq (.listFiles (java.io.File. (str dir "/diagnoses")))))

(deftest diagnosis-naming-resolvable-production-functions-is-available
  (let [dir (root)
        result (diagnose-with dir (diagnosis-fixture :configuration))
        receipt (edn/read-string (slurp (get-in result [:diagnosis-evidence-pin :path])))]
    (is (= :available (:status result)))
    (is (= production-functions (get-in receipt [:diagnosis :production-functions])))))

(deftest diagnosis-without-verified-production-functions-is-refused-unpersisted
  (let [ghost "futon2.aif.meta-live-outer-selector/no-such-selector"
        absent (diagnose-with (root) {:class :configuration})
        unresolvable-dir (root)
        unresolvable (diagnose-with
                      unresolvable-dir
                      (diagnosis-fixture-with-entry 0 {:var ghost}))
        role-dir (root)
        missing-role (diagnose-with
                      role-dir
                      {:class :configuration
                       :production-functions (vec (take 2 production-functions))})
        escaped (diagnose-with
                 (root)
                 (diagnosis-fixture-with-entry
                  2 {:file "../elsewhere/src/futon2/aif/full_loop_runner.clj"}))]
    (is (= :meta-self-heal/diagnosis-production-functions-absent (:reason absent)))
    (is (= :absent (:production-functions-field absent)))
    (is (= production/required-production-roles (:missing-roles absent)))
    (is (= :meta-self-heal/diagnosis-production-functions-absent (:reason unresolvable)))
    (is (= [ghost] (:unresolvable-vars unresolvable)))
    (is (= [:selection-entrypoint] (:missing-roles unresolvable)))
    (is (nil? (persisted-diagnoses unresolvable-dir)))
    (is (= :meta-self-heal/diagnosis-production-functions-absent (:reason missing-role)))
    (is (= [:terminal-routing-entrypoint] (:missing-roles missing-role)))
    (is (nil? (:unresolvable-vars missing-role)))
    (is (nil? (persisted-diagnoses role-dir)))
    (is (= ["../elsewhere/src/futon2/aif/full_loop_runner.clj"] (:missing-files escaped)))
    (is (= [:terminal-routing-entrypoint] (:missing-roles escaped)))
    (is (every? nil? (map :diagnosis-evidence-pin [absent unresolvable missing-role escaped])))
    (is (every? false? (map :repair-claimed? [absent unresolvable missing-role escaped])))))

(deftest acceptance-request-carries-the-diagnosis-production-functions
  (let [dir (root)
        diagnosis (diagnose-with dir (diagnosis-fixture :configuration))
        seen (atom nil)
        result (production/produce-acceptance!
                {:meta-acceptance-root (str dir "/acceptances")
                 :meta-repository-root repository-root
                 :meta-acceptance-fn (fn [request] (reset! seen request)
                                       {:status :failed :reason :stub})}
                diagnosis)]
    (is (= :meta-self-heal/acceptance-unavailable (:reason result)))
    ;; F12 adds :resolvable?/:arglists/:doc to each verified entry.
    (is (= production-functions
           (map #(select-keys % [:role :var :file])
                (get-in @seen [:diagnosis :production-functions]))))))

(deftest pinned-receipt-without-production-functions-never-reaches-the-author
  ;; The shape of diagnosis-83b348..., pinned before F11.
  (let [dir (root)
        source-pin (recorded-injury-source! dir)
        receipt {:schema :wm/meta-diagnosis-receipt-v1 :status :available
                 :injury-id "injury-recorded-l1" :source-record-pin source-pin
                 :diagnosis {:class :configuration} :repair-claimed? false}
        written (production/persist-diagnosis! (str dir "/diagnoses/old.edn") receipt)
        calls (atom 0)
        result (production/produce-acceptance!
                {:meta-repository-root repository-root
                 :meta-acceptance-fn (fn [_] (swap! calls inc) {:status :failed})}
                {:injury-id "injury-recorded-l1" :source-record-pin source-pin
                 :diagnosis-evidence-pin (:diagnosis-evidence-pin written)})]
    (is (= :meta-self-heal/diagnosis-production-functions-absent (:reason result)))
    (is (= production/required-production-roles (:missing-roles result)))
    (is (zero? @calls))))

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
                    :acceptance-test
                    (acceptance-case "P17-reproduce-recorded-injury"
                                     {:kind :configuration-injury})})}
                diagnosis)
        bytes (java.nio.file.Files/readAllBytes
               (.toPath (java.io.File. (get-in result [:acceptance-evidence-pin :path]))))
        receipt (edn/read-string (String. bytes "UTF-8"))]
    (is (= :proposed (:status result)))
    (is (= (:diagnosis-evidence-pin diagnosis) (:diagnosis-evidence-pin @seen)))
    (is (= (:source-record-pin diagnosis) (:source-record-pin @seen)))
    (is (= :unreviewed (:review-status receipt)))
    (is (= :not-run (:result-status receipt)))
    (is (= (get-in result [:acceptance-test :source])
           (get-in receipt [:acceptance-test :source])))
    (is (= (:acceptance-evidence-pin result)
           {:path (get-in result [:acceptance-evidence-pin :path])
            :sha256 (load-identity/sha256 bytes)}))
    (is (false? (:repair-claimed? result)))))

(def ^:private documented-function
  ;; An extra, well-formed entry whose var has a docstring, so :doc is exercised.
  {:role :runtime-entrypoint
   :var "futon2.aif.meta-self-heal-production/produce-acceptance!"
   :file "src/futon2/aif/meta_self_heal_production.clj"})

(deftest acceptance-request-binds-what-a-no-stub-test-needs
  ;; F12: replay 2 (2026-10-19 18:40Z) and click wm-click-fca1c921.
  (let [dir (root)
        diagnosis (produced-diagnosis!
                   dir {:class :configuration
                        :production-functions (conj production-functions
                                                    documented-function)})
        seen (atom nil)
        result (production/produce-acceptance!
                {:meta-acceptance-root (str dir "/acceptances")
                 :meta-injury-ledger-path "/some/ledger.edn"
                 :meta-acceptance-fn
                 (fn [request]
                   (reset! seen request)
                   {:status :proposed
                    :acceptance-test (acceptance-case "F12-enrichment")})}
                diagnosis)
        request @seen
        functions (get-in request [:diagnosis :production-functions])
        receipt (edn/read-string
                 (slurp (get-in result [:acceptance-evidence-pin :path])))]
    (is (= :proposed (:status result)))
    (is (= 4 (count functions)))
    (is (every? :resolvable? functions))
    (is (every? #(string? (:arglists %)) functions))
    (is (string? (:doc (last functions))))
    (is (every? #(<= (count (:doc %)) 700) functions))
    (is (= (map :var production-functions) (map :var (take 3 functions))))
    (let [entries (get-in request [:test-harness :entrypoints])]
      (is (= ["futon2.aif.meta-self-heal-harness/prepare-environment!"
              "futon2.test-support.runner-fixture/isolated-runner-opts"
              "futon2.test-support.runner-fixture/with-hermetic-traces"
              "futon2.test-support.runner-fixture/with-loop-exercise-evidence"]
             (mapv :var entries)))
      (is (every? :file entries))
      (is (string? (:arglists (first entries)))))
    (is (= 3 (count (get-in request [:test-harness :examples]))))
    (is (= {:source-record (:source-record-pin diagnosis)
            :injury-ledger "/some/ledger.edn"
            :note "read-only in the test; isolated copies via the harness"}
           (:real-input-authority request)))
    (is (str/includes? (get-in request [:persistence-destination :note])
                       "canonical data/ and src/ are read-only"))
    (is (= "/home/joe/code/futon2" (:repository request)))
    (is (= (select-keys request [:test-harness :real-input-authority
                                 :persistence-destination :repository])
           (dissoc (:request-enrichment receipt) :production-functions)))
    (is (= functions (:production-functions (:request-enrichment receipt))))))

(deftest acceptance-request-records-unresolvable-harness-entries
  (with-redefs [production/test-harness
                (update production/test-harness :entrypoints conj
                        {:role :x :var "no.such.ns/nothing" :file "test/x.clj"})]
    (let [dir (root)
          seen (atom nil)]
      (production/produce-acceptance!
       {:meta-acceptance-root (str dir "/acceptances")
        :meta-repository-root repository-root
        :meta-acceptance-fn (fn [r] (reset! seen r) {:status :failed})}
       (produced-diagnosis! dir))
      (let [entry (last (get-in @seen [:test-harness :entrypoints]))]
        (is (false? (:resolvable? entry)))
        (is (string? (:note entry))))
      (is (= repository-root (:repository @seen))))))

(deftest unresolvable-production-function-is-refused-before-the-port
  ;; Two layers.  F11a's validation fires first when a diagnosis names an
  ;; invented var: it is refused at diagnosis time and never pinned.  F12's own
  ;; refusal fires only when a var that F11a verified no longer resolves when
  ;; the request is built; the second half forces that with a redef of the
  ;; request-time resolver.
  (let [dir (root)
        invented (produced-diagnosis!
                  dir {:class :configuration
                       :production-functions
                       (conj production-functions
                             {:role :x :var "no.such.ns/invented-fn" :file "src/x.clj"})})]
    (is (= :meta-self-heal/diagnosis-production-functions-absent (:reason invented)))
    (is (= ["no.such.ns/invented-fn"] (:unresolvable-vars invented))))
  (let [dir (root)
        calls (atom 0)
        diagnosis (produced-diagnosis! dir)
        dropped "futon2.aif.full-loop-runner/persist-run-record!"
        result (with-redefs-fn
                 {#'production/resolve-var
                  (fn [v] (when-not (= v dropped) (requiring-resolve (symbol v))))}
                 #(production/produce-acceptance!
                   {:meta-acceptance-root (str dir "/acceptances")
                    :meta-acceptance-fn (fn [_] (swap! calls inc) {:status :proposed})}
                   diagnosis))]
    (is (= :refused (:status result)))
    (is (= :meta-self-heal/acceptance-request-function-unresolvable (:reason result)))
    (is (= [dropped] (:unresolvable result)))
    (is (zero? @calls))
    (is (false? (:repair-claimed? result)))))

(deftest acceptance-rejects-prose-and-unverified-diagnosis-identities
  (let [dir (root)
        diagnosis (produced-diagnosis! dir)
        calls (atom 0)
        port (fn [_] (swap! calls inc) {:status :proposed :prose "fixed"})
        missing (production/produce-acceptance!
                 {:meta-acceptance-fn (fn [_] (throw (Exception. "must not call")))} {})
        ;; F10: a typed refusal from the producer (its :failure, and a
        ;; transport :detail) travels onto the record.
        failed (production/produce-acceptance!
                {:meta-acceptance-fn
                 (constantly {:status :failed :reason :offline
                              :failure {:kind :production-function-identity-absent
                                        :missing {:callable-production-vars [:selection-entrypoint]}}
                              :detail "Exit 1: exceeded retry limit, last status: 429 Too Many Requests"
                              ;; F20: the reader's error, the one informed
                              ;; re-ask and the one transport retry travel too.
                              :read-error "EOF while reading"
                              :read-retry {:first-error "read a symbol, not a map with keyword keys"
                                           :first-preview "I have everything verified…"}
                              :transport-retry {:status 429 :waited-ms 0}})}
                diagnosis)
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
    (is (= :failed (:acceptance-status failed)))
    (is (= :offline (:acceptance-reason failed)))
    (is (= {:kind :production-function-identity-absent
            :missing {:callable-production-vars [:selection-entrypoint]}}
           (:acceptance-failure failed)))
    (is (= "Exit 1: exceeded retry limit, last status: 429 Too Many Requests"
           (:acceptance-detail failed)))
    (is (= "EOF while reading" (:acceptance-read-error failed))
        "F20: deleting the :acceptance-read-error copy makes this fail")
    (is (= {:first-error "read a symbol, not a map with keyword keys"
            :first-preview "I have everything verified…"}
           (:acceptance-read-retry failed)))
    (is (= {:status 429 :waited-ms 0} (:acceptance-transport-retry failed)))
    (is (not (contains? (production/produce-acceptance!
                         {:meta-acceptance-fn (constantly {:status :failed :reason :offline})}
                         diagnosis)
                        :acceptance-read-retry))
        "an answer that needed no re-ask carries no retry key")
    (is (= :meta-self-heal/acceptance-port-malformed (:reason malformed)))
    (is (= :meta-self-heal/acceptance-prose-or-malformed (:reason prose)))
    (is (= :meta-self-heal/acceptance-diagnosis-identity-mismatch (:reason changed-source)))
    (is (= :meta-self-heal/acceptance-diagnosis-unreadable (:reason unreadable)))
    (is (= :meta-self-heal/acceptance-diagnosis-pin-mismatch (:reason drifted)))
    (is (= 1 @calls))
    (is (every? nil? (map :acceptance-evidence-pin
                          [missing failed malformed prose changed-source unreadable drifted])))))

(deftest acceptance-source-is-required-and-namespace-path-must-match-command
  (let [dir (root)
        diagnosis (produced-diagnosis! dir)
        persisted (atom 0)
        missing-case (dissoc (acceptance-case "missing-source") :source)
        mismatched-case (assoc-in (acceptance-case "mismatched-source")
                                  [:source :path]
                                  "test/futon2/aif/not_the_command_namespace.clj")
        produce (fn [acceptance-test]
                  (with-redefs [production/persist-acceptance!
                                (fn [& _] (swap! persisted inc)
                                  {:status :persisted})]
                    (production/produce-acceptance!
                     {:meta-acceptance-fn
                      (constantly {:status :proposed
                                   :acceptance-test acceptance-test})}
                     diagnosis)))
        ;; A namespace with an unsafe segment ("../x") has no test path under
        ;; the shared segment rule (F6b derives the same path in the run port),
        ;; even when its :path is spelled to match a naive derivation.
        unsafe-case (-> (acceptance-case "unsafe-namespace")
                        (assoc :command ["clojure" "-M:test" "-n" "../x"])
                        (assoc :source {:namespace "../x" :path "test///x.clj"
                                        :text "(ns x)"}))
        missing (produce missing-case)
        mismatched (produce mismatched-case)
        unsafe (produce unsafe-case)]
    (is (= :meta-self-heal/acceptance-prose-or-malformed (:reason missing)))
    (is (= :meta-self-heal/acceptance-proposal-source-mismatch
           (:reason mismatched)))
    (is (= :meta-self-heal/acceptance-proposal-source-mismatch (:reason unsafe)))
    (is (nil? (:acceptance-evidence-pin missing)))
    (is (nil? (:acceptance-evidence-pin mismatched)))
    (is (nil? (:acceptance-evidence-pin unsafe)))
    (is (zero? @persisted))))

(deftest historical-source-less-proposal-never-runs-or-reaches-review
  (let [dir (root)
        diagnosis (produced-diagnosis! dir)
        valid (production/produce-acceptance!
               {:meta-acceptance-root (str dir "/acceptances")
                :meta-acceptance-fn
                (constantly {:status :proposed
                             :acceptance-test (acceptance-case "historical-control")})}
               diagnosis)
        old-receipt (-> (edn/read-string
                         (slurp (get-in valid [:acceptance-evidence-pin :path])))
                        (update :acceptance-test dissoc :source))
        old-path (str dir "/historical-source-less.edn")
        old-bytes (.getBytes (str (pr-str old-receipt) "\n") "UTF-8")
        _ (spit old-path (String. old-bytes "UTF-8"))
        historical (-> valid
                       (update :acceptance-test dissoc :source)
                       (assoc :acceptance-evidence-pin
                              {:path old-path :sha256 (load-identity/sha256 old-bytes)}))
        calls (atom [])
        refused (production/admit-acceptance!
                 {:meta-acceptance-run-fn #(swap! calls conj [:run %])
                  :meta-acceptance-review-fn #(swap! calls conj [:review %])}
                 historical)]
    (is (= :meta-self-heal/acceptance-proposal-not-concrete (:reason refused)))
    (is (empty? @calls))))

(deftest acceptance-persistence-is-immutable-and-failure-never-claims-a-pin
  (let [dir (root)
        diagnosis (produced-diagnosis! dir)
        calls (atom 0)
        opts {:meta-acceptance-root (str dir "/acceptances")
              :meta-acceptance-fn
              (fn [_] {:status :proposed
                         :acceptance-test
                         (acceptance-case (str "P17-" (swap! calls inc)))})}
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
  (let [acceptance (acceptance-case "supplied" :unknown)
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
                                :acceptance-test
                                (acceptance-case "P22-admission" :injury)})}
                  diagnosis)
        run-result {:status :ran :outcome :failed :exit 1
                    :summary "reproduced fold-output-invalid"
                    :run-pin {:path "test://acceptance-before-repair"
                              :sha256 (apply str (repeat 64 "b"))}}
        seen-review (atom nil)
        run-calls (atom [])
        run-once (fn [acceptance-test]
                   (swap! run-calls conj acceptance-test)
                   run-result)
        accept (fn [request]
                 (reset! seen-review request)
                 (assoc (select-keys request [:injury-id :source-record-pin
                                              :diagnosis-evidence-pin
                                              :proposal-evidence-pin :acceptance-test])
                        :status :accepted))
        admitted (production/admit-acceptance!
                  {:meta-acceptance-root (str dir "/acceptances")
                   :meta-acceptance-run-fn run-once
                   :meta-acceptance-review-fn accept} proposal)
        tampered (production/admit-acceptance!
                  {:meta-acceptance-run-fn (constantly run-result)
                   :meta-acceptance-review-fn accept}
                  (assoc-in proposal [:acceptance-test :id] "changed"))
        refused (production/admit-acceptance!
                 {:meta-acceptance-run-fn (constantly run-result)
                  :meta-acceptance-review-fn
                  (constantly {:status :refused
                               :reason/type :acceptance-test-still-fails})}
                 proposal)
        review-calls (atom 0)
        missing-run (production/admit-acceptance!
                     {:meta-acceptance-review-fn
                      (fn [_] (swap! review-calls inc) {:status :accepted})}
                     proposal)
        ;; F16: a run that passes before any repair does not pin the injury;
        ;; refused typed, and the reviewer is never asked.
        passing-run (production/admit-acceptance!
                     {:meta-acceptance-run-fn (constantly (assoc run-result :outcome :passed :exit 0))
                      :meta-acceptance-review-fn
                      (fn [_] (swap! review-calls inc) {:status :accepted})}
                     proposal)
        ;; A run port that answers with a shape the protocol does not accept
        ;; (no outcome, exit, summary or pin) is a typed refusal before review.
        malformed-run (production/admit-acceptance!
                       {:meta-acceptance-run-fn (constantly {:status :ran})
                        :meta-acceptance-review-fn
                        (fn [_] (swap! review-calls inc) {:status :accepted})}
                       proposal)]
    (is (= :accepted (:status admitted)))
    (is (= (:acceptance-evidence-pin proposal) (:proposal-evidence-pin admitted)))
    (is (= (:acceptance-test proposal) (:acceptance-test admitted)))
    (is (= :ran-before-repair (:result-status @seen-review)))
    (is (= run-result (:result @seen-review)))
    (is (= run-result (:result admitted)))
    (is (= (get-in proposal [:acceptance-test :source])
           (get-in @seen-review [:acceptance-test :source])))
    (is (= [(get proposal :acceptance-test)] @run-calls))
    (is (= :meta-self-heal/acceptance-proposal-identity-mismatch (:reason tampered)))
    (is (= :meta-self-heal/acceptance-review-refused (:reason refused)))
    (is (= :acceptance-test-still-fails (:review-reason refused)))
    (is (= [(:acceptance-test proposal)] @run-calls)
        "the run port is called exactly once, with the proposal's own acceptance test")
    (is (= :meta-self-heal/acceptance-run-port-unavailable (:reason missing-run)))
    (is (= :meta-self-heal/acceptance-test-passes-before-repair (:reason passing-run)))
    (is (= :passed (get-in passing-run [:acceptance-result :outcome])))
    (is (= :meta-self-heal/acceptance-run-refused (:reason malformed-run)))
    (is (= {:status :ran} (:acceptance-result malformed-run)))
    (is (zero? @review-calls)
        "neither a missing port nor a malformed run result reaches the reviewer")
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
                                 {:status :succeeded :diagnosis (diagnosis-fixture :configuration)})}))
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
                                                    :diagnosis (diagnosis-fixture :configuration)})}
                  durable active-a)
        diagnosis-a (:diagnosis-evidence-pin produced)
        diagnosis-b {:path "diagnoses/b.edn" :sha256 (apply str (repeat 64 "d"))}
        transaction-b {:work-order {:injury-id "injury-b"
                                    :cause-record-pin source-b
                                    :diagnosis-evidence-pin diagnosis-b}}
        transaction-a {:work-order {:injury-id "injury-a"
                                    :cause-record-pin source-a
                                    :diagnosis-evidence-pin diagnosis-a
                                    :acceptance-test
                                    (acceptance-case "P17-absent-but-shaped"
                                                     :unavailable)}}
        base-opts {:meta-injury-ledger-path ledger-path
                   :meta-repair-transaction-path (str dir "/transaction.edn")
                   :meta-repair-ports {:author (constantly nil)}
                   :meta-acceptance-run-fn
                   (constantly {:status :ran :outcome :failed :exit 1
                                :summary "fixture injury reproduced"
                                :run-pin {:path "fixture://route-acceptance"
                                          :sha256 (apply str (repeat 64 "e"))}})
                   :acceptance-test
                   (acceptance-case "P17-absent-but-shaped" :unavailable)
                   :diagnosis-evidence-pin diagnosis-a
                   :meta-diagnosis-observation produced
                   :meta-acceptance-observation
                   {:status :accepted :injury-id "injury-a"
                    :source-record-pin source-a
                    :diagnosis-evidence-pin diagnosis-a
                    :acceptance-test
                    (acceptance-case "P17-absent-but-shaped" :unavailable)}}
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
                                 {:status :succeeded :diagnosis (diagnosis-fixture :configuration)})
            :meta-acceptance-fn (fn [_] (swap! acceptance-calls inc)
                                  {:status :proposed
                                   :acceptance-test
                                   (acceptance-case "P17-runtime")})
            :meta-acceptance-run-fn
            (constantly {:status :ran :outcome :failed :exit 1
                         :summary "runtime reproduced injury"
                         :run-pin {:path "fixture://runtime-acceptance"
                                   :sha256 (apply str (repeat 64 "f"))}})
            :meta-acceptance-review-fn
            (fn [request]
              (assoc (select-keys request [:injury-id :source-record-pin
                                           :diagnosis-evidence-pin
                                           :proposal-evidence-pin :acceptance-test])
                     :status :accepted))}))
        supplied-test (acceptance-case "already-reviewed" :unknown)
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
                                 {:status :succeeded :diagnosis (diagnosis-fixture :configuration)})
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

(deftest refused-preparation-is-its-own-reason-not-a-diagnosis-mismatch
  "Click wm-click-c78d0f27 (2026-10-19): production options carried no cast
   sessions and no worktree, prepare refused :work-order/invalid, and route!
   compared an :available diagnosis observation with the absent work order.
   The record said diagnosis-transaction-mismatch although observation and
   ledger agreed. The refusal of preparation is reported as itself, with
   prepare's defects, and no effect runs."
  (let [dir (root)
        ledger-path (str dir "/ledger.edn")
        _ (spit ledger-path "{}")
        source-a {:path "records/a.edn" :sha256 (apply str (repeat 64 "a"))}
        diagnosis-a {:path "diagnoses/a.edn" :sha256 (apply str (repeat 64 "c"))}
        effects (atom 0)
        base-opts {:meta-injury-ledger-path ledger-path
                   :meta-repair-transaction-path (str dir "/transaction.edn")
                   :meta-repair-ports {:author (constantly nil)}
                   :meta-acceptance-run-fn (constantly {:status :ran})
                   :acceptance-test (acceptance-case "P17-absent-but-shaped" :unavailable)
                   :diagnosis-evidence-pin diagnosis-a
                   :meta-diagnosis-observation
                   {:status :available :injury-id "injury-a"
                    :source-record-pin source-a :diagnosis-evidence-pin diagnosis-a}}
        result (with-redefs [store/load-durable-ledger
                             (constantly {:status :loaded :ledger {}})
                             ledger/active-injuries
                             (constantly {:status :computed
                                          :injuries {"injury-a" {:injury-id "injury-a"}}})
                             coordinator/prepare
                             (constantly {:status :refused
                                          :reason :coordinator/preparation-refused
                                          :defects [:work-order/invalid]})
                             coordinator/coordinate
                             (fn [& _] (swap! effects inc) nil)]
                 (production/route! base-opts))]
    (is (= :refused (:status result)))
    (is (= :meta-self-heal/preparation-refused (:reason result)))
    (is (= :coordinator/preparation-refused (get-in result [:prepare :reason])))
    (is (= [:work-order/invalid] (get-in result [:prepare :defects])))
    (is (= :dedicated-meta-self-heal (:route result)))
    (is (false? (:ordinary-selection-called? result)))
    (is (zero? @effects))))

(deftest production-shaped-options-without-identities-refuse-at-preparation
  "The exact option shape of click wm-click-c78d0f27, through the runtime:
   a real durable ledger, real ports, diagnosis pin and acceptance test, and
   none of the three sessions or the worktree. The real coordinator/prepare
   refuses, and the runtime reports that refusal rather than a mismatch or an
   exception from coordinating a nil transaction."
  (let [env (harness/prepare-environment! (root))
        calls (atom {})
        ports (harness/fake-ports (:repo env) calls :success)
        result (runtime/run-opportunity!
                (dissoc (opts env ports)
                        :author-session :reviewer-session
                        :repair-reviewer-session :target-worktree))]
    (is (= :refused (:status result)))
    (is (= :meta-self-heal/preparation-refused (:reason result)))
    (is (= [:work-order/invalid] (get-in result [:prepare :defects])))
    (is (= :dedicated-meta-self-heal (:route result)))
    (is (false? (:ordinary-selection-called? result)))
    (is (= {} @calls) "no phase port is invoked without a transaction")))

;; M4a (futon2 dfe565350 + review fix, 2026-10-20): `route!` returns a
;; non-checkpoint coordinator result as the disposition. These four run the
;; real `store/persist-ledger!` and `persist-transaction!` on the harness
;; environment; only `coordinator/coordinate` is scripted, call by call.
(defn- scripted-route
  "Run route! on a fresh harness environment with coordinate answering from
  RESULTS in order. Returns {:result :calls :env :ledger :transaction}."
  ([results] (scripted-route (harness/prepare-environment! (root)) results))
  ([env results]
  (let [prepared (coordinator/prepare (:durable env) (harness/work-order-options env))
        ledger (:ledger (:durable env))
        transaction (:transaction prepared)
        calls (atom 0)
        script (mapv (fn [r] (if (fn? r) (r ledger transaction) r)) results)
        result (with-redefs [coordinator/coordinate
                             (fn [& _]
                               (let [n (swap! calls inc)]
                                 (if (<= n (count script))
                                   (nth script (dec n))
                                   (throw (ex-info "coordinate called past the script"
                                                   {:call n})))))]
                 (production/route!
                  (assoc (opts env (harness/fake-ports (:repo env) (atom {}) :success))
                         ;; The admitted acceptance observation route! binds
                         ;; back to the work order before coordination. Built
                         ;; from the work order, so the binding passes by
                         ;; construction: these are loop tests, not binding
                         ;; tests; the binding's own red test is
                         ;; acceptance-binding-refuses-a-proposal-the-work-order-does-not-carry.
                         :meta-acceptance-observation
                         (let [order (:work-order transaction)]
                           {:status :accepted :injury-id (:injury-id order)
                            :source-record-pin (:cause-record-pin order)
                            :diagnosis-evidence-pin (:diagnosis-evidence-pin order)
                            :acceptance-test (:acceptance-test order)}))))]
    {:result result :calls @calls :env env :ledger ledger :transaction transaction})))

(deftest coordinator-refusal-without-ledger-is-returned-not-reported-as-persist-failure
  ;; Refusal-totality row #66: before dfe565350 this surfaced as
  ;; :meta-self-heal/checkpoint-persist-failed because the nil ledger was
  ;; handed to persist-ledger!.
  (let [env (harness/prepare-environment! (root))
        before (slurp (:ledger-path env))
        {:keys [result calls]} (scripted-route
                                env
                                [{:status :refused :reason :coordinator/input-refused
                                  :defects [:attempt]}])]
    (is (= :refused (:status result)))
    (is (= :coordinator/input-refused (:reason result)))
    (is (= :claim (:phase result)))
    (is (not (str/includes? (pr-str result) "checkpoint-persist-failed")))
    (is (not (contains? result :ledger-write)))
    (is (not (contains? result :transaction-write)))
    (is (= 1 calls))
    (is (= before (slurp (:ledger-path env))))))

(deftest held-result-stops-the-loop-and-keeps-its-reason-and-phase
  ;; Order error 4 / row #67: before dfe565350 a held result persisted fine
  ;; and the loop went on to :review as if a checkpoint had been reached.
  (let [{:keys [result calls]}
        (scripted-route
         [(fn [l t] {:status :checkpoint :phase :claim :ledger l :transaction t})
          (fn [l t] {:status :held :reason :author-failed :phase :author
                     :ledger l :transaction t :receipt {:status :failed}})])]
    (is (= :held (:status result)))
    (is (= :author-failed (:reason result)))
    (is (= :author (:phase result)))
    (is (= 2 calls) "the loop did not go on to :review and the final coordinate did not run")
    (is (= :persisted (get-in result [:ledger-write :status])))
    (is (= :persisted (get-in result [:transaction-write :status])))
    (is (= [:claim] (mapv :phase (:checkpoint-receipts result))))
    (is (= :dedicated-meta-self-heal (:route result)))))

(deftest checkpoint-persist-failure-is-still-reported
  ;; The one case that keeps :checkpoint-persist-failed: a :checkpoint result
  ;; whose ledger the store refuses.
  (let [{:keys [result calls]}
        (scripted-route
         [(fn [_ t] {:status :checkpoint :phase :claim :ledger nil :transaction t})])]
    (is (= :refused (:status result)))
    (is (= :meta-self-heal/checkpoint-persist-failed (:reason result)))
    (is (= :claim (:phase result)))
    (is (= :store/invalid-ledger (get-in result [:ledger-write :reason])))
    (is (= 1 calls))))

(deftest final-coordinate-result-carries-reason-and-phase
  ;; Row #68: before dfe565350 the final map had :status only.
  (let [checkpoint (fn [phase] (fn [l t] {:status :checkpoint :phase phase :ledger l :transaction t}))
        {:keys [result calls]}
        (scripted-route
         [(checkpoint :claim) (checkpoint :author) (checkpoint :review) (checkpoint :enact)
          (fn [l t] {:status :refused :reason :coordinator/enact-payload-refused
                     :ledger l :transaction t})])]
    (is (= :refused (:status result)))
    (is (= :coordinator/enact-payload-refused (:reason result)))
    (is (= :final (:phase result)) "the coordinator gave no phase; the loop had finished")
    (is (= 5 calls))
    (is (= [:claim :author :review :enact] (mapv :phase (:checkpoint-receipts result))))
    (is (= :persisted (get-in result [:ledger-write :status])))))

;; M6a (futon2 3975321a3, 2026-10-20; SelfHealRoute rev 6 mathlib4 494d0f8e6a): the
;; route decides from the one ledger read, and an uncomputable active set ends the click.
(deftest route-decides-from-the-context-read-not-a-second-read
  (let [env (harness/prepare-environment! (root))
        ports (harness/fake-ports (:repo env) (atom {}) :success)
        base (opts env ports)
        ctx (production/diagnosis-context base)
        no-second-read (fn [& _] (throw (ex-info "second read" {})))
        ;; Only the file read is forbidden: coordinator/prepare calls
        ;; ledger/active-injuries on the context's ledger inside its own try, so
        ;; forbidding that too would turn this into a preparation refusal and the
        ;; assertions below would pass for the wrong reason (M8R finding 13).
        routed (with-redefs [store/load-durable-ledger no-second-read]
                 (production/route! (assoc base :meta-diagnosis-context ctx)))]
    (is (= :computed (get-in ctx [:active-injuries :status])))
    (is (map? routed) "route! ran from the context without a read of its own")
    (is (= :dedicated-meta-self-heal (:route routed)))
    (is (not= :meta-self-heal/active-set-unavailable (:reason routed)))
    (is (not= :meta-self-heal/preparation-refused (:reason routed)) "prepare ran on the context's ledger")
    ;; The disagreement: the file changes after the context was taken.  Before M6a the
    ;; second read found no injury here and route! returned nil (ordinary work after a
    ;; diagnosis was spent, order error 3).
    (let [_ (spit (:ledger-path env) (pr-str ledger/empty-ledger))
          again (production/route! (assoc base :meta-diagnosis-context ctx))
          fresh (production/route! base)]
      (is (map? again) "the context's read still routes")
      (is (= :dedicated-meta-self-heal (:route again)))
      (is (nil? fresh) "without a context route! reads the file, which now has no injury"))))

(deftest uncomputable-active-set-ends-the-click-before-acceptance
  ;; Not reachable from a file today: load-ledger validates before :loaded and
  ;; active-injuries runs the same validate-ledger (meta_injury_store.clj L36-49,
  ;; meta_injury_ledger.clj L284-288), so the dependency is scripted to return the
  ;; refusal it would return.  The branch exists in prepare-diagnosis (P:786) and the
  ;; table makes it terminal.
  (let [env (harness/prepare-environment! (root))
        ports (harness/fake-ports (:repo env) (atom {}) :success)
        acceptance-calls (atom 0) ordinary-calls (atom 0)
        result (with-redefs [ledger/active-injuries
                             (constantly {:status :refused :reason :ledger/invalid :defects [:entry/seal]})]
                 (with-redefs-fn {#'production/prepare-acceptance (fn [& _] (swap! acceptance-calls inc) nil)
                                  #'runtime/run-ordinary-opportunity! (fn [& _] (swap! ordinary-calls inc) nil)}
                   ;; No supplied pin: with one, prepare-diagnosis answers :present before
                   ;; the active-set check, and the refusal then comes from route!'s own
                   ;; guard (the third test) rather than from run-opportunity!.
                   #(runtime/run-opportunity! (dissoc (opts env ports) :diagnosis-evidence-pin))))]
    (is (= :refused (:status result)))
    (is (= :meta-self-heal/active-set-unavailable (:reason result)))
    (is (= :dedicated-meta-self-heal (:route result)))
    (is (false? (:ordinary-selection-called? result)))
    (is (= {:status :refused :reason :ledger/invalid :defects [:entry/seal]} (:active-injuries result)))
    (is (= :meta-self-heal/diagnosis-active-set-unavailable (get-in result [:diagnosis-observation :reason])))
    (is (= :not-required (get-in result [:acceptance-observation :status])))
    (is (zero? @acceptance-calls))
    (is (zero? @ordinary-calls))
    ;; With a supplied pin (every harness click) the same clause fires, since the
    ;; read decides before the pin (prepare-diagnosis, M8R finding 15); before that
    ;; reorder the pin answered :present and only route!'s guard caught the case,
    ;; after an acceptance observation the rev 6 table has no edge for.
    (let [pinned (with-redefs [ledger/active-injuries
                               (constantly {:status :refused :reason :ledger/invalid :defects [:entry/seal]})]
                   (with-redefs-fn {#'production/prepare-acceptance (fn [& _] (swap! acceptance-calls inc) nil)
                                    #'runtime/run-ordinary-opportunity! (fn [& _] (swap! ordinary-calls inc) nil)}
                     #(runtime/run-opportunity! (opts env ports))))]
      (is (= :meta-self-heal/active-set-unavailable (:reason pinned)))
      (is (= :not-required (get-in pinned [:acceptance-observation :status])))
      (is (zero? @acceptance-calls))
      (is (zero? @ordinary-calls)))))

(deftest route-refuses-not-nil-when-the-context-active-set-is-uncomputable
  (let [env (harness/prepare-environment! (root))
        ports (harness/fake-ports (:repo env) (atom {}) :success)
        ctx (assoc (production/diagnosis-context (opts env ports))
                   :active-injuries {:status :refused :reason :ledger/invalid})
        result (production/route! (assoc (opts env ports) :meta-diagnosis-context ctx))]
    (is (= :loaded (get-in ctx [:durable-ledger :status])))
    (is (map? result) "a refusal, not nil")
    (is (= :meta-self-heal/active-set-unavailable (:reason result)))
    (is (false? (:ordinary-selection-called? result)))
    (is (= {:status :refused :reason :ledger/invalid} (:active-injuries result)))))

;; M8R (claude-2's review of the tests above, 2026-10-20): the cases it found missing.
(deftest coordinator-refusal-with-a-transaction-but-no-ledger-writes-only-the-transaction
  ;; Half of refusal-totality #66: port-receipt-refused (C:188) and port-threw (C:194)
  ;; carry :transaction without :ledger.
  (let [env (harness/prepare-environment! (root))
        before (slurp (:ledger-path env))
        {:keys [result calls]} (scripted-route
                                env
                                [(fn [_ t] {:status :refused :reason :coordinator/port-threw
                                            :phase :author :transaction t})])]
    (is (= :refused (:status result)))
    (is (= :coordinator/port-threw (:reason result)))
    (is (= :author (:phase result)))
    (is (= 1 calls))
    (is (= :persisted (get-in result [:transaction-write :status])))
    (is (not (contains? result :ledger-write)))
    (is (= before (slurp (:ledger-path env))))))

(deftest final-coordinate-keeps-the-coordinator-phase-over-final
  ;; The retry port runs inside the final call; a port refusal there carries
  ;; :phase :retry (SelfHealRoute rev 5 CoordinationPhase.retry), which wins over :final.
  (let [checkpoint (fn [phase] (fn [l t] {:status :checkpoint :phase phase :ledger l :transaction t}))
        {:keys [result calls]}
        (scripted-route
         [(checkpoint :claim) (checkpoint :author) (checkpoint :review) (checkpoint :enact)
          (fn [l t] {:status :refused :reason :coordinator/port-receipt-refused :phase :retry
                     :ledger l :transaction t})])]
    (is (= :refused (:status result)))
    (is (= :coordinator/port-receipt-refused (:reason result)))
    (is (= :retry (:phase result)))
    (is (= 5 calls))))

(deftest acceptance-binding-refuses-a-proposal-the-work-order-does-not-carry
  ;; Row #62: an admitted observation whose test differs from the work order's must
  ;; refuse before coordination (the scripted-route fixture bypasses this by construction).
  (let [env (harness/prepare-environment! (root))
        ports (harness/fake-ports (:repo env) (atom {}) :success)
        base (opts env ports)
        prepared (coordinator/prepare (:durable env) (harness/work-order-options env))
        order (:work-order (:transaction prepared))
        calls (atom 0)
        result (with-redefs [coordinator/coordinate (fn [& _] (swap! calls inc) (throw (ex-info "coordinate" {})))]
                 (production/route!
                  (assoc base :meta-acceptance-observation
                         {:status :accepted :injury-id (:injury-id order)
                          :source-record-pin (:cause-record-pin order)
                          :diagnosis-evidence-pin (:diagnosis-evidence-pin order)
                          :acceptance-test (assoc (:acceptance-test order) :id "some-other-proposal")})))]
    (is (= :refused (:status result)))
    (is (= :meta-self-heal/acceptance-transaction-mismatch (:reason result)))
    (is (= "some-other-proposal" (get-in result [:expected :acceptance-test :id])))
    (is (= (:id (:acceptance-test order)) (get-in result [:actual :acceptance-test :id])))
    (is (zero? @calls))))

(deftest effect-ports-carry-the-adapter-click-minting-function
  "M10b-3 (2026-10-20): the futon3c adapter exposes :retry-click-id-fn and the
   coordinator reads :retry-click-id from the ports map when it re-queues an
   inconclusive retry. Absence stays absent; a supplied port map passes through."
  (let [id-fn (fn [repair-id attempt ordinal] (str repair-id "/" attempt "/" ordinal))
        base {:agency-invoke-fn identity :repair-enact-fn identity
              :repair-retry-fn identity}
        with-fn (#'production/effect-ports (assoc base :retry-click-id-fn id-fn))
        without (#'production/effect-ports base)
        supplied (#'production/effect-ports
                  (assoc base :retry-click-id-fn id-fn
                         :meta-repair-ports {:author (constantly nil)}))]
    (is (identical? id-fn (:retry-click-id with-fn)))
    (is (every? fn? (map with-fn [:author :review :enact :retry])))
    (is (not (contains? without :retry-click-id)))
    (is (= {:author (:author supplied)} supplied)
        "an explicit port map is used as given, nothing merged in")))

(deftest latest-retry-click-follows-the-requeue-receipts-not-index-two
  "No live transaction.edn existed under data/wm-meta-injuries on 2026-10-20,
   so the receipts below are authored in the phase order the coordinator
   seals them: author, review, enact, retry, then retry-requeue-<n>."
  (let [receipt (fn [phase click] {:phase phase :payload {:retry-click click}})
        enacted [{:phase :author :payload {:patch-sha "p"}} {:phase :review}
                 (receipt :enact "click-0")]
        retried (conj enacted (receipt :retry "click-0"))
        requeued (into retried [(receipt :retry-requeue-1 "click-1")
                                (receipt :retry-requeue-2 "click-2")])]
    (is (= "click-2" (production/latest-retry-click {:receipts requeued}))
        "the newest requeue receipt names the click")
    (is (= "click-0" (production/latest-retry-click {:receipts retried})))
    (is (= "click-0" (production/latest-retry-click {:receipts enacted}))
        "before any retry receipt the enact receipt's minted click stands")
    (is (nil? (production/latest-retry-click {:receipts (subvec enacted 0 2)})))
    (is (nil? (production/latest-retry-click nil)))
    (is (= "click-0" (production/latest-retry-click
                      {:receipts (conj retried {:phase :retry-later :payload {:retry-click "x"}})}))
        "an unrelated phase whose name merely starts with retry is not a retry receipt")))
