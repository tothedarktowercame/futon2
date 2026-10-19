(ns futon2.aif.meta-outcome-dataset-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-outcome-dataset :as dataset]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.outer-target-belief :as belief]
            [futon2.aif.outer-policy-provider-runtime]))

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
    :cascade-feedback
    {:mission-state
     {:execution-outcomes
      {:status :verified :receipt/id "want-receipt-2"
       :selected-wants [["M-one" :want/a]]
       :by-class {:reached [] :progressed [] :blocked []
                  :untouched [["M-one" :want/a]]}}}
     :artifact {:repo "/repos/work" :commit "abc123" :grounded? true}
     :patterns {:selected [:pattern/a]
                :applications [{:pattern :pattern/a :status :no-want-effect
                                :reinforcement :none}]
                :positive-reinforcement []}
     :status :verified
     :blocker {:status :absent :reason :verified-grounded-work}}
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
           (dissoc (:counts result) :record-read)))
    (is (= {:status :present :value 120 :unit :milliseconds
            :source [:registered-run/timing :wall-clock-ms]}
           (:elapsed one)))
    (is (= {:status :present :value 15 :unit :tokens
            :source [:registered-run/model-usage :jobs :total-tokens]}
           (:token-use one)))
    (is (= {:status :absent :reason :not-explicitly-recorded} (:elapsed two)))
    (is (= {:status :absent :reason :not-explicitly-recorded} (:token-use two)))
    (is (= {:reached 0 :progressed 0 :blocked 0 :untouched 1}
           (get-in two [:selected-want-effects :counts])))
    (is (= :grounded-collateral-progress
           (get-in two [:collateral-progress :kind])))
    (is (= {:status :present
            :source [:cascade-feedback :artifact]
            :repo "/repos/work" :commit "abc123"}
           (:grounded-artifact two)))
    (is (= :selected-want-accounting-not-recorded
           (get-in one [:selected-want-effects :reason])))
    (is (= {:status :present :source [:cascade-feedback :patterns]
            :selected 1 :positive 0 :no-want-effect 1}
           (:pattern-reinforcement two)))
    (is (= ["run-2" "run-1"] (mapv :run/id (:rows result))))
    (is (= {:prior-occurrences 0 :position 1} (:repetition two)))
    (is (= {:prior-occurrences 1 :position 2} (:repetition one)))
    (is (true? (get-in one [:quality :outer-selection-retained])))
    (is (= :canonical-outer (get-in one [:quality :selected-identity-class])))
    (is (= :task-outcome (get-in one [:terminal-cause :class])))
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

(deftest unknown-typed-terminal-kind-is-not-task-evidence
  (let [cause (dataset/terminal-cause
               {:run-output {:outcome :abstained}
                :failure {:kind :novel-unreviewed-terminal-kind
                          :stage :selection}})]
    (is (= :unclassified (:class cause)))
    (is (= :terminal-cause-unclassified (:reason cause)))))

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

(deftest manifest-and-record-source-mutations-are-isolated
  (let [a (authority)]
    (is (= :manifest-paths-duplicated
           (:reason (dataset/produce
                     (let [m (dataset/manifest ["/runs/one.edn" "/runs/one.edn"] read-bytes)]
                       {:manifest m :expected-manifest-pin (:source-pin m)
                        :read-bytes read-bytes})))))
    (let [result (dataset/produce
                  (assoc a :read-bytes
                         #(if (= % "/runs/two.edn")
                            (.getBytes "changed" "UTF-8") (read-bytes %))))]
      (is (= :produced (:status result)))
      (is (= :record-source-drift (get-in result [:exclusions 0 :reason]))))
    (let [same-record {"/runs/copy-a.edn" (records "/runs/one.edn")
                       "/runs/copy-b.edn" (records "/runs/one.edn")}
          reader #(utf8 (same-record %))
          m (dataset/manifest (keys same-record) reader)]
      (is (= :record-identities-duplicated
             (:reason (dataset/produce {:manifest m
                                        :expected-manifest-pin (:source-pin m)
                                        :read-bytes reader})))))))

