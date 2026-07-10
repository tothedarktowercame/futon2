(ns manual-overnight-flights
  "Foreground, mana-gated overnight flight runner.

  This is intentionally not cron.  In armed mode it repeats the current WM
  one-shot tick followed by the selection->authoring dispatcher, then sleeps
  between attempts so the authoring agent has time to land its deposit.  The
  authoring agent still consumes fold-authoring mana as its first act; this
  runner only refuses early when the ledger is already empty."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.mana-gate :as mana]
            [futon2.aif.selection-authoring-coupling :as coupling])
  (:import [java.time Instant]))

(def default-log-file "/home/joe/code/futon2/logs/manual-overnight-flights.log")
(def default-count 5)
(def default-window-days 14)
(def default-interval-minutes 120)
(def default-reviewer "claude-4")

(defn- parse-long-option
  [label value]
  (try
    (Long/parseLong value)
    (catch Exception _
      (throw (ex-info (str label " must be an integer") {:value value})))))

(defn parse-args
  [args]
  (loop [args args
         opts {:count default-count
               :window-days default-window-days
               :interval-minutes default-interval-minutes
               :reviewer default-reviewer
               :log-file default-log-file}]
    (if (empty? args)
      opts
      (let [[a b & more] args]
        (case a
          "--armed" (recur (rest args) (assoc opts :armed? true))
          "--skip-scheduled" (recur (rest args) (assoc opts :skip-scheduled? true))
          "--count" (recur more (assoc opts :count (parse-long-option "--count" b)))
          "--window-days" (recur more (assoc opts :window-days (parse-long-option "--window-days" b)))
          "--interval-minutes" (recur more (assoc opts :interval-minutes
                                                  (parse-long-option "--interval-minutes" b)))
          "--reviewer" (recur more (assoc opts :reviewer b))
          "--log-file" (recur more (assoc opts :log-file b))
          (throw (ex-info (str "unknown option: " a) {:arg a})))))))

(defn- append-log!
  [log-file line]
  (.mkdirs (.getParentFile (io/file log-file)))
  (spit log-file (str line "\n") :append true))

(defn- stamp
  [message]
  (str "[" (Instant/now) "] " message))

(defn- sh!
  [log-file dir & cmd]
  (append-log! log-file (stamp (str "RUN " (pr-str cmd))))
  (let [{:keys [exit out err]} (apply shell/sh (concat cmd [:dir dir]))]
    (when (seq out)
      (append-log! log-file (str/trim-newline out)))
    (when (seq err)
      (append-log! log-file (str/trim-newline err)))
    (append-log! log-file (stamp (str "EXIT " exit " " (pr-str cmd))))
    {:exit exit :out out :err err :cmd cmd}))

(defn- git-sha
  []
  (let [{:keys [exit out]} (shell/sh "git" "-C" "/home/joe/code/futon2"
                                    "rev-parse" "--short" "HEAD")]
    (if (zero? exit) (str/trim out) "unknown")))

(defn- coupling-command
  [{:keys [reviewer armed? log-file]}]
  (cond-> ["clojure" "-M" "scripts/couple_selection_to_authoring.clj"
           "--from" reviewer
           "--log-file" log-file]
    (not armed?) (conj "--dry-run")))

(defn- scheduled-command
  [{:keys [window-days]}]
  ["clojure" "-M:wm-scheduled" (str window-days)])

(defn dry-run!
  [opts]
  (let [log-file (:log-file opts)
        balance (mana/balance coupling/fold-authoring-gate)]
    (append-log! log-file
                 (stamp (str "DRY-RUN manual overnight flights"
                             " sha=" (git-sha)
                             " balance=" balance
                             " count=" (:count opts))))
    (let [result (apply sh! log-file "/home/joe/code/futon2"
                        (coupling-command opts))]
      (println "manual-overnight-flights dry-run")
      (println "fold-authoring balance=" balance)
      (println "log=" log-file)
      (println (str/trim-newline (:out result)))
      result)))

(defn armed-run!
  [opts]
  (let [log-file (:log-file opts)
        count (long (:count opts))
        interval-ms (* 1000 60 (long (:interval-minutes opts)))]
    (append-log! log-file
                 (stamp (str "ARMED manual overnight flights"
                             " sha=" (git-sha)
                             " count=" count
                             " interval-minutes=" (:interval-minutes opts)
                             " window-days=" (:window-days opts))))
    (loop [i 1]
      (when (<= i count)
        (let [balance (mana/balance coupling/fold-authoring-gate)]
          (append-log! log-file (stamp (str "flight " i "/" count
                                            " balance-before=" balance)))
          (if (not (pos? balance))
            (do
              (append-log! log-file
                           (stamp (str "REFUSED flight " i
                                       " - fold-authoring mana exhausted")))
              (println "manual-overnight-flights refused: fold-authoring mana exhausted"))
            (let [scheduled (when-not (:skip-scheduled? opts)
                              (apply sh! log-file "/home/joe/code/futon2"
                                     (scheduled-command opts)))
                  coupling-result (when (or (:skip-scheduled? opts)
                                            (zero? (:exit scheduled)))
                                    (apply sh! log-file "/home/joe/code/futon2"
                                           (coupling-command opts)))]
              (println (str "flight " i "/" count
                            " scheduled-exit=" (or (:exit scheduled) :skipped)
                            " coupling-exit=" (or (:exit coupling-result) :skipped)))
              (cond
                (and scheduled (not (zero? (:exit scheduled))))
                (append-log! log-file
                             (stamp (str "STOP scheduled failed on flight " i)))

                (and coupling-result (not (zero? (:exit coupling-result))))
                (append-log! log-file
                             (stamp (str "STOP coupling failed on flight " i)))

                (= i count)
                (append-log! log-file (stamp "DONE requested flight count reached"))

                :else
                (do
                  (append-log! log-file
                               (stamp (str "SLEEP milliseconds=" interval-ms)))
                  (Thread/sleep interval-ms)
                  (recur (inc i)))))))))))

(defn -main
  [& args]
  (let [opts (parse-args args)]
    (if (:armed? opts)
      (armed-run! opts)
      (dry-run! opts))))

(apply -main *command-line-args*)
(shutdown-agents)
