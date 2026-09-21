(ns futon2.aif.token-belief-predecessor
  "Production token-carry admission. No production enactment authority exists
   for portfolio proofs. D task evidence has a separate, narrower authority."
  (:require [futon2.aif.load-identity :as load-identity]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.token-initialization-policy :as policy]
            [futon2.aif.d-predecessor-task-authority :as task]))

(load-identity/register! *ns* *file*)

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

(defn- legacy-input-receipt
  "Refused carry -> fresh fact initialization -> consumed belief. This is an
   admission receipt, never an observation update or an impossible observation."
  ([stage inspection]
   (legacy-input-receipt stage inspection (production-authority (:task-context inspection))))
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

(defn observation-authority [expected]
  (task/read-observations-v2 task/default-root expected task/agency-job))

(defn- valid-observation-authority? [a inspection]
  (and (= :wm/d-task-token-observations-v2 (:schema a))
       (= task/observation-authority (:authority a)) (= task/observation-scope (:scope a))
       (case (:status a)
         :admitted
         (and (valid-authority? (assoc (:execution-verification a) :source (:source a)) inspection)
              (= (:occurrence a) (get-in a [:execution-verification :occurrence]))
              (= (:carry-occurrence-id a) (get-in a [:execution-verification :carry-occurrence-id]))
              (= (:revision-pair a) (get-in a [:execution-verification :revision-pair]))
              (= :not-authorized (:consumption a))
              (= :independent-check-required (:causal-attribution a)))
         :refused (keyword? (:kind a))
         :invalid (keyword? (:kind a))
         false)))

(defn input-receipt
  "V3 authorizes only declared next-selection initialization from signed checks.
   The old execution admission remains separate (including for precision carry)."
  ([stage inspection]
   (input-receipt stage inspection (production-authority (:task-context inspection))
                  (when (policy/enabled? (:observation-initialization stage))
                    (observation-authority (:task-context inspection)))))
  ([stage inspection admission]
   (input-receipt stage inspection admission nil))
  ([stage inspection admission observations]
   (let [legacy (legacy-input-receipt stage inspection admission)]
     (if-not (= :wm/token-belief-stage-v2 (:schema stage)) legacy
       (let [outcome (if (and (policy/enabled? (:observation-initialization stage))
                              (not (valid-observation-authority? observations inspection)))
                       {:status :refused :kind :observation-authority-invalid
                        :observation-updates [] :continuation-belief (get-in stage [:initialization :value])}
                       (policy/apply-observations stage inspection observations))]
         (assoc legacy :schema :wm/token-belief-input-v3
                :conditioning-status (if (some #(= :updated (:status %)) (:observation-updates outcome))
                                       :observed-initialization :not-run)
                :reason (:kind outcome) :policy (:observation-initialization stage)
                :observation-authority observations :observation-initialization outcome
                :observation-updates (:observation-updates outcome)
                :continuation-belief (:continuation-belief outcome)))))))

(def legacy-unavailable-authority
  ;; Historical 2b receipt replay only; never used to admit a new predecessor.
  {:status :refused :kind :e2b/production-authority-unavailable
   :verifier 'futon2.aif.machine-enactment-correspondence/verify-correspondence
   :mode :production})

(defn valid-input?
  "Replay each version under its own policy. V1/V2 preserve fresh initialization;
   V3 replays signed observation updates. External snapshot origin is checked
   by the production reader, not by this retained-receipt replay."
  [receipt stage]
  (let [inspection (:inspection receipt)
        candidates (:candidates inspection)]
    (and (= :previous-trace-only (:scope inspection))
         (= candidate-paths (mapv :path candidates))
         (every? (fn [{:keys [record status sha256]}]
                   (and (= status (if (nil? record) :absent :present))
                        (= sha256 (evidence/value-digest record)))) candidates)
         (case (:schema receipt)
           :wm/token-belief-input-v1
           (= receipt (assoc (legacy-input-receipt stage inspection legacy-unavailable-authority)
                             :schema :wm/token-belief-input-v1))
           :wm/token-belief-input-v2
           (and (valid-authority? (get-in receipt [:carry-admission :authority]) inspection)
                (= receipt (legacy-input-receipt stage inspection (get-in receipt [:carry-admission :authority]))))
           :wm/token-belief-input-v3
           (and (= :wm/token-belief-stage-v2 (:schema stage))
                (valid-authority? (get-in receipt [:carry-admission :authority]) inspection)
                (= receipt (input-receipt stage inspection (get-in receipt [:carry-admission :authority])
                                          (:observation-authority receipt))))
           false))))
