(ns futon2.aif.served-by-reading
  "The read step's half of a served-by link beyond M-futon-seams (PROOF-2a
  H-C-reach; M-wm-wiring row 2). A seat proposes a link by QUOTING the text:

    {:instance n :outcome :o-k :artefact s | {:outcome-phrase s1 :want-phrase s2}
     :want-quote \"...\" :outcome-quote \"...\"}

  Seats count code points unreliably, so this namespace turns the quotes into
  the spans the verifier takes (code points, zero-based, end-exclusive, the
  unit every :via span uses) and hands the proposal, pinned to the text's
  sha256, to `verify-proposed-link` (futon2 scripts/wm/extract-outcomes.clj,
  claude-13). The verifier's pass or refusal comes back unchanged.

  Placement:
    want side     the quote must occur exactly once inside instance n's
                  section (lines of `instance-sections`); an occurrence
                  elsewhere in the file does not count.
    outcome side  the span is one of outcome :o-k's own cue spans, the one
                  whose text contains the quote; never recomputed.
  A quote that cannot be placed is the read step's own typed refusal, before
  the verifier sees anything:
    {:status :refused :reason :quote-absent | :quote-ambiguous :side :want | :outcome
     :detail {...}}
  An unknown instance or outcome is not placed here: the verifier's own
  :instance-unknown / :outcome-unknown answers it, so the rule has one home.

  VERIFY is passed in (the script has no ns form and cannot be required yet;
  row 2(b)). Pure apart from calling it."
  (:require [clojure.string :as str])
  (:import [java.security MessageDigest]))

(defn sha256
  "Hex sha256 of TEXT's UTF-8 bytes (the verifier's :text-sha256 rule)."
  [^String text]
  (let [d (.digest (MessageDigest/getInstance "SHA-256") (.getBytes text "UTF-8"))]
    (apply str (map #(format "%02x" %) d))))

(defn- line-of [^String text i]
  (inc (count (filter #(= \newline %) (subs text 0 i)))))

(defn- cp-span [^String text i j]
  [(.codePointCount text 0 i) (.codePointCount text 0 j)])

(defn- cp-subs [^String text [a b]]
  (subs text (.offsetByCodePoints text 0 a) (.offsetByCodePoints text 0 b)))

(defn- occurrences
  "UTF-16 start indices of every occurrence of Q in TEXT."
  [^String text ^String q]
  (loop [from 0 acc []]
    (let [i (str/index-of text q from)]
      (if (nil? i) acc (recur (inc i) (conj acc i))))))

(defn- refuse [reason side detail]
  {:status :refused :reason reason :side side :detail detail})

(defn place-want
  "Code-point span of WANT-QUOTE inside SECTION's lines, or a typed refusal."
  [^String text {:keys [instance lines]} ^String want-quote]
  (let [[lo hi] lines
        all (occurrences text want-quote)
        inside (filterv #(and (<= lo (line-of text %))
                              (<= (line-of text (max % (dec (+ % (count want-quote))))) hi))
                        all)]
    (case (count inside)
      1 (cp-span text (first inside) (+ (first inside) (count want-quote)))
      0 (refuse :quote-absent :want {:instance instance :section-lines lines :quote want-quote
                                     :occurrences-elsewhere (count all)})
      (refuse :quote-ambiguous :want {:instance instance :section-lines lines :quote want-quote
                                      :occurrences (count inside)}))))

(defn place-outcome
  "The one cue span of OUTCOME whose text contains OUTCOME-QUOTE, or a typed
  refusal."
  [^String text outcome ^String outcome-quote]
  (let [hits (filterv #(str/includes? (cp-subs text (:span %)) outcome-quote) (:cues outcome))]
    (case (count hits)
      1 (:span (first hits))
      0 (refuse :quote-absent :outcome {:outcome (:id outcome) :quote outcome-quote
                                        :cue-spans (mapv :span (:cues outcome))})
      (refuse :quote-ambiguous :outcome {:outcome (:id outcome) :quote outcome-quote
                                         :cue-spans (mapv :span hits)}))))

(defn proposal
  "The verifier-ready proposal for quote proposal Q over TEXT, or the read
  step's typed refusal. An unknown instance or outcome gets empty spans so the
  verifier's own reason answers it."
  [^String text isecs outcomes {:keys [instance outcome artefact want-quote outcome-quote]}]
  (let [sec (first (filter #(= instance (:instance %)) isecs))
        o (first (filter #(= outcome (:id %)) outcomes))
        want (if sec (place-want text sec want-quote) [0 0])
        out (cond (map? want) nil
                  o (place-outcome text o outcome-quote)
                  :else [0 0])]
    (cond
      (map? want) want
      (map? out) out
      :else {:instance instance :outcome outcome :artefact artefact
             :want-span want :outcome-span out :text-sha256 (sha256 text)})))

(defn read-link
  "Place Q's quotes and verify: the verifier's row (with :via :basis
  :proposed-verified) or refusal, unchanged; or the read step's own refusal."
  [verify ^String text isecs outcomes q]
  (let [p (proposal text isecs outcomes q)]
    (if (= :refused (:status p)) p (verify text isecs outcomes p))))
