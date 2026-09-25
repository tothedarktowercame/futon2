(ns futon2.aif.c8-registry-timeout-test
  "WM-SPIKE-FIX-II D: a C8 registry read that times out keeps its reason.
  The second flight (flight-ffcd772b) recorded both C8 checks as bare
  {:status :unreachable}: /api/alpha/test-registry/latest answered in 5.66 s
  against the reader's 5 s timeout. A real local HTTP server that sleeps
  past a bound timeout, so the real client's timeout is what is classified."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.observation-checks :as oc])
  (:import [com.sun.net.httpserver HttpServer HttpHandler]
           [java.net InetSocketAddress ServerSocket]))

(defn- with-slow-server [sleep-ms f]
  (let [server (HttpServer/create (InetSocketAddress. "127.0.0.1" 0) 0)]
    (.createContext server "/" (reify HttpHandler
                                 (handle [_ ex]
                                   (Thread/sleep (long sleep-ms))
                                   (let [b (.getBytes "{\"latest\": []}")]
                                     (.sendResponseHeaders ex 200 (count b))
                                     (with-open [o (.getResponseBody ex)] (.write o b))))))
    (.start server)
    (try (f (str "http://127.0.0.1:" (.getPort (.getAddress server))))
         (finally (.stop server 0)))))

(deftest a-timeout-is-named-with-the-timeout-that-applied
  (with-slow-server 2000
    (fn [base]
      (binding [oc/*registry-timeout-ms* 300]
        (doseq [r [(oc/fetch-latest-for-namespace base "demo-test")
                   (oc/fetch-registry-entry base "test-registry-x")]]
          (is (= :registry-unreadable (:kind r))
              (pr-str r))
          (is (= :unreachable (get-in r [:data :status])))
          (is (= 300 (get-in r [:data :timeout-ms])) "the timeout that applied")
          (is (string? (get-in r [:data :message]))))))))

(deftest a-refused-connection-is-not-a-timeout
  (let [port (with-open [s (ServerSocket. 0)] (.getLocalPort s))
        r (oc/fetch-latest-for-namespace (str "http://127.0.0.1:" port) "demo-test")]
    (is (= :unreachable (get-in r [:data :status])))
    (is (nil? (get-in r [:data :timeout-ms])))
    (is (string? (get-in r [:data :class])))))
