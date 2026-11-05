(ns futon2.aif.refusal-site-index
  "Executable inventory of production War Machine refusal decisions."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def resource "wm/refusal-sites.edn")
(def terminal-kind-resource "wm/terminal-kind-classification.edn")
(def schema :wm/refusal-site-index-v1)
(def cases #{:L1 :L2 :C1 :C2 :C3 :debugger-error})
(def evidence-fields
  #{:scoredField :operatorSignal :satisfactionMeasured :ambiguityCited
    :questionInBrief :harmRuleMatched :exclusionRecorded})
(def evidence-values #{:present :derivable :absent})
(def loop-contract "DarkTower.WarMachine.RefusalLegality.loopStopLegal")
(def required-clojure-site-ids
  #{:selection-empty-field :selection-refusal-terminal :phase-result-terminal
    :phase-breakpoint-operator-abort :agency-job-cancelled
    :author-already-satisfied :author-guardrail-refusal :author-invalid-refusal
    :author-unmeasured-satisfaction :author-correction-refused
    :duplicate-effective-run :reading-refusal-reselection-exhausted
    :open-cascade-author-answer :open-cascade-review-answer
    :close-dynamic-outcome :close-fallback :terminal-route-missing
    :outer-run-catch})

(defn read-index []
  (-> resource io/resource slurp edn/read-string))

(defn- read-terminal-kinds []
  (-> terminal-kind-resource io/resource slurp edn/read-string keys set))

(defn- occurrences [text needle]
  (if (str/blank? needle)
    0
    (loop [from 0, n 0]
      (let [at (.indexOf ^String text ^String needle (int from))]
        (if (neg? at) n (recur (+ at (count needle)) (inc n)))))))

(defn- supervisor-row? [{:keys [source]}]
  (contains? #{"scripts/wm_click_repair_loop.py" "scripts/wm_click_loop.sh"}
             source))

(defn audit-index
  "Audit INDEX against production sources rooted at REPO."
  [repo {:keys [schema sites]}]
  (let [known-kinds (read-terminal-kinds)
        ids (map :id sites)
        id-set (set ids)
        python-source "scripts/wm_click_repair_loop.py"
        python-text (slurp (io/file repo python-source))
        python-stop-count (count (filter #(str/includes? % "return stop(args")
                                         (str/split-lines python-text)))
        indexed-python-count (count (filter #(= python-source (:source %)) sites))
        problems
        (concat
         (when-not (= schema futon2.aif.refusal-site-index/schema)
           [{:reason :schema-mismatch :actual schema}])
         (when-not (= (count ids) (count (distinct ids)))
           [{:reason :duplicate-site-id}])
         (when-not (= python-stop-count indexed-python-count)
           [{:reason :python-stop-coverage-mismatch
             :produced python-stop-count :indexed indexed-python-count}])
         (when-let [missing (seq (remove id-set required-clojure-site-ids))]
           [{:reason :clojure-refusal-constructor-coverage-mismatch
             :missing (vec (sort missing))}])
         (mapcat
          (fn [{:keys [id source anchor kinds case evidence lean-contract] :as site}]
            (let [file (io/file repo source)
                  text (when (.isFile file) (slurp file))
                  anchor-count (when text (occurrences text anchor))]
              (cond-> []
                (not (.isFile file))
                (conj {:site id :reason :source-missing :source source})
                (and text (zero? anchor-count))
                (conj {:site id :reason :anchor-missing :source source :anchor anchor})
                (and text (> anchor-count 1))
                (conj {:site id :reason :anchor-not-unique :source source
                       :anchor anchor :count anchor-count})
                (not (contains? cases case))
                (conj {:site id :reason :case-invalid :case case})
                (not (and (map? evidence)
                          (= evidence-fields (set (keys evidence)))
                          (every? evidence-values (vals evidence))))
                (conj {:site id :reason :evidence-invalid :evidence evidence})
                (and (contains? #{:L1 :L2} case) (not= loop-contract lean-contract))
                (conj {:site id :reason :lean-contract-missing})
                (not (and (vector? kinds) (seq kinds) (every? keyword? kinds)))
                (conj {:site id :reason :kinds-invalid :kinds kinds})
                (and (not (supervisor-row? site))
                     (seq kinds)
                     (not-every? known-kinds kinds))
                (conj {:site id :reason :terminal-kind-unclassified
                       :kinds (vec (remove known-kinds kinds))}))))
          sites))]
    {:schema :wm/refusal-site-audit-v1
     :status (if (seq problems) :refused :verified)
     :site-count (count sites)
     :problems (vec problems)}))

(defn audit [repo]
  (audit-index repo (read-index)))
