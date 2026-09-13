(ns futon2.aif.on-demand-entrypoint-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.on-demand-entrypoint :as entry]))

(def valid-config
  {:schema/version :wm/on-demand-whole-loop-v1
   :run/id "row26-isolated-1"
   :author "author-seat"
   :reviewer "reviewer-seat"
   :repair-reviewer "repair-reviewer-seat"
   :opportunity-count 1
   :transport-timeout-ms 10
   :poll-ms 1
   :timeout-ms 100})

(deftest delegates-once-to-existing-gated-click-and-retains-refusal
  (let [posts (atom []) gets (atom 0)
        result (entry/run-on-demand!
                valid-config
                {:post! (fn [url request]
                          (swap! posts conj [url request])
                          {:status 200 :body (json/generate-string
                                              {:click-id "wm-click-isolated"
                                               :started-at "2026-09-13T00:00:00Z"})})
                 :get! (fn [_ _]
                         (let [n (swap! gets inc)]
                           {:status 200
                            :body (json/generate-string
                                   (if (= 1 n)
                                     {:running? true :click-id "wm-click-isolated"}
                                     {:running? false :click-id "wm-click-isolated"
                                      :last-result {:click-id "wm-click-isolated"
                                                    :outcome "incomplete"
                                                    :run-id-observation
                                                    {:status "present"
                                                     :source "runner-return"
                                                     :value "observed-run"}
                                                    :failure-kind "r9-refused"}}))}))
                 :now-ms (constantly 0)
                 :sleep! (fn [_])})
        payload (json/parse-string (get-in @posts [0 1 :body]) true)]
    (is (= 1 (count @posts)))
    (is (= "row26-isolated-1" (:run-id payload)))
    (is (= "duree-click-on-demand" (:trigger payload)))
    (is (= 10 (get-in @posts [0 1 :timeout])))
    (is (= "row26-isolated-1" (:run/requested-id result)))
    (is (= "observed-run" (:run/id result)))
    (is (= :incomplete (:outcome result)))
    (is (= "r9-refused" (get-in result [:terminal :failure-kind])))))

(deftest invalid-and-malformed-responses-refuse-before-success
  (testing "more than one opportunity is never accepted"
    (is (= :on-demand/not-exactly-one
           (try (entry/run-on-demand! (assoc valid-config :opportunity-count 2) {})
                (catch clojure.lang.ExceptionInfo e
                  (get-in (ex-data e) [:refusal :kind]))))))
  (testing "status from another click cannot close this run"
    (is (= :on-demand/status-click-mismatch
           (try
             (entry/run-on-demand!
              valid-config
              {:post! (fn [_ _] {:status 200 :body "{\"click-id\":\"ours\",\"started-at\":\"now\"}"})
               :get! (fn [_ _] {:status 200 :body "{\"running?\":false,\"click-id\":\"borrowed\"}"})
               :now-ms (constantly 0) :sleep! (fn [_])})
             (catch clojure.lang.ExceptionInfo e
               (get-in (ex-data e) [:refusal :kind]))))))
  (testing "the lead's missing terminal shape refuses"
    (is (= :on-demand/malformed-terminal
           (try
             (entry/run-on-demand!
              valid-config
              {:post! (fn [_ _] {:status 200 :body "{\"click-id\":\"ours\",\"started-at\":\"now\"}"})
               :get! (fn [_ _] {:status 200 :body "{\"running?\":false,\"click-id\":\"ours\"}"})
               :now-ms (constantly 0) :sleep! (fn [_])})
             (catch clojure.lang.ExceptionInfo e
               (get-in (ex-data e) [:refusal :kind]))))))
  (testing "malformed JSON is typed"
    (is (= :on-demand/malformed-json
           (try
             (entry/run-on-demand!
              valid-config
              {:post! (fn [_ _] {:status 200 :body "not-json"})
               :get! (fn [_ _] nil) :now-ms (constantly 0) :sleep! (fn [_])})
             (catch clojure.lang.ExceptionInfo e
               (get-in (ex-data e) [:refusal :kind]))))))
  (testing "terminal run identity must be an observed typed value or absence"
    (is (= :on-demand/malformed-run-id-observation
           (try
             (entry/run-on-demand!
              valid-config
              {:post! (fn [_ _] {:status 200 :body "{\"click-id\":\"ours\",\"started-at\":\"now\"}"})
               :get! (fn [_ _]
                       {:status 200
                        :body "{\"running?\":false,\"click-id\":\"ours\",\"last-result\":{\"click-id\":\"ours\",\"outcome\":\"incomplete\"}}"})
               :now-ms (constantly 0) :sleep! (fn [_])})
             (catch clojure.lang.ExceptionInfo e
               (get-in (ex-data e) [:refusal :kind])))))))

(deftest whole-client-timeout-does-not-claim-worker-cancellation
  (let [clock (atom 0)
        posts (atom 0)
        refusal
        (try
          (entry/run-on-demand!
           (assoc valid-config :timeout-ms 5)
           {:post! (fn [_ request]
                     (swap! posts inc)
                     (is (= 10 (:timeout request)))
                     (reset! clock 4)
                     {:status 200 :body "{\"click-id\":\"ours\",\"started-at\":\"now\"}"})
            :get! (fn [_ _]
                    (reset! clock 6)
                    {:status 200 :body "{\"running?\":true,\"click-id\":\"ours\"}"})
            :now-ms #(deref clock)
            :sleep! (fn [_])})
          nil
          (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e))))]
    (is (= 1 @posts) "an ambiguous accepted POST is never retried")
    (is (= :on-demand/observation-timeout (:kind refusal)))
    (is (false? (:worker-cancelled? refusal)))))

(deftest live-status-shape-retains-server-observed-run-id
  (let [live (slurp "holes/labs/wm-contract/runs/row-26-on-demand-entrypoint-2026-09-13/lead-live-status.json")
        click-id (:click-id (json/parse-string live true))
        result (entry/run-on-demand!
                valid-config
                {:post! (fn [_ _] {:status 200
                                    :body (json/generate-string
                                           {:click-id click-id :started-at "observed"})})
                 :get! (fn [_ _] {:status 200 :body live})
                 :now-ms (constantly 0) :sleep! (fn [_])})]
    (is (= "row26-isolated-1" (:run/requested-id result)))
    (is (= "d6785ee0-2d38-4bd2-a20f-ecf511569532" (:run/id result)))
    (is (not= (:run/requested-id result) (:run/id result)))
    (is (= :incomplete (:outcome result)))))
