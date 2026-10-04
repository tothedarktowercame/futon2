(ns futon2.aif.registered-run-telemetry
  "Evidence projection used by registered War Machine opportunities."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(defn phase-timings
  ([events] (phase-timings events nil nil))
  ([events dwell-ledger run-id]
   (let [ends (filter #(and (= :end (:transition %))
                            (number? (:duration-ms %))) events)
         raw (reduce (fn [m {:keys [phase duration-ms]}]
                       (update m phase (fnil + 0) duration-ms)) {} ends)
         ledger (some-> dwell-ledger deref)
         receipts (:receipts ledger)
         errors (:errors ledger)
         valid? (and (or (nil? dwell-ledger) (= run-id (:run-id ledger)))
                     (empty? errors)
                     (every? (fn [{:keys [schema run-id phase condition-kind
                                          stopped-at-monotonic-ns
                                          resumed-at-monotonic-ns restart-choice
                                          duration-ms]}]
                               (and (= :wm/debugger-dwell-v1 schema)
                                    (= run-id (:run-id ledger))
                                    (keyword? phase) (keyword? condition-kind)
                                    (integer? stopped-at-monotonic-ns)
                                    (integer? resumed-at-monotonic-ns)
                                    (<= stopped-at-monotonic-ns
                                        resumed-at-monotonic-ns)
                                    (contains? #{:retry :abort :use-value}
                                               restart-choice)
                                    (= duration-ms
                                       (quot (- resumed-at-monotonic-ns
                                                stopped-at-monotonic-ns)
                                             1000000))))
                             receipts))
         accepted (if valid? receipts [])
         dwell-by-phase (reduce (fn [m {:keys [phase duration-ms]}]
                                  (update m phase (fnil + 0) duration-ms))
                                {} accepted)
         adjusted (reduce-kv (fn [m phase duration-ms]
                               (assoc m phase
                                      (max 0 (- duration-ms
                                                (get dwell-by-phase phase 0)))))
                             {} raw)
         base {:status (if (seq ends) :complete :typed-missing)
               :clock :monotonic :phase-timings-ms adjusted}]
     (if (nil? dwell-ledger)
       base
       (assoc base
              :phase-wall-timings-ms raw
              :debugger-stopped-ms (reduce + 0 (map :duration-ms accepted))
              :debugger-stopped-by-phase-ms dwell-by-phase
              :debugger-dwell-receipts (vec accepted)
              :debugger-dwell-status (if valid? :complete :typed-missing)
              :debugger-dwell-errors
              (cond-> (vec errors)
                (and dwell-ledger (not= run-id (:run-id ledger)))
                (conj {:reason :debugger-dwell-run-mismatch
                       :expected-run-id run-id
                       :ledger-run-id (:run-id ledger)})))))))

(defn- normalized-usage [usage]
  (let [input (or (:input-tokens usage) (:input_tokens usage)
                  (:prompt-tokens usage) (:prompt_tokens usage)
                  (:cost/input-tokens usage))
        output (or (:output-tokens usage) (:output_tokens usage)
                   (:completion-tokens usage) (:completion_tokens usage)
                   (:cost/output-tokens usage))
        total (or (:total-tokens usage) (:total_tokens usage)
                  (:cost/total-tokens usage)
                  (when (and (integer? input) (integer? output)) (+ input output)))
        cached (or (:cached-input-tokens usage) (:cached_input_tokens usage)
                   (:cost/cached-input-tokens usage))]
    (when (and (integer? input) (<= 0 input)
               (integer? output) (<= 0 output)
               (= total (+ input output)))
      (cond-> {:input-tokens input :output-tokens output :total-tokens total}
        (and (integer? cached) (<= 0 cached) (<= cached input))
        (assoc :cached-input-tokens cached
               :uncached-input-tokens (- input cached))
        (or (:model usage) (:cost/model usage))
        (assoc :model (or (:model usage) (:cost/model usage)))
        (or (:source usage) (:cost/source usage))
        (assoc :provider (or (:source usage) (:cost/source usage)))))))

(defn- enum=
  "Compare an enum across the EDN producer and JSON transport boundary."
  [expected observed]
  (= (name expected)
     (cond
       (keyword? observed) (name observed)
       (string? observed) observed
       :else nil)))

