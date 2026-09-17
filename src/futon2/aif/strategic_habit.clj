(ns futon2.aif.strategic-habit
  "Forward-only strategic selections. Never reads or modifies scheduler counts."
  (:require [clojure.string :as str]))

(def state-version 2)
(def alpha 1.0)

(defn enabled? [opts env-value]
  (if (contains? opts :accumulate-strategic-habit?)
    (let [v (:accumulate-strategic-habit? opts)]
      (when-not (boolean? v)
        (throw (ex-info "strategic accumulation requires a boolean" {:value v})))
      v)
    (= "1" env-value)))

(defn- text? [v] (and (string? v) (not (str/blank? v))))

(defn load-state
  "Absent side on legacy traces means unobserved, not a measured zero prior."
  [state]
  (if (nil? state)
    {:version state-version :grain :strategic :alpha alpha
     :status :empty :empty-reason :forward-accumulation-not-started
     :counts {} :events {}}
    (do
      (when-not (and (= state-version (:version state))
                     (= :strategic (:grain state))
                     (= alpha (:alpha state))
                     (map? (:events state))
                     (every? (fn [[id e]]
                               (and (text? id) (text? (:policy-id e))
                                    (= :strategic (:grain e))
                                    (text? (:captured-at e))))
                             (:events state))
                     (= (:counts state)
                        (frequencies (map :policy-id (vals (:events state)))))
                     (if (seq (:events state))
                       (and (= :accumulating (:status state))
                            (not (contains? state :empty-reason)))
                       (and (= :empty (:status state))
                            (= :forward-accumulation-not-started
                               (:empty-reason state)))))
        (throw (ex-info "invalid strategic habit store" {:state state})))
      state)))

(defn first-acting-pattern
  "The enacted step of a cascade decision's chosen candidate: the first
   element of its :precedence (the same projection
   futon2.aif.decision-gate/first-acting-pattern and policy.clj's
   cascade-first-action use). Nil for anything that is not a cascade
   candidate with a non-empty precedence."
  [decision]
  (let [action (:action decision)]
    (when (and (map? action)
               (= :cascade-candidate (:kind action))
               (seq (:precedence action)))
      (first (:precedence action)))))

(defn abstention?
  "A typed abstention {:status :abstained :refusals […]}. An abstention
   enacts nothing, so there is no selection to observe."
  [decision]
  (and (map? decision) (= :abstained (:status decision))))

(defn accumulate
  "Fold the enacted first acting pattern of the chosen cascade — the policy
   identity the tick actually enacted — not a flat action (SPEC
   flat-removal H4, Joe's 2026-09-17 ruling). An abstention observes
   NOTHING: the store is returned unchanged with the abstention recorded
   nowhere, because no policy acted. Replays of an identical event are
   idempotent; conflicting event ids refuse."
  [previous decision event-id captured-at]
  (let [state (load-state previous)]
    (cond
      (abstention? decision) state

      :else
      (let [pattern (first-acting-pattern decision)
            policy-id (some-> pattern str)
            event {:grain :strategic :policy-id policy-id :captured-at captured-at
                   :boundary :reason-bearing-strategic-policy}]
        ;; A cascade decision is identified by its applied selection law, which
        ;; the decision gate also checks. The flat path's :selection-boundary
        ;; label is not the identity (claude-4 review: select-action-cascades
        ;; records :strategic-recommendation, so the old boundary check threw on
        ;; every real cascade decision).
        (when-not (and (= :cascade-selection-posterior
                          (get-in decision [:selection-law :applied]))
                       (text? policy-id) (text? event-id) (text? captured-at))
          (throw (ex-info "insufficient strategic selection identity"
                          {:decision decision :event-id event-id
                           :captured-at captured-at})))
        (if-let [old (get-in state [:events event-id])]
          (if (= old event) state
              (throw (ex-info "conflicting strategic selection event"
                              {:event-id event-id})))
          (-> state
              (dissoc :empty-reason)
              (assoc :status :accumulating :captured-at captured-at)
              (assoc-in [:events event-id] event)
              (update-in [:counts policy-id] (fnil inc 0))))))))

(defn carry
  "Off carries an existing store unchanged, and adds nothing to legacy traces."
  [previous decision event-id captured-at enabled]
  (if enabled
    (accumulate previous decision event-id captured-at)
    previous))

(defn require-promotable
  "A nonempty labelled store is necessary, not authorization to promote.
   No production selector calls this function in the accumulation-only phase."
  [state]
  (let [state (load-state state)]
    (when-not (seq (:counts state))
      (throw (ex-info "promotion requires nonempty grain-labelled strategic data"
                      {:reason (:empty-reason state)})))
    state))
