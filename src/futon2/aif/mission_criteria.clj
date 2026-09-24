(ns futon2.aif.mission-criteria
  "A mission's completion criteria, read from its text, as flight wants
  (H-exits; amendment A-exits in proof2/packets/H-EXITS-D.md).

  Two stated forms are read, beside the `- [ ]` tasks mission_hole_wants
  already reads:

    :phase-exit            a paragraph opening `**Exit criterion:**`, its
                           phase the enclosing `## ` heading
    :completion-criterion  a bullet under a criteria heading (any level):
                           `Completion criteria`, `Acceptance`, `Success
                           criteria`, `Exit criteria`, `Done when`

  Every criterion becomes a want: the flight is not done until each is met.
  A criterion is OBSERVABLE when the mission states a verdict for it inline
  (`**Met…`, `**Not met…`, `**Not started…`). Its locator is C4 over the
  mission file with :decl = the criterion's own stated text, line breaks
  kept, followed by `**Met.**`. That declaration is present only when THIS
  criterion reads exactly Met: a partial verdict (`**Met for instance 4**`)
  is not Met, and because the declaration begins with the criterion's own
  words it cannot be satisfied by another criterion's verdict (the falsifier
  of H-EXITS-D §4: a locator over the criterion sentence alone reads true
  under either verdict). A criterion with no stated verdict has no locator
  and is returned with :reason :verdict-not-stated; the tick's assembly then
  refuses the target naming it (:tokens-without-checkable-locator), which is
  the hole the mission has to close by stating a verdict.

  Pure over the text, except `read-mission`, which reads the file at HEAD."
  (:require [clojure.edn]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [futon2.aif.observation-checks :as checks])
  (:import [java.security MessageDigest]))

