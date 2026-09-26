(ns futon2.aif.relation-derivation-test
  "WM-RELATION-I: an M- target with no row derives its relation from (a) its
  mission's stated ## Relations, walked through M- targets (hop bound 2) to a
  rowed target, then (b) its nearest rowed neighbour in the pinned structure
  embedding, re-hashed against the pins; else :unknown with the reason. A hand
  row wins; T- targets are unchanged. Live pin: M-autoclock-in against the
  real facets file (resources/wm/focus/commit-facets-v1.json) and the real
  pinned embedding (futon6/data/mission-structure-embed, sha256 57ab4aa4… and
  0c2d76be…) derives associated/WM via M-aif4iad at 0.3777004044318593,
  runner-up M-futonzero-generative 0.11165555990299686 (the fourth flight
  refused :class-unknown-no-scalar-g on this target, flight-e70b4baf)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.focus-receipt :as fr]))

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
  (let [c (classify (assoc-in inputs [:embedding :min-cosine] 0.5) "M-autoclock-in"
                    {:mission-text-fn (text-fn {"M-autoclock-in" (relations "M-no-row")})})]
    (is (= :unknown (:class c)))
    (is (= :nearest-below-threshold (get-in c [:relation :reason])))
    (is (= {:nearest "M-aif4iad" :cosine 0.3777004044318593 :min-cosine 0.5}
           (select-keys (get-in c [:relation :derivation :embedding]) [:nearest :cosine :min-cosine])))))

(deftest with-no-floor-declared-the-neighbours-class-is-derived
  (let [c (classify inputs "M-autoclock-in" {:mission-text-fn (text-fn {"M-autoclock-in" (relations "M-no-row")})})]
    (is (= :associated (:class c)))
    (is (= {:absent :no-floor-declared} (get-in c [:derived-via :floor])))
    (is (= {:absent :no-stated-path-to-a-classified-target} (get-in c [:derived-via :stated-relation])))))

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
