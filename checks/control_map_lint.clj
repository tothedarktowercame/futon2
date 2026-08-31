#!/usr/bin/env bb
(ns checks.control-map-lint
  "Fail-closed contract inventory for the Figure-4 control-map edges."
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def default-edge-path
  "/home/joe/code/p4ng/empirics-futon/control-map-edges.edn")
(def default-records-dir "/home/joe/code/futon2/holes/problems")

;; The independent baseline is deliberately explicit: without a second record,
;; deleting a drawn edge from the source is indistinguishable from never having
;; drawn it.  Labels are descriptive, so identity is the endpoint pair + kind.
(def expected-drawn-edges
  #{{:from :R1 :to :R4 :kind :control}
    {:from :R2 :to :R3 :kind :control}
    {:from :R3 :to :R1 :kind :control}
    {:from :R4 :to :R5 :kind :control}
    {:from :R5 :to :R6 :kind :control}
    {:from :R6 :to :R13 :kind :control}
    {:from :R11 :to :R16 :kind :control}
    {:from :R13 :to :R14 :kind :control}
    {:from :R14 :to :R16 :kind :control}
    {:from :R16 :to :R2 :kind :control}
    {:from :R6 :to :R11 :kind :support}
    {:from :R7 :to :R3 :kind :support}
    {:from :R7 :to :R8 :kind :support}
    {:from :R7 :to :R14 :kind :support}
    {:from :R8 :to :R5 :kind :support}
    {:from :R9 :to :R16 :kind :support}
    {:from :R10 :to :R8 :kind :support}
    {:from :R12 :to :R7 :kind :support}
    {:from :R15 :to :R13 :kind :support}
    {:from :R15 :to :R16 :kind :support}
    {:from :R20 :to :R7 :kind :support}})

(defn edge-key [edge]
  (select-keys edge [:from :to :kind]))

(defn- node-record-path [records-dir node]
  (fs/path records-dir (str "P-" (name node) ".md")))

(defn- record-text [records-dir node]
  (let [path (node-record-path records-dir node)]
    (when (fs/regular-file? path) (slurp (str path)))))

(defn- edge-reference-pattern [{:keys [from to]}]
  (re-pattern (str "(?i)" (name from) "\\s*(?:→|->)\\s*" (name to))))

(defn- schema-terms [schema]
  ;; Records agree on the payload's field vocabulary.  Field types may be
  ;; rendered as prose, aliases, or links, so values are not text-matched.
  (->> (tree-seq coll? seq schema)
       (filter map?)
       (mapcat keys)
       (keep (fn [x]
               (cond
                 (keyword? x) (name x)
                 (symbol? x) (name x)
                 (string? x) x)))
       (remove str/blank?)
       set))

(defn- term-pattern
  "A schema field name, matched as a whole token.  Bare substring matching
   reported agreement for records that merely contained the letters -- `id`
   inside `identity`, `val` inside `evaluation` -- so two records that never
   mention a field agreed about it.  That manufactures agreement at exactly
   the point CML-D2 starts creating endpoint records; the instrument is fixed
   before it runs, not audited after (delivery-lifecycle log row 12a).
   Hyphen is excluded on both sides so `id` does not match the field `id-key`."
  [term]
  (re-pattern (str "(?i)(?<![A-Za-z0-9-])"
                   (java.util.regex.Pattern/quote term)
                   "(?![A-Za-z0-9-])")))

(defn- record-agrees?
  [text edge schema]
  (and (re-find (edge-reference-pattern edge) text)
       (every? #(re-find (term-pattern %) text)
               (schema-terms schema))))

