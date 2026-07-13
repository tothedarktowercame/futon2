#!/usr/bin/env bb
;; wm_clicks_exhibit.bb — M-aif-faithfulness: THE visual surface for WM click /
;; burn-in evidence (owner claude-12). Joe's ask: "inspectable surface that is both
;; aggregatable and meaningful." ONE visual face over the c9/ledger EDNs — not a new
;; numeric artifact. Reads data/wm-trace/* + data/r18-badges.edn.
;;
;; DETERMINISTIC: same corpus ⇒ byte-identical output. NO wall-clock stamps;
;; provenance footer = corpus boundary (last file + last tick ts) + generator name.
;;
;; Palette (dataviz method, validated both modes): series color follows the ENTITY.
;;   winner-G #2a78d6 (dark #3987e5) · G-core #1baf7a (dark #199e70) ·
;;   controller-augmentation #eda100 (dark #c98500). Light contrast WARN on aqua/yellow ⇒
;;   RELIEF: direct line-end labels + the data table (both mandatory, present).
;;   Trigger encoded by SHAPE not color: click = filled dot, cron = open ring.
;;
;; Run: bb scripts/wm_clicks_exhibit.bb  [trace-dir]
(require '[clojure.edn :as edn] '[clojure.java.io :as io] '[clojure.string :as str])

(def trace-dir (or (first *command-line-args*) "data/wm-trace"))
(def out-html "holes/labs/M-aif-faithfulness/wm-clicks-exhibit.html")
(def manual-sha "515a5936d19e5d9dfcf00fcda984b14e9375c389")
(def flip-ts "2026-07-04T06:33:00Z")   ; cd0d25d :risk-mode :kl production flip

(defn read-all-forms [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [acc []]
      (let [form (edn/read {:eof ::eof :default (fn [_tag v] v)} r)]
        (if (= ::eof form) acc (recur (conj acc form)))))))

(defn epoch-min [ts] (/ (.toEpochMilli (java.time.Instant/parse ts)) 60000.0))
(defn hhmm [ts] (subs ts 11 16))
(defn h [x] (-> (str x) (str/replace "&" "&amp;") (str/replace "<" "&lt;") (str/replace ">" "&gt;") (str/replace "\"" "&quot;")))
(defn f2 [x] (if (number? x) (format "%.2f" (double x)) "—"))

;; --- data --------------------------------------------------------------------
(def files (->> (.listFiles (io/file trace-dir))
                (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
                (sort-by #(.getName %))))
(def all (->> files (mapcat read-all-forms)
              (map (fn [t]
                     (let [ras (vec (filter :G-risk (:ranked-actions t)))
                           kl? (some #(= :kl (:goal-outcome-mode %)) ras)
                           w (when (seq ras) (apply min-key #(double (:controller-score %)) ras))]
                       {:ts (str (:timestamp t))
                        :sha (get-in t [:wm-version :git-sha])
                        :kl? kl?
                        :trigger (get-in t [:wm-version :trigger])
                        :winner-G (some-> (get-in t [:decision :controller-score]) double)
                        :G-core (some-> (:G-core w) double)
                        :G-aug (some-> (:controller-augmentation w) double)
                        :gamma (some-> (get-in t [:decision :selection-gain]) double)
                        :decision-type (get-in t [:decision :action :type])
                        :decision-target (get-in t [:decision :action :target])
                        :abstain? (nil? (get-in t [:decision :action]))})))
              (sort-by :ts)))
(def before (filter #(not (:kl? %)) all))
(def kl (filter #(and (:kl? %) (not= manual-sha (:sha %))) all))
(def clicks (filter #(= :duree-click-regulated (:trigger %)) kl))
(def crons  (filter #(= :wallclock-cron (:trigger %)) kl))
(def badges (frequencies (map (fn [[_ v]] (:badge v)) (:quantities (edn/read-string (slurp "data/r18-badges.edn"))))))

;; --- SVG line-chart helper ---------------------------------------------------
(def W 900) (def PL 56) (def PR 120) (def PT 18) (def PB 34)
(defn xscale [t0 t1 h-plot] (fn [t] (+ PL (* (- W PL PR) (if (= t1 t0) 0.5 (/ (- t t0) (- t1 t0)))))))
(defn yscale [y0 y1 H] (fn [y] (- (- H PB) (* (- H PT PB) (if (= y1 y0) 0.5 (/ (- y y0) (- y1 y0)))))))

(defn line-path [pts xf yf] (str "M " (str/join " L " (map (fn [[x y]] (str (format "%.1f" (double (xf x))) " " (format "%.1f" (double (yf y))))) pts))))

(defn g-chart []
  (let [H 320
        recs kl
        t0 (epoch-min flip-ts)
        t1 (apply max (map #(epoch-min (:ts %)) recs))
        ys (concat (keep :winner-G recs) (keep :G-core recs) (keep :G-aug recs))
        y0 (min 0.0 (apply min ys)) y1 (* 1.08 (apply max ys))
        xf (xscale t0 t1 H) yf (yscale y0 y1 H)
        series [["winner-G" :winner-G "var(--c-winner)"] ["G-core" :G-core "var(--c-core)"] ["controller-augmentation" :G-aug "var(--c-aug)"]]
        first-kl (epoch-min (:ts (first recs)))
        marker (fn [r ser col]
                 (let [x (xf (epoch-min (:ts r))) y (yf (double (get r ser)))]
                   (if (= :duree-click-regulated (:trigger r))
                     (format "<circle cx='%.1f' cy='%.1f' r='4.2' fill='%s' stroke='var(--surface)' stroke-width='2'/>" x y col)
                     (format "<circle cx='%.1f' cy='%.1f' r='4.4' fill='var(--surface)' stroke='%s' stroke-width='2'/>" x y col))))]
    (str "<svg viewBox='0 0 " W " " H "' class='chart' role='img' aria-label='G trajectory across the KL-era click campaign'>"
         ;; y gridlines + labels
         (apply str (for [g (range 6) :let [yv (+ y0 (* (/ g 5.0) (- y1 y0))) py (yf yv)]]
                      (str "<line x1='" PL "' x2='" (- W PR) "' y1='" (format "%.1f" py) "' y2='" (format "%.1f" py) "' class='grid'/>"
                           "<text x='" (- PL 6) "' y='" (format "%.1f" (+ py 3)) "' class='axis' text-anchor='end'>" (format "%.0f" yv) "</text>")))
         ;; campaign band
         (let [cx0 (xf (epoch-min (:ts (first clicks)))) cx1 (xf (epoch-min (:ts (last clicks))))]
           (str "<rect x='" (format "%.1f" cx0) "' y='" PT "' width='" (format "%.1f" (- cx1 cx0)) "' height='" (- H PT PB) "' class='band'/>"
                "<text x='" (format "%.1f" (/ (+ cx0 cx1) 2)) "' y='" (+ PT 12) "' class='axis' text-anchor='middle'>click campaign-1</text>"))
         ;; flip + first-tick annotation rules
         (let [fx (xf t0) kx (xf first-kl)]
           (str "<line x1='" (format "%.1f" fx) "' x2='" (format "%.1f" fx) "' y1='" PT "' y2='" (- H PB) "' class='rule'/>"
                "<text x='" (format "%.1f" (+ fx 3)) "' y='" (+ PT 24) "' class='axis rule-lbl'>cd0d25d flip 06:33Z</text>"
                "<line x1='" (format "%.1f" kx) "' x2='" (format "%.1f" kx) "' y1='" PT "' y2='" (- H PB) "' class='rule'/>"
                "<text x='" (format "%.1f" (+ kx 3)) "' y='" (+ PT 38) "' class='axis rule-lbl'>first KL tick 07:03Z</text>"))
         ;; series lines + markers + direct end labels (guard records missing a value)
         (apply str (for [[nm k col] series
                          :let [rr (filter #(number? (get % k)) recs)
                                pts (map (fn [r] [(epoch-min (:ts r)) (double (get r k))]) rr)
                                lastr (last rr)]
                          :when (seq rr)]
                      (str "<path d='" (line-path pts xf yf) "' fill='none' stroke='" col "' stroke-width='2'/>"
                           (apply str (map #(marker % k col) rr))
                           "<text x='" (format "%.1f" (+ (xf (epoch-min (:ts lastr))) 8)) "' y='" (format "%.1f" (+ (yf (double (get lastr k))) 3)) "' class='endlbl' style='fill:" col "'>" nm "</text>")))
         ;; x labels (first/last time)
         "<text x='" PL "' y='" (- H 8) "' class='axis'>" (hhmm (:ts (first recs))) "Z</text>"
         "<text x='" (- W PR) "' y='" (- H 8) "' class='axis' text-anchor='end'>" (hhmm (:ts (last recs))) "Z</text>"
         "<rect id='g-hit' x='" PL "' y='" PT "' width='" (- W PL PR) "' height='" (- H PT PB) "' fill='transparent'/>"
         "</svg>")))

(defn gamma-chart []
  (let [H 150
        tail (filter #(number? (:gamma %)) (concat (take-last 10 before) kl))
        boundary-x-ts (:ts (first kl))
        t0 (epoch-min (:ts (first tail))) t1 (epoch-min (:ts (last tail)))
        ys (keep :gamma tail) y0 (* 0.98 (apply min ys)) y1 (* 1.02 (apply max ys))
        xf (xscale t0 t1 H) yf (yscale y0 y1 H)
        pts (map (fn [r] [(epoch-min (:ts r)) (:gamma r)]) tail)]
    (str "<svg viewBox='0 0 " W " " H "' class='chart' role='img' aria-label='selection gain trajectory'>"
         (apply str (for [g (range 4) :let [yv (+ y0 (* (/ g 3.0) (- y1 y0))) py (yf yv)]]
                      (str "<line x1='" PL "' x2='" (- W PR) "' y1='" (format "%.1f" py) "' y2='" (format "%.1f" py) "' class='grid'/>"
                           "<text x='" (- PL 6) "' y='" (format "%.1f" (+ py 3)) "' class='axis' text-anchor='end'>" (format "%.2f" yv) "</text>")))
         (let [bx (xf (epoch-min boundary-x-ts))]
           (str "<line x1='" (format "%.1f" bx) "' x2='" (format "%.1f" bx) "' y1='" PT "' y2='" (- H PB) "' class='rule'/>"
                "<text x='" (format "%.1f" (- bx 4)) "' y='" (+ PT 12) "' class='axis rule-lbl' text-anchor='end'>hinge→KL boundary</text>"))
         "<path d='" (line-path pts xf yf) "' fill='none' stroke='var(--c-winner)' stroke-width='2'/>"
         (apply str (map (fn [[x y]] (format "<circle cx='%.1f' cy='%.1f' r='3.2' fill='var(--c-winner)' stroke='var(--surface)' stroke-width='1.5'/>" (double (xf x)) (double (yf y)))) pts))
         "<text x='" (- W PR -8) "' y='" (format "%.1f" (+ (yf (:gamma (last tail))) 3)) "' class='endlbl' style='fill:var(--c-winner)'>γ</text>"
         "</svg>")))

;; --- decision-type 100% bars -------------------------------------------------
(def dt-colors ["#2a78d6" "#1baf7a" "#eda100" "#8b5cf6" "#9a9791"])  ; categorical order; 5th+ = Other gray
(defn dist-bar [label recs]
  (let [n (count recs)
        freq (->> (map :decision-type recs) frequencies (sort-by (comp - val)))
        top (take 4 freq) other (reduce + (map val (drop 4 freq)))
        segs (concat (map-indexed (fn [i [k c]] [k c (nth dt-colors i)]) top)
                     (when (pos? other) [[:Other other (last dt-colors)]]))]
    (str "<div class='bar-label'>" (h label) " <span class='muted'>(n=" n ")</span></div>"
         "<svg viewBox='0 0 900 34' class='bar' role='img' aria-label='" (h label) " decision types'>"
         (loop [x 0.0 acc "" ss segs]
           (if (empty? ss) acc
               (let [[k c col] (first ss) frac (/ (double c) n) wpx (* 894.0 frac)]
                 (recur (+ x wpx 2.0)
                        (str acc "<rect x='" (format "%.1f" x) "' y='2' width='" (format "%.1f" (max 0.0 (- wpx 2))) "' height='30' rx='4' fill='" col "'/>"
                             (when (> wpx 46) (str "<text x='" (format "%.1f" (+ x 6)) "' y='21' class='barlbl'>" (h (name k)) " " c " · " (format "%.0f%%" (* 100 frac)) "</text>")))
                        (rest ss)))))
         "</svg>")))

;; --- JSON for tooltip JS -----------------------------------------------------
(defn kl-json []
  (str "[" (str/join "," (map (fn [r] (format "{\"ts\":\"%s\",\"w\":%.3f,\"c\":%.3f,\"a\":%.3f,\"g\":%.3f,\"trig\":\"%s\",\"tgt\":\"%s\"}"
                                              (hhmm (:ts r)) (double (or (:winner-G r) 0)) (double (or (:G-core r) 0))
                                              (double (or (:G-aug r) 0)) (double (or (:gamma r) 0))
                                              (if (= :duree-click-regulated (:trigger r)) "click" "cron")
                                              (str (:decision-target r)))) kl)) "]"))

;; --- assemble ----------------------------------------------------------------
(def html
  (str "<!doctype html><html lang=en><head><meta charset=utf-8>"
       "<meta name=viewport content='width=device-width,initial-scale=1'>"
       "<title>WM clicks &amp; burn-in exhibit</title><style>"
       ".viz-root{--surface:#fcfcfb;--ink:#0b0b0b;--ink2:#52514e;--c-winner:#2a78d6;--c-core:#1baf7a;--c-aug:#eda100;"
       "--grid:#e7e5df;--band:rgba(120,120,120,.08);--rule:#b7b4ab;"
       "font:14px/1.5 -apple-system,system-ui,sans-serif;background:var(--surface);color:var(--ink);margin:0;padding:24px 30px;max-width:1000px}"
       "@media(prefers-color-scheme:dark){.viz-root{--surface:#1a1a19;--ink:#fff;--ink2:#c3c2b7;--c-winner:#3987e5;--c-core:#199e70;--c-aug:#c98500;--grid:#33322e;--band:rgba(200,200,200,.06);--rule:#55534c}}"
       "h1{font-size:21px;margin:0 0 2px}h2{font-size:14px;color:var(--ink2);border-bottom:1px solid var(--grid);padding-bottom:4px;margin:22px 0 6px}"
       ".tiles{display:flex;gap:12px;flex-wrap:wrap;margin:12px 0}"
       ".tile{border:1px solid var(--grid);border-radius:8px;padding:9px 14px;min-width:96px}"
       ".tile .v{font-size:22px;font-weight:700}.tile .k{font-size:11px;color:var(--ink2)}"
       ".muted{color:var(--ink2)}.chart,.bar{width:100%;height:auto;display:block}"
       ".grid{stroke:var(--grid);stroke-width:1}.axis{fill:var(--ink2);font-size:10px}"
       ".band{fill:var(--band)}.rule{stroke:var(--rule);stroke-width:1;stroke-dasharray:3 3}.rule-lbl{fill:var(--ink2)}"
       ".endlbl{font-size:11px;font-weight:600}.barlbl{fill:#fff;font-size:11px;font-weight:600}.bar-label{margin:8px 0 2px;font-weight:600}"
       ".legend{display:flex;gap:16px;font-size:12px;color:var(--ink2);margin:4px 0}.legend b{display:inline-block;width:10px;height:10px;border-radius:2px;margin-right:5px;vertical-align:baseline}"
       "table{border-collapse:collapse;width:100%;font-size:12px;margin-top:6px}th,td{border:1px solid var(--grid);padding:3px 7px;text-align:right}th,td:first-child{text-align:left}"
       "#tt{position:fixed;pointer-events:none;background:var(--surface);border:1px solid var(--rule);border-radius:6px;padding:6px 9px;font-size:11px;opacity:0;box-shadow:0 2px 8px rgba(0,0,0,.15);z-index:9}"
       "footer{margin-top:20px;font-size:11px;color:var(--ink2);border-top:1px solid var(--grid);padding-top:8px}"
       "</style></head><body><div class='viz-root' data-palette='#2a78d6,#1baf7a,#eda100'>"
       "<h1>WM clicks &amp; burn-in</h1><p class='muted'>The visual face of the C9 before/after census — what the War Machine did across the 2026-07-04 click campaign, after the <code>:kl</code> production flip. Directional (one day); the numbers of record live in the c9 / ledger EDNs.</p>"
       ;; Panel 1 — stat tiles
       "<div class='tiles'>"
       "<div class='tile'><div class='v'>" (count all) "</div><div class='k'>total ticks</div></div>"
       "<div class='tile'><div class='v'>" (count kl) "</div><div class='k'>KL-era n (" (count clicks) " click / " (count crons) " cron)</div></div>"
       "<div class='tile'><div class='v'>" (format "%.0f%%" (* 100.0 (/ (count (filter :abstain? all)) (double (count all))))) "</div><div class='k'>abstain rate</div></div>"
       "<div class='tile'><div class='v'>" (get badges :analogical 0) " · " (get badges :principled-approximation 0) " · " (get badges :derived-from-FEP 0) "</div><div class='k'>badges: analogical · PA · FEP <span class='muted'>(was 9/7/0)</span></div></div>"
       "</div>"
       ;; Panel 2 — G trajectory
       "<h2>G trajectory — KL-era, per record</h2>"
       "<div class='legend'><span><b style='background:var(--c-winner)'></b>winner-G</span><span><b style='background:var(--c-core)'></b>G-core (belly)</span><span><b style='background:var(--c-aug)'></b>controller-augmentation (KL layer)</span><span class='muted'>● click · ○ cron tick</span></div>"
       (g-chart)
       ;; Panel 3 — gamma
       "<h2>selection gain — last hinge samples → KL era</h2>"
       (gamma-chart)
       ;; Panel 4 — decision-type distributions
       "<h2>Decision type — before (hinge) vs after (KL)</h2>"
       (dist-bar "BEFORE — hinge era" before)
       (dist-bar "AFTER — KL era" kl)
       ;; Panel 5 — data table (relief leg + a11y)
       "<h2>KL-era records</h2><details><summary class='muted'>table of " (count kl) " KL-era rows (ts · trigger · winner-G · G-core · G-aug · decision · γ)</summary>"
       "<table><tr><th>ts</th><th>trigger</th><th>winner-G</th><th>G-core</th><th>G-aug</th><th>decision→target</th><th>γ</th></tr>"
       (apply str (for [r kl] (str "<tr><td>" (hhmm (:ts r)) "Z</td><td>" (h (name (or (:trigger r) :?))) "</td><td>" (f2 (:winner-G r)) "</td><td>" (f2 (:G-core r)) "</td><td>" (f2 (:G-aug r)) "</td><td>" (h (str (:decision-type r) "→" (:decision-target r))) "</td><td>" (f2 (:gamma r)) "</td></tr>")))
       "</table></details>"
       "<div id='tt'></div>"
       "<footer>Generated by <code>scripts/wm_clicks_exhibit.bb</code> · corpus boundary: " (h (some-> (last files) .getName)) " @ last tick " (h (:ts (last all))) " · deterministic (no wall-clock). Manual record " (subs manual-sha 0 8) "… excluded; clicks reported separately from cron.</footer>"
       ;; tooltip JS (crosshair on G chart)
       "<script>(function(){var D=" (kl-json) ";var svg=document.querySelector('.chart');var hit=document.getElementById('g-hit');var tt=document.getElementById('tt');if(!hit)return;"
       "var PL=" PL ",PR=" PR ",W=" W ";hit.addEventListener('mousemove',function(e){var r=svg.getBoundingClientRect();var sx=(e.clientX-r.left)/r.width*W;var frac=(sx-PL)/(W-PL-PR);var i=Math.max(0,Math.min(D.length-1,Math.round(frac*(D.length-1))));var d=D[i];"
       "tt.innerHTML='<b>'+d.ts+'Z</b> ('+d.trig+')<br>winner-G '+d.w.toFixed(2)+' · core '+d.c.toFixed(2)+' · aug '+d.a.toFixed(2)+'<br>γ '+d.g.toFixed(3)+' · →'+d.tgt;tt.style.opacity=1;tt.style.left=(e.clientX+14)+'px';tt.style.top=(e.clientY+14)+'px';});"
       "hit.addEventListener('mouseleave',function(){tt.style.opacity=0;});})();</script>"
       "</div></body></html>"))

(io/make-parents out-html)
(spit out-html html)
(println "wrote" out-html)
(println (format "corpus: %d ticks · KL-era %d (click %d / cron %d) · badges %s"
                 (count all) (count kl) (count clicks) (count crons) (pr-str badges)))
