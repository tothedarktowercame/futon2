;; H-C-REACH-D: why served-by links are 0 beyond M-futon-seams, and whether a
;; field-derived rule links anything (claude-13, 2026-09-25). Read-only: reads
;; the target-field fixture, each target's text at the head the fixture
;; recorded (git show), and M-futon-seams at the pinned sha the extractor test
;; uses; writes nothing; prints to stdout.
;;
;;   cd /home/joe/code/futon2
;;   bb holes/labs/wm-contract/proof2/packets/h_c_reach_d.clj
;;
;; Rule A is the extractor as it stands (coupling-artefacts, ### n. sections).
;; Rule B is the proposal measured here, predeclared only in its SHAPE:
;;   an artefact is a token of one of three lexical shapes -- a backticked
;;   code span, a file name with a source extension, or a hyphenated
;;   identifier of >= 3 parts -- that occurs in an admitted outcome's cue AND
;;   in a sentence of the want's unit carrying a direction verb from
;;   `direction-verbs` below. Nothing in B names an artefact of any target.
;; The want's unit is an instance section (### n. under an instances anchor,
;; the extractor's shape) or, at target grain, the want's criterion line as
;; the criteria reader cued it (fixture :interpretations-for).

(require '[clojure.edn :as edn]
         '[clojure.java.shell :as sh]
         '[clojure.string :as str]
         '[clojure.set :as set])

(def root "/home/joe/code")
(def fixture (str root "/futon2/test/fixtures/target-field/target-field@futon2-7bd17dfb.edn"))
(def extractor (str root "/futon2/scripts/wm/extract-outcomes.clj"))
(load-file extractor)

(defn git-show [repo rev path]
  (let [r (sh/sh "git" "-C" (str root "/" repo) "show" (str rev ":" path))]
    (when (zero? (:exit r)) (:out r))))

;; ------------------------------------------------------------------ rule B

(def direction-verbs
  "Generic verbs of acting on an artefact. Predeclared; no entry names an
  artefact."
  [[:declare #"(?i)\bdeclar"] [:convert #"(?i)\bconvert"] [:retire #"(?i)\bretir"]
   [:replace #"(?i)\breplac|\binstead of\b"] [:remove #"(?i)\bremov|\bdelet|\bdrop"]
   [:migrate #"(?i)\bmigrat|\bport(?:ed|ing)?\b"] [:rename #"(?i)\brenam"]
   [:extract #"(?i)\bextract"] [:unify #"(?i)\bunif|\bmerg|\bconsolidat"]
   [:bind #"(?i)\bbind|\bwire[sd]?\b|\bwiring\b"] [:test #"(?i)\btest"]
   [:reimplement #"(?i)\breimplement|\bimpersonat|\bmimic"]
   [:sync #"(?i)kept in sync|\bdrift"] [:absorb #"(?i)\babsorb|\bdisappear"]])

(def token-shapes
  [[:code-span #"`([^`\n]{2,80})`"]
   [:file #"\b([\w./-]+\.(?:clj|cljc|cljs|el|py|edn|md|lean|sh|json|js|ts))\b"]
   [:hyphenated #"\b([a-z][a-z0-9]*(?:-[a-z0-9]+){2,})\b"]])

(defn tokens [s]
  (set (for [[kind re] token-shapes
             m (re-seq re s)]
         [kind (str/lower-case (second m))])))

(defn sentences [^String text]
  ;; the extractor's own sentence boundaries, walked from the start
  (loop [i 0 acc []]
    (if (>= i (count text))
      acc
      (let [[s e] (sentence-around text (min i (dec (count text))))
            e (max e (inc i))]
        (recur e (conj acc (subs text s (min e (count text)))))))))

(defn direction [sentence]
  (some (fn [[kw re]] (when (re-find re sentence) kw)) direction-verbs))

(defn b-links
  "units: [{:unit id :text s}], outcomes: admitted with :cues. One link per
  (unit, outcome): the shared token, the unit sentence and its verb."
  [units outcomes]
  (vec (for [u units
             o outcomes
             :let [otoks (reduce set/union #{} (map (comp tokens :quote) (:cues o)))
                   hit (when (seq otoks)
                         (first (for [snt (sentences (:text u))
                                      :let [shared (set/intersection otoks (tokens snt))
                                            dir (direction snt)]
                                      :when (and (seq shared) dir)]
                                  {:shared (vec (sort shared)) :direction dir
                                   :sentence (str/trim (subs snt 0 (min 160 (count snt))))})))]
             :when hit]
         (merge {:unit (:unit u) :outcome (:id o)} hit))))

;; ------------------------------------------------------------ extraction

(defn pipeline [text]
  (let [lines (count (str/split-lines text))
        hs (headings text)
        isecs (instance-sections hs lines)
        section-of (fn [l] (:instance (first (filter #(<= (first (:lines %)) l (second (:lines %))) isecs))))
        cs (mapv #(assoc %1 :id (keyword (str "o-" (inc %2))))
                 (consolidate (extract text section-of {})) (range))
        {:keys [outcomes]} (filter-outcomes text cs)
        line-text (fn [[a b]] (str/join "\n" (subvec (vec (str/split-lines text)) (dec a) (min b lines))))]
    {:hs hs :isecs isecs :outcomes outcomes
     :sections (mapv (fn [s] {:unit (:instance s) :title (:title s) :text (line-text (:lines s))}) isecs)
     :a-links (artefact-links text isecs outcomes)}))

(defn anchor [hs]
  (first (filter #(and (= 2 (:level %))
                       (re-find #"(?i)instances?$|(?i)^the \w+ instances" (:title %))) hs)))

;; --------------------------------------------------------- shape census

(defn shape-census [text]
  (let [ls (str/split-lines text)
        hs (headings text)]
    {:h2 (count (filter #(= 2 (:level %)) hs))
     :h3 (count (filter #(= 3 (:level %)) hs))
     :h3-numbered (count (filter #(and (= 3 (:level %)) (re-find #"^\d+\.\s" (:title %))) hs))
     :instances-anchor (boolean (anchor hs))
     :instance-heading (count (filter #(re-find #"(?i)\binstance\b" (:title %)) hs))
     :phase-heading (count (filter #(re-find #"^(?:HEAD|IDENTIFY|MAP|DERIVE|ARGUE|VERIFY|INSTANTIATE|DOCUMENT)\b" (:title %)) hs))
     :table-rows (count (filter #(re-find #"^\s*\|" %) ls))
     :numbered-bullets (count (filter #(re-find #"^\s*\d+\.\s" %) ls))
     :checkboxes (count (filter #(re-find #"^\s*[-*] \[[ xX]\]" %) ls))
     :exit-criterion (count (filter #(re-find #"\*\*Exit criteri" %) ls))}))

;; ------------------------------------------------------------------ main

(def pinned-seams-sha "d13c5cfe9e9b19b445bd5bb73507f286a9e5ff3b478a1c5bc6a2250d70c6f6fd")

(def reference-served-by
  ;; futon2 test/futon2/wm/extract_outcomes_test.clj L319-332 (a31f9cee)
  [[4 :prefix-routing-retired :o-4] [4 :caller-converted :o-2] [4 :redirect-test :o-2]
   [5 :protocol-declared :o-2] [5 :impersonation-retired :o-4] [5 :adapter-conformance-test :o-5]
   [6 :one-authority :o-5] [6 :one-authority :o-8] [6 :flag-retired :o-5]
   [7 :record-schema-declared :o-1] [7 :record-schema-declared :o-2]
   [7 :writers-converted :o-5] [7 :divergence-test :o-5]])

(defn score [link-pairs]
  (let [ref-pairs (set (map (fn [[i _ o]] [i o]) reference-served-by))
        reproduced (filter (fn [[i _ o]] (contains? link-pairs [i o])) reference-served-by)]
    {:recall (str (count reproduced) "/" (count reference-served-by))
     :misses (vec (remove (set reproduced) reference-served-by))
     :links (count link-pairs)
     :links-outside-reference (vec (sort (remove ref-pairs link-pairs)))}))

(defn seams-text []
  ;; the pinned mission text: find the futon3c commit whose file has the sha
  (let [revs (str/split-lines (:out (sh/sh "git" "-C" (str root "/futon3c") "log" "--format=%H"
                                           "--" "holes/missions/M-futon-seams.md")))]
    (some (fn [r] (let [t (git-show "futon3c" r "holes/missions/M-futon-seams.md")]
                    (when (and t (= pinned-seams-sha (sha256 t))) [r t])))
          revs)))

(defn -run []
  (println ";; extractor sha256" (sha256 (slurp extractor)))
  (println ";; fixture sha256" (sha256 (slurp fixture)))
  (println ";; this script sha256" (sha256 (slurp *file*)))
  (let [[rev seams] (seams-text)
        {:keys [sections outcomes a-links]} (pipeline seams)
        cascaded (filter #(#{4 5 6 7} (:unit %)) sections)
        b (b-links cascaded outcomes)
        containment (set (for [o outcomes c (:cues o) :when (:instance o)] [(:instance o) (:id o)]))]
    (println "\n== M-futon-seams control, futon3c" (subs rev 0 8) "sha256" pinned-seams-sha)
    (println "outcomes" (count outcomes) " rule A:" (pr-str (score (set (map (juxt :instance :outcome) a-links)))))
    (println "containment baseline:" (pr-str (score containment)))
    (println "rule B (sections 4-7):" (pr-str (score (set (map (juxt :unit :outcome) b)))))
    (doseq [l b] (prn (select-keys l [:unit :outcome :shared :direction :sentence])))
    (println "rule A+B union:" (pr-str (score (set/union (set (map (juxt :instance :outcome) a-links))
                                                          (set (map (juxt :unit :outcome) b)))))))
  (let [f (edn/read-string {:default tagged-literal} (slurp fixture))
        tf (get-in f [:decision :target-field])
        heads (get-in f [:read :heads])
        by-target (into {} (map (juxt :target identity)) (:considered tf))
        rows (vec (for [t (:feasible tf)
                        :let [c (by-target (:target t))
                              text (git-show (:repo c) (get heads (:repo c)) (:path c))]]
                    (assoc t :repo (:repo c) :path (:path c) :text text)))
        with-sections (filter #(seq (:isecs (pipeline (:text %)))) rows)]
    (println "\n== Q1: the" (count with-sections) "targets with ### n. sections")
    (doseq [r with-sections
            :let [{:keys [hs sections outcomes]} (pipeline (:text r))
                  obst (for [e coupling-artefacts re (:obstacle-res e) :when (re-find re (:text r))] (:id e))
                  cue-obst (for [o outcomes c (:cues o) e coupling-artefacts re (:obstacle-res e)
                                 :when (re-find re (:quote c))] (:id e))
                  mentions (for [s sections e coupling-artefacts re (:mention-res e)
                                 :when (re-find re (:text s))] (:id e))
                  b (b-links sections outcomes)]]
      (prn {:target (:target r)
            :anchor (:title (anchor hs))
            :sections (count sections)
            :section-titles (mapv #(subs (:title %) 0 (min 48 (count (:title %)))) (take 3 sections))
            :outcomes (count outcomes)
            :i-vocabulary (cond (empty? outcomes) :no-admitted-outcome
                                (seq cue-obst) {:matched (vec (distinct cue-obst))}
                                :else {:miss true :obstacle-hits-anywhere-in-text (count obst)})
            :ii-mention (if (seq mentions) {:matched (vec (distinct mentions))} {:miss true})
            :iii-shape (if (anchor hs) :instances-anchor :numbered-subsections-no-instances-anchor)
            :b-links (count b)})
      (doseq [o outcomes] (prn {:target (:target r) :cue (:quote (first (:cues o)))
                                :tokens (vec (sort (tokens (:quote (first (:cues o))))))}))
      (doseq [l b] (prn (assoc (select-keys l [:unit :outcome :shared :direction :sentence]) :target (:target r)))))
    (println "\n== Q3: shape census, the 23 and a seeded sample of 40 of the other" (- (count rows) (count with-sections)))
    (let [others (vec (remove (set (map :target with-sections)) (map :target rows)))
          rnd (java.util.Random. 20260925)
          sample (set (take 40 (sort-by (fn [_] (.nextDouble rnd)) others)))
          census (fn [rs] (let [cs (map (comp shape-census :text) rs)]
                            (into (sorted-map)
                                  (for [k (keys (first cs))]
                                    [k (if (boolean? (get (first cs) k))
                                         (count (filter k cs))
                                         {:targets-with (count (filter #(pos? (get % k)) cs))
                                          :total (reduce + (map k cs))})]))))]
      (println "the 23:" (pr-str (census with-sections)))
      (println "sample 40:" (pr-str (census (filter #(sample (:target %)) rows))))
      (println "sample ids:" (pr-str (vec (sort sample)))))
    (println "\n== Q4(e): rule B at target grain, want = criterion line (the 12 with named wants)")
    (let [twelve (filter #(= :ask-interpretation (:next-step %)) rows)]
      (doseq [r twelve
              :let [{:keys [outcomes]} (pipeline (:text r))
                    ls (vec (str/split-lines (:text r)))
                    units (vec (for [w (:interpretations-for r)]
                                 {:unit (:want w)
                                  :text (or (some->> (:line w) dec (get ls)) (:criterion w) "")}))
                    b (b-links units outcomes)]]
        (prn {:target (:target r) :wants (count units) :outcomes (count outcomes) :b-links (count b)})
        (doseq [l b] (prn (assoc (select-keys l [:unit :outcome :shared :direction :sentence]) :target (:target r))))))
    (println "\n== Q4(e): rule B on the 23 (sections as units)")
    (let [n (count (filter #(seq (b-links (:sections (pipeline (:text %))) (:outcomes (pipeline (:text %))))) with-sections))]
      (println "targets gaining >= 1 link:" n "of" (count with-sections)))))

(-run)
