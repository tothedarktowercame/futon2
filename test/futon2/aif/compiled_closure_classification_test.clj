(ns futon2.aif.compiled-closure-classification-test
  "C1 (declared-effect contract review, claude-2, F1): a co-apply (compiled)
  cascade carries its patterns in a MAP under :precedence, so the
  vector-shaped enacted-step-pattern reader sees no declared products and
  classify-grounded-outcome used to preserve a requested :grounded-change --
  which the outer dataset counts as :closure -- with no mechanically
  verified intended-effect evidence.

  These tests pin the one behavior: on the co-apply shape the declared
  products are the union of the co-applied patterns' own :produces, and the
  SAME real accepted-increment observation checks decide closure.  The
  positive path runs the real C4 declaration-head checks (real git, real
  observation-checks code) against a throwaway fixture repository whose
  declaration is absent at the before-sha and present at the after-sha;
  observation-checks/repo-root and cascade-sources/load-declared are
  rebound only to point the real code at that fixture.  No live state is
  written."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-sources :as cascade-sources]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.meta-outcome-dataset :as dataset]
            [futon2.aif.observation-checks :as observation]))
  ;; deliberately does NOT alter full-loop-runner-test; dedicated namespace.

(def ^:dynamic *git-environment* (into {} (System/getenv)))

(defn- isolated-git-environment []
  (apply dissoc *git-environment*
         ["GIT_DIR" "GIT_WORK_TREE" "GIT_INDEX_FILE" "GIT_COMMON_DIR"]))

(defn- git! [repo & args]
  (let [r (apply shell/sh
                 (concat ["git" "-C" (str repo)] args
                         [:env (isolated-git-environment)]))]
    (when-not (zero? (:exit r)) (throw (ex-info "fixture git failed" r)))
    (str/trim (:out r))))

(defn- with-fixture-repo
  "Creates a throwaway git repository under a temp directory with two
  commits: BEFORE (a file whose declaration head is absent) and AFTER (the
  head present at line start).  Calls f with {:root :before-sha :after-sha
  :path :decl}.  Fixture only; no live state."
  [f]
  (let [dir (io/file (str (java.nio.file.Files/createTempDirectory
                           "compiled-closure-"
                           (make-array java.nio.file.attribute.FileAttribute 0))))
        repo (io/file dir "repo")]
    (try
      (.mkdirs repo)
      (git! repo "init" "-q")
      (spit (io/file repo "README.md") "fixture\n")
      (git! repo "add" ".")
      (git! repo "-c" "user.name=C1 fixture"
            "-c" "user.email=c1-fixture@example.invalid"
            "commit" "-qm" "before")
      (let [before-sha (git! repo "rev-parse" "HEAD")
            path "resources/wm/fixture-effect.md"
            decl "EFFECT-OBSERVED"]
        (.mkdirs (io/file repo "resources/wm"))
        (spit (io/file repo path) (str "# fixture\n" decl " token\n"))
        (git! repo "add" ".")
        (git! repo "-c" "user.name=C1 fixture"
              "-c" "user.email=c1-fixture@example.invalid"
              "commit" "-qm" "after")
        (let [after-sha (git! repo "rev-parse" "HEAD")]
          (f {:root (str dir) :repo-name "repo"
              :before-sha before-sha :after-sha after-sha
              :path path :decl decl})))
      (finally
        (doseq [file (reverse (file-seq dir))] (.delete file))))))

(defn- fixture-sources
  "A sources map in the real load-declared shape, pointing the target's
  want token and acceptance at C4 locators in the fixture repo at SHA."
  [{:keys [repo-name sha path decl]}]
  ;; the real load-declared shape: :wants (via which acceptance-of picks
  ;; the acceptance token), :locators per [target token], :files provenance
  (let [loc (fn [] {:class :C4 :repo repo-name :sha sha :path path :decl decl})]
    {:wants {"T-fixture-closure" [:fixture-done]}
     :locators {"T-fixture-closure" {:fixture-done (loc)
                                     :fixture-done-2 (loc)}}
     :files [{:path "fixture/T-fixture-closure.edn"
              :target "T-fixture-closure"}]}))

(defn- fresh-binding [commit head]
  {:repo "repo" :commit commit :pre-dispatch-head head
   :descendant? true :corroborates? true :claim-in-author-window? true
   :observed-valid? true})

(defn- co-apply-action
  "A co-apply cascade candidate for TARGET whose units each declare the
  given product tokens: units is a sequence of [id tokens]."
  [target units]
  {:kind :cascade-candidate :target target
   :precedence {:co-apply
                {:units (mapv first units)
                 :descent []
                 :patterns (into {}
                                 (for [[id tokens] units]
                                   [id {:id id
                                        :produces (set (map #(vector target %)
                                                            tokens))}]))}}})

