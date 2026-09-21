(ns futon2.aif.token-belief-carry
  "Versioned initialization staging. V1 retains the historical no-update chain;
   V2 records the declaration and fresh evidence for input-receipt admission.
   Prospective carry identifies the predecessor, never a predicted posterior."
  (:require [futon2.aif.load-identity :as load-identity]
            [futon2.aif.cascade-problems :as problems]))

(load-identity/register! *ns* *file*)

(defn domain-inputs [assembled-problems]
  (mapv (fn [{:keys [target cascade-problem]}]
          {:target target
           :declaration (select-keys cascade-problem [:facts :want :interpretations])})
        assembled-problems))

(defn token-universe [inputs]
  (set (for [{:keys [target declaration]} inputs
             token (problems/problem-tokens (:facts declaration) (:want declaration)
                                            (:interpretations declaration))]
         [target token])))

(defn- legacy-stage
  "Record initialization -> no update -> consumed value. Context identifies
   this selection occurrence, not a fabricated observation; tau stays nil
   until an actual observation supplies it. Previous carry is retained for
   inspection only, with no predecessor/transition authority claimed."
  [initialization inputs previous context]
  (let [universe (token-universe inputs)
        belief (:value initialization)
        occurrence-id (:occurrence-id context)]
    {:schema :wm/token-belief-stage-v1
     :conditioning-status :not-wired
     :z-semantics :per-step-redraw
     :occurrence-id occurrence-id
     :tau nil
     :observation {:status :missing :reason :conditioning-not-wired
                   :occurrence-id occurrence-id :tau nil}
     :initialization initialization
     :domain-inputs inputs
     :carrier {:token-count (count universe)
               :state-count (reduce *' 1 (repeat (count universe) 2))
               :support-count (count (filter (comp pos? val) belief))}
     :prospective-prior previous
     :prospective-prior-authority :not-admitted-for-consumption
     :observation-updates []
     :consumed-source :fresh-fact-initialization
     :continuation-belief belief
     :prospective-carry {:schema :wm/prospective-token-carry-v1
                         :conditioning-status :not-wired
                         :occurrence-id occurrence-id
                         :universe universe
                         :belief belief
                         :initialization-sha256 (:sha256 initialization)}}))

(defn stage [initialization inputs previous context]
  (let [base (legacy-stage initialization inputs previous context)]
    (if-let [observations (:observation-initialization context)]
      (assoc base :schema :wm/token-belief-stage-v2
             :conditioning-status :awaiting-observation-admission
             :consumed-source :pending-input-receipt
             :observation {:status :pending :reason :input-admission-required
                           :occurrence-id (:occurrence-id base) :placement :next-selection}
             :prospective-carry (-> (:prospective-carry base)
                                    (dissoc :belief)
                                    (assoc :schema :wm/prospective-token-carry-v2
                                           :conditioning-status :see-token-belief-input))
             :observation-initialization observations)
      base)))

(defn valid-stage?
  "Check the staged chain against the independently checked initialization.
   An asserted posterior, invented update, changed carry or consumed value
   cannot be passed off as staged evidence. No numerical conditioning runs."
  [receipt initialization]
  (and (= (:inputs initialization)
          (mapv (fn [{:keys [target declaration]}]
                  {:target target :facts (:facts declaration)})
                (:domain-inputs receipt)))
       (= receipt (stage initialization (:domain-inputs receipt)
                         (:prospective-prior receipt)
                         (cond-> {:occurrence-id (:occurrence-id receipt)}
                           (= :wm/token-belief-stage-v2 (:schema receipt))
                           (assoc :observation-initialization (:observation-initialization receipt)))))))
