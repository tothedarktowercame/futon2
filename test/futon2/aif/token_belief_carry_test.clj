(ns futon2.aif.token-belief-carry-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.exact-belief-adapter :as adapter]
            [futon2.aif.declaration-reads-test :as files]
            [futon2.aif.locator-fixtures :as locators]
            [futon2.aif.scoring-input-receipts :as receipts]
            [futon2.aif.token-belief-carry :as carry]
            [futon2.aif.trace :as trace]
            [futon2.report.cascade-decision-test :as fixture]
            [futon2.report.war-machine :as wm]))

(def absent-habit-path "resources/fixtures/d-token-carry/absent-habit.edn")

(defn assembled []
  (problems/assemble {:targets [fixture/tick-1-target]
                      :sources (locators/locate-all fixture/tick-1-sources)}))

(defn decision [previous]
  (when (.exists (io/file absent-habit-path))
    (throw (ex-info "Conservativity fixture requires an absent habit store"
                    {:path absent-habit-path})))
  (with-redefs [habit/default-path absent-habit-path]
    (:decision (wm/cascade-decision
                (assembled)
                (assoc fixture/live-c-opts
                       :cascade-habit-path absent-habit-path
                       :prospective-token-carry previous
                       :token-belief-context {:occurrence-id "d-2a-fixture"})))))

(defn outcomes [d]
  ;; This projection names the outcomes whose bytes must remain identical.
  ;; Certificate additions (D staging and concurrent Q evidence) are not
  ;; selection outcomes. D is read from the actual consumed-value census.
  (assoc (select-keys d [:action :chosen-action-mass :beta :selection-law])
         :D (mapv #(get-in % [:terms :D])
                  (get-in d [:selection-certificate :g-term-decomposition :policies]))))

