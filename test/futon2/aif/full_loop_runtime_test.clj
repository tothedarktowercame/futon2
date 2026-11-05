(ns futon2.aif.full-loop-runtime-test
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-feedback :as cascade-feedback]
            [futon2.aif.action-identity :as action-identity]
            [futon2.aif.flight-runner :as flight-runner]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.meta-repair-coordinator :as coordinator]
            [futon2.aif.meta-self-heal-production :as meta-self-heal]
            [futon2.aif.meta-field-observation :as meta-field]
            [futon2.aif.outer-observation :as outer-observation]
            [futon2.aif.outer-state :as outer-state]
            [futon2.aif.placement-adapter :as placement-adapter]
            [futon2.aif.preference-registry :as preference-registry]
            [futon2.aif.ticket-readiness-inputs :as readiness-inputs]
            [futon2.aif.ticket-readiness :as ticket-readiness]
            [futon2.aif.seat-call-index :as seat-call-index]
            [futon2.aif.selected-reading :as selected-reading]
            [futon2.aif.selected-target-reading :as selected-target-reading]
            [futon2.aif.occurrence-square-authoring :as square-authoring]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.interpretation-request :as interpretation-request]
            [futon2.aif.mission-registry :as mission-registry]
            [futon2.aif.reading-policy-precision :as reading-precision]
            [futon2.aif.wm.cascade-decision :as cascade-decision]
            [futon2.aif.wm.open-cascade-resume :as open-resume]
            [futon2.report.war-machine :as wm]))

(defn- run-supplied-task! [opts]
  (runtime/run-opportunity! (merge {:task {:id "M-production"}} opts)))

(defn- digest [x] (evidence/value-digest x))

(deftest pending-learning-replays-the-selection-pinned-placements
  ;; Before R32-b the placed-gate witness reached outer-score and threw
  ;; :outer-score/placement-unavailable because this replay field had no
  ;; :placement keys (PlacementReplay.witness_refuses_today_scores_repaired).
  (let [root (io/file "test/fixtures/placement-replay")
        previous-dir (io/file root "giraffe/previous")
        current-dir (io/file root "giraffe/current")
        tsv (io/file root "placements.tsv")
        tsv-sha "30a6c848c8c4a8c93116ea3fea7e1bdb9a7d696b39b95dc3e5af7b739ba609fb"
        observation (edn/read-string
                     (slurp (io/file previous-dir "observation.edn")))
        makeup {:fixture :placement-replay}
        base-selection
        {:selection {:selected {:target "M-alpha"
                                :steps [:ready-gate :placed-gate]}}
         :selected-relation :focused}
        pending {:run-id "pending-run" :giraffe/run-id "previous"
                 :selection (assoc base-selection :placement/source
                                   {:status :pinned
                                    :tsv {:path (.getPath tsv)
                                          :sha256 tsv-sha}})}
        base-state {:pending pending}
        current-observation observation
        replayed (atom nil)
        resolve! (fn [state]
                   (#'runtime/resolve-pending-learning
                    {} makeup state current-dir current-observation))
        consume (fn [_ state _ replay _]
                  (reset! replayed replay)
                  (is (every? #(contains? % :placement)
                              (vals (:facts replay))))
                  {:status :applied :state (dissoc state :pending)})]
    (with-redefs-fn {#'runtime/pending-run-ending
                     (fn [_ run-id]
                       {:status :ready :run-id run-id :record-pin "fixture"
                        :ending-class :ordinary})
                     #'outer-observation/giraffe-field
                     (fn [_ _] observation)
                     #'outer-state/consume-observation consume}
      (fn []
       (let [applied (resolve! base-state)]
        (is (= :applied (:status applied)))
        (is (nil? (get-in applied [:state :pending]))))

      (let [changed (assoc-in base-state
                              [:pending :selection :placement/source :tsv :sha256]
                              (apply str (repeat 64 "f")))
            result (resolve! changed)]
        (is (= :applied (:status result)))
        (is (nil? (get-in result [:state :pending])))
        (is (= :typed-absence (get-in result [:placement-source :resolution]))))

      (let [missing (assoc-in base-state
                              [:pending :selection :placement/source :tsv :path]
                              (str root "/missing.tsv"))
            result (resolve! missing)
            next-click (resolve! (:state result))]
        (is (= :applied (:status result)))
        (is (nil? (get-in result [:state :pending])))
        (is (= :typed-absence (get-in result [:placement-source :resolution])))
        (is (= :none (:status next-click))))

      (let [unpinned (-> base-state
                         (update-in [:pending :selection] dissoc :placement/source)
                         (assoc-in [:pending :selection :selection :selected :steps]
                                   [:ready-gate]))
            result (resolve! unpinned)]
        (is (= :applied (:status result)))
        (is (nil? (get-in result [:state :pending]))))

      (let [selection-absent
            (assoc-in base-state [:pending :selection :placement/source]
                      {:status :absent :reason :placements-unavailable
                       :tsv {:path (.getPath tsv) :status :absent}})
            result (resolve! selection-absent)]
        (is (= :applied (:status result)))
        (is (every? #(= :absent (get-in % [:placement :status]))
                    (vals (:facts @replayed)))))

      (let [classes (io/file root "classes.tsv")
            classes-absent
            (assoc-in base-state [:pending :selection :placement/source :classes]
                      {:path (.getPath classes) :status :absent})
            result (resolve! classes-absent)]
        (is (= :applied (:status result)))
        (is (not-any? #(= :class (get-in % [:placement :source]))
                      (vals (:facts @replayed)))))

      (let [repo (.toFile (java.nio.file.Files/createTempDirectory
                           "placement-history" (make-array java.nio.file.attribute.FileAttribute 0)))
            source (io/file repo "placements.tsv")
            store (io/file repo "retained")
            old-text (slurp tsv)
            old-sha (action-identity/sha256 old-text)]
        (spit source old-text)
        (shell/sh "git" "init" "-q" (.getPath repo))
        (shell/sh "git" "-C" (.getPath repo) "add" "placements.tsv")
        (shell/sh "git" "-C" (.getPath repo) "-c" "user.name=Test"
                  "-c" "user.email=test@example.invalid" "commit" "-qm" "old placements")
        (spit source (str old-text "\nchanged\n"))
        (let [historical (assoc-in base-state [:pending :selection :placement/source]
                                   {:status :pinned
                                    :tsv {:path (.getPath source) :sha256 old-sha}})
              result (#'runtime/resolve-pending-learning
                      {:placement/source-store-dir (.getPath store)}
                      makeup historical current-dir current-observation)]
          (is (= :applied (:status result)))
          (is (nil? (get-in result [:state :pending])))
          (is (= :git-history
                 (get-in result [:placement-source :resolution :tsv :resolution])))))))))

(deftest retained-placement-bytes-survive-authority-edit
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "placement-retention" (make-array java.nio.file.attribute.FileAttribute 0)))
        source (io/file root "placements.tsv")
        store (io/file root "store")
        text "id\twhat\tstatus\twing\tway\taction\tneed_goes_to\tfinish\nM-a\tx\tready\tleft\tnone\tdo\t\tok\n"]
    (spit source text)
    (let [loaded (placement-adapter/load-placements
                  {:tsv (.getPath source) :classes nil})
          retained (placement-adapter/retain-sources! loaded store)
          pin (assoc (:source retained) :status :pinned)]
      (spit source (str text "M-b\ty\tready\tright\tnone\tdo\t\tok\n"))
      (let [replayed (#'runtime/replay-placements
                      {:placement/source-store-dir (.getPath store)} pin)]
        (is (= :pinned (:status replayed)))
        (is (= #{"M-a"} (set (keys (:rows replayed)))))
        (is (= :retained-store
               (get-in replayed [:source :resolution :tsv :resolution])))))))

