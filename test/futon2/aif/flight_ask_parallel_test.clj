(ns futon2.aif.flight-ask-parallel-test
  "Parallel asks (follow-up to futon2 a49bfaa): one target's wants are asked
  concurrently (bounded, seats round-robin), publishes are serialised
  per target, and pin! tolerates two requests racing the snapshot
  CREATE_NEW. Click 17 measured six sequential asks at 25-43 s seat time
  each; local work between asks is ~10 s, so parallel the ask step is about
  one seat round trip."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.interpretation-request :as ireq]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.want-interpretation :as wi])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.util.concurrent CountDownLatch]))

(def roots (atom []))
(use-fixtures :each
  (fn [f]
    (try (f) (finally
               (doseq [root @roots file (reverse (file-seq root))]
                 (Files/delete (.toPath file)))
               (reset! roots [])))))

(defn- temp-dir [prefix]
  (let [d (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0)))]
    (swap! roots conj d) d))

;; the first-flight fixture, as in flight-ask-test: two wants (ARGUE and
;; DOCUMENT), the pinned library, kimi-6's proposals.

(def mission-text (slurp "test/fixtures/mission-criteria/M-futon-seams@futon3c-20959e4f.md"))
(def proposals (edn/read-string (slurp "test/fixtures/want-interp-library/M-futon-seams-interpretations@futon2-78439f58.edn")))
(def argue :exit/hac75428b9c97)
(def document :exit/h54d16050a9dc)
(def by-want {document :writing-coherence/meet-the-reader-where-they-are
              argue :writing-coherence/plain-language-thesis})

(defn- reply-for [id]
  (str "Here is my reading.\n\n```edn\n"
       (with-out-str (pp/pprint (merge {:schema wi/response-schema :pattern id
                                        :receipt (get-in proposals [:interpretation-receipts id])}
                                       (get-in proposals [:patterns id]))))
       "```\n"))

(defn- request-options []
  (let [d (temp-dir "ask-code") code (io/file d "code.py") index (io/file d "index.json")]
    (spit code "# retriever fixture") (spit index "[]")
    {:resolve-fn (fn [_] {:id "M-futon-seams" :path (.getCanonicalPath (io/file "test/fixtures/mission-criteria/M-futon-seams@futon3c-20959e4f.md"))})
     :revision-fn (constantly "fixture-revision") :library-fn (constantly [])
     :retrieve-fn (fn [_] [{:pattern "writing-coherence/plain-language-thesis" :score 1}])
     :retriever-specs (mapv #(assoc % :implementation (.getCanonicalPath code)
                                    :index (.getCanonicalPath index)) ireq/retrievers)}))

(defn- seams-flight []
  (flight/start {:target "M-futon-seams" :chosen-because {:kind :requested}}
                {:kind :a-exits :repo "futon3c" :path "holes/missions/M-futon-seams.md"
                 :read-text (fn [& _] mission-text)
                 :observe #(checks/decl-present? mission-text (:decl %))}
                {:id "flight-ask-parallel"}))

(def tick-sources {:beta-by-context {:WM {:beta 1}}})

(defn- ask [store answer-fn & [opts]]
  (let [f (seams-flight)
        wants (flight/click-wants f tick-sources)]
    ((fr/ask-fn (merge {:store (.getCanonicalPath store) :answer-fn answer-fn
                        :code-root (.getCanonicalPath (io/file "test/fixtures/want-interp-library"))
                        :request-options (request-options)}
                       opts))
     f wants tick-sources)))

;; ---------------------------------------------------------------------------
;; (a) the wants are asked concurrently

(deftest asks-run-concurrently-and-all-publish
  (testing "the fixture's two wants, each answered after a 1 s seat round
            trip, settle in ~one round trip, not their sum"
    (let [store (temp-dir "ask-store")
          in-flight (atom 0) max-in-flight (atom 0)
          answer (fn [issued]
                   (swap! in-flight inc)
                   (swap! max-in-flight max @in-flight)
                   (Thread/sleep 1000)
                   (swap! in-flight dec)
                   {:seat "kimi-6" :job-id (str "job-" (:request-id issued)) :state "done"
                    :text (reply-for (by-want (get-in issued [:want :token])))})
          start (System/nanoTime)
          r (ask store answer {:interpretation-ask-parallelism 3})
          elapsed-ms (/ (- (System/nanoTime) start) 1e6)
          published (wi/read-published (.getCanonicalPath store) "M-futon-seams")]
      (is (< elapsed-ms 1800)
          (str "ask step took " elapsed-ms " ms; sequential would be >= 2000 ms"))
      (is (= 2 @max-in-flight) "both asks were in flight together")
      (is (= [argue document] (mapv :want (:asked r))) "results keep want order")
      (is (= [:published :published] (mapv :outcome (:asked r))))
      (is (= #{:writing-coherence/meet-the-reader-where-they-are
               :writing-coherence/plain-language-thesis}
             (set (keys (:patterns published))))
          "the target file holds both interpretations"))))

(deftest parallelism-1-serialises-the-asks
  (testing "the bound is real: parallelism 1 makes the same two asks take
            the sum of their seat times (>= 2 s)"
    (let [store (temp-dir "ask-store")
          answer (fn [issued]
                   (Thread/sleep 1000)
                   {:seat "kimi-6" :job-id "job-1" :state "done"
                    :text (reply-for (by-want (get-in issued [:want :token])))})
          start (System/nanoTime)
          _ (ask store answer {:interpretation-ask-parallelism 1})
          elapsed-ms (/ (- (System/nanoTime) start) 1e6)]
      (is (>= elapsed-ms 2000)
          (str "parallelism 1 took only " elapsed-ms " ms")))))

