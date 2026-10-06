(ns rank-with-readings
  (:require [clojure.edn :as edn]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.epistemic-value :as eig]
            [futon2.aif.meta-adapter-discovery :as discovery]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-outer-policy :as policy]
            [futon2.aif.meta-policy-constructor :as constructor]))

(def run-path "data/wm-runs/tick-run-record-2026-10-05-c9d25d6a-f2bb-42bf-a162-2c4a000e804f.edn")
(def contract-path "holes/labs/wm-contract/proposals/meta-outer-policy-cascade-measurement-v1.edn")
(def prior-path "holes/labs/wm-contract/proposals/meta-outer-provisional-prior-v2-proposal.edn")
(def sheet-path "holes/labs/wm-contract/REPORT-cheat-sheets-2026-10-05.md")
(def load-path "holes/labs/wm-contract/REPORT-operator-load-2026-10-05.md")
(def attention-path "holes/labs/wm-contract/REPORT-revealed-attention-2026-10-05.md")
(def coupling-path "holes/labs/wm-contract/REPORT-centrality-tokens-2026-10-05.md")
(def report-path "holes/labs/wm-contract/REPORT-rank-with-readings-2026-10-06.md")
(def script-path "scripts/rank_with_readings.clj")
(def resource-envelope {:time-budget-ms 5000 :token-budget 800
                        :author-seat "readings-author" :reviewer-seat "readings-reviewer"})
(def vocabulary [:closure :grounded-progress :downstream-unblocking
                 :abstention-or-failure :elapsed-budget-fraction
                 :token-budget-fraction :operator-demand])
(def prediction-rule
  {:status :default-setting-tune-later
   :authority {:actor "joe" :date "2026-10-06" :policy :defaults}
   :outcome-vocabulary vocabulary
   :means
   {:closure "0.1 + 0.4*register"
    :grounded-progress "clamp(0.2 + 0.5*register + 0.2*I(dispatches>0))"
    :downstream-unblocking "clamp(0.1 + 0.3*I(dispatches>0) + 0.3*I(operator-turns-14d>0) + 0.3*min(1,co-work-degree/5))"
    :abstention-or-failure "0.5 - 0.4*register"
    :elapsed-budget-fraction 0.5
    :token-budget-fraction 0.5
    :operator-demand "clamp(min(1,marker-lines/10) + 0.2*I(operator-turns-14d>0))"}
   :variances {:all 0.25 :claim :wide-no-calibration}
   :ablation {:no-sheet {:register :median-over-103 :marker-lines 0}
              :no-attention {:dispatches 0 :operator-turns-14d 0 :co-work-degree 0}}
   :coupling {:shared-missions :receipt-only :scored false}
   :uninformed-resources [:elapsed-budget-fraction :token-budget-fraction]})
(def zero-information-model
  {:prior {:only-state 1.0}
   :predicted-observations {:no-new-observation 1.0}
   :posteriors {:no-new-observation {:only-state 1.0}}})

