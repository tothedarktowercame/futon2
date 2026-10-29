(ns futon2.aif.wm.verified-answer-delivery-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.reading-policy-class-history-index :as history-index]
            [futon2.test-support.runner-fixture :as fixture]
            [futon2.aif.wm.open-cascade-resume :as resume]
            [futon2.aif.wm.authored.req-f0ba279a :as authored]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)

(defn frozen [name]
  (edn/read-string (slurp (str "test/resources/wm/run001-answer-delivery/" name ".edn"))))

(deftest frozen-run001-reaches-construction
  (let [f (frozen "selection")
        ;; The durable projection retains the chosen action in selection-law.
        ;; Restore that exact action into the judge carrier; do not reselect.
        d (:decision f)
        decision (assoc d :action (get-in d [:selection-law :per-policy-argmax :action]))
        events (atom [])
        constructed (atom nil)
        contacts (atom [])
        root (.toFile (java.nio.file.Files/createTempDirectory "answer-delivery-"
                                    (make-array java.nio.file.attribute.FileAttribute 0)))
        handler (runtime/open-cascade-resume-handler
                  {:author "wm-author" :reviewer "wm-reviewer"
                   :open-cascade-author-answer-fn
                   (fn [req] (swap! contacts conj [:author (:requisition-id req)])
                     {:state "done" :result (:response (frozen "author"))})
                   :open-cascade-reviewer-answer-fn
                   (fn [req] (swap! contacts conj [:reviewer (get-in req [:requisition :requisition-id])])
                     {:state "done" :result (:response (frozen "reviewer"))})})
        result (binding [runner/*wm-status-reporting?* false]
                 (with-redefs [runner/refuse-on-runner-source-drift! (fn [] nil)
                               ;; Exclude unrelated history-index tree walker: on this
                               ;; frozen carrier it compares keyword :schema to string
                               ;; keys in a sorted map. Durable record writing stays real.
                               history-index/append-run-record! (fn [& _] nil)]
                   (runner/run-opportunity!
                     (merge (fixture/isolated-runner-opts)
                       {:run-id "frozen-run001-answer-delivery"
                        :author "wm-author" :reviewer "wm-reviewer"
                        :roster-fn (fn [_] {:wm-author {:status "idle" :invoke-ready? true}
                                            :wm-reviewer {:status "idle" :invoke-ready? true}
                                            :codex-1 {:status "idle" :invoke-ready? true}})
                        :judge-fn (fn [_] {:judgement {:decision decision :belief {} :belief-pre {}
                                                       :observation {} :free-energy {} :prediction-errors {}
                                                       :precision-state {} :micro-step-trace [] :mode :maintain}})
                        :open-cascade-resume-fn handler
                        :executable-cascade-root (str (io/file root "cascades"))
                        :execution-journal-root (str (io/file root "journal"))
                        :pattern-library-root (str (io/file root "library"))
                        :phase-log-fn #(swap! events conj %)
                        :construct-fn (fn [entry]
                                        (let [construction (runner/construct-for-decision entry)]
                                          (reset! constructed construction)
                                          ;; Authorized witness ends at construction, before dispatch.
                                          (throw (ex-info "Frozen witness reached construction"
                                                   {:outcome :incomplete :failure-stage :construction
                                                    :failure-kind :test/construction-witness-stop}))))
                        :dispatch-fn (fn [& _] (throw (ex-info "Unexpected external dispatch" {})))}))))
        delivery (get-in result [:checkpoints :selection :judgment :answer-delivery])
        records (filter #(.isFile %) (file-seq (io/file runner/default-run-record-dir)))
        record (some (fn [p] (when (= (.getName p) "tick-run-record-frozen-run001-answer-delivery.edn")
                              (let [r (edn/read-string (slurp p))]
                                (when (= "frozen-run001-answer-delivery" (or (:run/id r) (:run-id r))) r)))) records)]
    (println "FROZEN-WITNESS" (pr-str {:record-sha (:record-sha256 f)
                                      :contacts @contacts :construction? (some? @constructed)
                                      :delivery delivery :outcome (:outcome result)
                                      :phases (mapv #(select-keys % [:phase :transition :outcome :status :kind]) @events)}))
    (is (= 2 (count @contacts)) "one author and one reviewer, no correction round")
    (is (some? @constructed) "real runner entered construction using the frozen selection")
    (is (some #(and (= :cascade-resume (:phase %)) (= :end (:transition %))
                    (= :resumed (:status %))) @events)
        "the real resume compiler returned cascade-ready before construction")
    (is (= 9 (count (:answered delivery))))
    (is (= :author-response/decisions-not-operational
           (get-in delivery [:author-response-defects 0 :kind])))
    (is (= delivery (:answer-delivery record)) "typed JSON defect survives durable run record")
    (is (some? (:answer-delivery record)))))

(deftest missing-function-answer-remains-unresolved
  (let [id (first (keys authored/clauses))
        req {:requisition-id "frozen-control"
             :holes (mapv #(hash-map :hole-id %) (keys authored/clauses))}
        result (resume/validate-product req {:decisions authored/clauses}
                                        (dissoc authored/clauses id))]
    (is (= :answer-holes-unresolved (:kind result)))
    (is (= [id] (get-in result [:repair :missing-ids])))))

(deftest identity-validation-still-precedes-loading
  (let [loads (atom 0)
        digest "af6294ee687ca69fac2cb0ddd7e75c92cccf38e716a5082f3a3333d41e218f91"
        error {:kind :wm/open-cascade-execution-error :cascade-digest digest
               :phase :compile :holes (mapv #(hash-map :hole-id %) (keys authored/clauses))
               :resume-port {:retry (fn [_] (throw (ex-info "must not resume" {})))}}
        req (resume/authoring-requisition (resume/recognize error) "wm-author" "wm-reviewer")
        result (resume/handle-error! (resume/new-ledger "identity-control") error
                   "wm-author" "wm-reviewer"
                   {:requisition-id (:requisition-id req) :cascade-digest digest
                    :by "wm-author" :accepted-by "unreviewed"
                    :decisions {:holes-resolved 9}
                    :patch {:namespace "futon2.aif.wm.authored.req-f0ba279a" :var "decisions"
                            :source-digest "713729efbe46fc72aedfb69158773291033bb11cd6a9d664ce8ae2a23517affd"}}
                   {:patch-loader (fn [_] (swap! loads inc) authored/decisions)})]
    (is (= :answer-reviewer-mismatch (:kind result)))
    (is (zero? @loads))))

(deftest json-claim-compares-in-transport-value-domain
  (let [req {:requisition-id "claim-control" :holes [{:hole-id ["pattern" 0]}]}
        product {["pattern" 0] {:operation :apply :capability :wm/pattern-apply
                                :completion-check [:observed "result"]}}
        claim {["pattern" 0] {:operation "apply" :capability "wm/pattern-apply"
                              :completion-check ["observed" "result"]}}
        result (resume/validate-product req {:decisions claim} product)]
    (is (true? (get-in result [:answer-delivery :claim-matches?])))
    (is (empty? (get-in result [:answer-delivery :author-response-defects])))))
