(ns futon2.aif.contextual-preferences
  "Pure fixture binding derivation. Supplied warrants/evidence are fixture
   authority, never authenticated production authority. No events or adoption."
  (:require [clojure.string :as str])
  (:import (java.nio.charset StandardCharsets)
           (java.security MessageDigest)))

(defn- text? [x] (and (string? x) (not (str/blank? x))))
(defn- revision? [x] (and (integer? x) (pos? x)))
(defn- refusal [reason path]
  {:status :refused :reason reason :path path})

(defn- digest [view]
  ;; Hash the exact supplied UTF-8 view bytes, not an EDN printer's encoding.
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (MessageDigest/getInstance "SHA-256")
                           (.getBytes ^String view StandardCharsets/UTF_8)))))

(defn- field-error [binding fields]
  (some (fn [[path valid?]]
          (let [value (get-in binding path)]
            (cond
              (nil? value) (refusal :missing-prerequisite path)
              (not (valid? value)) (refusal :invalid-prerequisite path))))
        fields))

(defn- binding-error [b]
  (or
   (when-not (map? b) (refusal :invalid-binding []))
   (field-error b
                [[[:fixture?] true?]
                 [[:instance-id] text?]
                 [[:task] text?]
                 [[:feedback] text?]
                 [[:revision] revision?]
                 [[:membership :status] #{:established}]
                 [[:membership :revision] revision?]
                 [[:membership :establisher] text?]
                 [[:membership :recipients] vector?]
                 [[:applicability :status] #{:established :not-applicable}]
                 [[:applicability :evidence] text?]
                 [[:applicability :scope] text?]
                 [[:authority :warrant] text?]
                 [[:authority :actor] text?]
                 [[:authority :instance-id] text?]
                 [[:payload :finding] text?]
                 [[:payload :reason] text?]
                 [[:payload :response-route] text?]
                 [[:payload :view] text?]
                 [[:payload :digest] text?]
                 [[:receipt-standard] #{:authorized-inbox}]
                 [[:inbox-adapter :status] #{:verifiable :missing}]
                 [[:claims-timeliness?] boolean?]])
   (when-not (= (:task b) (get-in b [:applicability :scope]))
     (refusal :scope-mismatch [:applicability :scope]))
   (when-not (= (:instance-id b) (get-in b [:authority :instance-id]))
     (refusal :authority-mismatch [:authority :instance-id]))
   (when (= :missing (get-in b [:inbox-adapter :status]))
     (refusal :adapter-gap [:inbox-adapter]))
   (field-error b [[[:inbox-adapter :evidence] text?]])
   (when (:claims-timeliness? b)
     ;; Fixture clock uses integer ticks; no wall clock or invented duration.
     (field-error b [[[:deadline] #(and (integer? %) (<= 0 %))]]))
   (some (fn [[index recipient]]
           (when-let [error (field-error recipient
                                        [[[:id] text?] [[:role] text?]
                                         [[:reason] text?] [[:evidence] text?]])]
             (update error :path #(into [:membership :recipients index] %))))
         (map-indexed vector (get-in b [:membership :recipients])))
   (let [ids (map :id (get-in b [:membership :recipients]))]
     (when-not (= (count ids) (count (distinct ids)))
       (refusal :duplicate-recipient [:membership :recipients])))
   (when-not (= (digest (get-in b [:payload :view]))
                (get-in b [:payload :digest]))
     (refusal :digest-mismatch [:payload :digest]))))

(defn derive-binding
  "Validate a fixture instance and derive its initial obligations.
   Returns :refused with a typed reason/path, :not-applicable, or :derived.
   Membership is the supplied frozen roster, including affected consumers;
   each entry needs role, reason and evidence. Empty coverage is vacuous and
   earns no delivery evidence. Registry presence is not an input to derivation."
  [binding]
  (if-let [error (binding-error binding)]
    error
    (if (= :not-applicable (get-in binding [:applicability :status]))
      {:status :not-applicable :binding binding :obligations {}}
      (let [recipients (get-in binding [:membership :recipients])]
        {:status :derived
         :binding binding
         :obligations (into {} (map (fn [{:keys [id]}]
                                     [id {:state :pending
                                          :revision (:revision binding)
                                          :digest (get-in binding [:payload :digest])
                                          :receipt-standard :authorized-inbox}])
                                   recipients))
         :coverage {:required (count recipients) :received 0}
         :vacuous? (empty? recipients)
         :delivery-evidence []
         :delivery-complete false
         :consideration :unobserved
         :revision-acceptance :unobserved
         :subsequent-use :unobserved}))))

;; --- Frozen fixture episode: pure event derivation (no mutation, no adoption) ---
;;
;; apply-event derives the next episode state from a state previously derived
;; by derive-binding (plus any applied events). It is a pure function: it
;; returns {:status :applied :state ... :result ...} or a typed
;; {:status :refused :reason ... :path ...}; it never mutates, marks done, or
;; adopts anything. Idempotent replay of the exact same event returns the
;; recorded result with :replayed? true; reusing an event id with conflicting
;; content refuses. Registry absence is recorded as route evidence only; it
;; alone never establishes inbox unavailability.

(defn- event-refusal [reason path] {:status :refused :reason reason :path path})

(defn- event-error [e]
  (or (when-not (map? e) (event-refusal :invalid-event []))
      (when-not (text? (:event-id e)) (event-refusal :missing-prerequisite [:event-id]))
      (when-not (keyword? (:type e)) (event-refusal :missing-prerequisite [:type]))
      (when-not (text? (:actor e)) (event-refusal :missing-prerequisite [:actor]))
      (when-not (contains? #{:feedback-created :delivery-attempted :inbox-receipt
                             :registry-exit :deadline-reached :disputed :amendment}
                           (:type e))
        (event-refusal :invalid-prerequisite [:type]))))

(defn- obligation-for [state recipient]
  (get (:obligations state) recipient))

(defn- authorized-actor? [state actor]
  (or (= actor (get-in state [:binding :authority :actor]))
      (= actor (get-in state [:binding :membership :establisher]))))

(defn- base-event-error [state e]
  (or (event-error e)
      (when-not (contains? #{:derived :not-applicable} (:status state))
        (event-refusal :invalid-state []))
      (when (= :not-applicable (:status state))
        (event-refusal :episode-not-applicable []))
      (when (true? (:vacuous? state))
        (event-refusal :vacuous-episode []))
      (when-let [recorded (get-in state [:event-log (:event-id e)])]
        (if (= (dissoc recorded :result) e)
          {:replay recorded}
          (event-refusal :conflicting-event-reuse [:event-id])))))

(defn- record [state e result]
  (let [state' (-> state
                   (assoc-in [:event-log (:event-id e) :result] result)
                   (update-in [:event-log (:event-id e)] merge (dissoc e :result)))
        state' (if (:replay? result) state (update state' :applied-events (fnil conj []) (:event-id e)))]
    (if (:replay? result)
      {:status :applied :replayed? true :state state :result (or (:result result) result)}
      {:status :applied :state state' :result result})))

(defn apply-event
  "Derive the next frozen-episode state from one event. Pure derivation only:
   there is no mark-done, no adoption, and no transport. Coverage counts
   authorized inbox receipts (digest+revision matched) per recipient; a
   receipt by one recipient never discharges another's obligation. Transport
   acceptance, registry exit, deadlines, disputes, and amendments are all
   recorded or refused as typed data."
  [state e]
  (let [guard (base-event-error state e)]
    (cond
      (:reason guard) guard
      (:replay guard) {:status :applied :replayed? true :state state
                       :result (get-in guard [:replay :result])}
      :else
      (case (:type e)
        :feedback-created
        (if-not (authorized-actor? state (:actor e))
          (event-refusal :unauthorized-actor [:actor])
          (if (:episode-opened state)
            (event-refusal :episode-already-opened [])
            (record (assoc state :episode-opened true) e
                    {:type :feedback-created
                     :coverage (:coverage state)})))
        :delivery-attempted
        (if-not (text? (:recipient e)) (event-refusal :missing-prerequisite [:recipient])
          (if-not (obligation-for state (:recipient e))
            (event-refusal :unknown-recipient [:recipient])
            (record (update state :delivery-evidence conj
                            {:event-id (:event-id e) :recipient (:recipient e)
                             :acceptance :transport})
                    e {:type :delivery-attempted
                       :coverage (:coverage state)
                       :note "transport acceptance is not inbox receipt"})))
        :inbox-receipt
        (let [r (:recipient e)]
          (cond
            (not (text? r)) (event-refusal :missing-prerequisite [:recipient])
            (not (obligation-for state r)) (event-refusal :unknown-recipient [:recipient])
            (not (text? (:digest e))) (event-refusal :missing-prerequisite [:digest])
            (not= (:digest e) (get-in state [:binding :payload :digest]))
            (event-refusal :digest-mismatch [:digest])
            (not (revision? (:revision e))) (event-refusal :missing-prerequisite [:revision])
            (not= (:revision e) (get-in state [:binding :revision]))
            (event-refusal :revision-mismatch [:revision])
            (= :receipted (:state (obligation-for state r)))
            (event-refusal :already-receipted [:recipient])
            :else
            (let [state' (-> state
                             (assoc-in [:obligations r :state] :receipted)
                             (update-in [:coverage :received] inc)
                             (update :delivery-evidence conj
                                     {:event-id (:event-id e) :recipient r
                                      :acceptance :authorized-inbox
                                      :digest (:digest e) :revision (:revision e)}))
                  complete (= (get-in state' [:coverage :received])
                              (get-in state' [:coverage :required]))]
              (record (assoc state' :delivery-complete complete) e
                      {:type :inbox-receipt :recipient r
                       :coverage (:coverage state') :delivery-complete complete
                       :consideration :unobserved :revision-acceptance :unobserved
                       :subsequent-use :unobserved}))))
        :registry-exit
        (if-not (text? (:recipient e)) (event-refusal :missing-prerequisite [:recipient])
          (if-not (obligation-for state (:recipient e))
            (event-refusal :unknown-recipient [:recipient])
            ;; Denominator unchanged; obligation outstanding; absence alone is
            ;; route evidence, never inbox unavailability.
            (record (update state :route-evidence (fnil conj [])
                            {:event-id (:event-id e) :recipient (:recipient e)
                             :registry-presence :absent})
                    e {:type :registry-exit
                       :coverage (:coverage state)
                       :note "registry absence does not establish inbox unavailability"})))
        :deadline-reached
        (if-not (integer? (:tick e)) (event-refusal :missing-prerequisite [:tick])
          (if-not (integer? (get-in state [:binding :deadline]))
            (event-refusal :no-deadline-claimed [:deadline])
            (let [overdue (into {}
                                (map (fn [[id ob]]
                                       [id (if (= :pending (:state ob))
                                             (assoc ob :state :overdue) ob)]))
                                (:obligations state))]
              (record (assoc state :obligations overdue) e
                      {:type :deadline-reached :tick (:tick e)
                       :overdue (mapv key (filter #(= :overdue (:state (val %))) overdue))
                       :note "overdue is never auto-satisfied"}))))
        :disputed
        (let [r (:recipient e)]
          (cond
            (not (text? r)) (event-refusal :missing-prerequisite [:recipient])
            (not (obligation-for state r)) (event-refusal :unknown-recipient [:recipient])
            (not (text? (:claim e))) (event-refusal :missing-prerequisite [:claim])
            (not= :receipted (:state (obligation-for state r)))
            (event-refusal :not-receipted [:recipient])
            :else
            ;; Disagreement counts as receipt, never endorsement; a separate
            ;; dispute record opens without touching coverage or receipt state.
            (record (-> state
                        (assoc-in [:disputes r] {:event-id (:event-id e)
                                                 :claim (:claim e)})
                        (assoc-in [:obligations r :disputed?] true))
                    e {:type :disputed :recipient r
                       :coverage (:coverage state)
                       :note "dispute is separate from receipt"})))
        :amendment
        ;; This fixture interpreter has no amendment pathway; authority or
        ;; not, an amendment event derives nothing and obligations stand.
        (if-not (authorized-actor? state (:actor e))
          (event-refusal :unauthorized-actor [:actor])
          (event-refusal :amendment-unsupported [:type]))))))
