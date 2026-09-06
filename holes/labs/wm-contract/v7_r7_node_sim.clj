(ns v7-r7-node-sim
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.set :as set]
            [futon2.aif.precision :as precision]))

(def lab (io/file "holes/labs/wm-contract"))
(def carriers (edn/read-string (slurp (io/file lab "sim/R7-carriers.edn"))))
(def defaults (:defaults carriers))

(defn variance-reference [history opts]
  (let [{:keys [prior-variance prior-strength]} (merge defaults opts)]
    (/ (+ (* prior-strength prior-variance) (reduce + (map #(* % %) history)))
       (+ prior-strength (count history)))))

(defn sample-variance [xs]
  (if (< (count xs) 2) 0.0
      (let [mean (/ (reduce + xs) (count xs))]
        (/ (reduce + (map #(let [d (- % mean)] (* d d)) xs)) (dec (count xs))))))

(defn reference-channel [previous new-error opts]
  (let [{:keys [window-size min-variance floor cap]} (merge defaults opts)
        appended (conj (vec (:error-history previous [])) (double new-error))
        history (vec (take-last window-size appended))
        variance (variance-reference history opts)
        component (/ 1.0 (max variance min-variance))]
    {:history history :variance variance :registry component
     :precision (min cap (max floor component))}))

(def shipped
  {:update precision/update-precision-state
   :weighted precision/weighted-error})

(defn update-history [node channel history opts]
  (reduce (fn [state e]
            ((:update node) state {channel {:error e :observed 0.75}} opts))
          {} history))

(defn component-checks [node]
  (let [unclamped
        (mapv (fn [{:keys [id channel history]}]
                (let [actual (get (update-history node channel history {}) channel)
                      ref (reference-channel {:error-history (butlast history)} (last history) {})]
                  {:id id :actual (:precision actual) :registry (:registry ref)
                   :deviation (Math/abs (- (double (:precision actual)) (double (:registry ref))))}))
              (:unclamped-fixtures carriers))
        floor-f (:floor-fixture carriers)
        floor-actual (get (update-history node (:channel floor-f) (:history floor-f) {}) (:channel floor-f))
        floor-ref (reference-channel {:error-history []} (first (:history floor-f)) {})
        constant (:constant-fixture carriers)
        constant-actual (get (update-history node (:channel constant) (:history constant) {}) (:channel constant))
        regularized (variance-reference (:history constant) {})
        sample (sample-variance (:history constant))
        extremal-v (variance-reference (repeat 20 0.0) {})
        extremal-p (/ 1.0 extremal-v)
        wide (:wide-window-fixture carriers)
        wide-v (variance-reference (repeat (:history-count wide) (:error wide)) {:window-size (:window-size wide)})
        wide-registry (/ 1.0 (max wide-v (:min-variance defaults)))
        window (:window-fixture carriers)
        window-state {::window {:precision 1.0 :error-history (:prior-history window)}}
        window-result (get ((:update node) window-state
                            {::window {:error (:new-error window) :observed 0.5}}
                            {:window-size (:window-size window)}) ::window)
        untouched-state {:untouched {:precision 7.0 :error-history [0.25 0.5] :marker :identical}}
        untouched ((:update node) untouched-state {} {})
        first-sight (get ((:update node) {} {:new {:error 0.5 :observed 0.75}} {}) :new)
        malformed (get ((:update node) {} {:m {:producer-contract :prediction-error/v1 :observed 0.5}} {}) :m)
        legacy (get ((:update node) {} {:l {:observed 0.5}} {}) :l)
        need (:need-fixture carriers)
        separate (get ((:update node) {} {(:channel need) {:error (:error need) :observed (:observed need)}}
                       {:salience-mode :separate}) (:channel need))
        summed (get ((:update node) {} {(:channel need) {:error (:error need) :observed (:observed need)}}
                     {:salience-mode :summed}) (:channel need))
        adaptive-state {:annotation-health {:precision 4.0}}
        incoming {:error 0.5 :precision 8.0 :weighted-error 4.0}
        weighted ((:weighted node) adaptive-state :annotation-health incoming)
        missing-weighted ((:weighted node) adaptive-state :annotation-health {:error 0.5})]
    [{:id :registry-form-holds-only-in-the-unclamped-band
      :result (if (and (every? #(zero? (:deviation %)) unclamped)
                       (= 0.1 (:precision floor-actual))
                       (not= (:registry floor-ref) (:precision floor-actual))) :pass :fail)
      :unclamped-comparisons (count unclamped) :unclamped-max-deviation (reduce max 0.0 (map :deviation unclamped))
      :floor-history (:history floor-f) :registry-value (:registry floor-ref) :shipped-value (:precision floor-actual)}
     {:id :variance-is-regularized-not-sample
      :result (if (and (= 1.0 regularized) (= 0.0 sample) (= 1.0 (:precision constant-actual))) :pass :fail)
      :history (:history constant) :regularized-variance regularized :sample-variance sample
      :shipped-precision (:precision constant-actual)}
     {:id :both-declared-guards-are-inert-under-the-shipped-defaults
      :result (if (and (= (/ 1.0 21.0) extremal-v) (= 21.0 extremal-p)
                       (> extremal-v (:min-variance defaults)) (< extremal-p (:cap defaults))
                       (< wide-v (:min-variance defaults)) (= 100.0 wide-registry)) :pass :fail)
      :default-v-min extremal-v :default-pi-max extremal-p
      :min-variance (:min-variance defaults) :cap (:cap defaults)
      :wide-window (:window-size wide) :wide-v wide-v :wide-registry-value wide-registry}
     {:id :floor-is-the-only-reachable-bound
      :result (if (and (= 0.1 (:precision floor-actual))
                       (> (:variance floor-ref) (:min-variance defaults))
                       (< (:registry floor-ref) (:floor defaults))) :pass :fail)
      :variance (:variance floor-ref) :unbounded-precision (:registry floor-ref)
      :floor (:floor defaults) :min-variance-bound? false :cap-bound? false}
     {:id :window-is-bounded-and-keeps-the-most-recent
      :result (if (= (:expected window) (:error-history window-result)) :pass :fail)
      :window-size (:window-size window) :actual (:error-history window-result) :expected (:expected window)}
     {:id :untouched-channels-pass-through-unchanged
      :result (if (= untouched-state untouched) :pass :fail) :whole-map-equal (= untouched-state untouched)}
     {:id :a-first-sight-channel-does-not-come-back-at-the-prior
      :result (if (and (= 1.0 precision/default-initial-precision)
                       (not= 1.0 (:precision first-sight))) :pass :fail)
      :prior-location "src/futon2/aif/precision.clj:44,176-178"
      :initial-prior 1.0 :returned-precision (:precision first-sight)
      :returned-history (:error-history first-sight)}
     {:id :absent-error-fields-are-coerced-to-zero-and-typed
      :result (if (and (= 0.0 (last (:error-history malformed)))
                       (= :malformed (get-in malformed [:input-status :error :reason]))
                       (= 0.0 (last (:error-history legacy)))
                       (= :legacy-era (get-in legacy [:input-status :error :reason]))) :pass :fail)
      :malformed malformed :legacy legacy}
     {:id :need-term-is-emitted-and-never-summed-under-the-default
      :result (if (and (pos? (:need-component separate)) (= (:need-component separate) (:salience separate))
                       (= (:precision separate) (:variance-component separate))
                       (not= (:precision separate) (:precision summed))) :pass :fail)
      :production-mode :separate :production-mode-at "scripts/futon2/report/war_machine.clj:849-855,6132-6134"
      :separate separate :summed summed}
     {:id :weighted-error-overwrites-the-per-call-precision-and-preserves-it
      :result (if (and (= 4.0 (:precision weighted)) (= 2.0 (:weighted-error weighted))
                       (= 8.0 (:per-call-precision weighted))
                       (= 4.0 (:precision missing-weighted)) (= 2.0 (:weighted-error missing-weighted))
                       (nil? (:per-call-precision missing-weighted))) :pass :fail)
      :adaptive-reference 4.0 :weighted-reference 2.0
      :incoming-present weighted :incoming-absent missing-weighted}]))

(defn trace-files []
  (->> (.listFiles (io/file "data/wm-trace"))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
       (sort-by #(.getName %))))
(defn read-records [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [out []]
      (let [v (try (edn/read {:eof ::eof :default (fn [_ v] v)} r) (catch Exception _ ::bad))]
        (cond (= v ::eof) out (= v ::bad) (recur out) :else (recur (conj out v)))))))
(def records (vec (mapcat (fn [f] (map-indexed (fn [i r] {:file (.getName f) :index i :record r}) (read-records f))) (trace-files))))

(defn corpus-measurements []
  (let [with-state (filter #(map? (get-in % [:record :precision-state])) records)
        states (vec (mapcat (fn [{:keys [file index record]}]
                              (map (fn [[ch s]] {:file file :index index :channel ch :state s}) (:precision-state record))) with-state))
        channel-sets (frequencies (map #(vec (sort (keys (get-in % [:record :precision-state])))) with-state))
        ps (vec (keep #(get-in % [:state :precision]) states))
        lengths (vec (map #(count (get-in % [:state :error-history] [])) states))
        identity (mapv (fn [{:keys [state] :as x}]
                         (let [h (:error-history state) ref (:precision (reference-channel {:error-history (butlast h)} (last h) {}))]
                           (assoc (select-keys x [:file :index :channel]) :actual (:precision state) :reference ref
                                  :deviation (Math/abs (- (double (:precision state)) ref)))))
                       (filter #(seq (get-in % [:state :error-history])) states))
        mismatches (filter #(> (:deviation %) 1.0e-12) identity)
        mismatch-groups (->> mismatches (group-by (juxt :file :channel))
                             (map (fn [[[f ch] xs]] {:file f :channel ch :count (count xs)
                                                     :max-deviation (apply max (map :deviation xs))}))
                             (sort-by (juxt :file (comp str :channel))) vec)
        mismatch-shapes
        (frequencies
         (map (fn [{:keys [file index channel]}]
                (let [s (get-in (first (filter #(and (= file (:file %)) (= index (:index %))) with-state))
                                [:record :precision-state channel])]
                  {:has-salience (contains? s :salience)
                   :precision-equals-variance-component (= (:precision s) (:variance-component s))
                   :need-nonzero (pos? (double (:need-component s 0.0)))})) mismatches))
        consecutive (partition 2 1 with-state)
        carry-results
        (vec (mapcat (fn [[a b]]
                       (let [pa (get-in a [:record :precision-state]) pb (get-in b [:record :precision-state])]
                         (for [ch (sort (set/intersection (set (keys pa)) (set (keys pb))))
                               :let [old (get-in pa [ch :error-history] []) new (get-in pb [ch :error-history] [])
                                     expected (vec (take-last 20 (conj (vec old) (last new))))
                                     agrees (= expected new)]]
                           {:file (:file b) :channel ch :agrees agrees
                            :micro-steps (count (get-in b [:record :micro-step-trace]))
                            :old-length (count old) :new-length (count new)}))) consecutive))
        carry-bad (remove :agrees carry-results)
        carry-groups (->> carry-bad (group-by (juxt :file :channel :old-length :new-length))
                          (map (fn [[[f ch ol nl] xs]] {:file f :channel ch :old-length ol :new-length nl :count (count xs)}))
                          (sort-by (juxt :file (comp str :channel) :old-length :new-length)) vec)
        errors (vec (mapcat (fn [{:keys [file index record]}]
                              (map (fn [[ch e]] {:file file :index index :channel ch :entry e}) (:prediction-errors record))) records))
        both (filter #(and (number? (get-in % [:entry :precision])) (number? (get-in % [:entry :per-call-precision]))) errors)
        ratios (keep #(let [d (double (get-in % [:entry :per-call-precision]))]
                        (when-not (zero? d) (/ (double (get-in % [:entry :precision])) d))) both)
        nonzero-need (filter #(or (pos? (double (get-in % [:state :salience] 0.0)))
                                  (pos? (double (get-in % [:state :need-component] 0.0)))) states)]
    {:M1 {:records-total (count records) :records-with-precision-state (count with-state)
          :channel-set-distribution (into (sorted-map-by #(compare (pr-str %1) (pr-str %2))) channel-sets)
          :declared-eight (:likelihood-channels carriers)}
     :M2 {:channel-states (count ps) :min (when (seq ps) (apply min ps)) :max (when (seq ps) (apply max ps))
          :at-floor (count (filter #(= 0.1 %) ps)) :at-100 (count (filter #(= 100.0 %) ps))
          :above-21 (count (filter #(> % 21.0) ps))}
     :M3 {:channel-states (count lengths) :length-distribution (into (sorted-map) (frequencies lengths))
          :maximum (reduce max 0 lengths) :filled-window (count (filter #(= 20 %) lengths))}
     :M4 {:comparisons (count identity) :tolerance 1.0e-12
          :max-deviation (reduce max 0.0 (map :deviation identity)) :mismatch-count (count mismatches)
          :agreement-count (- (count identity) (count mismatches))
          :mismatch-first-file (:file (first mismatches)) :mismatch-last-file (:file (last mismatches))
          :mismatch-shapes mismatch-shapes
          :mismatches-by-file-channel mismatch-groups}
     :M5 {:record-pairs (count consecutive) :channel-comparisons (count carry-results)
          :agreements (count (filter :agrees carry-results)) :disagreements (count carry-bad)
          :disagreement-first-file (:file (first carry-bad)) :disagreement-last-file (:file (last carry-bad))
          :disagreements-by-micro-step-count (into (sorted-map) (frequencies (map :micro-steps carry-bad)))
          :disagreements-by-file-channel-and-length carry-groups
          :criterion "new history equals previous history plus exactly one appended error, bounded to the last 20"}
     :M6 {:prediction-error-entries (count errors) :both-precisions (count both)
          :differing (count (filter #(not= (get-in % [:entry :precision]) (get-in % [:entry :per-call-precision])) both))
          :ratio-min (when (seq ratios) (apply min ratios)) :ratio-max (when (seq ratios) (apply max ratios))}
     :M7 {:channel-states (count states) :nonzero-salience-or-need (count nonzero-need)
          :by-channel (into (sorted-map) (frequencies (map :channel nonzero-need)))}}))

(def corpus (corpus-measurements))
(def measurement-checks
  (mapv (fn [[id m]] {:id (keyword (str "production-" (name id))) :result :pass :measurement m}) corpus))

(def plants
  [{:id :sample-variance-instead-of-regularized
    :node (assoc shipped :update (fn [prev errors opts]
                                   (let [r ((:update shipped) prev errors opts)]
                                     (reduce (fn [x [ch _]] (let [h (get-in x [ch :error-history]) v (sample-variance h)]
                                                             (assoc-in x [ch :precision] (/ 1.0 (max v 0.01))))) r errors))))}
   {:id :floor-dropped :node (assoc shipped :update (fn [prev errors opts] ((:update shipped) prev errors (assoc opts :floor 0.0))))}
   {:id :window-keeps-first :node (assoc shipped :update (fn [prev errors opts]
                                                           (let [r ((:update shipped) prev errors opts) n (:window-size opts 20)]
                                                             (reduce (fn [x [ch _]] (assoc-in x [ch :error-history]
                                                                                           (vec (take n (conj (get-in prev [ch :error-history] [])
                                                                                                             (get-in errors [ch :error])))))) r errors))))}
   {:id :need-summed-under-separate :node (assoc shipped :update (fn [prev errors opts] ((:update shipped) prev errors (assoc opts :salience-mode :summed))))}
   {:id :untouched-channels-dropped :node (assoc shipped :update (fn [_ errors opts] ((:update shipped) {} errors opts)))}
   {:id :absent-error-coerced-to-one :node (assoc shipped :update (fn [prev errors opts]
                                                                   ((:update shipped) prev (into {} (map (fn [[ch e]] [ch (if (contains? e :error) e (assoc e :error 1.0))]) errors)) opts)))}
   {:id :weighted-error-uses-per-call
    :node (assoc shipped :weighted (fn [s ch e]
                                     (if (number? (:precision e))
                                       (assoc e :weighted-error (* (:error e) (:precision e)))
                                       ((:weighted shipped) s ch e))))}
   {:id :per-call-precision-not-preserved
    :node (assoc shipped :weighted (fn [s ch e] (dissoc ((:weighted shipped) s ch e) :per-call-precision)))}
   {:id :precision-is-variance :node (assoc shipped :update (fn [prev errors opts]
                                                              (let [r ((:update shipped) prev errors opts)]
                                                                (reduce (fn [x [ch _]] (assoc-in x [ch :precision]
                                                                                              (variance-reference (get-in x [ch :error-history]) opts))) r errors))))}])

(def fixture-checks (component-checks shipped))
(def checks (vec (concat fixture-checks measurement-checks)))
(def plant-results
  (mapv (fn [{:keys [id node]}]
          (let [failed (mapv :id (filter #(= :fail (:result %)) (component-checks node)))]
            {:id id :result (if (seq failed) :caught :escaped) :caught-by failed :would-be-caught (boolean (seq failed))})) plants))
(def all-caught (every? #(= :caught (:result %)) plant-results))
(def all-pass (every? #(= :pass (:result %)) checks))

(def receipt
  {:harness :v7-r7-node-sim :row :V7 :slice 7 :node :R7 :stage "BELIEVE"
   :stage-at "p4ng/empirics-futon/control-stages.edn:20"
   :carriers "holes/labs/wm-contract/sim/R7-carriers.edn"
   :reference-independence "Variance, bounds, histories and weighted-error references are computed only from R7-carriers.edn and the registry formula. futon2.aif.precision supplies only node-under-test results."
   :declaration-sites (:declaration-sites carriers) :checks checks :verdict (if all-pass :pass :fail)
   :negative-controls {:plants plant-results :n (count plant-results) :all-caught all-caught}
   :corpus {:root "data/wm-trace" :read-only true :file-count (count (trace-files)) :record-count (count records)}
   :not-done ["No live tick or run lock; data/wm-trace was read only."
              "No production, registry, ledger, document, p4ng, or data file changed."
              "Corpus differences are characterized measurements, not fixture-gate failures."]})
(def out (io/file lab "runs/V7-R7-node-sim/00-r7.edn"))
(.mkdirs (.getParentFile out))
(with-open [w (io/writer out)] (binding [*out* w] (pp/pprint receipt)))
(doseq [c checks] (println (format "  %-64s %s" (name (:id c)) (name (:result c)))))
(doseq [p plant-results] (println "  planted" (:id p) (:result p) (:caught-by p)))
(println "  verdict" (:verdict receipt) "all-caught" all-caught "receipt" (str out))
(System/exit (if (and all-pass all-caught) 0 1))
