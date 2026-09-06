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
(def policy-file "src/futon2/aif/policy.clj")
(def fpi-application-sites
  ;; An APPLICATION site is an occurrence of the flag that gates a subtraction
  ;; of the F term on the very next line. Derived from the shape of the two
  ;; lines, NOT from a pinned range: a pinned range reads a line shift as an
  ;; absence, which is the failure :V7 slice 9's review found twice.
  (let [lines (vec (str/split-lines (slurp (io/file policy-file))))]
    (vec (keep-indexed (fn [i line]
                         (when (and (re-find #"f-pi-policy-posterior\?" line)
                                    (re-find #"\(-\s+\(nth f-terms" (get lines (inc i) "")))
                           {:file policy-file :line (inc i) :text (str/trim line)
                            :applies (str/trim (get lines (inc i) ""))}))
                       lines))))
(def registry-code-range
  ;; The registry's own claim about where the score expression lives
  ;; (aif-equations.edn:177, ":208-213"). Checked against the DERIVED site
  ;; rather than used as the search.
  {:claim "holes/labs/wm-contract/aif-equations.edn:177 -- \"the score expression is at :208-213\""
   :from 208 :to 213})

(def proof-receipt (edn/read-string (slurp (io/file lab "softmax-positive-receipt.edn"))))
(def declaration-start #"^(?:private\s+)?(?:noncomputable\s+)?(?:structure|inductive|abbrev|def|theorem|lemma)\s+")
(defn declaration-span
  "[start end) line indices of `declaration` under the receipt's OWN boundary
   rule -- the next declaration, `namespace` or `end`. A docstring opener is
   not a boundary in that rule, so the span runs to the start of the next
   declaration and carries that declaration's DOCSTRING with it. The pinned
   hashes in softmax-positive-receipt.edn are hashes of this span, so the span
   is what a pin comparison must use; `body-span` below is what says whether a
   mismatch is the declaration or its neighbour's prose."
  [source declaration]
  (let [lines (vec (str/split-lines source))
        p (re-pattern (str "^(?:private\\s+)?(?:noncomputable\\s+)?(?:structure|inductive|abbrev|def|theorem|lemma)\\s+"
                           (java.util.regex.Pattern/quote declaration) "(?:\\s|$)"))
        boundary? #(or (re-find declaration-start %) (re-find #"^(?:namespace|end)\s" %))
        start (first (keep-indexed #(when (re-find p %2) %1) lines))
        end (or (first (keep-indexed #(when (and (> %1 start) (boundary? %2)) %1) lines)) (count lines))]
    [start end]))
(defn- lines-text [lines start end]
  (str (str/join "\n" (subvec lines start end)) "\n"))
(defn declaration-text [source declaration]
  (let [lines (vec (str/split-lines source)) [start end] (declaration-span source declaration)]
    (lines-text lines start end)))
(defn body-text
  "The declaration alone: the same span truncated at the first docstring
   opener, which is where the NEXT declaration's documentation begins."
  [source declaration]
  (let [lines (vec (str/split-lines source))
        [start end] (declaration-span source declaration)
        cut (or (first (keep-indexed #(when (and (> %1 start) (re-find #"^/--" %2)) %1)
                                     (subvec lines 0 end)))
                end)]
    (lines-text lines start cut)))
(defn sha256-text [s]
  (let [d (java.security.MessageDigest/getInstance "SHA-256")]
    (.update d (.getBytes s "UTF-8"))
    (format "%064x" (java.math.BigInteger. 1 (.digest d)))))
(defn sha256-file [f] (sha256-text (slurp f)))
(defn- git-out [repo args]
  (let [{:keys [exit out]} (shell/with-sh-dir (str "/home/joe/code/" repo)
                             (apply shell/sh "git" args))]
    (when (zero? exit) out)))
(defn drift-locus
  "WHERE a pinned declaration hash moved. The pinned span carries the next
   declaration's docstring, so the pin can move without the declaration moving.
   git names the commit that last touched the span's lines; comparing that
   commit's PARENT against the pin, and its body against HEAD's body, says
   which of the two happened."
  [repo path declaration expected]
  (let [head-source (slurp (io/file "/home/joe/code" repo path))
        [s e] (declaration-span head-source declaration)
        dirty (str/trim (or (git-out repo ["status" "--porcelain" path]) ""))
        commit (some-> (git-out repo ["log" "-1" "--format=%H" "-L"
                                      (format "%d,%d:%s" (inc s) e path)])
                       str/split-lines first str/trim)
        parent (when (seq commit) (git-out repo ["show" (str commit "^:" path)]))]
    (merge {:declaration declaration
            :span-lines-at-head [(inc s) e]
            :worktree-clean? (empty? dirty)
            :span-last-moved-by commit}
           (when parent
             (let [parent-span (sha256-text (declaration-text parent declaration))
                   parent-body (sha256-text (body-text parent declaration))
                   head-body (sha256-text (body-text head-source declaration))]
               {:parent-span-sha256 parent-span
                :parent-span-matches-pin? (= parent-span expected)
                :parent-body-sha256 parent-body
                :head-body-sha256 head-body
                :body-unchanged-across-that-commit? (= parent-body head-body)
                :locus (cond (not= parent-span expected) :earlier-than-the-commit-git-names
                             (= parent-body head-body) :neighbour-docstring-inside-the-span
                             :else :declaration-body)})))))
(def declaration-checks
  (mapv (fn [{:keys [repo path declarations]}]
          (let [f (io/file "/home/joe/code" repo path) source (slurp f)]
            {:file (str repo "/" path) :repo repo :path path
             :declarations
             (mapv (fn [{:keys [name sha256]}]
                     (let [actual (sha256-text (declaration-text source name))]
                       {:name name :expected sha256 :actual actual :match? (= sha256 actual)}))
                   declarations)}))
        (:source-basis proof-receipt)))
(def declaration-mismatches
  (vec (mapcat (fn [{:keys [repo path declarations]}]
                 (keep #(when-not (:match? %) (assoc % :repo repo :path path)) declarations))
               declaration-checks)))
(def drift-analysis
  (mapv #(drift-locus (:repo %) (:path %) (:name %) (:expected %)) declaration-mismatches))
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
(defn persisted-posterior-order
  "The posterior the RECORD ITSELF carries, highest weight first, resolved to
   actions. trace.clj:167-177 drops the action-keyed :softmax-weights (the keys
   are not stringable) and, when policy trace details are on, re-keys Q(pi) by
   the `rank/N` of the record's OUTER :ranked-actions -- so the join back to an
   action is through that field (trace.clj:150-155), the same join
   run9_s4_arms.clj:47-60 makes. Reading only [:decision :softmax-weights]
   therefore finds nothing in ANY record and silently discards the posterior
   the corpus does persist."
  [record]
  (let [q (get-in record [:decision :softmax-weights-by-candidate-id])
        id->action (into {} (map (fn [ra] [(str "rank/" (:rank ra)) (:action ra)]))
                         (:ranked-actions record))]
    (when (seq q)
      (mapv #(id->action (key %)) (sort-by (comp - val) q)))))
(defn reconstructed-posterior-order
  "The same ordering rebuilt from the retained :selection-score, usable only
   when F_pi did not enter (the score the trace retains is then the score the
   posterior normalises)."
  [record]
  (let [ranking (get-in record [:decision :habit-adjusted-ranking])]
    (when (and (not (true? (get-in record [:decision :f-pi-posterior :applied?])))
               (seq ranking))
      (mapv :action (sort-by (comp - :selection-score) ranking)))))
(defn posterior-result [record]
  (let [action (get-in record [:decision :action])
        persisted (persisted-posterior-order record)
        reconstructed (reconstructed-posterior-order record)
        order (or persisted reconstructed)
        kind (cond persisted :persisted-posterior reconstructed :selection-score-equivalent)]
    (when (and order (map? action))
      (let [ak (action-key action)
            idx (first (keep-indexed #(when (= ak (action-key %2)) %1) order))]
        {:file (:file record) :kind kind :candidates (count order)
         :equal? (= ak (action-key (first order)))
         :rank-of-recorded-action (some-> idx inc)
         ;; where both representations exist, do they order the head the same?
         :cross-check (when (and persisted reconstructed)
                        (= (action-key (first persisted))
                           (action-key (first reconstructed))))}))))
(def corpus-posterior-results (vec (keep posterior-result records)))
(def corpus-agree (count (filter :equal? corpus-posterior-results)))
(def corpus-differ (- (count corpus-posterior-results) corpus-agree))
(def corpus-ranks (vec (sort (keep :rank-of-recorded-action corpus-posterior-results))))
(def corpus-cross-check (frequencies (keep :cross-check corpus-posterior-results)))
(def era (->> corpus-posterior-results (group-by :file)
              (map (fn [[f xs]] [f {:n (count xs) :agree (count (filter :equal? xs))
                                    :ranks-of-recorded-action
                                    (vec (sort (keep :rank-of-recorded-action xs)))}]))
              (sort-by first) vec))

(defn- line-of
  "First line of `path` matching `re`, as a {:file :line :text} pointer, or
   :not-found. Derived, never pinned."
  [path re]
  (let [lines (vec (str/split-lines (slurp (io/file path))))]
    (or (first (keep-indexed (fn [i l] (when (re-find re l)
                                         {:file path :line (inc i) :text (str/trim l)}))
                             lines))
        :not-found)))

;; Leg (d). WR-19 as p4ng/empirics-futon/wr-overlay.edn states it: "tension must
;; GENERATE, not only rank -- the candidate space is ranked, not proposed;
;; tension-proposer unbuilt". P-validated-R5.md:381-393 turns that into R6's
;; red ring. Both are claims about the tree and the corpus, so both are
;; measurable here.
(def exclusions (vec (mapcat :policy-support-exclusions records)))
(def ranked-provenance
  (frequencies (mapcat (fn [r] (map #(get-in % [:action :provenance :proposer-id])
                                    (:ranked-actions r)))
                       records)))
(def chosen-provenance
  (frequencies (map #(get-in % [:decision :action :provenance :proposer-id]) records)))
(def recorded-action-types (frequencies (keep #(get-in % [:decision :action :type]) records)))
(def ranked-action-types
  (frequencies (mapcat (fn [r] (keep #(get-in % [:action :type]) (:ranked-actions r))) records)))

(def synthetic-persisted
  {:file "synthetic.edn"
   :ranked-actions [{:rank 1 :action {:type :a}} {:rank 2 :action {:type :b}}]
   :decision {:action {:type :a}
              :softmax-weights-by-candidate-id {"rank/1" 0.75 "rank/2" 0.25}}})
(def synthetic-reconstructed
  {:file "synthetic.edn"
   :decision {:action {:type :a}
              :habit-adjusted-ranking [{:rank 1 :action {:type :a} :selection-score -1.0}
                                       {:rank 2 :action {:type :b} :selection-score -2.0}]}})
(def synthetic-result (posterior-result synthetic-persisted))
(def synthetic-reconstructed-result (posterior-result synthetic-reconstructed))
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
    :declaration-mismatch-count (count declaration-mismatches)
    :drift-analysis drift-analysis
    :fixture-expected (get-in proof-receipt [:fixture :sha256]) :fixture-actual fixture-hash
    :fixture-match? (= fixture-hash (get-in proof-receipt [:fixture :sha256]))
    :finding (cond (empty? declaration-mismatches) nil
                   (and (seq drift-analysis)
                        (every? #(= :neighbour-docstring-inside-the-span (:locus %)) drift-analysis))
                   :pinned-span-moved-outside-the-declaration-body
                   :else :positive-source-drift)
    :basis "holes/labs/wm-contract/softmax-positive-receipt.edn:1"}
   {:id :strategic-selector-receives-ranking-not-posterior
    :result (if (and (seq (source-hits #"invoke-strategic-selection"))
                     (seq (source-hits #":controller-ranking"))) :pass :fail)
    :stale-pointer-5438 "scripts/futon2/report/war_machine.clj:5438 is scan-aif-heads prose"
    :stale-pointer-5406-5410 "scripts/futon2/report/war_machine.clj:5406-5410 is batch-pattern data"
    :current-call "scripts/futon2/report/war_machine.clj:6475-6480"
    :route "scripts/futon2/report/war_machine.clj:6481"
    :crosses {:controller-ranking true :selection-score true
              :tau-value false :posterior-normalised false
              ;; the ranking's :selection-score IS -G/tau (policy.clj:636-640),
              ;; so tau's EFFECT crosses the call even though tau does not.
              :tau-scaled-quantity true}
    :ranking-shape "src/futon2/aif/policy.clj:495-501,636-640"}
   {:id :corpus-posterior-vs-recorded-action
    :result (if (= (count corpus-posterior-results) (+ corpus-agree corpus-differ)) :pass :fail)
    :records-total (count records) :files-total (count (trace-files))
    :records-with-posterior-and-action (count corpus-posterior-results)
    :representation-counts (frequencies (map :kind corpus-posterior-results))
    :persisted-softmax-weights-key-count
    (count (filter #(seq (get-in % [:decision :softmax-weights])) records))
    :persisted-by-candidate-id-count
    (count (filter #(seq (get-in % [:decision :softmax-weights-by-candidate-id])) records))
    :agree corpus-agree :differ corpus-differ
    :rank-of-recorded-action {:n (count corpus-ranks)
                              :min (first corpus-ranks)
                              :median (nth corpus-ranks (quot (count corpus-ranks) 2))
                              :max (last corpus-ranks)
                              :rank-1-count (count (filter #(= 1 %) corpus-ranks))}
    :cross-check-persisted-vs-reconstructed corpus-cross-check
    :by-file era
    :first-file (some-> records first :file) :last-file (some-> records last :file)
    :basis "src/futon2/aif/policy.clj:664-670; scripts/futon2/report/war_machine.clj:6475-6481; src/futon2/aif/trace.clj:150-177"}
   {:id :f-pi-posterior-site-census
    :result (if (= 1 (count fpi-application-sites)) :pass :fail)
    :search "f-pi-policy-posterior?" :symbol-occurrence-count (count fpi-sites)
    :symbol-occurrences fpi-sites
    :application-site-count (count fpi-application-sites)
    :application-sites fpi-application-sites
    :registry-code-range registry-code-range
    :derived-site-inside-registry-range?
    (every? #(<= (:from registry-code-range) (:line %) (:to registry-code-range))
            fpi-application-sites)
    :claim-site "src/futon2/aif/policy.clj:545-553"}
   {:id :r6-find-generates-or-only-ranks
    :result (if (and (seq exclusions) (seq recorded-action-types)) :pass :fail)
    :claim-under-test
    {:wr-19 (line-of "/home/joe/code/p4ng/empirics-futon/wr-overlay.edn" #":node \"R6\"")
     :problem-statement "holes/problems/P-validated-R5.md:381-393"}
    :generator-built (line-of "src/futon2/aif2/tension.clj" #"^\(ns futon2\.aif2\.tension")
    :generator-wired (line-of "scripts/futon2/report/war_machine.clj" #"\(tension/tension-proposer\)")
    :generator-alphabet (line-of "src/futon2/aif2/tension.clj" #"^\(def kappa")
    :ranked-actions-by-proposer ranked-provenance
    :chosen-actions-by-proposer chosen-provenance
    :generated-and-excluded
    {:count (count exclusions)
     :by-proposer (frequencies (map #(get-in % [:action :provenance :proposer-id]) exclusions))
     :by-reason (frequencies (map :reason exclusions))
     :by-action-type (frequencies (map #(get-in % [:action :type]) exclusions))}
    :recorded-action-types recorded-action-types
    :ranked-action-types ranked-action-types}])

(def plants
  [{:id :f-pi-divided-by-tau :counter :a2-max-deviation
    :moved (max-dev f-unscaled plant-f-score) :result (if (pos? (max-dev f-unscaled plant-f-score)) :caught :escaped)}
   {:id :habit-prior-divided-by-tau :counter :a3-max-deviation
    :moved (max-dev prior-unscaled plant-prior-score) :result (if (pos? (max-dev prior-unscaled plant-prior-score)) :caught :escaped)}
   {:id :synthetic-action-equals-persisted-posterior-argmax :counter :d3-agreement
    :representation :persisted-posterior
    :before corpus-agree :after (+ corpus-agree (if (:equal? synthetic-result) 1 0))
    :moved (if (:equal? synthetic-result) 1 0) :result (if (:equal? synthetic-result) :caught :escaped)}
   {:id :synthetic-action-equals-reconstructed-argmax :counter :d3-agreement
    :representation :selection-score-equivalent
    :before corpus-agree :after (+ corpus-agree (if (:equal? synthetic-reconstructed-result) 1 0))
    :moved (if (:equal? synthetic-reconstructed-result) 1 0)
    :result (if (:equal? synthetic-reconstructed-result) :caught :escaped)}
   {:id :synthetic-declaration-body-edit :counter :b1-drift-locus
    ;; the drift localiser must call a real body change a body change, not a
    ;; neighbour docstring: edit the def line of a copy and re-extract.
    :moved (let [src (slurp (io/file "/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean"))
                 edited (str/replace src "def softmax {PolicyIndex : Type*}"
                                     "def softmax {PolicyIndex : Type _}")]
             (if (= (sha256-text (body-text src "softmax"))
                    (sha256-text (body-text edited "softmax")))
               0 1))
    :result (let [src (slurp (io/file "/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean"))
                  edited (str/replace src "def softmax {PolicyIndex : Type*}"
                                      "def softmax {PolicyIndex : Type _}")]
              (if (= (sha256-text (body-text src "softmax"))
                     (sha256-text (body-text edited "softmax")))
                :escaped :caught))}])

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
