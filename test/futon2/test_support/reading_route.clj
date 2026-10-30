(ns futon2.test-support.reading-route
  "A shared real fixture for the Q5 reading-family decision route: a pinned
  pattern graph, a current 象 reading, a declared beta, the real
  explicit-absence flight record as a complete history ledger (so every
  never-run policy gets the certified zero-history carrier), and helpers for
  admitted prefixes and identity-bound beta declarations. Nothing here
  defaults a gate: every input is the one the production decision reads."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.enactment-fold-source :as fold-source]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.occurrence-square-registry :as occurrence-square-registry]
            [futon2.aif.pattern-graph-pin :as graph-pin]
            [futon2.aif.policy :as policy]
            [futon2.aif.policy-prefix-admission :as admission]
            [futon2.aif.reading-policy-precision :as precision]
            [futon2.aif.target-reading-registry :as reading-registry]
            [futon2.aif.three-halves-square :as three-halves-square]
            [futon2.aif.ticket-queue :as ticket-queue]
            [futon2.aif.wm.cascade-decision :as wm-cd])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def target "M-current")
(def real-flight "test/fixtures/prefix-history/flight-278b6988.edn")

(defn temp-dir [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn with-empty-learning-ledger
  "clojure.test :each fixture: an empty, private learning-trial ledger, so
  theta is the documented prior and no real store is read."
  [run]
  (with-redefs [ledger/default-root (.getPath (temp-dir "reading-route-ledger-"))]
    (run)))

(def library-patterns
  "The graph's three patterns, and p/d, a library pattern outside the graph
  (a reading that names it records :seed-not-in-graph)."
  ["p/a" "p/b" "p/c" "p/d"])

(defn- pattern-library
  "A pattern library of LIBRARY-PATTERNS, one <root>/<pattern>.flexiarg
  file each, as the production library is laid out."
  []
  (let [root (temp-dir "reading-route-library-")]
    (doseq [p library-patterns]
      (let [f (io/file root (str p ".flexiarg"))]
        (.mkdirs (.getParentFile f))
        (spit f (str "@flexiarg " p "\n"
                     "+ IF: fixture condition for " p "\n"
                     "+ HOWEVER: fixture contrast for " p "\n"
                     "+ BECAUSE: fixture reason for " p "\n"
                     "+ THEN: fixture consequence for " p "\n"
                     "! conclusion: fixture pattern " p "\n"))))
    root))

(defn library-sha256
  "The sha256 of PATTERN's file in the library at ROOT."
  [root pattern]
  (identity/sha256 (Files/readAllBytes (.toPath (io/file root (str pattern ".flexiarg"))))))

(defn- analysis
  "One sentence, one fragment per element of PATTERNS: a pattern id, or a
  vector of the ids one fragment cites over the same span."
  [library patterns]
  {:status "analyzed"
   :sentences
   [{:id "s1"
     :fragments (vec (map-indexed
                      (fn [i cited]
                        (let [cited (if (string? cited) [cited] cited)]
                          {:start i :end (inc i) :text (str/join " " cited) :intent "act"
                           :target "target" :rationale "fixture fit" :relations ["action"]
                           :pattern_refs (vec (for [pattern cited]
                                                {:id pattern :status "candidate"
                                                 :rationale "fixture fit"
                                                 ;; the library bytes the reading cited
                                                 :source_sha256 (library-sha256 library pattern)}))}))
                      patterns))}]})

(defn- pinned-graph []
  (let [file (io/file (temp-dir "reading-route-graph-") "graph.json")]
    (spit file (json/generate-string
                {:records 1 :patterns 3 :pattern_ids ["p/a" "p/b" "p/c"]
                 :summary [{:through "why" :edges 2} {:through "how" :edges 0}]
                 :edges [{:a "p/a" :b "p/b" :kind "why" :evidence []}
                         {:a "p/b" :b "p/c" :kind "why" :evidence []}]}))
    (graph-pin/pin! file)
    file))

(defn fixture
  "M-current with a current reading naming READ (default p/a and p/b, whose
  policies owe nothing), a pinned three-pattern graph, and a complete flight
  ledger holding the real explicit-absence record."
  ([] (fixture ["p/a" "p/b"]))
  ([read]
   (let [root (temp-dir "reading-route-root-")
         current (io/file root (str target ".md"))
         flights (temp-dir "reading-route-flights-")
         library (pattern-library)]
     (spit current "# Current\n\n## HEAD\nCurrent work.\n")
     (io/copy (io/file real-flight) (io/file flights "flight-278b6988.edn"))
     (let [digest (reading-registry/excerpt-digest current)]
       (reading-registry/publish!
        root {:target-id target :source-path (.getPath current)
              :excerpt-digest digest
              :request {:task {:target_id target :excerpt_sha256 digest}}
              :analysis (analysis library read) :validator-version 1}))
     {:root root :flights flights :graph (pinned-graph) :library library
      :row {:target-id target :source-kind :head
            :source-path (.getPath current) :source-absent nil}})))

