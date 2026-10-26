(ns futon2.aif.selected-outer-cascade-policy
  "Sealed handoff from the strict outer policy selector to cascade execution."
  (:require [clojure.string :as str]
            [futon2.aif.token-outcome :as token-outcome]
            [futon2.aif.token-belief-carry :as token-carry]
            [futon2.aif.scoring-input-receipts :as scoring-inputs]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.assessed-policy-basis :as assessed-basis]
            [futon2.aif.interpretation-evidence :as interpretation-evidence]
            [futon2.aif.outer-policy-evidence :as evidence]
            [futon2.aif.outer-policy-objective :as objective]
            [futon2.aif.outer-canonical-g :as canonical-g]
            [futon2.aif.outer-canonical-selection :as canonical-selection]
            [futon2.aif.previous-run :as previous-run]
            [futon2.aif.wm.executable-cascade :as executable]
            [futon2.aif.wm.family-selection :as family]))

(load-identity/register! *ns* *file*)

(def schema :wm/selected-outer-cascade-policy-v1)
(def ^:private body-keys
  [:schema :status :target-id :policy-id :cascade-digest :selected-entry
   :sealed-menu :menu-pin :evidence-carrier :evidence-pin
   :objective-receipt :objective-pin :recomputation
   :basis-kind :scope :assessed-basis :canonical-frontier :canonical-selection
   :pattern-prediction-input :q6-exclusion])

(defn- pin [path value]
  ;; Pinned text must not depend on the caller's print bindings: a REPL binds
  ;; *print-namespace-maps* true, the click path leaves it false.
  {:path path
   :sha256 (load-identity/sha256
            (.getBytes (binding [*print-namespace-maps* false *print-length* nil
                                 *print-level* nil *print-meta* false]
                         (pr-str value))
                       "UTF-8"))})

(defn objective-pin [receipt]
  (pin "wm://outer-policy-objective-v1" (dissoc receipt :objective-pin)))

(defn carrier-pin [carrier]
  (pin "wm://selected-outer-cascade-policy-v1"
       (select-keys carrier body-keys)))

(defn- seal-body [body]
  (let [body (assoc body :pattern-prediction-input
                    (token-outcome/pattern-prediction-input (:selected-entry body)))]
    (assoc body :carrier-pin (carrier-pin body))))

(defn seal
  "Seal an objective selection, including a replayable draw among equal minima."
  [menu evidence-carrier objective-receipt]
  (if-not (= :selected (:status objective-receipt))
    {:schema schema :status :refused :reason :strict-policy-selection-required
     :objective-status (:status objective-receipt)
     :tied (:tied objective-receipt)}
    (let [entry (first (filter #(and (= (:policy-id %) (get-in objective-receipt [:action :policy-id]))
                                     (= (:cascade-digest %) (get-in objective-receipt [:action :cascade-digest])))
                               (:entries menu)))
          objective-receipt (assoc objective-receipt :objective-pin
                                   (objective-pin objective-receipt))
          body {:schema schema :status :sealed
                :basis-kind :strict-objective
                :target-id (get-in objective-receipt [:action :target])
                :policy-id (get-in objective-receipt [:action :policy-id])
                :cascade-digest (get-in objective-receipt [:action :cascade-digest])
                :selected-entry entry
                :sealed-menu menu :menu-pin (:menu-pin menu)
                :evidence-carrier evidence-carrier
                :evidence-pin (:evidence-pin evidence-carrier)
                :objective-receipt objective-receipt
                :objective-pin (:objective-pin objective-receipt)
                :recomputation {:q2 {:target-id (get-in objective-receipt [:action :target])
                                     :policy-id (get-in objective-receipt [:action :policy-id])
                                     :cascade-digest (get-in objective-receipt [:action :cascade-digest])}
                                :q4 (:policies objective-receipt)
                                :q8 (:comparison objective-receipt)}}]
      (seal-body body))))

(defn seal-assessed
  "Adapt a verified P07a assessed basis into executable identity transport.
  This retains the original menu, full evidence and partial receipt; it does
  not describe the selected action as a full-menu strict optimum."
  [basis]
  (if-let [defect (assessed-basis/verify basis)]
    {:schema schema :status :refused :reason :assessed-policy-basis-required
     :basis-defect defect}
    (let [menu (:menu basis)
          ev (:evidence basis)
          receipt (:receipt basis)
          action (get-in receipt [:assessed-selection :action])
          entry (first (filter #(and (= (:policy-id %) (:policy-id action))
                                     (= (:cascade-digest %) (:cascade-digest action)))
                               (:entries menu)))
          objective-receipt (assoc receipt :objective-pin (objective-pin receipt))
          body {:schema schema :status :sealed
                :basis-kind :assessed-population :scope :assessed-population
                :target-id (:target action) :policy-id (:policy-id action)
                :cascade-digest (:cascade-digest action) :selected-entry entry
                :sealed-menu menu :menu-pin (:menu-pin menu)
                :evidence-carrier ev :evidence-pin (:evidence-pin ev)
                :objective-receipt objective-receipt
                :objective-pin (:objective-pin objective-receipt)
                :assessed-basis basis
                :recomputation {:q2 (select-keys action [:target :policy-id :cascade-digest])
                                :q4 (:policies receipt)
                                :q8 {:status :absent
                                     :reason :assessed-population-basis}}}]
      (seal-body body))))

