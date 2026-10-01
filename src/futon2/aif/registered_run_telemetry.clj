(ns futon2.aif.registered-run-telemetry
  "Evidence projection used by registered War Machine opportunities."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(defn phase-timings [events]
  (let [ends (filter #(and (= :end (:transition %))
                           (number? (:duration-ms %))) events)]
    {:status (if (seq ends) :complete :typed-missing)
     :clock :monotonic
     :phase-timings-ms
     (reduce (fn [m {:keys [phase duration-ms]}]
               (update m phase (fnil + 0) duration-ms)) {} ends)}))

(defn- normalized-usage [usage]
  (let [input (or (:input-tokens usage) (:input_tokens usage)
                  (:prompt-tokens usage) (:prompt_tokens usage))
        output (or (:output-tokens usage) (:output_tokens usage)
                   (:completion-tokens usage) (:completion_tokens usage))
        total (or (:total-tokens usage) (:total_tokens usage)
                  (when (and (integer? input) (integer? output)) (+ input output)))]
    (when (and (integer? input) (<= 0 input)
               (integer? output) (<= 0 output)
               (= total (+ input output)))
      {:input-tokens input :output-tokens output :total-tokens total})))

(defn model-usage
  "Aggregate authoritative receipts from distinct Agency jobs in RESULT.
   If any dispatched job lacks provider usage, retain typed partial evidence."
  [result]
  (let [job-id-of (fn [m] (try (get m :job-id) (catch ClassCastException _ nil)))
        jobs (->> (tree-seq coll? seq result)
                  (filter map?)
                  (filter job-id-of)
                  (reduce (fn [m job] (assoc m (job-id-of job) job)) {})
                  vals)
        rows (keep #(some-> (:usage %) normalized-usage
                            (assoc :job-id (:job-id %))) jobs)
        missing (vec (sort (remove (set (map :job-id rows)) (map :job-id jobs))))]
    (if (and (seq jobs) (empty? missing))
      (merge {:status :complete :source :agency-provider-receipts
              :jobs (vec (sort-by :job-id rows))}
             (reduce (fn [m row]
                       (-> m
                           (update :input-tokens + (:input-tokens row))
                           (update :output-tokens + (:output-tokens row))
                           (update :total-tokens + (:total-tokens row))))
                     {:input-tokens 0 :output-tokens 0 :total-tokens 0} rows))
      {:status (if (seq rows) :partial :typed-missing)
       :source :agency-provider-receipts
       :jobs (vec (sort-by :job-id rows))
       :missing-job-ids missing})))

(defn- git-head [path]
  (let [{:keys [exit out]} (shell/sh "git" "-C" path "rev-parse" "HEAD")]
    (if (zero? exit) (.trim out) {:absent :git-head-unavailable})))

(defn source-revisions
  ([] (source-revisions {"futon2" "/home/joe/code/futon2"
                         "futon3-pattern-library" "/home/joe/code/futon3"}))
  ([roots]
   (into (sorted-map) (map (fn [[id path]] [id (git-head path)]) roots))))

(defn digest [x]
  (load-identity/sha256 (.getBytes (pr-str x) "UTF-8")))

(defn latest-prior-record [dir]
  (when-let [f (->> (or (.listFiles (io/file dir)) (make-array java.io.File 0))
                    (filter #(re-matches #"tick-run-record-.*\.edn" (.getName ^java.io.File %)))
                    (sort-by #(.lastModified ^java.io.File %) >)
                    first)]
    (try
      (let [r (clojure.edn/read-string (slurp f))]
        {:run/id (:run/id r)
         :source-revisions-after (get-in r [:registered-run/chronology
                                            :source-revisions-after])})
      (catch Throwable _ nil))))

(defn chronology-start [opts]
  (let [revisions (source-revisions (or (:registered-run/source-roots opts)
                                        {"futon2" "/home/joe/code/futon2"
                                         "futon3-pattern-library" "/home/joe/code/futon3"}))
        config (select-keys opts [:author :reviewer :repair-reviewer :window-days
                                  :semantic-epoch :trigger :execution-cohort])
        prior (latest-prior-record (or (:run-record-dir opts)
                                       "/home/joe/code/futon2/data/wm-runs"))]
    {:schema :wm/registered-run-chronology-v1
     :source-revisions-before revisions
     :configuration-digest (digest config)
     :prior-opportunity prior}))

(defn chronology-finish [start opts refresh]
  (let [after (source-revisions (or (:registered-run/source-roots opts)
                                    {"futon2" "/home/joe/code/futon2"
                                     "futon3-pattern-library" "/home/joe/code/futon3"}))
        substrate (get refresh :freshness {:absent :refresh-not-recorded})]
    (assoc start
           :source-revisions-after after
           :substrate-snapshot-digest (digest substrate)
           :pattern-library-digest (get after "futon3-pattern-library")
           :intervening-change-receipts
           (let [prior (get-in start [:prior-opportunity :source-revisions-after])]
             (if (map? prior)
               (into (sorted-map)
                     (keep (fn [[id sha]]
                             (when (not= sha (get prior id))
                               [id {:before (get prior id) :after sha}])) after))
               {:absent :no-prior-registered-chronology})))))
