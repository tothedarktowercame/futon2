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
   :cascades [{:id :ready :patterns ["test"] :steps [:ready-gate]}]
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
    (is (vector? (get-in @received [:outer/selection :cascade-scores])))
    (is (= "fixture-missions-run"
           (get-in @received [:outer/selection :giraffe/run-id])))
    (is (nil? (get-in @received [:outer/selection :selection :candidates])))))

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
    (is (= [:none] (get-in result [:outer/selection :stopping-equivalent 0 :members])))
    (is (false? @called?))))

(deftest no-target-disposition-is-checked-against-the-retained-admitted-field
  (let [empty-field (#'runtime/work-exhausted-refusal {:outer-field/admitted []})
        live-field (#'runtime/work-exhausted-refusal
                    {:outer-field/admitted ["M-still-selectable"]})
        missing-field (#'runtime/work-exhausted-refusal {})]
    (is (= :loop-stop (:refusal/disposition empty-field)))
    (is (= [] (get-in empty-field [:refusal/evidence :scoredField])))
    (is (= :debugger-error (:refusal/disposition live-field)))
    (is (= ["M-still-selectable"]
           (get-in live-field [:refusal/evidence :scoredField])))
    (is (= :debugger-error (:refusal/disposition missing-field)))
    (is (nil? (get-in missing-field [:refusal/evidence :scoredField])))))

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
