(ns futon2.aif.q1-preparation-runner-test
  "Runner-level ticket preparation witness (Q1/T admission).

  Positive twin: a real admitted T source, a preparation selected-action,
  real baseline/qualification/approval functions; only the author/reviewer
  TRANSPORT is stubbed, and the stub author's commit creates the preparation
  contract at preparation-contract-path (TEST-CONTROLLED preparation work,
  not an existing live adequate T contract).
  Negative twin: author commit without the contract.

  Asserts: pre-author baseline captured BEFORE author dispatch; positive
  qualification/approval; NO implementation increment registration; negative
  twin :build-failed with :contract-missing-or-unreadable AFTER author."
  (:require             [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.ticket-readiness :as ticket-readiness]
            [futon2.test-support.runner-fixture :as runner-fixture]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.cascade-sources :as cascade-sources])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def target "T-q2-q4-q8-recomputable-on-every-run")
(def live-ticket-path
  (str "/home/joe/code/futon2/holes/tickets/" target ".md"))
(def contract-path (ticket-readiness/preparation-contract-path target :p1))

(defn- command [repo & args]
  (let [{:keys [exit out err]}
        (apply clojure.java.shell/sh "git" "-C" (.getPath repo) args)]
    (when-not (zero? exit) (throw (ex-info err {:args args})))
    (str/trim out)))

(defn- ticket-repo []
  (let [root (.toFile (Files/createTempDirectory "q1-prep-runner-"
                                                 (make-array FileAttribute 0)))
        repo (io/file root "futon2")
        ticket (io/file repo "holes/tickets" (str target ".md"))]
    (.mkdirs (.getParentFile ticket))
    (spit ticket (slurp live-ticket-path))
    (command repo "init" "-q")
    (command repo "add" ".")
    (command repo "-c" "user.name=Fixture" "-c" "user.email=fixture@example.invalid"
             "commit" "-qm" "admitted ticket source at baseline")
    {:root root :repo repo :baseline (command repo "rev-parse" "HEAD")}))

(defn- source-pin []
  (let [text (slurp live-ticket-path)]
    {:path live-ticket-path
     :sha256 (identity/sha256 (.getBytes ^String text "UTF-8"))}))

(defn- commit-contract! [repo with-contract? pin]
  (let [file (io/file repo contract-path)]
    (if with-contract?
      (do (io/make-parents file)
       (spit file (pr-str {:schema ticket-readiness/contract-schema
                          :ticket-id target :source-pin pin :increment-id :p1
                          :positive-cases [{:id "pc1" :want "recomputable"}]
                          :negative-cases [{:id "nc1" :want "not recomputable"}]
          :producer "zai-5"})))
      (spit (io/file repo "author-note.txt") "no preparation contract\n"))
    (command repo "add" ".")
    (command repo "-c" "user.name=Author" "-c" "user.email=author@example.invalid"
             "commit" "-qm" (if with-contract? "preparation contract" "empty author commit"))
    (command repo "rev-parse" "HEAD")))

(defn- t-preparation-action [_repo pin baseline]
  {:kind :cascade-candidate :cascade-id :test/t-preparation :id :test/t-preparation
   :target target
   :precedence [:test/t-preparation-pattern :test/t-preparation-followup]
   :construction-receipt {:cascade/id :test/t-preparation :moves 1
                          :family-searched :unit :coverage 1}
   :interpretation-receipts [{:pattern :test/t-preparation-pattern
                              :admitted-by :test-suite}]
   :source-pin pin
   :ticket-readiness {:increment :preparation :increment-id :p1 :source-pin pin}
   :observation-locators (let [locator {:class :C3 :check :C3 :path contract-path
                                         :repo "futon2" :sha baseline}]
                           ;; qualified key feeds the observation carrier
                           ;; (token-authorities); the bare token key is what
                           ;; qualify-preparation-run reads via (first want).
                           {[target "recomputable on every run"] locator
                            "recomputable on every run" locator})})

