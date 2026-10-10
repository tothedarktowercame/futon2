(ns futon2.aif.prefix-free-energy-cold-start-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.policy-prefix-admission :as admission]
            [futon2.aif.prefix-free-energy-cold-start :as sut]))

(def candidate {:id :c1 :target "M-a" :precedence [{:id :p/a}]})
(def candidate2 {:id :c2 :target "M-b" :precedence [{:id :p/b}]})
(def policy (admission/candidate-key candidate))
(def law-bytes (.getBytes "calibration-law-v1" "UTF-8"))
(def update-bytes (.getBytes "update-law-v1" "UTF-8"))
(def law-pin {:path "laws/prefix-f.clj" :revision "r1" :sha256 (evidence/sha256 law-bytes)})
(def update-pin {:path "laws/update.clj" :revision "r1" :sha256 (evidence/sha256 update-bytes)})
(def captured {[(:path law-pin) (:revision law-pin)] law-bytes
               [(:path update-pin) (:revision update-pin)] update-bytes})
(defn c [id content] (sut/seal {:id id :content content}))
(def authority-id "prior-author")
(def registry-id "law-registry")
(def executable-registry-id "executable-registry")
(def update-id "update-author")
(defn terminal-job [job-id agent result]
  (let [j {:job-id job-id :agent-id agent :state "done" :result result}]
    (assoc j :result-digest (evidence/value-digest j))))
(defn auth-ref [job] {:job-id (:job-id job) :authority (:agent-id job)
                      :result-digest (:result-digest job)})

(defn fixture
  ([] (fixture candidate))
  ([cand]
   (let [key (admission/candidate-key cand)
         params (c "params" {:alpha 1.0}) distribution (c "dist" {:family :declared})
         ev (c "evidence" {:source "calibration"}) ledger (c "ledger" {:entries ["evidence"]})
         rule (c "rule" {:relation :fixture-update})
         prior0 {:schema :wm/prefix-f-bootstrap-prior-v1 :policy-key key
                 :calibration-law-id "law-1" :model "model-1" :version "v1"
                 :parameters params :distribution distribution :evidence ev
                 :calibration-epoch "epoch-1" :ledger ledger :update-rule rule
                 :rationale "external calibration"}
         prior-job0 (terminal-job "prior-job" authority-id
                                  {:schema :wm/prefix-f-bootstrap-prior-v1 :prior prior0})
         prior (sut/seal (assoc prior0 :authority (auth-ref prior-job0)))
         law0 {:id "law-1" :model "model-1" :version "v1" :authority "law-author"
               :evaluator-id :fixture-evaluator :evaluator-version "v1" :source-pin law-pin
               :implementation-digest (:sha256 law-pin)}
         registry-job (terminal-job "registry-job" registry-id
                                    {:schema :wm/calibration-law-authorization-v1 :law law0})
         law (assoc law0 :registry-authority (auth-ref registry-job))
         descriptor (sut/seal {:id "fixture-evaluator-registration"
                               :kind :calibration-evaluator :executable-id :fixture-evaluator
                               :version "v1" :implementation-digest (:sha256 law-pin)})
         executable-job (terminal-job "evaluator-registry-job" executable-registry-id
                                      {:schema :wm/executable-registry-authorization-v1
                                       :descriptor descriptor})
         executable (assoc (sut/registered-executable descriptor (fn [_] 1.25))
                           :authority (auth-ref executable-job))
         context {:captured-sources captured
                  :authority-jobs {"prior-job" prior-job0 "registry-job" registry-job
                                   "evaluator-registry-job" executable-job}
                  :law-registry {"law-1" law}
                  :executable-registry {:fixture-evaluator executable}}]
     {:request {:policy key :candidate cand :raw-records [] :bootstrap-prior prior
                :scorer-authority "scorer"}
      :context context})))
(defn refusal [request context]
  (try (sut/evaluate! request context) nil
       (catch clojure.lang.ExceptionInfo e (:prefix-f-cold-start/refusal (ex-data e)))))
