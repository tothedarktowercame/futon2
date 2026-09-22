(ns futon2.aif.full-loop-cohort-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.close-retention :as close-retention]
            [futon2.aif.evidence-manifest :as evidence-manifest]
            [futon2.aif.full-loop-cohort :as cohort])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def prereg-path
  "/home/joe/code/futon2/holes/labs/M-aif-full-loop-40/cohort.edn")

(defn tmp-root []
  (.getPath (.toFile (Files/createTempDirectory
                      "full-loop-cohort-test"
                      (make-array FileAttribute 0)))))

(defn term [judgment]
  {:judgment judgment :ground {:kind :test-witness}})

(defn open! [root opportunity-id]
  (cohort/start-attempt!
   prereg-path root
   (term {:opportunity-id opportunity-id
          :trigger :wallclock-cron
          :machine-state {:tick 1}
          :agent-roster []
          :code-state {:git-sha "abc"
                       :git-dirty? false
                       :resolved-mode-flags {}
                       :configuration-digest "test"}
          :semantic-epoch :epoch-1})))

(defn append-required! [root attempt]
  (doseq [checkpoint [:selection :construction :dispatch :build :adjudication]]
    (cohort/append-checkpoint! prereg-path root attempt checkpoint
                               {:sorry {:kind (keyword (str "test-" (name checkpoint)))}})))

(defn retention-occurrence []
  (let [ids (atom ["00000000-0000-4000-8000-000000000011"
                   "00000000-0000-4000-8000-000000000012"])]
    (close-retention/mint-occurrence
     {:run-id "writer-run" :cohort-id "wm-full-loop-40-v1"
      :attempt-id "writer-attempt" :selected-action {:type :writer-test}
      :now (constantly "2026-09-14T00:00:00Z")
      :uuid-fn #(let [id (first @ids)] (swap! ids subvec 1) id)})))

(defn retention-close [inputs]
  (assoc (term {:outcome :agent-unavailable :grounded? false
                :artifact-only? false :duration-ms 1
                :resource-use {:agent-turns 0}})
         :retention-inputs inputs))

(defn close-path [root attempt]
  (io/file root (name (:cohort/id (cohort/read-edn prereg-path)))
           attempt "007-closed.edn"))

(deftest close-writer-optionally-completes-retention
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        absent-inputs {:occurrence (retention-occurrence)
                       :state {:status :absent
                               :reason :independent-observation-unavailable}
                       :model {:status :absent
                               :reason :declared-model-identity-unthreaded}
                       :admitted-evidence []}
        legacy-attempt (:attempt/id (open! root "clock/legacy-close"))
        absent-attempt (:attempt/id (open! root "clock/retained-close"))
        observed-attempt (:attempt/id (open! root "clock/observed-close"))]
    (doseq [attempt [legacy-attempt absent-attempt observed-attempt]]
      (append-required! root attempt))
    (let [legacy (cohort/close-attempt!
                  prereg-path root legacy-attempt
                  (term {:outcome :agent-unavailable :grounded? false
                         :artifact-only? false :duration-ms 1
                         :resource-use {:agent-turns 0}}))]
      (is (not (contains? (:payload legacy) :close-retention)))
      (is (= #{:judgment :ground} (set (keys (:payload legacy))))))
    (let [event (cohort/close-attempt! prereg-path root absent-attempt
                                       (retention-close absent-inputs))
          retained (get-in event [:payload :close-retention])]
      (is (= (:recorded-at event) (:closed-at retained)))
      (is (= (:recorded-at event) (:evidence-cutoff retained)))
      (is (= :absent (get-in retained [:state :status])))
      (is (= :absent (get-in retained [:model :status])))
      (is (not (contains? (:payload event) :retention-inputs)))
      (is (= event (cohort/read-edn (close-path root absent-attempt)))))
    (let [observed (assoc absent-inputs
                          :state {:status :observed
                                  :method :independent-categorical-observation
                                  :state :refined
                                  :state-at "2026-09-14T00:02:00Z"
                                  :observed-at "2026-09-14T00:01:00Z"
                                  :evidence/id "e-observed"}
                          :admitted-evidence ["e-observed"])
          event (cohort/close-attempt! prereg-path root observed-attempt
                                       (retention-close observed))]
      (is (= :observed (get-in event [:payload :close-retention :state :status])))
      (is (= ["e-observed"]
             (get-in event [:payload :close-retention :admitted-evidence]))))))

(deftest close-writer-refuses-invalid-retention-before-append
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        base {:occurrence (retention-occurrence)
              :state {:status :absent :reason :independent-observation-unavailable}
              :model {:status :absent :reason :declared-model-identity-unthreaded}
              :admitted-evidence []}
        cases [[:caller-cutoff (assoc base :closed-at "2026-09-14T00:03:00Z")]
               [:unadmitted-state
                (assoc base :state {:status :observed
                                    :method :independent-categorical-observation
                                    :state :refined
                                    :state-at "2026-09-14T00:02:00Z"
                                    :observed-at "2026-09-14T00:01:00Z"
                                    :evidence/id "e-missing"})]
               [:duplicate-evidence (assoc base :admitted-evidence ["e" "e"])]]]
    (doseq [[label inputs] cases]
      (let [attempt (:attempt/id (open! root (str "clock/invalid-" (name label))))]
        (append-required! root attempt)
        (is (thrown? clojure.lang.ExceptionInfo
                     (cohort/close-attempt! prereg-path root attempt
                                            (retention-close inputs))))
        (is (not (.exists (close-path root attempt))))))))

(deftest retention-marker-on-non-close-checkpoint-refuses
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        attempt (:attempt/id (open! root "clock/misplaced-marker"))]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"non-close checkpoint"
                          (cohort/append-checkpoint!
                           prereg-path root attempt :selection
                           {:sorry {:kind :test-selection}
                            :retention-inputs {}})))
    (is (not (.exists (io/file root (name (:cohort/id (cohort/read-edn prereg-path)))
                               attempt "002-selection.edn"))))))

