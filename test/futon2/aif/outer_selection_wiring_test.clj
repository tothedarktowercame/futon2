(ns futon2.aif.outer-selection-wiring-test
  (:require [futon2.aif.outer-menu-fixture :as menu-fixture]
            [clojure.edn :as edn]
            [babashka.http-client :as http]
            [futon2.aif.area-focus :as area-focus]
            [futon2.aif.operator-focus :as operator-focus]
            [futon2.aif.mission-registry :as registry]
            [futon2.aif.placement-adapter :as placement]
            [futon2.aif.serving-envelope :as serving-envelope]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [wm-run-facts :as run-facts]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.outer-observation :as outer-observation]
            [futon2.aif.outer-select :as outer-select]
            [futon2.aif.outer-select-test :as fixtures]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.effective-run-history :as effective-run-history]
            [futon2.aif.wm.debugger :as debugger]
            [futon2.aif.outer-state :as outer-state]
            [futon2.test-support.runner-fixture :as runner-fixture]))

(use-fixtures :once hermetic/with-hermetic-stores
              runner-fixture/with-hermetic-traces)

(defonce ^:private giraffe-loader
  (doto (clojure.lang.DynamicClassLoader.
         (.getContextClassLoader (Thread/currentThread)))
    (.addURL (.toURL (.toURI (io/file "/home/joe/code/futon3c/src"))))))

(defn actual-var [symbol]
  (with-bindings {clojure.lang.Compiler/LOADER giraffe-loader}
    (requiring-resolve symbol)))

