(ns futon2.wm-run-lock
  "One machine, one runner: an exclusive lock over the shared per-date WM trace file.

  data/wm-trace/wm-trace-<date>.edn is shared by every run on the day. On
  2026-09-01 two agents started ticks three minutes apart and three
  run-tick-once JVMs wrote into one file (holes/labs/wm-contract/runs/
  2026-09-01-s1b/COLLISION-NOTE.md). The bell that preceded the second start was
  a convention; this file is the mechanism.

  REFUSAL. `acquire!` neither waits nor proceeds when the lock is held: it
  throws ex-info carrying ::refused and names the holder's pid, agent, sha,
  host and acquisition time. futon2.run-tick-once/-main turns that into a
  stderr line and exit code 3.

  NESTING. A run is many run-tick-once processes. A run script takes the lock
  once before the first tick and exports FUTON_WM_RUN_LOCK_TOKEN; each tick
  inside it sees its own token in the lock file, passes through, and does NOT
  release on exit. The script releases after the last tick. Without that
  variable the lock's scope is a single tick.

  NOT futon2.aif.lane-futility/with-index-lock (lane_futility.clj:175-183),
  which takes an OS FileLock on .lane-futility-index.lock in the same directory
  and BLOCKS until it is free. That is right for a short sidecar update and
  wrong here twice over: RUN12 requires a second starter to fail rather than
  wait, and an OS lock carries no identity, so its refusal could not name who
  holds it. This lock is a file whose CONTENT is the holder.

  STALENESS is decided, not assumed. A lock is reclaimable only when its holder
  is provably gone: same host, and either no live process carries the pid, or a
  live process carries it but started at a different instant than the one
  recorded (pid reuse). Every reclaim prints a line to stderr. Every other
  reading is fail-closed and refuses: a lock written on another host cannot be
  checked from here, and a lock file that is empty or unparseable is what an
  acquirer looks like between creating the file and writing it, so it is read as
  held. A corrupt lock is named in the refusal for removal by hand."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str])
  (:import (java.lang ProcessHandle)
           (java.net InetAddress)
           (java.time Instant)
           (java.util UUID)))

(def ^:private lock-path-env "FUTON_WM_RUN_LOCK")
(def ^:private agent-env "FUTON_WM_AGENT")
(def ^:private token-env "FUTON_WM_RUN_LOCK_TOKEN")

(defn repo-root
  "The futon2 checkout this namespace is serving from."
  []
  (str (System/getProperty "user.home") "/code/futon2"))

(defn default-lock-path
  "Beside the trace files it protects. FUTON_WM_RUN_LOCK overrides it, which is
  how the tests and the negative control keep off the real lock."
  []
  (or (not-empty (str (System/getenv lock-path-env)))
      (str (repo-root) "/data/wm-trace/.run-lock")))

(defn- hostname []
  (try (.getHostName (InetAddress/getLocalHost))
       (catch Throwable _ "unknown-host")))

(defn- current-pid []
  (.pid (ProcessHandle/current)))

(defn- start-ms
  "Epoch-ms at which the live process with this pid started, or nil when no
  live process carries the pid (or the OS will not say)."
  [pid]
  (let [handle (ProcessHandle/of (long pid))]
    (when (.isPresent handle)
      (let [h (.get handle)]
        (when (.isAlive h)
          (let [started (.startInstant (.info h))]
            (when (.isPresent started)
              (.toEpochMilli ^Instant (.get started)))))))))

(defn- head-sha []
  (try
    (let [{:keys [exit out]} (shell/sh "git" "-C" (repo-root) "rev-parse" "HEAD")]
      (if (zero? exit) (str/trim out) "unknown"))
    (catch Throwable _ "unknown")))

(defn- agent-id []
  (or (not-empty (str (System/getenv agent-env)))
      (not-empty (str (System/getenv "USER")))
      "unknown-agent"))

(defn inherited-token
  "The outer run's token, when a run script has taken the lock around us."
  []
  (not-empty (str (System/getenv token-env))))

(defn read-lock
  "nil when the file is absent, ::unreadable when it exists but does not hold a
  lock map, otherwise the lock map."
  [path]
  (let [f (io/file path)]
    (when (.exists f)
      (try
        (let [v (edn/read-string (slurp f))]
          (if (and (map? v) (:pid v)) v ::unreadable))
        (catch Throwable _ ::unreadable)))))

(defn holder-state
  "What the lock file says about its holder, from this host's point of view.
  :absent :alive :dead :pid-reused :unverifiable-host :unreadable"
  [lock]
  (cond
    (nil? lock) :absent
    (= ::unreadable lock) :unreadable
    (not= (:host lock) (hostname)) :unverifiable-host
    :else
    (let [live (start-ms (:pid lock))]
      (cond
        (nil? live) :dead
        (and (:pid-start-ms lock) (not= (:pid-start-ms lock) live)) :pid-reused
        :else :alive))))

(defn describe-holder [lock]
  (if (= ::unreadable lock)
    "an unreadable lock file (no pid recorded)"
    (format "pid %s (agent %s, sha %s, host %s, acquired %s, run %s)"
            (:pid lock) (:agent lock) (:sha lock) (:host lock)
            (:acquired-at lock) (:run-id lock))))

