#!/usr/bin/env bb
(ns checks.r2-channel-contract
  "Fail-closed contract checker for the R2 observation-channel type."
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str])
  (:import [java.math BigInteger]
           [java.security MessageDigest]))

(def default-observation-source
  "/home/joe/code/futon2/src/futon2/aif/observation.clj")
(def default-belief-source
  "/home/joe/code/futon2/src/futon2/aif/belief.clj")
(def default-trace-dir "/home/joe/code/futon2/data/wm-trace")

;; This is an independent, explicit Option-None arm.  Computing it from the
;; declaration difference would make a newly declared but unmodelled channel
;; silently classify itself.
(def likelihood-none-channels
  [:consulting-pct :depositing-signal :loop-health :mathematics-pct
   :portfolio-pct :stack-pct])

(def lean-channel-constructors
  {:loop-health ".loopHealth"
   :support-coverage ".supportCoverage"
   :attack-coverage ".attackCoverage"
   :mission-health ".missionHealth"
   :stack-pct ".stackPct"
   :consulting-pct ".consultingPct"
   :portfolio-pct ".portfolioPct"
   :mathematics-pct ".mathematicsPct"
   :active-repo-ratio ".activeRepoRatio"
   :sorry-count-norm ".sorryCountNorm"
   :coupling-density ".couplingDensity"
   :ticks-firing-ratio ".ticksFiringRatio"
   :depositing-signal ".depositingSignal"
   :annotation-health ".annotationHealth"})

(defn- read-clojure-forms [path]
  (binding [*read-eval* false]
    (with-open [reader (java.io.PushbackReader. (io/reader path))]
      (let [eof (Object.)]
        (loop [forms []]
          (let [form (read {:eof eof} reader)]
            (if (identical? eof form)
              forms
              (recur (conj forms form)))))))))

