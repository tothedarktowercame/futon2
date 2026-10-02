(ns futon2.aif.wm.apparatus-certificates
  "Fail-closed runtime-certificate census for catalogue nodes which are not
  represented by a Lean equation.  A namespace or green unit test is never a
  runtime certificate: only evidence retained by this run can yield :present."
  (:require [futon2.aif.hierarchical-budget-adapter :as r11]))

(def schema :wm/apparatus-certificates-v1)

(defn- present [node kind evidence]
  {:node node :status :present :kind kind :evidence evidence})

(defn- refused [node reason & [detail]]
  (cond-> {:node node :status :refused :reason reason}
    detail (assoc :detail detail)))

(defn- not-applicable [node reason]
  {:node node :status :not-applicable :reason reason})

(defn- r9-certificate [e]
  (let [roles (:roles e)]
    (if (and (= :wm/r9-independence-admission-v1 (:schema e))
             (string? (:author roles)) (string? (:reviewer roles))
             (not= (:author roles) (:reviewer roles))
             (seq (:joins e))
             (every? #{:passed} (vals (:joins e))))
      (present :R9 :independent-pre-enact-authorization e)
      (refused :R9 :canonical-pre-enact-authorization-not-retained))))

(defn- r10-certificate [trigger e]
  (if (not= :scheduled trigger)
    (not-applicable :R10 :run-was-not-scheduler-initiated)
    (if (and (= :verified-causal-route (:status e))
             (true? (:production-edge-fired? e)))
      (present :R10 :scheduled-causal-route e)
      (refused :R10 :production-scheduled-route-not-verified))))

(defn- r11-certificate [e]
  (if (nil? e)
    (not-applicable :R11 :no-shared-budget-arbitration-requested)
    (try
      (let [replay (r11/replay e)]
        (if (:replay/identical? replay)
          (present :R11 :exact-shared-budget-replay
                   {:receipt e :replay/identical? true})
          (refused :R11 :shared-budget-replay-disagrees)))
      (catch Throwable t
        (refused :R11 :shared-budget-receipt-invalid
                 {:message (.getMessage t)})))))

(defn- independent-layer-2? [e]
  (let [layer-2 (get-in e [:returned :artifact :layer-2/independent-evidence])]
    (and (= :admitted (:status e))
         (= :R12 (:node e))
         (= :R12/layer-2 (:layer layer-2))
         (= :admitted (:status layer-2))
         (true? (:independent? layer-2)))))

(defn- r12-certificate [e]
  (if (independent-layer-2? e)
    (present :R12 :independent-layer-2-calibration e)
    (refused :R12 :independent-layer-2-calibration-not-retained)))

(defn- r15-certificate [e]
  (if (and (= :wm/r15-two-tick-certificate-v1 (:schema e))
           (true? (:fast-outcome-independently-witnessed? e))
           (some? (:slow-state-before e))
           (some? (:slow-state-after e))
           (not= (:slow-state-before e) (:slow-state-after e))
           (= (:slow-state-after e) (:next-tick-consumed-slow-state e)))
    (present :R15 :two-tick-strategic-tactical-feedback e)
    (refused :R15 :two-tick-feedback-correspondence-not-retained)))

(defn receipt
  "Project the five apparatus certificates from this run's retained DATA.
  Canonical producers may attach evidence below :apparatus-evidence keyed by
  R-node.  TRIGGER is the runner trigger, not reconstructed from prose."
  [result trigger]
  (let [evidence (get-in result [:data :apparatus-evidence])
        by-node {:R9 (r9-certificate (:R9 evidence))
                 :R10 (r10-certificate trigger (:R10 evidence))
                 :R11 (r11-certificate (:R11 evidence))
                 :R12 (r12-certificate (:R12 evidence))
                 :R15 (r15-certificate (:R15 evidence))}
        counts (frequencies (map :status (vals by-node)))]
    {:schema schema
     :status (if (zero? (get counts :refused 0)) :complete :incomplete)
     :by-node by-node
     :counts (merge {:present 0 :refused 0 :not-applicable 0} counts)}))
