(ns futon2.aif.shadow-cascade-g-test
  "Behavioural controls for the shadow cascade G scorer. The computed values
  exercise the same Lean-quantities path as cascade-model-manifest's own
  fixture tests (horizonEFE at zero rates, outcomeRisk with zero-C refusal);
  here the contract under test is the SHADOW role: computed-or-typed for each
  arm, one common universe, declared inputs recorded, and construction
  output (:shown/:selected-action) unchanged by the wiring."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-policy :as policy]
            [futon2.aif.find-receipt-test :as find-fixture]
            [futon2.aif.receipt-construction :as construction]
            [futon2.aif.shadow-cascade-g :as shadow])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- find-sample [] (find-fixture/sample))

(def absent-score {:status :none :reason :cascade-g-not-computed})

(defn- tmp-dir []
  (.toFile (Files/createTempDirectory "shadow-g" (make-array FileAttribute 0))))

(defn- fixture-env []
  (let [root (tmp-dir)
        lib (io/file root "library")]
    (.mkdirs lib)
    (spit (io/file root "mission.md")
          (str "# Mission: M-shadow-test\n\n"
               "**Status:** alpha built and ready\n"
               "**Status:** beta missing\n"))
    (doseq [id ["p/a" "p/b" "p/c"]]
      (.mkdirs (.getParentFile (io/file lib (str id ".flexiarg")))))
    (spit (io/file lib "p/a.flexiarg")
          (str "  + IF: alpha\n"
               "  + HOWEVER: not blocked\n"
               "  + THEN: gamma delta\n"))
    (spit (io/file lib "p/b.flexiarg")
          (str "  + IF: gamma and delta\n"
               "  + HOWEVER: not blocked\n"
               "  + THEN: omega alpha\n"))
    (spit (io/file lib "p/c.flexiarg")
          "  + IF: alpha or maybe\n  + HOWEVER: not blocked\n  + THEN: nothing\n")
    {:root root :lib lib
     :mission-path (.getCanonicalPath (io/file root "mission.md"))}))

(defn- fixture-record [env]
  {:schema :wm/interpreted-pattern-set-v1
   :identity {:occurrence {:action/value {:type :advance-mission :target "M-shadow-test"}
                           :action-at "2026-09-16T12:00:00Z"}}
   :sources [{:id "mission-src" :path (:mission-path env) :sha256 "pinned"}]
   :target {:id "M-shadow-test" :kind :mission
            :source "mission-src"
            :citations [{:source "mission-src" :lines [1 1] :quote "# Mission"}]
            :pinned-at "2026-09-16T12:00:00Z"}
   :facts [] :interpretations []})

(deftest shadow-g-computed-on-fixture
  (let [env (fixture-env)
        record (fixture-record env)
        before {:nodes #{} :precedence []}
        after {:nodes #{:p/a :p/b} :precedence [:p/a :p/b]}
        results (shadow/shadow-cascade-g record {:before before :after after}
                                         {:library-root (str (:lib env))})]
    (doseq [arm [:before :after]
            :let [r (get results arm)]]
      (is (= :computed (:status r)) (pr-str arm r))
      (is (or (double? (:g r)) (= :infinite (:g r))) (pr-str arm r))
      (is (double? (:g r)) "the empty-arm fixture has no zero-C outcome: finite")
      (let [inputs (:inputs r)]
        (is (= 3 (:horizon inputs)))
        (is (= :declared-zero (:rates inputs)))
        (is (= {:evidence #{} :lam 1 :mu 1 :zeroed #{}} (select-keys (:spec inputs) [:evidence :lam :mu :zeroed])))
        (is (= :documented-interpretation (:authority inputs)))
        (is (= :shadow-not-consumed (:role inputs)))
        (is (seq (:universe inputs)))
        (is (contains? (set (:universe inputs)) "alpha"))
        (is (contains? (set (:universe inputs)) "omega")))))
  ;; one common universe: both arms carry identical universe/inputs except precedence
  (let [env (fixture-env)
        record (fixture-record env)
        results (shadow/shadow-cascade-g record
                                         {:before {:nodes #{:p/b} :precedence [:p/b]}
                                          :after {:nodes #{:p/a :p/b} :precedence [:p/a :p/b]}}
                                         {:library-root (str (:lib env))})]
    (is (= (get-in results [:before :inputs :universe])
           (get-in results [:after :inputs :universe])))))

(deftest shadow-g-refusals
  (let [env (fixture-env)]
    ;; no derivable mission document: target cites nothing, identity target has no file
    (let [record (-> (fixture-record env)
                     (assoc-in [:sources 0 :path] "/nonexistent/mission.md")
                     (assoc-in [:identity :occurrence :action/value :target] "M-nowhere"))
          r (shadow/shadow-cascade-g record {:after {:nodes #{:p/a} :precedence [:p/a]}}
                                     {:library-root (str (:lib env))})]
      (is (= {:status :missing :kind :mission-document-not-derivable}
             (get r :after))))
    ;; node without an interpretable flexiarg
    (let [record (fixture-record env)
          r (shadow/shadow-cascade-g record {:after {:nodes #{:p/c :p/a} :precedence [:p/a :p/c]}}
                                     {:library-root (str (:lib env))})]
      (is (= :missing (:status (get r :after))))
      (is (= :missing-pattern-interpretation (:kind (get r :after))))
      (is (= :p/c (:pattern (get r :after)))))
    ;; node with no library file at all
    (let [record (fixture-record env)
          r (shadow/shadow-cascade-g record {:after {:nodes #{:p/absent} :precedence [:p/absent]}}
                                     {:library-root (str (:lib env))})]
      (is (= :missing-pattern-interpretation (:kind (get r :after)))))
    ;; a thrown exception becomes the typed shadow-scorer-error
    (let [record (fixture-record env)
          r (shadow/shadow-cascade-g record {:after {:nodes #{:p/a} :precedence [:p/a]}}
                                     {:library-root (str (:lib env))
                                      :interpret-fn (fn [& _] (throw (IllegalStateException. "boom")))})]
      (is (= {:status :missing :kind :shadow-scorer-error :message "boom"}
             (get r :after))))))

(deftest shadow-does-not-change-shown
  ;; construction is exercised through the fixture corpus of find-receipt-test,
  ;; the same real retained record the receipt-construction tests use.
  (let [{:keys [record captured]} (find-sample)
        root "/home/joe/code/futon3/library"
        prev {:cascade policy/first-attempt-cascade :admitted {}
              :provenance {:status :none :reason :no-earlier-target-construction}
              :admission-reason :first-attempt-no-admissions}
        with-shadow (construction/construct record captured root prev nil)
        with-constant (construction/construct record captured root prev nil
                                              (constantly absent-score))]
    (is (= (:shown with-shadow) (:shown with-constant)))
    (is (= (:selected-action with-shadow) (:selected-action with-constant)))
    ;; the old constant is still available through the override
    (is (= absent-score
           (get-in with-constant [:receipted-construction :cascade-diff :score-after])))
    ;; the shadow result is recorded in the diff and is computed or typed-missing
    (let [score-after (get-in with-shadow [:receipted-construction :cascade-diff :score-after])]
      (is (contains? score-after :status))
      (is (not= :cascade-g-not-computed (:reason score-after))))))
