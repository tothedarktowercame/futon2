(ns futon2.aif.wm.debugger
  "An attachable restart debugger for the one real War Machine runner.

  A failing run thread signals a Farolero condition with restarts in dynamic
  scope, registers its public stop description, and blocks on a promise.  An
  operator thread only delivers the choice; the run thread invokes its own
  restart, so Farolero's non-local control transfer never crosses threads."
  (:require [farolero.core :as far :refer [handler-bind restart-case]])
  (:import [java.time Instant]))

(defonce ^:private !attached? (atom false))
(defonce ^:private !stops (atom {}))
(defonce ^:private !breakpoints (atom #{}))

(def dwell-schema :wm/debugger-dwell-v1)

(defn new-dwell-ledger [run-id]
  (atom {:run-id run-id :receipts [] :errors []}))

(defn attach! []
  (reset! !attached? true)
  {:attached true})

(defn detach! []
  (reset! !attached? false)
  {:attached false})

(defn attached? [] @!attached?)

(defn armed-breakpoints []
  (vec (sort-by pr-str @!breakpoints)))

(defn arm-breakpoint! [breakpoint]
  (swap! !breakpoints conj breakpoint)
  {:armed (armed-breakpoints)})

(defn clear-breakpoint! [breakpoint]
  (swap! !breakpoints disj breakpoint)
  {:armed (armed-breakpoints)})

(defn breakpoint-armed? [breakpoint]
  (contains? @!breakpoints breakpoint))

(defn stoppable-failure?
  "True when THROWABLE is a failure that the runner would close as a failure.

  Typed judge and gate refusals are failures too.  Detached runs retain the
  existing close behaviour because run-phase! consults this predicate only
  after checking attached?.  An attached run stops before its surrounding
  close handler, so the operator can repair and retry the same phase or abort
  to preserve the ordinary terminal refusal."
  [_throwable]
  true)

(defn stopped
  "Public descriptions of run threads currently waiting for a restart."
  []
  (->> @!stops vals
       (mapv (fn [stop]
               (-> stop
                   (dissoc :decision :throwable :stop-value :has-stop-value?)
                   (update :ex-data dissoc :debugger/result))))
       (sort-by (juxt :stopped-at :run-id)) vec))

(defn stop-value
  "Return the retained computed value for stopped RUN-ID.
  Failure stops do not have such a value."
  [run-id]
  (let [stop (get @!stops run-id)]
    (when-not stop
      (throw (ex-info "Run is not stopped"
                      {:kind :debugger/run-not-stopped :run-id run-id})))
    (when-not (:has-stop-value? stop)
      (throw (ex-info "Stopped condition has no computed value"
                      {:kind :debugger/stop-value-unavailable :run-id run-id})))
    (:stop-value stop)))

(defn continue!
  "Deliver CHOICE to stopped RUN-ID. Choice is :continue, :retry, :abort, or
  [:use-value value]. The restart itself is invoked by the waiting run thread."
  [run-id choice]
  (when-not (or (#{:continue :retry :abort} choice)
                (and (vector? choice) (= :use-value (first choice)) (= 2 (count choice))))
    (throw (ex-info "Unknown debugger restart choice"
                    {:kind :debugger/invalid-choice :run-id run-id :choice choice})))
  (let [stop (get @!stops run-id)]
    (when-not stop
      (throw (ex-info "Run is not stopped"
                      {:kind :debugger/run-not-stopped :run-id run-id})))
    (when (and (= :continue choice) (not (:has-stop-value? stop)))
      (throw (ex-info "Continue requires a retained phase result"
                      {:kind :debugger/invalid-choice-for-stop
                       :run-id run-id :choice choice
                       :condition-kind (get-in stop [:condition :kind])})))
    (if (deliver (:decision stop) choice)
      {:run-id run-id
       :choice (if (vector? choice) (first choice) choice)
       :delivered true}
      (throw (ex-info "A restart choice was already delivered"
                      {:kind :debugger/already-continued :run-id run-id})))))

(defn- condition-kind [throwable]
  (let [data (ex-data throwable)]
    (or (:kind data) (:failure-kind data) (:reason data)
        (get-in data [:judge-refusal :kind])
        :wm/phase-failure)))

(defn- retain-dwell! [ledger receipt]
  (when ledger
    (swap! ledger
           (fn [state]
             (if (= (:run-id state) (:run-id receipt))
               (update state :receipts (fnil conj []) receipt)
               (update state :errors (fnil conj [])
                       {:reason :debugger-dwell-run-mismatch
                        :ledger-run-id (:run-id state)
                        :receipt-run-id (:run-id receipt)})))))
  receipt)

(defn- value-summary [value]
  (if (map? value)
    {:top-level-keys (vec (sort-by pr-str (keys value)))}
    {:type (if (nil? value) "nil" (.getName (class value))) }))

(defn await-restart!
  "Signal THROWABLE as a phase condition and wait for an operator restart.
  Returns {:action ...}; it never invokes a restart from the operator thread."
  [{:keys [run-id attempt-id opportunity-id phase nano-time-fn
           debugger-dwell-ledger debugger-stop-value debugger-has-stop-value?]}
   throwable]
  (let [decision (promise)
        nano-time (or nano-time-fn #(System/nanoTime))
        stopped-ns (nano-time)
        condition {:kind (condition-kind throwable)
                   :class (.getName (class throwable))
                   :message (.getMessage throwable)}
        entry (cond-> {:run-id run-id :attempt-id attempt-id
               :opportunity-id opportunity-id :phase phase
               :condition condition :ex-data (ex-data throwable)
               :stopped-at (str (Instant/now))
               :stopped-at-monotonic-ns stopped-ns
               :decision decision :throwable throwable}
                debugger-has-stop-value?
                (assoc :has-stop-value? true
                       :stop-value debugger-stop-value
                       :result-summary (value-summary debugger-stop-value)))]
    #_{:clj-kondo/ignore [:unresolved-symbol]}
    (restart-case
      (handler-bind
       [::phase-failure
        (fn [_ _condition]
          (swap! !stops assoc run-id entry)
          (try
            (let [choice @decision
                  resumed-ns (nano-time)
                  restart-choice (if (vector? choice) (first choice) choice)
                  receipt (retain-dwell!
                           debugger-dwell-ledger
                           {:schema dwell-schema
                            :run-id run-id
                            :phase phase
                            :condition-kind (:kind condition)
                            :stopped-at-monotonic-ns stopped-ns
                            :resumed-at-monotonic-ns resumed-ns
                            :restart-choice restart-choice
                            :duration-ms (quot (max 0 (- resumed-ns stopped-ns))
                                               1000000)})]
              (cond
                (= :continue choice)
                (far/invoke-restart ::continue debugger-stop-value receipt)
                (= :retry choice) (far/invoke-restart ::retry receipt)
                (= :abort choice) (far/invoke-restart ::abort receipt)
                (= :use-value (first choice))
                (far/invoke-restart ::use-value (second choice) receipt)))
            (finally
              (swap! !stops dissoc run-id))))]
       (far/error ::phase-failure entry))
      (::retry [receipt] {:action :retry :debugger-dwell receipt})
      (::continue [value receipt]
        {:action :continue :value value :debugger-dwell receipt})
      (::use-value [value receipt]
        {:action :use-value :value value :debugger-dwell receipt})
      (::abort [receipt] {:action :abort :debugger-dwell receipt}))))
