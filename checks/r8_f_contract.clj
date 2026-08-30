#!/usr/bin/env bb
(ns checks.r8-f-contract
  "Fail-closed census and era checker for the R8 free-energy trace contract."
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.math BigInteger]
           [java.security MessageDigest]))

(def default-trace-dir "/home/joe/code/futon2/data/wm-trace")
(def default-boundary 20260714)
(def expected-census {:missing-F-computable 755
                      :stored-F 32
                      :insufficient-inputs 5})

(defn trace-files [trace-dir]
  (->> (fs/list-dir trace-dir)
       (filter fs/regular-file?)
       (filter #(re-matches #"wm-trace-.*\.edn" (str (fs/file-name %))))
       (sort-by str)
       vec))

(defn- read-edn-forms [path]
  (with-open [reader (java.io.PushbackReader. (io/reader (str path)))]
    (let [eof (Object.)]
      (loop [forms []]
        (let [form (edn/read {:eof eof :default (fn [_ value] value)} reader)]
          (if (identical? eof form)
            forms
            (recur (conj forms form))))))))

(defn read-corpus [trace-dir]
  (let [files (trace-files trace-dir)]
    {:files files
     :records
     (vec
      (mapcat
       (fn [path]
         (let [file (str (fs/file-name path))
               [_ yyyy mm dd] (re-matches
                               #"wm-trace-([0-9]{4})-([0-9]{2})-([0-9]{2})\.edn"
                               file)
               file-date (parse-long (str yyyy mm dd))]
           (map-indexed (fn [index value]
                          {:file file :form (inc index)
                           :file-date file-date :value value})
                        (read-edn-forms path))))
       files))}))

;; These three arms are kept in the same order as r8Disposition in Holes.lean.
(defn disposition [tick]
  (let [prediction-errors (:prediction-errors tick)
        precision-state (:precision-state tick)
        stored-f (:variational-free-energy tick)]
    (cond
      (and (some? prediction-errors) (some? precision-state) (nil? stored-f))
      :missing-F-computable

      (and (some? prediction-errors) (some? precision-state) (some? stored-f))
      :stored-F

      :else :insufficient-inputs)))

(defn free-energy-shape [tick]
  (let [free-energy (:free-energy tick)]
    (cond
      (and (map? free-energy)
           (contains? free-energy :G-total)
           (not (contains? free-energy :controller-score))) :g-map
      (and (map? free-energy)
           (contains? free-energy :controller-score)
           (not (contains? free-energy :G-total)))
      :controller-map
      :else :unexplained-regime)))

(defn recompute-f [tick]
  (let [errors (:prediction-errors tick)
        precision (:precision-state tick)]
    (when-not (and (map? errors) (seq errors) (map? precision))
      (throw (ex-info "computable tick has malformed input maps" {})))
    (let [terms
          (mapv (fn [[channel error-state]]
                  (let [error (:error error-state)
                        pi (get-in precision [channel :precision])]
                    (when-not (and (number? error) (number? pi))
                      (throw (ex-info "computable tick has a malformed channel"
                                      {:channel channel})))
                    (* (double pi) (double error) (double error))))
                errors)]
      (* 0.5 (/ (reduce + 0.0 terms) (count terms))))))

(defn- finite? [number]
  (Double/isFinite (double number)))

(defn- quantile [sorted-values fraction]
  (when (seq sorted-values)
    (let [position (* fraction (dec (count sorted-values)))
          lower (long (Math/floor position))
          upper (long (Math/ceil position))
          weight (- position lower)]
      (+ (* (- 1.0 weight) (nth sorted-values lower))
         (* weight (nth sorted-values upper))))))

(defn distribution [values]
  (let [finite-values (vec (sort (filter finite? values)))]
    {:count (count values)
     :non-finite (- (count values) (count finite-values))
     :min (first finite-values)
     :q25 (quantile finite-values 0.25)
     :median (quantile finite-values 0.5)
     :q75 (quantile finite-values 0.75)
     :max (last finite-values)}))

(defn- sha256 [text]
  (let [digest (doto (MessageDigest/getInstance "SHA-256")
                 (.update (.getBytes (str text) "UTF-8")))]
    (format "%064x" (BigInteger. 1 (.digest digest)))))

(defn content-pin [records]
  (->> records
       ;; Hash each parsed top-level form, sort those hashes so file traversal
       ;; does not affect the pin, then hash the newline-delimited digest list.
       (map (comp sha256 pr-str :value))
       sort
       (str/join "\n")
       sha256))

(defn- tick-id [{:keys [file form value]}]
  {:file file :form form :timestamp (:timestamp value)})

(defn- mean [values]
  (when (seq values) (/ (reduce + 0.0 values) (count values))))

(defn- era-metrics [records]
  (let [usable-records (filter #(and (map? (:prediction-errors (:value %)))
                                      (map? (:precision-state (:value %))))
                               records)
        precision-values
        (mapcat (fn [{:keys [value]}]
                  (keep :precision (vals (:precision-state value))))
                usable-records)
        errors (mapcat (comp vals :prediction-errors :value) usable-records)]
    {:precision {:mean (mean precision-values)
                 :channels (count precision-values)
                 :records (count usable-records)}
     :absolute-error {:mean (mean (map #(Math/abs (double (:error %))) errors))
                      :channels (count errors)
                      :records (count usable-records)}
     :channels-per-form {:mean (mean (map #(count (:prediction-errors (:value %)))
                                          usable-records))
                         :records (count usable-records)}}))

(defn analyze-corpus [{:keys [files records]} boundary]
  (let [classified (mapv #(assoc %
                                 :disposition (disposition (:value %))
                                 :shape (free-energy-shape (:value %)))
                         records)
        by-disposition (group-by :disposition classified)
        census (into {} (map (fn [[kind xs]] [kind (count xs)]))
                     by-disposition)
        computable (filter #(contains? #{:missing-F-computable :stored-F}
                                        (:disposition %))
                           classified)
        computed (mapv #(assoc % :computed-f (recompute-f (:value %))) computable)
        by-computed-disposition (group-by :disposition computed)
        without-stored (filter #(nil? (:variational-free-energy (:value %)))
                               classified)
        with-stored (filter #(some? (:variational-free-energy (:value %)))
                            classified)
        era-violations
        (reduce
         (fn [result {:keys [file-date shape value] :as record}]
           (let [stored? (some? (:variational-free-energy value))
                 gain? (some? (:selection-gain value))
                 controller? (= :controller-map shape)
                 suffix? (<= boundary file-date)
                 add (fn [m key] (update m key conj (tick-id record)))]
             (cond-> result
               (and stored? (not gain?)) (add :stored-without-gain)
               (and gain? (not stored?)) (add :gain-without-stored)
               (and stored? (not controller?)) (add :stored-not-controller)
               (and controller? (not stored?)) (add :controller-without-stored)
               (and stored? (not suffix?)) (add :stored-before-boundary)
               (and suffix? (not stored?)) (add :suffix-without-stored))))
         {:stored-without-gain [] :gain-without-stored []
          :stored-not-controller [] :controller-without-stored []
          :stored-before-boundary [] :suffix-without-stored []}
         classified)
        stored-computed (get by-computed-disposition :stored-F [])
        stored-deltas (mapv #(Math/abs (- (double (:computed-f %))
                                               (double (:variational-free-energy
                                                        (:value %)))))
                            stored-computed)
        unknown-shapes (mapv tick-id (filter #(= :unexplained-regime (:shape %))
                                             classified))
        shape-counts (frequencies (map :shape classified))
        both-keys (count (filter #(let [free-energy (:free-energy (:value %))]
                                    (and (contains? free-energy :controller-score)
                                         (contains? free-energy :G-total)))
                                 classified))
        neither-key (count (filter #(let [free-energy (:free-energy (:value %))]
                                      (and (not (contains? free-energy
                                                           :controller-score))
                                           (not (contains? free-energy :G-total))))
                                   classified))
        before-dates (map :file-date
                          (filter #(and (< (:file-date %) boundary)
                                        (nil? (:variational-free-energy
                                               (:value %))))
                                  classified))
        suffix-dates (map :file-date
                          (filter #(and (<= boundary (:file-date %))
                                        (some? (:variational-free-energy
                                                (:value %))))
                                  classified))
        missing-values (mapv :computed-f
                             (get by-computed-disposition :missing-F-computable []))
        checks (cond-> []
                 (not= expected-census census)
                 (conj {:check :r8-census :expected expected-census :actual census})
                 (not= (count records) (reduce + (vals census)))
                 (conj {:check :disposition-partition :forms (count records)
                        :classified (reduce + (vals census))})
                 (seq unknown-shapes)
                 (conj {:check :free-energy-shape :records unknown-shapes})
                 (some seq (vals era-violations))
                 (conj {:check :era-boundary :boundary boundary
                        :violations era-violations})
                 (pos? (:non-finite (distribution missing-values)))
                 (conj {:check :recomputed-F :reason :non-finite}))]
    {:summary {:pass? (empty? checks)
               :files (count files) :forms (count records)
               :boundary boundary :failures (count checks)}
     :content-pin {:algorithm :sha256-over-newline-joined-sorted-form-sha256
                   :sha256 (content-pin records)}
     :r8CensusWmTrace
     {:status :fixture-evidence
      :classifier-keys [:prediction-errors :precision-state
                        :variational-free-energy]
      :counts census
      :ticks (into {} (map (fn [[kind xs]] [kind (mapv tick-id xs)]))
                   by-disposition)}
     :r8EraBoundary
     {:status :fixture-evidence
      :boundary boundary
      :era-counts {:without-stored-F (count without-stored)
                   :with-stored-F (count with-stored)}
      :shape-counts {:g-map (get shape-counts :g-map 0)
                     :controller-map (get shape-counts :controller-map 0)
                     :unknown (get shape-counts :unexplained-regime 0)
                     :both-keys both-keys :neither-key neither-key}
      :violations era-violations
      :conjunct-violations
      {:storedF-iff-selectionGain
       (+ (count (:stored-without-gain era-violations))
          (count (:gain-without-stored era-violations)))
       :storedF-iff-controllerMap
       (+ (count (:stored-not-controller era-violations))
          (count (:controller-without-stored era-violations)))
       :storedF-iff-dateSuffix
       (+ (count (:stored-before-boundary era-violations))
          (count (:suffix-without-stored era-violations)))}
      :date-margin {:latest-pre-boundary (when (seq before-dates)
                                           (apply max before-dates))
                    :earliest-post-boundary (when (seq suffix-dates)
                                              (apply min suffix-dates))}
      :identity-conjuncts {:status :source-consistency
                           :write-site {:file "scripts/futon2/report/war_machine.clj"
                                        :lines [4664 4665 4687]}}
      :date-conjunct {:status :contingent-non-interleaving}}
     :F
     {:missing-F-computable (distribution missing-values)
      :stored-F (distribution (mapv :computed-f stored-computed))
      :stored-recompute-consistency
      {:status :tautological-consistency-check
       :epsilon (if (seq stored-deltas) (apply max stored-deltas) 0.0)
       :max-absolute-delta (if (seq stored-deltas) (apply max stored-deltas) 0.0)}
      :era-metrics {:without-stored-F (era-metrics without-stored)
                    :with-stored-F (era-metrics with-stored)}
      :cause {:status :inferred-untested}}
     :checks checks}))

