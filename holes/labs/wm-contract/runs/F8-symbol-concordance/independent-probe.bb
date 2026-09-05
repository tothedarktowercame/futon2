#!/usr/bin/env bb
;; F8 leg 2 slice 1 -- INDEPENDENT PROBE, run by the reviewing seat BEFORE the
;; dispatch went out, so the packet's premises are checked rather than asserted.
;; Emits, mechanically: (1) the symbol census aif-equations.edn already carries
;; and its case-fold collisions; (2) every line of p4ng/sec-glossary.tex on which
;; a declared bare symbol occurs as a standalone math token.
;; Reading-level judgements are NOT made here: this prints occurrences.
(require '[clojure.edn :as edn] '[clojure.string :as str])

(def home (System/getenv "HOME"))
(def eq-path (str home "/code/futon2/holes/labs/wm-contract/aif-equations.edn"))
(def glossary (str home "/code/p4ng/sec-glossary.tex"))

(def m (edn/read-string (slurp eq-path)))
(def eqs (:equations m))
(def defs (mapv :defines eqs))
(def imps (vec (distinct (mapcat :imports eqs))))
(def exs (mapv :symbol (:exogenous m)))
(def all (vec (sort (distinct (concat defs imps exs)))))

(println "=== 1. SYMBOL CENSUS FROM aif-equations.edn ===")
(println "file:" eq-path)
(printf "defines   %2d %s%n" (count defs) (pr-str defs))
(printf "imports   %2d %s%n" (count imps) (pr-str imps))
(printf "exogenous %2d %s%n" (count exs) (pr-str exs))
(printf "union     %2d %s%n" (count all) (pr-str all))
(printf "imports neither defined nor exogenous: %s%n"
        (pr-str (vec (remove (set (concat defs exs)) imps))))

(println)
(println "=== 2. CASE-FOLD COLLISIONS WITHIN THE REGISTRY ===")
(let [g (group-by #(str/lower-case (name %)) all)
      c (into (sorted-map) (filter #(> (count (val %)) 1) g))]
  (if (seq c)
    (doseq [[k v] c] (printf "  %-8s -> %s%n" k (pr-str v)))
    (println "  none"))
  (printf "collided fold keys: %d%n" (count c)))

(println)
(println "=== 3. GLOSSARY OCCURRENCES OF DECLARED BARE SYMBOLS ===")
(println "file:" glossary)
;; The LaTeX spelling of each bare symbol under test.  A symbol is counted on a
;; line when it occurs as a standalone math token: preceded by a math delimiter,
;; brace, backslash-space, operator or whitespace, and followed by a character
;; that cannot continue an identifier (so \Pi_H counts for Pi, \Pi_{...} counts,
;; and \Phi does not count for Pi).
(def probes
  [["Pi"     #"\\Pi(?![a-zA-Z])"]
   ["pi"     #"\\pi(?![a-zA-Z])"]
   ["A"      #"(?<![a-zA-Z\\])A(?![a-zA-Z])"]
   ["T"      #"(?<![a-zA-Z\\])T(?![a-zA-Z])"]
   ["H"      #"(?<![a-zA-Z\\])H(?![a-zA-Z])"]
   ["F"      #"(?<![a-zA-Z\\])F(?![a-zA-Z])"]
   ["G"      #"(?<![a-zA-Z\\])G(?![a-zA-Z])"]
   ["C"      #"(?<![a-zA-Z\\])C(?![a-zA-Z])"]
   ["B"      #"(?<![a-zA-Z\\])B(?![a-zA-Z])"]
   ["E"      #"(?<![a-zA-Z\\])E(?![a-zA-Z])"]
   ["U"      #"(?<![a-zA-Z\\])U(?![a-zA-Z])"]
   ["o"      #"(?<![a-zA-Z\\])o(?![a-zA-Z])"]
   ["u"      #"(?<![a-zA-Z\\])u(?![a-zA-Z])"]
   ["tau"    #"\\tau(?![a-zA-Z])"]
   ["mu"     #"\\mu(?![a-zA-Z])"]
   ["alpha"  #"\\alpha(?![a-zA-Z])"]
   ["gamma"  #"\\gamma(?![a-zA-Z])"]
   ["beta"   #"\\beta(?![a-zA-Z])"]
   ["eps"    #"\\varepsilon(?![a-zA-Z])"]
   ["L"      #"(?<![a-zA-Z\\])L(?![a-zA-Z])"]
   ["S"      #"(?<![a-zA-Z\\])S(?![a-zA-Z])"]
   ["g"      #"(?<![a-zA-Z\\])g(?![a-zA-Z])"]
   ["rho"    #"\\rho(?![a-zA-Z])"]
   ["D"      #"(?<![a-zA-Z\\])D(?![a-zA-Z])"]
   ["lambda" #"\\lambda(?![a-zA-Z])"]])

(def lines (vec (str/split-lines (slurp glossary))))
(defn math-only
  "Keep only the math spans of a line, so prose capitals do not count."
  [s]
  (str/join " " (map second (re-seq #"\$([^$]*)\$" s))))
(doseq [[nm re] probes]
  (let [hits (keep-indexed
              (fn [i l] (when (re-find re (math-only l)) (inc i)))
              lines)]
    (printf "  %-7s %2d line(s): %s%n" nm (count hits) (pr-str (vec hits)))))

(println)
(println "=== 4. PARAGRAPH HEADS (for reading the occurrence lines) ===")
(doseq [[i l] (map-indexed vector lines)
        :when (str/starts-with? l "\\paragraph{")]
  (printf "  %3d  %s%n" (inc i)
          (str/replace (subs l 0 (min 90 (count l))) #"\\paragraph\{" "")))
