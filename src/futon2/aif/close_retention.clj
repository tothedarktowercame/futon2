(ns futon2.aif.close-retention
  "Pure construction and validation of the close-retention v1 carrier."
  (:require [clojure.string :as str])
  (:import (java.security MessageDigest)
           (java.time Instant)))

(def occurrence-schema :wm/action-transition-occurrence-v1)
(def retention-schema :wm/close-retention-v1)
(def status-support
  #{:spawned :refined :strengthened :addressed :falsified :foreclosed :reopened})

(defn- refuse! [code path & [data]]
  (throw (ex-info "Close retention refused"
                  (merge {:close-retention/refusal code :path path} data))))

(defn- exact-map! [x ks path]
  (when-not (and (map? x) (= ks (set (keys x))))
    (refuse! :shape-invalid path {:expected ks :actual (some-> x keys set)}))
  x)

(defn- text! [x path]
  (when-not (and (string? x) (not (str/blank? x)))
    (refuse! :identity-invalid path))
  x)

(defn- instant! [x path]
  (try
    (Instant/parse (text! x path))
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Throwable _ (refuse! :timestamp-invalid path))))

(defn- sha256 [s]
  (let [bytes (.digest (MessageDigest/getInstance "SHA-256")
                       (.getBytes ^String s "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and 0xff %)) bytes))))

(defn- minted-id! [kind value path]
  (text! value path)
  (let [prefix (str (name kind) "-")]
    (when-not (and (str/starts-with? value prefix)
                   (try
                     (java.util.UUID/fromString (subs value (count prefix)))
                     true
                     (catch Throwable _ false)))
      (refuse! (if (= kind :action) :action-id-coercion
                   :transition-id-coercion)
               path)))
  value)

(defn validate-occurrence [occurrence]
  (exact-map! occurrence
              #{:schema :run/id :cohort/id :attempt/id :transition/id
                :action/id :action/value :action/value-sha256 :action-at}
              [:occurrence])
  (when-not (= occurrence-schema (:schema occurrence))
    (refuse! :schema-mismatch [:occurrence :schema]))
  (doseq [k [:run/id :cohort/id :attempt/id]]
    (text! (get occurrence k) [:occurrence k]))
  (minted-id! :transition (:transition/id occurrence) [:occurrence :transition/id])
  (minted-id! :action (:action/id occurrence) [:occurrence :action/id])
  (when (nil? (:action/value occurrence))
    (refuse! :action-value-missing [:occurrence :action/value]))
  (let [actual (sha256 (pr-str (:action/value occurrence)))]
    (when-not (= actual (:action/value-sha256 occurrence))
      (refuse! :occurrence-action-drift [:occurrence :action/value-sha256]
               {:expected (:action/value-sha256 occurrence) :actual actual})))
  (instant! (:action-at occurrence) [:occurrence :action-at])
  occurrence)

(defn mint-occurrence
  "Mint immediately after selection discrimination and before construction.
  NOW and UUID-FN are mandatory injected capabilities; UUID-FN is called once
  for each new occurrence identity."
  [{:keys [run-id cohort-id attempt-id selected-action now uuid-fn] :as inputs}]
  (when-not (= #{:run-id :cohort-id :attempt-id :selected-action :now :uuid-fn}
               (set (keys inputs)))
    (refuse! :shape-invalid [:mint-input]))
  (when-not (and (fn? now) (fn? uuid-fn))
    (refuse! :mint-capability-missing [:mint-input]))
  (let [action-bytes (pr-str selected-action)
        occurrence {:schema occurrence-schema
                    :run/id run-id :cohort/id cohort-id :attempt/id attempt-id
                    :transition/id (str "transition-" (uuid-fn))
                    :action/id (str "action-" (uuid-fn))
                    :action/value selected-action
                    :action/value-sha256 (sha256 action-bytes)
                    :action-at (str (now))}]
    (validate-occurrence occurrence)))

(defn- validate-state-port! [state cutoff]
  (when-not (map? state) (refuse! :state-port-invalid [:state]))
  (case (:status state)
    :absent
    (do (exact-map! state #{:status :reason} [:state])
        (when-not (keyword? (:reason state))
          (refuse! :state-absence-reason-invalid [:state :reason])))

    :observed
    (do
      (exact-map! state #{:status :method :state :state-at :observed-at
                          :evidence/id} [:state])
      (when-not (= :independent-categorical-observation (:method state))
        (refuse! (case (:method state)
                   :selection-belief :selection-belief-as-state
                   :derived-unique-argmax-of-mu-post :argmax-as-state
                   :close-disposition :disposition-as-state
                   :state-method-invalid)
                 [:state :method]))
      (when-not (contains? status-support (:state state))
        (refuse! :state-value-invalid [:state :state]))
      (text! (:evidence/id state) [:state :evidence/id])
      (let [state-at (instant! (:state-at state) [:state :state-at])
            observed-at (instant! (:observed-at state) [:state :observed-at])]
        (when (.isAfter observed-at state-at)
          (refuse! :state-observation-after-state [:state]))
        (when-not (.isBefore observed-at cutoff)
          (refuse! (if (= observed-at cutoff) :collapsed-evidence-freezes
                       :evidence-after-cutoff)
                   [:state :observed-at]))))

    (refuse! :state-port-invalid [:state :status])))

(defn- validate-model-port! [model]
  (when-not (map? model) (refuse! :model-port-invalid [:model]))
  (case (:status model)
    :absent
    (do (exact-map! model #{:status :reason} [:model])
        (when-not (= :declared-model-identity-unthreaded (:reason model))
          (refuse! :model-absence-reason-invalid [:model :reason])))

    :present
    (do (exact-map! model #{:status :source :model/id :model/revision} [:model])
        (when-not (= :declared-machine-model (:source model))
          (refuse! :model-revision-coercion [:model :source]))
        (text! (:model/id model) [:model :model/id])
        (text! (:model/revision model) [:model :model/revision]))

    (refuse! :model-port-invalid [:model :status])))

(defn validate-retention-block [block]
  (exact-map! block #{:schema :occurrence :state :model :closed-at
                      :evidence-cutoff :admitted-evidence}
              [:retention])
  (when-not (= retention-schema (:schema block))
    (refuse! :schema-mismatch [:retention :schema]))
  (let [occurrence (validate-occurrence (:occurrence block))
        action-at (instant! (:action-at occurrence) [:occurrence :action-at])
        closed-at (instant! (:closed-at block) [:closed-at])
        cutoff (instant! (:evidence-cutoff block) [:evidence-cutoff])]
    (when-not (= closed-at cutoff)
      (refuse! :cutoff-not-closed-at [:evidence-cutoff]))
    (when (.isAfter action-at closed-at)
      (refuse! :temporal-order-invalid [:occurrence :action-at]))
    (validate-state-port! (:state block) cutoff)
    (when (= :observed (get-in block [:state :status]))
      (let [state-at (instant! (get-in block [:state :state-at]) [:state :state-at])]
        (when (or (.isBefore state-at action-at) (.isAfter state-at closed-at))
          (refuse! :temporal-order-invalid [:state :state-at]))))
    (validate-model-port! (:model block))
    (when-not (and (vector? (:admitted-evidence block))
                   (= (count (:admitted-evidence block))
                      (count (distinct (:admitted-evidence block)))))
      (refuse! :admitted-evidence-invalid [:admitted-evidence]))
    (doseq [[i id] (map-indexed vector (:admitted-evidence block))]
      (text! id [:admitted-evidence i]))
    (when (and (= :observed (get-in block [:state :status]))
               (not (some #{(get-in block [:state :evidence/id])}
                          (:admitted-evidence block))))
      (refuse! :state-evidence-not-admitted [:state :evidence/id])))
  block)

(defn build-retention-block [inputs]
  (when-not (= #{:occurrence :state :model :closed-at :evidence-cutoff
                 :admitted-evidence} (set (keys inputs)))
    (refuse! :shape-invalid [:build-input]))
  (validate-retention-block (assoc inputs :schema retention-schema)))
