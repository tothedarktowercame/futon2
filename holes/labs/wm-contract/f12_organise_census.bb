#!/usr/bin/env bb
;; f12_organise_census.bb -- :F12 slice 1. RECOMPUTES the census C539 reports,
;; so the account's numbers are derived here rather than read off by hand.
;;
;; It answers one question: for `organise` (P-validated-R5 s3e), what is
;; witnessed WHERE today -- in Lean, in Clojure, and on which recorded cascade.
;; It writes runs/F12-organise/00-census.edn and exits 0. It builds nothing,
;; edits no registry, elaborates no Lean and takes no tick.
;;
;; Determinism: every collection is sorted before it is written; no timestamp,
;; no digest of a file this repo does not own, no wall clock. Two runs of this
;; script over an unchanged tree are byte-identical.

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pprint]
         '[clojure.set :as set]
         '[clojure.string :as str])

(def home (System/getProperty "user.home"))
;; The four inputs are env-overridable so a NEGATIVE CONTROL can point the
;; census at a planted copy and see the roll-up move. Without this the script
;; would be unfalsifiable: it would report the same summary whether or not it
;; had actually read the files it names. Defaults are the real paths.
(def holes (or (System/getenv "F12_HOLES")
               (str home "/code/mathlib4/DarkTower/WarMachine/Holes.lean")))
(def find-organise (or (System/getenv "F12_FIND_ORGANISE")
                       (str home "/code/futon3/checks/find_organise.clj")))
(def code-root (or (System/getenv "F12_CODE_ROOT") (str home "/code")))

(defn lines-of [path]
  (if (.exists (io/file path))
    (str/split-lines (slurp path))
    (throw (ex-info "census: file not found" {:path path}))))

(defn line-of
  "1-indexed line number of the first line matching `re`, or :not-found."
  [ls re]
  (or (some (fn [[i l]] (when (re-find re l) (inc i)))
            (map-indexed vector ls))
      :not-found))

