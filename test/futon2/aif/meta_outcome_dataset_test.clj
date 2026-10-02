(ns futon2.aif.meta-outcome-dataset-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-outcome-dataset :as dataset]))

(defn- utf8 [x] (.getBytes (pr-str x) "UTF-8"))

(def records
  {"/runs/one.edn"
   {:run/id "run-1" :click/id "click-1"
    :startedAt "2026-09-19T00:54:11.109006657Z"
    :outer-task-selection
    {:schema :wm/outer-task-selection-v1
     :policy {:kind :seeded-uniform-task-support}
     :support [] :excluded [] :draw {:seed 1}
     :chosen {:id "M-one" :kind :mission}
     :action {:target "M-one" :type :advance-mission}}
    :decision {:selection-law {:per-policy-argmax {:action {:target "M-one"}}}}
    :terminal-receipt {:outcome :grounded-change}
    :registered-run/timing {:status :complete :wall-clock-ms 120
                            :phase-timings-ms {:selection 10}}
    :registered-run/model-usage {:status :complete
                                 :jobs [{:input-tokens 10 :output-tokens 5
                                         :total-tokens 15}]}
    :run-output {:status :present} :traceWritten true
    :d-task-enactment {:target "M-one"}}
   "/runs/two.edn"
   {:run/id "run-2" :click/id "click-2"
    :startedAt "2026-09-19T00:00:11.302351062Z"
    :outer-task-selection {:schema :wm/outer-task-selection-v1
                           :status :refused :reason :identity-inconsistent}
    :decision {:selection-law {:per-policy-argmax {:action {:target "M-one"}}}}
    :terminal-receipt {:outcome :grounded-progress}
    :registered-run/timing {:status :typed-missing}
    :registered-run/model-usage {:status :partial
                                 :jobs [{:input-tokens 2 :output-tokens 3
                                         :total-tokens 5}]}
    :traceWritten false}
   "/runs/no-target.edn"
   {:run/id "run-3" :click/id "click-3"
    :terminal {:outcome :incomplete} :traceWritten false}})

(defn- read-bytes [path] (utf8 (records path)))

(defn- authority []
  (let [manifest (dataset/manifest ["/runs/one.edn" "/runs/two.edn"
                                    "/runs/no-target.edn"] read-bytes)]
    {:manifest manifest :expected-manifest-pin (:source-pin manifest)
     :read-bytes read-bytes}))

(deftest extracts-only-explicit-measurements-and-repetition
  (let [a (authority) result (dataset/produce a)
        by-run (into {} (map (juxt :run/id identity) (:rows result)))
        one (by-run "run-1") two (by-run "run-2")]
    (is (= :produced (:status result)))
    (is (= {:inputs 3 :usable 2 :excluded 1
            :task-kinds {:mission 2}
            :terminal-classes {:closure 1 :grounded-progress 1}
            :availability {:elapsed-ms 1 :token-use 1
                           :closure-or-progress 2 :repetition 1}}
           (:counts result)))
    (is (= {:status :present :value 120 :unit :milliseconds
            :source [:registered-run/timing :wall-clock-ms]}
           (:elapsed one)))
    (is (= {:status :present :value 15 :unit :tokens
            :source [:registered-run/model-usage :jobs :total-tokens]}
           (:token-use one)))
    (is (= {:status :absent :reason :not-explicitly-recorded} (:elapsed two)))
    (is (= {:status :absent :reason :not-explicitly-recorded} (:token-use two)))
    (is (= ["run-2" "run-1"] (mapv :run/id (:rows result))))
    (is (= {:prior-occurrences 0 :position 1} (:repetition two)))
    (is (= {:prior-occurrences 1 :position 2} (:repetition one)))
    (is (true? (get-in one [:quality :outer-selection-retained])))
    (is (= :canonical-outer (get-in one [:quality :selected-identity-class])))
    (is (false? (get-in two [:quality :outer-selection-retained])))
    (is (= :legacy-inner-fallback
           (get-in two [:quality :selected-identity-class])))
    (is (= :outer-task-identity-missing (get-in result [:exclusions 0 :reason])))
    (is (= :verified (:status (dataset/verify result a))))
    (is (= :external-run-record-authority-required
           (:reason (dataset/verify result))))))