(defn temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "outer-selection-wiring-test"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(def fixture-missions
  {:missions (mapv (fn [id]
                     {:id id :path (str "/fixture/" id ".md") :status-class :open
                      :source {:path (str "/fixture/" id ".md") :sha256 "fixture-sha"}})
                   ["M-selected" "M-unknown"])})

(defn with-selection-inputs [f]
  (let [root (temp-dir)
        corpus {:rows {"M-selected" {:target "M-selected" :area "WM"}
                       "M-unknown" {:target "M-unknown" :area "WM"}}
                :sha256 (identity/sha256 "fixture task areas")}
        live-calls (atom [])
        forbidden (fn [& _]
                    (swap! live-calls conj :http-request)
                    (throw (AssertionError. "Live I/O forbidden in wiring tests")))]
    (with-redefs [operator-focus/dispatch-reader!
                  (fn [& _]
                    (swap! live-calls conj :dispatch-reader)
                    (throw (AssertionError. "dispatch-reader! forbidden in wiring tests")))
                  http/request forbidden
                  runner/*wm-status-reporting?* false
                  runner/ensure-dispatch-seat! (fn [_] nil)
                  registry/load-missions (fn [& _] fixture-missions)
                  registry/load-excursions (fn [& _] {:excursions []})
                  registry/load-tickets (fn [& _] {:tickets []})
                  area-focus/load-corpus (fn [& _] corpus)
                  placement/default-paths {:tsv (str (io/file root "placements.tsv"))
                                           :classes (str (io/file root "classes.tsv"))}]
      (f)
      ;; Also detect a caller swallowing the guard's exception.
      (is (empty? @live-calls) (str "Unexpected live calls: " @live-calls)))))

(use-fixtures :each menu-fixture/with-fixture-constructor with-selection-inputs)

(defn fixture-focus-options [opts]
  (let [root (temp-dir)
        as-of (or (:decision-as-of opts) "2026-10-28T12:00:00Z")]
    (operator-focus/write-manifest!
     root {:as-of as-of :result {:status :present :ranked [{:area "WM"}]}})
    (merge {:decision-as-of as-of :operator-focus/manifest-dir (str root)} opts)))

(defn fixture-run! []
  (let [root (temp-dir)
        pass {:as-of "2026-10-28"
              :counts {:open 2 :ready 1 :ready-ignoring-externals 1}
              :order [{:id :M-selected :status :ready
                       :reason :order/ready-by-unblocks
                       :unblocks 2 :cone 3 :source-order 0}
                      {:id :M-unknown :status :undetermined
                       :reason :order/not-ready-by-source-order
                       :unblocks 0 :cone 1 :source-order 1}]
              :readiness {:M-selected {:status :ready}
                          :M-unknown {:status :undetermined}}
              :cones {:M-selected [:a :b :c] :M-unknown [:x]}
              :unblocks {:M-selected [:a :b] :M-unknown []}
              :cycles [] :anomalies []}
        write-run! (actual-var 'futon3c.giraffe.run/write-run!)
        result (write-run! {:root (.getPath root) :run-id "fixture-missions-run"
                            :git-sha "fixture"
                            :adapter-result {:input {:as-of "2026-10-28"
                                                     :items [] :edges []}
                                             :rejected []}
                            :pass-output (update pass :order
                                                 #(mapv (fn [row]
                                                          (assoc row :dependency-reading
                                                                 {:record-pin {:sha256 (identity/sha256 (pr-str row))}}))
                                                        %))})]
    (io/file (:dir result))))

(def makeup
  {:makeup/sha256 (apply str (repeat 64 "a"))
   :default-reach 1/2
   :cascades [{:id :ready :patterns ["test"] :steps [:ready-gate]}
              {:id :undetermined :patterns ["test"]
               :steps [{:status-gate :undetermined}]}]
   :enactment-modes {:ready :implementation :undetermined :implementation}
   :c-kit {:focused 55/100 :related 35/100 :unrelated 5/100
           :stop-the-line 5/100 :not-yet-evaluated 0}
   :kappa 100
   :state-kit {:ready {:doable 3 :blocked 1}
               :not-ready {:doable 1 :blocked 3}
               :undetermined {:doable 1 :blocked 1}}
   :gain-kit {:none 1 :one 2 :several 3}})

(defn call-ordinary [opts redefs]
  (binding [outer-observation/*resolve-var* actual-var]
    (with-redefs-fn
      (merge {#'runtime/find-giraffe-run (fn [_] (fixture-run!))
              #'outer-state/load-makeup (fn [_] makeup)
              #'outer-state/read-state (fn [m _] (outer-state/init-state m))
              #'outer-state/write-state! (fn [& _] {:status :written})}
             redefs)
      (fn [] (#'runtime/run-ordinary-opportunity! (fixture-focus-options opts))))))

(deftest missing-task-selects-and-enters-existing-path
  (let [received (atom nil)
        result (call-ordinary
                {:decision-as-of "2026-10-28T12:00:00Z"}
                {#'runtime/run-ordinary-opportunity-with-task!
                 (fn [opts] (reset! received opts) {:ran true})})]
    ;; Ordinary selection now always retains its decision. Check that additional
    ;; result field as well as the unchanged runner payload.
    (is (= {:ran true :selection-record (:selection-record result)} result))
    (let [pin (:selection-record result)
          text (slurp (:path pin))
          record (edn/read-string text)]
      (is (= (:sha256 pin) (identity/sha256 text)))
      (is (= :chosen (get-in record [:selection-outcome :status]))))
    (is (= {:id "M-selected"} (:task @received)))
    (is (= :ready (get-in @received [:outer/selection :cascade-id])))
    (is (= :implementation (get-in @received [:outer/selection :enactment :mode])))
    (is (vector? (get-in @received [:outer/selection :cascade-scores])))
    (is (= "fixture-missions-run"
           (get-in @received [:outer/selection :giraffe/run-id])))
    (is (nil? (get-in @received [:outer/selection :selection :candidates])))))

(deftest find-out-selection-retains-discovery-mode-at-runner-seam
  (let [received (atom nil)
        discovery (assoc makeup
                         :cascades [{:id :find-out-before-building
                                     :patterns ["test"] :steps [:ready-gate]}
                                    {:id :discovery-companion
                                     :patterns ["test"]
                                     :steps [{:status-gate :undetermined}]}]
                         :enactment-modes {:find-out-before-building :discovery
                                           :discovery-companion :discovery})]
    (call-ordinary
     {:decision-as-of "2026-10-28T12:00:00Z"}
     {#'outer-state/load-makeup (fn [_] discovery)
      #'runtime/run-ordinary-opportunity-with-task!
      (fn [opts] (reset! received opts) {:ran true})})
    (is (= {:mode :discovery
            :outer-cascade-id :find-out-before-building}
           (select-keys (get-in @received [:outer/selection :enactment])
                        [:mode :outer-cascade-id])))
    (is (= :off (:wm/discovery-enactment @received)))
    (is (= "M-selected"
           (get-in @received [:outer/selection :selection :selected :target])))))

(deftest supplied-task-bypasses-selection
  (let [received (atom nil)
        opts {:task {:id "M-caller"}}
        result (with-redefs-fn
                 {#'outer-select/select (fn [& _] (throw (ex-info "must not select" {})))
                  #'runtime/run-ordinary-opportunity-with-task!
                  (fn [x] (reset! received x) {:ran true})}
                 (fn [] (#'runtime/run-ordinary-opportunity! opts)))]
    (is (= {:ran true} (dissoc result :selection-record)))
    (is (= opts (select-keys @received (keys opts))))))

(deftest no-verified-run-is-typed-absent
  (let [called? (atom false)
        result (with-redefs-fn
                 {#'runtime/find-giraffe-run (constantly nil)
                  #'runtime/run-ordinary-opportunity-with-task!
                  (fn [_] (reset! called? true))}
                 (fn [] (#'runtime/run-ordinary-opportunity! {})))]
    (is (= {:absent :no-verified-giraffe-run} (dissoc result :selection-record)))
    (is (false? @called?))))

(deftest empty-chosen-cascade-retains-typed-abstention
  (let [called? (atom false)
        gates-out (assoc makeup :cascades
                         [{:id :none :patterns ["test"]
                           :steps [{:gate-status :not-ready}]}])
        result (call-ordinary
                {:decision-as-of "2026-10-28T12:00:00Z"}
                {#'outer-state/load-makeup (fn [_] gates-out)
                 #'outer-observation/outer-observation
                 (fn [_] {:field [{:target "M-selected" :reach 1/2 :reach-source :default-reach}]
                          :facts {"M-selected" {:status :ready :cone 1 :unblocks 1}}
                          :focus "WM" :relation (fn [_ _] :associated)})
                 #'runtime/run-ordinary-opportunity-with-task!
                 (fn [_] (reset! called? true))})]
    (is (= :no-task-selected (:absent result)))
    (is (= {:status :abstained :case :all-policies-predict-stopping}
           (get-in result [:outer/selection :outcome])))
    (is (= :debugger-error (get-in result [:refusal/disposition :disposition])))
    (is (= :debugger-error
           (get-in result [:outer/selection :refusal/disposition :disposition])))
    (is (= :debugger-error
           (get-in (edn/read-string (slurp (get-in result [:selection-record :path])))
                   [:outer/selection :refusal/disposition :disposition])))
    (is (= [:none] (get-in result [:outer/selection :stopping-equivalent 0 :members])))
    (is (false? @called?))))

(deftest singleton-posterior-retains-abstention-without-author-dispatch
  (let [called? (atom false)
        singleton-makeup
        (assoc makeup :cascades
               [{:id :ready-a :patterns ["test"] :steps [:ready-gate]}
                {:id :ready-b :patterns ["test"] :steps [:ready-gate]}
                {:id :stopping-a :patterns ["test"]
                 :steps [{:gate-status :not-ready}]}
                {:id :stopping-b :patterns ["test"]
                 :steps [{:gate-status :not-ready}]}])
        result (call-ordinary
                {:decision-as-of "2026-10-28T12:00:00Z"}
                {#'outer-state/load-makeup (fn [_] singleton-makeup)
                 #'runtime/run-ordinary-opportunity-with-task!
                 (fn [_] (reset! called? true) {:outcome :grounded-progress})})
        retained (:outer/selection result)
        record (edn/read-string
                (slurp (get-in result [:selection-record :path])))]
    (is (= :no-task-selected (:absent result)))
    (is (= {:status :abstained :case :insufficient-comparison}
           (:outcome retained)))
    (is (= {:status :abstained :case :insufficient-comparison}
           (:selection-outcome record)))
    (is (= 1 (count (:classes retained))))
    (is (= 1 (count (:stopping-equivalent retained))))
    (is (false? @called?))))

(deftest no-target-disposition-is-checked-against-the-retained-admitted-field
  (let [empty-field (#'runtime/work-exhausted-refusal {:outer-field/admitted []})
        live-field (#'runtime/work-exhausted-refusal
                    {:outer-field/admitted ["M-still-selectable"]})
        missing-field (#'runtime/work-exhausted-refusal {})
        ;; bad case: the runtime copies (:outer-score/admitted selection), so
        ;; a scorer that produced no admitted list retains the key with nil.
        ;; nil is "carrier absent", never an empty field.
        nil-field (#'runtime/work-exhausted-refusal {:outer-field/admitted nil})]
    (is (= :loop-stop (:disposition empty-field)))
    (is (= [] (get-in empty-field [:evidence :scoredField])))
    (is (= :debugger-error (:disposition live-field)))
    (is (= ["M-still-selectable"]
           (get-in live-field [:evidence :scoredField])))
    (is (= :debugger-error (:disposition missing-field)))
    (is (nil? (get-in missing-field [:evidence :scoredField])))
    (is (= :debugger-error (:disposition nil-field)))))

(defn- supervisor-predicate
  "Run the supervisor's own bb predicate (read from
  scripts/wm_click_repair_loop.py, not a copy) on PATH."
  [constant path]
  (let [form (:out (shell/sh "python3" "-c"
                             (str "import sys; sys.path.insert(0, 'scripts'); "
                                  "import wm_click_repair_loop as m; "
                                  "sys.stdout.write(m." constant ")")))]
    (assert (seq form) (str constant " not readable from the supervisor"))
    (shell/sh "bb" "-e" form path)))

(deftest legal-terminal-exclusions-make-work-exhaustion-reachable
  ;; RefusalLegality.legal_terminals_bounded: once every member of the scored
  ;; field has a recorded legal-terminal exclusion, selection reaches L1.
  (let [sha "fixture-sha"
        digest (identity/digest {:mission-source-sha256 sha})
        rows (mapv (fn [target]
                     {:c1-exclusion
                      {:status :row
                       :row {:schema :wm/c1-exclusion-v1
                             :target target
                             :evidence-digest digest
                             :run-id (str "satisfied-" target)}}})
                   ["M-ready" "M-unknown"])
        result (call-ordinary
                {:decision-as-of "2026-10-28T12:00:00Z"}
                {#'outer-observation/outer-observation
                 (fn [_]
                   (assoc fixtures/base-observation
                          :mission-source-sha256-by-target
                          {"M-ready" sha "M-unknown" sha}
                          :giraffe/run-id "fixture-missions-run"))
                 #'effective-run-history/index-rows (fn [_] rows)
                 #'runtime/run-ordinary-opportunity-with-task!
                 (fn [_] (throw (AssertionError. "empty field must not run a task")))})
        retained (:outer/selection result)
        record-path (get-in result [:selection-record :path])
        record (edn/read-string (slurp record-path))
        supervisor-check (supervisor-predicate "_WORK_EXHAUSTED_EDN" record-path)
        supervisor-claim (supervisor-predicate "_CLAIMS_WORK_EXHAUSTED_EDN" record-path)]
    (is (= :no-task-selected (:absent result)))
    (is (= [] (:outer-field/admitted retained)))
    (is (= #{"M-ready" "M-unknown"}
           (set (map :target (:outer-field/excluded retained)))))
    (is (every? #(= :c1-already-satisfied (:reason %))
                (:outer-field/excluded retained)))
    (is (= :loop-stop (get-in result [:refusal/disposition :disposition])))
    (is (= {:status :abstained :case :empty-field}
           (:selection-outcome record)))
    (is (= :loop-stop
           (get-in record [:outer/selection :refusal/disposition :disposition])))
    (is (= 0 (:exit supervisor-check)))
    (is (= "true\n" (:out supervisor-check)))
    (is (= "true\n" (:out supervisor-claim)))))

(deftest c3-legal-terminal-exclusion-makes-work-exhaustion-reachable
  ;; RefusalLegality.legal_terminal_excludes / legal_terminals_bounded: a
  ;; current C3 exclusion removes the last scored target and reaches L1.
  (let [sha "fixture-sha"
        digest (identity/digest {:mission-source-sha256 sha})
        rows (mapv (fn [target]
                     {:c3-exclusion
                      {:status :row
                       :row {:schema :wm/c3-exclusion-v1
                             :target target
                             :evidence-digest digest
                             :rule :H1
                             :declined-action-digest (str "action-" target)
                             :run-id (str "harm-" target)}}})
                   ["M-ready" "M-unknown"])
        result (call-ordinary
                {:decision-as-of "2026-10-28T12:00:00Z"}
                {#'outer-observation/outer-observation
                 (fn [_]
                   (assoc fixtures/base-observation
                          :mission-source-sha256-by-target
                          {"M-ready" sha "M-unknown" sha}
                          :giraffe/run-id "fixture-missions-run"))
                 #'effective-run-history/index-rows (fn [_] rows)
                 #'runtime/run-ordinary-opportunity-with-task!
                 (fn [_] (throw (AssertionError. "empty field must not run a task")))})
        retained (:outer/selection result)]
    (is (= :no-task-selected (:absent result)))
    (is (= [] (:outer-field/admitted retained)))
    (is (= #{{:target "M-ready" :reason :c3-harm-declined}
             {:target "M-unknown" :reason :c3-harm-declined}}
           (set (map #(select-keys % [:target :reason])
                     (:outer-field/excluded retained)))))
    (is (= :loop-stop (get-in result [:refusal/disposition :disposition])))))

(deftest legal-empty-field-does-not-hold-an-attached-debugger
  (let [awaited (atom 0)
        sha "fixture-sha"
        digest (identity/digest {:mission-source-sha256 sha})
        rows (mapv (fn [target]
                     {:c1-exclusion {:status :row
                                     :row {:target target :evidence-digest digest
                                           :run-id (str "satisfied-" target)}}})
                   ["M-ready" "M-unknown"])
        result (call-ordinary
                {:decision-as-of "2026-10-28T12:00:00Z"}
                {#'outer-observation/outer-observation
                 (fn [_]
                   (assoc fixtures/base-observation
                          :mission-source-sha256-by-target {"M-ready" sha "M-unknown" sha}
                          :giraffe/run-id "fixture-missions-run"))
                 #'effective-run-history/index-rows (fn [_] rows)
                 #'debugger/attached? (constantly true)
                 #'debugger/await-restart! (fn [& _] (swap! awaited inc) {:action :abort})})]
    (is (zero? @awaited))
    (is (= :loop-stop (get-in result [:refusal/disposition :disposition])))))

(deftest defect-exclusion-does-not-become-work-exhaustion
  (let [sha "fixture-sha"
        row {:c1-exclusion
             {:status :row
              :row {:schema :wm/c1-exclusion-v1
                    :target "M-ready"
                    :evidence-digest (identity/digest {:mission-source-sha256 sha})
                    :run-id "satisfied-ready"}}}
        result (call-ordinary
                {:decision-as-of "2026-10-28T12:00:00Z"}
                {#'outer-observation/outer-observation
                 (fn [_]
                   (assoc fixtures/base-observation
                          :relation (fn [_ _] :unknown)
                          :mission-source-sha256-by-target {"M-ready" sha}
                          :giraffe/run-id "fixture-missions-run"))
                 #'effective-run-history/index-rows (fn [_] [row])})]
    (is (= :outer-selection-refused (:absent result)) (pr-str result))
    (is (= :outer-score/no-admitted-tasks (:reason result)) (pr-str result))
    (is (not= :loop-stop (get-in result [:refusal/disposition :disposition])))))

(defn- record-files [dir]
  (filter #(and (.isFile %)
                (.startsWith (.getName %) "tick-run-record-")
                (not (.endsWith (.getName %) ".learning.edn")))
          (file-seq dir)))

(deftest runner-persists-only-supplied-outer-selection
  (let [root (temp-dir)
        record-dir (io/file root "records")
        selection {:cascade-id :ready
                   :cascade-scores [{:cascade-id :ready :g 17/10}]
                   :tie? false
                   :tied-cascades [:ready]
                   :giraffe/run-id "fixture-run"
                   :giraffe/manifest-sha256 (apply str (repeat 64 "b"))
                   :makeup/sha256 (apply str (repeat 64 "a"))
                   :prediction {:ending-class {:grounded-change 3/4}
                                :gain {:several 2/3}}}
        base-opts (merge (hermetic/runner-repair-options)
                         {:run-record-dir (.getPath record-dir)
                          :serving-envelope-fn (constantly serving-envelope/absent-receipt)
                          :scan-render-fn (fn [& _] nil)
                          :meta-injury-root (.getPath (io/file root "injuries"))
                          :meta-injury-ledger-path (.getPath (io/file root "injuries/ledger.edn"))})]
    (is (empty? (record-files record-dir)))
    ;; The persistence witness runs from an isolated worktree. Compare the
    ;; actually loaded runtime to that checkout, not unrelated canonical bytes.
    ;; The source checker still hashes and compares the files; it is not stubbed.
    (with-redefs [load-identity/required-sources
                  (assoc load-identity/required-sources 'futon2.aif.full-loop-runtime
                         (.getCanonicalPath (io/file "src/futon2/aif/full_loop_runtime.clj"))
                         'futon2.aif.full-loop-runner
                         (.getCanonicalPath (io/file "src/futon2/aif/full_loop_runner.clj")))
                  runner/run-opportunity-core!
                  (fn [_]
                    {:attempt-id "outer-selection-persistence"
                     :outcome :grounded-change
                     :wm/route [{:node :R20 :via "fixture"
                                 :at "2026-10-28T00:00:00Z"}]})]
      (let [with-selection (runner/run-opportunity!
                            (assoc base-opts
                                   :run-id "outer-selection-present"
                                   :click-id "outer-selection-present"
                                   :outer/selection selection))
            present-record (edn/read-string (slurp (:run-record with-selection)))
            without-selection (runner/run-opportunity!
                               (assoc base-opts
                                      :run-id "outer-selection-absent"
                                      :click-id "outer-selection-absent"))
            absent-record (edn/read-string (slurp (:run-record without-selection)))]
        (is (= selection (:outer/selection present-record)))
        (is (not (contains? present-record :outer-task-selection)))
        (is (not (contains? absent-record :outer-task-selection)))
        (let [project (fn [r] (get-in (run-facts/selection-facts-for-record r "fresh-record" {} {})
                                     [:facts "pathAbsenceCount"]))
              with-old-stub (assoc present-record :outer-task-selection
                                   {:status :absent :reason :receipt-not-retained})]
          (is (= (inc (project present-record)) (project with-old-stub))))
        (is (not (contains? absent-record :outer/selection)))
        (is (= 2 (count (record-files record-dir))))))))

(deftest reading-refusal-forbids-without-changing-the-field
  ;; First two ready rows extracted unchanged from pass
  ;; 20261101T044121.422Z-730407c5 (original SHA256
  ;; a9ad27a8cb6fd0c808413ce639019b117aed746fbcb6b2945560cfc71423ff91).
  ;; Keep these retained facts in the test classpath, independent of live storage.
  (let [text (slurp (io/resource "futon2/fixtures/outer-selection-ready-rows.edn"))
        _ (is (= "7cd3197af9549f7b7dbe10ec397590511a54605aa7edfdaa58d936f247a91d49"
                 (identity/sha256 text)))
        rows (edn/read-string text)
        targets (mapv (comp name :id) rows)
        facts (into {} (map (fn [row] [(name (:id row))
                                       (assoc row :pin (get-in row [:dependency-reading :record-pin :sha256]))]) rows))
        field (mapv #(hash-map :target % :reach 3/4) targets)
        observation {:field field :facts facts :focus (first targets)
                     :relation (fn [_ t] (if (= t (first targets)) :focus :associated))
                     :field-facts {:enumerated targets :admitted facts :excluded []}}
        makeup {:cascades (mapv (fn [t] {:id t :steps [{:gate #(= t (:target %))}]}) targets)}
        selection (outer-select/select makeup fixtures/base-state observation)
        carrier (atom {:task {:id (first targets)} :outer/selection selection})
        persisted (atom [])
        port (runtime/reading-reselection-port makeup fixtures/base-state observation selection carrier
                                               #(swap! persisted conj %))
        refusal {:target (first targets) :kind :repeat-choice-after-refusal :digest "retained-input"}
        result (port refusal)
        retained (:selection result)]
    (is (= (second targets) (:target result)))
    (is (= (:field-facts selection) (:field-facts retained)))
    (is (= (set targets) (set (map :pick (:cascades retained)))))
    (is (= {:target (first targets) :reason :repeat-choice-after-refusal
            :pin (identity/digest refusal) :receipt refusal} (first (:forbidden retained))))
    (is (= [retained] @persisted))
    (is (= (second targets) (get-in @carrier [:task :id])))
    (is (nil? (port refusal)))
    (is (= 1 (count @persisted)))))

(deftest exhausted-forbidden-menu-retains-refusal-without-pending-write
  (let [observation fixtures/base-observation
        makeup {:cascades [(first (:cascades fixtures/base-makeup))]}
        selection (outer-select/select makeup fixtures/base-state observation)
        carrier (atom {:task {:id "M-ready"} :outer/selection selection})
        writes (atom 0)
        port (runtime/reading-reselection-port makeup fixtures/base-state observation selection carrier
                                               (fn [_] (swap! writes inc)))
        error (try (port {:target "M-ready" :kind :repeat-choice-after-refusal})
                   (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (= :no-admissible-pick (:kind error)))
    (is (= :no-admissible-pick (get-in @carrier [:outer/selection :reason])))
    (is (= (:cascades selection) (get-in @carrier [:outer/selection :cascades])))
    (is (nil? (:task @carrier)))
    (is (zero? @writes))))

(deftest scoring-refusal-retains-the-computed-learning-status
  (doseq [status [:none :applied :deferred]]
    (let [dir (temp-dir)
          learning {:status status :reason :fixture :receipt {:run-id "previous"}
                    :state (outer-state/init-state makeup)}
          result (call-ordinary
                  {:run-record-dir (str dir)
                   :world-task-sets-fn (fn [_] {:missions [] :excursions [] :tickets []})}
                  {#'outer-observation/outer-observation (fn [_] {:field [] :facts {} :focus "WM"})
                   #'runtime/resolve-pending-learning (fn [& _] learning)
                   #'outer-select/select (fn [& _]
                                          (throw (ex-info "fixture scoring refusal"
                                                   {:kind :pick-not-below-abstention
                                                    :selection-evidence {:classes []}})))})
          retained (edn/read-string (slurp (get-in result [:selection-record :path])))]
      (is (= :refused (get-in retained [:selection-outcome :status])))
      (is (= (dissoc learning :state) (get-in retained [:outer/selection :outer/learning]))))))
