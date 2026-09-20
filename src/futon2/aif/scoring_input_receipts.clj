(ns futon2.aif.scoring-input-receipts
  "Read-time state receipts. Uses the declaration provenance status vocabulary."
  (:require [clojure.edn :as edn]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.token-belief-carry :as token-carry]
            [futon2.aif.interpretation-evidence :as evidence]))

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

(defn habit-log [state]
  {:status (cond (nil? state) :not-observed (empty? state) :absent :else :present)
   :reason (when (and (some? state) (empty? state)) :never-read)
   :occurrences (vec state)})

(defn- validate-record*
  "Check retained receipts and their joins without reading any external store."
  [record]
  (let [decision (:decision record)
        candidates (get-in decision [:selection-certificate :candidates])
        initial (:initial-belief-receipt decision)
        stage-path [:selection-certificate :token-belief-stage]
        stage (get-in decision stage-path)
        staged? (contains? (:selection-certificate decision) :token-belief-stage)
        log (:habit-reads record)
        occurrences (:occurrences log)
        errors (cond-> []
                 (or (nil? log) (= :not-observed (:status log))) (conj :habit-receipts-not-observed)
                 (and (seq candidates) (not= :present (:status initial)))
                 (conj :initial-belief-receipt-not-observed))
        errors (if (= :present (:status initial))
                 (let [expected (initial-belief
                                 (mapv #(hash-map :target (:target %) :cascade-problem {:facts (:facts %)})
                                       (:inputs initial)))]
                   (cond-> errors
                     (not= expected initial) (conj :initial-belief-origin-mismatch)
                     (some #(not= (:value initial) (get-in % [:evaluations 0 :incoming-belief]))
                           (get-in decision [:selection-certificate :node-evaluation-traces]))
                     (conj :initial-belief-value-mismatch))) errors)
        ;; Phase 2a is an extension: the original origin and incoming-value
        ;; equalities above still run, including on records without carry.
        errors (cond-> errors
                 (and staged? (not (token-carry/valid-stage? stage initial)))
                 (conj :token-belief-stage-mismatch))]
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
                                                 (or (= :absent (:status receipt))
                                                     (and (= (:sha256 receipt) (sha (:snapshot-edn receipt)))
                                                          (= (:state receipt) (edn/read-string (:snapshot-edn receipt))))))))
                                        occurrences)))
                              (filter #(= :cascade-prior (get-in % [:habit-provenance :source])) candidates)))
               :valid :invalid)
     :errors errors}))

(defn validate-record [record]
  (try (validate-record* record)
       (catch Exception _ {:status :invalid :errors [:incompatible-meaning]})))
