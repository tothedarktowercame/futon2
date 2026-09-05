#!/usr/bin/env bb
;; F8 leg 2 slice 1 -- the REVIEWING seat's own column check on
;; symbol-concordance.edn. Written before the delivery was read, and run
;; against it afterwards. This is NOT the leg-2 checker (that is slice 2);
;; it resolves every pointer the registry makes and reports what does not
;; resolve. Nothing here rules on a reading.

(require '[clojure.string :as str] '[clojure.edn :as edn])

(def futon2 "/home/joe/code/futon2")
(def p4ng "/home/joe/code/p4ng")
(def mathlib4 "/home/joe/code/mathlib4")

(def conc (edn/read-string (slurp (str futon2 "/holes/labs/wm-contract/symbol-concordance.edn"))))
(def eqs  (edn/read-string (slurp (str futon2 "/holes/labs/wm-contract/aif-equations.edn"))))
(def stages (edn/read-string (slurp (str p4ng "/empirics-futon/control-stages.edn"))))

(def rows (:symbols conc))
(def findings (atom []))
(def notes (atom []))
(defn finding! [id & msg] (swap! findings conj (str id ": " (str/join " " msg))))

(defn lines-of [path] (when (.exists (java.io.File. path)) (str/split-lines (slurp path))))
(defn line-at [path n] (let [ls (lines-of path)] (when (and ls (<= 1 n (count ls))) (nth ls (dec n)))))
(defn parse-range [s]
  (cond (number? s) [s s]
        (and (string? s) (re-matches #"\d+-\d+" s)) (let [[a b] (str/split s #"-")] [(parse-long a) (parse-long b)])
        (and (string? s) (re-matches #"\d+" s)) [(parse-long s) (parse-long s)]
        :else nil))

;; A bare symbol is an ASCII name ("mu", "Pi", "Delta-F", "Q-o-pi"). The glossary
;; writes LaTeX. Candidates: the whole name, its head component, and the greek macro
;; for any component. A hit on any candidate means the cited line does write the symbol.
(def greek {"mu" "\\mu" "pi" "\\pi" "Pi" "\\Pi" "tau" "\\tau" "alpha" "\\alpha"
            "beta" "\\beta" "gamma" "\\gamma" "rho" "\\rho" "lambda" "\\lambda"
            "eps" "\\varepsilon" "eps0" "\\varepsilon" "Delta" "\\Delta"})
(defn latex-candidates [bare]
  (let [parts (str/split bare #"-")
        head (first parts)]
    (distinct (concat (keep greek parts)
                      [(str "$" head) (str "\\" head)]
                      (when (re-matches #"[A-Za-z]" head) [(str "$" head "$")])
                      [head]
                      ;; "Q(o|pi)" -> the leading alphabetic run "Q", which is how the
                      ;; glossary writes the kernel's head before its LaTeX arguments.
                      (when-let [lead (re-find #"^[A-Za-z]+" head)]
                        [(str lead "(") (str "\\widehat " lead) (str "$" lead "$")])))))

(println "=== A. ROW HYGIENE ===")
(println "rows:" (count rows))
(let [ids (map :id rows) dups (->> ids frequencies (filter #(> (val %) 1)) (map key))]
  (println "distinct :id:" (count (distinct ids)) (if (seq dups) (str "DUPLICATES " (vec dups)) "no duplicates")))
(doseq [r rows]
  (when-not (:id r) (finding! "?" ":id missing"))
  (when-not (:bare r) (finding! (:id r) ":bare missing"))
  (when-not (:fold r) (finding! (:id r) ":fold missing"))
  (when (and (:bare r) (:fold r) (not= (str/lower-case (:bare r)) (:fold r)))
    (finding! (:id r) ":fold" (pr-str (:fold r)) "is not the lower-case of :bare" (pr-str (:bare r))))
  (when-not (:reading r) (finding! (:id r) ":reading missing")))

(println)
(println "=== B. REGISTRY-SYMBOL COVERAGE (aif-equations.edn) ===")
(def eq-syms (set (concat (keep :defines (:equations eqs)) (keep :symbol (:exogenous eqs)))))
(def eq-node-of (merge (into {} (for [e (:equations eqs)] [(:defines e) (:node e)]))
                       (into {} (for [e (:exogenous eqs)] [(:symbol e) (:node e)]))))
(def claimed (->> rows (keep :registry-symbol) set))
(println "aif-equations symbols:" (count eq-syms) "claimed by concordance:" (count claimed))
(doseq [s (sort (remove claimed eq-syms))] (finding! "COVERAGE" "aif-equations symbol" s "has no concordance row"))
(doseq [s (sort (remove eq-syms claimed))] (finding! "COVERAGE" ":registry-symbol" s "is not an aif-equations symbol"))
(let [dups (->> rows (keep :registry-symbol) frequencies (filter #(> (val %) 1)))]
  (doseq [[s n] dups] (finding! "COVERAGE" ":registry-symbol" s "claimed by" n "rows")))

(println)
(println "=== C. NODE ===")
(def node-ids (set (map #(keyword (:node %)) (:nodes stages))))
(println "control-stages nodes:" (count node-ids))
(doseq [r rows]
  (when-let [nd (:node r)]
    (when-not (node-ids nd) (finding! (:id r) ":node" nd "not in control-stages.edn"))
    (when-let [rs (:registry-symbol r)]
      (let [en (eq-node-of rs)]
        (when (and en (not= en nd))
          (finding! (:id r) ":node" nd "disagrees with aif-equations :node" en "for" rs))))))

(println)
(println "=== D. GLOSSARY POINTERS ===")
(doseq [r rows]
  (let [g (:glossary r)]
    (cond
      (nil? g) (finding! (:id r) ":glossary key absent entirely")
      (:absent g) (println " " (:id r) "glossary ABSENT:" (:absent g))
      :else
      (let [path (str "/home/joe/code/" (:file g))
            ln (line-at path (:at g))]
        (if (nil? ln)
          (finding! (:id r) ":glossary" (:file g) "line" (:at g) "does not exist")
          (let [bare (:bare r)
                pats (latex-candidates bare)
                hit (some #(str/includes? ln %) pats)]
            (println (format " %-34s %-4s L%-4s %s" (str (:id r)) bare (:at g) (if hit "symbol PRESENT on line" "SYMBOL NOT ON LINE")))
            (when-not hit (finding! (:id r) ":glossary line" (:at g) "does not contain" bare))))))))

(println)
(println "=== E. LEAN POINTERS ===")
(doseq [r rows]
  (let [l (:lean r)]
    (cond
      (nil? l) (finding! (:id r) ":lean key absent entirely")
      (:absent l) (println " " (:id r) "lean ABSENT:" (:absent l))
      :else
      (let [path (str mathlib4 "/" (:file l))
            ident (:ident l)]
        (if-not (.exists (java.io.File. path))
          (finding! (:id r) ":lean file" (:file l) "does not exist")
          (let [ls (lines-of path)
                decl-re (re-pattern (str "^\\s*(?:@\\[[^\\]]*\\]\\s*)?(?:noncomputable\\s+)?(?:private\\s+)?(?:protected\\s+)?(?:def|abbrev|theorem|lemma|structure|inductive|instance|class|axiom)\\s+" (java.util.regex.Pattern/quote ident) "\\b"))
                ;; A concordance row may name a STRUCTURE FIELD (perCallPrecision is a
                ;; field of PresentRecord), which is not a top-level declaration.
                field-re (re-pattern (str "^\\s+" (java.util.regex.Pattern/quote ident) "\\s*:\\s"))
                decl-lines (keep-indexed (fn [i s] (when (or (re-find decl-re s) (re-find field-re s)) (inc i))) ls)
                [a b] (parse-range (:at l))]
            (cond
              (empty? decl-lines) (finding! (:id r) ":lean ident" ident "is neither declared nor a structure field in" (:file l))
              (and (:at l) (nil? a)) (finding! (:id r) ":lean :at" (pr-str (:at l)) "is not a line or line range")
              (and a (not (some #(<= (- a 2) % (+ b 2)) decl-lines)))
              (finding! (:id r) ":lean" ident "declared at" (vec decl-lines) "but :at says" (pr-str (:at l)))
              :else (println (format " %-34s %s @ %s:%s OK" (str (:id r)) ident (:file l) (:at l))))))))))

(println)
(println "=== F. RUNTIME POINTERS ===")
(doseq [r rows]
  (let [rt (:runtime r)]
    (cond
      (nil? rt) (finding! (:id r) ":runtime key absent entirely")
      (:absent rt) (println " " (:id r) "runtime ABSENT:" (:absent rt))
      :else
      (let [path (str futon2 "/" (:file rt))
            v (:var rt)
            nm (when v (last (str/split v #"/")))
            [a b] (parse-range (:at rt))]
        (cond
          (not (.exists (java.io.File. path))) (finding! (:id r) ":runtime file" (:file rt) "does not exist")
          (nil? a) (finding! (:id r) ":runtime :at" (pr-str (:at rt)) "is not a line or line range")
          (str/includes? (str v) " ")
          (let [ls (lines-of path)]
            (swap! notes conj (str (:id r) ": :runtime :var " (pr-str v) " is prose, not a var name; range " (:at rt) (if (<= b (count ls)) " is in file" " IS PAST END OF FILE")))
            (when (> b (count ls)) (finding! (:id r) ":runtime range end" b "past end of file" (count ls))))
          :else
          (let [ls (lines-of path)
                def-re (re-pattern (str "^\\(def(?:n|n-|macro)?-?\\s+\\^?\\{?[^\\s]*\\s*" (java.util.regex.Pattern/quote nm) "\\b|^\\(def[a-z-]*\\s+" (java.util.regex.Pattern/quote nm) "\\b"))
                def-lines (keep-indexed (fn [i s] (when (re-find def-re s) (inc i))) ls)]
            (cond
              (empty? def-lines) (finding! (:id r) ":runtime var" nm "has no top-level def in" (:file rt))
              (not (some #(<= a % b) def-lines))
              (finding! (:id r) ":runtime" nm "defined at" (vec def-lines) "which is outside the cited range" (:at rt))
              (> b (count ls)) (finding! (:id r) ":runtime range end" b "past end of file" (count ls))
              :else (println (format " %-34s %-40s %s:%s OK" (str (:id r)) nm (:file rt) (:at rt))))))))))

(println)
(println "=== G. BARE-SYMBOL COLLISIONS (report only; refusal is slice 2) ===")
(doseq [[f rs] (sort-by key (group-by :fold rows))]
  (when (> (count rs) 1)
    (println " fold" (pr-str f) "->" (count rs) "rows:" (vec (map :id rs)))))

(println)
(println "=== NOTES (shape, not resolution) ===")
(if (empty? @notes) (println "none") (doseq [n @notes] (println " *" n)))
(println)
(println "=== FINDINGS ===")
(if (empty? @findings)
  (println "none")
  (doseq [f @findings] (println " -" f)))
(println "count:" (count @findings))
