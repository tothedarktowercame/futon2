(ns futon2.aif.mission-criteria-test
  "Completion criteria read from mission text as flight wants (A-exits,
  proof2/packets/H-EXITS-D.md). The falsifier is §4's: a locator over the
  criterion sentence alone reads true under either verdict."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.flight :as flight]
            [futon2.aif.mission-criteria :as mc]
            [futon2.aif.observation-checks :as checks]
            [futon2.report.war-machine :as wm]))

(def mission
  (str/join "\n"
            ["# M-test"
             ""
             "## MAP — what exists"
             ""
             "**Exit criterion:** every MAP question has a concrete answer; the"
             "table is complete. **Met.**"
             ""
             "## DERIVE — the method"
             ""
             "**Exit criterion:** someone could implement the mission from DERIVE"
             "alone. **Not met** — four steps are by hand."
             ""
             "## INSTANTIATE"
             ""
             "**Exit criterion:** every completion criterion has a demonstration."
             "**Met for instance 4** — not for the mission."
             ""
             "### Completion criteria"
             ""
             "- **C1 — Identity:** theta has a versioned domain and"
             "  a schema."
             "- [ ] **C2 — a checkbox task:** read by mission_hole_wants, not here"
             ""
             "## After"
             ""
             "- a bullet outside the section"]))

(defn- observe-in [text] (fn [loc] (checks/decl-present? text (:decl loc))))

(defn- read-with [text]
  (let [cs (mc/criteria "M-test" text)]
    (mc/wants cs {:repo "r" :path "p" :observe (observe-in text)})))

(defn- by-phase [w phase]
  (let [c (first (filter #(= phase (:phase %)) (:criteria w)))]
    (get (:universe w) (:token c))))

(deftest reads-both-forms
  (let [w (read-with mission)]
    (is (= [:phase-exit :phase-exit :phase-exit :completion-criterion]
           (mapv :kind (:criteria w))))
    (testing "a checkbox task and bullets outside the section are not read here"
      (is (= 4 (count (:wants w)))))
    (is (= [{:token (:token (last (:criteria w))) :line 20 :kind :completion-criterion
             :reason :verdict-not-stated}]
           (:unlocated w)))))

(deftest only-an-exact-met-verdict-is-met
  (let [w (read-with mission)]
    (is (true? (by-phase w "MAP — what exists")))
    (is (false? (by-phase w "DERIVE — the method")))
    (is (false? (by-phase w "INSTANTIATE")) "a partial verdict is not Met")))

(deftest falsifier-criterion-sentence-alone-reads-true-under-not-met
  ;; H-EXITS-D §4: the bad locator is the criterion sentence alone, which is
  ;; present whatever the verdict. Ours carries the verdict and is false.
  (let [w (read-with mission)
        derive (first (filter #(= "DERIVE — the method" (:phase %)) (:criteria w)))
        bad {:decl "**Exit criterion:** someone could implement the mission from DERIVE"}]
    (is (true? (checks/decl-present? mission (:decl bad))) "the bad locator reads met")
    (is (false? (get (:universe w) (:token derive))) "ours reads not met")))

(deftest flipping-the-verdict-flips-only-that-criterion-and-keeps-its-token
  (let [before (read-with mission)
        after-text (str/replace mission "alone. **Not met** — four steps are by hand."
                                "alone. **Met.**")
        after (read-with after-text)]
    (is (true? (by-phase after "DERIVE — the method")))
    (is (= (:wants before) (:wants after)) "the want token does not depend on the verdict")
    (is (false? (by-phase after "INSTANTIATE")) "another criterion is untouched")))

(deftest another-criterions-met-verdict-does-not-satisfy-this-one
  ;; the declaration starts with this criterion's own words, so MAP's
  ;; **Met.** cannot make DERIVE's locator true
  (let [w (read-with mission)
        derive-loc (some (fn [c] (when (= "DERIVE — the method" (:phase c))
                                   (get (:locators w) (:token c))))
                         (:criteria w))]
    (is (str/starts-with? (:decl derive-loc) "**Exit criterion:** someone could implement"))
    (is (false? (checks/decl-present? mission (:decl derive-loc))))))

(deftest a-exits-want-source-feeds-the-judge
  (let [f (flight/start {:target "M-test" :chosen-because {:kind :requested}}
                        {:kind :a-exits :repo "r" :path "p"
                         :read-text (fn [_ _ _] mission)
                         :observe (observe-in mission)}
                        {:id "flight-a"})
        sources {:wants {"M-test" [:hole/hcheckbox]}}
        cw (flight/click-wants f sources)
        opts (:flight (flight/judge-opts f cw))
        input (wm/flight-assembly-input opts {:targets ["M-test" "M-other"]
                                              :sources {:wants {"M-test" [:hole/hcheckbox]}
                                                        :locators {"M-test" {:hole/hcheckbox {:class :C4}}}
                                                        :universes {"M-test" {:hole/hcheckbox false}}}})]
    (is (= 5 (count (:wants cw))) "the checkbox want plus four criteria")
    (is (= :hole/hcheckbox (first (:wants cw))))
    (is (= 1 (count (get-in cw [:source :unlocated]))))
    (testing "the judge sees the criteria's locators and observations beside the checkbox's"
      (is (= ["M-test"] (:targets input)))
      (is (= 4 (count (get-in input [:sources :locators "M-test"]))))
      (is (true? (get-in input [:sources :universes "M-test" (:token (first (:criteria (read-with mission))))]))))))
