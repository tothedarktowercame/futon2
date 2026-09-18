#!/usr/bin/env bb
;; lean_state_probe.bb -- U35 (TN-edge-review worklist): the machine-derived
;; Lean-state report for the AIF formalism.
;;
;; Four parts, all derived by this script from the sources it names:
;;   (a) module inventory  -- files, lines, declaration counts by kind
;;   (b) type-check verdict -- `lake env lean FILE` run fresh per module, exit
;;       code and wall time recorded, with the corpus tree sha and timestamps
;;   (c) sorry/axiom census -- source tokens with file:line, cross-checked
;;       against Lean's own `declaration uses \`sorry\`` warnings
;;   (d) the joins -- glossary paragraphs to owning Lean declarations
;;       (variable-situation-accounting) and AIF equations to their declared
;;       Lean carriers (aif-equations), each resolved by name against the
;;       corpus index or recorded as "not found"
;;
;; Reports what Lean validates TODAY.  Formalism present in prose or Clojure
;; and absent from Lean is output, typed, not suppressed.
;;
;; Usage:  bb lean_state_probe.bb [--out PATH] [--no-typecheck]
;; Writes: runs/U35-lean-state/lean-state-report.edn (atomic).

(require '[babashka.process :as p]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[clojure.set :as set]
         '[clojure.pprint :as pprint]
         '[cheshire.core :as json])
(import '[java.security MessageDigest]
        '[java.time Instant])

(def here (.getCanonicalFile (io/file (or (System/getenv "WM_CONTRACT_DIR")
                                          "/home/joe/code/futon2/holes/labs/wm-contract"))))
(def mathlib (.getCanonicalFile (io/file (or (System/getenv "MATHLIB_ROOT")
                                             "/home/joe/code/mathlib4"))))
(def corpus-rel (or (System/getenv "WM_LEAN_DIR") "DarkTower/WarMachine"))
(def corpus-dir (io/file mathlib corpus-rel))
(def contract-file (io/file corpus-dir "holes-contract.json"))
(def accounting-file (io/file here "variable-situation-accounting.edn"))
(def equations-file (io/file here "aif-equations.edn"))

(def args (set *command-line-args*))
(def typecheck? (not (contains? args "--no-typecheck")))
(def out-file
  (io/file (or (second (drop-while #(not= "--out" %) *command-line-args*))
               (str (io/file here "runs/U35-lean-state/lean-state-report.edn")))))

(defn fail! [msg data]
  (binding [*out* *err*] (println "lean_state_probe: ERROR" msg (pr-str data)))
  (System/exit 1))

(defn sha256 [^String s]
  (let [d (MessageDigest/getInstance "SHA-256")]
    (.update d (.getBytes s "UTF-8"))
    (format "%064x" (BigInteger. 1 (.digest d)))))

(defn sh [dir & cmd]
  (let [r (apply p/sh {:dir (str dir) :out :string :err :string} cmd)]
    (assoc r :out (str/trim (:out r)))))

(defn git [& cmd] (:out (apply sh mathlib "git" cmd)))

;; ---------------------------------------------------------------- source scan
;; Lean comments are stripped before any counting so that a `def` inside a
;; docstring or a `sorry` inside a #guard_msgs error block is not counted as a
;; declaration or a hole.  Block comments nest in Lean; line comments do not.
(defn strip-comments
  "Replace comment characters with spaces, preserving line structure so that
   line numbers of the surviving tokens are the source's own."
  [^String src]
  (let [n (count src)
        sb (StringBuilder. src)]
    (loop [i 0, depth 0, line-comment? false]
      (if (>= i n)
        (.toString sb)
        (let [c (.charAt src i)
              c2 (when (< (inc i) n) (.charAt src (inc i)))]
          (cond
            (= c \newline)
            (recur (inc i) depth false)

            line-comment?
            (do (.setCharAt sb i \space) (recur (inc i) depth true))

            (and (zero? depth) (= c \-) (= c2 \-))
            (do (.setCharAt sb i \space) (.setCharAt sb (inc i) \space)
                (recur (+ i 2) depth true))

            (and (= c \/) (= c2 \-))
            (do (.setCharAt sb i \space) (.setCharAt sb (inc i) \space)
                (recur (+ i 2) (inc depth) false))

            (and (pos? depth) (= c \-) (= c2 \/))
            (do (.setCharAt sb i \space) (.setCharAt sb (inc i) \space)
                (recur (+ i 2) (dec depth) false))

            (pos? depth)
            (do (.setCharAt sb i \space) (recur (inc i) depth false))

            :else (recur (inc i) depth false)))))))

(def decl-kinds
  ["theorem" "lemma" "def" "abbrev" "structure" "inductive" "class" "instance"
   "example" "axiom" "opaque"])

(def decl-re
  ;; A declaration opens a line, possibly behind modifiers or an attribute set.
  (re-pattern
   (str "^(?:@\\[[^\\]]*\\]\\s*)?"
        "(?:(?:private|protected|noncomputable|partial|unsafe|scoped|local|nonrec)\\s+)*"
        "(" (str/join "|" decl-kinds) ")\\b\\s*([A-Za-z_][A-Za-z0-9_'\\.!?]*)?")))

(defn scan-module [^java.io.File f]
  (let [raw (slurp f)
        code (strip-comments raw)
        raw-lines (str/split-lines raw)
        lines (str/split-lines code)
        rel (str corpus-rel "/" (.getName f))
        decls (keep-indexed
               (fn [idx ^String line]
                 (when-let [m (re-find decl-re line)]
                   {:kind (nth m 1)
                    :name (nth m 2)
                    :path rel
                    :line (inc idx)}))
               lines)
        sorries (keep-indexed
                 (fn [idx ^String line]
                   ;; the bare token, not `.sorryCountNorm` and not `sorryAx`
                   (when (re-find #"(?<![A-Za-z0-9_.'])sorry(?![A-Za-z0-9_'])" line)
                     {:path rel :line (inc idx)
                      :text (str/trim (nth raw-lines idx ""))}))
                 lines)]
    {:module (str "DarkTower.WarMachine." (str/replace (.getName f) #"\.lean$" ""))
     :path rel
     :negative-witness?
     ;; A source-level declaration of intent, read from the raw bytes: a
     ;; module whose header carries this marker is EXPECTED NOT to elaborate.
     ;; Without it, a nonzero exit is indistinguishable from breakage, so the
     ;; report must be able to tell them apart. The prose "-- Must fail:"
     ;; convention is NOT this marker: 21 of its 23 uses document a refuted
     ;; attempt in a comment while the module still elaborates.
     (boolean (re-find #"(?m)^--\s*NEGATIVE-WITNESS:" raw))
     :lines (count raw-lines)
     :bytes (count (.getBytes raw "UTF-8"))
     :sha256 (sha256 raw)
     :declarations (into (sorted-map)
                         (map (fn [[k v]] [(keyword k) (count v)]))
                         (group-by :kind decls))
     :declaration-total (count decls)
     :decl-index (vec decls)
     :sorry-tokens (vec sorries)}))

;; ------------------------------------------------------------------ type-check
(def sorry-warning-re
  #"(?m)^(\S+\.lean):(\d+):(\d+): warning: declaration uses `sorry`")
(def diagnostic-re #"(?m)^\S+\.lean:\d+:\d+: (error|warning): ")

(defn typecheck-module [^java.io.File f]
  (let [t0 (System/currentTimeMillis)
        r (apply p/sh {:dir (str mathlib) :out :string :err :string}
                 ["lake" "env" "lean" (str corpus-rel "/" (.getName f))])
        ms (- (System/currentTimeMillis) t0)
        text (str (:out r) (:err r))
        warns (map (fn [[_ path line col]]
                     {:path path :line (parse-long line) :col (parse-long col)})
                   (re-seq sorry-warning-re text))
        diags (frequencies (map second (re-seq diagnostic-re text)))]
    {:exit (:exit r)
     :ms ms
     :sorry-warnings (vec warns)
     :diagnostics {:error (get diags "error" 0) :warning (get diags "warning" 0)}
     :output-sha256 (sha256 text)}))

;; ----------------------------------------------------------------------- main
(when-not (.isDirectory corpus-dir)
  (fail! "Lean corpus directory is missing" {:path (.getPath corpus-dir)}))

(def lean-files
  (->> (.listFiles corpus-dir)
       (filter #(str/ends-with? (.getName %) ".lean"))
       (sort-by #(.getName %))
       vec))

(when (empty? lean-files)
  (fail! "Lean corpus contains no .lean modules (refusing an empty report)"
         {:path (.getPath corpus-dir)}))

(def scans (mapv scan-module lean-files))

;; Corpus-wide declaration index: name -> first defining site.  Used by both
;; joins; a name that is not here resolves to "not found", never to a guess.
(def decl-index
  (reduce (fn [acc d]
            (if (and (:name d) (not (contains? acc (:name d))))
              (assoc acc (:name d) (select-keys d [:path :line :kind]))
              acc))
          {}
          (mapcat :decl-index scans)))

(def started-at (str (Instant/now)))
(def t-start (System/currentTimeMillis))

(def checks
  (if typecheck?
    (mapv (fn [f]
            (print (str "  " (.getName f) " ... ")) (flush)
            (let [r (typecheck-module f)]
              (println (str "exit " (:exit r) " (" (:ms r) " ms)"))
              r))
          lean-files)
    (vec (repeat (count lean-files) {:exit nil :ms nil :sorry-warnings []
                                     :diagnostics {:error 0 :warning 0}
                                     :output-sha256 nil}))))

(def wall-ms (- (System/currentTimeMillis) t-start))
(def finished-at (str (Instant/now)))

(def modules
  (mapv (fn [s c]
          (-> (dissoc s :decl-index :sorry-tokens)
              (assoc :typecheck (dissoc c :sorry-warnings)
                     :sorry-warnings (count (:sorry-warnings c)))))
        scans checks))

;; ------------------------------------------------------------- (c) the census
(def source-sorries (vec (mapcat :sorry-tokens scans)))
(def lean-sorries (vec (mapcat :sorry-warnings checks)))
(def axioms
  (vec (for [s scans, d (:decl-index s) :when (= "axiom" (:kind d))]
         (select-keys d [:path :line :name]))))

;; A source `sorry` token and a Lean warning name the same declaration when the
;; warning's line is the line of the declaration whose body holds the token.
;; Both are reported with locations; agreement is asserted on counts and lines.
(def sorry-lines-source (set (map (juxt :path :line) source-sorries)))
(def sorry-lines-lean (set (map (juxt :path :line) lean-sorries)))

;; ----------------------------------------------- (d) the contract cross-check
(def contract
  (when (.isFile contract-file)
    (try (json/parse-string (slurp contract-file) true)
         (catch Throwable t (fail! "holes-contract.json is malformed"
                                   {:cause (ex-message t)})))))
(def contract-holes
  (vec (sort (map :name (filter #(= "hole" (:kind %)) (:declarations contract))))))
;; The hole set at HEAD is derived from the source, not hard-coded: first learn
;; which private constructors mint `kind := .hole` (there are two -- `mkHole` and
;; `mkRefused` -- and a third could be added), then count their applications.
(def holes-source (strip-comments (slurp (io/file corpus-dir "Holes.lean"))))
(def hole-constructors
  (vec (sort (for [[_ nm body]
                   (re-seq #"(?s)private def (mk[A-Za-z0-9_']+)[^\n]*:\s*Declaration\s*:=(.*?)(?=\nprivate def |\ndef |\Z)"
                           holes-source)
                   :when (re-find #"kind\s*:=\s*\.hole" body)]
               nm))))
(when (empty? hole-constructors)
  (fail! "no hole-minting constructor found in Holes.lean (the registry shape changed)"
         {:path (str corpus-rel "/Holes.lean")}))
(def source-hole-entries
  (vec (sort-by :name
                (for [ctor hole-constructors
                      [_ nm] (re-seq (re-pattern (str ctor "\\s+\"([A-Za-z0-9_']+)\""))
                                     holes-source)]
                  {:name nm :constructor ctor}))))
(def source-hole-names (vec (sort (map :name source-hole-entries))))
(def sorry-warned-decls
  ;; the declaration whose header is at (or immediately above) the warned line
  (vec (sort (distinct
              (for [w lean-sorries
                    :let [hit (->> (mapcat :decl-index scans)
                                   (filter #(and (= (:path %) (:path w))
                                                 (= (:line %) (:line w))))
                                   first)]
                    :when hit]
                (:name hit))))))

;; --------------------------------------------------------- (d) the two joins
(defn resolve-name [nm]
  (if-let [hit (get decl-index nm)]
    (assoc hit :found true)
    {:found false :resolution :not-found}))

(def accounting
  (try (edn/read-string (slurp accounting-file))
       (catch Throwable t (fail! "variable-situation-accounting.edn unreadable"
                                 {:cause (ex-message t)}))))
(defn glossary-owned? [row]
  (str/starts-with? (str (:owner row)) "sec-glossary.tex"))

(def glossary-join
  (vec (for [row (:rows accounting) :when (glossary-owned? row)]
         (let [r (resolve-name (:name row))]
           {:name (:name row)
            :owner (:owner row)
            :row-source (:row-source row)
            :content-status (:content-status row)
            :lean (if (:found r)
                    {:found true :path (:path r) :line (:line r) :kind (:kind r)}
                    {:found false :resolution :not-found})}))))

(def equations
  (try (edn/read-string (slurp equations-file))
       (catch Throwable t (fail! "aif-equations.edn unreadable" {:cause (ex-message t)}))))

(def equation-join
  (vec (for [e (:equations equations)]
         (let [declared (:lean e)
               ;; the registry sometimes annotates the name ("X (carrier only, ...)")
               nm (when declared (first (str/split (str/trim declared) #"\s")))
               r (when nm (resolve-name nm))]
           (cond-> {:equation (:id e)
                    :defines (:defines e)
                    :node (:node e)
                    :class (:class e)
                    :declared-lean declared
                    :declared-lean-status (:lean-status e)}
             nm (assoc :resolved-name nm
                       :lean (if (:found r)
                               {:found true :path (:path r) :line (:line r) :kind (:kind r)}
                               {:found false :resolution :not-found}))
             (nil? nm) (assoc :lean {:found false :resolution :no-lean-carrier-declared}))))))

;; ---------------------------------------------------------------------- report
(def other-darktower
  (count (filter #(str/ends-with? (.getName %) ".lean") (.listFiles (io/file mathlib "DarkTower")))))

(def report
  {:schema :wm/lean-state-report-v1
   :item :U35
   :produced-by "futon2/holes/labs/wm-contract/lean_state_probe.bb"
   :as-of finished-at
   :corpus
   {:root (str "mathlib4/" corpus-rel)
    :git-sha (git "rev-parse" "HEAD")
    :git-describe (git "log" "-1" "--format=%h %s")
    :worktree-clean? (empty? (git "status" "--porcelain" corpus-rel))
    :toolchain (str/trim (slurp (io/file mathlib "lean-toolchain")))
    :lake-version (:out (sh mathlib "lake" "--version"))
    :modules (count lean-files)
    :source-lines (reduce + (map :lines scans))
    :declaration-total (reduce + (map :declaration-total scans))
    :declarations-by-kind (into (sorted-map)
                                (apply merge-with +
                                       (map #(:declarations %) scans)))
    :out-of-scope {:darktower-sibling-modules other-darktower
                   :note "DarkTower/*.lean outside WarMachine/ (APM and other lines) are not in this report's scope"}}
   :typecheck
   {:method "lake env lean <module> run per module, fresh, in the corpus checkout"
    :ran? typecheck?
    :started-at started-at
    :finished-at finished-at
    :wall-ms wall-ms
    :modules-checked (count (filter #(some? (:exit %)) checks))
    :exit-zero (count (filter #(= 0 (:exit %)) checks))
    :exit-nonzero (count (filter #(and (some? (:exit %)) (not= 0 (:exit %))) checks))
    :exit-nonzero-expected
    (count (for [[s c] (map vector scans checks)
                 :when (and (some? (:exit c)) (not= 0 (:exit c))
                            (:negative-witness? s))]
             c))
    :exit-nonzero-unexpected
    (count (for [[s c] (map vector scans checks)
                 :when (and (some? (:exit c)) (not= 0 (:exit c))
                            (not (:negative-witness? s)))]
             c))
    :nonzero-modules (vec (for [[s c] (map vector scans checks)
                                :when (and (some? (:exit c)) (not= 0 (:exit c)))]
                            {:module (:module s) :exit (:exit c)
                             :expected? (:negative-witness? s)}))
    :error-diagnostics (reduce + (map #(get-in % [:diagnostics :error] 0) checks))
    :verdict (cond (not typecheck?) :not-run
                   (and (every? #(= 0 (:exit %)) checks)
                        (zero? (reduce + (map #(get-in % [:diagnostics :error] 0) checks))))
                   :all-modules-elaborate-exit-zero
                   :else :some-modules-fail)}
   :sorry-census
   {:source-token-count (count source-sorries)
    :lean-reported-count (count lean-sorries)
    :agreement (cond (not typecheck?) :typecheck-not-run
                     (= sorry-lines-source sorry-lines-lean) :exact-by-location
                     :else :divergent)
    :source-only (vec (sort (map (fn [[p l]] (str p ":" l))
                                 (set/difference sorry-lines-source sorry-lines-lean))))
    :lean-only (vec (sort (map (fn [[p l]] (str p ":" l))
                               (set/difference sorry-lines-lean sorry-lines-source))))
    :locations (vec (sort-by (juxt :path :line) source-sorries))}
   :axiom-census
   {:count (count axioms)
    :locations axioms
    :method "`axiom` declarations in the comment-stripped corpus sources"}
   :contract-cross-check
   {:contract-path (str "mathlib4/" corpus-rel "/holes-contract.json")
    :contract-git-sha (get-in contract [:source :git-sha])
    :corpus-git-sha (git "rev-parse" "HEAD")
    ;; C175 (holes/labs/wm-contract/C175-contract-authority-current.md): the pin's
    ;; invariant is the Holes.lean SOURCE, not mathlib HEAD. Repository-HEAD lag is
    ;; allowed and reported. Comparing against HEAD made :pin-state read :stale for
    ;; every unrelated mathlib commit -- and unavoidably for the regeneration commit
    ;; itself, which lands holes-contract.json and so always moves HEAD past the
    ;; authority the contract just recorded. checks/contract_authority_current.clj
    ;; already uses the source-scoped comparison; this now agrees with it.
    :holes-last-commit-sha (git "log" "-1" "--format=%H" "--"
                                (str corpus-rel "/Holes.lean"))
    :pin-state (if (= (get-in contract [:source :git-sha])
                      (git "log" "-1" "--format=%H" "--" (str corpus-rel "/Holes.lean")))
                 :current :stale)
    :contract-hole-count (count contract-holes)
    :contract-holes contract-holes
    :hole-constructors hole-constructors
    :source-hole-count (count source-hole-names)
    :source-holes source-hole-entries
    :in-source-not-in-contract (vec (sort (remove (set contract-holes) source-hole-names)))
    :in-contract-not-in-source (vec (sort (remove (set source-hole-names) contract-holes)))
    :sorry-warned-declarations (if typecheck? sorry-warned-decls :typecheck-not-run)
    :holes-without-a-sorry (if typecheck?
                             (vec (sort (remove (set sorry-warned-decls) source-hole-names)))
                             :typecheck-not-run)
    :note "The contract JSON is a committed emission pinned to one Holes.lean revision; :pin-state compares it to that source file's last commit (C175), while :corpus-git-sha reports mathlib HEAD for context. A hole without a `sorry` is a declaration stated as a Prop whose body is not itself a hole."}
   :glossary-lean-join
   {:source "futon2/holes/labs/wm-contract/variable-situation-accounting.edn"
    :source-as-of (:as-of accounting)
    :glossary-owned-rows (count glossary-join)
    :with-lean-declaration (count (filter #(get-in % [:lean :found]) glossary-join))
    :without-lean-declaration (count (remove #(get-in % [:lean :found]) glossary-join))
    :by-row-source (frequencies (map :row-source glossary-join))
    :rows glossary-join}
   :equation-lean-join
   {:source "futon2/holes/labs/wm-contract/aif-equations.edn"
    :source-as-of (:as-of equations)
    :equations (count equation-join)
    :with-declared-carrier (count (filter :resolved-name equation-join))
    :carrier-resolves (count (filter #(get-in % [:lean :found]) equation-join))
    :carrier-declared-but-not-found
    (vec (for [e equation-join
               :when (and (:resolved-name e) (not (get-in e [:lean :found])))]
           (:equation e)))
    :no-carrier-declared (vec (for [e equation-join :when (nil? (:resolved-name e))]
                                (:equation e)))
    :rows equation-join}
   :modules modules})

(io/make-parents out-file)
(let [tmp (io/file (str (.getPath out-file) ".tmp"))]
  (spit tmp (with-out-str (pprint/pprint report)))
  (.renameTo tmp out-file))

(println)
(println (format "lean_state_probe: %d modules, %d declarations, %d source lines"
                 (count lean-files)
                 (get-in report [:corpus :declaration-total])
                 (get-in report [:corpus :source-lines])))
(println (format "lean_state_probe: type-check %s -- %d/%d exit 0, %d error diagnostics, %d ms"
                 (name (get-in report [:typecheck :verdict]))
                 (get-in report [:typecheck :exit-zero])
                 (get-in report [:typecheck :modules-checked])
                 (get-in report [:typecheck :error-diagnostics])
                 wall-ms))
(println (format "lean_state_probe: sorry %d source / %d Lean-reported (%s); axioms %d"
                 (count source-sorries) (count lean-sorries)
                 (name (get-in report [:sorry-census :agreement]))
                 (count axioms)))
(println (format "lean_state_probe: glossary join %d/%d resolve; equation carriers %d/%d declared, %d resolve"
                 (get-in report [:glossary-lean-join :with-lean-declaration])
                 (get-in report [:glossary-lean-join :glossary-owned-rows])
                 (get-in report [:equation-lean-join :with-declared-carrier])
                 (get-in report [:equation-lean-join :equations])
                 (get-in report [:equation-lean-join :carrier-resolves])))
(println (str "lean_state_probe: wrote " (.getPath out-file)))
