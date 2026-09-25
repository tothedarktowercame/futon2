(ns futon2.aif.locator-cue-test
  "WM-CUE-I: a locator cue may elide with `...`; each piece is the
  criterion's own words, in order. Live pins: the five quotes rejected
  :cue-not-in-criterion on flight-ffcd772b and their criteria's :stated text
  (fixture header gives the sources and shas)."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.mission-reading :as reading]))

(def rows (edn/read-string (slurp "test/futon2/aif/fixtures/locator-cues@flight-ffcd772b.edn")))
(def by-want (into {} (map (juxt :want identity)) rows))

(defn- validate [row quote]
  (reading/validate-locator {:want {:token (:want row)} :criterion {:stated (:stated row)}}
                            {:locator {:class :C8 :repo "futon3c" :namespace "n"}
                             :cue {:quote quote} :reading "r"}
                            {:observe (fn [m] {:observed #{} :results (zipmap (keys m) (repeat {:observed false})) :refused {}})}))

(defn- cue-reason [v] (first (filter #(= :cue-not-in-criterion (:reason %)) (:reasons v))))

(deftest the-five-faithful-quotes-are-accepted
  (is (= 5 (count rows)))
  (doseq [r rows]
    (is (nil? (reading/cue-missing-piece (:stated r) (:quote r))) (str (:want r)))
    (is (= :valid (:status (validate r (:quote r)))) (str (:want r)))))

(def h7afd (by-want :exit/h7afd70f40a29))
(def h0c55 (by-want :exit/h0c55648e57e5))
(def h59ad (by-want :exit/h59adca2cd730))

(deftest pieces-out-of-order-are-refused-naming-the-piece
  (let [[a b] (map str/trim (str/split (:quote h7afd) #"\.\.\."))
        v (validate h7afd (str b " ... " a))]
    (is (= :rejected (:status v)))
    (is (= (#'reading/words a) (:piece (cue-reason v))) "the piece found only before the previous one")))

(deftest a-piece-from-another-criterion-is-refused
  ;; h0c55's second piece replaced by words of h59ad's criterion
  (let [foreign "Every accepted turn must have a persisted decision"
        _ (assert (str/includes? (#'reading/words (:stated h59ad)) foreign))
        [a _ c] (map str/trim (str/split (:quote h0c55) #"\.\.\."))
        v (validate h0c55 (str a " ... " foreign " ... " c))]
    (is (= :rejected (:status v)))
    (is (= foreign (:piece (cue-reason v))))))

(deftest blank-pieces-and-blank-quotes-are-refused
  (is (= "" (:piece (cue-reason (validate h7afd "auto-clocking on a fuzzy mention ... ... A wrong auto-clock")))))
  (is (some? (cue-reason (validate h7afd ""))))
  (is (= "" (:piece (cue-reason (validate h7afd "...")))) "an ellipsis alone quotes nothing"))

(deftest the-contiguous-case-is-unchanged
  (is (= :valid (:status (validate h7afd "would mislabel turns and corrupt `turn→mission` ground truth."))))
  (is (= :valid (:status (validate h7afd "... would mislabel turns and corrupt"))) "an ellipsis at an edge elides the criterion's edge")
  (is (some? (cue-reason (validate h7afd "would mislabel turns and wreck"))) "a single piece not in the criterion is refused, as before"))
