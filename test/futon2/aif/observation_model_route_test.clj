(ns futon2.aif.observation-model-route-test
  (:require [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.cascade-observation-route :as route]
            [futon2.aif.efe :as efe]
            [futon2.aif.observation-model :as om])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn close? [a b]
  (< (Math/abs (- (double a) (double b))) 1e-9))

(defn rates [universe fn-rate fp-rate]
  (zipmap universe (repeat {:false-neg fn-rate :false-pos fp-rate})))

(defn model [kind universe]
  (merge {:schema :wm/observation-model-v1 :backend :exact-enumeration
          :kind kind :universe universe
          :provenance {:status :synthetic :calibrated false
                       :source "coupling-sidebyside-2026-09-18; declared experiment"}}
         (case kind
           :exact-checks {:rates (rates universe 0 0)}
           :independent-judgement {:rates (rates universe 1/10 1/100)}
           :coupled-judgement {:components [{:id :good-day :weight 17/20
                                            :rates (rates universe 1/34 1/100)}
                                           {:id :bad-day :weight 3/20
                                            :rates (rates universe 1/2 1/100)}]})))

(def context {:occurrence-id "synthetic-occurrence-1" :tau 1})

(defn observation [universe present]
  (merge context {:status :observed :present present :absent (set/difference universe present)}))

(defn query-p [model state event]
  (:probability (om/query model {:op :likelihood :state state :event event})))

(def ten (set (range 10)))
(def five (set (range 5)))

