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

(def shape-checks
  {"AblationTable" ablation-table?
   "EraTable" era-table?
   "FindReceiptTable" find-receipt-table?
   "VerdictTable" verdict-table?
   "List R2TickLit" r2-tick-list?
   "IllFormedList" ill-formed-list?
   "List R8TickLit" r8-tick-list?
   "R8DispositionEvidence" r8-disposition-evidence?
   "TickRunWitness" tick-run-witness?})

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
                      stale? (and b (or (not= (:contract-sha b) (:git-sha source))
                                        (not= (:run-sha b) (sha-fn (:fixture b)))))
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
                  (assoc (select-keys d [:name :kind :owner :holder :evidence])
                         :judgement judgement :shape-check shape)))
              decls)
        owners (->> rows (group-by :owner) (sort-by key)
                    (mapv (fn [[owner rs]]
                            {:owner owner
                             :declared-with-body (count (filter #(= "closed" (:kind %)) rs))
                             :declared-with-sorry (count (filter #(= "hole" (:kind %)) rs))})))
        counts (into (sorted-map) (frequencies (map :judgement rows)))]
    {:summary {:pass? (and (empty? errors) (not (contains? counts :wrong-authority)))
               :authority authority :counts counts}
     :owners owners :declarations rows :errors errors}))

(defn lint-file [{:keys [contract registry authority]}]
  (lint-data {:contract (read-json contract) :registry (read-edn registry)
              :authority authority}))

(defn- args-map [args]
  (loop [xs args out {}]
    (if (empty? xs)
      out
      (if (= "--negative" (first xs))
        (recur (rest xs) (assoc out :negative? true))
        (do
          (when-not (second xs) (throw (ex-info "arguments must be flag/value pairs" {})))
          (recur (nnext xs) (assoc out (keyword (subs (first xs) 2)) (second xs))))))))

(defn -main [& args]
  (let [{:keys [report negative?] :as opts} (args-map args)]
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
      (if negative?
        (if (and (not (get-in result [:summary :pass?]))
                 (pos? (get-in result [:summary :counts :wrong-authority] 0)))
          (do (println "contract-lint: PASS negative authority mutation rejected exit-convention=0-pass/1-fail")
              (System/exit 0))
          (do (println "contract-lint: FAIL negative authority mutation slipped exit-convention=0-pass/1-fail")
              (System/exit 2)))
        (do (println (str "contract-lint: " (if (get-in result [:summary :pass?]) "PASS" "FAIL")
                          " exit-convention=0-pass/1-fail"))
            (when-not (get-in result [:summary :pass?]) (System/exit 1)))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