(deftest production-self-heal-options-carry-declared-static-correction-limit
  (let [declared (runtime/self-heal-runtime-config)
        composed (runtime/production-self-heal-options {})]
    (is (= :wm/self-heal-runtime-v1 (:schema declared)))
    (is (= (:acceptance-static-correction-limit declared)
           (:meta-acceptance-static-correction-limit composed)))
    (is (= 1 (:meta-acceptance-static-correction-limit composed)))
    (is (= (:acceptance-return-limit declared)
           (:meta-acceptance-return-limit composed)))
    (is (= 1 (:meta-acceptance-return-limit composed)))))

(deftest open-cascade-author-prompt-states-operational-clause-schema
  (let [prompt (#'runtime/open-cascade-author-prompt
                {:requisition-id "retained"
                 :prior-resume-cause
                 {:kind :resume-incomplete
                  :missing-fields
                  [{:hole-id ["pattern/a" 0]
                    :missing [:operation :capability :completion-check]}]}})]
    (is (str/includes? prompt ":operation"))
    (is (str/includes? prompt ":capability"))
    (is (str/includes? prompt ":completion-check"))
    (is (str/includes? prompt ":consumes"))
    (is (str/includes? prompt ":produces"))
    (is (str/includes? prompt ":resume-incomplete"))))

(defn- f32-root []
  (.getCanonicalPath
   (.toFile (java.nio.file.Files/createTempDirectory
             "wm-f32-carried-bindings"
             (make-array java.nio.file.attribute.FileAttribute 0)))))

(defn- f32-fixture [name]
  (edn/read-string
   (slurp (str "test/futon2/aif/fixtures/click-7-" name ".edn"))))

(defn- f32-seal [request status payload]
  (let [work-order (:work-order request)]
    (coordinator/seal-port-receipt
     {:phase (:phase request) :injury-id (:injury-id work-order)
      :repair-id (:repair-id work-order) :attempt (:attempt work-order)
      :predecessor-pin (:predecessor-pin request)
     :status status :payload payload})))