(defn lint-paths [{:keys [trace-dir boundary]
                    :or {trace-dir default-trace-dir boundary default-boundary}}]
  (analyze-corpus (read-corpus trace-dir) boundary))

(defn- option-unit-literal [present?]
  (if present? "some ()" "none"))

(defn- lean-tick-literal [{:keys [file-date value]}]
  (let [free-energy (:free-energy value)]
    (str "  { predictionErrors := "
         (option-unit-literal (some? (:prediction-errors value))) "\n"
         "    precisionState := "
         (option-unit-literal (some? (:precision-state value))) "\n"
         "    storedF := " (if (some? (:variational-free-energy value))
                              "some 0" "none") "\n"
         "    selectionGain := "
         (option-unit-literal (some? (:selection-gain value))) "\n"
         "    hasControllerScore := "
         (if (contains? free-energy :controller-score) "true" "false") "\n"
         "    hasGTotal := "
         (if (contains? free-energy :G-total) "true" "false") "\n"
         "    fileDate := " file-date " }")))

(defn lean-fixture-text [report records]
  (str "import DarkTower.WarMachine.Holes\n\n"
       "/- Generated by checks/r8_f_contract.clj.\n"
       "   Content-pin algorithm: "
       (name (get-in report [:content-pin :algorithm])) "\n"
       "   EDN content pin: " (get-in report [:content-pin :sha256]) "\n"
       "   Files/forms: " (get-in report [:summary :files]) "/"
       (get-in report [:summary :forms]) "\n"
       "   Options encode field presence; stored `some 0` is only the presence\n"
       "   witness consumed by these laws, not a transcription of F's value.\n-/\n\n"
       "namespace DarkTower.WarMachine.Holes.R8GeneratedFixture\n\n"
       "def wmTraceR8Generated : List R8TickLit :=\n[\n"
       (str/join ",\n" (map lean-tick-literal records))
       "\n]\n\n"
       "theorem generatedCensus :\n"
       "    r8Census wmTraceR8Generated = (755, 32, 5) := by\n"
       "  native_decide\n\n"
       "theorem generatedEraBoundary :\n"
       "    ∀ t ∈ wmTraceR8Generated,\n"
       "      (t.storedF.isSome ↔ t.selectionGain.isSome) ∧\n"
       "      (t.storedF.isSome ↔ t.freeEnergyShape = .controllerMap) ∧\n"
       "      (t.storedF.isSome ↔ 20260714 ≤ t.fileDate) := by\n"
       "  native_decide\n\n"
       "end DarkTower.WarMachine.Holes.R8GeneratedFixture\n"))

