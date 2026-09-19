(ns futon2.aif.cascade-evaluation-trace-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.cascade-evaluation-trace :as trace]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.efe :as efe]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.policy :as policy])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores)

(defn pattern [id needs produces theta]
  {:id id :produces produces :theta theta
   :guard {:status :interpreted
           :clauses [{:status :interpreted :present needs :absent #{}}]}})

(def p0 (pattern :p0 #{:seed} #{:x} 1/2))
(def p1 (pattern :p1 #{} #{:y} 1))
(def p2 (pattern :p2 #{:seed} #{:z} 1))
(def precedence [p0 p1 p2])
(def q0 {#{:seed} 1/2 #{} 1/2})
(def candidate {:kind :cascade-candidate :id :C0 :target :alpha :precedence precedence})
(def trace-path [:decision :selection-certificate :node-evaluation-traces])

(defn sample-record []
  {:decision {:selection-certificate
              {:candidates [{:id candidate}]
               :node-evaluation-traces
               [{:id candidate :status :recorded :horizon 2
                 :evaluations (:evaluations (m/rollout-evaluation (constantly precedence) q0 2))}]}}})

(defn error-kinds [record]
  (set (map :kind (:errors (trace/validate-record record)))))

(deftest actual-evaluations-not-a-second-replay
  (let [guards (atom []) kernels (atom [])
        original-guard m/guard-holds? original-kernel m/pattern-kernel
        result (with-redefs [m/guard-holds? (fn [p s] (swap! guards conj [(:id p) s])
                                            (original-guard p s))
                             m/pattern-kernel (fn [p s] (swap! kernels conj [(:id p) s])
                                               (original-kernel p s))]
                 (m/rollout-evaluation (constantly precedence) q0 2))
        rows (mapcat :states (:evaluations result))]
    (is (= (count @guards) (reduce + (map (comp count :guard-search) rows))))
    (is (= @guards (vec (for [row rows search (:guard-search row)]
                         [(:pattern-id search) (:state row)]))))
    (is (= @kernels (vec (for [row rows :when (some? (:selected-index row))]
                          [(:pattern-id row) (:state row)]))))
    (is (= {#{:seed :x :y} 1/4 #{:seed :x} 1/8 #{:seed} 1/8 #{:y} 1/2}
           (:belief result)))
    (is (= (:belief result) (m/rollout (constantly precedence) q0 2)))
    (is (= :valid (:status (trace/validate-record (sample-record)))))
    (let [first-step (first (:evaluations result))
          row (some #(when (= #{:seed} (:state %)) %) (:states first-step))]
      (is (= {#{:seed :x} 1/2 #{:seed} 1/2} (:kernel row)))
      (is (= {#{:seed :x} 1/4 #{:seed} 1/4} (:mass-contribution row)))
      (is (= 0 (:selected-index row)))
      (is (= 1 (count (:guard-search row)))))))

(deftest corruption-controls
  (let [record (sample-record)
        step (conj trace-path 0 :evaluations 0)
        state-index (first (keep-indexed #(when (= #{:seed} (:state %2)) %1)
                                         (:states (get-in record step))))
        row (conj step :states state-index)]
    (testing "mass contributions must reconstruct outgoing belief"
      (is (contains? (error-kinds (assoc-in record (conj step :outgoing-belief) {#{} 1}))
                     :outgoing-contributions-mismatch)))
    (testing "contributions themselves must be weighted applied kernels"
      (is (contains? (error-kinds (assoc-in record (conj row :mass-contribution) {#{} 1/2}))
                     :state-contribution-mismatch)))
    (testing "a locally valid next step with the wrong incoming belief breaks the link"
      (let [other (assoc (first (:evaluations (m/rollout-evaluation (constantly precedence) {#{} 1} 1))) :tau 2)
            broken (assoc-in record (conj trace-path 0 :evaluations 1) other)]
        (is (contains? (error-kinds broken) :broken-horizon-link))))
    (testing "the selected pattern must belong to the full candidate"
      (is (contains? (error-kinds (assoc-in record (conj row :pattern-id) :outsider)) :wrong-pattern-id)))
    (testing "membership alone is insufficient: a later enabled pattern is wrong"
      (let [broken (-> record
                       (assoc-in (conj row :selected-index) 1)
                       (assoc-in (conj row :pattern-id) :p1)
                       (assoc-in (conj row :kernel) {#{:seed :y} 1}))]
        (is (contains? (error-kinds broken) :not-first-enabled))
        (is (contains? (error-kinds broken) :applied-kernel-mismatch))))
    (testing "unselected guard failures may not claim an applied transition"
      (let [empty-index (first (keep-indexed #(when (= #{} (:state %2)) %1)
                                           (:states (get-in record step))))
            broken (assoc-in record (conj step :states empty-index :guard-search 0 :applied?) true)]
        (is (contains? (error-kinds broken) :guard-search-mismatch))))
    (testing "dropping or weakening the full-candidate join fails"
      (is (contains? (error-kinds (update-in record [:decision :selection-certificate]
                                            dissoc :node-evaluation-traces)) :candidate-join-mismatch))
      (is (contains? (error-kinds (assoc-in record (conj trace-path 0 :id) :C0)) :candidate-join-mismatch)))
    (is (contains? (error-kinds (update-in record (conj trace-path 0 :evaluations) pop)) :horizon-coverage))
    (is (contains? (error-kinds (assoc-in record (conj step :model :precedence 0 :theta) 1))
                   :model-candidate-mismatch))))

(deftest identity-defaults-and-refusals
  (let [p (dissoc p1 :theta)
        r (m/rollout-evaluation (constantly [p]) {#{} 1} 2)
        second-row (get-in r [:evaluations 1 :states 0])]
    (is (= 1 (get-in r [:evaluations 0 :model :precedence 0 :theta])))
    (is (= :documented-default (get-in r [:evaluations 0 :model :precedence 0 :theta-source])))
    (is (= :identity (:kernel-kind second-row)))
    (is (nil? (:selected-index second-row)))
    (is (= {#{:y} 1} (:kernel second-row)))
    (is (every? (comp false? :applied?) (:guard-search second-row))))
  (doseq [bad [(assoc p1 :theta 2) (assoc p1 :guard {:status :missing})]]
    (let [r (m/rollout-evaluation (constantly [bad]) {#{} 1} 3)]
      (is (= (m/rollout (constantly [bad]) {#{} 1} 3) (:belief r)))
      (is (= 1 (count (:evaluations r))))
      (is (= :refused (get-in r [:evaluations 0 :status]))))))

(def fixture (edn/read-string (slurp "test/fixtures/observation-model/tick-001.edn")))

(defn fixture-candidates []
  ;; Deliberately repeat nested labels across targets. Full maps stay unique.
  (vec (for [target [:alpha :beta] c (:candidates fixture)] (assoc c :target target))))

(deftest scored-predictions-and-joins
  (let [candidates (fixture-candidates)
        rates (zipmap (:universe fixture) (repeat {:false-neg 1/10 :false-pos 1/100}))]
    (doseq [extra [{} {:adjudication-rates rates}
                  {:observation-model
                   {:schema :wm/observation-model-v1 :backend :exact-enumeration
                    :kind :coupled-judgement :universe (:universe fixture)
                    :provenance {:status :synthetic :calibrated false :source "evaluation trace fixture"}
                    :components [{:id :good :weight 17/20
                                  :rates (update-vals rates #(assoc % :false-neg 1/34))}
                                 {:id :bad :weight 3/20
                                  :rates (update-vals rates #(assoc % :false-neg 1/2))}]}
                   :prediction-context {:occurrence-id :trace-test :tau 3}
                   :observation {:status :observed :occurrence-id :trace-test :tau 3
                                 :present (:universe fixture) :absent #{}}}]]
      ;; The bounded scorer already requires unique local IDs within its
      ;; single family. Preserve that admission check; repeated local IDs
      ;; across targets are exercised on the ordinary joint-menu path.
      (let [family (if (:observation-model extra) (:candidates fixture) candidates)
            ranked (efe/rank-actions {:cascade-belief (:q0 fixture)} family
                                     (merge {:horizon-steps 3 :cascade-spec (:spec fixture)} extra))]
        (is (vector? ranked))
        (is (= (count family) (count ranked)))
        (doseq [entry ranked
                :let [evaluations (get-in entry [:certificate :node-evaluations])]]
          (is (= 3 (count evaluations)))
          (is (= (:q0 fixture) (:incoming-belief (first evaluations))))
          (is (= (:belief (m/rollout-evaluation (constantly (get-in entry [:action :precedence]))
                                                (:q0 fixture) 3))
                 (:outgoing-belief (last evaluations))))
          (when (empty? extra)
            (is (< (Math/abs (- (:controller-score entry) (get-in fixture [:expected-g (:cascade-id entry)]))) 1e-9)))
          (when-let [q-steps (get-in entry [:certificate :consumed-g :Q :steps])]
            (is (= (mapv :belief q-steps) (mapv :outgoing-belief evaluations)))))))))

(deftest actual-run-writer-retains-trace
  (let [dir (.toFile (Files/createTempDirectory "evaluation-run-record-" (make-array FileAttribute 0)))
        candidates (fixture-candidates)
        ranked (efe/rank-actions {:cascade-belief (:q0 fixture)} candidates
                                 {:horizon-steps 3 :cascade-spec (:spec fixture)})
        decision (policy/select-action-cascades ranked {:beta 1 :cascade-habit-path (str (io/file dir "absent-habits.edn"))})
        result {:outcome :offline-selection-replay
                :checkpoints {:selection {:judgment {:controller-decision decision}}}}]
    (try
      (let [written (#'runner/persist-run-record! {:run-record-dir (.getPath dir)}
                                                 "evaluation-trace-test" "2026-09-19T00:00:00Z" result)
            record (edn/read-string (slurp (:run-record written)))
            cert (get-in record [:decision :selection-certificate])
            traces (:node-evaluation-traces cert)]
        (is (= :valid (:status (trace/validate-record record))))
        (is (= (mapv :id (:candidates cert)) (mapv :id traces)))
        (is (= (set (keys (get-in record [:decision :selection-law :posterior]))) (set (map :id traces))))
        (is (= 4 (count (set (map (comp :id :id) traces)))))
        (is (= 8 (count (set (map :id traces)))))
        (is (= (mapv #(get-in % [:certificate :node-evaluations]) ranked) (mapv :evaluations traces)))
        (is (= (mapv :id (get-in record [:decision :g-term-decomposition :policies])) (mapv :id traces)))
        (println "TEST-RUN-RECORD" (pr-str record)))
      (finally
        (doseq [f (reverse (file-seq dir))] (io/delete-file f))))))
