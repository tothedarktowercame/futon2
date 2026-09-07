#!/usr/bin/env bb
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.java.shell :as sh]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def root "/home/joe/code")
(def futon3 (str root "/futon3"))
(def pin "cdb5e8a56fd907beb6a99f8b88af9de50ff93126")
(def lab (str root "/futon2/holes/labs/wm-contract"))
(def census-path (str lab "/runs/F12-organise/20-o4-edges-census.edn"))
(def out-path (str lab "/runs/F12-organise/23-snatch-joint.edn"))
(def findings (atom []))
(defn fail! [finding detail] (swap! findings conj (sorted-map :detail detail :finding finding)))

(def head (str/trim (:out (sh/sh "git" "-C" futon3 "rev-parse" "HEAD"))))
(when-not (= pin head) (fail! :futon3-pin-moved {:expected pin :actual head}))

(def expression
  (str "(load-file \"checks/find_organise.clj\") "
       "(require '[clojure.edn :as edn]) "
       "(let [n (find-ns 'find-organise) r #(deref (ns-resolve n %)) "
       "fixture (edn/read-string (slurp \"checks/snatch-cascade.edn\")) "
       "repo ((r 'read-repository) \"library\" [:snatch]) "
       "rows ((r 'cascade-diff-table) fixture repo) laws (r 'organise-laws) "
       "repro (r 'organise-reproduces-record?)] "
       "(prn (mapv (fn [row] {:row row "
       ":laws (into (sorted-map) (map (fn [[k f]] [k (boolean (f row))]) laws)) "
       ":reproduces-record? (boolean (repro row))}) rows)))"))
(def subprocess (sh/sh "bb" "-cp" "checks" "-e" expression :dir futon3))
(when-not (zero? (:exit subprocess))
  (fail! :find-organise-subprocess-failed
         {:exit (:exit subprocess) :err (:err subprocess)}))
(def derived (if (zero? (:exit subprocess)) (edn/read-string (:out subprocess)) []))

(defn stable-value [x]
  (cond
    (set? x) (vec (sort-by pr-str (map stable-value x)))
    (map? x) (into (sorted-map) (map (fn [[k v]] [k (stable-value v)]) x))
    (sequential? x) (mapv stable-value x)
    :else x))

(def rows
  (vec
   (sort-by :scenario
    (for [{:keys [row laws reproduces-record?]} derived]
      (let [edges (vec (sort-by pr-str (map vec (:edges row))))
            precedence-moved? (not= (:precedence-before row) (:precedence-after row))
            acting-moved? (not= (:acting-order-before row) (:acting-order-after row))
            score-moved? (not= (:score-before row) (:score-after row))]
        (sorted-map
         :acting-order-moved? acting-moved?
         :carries-edges-and-a-moved-precedence? (boolean (and (seq edges) precedence-moved?))
         :edge-count (count edges)
         :edges (stable-value edges)
         :o4-holds? (boolean (:O4 laws))
         :precedence-moved? precedence-moved?
         :reproduces-record? reproduces-record?
         :scenario (:scenario row)
         :score-after (:score-after row)
         :score-before (:score-before row)
         :score-moved? score-moved?))))))

(def census (edn/read-string (slurp census-path)))
(def snatch (first (filter #(= :snatch (:record-id %)) (:records census))))
(def census-gap
  (sorted-map
   :before-after-key-path (:before-after-key-path snatch)
   :cascade-edge-key-paths (mapv :key-path (get-in snatch [:cascade-edge-counts :per-run]))
   :carries-both? (:carries-both? snatch)
   :has-before-after-pair? (:has-before-after-pair? snatch)
   :o4-key-paths (get-in snatch [:o4-shape :key-paths])
   :o4-shape (get-in snatch [:o4-shape :shape])
   :pointers ["futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:238-249"
              "futon2:holes/labs/wm-contract/f12_o4_edges_census.bb:8-22"]))

(defn verdict [rs gap]
  (and (= 6 (count rs))
       (every? :carries-edges-and-a-moved-precedence? rs)
       (every? :o4-holds? rs)
       (= :absent (:o4-shape gap))
       (false? (:has-before-after-pair? gap))
       (false? (:carries-both? gap))
       (= "not found" (:before-after-key-path gap))
       (empty? (:cascade-edge-key-paths gap))
       (= ["not found"] (:o4-key-paths gap))
       (= "futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:238-249"
          (first (:pointers gap)))))

(when-not (verdict rows census-gap)
  (fail! :joint-census-verdict-failed {:rows rows :census-gap census-gap}))

(def mutations
  [{:plant :empty-edge-set
    :landed? (and (seq (:edges (first rows)))
                  (empty? (:edges (assoc (first rows) :edges [] :edge-count 0
                                         :carries-edges-and-a-moved-precedence? false))))
    :verdict-after (verdict (assoc rows 0 (assoc (first rows) :edges [] :edge-count 0
                                                :carries-edges-and-a-moved-precedence? false))
                            census-gap)}
   {:plant :flatten-precedence-after
    :landed? (:precedence-moved? (first rows))
    :verdict-after (verdict (assoc rows 0 (assoc (first rows) :precedence-moved? false
                                                :carries-edges-and-a-moved-precedence? false))
                            census-gap)}
   {:plant :drop-scenario
    :landed? (= 5 (count (pop rows)))
    :verdict-after (verdict (pop rows) census-gap)}
   {:plant :repoint-census-gap
    :landed? (not= "futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:238-249"
                   "futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:1")
    :verdict-after
    (verdict
     rows
     (assoc census-gap :pointers
            ["futon2:holes/labs/wm-contract/runs/F12-organise/20-o4-edges-census.edn:1"
             (second (:pointers census-gap))]))}])
(doseq [m mutations]
  (when-not (and (:landed? m) (false? (:verdict-after m)))
    (fail! :mutation-did-not-fail m)))

(def rows-both (mapv :scenario (filter :carries-edges-and-a-moved-precedence? rows)))
(def score-moved (mapv :scenario (filter :score-moved? rows)))
(def artifact
  (sorted-map
   :any-row-carries-edges-and-a-moved-precedence? (boolean (seq rows-both))
   :census-gap census-gap
   :findings (vec (sort-by :finding @findings))
   :futon3-pin pin
   :mutation-plants (mapv #(into (sorted-map) %) mutations)
   :record :F12-slice-8-snatch-joint-exemplar
   :rows rows
   :rows-carrying-both rows-both
   :rows-total (count rows)
   :rows-where-the-score-moved score-moved
   :source-pointers ["futon3:checks/find_organise.clj:603-633"
                     "futon3:checks/snatch-cascade.edn:84"
                     "futon3:checks/snatch-cascade.edn:210-211"
                     "futon3:checks/find-organise.edn:62-64"]
   :verdict (if (empty? @findings) :pass :fail)))

(io/make-parents out-path)
(spit out-path (with-out-str (pp/pprint artifact)))
(println (format "f12_snatch_joint_check: %s rows=%d both=%d artifact=%s"
                 (name (:verdict artifact)) (count rows) (count rows-both) out-path))
(when (= :fail (:verdict artifact)) (System/exit 1))
