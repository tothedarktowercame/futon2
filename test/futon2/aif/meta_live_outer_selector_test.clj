(ns futon2.aif.meta-live-outer-selector-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-live-outer-selector :as live]))

(defn pin [path ch] {:path path :sha256 (apply str (repeat 64 ch))})
(def snapshot
  {:schema :wm/pipeline-cascade-snapshot-v1
   :summary-source (pin "summary" "a") :graph-source (pin "graph" "b")
   :summary {:consistent? true
             :standards {:s1-regenerates true :s2-evidence true
                         :s3-reconstitution true :s4-honest-holes true
                         :s5-composed true}}
   :graph {:section-status {:clusters {:status :ok}}
           :lineage [] :clusters [{:mission "M-a"} {:mission "M-b"}]
           :arrows [] :held []
           :tickets {:items [{:stem "M-a" :mtime-ms 200}
                             {:stem "M-b" :mtime-ms 100}]}
           :patterns {:edges []}}})
(def tasks
  [{:id "M-a" :kind :mission :priority 2 :source (pin "M-a.md" "c")}
   {:id "M-b" :kind :mission :priority 8 :source (pin "M-b.md" "d")}])

(deftest live-adapter-selects-and-translates-without-a-draw
  (let [receipt (live/select-live {:tasks tasks :fetch-snapshot (constantly snapshot)})]
    (is (= :wm/outer-task-selection-v1 (:schema receipt)))
    (is (= :meta-live-pipeline-task-state (get-in receipt [:policy :kind])))
    (is (= "M-a" (get-in receipt [:chosen :id])))
    (is (= {:type :advance-mission :target "M-a"} (:action receipt)))
    (is (= {:absent :deterministic-meta-policy} (:draw receipt)))
    (is (= :minimum-pairwise-task-state-G
           (get-in receipt [:policy :meta-selection :reason])))
    (is (contains? (set (get-in receipt [:policy :meta-selection
                                         :pairwise 0 :shared-channels]))
                   :pipeline-freshness-cost))))

(deftest missing-or-equal-evidence-refuses-with-complete-support
  (testing "missing is absence"
    (let [receipt (live/select-live {:tasks (mapv #(dissoc % :priority) tasks)
                                     :fetch-snapshot (constantly snapshot)
                                     :candidate-fn
                                     (fn [task-rows _]
                                       (mapv (fn [{:keys [id kind]}]
                                               {:id id :kind kind
                                                :support {:automated-feasibility :unknown}
                                                :channels {}})
                                             task-rows))})]
      (is (= :shared-current-channel-unavailable
             (get-in receipt [:policy :meta-selection :reason])))
      (is (= 2 (count (:support receipt))))
      (is (nil? (:action receipt)))))
  (testing "ties do not acquire a hidden first-item rule"
    (let [equal-snapshot (assoc-in snapshot [:graph :tickets :items 1 :mtime-ms] 200)
          receipt (live/select-live {:tasks (mapv #(assoc % :priority 1) tasks)
                                     :fetch-snapshot (constantly equal-snapshot)})]
      (is (= :no-unique-task-state-minimum
             (get-in receipt [:policy :meta-selection :reason])))
      (is (nil? (:action receipt))))))

(deftest partial-live-snapshot-remains-a-typed-outer-refusal
  (let [receipt (live/select-live
                 {:tasks tasks
                  :fetch-snapshot #(assoc-in snapshot
                                             [:graph :section-status :clusters :status]
                                             :failed)})]
    (is (= :pipeline-snapshot-invalid
           (get-in receipt [:policy :meta-selection :reason])))
    (is (= 2 (count (:support receipt))))
    (is (nil? (:action receipt)))))

(deftest off-map-tasks-are-accounted-as-typed-exclusions
  (let [off-map {:id "M-z" :kind :mission :priority 0
                 :source (pin "M-z.md" "e")}
        receipt (live/select-live {:tasks (conj tasks off-map)
                                   :fetch-snapshot (constantly snapshot)})]
    (is (= "M-a" (get-in receipt [:chosen :id])))
    (is (= ["M-z"] (mapv :id (:excluded receipt))))
    (is (= :pipeline/not-on-current-map
           (get-in receipt [:excluded 0 :ineligible-reason])))
    (is (= 3 (+ (count (:support receipt)) (count (:excluded receipt)))))))