(defn evidence-manifest [ids admitted-at]
  (evidence-manifest/build-manifest
   {:entries (mapv (fn [id]
                     {:evidence/id id
                      :source-path (str "/evidence/" id ".edn")
                      :admitted-at admitted-at})
                   ids)
    :read-bytes #(.getBytes ^String (str "literal:" %) "UTF-8")}))

(deftest close-writer-retains-agreeing-evidence-manifest
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        attempt (:attempt/id (open! root "clock/manifest-close"))
        ids ["evidence-one" "evidence-two"]
        inputs {:occurrence (retention-occurrence)
                :state {:status :absent :reason :independent-observation-unavailable}
                :model {:status :absent :reason :declared-model-identity-unthreaded}
                :admitted-evidence ids}
        manifest (evidence-manifest ids "2026-09-14T00:01:00Z")]
    (append-required! root attempt)
    (let [event (cohort/close-attempt!
                 prereg-path root attempt
                 (assoc (retention-close inputs) :evidence-manifest manifest))
          payload (:payload event)]
      (is (= manifest (:close-evidence-manifest payload)))
      (is (= ids (get-in payload [:close-retention :admitted-evidence])))
      (is (= ids (mapv :evidence/id
                       (get-in payload [:close-evidence-manifest :entries]))))
      (is (not (contains? payload :retention-inputs)))
      (is (not (contains? payload :evidence-manifest)))
      (is (= event (cohort/read-edn (close-path root attempt)))))))

(deftest close-writer-refuses-invalid-manifest-before-append
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        occurrence (retention-occurrence)
        inputs (fn [ids]
                 {:occurrence occurrence
                  :state {:status :absent :reason :independent-observation-unavailable}
                  :model {:status :absent :reason :declared-model-identity-unthreaded}
                  :admitted-evidence ids})
        good (evidence-manifest ["evidence-one" "evidence-two"]
                                "2026-09-14T00:01:00Z")
        cases [[:without-retention
                (assoc (term {:outcome :agent-unavailable :grounded? false
                              :artifact-only? false :duration-ms 1
                              :resource-use {:agent-turns 0}})
                       :evidence-manifest good)
                :manifest-without-retention]
               [:reordered
                (assoc (retention-close (inputs ["evidence-two" "evidence-one"]))
                       :evidence-manifest good)
                :retention-evidence-mismatch]
               [:future-admission
                (assoc (retention-close (inputs ["evidence-one"]))
                       :evidence-manifest
                       (evidence-manifest ["evidence-one"] "9999-01-01T00:00:00Z"))
                :evidence-admitted-after-close]
               [:invalid-sha
                (assoc (retention-close (inputs ["evidence-one" "evidence-two"]))
                       :evidence-manifest
                       (assoc good :manifest-sha256 (apply str (repeat 64 "0"))))
                :manifest-sha256-mismatch]]]
    (doseq [[label cell expected] cases]
      (let [attempt (:attempt/id (open! root (str "clock/manifest-" (name label))))]
        (append-required! root attempt)
        (is (= expected
               (try
                 (cohort/close-attempt! prereg-path root attempt cell)
                 nil
                 (catch clojure.lang.ExceptionInfo e
                   (or (:evidence-manifest/refusal (ex-data e))
                       (:close-retention/refusal (ex-data e)))))))
        (is (not (.exists (close-path root attempt))))))))

(deftest evidence-manifest-on-non-close-checkpoint-refuses
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        attempt (:attempt/id (open! root "clock/misplaced-manifest"))]
    (is (= :retention-marker-misplaced
           (try
             (cohort/append-checkpoint!
              prereg-path root attempt :selection
              {:sorry {:kind :test-selection}
               :evidence-manifest (evidence-manifest [] "2026-09-14T00:00:00Z")})
             nil
             (catch clojure.lang.ExceptionInfo e
               (:evidence-manifest/refusal (ex-data e))))))
    (is (not (.exists (io/file root (name (:cohort/id (cohort/read-edn prereg-path)))
                               attempt "002-selection.edn"))))))

(deftest preregistration-is-valid
  (let [p (edn/read-string (slurp prereg-path))]
    (is (cohort/valid-preregistration? p))
    (is (= 40 (get-in p [:stopping-rule :target])))
    (is (false? (get-in p [:grounded-success :artifact-only-counts?])))))

(deftest activation-and-attempts-are-append-only
  (let [root (tmp-root)]
    (testing "pre-activation attempts are refused"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"not activated"
                            (open! root "clock/1"))))
    (cohort/activate! prereg-path root)
    (is (= 1 (:attempt/ordinal (open! root "clock/1"))))
    (is (= 2 (:attempt/ordinal (open! root "clock/2"))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"duplicate"
                          (open! root "clock/1")))
    (is (thrown-with-msg? java.nio.file.FileAlreadyExistsException #"activation.edn"
                          (cohort/activate! prereg-path root)))))

(deftest closure-requires-the-whole-dossier
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        attempt (:attempt/id (open! root "clock/complete"))
        close (term {:outcome :agent-unavailable
                     :grounded? false
                     :artifact-only? false
                     :duration-ms 1200
                     :resource-use {:agent-turns 0}})]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"checkpoints missing"
                          (cohort/close-attempt! prereg-path root attempt close)))
    (append-required! root attempt)
    (cohort/close-attempt! prereg-path root attempt close)
    (let [ledger (cohort/ledger prereg-path root)]
      (is (= 1 (:attempt-count ledger)))
      (is (= 1 (:closed-count ledger)))
      (is (= {:agent-unavailable 1} (:outcomes ledger)))
      (is (= 5 (get-in ledger [:attempts 0 :typed-sorries]))))))

