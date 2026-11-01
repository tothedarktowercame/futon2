(ns futon2.aif.environmental-probe-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.environmental-probe :as probe]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-self-heal-production :as production]
            [futon2.aif.meta-terminal-injury :as terminal]
            [futon2.aif.out-of-band-fix :as out-of-band]))

(defn- temp-root []
  (.toFile (java.nio.file.Files/createTempDirectory
            "environmental-probe-"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- fixture [classification]
  (let [root (temp-root)
        acceptance-root (str (io/file root "acceptances"))
        cause {:run/id "environmental-fixture" :click/id "click-fixture"
               :failure {:kind :external-read-timeout :stage :refresh
                         :detail {:method :get :url "http://fixture.invalid/read"
                                  :timeout-ms 1000}}}
        cause-pin (out-of-band/retain! (.getPath root) "causes" cause)
        classified (terminal/classify cause cause-pin)
        diagnosis {:schema :wm/meta-diagnosis-receipt-v1
                   :diagnosis {:injury-id (:injury-id classified)
                               :source-record-pin cause-pin
                               :classification classification}}
        diagnosis-pin (out-of-band/retain! (.getPath root) "diagnoses" diagnosis)
        history (:ledger (terminal/integrate ledger/empty-ledger classified))
        ledger-path (str (io/file root "ledger.edn"))]
    (spit ledger-path (str (pr-str history) "\n"))
    {:root root :acceptance-root acceptance-root :cause-pin cause-pin
     :diagnosis-pin diagnosis-pin :ledger-path ledger-path :history history}))

(defn- run-probe [env response]
  (binding [probe/*http-get* (fn [_ _] response)]
    (probe/probe! (merge (select-keys env [:acceptance-root :cause-pin
                                           :diagnosis-pin])
                         {:injury-id (get-in env [:history :entries 0 :injury-id])
                          :probed-by "medic/test"
                          :source-revision "revision/test"}))))

(defn- retained-authority [observation]
  (probe/retained-checked-medic-authority observation))

(defn- route-opts [env]
  {:meta-injury-ledger-path (:ledger-path env)
   :meta-repair-transaction-path (str (io/file (:root env) "transaction.edn"))
   :meta-acceptance-root (:acceptance-root env)
   :meta-repository-root (.getPath (:root env))
   :meta-source-revision "revision/route-test"
   :meta-medic-authority-verifier (constantly false)
   :meta-diagnosis-observation
   {:status :available
    :injury-id (get-in env [:history :entries 0 :injury-id])
    :source-record-pin (:cause-pin env)
    :diagnosis-evidence-pin (:diagnosis-pin env)}})

(deftest checked-probe-discharges-exact-occurrence-and-replay-is-duplicate
  (let [env (fixture :environmental-substrate-read-timeout)
        observed (run-probe env {:status 200 :body "healthy"})
        result (probe/discharge! (merge (select-keys env [:ledger-path])
                                        (select-keys observed [:receipt :receipt-pin])
                                        {:injury-id (get-in env [:history :entries 0 :injury-id])
                                         :normal-authority (constantly false)}))
        loaded (store/load-durable-ledger (:ledger-path env) retained-authority)
        replay (probe/discharge! (merge (select-keys env [:ledger-path])
                                        (select-keys observed [:receipt :receipt-pin])
                                        {:injury-id (get-in env [:history :entries 0 :injury-id])
                                         :normal-authority retained-authority}))]
    (is (= :checked (:status observed)))
    (is (= :appended (:status result)) (pr-str result))
    (is (= :environmental-recovery
           (get-in result [:entry :medic-disposition])))
    (is (empty? (get-in (ledger/effective-active-injuries
                         (:ledger loaded) retained-authority)
                        [:injuries])))
    (is (= :duplicate (:status replay)))
    (is (= :medic/environmental-already-recovered (:reason replay)))))

(deftest refused-probes-never-change-ledger
  (doseq [[response expected]
          [[{:status 504 :body "gateway timeout"}
            :environmental-probe/http-status]
           [(fn [_ _] (throw (java.net.http.HttpTimeoutException. "timeout")))
            :environmental-probe/timeout]]]
    (let [env (fixture :environmental-substrate-read-timeout)
          before (slurp (:ledger-path env))
          observed (binding [probe/*http-get*
                             (if (fn? response) response (fn [_ _] response))]
                     (probe/probe! (merge
                                    (select-keys env [:acceptance-root :cause-pin
                                                      :diagnosis-pin])
                                    {:injury-id (get-in env [:history :entries 0 :injury-id])
                                     :probed-by "medic/test"
                                     :source-revision "revision/test"})))
          result (probe/discharge! (merge (select-keys env [:ledger-path])
                                          (select-keys observed [:receipt :receipt-pin])
                                          {:injury-id (get-in env [:history :entries 0 :injury-id])
                                           :normal-authority (constantly false)}))]
      (is (= :refused (:status observed)))
      (is (= expected (get-in observed [:receipt :typed-failure])))
      (is (= :medic/environmental-probe-not-checked (:reason result)))
      (is (= before (slurp (:ledger-path env)))))))

(deftest non-environmental-diagnosis-refuses-before-http
  (let [env (fixture :code-defect)
        called? (atom false)
        before (slurp (:ledger-path env))
        result (binding [probe/*http-get* (fn [& _] (reset! called? true))]
                 (probe/probe! (merge
                                (select-keys env [:acceptance-root :cause-pin
                                                  :diagnosis-pin])
                                {:injury-id (get-in env [:history :entries 0 :injury-id])
                                 :probed-by "medic/test"
                                 :source-revision "revision/test"})))]
    (is (= :environmental-probe/classification-required (:reason result)))
    (is (false? @called?))
    (is (= before (slurp (:ledger-path env))))))

(deftest tampered-probe-receipt-cannot-authorise-discharge
  (let [env (fixture :environmental-substrate-read-timeout)
        observed (run-probe env {:status 200 :body "healthy"})
        before (slurp (:ledger-path env))
        pin (:receipt-pin observed)
        _ (spit (:path pin) (str (pr-str (assoc (:receipt observed) :probed-by "tampered"))
                                 "\n"))
        result (probe/discharge! (merge (select-keys env [:ledger-path])
                                        (select-keys observed [:receipt :receipt-pin])
                                        {:injury-id (get-in env [:history :entries 0 :injury-id])
                                         :normal-authority (constantly false)}))]
    (is (= :medic/authority-rejected (:reason result)) (pr-str result))
    (is (= before (slurp (:ledger-path env))))))

(deftest self-heal-route-discharges-an-environmental-injury-by-one-probe
  (let [env (fixture :environmental-substrate-read-timeout)
        result (binding [probe/*http-get* (fn [_ _] {:status 200 :body "healthy"})]
                 (production/route! (route-opts env)))
        verifier retained-authority
        loaded (store/load-durable-ledger (:ledger-path env) verifier)
        next-result (production/route! (assoc (route-opts env)
                                         :meta-medic-authority-verifier verifier))]
    (is (= :appended (:status result)) (pr-str result))
    (is (= {:kind :environmental-probe :status :checked
            :receipt-pin (get-in result [:environmental-probe :receipt-pin])}
           (:environmental-probe result)))
    (is (empty? (get-in (ledger/effective-active-injuries
                         (:ledger loaded) verifier) [:injuries])))
    (is (nil? next-result) "the next click is free to take the ordinary route")))

(deftest self-heal-route-retains-one-refused-probe-without-changing-ledger
  (let [env (fixture :environmental-substrate-read-timeout)
        before (slurp (:ledger-path env))
        result (binding [probe/*http-get* (fn [_ _] {:status 504 :body "timeout"})]
                 (production/route! (route-opts env)))
        pin (get-in result [:environmental-probe :receipt-pin])]
    (is (= :refused (:status result)))
    (is (= :meta-self-heal/acceptance-run-refused (:reason result)))
    (is (= :refused (get-in result [:environmental-probe :status])))
    (is (and (map? pin) (.isFile (io/file (:path pin)))))
    (is (= before (slurp (:ledger-path env))))))

(deftest self-heal-route-does-not-probe-a-code-diagnosis
  (let [env (fixture :code-defect)
        called? (atom false)
        result (binding [probe/*http-get* (fn [& _] (reset! called? true))]
                 (production/route! (route-opts env)))]
    (is (false? @called?))
    (is (= :meta-self-heal/production-config-required (:reason result)))
    (is (some #{:acceptance-test} (:absent-terms result)))))
