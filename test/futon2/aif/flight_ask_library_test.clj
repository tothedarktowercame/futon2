(ns futon2.aif.flight-ask-library-test
  "M-wm-wiring row 3: the ask step passes the flight's library root to the
  want-interpretation prompt; a reply's :retrieval reaches validate-response
  and its outcome is on the ask record; the ask step's publication is in the
  click's view; an answer still in flight at the click is a pending need and
  the flight ends :awaiting-answer, not :no-progress. No seat is asked:
  every answer is a fixture function.

  agency-answer-fn today: a bell to the seat, then runner/poll-job!, which
  waits until the job is terminal (recording stalls, never abandoning). So a
  real answer is settled before the click posts; a pending answer reaches
  the ask step only when an answer function returns before the job ends,
  which the pending test supplies."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.interpretation-request :as ireq]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.want-interpretation :as wi])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- temp-dir [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(def mission-file "test/fixtures/mission-criteria/M-futon-seams@futon3c-20959e4f.md")
(def mission-text (slurp mission-file))
(def proposals (edn/read-string (slurp "test/fixtures/want-interp-library/M-futon-seams-interpretations@futon2-78439f58.edn")))
(def document :exit/h54d16050a9dc)
(def argue :exit/hac75428b9c97)
(def by-want {document :writing-coherence/meet-the-reader-where-they-are
              argue :writing-coherence/plain-language-thesis})

(defn- reply-for [id & [extra]]
  (str "```edn\n"
       (with-out-str (pp/pprint (merge {:schema wi/response-schema :pattern id
                                        :receipt (get-in proposals [:interpretation-receipts id])}
                                       (get-in proposals [:patterns id])
                                       extra)))
       "```\n"))

(defn- request-options []
  (let [d (temp-dir "ask-code") code (io/file d "code.py") index (io/file d "index.json")]
    (spit code "# retriever fixture") (spit index "[]")
    {:resolve-fn (fn [_] {:id "M-futon-seams" :path (.getCanonicalPath (io/file mission-file))})
     :revision-fn (constantly "fixture-revision") :library-fn (constantly [])
     :retrieve-fn (fn [_] [{:pattern "writing-coherence/plain-language-thesis" :score 1}])
     :retriever-specs (mapv #(assoc % :implementation (.getCanonicalPath code)
                                    :index (.getCanonicalPath index)) ireq/retrievers)}))

(defn- seams-flight []
  (flight/start {:target "M-futon-seams" :chosen-because {:kind :requested}}
                {:kind :a-exits :repo "futon3c" :path "holes/missions/M-futon-seams.md"
                 :read-text (fn [& _] mission-text)
                 :observe #(checks/decl-present? mission-text (:decl %))}
                {:id "flight-ask-library"}))

(def tick-sources {:beta-by-context {:WM {:beta 1}}})
(def code-root (.getCanonicalPath (io/file "test/fixtures/want-interp-library")))

(defn- ask-fn [store answer-fn]
  (fr/ask-fn {:store store :answer-fn answer-fn :code-root code-root
              :request-options (request-options)}))

(deftest the-library-root-is-on-the-prompt-from-the-flight-opts
  (let [sent (atom nil)
        af (fn [root] (fr/agency-answer-fn (cond-> {:seat "s" :opts {}
                                                    :dispatch! (fn [_ _ _ _ p] (reset! sent p) {:job-id "j"})
                                                    :poll! (fn [_ id] {:state "done" :job-id id})
                                                    :job-text (constantly "")}
                                             root (assoc :library-root root))))
        issued {:target "M-futon-seams" :request-id "r" :want {:token argue}}
        with (do ((af "/fixture/library-root") issued) @sent)
        a ((af nil) issued)]
    (is (str/includes? with "/fixture/library-root"))
    (is (= {:absent :not-in-flight-opts} (:library-root a)))
    (is (str/includes? @sent ireq/library-root) "none in the opts: the prompt's own pinned default, recorded as absent from the flight")))

(deftest retrieval-reaches-validation-and-its-absence-is-recorded
  (let [answer (fn [runs] (fn [issued]
                            {:seat "s" :job-id "j" :state "done"
                             :text (reply-for (by-want (get-in issued [:want :token]))
                                              (when runs {:retrieval {:runs runs}}))}))
        run (fn [answer-fn] (let [f (seams-flight)]
                              ((ask-fn (str (temp-dir "store")) answer-fn) f (flight/click-wants f tick-sources) tick-sources)))
        none (run (answer nil))
        bad (run (answer [{:retriever "not-agent-search" :candidates []}]))]
    (is (every? #(= {:absent :no-appended-runs} (:retrieval %)) (:asked none)))
    (is (every? #(= {:appended-runs 1} (:retrieval %)) (:asked bad)))
    (is (every? #(= :rejected (:outcome %)) (:asked bad)))
    (is (some #(= :appended-run-not-agent-search (:reason %)) (mapcat :reasons (:asked bad)))
        "validate-response saw the appended run and refused it")))

(deftest the-ask-steps-publication-is-in-the-clicks-view
  (let [store (str (temp-dir "store"))
        seen (atom nil)
        f (flight/run! (seams-flight)
                       {:ask-fn (ask-fn store (fn [issued] {:seat "s" :job-id "j" :state "done"
                                                            :text (reply-for (by-want (get-in issued [:want :token])))}))
                        :click-fn (fn [_] (reset! seen (set (keys (:patterns (wi/read-published store "M-futon-seams")))))
                                    {:click-id "c"})
                        :observe-fn (fn [_ _] {})
                        :sources-fn (constantly tick-sources)
                        :max-clicks 1})]
    (is (= #{:writing-coherence/meet-the-reader-where-they-are :writing-coherence/plain-language-thesis} @seen)
        "published before the click posted")
    (is (not= :awaiting-answer (:status f)))))

(deftest a-pending-answer-is-a-pending-need-not-no-progress
  (let [f (flight/run! (seams-flight)
                       {:ask-fn (ask-fn (str (temp-dir "store")) (fn [_] {:seat "s" :job-id "j-running" :state "running" :text nil}))
                        :click-fn (fn [_] {:click-id "c"})
                        :observe-fn (fn [_ _] {})
                        :sources-fn (constantly tick-sources)
                        :max-clicks 3})]
    (is (= :awaiting-answer (:status f)))
    (is (seq (:pending f)))
    (is (every? #(and (= :pending (:kind %)) (= "running" (:state %)) (= "j-running" (:job-id %))) (:pending f)))
    (is (= 1 (count (:clicks f))) "one click, then the flight waits instead of spending more")))
