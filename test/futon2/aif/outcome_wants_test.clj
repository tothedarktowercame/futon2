(ns futon2.aif.outcome-wants-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is]]
            [futon2.aif.outcome-wants :as ow]
            [futon2.aif.mission-reading :as reading]
            [futon2.aif.cascade-problems :as cp]
            [futon2.wm.extract-outcomes :as extractor]))

(def target "M-futon-seams")
(def pin "dd7f6b8ba7c3676ca25bbfae8158530e289f85b7")
(def first-criterion {:kind :outcome-statement, :role :why, :stated "Rob's closing ask for the future: a seam in the Emacs layer, so a VS Code / TypeScript implementation could reuse the core functionality rather than copy-pasting interactions into webhooks — which is what he did, and which is \"precisely what one does not want, because now the systems evolve separately, making their unification to a common interface even harder later on.\"", :token :outcome/h51b2c0376b8e7846})
(def outcomes [{:statement "Tests pass."} {:quote "Artifacts are published."}])
(def located (:token (ow/outcome-criterion target (first outcomes))))
(def missing (:token (ow/outcome-criterion target (second outcomes))))
(def locator {:class :C8 :repo "futon2" :namespace "futon2.aif.outcome-wants-test"})
(defn wanted [] (ow/wants target outcomes {located locator} (constantly false)))

(deftest pinned-mission-outcomes
  (let [repo (.getCanonicalPath (io/file ".." "futon3c"))
        r (shell/sh "git" "-C" repo "show" (str pin ":holes/missions/M-futon-seams.md"))
        result (extractor/read-outcomes "holes/missions/M-futon-seams.md" (:out r))
        cs (mapv #(ow/outcome-criterion target %) (:outcomes result))]
    (is (zero? (:exit r)) (:err r))
    (is (= 6 (count cs)))
    (is (= first-criterion (first cs)))
    (println :pinned-outcomes cs)))

(deftest only-located-outcomes-are-wants
  (let [w (wanted)]
    (is (= [located] (:wants w)))
    (is (= {located false} (:universe w)))
    (is (= {located locator} (:locators w)))
    (is (= [{:token missing :stated "Artifacts are published." :role :why
             :reason :no-admitted-locator}] (:unlocated w)))
    (is (= [missing] (mapv :token (:to-ask w))))
    (is (every? #(= :why (:role %)) (vals (:criteria-by-token w))))
    (is (= located (:token (ow/outcome-criterion target (first outcomes)))))
    (is (not= located (:token (ow/outcome-criterion target {:statement "Tests all pass."}))))
    (is (= [located] (mapv :token (:unlocated
                                  (ow/wants target [(first outcomes)]
                                            {located {:class :J}} (fn [_] (throw (Exception. "must not observe"))))))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"boolean"
                         (ow/wants target outcomes {located locator} (constantly {:absent :unknown}))))))

(deftest existing-locator-reading-accepts-an-outcome
  (let [criterion (ow/outcome-criterion target (first outcomes))
        request (reading/locator-request target {} criterion)
        response {:locator locator :cue {:quote "Tests pass."}
                  :reading "The registered test run decides the statement."}
        observe (fn [_] {:observed #{located} :refused {}})
        good (reading/validate-locator request response {:observe observe})
        bad (reading/validate-locator request (assoc response :cue {:quote "Unstated words"})
                                      {:observe observe})]
    (is (= :valid (:status good)))
    (is (true? (:observed good)))
    (is (= :rejected (:status bad)))
    (is (= [:cue-not-in-criterion] (mapv :reason (:reasons bad))))))

(deftest assembly-refuses-a-forced-unlocated-outcome
  (let [w (wanted)
        sources {:universes {target (:universe w)} :wants {target (:wants w)}
                 :locators {target (:locators w)}
                 :interpretations {target {:patterns {:p {:guard {:needs #{} :forbids #{}}
                                                         :produces #{located}}}}}
                 :candidates {target [{:precedence [:p] :construction-receipt {:kind :fixture}}]}
                 :horizon-steps 1 :context-of (constantly :WM) :beta-by-context {:WM 1}}
        good (cp/assemble {:targets [target] :sources sources})
        bad (cp/assemble {:targets [target]
                          :sources (update-in sources [:wants target] conj missing)})]
    (is (empty? (:refusals good)))
    (is (= 1 (count (:problems good))))
    (is (= :universe-not-admitted (get-in bad [:refusals 0 :kind])))
    (is (= [missing] (get-in bad [:refusals 0 :tokens-without-checkable-locator])))
    (println :assembly-good good :assembly-forced-unlocated bad)))
