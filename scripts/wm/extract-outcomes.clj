#!/usr/bin/env bb
;; H-C-D: read a mission's stated outcomes C from its text, at click time.
;;
;; PROOF-2a (futon2 924a6820): "the outcomes are there to be read, and the War
;; Machine should read them when it needs them: C is computed at click time from
;; the mission text, as a step of the click with its own receipt (each outcome
;; cued to the span it came from; each served-by link to the instance want it
;; names), not required as a record prepared in advance. A mission is never
;; refused for lacking a precomputed C."
;;
;; So this is a READER, not an authoring tool. It reports what the mission says
;; and refuses to report anything it cannot cue. Three disciplines, each of
;; which the output carries so a reviewer can check it:
;;
;;   1. PREDECLARED CUES. The rule table below is fixed before any mission is
;;      read, and every outcome names the rule that found it. A cue table tuned
;;      per mission would make the extractor a transcription of its author's
;;      reading, which is what the reference exists to test against.
;;   2. VERIFIED SPANS. Every quote of every cue must occur EXACTLY ONCE in the
;;      file and at the line range reported. One failure refuses the whole
;;      output: a C whose cues are half-checked is worse than none, because the
;;      half that resolve make the rest look checked.
;;   3. TYPED ABSENCE, NEVER A SUBSTITUTED VALUE. No outcome found emits
;;      {:absent :no-stated-outcome} naming every section read. An instance with
;;      no cascade emits {:absent :no-cascade}, not an empty want list. An
;;      unstated weighting emits {:absent :unstated}, not a uniform prior.
;;
;; Served-by is drawn from SECTION CONTAINMENT: an outcome stated inside
;; "### <n>. <title>" is served by the wants of that instance's cascade. That is
;; structural and exact. An outcome stated outside any instance section gets
;; {:unlinked :outside-instance-sections} -- the honest answer, since linking it
;; would mean guessing which instance a mission-level sentence is about.
;;
;; Fixes against H-C-D section 4 (futon2 f20084da had both defects):
;;
;;   E1 OUTCOME IDENTITY. One row per cued sentence counted an outcome stated
;;      three times as three outcomes. Cued sentences that name the same
;;      instance, or the same party-and-artefact, or the same distinctive
;;      artefact with compatible attribution now consolidate into ONE outcome
;;      carrying ALL its spans as a list of cues, first span first
;;      (consolidate, below; the vocabularies it uses are data).
;;   E2 CONSEQUENCE VOICE. An outcome stated as the cost of its absence was not
;;      cued. The cue table now carries that voice (:cue/must-impersonate,
;;      :cue/every-later-must, :cue/otherwise, :cue/cost-of), each gated by
;;      :requires :artefact -- a consequence sentence that names no artefact is
;;      not cued, because a "must" about nothing is not an outcome.
;;
;; Read-only. Writes nothing; prints EDN on stdout. Exits 2 on refusal.
;;
;; OFFSET UNIT (H-C-D R1, adopted in mission-C.edn futon3c 6149272b): every
;; :span this script emits, and every span it reads from a reference C, is in
;; Unicode code points, zero-based, end-exclusive, declared as :offset-unit on
;; the map that carries it. The mission has 392 more UTF-8 bytes than code
;; points, so a byte reading is wrong everywhere; a reference that declares no
;; unit, or a different one, is refused rather than reinterpreted.
;;
;; Run: clojure -M scripts/wm/extract-outcomes.clj <mission.md> [--cascades DIR] [--reference C.edn]
;;  or: bb scripts/wm/extract-outcomes.clj <mission.md> [--cascades DIR] [--reference C.edn]

