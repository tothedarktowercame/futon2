(ns futon2.aif.revision-scanner-test
  (:require [clojure.test :refer [deftest is]] [clojure.java.io :as io]
            [clojure.java.shell :as sh] [futon2.aif.action-identity :as identity]))
(try (require 'futon2.aif.revision-scanner) (catch java.io.FileNotFoundException _ nil))
(defn scan [c]
  (when-let [n (find-ns 'futon2.aif.revision-scanner)] ((ns-resolve n 'scan) c)))
(def token ["target" :effect])
(def action {:kind :cascade-candidate :target "target" :precedence [{:id :pattern}]})
(defn surprise [id occurrence at]
  {:schema :wm/surprise-v1 :surprise/id id :model-part :B-effect :token token
   :occurrence {:action/id occurrence :action/value action}
   :expectation {:id (str "expectation-" id) :digest id :rule :positive-marginal-support
                 :scope {:target "target"} :declared-at at}
   :observation {:observed-at at}})
(defn command [repo & args]
  (let [result (apply sh/sh "git" "-C" repo args)]
    (assert (zero? (:exit result)) (:err result)) (:out result)))
(defn commit! [repo message at]
  (command repo "add" ".")
  (let [r (sh/sh "git" "-C" repo "-c" "user.name=Test" "-c" "user.email=test@example.invalid"
                 "commit" "-qm" message :env (assoc (into {} (System/getenv))
                                                     "GIT_AUTHOR_DATE" at "GIT_COMMITTER_DATE" at))]
    (assert (zero? (:exit r)) (:err r))))
(defn with-records [f]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory "revision-scanner" (make-array java.nio.file.attribute.FileAttribute 0)))
        repo (.getPath (io/file root "repo")) path "model.clj"
        surprises (.getPath (io/file root "surprises.edn")) run (.getPath (io/file root "run.edn"))]
    (try
      (.mkdirs (io/file repo)) (command repo "init" "-q")
      (spit (io/file repo path) "(ns model)\n(def effect false)\n")
      (commit! repo "base" "2026-09-01T00:00:00Z")
      (spit surprises (pr-str [(surprise "s1" "a1" "2026-09-02T00:00:00Z")]))
      (spit (io/file repo path) "(ns model)\n(def effect true)\n")
      (commit! repo "Revise effect\n\nSurprise: s1\nSurprise: nonexistent" "2026-09-03T00:00:00Z")
      (let [prediction {:status :frozen :prediction-rule :positive-marginal-support :target "target"
                        :action action :wanted [{:token token :predicted 1}]
                        :rollout {:belief {#{token} 1}} :intended-outputs #{token}}
            source {:identity-kind :source-digest-at-namespace-load
                    :namespaces {'model {:canonical-path (str repo "/" path)
                                         :loaded-source {:status :captured :captured-at "2026-09-04T00:00:00Z"
                                                         :sha256 (identity/sha256 (slurp (io/file repo path)))}}}}
            receipt {:run/id "later" :startedAt "2026-09-05T00:00:00Z"
                     :runner/source source :token-outcome-prediction prediction}
            config {:repos [repo] :surprise-files [surprises] :run-files []
                    :bindings [{:repo repo :path path :namespace 'model :model-part :B-effect}]
                    :as-of "2026-09-07T00:00:00Z" :unanswered-after-seconds 86400}]
        (f {:config config :run run :receipt receipt :surprises surprises}))
      (finally (doseq [p (reverse (file-seq root))] (io/delete-file p true))))))

(deftest trailer-and-grades-require-real-git-and-receipts
  (with-records
    (fn [{:keys [config run receipt]}]
      (let [result (scan config)]
        (is (= 1 (count (:learning-event-candidates result))))
        (is (= :committed (get-in result [:learning-event-candidates 0 :grade])))
        (is (= [:committed] (get-in result [:learning-event-candidates 0 :attained-grades])))
        (is (= :unknown-surprise-id (get-in result [:flags 0 :kind]))))
      (spit run (pr-str (dissoc receipt :token-outcome-prediction)))
      (is (= :loaded (get-in (scan (assoc config :run-files [run])) [:learning-event-candidates 0 :grade])))
      (spit run (pr-str (assoc-in receipt [:token-outcome-prediction :target] "unrelated")))
      (is (= :untested (get-in (scan (assoc config :run-files [run])) [:learning-event-candidates 0 :grade])))
      (spit run (pr-str receipt))
      (let [result (scan (assoc config :run-files [run]))]
        (is (= :consumed (get-in result [:learning-event-candidates 0 :grade])))
        (is (= [:committed :loaded :consumed] (get-in result [:learning-event-candidates 0 :attained-grades]))))
      ;; The actual older tick receipt shape: evaluated per-policy Q, with
      ;; declared wants. No newly manufactured token prediction is required.
      (let [a (assoc-in action [:precedence 0 :produces] #{token})
            tick (-> receipt (dissoc :token-outcome-prediction)
                     (assoc :decision
                            {:g-term-decomposition
                             {:policies [{:id a :terms {:Q {:status :present
                                                           :value {:steps [{:belief {#{token} 1}}]}}}}]}
                             :selection-certificate {:token-belief-stage
                                                     {:domain-inputs [{:target "target" :declaration {:want [:effect]}}]}}}))]
        (spit run (pr-str tick))
        (is (= :consumed (get-in (scan (assoc config :run-files [run])) [:learning-event-candidates 0 :grade]))))
      (spit run (pr-str (assoc-in receipt [:runner/source :namespaces 'model :loaded-source :sha256] "wrong")))
      (is (= :committed (get-in (scan (assoc config :run-files [run])) [:learning-event-candidates 0 :grade]))))))

(deftest recurrence-is-a-class-and-unanswered-is-visible
  (with-records
    (fn [{:keys [config surprises]}]
      (spit surprises (pr-str [(surprise "s1" "a1" "2026-09-02T00:00:00Z")
                              (surprise "s2" "a2" "2026-09-05T00:00:00Z")]))
      (let [r (scan config) friction (:friction-candidates r)]
        (is (= #{:recurring-after-revision :unanswered} (set (map :kind friction))))
        (is (= #{"s2"} (set (map :surprise/id friction)))))
      (let [r (scan (assoc config :as-of "2026-09-05T01:00:00Z"))]
        (is (not-any? #(= :unanswered (:kind %)) (:friction-candidates r)))))))