(deftest closed-execution-rejects-a-foreign-or-truncated-lifecycle
  (let [root (tmp-root)
        raw (slurp prereg-path)
        binding {:preregistration prereg-path :data-root root
                 :cohort-id (:cohort/id (edn/read-string raw))
                 :sha256 (#'cohort/sha256 raw)}
        _ (cohort/activate! prereg-path root)
        attempt (:attempt/id (open! root "clock/strict-close"))]
    (append-required! root attempt)
    (cohort/close-attempt! prereg-path root attempt
                           (term {:outcome :agent-unavailable :grounded? false
                                  :artifact-only? false :duration-ms 1
                                  :resource-use {:agent-turns 0}}))
    (is (= attempt (:attempt-id (cohort/closed-execution binding attempt))))
    (let [close (io/file root (name (:cohort-id binding)) attempt "007-closed.edn")
          value (edn/read-string (slurp close))]
      (spit close (str (pr-str (assoc value :cohort/id :foreign-cohort
                                      :attempt/id "attempt-999")) "\n"))
      (is (thrown? clojure.lang.ExceptionInfo
                   (cohort/closed-execution binding attempt))))))

(deftest execution-authority-binds-canonical-data-root
  (let [base (tmp-root)
        raw (slurp prereg-path)
        cohort-id (:cohort/id (edn/read-string raw))
        sha (#'cohort/sha256 raw)
        binding (fn [suffix]
                  {:preregistration prereg-path
                   :data-root (str base "/" suffix)
                   :cohort-id cohort-id :sha256 sha})
        a (binding "a") b (binding "b")]
    (.mkdir (io/file (:data-root a)))
    (.mkdir (io/file (:data-root b)))
    (cohort/activate! prereg-path (:data-root a))
    (cohort/activate! prereg-path (:data-root b))
    (let [aa (cohort/execution-authority a)
          aa-replay (cohort/execution-authority a)
          ba (cohort/execution-authority b)
          ai (cohort/execution-provenance aa "attempt-001")
          bi (cohort/execution-provenance ba "attempt-001")]
      (is (= aa aa-replay))
      (is (not= (:authority-id aa) (:authority-id ba)))
      (is (not= (:id ai) (:id bi)))
      (is (= "attempt-001" (:attempt-id ai) (:attempt-id bi))))))

(deftest present-malformed-execution-authority-cannot-downgrade-to-legacy
  (let [root (tmp-root)
        raw (slurp prereg-path)
        binding {:preregistration prereg-path :data-root root
                 :cohort-id (:cohort/id (edn/read-string raw))
                 :sha256 (#'cohort/sha256 raw)}
        _ (cohort/activate! prereg-path root)
        authority (cohort/execution-authority binding)
        attempt (:attempt/id
                 (cohort/start-attempt!
                  prereg-path root
                  (term {:opportunity-id "clock/authority"
                         :trigger :wallclock-cron
                         :machine-state {:tick 1}
                         :agent-roster []
                         :code-state {:git-sha "abc" :git-dirty? false
                                      :resolved-mode-flags {}
                                      :configuration-digest "test"}
                         :semantic-epoch :epoch-1
                         :execution-authority authority})))
        _ (append-required! root attempt)
        _ (cohort/close-attempt!
           prereg-path root attempt
           (term {:outcome :agent-unavailable :grounded? false
                  :artifact-only? false :duration-ms 1
                  :resource-use {:agent-turns 0}}))
        time-step (io/file root (name (:cohort-id binding)) attempt
                           "001-time-step.edn")
        original (edn/read-string (slurp time-step))]
    (is (= 1 (:identity-version (cohort/closed-execution binding attempt))))
    (doseq [malformed [nil false {} (assoc authority :authority-id (apply str (repeat 64 "0")))]]
      (spit time-step
            (pr-str (assoc-in original
                              [:payload :judgment :execution-authority]
                              malformed)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Closed cohort execution unavailable"
                            (cohort/closed-execution binding attempt))))))

(deftest grounded-checkpoints-cannot-omit-preregistered-fields
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        attempt (:attempt/id (open! root "clock/incomplete-selection"))]
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo #"invalid checkpoint cell"
         (cohort/append-checkpoint!
          prereg-path root attempt :selection
          (term {:selected-mission "M-x"}))))
    (is (= [[:missing-judgment-key :ranked-candidates]
            [:missing-judgment-key :selected-action]
            [:missing-judgment-key :selection-reasons]]
           (cohort/checkpoint-cell-errors
            (cohort/read-edn prereg-path) :selection
            (term {:selected-mission "M-x"}))))))

(deftest construction-checkpoint-refuses-silent-nil-wiring
  (let [p (cohort/read-edn prereg-path)
        base {:mission "M-x" :cascade {} :sorries [] :patterns [] :deposit nil}]
    (is (= [[:invalid-fold-output :nil-fold-output]]
           (cohort/checkpoint-cell-errors
            p :construction (term (assoc base :wiring nil :fold-output nil)))))
    (is (empty?
         (cohort/checkpoint-cell-errors
          p :construction
          (term (assoc base
                       :wiring nil
                       :fold-output
                       {:fold/refused true
                        :why "No grounded construction evidence"
                        :refusal/class :construction-evidence-unavailable})))))
    (is (= [[:invalid-fold-output :refusal-why-missing]]
           (cohort/checkpoint-cell-errors
            p :construction
            (term (assoc base :wiring nil
                         :fold-output {:fold/refused true
                                       :refusal/class :invalid})))))))

(deftest artifact-only-cannot-be-laundered-as-grounded
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        attempt (:attempt/id (open! root "clock/artifact"))]
    (append-required! root attempt)
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"invalid close"
                          (cohort/close-attempt!
                           prereg-path root attempt
                           (term {:outcome :grounded-change
                                  :grounded? true
                                  :artifact-only? true
                                  :witness {:before "b" :after "a"
                                            :resolved? true :dial-moved? true}}))))
    (cohort/close-attempt!
     prereg-path root attempt
     (term {:outcome :artifact-only :grounded? false :artifact-only? true
            :duration-ms 20 :resource-use {:agent-turns 0}}))
    (is (= 1 (:artifact-only-count (cohort/ledger prereg-path root))))))

