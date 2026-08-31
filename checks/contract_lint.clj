#!/usr/bin/env bb
(ns checks.contract-lint
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def repo-root "/home/joe/code")
(defn- read-json [p] (json/parse-string (slurp p) true))
(defn- read-edn [p] (edn/read-string (slurp p)))
(defn- names [binding]
  (let [x (:witnesses binding)] (if (sequential? x) x [x])))
(defn- artifact-path [{:keys [repo path]}]
  (str (fs/path repo-root repo path)))
(defn- last-sha [fixture]
  (let [{:keys [repo path]} fixture]
    (str/trim (:out (shell {:out :string :err :string}
                           "git" "-C" (str (fs/path repo-root repo))
                           "log" "-1" "--format=%H" "--" path)))))

(defn ablation-table? [x]
  (let [rows (:ablation x)
        ablated (filter #(= :ablated (:status %)) rows)]
    (and (vector? rows) (seq rows) (seq ablated)
         (every? #(and (keyword? (:prior %))
                       (sequential? (:argmin-G %))
                       (sequential? (:argmin-risk %))
                       (boolean? (:moved? %))) ablated))))

(defn era-table? [x]
  (let [era (or (:r8EraBoundary x) x)
        per-era (:perEra era)]
    (and (map? era) (integer? (:boundary era)) (map? per-era)
         (every? #(contains? per-era %) [:before :after]))))

(defn find-receipt-table? [x]
  (let [scenarios (:scenarios x)
        rounds (mapcat :round-results scenarios)]
    (and (vector? scenarios) (seq scenarios) (seq rounds)
         (every? (fn [round]
                   (let [find (:find round)
                         selected (:selected find)
                         receipts (:receipts find)]
                     (and (sequential? selected) (map? receipts)
                          (every? #(contains? receipts %) selected)
                          (every? (fn [receipt]
                                    (and (keyword? (:route receipt))
                                         (string? (get-in receipt [:warrant :file]))))
                                  (vals receipts)))))
                 rounds))))

(defn verdict-table? [x]
  (let [runs (:runs x)
        ledger (get-in runs [:ledger-alone :rows])
        declared (get-in runs [:declared :rows])
        valid-row? #(and (string? (:row %))
                         (contains? #{:unknown :self :independent} (:verdict %))
                         (contains? % :declaration-source))]
    (and (seq ledger) (seq declared)
         (= (mapv :row ledger) (mapv :row declared))
         (every? valid-row? (concat ledger declared))
         (true? (get-in x [:checks :per-row-sources?]))
         (true? (get-in x [:checks :declared-sound?])))))

(defn r2-tick-list? [x]
  (and (pos-int? (get-in x [:summary :forms]))
       (= 64 (count (get-in x [:content-pin :sha256] "")))
       (= 14 (count (get-in x [:channel :values])))))

(defn ill-formed-list? [x]
  (let [n (get-in x [:r2ContractCensusWmTrace :ill-formed])
        rows (get-in x [:r2ContractCensusWmTrace :ill-formed-ticks])]
    (and (nat-int? n) (vector? rows) (= n (count rows))
         (every? #(and (string? (:file %)) (seq (:missing %))) rows))))

(defn r8-tick-list? [x]
  (and (pos-int? (get-in x [:summary :forms]))
       (= 64 (count (get-in x [:content-pin :sha256] "")))
       (map? (get-in x [:r8CensusWmTrace :ticks]))))

(defn r8-disposition-evidence? [x]
  (let [forms (get-in x [:summary :forms])
        counts (get-in x [:r8CensusWmTrace :counts])
        required [:insufficient-inputs :missing-F-computable :stored-F]]
    (and (pos-int? forms) (map? counts)
         (every? #(nat-int? (get counts %)) required)
         (= forms (reduce + (map counts required))))))

(defn tick-run-witness? [x]
  (and (string? (:startedAt x)) (true? (:traceWritten x))
       (pos-int? (:storeBasisCount x)) (seq (:route x))
       (every? #(and (string? (:fromNode %)) (string? (:toNode %))
                     (string? (:via %)) (string? (:at_ %)))
               (:route x))))

(defn log-multivariate-beta-witness? [x]
  (let [cases (into {} (map (juxt :concentrations identity) (:cases x)))]
    (and (= :log-multivariate-beta-reference/v1 (:schema x))
         (= {:nonempty true :concentrations :strictly-positive-real} (:domain x))
         (= {:numerator 1 :denominator 1}
            (get-in cases [[1 1] :normaliser]))
         (= "0" (get-in cases [[1 1] :expected-log]))
         (= {:numerator 1 :denominator 2}
            (get-in cases [[2 1] :normaliser]))
         (= "-log(2)" (get-in cases [[2 1] :expected-log])))))

(defn expected-free-energy-witness? [x]
  (= [{:id :one-point :predictive-masses [1] :preference-masses [1]
       :risk 0 :ambiguity 2 :epistemic-gain -2 :expected-free-energy 2}]
     (:cases x)))

(defn expected-information-gain-witness? [x]
  (= [{:id :binary-prior-point-posterior
       :predictive-outcome-masses [1]
       :parameter-prior-masses [1/2 1/2]
       :parameter-posterior-masses [1 0]
       :expected-information-gain "log(2)"}]
     (:cases x)))

(defn generative-model-witness? [x]
  (and (= [:observation-mass :transition-mass :policy-prior-mass]
          (:factorisation x))
       (= [{:id :binary-observation-deterministic-transition
            :observation-mass 1/2 :transition-mass 1 :policy-prior-mass 1
            :joint-factor-mass 1/2}]
          (:cases x))))

(defn- graph-reachable? [edges from to]
  (loop [frontier [from] seen #{}]
    (if-let [node (first frontier)]
      (cond (= node to) true
            (seen node) (recur (rest frontier) seen)
            :else (recur (into (vec (rest frontier))
                               (map second (filter #(= node (first %)) edges)))
                         (conj seen node)))
      false)))

(defn- fast-forward-edge? [selected edges from to]
  (letfn [(walk [node seen]
            (some (fn [[_ next]]
                    (cond (= next to) true
                          (selected next) false
                          (seen next) false
                          :else (walk next (conj seen next))))
                  (filter #(= node (first %)) edges)))]
    (and (selected from) (selected to) (not= from to)
         (boolean (walk from #{from})))))

(defn cascade-diff? [x]
  (let [selected (set (:selected x))
        additions (set (:added-by-organise x))
        nodes (set (:nodes x))
        authored (set (:authored-edges x))
        organised (set (:organised-edges x))
        expected (set (for [u selected v selected
                            :when (fast-forward-edge? selected authored u v)] [u v]))
        [before after :as variants] (:precedence-variants x)]
    (and (= :cascade-diff/v1 (:schema x))
         (seq selected) (= nodes (into selected additions))
         (every? (fn [[u v]] (graph-reachable? authored u v)) organised)
         (= organised expected) (= 2 (count variants))
         (= nodes (set (:precedence before)))
         (= nodes (set (:precedence after)))
         (not= (:precedence before) (:precedence after))
         (or (not= (:acting-order before) (:acting-order after))
             (not= (:score before) (:score after))))))

(def shape-checks
  {"AblationTable" ablation-table?
   "EraTable" era-table?
   "FindReceiptTable" find-receipt-table?
   "VerdictTable" verdict-table?
   "List R2TickLit" r2-tick-list?
   "IllFormedList" ill-formed-list?
   "List R8TickLit" r8-tick-list?
   "R8DispositionEvidence" r8-disposition-evidence?
   "TickRunWitness" tick-run-witness?
   "LogMultivariateBetaWitness" log-multivariate-beta-witness?
   "ExpectedFreeEnergyWitness" expected-free-energy-witness?
   "ExpectedInformationGainWitness" expected-information-gain-witness?
   "GenerativeModelWitness" generative-model-witness?
   "CascadeDiff" cascade-diff?})

(defn shape-result [evidence fixture read-fixture]
  (if-not (contains? shape-checks evidence)
    :shape-check-not-implemented
    (try
      (let [x (read-fixture fixture)]
        (if ((get shape-checks evidence) x)
          :conformant :wrong-shape))
      (catch Exception _ :wrong-shape))))

(defn lint-data [{:keys [contract registry authority sha-fn read-fixture]
                  :or {sha-fn last-sha
                       read-fixture #(read-edn (artifact-path %))}}]
  (let [decls (:declarations contract)
        by-name (into {} (map (juxt :name identity) decls))
        expanded (mapcat (fn [b] (map #(assoc b :witnesses %) (names b))) registry)
        unknown (vec (remove #(contains? by-name (:witnesses %)) expanded))
        malformed (vec (filter #(or (not (contains? % :result))
                                    (not (contains? #{:passed :failed} (:result %)))
                                    (not (contains? % :recorded-at))) expanded))
        duplicates (->> expanded (group-by :witnesses)
                        (keep (fn [[n xs]] (when (> (count xs) 1) n))) vec)
        errors (vec (concat
                     (map #(hash-map :error :not-in-contract :name (:witnesses %)) unknown)
                     (map #(hash-map :error :missing-run-result :name (:witnesses %)) malformed)
                     (map #(hash-map :error :duplicate-binding :name %) duplicates)))
        bindings (into {} (map (juxt :witnesses identity) expanded))
        source (:source contract)
        authority-ok? (and (= "DarkTower.WarMachine.Holes" (:module source))
                           (= authority (:git-sha source)))
        rows (mapv
              (fn [d]
                (let [b (bindings (:name d))
                      evidence (:evidence d)
                      contract-stale? (and b (not= (:contract-sha b) (:git-sha source)))
                      fixture-stale? (and b (not= (:run-sha b) (sha-fn (:fixture b))))
                      stale? (or contract-stale? fixture-stale?)
                      shape (when b (shape-result evidence (:fixture b) read-fixture))
                      judgement
                      (cond
                        (= "closed" (:kind d)) :closed-by-record
                        (not authority-ok?) :wrong-authority
                        (nil? evidence) :refused-implementation
                        (nil? b) :unwitnessed
                        stale? :stale
                        (= :failed (:result b)) :witness-failed
                        (= :wrong-shape shape) :wrong-shape
                        (= :conformant shape) :conformant
                        (= :shape-check-not-implemented shape) :witnessed
                        :else :unwitnessed)]
                  (cond-> (assoc (select-keys d [:name :kind :owner :holder :evidence])
                                 :judgement judgement :shape-check shape)
                    stale? (assoc :stale-reasons
                                  (cond-> []
                                    contract-stale? (conj :contract-drift)
                                    fixture-stale? (conj :fixture-drift))
                                  :stale-remediation
                                  (if (every? #(get-in b [:check %]) [:repo :path :entrypoint])
                                    :rerun-and-rebind
                                    :manual-triage-required)))))
              decls)
        owners (->> rows (group-by :owner) (sort-by key)
                    (mapv (fn [[owner rs]]
                            {:owner owner
                             :declared-with-body (count (filter #(= "closed" (:kind %)) rs))
                             :declared-with-sorry (count (filter #(= "hole" (:kind %)) rs))})))
        counts (into (sorted-map) (frequencies (map :judgement rows)))
        ;; Freshness is independent of judgement precedence: an authority failure
        ;; must not make stale bindings disappear from the qualification report.
        stale-rows (filterv #(seq (:stale-reasons %)) rows)
        structural-valid? (and (empty? errors) (not (contains? counts :wrong-authority)))
        bindings-fresh? (empty? stale-rows)]
    {:summary {:pass? structural-valid?
               :authority authority :counts counts
               :qualification
               {:structural-valid? structural-valid?
                :bindings-fresh? bindings-fresh?
                :strict-pass? (and structural-valid? bindings-fresh?)
                :stale-declarations (mapv :name stale-rows)
                :stale-remediation-counts
                (into (sorted-map) (frequencies (map :stale-remediation stale-rows)))}}
     :owners owners :declarations rows :errors errors}))

(defn lint-file [{:keys [contract registry authority]}]
  (lint-data {:contract (read-json contract) :registry (read-edn registry)
              :authority authority}))

(defn- args-map [args]
  (loop [xs args out {}]
    (if (empty? xs)
      out
      (if (contains? #{"--negative" "--strict"} (first xs))
        (recur (rest xs) (assoc out (keyword (str (subs (first xs) 2) "?")) true))
        (do
          (when-not (second xs) (throw (ex-info "arguments must be flag/value pairs" {})))
          (recur (nnext xs) (assoc out (keyword (subs (first xs) 2)) (second xs))))))))

(defn -main [& args]
  (let [{:keys [report negative? strict?] :as opts} (args-map args)]
    (when-not (every? opts [:contract :registry :report :authority])
      (throw (ex-info "usage: [--negative] --contract JSON --registry EDN --report EDN --authority SHA" {})))
    (let [result (try (if negative?
                        ;; Semantic mutation: preserve the contract's valid JSON shape but
                        ;; sever its authority pin. The lint exists in part to reject a
                        ;; generated contract that is not from the named authority.
                        (lint-data {:contract (assoc-in (read-json (:contract opts))
                                                        [:source :git-sha]
                                                        "mutation/not-the-authority")
                                    :registry (read-edn (:registry opts))
                                    :authority (:authority opts)})
                        (lint-file opts))
                      (catch Exception e
                        {:summary {:pass? false :counts {}}
                         :owners [] :declarations []
                         :errors [{:error :lint-exception :message (.getMessage e)}]}))]
      ;; A negative control never overwrites the caller's positive report.
      (when-not negative? (spit report (str (pr-str result) "\n")))
      (doseq [{:keys [owner declared-with-body declared-with-sorry]} (:owners result)]
        (println owner "declared-with-body" declared-with-body)
        (println owner "declared-with-sorry" declared-with-sorry))
      (doseq [[j n] (get-in result [:summary :counts])]
        (println (name j) n))
      (let [{:keys [structural-valid? bindings-fresh? strict-pass?
                    stale-declarations stale-remediation-counts]}
            (get-in result [:summary :qualification])]
        (println "structural-valid" structural-valid?)
        (println "bindings-fresh" bindings-fresh?)
        (println "strict-qualification" strict-pass?)
        (when (seq stale-declarations)
          (println "strict-stale-declarations" (str/join "," stale-declarations))
          (println "strict-stale-remediation" (pr-str stale-remediation-counts))))
      (if negative?
        (if (and (not (get-in result [:summary :pass?]))
                 (pos? (get-in result [:summary :counts :wrong-authority] 0)))
          (do (println "contract-lint: PASS negative authority mutation rejected exit-convention=0-pass/1-fail")
              (System/exit 0))
          (do (println "contract-lint: FAIL negative authority mutation slipped exit-convention=0-pass/1-fail")
              (System/exit 2)))
        (let [pass? (if strict?
                      (get-in result [:summary :qualification :strict-pass?])
                      (get-in result [:summary :pass?]))]
          (println (str "contract-lint: " (if pass? "PASS" "FAIL")
                        (when strict? " mode=strict")
                        " exit-convention=0-pass/1-fail"))
          (when-not pass? (System/exit 1)))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
