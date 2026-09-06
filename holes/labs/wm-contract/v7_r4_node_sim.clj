(ns v7-r4-node-sim
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.machine-q :as machine-q]))

(def lab (io/file "holes/labs/wm-contract"))
(def carriers (edn/read-string (slurp (io/file lab "sim/R4-carriers.edn"))))
(def tolerance 1.0e-12)

(defn row-sum [row] (reduce + (vals row)))
(defn model-valid? [c]
  (and (= (set (:states c)) (set (keys (:A c))))
       (every? #(= (set (:outcomes c)) (set (keys %))) (vals (:A c)))
       (every? #(< (Math/abs (- 1.0 (row-sum %))) tolerance) (vals (:A c)))
       (every? (fn [[_ rows]]
                 (and (= (set (:states c)) (set (keys rows)))
                      (every? #(< (Math/abs (- 1.0 (row-sum %))) tolerance) (vals rows)))) (:B c))))

(defn roll [c]
  (reduce (fn [q action]
            (into {} (for [next (:states c)]
                       [next (reduce + (for [s (:states c)]
                                         (* (get q s) (get-in c [:B action s next]))))])))
          (:mu c) (take (:T c) (:pi c))))
(defn outcome-row [c]
  (let [q (roll c)]
    (into {} (for [o (:outcomes c)]
               [o (reduce + (for [s (:states c)] (* (get-in c [:A s o]) (get q s))))]))))

(def reference {:source :carriers :state-row (roll carriers) :outcome-row (outcome-row carriers)})
(def production (:production carriers))
(def single-a (fm/predict (:state-a production) (:action production)))
(def single-b (fm/predict (:state-b production) (:action production)))
(def single-c (fm/predict (:state-c production) (:action production)))
(def multi-a (fm/predict-multi-horizon (:state-a production) (:action production) (:T carriers)))
(def multi-b (fm/predict-multi-horizon (:state-b production) (:action production) (:T carriers)))
(def multi-c (fm/predict-multi-horizon (:state-c production) (:action production) (:T carriers)))

;; REVIEW FIX (wm-build-work): the delivered harness exercised the node only
;; under {:type :no-op}, whose predict-effects arm returns an empty observation
;; delta (forward_model.clj:65), so belief-independence of the OBSERVATION held
;; for the trivial reason that nothing moved it. These runs use an arm that does
;; move it (forward_model.clj:129-141), so what is measured is that the DELTA
;; itself is belief-independent.
(def acting (:acting-action production))
(def acting-a (fm/predict (:state-a production) acting))
(def acting-b (fm/predict (:state-b production) acting))
(def acting-multi-a (fm/predict-multi-horizon (:state-a production) acting (:T carriers)))
(def acting-multi-b (fm/predict-multi-horizon (:state-b production) acting (:T carriers)))

(defn iterate-single-step
  "The K-step observation the node would produce if `predict` were simply
   re-applied to its own predicted observation K times, with the belief held at
   its INITIAL value. Compared against `predict-multi-horizon`'s final-state
   observation, this measures whether the roll-forward iterates any DYNAMICS or
   merely re-applies one action delta. Derived here from the single-step node
   only -- it is not read from `predict-multi-horizon`."
  [state action k]
  (loop [i 0 obs (:observation state)]
    (if (>= i k) obs
        (recur (inc i)
               (get-in (fm/predict (assoc state :observation obs) action)
                       [:next-observation :mean])))))
(def iterated-obs (iterate-single-step (:state-a production) acting (:T carriers)))

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
(defn tree-has-key? [x k]
  (cond (map? x) (or (contains? x k) (some #(tree-has-key? % k) (vals x)))
        (sequential? x) (some #(tree-has-key? % k) x)
        :else false))
(defn tree-values [x k]
  (cond (map? x) (concat (when (contains? x k) [(get x k)])
                         (mapcat #(tree-values % k) (vals x)))
        (sequential? x) (mapcat #(tree-values % k) x)
        :else []))

(defn source-files [root]
  (->> (file-seq (io/file root)) (filter #(.isFile %))
       (filter #(re-find #"\.(clj|cljc|bb)$" (.getName %)))
       (sort-by #(.getPath %))))
(defn search-hits [pattern roots]
  (vec (mapcat (fn [f]
                 (keep-indexed (fn [i line]
                                 (when (re-find pattern line)
                                   (str (.getPath f) ":" (inc i) ":" (str/trim line))))
                               (str/split-lines (slurp f))))
               (mapcat source-files roots))))

(def horizon-hits (search-hits #"horizon-steps|predict-multi-horizon" ["src" "scripts" "checks"]))
(def kernel-hits (search-hits #"predict-effects|forward-model/predict|fm/predict" ["src" "scripts" "checks"]))
(def horizon-field-records (filter #(tree-has-key? (:record %) :horizon-steps) records))
(def horizon-records
  (filter #(or (some some? (tree-values (:record %) :horizon-steps))
               (some seq (filter sequential? (tree-values (:record %) :trajectory)))) records))
(def horizon-values
  (->> records (mapcat #(tree-values (:record %) :horizon-steps)) frequencies
       (sort-by (comp pr-str key)) (into (sorted-map-by #(compare (pr-str %1) (pr-str %2))))))
(def machine-q-records (filter #(tree-has-key? (:record %) :machine-q) records))

(def machine-q-refusal
  (try (machine-q/predictive-outcome-row! single-a (:outcomes carriers)) nil
       (catch clojure.lang.ExceptionInfo e (ex-data e))))

(def alphabet-intersection
  (vec (sort (set/intersection (set (:outcomes carriers))
                               (set (keys (get-in single-a [:next-observation :mean])))))))
(def shipped-return-is-categorical?
  "Computed, not asserted: the shipped return can be read as the declared
   Q(o|pi) only if its observation map is a mass function over the DECLARED
   outcome alphabet. Both conjuncts are measured."
  (and (= (set (:outcomes carriers))
          (set (keys (get-in single-a [:next-observation :mean]))))
       (< (Math/abs (- 1.0 (row-sum (get-in single-a [:next-observation :mean])))) tolerance)))

(def enact-live-range
  "The live enactment path named by the carrier's :live-dynamics site."
  {:file "src/futon2/aif/enact.clj" :from 250 :to 336})
(defn hits-in-live-range
  "REVIEW FIX: the delivered harness asserted :live-path-calls-predictive-kernel
   false as a literal beside a computed hit list. This derives it from the hit
   list, so the claim fails if a predictive-kernel call ever appears in the live
   range."
  [hits]
  (vec (filter (fn [h]
                 (let [[path line] (str/split h #":" 3)]
                   (and (= path (:file enact-live-range))
                        (<= (:from enact-live-range) (parse-long line) (:to enact-live-range)))))
               hits)))
(def live-range-kernel-hits (hits-in-live-range kernel-hits))

(def horizon-supplied-in-source
  "Source lines that SUPPLY a horizon depth (as opposed to naming the symbol)."
  (vec (filter #(re-find #":horizon-steps" %) horizon-hits)))
(def recorded-horizon-depths (vec (sort (keep identity (keys horizon-values)))))

(def checks
  [{:id :declared-equation-produces-normalised-categorical
    :result (if (and (model-valid? carriers)
                     (< (Math/abs (- 1.0 (row-sum (:state-row reference)))) tolerance)
                     (< (Math/abs (- 1.0 (row-sum (:outcome-row reference)))) tolerance)) :pass :fail)
    :reference-source (:source reference) :state-mass (row-sum (:state-row reference))
    :outcome-mass (row-sum (:outcome-row reference)) :state-row (:state-row reference)
    :outcome-row (:outcome-row reference) :state-count (count (:states carriers))
    :outcome-count (count (:outcomes carriers)) :tolerance tolerance}
   {:id :shipped-return-is-a-channel-gaussian-not-declared-q
    :result (if shipped-return-is-categorical? :fail :pass)
    :return-keys (vec (sort (keys single-a)))
    :next-observation-keys (vec (sort (keys (:next-observation single-a))))
    :mean-channel-count (count (get-in single-a [:next-observation :mean]))
    :mean-channels (vec (sort (keys (get-in single-a [:next-observation :mean]))))
    :reference-outcomes (:outcomes carriers)
    :alphabet-intersection alphabet-intersection
    :shipped-observation-mass (row-sum (get-in single-a [:next-observation :mean]))
    :multi-return-keys (vec (sort (keys multi-a)))
    :trajectory-count (count (:trajectory multi-a))
    :final-state-keys (vec (sort (keys (:final-state multi-a))))
    :can-read-as-reference shipped-return-is-categorical?
    :reason (if (empty? alphabet-intersection)
              :disjoint-alphabets
              :shared-alphabet-but-not-a-mass-function)}
   {:id :observation-prediction-does-not-read-belief
    :result (if (and (= (:next-observation single-a) (:next-observation single-b))
                     (not= (:next-belief single-a) (:next-belief single-b))
                     (not= (:next-observation single-a) (:next-observation single-c))
                     (= (mapv :next-observation (:trajectory multi-a))
                        (mapv :next-observation (:trajectory multi-b)))
                     (not= (:final-state multi-a) (:final-state multi-b))
                     (not= (:final-state multi-a) (:final-state multi-c))) :pass :fail)
    :single-observation-equal-under-belief-change (= (:next-observation single-a) (:next-observation single-b))
    :single-belief-equal-under-belief-change (= (:next-belief single-a) (:next-belief single-b))
    :single-observation-equal-under-observation-change (= (:next-observation single-a) (:next-observation single-c))
    :multi-observation-trajectories-equal-under-belief-change
    (= (mapv :next-observation (:trajectory multi-a)) (mapv :next-observation (:trajectory multi-b)))
    :multi-final-states-equal-under-belief-change (= (:final-state multi-a) (:final-state multi-b))
    :acting-action acting
    :acting-observation-delta-nonempty
    (not= (get-in acting-a [:next-observation :mean]) (:observation (:state-a production)))
    :acting-observation-equal-under-belief-change
    (= (:next-observation acting-a) (:next-observation acting-b))
    :acting-multi-observation-equal-under-belief-change
    (= (mapv :next-observation (:trajectory acting-multi-a))
       (mapv :next-observation (:trajectory acting-multi-b)))}
   {:id :roll-forward-iterates-no-dynamics
    :result (if (= iterated-obs (get-in acting-multi-a [:final-state :observation])) :pass :fail)
    :what "REVIEW ADDITION (wm-build-work). FUNDAMENTALS.edn:169-176 states, as a consequence of the uncontrolled runtime B, that predict-multi-horizon 'chains the same action-delta K times; there is no dynamics to iterate'. That is a claim about the node, so it is MEASURED here rather than cited: the K-step final observation is compared against re-applying the SINGLE-step node to its own output K times with the belief held fixed. Equality means the roll-forward carries no state dynamics the single step does not already carry."
    :depth (:T carriers)
    :iterated-single-step-observation iterated-obs
    :multi-horizon-final-observation (get-in acting-multi-a [:final-state :observation])
    :equal (= iterated-obs (get-in acting-multi-a [:final-state :observation]))
    :basis "holes/labs/wm-contract/FUNDAMENTALS.edn:169-176"}
   {:id :depth-is-opt-in-and-corpus-shows-effective-history
    ;; REVIEW FIX: :result was a literal :pass. It is now a CROSS-CHECK that can
    ;; fail -- if the source supplies a horizon depth, the corpus must record
    ;; that depth, and if the source supplies none, the corpus must record none.
    :result (if (= (boolean (seq horizon-supplied-in-source))
                   (boolean (seq recorded-horizon-depths))) :pass :fail)
    :source-supplies-horizon (vec horizon-supplied-in-source)
    :recorded-horizon-depths recorded-horizon-depths
    :default-horizon fm/default-horizon-steps
    :source-search-roots ["src" "scripts" "checks"] :source-hit-count (count horizon-hits)
    :source-hits horizon-hits
    :records-total (count records) :records-with-horizon-field (count horizon-field-records)
    :records-with-multi-horizon-evidence (count horizon-records)
    :records-without-horizon-evidence (- (count records) (count horizon-records))
    :recorded-horizon-value-distribution horizon-values
    :first-file (some-> records first :file) :last-file (some-> records last :file)
    :recorded-effective-depth (if (seq horizon-records) :mixed :single-step-no-recorded-horizon)}
   {:id :live-and-predictive-dynamics-do-not-share-one-kernel
    ;; REVIEW FIX: :result and :live-path-calls-predictive-kernel were literals
    ;; sitting beside a computed hit list. Both are now derived from that list,
    ;; so a future live-path call to the predictive kernel turns this check red
    ;; instead of leaving a stale :pass behind.
    :result (if (seq live-range-kernel-hits) :fail :pass)
    :search-patterns ["predict-effects" "forward-model/predict" "fm/predict"]
    :search-roots ["src" "scripts" "checks"] :hit-count (count kernel-hits) :hits kernel-hits
    :live-dynamics "src/futon2/aif/enact.clj:250-336 (enact! and close-loop!)"
    :live-range enact-live-range
    :live-range-kernel-hits live-range-kernel-hits
    :live-path-calls-predictive-kernel (boolean (seq live-range-kernel-hits))}
   {:id :machine-q-boundary-refuses-the-gaussian-proxy
    :result (if (= :refusal/action-grain-gaussian-proxy (:refusal machine-q-refusal)) :pass :fail)
    :refusal machine-q-refusal :records-total (count records)
    :records-with-machine-q (count machine-q-records)}])

(def plants
  [{:id :unnormalised-a-row :carrier (assoc-in carriers [:A :s0] {:good 0.75 :bad 0.75})
    :caught-by :declared-model-validation}
   {:id :unnormalised-b-row :carrier (assoc-in carriers [:B :advance :s0] {:s0 0.75 :s1 0.75})
    :caught-by :declared-model-validation}
   {:id :rolls-t-minus-one :carrier (update carriers :T dec) :caught-by :reference-row}
   {:id :sums-over-outcomes-instead-of-states :carrier (assoc carriers :outcomes [:good])
    :caught-by :declared-model-validation}
   {:id :depth-zero-returns-mu :carrier (assoc carriers :T 0) :caught-by :reference-row}
   ;; REVIEW FIX (wm-build-work): the delivered plant compared the literal
   ;; :shipped-node against the literal :carriers and so could not fail -- a
   ;; negative control that exercises nothing. It now BUILDS the wrong reference
   ;; out of the shipped node's own return and requires that it not reproduce
   ;; the canonical row.
   {:id :reference-reads-shipped-node
    :built-row (get-in single-a [:next-observation :mean])
    :caught-by :reference-provenance}])
(def canonical-row (:outcome-row reference))
(def plant-results
  (mapv (fn [{:keys [id carrier built-row caught-by]}]
          (let [caught (case caught-by
                         :declared-model-validation (not (model-valid? carrier))
                         :reference-row (not= canonical-row (outcome-row carrier))
                         :reference-provenance (not= canonical-row built-row))]
            {:id id :result (if caught :caught :escaped) :caught-by [caught-by]
             :evidence (case caught-by
                         :declared-model-validation {:model-valid (model-valid? carrier)}
                         :reference-row {:planted-row (outcome-row carrier)
                                         :canonical-row canonical-row}
                         :reference-provenance {:built-row built-row
                                                :canonical-row canonical-row})}))
        plants))
(def all-caught (every? #(= :caught (:result %)) plant-results))
(def all-pass (every? #(= :pass (:result %)) checks))

(def receipt
  {:harness :v7-r4-node-sim :row :V7 :slice 8 :node :R4 :stage "EVALUATE"
   :stage-at "p4ng/empirics-futon/control-stages.edn:21"
   :carriers "holes/labs/wm-contract/sim/R4-carriers.edn"
   :reference-independence "The categorical Q(s|pi) and Q(o|pi) references are computed only from R4-carriers.edn. futon2.aif.forward-model is called only for node-under-test observations."
   :declaration-sites (:declaration-sites carriers) :checks checks :verdict (if all-pass :pass :fail)
   :negative-controls {:plants plant-results :n (count plant-results) :all-caught all-caught}
   :corpus {:root "data/wm-trace" :read-only true :file-count (count (trace-files)) :record-count (count records)}
   :review
   {:by "wm-build-work (the reviewing seat), 2026-09-06"
    :authored-by "codex-9, bell invoke-1788718455975-13359-32467865, futon2 a53f991e"
    :fixes ["Check :shipped-return-is-a-channel-gaussian-not-declared-q carried a LITERAL :result :pass and a literal :can-read-as-reference false beside a computed :alphabet-intersection. Both are now derived from the shipped return's own key set and mass."
            "Check :live-and-predictive-dynamics-do-not-share-one-kernel carried a literal :result :pass and a literal :live-path-calls-predictive-kernel false beside a computed hit list. Both are now derived from the hits that fall inside the declared live range."
            "Check :depth-is-opt-in-and-corpus-shows-effective-history carried a literal :result :pass. It is now a cross-check between what the source supplies and what the corpus recorded, and fails if they disagree."
            "The plant :reference-reads-shipped-node compared the literal :shipped-node against the literal :carriers -- a negative control that exercised nothing and could not fail. It now builds the wrong reference from the shipped node's own return and requires that it not reproduce the canonical row."
            "The carrier exercised the node only under {:type :no-op}, whose predict-effects arm returns an EMPTY observation delta, so belief-independence of the observation held for the trivial reason that nothing moved it. An :acting-action arm that does move the observation was added and the belief-independence measured under it."]
    :additions ["Check :roll-forward-iterates-no-dynamics, which MEASURES the consequence FUNDAMENTALS.edn:169-176 asserts about the roll-forward instead of citing it."]}
   :not-done ["No live tick or run lock; trace data was read only."
              "No source, registry, ledger, document, p4ng, or data file changed."
              "No equation-level numerical equality is claimed between disjoint outcome and channel alphabets."]})
(def out (io/file lab "runs/V7-R4-node-sim/00-r4.edn"))
(.mkdirs (.getParentFile out))
(with-open [w (io/writer out)] (binding [*out* w] (pp/pprint receipt)))
(doseq [c checks] (println (format "  %-62s %s" (name (:id c)) (name (:result c)))))
(doseq [p plant-results] (println "  planted" (:id p) (:result p) (:caught-by p)))
(println "  verdict" (:verdict receipt) "all-caught" all-caught "receipt" (str out))
(System/exit (if (and all-pass all-caught) 0 1))