(defn sha256 [s] (field/sha256 (.getBytes ^String s "UTF-8")))
(defn pin [path] (let [body (slurp path)] {:path path :sha256 (sha256 body)}))
(defn git-head [] (str/trim (:out (shell/sh "git" "rev-parse" "HEAD"))))
(defn cells [line] (mapv str/trim (rest (butlast (str/split line #"\|" -1)))))
(defn uncode [s] (str/replace s "`" ""))
(defn first-number [s]
  (some-> (re-find #"-?\d+(?:\.\d+)?" s) Double/parseDouble))
(defn table-lines [path]
  (filter #(and (str/starts-with? % "| `") (not (str/includes? % "| `:kind`")))
          (str/split-lines (slurp path))))
(defn parse-sheets []
  (into {} (keep (fn [line]
                   (let [c (cells line) id (uncode (get c 0 "")) reg-cell (get c 4 "")]
                     (when (and (re-matches #"[MET]-.*" id) (str/includes? reg-cell "register"))
                       [id {:register (first-number reg-cell)}])))
                 (table-lines sheet-path))))
(defn parse-load []
  (into {} (keep (fn [line]
                   (let [c (cells line) id (uncode (get c 0 ""))]
                     (when (and (re-matches #"[MET]-.*" id) (<= 11 (count c)))
                       [id {:markers (long (or (first-number (get c 9)) 0))
                            :coupling (long (or (first-number (get c 11)) 0))}])))
                 (table-lines load-path))))
(defn parse-attention []
  (into {} (keep (fn [line]
                   (let [c (cells line) id (uncode (get c 0 ""))]
                     (when (and (re-matches #"[MET]-.*" id) (= 9 (count c)))
                       [id {:dispatches (long (or (first-number (get c 2)) 0))
                            :operator-turns (long (or (first-number (get c 7)) 0))
                            :co-work (long (or (first-number (get c 8)) 0))
                            :last-dispatch (get c 6)}])))
                 (table-lines attention-path))))
(defn clamp [x] (max 0.0 (min 1.0 (double x))))
(defn median [xs]
  (let [v (vec (sort xs)) n (count v)]
    (if (odd? n) (v (quot n 2)) (/ (+ (v (dec (quot n 2))) (v (quot n 2))) 2.0))))
(defn readings [ids]
  (let [sheets (parse-sheets) loads (parse-load) attention (parse-attention)]
    (into {} (for [id ids]
               (let [r (merge (get sheets id) (get loads id) (get attention id))]
                 (when-not (every? #(number? (get r %)) [:register :markers :coupling :dispatches :operator-turns :co-work])
                   (throw (ex-info "reading missing" {:id id :reading r})))
                 [id r])))))
(defn effective-reading [r median-register {:keys [no-sheet no-attention]}]
  (cond-> r
    no-sheet (assoc :register median-register :markers 0)
    no-attention (assoc :dispatches 0 :operator-turns 0 :co-work 0)))
(defn prediction [{:keys [register markers dispatches operator-turns co-work]}]
  [(+ 0.1 (* 0.4 register))
   (clamp (+ 0.2 (* 0.5 register) (if (pos? dispatches) 0.2 0.0)))
   (clamp (+ 0.1 (if (pos? dispatches) 0.3 0.0)
             (if (pos? operator-turns) 0.3 0.0) (* 0.3 (min 1.0 (/ co-work 5.0)))))
   (- 0.5 (* 0.4 register)) 0.5 0.5
   (clamp (+ (min 1.0 (/ markers 10.0)) (if (pos? operator-turns) 0.2 0.0)))])
(defn preferences [prior]
  (let [by-outcome (into {} (map (juxt :outcome :preference)) (:channels prior))]
    {:means (mapv #(get-in by-outcome [% :mean]) vocabulary)
     :variances (mapv #(get-in by-outcome [% :variance]) vocabulary)
     :weights (mapv #(get-in by-outcome [% :weight]) vocabulary)}))
(defn add-g-input [candidate r pref g-pin]
  (assoc candidate :g-input
         {:means (prediction r) :variances (vec (repeat 7 0.25))
          :preference-means (:means pref) :preference-variances (:variances pref)
          :weights (:weights pref) :information-model zero-information-model
          :source-pin g-pin :outcome-vocabulary vocabulary}))

(defn evaluate! [label input]
  (let [receipt (policy/evaluate input)]
    (when (= :refused (:status receipt))
      (throw (ex-info "evaluate refused" {:scenario label :receipt receipt})))
    receipt))
(defn ranking [receipt]
  (let [targets (into {} (map (juxt :id #(get-in % [:slots :target])))
                      (:candidate-slot-fillings receipt))]
    (->> (:g receipt) (sort-by (juxt val (comp str key)))
         (map-indexed (fn [i [policy-id g]]
                        {:rank (inc i) :id (targets policy-id) :policy-id policy-id :g g})) vec)))
(defn kendall-tau-b [ids live-rank g-map]
  (let [pairs (for [i (range (count ids)) j (range (inc i) (count ids))]
                [(ids i) (ids j)])
        counts (frequencies
                (for [[a b] pairs
                      :let [x (compare (live-rank a) (live-rank b))
                            y (compare (g-map a) (g-map b))]]
                  (cond (zero? x) :tie-live (zero? y) :tie-measurement
                        (= (neg? x) (neg? y)) :concordant :else :discordant)))
        c (get counts :concordant 0) d (get counts :discordant 0)
        tx (get counts :tie-live 0) ty (get counts :tie-measurement 0)
        den (Math/sqrt (* (+ c d tx) (+ c d ty)))]
    {:n (count ids) :tau (if (zero? den) 0.0 (/ (- c d) den)) :counts counts}))
(defn fmt [x] (format "%.9f" (double x)))
(defn md-id [x] (str "`" x "`"))
(defn top3-terms [scenario]
  (str/join "\n" (for [{:keys [rank id policy-id g]} (take 3 (:ranking scenario))
                         :let [t (get-in scenario [:receipt :g-terms policy-id])]]
                     (str "| " (:label scenario) " | " rank " | " (md-id id) " | " (fmt (:risk t))
                          " | " (fmt (:ambiguity t)) " | " (fmt (:epistemic-value t)) " | " (fmt g) " |"))))
(defn render [{:keys [head pins median-register zero-information scenarios live-order readings]}]
  (let [by-label (into {} (map (juxt :key identity)) scenarios)
        ranks (into {} (for [[k s] by-label] [k (into {} (map (juxt :id :rank)) (:ranking s))]))
        target "M-interim-director-proxy-metric-inventory" target-reading (readings target)
        full-winner (:id (first (:ranking (by-label :full))))
        full-winner-reading (readings full-winner)]
    (str "# Ranking the 103 filled META candidates with recorded readings — 2026-10-06\n\n"
         "Reproduce in a fresh process from `/home/joe/code/futon2`:\n\n```sh\nclojure -M scripts/rank_with_readings.clj\n```\n\n"
         "futon2 HEAD before generation: `" head "`. The evaluator is `futon2.aif.meta-outer-policy/evaluate`; construction and evaluation both run in this fresh process.\n\n"
         "## Sources and pins\n\n| source | SHA-256 |\n|---|---|\n"
         (str/join "\n" (for [[k {:keys [path sha256]}] pins] (str "| `" k "`: `" path "` | `" sha256 "` |")))
         "\n\nThe measurement contract is passed unchanged and its pin is `:contract-source`. Coupling B1(b) travels in the readings map and report only; it is not included in any predicted mean or G input.\n\n"
         "The field observation remains whole. Its 381 rows outside the persisted fully-filled 103 are supplied to the evaluator as typed construction exclusions with reason `:outside-persisted-fully-filled-103`; this preserves exact field coverage while measuring only the requested candidates.\n\n"
         "## Prediction mapping\n\nThis is a **default setting, tune later**, not an empirical calibration:\n\n```clojure\n" (pr-str prediction-rule) "\n```\n\n"
         "The median register used by `--no-sheet` is " (fmt median-register) ". Both resource-fraction means are 0.5 in every scenario because the readings contain no candidate-conditioned elapsed-time or token-use prediction. Every predicted variance is 0.25.\n\n"
         "The zero-information model is:\n\n```clojure\n" (pr-str zero-information-model) "\n```\n\n"
         "The real EIG kernel returns **" (fmt zero-information) "**: its one predicted observation leaves the singleton prior unchanged, so this is an honest zero rather than an omitted term.\n\n"
         "## Evaluator receipts and live comparison\n\nKendall tau-b is computed over the 103 common items. The live ranking has no ties; measurement-score ties are retained and enter tau-b's tie denominator. Canonical ID breaks score ties only for displaying a total order and selecting a policy.\n\n"
         "| scenario | flags | selected policy | selected target | nearest alternative | tau-b vs live | n | measurement ties |\n|---|---|---|---|---|---:|---:|---:|\n"
         (str/join "\n" (for [{:keys [label flags receipt kendall ranking]} scenarios]
                              (str "| " label " | `" flags "` | " (md-id (:selected-policy receipt)) " | "
                                   (md-id (:id (first ranking))) " | " (md-id (:nearest-alternative receipt)) " | "
                                   (fmt (:tau kendall)) " | " (:n kendall)
                                   " | " (get-in kendall [:counts :tie-measurement] 0) " |")))
         "\n\n## Top ten side by side\n\n| rank | live selector | full | no sheet | no attention | neither |\n|---:|---|---|---|---|---|\n"
         (str/join "\n" (for [i (range 10)]
                              (str "| " (inc i) " | " (md-id (nth live-order i)) " | "
                                   (md-id (:id (nth (:ranking (by-label :full)) i))) " | "
                                   (md-id (:id (nth (:ranking (by-label :no-sheet)) i))) " | "
                                   (md-id (:id (nth (:ranking (by-label :no-attention)) i))) " | "
                                   (md-id (:id (nth (:ranking (by-label :neither)) i))) " |")))
         "\n\n## Proxy-metric-inventory position and readings\n\nIts recorded readings are `" (pr-str target-reading) "`.\n\n| live | full | no sheet | no attention | neither |\n|---:|---:|---:|---:|---:|\n| " (inc (.indexOf ^java.util.List live-order target)) " | "
         (get-in ranks [:full target]) " | " (get-in ranks [:no-sheet target]) " | "
         (get-in ranks [:no-attention target]) " | " (get-in ranks [:neither target]) " |\n\n"
         "## G terms for each scenario's top three\n\n| scenario | rank | id | risk | ambiguity | epistemic value | G |\n|---|---:|---|---:|---:|---:|---:|\n"
         (str/join "\n" (map top3-terms scenarios))
         "\n\n## Full rankings\n\n| scenario | rank | id | G |\n|---|---:|---|---:|\n"
         (str/join "\n" (for [s scenarios r (:ranking s)]
                              (str "| " (:label s) " | " (:rank r) " | " (md-id (:id r)) " | " (fmt (:g r)) " |")))
         "\n\n## What changed\n\n"
         "The full winner, `" full-winner "`, has readings `" (pr-str full-winner-reading) "`; the live winner has `" (pr-str target-reading) "` and lands 17th in the full measurement. Removing the sheet replaces register with the cohort median and clears marker lines, so closure, progress, failure, and operator-demand lose item-specific variation. Removing attention clears dispatch, operator-turn, and co-work inputs, so progress and downstream-unblocking lose their attention increments and operator-demand loses its turn increment. The tables show the resulting movements; coupling remains visible but cannot cause any movement because R6 keeps it outside G.\n")))

(defn -main [& flags]
  (let [flag-set (set flags)
        _ (when-not (every? #{"--no-sheet" "--no-attention"} flag-set)
            (throw (ex-info "unknown ablation flag" {:flags flags})))
        focus (cond (= flag-set #{"--no-sheet" "--no-attention"}) :neither
                    (= flag-set #{"--no-sheet"}) :no-sheet
                    (= flag-set #{"--no-attention"}) :no-attention
                    :else :full)
        run (edn/read-string (slurp run-path)) ranked (get-in run [:outer-task-selection :policy :meta-selection :ranking])
        ranked-ids (set (map :id ranked)) contract (edn/read-string (slurp contract-path))
        prior (edn/read-string (slurp prior-path))
        _ (when-let [errors (seq (#'policy/contract-errors contract))]
            (throw (ex-info "measurement contract refused" {:errors errors})))
        _ (when-not (= vocabulary (:outcome-vocabulary prior))
            (throw (ex-info "prior vocabulary mismatch" {:prior (:outcome-vocabulary prior)})))
        zero (eig/expected-information-gain zero-information-model)
        _ (when-not (zero? zero) (throw (ex-info "zero information model was nonzero" {:value zero})))
        fo (field/observe {}) _ (when (= :refused (:status fo)) (throw (ex-info "observe refused" fo)))
        found (discovery/discover {:field-observation fo :expected-field-pin (:source-pin fo)})
        _ (when (= :refused (:status found)) (throw (ex-info "discover refused" found)))
        made (constructor/construct {:field-observation fo :expected-field-pin (:source-pin fo)
                                    :adapters (:adapters found) :resource-envelope resource-envelope})
        _ (when (= :refused (:status made)) (throw (ex-info "construct refused" made)))
        templates (filterv #(ranked-ids (get-in % [:slots :target])) (:templates made))
        _ (when-not (= 103 (count templates)) (throw (ex-info "expected 103 templates" {:count (count templates)})))
        ids (mapv #(get-in % [:slots :target]) templates) readings (readings ids)
        med (median (map #(get-in readings [% :register]) ids)) pref (preferences prior)
        candidate-id-set (set ids)
        measurement-exclusions (mapv (fn [row]
                                       {:id (:id row) :kind (:kind row) :source (:source row)
                                        :reason :outside-persisted-fully-filled-103})
                                     (remove #(candidate-id-set (:id %)) (:rows fo)))
        g-pin (pin script-path) contract-pin (pin contract-path)
        base {:contract contract :contract-source contract-pin :field-observation fo
              :observation {:injury-observation :absent}
              :construction-exclusions measurement-exclusions}
        specs [{:key :full :label "full" :flags "none" :options {}}
               {:key :no-sheet :label "no sheet" :flags "--no-sheet" :options {:no-sheet true}}
               {:key :no-attention :label "no attention" :flags "--no-attention" :options {:no-attention true}}
               {:key :neither :label "neither" :flags "--no-sheet --no-attention"
                :options {:no-sheet true :no-attention true}}]
        live-rank (into {} (map (juxt :id :rank)) ranked)
        scenarios (mapv (fn [{:keys [key label flags options]}]
                          (let [candidates (mapv (fn [c] (let [id (get-in c [:slots :target])]
                                                          (add-g-input c (effective-reading (readings id) med options) pref g-pin))) templates)
                                receipt (evaluate! key (assoc base :candidates candidates)) rank (ranking receipt)
                                common (mapv :id rank) target-g (into {} (map (juxt :id :g)) rank)]
                            {:key key :label label :flags flags :receipt receipt :ranking rank
                             :kendall (kendall-tau-b common live-rank target-g)})) specs)
        live-order (->> ranked (filter #(ranked-ids (:id %))) (filter #(some #{(:id %)} ids)) (sort-by :rank) (mapv :id))
        pins {:run-record (pin run-path) :measurement-contract contract-pin :prior-v2 (pin prior-path)
              :sheet-report (pin sheet-path) :operator-load-report (pin load-path)
              :attention-report (pin attention-path) :coupling-report (pin coupling-path) :mapping-script g-pin}
        result {:head (git-head) :pins pins :median-register med :zero-information zero
                :scenarios scenarios :live-order live-order :readings readings}]
    (spit report-path (render result))
    (println report-path)
    (println "focused-scenario" focus
             "selected-policy" (:selected-policy (:receipt (first (filter #(= focus (:key %)) scenarios)))))))

(apply -main *command-line-args*)
