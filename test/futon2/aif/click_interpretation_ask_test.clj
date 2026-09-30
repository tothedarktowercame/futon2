(ns futon2.aif.click-interpretation-ask-test
  "PROOF-2b: an ordinary War Machine click asks for an interpretation by
  itself. When the tick's decision abstains refusing a candidate target
  :no-admitted-interpretation, the click asks ONCE for that target through
  the runner's :interpretation-ask-fn (production: wm.click-ask/click-ask-fn,
  the same flight-runner ask step run-flight! uses), and, when the ask
  published, re-runs the decision once in the same click so the target can be
  selected. The ask is recorded on the run record under :interpretation-ask
  whether or not it published; a tick that selected something never asks."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.interpretation-request :as ireq]
            [futon2.aif.policy :as policy]
            [futon2.aif.want-interpretation :as wi]
            [futon2.aif.wm.click-ask :as click-ask]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.test-support.runner-fixture :as fixture])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false] (f))))

(def target "M-futon-seams")
(def mission-file "test/fixtures/mission-criteria/M-futon-seams@futon3c-20959e4f.md")
(def mission-text (slurp mission-file))
(def proposals (edn/read-string
                (slurp "test/fixtures/want-interp-library/M-futon-seams-interpretations@futon2-78439f58.edn")))
(def document :exit/h54d16050a9dc)
(def instatiate :exit/h4ef5c183bc55)

(defn- reply-for [id]
  (str "Here is my reading.\n\n```edn\n"
       (with-out-str (pp/pprint (merge {:schema wi/response-schema :pattern id
                                        :receipt (get-in proposals [:interpretation-receipts id])}
                                       (get-in proposals [:patterns id]))))
       "```\\n"))

(defn- request-options []
  (let [d (.toFile (Files/createTempDirectory "click-ask-code" (make-array FileAttribute 0)))
        code (io/file d "code.py") index (io/file d "index.json")]
    (spit code "# retriever fixture") (spit index "[]")
    {:resolve-fn (fn [_] {:id target :path (.getCanonicalPath (io/file mission-file))})
     :revision-fn (constantly "fixture-revision") :library-fn (constantly [])
     :retrieve-fn (fn [_] [{:pattern "writing-coherence/meet-the-reader-where-they-are" :score 1}])
     :retriever-specs (mapv #(assoc % :implementation (.getCanonicalPath code)
                                    :index (.getCanonicalPath index)) ireq/retrievers)}))

;; the mission-hole-wants entry the click's ask derives wants from, in the
;; tick's own shape (ask-demo.clj): one open want (ARGUE), both exits in the
;; universe so the fixture pattern's guard stays checkable
;; the want's :stated must occur verbatim in the mission file
;; (want-interpretation/citation-for locates the criterion by its text)
(def document-criterion-text
  "DOCUMENT asks for something unrelated")

(def pick {:target target
           :want [document]
           ;; INSTANTIATE already reads Met in the fixture mission (ee86811c),
           ;; so meet-the-reader's guard (needs INSTANTIATE, forbids DOCUMENT)
           ;; is satisfiable and its candidate reaches the DOCUMENT want alone
           :universe {document false instatiate true}
           :locators {document {:class :C4 :repo "futon3c" :sha "HEAD"
                                :path "holes/missions/M-futon-seams.md"
                                :decl document-criterion-text}
                      instatiate {:class :C4 :repo "futon3c" :sha "HEAD"
                                  :path "holes/missions/M-futon-seams.md"
                                  :decl "INSTANTIATE-1 complete for instance 4"}}
           :holes [{:id "document" :kind :checkbox :line 507
                    :text document-criterion-text}]})

(def tick-sources {:beta-by-context {:WM {:beta 1}}})

(defn- test-ask-fn
  "The REAL click ask step (wm.click-ask/click-ask-fn driving
  flight-runner/ask-fn) with hermetic store/sources and a stubbed seat."
  [store answer-fn]
  (click-ask/click-ask-fn
   {:machine-interpretations-dir (.getCanonicalPath store)
    :interpretation-answer-fn answer-fn
    :mission-hole-sources [pick]
    :cascade-sources tick-sources
    :ask-options {:code-root (.getCanonicalPath (io/file "test/fixtures/want-interp-library"))
                  :request-options (request-options)}}))