(deftest production-default-readiness-without-priority-contacts-nobody
  (let [dir (f32-root)
        manifest (str dir "/readiness.edn")
        contacts (atom 0)
        snapshot {:graph {:section-status {}}
                  :agency-source {:path "fixture://agency" :sha256 (digest :agency)}
                  :agency {:agents {}}}
        opts {:ticket-readiness-manifest-path manifest
              :author "fixture-unavailable"}
        config (with-redefs [readiness-inputs/fetch-pipeline-snapshot
                             (constantly snapshot)
                             runner/agent-roster (constantly {})
                             flight-runner/agency-answer-fn
                             (fn [_]
                               (fn [_]
                                 (swap! contacts inc)
                                 {:state :not-contacted}))]
                 (with-redefs-fn
                   {#'runtime/find-giraffe-run (constantly nil)}
                   #(runtime/ticket-readiness-refresh-config opts)))
        result (ticket-readiness/refresh-one! config)
        preview (with-redefs [flight-runner/agency-answer-fn
                              (fn [_]
                                (fn [_]
                                  (swap! contacts inc)
                                  {:state :not-contacted}))]
                  ;; Constructing production defaults is the public preview-side
                  ;; composition boundary: it must not execute refresh-one!.
                  (runtime/production-defaults opts))]
    (is (= 0 @contacts) "unavailable designated producer performs no transport")
    (is (= :priority-projection-unavailable (:status result)))
    (is (= :giraffe-run-unavailable (:reason result)))
    (is (not (.exists (io/file manifest))) "unavailable priority writes no attempt")
    (is (fn? (:ticket-readiness-refresh preview)))
    (is (nil? (:ticket-id result)))))

(deftest readiness-config-pins-verified-priority-from-selection-resolver
  (let [dir (io/file (f32-root) "giraffe-run")
        _ (.mkdirs dir)
        priority-path (io/file dir "priority.edn")
        rows [{:target "T-z" :priority-band :associated
               :cutoff-status :within-cutoff}]
        _ (spit priority-path (str (pr-str rows) "\n"))
        pin (ticket-readiness/file-pin priority-path)
        _ (spit (io/file dir "manifest.edn")
                (pr-str {:priority-projection
                         {:status :present :file "priority.edn"
                          :sha256 (:sha256 pin)}}))
        config (with-redefs [runner/agent-roster (constantly {})
                             flight-runner/agency-answer-fn
                             (fn [_] (constantly {:state :not-contacted}))]
                 (with-redefs-fn
                   {#'runtime/find-giraffe-run (constantly dir)}
                   #(runtime/ticket-readiness-refresh-config
                     {:author "fixture"
                      :ticket-readiness-manifest-path
                      (str (f32-root) "/readiness.edn")})))]
    (is (= :present (get-in config [:priority-source :status])))
    (is (= pin (get-in config [:priority-source :pin])))
    (is (= rows (get-in config [:priority-source :rows])))))

(defn- readiness-proposal-with-sources [n]
  {:schema :wm/cascade-proposal-request-v1
   :status :proposed
   :origin :retrieval-proposed
   :target {:id "T-evidence-pinned-to-mutable-prose-26082026"
            :path "/home/joe/code/futon3c/holes/tickets/T-evidence-pinned-to-mutable-prose-26082026.md"
            :sha256 (apply str (repeat 64 "a"))}
   :sources
   (mapv (fn [i]
           {:id (str "/retained/source/" i "#" (apply str (repeat 64 "b")))
            :path (str "/retained/source/" i)
            :sha256 (apply str (repeat 64 "b"))
            :revision (apply str (repeat 40 "c"))
            :body (apply str (repeat 4096 "unbounded source body "))})
         (range n))})

(deftest ^:slow real-retained-readiness-proposal-is-bounded-and-informative
  (let [ticket-id "T-evidence-pinned-to-mutable-prose-26082026"
        ticket (first (filter #(= ticket-id (:id %))
                              (:tickets (mission-registry/load-tickets))))
        proposal (interpretation-request/prepare-proposal!
                  ticket-id :ticket (System/getProperty "user.dir"))
        citations (get-in proposal [:retrieval :citations])
        result (#'runtime/ticket-readiness-prompt
                {:ticket-readiness-prompt-budget-chars
                 runtime/default-ticket-readiness-prompt-budget-chars
                 :ticket-readiness-source-summary-limit
                 runtime/default-ticket-readiness-source-summary-limit
                 :ticket-readiness-citation-limit
                 runtime/default-ticket-readiness-citation-limit
                 :ticket-readiness-citation-chars
                 runtime/default-ticket-readiness-citation-chars
                 :ticket-readiness-candidate-limit
                 runtime/default-ticket-readiness-candidate-limit
                 :ticket-readiness-candidate-chars
                 runtime/default-ticket-readiness-candidate-chars}
                proposal)]
    (is (> (count (pr-str proposal))
           runtime/default-ticket-readiness-prompt-budget-chars)
        "iteration-8 ticket reconstructs a genuinely over-budget proposal")
    (is (= :ready (:status result)))
    (is (< (:prompt-chars result) (:prompt-budget-chars result)))
    (is (seq (get-in result [:proposal-summary :candidates])))
    (doseq [citation citations]
      (let [prefix (subs (:quote citation)
                         0 (min 128 (count (:quote citation))))
            escaped-prefix (subs (pr-str prefix) 1 (dec (count (pr-str prefix))))]
        (is (str/includes? (:prompt result) escaped-prefix))))
    (is (str/includes? (:prompt result) (pr-str (:source ticket)))
        "the prompt contains the exact source-pin the validator requires")))

(deftest over-budget-readiness-refuses-before-provider-contact
  (let [dir (f32-root)
        contacts (atom 0)
        proposal (readiness-proposal-with-sources 1)
        config (with-redefs [flight-runner/agency-answer-fn
                             (fn [_]
                               (fn [_]
                                 (swap! contacts inc)
                                 {:state :completed :text "unexpected"}))]
                 (runtime/ticket-readiness-refresh-config
                  {:author "wm-author"
                   :ticket-readiness-manifest-path (str dir "/manifest.edn")
                   :ticket-readiness-prompt-budget-chars 1
                   :ticket-readiness-source-summary-limit 1}))
        result ((:answer-fn config) proposal)]
    (is (= 0 @contacts))
    (is (= :refused (:status result)))
    (is (= :readiness/prompt-over-budget (:reason result)))
    (is (pos? (:prompt-chars result)))
    (is (= 1 (:prompt-budget-chars result)))
    (is (= (select-keys result [:prompt-chars :prompt-budget-chars :proposal-pin])
           (select-keys (edn/read-string (slurp (get-in result [:result-pin :path])))
                        [:prompt-chars :prompt-budget-chars :proposal-pin])))))

(deftest contacted-readiness-retains-bounded-prompt-measurement
  (let [dir (f32-root)
        proposal (readiness-proposal-with-sources 1)
        config (with-redefs [runner/agent-roster
                             (constantly {"wm-author" {:status :idle
                                                        :invoke-ready? true}})
                             flight-runner/agency-answer-fn
                             (fn [_]
                               (fn [_]
                                 {:job-id "job-readiness-bounded"
                                  :seat "wm-author" :state :completed
                                  :text "{}"}))]
                 (runtime/ticket-readiness-refresh-config
                  {:author "wm-author"
                   :ticket-readiness-manifest-path (str dir "/manifest.edn")}))
        result ((:answer-fn config) proposal)
        retained (edn/read-string (slurp (get-in result [:result-pin :path])))]
    (is (= "job-readiness-bounded" (:job-id retained)))
    (is (= (select-keys result [:prompt-chars :prompt-budget-chars :proposal-pin])
           (select-keys retained [:prompt-chars :prompt-budget-chars :proposal-pin])))
    (is (pos? (:prompt-chars retained)))
    (is (< (:prompt-chars retained) (:prompt-budget-chars retained)))))

(deftest claimed-injury-carries-intake-bindings-from-its-transaction
  (let [dir (f32-root)
        ledger-path (str dir "/ledger.edn")
        transaction-path (str dir "/transaction.edn")
        history (f32-fixture "ledger")
        transaction (f32-fixture "transaction")
        order (:work-order transaction)
        calls (atom {:diagnosis 0 :acceptance 0 :run 0 :admission-review 0})
        never (fn [key]
                (fn [& _]
                  (swap! calls update key inc)
                  {:status :refused :reason :unexpected-intake-call}))
        ports {:author #(f32-seal % :succeeded
                                  {:patch-sha (apply str (repeat 40 "a"))
                                   :worktree dir})
               :review #(f32-seal % :failed {:reason :review-offline})
               :enact #(f32-seal % :failed {:reason :unused})
               :retry #(f32-seal % :failed {:reason :unused})}
        _ (spit ledger-path (str (pr-str history) "\n"))
        _ (spit transaction-path (str (pr-str transaction) "\n"))
        result (run-supplied-task!
                {:meta-injury-ledger-path ledger-path
                 :meta-repair-transaction-path transaction-path
                 :meta-diagnosis-fn (never :diagnosis)
                 :meta-acceptance-fn (never :acceptance)
                 :meta-acceptance-run-fn (never :run)
                 :meta-acceptance-review-fn (never :admission-review)
                 :meta-repair-ports ports
                 :author (:author order) :reviewer (:reviewer order)
                 :repair-reviewer "fixture/repair-reviewer"
                 :author-session "fixture-author-attempt-two"
                 :reviewer-session "fixture-reviewer-attempt-two"
                 :repair-reviewer-session "fixture-repair-reviewer-attempt-two"
                 :target-repository (:target-repository order)
                 :target-worktree dir})]
    (is (= {:diagnosis 0 :acceptance 0 :run 0 :admission-review 0} @calls))
    (is (= {:status :present :source :supplied}
           (select-keys (:diagnosis-observation result) [:status :source])))
    (is (= :present (get-in result [:acceptance-observation :status])))
    (is (= :carried-transaction (get-in result [:carried-bindings :source])))
    (is (= :dedicated-meta-self-heal (:route result)))
    (is (= 2 (:attempt result)))
    (is (= 1 (get-in result [:continued-from 0 :attempt])))))

(def ^:private iteration-21-root
  "/home/joe/notes/wm-next/criteria-completeness/repair-iteration-21")

(defn- iteration-21-context [ledger-name]
  (let [ledger (edn/read-string (slurp (str iteration-21-root "/" ledger-name)))
        transaction-path (str iteration-21-root "/transaction-after.edn")
        transaction (edn/read-string (slurp transaction-path))
        injury-id (get-in transaction [:work-order :injury-id])
        latest (last (filter #(and (= :wm/meta-injury-transition-v1 (:schema %))
                                   (= injury-id (:injury-id %)))
                             (:entries ledger)))]
    {:opts {:meta-repair-transaction-path transaction-path}
     :context {:durable-ledger {:ledger ledger}
               :active-injuries {:status :computed :injuries {injury-id latest}}
               :injury-selection {:transaction-path transaction-path}}}))

(deftest iteration-21-claim-forces-a-fresh-acceptance-proposal
  (let [{:keys [opts context]} (iteration-21-context "ledger-after.edn")
        carried (meta-self-heal/carried-bindings-observation opts context)
        intake (runtime/self-heal-acceptance-intake opts carried context)
        request (atom nil)
        proposal (meta-self-heal/produce-acceptance!
                  (assoc (:opts intake)
                         :meta-repository-root (System/getProperty "user.dir")
                         :meta-acceptance-root (str (f32-root) "/acceptances")
                         :meta-acceptance-fn
                         (fn [r]
                           (reset! request r)
                           {:status :proposed
                            :acceptance-test
                            {:id "iteration-21-fresh"
                             :command ["clojure" "-M:test" "-n"
                                       "futon2.aif.iteration-21-fresh-test"]
                             :expected {:exit 0}
                             :source
                             {:namespace "futon2.aif.iteration-21-fresh-test"
                              :path "test/futon2/aif/iteration_21_fresh_test.clj"
                              :text (str "(ns futon2.aif.iteration-21-fresh-test "
                                         "(:require [clojure.test :refer [deftest is]]))\n"
                                         "(deftest fresh (is true))\n")}}}))
                  (:diagnosis-observation intake))
        claims (filter #(= :acceptance-unsatisfiable-claimed (:observation-kind %))
                       (get-in context [:durable-ledger :ledger :entries]))]
    (is (= :available (get-in intake [:diagnosis-observation :status])))
    (is (nil? (get-in intake [:opts :acceptance-test])))
    (is (= :proposed (:status proposal)))
    (is (= (get-in carried [:acceptance-unsatisfiable-claim :author-evidence])
           (get-in @request [:author-unsatisfiable-claim :author-evidence])))
    (is (= 1 (count claims)) "preparation does not spend another return")))

(deftest iteration-21-ordinary-resume-keeps-the-admitted-acceptance
  (let [{:keys [opts context]} (iteration-21-context "ledger-before.edn")
        carried (meta-self-heal/carried-bindings-observation opts context)
        intake (runtime/self-heal-acceptance-intake opts carried context)]
    (is (nil? (:acceptance-unsatisfiable-claim carried)))
    (is (= (:acceptance-test carried) (get-in intake [:opts :acceptance-test])))
    (is (= :present (get-in intake [:diagnosis-observation :status])))))

(deftest missing-transaction-preserves-diagnosis-production
  (let [dir (f32-root)
        ledger-path (str dir "/ledger.edn")
        calls (atom 0)
        _ (spit ledger-path (str (pr-str (f32-fixture "ledger")) "\n"))
        result (run-supplied-task!
                {:meta-injury-ledger-path ledger-path
                 :meta-repair-transaction-path (str dir "/missing.edn")
                 :meta-diagnosis-fn (fn [& _]
                                      (swap! calls inc)
                                      {:status :unavailable
                                       :reason :fixture-unavailable})})]
    (is (= 1 @calls))
    (is (= {:status :absent :reason :no-transaction
            :injury-reason :effective-active-selected}
           (select-keys (:carried-bindings result)
                        [:status :reason :injury-reason])))))

(deftest production-registers-the-bounded-beta-campaign
  (runtime/production-defaults {})
  (let [status (reading-precision/campaign-status)]
    (is (= :registered (:status status)))
    (is (= 1 (:beta status)))
    (is (= 4 (:identity-limit status)))
    (is (= 4 (:remaining status)))))

(deftest occurrence-source-carries-the-actions-validated-square
  (let [square {:schema :wm/three-halves-square-v1 :fixture true}
        entry {:target "M-square"
               :action {:id "policy-square"
                        :arrangement
                        {:nodes [{:occurrence-id ["p/x" 0]
                                  :fragment-span {:start 1 :end 2}
                                  :source-sha256 (apply str (repeat 64 "a"))
                                  :blend-square square}]}}}
        readings (cascade-decision/occurrence-readings
                  entry {:excerpt-digest (digest :excerpt)
                         :analysis-digest (digest :analysis)})]
    (is (= square (get-in readings [["p/x" 0] :square])))
    (is (= ["p/x" 0] (first (keys readings))))))

(deftest production-composition-installs-the-reviewed-same-click-handler
  ;; No active injury: the production gate reads the ledger and transaction
  ;; paths it is given, never the live data/wm-meta-injuries files.
  (let [seen (atom nil)
        dir (f32-root)
        d (digest :cascade)
        holes [{:hole-id :h :owner :wm :question :operation}]
        patch {:namespace "futon2.aif.full-loop-runtime-test"
               :var "identity"
               :source-digest (digest :source)}
        error {:kind :wm/open-cascade-execution-error
               :cascade-digest d :holes holes :phase :compile :attempts []
               :resume-port {:retry (fn [_] {:status :cascade-ready
                                             :cascade-digest d
                                             :outcome :redone})}}
        fenced (fn [value]
                 {:state "done"
                  :result (str "```json\n" (json/generate-string value) "\n```")})
        author-answer (fn [req]
                        (fenced
                         {:requisition-id (:requisition-id req)
                          :cascade-digest d :by "wm-author"
                          :decisions {:h {:decision :implement}}
                          :patch patch}))
        reviewer-answer
        (fn [{:keys [requisition author-answer]}]
          (fenced
           (assoc author-answer :requisition-id (:requisition-id requisition)
                  :accepted-by "wm-reviewer")))]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! seen opts)
                    ((:open-cascade-resume-fn opts) error))]
      (is (= :resumed
             (:status
              (run-supplied-task!
               {:run-id "resume-production"
                :meta-injury-ledger-path (str dir "/ledger.edn")
                :meta-repair-transaction-path (str dir "/transaction.edn")
                :author "wm-author" :reviewer "wm-reviewer"
                :open-cascade-author-answer-fn author-answer
                :open-cascade-reviewer-answer-fn reviewer-answer
                :open-cascade-patch-loader (constantly identity)})))))
    (is (fn? (:open-cascade-resume-fn @seen)))
    (is (some? (:click-budget @seen)))
    (is (some? (:selection-counter @seen)))
    (is (= :held (:status (open-resume/recognize error))))))

