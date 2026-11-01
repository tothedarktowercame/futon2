(ns futon2.aif.relation-derivation-test
  "WM-RELATION-I: a target with no row derives its relation from (a) stated
  relations in its task text, then (b) the observation's pinned Giraffe graph,
  then (c) its nearest rowed target in the checked, inputs-pinned structure
  embedding, re-hashed against the pins; else :unknown with the reason. A hand
  row wins; tickets classify their parent through the same producer. Live pin: M-autoclock-in against the
  real facets file (resources/wm/focus/commit-facets-v1.json) and the real
  pinned embedding (futon6/data/mission-structure-embed, sha256 57ab4aa4… and
  0c2d76be…) derives associated/WM via M-aif4iad at 0.3777004044318593,
  runner-up M-futonzero-generative 0.11165555990299686 (the fourth flight
  refused :class-unknown-no-scalar-g on this target, flight-e70b4baf)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.focus-receipt :as fr]
            [futon2.aif.load-identity :as identity]))

(def inputs (fr/read-inputs))

(def discovery
  ;; the war machine's retained focus: discovered at the last window's end,
  ;; retained at the decision time (WM active, APM background)
  (let [ret (last (sort (map :valid-through (:windows inputs))))
        est (fr/discover inputs ret nil)]
    (fr/discover inputs "2026-09-26T02:00:00Z" {:focus (:focus est) :as-of ret})))

(def as-of "2026-09-26T02:00:00Z")

(defn- text-fn [m]
  (fn [t] (when-let [txt (get m t)] {:path (str "/fixture/" t ".md") :text txt :sha256 (str "sha-" t)})))

(defn- relations [& ids]
  (str "# M\n\n## Relations\n\n" (apply str (for [i ids] (str "- [[" i "]] related\n"))) "\n## Next\n"))

(defn- classify [inputs target ctx] (fr/classify-target inputs discovery as-of target ctx))

(def live-giraffe-run
  "/home/joe/code/storage/giraffe/20261101T044121.422Z-730407c5")

(defn- graph-run [links]
  (let [dir (doto (io/file (System/getProperty "java.io.tmpdir")
                           (str "relation-graph-" (System/nanoTime))) .mkdirs)
        input (io/file dir "input.edn")
        bytes (.getBytes (pr-str {:edges [] :reading-evidence {:links links}}) "UTF-8")
        pin (identity/sha256 bytes)]
    (spit input (String. bytes "UTF-8"))
    (spit (io/file dir "manifest.edn")
          (pr-str {:run-id "fixture-graph" :files [{:file "input.edn" :sha256 pin}]}))
    (.getPath dir)))

(defn- graph-link [from to kind basis]
  {:from from :to to :kind kind :basis basis
   :evidence {:path "/fixture/source.md" :line 1 :quote "fixture" :sha256 "fixture-pin"}})

