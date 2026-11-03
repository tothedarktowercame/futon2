#!/usr/bin/env bb
;; render_cascade_record.bb RECORD.edn OUT.html -- one cascade record
;; (:cascade/record-draft-v0, claude-16's shape) as a page a person can read:
;; the rationale first, then the units in order with what each needed and
;; what it established, the reads with their sources and cites, the
;; alternatives with G beside each, and the attestation. Joe, 2026-11-03:
;; "If I can see the cascade that justifies the selection of a mission ...
;; then I'll have a pretty good 'feel' for whether or not it is valid or useful".
(require '[clojure.edn :as edn] '[clojure.string :as str])

(def in (first *command-line-args*))
(def out (second *command-line-args*))
(def r (edn/read-string {:default (fn [_ v] v)} (slurp in)))

(defn esc [s] (-> (str s) (str/replace "&" "&amp;") (str/replace "<" "&lt;") (str/replace ">" "&gt;")))
(defn kw [x] (if (keyword? x) (name x) (pr-str x)))
(defn fmt [g] (if (number? g) (format "%.4f" (double g)) "nil"))

(def reads (:reads r))

(defn read-cell [k]
  (let [rd (get reads k)]
    (if-not rd
      (str "<span class=missing>" (esc (kw k)) " (no read)</span>")
      (str "<b>" (esc (kw k)) "</b> = " (esc (pr-str (:value rd)))
           " <span class=" (if (= :proxy (:kind rd)) "proxy" "read") ">" (name (or (:kind rd) :read)) "</span>"
           (when (:cite rd) (str "<br><code>" (esc (:cite rd)) "</code>"))
           (when (:quote rd) (str "<br><q>" (esc (:quote rd)) "</q>"))))))

(def units-html
  (str/join "\n"
    (for [u (sort-by :index (:units r))]
      (str "<li><div class=unit><b>" (esc (:index u)) ". " (esc (last (str/split (str (:pattern u)) #"/"))) "</b>"
           " <span class=step>" (esc (kw (:step u))) "</span>"
           (when (:sha256 u) (str " <code class=sha>" (subs (:sha256 u) 0 12) "</code>"))
           "<div class=quote>“" (esc (:quote u)) "”</div>"
           "<div>needs: " (str/join "; " (map read-cell (:needs u))) "</div>"
           "<div>establishes: <i>" (esc (kw (:establishes u))) "</i>"
           (when (:applied u) (str " — " (esc (:applied u)))) "</div></div></li>"))))

(def alts-html
  (str/join "\n"
    (for [a (:alternatives r)]
      (str "<tr><td>" (esc (kw (:id a))) "</td><td>" (esc (pr-str (:steps a))) "</td><td>"
           (esc (or (:pick a) "stop")) "</td><td class=num>" (fmt (:G a)) "</td></tr>"))))

(def readers-html
  (str/join "\n"
    (for [x (get-in r [:attestation :readers])]
      (str "<li>" (esc (:who x)) ": " (esc (:what x)) " <code>" (esc (:job x)) "</code> " (esc (:at x)) "</li>"))))

(def html
  (str "<!doctype html><meta charset=utf-8><title>Cascade: " (esc (kw (:id r))) "</title>
<style>body{max-width:52em;margin:2em auto;padding:0 1em;font:16px/1.5 Georgia,serif;color:#222}
h1,h2{font-family:system-ui,sans-serif}code{font-size:85%;background:#f3f3f3;padding:0 .2em}
.unit{border-left:3px solid #999;padding:.3em .8em;margin:.6em 0}.quote{color:#444;font-style:italic}
.step{font-family:monospace;color:#555}.read{color:#2a7;font-size:85%}.proxy{color:#c60;font-size:85%}
.missing{color:#c00}q{color:#555}table{border-collapse:collapse}td,th{border:1px solid #ccc;padding:.2em .6em}
.num{text-align:right;font-family:monospace}.sel{background:#eef8ee}.rationale{font-size:110%;border:1px solid #cde;padding:.8em;background:#f6fbff}
.sha{color:#888}</style>
<h1>The cascade that selected " (esc (:selected r)) "</h1>
<p class=muted>record <code>" (esc in) "</code> · schema " (esc (kw (:schema r))) " · " (esc (:applies-to r)) " · run <code>" (esc (:run r)) "</code> · " (esc (:selected-at r)) "</p>
<h2>Read as a rationale</h2>
<div class=rationale>" (esc (:rationale r)) "</div>
<h2>The cascade, step by step</h2>
<ol start=0>" units-html "</ol>
<p>edges: " (esc (pr-str (:edges r))) (when (seq (:independent r)) (str " · independent: " (esc (pr-str (:independent r))))) "</p>
<h2>Scored against the alternatives</h2>
<table><tr><th>cascade</th><th>steps</th><th>pick</th><th>G</th></tr>
<tr class=sel><td><b>" (esc (kw (:id r))) "</b> (this one)</td><td>" (esc (pr-str (mapv :step (sort-by :index (:units r))))) "</td><td>" (esc (:selected r)) "</td><td class=num>" (fmt (:G r)) "</td></tr>
" alts-html "</table>
<p>" (esc (:G-note r)) "</p>
<h2>Attestation</h2>
<p>" (esc (get-in r [:attestation :source])) "</p>
<ul>" readers-html "</ul>
<p>held-out: " (esc (pr-str (get-in r [:attestation :held-out]))) "</p>
<p><i>" (esc (get-in r [:attestation :note])) "</i></p>
<h2>Every read</h2>
<ul>" (str/join "\n" (for [[k _] reads] (str "<li>" (read-cell k) "</li>"))) "</ul>
"))

(spit out html)
(println (str "wrote " out " (" (count html) " bytes)"))
