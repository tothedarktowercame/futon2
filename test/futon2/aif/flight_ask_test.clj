(ns futon2.aif.flight-ask-test
  "D11 part 4: the flight asks for the interpretations its wants lack. The
  real case is the first flight: M-futon-seams at futon3c 20959e4f (mission
  da2ac70e, after claude-1 retracted the ARGUE -> DOCUMENT dependency), whose
  open exits are ARGUE and DOCUMENT; the answering seat is stubbed with
  kimi-6's readings (futon2 66a1779e), in the reply grammar."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.interpretation-request :as ireq]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.want-interpretation :as wi])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def roots (atom []))
(use-fixtures :each
  (fn [f]
    (try (f) (finally
               (doseq [root @roots file (reverse (file-seq root))] (Files/delete (.toPath file)))
               (reset! roots [])))))

(defn- temp-dir [prefix]
  (let [d (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0)))]
    (swap! roots conj d) d))

(def mission-file "test/fixtures/mission-criteria/M-futon-seams@futon3c-20959e4f.md")
(def mission-text (slurp mission-file))
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

(defn- stub-answer [text-fn]
  (let [n (atom 0)]
    (fn [issued]
      {:seat "kimi-6" :job-id (str "job-" (swap! n inc)) :state "done"
       :text (text-fn (get-in issued [:want :token]))})))

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
                {:id "flight-ask"}))

(def tick-sources {:beta-by-context {:WM {:beta 1}}})

(defn- ask [store answer-fn & [declared]]
  (let [f (seams-flight)
        wants (flight/click-wants f tick-sources)]
    ((fr/ask-fn (cond-> {:store (.getCanonicalPath store) :answer-fn answer-fn
                         ;; pinned library bytes (see want-interpretation-test)
                         :code-root (.getCanonicalPath (io/file "test/fixtures/want-interp-library"))
                         :request-options (request-options)}
                  declared (assoc :constraints declared)))
     f wants tick-sources)))

(deftest parse-reply-grammar
  (is (= :writing-coherence/x (get-in (wi/parse-reply (str "```edn\n" (pr-str {:schema wi/response-schema :pattern :writing-coherence/x}) "\n```")) [:response :pattern])))
  (is (= {:reason :none} (:decline (wi/parse-reply (str "```edn\n" (pr-str {:schema wi/response-schema :decline {:reason :none}}) "\n```")))))
  (testing "not exactly one form of the schema is unparseable, never a decline"
    (is (:unparseable-response (wi/parse-reply "no fences here")))
    (is (:unparseable-response (wi/parse-reply (str (reply-for :writing-coherence/plain-language-thesis)
                                                    (reply-for :writing-coherence/plain-language-thesis)))))
    (is (:unparseable-response (wi/parse-reply "```edn\n{:schema :something-else}\n```")))
    (is (:unparseable-response (wi/parse-reply "```edn\n{:schema :wm/want-interpretation-response-v1\n```")))))