(defn assembled
  "The assembly the decision receives for the fixture's one target, with
  its declared beta (none when BETA is nil)."
  [fx beta]
  {:problems []
   :refusals [{:target target :kind :no-admitted-interpretation}]
   :target-sources [(:row fx)]
   :declared-betas (if beta {target beta} {})})

(defn ledger-steps
  "The fixture's flight ledger, read as production reads it."
  [fx]
  (fold-source/conditioning-steps (str (:flights fx))))

(defn opts
  "The production decision options for FX, merged with EXTRA."
  [fx extra]
  (merge {:task {:id target}
          :outer/makeup-path (.getCanonicalPath (io/file "data/wm-learning/outer-makeup.edn"))
          :pattern-graph-path (str (:graph fx))
          :pattern-library-root (str (:library fx))
          :target-reading-root (str (:root fx))
          :conditioning-steps (ledger-steps fx)
          :ticket-queue ticket-queue/empty-declaration
          :novelty-inputs {}}
         extra))

(defn decide
  "The production cascade decision for FX at declared BETA (1 by default),
  with EXTRA options. Returns the result with :selector-calls."
  ([fx] (decide fx 1 {}))
  ([fx beta extra]
   (let [calls (atom 0)
         select policy/select-action-cascades]
     (with-redefs [policy/select-action-cascades
                   (fn [& args] (swap! calls inc) (apply select args))]
       (assoc (wm-cd/cascade-decision (assembled fx beta) (opts fx extra))
              :selector-calls @calls)))))

(defn refusal-of
  "The ex-data of a decision that throws, else nil."
  [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e))))

(defn policy-keys
  "Every compared policy's history key in DECISION, by policy id."
  [decision]
  (into (sorted-map)
        (for [c (get-in decision [:selection-certificate :candidates])]
          [(get-in c [:id :id]) (admission/candidate-key (:id c))])))

