(ns futon2.aif.scoring-input-receipts
  "Read-time state receipts. Uses the declaration provenance status vocabulary."
  (:require [futon2.aif.load-identity :as load-identity]
            [futon2.aif.preference-audit :as preference-audit]
            [futon2.aif.focus-receipt :as focus-receipt]
            [clojure.edn :as edn]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.token-belief-carry :as token-carry]
            [futon2.aif.token-belief-predecessor :as token-predecessor]
            [futon2.aif.interpretation-evidence :as evidence]))

(load-identity/register! *ns* *file*)

(def ^:dynamic *habit-reads* nil)
(def ^:dynamic *habit-read-purpose* :unspecified)

(defn sha [text] (evidence/sha256 (.getBytes ^String text "UTF-8")))

(defn initial-belief [problems]
  (let [inputs (mapv (fn [p] {:target (:target p) :facts (get-in p [:cascade-problem :facts])}) problems)
        snapshot (pr-str inputs)
        value (model/observed-belief
               (set (for [{:keys [target facts]} inputs [token v] facts :when (true? v)]
                      [target token])))]
    {:status :present :origin :assembled-target-facts
     :derivation :target-qualified-true-facts-point-mass-v1
     :snapshot-edn snapshot :sha256 (sha snapshot) :inputs inputs :value value}))

(defn with-preference-audit [decision]
  (preference-audit/attach decision))

(defn habit-log [state]
  {:status (cond (nil? state) :not-observed (empty? state) :absent :else :present)
   :reason (when (and (some? state) (empty? state)) :never-read)
   :occurrences (vec state)})

(def ^:private fold-fields [:counts :samples :alpha :folded-from])

(defn- matching-habit-reads [snapshot log]
  (into []
        (keep-indexed (fn [i occurrence]
                        (when (and (string? (:sha256 snapshot))
                                   (= (:sha256 snapshot)
                                      (get-in occurrence [:receipt :sha256])))
                          i)))
        (:occurrences log)))

(defn- ln-e-source
  "Where the live ln E came from, read off the route DECISION took."
  [decision consumed?]
  (cond
    (= :wm/selected-outer-cascade-policy-v1
       (get-in decision [:selection-certificate :selection-route]))
    :outer-controller-inputs
    consumed? :habit-fold
    :else :not-observed))

(defn habit-fold-snapshot
  "Retain the supplied selection input, without re-reading or consuming it.
  Hash the full fold exactly as attach-state does; expose its count fields
  beside that snapshot for inspection. Nil means the runner did not get it."
  [fold log decision]
  (if (nil? fold)
    {:schema :wm/habit-fold-snapshot-v1
     :status :absent :reason :fold-not-passed-to-runner}
    (let [text (pr-str fold)
          snapshot (merge {:schema :wm/habit-fold-snapshot-v1 :status :present
                           :snapshot-edn text :sha256 (sha text)}
                          (select-keys fold fold-fields))
          consumed-by (matching-habit-reads snapshot log)
          consumed? (boolean (seq consumed-by))]
      (assoc snapshot :consumed? consumed? :consumed-by consumed-by
             :ln-e-source (ln-e-source decision consumed?)))))

(defn- intact-fold-snapshot? [snapshot]
  (try
    (let [text (:snapshot-edn snapshot)]
      (and (string? text)
           (= (:sha256 snapshot) (sha text))
           (= (select-keys snapshot fold-fields)
              (select-keys (edn/read-string text) fold-fields))))
    (catch Exception _ false)))

