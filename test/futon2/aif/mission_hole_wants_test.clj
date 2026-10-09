(ns futon2.aif.mission-hole-wants-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [futon2.aif.mission-hole-wants :as mhw]))

(def ^:private mission
  {:id "M-probe" :path "/root/futonX/holes/missions/M-probe.md" :status-class :active
   :text "# M-probe\n\n- [ ] a stated want\n"
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
    (is (re-find #"\[x\]" (:decl (first (vals (:locators (mhw/mission-source "/root" mission)))))))))

(deftest indented-excursion-checkbox-is-an-observable-want
  (let [excursion {:id "E-indented" :path "/root/repo/holes/E-indented.md"
                   :status-class :open
                   :text "# Excursion\n\n  - [ ] run the disruption soak test\n"}
        source (mhw/mission-source "/root" excursion)
        token (first (:want source))]
    (is (= 1 (count (:want source))))
    (is (= false (get-in source [:universe token])))
    (is (= "- [x] run the disruption soak test"
           (get-in source [:locators token :decl])))))

(deftest dated-provisional-closure-heading-survives-source-projection
  (let [marker {:status :provisional :date "2026-10-09"
                :heading "## Closure criteria (provisional, 2026-10-09)"
                :heading-line 3}
        m {:id "M-provisional" :path "/root/repo/holes/M-provisional.md"
           :status-class :open
           :text (str "# Provisional\n\n"
                      "## Closure criteria (provisional, 2026-10-09)\n\n"
                      "- [ ] publish the measured result\n")}
        source (mhw/mission-source "/root" m)
        token (first (:want source))]
    (is (= marker (get-in source [:holes 0 :criterion-status])))
    (is (= marker (get-in source [:want-provenance token])))
    (is (= [marker] (get-in source [:source :provisional-criteria])))
    (is (= false (get-in source [:universe token])))
    (is (map? (get-in source [:locators token])))))

(deftest provisional-marker-is-exact-and-section-bounded
  (let [holes (mhw/current-checkboxes
               "M-boundary"
               (str "## Closure criteria (provisional, someday)\n"
                    "- [ ] undated is ordinary\n"
                    "## Closure criteria (provisional, 2026-10-09)\n"
                    "- [ ] dated is provisional\n"
                    "## Notes\n"
                    "- [ ] later is ordinary\n"))]
    (is (= [nil :provisional nil]
           (mapv #(get-in % [:criterion-status :status]) holes)))
    (is (= "2026-10-09" (get-in holes [1 :criterion-status :date])))))

(deftest the-locator-is-repo-relative-and-checkable
  (let [loc (first (vals (:locators (mhw/mission-source "/root" mission))))]
    (is (= :C4 (:class loc)))
    (is (= "futonX" (:repo loc)))
    (is (= "holes/missions/M-probe.md" (:path loc)) "repo prefix is stripped, not doubled")))

(deftest current-mission-state-wins-over-a-declared-snapshot
  (let [declared {:universes {"M-probe" {:declared-token false}}
                  :wants {"M-probe" [:declared-token]}
                  :locators {} :interpretations {"M-probe" {:patterns {:p {}}}}
                  :candidates {"M-probe" [{:old-order true}]} :context-by-target {}
                  :preference-schedules {"M-probe" {:placement {:value :terminal}}}
                  :preference-scales {"M-probe" {:lam {:value 1} :mu {:value 0}}}}
        merged (mhw/merge-into-sources declared "/root" [mission] :WM)]
    (is (not= [:declared-token] (get-in merged [:wants "M-probe"]))
        "a prior declaration cannot freeze an older mission state")
    (is (= {:patterns {:p {}}} (get-in merged [:interpretations "M-probe"])))
    (is (= [] (get-in merged [:candidates "M-probe"])))
    (is (= 1 (get-in merged [:mission-hole-coverage :targets-added])))
    (is (= ["M-probe"] (get-in merged [:mission-hole-coverage :declared-targets-refreshed])))))

(deftest coverage-accounts-for-what-was-not-projected
  (let [{:keys [coverage]} (mhw/mission-sources "/root" [mission])]
    (is (= 3 (:holes-retained coverage)))
    (is (= 1 (:holes-projected coverage)))
    (is (= 2 (:holes-not-projected coverage)))
    (is (= {:work-marker 1 :open-section-item 1} (:not-projected-by-kind coverage))
        "the gap between stated and projected must be visible, not silent")))

(deftest terminal-missions-state-no-wants
  (doseq [sc [:complete :inactive]]
    (is (nil? (mhw/mission-source "/root" (assoc mission :status-class sc))) (str sc))))

(deftest current-head-not-retained-open-holes-is-decision-authority
  (let [closed (assoc mission :text "# M-probe\n\n- [x] a stated want\n")]
    (is (nil? (mhw/mission-source "/root" closed))
        "a stale substrate open-hole cannot recreate a want already checked at HEAD")))

(deftest current-completion-criteria-join-checkbox-wants
  (let [m (assoc mission :text (str "# M-probe\n\n- [ ] a stated want\n\n"
                                   "## Completion criteria\n\n"
                                   "- the generated report is reproducible\n"
                                   "  **Not started.**\n"))
        src (mhw/mission-source "/root" m)]
    (is (= 2 (count (:want src))))
    (is (= :current-mission-head (get-in src [:source :kind])))
    (is (= 2 (count (:locators src))))
    (is (every? false? (vals (:universe src))))))

(deftest operational-checkbox-does-not-become-blocked-by-unlocated-prose
  (let [m (assoc mission :text (str "# M-probe\n\n- [ ] current work\n\n"
                                   "## Completion criteria\n\n"
                                   "- old prose criterion with no verdict\n"))
        src (mhw/mission-source "/root" m)]
    (is (= 1 (count (:want src))))
    (is (= 1 (count (:locators src))))
    (is (= :verdict-not-stated (get-in src [:unlocated 0 :reason]))
        "the prose gap remains visible even though it cannot veto the checklist")))

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

(deftest want-tokens-survive-a-print-read-round-trip
  (testing "a hex-digest id yields a keyword the READER accepts"
    (doseq [id ["2f9b03b16170" "abc123" "sha256#730434653957" "0000"]]
      (let [tok (mhw/want-token {:id id})]
        (is (= tok (edn/read-string (pr-str tok)))
            (str "unreadable token from id " id
                 " -- it would print into a run record and throw on the next read"))
        (is (= [tok] (edn/read-string (pr-str [tok]))) "also inside a collection")))))

(deftest live-ticket-status-is-a-checkable-closure-want
  (let [ticket {:id "T-repair-one" :status-class :live
                :path "/root/futon2/holes/tickets/T-repair-one.md"
                :text "# Repair\n\n**Status:** OPEN\n"}
        source (mhw/ticket-source "/root" ticket)
        token (first (:want source))]
    (is (= false (get-in source [:universe token])))
    (is (= {:class :C4 :repo "futon2" :sha "HEAD"
            :path "holes/tickets/T-repair-one.md" :decl "**Status:** DONE"}
           (get-in source [:locators token])))
    (is (= :current-ticket-head (get-in source [:source :kind])))
    (is (= [{:id (name token) :kind :ticket-closure
             :line 3 :text "**Status:** OPEN"}]
           (:holes source))
        "the current status is the source-stated interpretation criterion")
    (is (nil? (mhw/ticket-source "/root" (assoc ticket :status-class :terminal))))))
