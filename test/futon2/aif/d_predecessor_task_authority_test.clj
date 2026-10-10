(ns futon2.aif.d-predecessor-task-authority-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.action-identity :as action-identity]
            [futon2.aif.close-retention :as retention]
            [futon2.aif.d-predecessor-task-authority :as task]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.observation-checks :as observation]
            [futon2.aif.policy-precision-carry :as precision-carry]
            [futon2.aif.task-execution-evidence :as execution]
            [futon2.aif.token-belief-predecessor :as predecessor]
            [futon2.aif.trace :as trace]
            [futon2.data-paths :as data-paths]
            [clojure.edn :as edn])
  (:import (java.nio.file Files) (java.time Instant) (java.util UUID)))

(def ^:dynamic *git-environment* (into {} (System/getenv)))

(deftest historical-production-defaults-resolve-through-test-authority
  (let [expected (data-paths/path "wm-d-task-enactment")
        canonical (str (io/file data-paths/production-data-root
                                "wm-d-task-enactment"))]
    (is (= expected (#'task/effective-root "data/wm-d-task-enactment")))
    (is (= expected (#'task/effective-root canonical)))))

(def ^:private git-routing-environment-keys
  ["GIT_DIR" "GIT_WORK_TREE" "GIT_INDEX_FILE" "GIT_COMMON_DIR"])

(defn- isolated-git-environment []
  (apply dissoc *git-environment* git-routing-environment-keys))

(defn git! [repo & args]
  (let [r (apply shell/sh
                 (concat ["git" "-C" (str repo)] args
                         [:env (isolated-git-environment)]))]
    (when-not (zero? (:exit r)) (throw (ex-info "fixture git failed" r)))
    (str/trim (:out r))))

(defn precision-family [action schedule]
  (precision-carry/family
   {:action action
    :selection-certificate {:candidates [{:id action :g 1.0 :habit 1.0}]}}
   {:q0 {#{} 1} :rates {} :horizon 1}
   {(:target action) schedule}))

(defn with-artifact
  ([f] (with-artifact {} f))
  ([opts f]
  (let [dir (.toFile (Files/createTempDirectory "d-task-authority-" (make-array java.nio.file.attribute.FileAttribute 0)))
        repo (io/file dir "repo") root (io/file dir "claims")
        target (get opts :target "target")]
    (try
      (.mkdirs repo)
      (git! repo "init" "-q")
      (when-not (.isDirectory (io/file repo ".git"))
        (throw (ex-info "fixture git init escaped its repository"
                        {:repo (str repo)
                         :expected-git-dir (str (io/file repo ".git"))})))
      (spit (io/file repo "base.txt") "base\n")
      (doseq [[path text] (:before-files opts)]
        (let [file (io/file repo path)] (io/make-parents file) (spit file text)))
      (git! repo "add" ".")
      (git! repo "-c" "user.name=D fixture"
            "-c" "user.email=d-fixture@example.invalid"
            "commit" "-qm" "before")
      (let [before {:repo (str repo) :head (git! repo "rev-parse" "HEAD")
                    :observed-at-ms (System/currentTimeMillis)}
            action (or (:action opts) {:kind :cascade-candidate :id :C0 :target target
                    :precedence [{:id :make-file :theta 1 :produces #{[target :artifact]}}]})
            occurrence (retention/mint-occurrence
                        {:run-id "run" :cohort-id "cohort" :attempt-id "attempt"
                         :selected-action action :now #(Instant/now) :uuid-fn #(UUID/randomUUID)})
            occurrence ((or (:occurrence-fn opts) identity) occurrence)
            declaration (io/file dir "declaration.edn")
            _ (spit declaration (pr-str (cond-> {:target (get opts :declaration-target target) :locators
                                        {:artifact {:class :C3 :repo "repo" :sha (:head before)
                                                    :path (or (:locator-path opts) "created.clj")}}}
                                          (:locators opts)
                                          (update :locators merge (:locators opts))
                                          (:observation-schedule opts)
                                          (assoc :observation-schedule (:observation-schedule opts)))))
            pins [{:path (str declaration)
                   :sha256 (evidence/sha256 (Files/readAllBytes (.toPath declaration)))}]
            dispatch (task/capture {:occurrence occurrence :carry-occurrence-id "carry"
                                    :universe (or (:universe opts) #{[target :artifact]})
                                    :selected-action action
                                    :precision-family (when-let [f (:precision-family-fn opts)]
                                                        (f action))
                                    :declaration-reads pins :before before})
            _ (spit (io/file repo "created.clj") "(ns created)\n")
            _ (git! repo "add" "created.clj")
            _ (git! repo "-c" "user.name=D fixture"
                    "-c" "user.email=d-fixture@example.invalid"
                    "commit" "-qm" "execute task")
            commit (git! repo "rev-parse" "HEAD")
            author {:job-id "author-job" :agent-id "author" :state "done"
                    :result (str "FULL_LOOP_AUTHOR: DONE " commit)
                    :events [{:type "prompt" :text (task/prompt-binding dispatch)}]}
            reviewer {:job-id "review-job" :agent-id "reviewer" :state "done"
                      :result "FULL_LOOP_REVIEW: APPROVE"
                      :events [{:type "prompt" :text (str (task/prompt-binding dispatch) "\nReview " commit "\nRepository: " repo)}
                               {:type "tool_use" :tools ["Bash"]}]}
            jobs {"author-job" author "review-job" reviewer}
            binding (execution/fresh-artifact-binding {} (str repo) before author)
            inputs {:dispatch dispatch :artifact-binding binding :author-job author
                    :review-job reviewer :files ["created.clj"] :repository (str repo) :route :fresh-author}
            expected {:occurrence occurrence :carry-occurrence-id "carry"
                      :selected-action action
                      :universe (or (:universe opts) #{[target :artifact]}) :declaration-pins pins}]
        ;; Only repository location and external Agency read ports are local;
        ;; producer, verifier, Git, occurrence and observation checks are real.
        (with-redefs [observation/repo-root (str dir)]
          (f {:inputs inputs :expected expected :jobs jobs :root (str root)
              :repo repo :commit commit})))
      (finally
        (doseq [file (reverse (file-seq dir))] (io/delete-file file true)))))))

(deftest real-producer-verifier-and-exact-reader
  (with-artifact
   (fn [{:keys [inputs expected jobs root]}]
     (let [produced (task/produce! root inputs expected jobs)
           reread (task/read-predecessor root expected jobs)]
       (is (= :admitted (get-in produced [:verification :status])))
       (is (= :admitted (:status reread)))
       (is (= :executed-with-artifacts (get-in reread [:scope :certifies])))
       (is (not (contains? (get-in reread [:scope :does-not-establish])
                           :machine-enactment-correspondence)))
       (is (= :verified (get-in reread [:candidate-to-minted-join :status])))
       (is (= (get-in reread [:candidate-to-minted-join :selected-action-sha256])
              (get-in reread [:candidate-to-minted-join :enacted-action-sha256])))
       (is (= :not-measured (:before-evidence reread)))
       (is (= #{["target" :artifact]} (:present reread)))
       (is (= {:status :verified} (:token-observation-verification reread)))
       (is (= :declared-kernel-of-verified-macro-action (:b-authority reread)))
       (is (= :independent-check-required (:causal-attribution reread)))
       (is (= :carry-no-predecessor (:kind (task/read-predecessor root (dissoc expected :occurrence) jobs))))))))

(deftest independent-evidence-controls
  (with-artifact
   (fn [{:keys [inputs expected jobs]}]
     (let [record (task/claim inputs)
           check #(task/verify % expected jobs)]
       (doseq [[route kind] [[:deferred-completion :deferred-artifact-not-fresh-execution]
                            [:recovery :deferred-artifact-not-fresh-execution]
                            [:historical :historical-verification-not-execution]
                            [:incomplete :task-execution-incomplete]]]
         (is (= kind (:kind (check (assoc record :route route))))))
       (is (= :carry-domain-changed
              (:kind (task/verify record (assoc expected :universe #{}) jobs))))
       (is (= :carry-occurrence-mismatch
              (:kind (task/verify record (assoc expected :carry-occurrence-id "other") jobs))))
       (is (= :job-occurrence-binding-unestablished
              (:kind (check (assoc-in record [:dispatch :declarations 0 :snapshot :target] "forged")))))
       (is (= :after-token-evidence-mismatch (:kind (check (assoc record :after-token-evidence [])))))
       (is (= :independent-review-not-passed
              (:kind (task/verify record expected
                                  (assoc-in jobs ["review-job" :events]
                                            [{:type "prompt" :text (get-in jobs ["review-job" :events 0 :text])}])))))
       (is (= :job-occurrence-binding-unestablished
              (:kind (task/verify record expected (assoc-in jobs ["review-job" :events 0 :text] "Review another commit")))))
       (is (= :independent-jobs-unestablished
              (:kind (task/verify record expected (assoc-in jobs ["review-job" :agent-id] "author")))))
       (is (not= :admitted (:status (check (assoc-in record [:artifact-binding :commit]
                                                    (get-in inputs [:dispatch :before :head]))))))))))

(deftest selected-and-enacted-payload-mutations-refuse
  (with-artifact
   (fn [{:keys [inputs expected jobs]}]
     (let [action (:selected-action expected)
           mismatch (task/capture-result
                     (assoc (:dispatch inputs)
                            :occurrence (:occurrence expected)
                            :selected-action (assoc action :target "other-target")
                            :declaration-reads []))]
       (is (= :selected-enacted-action-mismatch (:kind mismatch))))
     (let [record (task/claim inputs)
           forged-dispatch (assoc-in (:dispatch inputs)
                                     [:candidate-to-minted-join :enacted-action-sha256]
                                     (action-identity/digest
                                      (assoc (:selected-action expected) :target "other-target")))
           binding (task/prompt-binding forged-dispatch)
           jobs (-> jobs
                    (assoc-in ["author-job" :events 0 :text] binding)
                    (assoc-in ["review-job" :events 0 :text]
                              (str binding "\nReview " (get-in inputs [:artifact-binding :commit])
                                   "\nRepository: " (:repository inputs))))]
       (is (= :selected-enacted-correspondence-invalid
              (:kind (task/verify (assoc record :dispatch forged-dispatch)
                                  expected jobs))))))))

(deftest missing-selected-declaration-does-not-mask-enactment-authority
  (let [schedule {:status :held :reason :observation-placement-not-declared}]
    (with-artifact
     {:declaration-target "different-target"
      :precision-family-fn #(precision-family % schedule)}
     (fn [{:keys [inputs expected jobs]}]
       (let [verification (task/verify (task/claim inputs) expected jobs)]
         (is (= :admitted (:status verification)))
         (is (= :verified (get-in verification [:candidate-to-minted-join :status])))
         (is (= {:status :refused
                 :kind :precision-selected-declaration-unestablished}
                (:precision-verification verification)))
         (is (= :after-token-evidence-unavailable
                (get-in verification [:token-observation-verification :kind])))
         (is (= (:universe expected) (:unknown verification)))))))
  (let [declared {:tau {:value 1 :status :declared}}
        mutated {:tau {:value 2 :status :declared}}]
    (with-artifact
     {:observation-schedule declared
      :precision-family-fn #(precision-family % mutated)}
     (fn [{:keys [inputs expected jobs]}]
       (let [verification (task/verify (task/claim inputs) expected jobs)]
         (is (= :admitted (:status verification)))
         (is (= {:status :refused :kind :precision-observation-schedule-mismatch}
                (:precision-verification verification)))
         (is (= :precision-observation-schedule-mismatch
                (:reason (precision-carry/advance
                          {:initialized-beta 1.0
                           :model-id (get-in verification [:precision-family :model-id])
                           :admission verification
                          :family (:precision-family verification)})))))))))

(deftest retained-mission-action-measures-through-its-own-observation-carrier
  ;; The selected action of run 2026-10-05-c9d25d6a, from the tracked fixture
  ;; (copied from the run record at [:decision :selection-law
  ;; :per-policy-argmax :action]; the run record and the retained dispatch are
  ;; not tracked in git, so the test reads neither). The declaration reads are
  ;; two hand-declared sources for other targets, pinned by their bytes now,
  ;; which is what `capture` checks. The measurement runs against the author's
  ;; commit of that run in futon7.
  (let [action (:action
                (edn/read-string
                 (slurp "test/fixtures/selected-want-outcome/2026-10-05-c9d25d6a-action.edn")))
        universe (set (map (fn [want] [(:target action) want]) (:want action)))
        pins (mapv (fn [path]
                     {:path path
                      :sha256 (evidence/sha256 (Files/readAllBytes (.toPath (io/file path))))})
                   ["resources/wm/cascade-sources/M-expressions-of-interest.edn"
                    "resources/wm/cascade-sources/M-wm-08-external-f2.edn"])
        mint (fn [selected]
               (retention/mint-occurrence
                {:run-id "carrier-test" :cohort-id "cohort" :attempt-id "attempt"
                 :selected-action selected :now #(Instant/now) :uuid-fn #(UUID/randomUUID)}))
        capture-action
        (fn [selected]
          (task/capture {:occurrence (mint selected)
                         :carry-occurrence-id "carrier-test-carry"
                         :universe universe
                         :selected-action selected
                         :declaration-reads pins
                         :before "ae45d5472d7764c99ab4cbb182b247f7adebd56e"}))
        dispatch (capture-action action)
        commit "7a9113dfd245ab9918da304f32fc62845ef65741"
        rows (task/artifact-tokens dispatch "/home/joe/code/futon7" commit)
        first-token (first (sort-by pr-str universe))
        positive-action
        (assoc-in action [:observation-locators first-token :decl]
                  "- [ ] The inventory records a follow-up mission’s first concrete artifact and its business-validation measurement.")
        positive-dispatch (capture-action positive-action)
        positive-rows (task/artifact-tokens positive-dispatch "/home/joe/code/futon7" commit)
        ;; the same dispatch as captured before the carrier existed
        without-carrier (dissoc dispatch :selected-action-observation-carrier)]
    (is (= 4 (count universe)))
    (is (= 2 (count (:declarations dispatch))))
    (is (empty? (task/artifact-tokens without-carrier "/home/joe/code/futon7" commit))
        "declaration files alone give no measurement for a mission-derived target")
    (is (every? #(not= (:target action) (get-in % [:snapshot :target]))
                (:declarations dispatch)))
    (is (= {:schema :wm/selected-action-observation-carrier-v1
            :target (:target action)
            :selected-action-sha256 (action-identity/digest action)
            :observation-locators (:observation-locators action)}
           (:selected-action-observation-carrier dispatch)))
    (is (= universe (set (map :token rows))))
    (is (= 4 (count rows)))
    (is (every? #(= commit (get-in % [:after-locator :sha])) rows))
    (is (every? #(= commit (get-in % [:result :evidence :resolved-sha])) rows))
    (is (every? #(= (get-in action [:observation-locators (:token %)])
                    (:declared-locator %)) rows))
    (is (every? #(false? (get-in % [:result :observed])) rows))
    (is (= {:status :refused :kind :after-token-not-observed
            :measured-token-count 4}
           (task/token-observation-verification rows)))
    (is (= {:status :refused :kind :after-token-evidence-unavailable}
           (task/token-observation-verification [])))
    (is (= {:status :verified}
           (task/token-observation-verification positive-rows)))
    (is (true? (get-in (first (filter #(= first-token (:token %)) positive-rows))
                       [:result :observed])))))

(deftest measured-false-carrier-remains-admitted-with-a-truthful-diagnostic
  (let [schedule {:status :held :reason :observation-placement-not-declared}
        action {:kind :cascade-candidate :id :C0 :target "target"
                :observation-locators
                {["target" :artifact]
                 {:class :C3 :repo "repo" :sha "HEAD" :path "absent.clj"}}
                :precedence [{:id :make-file :produces #{["target" :artifact]}}]}]
    (with-artifact
     {:action action :declaration-target "different-target"
      :precision-family-fn #(precision-family % schedule)}
     (fn [{:keys [inputs expected jobs]}]
       (let [verification (task/verify (task/claim inputs) expected jobs)]
         (is (= :admitted (:status verification)))
         (is (= #{} (:present verification)))
         (is (= (:universe expected) (:unknown verification)))
         (is (= {:status :refused :kind :after-token-not-observed
                 :measured-token-count 1}
                (:token-observation-verification verification))))))))

(deftest refusal-is-preserved-by-persistence
  (with-artifact
   (fn [{:keys [inputs expected jobs root]}]
     (let [produced (task/produce! root (assoc inputs :route :recovery) expected jobs)
           reread (task/read-predecessor root expected jobs)]
       (is (= :deferred-completion
              (:route (edn/read-string (slurp (get-in produced [:source :path]))))))
       (is (= :refused (get-in produced [:verification :status])))
       (is (= :deferred-artifact-not-fresh-execution (:kind reread)))
       (spit (get-in produced [:source :path]) "{:broken")
       (is (= :invalid (:status (task/read-predecessor root expected jobs))))))))

(deftest absent-artifact-and-unrelated-head-are-not-execution-evidence
  (with-artifact {:locator-path "never-created.clj"}
    (fn [{:keys [inputs expected jobs]}]
      (is (= :after-token-evidence-unavailable
             (:kind (task/verify (task/claim inputs) expected jobs))))))
  (with-artifact
    (fn [{:keys [inputs expected jobs repo]}]
      (let [record (task/claim inputs)]
        (spit (io/file repo "unrelated.txt") "unrelated\n")
        (git! repo "add" "unrelated.txt")
        (git! repo "-c" "user.name=D fixture"
              "-c" "user.email=d-fixture@example.invalid"
              "commit" "-qm" "unrelated concurrent work")
        (is (not= :admitted
                  (:status (task/verify record expected
                                        (assoc-in jobs ["author-job" :result]
                                                  (str "FULL_LOOP_AUTHOR: DONE " (git! repo "rev-parse" "HEAD")))))))))))

(deftest trace-to-admission-reader-preserves-authority-scope
  (with-artifact
    (fn [{:keys [inputs expected jobs root repo]}]
      (task/produce! root inputs expected jobs)
      (let [path (trace/write-trace! {:decision {:action (get-in expected [:occurrence :action/value])}
                                     :d-task-context expected}
                                    :dir (str (io/file repo "trace")))
            retained (edn/read-string (slurp path))
            stage {:occurrence-id "next-carry" :initialization {:value {#{} 1}}
                   :prospective-prior {:universe (:universe expected)}
                   :prospective-carry {:universe (:universe expected)}}]
        (with-redefs [task/default-root root task/agency-job jobs]
          (let [inspection (predecessor/inspect-trace retained)
                input (predecessor/input-receipt stage inspection)]
            (is (= :admitted (get-in input [:carry-admission :status])))
            (is (= :executed-with-artifacts (get-in input [:carry-admission :authority :scope :certifies])))
            (is (predecessor/valid-input? input stage))
            (is (= :not-wired (:conditioning-status input)))
            (is (= [] (:observation-updates input)))
            (is (= {#{} 1} (:continuation-belief input)))))))))


(deftest failed-fresh-author-is-not-a-recovered-artifact
  ;; Tick B: completed fresh revision, retained binding, rejected second review.
  (with-artifact
    (fn [{:keys [inputs expected jobs root]}]
      (let [jobs (assoc-in jobs ["review-job" :result]
                           "FULL_LOOP_REVIEW: REQUEST_CHANGES locator contract")
            data (assoc inputs :commit (get-in inputs [:artifact-binding :commit])
                               :dispatch-route :fresh-author)
            result (task/complete! root {:status :captured :dispatch (:dispatch inputs)}
                                   expected data jobs)
            claim (edn/read-string (slurp (get-in result [:source :path])))]
        (is (= :fresh-author (:route claim)))
        (is (= (:artifact-binding inputs) (:artifact-binding claim)))
        (is (= :independent-review-not-passed (get-in result [:verification :kind])))))))

(deftest absent-binding-is-not-positive-recovery-evidence
  (with-artifact
    (fn [{:keys [inputs expected jobs root]}]
      (doseq [[data expected-kind]
              [[(dissoc (assoc inputs :commit "present" :dispatch-route :fresh-author)
                         :artifact-binding) :binding-not-retained]
               [(assoc inputs :commit "present" :dispatch-route :recovery)
                :deferred-artifact-not-fresh-execution]
               [(assoc inputs :commit "present") :dispatch-not-retained]]]
        (let [result (task/complete! (str root "/" (name expected-kind))
                                    {:status :captured :dispatch (:dispatch inputs)}
                                    expected data jobs)]
          (is (= expected-kind (get-in result [:verification :kind]))))))))

(deftest tick-b-retained-dossier-replay
  (let [fixture (edn/read-string (slurp "test/fixtures/tick-b-enactment.edn"))
        dispatch (:dispatch fixture)
        expected {:occurrence (:occurrence dispatch)
                  :selected-action (get-in dispatch [:occurrence :action/value])
                  :carry-occurrence-id (:carry-occurrence-id dispatch)
                  :universe (:universe dispatch)}
        jobs (into {} (map (juxt :job-id identity)
                           [(:author-job fixture) (:review-job fixture)]))
        root (.toFile (Files/createTempDirectory "tick-b-replay-"
                       (make-array java.nio.file.attribute.FileAttribute 0)))]
    (try
      ;; The observation port is not under test. Do not read live repositories;
      ;; the literal replay receipt also exercises the real readers separately.
      (with-redefs [observation/check-path-exists (constantly {:status :missing})
                    observation/check-decl-in-file (constantly {:status :missing})]
        (let [result (task/complete! (str root) {:status :captured :dispatch dispatch}
                                    expected
                                    (assoc fixture :dispatch-route :fresh-author
                                      :commit (get-in fixture [:artifact-binding :commit])) jobs)
              claim (edn/read-string (slurp (get-in result [:source :path])))]
          (is (= :fresh-author (:route claim)))
          (is (true? (get-in claim [:artifact-binding :fresh-author?])))
          (is (= :request-changes (execution/review-verdict (:review-job claim))))
          ;; Preserve the real next refusal; never turn a rejected run green.
          (is (= :selected-enacted-correspondence-invalid
                 (get-in result [:verification :kind])))))
      (finally
        (doseq [f (reverse (file-seq root))] (io/delete-file f true))))))

(deftest hostile-git-routing-environment-cannot-escape-fixture-repository
  (let [dir (.toFile (Files/createTempDirectory
                      "d-task-git-isolation-"
                      (make-array java.nio.file.attribute.FileAttribute 0)))
        probe (io/file dir "probe")
        fixture (io/file dir "fixture")]
    (try
      (.mkdirs probe)
      (.mkdirs fixture)
      (git! probe "init" "-q")
      (git! probe "config" "user.name" "Probe owner")
      (git! probe "config" "user.email" "probe@example.invalid")
      (spit (io/file probe "probe.txt") "unchanged\n")
      (git! probe "add" "probe.txt")
      (git! probe "-c" "user.name=Probe owner"
            "-c" "user.email=probe@example.invalid"
            "commit" "-qm" "probe base")
      (let [probe-head (git! probe "rev-parse" "HEAD")
            hostile-env (assoc (into {} (System/getenv))
                               "GIT_DIR" (str (io/file probe ".git"))
                               "GIT_WORK_TREE" (str probe))]
        (binding [*git-environment* hostile-env]
          (git! fixture "init" "-q")
          (is (.isDirectory (io/file fixture ".git")))
          (spit (io/file fixture "fixture.txt") "isolated\n")
          (git! fixture "add" "fixture.txt")
          (git! fixture "-c" "user.name=D fixture"
                "-c" "user.email=d-fixture@example.invalid"
                "commit" "-qm" "fixture commit")
          (is (seq (git! fixture "rev-parse" "HEAD"))))
        (is (= "Probe owner" (git! probe "config" "user.name")))
        (is (= "probe@example.invalid" (git! probe "config" "user.email")))
        (is (= probe-head (git! probe "rev-parse" "HEAD"))))
      (finally
        (doseq [file (reverse (file-seq dir))]
          (io/delete-file file true))))))
