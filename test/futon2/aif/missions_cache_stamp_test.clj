(ns futon2.aif.missions-cache-stamp-test
  "WM-MISSION-READ-CACHE-I: load-missions-cached stamped its entry with the
  time captured BEFORE the read, so an entry was already as old as the read
  took; with the substrate read at ~9-40 s against the 5 s TTL every call
  missed (WM-MISSION-READ-COST-D, futon2 417ece2e: five calls with a 6 s
  stubbed read made five reads). The entry is now stamped when the read
  returns. The pre-fix code is materialised from git (417ece2e) under a
  renamed namespace, never loaded over the real one."
  (:require [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.mission-registry :as mr]))

(def ttl-ms 200)
(def read-ms 300)

(defn- reset-cache [f]
  (reset! @#'mr/missions-cache nil)
  (f)
  (reset! @#'mr/missions-cache nil))

(use-fixtures :each reset-cache)

(def pre-fix-ns
  "futon2.aif.mission-registry at 417ece2e, loaded as its own namespace."
  (delay
    (let [{:keys [exit out err]} (sh/sh "git" "show" "417ece2e:src/futon2/aif/mission_registry.clj")
          renamed 'futon2.aif.mission-registry-at-417ece2e]
      (when-not (zero? exit) (throw (ex-info "git show failed" {:err err})))
      (load-string (str/replace-first out "(ns futon2.aif.mission-registry" (str "(ns " renamed)))
      (the-ns renamed))))

(defn- five-calls
  "Five cached reads in NS against a counting load-missions that sleeps
  read-ms; the number of reads made."
  [ns]
  (let [reads (atom 0)
        v (fn [sym] (ns-resolve ns sym))]
    (with-redefs-fn {(v 'load-missions) (fn [& _] (swap! reads inc) (Thread/sleep read-ms) {:missions []})
                     (v 'missions-cache-ttl-ms) ttl-ms}
      #(dotimes [_ 5] ((v 'load-missions-cached))))
    @reads))

(deftest a-read-slower-than-the-ttl-is-cached
  (is (= 1 (five-calls (the-ns 'futon2.aif.mission-registry)))))

(deftest the-pre-fix-stamp-missed-every-time
  ;; the bad case, the D's count: the same five calls through 417ece2e
  (let [ns @pre-fix-ns]
    (reset! @(ns-resolve ns 'missions-cache) nil)
    (is (= 5 (five-calls ns)))))

(deftest an-entry-older-than-the-ttl-from-the-reads-end-is-refreshed
  (let [reads (atom 0)]
    (with-redefs [mr/load-missions (fn [& _] (swap! reads inc) (Thread/sleep read-ms) {:missions []})
                  mr/missions-cache-ttl-ms ttl-ms]
      (mr/load-missions-cached)
      (Thread/sleep (quot ttl-ms 2))
      (mr/load-missions-cached)
      (is (= 1 @reads) "within the TTL of the read's end: a hit")
      (Thread/sleep (+ ttl-ms 50))
      (mr/load-missions-cached)
      (is (= 2 @reads) "past it: refreshed"))))

(deftest a-different-code-root-misses
  (let [reads (atom [])]
    (with-redefs [mr/load-missions (fn [& args] (swap! reads conj (vec args)) {:missions []})]
      (mr/load-missions-cached "/tmp/root-a")
      (mr/load-missions-cached "/tmp/root-a")
      (mr/load-missions-cached "/tmp/root-b")
      (is (= [["/tmp/root-a"] ["/tmp/root-b"]] @reads)))))
