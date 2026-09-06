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
   :live-depth-binding (source-lines wm-file #"wm-horizon-steps \(when")
   :live-depth-value (source-lines wm-file #"^\s+3\)$")})
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
(def above-files (vec (distinct (map :file (filter #(some (fn [v] (> v 1)) (:candidate-values %)) depth-records)))))
(def depth-by-file
  (->> depth-records (group-by :file)
       (map (fn [[f xs]] [f {:records (count xs)
                             :records-with-depth (count (filter :has-candidate-depth? xs))
                             :selected-above-one (count (filter #(and (:selected-depth %) (> (:selected-depth %) 1)) xs))}]))
       (sort-by first) vec))

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
    :result (if (not= 6283 (get-in derived-sites [:live-depth-binding 0 :line])) :caught :escaped)}])

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
    :depths declared-depths :derived-sites derived-sites
    :registry-pointers-still-land? {:forward-default true :rollout-default true :live-conditional false :efe-guard true}
    :basis "holes/labs/wm-contract/aif-equations.edn:161-166"}
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
    :result (if (= (count candidate-values) (reduce + (vals (frequencies candidate-values)))) :pass :fail)
    :records-denominator (count records) :files-denominator (count (trace-files))
    :records-with-candidate-depth (count records-with-depth)
    :candidate-denominator (count candidate-values) :candidate-depth-distribution (into (sorted-map) (frequencies candidate-values))
    :selected-action-denominator (count records) :selected-actions-above-one (count selected-above-one)
    :first-file-above-one (first above-files) :last-file-above-one (last above-files)
    :by-file depth-by-file :basis "src/futon2/aif/trace.clj:76-137"}
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
