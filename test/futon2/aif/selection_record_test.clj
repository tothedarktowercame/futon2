(ns futon2.aif.selection-record-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [futon2.aif.selection-record :as store]
            [futon2.aif.previous-run :as previous]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.outer-select :as select]
            [futon2.aif.outer-state :as state]
            [futon2.aif.outer-observation :as observation]
            [wm-run-facts :as facts]))

(defn temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory "selection-record-test" (make-array java.nio.file.attribute.FileAttribute 0))))
(def section
  {:construction {:policy-count 1} :classes [] :cascades []
   :chosen-class "class-1" :selection {:selected {:target "M-task"}}
   :choice-input-digest {:sha256 "unchanged" :covers {:focus "M-focus"}}
   :outcome {:status :chosen}})
(def opts {:run-id "first" :startedAt "2026-11-01T12:00:00Z"})
(def snap {:open-task-ids #{"M-task"} :patterns #{} :seats #{}})

(deftest immutable-decision-completion-and-predecessor
  (let [dir (temp-dir)
        ref (store/write-decision! dir opts section {:status :refused :kind :repeat-choice})
        record (store/read-reference ref)
        before (slurp (:path ref))
        p (previous/selection-predecessor dir "second" "2026-11-01T12:01:00Z")
        exported (facts/selection-facts-for-record
                  {:outer/selection (assoc section :previous p)} "fixture" snap {})]
    (is (= :refused (:outcome p)))
    (is (= "unchanged" (:choice-input-digest p)))
    (is (= "class-1" (:chosen-class p)))
    (is (= "refused" (get-in exported [:facts "previousOutcome"])))
    (is (= "unchanged" (get-in exported [:facts "previousInputDigest"])))
    (is (= {"target" "M-task" "cascade" "class-1"} (get-in exported [:facts "previousChoice"])))
    (is (= "not-run" (get-in (facts/selection-facts-for-record record (:path ref) snap {}) [:diagnostics :workRunStatus])))
    (store/complete! ref {:status :refused})
    (is (= before (slurp (:path ref))))
    (is (= (:sha256 ref) (store/file-sha (:path ref))))
    (is (thrown? java.nio.file.FileAlreadyExistsException
                 (store/write-decision! dir opts section {:status :chosen})))
    (spit (:path ref) "{}")
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"does not match" (store/read-reference ref)))
    (is (= :not-recomputable (:status (previous/selection-predecessor dir "second" "2026-11-01T12:01:00Z"))))))

(deftest tick-reference-is-authoritative-and-completion-proves-work
  (let [dir (temp-dir) ref (store/write-decision! dir opts section {:status :chosen})
        tick (io/file dir "tick.edn")
        record {:run/id "first" :selection-record ref :outer/selection section}
        projected (facts/selection-facts-for-record record (str tick) snap {})]
    (is (= {"target" "M-task" "cascade" "class-1"} (get-in projected [:facts "currentChoice"])))
    (is (= {"not-recomputable" "selection-reselected-after-decision"}
           (get-in (facts/selection-facts-for-record (assoc record :outer/selection {:chosen-class "wrong"}) (str tick) snap {})
                   [:facts "currentChoice"])))
    (spit tick (pr-str record))
    (store/complete! ref {:run-record tick :outcome :incomplete})
    (is (= :changed (:outcome (previous/selection-predecessor dir "second" "2026-11-01T12:01:00Z"))))
    (is (= (store/file-sha tick) (get-in (edn/read-string (slurp (str (:path ref) ".completion.edn"))) [:tick-record :sha256])))
    (spit (:path ref) "{}")
    (is (thrown? clojure.lang.ExceptionInfo (facts/selection-facts-for-record record (str tick) snap {})))))

