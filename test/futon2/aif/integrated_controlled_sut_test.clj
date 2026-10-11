(ns futon2.aif.integrated-controlled-sut-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-efe-sut-test :as efe-fixture]
            [futon2.aif.controlled-run-persistence-test :as persist-fixture]
            [futon2.aif.integrated-controlled-sut :as sut]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.open-cascade-relatedness-sut-test :as revision-fixture]
            [futon2.aif.outer-objective-sut-test :as outer-fixture]
            [futon2.aif.pattern-registry :as registry]
            [futon2.aif.q8-frontier-certificate :as q8]
            [wm-check-run-facts :as checker]
            [wm-run-facts :as run-facts]))

(use-fixtures :once outer-fixture/isolated-store)

(def target "controlled-3halves")

(defn- sentinel [paths]
  (into {} (map (fn [path]
                  (let [f (io/file path)]
                    [(.getCanonicalPath f)
                     {:exists (.exists f) :modified (when (.exists f) (.lastModified f))
                      :length (when (.isFile f) (.length f))}])) paths)))

(defn- target-snapshot []
  (-> (outer-fixture/snapshot 10)
      (assoc-in [:graph :clusters] [{:mission target}])
      (assoc-in [:graph :tickets :items] [{:stem target :mtime-ms 10}])))

