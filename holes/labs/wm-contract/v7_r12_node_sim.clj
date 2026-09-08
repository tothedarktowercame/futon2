(ns v7-r12-node-sim
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

(def lab (io/file "holes/labs/wm-contract"))
(def equation-text (slurp (io/file lab "aif-equations.edn")))
(def catalogue-text (slurp "../p4ng/sec-catalog.tex"))
(def contract-text (slurp "docs/futon-aif-completeness.md"))
(def concordance-text (slurp "../p4ng/R-concordance.md"))
(def census-text (slurp (io/file lab "ALIGN-rnode-process-census.md")))

(defn read-stream [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [xs []]
      (let [x (edn/read {:eof ::eof
                         :default (fn [tag value] {:tag tag :value value})} r)]
        (if (= ::eof x) xs (recur (conj xs x)))))))

(def trace-files
  (->> (.listFiles (io/file "data/wm-trace"))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
       (sort-by #(.getName %))))
(def trace-records (vec (mapcat read-stream trace-files)))

(defn tree-values [x k]
  (cond
    (map? x) (concat (when (contains? x k) [(get x k)])
                     (mapcat #(tree-values % k) (vals x)))
    (sequential? x) (mapcat #(tree-values % k) x)
    :else []))

(defn route? [node record]
  (boolean
   (some #(and (map? %) (= node (:node %)))
         (mapcat #(if (sequential? %) % [%])
                 (concat (tree-values record :wm/route)
                         (tree-values record :route))))))

(def r12-records (vec (filter #(route? :R12 %) trace-records)))
(def calibration-keys
  [:layer-1 :layer-2 :calibration/layer-1 :calibration/layer-2
   :calibration-commission-id :calibration-checker :calibration-returned-artifact])
(def process-keys
  [:commission-id :commission/id :dispatch-id :dispatch/id :checker-id :reviewer-id])

(def checks
  [{:id :registry-declares-r12-plumbing
    :pass? (and (not (str/includes? equation-text ":node :R12"))
                (boolean (re-find #":plumbing\s+\[[^\]]*:R12" equation-text)))}
   {:id :catalogue-keeps-two-labels-and-only-layer-two-clears-value
    :pass? (and (str/includes? catalogue-text "Layer~1")
                (str/includes? catalogue-text "Layer~2")
                (str/includes? catalogue-text "Only Layer~2 clears value"))}
   {:id :contract-r12-is-hyperparameter-inference
    :pass? (and (str/includes? contract-text "R12 — Dual-loop / hyperparameter inference")
                (str/includes? contract-text "treats the inner loop's hyperparameters"))}
   {:id :concordance-declares-the-two-r12s-different
    :pass? (str/includes? concordance-text
                          "they are two different second loops")}
   {:id :align-census-keeps-r12-seven-cells-absent
    :pass? (boolean
            (re-find #"R12 — Two-layer calibration \| absent \[A\] \| absent \[A\] \| absent \[A\] \| absent \[A\] \| absent \[A\] \| absent \[A\] \| absent \[A\]"
                     census-text))}
   {:id :r12-route-is-only-attribution-in-corpus
    :pass? (and (seq r12-records)
                (every? #(not-any? (fn [k] (seq (tree-values % k)))
                                   calibration-keys)
                        r12-records))}
   {:id :r12-route-carries-no-process-identity
    :pass? (every? #(not-any? (fn [k] (seq (tree-values % k)))
                              process-keys)
                   r12-records)}])

(def plants
  [{:id :collapse-layer-labels
    :caught? (not= (:layer-1 {:layer-1 :internal})
                   (:layer-2 {:layer-2 :external}))}
   {:id :invent-returned-artifact
    :caught? (some? (first (tree-values {:calibration-returned-artifact "x"}
                                        :calibration-returned-artifact)))}
   {:id :invent-independent-checker
    :caught? (some? (first (tree-values {:checker-id "other"} :checker-id)))}
   {:id :confuse-contract-and-catalogue-r12
    :caught? (and (str/includes? contract-text "hyperparameter inference")
                  (not (str/includes? catalogue-text
                                      "Dual-loop / hyperparameter inference")))}])

(def receipt
  {:schema :wm/v7-r12-node-sim-v1
   :node :R12
   :carriers {:used? false
              :reason :registry-declares-plumbing-with-no-equation}
   :registry {:equation-row-count 0 :plumbing? true
              :pointer "holes/labs/wm-contract/aif-equations.edn:1010"}
   :distinct-contracts
   {:catalogue "p4ng/sec-catalog.tex:297"
    :contract "docs/futon-aif-completeness.md:286-294"
    :concordance "p4ng/R-concordance.md:63-67"}
   :align-census
   {:row "holes/labs/wm-contract/ALIGN-rnode-process-census.md:86"
    :blocking-cells "holes/labs/wm-contract/ALIGN-rnode-process-census.md:178-180"
    :commissioned :absent :dispatched :absent :parked :absent
    :returned :absent :checked :absent :recorded :absent :surfaced :absent}
   :corpus {:files (count trace-files)
            :records (count trace-records)
            :r12-route-records (count r12-records)
            :r12-records-with-calibration-fields
            (count (filter #(some (fn [k] (seq (tree-values % k))) calibration-keys)
                           r12-records))
            :r12-records-with-process-identity
            (count (filter #(some (fn [k] (seq (tree-values % k))) process-keys)
                           r12-records))}
   :checks checks
   :plants plants
   :summary {:checks-pass (count (filter :pass? checks))
             :checks-total (count checks)
             :plants-caught (count (filter :caught? plants))
             :plants-total (count plants)}})

(when-not (and (every? :pass? checks) (every? :caught? plants))
  (binding [*out* *err*] (pp/pprint receipt))
  (System/exit 1))
(let [f (io/file lab "runs/V7-R12-node-sim/00-r12.edn")]
  (io/make-parents f)
  (with-open [w (io/writer f)] (binding [*out* w] (pp/pprint receipt))))
(pp/pprint (:summary receipt))