(deftest the-first-flight-asks-and-both-exits-become-constructible
  (let [store (temp-dir "ask-store")
        r (ask store (stub-answer #(reply-for (by-want %))))
        published (wi/read-published (.getCanonicalPath store) "M-futon-seams")]
    (testing "the reader lists ARGUE before DOCUMENT; ARGUE settles by revalidation"
      (is (= [argue document] (mapv :want (:asked r))))
      (is (= [:published :published] (mapv :outcome (:asked r))))
      (is (= :revalidation (:published-on (first (:asked r))))))
    (is (empty? (:needs r)))
    (is (= #{:writing-coherence/meet-the-reader-where-they-are :writing-coherence/plain-language-thesis}
           (set (keys (:patterns published)))))
    (is (= {:seat "kimi-6" :job-id "job-1"}
           (get-in published [:receipts :writing-coherence/plain-language-thesis :answered-by])))))

(deftest what-is-not-a-publication-is-a-need
  (testing "unparseable, with the job id"
    (let [r (ask (temp-dir "ask-store") (stub-answer (constantly "I think plain-language-thesis fits.")))]
      (is (= #{:unparseable-response} (set (map :kind (:needs r)))))
      (is (every? :job-id (:needs r)))))
  (testing "a job that did not finish is not answered, not declined"
    (let [r (ask (temp-dir "ask-store") (fn [_] {:seat "kimi-6" :job-id "j" :state "failed" :text nil}))]
      (is (= #{:not-answered} (set (map :kind (:needs r)))))))
  (testing "a typed decline: a need with its job id, and nothing published"
    (let [store (temp-dir "ask-store")
          r (ask store
                 (stub-answer (constantly (str "```edn\n" (pr-str {:schema wi/response-schema
                                                                   :decline {:reason :no-library-pattern}}) "\n```"))))]
      (is (= #{:declined} (set (map :kind (:needs r)))))
      (is (every? :job-id (:needs r)))
      (is (nil? (wi/read-published (.getCanonicalPath store) "M-futon-seams"))))))

(deftest agency-answer-fn-bells-then-polls
  (let [calls (atom [])
        af (fr/agency-answer-fn {:seat "kimi-6" :opts {:agency-base "http://x"}
                                 :dispatch! (fn [_ seat caller mission prompt]
                                              (swap! calls conj [:bell seat caller mission (str/includes? prompt "REPLY GRAMMAR")
                                                                 (str/starts-with? prompt "Requisition: M-futon-seams — ")])
                                              {:job-id "job-9"})
                                 :poll! (fn [_ id] (swap! calls conj [:poll id]) {:state "done" :job-id id})
                                 :job-text (constantly "reply text")})
        a (af {:target "M-futon-seams" :request-id "request-1" :want {:token argue}})]
    (is (= [[:bell "kimi-6" "wm-flight" "M-futon-seams" true true] [:poll "job-9"]] @calls)
        "requisition is the mission, on the prompt's first line (Kimi seats refuse without it); the prompt states the grammar")
    (is (= {:seat "kimi-6" :job-id "job-9" :state "done" :text "reply text"
            ;; row 3: no library root in the flight's opts is recorded, typed
            :library-root {:absent :not-in-flight-opts}} a))))

;; claude-2, answering an M-f11 coverage request (2026-09-24): the dispatch
;; sent :coverage and :constraints requests the want-interpretation grammar,
;; whose reply read-one then discards as unparseable
(deftest each-request-kind-is-sent-the-grammar-its-reply-is-read-in
  (let [sent (atom nil)
        af (fr/agency-answer-fn {:seat "claude-2" :opts {}
                                 :dispatch! (fn [_ _ _ _ prompt] (reset! sent prompt) {:job-id "j"})
                                 :poll! (fn [_ id] {:state "done" :job-id id})
                                 :job-text (constantly "")})
        grammar (fn [kind]
                  (af {:kind kind :target "M-f11" :request-id "r" :want {:token :t}})
                  (second (re-find #"REPLY GRAMMAR: exactly one fenced ```edn block holding \{:schema (\S+)" @sent)))]
    (is (= ":wm/criteria-response-v1" (grammar :coverage)))
    (is (str/includes? @sent ":scope-outs"))
    (is (= ":wm/constraints-response-v1" (grammar :constraints)))
    (is (= ":wm/locator-response-v1" (grammar :locator)))
    (is (= ":wm/criteria-response-v1" (grammar :criteria)))
    (is (not (str/includes? (do (grammar nil) @sent) "D11 part 5")) "a want interpretation keeps its own prompt")))

(deftest the-loop-asks-before-each-click
  (let [asks (atom 0)
        f (flight/run! (seams-flight)
                       {:ask-fn (fn [_ _ _] (swap! asks inc) {:asked [] :needs [{:kind :declined :want argue}]})
                        :click-fn (fn [_] {:click-id "c"})
                        :observe-fn (fn [_ _] {})
                        :sources-fn (constantly tick-sources)
                        :max-clicks 3})]
    (is (= 1 @asks) "the click advanced nothing, so the flight stopped after one")
    (is (= [{:kind :declined :want argue}] (:needs f)))
    (is (= 1 (:before-click (first (:asks f)))))))

(deftest a-declared-constraint-must-be-stated-in-the-text
  ;; claude-1 retracted ARGUE -> DOCUMENT (futon3c 20959e4f): a declared edge
  ;; restating it now refuses, because the text no longer states it
  (let [answer (stub-answer #(reply-for (by-want %)))]
    (is (= :want/declared-constraint-not-in-text
           (try (ask (temp-dir "ask-store") answer [{:want argue :requires document :by "claude-1"}]) nil
                (catch clojure.lang.ExceptionInfo e (:interpretation/refusal (ex-data e))))))
    (testing "with no declared constraint the same readings publish"
      (is (empty? (:needs (ask (temp-dir "ask-store") answer)))))))

(deftest a-construction-exception-is-a-refusal-never-a-request
  ;; WM-SPIKE-FIX-I C: the spike's last three wants hit an ExceptionInfo with
  ;; no :interpretation/refusal; the refusal was nil, ask-one fell through and
  ;; {::refused nil} went to claude-5 as the request (request-id nil)
  (let [asked-seat (atom [])
        r (with-redefs [wi/issue! (fn [& _] (throw (ex-info "evidence write failed" {:path "/x"})))]
            (ask (temp-dir "ask-store") (fn [issued] (swap! asked-seat conj issued)
                                          {:seat "kimi-6" :job-id "j" :state "done" :text ""})))]
    (is (= [] @asked-seat) "the seat is never asked")
    (is (= [:request-refused :request-refused] (mapv :outcome (:asked r))))
    (is (= {:kind :construction-threw :class "clojure.lang.ExceptionInfo"
            :message "evidence write failed" :data-keys [:path]}
           (:refusal (first (:asked r)))))
    (is (every? #(= :request-refused (:kind %)) (:needs r)))))

(deftest a-construction-exception-keeps-its-kind-and-its-causes
  ;; WM-SPIKE-FIX-II E: mission-registry throws {:kind :substrate-unreachable}
  ;; with the substrate's exception as cause; the second flight's record kept
  ;; only the outer message
  (let [root (java.net.SocketTimeoutException. "Read timed out")
        mid (ex-info "futon1b entities query failed" {:url "http://localhost:7073/api/entities"} root)
        outer (ex-info "substrate-2 mission registry unreachable" {:kind :substrate-unreachable} mid)
        r (with-redefs [wi/issue! (fn [& _] (throw outer))]
            (ask (temp-dir "ask-store") (fn [_] (throw (ex-info "the seat must not be asked" {})))))
        refusal (:refusal (first (:asked r)))]
    (is (= :construction-threw (:kind refusal)))
    (is (= :substrate-unreachable (:ex-kind refusal)))
    (is (= "substrate-2 mission registry unreachable" (:message refusal)))
    (is (= [{:class "clojure.lang.ExceptionInfo" :message "futon1b entities query failed"}
            {:class "java.net.SocketTimeoutException" :message "Read timed out"}]
           (:cause refusal)))))

;; ---------------------------------------------------------------------------
;; WM-SPIKE-FIX-III: no exception escapes a flight step unrecorded. The third
;; flight (flight-74325007) died in the ask step on the Agency job poll's
;; HttpTimeoutException and wrote no record. Live pin: its error report.

(def third-flight-error
  (edn/read-string {:default tagged-literal}
                   (slurp "/home/joe/code/futon3c/holes/labs/M-wm-wiring/spike/driver-error-report-flight-74325007.edn")))

(defn- pinned-timeout []
  (let [{:clojure.error/keys [class cause]} (:clojure.main/triage third-flight-error)]
    (assert (= 'java.net.http.HttpTimeoutException class))
    (java.net.http.HttpTimeoutException. cause)))

(deftest an-ask-that-throws-is-an-entry-and-the-flight-goes-on
  (let [store (temp-dir "ask-store")
        calls (atom 0) clicks (atom 0)
        stub (stub-answer #(reply-for (by-want %)))
        answer (fn [issued] (if (= 2 (swap! calls inc)) (throw (pinned-timeout)) (stub issued)))
        f (flight/run! (seams-flight)
                       {:ask-fn (fr/ask-fn {:store (.getCanonicalPath store) :answer-fn answer
                                            :code-root (.getCanonicalPath (io/file "test/fixtures/want-interp-library"))
                                            :request-options (request-options)})
                        :click-fn (fn [_] (swap! clicks inc) {:click-id "c1" :abstention {:kind :no-admitted-interpretation}})
                        :observe-fn (fn [_ _] {})
                        :sources-fn (constantly tick-sources)
                        :max-clicks 1})
        asked (:asked (first (:asks f)))
        threw (first (filter #(= :ask-threw (:outcome %)) asked))]
    (is (= 2 (count asked)))
    (is (not= :ask-threw (:outcome (first asked))) "the first want's entry stands")
    (is (= {:kind :ask-threw :class "java.net.http.HttpTimeoutException" :message "request timed out"}
           (select-keys (:refusal threw) [:kind :class :message])))
    (is (some #(= :ask-threw (:kind %)) (:needs f)) "and it is a need")
    (is (= 1 @clicks) "the flight reached its click")
    (is (not= :aborted (:status f)))))

(deftest an-answer-poll-failure-keeps-the-job-id
  (let [answer (fr/agency-answer-fn {:seat "claude-5" :dispatch! (fn [& _] {:job-id "job-p"})
                                     :poll! (fn [& _] (throw (pinned-timeout)))})
        r (with-redefs [wi/issue! (fn [_ req] (assoc req :request-id "r1"))]
            (ask (temp-dir "ask-store") answer))]
    (is (every? #(= "job-p" (:job-id %)) (:asked r)))
    (is (= "java.net.http.HttpTimeoutException" (get-in r [:asked 0 :refusal :class]))
        "the refusal names the poll's failure, not the wrapper")))

(deftest a-step-that-throws-aborts-with-the-record-kept
  (let [e (try (flight/run! (seams-flight)
                            {:ask-fn (fn [_ _ _] {:asked [{:want argue :outcome :declined}] :needs []})
                             :click-fn (fn [_] (throw (pinned-timeout)))
                             :observe-fn (fn [_ _] {})
                             :sources-fn (constantly tick-sources)
                             :max-clicks 1})
               nil
               (catch clojure.lang.ExceptionInfo e e))
        aborted (flight/aborted-flight e)]
    (is (some? e) "run! rethrows")
    (is (= :aborted (:status aborted)))
    (is (= {:step :click :class "java.net.http.HttpTimeoutException" :message "request timed out"}
           (select-keys (:aborted aborted) [:step :class :message])))
    (is (= [{:want argue :outcome :declined}] (:asked (first (:asks aborted)))) "the asks before it are kept")
    (is (instance? java.net.http.HttpTimeoutException (ex-cause e)))))
