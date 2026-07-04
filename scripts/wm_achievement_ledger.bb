#!/usr/bin/env bb
;; wm_achievement_ledger.bb — M-aif-faithfulness B-0c (owner claude-12).
;;
;; Joe's visibility ask: a consolidated readout of what the WM has actually DONE
;; over 6+ weeks of ticks. Nothing renders this today; the raw trace has it all.
;;
;; SIM-ONLY, read-only: sweeps data/wm-trace/*.edn (one EDN form PER TICK) into a
;; per-day AND per-week achievement ledger — ticks · decisions/modes · act-gate
;; passes/fails (with missions) · enactments + realized-outcomes (expected vs
;; realized G) · gamma sample count + value trajectory · channel-gap trajectory.
;;
;; Placeholder guard (E6/census convention, M-evaluate-policies §15 /
;; wm_ihtb2_check.clj): cascade-lane ranked-actions are persisted with only
;; {:G-total 0.0 :rank :action} (no :G-risk); they never enter the selection pool
;; and are EXCLUDED from the scored-candidate counts.
;;
;; suspend-gaps are NOT failures (machine suspended, e.g. overnight) — see
;; futon2/holes/wm-baseline.md Clocks caveat; large inter-tick gaps are listed,
;; not counted against liveness.
;;
;; :wm-version segmentation is designed-for (B-0a — live in records from
;; 2026-07-04 08:00 onward); the pre-stamp corpus falls back to date + :mode.
;; :risk-mode IS persisted per ranked action since 2d6533e; the swept corpus
;; (≤ 2026-07-03) predates the cd0d25d production :kl flip, so it reads
;; uniformly :hinge — noted in the artifact.
;;
;; Deterministic: same trace corpus in ⇒ same artifact out.
;; Usage: bb scripts/wm_achievement_ledger.bb [trace-dir] [out-edn]
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def trace-dir (or (first *command-line-args*) "data/wm-trace"))
(def out-edn (or (second *command-line-args*)
                 "holes/labs/M-aif-faithfulness/wm-achievement-ledger.edn"))
(def out-html (str/replace out-edn #"\.edn$" ".html"))

(defn read-all-forms [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [acc []]
      (let [form (edn/read {:eof ::eof :default (fn [_tag v] v)} r)]
        (if (= ::eof form) acc (recur (conj acc form)))))))

(defn iso-week [day]                       ; "YYYY-MM-DD" -> "YYYY-Www"
  (let [d (java.time.LocalDate/parse day)
        wk (.get d java.time.temporal.IsoFields/WEEK_OF_WEEK_BASED_YEAR)
        yr (.get d java.time.temporal.IsoFields/WEEK_BASED_YEAR)]
    (format "%d-W%02d" yr wk)))

(defn tick->rec [t]
  (let [ts (str (:timestamp t))
        agv (:act-gate-verdicts t)
        ro (:realized-outcome t)
        pc (:free-energy t)
        chans (:per-channel pc)
        ra (:ranked-actions t)]
    {:ts ts
     :day (when (>= (count ts) 10) (subs ts 0 10))
     :instant (try (.toEpochMilli (java.time.Instant/parse ts)) (catch Throwable _ nil))
     :mode (:mode t)
     :decided? (some? (get-in t [:decision :action]))
     :gamma (get-in t [:decision :policy-precision])
     :gamma-samples (get-in t [:policy-precision :samples])
     :agv-pass (count (filter #(= :pass (:verdict %)) agv))
     :agv-fail (count (filter #(= :fail (:verdict %)) agv))
     :agv-abstain (count (filter #(not (#{:pass :fail} (:verdict %))) agv))
     :missions-passed (mapv :mission (filter #(= :pass (:verdict %)) agv))
     :enacted (when-let [e (:enactment t)] {:mission (:mission e) :expected-G (:gamma-expected-G e)
                                            :boxes (:boxes e) :policy-holes (:policy-holes e)})
     :realized (when (number? (:realized-G ro))
                 {:policy (:policy ro) :expected-G (:expected-G ro) :realized-G (:realized-G ro)})
     :oor-channels (count (filter (fn [[_ v]] (false? (:in-range? v))) chans))
     :n-scored (count (filter #(some? (:G-risk %)) ra))
     :n-placeholder (count (filter #(nil? (:G-risk %)) ra))}))

(defn- summarise [recs]
  (let [n (count recs)
        modes (frequencies (map :mode recs))
        enacted (keep :enacted recs)
        realized (keep :realized recs)
        gammas (keep :gamma recs)]
    {:ticks n
     :decisions (count (filter :decided? recs))
     :modes modes
     :act-gates {:evaluated (reduce + (map #(+ (:agv-pass %) (:agv-fail %) (:agv-abstain %)) recs))
                 :pass (reduce + (map :agv-pass recs))
                 :fail (reduce + (map :agv-fail recs))
                 :abstain (reduce + (map :agv-abstain recs))
                 :missions-passed (vec (distinct (mapcat :missions-passed recs)))}
     :enactments {:count (count enacted) :missions (frequencies (map :mission enacted))}
     :realized-outcomes {:count (count realized)
                         :mean-abs-error (when (seq realized)
                                           (/ (reduce + (map #(Math/abs (- (double (:realized-G %))
                                                                           (double (:expected-G %)))) realized))
                                              (count realized)))
                         :samples realized}
     :gamma {:first (first gammas) :last (last gammas)
             :min (when (seq gammas) (apply min gammas)) :max (when (seq gammas) (apply max gammas))
             :max-samples (apply max 0 (keep :gamma-samples recs))}
     :mean-oor-channels (when (pos? n) (/ (reduce + (map :oor-channels recs)) (double n)))
     :mean-scored-candidates (when (pos? n) (/ (reduce + (map :n-scored recs)) (double n)))}))

(defn- suspend-gaps [recs]                  ; inter-tick gaps > 2h = machine suspended, NOT failures
  (->> (map vector recs (rest recs))
       (keep (fn [[a b]]
               (when-let [ga (and (:instant a) (:instant b) (- (:instant b) (:instant a)))]
                 (when (> ga (* 2 60 60 1000))
                   {:from (:ts a) :to (:ts b) :hours (/ (Math/round (/ ga 360000.0)) 10.0)}))))
       vec))

;; --- sweep ------------------------------------------------------------------
(def files (->> (.listFiles (io/file trace-dir))
                (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
                (sort-by #(.getName %))))
(println (format "Reading %d trace files from %s …" (count files) trace-dir))

(def recs (vec (sort-by :instant (mapcat #(map tick->rec (read-all-forms %)) files))))
(def by-day (into (sorted-map) (update-vals (group-by :day recs) summarise)))
(def by-week (into (sorted-map) (update-vals (group-by #(when (:day %) (iso-week (:day %))) recs) summarise)))

(def ledger
  {:generated-by "scripts/wm_achievement_ledger.bb (M-aif-faithfulness B-0c)"
   :sim-only true :read-only true
   :corpus {:files (count files) :ticks (count recs)
            :span [(:day (first recs)) (:day (last recs))]}
   :notes ["risk-mode not persisted per tick (production :hinge; :kl lane dark/scheduled) — segmented by date+mode."
           ":wm-version segmentation designed-for (B-0a) but not yet persisted; date+mode is the current fallback."
           "suspend-gaps (>2h between ticks) are machine-suspended, NOT liveness failures (wm-baseline.md Clocks caveat)."
           "cascade-lane placeholder ranked-actions (no :G-risk) excluded from scored-candidate counts."]
   :summary (summarise recs)
   :suspend-gaps (suspend-gaps recs)
   :per-week by-week
   :per-day by-day})

(io/make-parents out-edn)
(spit out-edn (with-out-str (pp/pprint ledger)))

;; --- rendered HTML (the table Joe reads) ------------------------------------
(defn- h [x] (-> (str x) (str/replace "&" "&amp;") (str/replace "<" "&lt;") (str/replace ">" "&gt;")))
(defn- f2 [x] (if (number? x) (format "%.2f" (double x)) "—"))
(def s (:summary ledger))
(def html
  (str "<!doctype html><html><head><meta charset=utf-8><title>WM Achievement Ledger</title>"
       "<style>body{font:14px/1.5 -apple-system,system-ui,sans-serif;margin:24px 30px;color:#1a1a1a;max-width:1180px}"
       "h1{font-size:22px}h2{font-size:16px;border-bottom:2px solid #e3ddcf;padding-bottom:4px;margin-top:26px}"
       ".tiles{display:flex;gap:14px;flex-wrap:wrap;margin:14px 0}"
       ".tile{background:#faf8f3;border:1px solid #e3ddcf;border-radius:8px;padding:10px 16px;min-width:120px}"
       ".tile .v{font-size:24px;font-weight:700}.tile .k{font-size:12px;color:#6b6b6b}"
       "table{border-collapse:collapse;width:100%;font-size:13px;margin:8px 0}"
       "th,td{border:1px solid #e3ddcf;padding:4px 8px;text-align:right}th{background:#f3efe6}"
       "td:first-child,th:first-child{text-align:left}.muted{color:#6b6b6b;font-size:12px}</style></head><body>"
       "<h1>WM Achievement Ledger <span class=muted>— what the War Machine has actually done</span></h1>"
       "<p class=muted>" (h (get-in ledger [:corpus :files])) " trace files · "
       (h (get-in ledger [:corpus :ticks])) " ticks · "
       (h (first (get-in ledger [:corpus :span]))) " → " (h (last (get-in ledger [:corpus :span])))
       " · generated by <code>scripts/wm_achievement_ledger.bb</code> (deterministic, read-only).</p>"
       "<div class=tiles>"
       (apply str (for [[k v] [["ticks" (:ticks s)] ["decisions" (:decisions s)]
                               ["act-gates" (get-in s [:act-gates :evaluated])]
                               ["gate passes" (get-in s [:act-gates :pass])]
                               ["enactments" (get-in s [:enactments :count])]
                               ["realized γ-samples" (get-in s [:realized-outcomes :count])]
                               ["γ (latest)" (f2 (get-in s [:gamma :last]))]]]
                    (str "<div class=tile><div class=v>" (h v) "</div><div class=k>" (h k) "</div></div>")))
       "</div>"
       "<h2>Per week</h2><table><tr><th>week</th><th>ticks</th><th>decisions</th>"
       "<th>gate pass</th><th>gate fail</th><th>enacted</th><th>realized</th><th>γ last</th><th>mean OOR ch</th></tr>"
       (apply str (for [[wk w] (:per-week ledger)]
                    (str "<tr><td>" (h wk) "</td><td>" (h (:ticks w)) "</td><td>" (h (:decisions w))
                         "</td><td>" (h (get-in w [:act-gates :pass])) "</td><td>" (h (get-in w [:act-gates :fail]))
                         "</td><td>" (h (get-in w [:enactments :count])) "</td><td>" (h (get-in w [:realized-outcomes :count]))
                         "</td><td>" (f2 (get-in w [:gamma :last])) "</td><td>" (f2 (:mean-oor-channels w)) "</td></tr>")))
       "</table>"
       "<h2>Enactments &amp; realized outcomes (expected vs realized ΔG)</h2><table>"
       "<tr><th>mission (policy)</th><th>expected-G</th><th>realized-G</th><th>error</th></tr>"
       (apply str (for [r (get-in s [:realized-outcomes :samples])]
                    (str "<tr><td>" (h (:policy r)) "</td><td>" (f2 (:expected-G r)) "</td><td>" (f2 (:realized-G r))
                         "</td><td>" (f2 (- (double (:realized-G r)) (double (:expected-G r)))) "</td></tr>")))
       "</table><p class=muted>mean |expected − realized| = " (f2 (get-in s [:realized-outcomes :mean-abs-error]))
       " nats over " (h (get-in s [:realized-outcomes :count])) " realized samples; γ spanned "
       (f2 (get-in s [:gamma :min])) "–" (f2 (get-in s [:gamma :max])) " over " (h (get-in s [:gamma :max-samples])) " samples.</p>"
       "<h2>Mode distribution</h2><p>"
       (apply str (for [[m c] (sort-by (comp - val) (:modes s))] (str "<code>" (h m) "</code> " (h c) " &nbsp; ")))
       "</p><h2>Suspend gaps <span class=muted>(machine suspended — NOT failures)</span></h2><p class=muted>"
       (h (count (:suspend-gaps ledger))) " gaps &gt; 2h: "
       (apply str (interpose ", " (for [g (:suspend-gaps ledger)] (str (h (:hours g)) "h"))))
       "</p></body></html>"))
(spit out-html html)

(println (format "\n=== WM ACHIEVEMENT LEDGER ==="))
(println (format "corpus: %d files, %d ticks, %s → %s"
                 (count files) (count recs) (first (get-in ledger [:corpus :span])) (last (get-in ledger [:corpus :span]))))
(println (format "decisions: %d | act-gates: %d (pass %d / fail %d / abstain %d) | enactments: %d | realized: %d"
                 (:decisions s) (get-in s [:act-gates :evaluated]) (get-in s [:act-gates :pass])
                 (get-in s [:act-gates :fail]) (get-in s [:act-gates :abstain])
                 (get-in s [:enactments :count]) (get-in s [:realized-outcomes :count])))
(println (format "gamma: %.3f → %.3f over %d samples | mean realized error %.3f nats"
                 (double (or (get-in s [:gamma :first]) 0)) (double (or (get-in s [:gamma :last]) 0))
                 (get-in s [:gamma :max-samples]) (double (or (get-in s [:realized-outcomes :mean-abs-error]) 0))))
(println "suspend-gaps (>2h, not failures):" (count (:suspend-gaps ledger)))
(println "wrote" out-edn "and" out-html)