(deftest staged-carry-preserves-outcomes-even-when-prior-disagrees
  (let [first-decision (decision nil)
        stage (get-in first-decision [:selection-certificate :token-belief-stage])
        previous (assoc (:prospective-carry stage) :belief {#{} 1})
        next-decision (decision previous)
        next-stage (get-in next-decision [:selection-certificate :token-belief-stage])
        baseline (edn/read-string
                  (slurp (io/resource "fixtures/d-token-carry/baseline-outcomes.edn")))
        ;; c155d690 removed the empty diagnostic C0 from executable scoring.
        ;; Derive the authorized renormalization from the retained old fixture,
        ;; never from the decision under test. Candidate metadata may grow.
        old-acting (into {} (filter (comp seq :precedence key))
                         (get-in baseline [:selection-law :posterior]))
        acting-mass (reduce + (vals old-acting))
        expected (into {} (map (fn [[a p]] [(:id a) (/ p acting-mass)])) old-acting)
        posterior (get-in first-decision [:selection-law :posterior])
        actual (into {} (map (fn [[a p]] [(:id a) p])) posterior)]
    ;; The invariant is prospective carry cannot change *this* decision,
    ;; including the complete law and all candidate metadata, byte for byte.
    (is (= (pr-str (outcomes first-decision)) (pr-str (outcomes next-decision))))
    (is (= #{:C1 :C2 :C3} (set (keys actual))))
    (doseq [[id p] expected]
      (is (< (Math/abs (- p (get actual id Double/NaN))) 1.0e-12) (str id)))
    (is (= (select-keys (:action baseline) [:id :target :precedence])
           (select-keys (:action first-decision) [:id :target :precedence])))
    (is (< (Math/abs (- (get expected :C2) (:chosen-action-mass first-decision))) 1.0e-12))
    (is (= (:beta baseline) (:beta first-decision)))
    (is (= (vec (repeat (count expected) (first (:D baseline))))
           (:D (outcomes first-decision))))
    (is (= (:value (first (:D baseline)))
           (get-in first-decision [:initial-belief-receipt :value])))
    (is (not= (:belief previous) (get-in first-decision [:initial-belief-receipt :value])))
    (is (not= (:belief previous) (:continuation-belief next-stage)))
    (is (= previous (:prospective-prior next-stage)))
    (is (= :not-admitted-for-consumption (:prospective-prior-authority next-stage)))
    (is (= :not-wired (:conditioning-status next-stage)))
    (is (= [] (:observation-updates next-stage)))
    (is (= :missing (get-in next-stage [:observation :status])))
    (is (nil? (:tau next-stage)))
    (is (= "d-2a-fixture" (:occurrence-id next-stage)))
    (is (= {:token-count 6 :state-count 64 :support-count 1} (:carrier next-stage)))
    (println "D-2A-CONSERVATIVITY" (pr-str {:outcome-bytes-identical true
                                           :carrier (:carrier next-stage)
                                           :disagreeing-prior-retained-as-prospective true
                                           :conditioning-status (:conditioning-status next-stage)}))))

(deftest prospective-state-survives-real-trace-serialization
  (files/with-dir
    (fn [dir]
      (let [d (decision nil)
            health {"entity" {:healthy 1}}
            path (trace/write-trace! {:decision d :belief health :belief-pre health}
                                    :dir (str dir))
            record (edn/read-string (slurp path))
            previous (get-in record [:decision :selection-certificate :token-belief-stage
                                     :prospective-carry])
            next-stage (get-in (decision previous) [:selection-certificate :token-belief-stage])]
        (is (some? previous))
        (is (= previous (:prospective-prior next-stage)))
        (is (= health (:mu-post record)))
        (is (= health (:mu-pre record)))
        (is (= :not-wired (:conditioning-status next-stage)))))))

(deftest transition-orientation-on-the-assembled-token-carrier
  (let [d (decision nil)
        universe (get-in d [:selection-certificate :token-belief-stage :prospective-carry :universe])
        states (reduce (fn [ss token] (into ss (map #(conj % token) ss))) [#{}] universe)
        pattern (-> (some #(when (= :test-step-covering-missing-total-repos (:id %)) %)
                          (mapcat :precedence (keys (get-in d [:selection-law :posterior]))))
                    (assoc :theta 1/2 :theta-source :declared-test-fixture))
        s0 #{}
        s1 #{[fixture/tick-1-target :summary-without-total-repos-throws]}
        s2 (conj s1 [fixture/tick-1-target :test-covers-missing-total-repos])
        prior {s0 1/3 s1 1/3 s2 1/3}
        B #(model/cascade-kernel [pattern] %)
        A (fn [s] {s 1})
        result (adapter/exact-update states A B s2 prior {:status :declared})
        transposed (fn [s] (into {} (for [x states] [x (get (B x) s 0)])))
        bad (adapter/exact-update states A transposed s2 prior {:status :declared})]
    (is (= 64 (count states)))
    (is (= {s0 1/3 s1 1/6 s2 1/2}
           (into {} (filter (comp pos? val)) (:predicted-state result))))
    (is (= {s2 1} (into {} (filter (comp pos? val)) (:posterior result))))
    (is (= :invalid (:status bad)))
    (is (= :invalid-transition-kernel (:kind bad)))))

(deftest stage-chain-rejects-invented-update-and-wrong-consumed-belief
  (let [d (decision nil)
        initial (:initial-belief-receipt d)
        stage (get-in d [:selection-certificate :token-belief-stage])]
    (is (carry/valid-stage? stage initial))
    (doseq [bad [(assoc stage :conditioning-status :conditioned)
                 (assoc stage :continuation-belief {#{} 1})
                 (assoc stage :observation-updates [{:status :value}])
                 (assoc-in stage [:prospective-carry :belief] {#{} 1})
                 (assoc-in stage [:carrier :state-count] 1)
                 (assoc-in stage [:observation :occurrence-id] "another-tick")
                 (assoc-in stage [:observation :status] :observed)]]
      (is (not (carry/valid-stage? bad initial))))
    ;; Exercise the original invariant against the real validator; an
    ;; added stage must never excuse a different incoming scoring belief.
    (let [record {:decision (-> d
                                (assoc-in [:selection-certificate :candidates] [])
                                (assoc-in [:selection-certificate :node-evaluation-traces 0
                                           :evaluations 0 :incoming-belief] {#{} 1}))
                  :habit-reads (receipts/habit-log [])}]
      (is (some #{:initial-belief-value-mismatch} (:errors (receipts/validate-record record)))))))