;; ---------------------------------------------------------------------------
;; (b) concurrent publishes for one target

(defn- validated-pair
  "The first flight's two validated interpretations: DOCUMENT against the
  empty sources, ARGUE against the sources with DOCUMENT admitted (the owner
  constraint), exactly as the sequential test validates them."
  []
  (let [doc-resp {:pattern :writing-coherence/meet-the-reader-where-they-are}
        arg-resp {:pattern :writing-coherence/plain-language-thesis}
        ;; publish! only inspects :status :valid, the interpretation, the
        ;; want/target binding and the receipt — the fixture's full validated
        ;; maps come from want-interpretation-test's real validator; here the
        ;; concurrency property is under test, so a minimal :valid map with
        ;; the real interpretation bodies is sufficient.
        interp (fn [id] {id (assoc (get-in proposals [:patterns id])
                                   :receipt (get-in proposals [:interpretation-receipts id]))})
        v-doc {:status :valid :want document :target "M-futon-seams"
               :interpretation (interp :writing-coherence/meet-the-reader-where-they-are)}
        v-arg {:status :valid :want argue :target "M-futon-seams"
               :interpretation (interp :writing-coherence/plain-language-thesis)}]
    {:doc [doc-resp v-doc] :arg [arg-resp v-arg]}))

(defn- race-round!
  "Two threads publish the two interpretations for ONE fresh target store,
  released together by a latch; returns the store path."
  []
  (let [store (.getCanonicalPath (temp-dir "race-store"))
        {:keys [doc arg]} (validated-pair)
        latch (CountDownLatch. 1)
        run! (fn [[resp v]]
               (.await latch)
               (try (wi/publish! store (wi/issue! store {:target "M-futon-seams"
                                                         :want {:token (:want v)}})
                                 resp v)
                    (catch Throwable t (.getMessage t))))
        f1 (future (run! doc))
        f2 (future (run! arg))]
    (.countDown latch)
    @f1 @f2
    store))

(defn- pattern-count [store]
  (count (get-in (wi/read-published store "M-futon-seams") [:patterns])))

(deftest ten-concurrent-publishes-for-one-target-lose-nothing
  (testing "10 concurrent publish! calls (5 rounds x 2 threads, one target
            per round): every target ends with both interpretations"
    (let [stores (doall (take 5 (repeatedly race-round!)))]
      ;; 10 publish! calls total across the rounds; count the calls for the
      ;; record and require every store to hold both patterns
      (is (= [2 2 2 2 2] (mapv pattern-count stores))
          (str "pattern counts: " (mapv pattern-count stores))))))

(deftest plant-without-the-lock-publishes-lose-writes
  ;; PLANT: publish-lock redefined to a FRESH monitor per call, which is
  ;; removing the lock. Run enough rounds to be sure: 100 rounds below, and
  ;; the observed loss count is reported via the assertion message.
  (let [losses (atom 0)]
    (with-redefs-fn {#'wi/publish-lock (fn [_ _] (Object.))}
      (fn []
        (dotimes [_ 100]
          (let [store (race-round!)]
            (when (< (pattern-count store) 2)
              (swap! losses inc))))))
    ;; the unlocked RMW must lose at least one of the 100 rounds; if it ever
    ;; reaches 100/100 intact the lock is not what is protecting the writes
    (is (pos? @losses)
        (str "unlocked publish lost " @losses "/100 rounds; expected > 0"))))

;; ---------------------------------------------------------------------------
;; (c) pin! under a snapshot race

(defn- pin-pair!
  "Two threads pin! the same NEW source path concurrently; returns both
  results (or the throwable each saw)."
  [dir path]
  (let [latch (CountDownLatch. 1)
        pin (fn []
              (.await latch)
              (try (#'ireq/pin! dir path (constantly "fixture-rev"))
                   (catch Throwable t t)))
        f1 (future (pin)) f2 (future (pin))]
    (.countDown latch)
    [@f1 @f2]))

(deftest concurrent-pins-of-one-source-both-succeed
  (let [dir (temp-dir "snapshots")
        src (io/file dir "M-source.md")
        snaps (doto (io/file dir "snaps") (.mkdirs))]
    (spit src "# M-source\n\npinned once\n")
    (let [[a b] (pin-pair! (.getCanonicalPath snaps)
                           (.getCanonicalPath src))]
      (is (map? a) (str "first pin: " (pr-str a)))
      (is (map? b) (str "second pin: " (pr-str b)))
      (is (= (:snapshot a) (:snapshot b))
          "both threads share the winning snapshot")
      (is (= (get-in a [:source :sha256]) (get-in b [:source :sha256])))))

  (testing "a pre-existing snapshot with DIFFERENT bytes still refuses"
    ;; the snapshot's name embeds the source digest, so a same-name
    ;; mismatching snapshot means the file on disk was corrupted/replaced
    (let [dir (temp-dir "snapshots")
          src (io/file dir "M-other.md")
          snaps (.getCanonicalPath (doto (io/file dir "snaps") (.mkdirs)))]
      (spit src "# original bytes\n")
      (let [first (#'ireq/pin! snaps (.getCanonicalPath src)
                              (constantly "fixture-rev"))]
        (spit (io/file (:snapshot first)) "# corrupted snapshot bytes\n")
        (let [second (try (#'ireq/pin! snaps (.getCanonicalPath src)
                                      (constantly "fixture-rev"))
                          nil
                          (catch clojure.lang.ExceptionInfo e e))]
          (is (some? second))
          (is (= :interpretation/source-changed
                 (:interpretation/refusal (ex-data second)))))))))