(deftest authoritative-reconstruction-catches-semantic-mutations
  (let [a (authority) result (dataset/produce a)]
    (doseq [[label mutate]
            [[:dropped-row #(update % :rows pop)]
             [:task-swap #(assoc-in % [:rows 0 :task :id] "E-forged")]
             [:unit-change #(assoc-in % [:rows 0 :elapsed :unit] :seconds)]
             [:outcome-relabel #(assoc-in % [:rows 0 :terminal-outcome :class]
                                          :abstention-or-failure)]
             [:proof-rewrite #(-> % (assoc :rows [])
                                  (assoc :exclusions [])
                                  (assoc :counts {:inputs 0 :usable 0 :excluded 0})
                                  (assoc :source-pin {:path "wm://meta-outcome-dataset-v1"
                                                      :sha256 (apply str (repeat 64 "a"))}))]]]
      (testing (name label)
        (is (= :dataset-does-not-match-run-record-authority
               (:reason (dataset/verify (mutate result) a))))))))

(deftest chronology-and-outer-receipt-quality-fail-closed
  (let [base (select-keys records ["/runs/one.edn" "/runs/two.edn"])
        altered
        (-> base
            (assoc-in ["/runs/one.edn" :outer-task-selection]
                      {:schema :wm/outer-task-selection-v1
                       :status :absent :reason :receipt-not-retained}))
        reader #(utf8 (altered %))
        m (dataset/manifest (keys altered) reader)
        result (dataset/produce {:manifest m :expected-manifest-pin (:source-pin m)
                                 :read-bytes reader})]
    (is (= [:legacy-inner-fallback :legacy-inner-fallback]
           (mapv #(get-in % [:quality :selected-identity-class]) (:rows result))))
    (is (every? false? (map #(get-in % [:quality :outer-selection-retained])
                            (:rows result))))
    (doseq [[label started reason]
            [[:missing nil :start-instant-missing]
             [:malformed "yesterday" :start-instant-malformed]]]
      (testing (name label)
        (let [record (cond-> (records "/runs/one.edn")
                       true (dissoc :startedAt)
                       started (assoc :startedAt started))
              source {"/runs/time.edn" record}
              reader #(utf8 (source %))
              m (dataset/manifest (keys source) reader)
              result (dataset/produce
                      {:manifest m :expected-manifest-pin (:source-pin m)
                       :read-bytes reader})]
          (is (= reason (get-in result [:exclusions 0 :reason]))))))
    (let [same-time (assoc-in base ["/runs/two.edn" :startedAt]
                              (get-in base ["/runs/one.edn" :startedAt]))
          reader #(utf8 (same-time %))
          m (dataset/manifest (keys same-time) reader)]
      (is (= :record-start-instants-duplicated
             (:reason (dataset/produce
                       {:manifest m :expected-manifest-pin (:source-pin m)
                        :read-bytes reader})))))))

(deftest manifest-and-record-source-mutations-refuse
  (let [a (authority)]
    (is (= :manifest-paths-duplicated
           (:reason (dataset/produce
                     (let [m (dataset/manifest ["/runs/one.edn" "/runs/one.edn"] read-bytes)]
                       {:manifest m :expected-manifest-pin (:source-pin m)
                        :read-bytes read-bytes})))))
    (is (= :record-source-drift
           (:reason (dataset/produce
                     (assoc a :read-bytes
                            #(if (= % "/runs/two.edn")
                               (.getBytes "changed" "UTF-8") (read-bytes %)))))))
    (let [same-record {"/runs/copy-a.edn" (records "/runs/one.edn")
                       "/runs/copy-b.edn" (records "/runs/one.edn")}
          reader #(utf8 (same-record %))
          m (dataset/manifest (keys same-record) reader)]
      (is (= :record-identities-duplicated
             (:reason (dataset/produce {:manifest m
                                        :expected-manifest-pin (:source-pin m)
                                        :read-bytes reader})))))))

(deftest canonical-outcome-vocabulary-is-explicit
  (is (= :closure (dataset/outcome-class :grounded-change)))
  (is (= :closure (dataset/outcome-class :already-satisfied)))
  (is (= :grounded-progress (dataset/outcome-class :grounded-progress)))
  (is (= :typed-blocker (dataset/outcome-class :guardrail-refusal)))
  (is (= :abstention-or-failure (dataset/outcome-class :abstained)))
  (is (= :typed-unknown (dataset/outcome-class :future-outcome))))
