#!/usr/bin/env bb
(ns checks.contract-lint
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def repo-root "/home/joe/code")
(def implemented-shapes #{"AblationTable" "EraTable"})

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
  (and (map? x) (integer? (:boundary x)) (map? (:per-era x))
       (every? #(contains? (:per-era x) %) [:pre :post])))

(defn shape-result [evidence fixture read-fixture]
  (if-not (implemented-shapes evidence)
    :shape-check-not-implemented
    (try
      (let [x (read-fixture fixture)]
        (if ((case evidence "AblationTable" ablation-table? "EraTable" era-table?) x)
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
  (when (odd? (count args)) (throw (ex-info "arguments must be pairs" {})))
  (into {} (map (fn [[k v]] [(keyword (subs k 2)) v]) (partition 2 args))))

(defn -main [& args]
  (let [{:keys [report] :as opts} (args-map args)]
    (when-not (every? opts [:contract :registry :report :authority])
      (throw (ex-info "usage: --contract JSON --registry EDN --report EDN --authority SHA" {})))
    (let [result (try (lint-file opts)
                      (catch Exception e
                        {:summary {:pass? false :counts {}}
                         :owners [] :declarations []
                         :errors [{:error :lint-exception :message (.getMessage e)}]}))]
      (spit report (str (pr-str result) "\n"))
      (doseq [{:keys [owner declared-with-body declared-with-sorry]} (:owners result)]
        (println owner "declared-with-body" declared-with-body)
        (println owner "declared-with-sorry" declared-with-sorry))
      (doseq [[j n] (get-in result [:summary :counts])]
        (println (name j) n))
      (when-not (get-in result [:summary :pass?]) (System/exit 1)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
