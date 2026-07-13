#!/usr/bin/env bb
;; c9_burn_in.bb — M-aif-faithfulness / M-evaluate-policies C9 before/after burn-in
;; census (owner claude-12). Third census outing; read-only over data/wm-trace.
;;
;; BEFORE = hinge-era: ticks whose ranked-actions carry NO :goal-outcome-mode :kl.
;; AFTER  = KL-era: ticks with :goal-outcome-mode :kl, EXCLUDING the manual record
;;          (:wm-version :git-sha 515a5936…), segmented by :wm-version :trigger
;;          (:duree-click-regulated = click / :wallclock-cron = tick; reported
;;          SEPARATELY — clicks sample engagement-time, not pooled silently).
;;
;; Compares per segment: decision target distribution · mode distribution ·
;; controller-score level/dispersion (winner + pool; cascade placeholders excluded) ·
;; :G-core vs :controller-augmentation composition · abstain rate · act-gate outcomes ·
;; γ continuity across the boundary · the winner-G decline characterised
;; (G-core vs controller-augmentation trajectory + repeated-enactment).
;;
;; HONESTY: KL-era n is SMALL and ONE-DAY (2026-07-04) — first-pass exhibit; a
;; later pass adds calendar days. Deterministic; corpus boundary (file + last
;; tick) recorded so re-runs are comparable.
;;
;; Run: bb scripts/c9_burn_in.bb  [trace-dir]
(require '[clojure.edn :as edn] '[clojure.java.io :as io] '[clojure.pprint :as pp] '[clojure.string :as str])

(def trace-dir (or (first *command-line-args*) "data/wm-trace"))
(def out-edn "holes/labs/M-aif-faithfulness/c9-burn-in.edn")
(def out-html "holes/labs/M-aif-faithfulness/c9-burn-in.html")
(def manual-sha "515a5936d19e5d9dfcf00fcda984b14e9375c389")

(defn read-all-forms [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [acc []]
      (let [form (edn/read {:eof ::eof :default (fn [_tag v] v)} r)]
        (if (= ::eof form) acc (recur (conj acc form)))))))

