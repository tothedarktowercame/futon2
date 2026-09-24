(ns futon2.aif.flight-test
  "A flight: target fixed across clicks, stop-lines first, wants carried,
  closure, and a click that advances nothing ends the flight. The judge's
  side (futon2.report.war-machine/flight-assembly-input) restricts
  assembly to the flight's target and wants."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.flight :as flight]
            [futon2.report.war-machine :as wm]))

(def target "M-test")
(def wants [:a :b :c])

(def sources {:wants {target wants "M-other" [:z]}})

(defn- new-flight [& [ws]]
  (flight/start (flight/choose-target {:requested target})
                (or ws {:kind :checkbox}) {:id "flight-t"}))

(deftest stop-lines-come-first
  (let [c (flight/choose-target
           {:requested target
            :open-obligations [{:repair/id "occ-b" :opened-at "2026-09-24T02"}
                               {:repair/id "occ-a" :opened-at "2026-09-24T01"}]})]
    (is (= "T-occ-a" (:target c)))
    (is (= :open-stop-line (get-in c [:chosen-because :kind])))
    (is (= 2 (get-in c [:chosen-because :open-stop-lines]))))
  (is (= target (:target (flight/choose-target {:requested target :open-obligations []}))))
  (is (nil? (flight/choose-target {}))))

(defn- world
  "A target whose facts change as clicks land: STEPS is a vector of the
  facts after each click. Returns click-fn / observe-fn over an atom, and
  records every judge-opts the flight passed."
  [initial steps & [unreached]]
  (let [facts (atom initial) n (atom 0) seen (atom [])]
    {:seen seen
     :click-fn (fn [opts]
                 (swap! seen conj opts)
                 (let [i @n]
                   (swap! n inc)
                   (when (< i (count steps)) (reset! facts (nth steps i)))
                   {:click-id (str "click-" (inc i))
                    :unreached-wants (get unreached i [])}))
     :observe-fn (fn [_] @facts)
     :sources-fn (constantly sources)}))

(deftest a-flight-closes-its-target
  (let [w (world {:a false :b false :c false}
                 [{:a true :b false :c false}
                  {:a true :b true :c true}])
        f (flight/run! (new-flight) (assoc w :max-clicks 5))]
    (is (= :closed (:status f)))
    (is (= [[:a] [:b :c]] (mapv :advanced (:clicks f))))
    (testing "the target never changes across clicks"
      (is (= #{target} (set (map #(get-in % [:flight :target]) @(:seen w))))))
    (testing "the judge is told the flight's wants"
      (is (= wants (get-in (first @(:seen w)) [:flight :wants]))))))

(deftest a-click-that-advances-nothing-ends-the-flight
  ;; the bad case rule 2 names: a click with no progress must not be
  ;; followed by more clicks
  (let [w (world {:a false :b false :c false}
                 [{:a true :b false :c false}
                  {:a true :b false :c false}
                  {:a true :b true :c true}])
        f (flight/run! (new-flight) (assoc w :max-clicks 5))]
    (is (= :no-progress (:status f)))
    (is (= 2 (count (:clicks f))))
    (is (= [true false] (mapv :progress? (:clicks f))))))

(deftest unreached-wants-are-carried-into-the-next-click
  ;; the want source drops :d (not in the mission's list), but click 1's
  ;; plan left it unreached, so click 2 still asks for it
  (let [w (world {:a false :b false :c false}
                 [{:a true :b false :c false} {:a true :b true :c true :d true}]
                 [[{:token :d :reason :beyond-horizon}]])
        f (flight/run! (new-flight) (assoc w :max-clicks 5))]
    (is (= [:a :b :c :d] (get-in (second @(:seen w)) [:flight :wants])))
    (is (= [:d] (:carried-wants f)))
    (is (= :closed (:status f)))))

(deftest abstention-missing-input-is-recorded-as-a-need
  (let [f (flight/record-click (new-flight)
                               {:click-id "c1" :wants wants
                                :before {:a false} :after {:a true}
                                :abstention {:kind :no-admitted-interpretation
                                             :missing :interpretations}})]
    (is (= [{:click-id "c1" :kind :no-admitted-interpretation :missing :interpretations}]
           (:needs f)))))

(deftest click-limit-is-not-closure
  (let [w (world {:a false :b false :c false}
                 [{:a true :b false :c false} {:a true :b true :c false}])
        f (flight/run! (new-flight) (assoc w :max-clicks 2))]
    (is (= :click-limit (:status f)))
    (is (= [:c] (:open-after (last (:clicks f)))))))

(deftest operator-declared-wants-are-typed
  (let [f (new-flight {:kind :operator-declared :wants [:x] :declared-by "test"})
        {:keys [wants source]} (flight/click-wants f sources)]
    (is (= [:x] wants))
    (is (= :operator-declared (:kind source)))))

(deftest judge-assembles-only-the-flight-target
  (let [input {:targets [target "M-other" "T-occ-z"]
               :sources {:wants {target [:a] "M-other" [:z]} :horizon-steps 2}}
        f {:target target :wants [:a :b :c :d]}
        out (wm/flight-assembly-input f input)]
    (is (= [target] (:targets out)))
    (is (= [:a :b :c :d] (get-in out [:sources :wants target])))
    (is (= input (wm/flight-assembly-input nil input)) "no flight: unchanged")))
