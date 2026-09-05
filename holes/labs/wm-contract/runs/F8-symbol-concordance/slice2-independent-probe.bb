#!/usr/bin/env bb
;; F8 leg 2 slice 2 -- INDEPENDENT PROBE, run by the REVIEWING seat BEFORE the
;; slice-2 dispatch went out. It computes, from source, the answers the leg-2
;; checker will have to produce, so the delivery is compared against a number
;; this seat derived rather than against the delivery's own report.
;;
;; It is NOT the checker. It refuses nothing and exits 0 whatever it finds; the
;; checker is symbol_concordance_check.bb and is slice 2's deliverable.
;;
;; What it derives:
;;   1. the case-fold groups over :bare, RECOMPUTED (never reading :fold), and
;;      which of them have more than one member;
;;   2. whether each declared :collisions entry names exactly the recomputed
;;      group for its fold, and whether every :members id resolves to exactly
;;      one row;
;;   3. every occurrence of \Pi in p4ng/sec-glossary.tex, scanned over the WHOLE
;;      file rather than over dollar-delimited spans -- the gap slice 1 recorded
;;      in the registry's :limits (Pi_feasible at :47 sits in a display whose
;;      delimiters are on separate lines);
;;   4. the file:line pointers symbol-concordance.edn carries in string values,
;;      split by extension, so the coverage pointer_check.bb would gain by
;;      scanning this file is a measured number and not an estimate.
(require '[clojure.edn :as edn] '[clojure.string :as str])

(def home (System/getenv "HOME"))
(def conc-path (str home "/code/futon2/holes/labs/wm-contract/symbol-concordance.edn"))
(def glossary  (str home "/code/p4ng/sec-glossary.tex"))
(def conc (edn/read-string {:default (fn [_ v] v)} (slurp conc-path)))
(def rows (:symbols conc))