(defn reseal-prior [p] (sut/seal (dissoc p :digest)))
(defn step [key click f & [s-prev q]]
  {:step {:status :present :policy-key key :occurrence {:flight "f" :click click}
          :s-prev {:value (or s-prev {#{} 1})} :q (or q {#{} 1}) :f f}
   :path "captured" :sha256 "captured"})

(deftest empirical-and-bootstrap-routes
  (let [{:keys [request context]} (fixture)
        bootstrap (sut/evaluate! request context)
        empirical (sut/evaluate! (assoc request :raw-records [(step policy "1" 0.5)]) context)]
    (is (= [:bootstrap 1.25] [(:route bootstrap) (:f bootstrap)]))
    (is (string? (get-in bootstrap [:law :executable-registration-digest])))
    (is (= [:empirical 0.5] [(:route empirical) (:f empirical)]))))

(deftest bootstrap-refusals
  (let [{:keys [request context]} (fixture)]
    (is (= :unseen-without-bootstrap (refusal (dissoc request :bootstrap-prior) context)))
    (is (= :prior-policy-mismatch
           (refusal (update request :bootstrap-prior #(reseal-prior (assoc % :policy-key [:wrong]))) context)))
    (is (= :self-authored-bootstrap
           (let [p (assoc-in (get request :bootstrap-prior) [:authority :authority] "scorer")]
             (refusal (assoc request :bootstrap-prior (reseal-prior p)) context))))
    (is (= :self-authored-calibration-law
           (refusal request (assoc-in context [:law-registry "law-1" :authority] "scorer"))))
    (is (= :source-digest-mismatch
           (refusal request (assoc-in context [:captured-sources [(:path law-pin) (:revision law-pin)]]
                                      (.getBytes "substituted" "UTF-8")))))
    (is (= :authority-payload-mismatch
           (refusal request (assoc-in context [:law-registry "law-1" :evaluator-id]
                                      :substituted-evaluator))))
    (is (= :executable-registration-mismatch
           (refusal request (assoc-in context [:executable-registry :fixture-evaluator :executable]
                                      (fn [_] -999.0)))))
    (is (= :supplied-f-forbidden
           (refusal (update request :bootstrap-prior #(reseal-prior (assoc % :supplied-f -999))) context)))
    (is (= :bootstrap-f-nonfinite
           (let [entry (get-in context [:executable-registry :fixture-evaluator])
                 replacement (with-meta (fn [_] ##Inf) (meta (:executable entry)))]
             (refusal request (assoc-in context [:executable-registry :fixture-evaluator :executable]
                                        replacement)))))
    (doseq [k [:ledger :evidence]]
      (let [p (update (get request :bootstrap-prior) k #(sut/seal (assoc-in % [:content :stale] true)))]
        (is (= :authority-payload-mismatch
               (refusal (assoc request :bootstrap-prior (reseal-prior p)) context)))))))

(deftest malformed-or-contradictory-never-falls-through
  (let [{:keys [request context]} (fixture)
        broken [(step policy "1" 0.2 {#{} 1} {#{:x} 1})
                (step policy "2" 0.3 {#{:wrong} 1} {#{:y} 1})]
        contradiction [(step policy "1" :contradiction)]]
    (is (= :malformed-empirical-history (refusal (assoc request :raw-records broken) context)))
    (is (= :contradictory-empirical-history
           (refusal (assoc request :raw-records contradiction) context)))))

(defn transition-fixture []
  (let [before (c "before" {:parameters {:a 1} :ledger-digest "l1" :epoch "e1" :evidence-id "old"})
        after (c "after" {:parameters {:a 1} :ledger-digest "l2" :epoch "e2"})
        new-evidence (c "new" {:occurrence "run-1"}) rule (c "rule" {:relation :fixture-update})
        impl {:relation-id :fixture-update :version "v1" :source-pin update-pin
              :implementation-digest (:sha256 update-pin)}
        t0 {:before before :after after :new-evidence new-evidence :update-implementation impl
            :raw-records [(step policy "1" 0.5)] :policy policy :candidate candidate :update-rule rule}
        job (terminal-job "update-job" update-id
                          {:schema :wm/prefix-f-calibration-update-v1 :transition t0})
        t (assoc t0 :authority (auth-ref job))
        descriptor (sut/seal {:id "fixture-update-registration"
                              :kind :calibration-update-relation :executable-id :fixture-update
                              :version "v1" :implementation-digest (:sha256 update-pin)})
        registry-job (terminal-job "update-registry-job" executable-registry-id
                                   {:schema :wm/executable-registry-authorization-v1
                                    :descriptor descriptor})
        executable (assoc (sut/registered-executable descriptor (fn [_] true))
                          :authority (auth-ref registry-job))
        ctx {:captured-sources captured
             :authority-jobs {"update-job" job "update-registry-job" registry-job}
             :executable-registry {:fixture-update executable} :scorer-authority "scorer"}]
    [t ctx]))
(defn reauthorize-transition [t ctx]
  (let [job (terminal-job "update-job" update-id
                          {:schema :wm/prefix-f-calibration-update-v1
                           :transition (dissoc t :authority)})]
    [(assoc t :authority (auth-ref job)) (assoc-in ctx [:authority-jobs "update-job"] job)]))
(defn transition-refusal [t ctx]
  (try (sut/validate-transition! t ctx) nil
       (catch clojure.lang.ExceptionInfo e (:prefix-f-cold-start/refusal (ex-data e)))))

(deftest calibration-transition-falsifiers
  (let [[t ctx] (transition-fixture)]
    (let [validated (sut/validate-transition! t ctx)]
      (is (= :valid (:status validated)))
      (is (string? (:executable-registration-digest validated))))
    (is (= :epoch-not-advanced
           (let [[t* ctx*] (reauthorize-transition
                            (update t :after #(sut/seal (assoc-in % [:content :epoch] "e1"))) ctx)]
             (transition-refusal t* ctx*))))
    (is (= :ledger-not-advanced
           (let [[t* ctx*] (reauthorize-transition
                            (update t :after #(sut/seal (assoc-in % [:content :ledger-digest] "l1"))) ctx)]
             (transition-refusal t* ctx*))))
    (is (= :old-evidence-reused
           (let [[t* ctx*] (reauthorize-transition
                            (assoc t :new-evidence (c "old" {:occurrence "run-1"})) ctx)]
             (transition-refusal t* ctx*))))
    (is (= :executable-registration-mismatch
           (transition-refusal
            t
            (assoc-in ctx [:executable-registry :fixture-update :executable] (fn [_] true)))))))

(deftest multiple-policy-route-value-alignment
  (let [a (fixture candidate) b (fixture candidate2)
        rb (assoc (:request b) :raw-records [(step (get-in b [:request :policy]) "b" 0.75)])
        rows (sut/evaluate-menu! [(:request a) rb] (:context a))]
    (is (= [[(get-in a [:request :policy]) :bootstrap 1.25]
            [(get-in b [:request :policy]) :empirical 0.75]]
           (mapv (juxt :policy :route :f) rows)))))
