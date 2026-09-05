#!/usr/bin/env bb
;; :B2 -- the CHEAPEST refuting experiment for the five strawman holes that do
;; not need a JVM. The other two (E3, E4) are in b2_strawman_experiments.clj.
;;
;;   bb holes/labs/wm-contract/b2_strawman_reads.bb [outdir]
;;
;; Each probe below is an ENUMERATION, not a grep verdict: it reports the whole
;; set it searched and the whole set it found, so an empty result is readable as
;; "the search was exhausted" rather than "the pattern was anchored wrong".
;;
;; G2 is a REPLAY on recorded data, not a read: the only committed cascade
;; artifact with :truncated true is re-scored over its own :shown prefix and the
;; two numbers are compared. Live construction is not attempted (the constructor
;; imports sentence_transformers, which is not installed on this machine --
;; recorded verbatim in the run record).
;;
;; NOTHING IS WRITTEN UNDER data/. Read and replay only.

(require '[babashka.fs :as fs]
         '[cheshire.core :as json]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def futon2 "/home/joe/code/futon2")

(defn- clj-files
  "Every .clj under the futon2 source, script and test trees -- the set every
   absence claim below is stated over."
  []
  (->> ["src" "scripts" "test"]
       (mapcat #(file-seq (io/file futon2 %)))
       (filter #(and (.isFile %) (str/ends-with? (.getName %) ".clj")))
       (sort-by #(.getPath %))))

(defn- hits
  "Every [relative-path line-number line] in the tree matching RE."
  [re]
  (vec
   (for [f (clj-files)
         [i line] (map-indexed vector (str/split-lines (slurp f)))
         :when (re-find re line)]
     [(str/replace (.getPath f) (str futon2 "/") "") (inc i) (str/trim line)])))

;; --------------------------------------------------------------------------
;; G1 -- "Adapt one non-threshold diversity source for Slice 1b"
;; Refuted if an adapter already emits the cascade-candidate schema from an
;; arguing-worlds or slush artifact. Probe: every :candidate-source in the tree,
;; and every producer of the :semilattice key that a complete policy identity
;; needs (mission invariant 2).
;; --------------------------------------------------------------------------

(def g1
  (let [sources (hits #":candidate-source\s+:")
        semilattice (hits #":semilattice")]
    {:hole "Adapt one non-threshold diversity source for Slice 1b"
     :refutation-attempted
     "Enumerate every :candidate-source declared in the tree and every file that mentions :semilattice; refuted if one of them adapts an arguing-worlds or slush artifact into the cascade-candidate schema."
     :candidate-sources-declared sources
     :candidate-source-values (vec (sort (distinct (map (fn [[_ _ l]] (second (re-find #":candidate-source\s+(:[^\s}]+)" l))) sources))))
     :semilattice-files (vec (sort (distinct (map first semilattice))))
     :arguing-worlds-emits-semilattice?
     (boolean (some #(str/includes? % "arguing_worlds") (map first semilattice)))
     :nearest-existing-non-threshold-artifact
     {:path "holes/labs/slush-demo/findings/proposals/batch-2-worklist.json"
      :shape (let [d (json/parse-string (slurp (str futon2 "/holes/labs/slush-demo/findings/proposals/batch-2-worklist.json")) true)]
               {:rows (count d) :keys (vec (sort (map name (keys (first d)))))})
      :note "A pattern BAG keyed by mission. No ordering, no :semilattice, no inclusion/exclusion reasons -- so it is not a complete policy identity under mission invariant 2 (holes/missions/M-wm-aif-policy-grain-compliance.md:65)."}}))

;; --------------------------------------------------------------------------
;; G2 -- "Make cascade scoring prefix-local and auditable"
;; Refuted if the emitted score already describes the enacted prefix. Probe: the
;; one committed cascade artifact with :truncated true, re-scored over its own
;; :shown rows.
;; --------------------------------------------------------------------------

(def g2
  (let [p (str futon2 "/holes/labs/M-evaluate-policies/exhibit/cascade-3-serve.json")
        d (json/parse-string (slurp p) true)
        shown (:shown d)
        prefix-coverage (reduce + 0.0 (map :mc shown))
        prefix-intensity (reduce + 0.0 (map :rel shown))]
    {:hole "Make cascade scoring prefix-local and auditable"
     :refutation-attempted
     "Re-score the one committed truncated cascade over its own :shown prefix and compare with the score it emitted. Refuted if they agree."
     :artifact "holes/labs/M-evaluate-policies/exhibit/cascade-3-serve.json"
     :full-size (:size d)
     :budget (:budget d)
     :shown-count (count shown)
     :truncated (:truncated d)
     :emitted-coverage-reward (:accuracy d)
     :prefix-coverage-reward (Double/parseDouble (format "%.3f" prefix-coverage))
     :coverage-difference (Double/parseDouble (format "%.3f" (- (:accuracy d) prefix-coverage)))
     :emitted-T-intensity (:T-intensity d)
     :prefix-T-intensity (Double/parseDouble (format "%.3f" prefix-intensity))
     :emitted-score (:F-free-energy d)
     :score-identity-check
     {:claim "the emitted score is accuracy - lambda*complexity over the FULL cascade"
      :recomputed (Double/parseDouble (format "%.3f" (- (:accuracy d) (* (:lambda d) (:complexity d)))))}
     :code-basis
     ["futon3a/holes/labs/M-memes-arrows/cascade_construct.py:206 construct_cascade takes no budget"
      "futon3a/holes/labs/M-memes-arrows/cascade_construct.py:236-238 accuracy over the whole trajectory, complexity over the whole chosen set"
      "futon3a/holes/labs/M-memes-arrows/cascade_serve.py:27 shown = full[:budget]"
      "futon3a/holes/labs/M-memes-arrows/cascade_serve.py:30 only the semilattice is recomputed on the prefix"
      "futon3a/holes/labs/M-memes-arrows/cascade_serve.py:36-37 the emitted coverage-reward/prior-cost/cascade-score are the untruncated ones"]
     :live-construction-not-attempted
     "python3 cascade_serve.py raises ModuleNotFoundError: No module named 'sentence_transformers' (cascade_construct.py:56) on this machine, so the replay is over the recorded artifact."}))

;; --------------------------------------------------------------------------
;; G3 -- "Specify and persist the cascade-habit return event"
;; Refuted if :cascade-prior-state already exists and something increments it.
;; --------------------------------------------------------------------------

(def g3
  (let [cascade-state (hits #":cascade-prior-state")
        habit-state (hits #":habit-prior-state")
        prior-requires (hits #"futon2\.aif\.cascade-prior|aif\.cascade-prior\s+:as")]
    {:hole "Specify and persist the cascade-habit return event"
     :refutation-attempted
     "Enumerate every mention of :cascade-prior-state in the source/script/test trees, and every namespace that requires cascade-prior. Refuted if a non-test caller persists or increments it."
     :cascade-prior-state-mentions cascade-state
     :habit-prior-state-mentions habit-state
     :cascade-prior-requiring-namespaces (vec (sort (distinct (map first prior-requires))))
     :non-test-callers
     (vec (sort (distinct (remove #(str/starts-with? % "test/") (map first prior-requires)))))}))

;; --------------------------------------------------------------------------
;; G4 -- "Run a dark end-to-end hierarchy shadow"
;; Refuted if a committed shadow report exists. Probe: the shadow-rank call
;; sites, and the lab directory the mission's artifacts would live in.
;; --------------------------------------------------------------------------

(def g4
  (let [ranks (filterv (fn [[p _ l]]
                         (and (str/includes? l "shadow-rank")
                              (str/includes? p "cascade_prior")))
                       (hits #"shadow-rank"))]
    {:hole "Run a dark end-to-end hierarchy shadow"
     :refutation-attempted
     "Enumerate cascade-prior/shadow-rank's call sites and look for the mission's lab directory. Refuted if a committed report states the useful-case fraction and the failure rates."
     :shadow-rank-sites ranks
     :non-test-call-sites (vec (sort (distinct (remove #(str/starts-with? % "test/") (map first ranks)))))
     :lab-dir "holes/labs/M-wm-aif-policy-grain-compliance"
     :lab-dir-exists? (fs/exists? (str futon2 "/holes/labs/M-wm-aif-policy-grain-compliance"))
     :lab-dirs-present (vec (sort (map fs/file-name (filter fs/directory? (fs/list-dir (str futon2 "/holes/labs"))))))}))

;; --------------------------------------------------------------------------
;; E5 -- "Collect a default-off EIG shadow and calibration packet"
;; Refuted if the five telemetry fields the acceptance names are persisted
;; anywhere.
;; --------------------------------------------------------------------------

(def e5
  (let [fields {:prior-entropy #"prior-entropy"
                :expected-posterior-entropy #"expected-posterior-entropy"
                :expected-information-gain-key #":expected-information-gain"
                :degeneracy-reason #"degeneracy"
                :realised-information-gain #"real[is]zed-information-gain"}]
    {:hole "Collect a default-off EIG shadow and calibration packet"
     :refutation-attempted
     "Enumerate, over the whole source/script/test tree, every mention of the five fields the acceptance requires persisted. Refuted if they are already written."
     :fields-probed (into (sorted-map) (map (fn [[k re]] [k (hits re)])) fields)
     :eig-kernel-call-sites (hits #"epistemic-value/expected-information-gain")
     :lab-dir "holes/labs/M-aif-policy-conditioned-eig"
     :lab-dir-exists? (fs/exists? (str futon2 "/holes/labs/M-aif-policy-conditioned-eig"))}))

(def record
  {:schema :wm/b2-strawman-reads-v1
   :row :B2
   :at "2026-09-05"
   :what-this-is
   "Measured output of five refutation attempts, produced by running them. Each
    probe reports the set it searched as well as the set it found."
   :search-space
   {:trees ["src" "scripts" "test"]
    :clj-file-count (count (clj-files))}
   :G1 g1 :G2 g2 :G3 g3 :G4 g4 :E5 e5})

(let [dir (io/file (or (first *command-line-args*)
                       (str futon2 "/holes/labs/wm-contract/runs/B2-strawman")))
      f (io/file dir "02-read-and-replay-probes.edn")]
  (.mkdirs dir)
  (spit f (with-out-str (pp/pprint record)))
  (println "wrote" (str f))
  (println "G1 candidate-source values:" (:candidate-source-values g1))
  (println "G1 arguing-worlds emits :semilattice?" (:arguing-worlds-emits-semilattice? g1))
  (println "G2 emitted coverage" (:emitted-coverage-reward g2) "vs prefix" (:prefix-coverage-reward g2)
           "difference" (:coverage-difference g2))
  (println "G3 :cascade-prior-state mentions:" (count (:cascade-prior-state-mentions g3))
           "non-test cascade-prior callers:" (:non-test-callers g3))
  (println "G4 shadow-rank non-test call sites:" (:non-test-call-sites g4)
           "lab dir exists?" (:lab-dir-exists? g4))
  (println "E5 field hit counts:" (into (sorted-map) (map (fn [[k v]] [k (count v)])) (:fields-probed e5))
           "lab dir exists?" (:lab-dir-exists? e5)))
