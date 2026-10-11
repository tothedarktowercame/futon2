(ns futon2.aif.factorized-preference-sut
  "Controlled Q9 factorized-preference producer at the real class scorer.
   This namespace is deliberately absent from the serving composition."
  (:require [futon2.aif.cascade-observation-scoring :as scoring]
            [futon2.aif.interpretation-evidence :as evidence]))

(def schema :wm/factorized-preference-sut-v1)
(def carrier-schema :wm/factorized-preference-v1)
(def factorization-implementation
  {:id :futon2/completion-first-softmax-v1 :version 1
   :semantics :completion-then-conditional-relevance})
(def calibration-implementation
  {:id :futon2/ordinal-relevance-cost-v1 :version 1
   :semantics :externally-calibrated-strict-cost-order})

(defn implementation-pin [implementation] (evidence/value-digest implementation))
(defn- refusal [kind & [detail]] {:schema schema :status :refused :kind kind :detail detail})
(defn- unique? [xs] (= (count xs) (count (set xs))))

(defn outcome-valid? [{:keys [completion relevance]}]
  (case completion
    :completed (contains? #{:focused :related :unrelated} relevance)
    (:not-completed :unmeasured) (nil? relevance)
    false))

(defn preferred?
  "Lean's lexicographic relation. Unknown/unmeasured is incomparable."
  [calibration a b]
  (and (outcome-valid? a) (outcome-valid? b)
       (or (and (= :completed (:completion a)) (= :not-completed (:completion b)))
           (and (= :completed (:completion a)) (= :completed (:completion b))
                (< (get calibration (keyword (str (name (:relevance a)) "-cost")))
                   (get calibration (keyword (str (name (:relevance b)) "-cost"))))))))

(defn- authorization [registry receipt]
  (first (filter #(= receipt (:receipt %)) registry)))

(defn- validate-certificate! [{:keys [certificate calibration-authorizations
                                      factorization-authorizations] :as carrier}]
  (when-not (= carrier-schema (:schema carrier))
    (throw (ex-info "factorized preference refused" {:kind :carrier-schema-invalid})))
  (let [{:keys [calibration selector-authority factorization-identity
                factorization-version factorization-authority
                calibration-authorization factorization-authorization]} certificate
        ca (authorization calibration-authorizations calibration-authorization)
        fa (authorization factorization-authorizations factorization-authorization)]
    (when-not (and (pos-int? selector-authority)
                   (every? pos-int? (map calibration
                                          [:identity :version :authority :registration-token]))
                   (pos-int? factorization-identity) (pos-int? factorization-version)
                   (pos-int? factorization-authority)
                   (not= selector-authority (:authority calibration))
                   (not= selector-authority factorization-authority)
                   (< (:focused-cost calibration) (:related-cost calibration)
                      (:unrelated-cost calibration)))
      (throw (ex-info "factorized preference refused" {:kind :certificate-invalid})))
    (when-not (and ca (= calibration (:calibration ca))
                   (= (:authority calibration) (:authorized-by ca))
                   (= (implementation-pin calibration-implementation)
                      (:implementation-pin ca))
                   (= (:implementation ca) (:id calibration-implementation)))
      (throw (ex-info "factorized preference refused" {:kind :calibration-authorization-mismatch})))
    (when-not (and fa (= factorization-identity (:identity fa))
                   (= factorization-version (:version fa))
                   (= factorization-authority (:authority fa))
                   (= :completion-then-conditional-relevance (:law fa))
                   (= (implementation-pin factorization-implementation)
                      (:implementation-pin fa))
                   (= (:implementation fa) (:id factorization-implementation)))
      (throw (ex-info "factorized preference refused" {:kind :factorization-authorization-mismatch})))
    carrier))

(defn- kernel [calibration]
  ;; Completion dominates the complete relevance range structurally: the
  ;; noncompletion cost is one beyond the authorized unrelated endpoint.
  (let [costs {:focused (:focused-cost calibration)
               :related (:related-cost calibration)
               :unrelated (:unrelated-cost calibration)
               :stop-the-line (inc (:unrelated-cost calibration))}
        ;; Exact rational monotone scalarization; no floating normalization
        ;; drift and no additional caller-controlled parameter.
        masses (update-vals costs #(/ 1 (inc %)))
        z (reduce + (vals masses))
        focused (/ (:focused masses) z)
        related (/ (:related masses) z)
        unrelated (/ (:unrelated masses) z)
        probabilities {:focused focused :related related :unrelated unrelated
                       :stop-the-line (- 1 (+ focused related unrelated))}]
    {:costs costs :probabilities probabilities
     :law :completion-then-conditional-relevance}))

(defn compose-and-score!
  "Validate the retained Q9 carrier and policy rows, derive (never accept) the
   kernel, then call the production class scorer. All supplied policies must
   share semantics/horizon/schedule. Production mode has no registry and
   refuses before scoring."
  [{:keys [mode carrier policies candidates state universe acceptance horizon schedule]}]
  (try
    (when-not (= :controlled-sut mode)
      (throw (ex-info "factorized preference refused" {:kind :production-registries-unconfigured})))
    (when (or (:class-preference carrier) (:weights carrier) (:computed-kernel carrier))
      (throw (ex-info "factorized preference refused" {:kind :flat-table-not-a-certificate})))
    (validate-certificate! carrier)
    (when-not (and (vector? policies) (seq policies)
                   (unique? (map :policy-id policies)) (every? outcome-valid? policies))
      (throw (ex-info "factorized preference refused" {:kind :typed-outcome-invalid})))
    (when (some #(= :unmeasured (:completion %)) policies)
      (throw (ex-info "factorized preference refused" {:kind :unmeasured-not-observed-noncompletion})))
    (let [policy-ids (mapv :policy-id policies)
          candidate-ids (mapv :id candidates)
          semantics (set (map :semantics policies))
          horizons (set (map :horizon policies))
          schedules (set (map :schedule policies))]
      (when-not (= (set policy-ids) (set candidate-ids))
        (throw (ex-info "factorized preference refused" {:kind :policy-population-mismatch})))
      (when-not (and (= 1 (count semantics)) (= 1 (count horizons)) (= 1 (count schedules))
                     (= #{horizon} horizons) (= #{schedule} schedules))
        (throw (ex-info "factorized preference refused" {:kind :posterior-grain-mismatch})))
      (when-not (unique? (map :target candidates))
        (throw (ex-info "factorized preference refused" {:kind :target-projection-not-injective})))
      (let [calibration (get-in carrier [:certificate :calibration])
            computed (kernel calibration)
            terminal (:probabilities computed)
            not-yet :ending/not-yet-evaluated
            model {:schema :wm/observation-model-v1 :backend :exact-enumeration
                   :kind :class-emission :universe universe :horizon horizon
                   :class-universe [:focused :related :unrelated :stop-the-line not-yet]
                   :acceptance acceptance
                   :target-class (into {} (map (juxt :target :relevance)) policies)
                   :class-preference (into {} (for [tau (range 1 (inc horizon))]
                                                [tau (if (= tau horizon) terminal {not-yet 1})]))
                   :provenance {:status :synthetic :calibrated false
                                :source "controlled authorized Q9 factorized preference"}}
            scored (scoring/rank-cascade-actions
                    state candidates
                    {:observation-model model :horizon-steps horizon
                     :prediction-context {:occurrence-id "controlled-q9" :tau horizon}
                     :cascade-spec {:want acceptance :evidence #{} :zeroed #{}
                                    :c-schedule schedule}})]
        (if (map? scored)
          (refusal :real-class-scorer-refused scored)
          (let [consumed (mapv #(get-in % [:certificate :consumed-g :C :steps]) scored)]
            (when-not (every? #(= terminal (:distribution (last %))) consumed)
              (throw (ex-info "factorized preference refused" {:kind :computed-kernel-not-consumed})))
            {:schema schema :status :scored :carrier carrier :policies policies
             :kernel computed :observation-model model :scored scored
             :posterior-grain {:policy-identities policy-ids :semantics (first semantics)
                               :horizon horizon :schedule schedule}
             :consumption {:boundary :cascade-observation-scoring/rank-cascade-actions
                           :certified-terminal-distribution terminal
                           :rows consumed}}))))
    (catch clojure.lang.ExceptionInfo e
      (refusal (:kind (ex-data e)) (dissoc (ex-data e) :kind)))
    (catch Exception e
      (refusal :factorized-preference-composition-failed
               {:class (.getName (class e)) :message (.getMessage e)}))))