(defn call-ordinary [dir selector inner]
  (with-redefs-fn
    {#'runtime/find-giraffe-run (constantly dir)
     #'state/load-makeup (constantly {:makeup/sha256 "makeup-pin"})
     #'state/read-state (fn [& _] {})
     #'state/write-state! (fn [& _] nil)
     #'runtime/resolve-pending-learning (fn [& _] {:status :none :state {}})
     #'observation/outer-observation (fn [_] {:giraffe/run-id "pinned-pass" :focus "M-focus" :relation (fn [& _] :associated)})
     #'select/select selector
     #'runtime/run-ordinary-opportunity-with-task! inner}
    #(#'runtime/run-ordinary-opportunity! (assoc opts :run-record-dir dir))))

(deftest refusal-retained-and-success-retained-before-inner-entry
  (let [dir (temp-dir)
        result (call-ordinary dir
                 (fn [& _] (throw (ex-info "refused" {:kind :test-refusal :selection-evidence section})))
                 (fn [_] (throw (AssertionError. "inner ran"))))
        record (store/read-reference (:selection-record result))]
    (is (= :test-refusal (get-in record [:selection-outcome :kind])))
    (is (= section (select-keys (:outer/selection record) (keys section))))
    (is (empty? (filter #(str/starts-with? (.getName %) "tick-") (.listFiles dir)))))
  (let [dir (temp-dir)
        result (call-ordinary dir (fn [& _] section)
                 (fn [o]
                   (is (= :chosen (get-in (store/read-reference (:selection-record o)) [:selection-outcome :status])))
                   (let [tick (io/file dir "tick.edn")]
                     (spit tick (pr-str {:selection-record (:selection-record o)}))
                     {:run-record tick :outcome :incomplete})))
        tick (edn/read-string (slurp (:run-record result)))]
    (is (= (:selection-record result) (:selection-record tick)))
    (is (= "class-1" (get-in (store/read-reference (:selection-record tick)) [:outer/selection :chosen-class])))))

(deftest no-giraffe-and-meta-bypass-are-retained
  (let [dir (temp-dir)]
    (with-redefs [runtime/find-giraffe-run (constantly nil)]
      (is (= :no-verified-giraffe-run
             (get-in (store/read-reference (:selection-record (#'runtime/run-ordinary-opportunity! (assoc opts :run-record-dir dir))))
                     [:selection-outcome :kind])))))
  (let [dir (temp-dir)]
    (with-redefs-fn {#'runtime/resolve-medic-authority (constantly {:status :refused :reason :test-authority})}
      #(let [result (runtime/run-opportunity! (assoc opts :run-record-dir dir))
             record (store/read-reference (:selection-record result))]
         (is (= {:status :not-exercised :path :meta-self-heal :reason :test-authority} (:selection-outcome record)))
         (is (.isFile (io/file (get-in result [:selection-completion :path]))))))))

(deftest reading-reselection-retains-a-chain-and-tick-names-terminal
  (let [dir (temp-dir) n (atom 0) root-ref (atom nil)
        result (call-ordinary dir
                 (fn [& _] (if (= 1 (swap! n inc)) section
                               (assoc section :chosen-class "class-2"
                                              :selection {:selected {:target "M-next"}})))
                 (fn [opts]
                   (reset! root-ref (:selection-record opts))
                   ((:selected-target-reading-reselect-fn opts)
                    {:target "M-task" :kind :reader-refused})
                   (let [current @(:reading-reselection/state opts)
                         tick (io/file dir "tick.edn")]
                     (spit tick (pr-str (assoc (select-keys current [:outer/selection :selection-record]) :run/id "first")))
                     {:run-record tick :outcome :incomplete})))
        terminal (:selection-record result)
        chain (store/read-chain terminal)
        record (edn/read-string (slurp (:run-record result)))
        projected (facts/selection-facts-for-record record (str (:run-record result)) snap {})]
    (is (= 2 (:length chain)))
    (is (= @root-ref (:root chain)))
    (is (= (:sha256 @root-ref) (:supersedes (store/read-reference terminal))))
    (is (= :reader-refusal (get-in (store/read-reference terminal) [:cause :kind])))
    (is (= (:outer/selection (store/read-reference terminal)) (:outer/selection record)))
    (is (= {"target" "M-next" "cascade" "class-2"} (get-in projected [:facts "currentChoice"])))
    (store/complete! terminal result)
    (is (= @root-ref (:selection-root (store/read-completion terminal))))
    (is (= terminal (:selection-record (store/read-completion @root-ref))))
    (is (= 2 (:chain-length (previous/selection-predecessor dir "next" "2026-11-01T12:02:00Z"))))
    (is (= "class-2" (:chosen-class (previous/selection-predecessor dir "next" "2026-11-01T12:02:00Z"))))))

(deftest retained-comparison-survives-refusal-roundtrip
  (let [{:keys [section source]} (edn/read-string (slurp "test/fixtures/wm/l5-4f-selection-decision.edn"))
        dir (temp-dir)
        ref (store/write-decision! dir opts section {:status :refused :kind :fixture-post-comparison-refusal})
        next (previous/selection-predecessor dir "next" "2026-11-01T12:02:00Z")]
    (is (= 64 (count (:sha256 source))))
    (is (= "M-pattern-retrieval-calibration" (:pick next)))
    (is (= 2 (count (:classes section))))
    (is (= section (:outer/selection (store/read-reference ref))))
    (is (= :refused (:outcome next)))
    (is (= (get-in section [:choice-input-digest :sha256]) (:choice-input-digest next)))))

(deftest missing-index-and-changed-completion-do-not-skip-the-refusal
  (let [dir (temp-dir) ref (store/write-decision! dir opts section {:status :refused})]
    (.delete (io/file dir store/index-name))
    (is (= :not-recomputable (:status (previous/selection-predecessor dir "second" "2026-11-01T12:01:00Z"))))
    (store/complete! ref {:status :refused})
    (spit (str (:path ref) ".completion.edn") "{}")
    (is (thrown? clojure.lang.ExceptionInfo (store/read-completion ref)))))

(deftest filename-carries-click-id-while-record-retains-run-id
  (let [dir (temp-dir) ref (store/write-decision! dir (assoc opts :click-id "wm-click-explicit") section {:status :chosen})]
    (is (= "selection-record-2026-11-01-wm-click-explicit.edn" (.getName (io/file (:path ref)))))
    (is (= "first" (:run/id (store/read-reference ref))))))