(deftest grounded-change-needs-independent-resolved-witness
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        attempt (:attempt/id (open! root "clock/grounded"))]
    (append-required! root attempt)
    (is (thrown? clojure.lang.ExceptionInfo
                 (cohort/close-attempt!
                  prereg-path root attempt
                  (term {:outcome :grounded-change :grounded? true
                         :artifact-only? false :witness {:resolved? true}}))))
    (cohort/close-attempt!
     prereg-path root attempt
     (term {:outcome :grounded-change :grounded? true :artifact-only? false
            :duration-ms 2000 :resource-use {:agent-turns 1}
            :witness {:before "substrate-before.edn"
                      :after "substrate-after.edn"
                      :resolved? true
                      :dial-moved? true}}))
    (is (= 1 (:grounded-change-count (cohort/ledger prereg-path root))))))

(deftest cancelled-attempt-is-recorded-outside-preregistered-denominator
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        cancelled-id (:attempt/id (open! root "clock/cancelled"))]
    (append-required! root cancelled-id)
    (cohort/close-attempt!
     prereg-path root cancelled-id
     (term {:outcome :cancelled :grounded? false :artifact-only? false
            :duration-ms 20 :resource-use {:agent-turns 0}}))
    (let [ledger (cohort/ledger prereg-path root)
          recorded-count (:recorded-attempt-count ledger)
          cancelled-stratum (first (:semantic-strata ledger))]
      (is (= 0 (:attempt-count ledger))
          "cancelled does not dilute the preregistered denominator")
      (is (= 40 (:remaining ledger)))
      (is (= 1 recorded-count)
          "the immutable attempt dossier remains recorded")
      (is (= :post-preregistration/cancelled (:stratum/id cancelled-stratum)))
      (is (= [cancelled-id]
             (mapv :attempt/id (:attempts cancelled-stratum))))
      (is (= 2 (:attempt/ordinal (open! root "clock/after-cancellation")))
          "the next cohort attempt is admitted but retains a unique ordinal")
      (is (= 1 (:attempt-count (cohort/ledger prereg-path root)))))))

;; PROOF-wm-works ⟨1⟩1 ⟨2⟩4: attempt lifecycle bad cases, against the real
;; cohort code and the real preregistration (no stubs).

(deftest append-to-a-closed-attempt-is-rejected
  ;; (b): a closed attempt is final. Appending any further checkpoint --
  ;; including a second close -- must throw, and the attempt's event files
  ;; must be unchanged afterwards.
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        attempt (:attempt/id (open! root "clock/closed-final"))
        _ (append-required! root attempt)
        close (term {:outcome :agent-unavailable :grounded? false
                     :artifact-only? false :duration-ms 1
                     :resource-use {:agent-turns 0}})
        _ (cohort/close-attempt! prereg-path root attempt close)
        events-before (count (.listFiles (io/file root
                                                   (name (:cohort/id (cohort/read-edn prereg-path)))
                                                   attempt)))]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"already closed"
                          (cohort/append-checkpoint!
                           prereg-path root attempt :build
                           {:sorry {:kind :test-build}})))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"already closed"
                          (cohort/close-attempt! prereg-path root attempt close)))
    (is (= events-before
           (count (.listFiles (io/file root
                                       (name (:cohort/id (cohort/read-edn prereg-path)))
                                       attempt))))
        "a rejected append leaves the closed dossier byte-identical in file count")))

(deftest resume-appends-under-the-same-open-attempt-id
  ;; (a): an interrupted attempt that is still open (no 007-closed.edn)
  ;; resumes under the SAME attempt id: further checkpoints append with
  ;; increasing sequence numbers.
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        attempt (:attempt/id (open! root "clock/resume-me"))
        _ (append-required! root attempt) ; interrupted here: no close
        resumed (cohort/append-checkpoint!
                 prereg-path root attempt :adjudication
                 {:sorry {:kind :test-resumed-adjudication}})]
    (is (= attempt (:attempt/id resumed))
        "resumption keeps the attempt id")
    (is (= 7 (:event/sequence resumed))
        "the resumed checkpoint continues the sequence")))

(deftest retry-after-an-abandoned-attempt-uses-a-new-id
  ;; (c)+(d): a retry starts a NEW attempt id; the abandoned attempt keeps
  ;; its events (never rewritten) and is never counted as accepted work
  ;; (no close, no outcome in the ledger).
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        abandoned (:attempt/id (open! root "clock/abandoned"))
        _ (append-required! root abandoned) ; produced work, then interrupted
        events-before (sort (.listFiles (io/file root
                                                  (name (:cohort/id (cohort/read-edn prereg-path)))
                                                  abandoned)))
        retry (:attempt/id (open! root "clock/retry"))]
    (is (not= abandoned retry) "the retry gets a new attempt id")
    ;; The abandoned attempt's dossier is untouched by the retry:
    (is (= events-before
           (sort (.listFiles (io/file root
                                      (name (:cohort/id (cohort/read-edn prereg-path)))
                                      abandoned)))))
    ;; Abandoned work is not accepted work: no close event, no outcome.
    (let [events (cohort/attempt-events
                  (io/file root (name (:cohort/id (cohort/read-edn prereg-path))) abandoned))]
      (is (not (some #(= :closed (:checkpoint/type %)) events))
          "the abandoned attempt has no close")
      (is (= nil (:outcome (cohort/attempt-summary
                            (io/file root (name (:cohort/id (cohort/read-edn prereg-path)))
                                     abandoned))))
          "no outcome is attributed to the abandoned attempt"))))