(defn- temp-dir [prefix]
  (.getPath (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0)))))

(def abstained-decision
  {:status :abstained
   :refusals [{:target target :kind :no-admitted-interpretation
               :missing :interpretations}]})

(def ^:private selected-action
  {:kind :cascade-candidate :cascade-id :test/ask-selected :id :test/ask-selected
   :target target :precedence [:writing-coherence/meet-the-reader-where-they-are]
   :construction-receipt {:cascade/id :test/ask-selected :moves 1
                          :family-searched :unit :coverage 1}
   :interpretation-receipts [{:pattern :writing-coherence/meet-the-reader-where-they-are
                              :admitted-by :test-suite}]})

(defn- selected-decision []
  (policy/select-action-cascades
   [{:action selected-action :controller-score -2.0 :rank 1}
    {:action (assoc selected-action :cascade-id :test/other :id :test/other
                    :precedence [:writing-coherence/other])
     :controller-score -1.0 :rank 2}]
   {:beta 2.0}))

(defn- judgement-for [decision]
  {:decision decision :belief {} :belief-pre {} :observation {} :free-energy {}
   :prediction-errors {} :precision-state {} :micro-step-trace [] :mode :maintain})

(defn- run-click
  "One ordinary click with a counting judge-fn (first call ABSTAINED-decision,
  every later call SECOND, default the SELECTED-decision) and the given
  ask-fn. SECOND is a zero-arg fn returning the decision or throwing."
  ([ask-fn] (run-click ask-fn (fn [] (selected-decision))))
  ([ask-fn second]
   (let [judge-calls (atom [])
         findings (atom [])
         result (runner/run-opportunity!
                 (merge (fixture/isolated-runner-opts)
                        {:judge-fn (fn [days]
                                     (let [n (count (swap! judge-calls conj days))]
                                       {:judgement (judgement-for
                                                    (if (= 1 n) abstained-decision (second)))}))
                         :interpretation-ask-fn ask-fn
                         :repair-system-record-fn (fn [m] (swap! findings conj m)
                                                    {:repair/id (str "repair-test-" (count @findings))
                                                     :repair/class (:repair-class m)})
                         :dispatch-fn (fn [& _] (throw (ex-info "Unexpected dispatch" {})))}))]
     {:result result :judge-calls @judge-calls :findings @findings
      :record (edn/read-string (slurp (:run-record result)))})))

(deftest an-abstained-tick-asks-then-selects-in-the-same-click
  ;; (a) the only candidate refused :no-admitted-interpretation: the click
  ;; asks exactly once, the answer publishes into the hermetic store, and the
  ;; re-run decision selects the same target
  (let [store (io/file (temp-dir "click-ask-store"))
        answers (atom 0)
        answer-fn (fn [_issued]
                    (swap! answers inc)
                    {:seat "kimi-6" :job-id (str "job-" @answers) :state "done"
                     :text (reply-for :writing-coherence/meet-the-reader-where-they-are)})
        {:keys [record judge-calls]} (run-click (test-ask-fn store answer-fn))]
    (is (= 1 @answers) "the seat is asked exactly once (one want, one ask per click)")
    (is (= 2 (count judge-calls)) "the decision was re-run once after the ask published")
    (is (= {:target target :want document :outcome :published :job-id "job-1" :published true}
           (select-keys (:interpretation-ask record)
                        [:target :want :outcome :job-id :published]))
        "the ask is recorded on the run record")
    (is (contains? (get-in (wi/read-published (.getCanonicalPath store) target)
                           [:patterns :writing-coherence/meet-the-reader-where-they-are])
                   :guard)
        "the interpretation was published to the hermetic store")
    (is (= target (get-in record [:decision :chosen :target]))
        "the same tick then selects the refused target")))

