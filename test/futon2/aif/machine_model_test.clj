(ns futon2.aif.machine-model-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
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

(deftest float-carried-row-admission-v1-1
  ;; Contract v1.1: declared order-independent numeric criterion.
  (let [one-ulp-off {:a 0.10532904980883244 :b 0.14086253396643547
                     :c 0.31240050666821867 :d 0.09555013225308848
                     :e 0.07447381184188043 :f 0.1660549156527121
                     :g 0.10532904980883244}]
    (is (= :float-carried (m/row-sum-admission one-ulp-off))
        "real production row summing one ulp off 1 admits as float-carried")
    (is (= :float-carried (m/row-sum-admission (into {} (reverse one-ulp-off))))
        "admission is order-independent")
    (is (nil? (m/row-sum-admission (assoc one-ulp-off :a 0.104)))
        "a genuinely unnormalized float row refuses")
    (is (= :exact (m/row-sum-admission {:a 1/2 :b 1/2})))
    (is (nil? (m/row-sum-admission {:a 1/2 :b 1/3}))
        "exact rows keep the exact ==1 requirement")))

(def numeric-receipt-root
  "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-numeric-1/")

(deftest detailed-admission-agrees-with-independent-fractions
  (let [cases (edn/read-string (slurp (str numeric-receipt-root "cases.edn")))
        independent (edn/read-string (slurp (str numeric-receipt-root "independent.edn")))
        expected {:exact-thirds :exact :exact-nonunit nil
                  :float-seven :float-carried :float-unit :float-carried
                  :float-inside :float-carried :float-outside nil
                  :mixed-ratio-float :float-carried :mixed-decimal-float :float-carried
                  :mixed-ratio-float-outside nil :mixed-decimal-float-outside nil
                  :decimal-outside nil
                  :decimal-unit :float-carried :decimal-near :float-carried
                  :mixed-exact-unit :float-carried :mixed-exact-near :float-carried
                  :decimal-coercion-crosses-bound nil :mixed-five-sixths nil
                  :decimal-at-bound :float-carried}]
    (doseq [[name row] cases]
      (testing (str name)
        (let [result (m/numeric-row-admission row)
              reversed (m/numeric-row-admission (into (array-map) (reverse row)))]
          (is (= row (:values result)))
          (is (= result reversed))
          (is (= result (edn/read-string (pr-str result))))
          (is (= (get-in independent [name :total]) (:exact-total result)))
          (is (= (get-in independent [name :deviation]) (:exact-deviation result)))
          (is (= (zero? (get-in independent [name :deviation])) (:exactly-normalized? result)))
          (is (= (get expected name) (:admission result) (m/row-sum-admission row)))
          (when (not= :exact-rational (:representation result))
            (is (= {:id :absolute-row-sum :revision "v1.1" :target 1
                    :max-absolute-deviation 1/1000000000000} (:criterion result)))))))))

(deftest representation-cases-and-stable-float-evidence
  (doseq [[row representation types]
          [[{:a 1 :b 0N} :exact-rational {:a :integer :b :integer}]
           [{:a 1/3 :b 2/3} :exact-rational {:a :ratio :b :ratio}]
           [{:a 0.1M :b 0.9M} :exact-decimal {:a :decimal :b :decimal}]
           [{:a 1/3 :b 1/6 :c 0.5M} :mixed-exact {:a :ratio :b :ratio :c :decimal}]
           [{:a 0.25 :b 0.75} :ieee-floating {:a :float64 :b :float64}]
           [{:a 1/2 :b 0.5} :mixed-floating {:a :ratio :b :float64}]]]
    (let [result (m/numeric-row-admission row)]
      (is (= representation (:representation result)))
      (is (= types (:representations result)))
      (is (:exactly-normalized? result))))
  (let [f (float 0.1) row {:a f :b (- 1.0 (double f))}
        result (m/numeric-row-admission row)]
    (is (:ok result))
    (is (= :float-carried (:admission result)))
    (is (= 1 (:exact-total result)))
    (is (= 0 (:exact-deviation result)))
    (is (= {:a :float32 :b :float64} (:representations result)))
    (is (= row (:values result)))
    (is (instance? Float (:a row)) "caller row retains its original representation")
    (is (= result (edn/read-string (pr-str result))))
    (is (not= row (edn/read-string (pr-str row)))
        "control demonstrates why raw Float is unsuitable as retained EDN evidence"))
  (let [row {:a Long/MAX_VALUE :b Long/MAX_VALUE} result (m/numeric-row-admission row)]
    (is (= 18446744073709551614N (:exact-total result)))
    (is (= :unnormalized-row (get-in result [:refusal :kind])))))

(deftest shared-refusals-and-wrapper-projection
  (doseq [[v kind] [[Double/NaN :invalid-mass] [Double/POSITIVE_INFINITY :invalid-mass]
                    [Float/NEGATIVE_INFINITY :invalid-mass] [-1 :invalid-mass]
                    [(java.util.concurrent.atomic.AtomicInteger. 1) :unsupported-numeric-type]
                    [nil :unsupported-numeric-type] ["1" :unsupported-numeric-type]]]
    (let [result (m/numeric-row-admission {:a v})]
      (is (= kind (get-in result [:refusal :kind])))
      (is (= result (edn/read-string (pr-str result))))
      (is (nil? (m/row-sum-admission {:a v})))))
  (let [calls (atom [])]
    (with-redefs [m/numeric-row-admission (fn [row] (swap! calls conj row) {:admission :from-shared-result})]
      (is (= :from-shared-result (m/row-sum-admission {:a 1})))
      (is (= [{:a 1}] @calls)))))

(deftest full-model-numeric-and-support-boundary
  (with-example
    (fn [x]
      (let [row (get-in x [:belief :posteriors "mission/example"])]
        (doseq [[kind r]
                [[:distribution-support-mismatch (assoc (dissoc row :spawned) :foreign 1/7)]
                 [:distribution-support-mismatch (assoc row :extra 0)]
                 [:invalid-mass (assoc row :spawned -1/7 :refined 3/7)]
                 [:invalid-mass (assoc row :spawned Double/NaN)]
                 [:invalid-mass (assoc row :spawned Double/POSITIVE_INFINITY)]
                 [:invalid-mass (assoc row :spawned Double/NEGATIVE_INFINITY)]
                 [:unsupported-numeric-type (assoc row :spawned (java.util.concurrent.atomic.AtomicInteger. 0))]]]
          (is (= kind (get-in (m/validate (assoc-in x [:belief :posteriors "mission/example"] r))
                             [:refusal :kind]))))
        (is (:ok (m/validate (update x :state-support #(vec (reverse %))))))
        (is (= row (get-in x [:belief :posteriors "mission/example"])))))))

(deftest codex-28-representation-and-criterion-ruling
  (doseq [[row total normalized? representation legacy]
          [[{:a 0.1M :b 0.9M} 1 true :exact-decimal :float-carried]
           [{:a 0.1M :b 0.9000000000005M} 2000000000001/2000000000000
            false :exact-decimal :float-carried]
           [{:a 0.01M :b 0.990000000001000001M} 1000000000001000001/1000000000000000000
            false :exact-decimal nil]
           [{:a 1/3 :b 0.5M} 5/6 false :mixed-exact nil]
           [{:a 0.5 :b 0.5} 1 true :ieee-floating :float-carried]]]
    (let [result (m/numeric-row-admission row)]
      (is (= row (:values result)))
      (is (= total (:exact-total result)))
      (is (= (abs (- total 1)) (:exact-deviation result)))
      (is (= normalized? (:exactly-normalized? result)))
      (is (= representation (:representation result)))
      (is (= legacy (:admission result) (m/row-sum-admission row)))
      (is (= {:id :absolute-row-sum :revision "v1.1" :target 1
              :max-absolute-deviation 1/1000000000000} (:criterion result)))))
  (let [row {:a 0.01M :b 0.990000000001000001M}
        ;; Independent retained old-coercion total from Python Fraction.
        old-total 576460752303999931/576460752303423488
        result (m/numeric-row-admission row)]
    (is (<= (abs (- old-total 1)) 1/1000000000000))
    (is (> (:exact-deviation result) 1/1000000000000))
    (is (= :unnormalized-row (get-in result [:refusal :kind]))))
  (let [at-bound (m/numeric-row-admission {:a 0.01M :b 0.990000000001M})]
    (is (= 1/1000000000000 (:exact-deviation at-bound)))
    (is (= :float-carried (:admission at-bound)))
    (is (false? (:exactly-normalized? at-bound)))))

(deftest shared-distribution-support-contract
  (doseq [[support kind] [[[:a :a] :duplicate-support]
                         [nil :missing-support] [[] :missing-support]
                         [(list :a) :missing-support] [#{:a} :missing-support]
                         [42 :missing-support]]]
    (is (= kind (get-in (m/distribution-admission {:a 1} support) [:refusal :kind]))))
  (doseq [row [{} {:a 1 :outside 0}]]
    (is (= :distribution-support-mismatch
           (get-in (m/distribution-admission row [:a]) [:refusal :kind]))))
  (let [row {:a 1/4 :b 3/4}
        forward (m/distribution-admission row [:a :b])
        reverse-support (m/distribution-admission row [:b :a])
        swapped-values (m/distribution-admission {:a 3/4 :b 1/4} [:a :b])]
    (is (:ok (m/distribution-admission {:a 1} [:a])))
    (is (:ok (m/distribution-admission (zipmap b/status-set (repeat 1/7)) (vec b/status-set))))
    (is (:ok forward))
    (is (:ok reverse-support))
    (is (= [:b :a] (:support reverse-support)))
    (is (= row (:values forward) (:values reverse-support)))
    (is (not= (:values forward) (:values swapped-values)))
    (doseq [result [forward reverse-support swapped-values]]
      (is (and (vector? (:support result)) (seq (:support result))))
      (is (= (count (:support result)) (count (set (:support result)))))
      (is (= (set (:support result)) (set (keys (:values result))))))))

(deftest full-model-uses-the-common-support-shape-law
  (with-example
    (fn [x]
      (let [support (:state-support x)]
        (doseq [[s kind] [[(conj support (first support)) :duplicate-support]
                           [[] :missing-support] [(apply list support) :missing-support]
                           [(set support) :missing-support]
                           [nil :missing-field]]]
          (is (= kind (get-in (m/validate (assoc x :state-support s)) [:refusal :kind]))))
        (is (:ok (m/validate x)))))))