(println "=== 1. CASE-FOLD GROUPS, RECOMPUTED FROM :bare ===")
(println "file:" conc-path)
(printf "rows %d%n" (count rows))
(def recomputed (group-by #(str/lower-case (:bare %)) rows))
(def multi (into (sorted-map) (filter (fn [[_ v]] (> (count v) 1)) recomputed)))
(doseq [[f v] multi]
  (printf "  fold %-10s %d members %s%n" f (count v) (pr-str (mapv :id v))))
(printf "folds with more than one member: %d%n" (count multi))
(def fold-field-disagrees
  (vec (for [r rows :when (not= (:fold r) (str/lower-case (:bare r)))]
         [(:id r) (:bare r) (:fold r)])))
(printf "rows whose stored :fold differs from lower-case of :bare: %d %s%n"
        (count fold-field-disagrees) (pr-str fold-field-disagrees))

(println)
(println "=== 2. DECLARED :collisions AGAINST THE RECOMPUTED GROUPS ===")
(def by-id (group-by :id rows))
(doseq [c (:collisions conc)]
  (let [f (:fold c)
        declared (set (:members c))
        actual   (set (map :id (get recomputed f)))
        unresolved (vec (remove #(= 1 (count (get by-id %))) declared))]
    (printf "  fold %-10s declared %d, recomputed %d, agree %s%s%n"
            f (count declared) (count actual) (= declared actual)
            (if (seq unresolved) (str "; members not resolving to exactly one row: " (pr-str unresolved)) ""))))
(def undeclared (vec (remove (set (map :fold (:collisions conc))) (keys multi))))
(printf "multi-member folds with NO :collisions entry: %d %s%n" (count undeclared) (pr-str undeclared))
(def overdeclared (vec (remove (set (keys multi)) (map :fold (:collisions conc)))))
(printf ":collisions entries for folds that are not multi-member: %d %s%n"
        (count overdeclared) (pr-str overdeclared))

(println)
(println "=== 3. \\Pi IN THE GLOSSARY, WHOLE-FILE SCAN ===")
(println "file:" glossary)
(doseq [[i l] (map-indexed vector (str/split-lines (slurp glossary)))
        :when (re-find #"\\Pi" l)]
  (printf "  %3d %s%n" (inc i)
          (str/join " | " (re-seq #"\\Pi(?:_\{[^}]*\}|_[A-Za-z0-9]+)?(?:\([^)]*\))?" l))))

(println)
(println "=== 4. file:line POINTERS IN symbol-concordance.edn STRING VALUES ===")
(def texts (filter string? (tree-seq coll? seq conc)))
(def ptr-re #"([A-Za-z0-9_.\-]+\.(?:clj|lean|edn|tex|md|bb)):(\d+)(?:-(\d+))?")
(def ptrs (vec (for [t texts m (re-seq ptr-re t)] (first m))))
(printf "pointers in string values: %d%n" (count ptrs))
(doseq [[ext n] (sort-by (comp - val) (frequencies (map #(last (str/split (first (str/split % #":")) #"\.")) ptrs)))]
  (printf "  .%-5s %d%n" ext n))
(printf "distinct: %d%n" (count (distinct ptrs)))
(println "  " (pr-str (vec (sort (distinct ptrs)))))

(println)
(println "=== 5. COLUMN POINTERS (:file + :at PAIRS), WHICH ARE NOT file:line STRINGS ===")
(def col-ptrs
  (vec (for [r rows k [:glossary :lean :runtime]
             :let [c (get r k)] :when (and (map? c) (:file c) (:at c))]
         (str (:file c) ":" (:at c)))))
(printf "column pointers: %d%n" (count col-ptrs))
(doseq [[ext n] (sort-by (comp - val) (frequencies (map #(last (str/split (first (str/split % #":")) #"\.")) col-ptrs)))]
  (printf "  .%-5s %d%n" ext n))
(println "NOTE: pointer_check.bb's regex only sees file:line inside ONE string, so")
(println "      adding this file to its extra-paths gates section 4's pointers and")
(println "      NOT section 5's. The two numbers above are that gap, measured.")

(println)
(println "=== 6. DO THEY RESOLVE? (this seat's own resolution, roots fixed here) ===")
;; Deliberately NOT pointer_check.bb's roots list: if the checker under review
;; and the probe shared a root list, a root defect would cancel out of the
;; comparison. These are the four repositories the registry cites, walked.
(def bases [(str home "/code/futon2/") (str home "/code/p4ng/") (str home "/code/mathlib4/")])
;; Resolution is by SUFFIX on the relative path the registry actually wrote, not
;; by bare filename. That distinction is itself a finding: futon2 carries TWO
;; efe.clj (src/ants/aif/efe.clj, 21 lines, and src/futon2/aif/efe.clj, 981), so
;; bare-name resolution -- which is what pointer_check.bb does -- picks by roots
;; order and can silently answer about the wrong file. The concordance writes the
;; full relative path, which is unambiguous; the gate throws that away.
(def all-files
  (vec (for [b bases f (file-seq (java.io.File. b))
             :when (and (.isFile f) (not (str/includes? (.getPath f) "/.git/"))
                        (not (str/includes? (.getPath f) "/target/")))]
         (.getPath f))))
(defn find-file [rel]
  (let [hits (filterv #(str/ends-with? % (str "/" rel)) all-files)]
    (cond (= 1 (count hits)) (first hits)
          (seq hits) (first (sort-by count hits))
          :else (let [base (last (str/split rel #"/"))
                      h2 (filterv #(str/ends-with? % (str "/" base)) all-files)]
                  (when (= 1 (count h2)) (first h2))))))
(def cache (atom {}))
(defn resolve1 [nm] (if (contains? @cache nm) (@cache nm)
                        (let [v (find-file nm)] (swap! cache assoc nm v) v)))
(defn check [ptr]
  (let [[nm rng] [(first (str/split ptr #":")) (second (str/split ptr #":"))]
        path (resolve1 nm)
        n (when path (count (str/split-lines (slurp path))))
        [lo hi] (if (str/includes? rng "-")
                  (mapv parse-long (str/split rng #"-"))
                  [(parse-long rng) (parse-long rng)])]
    (cond (nil? path) [ptr "file not found"]
          (nil? lo) [ptr "unparsed range"]
          (> lo hi) [ptr "inverted range"]
          (> hi n) [ptr (str "end beyond file (" n " lines)")]
          :else nil)))
(def all-ptrs (vec (distinct (concat ptrs col-ptrs))))
(def bad (vec (keep check all-ptrs)))
(printf "checked %d distinct pointers (%d string + %d column), unresolved %d%n"
        (count all-ptrs) (count (distinct ptrs)) (count (distinct col-ptrs)) (count bad))
(doseq [[p why] bad] (printf "  UNRESOLVED %s (%s)%n" p why))