(def exit-marker "**Exit criterion:**")
(def verdict-re #"\*\*(?:Met|Not met|Not started)")
(def met-token "**Met.**")
(def criteria-heading-re
  ;; the heading IS the name, optionally followed by a dash, colon or
  ;; parenthetical: "Acceptance (2026-09-12)" yes, "Acceptance tests" no
  #"(?i)^(?:completion criteria|acceptance(?: criteria)?|success criteria|exit criteria|done when)\s*(?:$|[:(]|[—–-]\s)")

(defn- sha1-12 [^String s]
  (let [d (.digest (MessageDigest/getInstance "SHA-1") (.getBytes s "UTF-8"))]
    (subs (apply str (map #(format "%02x" %) d)) 0 12)))

(defn- heading [line]
  (when-let [[_ hashes title] (re-matches #"^(#+)\s+(.*)$" line)]
    {:level (count hashes) :title (str/trim title)}))

(defn- paragraph
  "Lines from I until a blank line or a heading, joined with newlines."
  [lines i]
  (str/join "\n" (take-while #(and (not (str/blank? %)) (nil? (heading %)))
                             (cons (nth lines i) (drop (inc i) lines)))))

(defn- bullet
  "A bullet's text: its first line and the indented continuation lines."
  [lines i]
  (str/join "\n" (cons (nth lines i)
                       (take-while #(and (not (str/blank? %)) (re-find #"^\s+\S" %)
                                         (not (re-find #"^\s*[-*]\s" %)))
                                   (drop (inc i) lines)))))

(defn verdict-class
  "The stated verdict, typed: :met only for exactly **Met.**; a verdict
  that begins Met but qualifies it (\"**Met for instance 4**\") is
  :verdict-partial with its :qualifier, because the owner has evidence for
  part of the exit, which is a different next step from none."
  [verdict]
  (let [inner (some-> verdict (str/replace #"^\*\*|\*\*$" "") str/trim)]
    (cond
      (nil? verdict) {:class :verdict-not-stated}
      (= verdict met-token) {:class :met}
      (re-find #"^Met\b" inner) {:class :verdict-partial :qualifier inner}
      ;; a negative result the owner keeps: no honest work produces the
      ;; exit, so it is never a want (the flight would drive toward
      ;; softening the finding); it is named out of view instead
      (re-find #"(?i)^Not met, retained as a finding" inner) {:class :verdict-not-met-retained :qualifier inner}
      (re-find #"^Not started" inner) {:class :verdict-not-started}
      (re-find #"^Not met" inner) {:class :verdict-not-met :qualifier inner}
      :else {:class :verdict-unrecognised :qualifier inner})))

(defn- criterion [mission-id kind line-no phase text]
  (let [m (re-matcher verdict-re text)
        at (when (.find m) (.start m))
        stated (if at (subs text 0 at) text)]
    (cond-> {:kind kind
             :line line-no
             :phase phase
             :stated (str/trimr stated)
             :token (keyword "exit" (str "h" (sha1-12 (str mission-id "\n" kind "\n"
                                                          (first (str/split-lines stated))))))}
      at (as-> c (assoc c :verdict (re-find #"^\*\*[^*]*\*\*" (subs text at))
                              :met-decl (str stated met-token))
           (assoc c :verdict-class (verdict-class (:verdict c))))
      (not at) (assoc :reason :verdict-not-stated
                      :verdict-class {:class :verdict-not-stated}))))

(defn criteria
  "Every completion criterion stated in TEXT, in document order."
  [mission-id text]
  (let [lines (vec (str/split-lines text))]
    (loop [i 0 phase nil cc-level nil out []]
      (if (>= i (count lines))
        out
        (let [line (nth lines i)
              h (heading line)]
          (cond
            h (recur (inc i)
                     (if (= 2 (:level h)) (:title h) phase)
                     (cond (re-find criteria-heading-re (:title h)) (:level h)
                           (and cc-level (<= (:level h) cc-level)) nil
                           :else cc-level)
                     out)
            (str/starts-with? (str/triml line) exit-marker)
            (recur (inc i) phase cc-level
                   (conj out (criterion mission-id :phase-exit (inc i) phase (paragraph lines i))))
            (and cc-level (re-find #"^[-*]\s+(?!\[[ xX]\])\S" line))
            (recur (inc i) phase cc-level
                   (conj out (criterion mission-id :completion-criterion (inc i) phase (bullet lines i))))
            :else (recur (inc i) phase cc-level out)))))))

(defn wants
  "Flight wants from ALL-CRITERIA over the mission file REPO/PATH at HEAD:
  {:wants [token …] :locators {token C4} :universe {token bool}
   :unlocated [{:token :line :kind :reason}]}. OBSERVE takes a locator and
  returns true/false (default: observation-checks/check-decl-in-file)."
  [all-criteria {:keys [repo path observe]}]
  (let [observe (or observe #(true? (:observed (checks/check-decl-in-file %))))
        retained? #(= :verdict-not-met-retained (get-in % [:verdict-class :class]))
        retained (filter retained? all-criteria)
        criteria (remove retained? all-criteria)
        located (filter :met-decl criteria)
        locator (fn [c] {:class :C4 :repo repo :sha "HEAD" :path path :decl (:met-decl c)})]
    {:wants (mapv :token criteria)
     :locators (into {} (map (fn [c] [(:token c) (locator c)])) located)
     :universe (into {} (map (fn [c] [(:token c) (boolean (observe (locator c)))])) located)
     :unlocated (mapv #(select-keys % [:token :line :kind :reason])
                      (remove :met-decl criteria))
     :criteria (mapv #(dissoc % :met-decl) criteria)
     ;; retained findings: stated, not met, and kept so by the owner; never
     ;; wants, named so a closure does not read as having met them
     :retained (mapv (fn [c] {:token (:token c) :phase (first (str/split (str (:phase c)) #"\s"))
                              :line (:line c) :verdict (:verdict c) :reason :retained-finding})
                     retained)}))

(defn read-mission
  "The mission file REPO/PATH at HEAD, under CODE-ROOT."
  [code-root repo path]
  (let [{:keys [exit out]} (sh/sh "git" "-C" (str code-root "/" repo) "show" (str "HEAD:" path))]
    (when (zero? exit) out)))

(defn data-only-phases
  "Phases a mission's lifecycle data (EDN TEXT) declares :verdict-source
  :data-only: their closure is judged in data, not in a verdict line, so the
  reader emits no want for them. No checkable class can observe a keyed EDN
  value yet (C4 is line-anchored and :status sits on another line than
  :id), so they are listed OUT OF VIEW with that reason, never as wants and
  never as met. The status is copied as read, unobserved."
  [lifecycle-text]
  (vec (for [p (:phases (clojure.edn/read-string lifecycle-text))
             :when (= :data-only (:verdict-source p))]
         {:phase (:id p) :title (:title p) :status-as-read (:status p)
          :reason :data-only-no-checkable-class})))

(def closes-through-re
  #"^\*\*This phase closes only through ([A-Z][A-Z-]*)'s\b")

(defn constraints
  "Ordering constraints the mission states in its own words: a paragraph in
  a phase's section opening **This phase closes only through <PHASE>'s … is
  a :requires edge from this phase's exit token to <PHASE>'s. The edge is
  read, never inferred, and carries the line and the opening clause. A named
  phase with no exit criterion is returned under :unresolved, not dropped."
  [mission-id text]
  (let [cs (criteria mission-id text)
        token-of (into {} (for [c cs :when (= :phase-exit (:kind c))]
                            [(first (str/split (str (:phase c)) #"\s")) (:token c)]))
        lines (vec (str/split-lines text))]
    (loop [i 0 phase nil out {:requires [] :unresolved []}]
      (if (>= i (count lines))
        out
        (let [line (nth lines i) h (heading line)]
          (cond
            (and h (= 2 (:level h))) (recur (inc i) (:title h) out)
            :else
            (if-let [[_ named] (re-find closes-through-re line)]
              (let [this (token-of (first (str/split (str phase) #"\s")))
                    that (token-of named)
                    edge {:want this :requires that :by :mission-text :line (inc i)
                          :phase (first (str/split (str phase) #"\s")) :through named
                          :quote (str/trim (second (str/split (paragraph lines i) #"\*\*" 3)))}]
                (recur (inc i) phase
                       (if (and this that)
                         (update out :requires conj edge)
                         (update out :unresolved conj (dissoc edge :want :requires)))))
              (recur (inc i) phase out))))))))
