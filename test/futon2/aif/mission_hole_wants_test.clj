(ns futon2.aif.mission-hole-wants-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.mission-hole-wants :as mhw]))

(def ^:private mission
  {:id "M-probe" :path "/root/futonX/holes/missions/M-probe.md" :status-class :active
   :open-holes [{:id "M-probe#aaa111" :kind :unchecked-task :line 3
                 :text "- [ ] a stated want"}
                {:id "M-probe#bbb222" :kind :work-marker :line 5
                 :text "TODO: something with no checkbox"}
                {:id "M-probe#ccc333" :kind :open-section-item :line 9
                 :text "- an item under an open heading"}]})

(deftest only-holes-whose-closure-a-check-can-witness-are-projected
  (testing "an unchecked task has a closed form; the other kinds do not"
    (is (mhw/observable-hole? (first (:open-holes mission))))
    (is (not (mhw/observable-hole? (second (:open-holes mission)))))
    (is (not (mhw/observable-hole? (nth (:open-holes mission) 2)))))
  (let [src (mhw/mission-source "/root" mission)]
    (is (= 1 (count (:want src)))
        "projecting a hole no check can witness would raise coverage while giving the decision nothing to act on")))

(deftest the-closed-form-is-the-same-item-with-its-box-ticked
  (is (= "- [x] a stated want" (mhw/closed-form (first (:open-holes mission)))))
  (testing "the witness is presence of a checked item, never absence of an unchecked one"
    (is (re-find #"\[x\]" (get-in (mhw/mission-source "/root" mission)
                                  [:locators (mhw/want-token (first (:open-holes mission))) :decl])))))

(deftest the-locator-is-repo-relative-and-checkable
  (let [loc (get (:locators (mhw/mission-source "/root" mission))
                 (mhw/want-token (first (:open-holes mission))))]
    (is (= :C4 (:class loc)))
    (is (= "futonX" (:repo loc)))
    (is (= "holes/missions/M-probe.md" (:path loc)) "repo prefix is stripped, not doubled")))

(deftest a-declared-target-wins-over-a-generated-one
  (let [declared {:universes {"M-probe" {:declared-token false}}
                  :wants {"M-probe" [:declared-token]}
                  :locators {} :interpretations {} :candidates {} :context-by-target {}}
        merged (mhw/merge-into-sources declared "/root" [mission] :WM)]
    (is (= [:declared-token] (get-in merged [:wants "M-probe"]))
        "a hand-written declaration is an operator statement; a generated one only reads a document")
    (is (= 0 (get-in merged [:mission-hole-coverage :targets-added])))
    (is (= ["M-probe"] (get-in merged [:mission-hole-coverage :targets-deferred-to-declaration])))))

(deftest coverage-accounts-for-what-was-not-projected
  (let [{:keys [coverage]} (mhw/mission-sources "/root" [mission])]
    (is (= 3 (:holes-retained coverage)))
    (is (= 1 (:holes-projected coverage)))
    (is (= 2 (:holes-not-projected coverage)))
    (is (= {:work-marker 1 :open-section-item 1} (:not-projected-by-kind coverage))
        "the gap between stated and projected must be visible, not silent")))

(deftest terminal-missions-state-no-wants
  (doseq [sc [:complete :inactive :draft]]
    (is (nil? (mhw/mission-source "/root" (assoc mission :status-class sc))) (str sc))))

(def ^:private terminal-schedule
  {:placement {:value :terminal :status :declared}
   :elsewhere {:value :uniform-over-non-ruled-zero :status :declared}})
(def ^:private scales {:lam {:value 1 :status :declared} :mu {:value 0 :status :declared}})

(defn- declared-with [schedules scales-map]
  {:universes {"M-declared" {:t false}} :wants {"M-declared" [:t]}
   :locators {} :interpretations {} :candidates {} :context-by-target {}
   :preference-schedules schedules :preference-scales scales-map})

(deftest generated-targets-adopt-the-family-schedule-and-scales
  (testing "a generated target carries the declared family's schedule and scales"
    (let [merged (mhw/merge-into-sources
                  (declared-with {"M-declared" terminal-schedule} {"M-declared" scales})
                  "/root" [mission] :WM)]
      (is (= terminal-schedule (get-in merged [:preference-schedules "M-probe"]))
          "without this the target defaults to :every-step and live-c/family-schedule
           refuses the whole comparison as :incommensurable-family")
      (is (= scales (get-in merged [:preference-scales "M-probe"])))
      (is (= 1 (get-in merged [:mission-hole-coverage :targets-added]))))))

(deftest disagreeing-declared-sources-generate-nothing
  (testing "two declared schedules mean no single family value to adopt"
    (let [other {:placement {:value :every-step :status :declared}}
          declared (-> (declared-with {"M-declared" terminal-schedule "M-two" other}
                                      {"M-declared" scales "M-two" scales})
                       (assoc-in [:universes "M-two"] {:t false}))
          merged (mhw/merge-into-sources declared "/root" [mission] :WM)]
      (is (= 0 (get-in merged [:mission-hole-coverage :targets-added]))
          "generating here would produce a family the tick cannot score at all")
      (is (= :declared-sources-lack-one-agreed-schedule-or-scales
             (get-in merged [:mission-hole-coverage :not-generated-reason])))
      (is (nil? (get-in merged [:wants "M-probe"])))))
  (testing "the generator never invents a value when the declared side has none"
    (let [merged (mhw/merge-into-sources
                  (declared-with {} {}) "/root" [mission] :WM)]
      (is (= 0 (get-in merged [:mission-hole-coverage :targets-added])))
      (is (nil? (get-in merged [:mission-hole-coverage :adopted-schedule]))))))
