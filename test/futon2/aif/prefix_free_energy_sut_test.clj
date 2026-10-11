(ns futon2.aif.prefix-free-energy-sut-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.policy-prefix-admission :as admission]
            [futon2.aif.prefix-free-energy-cold-start :as cold]
            [futon2.aif.prefix-free-energy-sut :as sut]
            [futon2.aif.wm.cascade-decision :as cascade-decision]))

(def a {:id :a :target "M-a" :precedence [{:id :p/a}]})
(def b {:id :b :target "M-b" :precedence [{:id :p/b}]})
(def ka (admission/candidate-key a))
(def kb (admission/candidate-key b))
(def sut-root (.toFile (java.nio.file.Files/createTempDirectory
                        "prefix-f-sut"
                        (make-array java.nio.file.attribute.FileAttribute 0))))
(def law-bytes (.getBytes "sut-prefix-law" "UTF-8"))
(def update-bytes (.getBytes "sut-update-law" "UTF-8"))
(def law-path (.getAbsolutePath (java.io.File. sut-root "prefix-law.clj")))
(def update-path (.getAbsolutePath (java.io.File. sut-root "update-law.clj")))
(spit law-path "sut-prefix-law")
(spit update-path "sut-update-law")
(def law-pin {:path law-path :revision "r1"
              :sha256 (evidence/sha256 law-bytes)})
(def update-pin {:path update-path :revision "r1"
                 :sha256 (evidence/sha256 update-bytes)})
(def calls (atom 0))

(defn carrier [id content] (cold/seal {:id id :content content}))
(defn job [id agent result]
  (let [j {:job-id id :agent-id agent :state "done" :result result}]
    (assoc j :result-digest (evidence/value-digest j))))
(defn auth-ref [j] {:job-id (:job-id j) :authority (:agent-id j)
               :result-digest (:result-digest j)})

(defn authority-fixture []
  (let [descriptor (cold/seal
                    {:id "eval-registration" :kind :calibration-evaluator
                     :executable-id :sut-evaluator :version "v1"
                     :implementation-digest (:sha256 law-pin)})
        executable-job (job "eval-job" "exec-registry"
                            {:schema :wm/executable-registry-authorization-v1
                             :descriptor descriptor})
        executable (assoc
                    (cold/registered-executable
                     descriptor (fn [_] (swap! calls inc) 1.25))
                    :authority (auth-ref executable-job))
        law0 {:id "law-1" :model "model-1" :version "v1"
              :authority "law-author" :evaluator-id :sut-evaluator
              :evaluator-version "v1" :source-pin law-pin
              :implementation-digest (:sha256 law-pin)}
        law-job (job "law-job" "law-registry"
                     {:schema :wm/calibration-law-authorization-v1 :law law0})
        law (assoc law0 :registry-authority (auth-ref law-job))]
    {:captured-sources
     {[(:path law-pin) (:revision law-pin)]
      (java.nio.file.Files/readAllBytes (.toPath (java.io.File. law-path)))}
     :authority-jobs {"eval-job" executable-job "law-job" law-job}
     :law-registry {"law-1" law}
     :executable-registry {:sut-evaluator executable}}))

(defn prior [policy]
  (let [p0 {:schema :wm/prefix-f-bootstrap-prior-v1 :policy-key policy
            :calibration-law-id "law-1" :model "model-1" :version "v1"
            :parameters (carrier "params" {:alpha 1})
            :distribution (carrier "distribution" {:family :fixture})
            :evidence (carrier "evidence" {:source :sut})
            :calibration-epoch "epoch-1"
            :ledger (carrier "ledger" {:entries []})
            :update-rule (carrier "rule" {:relation :sut-update})
            :rationale "non-production SUT calibration"}
        j (job (str "prior-" (hash policy)) "prior-author"
               {:schema :wm/prefix-f-bootstrap-prior-v1 :prior p0})]
    [(cold/seal (assoc p0 :authority (auth-ref j))) j]))

(defn commissioning-for
  "Controlled external commissioning for one exact candidate PolicyKey.
  Test fixtures use this carrier instead of supplying a numeric F."
  [candidate]
  (let [policy (admission/candidate-key candidate)
        context (authority-fixture)
        [bootstrap-prior prior-job] (prior policy)]
    {:bootstrap-priors {policy bootstrap-prior}
     :scorer-authority "scorer"
     :context (update context :authority-jobs assoc
                      (:job-id prior-job) prior-job)}))

