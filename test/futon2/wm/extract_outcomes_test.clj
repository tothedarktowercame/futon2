;; Tests for scripts/wm/extract-outcomes.clj, H-C-D §4 fixes E1 and E2.
;; The script is load-filed into its own namespace; it is not a lib on the
;; classpath. Run: clojure -M:test -m cognitect.test-runner -d test/futon2/wm
(ns futon2.wm.extract-outcomes-test
  (:require [clojure.test :refer [deftest is]]))

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

;; Verified spans: every cue in the output resolves to its quote in the text,
;; and a cue that does not is caught (the refusal the script exits 2 on).
(deftest every-cue-resolves
  (let [text (str e1-text "\n" e2-text)
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
