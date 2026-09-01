#!/usr/bin/env bb
(ns checks.wm-click-resource-observer
  "External observer for one serving-JVM War Machine click."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]))

(def schema "wm-click-resource-v1")
(def shared-scope "shared-serving-jvm")

(defn now [] (str (java.time.Instant/now)))
(defn instant [x]
  (when (string? x)
    (try (java.time.Instant/parse x) (catch Throwable _ nil))))

(defn resource-status [{:keys [before after journal-readable? native-markers]}]
  (cond
    (or (nil? before) (nil? after) (not journal-readable?)) "unavailable"
    (or (pos? (- after before)) (seq native-markers)) "dirty"
    :else "clean"))

(defn envelope-valid?
  [run receipt]
  (let [started (instant (:started-at receipt))
        finished (instant (:finished-at receipt))
        run-start (instant (:startedAt run))]
    (and (= schema (:schema receipt))
         (= shared-scope (:observation-scope receipt))
         (= (:run/id run) (:run-id receipt))
         (= (:click/id run) (:click-id receipt))
         started finished run-start
         (not (.isAfter started run-start))
         (not (.isBefore finished run-start))
         (contains? #{"clean" "dirty"} (:resource-status receipt)))))

(defn certificate-resource [run-id source-path receipt]
  {:schema 2
   :run/id run-id
   :source-schema :wm-click-resource-v1
   :observation-scope :shared-serving-jvm
   :status (keyword (:resource-status receipt))
   :reason (some-> (:reason receipt) keyword)
   :execution-outcome (some-> (:terminal-outcome receipt) keyword)
   :command-exit nil
   :wrapper-exit nil
   :pids-events-max-delta (:pids-events-max-delta receipt)
   :native-thread-exhaustion (boolean (seq (:native-thread-markers receipt)))
   :tasks-peak (:pids-peak receipt)
   :source-receipt source-path})

(defn observe!
  "Observe one click. Dependencies are injected so the throwaway rehearsal
   crosses the real handler without contacting production."
  [{:keys [post-click status cgroup-sample journal-sample clock sleep-ms
           payload receipt-path]
    :or {clock now sleep-ms #(Thread/sleep %)}}]
  (let [observed-start (clock)
        before (cgroup-sample)
        accepted (post-click payload)
        click-id (:click-id accepted)]
    (if-not (and (string? click-id) (not (str/blank? click-id)))
      {:schema schema :observation-scope shared-scope
       :resource-status "unavailable" :reason "click-not-accepted"
       :started-at observed-start :finished-at (clock)}
      (let [peak (atom (long (or (:pids-current before) 0)))
            terminal
            (loop [n 0]
              (let [s (status)
                    sample (cgroup-sample)]
                (swap! peak max (long (or (:pids-current sample) 0)))
                (cond
                  (and (= click-id (:click-id s)) (false? (:running? s))) s
                  (>= n 600) nil
                  :else (do (sleep-ms 100) (recur (inc n))))))
            after (cgroup-sample)
            journal (journal-sample observed-start)
            last-result (:last-result terminal)
            run-path (:run-record last-result)
            run (when run-path
                  (try (edn/read-string (slurp run-path))
                       (catch Throwable _ nil)))
            measurements {:before (:pids-events-max before)
                          :after (:pids-events-max after)
                          :journal-readable? (:readable? journal)
                          :native-markers (:native-thread-markers journal)}
            status-value (resource-status measurements)
            receipt {:schema schema
                     :observation-scope shared-scope
                     :observer-process (.pid (java.lang.ProcessHandle/current))
                     :serving-cgroup (:cgroup before)
                     :click-id click-id
                     :run-id (:run/id run)
                     :run-record run-path
                     :started-at observed-start
                     :click-started-at (:started-at accepted)
                     :run-started-at (:startedAt run)
                     :finished-at (clock)
                     :terminal-outcome (:outcome last-result)
                     :run-record-status (:run-record-status last-result)
                     :serving-runner-code (:serving-runner-code terminal)
                     :pids-events-max-before (:before measurements)
                     :pids-events-max-after (:after measurements)
                     :pids-events-max-delta
                     (when (and (:before measurements) (:after measurements))
                       (- (:after measurements) (:before measurements)))
                     :pids-peak (max @peak (long (or (:pids-current after) 0)))
                     :native-thread-markers (vec (:native-markers measurements))
                     :resource-status status-value
                     :reason (cond
                               (nil? terminal) "click-status-unavailable"
                               (nil? run) "run-record-unavailable"
                               (= "unavailable" status-value) "resource-observation-unavailable"
                               :else nil)}]
        (when receipt-path
          (io/make-parents receipt-path)
          (let [tmp (str receipt-path ".tmp")]
            (spit tmp (str (json/generate-string receipt {:pretty true}) "\n"))
            (java.nio.file.Files/move
             (.toPath (io/file tmp)) (.toPath (io/file receipt-path))
             (into-array java.nio.file.StandardCopyOption
                         [java.nio.file.StandardCopyOption/ATOMIC_MOVE
                          java.nio.file.StandardCopyOption/REPLACE_EXISTING]))))
        receipt))))

(defn- service-cgroup []
  (let [{:keys [exit out]} (shell/sh "systemctl" "--user" "show"
                                     "futon3c-zone.service"
                                     "--property=ControlGroup" "--value")]
    (when (zero? exit) (str/trim out))))

(defn- read-int [path]
  (try (parse-long (str/trim (slurp path))) (catch Throwable _ nil)))

(defn- cgroup-sample []
  (let [rel (service-cgroup)
        root (when rel (str "/sys/fs/cgroup" rel))
        events (when root
                 (try (into {} (map #(str/split % #"\s+")
                                    (str/split-lines (slurp (str root "/pids.events")))))
                      (catch Throwable _ nil)))]
    {:cgroup rel
     :pids-events-max (some-> (get events "max") parse-long)
     :pids-current (when root (read-int (str root "/pids.current")))}))

(defn- journal-sample [since]
  (let [{:keys [exit out]} (shell/sh "journalctl" "--user"
                                     "-u" "futon3c-zone.service"
                                     "--since" since "--no-pager" "-o" "cat")
        lines (str/split-lines out)]
    {:readable? (zero? exit)
     :native-thread-markers
     (filterv #(or (str/includes? % "pthread_create failed")
                   (str/includes? % "Failed to start the native thread"))
              lines)}))

(defn- json-body [response] (json/parse-string (:body response) true))

(defn -main [& args]
  (let [[receipt-path reviewer] args
        base "http://127.0.0.1:7070"]
    (when (str/blank? receipt-path)
      (binding [*out* *err*] (println "usage: wm_click_resource_observer.clj RECEIPT REVIEWER"))
      (System/exit 1))
    (let [receipt
          (observe! {:payload {:reviewer reviewer :trigger "duree-click-on-demand"}
                     :receipt-path receipt-path
                     :post-click #(json-body (http/post (str base "/api/alpha/wm/click")
                                                        {:body (json/generate-string %)
                                                         :headers {"Content-Type" "application/json"}}))
                     :status #(json-body (http/get (str base "/api/alpha/wm/click")))
                     :cgroup-sample cgroup-sample
                     :journal-sample journal-sample})]
      (println (pr-str receipt))
      (System/exit (if (= "clean" (:resource-status receipt)) 0 1)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