(deftest co-apply-map-without-evidence-is-grounded-progress-not-closure
  ;; The exact shape of the classification probe
  ;; (compiled-closure-classification-probe.clj): a co-apply map precedence
  ;; declaring a product for a target with no cascade-source declaration.
  ;; Before C1 this preserved the requested :grounded-change and the outer
  ;; dataset counted :closure with zero effect measurements.
  (let [action (co-apply-action "M-probe" [[:p [:wanted]]])
        actual (runner/classify-grounded-outcome :grounded-change action {})]
    (is (= :grounded-progress actual))
    (is (= :grounded-progress (dataset/outcome-class actual))
        "the outer dataset must not count this as closure")))

(deftest co-apply-positive-runs-real-locator-checks
  (with-fixture-repo
    (fn [fx]
      (with-redefs [observation/repo-root (:root fx)
                    cascade-sources/load-declared (fn [& _] (fixture-sources fx))]
        (testing "decl absent at the before-sha: honest progress, not closure"
          (let [sources-before (fixture-sources (assoc fx :sha (:before-sha fx)))]
            (with-redefs [cascade-sources/load-declared (fn [& _] sources-before)]
              (let [actual (runner/classify-grounded-outcome
                            :grounded-change
                            (co-apply-action "T-fixture-closure" [[:p [:fixture-done]]])
                            {:artifact-binding (fresh-binding (:after-sha fx)
                                                              (:before-sha fx))
                             :commit (:after-sha fx)})]
                (is (= :grounded-progress actual))
                (is (= :grounded-progress (dataset/outcome-class actual)))))))
        (testing "decl present at the after-sha with a fresh binding: closure"
          (with-redefs [cascade-sources/load-declared
                        (fn [& _] (fixture-sources (assoc fx :sha (:after-sha fx))))]
          (let [actual (runner/classify-grounded-outcome
                        :grounded-change
                        (co-apply-action "T-fixture-closure" [[:p [:fixture-done]]])
                        {:artifact-binding (fresh-binding (:after-sha fx)
                                                          (:before-sha fx))
                         :commit (:after-sha fx)})]
            (is (= :grounded-change actual))
            (is (= :closure (dataset/outcome-class actual))))))
        (testing "a status-only binding (no corroboration) is not accepted"
          (with-redefs [cascade-sources/load-declared
                        (fn [& _] (fixture-sources (assoc fx :sha (:after-sha fx))))]
          (let [actual (runner/classify-grounded-outcome
                        :grounded-change
                        (co-apply-action "T-fixture-closure" [[:p [:fixture-done]]])
                        {:artifact-binding (assoc (fresh-binding (:after-sha fx)
                                                                 (:before-sha fx))
                                                  :corroborates? false)
                         :commit (:after-sha fx)})]
            (is (= :grounded-progress actual)))))))))

