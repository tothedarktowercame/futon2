(ns futon2.aif.repair-discharge-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.interpretation-evidence :as digest]
            [futon2.aif.repair-discharge :as discharge]
            [futon2.aif.repair-discharge-evidence :as evidence]
            [futon2.aif.repair-discharge-receipt :as receipt]
            [futon2.aif.repair-obligation :as repair])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn tmp [] (.getPath (.toFile (Files/createTempDirectory "repair-stage-test-" (make-array FileAttribute 0)))))
(defn git [repo & args]
  (let [r (apply shell/sh "git" "-C" repo args)]
    (when-not (zero? (:exit r)) (throw (ex-info "fixture git failed" r)))
    (str/trim (:out r))))
(defn commit! [repo file text]
  (spit (io/file repo file) text)
  (git repo "add" "--" file)
  (git repo "commit" "-m" file)
  (git repo "rev-parse" "HEAD"))
(defn job [id agent prompt]
  {:job-id id :agent-id agent :state "done" :result "FULL_LOOP_REVIEW: APPROVE"
   :execution {:executed true :tool-events 1} :events [{:type "prompt" :text prompt}]})

(defn fixture []
  (let [root (tmp) repo (tmp)
        _ (git repo "init") _ (git repo "config" "user.email" "fixture@example.invalid")
        _ (git repo "config" "user.name" "fixture")
        source "fixture evaluator bytes\n"
        code (commit! repo "evaluator.clj" source)
        finding {:repair/id "repair-fixture" :repair/schema-version 3 :repair/class :machine-failure
                 :repair/status :open :attempt-id "failed" :failed-commit "bad"
                 :failure-kind :fixture-kind :machine-repo repo
                 :discharge-contract {:artifact-shape :code-commit
                                      :requires [:distinct-repair-commit :independent-review :grounded-repair
                                                 :distinct-production-shaped-successor]}}
        _ (io/make-parents (io/file root "findings/repair-fixture.edn"))
        _ (spit (io/file root "findings/repair-fixture.edn") (pr-str finding))
        admission {:schema :wm/repair-evaluator-admission-v1 :failure-kind :fixture-kind
                   :evaluator :fixture :author "evaluator-author" :reviewer "evaluator-reviewer"
                   :review-job "evaluator-job" :source-sha code :source-paths ["evaluator.clj"]}
        admission-sha (commit! repo "admission.edn" (pr-str admission))
        jobs {"evaluator-job" (job "evaluator-job" "evaluator-reviewer"
                                   (str "REPAIR_EVALUATOR_ADMISSION_SHA256: "
                                        (digest/value-digest (dissoc admission :review-job))))
              "review-A" (job "review-A" "reviewer" "fixture A")
              "review-B" (job "review-B" "reviewer" "fixture B")}
        evaluate (fn [{:keys [finding finding-pin close artifact]}]
                   {:schema :wm/repair-observation-v1
                    :identity {:repair/id (:repair/id finding) :finding-sha256 (:sha256 finding-pin)
                               :attempt/id (:attempt/id close) :run/id (:run/id close) :artifact-commit (:commit artifact)}
                    :recorded-failure {:reproduced-before? true :reproduced-after? false :evidence [:fixture]}
                    :successor {:production-shaped? true :passed? true :evidence [:fixture]}})
        base {:root root :repo repo
              :action {:type :repair-machine-failure :target "repair-fixture" :repair-obligation finding}
              :files ["evaluator.clj"] :author "author" :reviewer "reviewer" :read-job jobs
              :evaluators {:fixture-kind {:repo repo :sha admission-sha :path "admission.edn"}}
              :registry {:fixture {:loaded-source {"evaluator.clj" (evidence/sha256 (evidence/text-bytes source))}
                                   :evaluate evaluate}}
              :artifact {:repo repo :commit code}
              :artifact-binding {:repo repo :commit code :fresh-author? true :descendant? true
                                 :in-author-window? true :corroborates? true :disagreement? false}
              :producer {:runner-source-sha256 "fixture"}}]
    {:root root :repo repo :base base :finding finding}))
(defn attempt [base id timestamp]
  (assoc base :close {:attempt/id id :run/id (str "run-" id) :closed-at timestamp
                     :grounded? true :close-snapshot {:fixture true}}
             :review-job {:job-id (str "review-" id)}))