(deftest a-declined-ask-leaves-the-tick-abstained
  ;; (b) a stubbed decline: no publication, no re-decide, the tick stays
  ;; abstained and :interpretation-ask records the decline
  (let [store (io/file (temp-dir "click-ask-store"))
        answer-fn (fn [_]
                    {:seat "kimi-6" :job-id "job-1" :state "done"
                     :text (str "```edn\n"
                                (pr-str {:schema wi/response-schema
                                         :decline {:reason "no reading fits"}})
                                "\n```")})
        {:keys [result record judge-calls]} (run-click (test-ask-fn store answer-fn))]
    (is (= 1 (count judge-calls)) "no second decision: nothing was published")
    (is (= :declined (:outcome (:interpretation-ask record))))
    (is (not (:published (:interpretation-ask record))))
    (is (= "job-1" (:job-id (:interpretation-ask record))))
    (is (nil? (wi/read-published (.getCanonicalPath store) target)))
    (is (= :abstained (get-in record [:decision :abstention :status])))
    (is (= :abstained (get-in result [:data :failure-kind])))))

(deftest a-selecting-tick-never-asks
  ;; (c) the decision selected something: the ask fn is never invoked
  (let [asks (atom 0)
        judge-calls (atom [])
        result (runner/run-opportunity!
                (merge (fixture/isolated-runner-opts)
                       {:judge-fn (fn [_] (swap! judge-calls conj :called)
                                    {:judgement (judgement-for (selected-decision))})
                        :interpretation-ask-fn (fn [& _] (swap! asks inc)
                                                 (throw (ex-info "must not be asked" {})))
                        :repair-system-record-fn (fn [m] {:repair/id "repair-test"
                                                          :repair/class (:repair-class m)})
                        :dispatch-fn (fn [& _] (throw (ex-info "Unexpected dispatch" {})))}))
        record (edn/read-string (slurp (:run-record result)))]
    (is (= 0 @asks) "the ask fn was never invoked")
    (is (= {:status :absent :reason :no-interpretation-ask}
           (:interpretation-ask record)))))

(defn- publishing-ask-fn
  "An ask whose stubbed seat publishes (the real ask step, hermetic store)."
  [store]
  (test-ask-fn store (fn [_] {:seat "kimi-6" :job-id "job-1" :state "done"
                              :text (reply-for :writing-coherence/meet-the-reader-where-they-are)})))

(deftest a-typed-refusal-of-the-redecision-is-the-ticks-typed-abstention
  ;; (d) click 15 (tick-run-record-2026-09-30-1790742842): the ask published,
  ;; then the re-decision refused the cascade decision typed and the click
  ;; closed :untyped-failure. The re-decision now goes through the same
  ;; judge-refusal handling as the first call, and the ask record survives.
  (let [store (io/file (temp-dir "click-ask-store"))
        {:keys [result record judge-calls]}
        (run-click (publishing-ask-fn store)
                   (fn [] (throw (ex-info "cascade decision refused"
                                          {:kind :class-unknown-no-scalar-g
                                           :target target}))))]
    (is (= 2 (count judge-calls)) "the ask published and the decision was re-run")
    (is (= :abstained (get-in result [:data :failure-kind]))
        "a typed refusal of the re-decision is the tick's typed abstention")
    (is (not= :untyped-failure (get-in result [:data :failure-kind])))
    (is (= :class-unknown-no-scalar-g
           (get-in record [:decision :abstention :targets 0 :kind])))
    (is (= true (:published (:interpretation-ask record))))
    (is (= "job-1" (:job-id (:interpretation-ask record))))))

(deftest an-untyped-redecision-failure-keeps-the-ask-record
  ;; (e) the re-decision threw untyped: the failure is recorded as before,
  ;; and the ask record is still on the run record
  (let [store (io/file (temp-dir "click-ask-store"))
        {:keys [result record]}
        (run-click (publishing-ask-fn store)
                   (fn [] (throw (RuntimeException. "boom"))))]
    (is (= :untyped-failure (get-in result [:data :failure-kind]))
        "an untyped re-decision failure is recorded as today")
    (is (= true (:published (:interpretation-ask record))))
    (is (= "job-1" (:job-id (:interpretation-ask record))))))
