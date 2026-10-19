(ns futon2.aif.outer-policy-evidence-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.outer-policy-evidence :as sut]
            [futon2.aif.parameter-novelty :as novelty]
            [futon2.aif.wm.executable-cascade :as xc]
            [futon2.aif.wm.family-selection :as family-selection]))

(defn pin [p c] {:path p :sha256 (apply str (repeat 64 c))})
(def entries [{:policy-id "p1" :target-id "T" :cascade-digest "d1"
               :canonical-cascade {:digest "d1"}}
              {:policy-id "p2" :target-id "T" :cascade-digest "d2"
               :canonical-cascade {:digest "d2"}}])
(def sealed-body {:schema :wm/sealed-outer-policy-menu-v1
                  :field-source-pin (pin "wm://field" "f")
                  :family-census {} :entries entries :refusals [] :failures []})
(def sealed (assoc sealed-body :status :sealed
                   :menu-pin (family-selection/sealed-menu-pin sealed-body)))
(use-fixtures :each (fn [run]
                      (with-redefs [xc/digest :digest
                                    xc/validate-cascade (constantly {:status :certified})]
                        (run))))
(defn spec [a]
  {:prior-alpha a
   :ln-e (into {} (map (fn [k] [k {:value 0.0 :units :nats
                                    :source-pin (pin (str k) "b")}]))
               sut/component-names)})
(defn declaration [policies]
  (let [d {:schema :wm/outer-policy-evidence-declaration-v1
           :outcome-vocabulary [:success :failure]
           :gamma 1.0 :preference-authority (pin "prefs" "c")
           :preference {:vocabulary [:success :failure]
                        :distribution [0.7 0.3] :units :probability
                        :authority (pin "prefs" "c")}
           :policies policies}]
    (assoc d :pin {:path "wm://declaration"
                   :sha256 (identity/sha256 (.getBytes (pr-str d) "UTF-8"))})))
(defn reseal [d]
  (assoc-in d [:pin :sha256]
            (identity/sha256 (.getBytes (pr-str (dissoc d :pin)) "UTF-8"))))

(deftest categorical-law-controls
  (let [cat (novelty/categorical-information [:s :f] [2.0 3.0])
        beta (novelty/beta-information 2.0 3.0)
        low (novelty/categorical-information [:s :f] [1.0 1.0])
        high (novelty/categorical-information [:s :f] [10.0 10.0])]
    (is (< (abs (- (:nats cat) (:nats beta))) 1.0e-12))
    (is (> (:nats low) (:nats high)))
    (is (= (:nats low) (:nats (novelty/categorical-information [:s :f] [1.0 1.0]))))
    (is (= :invalid-categorical-prior
           (:reason (novelty/categorical-information [:s :f] [1.0 0.0]))))))

(deftest exact-digest-and-terminal-cause-fold
  (let [records [{:run/id "ok" :selected-cascade-digest "d1"
                  :terminal {:outcome :grounded-progress}
                  :observed-outcome :success}
                 {:run/id "apparatus" :selected-cascade-digest "d1"
                  :terminal {:outcome :incomplete}
                  :failure {:kind :phase-failure :stage :build}
                  :observed-outcome :failure}
                 {:run/id "other" :selected-cascade-digest "d2"
                  :terminal {:outcome :grounded-progress}
                  :observed-outcome :failure}]
        r (sut/join sealed (declaration {"d1" (spec [1.0 1.0])
                                         "d2" (spec [1.0 1.0])}) records)]
    (is (= :computed (:status r)))
    (is (= [1 0] (get-in r [:entries 0 :task-counts])))
    (is (= [0 1] (get-in r [:entries 1 :task-counts])))
    (is (= [(/ 2.0 3.0) (/ 1.0 3.0)]
           (get-in r [:entries 0 :posterior-predictive :distribution])))
    (is (nil? (get-in r [:entries 0 :risk])))
    (is (= 1 (count (get-in r [:entries 0 :excluded]))))
    (is (= r (sut/join sealed (declaration {"d1" (spec [1.0 1.0])
                                            "d2" (spec [1.0 1.0])}) records)))))

