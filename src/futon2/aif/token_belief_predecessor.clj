(ns futon2.aif.token-belief-predecessor
  "Production token-carry admission. No production enactment authority exists
   yet: inspect available predecessor records and explicitly initialize fresh."
  (:require [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.machine-enactment-correspondence :as correspondence]))

(def candidate-paths
  [[:decision :action] [:selection-enaction] [:enactment]
   [:realized-outcome] [:enactment-plan] [:acting-order-after]])

(defn inspect-trace
  "Snapshot exactly the candidate fields inspected in the previous trace.
   This is not a search of construction history or evidence of execution."
  [trace]
  {:scope :previous-trace-only
   :identity (select-keys trace [:timestamp :run/id :cohort-attempt])
   :candidates
   (mapv (fn [path]
           (let [record (get-in trace path)]
             {:path path :status (if (nil? record) :absent :present)
              :record record :sha256 (evidence/value-digest record)}))
         candidate-paths)})

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
  "Consult the real production verifier. Only its known authority absence
   is an admissible continuation here; other defects propagate."
  []
  (try
    (correspondence/verify-correspondence {:mode :production})
    (throw (ex-info "Production authority changed; token transition admission requires review"
                    {:kind :token-carry-admission-contract-changed}))
    (catch clojure.lang.ExceptionInfo e
      (if (= :e2b/production-authority-unavailable (:refusal (ex-data e)))
        {:status :refused :kind (:refusal (ex-data e))
         :verifier 'futon2.aif.machine-enactment-correspondence/verify-correspondence
         :mode :production}
        (throw e)))))

(defn input-receipt
  "Refused carry -> fresh fact initialization -> consumed belief. This is an
   admission receipt, never an observation update or an impossible observation."
  [stage inspection]
  (let [previous (:prospective-prior stage)
        universe (get-in stage [:prospective-carry :universe])
        changed? (and (some? previous) (not= universe (:universe previous)))]
    {:schema :wm/token-belief-input-v1
     :occurrence-id (:occurrence-id stage)
     :z-semantics :per-step-redraw
     :conditioning-status :not-run
     :reason :carry-admission-refused
     :inspection inspection
     :carry-admission
     {:status :refused
      :kind (if changed? :carry-domain-changed :carry-no-predecessor)
      :authority (production-authority)
      :previous-universe (:universe previous) :current-universe universe
      :inspected (mapv reject-candidate (:candidates inspection))}
     :initialization (:initialization stage)
     :policy :fresh-fact-initialization-after-carry-refusal
     :continuation-belief (get-in stage [:initialization :value])
     :observation-updates []}))

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
         (= receipt (input-receipt stage inspection)))))