(defn seal-canonical
  "Seal a selection already made over O3-1's retained canonical frontier.
   The objective receipt remains evidence/reporting, not this choice basis."
  ([menu evidence-carrier objective-receipt frontier selection]
   (seal-canonical menu evidence-carrier objective-receipt frontier selection
                   (:carrier (previous-run/canonical-exclusion nil menu frontier))))
  ([menu evidence-carrier objective-receipt frontier selection q6-exclusion]
   (if-not (= :selected (:status selection))
     {:schema schema :status :refused :basis-kind :canonical-G-frontier
      :reason :canonical-policy-selection-required
      :canonical-status (:status selection) :q6-exclusion q6-exclusion}
     (let [action (:action selection)
          entry (first (filter #(and (= (:policy-id %) (:policy-id action))
                                     (= (:cascade-digest %) (:cascade-digest action))) (:entries menu)))
          objective-receipt (assoc objective-receipt :objective-pin (objective-pin objective-receipt))
          body {:schema schema :status :sealed :basis-kind :canonical-G-frontier
                :target-id (:target action) :policy-id (:policy-id action)
                :cascade-digest (:cascade-digest action) :selected-entry entry
                :sealed-menu menu :menu-pin (:menu-pin menu)
                :evidence-carrier evidence-carrier :evidence-pin (:evidence-pin evidence-carrier)
                :objective-receipt objective-receipt :objective-pin (:objective-pin objective-receipt)
                :canonical-frontier frontier :canonical-selection selection
                :q6-exclusion q6-exclusion
                :recomputation {:q2 (select-keys action [:target :policy-id :cascade-digest])
                                :q4 (:policies objective-receipt) :q8 (:comparison objective-receipt)}}]
       (seal-body body)))))

