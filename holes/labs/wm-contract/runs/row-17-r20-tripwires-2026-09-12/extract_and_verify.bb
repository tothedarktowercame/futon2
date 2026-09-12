#!/usr/bin/env bb

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.string :as str])
(import '(java.security MessageDigest)
        '(java.time Instant))

(def ledger-path "/tmp/futon3c-invoke-jobs.edn")
(def out-dir "holes/labs/wm-contract/runs/row-17-r20-tripwires-2026-09-12")
(def terminal-types
  #{"done" "succeeded" "failed" "error" "timeout" "cancelled" "deduped"})

(defn sha256 [^String s]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn latest [jobs pred]
  (last (sort-by :created-at (filter pred jobs))))

(defn safe-event [event]
  (select-keys event [:seq :type :at :code :message :status :surface
                      :destination :recorded-at :note]))

(defn safe-job [class job]
  {:fixture/class class
   :job (-> (select-keys job [:job-id :agent-id :caller :surface :mode :state
                              :created-at :started-at :finished-at :terminal-code
                              :terminal-message :execution :delivery :event-seq
                              :events-trimmed :request-digest :trace-id :artifact-ref])
            (assoc :events (mapv safe-event (:events job))))})

(defn parse-time [s]
  (when s (Instant/parse s)))

(defn assert! [truth data]
  (when-not truth (throw (ex-info "Row 17 R20 fixture verification failed" data))))

(defn verify-job! [{class :fixture/class job :job}]
  (let [accepted (some #(when (= "accepted" (:type %)) %) (:events job))
        terminal-events (filter #(terminal-types (:type %)) (:events job))
        created (parse-time (:created-at job))
        accepted-at (parse-time (:at accepted))
        started (parse-time (:started-at job))
        finished (parse-time (:finished-at job))]
    (assert! accepted {:class class :failure :accepted-event-missing})
    (assert! (and created accepted-at (not (.isAfter created accepted-at)))
             {:class class :failure :created-after-accepted})
    (assert! (and started (not (.isAfter accepted-at started)))
             {:class class :failure :accepted-after-running})
    (assert! (and finished (not (.isAfter started finished)))
             {:class class :failure :running-after-terminal})
    (assert! (= 1 (count terminal-events))
             {:class class :failure :terminal-transition-count
              :count (count terminal-events)})
    (assert! (= (:state job) (:type (first terminal-events)))
             {:class class :failure :terminal-state-event-mismatch})
    (assert! (map? (:execution job)) {:class class :failure :execution-missing})
    (assert! (map? (:delivery job)) {:class class :failure :delivery-missing})
    true))

(let [source-text (slurp ledger-path)
      ledger (edn/read-string source-text)
      jobs (vals (:jobs ledger))
      predicates
      {:worker-lost-on-restart #(= "worker-lost-on-restart" (:terminal-code %))
       :invoke-no-execution-evidence #(= "invoke-no-execution-evidence" (:terminal-code %))
       :timeout-or-cancellation #(contains? #{"timeout" "cancelled"} (:state %))
       :delivery-failure #(= "delivery-failed" (get-in % [:delivery :status]))
       :successful-control #(and (= "done" (:state %))
                                 (true? (get-in % [:execution :executed]))
                                 (= "delivered" (get-in % [:delivery :status])))}
      selections (into {} (map (fn [[class pred]] [class (latest jobs pred)]) predicates))
      present (->> selections (keep (fn [[class job]] (when job (safe-job class job)))) vec)
      absent (->> selections (keep (fn [[class job]] (when-not job class))) sort vec)
      _ (doseq [fixture present] (verify-job! fixture))
      extraction-at (str (Instant/now))
      fixture {:schema :wm/agency-work-execution-fixture-v1
               :work-remaining/row 17
               :source {:path ledger-path
                        :sha256 (sha256 source-text)
                        :extracted-at extraction-at
                        :futon3c/head "3682c693a7e379dc8d86cf90716d34da5c478544"}
               :redaction {:retained "lifecycle identity, timestamps, outcome, execution, delivery, and bounded event metadata"
                           :removed [:prompt :result :result-text :result-summary
                                     :event/text :event/output :event/previews]}
               :records present
               :absences (mapv (fn [class]
                                 {:fixture/class class
                                  :status :absent
                                  :refusal :no-real-ledger-instance})
                               absent)}
      checks {:schema :wm/agency-work-execution-verification-v1
              :work-remaining/row 17
              :source-sha256 (get-in fixture [:source :sha256])
              :at extraction-at
              :ok true
              :records-checked (count present)
              :assertions (* 8 (count present))
              :checks [:created-before-or-at-accepted
                       :accepted-before-or-at-running
                       :running-before-or-at-terminal
                       :exactly-one-retained-terminal-transition
                       :terminal-state-matches-event
                       :execution-retained
                       :delivery-retained]
              :absent-classes absent}
      credit {:schema :wm/working-evidence-proposal-v1
              :work-remaining/row 17
              :node :R20
              :status :proposed
              :credit "Agency work-execution tripwires"
              :claim "The Agency invoke lifecycle records and types worker loss, cancellation, and delivery failure while retaining execution and outcome fields; the selected successful record is the positive control."
              :maximum-claim "This credits only the evidenced work-execution lifecycle properties. It does not credit the catalogue-wide R20 interoceptive-tripwire weave, calibration, blind-spot map, or universal freeze/record/park/summon action. VERIFY-r-nodes.edn's R20 zeros remain unchanged."
              :evidence {:fixture "fixture.edn"
                         :verification "verification.edn"
                         :source-sha256 (get-in fixture [:source :sha256])}
              :retention {:policy "Detailed terminal transcripts compact after 24 hours; unreferenced tombstones after seven days."
                          :finding "worker-lost was already compacted; the other selected rows were extracted with their retained ledger representation. The bounded fixture preserves only claim-relevant fields."}
              :per-fixture (mapv (fn [{class :fixture/class job :job}]
                                   {:fixture/class class
                                    :job-id (:job-id job)
                                    :property (case class
                                                :worker-lost-on-restart "restart recovery records an orphaned running worker as failed"
                                                :timeout-or-cancellation "cancellation is a terminal recorded outcome"
                                                :delivery-failure "delivery failure remains explicit after work terminalizes"
                                                :successful-control "executed successful work and delivery remain explicit")})
                                 present)
              :typed-absences (:absences fixture)}]
  (.mkdirs (io/file out-dir))
  (spit (str out-dir "/fixture.edn") (str (pr-str fixture) "\n"))
  (spit (str out-dir "/verification.edn") (str (pr-str checks) "\n"))
  (spit (str out-dir "/working-evidence.edn") (str (pr-str credit) "\n"))
  (println (pr-str {:ok true :records (count present) :absent absent
                    :source-sha256 (get-in fixture [:source :sha256])})))
