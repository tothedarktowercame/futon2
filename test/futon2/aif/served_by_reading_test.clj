(ns futon2.aif.served-by-reading-test
  "Quotes to spans for served-by proposals, against the live M-futon-seams
  text (its sha is checked by the verifier's :text-sha256 pin on every call).
  The extractor script is load-filed into its own namespace, as
  futon2.wm.extract-outcomes-test does, until it is a requirable namespace."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.served-by-reading :as sbr]))

(def script-ns 'extract-outcomes-for-reading)

(binding [*ns* (create-ns script-ns)]
  (clojure.core/refer-clojure)
  (load-file "scripts/wm/extract-outcomes.clj"))

(defn- f [v] @(ns-resolve script-ns v))

(def mission-path "../futon3c/holes/missions/M-futon-seams.md")

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