(defn canonical-policy-stages
  "Project the selected target's canonical menu, frontier and q4 rows into
   the policy-stage shape consumed by Q8. Non-canonical carriers have no such
   projection."
  [carrier]
  (when (= :canonical-G-frontier (:basis-kind carrier))
    (let [target (:target-id carrier)
          entries (filterv #(= target (:target-id %))
                           (get-in carrier [:sealed-menu :entries]))
          frontier (get-in carrier [:canonical-frontier :entries])
          q4-rows (vec (get-in carrier [:recomputation :q4]))
          ordered (fn [keys] (vec (sort-by pr-str keys)))
          key-of #(get-in % [:action :policy-key])
          matched? (fn [entry row]
                     (let [digest (get-in entry [:action :cascade-digest])]
                       (and (= :scored (:status row))
                            (= (:policy-id entry) (:policy-id row))
                            (= digest (:cascade-digest row))
                            (= digest (get-in row [:action-pin :cascade-digest])))))
          q4-matched? (fn [entry row]
                        (and (= (:policy-id entry) (:policy-id row))
                             (= (get-in entry [:action :cascade-digest])
                                (:cascade-digest row))))
          constructed (ordered (map key-of entries))
          scored (ordered (for [entry entries
                                :when (some #(matched? entry %) frontier)]
                            (key-of entry)))
          posterior (ordered (for [entry entries
                                   :when (some #(q4-matched? entry %) q4-rows)]
                               (key-of entry)))]
      {:stages {target {:constructed constructed
                        :admitted constructed
                        :scored scored
                        :posterior posterior}}
       :provenance
       {:menu-pin (or (get-in carrier [:sealed-menu :menu-pin :sha256])
                      (get-in carrier [:menu-pin :sha256]))
        :frontier-pin (get-in carrier [:canonical-frontier :frontier-pin :sha256])
        :q4-pin (interpretation-evidence/value-digest q4-rows)}})))

(defn verify
  "Total semantic verification. Returns nil when valid, otherwise a keyword."
  [carrier]
  (try
    (let [menu (:sealed-menu carrier)
          ev (:evidence-carrier carrier)
          receipt (:objective-receipt carrier)
          raw-receipt (dissoc receipt :objective-pin)
          basis-kind (or (:basis-kind carrier) :strict-objective)
          basis (:assessed-basis carrier)
          entry (:selected-entry carrier)
          action (cond (= :assessed-population basis-kind) (get-in raw-receipt [:assessed-selection :action])
                       (= :canonical-G-frontier basis-kind) (get-in carrier [:canonical-selection :action])
                       :else (:action raw-receipt))
          expected-entry (first (filter #(and (= (:policy-id %) (:policy-id action))
                                             (= (:cascade-digest %) (:cascade-digest action))
                                             (= (:target-id %) (:target action)))
                                        (:entries menu)))
          recomputed (when (and (map? ev) (map? menu))
                       (objective/evaluate ev menu
                                           (if (= :assessed-population basis-kind)
                                             (get-in raw-receipt [:assessed-selection :draw :u])
                                             (get-in raw-receipt [:draw :u]))))]
      (cond
        (not= schema (:schema carrier)) :selected-policy-schema
        (not= :sealed (:status carrier)) :selected-policy-status
        (not (contains? #{:strict-objective :assessed-population :canonical-G-frontier} basis-kind))
        :selected-policy-basis-kind
        (not= (carrier-pin carrier) (:carrier-pin carrier)) :selected-policy-seal
        (family/verify-sealed-menu menu) :selected-policy-menu-invalid
        (not= (:menu-pin menu) (:menu-pin carrier)) :selected-policy-menu-substitution
        (evidence/verify-evidence ev) :selected-policy-evidence-invalid
        (not= (:evidence-pin ev) (:evidence-pin carrier)) :selected-policy-evidence-substitution
        (not= (objective-pin receipt) (:objective-pin receipt)) :selected-policy-objective-seal
        (not= (:objective-pin receipt) (:objective-pin carrier)) :selected-policy-objective-substitution
        (and (= :assessed-population basis-kind)
             (not= :assessed-population (:scope carrier))) :selected-policy-assessed-scope
        (and (= :assessed-population basis-kind) (assessed-basis/verify basis))
        :selected-policy-assessed-basis-invalid
        (and (= :assessed-population basis-kind) (not= menu (:menu basis)))
        :selected-policy-assessed-menu-substitution
        (and (= :assessed-population basis-kind) (not= ev (:evidence basis)))
        :selected-policy-assessed-evidence-substitution
        (and (= :assessed-population basis-kind) (not= raw-receipt (:receipt basis)))
        :selected-policy-assessed-receipt-substitution
        (and (= :assessed-population basis-kind) (not= :partial (:status raw-receipt)))
        :selected-policy-assessed-not-partial
        (and (= :assessed-population basis-kind) (not= raw-receipt recomputed))
        :selected-policy-assessed-recomputation
        (and (= :strict-objective basis-kind) (not= :selected (:status raw-receipt)))
        :selected-policy-not-strict
        (and (= :strict-objective basis-kind)
             (not= receipt (assoc recomputed :objective-pin (objective-pin recomputed))))
        :selected-policy-objective-recomputation
        (and (= :canonical-G-frontier basis-kind)
             (not (previous-run/intact? (:q6-exclusion carrier))))
        :selected-policy-q6-carrier-tampered
        (and (= :canonical-G-frontier basis-kind)
             (not= (:canonical-frontier carrier) (canonical-g/produce menu)))
        :selected-policy-canonical-frontier-replay
        (and (= :canonical-G-frontier basis-kind)
             (not= (:canonical-selection carrier)
                   (canonical-selection/evaluate
                    (:canonical-frontier carrier) menu
                    (get-in carrier [:canonical-selection :draw :u])
                    (set (get-in carrier [:q6-exclusion :excluded-keys])))))
        :selected-policy-canonical-replay
        (nil? entry) :selected-policy-entry-missing
        (not= entry expected-entry) :selected-policy-entry-substitution
        (and (contains? carrier :pattern-prediction-input)
             (not= (:pattern-prediction-input carrier)
                   (token-outcome/pattern-prediction-input entry)))
        :selected-policy-pattern-input-substitution
        (not= (:cascade-digest entry) (get-in entry [:canonical-cascade :digest]))
        :selected-policy-canonical-record
        (not= (:cascade-digest entry) (executable/digest (:canonical-cascade entry)))
        :selected-policy-canonical-record
        (not= (:target-id carrier) (:target-id entry) (:target action)) :selected-policy-target-mismatch
        (not= (:policy-id carrier) (:policy-id entry) (:policy-id action)) :selected-policy-occurrence-mismatch
        (not= (:cascade-digest carrier) (:cascade-digest entry) (:cascade-digest action))
        :selected-policy-digest-mismatch
        :else nil))
    (catch Throwable _ :selected-policy-malformed)))

(defn- with-token-stage
  "Record mission-source facts for observation dispatch, never for scoring.
   Occurrence identifies the sealed selection; it is not a minted enactment."
  [decision occurrence-id]
  (let [{:keys [target want declared-wants]} (:action decision)
        facts (:universe declared-wants)
        source-path (get-in declared-wants [:source :path])
        pinned-path (get-in declared-wants [:target-source-pin :path])
        reason (cond
                 (not= :available (:status declared-wants)) :declared-wants-unavailable
                 (not (seq want)) :declared-wants-empty
                 (not (contains? declared-wants :universe)) :declared-wants-universe-absent
                 (not (and (string? target) (string? source-path)
                           (str/ends-with? source-path (str "/" target ".md"))
                           (or (nil? pinned-path)
                               (and (string? pinned-path)
                                    (str/ends-with? pinned-path (str "/" target ".md"))))))
                 :declared-wants-target-mismatch
                 (not (and (map? facts) (seq facts)
                           (every? boolean? (vals facts))
                           (every? #(contains? facts %) want))) :declared-wants-universe-invalid)]
    (if reason
      (assoc-in decision [:selection-certificate :token-belief-stage-status]
                {:status :unavailable :reason reason})
      (let [problems [{:target target :cascade-problem
                       {:facts facts :want want :interpretations {}}}]
            initialization (scoring-inputs/initial-belief problems)
            stage (token-carry/stage initialization (token-carry/domain-inputs problems)
                                     nil {:occurrence-id occurrence-id})]
        (-> decision
            (assoc :initial-belief-receipt initialization)
            (assoc-in [:selection-certificate :token-belief-stage] stage))))))

(defn decision
  "Project a verified handoff into the downstream decision shape without selection."
  [carrier]
  (if-let [defect (verify carrier)]
    (cond-> {:status :refused :reason defect}
      (and (= :canonical-G-frontier (:basis-kind carrier))
           (previous-run/intact? (:q6-exclusion carrier)))
      (assoc :q6 (:q6-exclusion carrier)))
    (let [entry (:selected-entry carrier)
          assessed? (= :assessed-population (:basis-kind carrier))
          canonical? (= :canonical-G-frontier (:basis-kind carrier))
          canonical-stages (when canonical? (canonical-policy-stages carrier))
          raw-receipt (dissoc (:objective-receipt carrier) :objective-pin)
          selection (when assessed? (:assessed-selection raw-receipt))]
      {:status :selected
       :decision (with-token-stage
                 (cond-> {:status :selected
                           :action (:action entry)
                           :selection-certificate
                           {:selection-route :wm/selected-outer-cascade-policy-v1
                   :pattern-prediction-input
                   (or (:pattern-prediction-input carrier)
                       {:schema token-outcome/pattern-input-schema :status :absent
                        :reason :pattern-prediction-input-not-sealed})
                   :selected-canonical-cascade (:canonical-cascade entry)
                   :selected-outer-cascade-policy
                   (assoc (select-keys carrier [:schema :status :basis-kind :scope
                                                :target-id :policy-id :cascade-digest
                                                :selected-entry :menu-pin :evidence-pin
                                                :objective-pin :carrier-pin])
                          :target-relations (get-in carrier [:sealed-menu :target-relations])
                          :target-relations-source
                          {:at [:outer-task-selection :policy
                                :selected-outer-cascade-policy :sealed-menu
                                :target-relations]
                           :menu-pin (:menu-pin carrier)}
                          :carried-at [:outer-task-selection :policy
                                       :selected-outer-cascade-policy]
                          :sealed-menu {:status :carried-on-receipt
                                        :menu-pin (:menu-pin carrier)})
                   :selection-basis (cond
                                      canonical? {:kind :canonical-G-frontier
                                                  :basis (get-in carrier [:canonical-selection :basis])
                                                  :frontier-pin (get-in carrier [:canonical-selection :frontier-pin])
                                                  :draw (get-in carrier [:canonical-selection :draw])}
                                      assessed?
                                      {:kind :assessed-population
                                       :scope (:scope selection)
                                       :assessed-ids (:assessed-ids selection)
                                       :absent (:absent selection)
                                       :draw (:draw selection)}
                                      :else {:kind :strict-objective})
                   :q2 (get-in carrier [:recomputation :q2])
                   :q4 (get-in carrier [:recomputation :q4])
                   :q8 (get-in carrier [:recomputation :q8])
                   ;; Observed outer-route terms, not a beta update: the
                   ;; objective receipt is sealed and replay-verified above.
                            :precision-row (:precision-row raw-receipt)}}
                   canonical-stages
                   (assoc-in [:selection-certificate :policy-stages]
                             (:stages canonical-stages))
                   canonical-stages
                   (assoc-in [:selection-certificate :stage-provenance]
                             (:provenance canonical-stages))
                   canonical?
                   (assoc-in [:selection-certificate :q6]
                             (:q6-exclusion carrier))
                   (some? (:cascade-id entry))
                   (assoc :selection-law {:candidate (:cascade-id entry)}))
                 (str "outer-selection/" (get-in carrier [:carrier-pin :sha256])))
       :ranked [entry]
       :preselected true})))
