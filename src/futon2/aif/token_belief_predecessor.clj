(ns futon2.aif.token-belief-predecessor
  "Production token-carry admission. No production enactment authority exists
   for portfolio proofs. D task evidence has a separate, narrower authority."
  (:require [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.d-predecessor-task-authority :as task]))

(def candidate-paths
  [[:decision :action] [:selection-enaction] [:enactment]
   [:realized-outcome] [:enactment-plan] [:acting-order-after]])

(defn inspect-trace
  "Snapshot exactly the candidate fields inspected in the previous trace.
   This is not a search of construction history or evidence of execution."
  [trace]
  (cond-> {:scope :previous-trace-only
   :identity (select-keys trace [:timestamp :run/id :cohort-attempt])
   :candidates
   (mapv (fn [path]
           (let [record (get-in trace path)]
             {:path path :status (if (nil? record) :absent :present)
              :record record :sha256 (evidence/value-digest record)}))
         candidate-paths)}
    (:d-task-context trace) (assoc :task-context (:d-task-context trace))))

(defn- reject-candidate [{:keys [path status record] :as candidate}]
  (assoc candidate :admission :refused
         :reason (cond
                   (= :absent status) :record-absent
                   (= :runner-selection (get-in record [:evidence :source]))
                   :selection-self-comparison-is-not-enactment
                   (= path [:decision :action]) :selection-is-not-enactment
                   (= path [:selection-enaction]) :unverified-selection-enaction
                   (= path [:enactment-plan]) :plan-is-not-enactment
                   (= path [:acting-order-after]) :simulated-order-is-not-enactment
                   :else :independent-token-transition-evidence-unestablished)))

(defn production-authority
  "D carry only: executed-with-artifacts in minted-identity space. This does
   not replace E2b or establish its E1/E2a portfolio proposition."
  ([] (production-authority nil))
  ([expected]
   (task/read-predecessor task/default-root expected task/agency-job)))

(defn- valid-authority? [a inspection]
  (and (= task/authority (:authority a)) (= task/scope (:scope a))
       (case (:status a)
         :admitted (and (= (get-in inspection [:task-context :occurrence]) (:occurrence a))
                        (= (get-in inspection [:task-context :carry-occurrence-id]) (:carry-occurrence-id a))
                        (= :not-established (:candidate-to-minted-join a))
                        (= :task (:enactment-grain a))
                        (= :declared-kernel-of-verified-macro-action (:b-authority a))
                        (string? (:record-sha256 a)) (string? (get-in a [:source :sha256])))
         :refused (keyword? (:kind a))
         :invalid (keyword? (:kind a))
         false)))

(defn input-receipt
  "Refused carry -> fresh fact initialization -> consumed belief. This is an
   admission receipt, never an observation update or an impossible observation."
  ([stage inspection]
   (input-receipt stage inspection (production-authority (:task-context inspection))))
  ([stage inspection admission]
  (let [previous (:prospective-prior stage)
        universe (get-in stage [:prospective-carry :universe])
        changed? (and (some? previous) (not= universe (:universe previous)))
        admitted? (and (not changed?) (= :admitted (:status admission)))]
    {:schema :wm/token-belief-input-v2
     :occurrence-id (:occurrence-id stage)
     :z-semantics :per-step-redraw
     :conditioning-status (if admitted? :not-wired :not-run)
     :reason (if admitted? :conditioning-consumption-not-wired :carry-admission-refused)
     :inspection inspection
     :carry-admission
     {:status (if changed? :refused (:status admission))
      :kind (if changed? :carry-domain-changed
                (if (= :admitted (:status admission)) :task-execution-verified :carry-no-predecessor))
      :authority admission
      :previous-universe (:universe previous) :current-universe universe
      :inspected (mapv reject-candidate (:candidates inspection))}
     :initialization (:initialization stage)
     :policy (if admitted?
               :fresh-fact-initialization-conditioning-not-wired
               :fresh-fact-initialization-after-carry-refusal)
     :continuation-belief (get-in stage [:initialization :value])
     :observation-updates []})))

(def legacy-unavailable-authority
  ;; Historical 2b receipt replay only; never used to admit a new predecessor.
  {:status :refused :kind :e2b/production-authority-unavailable
   :verifier 'futon2.aif.machine-enactment-correspondence/verify-correspondence
   :mode :production})

(defn valid-input?
  "Replay the refusal chain; preserve the original initializer equality.
   This validates retained evidence, not the external origin of its snapshots."
  [receipt stage]
  (let [inspection (:inspection receipt)
        candidates (:candidates inspection)]
    (and (= :previous-trace-only (:scope inspection))
         (= candidate-paths (mapv :path candidates))
         (every? (fn [{:keys [record status sha256]}]
                   (and (= status (if (nil? record) :absent :present))
                        (= sha256 (evidence/value-digest record)))) candidates)
         (if (= :wm/token-belief-input-v1 (:schema receipt))
           (= receipt (assoc (input-receipt stage inspection legacy-unavailable-authority)
                             :schema :wm/token-belief-input-v1))
           (and (valid-authority? (get-in receipt [:carry-admission :authority]) inspection)
                (= receipt (input-receipt stage inspection (get-in receipt [:carry-admission :authority]))))))))