(deftest vector-and-non-grounded-behavior-is-preserved
  (testing "non-grounded requested outcomes pass through untouched"
    (is (= :grounded-no-change
           (runner/classify-grounded-outcome
            :grounded-no-change (co-apply-action "M-probe" [[:p [:wanted]]]) {})))
    (is (= :abstained
           (runner/classify-grounded-outcome
            :abstained (co-apply-action "M-probe" [[:p [:wanted]]]) {}))))
  (testing "vector precedence with declared products keeps its existing path"
    ;; no declaration for this target, so the accepted-increment check
    ;; fails honestly -- exactly the pre-C1 vector behavior.
    (let [action {:kind :cascade-candidate :target "M-probe"
                  :precedence [{:id :p :produces #{["M-probe" :wanted]}}]}
          actual (runner/classify-grounded-outcome :grounded-change action {})]
      (is (= :grounded-progress actual))))
  (testing "vector precedence declaring nothing retains the legacy outcome"
    (let [action {:kind :cascade-candidate :target "M-probe"
                  :precedence [{:id :p}]}
          actual (runner/classify-grounded-outcome :grounded-change action {})]
      (is (= :grounded-change actual))
      (is (= :closure (dataset/outcome-class actual)))))
  (testing "co-apply declaring nothing is honest progress, not closure"
    ;; codex-11 review of 0bb9f6a4b: the declare-nothing legacy outcome
    ;; belongs to the legacy VECTOR precedence only. An empty union on the
    ;; compiled map route claims nothing verifiable; no closure credit.
    (let [action {:kind :cascade-candidate :target "M-probe"
                  :precedence {:co-apply {:units [:p] :descent []
                                          :patterns {:p {:id :p}}}}}
          actual (runner/classify-grounded-outcome :grounded-change action {})]
      (is (= :grounded-progress actual))
      (is (= :grounded-progress (dataset/outcome-class actual))))))

(deftest exact-review-negatives-replay-to-progress
  ;; The exact executed probes compiled-closure-review-negatives.clj: both
  ;; cases replayed to :grounded-change/:closure before the follow-up.
  (doseq [[label precedence] [[:empty-products {:co-apply {:units [:p] :descent []
                                                           :patterns {:p {:id :p}}}}]
                              [:malformed {}]]]
    (testing (str "case " label)
      (let [v (runner/classify-grounded-outcome
               :grounded-change
               {:kind :cascade-candidate :target "M-probe" :precedence precedence}
               {})]
        (is (= :grounded-progress v))
        (is (= :grounded-progress (dataset/outcome-class v)))))))

(deftest malformed-co-apply-census-is-progress
  ;; The union is taken ONLY from the recorded :units census, and the
  ;; co-apply must be internally coherent. Each malformed variant yields
  ;; :grounded-progress -- never the legacy :grounded-change preserved for
  ;; a legacy vector cascade declaring nothing.
  (doseq [[label precedence]
          [[:units-missing {:co-apply {:descent []
                                       :patterns {:p {:id :p :produces []}}}}]
           [:units-empty {:co-apply {:units [] :descent []
                                     :patterns {:p {:id :p :produces []}}}}]
           [:unit-without-pattern {:co-apply {:units [:p :q] :descent []
                                              :patterns {:p {:id :p :produces []}}}}]
           [:pattern-outside-census {:co-apply {:units [:p] :descent []
                                                :patterns {:p {:id :p :produces []}
                                                           :ghost {:id :ghost
                                                                   :produces ["x"]}}}}]
           [:pattern-produces-not-a-collection {:co-apply {:units [:p] :descent []
                                                     :patterns {:p {:id :p
                                                                    :produces :bogus}}}}]
           [:co-apply-not-a-map {:co-apply [:p]}]
           [:patterns-not-a-map {:co-apply {:units [:p] :patterns #{:p}}}]]]
    (testing (name label)
      (let [v (runner/classify-grounded-outcome
               :grounded-change
               {:kind :cascade-candidate :target "M-probe" :precedence precedence}
               {})]
        (is (= :grounded-progress v))
        (is (= :grounded-progress (dataset/outcome-class v)))))))

(deftest legitimate-multi-unit-positive-closes-and-partial-fails
  (with-fixture-repo
    (fn [fx]
      (with-redefs [observation/repo-root (:root fx)
                    cascade-sources/load-declared
                    (fn [& _] (fixture-sources (assoc fx :sha (:after-sha fx))))]
        (testing "two units, both products observed true at the after-sha: closure"
          (let [actual (runner/classify-grounded-outcome
                        :grounded-change
                        (co-apply-action "T-fixture-closure"
                                         [[:p [:fixture-done]]
                                          [:q [:fixture-done-2]]])
                        {:artifact-binding (fresh-binding (:after-sha fx)
                                                          (:before-sha fx))
                         :commit (:after-sha fx)})]
            (is (= :grounded-change actual))
            (is (= :closure (dataset/outcome-class actual)))))
        (testing "one unit's product unobserved (locator false): honest progress"
          ;; :fixture-done-3 has no locator in the sources map, so the
          ;; accepted-increment (b) conjunct fails with a missing token.
          (let [actual (runner/classify-grounded-outcome
                        :grounded-change
                        (co-apply-action "T-fixture-closure"
                                         [[:p [:fixture-done]]
                                          [:q [:fixture-done-3]]])
                        {:artifact-binding (fresh-binding (:after-sha fx)
                                                          (:before-sha fx))
                         :commit (:after-sha fx)})]
            (is (= :grounded-progress actual))))))))

(deftest forged-binding-with-true-locators-is-not-accepted
  (with-fixture-repo
    (fn [fx]
      (with-redefs [observation/repo-root (:root fx)
                    cascade-sources/load-declared
                    (fn [& _] (fixture-sources (assoc fx :sha (:after-sha fx))))]
        ;; A forged binding claims every freshness flag but cannot supply
        ;; a pre-dispatch head: the accepted-increment (a) conjunct fails
        ;; honestly (:binding-not-fresh) even with every locator true.
        (let [forged (dissoc (fresh-binding (:after-sha fx) (:before-sha fx))
                             :pre-dispatch-head)
              actual (runner/classify-grounded-outcome
                      :grounded-change
                      (co-apply-action "T-fixture-closure" [[:p [:fixture-done]]])
                      {:artifact-binding forged
                       :commit (:before-sha fx)})]
          (is (= :grounded-progress actual))
          (is (= :grounded-progress (dataset/outcome-class actual))))))))

(deftest foreign-qualified-product-cannot-use-local-locator
  (with-fixture-repo
    (fn [fx]
      (with-redefs [observation/repo-root (:root fx)
                    cascade-sources/load-declared
                    (fn [& _] (fixture-sources (assoc fx :sha (:after-sha fx))))]
        (let [action (co-apply-action "T-fixture-closure" [[:p [:fixture-done]]])
              data {:artifact-binding (fresh-binding (:after-sha fx) (:before-sha fx))
                    :commit (:after-sha fx)}]
          (is (= :grounded-change
                 (runner/classify-grounded-outcome :grounded-change action data)))
          (doseq [product [["foreign-target" :fixture-done]
                           ["T-fixture-closure" :fixture-done :extra]]]
            (is (= :grounded-progress
                   (runner/classify-grounded-outcome
                    :grounded-change
                    (assoc-in action [:precedence :co-apply :patterns :p :produces]
                              #{product})
                    data)))))))))
