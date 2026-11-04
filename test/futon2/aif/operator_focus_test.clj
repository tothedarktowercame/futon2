(ns futon2.aif.operator-focus-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.operator-focus :as focus]))

(def as-of "2026-11-04T00:00:00Z")
(def areas {"M-wm" "WM" "M-giraffe" "Giraffe"
            "M-futonzero-prelim-practice" "other:futonzero"})
(defn turn [id seat at text & [target]]
  {:evidence-id id :seat seat :at at :text text :captured-target target})

(deftest vote-and-tie
  (let [turns [(turn "1" "a" "2026-11-03T23:00:00Z" "M-wm")
               (turn "2" "b" "2026-11-03T22:00:00Z" "M-wm")
               (turn "3" "c" "2026-11-03T21:00:00Z" "M-wm")
               (turn "4" "d" "2026-11-03T20:00:00Z" "M-giraffe")]
        v (focus/focus-vote turns areas {:as-of as-of})]
    (is (= #{"WM"} (get-in v [:result :focus])))
    (is (= 4 (count (:seat-votes v))))
    (is (= #{"WM" "Giraffe"}
           (get-in (focus/focus-vote (assoc-in turns [2 :text] "M-giraffe")
                                     areas {:as-of as-of})
                   [:result :focus])))))

(deftest recency-window-and-seat-cap
  (let [recent (mapv #(turn (str %) (str "s" %) (format "2026-11-03T%02d:00:00Z" %) "M-wm")
                     (range 12))
        old (turn "old" "old-seat" "2026-10-26T00:00:00Z" "M-wm")
        v (focus/focus-vote (conj recent old) areas {:as-of as-of})]
    (is (= 10 (count (:seats v))))
    (is (not (some #{"s0" "s1" "old-seat"} (:seats v))))
    (is (= #{"WM"} (get-in v [:result :focus])))))

(deftest abstention-zero-and-precedence
  (testing "unmapped seats abstain and never create a default"
    (let [v (focus/focus-vote [(turn "x" "a" "2026-11-03T12:00:00Z" "hello")]
                              areas {:as-of as-of})]
      (is (= :no-recent-operator-turns (get-in v [:result :reason])))
      (is (= :no-mappable-turns (get-in v [:abstentions 0 :reason])))))
  (testing "task named in text precedes a contradictory captured target"
    (let [v (focus/focus-vote
             [(turn "x" "a" "2026-11-03T12:00:00Z" "please work on M-giraffe"
                    "M-futonzero-prelim-practice")]
             areas {:as-of as-of})]
      (is (= #{"Giraffe"} (get-in v [:result :focus])))
      (is (= :text-task (get-in v [:seat-votes 0 :turns 0 :step]))))))

(deftest replay-is-retained
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "operator-focus-test" (make-array java.nio.file.attribute.FileAttribute 0)))
        turns [(turn "1" "a" "2026-11-03T12:00:00Z" "M-wm")]
        vote (focus/focus-vote turns areas {:as-of as-of})
        m (focus/manifest turns vote "corpus-sha" {})
        pin (focus/write-manifest! dir m)]
    ;; A mutable turn source is irrelevant to replay; the manifest is sufficient.
    (spit (java.io.File. dir "turn-copy.edn") (pr-str {:text "M-giraffe"}))
    (is (= (:result vote) (:result (focus/replay (:path pin)))))
    (is (= m (edn/read-string (slurp (:path pin)))))))