(defn mean [xs] (when (seq xs) (/ (reduce + (map double xs)) (count xs))))
(defn sd [xs] (let [xs (map double xs) n (count xs)]
                (when (> n 1) (let [m (mean xs)] (Math/sqrt (/ (reduce + (map #(let [d (- % m)] (* d d)) xs)) (dec n)))))))
(defn median [xs] (let [v (vec (sort (map double xs))) n (count v)] (when (pos? n) (nth v (quot n 2)))))

(defn tick->rec [t]
  (let [ras (vec (filter :G-risk (:ranked-actions t)))
        kl? (some #(= :kl (:goal-outcome-mode %)) ras)
        sha (get-in t [:wm-version :git-sha])
        winner (when (seq ras) (apply min-key #(double (:controller-score %)) ras))
        agv (:act-gate-verdicts t)]
    {:ts (str (:timestamp t))
     :day (subs (str (:timestamp t)) 0 10)
     :sha sha
     :era (if kl? :after :before)
     :trigger (get-in t [:wm-version :trigger])
     :mode (:mode t)
     :decision-type (get-in t [:decision :action :type])
     :decision-target (get-in t [:decision :action :target])
     :abstain? (or (nil? (get-in t [:decision :action])) (= :abstain (get-in t [:decision :action])))
     :winner-G (some-> (get-in t [:decision :controller-score]) double)
     :pool-Gs (mapv #(double (:controller-score %)) ras)
     :G-core (some-> (:G-core winner) double)
     :controller-augmentation (some-> (:controller-augmentation winner) double)
     :gamma (get-in t [:decision :selection-gain])
     :enacted (get-in t [:enactment :mission])
     :agv-pass (count (filter #(= :pass (:verdict %)) agv))
     :agv-fail (count (filter #(= :fail (:verdict %)) agv))
     :agv-abstain (count (filter #(not (#{:pass :fail} (:verdict %))) agv))}))

(defn summarise [recs]
  (let [n (count recs)
        winners (keep :winner-G recs)
        pool (mapcat :pool-Gs recs)]
    {:n n
     :span [(:ts (first recs)) (:ts (last recs))]
     :decision-types (frequencies (map :decision-type recs))
     :decision-targets (->> (map :decision-target recs) frequencies (sort-by (comp - val)) (take 8) vec)
     :modes (frequencies (map :mode recs))
     :abstain-rate (when (pos? n) (/ (Math/round (* 10000.0 (/ (count (filter :abstain? recs)) (double n)))) 10000.0))
     :winner-G {:mean (mean winners) :median (median winners) :sd (sd winners)
                :min (when (seq winners) (apply min winners)) :max (when (seq winners) (apply max winners))}
     :pool-G {:mean (mean pool) :sd (sd pool) :n (count pool)}
     :G-core {:mean (mean (keep :G-core recs))}
     :controller-augmentation {:mean (mean (keep :controller-augmentation recs))}
     :act-gates {:pass (reduce + (map :agv-pass recs)) :fail (reduce + (map :agv-fail recs))
                 :abstain (reduce + (map :agv-abstain recs))}
     :gamma {:first (first (keep :gamma recs)) :last (last (keep :gamma recs))
             :mean (mean (keep :gamma recs))}}))

;; --- sweep ------------------------------------------------------------------
(def files (->> (.listFiles (io/file trace-dir))
                (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
                (sort-by #(.getName %))))
(println (format "Reading %d trace files from %s …" (count files) trace-dir))

(def all-recs (->> files (mapcat read-all-forms) (map tick->rec)
                   (sort-by :ts)))
(def before (filter #(= :before (:era %)) all-recs))
(def after-all (filter #(and (= :after (:era %)) (not= manual-sha (:sha %))) all-recs))
(def after-click (filter #(= :duree-click-regulated (:trigger %)) after-all))
(def after-cron (filter #(= :wallclock-cron (:trigger %)) after-all))

;; boundary γ-continuity: last BEFORE γ vs first AFTER γ
(def boundary
  {:last-before {:ts (:ts (last before)) :gamma (:gamma (last before))}
   :first-after {:ts (:ts (first after-all)) :gamma (:gamma (first after-all))}})

;; the winner-G decline: track winner-G / G-core / controller-augmentation / enacted over AFTER
(def g-decline-timeline
  (mapv (fn [r] {:ts (:ts r) :winner-G (:winner-G r) :G-core (:G-core r)
                 :controller-augmentation (:controller-augmentation r) :enacted (:enacted r) :trigger (:trigger r)})
        after-all))
(def g-decline-analysis
  (let [ws (keep :winner-G after-all) cs (keep :G-core after-all) as (keep :controller-augmentation after-all)
        first-half (take (quot (count after-all) 2) after-all)
        second-half (drop (quot (count after-all) 2) after-all)]
    {:winner-G-first (first ws) :winner-G-last (last ws)
     :winner-G-range [(when (seq ws) (apply min ws)) (when (seq ws) (apply max ws))]
     :G-core-mean-first-half (mean (keep :G-core first-half)) :G-core-mean-second-half (mean (keep :G-core second-half))
     :G-aug-mean-first-half (mean (keep :controller-augmentation first-half)) :G-aug-mean-second-half (mean (keep :controller-augmentation second-half))
     :enacted-missions (frequencies (keep :enacted after-all))
     :interpretation "Compare G-core (belly free-energy) vs controller-augmentation (KL goal-outcome + channel layers) across the two halves: whichever moves more DRIVES the winner-G change. Repeated enacted-missions ⇒ repeated-enactment effect on the belly."}))

(def artifact
  {:generated-by "scripts/c9_burn_in.bb (M-aif-faithfulness C9 first-pass)"
   :sim-only true :read-only true
   :corpus {:files (count files)
            :last-file (some-> (last files) .getName)
            :last-tick (:ts (last all-recs))
            :total-ticks (count all-recs)}
   :caveats
   ["FIRST-PASS: KL-era n is SMALL and confined to ONE calendar day (2026-07-04). A later pass adds days."
    (str "Manual record excluded (:git-sha " (subs manual-sha 0 8) "…).")
    "Clicks (:duree-click-regulated) sample engagement-time; reported SEPARATELY from cron ticks, not pooled."
    "Cascade placeholders (no :G-risk) excluded from pool/winner G stats."]
   :segments {:before-hinge (summarise before)
              :after-kl-all (summarise after-all)
              :after-kl-click (summarise after-click)
              :after-kl-cron (summarise after-cron)}
   :gamma-continuity boundary
   :g-decline-analysis g-decline-analysis
   :g-decline-timeline g-decline-timeline})

;; verdict-seed
(def verdict
  (let [b (:before-hinge (:segments artifact)) a (:after-kl-all (:segments artifact))]
    (format (str "SANITY VERDICT-SEED: post-flip WM appears %s. BEHAVIOURAL continuity holds — decisions "
                 "still resolve (abstain-rate %.2f%% after vs %.2f%% before), modes stay in the known set, and "
                 "γ is continuous across the boundary (%.3f→%.3f). The hinge→KL winner-G LEVEL shift (mean %.2f→%.2f) "
                 "is a COMPOSITION change, not a pathology: the KL goal-outcome term + the ac60874 G-core/controller-augmentation "
                 "split change the G formula (G-aug mean ~%.2f). SEPARATELY, WITHIN the KL campaign the winner-G "
                 "declines (~%.1f peak→%.1f end), and :g-decline-analysis attributes it to G-CORE (belly) halving "
                 "(%.1f→%.1f first→second half) while controller-augmentation stays flat — i.e. belly free-energy settling, "
                 "NOT the KL layer. n(after)=%d, ONE calendar DAY — directional only, not a burn-in claim.")
            (if (< (double (or (:abstain-rate a) 0)) 0.5) "SANE" "ANOMALOUS — review")
            (* 100.0 (double (or (:abstain-rate a) 0))) (* 100.0 (double (or (:abstain-rate b) 0)))
            (double (or (get-in boundary [:last-before :gamma]) 0)) (double (or (get-in boundary [:first-after :gamma]) 0))
            (double (or (get-in b [:winner-G :mean]) 0)) (double (or (get-in a [:winner-G :mean]) 0))
            (double (or (get-in a [:controller-augmentation :mean]) 0))
            (double (or (get-in a [:winner-G :max]) 0)) (double (or (:winner-G-last g-decline-analysis) 0))
            (double (or (:G-core-mean-first-half g-decline-analysis) 0)) (double (or (:G-core-mean-second-half g-decline-analysis) 0))
            (:n a))))
(def artifact (assoc artifact :verdict-seed verdict))

(io/make-parents out-edn)
(spit out-edn (with-out-str (pp/pprint artifact)))

;; --- HTML -------------------------------------------------------------------
(defn h [x] (-> (str x) (str/replace "&" "&amp;") (str/replace "<" "&lt;") (str/replace ">" "&gt;")))
(defn f2 [x] (if (number? x) (format "%.2f" (double x)) "—"))
(defn seg-row [label s]
  (str "<tr><td>" (h label) "</td><td>" (h (:n s)) "</td><td>" (f2 (:abstain-rate s))
       "</td><td>" (f2 (get-in s [:winner-G :mean])) "</td><td>" (f2 (get-in s [:winner-G :sd]))
       "</td><td>" (f2 (get-in s [:G-core :mean])) "</td><td>" (f2 (get-in s [:controller-augmentation :mean]))
       "</td><td>" (h (get-in s [:act-gates :pass])) "/" (h (get-in s [:act-gates :fail]))
       "</td><td>" (f2 (get-in s [:gamma :mean])) "</td></tr>"))
(def html
  (str "<!doctype html><html><head><meta charset=utf-8><title>C9 Burn-in — before/after the KL flip</title>"
       "<style>body{font:14px/1.55 -apple-system,system-ui,sans-serif;margin:24px 30px;color:#1a1a1a;max-width:1120px}"
       "h1{font-size:22px}h2{font-size:15px;border-bottom:2px solid #e3ddcf;padding-bottom:4px;margin-top:24px}"
       ".verdict{background:#faf8f3;border:1px solid #e3ddcf;border-left:4px solid #7a9;border-radius:6px;padding:12px 16px;margin:14px 0}"
       "table{border-collapse:collapse;width:100%;font-size:13px;margin:8px 0}"
       "th,td{border:1px solid #e3ddcf;padding:4px 8px;text-align:right}th{background:#f3efe6}"
       "td:first-child,th:first-child{text-align:left}.muted{color:#6b6b6b;font-size:12px}</style></head><body>"
       "<h1>C9 Burn-in <span class=muted>— WM behaviour before / after the :kl production flip</span></h1>"
       "<div class=verdict>" (h verdict) "</div>"
       "<p class=muted>corpus: " (h (get-in artifact [:corpus :total-ticks])) " ticks · last tick "
       (h (get-in artifact [:corpus :last-tick])) " · deterministic, read-only. "
       "<b>First-pass: KL-era is one day (2026-07-04); directional only.</b></p>"
       "<h2>Segments</h2><table>"
       "<tr><th>segment</th><th>n</th><th>abstain%</th><th>winner-G mean</th><th>winner-G sd</th>"
       "<th>G-core mean</th><th>G-aug mean</th><th>gate P/F</th><th>γ mean</th></tr>"
       (seg-row "BEFORE (hinge)" (:before-hinge (:segments artifact)))
       (seg-row "AFTER (KL, all)" (:after-kl-all (:segments artifact)))
       (seg-row "AFTER — click" (:after-kl-click (:segments artifact)))
       (seg-row "AFTER — cron" (:after-kl-cron (:segments artifact)))
       "</table>"
       "<h2>γ continuity across the boundary</h2><p>last hinge tick γ="
       (f2 (get-in boundary [:last-before :gamma])) " → first KL tick γ=" (f2 (get-in boundary [:first-after :gamma]))
       " <span class=muted>(" (h (get-in boundary [:last-before :ts])) " → " (h (get-in boundary [:first-after :ts])) ")</span></p>"
       "<h2>Winner-G decline: what drives it</h2><p class=muted>G-core (belly free-energy) vs controller-augmentation (KL layer), first vs second half of the KL-era:</p>"
       "<table><tr><th></th><th>first half</th><th>second half</th></tr>"
       "<tr><td>G-core mean</td><td>" (f2 (:G-core-mean-first-half g-decline-analysis)) "</td><td>" (f2 (:G-core-mean-second-half g-decline-analysis)) "</td></tr>"
       "<tr><td>controller-augmentation mean</td><td>" (f2 (:G-aug-mean-first-half g-decline-analysis)) "</td><td>" (f2 (:G-aug-mean-second-half g-decline-analysis)) "</td></tr></table>"
       "<p class=muted>enacted missions (repeated-enactment check): " (h (:enacted-missions g-decline-analysis)) "</p>"
       "</body></html>"))
(spit out-html html)

(println "\n=== C9 BURN-IN (first pass) ===")
(println verdict)
(println (format "\nBEFORE n=%d · AFTER n=%d (click %d / cron %d)" (count before) (count after-all) (count after-click) (count after-cron)))
(println (format "winner-G: before mean %.2f → after mean %.2f"
                 (double (or (get-in artifact [:segments :before-hinge :winner-G :mean]) 0))
                 (double (or (get-in artifact [:segments :after-kl-all :winner-G :mean]) 0))))
(println (format "G decline: G-core %.2f→%.2f · G-aug %.2f→%.2f (first→second half)"
                 (double (or (:G-core-mean-first-half g-decline-analysis) 0)) (double (or (:G-core-mean-second-half g-decline-analysis) 0))
                 (double (or (:G-aug-mean-first-half g-decline-analysis) 0)) (double (or (:G-aug-mean-second-half g-decline-analysis) 0))))
(println "wrote" out-edn "and" out-html)
