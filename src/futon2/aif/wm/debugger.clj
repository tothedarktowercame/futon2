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

(defn attach! []
  (reset! !attached? true)
  {:attached true})

(defn detach! []
  (reset! !attached? false)
  {:attached false})

(defn attached? [] @!attached?)

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
  (->> @!stops vals (mapv #(dissoc % :decision :throwable))
       (sort-by (juxt :stopped-at :run-id)) vec))

(defn continue!
  "Deliver CHOICE to stopped RUN-ID. Choice is :retry, :abort, or
  [:use-value value]. The restart itself is invoked by the waiting run thread."
  [run-id choice]
  (when-not (or (#{:retry :abort} choice)
                (and (vector? choice) (= :use-value (first choice)) (= 2 (count choice))))
    (throw (ex-info "Unknown debugger restart choice"
                    {:kind :debugger/invalid-choice :run-id run-id :choice choice})))
  (let [stop (get @!stops run-id)]
    (when-not stop
      (throw (ex-info "Run is not stopped"
                      {:kind :debugger/run-not-stopped :run-id run-id})))
    (if (deliver (:decision stop) choice)
      {:run-id run-id :choice choice :delivered true}
      (throw (ex-info "A restart choice was already delivered"
                      {:kind :debugger/already-continued :run-id run-id})))))

(defn- condition-kind [throwable]
  (let [data (ex-data throwable)]
    (or (:kind data) (:failure-kind data) (:reason data)
        (get-in data [:judge-refusal :kind])
        :wm/phase-failure)))

(defn await-restart!
  "Signal THROWABLE as a phase condition and wait for an operator restart.
  Returns {:action ...}; it never invokes a restart from the operator thread."
  [{:keys [run-id attempt-id opportunity-id phase]} throwable]
  (let [decision (promise)
        condition {:kind (condition-kind throwable)
                   :class (.getName (class throwable))
                   :message (.getMessage throwable)}
        entry {:run-id run-id :attempt-id attempt-id
               :opportunity-id opportunity-id :phase phase
               :condition condition :ex-data (ex-data throwable)
               :stopped-at (str (Instant/now))
               :decision decision :throwable throwable}]
    #_{:clj-kondo/ignore [:unresolved-symbol]}
    (restart-case
      (handler-bind
       [::phase-failure
        (fn [_ _condition]
          (swap! !stops assoc run-id entry)
          (try
            (let [choice @decision]
              (cond
                (= :retry choice) (far/invoke-restart ::retry)
                (= :abort choice) (far/invoke-restart ::abort)
                (= :use-value (first choice))
                (far/invoke-restart ::use-value (second choice))))
            (finally
              (swap! !stops dissoc run-id))))]
       (far/error ::phase-failure entry))
      (::retry [] {:action :retry})
      (::use-value [value] {:action :use-value :value value})
      (::abort [] {:action :abort}))))
