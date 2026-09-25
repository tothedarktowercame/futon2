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
;; Served-by is drawn from SHARED NAMED ARTEFACTS (H-C-DEF §4, E6): outcome o
;; is served by the wants of instance i iff i's section names the same artefact
;; one of o's cues carries as its obstacle, AND the mention sentence carries a
;; direction verb (the section treats the artefact as something being declared,
;; converted, retired, tested, absorbed, replaced or kept-in-sync -- acted on,
;; not merely present). The coupling-artefact vocabulary below is data,
;; predeclared like the cue table. ALL wants of a linked instance serve each
;; linked outcome: per-want lexical grounding was judged too fragile (the want
;; tokens do not lexically recur in the sections), so the link is stated at
;; outcome granularity with the mention sentence as :via evidence. An outcome
;; no instance section links this way gets {:absent :no-shared-artefact}.
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
;;   E4/E5 FOUR-CLAUSE FILTER (H-C-DEF §2). Cued sentences that are method
;;      rules, completion criteria, or past/current facts are not outcomes.
;;      Each cue is scored against the four clauses (clause-verdict); a
;;      sentence failing any clause is emitted under :rejected with :clause and
;;      its cue, never under :outcomes. An evidence-voiced cue inside an
;;      "Evidence it is needed" paragraph is a finer-grain facet of its
;;      instance's outcome (:facets, :facet-of), not a new outcome.
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

(defn instances-anchor
  "The level-2 heading that introduces the instances (`## ... instances`,
   `## The <n> instances`), or nil."
  [hs]
  (first (filter #(and (= 2 (:level %))
                       (re-find #"(?i)instances?$|(?i)^the \w+ instances" (:title %)))
                 hs)))

