#!/usr/bin/env bb
;; wm_round_exhibit.bb — a LIVE (posthoc) argue-exhibit for the most recent WM round.
;;
;; Mirrors holes/labs/M-evaluate-policies/exhibit/argue-exhibit.pdf, but from live
;; trace data: reads the latest wm-trace record, re-serves each cascade the round
;; evaluated (deterministic in (psi, budget)), draws it as Mermaid, and renders an
;; HTML page — round summary + one section per cascade (diagram + valuations + the
;; WM's own verdict) + a cross-cascade comparison.
;;
;; Usage: bb scripts/wm_round_exhibit.bb [out.html]     (default: /tmp/wm-round-exhibit.html)
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[cheshire.core :as json]
         '[babashka.process :as p])

(def code (str (System/getProperty "user.home") "/code"))
(def serve-py (str code "/futon3a/.venv/bin/python"))
(def serve-script (str code "/futon3a/holes/labs/M-memes-arrows/cascade_serve.py"))
(def phylo-path (str code "/futon6/data/pattern-phylogeny-edges.json"))
(def doc-roots (for [r ["futon0" "futon1" "futon2" "futon3" "futon3a" "futon3b"
                        "futon3c" "futon3c-index-check" "futon4" "futon5" "futon5a" "futon6" "futon7"]
                     s ["holes/missions" "holes"]]
                 (str code "/" r "/" s)))

(defn latest-trace []
  (->> (io/file (str code "/futon2/data/wm-trace"))
       .listFiles seq
       (filter #(re-find #"wm-trace-.*\.edn$" (.getName %)))
       (sort-by #(.getName %)) last))

(defn read-last-record [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [prev nil]
      (let [x (edn/read {:eof ::eof :default (fn [_ v] v)} r)]
        (if (= x ::eof) prev (recur x))))))

(defn mission->psi
  "Replicate cascade_lane.clj's mission->psi (have→want), then append the ranked
   action's rationale — exactly the |psi> the WM's cascade-lane scored."
  [target rationale]
  (let [stem (-> (str target) (str/replace #"^M-" "") (str/replace "-" " "))
        f (some (fn [root] (let [p (io/file root (str target ".md"))] (when (.exists p) p))) doc-roots)
        [title status]
        (when f
          (let [lines (str/split-lines (slurp f))
                title (some #(when (str/starts-with? % "# ") (str/trim (subs % 2))) lines)
                status (some #(let [m (re-find #"(?i)^\**status\**:?\s*(.+)" %)] (second m)) lines)]
            [title status]))
        base (str stem " — want: " (subs (or title stem) 0 (min 160 (count (or title stem))))
                  (when status (str ". have: " (subs status 0 (min 160 (count status))))))]
    (str/trim (str base " " (or rationale "")))))

(defn run-serve [psi budget]
  (let [res (p/sh {:dir (str code "/futon3a/holes/labs/M-memes-arrows") :err :string :out :string}
                  serve-py serve-script psi (str budget))
        ;; the serve prints an HF/BERT banner before the JSON; take the last JSON line
        j (->> (str/split-lines (:out res)) reverse
               (some (fn [ln] (try (json/parse-string ln true) (catch Exception _ nil)))))]
    j))

(defn load-phylo []
  (let [d (json/parse-string (slurp phylo-path) true)]
    {:co (into {} (map (fn [[a b w]] [#{a b} w]) (:co_app d)))
     :desc (set (map (fn [e] [(first e) (second e)]) (or (:descent d) (:desc d))))}))

(defn stem [pid] (last (str/split (str pid) #"/")))

(defn cascade-mermaid
  "Mermaid graph of one cascade: nodes = shown patterns (rel label), edges = phylogeny
   (descent directed, co_app undirected), dashed greedy-order fallback for orphans."
  [shown {:keys [co desc]}]
  (let [order (mapv (comp stem :pattern) shown)
        idx (into {} (map-indexed (fn [i s] [s i]) order))
        node (fn [s] (str "n" (idx s)))
        rel-of (into {} (map (fn [p] [(stem (:pattern p)) (:rel p)]) shown))
        lines (atom (concat ["graph TD"]
                            (map-indexed (fn [i s]
                                           (str "  " (node s) "[\"" (inc i) ". " s "<br/>rel "
                                                (format "%.2f" (double (or (rel-of s) 0))) "\"]"))
                                         order)))
        drawn (atom #{})]
    ;; descent among shown (directed)
    (doseq [[a b] desc :when (and (idx a) (idx b) (not (@drawn #{a b})))]
      (swap! drawn conj #{a b})
      (swap! lines concat [(str "  " (node a) " --> " (node b))]))
    ;; co_app among shown (undirected), strongest first
    (doseq [[pr w] (sort-by (comp - val) co)
            :let [[a b] (seq pr)]
            :when (and (idx a) (idx b) (not (@drawn #{a b})))]
      (swap! drawn conj #{a b})
      (swap! lines concat [(str "  " (node a) " --- " (node b))]))
    ;; greedy-order fallback so orphans stay legible
    (doseq [i (range (dec (count order)))
            :let [a (order i) b (order (inc i))]
            :when (not (some @drawn [#{a b}]))]
      (swap! lines concat [(str "  " (node a) " -.-> " (node b))]))
    (str/join "\n" @lines)))

(defn badge [verdict]
  (let [v (name (or verdict :?))]
    (str "<span class=\"badge " v "\">" (str/upper-case v) "</span>")))

(defn num [x] (if (number? x) (format "%.3g" (double x)) (str (or x "—"))))

(defn cascade-section [{:keys [mission verdict delta-G]} serve phylo]
  (if-not serve
    (str "<section><h2>" mission " " (badge verdict)
         "</h2><p class=\"warn\">cascade did not construct (serve unavailable / no patterns).</p></section>")
    (str "<section><h2>" mission " " (badge verdict) "</h2>"
         "<div class=\"row\">"
         "<div class=\"diagram\"><pre class=\"mermaid\">" (cascade-mermaid (:shown serve) phylo) "</pre></div>"
         "<table class=\"vals\">"
         "<tr><th>size</th><td>" (:size serve) "</td><th>wholeness</th><td>" (num (:wholeness serve)) "</td></tr>"
         "<tr><th>accuracy</th><td>" (num (:accuracy serve)) "</td><th>complexity</th><td>" (num (:complexity serve)) "</td></tr>"
         "<tr><th>F (acc−λ·cx)</th><td>" (num (:F-free-energy serve)) "</td><th>λ</th><td>" (num (:lambda serve)) "</td></tr>"
         "<tr><th>WM verdict</th><td>" (name (or verdict :?)) "</td><th>ΔG (rollout)</th><td>" (num delta-G) "</td></tr>"
         "</table></div>"
         "<p class=\"psi\">|ψ⟩ &nbsp; " (str/replace (or (:psi serve) "") "<" "&lt;") "</p>"
         "</section>")))

(defn -main [& args]
  (let [out (or (first args) "/tmp/wm-round-exhibit.html")
        f (latest-trace)
        t (read-last-record f)
        w (first (:ranked-actions t))
        action (get-in t [:decision :action])
        ra-by-mission (into {} (map (fn [e] [(get-in e [:action :target]) (get-in e [:action :rationale])])
                                    (:ranked-actions t)))
        phylo (load-phylo)
        cascades (:act-gate-verdicts t)
        _ (binding [*out* *err*] (println "serving" (count cascades) "cascades (model load per cascade, ~10s each)…"))
        served (mapv (fn [c]
                       (binding [*out* *err*] (println "  serve:" (:mission c)))
                       (assoc c :serve (run-serve (mission->psi (:mission c) (ra-by-mission (:mission c))) 6)))
                     cascades)
        gb (fn [k] (get w k))
        html (str "<!doctype html><html><head><meta charset=\"utf-8\">"
                  "<title>Live WM Round — " (:timestamp t) "</title>"
                  "<script type=\"module\">import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs';mermaid.initialize({startOnLoad:true,theme:'neutral'});</script>"
                  "<style>body{font:15px/1.5 system-ui,sans-serif;max-width:960px;margin:2rem auto;padding:0 1rem;color:#223}"
                  "h1{font-size:22px}h2{font-size:17px;border-bottom:1px solid #dde;padding-bottom:4px}"
                  ".summary{background:#f5f7f6;border:1px solid #dde;border-radius:8px;padding:12px 16px}"
                  ".row{display:flex;gap:18px;align-items:flex-start;flex-wrap:wrap}.diagram{flex:1 1 340px}"
                  ".vals{border-collapse:collapse;font-size:13px}.vals th{text-align:right;color:#667;padding:2px 8px;font-weight:600}.vals td{padding:2px 12px 2px 0;font-variant-numeric:tabular-nums}"
                  ".badge{font-size:11px;padding:1px 8px;border-radius:10px;color:#fff;vertical-align:middle}.badge.pass{background:#3f8b57}.badge.fail{background:#b1543a}"
                  ".psi{font-size:12px;color:#778;border-left:3px solid #ccd;padding-left:10px}.warn{color:#b1543a}"
                  "table.cross{border-collapse:collapse;font-size:13px;width:100%}table.cross th,table.cross td{border:1px solid #dde;padding:4px 8px;text-align:left}</style></head><body>"
                  "<h1>Live War Machine round <small style=\"color:#889;font-weight:400\">" (:timestamp t) "</small></h1>"
                  "<div class=\"summary\"><b>" (name (or (:mode t) :?)) "</b> · chose <b>"
                  (name (or (:type action) :?)) " → " (:target action) "</b>"
                  " · G " (num (:G-total w)) " · risk-mode <b>" (name (or (:risk-mode w) :?)) "</b><br>"
                  "<span style=\"font-size:13px\">EFE: risk " (num (gb :G-risk)) " · ambiguity " (num (gb :G-ambiguity))
                  " · info " (num (gb :G-info)) " · goal-outcome " (num (gb :G-goal-outcome))
                  " · survival " (num (gb :G-survival)) "</span><br>"
                  "<span style=\"font-size:13px\">Realized G " (num (get-in t [:realized-outcome :realized-G]))
                  " vs expected " (num (get-in t [:realized-outcome :expected-G])) "</span>"
                  (when-let [rat (:rationale action)] (str "<p class=\"psi\">" rat "</p>"))
                  "</div>"
                  "<p style=\"color:#778;font-size:13px\">The cascades this round evaluated, drawn from live serve data "
                  "(deterministic in |ψ⟩, budget) — the same structure as the M-evaluate-policies argue-exhibit, posthoc.</p>"
                  (str/join "" (map #(cascade-section % (:serve %) phylo) served))
                  ;; cross-cascade comparison
                  "<h2>Cross-cascade comparison</h2><table class=\"cross\">"
                  "<tr><th>cascade</th><th>size</th><th>wholeness</th><th>F</th><th>WM verdict</th><th>ΔG</th></tr>"
                  (str/join "" (map (fn [c] (let [s (:serve c)]
                                              (str "<tr><td>" (:mission c) "</td><td>" (or (:size s) "—")
                                                   "</td><td>" (num (:wholeness s)) "</td><td>" (num (:F-free-energy s))
                                                   "</td><td>" (name (or (:verdict c) :?)) "</td><td>" (num (:delta-G c)) "</td></tr>")))
                                    served))
                  "</table>"
                  "<p style=\"color:#99a;font-size:11px;margin-top:2rem\">Generated by scripts/wm_round_exhibit.bb from "
                  (.getName f) " · latest record " (:timestamp t) "</p>"
                  "</body></html>")]
    (spit out html)
    (println "wrote" out "(" (count served) "cascades )")))

(apply -main *command-line-args*)