(deftest production-resume-contacts-the-explicit-cast-in-work-mode
  (let [configurations (atom [])
        live-ledger (atom {:order [] :jobs {}})]
    (with-redefs [flight-runner/agency-answer-fn
                  (fn [configuration]
                    (swap! configurations conj configuration)
                    (constantly {:state :not-contacted}))]
      (let [handler (runtime/open-cascade-resume-handler
                     {:author "wm-author" :reviewer "wm-reviewer"})]
        (is (fn? handler))
        ;; Contacts are constructed at invocation, after the runner has minted
        ;; the registered ledger, rather than against stale composition opts.
        (handler {:kind :not-a-held-cascade}
                 {:registered-run/job-ledger live-ledger})))
    (is (= ["wm-author" "wm-reviewer"] (mapv :seat @configurations)))
    (is (= ["work" "work"]
           (mapv #(get-in % [:opts :invoke-mode]) @configurations)))
    (is (every? #(identical? live-ledger
                             (get-in % [:opts :registered-run/job-ledger]))
                @configurations))))

(deftest flight-runner-does-not-load-the-report
  ;; No :test alias in the child: since b463d85cc (2026-10-20) the alias
  ;; carries :main-opts for cognitect.test-runner, so "-M:test -e" started
  ;; the whole suite again, including this test, without end.  The child
  ;; only needs src, which the root :paths provide.
  (let [{:keys [exit out err]}
        (shell/sh "clojure" "-M" "-e"
                  (str "(require 'futon2.aif.flight-runner) "
                       "(prn (boolean (find-ns 'futon2.report.war-machine)))"))]
    (is (zero? exit) err)
    ;; JVM startup may emit container thread/timing diagnostics on stdout;
    ;; the child contract is its final EDN form.
    (is (= false (edn/read-string (last (str/split-lines out)))))))

(deftest runtime-supplies-the-report-backed-judge
  (let [seen (atom nil)
        opts {:run-id "runtime-test" :flight {:target "T"}
              :cascade-feedback-path "/tmp/runtime-test-feedback.edn"}
        expected {:judgement :selected}]
    (with-redefs [wm/accumulation-config (constantly {:accumulation :configured})
                  wm/generate-war-machine
                  (fn [days judge-opts]
                    (reset! seen [days judge-opts])
                    expected)]
      (let [defaults (runtime/production-defaults opts)]
        (is (fn? (:cascade-revision-proposals-fn defaults)))
        (is (= expected ((:judge-fn defaults) 17))))
      (is (= 17 (first @seen)))
      (let [actual (dissoc (second @seen) :outer-task-selection-fn)
            observation (:meta-field-observation actual)
            source-pin (:meta-field-source-pin actual)]
        (is (= (merge {:accumulation :configured
              :run-id "runtime-test"
              :flight {:target "T"}
              :cascade-feedback-path "/tmp/runtime-test-feedback.edn"
              :factorized-preference (preference-registry/load!)
              :trace? false
              :include-advisory-lanes? false
              :defer-render? true}
                      {:meta-field-observation observation
                       :meta-field-source-pin source-pin})
               actual))
        (is (= :verified
               (:status (meta-field/verify
                         observation {:expected-snapshot-pin source-pin}))))
        (is (= :external-snapshot-pin-mismatch
               (:reason (meta-field/verify
                         observation
                         {:expected-snapshot-pin
                          (assoc source-pin :sha256 (apply str (repeat 64 "0")))}))))
        (is (= :external-snapshot-authority-invalid
               (:reason (meta-field/verify observation
                                           {:expected-snapshot-pin nil})))))
      (is (nil? (:outer-task-selection-fn (second @seen)))))))

(deftest selected-target-reading-contacts-only-the-designated-author
  (let [captured (atom nil)]
    (with-redefs [runner/agent-roster
                  (constantly {"random-idle-seat"
                               {:status :idle :invoke-ready? true}})
                  selected-target-reading/repair!
                  (fn [opts _]
                    (reset! captured ((:answer-fn opts) {:target "M-selected"}))
                    @captured)]
      (let [defaults (runtime/production-defaults
                      {:run-id "designated-author" :author "wm-author"
                       :reviewer "wm-reviewer"
                       :repair-reviewer "wm-repair-reviewer"})]
        ((:selected-target-reading-fn defaults) {} {})))
    (is (= {:state :not-contacted
            :reason :designated-author-unavailable
            :author "wm-author"}
           @captured))))

(deftest production-runner-receives-and-exercises-selected-target-reading-port
  (let [dir (f32-root)
        isolated {:meta-injury-ledger-path (str dir "/ledger.edn")
                  :meta-repair-transaction-path (str dir "/transaction.edn")}
        runner-opts (atom nil)
        contacted (atom [])
        repair-result {:schema :wm/selected-target-reading-request-v1
                       :status :refused :published false
                       :kind :probe-complete}]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! runner-opts opts)
                    ((:selected-target-reading-fn opts)
                     {}
                     {:outer-task-selection {:chosen {:id "M-production"}}})
                    {:status :ran})
                  selected-target-reading/repair!
                  (fn [opts _]
                    ((:answer-fn opts) {:target "M-production"})
                    repair-result)]
      (is (= :ran
             (:status
              (run-supplied-task!
               (assoc isolated
                      :run-id "production-selected-reading"
                      :author "wm-author"
                      :reviewer "wm-reviewer"
                      :repair-reviewer "wm-repair-reviewer"
                      :selected-target-reading-answer-fn
                      (fn [issued]
                        (swap! contacted conj [(:target issued) "wm-author"])
                        {:state :not-contacted :reason :test-probe})))))))
    (is (fn? (:selected-target-reading-fn @runner-opts))
        "the real production wrapper must pass the port on runner opts")
    (is (= [["M-production" "wm-author"]] @contacted))))