(deftest partial-policy-evidence-is-measured-without-resealing
  (testing "missing one digest retains its complete neighbour"
    (let [r (sut/join sealed (declaration {"d1" (spec [1.0 1.0])}) [])]
      (is (= :partial (:status r)))
      (is (nil? (sut/verify-evidence r)))
      (is (= :computed (get-in r [:entries 0 :status])))
      (is (= :absent (get-in r [:entries 1 :status])))
      (is (= [:policy-declaration :prior-alpha :ln-e-resource :ln-e-cadence
              :ln-e-continuation]
             (get-in r [:entries 1 :absent-terms])))
      (is (= {:count 2 :policy-digests ["d1" "d2"]}
             (get-in r [:frontier :menu])))
      (is (= {:count 1 :policy-digests ["d1"]}
             (get-in r [:frontier :joined])))
      (is (= {:count 1
              :policies [{:policy-id "p2" :target-id "T" :cascade-digest "d2"
                          :absent-terms [:policy-declaration :prior-alpha :ln-e-resource
                                         :ln-e-cadence :ln-e-continuation]}]}
             (get-in r [:frontier :term-absent])))))
  (testing "a missing cadence pin is isolated to that digest"
    (let [bad (update-in (spec [1.0 1.0]) [:ln-e :cadence :source-pin] dissoc :sha256)
          r (sut/join sealed (declaration {"d1" (spec [1.0 1.0]) "d2" bad}) [])]
      (is (= :partial (:status r)))
      (is (= :computed (get-in r [:entries 0 :status])))
      (is (= {:status :absent :policy-id "p2" :target-id "T" :cascade-digest "d2"
              :absent-terms [:ln-e-cadence] :excluded []}
             (get-in r [:entries 1])))))
  (testing "all incomplete is an honest empty joined set"
    (let [r (sut/join sealed (declaration {}) [])]
      (is (= :partial (:status r)))
      (is (= 0 (get-in r [:frontier :joined :count])))
      (is (= 2 (get-in r [:frontier :term-absent :count])))))
  (testing "explicit neutral is accepted; omission is measured"
    (is (= :computed (:status (sut/join sealed
                                        (declaration {"d1" (spec [1.0 1.0])
                                                      "d2" (spec [1.0 1.0])}) []))))
    (is (= :partial (:status (sut/join sealed
                                       (declaration {"d1" (update (spec [1.0 1.0]) :ln-e dissoc :resource)
                                                     "d2" (spec [1.0 1.0])}) [])))))
  (testing "unknown outcome is retained as partial and declaration tamper refuses"
    (is (= :partial (:status (sut/join sealed
      (declaration {"d1" (spec [1.0 1.0]) "d2" (spec [1.0 1.0])})
      [{:selected-cascade-digest "d1" :terminal {:outcome :grounded-progress}
        :observed-outcome :unknown}]))))
    (let [d (declaration {"d1" (spec [1.0 1.0]) "d2" (spec [1.0 1.0])})]
      (is (= :declaration-pin (:reason (sut/join sealed (assoc d :gamma 2.0) [])))))))

(deftest provider-score-absence-remains-attached-to-the-policy
  (let [score-records [{:policy-id "p1" :target-id "T" :cascade-digest "d1"
                        :status :computed}
                       {:policy-id "p2" :target-id "T" :cascade-digest "d2"
                        :status :absent :reason :preference-transform-invalid}]
        r (sut/join sealed (declaration {"d1" (spec [1.0 1.0])}) [] score-records)]
    (is (= :partial (:status r)))
    (is (= :computed (get-in r [:entries 0 :status])))
    (is (= [:policy-declaration :prior-alpha :ln-e-resource :ln-e-cadence
            :ln-e-continuation :policy-score-absent]
           (get-in r [:entries 1 :absent-terms])))
    (is (= :preference-transform-invalid
           (get-in r [:entries 1 :score-absence :reason])))
    (is (= :preference-transform-invalid
           (get-in r [:frontier :term-absent :policies 0 :score-absence :reason])))))

(deftest adversarial-authority-and-sealed-carrier-validation
  (let [d (declaration {"d1" (spec [1.0 1.0]) "d2" (spec [1.0 1.0])})]
    (is (= :declaration-pin (:reason (sut/join sealed (assoc-in d [:pin :path] nil) []))))
    (is (= :preference-vocabulary
           (:reason (sut/join sealed (reseal (assoc-in d [:preference :vocabulary]
                                                        [:failure :success])) []))))
    (is (= :preference-mass
           (:reason (sut/join sealed (reseal (assoc-in d [:preference :distribution]
                                                        [1.0 0.0])) []))))
    (is (= :preference-normalization
           (:reason (sut/join sealed (reseal (assoc-in d [:preference :distribution]
                                                        [0.8 0.3])) []))))
    (is (= :menu-pin-mismatch (:reason (sut/join (dissoc sealed :menu-pin) d []))))
    (is (= :preference-units
           (:reason (sut/join sealed (reseal (assoc-in d [:preference :units] :nats)) []))))
    (is (= :duplicate-policy-id
           (:reason (sut/join (let [s (assoc-in sealed [:entries 1 :policy-id] "p1")]
                                (assoc s :menu-pin (family-selection/sealed-menu-pin s))) d []))))
    (is (= :duplicate-cascade-digest
           (:reason (sut/join (let [s (-> sealed
                                          (assoc-in [:entries 1 :cascade-digest] "d1")
                                          (assoc-in [:entries 1 :canonical-cascade :digest] "d1"))]
                                (assoc s :menu-pin (family-selection/sealed-menu-pin s))) d []))))
    (is (= :menu-pin-mismatch
           (:reason (sut/join (assoc-in sealed [:entries 0 :cascade-digest] "forged") d []))))))
