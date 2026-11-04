(ns futon2.aif.operator-focus-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.operator-focus :as focus]))

(def as-of "2026-11-04T00:00:00Z")
(def areas {"M-wm" "WM" "M-giraffe" "Giraffe"
            "M-futonzero-prelim-practice" "futonzero"
            "M-patterns" "patterns" "M-apm" "APM"})
(defn turn [id seat at text]
  {:evidence-id id :seat seat :at at :text text})
(defn clock [seat at target] {:seat seat :at at :target target})

(deftest vote-and-tie
  (let [turns [(turn "1" "a" "2026-11-03T23:00:00Z" "M-wm")
               (turn "2" "b" "2026-11-03T22:00:00Z" "M-wm")
               (turn "3" "c" "2026-11-03T21:00:00Z" "M-wm")
               (turn "4" "d" "2026-11-03T20:00:00Z" "M-giraffe")]
        answers (into {} (map (fn [{:keys [seat text]}] [seat {:status :voted :area (areas text)}]) turns))
        v (focus/focus-vote turns areas {:as-of as-of :text-answers answers})]
    (is (= "WM" (get-in v [:result :ranked 0 :area])))
    (is (= 3/4 (get-in v [:result :ranked 0 :share])))
    (is (= 4 (count (:seat-votes v))))
    (let [tie (focus/focus-vote turns areas
                                {:as-of as-of
                                 :text-answers (assoc answers "c" {:status :voted :area "Giraffe"})})]
      (is (= #{"WM" "Giraffe"} (set (map :area (get-in tie [:result :ranked]))))))))

(deftest top-three-retains-third-place-tie
  (let [spec (concat (repeat 4 "M-wm") (repeat 3 "M-giraffe")
                     (repeat 2 "M-patterns") (repeat 2 "M-apm"))
        turns (mapv (fn [i task]
                      (turn (str i) (str "s" i)
                            (format "2026-11-03T%02d:00:00Z" (mod i 24)) task))
                    (range) spec)
        v (focus/focus-vote turns areas {:as-of as-of :seat-count 20 :text-answers (into {} (map (fn [t] [(:seat t) {:status :voted :area (areas (:text t))}]) turns))})]
    (is (= #{"WM" "Giraffe" "patterns" "APM"}
           (set (map :area (get-in v [:result :ranked])))))
    (is (= 4 (count (get-in v [:result :ranked]))))))

(deftest recency-window-and-seat-cap
  (let [recent (mapv #(turn (str %) (str "s" %) (format "2026-11-03T%02d:00:00Z" %) "M-wm")
                     (range 12))
        old (turn "old" "old-seat" "2026-10-26T00:00:00Z" "M-wm")
        v (focus/focus-vote (conj recent old) areas {:as-of as-of :text-answers (into {} (map (fn [t] [(:seat t) {:status :voted :area "WM"}]) recent))})]
    (is (= 10 (count (:seats v))))
    (is (not (some #{"s0" "s1" "old-seat"} (:seats v))))
    (is (= "WM" (get-in v [:result :ranked 0 :area])))))

(deftest abstention-zero-and-two-halves
  (testing "unmapped seats with no clock abstain and never create a default"
    (let [v (focus/focus-vote [(turn "x" "a" "2026-11-03T12:00:00Z" "hello")]
                              areas {:as-of as-of})]
      (is (= :no-recent-operator-turns (get-in v [:result :reason])))
      (is (= :both-halves-abstained (get-in v [:abstentions 0 :reason])))))
  (testing "a recent clock and contrary text each cast half"
    (let [v (focus/focus-vote
             [(turn "x" "a" "2026-11-03T12:00:00Z" "please work on M-giraffe")]
             areas {:as-of as-of :text-answers {"a" {:status :voted :area "Giraffe"}}
                    :clocks [(clock "a" "2026-11-03T11:00:00Z"
                                    "M-futonzero-prelim-practice")]})]
      (is (= {"Giraffe" 1/2 "futonzero" 1/2}
             (get-in v [:seat-votes 0 :votes])))
      (is (= "Giraffe" (get-in v [:seat-votes 0 :text-half :area])))))
  (testing "a nine-day-old clock abstains while text still casts its half"
    (let [v (focus/focus-vote
             [(turn "x" "a" "2026-11-03T12:00:00Z" "M-giraffe")]
             areas {:as-of as-of :text-answers {"a" {:status :voted :area "Giraffe"}}
                    :clocks [(clock "a" "2026-10-26T00:00:00Z" "M-wm")]})]
      (is (= {"Giraffe" 1/2} (get-in v [:seat-votes 0 :votes])))
      (is (= :clock-outside-window (get-in v [:seat-votes 0 :clock-half :reason]))))))

(deftest replay-is-retained
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "operator-focus-test" (make-array java.nio.file.attribute.FileAttribute 0)))
        turns [(turn "1" "a" "2026-11-03T12:00:00Z" "M-wm")]
        vote (focus/focus-vote turns areas {:as-of as-of :text-answers {"a" {:status :voted :area "WM"}}})
        m (focus/manifest turns vote "corpus-sha" {:clock-response-sha256 "clock-sha"})
        pin (focus/write-manifest! dir m)]
    (spit (java.io.File. dir "turn-copy.edn") (pr-str {:text "M-giraffe"}))
    (is (= (:result vote) (:result (focus/replay (:path pin)))))
    (is (= "clock-sha" (:clock-response-sha256 m)))
    (is (= m (edn/read-string (slurp (:path pin)))))))

(deftest reader-reply-is-checked
  (let [sent [(turn "e1" "a" as-of "x")]
        unknown-id (focus/check-reply "{\"answers\":[{\"seat\":\"a\",\"area\":\"WM\",\"evidence_ids\":[\"bad\"]}]}" sent ["a"])
        unknown-area (focus/check-reply "{\"answers\":[{\"seat\":\"a\",\"area\":\"bogus\",\"evidence_ids\":[\"e1\"]}]}" sent ["a"])]
    (is (= :reader-unknown-evidence-id (get-in unknown-id ["a" :reason])))
    (is (= :reader-unknown-area (get-in unknown-area ["a" :reason])))))

(deftest reader-json-may-be-fenced
  (let [raw "note\n```json\n{\"answers\":[{\"seat\":\"a\",\"area\":\"WM\",\"evidence_ids\":[\"e1\"]}]}\n```\nreview"
        checked (focus/check-reply raw [(turn "e1" "a" as-of "x")] ["a"])]
    (is (= "WM" (get-in checked ["a" :area])))))

(deftest reader-packet-represents-every-selected-seat
  (let [turns (mapv (fn [i] (turn (str i) (str "s" i) (str "2026-11-03T" (format "%02d" i) ":00:00Z") "x")) (range 10))]
    (is (= (set (map :seat turns)) (set (map :seat (focus/reader-turns turns (mapv :seat turns))))))))

(deftest unresolved-clock-reply-is-checked
  (let [input [{:target "M-x" :text "# X\n\nUseful work"}]
        good (focus/check-clock-reply "{\"clock_answers\":[{\"target\":\"M-x\",\"area\":\"Agency\",\"line\":3}]}" input)
        bad (focus/check-clock-reply "{\"clock_answers\":[{\"target\":\"M-x\",\"area\":\"Agency\",\"line\":2}]}" input)]
    (is (= "Agency" (get-in good ["M-x" :area])))
    (is (= :reader-invalid-line-citation (get-in bad ["M-x" :reason])))))
