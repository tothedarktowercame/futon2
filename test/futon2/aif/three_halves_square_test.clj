(ns futon2.aif.three-halves-square-test
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.legacy-three-halves-registry :as legacy]
            [futon2.aif.token-observation-initialization-test :as initialization]
            [futon2.aif.three-halves-square :as sut]))

(def fixture (edn/read-string
              (slurp "test/fixtures/three-halves-square/publication-cadence.edn")))

(def library-root "/home/joe/code/futon3/library")
(def publication-text
  (slurp (str library-root "/ukrns/publication-cadence.flexiarg")))
(def publication-parts
  "Whole parts of publication-cadence, as the 象 emitter reads them, with the
   source pin of the bytes they were read from."
  (edn/read-string
   (slurp "test/fixtures/three-halves-square/publication-cadence-parts.edn")))
(def publication-object-texts (:texts publication-parts))
(def publication-first-sentences
  "The first sentence of each part: a substring, not the part."
  {:I1 "The pattern operates on the axis: living document (continuously-updated canonical version) ↔ frozen snapshots (versioned fixed publications with citable identifiers)."
   :I2 "Real costs: living costs archival durability — a snapshot citable today may differ from the same URL tomorrow; frozen costs currency — citable versions drift further from the current evidence state over time."
   :G "Citation integrity: an academic artefact that readers cite must be citable as a specific object."
   :B "(3) Both: living current version + periodic frozen snapshots, each snapshot declaring its evidence-file revision and compute-date."})

