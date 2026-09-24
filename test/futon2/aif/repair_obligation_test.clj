(ns futon2.aif.repair-obligation-test
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pp]
            [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.tripwire :as tripwire]))

(defn- temp-root []
  (let [f (java.io.File/createTempFile "wm-repair-" "")]
    (.delete f)
    (.mkdirs f)
    (.getPath f)))

(defn- dismissal-refusal [f]
  (try (f) nil
       (catch clojure.lang.ExceptionInfo e
         (:repair-dismissal/refusal (ex-data e)))))

(defn- write-record! [root child record]
  (let [file (io/file root child (str (:repair/id record) ".edn"))]
    (io/make-parents file)
    (spit file (pr-str record))
    record))

(deftest dismiss-grounding-readback-degraded-proof-controls
  (let [commit "a2d8aba0ae57126b22b9afa9c7ff0888a54afdaa"
        impl-id (str "full-loop/implementation/" commit)
        finding {:repair/id "finding-gnc" :repair/schema-version 3
                 :repair/class :machine-failure :repair/status :open
                 :attempt-id "attempt-002"
                 :failure-kind :grounded-no-change :failure-stage :grounding
                 :failure-outcome :grounded-no-change
                 :opened-at "2026-09-23T17:32:56Z"
                 :discharge-contract {:artifact-shape :code-commit :requires [:grounded-repair]}
                 :repair/occurrence {:occurrence/id "occ-x"
                                     :occurrence/origin "/repo::2026-09-23-1790184736"
                                     :occurrence/event-id "ea1-hash--attempt-002"}}
        close {:payload {:judgment {:witness {:implementation-id impl-id
                                              :resolved? false :dial-moved? true}}}}
        disposition {:authority "Joe 2026-09-24: repair stop-lines from outside"
                     :reason :grounding-readback-degraded :actor "kimi-6"}
        string-props (pr-str {:implementation/commit commit :implementation/files ["f"]})
        ;; The readers are injected through the PRIVATE impl var, not the
        ;; public route: the public one takes no such argument, so a caller
        ;; cannot hand this route its own proof. Driving the legs is a test
        ;; privilege, and reaching through #' is what says so.
        impl #'repair/dismiss-grounding-readback-degraded-impl!
        run (fn [root entity close-ret]
              (impl root "finding-gnc" disposition
                    {:close-read-fn (fn [_ _] close-ret)
                     :entity-by-id-fn (fn [_] entity)}))]
    (testing "the false finding dismisses: string props naming the same commit"
      (let [root (temp-root)]
        (write-record! root "findings" finding)
        (let [record (run root {:props string-props} close)]
          (is (= :dismissed-grounding-readback-degraded (:repair/status record)))
          (is (= :grounding-readback-degraded (:dismissal/kind record)))
          (is (= impl-id (get-in record [:evidence :implementation-id])))
          (is (= :already-dismissed
                 (dismissal-refusal
                  #(run root {:props string-props} close)))))))
    (testing "the public route exposes no seam to supply the proof through"
      ;; Every sibling route in repair_obligation.clj is (finding-id
      ;; disposition) / (root finding-id disposition) and finds its own
      ;; evidence. If this one ever grows a 4-arity again, a caller can
      ;; fabricate the close and the readback and dismiss an HONEST finding.
      (is (= #{2 3} (into #{} (map count)
                          (:arglists (meta #'repair/dismiss-grounding-readback-degraded!))))))
    (testing "THE REFUSAL CASE: a genuine grounded-no-change refuses"
      ;; The entity's props read back as a proper MAP: the dial really did
      ;; not move, the finding is honest, and this route must not clear it.
      (let [root (temp-root)]
        (write-record! root "findings" finding)
        (is (= :grounding-readback-not-degraded
               (dismissal-refusal #(run root {:props {:implementation/commit commit}} close))))
        (is (empty? (.listFiles (io/file root "dismissals")))
            "a refused route writes no dismissal")))
    (testing "each proof leg refuses on its own absence"
      (doseq [[label mutate-finding entity close-ret expected]
              [[:finding-shape #(assoc % :failure-outcome :incomplete) {:props string-props} close
                :finding-not-false-grounding]
               [:occurrence #(dissoc % :repair/occurrence) {:props string-props} close
                :occurrence-unavailable]
               [:close identity {:props string-props} nil :close-unavailable]
               [:witness identity {:props string-props}
                {:payload {:judgment {:witness {:implementation-id impl-id
                                                :resolved? true :dial-moved? true}}}}
                :witness-not-false-grounded]
               [:entity-missing identity nil close :grounding-readback-unavailable]
               [:commit-mismatch identity
                {:props (pr-str {:implementation/commit "deadbeefdeadbeef"})} close
                :readback-commit-mismatch]]]
        (let [root (temp-root)]
          (write-record! root "findings" (mutate-finding finding))
          (is (= expected (dismissal-refusal #(run root entity close-ret)))
              (str label))
          (is (not (.exists (io/file root "dismissals" "finding-gnc.edn")))))))))

(deftest dismiss-repaired-elsewhere-proof-controls
  (let [repo (temp-root)
        git (fn [& args]
              (let [r (apply shell/sh "git" "-C" repo args)]
                (when-not (zero? (:exit r)) (throw (ex-info "fixture git failed" r)))
                (str/trim (:out r))))]
    (git "init") (git "config" "user.email" "fixture@example.invalid")
    (git "config" "user.name" "fixture")
    ;; The fixing commit: its content carries the diagnosis anchor.
    (spit (io/file repo "schedule.clj")
          "(ns schedule)\n;; resolves :incommensurable-family by adopting the family schedule\n")
    (git "add" "schedule.clj")
    (git "-c" "commit.gpgsign=false" "commit" "--date=2026-09-20T21:43:00Z" "-m" "adopt the family schedule")
    ;; An unrelated commit that does not speak to the diagnosis.
    (spit (io/file repo "unrelated.txt") "nothing about schedules\n")
    (git "add" "unrelated.txt")
    (git "-c" "commit.gpgsign=false" "commit" "--date=2026-09-20T21:45:00Z" "-m" "unrelated")
    ;; A side-branch commit that never landed.
    (git "checkout" "-b" "side")
    (spit (io/file repo "side.clj") ";; :incommensurable-family\n")
    (git "add" "side.clj")
    (git "-c" "commit.gpgsign=false" "commit" "--date=2026-09-20T21:46:00Z" "-m" "side fix")
    (git "checkout" "master")
    (let [fix-sha (git "rev-parse" "HEAD~1")
          unrelated-sha (git "rev-parse" "HEAD")
          side-sha (git "rev-parse" "side")
          finding {:repair/id "finding-re" :repair/schema-version 3
                   :repair/class :machine-failure :repair/status :open
                   :attempt-id "attempt-002"
                   :failure-kind :untyped-failure :failure-stage :selection
                   :failure-outcome :incomplete
                   :opened-at "2026-09-20T20:52:19Z"
                   :failure-error "incompatible preference schedules"
                   :failure-data {:kind :incommensurable-family}
                   :machine-repo repo
                   :discharge-contract {:artifact-shape :code-commit :requires [:grounded-repair]}}
          disposition (fn [c] {:authority "Joe 2026-09-24 emacs-repl" :reason :repaired-elsewhere
                               :actor "kimi-6" :commit c})]
      (testing "the repaired condition dismisses, with evidence"
        (let [root (temp-root)]
          (write-record! root "findings" finding)
          (let [record (repair/dismiss-repaired-elsewhere! root "finding-re" (disposition fix-sha))]
            (is (= :dismissed-repaired-elsewhere (:repair/status record)))
            (is (= :repaired-elsewhere (:dismissal/kind record)))
            (is (= fix-sha (get-in record [:evidence :commit])))
            (is (= "incommensurable-family" (get-in record [:evidence :diagnosis-anchor]))))
          (is (= :already-dismissed
                 (dismissal-refusal
                  #(repair/dismiss-repaired-elsewhere! root "finding-re" (disposition fix-sha)))))))
      (testing "a commit that does not speak to the diagnosis refuses"
        (let [root (temp-root)]
          (write-record! root "findings" finding)
          (is (= :repair-not-evidenced
                 (dismissal-refusal
                  #(repair/dismiss-repaired-elsewhere! root "finding-re" (disposition unrelated-sha)))))
          (is (not (.exists (io/file root "dismissals" "finding-re.edn"))))))
      (testing "a finding with an implementation record refuses (route already engaged)"
        (let [root (temp-root)]
          (write-record! root "findings" finding)
          (write-record! root "implementations"
                         {:repair/id "finding-re" :repair/status :awaiting-validation
                          :implementation-attempt "other"})
          (is (= :finding-not-open
                 (dismissal-refusal
                  #(repair/dismiss-repaired-elsewhere! root "finding-re" (disposition fix-sha)))))
          (is (not (.exists (io/file root "dismissals" "finding-re.edn"))))))
      (testing "a commit that predates the finding refuses"
        (let [root (temp-root)]
          (write-record! root "findings" (assoc finding :opened-at "2026-09-21T00:00:00Z"))
          (is (= :commit-predates-finding
                 (dismissal-refusal
                  #(repair/dismiss-repaired-elsewhere! root "finding-re" (disposition fix-sha)))))))
      (testing "a commit that never landed refuses"
        (let [root (temp-root)]
          (write-record! root "findings" finding)
          (is (= :commit-not-ancestor
                 (dismissal-refusal
                  #(repair/dismiss-repaired-elsewhere! root "finding-re" (disposition side-sha)))))))
      (testing "not a commit at all refuses"
        (let [root (temp-root)]
          (write-record! root "findings" finding)
          (is (= :commit-invalid
                 (dismissal-refusal
                  #(repair/dismiss-repaired-elsewhere! root "finding-re" (disposition "deadbeef"))))))))))

(deftest public-dismissal-arglists-are-seam-free
  ;; d7c8f8b1: a public opts map lets a caller supply the proof. Pin the
  ;; public shape of every dismissal route so the seam cannot regrow.
  (doseq [v [#'repair/dismiss-grounding-readback-degraded!
             #'repair/dismiss-repaired-elsewhere!]]
    (is (= #{2 3} (into #{} (map count) (:arglists (meta v)))))))

(deftest dismiss-superseded-attempt-retained-proof-controls
  (doseq [[label finding-extra record expected]
          [[:later {} {:implementation-attempt "other-attempt"
                       :implemented-at "2026-09-19T12:00:01Z"} nil]
           [:live {} nil :target-not-implemented]
           [:self {} {:implementation-attempt "failed-attempt"
                      :implemented-at "2026-09-19T12:00:01Z"} :self-implementation]
           [:alias {:attempt-id "cohort--attempt-001"}
            {:implementation-attempt "attempt-001"
             :implemented-at "2026-09-19T12:00:01Z"} :self-implementation]
           [:earlier {} {:implementation-attempt "other-attempt"
                         :implemented-at "2026-09-19T11:00:00Z"}
            :implementation-predates-attempt]
           [:equal {} {:implementation-attempt "other-attempt"
                       :implemented-at "2026-09-19T12:00:00Z"}
            :implementation-predates-attempt]
           [:bad-time {} {:implementation-attempt "other-attempt"
                          :implemented-at "not-a-time"} :implementation-predates-attempt]
           [:missing-id {} {:implemented-at "2026-09-19T12:00:01Z"}
            :target-not-implemented]
           [:conflict {:selected-entry {:action {:target "ancestor"}}}
            nil :target-not-resolvable]
           [:deep-only {:target nil :selected-entry
                        {:action {:target "target" :repair-obligation
                                  {:repair/id "target" :target "ancestor"}}}}
            {:implementation-attempt "other-attempt"
             :implemented-at "2026-09-19T12:00:01Z"} nil]
           [:absent {:target nil} nil :target-not-resolvable]]]
    (testing (name label)
      (let [root (temp-root)
            finding (merge {:repair/id "finding" :repair/status :open
                            :attempt-id "failed-attempt" :target "target"
                            :opened-at "2026-09-19T12:00:00Z"} finding-extra)
            disposition {:authority "test" :actor "codex-1" :reason :superseded}
            path (io/file root "findings" "finding.edn")]
        (write-record! root "findings" finding)
        (write-record! root "findings" {:repair/id "target" :repair/status :open})
        (when record
          (write-record! root "implementations"
                         (merge {:repair/id "target" :repair/status :awaiting-validation}
                                record)))
        (let [before (slurp path)]
          (is (= :disposition-invalid
                 (dismissal-refusal
                  #(repair/dismiss-superseded-attempt!
                    root "finding" (assoc disposition :target "forged")))))
          (is (= expected (dismissal-refusal
                           #(repair/dismiss-superseded-attempt! root "finding" disposition))))
          (is (= before (slurp path)))
          (if expected
            (do (is (not (.exists (io/file root "dismissals" "finding.edn"))))
                (is (some #(= "finding" (:repair/id %)) (repair/open-obligations root))))
            (let [history (first (repair/obligation-history root (:attempt-id finding)))
                  dismissal (:repair/dismissal history)]
              (is (= :dismissed-superseded-attempt (:repair/status history)))
              (is (= "target" (:target-id dismissal)))
              (is (= "other-attempt" (:implementation-attempt dismissal)))
              (is (.isFile (io/file (:implementation-record-path dismissal))))
              ;; T8 consumes this directory-based open queue; no status rewrite.
              (is (not-any? #(= "finding" (:repair/id %)) (repair/open-obligations root)))
              (is (= :already-dismissed
                     (dismissal-refusal
                      #(repair/dismiss-superseded-attempt! root "finding" disposition)))))))))))

(deftest dismiss-superseded-attempt-resolution-proof
  (let [root (temp-root)
        disposition {:authority "test" :actor "codex-1" :reason :superseded}]
    (write-record! root "findings" {:repair/id "finding" :repair/status :open
                                   :attempt-id "failed" :target "target"
                                   :opened-at "2026-09-19T12:00:00Z"})
    (write-record! root "findings" {:repair/id "target" :repair/status :open})
    (write-record! root "resolutions" {:repair/id "target" :repair/status :superseded
                                      :validation-attempt "successor"
                                      :resolved-at "2026-09-19T12:00:01Z"})
    (is (= :target-not-implemented
           (dismissal-refusal #(repair/dismiss-superseded-attempt! root "finding" disposition))))
    (write-record! root "resolutions" {:repair/id "target" :repair/status :resolved
                                      :validation-attempt "successor"
                                      :resolved-at "2026-09-19T12:00:01Z"})
    (is (= :dismissed-superseded-attempt
           (:repair/status (repair/dismiss-superseded-attempt! root "finding" disposition))))))

(deftest superseded-attempt-dismissal-closes-t8-sources
  (let [root (temp-root)
        now (java.time.Instant/now)
        observation #(let [context {:repair-root root :cohort? true
                                    :tripwire/cohort-history [] :tripwire/a-matrix-events []
                                    :tripwire/grounding-witnesses []}]
                       (#'tripwire/cross-run-observation
                        context {:phase :opportunity :transition :start}))]
    (write-record! root "findings" {:repair/id "target" :repair/status :open})
    (write-record! root "implementations"
                   {:repair/id "target" :repair/status :awaiting-validation
                    :implementation-attempt "later" :implemented-at (str (.plusSeconds now 1))})
    (doseq [id ["a" "b" "c"]]
      (write-record! root "findings"
                     {:repair/id id :repair/status :open :repair/class :machine-failure
                      :failure-kind :build-failed :target "target" :attempt-id id
                      :opened-at (str now)}))
    (is (= 1 (count (tripwire/evaluate-wire :T8 (observation)))))
    (doseq [id ["a" "b" "c"]]
      (repair/dismiss-superseded-attempt!
       root id {:authority "test" :actor "codex-1" :reason :superseded}))
    (is (empty? (tripwire/evaluate-wire :T8 (observation))))
    (is (= #{"a" "b" "c"} (:closed-repair-ids (observation))))))

(defn- dispatch-finding! [root attempt-id execution]
  (repair/record-system-failure!
   root {:attempt-id attempt-id
         :repair-class :machine-failure
         :failure-stage :author-wait
         :outcome :build-failed
         :failure-kind :build-failed
         :error "Author job did not complete"
         :failure-data {:author-job {:job-id (str "invoke-" attempt-id)
                                     :state "failed"
                                     :execution execution}}
         :discharge-contract {:requires [:distinct-repair-commit]
                              :artifact-shape :code-commit}}))

(deftest dismiss-unexecuted-is-append-only-and-fail-closed
  (let [root (temp-root)
        executed (dispatch-finding! root "executed"
                                    {:executed true :tool-events 1
                                     :command-events 0})
        absent (dispatch-finding! root "ambiguous" {})
        finding (dispatch-finding! root "never-executed"
                                   {:executed false :tool-events 0
                                    :command-events 0})
        finding-file (io/file root "findings" (str (:repair/id finding) ".edn"))
        before (java.nio.file.Files/readAllBytes (.toPath finding-file))
        disposition {:authority "Joe/repair-queue/2026-09-19"
                     :reason :never-executed-dispatch
                     :cause-fix "futon3c@7829ea83"
                     :actor "claude-12"}]
    (is (= :finding-executed
           (dismissal-refusal
            #(repair/dismiss-unexecuted! root (:repair/id executed) disposition))))
    (is (= :execution-not-retained
           (dismissal-refusal
            #(repair/dismiss-unexecuted! root (:repair/id absent) disposition))))
    (is (= :dismissed-unexecuted
           (:repair/status
            (repair/dismiss-unexecuted! root (:repair/id finding) disposition))))
    (is (= #{(:repair/id executed) (:repair/id absent)}
           (->> (repair/open-obligations root)
                (filter #(and (= :open (:repair/status %))
                              (not= :environmental-hold (:repair/class %))))
                (map :repair/id)
                set)))
    (let [readback (first (repair/obligation-history root "never-executed"))]
      (is (= :dismissed-unexecuted (:repair/status readback)))
      (is (= disposition
             (select-keys (:repair/dismissal readback)
                          [:authority :reason :cause-fix :actor]))))
    (is (java.util.Arrays/equals
         before (java.nio.file.Files/readAllBytes (.toPath finding-file))))
    (is (= :already-dismissed
           (dismissal-refusal
            #(repair/dismiss-unexecuted! root (:repair/id finding) disposition))))))

(defn- echo-finding! [root attempt-id source-ids]
  (repair/record-system-failure!
   root {:attempt-id attempt-id
         :repair-class :machine-failure
         :failure-stage :initialization
         :outcome :incomplete
         :failure-kind :tripwire-tripped
         :error "T8 echo"
         :failure-data {:tripwire/wire-id :T8
                        :tripwire/witness
                        {:kind :duplicate-finding-livelock
                         :repair-ids source-ids}}
         :discharge-contract {:requires [:distinct-repair-commit]
                              :artifact-shape :code-commit}}))

(deftest dismiss-echo-requires-retained-witness-and-disposed-sources
  (let [root (temp-root)
        source-a (dispatch-finding! root "source-a"
                                    {:executed false :tool-events 0
                                     :command-events 0})
        source-b (dispatch-finding! root "source-b"
                                    {:executed false :tool-events 0
                                     :command-events 0})
        echo (echo-finding! root "echo" [(:repair/id source-a)
                                          (:repair/id source-b)])
        no-witness (dispatch-finding! root "executed-repair"
                                      {:executed true :tool-events 1
                                       :command-events 1})
        disposition {:authority "Joe/repair-queue/2026-09-19"
                     :reason :disposed-source-echo
                     :cause-note "T8 retained both source ids"
                     :actor "claude-12"}
        finding-file (io/file root "findings" (str (:repair/id echo) ".edn"))
        before (java.nio.file.Files/readAllBytes (.toPath finding-file))]
    (repair/dismiss-unexecuted!
     root (:repair/id source-a)
     {:authority "Joe/repair-queue/2026-09-19"
      :reason :never-executed-dispatch :actor "claude-12"})
    (let [data (try (repair/dismiss-echo! root (:repair/id echo) disposition)
                    nil
                    (catch clojure.lang.ExceptionInfo e (ex-data e)))]
      (is (= :sources-not-disposed (:repair-dismissal/refusal data)))
      (is (= [(:repair/id source-b)] (:live-source-ids data))))
    (is (= :no-witness-retained
           (dismissal-refusal
            #(repair/dismiss-echo! root (:repair/id no-witness) disposition))))
    (repair/dismiss-unexecuted!
     root (:repair/id source-b)
     {:authority "Joe/repair-queue/2026-09-19"
      :reason :never-executed-dispatch :actor "claude-12"})
    (let [dismissal (repair/dismiss-echo! root (:repair/id echo) disposition)]
      (is (= :dismissed-echo (:repair/status dismissal)))
      (is (= [{:repair/id (:repair/id source-a)
               :status-at-dismissal :dismissed-unexecuted}
              {:repair/id (:repair/id source-b)
               :status-at-dismissal :dismissed-unexecuted}]
             (:witness-sources dismissal))))
    (is (not-any? #{(:repair/id echo)}
                  (map :repair/id (repair/open-obligations root))))
    (is (= :dismissed-echo
           (:repair/status (first (repair/obligation-history root "echo")))))
    (is (java.util.Arrays/equals
         before (java.nio.file.Files/readAllBytes (.toPath finding-file))))
    (is (= :already-dismissed
           (dismissal-refusal
            #(repair/dismiss-echo! root (:repair/id echo) disposition))))))

(deftest dismiss-fixture-pollution-requires-retained-nonproduction-resolution
  (let [root (temp-root)
        finding (write-record! root "findings"
                               {:repair/id "fixture-leak"
                                :repair/status :open
                                :attempt-id "fixture-attempt"
                                :machine-repo "/futon2"
                                :failure-data
                                {:artifact-binding {:repo "/repo"}
                                 :resolved-repository "/tmp/debug-fixture"}})
        production (write-record! root "findings"
                                  {:repair/id "production-mismatch"
                                   :repair/status :open
                                   :attempt-id "production-attempt"
                                   :machine-repo "/home/joe/code/futon2"
                                   :failure-data
                                   {:artifact-binding
                                    {:repo "/home/joe/code/futon2"}}})
        ambiguous (write-record! root "findings"
                                 {:repair/id "ambiguous-resolution"
                                  :repair/status :open
                                  :attempt-id "ambiguous-attempt"
                                  :machine-repo "/futon2"
                                  :failure-data {:artifact-binding
                                                 {:repo "/repo"}}})
        disposition {:authority "Joe/repair-queue/2026-09-19"
                     :reason :fixture-pollution
                     :cause-fix "6cdb308a"
                     :actor "claude-12"}
        file (io/file root "findings" "fixture-leak.edn")
        before (java.nio.file.Files/readAllBytes (.toPath file))]
    (is (= :subject-is-production
           (dismissal-refusal
            #(repair/dismiss-fixture-pollution!
              root (:repair/id production) disposition))))
    (is (= :resolution-not-retained
           (dismissal-refusal
            #(repair/dismiss-fixture-pollution!
              root (:repair/id ambiguous) disposition))))
    (let [dismissal (repair/dismiss-fixture-pollution!
                     root (:repair/id finding) disposition)]
      (is (= :dismissed-fixture-pollution (:repair/status dismissal)))
      (is (= {:machine-repo "/futon2"
              :claimed-repository "/repo"
              :resolved-repository "/tmp/debug-fixture"}
             (:resolution-evidence dismissal))))
    (is (not-any? #{(:repair/id finding)}
                  (map :repair/id (repair/open-obligations root))))
    (is (= :dismissed-fixture-pollution
           (:repair/status (first (repair/obligation-history
                                   root "fixture-attempt")))))
    (is (java.util.Arrays/equals
         before (java.nio.file.Files/readAllBytes (.toPath file))))
    (is (= :already-dismissed
           (dismissal-refusal
            #(repair/dismiss-fixture-pollution!
              root (:repair/id finding) disposition))))))

(def grounded-review
  {:reviewer "reviewer" :review-job "review-job"
   :witness {:resolved? true :dial-moved? true}})

(defn- discharge-ctx
  "Minimal valid :wm/repair-discharge-context-v1 for the store writers.
  H-PUBLISH-A1 made the context mandatory: it must bind the phase, the
  obligation id, the record's attempt-id and review-job."
  [phase id attempt-id review-job]
  {:schema :wm/repair-discharge-context-v1
   :phase phase :repair/id id
   :close {:attempt/id attempt-id}
   :review-job {:job-id review-job}})

(defn- shaped-obligation [shape & [extra]]
  (merge {:repair/id (str "repair-" (name shape))
          :repair/status :open
          :repair/class :machine-failure
          :attempt-id "failed-attempt"
          :discharge-contract {:artifact-shape shape}}
         extra))

(def successor-sha (apply str (repeat 64 "a")))

(defn- successor-fixture []
  (let [repair-id "repair-successor-fixture"]
    {:discharge-context
     (discharge-ctx :successor-validation repair-id
                    "successor-attempt" "review-job-2")
     :obligation
     (shaped-obligation
      :code-commit
      {:repair/id repair-id
       :discharge-contract
       {:artifact-shape :code-commit
        :requires [:distinct-repair-commit :independent-review
                   :grounded-repair :distinct-production-shaped-successor]}
       :repair/implementation
       {:implementation-attempt "repair-attempt"
        :replacement-commit "abc1234"}})
     :repair-close
     {:attempt/id "repair-attempt" :run/id "repair-run"
      :repair/id repair-id :closed-at "2026-09-14T20:00:00Z"
      :commit "abc1234" :review-receipt-ids ["review-r.edn"]
      :review-sha256 successor-sha :grounded? true}
     :successor-close
     {:attempt/id "successor-attempt" :run/id "successor-run"
      :repair/id repair-id :closed-at "2026-09-14T21:00:00Z"
      :witness-ref "successor-witness.edn" :witness-sha256 successor-sha
      :grounded? true :production-shaped? true
      :witness {:resolved? true :dial-moved? true :implementation-id "impl-1"}}
     :authority {:decided-by "reviewer-2" :review-job "review-job-2"}}))

(defn- successor-refusal [inputs]
  (try (repair/successor-resolution! inputs) nil
       (catch clojure.lang.ExceptionInfo e
         (:repair-successor/refusal (ex-data e)))))

(deftest successor-discharge-retains-explicit-r-s-relation
  (let [root (temp-root)
        fixture (successor-fixture)
        result (repair/successor-resolution!
                (assoc fixture
                       :resolution-read-fn (constantly nil)
                       :resolve-fn (partial repair/resolve! root)))
        stored (edn/read-string
                (slurp (io/file root "resolutions"
                                "repair-successor-fixture.edn")))]
    (is (= :resolved (:status result)))
    (is (= (:relation result) (:successor-relation stored)))
    (is (= "repair-attempt"
           (get-in stored [:successor-relation :repair-attempt/id])))
    (is (= "successor-attempt"
           (get-in stored [:successor-relation :successor-attempt/id])))
    (is (= successor-sha
           (get-in stored [:successor-relation :successor-witness-sha256])))))

(deftest successor-discharge-refuses-missing-borrowed-self-and-projection
  (let [fixture (assoc (successor-fixture)
                       :resolution-read-fn (constantly nil)
                       :resolve-fn (fn [& _]
                                     (throw (AssertionError. "must not write"))))]
    (is (= :successor-missing
           (successor-refusal (assoc fixture :successor-close nil))))
    (is (= :successor-lineage-mismatch
           (successor-refusal
            (assoc-in fixture [:successor-close :repair/id] "borrowed-repair"))))
    (is (= :self-successor
           (successor-refusal
            (-> fixture
                (assoc-in [:successor-close :attempt/id] "repair-attempt")
                (assoc-in [:successor-close :run/id] "repair-run")))))
    (is (= :successor-evidence-invalid
           (successor-refusal
            (assoc fixture :successor-close
                   {:delivery-projection "not-a-successor-witness"}))))))

(deftest existing-resolution-is-stable-and-cutoff-readback-is-temporal
  (let [{:keys [obligation] :as fixture} (successor-fixture)
        existing {:repair/id (:repair/id obligation) :repair/status :resolved
                  :resolved-at "2026-09-14T21:30:00Z"}
        writes (atom 0)
        result (repair/successor-resolution!
                (assoc fixture
                       :resolution-read-fn (constantly existing)
                       :resolve-fn (fn [& _] (swap! writes inc))))
        before (repair/repair-derived-state
                (:repair/id obligation) "2026-09-14T21:00:00Z"
                obligation existing)
        after (repair/repair-derived-state
               (:repair/id obligation) "2026-09-14T22:00:00Z"
               obligation existing)]
    (is (= :already-resolved (:status result)))
    (is (zero? @writes))
    (is (= :open (:derived-status before)))
    (is (nil? (:resolution before)))
    (is (= :resolved (:derived-status after)))
    (is (= existing (:resolution after)))
    (is (= [:finding :resolution] (:derived-from after)))))

(deftest finding-remains-open-until-grounded-successor-resolution
  (let [root (temp-root)
        finding (repair/record-review-failure!
                 root {:attempt-id "failed-1"
                       :target :sorry/g2
                       :commit "bad123"
                       :selected-entry {:action {:type :address-sorry
                                                 :target :sorry/g2}}
                       :reviewer "codex-7"
                       :review-job "review-1"
                       :review-verdict :request-changes
                       :review-text "provenance gate is optional"})]
    (is (= [finding] (repair/open-obligations root)))
    (testing "tests or prose without a grounded witness cannot clear the line"
      (is (thrown? clojure.lang.ExceptionInfo
                   (repair/resolve! root finding
                                    {:attempt-id "repair-1" :commit "good456"
                                     :reviewer "codex-7" :review-job "review-2"
                                     :repair/discharge-context
                                     (discharge-ctx :successor-validation
                                                    (:repair/id finding)
                                                    "repair-1" "review-2")
                                     :witness {:resolved? true :dial-moved? false}}))))
    (repair/record-implementation!
     root finding {:attempt-id "repair-1" :commit "good456"
                   :reviewer "codex-7" :review-job "review-2"
                   :repair/discharge-context
                   (discharge-ctx :implementation (:repair/id finding)
                                  "repair-1" "review-2")
                   :witness {:resolved? true :dial-moved? true}})
    (let [awaiting (first (repair/open-obligations root))]
      (is (= :awaiting-validation (:repair/status awaiting)))
      (repair/resolve! root awaiting
                       {:attempt-id "successor-1" :commit "next789"
                        :reviewer "codex-7" :review-job "review-3"
                        :repair/discharge-context
                        (discharge-ctx :successor-validation (:repair/id finding)
                                       "successor-1" "review-3")
                        :witness {:resolved? true :dial-moved? true}
                        :validation {:production-shaped? true}}))
    (is (empty? (repair/open-obligations root)))))

(deftest review-failure-finding-carries-typed-discharge-contract
  ;; repair-attempt-054: schema-1 review-failure findings carried no
  ;; discharge contract, so the repair attempt's construction ran with
  ;; :discharge nil and the discharge requirements were never
  ;; machine-visible. Every new finding mints the typed contract.
  (let [root (temp-root)
        base {:target :target/a :commit "bad123"
              :selected-entry {:action {:type :x}}
              :reviewer "codex-1" :review-job "review-1"
              :review-text "keep classification typed"}
        requested (repair/record-review-failure!
                   root (assoc base
                               :attempt-id "failed-rc"
                               :review-verdict :request-changes))
        rejected (repair/record-review-failure!
                  root (assoc base
                              :attempt-id "failed-rj"
                              :review-verdict :reject))]
    (is (= 2 (:repair/schema-version requested)))
    (is (= "repair-failed-rc-review-request-changes"
           (:repair/id requested)))
    (is (= "repair-failed-rj-review-rejected"
           (:repair/id rejected)))
    (is (= :independent-review (:failure-stage requested)))
    (is (= :review-request-changes (:failure-kind requested)))
    (is (= :review-rejected (:failure-kind rejected)))
    (is (= repair/review-failure-discharge-contract
           (:discharge-contract requested)))
    (is (= [:distinct-repair-commit :independent-review
            :grounded-repair :distinct-production-shaped-successor]
           (get-in requested [:discharge-contract :requires])))
    (is (= :code-commit
           (get-in requested [:discharge-contract :artifact-shape])))))

(deftest distinct-review-outcomes-do-not-collide-in-the-finding-store
  (let [root (temp-root)
        base {:attempt-id "attempt-002" :target :target/a :commit "bad123"
              :selected-entry {:action {:type :x}}
              :reviewer "codex-24" :review-job "review-1"
              :review-text "review outcome remains unresolved"}
        requested (repair/record-review-failure!
                   root (assoc base :review-verdict :request-changes))
        rejected (repair/record-review-failure!
                  root (assoc base :review-verdict :reject))]
    (is (not= (:repair/id requested) (:repair/id rejected)))
    (is (= #{:review-request-changes :review-rejected}
           (set (map :failure-kind (repair/open-obligations root)))))))

(deftest system-actuation-failure-is-distinct-durable-stop-line-memory
  (let [root (temp-root)
        finding (repair/record-system-failure!
                 root {:attempt-id "attempt-002"
                       :repair-class :machine-failure
                       :machine-repo "/home/joe/code/futon2"
                       :target :fire-pattern
                       :selected-entry
                       {:action {:type :learn-action-class
                                 :target-class :fire-pattern}}
                       :failure-stage :construction
                       :outcome :construction-failed
                       :error "No construction for selected decision"})]
    (is (= :machine-failure (:repair/class finding)))
    (is (= "/home/joe/code/futon2" (:machine-repo finding)))
    (is (= [finding] (repair/open-obligations root)))
    (repair/record-implementation!
     root finding {:attempt-id "canary-repair" :commit "good456"
                   :reviewer "claude-1" :review-job "review-2"
                   :review-evidence
                   {:job-id "review-2" :reviewer "claude-1"
                    :state "done" :verdict :approve :valid? true
                    :execution {:executed true :tool-events 1
                                :command-events 1}}
                   :artifact-binding
                   {:repo "/home/joe/code/futon2" :commit "good456"
                    :fresh-author? true :descendant? true
                    :in-author-window? true :corroborates? true
                    :disagreement? false}
                   :repair/discharge-context
                   (discharge-ctx :implementation (:repair/id finding)
                                  "canary-repair" "review-2")
                   :witness {:resolved? true :dial-moved? true}})
    (repair/resolve! root (first (repair/open-obligations root))
                     {:attempt-id "canary-successor" :commit "next789"
                      :reviewer "claude-1" :review-job "review-3"
                      :repair/discharge-context
                      (discharge-ctx :successor-validation (:repair/id finding)
                                     "canary-successor" "review-3")
                      :witness {:resolved? true :dial-moved? true}
                      :validation {:production-shaped? true}})
    (is (empty? (repair/open-obligations root)))))

(deftest one-attempt-can-open-independent-typed-findings
  (let [root (temp-root)
        common {:attempt-id "attempt-006"
                :failure-stage :author-wait
                :outcome :incomplete
                :error "work was incorrectly declared stalled"}
        machine (repair/record-system-failure!
                 root (assoc common
                             :repair-class :machine-failure
                             :failure-kind :false-timeout))
        artifact (repair/record-system-failure!
                  root (assoc common
                              :repair-class :incomplete-recoverable
                              :failure-kind :late-author-artifact))]
    (is (= #{"repair-attempt-006-false-timeout"
             "repair-attempt-006-late-author-artifact"}
           (set (map :repair/id (repair/open-obligations root)))))
    (is (not= (:repair/id machine) (:repair/id artifact)))))

(deftest system-finding-replay-is-byte-exact-or-a-typed-conflict
  (let [root (temp-root)
        finding {:attempt-id "cohort--ea1-authority--attempt-001"
                 :repair-class :environmental-hold
                 :failure-stage :agent-readiness
                 :outcome :agent-unavailable
                 :failure-kind :agent-readiness-failed
                 :error "Agency unavailable"
                 :opened-at "2026-09-11T14:45:12Z"
                 :backtrace {:source :disposable}}
        first-record (repair/record-system-failure! root finding)
        replay (repair/record-system-failure! root finding)
        conflict (try
                   (repair/record-system-failure!
                    root (assoc finding :error "different evidence"))
                   nil
                   (catch clojure.lang.ExceptionInfo e e))]
    (is (= first-record replay))
    (is (= :repair-finding-conflict (:reason (ex-data conflict))))
    (is (= [first-record] (repair/open-obligations root)))))

(deftest system-finding-replay-rejects-symlink-authority-and-is-race-safe
  (let [root (temp-root)
        outside-root (temp-root)
        finding {:attempt-id "cohort--ea1-authority--attempt-002"
                 :repair-class :environmental-hold
                 :failure-stage :agent-readiness
                 :outcome :agent-unavailable
                 :failure-kind :agent-readiness-failed
                 :error "Agency unavailable"
                 :opened-at "2026-09-11T14:45:12Z"}
        results (mapv deref
                      [(future (repair/record-system-failure! root finding))
                       (future (repair/record-system-failure! root finding))])
        record (first results)
        target (java.io.File. root
                              (str "findings/" (:repair/id record) ".edn"))
        outside (java.io.File. outside-root "outside.edn")]
    (is (= (first results) (second results)))
    (io/copy target outside)
    (io/delete-file target)
    (java.nio.file.Files/createSymbolicLink
     (.toPath target) (.toPath outside)
     (make-array java.nio.file.attribute.FileAttribute 0))
    (is (= :repair-finding-conflict
           (:reason
            (ex-data
             (try (repair/record-system-failure! root finding) nil
                  (catch clojure.lang.ExceptionInfo e e))))))
    (let [symlink-root (str (temp-root) "-link")]
      (java.nio.file.Files/createSymbolicLink
       (.toPath (java.io.File. symlink-root)) (.toPath (java.io.File. root))
       (make-array java.nio.file.attribute.FileAttribute 0))
      (is (= :repair-finding-root-refused
             (:reason
              (ex-data
               (try (repair/record-system-failure! symlink-root
                                                   (assoc finding :attempt-id "other"))
                    nil
                    (catch clojure.lang.ExceptionInfo e e)))))))
    (let [parent-root (temp-root)
          external-findings (java.io.File. (temp-root) "external-findings")]
      (.mkdir external-findings)
      (java.nio.file.Files/createSymbolicLink
       (.toPath (java.io.File. parent-root "findings"))
       (.toPath external-findings)
       (make-array java.nio.file.attribute.FileAttribute 0))
      (is (= :repair-finding-root-refused
             (:reason
              (ex-data
               (try (repair/record-system-failure! parent-root
                                                   (assoc finding :attempt-id "parent"))
                    nil
                    (catch clojure.lang.ExceptionInfo e e)))))))))

(deftest failed-commit-cannot-be-its-own-repair-implementation
  (let [root (temp-root)
        finding (repair/record-review-failure!
                 root {:attempt-id "failed-same" :target :target/a
                       :commit "bad123" :selected-entry {:action {:type :x}}
                       :reviewer "reviewer" :review-job "review-1"
                       :review-verdict :request-changes
                       :review-text "still defective"})]
    (is (thrown? clojure.lang.ExceptionInfo
                 (repair/record-implementation!
                  root finding {:attempt-id "repair-attempt"
                                :commit "bad123"
                                :reviewer "reviewer" :review-job "review-2"
                                :repair/discharge-context
                                (discharge-ctx :implementation (:repair/id finding)
                                               "repair-attempt" "review-2")
                                :witness {:resolved? true :dial-moved? true}})))))

(deftest artifact-shapes-validate-only-their-own-evidence
  (testing "an absent shape remains the historical code-commit contract"
    (let [root (temp-root)
          obligation (dissoc (shaped-obligation :code-commit
                                                {:failed-commit "bad123"})
                             :discharge-contract)
          record (repair/record-implementation!
                  root obligation
                  (merge grounded-review
                         {:attempt-id "repair-code" :commit "good456"
                          :repair/discharge-context
                          (discharge-ctx :implementation (:repair/id obligation)
                                         "repair-code" "review-job")}))]
      (is (= "good456" (:replacement-commit record)))
      (is (nil? (:artifact-shape record)))))

  (testing "code contracts reject deposit evidence"
    (is (thrown? clojure.lang.ExceptionInfo
                 (repair/record-implementation!
                  (temp-root) (shaped-obligation :code-commit)
                  (merge grounded-review
                         {:attempt-id "wrong-code"
                          :repair/discharge-context
                          (discharge-ctx :implementation "repair-code-commit"
                                         "wrong-code" "review-job")
                          :store-url "http://store" :record-type :records
                          :count-before 1 :count-after 2
                          :deposit-run-id "deposit-1"})))))

  (testing "data contracts reject a bare commit"
    (is (thrown? clojure.lang.ExceptionInfo
                 (repair/record-implementation!
                  (temp-root) (shaped-obligation :data-deposit)
                  (merge grounded-review
                         {:attempt-id "wrong-data" :commit "good456"
                          :repair/discharge-context
                          (discharge-ctx :implementation "repair-data-deposit"
                                         "wrong-data" "review-job")})))))

  (testing "spec contracts reject a bare commit"
    (is (thrown? clojure.lang.ExceptionInfo
                 (repair/record-implementation!
                  (temp-root) (shaped-obligation :spec-document)
                  (merge grounded-review
                         {:attempt-id "wrong-spec" :commit "good456"
                          :repair/discharge-context
                          (discharge-ctx :implementation "repair-spec-document"
                                         "wrong-spec" "review-job")}))))))

(deftest data-deposit-validation-reads-current-count-without-writing
  (let [root (temp-root)
        reads (atom [])
        obligation (shaped-obligation :data-deposit)
        evidence {:store-url "http://read-only-store"
                  :record-type :wm-hyperparameter-update
                  :count-before 4 :count-after 7
                  :deposit-run-id "deposit-run-7"}]
    (binding [repair/*store-count-reader*
              (fn [url record-type]
                (swap! reads conj [url record-type])
                7)]
      (let [implementation
            (repair/record-implementation!
             root obligation
             (merge grounded-review evidence
                    {:attempt-id "repair-data"
                     :repair/discharge-context
                     (discharge-ctx :implementation (:repair/id obligation)
                                    "repair-data" "review-job")}))]
        (repair/resolve!
         root (assoc obligation
                     :repair/status :awaiting-validation
                     :repair/implementation implementation)
         (merge grounded-review evidence
                {:attempt-id "validate-data"
                 :repair/discharge-context
                 (discharge-ctx :successor-validation (:repair/id obligation)
                                "validate-data" "review-job")
                 :validation {:production-shaped? true}}))))
    (is (= [["http://read-only-store" :wm-hyperparameter-update]
            ["http://read-only-store" :wm-hyperparameter-update]]
           @reads))
    (testing "the declared after-count must match the independent store read"
      (binding [repair/*store-count-reader* (fn [_ _] 6)]
        (is (thrown? clojure.lang.ExceptionInfo
                     (repair/record-implementation!
                      (temp-root) obligation
                      (merge grounded-review evidence
                             {:attempt-id "stale-data-evidence"
                              :repair/discharge-context
                              (discharge-ctx :implementation (:repair/id obligation)
                                             "stale-data-evidence" "review-job")}))))))))

(deftest spec-document-requires-ancestor-commit-that-touched-existing-path
  (let [root (temp-root)
        repo (temp-root)
        path "repair-spec.md"
        file (java.io.File. repo path)]
    (is (zero? (:exit (shell/sh "git" "-C" repo "init" "-q"))))
    (is (zero? (:exit (shell/sh "git" "-C" repo "config"
                                "user.email" "repair-test@example.invalid"))))
    (is (zero? (:exit (shell/sh "git" "-C" repo "config"
                                "user.name" "Repair Test"))))
    (spit file "declared repair contract\n")
    (is (zero? (:exit (shell/sh "git" "-C" repo "add" path))))
    (is (zero? (:exit (shell/sh "git" "-C" repo "commit" "-q"
                                "-m" "Add repair spec"))))
    (let [sha (str/trim
               (:out (shell/sh "git" "-C" repo "rev-parse" "HEAD")))
          obligation (shaped-obligation :spec-document {:machine-repo repo})
          evidence {:path path :git-sha sha}
          implementation (repair/record-implementation!
                          root obligation
                          (merge grounded-review evidence
                                 {:attempt-id "repair-spec"
                                  :repair/discharge-context
                                  (discharge-ctx :implementation (:repair/id obligation)
                                                 "repair-spec" "review-job")}))]
      (is (= evidence (:replacement-artifact implementation)))
      (is (thrown? clojure.lang.ExceptionInfo
                   (repair/record-implementation!
                    (temp-root) obligation
                    (merge grounded-review {:attempt-id "bad-spec"
                                            :repair/discharge-context
                                            (discharge-ctx :implementation (:repair/id obligation)
                                                           "bad-spec" "review-job")
                                            :path path :git-sha "deadbeef"}))))
      (let [resolved (repair/resolve!
                      root (assoc obligation
                                  :repair/status :awaiting-validation
                                  :repair/implementation implementation)
                      (merge grounded-review evidence
                             {:attempt-id "validate-spec"
                              :repair/discharge-context
                              (discharge-ctx :successor-validation (:repair/id obligation)
                                             "validate-spec" "review-job")
                              :validation {:production-shaped? true}}))]
        (is (= :spec-document (:artifact-shape resolved)))
        (is (= evidence (:validation-artifact resolved)))))))

(deftest impossible-recovery-is-immutably-superseded-by-typed-successor
  (let [root (temp-root)
        old (repair/record-system-failure!
             root {:attempt-id "old" :repair-class :incomplete-recoverable
                   :failure-stage :author-wait :outcome :incomplete
                   :failure-kind :agent-budget-expired :error "budget"})
        successor (repair/record-system-failure!
                   root {:attempt-id "new" :repair-class :machine-failure
                         :failure-stage :author-wait :outcome :incomplete
                         :failure-kind :recovery-job-terminal
                         :error "job failed"})]
    (repair/supersede! root old successor :recovery-job-terminal)
    (is (= [(:repair/id successor)]
           (mapv :repair/id (repair/open-obligations root))))
    (is (thrown? java.nio.file.FileAlreadyExistsException
                 (repair/supersede! root old successor
                                    :recovery-job-terminal)))))

(deftest implementation-refusals-name-their-failed-conjuncts
  ;; repair-ea1-7093...-untyped-failure: the stop-line refusal conflated
  ;; eight conjuncts behind one message. The refusal must stay fail-closed
  ;; and identical in kind, but name what failed.
  (let [root (temp-root)
        finding (repair/record-review-failure!
                 root {:attempt-id "same-attempt" :target :target/a
                       :commit "bad123" :selected-entry {:action {:type :x}}
                       :reviewer "reviewer" :review-job "review-1"
                       :review-verdict :request-changes
                       :review-text "defective"})
        refusal (fn [impl]
                  (try (repair/record-implementation! root finding impl)
                       nil
                       (catch clojure.lang.ExceptionInfo e (ex-data e))))]
    (let [d (refusal {:attempt-id "same-attempt" :commit "good456"
                      :reviewer "reviewer" :review-job "review-2"
                      :repair/discharge-context
                      (discharge-ctx :implementation (:repair/id finding)
                                     "same-attempt" "review-2")
                      :witness {:resolved? true :dial-moved? true}})]
      (is (= :machine-repair-lacks-grounded-review-evidence (:failure-kind d)))
      (is (= :stop-line-resolution (:failure-stage d)))
      (is (some #{:implementation-attempt-not-distinct} (:failure-detail d)))
      (is (not-any? #{:witness-not-resolved} (:failure-detail d))))
    (let [d (refusal {:attempt-id "repair-2" :commit "good456"
                      :reviewer "reviewer" :review-job "review-2"
                      :repair/discharge-context
                      (discharge-ctx :implementation (:repair/id finding)
                                     "repair-2" "review-2")
                      :witness {:resolved? true :dial-moved? false}})]
      (is (some #{:witness-dial-not-moved} (:failure-detail d)))
      (is (not-any? #{:implementation-attempt-not-distinct} (:failure-detail d))))))

(deftest schema-three-implementation-requires-grounded-independent-review
  (let [root (temp-root)
        obligation {:repair/id "repair-grounded-review"
                    :repair/schema-version 3
                    :repair/status :open
                    :repair/class :machine-failure
                    :attempt-id "ea1-old--attempt-001"
                    :machine-repo "/srv/futon2"
                    :discharge-contract {:artifact-shape :code-commit}}
        base {:attempt-id "ea1-new--attempt-001"
              :commit "abc1234"
              :reviewer "codex-24"
              :review-job "review-42"
              :repair/discharge-context
              (discharge-ctx :implementation "repair-grounded-review"
                             "ea1-new--attempt-001" "review-42")
              :witness {:resolved? true :dial-moved? true}}
        review-evidence {:job-id "review-42" :reviewer "codex-24"
                         :state "done" :verdict :approve :valid? true
                         :execution {:executed true :tool-events 2
                                     :command-events 1}}
        binding {:repo "/srv/futon2" :commit "abc1234"
                 :fresh-author? true :descendant? true
                 :in-author-window? true :corroborates? true
                 :disagreement? false}
        refusal (fn [implementation]
                  (try (repair/record-implementation! root obligation implementation)
                       nil
                       (catch clojure.lang.ExceptionInfo e (ex-data e))))]
    (testing "self-asserted legacy labels do not discharge a schema-3 finding"
      (is (some #{:grounded-review-evidence-invalid}
                (:failure-detail (refusal base)))))
    (testing "borrowed review and artifact bindings refuse at the same gate"
      (is (some #{:grounded-review-evidence-invalid}
                (:failure-detail
                 (refusal (assoc base
                                 :review-evidence
                                 (assoc review-evidence :job-id "review-other")
                                 :artifact-binding binding)))))
      (is (some #{:grounded-review-evidence-invalid}
                (:failure-detail
                 (refusal (assoc base
                                 :review-evidence review-evidence
                                 :artifact-binding
                                 (assoc binding :commit "def5678")))))))
    (testing "the exact independently reviewed implementation is retained"
      (let [record (repair/record-implementation!
                    root obligation
                    (assoc base :review-evidence review-evidence
                           :artifact-binding binding))]
        (is (= "abc1234" (:replacement-commit record)))
        (is (= review-evidence (:grounded-review-evidence record)))
        (is (= binding (:artifact-binding record)))
        (is (= record
               (edn/read-string
                (slurp (io/file root "implementations"
                                "repair-grounded-review.edn")))))))))

(deftest system-finding-replay-accepts-a-finding-written-before-the-pr-str-switch
  ;; These files were pprinted until 2026-09-19. Measured on a real 19.4 MB
  ;; finding: pprint 47,306 ms, pr-str 212 ms — and those 47 seconds were spent
  ;; holding the contended store lock. Replay here is decided by exact bytes,
  ;; deliberately, so the format change would have turned every replay of an
  ;; already-stored finding into a :repair-finding-conflict. This pins that it
  ;; does not.
  (let [root (temp-root)
        finding {:attempt-id "cohort--ea1-legacy-bytes--attempt-001"
                 :repair-class :environmental-hold
                 :failure-stage :agent-readiness
                 :outcome :agent-unavailable
                 :failure-kind :agent-readiness-failed
                 :error "Agency unavailable"
                 :opened-at "2026-09-11T14:45:12Z"
                 :backtrace {:source :disposable}}
        ;; Publish once, then rewrite the file in the OLD pprint form to stand
        ;; in for everything already on disk.
        record (repair/record-system-failure! root finding)
        path (str root "/findings/" (:repair/id record) ".edn")
        stored (edn/read-string (slurp path))
        _ (spit path (with-out-str (pp/pprint stored)))
        legacy-bytes (count (slurp path))
        replay (repair/record-system-failure! root finding)]

    (testing "the file on disk really is in the legacy pretty-printed form"
      (is (re-find #"\n " (slurp path)))
      (is (> legacy-bytes (count (pr-str stored)))))

    (testing "replaying it is acknowledged, not raised as a byte conflict"
      (is (= record replay)))

    (testing "a genuinely different finding is still a typed conflict"
      (is (= :repair-finding-conflict
             (:reason (ex-data (try (repair/record-system-failure!
                                     root (assoc finding :error "different evidence"))
                                    nil
                                    (catch clojure.lang.ExceptionInfo e e)))))))))

(deftest durable-finding-copy-is-bounded-with-visible-elision
  ;; The finding opened 2026-09-19T01:09 was 19,220,142 bytes for one failed
  ;; attempt, serialised while holding the publication lock. The disk copy is
  ;; now bounded like tripwire's durable trip reports; the in-memory record
  ;; stays whole, and the elision is visible, never silent.
  (let [root (temp-root)
        ;; ~1500 collection nodes: far over finding-node-budget (1024).
        huge-checkpoints (zipmap (map #(str "checkpoint-" %) (range 1500))
                                 (range 1500))
        finding (repair/record-system-failure!
                 root {:attempt-id "cohort--ea1-bound--attempt-001"
                       :repair-class :machine-failure
                       :machine-repo "/home/joe/code/futon2"
                       :failure-stage :construction
                       :outcome :construction-failed
                       :error "build failed"
                       :backtrace {:job-id "author-job-9"
                                   :code-state {:repo "/home/joe/code/futon2"
                                                :branch "main"}
                                   :checkpoints huge-checkpoints}
                       :selected-entry {:action {:type :learn-action-class
                                                 :payload huge-checkpoints}}})
        path (str root "/findings/" (:repair/id finding) ".edn")
        stored (edn/read-string (slurp path))]
    (testing "the in-memory record is left whole"
      (is (= huge-checkpoints (get-in finding [:backtrace :checkpoints]))))
    (testing "small consumer-read keys survive verbatim"
      (is (= "author-job-9" (get-in stored [:backtrace :job-id])))
      (is (= "/home/joe/code/futon2"
             (get-in stored [:backtrace :code-state :repo]))))
    (testing "oversized entries are visibly elided, not silently dropped"
      (is (= :exceeds-durable-finding-budget
             (get-in stored [:backtrace :checkpoints :elided/reason])))
      (is (pos? (get-in stored [:backtrace :checkpoints :elided/count])))
      ;; The real 1.7 MB :selected-entry/:action elides whole; this synthetic
      ;; one is narrow, so the elision lands on its oversized :payload.
      (is (= :exceeds-durable-finding-budget
             (get-in stored [:selected-entry :action :payload :elided/reason]))))
    (testing "the disk copy is small"
      (is (< (.length (io/file path)) 16384)))
    (testing "replay of the bounded finding is acknowledged byte-exactly"
      (is (= finding
             (repair/record-system-failure!
              root {:attempt-id "cohort--ea1-bound--attempt-001"
                    :repair-class :machine-failure
                    :machine-repo "/home/joe/code/futon2"
                    :failure-stage :construction
                    :outcome :construction-failed
                    :error "build failed"
                    :backtrace {:job-id "author-job-9"
                                :code-state {:repo "/home/joe/code/futon2"
                                             :branch "main"}
                                :checkpoints huge-checkpoints}
                    :selected-entry {:action {:type :learn-action-class
                                              :payload huge-checkpoints}}
                    :opened-at (:opened-at finding)}))))))

(deftest replay-of-a-pre-bound-raw-finding-is-acknowledged
  ;; Findings written before the bound are on disk UNBOUNDED. A genuine
  ;; replay of one must still be acknowledged rather than raised as a byte
  ;; conflict, so the exists-path also compares the raw pr-str bytes.
  (let [root (temp-root)
        base {:attempt-id "cohort--ea1-prebound--attempt-001"
              :repair-class :machine-failure
              :failure-stage :construction
              :outcome :construction-failed
              :error "build failed"
              :opened-at "2026-09-19T01:09:43.610753307Z"
              :backtrace {:checkpoints (zipmap (map str (range 1500))
                                               (range 1500))}}
        record (repair/record-system-failure! root base)
        path (str root "/findings/" (:repair/id record) ".edn")]
    ;; Stand in for everything already on disk: rewrite the file with the
    ;; UNBOUNDED record, exactly as written before this change.
    (spit path (pr-str record))
    (is (some? (repair/record-system-failure! root base)))))

(deftest occurrence-identity-publishes-once-and-retains-observations
  (let [root (temp-root)
        occurrence (repair/occurrence-identity
                    {:origin "wm/test-authority/run-1"
                     :event-id "invoke-author-1"
                     :failure-kind :build-failed
                     :created-at "2026-09-19T12:00:00Z"})
        base {:attempt-id "attempt-001"
              :occurrence occurrence
              :repair-class :machine-failure
              :machine-repo "/repo"
              :target "target-1"
              :failure-stage :author-wait
              :outcome :build-failed
              :failure-kind :build-failed
              :error "author failed"
              :discharge-contract {:requires [:distinct-repair-commit]
                                   :artifact-shape :code-commit}}
        first-record (repair/record-system-failure!
                      root (assoc base :observation
                                  {:observation/id "inner-catch"}))
        finding-path (io/file root "findings"
                              (str (:repair/id first-record) ".edn"))
        first-bytes (java.nio.file.Files/readAllBytes (.toPath finding-path))
        replay (repair/record-system-failure!
                root (assoc base :observation
                            {:observation/id "outer-catch"
                             :observed-at "2026-09-19T12:05:00Z"}))
        evidence-dir (io/file root "occurrence-evidence"
                              (:occurrence/id occurrence))]
    (is (= (:repair/id first-record) (:repair/id replay)))
    (is (= occurrence (:repair/occurrence first-record)))
    (is (= (:opened-at first-record) (:opened-at replay))
        "a retry retains the first publication timestamp")
    (is (java.util.Arrays/equals
         first-bytes (java.nio.file.Files/readAllBytes (.toPath finding-path))))
    (is (= 1 (count (filter #(and (.isFile %)
                                  (str/ends-with? (.getName %) ".edn"))
                            (file-seq (io/file root "findings"))))))
    (is (= 2 (count (filter #(.isFile %) (file-seq evidence-dir))))
        "inner and outer observations append evidence, not findings")
    (is (= :repair-finding-conflict
           (:reason
            (ex-data
             (try (repair/record-system-failure!
                   root (assoc base :error "semantically different"))
                  nil
                  (catch clojure.lang.ExceptionInfo e e)))))
        "one occurrence id cannot alias conflicting semantic payloads")))

(deftest occurrence-publication-refuses-id-and-payload-aliases-before-write
  (let [root (temp-root)
        occurrence (repair/occurrence-identity
                    {:origin "wm/test-authority/run-alias"
                     :event-id "job-alias" :failure-kind :build-failed})
        base {:attempt-id "attempt-001" :occurrence occurrence
              :repair-class :machine-failure :failure-stage :author-wait
              :outcome :build-failed :failure-kind :build-failed
              :error "failed"}
        refusal (fn [finding]
                  (try (repair/record-system-failure! root finding) nil
                       (catch clojure.lang.ExceptionInfo e (ex-data e))))]
    (is (= :occurrence-identity-conflict
           (:repair-occurrence/refusal
            (refusal (assoc base :repair-id "repair-caller-alias")))))
    (is (= :occurrence-identity-conflict
           (:repair-occurrence/refusal
            (refusal (assoc-in base [:occurrence :occurrence/id]
                               "occ-fabricated")))))
    (is (empty? (filter #(and (.isFile %)
                              (str/ends-with? (.getName %) ".edn"))
                        (file-seq (io/file root "findings"))))
        "contradictory occurrence evidence publishes no finding")))

(deftest parallel-occurrence-publication-is-create-new-safe
  (let [root (temp-root)
        occurrence (repair/occurrence-identity
                    {:origin "wm/test-authority/run-parallel"
                     :event-id "invoke-parallel"
                     :failure-kind :build-failed
                     :created-at "2026-09-19T12:00:00Z"})
        finding {:attempt-id "attempt-001" :occurrence occurrence
                 :observation {:observation/id "same-observation"}
                 :repair-class :machine-failure :failure-stage :author-wait
                 :outcome :build-failed :failure-kind :build-failed
                 :error "never executed"}
        records (mapv deref
                      [(future (repair/record-system-failure! root finding))
                       (future (repair/record-system-failure! root finding))])]
    (is (apply = (map :repair/id records)))
    (is (= 1 (count (filter #(and (.isFile %)
                                  (str/ends-with? (.getName %) ".edn"))
                            (file-seq (io/file root "findings"))))))))

(deftest distinct-occurrences-remain-visible-to-t8
  (let [root (temp-root)
        now (str (java.time.Instant/now))
        ids (mapv (fn [event-id]
                    (:repair/id
                     (repair/record-system-failure!
                      root {:attempt-id "attempt-001"
                            :occurrence
                            (repair/occurrence-identity
                             {:origin "wm/test-authority/run-distinct"
                              :event-id event-id :failure-kind :build-failed
                              :created-at now})
                            :repair-class :machine-failure
                            :target "same-target"
                            :failure-stage :author-wait :outcome :build-failed
                            :failure-kind :build-failed :error "failed"})))
                  ["job-a" "job-b" "job-c"])
        findings (repair/open-obligations root)]
    (is (= 3 (count (set ids))) "distinct jobs mint distinct occurrences")
    (is (= 1 (count (tripwire/livelock-violations findings #{})))
        "occurrence deduplication does not blind repetition detection")))

(deftest dismiss-repaired-elsewhere-refuses-a-commit-that-merely-coincides
  ;; Both cases below DISMISSED against the first version of the anchor rule
  ;; (claude-5's probes, 2026-09-24). Neither commit addresses its finding.
  (let [repo (temp-root)
        git (fn [& args]
              (let [r (apply shell/sh "git" "-C" repo args)]
                (when-not (zero? (:exit r)) (throw (ex-info "fixture git failed" r)))
                (str/trim (:out r))))
        fire (fn [root finding sha]
               (spit (io/file root "findings" (str (:repair/id finding) ".edn"))
                     (pr-str finding))
               (dismissal-refusal
                #(repair/dismiss-repaired-elsewhere!
                  root (:repair/id finding)
                  {:authority "fixture" :reason :repaired-elsewhere
                   :actor "test" :commit sha})))]
    (git "init") (git "config" "user.email" "fixture@example.invalid")
    (git "config" "user.name" "fixture")
    (.mkdirs (io/file repo "src/futon2/aif"))
    (spit (io/file repo "src/futon2/aif/full_loop_runner.clj") "(ns futon2.aif.full-loop-runner)\n")
    (git "add" ".")
    (git "-c" "commit.gpgsign=false" "commit" "--date=2026-09-20T10:00:00Z" "-m" "seed")
    ;; (1) a commit that touches a file the finding names, about something else
    (spit (io/file repo "src/futon2/aif/full_loop_runner.clj")
          "(ns futon2.aif.full-loop-runner)\n;; tidy a docstring typo\n")
    ;; (2) and mentions the schema key every finding in the store carries
    (spit (io/file repo "notes.clj") "(def x {:repair/status :open})\n")
    (git "add" ".")
    (git "-c" "commit.gpgsign=false" "commit" "--date=2026-09-21T09:00:00Z"
         "-m" "fix a typo in a docstring")
    (let [sha (git "rev-parse" "HEAD")
          base {:repair/schema-version 3 :repair/class :machine-failure
                :repair/status :open :attempt-id "a1"
                :failure-stage :selection :failure-outcome :incomplete
                :opened-at "2026-09-20T20:00:00Z" :machine-repo repo}]
      (testing "a filename the finding mentions is no anchor at all"
        ;; A path says the commit touched a file the finding names, not that
        ;; it addressed the condition. It is not an anchor, so a finding
        ;; whose only link to code is a filename has none and refuses here
        ;; rather than at the match.
        (let [root (temp-root)]
          (.mkdirs (io/file root "findings"))
          (is (= :diagnosis-anchor-absent
                 (fire root (assoc base
                                   :repair/id "finding-path-anchor"
                                   :failure-kind :untyped-failure
                                   :failure-error (str "Exception thrown at "
                                                       "src/futon2/aif/full_loop_runner.clj"
                                                       " during selection")
                                   :failure-data {:detail "nothing to do with typos"})
                       sha)))
          (is (empty? (.listFiles (io/file root "dismissals"))))))
      (testing "the finding's own :repair/ schema keys are not anchors"
        ;; Otherwise every finding is evidenced by any commit touching the
        ;; repair store -- including the commit that added this route.
        (let [root (temp-root)]
          (.mkdirs (io/file root "findings"))
          (is (= :diagnosis-anchor-absent
                 (fire root (assoc base
                                   :repair/id "finding-schema-anchor"
                                   :failure-kind :fold-output-invalid
                                   :failure-error "the fold gate refused policy holes"
                                   :failure-data {:detail "unrelated to bookkeeping"})
                       sha)))
          (is (empty? (.listFiles (io/file root "dismissals")))))))))
