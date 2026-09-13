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
   :poll-ms 1
   :timeout-ms 100})

(deftest delegates-once-to-existing-gated-click-and-retains-refusal
  (let [posts (atom []) gets (atom 0)
        result (entry/run-on-demand!
                valid-config
                {:post! (fn [url request]
                          (swap! posts conj [url request])
                          {:status 200 :body (json/generate-string
                                              {:click-id "wm-click-isolated"})})
                 :get! (fn [_ _]
                         (let [n (swap! gets inc)]
                           {:status 200
                            :body (json/generate-string
                                   (if (= 1 n)
                                     {:running? true :click-id "wm-click-isolated"}
                                     {:running? false :click-id "wm-click-isolated"
                                      :last-result {:outcome "incomplete"
                                                    :failure-kind "r9-refused"}}))}))
                 :now-ms (constantly 0)
                 :sleep! (fn [_])})
        payload (json/parse-string (get-in @posts [0 1 :body]) true)]
    (is (= 1 (count @posts)))
    (is (= "row26-isolated-1" (:run-id payload)))
    (is (= "duree-click-on-demand" (:trigger payload)))
    (is (= :incomplete (:outcome result)))
    (is (= "r9-refused" (get-in result [:terminal :failure-kind])))))

(deftest invalid-bounds-and-cross-click-status-refuse-before-success
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
              {:post! (fn [_ _] {:status 200 :body "{\"click-id\":\"ours\"}"})
               :get! (fn [_ _] {:status 200 :body "{\"running?\":false,\"click-id\":\"borrowed\"}"})
               :now-ms (constantly 0) :sleep! (fn [_])})
             (catch clojure.lang.ExceptionInfo e
               (get-in (ex-data e) [:refusal :kind])))))))
