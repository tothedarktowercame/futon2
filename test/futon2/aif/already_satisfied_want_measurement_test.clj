(ns futon2.aif.already-satisfied-want-measurement-test
  "ALREADY-SATISFIED-WANTS-UNMEASURED (run 4955b6e6): an already-satisfied
  claim must carry the same measurement of each selected want at the
  REVIEWED commit, and the mission's own text is never an artifact.

  Hermetic fixture tests cover claim-mode measurement semantics; the
  retained 4955b6e6 selected action is measured read-only against the real
  futon3c repository (no writes, no runner, no canonical store access
  beyond reading the run record and git objects)."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.d-predecessor-task-authority-test :as fixture]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.meta-terminal-injury :as terminal]
            [futon2.aif.mission-observation :as mission]
            [futon2.aif.observation-checks :as observation]))

(def futon3c "/home/joe/code/futon3c")
(def claimed-commit "62baed0b041b757675b7c2595f57bdd3251d3c56")
(def pre-author-head "d5adbceb05068ce518c1d7f59fb7a84e0cc2aa98")
(def mission-source-path (str futon3c "/holes/missions/M-typed-memories.md"))

;; Verbatim from the retained 4955b6e6 selection: four wants, every locator
;; is a C4 checkbox in the mission's own markdown.
(def retained-locators
  {["M-typed-memories" :hole/h4c0da9d5dca9]
   {:class :C4 :repo "futon3c" :sha "HEAD" :path "holes/missions/M-typed-memories.md"
    :decl "- [x] P1b produces a traceable candidate pattern when every bounded R6 candidate is rejected, and a later witnessed trial changes that candidate's status or precision."}
   ["M-typed-memories" :hole/ha4d7c19e5cdd]
   {:class :C4 :repo "futon3c" :sha "HEAD" :path "holes/missions/M-typed-memories.md"
    :decl "- [x] P2 demonstrates supersession with correct current and as-of answers while retaining the correction event."}
   ["M-typed-memories" :hole/h502c4971a438]
   {:class :C4 :repo "futon3c" :sha "HEAD" :path "holes/missions/M-typed-memories.md"
    :decl "- [x] P3 performs a facet split as a graph operation without rewriting any memory."}
   ["M-typed-memories" :hole/hc32323ac62d1]
   {:class :C4 :repo "futon3c" :sha "HEAD" :path "holes/missions/M-typed-memories.md"
    :decl "- [x] P4 records a real declare → record → attach → fill → promote arc and its measured time-to-fill."}})

(def retained-wants (vec (keys retained-locators)))

(defn retained-dispatch
  "A minimal selected dispatch in the shape mission-observation reads: the
  4955b6e6 selected action's target, wants, locators and pre-author head."
  ([]
   (retained-dispatch retained-wants))
  ([wants]
   {:schema :wm/d-task-dispatch-v1
    :universe (set wants)
    :selected-action-observation-carrier
    {:target "M-typed-memories"
     :observation-locators (select-keys retained-locators wants)
     :selected-action-sha256 "4955b6e6-selected-action"}
    :occurrence {:action/id "4955b6e6-b818-selected"
                 :action/value {:target "M-typed-memories" :want wants
                                :observation-locators (select-keys retained-locators wants)}}
    :before {:repo futon3c :head pre-author-head}}))

