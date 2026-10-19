(ns futon2.aif.outer-policy-evidence-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.outer-policy-evidence :as sut]
            [futon2.aif.parameter-novelty :as novelty]))

(defn pin [p c] {:path p :sha256 (apply str (repeat 64 c))})
(def entries [{:policy-id "p1" :target-id "T" :cascade-digest "d1"}
              {:policy-id "p2" :target-id "T" :cascade-digest "d2"}])
(def sealed {:schema :wm/sealed-outer-policy-menu-v1 :status :sealed
             :menu-pin (pin "wm://menu" "a") :entries entries})
(defn spec [a]
  {:prior-alpha a :risk {:units :nats :value 1.2}
   :ambiguity {:units :nats :value 0.4}
   :ln-e (into {} (map (fn [k] [k {:value 0.0 :units :nats
                                    :source-pin (pin (str k) "b")}]))
               sut/component-names)})
(defn declaration [policies]
  (let [d {:schema :wm/outer-policy-evidence-declaration-v1
           :outcome-vocabulary [:success :failure]
           :gamma 1.0 :preference-authority (pin "prefs" "c")
           :policies policies}]
    (assoc d :pin {:path "wm://declaration"
                   :sha256 (identity/sha256 (.getBytes (pr-str d) "UTF-8"))})))

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
    (is (= 1 (count (get-in r [:entries 0 :excluded]))))
    (is (= r (sut/join sealed (declaration {"d1" (spec [1.0 1.0])
                                            "d2" (spec [1.0 1.0])}) records)))))

(deftest fail-closed-declarations
  (testing "missing one digest and absent components refuse atomically"
    (let [r (sut/join sealed (declaration {"d1" (spec [1.0 1.0])}) [])]
      (is (= :refused (:status r)))
      (is (= ["d2"] (mapv :cascade-digest (:missing r))))))
  (testing "explicit neutral is accepted; omission is not"
    (is (= :computed (:status (sut/join sealed
                                        (declaration {"d1" (spec [1.0 1.0])
                                                      "d2" (spec [1.0 1.0])}) []))))
    (is (= :refused (:status (sut/join sealed
                                       (declaration {"d1" (update (spec [1.0 1.0]) :ln-e dissoc :resource)
                                                     "d2" (spec [1.0 1.0])}) [])))))
  (testing "unknown outcome and declaration tamper refuse"
    (is (= :refused (:status (sut/join sealed
      (declaration {"d1" (spec [1.0 1.0]) "d2" (spec [1.0 1.0])})
      [{:selected-cascade-digest "d1" :terminal {:outcome :grounded-progress}
        :observed-outcome :unknown}]))))
    (let [d (declaration {"d1" (spec [1.0 1.0]) "d2" (spec [1.0 1.0])})]
      (is (= :declaration-pin (:reason (sut/join sealed (assoc d :gamma 2.0) [])))))))
