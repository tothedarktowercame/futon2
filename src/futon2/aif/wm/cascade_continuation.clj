(ns futon2.aif.wm.cascade-continuation
  "Digest-bound continuation from :cascade-ready through verified frontier
  effects to the existing close port.  This function never selects and never
  spends a click; every effect must be supplied by a registered capability
  executor and survive journal validation before close is callable."
  (:require [clojure.java.io :as io]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.wm.cascade-capability-dispatch :as dispatch]
            [futon2.aif.wm.executable-cascade :as cascade]
            [futon2.aif.wm.execution-journal :as journal])
  (:import [java.time Instant]))

(def schema :wm/cascade-continuation-v1)

(defn- hex64? [x]
  (and (string? x) (re-matches #"[0-9a-f]{64}" x)))

(defn- fail [kind data]
  {:status :error :kind kind :repair data})

(defn- load-compiled [root digest]
  (let [path (io/file root (str digest ".edn"))]
    (when (.isFile path) (cascade/load-record path))))

(defn continue-certified-cascade!
  "Consume one certified :cascade-ready carrier.

  EXECUTORS maps a capability to an executor returning a sealed verified
  effect receipt. CLOSE-FN is called only after every frontier occurrence has
  such a receipt and the append-only execution journal certifies. It receives
  the complete evidence packet and must return a digest-bound :closed result.
  Missing ports and malformed evidence are typed errors, never abstentions."
  [{:keys [ready executable-cascade-root execution-journal-root executors
           close-fn instant-fn]
    :or {instant-fn #(Instant/now)}}]
  (let [selected (:cascade-digest ready)
        compiled (:compiled-digest ready)
        routed (get-in ready [:capability-routing :steps])]
    (cond
      (not= :cascade-ready (:status ready))
      (fail :continuation-not-ready {:status (:status ready)})
      (not (and (hex64? selected) (hex64? compiled)))
      (fail :continuation-digest-invalid
            {:selected selected :compiled compiled})
      (not (sequential? routed))
      (fail :continuation-routing-absent {})
      (not (map? executors))
      (fail :continuation-executors-absent {})
      (not (ifn? close-fn))
      (fail :continuation-close-port-absent {})
      :else
      (let [record (load-compiled executable-cascade-root compiled)]
        (cond
          (nil? record)
          (fail :continuation-cascade-absent {:digest compiled})
          (not= compiled (:digest record))
          (fail :continuation-cascade-digest-mismatch
                {:expected compiled :found (:digest record)})
          :else
          (let [validation (cascade/validate-cascade record)
                frontier (:frontier record)
                expected (mapv :occurrence-id frontier)
                offered (mapv :occurrence-id routed)]
            (cond
              (not (cascade/certified? validation))
              (fail :continuation-cascade-not-certified
                    {:validation validation})
              (not= expected offered)
              (fail :continuation-routing-mismatch
                    {:expected expected :offered offered})
              :else
              (loop [nodes frontier receipts [] appends [] n 0]
                (if-let [node (first nodes)]
                  (let [effect (dispatch/dispatch-node executors compiled node)]
                    (if-not (= :enacted (:status effect))
                      (assoc effect :status :error
                             :kind (or (:kind effect)
                                       :continuation-effect-failed)
                             :completed receipts)
                      (let [receipt (:receipt effect)
                            now (instant-fn)
                            entry (journal/entry
                                   {:journal-id compiled
                                    :node-or-group (:occurrence-id node)
                                    :phase :enacted
                                    :evidence
                                    {:kind :verified-effect
                                     :digest (:digest receipt)}
                                    :valid-time (str now)
                                    :recorded-time
                                    (str (.plusNanos ^Instant now (inc n)))})]
                        (if (:refusal entry)
                          (fail :continuation-journal-entry-invalid
                                (:refusal entry))
                          (recur (next nodes)
                                 (conj receipts receipt)
                                 (conj appends
                                       (journal/append!
                                        execution-journal-root entry))
                                 (inc n))))))
                  (let [entries (journal/load-journal execution-journal-root)
                        idx (journal/cascade-index record)
                        certificate (journal/validate-journal entries idx)
                        replay (when (journal/certified? certificate)
                                 (journal/replay entries))]
                    (cond
                      (not (journal/certified? certificate))
                      (fail :continuation-journal-invalid
                            {:validation certificate})
                      (not (every? (:enacted replay) expected))
                      (fail :continuation-frontier-not-enacted
                            {:expected expected
                             :enacted (vec (:enacted replay))})
                      :else
                      (let [closed (close-fn
                                    {:schema schema
                                     :selected-cascade-digest selected
                                     :cascade-digest compiled
                                     :frontier expected
                                     :effect-receipts receipts
                                     :journal-certificate certificate
                                     :journal-replay replay})]
                        (if (and (= :closed (:status closed))
                                 (= compiled (:cascade-digest closed))
                                 (= :verified-effect
                                    (get-in closed [:close-receipt
                                                    :evidence-kind]))
                                 (hex64? (get-in closed [:close-receipt
                                                        :digest])))
                          {:status :closed
                           :schema schema
                           :selected-cascade-digest selected
                           :cascade-digest compiled
                           :effect-receipts receipts
                           :journal-append appends
                           :journal-certificate certificate
                           :close-result closed
                           :selection-ran false
                           :clicks-spent 0}
                          (fail :continuation-close-unverified
                                {:close-result closed}))))))))))))))
