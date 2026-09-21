(ns futon2.aif.d-predecessor-task-authority-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.close-retention :as retention]
            [futon2.aif.d-predecessor-task-authority :as task]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.observation-checks :as observation]
            [futon2.aif.task-execution-evidence :as execution]
            [futon2.aif.token-belief-predecessor :as predecessor]
            [futon2.aif.trace :as trace]
            [clojure.edn :as edn])
  (:import (java.nio.file Files) (java.time Instant) (java.util UUID)))

(defn git! [repo & args]
  (let [r (apply shell/sh "git" "-C" (str repo) args)]
    (when-not (zero? (:exit r)) (throw (ex-info "fixture git failed" r)))
    (str/trim (:out r))))

(defn with-artifact
  ([f] (with-artifact {} f))
  ([opts f]
  (let [dir (.toFile (Files/createTempDirectory "d-task-authority-" (make-array java.nio.file.attribute.FileAttribute 0)))
        repo (io/file dir "repo") root (io/file dir "claims")]
    (try
      (.mkdirs repo)
      (git! repo "init" "-q")
      (git! repo "config" "user.name" "D fixture")
      (git! repo "config" "user.email" "d-fixture@example.invalid")
      (spit (io/file repo "base.txt") "base\n")
      (git! repo "add" "base.txt")
      (git! repo "commit" "-qm" "before")
      (let [before {:repo (str repo) :head (git! repo "rev-parse" "HEAD")
                    :observed-at-ms (System/currentTimeMillis)}
            action (or (:action opts) {:kind :cascade-candidate :id :C0 :target "target"
                    :precedence [{:id :make-file :theta 1 :produces #{["target" :artifact]}}]})
            occurrence (retention/mint-occurrence
                        {:run-id "run" :cohort-id "cohort" :attempt-id "attempt"
                         :selected-action action :now #(Instant/now) :uuid-fn #(UUID/randomUUID)})
            occurrence ((or (:occurrence-fn opts) identity) occurrence)
            declaration (io/file dir "declaration.edn")
            _ (spit declaration (pr-str (cond-> {:target "target" :locators
                                        {:artifact {:class :C3 :repo "repo" :sha (:head before)
                                                    :path (or (:locator-path opts) "created.clj")}}}
                                          (:locators opts)
                                          (update :locators merge (:locators opts))
                                          (:observation-schedule opts)
                                          (assoc :observation-schedule (:observation-schedule opts)))))
            pins [{:path (str declaration)
                   :sha256 (evidence/sha256 (Files/readAllBytes (.toPath declaration)))}]
            dispatch (task/capture {:occurrence occurrence :carry-occurrence-id "carry"
                                    :universe (or (:universe opts) #{["target" :artifact]}) :declaration-reads pins :before before})
            _ (spit (io/file repo "created.clj") "(ns created)\n")
            _ (git! repo "add" "created.clj")
            _ (git! repo "commit" "-qm" "execute task")
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
                      :universe (or (:universe opts) #{["target" :artifact]}) :declaration-pins pins}]
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
       (is (= :not-established (:candidate-to-minted-join reread)))
       (is (= :not-measured (:before-evidence reread)))
       (is (= #{["target" :artifact]} (:present reread)))
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
        (git! repo "commit" "-qm" "unrelated concurrent work")
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
          (is (= :job-occurrence-binding-unestablished
                 (get-in result [:verification :kind])))))
      (finally
        (doseq [f (reverse (file-seq root))] (io/delete-file f true))))))
