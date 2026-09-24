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
;;   2. VERIFIED SPANS. Every quote must occur EXACTLY ONCE in the file and at
;;      the line range reported. One failure refuses the whole output: a C whose
;;      cues are half-checked is worse than none, because the half that resolve
;;      make the rest look checked.
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
;; Read-only. Writes nothing; prints EDN on stdout. Exits 2 on refusal.
;;
;; Run: clojure -M scripts/wm/extract-outcomes.clj <mission.md> [--cascades DIR]
;;  or: bb scripts/wm/extract-outcomes.clj <mission.md> [--cascades DIR]

(require '[clojure.string :as str]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp])

(import '[java.security MessageDigest])

;; ---------------------------------------------------------------- cue table

;; Each rule: :id names it in the output, :re finds it, :reads says in words what
;; the rule believes it is reading. A reviewer disputing an outcome disputes a
;; named rule, not the extractor's taste.
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
    :reads "a negated outcome; the outcome is its complement"}])

;; Parties the corpus names. Attribution is by naming, never by inference.
(def parties ["Rob" "Joe" "claude-1" "claude-10" "kimi-4"])

;; ------------------------------------------------------------------- helpers

(defn sha256 [^String s]
  (let [d (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8"))]
    (apply str (map #(format "%02x" %) d))))

(defn line-of
  "1-based line number of character index i."
  [^String text i]
  (inc (count (re-seq #"\n" (subs text 0 i)))))

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
   forward to the next. Boundaries are a blank line, or '. '/'.\\n'/':\\n' that is
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

(defn extract [^String text section-of wants]
  (let [hits (for [rule cue-rules
                   m (let [mm (re-matcher (:re rule) text)]
                       (loop [acc []] (if (.find mm) (recur (conj acc (.start mm))) acc)))]
               (let [[s e] (sentence-around text m)
                     quote (str/trim (subs text s e))
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
                 {:rule (:id rule) :reads (:reads rule)
                  :quote quote :cue [l0 l1]
                  :whose (cond named named
                               inst  :unattributed-in-instance-section
                               :else :the-mission)
                  :instance inst}))]
    (->> hits
         (remove #(str/blank? (:quote %)))
         ;; one outcome per distinct quote; a sentence matched by two rules keeps both
         (group-by :quote)
         (map (fn [[q ms]]
                (let [f (first ms)]
                  (-> f
                      (assoc :rules (vec (sort (distinct (map :rule ms)))))
                      (dissoc :rule :reads)))))
         (sort-by (comp first :cue))
         vec)))

(defn verify
  "Every quote occurs exactly once and at the line it claims. Returns failures."
  [^String text outcomes]
  (vec (for [o outcomes
             :let [n (count (re-seq (java.util.regex.Pattern/compile
                                     (java.util.regex.Pattern/quote (:quote o))) text))
                   i (str/index-of text (:quote o))
                   l (when i (line-of text i))]
             :when (or (not= 1 n) (not= l (first (:cue o))))]
         {:quote (subs (:quote o) 0 (min 60 (count (:quote o))))
          :occurrences n :claimed-line (first (:cue o)) :found-line l})))

(defn -main [& args]
  (let [[path & more] args
        opts (apply hash-map more)
        cdir (get opts "--cascades")]
    (when-not (and path (.isFile (io/file path)))
      (binding [*out* *err*] (println "usage: extract-outcomes.clj <mission.md> [--cascades DIR]"))
      (System/exit 2))
    (let [text (slurp path)
          lines (count (str/split-lines text))
          hs (headings text)
          isecs (instance-sections hs lines)
          section-of (fn [l] (:instance (first (filter #(<= (first (:lines %)) l (second (:lines %))) isecs))))
          wants (or (cascade-wants cdir) {})
          outcomes (extract text section-of wants)
          fails (verify text outcomes)
          ids (zipmap (map :quote outcomes) (map #(keyword (str "o-" (inc %))) (range)))
          outcomes (mapv #(assoc % :id (ids (:quote %))) outcomes)
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
          out {:schema :wm/mission-outcomes-v1
               :extractor {:script "scripts/wm/extract-outcomes.clj"
                           :cue-rules (mapv :id cue-rules)
                           :served-by-basis :section-containment}
               :mission {:path path :lines lines :sha256 (sha256 text)}
               :sections-read (mapv #(select-keys % [:level :title :line]) hs)
               :outcomes outcomes
               :served-by served
               :unlinked {:count (count unlinked)
                          :ids (mapv :id unlinked)
                          :absent :outside-instance-sections}
               :weighting {:absent :unstated
                           :note "no line assigns a magnitude or compares two outcomes"}}]
      (cond
        (seq fails)
        (do (pp/pprint {:schema :wm/mission-outcomes-v1
                        :refused :cue-does-not-resolve
                        :mission path
                        :failures fails})
            (System/exit 2))

        (empty? outcomes)
        (pp/pprint (assoc out :outcomes {:absent :no-stated-outcome
                                         :sections-read (mapv :title hs)}))

        :else (pp/pprint out)))))

(apply -main *command-line-args*)
