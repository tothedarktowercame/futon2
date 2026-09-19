(ns futon2.aif.machine-preference-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.belief :as belief]
            [futon2.aif.c-fold-config :as loader]
            [futon2.aif.machine-model :as model]
            [futon2.aif.ruled-outcome-c :as c])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.security MessageDigest]))

(def organization-support
  (vec (sort (:support c/seeded-c))))

(defn model-context [registration]
  (let [states (vec (sort belief/status-set))
        outcome (model/outcome-authority)
        outcomes (:support outcome)
        outcome-row (assoc (zipmap outcomes (repeat 0))
                           [:organization :grounded-change] 1)
        state-row (zipmap states (repeat 1/7))
        a {:authority :declared-prior :name "row10-reference-A"
           :rows (zipmap states (repeat outcome-row))}]
    {:schema model/schema :model {:id "wm-row10-test" :revision "v1"}
     :context {:entity/id "mission/row10"} :state-support states
     :belief {:mode :single-entity :posteriors {"mission/row10" state-row}}
     :actions [:inspect]
     :policies [{:id "row10-policy" :revision "v1" :entity/id "mission/row10"
                 :cascade {:id "row10-cascade" :revision "v1" :nodes ["read"]}
                 :actions [:inspect]}]
     :outcome outcome :A a
     :B {:authority :declared-prior :name "row10-reference-B"
         :rows (into {} (for [s states]
                          [[s :inspect] (assoc (zipmap states (repeat 0)) s 1)]))}
     :D state-row :policy-prior {"row10-policy" 1}
     :parameters {:kind :finite-registered-hypotheses
                  :hypotheses [{:id "theta-row10" :revision "v1"
                                :registration registration :likelihood a}]
                  :prior {"theta-row10" 1}}
     :observation-encoding {:id "tagged-organization" :revision "v1"
                            :support outcomes}
     :semantics {:missing-producer :refuse :zero-mass :retain
                 :impossible-outcome :typed-refusal}}))

(defn with-model [f]
  (let [path (Files/createTempFile "row10-registration" ".edn"
                                  (make-array FileAttribute 0))]
    (try
      (let [base (model-context {})]
        (spit (.toFile path)
              (pr-str {:schema :wm/parameter-hypothesis-v1
                       :id "theta-row10" :revision "v1" :likelihood (:A base)}))
        (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                              (Files/readAllBytes path))
              pin {:path (str path)
                   :sha256 (apply str (map #(format "%02x" (bit-and 255 %)) digest))}]
          (f (model-context pin))))
      (finally (Files/deleteIfExists path)))))

(def loader-input
  {:seed c/seeded-c
   :layers c/fold-declaration
   :provenance {:seed {:id :ruled-outcome-c-v1 :revision "fixture"}
                :layers {:revision "fixture"}}})

(deftest full-tagged-preference-construction
  (with-model
    (fn [context]
      (let [result (c/construct-machine-preference context loader-input)]
        (is (:ok result))
        (is (= :unit (:unconditional-on result)))
        (is (= 12 (count (get-in result [:distribution :support]))))
        (is (= 1 (reduce + (vals (get-in result [:distribution :mass])))))
        (is (= 7 (count (:named-zero-outcomes result))))
        (is (= [] (get-in result [:vertices :nouns :support])))
        (is (= [] (get-in result [:vertices :verbs :support])))
        (is (= :owed (get-in result [:vertices :evidence :support])))
        (is (= 1/2 (:mass (c/preference-mass result [:organization :grounded-change]))))))))

(deftest declared-refusal-boundaries
  (with-model
    (fn [context]
      (let [result (c/construct-machine-preference context loader-input)]
        (testing "evidence cannot be consumed"
          (is (= :evidence-vocabulary-owed
                 (get-in (c/require-machine-preference context loader-input)
                         [:refusal :kind]))))
        (testing "a changed layer revision refuses"
          (is (= :preference-layer-revision-mismatch
                 (get-in (c/construct-machine-preference
                          context (update loader-input :layers assoc-in [0 :layer/id] :changed))
                         [:refusal :kind]))))
        (testing "the superseded undeclared c-mis revision refuses"
          (let [old-layers (mapv #(if (= :c-mis (:layer/id %))
                                    (-> %
                                        (assoc :in-ruled-sum :undeclared
                                               :owed "superseded")
                                        (dissoc :composition-law :reason))
                                    %)
                                 c/fold-declaration)]
            (is (= :preference-layer-revision-mismatch
                   (get-in (c/construct-machine-preference
                            context (assoc loader-input :layers old-layers))
                           [:refusal :kind])))))
        (testing "unknown outcomes refuse rather than acquire implicit zero mass"
          (is (= :unknown-preference-outcome
                 (get-in (c/preference-mass result [:organization :unknown])
                         [:refusal :kind]))))
        (testing "Q positive where C is named zero exposes infinite risk"
          (is (= {:kind :positive-prediction-at-zero-preference
                 :path [:distribution :mass [:organization :cancelled]]
                  :risk :infinite}
                 (:refusal (c/unsupported-risk result [:organization :cancelled] 1/8)))))))))

(deftest pinned-production-loader-witness
  (with-model
    (fn [context]
      (let [config "holes/labs/wm-contract/runs/RUN4-F11-production-successor-2026-09-12-v2/run-config.edn"
            loaded (loader/resolve-opts {} config slurp)
            input {:seed (:seeded-c loaded)
                   :layers c/fold-declaration
                   :provenance (:c-fold-provenance loaded)}
            result (c/construct-machine-preference context input)
            masses (get-in result [:distribution :mass])
            expected (into {} (map (fn [[k v]] [[:organization k] v])) c/seeded-positive-masses)
            positives (filter #(pos? (get masses %)) (get-in result [:distribution :support]))]
        (is (:ok result))
        (is (= 12 (count masses)))
        (doseq [[outcome mass] masses]
          (is (= (get expected outcome 0) mass) (str "exact Lean reference mass " outcome)))
        (is (= [:organization :abstained] (first (get-in result [:distribution :support]))))
        (is (= [:organization :agent-unavailable] (first positives)))
        (is (= :preference-layer-revision-mismatch
               (get-in (c/construct-machine-preference
                        context (update input :layers update-in [0 :basis] str "#changed"))
                       [:refusal :kind])))
        (is (= :unknown-preference-outcome
               (get-in (c/preference-mass result [:organization :foreign]) [:refusal :kind])))
        (is (= :infinite
               (get-in (c/unsupported-risk result [:organization :abstained] 1/8)
                       [:refusal :risk])))))))
