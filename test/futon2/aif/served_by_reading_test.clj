(ns futon2.aif.served-by-reading-test
  "Quotes to spans for served-by proposals, against the live M-futon-seams
  text (its sha is checked by the verifier's :text-sha256 pin on every call).
  The extractor is required as futon2.wm.extract-outcomes (row 2(b))."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.served-by-reading :as sbr]
            [futon2.wm.extract-outcomes]
            [futon2.aif.flight]
            [futon2.aif.flight-runner]))

(defn- f [v] @(ns-resolve 'futon2.wm.extract-outcomes v))

(def mission-path "../futon3c/holes/missions/M-futon-seams.md")
;; The text is read live from futon3c, so the warrant on this namespace does
;; not pin it; this pin does (claude-8, review of d7deccba). Content sha of
;; M-futon-seams.md since futon3c 3f5f44dd, the same pin extract_outcomes_test
;; carries.
(def mission-sha-pinned "d13c5cfe9e9b19b445bd5bb73507f286a9e5ff3b478a1c5bc6a2250d70c6f6fd")

(defn- context [text]
  (let [isecs ((f 'instance-sections) ((f 'headings) text) (count (str/split-lines text)))
        section-of (fn [l] (:instance (first (filter #(<= (first (:lines %)) l (second (:lines %))) isecs))))
        cs (mapv #(assoc %1 :id (keyword (str "o-" (inc %2))))
                 ((f 'consolidate) ((f 'extract) text section-of {})) (range))]
    {:text text :isecs isecs :outcomes (:outcomes ((f 'filter-outcomes) text cs))}))

(defn- read-link [{:keys [text isecs outcomes]} q]
  (sbr/read-link (f 'verify-proposed-link) text isecs outcomes q))

(def roles-sentence
  "Roles resolve to seats; seats declare provider and availability; code asks for a role instead of pattern-matching an id.")

(def reading-route
  ;; 4/:caller-converted -> :o-2, the link the vocabulary never reaches
  ;; (futon2.wm.extract-outcomes-test v-2 two-string), proposed by quotes
  {:instance 4 :outcome :o-2
   :artefact {:outcome-phrase "later implementation" :want-phrase "code asks for a role"}
   :want-quote roles-sentence :outcome-quote "later implementation"})

(defn- section-text [{:keys [text isecs]} n]
  (let [[lo hi] (:lines (first (filter #(= n (:instance %)) isecs)))]
    (str/join "\n" (subvec (str/split-lines text) (dec lo) hi))))

(deftest the-reading-route-round-trips-as-quotes
  (let [ctx (context (slurp mission-path))
        p (sbr/proposal (:text ctx) (:isecs ctx) (:outcomes ctx) reading-route)
        r (read-link ctx reading-route)]
    (is (= roles-sentence ((f 'cp-subs) (:text ctx) (first (:want-span p)) (second (:want-span p)))))
    (is (= mission-sha-pinned (sbr/sha256 (:text ctx)))
        "the fixture text is the pinned M-futon-seams; a changed text fails here, not silently downstream")
    (is (= (sbr/sha256 (:text ctx)) (:text-sha256 p)) "the proposal is pinned to the text it was placed in")
    (is (= [4 :o-2] [(:instance r) (:outcome r)]) (pr-str r))
    (is (= :proposed-verified (get-in r [:via :basis])))
    (is (= :reader-claimed (get-in r [:via :coreference])))
    (is (= (:want-span p) (get-in r [:via :want-span])))))

(deftest a-quote-occurring-twice-in-its-section-is-ambiguous
  (let [ctx (context (slurp mission-path))
        q "role"]
    (is (< 1 (count (re-seq #"role" (section-text ctx 4)))) "fixture: the quote repeats in section 4")
    (let [r (read-link ctx (assoc reading-route :want-quote q))]
      (is (= [:refused :quote-ambiguous :want] [(:status r) (:reason r) (:side r)]) (pr-str r)))))

(deftest a-quote-found-only-outside-its-section-is-absent
  ;; the sentence is in section 4; proposed for instance 5 it must not be
  ;; matched anywhere else in the file
  (let [ctx (context (slurp mission-path))
        r (read-link ctx (assoc reading-route :instance 5))]
    (is (str/includes? (:text ctx) roles-sentence))
    (is (not (str/includes? (section-text ctx 5) roles-sentence)))
    (is (= [:refused :quote-absent :want] [(:status r) (:reason r) (:side r)]) (pr-str r))
    (is (= 1 (get-in r [:detail :occurrences-elsewhere])))))

(deftest an-outcome-quote-in-none-of-its-cues-is-absent
  (let [ctx (context (slurp mission-path))
        r (read-link ctx (assoc reading-route :outcome-quote "neo4j adapter"))]
    (is (= [:refused :quote-absent :outcome] [(:status r) (:reason r) (:side r)]) (pr-str r))))

(deftest spans-are-code-points-after-an-astral-character
  ;; one character outside the BMP before everything: UTF-16 offsets now run
  ;; one ahead of code points, so a span taken as a Java index would name a
  ;; different substring
  (let [ctx (context (str "𝔸" (slurp mission-path)))
        text (:text ctx)
        p (sbr/proposal text (:isecs ctx) (:outcomes ctx) reading-route)
        i (str/index-of text roles-sentence)
        r (read-link ctx reading-route)]
    (is (not= [i (+ i (count roles-sentence))] (:want-span p)) "the UTF-16 index differs")
    (is (= roles-sentence ((f 'cp-subs) text (first (:want-span p)) (second (:want-span p)))))
    (is (= :proposed-verified (get-in r [:via :basis])) (pr-str r))))

(deftest a-verifier-refusal-comes-back-unchanged
  (let [ctx (context (slurp mission-path))]
    (testing "placed quotes, artefact in neither span: the verifier's reason"
      (let [r (read-link ctx (assoc reading-route :artefact "neo4j"))]
        (is (= :refused (:status r)))
        (is (= :artefact-not-in-both (:reason r)))
        (is (not (contains? r :side)) "not the read step's refusal")))
    (testing "an unknown instance or outcome is answered by the verifier, not here"
      (is (= :instance-unknown (:reason (read-link ctx (assoc reading-route :instance 99)))))
      (is (= :outcome-unknown (:reason (read-link ctx (assoc reading-route :outcome :o-99))))))))

;; ---------------------------------------------------------------------------
;; The flight hop (M-wm-wiring row 2): flight-runner/read-fn records the
;; served-by reading on the flight's read record. M-futon-seams is a FIXTURE
;; here (its text pinned by sha), not a flight target; M-autoclock-in is the
;; first target. No seat is asked for quotes: they are supplied by the caller
;; or recorded absent.

(def seams-sha "d13c5cfe9e9b19b445bd5bb73507f286a9e5ff3b478a1c5bc6a2250d70c6f6fd")

(defn- temp-store []
  (str (.toFile (java.nio.file.Files/createTempDirectory
                 "served-by-flight" (make-array java.nio.file.attribute.FileAttribute 0)))))

(defn- read-record [target repo path text opts]
  (let [f (futon2.aif.flight/start {:target target :chosen-because {:kind :requested}}
                                   {:kind :a-exits :repo repo :path path :read-text (fn [& _] text)}
                                   {:id (str "flight-read-" target)})
        rf (futon2.aif.flight-runner/read-fn
            (merge {:store (temp-store)
                    ;; every reading request goes unanswered: a :need, never a refusal
                    :answer-fn (fn [_] {:seat "none" :job-id "none" :state "failed"})}
                   opts))]
    (rf f {})))

(deftest the-flight-read-step-records-the-reading-route-link-and-a-refusal
  (let [text (slurp mission-path)
        refused (assoc reading-route :artefact "neo4j")
        rec (read-record "M-futon-seams" "futon3c" "holes/missions/M-futon-seams.md" text
                         {:served-by-quotes {"M-futon-seams" [reading-route refused]}
                          :served-by-cascades {"M-futon-seams" "../futon3c/holes/labs/M-futon-seams/proto"}})
        [ok bad] (get-in rec [:served-by :proposals])]
    (is (= seams-sha (:text-sha256 rec)) "the text sha on the record is the fixture's pinned sha")
    (is (= :wm/mission-outcomes-v5 (get-in rec [:served-by :outcomes :schema])))
    (is (vector? (get-in rec [:served-by :outcomes :served-by])) "v5 served-by rows, cascades read")
    (is (= reading-route (:proposal ok)))
    (is (= [4 :o-2] [(get-in ok [:result :instance]) (get-in ok [:result :outcome])]))
    (is (= :reader-claimed (get-in ok [:result :via :coreference])))
    (is (= [:refused :artefact-not-in-both] [(get-in bad [:result :status]) (get-in bad [:result :reason])])
        "a refused proposal is on the record with its reason, not dropped")
    (is (some? (get-in bad [:result :detail])))))

(deftest m-autoclock-in-records-the-anchor-absence-and-no-links
  (let [text (slurp "../futon3c/holes/missions/M-autoclock-in.md")
        rec (read-record "M-autoclock-in" "futon3c" "holes/missions/M-autoclock-in.md" text
                         {:served-by-quotes {"M-autoclock-in" [reading-route]}})
        bare (read-record "M-autoclock-in" "futon3c" "holes/missions/M-autoclock-in.md" text {})]
    (is (= {:absent :no-instances-anchor} (get-in rec [:served-by :outcomes :served-by])))
    (is (= {:absent :no-instances-anchor} (get-in rec [:served-by :proposals])))
    (is (= {:absent :no-quotes} (get-in bare [:served-by :proposals])) "no seat asked: absent, typed")
    (is (= (sbr/sha256 text) (:text-sha256 rec)))))
