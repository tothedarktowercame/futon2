(ns futon2.test-support.reading-route
  "A shared real fixture for the Q5 reading-family decision route: a pinned
  pattern graph, a current 象 reading, a declared beta, the real
  explicit-absence flight record as a complete history ledger (so every
  never-run policy gets the certified zero-history carrier), and helpers for
  admitted prefixes and identity-bound beta declarations. Nothing here
  defaults a gate: every input is the one the production decision reads."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [futon2.aif.enactment-fold-source :as fold-source]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.pattern-graph-pin :as graph-pin]
            [futon2.aif.policy :as policy]
            [futon2.aif.policy-prefix-admission :as admission]
            [futon2.aif.reading-policy-precision :as precision]
            [futon2.aif.target-reading-registry :as reading-registry]
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

(defn- analysis [patterns]
  {:status "analyzed"
   :sentences
   [{:id "s1"
     :fragments (vec (map-indexed
                      (fn [i pattern]
                        {:start i :end (inc i) :text pattern :intent "act" :target "target"
                         :rationale "fixture fit" :relations ["action"]
                         :pattern_refs [{:id pattern :status "candidate"
                                         :rationale "fixture fit"
                                         :source_sha256 (apply str (repeat 64 "a"))}]})
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
         flights (temp-dir "reading-route-flights-")]
     (spit current "# Current\n\n## HEAD\nCurrent work.\n")
     (io/copy (io/file real-flight) (io/file flights "flight-278b6988.edn"))
     (let [digest (reading-registry/excerpt-digest current)]
       (reading-registry/publish!
        root {:target-id target :source-path (.getPath current)
              :excerpt-digest digest
              :request {:task {:target_id target :excerpt_sha256 digest}}
              :analysis (analysis read) :validator-version 1}))
     {:root root :flights flights :graph (pinned-graph)
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
  (merge {:pattern-graph-path (str (:graph fx))
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