(deftest production-runner-receives-occurrence-square-authoring-port
  (let [dir (f32-root)
        isolated {:meta-injury-ledger-path (str dir "/ledger.edn")
                  :meta-repair-transaction-path (str dir "/transaction.edn")}
        seen (atom nil)]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! seen opts)
                    ((:occurrence-square-authoring-fn opts) {} {})
                    {:status :ran})
                  square-authoring/repair! (fn [opts _]
                                             (is (fn? (:answer-fn opts)))
                                             {:status :probe})]
      (is (= :ran (:status
                   (run-supplied-task!
                    (assoc isolated
                           :run-id "production-square-authoring"
                           :author "wm-author" :reviewer "wm-reviewer"))))))
    (is (true? (:require-occurrence-squares? @seen)))
    (is (fn? (:occurrence-square-authoring-fn @seen)))))

(deftest production-runner-receives-ticket-readiness-refresh-port
  (let [dir (f32-root)
        seen (atom nil)]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! seen opts)
                    {:status :ran})]
      (is (= :ran (:status
                   (run-supplied-task!
                    {:meta-injury-ledger-path (str dir "/ledger.edn")
                     :meta-repair-transaction-path (str dir "/transaction.edn")
                     :run-id "production-ticket-readiness"
                     :author "wm-author" :reviewer "wm-reviewer"})))))
    (is (fn? (:ticket-readiness-refresh @seen))
        "the runner reads readiness refresh from explicit options")))