(defn- row-of [& rows]
  (let [m (into {} (map (juxt :target identity) rows))] #(get m %)))

(deftest a-hand-row-wins-over-a-stated-path
  (let [ins (update inputs :relations conj {:target "M-hand" :relation "focus" :facet "WM"
                                           :effective-from "2026-09-20T00:00:00Z" :source {:repo "fixture"}})
        c (classify ins "M-hand" {:mission-text-fn (text-fn {"M-hand" (relations "M-aif4iad")})})]
    (is (= :focus (:class c)) "its own row, not aif4iad's associated")
    (is (nil? (:derived-via c)))))

(deftest a-ticket-target-is-unchanged
  (let [dir (doto (io/file (System/getProperty "java.io.tmpdir") (str "rel-ticket-" (System/nanoTime))) .mkdirs)
        _ (spit (io/file dir "T-x.md") "# T-x\n\nParent: M-aif4iad\n")
        called (atom false)
        c (classify inputs "T-x" {:ticket-dir (str dir) :mission-text-fn (fn [_] (reset! called true) nil)})]
    (is (= :associated (:class c)))
    (is (= :ticket-parent (get-in c [:derived-via :kind])))
    (is (false? @called) "no Relations read for a T- target")))

(deftest a-pin-mismatch-refuses-the-embedding-typed
  (let [ins (update-in inputs [:embedding :source-pins]
                       (fn [ps] (mapv #(if (.endsWith (str (:path %)) ".npy") (assoc % :sha256 "0000") %) ps)))
        c (classify ins "M-autoclock-in" {:mission-text-fn (text-fn {"M-autoclock-in" (relations "M-no-row")})})]
    (is (= :unknown (:class c)))
    (is (= :embedding-pin-mismatch (get-in c [:relation :reason])))
    (is (= "0000" (get-in c [:relation :derivation :embedding :mismatch 0 :pinned])))))

(deftest a-declared-floor-above-the-cosine-keeps-it-unknown-typed
  (let [c (classify (assoc inputs :embedding-floor 0.5) "M-autoclock-in"
                    {:mission-text-fn (text-fn {"M-autoclock-in" (relations "M-no-row")})})]
    (is (= :unknown (:class c)))
    (is (= :embedding-below-floor (get-in c [:relation :reason])))
    (is (= {:nearest "M-aif4iad" :cosine 0.3777004044318593 :floor 0.5}
           (select-keys (get-in c [:relation :derivation :embedding]) [:nearest :cosine :floor])))))

(deftest declared-zero-floor-preserves-neighbour-derivation
  (let [c (classify inputs "M-autoclock-in" {:mission-text-fn (text-fn {"M-autoclock-in" (relations "M-no-row")})})]
    (is (= :associated (:class c)))
    (is (= 0.0 (get-in c [:derived-via :floor])))
    (is (= {:absent :no-stated-path-to-a-classified-target} (get-in c [:derived-via :stated-relation])))))

(deftest missing-floor-refuses-embedding-derivation-by-name
  (let [c (classify (dissoc inputs :embedding-floor) "M-autoclock-in"
                    {:mission-text-fn (text-fn {"M-autoclock-in" (relations "M-no-row")})})]
    (is (= :unknown (:class c)))
    (is (= :embedding-floor-not-declared (get-in c [:relation :reason])))))

(deftest missing-graph-parameter-refuses-by-name
  (let [c (classify (dissoc inputs :graph-hop-bound) "E-no-such-node"
                    {:giraffe-run-dir live-giraffe-run :mission-text-fn (constantly nil)})]
    (is (= :unknown (:class c)))
    (is (= :graph-hop-bound-not-declared (get-in c [:relation :reason])))))

(deftest graph-link-kind-and-basis-parameters-are-enforced
  (let [anchor {:target "M-anchor" :relation "focus"}
        rows (row-of anchor)
        superseded (graph-run [(graph-link "E-target" "M-anchor" "superseded-by" "declared")])
        inferred (graph-run [(graph-link "E-target" "M-anchor" "relates-to" "inferred")])]
    (is (= :no-graph-path-to-classified-target
           (:absent (fr/giraffe-graph-path inputs "E-target" rows superseded))))
    (is (= :no-graph-path-to-classified-target
           (:absent (fr/giraffe-graph-path inputs "E-target" rows inferred))))
    (is (= "M-anchor"
           (get-in (fr/giraffe-graph-path
                    (update inputs :graph-link-bases conj "inferred")
                    "E-target" rows inferred)
                   [:derived-via :anchor])))))

(deftest equal-distance-anchors-with-different-classes-refuse
  (let [run (graph-run [(graph-link "E-target" "M-focus" "relates-to" "declared")
                        (graph-link "E-target" "M-elsewhere" "relates-to" "declared")])
        rows (row-of {:target "M-focus" :relation "focus"}
                     {:target "M-elsewhere" :relation "useful-elsewhere"})
        result (fr/giraffe-graph-path inputs "E-target" rows run)]
    (is (= :graph-anchors-disagree (:absent result)))
    (is (= [{:node "M-elsewhere" :class :useful-elsewhere}
            {:node "M-focus" :class :associated}]
           (:anchors result)))))

(deftest live-november-graph-paths-are-pinned
  (let [ctx (assoc (fr/relation-context "/home/joe/code")
                   :giraffe-run-dir live-giraffe-run)
        [pattern interim comprehension]
        (mapv #(fr/classify-target inputs discovery "2026-11-01T06:02:00Z" % ctx)
              ["M-pattern-retrieval-calibration" "M-interim-director"
               "E-comprehension-foundation"])]
    (is (= [:associated :associated :associated]
           (mapv :class [pattern interim comprehension])))
    (is (= [:giraffe-graph :embedding-neighbour :giraffe-graph]
           (mapv #(get-in % [:derived-via :kind]) [pattern interim comprehension])))
    (is (= [3 3] (mapv #(get-in % [:derived-via :hops]) [pattern comprehension])))
    (is (= ["M-apm-capability-ratchet" "M-wm-aif-policy-grain-compliance"]
           (mapv #(get-in % [:derived-via :anchor]) [pattern comprehension])))
    (is (every? #(= "730407c514e2fd21905da2b533d92e046a454372c919fbd89089fc953ebf1667"
                     (get-in % [:derived-via :input-sha]))
                [pattern comprehension]))))

(deftest excursion-uses-the-stated-relation-producer
  (let [c (classify inputs "E-fixture"
                    {:mission-text-fn (text-fn {"E-fixture" (relations "M-aif4iad")})})]
    (is (= :associated (:class c)))
    (is (= :stated-relation (get-in c [:derived-via :kind])))
    (is (= "E-fixture" (get-in c [:derived-via :path 0 :from])))))

(deftest live-excursion-without-a-row-path-or-embedding-stays-typed-unknown
  (let [c (classify inputs "E-efe-education" (fr/relation-context "/home/joe/code"))]
    (is (= :unknown (:class c)))
    (is (= :embedding-node-not-retained (get-in c [:relation :reason])))))

(deftest live-ticket-inherits-its-parents-derived-chain
  (let [target "T-repair-occ-9b6c9871d59bde364a23f9c346ddc708e2f68f4a026d9afe0aac6c9d538f087d"
        c (classify inputs target (fr/relation-context "/home/joe/code"))]
    (is (= :associated (:class c)))
    (is (= :ticket-parent (get-in c [:derived-via :kind])))
    (is (= "M-autonomous-pattern-lifecycle" (get-in c [:derived-via :parent])))
    (is (= :embedding-neighbour (get-in c [:derived-via :then :kind])))
    (is (= 0.0 (get-in c [:derived-via :then :floor])))))

(deftest live-november-picks-obey-the-declared-floor
  (let [ctx (fr/relation-context "/home/joe/code")
        targets ["M-pattern-retrieval-calibration" "M-interim-director"]
        at-zero (mapv #(classify inputs % ctx) targets)
        at-point-one (mapv #(classify (assoc inputs :embedding-floor 0.1) % ctx) targets)]
    (is (= [:associated :associated] (mapv :class at-zero)))
    (is (= [-0.013961024444296847 0.07298983447075927]
           (mapv #(get-in % [:derived-via :cosine]) at-zero)))
    (is (= [0.0 0.0] (mapv #(get-in % [:derived-via :floor]) at-zero)))
    (is (= [:unknown :unknown] (mapv :class at-point-one)))
    (is (= [:embedding-below-floor :embedding-below-floor]
           (mapv #(get-in % [:relation :reason]) at-point-one)))
    (is (= [0.1 0.1]
           (mapv #(get-in % [:relation :derivation :embedding :floor]) at-point-one)))))

(defn- census-source [receipt]
  (or (get-in receipt [:derived-via :kind])
      (get-in receipt [:relation :reason])
      :declared))

(deftest live-open-task-relation-census
  (let [ids (:ids (edn/read-string
                   (slurp "holes/labs/wm-contract/witnesses/witnesses-open-tasks-2026-11-01.edn")))
        ctx (assoc (fr/relation-context "/home/joe/code")
                   :giraffe-run-dir live-giraffe-run)
        receipts (map #(fr/classify-target inputs discovery "2026-11-01T06:02:00Z" % ctx) ids)
        table (frequencies
               (map (fn [r] [(subs (:target r) 0 2) (:class r) (census-source r)])
                    receipts))]
    (is (= 626 (count receipts)))
    (is (= 72 (get table ["E-" :associated :giraffe-graph])))
    (is (= 49 (reduce + (for [[[prefix class _] n] table
                              :when (and (= "T-" prefix) (not= :unknown class))]
                          n))))
    (is (= 251 (count (remove #(= :unknown (:class %)) receipts))))))

(deftest m-autoclock-in-live
  (let [c (classify inputs "M-autoclock-in" {:code-root "/home/joe/code"})
        d (:derived-via c)]
    (is (= :associated (:class c)))
    (is (= {:target "M-aif4iad" :relation "associated" :facet "WM"}
           (select-keys (:relation c) [:target :relation :facet])))
    (is (= {:kind :embedding-neighbour :neighbour "M-aif4iad" :cosine 0.3777004044318593
            :runner-up ["M-futonzero-generative" 0.11165555990299686]}
           (select-keys d [:kind :neighbour :cosine :runner-up])))
    (is (= {:absent :no-stated-path-to-a-classified-target} (:stated-relation d))
        "its Relations reach M-operational-vocabulary, which has no row and no Relations section")
    (is (= #{"57ab4aa46319eed4f29a5b0d9f84c86e14f0d30c8ddca906ef45ca0d9ac100fb"
             "0c2d76be8ed53d4bdc2c52e3cd551baf43d7a31f62d510d0791633ad4ac3abe4"}
           (set (map :sha256 (:pins d)))))))

(deftest a-stated-path-is-followed-two-hops-and-no-further
  (let [one (classify inputs "M-a" {:mission-text-fn (text-fn {"M-a" (relations "M-no-row" "M-aif4iad")})})
        two (classify inputs "M-a" {:mission-text-fn (text-fn {"M-a" (relations "M-b") "M-b" (relations "M-aif4iad")})})
        three (classify inputs "M-a" {:mission-text-fn (text-fn {"M-a" (relations "M-b") "M-b" (relations "M-c")
                                                                  "M-c" (relations "M-aif4iad")})})]
    (is (= :associated (:class one)))
    (is (= {:kind :stated-relation :parent "M-aif4iad"} (select-keys (:derived-via one) [:kind :parent])))
    (is (= ["M-a"] (mapv :from (get-in one [:derived-via :path]))))
    (is (= "- [[M-aif4iad]] related" (get-in one [:derived-via :path 0 :quote])))
    (is (= ["M-a" "M-b"] (mapv :from (get-in two [:derived-via :path]))))
    (is (= :unknown (:class three)) "three hops is beyond the bound; M-a has no embedding stem")
    (is (= :embedding-node-not-retained (get-in three [:relation :reason])))))

(deftest a-missing-graph-parameter-does-not-fall-through-to-the-embedding
  ;; claude-2, L5-4j review: M-pattern-retrieval-calibration has an embedding
  ;; neighbour; with the hop bound undeclared it must read unknown by the
  ;; parameter's name, not associated through the embedding.
  (let [inputs (fr/read-inputs)
        as-of "2026-11-01T06:02:00Z"
        derived (:info (fr/decision-focus inputs as-of nil))
        ctx (assoc (fr/relation-context "/home/joe/code") :giraffe-run-dir live-giraffe-run)
        r (fr/classify-target (dissoc inputs :graph-hop-bound) derived as-of "M-pattern-retrieval-calibration" ctx)]
    (is (= :unknown (:class r)))
    (is (= :graph-hop-bound-not-declared (get-in r [:relation :reason])))
    (is (nil? (get-in r [:derived-via :kind])))))