(defn count-matching [ls re]
  (count (filter #(re-find re %) ls)))

;; ---------------------------------------------------------------------------
;; 1. Lean: where organise's sorry is, and what the four O-laws are stated OF
;; ---------------------------------------------------------------------------

(def lean-lines (lines-of holes))

(def lean-organise
  (let [n (line-of lean-lines #"^def organise \{Policy P : Type\*\}")]
    {:decl-line n
     :is-sorry? (and (number? n)
                     (str/ends-with? (str/trim (nth lean-lines (dec n))) ":= sorry"))
     :type (when (number? n)
             (-> (nth lean-lines (dec n))
                 (str/replace #"^def organise \{Policy P : Type\*\} : " "")
                 (str/replace #" := sorry$" "")))}))

(defn decl-body
  "The lines of a `def NAME ... := by` block: from its declaration line up to
   the blank line that ends it. Used to read what a law is stated ABOUT."
  [ls name]
  (let [n (line-of ls (re-pattern (str "^def " name "\\b")))]
    (when (number? n)
      {:line n
       :body (->> (drop (dec n) ls)
                  (take-while #(not (str/blank? %)))
                  vec)})))

(def o-laws
  (into (sorted-map)
        (for [[law nm] [[:O1 "organiseO1NodesRecorded"]
                        [:O2 "organiseO2AuthoredReachability"]
                        [:O3 "organiseO3FastForward"]
                        [:O4 "organiseO4PrecedenceGovernance"]]]
          (let [{:keys [line body]} (decl-body lean-lines nm)
                joined (str/join "\n" body)]
            [law {:decl nm
                  :line line
                  ;; the carrier each law quantifies over -- the whole point of
                  ;; the census: a law stated of a fixture is witnessed on that
                  ;; fixture and on nothing else.
                  :stated-of (cond
                               (str/includes? joined "wmCascadeDiffFixture") "wmCascadeDiffFixture"
                               :else :other)
                  :mentions-organise? (boolean (re-find #"\borganise\b(?!O)" joined))}]))))

;; Is the sorry load-bearing? A sorry no declaration depends on cannot be
;; discharged INTO anything: nothing downstream changes when it closes.
(def organise-term-references
  "Every occurrence of the bare identifier `organise` outside its own
   declaration, CLASSIFIED rather than counted. Prose inside a `/-- ... -/` doc
   block and a name inside a string literal are not uses: a `sorry` that no
   declaration depends on cannot be discharged INTO anything, and that is a
   claim about TERM references only. The classification is what establishes it."
  (let [classified
        (loop [ls (map-indexed vector lean-lines) in-doc? false acc []]
          (if-let [[i l] (first ls)]
            (let [opens? (re-find #"/--" l)
                  closes? (re-find #"-/" l)
                  doc-here? (or in-doc? (boolean opens?))
                  ;; a string literal cannot be a term use of the identifier
                  stripped (str/replace l #"\"[^\"]*\"" "\"\"")
                  hit? (and (re-find #"\borganise\b" stripped)
                            (not (re-find #"organiseO[1-4]" stripped))
                            (not (re-find #"^def organise " stripped)))
                  in-string-only? (and (re-find #"\borganise\b" l)
                                       (not (re-find #"\borganise\b" stripped)))]
              (recur (rest ls)
                     (and doc-here? (not closes?))
                     (cond
                       (and hit? doc-here?) (conj acc {:line (inc i) :kind :doc-comment-prose})
                       hit? (conj acc {:line (inc i) :kind :term-reference})
                       in-string-only? (conj acc {:line (inc i) :kind :string-literal-name})
                       :else acc)))
            acc))]
    {:by-kind (into (sorted-map)
                    (for [[k v] (group-by :kind classified)]
                      [k (vec (sort (map :line v)))]))
     :term-reference-count (count (filter #(= :term-reference (:kind %)) classified))}))

;; The hole registry's own reading of `organise`.
(def organise-in-hole-registry
  (let [n (line-of lean-lines #"mkRefused \"organise\"")]
    {:line n
     :entry (when (number? n) (str/trim (nth lean-lines (dec n))))}))

;; The fixture the four laws are witnessed on.
(def fixture
  (let [n (line-of lean-lines #"^def wmCascadeDiffFixture : ")
        body (when (number? n)
               (->> (drop (dec n) lean-lines) (take-while #(not (str/blank? %))) vec))
        field (fn [k]
                (some (fn [l]
                        (when-let [m (re-find (re-pattern (str "\\b" k " := (.*?)\\s*\\}?$")) l)]
                          (second m)))
                      body))]
    {:line n
     :carrier (when (number? n)
                (-> (nth lean-lines (dec n))
                    (str/replace #"^def wmCascadeDiffFixture : " "")
                    (str/replace #" :=$" "")))
     :addedByOrganise (field "addedByOrganise")
     :admittedBy (field "admittedBy")
     :selected (field "selected")
     :nodes (field "nodes")}))

;; The Cascade carrier organise RETURNS, versus the CascadeDiff the laws use.
(defn struct-fields [ls name]
  (let [n (line-of ls (re-pattern (str "^structure " name " ")))]
    (when (number? n)
      {:line n
       :fields (->> (drop n ls)
                    (take-while #(re-find #"^  \S+ :" %))
                    (map #(str/trim (first (str/split % #" :"))))
                    vec)})))

(def carriers
  {:Cascade (struct-fields lean-lines "Cascade")
   :CascadeDiff (struct-fields lean-lines "CascadeDiff")
   :Repository (struct-fields lean-lines "Repository")})

;; ---------------------------------------------------------------------------
;; 2. Clojure: the s3e mirror
;; ---------------------------------------------------------------------------

(def clj-lines (lines-of find-organise))

(def clj-organise
  {:file "futon3:checks/find_organise.clj"
   :organise-line (line-of clj-lines #"^\(defn organise$")
   :arglist (let [n (line-of clj-lines #"^\(defn organise$")]
              (when (number? n)
                (some #(when (re-find #"^  \[" %) (str/trim %))
                      (take 30 (drop (dec n) clj-lines)))))
   :admitted-by-line (line-of clj-lines #"^\s+:admitted-by #\{\}")
   :construct-line (line-of clj-lines #"^\(defn construct\b")
   :apply-edit-admits-line (line-of clj-lines #"\[:admitted-by by\]")
   :reproduces-record-line (line-of clj-lines #"^\(defn organise-reproduces-record\?$")
   :mutate-diff-line (line-of clj-lines #"^\(defn mutate-diff\b")})

;; ---------------------------------------------------------------------------
;; 3. The recorded cascades -- which laws are witnessed on a REAL construction
;; ---------------------------------------------------------------------------

(def cascade-records
  ["futon3/checks/construct-cascade.edn"
   "futon3/checks/zaif-cascade.edn"
   "futon3/checks/ants-cascade.edn"
   "futon3/checks/alfworld-cascade.edn"
   "futon3/checks/snatch-cascade.edn"
   "futon3/checks/open-cascade.edn"
   "futon3/checks/open-cascade-short-cue.edn"
   "futon3/checks/retrodiction-cascade.edn"
   "futon3/checks/retrodiction-cascade-per-clause.edn"])

(defn o4-status
  "O4 is the disjunction 'changed precedence changes acting order OR score'.
   A record either EXERCISES it (a map with :holds?) or says why it could not."
  [o4]
  (cond
    (nil? o4) :absent
    (map? o4) {:exercised? true
               :holds? (:holds? o4)
               :precedence-changed? (:precedence-changed? o4)
               :score-changed? (:score-changed? o4)
               :acting-order-changed? (not= (get-in o4 [:row :acting-order-before])
                                            (get-in o4 [:row :acting-order-after]))}
    :else {:exercised? false :reason o4}))

(defn survey [rel]
  (let [path (str code-root "/" rel)]
    (if-not (.exists (io/file path))
      {:record rel :status :not-found}
      (let [d (edn/read-string (slurp path))
            as-of (:as-of d)
            runs (:runs d)]
        {:record rel
         :status :read
         :repository {:patterns (:patterns as-of)
                      :authored-why-edges (:authored-why-edges as-of)
                      :sections (let [s (:sections as-of)] (if (coll? s) (vec (sort (map str s))) s))}
         :tension (:id (:tension d))
         :runs (when runs
                 (into (sorted-map)
                       (for [[k r] runs]
                         [k {:laws (into (sorted-map) (:laws r))
                             :cascade-nodes (:cascade-nodes r)
                             :cascade-edges (:cascade-edges r)
                             :admitted-count (count (:admitted r))
                             :stop (:stop r)}])))
         :o4 (o4-status (:o4 d))
         :cascades-differ? (:cascades-differ? d)}))))

(def cascades (mapv survey cascade-records))

;; The F7 record the acceptance offers as one of its two witness options.
(def f7
  (let [path (str code-root "/futon2/holes/labs/wm-contract/runs/F7-cascade-policy/f7-cascade-policy-decision.edn")
        d (edn/read-string (slurp path))
        cand (first (:candidates d))
        ;; the fields any O-law needs to be stated of a value
        needed #{:nodes :selected :added-by-organise :admitted-by :edges :precedence}]
    {:record "futon2/holes/labs/wm-contract/runs/F7-cascade-policy/f7-cascade-policy-decision.edn"
     :candidate-count (count (:candidates d))
     :candidate-keys (vec (sort (map str (keys cand))))
     :o-law-fields-present (vec (sort (map str (set/intersection needed (set (keys cand))))))
     :o-law-fields-absent (vec (sort (map str (set/difference needed (set (keys cand))))))}))

;; ---------------------------------------------------------------------------
;; 4. The roll-up the account reports
;; ---------------------------------------------------------------------------

(defn law-witnessed-on-a-real-cascade
  "For each O-law, the recorded cascades that witness it holding."
  [law]
  (vec (sort (if (= law :O4)
               (for [c cascades
                     :when (and (map? (:o4 c)) (:exercised? (:o4 c)) (true? (:holds? (:o4 c))))]
                 (:record c))
               (for [c cascades
                     [_ r] (:runs c)
                     :when (true? (get-in r [:laws law]))]
                 (:record c))))))

(def rollup
  (into (sorted-map)
        (for [law [:O1 :O2 :O3 :O4]]
          [law {:lean-stated-of (get-in o-laws [law :stated-of])
                :lean-line (get-in o-laws [law :line])
                :real-cascades-witnessing (distinct (law-witnessed-on-a-real-cascade law))}])))

(def library-wide
  "Which recorded cascades are over the futon3 library rather than a game
   domain -- the acceptance's 'fresh construction from the library'."
  (vec (sort (for [c cascades
                   :when (and (= :read (:status c))
                              (number? (get-in c [:repository :patterns]))
                              (> (get-in c [:repository :patterns]) 100))]
               (:record c)))))

(def receipt
  {:slice :F12-slice-1-census
   :what "Where organise is witnessed today: Lean carriers and laws, the Clojure mirror, and the recorded cascades."
   :lean {:file "mathlib4/DarkTower/WarMachine/Holes.lean"
          :organise lean-organise
          :organise-term-references-outside-its-own-def organise-term-references
          :organise-in-hole-registry organise-in-hole-registry
          :o-laws o-laws
          :fixture fixture
          :carriers carriers}
   :clojure clj-organise
   :recorded-cascades cascades
   :f7-record f7
   :rollup rollup
   :library-wide-records library-wide})

(def out (or (System/getenv "F12_OUT")
             (str home "/code/futon2/holes/labs/wm-contract/runs/F12-organise/00-census.edn")))
(io/make-parents out)
(spit out (with-out-str (pprint/pprint receipt)))

(println "F12 census written:" out)
(println "organise sorry at Holes.lean:" (:decl-line lean-organise)
         "| TERM references outside its own def:" (:term-reference-count organise-term-references)
         "| other occurrences:" (pr-str (dissoc (:by-kind organise-term-references) :term-reference)))
(doseq [[law m] rollup]
  (println (format "  %s stated of %-22s at Holes.lean:%-4s | real cascades witnessing: %d"
                   (name law) (str (:lean-stated-of m)) (str (:lean-line m))
                   (count (:real-cascades-witnessing m)))))
(println "library-wide recorded cascades:" (count library-wide))
(println "F7 record O-law fields present:" (:o-law-fields-present f7)
         "absent:" (count (:o-law-fields-absent f7)))
