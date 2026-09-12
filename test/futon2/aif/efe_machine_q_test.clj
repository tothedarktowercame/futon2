(ns futon2.aif.efe-machine-q-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.belief :as belief]
            [futon2.aif.efe :as efe]
            [futon2.aif.machine-model-test :as model-test]
            [futon2.aif.machine-predictive :as predictive]
            [futon2.aif.machine-transition :as transition]
            [futon2.aif.machine-transition-test :as transition-test]))

(def entity "mission/example")
(def state {:observation {:loop-health 0.9 :support-coverage 0.9
                          :attack-coverage 0.9 :mission-health 0.7
                          :stack-pct 0.2 :consulting-pct 0.25
                          :portfolio-pct 0.25 :mathematics-pct 0.2
                          :active-repo-ratio 0.8 :sorry-count-norm 0.1
                          :coupling-density 0.2 :ticks-firing-ratio 0.0
                          :depositing-signal 0.1}
            :belief (belief/initial-belief-state [:m1 :m2])})
(def action-a {:type :no-op :policy "a"})
(def action-b {:type :no-op :policy "b"})

(defn sha256 [x]
  (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                        (.getBytes (pr-str x) "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and 255 %)) digest))))

(deftest absent-option-is-identical
  (let [actual (efe/compute-efe state action-a)
        pin (edn/read-string
             (slurp "holes/labs/wm-contract/runs/row-14-efe-injection-2026-09-12/baseline.edn"))]
    (is (= (:result-sha256 pin) (sha256 actual)))
    (is (= actual (efe/compute-efe state action-a {}))))
  (is (not (contains? (efe/compute-efe state action-a) :machine-q))))

(deftest real-predictive-provider-and-c-flip
  (model-test/with-example
    (fn [example]
      (let [base (transition-test/model example)
            a-kernel (predictive/declared-outcome-a base)
            model (assoc base :A (dissoc a-kernel :ok))
            kernel (transition/controlled-transition-kernel
                    model transition-test/actions transition-test/params)
            belief-input {:mode :single-entity
                          :posteriors {entity (transition-test/point :spawned)}}
            policies {"a" {:id "a" :revision "v1" :entity/id entity
                            :model/revision "v1" :actions [:advance-mission]}
                      "b" {:id "b" :revision "v1" :entity/id entity
                            :model/revision "v1" :actions [:apply-cascade]}}
            provider (fn [_ action]
                       (let [id (:policy action)
                             result (predictive/predictive-outcome-kernel
                                     model belief-input kernel [(policies id)])]
                         {:policy/id id :model (:model result) :support (:support result)
                          :mass (get-in result [:rows id]) :authority (:authority result)
                          :pins (:pins result)}))
            support (get-in model [:outcome :support])
            c-row (fn [favored]
                    (let [small (/ 1 22) large 1/2]
                      (assoc (zipmap support (repeat small)) favored large)))
            qa (provider state action-a) qb (provider state action-b)
            oa (first (for [[o m] (:mass qa) :when (= 1 m)] o))
            ob (first (for [[o m] (:mass qb) :when (= 1 m)] o))
            c-record (fn [favored] {:model (:model qa) :support support
                                    :mass (c-row favored) :provenance {:pin "test"}})
            score (fn [action c] (efe/compute-efe state action
                                                   {:machine-q {:provider provider :c c}}))
            aa (score action-a (c-record oa)) ab (score action-b (c-record oa))
            ba (score action-a (c-record ob)) bb (score action-b (c-record ob))]
        (is (= (:mass qa) (:mass (provider state action-a))))
        (is (< (:controller-score aa) (:controller-score ab)))
        (is (> (:controller-score ba) (:controller-score bb)))
        (is (= qa (get-in aa [:machine-q :q])))
        (is (= (c-record oa) (get-in aa [:machine-q :c])))
        (is (= (get-in aa [:machine-q :G-machine-q-risk])
               (get-in aa [:machine-q :risk :risk])))))))

(deftest adapter-refusal-propagates
  (let [bad {:provider (fn [_ _] {:policy/id "p" :model {:id "m" :revision "r"}
                                   :support (vec (repeat 12 :duplicate)) :mass {}
                                   :authority :declared :pins {:pin "q"}})
             :c {:model {:id "m" :revision "r"} :support [] :mass {}
                 :provenance {:pin "c"}}}]
    (testing "exception data is not caught or rewritten"
      (is (= :support-mismatch
             (try (efe/compute-efe state action-a {:machine-q bad}) nil
                  (catch clojure.lang.ExceptionInfo e
                    (get-in (ex-data e) [:refusal :kind]))))))))
