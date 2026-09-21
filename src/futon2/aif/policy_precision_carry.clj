(ns futon2.aif.policy-precision-carry
  "Joint WM rate carry. Task evidence certifies execution with artifacts only;
   it does not establish E1 membership, R6-R11 identity or E2b correspondence.
   Unknown observation coordinates are marginalized, never treated as absent."
  (:require [clojure.set :as set]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.matched-observation-evidence :as observed]
            [futon2.aif.policy-precision :as precision]))

(defn- require! [p kind]
  (when-not p (throw (ex-info (name kind) {:kind kind :status :invalid}))))
(defn- finite? [x] (and (number? x) (Double/isFinite (double x))))
(defn seal [value] (assoc value :sha256 (evidence/value-digest value)))
(defn intact? [value]
  (and (map? value) (= (:sha256 value) (evidence/value-digest (dissoc value :sha256)))))

(defn event-probability
  "Exact factorized marginal: multiply only measured coordinates. Validate the
   whole declared rate domain first; marginalization cannot hide invalid rates."
  [rates state {:keys [present absent]}]
  (require! (and (set? state) (set? present) (set? absent)
                 (empty? (set/intersection present absent))
                 (set/subset? (set/union state present absent) (set (keys rates))))
            :invalid-measured-event)
  (let [check (m/token-likelihood rates state #{})]
    (require! (number? check) :invalid-observation-rates))
  (let [measured (set/union present absent)]
    (m/token-likelihood (select-keys rates measured) (set/intersection state measured) present)))

(defn retrospective-f
  "Evaluate the declared event time on every frozen policy, retaining exact
   probabilities. No selection-time F or new-menu G is substituted."
  [{:keys [model candidates]} {:keys [tau present absent] :as event}]
  (let [{:keys [q0 rates horizon]} model]
    (require! (and (integer? tau) (<= 0 tau horizon)) :observation-outside-horizon)
    (require! (seq (set/union present absent)) :no-measured-observation)
    (into {}
          (for [{:keys [id]} candidates
                :let [q (m/rollout (constantly (:precedence id)) q0 tau)
                      _ (require! (and (map? q) (not (:status q))) :prediction-refused)
                      p (reduce +' 0 (for [[s mass] q] (*' mass (event-probability rates s event))))]]
            [id {:probability p :f (if (zero? p) ##Inf (observed/surprisal p))}]))))

(defn model-identity [model candidates schedules]
  (evidence/value-digest
   {:model (dissoc model :q0)
    :policies (set (map :id candidates)) :observation-schedules schedules}))

(defn family
  "Freeze actual consumed policy E/G and the scorer's model; no second habit read."
  [decision model schedules]
  (let [candidates (get-in decision [:selection-certificate :candidates])]
    (seal {:schema :wm/precision-family-v1 :context :WM :z-semantics :per-step-redraw
           :model model :candidates (mapv #(select-keys % [:id :g :habit]) candidates)
           :observation-schedules schedules
           :model-id (model-identity model candidates schedules)
           :selected-action (:action decision)})))

(defn validate-binding!
  "Dispatch-created binding in minted space. Never infer an old candidate join
   from equality of values; the full family is retained in the dispatch prompt."
  [family occurrence]
  (require! (intact? family) :precision-family-tampered)
  (require! (= :wm/precision-family-v1 (:schema family)) :precision-family-schema)
  (require! (= :WM (:context family)) :precision-context-mismatch)
  (require! (= :per-step-redraw (:z-semantics family)) :precision-z-semantics-mismatch)
  (require! (= (:model-id family)
               (model-identity (:model family) (:candidates family) (:observation-schedules family)))
            :precision-model-identity-mismatch)
  (require! (and (integer? (get-in family [:model :horizon]))
                 (<= 0 (get-in family [:model :horizon]))
                 (map? (get-in family [:model :rates]))
                 (m/normalized-exact? (get-in family [:model :q0])))
            :precision-model-invalid)
  (require! (= (:selected-action family) (:action/value occurrence)) :precision-selected-action-mismatch)
  (require! (some #(= (:id %) (:selected-action family)) (:candidates family)) :precision-selected-policy-missing)
  family)

(defn advance
  "Use independently verified task admission supplied by the production reader.
   Persist this sealed result in the decision trace before dispatch. Replay of a
   consumed occurrence holds. Invalid state refuses rather than initializing."
  [{:keys [previous initialized-beta model-id admission family]}]
  (require! (and (finite? initialized-beta) (pos? initialized-beta)) :invalid-initialized-beta)
  (when previous
    (require! (and (intact? previous) (= :wm/precision-carry-v1 (:schema previous))
                   (finite? (:beta previous)) (pos? (:beta previous))
                   (set? (:consumed previous))
                   (#{:declared :learned} (:beta-status previous))) :invalid-precision-carry))
  (let [beta (if previous (:beta previous) initialized-beta)
        consumed (or (:consumed previous) #{})
        base {:schema :wm/precision-carry-v1 :context :WM
              :authority :wm/precision-retrospective-task-v1
              :scope :verified-task-evidence-not-portfolio-approval
              :z-semantics :per-step-redraw :model-id model-id
              :beta beta :gamma (/ 1.0 beta) :tau beta
              :beta-status (or (:beta-status previous) :declared)
              :tau-source :carry-beta :initialized-beta initialized-beta
              :consumed consumed :predecessor-sha256 (:sha256 previous)}
        hold (fn [reason & [extra]] (seal (merge base {:status :held :reason reason} extra)))]
    (cond
      (and previous (not= model-id (:model-id previous)))
      (hold :precision-model-changed {:reinitialization {:policy :retain-last-valid-rate
                                                        :before (:model-id previous) :after model-id}})
      (and previous (not= initialized-beta (:initialized-beta previous)))
      (hold :precision-initialization-changed
            {:reinitialization {:policy :retain-last-valid-rate
                                :before (:initialized-beta previous) :after initialized-beta}})
      (not= :admitted (:status admission))
      (hold :precision-no-admitted-predecessor {:admission admission})
      (nil? family) (hold :precision-dispatch-binding-absent)
      :else
      (try
        (validate-binding! family (:occurrence admission))
        (let [id (:occurrence admission)
              selected (:selected-action family)
              schedule (get-in family [:observation-schedules (:target selected)])
              tau (get-in schedule [:tau :value])]
          (cond
            (contains? consumed id) (hold :precision-observation-already-consumed)
            (not= model-id (:model-id family)) (hold :precision-predecessor-model-changed)
            (nil? tau) (hold :observation-placement-not-declared)
            :else
            (let [event {:occurrence-id (:occurrence admission) :tau tau
                         :present (:present admission) :absent (:absent admission)
                         :unknown (:unknown admission)}
                  f (retrospective-f family event)
                  result (:WM (precision/cascade-beta-update
                               {:WM {:beta beta :solved-tick-count 0}} :WM
                               (:candidates family) (update-vals f :f) {}))
                  learned? (= :converged-posterior (:beta-source result))
                  b (:beta result)]
              (seal (merge base
                           {:status (if learned? :updated :held) :reason (:reason result)
                            :beta b :gamma (/ 1.0 b) :tau b
                            :beta-status (if learned? :learned (:beta-status base))
                            :consumed (conj consumed id) :event event :evidence f
                            :family-sha256 (:sha256 family) :update result})))))
        (catch clojure.lang.ExceptionInfo e
          (hold (or (:kind (ex-data e)) (:error (ex-data e))) {:refusal (ex-data e)}))))))
