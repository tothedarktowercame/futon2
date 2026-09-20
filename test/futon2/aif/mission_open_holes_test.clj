(ns futon2.aif.mission-open-holes-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.mission-registry :as mr]))

(def ^:private doc
  ["# Mission: probe"
   "**Status:** ACTIVE"
   "- [ ] first stated want"
   "- [x] already done, not a want"
   "Some prose with a TODO in it."
   "## Remaining work"
   "- an item under an open heading"
   "## Closed section"
   "- not under an open heading"])

(deftest retains-the-items-the-count-summarises
  (let [holes (#'mr/open-holes "M-probe" :active doc)]
    (testing "every retained item carries kind, line, text and a stable id"
      (is (every? #(and (:kind %) (:line %) (seq (:text %)) (seq (:id %))) holes)))
    (testing "the count equals the items, so they cannot disagree"
      (is (= (#'mr/open-hole-count "M-probe" :active doc) (count holes))))
    (testing "an unchecked task is retained and a checked one is not"
      (is (some #(= "- [ ] first stated want" (:text %)) holes))
      (is (not-any? #(re-find #"already done" (:text %)) holes)))
    (testing "a list item under an open heading is retained, one under another heading is not"
      (is (some #(= "- an item under an open heading" (:text %)) holes))
      (is (not-any? #(re-find #"not under an open" (:text %)) holes)))))

(deftest terminal-states-have-no-wants
  (doseq [sc [:complete :inactive :draft]]
    (is (= [] (#'mr/open-holes "M-probe" sc doc)) (str sc))))

(deftest ids-are-stable-under-line-movement-and-distinct-per-item
  (let [a (#'mr/open-holes "M-probe" :active doc)
        moved (#'mr/open-holes "M-probe" :active (into ["" "" ""] doc))]
    (testing "same items keep their ids when the document shifts"
      (is (= (set (map :id a)) (set (map :id moved)))))
    (testing "line numbers did move, so the ids are not just positions"
      (is (not= (set (map :line a)) (set (map :line moved)))))
    (testing "distinct items get distinct ids"
      (is (= (count (distinct (map :id a))) (count (distinct (map :text a))))))))

(deftest a-stored-count-without-items-is-typed-not-silent
  (let [legacy (#'mr/substrate-entity->entry
                {:entity/external-id "M-legacy"
                 :entity/props (pr-str {:mission/open-hole-count 7})})]
    (is (= 7 (:open-hole-count legacy)))
    (is (= [] (:open-holes legacy)))
    (is (= :not-ingested (:open-holes-status legacy))
        "a count with no items must not read as 'no holes' -- that silence is the facade")))
