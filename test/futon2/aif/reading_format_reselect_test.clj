(ns futon2.aif.reading-format-reselect-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [futon2.aif.handoff-contract :as handoff]
            [futon2.aif.outer-state :as outer-state]
            [futon2.aif.outer-select :as outer-select]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.selected-target-reading :as reading]
            [futon2.aif.selected-target-reading-test :as existing]
            [futon2.aif.target-reading-registry :as registry]
            [futon2.test-support.runner-fixture :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]))
(use-fixtures :once fixture/with-hermetic-traces hermetic/with-hermetic-stores)
(def raw (slurp (io/resource "fixtures/reading-format-reselect/response.txt")))
(def real-response {:state "done" :job-id "invoke-1793315962107-7916-14ee8762" :result raw})

(deftest retained-response-receives-one-format-correction-and-publishes
  (let [work (#'existing/temp-dir "reading-format-")
        source (io/file work "M-selected.md")
        _ (spit source "# M-selected\n\n## HEAD\nOPEN work.\n")
        excerpt (registry/excerpt-digest source)
        contacts (atom [])
        result (reading/repair!
                {:run-id "format" :author "wm-author" :request-root (str work)
                 :target-reading-root (str (io/file work "readings"))
                 :answer-fn (fn [request]
                              (swap! contacts conj request)
                              (if (= 1 (count @contacts)) real-response
                                  {:analysis (#'existing/analysis-for (:request request))}))}
                (#'existing/judgement "M-selected" (str source) excerpt))]
    (is (= 2 (count @contacts)))
    (is (= :published (:status result)))
    (is (re-find #"No JSON object was found" (reading/prompt (second @contacts))))
    (is (= 1 (get-in result [:format-correction :limit])))
    (is (re-find #"Unrecognized token 'Good'" (get-in @contacts [1 :format-correction :parser-error])))))

(deftest retained-response-exhaustion-reselects-once-and-retains-both-refusals
  (let [work (#'existing/temp-dir "reading-reselect-")
        source (io/file work "M-typed-memories.md")
        _ (spit source "# M-typed-memories\n\n## HEAD\nOPEN work.\n")
        excerpt (registry/excerpt-digest source)
        j (#'existing/judgement "M-typed-memories" (str source) excerpt)
        contacts (atom []) reselected (atom []) reading-state (atom nil)
        result (runner/selected-target-reading-step
                {:run-id "reselect" :author "wm-author" :request-root (str work)
                 :selected-target-reading/state reading-state
                 :target-reading-root (str (io/file work "readings"))
                 :answer-fn (fn [request] (swap! contacts conj request) real-response)
                 :selected-target-reading-reselect-fn
                 (fn [refusal _]
                   (swap! reselected conj refusal)
                   (#'existing/judgement "M-next" (str source) excerpt))}
                j reading/repair! (fn [_] (throw (ex-info "Must not publish" {}))))]
    (is (= 4 (count @contacts)))
    (is (= 1 (count @reselected)))
    (is (= :selected-target-reading-unavailable (:kind (first @reselected))))
    (is (= "M-next" (get-in result [:judgement :task :id])))
    (is (= 2 (count (get-in result [:record :reading-attempts]))))
    (is (= (:record result) (:record @reading-state)))
  (is (nil? (handoff/response-map real-response))))
)

(deftest frozen-field-replacement-updates-all-selection-carriers
  (let [{:keys [observation state relation-classes]}
        (edn/read-string (slurp (io/resource "fixtures/reading-format-reselect/outer.edn")))
        observation (assoc observation :relation (fn [_ t] (get relation-classes t)))
        makeup (outer-state/load-makeup)
        first-choice (outer-select/select makeup state observation)
        selection-state (atom {:task {:id "M-typed-memories"} :outer/selection first-choice})
        persisted (atom [])
        opts {:reading-reselection/state selection-state}
        port (runtime/reading-reselection-port makeup state observation first-choice
                                               selection-state #(swap! persisted conj %))
        refusal {:target "M-typed-memories" :kind :selected-target-reading-unavailable
                 :detail {:reader-job-ids ["7916" "correction"]}}
        replacement (port refusal)
        retained (:outer/selection @selection-state)
        target (get-in retained [:selection :selected :target])]
    (is (= "M-typed-memories" (get-in first-choice [:selection :selected :target])))
    (is (= "M-peradam-grounding" target))
    (is (= target (:target replacement)))
    (is (= target (get-in (reading/current-options opts) [:task :id])))
    (is (= retained (first @persisted)))
    (is (= retained (:outer/selection (reading/current-options opts))))
    (is (some #(and (= "M-typed-memories" (:target %))
                    (= :selected-target-reading-unavailable (:reason %)))
              (:outer-field/excluded retained)))
    (is (not (some #{"M-typed-memories"} (:outer-field/admitted retained))))
    (is (= first-choice (get-in retained [:reading-reselection :previous-selection])))
    (is (nil? (port refusal)))
    (is (= 1 (count @persisted)))
    (println "frozen replacement" target (:prediction retained))))

(deftest exhausted-first-target-then-published-second-target-does-not-abstain
  (let [work (#'existing/temp-dir "reading-next-publishes-")
        source (io/file work "M-selected.md")
        _ (spit source "# M-selected\n\n## HEAD\nOPEN work.\n")
        excerpt (registry/excerpt-digest source)
        contacts (atom [])
        result (runner/selected-target-reading-step
                {:run-id "next-publishes" :author "wm-author" :request-root (str work)
                 :target-reading-root (str (io/file work "readings"))
                 :answer-fn (fn [req]
                              (swap! contacts conj (:target req))
                              (if (= "M-typed-memories" (:target req)) real-response
                                  {:analysis (#'existing/analysis-for (:request req))}))
                 :selected-target-reading-reselect-fn
                 (fn [_ _] (#'existing/judgement "M-next" (str source) excerpt))}
                (#'existing/judgement "M-typed-memories" (str source) excerpt)
                reading/repair!
                (fn [record] {:task {:id (:target record)}
                              :decision {:status :selected :action {:target (:target record)}}}))]
    (is (= ["M-typed-memories" "M-typed-memories" "M-next"] @contacts))
    (is (= :published (get-in result [:record :status])))
    (is (= :selected (get-in result [:judgement :decision :status])))
    (is (= "M-next" (get-in result [:judgement :task :id])))
    (is (= :selected-target-reading-unavailable
           (get-in result [:record :reading-attempts 0 :kind])))))
