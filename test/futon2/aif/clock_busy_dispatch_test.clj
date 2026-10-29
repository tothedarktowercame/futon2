(ns futon2.aif.clock-busy-dispatch-test
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.wm.debugger :as debugger]))

(def busy-body {:ok false :error "clock-store-busy"
                :error/code "clock/store-busy" :dispatch/not-created? true})
(defn response [status body]
  {:status status :headers {"retry-after" "2"}
   :body (json/generate-string body)})
(defn dispatch []
  (runner/dispatch! {:agency-base "http://agency.invalid"}
                    "wm-author" "wm-full-loop" "M-test" "ask"))
(defn failure [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e))))

(deftest preserves-proven-pre-job-clock-refusal
  (let [calls (atom 0)]
    (with-redefs [http/post (fn [& _] (swap! calls inc) (response 503 busy-body))]
      (let [data (failure dispatch)]
        (is (= :clock/store-busy (:failure-kind data)))
        (is (= :clock/store-busy (:error/code data)))
        (is (= :clock-store-busy (:outcome data)))
        (is (true? (:dispatch/not-created? data)))
        (is (= "2" (:retry-after data)))
        (is (= busy-body (:response data)))
        (is (= 503 (:status data)))
        (is (= 1 @calls) "no automatic redispatch")))))

(deftest other-errors-stay-dispatch-failures
  (doseq [[status body] [[500 busy-body]
                         [503 (dissoc busy-body :dispatch/not-created?)]
                         [503 (assoc busy-body :error/code "other")]
                         [503 (assoc busy-body :job-id "already-created")]
                         [503 {:error "unavailable"}]]]
    (with-redefs [http/post (fn [& _] (response status body))]
      (is (= :dispatch-failed (:outcome (failure dispatch))))))
  (let [original (ex-info "transport failure" {:kind :test})]
    (with-redefs [http/post (fn [& _] (throw original))]
      (is (identical? original (try (dispatch) (catch Throwable t t)))))))

(deftest held-phase-retains-refusal-and-retries-after-recovery
  (let [calls (atom 0)
        events (atom [])
        run-id "clock-busy-dispatch-test"
        task (atom nil)]
    (debugger/attach!)
    (try
      (binding [runner/*wm-status-reporting?* false]
       (with-redefs [http/post (fn [& _]
                               (if (= 1 (swap! calls inc))
                                 (response 503 busy-body)
                                 (response 200 {:accepted true})))]
        (reset! task (future (runner/run-phase!
                             {:run-id run-id :phase-log-fn #(swap! events conj %)}
                             {} :ticket-readiness-refresh dispatch)))
        (let [deadline (+ (System/nanoTime) 5000000000)
              stop (loop []
                     (or (first (filter #(= run-id (:run-id %)) (debugger/stopped)))
                         (when (< (System/nanoTime) deadline)
                           (Thread/sleep 10) (recur))))]
          (is (some? stop))
          (is (= :clock/store-busy (get-in stop [:ex-data :failure-kind])))
          (is (true? (get-in stop [:ex-data :dispatch/not-created?])))
          (is (= 1 @calls))
          (when stop (debugger/continue! run-id :retry :fixed))
          (is (= {:accepted true} (deref @task 5000 ::timeout)))
          (is (= 2 @calls))
          (is (= [[:start nil] [:end :error] [:start nil] [:end :ok]]
                 (mapv (juxt :transition :outcome) @events)))
          (is (= :retry (:debugger/restart (second @events))))
          (is (empty? (debugger/stopped))))))
      (finally
        (when (some #(= run-id (:run-id %)) (debugger/stopped))
          (debugger/continue! run-id :abort :failed))
        (when @task (future-cancel @task))
        (debugger/detach!)))))