(defn endpoint-agreement
  "Compute agreement only when both endpoint records exist.  Typed absence is
   distinct from a checked disagreement."
  [records-dir edge schema]
  (let [texts (keep #(record-text records-dir %)
                    (distinct [(:from edge) (:to edge)]))]
    (case (count texts)
      0 :no-endpoint-record
      1 :one-endpoint-record
      2 (if (= :unspecified schema)
          :schema-unspecified
          (every? #(record-agrees? % edge schema) texts)))))

(defn- normalize-drawn-edge [records-dir edge]
  (let [schema (get edge :schema :unspecified)
        fixture (get edge :fixture :unspecified)
        agreement (endpoint-agreement records-dir edge schema)]
    (assoc edge
           :schema schema
           :fixture fixture
           :endpoints-agree? agreement
           :fully-specified?
           (and (map? schema)
                (not= :unspecified fixture)
                (true? agreement)))))

(defn- node-summaries [edges]
  (let [nodes (sort (set (mapcat (juxt :from :to) edges)))]
    (into (sorted-map)
          (map (fn [node]
                 [node {:in (count (filter #(= node (:to %)) edges))
                        :out (count (filter #(= node (:from %)) edges))
                        :fully-specified
                        (count (filter #(and (:fully-specified? %)
                                             (or (= node (:from %))
                                                 (= node (:to %))))
                                       edges))}]))
          nodes)))

(defn lint-data
  [{:keys [data records-dir expected-drawn]
    :or {records-dir default-records-dir
         expected-drawn expected-drawn-edges}}]
  (let [drawn (filterv #(= :drawn (:status %)) (:edges data))
        normalized (mapv #(normalize-drawn-edge records-dir %) drawn)
        actual-keys (set (map edge-key drawn))
        missing (sort-by (juxt :from :to :kind)
                         (remove actual-keys expected-drawn))
        missing-derived-by (keep-indexed
                            (fn [i edge]
                              (when-not (seq (:by edge))
                                {:check :derived-undrawn-has-derivation
                                 :reason :missing-by :index i
                                 :edge (edge-key edge)}))
                            (:derived-undrawn data))
        malformed (mapcat
                   (fn [edge]
                     (cond-> []
                       (not (or (= :unspecified (:schema edge))
                                (map? (:schema edge))))
                       (conj {:check :drawn-edge-schema
                              :reason :invalid-schema
                              :edge (edge-key edge)})
                       (nil? (:fixture edge))
                       (conj {:check :drawn-edge-fixture
                              :reason :invalid-fixture
                              :edge (edge-key edge)})))
                   normalized)
        checks (vec
                (concat
                 (map (fn [edge]
                        {:check :drawn-edge-baseline
                         :reason :drawn-edge-missing
                         :edge edge})
                      missing)
                 missing-derived-by
                 malformed))
        specified (count (filter :fully-specified? normalized))
        unspecified (- (count normalized) specified)]
    {:summary {:pass? (empty? checks)
               :source (:source data)
               :drawn (count drawn)
               :unresolved (count (filter #(= :unresolved (:status %))
                                          (:edges data)))
               :derived-undrawn (count (:derived-undrawn data))
               :derived-chartered
               (count (filter #(= :chartered (:status %))
                              (:derived-undrawn data)))
               :specified specified
               :unspecified unspecified
               :failures (count checks)}
     :nodes (node-summaries normalized)
     :edges normalized
     :checks checks}))

(defn lint-file
  [{:keys [edges records-dir] :as opts}]
  (lint-data (assoc opts
                    :records-dir (or records-dir default-records-dir)
                    :data (edn/read-string (slurp (or edges default-edge-path))))))

(defn- parse-args [args]
  (loop [xs args out {}]
    (if (empty? xs)
      out
      (if (= "--negative" (first xs))
        (recur (rest xs) (assoc out :negative? true))
        (do
          (when-not (second xs) (throw (ex-info "arguments must be --key value pairs" {})))
          (recur (nnext xs)
                 (assoc out (keyword (str/replace (first xs) #"^--" ""))
                        (second xs))))))))

(defn -main [& args]
  (let [{:keys [negative?] :as opts} (parse-args args)
        report-path (:report opts)]
    (when-not report-path
      (binding [*out* *err*]
        (println "usage: control_map_lint.clj [--negative] [--edges FILE] [--records-dir DIR] --report FILE"))
      (System/exit 2))
    (let [report (try
                   (if negative?
                     ;; Semantic mutation: keep valid EDN and edge shapes, but
                     ;; remove one drawn edge the independent baseline requires.
                     (let [data (edn/read-string (slurp (or (:edges opts) default-edge-path)))
                           victim (first (filter #(= :drawn (:status %)) (:edges data)))
                           mutated (update data :edges
                                           #(vec (remove (fn [edge] (= (edge-key edge)
                                                                       (edge-key victim))) %)))]
                       (lint-data {:data mutated
                                   :records-dir (or (:records-dir opts) default-records-dir)}))
                     (lint-file opts))
                   (catch Exception e
                     {:summary {:pass? false :specified 0 :unspecified 0
                                :failures 1}
                      :nodes {}
                      :edges []
                      :checks [{:check :linter :reason :linter-error
                                :message (.getMessage e)}]}))]
      (when-not negative?
        (when-let [parent (fs/parent report-path)]
          (fs/create-dirs parent))
        (spit report-path (str (pr-str report) "\n")))
      (println "drawn edges specified:" (get-in report [:summary :specified]))
      (println "drawn edges unspecified:" (get-in report [:summary :unspecified]))
      (if negative?
        (if (and (not (get-in report [:summary :pass?]))
                 (some #(= :drawn-edge-baseline (:check %)) (:checks report)))
          (do (println "control-map-lint: PASS negative drawn-edge mutation rejected exit-convention=0-pass/1-fail")
              (System/exit 0))
          (do (println "control-map-lint: FAIL negative drawn-edge mutation slipped exit-convention=0-pass/1-fail")
              (System/exit 2)))
        (do (println (str "control-map-lint: " (if (get-in report [:summary :pass?]) "PASS" "FAIL")
                          " exit-convention=0-pass/1-fail"))
            (System/exit (if (get-in report [:summary :pass?]) 0 1)))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
