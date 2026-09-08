(ns v7-r10-node-sim
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

(def lab (io/file "holes/labs/wm-contract"))
(def equation-text (slurp (io/file lab "aif-equations.edn")))
(def scheduled-text (slurp "scripts/wm_scheduled_run.clj"))
(def cron-text (slurp "scripts/wm_full_loop_cron.sh"))
(def contract-text (slurp "docs/futon-aif-completeness.md"))

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
(def records-by-file
  (mapv (fn [f] [(.getName f) (read-stream f)]) trace-files))
(def trace-records (vec (mapcat second records-by-file)))

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

(def wallclock-by-file
  (into (sorted-map)
        (keep (fn [[name records]]
                (let [n (count (filter #(some #{:wallclock-cron}
                                               (tree-values % :trigger))
                                       records))]
                  (when (pos? n) [name n]))))
        records-by-file))
(def r10-records (vec (filter #(route? :R10 %) trace-records)))
(def identity-keys [:commission-id :commission/id :dispatch-id :dispatch/id])
(def state-change-keys [:state-change :state-changed? :grounded-change])

(def checks
  [{:id :registry-declares-r10-plumbing
    :pass? (and (not (str/includes? equation-text ":node :R10"))
                (boolean (re-find #":plumbing\s+\[[^\]]*:R10" equation-text)))}
   {:id :entrypoint-persists-one-trace
    :pass? (and (str/includes? scheduled-text "trace/write-trace! judgement")
                (str/includes? scheduled-text "System/exit 0"))}
   {:id :cron-wrapper-locks-and-dispatches
    :pass? (and (str/includes? cron-text "flock -n 9")
                (str/includes? cron-text "exec /usr/local/bin/clojure -M:wm-scheduled"))}
   {:id :contract-says-install-pending
    :pass? (str/includes? contract-text
                          "scheduled execution is *ready* but is not *currently running on this machine*")}
   {:id :trace-has-no-r10-attribution-or-process-identity
    :pass? (and (empty? r10-records)
                (every? #(not-any? (fn [k] (seq (tree-values % k))) identity-keys)
                        r10-records))}
   {:id :wallclock-era-ended
    :pass? (and (seq wallclock-by-file)
                (= "wm-trace-2026-07-14.edn" (last (keys wallclock-by-file))))}
   {:id :no-explicit-state-change-witness
    :pass? (zero? (count (filter #(some (fn [k] (seq (tree-values % k)))
                                      state-change-keys)
                                trace-records)))}])

(def plants
  [{:id :invent-r10-process-identity
    :caught? (some? (first (tree-values {:commission-id "c"} :commission-id)))}
   {:id :invent-r10-route
    :caught? (route? :R10 {:wm/route [{:node :R10}]})}
   {:id :wrong-trigger-called-wallclock
    :caught? (not (some #{:wallclock-cron} [:duree-click-regulated]))}
   {:id :scheduler-fire-called-state-change
    :caught? (not-any? #(seq (tree-values {:trigger :wallclock-cron} %))
                      state-change-keys)}])

(def receipt
  {:schema :wm/v7-r10-node-sim-v1
   :node :R10
   :registry {:equation-row-count 0 :plumbing? true}
   :apparatus {:entrypoint "scripts/wm_scheduled_run.clj:67-144"
               :cron-wrapper "scripts/wm_full_loop_cron.sh:1-17"
               :current-contract "docs/futon-aif-completeness.md:260-278"}
   :corpus {:files (count trace-files)
            :records (count trace-records)
            :r10-route-records (count r10-records)
            :r10-records-with-process-identity
            (count (filter #(some (fn [k] (seq (tree-values % k))) identity-keys)
                           r10-records))
            :wallclock-records-by-file wallclock-by-file
            :records-with-explicit-state-change-witness
            (count (filter #(some (fn [k] (seq (tree-values % k))) state-change-keys)
                           trace-records))}
   :checks checks
   :plants plants
   :summary {:checks-pass (count (filter :pass? checks))
             :checks-total (count checks)
             :plants-caught (count (filter :caught? plants))
             :plants-total (count plants)}})

(when-not (and (every? :pass? checks) (every? :caught? plants))
  (binding [*out* *err*] (pp/pprint receipt))
  (System/exit 1))
(let [f (io/file lab "runs/V7-R10-node-sim/00-r10.edn")]
  (io/make-parents f)
  (with-open [w (io/writer f)] (binding [*out* w] (pp/pprint receipt))))
(pp/pprint (:summary receipt))