(deftest verified-records-survive-missing-and-drifted-manifest-neighbours
  (let [missing "/tmp/click-scan-16313651870851502188/tick-run-record-scan-success.edn"
        changed "/runs/changed.edn"
        sources {"/runs/one.edn" (records "/runs/one.edn")
                 changed (records "/runs/two.edn")}
        reader (fn [path]
                 (cond
                   (= path missing) (throw (java.nio.file.NoSuchFileException. path))
                   (= path changed) (utf8 (assoc (sources path) :run/id "changed-bytes"))
                   :else (utf8 (sources path))))
        manifest-body {:schema dataset/manifest-schema
                       :entries [{:path "/runs/one.edn"
                                  :sha256 (field/sha256 (utf8 (sources "/runs/one.edn")))}
                                 {:path missing :sha256 (apply str (repeat 64 "0"))}
                                 {:path changed
                                  :sha256 (field/sha256 (utf8 (sources changed)))}]}
        manifest (assoc manifest-body :source-pin
                        {:path "wm://run-record-manifest-v1"
                         :sha256 (field/digest manifest-body)})
        result (dataset/produce {:manifest manifest
                                 :expected-manifest-pin (:source-pin manifest)
                                 :read-bytes reader})]
    (is (= :produced (:status result)))
    (is (= ["run-1"] (mapv :run/id (:rows result))))
    (is (= [missing changed] (mapv #(get-in % [:source :path]) (:exclusions result))))
    (is (= [:record-missing :record-source-drift]
           (mapv :reason (:exclusions result))))
    (is (= {:declared 3 :readable 2 :verified 1 :excluded 2
            :missing 1 :drifted 1
            :reasons {:record-missing 1 :record-source-drift 1}}
           (get-in result [:counts :record-read])))
    (is (= (records "/runs/one.edn")
           (:verified-record (first (:rows result)))))))

(deftest provider-terminal-projection-uses-the-verified-record-not-the-path
  (let [row {:source {:path "/tmp/click-scan-missing/record.edn"
                      :sha256 (apply str (repeat 64 "0"))}
             :verified-record (records "/runs/one.edn")
             :terminal-outcome {:class :closure}
             :terminal-cause {:class :task-outcome}}
        terminal-record (ns-resolve 'futon2.aif.outer-policy-provider-runtime
                                    'terminal-record)
        result (terminal-record row)]
    (is (= "run-1" (:run/id result)))
    (is (= :closure (:observed-outcome result)))
    (is (nil? (:selected-cascade-digest result)))))

(deftest canonical-outcome-vocabulary-is-explicit
  (is (= :closure (dataset/outcome-class :grounded-change)))
  (is (= :closure (dataset/outcome-class :already-satisfied)))
  (is (= :grounded-progress (dataset/outcome-class :grounded-progress)))
  (is (= :typed-blocker (dataset/outcome-class :guardrail-refusal)))
  (is (= :abstention-or-failure (dataset/outcome-class :abstained)))
  (is (= :typed-unknown (dataset/outcome-class :future-outcome))))

(deftest terminal-cause-boundary-pins-apparatus-and-ambiguous-evidence
  (let [selected (records "/runs/one.edn")
        rs {"/runs/resume.edn"
            (-> selected
                (assoc :run/id "resume" :click/id "resume-click"
                       :startedAt "2026-09-20T00:00:00Z"
                       :terminal-receipt {:outcome :incomplete
                                          :failure-kind :resume-protocol-violation}
                       :failure {:kind :resume-protocol-violation
                                 :stage :cascade-resume
                                 :detail {:repairable true}}))
            "/runs/phase.edn"
            (-> selected
                (assoc :run/id "phase" :click/id "phase-click"
                       :startedAt "2026-09-20T00:00:01Z"
                       :terminal-receipt {:outcome :incomplete :failure-kind :phase-failure}
                       :failure {:kind :phase-failure :stage :selection}))
            "/runs/untyped.edn"
            (-> selected
                (assoc :run/id "untyped" :click/id "untyped-click"
                       :startedAt "2026-09-20T00:00:02Z"
                       :terminal-receipt {:outcome :incomplete :failure-kind :untyped-failure}
                       :failure {:kind :untyped-failure :stage :cascade-execution}))
            "/runs/no-target.edn"
            {:run/id "no-target" :click/id "no-target-click"
             :startedAt "2026-09-20T00:00:03Z"
             :terminal-receipt {:outcome :incomplete :failure-kind :phase-failure}
             :failure {:kind :phase-failure :stage :selection}}}
        reader #(utf8 (rs %))
        m (dataset/manifest (keys rs) reader)
        result (dataset/produce {:manifest m :expected-manifest-pin (:source-pin m)
                                 :read-bytes reader})
        evidence (:interoceptive-evidence result)]
    (is (= 3 (count (:rows result))))
    (is (= 4 (count evidence)))
    (is (= #{:apparatus-failure :unclassified}
           (set (map #(get-in % [:cause :class]) evidence))))
    (is (= #{"resume" "phase" "untyped" "no-target"}
           (set (map :run/id evidence))))
    (is (every? #(re-matches #"[0-9a-f]{64}" (:digest %)) evidence))
    (is (= :outer-task-identity-missing (get-in result [:exclusions 0 :reason])))
    (is (= :apparatus-failure
           (:class (dataset/terminal-cause (rs "/runs/resume.edn")))))
    (is (= :unclassified
           (:class (dataset/terminal-cause (rs "/runs/untyped.edn")))))
    (is (= :task-outcome
           (:class (dataset/terminal-cause
                    (assoc selected :terminal-receipt {:outcome :guardrail-refusal}
                           :failure {:kind :task-failure :stage :enactment})))))))

(deftest same-coarse-incomplete-is-task-evidence-only-with-a-retained-task-cause
  (let [selected (records "/runs/one.edn")
        ;; These are temporary producer fixtures, not claims about a retained L2
        ;; record.  The repository has no canonical L2 terminal record available
        ;; to this test; the field below exercises the supported apparatus kind.
        rows {"/runs/task.edn"
              (-> selected
                  (assoc :run/id "task" :click/id "task-click"
                         :startedAt "2026-09-20T01:00:00Z"
                         :terminal-receipt {:outcome :incomplete}
                         :failure {:kind :task-failure :stage :enactment}))
              "/runs/l2.edn"
              (-> selected
                  (assoc :run/id "l2" :click/id "l2-click"
                         :startedAt "2026-09-20T01:00:01Z"
                         :terminal-receipt {:outcome :incomplete}
                         :failure {:kind :phase-failure :stage :loop-node}))}
        reader #(utf8 (rows %))
        manifest (dataset/manifest (keys rows) reader)
        produced (dataset/produce {:manifest manifest
                                   :expected-manifest-pin (:source-pin manifest)
                                   :read-bytes reader})
        posterior (belief/build {:dataset produced :prior (belief/prior 1)})]
    (is (= [:abstention-or-failure :abstention-or-failure]
           (mapv #(get-in % [:terminal-outcome :class]) (:rows produced))))
    (is (= [:task-outcome :apparatus-failure]
           (mapv #(get-in % [:terminal-cause :class]) (:rows produced))))
    (is (= 2 (get-in posterior [:targets "M-one" :alpha :typed-blocker]))
        "only the retained :task-failure advances the same target's alpha")
    (is (= {:count 1 :identities [["l2" "l2-click"]]
            :reasons {:apparatus-failure-not-task-evidence 1}}
           (get-in posterior [:non-task-observations :apparatus])))))
