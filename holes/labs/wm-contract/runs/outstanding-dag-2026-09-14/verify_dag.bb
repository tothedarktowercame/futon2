(require '[clojure.edn :as edn] '[clojure.java.io :as io]
         '[clojure.string :as str] '[clojure.walk :as walk]
         '[cheshire.core :as json])
(import '[java.security MessageDigest] '[java.nio.file Files])
(def root (.getParentFile (io/file *file*)))
(defn read-one [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (let [eof (Object.) x (edn/read {:eof eof} r)]
      (assert (not (identical? x eof)))
      (assert (identical? eof (edn/read {:eof eof} r))) x)))
(defn digest [f]
  (format "%064x" (java.math.BigInteger. 1 (.digest (MessageDigest/getInstance "SHA-256") (Files/readAllBytes (.toPath (io/file f)))))))
(def d (read-one (io/file root "dag.edn")))
(def pins (read-one (io/file root "source-pins.edn")))
(assert (= (:sources d) (:sources pins)))
(assert (= (walk/postwalk #(if (keyword? %) (subs (str %) 1) %) d)
           (json/parse-string (slurp (io/file root "dag.json")))))
(def nodes (:nodes d))
(def ids (set (map :id nodes)))
(assert (= (count ids) (count nodes)))
(doseq [n nodes]
  (assert (every? #(contains? n %) [:id :title :flag :paper-anchor :tracker :state :evidence]))
  (assert (contains? #{:outstanding :staged :honest-absence :parked :hidden-prereq} (:flag n)))
  ;; :done added 2026-09-14: the snapshot originally had no completion state,
  ;; so finished nodes stayed rendered as open (Joe: "they are all still red").
  (assert (contains? #{:unbuilt :built-not-wired :blocked :in-flight :ruled-parked :done} (:state n)))
  (assert (seq (:evidence n))))
(def sources (into {} (map (juxt :id identity) (:sources d))))
(doseq [s (:sources d)]
  (assert (= (:sha256 s) (digest (io/file root (:snapshot s)))))
  (assert (= (:bytes s) (.length (io/file root (:snapshot s))))))
(doseq [owner (concat nodes (:edges d) (:findings d) (:satisfied-baselines d)) e (:evidence owner)]
  (let [s (get sources (:source e))]
    (assert s)
    (assert (= (:sha256 s) (:sha256 e)))
    (assert (= (:snapshot s) (:snapshot e)))
    (assert (= (:file-line e) (str (:path s) ":" (:line e))))
    (assert (<= 1 (:line e) (:through e) (:lines s)))))
(doseq [e (:edges d)]
  (assert (and (ids (:from e)) (ids (:to e))))
  (assert (contains? #{:hard-prerequisite :evidence-prerequisite :ruling-prerequisite} (:kind e)))
  (assert (seq (:why e))))
(assert (= (count (:edges d)) (count (set (map (juxt :from :to :conditional-branch) (:edges d))))))
(def edges (remove :conditional-branch (:edges d)))
(def out (group-by :from edges))
(def order (get-in d [:metrics :topological-order]))
(def pos (zipmap order (range)))
(assert (= ids (set order)))
(assert (= (count ids) (count order)))
(doseq [{:keys [from to]} edges] (assert (< (pos from) (pos to))))
(def work (set (map :id (remove :occurrence-only nodes))))
(defn descendants [id]
  (reduce (fn [a e] (into (conj a (:to e)) (descendants (:to e)))) #{} (get out id)))
(doseq [r (get-in d [:metrics :reachability])]
  (assert (= (:downstream-work-nodes r) (count (filter work (descendants (:id r)))))))
(defn longest [id]
  (let [children (filter work (map :to (get out id)))]
    (if (seq children) (inc (apply max (map longest children))) 0)))
(doseq [c (get-in d [:metrics :longest-root-chains])]
  (assert (= (dec (count c)) (longest (first c))))
  (doseq [[a b] (partition 2 1 c)] (assert (some #(= b (:to %)) (get out a)))))
(def flags
  (for [s (:sources d) :when (str/starts-with? (name (:id s)) "paper-")
        [i line] (map-indexed vector (str/split-lines (slurp (io/file root (:snapshot s)))))
        [_ kind] (re-seq #"\\wkflag\{(OUTSTANDING|STAGED|HONEST ABSENCE)\}" line)]
    {:anchor (str (.getName (io/file (:path s))) ":" (inc i))
     :kind ({"OUTSTANDING" :outstanding "STAGED" :staged "HONEST ABSENCE" :honest-absence} kind)}))
(def paper (filter :occurrence-only nodes))
(assert (= (set flags) (set (map #(hash-map :anchor (:paper-anchor %) :kind (:flag %)) paper))))
(assert (= 13 (count paper)))
(assert (= {:outstanding 11 :staged 1 :honest-absence 1} (frequencies (map :flag paper))))
(assert (= 1 (count (filter :conditional-branch (:edges d)))))
(assert (empty? (get out :e6b-storage-chain)))
(def frontier (str/split-lines (slurp (io/file root "frontier.txt"))))
(doseq [line frontier] (assert (str/includes? (slurp (io/file root "PRIORITY.md")) line)))
(def drift (vec (for [s (:sources d) :when (not= (:sha256 s) (digest (:path s)))] (:path s))))
(prn {:status :pass :nodes (count nodes) :work-nodes (count work) :edges (count (:edges d))
      :paper-flags (frequencies (map :flag paper)) :source-snapshots (count sources)
      :dependency-cycle false :explicit-cycle-findings (mapv :id (:findings d))
      :maximum-chain-edges (apply max (map longest work)) :current-source-drift drift
      :checks [:strict-one-form :mirror-equality :required-fields :source-shas :evidence-anchors
               :edge-references :unique-nodes-edges :topological-order :reachability
               :longest-paths :all-paper-flags :conditional-ruling :parked-chain :verbatim-frontier]})
