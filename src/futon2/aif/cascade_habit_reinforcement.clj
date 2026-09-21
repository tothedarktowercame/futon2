(ns futon2.aif.cascade-habit-reinforcement
  "Declared close rule: one count only when a predicted wanted token was observed."
  (:require [futon2.aif.load-identity :as load-identity]
            [futon2.aif.cascade-habit-store :as store]
            [futon2.aif.cascade-prior :as prior]))

(load-identity/register! *ns* *file*)

(def rule-id :wm/cascade-habit-observed-want-v1)
(def comparison-interface
  {:schema :wm/token-outcome-comparison-v1
   :status :compared
   :prediction {:status :frozen :target :selected-target :action :selected-action
                :wanted [{:token [:selected-target :wanted-token] :predicted :probability}]}
   :tokens [{:token [:selected-target :wanted-token] :predicted :probability
             :observed :boolean-or-typed-missing :verdict :comparison-verdict}]
   :verdicts #{:predicted-and-observed :predicted-not-observed
               :not-predicted-observed :neither :observation-missing}})

(defn selection-event [decision]
  (if-not decision
    {:status :absent :reason :selection-not-reached :reinforcement :none}
    {:event (if (:action decision) :cascade-selected :selection-abstained)
   :target (get-in decision [:action :target])
   :cascade-id (get-in decision [:action :cascade-id])
   :policy-key (some-> (:action decision) store/policy-view prior/policy-key)
   :reinforcement :none :reason :selection-is-not-outcome}))

(defn- probability? [x]
  (and (number? x) (Double/isFinite (double x)) (<= 0 x 1)))

(defn- valid-row? [target wanted {:keys [token predicted observed verdict]}]
  (and (contains? (:verdicts comparison-interface) verdict)
       (vector? token) (= 2 (count token)) (= target (first token))
       (contains? wanted token) (= predicted (get wanted token)) (probability? predicted)
       (= verdict (cond
                    (and (map? observed) (= :missing (:status observed))) :observation-missing
                    (not (boolean? observed)) :invalid
                    (pos? predicted) (if observed :predicted-and-observed :predicted-not-observed)
                    observed :not-predicted-observed
                    :else :neither))))

(defn- valid-comparison? [action receipt]
  (let [prediction (:prediction receipt)
        wanted-rows (:wanted prediction)
        rows (:tokens receipt)]
    (and (= :wm/token-outcome-comparison-v1 (:schema receipt))
         (= :compared (:status receipt)) (= :frozen (:status prediction))
         (= action (:action prediction)) (= (:target action) (:target prediction))
         (vector? wanted-rows) (every? map? wanted-rows)
         (vector? rows) (seq rows) (every? map? rows)
         (let [wanted (into {} (map (juxt :token :predicted)) wanted-rows)]
           (and (= (count wanted) (count wanted-rows) (count rows))
                (= (set (keys wanted)) (set (map :token rows)))
                (every? #(valid-row? (:target action) wanted %) rows))))))

(defn evaluate
  "Pure rule receipt. Outcome labels are diagnostic inputs, never evidence.
   Consume fix-10a's full comparison receipt, not an approval/grounding label."
  [decision outcome comparison]
  (let [action (:action decision)
        key (some-> action store/policy-view prior/policy-key)
        reason (cond
                 (nil? comparison) :no-outcome-comparison
                 (nil? key) :no-policy-identity
                 (not (valid-comparison? action comparison)) :invalid-outcome-comparison
                 (not-any? #(= :predicted-and-observed (:verdict %)) (:tokens comparison))
                 :no-predicted-and-observed
                 :else :predicted-and-observed)
        reinforce? (= :predicted-and-observed reason)]
    {:rule/id rule-id
     :inputs {:policy-key key :target (:target action) :cascade-id (:cascade-id action)
              :outcome outcome
              :comparison (when comparison
                            {:schema (:schema comparison) :status (:status comparison)
                             :artifact-sha (:artifact-sha comparison)
                             :evidence-sha256 (:evidence-sha256 comparison)
                             :prediction (select-keys (:prediction comparison)
                                                      [:status :target :action :wanted])
                             :tokens (mapv #(select-keys % [:token :predicted :observed :verdict])
                                           (filter map? (:tokens comparison)))})}
     :reinforcement (if reinforce? :increment :none)
     :delta (if reinforce? 1 0) :reason reason}))

(defn close!
  "Apply the declared rule once at the runner's successful close boundary.
   No receipt means no store IO, including on a grounded-change outcome."
  [path decision outcome comparison]
  (let [receipt (evaluate decision outcome comparison)]
    (when (= :increment (:reinforcement receipt))
      (store/record-reinforcement! path decision rule-id))
    receipt))
