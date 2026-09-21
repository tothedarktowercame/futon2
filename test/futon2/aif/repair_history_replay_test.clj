(ns futon2.aif.repair-history-replay-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.interpretation-evidence :as digest]
            [futon2.aif.observation-checks :as observation]
            [futon2.aif.repair-discharge :as discharge]
            [futon2.aif.repair-discharge-evidence :as evidence]
            [futon2.aif.repair-discharge-test :as fixture]
            [futon2.aif.repair-evaluators :as evaluators]
            [futon2.aif.repair-obligation :as repair]))

(def repo "/home/joe/code/futon2")
(def finding-id "repair-occ-f9f4e9708737cb15163f92d0d31419aae2097c5095ffdf6b5c4a3dc08d7057ba")
(def finding-path (str repo "/data/wm-repair-obligations/findings/" finding-id ".edn"))
(def poison-path (str repo "/data/wm-quarantine/machinery-67-attempt-001/002-selection.edn"))

(deftest recorded-input-through-real-evaluator-store-and-publication
  ;; Integration fixture, not a live discharge: Agency jobs and outer A/B
  ;; identities are constructed. Replay, immutable store APIs, isolated git
  ;; publication and C3 are real; the production finding is never modified.
  (let [{:keys [root base] receipt-repo :repo} (fixture/fixture)
        finding (evidence/snapshot finding-path)
        input {:finding-sha256 (:sha256 finding)
               :recorded-locator (get-in finding [:value :failure-data :poisoned-artifact])
               :path poison-path :sha256 (evidence/sha256 (evidence/bytes-at poison-path))}
        code (fixture/git repo "rev-parse" "7fef1200^{commit}")
        _ (spit (io/file root "findings" (str finding-id ".edn")) (:edn-text finding))
        _ (doseq [path [evaluators/driver-path evaluators/launcher-path]]
            (io/make-parents (io/file receipt-repo path))
            (fixture/commit! receipt-repo path (slurp (io/file repo path))))
        source (fixture/git receipt-repo "rev-parse" "HEAD")
        admission {:schema :wm/repair-evaluator-admission-v1 :failure-kind :initialization-failed
                   :evaluator :history-artifact-read :author "evaluator-author" :reviewer "evaluator-reviewer"
                   :review-job "evaluator-job" :source-sha source
                   :source-paths [evaluators/driver-path evaluators/launcher-path]
                   :inputs {finding-id input}}
        admission-sha (fixture/commit! receipt-repo "admission.edn" (pr-str admission))
        jobs (assoc (:read-job base) "evaluator-job"
                    (fixture/job "evaluator-job" "evaluator-reviewer"
                                 (str "REPAIR_EVALUATOR_ADMISSION_SHA256: "
                                      (digest/value-digest (dissoc admission :review-job)))))
        base (assoc base :action {:type :repair-machine-failure :target finding-id
                                 :repair-obligation (:value finding)}
                         :registry evaluators/registry :read-job jobs
                         :evaluators {:initialization-failed {:repo receipt-repo :sha admission-sha :path "admission.edn"}}
                         :artifact {:repo repo :commit code}
                         :artifact-binding (assoc (:artifact-binding base) :repo repo :commit code))
        a (discharge/finalize! (fixture/attempt base "A" "2026-09-21T00:00:01Z"))
        b (discharge/finalize! (fixture/attempt base "B" "2026-09-21T00:00:02Z"))]
    (is (= :awaiting-successor (:status a)) (pr-str a))
    (is (= :receipt-committed (:status b)) (pr-str b))
    (is (true? (:observed (observation/check-path-exists (:c3-locator b)))))
    (is (= :successor-validation (get-in (repair/discharge-record root "resolutions" finding-id)
                                         [:value :repair/phase])))
    (is (= (:sha256 input) (evidence/sha256 (evidence/bytes-at poison-path))))
    (is (= (:edn-text finding) (slurp finding-path)))))

(deftest unrelated-readable-input-cannot-prove-the-repair
  (let [finding (evidence/snapshot finding-path)
        healthy (io/file (fixture/tmp) "healthy.edn")
        _ (spit healthy "{:this-is-readable true}")
        request {:finding (:value finding) :finding-pin finding
                 :close {:attempt/id "B" :run/id "fixture-B"}
                 :artifact {:repo repo :commit (fixture/git repo "rev-parse" "7fef1200^{commit}")}
                 :admission {:inputs {finding-id {:finding-sha256 (:sha256 finding)
                                                  :recorded-locator (get-in finding [:value :failure-data :poisoned-artifact])
                                                  :path (.getPath healthy)
                                                  :sha256 (evidence/sha256 (evidence/bytes-at healthy))}}}}
        observation (evaluators/history-artifact-read! request)]
    (is (false? (get-in observation [:recorded-failure :reproduced-before?])))
    (is (false? (get-in observation [:successor :passed?])))))
