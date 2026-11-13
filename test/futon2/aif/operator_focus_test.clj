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
      (is (= :all-seats-abstained (get-in v [:result :reason])))
      (is (= :both-halves-abstained (get-in v [:abstentions 0 :reason])))))
  (testing "planted regression wm-click-loop-repair-b1a9839d: turns read but reader reply unparseable is not 'no recent operator turns'"
    (let [turns (mapv (fn [i] (turn (str "e" i) "claude-4"
                                    (format "2026-11-12T23:%02d:00.000Z" (- 47 i)) "work"))
                      (range 5))
          v (focus/focus-vote turns areas
                              {:as-of "2026-11-13T02:28:18Z"
                               :text-answers {"claude-4" {:status :abstained
                                                          :reason :reader-invalid-json}}})]
      (is (= :all-seats-abstained (get-in v [:result :reason])))
      (is (not= :no-recent-operator-turns (get-in v [:result :reason])))
      (is (= ["claude-4"] (:seats v)))
      (is (= :reader-invalid-json (get-in v [:seat-votes 0 :text-half :reason])))))
  (testing "a genuinely quiet window keeps the no-recent-operator-turns reason"
    (let [v (focus/focus-vote [(turn "old" "a" "2026-10-26T00:00:00Z" "hello")]
                              areas {:as-of as-of})]
      (is (= :no-recent-operator-turns (get-in v [:result :reason])))))
  (testing "a recent clock and contrary text each cast half"
    (let [v (focus/focus-vote
             [(assoc (turn "x" "a" "2026-11-03T12:00:00Z" "please work on M-giraffe")
                     :captured-target "M-futonzero-prelim-practice")]
             areas {:as-of as-of :text-answers {"a" {:status :voted :area "Giraffe"}}})]
      (is (= {"Giraffe" 1/2 "futonzero" 1/2}
             (get-in v [:seat-votes 0 :votes])))
      (is (= "Giraffe" (get-in v [:seat-votes 0 :text-half :area])))))
  (testing "the captured clock records its source turn"
    (let [v (focus/focus-vote
             [(assoc (turn "x" "a" "2026-11-03T12:00:00Z" "M-giraffe") :captured-target "M-wm")]
             areas {:as-of as-of :text-answers {"a" {:status :voted :area "Giraffe"}}})]
      (is (= {"Giraffe" 1/2 "WM" 1/2} (get-in v [:seat-votes 0 :votes])))
      (is (= "x" (get-in v [:seat-votes 0 :clock-half :source-evidence-id]))))))

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

(deftest evidence-page-honors-bounded-retry-advice
  (let [calls (atom 0)
        get' (fn [_ _] (if (< (swap! calls inc) 3)
                         {:status 503 :body "{:retry-after-seconds 1}"}
                         {:status 200 :body "{:entries []}"}))
        r (focus/read-turns {:as-of as-of :http-get get' :sleep-fn (fn [_])})]
    (is (= :ok (:status r)))
    (is (= [3] (:page-attempts r)))))

(deftest evidence-page-busy-exhausts-within-bound
  (let [calls (atom 0)
        get' (fn [_ _] (swap! calls inc) {:status 503 :body "{:retry-after-seconds 1}"})
        r (focus/read-turns {:as-of as-of :http-get get' :sleep-fn (fn [_])})]
    (is (= :expensive-read-busy (:reason r)))
    (is (= [6] (:page-attempts r)))
    (is (= 6 @calls))))

(deftest evidence-read-applies-as-of-with-initial-cursor
  (let [seen-url (atom nil)
        get' (fn [url _]
               (reset! seen-url url)
               {:status 200 :body "{:entries []}"})
        r (focus/read-turns {:as-of as-of :http-get get'})]
    (is (= :ok (:status r)))
    (is (re-find #"cursor-at=2026-11-04T00%3A00%3A00Z" @seen-url))
    (is (re-find #"cursor-id=" @seen-url))
    (is (not (re-find #"system-as-of" @seen-url)))
    (is (= :event-time-cursor (:as-of-mode r)))))

(deftest evidence-read-post-filters-rows-after-as-of
  (let [row (fn [id at turn-id]
              {:evidence/id id :evidence/at at
               :evidence/origin {:kind :operator}
               :evidence/body {:role "user" :turn-id turn-id :text id}})
        body (pr-str {:entries [(row "future" "2026-11-04T00:00:01Z" "a-turn-2")
                                (row "kept" "2026-11-04T00:00:00Z" "a-turn-1")]})
        r (focus/read-turns {:as-of as-of
                             :http-get (fn [_ _] {:status 200 :body body})})]
    (is (= ["kept"] (mapv :evidence-id (:turns r))))))

;; The reader is a handoff (handoff-contract): the Lucy 2026-11-13 failure was a
;; correct judgement in prose, "evidence_ids":[all five], which never parsed.
(def ^:private lucy-prose-reply
  (str "Answer delivered for E-kimi-task-437.\n\n**Result:** `{\"answers\":[{\"seat\":\"claude-4\","
       "\"area\":\"futon5\",\"evidence_ids\":[all 5 emacs-* IDs]}],\"clock_answers\":[]}`"))

(defn- reader-run [replies]
  (let [left (atom replies) requests (atom [])
        a7 (doto (java.io.File/createTempFile "a7-authority" ".md") (spit "A7"))
        contact (fn [req] (swap! requests conj req)
                  (let [r (first @left)] (swap! left rest)
                    {:seat "猴-1" :job-id (str "job-" (count @requests)) :state "done" :text r}))]
    [(focus/dispatch-reader!
      [{:evidence-id "e1" :seat "claude-4" :text "MCA"}]
      {:prompt-resource "wm/focus/operator-focus-reader-v1.md" :reader-contact contact
       :a7-path (.getPath a7) :code-root "/tmp"})
     @requests]))

(deftest reader-reply-in-prose-gets-one-format-correction
  (let [[r reqs] (reader-run [lucy-prose-reply
                              "{\"answers\":[{\"seat\":\"claude-4\",\"area\":\"futon5\",\"evidence_ids\":[\"e1\"]}],\"clock_answers\":[]}"])]
    (is (= :ok (:status r)))
    (is (= 2 (count reqs)) "one correction round")
    (is (:format-correction (second reqs)))
    (is (re-find #"ACCEPTANCE CONTRACT" (focus/reader-prompt (first reqs))))
    (is (re-find #"futon5" (:raw-reply r)))))

(deftest reader-that-never-replies-json-is-a-typed-refusal
  (let [[r _] (reader-run [lucy-prose-reply lucy-prose-reply])]
    (is (= :absent (:status r)))
    (is (= :reader-handoff-refused (:reason r)))
    (is (= :response-invalid (get-in r [:handoff :reason])))))

(deftest reader-without-contact-is-not-contacted
  (let [a7 (doto (java.io.File/createTempFile "a7-authority" ".md") (spit "A7"))
        r (focus/dispatch-reader! [] {:prompt-resource "wm/focus/operator-focus-reader-v1.md"
                                               :a7-path (.getPath a7) :code-root "/tmp"})]
    (is (= :absent (:status r)))
    (is (= :not-contacted (get-in r [:handoff :status])))))
