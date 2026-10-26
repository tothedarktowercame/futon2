(ns futon2.aif.completion-check-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.completion-check :as completion-check]
            [futon2.aif.observation-checks :as observation-checks]))

(def after "547b191f8fa1e818b0edca02560abf0017b96abd")
(def before "68120423443eab3243c400563cc9c66f45fea6e7")
(def revision {:repo "futon5a" :sha after})
(def mission-path "holes/missions/M-war-machine-wiring.md")
(def fixture
  (edn/read-string
   (slurp "test/resources/wm/retained-25e24-pattern-fold-projection.edn")))

(deftest prose-and-square-checks-are-not-executable
  (let [prose (get-in fixture [:fold-conditions 0 :witness :authored-clause
                               :completion-check])
        square [:square-B-observed [:some :elements]]]
    (doseq [check [prose square]]
      (is (false? (completion-check/executable? check)))
      (is (= {:status :missing :reason :completion-check-not-executable}
             (completion-check/evaluate check revision))))))

(deftest c3-observes-presence-at-the-after-revision
  (let [check [:artifact-locator
               {:class :C3 :repo "futon5a" :path mission-path}]
        result (completion-check/evaluate check revision)]
    (is (completion-check/executable? check))
    (is (= [:observed true :C3 after]
           ((juxt :status :value :check #(get-in % [:evidence :resolved-sha]))
            result)))))

(deftest c3-false-is-an-observation-not-an-absence
  (let [result (completion-check/evaluate
                [:artifact-locator
                 {:class :C3 :repo "futon5a"
                  :path "holes/missions/no-such-file.md"}]
                revision)]
    (is (= [:observed false :C3]
           ((juxt :status :value :check) result)))))

(deftest c4-observes-declarations-at-the-after-revision
  (let [check (fn [decl]
                [:artifact-locator
                 {:class :C4 :repo "futon5a" :path mission-path :decl decl}])]
    (is (= true (:value (completion-check/evaluate (check "## 1. IDENTIFY")
                                                    revision))))
    (is (= false (:value (completion-check/evaluate (check "## NO-SUCH-HEADING")
                                                     revision))))))

(deftest supplied-revision-overrides-an-authored-sha
  (let [result (completion-check/evaluate
                [:artifact-locator
                 {:class :C3 :repo "futon5a" :path mission-path :sha "HEAD"}]
                revision)]
    (is (= :observed (:status result)))
    (is (= after (get-in result [:evidence :resolved-sha])))
    (is (= "HEAD" (get-in result [:evidence :authored-sha])))))

(deftest wrong-repository-short-circuits-the-reader
  (with-redefs [observation-checks/check-path-exists
                (fn [_] (throw (ex-info "reader must not run" {})))]
    (is (= {:status :missing :reason :different-artifact-repository
            :repo "futon2" :revision-repo "futon5a"}
           (completion-check/evaluate
            [:artifact-locator {:class :C3 :repo "futon2" :path "deps.edn"}]
            revision)))))

(deftest revision-refusals-stay-typed
  (testing "unknown commit"
    (let [result (completion-check/evaluate
                  [:artifact-locator
                   {:class :C3 :repo "futon5a" :path mission-path}]
                  {:repo "futon5a" :sha (apply str (repeat 40 "0"))})]
      (is (= [:missing :unknown-sha]
             ((juxt :status :reason) result)))
      (is (map? (:data result)))))
  (testing "unresolved revision name"
    (is (= {:status :missing :reason :after-revision-missing}
           (completion-check/evaluate
            [:artifact-locator {:class :C3 :repo "futon5a" :path mission-path}]
            {:repo "futon5a" :sha "HEAD"})))))

(deftest malformed-locators-are-not-executable
  (doseq [check [[:artifact-locator {:class :C4 :repo "futon5a" :path "x"}]
                 [:artifact-locator {:class :C5 :repo "futon5a" :path "x"}]
                 [:artifact-locator {:class :C3 :repo "" :path "x"}]
                 nil]]
    (is (false? (completion-check/executable? check)))
    (is (= :completion-check-not-executable
           (:reason (completion-check/evaluate check revision))))))
