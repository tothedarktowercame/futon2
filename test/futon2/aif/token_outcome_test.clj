(ns futon2.aif.token-outcome-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-policy :as policy]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.token-outcome :as outcome])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

;; This fixture describes the original updater-only attempt, paired with the
;; pre-aeb352f8 mission below. Later live source alternatives are not that attempt.
(def declaration
  (edn/read-string (slurp (io/resource "fixtures/eig-source-remaining/updater-only-declaration.edn"))))
(def target (:target declaration))
(defn qualify [t] [target t])
(def updater (qualify :hole/h6378c65a4012))

(defn decision []
  (let [patterns (mapv (fn [[id p]]
                         (policy/token-interpretation
                          id {:guard {:needs (set (map qualify (get-in p [:guard :needs])))
                                      :forbids (set (map qualify (get-in p [:guard :forbids])))}
                              :produces (set (map qualify (:produces p)))}))
                       (:patterns declaration))
        action {:kind :cascade-candidate :id :C1 :target target
                :precedence patterns
                :construction-receipt (get-in declaration [:candidates 0 :construction-receipt])
                :interpretation-receipts (:interpretation-receipts declaration)
                :observation-locators
                (into {} (map (fn [[t l]] [(qualify t) l])) (:locators declaration))}]
    {:action action
     :selection-certificate
     {:precision-family {:model-id "fixture-model" :selected-action action
                         :model {:q0 {#{(qualify :admission/task-stated)} 1} :horizon 1}}
      :token-belief-stage {:domain-inputs [{:target target :declaration declaration}]}}}))

(defn command [repo & args]
  (let [{:keys [exit out err]} (apply shell/sh "git" "-C" (str repo) args)]
    (when-not (zero? exit) (throw (ex-info err {:args args :exit exit})))
    (str/trim out)))

(defn with-artifact [f]
  (let [root (.toFile (Files/createTempDirectory "token-outcome-" (make-array FileAttribute 0)))
        repo (io/file root "futon2")]
    (try
      (.mkdirs repo)
      (command repo "init" "-q")
      (let [mission (io/file repo "holes/missions/M-aif-policy-conditioned-eig.md")]
        (io/make-parents mission)
        ;; Frozen at abde70b9, before the first renewal-4 click's commit
        ;; aeb352f8 ticked the updater hole. Copying the live mission file made
        ;; every "predicted, not observed" fixture depend on today's HEAD.
        (spit mission (slurp "test/fixtures/M-aif-policy-conditioned-eig-pre-aeb352f8.md")))
      (command repo "add" ".")
      (command repo "-c" "user.name=Fixture" "-c" "user.email=fixture@example.invalid"
               "commit" "-qm" "mission before unrelated build")
      (spit (io/file repo "unrelated.clj") "(ns unrelated)\n")
      (command repo "add" ".")
      (command repo "-c" "user.name=Fixture" "-c" "user.email=fixture@example.invalid"
               "commit" "-qm" "reviewed-looking unrelated build")
      (with-redefs [checks/repo-root (.getPath root)]
        (f (command repo "rev-parse" "HEAD")))
      (finally (doseq [file (reverse (file-seq root))] (.delete file))))))

(defn measurements [sha]
  (mapv (fn [[token locator]]
          (let [after (assoc locator :sha sha)]
            {:token (qualify token) :declared-locator locator :after-locator after
             :result (checks/check-decl-in-file after)}))
        (:locators declaration)))

(deftest unrelated-grounded-commit-does-not-produce-the-want
  (with-artifact
    (fn [sha]
      (let [prediction (outcome/freeze-prediction (decision))
            receipt (outcome/compare-outcomes prediction (measurements sha) sha)
            rows (into {} (map (juxt :token identity)) (:tokens receipt))
            close {:outcome :grounded-change :witness {:resolved? true :dial-moved? true}
                   :token-outcome-comparison receipt}]
        (is (= :frozen (:status prediction)))
        (is (= #{updater} (:intended-outputs prediction)))
        (is (= 3 (count rows)))
        (is (= [1 false :predicted-not-observed]
               ((juxt :predicted :observed :verdict) (rows updater))))
        (doseq [token (disj (set (map qualify (:want declaration))) updater)]
          (is (= [0 false :neither] ((juxt :predicted :observed :verdict) (rows token)))))
        (is (= :predicted-not-observed
               (get-in close [:token-outcome-comparison :tokens
                              (.indexOf (mapv :token (:tokens receipt)) updater) :verdict])))
        (testing "unknown Git revision remains missing"
          (let [missing (outcome/compare-outcomes prediction (measurements "not-a-revision") sha)]
            (is (every? #(= :observation-missing (:verdict %)) (:tokens missing)))
            (is (every? #(= :unknown-sha (get-in % [:observed :kind])) (:tokens missing)))))))))

(deftest actual-rollout-not-output-union
  (let [d (decision)
        blocked (assoc-in d [:selection-certificate :precision-family :model :q0] {#{} 1})
        fractional (assoc-in d [:action :precedence 0 :theta] 1/4)]
    (is (every? #(zero? (:predicted %)) (:wanted (outcome/freeze-prediction blocked))))
    (is (= 1/4 (:predicted (first (filter #(= updater (:token %))
                                         (:wanted (outcome/freeze-prediction fractional)))))))
    (is (= :prediction-input-unavailable (:reason (outcome/freeze-prediction {}))))))

(deftest all-verdicts-and-evidence-identity
  (let [prediction {:status :frozen :wanted [{:token :yes :predicted 1/4}
                                            {:token :no :predicted 0}]}
        rows (fn [observed] (mapv #(hash-map :token % :result
                                            {:observed observed :evidence {:resolved-sha "sha"}})
                                 [:yes :no]))
        verdicts #(mapv :verdict (:tokens (outcome/compare-outcomes prediction % "sha")))]
    (is (= [:predicted-and-observed :not-predicted-observed] (verdicts (rows true))))
    (is (= [:predicted-not-observed :neither] (verdicts (rows false))))
    (is (= [:observation-missing :observation-missing] (verdicts [])))
    (is (= [:observation-missing :observation-missing] (verdicts (concat (rows true) (rows true)))))
    (is (every? #(= :artifact-revision-mismatch (:reason %))
                (:tokens (outcome/compare-outcomes prediction (rows true) "other"))))))