(deftest readiness-dispatch-uses-the-run-local-job-ledger
  (let [dir (f32-root)
        seat-call-path (str dir "/seat-calls.jsonl")
        ledger (atom {:order [] :jobs {}})
        posted (atom nil)
        resolved-opts (atom nil)
        failure (atom nil)]
    (binding [seat-call-index/*path* seat-call-path]
      (with-redefs [runner/agent-roster
                  (constantly {"wm-author" {:status :idle :invoke-ready? true}})
                  http/post (fn [_ request]
                              (reset! posted (json/parse-string (:body request) true))
                              {:status 200 :body "{\"job-id\":\"readiness-job\"}"})
                  runner/poll-job! (fn [_ _] {:state "done" :result "{}"})
                  runner/read-job! (fn [opts _]
                                     (reset! resolved-opts opts)
                                     {:state "done"})
                  ticket-readiness/refresh-one!
                  (fn [config]
                    (try
                      ((:answer-fn config) {:target "T-readiness"})
                      (catch clojure.lang.ExceptionInfo e
                        (reset! failure (ex-data e))))
                    ((:contact-resolver config) "readiness-job"))
                  runner/run-opportunity!
                  (fn [opts]
                    (let [live-opts (assoc opts :registered-run/job-ledger ledger
                                               :click-id "readiness-live-click")]
                      (runner/ticket-readiness-refresh-step live-opts)
                      {:status :ran}))]
        (run-supplied-task!
         {:meta-injury-ledger-path (str dir "/ledger.edn")
          :meta-repair-transaction-path (str dir "/transaction.edn")
          :ticket-readiness-manifest-path (str dir "/readiness.edn")
          :run-id "readiness-live-run" :author "wm-author"})))
    (is (nil? @failure) (pr-str @failure))
    (is (= "readiness-live-click" (get-in @posted [:harness :source-ref])))
    (is (= "readiness-live-run" (get-in @posted [:harness :execution-id])))
    (is (= ["readiness-job"] (:order @ledger)))
    (is (= "readiness-live-click" (get-in @ledger [:jobs "readiness-job" :click-id])))
    (is (identical? ledger (:registered-run/job-ledger @resolved-opts)))
    (is (= "readiness-live-click" (:click-id @resolved-opts)))
    (let [rows (map #(json/parse-string % true)
                    (str/split-lines (slurp seat-call-path)))]
      (is (= 1 (count rows)))
      (is (= "readiness-live-click" (:click-id (first rows))))
      (is (= "readiness-author" (:role (first rows))))
      (is (= "readiness-job" (:job-id (first rows)))))))

(deftest selected-target-answer-is-built-with-the-run-local-job-ledger
  (let [ledger (atom {:order [] :jobs {}})
        answer-opts (atom nil)]
    (with-redefs [runner/agent-roster
                  (constantly {"wm-author" {:status :idle :invoke-ready? true}})
                  flight-runner/agency-answer-fn
                  (fn [{:keys [opts]}]
                    (reset! answer-opts opts)
                    (fn [_] {:state :not-contacted :reason :test-probe}))
                  selected-target-reading/repair!
                  (fn [opts _]
                    ((:answer-fn opts) {:target "M-production"}))]
      (let [defaults (runtime/production-defaults
                      {:run-id "production-selected-reading"
                       :author "wm-author"})]
        ((:selected-target-reading-fn defaults)
         {:click-id "wm-click-probe"
          :registered-run/job-ledger ledger}
         {})))
    (is (identical? ledger (:registered-run/job-ledger @answer-opts)))
    (is (= "wm-click-probe" (:click-id @answer-opts)))))

(deftest selected-policy-reading-is-wired-late-to-the-designated-author
  (let [ledger (atom {:order [] :jobs {}})
        answer-opts (atom nil)
        prompt-fn (atom nil)]
    (with-redefs [runner/agent-roster
                  (constantly {"wm-author" {:status :idle :invoke-ready? true}})
                  flight-runner/agency-answer-fn
                  (fn [{:keys [opts] :as configuration}]
                    (reset! answer-opts opts)
                    (reset! prompt-fn (:prompt-fn configuration))
                    (fn [_] {:request-id "r" :verdicts [] :by "wm-author"}))]
      (let [defaults (runtime/production-defaults
                      {:run-id "production-policy-reading"
                       :author "wm-author"})
            ask ((:selected-reading-ask-builder defaults)
                 {:click-id "wm-click-policy-reading"
                  :registered-run/job-ledger ledger})]
        (is (map? (ask {:request-id "r"}))))
      (is (identical? ledger (:registered-run/job-ledger @answer-opts)))
      (is (= "wm-click-policy-reading" (:click-id @answer-opts)))
      (is (identical? selected-reading/prompt @prompt-fn)))))

(deftest runner-refuses-an-absent-runtime-default
  (binding [runner/*runtime-defaults* nil]
    (let [failure (try
                    (runner/runtime-default {} :judge-fn)
                    nil
                    (catch clojure.lang.ExceptionInfo e e))]
      (is (some? failure))
      (is (= {:failure-kind :missing-runtime-default
              :missing-default :judge-fn
              :supplied-by 'futon2.aif.full-loop-runtime}
             (ex-data failure))))))

(deftest production-composition-supplies-the-feedback-store
  (let [dir (f32-root)
        isolated {:meta-injury-ledger-path (str dir "/ledger.edn")
                  :meta-repair-transaction-path (str dir "/transaction.edn")}
        seen (atom nil)]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! seen opts)
                    {:status :ran})]
      (is (= :ran (:status (run-supplied-task!
                            (assoc isolated :run-id "feedback-default")))))
      (is (= cascade-feedback/default-path
             (:cascade-feedback-path @seen)))
      (is (= runtime/default-pattern-graph-path (:pattern-graph-path @seen)))
      (is (= runtime/default-pattern-graph-diff-dir (:pattern-graph-diff-dir @seen)))
      (is (map? (:cascade-feedback-metadata @seen))))))

(deftest production-composition-shares-one-selection-timing-collector
  (let [dir (f32-root)
        isolated {:meta-injury-ledger-path (str dir "/ledger.edn")
                  :meta-repair-transaction-path (str dir "/transaction.edn")}
        runner-opts (atom nil)
        judge-opts (atom nil)]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! runner-opts opts)
                    ((:judge-fn runner/*runtime-defaults*) 1)
                    {:status :ran})
                  wm/generate-war-machine
                  (fn [_ opts]
                    (reset! judge-opts opts)
                    {:judgement {}})]
      (is (= :ran (:status (run-supplied-task!
                            (assoc isolated :run-id "timing-state")))))
      (is (instance? clojure.lang.IAtom
                     (:selection-timing/state @runner-opts)))
      (is (identical? (:selection-timing/state @runner-opts)
                      (:selection-timing/state @judge-opts))))))

(deftest production-mints-run-identity-before-closing-over-the-judge
  (let [runner-opts (atom nil)
        judge-opts (atom nil)
        dir (f32-root)
        isolated {:meta-injury-ledger-path (str dir "/ledger.edn")
                  :meta-repair-transaction-path (str dir "/transaction.edn")}]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! runner-opts opts)
                    ((:judge-fn runner/*runtime-defaults*) 1)
                    {:status :ran})
                  wm/generate-war-machine
                  (fn [_ opts]
                    (reset! judge-opts opts)
                    {:judgement {}})]
      (is (= :ran (:status (run-supplied-task! isolated))))
      (is (string? (:run-id @runner-opts)))
      (is (= (:run-id @runner-opts) (:run-id @judge-opts))
          "accumulation and the durable run record must receive one identity"))))