(defn step [policy click f & [s-prev q]]
  {:step {:status :present :policy-key policy
          :occurrence {:flight "sut" :click click}
          :s-prev {:value (or s-prev {#{} 1})} :q (or q {#{} 1}) :f f}
   :path "sut-record" :sha256 "sut-record"})

(defn menu-fixture [records]
  (let [ctx (authority-fixture)
        [pa ja] (prior ka) [pb jb] (prior kb)]
    {:input {:candidates [a b]
             :ranked [{:id :rank-a :action a :g 2.0 :f -999}
                      {:id :rank-b :action b :g 3.0 :f -999}]
             :conditioning-steps {:steps records}
             :bootstrap-priors {ka pa kb pb}
             :scorer-authority "scorer"}
     :context (update ctx :authority-jobs assoc
                      (:job-id ja) ja (:job-id jb) jb)}))

(defn refusal [f]
  (try (f) nil
       (catch clojure.lang.ExceptionInfo e
         (:prefix-f-cold-start/refusal (ex-data e)))))

(deftest real-prefix-menu-routes-once-and-retains-alignment
  (reset! calls 0)
  (let [{:keys [input context]} (menu-fixture [(step ka "1" 0.5)])
        result (sut/compose-menu! input context)]
    (is (= :ready-for-posterior (:status result)))
    (is (= [ka kb] (:policy-keys result)))
    (is (= [:empirical :bootstrap] (mapv :route (:routes result))))
    (is (= [0.5 1.25] (mapv :f (:routes result))))
    (is (= (mapv :f (:routes result))
           (mapv #(get-in % [:f-prefix :f]) (:posterior-input result))))
    (is (= 1 @calls) "only the unseen policy invokes bootstrap once")
    (is (= 3 (count (get-in result [:routes 1 :authority-terminal-records]))))
    (is (= {:candidates [ka kb] :routes [ka kb] :posterior-input [ka kb]}
           (:population-correspondence result)))
    (is (every? #(not (contains? % :f)) (:posterior-input result)))))

(deftest empirical-history-never-consults-bootstrap
  (reset! calls 0)
  (let [{:keys [input context]} (menu-fixture [(step ka "1" 0.5)
                                                (step kb "1" 0.75)])
        result (sut/compose-menu! (assoc input :bootstrap-priors {}) context)]
    (is (= [:empirical :empirical] (mapv :route (:routes result))))
    (is (= 0 @calls))))

(deftest malformed-and-contradictory-history-never-falls-through
  (doseq [[records expected]
          [[[(step ka "1" 0.2 {#{} 1} {#{:x} 1})
             (step ka "2" 0.3 {#{:wrong} 1} {#{:y} 1})]
            :malformed-empirical-history]
           [[(step ka "1" :contradiction)] :contradictory-empirical-history]]]
    (reset! calls 0)
    (let [{:keys [input context]} (menu-fixture records)]
      (is (= expected (refusal #(sut/compose-menu! input context))))
      (is (= 0 @calls)))))

(deftest configuration-and-authority-falsifiers
  (let [{:keys [input context]} (menu-fixture [])]
    (is (= :production-prefix-f-configuration-absent
           (:reason (sut/compose-menu! input {}))))
    (testing "wrong complete PolicyKey"
      (is (= :prior-policy-mismatch
             (refusal #(sut/compose-menu!
                        (assoc-in input [:bootstrap-priors kb]
                                  (get-in input [:bootstrap-priors ka])) context)))))
    (testing "self authorization"
      (is (= :self-authored-calibration-law
             (refusal #(sut/compose-menu!
                        input (assoc-in context [:law-registry "law-1" :authority]
                                        "scorer"))))))
    (testing "stale authority job"
      (is (= :authority-job-not-terminal
             (refusal #(sut/compose-menu!
                        input (assoc-in context [:authority-jobs "law-job" :state]
                                        "running"))))))
    (testing "substituted evaluator"
      (is (= :executable-registration-mismatch
             (refusal #(sut/compose-menu!
                        input (assoc-in context
                                        [:executable-registry :sut-evaluator :executable]
                                        (fn [_] 9)))))))
    (testing "nonfinite output"
      (let [entry (get-in context [:executable-registry :sut-evaluator])
            replacement (with-meta (fn [_] ##Inf) (meta (:executable entry)))]
        (is (= :bootstrap-f-nonfinite
               (refusal #(sut/compose-menu!
                          input (assoc-in context
                                          [:executable-registry :sut-evaluator :executable]
                                          replacement)))))))))

(deftest commissioned-selection-boundary-is-explicit-and-refusal-default
  (reset! calls 0)
  (let [ranked [{:id :rank-a :action a :g 2.0}]
        token-input {:policy-prefixes
                     (admission/prefixes [a]
                                         {:steps []
                                          :dir-status {:absent :fixture}})}
        uncommissioned (#'cascade-decision/prefix-f-posterior-input
                        ranked [a] token-input {})
        commissioned (#'cascade-decision/prefix-f-posterior-input
                      ranked [a] token-input
                      {:prefix-f-commissioning (commissioning-for a)})]
    (is (= :not-supplied
           (get-in uncommissioned [:ranked 0 :f-prefix :status])))
    (is (= :ready-for-posterior (get-in commissioned [:receipt :status])))
    (is (= :bootstrap
           (get-in commissioned [:ranked 0 :f-prefix :route])))
    (is (= 1 @calls) "the commissioned evaluator is invoked exactly once")
    (testing "an invalid commissioning refuses before posterior selection"
      (is (= :self-authored-calibration-law
             (try
               (#'cascade-decision/prefix-f-posterior-input
                ranked [a] token-input
                {:prefix-f-commissioning
                 (assoc-in (commissioning-for a)
                           [:context :law-registry "law-1" :authority]
                           "scorer")})
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:prefix-f-cold-start/refusal (ex-data e)))))))))

(defn transition-fixture [raw-records]
  (let [before (carrier "before" {:ledger-digest "l1" :epoch "e1"
                                   :evidence-id "old"})
        after (carrier "after" {:ledger-digest "l2" :epoch "e2"})
        evidence* (carrier "new" {:occurrence "run-1"})
        rule (carrier "update-rule" {:relation :sut-update})
        impl {:relation-id :sut-update :version "v1" :source-pin update-pin
              :implementation-digest (:sha256 update-pin)}
        t0 {:before before :after after :new-evidence evidence*
            :update-implementation impl :raw-records raw-records
            :policy kb :candidate b :update-rule rule}
        update-job (job "update-job" "update-author"
                        {:schema :wm/prefix-f-calibration-update-v1
                         :transition t0})
        t (assoc t0 :authority (auth-ref update-job))
        descriptor (cold/seal
                    {:id "update-registration" :kind :calibration-update-relation
                     :executable-id :sut-update :version "v1"
                     :implementation-digest (:sha256 update-pin)})
        registry-job (job "update-registry-job" "exec-registry"
                          {:schema :wm/executable-registry-authorization-v1
                           :descriptor descriptor})
        executable (assoc (cold/registered-executable descriptor (fn [_] true))
                          :authority (auth-ref registry-job))]
    [t {:captured-sources
        {[(:path update-pin) (:revision update-pin)]
         (java.nio.file.Files/readAllBytes (.toPath (java.io.File. update-path)))}
        :authority-jobs {"update-job" update-job
                         "update-registry-job" registry-job}
        :executable-registry {:sut-update executable}
        :scorer-authority "scorer"}]))

(deftest certified-transition-retires-bootstrap
  (let [records [(step kb "after" 0.4)]
        [transition transition-context] (transition-fixture records)
        {:keys [input context]} (menu-fixture [])
        request {:policy kb :candidate b :raw-records records
                 :bootstrap-prior (get-in input [:bootstrap-priors kb])
                 :scorer-authority "scorer"}
        result (sut/transition-to-empirical! transition transition-context
                                             request context)]
    (is (= :empirical (:status result)))
    (is (true? (:bootstrap-retired result)))
    (is (= ["e1" "e2"]
           [(get-in result [:transition :before :content :epoch])
            (get-in result [:transition :after :content :epoch])]))
    (is (= :empirical (get-in result [:route :route])))))

(deftest substituted-update-law-refuses
  (let [records [(step kb "after" 0.4)]
        [transition transition-context] (transition-fixture records)
        {:keys [input context]} (menu-fixture [])
        request {:policy kb :candidate b :raw-records records
                 :bootstrap-prior (get-in input [:bootstrap-priors kb])
                 :scorer-authority "scorer"}]
    (is (= :executable-registration-mismatch
           (refusal #(sut/transition-to-empirical!
                      transition
                      (assoc-in transition-context
                                [:executable-registry :sut-update :executable]
                                (fn [_] true))
                      request context))))))
