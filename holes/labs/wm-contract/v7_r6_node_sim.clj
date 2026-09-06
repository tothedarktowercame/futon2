(ns v7-r6-node-sim
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.aif.policy :as policy]))

(def lab (io/file "holes/labs/wm-contract"))
(def carriers (edn/read-string (slurp (io/file lab "sim/R6-carriers.edn"))))
(def tolerance 1.0e-12)

(defn reference-scores [g tau priors {:keys [f-pi-policy-posterior? f-pi-values f-pi-scaling]}]
  (mapv (fn [grade prior f]
          (- (+ prior (/ (- grade) tau))
             (if f-pi-policy-posterior?
               (if (= :by-tau f-pi-scaling) (/ f tau) f)
               0.0)))
        g (or priors (repeat (count g) 0.0)) (or f-pi-values (repeat (count g) 0.0))))

(defn reference-softmax [scores]
  (let [m (apply max scores)
        ws (mapv #(Math/exp (- % m)) scores)
        z (reduce + ws)]
    (mapv #(/ % z) ws)))

(defn max-dev [a b]
  (apply max 0.0 (map #(Math/abs (- (double %1) (double %2))) a b)))

(def g (:g carriers))
(def tau (:tau carriers))
(def priors (:log-priors carriers))
(def off (:opts-off carriers))
(def on (:opts-unscaled carriers))
(def by-tau (:opts-by-tau carriers))
(def reference-off (reference-scores g tau priors off))
(def reference-on (reference-scores g tau priors on))
(def score-comparisons
  [{:arity 3 :node (policy/selection-scores g tau priors) :reference reference-off}
   {:arity 4 :mode :off :node (policy/selection-scores g tau priors off) :reference reference-off}
   {:arity 4 :mode :unscaled :node (policy/selection-scores g tau priors on) :reference reference-on}])
(def softmax-comparisons
  [{:arity 2 :node (policy/softmax-weights g tau)
    :reference (reference-softmax (reference-scores g tau nil off))}
   {:arity 3 :node (policy/softmax-weights g tau priors)
    :reference (reference-softmax reference-off)}
   {:arity 4 :mode :off :node (policy/softmax-weights g tau priors off)
    :reference (reference-softmax reference-off)}
   {:arity 4 :mode :unscaled :node (policy/softmax-weights g tau priors on)
    :reference (reference-softmax reference-on)}])

(def f-unscaled (policy/selection-scores g tau priors on))
(def f-scaled (policy/selection-scores g tau priors by-tau))
(def prior-unscaled (policy/selection-scores g tau priors off))
(def prior-scaled-reference (mapv (fn [grade prior] (+ (/ (- grade) tau) (/ prior tau))) g priors))

(def source-search-files
  (->> [(io/file "src/futon2/aif") (io/file "scripts/futon2/report")]
       (mapcat file-seq) (filter #(.isFile %))
       (filter #(re-find #"\.(clj|cljc)$" (.getName %)))
       (sort-by #(.getPath %))))
(defn source-hits [re]
  (vec (mapcat (fn [f]
                 (keep-indexed (fn [i line]
                                 (when (re-find re line)
                                   {:file (.getPath f) :line (inc i) :text (str/trim line)}))
                               (str/split-lines (slurp f))))
               source-search-files)))
(def candidate-hits (source-hits #"propose-actions|action-proposer|wm-candidates|:candidates"))
(def fpi-sites (source-hits #"f-pi-policy-posterior\?"))
(def fpi-application-sites
  (vec (filter #(and (= "src/futon2/aif/policy.clj" (:file %))
                     (<= 208 (:line %) 213))
               fpi-sites)))

(def proof-receipt (edn/read-string (slurp (io/file lab "softmax-positive-receipt.edn"))))
(def declaration-start #"^(?:private\s+)?(?:noncomputable\s+)?(?:structure|inductive|abbrev|def|theorem|lemma)\s+")
(defn declaration-text [source declaration]
  (let [lines (vec (str/split-lines source))
        p (re-pattern (str "^(?:private\\s+)?(?:noncomputable\\s+)?(?:structure|inductive|abbrev|def|theorem|lemma)\\s+"
                           (java.util.regex.Pattern/quote declaration) "(?:\\s|$)"))
        boundary? #(or (re-find declaration-start %) (re-find #"^(?:namespace|end)\s" %))
        start (first (keep-indexed #(when (re-find p %2) %1) lines))
        end (or (first (keep-indexed #(when (and (> %1 start) (boundary? %2)) %1) lines)) (count lines))]
    (str (str/join "\n" (subvec lines start end)) "\n")))
(defn sha256-text [s]
  (let [d (java.security.MessageDigest/getInstance "SHA-256")]
    (.update d (.getBytes s "UTF-8"))
    (format "%064x" (java.math.BigInteger. 1 (.digest d)))))
(defn sha256-file [f] (sha256-text (slurp f)))
(def declaration-checks
  (mapv (fn [{:keys [repo path declarations]}]
          (let [f (io/file "/home/joe/code" repo path) source (slurp f)]
            {:file (str repo "/" path)
             :declarations
             (mapv (fn [{:keys [name sha256]}]
                     (let [actual (sha256-text (declaration-text source name))]
                       {:name name :expected sha256 :actual actual :match? (= sha256 actual)}))
                   declarations)}))
        (:source-basis proof-receipt)))
(def fixture-hash (sha256-file (io/file lab "softmax-reference.edn")))
(def lean-run
  (shell/with-sh-dir "/home/joe/code/mathlib4"
    (shell/sh "lake" "env" "lean" "DarkTower/WarMachine/SoftmaxWitness.lean")))
(def axiom-file (java.io.File/createTempFile "v7-r6-axioms" ".lean"))
(spit axiom-file "import DarkTower.WarMachine.SoftmaxWitness\n#print axioms DarkTower.WarMachine.SoftmaxWitness.referenceWeights\n")
(def axiom-run
  (shell/with-sh-dir "/home/joe/code/mathlib4"
    (shell/sh "lake" "env" "lean" (.getAbsolutePath axiom-file))))
(.delete axiom-file)
(def axiom-output (str/trim (str (:out axiom-run) (:err axiom-run))))

(defn trace-files []
  (->> (.listFiles (io/file "data/wm-trace"))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
       (sort-by #(.getName %))))
(defn read-records [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [out []]
      (let [x (try (edn/read {:eof ::eof :default (fn [_ v] v)} r) (catch Exception _ ::bad))]
        (cond (= x ::eof) out (= x ::bad) (recur out) :else (recur (conj out x)))))))
(def records (vec (mapcat (fn [f] (map #(assoc % :file (.getName f)) (read-records f))) (trace-files))))
(defn action-key [a] (select-keys a [:type :target :target-class]))
(defn first-max-by [f xs]
  (reduce (fn [best x] (if (> (double (f x)) (double (f best))) x best)) (first xs) (rest xs)))
(defn posterior-result [record]
  (let [weights (get-in record [:decision :softmax-weights])
        ranking (get-in record [:decision :habit-adjusted-ranking])
        f-pi-applied? (true? (get-in record [:decision :f-pi-posterior :applied?]))
        action (get-in record [:decision :action])
        [kind best]
        (cond
          (and (map? weights) (seq weights)) [:softmax-weights (key (first-max-by val (seq weights)))]
          (and (not f-pi-applied?) (seq ranking))
          [:selection-score-equivalent (:action (first-max-by :selection-score ranking))]
          :else [nil nil])]
    (when (and kind (map? action))
      {:file (:file record) :kind kind
       :equal? (= (action-key action) (action-key best))})))
(def corpus-posterior-results (vec (keep posterior-result records)))
(def corpus-agree (count (filter :equal? corpus-posterior-results)))
(def corpus-differ (- (count corpus-posterior-results) corpus-agree))
(def era (->> corpus-posterior-results (group-by :file)
              (map (fn [[f xs]] [f {:n (count xs) :agree (count (filter :equal? xs))}]))
              (sort-by first) vec))

(def synthetic {:file "synthetic.edn" :decision {:action {:type :a}
                                                  :softmax-weights {{:type :a} 0.75 {:type :b} 0.25}}})
(def synthetic-result (posterior-result synthetic))
(def plant-f-score (reference-scores g tau priors by-tau))
(def plant-prior-score prior-scaled-reference)

(def checks
  [{:id :posterior-agrees-with-independent-reference
    :result (if (and (every? #(<= (max-dev (:node %) (:reference %)) tolerance) score-comparisons)
                     (every? #(<= (max-dev (:node %) (:reference %)) tolerance) softmax-comparisons)) :pass :fail)
    :score-comparisons (mapv #(assoc % :max-deviation (max-dev (:node %) (:reference %))) score-comparisons)
    :softmax-comparisons (mapv #(assoc % :max-deviation (max-dev (:node %) (:reference %))) softmax-comparisons)
    :tolerance tolerance :basis "src/futon2/aif/policy.clj:157-235"}
   {:id :f-pi-enters-unscaled
    :result (if (and (= f-unscaled reference-on) (not= f-unscaled f-scaled)) :pass :fail)
    :tau tau :node-unscaled f-unscaled :by-tau-counterfactual f-scaled
    :max-difference-from-by-tau (max-dev f-unscaled f-scaled)
    :basis "src/futon2/aif/policy.clj:199-212"}
   {:id :habit-prior-enters-unscaled
    :result (if (and (= prior-unscaled reference-off) (not= prior-unscaled prior-scaled-reference)) :pass :fail)
    :tau tau :node-scores prior-unscaled :prior-divided-counterfactual prior-scaled-reference
    :max-difference-from-divided-prior (max-dev prior-unscaled prior-scaled-reference)
    :basis "src/futon2/aif/policy.clj:215-229"}
   {:id :candidate-space-has-runtime-constructors-but-no-single-r6-carrier
    :result (if (seq candidate-hits) :pass :fail)
    :search "propose-actions|action-proposer|wm-candidates|:candidates"
    :hit-count (count candidate-hits) :hits candidate-hits
    :registry-limit "holes/labs/wm-contract/aif-equations.edn:158-160 has no :code or :lean"
    :problem-basis "holes/problems/P-validated-R5.md:381-393"}
   {:id :lean-positive-receipt-revalidates
    :result (if (and (zero? (:exit lean-run)) (zero? (:exit axiom-run))) :pass :fail)
    :elaboration-exit (:exit lean-run) :elaboration-output (str/trim (str (:out lean-run) (:err lean-run)))
    :axiom-exit (:exit axiom-run) :axiom-output axiom-output
    :declaration-checks declaration-checks
    :declaration-match-count (count (filter :match? (mapcat :declarations declaration-checks)))
    :declaration-mismatch-count (count (remove :match? (mapcat :declarations declaration-checks)))
    :fixture-expected (get-in proof-receipt [:fixture :sha256]) :fixture-actual fixture-hash
    :fixture-match? (= fixture-hash (get-in proof-receipt [:fixture :sha256]))
    :finding (when-not (every? :match? (mapcat :declarations declaration-checks))
               :positive-source-drift)
    :basis "holes/labs/wm-contract/softmax-positive-receipt.edn:1"}
   {:id :strategic-selector-receives-ranking-not-posterior
    :result (if (and (seq (source-hits #"invoke-strategic-selection"))
                     (seq (source-hits #":controller-ranking"))) :pass :fail)
    :stale-pointer-5438 "scripts/futon2/report/war_machine.clj:5438 is scan-aif-heads prose"
    :stale-pointer-5406-5410 "scripts/futon2/report/war_machine.clj:5406-5410 is batch-pattern data"
    :current-call "scripts/futon2/report/war_machine.clj:6475-6480"
    :route "scripts/futon2/report/war_machine.clj:6481"
    :crosses {:controller-ranking true :selection-score true :tau false :posterior false}
    :ranking-shape "src/futon2/aif/policy.clj:495-501,636-640"}
   {:id :corpus-posterior-vs-recorded-action
    :result (if (= (count corpus-posterior-results) (+ corpus-agree corpus-differ)) :pass :fail)
    :records-total (count records) :files-total (count (trace-files))
    :records-with-posterior-and-action (count corpus-posterior-results)
    :representation-counts (frequencies (map :kind corpus-posterior-results))
    :agree corpus-agree :differ corpus-differ :by-file era
    :first-file (some-> records first :file) :last-file (some-> records last :file)
    :basis "src/futon2/aif/policy.clj:664-670; scripts/futon2/report/war_machine.clj:6475-6481"}
   {:id :f-pi-posterior-site-census
    :result (if (= 1 (count fpi-application-sites)) :pass :fail)
    :search "f-pi-policy-posterior?" :symbol-occurrence-count (count fpi-sites)
    :symbol-occurrences fpi-sites
    :application-site-count (count fpi-application-sites)
    :application-sites fpi-application-sites
    :claim-site "src/futon2/aif/policy.clj:545-553"}])

(def plants
  [{:id :f-pi-divided-by-tau :counter :a2-max-deviation
    :moved (max-dev f-unscaled plant-f-score) :result (if (pos? (max-dev f-unscaled plant-f-score)) :caught :escaped)}
   {:id :habit-prior-divided-by-tau :counter :a3-max-deviation
    :moved (max-dev prior-unscaled plant-prior-score) :result (if (pos? (max-dev prior-unscaled plant-prior-score)) :caught :escaped)}
   {:id :synthetic-action-equals-posterior-argmax :counter :d3-agreement
    :before corpus-agree :after (+ corpus-agree (if (:equal? synthetic-result) 1 0))
    :moved (if (:equal? synthetic-result) 1 0) :result (if (:equal? synthetic-result) :caught :escaped)}])

(def all-pass (every? #(= :pass (:result %)) checks))
(def all-caught (every? #(= :caught (:result %)) plants))
(def receipt
  {:harness :v7-r6-node-sim :row :V7 :slice 10 :node :R6 :stage "SELECT"
   :stage-at "/home/joe/code/p4ng/empirics-futon/control-stages.edn:23"
   :carriers "holes/labs/wm-contract/sim/R6-carriers.edn"
   :reference-independence "Scores and softmax are derived only from R6-carriers.edn and the registry formal line; futon2.aif.policy is called only as the node under test."
   :declaration-sites (:declaration-sites carriers) :checks checks
   :negative-controls {:plants plants :all-caught all-caught}
   :corpus {:root "data/wm-trace" :read-only true :files (count (trace-files)) :records (count records)}
   :verdict (if (and all-pass all-caught) :pass :fail)
   :not-done ["No live tick or run lock; trace files were read only."
              "No production, registry, ledger, document, p4ng, Lean, or data file was modified."]})
(def out (io/file lab "runs/V7-R6-node-sim/00-r6.edn"))
(.mkdirs (.getParentFile out))
(with-open [w (io/writer out)] (binding [*out* w] (pp/pprint receipt)))
(doseq [c checks] (println (format "  %-58s %s" (name (:id c)) (name (:result c)))))
(println "  verdict" (:verdict receipt) "receipt" (str out))
(System/exit (if (= :pass (:verdict receipt)) 0 1))