(deftest retained-4955b6e6-wants-are-measured-and-the-mission-text-refused
  (with-redefs [observation/repo-root (io/file "/home/joe/code")]
    (let [dispatch (retained-dispatch)
          baseline (mission/capture-before
                    {:dispatch dispatch :repository futon3c
                     :revision pre-author-head})]
      (testing "all four retained wants come out MEASURED at the baseline"
        (is (= :captured (:status baseline)))
        (is (= 4 (count (:rows baseline))))
        (is (every? #(instance? Boolean (:observed %)) (:rows baseline))
            "each want has a boolean measurement"))
      (let [after-rows (mission/capture-after
                        {:dispatch dispatch :repository futon3c
                         :revision claimed-commit})
            normal (mission/project
                    {:baseline baseline :dispatch dispatch :repository futon3c
                     :after-revision claimed-commit :after-rows after-rows})
            claim (mission/project
                   {:baseline baseline :dispatch dispatch :repository futon3c
                    :after-revision claimed-commit :after-rows after-rows
                    :claim {:mission-source-path mission-source-path}})]
        (testing "the honest normal path stands on its own terms"
          ;; at this run's pre-author head the boxes were unchecked, so the
          ;; normal projection admits: the run's defect was that NOTHING
          ;; like this measurement ever ran on the already-satisfied path
          (is (contains? #{:admitted :refused} (:status normal))))
        (testing "claim mode refuses the mission text as the artifact"
          (is (= :refused (:status claim)))
          (is (= :claim-mission-text-not-an-artifact (:reason claim)))
          (is (= "holes/missions/M-typed-memories.md" (get-in claim [:details :locator]))))
        (testing "each of the four wants is explicitly refused on its own"
          (doseq [want retained-wants]
            (let [d1 (retained-dispatch [want])
                  b1 (mission/capture-before
                      {:dispatch d1 :repository futon3c :revision pre-author-head})
                  r1 (mission/project
                      {:baseline b1 :dispatch d1 :repository futon3c
                       :after-revision claimed-commit
                       :after-rows (mission/capture-after
                                    {:dispatch d1 :repository futon3c
                                     :revision claimed-commit})
                       :claim {:mission-source-path mission-source-path}})]
              (is (= :refused (:status r1)) (pr-str want))
              (is (= :claim-mission-text-not-an-artifact (:reason r1))
                  (pr-str want))
              (is (= want (get-in r1 [:details :token]))))))))))

(deftest a-claim-refuted-at-its-own-commit-is-not-admitted
  (fixture/with-artifact
   {:before-files {"created.clj" "pre-existing content\n"}
    :action {:kind :cascade-candidate :id :C0 :target "target"
             :want [:artifact]
             :precedence [{:id :make-file :theta 1
                           :produces #{["target" :artifact]}}]}}
   (fn [{:keys [inputs repo]}]
     (with-redefs [observation/repo-root (.getParent (java.io.File. (str repo)))]
       (let [dispatch (:dispatch inputs)
             git #(apply shell/sh "git" "-C" (str repo) %&)
             _ (git "rm" "-q" "created.clj")
             _ (git "-c" "user.name=t" "-c" "user.email=t@t" "commit" "-q" "-m" "remove")
             removed (str/trim (:out (git "rev-parse" "HEAD")))
             baseline (mission/capture-before
                       {:dispatch dispatch :repository (str repo)
                        :revision (get-in dispatch [:before :head])})
             after-rows (mission/capture-after
                         {:dispatch dispatch :repository (str repo) :revision removed})
             claim (mission/project
                    {:baseline baseline :dispatch dispatch :repository (str repo)
                     :after-revision removed :after-rows after-rows
                     :claim {:mission-source-path "/elsewhere/mission.md"}})]
         (is (false? (get-in (first after-rows) [:result :observed])) (pr-str after-rows))
         (is (= :refused (:status claim)) (pr-str claim))
         (is (= :claim-want-false-at-reviewed-commit (:reason claim))))))))

(deftest a-mission-claim-without-a-measurement-is-not-measured
  (let [measured? #'runner/claim-measured?]
    (is (false? (measured? "M-typed-memories" nil))
        "a missing baseline or dispatch cannot pass a mission claim")
    (is (false? (measured? "M-typed-memories" {:status :refused})))
    (is (true? (measured? "M-typed-memories" {:status :admitted})))
    (is (true? (measured? "T-some-ticket" nil)))
    (is (false? (measured? "T-some-ticket" {:status :refused})))))

(deftest an-unmeasured-claim-is-not-an-injury
  (is (= :already-satisfied-wants-unmeasured
         (:already-satisfied-wants-unmeasured terminal/explicit-non-injury-kinds))))

(deftest claim-mode-measures-a-real-artifact-and-keeps-the-normal-refusal
  (fixture/with-artifact
   {:before-files {"created.clj" "pre-existing content\n"}
    :action {:kind :cascade-candidate :id :C0 :target "target"
             :want [:artifact]
             :precedence [{:id :make-file :theta 1
                           :produces #{["target" :artifact]}}]}}
   (fn [{:keys [inputs repo commit]}]
     (with-redefs [observation/repo-root (.getParent (java.io.File. (str repo)))]
       (let [dispatch (:dispatch inputs)
             before (get-in dispatch [:before :head])
             baseline (mission/capture-before
                       {:dispatch dispatch :repository (str repo) :revision before})
             after-rows (mission/capture-after
                         {:dispatch dispatch :repository (str repo) :revision commit})
             run #(mission/project
                   {:baseline baseline :dispatch dispatch :repository (str repo)
                    :after-revision commit :after-rows after-rows :claim %})]
         (is (= :captured (:status baseline)))
         (is (true? (get-in baseline [:rows 0 :observed]))
             "the artifact already exists before: the claim's own situation")
         (testing "normal path keeps the honest true-before refusal"
           (is (= :selected-want-true-before (:reason (run nil)))))
         (testing "claim mode with a real artifact admits the measurement"
           (let [claim (run {:mission-source-path "/elsewhere/mission.md"})]
             (is (= :admitted (:status claim)) (pr-str claim))
             (is (true? (get-in claim [:rows 0 :after :observed])))))
         (testing "claim mode refuses when the only artifact IS the mission text"
           (is (= :claim-mission-text-not-an-artifact
                  (:reason (run {:mission-source-path (str (io/file repo "created.clj"))}))))))))))

(deftest an-admitted-measurement-reaches-the-record-satisfaction-reads
  (let [detail (#'runner/failure-detail
                {:mission-observation {:status :admitted :rows [{:token :a}]}})
        record {:failure {:kind :already-satisfied :target "M-x" :detail detail}
                :outcome :already-satisfied}]
    (is (= :admitted (get-in record [:failure :detail :mission-observation :status])))
    (is (true? (:wants-measured (terminal/satisfaction-evidence record))))))