(defn register-job!
  "Register one WM dispatch at the point Agency returns its job identity."
  [ledger {:keys [run-id click-id job-id] :as entry}]
  (when-not (and (instance? clojure.lang.IAtom ledger)
                 (every? #(and (string? %) (not-empty %))
                         [run-id click-id job-id]))
    (throw (ex-info "WM job registration lacks run/click/job identity"
                    {:failure-kind :wm-job-ledger-registration-invalid
                     :entry entry})))
  (swap! ledger
         (fn [state]
           (when (get-in state [:jobs job-id])
             (throw (ex-info "WM job registered more than once"
                             {:failure-kind :wm-job-ledger-duplicate-job
                              :job-id job-id})))
           (-> state
               (update :order (fnil conj []) job-id)
               (assoc-in [:jobs job-id] (assoc entry :status :dispatched)))))
  entry)

(defn retain-terminal-job!
  "Attach the exact terminal Agency job map to its dispatch-ledger entry."
  [ledger job]
  (let [job-id (:job-id job)]
    (swap! ledger
           (fn [state]
             (cond
               (nil? (get-in state [:jobs job-id]))
               (update state :errors (fnil conj [])
                       {:reason :terminal-job-unregistered :job-id job-id})

               (get-in state [:jobs job-id :terminal])
               (update state :errors (fnil conj [])
                       {:reason :terminal-job-duplicated :job-id job-id})

               :else
               (-> state
                   (assoc-in [:jobs job-id :status] :terminal)
                   (assoc-in [:jobs job-id :terminal] job))))))
  job)

(defn model-usage
  "Project provider usage solely from the run-local dispatch ledger."
  [ledger {:keys [run-id click-id]}]
  (let [{:keys [order jobs errors]} (or (some-> ledger deref) {})
        rows (mapv
              (fn [job-id]
                (let [{:keys [terminal role phase] :as entry} (get jobs job-id)
                      harness (:harness terminal)
                      joined? (and (= run-id (:run-id entry))
                                   (= click-id (:click-id entry))
                                   (= job-id (:job-id terminal))
                                   (enum= :war-machine (:kind harness))
                                   (enum= :producer-context (:basis harness))
                                   (= run-id (:execution-id harness))
                                   (= click-id (:source-ref harness)))
                      usage (when joined? (normalized-usage (:usage terminal)))
                      reason (cond
                               (nil? terminal) :terminal-receipt-missing
                               (not (contains? #{"done" "failed" "cancelled" "timed-out"}
                                               (:state terminal))) :job-nonterminal
                               (not joined?) :run-click-job-join-mismatch
                               (nil? usage) :provider-usage-missing-or-invalid)]
                  (cond-> {:job-id job-id :run-id (:run-id entry)
                           :click-id (:click-id entry) :role role :phase phase
                           :status (if reason :typed-missing :complete)
                           :unit :tokens}
                    reason (assoc :reason reason)
                    usage (merge usage))))
              (or order []))
        valid (filterv #(= :complete (:status %)) rows)
        missing (filterv #(not= :complete (:status %)) rows)
        all-valid? (and (seq rows) (empty? missing) (empty? errors))]
    (cond-> {:status (cond all-valid? :complete
                           (seq valid) :partial
                           :else :typed-missing)
             :source :wm-run-local-agency-ledger
             :unit :tokens
             :jobs rows}
      (seq missing) (assoc :missing (mapv #(select-keys % [:job-id :reason]) missing))
      (seq errors) (assoc :ledger-errors errors)
      (empty? rows) (assoc :reason :no-wm-jobs-dispatched)
      (seq valid)
      (merge (reduce (fn [m row]
                       (-> m
                           (update :input-tokens + (:input-tokens row))
                           (update :output-tokens + (:output-tokens row))
                           (update :total-tokens + (:total-tokens row))
                           (update :cached-input-tokens + (or (:cached-input-tokens row) 0))
                           (update :uncached-input-tokens + (or (:uncached-input-tokens row)
                                                               (:input-tokens row)))))
                     {:input-tokens 0 :output-tokens 0 :total-tokens 0
                      :cached-input-tokens 0 :uncached-input-tokens 0}
                     valid)))))

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
      (let [r (edn/read-string (slurp f))]
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
