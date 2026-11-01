(ns futon2.aif.not-exercised-selection-test
  "L5-5a: a bypass is projected, alerted and read back as a bypass. Scratch
  decision records go through the real store, projector and predecessor reader."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.previous-run :as previous]
            [futon2.aif.selection-record :as store]
            [wm-run-facts :as facts]))

(defn- temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "not-exercised" (make-array java.nio.file.attribute.FileAttribute 0))))

(def ^:private snap {:open-task-ids #{"M-task"} :patterns #{} :seats #{}})

(def ^:private exercised
  {:construction {:policy-count 1} :classes [] :cascades []
   :chosen-class "class-1" :selection {:selected {:target "M-task"}}
   :choice-input-digest {:sha256 "unchanged" :covers {:focus "M-focus"}}
   :outcome {:status :chosen}})

(defn- at [n] (format "2026-11-01T12:%02d:00Z" n))

(defn- direct-entry!
  "The real direct-task branch, stopped at the inner entry."
  [dir run-id started extra]
  (with-redefs-fn {#'runtime/run-ordinary-opportunity-with-task! (fn [o] (select-keys o [:selection-record]))}
    #(:selection-record
      (#'runtime/run-ordinary-opportunity!
       (merge {:run-id run-id :startedAt started :run-record-dir dir :task {:id "M-asked"}} extra)))))

(defn- bypass! [dir run-id started path]
  (store/write-decision! dir {:run-id run-id :startedAt started}
                         {:status :not-exercised :reason path}
                         {:status :not-exercised :path path :reason :test}))

(deftest direct-task-record-names-the-task-and-its-source
  (let [dir (temp-dir)
        record (store/read-reference
                (direct-entry! dir "direct" (at 0)
                               {:trigger :duree-click-on-demand
                                :issuer-provenance {:status :present :identity "claude-3"
                                                    :source :wm-click-http-boundary}}))
        api (:selection-outcome record)
        bare (:selection-outcome (store/read-reference (direct-entry! dir "bare" (at 1) {})))]
    (is (= "M-asked" (:task api) (get-in record [:outer/selection :task])))
    (is (= {:kind :api :caller "claude-3" :trigger :duree-click-on-demand
            :session-id {:status :absent :reason :session-id-not-supplied}}
           (:entry api)))
    (testing "an in-process caller is not reported as an API entry"
      (is (= :in-process (get-in bare [:entry :kind])))
      (is (= {:status :absent :reason :issuer-not-supplied} (get-in bare [:entry :caller]))))))

(deftest not-exercised-record-projects-as-not-exercised
  (let [dir (temp-dir)
        ref (direct-entry! dir "direct" (at 0) {})
        out (facts/selection-facts-for-record (store/read-reference ref) (:path ref) snap {})
        why {"not-recomputable" "selection not exercised: direct-task"}
        kept #{"kind" "policyGScale" "openMissions" "openExcursions" "openTickets"}]
    (is (not (str/includes? (pr-str (:facts out)) "abstain-empty-field")))
    (is (= "selection" (get-in out [:facts "kind"])))
    (is (= why (get-in out [:facts "currentChoice"])))
    (is (= why (get-in out [:facts "enumeratedTasks"]))
        "the record holds no field, so an empty list would claim an empty field")
    (is (every? (fn [[k v]] (or (kept k) (= why v))) (:facts out)))
    (is (= "direct-task" (get-in out [:diagnostics :selectionBypass])))
    (is (= "M-asked" (get-in out [:diagnostics :selectionOutcome :task])))
    (is (= "not-run" (get-in out [:diagnostics :workRunStatus])) "no completion, no tick")))

(deftest a-field-the-record-holds-is-kept
  (let [dir (temp-dir)
        ref (store/write-decision! dir {:run-id "r" :startedAt (at 0)}
                                   {:status :not-exercised :outer-field/admitted ["M-a"]
                                    :outer-field/excluded [{:target "M-b" :reason :open}]}
                                   {:status :not-exercised :path :direct-task})
        out (facts/selection-facts-for-record (store/read-reference ref) (:path ref) snap {})]
    (is (= ["M-a" "M-b"] (sort (get-in out [:facts "enumeratedTasks"]))))
    (is (= {"not-recomputable" "selection not exercised: direct-task"}
           (get-in out [:facts "currentChoice"])))))

(deftest work-status-follows-the-completion-receipt
  (let [dir (temp-dir)
        ref (bypass! dir "b" (at 0) :direct-task)
        tick (doto (io/file dir "tick.edn") (spit "{:run/id \"b\"}"))
        before (facts/selection-facts-for-record (store/read-reference ref) (:path ref) snap {})]
    (is (= "not-run" (get-in before [:diagnostics :workRunStatus])))
    (store/complete! ref {:run-record tick :outcome :incomplete})
    (let [out (facts/selection-facts-for-record (store/read-reference ref) (:path ref) snap {})]
      (is (nil? (get-in out [:diagnostics :workRunStatus])))
      (is (= (str tick) (get-in out [:diagnostics :workRecord]))))))

(deftest a-tick-referencing-a-bypass-projects-as-not-exercised
  (let [dir (temp-dir)
        ref (bypass! dir "b" (at 0) :direct-task)
        out (facts/selection-facts-for-record {:run/id "b" :selection-record ref}
                                              (str (io/file dir "tick.edn")) snap {})]
    (is (= {"not-recomputable" "selection not exercised: direct-task"}
           (get-in out [:facts "currentChoice"])))
    (is (nil? (get-in out [:diagnostics :workRunStatus])))))

(deftest predecessor-walks-back-past-bypasses
  (let [dir (temp-dir)]
    (store/write-decision! dir {:run-id "real" :startedAt (at 0)} exercised
                           {:status :refused :kind :repeat-choice})
    (bypass! dir "direct" (at 1) :direct-task)
    (let [p (previous/selection-predecessor dir "next" (at 2))
          out (facts/selection-facts-for-record
               {:outer/selection (assoc exercised :previous p)} "fixture" snap {})]
      (is (= :present (:status p)))
      (is (= {:count 1 :paths [:direct-task]} (:previous-skipped p)))
      (is (= :refused (:outcome p)))
      (is (= "class-1" (:chosen-class p)))
      (is (= "unchanged" (get-in out [:facts "previousInputDigest"])))
      (is (= "refused" (get-in out [:facts "previousOutcome"])))
      (is (= {"target" "M-task" "cascade" "class-1"} (get-in out [:facts "previousChoice"]))))
    (bypass! dir "heal" (at 3) :meta-self-heal)
    (is (= {:count 2 :paths [:meta-self-heal :direct-task]}
           (:previous-skipped (previous/selection-predecessor dir "next" (at 4)))))))

(deftest a-refusal-before-scoring-is-passed-over-like-a-bypass
  ;; click 4 of 2026-11-01: refused before the field was read, no input digest
  (let [dir (temp-dir)]
    (store/write-decision! dir {:run-id "real" :startedAt (at 0)} exercised {:status :chosen :pick "M-task"})
    (store/write-decision! dir {:run-id "refused-early" :startedAt (at 1)}
                           {:status :absent :reason :selection-failed-before-scoring
                            :failure-evidence {:cascade-digest nil :cascade-id :what-others-wait-on-first}}
                           {:status :refused :kind :pending-steps-unrecoverable
                            :detail {:cascade-digest nil :cascade-id :what-others-wait-on-first}})
    (let [p (previous/selection-predecessor dir "next" (at 2))]
      (is (= :present (:status p)))
      (is (= "class-1" (:chosen-class p)))
      (is (= {:count 1 :paths [:pending-steps-unrecoverable]} (:previous-skipped p))))
    (testing "a refusal after scoring (an input digest exists) is a real predecessor"
      (store/write-decision! dir {:run-id "refused-late" :startedAt (at 3)}
                             (assoc exercised :outcome {:status :refused})
                             {:status :refused :kind :repeat-choice})
      (let [p (previous/selection-predecessor dir "next" (at 4))]
        (is (= :refused (:outcome p)))
        (is (not (contains? p :previous-skipped)))))))

(deftest no-skip-without-a-bypass
  (let [dir (temp-dir)]
    (store/write-decision! dir {:run-id "real" :startedAt (at 0)} exercised {:status :chosen :pick "M-task"})
    (is (not (contains? (previous/selection-predecessor dir "next" (at 1)) :previous-skipped)))))

(deftest only-bypasses-keeps-a-typed-absence
  (let [dir (temp-dir)]
    (bypass! dir "direct" (at 0) :direct-task)
    (let [p (previous/selection-predecessor dir "next" (at 1))]
      (is (= :absent (:status p)))
      (is (= {:count 1 :paths [:direct-task]} (:previous-skipped p))))))

(deftest a-changed-record-behind-a-bypass-still-refuses
  (let [dir (temp-dir)
        real (store/write-decision! dir {:run-id "real" :startedAt (at 0)} exercised {:status :chosen :pick "M-task"})]
    (bypass! dir "direct" (at 1) :direct-task)
    (spit (:path real) "{}")
    (is (= :not-recomputable (:status (previous/selection-predecessor dir "next" (at 2))))
        "the walk may pass a bypass, never a record it cannot verify")))

(deftest the-walk-reaches-the-sealed-tick-stream
  (let [dir (temp-dir)
        terminal (previous/terminal {:run-id "old" :started-at (at 0)})
        file (io/file dir "retained.edn")]
    (previous/start! dir "old" (at 0))
    (spit file (pr-str {:run/id "old" :outer/selection exercised :decision {:status :ran}
                        :q6 {:terminal terminal}}))
    (previous/close! dir terminal file)
    (bypass! dir "first-bypass" (at 1) :meta-self-heal)
    (let [p (previous/selection-predecessor dir "next" (at 2))]
      (is (= :present (:status p)))
      (is (= "class-1" (:chosen-class p)))
      (is (= {:count 1 :paths [:meta-self-heal]} (:previous-skipped p))))))

(deftest authority-refusal-and-arbitration-exception-carry-their-labels
  (let [dir (temp-dir)]
    (with-redefs-fn {#'runtime/resolve-medic-authority (constantly {:status :refused :reason :no-authority})}
      #(let [record (store/read-reference
                     (:selection-record (runtime/run-opportunity!
                                         {:run-id "a" :startedAt (at 0) :run-record-dir dir})))]
         (is (= :authority-refusal (get-in record [:selection-outcome :path])))
         (is (= :authority-refusal (get-in record [:outer/selection :reason])))
         (is (= :no-authority (get-in record [:selection-outcome :reason])))))
    (with-redefs-fn {#'runtime/resolve-medic-authority (fn [_] (throw (ex-info "boom" {})))}
      #(let [state (atom nil)]
         (with-redefs [store/complete! (fn [ref _] (reset! state ref) nil)]
           (is (thrown? clojure.lang.ExceptionInfo
                        (runtime/run-opportunity! {:run-id "e" :startedAt (at 1) :run-record-dir dir}))))
         (is (= {:path :arbitration-exception :reason :opportunity-exception}
                (select-keys (:selection-outcome (store/read-reference @state)) [:path :reason])))))))

(deftest a-self-heal-route-keeps-its-own-label
  (let [dir (temp-dir)]
    (with-redefs-fn {#'runtime/resolve-medic-authority (constantly {:status :ready})
                     #'runtime/run-opportunity-with-authority!
                     (constantly {:status :refused :reason :meta-self-heal/production-config-required
                                  :route :dedicated-meta-self-heal})}
      #(let [record (store/read-reference
                     (:selection-record (runtime/run-opportunity!
                                         {:run-id "h" :startedAt (at 0) :run-record-dir dir})))]
         (is (= :meta-self-heal (get-in record [:selection-outcome :path])))
         (is (= :meta-self-heal-route (get-in record [:outer/selection :reason])))))))