(defn admitted-step
  "One admitted (:present) conditioning step for policy KEY with free
  energy F, in the flight writer's shape, at occurrence OCC."
  [key f occ]
  {:step {:schema :wm/conditioning-step-v1 :status :present :policy-key key
          :occurrence {:flight "reading-route-fixture" :click occ}
          :s-prev {:value {#{} 1}} :q {#{} 1} :f f}
   :path "reading-route-fixture" :sha256 "fixture"})

(defn with-prefixes
  "Conditioning steps giving each policy key in KEY->F its admitted
  prefix with that F, on top of the fixture's complete ledger."
  [fx key->f]
  (let [base (ledger-steps fx)
        added (vec (map-indexed (fn [i [k f]] (admitted-step k f (str "click-" i))) key->f))]
    (-> base
        (update :steps into added)
        ;; the census reads records, so the added steps are a read record too
        (update :read conj {:path "reading-route-fixture" :sha256 "fixture"
                            :steps (count added) :enactments (count added)
                            :unattributed-enactments 0}))))

(defn declaration
  "A sealed beta declaration for the identity a refused carry names."
  [refused-carry beta]
  (precision/declare-beta (get-in refused-carry [:rearm :identity]) beta
                          {:status :declared :version "operator declaration v1"}))

(defn publish-target!
  "Publish target T's READ into FIELD and return its target-source row."
  [{:keys [root library squares]} t read]
  (let [f (io/file root (str t ".md"))
        pattern-texts (occurrence-square-registry/library-resolver library)
        author-maps (into {}
                          (for [role three-halves-square/roles]
                            [role {:element (str "fixture situation " (name role))
                                   :source "reading-route fixture"}]))]
    (when-not (.isFile f)
      (spit f (str "# " t "\n\n## HEAD\nWork on " t ".\n")))
    (when read
      (let [digest (reading-registry/excerpt-digest f)
            analysis (analysis library read)]
        (reading-registry/publish!
         root {:target-id t :source-path (.getPath f)
               :excerpt-digest digest
               :request {:task {:target_id t :excerpt_sha256 digest}}
               :analysis analysis :validator-version 1})
        (let [reading (reading-registry/current-reading root t digest)
              analysis-digest (:analysis-digest reading)
              entries
              (into {}
                    (mapcat
                     (fn [[i cited]]
                       (for [pattern (if (string? cited) [cited] cited)
                             :let [pattern-file (io/file library (str pattern ".flexiarg"))
                                   pattern-text (slurp pattern-file)
                                   pattern-sha (library-sha256 library pattern)
                                   occurrence-id [pattern i]]]
                         [occurrence-id
                          {:occurrence-id occurrence-id
                           :pattern pattern
                           :pattern-sha256 pattern-sha
                           :square
                           (three-halves-square/build-from-situation-maps
                            pattern pattern-text
                            {:path (.getPath pattern-file) :sha256 pattern-sha}
                            author-maps)}]))
                     (map-indexed vector read)))
              published
              (occurrence-square-registry/publish!
               squares
               {:target-id t
                :excerpt-digest digest
                :analysis-digest analysis-digest
                :author "reading-route-fixture"
                :entries entries}
               pattern-texts)
              current
              (occurrence-square-registry/current
               squares t analysis-digest pattern-texts)]
          (when-not (= published current)
            (throw (ex-info "Reading-route occurrence squares invalid"
                            {:target-id t :published published :current current}))))))
    {:target-id t :source-kind :head :source-path (.getPath f)
     :source-absent nil}))

(defn field
  "Several targets in one field: TARGET->READ maps each target id to the
  patterns its current reading names, or nil for a target whose mission
  file has no reading. One pinned graph and one complete ledger serve all."
  [target->read]
  (let [field {:root (temp-dir "reading-route-field-")
               :flights (temp-dir "reading-route-field-flights-")
               :graph (pinned-graph)
               :library (pattern-library)
               :squares (temp-dir "reading-route-field-squares-")}]
    (io/copy (io/file real-flight) (io/file (:flights field) "flight-278b6988.edn"))
    (assoc field :rows (mapv (fn [[t read]] (publish-target! field t read))
                             target->read))))

(defn republish!
  "FIELD with target T's reading replaced by READ (or added, for a new
  target), published into the field's own roots so every other target's
  squares and cascade digests are untouched."
  [field t read]
  (let [row (publish-target! field t read)]
    (update field :rows
            (fn [rows]
              (conj (vec (remove #(= t (:target-id %)) rows)) row)))))

(defn decide-field
  "The production decision over FIELD's targets with TARGET->BETA declared
  betas and EXTRA options; :selector-calls counted."
  [fld target->beta extra]
  (let [calls (atom 0)
        select policy/select-action-cascades]
    (with-redefs [policy/select-action-cascades
                  (fn [& args] (swap! calls inc) (apply select args))]
      (assoc (wm-cd/cascade-decision
              {:problems []
               :refusals (mapv #(hash-map :target (:target-id %) :kind :no-admitted-interpretation)
                               (:rows fld))
               :target-sources (:rows fld)
               :declared-betas target->beta}
              (opts fld extra))
             :selector-calls @calls))))
