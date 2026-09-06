(ns v7-r14-node-sim
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.aif.policy :as policy]
            [futon2.aif.selection-gain :as selection-gain]))

(load-file "scripts/futon2/report/war_machine.clj")
(def tau-mode-of (ns-resolve 'futon2.report.war-machine 'tau-mode-of))
(def lab (io/file "holes/labs/wm-contract"))
(def carriers (edn/read-string (slurp (io/file lab "sim/R14-carriers.edn"))))
(def tolerance 1.0e-12)

(def g-totals (:g-totals carriers))
(def gain (:selection-gain carriers))
(def beta (:variational-beta carriers))
(def tau-min (:tau-min carriers))
(def spread-k (:spread-k carriers))
(defn reference-spread [k]
  (max tau-min (/ (- (apply max g-totals) (apply min g-totals)) k)))
(defn reference-law [mode]
  (let [g (max tau-min gain)]
    (case mode :spread (/ (reference-spread spread-k) g)
          :selection-gain-only (/ 1.0 g)
          :variational-beta-gamma beta)))
(def modes [:spread :selection-gain-only :variational-beta-gamma])
(def law-results
  (mapv (fn [mode]
          (let [reference (reference-law mode)
                node (policy/effective-temperature
                      g-totals gain {:tau-mode mode :variational-beta beta
                                     :variational-beta-source :converged-posterior})]
            {:mode mode :reference reference :node node
             :deviation (Math/abs (- node reference))})) modes))
(def gammas (mapv (fn [{:keys [mode node]}] {:mode mode :gamma (/ 1.0 node)
                                             :equals-one-over-beta? (= (/ 1.0 node) (/ 1.0 beta))}) law-results))

(defn source-lines [f re]
  (->> (str/split-lines (slurp f))
       (keep-indexed (fn [i line] (when (re-find re line) {:line (inc i) :text (str/trim line)}))) vec))