(defn- validate-record*
  "Check retained receipts and their joins without reading any external store."
  [record]
  (let [decision (:decision record)
        candidates (get-in decision [:selection-certificate :candidates])
        initial (:initial-belief-receipt decision)
        stage-path [:selection-certificate :token-belief-stage]
        stage (get-in decision stage-path)
        staged? (contains? (:selection-certificate decision) :token-belief-stage)
        input (get-in decision [:selection-certificate :token-belief-input])
        input? (contains? (:selection-certificate decision) :token-belief-input)
        log (:habit-reads record)
        occurrences (:occurrences log)
        snapshot (:habit-fold-snapshot record)
        snapshot-present? (= :present (:status snapshot))
        consumed-by (matching-habit-reads snapshot log)
        errors (cond-> []
                 (nil? snapshot) (conj :habit-fold-snapshot-not-observed)
                 (and snapshot-present? (not (intact-fold-snapshot? snapshot)))
                 (conj :habit-fold-snapshot-hash-mismatch)
                 (and snapshot-present?
                      (or (not= (:consumed? snapshot) (boolean (seq consumed-by)))
                          (not= (:consumed-by snapshot) consumed-by)))
                 (conj :habit-fold-consumption-unwitnessed)
                 (and snapshot-present?
                      (not= (:ln-e-source snapshot)
                            (ln-e-source decision (boolean (seq consumed-by)))))
                 (conj :habit-fold-ln-e-source-not-route)
                 (and (contains? (:selection-certificate decision) :focus-receipt)
                      (not (focus-receipt/valid? decision (get-in decision [:selection-certificate :focus-receipt]))))
                 (conj :focus-receipt-mismatch)
                 (and (contains? (:selection-certificate decision) :preference-audit)
                      (not (preference-audit/valid? decision (get-in decision [:selection-certificate :preference-audit]))))
                 (conj :preference-audit-mismatch)
                 (or (nil? log) (= :not-observed (:status log))) (conj :habit-receipts-not-observed)
                 (and (seq candidates) (not= :present (:status initial)))
                 (conj :initial-belief-receipt-not-observed))
        errors (if (= :present (:status initial))
                 (let [expected (initial-belief
                                 (mapv #(hash-map :target (:target %) :cascade-problem {:facts (:facts %)})
                                       (:inputs initial)))]
                   (cond-> errors
                     (not= expected initial) (conj :initial-belief-origin-mismatch)
                     (some #(not= (if (= :wm/token-belief-input-v3 (:schema input))
                                      (:continuation-belief input) (:value initial))
                                  (get-in % [:evaluations 0 :incoming-belief]))
                           (get-in decision [:selection-certificate :node-evaluation-traces]))
                     (conj :initial-belief-value-mismatch))) errors)
        ;; V3 replays signed updates before accepting its consumed q0. Historical
        ;; versions still require the original initializer equality.
        errors (cond-> errors
                 (and staged? (not (token-carry/valid-stage? stage initial)))
                 (conj :token-belief-stage-mismatch)
                 (and input? (or (not staged?)
                                 (not (token-predecessor/valid-input? input stage))))
                 (conj :token-belief-input-mismatch))]
    {:status (if (and (empty? errors)
                      (every? (fn [c]
                                (let [p (:habit-provenance c)]
                                  (some (fn [{:keys [receipt consumption purpose]}]
                                          (let [ids (:candidate-ids consumption)
                                                i (first (keep-indexed #(when (= (:id c) %2) %1) ids))]
                                            (and (= :joint-selection purpose)
                                                 (= (set (map :id candidates)) (set ids))
                                                 (some? i)
                                                 (= (:habit c) (nth (:masses consumption) i))
                                                 (= (:policy-key p) (nth (:policy-keys consumption) i))
                                                 (= (:count p) (get-in receipt [:state :counts (:policy-key p)] 0))
                                                 (= (:alpha p) (get-in receipt [:state :alpha]))
                                                 (= (:samples p) (get-in receipt [:state :samples]))
                                                 ;; an absent store read carries no snapshot;
                                                 ;; an absent enactment fold carries the initial
                                                 ;; state it consumed, so any snapshot is checked
                                                 (or (and (= :absent (:status receipt))
                                                          (nil? (:snapshot-edn receipt)))
                                                     (and (= (:sha256 receipt) (sha (:snapshot-edn receipt)))
                                                          (= (:state receipt) (edn/read-string (:snapshot-edn receipt))))))))
                                        occurrences)))
                              ;; the store (pre-drop records) and the enactment fold
                              ;; (M-wm-wiring step 8) are checked the same way: the
                              ;; consumed state is in the occurrence's receipt
                              (filter #(#{:cascade-prior :enactment-fold}
                                        (get-in % [:habit-provenance :source])) candidates)))
               :valid :invalid)
     :errors errors
     ;; a record whose selection read the legacy habit store predates step 8:
     ;; it validates as it did, and says it is history
     :history (if (some #(= :cascade-prior (get-in % [:habit-provenance :source])) candidates)
                :pre-drop-store-read
                :enactment-fold)}))

(defn validate-record [record]
  (try (validate-record* record)
       (catch Exception _ {:status :invalid :errors [:incompatible-meaning]})))
