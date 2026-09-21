(ns futon2.aif.kernel-example-test
  (:require [checks.disposition-kernel :as fit]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.d-predecessor-task-authority :as task]
            [futon2.aif.d-predecessor-task-authority-test :as fixture]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.kernel-example :as example]
            [futon2.aif.ruled-outcome-c :as ruled]
            [futon2.aif.token-outcome :as outcome]
            [futon2.aif.token-outcome-test :as want]))

(defn with-example [f]
  (let [decision (want/decision)
        locators (assoc (into {} (map (fn [[t l]] [t (assoc l :repo "repo")])) (:locators want/declaration))
                        :unobservable {:class :C5 :repo "repo" :sha "HEAD" :path "created.clj"})
        action (assoc (:action decision) :observation-locators
                      (into {} (map (fn [[t l]] [(want/qualify t) l])) locators))]
    (fixture/with-artifact
     {:action action :target want/target :locators locators
      :universe (conj (set (map want/qualify (keys locators))) [want/target :artifact])
      :before-files {"holes/missions/M-aif-policy-conditioned-eig.md"
                     (slurp "holes/missions/M-aif-policy-conditioned-eig.md")}}
     (fn [{:keys [inputs expected jobs commit]}]
       (let [record (task/claim inputs)
             prediction (outcome/freeze-prediction (assoc decision :action action))
             context {:record record :prediction prediction :occurrence (:occurrence expected)
                      :artifact-sha commit :outcome :grounded-change :domain (example/declaration)}]
         (f {:context context :expected expected :jobs jobs
             :signed (task/verify-observations-v2 record expected jobs)}))))))