(defn- sibling-lean-path [report-path]
  (str (fs/path (or (fs/parent report-path) ".")
                (str (fs/strip-ext (fs/file-name report-path)) ".lean"))))

(defn- parse-args [args]
  (when (odd? (count args))
    (throw (ex-info "arguments must be --key value pairs" {})))
  (into {} (map (fn [[key value]]
                  [(keyword (str/replace key #"^--" "")) value]))
        (partition 2 args)))

(defn -main [& args]
  (let [opts (parse-args args)
        report-path (:report opts)]
    (when-not report-path
      (binding [*out* *err*]
        (println "usage: r8_f_contract.clj [--trace-dir DIR] --report FILE"))
      (System/exit 2))
    (let [report (try
                   (lint-paths (cond-> opts (:boundary opts)
                                       (update :boundary parse-long)))
                   (catch Exception exception
                     {:summary {:pass? false :files 0 :forms 0
                                :boundary default-boundary :failures 1}
                      :checks [{:check :checker :reason :checker-error
                                :message (.getMessage exception)}]}))]
      (when-let [parent (fs/parent report-path)] (fs/create-dirs parent))
      (spit report-path (str (pr-str report) "\n"))
      (when (get-in report [:summary :pass?])
        (let [corpus (read-corpus (or (:trace-dir opts) default-trace-dir))]
          (spit (sibling-lean-path report-path)
                (lean-fixture-text report (:records corpus)))))
      (println "trace files:" (get-in report [:summary :files]))
      (println "trace forms:" (get-in report [:summary :forms]))
      (println "dispositions:" (get-in report [:r8CensusWmTrace :counts]))
      (println "content pin:" (get-in report [:content-pin :sha256]))
      (System/exit (if (get-in report [:summary :pass?]) 0 1)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
