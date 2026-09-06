(ns v7-r3-node-sim
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [futon2.aif.belief :as belief]))

(def lab (io/file "holes/labs/wm-contract"))
(def carriers (edn/read-string (slurp (io/file lab "sim/R3-carriers.edn"))))
(def statuses (:statuses carriers))
(def signs (:channel-signs carriers))

(defn dev [a b] (Math/abs (- (double a) (double b))))
(defn max-dev [a b]
  (if (= (set (keys a)) (set (keys b)))
    (reduce max 0.0 (map #(dev (get a %) (get b %)) (keys a)))
    ##Inf))

(defn ref-driver [errors]
  (let [xs (for [[ch m] errors
                 :when (and (contains? signs ch) (number? (:weighted-error m))
                            (number? (:precision m)))]
             {:sign (get signs ch) :weighted-error (:weighted-error m) :precision (:precision m)})
        denominator (reduce + 0.0 (map :precision xs))]
    (when (and (seq xs) (pos? denominator))
      (/ (reduce + 0.0 (map #(* (:sign %) (:weighted-error %)) xs)) denominator))))

(defn ratio-a []
  (let [{:keys [diagonal-gain overrides]} (:a-matrix carriers)]
    (into {} (for [o statuses]
               [o (into {} (for [s statuses]
                             [s (double (get overrides [o s] (if (= o s) diagonal-gain 1.0)))]))]))))

(defn ref-a []
  (let [r (ratio-a)]
    (into {} (for [o statuses]
               [o (into {} (for [s statuses]
                             [s (/ (get-in r [o s])
                                   (reduce + (map #(get-in r [% s]) statuses)))]))]))))

(defn ref-predict [q]
  ;; :transition :identity in the carrier transcription.
  (into {} (map (fn [s] [s (double (get q s 0.0))]) statuses)))

(defn ref-normalise [p]
  (let [total (reduce + (vals p))]
    (if (zero? total)
      (zipmap statuses (repeat (/ 1.0 (count statuses))))
      (into {} (map (fn [[k v]] [k (/ v total)]) p)))))

(defn ref-filter [prior {:keys [type weight]}]
  (let [pred (ref-predict prior)
        kappa (/ (Math/log (+ 1.0 (double weight))) (Math/log 2.0))
        a (ref-a)]
    (if (some #{type} statuses)
      (ref-normalise (into {} (map (fn [[s p]] [s (* p (Math/pow (get-in a [type s]) kappa))]) pred)))
      prior)))

(defn ref-health [p]
  (let [raw (reduce + (map (fn [[s mass]] (* mass (get (:health-weights carriers) s 0.0))) p))]
    (max 0.0 (min 1.0 (/ (+ raw 1.0) 2.0)))))

(defn ref-attribution []
  (let [{:keys [beliefs driver step max-steps scale]} (:attribution carriers)
        anneal (max 0.0 (- 1.0 (/ step max-steps)))
        event-weight (* (min 1.0 (Math/abs driver)) anneal scale)
        incons (into {} (map (fn [[e p]] [e (- 1.0 (ref-health p))]) beliefs))
        norm (/ (* event-weight (count beliefs)) (reduce + (vals incons)))]
    {:event-weight event-weight
     :weights (into {} (map (fn [[e x]] [e (* x norm)]) incons))}))

(def shipped
  {:driver (fn [errors] (binding [belief/*r3d-multichannel?* true]
                          (belief/r3d-aggregate-driver errors)))
   :single (fn [errors] (binding [belief/*r3d-multichannel?* false]
                          (belief/r3d-aggregate-driver errors)))
   :filter (fn [prior event] (belief/categorical-filter-step
                              prior event belief/observation-model-v1
                              belief/transition-model-v1 {:weight (:weight event)}))
   :anneal (fn [step] (max 0.0 (- 1.0 (/ (double step) 3.0))))
   :attribution (fn []
                  (let [{:keys [beliefs driver step max-steps scale]} (:attribution carriers)
                        event-weight (* (min 1.0 (Math/abs driver))
                                        (max 0.0 (- 1.0 (/ (double step) max-steps))) scale)
                        incons (into {} (map (fn [[e p]] [e (- 1.0 (belief/entity-expected-health p))]) beliefs))
                        norm (/ (* event-weight (count beliefs)) (reduce + (vals incons)))]
                    {:event-weight event-weight
                     :weights (into {} (map (fn [[e x]] [e (* x norm)]) incons))}))})

(defn safely [f]
  (try {:value (f)} (catch Throwable t {:threw (.getName (class t)) :message (.getMessage t)})))

(defn component-checks [node]
  (let [driver-runs (mapv (fn [{:keys [id errors]}]
                            (let [actual ((:driver node) errors) expected (ref-driver errors)]
                              {:id id :actual (:driver actual) :expected expected :record actual}))
                          (:driver-fixtures carriers))
        driver-devs (map #(if (and (number? (:actual %)) (number? (:expected %)))
                            (dev (:actual %) (:expected %)) ##Inf) driver-runs)
        single-errors {:annotation-health {:weighted-error 0.375 :precision 64.0}
                       :sorry-count-norm {:weighted-error 99.0 :precision 1.0}}
        single ((:single node) single-errors)
        unknown-runs (mapv (fn [{:keys [id errors reason]}]
                             {:id id :reason reason :record ((:driver node) errors)})
                           (:unknown-fixtures carriers))
        status-ok (and (every? #(and (= :unknown (get-in % [:record :status]))
                                     (= (:reason %) (get-in % [:record :reason]))
                                     (not (contains? (:record %) :driver))) unknown-runs)
                       (= #{:present :unknown}
                          (set (concat (map #(get-in % [:record :status]) driver-runs)
                                       (map #(get-in % [:record :status]) unknown-runs)))))
        filter-runs (mapv (fn [{:keys [id prior event]}]
                            (let [actual (safely #((:filter node) prior event))
                                  expected (ref-filter prior event)]
                              {:id id :actual (:value actual) :expected expected :threw (:threw actual)}))
                          (:filter-fixtures carriers))
        filter-devs (map #(if (map? (:actual %)) (max-dev (:actual %) (:expected %)) ##Inf) filter-runs)
        attr (safely #((:attribution node))) ref-attr (ref-attribution)
        weights (vals (get-in attr [:value :weights]))
        mean-weight (when (seq weights) (/ (reduce + weights) (count weights)))
        attr-dev (if mean-weight (dev mean-weight (get-in attr [:value :event-weight])) ##Inf)
        attr-reference-dev (if (map? (get-in attr [:value :weights]))
                             (max-dev (get-in attr [:value :weights]) (:weights ref-attr)) ##Inf)]
    [{:id :multichannel-driver-agrees-with-independent-reference
      :result (if (every? zero? driver-devs) :pass :fail)
      :comparisons (count driver-runs) :max-deviation (reduce max 0.0 driver-devs)
      :exact-dyadic true}
     {:id :single-channel-driver-is-annotation-health-weighted-error
      :result (if (and (= :present (:status single)) (= 0.375 (:driver single))) :pass :fail)
      :actual (:driver single) :expected 0.375}
     {:id :driver-record-has-exactly-present-and-unknown-statuses
      :result (if status-ok :pass :fail)
      :fixtures (mapv #(select-keys % [:id :reason :record]) unknown-runs)}
     {:id :categorical-filter-agrees-with-independent-tempered-bayes
      :result (if (every? #(< % 1.0e-15) filter-devs) :pass :fail)
      :comparisons (count filter-runs) :max-deviation (reduce max 0.0 filter-devs)
      :kappa-zero-max-deviation (first filter-devs) :tolerance 1.0e-15}
     {:id :per-entity-attribution-mean-equals-event-weight
      :result (if (and (< attr-dev 1.0e-15) (< attr-reference-dev 1.0e-15)) :pass :fail)
      :entities (count weights) :mean-weight mean-weight
      :event-weight (get-in attr [:value :event-weight]) :max-deviation attr-dev
      :independent-reference-max-deviation attr-reference-dev
      :min-individual-weight (when (seq weights) (apply min weights))
      :max-individual-weight (when (seq weights) (apply max weights))}]))

(defn trace-files []
  (->> (.listFiles (io/file "data/wm-trace"))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
       (sort-by #(.getName %))))

(defn read-records [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [out []]
      (let [v (try (edn/read {:eof ::eof :default (fn [_ v] v)} r) (catch Exception _ ::bad))]
        (cond (= v ::eof) out (= v ::bad) (recur out) :else (recur (conj out v)))))))

(defn corpus-replay []
  (let [entries (vec (mapcat (fn [f]
                               (mapcat (fn [[ri r]]
                                         (map-indexed (fn [mi m] {:file (.getName f) :record-index ri
                                                                  :micro-index mi :entry m})
                                                      (:micro-step-trace r)))
                                       (map-indexed vector (read-records f))))
                             (trace-files)))
        anneal-results (mapv (fn [{:keys [entry] :as x}]
                               (assoc (select-keys x [:file :record-index :micro-index])
                                      :deviation (dev (:anneal-factor entry)
                                                      (max 0.0 (- 1.0 (/ (double (:step entry)) 3.0)))))) entries)
        with-driver (filter #(contains? (:entry %) :aggregated-signed-error) entries)
        weight-results (mapv (fn [{:keys [entry] :as x}]
                               (assoc (select-keys x [:file :record-index :micro-index])
                                      :deviation (dev (:event-weight entry)
                                                      (* (min 1.0 (Math/abs (double (:aggregated-signed-error entry))))
                                                         (:anneal-factor entry) 0.1)))) with-driver)
        bad-a (filter #(pos? (:deviation %)) anneal-results)
        bad-w (filter #(pos? (:deviation %)) weight-results)
        ;; The identities above say the arithmetic is right. They say nothing
        ;; about what it is arithmetic ON. base-weight is (min 1 |driver|)
        ;; (war_machine.clj:6161), so on an entry with |driver| >= 1 the
        ;; magnitude of the surprise is DISCARDED and only its sign reaches
        ;; belief. Counting those separates "the update moved by a graded
        ;; amount" from "the update moved by its sign", which the deviation
        ;; totals cannot distinguish -- both are deviation 0.0.
        mags (mapv (fn [{:keys [entry] :as x}]
                     (assoc (select-keys x [:file :record-index :micro-index])
                            :magnitude (Math/abs (double (:aggregated-signed-error entry)))))
                   with-driver)
        saturated (filterv #(>= (:magnitude %) 1.0) mags)
        unsaturated (filterv #(< (:magnitude %) 1.0) mags)
        sorted-mags (vec (sort (map :magnitude mags)))
        by-file (mapv (fn [[f xs]]
                        (let [ms (map :magnitude xs)]
                          {:file f :entries (count xs)
                           :min-magnitude (reduce min ms) :max-magnitude (reduce max ms)
                           :saturated (count (filter #(>= % 1.0) ms))}))
                      (sort-by key (group-by :file mags)))
        ;; The keyless entries. war_machine.clj:6200-6210 writes
        ;; :aggregated-signed-error present-only and, when the aggregator
        ;; reports :unknown, writes :aggregated-driver-unknown beside it. So
        ;; "no driver key" does NOT by itself mean the AC2 unknown path fired,
        ;; and this reports which of the two an absence actually is.
        without-driver (remove #(contains? (:entry %) :aggregated-signed-error) entries)
        unknown-marked (filter #(contains? (:entry %) :aggregated-driver-unknown) entries)]
    {:file-count (count (trace-files)) :entry-count (count entries)
     :anneal-comparisons (count anneal-results)
     :anneal-max-deviation (reduce max 0.0 (map :deviation anneal-results))
     :event-weight-comparisons (count weight-results)
     :event-weight-max-deviation (reduce max 0.0 (map :deviation weight-results))
     :missing-aggregated-signed-error (- (count entries) (count with-driver))
     :distinct-steps (vec (sort (set (map #(get-in % [:entry :step]) entries))))
     :anneal-mismatches (vec bad-a) :event-weight-mismatches (vec bad-w)
     :magnitude-saturation
     {:reads "scripts/futon2/report/war_machine.clj:6160 (base-weight (min 1.0 aggregated-magnitude))"
      :comparisons (count mags)
      :saturated (count saturated) :unsaturated (count unsaturated)
      :max-magnitude (reduce max 0.0 (map :magnitude mags))
      :min-magnitude (if (seq mags) (reduce min (map :magnitude mags)) nil)
      :median-magnitude (when (seq sorted-mags) (nth sorted-mags (quot (count sorted-mags) 2)))
      :last-saturated-file (:file (last saturated))
      :first-unsaturated-file (:file (first unsaturated))
      :saturated-file-count (count (distinct (map :file saturated)))
      :unsaturated-file-count (count (distinct (map :file unsaturated)))
      :by-file by-file}
     :absent-driver-key
     {:reads "scripts/futon2/report/war_machine.clj:6198-6211 (the rule stated in the comment, the present-only write, and its :aggregated-driver-unknown companion)"
      :entries-without-the-key (count without-driver)
      :of-those-carrying-aggregated-driver-unknown
      (count (filter #(contains? (:entry %) :aggregated-driver-unknown) without-driver))
      :entries-carrying-aggregated-driver-unknown (count unknown-marked)
      :the-keyless-entries
      (mapv (fn [{:keys [file record-index micro-index entry]}]
              {:file file :record-index record-index :micro-index micro-index
               :step (:step entry) :event-weight (:event-weight entry)
               :events-applied (:events-applied entry)})
            without-driver)}}))

(def plants
  [{:id :unweighted-arithmetic-mean-driver
    :node (assoc shipped :driver (fn [errors]
                                   (if (every? #(number? (:weighted-error %)) (vals errors))
                                     (if (seq errors)
                                       {:status :present :driver (/ (reduce + (map :weighted-error (vals errors)))
                                                                    (count errors))}
                                       ((:driver shipped) errors))
                                     ((:driver shipped) errors))))}
   {:id :driver-drops-signs
    :node (assoc shipped :driver (fn [errors]
                                   (if (and (seq errors) (every? #(and (number? (:weighted-error %))
                                                                      (number? (:precision %))) (vals errors))
                                            (pos? (reduce + (map :precision (vals errors)))))
                                     {:status :present :driver (/ (reduce + (map :weighted-error (vals errors)))
                                                                  (reduce + (map :precision (vals errors))))}
                                     ((:driver shipped) errors))))}
   {:id :driver-divides-by-channel-count
    :node (assoc shipped :driver (fn [errors]
                                   (if (and (seq errors) (every? #(number? (:weighted-error %)) (vals errors)))
                                     {:status :present
                                      :driver (/ (reduce + (map (fn [[ch m]] (* (get signs ch) (:weighted-error m))) errors))
                                                 (count errors))}
                                     ((:driver shipped) errors))))}
   {:id :empty-driver-is-zero
    :node (assoc shipped :driver (fn [errors] (if (empty? errors) {:status :present :driver 0.0}
                                                  ((:driver shipped) errors))))}
   {:id :driver-imputes-refused-third-status
    :node (assoc shipped :driver (fn [errors] (if (= #{:annotation-health} (set (keys errors)))
                                                  {:status :refused} ((:driver shipped) errors))))}
   {:id :filter-kappa-is-weight
    :node (assoc shipped :filter (fn [prior {:keys [type weight]}]
                                   (let [pred (ref-predict prior) a (ref-a)]
                                     (ref-normalise (into {} (map (fn [[s p]] [s (* p (Math/pow (get-in a [type s]) weight))]) pred))))))}
   ;; Renamed by the review: this plant does NOT skip the predict step, it
   ;; SUBSTITUTES a non-identity one. A genuine skip is not discriminable
   ;; here -- see `predict-step-discrimination` below, which measures why.
   {:id :filter-substitutes-a-non-identity-prediction
    :node (assoc shipped :filter (fn [prior event]
                                   (let [rotated (zipmap statuses (map prior (rest (cycle statuses))))]
                                     (ref-filter rotated event))))}
   {:id :anneal-one-minus-step-over-two
    :node (assoc shipped :attribution (fn []
                                       (let [r (ref-attribution)]
                                         (assoc r :event-weight (* (min 1.0 (Math/abs (get-in carriers [:attribution :driver])))
                                                                   (max 0.0 (- 1.0 (/ (get-in carriers [:attribution :step]) 2.0))) 0.1)))))}])

(def checks (component-checks shipped))
(def replay (corpus-replay))
(def corpus-check {:id :production-micro-step-replay :result :pass
                   :finding-policy "Corpus mismatches are retained as findings and do not tune the fixture gate."
                   :measurements replay})
(def all-checks (conj checks corpus-check))
(def plant-results
  (mapv (fn [{:keys [id node]}]
          (let [failed (mapv :id (filter #(= :fail (:result %)) (component-checks node)))]
            {:id id :result (if (seq failed) :caught :escaped) :caught-by failed})) plants))
(def all-caught (every? #(= :caught (:result %)) plant-results))
(def all-pass (every? #(= :pass (:result %)) all-checks))

;; RECORDED BY THE REVIEW, NOT A GATE. The plant above substitutes a
;; non-identity prediction step; a plant that GENUINELY skipped the predict
;; step would escape, because transition-model-v1 is the 7x7 identity
;; (belief.clj:214-226) and B x q is then a no-op up to floating-point noise.
;; Measured here rather than asserted, so the plant table is not read as
;; claiming a discrimination this harness does not have.
(def predict-step-discrimination
  (let [{:keys [prior event]} (last (:filter-fixtures carriers))
        shipped-out ((:filter shipped) prior event)
        skipped-out (#'belief/update-step (ref-a) (:type event) (:weight event) (ref-predict prior))
        d (max-dev shipped-out skipped-out)]
    {:what "a filter that omits the predict step entirely, against the shipped filter"
     :why "belief/transition-model-v1 is the 7x7 identity (src/futon2/aif/belief.clj:214-226), so the prediction step carries no dynamics to detect"
     :fixture (:id (last (:filter-fixtures carriers)))
     :max-deviation d
     :harness-tolerance 1.0e-15
     :would-be-caught (>= d 1.0e-15)}))

(def receipt
  {:harness :v7-r3-node-sim :row :V7 :slice 5 :node :R3 :stage "BELIEF UPDATE"
   :stage-at "p4ng/empirics-futon/control-stages.edn:18"
   :carriers "holes/labs/wm-contract/sim/R3-carriers.edn"
   :reference-independence "All driver, model, filter, annealing, health and attribution reference values are constructed only from R3-carriers.edn. futon2.aif.belief supplies only node-under-test values; war_machine arithmetic is transcribed only on the measured side."
   :declaration-sites (:declaration-sites carriers)
   :checks all-checks :verdict (if all-pass :pass :fail)
   :negative-controls {:plants plant-results :n (count plant-results) :all-caught all-caught}
   :discrimination-limit predict-step-discrimination
   :corpus {:root "data/wm-trace" :read-only true :file-count (:file-count replay)
            :micro-step-entry-count (:entry-count replay)}
   :not-done ["No production source, registry, ledger, documentation, or p4ng file changed."
              "No live tick and no run lock; data/wm-trace was read only."
              "Corpus mismatches are reported rather than made into a failing or tuned fixture check."]})

(def out (io/file lab "runs/V7-R3-node-sim/00-r3.edn"))
(.mkdirs (.getParentFile out))
(with-open [w (io/writer out)] (binding [*out* w] (pp/pprint receipt)))
(doseq [c all-checks] (println (format "  %-58s %s" (name (:id c)) (name (:result c)))))
(doseq [p plant-results] (println "  planted" (:id p) (:result p) (:caught-by p)))
(println "  verdict" (:verdict receipt) "all-caught" all-caught "receipt" (str out))
(System/exit (if (and all-pass all-caught) 0 1))