(deftest real-updater-negative-is-aligned-not-made-successful-by-grounding
  (with-example
    (fn [{:keys [context expected jobs]}]
      (let [result (example/collect context expected jobs)
            rows (into {} (map (juxt :token identity)) (:tokens result))]
        (is (= :recorded (:status result)) (pr-str result))
        (is (= :admitted (get-in result [:observation-projection :status])))
        (is (= [1 false] ((juxt :predicted :observed) (rows want/updater))))
        (is (= :grounded-change (:close-outcome result)))
        (is (= {:status :disposition :value :grounded-change} (:disposition result)))
        (is (= 3 (count rows)))
        (is (= #{[want/target :unobservable]} (get-in result [:missingness :missing-tokens])))
        (is (every? #(= :absent (get-in % [:attestation :status])) (vals rows)))
        (is (= result (edn/read-string (pr-str result))))
        (is (= (:prediction context) (:prediction result)))))))

(deftest failure-attempt-retains-prediction-and-missingness
  (with-example
    (fn [{:keys [context expected jobs]}]
      (let [failed (example/collect (assoc context :record nil :artifact-sha nil :outcome :build-failed)
                                    expected jobs)]
        (is (= :recorded (:status failed)))
        (is (= :build-failed (get-in failed [:disposition :value])))
        (is (= 3 (count (:tokens failed))))
        (is (every? #(= {:status :missing :kind :d-task-record-unavailable} (:observed %)) (:tokens failed)))
        (is (= :absent (get-in failed [:artifact :status]))))
      (is (= :occurrence-mismatch
             (:kind (example/collect context (assoc-in expected [:occurrence :action/id] "other") jobs))))
      (let [failed (example/collect (assoc-in context [:record :route] :incomplete) expected jobs)]
        (is (= :unavailable (get-in failed [:missingness :observations])))
        (is (every? #(= :missing (get-in % [:observed :status])) (:tokens failed)))))))

(deftest unverifiable-execution-keeps-the-disposition
  ;; The verifier's catch-all refusal (e.g. Agency cannot return the author
  ;; job) is not an identity mismatch: observations are unavailable.
  (with-example
    (fn [{:keys [context expected]}]
      (let [unreadable (fn [_] (throw (ex-info "job unavailable" {:status 404})))
            r (example/collect context expected unreadable)]
        (is (= :recorded (:status r)))
        (is (= (:outcome context) (get-in r [:disposition :value])))
        (is (= :unavailable (get-in r [:missingness :observations])))
        (is (every? #(= :missing (get-in % [:observed :status])) (:tokens r)))))))

(deftest wrong-identities-refuse
  (with-example
    (fn [{:keys [context signed]}]
      (let [inputs (assoc context :signed signed)]
        (doseq [[bad kind]
                [[(assoc-in inputs [:signed :occurrence :action/id] "other") :observation-occurrence-mismatch]
                 [(assoc inputs :artifact-sha "other") :observation-artifact-mismatch]
                 [(assoc-in inputs [:signed :observations want/updater :meaning :locator :path] "other") :observation-meaning-mismatch]
                 [(assoc-in inputs [:prediction :observation-locators want/updater :path] "other") :prediction-meaning-mismatch]
                 [(assoc-in inputs [:signed :observations want/updater :artifact-observation :observed] true) :observation-evidence-mismatch]
                 [(assoc-in inputs [:signed :observations want/updater :artifact-observation :artifact-sha] "other") :observation-artifact-mismatch]
                 [(assoc-in inputs [:prediction :action :target] "other") :prediction-occurrence-mismatch]]]
          (is (= kind (:kind (example/align bad)))))))))

(deftest versioned-fourteen-to-twelve-join-keeps-administrative-mass
  (let [domain (example/declaration)
        counts (assoc (zipmap cohort/outcome-kinds (repeat 0))
                      :grounded-change 3 :historical-verification-refused 2
                      :historical-verification-awaiting-validation 1)
        joined (example/join-counts domain counts)]
    (is (= (:support ruled/seeded-c) (set (keys (:dispositions joined)))))
    (is (= [6 3 3] ((juxt :total :disposition-total :administrative-total) joined)))
    (is (= 12 (count (:dispositions joined))))
    (is (= 2 (count (:administrative joined))))
    (doseq [label ruled/non-disposition-outcomes]
      (is (= :not-a-disposition (get-in joined [:mapping :mapping label :status]))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Kernel example refused"
                         (example/join-counts domain (dissoc counts :historical-verification-refused))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Kernel example refused"
                         (example/validate-domain! (update domain :mapping dissoc :historical-verification-refused))))))

(deftest reference-1789964661-negative-updater-grounded-pair
  ;; Read-only retained-evidence replay: real Git/C4/identity checks, retained
  ;; independent job snapshots as the job port. No fresh Agency or store writes.
  (let [record (edn/read-string (slurp "/home/joe/code/futon3c/data/wm-d-task-enactment/action-a5d2326d-a73a-447c-8bc7-7f77c8bbc187.edn"))
        run (edn/read-string (slurp "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-09-21-1789964661.edn"))
        dispatch (:dispatch record) occurrence (:occurrence dispatch)
        action (:action/value occurrence)
        terms (get-in run [:decision :g-term-decomposition :policies 0 :terms])
        decision (-> (:decision run)
                     (assoc :action action)
                     (assoc-in [:selection-certificate :precision-family]
                               {:model-id :historical-retained-scorer-replay
                                :model {:q0 (get-in terms [:D :value])
                                        :horizon (count (get-in terms [:C :value :steps]))}}))
        expected {:occurrence occurrence :carry-occurrence-id (:carry-occurrence-id dispatch)
                  :universe (:universe dispatch)
                  :declaration-pins (mapv #(select-keys % [:path :sha256]) (:declarations dispatch))}
        jobs (into {} (map (juxt :job-id identity)) [(:author-job record) (:review-job record)])
        result (example/collect {:record record :occurrence occurrence
                                 :prediction (outcome/freeze-prediction decision)
                                 :artifact-sha (get-in record [:revision-pair :after])
                                 :outcome :grounded-change :domain (example/declaration)} expected jobs)
        updater (first (filter #(= want/updater (:token %)) (:tokens result)))]
    (is (= :recorded (:status result)) (pr-str result))
    (is (= :admitted (get-in result [:observation-projection :status])))
    (is (= [1 false] ((juxt :predicted :observed) updater)))
    (is (= :grounded-change (get-in result [:disposition :value])))))


(deftest existing-fourteen-wide-fit-has-an-explicit-lossless-join
  (let [kernel (fit/read-kernel fit/default-ledger)
        joined (example/join-counts (example/declaration) (:outcome-counts kernel))]
    (is (= 14 (count (:support kernel))))
    (is (= (:sample-size kernel) (:total joined)))
    (is (= (:total joined) (+ (:disposition-total joined) (:administrative-total joined))))
    (is (= (:support ruled/seeded-c) (set (keys (:dispositions joined)))))))