(deftest fresh-ledger-renders-without-legacy-rows
  (let [root (tmp-root)
        value (cohort/ledger prereg-path root)
        html (cohort/render-html value)]
    (is (= 0 (:attempt-count value)))
    (is (= 40 (:remaining value)))
    (is (.contains html "No attempts recorded"))
    (is (.contains html "Historical artifact-only ticks are excluded"))))

(deftest attempt-summary-carries-fresh-repo-observation
  (let [attempt-dir (.toFile (Files/createTempDirectory
                              "cohort-attempt-summary"
                              (make-array FileAttribute 0)))
        event (fn [sequence checkpoint payload]
                {:attempt/id "attempt-016" :attempt/ordinal 16
                 :event/sequence sequence :checkpoint/type checkpoint
                 :payload payload})]
    (spit (io/file attempt-dir "001-time-step.edn")
          (pr-str (event 1 :time-step
                         {:judgment {:opportunity-id "clock/16"}})))
    (spit (io/file attempt-dir "002-selection.edn")
          (pr-str (event 2 :selection
                         {:judgment {:selected-mission "repair-attempt-010"}})))
    (spit (io/file attempt-dir "003-build.edn")
          (pr-str (event 3 :build
                         {:judgment
                          {:validation
                           {:artifact-binding
                            {:fresh-author? true
                             :pre-dispatch-head "2b912f0"
                             :commit "bf14dca"}}}})))
    (let [summary (cohort/attempt-summary attempt-dir)]
      (is (= "bf14dca" (:fresh-commit summary)))
      (is (= "2b912f0"
             (get-in summary [:artifact-binding :pre-dispatch-head]))))))

(deftest ineligible-trigger-is-refused-and-typed-as-a-precondition
  ;; repair-initialization-1f894133: a run fired with :instrumented-campaign-repair,
  ;; which the cohort never preregistered. The refusal is correct — admitting an
  ;; unlisted trigger would contaminate the sample the cohort is running — but
  ;; untyped it reached the runner's outer boundary as :initialization-failed and
  ;; was charged :machine-failure, demanding a repair commit and an independent
  ;; review for a caller-side config condition.
  (let [root (tmp-root)]
    (cohort/activate! prereg-path root)
    (testing "the guard still REFUSES: no unpreregistered trigger is admitted"
      (let [thrown (try (cohort/start-attempt!
                         prereg-path root
                         (term {:opportunity-id "ineligible-1"
                                :trigger :instrumented-campaign-repair
                                :machine-state {:tick 1}
                                :agent-roster []
                                :code-state {:git-sha "abc"
                                             :git-dirty? false
                                             :resolved-mode-flags {}
                                             :configuration-digest "test"}
                                :semantic-epoch :epoch-1}))
                        nil
                        (catch clojure.lang.ExceptionInfo e e))]
        (is (some? thrown) "an unlisted trigger must not open an attempt")
        (is (= "ineligible trigger" (ex-message thrown)))
        (testing "and it carries the typing that keeps it off the machine-failure line"
          (is (= :trigger-ineligible (:failure-kind (ex-data thrown))))
          (is (= :incomplete (:outcome (ex-data thrown)))))
        (testing "and names the precondition rather than leaving it to be guessed"
          (is (= :instrumented-campaign-repair (:trigger (ex-data thrown))))
          (is (contains? (:allowed (ex-data thrown)) :wallclock-cron)
              "the eligible set travels with the refusal"))))
    (testing "an eligible trigger still opens normally"
      (is (some? (open! root "eligible-1"))))))

(deftest explicit-cohort-pin-and-capacity
  (let [root (tmp-root)
        path (str root "/cohort.edn")
        p (-> (edn/read-string (slurp prereg-path))
              (assoc :cohort/id :run4-test)
              (assoc-in [:stopping-rule :target] 1))
        raw (pr-str p)
        digest (apply str (map #(format "%02x" (bit-and 255 %))
                              (.digest (java.security.MessageDigest/getInstance "SHA-256")
                                       (.getBytes raw "UTF-8"))))
        binding {:preregistration path :data-root root :cohort-id :run4-test :sha256 digest}]
    (spit path raw)
    (is (thrown? clojure.lang.ExceptionInfo (cohort/execution-preflight binding)))
    (cohort/activate! path root)
    (let [{:keys [snapshot remaining]} (cohort/execution-preflight binding)
          cell (term {:opportunity-id "run4/one" :trigger :wallclock-cron
                      :machine-state {} :agent-roster []
                      :code-state {:git-sha "test" :git-dirty? false
                                   :resolved-mode-flags {} :configuration-digest "test"}
                      :semantic-epoch :test})
          event (cohort/start-attempt! snapshot root cell)
          attempt (:attempt/id event)]
      (is (= 1 remaining))
      (is (= :run4-test (:cohort/id event)))
      (is (thrown? clojure.lang.ExceptionInfo (cohort/execution-preflight binding)))
      (is (= 0 (:remaining (cohort/execution-preflight binding false))))
      (doseq [checkpoint [:selection :construction :dispatch :build :adjudication]]
        (cohort/append-checkpoint! snapshot root attempt checkpoint {:sorry {:kind :test}}))
      (cohort/close-attempt! snapshot root attempt
                             (term {:outcome :agent-unavailable :grounded? false
                                    :artifact-only? false :duration-ms 1 :resource-use {}}))
      (is (= 1 (:closed-count (cohort/ledger snapshot root))))
      ;; Past the target the attempt runs and is recorded, outside the
      ;; preregistered window. The stopping rule still fixes what the cohort
      ;; MEASURES; it no longer decides whether the machine may work at all.
      (let [beyond (cohort/start-attempt!
                    snapshot root
                    (assoc-in cell [:judgment :opportunity-id] "run4/two"))
            after (cohort/ledger snapshot root)
            stratum (first (filter #(= :post-preregistration/beyond-window
                                       (:stratum/id %))
                                   (:semantic-strata after)))]
        (is (true? (:cohort/beyond-window? beyond)))
        (is (= {:target 1 :attempted 1} (:cohort/window beyond)))
        (is (= 1 (:attempt-count after))
            "a closed cohort's denominator cannot be moved by a later run")
        (is (= 0 (:remaining after)))
        (is (= 2 (:recorded-attempt-count after))
            "but the attempt keeps its immutable dossier")
        (is (= 1 (:attempt-count stratum))
            "and is reported in its own stratum, never pooled")))
    (is (thrown? clojure.lang.ExceptionInfo
                 (cohort/execution-preflight (assoc binding :cohort-id :foreign) false)))
    (spit path (str raw "\n"))
    (is (thrown? clojure.lang.ExceptionInfo (cohort/execution-preflight binding false))))
)