(defn- def-value [forms symbol]
  (some (fn [form]
          (when (and (seq? form) (= 'def (first form)) (= symbol (second form)))
            (last form)))
        forms))

(defn- definition-line [path symbol]
  (let [pattern (re-pattern (str "^\\(def\\s+"
                                 (java.util.regex.Pattern/quote (name symbol))
                                 "(?:\\s|$)"))]
    (some (fn [[index line]]
            (when (re-find pattern line) (inc index)))
          (map-indexed vector (str/split-lines (slurp path))))))

(defn read-declarations
  [{:keys [observation-source belief-source]
    :or {observation-source default-observation-source
         belief-source default-belief-source}}]
  (let [observation-forms (read-clojure-forms observation-source)
        belief-forms (read-clojure-forms belief-source)
        channels (def-value observation-forms 'observation-channels)
        modeled (def-value belief-forms 'channels-with-likelihood)]
    (when-not (and (vector? channels) (every? keyword? channels))
      (throw (ex-info "observation-channels is not a keyword vector"
                      {:path observation-source :value channels})))
    (when-not (and (set? modeled) (every? keyword? modeled))
      (throw (ex-info "channels-with-likelihood is not a keyword set"
                      {:path belief-source :value modeled})))
    {:channels channels
     :modeled modeled
     :channel-source {:file observation-source
                      :line (definition-line observation-source
                                             'observation-channels)
                      :definition 'observation-channels}
     :likelihood-source {:file belief-source
                         :line (definition-line belief-source
                                                'channels-with-likelihood)
                         :definition 'channels-with-likelihood}}))

(defn- trace-files [trace-dir]
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

(defn read-trace-corpus [trace-dir]
  (let [files (trace-files trace-dir)]
    {:files files
     :records
     (vec (mapcat (fn [path]
                    (map-indexed
                     (fn [index form]
                       {:file (str (fs/file-name path))
                        :form (inc index)
                        :value form})
                     (read-edn-forms path)))
                  files))}))

(defn- sha256 [text]
  (let [digest (doto (MessageDigest/getInstance "SHA-256")
                 (.update (.getBytes (str text) "UTF-8")))]
    (format "%064x" (BigInteger. 1 (.digest digest)))))

(defn- content-pin [records]
  ;; This is the recorded R2/R8 pin algorithm: Clojure's value hash for each
  ;; parsed form, rendered in decimal, sorted, concatenated, then SHA-256.
  ;; It intentionally pins parsed content rather than file traversal order.
  (->> records
       (map (comp str hash :value))
       sort
       (apply str)
       sha256))

(defn- record-check [declared {:keys [file form value]}]
  (let [observation (:observation value)]
    (cond
      (not (map? value))
      {:check :trace-form :reason :not-a-map :file file :form form}

      (not (map? observation))
      {:check :observation :reason :not-a-map :file file :form form
       :timestamp (:timestamp value)}

      (not= declared (set (keys observation)))
      {:check :observation-keys :reason :channel-key-set-mismatch
       :file file :form form :timestamp (:timestamp value)
       :missing (vec (filter #(not (contains? observation %)) declared))
       :undeclared (vec (sort (set/difference (set (keys observation))
                                             (set declared))))}

      :else nil)))

(defn- likelihood-option [modeled channel]
  (if (contains? modeled channel)
    {:option :some :value :likelihood-model}
    {:option :none :reason :n-a-by-design}))

(defn- docstring-finding [observation-source channel-count]
  (let [lines (str/split-lines (slurp observation-source))
        match (some (fn [[index line]]
                      (when-let [[_ count-text] (re-find #"a ([0-9]+)-channel" line)]
                        {:line (inc index) :count (parse-long count-text)}))
                    (map-indexed vector lines))]
    (when (and match (not= channel-count (:count match)))
      {:finding :channel-count-docstring-drift
       :declared-count channel-count
       :docstring-count (:count match)
       :docstring {:file observation-source :line (:line match)}
       :declaration {:file observation-source
                     :line (definition-line observation-source
                                            'observation-channels)}})))

(defn lint-paths
  [{:keys [trace-dir] :as opts}]
  (let [{:keys [channels modeled channel-source likelihood-source]}
        (read-declarations opts)
        trace-dir (or trace-dir default-trace-dir)
        {:keys [files records]} (read-trace-corpus trace-dir)
        declared (set channels)
        explicit-none (set likelihood-none-channels)
        actual-none (set/difference declared modeled)
        modeled-undeclared (set/difference modeled declared)
        lean-channel-mismatch
        (when (not= declared (set (keys lean-channel-constructors)))
          {:check :lean-channel-adapter
           :reason :constructor-key-set-mismatch
           :missing (vec (filter #(not (contains? lean-channel-constructors %))
                                 channels))
           :undeclared (vec (sort (set/difference
                                   (set (keys lean-channel-constructors))
                                   declared)))})
        partition-checks
        (cond-> []
          lean-channel-mismatch
          (conj lean-channel-mismatch)
          (seq modeled-undeclared)
          (conj {:check :likelihood-partition
                 :reason :modeled-channel-not-declared
                 :channels (vec (sort modeled-undeclared))})
          (not= explicit-none actual-none)
          (conj {:check :likelihood-partition
                 :reason :explicit-none-mismatch
                 :expected likelihood-none-channels
                 :actual (vec (filter actual-none channels))
                 :declared-in-neither
                 (vec (filter #(and (not (contains? modeled %))
                                    (not (contains? explicit-none %)))
                              channels))}))
        record-checks (vec (keep #(record-check declared %) records))
        checks (into (vec partition-checks) record-checks)
        malformed (count (filter #(contains? #{:trace-form :observation}
                                               (:check %))
                                 record-checks))
        mismatched (count (filter #(= :observation-keys (:check %))
                                  record-checks))
        finding (docstring-finding (:file channel-source) (count channels))
        digest (content-pin records)]
    {:summary {:pass? (empty? checks)
               :files (count files)
               :forms (count records)
               :conformant-records (- (count records) (count record-checks))
               :key-set-mismatches mismatched
               :malformed-observations malformed
               :failures (count checks)}
     :content-pin {:algorithm :sha256-over-concatenated-sorted-clojure-form-hashes
                   :sha256 digest
                   :prefix (subs digest 0 16)}
     :channel {:source channel-source
               :values channels}
     :likelihood {:source likelihood-source
                  :options (mapv (fn [channel]
                                   [channel (likelihood-option modeled channel)])
                                 channels)
                  :explicit-none likelihood-none-channels
                  :partition-valid? (empty? partition-checks)}
     :r2ContractCensusWmTrace
     {:status :fixture-evidence
      :well-formed-predicate
      "fun tick => Channel.all.all (fun c => (tick.observation c).isSome)"
      :ill-formed (+ mismatched malformed)
      :ill-formed-ticks
      (mapv #(select-keys % [:file :form :timestamp :missing :undeclared])
            record-checks)}
     :turn-channel
     {:status :blocked-design-decision
      :required-kind :value-must-depend-on-typed-turn-content
      :refused :presence-count-recency-or-timestamp-only-signal
      :candidates
      [{:kind :association-content
        :decision-needed :polarity-and-aggregation-of-cross-check-lightbulb}
       {:kind :likelihood-evidence
        :decision-needed :hidden-state-and-mark-conditioned-likelihood-rows}]}
     :findings (cond-> [] finding (conj finding))
     :checks checks}))

(defn- lean-tick-literal [channels observation]
  (let [missing (keep (fn [channel]
                        (when-not (contains? observation channel)
                          (get lean-channel-constructors channel)))
                      channels)]
    (if (empty? missing)
      "  { observation := fun _ => some () }"
      (str "  { observation := fun c => if "
           (str/join " ∨ " (map #(str "c = " %) missing))
           " then none else some () }"))))

(defn lean-fixture-text [report records]
  (let [channels (get-in report [:channel :values])
        digest (get-in report [:content-pin :sha256])
        ticks (map #(lean-tick-literal channels (get-in % [:value :observation]))
                   records)]
    (str "import DarkTower.WarMachine.Holes\n\n"
         "/- Generated by checks/r2_channel_contract.clj.\n"
         "   EDN content pin: " digest "\n"
         "   Files/forms: " (get-in report [:summary :files]) "/"
         (get-in report [:summary :forms]) "\n-/\n\n"
         "namespace DarkTower.WarMachine.Holes.R2GeneratedFixture\n\n"
         "def wmTraceR2Generated : List R2TickLit :=\n[\n"
         (str/join ",\n" ticks)
         "\n]\n\n"
         "example :\n"
         "    r2ContractCensus wmTraceR2Generated\n"
         "      (fun tick => Channel.all.all (fun c => (tick.observation c).isSome)) = 2 := by\n"
         "  native_decide\n\n"
         "end DarkTower.WarMachine.Holes.R2GeneratedFixture\n")))

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
        (println (str "usage: r2_channel_contract.clj [--trace-dir DIR] "
                      "[--observation-source FILE] [--belief-source FILE] "
                      "--report FILE")))
      (System/exit 2))
    (let [result (try
                   (let [report (lint-paths opts)
                         corpus (read-trace-corpus
                                 (or (:trace-dir opts) default-trace-dir))]
                     {:report report :records (:records corpus)})
                   (catch Exception exception
                     {:report
                      {:summary {:pass? false :files 0 :forms 0
                                 :conformant-records 0 :key-set-mismatches 0
                                 :malformed-observations 0 :failures 1}
                       :content-pin {:algorithm :unavailable :sha256 nil :prefix nil}
                       :channel {:source :unavailable :values []}
                       :likelihood {:source :unavailable :options []
                                    :explicit-none likelihood-none-channels
                                    :partition-valid? false}
                       :findings []
                       :checks [{:check :linter :reason :linter-error
                                 :message (.getMessage exception)}]}
                      :records []}))
          report (:report result)
          lean-path (sibling-lean-path report-path)]
      (when-let [parent (fs/parent report-path)]
        (fs/create-dirs parent))
      (spit report-path (str (pr-str report) "\n"))
      (when (seq (:records result))
        (spit lean-path (lean-fixture-text report (:records result))))
      (println "trace files:" (get-in report [:summary :files]))
      (println "trace forms:" (get-in report [:summary :forms]))
      (println "content pin:" (get-in report [:content-pin :prefix]))
      (println "records conforming:" (get-in report [:summary :conformant-records]))
      (println "records firing:" (+ (get-in report [:summary :key-set-mismatches])
                                     (get-in report [:summary :malformed-observations])))
      (println "Lean fixture:" lean-path)
      (System/exit (if (get-in report [:summary :pass?]) 0 1)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