(def policy-file (io/file "src/futon2/aif/policy.clj"))
(def wm-file (io/file "scripts/futon2/report/war_machine.clj"))
(def gain-file (io/file "src/futon2/aif/selection_gain.clj"))
(def derived-sites
  {:tau-mode (source-lines wm-file #"defn tau-mode-of")
   :function-default (source-lines policy-file #":or \{tau-min 0\.01 tau-mode :spread\}")
   :score-divide (source-lines policy-file #"\(/ \(- \(double g\)\) \(double tau\)\)")
   :burn-in (source-lines gain-file #"def default-min-history")})
(def default-results {:arena-nil (tau-mode-of nil) :arena-junk (tau-mode-of "junk")
                      :function (if (= (policy/effective-temperature g-totals gain {})
                                       (policy/effective-temperature g-totals gain {:tau-mode :spread}))
                                  :spread :not-spread)})

(defn first-max-index [xs]
  (reduce (fn [best i] (if (> (double (nth xs i)) (double (nth xs best))) i best)) 0 (range 1 (count xs))))
(defn selection-measure [{:keys [ids g log-priors temperatures]}]
  (mapv (fn [tau]
          (let [scores (policy/selection-scores g tau log-priors)
                weights (policy/softmax-weights g tau log-priors)
                i (first-max-index scores)]
            {:tau tau :scores scores :weights weights :order (mapv ids (sort-by #(nth scores %) > (range (count ids))))
             :argmax (nth ids i)})) temperatures))
(def zero-prior-measures (selection-measure (:zero-prior-case carriers)))
(def habit-measures (selection-measure (:habit-case carriers)))

(def lean-generated (source-lines (io/file "/home/joe/code/p4ng/sec-lean-state-generated.tex") #"temperature.*R14"))
(def registry-temperature (source-lines (io/file lab "aif-equations.edn") #":id :temperature"))

(defn trace-files []
  (->> (.listFiles (io/file "data/wm-trace"))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
       (sort-by #(.getName %))))
(defn read-records
  "Records of one trace file, plus whether an unreadable form stopped the read.
   REVIEW FIX (slice 12): the delivered form recurred on an unreadable form
   without consuming it, which spins if one ever appears and silently drops the
   file's tail if it does not. Stopping is reported, so a truncated read is
   visible in the receipt rather than absorbed into a count."
  [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [out []]
      (let [x (try (edn/read {:eof ::eof :default (fn [_ v] v)} r) (catch Exception _ ::bad))]
        (cond (= x ::eof) {:records out :truncated? false}
              (= x ::bad) {:records out :truncated? true}
              :else (recur (conj out x)))))))
(def per-file (mapv (fn [f] (assoc (read-records f) :file (.getName f))) (trace-files)))
(def truncated-files (mapv :file (filter :truncated? per-file)))
(def records (vec (mapcat (fn [{:keys [file records]}] (map #(assoc % :source-file file) records)) per-file)))
(defn tree-values [x k]
  (cond (map? x) (concat (when (contains? x k) [(get x k)]) (mapcat #(tree-values % k) (vals x)))
        (sequential? x) (mapcat #(tree-values % k) x) :else []))
(defn field-record-count [k] (count (filter #(seq (tree-values % k)) records)))
(defn distribution [xs] (into (sorted-map-by #(compare (pr-str %1) (pr-str %2))) (frequencies xs)))
(def tau-values (vec (mapcat #(tree-values % :tau) records)))
(def tau-source-values (vec (mapcat #(tree-values % :tau-source) records)))
(def gain-states (vec (keep :selection-gain records)))
(def samples (vec (keep :samples gain-states)))
(def gain-values (vec (keep :selection-gain gain-states)))
(def gain-files (vec (distinct (map :source-file (filter :selection-gain records)))))

(defn action-key [a] (select-keys a [:type :target :target-class]))
(defn first-max-by [f xs] (reduce (fn [b x] (if (> (double (f x)) (double (f b))) x b)) (first xs) (rest xs)))
(defn corpus-choice [r]
  (let [ranking (get-in r [:decision :habit-adjusted-ranking]) action (get-in r [:decision :action])]
    (when (and (seq ranking) (map? action))
      (= (action-key action) (action-key (:action (first-max-by :selection-score ranking)))))))
(def corpus-choices (vec (keep corpus-choice records)))

(defn paths-of [x k prefix]
  (cond (map? x) (concat (when (contains? x k) [[(conj prefix k) (get x k)]])
                         (mapcat (fn [[kk v]] (paths-of v k (conj prefix kk))) x))
        (sequential? x) (mapcat #(paths-of % k (conj prefix :*)) x)
        :else []))
(defn era [rs]
  (let [fs (sort (distinct (map :source-file rs)))]
    {:files (count fs) :first (first fs) :last (last fs)}))

;; REVIEW FIX (slice 12): the delivered harness looked for the outcome-learned
;; gain under the TOP-LEVEL :selection-gain key only, which is one of two
;; carriers the corpus has held it in. The key-path census is what shows the
;; other one.
(def gain-key-paths (distribution (map first (mapcat #(paths-of % :selection-gain []) records))))
(def samples-key-paths (distribution (map first (mapcat #(paths-of % :samples []) records))))

;; Carrier 2, live today: selection_gain.clj, added futon2 9d8f2dee (2026-07-14).
(def sg-records (vec (filter :selection-gain records)))
(def sg-gains (mapv #(double (get-in % [:selection-gain :selection-gain])) sg-records))
(def sg-samples (mapv #(long (get-in % [:selection-gain :samples] 0)) sg-records))
(def sg-census
  {:records (count sg-records) :era (era sg-records)
   :gain (distribution sg-gains) :samples (distribution sg-samples)
   :off-prior (count (remove #(= 1.0 %) sg-gains))
   :burn-in selection-gain/default-min-history
   :past-burn-in (count (filter #(>= % (long selection-gain/default-min-history)) sg-samples))})
(defn sg-law-deviation [r floor-fn]
  (Math/abs (- (double (get-in r [:decision :tau]))
               (/ 1.0 (floor-fn tau-min (double (get-in r [:selection-gain :selection-gain])))))))
(def sg-law-deviations (mapv #(sg-law-deviation % max) sg-records))

;; Carrier 1, retired: the :policy-precision state of policy_precision.clj as it
;; was first committed at futon2 e8680237 (2026-06-27, "R14 precision-over-policies").
(def pp-records (vec (filter :policy-precision records)))
(def pp-gammas (mapv #(double (get-in % [:policy-precision :policy-precision])) pp-records))
(def pp-samples (mapv #(long (get-in % [:policy-precision :samples] 0)) pp-records))
(def pp-census
  {:records (count pp-records) :era (era pp-records)
   :gamma-distinct (count (distinct pp-gammas))
   :gamma-min (apply min pp-gammas) :gamma-max (apply max pp-gammas)
   :off-prior (count (remove #(= 1.0 %) pp-gammas))
   :past-burn-in (count (filter #(>= % (long selection-gain/default-min-history)) pp-samples))
   :state-shapes (distribution (map #(vec (sort (map name (keys (:policy-precision %))))) pp-records))})
(defn pp-law-deviation [r floor-fn]
  (Math/abs (- (double (get-in r [:decision :tau]))
               (/ (double (get-in r [:decision :tau-spread]))
                  (floor-fn tau-min (double (get-in r [:policy-precision :policy-precision])))))))
(def pp-law-deviations (mapv #(pp-law-deviation % max) pp-records))

;; The feed that earns the gain.
(def ro-records (vec (filter #(seq (tree-values % :realized-outcome)) records)))
(def feed-census
  {:records (count ro-records) :era (era ro-records)
   :all-before-the-live-carrier?
   (boolean (and (seq ro-records) (seq sg-records)
                 (neg? (compare (:last (era ro-records)) (:first (era sg-records))))))})

;; The recorded orderings, and whether the dial could have moved them.
(def ranked-records (vec (filter #(seq (get-in % [:decision :habit-adjusted-ranking])) records)))
(defn entry-score [tau e]
  (+ (/ (- (double (:controller-score e))) (double tau)) (double (or (:habit-prior-bias e) 0.0))))
(defn argmax-action-at [entries tau]
  (:action (reduce (fn [b e] (if (> (entry-score tau e) (entry-score tau b)) e b)) (first entries) (rest entries))))
(defn score-identity-deviation [r tau-scale]
  (let [tau (* tau-scale (double (get-in r [:decision :tau])))]
    (apply max 0.0 (map (fn [e] (Math/abs (- (double (:selection-score e)) (entry-score tau e))))
                        (get-in r [:decision :habit-adjusted-ranking])))))
(def score-identity-deviations (mapv #(score-identity-deviation % 1.0) ranked-records))
(def dial-grid [0.01 0.1 0.5 1.0 2.0 5.0 14.6 100.0])
(def observed-grid [0.1 0.5 1.0 2.0 5.0 14.6])
(defn argmax-count [r grid]
  (let [es (get-in r [:decision :habit-adjusted-ranking])]
    (count (distinct (map #(argmax-action-at es %) grid)))))
(def dial
  {:records (count ranked-records) :era (era ranked-records)
   :recorded-tau (distribution (map #(get-in % [:decision :tau]) ranked-records))
   :prior-vector-non-constant
   (count (filter (fn [r] (> (count (distinct (map #(double (or (:habit-prior-bias %) 0.0))
                                                   (get-in r [:decision :habit-adjusted-ranking])))) 1))
                  ranked-records))
   :argmax-moves-between-the-extremes (count (filter #(> (argmax-count % [(first dial-grid) (last dial-grid)]) 1) ranked-records))
   :argmax-moves-over-the-grid (count (filter #(> (argmax-count % dial-grid) 1) ranked-records))
   :argmax-moves-within-the-observed-range (count (filter #(> (argmax-count % observed-grid) 1) ranked-records))
   :grid dial-grid :observed-range observed-grid})
(def habit-flags
  {:habit-prior-applied? (distribution (mapcat #(tree-values % :habit-prior-applied?) records))
   :habit-authority (distribution (mapcat #(tree-values % :habit-authority) records))
   :governed-by (distribution (mapcat #(tree-values % :governed-by) records))})

(def artifact (io/file lab "runs/F8-temperature/clojure-readback.txt"))
(defn sha256-file [f]
  (let [d (java.security.MessageDigest/getInstance "SHA-256")]
    (.update d (java.nio.file.Files/readAllBytes (.toPath f)))
    (format "%064x" (java.math.BigInteger. 1 (.digest d)))))
(def artifact-before (sha256-file artifact))
(def readback-run (shell/sh "clojure" "-M" "holes/labs/wm-contract/f8_temperature_readback.clj"))
(def artifact-after (sha256-file artifact))

(def k4-spread (reference-spread 4.0))
(def min-floor-g (min tau-min gain))
(def reordered
  (update (:zero-prior-case carriers) :ids
          #(vec (concat (rest %) [(first %)]))))
(def reordered-measure (selection-measure reordered))
(def plants
  [{:id :spread-k-four :deviation (Math/abs (- (reference-law :spread) (/ k4-spread (max tau-min gain))))
    :result (if (not= (reference-law :spread) (/ k4-spread (max tau-min gain))) :caught :escaped)}
   {:id :gain-floor-uses-min :bad-g min-floor-g :bad-value (/ (reference-spread spread-k) min-floor-g)
    :deviation (Math/abs (- (reference-law :spread) (/ (reference-spread spread-k) min-floor-g)))
    :result (if (not= (reference-law :spread) (/ (reference-spread spread-k) min-floor-g)) :caught :escaped)}
   {:id :positional-argmax-after-reorder
    :original-argmax (:argmax (first zero-prior-measures))
    :corrupted-argmax (:argmax (first reordered-measure))
    :reported-move (not= (:argmax (first zero-prior-measures)) (:argmax (first reordered-measure)))
    :result (if (not= (:argmax (first zero-prior-measures)) (:argmax (first reordered-measure))) :caught :escaped)}
   {:id :live-law-floor-inverted
    :max-deviation (apply max 0.0 (mapv #(sg-law-deviation % min) sg-records))
    :result (if (some #(>= % tolerance) (mapv #(sg-law-deviation % min) sg-records)) :caught :escaped)}
   {:id :spread-law-floor-inverted
    :max-deviation (apply max 0.0 (mapv #(pp-law-deviation % min) pp-records))
    :result (if (some #(>= % tolerance) (mapv #(pp-law-deviation % min) pp-records)) :caught :escaped)}
   {:id :recorded-scores-at-double-tau
    :max-deviation (apply max 0.0 (mapv #(score-identity-deviation % 2.0) ranked-records))
    :result (if (some #(>= % 1.0e-9) (mapv #(score-identity-deviation % 2.0) ranked-records)) :caught :escaped)}
   {:id :pinned-tau-mode-line :pinned 865 :derived (get-in derived-sites [:tau-mode 0 :line])
    :result (if (not= 865 (get-in derived-sites [:tau-mode 0 :line])) :caught :escaped)}])

(def checks
  [{:id :three-laws-against-an-independent-reference
    :result (if (every? #(zero? (:deviation %)) law-results) :pass :fail)
    :laws law-results :basis "src/futon2/aif/policy.clj:33-146"}
   {:id :the-row-names-one-quantity-and-the-code-has-three-laws
    :result (if (= 3 (count (distinct (map :node law-results)))) :pass :fail)
    :values (mapv #(select-keys % [:mode :node]) law-results) :pairwise-distinct? (= 3 (count (distinct (map :node law-results))))
    :basis "holes/labs/wm-contract/aif-equations.edn:167-172"}
   {:id :gamma-equals-one-over-beta-holds-for-which-arm
    :result (if (= [:variational-beta-gamma] (mapv :mode (filter :equals-one-over-beta? gammas))) :pass :fail)
    :one-over-beta (/ 1.0 beta) :gammas gammas :score-divide-site (:score-divide derived-sites)
    :basis "src/futon2/aif/policy.clj:208-212"}
   {:id :live-default-and-function-default
    :result (if (and (= :selection-gain-only (:arena-nil default-results) (:arena-junk default-results))
                     (= :spread (:function default-results))) :pass :fail)
    :defaults default-results :agree? (= (:arena-nil default-results) (:function default-results))
    :derived-sites (select-keys derived-sites [:tau-mode :function-default])
    :basis "scripts/futon2/report/war_machine.clj:857-871; src/futon2/aif/policy.clj:127-134"}
   {:id :does-tau-reach-the-choice
    :result (if (and (= 1 (count (distinct (map :argmax zero-prior-measures))))
                     (> (count (distinct (map :weights zero-prior-measures))) 1)
                     (> (count (distinct (map :argmax habit-measures))) 1)) :pass :fail)
    :zero-prior zero-prior-measures :zero-prior-order-moves? (> (count (distinct (map :order zero-prior-measures))) 1)
    :zero-prior-argmax-moves? (> (count (distinct (map :argmax zero-prior-measures))) 1)
    :zero-prior-weights-move? (> (count (distinct (map :weights zero-prior-measures))) 1)
    :habit-prior habit-measures :habit-winner-moves? (> (count (distinct (map :argmax habit-measures))) 1)
    :basis "src/futon2/aif/policy.clj:157-235"}
   {:id :lean-temperature-readback-still-matches
    :result (if (and (zero? (:exit readback-run)) (= artifact-before artifact-after)) :pass :fail)
    :exit (:exit readback-run) :before-sha artifact-before :after-sha artifact-after :byte-identical? (= artifact-before artifact-after)
    :basis "holes/labs/wm-contract/f8_temperature_readback.clj:1-173"}
   {:id :dossier-residual-1-no-lean-site
    :result (if (and (seq lean-generated) (str/includes? (:text (first lean-generated)) "machineTemperature")) :pass :fail)
    :generated-lines lean-generated :registry-row registry-temperature
    :finding :dossier-stale-lean-absence :basis "holes/labs/wm-contract/PROBLEMS-r16-r13-r14-batch4.md:143-149"}
   {:id :dossier-residual-2-counterfactual-dial
    ;; REVIEW FIX (slice 12): the delivered predicate was
    ;; (= (count xs) (+ trues falses)) over a seq of booleans, which is true of
    ;; every input and so could not fail. The predicate is now the corpus
    ;; measurement itself: every record that carries an ordering carries a tau,
    ;; and the enacted action is compared against that ordering's argmax.
    :result (if (and (pos? (count corpus-choices))
                     (= (count corpus-choices) (count ranked-records))) :pass :fail)
    :record-denominator (count records) :field-record-counts
    {:tau (field-record-count :tau) :tau-effective (field-record-count :tau-effective)
     :tau-mode (field-record-count :tau-mode) :tau-source (field-record-count :tau-source)
     :tau-spread (field-record-count :tau-spread)}
    :tau-value-denominator (count tau-values) :tau-values (distribution tau-values)
    :tau-source-denominator (count tau-source-values) :tau-sources (distribution tau-source-values)
    :choice-denominator (count corpus-choices) :choice-equals-score-argmax (count (filter true? corpus-choices))
    :choice-differs-from-score-argmax (count (filter false? corpus-choices))
    :basis "holes/labs/wm-contract/PROBLEMS-r16-r13-r14-batch4.md:150-154"}
   {:id :the-live-law-reproduces-its-own-era-of-the-corpus
    :result (if (and (seq sg-law-deviations) (every? #(< % tolerance) sg-law-deviations)) :pass :fail)
    :records (count sg-records) :max-deviation (apply max 0.0 sg-law-deviations) :census sg-census
    :basis "src/futon2/aif/policy.clj:132-134; src/futon2/aif/selection_gain.clj:72-80"}
   {:id :the-spread-law-reproduces-the-retired-carriers-era
    :result (if (and (seq pp-law-deviations) (every? #(< % tolerance) pp-law-deviations)) :pass :fail)
    :records (count pp-records) :max-deviation (apply max 0.0 pp-law-deviations) :census pp-census
    :key-paths {:selection-gain gain-key-paths :samples samples-key-paths}
    :basis "src/futon2/aif/policy.clj:33-45,132-133"}
   {:id :the-recorded-orderings-reproduce-and-the-dial-has-authority
    :result (if (and (seq score-identity-deviations)
                     (every? #(< % 1.0e-9) score-identity-deviations)
                     (= (:records dial) (:argmax-moves-between-the-extremes dial))) :pass :fail)
    :max-score-deviation (apply max 0.0 score-identity-deviations)
    :dial dial :habit-flags habit-flags
    :basis "src/futon2/aif/policy.clj:208-213; p4ng/sec-catalog.tex:241"}
   {:id :the-outcome-feed-stopped-before-the-live-carrier-existed
    :result (if (:all-before-the-live-carrier? feed-census) :pass :fail)
    :feed feed-census :basis "src/futon2/aif/selection_gain.clj:100-161"}
   {:id :dossier-residual-3-gain-live-or-at-prior
    :result (if (= (count gain-states) (count gain-values)) :pass :fail)
    :record-denominator (count records) :records-with-gain-state (count gain-states)
    :samples-distribution (distribution samples) :gain-distribution (distribution gain-values)
    :gain-left-prior? (boolean (some #(not= 1.0 %) gain-values))
    :burn-in selection-gain/default-min-history :burn-in-site (:burn-in derived-sites)
    :reached-burn-in? (boolean (some #(>= % selection-gain/default-min-history) samples))
    :first-file (first gain-files) :last-file (last gain-files)
    :basis "src/futon2/aif/selection_gain.clj:72-80,100-161"}])

(def all-pass (every? #(= :pass (:result %)) checks))
(def all-caught (every? #(= :caught (:result %)) plants))
(def receipt
  {:harness :v7-r14-node-sim :row :V7 :slice 12 :node :R14 :stage "SELECT"
   :stage-at "/home/joe/code/p4ng/empirics-futon/control-stages.edn:25"
   :carriers "holes/labs/wm-contract/sim/R14-carriers.edn"
   :reference-independence "All three expected temperature laws are computed from R14-carriers.edn and the registry/docstring formula; policy/effective-temperature supplies only node-under-test values."
   :declaration-sites (:declaration-sites carriers) :checks checks
   :negative-controls {:plants plants :all-caught all-caught}
   :corpus {:root "data/wm-trace" :read-only true :files (count (trace-files)) :records (count records)
            :files-truncated-by-an-unreadable-form truncated-files
            :carrier-eras {:selection-gain (era sg-records) :policy-precision (era pp-records)
                           :realized-outcome (era ro-records) :recorded-orderings (era ranked-records)}}
   :verdict (if (and all-pass all-caught) :pass :fail)
   :not-done ["No live tick or run lock; trace files were read only." "No production, registry, ledger, document, p4ng, Lean, or data file was modified."]})
(def out (io/file lab "runs/V7-R14-node-sim/00-r14.edn"))
(.mkdirs (.getParentFile out))
(with-open [w (io/writer out)] (binding [*out* w] (pp/pprint receipt)))
(doseq [c checks] (println (format "  %-58s %s" (name (:id c)) (name (:result c)))))
(println "  verdict" (:verdict receipt) "receipt" (str out))
(System/exit (if (= :pass (:verdict receipt)) 0 1))
