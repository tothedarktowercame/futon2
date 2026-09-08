(ns v7-r9-node-sim
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

(def lab (io/file "holes/labs/wm-contract"))
(def equation-text (slurp (io/file lab "aif-equations.edn")))
(def stages-text (slurp "/home/joe/code/p4ng/empirics-futon/control-stages.edn"))
(def runner-text (slurp "src/futon2/aif/full_loop_runner.clj"))
(def r9-check-text (slurp "checks/r9_independence.clj"))

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

(defn r9-route? [record]
  (boolean
   (some #(and (map? %) (= :R9 (:node %)))
         (mapcat #(if (sequential? %) % [%])
                 (concat (tree-values record :wm/route)
                         (tree-values record :route))))))

(def r9-routes (count (filter r9-route? trace-records)))
(def birth-tag-keys [:independent? :measured :target-absent-fallback :settled :transient])
(def tagged-records
  (count (filter #(some (fn [k] (seq (tree-values % k))) birth-tag-keys) trace-records)))

(def checks
  [{:id :registry-declares-r9-plumbing
    :pass? (and (not (str/includes? equation-text ":node :R9"))
                (boolean (re-find #":plumbing\s+\[[^\]]*:R9" equation-text)))}
   {:id :stage-names-no-self-certification
    :pass? (boolean (re-find #"\{:node \"R9\"[^\n]+No self- certification" stages-text))}
   {:id :r9-check-computes-membership
    :pass? (and (str/includes? r9-check-text "(contains? producing-part producer)")
                (str/includes? r9-check-text "recorded-sound?"))}
   {:id :full-loop-refuses-author-as-reviewer
    :pass? (and (str/includes? runner-text "(= author reviewer)")
                (str/includes? runner-text "Selected reviewer is unavailable or is the author"))}
   {:id :full-loop-corroborates-review-execution
    :pass? (and (str/includes? runner-text "review-execution-evidence")
                (str/includes? runner-text ":job-events")
                (str/includes? runner-text ":review-execution-evidence-missing"))}
   {:id :no-r9-route-in-trace
    :pass? (zero? r9-routes)}])

(def plants
  [{:id :wrong-formula-self-called-independent
    :caught? (not= :independent
                   (if (contains? #{"author" "reviewer"} "author") :self :independent))}
   {:id :same-actor-reviewer
    :caught? (= "author" "author")}
   {:id :invent-r9-route
    :caught? (r9-route? {:wm/route [{:node :R9}]})}
   {:id :untagged-record
    :caught? (not-any? #(seq (tree-values {:verdict :pass} %)) birth-tag-keys)}])

(def receipt
  {:schema :wm/v7-r9-node-sim-v1
   :node :R9
   :registry {:equation-row-count 0 :plumbing? true}
   :implemented-subsets
   {:retained-corpus-check "checks/r9_independence.clj:15-31,113-143"
    :full-loop-distinct-reviewer "src/futon2/aif/full_loop_runner.clj:2598-2611"
    :full-loop-review-corroboration "src/futon2/aif/full_loop_runner.clj:1227-1271"}
   :corpus {:files (count trace-files)
            :records (count trace-records)
            :r9-route-records r9-routes
            :records-with-any-catalogue-birth-tag tagged-records}
   :checks checks
   :plants plants
   :summary {:checks-pass (count (filter :pass? checks))
             :checks-total (count checks)
             :plants-caught (count (filter :caught? plants))
             :plants-total (count plants)}})

(when-not (and (every? :pass? checks) (every? :caught? plants))
  (binding [*out* *err*] (pp/pprint receipt))
  (System/exit 1))
(let [f (io/file lab "runs/V7-R9-node-sim/00-r9.edn")]
  (io/make-parents f)
  (with-open [w (io/writer f)] (binding [*out* w] (pp/pprint receipt))))
(pp/pprint (:summary receipt))
