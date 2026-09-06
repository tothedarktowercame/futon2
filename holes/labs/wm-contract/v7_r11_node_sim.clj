(ns v7-r11-node-sim
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

(def lab (io/file "holes/labs/wm-contract"))
(def equation-file (io/file lab "aif-equations.edn"))
(def budget-file (io/file "src/futon2/aif/hierarchical_budget.clj"))
(def adapter-file (io/file "src/futon2/aif/hierarchical_budget_adapter.clj"))
(def policy-file (io/file "src/futon2/aif/policy.clj"))
(def close-file (io/file "src/futon2/aif/close_loop.clj"))
(def dossier-file (io/file lab "PROBLEMS-r15-r11-r17-batch5.md"))

(defn numbered-lines [f]
  (map-indexed (fn [i text] {:line (inc i) :text text})
               (str/split-lines (slurp f))))
(defn matching-lines [f re]
  (->> (numbered-lines f) (filter #(re-find re (:text %)))
       (mapv #(update % :text str/trim))))
(defn lines-in [f lo hi]
  (->> (numbered-lines f) (filter #(<= lo (:line %) hi))
       (mapv #(update % :text str/trim))))
(defn relative-to [root f]
  (str (.relativize (.toPath (.getCanonicalFile (io/file root)))
                    (.toPath (.getCanonicalFile f)))))
(defn code-files [root]
  (->> (file-seq (io/file root))
       (filter #(.isFile %))
       (filter #(re-find #"\.(clj|cljc|bb)$" (.getName %)))
       (remove #(re-find #"/(?:\.git|target)/" (.getCanonicalPath %)))
       (sort-by #(.getCanonicalPath %))))
(defn source-hits [root files re]
  (vec (mapcat (fn [f]
                 (map #(assoc % :file (relative-to root f))
                      (matching-lines f re))) files)))

(def equation-r11 (matching-lines equation-file #":node\s+:R11(?:\s|[,}])"))
(def all-r11 (matching-lines equation-file #"R11"))
(def plumbing-r11 (matching-lines equation-file #":plumbing\s+\[.*:R11"))

(def call-re
  #"futon2\.aif\.hierarchical-budget(?:-adapter)?|hierarchical-budget/(?:arbitrate)|(?:policy/)?select-budgeted-actions|(?:adapter/)?select-ranked-proposal-fields")
(def all-code-files (code-files "."))
(def call-hits (source-hits "." all-code-files call-re))
(defn hit-class [{:keys [file text]}]
  (cond (= file "holes/labs/wm-contract/v7_r11_node_sim.clj") :this-harness
        (str/starts-with? file "test/") :test
        (#{"src/futon2/aif/hierarchical_budget.clj"
           "src/futon2/aif/hierarchical_budget_adapter.clj"} file) :own-namespace
        (and (= file "src/futon2/aif/policy.clj")
             (or (str/includes? text "hierarchical-budget")
                 (str/includes? text "select-budgeted-actions"))) :policy-boundary
        :else :production-caller))
(def classified-hits (mapv #(assoc % :class (hit-class %)) call-hits))
(def production-calls (vec (filter #(= :production-caller (:class %)) classified-hits)))

(def boundary-def (matching-lines policy-file #"defn select-budgeted-actions"))
(def boundary-call (matching-lines policy-file #"hierarchical-budget/arbitrate"))
(def adapter-def (matching-lines adapter-file #"defn select-ranked-proposal-fields"))
(def test-opt-ins
  (vec (filter #(and (= :test (:class %))
                     (re-find #"select-budgeted-actions|select-ranked-proposal-fields" (:text %)))
               classified-hits)))
(def semilattice-flag (matching-lines close-file #"def \^:dynamic \*semilattice-fold\?\*"))
(def semilattice-call (matching-lines close-file #"fs/semilattice-fold"))

(defn declaration-lines [f declaration]
  (let [lines (vec (numbered-lines f))
        start (first (keep-indexed
                      (fn [i {:keys [text]}]
                        (when (re-find (re-pattern (str "^\\(defn\\s+" declaration "(?:\\s|$)")) text) i))
                      lines))
        end (when start
              (or (first (keep-indexed
                          (fn [i {:keys [text]}]
                            (when (and (> i start) (re-find #"^\(def(?:n|macro|multi|method)?\s" text)) i))
                          lines))
                  (count lines)))]
    (if start (subvec lines start end) [])))
(def output-declaration (declaration-lines budget-file "arbitrate"))
(def adapter-declaration (declaration-lines adapter-file "select-ranked-proposal-fields"))
(def output-keys
  (->> output-declaration
       (mapcat #(map (comp keyword (fn [s] (subs s 1)))
                     (re-seq #":[A-Za-z][A-Za-z0-9?*/._-]*" (:text %))))
       set (sort-by name) vec))
(def adapter-keys
  (->> adapter-declaration
       (mapcat #(map (comp keyword (fn [s] (subs s 1)))
                     (re-seq #":[A-Za-z][A-Za-z0-9?*/._-]*" (:text %))))
       set (sort-by name) vec))
(def r11-evidence-keys
  [:adapter/schema-version :replay/receipt :selection-boundary
   :within-all-budgets? :node-usage :node-budgets :oversubscribed-nodes])

(defn trace-files []
  (->> (.listFiles (io/file "data/wm-trace"))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
       (sort-by #(.getName %))))
(defn read-one [r]
  (try
    {:value (edn/read {:eof ::eof :default (fn [tag value] {:tag tag :value value})} r)}
    (catch Exception e
      {:error {:class (.getName (class e)) :message (.getMessage e)}})))
(defn read-edn-stream [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [records []]
      (let [{:keys [value error]} (read-one r)]
        (cond error {:records records :error error}
              (= ::eof value) {:records records :error nil}
              :else (recur (conj records value)))))))
(def trace-reads (mapv (fn [f] (assoc (read-edn-stream f) :file (.getName f))) (trace-files)))
(def trace-errors (vec (keep #(when (:error %) (select-keys % [:file :error])) trace-reads)))
(def trace-records
  (vec (mapcat (fn [{:keys [file records]}]
                 (map-indexed (fn [i record] {:file file :index i :record record}) records))
               trace-reads)))
(defn tree-values [x k]
  (cond (map? x) (concat (when (contains? x k) [(get x k)])
                         (mapcat #(tree-values % k) (vals x)))
        (sequential? x) (mapcat #(tree-values % k) x)
        :else []))
(defn r11-route? [record]
  (boolean (some #(and (map? %) (= :R11 (:node %)))
                 (mapcat #(if (sequential? %) % [%])
                         (concat (tree-values record :wm/route)
                                 (tree-values record :route))))))
(defn r11-witness? [record]
  (or (some #(seq (tree-values record %))
            (remove #{:selection-boundary} r11-evidence-keys))
      (some #{:hierarchical-shared-budget} (tree-values record :selection-boundary))))
(def r11-traces (vec (filter #(r11-witness? (:record %)) trace-records)))
(def r11-routes (vec (filter #(r11-route? (:record %)) trace-records)))
(def budget-values (vec (mapcat #(tree-values (:record %) :budget) trace-records)))
(def budget-contexts
  (->> trace-records
       (filter #(seq (tree-values (:record %) :budget)))
       (mapv #(select-keys % [:file :index]))))

(def campaign-ids
  ["e-8ae4e209-f13b-4722-80de-a7fc7d5d68fe"
   "canary-0a33ac68-aa60-4018-a896-5643f259a2d4"
   "wm-instrumented-S-20260821/r17-intertick"
   "97266161492420cddfdffa394e49dbc540bbcf1c2e80616483b2732754151990"])
(def search-roots
  ["/home/joe/code/futon2/holes" "/home/joe/code/futon2/docs"
   "/home/joe/code/p4ng" "/home/joe/code/futon3c/holes" "/home/joe/code/futon3c/docs"])
(defn text-files [roots]
  (->> roots (map io/file) (filter #(.exists %)) (mapcat file-seq)
       (filter #(.isFile %))
       (filter #(re-find #"\.(?:edn|json|txt|md|tex|clj|cljc|bb)$" (.getName %)))
       (remove #(re-find #"/(?:\.git|target)/" (.getCanonicalPath %)))
       (remove #(= (.getCanonicalPath (io/file lab "runs/V7-R11-node-sim/00-r11.edn"))
                   (.getCanonicalPath %)))
       (remove #(= (.getCanonicalPath (io/file lab "v7_r11_node_sim.clj"))
                   (.getCanonicalPath %)))
       (sort-by #(.getCanonicalPath %))))
(def campaign-files (vec (text-files search-roots)))
(defn id-hits [f]
  (let [text (slurp f)]
    (vec (for [id campaign-ids :when (str/includes? text id)] id))))
(def campaign-hits
  (vec (keep (fn [f] (when-let [ids (seq (id-hits f))]
                       {:file (.getCanonicalPath f) :ids (vec ids)})) campaign-files)))
(def record-identity-keys
  #{:run-id :run/id :receipt-id :receipt/id :attempt-id :attempt/id
    :canary-id :campaign-id :corpus-sha256 :corpus/sha256})
(defn record-identity-hits [x]
  (cond (map? x) (concat
                  (for [[k v] x :when (and (record-identity-keys k)
                                            (some #{v} campaign-ids))]
                    {:key k :id v})
                  (mapcat record-identity-hits (vals x)))
        (sequential? x) (mapcat record-identity-hits x)
        :else []))
(def structured-campaign-values
  (vec (for [f campaign-files
             :when (str/ends-with? (.getName f) ".edn")
             :let [x (try (edn/read-string {:default (fn [tag value] {:tag tag :value value})} (slurp f))
                          (catch Exception _ nil))]
             hit (record-identity-hits x)]
         (assoc hit :file (.getCanonicalPath f)))))

(def stale-equation-lines (lines-in equation-file 472 473))
(def dossier-r11 (lines-in dossier-file 62 110))
(def docs-r11 (lines-in (io/file "docs/futon-aif-completeness.md") 280 284))
(def catalog-r11 (lines-in (io/file "/home/joe/code/p4ng/sec-catalog.tex") 243 243))
(def factoring-r11 (lines-in (io/file "/home/joe/code/p4ng/empirics-futon/factoring-table.tex") 32 36))

(def lean-files
  (->> (file-seq (io/file "/home/joe/code/mathlib4/DarkTower/WarMachine"))
       (filter #(.isFile %)) (filter #(str/ends-with? (.getName %) ".lean"))))
(def lean-hits (source-hits "/home/joe/code/mathlib4" lean-files #"(?i)R11|hierarchical.?budget|budget.?arbiter"))
(def concordance-hits
  (matching-lines (io/file lab "symbol-concordance.edn") #"(?i)R11|hierarchical.?budget|budget.?arbiter"))
(def generated-hits
  (matching-lines (io/file "/home/joe/code/p4ng/sec-lean-state-generated.tex") #"(?i)R11|hierarchical.?budget|budget.?arbiter"))
(def carrier-like-lean-hits
  (vec (filter #(re-find #"(?i)(def|structure|inductive|abbrev).*budget" (:text %)) lean-hits)))

(def align-file (io/file lab "ALIGN-rnode-process-census.md"))
(def align-r11 (matching-lines align-file #"R11"))
(def stage-r11
  (matching-lines (io/file "/home/joe/code/p4ng/empirics-futon/control-stages.edn")
                  #"\{:node \"R11\""))

(def synthetic-source [{:file "synthetic.clj" :line 1
                        :text "(policy/select-budgeted-actions hierarchy)"}])
(def synthetic-trace {:selection-boundary :hierarchical-shared-budget
                      :within-all-budgets? true})
(def synthetic-equation [{:text "{:id :bad :node :R11}"}])
(def synthetic-artifact {:run-id (first campaign-ids)})
(def synthetic-citation {:claimed "aif-equations.edn:472-473"
                         :actual stale-equation-lines})
(def plant-results
  [{:id :production-requirer
    :detected (count (filter #(re-find call-re (:text %)) synthetic-source))
    :result (if (= 1 (count (filter #(re-find call-re (:text %)) synthetic-source))) :caught :escaped)}
   {:id :trace-budget-witness :detected (count (filter r11-witness? [synthetic-trace]))
    :result (if (= 1 (count (filter r11-witness? [synthetic-trace]))) :caught :escaped)}
   {:id :r11-equation :detected (count (filter #(re-find #":node\s+:R11" (:text %)) synthetic-equation))
    :result (if (= 1 (count (filter #(re-find #":node\s+:R11" (:text %)) synthetic-equation))) :caught :escaped)}
   {:id :campaign-s-artifact
    :detected (count (record-identity-hits synthetic-artifact))
    :result (if (= 1 (count (record-identity-hits synthetic-artifact))) :caught :escaped)}
   {:id :stale-citation
    :detected (if (not-any? #(str/includes? (:text %) ":plumbing") (:actual synthetic-citation)) 1 0)
    :result (if (not-any? #(str/includes? (:text %) ":plumbing") (:actual synthetic-citation)) :caught :escaped)}])

(def checks
  [{:id :registry-carries-no-r11-equation
    :result (if (and (empty? equation-r11) (= 1 (count plumbing-r11)) (= 1 (count all-r11))) :pass :fail)
    :equation-row-count (count equation-r11) :all-r11-occurrences all-r11
    :plumbing-hits plumbing-r11 :basis "holes/labs/wm-contract/aif-equations.edn:520-521"}
   {:id :requirer-census
    :result (if (and (seq classified-hits) (empty? production-calls)) :pass :fail)
    :roots ["src" "scripts" "test" "checks" "holes"] :files-searched (count all-code-files)
    :hit-count (count classified-hits) :hits classified-hits
    :production-caller-count (count production-calls) :production-callers production-calls
    :finding :arbiter-not-reached-by-production-tick}
   {:id :live-selection-boundary
    :result (if (and (= 1 (count boundary-def)) (= 1 (count boundary-call))
                     (= 1 (count adapter-def)) (seq test-opt-ins)
                     (empty? production-calls) (seq semilattice-call)) :pass :fail)
    :boundary-definition boundary-def :boundary-call boundary-call :adapter-boundary adapter-def
    :opt-in-kind :explicit-function-call :test-opt-ins test-opt-ins
    :outside-test-opt-ins production-calls
    :close-loop-r11-label semilattice-flag :close-loop-call semilattice-call
    :same-mechanism? false
    :reason "The policy boundary calls hierarchical-budget/arbitrate; close_loop calls fold-semilattice/semilattice-fold and never the arbiter."
    :basis "src/futon2/aif/policy.clj:20-31; src/futon2/aif/close_loop.clj:54-83"}
   {:id :corpus-census
    :result (if (and (empty? trace-errors) (empty? r11-traces) (empty? r11-routes)) :pass :fail)
    :files-total (count trace-reads) :records-total (count trace-records)
    :first-file (some-> trace-records first :file) :last-file (some-> trace-records last :file)
    :parse-errors trace-errors :arbiter-output-keys-derived output-keys
    :adapter-output-keys-derived adapter-keys :evidence-keys-searched r11-evidence-keys
    :records-with-r11-evidence (count r11-traces) :records-with-r11-route (count r11-routes)
    :generic-budget-value-count (count budget-values) :generic-budget-record-contexts budget-contexts
    :generic-budget-is-not-counted-as-r11 true
    :basis "src/futon2/aif/hierarchical_budget.clj:157-213; src/futon2/aif/hierarchical_budget_adapter.clj:60-100"}
   {:id :campaign-s-artifact
    :result (if (and (seq campaign-files) (empty? structured-campaign-values)) :pass :fail)
    :roots search-roots :files-searched (count campaign-files) :identifiers campaign-ids
    :narrative-hit-count (count campaign-hits) :narrative-hits campaign-hits
    :structured-record-hit-count (count structured-campaign-values)
    :structured-record-hits structured-campaign-values
    :finding :identifiers-found-only-inside-narrative-not-as-record-values
    :basis "/home/joe/code/p4ng/sec-catalog.tex:243; /home/joe/code/p4ng/empirics-futon/empirics.tex:50"}
   {:id :dossier-citation-freshness
    :result (if (and (not-any? #(str/includes? (:text %) ":plumbing") stale-equation-lines)
                     (some #(str/includes? (:text %) "N/A at this scope") docs-r11)
                     (some #(str/includes? (:text %) "Campaign S") catalog-r11)
                     (some #(str/includes? (:text %) "R11") factoring-r11)) :pass :fail)
    :dossier-lines dossier-r11
    :stale-equation-citation {:claimed "aif-equations.edn:472-473" :actual stale-equation-lines
                              :still-lands? false :current-plumbing plumbing-r11}
    :fresh-pointers {:contract docs-r11 :catalogue catalog-r11 :factoring factoring-r11}}
   {:id :dossier-residuals
    :result (if (and (empty? production-calls) (empty? structured-campaign-values)
                     (some #(str/includes? (:text %) "R11") factoring-r11)
                     (some #(str/includes? (:text %) "N/A at this scope") docs-r11)) :pass :fail)
    :scope-status-disagreement {:contract :n/a-at-this-scope
                                :catalogue-claim :campaign-s-exact-replay
                                :campaign-record :not-found}
    :factoring-residual {:status :confirmed-unmapped :evidence factoring-r11}
    :general-multi-agent-composition {:status :not-reached
                                      :production-arbiter-callers (count production-calls)
                                      :contract-evidence docs-r11}
    :basis "holes/labs/wm-contract/PROBLEMS-r15-r11-r17-batch5.md:83-99"}
   {:id :lean-carrier-search
    :result (if (and (empty? carrier-like-lean-hits) (empty? concordance-hits) (empty? generated-hits)) :pass :fail)
    :mathlib-files-searched (count lean-files) :all-lean-text-hits lean-hits
    :carrier-like-definition-hits carrier-like-lean-hits
    :symbol-concordance-hits concordance-hits :generated-state-hits generated-hits
    :finding :not-applicable-no-r11-budget-carrier-found
    :basis "/home/joe/code/mathlib4/DarkTower/WarMachine; holes/labs/wm-contract/symbol-concordance.edn; /home/joe/code/p4ng/sec-lean-state-generated.tex"}
   {:id :band-assurance-census
    :result (if (and (= 1 (count stage-r11))
                     (str/includes? (:text (first stage-r11)) ":band :loop")
                     (empty? align-r11)) :pass :fail)
    :stage-hits stage-r11 :align-search "R11" :align-hit-count (count align-r11)
    :align-hits align-r11 :finding :not-applicable-loop-band
    :basis "/home/joe/code/p4ng/empirics-futon/control-stages.edn; holes/labs/wm-contract/ALIGN-rnode-process-census.md"}
   {:id :negative-controls-discriminate
    :result (if (every? #(= :caught (:result %)) plant-results) :pass :fail)
    :plants plant-results}])

(def receipt
  {:harness :v7-r11-node-sim :row :V7 :slice 14 :node :R11 :stage "SELECT"
   :stage-at "/home/joe/code/p4ng/empirics-futon/control-stages.edn:27"
   :account-sites ["holes/labs/wm-contract/aif-equations.edn:520-521"
                   "src/futon2/aif/hierarchical_budget.clj:1-213"
                   "src/futon2/aif/hierarchical_budget_adapter.clj:1-109"
                   "holes/labs/wm-contract/PROBLEMS-r15-r11-r17-batch5.md:62-110"]
   :checks checks
   :verdict (if (and (every? #(= :pass (:result %)) checks)
                     (every? #(= :caught (:result %)) plant-results)) :pass :fail)
   :negative-controls {:plants plant-results
                       :all-caught (every? #(= :caught (:result %)) plant-results)}
   :corpus {:root "data/wm-trace" :read-only true :file-count (count trace-reads)
            :record-count (count trace-records) :parse-errors trace-errors}
   :not-done ["No equation reference exists for R11; this harness measures its process account and call graph."
              "No live tick or run lock; trace files were read only."
              "No production, registry, ledger, document, p4ng, Lean, or data file was modified."]})

(def out (io/file lab "runs/V7-R11-node-sim/00-r11.edn"))
(.mkdirs (.getParentFile out))
(with-open [w (io/writer out)] (binding [*out* w] (pp/pprint receipt)))
(doseq [check checks]
  (println (format "  %-46s %s" (name (:id check)) (name (:result check)))))
(println "  verdict" (:verdict receipt) "receipt" (str out))
(System/exit (if (= :pass (:verdict receipt)) 0 1))
