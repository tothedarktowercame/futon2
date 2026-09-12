(ns futon2.aif.machine-model-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-model :as m]
            [futon2.aif.belief :as b])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.security MessageDigest]))

(defn example [registration]
  (let [states (vec (sort b/status-set))
        o (m/outcome-authority) support (:support o)
        row (assoc (zipmap support (repeat 0)) [:organization :grounded-change] 1)
        prior (zipmap states (repeat 1/7))
        a {:authority :declared-prior :name "explicit-reference-likelihood"
           :rows (zipmap states (repeat row))}]
    {:schema m/schema :model {:id "registered-test-model" :revision "v1"}
     :context {:entity/id "mission/example"} :state-support states
     :belief {:mode :single-entity :posteriors {"mission/example" prior}}
     :actions [:inspect]
     :policies [{:id "inspect-policy" :revision "v1" :entity/id "mission/example"
                 :cascade {:id "inspect-cascade" :revision "v1" :nodes ["read-evidence"]}
                 :actions [:inspect]}]
     :outcome o :A a
     :B {:authority :declared-prior :name "explicit-identity-B"
         :rows (into {} (for [s states] [[s :inspect] (assoc (zipmap states (repeat 0)) s 1)]))}
     :D prior :policy-prior {"inspect-policy" 1}
     :parameters {:kind :finite-registered-hypotheses
                  :hypotheses [{:id "theta-1" :revision "v1" :registration registration :likelihood a}]
                  :prior {"theta-1" 1}}
     :observation-encoding {:id "tagged-organization" :revision "v1" :support support}
     :semantics {:missing-producer :refuse :zero-mass :retain :impossible-outcome :typed-refusal}}))

(defn with-example [f]
  (let [path (Files/createTempFile "machine-model-registration" ".edn" (make-array FileAttribute 0))
        file (.toFile path)]
    (try
      (spit file (pr-str {:schema :wm/parameter-hypothesis-v1 :id "theta-1" :revision "v1"
                           :likelihood (:A (example {}))}))
      (let [digest (.digest (MessageDigest/getInstance "SHA-256") (Files/readAllBytes path))
            pin {:path (str path) :sha256 (apply str (map #(format "%02x" (bit-and 255 %)) digest))}]
        (f (example pin)))
      (finally (Files/deleteIfExists path)))))

(deftest valid-and-consumer-boundaries
  (with-example
    (fn [x]
      (is (:ok (m/validate x)))
      (is (:ok (m/require-outcome-vocabulary x :organization)))
      (is (= :evidence-vocabulary-owed (get-in (m/require-outcome-vocabulary x :evidence) [:refusal :kind])))
      (is (= :missing-producer (get-in (m/require-producer {} :Q) [:refusal :kind])))
      (is (= :invalid-producer (get-in (m/require-producer {:Q :gaussian-proxy} :Q) [:refusal :kind])))
      (is (:ok (m/require-producer {:Q (fn [] :explicit)} :Q))))))

(deftest refusal-controls
  (with-example
    (fn [x]
      (doseq [[kind alter]
              [[:missing-field #(dissoc % :A)]
               [:unsupported-schema #(assoc % :schema :future)]
               [:malformed-map #(assoc % :model [])]
               [:missing-identity #(assoc-in % [:model :revision] "")]
               [:missing-entity #(assoc-in % [:context :entity/id] "")]
               [:state-carrier-mismatch #(assoc % :state-support [:spawned])]
               [:invalid-action #(assoc % :actions ["inspect"])]
               [:missing-belief #(assoc-in % [:belief :posteriors] [])]
               [:missing-distribution #(assoc-in % [:belief :posteriors] {})]
               [:unnamed-kernel #(assoc-in % [:A :name] "")]
               [:missing-kernel-rows #(assoc-in % [:A :rows] [])]
               [:encoding-support-mismatch #(assoc-in % [:observation-encoding :support] [])]
               [:measurement-artifact-missing #(assoc-in % [:parameters :hypotheses 0 :registration :path] "/nonexistent-machine-model-artifact")]
               [:missing-support #(assoc % :state-support 3)]
               [:duplicate-support #(update % :state-support conj (first (:state-support %))) ]
               [:unnormalized-row #(assoc-in % [:D :spawned] 2)]
               [:hypothesis-registration-mismatch #(assoc-in % [:parameters :hypotheses 0 :revision] "unregistered")]
               [:invalid-mass #(assoc-in % [:D :spawned] Double/NaN)]
               [:distribution-support-mismatch #(update % :D dissoc :spawned)]
               [:undeclared-authority #(assoc-in % [:A :authority] :guessed)]
               [:measurement-pointer-missing #(assoc-in % [:A :authority] :observed-estimate)]
               [:entity-averaging-forbidden #(assoc-in % [:belief :mode] :average-entities)]
               [:joint-construction-required #(assoc-in % [:policies 0 :entity/id] "other")]
               [:outcome-authority-mismatch #(assoc-in % [:outcome :vertices :evidence] {:status :ruled :carrier #{:invented}})]
               [:fallback-forbidden #(assoc-in % [:semantics :missing-producer] :uniform)]
               [:parameter-representation-excluded #(assoc-in % [:parameters :kind] :continuous-dirichlet)]
               [:kernel-input-mismatch #(assoc-in % [:B :rows] {})]
               [:undeclared-policy-action #(assoc-in % [:policies 0 :actions] [:invented])]
               [:duplicate-support #(update % :policies conj (assoc (first (:policies %)) :revision "v2"))]
               [:measurement-pin-mismatch #(assoc-in % [:parameters :hypotheses 0 :registration :sha256] (apply str (repeat 64 "0")))]]]
        (testing (str kind)
          (is (= kind (get-in (m/validate (alter x)) [:refusal :kind]))))))))

(deftest observed-authority-and-identity-exclusion
  (with-example
    (fn [x]
      (let [path (Files/createTempFile "machine-model-measurement" ".edn" (make-array FileAttribute 0))]
        (try
          (doseq [[key expected] [[:A nil] [:B :identity-b-must-be-declared]]]
            (spit (.toFile path) (pr-str {:schema :wm/kernel-measurement-v1
                                        :kernel/name (get-in x [key :name])
                                        :rows (get-in x [key :rows]) :outcome :measured}))
            (let [digest (.digest (MessageDigest/getInstance "SHA-256") (Files/readAllBytes path))
                  pin {:path (str path) :sha256 (apply str (map #(format "%02x" (bit-and 255 %)) digest))}
                  model (update x key assoc :authority :observed-estimate :measurement pin)]
              (is (= expected (get-in (m/validate model) [:refusal :kind])))
              (is (= :measurement-kernel-mismatch
                     (get-in (m/validate (assoc-in model [key :name] "foreign")) [:refusal :kind])))))
          (finally (Files/deleteIfExists path)))))))
