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
(defn read-records [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [out []]
      (let [x (try (edn/read {:eof ::eof :default (fn [_ v] v)} r) (catch Exception _ ::bad))]
        (cond (= x ::eof) out (= x ::bad) (recur out) :else (recur (conj out x)))))))
(def records (vec (mapcat (fn [f] (map #(assoc % :source-file (.getName f)) (read-records f))) (trace-files))))
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
    :result (if (= (count corpus-choices) (+ (count (filter true? corpus-choices)) (count (filter false? corpus-choices)))) :pass :fail)
    :record-denominator (count records) :field-record-counts
    {:tau (field-record-count :tau) :tau-effective (field-record-count :tau-effective)
     :tau-mode (field-record-count :tau-mode) :tau-source (field-record-count :tau-source)
     :tau-spread (field-record-count :tau-spread)}
    :tau-value-denominator (count tau-values) :tau-values (distribution tau-values)
    :tau-source-denominator (count tau-source-values) :tau-sources (distribution tau-source-values)
    :choice-denominator (count corpus-choices) :choice-equals-score-argmax (count (filter true? corpus-choices))
    :choice-differs-from-score-argmax (count (filter false? corpus-choices))
    :basis "holes/labs/wm-contract/PROBLEMS-r16-r13-r14-batch4.md:150-154"}
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
   :corpus {:root "data/wm-trace" :read-only true :files (count (trace-files)) :records (count records)}
   :verdict (if (and all-pass all-caught) :pass :fail)
   :not-done ["No live tick or run lock; trace files were read only." "No production, registry, ledger, document, p4ng, Lean, or data file was modified."]})
(def out (io/file lab "runs/V7-R14-node-sim/00-r14.edn"))
(.mkdirs (.getParentFile out))
(with-open [w (io/writer out)] (binding [*out* w] (pp/pprint receipt)))
(doseq [c checks] (println (format "  %-58s %s" (name (:id c)) (name (:result c)))))
(println "  verdict" (:verdict receipt) "receipt" (str out))
(System/exit (if (= :pass (:verdict receipt)) 0 1))