(deftest two-phases-and-crash-regeneration
  ;; Constructed evaluator authority tests store/publication mechanics. This is
  ;; NOT the production-kind replay acceptance; that has a separate test.
  (let [{:keys [root repo base]} (fixture)
        a (attempt base "A" "2026-09-21T00:00:01Z")
        b (attempt base "B" "2026-09-21T00:00:02Z")]
    (is (= :awaiting-successor (:status (discharge/finalize! a))))
    (is (= :implementation (get-in (repair/discharge-record root "implementations" "repair-fixture")
                                   [:value :repair/phase])))
    (is (nil? (repair/discharge-record root "resolutions" "repair-fixture")))
    (is (thrown-with-msg? AssertionError #"simulated process death"
                         (discharge/finalize! (assoc b :after-resolution-fn
                                                     #(throw (AssertionError. "simulated process death"))))))
    (let [expected (evidence/canonical-text (receipt/derive root "repair-fixture"))
          before (repair/discharge-record root "resolutions" "repair-fixture")
          _ (spit (io/file repo "unrelated") "leave staged")
          _ (git repo "add" "unrelated")
          recovered (first (receipt/catch-up! root repo))]
      (is (= :receipt-committed (:status recovered)) (pr-str recovered))
      (is (= expected (slurp (io/file repo (receipt/receipt-path "repair-fixture")))))
      (is (= before (repair/discharge-record root "resolutions" "repair-fixture")))
      (is (= "unrelated" (git repo "diff" "--cached" "--name-only")))
      (is (= :receipt-committed (:status (first (receipt/catch-up! root repo)))))
      (is (thrown? Exception (receipt/verify! root "repair-fixture" (pr-str {:status :resolved})))))))

(deftest missing-evaluator-rejected-review-and-real-store-refusal
  (doseq [[alter expected]
          [[#(assoc % :evaluators {}) :evidence-unavailable]
           [#(assoc % :read-job (assoc (:read-job %) "review-A"
                                      (assoc (get (:read-job %) "review-A") :result "FULL_LOOP_REVIEW: REJECT")))
            :review-not-approved]
           [#(assoc % :artifact-binding (assoc (:artifact-binding %) :fresh-author? false)) :store-refused]]]
    (let [{:keys [root repo base]} (fixture)
          head (git repo "rev-parse" "HEAD")
          result (discharge/finalize! (alter (attempt base "A" "2026-09-21T00:00:01Z")))]
      (is (= expected (:status result)) (pr-str result))
      (is (nil? (repair/discharge-record root "implementations" "repair-fixture")))
      (is (nil? (repair/discharge-record root "resolutions" "repair-fixture")))
      (is (= head (git repo "rev-parse" "HEAD"))))))

(deftest evidence-binding-controls
  (doseq [[alter reason]
          [[#(assoc-in % [:action :target] "different") :finding-binding-mismatch]
           [#(assoc-in % [:registry :fixture :loaded-source "evaluator.clj"] "wrong") :evaluator-source-mismatch]
           [#(update-in % [:registry :fixture :evaluate]
                         (fn [f] (fn [x] (assoc-in (f x) [:recorded-failure :reproduced-after?] true))))
            :repair-observation-not-established]]]
    (let [{:keys [root base]} (fixture)
          result (discharge/finalize! (alter (attempt base "A" "2026-09-21T00:00:01Z")))]
      (is (= reason (:reason result)) (pr-str result))
      (is (nil? (repair/discharge-record root "implementations" "repair-fixture"))))))

(deftest same-run-is-not-a-distinct-successor
  (let [{:keys [root base]} (fixture)
        a (attempt base "A" "2026-09-21T00:00:01Z")
        b (assoc-in (attempt base "B" "2026-09-21T00:00:02Z") [:close :run/id] "run-A")]
    (is (= :awaiting-successor (:status (discharge/finalize! a))))
    (is (= :awaiting-successor (:status (discharge/finalize! a))))
    (let [result (discharge/finalize! b)]
      (is (= :store-refused (:status result)))
      (is (= "Repair successor evidence refused" (:error result)))
      (is (= :self-successor (:repair-successor/refusal (evidence/read-one (:error-data-edn result)))))
      (is (nil? (repair/discharge-record root "resolutions" "repair-fixture"))))))

(deftest t-targets-need-native-pinned-admission
  (let [{:keys [root base finding]} (fixture)
        pin {:path (.getCanonicalPath (io/file root "findings/repair-fixture.edn"))
             :sha256 (:sha256 (repair/discharge-record root "findings" "repair-fixture"))}
        action {:type :cascade-candidate :target "T-repair-fixture" :repair/id "repair-fixture"
                :finding-source pin :discharge-contract (:discharge-contract finding)}
        receipts {:p {:kind :hand-admitted :reading "fixture"}}
        a (assoc (attempt base "A" "2026-09-21T00:00:01Z") :action action :interpretation receipts)]
    (doseq [bad [(dissoc a :interpretation)
                 (assoc-in a [:action :finding-source :sha256] "stale")
                 (update a :action dissoc :repair/id)]]
      (is (= :evidence-unavailable (:status (discharge/finalize! bad))))
      (is (nil? (repair/discharge-record root "implementations" "repair-fixture"))))
    (is (= :awaiting-successor (:status (discharge/finalize! a))))))

(deftest head-movement-and-forged-receipt-refuse
  (let [{:keys [root repo base]} (fixture)
        _ (discharge/finalize! (attempt base "A" "2026-09-21T00:00:01Z"))
        _ (try (discharge/finalize! (assoc (attempt base "B" "2026-09-21T00:00:02Z")
                                         :after-resolution-fn #(throw (AssertionError. "crash"))))
               (catch AssertionError _ nil))
        before (git repo "rev-parse" "HEAD")
        _ (commit! repo "concurrent" "other lane")]
    (is (= :publication-head-moved
           (try (receipt/publish! root repo "repair-fixture" before) nil
                (catch Exception e (:repair-discharge/refusal (ex-data e))))))
    (let [path (io/file repo (receipt/receipt-path "repair-fixture"))]
      (io/make-parents path)
      (spit path "{:status :resolved}")
      (is (= :receipt-worktree-conflict
             (:reason (receipt/publication-result! root repo "repair-fixture"))))
      (is (= "{:status :resolved}" (slurp path))))))
