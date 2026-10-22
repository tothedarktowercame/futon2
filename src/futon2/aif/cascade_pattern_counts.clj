(ns futon2.aif.cascade-pattern-counts
  "One pattern-belief table for a comparison. Consumes arranged candidates'
   retained theta records; never re-reads the ledger or changes transition theta.")

(defn- record-signature [record]
  (select-keys record [:status :trials-count :successes :identities :reason]))

(defn- table-entry [{:keys [status trials-count successes reason]}]
  (case status
    :recorded-trials
    (when (and (integer? trials-count) (pos? trials-count)
               (integer? successes) (<= 0 successes trials-count))
      {:counts [(+ successes 1/2) (+ (- trials-count successes) 1/2)]
       :source status :trials-count trials-count :successes successes})
    :no-recorded-trials
    {:counts [1/2 1/2] :source :jeffreys-prior :trials-count 0 :successes 0}
    :defaulted
    {:counts [1/2 1/2] :source :defaulted :trials-count 0 :successes 0 :reason reason}
    nil))

(defn family-pattern-counts
  "Build counts once per pattern from a family's retained records. Conflicting
   records refuse the whole snapshot (including differing default reasons).
   Missing/invalid unit beliefs refuse only their candidate, with no partial
   count map. Malformed family identities refuse rather than overwrite entries."
  [candidates]
  (if-not (and (vector? candidates) (every? :id candidates)
               (= (count candidates) (count (distinct (map :id candidates)))))
    {:status :refused :kind :invalid-candidate-family}
    (let [rows (mapv (fn [c]
                       (let [{:keys [units patterns]} (get-in c [:precedence :co-apply])]
                         {:id (:id c) :units units
                          :patterns (mapv #(get patterns %) units)})) candidates)
          records (reduce (fn [out p]
                            (if (and (:pattern-id p) (map? (:theta-record p)))
                              (update out (:pattern-id p) (fnil conj []) (:theta-record p))
                              out)) {} (mapcat :patterns rows))
          conflict (first (filter (fn [[_ rs]]
                                    (> (count (distinct (map record-signature rs))) 1)) records))]
      (if conflict
        {:status :refused :kind :inconsistent-pattern-belief
         :pattern-id (key conflict) :records (val conflict)}
        (let [table (into {} (keep (fn [[id rs]]
                                    (when-let [entry (table-entry (first rs))] [id entry])) records))]
          (reduce
           (fn [out {:keys [id units patterns]}]
             (let [missing? (some #(or (nil? (:pattern-id %))
                                      (not (map? (:theta-record %)))) patterns)
                   invalid? (or (not (vector? units)) (empty? units)
                                (not= (count units) (count (distinct units)))
                                (some #(nil? (get table (:pattern-id %))) patterns))]
               (if (or missing? invalid?)
                 (assoc-in out [:refused id]
                           {:kind (if missing? :pattern-belief-missing :pattern-belief-invalid)})
                 (assoc-in out [:pattern-counts id]
                           (zipmap units (map #(get-in table [(:pattern-id %) :counts]) patterns))))))
           {:pattern-counts {} :table table :refused {}} rows))))))
