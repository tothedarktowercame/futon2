(ns futon2.aif.outer-selection-trace
  "Pure projection of a retained outer-task-selection receipt
  (schema :wm/outer-task-selection-v1) into a stage-by-stage trace.

  Reader only: adds no key to any producer and touches no selector
  file. Every count comes from paths the receipt already retains;
  a path the receipt does not retain yields a typed absence
  ({:absent <reason-kw>}) in place of a number, never nil and never 0.")

(def ^:private absent-count-reason
  "Typed-absence reason used when a counted collection is not retained."
  :rows-not-retained)

(defn- ncount
  "Count coll, or a typed absence when coll is not retained."
  [coll]
  (if (nil? coll) {:absent absent-count-reason} (count coll)))

(defn- nsum-counts
  "Sum of counts of colls; typed absence when any coll is not retained."
  [colls]
  (if (some nil? colls)
    {:absent absent-count-reason}
    (reduce + (map count colls))))

(defn- nval
  "Value at path, or a typed absence with reason kw."
  ([m path reason]
   (let [v (get-in m path)]
     (if (nil? v) {:absent reason} v))))

(defn- freqs
  "Frequencies of (keep kf rows); {} when no rows carry kf."
  [kf rows]
  (frequencies (keep kf rows)))

(defn trace
  "Project a retained outer-task-selection receipt (the value at
  [:payload :judgment :outer-task-selection] of a selection file)
  into {:schema :wm/outer-selection-trace-v1 :stages [...]}."
  [receipt]
  (let [support (:support receipt)
        excluded (:excluded receipt)
        s0-n (nsum-counts [support excluded])
        s1-in (ncount support)
        s1-out s1-in
        families (get-in receipt [:policy-field :families])
        s1-dropped (freqs :ineligible-reason excluded)
        computed (filter #(= :computed (:status %)) families)
        failed (remove #(= :computed (:status %)) families)
        s2-in s1-out
        s2-out (if (nil? families) {:absent absent-count-reason} (count computed))
        s2-dropped (if (nil? families)
                     {:absent absent-count-reason}
                     (frequencies (mapcat #(map :kind (:failures %)) failed)))
        s3-in s2-out
        s3-out (if (nil? families)
                 {:absent absent-count-reason}
                 (reduce + (map #(count (:policies %)) computed)))
        per-family (when (and (seq families) (seq computed))
                     (into {} (map (juxt :target-id #(count (:policies %))) computed)))
        menu (get-in receipt [:policy :selected-outer-cascade-policy :sealed-menu])
        entries (:entries menu)
        entry-targets (set (map :target-id entries))
        unbound (remove #(entry-targets (:target-id %)) computed)
        unbound-targets (mapv :target-id unbound)
        unbound-kinds (when (seq unbound)
                        (frequencies
                         (mapcat (fn [t] (keep :kind (filter #(= t (:target-id %))
                                                             (:failures menu))))
                                 unbound-targets)))
        s4-in s3-out
        s4-out (ncount entries)
        s4-dropped (if (nil? entries)
                     {:absent absent-count-reason}
                     (merge-with + (freqs :kind (:refusals menu)) unbound-kinds))
        refused-policy-ids (when entries
                             (into {} (map (fn [[k rows]] [k (mapv :policy-id rows)]))
                                   (group-by :kind (:refusals menu))))
        frontier (get-in receipt [:policy :canonical-g-frontier])
        fr-entries (:entries frontier)
        refused-rows (:refused frontier)
        s5-in s4-out
        s5-out (if (nil? fr-entries)
                 {:absent absent-count-reason}
                 (count (filter #(= :scored (:status %)) fr-entries)))]
    {:schema :wm/outer-selection-trace-v1
     :stages
     [{:stage :s0-registry-field
       :where "[:support] + [:excluded]"
       :in s0-n
       :out s0-n
       :dropped {}}
      {:stage :s1-standing-map-ownership
       :where "[:support], :ineligible-reason over [:excluded]"
       :in s1-in
       :out s1-out
       :dropped s1-dropped}
      {:stage :s2-target-readings
       :where "[:policy-field :families] by :status, :kind over :failures"
       :in s2-in
       :out s2-out
       :dropped s2-dropped}
      {:stage :s3-policies
       :where "(count :policies) over computed [:policy-field :families]"
       :in s3-in
       :out s3-out
       :dropped {}
       :per-family (or per-family {:absent absent-count-reason})}
      {:stage :s4-canonical-binding
       :where "[:policy :selected-outer-cascade-policy :sealed-menu :entries] vs :refusals/:failures"
       :in s4-in
       :out s4-out
       :dropped s4-dropped
       :refused-policy-ids (or refused-policy-ids {:absent absent-count-reason})
       :unbound-targets unbound-targets}
      {:stage :s5-canonical-g-frontier
       :where "[:policy :canonical-g-frontier :entries] by :status, :reason over :refused"
       :in s5-in
       :out s5-out
       :dropped (if (nil? refused-rows)
                  {:absent :refused-rows-not-retained}
                  (freqs :reason refused-rows))
       :horizon (nval receipt [:policy :canonical-g-frontier :horizon-choice :horizon]
                      :horizon-choice-not-retained)
       :rows (if (nil? fr-entries)
               {:absent absent-count-reason}
               (mapv #(select-keys % [:target-id :G :rank :status]) fr-entries))}
      {:stage :s6-selection
       :where "[:policy :canonical-policy-selection :status/:selected/:draw]"
       :in s5-out
       :out (if (= :selected (get-in receipt [:policy :canonical-policy-selection :status])) 1 0)
       :dropped {}
       :basis (nval receipt [:policy :canonical-policy-selection :basis] :basis-not-retained)
       :selected-target (nval receipt [:policy :canonical-policy-selection :selected :target-id]
                              :selection-not-retained)
       :tie (or (get-in receipt [:policy :canonical-policy-selection :draw])
                {:absent :no-tie})}]}))
