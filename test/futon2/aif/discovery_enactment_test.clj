(ns futon2.aif.discovery-enactment-test
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]))

(def construction
  (edn/read-string
   (slurp "test/fixtures/narrative-trace/construction-1789964661.edn")))

(def prompt-opts
  {:author "author" :reviewer "reviewer"
   :target-repository "/repo" :target-repository-head "head"
   :repository-plan {:allowed []}})

(def discovery-construction
  (assoc construction :outer-enactment
         {:mode :discovery :outer-cascade-id :find-out-before-building
          :selection-sha "selection-pin"}))

(deftest discovery-prompt-is-gated-and-implementation-is-unchanged
  (let [target (:mission construction)
        mission {:id target}
        old-prompt (#'runner/author-prompt prompt-opts target mission
                                           discovery-construction [])
        off-prompt (runner/author-enactment-prompt
                    {:wm/discovery-enactment :off} prompt-opts target mission
                    discovery-construction [])
        on-prompt (runner/author-enactment-prompt
                   {:wm/discovery-enactment :on} prompt-opts target mission
                   discovery-construction [])
        implementation (assoc discovery-construction :outer-enactment
                              {:mode :implementation
                               :outer-cascade-id :giraffe-order})]
    (is (= old-prompt off-prompt))
    (is (str/includes? on-prompt "FULL-LOOP DISCOVERY OPPORTUNITY"))
    (is (str/includes? on-prompt "FULL_LOOP_DISCOVERY:"))
    (is (str/includes? on-prompt "read-only task"))
    (is (not (str/includes? on-prompt "REPOSITORY ARTIFACT CONTRACT")))
    (is (str/includes? on-prompt "Do not emit FULL_LOOP_ARTIFACTS"))
    (is (str/includes? on-prompt "do not emit FULL_LOOP_AUTHOR: DONE"))
    (doseq [switch [:on :off]]
      (is (= (#'runner/author-prompt prompt-opts target mission implementation [])
             (runner/author-enactment-prompt
              {:wm/discovery-enactment switch} prompt-opts target mission
              implementation []))))
    (is (= :discovery-enactment-option-required
           (:failure-kind
            (try
              (runner/author-enactment-prompt
               {} prompt-opts target mission discovery-construction [])
              (catch clojure.lang.ExceptionInfo e (ex-data e))))))))

(defn- wire [item]
  (str "FULL_LOOP_DISCOVERY: "
       (pr-str {:schema :wm/find-out-result-v1
                :target "M-example" :items [item]})))

(def good-question
  {:question "Which reading applies?"
   :span {:lines [4 5] :quote "ambiguous mission text"}
   :alternatives ["reading A" "reading B"]})

(deftest discovery-result-parser-types-malformed-items
  (is (= :accepted
         (:status (runner/parse-discovery-result
                   (wire {:verdict :current
                          :evidence ["holes/M-example.md:25-33"]})))))
  (doseq [[label item kind]
          [["missing verdict" {:evidence ["x:1"]}
            :find-out-result/verdict-missing]
           ["unknown verdict" {:verdict :maybe :evidence ["x:1"]}
            :find-out-result/verdict-unknown]
           ["missing evidence" {:verdict :current :evidence []}
            :find-out-result/evidence-missing]
           ["blocked without questions" {:verdict :blocked :evidence ["x:1"]}
            :find-out-result/blocked-questions-missing]]]
    (testing label
      (is (= kind (:kind (runner/parse-discovery-result (wire item)))))))
  (is (= :accepted
         (:status (runner/parse-discovery-result
                   (wire {:verdict :blocked :evidence ["x:1"]
                          :questions [good-question]}))))))

(def accepted-result
  {:schema :wm/find-out-result-v1 :target "M-example"
   :items [{:item "A" :verdict :current :evidence ["holes/M-example.md:4"]}
           {:item "B" :verdict :blocked :evidence ["holes/M-example.md:8-9"]
            :questions [good-question]}]})

(deftest discovery-review-is-required-and-rejections-retain-reasons
  (let [base {:target "M-example"
              :enactment-pin (:outer-enactment discovery-construction)
              :parsed {:status :accepted :result accepted-result}
              :before-head {:repo "/repo" :head "same"}
              :after-head {:repo "/repo" :head "same"}}
        unreviewed (runner/discovery-receipt
                    (assoc base :review {:status :not-dispatched}))
        rejected (runner/discovery-receipt
                  (assoc base :review {:status :reviewed :verdict :reject
                                       :reasons ["x:4 does not support current"]}))
        accepted (runner/discovery-receipt
                  (assoc base :review {:status :reviewed :verdict :accept}))]
    (is (= :unreviewed (:status unreviewed)))
    (is (empty? (:later-build-candidates unreviewed)))
    (is (= :review-rejected (:status rejected)))
    (is (= ["x:4 does not support current"]
           (get-in rejected [:review :reasons])))
    (is (= :accepted (:status accepted)))
    (is (= ["A"] (mapv :item (:later-build-candidates accepted))))
    (is (= (:outer-enactment discovery-construction)
           (:selection-enactment accepted)))))

(deftest discovery-review-prompt-and-verdict-are-typed
  (let [prompt (runner/discovery-reviewer-prompt
                {:author "author" :reviewer "reviewer"} "M-example"
                (:outer-enactment discovery-construction) accepted-result)]
    (is (str/includes? prompt "file:line locator"))
    (is (str/includes? prompt "SELECTION ENACTMENT PIN"))
    (is (str/includes? prompt "not code review"))
    (is (= {:status :reviewed :verdict :accept}
           (runner/parse-discovery-review
            "FULL_LOOP_DISCOVERY_REVIEW: ACCEPT")))
    (is (= {:status :reviewed :verdict :reject :reasons ["citation fails"]}
           (runner/parse-discovery-review
            "FULL_LOOP_DISCOVERY_REVIEW: REJECT {:reasons [\"citation fails\"]}")))
    (is (= :find-out-review/reasons-missing
           (:kind (runner/parse-discovery-review
                   "FULL_LOOP_DISCOVERY_REVIEW: REJECT {:reasons []}"))))))

(deftest discovery-round-reviews-without-implementation-and-preserves-head
  (let [dispatches (atom [])
        heads (atom 0)
        opts {:dispatch-fn
              (fn [_opts agent caller target prompt]
                (swap! dispatches conj {:agent agent :caller caller
                                        :target target :prompt prompt})
                {:job-id "review-job"})
              :poll-fn
              (fn [_opts job-id]
                (is (= "review-job" job-id))
                {:job-id job-id :state "done"
                 :result "FULL_LOOP_DISCOVERY_REVIEW: ACCEPT"})
              :repo-head-observation-fn
              (fn [repo]
                (swap! heads inc)
                {:repo repo :head "unchanged" :observed-at-ms @heads})}
        before {:repo "/repo" :head "unchanged" :observed-at-ms 0}
        author-job {:job-id "author-job" :state "done"
                    :result (str "FULL_LOOP_DISCOVERY: " (pr-str accepted-result))}
        receipt (runner/run-discovery-review!
                 opts {} (atom 1) prompt-opts "reviewer" "M-example"
                 discovery-construction "/repo" before author-job)]
    (is (= :accepted (:status receipt)))
    (is (true? (get-in receipt [:target-repository-head :unchanged?])))
    (is (= 1 @heads))
    (is (= 1 (count @dispatches)))
    (is (= "wm-full-loop-discovery-review" (:caller (first @dispatches))))
    (is (not (str/includes? (:prompt (first @dispatches))
                            "FULL-LOOP IMPLEMENTATION OPPORTUNITY")))))

(deftest reviewed-discovery-receipt-is-retained-in-the-run-record
  (let [dir (.toFile
             (java.nio.file.Files/createTempDirectory
              "discovery-receipt" (make-array java.nio.file.attribute.FileAttribute 0)))
        receipt (runner/discovery-receipt
                 {:target "M-example"
                  :enactment-pin (:outer-enactment discovery-construction)
                  :parsed {:status :accepted :result accepted-result}
                  :review {:status :reviewed :verdict :accept}
                  :before-head {:repo "/repo" :head "same"}
                  :after-head {:repo "/repo" :head "same"}})
        written (runner/persist-run-record!
                 {:run-record-dir (.getPath dir)
                  :wm/discovery-enactment :on
                  :scan-render-fn (fn [& _] nil)}
                 "discovery-receipt-test" "2026-11-06T00:00:00Z"
                 {:outcome :incomplete
                  :data {:failure-kind :discovery-close-unavailable
                         :discovery-receipt receipt}})
        record (edn/read-string (slurp (:run-record written)))]
    (is (= :on (:discovery-enactment record)))
    (is (= receipt (:discovery-receipt record)))
    (is (= (:outer-enactment discovery-construction)
           (get-in record [:discovery-receipt :selection-enactment])))))
