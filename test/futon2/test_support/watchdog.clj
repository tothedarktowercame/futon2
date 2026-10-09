(ns futon2.test-support.watchdog
  (:require [clojure.test :as test])
  (:import (java.io PrintWriter StringWriter)
           (java.util.concurrent.atomic AtomicReference)))

(def test-timeout-ms
  "The one suite-wide per-deftest wall-clock bound: ten minutes."
  600000)

(def ^:dynamic *test-timeout-ms* test-timeout-ms)

(defn- thread-dump []
  (let [sw (StringWriter.)
        out (PrintWriter. sw)]
    (doseq [[thread frames] (sort-by (comp #(.getName ^Thread %) key)
                                     (Thread/getAllStackTraces))]
      (.printf out "\n\"%s\" id=%d state=%s\n"
               (into-array Object [(.getName ^Thread thread)
                                   (.getId ^Thread thread)
                                   (.getState ^Thread thread)]))
      (doseq [frame frames]
        (.println out (str "\tat " frame))))
    (.toString sw)))

(defn timed-test-var
  "Run one deftest under the suite watchdog, retaining clojure.test bindings.
  A timed-out daemon is interrupted and then stopped so the census continues."
  [original-test-var v]
  (let [started (System/nanoTime)
        result (promise)
        thrown (AtomicReference.)
        test-name (str (-> v meta :ns ns-name) "/" (-> v meta :name))
        worker (Thread.
                ^Runnable
                (bound-fn []
                  (try
                    (original-test-var v)
                    (deliver result :completed)
                    (catch Throwable t
                      (.set thrown t)
                      (deliver result :threw))))
                (str "futon2-test-watchdog:" test-name))]
    (.setDaemon worker true)
    (.start worker)
    (let [outcome (deref result *test-timeout-ms* ::timeout)
          elapsed-ms (long (/ (- (System/nanoTime) started) 1000000))]
      (println "[test-duration]"
               (pr-str {:test test-name :duration-ms elapsed-ms
                        :timeout-ms *test-timeout-ms*
                        :status (if (= ::timeout outcome) :timeout :completed)}))
      (if (= ::timeout outcome)
        (let [dump (thread-dump)]
          (test/do-report
           {:type :fail
            :message (str "TIMEOUT: " test-name " exceeded " *test-timeout-ms*
                          " ms\nTHREAD DUMP\n" dump)
            :expected (list '<= 'elapsed-ms *test-timeout-ms*)
            :actual elapsed-ms})
          (.interrupt worker)
          (.join worker 1000)
          (when (.isAlive worker)
            ;; A census cannot be held forever by code that ignores interrupt.
            (.stop worker)))
        (when-let [t (.get thrown)]
          (throw t))))))

(defmacro with-watchdog [& body]
  `(let [original# test/test-var]
     (with-redefs [test/test-var (partial timed-test-var original#)]
       ~@body)))
