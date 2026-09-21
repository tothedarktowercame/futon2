(ns improve-2-normalization-probe
  "Read-only frozen-model arithmetic. No stores, fitting, scan or runner."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [checks.disposition-kernel :as kernel]
            [futon2.aif.disposition-risk :as risk]
            [futon2.aif.ruled-outcome-c :as ruled]
            [clojure.pprint :as pp]
            [clojure.set :as set]
            [futon2.aif.cascade-model-manifest :as model]))

(defn spread [xs] (- (apply max xs) (apply min xs)))
(defn close! [x y] (assert (< (Math/abs (- (double x) (double y))) 1e-8) [x y]))
(defn utility [w s] (reduce + 0 (map #(get w % 0) s)))
(defn log-sum-exp [xs]
  (let [m (apply max xs)] (+ m (Math/log (reduce + (map #(Math/exp (- % m)) xs))))))

(defn replay [suffix]
  (let [path (str "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-09-21-" suffix ".edn")
        raw (slurp path) r (edn/read-string raw)
        policies (get-in r [:decision :g-term-decomposition :policies])
        candidates (get-in r [:decision :selection-certificate :candidates])
        terms (:terms (first policies)) c (get-in terms [:C :value])
        terminal (:distribution (last (:steps c))) w (:weights terminal)
        v (:universe terminal) q0 (get-in terms [:D :value]) rates (get-in terms [:A :value])
        live (get-in r [:decision :selection-certificate :scoring 0 :c :weights-echo])
        provenance (get-in r [:decision :selection-certificate :scoring 0 :c :live-c])
        terminal-states (mapv #(-> % :terms :Q :value :steps last :belief keys first) policies)
        unique-states (set terminal-states)
        varying (set/difference (apply set/union terminal-states) (apply set/intersection terminal-states))
        total (reduce + (vals w)) live-total (reduce + (vals live))
        score (fn [weights universe]
                (mapv (fn [candidate]
                        (model/horizon-g-sparse
                         {:rates rates :q0 q0 :precedence-fn (constantly (:precedence (:id candidate)))
                          :horizon (count (:steps c)) :universe universe
                          :spec {:want (set (keys weights)) :weights weights :lam 1 :mu 0
                                 :evidence #{} :zeroed #{} :c-schedule (:schedule c)}})) candidates))
        original (score w v)
        _ (doseq [[computed candidate] (map vector original candidates)] (close! computed (:g candidate)))
        _ (assert (empty? (:zeroed terminal)))
        _ (assert (every? #(= c (get-in % [:terms :C :value])) policies))
        target-totals (reduce-kv (fn [m [target _] weight] (update m target (fnil + 0) weight)) {} w)
        variants {:all-consumed-weights-sum-one (into {} (map (fn [[t x]] [t (/ x total)])) w)
                  :projected-live-weights-sum-one-fallback-unchanged (merge w (into {} (map (fn [[t x]] [t (/ x live-total)])) live))
                  :each-target-weights-sum-one (into {} (map (fn [[[target _ :as t] x]] [t (/ x (target-totals target))])) w)}
        utilities (mapv #(double (utility w %)) terminal-states)
        restricted-log-z (log-sum-exp (map #(double (utility w %)) unique-states))
        restricted-risk (mapv #(- restricted-log-z %) utilities)
        varying-utilities (mapv #(double (utility w (set/intersection varying %))) terminal-states)
        _ (close! (spread original) (spread restricted-risk))
        _ (close! (spread original) (spread varying-utilities))]
    {:run suffix
     :sha256 (format "%064x" (java.math.BigInteger. 1 (.digest (java.security.MessageDigest/getInstance "SHA-256") (.getBytes raw "UTF-8"))))
     :source-entries (:n-entries provenance) :projected-sources (count (:projected-from provenance))
     :projected-tokens (count live) :universe-size (count v) :wanted-tokens (count w)
     :projected-live-total live-total :all-consumed-total total
     :fallback-weights (apply dissoc w (keys live))
     :current {:G original :spread (spread original) :G-odds-factor (Math/exp (spread original))}
     :outcome-renormalization-only {:terminal-outcomes (count unique-states)
                                   :terminal-risk restricted-risk :spread (spread restricted-risk)}
     :marginal-varying-only {:token-count (count varying) :spread (spread varying-utilities)}
     :utility-law-changes
     (into {} (map (fn [[kind weights]]
                    (let [gs (score weights v)] [kind {:G gs :spread (spread gs)
                                                     :G-odds-factor (Math/exp (spread gs))}])) variants))}))

(defn kernel-audit []
  (let [pinned (edn/read-string (slurp "resources/run4/checkpoint-kernel.edn"))
        fitted (kernel/read-kernel "holes/labs/M-aif-full-loop-46/ledger.edn")
        evaluate (fn [a]
                   (try {:risk (risk/disposition-risk {} (risk/constant-checkpoint-kernel a) ruled/seeded-c)}
                        (catch Exception e {:refusal (ex-data e)})))]
    {:pinned (merge (select-keys pinned [:sample-size :support :source :conditioning]) (evaluate pinned))
     :refitted (merge (select-keys fitted [:sample-size :support :source :supported-outcomes :conditioning]) (evaluate fitted))}))

(defn pattern-census []
  (let [root (io/file "/home/joe/code/futon3/library")
        files (sort-by str (filter #(and (.isFile %) (str/ends-with? (str %) ".flexiarg")) (file-seq root)))
        tags (mapcat #(set (map second (re-seq #"(?m)^@([\w-]+)" (slurp %)))) files)
        counts (frequencies tags)]
    {:root (str root) :files (count files)
     :inventory-sha256
     (let [text (pr-str (mapv #(vector (str %) (kernel/sha256 (str %))) files))]
       (format "%064x" (java.math.BigInteger. 1 (.digest (java.security.MessageDigest/getInstance "SHA-256") (.getBytes text "UTF-8")))))
     :header-counts (into (sorted-map) (map #(vector % (get counts % 0))
                                           ["why" "how" "violation-signature" "weight" "utility" "preference"
                                            "odds" "probability" "cost" "benefit" "priority" "score" "confidence"
                                            "energy" "aif-delta" "epistemic-value" "hamming-weight"]))}))

(pp/pprint {:replays (mapv replay ["1789964661" "1789952479"])
            :disposition (kernel-audit) :patterns (pattern-census)
            :validation "All 27 frozen Gs reproduce within 1e-8. Shared outcome conditioning and removal of fixed coordinates preserve spread; utility renormalizations are distinct C changes."})
