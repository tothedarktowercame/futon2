;; Tests for scripts/wm/extract-outcomes.clj, H-C-D §4 fixes E1 and E2.
;; The script is load-filed into its own namespace; it is not a lib on the
;; classpath. Run: clojure -M:test -m cognitect.test-runner -d test/futon2/wm
(ns futon2.wm.extract-outcomes-test
  (:require [clojure.edn :as edn]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]))

(def script-ns 'extract-outcomes-under-test)

(binding [*ns* (create-ns script-ns)]
  (clojure.core/refer-clojure)
  (load-file "scripts/wm/extract-outcomes.clj"))

(defn- f [v] (ns-resolve script-ns v))

(def extract (f 'extract))
(def consolidate (f 'consolidate))
(def verify (f 'verify))
(def cp-subs (f 'cp-subs))
(def read-reference (f 'read-reference))
(def cue-rules @(f 'cue-rules))
(def clause-verdict (f 'clause-verdict))
(def filter-outcomes (f 'filter-outcomes))
(def headings (f 'headings))
(def instance-sections (f 'instance-sections))
(def sha256 (f 'sha256))

(defn outcomes-of
  "extract + consolidate over text; section-of maps line -> instance number."
  [text section-of]
  (consolidate (extract text (fn [l] (get section-of l)) {})))

;; E1: one outcome stated three times is one outcome carrying all three spans.
(def e1-text
  (str "Rob's closing ask for the future: a seam in the core, so a second client could reuse it.\n"
       "\n"
       "He is not asking for adapters now, but wants a seam inserted so later implementations are less painful.\n"
       "\n"
       "Rob asked for the seam packaged so he could reuse it.\n"))

(deftest e1-same-outcome-three-cues
  (let [os (outcomes-of e1-text {3 7})] ;; middle sentence sits in instance 7's section
    (is (= 1 (count os)) "three statements of one outcome consolidate to one row")
    (is (= 3 (count (:cues (first os)))) "the outcome carries ALL its spans")
    (is (= [1 3 5] (mapv #(first (:cue %)) (:cues (first os)))) "first span first")
    (is (= "Rob" (:whose (first os))))
    (is (= 7 (:instance (first os))))))

;; E1 bad case: two sentences naming DIFFERENT instances are never merged,
;; even when they share party and artefact.
(def e1-veto-text
  (str "Rob could not use the adapter in instance 4.\n"
       "\n"
       "Rob could not use the adapter in instance 5.\n"))

(deftest e1-different-instances-not-merged
  (let [os (outcomes-of e1-veto-text {})]
    (is (= 2 (count (mapcat :cues os))) "BOTH sentences are cued")
    (is (every? #(= 1 (count (:cues %))) os) "each outcome carries its own sentence's cue")
    (is (= 2 (count os)) "same party-and-artefact but different named instances: no merge")))

;; E1 bad case, mirror: same distinctive artefact, different named parties.
(def e1-parties-text
  (str "Rob wants a seam packaged for reuse.\n"
       "\n"
       "Joe wants a seam packaged for reuse.\n"))

(deftest e1-different-parties-not-merged
  (let [os (outcomes-of e1-parties-text {})]
    (is (= 2 (count os)) "same artefact, different parties: no merge")))

;; E2: the consequence voice is cued, as data, when the sentence names an
;; artefact. (M-futon-seams L21/L22 shapes.)
(def e2-text
  (str "- *Cost of not:* every later implementation must impersonate the first.\n"
       "\n"
       "Two implementations that have already drifted cost more to unify than one implementation plus a stub.\n"))

(deftest e2-consequence-voice-cued
  (let [os (outcomes-of e2-text {})
        rules (set (mapcat :rules os))]
    (is (= 2 (count os)) "two distinct consequence outcomes, not merged")
    (is (some #(contains? rules %)
              [:cue/must-impersonate :cue/every-later-must :cue/cost-of])
        "L21 shape is cued by the consequence voice")
    (is (contains? (set (:rules (second os))) :cue/cost-of)
        "L22 shape (cost more to unify) is cued")
    (is (every? #(= :artefact (:requires %))
                (filter #(#{:cue/must-impersonate :cue/every-later-must
                            :cue/otherwise :cue/cost-of} (:id %))
                        cue-rules))
        "every consequence rule carries the artefact gate as data")))

;; E2 bad case: a consequence-voice sentence naming no artefact is not cued.
(def e2-no-artefact-text
  "Otherwise, everything falls apart and we must all work harder.\n")

(deftest e2-consequence-without-artefact-not-cued
  (is (= [] (extract e2-no-artefact-text (constantly nil) {}))
      "no artefact named: the consequence voice does not cue"))

;; E3: the capability contrast -- "with X you can V; with Y you must W" --
;; is cued, as data, when the sentence names an artefact (M-futon-seams L124).
(def e3-text
  (str "With hardcoded code you can grep for the literal; with a hardcoded prompt you must match natural language at runtime.\n"
       "\n"
       "With roles that is a property of the binding, not a string comparison in the dispatcher.\n"))

(deftest e3-capability-contrast-cued
  (let [os (outcomes-of e3-text {3 4})
        rules (set (mapcat :rules os))]
    (is (contains? rules :cue/contrast-with-you-can)
        "the two-arm contrast is cued")
    (is (contains? rules :cue/contrast-property-not)
        "the negated-capability contrast is cued")
    (is (every? #(= :artefact (:requires %))
                (filter #(#{:cue/contrast-with-you-can :cue/contrast-lets-you
                            :cue/contrast-property-not} (:id %))
                        cue-rules))
        "every capability-contrast rule carries the artefact gate as data")
    (is (= [] (verify e3-text os)) "every E3 cue resolves")))

;; E3 bad case: a contrast sentence naming no artefact or capability is not cued.
(def e3-no-artefact-text
  "With patience you can wait for the bus; with a car you must find parking.\n")

(deftest e3-contrast-without-artefact-not-cued
  (is (= [] (extract e3-no-artefact-text (constantly nil) {}))
      "no artefact named: the capability contrast does not cue"))

;; E3 bad case: a single-arm capability sentence is a method statement
;; (H-C-D E4), not an outcome. What keeps it out is structural: every E3 rule
;; requires BOTH arms of the contrast (\"you can ... ; with ... you must\" /
;; \"lets you ... where ... cannot\" / \"property of X, not a Y\"), and this
;; sentence has only one arm, so no E3 rule's regex matches at all.
(def e3-method-text
  "With the script you can regenerate the page.\n")

(deftest e3-method-statement-not-cued
  (is (= [] (extract e3-method-text (constantly nil) {}))
      "one arm only: the two-arm contrast shape keeps the method statement out"))

;; Verified spans: every cue in the output resolves to its quote in the text,
;; and a cue that does not is caught (the refusal the script exits 2 on).
(deftest every-cue-resolves
  (let [text (str e1-text "\n" e2-text "\n" e3-text)
          os (outcomes-of text {3 7})]
    (is (= [] (verify text os)) "every cue of every outcome resolves")
    (let [broken (assoc-in os [0 :cues 0 :cue] [999 999])]
      (is (seq (verify text broken)) "a wrong line number is a refusal"))))

(deftest duplicated-quote-refuses
  (let [dup "Rob wants a seam packaged for reuse.\n\nRob wants a seam packaged for reuse.\n"
        os (outcomes-of dup {})]
    (is (seq (verify dup os))
        "a quote occurring twice cannot identify a span: refusal")))

;; Offset unit (H-C-D R1): every cue carries a :span in Unicode code points,
;; zero-based, end-exclusive, and the span resolves to the quote. A multi-byte
;; character before the cue makes the byte reading wrong.
(def span-text "\u2014 is an em dash. Rob wants a seam packaged for reuse.\n")

(deftest cue-spans-are-codepoint-offsets
  (let [os (outcomes-of span-text {})
        [a b] (:span (first (:cues (first os))))
        quote (:quote (first (:cues (first os))))]
    (is (= quote "Rob wants a seam packaged for reuse."))
    (is (= [17 53] [a b]) "the em dash counts once, not three times (bytes)")
    (is (= quote (cp-subs span-text a b)) "the span resolves to the quote")
    (is (= [] (verify span-text os)))
    (is (not= a (count (.getBytes (subs span-text 0 a) "UTF-8")))
        "a byte reading of the same span would land somewhere else")))

;; A reference C that declares no offset unit, or a different one, is refused
;; with a typed reason, never reinterpreted.
(deftest reference-offset-unit-required
  (let [tmp (java.io.File/createTempFile "ref" ".edn")]
    (spit tmp "{:schema :x/c-v1 :outcomes {}}")
    (is (= :reference-offset-unit-mismatch (:refused (read-reference (.getPath tmp))))
        "absent :offset-unit refuses")
    (is (= :absent (:found (read-reference (.getPath tmp)))))
    (spit tmp "{:schema :x/c-v1 :offset-unit :bytes :outcomes {}}")
    (is (= :bytes (:found (read-reference (.getPath tmp))))
        "a different unit refuses, naming what it found")
    (spit tmp "{:schema :x/c-v1 :offset-unit :unicode-codepoints-zero-based-end-exclusive :outcomes {}}")
    (is (= {} (:outcomes (read-reference (.getPath tmp))))
        "the declared unit passes")
    (.delete tmp)))

;; ---------------------------------------------------------------- E4/E5
;; The four-clause filter (H-C-DEF §2), live-pinned to the mission at sha256
;; d13c5cfe9e9b19b445bd5bb73507f286a9e5ff3b478a1c5bc6a2250d70c6f6fd -- the sha
;; the H-C-DEF §3 classification was taken at, and the sha the reference
;; mission-C.edn declares. A mission that has moved is a different extraction,
;; not a failing one: the pin fails loudly so the test is re-pinned, not
;; silently re-read.

(def mission-path "../futon3c/holes/missions/M-futon-seams.md")
(def mission-sha-pinned "d13c5cfe9e9b19b445bd5bb73507f286a9e5ff3b478a1c5bc6a2250d70c6f6fd")

(defn mission-filter
  "The -main pipeline (headings -> sections -> extract -> consolidate -> ids
   -> filter-outcomes) over text, without cascades or a reference."
  [text]
  (let [hs (headings text)
        isecs (instance-sections hs (count (str/split-lines text)))
        section-of (fn [l] (:instance (first (filter #(<= (first (:lines %)) l (second (:lines %))) isecs))))
        cs (mapv #(assoc %1 :id (keyword (str "o-" (inc %2))))
                 (consolidate (extract text section-of {}))
                 (range))]
    (filter-outcomes text cs)))

(defn- quoted?
  "Does some cue of row quote a sentence containing s?"
  [row s]
  (some #(str/includes? (:quote %) s) (:cues row)))

(deftest e4e5-mission-pin
  (let [text (slurp mission-path)]
    (is (= mission-sha-pinned (sha256 text))
        "the mission moved: re-pin the fixture and re-check the classification")))

(deftest e4e5-all-six-reference-outcomes-admitted
  (let [text (slurp mission-path)
        {:keys [outcomes]} (mission-filter text)]
    (is (= mission-sha-pinned (sha256 text)))
    (doseq [q ["Rob's closing ask for the future: a seam in the Emacs layer"
               "every later implementation must impersonate the first"
               "Joe could not use Rob's memory MCP because it is neo4j-specific"
               "Rob could not use futon3c's agent roles because the code reads the provider out of the agent id"
               "Two implementations that have already drifted cost more to unify"
               "With hardcoded code you can grep for the literal"]]
      (is (some #(quoted? % q) outcomes)
          (str "reference outcome still emitted: " (subs q 0 (min 50 (count q))))))))

(deftest e4e5-ten-extras-rejected-or-facet
  ;; One assertion per extra of H-C-DEF §3, quoting the sentence, on the clause
  ;; it was classified under. Ids are the extractor's at the pinned sha.
  (let [text (slurp mission-path)
        {:keys [outcomes rejected facets]} (mission-filter text)
        rejected? (fn [clause q]
                    (some #(and (= clause (:clause %)) (quoted? % q)) rejected))
        in-outcomes? (fn [q] (some #(quoted? % q) outcomes))]
    (is (= 6 (count outcomes)) "exactly the 6 reference outcomes are admitted")
    (is (= 9 (count rejected)))
    (is (= 1 (count facets)))
    ;; :o-6 — the E1-residual: a finer facet of :rob-can-run-the-stack (:o-4),
    ;; not an outcome and not a rejection.
    (let [facet (first (filter #(quoted? % "With roles that is a property of the binding, not a string comparison in the dispatcher") facets))]
      (is facet "the facet cue is emitted under :facets")
      (is (= :o-4 (:facet-of facet)) "facet-of the instance-4 outcome (:rob-can-run-the-stack)")
      (is (not (in-outcomes? "property of the binding")) "never under :outcomes"))
    ;; the four evidence extras (E5) fail clause 1: facts true before the mission
    (is (rejected? 1 "Rob had to add configurable room and agent names") ":o-7 clause 1")
    (is (rejected? 1 "The Python one already had to reimplement sentence splitting to match the elisp.") ":o-10 clause 1")
    (is (rejected? 1 "**In:** a person who could not do something") ":o-14 clause 1")
    (is (rejected? 1 "`exemplar/check-ledger.edn` (claude-10)") ":o-16 clause 1")
    ;; the method extras (E4): clause 3 where the check is artefact inspection,
    ;; clause 4 where the mission completing discharges them
    (is (rejected? 3 "The intended end state is the abstract path absorbing the hardcoded one and the flag disappearing.") ":o-9 clause 3")
    (is (rejected? 3 "What this mission develops is a **capability**") ":o-13 clause 3")
    (is (rejected? 3 "a judgement must be recorded in a form something else can") ":o-15 clause 3")
    (is (rejected? 4 "the exit is: the interface is declared") ":o-11 clause 4")
    (is (rejected? 4 "**Do not** mint the abstraction and leave the hardcoded path alive indefinitely.") ":o-12 clause 4")
    ;; nothing rejected is also admitted
    (doseq [q ["had to add configurable room" "intended end state" "already had to reimplement"
               "the exit is:" "**Do not** mint" "What this mission develops"
               "**In:** a person" "must be recorded in a form" "check-ledger.edn"]]
      (is (not (in-outcomes? q)) (str "rejected sentence never under :outcomes: " q)))))

(deftest e4e5-bad-case-outcome-like-method-sentence
  ;; A method sentence with outcome-like wording ("the stack will then have
  ;; one registry") is rejected on the world-check clause (clause 3), not
  ;; admitted: its check is inspecting the artefact the mission leaves behind.
  (let [text "The intended end state is that the stack will then have one registry.\n"
        {:keys [outcomes rejected]} (mission-filter text)]
    (is (= [] outcomes) "not admitted")
    (is (= 1 (count rejected)))
    (is (= 3 (:clause (first rejected))) "rejected on the world-check clause")
    (is (= :clause3/enactment-end-state (:reason (first rejected))))))

(deftest e4e5-clause-verdict-unit
  ;; The clause rules as data, one probe per clause, cue-driven.
  (is (nil? (clause-verdict {:rules [:cue/could-not] :quote "Rob could not use the adapter." :whose "Rob"}))
      "a party's blocked discrepancy passes all four clauses")
  (is (= 1 (:clause (clause-verdict {:rules [:cue/had-to] :quote "Rob had to add a shim." :whose "Rob"})))
      "clause 1: past-tense report with no purpose clause")
  (is (nil? (clause-verdict {:rules [:cue/had-to :cue/so-party-could]
                             :quote "Configs had to be changed so the role could be served by Codex." :whose "Rob"}))
      "a purpose clause lifts the had-to cue (o-4's second cue)")
  (is (= 2 (:clause (clause-verdict {:rules [:cue/wants-a]
                                     :quote "One wants the thing to be better somehow." :whose :the-mission})))
      "clause 2: no party, no artefact, no discrepancy named")
  (is (= 3 (:clause (clause-verdict {:rules [:cue/recorded-form]
                                     :quote "A judgement must be recorded in a form." :whose :the-mission})))
      "clause 3: constraint on the mission's own artefacts")
  (is (= 4 (:clause (clause-verdict {:rules [:cue/do-not]
                                     :quote "**Do not** leave the old path alive." :whose :the-mission})))
      "clause 4: an imperative discharged by enactment"))


;; ---------------------------------------------------------------- E6
;; Served-by by shared named artefact (H-C-DEF §4), live-pinned at the same
;; mission sha as the E4/E5 block. The 13 scorable reference mappings are
;; mission-C.edn :served-by minus the 14th row (instance 8, :status
;; :prospective -- no cascade, unscorable by its own note). Reference outcome
;; names are translated to the extractor's ids at the pinned sha (o-1 =
;; :vs-code-implementation-possible, o-2 = :second-implementation-is-cheap,
;; o-3 = :joe-can-use-robs-work, o-4 = :rob-can-run-the-stack, o-5 =
;; :no-drifting-forks, o-8 = :coupling-visible-to-tooling; verified by the
;; :reference-comparison rows).

(def artefact-links (f 'artefact-links))
(def coupling-artefacts @(f 'coupling-artefacts))
(def cascade-wants (f 'cascade-wants))

(def cascade-dir "../futon3c/holes/labs/M-futon-seams/proto")

(def reference-served-by
  ;; [instance want outcome-id]: the 13 scorable mappings, ids at the pinned sha
  [[4 :prefix-routing-retired :o-4]
   [4 :caller-converted :o-2]
   [4 :redirect-test :o-2]
   [5 :protocol-declared :o-2]
   [5 :impersonation-retired :o-4]
   [5 :adapter-conformance-test :o-5]
   [6 :one-authority :o-5]
   [6 :one-authority :o-8]
   [6 :flag-retired :o-5]
   [7 :record-schema-declared :o-1]
   [7 :record-schema-declared :o-2]
   [7 :writers-converted :o-5]
   [7 :divergence-test :o-5]])

(defn mission-served
  "The -main pipeline over the pinned mission WITH cascades: admitted outcomes,
   artefact-links, and the served-by rows as -main emits them."
  [text]
  (let [hs (headings text)
        isecs (instance-sections hs (count (str/split-lines text)))
        section-of (fn [l] (:instance (first (filter #(<= (first (:lines %)) l (second (:lines %))) isecs))))
        cs (mapv #(assoc %1 :id (keyword (str "o-" (inc %2))))
                 (consolidate (extract text section-of {}))
                 (range))
        {:keys [outcomes]} (filter-outcomes text cs)
        wants (cascade-wants cascade-dir)
        links (artefact-links text isecs outcomes)
        served (vec (for [s isecs
                          :let [ws (get wants (:instance s))
                                ls (filterv #(= (:instance s) (:instance %)) links)]]
                      (if (nil? ws)
                        {:instance (:instance s) :absent :no-cascade}
                        {:instance (:instance s)
                         :wants ws
                         :serves (mapv (fn [l] {:outcome (:outcome l) :via (:via l)}) ls)
                         :basis :shared-named-artefact})))]
    {:outcomes outcomes :links links :served served}))

(deftest e6-served-by-links
  (let [text (slurp mission-path)
        _ (is (= mission-sha-pinned (sha256 text)))
        {:keys [links]} (mission-served text)
        link-set (set (map (juxt :instance :outcome) links))]
    (is (= #{[4 :o-4] [5 :o-2] [5 :o-5] [6 :o-5] [6 :o-8] [7 :o-1] [7 :o-2] [7 :o-5]}
           link-set)
        "exactly the shared-artefact links, one per (instance, outcome)")
    (doseq [{:keys [via]} links]
      (is (some #(= (:artefact via) (:id %)) coupling-artefacts)
          "every link names a predeclared coupling artefact")
      (is (keyword? (:direction via)) "every mention sentence carried a direction verb")
      (let [entry (first (filter #(= (:artefact via) (:id %)) coupling-artefacts))
            mention-sentence (apply cp-subs text (:want-span via))
            obstacle-quote (apply cp-subs text (:outcome-span via))]
        (is (some #(re-find % mention-sentence) (:mention-res entry))
            ":want-span resolves to a sentence naming the artefact")
        (is (some #(re-find % obstacle-quote) (:obstacle-res entry))
            ":outcome-span resolves to the cue carrying the artefact as obstacle")))))

(deftest e6-no-drifting-forks-five-links
  ;; All five reference mappings to :no-drifting-forks (:o-5) are reproduced,
  ;; each via a named artefact.
  (let [text (slurp mission-path)
        {:keys [served]} (mission-served text)
        o5-links (into {} (keep (fn [row]
                                  (when-let [sv (first (filter #(= :o-5 (:outcome %)) (:serves row)))]
                                    [(:instance row) (get-in sv [:via :artefact])]))
                                served))]
    (is (= {5 :second-implementation-kept-in-sync
            6 :second-implementation-kept-in-sync
            7 :second-implementation-kept-in-sync}
           o5-links)
        "instances 5, 6 and 7 each link :o-5 via :second-implementation-kept-in-sync")
    (let [o5-mappings (filter #(= :o-5 (nth % 2)) reference-served-by)
          reproduced (filter (fn [[i _w _o]] (contains? o5-links i)) o5-mappings)]
      (is (= 5 (count reproduced))
          "all five :no-drifting-forks mappings (5/:adapter-conformance-test, 6/:one-authority, 6/:flag-retired, 7/:writers-converted, 7/:divergence-test)"))))

(deftest e6-former-containment-false-positives-get-no-link
  ;; At HEAD's section-containment basis, instance 4's row served :o-4, which
  ;; at want granularity linked :caller-converted and :redirect-test to :o-4 --
  ;; the reference maps both to :o-2. Under the artefact basis there are no
  ;; per-want link claims at all (:via names an artefact, never a want), and
  ;; instance 4 links no outcome its section names no artefact for.
  (let [text (slurp mission-path)
        {:keys [served]} (mission-served text)
        row4 (first (filter #(= 4 (:instance %)) served))]
    (is (= [:o-4] (mapv :outcome (:serves row4)))
        "instance 4 links :o-4 only; :caller-converted/:redirect-test get no link to their reference outcome :o-2")
    (doseq [row served, sv (:serves row)]
      (is (not (contains? (:via sv) :want))
          "no per-want link is claimed: per-want lexical grounding was dropped as too fragile"))))

(deftest e6-instance-8-no-cascade-and-joe-unlinked
  (let [text (slurp mission-path)
        {:keys [links served]} (mission-served text)
        linked (set (map :outcome links))]
    (is (= {:instance 8 :absent :no-cascade}
           (first (filter #(= 8 (:instance %)) served)))
        "instance 8 stays {:absent :no-cascade}")
    (is (not (contains? linked :o-3))
        ":joe-can-use-robs-work (:o-3) is unlinked {:absent :no-shared-artefact}: no coupling-artefact entry names it and instance 8 has no cascade")))

(deftest e6-reference-recall
  ;; Counting method: a reference mapping (i, w, o) is reproduced when instance
  ;; i's served-by row links outcome o (all wants of i link, per the basis).
  (let [text (slurp mission-path)
        {:keys [served]} (mission-served text)
        links (into #{} (for [row served, sv (:serves row)] [(:instance row) (:outcome sv)]))
        hits (filterv (fn [[i _w o]] (contains? links [i o])) reference-served-by)
        recall (str (count hits) "/" (count reference-served-by))]
    (println "E6 served-by reference recall:" recall)
    (is (>= (count hits) 3)
        (str "recall " recall " is at least the measured 3/13 containment baseline"))
    (is (= 10 (count hits)) "the design's expected 10/13")
    ;; the three honest misses, asserted absent -- not forced to link
    (is (not (contains? links [4 :o-2])) "miss: 4/:caller-converted, 4/:redirect-test -> :o-2 (no artefact mention in section 4)")
    (is (not (contains? links [5 :o-4])) "miss: 5/:impersonation-retired -> :o-4 (section 5 does not name provider-in-id)")))

(deftest e6-no-direction-verb-no-link
  ;; Bad case: a section naming the artefact without treating it as acted on
  ;; (no direction verb) does not link -- presence is not a serve.
  ;; H-C-REACH-I: instance sections need an instances anchor, so the fixture
  ;; carries one (it had none, and was read by the whole-file fallback).
  (let [text (str "## The instances\n\n### 4. Prompts\n\n"
                  "With hardcoded code you can grep for the literal; with a hardcoded prompt you must match natural language at runtime.\n\n"
                  "The hardcoded prompt sits in the call site, as it always has.\n")
        hs (headings text)
        isecs (instance-sections hs (count (str/split-lines text)))
        section-of (fn [l] (:instance (first (filter #(<= (first (:lines %)) l (second (:lines %))) isecs))))
        cs (mapv #(assoc %1 :id (keyword (str "o-" (inc %2))))
                 (consolidate (extract text section-of {}))
                 (range))
        {:keys [outcomes]} (filter-outcomes text cs)]
    (is (= 1 (count outcomes)) "the contrast outcome is admitted")
    (is (= [] (artefact-links text isecs outcomes))
        "the section names the artefact but carries no direction verb: no link")
    (let [text2 (str text "\nThe intended end state is the abstract path absorbing the hardcoded one.\n")
          hs2 (headings text2)
          isecs2 (instance-sections hs2 (count (str/split-lines text2)))
          section-of2 (fn [l] (:instance (first (filter #(<= (first (:lines %)) l (second (:lines %))) isecs2))))
          cs2 (mapv #(assoc %1 :id (keyword (str "o-" (inc %2))))
                    (consolidate (extract text2 section-of2 {}))
                    (range))
          outcomes2 (:outcomes (filter-outcomes text2 cs2))
          links2 (artefact-links text2 isecs2 outcomes2)]
      (is (= 1 (count links2)) "adding a direction-verb mention links it")
      (is (= :absorb (:direction (:via (first links2))))))))

;; ---------------------------------------------------------- H-C-REACH-I
;; Reader precision (H-C-REACH-D, futon2 b10589a9): no instance units without
;; an instances anchor; heading, fenced-block and merge-note spans rejected by
;; shape. Live texts are read with git show at the shas the target-field
;; fixture (6d2b39a7) recorded, and pinned by sha256.

(def shape-verdict (f 'shape-verdict))
(def cue-shapes @(f 'cue-shapes))

(defn- git-show [repo rev path]
  (let [r (shell/sh "git" "-C" (str "../" repo) "show" (str rev ":" path))]
    (when (zero? (:exit r)) (:out r))))

(def close-s6 (delay (git-show "futon5a" "1e4ab8d7" "holes/excursions/E-close-S6.md")))
(def close-s6-sha "5ebc8363280017523d72dd1e55052c1ce496b48c89deb0cdaaca6cfc02a68798")
(def apm (delay (git-show "futon3c" "acdd14f4" "holes/missions/M-apm-demonstration.md")))
(def apm-sha "0abc8484a418f6ea4cd42573dd2942ce0b28571fb4216043664c4e6c89e7788e")

(defn- run-main-on
  "-main's EDN output for a text, via a temp file (no cascades)."
  [text]
  (let [tmp (java.io.File/createTempFile "h-c-reach-i" ".md")]
    (spit tmp text)
    (let [out (with-out-str ((f '-main) (.getPath tmp)))]
      (.delete tmp)
      (edn/read-string out))))

(deftest control-m-futon-seams-unchanged
  (let [text (slurp mission-path)
        _ (is (= mission-sha-pinned (sha256 text)))
        hs (headings text)
        isecs (instance-sections hs (count (str/split-lines text)))
        section-of (fn [l] (:instance (first (filter #(<= (first (:lines %)) l (second (:lines %))) isecs))))
        cs (mapv #(assoc %1 :id (keyword (str "o-" (inc %2))))
                 (consolidate (extract text section-of {})) (range))
        {:keys [outcomes rejected facets]} (filter-outcomes text cs)
        {:keys [links served]} (mission-served text)]
    (is (= [6 9 1] [(count outcomes) (count rejected) (count facets)]))
    (is (= 8 (count links)))
    (is (not-any? #(= :shape (:clause %)) rejected) "no shape rejection on the control")
    (is (= [1 2 3 4 5 6 7 8] (mapv :instance served)))
    (is (= [:no-cascade :no-cascade :no-cascade] (mapv :absent (take 3 served))))))

(def pre-change-rev "a31f9cee")

(defn- main-output
  "-main's parsed output for the script at REV (nil = the working file) on
   the pinned mission with its cascades."
  [rev]
  (let [nsym (symbol (str "extract-outcomes-" (or rev "head")))
        path (if rev
               (let [tmp (java.io.File/createTempFile "extract-outcomes-" ".clj")]
                 (spit tmp (:out (shell/sh "git" "show" (str rev ":scripts/wm/extract-outcomes.clj"))))
                 (.getPath tmp))
               "scripts/wm/extract-outcomes.clj")]
    (binding [*ns* (create-ns nsym)]
      (clojure.core/refer-clojure)
      (load-file path))
    (edn/read-string
     (with-out-str ((ns-resolve nsym '-main) mission-path "--cascades" cascade-dir)))))

(deftest control-m-futon-seams-output-identical-to-pre-change
  (let [before (main-output pre-change-rev)
        after (main-output nil)]
    (is (= mission-sha-pinned (get-in after [:mission :sha256])))
    (is (= (:served-by before) (:served-by after)) "served-by rows byte-identical")
    (is (= before after) "the whole -main output is unchanged on the control")))

(deftest anchor-1-numbered-headings-without-anchor-are-not-instances
  (let [text @close-s6
        _ (is (= close-s6-sha (sha256 text)))
        hs (headings text)
        numbered (filter #(and (= 3 (:level %)) (re-find #"^\d+\.\s" (:title %))) hs)]
    (is (= 4 (count numbered)) "the protocol steps 1-4 are numbered ### headings")
    (is (= [] (instance-sections hs (count (str/split-lines text)))))
    (is (= {:absent :no-instances-anchor} (:served-by (run-main-on text))))))

(deftest anchor-2-the-same-file-with-an-instances-heading
  (let [lines (vec (str/split-lines @close-s6))
        i (.indexOf ^java.util.List lines "### 1. Freeze the preregistration")
        text (str/join "\n" (concat (subvec lines 0 i) ["## Instances" ""] (subvec lines i)))
        units (instance-sections (headings text) (count (str/split-lines text)))]
    (is (pos? i))
    (is (= [1 2 3 4] (mapv :instance units)))
    (is (vector? (:served-by (run-main-on text))))))

(deftest shape-1-m-apm-demonstration
  (let [text @apm
        _ (is (= apm-sha (sha256 text)))
        cs (mapv #(assoc %1 :id (keyword (str "o-" (inc %2))))
                 (consolidate (extract text (constantly nil) {})) (range))
        before (filter-outcomes
                text cs)
        shape-rej (filter #(= :shape (:clause %)) (:rejected before))
        reasons (set (map :reason shape-rej))
        quote-of (fn [r] (:quote (first (:cues r))))]
    (is (contains? reasons :shape/heading))
    (is (contains? reasons :shape/fenced-block))
    (is (contains? reasons :shape/merge-note))
    (is (some #(str/starts-with? (quote-of %) "### First, the cost of tuning role cards") shape-rej))
    (is (some #(str/starts-with? (quote-of %) "```clojure\n:reg/escalation") shape-rej))
    (is (some #(str/starts-with? (quote-of %) "Merged (`see log`)") shape-rej))
    ;; 13 admitted before H-C-REACH-I (H-C-REACH-D §1); each shape rejection
    ;; removes exactly one
    (is (= 13 (+ (count (:outcomes before)) (count shape-rej))))
    (doseq [o (:outcomes before)]
      (is (not-any? #(shape-verdict text %) (:cues o))
          "no admitted outcome passes on a shape-matching cue alone"))))

(deftest shape-2-prose-containing-hash-or-backtick-is-not-rejected
  (is (nil? (shape-verdict "x" {:quote "Rob wants a seam so issue #12 in `roles.clj` could close." :span [0 1]})))
  (is (nil? (shape-verdict "x" {:quote "The branch was merged into the plan as a note." :span [0 1]})))
  (is (= :shape/heading (:reason (shape-verdict "x" {:quote "## A heading" :span [0 1]}))))
  (let [text "Prose.\n\n```\nso Rob could run it\n```\n"
        a (.codePointCount text 0 (str/index-of text "so Rob"))]
    (is (= :shape/fenced-block (:reason (shape-verdict text {:quote "so Rob could run it" :span [a (+ a 5)]})))
        "a span inside a fence is rejected by position")))