(defn- outer-input []
  {:sealed-registry outer-fixture/sealed
   :registration outer-fixture/public-registration
   :selector-authority 502 :snapshot (target-snapshot)
   :enumerate-fn #(vector (assoc (first (outer-fixture/enumerate-isolated)) :id target))})

(defn- retarget-revision [input original]
  (let [old revision-fixture/target
        sources (:sources input)
        retarget-map (fn [m] {target (get m old)})
        prior (let [x {:id "prior" :content original}]
                (assoc x :digest (evidence/value-digest x)))]
    (-> input
        (assoc :sources (-> sources
                            (update :universes retarget-map)
                            (update :wants retarget-map)
                            (update :locators retarget-map)
                            (update :interpretations retarget-map)))
        (assoc-in [:revision-context :target] target)
        (assoc-in [:revision-context :original] original)
        (assoc-in [:revision-context :construction] {:selected-action original})
        (assoc-in [:revision-context :whole-mission :target] target)
        (assoc-in [:revision-context :blocker :target] target)
        (assoc :prior-cascade prior)
        (assoc :blocker (let [x {:id "blocker" :content
                                 (assoc revision-fixture/blocker :target target)}]
                          (assoc x :digest (evidence/value-digest x)))))))

(defn- job [id authority result]
  (let [j {:job-id id :agent-id authority :state "done" :result result}]
    (assoc j :result-digest (evidence/value-digest j))))

(defn- checkpoint-write [dir closed]
  (let [path (io/file dir "selection.edn")
        value {:schema :wm/controlled-selection-checkpoint-v1
               :status :closed :run-id (:run-id closed)
               :certificates (get-in closed [:result :controlled-sut :certificates])}]
    (io/make-parents path) (spit path (pr-str value))
    {:path (.getCanonicalPath path) :value (edn/read-string (slurp path))}))

(defn fixture []
  (let [efe-input (efe-fixture/fixture-data)
        authority "controlled-test-agency"
        calls (atom [])
        dispatch-fn (fn [request]
                      (swap! calls conj request)
                      (job (str "dispatch-" (:attempt request)) authority
                           (cond-> {:request-digest (evidence/value-digest request)}
                             (= 1 (:attempt request))
                             (assoc :policy-key (:policy-key request) :status :blocked)
                             (= 2 (:attempt request))
                             (assoc :action-sha256 (:action-sha256 request)
                                    :observation {:status :verified-pattern-use}))))
        root (persist-fixture/temp-root)
        paths (persist-fixture/paths root)
        trace-output {:belief {:schema :controlled-integrated-belief}
                      :observation {:controlled true}
                      :free-energy {:controller-score 0}
                      :cascade-problems {:problems [] :refusals []}
                      :decision {:status :controlled} :mode :controlled-sut}]
    {:mode :controlled-sut :outer-input (outer-input) :efe-input efe-input
     :dispatch-authority authority :dispatch-fn dispatch-fn :dispatch-calls calls
     :revision-input-fn (fn [original _]
                          (retarget-revision (revision-fixture/input :admissible) original))
     :grounding-fn (fn [request]
                     {:schema :wm/controlled-grounding-v1 :status :verified
                      :request-digest (evidence/value-digest request)
                      :original-identity (:original-identity request)
                      :revised-identity (:revised-identity request)
                      :artifact-commit "0123456789abcdef0123456789abcdef01234567"})
     :persistence-input
     {:result {:checkpoints {:selection {:judgment {}}}}
      :closed-run {:schema :wm/closed-run-v1 :run-id "integrated-controlled-sut"
                   :started-at "2026-10-11T00:00:00Z"
                   :trace-output trace-output :trace-date "2026-10-11"
                   :brief-item {:attempt-id "integrated-controlled-sut"
                                :outcome :controlled-sut-closed
                                :achievement {:summary "seven-seam controlled SUT"}}
                   :checkpoint-write checkpoint-write}
      :options {:sut-root root :paths paths
                :writer-opts (persist-fixture/writer-opts root)}}}))

(deftest all-seven-seams-revise-close-and-hydrate
  (let [events (atom []) cwd (System/getProperty "user.dir")
        head-before (:out (shell/sh "git" "rev-parse" "HEAD"))
        status-before (:out (shell/sh "git" "status" "--porcelain"))
        production-paths ["data/wm-traces" "data/wm-run-records" "data/morning-brief"]
        production-before (sentinel production-paths)
        input (assoc (fixture) :phase-hook #(swap! events conj (:phase %)))
        result (binding [registry/*pattern-library-root* revision-fixture/library-root]
                 (sut/run-controlled! input))
        persisted (:persistence result)
        certs (get-in result [:closed :certificates])]
    (is (= :persisted (:status result)) (pr-str result))
    (is (= [:selection :construction :admission :scoring :dispatch :revision
            :dispatch :grounding :close] @events))
    (is (= [1 2] (mapv :attempt @(:dispatch-calls input))))
    (is (= (:construction result) (get-in result [:scoring :order])))
    (is (= (get-in result [:scoring :identity-chain :selected])
           (get-in @(:dispatch-calls input) [0 :policy-key])))
    (is (= (get-in result [:revision :dispatch :dispatched-action-sha256])
           (get-in @(:dispatch-calls input) [1 :action-sha256])))
    (is (= 8 (count certs)))
    (is (= :wm/q8-frontier-certificate-v1 (get-in certs [:q8 :schema])))
    (is (= certs (get-in persisted [:checkpoint :value :certificates])))
    (is (= certs (get-in persisted [:hydrated :run-record :controlled-sut :certificates])))
    (is (= certs (get-in persisted [:hydrated :morning-brief
                                    :controlled-sut :certificates])))
    (let [projection-input {:outer-objective (:selection result)
                            :efe-input (:efe-input input)
                            :construction (:construction result)
                            :scored (:scoring result)}
          stale (assoc-in projection-input
                          [:outer-objective :frontier :refresh :current 0
                           :entry :freshness-epoch] 99)
          wrong-target (assoc-in projection-input [:outer-objective :selected-task]
                                 "another-target")
          omitted-scored (update-in projection-input [:scored :preference :scored] pop)]
      (is (= (:q8 certs) (:certificate (q8/project projection-input))))
      (is (= :outer-refresh-invalid (first (:errors (q8/project stale)))))
      (is (some #{:selected-target-mismatch} (:errors (q8/project wrong-target))))
      (is (some #{:target-local-policy-population-mismatch}
                (:errors (q8/project omitted-scored)))))
    (let [snap {:open-missions #{target} :open-excursions #{} :open-tickets #{}
                :patterns #{} :seats #{} :pins {:controlled true}}
          exported (:facts (run-facts/facts-for-record
                            (get-in persisted [:hydrated :run-record])
                            "controlled-run-record" snap nil nil))
          source (checker/lean-source {"facts" exported} "integratedQ8")]
      (is (= (:q8 certs)
             {:schema :wm/q8-frontier-certificate-v1
              :frontier-refresh (exported "frontierRefresh")
              :frontier-selection (exported "frontierSelection")
              :selected-cascade (exported "selectedCascade")}))
      (is (.contains source "q8 := .known")))
    (is (= "integrated-controlled-sut"
           (get-in persisted [:hydrated :morning-brief :attempt-id])))
    (is (= {:status :absent :reason :writer-not-in-futon2}
           (:report-card persisted)))
    (is (pos? (get-in result [:telemetry :total-ms])))
    (is (map? (get-in result [:telemetry :phase-ms])))
    (is (every? pos? (vals (get-in result [:telemetry :artifact-bytes]))))
    (is (= head-before (:out (shell/sh "git" "rev-parse" "HEAD"))))
    (is (= status-before (:out (shell/sh "git" "status" "--porcelain"))))
    (is (= production-before (sentinel production-paths)))
    (is (= cwd (System/getProperty "user.dir")))))

(deftest each-seam-refuses-at-its-own-phase
  (let [base (fixture)
        cases [[(assoc base :outer-input {}) :selection]
               [(update-in base [:efe-input :order-control] dissoc :policy) :construction]
               [(assoc-in base [:efe-input :order-control :application-certificate :application-rows]
                          [[1 0 0] [0 1 0] [2 2 0]]) :construction]
               [(assoc-in base [:efe-input :prefix-input :candidates] []) :scoring]
               [(assoc-in base [:efe-input :preference-input :carrier :weights]
                          {:focused 0.55}) :scoring]
               [(update-in base [:efe-input :predictive :transitions] dissoc :Q) :scoring]
               [(assoc base :dispatch-fn
                       (fn [request]
                         (job "copied" (:dispatch-authority base)
                              {:request-digest "copied-from-another-request"
                               :policy-key (:policy-key request)}))) :dispatch]
               [(assoc base :revision-input-fn
                       (fn [original _dispatch]
                         (assoc (retarget-revision (revision-fixture/input :admissible)
                                                   original)
                                :judgments []))) :revision]
               [(assoc base :grounding-fn
                       (fn [_] {:schema :wm/controlled-grounding-v1 :status :verified
                                :request-digest "foreign-revision"
                                :artifact-commit "0123456789abcdef0123456789abcdef01234567"}))
                :grounding]]]
    (binding [registry/*pattern-library-root* revision-fixture/library-root]
      (doseq [[input phase] cases]
        (is (= phase (:phase (sut/run-controlled! input))))))))