(defn- with-object-texts [square texts]
  (reduce-kv #(assoc-in %1 [:objects %2 :text] %3) square texts))

(def good-parts-square (with-object-texts fixture publication-object-texts))

(deftest square-object-texts-come-from-their-assigned-pattern-parts
  (testing "the fixture's parts were read from the live file's bytes"
    (is (= (get-in publication-parts [:source :sha256])
           (evidence/sha256 (.getBytes ^String publication-text "UTF-8")))))
  (testing "every whole part spans a line break, so the check joins lines"
    (is (every? #(str/includes? % "\n") (vals publication-object-texts))))
  (is (= {:status :valid} (sut/parts-check good-parts-square publication-text)))
  (testing "the first sentence of a part is a substring, not the part"
    (is (= {:status :refused :kind :square-text-not-from-pattern
            :roles [:G :I1 :I2 :B]}
           (sut/parts-check (with-object-texts fixture publication-first-sentences)
                            publication-text))))
  (testing "one role citing one word of its part is refused by name"
    (is (= {:status :refused :kind :square-text-not-from-pattern :roles [:B]}
           (sut/parts-check (assoc-in good-parts-square [:objects :B :text] "snapshots")
                            publication-text))))
  (testing "the legacy per-target template is defaulted for every role"
    (let [legacy-square (#'legacy/square
                         "/home/joe/code"
                         "M-interim-director-proxy-metric-inventory"
                         :orchestration/recorded-handoff
                         {:source {:path "futon3/library/orchestration/recorded-handoff.flexiarg"
                                   :sha256 "232ec874e6061ce48f7edc39cc6a7029c7005a934c5769abd26cf4ed93c30620"}})]
      (is (= {:status :refused :kind :square-text-not-from-pattern
              :roles [:G :I1 :I2 :B]}
             (sut/parts-check legacy-square
                              (slurp (str library-root "/orchestration/recorded-handoff.flexiarg")))))))
  (testing "the authored interpretation-registry square is also defaulted"
    (let [registry (edn/read-string
                    (slurp "/home/joe/code/futon2/data/wm-interpretations/M-futonzero-mvp.edn"))
          square (get-in registry [:receipts :contracts/declare-the-reductions :blend-square])
          result (sut/parts-check
                  square (slurp (str library-root "/contracts/declare-the-reductions.flexiarg")))]
      (is (= :refused (:status result)))
      (is (= :square-text-not-from-pattern (:kind result)))
      (is (seq (:roles result)))))
  (testing "matching is per assigned part, not anywhere in the file"
    (let [swapped (-> good-parts-square
                      (assoc-in [:objects :I1 :text] (:I2 publication-object-texts))
                      (assoc-in [:objects :I2 :text] (:I1 publication-object-texts)))]
      (is (= {:status :refused :kind :square-text-not-from-pattern
              :roles [:I1 :I2]}
             (sut/parts-check swapped publication-text)))))
  (testing "a missing written part is named"
    (is (= {:status :refused :kind :pattern-part-missing :parts [:BECAUSE]}
           (sut/parts-check good-parts-square
                            (str/replace publication-text #"(?s)\n  \+ BECAUSE:.*?(?=\n  \+ NEXT-STEPS:)" ""))))))

(deftest real-library-reading-validates
  (is (= :valid (:status (sut/validate fixture)))))

(deftest one-authority-square-validates-and-joins-exact-interpretation
  (let [pattern :apparatus/one-authority-per-question
        declaration (initialization/declaration-with-one-authority-square)
        interpretation (get-in declaration [:patterns pattern])
        receipt (get-in declaration [:interpretation-receipts pattern])
        square (:blend-square receipt)]
    (is (= :valid (:status (sut/validate square))))
    (is (initialization/exact-one-authority-square?
         square pattern interpretation (dissoc receipt :blend-square)))
    (testing "a different pattern cannot borrow this square"
      (is (false? (initialization/exact-one-authority-square?
                   square :apparatus/done-is-observed-running
                   interpretation (dissoc receipt :blend-square)))))
    (testing "a mutated source pin cannot join the retained receipt"
      (is (false? (initialization/exact-one-authority-square?
                   (assoc-in square [:source :sha256] (apply str (repeat 64 "0")))
                   pattern interpretation (dissoc receipt :blend-square)))))
    (testing "an uncovered blend object is refused"
      (is (= :refused
             (:status (sut/validate (assoc-in square [:maps :b2] #{[20 30]}))))))
    (testing "inconsistent routes cannot commute"
      (let [bad (assoc-in square [:maps :b2] #{[20 32] [21 30]})
            gaps (:missing-evidence (sut/validate bad))]
        (is (some #{:noncommutation} gaps))
        (is (some #{:inconsistency} gaps))))))

(deftest adversarial-square-evidence-refuses
  (testing "wrong map direction"
    (is (some map? (:missing-evidence
                    (sut/validate (assoc-in fixture [:maps :a1] #{[10 0]}))))))
  (testing "well-formed but noncommuting routes"
    (is (some #{:noncommutation}
              (:missing-evidence
               (sut/validate (assoc-in fixture [:maps :a2] #{}))))))
  (testing "two active routes assigning different blend names are inconsistent"
    (let [bad (-> fixture
                  (assoc-in [:objects :B :theory :elems] #{30 31 32 33})
                  (assoc-in [:objects :B :theory :names 33] "other-address")
                  (assoc-in [:maps :b2] #{[20 33] [21 32]}))
          result (sut/validate bad)]
      (is (some #{:inconsistency} (:missing-evidence result))))))

(deftest uncovered-blend-object-refuses
  (let [result (sut/validate (assoc-in fixture [:maps :b2] #{[20 30]}))]
    (is (= :refused (:status result)))
    (is (= {:kind :pushout-coverage :uncovered #{32}}
           (some #(when (and (map? %) (= :pushout-coverage (:kind %))) %)
                 (:missing-evidence result))))))

(def inconsistent-covered
  (assoc-in fixture [:maps :b2] #{[20 32] [21 30]}))

(deftest auxiliary-flag-matrix-matches-the-lean-certificate
  (testing "without auxiliary flags, inconsistent noncommuting routes refuse"
    (let [result (sut/validate inconsistent-covered)]
      (is (= :refused (:status result)))
      (is (some #{:noncommutation} (:missing-evidence result)))
      (is (some #{:inconsistency} (:missing-evidence result)))
      (is (not-any? #(and (map? %) (= :pushout-coverage (:kind %)))
                    (:missing-evidence result)))))
  (testing "aux1 only accepts by retaining only route2 for consistency"
    (is (= :valid (:status
                   (sut/validate (assoc-in inconsistent-covered
                                           [:auxiliary :b1] true))))))
  (testing "aux2 only accepts by retaining only route1 for consistency"
    (is (= :valid (:status
                   (sut/validate (assoc-in inconsistent-covered
                                           [:auxiliary :b2] true))))))
  (testing "both auxiliary flags make route consistency vacuous"
    (is (= :valid (:status
                   (sut/validate (assoc inconsistent-covered
                                        :auxiliary {:b1 true :b2 true})))))))

(deftest both-auxiliary-flags-do-not-waive-joint-coverage
  (let [receipt (-> fixture
                    (assoc-in [:maps :b2] #{[20 30]})
                    (assoc :auxiliary {:b1 true :b2 true}))
        result (sut/validate receipt)]
    (is (= :refused (:status result)))
    (is (= {:kind :pushout-coverage :uncovered #{32}}
           (some #(when (and (map? %) (= :pushout-coverage (:kind %))) %)
                 (:missing-evidence result))))))

(deftest noncommutation-refuses-even-when-active-routes-are-consistent
  (let [result (sut/validate (assoc-in fixture [:maps :a2] #{}))]
    (is (= :refused (:status result)))
    (is (some #{:noncommutation} (:missing-evidence result)))
    (is (not (some #{:inconsistency} (:missing-evidence result))))))
