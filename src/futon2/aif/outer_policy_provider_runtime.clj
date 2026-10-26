(ns futon2.aif.outer-policy-provider-runtime
  "Adapt the existing learning authority and pinned outcomes to the current policy menu."
  (:require [futon2.aif.load-identity :as identity]
            [futon2.aif.outer-learning-runtime :as learning]
            [futon2.aif.outer-target-belief :as belief]
            [futon2.aif.outer-target-score :as score]
            [futon2.aif.outer-policy-evidence-provider :as provider]))

(defn- terminal-record [row]
  (let [record (:verified-record row)
        outcome (belief/observation row)]
    (assoc record
           :selected-cascade-digest
           (or (get-in record [:outer-task-selection :policy :selected-outer-cascade-policy :cascade-digest])
               (get-in record [:decision :selection-certificate :selected-canonical-cascade :digest]))
           :observed-outcome (:class outcome))))

(defn- score-record [entry result]
  (let [identity (select-keys entry [:policy-id :target-id :cascade-digest])]
    (if (= :scored (:status result))
      (assoc identity :status :computed :score result)
      (assoc identity :status :absent :reason (:reason result) :score result))))

(defn- score-frontier [score-records]
  (let [computed (filterv #(= :computed (:status %)) score-records)
        absent (filterv #(= :absent (:status %)) score-records)]
    {:menu {:count (count score-records)
            :policy-digests (mapv :cascade-digest score-records)}
     :scored {:count (count computed)
              :policy-digests (mapv :cascade-digest computed)}
     :score-absent {:count (count absent)
                    :policies (mapv #(select-keys % [:policy-id :target-id
                                                      :cascade-digest :reason])
                                    absent)}}))

(defn- policy-terms [score component-pin]
  (let [terms (:terms score)]
    {:ln-e (into {}
                 (map (fn [[k value]]
                        [k {:value value :units :nats :source-pin component-pin}])
                      {:resource (- (:weighted-resource-cost terms))
                       :cadence (- (:recent-selection-opportunity-cost terms))
                       :continuation (:continuation-credit terms)}))}))

(defn- declaration-for [state source vocab component-pin score-records]
  (let [computed (filterv #(= :computed (:status %)) score-records)]
    (if-let [result (:score (first computed))]
      (let [d {:schema :wm/outer-policy-evidence-declaration-v1
               ;; Missing sealed entries are represented only in score-records;
               ;; this map contains no invented declaration for them. P05 owns
               ;; the partial-evidence join.
               :outcome-vocabulary vocab
               :gamma (get-in state [:authority :outer-policy :gamma])
               :preference-authority source
               :preference {:vocabulary vocab :units :probability
                            :authority source
                            :distribution (mapv (:preference-distribution result) vocab)}
               :global-prior {:alpha (mapv (get-in state [:belief :prior :alpha]) vocab)}
               :policies (into {}
                               (map (fn [{:keys [cascade-digest score]}]
                                      [cascade-digest (policy-terms score component-pin)])
                                    computed))}]
        (assoc d :pin {:path "wm://dynamic-outer-policy-declaration"
                       :sha256 (identity/sha256 (.getBytes (pr-str d) "UTF-8"))}))
      {:schema :wm/outer-policy-evidence-declaration-v1
       :reason :no-policy-scores})))

(defn provide
  "Score every sealed entry independently. A score absence remains a typed
  receipt; it never erases another entry or substitutes a numerical score."
  [state menu records]
  (if-not (= :produced (get-in state [:belief :status]))
    (let [learning-belief (:belief state)]
      (cond-> {:schema provider/schema
               :status :refused
               :reason (or (:reason learning-belief) :learning-belief-not-produced)
               :belief-status (:status learning-belief)}
        (contains? learning-belief :details)
        (assoc :details (:details learning-belief))))
    (let [vocab belief/outcome-vocabulary
          source (get-in state [:preference-authority :source])
          component-pin {:path "wm://existing-outer-controller-inputs"
                         :sha256 (identity/sha256
                                  (.getBytes (pr-str [source (get-in state [:belief :source-pin])
                                                       (:controller state)]) "UTF-8"))}
          score-records (mapv (fn [entry]
                                (score-record entry
                                              (score/score (assoc state :target (:target-id entry)))))
                              (:entries menu))
          response (assoc (provider/provided
                           (declaration-for state source vocab component-pin score-records)
                           records
                           {:source-pin source :controller-input-pin component-pin})
                          :score-records score-records
                          :score-frontier (score-frontier score-records))]
      ;; provider/provided pins its base shape; re-pin after adding the
      ;; measured frontier and per-policy receipt.
      (assoc response :source-pin (provider/pin response)))))

(defn production-provider [menu]
  (let [state (learning/load-shadow)]
    (provide state menu (mapv terminal-record (get-in state [:dataset :rows])))))
