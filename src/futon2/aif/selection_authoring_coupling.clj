(ns futon2.aif.selection-authoring-coupling
  "Selection -> authoring coupling for the War Machine actuator.

  Reads the latest WM trace, finds the top lane mission that is blocked at
  :abstain-missing-leg, and dispatches exactly one mana-gated fold-authoring
  flight when the mission lacks an escrow deposit."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.fold-escrow :as esc]
            [futon2.aif.mana-gate :as mana]
            [futon2.aif.mission-registry :as missions])
  (:import [java.time Instant]))

(def default-trace-dir "/home/joe/code/futon2/data/wm-trace")
(def default-log-file "/home/joe/code/futon2/logs/selection-authoring-coupling.log")
(def default-lock-dir "/home/joe/code/futon2/logs/selection-authoring-coupling")
(def default-author-script "/home/joe/code/futon3c/scripts/author_deposit_for.sh")
(def fold-authoring-gate "fold-authoring")

(defn mission-key
  "Normalize a WM mission target or escrow canonical mission id to M-<stem>."
  [mission-id]
  (missions/mission-target-id mission-id))

(defn deposited-mission-keys
  [deposits]
  (set (keep (comp mission-key :mission) deposits)))

(defn latest-trace-file
  ([] (latest-trace-file default-trace-dir))
  ([trace-dir]
   (let [files (->> (.listFiles (io/file trace-dir))
                    (filter #(re-matches #"wm-trace-.*\.edn" (.getName ^java.io.File %)))
                    (sort-by #(.getName ^java.io.File %)))]
     (last files))))

(defn read-last-trace-record
  [trace-file]
  (when-not (and trace-file (.exists (io/file trace-file)))
    (throw (ex-info "selection-authoring: no trace file found"
                    {:trace-file (some-> trace-file str)})))
  (let [lines (->> (line-seq (io/reader trace-file))
                   (remove str/blank?))]
    (when-not (seq lines)
      (throw (ex-info "selection-authoring: trace file is empty"
                      {:trace-file (str trace-file)})))
    (edn/read-string (last lines))))

(defn first-abstaining-mission
  [trace-record]
  (first (filter #(= :abstain-missing-leg (:verdict %))
                 (:act-gate-verdicts trace-record))))

(defn lock-file
  [lock-dir mission-id]
  (io/file lock-dir (str (-> (or (mission-key mission-id) mission-id)
                             str
                             (str/replace #"[^A-Za-z0-9._-]" "_"))
                         ".lock")))

(defn in-flight?
  [lock-dir mission-id]
  (.exists (lock-file lock-dir mission-id)))

(defn decision
  "Pure decision core. Returns a small map with :status.

  Status values:
  :skipped-no-abstain, :skipped-has-deposit, :skipped-in-flight,
  :deferred-no-mana, :would-fire, :fired."
  [{:keys [trace-record deposit-keys mana-balance in-flight? dry-run? mission-override]}]
  (let [candidate (if mission-override
                    {:mission mission-override
                     :verdict :abstain-missing-leg
                     :forced? true}
                    (first-abstaining-mission trace-record))
        mission-id (:mission candidate)
        mkey (mission-key mission-id)]
    (cond
      (nil? candidate)
      {:status :skipped-no-abstain
       :reason "no :abstain-missing-leg lane mission in trace"}

      (contains? deposit-keys mkey)
      {:status :skipped-has-deposit
       :mission mission-id
       :mission-key mkey
       :reason "escrow deposit already exists"}

      in-flight?
      {:status :skipped-in-flight
       :mission mission-id
       :mission-key mkey
       :reason "authoring lock already exists"}

      (not (pos? (long (or mana-balance 0))))
      {:status :deferred-no-mana
       :mission mission-id
       :mission-key mkey
       :mana-balance (long (or mana-balance 0))
       :reason "fold-authoring mana balance is zero"}

      dry-run?
      {:status :would-fire
       :mission mission-id
       :mission-key mkey
       :mana-balance mana-balance
       :reason "dry-run: all guards clear"}

      :else
      {:status :fired
       :mission mission-id
       :mission-key mkey
       :mana-balance mana-balance
       :reason "all guards clear"})))

(defn log-decision!
  [log-file record]
  (.mkdirs (.getParentFile (io/file log-file)))
  (spit log-file (str (pr-str (assoc record :at (str (Instant/now)))) "\n")
        :append true))

(defn create-lock!
  [lock-dir mission-id content]
  (let [f (lock-file lock-dir mission-id)]
    (.mkdirs (.getParentFile f))
    (when (.exists f)
      (throw (ex-info "selection-authoring: lock already exists"
                      {:lock-file (str f) :mission mission-id})))
    (spit f (pr-str content))
    f))

(defn invoke-author!
  [{:keys [author-script mission reviewer]}]
  (let [cmd (cond-> [author-script mission]
              (seq reviewer) (into ["--from" reviewer]))
        pb (ProcessBuilder. ^java.util.List cmd)]
    (.directory pb (io/file "/home/joe/code"))
    (.redirectErrorStream pb true)
    (let [p (.start pb)
          out (slurp (.getInputStream p))
          exit (.waitFor p)]
      {:exit exit :out out :cmd cmd})))

(defn stable-summary
  [decision]
  (str "selection-authoring "
       (name (:status decision))
       (when-let [m (:mission decision)] (str " mission=" m))
       (when-let [mk (:mission-key decision)] (str " mission-key=" mk))
       (when (contains? decision :mana-balance)
         (str " mana=" (:mana-balance decision)))
       " reason=" (:reason decision)))

(defn run-once!
  [{:keys [trace-file trace-dir deposit-dir mana-dir log-file lock-dir
           author-script reviewer dry-run? mission-override]
    :or {trace-dir default-trace-dir
         log-file default-log-file
         lock-dir default-lock-dir
         author-script default-author-script
         reviewer "claude-4"}}]
  (let [trace-file (or trace-file (latest-trace-file trace-dir))
        trace-record (read-last-trace-record trace-file)
        deposits (:deposits (if deposit-dir
                              (esc/load-deposits deposit-dir)
                              (esc/load-deposits)))
        deposit-keys (deposited-mission-keys deposits)
        candidate (or mission-override (:mission (first-abstaining-mission trace-record)))
        dec (decision {:trace-record trace-record
                       :deposit-keys deposit-keys
                       :mana-balance (if mana-dir
                                       (mana/balance fold-authoring-gate mana-dir)
                                       (mana/balance fold-authoring-gate))
                       :in-flight? (when candidate (in-flight? lock-dir candidate))
                       :dry-run? dry-run?
                       :mission-override mission-override})]
    (if (= :fired (:status dec))
      (let [lock (create-lock! lock-dir (:mission dec)
                               {:mission (:mission dec)
                                :reviewer reviewer
                                :trace-file (str trace-file)
                                :status :dispatching})
            result (invoke-author! {:author-script author-script
                                    :mission (:mission dec)
                                    :reviewer reviewer})
            dec (assoc dec
                       :lock-file (str lock)
                       :invoke-exit (:exit result)
                       :invoke-out (:out result)
                       :invoke-cmd (:cmd result))]
        (log-decision! log-file dec)
        dec)
      (do
        (when-not dry-run?
          (log-decision! log-file dec))
        dec))))

(defn parse-args
  [args]
  (loop [args args
         opts {}]
    (if (empty? args)
      opts
      (let [[a b & more] args]
        (case a
          "--dry-run" (recur (rest args) (assoc opts :dry-run? true))
          "--from" (recur more (assoc opts :reviewer b))
          "--reviewer" (recur more (assoc opts :reviewer b))
          "--mission" (recur more (assoc opts :mission-override b))
          "--trace-file" (recur more (assoc opts :trace-file b))
          "--trace-dir" (recur more (assoc opts :trace-dir b))
          "--deposit-dir" (recur more (assoc opts :deposit-dir b))
          "--mana-dir" (recur more (assoc opts :mana-dir b))
          "--log-file" (recur more (assoc opts :log-file b))
          "--lock-dir" (recur more (assoc opts :lock-dir b))
          "--author-script" (recur more (assoc opts :author-script b))
          (throw (ex-info (str "unknown option: " a) {:arg a})))))))
