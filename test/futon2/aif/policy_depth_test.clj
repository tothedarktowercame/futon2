(ns futon2.aif.policy-depth-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.policy-depth :as depth]
            [futon2.aif.rollout :as rollout]
            [futon2.aif.efe :as efe]
            [futon2.aif.forward-model :as fm]
            [futon2.report.cascade-lane :as lane]))

(def config {:anticipation 3 :cascade-rollout 5})
(def moves
  (mapv (fn [i] {:move/id (str i) :move/class :close-hole
                 :have (str "mission/" i) :want (str "mission/" (inc i))
                 :score 1.0 :step-score-delta -0.1 :rank (inc i)})
        (range 5)))
(def state {:arrows {} :cap-overlay {} :reachable #{"mission/0"}})

(deftest anticipation-depth-and-fallback
  (is (= 3 (:horizon-steps (depth/anticipation {:events-loaded? true :events [{}]} config))))
  (doseq [snapshot [{} {:events-loaded? true :events []}]]
    (is (= {:kind :anticipation :requested 3 :effective 1 :fallback? true
            :reason :anticipation-events-unavailable}
           (:record (depth/anticipation snapshot config)))))
  (is (nil? (:record (depth/anticipation {} nil))))
  (is (= 3 (:horizon-steps (depth/anticipation {:events-loaded? true :events [{}]} nil))))
  (is (thrown? clojure.lang.ExceptionInfo (depth/validate {:anticipation 0 :cascade-rollout 3}))))

(deftest rollout-depth-and-absence
  (let [legacy (rollout/best-rollout state moves :depth 5 :top-k 3 :authority :diagnose)
        explicit-off (rollout/best-rollout state moves :depth 5 :top-k 3
                                         :authority :diagnose :record-depth? false)
        configured (rollout/best-rollout state moves :depth (:cascade-rollout config) :top-k 3
                                       :authority :diagnose :record-depth? true)]
    (is (= (pr-str legacy) (pr-str explicit-off)))
    (is (= ["0" "1" "2" "3" "4"] (mapv :move/id (:policy legacy))))
    (is (= ["0" "1" "2" "3" "4"] (mapv :move/id (:policy configured))))
    (is (= [{:kind :horizon :moves 5 :effective 5}]
           (get-in configured [:policy-depth :endings])))))

(deftest anticipation-reaches-forward-model
  (let [calls (atom []) predict fm/predict-multi-horizon
        snapshot {:events-loaded? true :events [{}]}
        options #(select-keys (depth/anticipation snapshot %) [:horizon-steps])]
    (with-redefs [fm/predict-multi-horizon
                  (fn [s a k opts] (swap! calls conj k) (predict s a k opts))]
      (let [legacy (efe/compute-efe {} {:type :no-op} (options nil))
            configured (efe/compute-efe {} {:type :no-op} (options config))]
        (is (= [3 3] @calls))
        (is (= (pr-str legacy) (pr-str configured)))))))

(deftest truthful-endings
  (doseq [[label input ms expected]
          [[:empty state (subvec moves 0 1) 1]
           [:terminal state [(assoc (first moves) :move/terminal? true)] 1]
           [:truncated (assoc state :truncated? true) moves 0]]]
    (testing (name label)
      (let [result (rollout/best-rollout input ms :depth 3 :record-depth? true
                                       :authority :diagnose)]
        (is (= [{:kind label :moves expected :effective 3}]
               (get-in result [:policy-depth :endings])))
        (is (= expected (count (:steps result)))))))
  (let [step rollout/apply-move]
    (with-redefs [rollout/apply-move (fn [s m] (assoc (step s m) :truncated? true))]
      (let [result (rollout/best-rollout state moves :depth 3 :record-depth? true
                                       :authority :diagnose)]
        (is (= [{:kind :truncated :moves 1 :effective 3}]
               (get-in result [:policy-depth :endings])))
        (is (= 1 (count (:steps result))))))))

(deftest effective-cascade-caller-and-cache
  ;; Replace only the external move-set loader/cache, not the rollout consumer.
  (with-redefs-fn {#'futon2.report.cascade-lane/!rollout-moves (delay moves)
                  #'rollout/seed-roots (fn [_ _] state)
                  #'futon2.report.cascade-lane/!rollout-cap-overlay (delay {})
                  #'futon2.report.cascade-lane/!rollout-g-cache (atom {})}
    (fn []
      (let [legacy (lane/policy-rollout "M-mission")
            configured (lane/policy-rollout "M-mission" config)
            events (lane/policy-rollout-events "M-mission" config)
            separating-config (assoc config :cascade-rollout 3)]
        (is (= legacy configured))
        ;; RUN4's five equals the legacy caller. Three distinguishes config
        ;; consumption from an implementation that merely keeps hardcoded five.
        (is (not= legacy (lane/policy-rollout "M-mission" separating-config)))
        (is (= 3 (:effective (first (lane/policy-rollout-events "M-mission" separating-config)))))
        (is (= legacy (lane/policy-rollout "M-mission")))
        (is (= [] (lane/policy-rollout-events "M-mission")))
        (is (= 5 (:effective (first events))))
        (is (= :horizon (get-in events [0 :endings 0 :kind])))))))