(defn product-preference [universe present]
  (let [states (reduce (fn [ss x] (into ss (map #(conj % x) ss))) [#{}] universe)]
    (into {} (for [o states]
               [o (reduce * (for [v universe
                                  :let [p (if (present v) 3/4 1/20)]]
                              (if (o v) p (- 1 p))))]))))

(deftest joint-events-and-cancellation
  (let [ind (model :independent-judgement ten)
        coup (model :coupled-judgement ten)
        nothing {:present #{} :absent ten}
        all-five-missed {:present #{} :absent five}
        pair-missed {:present #{} :absent #{0 1}}
        p-i (query-p ind five nothing)
        p-c (query-p coup five nothing)
        c (product-preference ten five)
        si (om/query ind {:op :score :belief {five 1} :preference c})
        sc (om/query coup {:op :score :belief {five 1} :preference c})
        ;; At equal marginals, H(product marginals)-H(joint) is TC.
        tc (- (:ambiguity si) (:ambiguity sc))]
    (is (= 1/10 (query-p ind five {:present #{} :absent #{0}})))
    (is (= 1/10 (query-p coup five {:present #{} :absent #{0}})))
    (is (= 1/100 (query-p ind five pair-missed)))
    (is (= (+ (* 17/20 1/34 1/34) (* 3/20 1/2 1/2))
           (query-p coup five pair-missed)))
    (is (= 1/100000 (query-p ind five all-five-missed)))
    (is (= (* (query-p coup five all-five-missed) (reduce * (repeat 5 99/100))) p-c))
    (is (close? p-i 0.000009509900499))
    (is (< 468 (/ p-c p-i) 470))
    (is (close? (:ambiguity si)
                (+ (* 5 (om/entropy {:present 9/10 :absent 1/10}))
                   (* 5 (om/entropy {:present 1/100 :absent 99/100})))))
    (is (< 0.230 tc 0.232))
    (is (close? (- (:risk sc) (:risk si)) tc))
    (is (close? (:g si) (:g sc)))
    (is (= ind (:model si)))
    (println "JOINT-CONTROL" (pr-str {:nothing {:independent p-i :coupled p-c :ratio (/ p-c p-i)}
                                     :pair-missed (query-p coup five pair-missed)
                                     :total-correlation tc :independent-g (:g si) :coupled-g (:g sc)}))))

(deftest coupling-changes-inference
  (let [q {#{} 1/2 five 1/2}
        obs (observation ten #{})
        conditioned (fn [kind]
                      (om/query (model kind ten) {:op :condition :belief q
                                                  :observation obs :context context}))
        i (conditioned :independent-judgement)
        c (conditioned :coupled-judgement)]
    (is (= :computed (:status c)))
    (is (m/normalized-exact? (:posterior c)))
    (is (> (get (:posterior c) five) (* 400 (get (:posterior i) five))))
    (is (not (close? (:f c) (:f i))))
    (testing "partial observations marginalize rather than fabricate absence"
      (let [r (om/query (model :exact-checks #{:x :y})
                        {:op :condition :belief {#{:x} 1/2 #{:x :y} 1/2}
                         :observation (merge context {:status :observed :present #{:x} :absent #{}})
                         :context context})]
        (is (= 1 (:probability r)))
        (is (= {#{:x} 1/2 #{:x :y} 1/2} (:posterior r)))))))

(deftest normalized-oracle-and-distributed-beliefs
  (let [u #{:x :y} states [#{} #{:x} #{:y} u]
        belief {#{} 1/3 u 2/3}]
    (doseq [kind [:exact-checks :independent-judgement :coupled-judgement]
            :let [a (model kind u)]
            s states]
      (let [r (om/query a {:op :row :state s})]
        (is (m/normalized-exact? (:distribution r)))
        (when-not (= :coupled-judgement kind)
          (is (= (m/observation-distribution (:rates a) s) (:distribution r))))))
    (doseq [kind [:exact-checks :independent-judgement :coupled-judgement]
            :let [a (model kind u)
                  prediction (:distribution (om/query a {:op :predict :belief belief}))
                  c (product-preference u u)
                  score (om/query a {:op :score :belief belief :preference c})]]
      (is (m/normalized-exact? prediction))
      (is (close? (:risk score) (m/outcome-risk prediction c)))
      (is (close? (:g score) (+ (:risk score) (:ambiguity score))))
      (is (= :risk-plus-ambiguity (:g-evaluation score))))))

(deftest observation-and-model-refusals
  (let [base (model :exact-checks #{:x})
        request {:op :condition :belief {#{:x} 1} :context context
                 :observation (observation #{:x} #{:x})}]
    (doseq [obs [nil {:status :missing} (merge context {:status :observed :present #{} :absent #{}})]]
      (is (= :missing-observation (:kind (om/query base (assoc request :observation obs))))))
    (is (= :observation-context-mismatch
           (:kind (om/query base (assoc-in request [:observation :tau] 2)))))
    (is (= :observation-context-mismatch
           (:kind (om/query base (assoc-in request [:observation :occurrence-id] "other")))))
    (is (= :invalid-observation-event
           (:kind (om/query base (assoc-in request [:observation :absent] #{:x})))))
    (let [r (om/query base (assoc request :observation (observation #{:x} #{})))]
      (is (= :contradiction (:status r)))
      (is (= 0 (:probability r)))
      (is (= ##Inf (:f r)))
      (is (not (contains? r :posterior))))
    (doseq [[model kind]
            [[(dissoc base :provenance) :synthetic-provenance-required]
             [(assoc base :universe (set (range 11))) :observation-universe-out-of-bounds]
             [(assoc base :rates {:x {:false-neg 1/10 :false-pos 0}}) :nonzero-checkable-rate]
             [(assoc-in (model :coupled-judgement #{:x}) [:components 0 :weight] 1) :invalid-common-cause-mixture]
             [(assoc base :backend :sdd) :unsupported-observation-backend]]]
      (let [r (om/query model request)]
        (is (= kind (:kind r)))
        (is (= model (:model r)))))))

(def fixture (edn/read-string (slurp "test/fixtures/observation-model/tick-001.edn")))

(defn fixture-opts [kind]
  (let [universe (:universe fixture)
        ctx {:occurrence-id "tick-001-replay" :tau (:horizon fixture)}]
    {:observation-model (model kind universe)
     :horizon-steps (:horizon fixture) :cascade-spec (:spec fixture)
     :prediction-context ctx
     ;; A declared synthetic observation of all six tokens, not a claim about
     ;; the historical tick. The candidates/q0/spec retain the recorded shape.
     :observation (merge (observation universe universe) ctx)}))

(deftest production-interface-and-recorded-family
  (let [candidates (:candidates fixture)
        state {:cascade-belief (:q0 fixture)}]
    (doseq [kind [:independent-judgement :coupled-judgement]]
      (let [opts (fixture-opts kind)
            ranked (efe/rank-actions state candidates opts)]
        (is (vector? ranked))
        (is (= 4 (count ranked)))
        (is (every? #(Double/isFinite (double (:f %))) ranked))
        (is (every? #(= (:observation-model opts) (:observation-model %)) ranked))
        (is (every? #(m/normalized-exact? (:posterior %)) ranked))
        (is (= 1 (:rank (some #(when (= :C1-test-first (:cascade-id %)) %) ranked))
                 (:rank (some #(when (= :C2-fix-first (:cascade-id %)) %) ranked))))
        (is (some #(and (= :C1-test-first (:cascade-id %))
                        (not= (set (keys (:q0 fixture))) (set (keys (:posterior %))))) ranked))
        (println "PRODUCTION-F" kind (pr-str (mapv #(select-keys % [:cascade-id :f :controller-score]) ranked)))))
    (testing "exact checks preserve the recorded G values when observed consistently"
      (doseq [candidate candidates]
        (let [predicted (m/rollout (constantly (:precedence candidate)) (:q0 fixture) (:horizon fixture))
              present (first (keys predicted))
              opts (update (fixture-opts :exact-checks) :observation
                           merge {:present present :absent (set/difference (:universe fixture) present)})
              entry (first (efe/rank-actions state [candidate] opts))]
          (is (close? (:controller-score entry) (get-in fixture [:expected-g (:id candidate)])))
          (is (zero? (:f entry)))
          (is (= :exact-checks (get-in entry [:observation-model :kind]))))))
    (testing "contradiction or missing input cannot flow to neutral-F selection"
      (let [bad (efe/rank-actions state candidates (fixture-opts :exact-checks))
            missing (efe/rank-actions state candidates (dissoc (fixture-opts :coupled-judgement) :observation))]
        (is (= :contradiction (:status bad)))
        (is (= :missing (:status missing)))
        (is (seq (:failures bad)))
        (is (every? #(not (contains? % :f)) (:candidates missing)))))
    (is (= :invalid-model-schema
           (:kind (efe/rank-actions state candidates {:observation-model nil}))))
    (is (= :conflicting-observation-options
           (:kind (efe/rank-actions state candidates (assoc (fixture-opts :coupled-judgement) :zeta 2)))))))

(deftest end-to-end-record-and-next-prediction
  (let [dir (Files/createTempDirectory "a-small-model-route-" (make-array FileAttribute 0))
        record-path (.resolve dir "outcome.edn")
        habit-path (str (.resolve dir "absent-habits.edn"))
        opts (fixture-opts :coupled-judgement)
        request {:state {:cascade-belief (:q0 fixture)} :candidates (:candidates fixture)
                 :opts opts :selection-opts {:beta 1 :cascade-habit-path habit-path}}]
    (try
      (let [record (route/run request)]
        (is (= :recorded (:status record)))
        (is (false? (:actuated? record)))
        (is (= :synthetic-bounded-replay (:scope record)))
        (is (= (:observation opts) (get-in record [:outcome :observation])))
        (is (every? #(and (= :computed (:f-status %)) (Double/isFinite (double (:f %))))
                    (get-in record [:selection :selection-certificate :policies])))
        (is (every? #(= (:observation-model opts) (get-in % [:rates-provenance :model]))
                    (vals (get-in record [:selection :selection-certificate :scoring]))))
        (spit (str record-path) (pr-str record))
        (is (= record (edn/read-string (slurp (str record-path)))))
        (let [next-record (route/run (assoc request :state (:next-state record)
                                           :opts (-> opts
                                                     (assoc-in [:prediction-context :occurrence-id] "next")
                                                     (assoc-in [:observation :occurrence-id] "next"))))]
          (is (= :recorded (:status next-record)))
          (is (= (get-in record [:outcome :posterior])
                 (get-in next-record [:scoring 0 :prediction :initial-belief]))))
        ;; Durable registered log retains the declared model, replay inputs,
        ;; selection inputs/law and outcome, not only a success boolean.
        (println "ROUTE-RECORD" (pr-str (select-keys record [:status :scope :actuated? :outcome
                                                           :request :observation-model :selection]))))
      (is (= :missing (:status (route/run (update request :opts dissoc :observation)))))
      (is (= :contradiction (:status (route/run (assoc-in request [:opts :observation-model]
                                                         (model :exact-checks (:universe fixture)))))))
      (finally
        (Files/deleteIfExists record-path)
        (Files/deleteIfExists dir)))))

(deftest producing-cascade-with-joint-observation-through-production
  (let [dir (Files/createTempDirectory "a-producing-cascade-" (make-array FileAttribute 0))
        candidate {:id :produce-five :kind :cascade-candidate
                   :precedence [{:id :produce :theta 1 :produces five
                                 :guard {:status :interpreted
                                         :clauses [{:status :interpreted :present #{} :absent #{}}]}}]}
        base {:state {:cascade-belief {#{} 1}} :candidates [candidate]
              :opts {:horizon-steps 1 :prediction-context context
                     :cascade-spec {:want five :evidence #{} :zeroed #{} :lam 1 :mu 1}}
              :selection-opts {:beta 1 :cascade-habit-path (str (.resolve dir "habits.edn"))}}]
    (try
      (doseq [kind [:exact-checks :independent-judgement :coupled-judgement]
              :let [a (model kind ten)
                    ;; Exact observation confirms the produced state. Both
                    ;; judgement configurations see the joint all-missed event.
                    obs (observation ten (if (= :exact-checks kind) five #{}))
                    r (route/run (update base :opts merge {:observation-model a :observation obs}))
                    entry (first (:scoring r))]]
        (is (= :recorded (:status r)))
        (is (= a (:observation-model r)))
        (is (= {five 1} (get-in entry [:prediction :belief])))
        (is (Double/isFinite (double (:f entry))))
        (is (close? (:f entry) (- (Math/log (double (query-p a five obs))))))
        (is (close? (:f entry) (get-in r [:selection :selection-certificate :policies 0 :f])))
        (println "PRODUCING-CASCADE" (pr-str {:model a :observation obs :f (:f entry)
                                             :consumed-f (get-in r [:selection :selection-certificate :policies 0 :f])})))
      (finally (Files/deleteIfExists dir)))))