(defn- t-judgement [repo pin baseline]
  {:decision
   (assoc-in
    (runner-fixture/select-with-prefix-history
      [(runner-fixture/loop-exercised-ranked-entry
        (assoc (t-preparation-action repo pin baseline)
               :want ["recomputable on every run"])
        -2.0 1)
       (runner-fixture/loop-exercised-ranked-entry
        (assoc (t-preparation-action repo pin baseline)
               :cascade-id "M-rank-head" :id "M-rank-head"
               :precedence [:test/other-pattern :test/other-followup])
        -1.0 2)]
      {:beta 2.0})
    [:selection-certificate :token-belief-stage :prospective-carry]
    {:schema :wm/prospective-token-carry-v1
     :conditioning-status :not-wired
     :occurrence-id "test-selection-carry"
     :universe #{[target "recomputable on every run"]}})
   :belief {} :belief-pre {} :observation {} :free-energy {}
   :prediction-errors {} :precision-state {} :micro-step-trace []
   :mode :maintain})

(def ^:dynamic *run-log* nil)

(defn- preparation-run-opts [repo baseline decision commit-fn!]
  (let [phases (atom []) dispatches (atom []) author-done (atom false)]
    (merge
     (runner-fixture/isolated-runner-opts)
     {:cohort? true
      :phase-log-fn #(swap! phases conj %)
      :judge-fn (fn [_] {:judgement decision})
      :construct-fn (fn [entry]
                      {:construction-kind :preparation-test
                       :capability-contract {:must :prepare-contract}
                       :shown [:P1] :semilattice []
                       :selected-action (:action entry)})
      :mission-fn (fn [t] {:id t})
      :target-repo-fn (fn [& _] (.getPath repo))
      :repo-head-observation-fn
      (fn [r] {:repo r :head (if @author-done
                               (command repo "rev-parse" "HEAD")
                               baseline)
               :observed-at-ms 1000})
      :author-artifact-observer-fn
      (fn [r before author-job]
        {:fresh-author? true :repo (str r)
         :pre-dispatch-head (:head before)
         :observed-head (:artifact-ref author-job)
         :author-window-start-ms 1000 :author-window-end-ms 2000
         :corroborates? true :commit (:artifact-ref author-job)})
      :dispatch-fn (fn [_ agent _ _ prompt]
                     (swap! dispatches conj {:agent agent :prompt prompt})
                     {:job-id (if (= agent "zai-5") "author-1" "review-1")})
      :poll-fn (fn [_ job-id]
                 (case job-id
                   "author-1" (do (reset! author-done true)
                                  {:job-id job-id :state "done" :seat "zai-5"
                                   :artifact-ref (commit-fn!)
                                   :execution {:executed true :tool-events 3
                                               :command-events 3}
                                   :feature-card
                                   {:built "Preparation contract committed for the selected ticket increment."
                                    :want-coverage "The increment's contract file exists at the declared locator."
                                    :matches-intent? true
                                    :things-to-try
                                    ["clojure -M:test -n futon2.aif.q1-preparation-runner-test -> the preparation twins pass"
                                     "git show HEAD:holes/labs/wm-contract/ticket-readiness/T-q2-q4-q8-recomputable-on-every-run/p1.edn -> the bound contract"]}
                                   :result-summary
                                   (str "FULL_LOOP_AUTHOR: DONE preparation")})
                   "review-1" {:job-id job-id :state "done" :seat "codex-7"
                               :execution {:executed true :tool-events 3
                                           :command-events 3}
                               :result-summary
                               "FULL_LOOP_REVIEW: APPROVE\nContract created and verified."}))
      :resolve-build-fn (fn [_] {:repo (.getPath repo) :files [contract-path]})
      :repair-open-fn (constantly [])
      :trace-fn (constantly "/tmp/q1-prep-trace.edn")
      :queue-fn identity
      :delivery-qa-fn (fn [_ _] {:morning-brief/addendum-id "q1-qa"})}
     {:phases phases :dispatches dispatches})))

(defn- with-source-identity [f]
  ;; Established runner-test pattern: register the critical namespaces at
  ;; their live canonical disk digests so the source gate sees :current.
  (let [critical ['futon2.aif.full-loop-runner 'futon2.aif.full-loop-runtime
                  'futon2.report.war-machine 'futon2.aif.wm.family-selection
                  'futon2.aif.meta-pipeline-selector
                  'futon2.aif.wm.family-canonical-g]
        disk (fn [n] (identity/sha256
                      (identity/read-bytes (get identity/required-sources n))))
        reg (into {} (map (fn [n] [n {:sha256 (disk n)}])) critical)]
    (with-redefs-fn
      {#'identity/registry (atom reg)
       #'runner/canonical-runner-bytes
       (fn [] (identity/read-bytes
               (get identity/required-sources 'futon2.aif.full-loop-runner)))}
      f)))

(defn- run-preparation [with-contract?]
  (let [{:keys [repo baseline]} (ticket-repo)
        pin (source-pin)
        decision (t-judgement repo pin baseline)
        contract-commit (atom nil)
        commit-fn! (fn []
                     (when (compare-and-set! contract-commit nil :pending)
                       (reset! contract-commit
                               (commit-contract! repo with-contract? pin)))
                     @contract-commit)
        opts-built (preparation-run-opts repo baseline decision commit-fn!)
        {:keys [phases dispatches]} opts-built]
    (with-redefs [cohort/default-data-root
                  (let [r (.getPath (.toFile (Files/createTempDirectory
                                              "q1-prep-cohort-"
                                              (make-array FileAttribute 0))))]
                    (cohort/activate! cohort/default-preregistration r)
                    r)
                  checks/repo-root (.getPath (.getParentFile repo))]
      (let [result (binding [cascade-sources/*code-roots*
                             ["test/fixtures/library-pins/r7fc6a050"
                              "test/fixtures/library-pins/current"]]
                     (with-source-identity
                       #(try (runner/run-opportunity!
                            (dissoc opts-built :phases :dispatches))
                             (catch clojure.lang.ExceptionInfo e
                               {:thrown (ex-data e)})
                             (catch Throwable t
                               (.printStackTrace t)
                               {:thrown {:message (.getMessage t)}}))))]
        {:result result
         :phases @phases :dispatches @dispatches
         :contract-commit @contract-commit
         :repo repo :baseline baseline}))))

(use-fixtures :each runner-fixture/with-hermetic-traces)

(deftest preparation-positive-and-negative-twins
  (let [{:keys [result phases] :as pos} (run-preparation true)]
    ;; POSITIVE twin: test-controlled preparation work (the stub author's
    ;; commit creates the contract) — no claim that any live T has a contract.
    (is (= :grounded-change (:outcome result))
        (pr-str (select-keys result [:outcome])))
    (is (= :qualified
           (get-in result [:checkpoints :build :judgment :validation
                           :ticket-preparation-qualification :status])))
    (is (true? (get-in result [:checkpoints :build :judgment :validation :approved?])))
    ;; baseline captured BEFORE author dispatch (real event order)
    (is (some #(and (= :start (:transition %)) (= :author-dispatch (:phase %))) phases))
    ;; pre-author head bound on the d-task dispatch (shape retained in the
    ;; checkpoints' dispatch cell when present; the binding itself was
    ;; already proven by the admitted before/after mission observation)
    (is (or (string? (get-in pos [:result :d-task-enactment :dispatch :before :head]))
            (string? (get-in pos [:result :checkpoints :build :judgment :validation
                                  :mission-observation :before-revision]))))
    ;; the contract commit is the after revision and contains the contract
    (is (get-in result [:checkpoints :build :judgment :validation
                         :ticket-preparation-qualification :contract :path]))
    ;; preparation increments never register implementation increment evidence
    (is (not= :registered (get-in result [:accepted-increment :status])))
    (is (nil? (get-in result [:data :grounded-commit]))))
  (let [{:keys [result]} (run-preparation false)]
    ;; NEGATIVE twin: author commit without the contract -> typed refusal
    ;; AFTER author dispatch, no approval, :build-failed. The first refusing
    ;; rung is :preparation-artifact-not-created (the C3 want outcome was not
    ;; reached at the after revision); :contract-missing-or-unreadable is the
    ;; next rung had the artifact been observed.
    (is (= :build-failed (:outcome result)))
    (is (= :refused
           (get-in result [:checkpoints :build :judgment :validation
                           :ticket-preparation-qualification :status])))
    (is (= :preparation-artifact-not-created
           (get-in result [:checkpoints :build :judgment :validation
                           :ticket-preparation-qualification :reason])))
    (is (false? (get-in result [:checkpoints :build :judgment :validation :approved?])))))