(deftest invalid-cell-refusals-carry-failure-kind
  ;; repair-ea1-3f4cac...-untyped-failure: the 2026-09-13 construction
  ;; failure refused here without :failure-kind and was classified
  ;; :untyped-failure. Same refusal, now typed; contract unchanged.
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        attempt (:attempt/id (open! root "clock/typed-invalid-cell"))
        data (fn [f]
               (try (f) nil
                    (catch clojure.lang.ExceptionInfo e (ex-data e))))]
    (is (= :invalid-checkpoint-cell
           (:failure-kind
            (data #(cohort/append-checkpoint!
                    prereg-path root attempt :construction
                    {:judgment {:not :a-valid-construction} :ground {:k 1}})))))
    (is (= :invalid-close-cell
           (:failure-kind
            (data #(cohort/close-attempt! prereg-path root attempt
                                          {:not-a-term true})))))
    (is (= :required-checkpoints-missing
           (:failure-kind
            (data #(cohort/close-attempt!
                    prereg-path root attempt
                    (term {:outcome :agent-unavailable :grounded? false
                           :artifact-only? false :duration-ms 1
                           :resource-use {:agent-turns 0}}))))))))

(deftest cohort-succession-carries-everything-but-identity
  ;; Eight cohorts (50..57) were minted by hand, and 56 vs 57 differ in
  ;; :cohort/id and narrative prose only -- every machine-read field is
  ;; byte-identical. That is the bean-count this removes, and it is also the
  ;; safety argument: the fields that could be tuned after seeing results are
  ;; exactly the ones carried over verbatim.
  (let [parent {:cohort/id :wm-contract-machinery-57-v1
                :protocol/version 4
                :status :preregistered
                :registered-on "2026-09-14"
                :purpose "machinery test"
                :casting {:author "codex-23" :reviewer "codex-22"
                          :repair-reviewer "codex-24"}
                :stopping-rule {:unit :trigger-opportunity :target 2
                                :counts-as-attempt [:build-failed :grounded-change]
                                :replacement "None."}
                :population "reviewed on-demand opportunities"}
        raw (pr-str parent)
        succ (cohort/successor-preregistration parent raw "2026-09-19")]

    (testing "identity advances"
      (is (= :wm-contract-machinery-58-v1 (:cohort/id succ)))
      (is (= "2026-09-19" (:registered-on succ)))
      (is (= :preregistered (:status succ))))

    (testing "provenance names the exact parent bytes"
      (is (= :wm-contract-machinery-57-v1 (:succeeds succ)))
      (is (re-matches #"[0-9a-f]{64}" (:succeeds-sha256 succ))))

    (testing "every tunable field is carried over verbatim"
      (doseq [k [:stopping-rule :casting :population :protocol/version :purpose]]
        (is (= (get parent k) (get succ k)) (str k " must not change on succession"))))

    (testing "a cohort id with no number cannot be succeeded"
      (is (nil? (cohort/successor-id :no-number-here)))
      (is (thrown? clojure.lang.ExceptionInfo
                   (cohort/successor-preregistration
                    (assoc parent :cohort/id :no-number-here) raw "2026-09-19"))))))

(deftest cohort-succession-fires-only-when-the-window-is-exhausted
  (let [root (tmp-root)
        lab (io/file root "M-aif-full-loop-90")
        _ (.mkdirs lab)
        path (.getPath (io/file lab "cohort.edn"))
        data-root (.getPath (io/file root "wm-full-loop-machinery-90"))
        prereg (assoc (read-string (slurp "holes/labs/M-aif-full-loop-57/cohort.edn"))
                      :cohort/id :wm-contract-machinery-90-v1)
        _ (spit path (with-out-str (pp/pprint (assoc-in prereg [:stopping-rule :target] 1))))
        _ (.mkdirs (io/file data-root))
        _ (cohort/activate! path data-root)
        cell (term {:opportunity-id "succ/one" :trigger :duree-click-on-demand
                    :machine-state {} :agent-roster []
                    :code-state {:git-sha "t" :git-dirty? false
                                 :resolved-mode-flags {} :configuration-digest "t"}
                    :semantic-epoch :test})]

    (testing "an unexhausted cohort does not mint a successor"
      (is (nil? (cohort/succeed! path data-root)))
      (is (not (.exists (io/file root "M-aif-full-loop-91")))))

    (cohort/start-attempt! (cohort/pin-preregistration
                            {:preregistration path :data-root data-root
                             :cohort-id :wm-contract-machinery-90-v1
                             :sha256 (@#'cohort/sha256 (slurp path))})
                           data-root cell)

    (testing "an exhausted one mints and activates its successor"
      (let [b (cohort/succeed! path data-root (constantly "2026-09-19"))]
        (is (= :wm-contract-machinery-91-v1 (:cohort-id b)))
        (is (.exists (io/file (:preregistration b))))
        (is (.exists (io/file (:data-root b))))
        (is (= 1 (:remaining (cohort/ledger (:preregistration b) (:data-root b))))
            "the successor starts with its parent's full target available")))

    (testing "calling again is idempotent, not a conflict"
      (is (= (:cohort-id (cohort/succeed! path data-root (constantly "2026-09-19")))
             :wm-contract-machinery-91-v1)))))

(deftest lineage-admits-only-a-byte-derivable-successor
  ;; This is what replaces a person re-pinning the server-owned binding. The
  ;; binding file and the source constant pinning its digest are untouched; a
  ;; successor is followed only when it is a pure function of the bytes they
  ;; pin. So the forged cases below must be refused, or the guarantee that
  ;; request data cannot select a cohort would be gone.
  (let [parent {:cohort/id :wm-contract-machinery-90-v1
                :protocol/version 4
                :status :preregistered
                :registered-on "2026-09-14"
                :stopping-rule {:unit :trigger-opportunity :target 2}
                :casting {:author "codex-23" :reviewer "codex-22"}}
        raw (pr-str parent)
        good (cohort/successor-preregistration parent raw "2026-09-19")]

    (testing "the derived successor is admitted"
      (is (cohort/verified-successor? parent raw good)))

    (testing "a tampered stopping rule is refused"
      (is (not (cohort/verified-successor?
                parent raw (assoc-in good [:stopping-rule :target] 99)))))

    (testing "tampered casting is refused"
      (is (not (cohort/verified-successor?
                parent raw (assoc-in good [:casting :author] "someone-else")))))

    (testing "an id that is not exactly successor-id is refused"
      (is (not (cohort/verified-successor?
                parent raw (assoc good :cohort/id :wm-contract-machinery-99-v1)))))

    (testing "a successor claiming the wrong parent bytes is refused"
      (is (not (cohort/verified-successor?
                parent raw (assoc good :succeeds-sha256 (apply str (repeat 64 "0"))))))
      (is (not (cohort/verified-successor?
                parent raw (assoc good :succeeds :some-other-cohort-v1)))))

    (testing "a successor that adds a field of its own is refused"
      (is (not (cohort/verified-successor?
                parent raw (assoc good :extra-privilege true)))))))

(deftest resolve-lineage-advances-past-an-exhausted-cohort
  (let [root (tmp-root)
        lab (io/file root "M-aif-full-loop-95")
        _ (.mkdirs lab)
        path (.getPath (io/file lab "cohort.edn"))
        data-root (.getPath (io/file root "wm-full-loop-machinery-95"))
        prereg (-> (read-string (slurp "holes/labs/M-aif-full-loop-57/cohort.edn"))
                   (assoc :cohort/id :wm-contract-machinery-95-v1)
                   (assoc-in [:stopping-rule :target] 1))
        _ (spit path (with-out-str (pp/pprint prereg)))
        _ (.mkdirs (io/file data-root))
        _ (cohort/activate! path data-root)
        binding {:preregistration path :data-root data-root
                 :cohort-id :wm-contract-machinery-95-v1
                 :sha256 (@#'cohort/sha256 (slurp path))}
        cell (term {:opportunity-id "lin/one" :trigger :duree-click-on-demand
                    :machine-state {} :agent-roster []
                    :code-state {:git-sha "t" :git-dirty? false
                                 :resolved-mode-flags {} :configuration-digest "t"}
                    :semantic-epoch :test})]

    (testing "while the pinned cohort has capacity, the binding is returned unchanged"
      (is (= binding (cohort/resolve-lineage! binding))))

    (cohort/start-attempt! (cohort/pin-preregistration binding) data-root cell)

    (testing "once exhausted it advances, minting the successor"
      (let [b (cohort/resolve-lineage! binding)]
        (is (= :wm-contract-machinery-96-v1 (:cohort-id b)))
        (is (pos? (:remaining (cohort/ledger (:preregistration b) (:data-root b)))))
        ;; The parent's target was overridden to 1 above, and the successor
        ;; inherits 1 -- from its actual parent, not from the charter this
        ;; fixture was copied from.
        (is (= 1 (:target (cohort/ledger (:preregistration b) (:data-root b)))))))

    (testing "a forged successor on disk is refused, not followed"
      (let [b (cohort/resolve-lineage! binding)
            forged (assoc (read-string (slurp (:preregistration b)))
                          :stopping-rule {:unit :trigger-opportunity :target 999})]
        (spit (:preregistration b) (with-out-str (pp/pprint forged)))
        (is (= :cohort-successor-not-derivable
               (:reason (ex-data (try (cohort/resolve-lineage! binding)
                                      nil
                                      (catch clojure.lang.ExceptionInfo e e))))))))))


(def retained-dispatch-path
  "holes/labs/wm-contract/runs/selection-fixture-migration-2026-09-20/baseline-non-readable-dispatch.edn")

(defn retained-runtime-event []
  ;; The old file cannot be read by the production EDN reader. Reconstitute its
  ;; captured runtime layout with actual functions/atoms at its #object sites,
  ;; solely to supply the original input shape to the REAL writer. Production
  ;; reads below have no custom readers.
  (let [raw (slurp retained-dispatch-path)]
    (is (= "850cb9737d9c22ad6d5b35e0e987c8fe27ba2e664a7bd127aaf90026292ec536"
           (#'cohort/sha256 raw)))
    (edn/read-string
     {:readers {'object (fn [[class-name _ description]]
                          (if (= 'clojure.lang.Atom class-name)
                            (atom description)
                            (fn [& _] nil)))}}
     raw)))

(deftest writer-replaces-retained-runtime-response-and-keeps-ledger-readable
  (let [event (retained-runtime-event)
        response (get-in event [:payload :ground :response])
        root (tmp-root)
        direct-path (io/file root "retained-event.edn")]
    (is (thrown? RuntimeException (edn/read-string (pr-str event)))
        "the reconstructed baseline input still exhibits the exact reader failure")
    (#'cohort/write-new! direct-path event)
    (let [saved (cohort/read-edn direct-path)
          placeholder (get-in saved [:payload :ground :response])]
      (is (= :non-edn-payload (:payload/status placeholder)))
      (is (= :not-round-trippable (:reason placeholder)))
      (is (<= (count (:summary placeholder)) 256))
      (is (= (dissoc event :payload) (dissoc saved :payload)))
      (is (= (get-in event [:payload :judgment]) (get-in saved [:payload :judgment]))))
    ;; Same input passes through the public checkpoint append and the real
    ;; ledger reader, then another checkpoint and close prove run continuity.
    (cohort/activate! prereg-path root)
    (let [attempt (:attempt/id (open! root "serialization/baseline"))]
      (doseq [checkpoint [:selection :construction]]
        (cohort/append-checkpoint! prereg-path root attempt checkpoint
                                   {:sorry {:kind :test}}))
      (cohort/append-checkpoint! prereg-path root attempt :dispatch
                                 (assoc-in (:payload event) [:ground :response] response))
      (doseq [checkpoint [:build :adjudication]]
        (cohort/append-checkpoint! prereg-path root attempt checkpoint
                                   {:sorry {:kind :test}}))
      (cohort/close-attempt! prereg-path root attempt
                             (term {:outcome :agent-unavailable :grounded? false
                                    :artifact-only? false :duration-ms 1
                                    :resource-use {:agent-turns 0}}))
      (is (= 1 (:closed-count (cohort/ledger prereg-path root)))))))

(defrecord UnreadableTaggedPayload [value])

(deftest writer-preserves-valid-bytes-and-marks-other-unreadable-values
  (let [root (tmp-root)
        valid (array-map :first [nil true false 1 1/3 2.5M :tag 'symbol]
                         :text "line one\nline two"
                         :nested {:set #{:a :b} :list '(1 2 3)})
        good-path (io/file root "good.edn")]
    (#'cohort/write-new! good-path valid)
    (is (= (seq (.getBytes (str (pr-str valid) "\n") "UTF-8"))
           (seq (Files/readAllBytes (.toPath good-path))))
        "valid output is byte-for-byte the prior writer output")
    (is (= valid (cohort/read-edn good-path)))
    (doseq [[name value] [["function" (fn [] nil)] ["atom" (atom {:x 1})]
                         ["object" (Object.)] ["bad-key" {(Object.) :value}]
                         ["tagged-record" (->UnreadableTaggedPayload :readable-field)]]]
      (let [path (io/file root (str name ".edn"))]
        (#'cohort/write-new! path value)
        (is (= :non-edn-payload (:payload/status (cohort/read-edn path))))))
    (let [path (io/file root "siblings.edn")]
      (#'cohort/write-new! path {:valid valid :runtime (atom "\u001b\nprivate")})
      (let [saved (cohort/read-edn path)]
        (is (= valid (:valid saved)))
        (is (= :non-edn-payload (get-in saved [:runtime :payload/status])))
        (is (not (re-find #"[\p{Cc}\p{Cf}]" (get-in saved [:runtime :summary]))))))))

(deftest unreadable-history-is-recorded-without-killing-initialization
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        first-event (open! root "history/poisoned")
        attempt (:attempt/id first-event)
        dir (io/file root (name (:cohort/id first-event)) attempt)
        poison (io/file dir "002-selection.edn")
        quarantine (io/file "/home/joe/code/futon2/data/wm-quarantine/machinery-67-attempt-001/002-selection.edn")
        original (slurp quarantine)
        binding {:preregistration prereg-path :data-root root
                 :cohort-id (:cohort/id first-event)
                 :sha256 (@#'cohort/sha256 (slurp prereg-path))}]
    (io/copy quarantine poison)
    (let [history (cohort/attempt-history dir)
          exclusion (first (:exclusions history))
          resolved (cohort/resolve-lineage! binding)
          next-event (open! root "history/next")
          state (cohort/ledger prereg-path root)
          next-path (io/file root (name (:cohort/id next-event))
                             (:attempt/id next-event) "001-time-step.edn")]
      (is (= [first-event] (:events history)) "readable control is consumed")
      (is (= 1 (count (:exclusions history))))
      (is (= :history-discovery-invalid (:history/refusal exclusion)))
      (is (= :unreadable-or-malformed-record (:reason exclusion)))
      (is (= attempt (:target exclusion)))
      (is (= (.getAbsolutePath poison) (:path exclusion)))
      (is (= [exclusion] (:history-exclusions (cohort/lineage-history resolved))))
      (is (= [exclusion] (:history-exclusions state)))
      (is (= 2 (:attempt-count state)) "damaged attempt still consumes its opportunity")
      (is (= [exclusion] (get-in next-event [:payload :judgment :history-exclusions])))
      (is (= next-event (cohort/read-edn next-path)) "persisted exclusion round-trips")
      (is (= exclusion (edn/read-string (pr-str exclusion)))))
    (is (thrown? RuntimeException (cohort/attempt-events dir))
        "strict append reader must not silently skip damaged active checkpoints")
    (is (= original (slurp quarantine)))
    (is (= original (slurp poison)))))

(deftest opportunity-identity-comes-only-from-the-authoritative-file
  (let [root (tmp-root)
        _ (cohort/activate! prereg-path root)
        event (open! root "identity/actual")
        dir (io/file root (name (:cohort/id event)))
        decoy (assoc-in event [:payload :judgment :opportunity-id] "identity/decoy")]
    (spit (io/file dir (:attempt/id event) "002-selection.edn") (pr-str decoy))
    (is (= #{"identity/actual"} (#'cohort/opened-opportunity-ids dir)))
    (is (= "attempt-002" (:attempt/id (open! root "identity/decoy"))))))
