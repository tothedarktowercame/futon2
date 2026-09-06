(ns f12-zaif-transcription
  "`:F12` slice 2. DERIVES the CascadeDiff transcription of one recorded LIBRARY
   cascade and CHECKS the Lean fixture against it.

   Slice 1 (C539) found that O1's three-way union is witnessed in Lean only on
   the C59 fixture, where `admittedBy` is empty and `nodes = selected`, so the
   third origin contributes nothing and the two readings of O3 are the same
   proposition.  This file transcribes the zaif run -- 20 nodes, 9 of them
   admitted by a policy-grain rule, over the whole 1239-pattern library -- into
   the integer encoding `wmZaifCascadeDiffFixture` uses, and then re-reads that
   fixture out of `Holes.lean` and fails if it disagrees.  Without the second
   half the Lean block would be hand-entered numbers with a citation attached.

   THE BASIS IS A COMMIT, NOT THE WORKING TREE.  `library/` has moved since the
   record was written: at HEAD the authored `@why` relation carries 507 edges
   against the record's 92, and the closure of the cascade's nodes grows from 27
   vertices to 41.  So the relation is read from `git archive` of
   `basis-sha` rather than from the checkout, and the four counts the record
   states in `:as-of` are re-derived and compared.  The record's `:read-digest`
   is NOT reproducible and is not treated as if it were: it hashes each entry's
   `(:file e)` (`futon3:checks/construct_cascade.clj:76`), so it moves with the
   root path, and `library/math-formalization` takes files from a live scribe
   with no baseline step (`construct_cascade.clj:67-73`, and the record's own
   `:untracked-flexiargs-are-possible? true`).  What is checked instead is the
   four counts plus the two `fast-forward` results, which the record corroborates
   independently through its own controls.

   THE ONE COUNT THAT DOES NOT MATCH, and what is done about it.  The basis
   commit carries 1238 patterns against the record's 1239.  The delta is the
   untracked-scribe condition above, and it is checked rather than excused: for
   each file `git` reports untracked under `futon3:library/`, the derivation is
   re-run with that file added, and the transcription FAILS unless the authored
   relation -- edge counts, the transcribed edge list, the node closure and both
   `fast-forward` results -- is unchanged by it.  Either untracked file restores
   the count to 1239, so which one the record read is undetermined; neither
   authors an edge, so it cannot move what this transcription rests on.

   Determinism: every collection is sorted before it is written; no wall clock,
   no timestamp.  Two runs over an unchanged tree are byte-identical.

   Run from the `futon3` checkout:

     clojure -Sdeps '{:paths [\"checks\" \".\" \"../futon2/holes/labs/wm-contract\"]
                      :deps {babashka/fs {:mvn/version \"0.5.25\"}}}' \\
       -M -m f12-zaif-transcription

   Env overrides, so a NEGATIVE CONTROL can point it at a planted copy and see
   the verdict move: F12_RECORD, F12_HOLES, F12_LIBRARY_ROOT, F12_BASIS, F12_OUT."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :as pprint]
            [clojure.set :as set]
            [clojure.string :as str]
            [construct-cascade :as cc]
            [find-organise :as fo]))

(def home (System/getProperty "user.home"))

(def basis-sha
  "The last futon3 commit that touched `checks/zaif-cascade.edn`
   (`LA5: review zaif cascade evidence`).  The three commits of that row all
   read the same 92 authored `@why` edges."
  (or (System/getenv "F12_BASIS") "1b8b1d1"))

(def record-path
  (or (System/getenv "F12_RECORD") (str home "/code/futon3/checks/zaif-cascade.edn")))

(def holes-path
  (or (System/getenv "F12_HOLES")
      (str home "/code/mathlib4/DarkTower/WarMachine/Holes.lean")))

(def out-path
  (or (System/getenv "F12_OUT")
      (str home "/code/futon2/holes/labs/wm-contract/runs/F12-organise/01-zaif-transcription.edn")))

(def run-id
  "The temperament whose run is transcribed.  `:widen-to-a-budget` is the larger
   of the record's two: 20 members against 17, 9 admitted against 6."
  :widen-to-a-budget)

;; ---------------------------------------------------------------------------
;; 1. the library at the basis commit
;; ---------------------------------------------------------------------------

(defn materialise-library
  "`library/` at `basis-sha`, extracted under a temp directory, so the relation
   read is the one the record was built over and not the one at HEAD."
  []
  (or (System/getenv "F12_LIBRARY_ROOT")
      (let [dir (str (System/getProperty "java.io.tmpdir") "/f12-basis-" basis-sha)
            root (str dir "/library")]
        (when-not (.exists (io/file root))
          (.mkdirs (io/file dir))
          (let [{:keys [exit err]}
                (shell/sh "bash" "-c"
                          (format "git -C %s/code/futon3 archive %s library | tar -x -C %s"
                                  home basis-sha dir))]
            (when-not (zero? exit)
              (throw (ex-info "f12: git archive of the basis library failed"
                              {:sha basis-sha :err err})))))
        root)))

(defn untracked-library-files
  "Files `git` reports untracked under `futon3:library/`.  These are the live
   scribe's (`construct_cascade.clj:67-73`); the record's `:as-of` was taken with
   at least one of them present, which is why its pattern count is one above the
   basis commit's."
  []
  (let [{:keys [exit out]}
        (shell/sh "bash" "-c"
                  (format "git -C %s/code/futon3 ls-files --others --exclude-standard library"
                          home))]
    (if (zero? exit)
      (vec (sort (remove str/blank? (str/split-lines out))))
      [])))

(defn with-untracked-file
  "A copy of the basis library with one untracked file added, so the derivation
   can be re-run against it.  The staging directory is named for the BASIS as
   well as the file, and is rebuilt every run: keyed on the filename alone it
   was reused across bases, and a probe of one basis then reported the previous
   basis's relation -- which reads exactly like the untracked file having moved
   the relation."
  [basis-root relative]
  (let [dir (str (System/getProperty "java.io.tmpdir") "/f12-plus-" basis-sha "-"
                 (str/replace relative #"[^A-Za-z0-9]" "-"))
        root (str dir "/library")
        {:keys [exit err]}
        (shell/sh "bash" "-c"
                  (format "rm -rf %s && mkdir -p %s && cp -a %s %s && cp %s/code/futon3/%s %s/%s"
                          dir dir basis-root root home relative root
                          (str/replace relative #"^library/" "")))]
    (when-not (zero? exit)
      (throw (ex-info "f12: could not stage the untracked file" {:file relative :err err})))
    root))

;; ---------------------------------------------------------------------------
;; 2. the transcription
;; ---------------------------------------------------------------------------

(defn transcribe
  "The record's run, the basis relation, and the integer encoding the Lean
   fixture uses: selected first, then admitted, then the bridges."
  [record library-root]
  (let [sections (cc/library-sections library-root)
        why (fo/read-repository library-root sections {:kinds #{:why}})
        wh (fo/read-repository library-root sections {:kinds #{:why :how}})
        stands-on (:stands-on why)
        cascade (get-in record [:runs run-id :cascade])
        nodes (set (:members cascade))
        selected (set (get-in record [:find :selected]))
        admitted (into #{} (for [[k v] (:provenance cascade) :when (vector? v)] k))
        closure (loop [seen nodes frontier nodes]
                  (if (empty? frontier)
                    seen
                    (let [nxt (set/difference
                               (into #{} (mapcat #(get stands-on % #{})) frontier) seen)]
                      (recur (into seen nxt) nxt))))
        bridges (set/difference closure nodes)
        order (vec (concat (sort selected) (sort admitted) (sort bridges)))
        idx (into {} (map-indexed (fn [i k] [k i]) order))
        authored (vec (sort (for [u (sort closure)
                                  v (sort (get stands-on u #{}))]
                              [(idx u) (idx v)])))
        ff-nodes (vec (sort (map (fn [[u v]] [(idx u) (idx v)])
                                 (fo/fast-forward nodes stands-on))))
        ff-selected (vec (sort (map (fn [[u v]] [(idx u) (idx v)])
                                    (fo/fast-forward selected stands-on))))]
    (sorted-map
     :basis (sorted-map
             :sha basis-sha
             :library-root library-root
             :record record-path
             :run run-id)
     :as-of-recomputed (sorted-map
                        :patterns (count (:patterns why))
                        :sections (count sections)
                        :authored-why-edges (count (:edges why))
                        :authored-why-how-edges (count (:edges wh)))
     :as-of-recorded (into (sorted-map)
                           (select-keys (:as-of record)
                                        [:patterns :sections :authored-why-edges
                                         :authored-why-how-edges]))
     :read-digest (sorted-map
                   :recorded (get-in record [:as-of :read-digest])
                   :reproducible? false
                   :reason (str "read-digest hashes each entry's :file "
                                "(futon3:checks/construct_cascade.clj:76), so it moves "
                                "with the root path; and library/math-formalization "
                                "takes untracked files from a live scribe "
                                "(construct_cascade.clj:67-73). Not used as a check."))
     :counts (sorted-map
              :selected (count selected)
              :admitted (count admitted)
              :added-by-organise 0
              :nodes (count nodes)
              :bridges (count bridges)
              :closure (count closure)
              :authored-edges (count authored))
     :index (vec (map-indexed (fn [i k] [i k]) order))
     :encoding (sorted-map
                :selected-below (count selected)
                :admitted-from (count selected)
                :nodes-below (count nodes))
     :authored-edges authored
     :fast-forward (sorted-map
                    :over-nodes ff-nodes
                    :over-selected ff-selected)
     :o4 (sorted-map
          :recorded (:o4 record)
          :precedence-before []
          :precedence-after []
          :note (str "cascade-of sets both to [] for this run and carries no score "
                     "(futon3:checks/construct_cascade.clj:402, fields at 420-421); no O4 is stated of "
                     "the Lean fixture.")))))

(defn relation-fingerprint
  "Everything in a transcription that the authored relation decides.  Two
   derivations that agree here transcribe the same cascade, whatever else about
   the library differs between them."
  [t]
  (sorted-map
   :authored-why-edges (get-in t [:as-of-recomputed :authored-why-edges])
   :authored-why-how-edges (get-in t [:as-of-recomputed :authored-why-how-edges])
   :closure (get-in t [:counts :closure])
   :bridges (get-in t [:counts :bridges])
   :index (:index t)
   :authored-edges (:authored-edges t)
   :fast-forward (:fast-forward t)))

;; ---------------------------------------------------------------------------
;; 3. the check against the Lean block
;; ---------------------------------------------------------------------------

(def block-begin "-- F12 SLICE 2 TRANSCRIPTION BEGIN")
(def block-end "-- F12 SLICE 2 TRANSCRIPTION END")

(defn lean-block
  "The transcribed region of `Holes.lean`, or a throw.  Bounded by sentinels so
   the check reads what this slice wrote and not a similarly named declaration
   elsewhere in an 8000-line file."
  [path]
  (let [ls (str/split-lines (slurp path))
        b (some (fn [[i l]] (when (str/starts-with? l block-begin) i))
                (map-indexed vector ls))
        e (some (fn [[i l]] (when (str/starts-with? l block-end) i))
                (map-indexed vector ls))]
    (when-not (and b e (< b e))
      (throw (ex-info "f12: the Lean transcription block is missing or inverted"
                      {:path path :begin b :end e})))
    {:text (str/join "\n" (subvec (vec ls) b (inc e)))
     :begin-line (inc b)
     :end-line (inc e)}))

(defn- number-after [text re what]
  (if-let [m (re-find re text)]
    (parse-long (second m))
    (throw (ex-info "f12: the Lean block does not state" {:what what}))))

(defn lean-facts
  "What the Lean block claims, read out of its source rather than assumed."
  [text]
  {:selected-below (number-after text #"zaifSelected : Set Nat := \{n \| n < (\d+)\}"
                                 :zaifSelected)
   :admitted-from (number-after text #"zaifAdmitted : Set Nat := \{n \| (\d+) ≤ n"
                                :zaifAdmitted-lower)
   :admitted-below (number-after text #"zaifAdmitted : Set Nat := \{n \| \d+ ≤ n ∧ n < (\d+)\}"
                                 :zaifAdmitted-upper)
   :nodes-below (number-after text #"zaifNodes : Set Nat := \{n \| n < (\d+)\}" :zaifNodes)
   :authored-edges (let [arms (second (re-find #"(?s)zaifAuthored : Nat → Nat → Prop\n(.*?)=> True"
                                               text))]
                     (when-not arms
                       (throw (ex-info "f12: the Lean block states no zaifAuthored arms" {})))
                     (vec (sort (map (fn [[_ a b]] [(parse-long a) (parse-long b)])
                                     (re-seq #"\|\s*(\d+),\s*(\d+)" arms)))))
   :organised-over-nodes? (some? (re-find #"organisedEdges := fastForward zaifNodes zaifAuthored"
                                          text))})

(defn check
  "Every disagreement between the derived transcription, the record and the Lean
   block, as a list.  Empty is the only pass."
  [t facts untracked]
  (let [enc (:encoding t)
        counts (:counts t)
        recomputed (:as-of-recomputed t)
        recorded (:as-of-recorded t)
        edge-keys [:sections :authored-why-edges :authored-why-how-edges]
        moved (remove (fn [{:keys [fingerprint-unchanged?]}] fingerprint-unchanged?) untracked)
        reachable-patterns (into #{(:patterns recomputed)} (map :patterns) untracked)]
    (cond-> []
      (not= (:selected-below facts) (:selected-below enc))
      (conj [:selected-bound (:selected-below facts) (:selected-below enc)])

      (not= (:admitted-from facts) (:admitted-from enc))
      (conj [:admitted-lower-bound (:admitted-from facts) (:admitted-from enc)])

      (not= (:admitted-below facts) (:nodes-below enc))
      (conj [:admitted-upper-bound (:admitted-below facts) (:nodes-below enc)])

      (not= (:nodes-below facts) (:nodes-below enc))
      (conj [:nodes-bound (:nodes-below facts) (:nodes-below enc)])

      (not= (:authored-edges facts) (:authored-edges t))
      (conj [:authored-edges
             (vec (sort (set/difference (set (:authored-edges facts))
                                        (set (:authored-edges t)))))
             (vec (sort (set/difference (set (:authored-edges t))
                                        (set (:authored-edges facts)))))])

      (not (:organised-over-nodes? facts))
      (conj [:organised-edges-not-over-nodes true false])

      (not= (select-keys recomputed edge-keys) (select-keys recorded edge-keys))
      (conj [:as-of (select-keys recomputed edge-keys) (select-keys recorded edge-keys)])

      ;; the pattern count may sit one below the record's, but only if an
      ;; untracked scribe file accounts for it -- see the docstring.
      (not (contains? reachable-patterns (:patterns recorded)))
      (conj [:patterns-unaccounted reachable-patterns (:patterns recorded)])

      ;; and only if no untracked file moves the relation this rests on.
      (seq moved)
      (conj [:untracked-file-moves-the-relation (vec (map :file moved)) []])

      (not= (:nodes counts) (+ (:selected counts) (:admitted counts)))
      (conj [:o1-union (:nodes counts) (+ (:selected counts) (:admitted counts))]))))

(defn -main [& _]
  (let [record (edn/read-string (slurp record-path))
        root (materialise-library)
        t (transcribe record root)
        fingerprint (relation-fingerprint t)
        untracked (vec (for [f (untracked-library-files)
                             :let [u (transcribe record (with-untracked-file root f))]]
                         (sorted-map
                          :file f
                          :patterns (get-in u [:as-of-recomputed :patterns])
                          :sections (get-in u [:as-of-recomputed :sections])
                          :fingerprint-unchanged? (= fingerprint (relation-fingerprint u)))))
        {:keys [text begin-line end-line]} (lean-block holes-path)
        facts (lean-facts text)
        problems (check t facts untracked)
        report (assoc t
                      :untracked-probe untracked
                      :lean (sorted-map
                             :path holes-path
                             :block-lines [begin-line end-line]
                             :facts (into (sorted-map) facts))
                      :problems (vec problems))]
    (io/make-parents out-path)
    (spit out-path (with-out-str (pprint/pprint report)))
    (println (format "f12 transcription: %s run %s, %d nodes = %d selected + %d admitted, %d authored edges over %d vertices"
                     basis-sha run-id (get-in t [:counts :nodes])
                     (get-in t [:counts :selected]) (get-in t [:counts :admitted])
                     (get-in t [:counts :authored-edges]) (get-in t [:counts :closure])))
    (println (format "  fast-forward over nodes %s, over selected %s"
                     (pr-str (get-in t [:fast-forward :over-nodes]))
                     (pr-str (get-in t [:fast-forward :over-selected]))))
    (println (format "  Lean block %s:%d-%d" holes-path begin-line end-line))
    (doseq [u untracked]
      (println (format "  untracked %s: patterns %d, relation unchanged? %s"
                       (:file u) (:patterns u) (:fingerprint-unchanged? u))))
    (println "  wrote" out-path)
    (if (seq problems)
      (do (println "f12 transcription: FAIL")
          (doseq [p problems] (println "   " (pr-str p)))
          (System/exit 1))
      (do (println "f12 transcription: PASS exit-convention=0-pass/1-fail")
          (System/exit 0)))))