(defn instance-sections
  "### <n>. <title> sections, with the line range each spans. These are the only
   sections whose contents can be linked to an instance by containment.

   Scoped to the level-2 section that introduces the instances. Without that
   scope DERIVE's numbered method steps (### 1. Someone hits the coupling, ...)
   parse as instances 1-3, and every served-by row is emitted twice.

   No anchor, no units (H-C-REACH-I). The earlier fallback scanned the whole
   file, so numbered findings, protocol steps, bands and rubric levels became
   117 'instance' units across 23 feasible targets, none an instance
   (H-C-REACH-D §3, futon2 b10589a9). -main records the anchor's absence as
   {:absent :no-instances-anchor} in the served-by slot."
  [hs total-lines]
  (let [h2 (filter #(= 2 (:level %)) hs)
        anchor (instances-anchor hs)
        stop (when anchor (first (filter #(> (:line %) (:line anchor)) h2)))
        lo (when anchor (:line anchor))
        hi (if stop (:line stop) (inc total-lines))
        ins (when anchor
              (filter #(and (= 3 (:level %))
                            (re-find #"^\d+\.\s" (:title %))
                            (< lo (:line %) hi)) hs))]
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

;; ---------------------------------------------------- E6: served-by by artefact
;; The coupling-artefact vocabulary (H-C-DEF §4). Each entry names ONE artefact
;; as it appears in two voices: :obstacle-res match an outcome's cue (the
;; artefact carried as the obstacle the outcome removes); :mention-res match a
;; sentence inside an instance section (the same artefact named where the
;; cascade lives); :direction-verbs are [keyword regex] pairs -- the mention
;; sentence must carry one, evidencing that the section treats the artefact as
;; being acted on. The matched keyword is reported as :direction. Predeclared
;; data, like the cue table: a reviewer disputes a named entry, not taste.
(def coupling-artefacts
  [{:id :provider-parsed-from-agent-id
    :obstacle-res [#"provider out of the agent id" #"hardcoded to talk to"]
    :mention-res [#"pattern-matching an id" #"parsed out of the id" #"provider-literal"]
    :direction-verbs [[:declare #"declare"] [:replace #"instead of"]]}
   {:id :first-implementation-impersonated
    :obstacle-res [#"impersonate the first"]
    :mention-res [#"impersonating the transport" #"mimic IRC"
                  #"agreeing by convention" #"reimplement sentence splitting"]
    :direction-verbs [[:impersonate #"impersonat"] [:mimic #"mimic"]
                      [:reimplement #"reimplement"] [:convention #"agreeing by convention"]]}
   {:id :second-implementation-kept-in-sync
    :obstacle-res [#"already drifted" #"drifted cost more to unify"]
    :mention-res [#"impersonating the transport" #"mimic IRC"
                  #"second source of truth" #"kept in sync"
                  #"agreeing by convention" #"reimplement sentence splitting"]
    :direction-verbs [[:keep-in-sync #"kept in sync"] [:impersonate #"impersonat"]
                      [:mimic #"mimic"] [:reimplement #"reimplement"]
                      [:convention #"agreeing by convention"] [:drift #"drifting"]]}
   {:id :hardcoded-prompt-text
    :obstacle-res [#"hardcoded prompt"]
    :mention-res [#"the hardcoded one" #"prompt text" #"hardcoded prompt"]
    :direction-verbs [[:absorb #"absorb"] [:disappear #"disappear"]
                      [:never-treated #"never treated"]]}
   {:id :emacs-turn-record-seam
    :obstacle-res [#"seam in the Emacs layer" #"turn-annotation seam" #"seam inserted"]
    :mention-res [#"turn record" #"block-quote parser" #"record writer"]
    :direction-verbs [[:written-again #"written again"]
                      [:convention #"agreeing by convention"] [:reimplement #"reimplement"]]}])

(defn- line-start-offsets
  "Char index where each 1-based line starts: (nth v (dec line))."
  [^String text]
  (loop [v [0] i 0]
    (if-let [j (str/index-of text "\n" i)]
      (recur (conj v (inc j)) (inc j))
      v)))

(defn- section-mention
  "The first sentence inside section s that names entry's artefact (a
   :mention-res hit) AND carries one of its :direction-verbs. Returns
   {:span [codepoints] :direction kw} or nil."
  [^String text line-starts s entry]
  (let [[l0 l1] (:lines s)
        ca (nth line-starts (dec l0))
        cb (if (< l1 (count line-starts)) (nth line-starts l1) (count text))
        section (subs text ca cb)]
    (first
     (for [mre (:mention-res entry)
           :let [mm (re-matcher mre section)]
           m (loop [acc []] (if (.find mm) (recur (conj acc (.start mm))) acc))
           :let [[s0 e0] (sentence-around section m)
                 sentence (subs section s0 e0)
                 dir (some (fn [[kw re]] (when (re-find re sentence) kw))
                           (:direction-verbs entry))]
           :when dir]
       {:span [(.codePointCount text 0 (+ ca s0)) (.codePointCount text 0 (+ ca e0))]
        :direction dir}))))

(defn artefact-links
  "E6 (H-C-DEF §4): outcome o is served by the wants of instance i iff i's
   section mentions an artefact that one of o's cues carries as its obstacle,
   and the mention sentence carries a direction verb. One link per
   (instance, outcome); :via carries the matched artefact, the mention
   sentence's span (:want-span), the obstacle cue's span (:outcome-span) and
   the direction verb found. All wants of a linked instance serve the outcome
   -- per-want lexical grounding was judged too fragile (the want tokens do
   not recur lexically in the sections)."
  [^String text isecs outcomes]
  (let [line-starts (line-start-offsets text)]
    (vec
     (for [s isecs
           o outcomes
           entry coupling-artefacts
           :let [ocue (first (for [c (:cues o)
                                   re (:obstacle-res entry)
                                   :when (re-find re (:quote c))]
                               c))
                 mention (when ocue (section-mention text line-starts s entry))]
           :when (and ocue mention)]
       {:instance (:instance s)
        :outcome (:id o)
        :via {:artefact (:id entry)
              :want-span (:span mention)
              :outcome-span (:span ocue)
              :direction (:direction mention)}}))))

;; ------------------------------------------- proposed links (H-C-REACH-I2)
;; Beyond M-futon-seams no lexical vocabulary generalises (H-C-REACH-D §2,
;; futon2 b10589a9): a served-by link is PROPOSED by a reader and VERIFIED here
;; against the same facts artefact-links writes into :via.

(def direction-verbs
  "Every coupling-artefacts direction verb, in table order, first occurrence
   kept. Reused, not extended."
  (vec (reduce (fn [acc [kw re]] (if (some #(= kw (first %)) acc) acc (conj acc [kw re])))
               [] (mapcat :direction-verbs coupling-artefacts))))

(defn- phrase-in?
  "PHRASE occurs in S as whole words, case-insensitive."
  [^String s phrase]
  (boolean
   (and (string? phrase) (not (str/blank? phrase)) s
        (re-find (java.util.regex.Pattern/compile
                  (str "(?i)(?<!\\w)" (java.util.regex.Pattern/quote phrase) "(?!\\w)"))
                 s))))

(defn- span-text [^String text [a b]]
  (try (when (and (integer? a) (integer? b) (<= 0 a b)) (cp-subs text a b))
       (catch Exception _ nil)))

(defn verify-proposed-link
  "Verify a PROPOSED served-by link {:instance n :outcome :o-k :artefact s
   :want-span [a b] :outcome-span [c d]} (spans in code points, the unit every
   :via span uses). Returns artefact-links' row shape with :via :basis
   :proposed-verified, or {:status :refused :reason r :detail ...} for the
   first failing condition, in order:
     :text-mismatch               the proposal's optional :text-sha256 is not
                                  this text's (spans are meaningless elsewhere)
     :instance-unknown            the instance is not a unit of isecs (a file
                                  with no instances anchor verifies nothing)
     :outcome-unknown             the outcome is not an admitted outcome id
     :want-span-outside-instance  the want span is not inside that section
     :outcome-span-not-a-cue      the outcome span is not exactly one of that
                                  outcome's cue spans
     :artefact-not-in-both        the artefact string (case-insensitive) is
                                  not in both span texts
     :no-direction-verb           the want span carries none of the
                                  coupling-artefacts direction verbs
   The conditions are the :via fields plus the two identity checks.

   :artefact is either ONE string, which must occur in both spans, or
   {:outcome-phrase s1 :want-phrase s2}: s1 in the outcome span, s2 in the
   want span (H-C-REACH-I3). The two-string form has the shape of a vocabulary
   entry (one artefact named by an :obstacle-res phrasing and a :mention-res
   phrasing). The difference is that a vocabulary entry's co-reference of its two
   phrasings was predeclared, while a reader's is CLAIMED. The verifier checks
   everything it can (spans resolve, each phrase in its own span, a direction
   verb) and records the co-reference as :coreference :reader-claimed. That
   claim is the only thing a two-string link rests on beyond the checked
   facts. Phrases match as whole words, case-insensitive (no word character
   on either side), so a stem shared by inflection is not an artefact:
   \"impersonat\" does not match \"impersonating\", and \"bind\" does not
   match \"bindings\". That is why \"impersonating\" vs \"impersonate\" needs
   the two-string form.

   A consumer can tell three link kinds apart. Vocabulary links carry :via
   :artefact as an entry id. One-string proposals carry a string and :basis
   :proposed-verified. Two-string proposals carry the phrase map, :basis
   :proposed-verified and :coreference :reader-claimed. Nothing here proposes
   links."
  [^String text isecs outcomes {:keys [instance outcome artefact want-span outcome-span text-sha256]}]
  (let [refuse (fn [reason detail] {:status :refused :reason reason :detail detail})
        sec (first (filter #(= instance (:instance %)) isecs))
        o (first (filter #(= outcome (:id %)) outcomes))
        want-text (span-text text want-span)
        out-text (span-text text outcome-span)
        want-lines (when want-text
                     (let [[a b] want-span
                           ca (.offsetByCodePoints text 0 a)
                           cb (.offsetByCodePoints text 0 b)]
                       [(line-of text ca) (line-of text (max ca (dec cb)))]))
        [out-phrase want-phrase] (if (map? artefact)
                                   [(:outcome-phrase artefact) (:want-phrase artefact)]
                                   [artefact artefact])
        missing (vec (concat (when-not (phrase-in? out-text out-phrase) [:outcome-phrase])
                             (when-not (phrase-in? want-text want-phrase) [:want-phrase])))
        dir (when want-text (some (fn [[kw re]] (when (re-find re want-text) kw)) direction-verbs))]
    (cond
      ;; optional pin (claude-10's read step, 2026-09-25): a proposal may carry
      ;; the sha256 of the text its spans were taken against; a different text
      ;; here would make every span mean something else, so it refuses first
      (and text-sha256 (not= text-sha256 (sha256 text)))
      (refuse :text-mismatch {:proposal-text-sha256 text-sha256 :text-sha256 (sha256 text)})
      (nil? sec) (refuse :instance-unknown {:instance instance :instances (mapv :instance isecs)})
      (nil? o) (refuse :outcome-unknown {:outcome outcome :admitted (mapv :id outcomes)})
      (not (and want-lines
                (<= (first (:lines sec)) (first want-lines))
                (<= (second want-lines) (second (:lines sec)))))
      (refuse :want-span-outside-instance {:want-span want-span :want-lines want-lines
                                           :section-lines (:lines sec)})
      (not (some #(= outcome-span (:span %)) (:cues o)))
      (refuse :outcome-span-not-a-cue {:outcome-span outcome-span :cue-spans (mapv :span (:cues o))})
      (seq missing)
      (refuse :artefact-not-in-both {:artefact artefact :missing missing})
      (nil? dir) (refuse :no-direction-verb {:want-span want-span})
      :else
      {:instance instance
       :outcome outcome
       ;; two phrases that are the same string (case-insensitive) claim nothing
       ;; beyond the one-string check, so they are recorded as the one-string
       ;; form: :coreference marks only a co-reference the checks did not verify
       :via (let [same? (and (map? artefact)
                             (= (str/lower-case (:outcome-phrase artefact))
                                (str/lower-case (:want-phrase artefact))))]
              (cond-> {:artefact (if same? (:outcome-phrase artefact) artefact)
                       :want-span want-span :outcome-span outcome-span
                       :direction dir :basis :proposed-verified}
                (and (map? artefact) (not same?)) (assoc :coreference :reader-claimed)))})))

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

;; ---------------------------------------------------- E4/E5: four-clause filter
;; H-C-DEF §2 (proof2/packets/H-C-DEF.md): an outcome is a sentence that
;;   1. states a property of the world after the mission that does not hold now;
;;   2. closes a named discrepancy for an attributable party;
;;   3. is checkable by a world-check OUTSIDE the mission document;
;;   4. is not discharged by the mission completing.
;; Each cued sentence is scored against the four clauses BY CUE: the rule set
;; that fired plus structural features of the quote decide, so a reviewer
;; disputes a named clause rule, not the extractor's taste. A sentence failing
;; any clause is never emitted under :outcomes; it goes to :rejected carrying
;; :clause <which> and its cue. An outcome (post-E1) is admitted when at least
;; one of its cues passes all four clauses; a cue that fails inside an admitted
;; outcome is carried on the outcome as :clause-failures (o-4's evidence-voiced
;; second cue is evidence FOR the outcome, not a second row).

(defn clause-verdict
  "Score one cued sentence against H-C-DEF §2's four clauses. Returns nil when
   the sentence passes all four, else {:clause n :reason kw :reads str} for the
   FIRST failing clause. `whose` is the consolidated outcome's attribution."
  [{:keys [rules quote whose]}]
  (let [rs (set rules)]
    (cond
      ;; clause 1 -- a past/current fact, not a post-mission world property
      (= rs #{:cue/had-to})
      {:clause 1 :reason :clause1/past-tense-report
       :reads "forced-work narrative with no purpose clause: true before the mission started"}
      (re-find #"(?i)^\*\*In:\*\*" quote)
      {:clause 1 :reason :clause1/derive-input
       :reads "a DERIVE step's input specification restating an existing discrepancy as method input"}
      (re-find #"^-\s+`[^`]+`" quote)
      {:clause 1 :reason :clause1/existing-artefact
       :reads "a bullet on an artefact that exists now; its property holds before the mission"}
      ;; clause 2 -- no attributable party and nothing in the world named.
      ;; A sentence cued by a mission-method rule is not judged here: its
      ;; subject is the mission's own artefacts, which the artefact vocabulary
      ;; deliberately does not name; clauses 3/4 give its verdict below.
      (and (= :the-mission whose)
           (not (re-find artefact-re quote))
           (not (some rs [:cue/intended-end :cue/develops :cue/recorded-form
                          :cue/exit-is :cue/do-not])))
      {:clause 2 :reason :clause2/no-party-no-artefact
       :reads "attributes to no party and names no artefact whose state could close a discrepancy"}
      ;; clause 3 -- checkable only by inspecting the mission's own artefacts
      (contains? rs :cue/intended-end)
      {:clause 3 :reason :clause3/enactment-end-state
       :reads "the end state of the mission's own enactment; checkable by inspecting the artefact"}
      (contains? rs :cue/develops)
      {:clause 3 :reason :clause3/method-description
       :reads "describes the mission's own method; checkable by reading the mission"}
      (contains? rs :cue/recorded-form)
      {:clause 3 :reason :clause3/artefact-form-constraint
       :reads "constrains the form of the mission's own artefacts; checked by reading them"}
      ;; clause 4 -- discharged by the mission completing
      (contains? rs :cue/exit-is)
      {:clause 4 :reason :clause4/completion-criterion
       :reads "an exit rule ('how will we know it's done'); discharged when the mission completes"}
      (contains? rs :cue/do-not)
      {:clause 4 :reason :clause4/imperative-to-the-mission
       :reads "an imperative to the mission's own process; discharged by enactment"}
      :else nil)))

;; ------------------------------------------------ cue shape (H-C-REACH-I)
;; H-C-DEF §2's four clauses presuppose a sentence. Off M-futon-seams the cue
;; rules also fire on spans that are not sentences at all; on
;; M-apm-demonstration (futon3c holes/missions/M-apm-demonstration.md) 13
;; outcomes were admitted, among them
;;   a heading:     "### First, the cost of tuning role cards — permitted, but priced"
;;   a fenced block: "```clojure\n:reg/escalation {:trigger    :no-improvement-across-student-attempts ..."
;;   a merge note:   "Merged (`see log`) but\nNOT reloaded — the live JVM predates the env flag, ..."
;; (H-C-REACH-D §1, futon2 b10589a9). A span of one of these shapes cannot be
;; an outcome, whatever cue fired on it. The table is data; a reviewer
;; disputes a named shape. :inside-fence is judged on the span's position in
;; the text, the others on the quote.

(def cue-shapes
  [{:id :shape/heading
    :re #"^#{1,6}\s"
    :reads "a markdown heading, not a sentence"}
   {:id :shape/fenced-block
    :re #"^```"
    :inside-fence true
    :reads "a fenced code block, or a span inside one"}
   {:id :shape/merge-note
    :re #"(?i)^(?:\*\*)?(?:[^:\n]{0,60}:\s*)?(?:reviewed and )?merged\b"
    :reads "a merge/review log line reporting that a change landed"}])

(defn- inside-fence?
  "True when code point offset a lies inside a ``` fenced block of text."
  [^String text a]
  (let [before (subs text 0 (.offsetByCodePoints text 0 a))]
    (odd? (count (re-seq #"(?m)^\s*```" before)))))

(defn shape-verdict
  "nil, or {:clause :shape :reason :shape/<id> :reads s} for the first
   cue-shapes entry the cue matches."
  [^String text {:keys [quote span]}]
  (some (fn [{:keys [id re inside-fence reads]}]
          (when (or (re-find re quote)
                    (and inside-fence span (inside-fence? text (first span))))
            {:clause :shape :reason id :reads reads}))
        cue-shapes))

(defn- cue-verdict
  "Shape first, then the four clauses."
  [^String text cue whose]
  (or (shape-verdict text cue)
      (clause-verdict (assoc cue :whose whose))))

(defn- facet-lead-in?
  "E1-residual (H-C-DEF §3, :o-6): a cue whose blank-line-delimited paragraph
   the mission itself introduces with 'Evidence it is needed' is a finer-grain
   facet of that instance's outcome, evidence-voiced -- not a new outcome."
  [^String text cue-span]
  (let [a (first cue-span)
        qs (.offsetByCodePoints text 0 a)
        pstart (if-let [i (str/last-index-of text "\n\n" qs)] (+ i 2) 0)
        pend (or (let [i (str/index-of text "\n\n" qs)] (when i i)) (count text))]
    (boolean (re-find #"(?i)evidence it is needed" (subs text pstart pend)))))

(defn filter-outcomes
  "E4/E5: partition consolidated outcomes into admitted / rejected / facets.
   Every row keeps its cues; rejected rows carry :clause, :reason and the cue
   that failed; facet rows carry :facet-of the admitted outcome of the same
   instance section (a facet with no admitted parent is rejected on clause 1,
   never silently admitted)."
  [^String text outcomes]
  (let [scored (mapv (fn [o]
                       (let [fails (vec (keep (fn [c]
                                                (when-let [v (cue-verdict text c (:whose o))]
                                                  (assoc v :cue (:cue c) :quote (:quote c))))
                                              (:cues o)))]
                         (assoc o
                                :facet? (boolean (some #(facet-lead-in? text (:span %)) (:cues o)))
                                :passes? (boolean (some #(nil? (cue-verdict text % (:whose o)))
                                                        (:cues o)))
                                :clause-failures fails)))
                       outcomes)
        admitted (filterv #(and (not (:facet? %)) (:passes? %)) scored)
        admitted-by-inst (into {} (keep (fn [o] (when (:instance o) [(:instance o) (:id o)])))
                               admitted)
        facets (filterv :facet? scored)
        facets (mapv (fn [f]
                       (if-let [parent (get admitted-by-inst (:instance f))]
                         (assoc f :facet-of parent)
                         (assoc f :facet? false :passes? false
                                :clause-failures [{:clause 1 :reason :clause1/facet-without-parent
                                                   :reads "evidence-voiced facet cue with no admitted outcome in its instance section"
                                                   :cue (:cue (first (:cues f)))
                                                   :quote (:quote (first (:cues f)))}])))
                     facets)
        ;; one row per original outcome: facets replace their scored entries
        facets-merged (mapv (fn [o] (or (some #(when (= (:id %) (:id o)) %) facets) o)) scored)
        rejected (filterv #(or (and (not (:facet? %)) (not (:passes? %)))
                               (and (:facet? %) (not (:facet-of %))))
                          facets-merged)
        rejected (mapv (fn [r]
                         (let [f0 (first (:clause-failures r))]
                           (-> r
                               (dissoc :facet? :passes?)
                               (assoc :clause (:clause f0) :reason (:reason f0)
                                      :reads (:reads f0)
                                      :failed-cue (:cue f0)))))
                       rejected)]
    {:outcomes (mapv #(dissoc % :facet? :passes?) admitted)
     :rejected (vec rejected)
     :facets (mapv #(dissoc % :facet? :passes? :clause-failures) (vec (filter :facet-of facets)))}))

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
          consolidated (consolidate rows) ;; already first-span-first
          consolidated (mapv #(assoc %1 :id (keyword (str "o-" (inc %2))))
                             consolidated
                             (range))
          {admitted :outcomes rejected :rejected facets :facets}
          (filter-outcomes text consolidated)
          fails (verify text (concat admitted rejected facets))
          links (artefact-links text isecs admitted)
          anchor (instances-anchor hs)
          served (vec (for [s isecs
                            :let [ws (get wants (:instance s))
                                  ls (filterv #(= (:instance s) (:instance %)) links)]]
                        (if (nil? ws)
                          {:instance (:instance s)
                           :absent :no-cascade
                           :cue (:lines s)}
                          {:instance (:instance s)
                           :wants ws
                           :serves (mapv (fn [l] {:outcome (:outcome l) :via (:via l)}) ls)
                           :basis :shared-named-artefact})))
          linked-ids (set (map :outcome links))
          unlinked (filterv #(not (contains? linked-ids (:id %))) admitted)
          out (cond-> {:schema :wm/mission-outcomes-v5
                       :offset-unit offset-unit
                       :extractor {:script "scripts/wm/extract-outcomes.clj"
                                   :cue-rules (mapv :id cue-rules)
                                   :consolidation :e1-same-instance-or-party-and-artefact
                                   :filter :e4-e5-four-clause-H-C-DEF-S2
                                   :served-by-basis :shared-named-artefact
                                   :coupling-artefacts (mapv :id coupling-artefacts)}
                       :mission {:path path :lines lines :sha256 (sha256 text)}
                       :sections-read (mapv #(select-keys % [:level :title :line]) hs)
                       :outcomes admitted
                       :rejected rejected
                       :facets facets
                       :filter-counts {:outcomes (count admitted)
                                       :rejected (count rejected)
                                       :facets (count facets)}
                       :served-by (if anchor served {:absent :no-instances-anchor})
                       :unlinked {:count (count unlinked)
                                  :outcomes (mapv (fn [o] {:outcome (:id o)
                                                           :absent :no-shared-artefact})
                                                  unlinked)}
                       :weighting {:absent :unstated
                                   :note "no line assigns a magnitude or compares two outcomes"}}
                ref (assoc :reference-comparison (compare-reference text ref admitted)))]
      (cond
        (seq fails)
        (do (pp/pprint {:schema :wm/mission-outcomes-v5
                        :offset-unit offset-unit
                        :refused :cue-does-not-resolve
                        :mission path
                        :failures fails})
            (System/exit 2))

        (empty? admitted)
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
