(ns wm-run-lock-test
  "RUN12. A lock that cannot refuse proves nothing, so every test here asserts
  on a refusal, a reclaim, or a pass-through -- not on the happy path alone."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.wm-run-lock :as lock])
  (:import (java.lang ProcessHandle)))

(defn- temp-lock-path []
  (str (System/getProperty "java.io.tmpdir")
       "/futon2-wm-run-lock-test-" (System/nanoTime) ".run-lock"))

(defn- with-temp-lock
  "Call f with a lock path of its own, and remove it however f ends."
  [f]
  (let [path (temp-lock-path)]
    (try (f path)
         (finally (io/delete-file (io/file path) true)))))

(defn- stale-lock-map [overrides]
  (merge {:pid 2147483647
          :pid-start-ms 1
          :agent "gone"
          :sha "deadbee"
          :host (#'lock/hostname)
          :run-id "run-gone"
          :acquired-at "2026-09-01T16:33:02Z"
          :token "stale-token"}
         overrides))

(deftest acquire-writes-pid-agent-and-sha-test
  (with-temp-lock
    (fn [path]
      (let [handle (lock/acquire! {:path path :agent "test-agent" :run-id "r1" :token nil})
            on-disk (edn/read-string (slurp path))]
        (is (false? (:nested? handle)))
        (is (= (:pid on-disk) (.pid (ProcessHandle/current)))
            "the lock records the pid of the process holding it")
        (is (= "test-agent" (:agent on-disk)))
        (is (string? (:sha on-disk)))
        (is (= "r1" (:run-id on-disk)))
        (is (:token on-disk))
        (is (= :alive (lock/holder-state on-disk)))
        (is (true? (:released? (lock/release! handle))))
        (is (not (.exists (io/file path))) "release deletes the lock file")))))

(deftest second-starter-refuses-and-names-the-holder-test
  ;; The negative control at unit scale: the lock is held by a live process
  ;; (this one), so a second acquire must refuse rather than wait or proceed.
  (with-temp-lock
    (fn [path]
      (let [held (lock/acquire! {:path path :agent "holder-agent" :run-id "run-A" :token nil})]
        (try
          (let [e (is (thrown? clojure.lang.ExceptionInfo
                               (lock/acquire! {:path path :agent "second" :token nil})))
                msg (ex-message e)]
            (is (lock/refused? e))
            (is (= :held (:reason (ex-data e))))
            (is (str/includes? msg (str (.pid (ProcessHandle/current))))
                "the refusal names the holder's pid")
            (is (str/includes? msg "holder-agent") "the refusal names the holding agent")
            (is (str/includes? msg "run-A") "the refusal names the holder's run id")
            (is (str/includes? msg path) "the refusal names the lock file")
            (is (str/includes? msg (:sha (:lock held))) "the refusal names the holder's sha"))
          (finally (lock/release! held)))))))

(deftest dead-pid-is-reclaimed-and-the-reclaim-is-logged-test
  (with-temp-lock
    (fn [path]
      ;; pid 2147483647 is above /proc/sys/kernel/pid_max on this host, so no
      ;; live process can carry it; the lock is provably abandoned.
      (spit path (pr-str (stale-lock-map {})))
      (is (= :dead (lock/holder-state (lock/read-lock path))))
      (let [err (java.io.StringWriter.)
            handle (binding [*err* err]
                     (lock/acquire! {:path path :agent "reclaimer" :token nil}))]
        (is (str/includes? (str err) "reclaiming stale lock")
            "the reclaim is logged, not silent")
        (is (str/includes? (str err) "run-gone") "the log names what was reclaimed")
        (is (= "reclaimer" (:agent (lock/read-lock path))))
        (lock/release! handle)))))

(deftest live-pid-with-a-different-start-instant-is-pid-reuse-test
  (with-temp-lock
    (fn [path]
      (spit path (pr-str (stale-lock-map {:pid (.pid (ProcessHandle/current))
                                          :agent "recycled"
                                          :run-id "run-recycled"})))
      (is (= :pid-reused (lock/holder-state (lock/read-lock path)))
          "a live pid that started at another instant is not the recorded holder")
      (let [err (java.io.StringWriter.)
            handle (binding [*err* err] (lock/acquire! {:path path :token nil}))]
        (is (str/includes? (str err) "pid-reused"))
        (lock/release! handle)))))

(deftest another-host-is-fail-closed-test
  (with-temp-lock
    (fn [path]
      (spit path (pr-str (stale-lock-map {:pid 1 :host "some-other-host"})))
      (is (= :unverifiable-host (lock/holder-state (lock/read-lock path))))
      (let [e (is (thrown? clojure.lang.ExceptionInfo
                           (lock/acquire! {:path path :token nil})))]
        (is (lock/refused? e))
        (is (str/includes? (ex-message e) "not this one")
            "a lock from another host refuses rather than being reclaimed")))))

(deftest unreadable-lock-is-read-as-held-test
  ;; An empty file is what an acquirer looks like between createNewFile and the
  ;; write, so it must refuse rather than be reclaimed.
  (with-temp-lock
    (fn [path]
      (spit path "")
      (is (= :unreadable (lock/holder-state (lock/read-lock path))))
      (let [e (is (thrown? clojure.lang.ExceptionInfo
                           (lock/acquire! {:path path :token nil})))]
        (is (lock/refused? e))
        (is (str/includes? (ex-message e) "delete")
            "the refusal says how to clear a corrupt lock")))))

(deftest outer-token-nests-and-does-not-release-test
  (with-temp-lock
    (fn [path]
      (let [outer (lock/acquire! {:path path :agent "run-script" :run-id "run-A" :token nil})
            token (:token (:lock outer))
            inner (lock/acquire! {:path path :agent "tick" :token token})]
        (is (true? (:nested? inner)) "a tick inside its own run passes through")
        (is (= {:released? false :reason :nested} (lock/release! inner)))
        (is (.exists (io/file path))
            "the nested tick leaves the run script's lock in place")
        (is (= "run-script" (:agent (lock/read-lock path))))
        (is (true? (:released? (lock/release! outer))))))))

(deftest release-does-not-delete-another-holders-lock-test
  (with-temp-lock
    (fn [path]
      (let [mine (lock/acquire! {:path path :agent "mine" :token nil})]
        (lock/release! mine)
        (let [theirs (lock/acquire! {:path path :agent "theirs" :token nil})
              result (lock/release! mine)]
          (is (= :not-ours (:reason result)))
          (is (.exists (io/file path)))
          (is (= "theirs" (:agent (lock/read-lock path))))
          (lock/release! theirs))))))

(deftest call-with-run-lock-releases-on-throw-test
  (with-temp-lock
    (fn [path]
      (is (thrown? RuntimeException
                   (lock/call-with-run-lock
                    {:path path :token nil}
                    (fn [_] (throw (RuntimeException. "tick failed"))))))
      (is (not (.exists (io/file path)))
          "a tick that throws still releases, or the next run is blocked by a ghost"))))

(deftest default-path-sits-beside-the-trace-files-test
  (is (str/ends-with? (lock/default-lock-path) "/data/wm-trace/.run-lock")))
