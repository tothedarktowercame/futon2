(ns futon2.aif.author-json-contract-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [futon2.aif.handoff-contract :as handoff]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.meta-terminal-injury :as injury]
            [futon2.aif.stop-line :as stop]
            [futon2.aif.tripwire :as tripwire]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.test-support.runner-fixture :as fixture]
            [futon2.aif.wm.cascade-runner-seam-test :as seam]
            [futon2.aif.wm.cascade-patch-delivery :as delivery]
            [futon2.aif.wm.open-cascade-resume :as resume]
            [futon2.aif.wm.authored.req-cfdb1f62 :as authored]))

;; Snapshot production before rebinding the hermetic injury ledger.
(use-fixtures :once fixture/with-hermetic-traces hermetic/with-hermetic-stores)
(def digest "309421670ca97dd1b1c73447bca9d818b93e2334d5d98d3c0208dd419b9336cb")
(def patch {:namespace "futon2.aif.wm.authored.req-cfdb1f62" :var "decisions"
            :source-digest "fe48324bdc0e11039484a434484c2ce1a12fa430e9b61fef8cd3c3ab32f2f894"
            :commit "bb1846638820488b1bd5494fbd714aa0213bfa66"})
(def raw (slurp (io/resource "fixtures/author-json-contract/response.txt")))
(def malformed {:state "done" :job-id "invoke-1793312635841-7821-b4aa5251" :result raw})
(def held {:kind :wm/open-cascade-execution-error :cascade-digest digest :phase :compile
           :holes (mapv (fn [id] {:hole-id id :owner :wm/enactment-compiler
                                  :question :operational-clause-owed}) (keys authored/clauses))
           :resume-port {:retry (fn [ctx] {:status :cascade-ready :cascade-digest digest
                                         :delivered (:authoring-product ctx)})}})
(defn answer [req]
  {:requisition-id (:requisition-id req) :cascade-digest digest :by "wm-author" :patch patch
   :decisions (mapv (fn [[id clause]] (assoc clause :hole id)) authored/clauses)})

(deftest retained-malformed-answer-corrects-once-and-reaches-real-function
  (let [contacts (atom []) reviews (atom [])
        check delivery/check-delivery
        handler (runtime/open-cascade-resume-handler
                 {:author "wm-author" :reviewer "wm-reviewer"
                  :open-cascade-author-answer-fn
                  (fn [req] (swap! contacts conj req)
                    (if (= 1 (count @contacts)) malformed
                      {:state "done" :job-id "corrected-author"
                       :result (json/generate-string (answer req))}))
                  :open-cascade-reviewer-answer-fn
                  (fn [{:keys [author-answer] :as req}]
                    (swap! reviews conj req)
                    (assoc author-answer :accepted-by "wm-reviewer"))})
        ;; Tooling classpath is this worktree. Point ONLY the gate's resource
        ;; resolver at canonical bytes; retain its real git + SHA checks.
        result (with-redefs [delivery/check-delivery
                             (fn [a] (check delivery/canonical-repository
                                      #(-> (io/file delivery/canonical-repository "src" %) .toURI .toURL) a))]
                 (handler held))]
    (is (nil? (handoff/response-map malformed)))
    (is (re-find #"was expecting double-quote to start field name" (handoff/response-parse-error malformed)))
    (is (= 2 (count @contacts)))
    (is (= 1 (get-in @contacts [1 :format-correction :limit])))
    (is (= (handoff/response-parse-error malformed)
           (get-in @contacts [1 :format-correction :parser-error])))
    (is (.contains (#'runtime/open-cascade-author-prompt (second @contacts))
                   (handoff/response-parse-error malformed)))
    (is (.contains (#'runtime/open-cascade-author-prompt (first @contacts))
                   "JSON array"))
    (is (= 1 (count @reviews)))
    (is (= :resumed (:status result)))
    (is (= authored/clauses (get-in result [:result :delivered])))
    (is (true? (get-in result [:answer-delivery :claim-matches?])))
    (is (empty? (get-in result [:answer-delivery :author-response-defects])))
    (is (= 1 (get-in result [:answer-delivery :format-correction :round])))))

(deftest legacy-map-claim-and-array-claim-compare-to-function
  (let [req (resume/authoring-requisition (resume/recognize held) "wm-author" "wm-reviewer")
        a (answer req)
        legacy (assoc a :decisions (into {} (map (fn [[k v]] [(pr-str k) v])) authored/clauses))]
    (doseq [value [a legacy]]
      (let [decoded (handoff/response-json-object (json/generate-string value))
            result (resume/validate-product req decoded (authored/decisions {:cascade-digest digest :phase :compile}))]
        (is (= :accepted (:status result)))
        (is (true? (get-in result [:answer-delivery :claim-matches?])))))
    (is (= :author-response/decisions-mismatch
           (get-in (resume/validate-product req
                     (assoc-in a [:decisions 0 :operation] :incorrect)
                     authored/clauses) [:answer-delivery :author-response-defects 0 :kind])))))

(defn exhausted []
  (let [calls (atom 0)
        result ((runtime/open-cascade-resume-handler
                 {:author "wm-author" :reviewer "wm-reviewer"
                  :open-cascade-author-answer-fn (fn [_] (swap! calls inc) malformed)
                  :open-cascade-reviewer-answer-fn (fn [_] (throw (ex-info "review contacted" {})))}) held)]
    (is (= 2 @calls))
    (is (= :author-response-unparseable (:kind result)))
    result))

(deftest exhausted-format-is-non-injury-and-apparatus-errors-stay-injuries
  (let [result (exhausted)
        record {:run/id "replay-39a845a3" :click/id "retained-b8777274"
                :failure {:kind (:kind result) :stage :cascade-resume}
                :terminal-receipt {:outcome :guardrail-refusal}}
        pin {:path "tooling-replay.edn" :sha256 (apply str (repeat 64 "a"))}]
    (is (= :non-injury (:classification (injury/classify record pin))))
    (is (= :injury (:classification (injury/classify (assoc-in record [:failure :kind] :hot-load-failed) pin))))
    (is (stop/no-finding-required? {:outcome :guardrail-refusal :failure-kind (:kind result)}))
    (is (empty? (tripwire/evaluate-wire :T3 {:phase :opportunity :transition :end
                                           :tripwire/force? true :outcome :guardrail-refusal
                                           :failure-kind (:kind result)})))))

(deftest runner-closes-format-refusal-without-repair-finding
  (let [result (exhausted)
        writes (atom [])
        store (hermetic/runner-repair-options)
        click (#'seam/click! [seam/px seam/py]
                (merge store
                  {:click-id "fixture-author-json-refusal"
                   :repair-system-record-fn #(swap! writes conj %)
                   :cascade-reading-pins {"M-current" {:excerpt-digest (#'seam/hex [:excerpt])
                                                       :analysis-digest (#'seam/hex [:analysis])}}
                   :cascade-occurrence-readings (fn [_] seam/square-readings)
                   :open-cascade-resume-fn (constantly result)})
                {:task {:id "M-current"}
                 :bind-cascade-identity {:readings (fn [_] seam/square-readings)}})]
    (is (= :guardrail-refusal (get-in click [:result :outcome])))
    (is (= :author-response-unparseable (get-in click [:result :data :failure-kind])))
    (is (empty? @writes))
    (is (= :non-injury (get-in click [:result :meta-terminal-injury-ingest :classification :classification])))
    (prn :format-refusal-replay
         (select-keys (:result click) [:outcome :run-record :meta-terminal-injury-ingest]))))
