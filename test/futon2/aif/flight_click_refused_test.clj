(ns futon2.aif.flight-click-refused-test
  "R1(c-1): a POST refusal is terminal flight evidence, not :no-progress.
  These controls use http-click-fn's injectable transport and run!; they do
  not hand-build the disputed abstention."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as flight-runner]
            [futon2.aif.flight-click-refusal-test :as refusal-fixture]))

(def target "M-flight-click-refused")
(def sources {:wants {target [:one]}})

(defn- new-flight []
  (flight/start (flight/choose-target {:requested target})
                {:kind :checkbox} {:id "flight-click-refused"}))

(defn- run-http-click
  [click-fn]
  (flight/run! (new-flight)
               {:click-fn click-fn
                :observe-fn (fn [_ _] {:one false})
                :sources-fn (constantly sources)
                :max-clicks 3}))

(deftest server-preflight-refusal-is-a-terminal-flight-status
  (let [posts (atom 0)
        f (run-http-click
           (flight-runner/http-click-fn
            {:today (constantly "2026-10-23")
             :post! (fn [_] (swap! posts inc) refusal-fixture/preflight-409)
             :get-status! (fn [] (throw (ex-info "must not poll a refused click" {})))}))]
    (is (= 1 @posts) "the non-open terminal status prevents another POST")
    (is (= :click-refused (:status f)))
    (is (= {:click-id "2026-10-23-flight-click-refused-click-1"
            :status 409
            :detail {:error "wm-click-cast-not-invoke-ready"
                     :message "WM click refused: a cast seat cannot be invoked"
                     :details {:unready {:author {:seat "zai-5" :reason "absent"}
                                         :reviewer {:seat "codex-7" :reason "absent"}}}}}
           (:refusal f)))
    (is (= :click-not-started (get-in f [:clicks 0 :abstention :kind])))))

(deftest no-response-is-a-terminal-flight-status
  (let [posts (atom 0)
        f (run-http-click
           (flight-runner/http-click-fn
            {:today (constantly "2026-10-23")
             :post! (fn [_]
                      (swap! posts inc)
                      (throw (java.net.ConnectException. "Connection refused")))
             :get-status! (fn [] (throw (ex-info "must not poll a refused click" {})))}))]
    (is (= 1 @posts))
    (is (= :click-refused (:status f)))
    (is (= {:absent :no-response} (get-in f [:refusal :status])))
    (is (= {:absent :no-response :message "Connection refused"}
           (get-in f [:refusal :detail])))))

(deftest a-started-click-without-progress-stays-no-progress
  (let [posts (atom 0)
        f (run-http-click
           (flight-runner/http-click-fn
            {:today (constantly "2026-10-23")
             :post! (fn [_] (swap! posts inc) {:status 200 :body {:click-id "started-click"}})
             :get-status! (constantly {:running? false :click-id "started-click"})
             :read-record! (constantly nil)}))]
    (is (= 1 @posts))
    (is (= :no-progress (:status f)))
    (is (nil? (:refusal f)))))

(deftest refusal-stops-a-flight-whose-observation-moved
  ;; another actor moves :a while the refused click is out; without the
  ;; terminal status the flight would read :open and POST again
  (let [posts (atom 0)
        seen (atom 0)
        f (flight/run! (new-flight)
                       {:click-fn (flight-runner/http-click-fn
                                   {:today (constantly "2026-10-23")
                                    :post! (fn [_] (swap! posts inc) refusal-fixture/preflight-409)
                                    :get-status! (fn [] (throw (ex-info "must not poll a refused click" {})))})
                        :observe-fn (fn [_ _] (if (= 1 (swap! seen inc))
                                                {:a false :b false}
                                                {:a true :b false}))
                        :sources-fn (constantly {:wants {target [:a :b]}})
                        :max-clicks 3})]
    (is (= [:a] (get-in f [:clicks 0 :advanced])) "the world did advance")
    (is (= 1 @posts))
    (is (= :click-refused (:status f)))))

(deftest a-200-without-click-id-is-a-refusal
  (let [posts (atom 0)
        f (run-http-click
           (flight-runner/http-click-fn
            {:today (constantly "2026-10-23")
             :post! (fn [_] (swap! posts inc) {:status 200 :body {:ok true}})
             :get-status! (fn [] (throw (ex-info "must not poll an unstarted click" {})))}))]
    (is (= 1 @posts))
    (is (= :click-refused (:status f)))
    (is (= 200 (get-in f [:refusal :status])))
    (is (= {:absent :no-reason-in-body :body {:ok true}} (get-in f [:refusal :detail])))))
