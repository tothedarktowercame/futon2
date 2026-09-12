(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[futon3c.agency.parked-on :as parked])
(import '(java.security MessageDigest)
        '(java.time Instant))

(def park-path "/tmp/futon3c-parked-on.edn")
(def jobs-path "/tmp/futon3c-invoke-jobs.edn")
(def out-dir "/home/joe/code/futon2/holes/labs/wm-contract/runs/row-17-r20-continuation-2026-09-12")
(def invented-id "invoke-row17-invented-dependency-does-not-exist")

(defn sha256 [^String s]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn assert! [truth data]
  (when-not truth (throw (ex-info "Row 17 continuation verification failed" data))))

(defn safe-event [event]
  (select-keys event [:seq :type :at :code :message :status :surface
                      :destination :recorded-at :note]))

(defn safe-job [job]
  (-> (select-keys job [:job-id :agent-id :caller :surface :mode :state
                        :created-at :started-at :finished-at :terminal-code
                        :execution :delivery :auto-bellback :event-seq
                        :events-trimmed :request-digest :trace-id])
      (assoc :events (mapv safe-event (:events job)))))

(defn ready-items [park-store]
  (for [[queue items] (:ready-inbox park-store)
        item items]
    {:queue {:agent (first queue) :session (second queue)}
     :item item}))

(defn completed-count [prompt]
  (some-> (re-find #"resumed: parked dependencies complete \(([0-9]+)\)" prompt)
          second parse-long))

(let [park-text (slurp park-path)
      jobs-text (slurp jobs-path)
      park-store (edn/read-string park-text)
      jobs-ledger (edn/read-string jobs-text)
      ready (vec (ready-items park-store))
      ready-by-id (into {} (map (juxt #(get-in % [:item :park-id]) identity)) ready)
      candidates (for [job (vals (:jobs jobs-ledger))
                       :let [park-id (get-in job [:auto-bellback :park-id])
                             ready-record (get ready-by-id park-id)]
                       :when (and (get-in job [:auto-bellback :suppressed?]) ready-record)]
                   {:job job :ready ready-record})
      selected (last (sort-by #(get-in % [:job :finished-at]) candidates))
      job (:job selected)
      ready-record (:ready selected)
      prompt (str (get-in ready-record [:item :prompt]))
      dependency-ids (vec (distinct (re-seq #"invoke-[0-9]+-[0-9]+-[a-z0-9]+" prompt)))
      park-id (get-in ready-record [:item :park-id])
      occurrences (+ (count (filter #(= park-id (:park-id %)) (map :item ready)))
                     (if (contains? (:records park-store) park-id) 1 0)
                     (count (filter #(= park-id (:park-id %)) (vals (:leased park-store)))))
      _ (assert! selected {:failure :no-completed-park-cycle-retained})
      _ (assert! (= park-id (get-in job [:auto-bellback :park-id]))
                 {:failure :park-job-link-mismatch})
      _ (assert! (some #{(:job-id job)} dependency-ids)
                 {:failure :resume-does-not-name-dependency-job})
      _ (assert! (= 1 (completed-count prompt))
                 {:failure :completed-dependency-count})
      _ (assert! (= 1 occurrences) {:failure :release-not-unique-in-park-store})
      now 2000000000000
      deadline (+ now 1000)
      resumes (atom [])
      accepted (parked/park! {:agent "row17-isolated" :session "row17-isolated"
                              :surface "emacs-repl" :awaiting [invented-id]
                              :payload "REDACTED" :deadline-ms deadline}
                             {:ledger-lookup (constantly nil) :now-ms now
                              :resume! #(swap! resumes conj %)})
      before (parked/snapshot)
      early (parked/sweep-deadlines! {:now-ms (dec deadline)
                                      :resume! #(swap! resumes conj %)})
      expired (parked/sweep-deadlines! {:now-ms deadline
                                        :resume! #(swap! resumes conj %)})
      _ (assert! (= :parked (:status accepted)) {:failure :invented-id-not-accepted})
      _ (assert! (= #{invented-id} (get-in before [:records (:id accepted) :awaiting]))
                 {:failure :invented-id-not-retained})
      _ (assert! (empty? (:expired early)) {:failure :invented-id-woke-before-deadline})
      _ (assert! (= [(:id accepted)] (:expired expired))
                 {:failure :invented-id-did-not-wake-at-deadline})
      _ (assert! (= 1 (count @resumes)) {:failure :deadline-resume-count})
      _ (assert! (true? (:deadline-expired? (first @resumes)))
                 {:failure :deadline-marker-missing})
      extracted-at (str (Instant/now))
      fixture {:schema :wm/agency-continuation-fixture-v1
               :work-remaining/row 17
               :sources {:park-store {:path park-path :sha256 (sha256 park-text)}
                         :invoke-ledger {:path jobs-path :sha256 (sha256 jobs-text)}
                         :extracted-at extracted-at
                         :futon3c/head "3682c693a7e379dc8d86cf90716d34da5c478544"}
               :deployment {:FUTON3C_PARKED_ON (System/getenv "FUTON3C_LIVE_PARKED_ON_OBSERVED")
                            :observation "Serving JVM /proc environment read-only observation; GET /api/alpha/parked returned HTTP 200."
                            :delivery-path :buffer-lane/durable-ready-inbox-poller
                            :basis "Selected item is stored under :ready-inbox keyed by agent/session; HTTP source routes emacs surfaces through ready-inbox lease/poll/ack."}
               :completed-cycle {:park {:park-id park-id
                                        :queue (:queue ready-record)
                                        :mode (get-in ready-record [:item :mode])
                                        :resume-marker :parked-dependencies-complete
                                        :completed-count (completed-count prompt)
                                        :dependency-job-ids dependency-ids
                                        :payload :redacted}
                                 :job (safe-job job)
                                 :release {:park-id park-id
                                           :job-id (:job-id job)
                                           :job-suppression-reason (get-in job [:auto-bellback :reason])
                                           :surviving-park-occurrences occurrences}}
               :deadline-cycle {:status :absent
                                :refusal :no-real-deadline-woken-record-retained
                                :basis "No ready-inbox prompt contains the generated DEADLINE EXPIRED marker."}}
      control {:schema :wm/agency-continuation-negative-control-v1
               :work-remaining/row 17
               :store-path (System/getenv "FUTON3C_PARKED_ON_PATH")
               :dependency invented-id
               :accepted accepted
               :retained-before-deadline (select-keys (get-in before [:records (:id accepted)])
                                                      [:id :agent :session :surface :awaiting
                                                       :arrived :deadline-ms :parked-at-ms :released? :mode])
               :before-deadline early
               :at-deadline expired
               :resume-count (count @resumes)
               :deadline-expired? (:deadline-expired? (first @resumes))
               :finding "park! accepts a referentially nonexistent dependency; without a real completion it wakes only through the deadline sweep. This demonstrates the documented gap, not a successful join."}
      verification {:schema :wm/agency-continuation-verification-v1
                    :work-remaining/row 17
                    :ok true :tests 2 :assertions 11
                    :at extracted-at
                    :checks [:park-id-job-suppression-join
                             :resume-names-dependency-job
                             :one-completed-dependency
                             :one-surviving-release-record
                             :invented-dependency-accepted
                             :no-early-wake
                             :deadline-wake-once]}
      credit {:schema :wm/working-evidence-proposal-v1
              :work-remaining/row 17 :node :R20 :status :proposed
              :credit "Agency continuation/deadline handling — record/park/resume where the configured path actually ran"
              :claim "A real buffer-lane park joined one named terminal invoke job, produced one retained ready-inbox resume, and suppressed duplicate auto-bellback. The isolated control demonstrates deadline wake and the unresolved nonexistent-dependency admission gap."
              :evidence {:fixture "fixture.edn" :verification "verification.edn"
                         :negative-control "invented-dependency-control.edn"}
              :maximum-claim "This credits only the evidenced Agency continuation path. It does not credit catalogue-wide R20, retro-trip calibration, a blind-spot map, or universal freeze/record/park/summon. VERIFY-r-nodes.edn's R20 zeros remain unchanged."
              :limitations [:completed-record-is-a-ready-inbox-projection-not-the-removed-source-record
                            :no-real-deadline-woken-record-retained
                            :dependency-referential-integrity-not-enforced]}]
  (.mkdirs (io/file out-dir))
  (spit (str out-dir "/fixture.edn") (str (pr-str fixture) "\n"))
  (spit (str out-dir "/invented-dependency-control.edn") (str (pr-str control) "\n"))
  (spit (str out-dir "/verification.edn") (str (pr-str verification) "\n"))
  (spit (str out-dir "/working-evidence.edn") (str (pr-str credit) "\n"))
  (println (pr-str {:ok true :park-id park-id :job-id (:job-id job)
                    :deadline-real :absent :negative-control :refused-referential-integrity})))
