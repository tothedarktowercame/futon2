(ns futon2.aif.temporal-input
  "Pure execution/observation join; admission does not compute q or consume an event."
  (:require [clojure.set :as set]
            [futon2.aif.cascade-policy :as policy]
            [futon2.aif.cascade-model-manifest :as manifest]
            [futon2.aif.exact-belief-adapter :as exact]
            [futon2.aif.exact-belief-core :as core]
            [futon2.aif.interpretation-evidence :as evidence]))

(defn absent [reason detail]
  {:schema :wm/temporal-input-v1 :status :absent :reason reason :detail detail})

(defn transition-reading
  "Read the dispatched pattern's declared add-only B, using selection's sole
   converter and the manifest's declared theta default. Qualify local tokens
   by target. Missing guards/produces never become an identity transition."
  [target id interpretation]
  (let [p (policy/declared->interpreted id interpretation)
        clauses (get-in p [:guard :clauses])
        produces (:produces p)
        qualify #(into #{} (map (fn [t] [target t])) %)
        p (manifest/with-pattern-theta
           (merge p (select-keys interpretation [:theta :theta-source])))]
    (if (and (some? target) (some? id) (set? produces)
             (= :interpreted (get-in p [:guard :status]))
             (= :and (get-in p [:guard :operator]))
             (seq clauses) (every? #(and (set? (:present %)) (set? (:absent %))) clauses)
             (= :interpreted (get-in p [:transition :status]))
             (= :union (get-in p [:transition :operator]))
             (= produces (get-in p [:transition :produces]))
             (core/probability? (:theta p)))
      (-> p (assoc :id id :produces (qualify produces))
          (assoc-in [:transition :produces] (qualify produces))
          (assoc-in [:guard :clauses]
                    (mapv #(-> % (update :present qualify) (update :absent qualify)) clauses)))
      (absent :transition-not-determinable {:pattern id}))))

(defn attempt-context
  "Retain exactly the dispatched interpretation and its explicit model/domain.
   These annotations cannot turn an unsuccessful attempt into execution."
  [click step]
  (let [i (:interpretation step)]
    {:click-id (:click-id click) :target (:target step)
     :interpretation i
     :transition (transition-reading (:target step) (:pattern step) i)
     :model-identity (or (:model-identity i) (absent :no-model-identity nil))
     :domain (or (:domain i) (absent :no-domain nil))}))

(defn event-id
  "Digest of click, step index, semantic locator and the checker-resolved SHA.
   The spelling of a git alias (:sha HEAD versus a hash) is not a new event."
  [enacted observed]
  (evidence/value-digest
   [(:click-id enacted) (:n enacted)
    (select-keys observed [:class :repo :path :decl :entry :bundle-path])
    (get-in observed [:result :evidence :resolved-sha])]))

(defn- previous-belief [{:keys [basis record stage trajectory-start? initialization-authority
                               occurrence-id domain model-identity]}]
  (case basis
    :declared-initialization
    (when (and (true? trajectory-start?) (= :wm/token-belief-input-v3 (:schema record))
               (= occurrence-id (:occurrence-id record) (:occurrence-id stage))
               (= domain (get-in stage [:prospective-carry :universe]))
               (some? initialization-authority)
               (= initialization-authority (:initialization record) (:initialization stage)))
      (:continuation-belief record))
    :posterior
    (when (and (not trajectory-start?) (= :wm/exact-belief-update-v1 (:schema record))
               (= :ok (:status record))
               (= {:occurrence-id occurrence-id :domain domain :model-identity model-identity}
                  (select-keys (:model record) [:occurrence-id :domain :model-identity]))
               (map? (:observation-rows record)) (map? (:transition-rows record))
               (= record (exact/exact-update (:states record) (:observation-rows record)
                                            (:transition-rows record) (:observation record)
                                            (:prior record) (:model record))))
      (:posterior record))
    nil))

(defn temporal-input
  "Join PREVIOUS, a retained flight attempt, and that attempt's check carrier.
   PREVIOUS declares :basis, :record, :occurrence-id, :domain, :model-identity
   {:A check-mechanism :B declared-transition-model}, :initial-event-id and
   :consumed-event-ids. A declared start also retains :stage and the v3
   :initialization-authority verbatim; later records must be exact posteriors.

   AIF validity: q must be a probability measure on the same declared domain;
   B must name the checked executed primitive, not its proposed cascade; A must
   name the actual checker. The scalar :produced/check protocol witnesses only
   a singleton produces set. Other executor shapes are absent, never guessed.
   Occurrence/commit/locator linkage and the supplied cursor prevent using a
   different or already-consumed observation. No required selection field, gate,
   new posterior update, IO, or atomic cursor publication is introduced here."
  [previous enacted observed]
  (let [{:keys [domain model-identity consumed-event-ids initial-event-id]} previous
        q (previous-belief previous)
        transition (:transition enacted)
        check-result (:result observed)
        revision (get-in check-result [:evidence :resolved-sha])
        locator (dissoc observed :result)
        event-id (event-id enacted observed)
        tokens (when (set? (:produces transition))
                 (reduce set/union (:produces transition)
                         (mapcat (juxt :present :absent) (get-in transition [:guard :clauses]))))]
    (cond
      (nil? previous) (absent :no-previous-posterior nil)
      (not (and (map? model-identity) (some? (:A model-identity)) (some? (:B model-identity))))
      (absent :no-model-identity nil)
      (not (set? domain)) (absent :no-domain nil)
      (not (and (core/distribution? q)
                (every? #(and (set? %) (set/subset? % domain)) (keys q))))
      (absent :invalid-previous-posterior {:basis (:basis previous)})
      (not (and (set? consumed-event-ids) (some? initial-event-id)
                (contains? consumed-event-ids initial-event-id)))
      (absent :no-consumed-event-cursor nil)
      (nil? enacted) (absent :no-enactment-supplied nil)
      (not (and (true? (:success enacted)) (some? (:commit enacted))
                (some? (:pattern enacted)) (pos-int? (:n enacted))
                (some? (:click-id enacted)) (= (:occurrence-id previous) (:click-id enacted))))
      (absent :execution-not-linked (select-keys enacted [:click-id :n :pattern :success]))
      (not (and tokens (= transition (transition-reading (:target enacted) (:pattern enacted)
                                                                        (:interpretation enacted)))))
      (absent :transition-not-determinable {:pattern (:pattern enacted)})
      (not= model-identity (:model-identity enacted)) (absent :model-identity-mismatch nil)
      (not (and (= domain (:domain enacted)) (set/subset? tokens domain)))
      (absent :domain-mismatch nil)
      (not= (:produces transition) #{[(:target enacted) (:produced enacted)]})
      (absent :execution-transition-unwitnessed {:produced (:produced enacted)})
      (not (and (map? observed) (= observed (:check enacted))
                (true? (:observed check-result)) (some? revision)))
      (absent :no-observed-outcome nil)
      (not (and (= revision (:commit enacted))
                (= (:class locator) (:check check-result))
                (= (:A model-identity) (:check-mechanism check-result))
                (= (select-keys locator [:repo :path :decl :entry :bundle-path])
                   (select-keys (:evidence check-result) [:repo :path :decl :entry :bundle-path]))))
      (absent :observation-not-linked {:resolved-sha revision})
      (contains? consumed-event-ids event-id)
      (absent :event-already-consumed {:consumed-event-id event-id})
      :else {:schema :wm/temporal-input-v1 :status :admitted
             :previous previous :enacted enacted :observed observed
             :consumed-event-id event-id})))
