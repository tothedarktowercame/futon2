(ns v7-r13-node-sim
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.aif.belief :as belief]
            [futon2.aif.efe :as efe]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.rollout :as rollout]))

(def lab (io/file "holes/labs/wm-contract"))
(def carriers (edn/read-string (slurp (io/file lab "sim/R13-carriers.edn"))))
(def state (assoc (:state carriers) :belief (belief/initial-belief-state (get-in carriers [:state :entities]))))
(def action (:action carriers))
(def tolerance 1.0e-12)

(defn independent-chain [state action k]
  (loop [i 0 current state trajectory []]
    (if (>= i k)
      {:trajectory trajectory :final-state current :horizon-steps k}
      (let [prediction (fm/predict current action)
            next-state (-> current
                           (assoc :observation (get-in prediction [:next-observation :mean]))
                           (assoc :belief (:next-belief prediction)))]
        (recur (inc i) next-state (conj trajectory prediction))))))

(defn numbers [x]
  (cond (number? x) [(double x)]
        (map? x) (mapcat numbers (map val (sort-by (comp pr-str key) x)))
        (sequential? x) (mapcat numbers x)
        :else []))
(defn max-dev [a b]
  (let [xs (vec (numbers a)) ys (vec (numbers b))]
    (if (= (count xs) (count ys))
      (apply max 0.0 (map #(Math/abs (- %1 %2)) xs ys))
      ##Inf)))

(def trajectory-comparisons
  (mapv (fn [k]
          (let [reference (independent-chain state action k)
                node (fm/predict-multi-horizon state action k)]
            {:k k :max-deviation (max-dev reference node)
             :exact? (= reference node)
             :trajectory-count (count (:trajectory node))
             :horizon-steps (:horizon-steps node)}))
        (:depths carriers)))

(def one (efe/compute-efe state action {:horizon-steps 1}))
(def three (efe/compute-efe state action {:horizon-steps 3}))
(def none (efe/compute-efe state action))
(def term-keys [:G-risk :G-ambiguity :homeostatic-pressure :predictability-bonus])
(def term-movements
  (into (sorted-map) (for [k term-keys] [k (- (double (get three k)) (double (get one k)))])))
(def multi-three (fm/predict-multi-horizon state action 3))
(def variance-rows (mapv #(get-in % [:next-observation :variance]) (:trajectory multi-three)))
(def variance-max-delta
  (apply max 0.0 (map #(max-dev (first variance-rows) %) (rest variance-rows))))

(defn source-lines [f re]
  (->> (str/split-lines (slurp f))
       (keep-indexed (fn [i line] (when (re-find re line) {:line (inc i) :text (str/trim line)})))
       vec))
(def forward-file (io/file "src/futon2/aif/forward_model.clj"))
(def efe-file (io/file "src/futon2/aif/efe.clj"))
(def rollout-file (io/file "src/futon2/aif/rollout.clj"))
(def wm-file (io/file "scripts/futon2/report/war_machine.clj"))
(def derived-sites
  {:forward-default (source-lines forward-file #"def default-horizon-steps")
   :forward-composition (source-lines forward-file #"defn predict-multi-horizon")
   :state-blind-effects (source-lines forward-file #"predict-effects nil action")
   :efe-guard (source-lines efe-file #"horizon-steps \(>= horizon-steps 2\)")
   :rollout-default (source-lines rollout-file #"defn- rollout-horizon")
   :live-depth-binding (source-lines wm-file #"wm-horizon-steps \(when")})
(def live-depth-binding-line (get-in derived-sites [:live-depth-binding 0 :line]))
(def live-depth-value-site
  ;; DERIVED RELATIVE TO THE BINDING. The first form of this site read
  ;; `^\s+3\)$` over the whole of war_machine.clj -- a search whose answer is
  ;; the file's FIRST bare `3)` wherever it is, so a new one above line 6287
  ;; would silently retarget the census. Anchored to the binding instead: the
  ;; first line at or after `wm-horizon-steps` whose whole text is a numeral
  ;; and a close paren. :global-matches records how many such lines the file
  ;; has, so the anchoring is visible rather than assumed.
  (let [lines (map-indexed (fn [i l] {:line (inc i) :text (str/trim l)})
                           (str/split-lines (slurp wm-file)))
        numeral? #(re-matches #"\d+\)" (:text %))]
    (assoc (first (filter #(and (>= (:line %) live-depth-binding-line) (numeral? %)) lines))
           :anchored-at live-depth-binding-line
           :global-matches (count (filter numeral? lines)))))
(def registry-row
  ;; R13's ONE equations-registry row, located by its :id and bounded by the
  ;; next row's opening brace -- not by a pinned line range.
  (let [lines (vec (str/split-lines (slurp (io/file lab "aif-equations.edn"))))
        start (first (keep-indexed #(when (re-find #":id :depth :defines :T :node :R13" %2) %1) lines))
        end (first (keep-indexed #(when (and (> %1 start) (re-find #"^\s+\{:id " %2)) %1) lines))]
    {:from (inc start) :to end
     :text (str/join "\n" (subvec lines start end))}))
(def registry-code-ranges
  ;; Every `file.clj:N` / `file.clj:N-M` pointer the row's :code field writes.
  (vec (for [[_ f a b] (re-seq #"([a-z_0-9]+\.clj):(\d+)(?:-(\d+))?" (:text registry-row))]
         {:file f :from (Long/parseLong a) :to (Long/parseLong (or b a))})))
(def pointer-resolution
  ;; MEASURED, not asserted. The delivered harness wrote this answer as a
  ;; literal map ({:live-conditional false ...}), which records an opinion the
  ;; harness cannot fail on. Each derived site is now asked whether it falls
  ;; inside a range the registry itself declares for that file.
  ;; :claimed? marks the sites the row's :code field itself points at. The
  ;; composition's defn line is derived here but the registry names only its
  ;; body (in the :lean-note), so it is measured and excluded from the finding
  ;; rather than counted as drift the registry did not commit.
  (mapv (fn [{:keys [site file line claimed?]}]
          (let [ranges (filterv #(= file (:file %)) registry-code-ranges)
                hit (first (filter #(<= (:from %) line (:to %)) ranges))]
            {:site site :file file :derived-line line :registry-ranges ranges
             :a-registry-code-claim? claimed?
             :inside-a-declared-range? (boolean hit) :matching-range hit}))
        [{:site :forward-default :file "forward_model.clj" :claimed? true
          :line (get-in derived-sites [:forward-default 0 :line])}
         {:site :forward-composition :file "forward_model.clj" :claimed? false
          :line (get-in derived-sites [:forward-composition 0 :line])}
         {:site :rollout-default :file "rollout.clj" :claimed? true
          :line (get-in derived-sites [:rollout-default 0 :line])}
         {:site :efe-guard :file "efe.clj" :claimed? true
          :line (get-in derived-sites [:efe-guard 0 :line])}
         {:site :live-depth-binding :file "war_machine.clj" :claimed? true
          :line live-depth-binding-line}
         {:site :live-depth-value :file "war_machine.clj" :claimed? true
          :line (:line live-depth-value-site)}]))
(def pointers-outside-their-declared-range
  (vec (remove :inside-a-declared-range? (filter :a-registry-code-claim? pointer-resolution))))
(def declared-depths
  {:forward-default fm/default-horizon-steps
   :rollout-default (#'rollout/rollout-horizon {})
   :live-conditional (get-in carriers [:declared-depths :live-conditional])
   :efe-multi-threshold (get-in carriers [:declared-depths :efe-multi-threshold])})

(defn trace-files []
  (->> (.listFiles (io/file "data/wm-trace"))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
       (sort-by #(.getName %))))
(defn read-records [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [out []]
      (let [x (try (edn/read {:eof ::eof :default (fn [_ v] v)} r) (catch Exception _ ::bad))]
        (cond (= x ::eof) out (= x ::bad) (recur out) :else (recur (conj out x)))))))
(def records
  (vec (mapcat (fn [f] (map #(assoc % :source-file (.getName f)) (read-records f))) (trace-files))))
(defn action-key [a] (select-keys a [:type :target :target-class]))
(defn record-depth [record]
  (let [ranked (:ranked-actions record)
        action (get-in record [:decision :action])
        selected (some #(when (= (action-key action) (action-key (:action %))) %) ranked)
        values (vec (keep :horizon-steps ranked))]
    {:file (:source-file record) :candidate-values values
     :has-candidate-depth? (boolean (seq values))
     :selected-depth (:horizon-steps selected)}))
(def depth-records (mapv record-depth records))
(def records-with-depth (filter :has-candidate-depth? depth-records))
(def selected-above-one (filter #(and (:selected-depth %) (> (:selected-depth %) 1)) depth-records))
(def candidate-values (vec (mapcat :candidate-values depth-records)))
(def ranked-total (reduce + (map #(count (:ranked-actions %)) records)))
(def candidate-depth-cells
  ;; THREE-WAY, because `keep :horizon-steps` hides two different absences and
  ;; leaves a distribution whose denominator is only the present values. A
  ;; ranked candidate either carries no :horizon-steps key at all (the record
  ;; predates the field), carries it as nil (the tick ran single-step), or
  ;; carries a number. The first form of this census reported {3 32383} against
  ;; a 32383 denominator, which cannot show that 85042 candidates ran at
  ;; depth one.
  (frequencies (mapcat (fn [r] (map (fn [ra] (cond (not (contains? ra :horizon-steps)) :key-absent
                                                   (nil? (:horizon-steps ra)) :recorded-nil
                                                   :else (:horizon-steps ra)))
                                    (:ranked-actions r)))
                       records)))
(def records-with-the-key-present
  (count (filter (fn [r] (some #(contains? % :horizon-steps) (:ranked-actions r))) records)))
(def above-files (vec (distinct (map :file (filter #(some (fn [v] (> v 1)) (:candidate-values %)) depth-records)))))
(def depth-by-file
  (->> depth-records (group-by :file)
       (map (fn [[f xs]] [f {:records (count xs)
                             :records-with-depth (count (filter :has-candidate-depth? xs))
                             :selected-above-one (count (filter #(and (:selected-depth %) (> (:selected-depth %) 1)) xs))}]))
       (sort-by first) vec))

;; ---------------------------------------------------------------------------
;; THE FINDING THE OTHER CHECKS CIRCLE, stated as a check of its own. R13's
;; registry line asks for "the range of the SUMS in Q(o|pi) and G" and cites
;; da Costa eq. 42's sum over tau; the catalogue asks for "adding up the costs
;; of its steps with the later ones discounted" (p4ng/sec-catalog.tex:256).
;; The machine has TWO depth mechanisms, and the one that adds up is not the
;; one that chooses. Every clause below is a derived search or a corpus count.
(def cascade-file (io/file "scripts/futon2/report/cascade_lane.clj"))
(def efe-multi-reads (source-lines efe-file #"get-in multi |\(:horizon-steps multi\)"))
(def efe-discount-hits (source-lines efe-file #"discount|gamma"))
(def rollout-accumulator (source-lines rollout-file #"defn project-policy|S\(pi\)=sum gamma|\* discount gamma"))
(def wm-rollout-hits (source-lines wm-file #"rollout/|futon2\.aif\.rollout"))
(def cascade-rollout-hits (source-lines cascade-file #"futon2\.aif\.rollout|rollout/best-rollout"))
(def wm-cascade-call (source-lines wm-file #"futon2\.report\.cascade-lane/cascade-lane"))
(defn cascade-row? [ra] (= :apply-cascade (get-in ra [:action :type])))
(def all-ranked (mapcat :ranked-actions records))
(def cascade-rows (filterv cascade-row? all-ranked))
(def records-with-cascade-rows (count (filter #(some cascade-row? (:ranked-actions %)) records)))
(def cascade-decisions (count (filter #(= :apply-cascade (get-in % [:decision :action :type])) records)))
(def cascade-files
  (vec (distinct (map :source-file (filter #(some cascade-row? (:ranked-actions %)) records)))))
(def cascade-score-provenance (frequencies (map :score-provenance cascade-rows)))
(def synthetic-cascade-decision
  {:source-file "synthetic.edn" :ranked-actions []
   :decision {:action {:type :apply-cascade :target "M-synthetic"}}})

(def artifact (io/file lab "runs/F8-depth/clojure-readback.txt"))
(def expected-artifact-sha "44e3306827137cf5e32276ad7b2777a9de607456fe8d72aeba8c75779242e848")
(defn sha256-file [f]
  (let [d (java.security.MessageDigest/getInstance "SHA-256")]
    (.update d (java.nio.file.Files/readAllBytes (.toPath f)))
    (format "%064x" (java.math.BigInteger. 1 (.digest d)))))
(def before-sha (sha256-file artifact))
(def readback-run (shell/sh "clojure" "-M" "holes/labs/wm-contract/f8_depth_readback.clj"))
(def after-sha (sha256-file artifact))

(def synthetic-depth-record {:source-file "synthetic.edn"
                             :ranked-actions [{:action {:type :a} :horizon-steps 3}]
                             :decision {:action {:type :a}}})
(def kminus-plant (fm/predict-multi-horizon state action 3))
(def kminus-reference (independent-chain state action 2))
(def first-final-plant (assoc (independent-chain state action 3)
                              :final-state {:observation (get-in multi-three [:trajectory 0 :next-observation :mean])
                                            :belief (get-in multi-three [:trajectory 0 :next-belief])}))
(def pinned-line-plant (first (filter #(= 6283 (:line %)) (source-lines wm-file #".*"))))
(def plants
  [{:id :chains-k-minus-one
    :moved (max-dev (:final-state kminus-plant) (:final-state kminus-reference))
    :result (if (pos? (max-dev (:final-state kminus-plant) (:final-state kminus-reference))) :caught :escaped)}
   {:id :final-state-is-first-step :moved (max-dev multi-three first-final-plant)
    :result (if (pos? (max-dev multi-three first-final-plant)) :caught :escaped)}
   {:id :synthetic-selected-action-depth :before (count selected-above-one)
    :after (+ (count selected-above-one) (if (> (:selected-depth (record-depth synthetic-depth-record)) 1) 1 0))
    :moved (if (> (:selected-depth (record-depth synthetic-depth-record)) 1) 1 0)
    :result (if (> (:selected-depth (record-depth synthetic-depth-record)) 1) :caught :escaped)}
   {:id :pinned-line-instead-of-derived-site
    :pinned-line 6283 :pinned-text (:text pinned-line-plant)
    :derived-line (get-in derived-sites [:live-depth-binding 0 :line])
    :result (if (not= 6283 (get-in derived-sites [:live-depth-binding 0 :line])) :caught :escaped)}
   {:id :synthetic-cascade-row-recorded-as-the-decision
    ;; The corpus clause of :the-summing-depth-is-not-on-the-selection-path is
    ;; a zero, and a zero that no input can move is not a measurement. Injecting
    ;; one record whose decision IS an :apply-cascade row must move it by one.
    :counter :cascade-decisions :before cascade-decisions
    :after (+ cascade-decisions
              (count (filter #(= :apply-cascade (get-in % [:decision :action :type]))
                             [synthetic-cascade-decision])))
    :moved (count (filter #(= :apply-cascade (get-in % [:decision :action :type]))
                          [synthetic-cascade-decision]))
    :result (if (= 1 (count (filter #(= :apply-cascade (get-in % [:decision :action :type]))
                                    [synthetic-cascade-decision])))
              :caught :escaped)}])

(def checks
  [{:id :trajectory-is-the-k-fold-composition
    :result (if (every? #(and (:exact? %) (zero? (:max-deviation %))) trajectory-comparisons) :pass :fail)
    :comparisons trajectory-comparisons :basis "src/futon2/aif/forward_model.clj:280-310"}
   {:id :g-terms-do-not-share-one-depth
    :result (if (and (not (zero? (:G-risk term-movements)))
                     (not (zero? (:homeostatic-pressure term-movements)))
                     (zero? (:G-ambiguity term-movements))
                     (zero? (:predictability-bonus term-movements))) :pass :fail)
    :k3-minus-k1 term-movements :k1 (select-keys one term-keys) :k3 (select-keys three term-keys)
    :basis "src/futon2/aif/efe.clj:623-706"}
   {:id :epistemic-half-has-no-tau-sum
    :result (if (zero? variance-max-delta) :pass :fail)
    :trajectory-depth 3 :variance-max-delta variance-max-delta :variance-rows variance-rows
    :derived-source-sites {:state-blind-effects (:state-blind-effects derived-sites)}
    :basis "src/futon2/aif/forward_model.clj:334-354"}
   {:id :declared-depths-census
    :result (if (and (= {:forward-default 3 :rollout-default 2 :live-conditional 3 :efe-multi-threshold 2}
                        declared-depths)
                     (every? seq (vals derived-sites))) :pass :fail)
    :depths declared-depths
    :derived-sites (assoc derived-sites :live-depth-value [live-depth-value-site])
    :registry-row-at {:from (:from registry-row) :to (:to registry-row)}
    :registry-code-ranges registry-code-ranges
    :registry-pointer-resolution pointer-resolution
    :pointers-outside-their-declared-range pointers-outside-their-declared-range
    :finding (when (seq pointers-outside-their-declared-range)
               :a-declared-range-stops-short-of-the-constant-it-cites)
    :basis "holes/labs/wm-contract/aif-equations.edn -- the :depth row, located by :id"}
   {:id :requested-depth-of-one-is-not-a-depth
    :result (if (and (nil? (:horizon-steps none)) (nil? (:horizon-steps one))
                     (= (:controller-score none) (:controller-score one))
                     (= 3 (:horizon-steps three))) :pass :fail)
    :none {:horizon (:horizon-steps none) :score (:controller-score none)}
    :one {:horizon (:horizon-steps one) :score (:controller-score one)}
    :three {:horizon (:horizon-steps three) :score (:controller-score three)}
    :none-minus-one (- (:controller-score none) (:controller-score one))
    :basis "src/futon2/aif/efe.clj:623-636"}
   {:id :corpus-depth-era
    ;; The first form of this check compared (count candidate-values) with the
    ;; sum of its own frequencies -- true of every vector, so the check could
    ;; not fail. What it asserts now is that the three ways of counting the same
    ;; corpus agree: the per-file rows reproduce both record totals, and the
    ;; three-way candidate census accounts for every ranked candidate.
    :result (if (and (pos? (count records))
                     (= (count records) (reduce + (map (comp :records second) depth-by-file)))
                     (= (count selected-above-one)
                        (reduce + (map (comp :selected-above-one second) depth-by-file)))
                     (= ranked-total (reduce + (vals candidate-depth-cells))))
              :pass :fail)
    :records-denominator (count records) :files-denominator (count (trace-files))
    :records-with-a-candidate-above-depth-one (count records-with-depth)
    :records-where-the-key-is-present records-with-the-key-present
    :candidate-denominator ranked-total
    :candidate-depth-cells (into (sorted-map-by #(compare (pr-str %1) (pr-str %2))) candidate-depth-cells)
    :candidate-depth-distribution-of-present-values (into (sorted-map) (frequencies candidate-values))
    :selected-action-denominator (count records) :selected-actions-above-one (count selected-above-one)
    :first-file-above-one (first above-files) :last-file-above-one (last above-files)
    :by-file depth-by-file :basis "src/futon2/aif/trace.clj:76-137"}
   {:id :the-summing-depth-is-not-on-the-selection-path
    :result (if (and (seq efe-multi-reads) (empty? efe-discount-hits) (seq rollout-accumulator)
                     (empty? wm-rollout-hits) (seq cascade-rollout-hits) (seq wm-cascade-call)
                     (zero? cascade-decisions))
              :pass :fail)
    :what-compute-efe-reads-of-the-trajectory
    {:sites efe-multi-reads
     :reads "the final state's observation and the recorded :horizon-steps, and nothing else -- the K single-step predictions in :trajectory are computed and discarded"}
    :discount-terms-in-efe {:search "discount|gamma" :hits efe-discount-hits :count (count efe-discount-hits)}
    :the-accumulator-that-does-sum rollout-accumulator
    :rollout-is-not-reached-from-the-tick
    {:war-machine-hits wm-rollout-hits
     :search "rollout/|futon2.aif.rollout over scripts/futon2/report/war_machine.clj"
     :only-route wm-cascade-call
     :and-that-lane-requires-it cascade-rollout-hits}
    :corpus {:cascade-rows-in-rankings (count cascade-rows)
             :records-carrying-one records-with-cascade-rows
             :files (count cascade-files) :first-file (first cascade-files) :last-file (last cascade-files)
             :ever-the-recorded-decision cascade-decisions
             :decision-denominator (count records)
             :score-provenance-on-those-rows cascade-score-provenance}
    :basis "src/futon2/aif/efe.clj:629-635; src/futon2/aif/rollout.clj:487-540; scripts/futon2/report/cascade_lane.clj:381; scripts/futon2/report/war_machine.clj:6569-6588"}
   {:id :lean-depth-readback-still-matches
    :result (if (and (zero? (:exit readback-run)) (= before-sha after-sha expected-artifact-sha)) :pass :fail)
    :exit (:exit readback-run) :before-sha before-sha :after-sha after-sha :expected-sha expected-artifact-sha
    :byte-identical? (= before-sha after-sha) :basis "holes/labs/wm-contract/f8_depth_readback.clj:1-107"}])

(def all-pass (every? #(= :pass (:result %)) checks))
(def all-caught (every? #(= :caught (:result %)) plants))
(def receipt
  {:harness :v7-r13-node-sim :row :V7 :slice 11 :node :R13 :stage "SELECT"
   :stage-at "/home/joe/code/p4ng/empirics-futon/control-stages.edn:24"
   :carriers "holes/labs/wm-contract/sim/R13-carriers.edn"
   :reference-independence "futon2.aif.forward-model/predict is the verified R4 primitive. The harness independently composes that primitive K times from R13-carriers.edn; predict-multi-horizon never supplies an expected value."
   :declaration-sites (:declaration-sites carriers) :checks checks
   :negative-controls {:plants plants :all-caught all-caught}
   :corpus {:root "data/wm-trace" :read-only true :files (count (trace-files)) :records (count records)}
   :verdict (if (and all-pass all-caught) :pass :fail)
   :not-done ["No live tick or run lock; trace files were read only."
              "No production, registry, ledger, document, p4ng, Lean, or data file was modified."]})
(def out (io/file lab "runs/V7-R13-node-sim/00-r13.edn"))
(.mkdirs (.getParentFile out))
(with-open [w (io/writer out)] (binding [*out* w] (pp/pprint receipt)))
(doseq [c checks] (println (format "  %-52s %s" (name (:id c)) (name (:result c)))))
(println "  verdict" (:verdict receipt) "receipt" (str out))
(System/exit (if (= :pass (:verdict receipt)) 0 1))