(defn- refusal-message [path lock state]
  (str "FUTON2 WM RUN LOCK: refused. " path " is held by " (describe-holder lock)
       (case state
         :unverifiable-host
         (str ". That host is not this one (" (hostname)
              "), so whether the holder is alive cannot be checked from here")
         :unreadable
         (str ". A lock file with no pid is what an acquirer looks like between"
              " creating the file and writing it, so it is read as held; if no"
              " runner is alive, delete " path " by hand")
         "")
       ". One machine, one runner: this process will not wait and will not"
       " proceed. A lock whose pid is dead is reclaimed automatically on the"
       " next attempt."))

(defn- write-lock! [path lock]
  (spit (io/file path) (str (pr-str lock) "\n")))

(defn- new-lock [{:keys [agent run-id]}]
  (let [pid (current-pid)]
    {:pid pid
     :pid-start-ms (start-ms pid)
     :agent (or agent (agent-id))
     :sha (head-sha)
     :host (hostname)
     :run-id (or run-id "none")
     :acquired-at (str (Instant/now))
     :token (str (UUID/randomUUID))}))

(defn- log-reclaim! [path lock state]
  (binding [*out* *err*]
    (println (str "FUTON2 WM RUN LOCK: reclaiming stale lock " path
                  " held by " (describe-holder lock)
                  " -- " (name state)
                  (when (= :pid-reused state)
                    " (a live process carries that pid but started at a different instant)")))
    (flush)))

(def ^:private max-attempts 3)

(defn acquire!
  "Take the lock, or throw. Returns a handle {:path :lock :nested? :reclaimed}.
  opts: :path (default (default-lock-path)), :agent, :run-id,
        :token (default (inherited-token)) -- a lock already carrying this token
        belongs to our own run, so we pass through without taking or releasing."
  ([] (acquire! {}))
  ([{:keys [path agent run-id token]
     :or {path nil token ::inherit}}]
   (let [path (or path (default-lock-path))
         token (if (= ::inherit token) (inherited-token) token)]
     (io/make-parents path)
     (loop [attempt 1]
       (let [existing (read-lock path)]
         (cond
           (and token (map? existing) (= token (:token existing)))
           {:path path :lock existing :nested? true :reclaimed nil}

           (.createNewFile (io/file path))
           (let [lock (new-lock {:agent agent :run-id run-id})]
             (write-lock! path lock)
             {:path path :lock lock :nested? false :reclaimed nil})

           :else
           (let [lock (read-lock path)
                 state (holder-state lock)]
             (cond
               (= :absent state)                    ; released under us
               (if (< attempt max-attempts)
                 (recur (inc attempt))
                 (throw (ex-info (str "FUTON2 WM RUN LOCK: refused. " path
                                      " changed hands " max-attempts
                                      " times while being taken.")
                                 {::refused true :reason :contended :path path})))

               (#{:dead :pid-reused} state)
               (do (log-reclaim! path lock state)
                   (io/delete-file (io/file path) true)
                   (if (< attempt max-attempts)
                     (recur (inc attempt))
                     (throw (ex-info (str "FUTON2 WM RUN LOCK: refused. " path
                                          " could not be taken after "
                                          max-attempts " reclaims.")
                                     {::refused true :reason :contended :path path}))))

               :else
               (throw (ex-info (refusal-message path lock state)
                               {::refused true
                                :reason :held
                                :path path
                                :state state
                                :holder (when (map? lock) lock)}))))))))))

(defn release!
  "Delete the lock only when it is still the one we wrote. A nested handle
  releases nothing: the run script that took the outer lock releases it."
  [{:keys [path lock nested?]}]
  (cond
    nested? {:released? false :reason :nested}
    :else
    (let [current (read-lock path)]
      (if (and (map? current) (= (:token current) (:token lock)))
        (do (io/delete-file (io/file path) true)
            {:released? true :path path})
        {:released? false :reason :not-ours :path path :current current}))))

(defn call-with-run-lock
  "Run f while holding the lock; release after, refuse (throw) instead of
  starting if another runner holds it."
  [opts f]
  (let [handle (acquire! opts)]
    (try (f handle)
         (finally (release! handle)))))

(defn refused?
  "True for the exception acquire! throws when another runner holds the lock."
  [e]
  (boolean (::refused (ex-data e))))

(defn -main
  "status                -- print the current holder and its state
  hold <seconds> [agent] -- take the lock, print its token, hold, release.
                            The first holder in the negative control."
  [& [cmd arg1 arg2]]
  (let [path (default-lock-path)]
    (case (str cmd)
      "status"
      (let [lock (read-lock path)]
        (println (pr-str {:path path
                          :state (holder-state lock)
                          :holder (when (map? lock) lock)})))

      "hold"
      (let [seconds (Long/parseLong (or arg1 "30"))
            handle (acquire! {:agent (or arg2 "wm-run-lock-hold")
                              :run-id "hold"
                              :token nil})]
        ;; A run script holds the outer lock by TERMing this process after the
        ;; last tick, so the release has to survive a signal as well as a return.
        (.addShutdownHook (Runtime/getRuntime)
                          (Thread. ^Runnable (fn [] (release! handle))))
        (println (pr-str {:held path
                          :pid (:pid (:lock handle))
                          :token (:token (:lock handle))}))
        (flush)
        (try (Thread/sleep (* 1000 seconds))
             (finally (println (pr-str (release! handle))) (flush))))

      (do (binding [*out* *err*]
            (println "usage: futon2.wm-run-lock status | hold <seconds> [agent]"))
          (System/exit 2)))))