(require '[clojure.string :as str]
         '[clojure.set :as set]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp])

(import '[java.security MessageDigest])

;; ---------------------------------------------------------------- cue table

;; Each rule: :id names it in the output, :re finds it, :reads says in words what
;; the rule believes it is reading. A reviewer disputing an outcome disputes a
;; named rule, not the extractor's taste. :requires :artefact gates a rule: the
;; cued sentence must name an artefact (below) or the hit is dropped.
(def cue-rules
  [{:id :cue/closing-ask      :re #"(?i)closing ask"
    :reads "an ask recorded as the speaker's own, for the future"}
   {:id :cue/wants-a          :re #"(?i)\bwants? (?:a|an|the)\b"
    :reads "a party states what it wants to exist"}
   {:id :cue/so-party-could   :re #"(?i)\bso (?:that )?\w[\w/ -]{0,40}? (?:could|can|would)\b"
    :reads "a purpose clause: the state something is for"}
   {:id :cue/could-not        :re #"(?i)\bcould not (?:use|do|be|run)\b"
    :reads "a party blocked; the outcome is the unblocking"}
   {:id :cue/had-to           :re #"(?i)\bhad to (?:be changed|add|change|impersonate|reimplement|transcribe)\b"
    :reads "work forced by the coupling; the outcome is not having to"}
   {:id :cue/intended-end     :re #"(?i)intended end state"
    :reads "the destination named as against a transitional state"}
   {:id :cue/exit-is          :re #"(?i)\bthe exit is\b"
    :reads "a phase exit: the condition the mission must reach"}
   {:id :cue/do-not           :re #"(?i)\*\*Do not\*\*"
    :reads "a prohibition; the outcome is the state it protects"}
   {:id :cue/develops         :re #"(?i)what this mission develops"
    :reads "the mission's own subject, stated as such"}
   {:id :cue/recorded-form    :re #"(?i)must be recorded in a form"
    :reads "a constraint the mission places on its own artefacts"}
   {:id :cue/not-what-wanted  :re #"(?i)what one does not want"
    :reads "a negated outcome; the outcome is its complement"}
   ;; E2: the consequence voice -- the outcome stated as the cost of its
   ;; absence. Each gated on :artefact (H-C-D E2: "must impersonate",
   ;; "must <verb> ... every later", "otherwise", "the cost of").
   {:id :cue/must-impersonate :re #"(?i)\bmust impersonate\b"
    :requires :artefact
    :reads "the coupling's cost stated as forced impersonation; the outcome is not having to"}
   {:id :cue/every-later-must :re #"(?i)\bevery later\b[^.\n]{0,80}?\bmust\b"
    :requires :artefact
    :reads "a burden every later instance must carry; the outcome is lifting it"}
   {:id :cue/otherwise        :re #"(?i)\botherwise\b"
    :requires :artefact
    :reads "the consequence of not reaching the outcome"}
   {:id :cue/cost-of          :re #"(?i)\bcosts? (?:of|more)\b"
    :requires :artefact
    :reads "the outcome's absence stated as a cost"}
   ;; E3: the capability contrast -- the outcome stated as "with X you can V;
   ;; with Y you must W" (or cannot). Both arms must be present: a single
   ;; "with the script you can regenerate the page" is a method statement
   ;; (H-C-D E4), not an outcome, and the two-arm shape is what keeps it out.
   ;; Gated on :artefact, as E2: a contrast about nothing named is not cued.
   ;; M-futon-seams L124, span [13700 13817]:
   ;;   "With hardcoded code you can grep for the literal; with a hardcoded
   ;;    prompt you must match natural language at runtime."
   {:id :cue/contrast-with-you-can
    :re #"(?i)\bwith (?:a |an |the )?[\w-][^.\n]{0,60}? you can\b[^.\n]{0,100}?; with (?:a |an |the )?[\w-][^.\n]{0,60}? you (?:must|cannot|can't)\b"
    :requires :artefact
    :reads "two couplings contrasted by the capability each leaves you; the outcome is the capable arm"}
   ;; Same voice, "lets you ... where ... cannot" form. Declared ahead of the
   ;; corpus: no mission sentence instantiates it yet, so no span is quoted --
   ;; the table is predeclared, and a shape with no instance cues nothing.
   {:id :cue/contrast-lets-you
    :re #"(?i)\blets? you\b[^.\n]{0,80}?\bwhere\b[^.\n]{0,60}?\b(?:cannot|can't|must not)\b"
    :requires :artefact
    :reads "a capability one artefact grants where its contrast cannot"}
   ;; Same voice, the negated-capability form "a property of X, not a Y":
   ;; M-futon-seams L93, span [10283 10371]:
   ;;   "With roles that is a property of the binding, not a string comparison
   ;;    in the dispatcher."
   {:id :cue/contrast-property-not
    :re #"(?i)\bproperty of the \w[^.\n]{0,40}?, not a\b"
    :requires :artefact
    :reads "a capability the outcome makes structural, named against the manual contrast it replaces"}])

;; Parties the corpus names. Attribution is by naming, never by inference.
(def parties ["Rob" "Joe" "claude-1" "claude-10" "kimi-4"])

;; ------------------------------------------------------- E1/E2 vocabularies

;; Artefacts an outcome can be ABOUT. Two purposes, both as data:
;;  - E2 gating: a consequence-voice sentence is cued only if it names one.
;;  - E1 identity: two cues naming the same party-and-artefact state one
;;    outcome twice. Matching is case-insensitive, word-bounded, a trailing
;;    plural "s" ignored. "code" is deliberately NOT an artefact: it matches
;;    "VS Code" and every "the code" sentence, and bridged two different
;;    outcomes into one when tried.
(def artefacts
  ["seam" "interface" "adapter" "transport" "protocol" "parser" "schema" "shim"
   "implementation" "MCP" "caller" "client" "server" "prompt" "store" "flag"
   "path" "role" "ledger" "abstraction" "binding" "module" "component"
   "matrix-ircd" "neo4j"])

;; Distinctive artefacts: rare enough that two cues naming the same one state
;; the same outcome even when one of them names no party (e.g. a sentence
;; inside an instance section restating a party's closing ask). Generic
;; artefacts like "implementation" are NOT here: two sentences about an
;; implementation can be about different outcomes.
(def distinctive-artefacts ["seam" "matrix-ircd" "neo4j" "MCP"])

(def artefact-re
  (re-pattern (str "(?i)\\b(" (str/join "|" (map #(java.util.regex.Pattern/quote %) artefacts)) ")s?\\b")))

(defn- normalise-artefact [^String mention]
  (str/replace (str/lower-case mention) #"s$" ""))

;; ------------------------------------------------------------------- helpers

(defn sha256 [^String s]
  (let [d (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8"))]
    (apply str (map #(format "%02x" %) d))))

(defn line-of
  "1-based line number of character index i."
  [^String text i]
  (inc (count (re-seq #"\n" (subs text 0 i)))))

;; ----------------------------------------------------------- offset unit
;; Every span is Unicode code points, zero-based, end-exclusive (H-C-D R1).
;; Java String indices are UTF-16 code units; these two convert. For BMP-only
;; text they coincide with char indices; for astral characters they do not.

(def offset-unit :unicode-codepoints-zero-based-end-exclusive)

(defn cp-subs
  "Substring by Unicode code point offsets, zero-based, end-exclusive."
  [^String text a b]
  (subs text (.offsetByCodePoints text 0 a) (.offsetByCodePoints text 0 b)))

(defn headings
  "Every markdown heading with its level, title and line."
  [^String text]
  (->> (str/split-lines text)
       (map-indexed (fn [i l]
                      (when-let [m (re-matches #"(#{1,6})\s+(.*)" l)]
                        {:level (count (nth m 1)) :title (str/trim (nth m 2)) :line (inc i)})))
       (remove nil?)
       vec))

(defn instance-sections
  "### <n>. <title> sections, with the line range each spans. These are the only
   sections whose contents can be linked to an instance by containment.

   Scoped to the level-2 section that introduces the instances. Without that
   scope DERIVE's numbered method steps (### 1. Someone hits the coupling, ...)
   parse as instances 1-3, and every served-by row is emitted twice."
  [hs total-lines]
  (let [h2 (filter #(= 2 (:level %)) hs)
        anchor (first (filter #(re-find #"(?i)instances?$|(?i)^the \w+ instances" (:title %)) h2))
        stop (when anchor (first (filter #(> (:line %) (:line anchor)) h2)))
        lo (if anchor (:line anchor) 0)
        hi (if stop (:line stop) (inc total-lines))
        ins (filter #(and (= 3 (:level %))
                          (re-find #"^\d+\.\s" (:title %))
                          (< lo (:line %) hi)) hs)]
    (mapv (fn [h]
            (let [n (Integer/parseInt (second (re-find #"^(\d+)\." (:title h))))
                  after (filter #(and (> (:line %) (:line h)) (<= (:level %) 3)) hs)
                  end (if (seq after) (dec (:line (first after))) total-lines)]
              {:instance n :title (:title h) :lines [(:line h) end]}))
          ins)))

(defn sentence-around
  "The sentence containing character index i: back to the previous boundary,
   forward to the next. Boundaries are a blank line, or '. '/'.\n'/':\n' that is
   not inside a markdown code span."
  [^String text i]
  (let [bstart (loop [j (max 0 (dec i))]
                 (cond (<= j 0) 0
                       (and (= \newline (.charAt text j))
                            (or (zero? j) (= \newline (.charAt text (dec j))))) (inc j)
                       (and (= \. (.charAt text j))
                            (< (inc j) (count text))
                            (#{\space \newline} (.charAt text (inc j)))) (+ j 2)
                       :else (recur (dec j))))
        bend (loop [j i]
               (cond (>= j (dec (count text))) (count text)
                     (and (= \newline (.charAt text j))
                          (= \newline (.charAt text (inc j)))) j
                     (and (= \. (.charAt text j))
                          (< (inc j) (count text))
                          (#{\space \newline} (.charAt text (inc j)))) (inc j)
                     ;; a colon ends a sentence only when it introduces a block
                     (and (= \: (.charAt text j))
                          (< (inc j) (count text))
                          (= \newline (.charAt text (inc j)))) (inc j)
                     :else (recur (inc j))))]
    [bstart bend]))

(defn cascade-wants
  "instance number -> its :want set, read from <dir>/instance-<n>.edn. Only the
   :want line is read; the cascade is not interpreted."
  [dir]
  (when (and dir (.isDirectory (io/file dir)))
    (into {}
          (keep (fn [f]
                  (when-let [m (re-find #"instance-(\d+)\.edn$" (.getName f))]
                    (let [body (slurp f)]
                      (when-let [w (re-find #":want\s+#\{([^}]*)\}" body)]
                        [(Integer/parseInt (second m))
                         (->> (str/split (str/trim (second w)) #"\s+")
                              (remove str/blank?)
                              (mapv #(keyword (str/replace % #"^:" ""))))]))))
                (.listFiles (io/file dir))))))

;; ---------------------------------------------------------------- extraction

(defn extract
  "One row per distinct cued sentence (before E1 consolidation). A rule gated
   :requires :artefact yields a hit only when the sentence names an artefact."
  [^String text section-of _wants]
  (let [hits (for [rule cue-rules
                   m (let [mm (re-matcher (:re rule) text)]
                       (loop [acc []] (if (.find mm) (recur (conj acc (.start mm))) acc)))]
               (let [[s e] (sentence-around text m)
                     raw (subs text s e)
                     quote (str/trim raw)
                     ;; UTF-16 index of the trimmed quote, then code points
                     qs (+ s (- (count raw) (count (str/triml raw))))
                     qe (+ qs (count quote))
                     span [(.codePointCount text 0 qs) (.codePointCount text 0 qe)]
                     l0 (line-of text s) l1 (line-of text (max s (dec e)))
                     named (->> parties
                                (keep (fn [p]
                                        (let [mm (re-matcher
                                                  (java.util.regex.Pattern/compile
                                                   (str "\\b" (java.util.regex.Pattern/quote p) "\\b"))
                                                  quote)]
                                          (when (.find mm) [(.start mm) p]))))
                                (sort-by first) first second)
                     inst (section-of l0)]
                 {:rule (:id rule) :reads (:reads rule) :requires (:requires rule)
                  :quote quote :cue [l0 l1] :span span
                  :whose (cond named named
                               inst  :unattributed-in-instance-section
                               :else :the-mission)
                  :instance inst}))]
    (->> hits
         (remove #(str/blank? (:quote %)))
         ;; E2 gate: consequence-voice rules cue only sentences about an artefact
         (remove #(and (= :artefact (:requires %))
                       (not (re-find artefact-re (:quote %)))))
         ;; one row per distinct quote; a sentence matched by two rules keeps both
         (group-by :quote)
         (map (fn [[_q ms]]
                (let [f (first ms)]
                  (-> f
                      (assoc :rules (vec (sort (distinct (map :rule ms)))))
                      (dissoc :rule :reads :requires)))))
         (sort-by (comp first :cue))
         vec)))

;; ---------------------------------------------------- E1: outcome identity

(defn- mentioned-instances
  "Instance numbers the cue text itself names (\"instance 4\")."
  [^String quote]
  (set (map #(Integer/parseInt (second %))
            (re-seq #"(?i)\binstances?\s+(\d+)\b" quote))))

(defn- quote-artefacts
  "The artefact vocabulary entries the cue text names, normalised (lowercase,
   plural stripped)."
  [^String quote]
  (set (map #(normalise-artefact (second %)) (re-seq artefact-re quote))))

(defn- compatible-whose?
  "Attributions that can be one outcome's: equal, or one is an unnamed sentence
   in an instance section and the other a named party. :the-mission merges with
   nothing but itself -- a mission-method sentence is not a party's outcome."
  [a b]
  (or (= a b)
      (and (= a :unattributed-in-instance-section) (string? b))
      (and (= b :unattributed-in-instance-section) (string? a))))

(defn consolidate
  "E1: cued sentences that name the same instance, the same party-and-artefact,
   or the same distinctive artefact with compatible attribution state ONE
   outcome. Union-find over those three keys; the outcome carries every cue as a
   list, first span first. Two sentences naming DIFFERENT instance numbers are
   never merged, whatever else they share."
  [rows]
  (let [rows (mapv #(assoc %
                           :mentioned (mentioned-instances (:quote %))
                           :named-artefacts (quote-artefacts (:quote %))
                           :distinctive (set/intersection (quote-artefacts (:quote %))
                                                          (set (map str/lower-case distinctive-artefacts))))
                   rows)
        n (count rows)
        parent (atom (vec (range n)))
        find-root (fn [p x] (let [r (nth p x)] (if (= r x) [p x] (recur p r))))
        union! (fn [i j]
                 (swap! parent
                        (fn [p]
                          (let [[p ri] (find-root p i)
                                [p rj] (find-root p j)]
                            (if (= ri rj) p (assoc p ri rj))))))
        veto? (fn [a b]
                (and (seq (:mentioned a)) (seq (:mentioned b))
                     (empty? (set/intersection (:mentioned a) (:mentioned b)))))
        mergeable? (fn [a b]
                     (and (not (veto? a b))
                          (or ;; the cue texts name the same instance
                              (and (seq (:mentioned a))
                                   (seq (set/intersection (:mentioned a) (:mentioned b))))
                              ;; the same party-and-artefact
                              (and (string? (:whose a)) (= (:whose a) (:whose b))
                                   (seq (set/intersection (:named-artefacts a) (:named-artefacts b))))
                              ;; the same distinctive artefact, compatibly attributed
                              (and (compatible-whose? (:whose a) (:whose b))
                                   (seq (set/intersection (:distinctive a) (:distinctive b)))))))]
    (doseq [i (range n) j (range (inc i) n)
            :when (mergeable? (nth rows i) (nth rows j))]
      (union! i j))
    (->> (group-by #(second (find-root @parent %)) (range n))
         vals
         (map (fn [idxs]
                (let [cs (->> idxs (map rows) (sort-by (comp first :cue)) vec)]
                  {:whose (or (some #(when (string? (:whose %)) (:whose %)) cs)
                              (:whose (first cs)))
                   :instance (some :instance cs)
                   :rules (vec (sort (distinct (mapcat :rules cs))))
                   :cues (mapv #(select-keys % [:quote :cue :span :rules]) cs)})))
         (sort-by (fn [o] (first (:cue (first (:cues o)))))) ;; first cue's line
         vec)))

(defn verify
  "Every cue's quote occurs exactly once, at the line it claims, and at the
   code-point span it claims. Returns failures; one failure refuses the whole
   output."
  [^String text outcomes]
  (vec (for [o outcomes
             c (:cues o)
             :let [q (:quote c)
                   n (count (re-seq (java.util.regex.Pattern/compile
                                     (java.util.regex.Pattern/quote q)) text))
                   i (str/index-of text q)
                   l (when i (line-of text i))
                   span-ok (try (= (cp-subs text (first (:span c)) (second (:span c))) q)
                                (catch Exception _ false))]
             :when (or (not= 1 n) (not= l (first (:cue c))) (not span-ok))]
         {:id (:id o)
          :quote (subs q 0 (min 60 (count q)))
          :occurrences n :claimed-line (first (:cue c)) :found-line l
          :span-resolves span-ok})))

;; ---------------------------------------------------- reference comparison

(defn read-reference
  "Read a reference C for the comparison. Refuses with a typed reason unless it
   declares exactly the offset unit the comparison reads spans in (H-C-D R1) --
   a C whose :cue units are undeclared is a span that cannot be read, not a
   span to be guessed at."
  [path]
  (let [ref (edn/read-string (slurp path))]
    (if (= offset-unit (:offset-unit ref))
      ref
      {:refused :reference-offset-unit-mismatch
       :required offset-unit
       :found (if (contains? ref :offset-unit) (:offset-unit ref) :absent)
       :reference path})))

(defn compare-reference
  "Recall and extras against a reference C. Each reference outcome's own :cue
   span is checked to resolve to its :cue-quote under the declared unit (a
   reference whose spans do not resolve is reported, not trusted); it is a HIT
   when its :cue-quote occurs in one of the emitted outcomes' cue quotes."
  [^String text ref outcomes]
  (let [rows (vec (for [[k v] (:outcomes ref)]
                    (let [[a b] (:cue v)
                          resolves (try (= (cp-subs text a b) (:cue-quote v))
                                        (catch Exception _ false))
                          hit (first (filter (fn [o] (some #(str/includes? (:quote %) (:cue-quote v))
                                                           (:cues o)))
                                             outcomes))]
                      {:reference-outcome k
                       :span-resolves resolves
                       :hit (when hit {:id (:id hit) :cues (mapv :cue (:cues hit))})})))
        total (count rows)
        hits (count (filter :hit rows))]
    {:reference-outcomes total
     :mission-sha-match (when (:mission-sha ref) (= (:mission-sha ref) (sha256 text)))
     :hits hits
     :recall (if (zero? total) {:absent :empty-reference} (str hits "/" total))
     :outcomes-emitted (count outcomes)
     :extras (- (count outcomes) hits)
     :rows rows}))

(defn -main [& args]
  (let [[path & more] args
        opts (apply hash-map more)
        cdir (get opts "--cascades")
        refpath (get opts "--reference")]
    (when-not (and path (.isFile (io/file path)))
      (binding [*out* *err*] (println "usage: extract-outcomes.clj <mission.md> [--cascades DIR] [--reference C.edn]"))
      (System/exit 2))
    (let [text (slurp path)
          ref (when refpath (read-reference refpath))]
      (if (:refused ref)
        (do (pp/pprint ref)
            (System/exit 2))
        (let [lines (count (str/split-lines text))
          hs (headings text)
          isecs (instance-sections hs lines)
          section-of (fn [l] (:instance (first (filter #(<= (first (:lines %)) l (second (:lines %))) isecs))))
          wants (or (cascade-wants cdir) {})
          rows (extract text section-of wants)
          outcomes (consolidate rows) ;; already first-span-first
          outcomes (mapv #(assoc %1 :id (keyword (str "o-" (inc %2))))
                         outcomes
                         (range))
          fails (verify text outcomes)
          served (vec (for [s isecs
                            :let [ws (get wants (:instance s))
                                  os (filterv #(= (:instance s) (:instance %)) outcomes)]]
                        (if (nil? ws)
                          {:instance (:instance s)
                           :absent :no-cascade
                           :cue (:lines s)
                           :outcomes-stated-here (mapv :id os)}
                          {:instance (:instance s)
                           :wants ws
                           :serves (mapv :id os)
                           :basis :section-containment})))
          unlinked (filterv #(nil? (:instance %)) outcomes)
          out (cond-> {:schema :wm/mission-outcomes-v3
                       :offset-unit offset-unit
                       :extractor {:script "scripts/wm/extract-outcomes.clj"
                                   :cue-rules (mapv :id cue-rules)
                                   :consolidation :e1-same-instance-or-party-and-artefact
                                   :served-by-basis :section-containment}
                       :mission {:path path :lines lines :sha256 (sha256 text)}
                       :sections-read (mapv #(select-keys % [:level :title :line]) hs)
                       :outcomes outcomes
                       :served-by served
                       :unlinked {:count (count unlinked)
                                  :ids (mapv :id unlinked)
                                  :absent :outside-instance-sections}
                       :weighting {:absent :unstated
                                   :note "no line assigns a magnitude or compares two outcomes"}}
                ref (assoc :reference-comparison (compare-reference text ref outcomes)))]
      (cond
        (seq fails)
        (do (pp/pprint {:schema :wm/mission-outcomes-v3
                        :offset-unit offset-unit
                        :refused :cue-does-not-resolve
                        :mission path
                        :failures fails})
            (System/exit 2))

        (empty? outcomes)
        (pp/pprint (assoc out :outcomes {:absent :no-stated-outcome
                                         :sections-read (mapv :title hs)}))

        :else (pp/pprint out)))))))

;; Run as a script only; a test that load-files this file (into its own ns)
;; gets the fns, not a System/exit. clojure -M <this-file> and bb both run in
;; the user ns; load-file from a test does not.
(when (and (= 'user (ns-name *ns*))
           (or *command-line-args*
               (and *file* (= *file* (System/getProperty "babashka.file")))))
  (apply -main *command-line-args*))
