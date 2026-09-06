(ns v7-r3a-node-sim
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.set :as set]
            [futon2.aif.belief :as belief]
            [futon2.aif.free-energy :as fe]
            [futon2.aif.observation :as observation]))

(def lab (io/file "holes/labs/wm-contract"))
(def carriers (edn/read-string (slurp (io/file lab "sim/R3a-carriers.edn"))))
(def declared-14 (set (:observation-channels carriers)))
(def declared-8 (set (:likelihood-channels carriers)))
(def declared-6 (set (:never-delivered carriers)))

(defn caller-project [triples]
  ;; Independent transcription of war_machine.clj:6116-6128.
  (if (some #(= :refused (:status %)) (vals triples))
    {}
    (into {} (filter (fn [[_ r]] (= :present (:status r))) triples))))

(def shipped
  {:channels (fn [] belief/channels-with-likelihood)
   :scalar fe/compute-prediction-error
   :envelope fe/channel-prediction-error
   :caller caller-project})

(defn safe [f]
  (try {:value (f)} (catch Throwable t {:threw (.getName (class t)) :message (.getMessage t)})))

(defn component-checks [node]
  (let [live (set ((:channels node))) complement (set/difference declared-14 declared-8)
        present-runs
        (mapv (fn [{:keys [id channel observed prediction opts]}]
                (let [obs {channel observed}
                      envelope ((:envelope node) obs channel prediction opts)
                      scalar ((:scalar node) observed prediction (assoc opts :channel channel))]
                  {:id id :equal? (= envelope scalar) :envelope envelope :scalar scalar}))
              (:present-fixtures carriers))
        missing-observation (observation/observe {})
        absence-runs
        [{:id :source-field-missing
          :record ((:envelope node) missing-observation :annotation-health {:mean 0.5 :variance 0.25})
          :expect-reason :source-field-missing}
         {:id :status-metadata-missing
          :record ((:envelope node) {} :annotation-health {:mean 0.5 :variance 0.25})
          :expect-reason :status-metadata-missing}]
        absence-ok? (every? (fn [{:keys [record expect-reason]}]
                              (and (= :absent (:status record)) (= expect-reason (:reason record))
                                   (not-any? #(contains? record %) [:error :precision :weighted-error]))) absence-runs)
        broken-runs
        (mapv (fn [{:keys [id prediction offenders]}]
                (let [r ((:scalar node) 0.5 prediction {:channel :annotation-health})]
                  {:id id :record r :expected (set offenders)
                   :actual (set (map :member (:offending r)))})) (:broken-models carriers))
        absent-broken ((:envelope node) missing-observation :annotation-health {})
        broken-ok? (and (every? #(and (= :refused (get-in % [:record :status]))
                                      (= (:expected %) (:actual %))) broken-runs)
                        (= :refused (:status absent-broken)))
        valid (into {} (map (fn [ch] [ch {:status :present :channel ch :error 0.25}]) declared-8))
        refused (assoc-in valid [:annotation-health :status] :refused)
        projected ((:caller node) refused)]
    [{:id :channel-set-is-eight-of-fourteen
      :result (if (and (= live declared-8) (= 8 (count live))
                       (set/subset? live declared-14) (not= live declared-14)
                       (= complement declared-6) (= 6 (count complement))) :pass :fail)
      :live-count (count live) :declared-count (count declared-14)
      :never-delivered (vec (sort complement))}
     {:id :envelope-form-equals-scalar-form-on-a-present-channel
      :result (if (every? :equal? present-runs) :pass :fail)
      :comparisons (count present-runs)
      ;; STRUCTURAL EQUALITY of the whole record, not a numeric deviation: the
      ;; two forms must agree on every key, so there is no tolerance to report
      ;; and no "max deviation 0.0" to read as an arithmetic result.
      :comparison-kind :whole-record-equality
      :unequal (mapv :id (remove :equal? present-runs))}
     {:id :absent-observation-omits-and-carries-the-envelopes-reason
      :result (if absence-ok? :pass :fail)
      :reasons-exercised (mapv :expect-reason absence-runs)
      :unconstructed-reasons [] :runs absence-runs}
     {:id :model-failure-refuses-and-names-every-offender
      :result (if broken-ok? :pass :fail) :comparisons (count broken-runs)
      :absent-and-broken-status (:status absent-broken) :runs broken-runs}
     {:id :any-refusal-empties-the-whole-error-map
      :result (if (= {} projected) :pass :fail)
      :input-channels (count refused) :refusals 1 :result-count (count projected)}]))

(defn trace-files []
  (->> (.listFiles (io/file "data/wm-trace"))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
       (sort-by #(.getName %))))

(defn read-records [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [out []]
      (let [v (try (edn/read {:eof ::eof :default (fn [_ v] v)} r) (catch Exception _ ::bad))]
        (cond (= v ::eof) out (= v ::bad) (recur out) :else (recur (conj out v)))))))

(def corpus-records
  (vec (mapcat (fn [f] (map-indexed (fn [i r] {:file (.getName f) :index i :record r})
                                    (read-records f))) (trace-files))))

(defn corpus-measurements []
  (let [coverage
        (mapv (fn [{:keys [file index record]}]
                (let [errors (:prediction-errors record) channels (set (keys errors))]
                  {:file file :index index :count (count channels)
                   :channels channels :outside (set/difference channels declared-8)})) corpus-records)
        distribution (into (sorted-map) (frequencies (map :count coverage)))
        first-at-level (into (sorted-map) (map (fn [[level xs]] [level (:file (first xs))])
                                                (group-by :count coverage)))
        outside (filter #(seq (:outside %)) coverage)
        multi (filter #(> (count (get-in % [:record :micro-step-trace])) 1) corpus-records)
        terminal-diffs
        (mapv (fn [{:keys [file index record]}]
                (let [steps (:micro-step-trace record)
                      a (:error-magnitude (first steps)) b (:error-magnitude (last steps))]
                  {:file file :index index :first a :terminal b :difference (- (double b) (double a))})) multi)
        changed (filter #(not (zero? (:difference %))) terminal-diffs)
        entries (vec (mapcat (fn [{:keys [file index record]}]
                               (map (fn [[ch r]] {:file file :index index :channel ch :entry r})
                                    (:prediction-errors record))) corpus-records))
        weighted (filter #(every? number? ((juxt :weighted-error :error :precision) (:entry %))) entries)
        weight-results (mapv #(assoc (select-keys % [:file :index :channel])
                                     :deviation (Math/abs (- (double (get-in % [:entry :weighted-error]))
                                                             (* (double (get-in % [:entry :error]))
                                                                (double (get-in % [:entry :precision])))))) weighted)
        triple-events (vec (mapcat (fn [{:keys [file index record]}]
                                     (map #(assoc % :trace/file file :trace/index index)
                                          (:prediction-triple-events record))) corpus-records))]
    {:coverage {:records (count coverage) :distribution distribution
                :first-file-at-count first-at-level :outside-declared-count (count outside)
                :outside-declared (vec outside)}
     :terminal-step {:multi-step-records (count multi) :different-first-terminal (count changed)
                     :difference-min (when (seq terminal-diffs) (apply min (map :difference terminal-diffs)))
                     :difference-max (when (seq terminal-diffs) (apply max (map :difference terminal-diffs)))
                     :difference-mean (when (seq terminal-diffs) (/ (reduce + (map :difference terminal-diffs))
                                                                    (count terminal-diffs)))}
     :weighted-identity {:entries (count entries) :comparisons (count weighted)
                         :max-deviation (reduce max 0.0 (map :deviation weight-results))
                         :mismatches (vec (filter #(pos? (:deviation %)) weight-results))}
     :typed-persistence {:entries (count entries) :status-distribution (into (sorted-map) (frequencies (map #(get-in % [:entry :status]) entries)))
                         :absent-or-refused (count (filter #{:absent :refused} (map #(get-in % [:entry :status]) entries)))
                         :prediction-triple-event-count (count triple-events)
                         :prediction-triple-status-distribution (into (sorted-map) (frequencies (map :status triple-events)))
                         :prediction-triple-events triple-events}}))

(def corpus (corpus-measurements))
(def corpus-checks
  ;; EVERY :result HERE IS DERIVED FROM THE MEASUREMENT BESIDE IT. An earlier
  ;; state of this file wrote :result :pass as a literal on all four, so an
  ;; injected mismatch (deviation 42.0, 7 channels outside the declaration, 99
  ;; absent/refused entries) still reported :pass and still reached
  ;; :verdict :pass. The numbers were right; the PASS was not evidence.
  [{:id :production-channel-coverage-and-its-era
    :result (if (zero? (:outside-declared-count (:coverage corpus))) :pass :fail)
    :gated-on "no persisted channel outside the declared eight"
    :finding-policy "The coverage DISTRIBUTION is reported, not gated -- six distinct sets is a finding, not a failure. What is gated is membership."
    :measurements (:coverage corpus)}
   {:id :persisted-errors-are-the-terminal-micro-step :result :measured
    :measured-not-gated "There is no property here to fail: the trace stores one aggregate magnitude per step, so this reports how far the terminal step sits from step 0 and nothing about it can be wrong."
    :measurements (:terminal-step corpus)
    :limit "The trace stores aggregate per-step error magnitude, not each step's channel map; terminal channel identity is established by the cited return seam."}
   {:id :weighted-error-identity-on-the-persisted-records
    :result (if (and (zero? (:max-deviation (:weighted-identity corpus)))
                     (empty? (:mismatches (:weighted-identity corpus)))) :pass :fail)
    :gated-on "weighted-error = error x precision at deviation 0.0 on every comparable entry"
    :finding-policy "Overlaps slice 3 at record rather than micro-step grain."
    :measurements (:weighted-identity corpus)}
   {:id :no-persisted-record-carries-an-absent-or-refused-status
    :result (if (zero? (:absent-or-refused (:typed-persistence corpus))) :pass :fail)
    :gated-on "no persisted :prediction-errors entry carries :absent or :refused"
    :measurements (:typed-persistence corpus)}])

(def plants
  [{:id :seven-channel-projection :node (assoc shipped :channels #(disj declared-8 :ticks-firing-ratio))}
   {:id :nine-channel-projection :node (assoc shipped :channels #(conj declared-8 :loop-health))}
   {:id :absent-observation-substitutes-zero
    :node (assoc shipped :envelope (fn [_ ch prediction & [opts]]
                                     ((:scalar shipped) 0.0 prediction (assoc opts :channel ch))))}
   {:id :absent-record-carries-error
    :node (assoc shipped :envelope (fn [obs ch prediction & [opts]]
                                     (let [r ((:envelope shipped) obs ch prediction opts)]
                                       (if (= :absent (:status r)) (assoc r :error 0.0) r))))}
   {:id :refusal-downgraded-to-absent
    :node (assoc shipped :scalar (fn [observed prediction opts]
                                   (let [r ((:scalar shipped) observed prediction opts)]
                                     (if (= :refused (:status r)) (assoc r :status :absent) r))))}
   {:id :offending-names-only-first
    :node (assoc shipped :scalar (fn [observed prediction opts]
                                   (let [r ((:scalar shipped) observed prediction opts)]
                                     (if (= :refused (:status r)) (update r :offending #(vec (take 1 %))) r))))}
   {:id :refusal-does-not-empty-map :node (assoc shipped :caller identity)}
   {:id :sign-flipped-error
    :node (assoc shipped :scalar (fn [observed prediction opts]
                                   (let [r ((:scalar shipped) observed prediction opts)]
                                     (if (= :present (:status r))
                                       (assoc r :error (- (:error r)) :weighted-error (- (:weighted-error r))) r))))}
   {:id :precision-without-minimum-variance-floor
    :node (assoc shipped :scalar (fn [observed prediction opts]
                                   ((:scalar shipped) observed prediction (assoc opts :min-variance Double/MIN_VALUE))))}])

(def discrimination-limits
  ;; Plants the harness CANNOT catch, run and recorded rather than omitted.
  ;; :sign-flipped-error above is caught only because it desynchronises the
  ;; scalar form from the envelope form. This harness holds NO independent
  ;; arithmetic reference for eps -- slice 3 (R8) owns that -- so a sign
  ;; convention wrong in BOTH forms at once passes everything here.
  [{:id :sign-flipped-in-both-forms
    :note "The same flip as :sign-flipped-error, applied to the scalar AND the envelope, so the two stay in step."
    :node (let [flip (fn [f] (fn [& args]
                               (let [r (apply f args)]
                                 (if (= :present (:status r))
                                   (assoc r :error (- (:error r))
                                            :weighted-error (- (:weighted-error r)))
                                   r))))]
            (assoc shipped :scalar (flip fe/compute-prediction-error)
                           :envelope (flip fe/channel-prediction-error)))}])

(def fixture-checks (component-checks shipped))
(def checks (vec (concat fixture-checks corpus-checks)))
(def plant-results
  (mapv (fn [{:keys [id node]}]
          (let [failed (mapv :id (filter #(= :fail (:result %)) (component-checks node)))]
            {:id id :result (if (seq failed) :caught :escaped) :caught-by failed
             :would-be-caught (boolean (seq failed))})) plants))
(def limit-results
  (mapv (fn [{:keys [id note node]}]
          (let [failed (mapv :id (filter #(= :fail (:result %)) (component-checks node)))]
            {:id id :note note :caught-by failed
             :would-be-caught (boolean (seq failed))
             :result (if (seq failed) :caught :escaped)}))
        discrimination-limits))
(def all-caught (every? #(= :caught (:result %)) plant-results))
(def all-pass (every? #(contains? #{:pass :measured} (:result %)) checks))

(def receipt
  {:harness :v7-r3a-node-sim :row :V7 :slice 6 :node :R3a :stage "BELIEVE"
   :stage-at "p4ng/empirics-futon/control-stages.edn:19"
   :carriers "holes/labs/wm-contract/sim/R3a-carriers.edn"
   :reference-independence "The 14-channel carrier, 8-channel projection and caller refusal rule come only from R3a-carriers.edn. Production vars supply only node-under-test results."
   :declaration-sites (:declaration-sites carriers)
   :equation-registry {:rows 0 :evidence "grep -n ':node :R3a' holes/labs/wm-contract/aif-equations.edn returned no matches"}
   :checks checks :verdict (if all-pass :pass :fail)
   :negative-controls {:plants plant-results :n (count plant-results) :all-caught all-caught
                       :discrimination-limits limit-results}
   :corpus {:root "data/wm-trace" :read-only true :file-count (count (trace-files))
            :record-count (count corpus-records)}
   :not-done ["No epsilon arithmetic replay was used as R3a's principal check; slice 3 owns that arithmetic."
              "No production, registry, ledger, document, p4ng, or data file changed."
              "No live tick and no run lock."]})

(def out (io/file lab "runs/V7-R3a-node-sim/00-r3a.edn"))
(.mkdirs (.getParentFile out))
(with-open [w (io/writer out)] (binding [*out* w] (pp/pprint receipt)))
(doseq [c checks] (println (format "  %-62s %s" (name (:id c)) (name (:result c)))))
(doseq [p plant-results] (println "  planted" (:id p) (:result p) (:caught-by p)))
(doseq [l limit-results] (println "  discrimination-limit" (:id l) (:result l) "would-be-caught" (:would-be-caught l)))
(println "  verdict" (:verdict receipt) "all-caught" all-caught "receipt" (str out))
(System/exit (if (and all-pass all-caught) 0 1))
