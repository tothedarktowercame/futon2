(ns v7-r20-node-sim
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

(def lab (io/file "holes/labs/wm-contract"))
(def wm-file (io/file "scripts/futon2/report/war_machine.clj"))
(def equation-file (io/file lab "aif-equations.edn"))

(defn numbered-lines [f]
  (map-indexed (fn [i s] {:line (inc i) :text s})
               (str/split-lines (slurp f))))

(defn matching-lines [f re]
  (->> (numbered-lines f)
       (filter #(re-find re (:text %)))
       (mapv #(update % :text str/trim))))

(defn source-files [roots]
  (->> roots
       (mapcat #(file-seq (io/file %)))
       (filter #(.isFile %))
       (filter #(re-find #"\.(clj|cljc|bb)$" (.getName %)))
       (sort-by #(.getCanonicalPath %))))

(defn source-hits [files re]
  (vec (mapcat (fn [f]
                 (map #(assoc % :file (.getCanonicalPath f))
                      (matching-lines f re)))
               files)))

(defn trace-files []
  (->> (.listFiles (io/file "data/wm-trace"))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
       (sort-by #(.getName %))))

(defn read-records [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [out []]
      (let [v (try (edn/read {:eof ::eof :default (fn [_ value] value)} r)
                   (catch Exception _ ::bad))]
        (cond (= v ::eof) out
              (= v ::bad) (recur out)
              :else (recur (conj out v)))))))

(def records
  (vec (mapcat (fn [f]
                 (map-indexed (fn [i record]
                                {:file (.getName f) :index i :record record})
                              (read-records f)))
               (trace-files))))

(defn tree-values [x k]
  (cond (map? x) (concat (when (contains? x k) [(get x k)])
                         (mapcat #(tree-values % k) (vals x)))
        (sequential? x) (mapcat #(tree-values % k) x)
        :else []))

(defn r20-route? [x]
  (boolean
   (some (fn [route]
           (some #(and (map? %) (= :R20 (:node %)))
                 (if (sequential? route) route [route])))
         (concat (tree-values x :wm/route) (tree-values x :route)))))

(def attributable-keys
  [:metabolic-balance :metabolic-stale? :override-suppressed-reason])

(defn attributable? [x]
  (boolean (some #(seq (tree-values x %)) attributable-keys)))

(def surface-keys
  [:bulletin :bulletins :notify/discharged-at :park :parked
   :stop-line :stop-line-record :summon])

(defn surfaced? [x]
  (and (r20-route? x)
       (boolean (some #(seq (tree-values x %)) surface-keys))))

(def equation-r20-lines (matching-lines equation-file #":node\s+:R20(?:\s|[,}])"))
(def plumbing-lines (matching-lines equation-file #":plumbing\s+\[.*:R20"))
(def stale-citation-lines (vec (filter #(<= 472 (:line %) 473) (numbered-lines equation-file))))

(def route-lines (matching-lines wm-file #"route-tag[^\n]*:(?:R[0-9]+|TRACE)(?:\s|\))"))
(def r20-route-lines (vec (filter #(re-find #":R20(?:\s|\))" (:text %)) route-lines)))
(def route-nodes
  (->> route-lines
       (keep #(second (re-find #"route-tag[^\n]*:([A-Za-z0-9-]+)(?:\s|\))" (:text %))))
       (map keyword) set (sort-by name) vec))

(def corpus-route (filter #(r20-route? (:record %)) records))
(def corpus-attributable (filter #(attributable? (:record %)) records))
(def corpus-surfaced (filter #(surfaced? (:record %)) records))

(def node-link-roots
  ["/home/joe/code/futon3c/src/futon3c/agency"
   "/home/joe/code/futon3c/src/futon3c/social"
   "/home/joe/code/futon3c/src/futon3c/transport"
   "scripts/futon2/report/war_machine.clj"
   "src/futon2/aif"])
(def node-link-files (source-files node-link-roots))
(def node-link-re #"(?i)(?::(?:node|wm/node|route/node|control-node)\s+:R20|route-tag[^\n]*:R20)")
(def node-link-hits (source-hits node-link-files node-link-re))

(def interoceptive-re
  #"(?i)stop-the-line|metabolic-(?:balance|stale)|override-suppressed-reason")
(def interoceptive-hits (source-hits (source-files ["scripts/futon2/report/war_machine.clj" "src/futon2/aif"])
                                     interoceptive-re))
;; The refusal decision is FOUND, not pinned: war_machine.clj is under
;; continuous edit, so a literal line number here would rot into a false
;; :fail. The evidence window is derived from whatever line the search
;; returns.
(def refusal-decision-sites (source-hits [wm-file] #"mode\s+\(cond"))
(def refusal-decision-line (:line (first refusal-decision-sites)))
(def refusing-hits
  (vec (when refusal-decision-line
         (filter #(and (= (.getCanonicalPath wm-file) (:file %))
                       (<= refusal-decision-line (:line %) (+ refusal-decision-line 10)))
                 interoceptive-hits))))
(def r20-linked-refusing-hits
  (vec (filter #(re-find #":R20" (:text %)) refusing-hits)))

(def surface-re #"(?i)bulletin|notify/discharged-at|park!|:parked|stop-line|summon")
(def surface-hits (source-hits node-link-files surface-re))
(def r20-surface-hits (vec (filter #(re-find #":R20" (:text %)) surface-hits)))

(def synthetic-trace
  {:wm/route [{:node :R20 :via "synthetic/scan-metabolic-balance"}]
   :metabolic-balance {:max-tier :stop-the-line}})
(def synthetic-source
  [{:file "synthetic.clj" :line 1
    :text "(route-tag route :R20 \"synthetic/check\")"}])
(def synthetic-surface
  {:wm/route [{:node :R20 :via "synthetic/trip"}]
   :stop-line-record {:status :frozen} :summon {:reviewer :outer}})
(def plants
  [{:id :synthetic-r20-trace
    :route-count (count (filter r20-route? [synthetic-trace]))
    :attributable-count (count (filter attributable? [synthetic-trace]))}
   {:id :synthetic-r20-source
    :node-link-count (count (filter #(re-find node-link-re (:text %)) synthetic-source))}
   {:id :synthetic-r20-surface
    :surface-count (count (filter surfaced? [synthetic-surface]))}])
(def plant-results
  (mapv (fn [p]
          (assoc p :result
                 (if (every? pos? (for [[k v] p :when (str/ends-with? (name k) "count")] v))
                   :caught :escaped)))
        plants))

(def checks
  [{:id :registry-has-no-r20-equation
    :result (if (and (empty? equation-r20-lines) (= 1 (count plumbing-lines))) :pass :fail)
    :equation-row-count (count equation-r20-lines)
    :equation-search ":node :R20"
    :plumbing-hit-count (count plumbing-lines) :plumbing-hits plumbing-lines
    :cited-lines-472-473 stale-citation-lines
    :citation-lands-on-plumbing? (boolean (some #(re-find #":plumbing" (:text %)) stale-citation-lines))}
   {:id :route-tags-r20-to-metabolic-scan
    ;; The result is derived from what the file HAS (one R20 route site naming
    ;; the metabolic scan). Whether that site still sits on the line the ALIGN
    ;; census cites is reported separately, so a line shift reads as pointer
    ;; drift rather than as the node's route tag disappearing.
    :result (if (and (= 1 (count r20-route-lines))
                     (str/includes? (:text (first r20-route-lines)) "scan-metabolic-balance")) :pass :fail)
    :census-pointer "holes/labs/wm-contract/ALIGN-rnode-process-census.md:102 cites war_machine.clj:7055"
    :census-pointer-still-lands? (= 7055 (:line (first r20-route-lines)))
    :r20-site-count (count r20-route-lines) :r20-sites r20-route-lines
    :route-tag-site-count (count route-lines) :route-node-count (count route-nodes)
    :route-nodes route-nodes}
   {:id :corpus-has-no-r20-produced-record-field
    :result (if (empty? corpus-attributable) :pass :fail)
    :records-total (count records) :files-total (count (trace-files))
    :first-file (some-> records first :file) :last-file (some-> records last :file)
    :records-with-r20-route (count corpus-route)
    :records-with-attributable-field (count corpus-attributable)
    :attributable-keys-searched attributable-keys}
   {:id :checking-is-named-only
    :result (if (and (= 1 (count node-link-hits))
                     (seq refusing-hits)
                     (empty? r20-linked-refusing-hits)) :pass :fail)
    :node-link-search (str node-link-re) :node-link-hit-count (count node-link-hits)
    :node-link-hits node-link-hits
    :unlinked-interoceptive-hit-count (count interoceptive-hits)
    :unlinked-refusal-decision-search "mode \\(cond over war_machine.clj -- the line is found, not pinned"
    :unlinked-refusal-decision-count (count refusal-decision-sites)
    :unlinked-refusal-decision-sites refusal-decision-sites
    :unlinked-refusal-evidence-line-count (count refusing-hits)
    :unlinked-refusal-evidence-lines refusing-hits
    :r20-linked-refusal-site-count (count r20-linked-refusing-hits)}
   {:id :no-r20-attributed-surfacing
    :result (if (and (empty? corpus-surfaced) (empty? r20-surface-hits)) :pass :fail)
    :surface-search (str surface-re) :source-surface-hit-count (count surface-hits)
    :r20-attributed-source-surface-hit-count (count r20-surface-hits)
    :r20-attributed-corpus-surface-count (count corpus-surfaced)
    :surface-keys-searched surface-keys}
   {:id :negative-controls-detect-nonzero-cases
    :result (if (every? #(= :caught (:result %)) plant-results) :pass :fail)
    :plants plant-results}])

(def receipt
  {:harness :v7-r20-node-sim :row :V7 :slice 9 :node :R20 :stage "EVALUATE"
   :stage-at "/home/joe/code/p4ng/empirics-futon/control-stages.edn:41"
   :account-sites
   ["/home/joe/code/p4ng/sec-catalog.tex:369"
    "holes/labs/wm-contract/ALIGN-rnode-process-census.md:87"
    "holes/labs/wm-contract/ALIGN-rnode-process-census.md:98-120"
    "/home/joe/code/p4ng/R-concordance.md:41"]
   :checks checks
   :verdict (if (every? #(= :pass (:result %)) checks) :pass :fail)
   :negative-controls {:plants plant-results
                       :all-caught (every? #(= :caught (:result %)) plant-results)}
   :corpus {:root "data/wm-trace" :read-only true
            :file-count (count (trace-files)) :record-count (count records)}
   :not-done ["No equation reference exists for R20; this harness measures its process account."
              "No live tick or run lock; trace files were read only."
              "No production, registry, ledger, document, p4ng, or data file was modified."]})

(def out (io/file lab "runs/V7-R20-node-sim/00-r20.edn"))
(.mkdirs (.getParentFile out))
(with-open [w (io/writer out)] (binding [*out* w] (pp/pprint receipt)))
(doseq [check checks]
  (println (format "  %-52s %s" (name (:id check)) (name (:result check)))))
(println "  verdict" (:verdict receipt) "receipt" (str out))
(System/exit (if (and (= :pass (:verdict receipt))
                      (get-in receipt [:negative-controls :all-caught])) 0 1))
